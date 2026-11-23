package com.songxi.app.feature.splashad

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.model.SplashAdCreative
import kotlinx.coroutines.delay

@Suppress("ModifierParameter")
@Composable
fun SplashAdScreen(
    creative: SplashAdCreative,
    countdownSeconds: Int = SplashAdPolicyEngine.DEFAULT_COUNTDOWN_SECONDS,
    onSkipOrFinish: () -> Unit,
    onExternalUrlConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
    onUserSkipped: (elapsedMs: Long) -> Unit = {},
    onCtaClicked: () -> Unit = {}
) {
    val skin = LocalSongxiSkin.current
    val safeCountdownSeconds = normalizeSplashDurationSeconds(countdownSeconds)
    val initialDurationMs = safeCountdownSeconds * 1_000L

    val latestOnSkipOrFinish by rememberUpdatedState(onSkipOrFinish)
    val latestOnExternalUrlConfirmed by rememberUpdatedState(onExternalUrlConfirmed)
    val latestOnUserSkipped by rememberUpdatedState(onUserSkipped)
    val latestOnCtaClicked by rememberUpdatedState(onCtaClicked)

    var remainingMs by remember(creative.id, initialDurationMs) { mutableLongStateOf(initialDurationMs) }
    var elapsedActiveMs by remember(creative.id, initialDurationMs) { mutableLongStateOf(0L) }
    var isConfirmSheetVisible by remember(creative.id) { mutableStateOf(false) }
    var hasFinished by remember(creative.id) { mutableStateOf(false) }

    val displayedSeconds = ((remainingMs + 999L) / 1_000L).toInt().coerceAtLeast(1)

    fun finishOnce(isManualSkip: Boolean) {
        if (hasFinished) return
        hasFinished = true
        isConfirmSheetVisible = false
        if (isManualSkip) {
            latestOnUserSkipped(elapsedActiveMs)
        }
        latestOnSkipOrFinish()
    }

    LaunchedEffect(creative.id, initialDurationMs, isConfirmSheetVisible, hasFinished) {
        while (remainingMs > 0L && !isConfirmSheetVisible && !hasFinished) {
            delay(COUNTDOWN_TICK_MS)
            if (!isConfirmSheetVisible && !hasFinished) {
                remainingMs = (remainingMs - COUNTDOWN_TICK_MS).coerceAtLeast(0L)
                elapsedActiveMs += COUNTDOWN_TICK_MS
            }
        }
        if (remainingMs <= 0L && !isConfirmSheetVisible && !hasFinished) {
            finishOnce(isManualSkip = false)
        }
    }

    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(12, 32).dp

    // Exact 78% / 22% vertical framing (zero spacer between backdrop and footer).
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("splash_ad_screen")
    ) {
        // Upper 78%: Gallery-framed Xuan/silk creative stage (strictly inert backdrop).
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.78f)
                .testTag("splash_ad_backdrop"),
            shape = RoundedCornerShape(cardRadiusDp),
            color = skin.palette.surfaceCard,
            border = BorderStroke(1.dp, skin.palette.borderHairline)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header row: unmistakable ad badge + immediate t=0ms >=48dp skip button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(skin.palette.surfaceCardSubtle, CircleShape)
                                .border(1.dp, skin.palette.borderHairline, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .semantics(mergeDescendants = true) {}
                                .testTag("splash_ad_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "广告 · 开屏雅鉴",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = skin.palette.inkPrimary,
                                maxLines = 1
                            )
                        }

                        Surface(
                            onClick = { finishOnce(isManualSkip = true) },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                                .testTag("splash_ad_skip_button"),
                            shape = CircleShape,
                            color = skin.palette.canvasPrimary,
                            border = BorderStroke(1.dp, skin.palette.borderHairline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "跳过 ${displayedSeconds}s",
                                    style = SongxiTabularNumericStyle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = skin.palette.inkPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Center stage: inert Eastern craft artwork canvas
                    SplashCreativeArtworkCanvas(
                        artworkStyle = creative.artworkTheme,
                        skin = skin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    )

                    // Editorial copy + bounded CTA pill
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = creative.sponsorName,
                            fontSize = 11.sp,
                            color = skin.palette.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = creative.title,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skin.palette.inkPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (creative.subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = creative.subtitle,
                                fontSize = 12.sp,
                                color = skin.palette.inkSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            onClick = {
                                if (!isConfirmSheetVisible && !hasFinished) {
                                    isConfirmSheetVisible = true
                                    latestOnCtaClicked()
                                }
                            },
                            enabled = !isConfirmSheetVisible && !hasFinished,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                                .testTag("splash_ad_cta_button"),
                            shape = CircleShape,
                            color = skin.palette.accentPrimary
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = creative.ctaLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = skin.palette.onAccent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // In-hierarchy non-merging confirmation sheet (pauses countdown while visible).
                if (isConfirmSheetVisible) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(10.dp)
                            .testTag("splash_ad_confirm_sheet"),
                        shape = RoundedCornerShape(18.dp),
                        color = skin.palette.surfaceCard,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, skin.palette.accentPrimary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "即将离开「松息」前往外部浏览器",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = skin.palette.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${creative.sponsorName} · ${creative.landingSummary}",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = skin.palette.inkSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { isConfirmSheetVisible = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("splash_ad_confirm_dismiss"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = skin.palette.accentPrimary
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "返回松息 · 不跳转",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = skin.palette.onAccent,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        val targetUrl = creative.landingUrl.trim()
                                        if (SplashAdComplianceValidator.isSafeHttpsLandingUrl(targetUrl)) {
                                            finishOnce(isManualSkip = false)
                                            latestOnExternalUrlConfirmed(targetUrl)
                                        } else {
                                            isConfirmSheetVisible = false
                                        }
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                                        .testTag("splash_ad_confirm_proceed"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = skin.palette.surfaceCardSubtle,
                                    border = BorderStroke(1.dp, skin.palette.borderHairline)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "继续前往",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = skin.palette.inkPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lower 22%: Brand seal sanctuary footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.22f)
                .padding(horizontal = 6.dp)
                .semantics(mergeDescendants = true) {}
                .testTag("splash_ad_brand_footer"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClassicalSealBadge(
                    skin = skin,
                    size = 36.dp
                )
                Column {
                    Text(
                        text = "松息 Sōngxī",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.inkPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "一松一息，身自舒展 · 全功能永久免费开放",
                        fontSize = 11.sp,
                        color = skin.palette.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "零摇一摇 · 静音雅鉴",
                fontSize = 10.sp,
                color = skin.palette.inkTertiary,
                maxLines = 1
            )
        }
    }
}

private const val COUNTDOWN_TICK_MS: Long = 100L
