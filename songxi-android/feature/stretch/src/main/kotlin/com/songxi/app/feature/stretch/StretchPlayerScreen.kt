package com.songxi.app.feature.stretch

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.designsystem.BreathingHaloCanvas
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.EditorialStretchFigureCanvas
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiEditorialSerifFamily
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.designsystem.songxiGentleStageReveal
import com.songxi.app.core.model.SomaticFeeling
import com.songxi.app.core.model.SoundProfileId
import com.songxi.app.core.model.StretchPose
import kotlinx.coroutines.delay

private const val PLAYER_TICK_MS: Long = 50L

@Composable
fun StretchPlayerOverlayHost(
    viewModel: StretchSessionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skin = LocalSongxiSkin.current

    when (val stage = uiState.sessionStage) {
        StretchSessionStage.Idle -> Unit
        is StretchSessionStage.Playing -> {
            StretchPlayerScreen(
                playing = stage,
                onAdvanceMillis = { delta -> viewModel.advancePlaybackMillis(delta, skin.id) },
                onPlayerEntered = viewModel::onPlayerScreenEntered,
                onPlayerExited = viewModel::onPlayerScreenExited,
                onTogglePause = viewModel::togglePause,
                onResumeFromFlip = viewModel::resumeFromFlipBanner,
                onToggleEyesFree = viewModel::toggleEyesFreeMode,
                onToggleVoiceGuidance = viewModel::toggleVoiceGuidance,
                onSkipPrepBuffer = { viewModel.skipPrepBuffer(skin.id) },
                onToggleBiomechanicalGuide = viewModel::toggleBiomechanicalGuide,
                onOpenClinicalGuide = viewModel::openClinicalGuide,
                onCloseClinicalGuide = viewModel::closeClinicalGuide,
                onCycleSound = { viewModel.cycleAudibleSoundProfile(skin.id) },
                onToggleSilent = { viewModel.toggleSilentHapticMode(skin.id) },
                onPreviousPose = viewModel::previousPose,
                onNextPose = { viewModel.nextPose(skin.id) },
                onClose = viewModel::closePlayerWithoutCompleting,
                clockNowMillis = viewModel.clockProvider::nowEpochMillis,
                modifier = modifier
            )
        }
        is StretchSessionStage.Completed -> {
            CompletionScreen(
                completed = stage,
                onSelectFeeling = viewModel::selectCompletionFeeling,
                onFinish = { viewModel.finishCompletionAndSave(skin.id) },
                modifier = modifier
            )
        }
    }
}

@Suppress("ModifierParameter")
@Composable
fun StretchPlayerScreen(
    playing: StretchSessionStage.Playing,
    onAdvanceMillis: (Long) -> Unit,
    onPlayerEntered: () -> Unit,
    onPlayerExited: () -> Unit,
    onTogglePause: () -> Unit,
    onResumeFromFlip: () -> Unit,
    onToggleEyesFree: () -> Unit,
    onCycleSound: () -> Unit,
    onToggleSilent: () -> Unit,
    onPreviousPose: () -> Unit,
    onNextPose: () -> Unit,
    onClose: () -> Unit,
    onToggleVoiceGuidance: () -> Unit = {},
    onSkipPrepBuffer: () -> Unit = {},
    onToggleBiomechanicalGuide: () -> Unit = {},
    onOpenClinicalGuide: () -> Unit = {},
    onCloseClinicalGuide: () -> Unit = {},
    clockNowMillis: () -> Long = { System.currentTimeMillis() },
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    val latestOnAdvanceMillis by rememberUpdatedState(onAdvanceMillis)
    @Suppress("UNUSED_VARIABLE")
    val latestClockNowMillis by rememberUpdatedState(clockNowMillis)
    val latestOnPlayerEntered by rememberUpdatedState(onPlayerEntered)
    val latestOnPlayerExited by rememberUpdatedState(onPlayerExited)

    // Keep screen awake strictly while active player_screen is mounted, releasing on exit/completion
    DisposableEffect(playing.sessionId, view) {
        val activity = context.findHostActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view.keepScreenOn = true
        latestOnPlayerEntered()
        onDispose {
            latestOnPlayerExited()
            view.keepScreenOn = false
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Deterministic monotonic frame-clock playback loop — suspended when paused, flipped, or reading clinical guide
    LaunchedEffect(playing.sessionId, playing.isEffectivelyPaused) {
        if (playing.isEffectivelyPaused) return@LaunchedEffect
        var lastFrameMs = androidx.compose.runtime.withFrameMillis { it }
        while (!playing.isEffectivelyPaused) {
            delay(PLAYER_TICK_MS)
            val frameMs = androidx.compose.runtime.withFrameMillis { it }
            val delta = (frameMs - lastFrameMs).coerceIn(0L, 5_000L)
            lastFrameMs = frameMs
            if (!playing.isEffectivelyPaused && delta > 0L) {
                latestOnAdvanceMillis(delta)
            }
        }
    }

    val breathPhase = playing.breathPhase
    val guidance = playing.timelineGuidance
    val clinical = playing.currentPose.clinical
    val cardShape = RoundedCornerShape(skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp)

    // Immersive sanctuary quietude ("三分画七分留白"): gently softens auxiliary chrome after 3.5s of undisturbed practice
    var interactionPulse by remember { mutableIntStateOf(0) }
    var settledQuiet by remember(playing.currentPoseIndex, playing.isEffectivelyPaused, interactionPulse) {
        mutableStateOf(false)
    }
    LaunchedEffect(playing.currentPoseIndex, playing.isEffectivelyPaused, playing.isInPrepBuffer, interactionPulse) {
        if (!playing.isEffectivelyPaused && !playing.isInPrepBuffer) {
            delay(3_500L)
            settledQuiet = true
        } else {
            settledQuiet = false
        }
    }
    val quietChromeAlpha by animateFloatAsState(
        targetValue = if (settledQuiet && !playing.isEffectivelyPaused && !playing.isClinicalGuideOpen) 0.62f else 1.0f,
        animationSpec = tween(durationMillis = 800),
        label = "songxi_quiet_chrome"
    )

    val stageBackgroundBrush = if (playing.isEyesFreeMode) {
        val deepGlow = lerp(skin.palette.canvasPrimary, Color(0xFF101715), if (skin.palette.isDark) 0.45f else 0.72f)
        val haloWash = lerp(deepGlow, skin.palette.accentPrimaryGlow, 0.28f + 0.18f * breathPhase.expansion)
        Brush.radialGradient(colors = listOf(haloWash, deepGlow))
    } else {
        Brush.verticalGradient(
            colors = listOf(skin.palette.canvasSkyWashTop, skin.palette.canvasPrimary)
        )
    }
    val primaryTextColor = if (playing.isEyesFreeMode) Color(0xFFEDE8DD) else skin.palette.inkPrimary
    val secondaryTextColor = if (playing.isEyesFreeMode) Color(0xFFB7C2BC) else skin.palette.inkSecondary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(stageBackgroundBrush)
    ) {
        val isCompact = maxHeight < 580.dp
        val haloSize = if (isCompact) 118.dp else 238.dp
        val figureSize = if (isCompact) 92.dp else 178.dp
        val verticalPad = if (isCompact) 6.dp else 16.dp
        val sectionGap = if (isCompact) 4.dp else 12.dp
        val controlMinHeight = if (isCompact) 36.dp else 46.dp
        val headerPillMinHeight = if (isCompact) 36.dp else 44.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .songxiGentleStageReveal(playing.sessionId)
                .verticalScroll(scrollState)
                .padding(horizontal = if (isCompact) 12.dp else 18.dp, vertical = verticalPad)
                .testTag("player_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row: routine progress + tabular countdown + biomechanics toggle + clinical guide + close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = quietChromeAlpha },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "${playing.routine.title} · 第 ${playing.currentPoseIndex + 1}/${playing.routine.poses.size} 式",
                        style = SongxiTabularNumericStyle,
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatPoseSecondsClock(playing.remainingPoseSeconds),
                            style = SongxiTabularNumericStyle,
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "${clinical.modality.badgeCn} · ${guidance.stage.labelCn}",
                            fontSize = if (isCompact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("player_timeline_stage_badge")
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongxiTogglePill(
                        isActive = playing.showBiomechanicalGuide,
                        onClick = {
                            interactionPulse++
                            onToggleBiomechanicalGuide()
                        },
                        shape = CircleShape,
                        activeBackgroundColor = skin.palette.accentPrimarySoft,
                        inactiveBackgroundColor = skin.palette.surfaceCard.copy(alpha = 0.78f),
                        activeBorderColor = skin.palette.accentPrimary,
                        inactiveBorderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minHeight = headerPillMinHeight),
                        testTag = "player_biomechanics_toggle"
                    ) {
                        Text(
                            text = "导引线",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (playing.showBiomechanicalGuide) skin.palette.accentPrimary else skin.palette.inkSecondary,
                            modifier = Modifier.padding(horizontal = if (isCompact) 7.dp else 9.dp),
                            maxLines = 1
                        )
                    }

                    SongxiActionSurface(
                        onClick = {
                            interactionPulse++
                            onOpenClinicalGuide()
                        },
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCard.copy(alpha = 0.78f),
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minHeight = headerPillMinHeight),
                        testTag = "player_clinical_guide_button"
                    ) {
                        Text(
                            text = "精要",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.inkPrimary,
                            modifier = Modifier.padding(horizontal = if (isCompact) 7.dp else 9.dp),
                            maxLines = 1
                        )
                    }

                    SongxiActionSurface(
                        onClick = onClose,
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCard.copy(alpha = 0.78f),
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(
                            minWidth = if (isCompact) 50.dp else 64.dp,
                            minHeight = if (isCompact) 40.dp else 48.dp
                        ),
                        testTag = "player_close_button"
                    ) {
                        Text(
                            text = "收起",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.inkPrimary,
                            modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 12.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Optional 3s Settle-In Prep Buffer Banner ("入席安顿缓冲")
            if (playing.isInPrepBuffer) {
                Spacer(modifier = Modifier.height(sectionGap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(skin.palette.accentPrimarySoft, cardShape)
                        .border(1.dp, skin.palette.accentPrimary, cardShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("player_prep_buffer_banner"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "入席安顿 ${playing.prepDisplaySeconds}s · 放稳手机沉落双肩",
                        style = SongxiTabularNumericStyle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    SongxiActionSurface(
                        onClick = onSkipPrepBuffer,
                        shape = CircleShape,
                        backgroundColor = skin.palette.accentPrimary,
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                        testTag = "player_skip_prep_button"
                    ) {
                        Text(
                            text = "立即入式",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skin.palette.onAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (playing.isFaceDownPaused) {
                Spacer(modifier = Modifier.height(sectionGap))
                SongxiActionSurface(
                    onClick = onResumeFromFlip,
                    shape = cardShape,
                    backgroundColor = skin.palette.accentPrimarySoft,
                    borderColor = skin.palette.accentPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = if (isCompact) 36.dp else 46.dp),
                    testTag = "player_paused_by_flip_banner"
                ) {
                    Text(
                        text = "已翻转静默暂存 · 拾起手机即可续接此息",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 16.dp))

            // Breathing Halo + Animated Editorial Figure Stage (Focal Sanctuary Centerpiece)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(haloSize + 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BreathingHaloCanvas(
                    breathProgress = breathPhase.phaseProgress,
                    isExhale = breathPhase.isExhale,
                    skin = skin,
                    eyesFreeMode = playing.isEyesFreeMode,
                    modifier = Modifier
                        .size(haloSize)
                        .testTag("player_breathing_halo")
                ) {
                    EditorialStretchFigureCanvas(
                        pose = playing.currentPose,
                        breathProgress = breathPhase.phaseProgress,
                        isExhale = breathPhase.isExhale,
                        isSecondHalf = playing.isSecondHalf,
                        asymmetry = playing.routine.asymmetry,
                        skin = skin,
                        eyesFreeMinimal = playing.isEyesFreeMode,
                        viewPlane = clinical.viewPlane,
                        showBiomechanicalGuide = playing.showBiomechanicalGuide,
                        modifier = Modifier
                            .size(figureSize)
                            .align(Alignment.Center)
                            .testTag("player_figure_canvas")
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // 4s Inhale / 6s Exhale Phase Pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(skin.palette.accentPrimarySoft.copy(alpha = 0.88f), CircleShape)
                    .border(0.8.dp, skin.palette.borderHairline, CircleShape)
                    .padding(horizontal = 16.dp, vertical = if (isCompact) 3.dp else 6.dp)
                    .testTag("player_breath_phase_label")
            ) {
                Text(
                    text = breathPhase.phaseLabelCn,
                    fontFamily = SongxiEditorialSerifFamily,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = skin.palette.accentPrimary
                )
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // Pose Title in Song-serif & 4-Stage Safe Coaching Cue (<= 36 chars)
            val sideSuffix = when {
                !playing.currentPose.bilateralSwitchAtHalfway -> ""
                playing.isSecondHalf -> " · 温和换边"
                guidance.activeBilateralSide > 0 -> " · 右侧偏重延展"
                guidance.activeBilateralSide < 0 -> " · 左侧延展"
                else -> ""
            }
            Text(
                text = "${playing.currentPose.title}$sideSuffix",
                style = MaterialTheme.typography.titleLarge,
                fontSize = if (isCompact) 15.sp else 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = guidance.activeCueCn,
                fontSize = if (isCompact) 11.sp else 13.sp,
                lineHeight = if (isCompact) 15.sp else 20.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                maxLines = if (isCompact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 16.dp))

            // Unified Translucent Sensory Dock (replaces 4 separate heavy bordered boxes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = quietChromeAlpha }
                    .clip(CircleShape)
                    .background(skin.palette.surfaceCard.copy(alpha = 0.68f), CircleShape)
                    .border(0.8.dp, skin.palette.borderHairline, CircleShape)
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SongxiTogglePill(
                    isActive = playing.isEyesFreeMode,
                    onClick = {
                        interactionPulse++
                        onToggleEyesFree()
                    },
                    shape = CircleShape,
                    activeBackgroundColor = skin.palette.accentPrimary,
                    inactiveBackgroundColor = Color.Transparent,
                    activeBorderColor = skin.palette.accentPrimary,
                    inactiveBorderColor = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_eyes_free_toggle"
                ) {
                    Text(
                        text = if (playing.isEyesFreeMode) "余光已启" else "闭眼余光",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (playing.isEyesFreeMode) skin.palette.onAccent else skin.palette.inkPrimary,
                        maxLines = 1
                    )
                }

                SongxiTogglePill(
                    isActive = playing.voiceGuidanceEnabled,
                    onClick = {
                        interactionPulse++
                        onToggleVoiceGuidance()
                    },
                    shape = CircleShape,
                    activeBackgroundColor = skin.palette.accentPrimary,
                    inactiveBackgroundColor = Color.Transparent,
                    activeBorderColor = skin.palette.accentPrimary,
                    inactiveBorderColor = Color.Transparent,
                    modifier = Modifier
                        .weight(0.82f)
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_voice_cue_toggle"
                ) {
                    Text(
                        text = if (playing.voiceGuidanceEnabled) "口令开" else "轻口令",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (playing.voiceGuidanceEnabled) skin.palette.onAccent else skin.palette.inkPrimary,
                        maxLines = 1
                    )
                }

                SongxiActionSurface(
                    onClick = {
                        interactionPulse++
                        onCycleSound()
                    },
                    shape = CircleShape,
                    backgroundColor = Color.Transparent,
                    borderColor = null,
                    modifier = Modifier
                        .weight(1.05f)
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_sound_selector"
                ) {
                    Text(
                        text = "音境·${playing.soundProfile.shortSelectorTitleCn()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.inkPrimary,
                        maxLines = 1
                    )
                }

                val isSilent = playing.soundProfile == SoundProfileId.SILENT_HAPTIC_ONLY
                SongxiTogglePill(
                    isActive = isSilent,
                    onClick = {
                        interactionPulse++
                        onToggleSilent()
                    },
                    shape = CircleShape,
                    activeBackgroundColor = skin.palette.accentPrimary,
                    inactiveBackgroundColor = Color.Transparent,
                    activeBorderColor = skin.palette.accentPrimary,
                    inactiveBorderColor = Color.Transparent,
                    modifier = Modifier
                        .weight(0.95f)
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_sound_silent"
                ) {
                    Text(
                        text = "静默微震",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSilent) skin.palette.onAccent else skin.palette.inkPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

            // Transport Controls: Prev / Pause-Resume / Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongxiActionSurface(
                    onClick = {
                        interactionPulse++
                        onPreviousPose()
                    },
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard.copy(alpha = 0.78f),
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(0.85f)
                        .graphicsLayer { alpha = quietChromeAlpha }
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_prev_pose_button"
                ) {
                    Text(
                        text = "上一式",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.inkPrimary
                    )
                }

                SongxiActionSurface(
                    onClick = {
                        interactionPulse++
                        onTogglePause()
                    },
                    shape = CircleShape,
                    backgroundColor = skin.palette.accentPrimary,
                    modifier = Modifier
                        .weight(1.3f)
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_pause_resume_button"
                ) {
                    Text(
                        text = if (playing.isEffectivelyPaused) "继续一息" else "暂歇片刻",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.onAccent
                    )
                }

                val isFinalPose = playing.currentPoseIndex >= playing.routine.poses.lastIndex
                SongxiActionSurface(
                    onClick = {
                        interactionPulse++
                        onNextPose()
                    },
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard.copy(alpha = 0.78f),
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(0.85f)
                        .graphicsLayer { alpha = quietChromeAlpha }
                        .defaultMinSize(minHeight = controlMinHeight),
                    testTag = "player_next_pose_button"
                ) {
                    Text(
                        text = if (isFinalPose) "完成" else "下一式",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.inkPrimary
                    )
                }
            }
        }

        // On-Demand Clinical Kinesiology Guide Sheet ("式法精要")
        if (playing.isClinicalGuideOpen) {
            ClinicalPoseGuideOverlay(
                pose = playing.currentPose,
                onDismiss = onCloseClinicalGuide
            )
        }
    }
}

@Composable
private fun ClinicalPoseGuideOverlay(
    pose: StretchPose,
    onDismiss: () -> Unit
) {
    val skin = LocalSongxiSkin.current
    val clinical = pose.clinical
    val cardShape = RoundedCornerShape(skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp)
    val subShape = RoundedCornerShape((skin.palette.cardCornerRadiusDp - 6).coerceIn(12, 24).dp)
    val sheetScroll = rememberScrollState()

    BackHandler(onBack = onDismiss)

    val backdropInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skin.palette.surfaceOverlay)
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardInteraction = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .songxiGentleStageReveal(pose.id)
                .clip(cardShape)
                .background(skin.palette.surfaceCard, cardShape)
                .border(1.dp, skin.palette.borderHairline, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                )
                .verticalScroll(sheetScroll)
                .padding(18.dp)
                .testTag("player_clinical_guide_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${clinical.phaseRole.stageTitleCn} · ${clinical.viewPlane.labelCn}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary
                    )
                    Text(
                        text = "式法精要 · ${pose.title}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary
                    )
                }
                ClassicalSealBadge(skin = skin, size = 30.dp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modality mechanism banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(subShape)
                    .background(skin.palette.accentPrimarySoft, subShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = "生理机制：${clinical.modality.badgeCn}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.accentPrimary
                )
                Text(
                    text = clinical.modality.mechanismSummaryCn,
                    fontSize = 11.sp,
                    color = skin.palette.inkSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ClinicalInfoRow(
                label = "靶向肌群与筋膜",
                body = clinical.primaryMusclesCn,
                shape = subShape
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClinicalInfoRow(
                label = "温和体感阈值（RPE 3–4/10）",
                body = clinical.targetSensationCn,
                shape = subShape
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClinicalInfoRow(
                label = "防代偿稳固锚点",
                body = clinical.antiCompensationCn,
                shape = subShape
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClinicalInfoRow(
                label = "临床安全边界提示",
                body = clinical.redFlagWarningCn,
                shape = subShape,
                accentVermilion = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            SongxiActionSurface(
                onClick = onDismiss,
                shape = CircleShape,
                backgroundColor = skin.palette.accentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp),
                testTag = "player_clinical_guide_close"
            ) {
                Text(
                    text = "知悉 · 续接此息",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.onAccent
                )
            }
        }
    }
}

@Composable
private fun ClinicalInfoRow(
    label: String,
    body: String,
    shape: RoundedCornerShape,
    accentVermilion: Boolean = false
) {
    val skin = LocalSongxiSkin.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(skin.palette.surfaceCardSubtle, shape)
            .border(1.dp, skin.palette.borderHairline, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accentVermilion) skin.palette.sealVermilion else skin.palette.inkPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = skin.palette.inkSecondary
        )
    }
}

@Suppress("ModifierParameter")
@Composable
fun CompletionScreen(
    completed: StretchSessionStage.Completed,
    onSelectFeeling: (SomaticFeeling) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardShape = RoundedCornerShape(skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
    ) {
        val isCompact = maxHeight < 580.dp
        val rippleSize = if (isCompact) 118.dp else 180.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .songxiGentleStageReveal(completed.sessionId)
                .verticalScroll(scrollState)
                .padding(
                    horizontal = if (isCompact) 14.dp else 20.dp,
                    vertical = if (isCompact) 8.dp else 18.dp
                )
                .testTag("completion_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ClassicalSealBadge(skin = skin, size = if (isCompact) 28.dp else 38.dp)

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))

            Text(
                text = "一息成 · 身自舒展",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = if (isCompact) 17.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = skin.meta.titleLetterSpacingSp.sp,
                color = skin.palette.inkPrimary
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

            BreathingHaloCanvas(
                breathProgress = 0.62f,
                isExhale = true,
                skin = skin,
                completionRippleMode = true,
                rippleCount = 4,
                modifier = Modifier
                    .size(rippleSize)
                    .testTag("completion_ripple_canvas")
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(if (isCompact) 10.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = skin.meta.completionVerse,
                    fontFamily = SongxiEditorialSerifFamily,
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    color = skin.palette.inkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${completed.routine.title} · 舒展 ${formatCompletionMinutesLabel(completed.routine.totalDurationSeconds)}",
                    style = SongxiTabularNumericStyle,
                    fontSize = 11.sp,
                    color = skin.palette.inkSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

            Text(
                text = "此刻身体有何微小的松动？（可选）",
                fontSize = 11.sp,
                color = skin.palette.inkSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SomaticFeeling.entries.forEach { feeling ->
                    val isSelected = completed.selectedFeeling == feeling
                    SongxiTogglePill(
                        isActive = isSelected,
                        onClick = { onSelectFeeling(feeling) },
                        shape = CircleShape,
                        activeBackgroundColor = skin.palette.accentPrimary,
                        inactiveBackgroundColor = skin.palette.surfaceCard,
                        activeBorderColor = skin.palette.accentPrimary,
                        inactiveBorderColor = skin.palette.borderHairline,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = if (isCompact) 36.dp else 44.dp),
                        testTag = "completion_feeling_${feeling.key}"
                    ) {
                        Text(
                            text = feeling.labelCn,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 20.dp))

            SongxiActionSurface(
                onClick = onFinish,
                shape = CircleShape,
                backgroundColor = skin.palette.accentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (isCompact) 40.dp else 48.dp),
                testTag = "completion_finish_button"
            ) {
                Text(
                    text = "收入松息手札",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.onAccent
                )
            }
        }
    }
}
