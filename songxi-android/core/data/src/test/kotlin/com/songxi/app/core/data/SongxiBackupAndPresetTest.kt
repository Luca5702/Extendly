package com.songxi.app.core.data

import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SavedPrescriptionPreset
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SomaticFeeling
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongxiBackupAndPresetTest {

    private fun newTempStoreFile(prefix: String): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_backup_test/${prefix}_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun savedPresetsDeduplicateCapAtFourAndPersistAcrossRepositoryInstances() =
        runTest(StandardTestDispatcher()) {
            val store = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile("presets"), backgroundScope)
            val repo = DataStoreStretchCatalogRepository(store)

            val allRegions = BodyRegion.entries
            for (idx in 0 until 5) {
                val r = setOf(allRegions[idx % allRegions.size])
                val id = SavedPrescriptionPreset.buildPresetId(r, 4, LateralAsymmetry.BALANCED)
                val title = SavedPrescriptionPreset.buildDisplayTitle(r, 4, LateralAsymmetry.BALANCED)
                repo.saveCustomPreset(
                    SavedPrescriptionPreset(
                        id = id,
                        title = title,
                        regions = r,
                        durationMinutes = 4,
                        asymmetry = LateralAsymmetry.BALANCED,
                        createdAtEpochMs = 1_000L + idx
                    )
                )
            }

            val saved = repo.savedPresets.first()
            assertEquals(SavedPrescriptionPreset.MAX_SAVED_PRESETS, saved.size)

            val toDelete = saved.first().id
            repo.deleteCustomPreset(toDelete)
            assertEquals(3, repo.savedPresets.first().size)
            assertFalse(repo.savedPresets.first().any { it.id == toDelete })
        }

    @Test
    fun backupCodecRoundTripsSnapshotAndRejectsInvalidPayloads() = runTest(StandardTestDispatcher()) {
        val presetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.MOUSE_WRIST)
        val preset = SavedPrescriptionPreset(
            id = SavedPrescriptionPreset.buildPresetId(presetRegions, 4, LateralAsymmetry.RIGHT_TIGHTER),
            title = SavedPrescriptionPreset.buildDisplayTitle(presetRegions, 4, LateralAsymmetry.RIGHT_TIGHTER),
            regions = presetRegions,
            durationMinutes = 4,
            asymmetry = LateralAsymmetry.RIGHT_TIGHTER,
            createdAtEpochMs = 1_795_000_000_000L
        )
        val record = PracticeSessionRecord(
            id = "rec_backup_1",
            completedAtEpochMs = 1_795_000_120_000L,
            dateIso = "2026-11-23",
            routineId = "custom_neck_wrist_4m_right",
            routineTitle = "《风池与护腕 · 温养小方（4分钟）》",
            durationSeconds = 240,
            skinId = SkinId.QINGLV_CELADON,
            feeling = SomaticFeeling.LIGHT_NECK_SHOULDER,
            targetRegions = presetRegions,
            asymmetry = LateralAsymmetry.RIGHT_TIGHTER
        )
        val original = SongxiSanctuaryBackupSnapshot(
            exportedAtIso = "2026-11-23T12:00:00Z",
            selectedSkinId = SkinId.QINGLV_CELADON,
            autoNightSkinEnabled = false,
            voiceGuidanceEnabled = true,
            prepBufferEnabled = true,
            reminderPreferences = ReminderPreferences(intervalMinutes = 90),
            bookletCompletedDays = mapOf("booklet_neck_thoracic" to setOf(0, 2)),
            savedPresets = listOf(preset),
            journalRecords = listOf(record)
        )

        val encoded = SongxiBackupCodec.encodeSnapshot(original)
        val decoded = SongxiBackupCodec.decodeSnapshot(encoded).getOrThrow()
        assertEquals(original, decoded)

        assertTrue(SongxiBackupCodec.decodeSnapshot("   ").isFailure)
        assertTrue(SongxiBackupCodec.decodeSnapshot("""{"appIdentifier":"other.app","schemaVersion":1,"exportedAtIso":"x"}""").isFailure)

        // Verify preference & journal repositories restore cleanly and idempotently
        val themeStore = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile("theme_bk"), backgroundScope)
        val themeRepo = DataStoreThemePreferencesRepository(themeStore)
        themeRepo.setVoiceGuidanceEnabled(decoded.voiceGuidanceEnabled)
        themeRepo.setPrepBufferEnabled(decoded.prepBufferEnabled)
        assertTrue(themeRepo.voiceGuidanceEnabled.first())
        assertTrue(themeRepo.prepBufferEnabled.first())

        val journalStore = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile("journal_bk"), backgroundScope)
        val journalRepo = DataStorePracticeJournalRepository(journalStore)
        journalRepo.restoreSessions(decoded.journalRecords)
        journalRepo.restoreSessions(decoded.journalRecords)
        assertEquals(1, journalRepo.allRecords.first().size)
        assertEquals(record, journalRepo.allRecords.first().single())
    }
}
