package com.songxi.app.feature.sanctuary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songxi.app.core.data.MonthlyRippleSummary
import com.songxi.app.core.data.PracticeJournalRepository
import com.songxi.app.core.data.ReminderPreferencesRepository
import com.songxi.app.core.data.SongxiBackupCodec
import com.songxi.app.core.data.SongxiSanctuaryBackupSnapshot
import com.songxi.app.core.data.SplashAdLedgerRepository
import com.songxi.app.core.data.StretchCatalogRepository
import com.songxi.app.core.data.ThemePreferencesRepository
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SomaticFeeling
import com.songxi.app.core.model.SystemClockProvider
import com.songxi.app.core.sensory.BreathingHapticEngine
import com.songxi.app.core.sensory.HarmonicAudioEngine
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Streak-free anatomical relief distribution & somatic feeling atlas for the displayed month.
 */
data class MonthlyBodyReliefAtlas(
    val regionCounts: Map<BodyRegion, Int> = BodyRegion.entries.associateWith { 0 },
    val feelingCounts: Map<SomaticFeeling, Int> = SomaticFeeling.entries.associateWith { 0 },
    val asymmetricSessionCount: Int = 0
) {
    val maxRegionCount: Int
        get() = regionCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    val hasAnyReliefData: Boolean
        get() = regionCounts.values.any { it > 0 }
}

internal fun computeMonthlyBodyReliefAtlas(
    monthRipples: Map<LocalDate, List<PracticeSessionRecord>>,
    catalogRepository: StretchCatalogRepository? = null
): MonthlyBodyReliefAtlas {
    val regionCounts = linkedMapOf<BodyRegion, Int>().apply {
        BodyRegion.entries.forEach { put(it, 0) }
    }
    val feelingCounts = linkedMapOf<SomaticFeeling, Int>().apply {
        SomaticFeeling.entries.forEach { put(it, 0) }
    }
    var asymmetricCount = 0

    for (record in monthRipples.values.flatten()) {
        val resolvedRegions = record.targetRegions.ifEmpty {
            catalogRepository?.findRoutineById(record.routineId)
                ?.poses
                ?.flatMapTo(linkedSetOf()) { it.targetRegions }
                .orEmpty()
        }
        for (region in resolvedRegions) {
            regionCounts[region] = (regionCounts[region] ?: 0) + 1
        }
        record.feeling?.let { feeling ->
            feelingCounts[feeling] = (feelingCounts[feeling] ?: 0) + 1
        }
        if (record.asymmetry != LateralAsymmetry.BALANCED) {
            asymmetricCount++
        }
    }

    return MonthlyBodyReliefAtlas(
        regionCounts = regionCounts,
        feelingCounts = feelingCounts,
        asymmetricSessionCount = asymmetricCount
    )
}

data class SanctuaryUiState(
    val selectedSkinId: SkinId = SkinId.INK_MIST,
    val autoNightSkinEnabled: Boolean = true,
    val voiceGuidanceEnabled: Boolean = false,
    val prepBufferEnabled: Boolean = false,
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedJournalDate: LocalDate? = null,
    val monthRipples: Map<LocalDate, List<PracticeSessionRecord>> = emptyMap(),
    val monthSummary: MonthlyRippleSummary = MonthlyRippleSummary(
        month = YearMonth.now(),
        totalSessions = 0,
        totalDurationSeconds = 0,
        practicedDaysCount = 0
    ),
    val reliefAtlas: MonthlyBodyReliefAtlas = MonthlyBodyReliefAtlas(),
    val reminderPreferences: ReminderPreferences = ReminderPreferences(),
    val personalizedSplashEnabled: Boolean = false,
    val showSharePreviewDialog: Boolean = false,
    val showReminderInAppBanner: Boolean = false,
    val showBackupDialog: Boolean = false,
    val latestBackupJson: String = "",
    val backupStatusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SanctuaryViewModel(
    private val appContext: Context,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val journalRepository: PracticeJournalRepository,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val splashAdLedgerRepository: SplashAdLedgerRepository,
    private val audioEngine: HarmonicAudioEngine,
    private val hapticEngine: BreathingHapticEngine,
    private val catalogRepository: StretchCatalogRepository? = null,
    val clockProvider: ClockProvider = SystemClockProvider,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private var lastObservedClockMonth: YearMonth = YearMonth.from(clockProvider.currentLocalDate())
    private val _selectedMonth = MutableStateFlow(lastObservedClockMonth)

    private val _uiState = MutableStateFlow(
        SanctuaryUiState(
            displayedMonth = lastObservedClockMonth,
            monthSummary = MonthlyRippleSummary(
                month = lastObservedClockMonth,
                totalSessions = 0,
                totalDurationSeconds = 0,
                practicedDaysCount = 0
            )
        )
    )
    val uiState: StateFlow<SanctuaryUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                themePreferencesRepository.selectedSkinId,
                themePreferencesRepository.autoNightSkinEnabled,
                reminderPreferencesRepository.preferences,
                splashAdLedgerRepository.personalizedAdsEnabled
            ) { skinId, autoNight, reminderPrefs, personalizedAds ->
                Quad(skinId, autoNight, reminderPrefs, personalizedAds)
            }.collectLatest { (skinId, autoNight, reminderPrefs, personalizedAds) ->
                _uiState.update { state ->
                    state.copy(
                        selectedSkinId = skinId,
                        autoNightSkinEnabled = autoNight,
                        reminderPreferences = reminderPrefs,
                        personalizedSplashEnabled = personalizedAds
                    )
                }
                SedentaryReminderScheduler.syncSchedule(appContext, reminderPrefs)
            }
        }

        scope.launch {
            combine(
                themePreferencesRepository.voiceGuidanceEnabled,
                themePreferencesRepository.prepBufferEnabled
            ) { voiceOn, prepOn ->
                voiceOn to prepOn
            }.collectLatest { (voiceOn, prepOn) ->
                _uiState.update { state ->
                    state.copy(
                        voiceGuidanceEnabled = voiceOn,
                        prepBufferEnabled = prepOn
                    )
                }
            }
        }

        scope.launch {
            _selectedMonth.flatMapLatest { month ->
                combine(
                    journalRepository.observeMonthRipples(month),
                    journalRepository.observeMonthSummary(month)
                ) { ripples, summary ->
                    Triple(month, ripples, summary)
                }
            }.collectLatest { (month, ripples, summary) ->
                val atlas = computeMonthlyBodyReliefAtlas(ripples, catalogRepository)
                _uiState.update { state ->
                    val keptDate = state.selectedJournalDate?.takeIf { YearMonth.from(it) == month }
                    state.copy(
                        displayedMonth = month,
                        selectedJournalDate = keptDate,
                        monthRipples = ripples,
                        monthSummary = summary,
                        reliefAtlas = atlas
                    )
                }
            }
        }
    }

    /**
     * Syncs displayed month when the wall/simulated clock advances into a new month,
     * without clobbering manual previous/next month browsing within the same clock month.
     */
    fun refreshCurrentClockMonth() {
        val currentMonth = YearMonth.from(clockProvider.currentLocalDate())
        if (currentMonth != lastObservedClockMonth) {
            lastObservedClockMonth = currentMonth
            _selectedMonth.value = currentMonth
        }
    }

    fun navigateJournalMonth(deltaMonths: Long) {
        if (deltaMonths == 0L) return
        val next = _selectedMonth.value.plusMonths(deltaMonths)
        _selectedMonth.value = next
        _uiState.update { it.copy(selectedJournalDate = null) }
    }

    fun returnJournalToCurrentMonth() {
        val currentMonth = YearMonth.from(clockProvider.currentLocalDate())
        lastObservedClockMonth = currentMonth
        _selectedMonth.value = currentMonth
        _uiState.update { it.copy(selectedJournalDate = null) }
    }

    fun toggleSelectedJournalDate(date: LocalDate) {
        _uiState.update { state ->
            val next = if (state.selectedJournalDate == date) null else date
            state.copy(selectedJournalDate = next)
        }
    }

    fun clearSelectedJournalDate() {
        _uiState.update { it.copy(selectedJournalDate = null) }
    }

    fun selectSkin(skinId: SkinId) {
        _uiState.update { it.copy(selectedSkinId = skinId) }
        scope.launch {
            themePreferencesRepository.setSelectedSkin(skinId)
        }
    }

    fun previewActiveSkinChime(effectiveSkinId: SkinId) {
        val skin = BuiltInSkinRegistry.skinFor(effectiveSkinId)
        audioEngine.playSkinPreviewNote(effectiveSkinId, muted = false)
        hapticEngine.pulseInhaleWave(maxAmplitude = skin.sensory.hapticAmplitude, enabled = true)
    }

    fun setAutoNightSkin(enabled: Boolean) {
        _uiState.update { it.copy(autoNightSkinEnabled = enabled) }
        scope.launch {
            themePreferencesRepository.setAutoNightSkin(enabled)
        }
    }

    fun setVoiceGuidanceEnabled(enabled: Boolean) {
        _uiState.update { it.copy(voiceGuidanceEnabled = enabled) }
        scope.launch {
            themePreferencesRepository.setVoiceGuidanceEnabled(enabled)
        }
    }

    fun setPrepBufferEnabled(enabled: Boolean) {
        _uiState.update { it.copy(prepBufferEnabled = enabled) }
        scope.launch {
            themePreferencesRepository.setPrepBufferEnabled(enabled)
        }
    }

    fun openSharePreviewDialog() {
        _uiState.update { it.copy(showSharePreviewDialog = true) }
    }

    fun dismissSharePreviewDialog() {
        _uiState.update { it.copy(showSharePreviewDialog = false) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        updateReminderPrefs { it.copy(enabled = enabled) }
    }

    fun selectReminderInterval(minutes: Int) {
        if (minutes !in ReminderPreferences.SUPPORTED_INTERVALS) return
        updateReminderPrefs { it.copy(intervalMinutes = minutes) }
    }

    fun setLunchQuietEnabled(enabled: Boolean) {
        updateReminderPrefs { it.copy(lunchQuietEnabled = enabled) }
    }

    fun setNightQuietEnabled(enabled: Boolean) {
        updateReminderPrefs { it.copy(nightQuietEnabled = enabled) }
    }

    fun setWeekendSilentEnabled(enabled: Boolean) {
        updateReminderPrefs { it.copy(weekendSilent = enabled) }
    }

    fun setMuteTodayEnabled(mutedToday: Boolean) {
        val todayIso = clockProvider.currentDateIso()
        updateReminderPrefs {
            it.copy(muteTodayDateIso = if (mutedToday) todayIso else null)
        }
    }

    fun triggerReminderPreview() {
        SedentaryNotificationHelper.postGentleReminderNotification(appContext)
        _uiState.update { it.copy(showReminderInAppBanner = true) }
    }

    fun dismissReminderInAppBanner() {
        _uiState.update { it.copy(showReminderInAppBanner = false) }
    }

    fun setPersonalizedSplashAds(enabled: Boolean) {
        _uiState.update { it.copy(personalizedSplashEnabled = enabled) }
        scope.launch {
            splashAdLedgerRepository.setPersonalizedAdsEnabled(enabled)
        }
    }

    fun exportSanctuaryBackup() {
        scope.launch {
            val records = journalRepository.allRecords.first()
            val bookletDays = catalogRepository?.bookletCompletedDays?.first().orEmpty()
            val presets = catalogRepository?.savedPresets?.first().orEmpty()
            val cur = _uiState.value
            val snapshot = SongxiSanctuaryBackupSnapshot(
                exportedAtIso = clockProvider.currentDateIso(),
                selectedSkinId = cur.selectedSkinId,
                autoNightSkinEnabled = cur.autoNightSkinEnabled,
                voiceGuidanceEnabled = cur.voiceGuidanceEnabled,
                prepBufferEnabled = cur.prepBufferEnabled,
                reminderPreferences = cur.reminderPreferences,
                bookletCompletedDays = bookletDays,
                savedPresets = presets,
                journalRecords = records
            )
            val json = SongxiBackupCodec.encodeSnapshot(snapshot)
            runCatching {
                val backupDir = File(appContext.filesDir, "backups").apply { mkdirs() }
                File(backupDir, "songxi_backup_latest.json").writeText(json, Charsets.UTF_8)
            }
            _uiState.update {
                it.copy(
                    latestBackupJson = json,
                    showBackupDialog = true,
                    backupStatusMessage = "已生成本地归档（含 ${records.size} 息手札与 ${presets.size} 道常备方）"
                )
            }
        }
    }

    fun openRestoreBackupDialog() {
        scope.launch {
            val cachedJson = runCatching {
                val file = File(appContext.filesDir, "backups/songxi_backup_latest.json")
                if (file.isFile) file.readText(Charsets.UTF_8) else ""
            }.getOrDefault("")
            _uiState.update {
                it.copy(
                    latestBackupJson = it.latestBackupJson.ifBlank { cachedJson },
                    showBackupDialog = true
                )
            }
        }
    }

    fun dismissBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = false) }
    }

    fun restoreSanctuaryFromJson(rawJson: String) {
        val decoded = SongxiBackupCodec.decodeSnapshot(rawJson)
        decoded.fold(
            onSuccess = { snapshot ->
                scope.launch {
                    journalRepository.restoreSessions(snapshot.journalRecords)
                    catalogRepository?.restoreBookletAndPresets(
                        bookletProgress = snapshot.bookletCompletedDays,
                        presets = snapshot.savedPresets
                    )
                    themePreferencesRepository.setSelectedSkin(snapshot.selectedSkinId)
                    themePreferencesRepository.setAutoNightSkin(snapshot.autoNightSkinEnabled)
                    themePreferencesRepository.setVoiceGuidanceEnabled(snapshot.voiceGuidanceEnabled)
                    themePreferencesRepository.setPrepBufferEnabled(snapshot.prepBufferEnabled)
                    reminderPreferencesRepository.updatePreferences(snapshot.reminderPreferences)
                    SedentaryReminderScheduler.syncSchedule(appContext, snapshot.reminderPreferences)
                    _uiState.update {
                        it.copy(
                            showBackupDialog = false,
                            backupStatusMessage = "已恢复 ${snapshot.journalRecords.size} 息手札记录与画境偏好"
                        )
                    }
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        backupStatusMessage = err.message ?: "归档内容无法解析，请核对格式。"
                    )
                }
            }
        )
    }

    fun resetTransientDialogs() {
        val currentMonth = YearMonth.from(clockProvider.currentLocalDate())
        lastObservedClockMonth = currentMonth
        _selectedMonth.value = currentMonth
        _uiState.update {
            it.copy(
                displayedMonth = currentMonth,
                selectedJournalDate = null,
                showSharePreviewDialog = false,
                showReminderInAppBanner = false,
                showBackupDialog = false,
                backupStatusMessage = null
            )
        }
    }

    private fun updateReminderPrefs(transform: (ReminderPreferences) -> ReminderPreferences) {
        val updated = transform(_uiState.value.reminderPreferences)
        _uiState.update { it.copy(reminderPreferences = updated) }
        SedentaryReminderScheduler.syncSchedule(appContext, updated)
        scope.launch {
            reminderPreferencesRepository.updatePreferences(updated)
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        fun provideFactory(
            appContext: Context,
            themePreferencesRepository: ThemePreferencesRepository,
            journalRepository: PracticeJournalRepository,
            reminderPreferencesRepository: ReminderPreferencesRepository,
            splashAdLedgerRepository: SplashAdLedgerRepository,
            audioEngine: HarmonicAudioEngine,
            hapticEngine: BreathingHapticEngine,
            catalogRepository: StretchCatalogRepository? = null,
            clockProvider: ClockProvider = SystemClockProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SanctuaryViewModel(
                    appContext = appContext.applicationContext,
                    themePreferencesRepository = themePreferencesRepository,
                    journalRepository = journalRepository,
                    reminderPreferencesRepository = reminderPreferencesRepository,
                    splashAdLedgerRepository = splashAdLedgerRepository,
                    audioEngine = audioEngine,
                    hapticEngine = hapticEngine,
                    catalogRepository = catalogRepository,
                    clockProvider = clockProvider
                ) as T
            }
        }
    }
}
