package com.songxi.app.core.data

import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SavedPrescriptionPreset
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ThemePreferencesRepository {
    val selectedSkinId: Flow<SkinId>
    val autoNightSkinEnabled: Flow<Boolean>
    val onboardingSeen: Flow<Boolean>
    val voiceGuidanceEnabled: Flow<Boolean>
        get() = flowOf(false)
    val prepBufferEnabled: Flow<Boolean>
        get() = flowOf(false)

    suspend fun setSelectedSkin(skinId: SkinId)
    suspend fun setAutoNightSkin(enabled: Boolean)
    suspend fun markOnboardingSeen(seen: Boolean = true)
    suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {}
    suspend fun setPrepBufferEnabled(enabled: Boolean) {}
    suspend fun resetAll()
}

interface StretchCatalogRepository {
    val allPoses: List<StretchPose>
    val quickReliefRoutines: List<StretchRoutine>
    val curatedBooklets: List<BookletSeries>
    val bookletCompletedDays: Flow<Map<String, Set<Int>>>
    val savedPresets: Flow<List<SavedPrescriptionPreset>>
        get() = flowOf(emptyList())

    fun heroRoutineFor(scene: PostureScene): StretchRoutine
    fun reminderQuickStartRoutine(): StretchRoutine
    fun findRoutineById(routineId: String): StretchRoutine?
    fun composeCustomRoutine(
        regions: Set<BodyRegion>,
        targetMinutes: Int,
        asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED
    ): StretchRoutine
    suspend fun markBookletDayCompleted(bookletId: String, dayIndex: Int)
    suspend fun saveCustomPreset(preset: SavedPrescriptionPreset) {}
    suspend fun deleteCustomPreset(presetId: String) {}
    suspend fun restoreBookletAndPresets(
        bookletProgress: Map<String, Set<Int>>,
        presets: List<SavedPrescriptionPreset>
    ) {}
    suspend fun resetAll()
}

/** Calm monthly summary with zero streak counters ("留白亦是休息"). */
data class MonthlyRippleSummary(
    val month: YearMonth,
    val totalSessions: Int,
    val totalDurationSeconds: Int,
    val practicedDaysCount: Int
) {
    val totalMinutesRoundedUp: Int
        get() = if (totalDurationSeconds <= 0) 0 else (totalDurationSeconds + 59) / 60
}

interface PracticeJournalRepository {
    val allRecords: Flow<List<PracticeSessionRecord>>

    fun observeMonthRipples(month: YearMonth): Flow<Map<LocalDate, List<PracticeSessionRecord>>>
    fun observeMonthSummary(month: YearMonth): Flow<MonthlyRippleSummary>
    suspend fun recordCompletedSession(record: PracticeSessionRecord)
    suspend fun restoreSessions(records: List<PracticeSessionRecord>) {
        records.forEach { recordCompletedSession(it) }
    }
    suspend fun clearAll()
}

interface ReminderPreferencesRepository {
    val preferences: Flow<ReminderPreferences>

    suspend fun updatePreferences(preferences: ReminderPreferences)
    fun notificationsSentOn(dateIso: String): Flow<Int>
    suspend fun recordNotificationSent(dateIso: String)
    suspend fun resetAll()
}

interface SplashAdLedgerRepository {
    val totalAppLaunches: Flow<Int>
    val personalizedAdsEnabled: Flow<Boolean>
    val lastImpressionEpochMs: Flow<Long?>
    val totalSkips: Flow<Int>
    val totalCtaClicks: Flow<Int>

    fun impressionsOn(dateIso: String): Flow<Int>
    suspend fun incrementAppLaunch(): Int
    suspend fun recordImpression(creativeId: String, epochMs: Long, dateIso: String)
    suspend fun recordSkip(creativeId: String, elapsedMs: Long)
    suspend fun recordCtaClick(creativeId: String)
    suspend fun setPersonalizedAdsEnabled(enabled: Boolean)
    suspend fun resetAll()
}
