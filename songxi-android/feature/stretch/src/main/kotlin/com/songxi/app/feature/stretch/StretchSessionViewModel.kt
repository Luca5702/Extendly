package com.songxi.app.feature.stretch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.songxi.app.core.data.PracticeJournalRepository
import com.songxi.app.core.data.StretchCatalogRepository
import com.songxi.app.core.data.ThemePreferencesRepository
import com.songxi.app.core.designsystem.BreathPhaseState
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.designsystem.resolveBreathPhase
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BreathRhythm
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PoseTimelineGuidance
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SomaticFeeling
import com.songxi.app.core.model.SoundProfileId
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine
import com.songxi.app.core.model.SystemClockProvider
import com.songxi.app.core.sensory.BreathingHapticEngine
import com.songxi.app.core.sensory.FaceDownSensorMonitor
import com.songxi.app.core.sensory.HarmonicAudioEngine
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveBookletContext(
    val bookletId: String,
    val dayIndex: Int
)

sealed interface StretchSessionStage {
    data object Idle : StretchSessionStage

    data class Playing(
        val sessionId: String,
        val routine: StretchRoutine,
        val skinId: SkinId,
        val bookletContext: ActiveBookletContext? = null,
        val currentPoseIndex: Int = 0,
        val poseElapsedMs: Long = 0L,
        val breathCycleElapsedMs: Long = 0L,
        val isManuallyPaused: Boolean = false,
        val isFaceDownPaused: Boolean = false,
        val isEyesFreeMode: Boolean = false,
        val showBiomechanicalGuide: Boolean = true,
        val isClinicalGuideOpen: Boolean = false,
        val soundProfile: SoundProfileId = SoundProfileId.defaultForSkin(skinId),
        val lastAudibleProfile: SoundProfileId = SoundProfileId.defaultForSkin(skinId)
    ) : StretchSessionStage {
        val isEffectivelyPaused: Boolean
            get() = isManuallyPaused || isFaceDownPaused || isClinicalGuideOpen

        val currentPose: StretchPose
            get() = routine.poses[currentPoseIndex.coerceIn(0, routine.poses.lastIndex)]

        val poseDurationMs: Long
            get() = currentPose.durationSeconds.coerceAtLeast(1) * 1_000L

        val remainingPoseSeconds: Int
            get() = (((poseDurationMs - poseElapsedMs).coerceAtLeast(0L) + 999L) / 1_000L).toInt()

        val timelineGuidance: PoseTimelineGuidance
            get() = currentPose.resolveTimelineGuidance(
                elapsedMs = poseElapsedMs,
                asymmetry = routine.asymmetry
            )

        val isSecondHalf: Boolean
            get() = timelineGuidance.isSecondSegment

        val breathPhase: BreathPhaseState
            get() = resolveBreathPhase(
                elapsedMs = breathCycleElapsedMs,
                inhaleDurationMs = BreathRhythm.INHALE_DURATION_MS.toInt(),
                exhaleDurationMs = BreathRhythm.EXHALE_DURATION_MS.toInt()
            )
    }

    data class Completed(
        val sessionId: String,
        val routine: StretchRoutine,
        val skinId: SkinId,
        val bookletContext: ActiveBookletContext? = null,
        val selectedFeeling: SomaticFeeling? = null
    ) : StretchSessionStage
}

data class StretchUiState(
    val selectedScene: PostureScene = PostureScene.DESK_SEATED,
    val onboardingSeen: Boolean = false,
    val selectedBodyRegions: List<BodyRegion> = listOf(
        BodyRegion.POSTERIOR_NECK,
        BodyRegion.TRAPEZIUS_SHOULDER
    ),
    val selectedDurationMinutes: Int = 4,
    val selectedAsymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED,
    val bookletCompletedDays: Map<String, Set<Int>> = emptyMap(),
    val sessionStage: StretchSessionStage = StretchSessionStage.Idle
)

class StretchSessionViewModel(
    val catalogRepository: StretchCatalogRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val journalRepository: PracticeJournalRepository,
    private val audioEngine: HarmonicAudioEngine,
    private val hapticEngine: BreathingHapticEngine,
    private val faceDownMonitor: FaceDownSensorMonitor,
    val clockProvider: ClockProvider = SystemClockProvider,
    initialOnboardingSeen: Boolean = false,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val sessionSequence = AtomicLong(1L)
    private val committedSessionIds = HashSet<String>()

    private val _uiState = MutableStateFlow(
        StretchUiState(onboardingSeen = initialOnboardingSeen)
    )
    val uiState: StateFlow<StretchUiState> = _uiState.asStateFlow()

    val hasActiveOrPausedSession: Boolean
        get() = _uiState.value.sessionStage is StretchSessionStage.Playing

    init {
        scope.launch {
            themePreferencesRepository.onboardingSeen.collectLatest { seen ->
                _uiState.update { state -> state.copy(onboardingSeen = seen) }
            }
        }
        scope.launch {
            catalogRepository.bookletCompletedDays.collectLatest { progress ->
                _uiState.update { state -> state.copy(bookletCompletedDays = progress) }
            }
        }
        scope.launch {
            faceDownMonitor.isFaceDown.collectLatest { faceDown ->
                val currentStage = _uiState.value.sessionStage
                if (currentStage is StretchSessionStage.Playing) {
                    if (faceDown && !currentStage.isFaceDownPaused) {
                        hapticEngine.cancel()
                        _uiState.update { state ->
                            val playing = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
                            state.copy(sessionStage = playing.copy(isFaceDownPaused = true))
                        }
                    } else if (!faceDown && currentStage.isFaceDownPaused) {
                        _uiState.update { state ->
                            val playing = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
                            state.copy(sessionStage = playing.copy(isFaceDownPaused = false))
                        }
                    }
                }
            }
        }
    }

    fun selectScene(scene: PostureScene) {
        _uiState.update { it.copy(selectedScene = scene) }
    }

    fun dismissOnboarding() {
        if (_uiState.value.onboardingSeen) return
        _uiState.update { it.copy(onboardingSeen = true) }
        scope.launch {
            themePreferencesRepository.markOnboardingSeen(true)
        }
    }

    fun cycleSkin() {
        scope.launch {
            val current = themePreferencesRepository.selectedSkinId.first()
            val all = SkinId.entries
            val next = all[(all.indexOf(current) + 1) % all.size]
            themePreferencesRepository.setSelectedSkin(next)
        }
    }

    fun toggleBodyRegion(region: BodyRegion) {
        _uiState.update { state ->
            state.copy(
                selectedBodyRegions = toggleBodyRegionSelection(
                    current = state.selectedBodyRegions,
                    region = region,
                    maxSelected = MAX_SELECTED_BODY_REGIONS
                )
            )
        }
    }

    fun selectCustomDuration(minutes: Int) {
        val normalized = when (minutes) {
            2, 4, 6 -> minutes
            else -> 4
        }
        _uiState.update { it.copy(selectedDurationMinutes = normalized) }
    }

    fun selectLateralAsymmetry(asymmetry: LateralAsymmetry) {
        _uiState.update { it.copy(selectedAsymmetry = asymmetry) }
    }

    fun composeAndStartCustomRoutine(skinId: SkinId) {
        val state = _uiState.value
        val composed = catalogRepository.composeCustomRoutine(
            regions = state.selectedBodyRegions.toSet(),
            targetMinutes = state.selectedDurationMinutes,
            asymmetry = state.selectedAsymmetry
        )
        startRoutine(routine = composed, skinId = skinId, bookletContext = null)
    }

    fun startBooklet(bookletIndex: Int, skinId: SkinId) {
        val booklets = catalogRepository.curatedBooklets
        if (bookletIndex !in booklets.indices) return
        val booklet = booklets[bookletIndex]
        val completed = _uiState.value.bookletCompletedDays[booklet.id].orEmpty()
        val nextDay = (0 until booklet.totalDays).firstOrNull { it !in completed } ?: 0
        val routine = booklet.routineForDay(nextDay)
        startRoutine(
            routine = routine,
            skinId = skinId,
            bookletContext = ActiveBookletContext(bookletId = booklet.id, dayIndex = nextDay)
        )
    }

    fun startReminderQuickStart(skinId: SkinId) {
        val quickRoutine = catalogRepository.reminderQuickStartRoutine()
        startRoutine(routine = quickRoutine, skinId = skinId, bookletContext = null)
    }

    fun startRoutine(
        routine: StretchRoutine,
        skinId: SkinId,
        bookletContext: ActiveBookletContext? = null
    ) {
        if (routine.poses.isEmpty()) return
        val defaultSound = SoundProfileId.defaultForSkin(skinId)
        val newSessionId = "sx_${clockProvider.nowEpochMillis()}_${sessionSequence.getAndIncrement()}"
        _uiState.update { state ->
            state.copy(
                sessionStage = StretchSessionStage.Playing(
                    sessionId = newSessionId,
                    routine = routine,
                    skinId = skinId,
                    bookletContext = bookletContext,
                    currentPoseIndex = 0,
                    poseElapsedMs = 0L,
                    breathCycleElapsedMs = 0L,
                    isManuallyPaused = false,
                    isFaceDownPaused = false,
                    isEyesFreeMode = false,
                    showBiomechanicalGuide = true,
                    isClinicalGuideOpen = false,
                    soundProfile = defaultSound,
                    lastAudibleProfile = defaultSound
                )
            )
        }
        emitBreathSensoryCue(skinId = skinId, soundProfile = defaultSound, isExhale = false)
    }

    fun onPlayerScreenEntered() {
        if (_uiState.value.sessionStage is StretchSessionStage.Playing) {
            faceDownMonitor.startMonitoring()
        }
    }

    fun onPlayerScreenExited() {
        faceDownMonitor.stopMonitoring()
    }

    fun advancePlaybackMillis(deltaMs: Long, activeSkinId: SkinId? = null) {
        if (deltaMs <= 0L) return
        val currentPlaying = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        if (currentPlaying.isEffectivelyPaused) return

        val effectiveSkin = activeSkinId ?: currentPlaying.skinId
        val prevBreathMs = currentPlaying.breathCycleElapsedMs
        val nextBreathMs = prevBreathMs + deltaMs
        val prevPhase = resolveBreathPhase(prevBreathMs)
        val nextPhase = resolveBreathPhase(nextBreathMs)
        val prevCycleIndex = prevBreathMs / BreathRhythm.CYCLE_DURATION_MS
        val nextCycleIndex = nextBreathMs / BreathRhythm.CYCLE_DURATION_MS

        val nextPoseElapsed = currentPlaying.poseElapsedMs + deltaMs
        if (nextPoseElapsed >= currentPlaying.poseDurationMs) {
            val nextIndex = currentPlaying.currentPoseIndex + 1
            if (nextIndex >= currentPlaying.routine.poses.size) {
                transitionToCompleted(currentPlaying, effectiveSkin)
                return
            } else {
                val updated = currentPlaying.copy(
                    skinId = effectiveSkin,
                    currentPoseIndex = nextIndex,
                    poseElapsedMs = 0L,
                    breathCycleElapsedMs = nextBreathMs,
                    isClinicalGuideOpen = false
                )
                _uiState.update { it.copy(sessionStage = updated) }
                val muted = updated.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
                audioEngine.playPoseTransitionChime(effectiveSkin, updated.soundProfile, muted = muted)
                if (prevPhase.isExhale != nextPhase.isExhale || prevCycleIndex != nextCycleIndex) {
                    emitBreathSensoryCue(effectiveSkin, updated.soundProfile, nextPhase.isExhale)
                }
                return
            }
        }

        val updated = currentPlaying.copy(
            skinId = effectiveSkin,
            poseElapsedMs = nextPoseElapsed,
            breathCycleElapsedMs = nextBreathMs
        )
        _uiState.update { it.copy(sessionStage = updated) }

        if (prevPhase.isExhale != nextPhase.isExhale || prevCycleIndex != nextCycleIndex) {
            emitBreathSensoryCue(
                skinId = effectiveSkin,
                soundProfile = updated.soundProfile,
                isExhale = nextPhase.isExhale
            )
        }
    }

    fun togglePause() {
        val playing = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        if (playing.isFaceDownPaused) {
            faceDownMonitor.simulateFaceDownForTest(false)
            _uiState.update { state ->
                val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
                state.copy(sessionStage = cur.copy(isFaceDownPaused = false, isManuallyPaused = false))
            }
            return
        }
        val nextPaused = !playing.isManuallyPaused
        if (nextPaused) {
            hapticEngine.cancel()
        }
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(isManuallyPaused = nextPaused, isClinicalGuideOpen = false))
        }
    }

    fun resumeFromFlipBanner() {
        faceDownMonitor.simulateFaceDownForTest(false)
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(isFaceDownPaused = false, isManuallyPaused = false))
        }
    }

    fun toggleEyesFreeMode() {
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(isEyesFreeMode = !cur.isEyesFreeMode))
        }
    }

    fun toggleBiomechanicalGuide() {
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(showBiomechanicalGuide = !cur.showBiomechanicalGuide))
        }
    }

    fun openClinicalGuide() {
        hapticEngine.cancel()
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(isClinicalGuideOpen = true))
        }
    }

    fun closeClinicalGuide() {
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = cur.copy(isClinicalGuideOpen = false))
        }
    }

    fun cycleAudibleSoundProfile(activeSkinId: SkinId? = null) {
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        val skinId = activeSkinId ?: cur.skinId
        val nextSound = nextAudibleSoundProfile(cur.soundProfile, cur.lastAudibleProfile)
        _uiState.update { state ->
            val latest = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(
                sessionStage = latest.copy(
                    soundProfile = nextSound,
                    lastAudibleProfile = nextSound
                )
            )
        }
        audioEngine.playPoseTransitionChime(skinId, nextSound, muted = false)
    }

    fun toggleSilentHapticMode(activeSkinId: SkinId? = null) {
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        val skinId = activeSkinId ?: cur.skinId
        val nextSound = if (cur.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY) {
            cur.lastAudibleProfile
        } else {
            SoundProfileId.SILENT_HAPTIC_ONLY
        }
        if (nextSound == SoundProfileId.SILENT_HAPTIC_ONLY) {
            audioEngine.release()
        }
        _uiState.update { state ->
            val latest = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = latest.copy(soundProfile = nextSound))
        }
        val amp = BuiltInSkinRegistry.skinFor(skinId).sensory.hapticAmplitude
        hapticEngine.pulseInhaleWave(maxAmplitude = amp, enabled = true)
    }

    fun previousPose() {
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            val clampedIndex = (cur.currentPoseIndex - 1).coerceAtLeast(0)
            state.copy(
                sessionStage = cur.copy(
                    currentPoseIndex = clampedIndex,
                    poseElapsedMs = 0L,
                    breathCycleElapsedMs = 0L,
                    isClinicalGuideOpen = false
                )
            )
        }
    }

    fun nextPose(activeSkinId: SkinId? = null) {
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        val effectiveSkin = activeSkinId ?: cur.skinId
        val nextIndex = cur.currentPoseIndex + 1
        if (nextIndex >= cur.routine.poses.size) {
            transitionToCompleted(cur, effectiveSkin)
        } else {
            _uiState.update { state ->
                val latest = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
                state.copy(
                    sessionStage = latest.copy(
                        skinId = effectiveSkin,
                        currentPoseIndex = nextIndex,
                        poseElapsedMs = 0L,
                        breathCycleElapsedMs = 0L,
                        isClinicalGuideOpen = false
                    )
                )
            }
            val muted = cur.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
            audioEngine.playPoseTransitionChime(effectiveSkin, cur.soundProfile, muted = muted)
        }
    }

    fun closePlayerWithoutCompleting() {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        _uiState.update { state ->
            state.copy(sessionStage = StretchSessionStage.Idle)
        }
    }

    fun selectCompletionFeeling(feeling: SomaticFeeling) {
        _uiState.update { state ->
            val completed = state.sessionStage as? StretchSessionStage.Completed ?: return@update state
            val toggled = if (completed.selectedFeeling == feeling) null else feeling
            state.copy(sessionStage = completed.copy(selectedFeeling = toggled))
        }
    }

    fun finishCompletionAndSave(activeSkinId: SkinId? = null) {
        val completed = _uiState.value.sessionStage as? StretchSessionStage.Completed ?: return
        if (!committedSessionIds.add(completed.sessionId)) return

        val effectiveSkin = activeSkinId ?: completed.skinId
        val nowMs = clockProvider.nowEpochMillis()
        val dateIso = Instant.ofEpochMilli(nowMs)
            .atZone(clockProvider.currentZoneId())
            .toLocalDate()
            .toString()

        val coveredRegions = completed.routine.poses.flatMapTo(linkedSetOf()) { it.targetRegions }
        val record = PracticeSessionRecord(
            id = completed.sessionId,
            completedAtEpochMs = nowMs,
            dateIso = dateIso,
            routineId = completed.routine.id,
            routineTitle = completed.routine.title,
            durationSeconds = completed.routine.totalDurationSeconds,
            skinId = effectiveSkin,
            feeling = completed.selectedFeeling,
            targetRegions = coveredRegions,
            asymmetry = completed.routine.asymmetry
        )

        _uiState.update { it.copy(sessionStage = StretchSessionStage.Idle) }

        scope.launch {
            journalRepository.recordCompletedSession(record)
            completed.bookletContext?.let { bookletCtx ->
                catalogRepository.markBookletDayCompleted(bookletCtx.bookletId, bookletCtx.dayIndex)
            }
        }
    }

    fun resetSessionState(onboardingSeen: Boolean = false) {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        committedSessionIds.clear()
        _uiState.value = StretchUiState(onboardingSeen = onboardingSeen)
    }

    private fun transitionToCompleted(playing: StretchSessionStage.Playing, effectiveSkin: SkinId) {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        val muted = playing.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
        audioEngine.playPoseTransitionChime(effectiveSkin, playing.soundProfile, muted = muted)
        _uiState.update { state ->
            state.copy(
                sessionStage = StretchSessionStage.Completed(
                    sessionId = playing.sessionId,
                    routine = playing.routine,
                    skinId = effectiveSkin,
                    bookletContext = playing.bookletContext,
                    selectedFeeling = null
                )
            )
        }
    }

    private fun emitBreathSensoryCue(
        skinId: SkinId,
        soundProfile: SoundProfileId,
        isExhale: Boolean
    ) {
        val muted = soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
        val amplitude = BuiltInSkinRegistry.skinFor(skinId).sensory.hapticAmplitude
        if (isExhale) {
            audioEngine.playExhaleChord(skinId, soundProfile, muted = muted)
            hapticEngine.pulseExhaleWave(maxAmplitude = amplitude, enabled = true)
        } else {
            audioEngine.playInhaleChord(skinId, soundProfile, muted = muted)
            hapticEngine.pulseInhaleWave(maxAmplitude = amplitude, enabled = true)
        }
    }

    override fun onCleared() {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            catalogRepository: StretchCatalogRepository,
            themePreferencesRepository: ThemePreferencesRepository,
            journalRepository: PracticeJournalRepository,
            audioEngine: HarmonicAudioEngine,
            hapticEngine: BreathingHapticEngine,
            faceDownMonitor: FaceDownSensorMonitor,
            clockProvider: ClockProvider = SystemClockProvider,
            initialOnboardingSeen: Boolean = false
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StretchSessionViewModel(
                    catalogRepository = catalogRepository,
                    themePreferencesRepository = themePreferencesRepository,
                    journalRepository = journalRepository,
                    audioEngine = audioEngine,
                    hapticEngine = hapticEngine,
                    faceDownMonitor = faceDownMonitor,
                    clockProvider = clockProvider,
                    initialOnboardingSeen = initialOnboardingSeen
                ) as T
            }
        }
    }
}
