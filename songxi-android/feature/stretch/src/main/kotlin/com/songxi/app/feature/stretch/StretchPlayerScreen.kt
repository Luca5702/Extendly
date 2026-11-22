package com.songxi.app.feature.stretch

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
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
import com.songxi.app.core.model.SomaticFeeling
import com.songxi.app.core.model.SoundProfileId
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
    clockNowMillis: () -> Long = { System.currentTimeMillis() },
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    val latestOnAdvanceMillis by rememberUpdatedState(onAdvanceMillis)
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

    // Deterministic monotonic frame-clock playback loop — suspended when paused or flipped face-down
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
    val cardShape = RoundedCornerShape(skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp)
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
        val haloSize = if (isCompact) 118.dp else 232.dp
        val figureSize = if (isCompact) 92.dp else 172.dp
        val verticalPad = if (isCompact) 6.dp else 14.dp
        val sectionGap = if (isCompact) 4.dp else 12.dp
        val controlMinHeight = if (isCompact) 36.dp else 46.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = if (isCompact) 12.dp else 18.dp, vertical = verticalPad)
                .testTag("player_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row: routine progress + countdown + close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${playing.routine.title} · 第 ${playing.currentPoseIndex + 1}/${playing.routine.poses.size} 式",
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatPoseSecondsClock(playing.remainingPoseSeconds),
                        fontSize = if (isCompact) 15.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor
                    )
                }

                SongxiActionSurface(
                    onClick = onClose,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier.defaultMinSize(
                        minWidth = 68.dp,
                        minHeight = if (isCompact) 40.dp else 48.dp
                    ),
                    testTag = "player_close_button"
                ) {
                    Text(
                        text = "收起",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.inkPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
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

            Spacer(modifier = Modifier.height(sectionGap))

            // Breathing Halo + Animated Editorial Figure Stage
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
                        skin = skin,
                        eyesFreeMinimal = playing.isEyesFreeMode,
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
                    .background(skin.palette.accentPrimarySoft, CircleShape)
                    .border(1.dp, skin.palette.borderHairline, CircleShape)
                    .padding(horizontal = 14.dp, vertical = if (isCompact) 3.dp else 6.dp)
                    .testTag("player_breath_phase_label")
            ) {
                Text(
                    text = breathPhase.phaseLabelCn,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.accentPrimary
                )
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // Pose Title & Gentle Cue (<= 36 chars)
            val poseHeading = if (playing.isSecondHalf) {
                "${playing.currentPose.title} · 温和换边"
            } else {
                playing.currentPose.title
            }
            Text(
                text = poseHeading,
                style = MaterialTheme.typography.titleMedium,
                fontSize = if (isCompact) 14.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = playing.currentPose.gentleCue,
                fontSize = if (isCompact) 11.sp else 13.sp,
                lineHeight = if (isCompact) 15.sp else 20.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                maxLines = if (isCompact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 14.dp))

            // Eyes-Free, Soundscape & Office-Silent Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SongxiTogglePill(
                    isActive = playing.isEyesFreeMode,
                    onClick = onToggleEyesFree,
                    shape = CircleShape,
                    activeBackgroundColor = skin.palette.accentPrimary,
                    inactiveBackgroundColor = skin.palette.surfaceCard,
                    activeBorderColor = skin.palette.accentPrimary,
                    inactiveBorderColor = skin.palette.borderHairline,
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

                SongxiActionSurface(
                    onClick = onCycleSound,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(1f)
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
                    onClick = onToggleSilent,
                    shape = CircleShape,
                    activeBackgroundColor = skin.palette.accentPrimary,
                    inactiveBackgroundColor = skin.palette.surfaceCard,
                    activeBorderColor = skin.palette.accentPrimary,
                    inactiveBorderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(1f)
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
                    onClick = onPreviousPose,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(0.85f)
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
                    onClick = onTogglePause,
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
                    onClick = onNextPose,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(0.85f)
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
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = skin.palette.inkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${completed.routine.title} · 舒展 ${formatCompletionMinutesLabel(completed.routine.totalDurationSeconds)}",
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
