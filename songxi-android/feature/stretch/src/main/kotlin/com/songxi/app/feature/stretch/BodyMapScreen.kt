package com.songxi.app.feature.stretch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.songxi.app.core.designsystem.BodyTensionMapCanvas
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.designsystem.songxiGentleStageReveal
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PrescriptionPhaseRole
import com.songxi.app.core.model.SavedPrescriptionPreset
import com.songxi.app.core.model.StretchRoutine

private val SUPPORTED_CUSTOM_MINUTES: List<Int> = listOf(2, 4, 6)

@Composable
fun BodyMapScreen(
    viewModel: StretchSessionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skin = LocalSongxiSkin.current
    val booklets = viewModel.catalogRepository.curatedBooklets
    val previewRoutine = remember(
        uiState.selectedBodyRegions,
        uiState.selectedDurationMinutes,
        uiState.selectedAsymmetry
    ) {
        viewModel.catalogRepository.composeCustomRoutine(
            regions = uiState.selectedBodyRegions.toSet(),
            targetMinutes = uiState.selectedDurationMinutes,
            asymmetry = uiState.selectedAsymmetry
        )
    }

    BodyMapScreen(
        selectedRegions = uiState.selectedBodyRegions,
        selectedDurationMinutes = uiState.selectedDurationMinutes,
        booklets = booklets,
        bookletCompletedDays = uiState.bookletCompletedDays,
        onToggleRegion = viewModel::toggleBodyRegion,
        onSelectDuration = viewModel::selectCustomDuration,
        onComposeAndStart = { viewModel.composeAndStartCustomRoutine(skin.id) },
        onStartBooklet = { index -> viewModel.startBooklet(index, skin.id) },
        selectedAsymmetry = uiState.selectedAsymmetry,
        previewRoutine = previewRoutine,
        onSelectAsymmetry = viewModel::selectLateralAsymmetry,
        savedPresets = uiState.savedPresets,
        onSaveCurrentPreset = viewModel::saveCurrentCustomPreset,
        onStartSavedPreset = { preset -> viewModel.startSavedPreset(preset, skin.id) },
        onDeleteSavedPreset = viewModel::deleteSavedPreset,
        inspectedBookletIndex = uiState.inspectedBookletIndex,
        onOpenBookletChapters = viewModel::openBookletDetail,
        onCloseBookletChapters = viewModel::closeBookletDetail,
        onStartBookletSpecificDay = { bIdx, dayIdx ->
            viewModel.startBookletDay(bIdx, dayIdx, skin.id)
        },
        modifier = modifier
    )
}

@Suppress("ModifierParameter")
@Composable
fun BodyMapScreen(
    selectedRegions: List<BodyRegion>,
    selectedDurationMinutes: Int,
    booklets: List<BookletSeries>,
    bookletCompletedDays: Map<String, Set<Int>>,
    onToggleRegion: (BodyRegion) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onComposeAndStart: () -> Unit,
    onStartBooklet: (Int) -> Unit,
    selectedAsymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED,
    previewRoutine: StretchRoutine? = null,
    onSelectAsymmetry: (LateralAsymmetry) -> Unit = {},
    savedPresets: List<SavedPrescriptionPreset> = emptyList(),
    onSaveCurrentPreset: () -> Unit = {},
    onStartSavedPreset: (SavedPrescriptionPreset) -> Unit = {},
    onDeleteSavedPreset: (String) -> Unit = {},
    inspectedBookletIndex: Int? = null,
    onOpenBookletChapters: (Int) -> Unit = {},
    onCloseBookletChapters: () -> Unit = {},
    onStartBookletSpecificDay: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)
    val subCardShape = RoundedCornerShape((skin.palette.cardCornerRadiusDp - 6).coerceIn(12, 24).dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(skin.palette.canvasPrimary)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("bodymap_screen")
        ) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "按位寻音 · 三阶成方",
                        fontSize = 12.sp,
                        color = skin.palette.inkSecondary
                    )
                    Text(
                        text = "身体寻音",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = skin.meta.titleLetterSpacingSp.sp,
                        color = skin.palette.inkPrimary
                    )
                }
                ClassicalSealBadge(skin = skin, size = 32.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive 6-Zone Body Tension Map Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeLabel = if (selectedRegions.isEmpty()) {
                        "默认温和养护：后颈风池 · 双肩斜方"
                    } else {
                        "已选 ${selectedRegions.size}/3 处：" +
                            selectedRegions.joinToString(" · ") { it.labelCn }
                    }
                    Text(
                        text = activeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(256.dp)
                        .clip(subCardShape)
                        .background(skin.palette.surfaceCardSubtle, subCardShape)
                        .border(1.dp, skin.palette.borderHairline, subCardShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BodyTensionMapCanvas(
                        selectedRegions = selectedRegions.toSet(),
                        onRegionToggle = onToggleRegion,
                        skin = skin,
                        exposeZoneTestTags = true,
                        showLabels = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("bodymap_canvas")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "舒展时长 · 完整四吸六呼节律",
                    fontSize = 11.sp,
                    color = skin.palette.inkSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SUPPORTED_CUSTOM_MINUTES.forEach { minutes ->
                        val isSelected = selectedDurationMinutes == minutes
                        SongxiTogglePill(
                            isActive = isSelected,
                            onClick = { onSelectDuration(minutes) },
                            shape = CircleShape,
                            activeBackgroundColor = skin.palette.accentPrimary,
                            inactiveBackgroundColor = skin.palette.canvasPrimary,
                            activeBorderColor = skin.palette.accentPrimary,
                            inactiveBorderColor = skin.palette.borderHairline,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp),
                            testTag = "bodymap_duration_$minutes"
                        ) {
                            Text(
                                text = "${minutes}分钟",
                                style = SongxiTabularNumericStyle,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bilateral Asymmetry Allocation (双侧均衡 5:5 / 左6:右4 / 右6:左4)
                Text(
                    text = "左右偏侧时长配比 · 惯用鼠标或单侧背包偏紧调配",
                    fontSize = 11.sp,
                    color = skin.palette.inkSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LateralAsymmetry.entries.forEach { option ->
                        val isSelected = selectedAsymmetry == option
                        SongxiTogglePill(
                            isActive = isSelected,
                            onClick = { onSelectAsymmetry(option) },
                            shape = CircleShape,
                            activeBackgroundColor = skin.palette.accentPrimary,
                            inactiveBackgroundColor = skin.palette.canvasPrimary,
                            activeBorderColor = skin.palette.accentPrimary,
                            inactiveBorderColor = skin.palette.borderHairline,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 38.dp),
                            testTag = "bodymap_asymmetry_${option.key}"
                        ) {
                            Text(
                                text = "${option.labelCn} ${option.ratioBadgeCn}",
                                style = SongxiTabularNumericStyle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Live 3-Stage Clinical Prescription Preview (解绑 -> 深舒 -> 归位)
                if (previewRoutine != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(subCardShape)
                            .background(skin.palette.surfaceCardSubtle, subCardShape)
                            .border(1.dp, skin.palette.borderHairline, subCardShape)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("bodymap_prescription_preview")
                    ) {
                        Text(
                            text = "三阶处方序 · 滑液解绑 → 靶向松解 → 归位沉降",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skin.palette.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PrescriptionPhaseRole.entries.forEach { role ->
                            val matching = previewRoutine.poses.filter { it.clinical.phaseRole == role }
                            if (matching.isNotEmpty()) {
                                Text(
                                    text = "${role.shortLabelCn}｜" + matching.joinToString(" · ") {
                                        "${it.title}(${it.durationSeconds}s)"
                                    },
                                    style = SongxiTabularNumericStyle,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = skin.palette.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row: Compose & Start + Save as Favorite Preset ("存常备方")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongxiActionSurface(
                        onClick = onComposeAndStart,
                        shape = CircleShape,
                        backgroundColor = skin.palette.accentPrimary,
                        modifier = Modifier
                            .weight(1.45f)
                            .defaultMinSize(minHeight = 46.dp),
                        testTag = "bodymap_compose_start_button"
                    ) {
                        Text(
                            text = "一秒成方 · 开始 ${selectedDurationMinutes} 分钟",
                            style = SongxiTabularNumericStyle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skin.palette.onAccent,
                            maxLines = 1
                        )
                    }

                    SongxiActionSurface(
                        onClick = onSaveCurrentPreset,
                        shape = CircleShape,
                        backgroundColor = skin.palette.canvasPrimary,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier
                            .weight(0.65f)
                            .defaultMinSize(minHeight = 46.dp),
                        testTag = "bodymap_save_preset_button"
                    ) {
                        Text(
                            text = "存常备方",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Saved Presets List ("我的常备方")
                if (savedPresets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bodymap_saved_presets_row")
                    ) {
                        Text(
                            text = "我的常备方（点击即练）",
                            fontSize = 11.sp,
                            color = skin.palette.inkTertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        savedPresets.forEachIndexed { idx, preset ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongxiActionSurface(
                                    onClick = { onStartSavedPreset(preset) },
                                    shape = CircleShape,
                                    backgroundColor = skin.palette.surfaceCardSubtle,
                                    borderColor = skin.palette.borderHairline,
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 38.dp),
                                    testTag = "bodymap_preset_chip_$idx"
                                ) {
                                    Text(
                                        text = "常备｜${preset.title}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = skin.palette.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                                SongxiActionSurface(
                                    onClick = { onDeleteSavedPreset(preset.id) },
                                    shape = CircleShape,
                                    backgroundColor = skin.palette.canvasPrimary,
                                    borderColor = skin.palette.borderHairline,
                                    modifier = Modifier.defaultMinSize(minWidth = 42.dp, minHeight = 38.dp),
                                    testTag = "bodymap_preset_delete_$idx"
                                ) {
                                    Text(
                                        text = "移除",
                                        fontSize = 10.sp,
                                        color = skin.palette.inkSecondary
                                    )
                                }
                            }
                            if (idx < savedPresets.lastIndex) {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Classical Curated Booklets (四时雅集)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "四时雅集",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary
                )
                Text(
                    text = "全册免费开放 · 可自选章节预习",
                    fontSize = 11.sp,
                    color = skin.palette.inkTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            booklets.take(4).forEachIndexed { index, booklet ->
                val completedDays = bookletCompletedDays[booklet.id].orEmpty()
                BookletSeriesCard(
                    booklet = booklet,
                    index = index,
                    completedCount = completedDays.size,
                    shape = subCardShape,
                    onClick = { onStartBooklet(index) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SongxiActionSurface(
                        onClick = { onOpenBookletChapters(index) },
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCardSubtle,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp),
                        testTag = "booklet_chapters_button_$index"
                    ) {
                        Text(
                            text = "选节目录 · 动作预习 ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                if (index < booklets.lastIndex.coerceAtMost(3)) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Booklet Chapter & Pose Preview Sheet ("雅集选节目录")
        val targetIdx = inspectedBookletIndex
        if (targetIdx != null && targetIdx in booklets.indices) {
            val targetBooklet = booklets[targetIdx]
            val completedSet = bookletCompletedDays[targetBooklet.id].orEmpty()
            BookletChapterDetailOverlay(
                booklet = targetBooklet,
                completedDays = completedSet,
                onSelectDay = { dayIndex -> onStartBookletSpecificDay(targetIdx, dayIndex) },
                onDismiss = onCloseBookletChapters
            )
        }
    }
}

@Composable
private fun BookletSeriesCard(
    booklet: BookletSeries,
    index: Int,
    completedCount: Int,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val skin = LocalSongxiSkin.current
    val calmSubtitle = sanitizeCalmSubtitle(booklet.subtitle)
    val progressLabel = "已习 $completedCount / ${booklet.totalDays} 节"

    SongxiActionSurface(
        onClick = onClick,
        shape = shape,
        backgroundColor = skin.palette.surfaceCard,
        borderColor = skin.palette.borderHairline,
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp)
            .semantics(mergeDescendants = true) {},
        testTag = "booklet_card_$index"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Classical thread-bound book spine accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(skin.palette.accentPrimary, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = booklet.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary,
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
                            text = progressLabel,
                            style = SongxiTabularNumericStyle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = calmSubtitle,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = skin.palette.inkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "共 ${booklet.totalDays} 节 · 每节 ${booklet.dailyMinutes} 分钟 · 随时暂歇续练亦无妨",
                    style = SongxiTabularNumericStyle,
                    fontSize = 11.sp,
                    color = skin.palette.inkTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BookletChapterDetailOverlay(
    booklet: BookletSeries,
    completedDays: Set<Int>,
    onSelectDay: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = LocalSongxiSkin.current
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
                .songxiGentleStageReveal(booklet.id)
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
                .testTag("booklet_detail_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "四时雅集 · 章节目录与动作预习",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary
                    )
                    Text(
                        text = booklet.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary
                    )
                }
                ClassicalSealBadge(skin = skin, size = 30.dp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "任选一节即可起息复练，无解锁门槛，亦不计连续打卡。",
                fontSize = 12.sp,
                color = skin.palette.inkSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            for (dayIdx in 0 until booklet.totalDays) {
                val routine = booklet.routineForDay(dayIdx)
                val isCompleted = dayIdx in completedDays
                SongxiActionSurface(
                    onClick = { onSelectDay(dayIdx) },
                    shape = subShape,
                    backgroundColor = skin.palette.surfaceCardSubtle,
                    borderColor = if (isCompleted) skin.palette.accentPrimary else skin.palette.borderHairline,
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp),
                    testTag = "booklet_day_item_$dayIdx"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = routine.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = skin.palette.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = if (isCompleted) "已习 · 复练 ›" else "起息 ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.accentPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = routine.poses.joinToString(" → ") { it.title },
                            fontSize = 11.sp,
                            color = skin.palette.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (dayIdx < booklet.totalDays - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SongxiActionSurface(
                onClick = onDismiss,
                shape = CircleShape,
                backgroundColor = skin.palette.canvasPrimary,
                borderColor = skin.palette.borderHairline,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp),
                testTag = "booklet_detail_close"
            ) {
                Text(
                    text = "收起目录",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = skin.palette.inkPrimary
                )
            }
        }
    }
}
