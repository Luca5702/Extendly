package com.songxi.app.feature.stretch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries

private val SUPPORTED_CUSTOM_MINUTES: List<Int> = listOf(2, 4, 6)

@Composable
fun BodyMapScreen(
    viewModel: StretchSessionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skin = LocalSongxiSkin.current
    val booklets = viewModel.catalogRepository.curatedBooklets

    BodyMapScreen(
        selectedRegions = uiState.selectedBodyRegions,
        selectedDurationMinutes = uiState.selectedDurationMinutes,
        booklets = booklets,
        bookletCompletedDays = uiState.bookletCompletedDays,
        onToggleRegion = viewModel::toggleBodyRegion,
        onSelectDuration = viewModel::selectCustomDuration,
        onComposeAndStart = { viewModel.composeAndStartCustomRoutine(skin.id) },
        onStartBooklet = { index -> viewModel.startBooklet(index, skin.id) },
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
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)
    val subCardShape = RoundedCornerShape((skin.palette.cardCornerRadiusDp - 6).coerceIn(12, 24).dp)

    Column(
        modifier = modifier
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
                    text = "按位寻音 · 随心成方",
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
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SongxiActionSurface(
                onClick = onComposeAndStart,
                shape = CircleShape,
                backgroundColor = skin.palette.accentPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp),
                testTag = "bodymap_compose_start_button"
            ) {
                Text(
                    text = "一秒成方 · 开始 ${selectedDurationMinutes} 分钟专属轻舒",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.onAccent,
                    maxLines = 1
                )
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
                text = "全册免费开放 · 留白随心",
                fontSize = 11.sp,
                color = skin.palette.inkTertiary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        booklets.take(4).forEachIndexed { index, booklet ->
            val completedCount = bookletCompletedDays[booklet.id]?.size ?: 0
            BookletSeriesCard(
                booklet = booklet,
                index = index,
                completedCount = completedCount,
                shape = subCardShape,
                onClick = { onStartBooklet(index) }
            )
            if (index < booklets.lastIndex.coerceAtMost(3)) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
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
                    fontSize = 11.sp,
                    color = skin.palette.inkTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
