package com.songxi.app.feature.sanctuary

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
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.ShanShuiBackdropCanvas
import com.songxi.app.core.designsystem.SongxiSkin
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.model.SkinId

@Composable
fun SkinGalleryScreen(
    viewModel: SanctuaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effectiveSkin = LocalSongxiSkin.current

    SkinGalleryScreen(
        selectedSkinId = uiState.selectedSkinId,
        autoNightEnabled = uiState.autoNightSkinEnabled,
        onSelectSkin = viewModel::selectSkin,
        onPreviewChime = { viewModel.previewActiveSkinChime(effectiveSkin.id) },
        onAutoNightChanged = viewModel::setAutoNightSkin,
        modifier = modifier
    )
}

@Suppress("ModifierParameter")
@Composable
fun SkinGalleryScreen(
    selectedSkinId: SkinId,
    autoNightEnabled: Boolean,
    onSelectSkin: (SkinId) -> Unit,
    onPreviewChime: () -> Unit,
    onAutoNightChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSkin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = activeSkin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(activeSkin.palette.canvasPrimary)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("skins_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "可替换五维皮肤 · 四境永久免费",
                    fontSize = 12.sp,
                    color = activeSkin.palette.inkSecondary
                )
                Text(
                    text = "山居画境",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = activeSkin.meta.titleLetterSpacingSp.sp,
                    color = activeSkin.palette.inkPrimary
                )
            }
            ClassicalSealBadge(skin = activeSkin, size = 32.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Audition active skin's harmonic chime
        SanctuarySurfaceButton(
            onClick = onPreviewChime,
            shape = CircleShape,
            backgroundColor = activeSkin.palette.surfaceCard,
            borderColor = activeSkin.palette.borderHairline,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            testTag = "skin_chime_preview_button"
        ) {
            Text(
                text = "试听当前画境泛音 · ${activeSkin.sensory.defaultSoundProfile.labelCn} (${activeSkin.sensory.baseFrequencyHz.toInt()}Hz)",
                style = SongxiTabularNumericStyle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = activeSkin.palette.accentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        SanctuarySwitchRow(
            title = "昼夜随顺换境",
            subtitle = "晚间 20:00 至清晨 08:00 自动切换「暮山听松」低眩光夜境，日间恢复自选画境",
            checked = autoNightEnabled,
            onCheckedChange = onAutoNightChanged,
            testTag = "skin_auto_night_switch"
        )

        Spacer(modifier = Modifier.height(14.dp))

        BuiltInSkinRegistry.allSkins.forEachIndexed { index, candidateSkin ->
            val isSelected = candidateSkin.id == selectedSkinId
            SkinGalleryCard(
                candidateSkin = candidateSkin,
                cardIndex = index,
                isSelected = isSelected,
                cardShape = cardShape,
                onClick = { onSelectSkin(candidateSkin.id) }
            )
            if (index < BuiltInSkinRegistry.allSkins.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun SkinGalleryCard(
    candidateSkin: SongxiSkin,
    cardIndex: Int,
    isSelected: Boolean,
    cardShape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val activeSkin = LocalSongxiSkin.current
    val borderColor = if (isSelected) activeSkin.palette.accentPrimary else activeSkin.palette.borderHairline

    SanctuarySurfaceButton(
        onClick = onClick,
        shape = cardShape,
        backgroundColor = activeSkin.palette.surfaceCard,
        borderColor = borderColor,
        selectedState = isSelected,
        contentAlignment = Alignment.TopStart,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        testTag = "skin_card_${candidateSkin.id.storageKey}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, candidateSkin.palette.borderHairline, RoundedCornerShape(14.dp))
            ) {
                ShanShuiBackdropCanvas(
                    skin = candidateSkin,
                    driftPhase = 0.18f + cardIndex * 0.15f,
                    modifier = Modifier.fillMaxSize()
                )
                ClassicalSealBadge(
                    skin = candidateSkin,
                    size = 28.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) candidateSkin.palette.accentPrimary else candidateSkin.palette.surfaceCard,
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSelected) "当前画境" else "免费换境",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) candidateSkin.palette.onAccent else candidateSkin.palette.inkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${candidateSkin.meta.titleCn} · ${candidateSkin.meta.subtitleCn}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeSkin.palette.inkPrimary
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(candidateSkin.palette.accentPrimary, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${candidateSkin.meta.fusionBadge} · ${candidateSkin.sensory.defaultSoundProfile.labelCn}",
                fontSize = 12.sp,
                color = activeSkin.palette.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
