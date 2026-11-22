package com.songxi.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class BreathPhaseState(
    val isExhale: Boolean,
    val phaseProgress: Float,
    val expansion: Float,
    val phaseLabelCn: String
)

fun resolveBreathPhase(
    elapsedMs: Long,
    inhaleDurationMs: Int = 4_000,
    exhaleDurationMs: Int = 6_000
): BreathPhaseState {
    val safeInhale = inhaleDurationMs.toLong().coerceAtLeast(1L)
    val safeExhale = exhaleDurationMs.toLong().coerceAtLeast(1L)
    val cycleMs = safeInhale + safeExhale
    val pos = Math.floorMod(elapsedMs, cycleMs)
    return if (pos < safeInhale) {
        val p = (pos.toDouble() / safeInhale.toDouble()).toFloat().coerceIn(0f, 1f)
        val exp = 0.5f - 0.5f * cos(PI.toFloat() * p)
        BreathPhaseState(
            isExhale = false,
            phaseProgress = p,
            expansion = exp,
            phaseLabelCn = "吸气 · 轻展"
        )
    } else {
        val p = ((pos - safeInhale).toDouble() / safeExhale.toDouble()).toFloat().coerceIn(0f, 1f)
        val exp = 0.5f + 0.5f * cos(PI.toFloat() * p)
        BreathPhaseState(
            isExhale = true,
            phaseProgress = p,
            expansion = exp,
            phaseLabelCn = "呼气 · 沉肩"
        )
    }
}

@Composable
fun BreathingHaloCanvas(
    modifier: Modifier = Modifier,
    breathProgress: Float = 0.35f,
    isExhale: Boolean = false,
    skin: SongxiSkin = LocalSongxiSkin.current,
    eyesFreeMode: Boolean = false,
    completionRippleMode: Boolean = false,
    rippleCount: Int = 3,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val cloudPath = remember { Path() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (!w.isFinite() || !h.isFinite() || w <= 2f || h <= 2f) return@Canvas
            val minDim = min(w, h)
            val center = Offset(w * 0.5f, h * 0.5f)
            val p = if (breathProgress.isFinite()) breathProgress.coerceIn(0f, 1f) else 0.35f
            val expansion = if (isExhale) {
                0.5f + 0.5f * cos(PI.toFloat() * p)
            } else {
                0.5f - 0.5f * cos(PI.toFloat() * p)
            }

            if (completionRippleMode) {
                drawCompletionWaterRipples(
                    skin = skin,
                    center = center,
                    minDim = minDim,
                    expansion = expansion,
                    rippleCount = rippleCount.coerceIn(1, 5)
                )
                return@Canvas
            }

            val strokeScale = if (eyesFreeMode) 1.28f else 1.0f
            if (eyesFreeMode) {
                val auraR = (minDim * (0.36f + 0.13f * expansion)).coerceAtLeast(10f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to skin.palette.accentPrimaryGlow.copy(alpha = 0.38f + 0.18f * expansion),
                        0.58f to skin.palette.accentPrimary.copy(alpha = 0.16f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = auraR
                    ),
                    radius = auraR,
                    center = center
                )
            }

            val r = (minDim * (0.31f + 0.085f * expansion)).coerceAtLeast(8f)
            when (skin.illustration.haloGeometry) {
                HaloGeometry.INK_CONCENTRIC_RIPPLE -> {
                    drawCircle(
                        color = skin.palette.accentPrimary.copy(alpha = 0.09f),
                        radius = r * 0.96f,
                        center = center
                    )
                    drawCircle(
                        color = skin.palette.accentPrimary.copy(alpha = 0.68f),
                        radius = r,
                        center = center,
                        style = Stroke(
                            width = 2.2.dp.toPx() * strokeScale,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(58f, 10f, 34f, 8f), expansion * 18f)
                        )
                    )
                    drawCircle(
                        color = skin.palette.inkPrimary.copy(alpha = 0.24f),
                        radius = r * 0.78f,
                        center = center,
                        style = Stroke(
                            width = 1.2.dp.toPx() * strokeScale,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(42f, 9f), -expansion * 14f)
                        )
                    )
                    drawCircle(
                        color = skin.palette.accentSecondary.copy(alpha = 0.32f),
                        radius = r * 0.56f,
                        center = center,
                        style = Stroke(width = 0.95.dp.toPx() * strokeScale)
                    )
                }

                HaloGeometry.JADE_CLOUD_AURA -> {
                    val lobeCount = 8
                    val rotDeg = expansion * 8f - 4f
                    rotate(degrees = rotDeg, pivot = center) {
                        cloudPath.reset()
                        for (i in 0 until lobeCount) {
                            val a0 = (2.0 * PI * i / lobeCount).toFloat()
                            val aMid = (2.0 * PI * (i + 0.5) / lobeCount).toFloat()
                            val a1 = (2.0 * PI * (i + 1) / lobeCount).toFloat()
                            val valleyR = r * 0.88f
                            val crestR = r * 1.07f
                            val startPt = Offset(center.x + cos(a0) * valleyR, center.y + sin(a0) * valleyR)
                            val ctrlPt = Offset(center.x + cos(aMid) * crestR, center.y + sin(aMid) * crestR)
                            val endPt = Offset(center.x + cos(a1) * valleyR, center.y + sin(a1) * valleyR)
                            if (i == 0) {
                                cloudPath.moveTo(startPt.x, startPt.y)
                            }
                            cloudPath.quadraticTo(ctrlPt.x, ctrlPt.y, endPt.x, endPt.y)
                        }
                        cloudPath.close()
                        drawPath(
                            path = cloudPath,
                            color = skin.palette.accentPrimary.copy(alpha = 0.12f)
                        )
                        drawPath(
                            path = cloudPath,
                            color = skin.palette.accentPrimary.copy(alpha = 0.64f),
                            style = Stroke(width = 1.45.dp.toPx() * strokeScale, cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        color = Color(0xFFB88655).copy(alpha = 0.56f),
                        radius = r * 0.76f,
                        center = center,
                        style = Stroke(
                            width = 1.1.dp.toPx() * strokeScale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 8f), 0f)
                        )
                    )
                }

                HaloGeometry.LUNAR_TIDE_RING -> {
                    drawCircle(
                        color = skin.palette.accentPrimary.copy(alpha = 0.11f),
                        radius = r * 0.94f,
                        center = center
                    )
                    drawCircle(
                        color = skin.palette.accentPrimary.copy(alpha = 0.42f),
                        radius = r * 0.86f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx() * strokeScale)
                    )
                    val arcTopLeft = Offset(center.x - r, center.y - r)
                    val arcSize = Size(r * 2f, r * 2f)
                    drawArc(
                        color = skin.palette.accentSecondary.copy(alpha = 0.76f),
                        startAngle = -140f + expansion * 18f,
                        sweepAngle = 235f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 2.2.dp.toPx() * strokeScale, cap = StrokeCap.Round)
                    )
                    val innerR = r * 0.70f
                    drawArc(
                        color = skin.palette.accentPrimary.copy(alpha = 0.68f),
                        startAngle = 55f - expansion * 14f,
                        sweepAngle = 125f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerR, center.y - innerR),
                        size = Size(innerR * 2f, innerR * 2f),
                        style = Stroke(width = 1.5.dp.toPx() * strokeScale, cap = StrokeCap.Round)
                    )
                }

                HaloGeometry.PETAL_BLOOM_HALO -> {
                    val baseRot = expansion * 10f - 5f
                    val rx = r
                    val ry = r * 0.54f
                    val ovalTopLeft = Offset(center.x - rx, center.y - ry)
                    val ovalSize = Size(rx * 2f, ry * 2f)
                    for (idx in 0 until 3) {
                        val deg = baseRot + idx * 60f
                        rotate(degrees = deg, pivot = center) {
                            drawOval(
                                color = if (idx % 2 == 0) {
                                    skin.palette.accentPrimary.copy(alpha = 0.08f)
                                } else {
                                    skin.palette.accentSecondary.copy(alpha = 0.07f)
                                },
                                topLeft = ovalTopLeft,
                                size = ovalSize
                            )
                            drawOval(
                                color = skin.palette.accentPrimary.copy(alpha = 0.36f),
                                topLeft = ovalTopLeft,
                                size = ovalSize,
                                style = Stroke(width = 1.3.dp.toPx() * strokeScale)
                            )
                        }
                    }
                    val stamenOrbit = r * 0.36f
                    val stamenR = 2.0.dp.toPx() * strokeScale
                    for (k in 0 until 6) {
                        val rad = Math.toRadians((k * 60.0 + baseRot).toDouble()).toFloat()
                        drawCircle(
                            color = skin.palette.accentPrimary.copy(alpha = 0.52f),
                            radius = stamenR,
                            center = Offset(center.x + cos(rad) * stamenOrbit, center.y + sin(rad) * stamenOrbit)
                        )
                    }
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawCompletionWaterRipples(
    skin: SongxiSkin,
    center: Offset,
    minDim: Float,
    expansion: Float,
    rippleCount: Int
) {
    val innerR = minDim * (0.18f + 0.03f * expansion)
    val outerR = minDim * (0.43f + 0.03f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(skin.palette.accentPrimary.copy(alpha = 0.18f), Color.Transparent),
            center = center,
            radius = outerR
        ),
        radius = outerR,
        center = center
    )

    for (i in 0 until rippleCount) {
        val frac = if (rippleCount == 1) 0.5f else i.toFloat() / (rippleCount - 1).toFloat()
        val ringR = innerR + (outerR - innerR) * frac
        val alpha = (0.68f - frac * 0.38f).coerceAtLeast(0.20f)
        val strokeW = (1.9f - frac * 0.7f).coerceAtLeast(0.9f).dp.toPx()
        val effect = if (i % 2 == 1) {
            PathEffect.dashPathEffect(floatArrayOf(56f, 10f, 34f, 8f), 0f)
        } else {
            null
        }
        drawCircle(
            color = skin.palette.accentPrimary.copy(alpha = alpha),
            radius = ringR,
            center = center,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, pathEffect = effect)
        )
    }
    drawCircle(
        color = skin.palette.sealVermilion.copy(alpha = 0.55f),
        radius = 3.2.dp.toPx(),
        center = center
    )
}
