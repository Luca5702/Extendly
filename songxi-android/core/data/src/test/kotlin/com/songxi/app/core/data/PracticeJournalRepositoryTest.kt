package com.songxi.app.core.data

import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SomaticFeeling
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeJournalRepositoryTest {

    private fun newTempStoreFile(): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_journal_test/journal_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun recordsMonthlyRipplesIdempotentUpsertAndClearAll() = runTest(StandardTestDispatcher()) {
        val store = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile(), backgroundScope)
        val repo = DataStorePracticeJournalRepository(store)

        assertTrue(repo.allRecords.first().isEmpty())

        val novMorning = PracticeSessionRecord(
            id = "session_1",
            completedAtEpochMs = 1_794_794_400_000L,
            dateIso = "2026-11-16",
            routineId = "quick_meeting_sigh",
            routineTitle = "连会余韵叹息",
            durationSeconds = 90,
            skinId = SkinId.INK_MIST,
            feeling = null
        )
        val novAfternoon = PracticeSessionRecord(
            id = "session_2",
            completedAtEpochMs = 1_794_816_000_000L,
            dateIso = "2026-11-16",
            routineId = "hero_desk_seated",
            routineTitle = "午后伏案 · 沉肩松颈帖",
            durationSeconds = 180,
            skinId = SkinId.QINGLV_CELADON,
            feeling = SomaticFeeling.LIGHT_NECK_SHOULDER
        )
        val novOtherDay = PracticeSessionRecord(
            id = "session_3",
            completedAtEpochMs = 1_794_988_800_000L,
            dateIso = "2026-11-18",
            routineId = "hero_evening_bedside",
            routineTitle = "晚间沉香 · 腰背解绑帖",
            durationSeconds = 360,
            skinId = SkinId.DUSK_PINE,
            feeling = SomaticFeeling.CALM_PEACEFUL
        )
        val decSession = PracticeSessionRecord(
            id = "session_4",
            completedAtEpochMs = 1_796_200_000_000L,
            dateIso = "2026-12-02",
            routineId = "quick_screen_warm_eyes",
            routineTitle = "久盯屏幕温目",
            durationSeconds = 120,
            skinId = SkinId.SPRING_BLOSSOM,
            feeling = SomaticFeeling.WARM_OPEN
        )

        repo.recordCompletedSession(novMorning)
        repo.recordCompletedSession(novAfternoon)
        repo.recordCompletedSession(novOtherDay)
        repo.recordCompletedSession(decSession)

        // Idempotent upsert: attach feeling to session_1 without creating duplicate ripple
        val novMorningWithFeeling = novMorning.copy(feeling = SomaticFeeling.WARM_OPEN)
        repo.recordCompletedSession(novMorningWithFeeling)

        val all = repo.allRecords.first()
        assertEquals(4, all.size)
        assertEquals("session_4", all.first().id)
        assertEquals(SomaticFeeling.WARM_OPEN, all.first { it.id == "session_1" }.feeling)

        val novRipples = repo.observeMonthRipples(YearMonth.of(2026, 11)).first()
        assertEquals(2, novRipples.size)
        assertEquals(2, novRipples[LocalDate.of(2026, 11, 16)]?.size)
        assertEquals(1, novRipples[LocalDate.of(2026, 11, 18)]?.size)

        val novSummary = repo.observeMonthSummary(YearMonth.of(2026, 11)).first()
        assertEquals(3, novSummary.totalSessions)
        assertEquals(90 + 180 + 360, novSummary.totalDurationSeconds)
        assertEquals(11, novSummary.totalMinutesRoundedUp)
        assertEquals(2, novSummary.practicedDaysCount)

        repo.clearAll()
        assertTrue(repo.allRecords.first().isEmpty())
    }
}
