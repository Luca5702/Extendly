package com.songxi.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class SongxiNavGlyph {
    HOME_ENSO,
    BODYMAP_MERIDIAN,
    SKINS_SCROLL,
    JOURNAL_RIPPLE,
    SETTINGS_CHIME
}

/**
 * Bespoke Song-dynasty hairline vector glyphs for the 5 bottom sanctuary tabs.
 */
@Composable
fun SongxiNavIconCanvas(
    glyph: SongxiNavGlyph,
    selected: Boolean,
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current
) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height)
        if (!s.isFinite() || s <= 4f) return@Canvas
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val strokeColor = if (selected) skin.palette.accentPrimary else skin.palette.inkSecondary
        val softFill = skin.palette.accentPrimarySoft
        val strokeW = if (selected) 1.65.dp.toPx() else 1.3.dp.toPx()

        if (selected) {
            drawCircle(
                color = softFill.copy(alpha = 0.72f),
                radius = s * 0.44f,
                center = Offset(cx, cy)
            )
        }

        when (glyph) {
            SongxiNavGlyph.HOME_ENSO -> {
                // Enso breath arc + distant mountain peak
                drawArc(
                    color = strokeColor,
                    startAngle = -215f,
                    sweepAngle = 295f,
                    useCenter = false,
                    topLeft = Offset(cx - s * 0.36f, cy - s * 0.36f),
                    size = Size(s * 0.72f, s * 0.72f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                path.reset()
                path.moveTo(cx - s * 0.20f, cy + s * 0.14f)
                path.quadraticTo(cx - s * 0.03f, cy - s * 0.10f, cx + s * 0.18f, cy + s * 0.14f)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeW * 0.9f, cap = StrokeCap.Round)
                )
            }

            SongxiNavGlyph.BODYMAP_MERIDIAN -> {
                // Serene head + shoulder arc + central Ren/Du meridian line
                drawCircle(
                    color = strokeColor,
                    radius = s * 0.13f,
                    center = Offset(cx, cy - s * 0.22f),
                    style = Stroke(width = strokeW)
                )
                path.reset()
                path.moveTo(cx - s * 0.30f, cy + s * 0.18f)
                path.quadraticTo(cx, cy - s * 0.08f, cx + s * 0.30f, cy + s * 0.18f)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(cx, cy - s * 0.04f),
                    end = Offset(cx, cy + s * 0.34f),
                    strokeWidth = strokeW * 0.9f,
                    cap = StrokeCap.Round
                )
                if (selected) {
                    drawCircle(
                        color = skin.palette.sealVermilion,
                        radius = s * 0.065f,
                        center = Offset(cx, cy + s * 0.08f)
                    )
                }
            }

            SongxiNavGlyph.SKINS_SCROLL -> {
                // Framed landscape scroll + sun dot
                val left = cx - s * 0.35f
                val top = cy - s * 0.28f
                val w = s * 0.70f
                val h = s * 0.56f
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(s * 0.10f, s * 0.10f),
                    style = Stroke(width = strokeW)
                )
                path.reset()
                path.moveTo(left + w * 0.12f, top + h * 0.78f)
                path.lineTo(left + w * 0.40f, top + h * 0.42f)
                path.lineTo(left + w * 0.62f, top + h * 0.66f)
                path.lineTo(left + w * 0.86f, top + h * 0.48f)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeW * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(
                    color = if (selected) skin.palette.sealVermilion else strokeColor,
                    radius = s * 0.06f,
                    center = Offset(left + w * 0.72f, top + h * 0.28f)
                )
            }

            SongxiNavGlyph.JOURNAL_RIPPLE -> {
                // Concentric water ripples on Xuan folio
                drawCircle(
                    color = strokeColor,
                    radius = s * 0.35f,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = strokeColor.copy(alpha = 0.75f),
                    radius = s * 0.19f,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeW * 0.85f)
                )
                drawCircle(
                    color = if (selected) skin.palette.sealVermilion else strokeColor,
                    radius = s * 0.065f,
                    center = Offset(cx, cy)
                )
            }

            SongxiNavGlyph.SETTINGS_CHIME -> {
                // Eaves wind chime (檐下风铎)
                drawLine(
                    color = strokeColor,
                    start = Offset(cx - s * 0.24f, cy - s * 0.30f),
                    end = Offset(cx + s * 0.24f, cy - s * 0.30f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                path.reset()
                path.moveTo(cx - s * 0.18f, cy + s * 0.08f)
                path.quadraticTo(cx - s * 0.15f, cy - s * 0.22f, cx, cy - s * 0.24f)
                path.quadraticTo(cx + s * 0.15f, cy - s * 0.22f, cx + s * 0.18f, cy + s * 0.08f)
                path.close()
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
                drawLine(
                    color = if (selected) skin.palette.sealVermilion else strokeColor,
                    start = Offset(cx, cy + s * 0.09f),
                    end = Offset(cx + s * 0.04f, cy + s * 0.34f),
                    strokeWidth = strokeW * 0.9f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
