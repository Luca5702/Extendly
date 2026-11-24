package com.songxi.app.core.designsystem

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
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
    val sealStrokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val nativeSealPath = remember { AndroidPath() }
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
                // Hand-carved stone weathered nicks ("刀味残破")
                drawCircle(
                    color = interiorPaper,
                    radius = borderW * 0.44f,
                    center = Offset(strokeLeft + strokeW * 0.84f, strokeTop)
                )
                drawCircle(
                    color = interiorPaper,
                    radius = borderW * 0.36f,
                    center = Offset(strokeLeft, strokeTop + strokeH * 0.72f)
                )
                SongxiSealCalligraphy.drawSealCharacters(
                    nativeCanvas = drawContext.canvas.nativeCanvas,
                    chars = chars,
                    innerRect = Rect(
                        left = strokeLeft + borderW * 0.82f,
                        top = strokeTop + borderW * 0.78f,
                        right = strokeLeft + strokeW - borderW * 0.82f,
                        bottom = strokeTop + strokeH - borderW * 0.78f
                    ),
                    colorArgb = vermilion.toArgb(),
                    isGourd = false,
                    textFallbackPaint = glyphPaint,
                    strokePaint = sealStrokePaint,
                    workPath = nativeSealPath
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
                // Carved stone edge grain nicks in Baiwen negative relief
                drawCircle(
                    color = interiorPaper.copy(alpha = 0.75f),
                    radius = borderW * 0.34f,
                    center = Offset(left + sealW * 0.18f, top + borderW * 0.25f)
                )
                val bevelInset = borderW * 0.88f
                if (sealW > bevelInset * 2.5f && sealH > bevelInset * 2.5f) {
                    drawRoundRect(
                        color = interiorPaper.copy(alpha = 0.32f),
                        topLeft = Offset(left + bevelInset, top + bevelInset),
                        size = Size(sealW - bevelInset * 2f, sealH - bevelInset * 2f),
                        cornerRadius = CornerRadius(0.08f * minSealDim, 0.08f * minSealDim),
                        style = Stroke(width = max(0.75.dp.toPx(), borderW * 0.34f))
                    )
                    // Subtle classical 4-quadrant Jiege (界格) cross hairlines for 4-char seal
                    if (chars.size >= 4) {
                        val jx = left + sealW * 0.5f
                        val jy = top + sealH * 0.5f
                        val jiegeW = max(0.65.dp.toPx(), borderW * 0.28f)
                        drawLine(
                            color = interiorPaper.copy(alpha = 0.22f),
                            start = Offset(jx, top + bevelInset * 1.4f),
                            end = Offset(jx, top + sealH - bevelInset * 1.4f),
                            strokeWidth = jiegeW
                        )
                        drawLine(
                            color = interiorPaper.copy(alpha = 0.22f),
                            start = Offset(left + bevelInset * 1.4f, jy),
                            end = Offset(left + sealW - bevelInset * 1.4f, jy),
                            strokeWidth = jiegeW
                        )
                    }
                }
                val padX = sealW * 0.12f
                val padY = sealH * 0.11f
                SongxiSealCalligraphy.drawSealCharacters(
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
                    textFallbackPaint = glyphPaint,
                    strokePaint = sealStrokePaint,
                    workPath = nativeSealPath
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
                SongxiSealCalligraphy.drawSealCharacters(
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
                    textFallbackPaint = glyphPaint,
                    strokePaint = sealStrokePaint,
                    workPath = nativeSealPath
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
                    sweepAngle = 342f,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcSize,
                    style = Stroke(width = borderW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = vermilion,
                    startAngle = 271f,
                    sweepAngle = 7f,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcSize,
                    style = Stroke(width = borderW * 0.85f, cap = StrokeCap.Round)
                )
                val innerInset = radius * 0.22f
                SongxiSealCalligraphy.drawSealCharacters(
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
                    textFallbackPaint = glyphPaint,
                    strokePaint = sealStrokePaint,
                    workPath = nativeSealPath
                )
            }
        }
    }
}

/**
 * Bespoke Vector Xiao-Zhuan ("小篆铁线篆") seal calligraphy renderer for canonical Songxi seals,
 * with graceful serif fallback for arbitrary custom strings.
 */
object SongxiSealCalligraphy {

    private val SUPPORTED_GLYPHS = setOf("松", "息", "山", "居", "清", "听", "春", "日")

    fun drawSealCharacters(
        nativeCanvas: android.graphics.Canvas,
        chars: List<String>,
        innerRect: Rect,
        colorArgb: Int,
        isGourd: Boolean,
        textFallbackPaint: Paint,
        strokePaint: Paint,
        workPath: AndroidPath
    ) {
        val innerW = innerRect.width.coerceAtLeast(1f)
        val innerH = innerRect.height.coerceAtLeast(1f)
        val cx = innerRect.center.x

        when {
            chars.size == 1 -> {
                drawGlyphOrFallback(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[0],
                    cx = cx,
                    cy = innerRect.center.y,
                    boxSizePx = 0.56f * min(innerW, innerH),
                    colorArgb = colorArgb,
                    textFallbackPaint = textFallbackPaint,
                    strokePaint = strokePaint,
                    workPath = workPath
                )
            }

            chars.size == 2 && isGourd -> {
                drawGlyphOrFallback(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[0],
                    cx = cx,
                    cy = innerRect.top + 0.25f * innerH,
                    boxSizePx = min(0.28f * innerH, 0.46f * innerW),
                    colorArgb = colorArgb,
                    textFallbackPaint = textFallbackPaint,
                    strokePaint = strokePaint,
                    workPath = workPath
                )
                drawGlyphOrFallback(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[1],
                    cx = cx,
                    cy = innerRect.top + 0.70f * innerH,
                    boxSizePx = min(0.33f * innerH, 0.54f * innerW),
                    colorArgb = colorArgb,
                    textFallbackPaint = textFallbackPaint,
                    strokePaint = strokePaint,
                    workPath = workPath
                )
            }

            chars.size == 2 -> {
                val glyphBox = min(0.41f * innerH, 0.64f * innerW)
                drawGlyphOrFallback(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[0],
                    cx = cx,
                    cy = innerRect.top + 0.27f * innerH,
                    boxSizePx = glyphBox,
                    colorArgb = colorArgb,
                    textFallbackPaint = textFallbackPaint,
                    strokePaint = strokePaint,
                    workPath = workPath
                )
                drawGlyphOrFallback(
                    nativeCanvas = nativeCanvas,
                    glyph = chars[1],
                    cx = cx,
                    cy = innerRect.top + 0.73f * innerH,
                    boxSizePx = glyphBox,
                    colorArgb = colorArgb,
                    textFallbackPaint = textFallbackPaint,
                    strokePaint = strokePaint,
                    workPath = workPath
                )
            }

            else -> {
                // Classical 2x2 seal grid (traditional right-to-left or pleasant 2x2 layout)
                val glyphBox = min(0.38f * innerH, 0.41f * innerW)
                val colLeft = innerRect.left + 0.27f * innerW
                val colRight = innerRect.left + 0.73f * innerW
                val rowTop = innerRect.top + 0.27f * innerH
                val rowBottom = innerRect.top + 0.73f * innerH
                val positions = arrayOf(
                    colLeft to rowTop,
                    colRight to rowTop,
                    colLeft to rowBottom,
                    colRight to rowBottom
                )
                val count = min(chars.size, 4)
                for (i in 0 until count) {
                    val (gx, gy) = positions[i]
                    drawGlyphOrFallback(
                        nativeCanvas = nativeCanvas,
                        glyph = chars[i],
                        cx = gx,
                        cy = gy,
                        boxSizePx = glyphBox,
                        colorArgb = colorArgb,
                        textFallbackPaint = textFallbackPaint,
                        strokePaint = strokePaint,
                        workPath = workPath
                    )
                }
            }
        }
    }

    fun drawGlyphOrFallback(
        nativeCanvas: android.graphics.Canvas,
        glyph: String,
        cx: Float,
        cy: Float,
        boxSizePx: Float,
        colorArgb: Int,
        textFallbackPaint: Paint,
        strokePaint: Paint,
        workPath: AndroidPath
    ) {
        if (glyph in SUPPORTED_GLYPHS && boxSizePx >= 6f) {
            val w = boxSizePx * 0.88f
            val h = boxSizePx * 1.02f
            val ox = cx - w * 0.5f
            val oy = cy - h * 0.5f
            strokePaint.color = colorArgb
            strokePaint.strokeWidth = (boxSizePx * 0.076f).coerceAtLeast(1.0f)
            workPath.reset()
            buildXiaoZhuanPath(glyph, ox, oy, w, h, workPath)
            nativeCanvas.drawPath(workPath, strokePaint)
        } else {
            textFallbackPaint.color = colorArgb
            textFallbackPaint.textSize = (boxSizePx * 0.88f).coerceAtLeast(4f)
            val fm = textFallbackPaint.fontMetrics
            val baselineY = cy - (fm.ascent + fm.descent) * 0.5f
            nativeCanvas.drawText(glyph, cx, baselineY, textFallbackPaint)
        }
    }

    private fun buildXiaoZhuanPath(
        glyph: String,
        ox: Float,
        oy: Float,
        w: Float,
        h: Float,
        p: AndroidPath
    ) {
        fun x(nx: Float): Float = ox + nx * w
        fun y(ny: Float): Float = oy + ny * h

        when (glyph) {
            "松" -> {
                // Left 木: tall trunk + upward curved canopy + downward curved roots
                p.moveTo(x(0.27f), y(0.09f))
                p.lineTo(x(0.27f), y(0.91f))
                p.moveTo(x(0.11f), y(0.22f))
                p.quadTo(x(0.27f), y(0.38f), x(0.42f), y(0.22f))
                p.moveTo(x(0.11f), y(0.79f))
                p.quadTo(x(0.27f), y(0.56f), x(0.42f), y(0.76f))
                // Right 公: parted 八 wings + coiled 厶 seal base
                p.moveTo(x(0.60f), y(0.16f))
                p.quadTo(x(0.53f), y(0.34f), x(0.47f), y(0.47f))
                p.moveTo(x(0.74f), y(0.16f))
                p.quadTo(x(0.82f), y(0.34f), x(0.89f), y(0.47f))
                p.moveTo(x(0.68f), y(0.44f))
                p.lineTo(x(0.52f), y(0.78f))
                p.quadTo(x(0.50f), y(0.84f), x(0.58f), y(0.84f))
                p.lineTo(x(0.83f), y(0.81f))
                p.moveTo(x(0.76f), y(0.65f))
                p.lineTo(x(0.87f), y(0.88f))
            }

            "息" -> {
                // Top 自: finial + round-shouldered vessel + 2 inner breath bars
                p.moveTo(x(0.53f), y(0.06f))
                p.lineTo(x(0.45f), y(0.15f))
                p.addRoundRect(
                    RectF(x(0.30f), y(0.15f), x(0.70f), y(0.52f)),
                    w * 0.09f,
                    h * 0.08f,
                    AndroidPath.Direction.CW
                )
                p.moveTo(x(0.31f), y(0.27f))
                p.lineTo(x(0.69f), y(0.27f))
                p.moveTo(x(0.31f), y(0.39f))
                p.lineTo(x(0.69f), y(0.39f))
                // Bottom 心: Xiao-Zhuan cradling heart chalice + central flame + flanking drops
                p.moveTo(x(0.33f), y(0.63f))
                p.cubicTo(x(0.33f), y(0.91f), x(0.67f), y(0.91f), x(0.67f), y(0.63f))
                p.moveTo(x(0.50f), y(0.58f))
                p.lineTo(x(0.50f), y(0.75f))
                p.moveTo(x(0.20f), y(0.66f))
                p.quadTo(x(0.14f), y(0.80f), x(0.22f), y(0.89f))
                p.moveTo(x(0.79f), y(0.64f))
                p.quadTo(x(0.87f), y(0.78f), x(0.79f), y(0.89f))
            }

            "山" -> {
                // Central peak + U-curved flanking peaks
                p.moveTo(x(0.50f), y(0.12f))
                p.lineTo(x(0.50f), y(0.85f))
                p.moveTo(x(0.18f), y(0.35f))
                p.lineTo(x(0.18f), y(0.76f))
                p.quadTo(x(0.18f), y(0.86f), x(0.30f), y(0.86f))
                p.lineTo(x(0.70f), y(0.86f))
                p.quadTo(x(0.82f), y(0.86f), x(0.82f), y(0.76f))
                p.lineTo(x(0.82f), y(0.35f))
            }

            "居" -> {
                // 尸 cliff canopy + 古 cross & rounded 口
                p.moveTo(x(0.26f), y(0.13f))
                p.lineTo(x(0.78f), y(0.13f))
                p.quadTo(x(0.83f), y(0.13f), x(0.83f), y(0.21f))
                p.lineTo(x(0.83f), y(0.29f))
                p.lineTo(x(0.27f), y(0.29f))
                p.moveTo(x(0.26f), y(0.13f))
                p.cubicTo(x(0.26f), y(0.54f), x(0.22f), y(0.75f), x(0.13f), y(0.90f))
                p.moveTo(x(0.35f), y(0.48f))
                p.lineTo(x(0.85f), y(0.48f))
                p.moveTo(x(0.59f), y(0.33f))
                p.lineTo(x(0.59f), y(0.64f))
                p.addRoundRect(
                    RectF(x(0.40f), y(0.64f), x(0.79f), y(0.89f)),
                    w * 0.08f,
                    h * 0.07f,
                    AndroidPath.Direction.CW
                )
            }

            "清" -> {
                // Left 氵: Xiao-Zhuan three sinuous water streams
                p.moveTo(x(0.14f), y(0.20f))
                p.quadTo(x(0.09f), y(0.34f), x(0.15f), y(0.46f))
                p.moveTo(x(0.14f), y(0.58f))
                p.quadTo(x(0.19f), y(0.72f), x(0.13f), y(0.86f))
                p.moveTo(x(0.25f), y(0.11f))
                p.cubicTo(x(0.19f), y(0.38f), x(0.31f), y(0.62f), x(0.24f), y(0.90f))
                p.moveTo(x(0.35f), y(0.20f))
                p.quadTo(x(0.40f), y(0.34f), x(0.34f), y(0.46f))
                p.moveTo(x(0.35f), y(0.58f))
                p.quadTo(x(0.30f), y(0.72f), x(0.36f), y(0.86f))
                // Right 青: 生 sprout + 丹/月 arch
                p.moveTo(x(0.66f), y(0.10f))
                p.lineTo(x(0.66f), y(0.48f))
                p.moveTo(x(0.49f), y(0.22f))
                p.quadTo(x(0.66f), y(0.28f), x(0.83f), y(0.22f))
                p.moveTo(x(0.51f), y(0.35f))
                p.lineTo(x(0.81f), y(0.35f))
                p.moveTo(x(0.44f), y(0.48f))
                p.lineTo(x(0.88f), y(0.48f))
                p.moveTo(x(0.50f), y(0.90f))
                p.lineTo(x(0.50f), y(0.63f))
                p.quadTo(x(0.50f), y(0.55f), x(0.59f), y(0.55f))
                p.lineTo(x(0.73f), y(0.55f))
                p.quadTo(x(0.82f), y(0.55f), x(0.82f), y(0.63f))
                p.lineTo(x(0.82f), y(0.90f))
                p.moveTo(x(0.51f), y(0.72f))
                p.lineTo(x(0.81f), y(0.72f))
            }

            "听" -> {
                // Left 口 capsule + Right 斤 curved banner & pillar
                p.addRoundRect(
                    RectF(x(0.14f), y(0.24f), x(0.39f), y(0.68f)),
                    w * 0.08f,
                    h * 0.08f,
                    AndroidPath.Direction.CW
                )
                p.moveTo(x(0.84f), y(0.14f))
                p.quadTo(x(0.64f), y(0.18f), x(0.50f), y(0.24f))
                p.cubicTo(x(0.50f), y(0.56f), x(0.46f), y(0.76f), x(0.37f), y(0.90f))
                p.moveTo(x(0.50f), y(0.46f))
                p.lineTo(x(0.87f), y(0.46f))
                p.moveTo(x(0.70f), y(0.46f))
                p.lineTo(x(0.70f), y(0.90f))
            }

            "春" -> {
                // Three sprouting curves + central stem + 八 canopy + 日 sun
                p.moveTo(x(0.25f), y(0.18f))
                p.quadTo(x(0.50f), y(0.22f), x(0.75f), y(0.18f))
                p.moveTo(x(0.28f), y(0.30f))
                p.quadTo(x(0.50f), y(0.34f), x(0.72f), y(0.30f))
                p.moveTo(x(0.16f), y(0.43f))
                p.lineTo(x(0.84f), y(0.43f))
                p.moveTo(x(0.50f), y(0.09f))
                p.lineTo(x(0.50f), y(0.43f))
                p.moveTo(x(0.50f), y(0.36f))
                p.quadTo(x(0.34f), y(0.54f), x(0.14f), y(0.67f))
                p.moveTo(x(0.50f), y(0.36f))
                p.quadTo(x(0.66f), y(0.54f), x(0.86f), y(0.67f))
                p.addRoundRect(
                    RectF(x(0.35f), y(0.57f), x(0.65f), y(0.90f)),
                    w * 0.09f,
                    h * 0.08f,
                    AndroidPath.Direction.CW
                )
                p.moveTo(x(0.36f), y(0.73f))
                p.lineTo(x(0.64f), y(0.73f))
            }

            "日" -> {
                p.addRoundRect(
                    RectF(x(0.26f), y(0.14f), x(0.74f), y(0.86f)),
                    w * 0.12f,
                    h * 0.12f,
                    AndroidPath.Direction.CW
                )
                p.moveTo(x(0.28f), y(0.50f))
                p.quadTo(x(0.50f), y(0.46f), x(0.72f), y(0.50f))
            }
        }
    }
}
