package com.songxi.app.core.designsystem

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun ClassicalSealBadge(
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current,
    textOverride: String? = null,
    shapeOverride: SealShape? = null,
    size: Dp = 36.dp
) {
    val effectiveShape = shapeOverride ?: skin.illustration.sealShape
    val effectiveText = (textOverride ?: skin.illustration.sealText).trim().ifEmpty { "松息" }
    val aspect = when (effectiveShape) {
        SealShape.SQUARE_ZHUWEN, SealShape.ROUND_ZHUWEN -> 1.0f
        SealShape.OBLONG_BAIWEN -> 0.78f
        SealShape.GOURD_XIANZHANG -> 0.76f
    }

    val glyphPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }
    }
    val gourdPath = remember { Path() }

    val safeSize = if (size.value.isFinite() && size.value > 0f) size else 36.dp

    Canvas(
        modifier = modifier
            .size(width = safeSize * aspect, height = safeSize)
            .semantics { contentDescription = effectiveText }
    ) {
        val canvasW = this.size.width
        val canvasH = this.size.height
        if (!canvasW.isFinite() || !canvasH.isFinite() || canvasW <= 4f || canvasH <= 4f) return@Canvas

        // Keep at least 2px clear margin around the outer perimeter so stone strokes & glyphs never clip.
        val outerMargin = max(2.2f, max(1.6.dp.toPx(), 0.075f * min(canvasW, canvasH)))
        val availW = (canvasW - outerMargin * 2f).coerceAtLeast(2f)
        val availH = (canvasH - outerMargin * 2f).coerceAtLeast(2f)
        val sealW = min(availW, availH * aspect)
        val sealH = min(availH, availW / aspect)
        val left = (canvasW - sealW) * 0.5f
        val top = (canvasH - sealH) * 0.5f
        val minSealDim = min(sealW, sealH)

        val vermilion = skin.palette.sealVermilion
        val interiorPaper = skin.palette.canvasPrimary
        val borderW = (0.068f * minSealDim).coerceAtLeast(1.35.dp.toPx())
        val halfBorder = borderW * 0.5f

        val strokeLeft = left + halfBorder
        val strokeTop = top + halfBorder
        val strokeW = (sealW - borderW).coerceAtLeast(1f)
        val strokeH = (sealH - borderW).coerceAtLeast(1f)

        val chars = effectiveText.map { it.toString() }

        when (effectiveShape) {
            SealShape.SQUARE_ZHUWEN -> {
                val cornerR = CornerRadius(0.14f * minSealDim, 0.14f * minSealDim)
                drawRoundRect(
                    color = interiorPaper,
                    topLeft = Offset(strokeLeft, strokeTop),
                    size = Size(strokeW, strokeH),
                    cornerRadius = cornerR
                )
                drawRoundRect(
                    color = vermilion,
                    topLeft = Offset(strokeLeft, strokeTop),
                    size = Size(strokeW, strokeH),
                    cornerRadius = cornerR,
                    style = Stroke(width = borderW)
                )
                // Organic stone corner nick in interior paper
                drawCircle(
                    color = interiorPaper,
                    radius = borderW * 0.42f,
                    center = Offset(strokeLeft + strokeW * 0.86f, strokeTop)
                )
                drawSealCharacters(
                    nativeCanvas = drawContext.canvas.nativeCanvas,
                    chars = chars,
                    innerRect = Rect(
                        left = strokeLeft + borderW * 0.85f,
                        top = strokeTop + borderW * 0.85f,
                        right = strokeLeft + strokeW - borderW * 0.85f,
                        bottom = strokeTop + strokeH - borderW * 0.85f
                    ),
                    colorArgb = vermilion.toArgb(),
                    isGourd = false,
                    paint = glyphPaint
                )
            }

            SealShape.OBLONG_BAIWEN -> {
                val cornerR = CornerRadius(0.12f * minSealDim, 0.12f * minSealDim)
                drawRoundRect(
                    color = vermilion,
                    topLeft = Offset(left, top),
                    size = Size(sealW, sealH),
                    cornerRadius = cornerR
                )
                val bevelInset = borderW * 0.9f
                if (sealW > bevelInset * 2.5f && sealH > bevelInset * 2.5f) {
                    drawRoundRect(
                        color = interiorPaper.copy(alpha = 0.28f),
                        topLeft = Offset(left + bevelInset, top + bevelInset),
                        size = Size(sealW - bevelInset * 2f, sealH - bevelInset * 2f),
                        cornerRadius = CornerRadius(0.08f * minSealDim, 0.08f * minSealDim),
                        style = Stroke(width = max(0.75.dp.toPx(), borderW * 0.36f))
                    )
                }
                val padX = sealW * 0.13f
                val padY = sealH * 0.12f
                drawSealCharacters(
                    nativeCanvas = drawContext.canvas.nativeCanvas,
                    chars = chars,
                    innerRect = Rect(
                        left = left + padX,
                        top = top + padY,
                        right = left + sealW - padX,
                        bottom = top + sealH - padY
                    ),
                    colorArgb = interiorPaper.toArgb(),
                    isGourd = false,
                    paint = glyphPaint
                )
            }

            SealShape.GOURD_XIANZHANG -> {
                val cx = left + sealW * 0.5f
                val upperW = strokeW * 0.72f
                val lowerW = strokeW * 0.96f
                val waistW = strokeW * 0.48f
                val topY = strokeTop
                val waistY = strokeTop + strokeH * 0.43f
                val bottomY = strokeTop + strokeH

                gourdPath.reset()
                gourdPath.moveTo(cx, topY)
                gourdPath.cubicTo(
                    cx + upperW * 0.58f,
                    topY,
                    cx + upperW * 0.56f,
                    waistY - strokeH * 0.08f,
                    cx + waistW * 0.5f,
                    waistY
                )
                gourdPath.cubicTo(
                    cx + lowerW * 0.60f,
                    waistY + strokeH * 0.08f,
                    cx + lowerW * 0.58f,
                    bottomY,
                    cx,
                    bottomY
                )
                gourdPath.cubicTo(
                    cx - lowerW * 0.58f,
                    bottomY,
                    cx - lowerW * 0.60f,
                    waistY + strokeH * 0.08f,
                    cx - waistW * 0.5f,
                    waistY
                )
                gourdPath.cubicTo(
                    cx - upperW * 0.56f,
                    waistY - strokeH * 0.08f,
                    cx - upperW * 0.58f,
                    topY,
                    cx,
                    topY
                )
                gourdPath.close()

                drawPath(path = gourdPath, color = interiorPaper)
                drawPath(
                    path = gourdPath,
                    color = vermilion,
                    style = Stroke(width = borderW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawSealCharacters(
                    nativeCanvas = drawContext.canvas.nativeCanvas,
                    chars = chars,
                    innerRect = Rect(
                        left = strokeLeft,
                        top = strokeTop,
                        right = strokeLeft + strokeW,
                        bottom = strokeTop + strokeH
                    ),
                    colorArgb = vermilion.toArgb(),
                    isGourd = true,
                    paint = glyphPaint
                )
            }

            SealShape.ROUND_ZHUWEN -> {
                val diameter = min(strokeW, strokeH)
                val circleCenter = Offset(canvasW * 0.5f, canvasH * 0.5f)
                val radius = diameter * 0.5f
                drawCircle(
                    color = interiorPaper,
                    radius = radius,
                    center = circleCenter
                )
                val arcRectTopLeft = Offset(circleCenter.x - radius, circleCenter.y - radius)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = vermilion,
                    startAngle = -78f,
                    sweepAngle = 344f,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcSize,
                    style = Stroke(width = borderW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = vermilion,
                    startAngle = 272f,
                    sweepAngle = 6f,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcSize,
                    style = Stroke(width = borderW * 0.85f, cap = StrokeCap.Round)
                )
                val innerInset = radius * 0.24f
                drawSealCharacters(
                    nativeCanvas = drawContext.canvas.nativeCanvas,
                    chars = chars,
                    innerRect = Rect(
                        left = circleCenter.x - radius + innerInset,
                        top = circleCenter.y - radius + innerInset,
                        right = circleCenter.x + radius - innerInset,
                        bottom = circleCenter.y + radius - innerInset
                    ),
                    colorArgb = vermilion.toArgb(),
                    isGourd = false,
                    paint = glyphPaint
                )
            }
        }
    }
}

private fun drawSealCharacters(
    nativeCanvas: android.graphics.Canvas,
    chars: List<String>,
    innerRect: Rect,
    colorArgb: Int,
    isGourd: Boolean,
    paint: Paint
) {
    val innerW = innerRect.width.coerceAtLeast(1f)
    val innerH = innerRect.height.coerceAtLeast(1f)
    val cx = innerRect.center.x

    when {
        chars.size == 1 -> {
            drawCenteredGlyph(
                nativeCanvas = nativeCanvas,
                glyph = chars[0],
                cx = cx,
                cy = innerRect.center.y,
                textSizePx = 0.48f * min(innerW, innerH),
                colorArgb = colorArgb,
                paint = paint
            )
        }

        chars.size == 2 && isGourd -> {
            drawCenteredGlyph(
                nativeCanvas = nativeCanvas,
                glyph = chars[0],
                cx = cx,
                cy = innerRect.top + 0.25f * innerH,
                textSizePx = min(0.24f * innerH, 0.42f * innerW),
                colorArgb = colorArgb,
                paint = paint
            )
            drawCenteredGlyph(
                nativeCanvas = nativeCanvas,
                glyph = chars[1],
                cx = cx,
                cy = innerRect.top + 0.70f * innerH,
                textSizePx = min(0.28f * innerH, 0.50f * innerW),
                colorArgb = colorArgb,
                paint = paint
            )
        }

        chars.size == 2 -> {
            val textSize = min(0.35f * innerH, 0.56f * innerW)
            drawCenteredGlyph(
                nativeCanvas = nativeCanvas,
                glyph = chars[0],
                cx = cx,
                cy = innerRect.top + 0.28f * innerH,
                textSizePx = textSize,
                colorArgb = colorArgb,
                paint = paint
            )
            drawCenteredGlyph(
                nativeCanvas = nativeCanvas,
                glyph = chars[1],
                cx = cx,
                cy = innerRect.top + 0.73f * innerH,
                textSizePx = textSize,
                colorArgb = colorArgb,
                paint = paint
            )
        }

        else -> {
            // Classical 2x2 seal grid (up to 4 characters)
            val textSize = min(0.33f * innerH, 0.37f * innerW)
            val colLeft = innerRect.left + 0.28f * innerW
            val colRight = innerRect.left + 0.72f * innerW
            val rowTop = innerRect.top + 0.29f * innerH
            val rowBottom = innerRect.top + 0.72f * innerH
            val positions = arrayOf(
                colLeft to rowTop,
                colRight to rowTop,
                colLeft to rowBottom,
                colRight to rowBottom
            )
            val count = min(chars.size, 4)
            for (i in 0 until count) {
                val (gx, gy) = positions[i]
                drawCenteredGlyph(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[i],
                    cx = gx,
                    cy = gy,
                    textSizePx = textSize,
                    colorArgb = colorArgb,
                    paint = paint
                )
            }
        }
    }
}

private fun drawCenteredGlyph(
    nativeCanvas: android.graphics.Canvas,
    glyph: String,
    cx: Float,
    cy: Float,
    textSizePx: Float,
    colorArgb: Int,
    paint: Paint
) {
    paint.color = colorArgb
    paint.textSize = textSizePx.coerceAtLeast(4f)
    val fm = paint.fontMetrics
    val baselineY = cy - (fm.ascent + fm.descent) * 0.5f
    nativeCanvas.drawText(glyph, cx, baselineY, paint)
}
