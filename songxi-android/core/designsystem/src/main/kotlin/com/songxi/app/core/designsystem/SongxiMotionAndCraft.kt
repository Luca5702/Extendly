package com.songxi.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

/**
 * Tabular numeric font feature (`tnum`) preventing horizontal digit jitter on ticking clocks & badges.
 */
val SongxiTabularNumericStyle: TextStyle = TextStyle(fontFeatureSettings = "tnum")

/**
 * Tactile physical press micro-interaction: gently compresses surface to [pressedScale] on touch-down
 * and springs back organically on release without mutating layout bounds or semantics.
 */
fun Modifier.songxiTactilePress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.974f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "songxi_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Non-blocking Xuan-paper entrance choreography (`graphicsLayer` alpha + subtle vertical settle).
 * Preserves 0-frame atomic semantics transitions while eliminating harsh visual cuts across tabs/sheets.
 */
@Composable
fun Modifier.songxiGentleStageReveal(
    stageKey: Any?,
    durationMillis: Int = 180
): Modifier {
    val revealProgress = remember(stageKey) { Animatable(0.88f) }
    LaunchedEffect(stageKey) {
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }
    return this.graphicsLayer {
        val p = revealProgress.value.coerceIn(0.88f, 1f)
        val norm = ((p - 0.88f) / 0.12f).coerceIn(0f, 1f)
        alpha = 0.86f + 0.14f * norm
        translationY = (1f - norm) * 6f
    }
}
