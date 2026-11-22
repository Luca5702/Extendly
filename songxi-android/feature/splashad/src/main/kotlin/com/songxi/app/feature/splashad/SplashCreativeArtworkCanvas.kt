package com.songxi.app.feature.splashad

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiSkin
import com.songxi.app.core.model.SplashArtworkStyle
import kotlin.math.min

@Composable
fun SplashCreativeArtworkCanvas(
    artworkStyle: SplashArtworkStyle,
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current
) {
    val pathA = remember { Path() }
    val pathB = remember { Path() }

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        if (!w.isFinite() || !h.isFinite() || w <= 4f || h <= 4f) return@Canvas
        val minDim = min(w, h)

        val frameCorner = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        drawRoundRect(
            brush = Brush.verticalGradient(
                0.0f to skin.palette.canvasSkyWashTop,
                1.0f to skin.palette.surfaceCardSubtle
            ),
            size = Size(w, h),
            cornerRadius = frameCorner
        )
        drawRoundRect(
            color = skin.palette.borderHairline,
            size = Size(w, h),
            cornerRadius = frameCorner,
            style = Stroke(width = 1.dp.toPx())
        )

        // Subtle distant Shan-Shui ridge atmosphere behind Eastern craft still-life
        pathA.reset()
        pathA.moveTo(0f, 0.68f * h)
        pathA.quadraticTo(0.24f * w, 0.44f * h, 0.50f * w, 0.58f * h)
        pathA.quadraticTo(0.76f * w, 0.42f * h, w, 0.56f * h)
        pathA.lineTo(w, h)
        pathA.lineTo(0f, h)
        pathA.close()
        drawPath(
            path = pathA,
            color = skin.landscape.farRidgeColor.copy(alpha = 0.42f)
        )

        when (artworkStyle) {
            SplashArtworkStyle.CELADON_TEA -> drawCeladonTeaArtwork(skin, w, h, minDim, pathA, pathB)
            SplashArtworkStyle.PINE_INCENSE -> drawPineIncenseArtwork(skin, w, h, minDim, pathA, pathB)
            SplashArtworkStyle.WOOD_DESK_STAND -> drawWoodDeskStandArtwork(skin, w, h, minDim, pathA)
        }
    }
}

private fun DrawScope.drawCeladonTeaArtwork(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    minDim: Float,
    cupPath: Path,
    wispPath: Path
) {
    // Warm sunlit orb in upper right
    drawCircle(
        color = skin.landscape.celestialColor.copy(alpha = 0.52f),
        radius = 0.11f * minDim,
        center = Offset(0.76f * w, 0.25f * h)
    )

    // Woven bamboo tea mat oval
    val matW = 0.52f * minDim * 1.45f
    val matH = 0.13f * minDim
    val cx = w * 0.50f
    val baseY = h * 0.74f
    drawOval(
        color = Color(0xFFC9BCA6).copy(alpha = 0.48f),
        topLeft = Offset(cx - matW * 0.5f, baseY - matH * 0.5f),
        size = Size(matW, matH)
    )

    // Ru celadon master cup body
    val cupTopW = 0.42f * minDim
    val cupBotW = 0.22f * minDim
    val cupH = 0.27f * minDim
    val rimY = baseY - cupH * 0.88f

    cupPath.reset()
    cupPath.moveTo(cx - cupTopW * 0.5f, rimY)
    cupPath.cubicTo(
        cx - cupTopW * 0.48f,
        rimY + cupH * 0.72f,
        cx - cupBotW * 0.65f,
        baseY,
        cx - cupBotW * 0.5f,
        baseY
    )
    cupPath.lineTo(cx + cupBotW * 0.5f, baseY)
    cupPath.cubicTo(
        cx + cupBotW * 0.65f,
        baseY,
        cx + cupTopW * 0.48f,
        rimY + cupH * 0.72f,
        cx + cupTopW * 0.5f,
        rimY
    )
    cupPath.close()

    drawPath(path = cupPath, color = Color(0xFFCFE2DC))
    drawPath(
        path = cupPath,
        color = Color(0xFF35685C),
        style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
    )

    // Rim & warm amber tea liquor oval
    val rimH = 0.085f * minDim
    drawOval(
        color = Color(0xFFDFECE6),
        topLeft = Offset(cx - cupTopW * 0.5f, rimY - rimH * 0.5f),
        size = Size(cupTopW, rimH)
    )
    drawOval(
        color = Color(0xFFD6B98C).copy(alpha = 0.62f),
        topLeft = Offset(cx - cupTopW * 0.38f, rimY - rimH * 0.28f),
        size = Size(cupTopW * 0.76f, rimH * 0.56f)
    )
    drawOval(
        color = Color(0xFF35685C),
        topLeft = Offset(cx - cupTopW * 0.5f, rimY - rimH * 0.5f),
        size = Size(cupTopW, rimH),
        style = Stroke(width = 1.4.dp.toPx())
    )

    // Delicate Ru-ware ice-crackle line & rising tea steam wisps
    drawLine(
        color = Color(0xFF8AA89F).copy(alpha = 0.55f),
        start = Offset(cx - cupTopW * 0.16f, rimY + cupH * 0.26f),
        end = Offset(cx + cupTopW * 0.08f, rimY + cupH * 0.62f),
        strokeWidth = 0.9.dp.toPx(),
        cap = StrokeCap.Round
    )

    for (dxSign in intArrayOf(-1, 1)) {
        val sx = cx + dxSign * 0.07f * minDim
        wispPath.reset()
        wispPath.moveTo(sx, rimY - 0.05f * minDim)
        wispPath.cubicTo(
            sx + dxSign * 0.05f * minDim,
            rimY - 0.15f * minDim,
            sx - dxSign * 0.05f * minDim,
            rimY - 0.24f * minDim,
            sx + dxSign * 0.02f * minDim,
            rimY - 0.33f * minDim
        )
        drawPath(
            path = wispPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.52f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawPineIncenseArtwork(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    minDim: Float,
    holderPath: Path,
    smokePath: Path
) {
    // Calm moon/sun orb
    drawCircle(
        color = skin.landscape.celestialColor.copy(alpha = 0.68f),
        radius = 0.115f * minDim,
        center = Offset(0.26f * w, 0.26f * h)
    )

    val cx = w * 0.50f
    val boatY = h * 0.74f
    val boatW = 0.68f * minDim
    val boatH = 0.085f * minDim

    // Carved walnut boat incense holder
    holderPath.reset()
    holderPath.moveTo(cx - boatW * 0.5f, boatY - boatH * 0.4f)
    holderPath.quadraticTo(cx, boatY + boatH * 0.9f, cx + boatW * 0.5f, boatY - boatH * 0.4f)
    holderPath.quadraticTo(cx, boatY + boatH * 0.15f, cx - boatW * 0.5f, boatY - boatH * 0.4f)
    holderPath.close()
    drawPath(path = holderPath, color = Color(0xFF685246))

    // Brass incense rest bead & angled cedar/pine stick
    val restCenter = Offset(cx - boatW * 0.26f, boatY - boatH * 0.25f)
    val emberTip = Offset(cx + boatW * 0.22f, boatY - 0.23f * minDim)
    drawCircle(color = Color(0xFFB88655), radius = 4.2.dp.toPx(), center = restCenter)
    drawLine(
        color = Color(0xFF8C6D58),
        start = restCenter,
        end = emberTip,
        strokeWidth = 2.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFFD47566), radius = 2.6.dp.toPx(), center = emberTip)

    // Rising serene S-curve smoke ribbon
    smokePath.reset()
    smokePath.moveTo(emberTip.x, emberTip.y - 3.dp.toPx())
    smokePath.cubicTo(
        emberTip.x + 0.09f * minDim,
        emberTip.y - 0.12f * minDim,
        emberTip.x - 0.11f * minDim,
        emberTip.y - 0.24f * minDim,
        emberTip.x + 0.03f * minDim,
        emberTip.y - 0.36f * minDim
    )
    drawPath(
        path = smokePath,
        color = skin.palette.inkSecondary.copy(alpha = 0.52f),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawWoodDeskStandArtwork(
    skin: SongxiSkin,
    w: Float,
    h: Float,
    minDim: Float,
    sprigPath: Path
) {
    val cx = w * 0.48f
    val deskY = h * 0.79f

    // Desk horizon line
    drawLine(
        color = skin.palette.borderHairline,
        start = Offset(0.14f * w, deskY),
        end = Offset(0.86f * w, deskY),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Black walnut mortise-and-tenon monitor riser
    val riserW = 0.66f * minDim
    val riserTopY = deskY - 0.11f * minDim
    val plankThick = 5.5.dp.toPx()
    drawRoundRect(
        color = Color(0xFF7C5843),
        topLeft = Offset(cx - riserW * 0.5f, riserTopY),
        size = Size(riserW, plankThick),
        cornerRadius = CornerRadius(2.5.dp.toPx())
    )
    val legW = 5.dp.toPx()
    val legH = deskY - riserTopY
    drawRect(
        color = Color(0xFF5E4030),
        topLeft = Offset(cx - riserW * 0.40f, riserTopY + plankThick * 0.6f),
        size = Size(legW, legH - plankThick * 0.6f)
    )
    drawRect(
        color = Color(0xFF5E4030),
        topLeft = Offset(cx + riserW * 0.40f - legW, riserTopY + plankThick * 0.6f),
        size = Size(legW, legH - plankThick * 0.6f)
    )

    // Eye-level framed display above riser
    val screenW = 0.48f * minDim
    val screenH = 0.31f * minDim
    val screenTopY = riserTopY - screenH - 0.06f * minDim
    drawRect(
        color = Color(0xFF8E6750),
        topLeft = Offset(cx - 4.dp.toPx(), riserTopY - 0.06f * minDim),
        size = Size(8.dp.toPx(), 0.06f * minDim)
    )
    val screenCorner = CornerRadius(6.dp.toPx())
    drawRoundRect(
        color = skin.palette.surfaceCard,
        topLeft = Offset(cx - screenW * 0.5f, screenTopY),
        size = Size(screenW, screenH),
        cornerRadius = screenCorner
    )
    drawRoundRect(
        color = Color(0xFF7C5843),
        topLeft = Offset(cx - screenW * 0.5f, screenTopY),
        size = Size(screenW, screenH),
        cornerRadius = screenCorner,
        style = Stroke(width = 2.dp.toPx())
    )

    // Celadon bud vase & green sprig beside stand
    val vaseCenter = Offset(cx + riserW * 0.46f, deskY - 0.075f * minDim)
    drawOval(
        color = Color(0xFFCFE2DC),
        topLeft = Offset(vaseCenter.x - 0.055f * minDim, vaseCenter.y - 0.075f * minDim),
        size = Size(0.11f * minDim, 0.15f * minDim)
    )
    drawOval(
        color = Color(0xFF35685C),
        topLeft = Offset(vaseCenter.x - 0.055f * minDim, vaseCenter.y - 0.075f * minDim),
        size = Size(0.11f * minDim, 0.15f * minDim),
        style = Stroke(width = 1.2.dp.toPx())
    )
    sprigPath.reset()
    sprigPath.moveTo(vaseCenter.x, vaseCenter.y - 0.07f * minDim)
    sprigPath.quadraticTo(
        vaseCenter.x + 0.05f * minDim,
        vaseCenter.y - 0.16f * minDim,
        vaseCenter.x + 0.09f * minDim,
        vaseCenter.y - 0.21f * minDim
    )
    drawPath(
        path = sprigPath,
        color = Color(0xFF486356),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )
}
