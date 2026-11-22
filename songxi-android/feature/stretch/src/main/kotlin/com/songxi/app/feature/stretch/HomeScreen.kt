package com.songxi.app.feature.stretch

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.EditorialStretchFigureCanvas
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.ShanShuiBackdropCanvas
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.StretchRoutine

@Composable
fun HomeScreen(
    viewModel: StretchSessionViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSkinGallery: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skin = LocalSongxiSkin.current
    val heroRoutine = viewModel.catalogRepository.heroRoutineFor(uiState.selectedScene)
    val quickReliefRoutines = viewModel.catalogRepository.quickReliefRoutines

    HomeScreen(
        selectedScene = uiState.selectedScene,
        heroRoutine = heroRoutine,
        quickReliefRoutines = quickReliefRoutines,
        onSelectScene = viewModel::selectScene,
        onStartRoutine = { routine ->
            viewModel.startRoutine(routine = routine, skinId = skin.id)
        },
        onSkinBadgeClick = {
            if (onNavigateToSkinGallery != null) {
                onNavigateToSkinGallery()
            } else {
                viewModel.cycleSkin()
            }
        },
        modifier = modifier,
        showOnboardingCard = !uiState.onboardingSeen,
        onDismissOnboarding = viewModel::dismissOnboarding
    )
}

@Suppress("ModifierParameter")
@Composable
fun HomeScreen(
    selectedScene: PostureScene,
    heroRoutine: StretchRoutine,
    quickReliefRoutines: List<StretchRoutine>,
    onSelectScene: (PostureScene) -> Unit,
    onStartRoutine: (StretchRoutine) -> Unit,
    onSkinBadgeClick: () -> Unit,
    modifier: Modifier = Modifier,
    showOnboardingCard: Boolean = false,
    onDismissOnboarding: () -> Unit = {}
) {
    val skin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)
    val subCardShape = RoundedCornerShape((skin.palette.cardCornerRadiusDp - 6).coerceIn(12, 24).dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
    ) {
        val isCompact = maxHeight < 560.dp
        val backdropHeight = when {
            maxHeight < 560.dp && showOnboardingCard -> 68.dp
            maxHeight < 560.dp -> 96.dp
            maxHeight < 820.dp -> 164.dp
            else -> 186.dp
        }
        val horizontalPad = if (isCompact) 14.dp else 18.dp
        val verticalPad = if (isCompact) 8.dp else 14.dp
        val sectionGap = if (isCompact) 6.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(skin.palette.canvasPrimary)
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad, vertical = verticalPad)
                .testTag("home_screen")
        ) {
            if (showOnboardingCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(skin.palette.surfaceCard, subCardShape)
                        .border(1.dp, skin.palette.borderHairline, subCardShape)
                        .padding(if (isCompact) 10.dp else 14.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("onboarding_card")
                ) {
                    Text(
                        text = "初见松息 · 一松一息，身自舒展",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "无需注册登录，全套微拉伸功法与四款山水插画画境永久免费开放；仅以克制合规的 3 秒「开屏雅鉴」支持独立维护。",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = skin.palette.inkSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SongxiActionSurface(
                        onClick = onDismissOnboarding,
                        shape = CircleShape,
                        backgroundColor = skin.palette.accentPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = if (isCompact) 38.dp else 44.dp),
                        testTag = "onboarding_enter_button"
                    ) {
                        Text(
                            text = "步入画境 · 开始今日一息",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.onAccent,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(sectionGap))
            }

            // Editorial greeting header + quick skin switcher badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = skin.meta.greetingPrefix,
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = skin.palette.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "今日一息",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = if (isCompact) 18.sp else 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = skin.meta.titleLetterSpacingSp.sp,
                        color = skin.palette.inkPrimary,
                        maxLines = 1
                    )
                }

                SongxiActionSurface(
                    onClick = onSkinBadgeClick,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCard,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier.defaultMinSize(
                        minWidth = 96.dp,
                        minHeight = if (isCompact) 34.dp else 38.dp
                    ),
                    testTag = "home_active_skin_badge"
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(skin.palette.accentPrimary, CircleShape)
                        )
                        Text(
                            text = skin.meta.titleCn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.inkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // Dynamic Shan-Shui + Editorial Illustration sanctuary stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight)
                    .clip(cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .testTag("home_shanshui_backdrop")
            ) {
                ShanShuiBackdropCanvas(
                    skin = skin,
                    driftPhase = 0.18f,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(if (isCompact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClassicalSealBadge(
                        skin = skin,
                        size = if (isCompact) 26.dp else 32.dp
                    )
                    Text(
                        text = skin.meta.subtitleCn,
                        fontSize = 11.sp,
                        color = skin.palette.inkSecondary,
                        maxLines = 1
                    )
                }

                val previewPose = heroRoutine.poses.firstOrNull()
                if (previewPose != null) {
                    EditorialStretchFigureCanvas(
                        pose = previewPose,
                        breathProgress = 0.65f,
                        isExhale = true,
                        skin = skin,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 2.dp)
                            .size(if (isCompact) 78.dp else 120.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // 3 Posture scene filter pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostureScene.entries.forEach { scene ->
                    val isSelected = scene == selectedScene
                    SongxiTogglePill(
                        isActive = isSelected,
                        onClick = { onSelectScene(scene) },
                        shape = CircleShape,
                        activeBackgroundColor = skin.palette.accentPrimary,
                        inactiveBackgroundColor = skin.palette.surfaceCard,
                        activeBorderColor = skin.palette.accentPrimary,
                        inactiveBorderColor = skin.palette.borderHairline,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = if (isCompact) 34.dp else 40.dp),
                        testTag = "home_scene_${scene.key}"
                    ) {
                        Text(
                            text = scene.labelCn,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // Single context-aware Hero Prescription Card (non-clickable so child button stays distinct)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(if (isCompact) 10.dp else 16.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("home_hero_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedScene.badgeCn,
                        fontSize = 11.sp,
                        color = skin.palette.accentPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(skin.palette.accentPrimarySoft, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatRoutineDurationLabel(heroRoutine.totalDurationSeconds),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 6.dp))

                Text(
                    text = heroRoutine.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (isCompact) 15.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = heroRoutine.subtitle,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    lineHeight = if (isCompact) 15.sp else 18.sp,
                    color = skin.palette.inkSecondary,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                SongxiActionSurface(
                    onClick = { onStartRoutine(heroRoutine) },
                    shape = CircleShape,
                    backgroundColor = skin.palette.accentPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = if (isCompact) 40.dp else 46.dp),
                    testTag = "home_start_hero_button"
                ) {
                    Text(
                        text = "开始一息 · 闭眼轻舒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.onAccent,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))

            // 4 Quick Micro-Relief Shortcuts (eager 2x2 grid — never LazyGrid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "片刻救急轻帖",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary
                )
                Text(
                    text = "工位微幅 · 随时一息",
                    fontSize = 11.sp,
                    color = skin.palette.inkTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedQuickRoutines = quickReliefRoutines.take(4)
            for (rowStart in displayedQuickRoutines.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val leftRoutine = displayedQuickRoutines[rowStart]
                    QuickReliefCard(
                        routine = leftRoutine,
                        index = rowStart,
                        shape = subCardShape,
                        onClick = { onStartRoutine(leftRoutine) },
                        modifier = Modifier.weight(1f)
                    )

                    val rightIndex = rowStart + 1
                    if (rightIndex < displayedQuickRoutines.size) {
                        val rightRoutine = displayedQuickRoutines[rightIndex]
                        QuickReliefCard(
                            routine = rightRoutine,
                            index = rightIndex,
                            shape = subCardShape,
                            onClick = { onStartRoutine(rightRoutine) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowStart + 2 < displayedQuickRoutines.size) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuickReliefCard(
    routine: StretchRoutine,
    index: Int,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    SongxiActionSurface(
        onClick = onClick,
        shape = shape,
        backgroundColor = skin.palette.surfaceCard,
        borderColor = skin.palette.borderHairline,
        contentAlignment = Alignment.TopStart,
        modifier = modifier.defaultMinSize(minHeight = 76.dp),
        testTag = "home_quick_routine_$index"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatRoutineDurationLabel(routine.totalDurationSeconds),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = skin.palette.accentPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${routine.poses.size}式",
                    fontSize = 10.sp,
                    color = skin.palette.inkTertiary,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = routine.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.palette.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = routine.subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = skin.palette.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
