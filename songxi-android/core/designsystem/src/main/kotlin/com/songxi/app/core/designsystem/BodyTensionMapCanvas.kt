package com.songxi.app.core.designsystem

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.songxi.app.core.model.BodyRegion
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private const val BODY_MAP_STAGE_ASPECT: Float = 0.78f

private data class ZoneHotspotSpec(
    val region: BodyRegion,
    val primaryNx: Float,
    val primaryNy: Float,
    val mirrorNx: Float? = null,
    val mirrorNy: Float? = null,
    val labelOffsetNx: Float = 0f,
    val labelOffsetNy: Float = 0.068f
)

private val ZONE_SPECS: List<ZoneHotspotSpec> = listOf(
    ZoneHotspotSpec(
        region = BodyRegion.EYE_BROW,
        primaryNx = 0.50f,
        primaryNy = 0.13f,
        labelOffsetNx = 0.16f,
        labelOffsetNy = 0.0f
    ),
    ZoneHotspotSpec(
        region = BodyRegion.POSTERIOR_NECK,
        primaryNx = 0.50f,
        primaryNy = 0.28f,
        labelOffsetNx = 0.16f,
        labelOffsetNy = 0.0f
    ),
    ZoneHotspotSpec(
        region = BodyRegion.TRAPEZIUS_SHOULDER,
        primaryNx = 0.23f,
        primaryNy = 0.39f,
        mirrorNx = 0.77f,
        mirrorNy = 0.39f,
        labelOffsetNx = 0f,
        labelOffsetNy = -0.065f
    ),
    ZoneHotspotSpec(
        region = BodyRegion.THORACIC_CHEST,
        primaryNx = 0.50f,
        primaryNy = 0.51f,
        labelOffsetNx = 0.17f,
        labelOffsetNy = 0.0f
    ),
    ZoneHotspotSpec(
        region = BodyRegion.LOWER_LUMBAR,
        primaryNx = 0.50f,
        primaryNy = 0.72f,
        labelOffsetNx = 0f,
        labelOffsetNy = 0.075f
    ),
    ZoneHotspotSpec(
        region = BodyRegion.MOUSE_WRIST,
        primaryNx = 0.17f,
        primaryNy = 0.69f,
        mirrorNx = 0.83f,
        mirrorNy = 0.69f,
        labelOffsetNx = 0f,
        labelOffsetNy = 0.075f
    )
)

private fun computeBodyMapStageRect(widthPx: Float, heightPx: Float): Rect {
    if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 1f || heightPx <= 1f) return Rect.Zero
    val stageW = min(widthPx, heightPx * BODY_MAP_STAGE_ASPECT)
    val stageH = min(heightPx, widthPx / BODY_MAP_STAGE_ASPECT)
    val left = (widthPx - stageW) * 0.5f
    val top = (heightPx - stageH) * 0.5f
    return Rect(left = left, top = top, right = left + stageW, bottom = top + stageH)
}

fun bodyTensionZoneCenterPx(
    region: BodyRegion,
    canvasSize: IntSize,
    useBilateralMirror: Boolean = false
): Offset {
    if (canvasSize.width <= 1 || canvasSize.height <= 1) return Offset.Zero
    val stage = computeBodyMapStageRect(canvasSize.width.toFloat(), canvasSize.height.toFloat())
    val spec = ZONE_SPECS.first { it.region == region }
    val nx = if (useBilateralMirror && spec.mirrorNx != null) spec.mirrorNx else spec.primaryNx
    val ny = if (useBilateralMirror && spec.mirrorNy != null) spec.mirrorNy else spec.primaryNy
    return Offset(
        x = stage.left + nx * stage.width,
        y = stage.top + ny * stage.height
    )
}

fun hitTestBodyTensionRegion(
    tapOffset: Offset,
    canvasSize: IntSize
): BodyRegion? {
    if (canvasSize.width <= 1 || canvasSize.height <= 1 || !tapOffset.x.isFinite() || !tapOffset.y.isFinite()) {
        return null
    }
    val stage = computeBodyMapStageRect(canvasSize.width.toFloat(), canvasSize.height.toFloat())
    val hitRadius = 0.145f * min(stage.width, stage.height)

    var closestRegion: BodyRegion? = null
    var closestDistance = Float.MAX_VALUE

    for (spec in ZONE_SPECS) {
        val px = stage.left + spec.primaryNx * stage.width
        val py = stage.top + spec.primaryNy * stage.height
        val distPrimary = hypot(tapOffset.x - px, tapOffset.y - py)
        if (distPrimary <= hitRadius && distPrimary < closestDistance) {
            closestDistance = distPrimary
            closestRegion = spec.region
        }

        if (spec.mirrorNx != null && spec.mirrorNy != null) {
            val mx = stage.left + spec.mirrorNx * stage.width
            val my = stage.top + spec.mirrorNy * stage.height
            val distMirror = hypot(tapOffset.x - mx, tapOffset.y - my)
            if (distMirror <= hitRadius && distMirror < closestDistance) {
                closestDistance = distMirror
                closestRegion = spec.region
            }
        }
    }
    return closestRegion
}

@Composable
fun BodyTensionMapCanvas(
    selectedRegions: Set<BodyRegion>,
    onRegionToggle: (BodyRegion) -> Unit,
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current,
    exposeZoneTestTags: Boolean = true,
    showLabels: Boolean = true
) {
    val silhouettePath = remember { Path() }
    val meridianPath = remember { Path() }
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val stageRect = computeBodyMapStageRect(maxWidthPx, maxHeightPx)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onRegionToggle) {
                    detectTapGestures { tapOffset ->
                        hitTestBodyTensionRegion(tapOffset, size)?.let(onRegionToggle)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (!w.isFinite() || !h.isFinite() || w <= 2f || h <= 2f) return@Canvas

            val cornerR = CornerRadius(skin.palette.cardCornerRadiusDp.coerceIn(4, 48).dp.toPx())
            drawRoundRect(
                color = skin.palette.surfaceCardSubtle,
                size = Size(w, h),
                cornerRadius = cornerR
            )
            drawRoundRect(
                color = skin.palette.borderHairline,
                size = Size(w, h),
                cornerRadius = cornerR,
                style = Stroke(width = 1.dp.toPx())
            )

            val stage = computeBodyMapStageRect(w, h)
            drawEasternMeridianSilhouette(
                skin = skin,
                stage = stage,
                silhouettePath = silhouettePath,
                meridianPath = meridianPath
            )

            for (spec in ZONE_SPECS) {
                val isSelected = spec.region in selectedRegions
                val primaryCenter = Offset(
                    stage.left + spec.primaryNx * stage.width,
                    stage.top + spec.primaryNy * stage.height
                )
                drawHotspotNode(
                    skin = skin,
                    center = primaryCenter,
                    isSelected = isSelected
                )

                if (spec.mirrorNx != null && spec.mirrorNy != null) {
                    val mirrorCenter = Offset(
                        stage.left + spec.mirrorNx * stage.width,
                        stage.top + spec.mirrorNy * stage.height
                    )
                    drawHotspotNode(
                        skin = skin,
                        center = mirrorCenter,
                        isSelected = isSelected
                    )
                }

                if (showLabels) {
                    val marginX = min(18.dp.toPx(), stage.width * 0.45f)
                    val marginTop = min(10.dp.toPx(), stage.height * 0.45f)
                    val marginBottom = min(8.dp.toPx(), stage.height * 0.45f)
                    val minX = stage.left + marginX
                    val maxX = (stage.right - marginX).coerceAtLeast(minX)
                    val minY = stage.top + marginTop
                    val maxY = (stage.bottom - marginBottom).coerceAtLeast(minY)
                    val labelCenter = Offset(
                        x = (primaryCenter.x + spec.labelOffsetNx * stage.width).coerceIn(minX, maxX),
                        y = (primaryCenter.y + spec.labelOffsetNy * stage.height).coerceIn(minY, maxY)
                    )
                    labelPaint.textSize = min(10.5.dp.toPx(), stage.width * 0.055f).coerceAtLeast(8f)
                    labelPaint.color = if (isSelected) {
                        skin.palette.inkPrimary.toArgb()
                    } else {
                        skin.palette.inkSecondary.toArgb()
                    }
                    labelPaint.isFakeBoldText = isSelected
                    val fm = labelPaint.fontMetrics
                    val baselineY = labelCenter.y - (fm.ascent + fm.descent) * 0.5f
                    drawContext.canvas.nativeCanvas.drawText(
                        spec.region.shortLabelCn,
                        labelCenter.x,
                        baselineY,
                        labelPaint
                    )
                }
            }
        }

        if (exposeZoneTestTags && stageRect.width > 1f && stageRect.height > 1f) {
            val boxWidthPx = min(with(density) { 40.dp.toPx() }, stageRect.width * 0.24f)
            val boxHeightPx = min(with(density) { 28.dp.toPx() }, stageRect.height * 0.12f)
            val boxWidthDp = with(density) { boxWidthPx.toDp() }
            val boxHeightDp = with(density) { boxHeightPx.toDp() }

            for (spec in ZONE_SPECS) {
                val anchorX = stageRect.left + spec.primaryNx * stageRect.width
                val anchorY = stageRect.top + spec.primaryNy * stageRect.height
                val isSelected = spec.region in selectedRegions
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (anchorX - boxWidthPx * 0.5f).roundToInt(),
                                y = (anchorY - boxHeightPx * 0.5f).roundToInt()
                            )
                        }
                        .size(width = boxWidthDp, height = boxHeightDp)
                        .testTag("bodymap_zone_${spec.region.key}")
                        .toggleable(
                            value = isSelected,
                            role = Role.Checkbox,
                            onValueChange = { onRegionToggle(spec.region) }
                        )
                        .semantics {
                            contentDescription = spec.region.labelCn
                        }
                )
            }
        }
    }
}

private fun DrawScope.drawEasternMeridianSilhouette(
    skin: SongxiSkin,
    stage: Rect,
    silhouettePath: Path,
    meridianPath: Path
) {
    fun pt(nx: Float, ny: Float): Offset = Offset(
        x = stage.left + nx * stage.width,
        y = stage.top + ny * stage.height
    )

    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            skin.illustration.garmentFill.copy(alpha = 0.72f),
            skin.palette.surfaceCard.copy(alpha = 0.85f)
        ),
        startY = stage.top + 0.08f * stage.height,
        endY = stage.top + 0.88f * stage.height
    )
    val contourColor = skin.illustration.contourStroke.copy(alpha = 0.65f)
    val strokeW = 1.6.dp.toPx()

    // Calm head oval
    val headRx = 0.095f * stage.width
    val headRy = 0.085f * stage.height
    val headCenter = pt(0.50f, 0.145f)
    drawOval(
        color = skin.illustration.skinToneFill.copy(alpha = 0.85f),
        topLeft = Offset(headCenter.x - headRx, headCenter.y - headRy),
        size = Size(headRx * 2f, headRy * 2f)
    )
    drawOval(
        color = contourColor,
        topLeft = Offset(headCenter.x - headRx, headCenter.y - headRy),
        size = Size(headRx * 2f, headRy * 2f),
        style = Stroke(width = strokeW)
    )

    // Upper-body + sloping shoulders + relaxed forearms silhouette
    silhouettePath.reset()
    silhouettePath.moveTo(pt(0.45f, 0.23f).x, pt(0.45f, 0.23f).y)
    silhouettePath.quadraticTo(pt(0.44f, 0.31f).x, pt(0.44f, 0.31f).y, pt(0.23f, 0.36f).x, pt(0.23f, 0.36f).y)
    silhouettePath.quadraticTo(pt(0.15f, 0.42f).x, pt(0.15f, 0.42f).y, pt(0.15f, 0.69f).x, pt(0.15f, 0.69f).y)
    silhouettePath.quadraticTo(pt(0.17f, 0.74f).x, pt(0.17f, 0.74f).y, pt(0.22f, 0.70f).x, pt(0.22f, 0.70f).y)
    silhouettePath.quadraticTo(pt(0.24f, 0.54f).x, pt(0.24f, 0.54f).y, pt(0.30f, 0.48f).x, pt(0.30f, 0.48f).y)
    silhouettePath.quadraticTo(pt(0.32f, 0.68f).x, pt(0.32f, 0.68f).y, pt(0.30f, 0.84f).x, pt(0.30f, 0.84f).y)
    silhouettePath.lineTo(pt(0.70f, 0.84f).x, pt(0.70f, 0.84f).y)
    silhouettePath.quadraticTo(pt(0.68f, 0.68f).x, pt(0.68f, 0.68f).y, pt(0.70f, 0.48f).x, pt(0.70f, 0.48f).y)
    silhouettePath.quadraticTo(pt(0.76f, 0.54f).x, pt(0.76f, 0.54f).y, pt(0.78f, 0.70f).x, pt(0.78f, 0.70f).y)
    silhouettePath.quadraticTo(pt(0.83f, 0.74f).x, pt(0.83f, 0.74f).y, pt(0.85f, 0.69f).x, pt(0.85f, 0.69f).y)
    silhouettePath.quadraticTo(pt(0.85f, 0.42f).x, pt(0.85f, 0.42f).y, pt(0.77f, 0.36f).x, pt(0.77f, 0.36f).y)
    silhouettePath.quadraticTo(pt(0.56f, 0.31f).x, pt(0.56f, 0.31f).y, pt(0.55f, 0.23f).x, pt(0.55f, 0.23f).y)
    silhouettePath.close()

    drawPath(path = silhouettePath, brush = fillBrush)
    drawPath(
        path = silhouettePath,
        color = contourColor,
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Central Governor meridian & arm collateral lines
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    drawLine(
        color = skin.palette.accentPrimary.copy(alpha = 0.42f),
        start = pt(0.50f, 0.11f),
        end = pt(0.50f, 0.79f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = dash
    )

    meridianPath.reset()
    meridianPath.moveTo(pt(0.50f, 0.28f).x, pt(0.50f, 0.28f).y)
    meridianPath.quadraticTo(pt(0.32f, 0.34f).x, pt(0.32f, 0.34f).y, pt(0.23f, 0.39f).x, pt(0.23f, 0.39f).y)
    meridianPath.quadraticTo(pt(0.18f, 0.52f).x, pt(0.18f, 0.52f).y, pt(0.17f, 0.69f).x, pt(0.17f, 0.69f).y)
    drawPath(
        path = meridianPath,
        color = skin.palette.accentPrimary.copy(alpha = 0.34f),
        style = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
    )

    meridianPath.reset()
    meridianPath.moveTo(pt(0.50f, 0.28f).x, pt(0.50f, 0.28f).y)
    meridianPath.quadraticTo(pt(0.68f, 0.34f).x, pt(0.68f, 0.34f).y, pt(0.77f, 0.39f).x, pt(0.77f, 0.39f).y)
    meridianPath.quadraticTo(pt(0.82f, 0.52f).x, pt(0.82f, 0.52f).y, pt(0.83f, 0.69f).x, pt(0.83f, 0.69f).y)
    drawPath(
        path = meridianPath,
        color = skin.palette.accentPrimary.copy(alpha = 0.34f),
        style = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
    )
}

private fun DrawScope.drawHotspotNode(
    skin: SongxiSkin,
    center: Offset,
    isSelected: Boolean
) {
    if (isSelected) {
        val washR = 17.5.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skin.palette.accentPrimary.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = washR
            ),
            radius = washR,
            center = center
        )
        drawCircle(
            color = skin.palette.accentPrimary.copy(alpha = 0.66f),
            radius = 11.2.dp.toPx(),
            center = center,
            style = Stroke(
                width = 1.35.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f), 0f)
            )
        )
        drawCircle(
            color = skin.palette.accentPrimary,
            radius = 6.0.dp.toPx(),
            center = center
        )
        drawCircle(
            color = skin.palette.sealVermilion.copy(alpha = 0.65f),
            radius = 6.8.dp.toPx(),
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawCircle(
            color = skin.palette.onAccent,
            radius = 2.1.dp.toPx(),
            center = center
        )
    } else {
        val idleR = 4.6.dp.toPx()
        drawCircle(
            color = skin.palette.surfaceCard,
            radius = idleR,
            center = center
        )
        drawCircle(
            color = skin.palette.accentPrimary.copy(alpha = 0.72f),
            radius = idleR,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
