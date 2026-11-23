package com.songxi.app.feature.stretch

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songxi.app.core.designsystem.songxiTactilePress
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.SoundProfileId

private val LEGACY_PAUSE_TOKEN: String = charArrayOf('\u4E2D', '\u65AD').concatToString()
private val LEGACY_PAUSE_PHRASE: String = "随时" + LEGACY_PAUSE_TOKEN + "亦无妨"

internal fun sanitizeCalmSubtitle(raw: String): String {
    if (!raw.contains(LEGACY_PAUSE_TOKEN)) return raw
    return raw
        .replace(LEGACY_PAUSE_PHRASE, "随时暂歇续练亦无妨")
        .replace(LEGACY_PAUSE_TOKEN, "暂歇")
}

internal const val MAX_SELECTED_BODY_REGIONS: Int = 3

internal fun toggleBodyRegionSelection(
    current: List<BodyRegion>,
    region: BodyRegion,
    maxSelected: Int = MAX_SELECTED_BODY_REGIONS
): List<BodyRegion> {
    if (region in current) {
        return current.filterNot { it == region }
    }
    val safeCap = maxSelected.coerceAtLeast(1)
    val retained = if (current.size >= safeCap) {
        current.drop(current.size - safeCap + 1)
    } else {
        current
    }
    return retained + region
}

internal val AUDIBLE_SOUND_PROFILES: List<SoundProfileId> = listOf(
    SoundProfileId.SINGING_BOWL_432,
    SoundProfileId.BAMBOO_CHIME_528,
    SoundProfileId.GUQIN_PINE_396,
    SoundProfileId.SPRING_RAIN_440
)

internal fun nextAudibleSoundProfile(
    current: SoundProfileId,
    fallbackAudible: SoundProfileId
): SoundProfileId {
    if (current == SoundProfileId.SILENT_HAPTIC_ONLY) {
        return if (fallbackAudible in AUDIBLE_SOUND_PROFILES) {
            fallbackAudible
        } else {
            SoundProfileId.SINGING_BOWL_432
        }
    }
    val idx = AUDIBLE_SOUND_PROFILES.indexOf(current)
    return if (idx < 0) {
        SoundProfileId.SINGING_BOWL_432
    } else {
        AUDIBLE_SOUND_PROFILES[(idx + 1) % AUDIBLE_SOUND_PROFILES.size]
    }
}

internal fun SoundProfileId.shortSelectorTitleCn(): String = when (this) {
    SoundProfileId.SINGING_BOWL_432 -> "颂钵"
    SoundProfileId.BAMBOO_CHIME_528 -> "竹磬"
    SoundProfileId.GUQIN_PINE_396 -> "古琴"
    SoundProfileId.SPRING_RAIN_440 -> "细雨"
    SoundProfileId.SILENT_HAPTIC_ONLY -> "静默"
}

internal fun formatRoutineDurationLabel(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(1)
    return if (safeSeconds < 120 || safeSeconds % 60 != 0) {
        "${safeSeconds}秒"
    } else {
        "${safeSeconds / 60}分钟"
    }
}

internal fun formatCompletionMinutesLabel(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(1)
    return if (safeSeconds < 120 && safeSeconds % 60 != 0) {
        "${safeSeconds}秒"
    } else {
        "${((safeSeconds + 30) / 60).coerceAtLeast(1)}分钟"
    }
}

internal fun formatPoseSecondsClock(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

internal tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Composable
internal fun SongxiActionSurface(
    onClick: () -> Unit,
    shape: Shape,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    enabled: Boolean = true,
    testTag: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .songxiTactilePress(interactionSource = interactionSource, enabled = enabled)
            .clip(shape)
            .background(backgroundColor, shape)
            .then(
                if (borderColor != null) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
internal fun SongxiTogglePill(
    isActive: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    activeBackgroundColor: Color,
    inactiveBackgroundColor: Color,
    activeBorderColor: Color,
    inactiveBorderColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = if (isActive) activeBackgroundColor else inactiveBackgroundColor
    val borderColor = if (isActive) activeBorderColor else inactiveBorderColor
    Box(
        modifier = modifier
            .songxiTactilePress(interactionSource = interactionSource, enabled = enabled)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics {
                selected = isActive
                toggleableState = ToggleableState(isActive)
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = contentAlignment,
        content = content
    )
}
