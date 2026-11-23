package com.songxi.app.feature.sanctuary

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.data.MonthlyRippleSummary
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SomaticFeeling
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAY_HEADERS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun PracticeJournalScreen(
    viewModel: SanctuaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val skin = LocalSongxiSkin.current

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentClockMonth()
    }

    PracticeJournalScreen(
        month = uiState.displayedMonth,
        today = viewModel.clockProvider.currentLocalDate(),
        monthRipples = uiState.monthRipples,
        monthSummary = uiState.monthSummary,
        showSharePreviewDialog = uiState.showSharePreviewDialog,
        onOpenShareDialog = viewModel::openSharePreviewDialog,
        onDismissShareDialog = viewModel::dismissSharePreviewDialog,
        onDispatchShare = {
            val shareText = buildString {
                appendLine("【松息日笺 · ${skin.meta.titleCn}】")
                appendLine(skin.meta.completionVerse)
                appendLine(
                    "本月舒展 ${uiState.monthSummary.totalSessions} 息 · 共 ${uiState.monthSummary.totalMinutesRoundedUp} 分钟"
                )
                append("一松一息，身自舒展。")
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "松息日笺")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "分享松息日笺").apply {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${month.year}年${month.monthValue}月 · 留白亦是休息",
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
                ClassicalSealBadge(skin = skin, size = 32.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calm streak-free monthly summary card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(14.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("journal_monthly_summary")
            ) {
                Text(
                    text = "本月舒展 ${monthSummary.totalSessions} 息 · 共 ${monthSummary.totalMinutesRoundedUp} 分钟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "留白亦是休息 · 落笔 ${monthSummary.practicedDaysCount} 日，从不计算任何连续天数",
                    fontSize = 12.sp,
                    color = skin.palette.inkSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Xuan-paper concentric water-ripple calendar grid
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

                val firstDayOffset = month.atDay(1).dayOfWeek.value - 1 // Monday = 0 .. Sunday = 6
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
                                WaterRippleDayCell(
                                    dayNumber = dayNum,
                                    sessionCount = daySessions.size,
                                    isToday = isToday,
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
                    text = "生成松息日笺 · 落印留存",
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

            // Practiced day somatic notes list
            val allMonthSessions = monthRipples.entries
                .sortedByDescending { it.key }
                .flatMap { (_, list) -> list }

            Text(
                text = "本月下旬 · 身息小记",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.palette.inkPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (allMonthSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(skin.palette.surfaceCardSubtle, subCardShape)
                        .border(1.dp, skin.palette.borderHairline, subCardShape)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "素宣留白，静候第一息舒展。忙时搁笔亦是松息。",
                        fontSize = 12.sp,
                        color = skin.palette.inkSecondary
                    )
                }
            } else {
                allMonthSessions.take(8).forEachIndexed { idx, session ->
                    val feelingText = session.feeling?.labelCn ?: "静默留白"
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
                                text = "${session.dateIso} · ${((session.durationSeconds + 30) / 60).coerceAtLeast(1)}分钟",
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
                    if (idx < allMonthSessions.take(8).lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // In-hierarchy Shareable Poetic Card Preview Overlay (松息日笺)
        if (showSharePreviewDialog) {
            BackHandler(onBack = onDismissShareDialog)
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
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(skin.palette.surfaceCard, cardShape)
                        .border(1.dp, skin.palette.borderHairline, cardShape)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {}
                        )
                        .padding(20.dp)
                        .testTag("journal_share_preview_dialog"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClassicalSealBadge(skin = skin, size = 36.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "松息日笺 · ${skin.meta.titleCn}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = skin.meta.completionVerse,
                        fontSize = 15.sp,
                        color = skin.palette.accentPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${today} · 本月舒展 ${monthSummary.totalSessions} 息（${monthSummary.totalMinutesRoundedUp} 分钟）",
                        fontSize = 12.sp,
                        color = skin.palette.inkSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SanctuarySurfaceButton(
                            onClick = onDismissShareDialog,
                            shape = CircleShape,
                            backgroundColor = skin.palette.surfaceCardSubtle,
                            borderColor = skin.palette.borderHairline,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 46.dp)
                        ) {
                            Text(
                                text = "关闭",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.inkPrimary
                            )
                        }

                        SanctuarySurfaceButton(
                            onClick = onDispatchShare,
                            shape = CircleShape,
                            backgroundColor = skin.palette.accentPrimary,
                            modifier = Modifier
                                .weight(1.3f)
                                .defaultMinSize(minHeight = 46.dp)
                        ) {
                            Text(
                                text = "分享日笺",
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
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val rippleColor = skin.palette.accentPrimary
    val fillSoft = skin.palette.accentPrimarySoft
    val todayRingColor = skin.palette.sealVermilion

    Box(
        modifier = modifier.height(38.dp),
        contentAlignment = Alignment.Center
    ) {
        if (sessionCount > 0 || isToday) {
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
            }
        }

        Text(
            text = dayNumber.toString(),
            fontSize = 12.sp,
            fontWeight = if (sessionCount > 0 || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = if (sessionCount > 0) skin.palette.accentPrimary else skin.palette.inkPrimary
        )
    }
}
