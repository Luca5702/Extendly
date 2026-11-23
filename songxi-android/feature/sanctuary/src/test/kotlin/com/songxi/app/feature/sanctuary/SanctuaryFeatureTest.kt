package com.songxi.app.feature.sanctuary

import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.songxi.app.core.data.DataStoreReminderPreferencesRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.ReminderPreferences
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SanctuaryFeatureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @After
    fun tearDown() {
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
    }

    @Test
    fun scheduler_enqueues_periodic_work_and_cancels_cleanly_when_disabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dsFile = File(tempFolder.newFolder(), "rem.preferences_pb")
        val repo = DataStoreReminderPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { dsFile })
        )
        var epoch = LocalDateTime.of(2026, 11, 18, 10, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val clock = ClockProvider { epoch }

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .setWorkerFactory(SongxiReminderWorkerFactory({ repo }, { clock }))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Shadows.shadowOf(nm).setNotificationsEnabled(true)

        SedentaryReminderScheduler.syncSchedule(context, ReminderPreferences(enabled = true, intervalMinutes = 90))
        val query = WorkQuery.fromStates(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)
        val scheduled = WorkManager.getInstance(context).getWorkInfos(query).get(2, TimeUnit.SECONDS)
        assertTrue(scheduled.any { it.periodicityInfo?.repeatIntervalMillis == 90L * 60_000L })

        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        scheduled.forEach {
            driver.setAllConstraintsMet(it.id)
            driver.setPeriodDelayMet(it.id)
        }
        Thread.sleep(50L)
        assertEquals(1, Shadows.shadowOf(nm).allNotifications.size)

        // Lunch quiet hour 12:30 -> suppressed
        nm.cancelAll()
        epoch = LocalDateTime.of(2026, 11, 18, 12, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        scheduled.forEach { driver.setPeriodDelayMet(it.id) }
        Thread.sleep(50L)
        assertEquals(0, Shadows.shadowOf(nm).allNotifications.size)

        // Disabling cancels periodic work
        SedentaryReminderScheduler.syncSchedule(context, ReminderPreferences(enabled = false))
        val afterDisable = WorkManager.getInstance(context).getWorkInfos(query).get(2, TimeUnit.SECONDS)
        assertTrue(afterDisable.isEmpty())
    }

    @Test
    fun monthly_body_relief_atlas_aggregates_direct_and_catalog_fallback_regions() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dsFile = File(tempFolder.newFolder(), "cat_atlas.preferences_pb")
        val catalogRepo = com.songxi.app.core.data.DataStoreStretchCatalogRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { dsFile })
        )
        val d1 = java.time.LocalDate.of(2026, 11, 18)
        val d2 = java.time.LocalDate.of(2026, 11, 19)
        val ripples = mapOf(
            d1 to listOf(
                com.songxi.app.core.model.PracticeSessionRecord(
                    id = "s1",
                    completedAtEpochMs = 1_000L,
                    dateIso = "2026-11-18",
                    routineId = "custom_neck_wrist_4m_right",
                    routineTitle = "《风池与护腕 · 温养小方（4分钟）》",
                    durationSeconds = 240,
                    skinId = com.songxi.app.core.model.SkinId.INK_MIST,
                    feeling = com.songxi.app.core.model.SomaticFeeling.LIGHT_NECK_SHOULDER,
                    targetRegions = setOf(
                        com.songxi.app.core.model.BodyRegion.POSTERIOR_NECK,
                        com.songxi.app.core.model.BodyRegion.MOUSE_WRIST
                    ),
                    asymmetry = com.songxi.app.core.model.LateralAsymmetry.RIGHT_TIGHTER
                )
            ),
            d2 to listOf(
                // Legacy record without stored targetRegions -> resolved via catalogRepository
                com.songxi.app.core.model.PracticeSessionRecord(
                    id = "s2",
                    completedAtEpochMs = 2_000L,
                    dateIso = "2026-11-19",
                    routineId = "quick_screen_warm_eyes",
                    routineTitle = "久盯屏幕温目",
                    durationSeconds = 120,
                    skinId = com.songxi.app.core.model.SkinId.QINGLV_CELADON,
                    feeling = com.songxi.app.core.model.SomaticFeeling.CALM_PEACEFUL
                )
            )
        )

        val atlas = computeMonthlyBodyReliefAtlas(ripples, catalogRepo)
        assertTrue(atlas.hasAnyReliefData)
        assertEquals(2, atlas.regionCounts[com.songxi.app.core.model.BodyRegion.POSTERIOR_NECK])
        assertEquals(1, atlas.regionCounts[com.songxi.app.core.model.BodyRegion.MOUSE_WRIST])
        assertEquals(1, atlas.regionCounts[com.songxi.app.core.model.BodyRegion.EYE_BROW])
        assertEquals(1, atlas.feelingCounts[com.songxi.app.core.model.SomaticFeeling.LIGHT_NECK_SHOULDER])
        assertEquals(1, atlas.feelingCounts[com.songxi.app.core.model.SomaticFeeling.CALM_PEACEFUL])
        assertEquals(1, atlas.asymmetricSessionCount)
    }
}
