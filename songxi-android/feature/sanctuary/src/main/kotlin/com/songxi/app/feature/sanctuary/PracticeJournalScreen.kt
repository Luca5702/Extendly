package com.songxi.app.feature.sanctuary

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.data.MonthlyRippleSummary
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiSharePosterRenderer
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.designsystem.songxiGentleStageReveal
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SomaticFeeling
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_HEADERS = listOf("一", "二", "三", "四", "五", "六", "日")

internal fun resolveJournalNotesSectionTitle(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?
): String {
    if (selectedDate != null) {
        return "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 · 当日身息小记"
    }
    val currentMonth = YearMonth.from(today)
    if (month == currentMonth) {
        val dekad = when {
            today.dayOfMonth <= 10 -> "上旬"
            today.dayOfMonth <= 20 -> "中旬"
            else -> "下旬"
        }
        return "本月$dekad · 身息小记"
    }
    return "${month.year}年${month.monthValue}月 · 身息小记"
}

@Composable
fun PracticeJournalScreen(
    viewModel: SanctuaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val skin = LocalSongxiSkin.current
    var savePosterFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentClockMonth()
    }

    val topRegionNames = remember(uiState.reliefAtlas) {
        BodyRegion.entries
            .filter { (uiState.reliefAtlas.regionCounts[it] ?: 0) > 0 }
            .sortedByDescending { uiState.reliefAtlas.regionCounts[it] ?: 0 }
            .take(2)
            .joinToString(" · ") { it.labelCn }
            .ifBlank { null }
    }

    val daySessionCounts = remember(uiState.monthRipples) {
        uiState.monthRipples.entries.associate { (date, list) -> date.dayOfMonth to list.size }
    }

    PracticeJournalScreen(
        month = uiState.displayedMonth,
        today = viewModel.clockProvider.currentLocalDate(),
        selectedDate = uiState.selectedJournalDate,
        monthRipples = uiState.monthRipples,
        monthSummary = uiState.monthSummary,
        showSharePreviewDialog = uiState.showSharePreviewDialog,
        onOpenShareDialog = {
            savePosterFeedback = null
            viewModel.openSharePreviewDialog()
        },
        onDismissShareDialog = viewModel::dismissSharePreviewDialog,
        onNavigatePrevMonth = { viewModel.navigateJournalMonth(-1L) },
        onNavigateNextMonth = { viewModel.navigateJournalMonth(1L) },
        onReturnToCurrentMonth = viewModel::returnJournalToCurrentMonth,
        onSelectDay = viewModel::toggleSelectedJournalDate,
        onClearSelectedDay = viewModel::clearSelectedJournalDate,
        posterSaveFeedback = savePosterFeedback,
        onSavePosterToAlbum = { bitmap ->
            val savedOk = savePosterBitmapToGalleryOrFiles(context, bitmap, uiState.displayedMonth)
            savePosterFeedback = if (savedOk) {
                "已存至本地画册 · 松息日笺 (${uiState.displayedMonth})"
            } else {
                "已缓存于应用画匣"
            }
        },
        onDispatchShare = {
            val posterBitmap = SongxiSharePosterRenderer.renderPosterBitmap(
                skin = skin,
                month = uiState.displayedMonth,
                totalSessions = uiState.monthSummary.totalSessions,
                totalMinutes = uiState.monthSummary.totalMinutesRoundedUp,
                practicedDaysCount = uiState.monthSummary.practicedDaysCount,
                daySessionCounts = daySessionCounts,
                topCaredRegionsLabel = topRegionNames
            )
            val pngFile = runCatching {
                SongxiSharePosterRenderer.writePosterToAppCache(context, posterBitmap, uiState.displayedMonth)
            }.getOrNull()
            val contentUri = pngFile?.let { file ->
                runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrNull()
            }

            val shareText = buildString {
                appendLine("【松息日笺 · ${skin.meta.titleCn}】")
                appendLine(skin.meta.completionVerse)
                appendLine(
                    "本月舒展 ${uiState.monthSummary.totalSessions} 息 · 共 ${uiState.monthSummary.totalMinutesRoundedUp} 分钟"
                )
                append("一松一息，身自舒展。")
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                if (contentUri != null) {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    clipData = ClipData.newRawUri("松息日笺", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_SUBJECT, "松息日笺")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "分享松息日笺").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        },
        reliefAtlas = uiState.reliefAtlas,
        modifier = modifier
    )
}

@Suppress("ModifierParameter")
@Composable
fun PracticeJournalScreen(
    month: YearMonth,
    today: LocalDate,
    monthRipples: Map<LocalDate, List<PracticeSessionRecord>>,
    monthSummary: MonthlyRippleSummary,
    showSharePreviewDialog: Boolean,
    onOpenShareDialog: () -> Unit,
    onDismissShareDialog: () -> Unit,
    onDispatchShare: () -> Unit,
    reliefAtlas: MonthlyBodyReliefAtlas = computeMonthlyBodyReliefAtlas(monthRipples),
    selectedDate: LocalDate? = null,
    onNavigatePrevMonth: () -> Unit = {},
    onNavigateNextMonth: () -> Unit = {},
    onReturnToCurrentMonth: () -> Unit = {},
    onSelectDay: (LocalDate) -> Unit = {},
    onClearSelectedDay: () -> Unit = {},
    posterSaveFeedback: String? = null,
    onSavePosterToAlbum: (Bitmap) -> Unit = {},
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
            .testTag("journal_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Header row with compact Cross-Month Switcher (< 上月 / 下月 >) adding 0dp vertical height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${month.year}年${month.monthValue}月 · 留白亦是休息",
                        style = SongxiTabularNumericStyle,
                        fontSize = 12.sp,
                        color = skin.palette.inkSecondary
                    )
                    Text(
                        text = "松息手札",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = skin.meta.titleLetterSpacingSp.sp,
                        color = skin.palette.inkPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SanctuarySurfaceButton(
                        onClick = onNavigatePrevMonth,
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCard,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minWidth = 38.dp, minHeight = 34.dp),
                        testTag = "journal_prev_month_button"
                    ) {
                        Text(
                            text = "‹ 上月",
                            fontSize = 11.sp,
                            color = skin.palette.inkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    if (month != YearMonth.from(today)) {
                        SanctuarySurfaceButton(
                            onClick = onReturnToCurrentMonth,
                            shape = CircleShape,
                            backgroundColor = skin.palette.accentPrimarySoft,
                            borderColor = skin.palette.accentPrimary,
                            modifier = Modifier.defaultMinSize(minHeight = 34.dp),
                            testTag = "journal_current_month_button"
                        ) {
                            Text(
                                text = "今月",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.accentPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    SanctuarySurfaceButton(
                        onClick = onNavigateNextMonth,
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCard,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minWidth = 38.dp, minHeight = 34.dp),
                        testTag = "journal_next_month_button"
                    ) {
                        Text(
                            text = "下月 ›",
                            fontSize = 11.sp,
                            color = skin.palette.inkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    ClassicalSealBadge(skin = skin, size = 30.dp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calm streak-free monthly summary card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .songxiGentleStageReveal(month)
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(14.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("journal_monthly_summary")
            ) {
                Text(
                    text = "本月舒展 ${monthSummary.totalSessions} 息 · 共 ${monthSummary.totalMinutesRoundedUp} 分钟",
                    style = SongxiTabularNumericStyle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "留白亦是休息 · 落笔 ${monthSummary.practicedDaysCount} 日，从不计算任何连续天数",
                    style = SongxiTabularNumericStyle,
                    fontSize = 12.sp,
                    color = skin.palette.inkSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Xuan-paper concentric water-ripple calendar grid (interactive day inspection)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(12.dp)
                    .testTag("journal_calendar_grid")
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    WEEKDAY_HEADERS.forEach { header ->
                        Text(
                            text = header,
                            fontSize = 11.sp,
                            color = skin.palette.inkTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
                val daysInMonth = month.lengthOfMonth()
                val totalSlots = ((firstDayOffset + daysInMonth + 6) / 7) * 7

                for (weekStart in 0 until totalSlots step 7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0 until 7) {
                            val slot = weekStart + col
                            val dayNum = slot - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = month.atDay(dayNum)
                                val daySessions = monthRipples[date].orEmpty()
                                val isToday = date == today
                                val isSelected = date == selectedDate
                                WaterRippleDayCell(
                                    dayNumber = dayNum,
                                    sessionCount = daySessions.size,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    onClick = { onSelectDay(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(38.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SanctuarySurfaceButton(
                onClick = onOpenShareDialog,
                shape = CircleShape,
                backgroundColor = skin.palette.accentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                testTag = "journal_share_card_button"
            ) {
                Text(
                    text = "生成松息日笺 · 宣纸水纹画笺",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.onAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Streak-Free Monthly Body Relief Atlas ("月度身体释压图谱")
            BodyReliefAtlasCard(
                reliefAtlas = reliefAtlas,
                cardShape = cardShape,
                subCardShape = subCardShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Practiced day somatic notes list (filtered by selectedDate if tapped, else full month)
            val displayedSessions = if (selectedDate != null) {
                monthRipples[selectedDate].orEmpty()
            } else {
                monthRipples.entries
                    .sortedByDescending { it.key }
                    .flatMap { (_, list) -> list }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resolveJournalNotesSectionTitle(month, today, selectedDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary,
                    modifier = Modifier.testTag("journal_notes_section_title")
                )

                if (selectedDate != null) {
                    SanctuarySurfaceButton(
                        onClick = onClearSelectedDay,
                        shape = CircleShape,
                        backgroundColor = skin.palette.surfaceCardSubtle,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                        testTag = "journal_clear_day_filter"
                    ) {
                        Text(
                            text = "查看全月",
                            fontSize = 11.sp,
                            color = skin.palette.accentPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayedSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(skin.palette.surfaceCardSubtle, subCardShape)
                        .border(1.dp, skin.palette.borderHairline, subCardShape)
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (selectedDate != null) {
                            "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日素宣留白，未落水纹。静歇亦是养身。"
                        } else {
                            "素宣留白，静候第一息舒展。忙时搁笔亦是松息。"
                        },
                        fontSize = 12.sp,
                        color = skin.palette.inkSecondary
                    )
                }
            } else {
                displayedSessions.take(10).forEachIndexed { idx, session ->
                    val feelingText = session.feeling?.labelCn ?: "静默留白"
                    val asymBadge = if (session.asymmetry == LateralAsymmetry.BALANCED) {
                        ""
                    } else {
                        " · ${session.asymmetry.labelCn}"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(subCardShape)
                            .background(skin.palette.surfaceCard, subCardShape)
                            .border(1.dp, skin.palette.borderHairline, subCardShape)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = session.routineTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.inkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${session.dateIso} · ${((session.durationSeconds + 30) / 60).coerceAtLeast(1)}分钟$asymBadge",
                                style = SongxiTabularNumericStyle,
                                fontSize = 11.sp,
                                color = skin.palette.inkTertiary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(skin.palette.accentPrimarySoft, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = feelingText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.accentPrimary
                            )
                        }
                    }
                    if (idx < displayedSessions.take(10).lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // In-hierarchy Shareable Poetic Poster Preview Overlay ("松息日笺 · 高清宣纸海报")
        if (showSharePreviewDialog) {
            BackHandler(onBack = onDismissShareDialog)
            val posterContext = LocalContext.current
            val dayCounts = remember(monthRipples) {
                monthRipples.entries.associate { (d, list) -> d.dayOfMonth to list.size }
            }
            val topRegionsLabel = remember(reliefAtlas) {
                BodyRegion.entries
                    .filter { (reliefAtlas.regionCounts[it] ?: 0) > 0 }
                    .sortedByDescending { reliefAtlas.regionCounts[it] ?: 0 }
                    .take(2)
                    .joinToString(" · ") { it.labelCn }
                    .ifBlank { null }
            }
            val posterBitmap = remember(skin, month, monthSummary, dayCounts, topRegionsLabel, posterContext) {
                SongxiSharePosterRenderer.renderPosterBitmap(
                    skin = skin,
                    month = month,
                    totalSessions = monthSummary.totalSessions,
                    totalMinutes = monthSummary.totalMinutesRoundedUp,
                    practicedDaysCount = monthSummary.practicedDaysCount,
                    daySessionCounts = dayCounts,
                    topCaredRegionsLabel = topRegionsLabel,
                    context = posterContext
                )
            }
            val dialogScroll = rememberScrollState()
            val backdropInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(skin.palette.surfaceOverlay)
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null,
                        onClick = onDismissShareDialog
                    )
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .songxiGentleStageReveal(month)
                        .clip(cardShape)
                        .background(skin.palette.surfaceCard, cardShape)
                        .border(1.dp, skin.palette.borderHairline, cardShape)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {}
                        )
                        .verticalScroll(dialogScroll)
                        .padding(14.dp)
                        .testTag("journal_share_preview_dialog"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "松息日笺 · ${skin.meta.titleCn}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = skin.meta.completionVerse,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 12.sp,
                        color = skin.palette.accentPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Rendered High-DPI Xuan-Paper Poster Preview Image
                    Image(
                        bitmap = posterBitmap.asImageBitmap(),
                        contentDescription = "松息日笺宣纸水纹海报预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(subCardShape)
                            .border(1.dp, skin.palette.borderHairline, subCardShape)
                            .testTag("journal_share_poster_preview")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${month.year}年${month.monthValue}月 · 舒展 ${monthSummary.totalSessions} 息（${monthSummary.totalMinutesRoundedUp} 分钟）",
                        style = SongxiTabularNumericStyle,
                        fontSize = 11.sp,
                        color = skin.palette.inkSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (!posterSaveFeedback.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = posterSaveFeedback,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.accentPrimary,
                            modifier = Modifier.testTag("journal_poster_save_feedback")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SanctuarySurfaceButton(
                            onClick = onDismissShareDialog,
                            shape = CircleShape,
                            backgroundColor = skin.palette.surfaceCardSubtle,
                            borderColor = skin.palette.borderHairline,
                            modifier = Modifier
                                .weight(0.75f)
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(
                                text = "关闭",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.inkPrimary
                            )
                        }

                        SanctuarySurfaceButton(
                            onClick = { onSavePosterToAlbum(posterBitmap) },
                            shape = CircleShape,
                            backgroundColor = skin.palette.canvasPrimary,
                            borderColor = skin.palette.borderHairline,
                            modifier = Modifier
                                .weight(0.95f)
                                .defaultMinSize(minHeight = 44.dp),
                            testTag = "journal_save_poster_button"
                        ) {
                            Text(
                                text = "存至画册",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.accentPrimary
                            )
                        }

                        SanctuarySurfaceButton(
                            onClick = onDispatchShare,
                            shape = CircleShape,
                            backgroundColor = skin.palette.accentPrimary,
                            modifier = Modifier
                                .weight(1.25f)
                                .defaultMinSize(minHeight = 44.dp),
                            testTag = "journal_share_confirm_button"
                        ) {
                            Text(
                                text = "分享画笺",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = skin.palette.onAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun savePosterBitmapToGalleryOrFiles(
    context: Context,
    bitmap: Bitmap,
    month: YearMonth
): Boolean {
    val displayName = "songxi_rijian_${month.year}_${month.monthValue}.png"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val savedMediaStore = runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Songxi")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                false
            }
        }.getOrDefault(false)
        if (savedMediaStore) return true
    }

    return runCatching {
        val dir = File(context.filesDir, "posters").apply { mkdirs() }
        FileOutputStream(File(dir, displayName)).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        true
    }.getOrDefault(false)
}

@Composable
private fun BodyReliefAtlasCard(
    reliefAtlas: MonthlyBodyReliefAtlas,
    cardShape: RoundedCornerShape,
    subCardShape: RoundedCornerShape
) {
    val skin = LocalSongxiSkin.current
    val topRegions = BodyRegion.entries
        .filter { (reliefAtlas.regionCounts[it] ?: 0) > 0 }
        .sortedByDescending { reliefAtlas.regionCounts[it] ?: 0 }
        .take(2)
    val summaryLine = if (topRegions.isNotEmpty()) {
        "重点温养：${topRegions.joinToString(" · ") { it.labelCn }} · 只观身息照拂，不设目标定额"
    } else {
        "素宣待染 · 完成任意一息即可显现六处肌群温养分布"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(skin.palette.surfaceCard, cardShape)
            .border(1.dp, skin.palette.borderHairline, cardShape)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("journal_body_relief_atlas")
    ) {
        Text(
            text = "月度身体释压图谱",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = skin.palette.inkPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = summaryLine,
            fontSize = 11.sp,
            color = skin.palette.inkSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        BodyRegion.entries.forEachIndexed { idx, region ->
            val count = reliefAtlas.regionCounts[region] ?: 0
            val fraction = if (count <= 0) {
                0.04f
            } else {
                (count.toFloat() / reliefAtlas.maxRegionCount.toFloat()).coerceIn(0.14f, 1f)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = region.labelCn,
                    fontSize = 11.sp,
                    color = skin.palette.inkPrimary,
                    maxLines = 1,
                    modifier = Modifier.width(76.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(skin.palette.surfaceCardSubtle, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (count > 0) skin.palette.accentPrimary else skin.palette.borderHairline,
                                CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${count}息",
                    style = SongxiTabularNumericStyle,
                    fontSize = 11.sp,
                    fontWeight = if (count > 0) FontWeight.Medium else FontWeight.Normal,
                    color = if (count > 0) skin.palette.accentPrimary else skin.palette.inkTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(30.dp)
                )
            }
            if (idx < BodyRegion.entries.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Somatic comfort check-in distribution pill bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(subCardShape)
                .background(skin.palette.surfaceCardSubtle, subCardShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SomaticFeeling.entries.forEach { feeling ->
                val fCount = reliefAtlas.feelingCounts[feeling] ?: 0
                Text(
                    text = "${feeling.labelCn} $fCount",
                    style = SongxiTabularNumericStyle,
                    fontSize = 11.sp,
                    color = if (fCount > 0) skin.palette.accentPrimary else skin.palette.inkSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WaterRippleDayCell(
    dayNumber: Int,
    sessionCount: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val rippleColor = skin.palette.accentPrimary
    val fillSoft = skin.palette.accentPrimarySoft
    val todayRingColor = skin.palette.sealVermilion
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("journal_day_cell_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        if (sessionCount > 0 || isToday || isSelected) {
            Canvas(modifier = Modifier.size(34.dp)) {
                val maxRadius = size.minDimension * 0.48f
                if (sessionCount > 0) {
                    drawCircle(
                        color = fillSoft,
                        radius = maxRadius * 0.82f
                    )
                    val rings = sessionCount.coerceIn(1, 3)
                    for (r in 1..rings) {
                        val frac = 0.45f + (r.toFloat() / rings.toFloat()) * 0.50f
                        drawCircle(
                            color = rippleColor.copy(alpha = 0.65f / r),
                            radius = maxRadius * frac,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                } else if (isToday) {
                    drawCircle(
                        color = todayRingColor.copy(alpha = 0.45f),
                        radius = maxRadius * 0.78f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                if (isSelected) {
                    drawCircle(
                        color = todayRingColor,
                        radius = maxRadius * 0.94f,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
        }

        Text(
            text = dayNumber.toString(),
            style = SongxiTabularNumericStyle,
            fontSize = 12.sp,
            fontWeight = if (sessionCount > 0 || isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isSelected -> skin.palette.sealVermilion
                sessionCount > 0 -> skin.palette.accentPrimary
                else -> skin.palette.inkPrimary
            }
        )
    }
}
