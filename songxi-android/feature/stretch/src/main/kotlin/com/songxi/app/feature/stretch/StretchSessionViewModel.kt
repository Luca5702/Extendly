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
import com.songxi.app.core.model.PoseTimelineStage
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SavedPrescriptionPreset
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SomaticFeeling
import com.songxi.app.core.model.SoundProfileId
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine
import com.songxi.app.core.model.SystemClockProvider
import com.songxi.app.core.sensory.BreathingHapticEngine
import com.songxi.app.core.sensory.FaceDownSensorMonitor
import com.songxi.app.core.sensory.HarmonicAudioEngine
import com.songxi.app.core.sensory.NoOpVoiceCueEngine
import com.songxi.app.core.sensory.VoiceCueEngine
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
        val prepRemainingMs: Long = 0L,
        val isManuallyPaused: Boolean = false,
        val isFaceDownPaused: Boolean = false,
        val isEyesFreeMode: Boolean = false,
        val voiceGuidanceEnabled: Boolean = false,
        val showBiomechanicalGuide: Boolean = true,
        val isClinicalGuideOpen: Boolean = false,
        val soundProfile: SoundProfileId = SoundProfileId.defaultForSkin(skinId),
        val lastAudibleProfile: SoundProfileId = SoundProfileId.defaultForSkin(skinId)
    ) : StretchSessionStage {
        val isEffectivelyPaused: Boolean
            get() = isManuallyPaused || isFaceDownPaused || isClinicalGuideOpen

        val isInPrepBuffer: Boolean
            get() = prepRemainingMs > 0L

        val prepDisplaySeconds: Int
            get() = (((prepRemainingMs).coerceAtLeast(0L) + 999L) / 1_000L).toInt()

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
    val savedPresets: List<SavedPrescriptionPreset> = emptyList(),
    val inspectedBookletIndex: Int? = null,
    val voiceGuidanceEnabled: Boolean = false,
    val prepBufferEnabled: Boolean = false,
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
    private val voiceEngine: VoiceCueEngine = NoOpVoiceCueEngine,
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
            themePreferencesRepository.voiceGuidanceEnabled.collectLatest { voiceOn ->
                _uiState.update { state ->
                    val updatedStage = (state.sessionStage as? StretchSessionStage.Playing)
                        ?.copy(voiceGuidanceEnabled = voiceOn)
                        ?: state.sessionStage
                    state.copy(voiceGuidanceEnabled = voiceOn, sessionStage = updatedStage)
                }
            }
        }
        scope.launch {
            themePreferencesRepository.prepBufferEnabled.collectLatest { prepOn ->
                _uiState.update { state -> state.copy(prepBufferEnabled = prepOn) }
            }
        }
        scope.launch {
            catalogRepository.bookletCompletedDays.collectLatest { progress ->
                _uiState.update { state -> state.copy(bookletCompletedDays = progress) }
            }
        }
        scope.launch {
            catalogRepository.savedPresets.collectLatest { presets ->
                _uiState.update { state -> state.copy(savedPresets = presets) }
            }
        }
        scope.launch {
            faceDownMonitor.isFaceDown.collectLatest { faceDown ->
                val currentStage = _uiState.value.sessionStage
                if (currentStage is StretchSessionStage.Playing) {
                    if (faceDown && !currentStage.isFaceDownPaused) {
                        hapticEngine.cancel()
                        voiceEngine.stop()
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

    fun saveCurrentCustomPreset() {
        val state = _uiState.value
        val regions = state.selectedBodyRegions.toSet().ifEmpty {
            setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER)
        }
        val duration = state.selectedDurationMinutes
        val asymmetry = state.selectedAsymmetry
        val preset = SavedPrescriptionPreset(
            id = SavedPrescriptionPreset.buildPresetId(regions, duration, asymmetry),
            title = SavedPrescriptionPreset.buildDisplayTitle(regions, duration, asymmetry),
            regions = regions,
            durationMinutes = duration,
            asymmetry = asymmetry,
            createdAtEpochMs = clockProvider.nowEpochMillis()
        )
        scope.launch {
            catalogRepository.saveCustomPreset(preset)
        }
    }

    fun deleteSavedPreset(presetId: String) {
        scope.launch {
            catalogRepository.deleteCustomPreset(presetId)
        }
    }

    fun startSavedPreset(preset: SavedPrescriptionPreset, skinId: SkinId) {
        val routine = catalogRepository.composeCustomRoutine(
            regions = preset.regions,
            targetMinutes = preset.durationMinutes,
            asymmetry = preset.asymmetry
        )
        startRoutine(routine = routine, skinId = skinId, bookletContext = null)
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

    fun openBookletDetail(bookletIndex: Int) {
        if (bookletIndex !in catalogRepository.curatedBooklets.indices) return
        _uiState.update { it.copy(inspectedBookletIndex = bookletIndex) }
    }

    fun closeBookletDetail() {
        _uiState.update { it.copy(inspectedBookletIndex = null) }
    }

    fun startBookletDay(bookletIndex: Int, dayIndex: Int, skinId: SkinId) {
        val booklets = catalogRepository.curatedBooklets
        if (bookletIndex !in booklets.indices) return
        val booklet = booklets[bookletIndex]
        val clampedDay = dayIndex.coerceIn(0, booklet.totalDays - 1)
        _uiState.update { it.copy(inspectedBookletIndex = null) }
        val routine = booklet.routineForDay(clampedDay)
        startRoutine(
            routine = routine,
            skinId = skinId,
            bookletContext = ActiveBookletContext(bookletId = booklet.id, dayIndex = clampedDay)
        )
    }

    fun startBooklet(bookletIndex: Int, skinId: SkinId) {
        val booklets = catalogRepository.curatedBooklets
        if (bookletIndex !in booklets.indices) return
        val booklet = booklets[bookletIndex]
        val completed = _uiState.value.bookletCompletedDays[booklet.id].orEmpty()
        val nextDay = (0 until booklet.totalDays).firstOrNull { it !in completed } ?: 0
        startBookletDay(bookletIndex = bookletIndex, dayIndex = nextDay, skinId = skinId)
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
        val stateSnapshot = _uiState.value
        val defaultSound = SoundProfileId.defaultForSkin(skinId)
        val prepMs = if (stateSnapshot.prepBufferEnabled) PREP_BUFFER_DURATION_MS else 0L
        val voiceOn = stateSnapshot.voiceGuidanceEnabled
        val newSessionId = "sx_${clockProvider.nowEpochMillis()}_${sessionSequence.getAndIncrement()}"
        _uiState.update { state ->
            state.copy(
                inspectedBookletIndex = null,
                sessionStage = StretchSessionStage.Playing(
                    sessionId = newSessionId,
                    routine = routine,
                    skinId = skinId,
                    bookletContext = bookletContext,
                    currentPoseIndex = 0,
                    poseElapsedMs = 0L,
                    breathCycleElapsedMs = 0L,
                    prepRemainingMs = prepMs,
                    isManuallyPaused = false,
                    isFaceDownPaused = false,
                    isEyesFreeMode = false,
                    voiceGuidanceEnabled = voiceOn,
                    showBiomechanicalGuide = true,
                    isClinicalGuideOpen = false,
                    soundProfile = defaultSound,
                    lastAudibleProfile = defaultSound
                )
            )
        }
        val muted = defaultSound == SoundProfileId.SILENT_HAPTIC_ONLY
        if (prepMs > 0L) {
            voiceEngine.speakCue(
                text = "入席安顿，放稳手机，自然沉落双肩。",
                enabled = voiceOn,
                muted = muted
            )
        } else {
            emitBreathSensoryCue(skinId = skinId, soundProfile = defaultSound, isExhale = false)
            val firstPose = routine.poses.first()
            voiceEngine.speakCue(
                text = "${firstPose.title}。${firstPose.clinical.setupCueCn}",
                enabled = voiceOn,
                muted = muted
            )
        }
    }

    fun skipPrepBuffer(activeSkinId: SkinId? = null) {
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        if (cur.prepRemainingMs <= 0L) return
        val skinId = activeSkinId ?: cur.skinId
        val updated = cur.copy(
            skinId = skinId,
            prepRemainingMs = 0L,
            poseElapsedMs = 0L,
            breathCycleElapsedMs = 0L
        )
        _uiState.update { it.copy(sessionStage = updated) }
        val muted = updated.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
        emitBreathSensoryCue(skinId = skinId, soundProfile = updated.soundProfile, isExhale = false)
        voiceEngine.speakCue(
            text = "${updated.currentPose.title}。${updated.currentPose.clinical.setupCueCn}",
            enabled = updated.voiceGuidanceEnabled,
            muted = muted
        )
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
        if (currentPlaying.prepRemainingMs > 0L) {
            val nextPrep = (currentPlaying.prepRemainingMs - deltaMs).coerceAtLeast(0L)
            val updated = currentPlaying.copy(
                skinId = effectiveSkin,
                prepRemainingMs = nextPrep,
                poseElapsedMs = 0L,
                breathCycleElapsedMs = 0L
            )
            _uiState.update { it.copy(sessionStage = updated) }
            if (nextPrep == 0L) {
                val muted = updated.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
                emitBreathSensoryCue(effectiveSkin, updated.soundProfile, isExhale = false)
                voiceEngine.speakCue(
                    text = "${updated.currentPose.title}。${updated.currentPose.clinical.setupCueCn}",
                    enabled = updated.voiceGuidanceEnabled,
                    muted = muted
                )
            }
            return
        }

        val prevBreathMs = currentPlaying.breathCycleElapsedMs
        val nextBreathMs = prevBreathMs + deltaMs
        val prevPhase = resolveBreathPhase(prevBreathMs)
        val nextPhase = resolveBreathPhase(nextBreathMs)
        val prevCycleIndex = prevBreathMs / BreathRhythm.CYCLE_DURATION_MS
        val nextCycleIndex = nextBreathMs / BreathRhythm.CYCLE_DURATION_MS

        val prevGuidance = currentPlaying.timelineGuidance
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
                voiceEngine.speakCue(
                    text = "${updated.currentPose.title}。${updated.currentPose.clinical.setupCueCn}",
                    enabled = updated.voiceGuidanceEnabled,
                    muted = muted
                )
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

        val nextGuidance = updated.timelineGuidance
        val muted = updated.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
        val amplitude = BuiltInSkinRegistry.skinFor(effectiveSkin).sensory.hapticAmplitude

        // Blind-operation Cue 1: Exact halfway bilateral switch (double-tap haptic + ascending chime + voice)
        if (updated.currentPose.bilateralSwitchAtHalfway &&
            !prevGuidance.isSecondSegment &&
            nextGuidance.isSecondSegment
        ) {
            hapticEngine.pulseHalfwaySwitchCue(maxAmplitude = (amplitude + 8).coerceAtMost(255), enabled = true)
            audioEngine.playHalfwaySwitchCue(effectiveSkin, updated.soundProfile, muted = muted)
            voiceEngine.speakCue(
                text = "温和换边，随呼气舒展另一侧。",
                enabled = updated.voiceGuidanceEnabled,
                muted = muted
            )
        } else if (prevGuidance.stage != PoseTimelineStage.SAFE_RETURN &&
            nextGuidance.stage == PoseTimelineStage.SAFE_RETURN
        ) {
            // Blind-operation Cue 2: Entering final safe-return window (soft settling pulse + sub-octave bell + voice)
            hapticEngine.pulseSafeReturnCue(maxAmplitude = amplitude, enabled = true)
            audioEngine.playSafeReturnCue(effectiveSkin, updated.soundProfile, muted = muted)
            voiceEngine.speakCue(
                text = updated.currentPose.clinical.safeReturnCueCn,
                enabled = updated.voiceGuidanceEnabled,
                muted = muted
            )
        } else if (prevPhase.isExhale != nextPhase.isExhale || prevCycleIndex != nextCycleIndex) {
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
            voiceEngine.stop()
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

    fun toggleVoiceGuidance() {
        val nextVoice = !_uiState.value.voiceGuidanceEnabled
        _uiState.update { state ->
            val updatedStage = (state.sessionStage as? StretchSessionStage.Playing)
                ?.copy(voiceGuidanceEnabled = nextVoice)
                ?: state.sessionStage
            state.copy(voiceGuidanceEnabled = nextVoice, sessionStage = updatedStage)
        }
        scope.launch {
            themePreferencesRepository.setVoiceGuidanceEnabled(nextVoice)
        }
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing
        if (!nextVoice) {
            voiceEngine.stop()
        } else if (cur != null) {
            val muted = cur.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
            voiceEngine.speakCue(
                text = "已开启轻声口令。${cur.currentPose.title}",
                enabled = true,
                muted = muted
            )
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
        voiceEngine.stop()
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
            voiceEngine.stop()
        }
        _uiState.update { state ->
            val latest = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            state.copy(sessionStage = latest.copy(soundProfile = nextSound))
        }
        val amp = BuiltInSkinRegistry.skinFor(skinId).sensory.hapticAmplitude
        hapticEngine.pulseInhaleWave(maxAmplitude = amp, enabled = true)
    }

    fun previousPose() {
        var targetPose: StretchPose? = null
        var voiceOn = false
        var muted = false
        _uiState.update { state ->
            val cur = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
            val clampedIndex = (cur.currentPoseIndex - 1).coerceAtLeast(0)
            val nextStage = cur.copy(
                currentPoseIndex = clampedIndex,
                poseElapsedMs = 0L,
                breathCycleElapsedMs = 0L,
                prepRemainingMs = 0L,
                isClinicalGuideOpen = false
            )
            targetPose = nextStage.currentPose
            voiceOn = nextStage.voiceGuidanceEnabled
            muted = nextStage.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
            state.copy(sessionStage = nextStage)
        }
        targetPose?.let { pose ->
            voiceEngine.speakCue("${pose.title}。${pose.clinical.setupCueCn}", enabled = voiceOn, muted = muted)
        }
    }

    fun nextPose(activeSkinId: SkinId? = null) {
        val cur = _uiState.value.sessionStage as? StretchSessionStage.Playing ?: return
        val effectiveSkin = activeSkinId ?: cur.skinId
        val nextIndex = cur.currentPoseIndex + 1
        if (nextIndex >= cur.routine.poses.size) {
            transitionToCompleted(cur, effectiveSkin)
        } else {
            val nextPoseObj = cur.routine.poses[nextIndex]
            _uiState.update { state ->
                val latest = state.sessionStage as? StretchSessionStage.Playing ?: return@update state
                state.copy(
                    sessionStage = latest.copy(
                        skinId = effectiveSkin,
                        currentPoseIndex = nextIndex,
                        poseElapsedMs = 0L,
                        breathCycleElapsedMs = 0L,
                        prepRemainingMs = 0L,
                        isClinicalGuideOpen = false
                    )
                )
            }
            val muted = cur.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
            audioEngine.playPoseTransitionChime(effectiveSkin, cur.soundProfile, muted = muted)
            voiceEngine.speakCue(
                text = "${nextPoseObj.title}。${nextPoseObj.clinical.setupCueCn}",
                enabled = cur.voiceGuidanceEnabled,
                muted = muted
            )
        }
    }

    fun closePlayerWithoutCompleting() {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        voiceEngine.stop()
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
        voiceEngine.stop()
        committedSessionIds.clear()
        _uiState.value = StretchUiState(onboardingSeen = onboardingSeen)
    }

    private fun transitionToCompleted(playing: StretchSessionStage.Playing, effectiveSkin: SkinId) {
        faceDownMonitor.stopMonitoring()
        hapticEngine.cancel()
        val muted = playing.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
        audioEngine.playPoseTransitionChime(effectiveSkin, playing.soundProfile, muted = muted)
        voiceEngine.speakCue(
            text = "一息既成，身自舒展。",
            enabled = playing.voiceGuidanceEnabled,
            muted = muted
        )
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
        voiceEngine.stop()
        super.onCleared()
    }

    companion object {
        const val PREP_BUFFER_DURATION_MS: Long = 3_000L

        fun provideFactory(
            catalogRepository: StretchCatalogRepository,
            themePreferencesRepository: ThemePreferencesRepository,
            journalRepository: PracticeJournalRepository,
            audioEngine: HarmonicAudioEngine,
            hapticEngine: BreathingHapticEngine,
            faceDownMonitor: FaceDownSensorMonitor,
            clockProvider: ClockProvider = SystemClockProvider,
            initialOnboardingSeen: Boolean = false,
            voiceEngine: VoiceCueEngine = NoOpVoiceCueEngine
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
                    initialOnboardingSeen = initialOnboardingSeen,
                    voiceEngine = voiceEngine
                ) as T
            }
        }
    }
}
