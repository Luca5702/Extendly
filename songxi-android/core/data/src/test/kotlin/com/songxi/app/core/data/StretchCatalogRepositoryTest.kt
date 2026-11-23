package com.songxi.app.core.data

import com.songxi.app.core.model.AnatomicalViewPlane
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PrescriptionPhaseRole
import com.songxi.app.core.model.StretchModality
import com.songxi.app.core.model.StretchPose
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchCatalogRepositoryTest {

    private fun newTempStoreFile(prefix: String): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_catalog_test/${prefix}_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun catalogContains24ValidPosesCoveringAllSixBodyRegions() = runTest(StandardTestDispatcher()) {
        val store = SongxiDataStoreFactory.createPreferenceDataStore(
            newTempStoreFile("catalog_poses"),
            backgroundScope
        )
        val repo = DataStoreStretchCatalogRepository(store)

        assertEquals(24, repo.allPoses.size)
        assertEquals(24, repo.allPoses.map { it.id }.toSet().size)

        for (region in BodyRegion.entries) {
            val matching = repo.allPoses.count { region in it.targetRegions }
            assertTrue("Expected >= 4 poses for $region, got $matching", matching >= 4)
        }

        val modalitiesCovered = repo.allPoses.map { it.clinical.modality }.toSet()
        assertEquals(StretchModality.entries.toSet(), modalitiesCovered)

        val viewPlanesCovered = repo.allPoses.map { it.clinical.viewPlane }.toSet()
        assertEquals(AnatomicalViewPlane.entries.toSet(), viewPlanesCovered)

        val phasesCovered = repo.allPoses.map { it.clinical.phaseRole }.toSet()
        assertEquals(PrescriptionPhaseRole.entries.toSet(), phasesCovered)

        for (pose in repo.allPoses) {
            assertTrue(
                "Cue length out of bounds for ${pose.id}: ${pose.gentleCue.length}",
                pose.gentleCue.length in 1..StretchPose.MAX_GENTLE_CUE_CHARS
            )
            assertTrue(pose.clinical.primaryMusclesCn.isNotBlank())
            assertTrue(pose.clinical.targetSensationCn.isNotBlank())
            assertTrue(pose.clinical.antiCompensationCn.isNotBlank())
            assertTrue(pose.clinical.redFlagWarningCn.isNotBlank())
            assertTrue(pose.clinical.setupCueCn.length in 1..StretchPose.MAX_GENTLE_CUE_CHARS)
            assertTrue(pose.clinical.safeReturnCueCn.length in 1..StretchPose.MAX_GENTLE_CUE_CHARS)
            assertNotEquals(
                "Expected distinct inhale and exhale keyframes for ${pose.id}",
                pose.inhaleKeyframe,
                pose.exhaleKeyframe
            )
        }
    }

    @Test
    fun heroQuickAndBookletRoutinesHaveExactExpectedDurations() = runTest(StandardTestDispatcher()) {
        val store = SongxiDataStoreFactory.createPreferenceDataStore(
            newTempStoreFile("catalog_routines"),
            backgroundScope
        )
        val repo = DataStoreStretchCatalogRepository(store)

        assertEquals(180, repo.heroRoutineFor(PostureScene.DESK_SEATED).totalDurationSeconds)
        assertEquals(150, repo.heroRoutineFor(PostureScene.TEA_BREAK_STANDING).totalDurationSeconds)
        assertEquals(360, repo.heroRoutineFor(PostureScene.EVENING_BEDSIDE).totalDurationSeconds)

        assertEquals(4, repo.quickReliefRoutines.size)
        assertEquals(listOf(90, 120, 120, 180), repo.quickReliefRoutines.map { it.totalDurationSeconds })
        assertEquals("quick_meeting_sigh", repo.reminderQuickStartRoutine().id)
        assertEquals(90, repo.reminderQuickStartRoutine().totalDurationSeconds)

        assertEquals(4, repo.curatedBooklets.size)
        for (booklet in repo.curatedBooklets) {
            assertEquals(
                "Booklet ${booklet.id} routines count must match totalDays",
                booklet.totalDays,
                booklet.routines.size
            )
            for (dayIndex in 0 until booklet.totalDays) {
                val daily = booklet.routineForDay(dayIndex)
                assertEquals(booklet.dailyMinutes * 60, daily.totalDurationSeconds)
                assertEquals(daily, repo.findRoutineById(daily.id))
            }
        }
    }

    @Test
    fun composeCustomRoutineExhaustiveSubsetsAndDurations() = runTest(StandardTestDispatcher()) {
        val store = SongxiDataStoreFactory.createPreferenceDataStore(
            newTempStoreFile("catalog_composer"),
            backgroundScope
        )
        val repo = DataStoreStretchCatalogRepository(store)
        val allRegions = BodyRegion.entries

        // Exhaustively verify all 2^6 = 64 subsets across 2m, 4m, and 6m durations
        for (mask in 0 until (1 shl allRegions.size)) {
            val subset = linkedSetOf<BodyRegion>()
            for (idx in allRegions.indices) {
                if ((mask and (1 shl idx)) != 0) {
                    subset.add(allRegions[idx])
                }
            }
            for (minutes in listOf(2, 4, 6)) {
                val routine = repo.composeCustomRoutine(subset, minutes)
                assertEquals(minutes * 60, routine.totalDurationSeconds)
                assertEquals(routine.poses.size, routine.poses.map { it.id }.toSet().size)

                // Verify clinical 3-stage progression is monotonically non-decreasing (解绑 -> 深舒 -> 归位)
                val phaseOrders = routine.poses.map { it.clinical.phaseRole.order }
                assertEquals(
                    "Expected physiological 3-phase ordering in ${routine.id}, got $phaseOrders",
                    phaseOrders.sorted(),
                    phaseOrders
                )

                for (pose in routine.poses) {
                    assertEquals(
                        "Pose duration in ${routine.id} must align to 10s breath cycle",
                        0,
                        pose.durationSeconds % 10
                    )
                }
                for (region in subset) {
                    assertTrue(
                        "Expected region $region to be covered in ${routine.id}",
                        routine.poses.any { region in it.targetRegions }
                    )
                }
            }
        }

        // Single-region prescription contains all 3 clinical phases (解绑 + 深舒 + 归位)
        for (singleRegion in allRegions) {
            val singleRoutine = repo.composeCustomRoutine(setOf(singleRegion), 4)
            assertEquals(
                PrescriptionPhaseRole.entries.toSet(),
                singleRoutine.poses.map { it.clinical.phaseRole }.toSet()
            )
        }

        // Verify set order independence and stateless findRoutineById reconstruction on a fresh repo
        val orderA = repo.composeCustomRoutine(
            linkedSetOf(BodyRegion.MOUSE_WRIST, BodyRegion.POSTERIOR_NECK),
            4
        )
        val orderB = repo.composeCustomRoutine(
            linkedSetOf(BodyRegion.POSTERIOR_NECK, BodyRegion.MOUSE_WRIST),
            4
        )
        assertEquals(orderA, orderB)

        val rightAsymmetric = repo.composeCustomRoutine(
            linkedSetOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.POSTERIOR_NECK),
            4,
            LateralAsymmetry.RIGHT_TIGHTER
        )
        assertEquals(LateralAsymmetry.RIGHT_TIGHTER, rightAsymmetric.asymmetry)
        assertTrue(rightAsymmetric.id.endsWith("_right"))

        val freshRepo = DataStoreStretchCatalogRepository(store)
        val resolvedAfterProcessRestart = freshRepo.findRoutineById(orderA.id)
        assertNotNull(resolvedAfterProcessRestart)
        assertEquals(orderA, resolvedAfterProcessRestart)
        assertEquals(rightAsymmetric, freshRepo.findRoutineById(rightAsymmetric.id))
    }

    @Test
    fun bookletCompletedDaysPersistsAcrossRepositoryInstancesAndResets() =
        runTest(StandardTestDispatcher()) {
            val file = newTempStoreFile("catalog_booklet_progress")
            val store = SongxiDataStoreFactory.createPreferenceDataStore(file, backgroundScope)
            val repo1 = DataStoreStretchCatalogRepository(store)

            assertTrue(repo1.bookletCompletedDays.first().isEmpty())

            repo1.markBookletDayCompleted("booklet_neck_thoracic", 0)
            repo1.markBookletDayCompleted("booklet_neck_thoracic", 2)
            repo1.markBookletDayCompleted("booklet_eyes_wrist_care", 1)

            val repo2 = DataStoreStretchCatalogRepository(store)
            val progress = repo2.bookletCompletedDays.first()
            assertEquals(setOf(0, 2), progress["booklet_neck_thoracic"])
            assertEquals(setOf(1), progress["booklet_eyes_wrist_care"])

            repo2.resetAll()
            assertFalse(repo2.bookletCompletedDays.first().containsKey("booklet_neck_thoracic"))
            assertTrue(repo2.bookletCompletedDays.first().isEmpty())
        }
}
