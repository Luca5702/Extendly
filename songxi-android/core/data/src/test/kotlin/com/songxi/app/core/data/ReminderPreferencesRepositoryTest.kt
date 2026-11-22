package com.songxi.app.core.data

import com.songxi.app.core.model.ReminderPreferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPreferencesRepositoryTest {

    private fun newTempStoreFile(): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_reminder_test/reminder_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun preferencesClampingDateIsolatedNotificationCountsAndReset() = runTest(StandardTestDispatcher()) {
        val store = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile(), backgroundScope)
        val repo = DataStoreReminderPreferencesRepository(store)

        assertEquals(ReminderPreferences(), repo.preferences.first())
        assertEquals(0, repo.notificationsSentOn("2026-11-16").first())

        repo.updatePreferences(
            ReminderPreferences(
                enabled = true,
                intervalMinutes = 90,
                dailyMaxCount = 99,
                lunchQuietEnabled = false,
                nightQuietEnabled = true,
                weekendSilent = false,
                muteTodayDateIso = "2026-11-16"
            )
        )
        val clampedHigh = repo.preferences.first()
        assertEquals(90, clampedHigh.intervalMinutes)
        assertEquals(3, clampedHigh.dailyMaxCount)
        assertEquals("2026-11-16", clampedHigh.muteTodayDateIso)

        repo.updatePreferences(clampedHigh.copy(dailyMaxCount = 0))
        assertEquals(1, repo.preferences.first().dailyMaxCount)

        repo.recordNotificationSent("2026-11-16")
        repo.recordNotificationSent("2026-11-16")
        repo.recordNotificationSent("2026-11-17")

        assertEquals(2, repo.notificationsSentOn("2026-11-16").first())
        assertEquals(1, repo.notificationsSentOn("2026-11-17").first())
        assertEquals(0, repo.notificationsSentOn("2026-11-18").first())

        repo.resetAll()
        assertEquals(ReminderPreferences(), repo.preferences.first())
        assertEquals(0, repo.notificationsSentOn("2026-11-16").first())
    }
}
