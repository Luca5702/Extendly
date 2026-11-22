package com.songxi.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.PoseKeyframe
import com.songxi.app.core.model.StretchPose
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun EditorialStretchFigureCanvas(
    pose: StretchPose,
    breathProgress: Float,
    isExhale: Boolean,
    modifier: Modifier = Modifier,
    isSecondHalf: Boolean = false,
    skin: SongxiSkin = LocalSongxiSkin.current,
    eyesFreeMinimal: Boolean = false
) {
    val p = breathProgress.finiteClamp(0f, 1f, 0f)
    val eased = 0.5f - 0.5f * cos(PI.toFloat() * p)
    val safeInhale = pose.inhaleKeyframe.sanitized()
    val safeExhale = pose.exhaleKeyframe.sanitized()
    val baseKeyframe = if (isExhale) {
        safeInhale.lerp(safeExhale, eased)
    } else {
        safeExhale.lerp(safeInhale, eased)
    }
    val effectiveKeyframe = if (pose.bilateralSwitchAtHalfway && isSecondHalf) {
        baseKeyframe.mirroredBilaterally()
    } else {
        baseKeyframe
    }
    val side = when {
        !pose.bilateralSwitchAtHalfway -> 0
        isSecondHalf -> 1
        else -> -1
    }
    EditorialStretchFigureCanvas(
        keyframe = effectiveKeyframe,
        modifier = modifier,
        skin = skin,
        highlightedRegions = pose.targetRegions,
        eyesFreeMinimal = eyesFreeMinimal,
        bilateralSide = side
    )
}

@Composable
fun EditorialStretchFigureCanvas(
    keyframe: PoseKeyframe,
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current,
    highlightedRegions: Set<BodyRegion> = emptySet(),
    eyesFreeMinimal: Boolean = false,
    bilateralSide: Int = 0
) {
    val tunicPath = remember { Path() }
    val armPath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (!w.isFinite() || !h.isFinite() || w <= 4f || h <= 4f) return@Canvas

        val s = min(w, h)
        val ox = (w - s) * 0.5f
        val oy = (h - s) * 0.5f
        fun pt(nx: Float, ny: Float): Offset = Offset(ox + nx * s, oy + ny * s)

        val safeKeyframe = keyframe.sanitized()
        val safeBilateral = bilateralSide.coerceIn(-1, 1)
        val kin = solveFigureKinematics(
            keyframe = safeKeyframe,
            bilateralSide = safeBilateral,
            ox = ox,
            oy = oy,
            s = s
        )

        if (eyesFreeMinimal) {
            drawEyesFreeFigure(
                skin = skin,
                kin = kin,
                s = s,
                highlightedRegions = highlightedRegions,
                bilateralSide = safeBilateral,
                tunicPath = tunicPath,
                armPath = armPath
            )
        } else {
            drawFullEditorialFigure(
                skin = skin,
                keyframe = safeKeyframe,
                kin = kin,
                s = s,
                highlightedRegions = highlightedRegions,
                bilateralSide = safeBilateral,
                pt = ::pt,
                tunicPath = tunicPath,
                armPath = armPath,
                detailPath = detailPath
            )
        }
    }
}

private data class ArmJoints(
    val shoulder: Offset,
    val elbow: Offset,
    val hand: Offset,
    val wristAngleDeg: Float,
    val isPosterior: Boolean
)

private data class FigureKinematics(
    val pelvis: Offset,
    val chest: Offset,
    val neck: Offset,
    val headCenter: Offset,
    val glabella: Offset,
    val leftShoulder: Offset,
    val rightShoulder: Offset,
    val leftArm: ArmJoints,
    val rightArm: ArmJoints,
    val cxNorm: Float,
    val chestNy: Float
)

private fun Float.finiteClamp(minVal: Float, maxVal: Float, fallback: Float): Float =
    if (isFinite()) coerceIn(minVal, maxVal) else fallback

private fun PoseKeyframe.sanitized(): PoseKeyframe = PoseKeyframe(
    headTiltDeg = headTiltDeg.finiteClamp(-30f, 30f, 0f),
    chinTuckRatio = chinTuckRatio.finiteClamp(-1f, 1f, 0f),
    leftShoulderDropRatio = leftShoulderDropRatio.finiteClamp(0f, 1f, 0.5f),
    rightShoulderDropRatio = rightShoulderDropRatio.finiteClamp(0f, 1f, 0.5f),
    spineArcRatio = spineArcRatio.finiteClamp(-1f, 1f, 0f),
    leftArmAngleDeg = leftArmAngleDeg.finiteClamp(0f, 165f, 15f),
    rightArmAngleDeg = rightArmAngleDeg.finiteClamp(0f, 165f, 15f),
    wristFlexRatio = wristFlexRatio.finiteClamp(-1f, 1f, 0f),
    eyesClosed = eyesClosed
)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x >= edge1) 1f else 0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun solveFigureKinematics(
    keyframe: PoseKeyframe,
    bilateralSide: Int,
    ox: Float,
    oy: Float,
    s: Float
): FigureKinematics {
    fun pt(nx: Float, ny: Float): Offset = Offset(ox + nx * s, oy + ny * s)

    val spine = keyframe.spineArcRatio.coerceIn(-1f, 1f)
    val chin = keyframe.chinTuckRatio.coerceIn(-1f, 1f)
    val tiltDeg = keyframe.headTiltDeg.coerceIn(-30f, 30f)
    val tiltRad = Math.toRadians(tiltDeg.toDouble()).toFloat()

    val cx = 0.50f + tiltDeg * 0.0018f + bilateralSide * 0.006f
    val chestNy = 0.54f - spine * 0.032f
    val neckNx = cx + tiltDeg * 0.0022f
    val neckNy = chestNy - 0.095f + chin * 0.018f
    val headNx = neckNx + sin(tiltRad) * 0.055f
    val headNy = neckNy - 0.078f + chin * 0.022f

    val shHalfW = 0.135f + spine * 0.016f
    val leftShNx = cx - shHalfW
    val leftShNy = chestNy - 0.065f + keyframe.leftShoulderDropRatio.coerceIn(0f, 1f) * 0.052f
    val rightShNx = cx + shHalfW
    val rightShNy = chestNy - 0.065f + keyframe.rightShoulderDropRatio.coerceIn(0f, 1f) * 0.052f

    fun solveArm(
        sideSign: Float,
        shNx: Float,
        shNy: Float,
        armDegRaw: Float,
        isActiveBilateralHand: Boolean
    ): ArmJoints {
        val armDeg = armDegRaw.coerceIn(0f, 165f)
        val wrist = keyframe.wristFlexRatio.coerceIn(-1f, 1f)
        val activeBias = if (isActiveBilateralHand) 1f else 0f

        // Smooth C1 partition of unity across 4 posture families
        val tMid = smoothstep(44f, 52f, armDeg)
        val tFace = smoothstep(84f, 94f, armDeg)
        val tOver = smoothstep(122f, 134f, armDeg)

        val wLow = 1f - tMid
        val wMid = tMid * (1f - tFace)
        val wFace = tFace * (1f - tOver)
        val wOver = tOver

        // Regime 1: Lap rest / shoulder sigh / chest-clasp behind hip (0..46 deg)
        val lowElNx = shNx + sideSign * (0.045f + (armDeg / 45f) * 0.055f + max(0f, spine) * 0.025f)
        val lowElNy = shNy + 0.135f - (armDeg / 45f) * 0.020f
        val lowHandNx = 0.50f + sideSign * (0.095f - max(0f, spine) * 0.025f + (armDeg - 15f) * 0.0012f)
        val lowHandNy = 0.755f - max(0f, -spine) * 0.020f

        // Regime 2: Desk support / wrist extension / prayer lotus (48..82 deg)
        val inward = smoothstep(58f, 74f, armDeg)
        val midElNx = shNx + sideSign * (0.075f - inward * 0.020f)
        val midElNy = shNy + 0.125f - (armDeg - 50f) * 0.0012f
        val midHandNx = cx + sideSign * lerpFloat(0.085f, 0.022f, inward)
        val midHandNy = chestNy + 0.045f - (armDeg - 50f) * 0.0018f - wrist * 0.032f - activeBias * 0.016f

        // Regime 3: Occiput cradle / Fengchi thumb press / warm eye palming (94..118 deg)
        val occiputT = smoothstep(-0.05f, -0.25f, chin)
        val faceLiftT = ((armDeg - 94f) / 26f).coerceIn(0f, 1f)
        val faceElNx = shNx + sideSign * lerpFloat(0.085f, 0.125f, occiputT)
        val faceElNy = shNy + lerpFloat(0.075f, -0.015f, faceLiftT)
        val faceHandNx = headNx + sideSign * lerpFloat(0.038f, 0.026f, occiputT)
        val faceHandNy = headNy + lerpFloat(0.005f, 0.018f, occiputT) - wrist * 0.015f

        // Regime 4: Overhead willow side bend (130..165 deg)
        val overT = ((armDeg - 130f) / 30f).coerceIn(0f, 1.2f)
        val overElNx = shNx + sideSign * (0.055f - overT * 0.035f)
        val overElNy = shNy - 0.115f - overT * 0.035f
        val overHandNx = headNx - sideSign * (0.035f + overT * 0.055f)
        val overHandNy = headNy - 0.115f - overT * 0.020f

        val elNx = wLow * lowElNx + wMid * midElNx + wFace * faceElNx + wOver * overElNx
        val elNy = wLow * lowElNy + wMid * midElNy + wFace * faceElNy + wOver * overElNy
        val handNx = wLow * lowHandNx + wMid * midHandNx + wFace * faceHandNx + wOver * overHandNx
        val handNy = wLow * lowHandNy + wMid * midHandNy + wFace * faceHandNy + wOver * overHandNy

        val wristAngleDeg = wrist * 38f * sideSign + if (isActiveBilateralHand) 18f * bilateralSide else 0f
        val isPosterior = armDeg in 88f..126f && chin < -0.12f

        return ArmJoints(
            shoulder = pt(shNx, shNy),
            elbow = pt(elNx, elNy),
            hand = pt(handNx, handNy),
            wristAngleDeg = wristAngleDeg,
            isPosterior = isPosterior
        )
    }

    val leftActive = bilateralSide < 0
    val rightActive = bilateralSide > 0
    val glabellaOffset = Offset(
        x = ox + (headNx + sin(tiltRad) * 0.015f) * s,
        y = oy + (headNy - cos(tiltRad) * 0.018f) * s
    )

    return FigureKinematics(
        pelvis = pt(0.50f, 0.78f),
        chest = pt(cx, chestNy),
        neck = pt(neckNx, neckNy),
        headCenter = pt(headNx, headNy),
        glabella = glabellaOffset,
        leftShoulder = pt(leftShNx, leftShNy),
        rightShoulder = pt(rightShNx, rightShNy),
        leftArm = solveArm(-1f, leftShNx, leftShNy, keyframe.leftArmAngleDeg, leftActive),
        rightArm = solveArm(1f, rightShNx, rightShNy, keyframe.rightArmAngleDeg, rightActive),
        cxNorm = cx,
        chestNy = chestNy
    )
}

private fun DrawScope.drawEyesFreeFigure(
    skin: SongxiSkin,
    kin: FigureKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    bilateralSide: Int,
    tunicPath: Path,
    armPath: Path
) {
    val glowStroke = 6.0.dp.toPx()
    val crispStroke = 2.0.dp.toPx()
    val glowColor = skin.palette.accentPrimaryGlow.copy(alpha = 0.22f)
    val lineColor = skin.palette.accentPrimary.copy(alpha = 0.72f)

    buildTorsoTunicPath(tunicPath, kin, s)
    drawPath(tunicPath, color = glowColor, style = Stroke(width = glowStroke, join = StrokeJoin.Round))
    drawPath(tunicPath, color = lineColor, style = Stroke(width = crispStroke, join = StrokeJoin.Round))

    val headRx = 0.060f * s
    val headRy = 0.072f * s
    val headTopLeft = Offset(kin.headCenter.x - headRx, kin.headCenter.y - headRy)
    val headSize = Size(headRx * 2f, headRy * 2f)
    drawOval(color = glowColor, topLeft = headTopLeft, size = headSize, style = Stroke(width = glowStroke))
    drawOval(color = lineColor, topLeft = headTopLeft, size = headSize, style = Stroke(width = crispStroke))

    for (arm in arrayOf(kin.leftArm, kin.rightArm)) {
        armPath.reset()
        armPath.moveTo(arm.shoulder.x, arm.shoulder.y)
        armPath.quadraticTo(arm.elbow.x, arm.elbow.y, arm.hand.x, arm.hand.y)
        drawPath(armPath, color = glowColor, style = Stroke(width = glowStroke * 1.1f, cap = StrokeCap.Round))
        drawPath(armPath, color = lineColor, style = Stroke(width = crispStroke, cap = StrokeCap.Round))
    }

    // Central breath meridian line
    drawLine(
        color = skin.palette.accentSecondary.copy(alpha = 0.50f),
        start = kin.neck,
        end = kin.pelvis,
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )

    if (bilateralSide != 0) {
        drawBilateralSideCues(skin = skin, kin = kin, s = s, bilateralSide = bilateralSide, detailPath = armPath)
    }
    drawHighlightedRegionWarmth(skin = skin, kin = kin, s = s, highlightedRegions = highlightedRegions)
}

private fun DrawScope.drawFullEditorialFigure(
    skin: SongxiSkin,
    keyframe: PoseKeyframe,
    kin: FigureKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    bilateralSide: Int,
    pt: (Float, Float) -> Offset,
    tunicPath: Path,
    armPath: Path,
    detailPath: Path
) {
    val ill = skin.illustration
    val contourW = ill.contourStrokeWidthDp.finiteClamp(0.5f, 6f, 1.8f).dp.toPx()

    // 1. Seated mat / chair horizon line
    drawLine(
        color = skin.palette.borderHairline,
        start = pt(0.24f, 0.81f),
        end = pt(0.76f, 0.81f),
        strokeWidth = 1.3.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = skin.palette.accentSecondary.copy(alpha = 0.30f),
        start = pt(0.32f, 0.84f),
        end = pt(0.68f, 0.84f),
        strokeWidth = 1.0.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Moonlit rim-light aura for DUSK_PINE (MOONLIT_CONTOUR)
    if (ill.figureStyle == FigureStyle.MOONLIT_CONTOUR) {
        buildTorsoTunicPath(tunicPath, kin, s)
        drawPath(
            path = tunicPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.24f),
            style = Stroke(width = 4.4.dp.toPx(), join = StrokeJoin.Round)
        )
    }

    // 3. Posterior arms (occiput cradle / Fengchi press) behind hair & head
    if (kin.leftArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.leftArm, s = s, contourW = contourW, armPath = armPath)
    }
    if (kin.rightArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.rightArm, s = s, contourW = contourW, armPath = armPath)
    }

    // 4. Posterior hair bun & hair silhouette
    val tiltDeg = keyframe.headTiltDeg.coerceIn(-30f, 30f)
    rotate(degrees = tiltDeg, pivot = kin.headCenter) {
        val bunCenter = Offset(kin.headCenter.x, kin.headCenter.y - 0.082f * s)
        drawCircle(
            color = ill.hairFill,
            radius = 0.032f * s,
            center = bunCenter
        )
        // Style-specific hair ornament
        when (ill.figureStyle) {
            FigureStyle.EDITORIAL_MINERAL_FLAT -> {
                // Carved mud-gold & celadon hairpin
                drawLine(
                    color = Color(0xFFB88655),
                    start = Offset(bunCenter.x - 0.046f * s, bunCenter.y + 0.004f * s),
                    end = Offset(bunCenter.x + 0.048f * s, bunCenter.y - 0.006f * s),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = skin.palette.accentPrimary,
                    radius = 2.4.dp.toPx(),
                    center = Offset(bunCenter.x + 0.048f * s, bunCenter.y - 0.006f * s)
                )
            }

            FigureStyle.BOTANICAL_GOUACHE -> {
                // Miniature spring blossom hair ornament
                val ornament = Offset(bunCenter.x + 0.026f * s, bunCenter.y - 0.004f * s)
                drawCircle(color = Color(0xFFE6A598), radius = 3.2.dp.toPx(), center = ornament)
                drawCircle(color = skin.palette.accentPrimary, radius = 1.4.dp.toPx(), center = ornament)
            }

            else -> Unit
        }

        val hairRx = 0.068f * s
        val hairRy = 0.078f * s
        drawOval(
            color = ill.hairFill,
            topLeft = Offset(kin.headCenter.x - hairRx, kin.headCenter.y - hairRy * 1.04f),
            size = Size(hairRx * 2f, hairRy * 1.85f)
        )
    }

    // 5. Neck column
    val neckWidth = 0.046f * s
    drawLine(
        color = ill.skinToneFill,
        start = Offset(kin.headCenter.x, kin.headCenter.y + 0.045f * s),
        end = Offset(kin.neck.x, kin.neck.y + 0.035f * s),
        strokeWidth = neckWidth,
        cap = StrokeCap.Round
    )

    // 6. Song tunic torso & style-specific garment details
    buildTorsoTunicPath(tunicPath, kin, s)
    if (ill.figureStyle == FigureStyle.INK_BRUSH_WASH) {
        // Offset dry-brush ink wash shadow
        drawPath(
            path = tunicPath,
            color = ill.contourStroke.copy(alpha = 0.16f),
            style = Stroke(width = 3.6.dp.toPx(), join = StrokeJoin.Round)
        )
    }
    drawPath(path = tunicPath, color = ill.garmentFill)

    // Lower seated lap drapery shadow band
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.165f * s, kin.pelvis.y - 0.03f * s)
    detailPath.quadraticTo(kin.pelvis.x, kin.pelvis.y + 0.035f * s, kin.pelvis.x + 0.165f * s, kin.pelvis.y - 0.03f * s)
    detailPath.lineTo(kin.pelvis.x + 0.175f * s, kin.pelvis.y + 0.025f * s)
    detailPath.quadraticTo(kin.pelvis.x, kin.pelvis.y + 0.055f * s, kin.pelvis.x - 0.175f * s, kin.pelvis.y + 0.025f * s)
    detailPath.close()
    drawPath(path = detailPath, color = ill.garmentShadow.copy(alpha = 0.85f))

    // Crossed Song lapel (交领右衽)
    val collarColor = when (ill.figureStyle) {
        FigureStyle.EDITORIAL_MINERAL_FLAT -> Color(0xFFB88655)
        FigureStyle.MOONLIT_CONTOUR -> skin.palette.accentSecondary
        FigureStyle.BOTANICAL_GOUACHE -> skin.palette.accentSecondary
        FigureStyle.INK_BRUSH_WASH -> ill.garmentShadow
    }
    val leftCollarPt = Offset(kin.neck.x - 0.034f * s, kin.neck.y + 0.015f * s)
    val rightCollarPt = Offset(kin.neck.x + 0.034f * s, kin.neck.y + 0.015f * s)
    val lapelCrossPt = Offset(kin.chest.x + 0.036f * s, kin.chest.y + 0.025f * s)
    drawLine(
        color = collarColor,
        start = leftCollarPt,
        end = lapelCrossPt,
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = collarColor.copy(alpha = 0.82f),
        start = rightCollarPt,
        end = Offset(kin.chest.x - 0.008f * s, kin.chest.y - 0.008f * s),
        strokeWidth = 2.1.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawPath(
        path = tunicPath,
        color = ill.contourStroke,
        style = Stroke(width = contourW, join = StrokeJoin.Round)
    )

    // 7. Bilateral seated crossed-ankle / active-side flow cues
    if (bilateralSide != 0) {
        drawBilateralSideCues(skin = skin, kin = kin, s = s, bilateralSide = bilateralSide, detailPath = detailPath)
    }

    // 8. Calm rotated head & facial poise
    rotate(degrees = tiltDeg, pivot = kin.headCenter) {
        val faceRx = 0.058f * s
        val faceRy = 0.070f * s
        val faceTopLeft = Offset(kin.headCenter.x - faceRx, kin.headCenter.y - faceRy)
        val faceSize = Size(faceRx * 2f, faceRy * 2f)
        drawOval(color = ill.skinToneFill, topLeft = faceTopLeft, size = faceSize)
        drawOval(
            color = ill.contourStroke,
            topLeft = faceTopLeft,
            size = faceSize,
            style = Stroke(width = contourW * 0.9f)
        )

        // Parted Song fringe along forehead
        detailPath.reset()
        detailPath.moveTo(kin.headCenter.x - faceRx * 0.96f, kin.headCenter.y - faceRy * 0.22f)
        detailPath.quadraticTo(
            kin.headCenter.x - faceRx * 0.35f,
            kin.headCenter.y - faceRy * 0.92f,
            kin.headCenter.x,
            kin.headCenter.y - faceRy * 0.56f
        )
        detailPath.quadraticTo(
            kin.headCenter.x + faceRx * 0.35f,
            kin.headCenter.y - faceRy * 0.92f,
            kin.headCenter.x + faceRx * 0.96f,
            kin.headCenter.y - faceRy * 0.22f
        )
        detailPath.quadraticTo(
            kin.headCenter.x,
            kin.headCenter.y - faceRy * 1.18f,
            kin.headCenter.x - faceRx * 0.96f,
            kin.headCenter.y - faceRy * 0.22f
        )
        detailPath.close()
        drawPath(path = detailPath, color = ill.hairFill)

        // Botanical gouache warm cheek blushes
        if (ill.figureStyle == FigureStyle.BOTANICAL_GOUACHE) {
            val blushColor = Color(0xFFE6A598).copy(alpha = 0.36f)
            val blushR = 0.015f * s
            drawCircle(blushColor, blushR, Offset(kin.headCenter.x - 0.030f * s, kin.headCenter.y + 0.014f * s))
            drawCircle(blushColor, blushR, Offset(kin.headCenter.x + 0.030f * s, kin.headCenter.y + 0.014f * s))
        }

        // Serene brows & eyes (closed crescent vs soft open iris)
        val featureColor = skin.palette.inkPrimary.copy(alpha = 0.80f)
        val eyeY = kin.headCenter.y - 0.004f * s
        val eyeDx = 0.023f * s
        val eyeHalfW = 0.012f * s
        val fineStroke = max(1.1.dp.toPx(), contourW * 0.68f)

        // Brows
        drawLine(
            color = featureColor.copy(alpha = 0.58f),
            start = Offset(kin.headCenter.x - eyeDx - eyeHalfW, eyeY - 0.016f * s),
            end = Offset(kin.headCenter.x - eyeDx + eyeHalfW, eyeY - 0.018f * s),
            strokeWidth = fineStroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = featureColor.copy(alpha = 0.58f),
            start = Offset(kin.headCenter.x + eyeDx - eyeHalfW, eyeY - 0.018f * s),
            end = Offset(kin.headCenter.x + eyeDx + eyeHalfW, eyeY - 0.016f * s),
            strokeWidth = fineStroke * 0.85f,
            cap = StrokeCap.Round
        )

        if (keyframe.eyesClosed) {
            for (sign in intArrayOf(-1, 1)) {
                val ex = kin.headCenter.x + sign * eyeDx
                detailPath.reset()
                detailPath.moveTo(ex - eyeHalfW, eyeY)
                detailPath.quadraticTo(ex, eyeY + 0.010f * s, ex + eyeHalfW, eyeY)
                drawPath(
                    path = detailPath,
                    color = featureColor,
                    style = Stroke(width = fineStroke, cap = StrokeCap.Round)
                )
            }
        } else {
            for (sign in intArrayOf(-1, 1)) {
                val ex = kin.headCenter.x + sign * eyeDx
                drawCircle(
                    color = featureColor,
                    radius = max(1.6.dp.toPx(), 0.0065f * s),
                    center = Offset(ex, eyeY + 0.002f * s)
                )
            }
        }

        // Gentle calm smile arc
        val mouthY = kin.headCenter.y + 0.028f * s
        detailPath.reset()
        detailPath.moveTo(kin.headCenter.x - 0.011f * s, mouthY)
        detailPath.quadraticTo(kin.headCenter.x, mouthY + 0.007f * s, kin.headCenter.x + 0.011f * s, mouthY)
        drawPath(
            path = detailPath,
            color = featureColor.copy(alpha = 0.68f),
            style = Stroke(width = fineStroke * 0.85f, cap = StrokeCap.Round)
        )
    }

    // 9. Anterior arms (lap, desk, overhead, or warm eye palming)
    if (!kin.leftArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.leftArm, s = s, contourW = contourW, armPath = armPath)
    }
    if (!kin.rightArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.rightArm, s = s, contourW = contourW, armPath = armPath)
    }

    // 10. Meridian warmth highlights on target body regions
    drawHighlightedRegionWarmth(skin = skin, kin = kin, s = s, highlightedRegions = highlightedRegions)
}

private fun buildTorsoTunicPath(
    path: Path,
    kin: FigureKinematics,
    s: Float
) {
    val hipHalfW = 0.172f * s
    val leftHip = Offset(kin.pelvis.x - hipHalfW, kin.pelvis.y + 0.02f * s)
    val rightHip = Offset(kin.pelvis.x + hipHalfW, kin.pelvis.y + 0.02f * s)

    path.reset()
    path.moveTo(kin.leftShoulder.x, kin.leftShoulder.y)
    path.quadraticTo(kin.neck.x, kin.neck.y + 0.032f * s, kin.rightShoulder.x, kin.rightShoulder.y)
    path.cubicTo(
        kin.rightShoulder.x + 0.032f * s,
        (kin.rightShoulder.y + rightHip.y) * 0.45f,
        rightHip.x - 0.01f * s,
        rightHip.y - 0.06f * s,
        rightHip.x,
        rightHip.y
    )
    path.quadraticTo(kin.pelvis.x, kin.pelvis.y + 0.048f * s, leftHip.x, leftHip.y)
    path.cubicTo(
        leftHip.x + 0.01f * s,
        leftHip.y - 0.06f * s,
        kin.leftShoulder.x - 0.032f * s,
        (kin.leftShoulder.y + leftHip.y) * 0.45f,
        kin.leftShoulder.x,
        kin.leftShoulder.y
    )
    path.close()
}

private fun DrawScope.drawSleeveAndHand(
    skin: SongxiSkin,
    arm: ArmJoints,
    s: Float,
    contourW: Float,
    armPath: Path
) {
    val ill = skin.illustration
    val sleeveWidth = 0.058f * s

    armPath.reset()
    armPath.moveTo(arm.shoulder.x, arm.shoulder.y)
    armPath.quadraticTo(arm.elbow.x, arm.elbow.y, arm.hand.x, arm.hand.y)

    if (ill.figureStyle == FigureStyle.MOONLIT_CONTOUR) {
        drawPath(
            path = armPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.24f),
            style = Stroke(width = sleeveWidth + 5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    drawPath(
        path = armPath,
        color = ill.contourStroke,
        style = Stroke(width = sleeveWidth + contourW * 2f, cap = StrokeCap.Round)
    )
    drawPath(
        path = armPath,
        color = ill.garmentFill,
        style = Stroke(width = sleeveWidth, cap = StrokeCap.Round)
    )

    if (ill.figureStyle == FigureStyle.EDITORIAL_MINERAL_FLAT) {
        drawCircle(
            color = Color(0xFFB88655).copy(alpha = 0.78f),
            radius = sleeveWidth * 0.48f,
            center = arm.hand,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }

    // Oriented palm / wrist capsule
    val handW = 0.046f * s
    val handH = 0.030f * s
    rotate(degrees = arm.wristAngleDeg, pivot = arm.hand) {
        val topLeft = Offset(arm.hand.x - handW * 0.5f, arm.hand.y - handH * 0.5f)
        val corner = CornerRadius(handH * 0.5f, handH * 0.5f)
        drawRoundRect(
            color = ill.skinToneFill,
            topLeft = topLeft,
            size = Size(handW, handH),
            cornerRadius = corner
        )
        drawRoundRect(
            color = ill.contourStroke,
            topLeft = topLeft,
            size = Size(handW, handH),
            cornerRadius = corner,
            style = Stroke(width = contourW * 0.85f)
        )
    }
}

private fun DrawScope.drawBilateralSideCues(
    skin: SongxiSkin,
    kin: FigureKinematics,
    s: Float,
    bilateralSide: Int,
    detailPath: Path
) {
    val sign = if (bilateralSide < 0) -1f else 1f

    // 1. Seated crossed-ankle / knee fold leaning from active side onto opposite knee
    val ankleStart = Offset(kin.pelvis.x + sign * 0.145f * s, kin.pelvis.y + 0.055f * s)
    val kneeTarget = Offset(kin.pelvis.x - sign * 0.065f * s, kin.pelvis.y + 0.012f * s)
    drawLine(
        color = skin.palette.accentPrimary.copy(alpha = 0.58f),
        start = ankleStart,
        end = kneeTarget,
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Active-hand meridian emphasis ring
    val activeHand = if (bilateralSide < 0) kin.leftArm.hand else kin.rightArm.hand
    drawCircle(
        color = skin.palette.accentPrimary.copy(alpha = 0.52f),
        radius = 0.038f * s,
        center = activeHand,
        style = Stroke(width = 1.4.dp.toPx())
    )

    // 3. Lateral breath-flow calligraphic arc on the active flank
    val arcX = kin.chest.x + sign * 0.235f * s
    detailPath.reset()
    detailPath.moveTo(arcX, kin.chest.y - 0.085f * s)
    detailPath.quadraticTo(
        arcX + sign * 0.036f * s,
        kin.chest.y,
        arcX,
        kin.chest.y + 0.085f * s
    )
    drawPath(
        path = detailPath,
        color = skin.palette.accentPrimary.copy(alpha = 0.48f),
        style = Stroke(
            width = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    )
}

private fun DrawScope.drawHighlightedRegionWarmth(
    skin: SongxiSkin,
    kin: FigureKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>
) {
    if (highlightedRegions.isEmpty()) return

    val auraRadius = (0.068f * s).coerceAtLeast(6f)
    val dotRadius = 2.3.dp.toPx()

    fun drawWarmPoint(pt: Offset) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skin.palette.accentPrimary.copy(alpha = 0.24f), Color.Transparent),
                center = pt,
                radius = auraRadius
            ),
            radius = auraRadius,
            center = pt
        )
        drawCircle(
            color = skin.palette.sealVermilion.copy(alpha = 0.64f),
            radius = dotRadius,
            center = pt
        )
    }

    for (region in highlightedRegions) {
        when (region) {
            BodyRegion.EYE_BROW -> drawWarmPoint(kin.glabella)
            BodyRegion.POSTERIOR_NECK -> drawWarmPoint(kin.neck)
            BodyRegion.TRAPEZIUS_SHOULDER -> {
                drawWarmPoint(kin.leftShoulder)
                drawWarmPoint(kin.rightShoulder)
            }
            BodyRegion.THORACIC_CHEST -> drawWarmPoint(kin.chest)
            BodyRegion.LOWER_LUMBAR -> drawWarmPoint(kin.pelvis)
            BodyRegion.MOUSE_WRIST -> {
                drawWarmPoint(kin.leftArm.hand)
                drawWarmPoint(kin.rightArm.hand)
            }
        }
    }
}
