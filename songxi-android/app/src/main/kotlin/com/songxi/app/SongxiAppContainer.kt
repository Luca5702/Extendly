package com.songxi.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.songxi.app.core.data.DataStorePracticeJournalRepository
import com.songxi.app.core.data.DataStoreReminderPreferencesRepository
import com.songxi.app.core.data.DataStoreSplashAdLedgerRepository
import com.songxi.app.core.data.DataStoreStretchCatalogRepository
import com.songxi.app.core.data.DataStoreThemePreferencesRepository
import com.songxi.app.core.data.MonthlyRippleSummary
import com.songxi.app.core.data.PracticeJournalRepository
import com.songxi.app.core.data.ReminderPreferencesRepository
import com.songxi.app.core.data.SongxiDataStoreFactory
import com.songxi.app.core.data.SplashAdLedgerRepository
import com.songxi.app.core.data.StretchCatalogRepository
import com.songxi.app.core.data.ThemePreferencesRepository
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine
import com.songxi.app.core.sensory.AndroidBreathingHapticEngine
import com.songxi.app.core.sensory.AndroidFaceDownSensorMonitor
import com.songxi.app.core.sensory.AndroidHarmonicAudioEngine
import com.songxi.app.core.sensory.BreathingHapticEngine
import com.songxi.app.core.sensory.FaceDownSensorMonitor
import com.songxi.app.core.sensory.HarmonicAudioEngine
import com.songxi.app.feature.sanctuary.SedentaryReminderScheduler
import com.songxi.app.feature.splashad.CuratedHouseSplashAdProvider
import com.songxi.app.feature.splashad.SplashAdPolicyEngine
import com.songxi.app.feature.splashad.SplashAdProvider
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SongxiMutableClockProvider : ClockProvider {
    @Volatile
    private var seedEpochMs: Long? = null

    @Volatile
    private var seedSystemTimeMs: Long = 0L

    fun applySimulatedEpoch(epochMs: Long?) {
        if (epochMs != null && epochMs > 0L) {
            seedEpochMs = epochMs
            seedSystemTimeMs = System.currentTimeMillis()
        }
    }

    fun clearSimulatedEpoch() {
        seedEpochMs = null
    }

    override fun nowEpochMillis(): Long {
        val seed = seedEpochMs ?: return System.currentTimeMillis()
        val delta = (System.currentTimeMillis() - seedSystemTimeMs).coerceAtLeast(0L)
        return seed + delta
    }
}

/**
 * Write-through instant StateFlow decorators backed by DataStore<Preferences>.
 * Guarantees 0ms main-thread state visibility across Activity.recreate() and rapid taps
 * while persisting durably to disk in the background.
 */
private class WriteThroughThemePreferencesRepository(
    private val backing: DataStoreThemePreferencesRepository,
    private val ioScope: CoroutineScope
) : ThemePreferencesRepository {
    private val _selectedSkinId = MutableStateFlow(runBlocking { backing.selectedSkinId.first() })
    private val _autoNight = MutableStateFlow(runBlocking { backing.autoNightSkinEnabled.first() })
    private val _onboardingSeen = MutableStateFlow(runBlocking { backing.onboardingSeen.first() })

    override val selectedSkinId: Flow<SkinId> = _selectedSkinId
    override val autoNightSkinEnabled: Flow<Boolean> = _autoNight
    override val onboardingSeen: Flow<Boolean> = _onboardingSeen

    override suspend fun setSelectedSkin(skinId: SkinId) {
        _selectedSkinId.value = skinId
        ioScope.launch { backing.setSelectedSkin(skinId) }
    }

    override suspend fun setAutoNightSkin(enabled: Boolean) {
        _autoNight.value = enabled
        ioScope.launch { backing.setAutoNightSkin(enabled) }
    }

    override suspend fun markOnboardingSeen(seen: Boolean) {
        _onboardingSeen.value = seen
        ioScope.launch { backing.markOnboardingSeen(seen) }
    }

    override suspend fun resetAll() {
        resetMemory()
        backing.resetAll()
    }

    fun resetMemory() {
        _selectedSkinId.value = SkinId.DEFAULT
        _autoNight.value = true
        _onboardingSeen.value = false
    }
}

private class WriteThroughStretchCatalogRepository(
    private val backing: DataStoreStretchCatalogRepository,
    private val ioScope: CoroutineScope
) : StretchCatalogRepository {
    private val _completedDays = MutableStateFlow(runBlocking { backing.bookletCompletedDays.first() })

    override val allPoses: List<StretchPose> get() = backing.allPoses
    override val quickReliefRoutines: List<StretchRoutine> get() = backing.quickReliefRoutines
    override val curatedBooklets: List<BookletSeries> get() = backing.curatedBooklets
    override val bookletCompletedDays: Flow<Map<String, Set<Int>>> = _completedDays

    override fun heroRoutineFor(scene: PostureScene): StretchRoutine = backing.heroRoutineFor(scene)
    override fun reminderQuickStartRoutine(): StretchRoutine = backing.reminderQuickStartRoutine()
    override fun findRoutineById(routineId: String): StretchRoutine? = backing.findRoutineById(routineId)
    override fun composeCustomRoutine(
        regions: Set<BodyRegion>,
        targetMinutes: Int,
        asymmetry: com.songxi.app.core.model.LateralAsymmetry
    ): StretchRoutine = backing.composeCustomRoutine(regions, targetMinutes, asymmetry)

    override suspend fun markBookletDayCompleted(bookletId: String, dayIndex: Int) {
        _completedDays.update { cur ->
            val updatedSet = (cur[bookletId].orEmpty() + dayIndex)
            cur + (bookletId to updatedSet)
        }
        ioScope.launch { backing.markBookletDayCompleted(bookletId, dayIndex) }
    }

    override suspend fun resetAll() {
        resetMemory()
        backing.resetAll()
    }

    fun resetMemory() {
        _completedDays.value = emptyMap()
    }
}

private class WriteThroughPracticeJournalRepository(
    private val backing: DataStorePracticeJournalRepository,
    private val ioScope: CoroutineScope
) : PracticeJournalRepository {
    private val _records = MutableStateFlow(runBlocking { backing.allRecords.first() })

    override val allRecords: Flow<List<PracticeSessionRecord>> = _records

    override fun observeMonthRipples(month: YearMonth): Flow<Map<LocalDate, List<PracticeSessionRecord>>> =
        _records.map { records ->
            val grouped = linkedMapOf<LocalDate, MutableList<PracticeSessionRecord>>()
            for (record in records) {
                val date = runCatching { LocalDate.parse(record.dateIso) }.getOrNull() ?: continue
                if (YearMonth.from(date) == month) {
                    grouped.getOrPut(date) { mutableListOf() }.add(record)
                }
            }
            grouped.mapValues { (_, list) -> list.toList() }
        }

    override fun observeMonthSummary(month: YearMonth): Flow<MonthlyRippleSummary> =
        observeMonthRipples(month).map { monthMap ->
            val flat = monthMap.values.flatten()
            MonthlyRippleSummary(
                month = month,
                totalSessions = flat.size,
                totalDurationSeconds = flat.sumOf { it.durationSeconds },
                practicedDaysCount = monthMap.keys.size
            )
        }

    override suspend fun recordCompletedSession(record: PracticeSessionRecord) {
        _records.update { current ->
            val mutable = current.toMutableList()
            val idx = mutable.indexOfFirst { it.id == record.id }
            if (idx >= 0) {
                mutable[idx] = record
            } else {
                mutable.add(record)
            }
            mutable.sortedWith(
                compareByDescending<PracticeSessionRecord> { it.completedAtEpochMs }
                    .thenByDescending { it.id }
            )
        }
        ioScope.launch { backing.recordCompletedSession(record) }
    }

    override suspend fun clearAll() {
        resetMemory()
        backing.clearAll()
    }

    fun resetMemory() {
        _records.value = emptyList()
    }
}

private class WriteThroughReminderPreferencesRepository(
    private val backing: DataStoreReminderPreferencesRepository,
    private val ioScope: CoroutineScope
) : ReminderPreferencesRepository {
    private val _prefs = MutableStateFlow(runBlocking { backing.preferences.first() })
    private val _dailyCounts = ConcurrentHashMap<String, MutableStateFlow<Int>>()

    override val preferences: Flow<ReminderPreferences> = _prefs

    override suspend fun updatePreferences(preferences: ReminderPreferences) {
        _prefs.value = preferences
        ioScope.launch { backing.updatePreferences(preferences) }
    }

    override fun notificationsSentOn(dateIso: String): Flow<Int> =
        _dailyCounts.computeIfAbsent(dateIso) { key ->
            MutableStateFlow(runBlocking { backing.notificationsSentOn(key).first() })
        }

    override suspend fun recordNotificationSent(dateIso: String) {
        val flow = _dailyCounts.computeIfAbsent(dateIso) { key ->
            MutableStateFlow(runBlocking { backing.notificationsSentOn(key).first() })
        }
        flow.update { it + 1 }
        ioScope.launch { backing.recordNotificationSent(dateIso) }
    }

    override suspend fun resetAll() {
        resetMemory()
        backing.resetAll()
    }

    fun resetMemory() {
        _prefs.value = ReminderPreferences()
        _dailyCounts.values.forEach { it.value = 0 }
        _dailyCounts.clear()
    }
}

private class WriteThroughSplashAdLedgerRepository(
    private val backing: DataStoreSplashAdLedgerRepository,
    private val ioScope: CoroutineScope
) : SplashAdLedgerRepository {
    private val _launches = MutableStateFlow(runBlocking { backing.totalAppLaunches.first() })
    private val _personalized = MutableStateFlow(runBlocking { backing.personalizedAdsEnabled.first() })
    private val _lastImpressionMs = MutableStateFlow(runBlocking { backing.lastImpressionEpochMs.first() })
    private val _skips = MutableStateFlow(runBlocking { backing.totalSkips.first() })
    private val _ctaClicks = MutableStateFlow(runBlocking { backing.totalCtaClicks.first() })
    private val _impressionsByDate = ConcurrentHashMap<String, MutableStateFlow<Int>>()

    override val totalAppLaunches: Flow<Int> = _launches
    override val personalizedAdsEnabled: Flow<Boolean> = _personalized
    override val lastImpressionEpochMs: Flow<Long?> = _lastImpressionMs
    override val totalSkips: Flow<Int> = _skips
    override val totalCtaClicks: Flow<Int> = _ctaClicks

    override fun impressionsOn(dateIso: String): Flow<Int> =
        _impressionsByDate.computeIfAbsent(dateIso) { key ->
            MutableStateFlow(runBlocking { backing.impressionsOn(key).first() })
        }

    override suspend fun incrementAppLaunch(): Int {
        var next = 0
        _launches.update { cur ->
            next = cur + 1
            next
        }
        ioScope.launch { backing.incrementAppLaunch() }
        return next
    }

    override suspend fun recordImpression(creativeId: String, epochMs: Long, dateIso: String) {
        _lastImpressionMs.value = epochMs
        val dayFlow = _impressionsByDate.computeIfAbsent(dateIso) { key ->
            MutableStateFlow(runBlocking { backing.impressionsOn(key).first() })
        }
        dayFlow.update { it + 1 }
        ioScope.launch { backing.recordImpression(creativeId, epochMs, dateIso) }
    }

    override suspend fun recordSkip(creativeId: String, elapsedMs: Long) {
        _skips.update { it + 1 }
        ioScope.launch { backing.recordSkip(creativeId, elapsedMs) }
    }

    override suspend fun recordCtaClick(creativeId: String) {
        _ctaClicks.update { it + 1 }
        ioScope.launch { backing.recordCtaClick(creativeId) }
    }

    override suspend fun setPersonalizedAdsEnabled(enabled: Boolean) {
        _personalized.value = enabled
        ioScope.launch { backing.setPersonalizedAdsEnabled(enabled) }
    }

    override suspend fun resetAll() {
        resetMemory()
        backing.resetAll()
    }

    fun resetMemory() {
        _launches.value = 0
        _personalized.value = false
        _lastImpressionMs.value = null
        _skips.value = 0
        _ctaClicks.value = 0
        _impressionsByDate.values.forEach { it.value = 0 }
        _impressionsByDate.clear()
    }
}

class SongxiAppContainer(context: Context) {
    private val appContext: Context = context.applicationContext
    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val clockProvider: SongxiMutableClockProvider = SongxiMutableClockProvider()

    @Volatile
    var firstLaunchEpochMs: Long? = null

    private val dataStore: DataStore<Preferences> =
        SongxiDataStoreFactory.getOrCreate(appContext, scope = ioScope)

    private val writeThroughThemeRepo = WriteThroughThemePreferencesRepository(
        DataStoreThemePreferencesRepository(dataStore),
        ioScope
    )
    private val writeThroughCatalogRepo = WriteThroughStretchCatalogRepository(
        DataStoreStretchCatalogRepository(dataStore),
        ioScope
    )
    private val writeThroughJournalRepo = WriteThroughPracticeJournalRepository(
        DataStorePracticeJournalRepository(dataStore),
        ioScope
    )
    private val writeThroughReminderRepo = WriteThroughReminderPreferencesRepository(
        DataStoreReminderPreferencesRepository(dataStore),
        ioScope
    )
    private val writeThroughSplashLedgerRepo = WriteThroughSplashAdLedgerRepository(
        DataStoreSplashAdLedgerRepository(dataStore),
        ioScope
    )

    val themePreferencesRepository: ThemePreferencesRepository get() = writeThroughThemeRepo
    val stretchCatalogRepository: StretchCatalogRepository get() = writeThroughCatalogRepo
    val practiceJournalRepository: PracticeJournalRepository get() = writeThroughJournalRepo
    val reminderPreferencesRepository: ReminderPreferencesRepository get() = writeThroughReminderRepo
    val splashAdLedgerRepository: SplashAdLedgerRepository get() = writeThroughSplashLedgerRepo

    val audioEngine: HarmonicAudioEngine = AndroidHarmonicAudioEngine()
    val hapticEngine: BreathingHapticEngine = AndroidBreathingHapticEngine(appContext)
    val faceDownSensorMonitor: FaceDownSensorMonitor = AndroidFaceDownSensorMonitor(appContext)

    val splashAdProvider: SplashAdProvider = CuratedHouseSplashAdProvider(
        ledgerRepository = splashAdLedgerRepository,
        clockProvider = clockProvider
    )

    val splashAdPolicyEngine: SplashAdPolicyEngine = SplashAdPolicyEngine(
        ledgerRepository = splashAdLedgerRepository,
        adProvider = splashAdProvider,
        clockProvider = clockProvider
    )

    fun resetAllStateSynchronously() {
        firstLaunchEpochMs = null
        audioEngine.release()
        faceDownSensorMonitor.stopMonitoring()
        hapticEngine.cancel()
        writeThroughThemeRepo.resetMemory()
        writeThroughCatalogRepo.resetMemory()
        writeThroughJournalRepo.resetMemory()
        writeThroughReminderRepo.resetMemory()
        writeThroughSplashLedgerRepo.resetMemory()
        ioScope.launch {
            SongxiDataStoreFactory.clearEntireDataStore(dataStore)
        }
        SedentaryReminderScheduler.syncSchedule(appContext, ReminderPreferences())
    }
}
