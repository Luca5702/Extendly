package com.songxi.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.songxi.app.core.model.SkinId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ShanShuiBackdropCanvas(
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current,
    driftPhase: Float = 0f,
    showCelestial: Boolean = true,
    showBotanical: Boolean = true,
    showPaperGrain: Boolean = true
) {
    val pathA = remember { Path() }
    val pathB = remember { Path() }
    val pathC = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (!w.isFinite() || !h.isFinite() || w <= 1f || h <= 1f) return@Canvas
        val minDim = min(w, h)

        // Layer 1: Solid Xuan/silk base + atmospheric vertical sky wash
        drawRect(color = skin.palette.canvasPrimary)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to skin.palette.canvasSkyWashTop,
                0.78f to skin.palette.canvasPrimary,
                1.0f to skin.palette.canvasPrimary
            )
        )

        // Layer 2: Deterministic Xuan paper / woven silk substrate grain
        if (showPaperGrain) {
            drawSubstrateGrain(skin = skin, w = w, h = h)
        }

        // Layer 3: Celestial body
        if (showCelestial) {
            drawCelestialBody(
                skin = skin,
                w = w,
                h = h,
                minDim = minDim,
                outerPath = pathA,
                cutoutPath = pathB,
                resultPath = pathC
            )
        }

        // Layer 4: Multi-plane Shan-Shui mountain ridges
        drawMountainRidges(
            skin = skin,
            w = w,
            h = h,
            fillPath = pathA,
            crestPath = pathB
        )

        // Layer 5: Translucent drifting mist bands
        drawMistBands(
            skin = skin,
            w = w,
            h = h,
            driftPhase = driftPhase
        )

        // Layer 6: Foreground botanical motif
        if (showBotanical) {
            drawBotanicalMotif(
                skin = skin,
                w = w,
                h = h,
                minDim = minDim,
                workPath = pathA
            )
        }
    }
}

private fun mix32(x0: Int): Int {
    var x = x0
    x = (x xor (x ushr 16)) * -0x7a143595
    x = (x xor (x ushr 13)) * -0x3d4d51cb
    return x xor (x ushr 16)
}

private fun unitFloat(seed: Int): Float =
    ((mix32(seed) ushr 8) and 0xFFFFFF) / 16777215f

private fun DrawScope.drawSubstrateGrain(
    skin: SongxiSkin,
    w: Float,
    h: Float
) {
    val rawGrain = skin.palette.substrateGrainAlpha
    val grainAlpha = if (rawGrain.isFinite()) rawGrain.coerceIn(0.005f, 0.08f) else 0.03f
    val baseSeed = (skin.id.ordinal + 1) * 0x9E37

    val isPaperSubstrate = skin.id == SkinId.INK_MIST || skin.id == SkinId.SPRING_BLOSSOM
    if (isPaperSubstrate) {
        val fiberColor = skin.palette.inkSecondary.copy(alpha = (grainAlpha * 2.8f).coerceAtMost(0.13f))
        val fiberStroke = 0.75.dp.toPx()
        for (i in 0 until 44) {
            val s = baseSeed + i * 17
            val x = unitFloat(s) * w
            val y = unitFloat(s + 101) * h
            val len = (4f + 7f * unitFloat(s + 202)).dp.toPx()
            val angle = (unitFloat(s + 303) - 0.5f) * 1.2f
            val dx = cos(angle) * len * 0.5f
            val dy = sin(angle) * len * 0.5f
            drawLine(
                color = fiberColor,
                start = Offset(x - dx, y - dy),
                end = Offset(x + dx, y + dy),
                strokeWidth = fiberStroke,
                cap = StrokeCap.Round
            )
        }
        val dotColor = skin.palette.inkTertiary.copy(alpha = (grainAlpha * 2.1f).coerceAtMost(0.10f))
        val dotRadius = 0.85.dp.toPx()
        for (i in 0 until 60) {
            val s = baseSeed + 2000 + i * 23
            val x = unitFloat(s) * w
            val y = unitFloat(s + 59) * h
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    } else {
        val warpStep = 7.dp.toPx().coerceAtLeast(4f)
        val weftStep = 16.dp.toPx().coerceAtLeast(8f)
        val warpColor = skin.palette.inkSecondary.copy(alpha = (grainAlpha * 1.55f).coerceAtMost(0.08f))
        val weftColor = skin.palette.inkSecondary.copy(alpha = (grainAlpha * 1.05f).coerceAtMost(0.06f))
        val threadStroke = 0.55.dp.toPx()

        var y = warpStep * 0.5f
        while (y < h) {
            drawLine(
                color = warpColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = threadStroke
            )
            y += warpStep
        }

        var x = weftStep * 0.5f
        while (x < w) {
            drawLine(
                color = weftColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = threadStroke
            )
            x += weftStep
        }

        val micaColor = skin.landscape.celestialColor.copy(alpha = (grainAlpha * 2.3f).coerceAtMost(0.12f))
        val micaRadius = 0.9.dp.toPx()
        for (i in 0 until 28) {
            val s = baseSeed + 4000 + i * 31
            val mx = unitFloat(s) * w
            val my = unitFloat(s + 73) * h
            drawCircle(
                color = micaColor,
                radius = micaRadius,
                center = Offset(mx, my)
            )
        }
    }
}

private fun DrawScope.drawCelestialBody(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    minDim: Float,
    outerPath: Path,
    cutoutPath: Path,
    resultPath: Path
) {
    val celColor = skin.landscape.celestialColor
    when (skin.landscape.celestialType) {
        CelestialType.HAZY_SUN -> {
            val center = Offset(0.78f * w, 0.22f * h)
            val glowRadius = (0.17f * minDim).coerceAtLeast(8f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(celColor.copy(alpha = 0.28f), celColor.copy(alpha = 0f)),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            drawCircle(
                color = celColor.copy(alpha = 0.56f),
                radius = 0.082f * minDim,
                center = center
            )
        }

        CelestialType.GOLDEN_MIST_ORB -> {
            val center = Offset(0.76f * w, 0.21f * h)
            val glowRadius = (0.18f * minDim).coerceAtLeast(8f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(celColor.copy(alpha = 0.26f), celColor.copy(alpha = 0f)),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            drawCircle(
                color = celColor.copy(alpha = 0.68f),
                radius = 0.078f * minDim,
                center = center
            )
            drawCircle(
                color = Color(0xFFB88655).copy(alpha = 0.45f),
                radius = 0.098f * minDim,
                center = center,
                style = Stroke(width = 0.85.dp.toPx())
            )
        }

        CelestialType.CRESCENT_MOON -> {
            val cx = 0.80f * w
            val cy = 0.21f * h
            val center = Offset(cx, cy)
            val glowRadius = (0.17f * minDim).coerceAtLeast(8f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(skin.palette.accentPrimary.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            outerPath.reset()
            cutoutPath.reset()
            resultPath.reset()
            val outerR = 0.082f * minDim
            val innerR = 0.072f * minDim
            outerPath.addOval(Rect(center = center, radius = outerR))
            cutoutPath.addOval(
                Rect(
                    center = Offset(cx - 0.036f * minDim, cy - 0.024f * minDim),
                    radius = innerR
                )
            )
            resultPath.op(outerPath, cutoutPath, PathOperation.Difference)
            drawPath(
                path = resultPath,
                color = celColor.copy(alpha = 0.90f)
            )
        }

        CelestialType.WARM_MORNING_GLOW -> {
            val center = Offset(0.22f * w, 0.23f * h)
            val glowRadius = (0.24f * minDim).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to celColor.copy(alpha = 0.42f),
                    0.55f to celColor.copy(alpha = 0.15f),
                    1.0f to celColor.copy(alpha = 0f),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
            drawCircle(
                color = celColor.copy(alpha = 0.54f),
                radius = 0.088f * minDim,
                center = center
            )
        }
    }
}

private fun DrawScope.drawMountainRidges(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    fillPath: Path,
    crestPath: Path
) {
    val farColor = skin.landscape.farRidgeColor
    val midColor = skin.landscape.midRidgeColor
    val baseColor = skin.palette.canvasPrimary
    val crestStrokeColor = skin.landscape.ridgeCrestStrokeColor

    when (skin.landscape.ridgeStyle) {
        RidgeStyle.INK_WASH_MIST -> {
            // Far boneless wash ridge
            fillPath.reset()
            fillPath.moveTo(0f, 0.56f * h)
            fillPath.cubicTo(0.08f * w, 0.49f * h, 0.15f * w, 0.34f * h, 0.22f * w, 0.34f * h)
            fillPath.cubicTo(0.30f * w, 0.34f * h, 0.37f * w, 0.45f * h, 0.44f * w, 0.45f * h)
            fillPath.cubicTo(0.54f * w, 0.45f * h, 0.61f * w, 0.29f * h, 0.70f * w, 0.29f * h)
            fillPath.cubicTo(0.80f * w, 0.29f * h, 0.90f * w, 0.43f * h, w, 0.47f * h)
            fillPath.lineTo(w, 0.84f * h)
            fillPath.lineTo(0f, 0.84f * h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(farColor.copy(alpha = 0.66f), baseColor.copy(alpha = 0.05f)),
                    startY = 0.28f * h,
                    endY = 0.84f * h
                )
            )

            // Mid boneless wash ridge
            fillPath.reset()
            fillPath.moveTo(0f, 0.66f * h)
            fillPath.cubicTo(0.11f * w, 0.58f * h, 0.19f * w, 0.44f * h, 0.28f * w, 0.44f * h)
            fillPath.cubicTo(0.38f * w, 0.44f * h, 0.45f * w, 0.55f * h, 0.54f * w, 0.55f * h)
            fillPath.cubicTo(0.65f * w, 0.55f * h, 0.73f * w, 0.40f * h, 0.82f * w, 0.40f * h)
            fillPath.cubicTo(0.90f * w, 0.40f * h, 0.95f * w, 0.50f * h, w, 0.54f * h)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(midColor.copy(alpha = 0.78f), baseColor.copy(alpha = 0.08f)),
                    startY = 0.39f * h,
                    endY = 0.96f * h
                )
            )

            // Mi-dot ink foliage washes
            val miDotColor = skin.palette.accentPrimary.copy(alpha = 0.18f)
            val miDots = arrayOf(
                0.25f to 0.46f,
                0.30f to 0.48f,
                0.28f to 0.51f,
                0.79f to 0.42f,
                0.84f to 0.45f
            )
            val rx = 0.038f * w
            val ry = 0.012f * h
            for ((nx, ny) in miDots) {
                drawOval(
                    color = miDotColor,
                    topLeft = Offset(nx * w - rx, ny * h - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
            }
        }

        RidgeStyle.QINGLV_MINERAL_GOLD_RIM -> {
            val goldColor = crestStrokeColor ?: Color(0xFFB88655)
            // Far mineral peaks (azurite + celadon)
            crestPath.reset()
            crestPath.moveTo(0f, 0.58f * h)
            crestPath.quadraticTo(0.09f * w, 0.42f * h, 0.17f * w, 0.29f * h)
            crestPath.quadraticTo(0.24f * w, 0.38f * h, 0.31f * w, 0.45f * h)
            crestPath.quadraticTo(0.39f * w, 0.31f * h, 0.47f * w, 0.22f * h)
            crestPath.quadraticTo(0.55f * w, 0.34f * h, 0.63f * w, 0.43f * h)
            crestPath.quadraticTo(0.72f * w, 0.32f * h, 0.81f * w, 0.26f * h)
            crestPath.quadraticTo(0.90f * w, 0.38f * h, w, 0.47f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, 0.85f * h)
            fillPath.lineTo(0f, 0.85f * h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    0.0f to skin.palette.accentSecondary.copy(alpha = 0.52f),
                    0.42f to farColor.copy(alpha = 0.78f),
                    1.0f to baseColor.copy(alpha = 0.06f),
                    startY = 0.22f * h,
                    endY = 0.85f * h
                )
            )
            drawPath(
                path = crestPath,
                color = goldColor.copy(alpha = 0.62f),
                style = Stroke(
                    width = 0.9.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Mid mineral peaks (malachite + gold rim)
            crestPath.reset()
            crestPath.moveTo(0f, 0.68f * h)
            crestPath.quadraticTo(0.06f * w, 0.55f * h, 0.13f * w, 0.43f * h)
            crestPath.quadraticTo(0.20f * w, 0.50f * h, 0.27f * w, 0.55f * h)
            crestPath.quadraticTo(0.35f * w, 0.43f * h, 0.42f * w, 0.35f * h)
            crestPath.quadraticTo(0.51f * w, 0.46f * h, 0.60f * w, 0.54f * h)
            crestPath.quadraticTo(0.68f * w, 0.45f * h, 0.76f * w, 0.38f * h)
            crestPath.quadraticTo(0.88f * w, 0.46f * h, w, 0.52f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    0.0f to skin.palette.accentPrimary.copy(alpha = 0.58f),
                    0.45f to midColor.copy(alpha = 0.82f),
                    1.0f to baseColor.copy(alpha = 0.08f),
                    startY = 0.35f * h,
                    endY = 0.96f * h
                )
            )
            drawPath(
                path = crestPath,
                color = goldColor.copy(alpha = 0.86f),
                style = Stroke(
                    width = 1.15.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Mud-gold descending facet cleavage lines
            val cleavageColor = goldColor.copy(alpha = 0.48f)
            val cleavageWidth = 0.8.dp.toPx()
            drawLine(cleavageColor, Offset(0.47f * w, 0.22f * h), Offset(0.43f * w, 0.36f * h), cleavageWidth, StrokeCap.Round)
            drawLine(cleavageColor, Offset(0.81f * w, 0.26f * h), Offset(0.77f * w, 0.38f * h), cleavageWidth, StrokeCap.Round)
            drawLine(cleavageColor, Offset(0.42f * w, 0.35f * h), Offset(0.38f * w, 0.52f * h), cleavageWidth, StrokeCap.Round)
            drawLine(cleavageColor, Offset(0.76f * w, 0.38f * h), Offset(0.71f * w, 0.54f * h), cleavageWidth, StrokeCap.Round)
        }

        RidgeStyle.DUSK_INDIGO_LAYER -> {
            // Horizon twilight range
            fillPath.reset()
            fillPath.moveTo(0f, 0.48f * h)
            fillPath.cubicTo(0.22f * w, 0.40f * h, 0.40f * w, 0.33f * h, 0.55f * w, 0.33f * h)
            fillPath.cubicTo(0.72f * w, 0.33f * h, 0.86f * w, 0.40f * h, w, 0.44f * h)
            fillPath.lineTo(w, 0.82f * h)
            fillPath.lineTo(0f, 0.82f * h)
            fillPath.close()
            drawPath(path = fillPath, color = farColor.copy(alpha = 0.55f))

            // Far indigo range
            crestPath.reset()
            crestPath.moveTo(0f, 0.56f * h)
            crestPath.cubicTo(0.10f * w, 0.45f * h, 0.18f * w, 0.36f * h, 0.26f * w, 0.36f * h)
            crestPath.cubicTo(0.36f * w, 0.36f * h, 0.46f * w, 0.46f * h, 0.56f * w, 0.45f * h)
            crestPath.cubicTo(0.65f * w, 0.44f * h, 0.70f * w, 0.31f * h, 0.76f * w, 0.31f * h)
            crestPath.cubicTo(0.84f * w, 0.31f * h, 0.92f * w, 0.39f * h, w, 0.45f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, 0.90f * h)
            fillPath.lineTo(0f, 0.90f * h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(farColor.copy(alpha = 0.86f), baseColor.copy(alpha = 0.12f)),
                    startY = 0.31f * h,
                    endY = 0.90f * h
                )
            )

            // Mid indigo range
            crestPath.reset()
            crestPath.moveTo(0f, 0.68f * h)
            crestPath.cubicTo(0.14f * w, 0.58f * h, 0.24f * w, 0.47f * h, 0.34f * w, 0.47f * h)
            crestPath.cubicTo(0.45f * w, 0.47f * h, 0.55f * w, 0.56f * h, 0.66f * w, 0.54f * h)
            crestPath.cubicTo(0.74f * w, 0.52f * h, 0.79f * w, 0.43f * h, 0.84f * w, 0.43f * h)
            crestPath.cubicTo(0.90f * w, 0.43f * h, 0.96f * w, 0.49f * h, w, 0.53f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(midColor.copy(alpha = 0.92f), baseColor.copy(alpha = 0.18f)),
                    startY = 0.43f * h,
                    endY = h
                )
            )
            if (crestStrokeColor != null) {
                drawPath(
                    path = crestPath,
                    color = crestStrokeColor.copy(alpha = 0.72f),
                    style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Moon-glint eastern ridge arc
            crestPath.reset()
            crestPath.moveTo(0.62f * w, 0.55f * h)
            crestPath.cubicTo(0.72f * w, 0.51f * h, 0.80f * w, 0.43f * h, 0.84f * w, 0.43f * h)
            crestPath.cubicTo(0.90f * w, 0.43f * h, 0.95f * w, 0.48f * h, 0.98f * w, 0.52f * h)
            drawPath(
                path = crestPath,
                color = skin.palette.accentPrimary.copy(alpha = 0.38f),
                style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        RidgeStyle.SPRING_SLOPE_STREAM -> {
            // Far warm slope
            crestPath.reset()
            crestPath.moveTo(0f, 0.45f * h)
            crestPath.cubicTo(0.20f * w, 0.36f * h, 0.42f * w, 0.48f * h, 0.62f * w, 0.54f * h)
            crestPath.cubicTo(0.76f * w, 0.48f * h, 0.88f * w, 0.38f * h, w, 0.35f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, 0.88f * h)
            fillPath.lineTo(0f, 0.88f * h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(farColor.copy(alpha = 0.68f), baseColor.copy(alpha = 0.06f)),
                    startY = 0.34f * h,
                    endY = 0.88f * h
                )
            )

            // Mid embracing valley slopes
            crestPath.reset()
            crestPath.moveTo(0f, 0.56f * h)
            crestPath.cubicTo(0.18f * w, 0.48f * h, 0.42f * w, 0.55f * h, 0.62f * w, 0.58f * h)
            crestPath.cubicTo(0.77f * w, 0.54f * h, 0.89f * w, 0.47f * h, w, 0.46f * h)

            fillPath.reset()
            fillPath.addPath(crestPath)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(midColor.copy(alpha = 0.78f), baseColor.copy(alpha = 0.08f)),
                    startY = 0.45f * h,
                    endY = h
                )
            )
            if (crestStrokeColor != null) {
                drawPath(
                    path = crestPath,
                    color = crestStrokeColor.copy(alpha = 0.65f),
                    style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Winding jade spring stream ribbon
            crestPath.reset()
            crestPath.moveTo(0.64f * w, 0.58f * h)
            crestPath.cubicTo(0.54f * w, 0.66f * h, 0.70f * w, 0.75f * h, 0.56f * w, 0.83f * h)
            crestPath.cubicTo(0.44f * w, 0.90f * h, 0.58f * w, 0.95f * h, 0.48f * w, h)
            drawPath(
                path = crestPath,
                color = skin.palette.accentSecondary.copy(alpha = 0.24f),
                style = Stroke(
                    width = max(5.dp.toPx(), 0.032f * w),
                    cap = StrokeCap.Round
                )
            )
            drawPath(
                path = crestPath,
                color = skin.palette.surfaceCard.copy(alpha = 0.66f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

private fun DrawScope.drawMistBands(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    driftPhase: Float
) {
    val safePhase = if (driftPhase.isFinite()) (driftPhase % 1f) else 0f
    val driftPx = sin(safePhase * 2f * PI.toFloat()) * (0.026f * w)
    val mistColor = skin.landscape.mistColor
    val rawOpacity = skin.landscape.mistOpacity
    val mistOpacity = if (rawOpacity.isFinite()) rawOpacity.coerceIn(0f, 0.95f) else 0.6f

    val centerA = Offset(0.30f * w + driftPx, 0.61f * h)
    val rxA = (0.35f * w).coerceAtLeast(10f)
    val ryA = (0.070f * h).coerceAtLeast(6f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(mistColor.copy(alpha = mistOpacity), Color.Transparent),
            center = centerA,
            radius = max(rxA, ryA)
        ),
        topLeft = Offset(centerA.x - rxA, centerA.y - ryA),
        size = Size(rxA * 2f, ryA * 2f)
    )

    val centerB = Offset(0.74f * w - driftPx * 0.85f, 0.68f * h)
    val rxB = (0.37f * w).coerceAtLeast(10f)
    val ryB = (0.075f * h).coerceAtLeast(6f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(mistColor.copy(alpha = mistOpacity * 0.85f), Color.Transparent),
            center = centerB,
            radius = max(rxB, ryB)
        ),
        topLeft = Offset(centerB.x - rxB, centerB.y - ryB),
        size = Size(rxB * 2f, ryB * 2f)
    )
}

private fun DrawScope.drawBotanicalMotif(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    minDim: Float,
    workPath: Path
) {
    when (skin.landscape.botanicalMotif) {
        BotanicalMotif.PINE_BRANCH -> {
            workPath.reset()
            workPath.moveTo(0f, 0.16f * h)
            workPath.quadraticTo(0.09f * w, 0.15f * h, 0.17f * w, 0.12f * h)
            workPath.quadraticTo(0.21f * w, 0.105f * h, 0.25f * w, 0.11f * h)
            drawPath(
                path = workPath,
                color = skin.palette.inkPrimary.copy(alpha = 0.72f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
            workPath.reset()
            workPath.moveTo(0.09f * w, 0.145f * h)
            workPath.quadraticTo(0.12f * w, 0.18f * h, 0.15f * w, 0.21f * h)
            drawPath(
                path = workPath,
                color = skin.palette.inkPrimary.copy(alpha = 0.64f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
            val needleColor = skin.palette.accentPrimary.copy(alpha = 0.76f)
            val needleLen = 0.055f * minDim
            drawPineNeedleFan(Offset(0.17f * w, 0.12f * h), needleLen, needleColor)
            drawPineNeedleFan(Offset(0.25f * w, 0.11f * h), needleLen * 0.92f, needleColor)
            drawPineNeedleFan(Offset(0.15f * w, 0.21f * h), needleLen * 0.85f, needleColor)
        }

        BotanicalMotif.BAMBOO_AND_ROCK -> {
            // Taihu garden rock in lower right
            workPath.reset()
            workPath.moveTo(0.81f * w, h)
            workPath.quadraticTo(0.82f * w, 0.87f * h, 0.89f * w, 0.84f * h)
            workPath.quadraticTo(0.95f * w, 0.86f * h, 0.97f * w, h)
            workPath.close()
            drawPath(
                path = workPath,
                color = skin.palette.accentSecondary.copy(alpha = 0.26f)
            )

            val bambooColor = Color(0xFF2B5B50).copy(alpha = 0.72f)
            val stemWidth = 1.9.dp.toPx()
            drawLine(bambooColor, Offset(0.88f * w, h), Offset(0.85f * w, 0.50f * h), stemWidth, StrokeCap.Round)
            drawLine(bambooColor, Offset(0.92f * w, h), Offset(0.90f * w, 0.56f * h), stemWidth * 0.85f, StrokeCap.Round)

            // Bamboo culm node ticks
            val tickW = 3.dp.toPx()
            for (frac in floatArrayOf(0.64f, 0.78f, 0.91f)) {
                val nx = 0.88f * w + (0.85f - 0.88f) * w * ((1f - frac) / 0.50f)
                val ny = frac * h
                drawLine(bambooColor, Offset(nx - tickW, ny), Offset(nx + tickW, ny), 1.2.dp.toPx(), StrokeCap.Round)
            }

            // Lancet bamboo leaves
            val leafLen = 0.068f * minDim
            drawBambooLeaf(Offset(0.85f * w, 0.52f * h), -2.4f, leafLen, bambooColor, workPath)
            drawBambooLeaf(Offset(0.85f * w, 0.52f * h), -1.8f, leafLen * 0.9f, bambooColor, workPath)
            drawBambooLeaf(Offset(0.865f * w, 0.64f * h), -2.6f, leafLen * 0.95f, bambooColor, workPath)
            drawBambooLeaf(Offset(0.90f * w, 0.57f * h), -0.7f, leafLen * 0.88f, bambooColor, workPath)
            drawBambooLeaf(Offset(0.905f * w, 0.69f * h), -2.3f, leafLen * 0.84f, bambooColor, workPath)
        }

        BotanicalMotif.NIGHT_PINE_NEEDLES -> {
            workPath.reset()
            workPath.moveTo(0f, 0.18f * h)
            workPath.quadraticTo(0.10f * w, 0.165f * h, 0.19f * w, 0.14f * h)
            drawPath(
                path = workPath,
                color = Color(0xFF465E57),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
            val needleColor = skin.palette.accentPrimary.copy(alpha = 0.62f)
            val needleLen = 0.052f * minDim
            drawPineNeedleFan(Offset(0.19f * w, 0.14f * h), needleLen, needleColor)
            drawPineNeedleFan(Offset(0.11f * w, 0.175f * h), needleLen * 0.88f, needleColor)
        }

        BotanicalMotif.PEACH_BLOSSOM_TWIG -> {
            val twigColor = skin.palette.inkSecondary
            workPath.reset()
            workPath.moveTo(w, 0.12f * h)
            workPath.quadraticTo(0.89f * w, 0.10f * h, 0.79f * w, 0.11f * h)
            workPath.quadraticTo(0.74f * w, 0.115f * h, 0.70f * w, 0.11f * h)
            drawPath(
                path = workPath,
                color = twigColor,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
            workPath.reset()
            workPath.moveTo(0.86f * w, 0.11f * h)
            workPath.quadraticTo(0.83f * w, 0.17f * h, 0.80f * w, 0.22f * h)
            drawPath(
                path = workPath,
                color = twigColor,
                style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)
            )

            // Spring jade leaf buds
            val leafBudColor = skin.palette.accentSecondary.copy(alpha = 0.74f)
            drawBambooLeaf(Offset(0.84f * w, 0.11f * h), -2.2f, 0.036f * minDim, leafBudColor, workPath)
            drawBambooLeaf(Offset(0.75f * w, 0.115f * h), 2.3f, 0.032f * minDim, leafBudColor, workPath)

            // 3 five-petal wild-peach blossoms
            drawPeachBlossom(Offset(0.79f * w, 0.11f * h), 1.0f, skin.palette.accentPrimary)
            drawPeachBlossom(Offset(0.71f * w, 0.11f * h), 0.86f, skin.palette.accentPrimary)
            drawPeachBlossom(Offset(0.80f * w, 0.22f * h), 0.92f, skin.palette.accentPrimary)
        }
    }
}

private fun DrawScope.drawPineNeedleFan(
    origin: Offset,
    length: Float,
    color: Color
) {
    val strokeW = 1.05.dp.toPx()
    for (i in 0 until 5) {
        val deg = -145f + i * 27.5f
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val end = Offset(origin.x + cos(rad) * length, origin.y + sin(rad) * length)
        drawLine(
            color = color,
            start = origin,
            end = end,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBambooLeaf(
    origin: Offset,
    angleRad: Float,
    length: Float,
    color: Color,
    path: Path
) {
    val dx = cos(angleRad) * length
    val dy = sin(angleRad) * length
    val nx = -sin(angleRad) * (length * 0.22f)
    val ny = cos(angleRad) * (length * 0.22f)
    val midX = origin.x + dx * 0.48f
    val midY = origin.y + dy * 0.48f
    val tipX = origin.x + dx
    val tipY = origin.y + dy

    path.reset()
    path.moveTo(origin.x, origin.y)
    path.quadraticTo(midX + nx, midY + ny, tipX, tipY)
    path.quadraticTo(midX - nx, midY - ny, origin.x, origin.y)
    path.close()
    drawPath(path = path, color = color)
}

private fun DrawScope.drawPeachBlossom(
    center: Offset,
    scale: Float,
    stamenColor: Color
) {
    val petalDist = 3.2.dp.toPx() * scale
    val petalRadius = 2.6.dp.toPx() * scale
    val petalColorPrimary = Color(0xFFE6A598)
    val petalColorSoft = Color(0xFFEBB7AC)

    for (k in 0 until 5) {
        val rad = Math.toRadians((k * 72.0) - 90.0).toFloat()
        val px = center.x + cos(rad) * petalDist
        val py = center.y + sin(rad) * petalDist
        drawCircle(
            color = if (k % 2 == 0) petalColorPrimary else petalColorSoft,
            radius = petalRadius,
            center = Offset(px, py)
        )
    }
    drawCircle(
        color = stamenColor,
        radius = 1.7.dp.toPx() * scale,
        center = center
    )
}
