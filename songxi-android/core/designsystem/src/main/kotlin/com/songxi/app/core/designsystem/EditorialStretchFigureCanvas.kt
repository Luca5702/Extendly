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
import com.songxi.app.core.model.AnatomicalViewPlane
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.LateralAsymmetry
import com.songxi.app.core.model.PoseKeyframe
import com.songxi.app.core.model.StretchPose
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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
    asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED,
    skin: SongxiSkin = LocalSongxiSkin.current,
    eyesFreeMinimal: Boolean = false,
    viewPlane: AnatomicalViewPlane = pose.clinical.viewPlane,
    showBiomechanicalGuide: Boolean = false
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
    val side = when {
        !pose.bilateralSwitchAtHalfway -> 0
        !isSecondHalf -> asymmetry.primarySideSign
        else -> -asymmetry.primarySideSign
    }
    val effectiveKeyframe = if (pose.bilateralSwitchAtHalfway && side > 0) {
        baseKeyframe.mirroredBilaterally()
    } else {
        baseKeyframe
    }
    EditorialStretchFigureCanvas(
        keyframe = effectiveKeyframe,
        modifier = modifier,
        skin = skin,
        highlightedRegions = pose.targetRegions,
        eyesFreeMinimal = eyesFreeMinimal,
        bilateralSide = side,
        viewPlane = viewPlane,
        showBiomechanicalGuide = showBiomechanicalGuide
    )
}

@Composable
fun EditorialStretchFigureCanvas(
    keyframe: PoseKeyframe,
    modifier: Modifier = Modifier,
    skin: SongxiSkin = LocalSongxiSkin.current,
    highlightedRegions: Set<BodyRegion> = emptySet(),
    eyesFreeMinimal: Boolean = false,
    bilateralSide: Int = 0,
    viewPlane: AnatomicalViewPlane = AnatomicalViewPlane.CORONAL_FRONT,
    showBiomechanicalGuide: Boolean = false
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

        if (viewPlane == AnatomicalViewPlane.SAGITTAL_SIDE) {
            val sagKin = solveSagittalKinematics(
                keyframe = safeKeyframe,
                bilateralSide = safeBilateral,
                ox = ox,
                oy = oy,
                s = s
            )
            if (eyesFreeMinimal) {
                drawEyesFreeSagittalFigure(
                    skin = skin,
                    kin = sagKin,
                    s = s,
                    highlightedRegions = highlightedRegions,
                    bilateralSide = safeBilateral,
                    tunicPath = tunicPath,
                    armPath = armPath
                )
            } else {
                drawSagittalEditorialFigure(
                    skin = skin,
                    keyframe = safeKeyframe,
                    kin = sagKin,
                    s = s,
                    highlightedRegions = highlightedRegions,
                    bilateralSide = safeBilateral,
                    pt = ::pt,
                    tunicPath = tunicPath,
                    armPath = armPath,
                    detailPath = detailPath
                )
                if (showBiomechanicalGuide) {
                    drawSagittalBiomechanicalGuide(
                        skin = skin,
                        keyframe = safeKeyframe,
                        kin = sagKin,
                        s = s,
                        highlightedRegions = highlightedRegions,
                        detailPath = detailPath
                    )
                }
            }
        } else {
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
                if (showBiomechanicalGuide) {
                    drawCoronalBiomechanicalGuide(
                        skin = skin,
                        keyframe = safeKeyframe,
                        kin = kin,
                        s = s,
                        highlightedRegions = highlightedRegions,
                        bilateralSide = safeBilateral,
                        detailPath = detailPath
                    )
                }
            }
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

private data class SagittalKinematics(
    val pelvis: Offset,
    val lumbarMid: Offset,
    val thoracicMid: Offset,
    val neckBase: Offset,
    val headCenter: Offset,
    val noseTip: Offset,
    val glabella: Offset,
    val shoulder: Offset,
    val knee: Offset,
    val ankle: Offset,
    val arm: ArmJoints,
    val headPitchDeg: Float,
    val hasDeskProp: Boolean
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

private fun solveSagittalKinematics(
    keyframe: PoseKeyframe,
    bilateralSide: Int,
    ox: Float,
    oy: Float,
    s: Float
): SagittalKinematics {
    fun pt(nx: Float, ny: Float): Offset = Offset(ox + nx * s, oy + ny * s)

    val spine = keyframe.spineArcRatio.coerceIn(-1f, 1f)
    val chin = keyframe.chinTuckRatio.coerceIn(-1f, 1f)
    val avgDrop = ((keyframe.leftShoulderDropRatio + keyframe.rightShoulderDropRatio) * 0.5f).coerceIn(0f, 1f)
    val armDeg = ((keyframe.leftArmAngleDeg + keyframe.rightArmAngleDeg) * 0.5f).coerceIn(0f, 165f)
    val wrist = keyframe.wristFlexRatio.coerceIn(-1f, 1f)

    val isHipHingeFold = spine < -0.15f && armDeg in 24f..44f
    val hingeAmount = if (isHipHingeFold) (-spine).coerceIn(0f, 1f) else 0f

    val pelvisNx = 0.39f - spine * 0.012f
    val pelvisNy = 0.755f

    // Articulated S-curve spine in sagittal profile (+x = anterior/right, -x = posterior/left)
    val lumbarNx = pelvisNx + spine * 0.042f + hingeAmount * 0.032f
    val lumbarNy = 0.635f + hingeAmount * 0.014f
    val thoracicNx = pelvisNx + 0.016f + spine * 0.048f + hingeAmount * 0.095f
    val thoracicNy = 0.515f - spine * 0.022f + hingeAmount * 0.042f
    val neckNx = thoracicNx + 0.012f - max(0f, chin) * 0.018f + hingeAmount * 0.032f
    val neckNy = thoracicNy - 0.088f + avgDrop * 0.010f

    // Axial posterior chin glide (-x) vs gentle occiput cradle upward tilt (-chin)
    val headNx = neckNx + 0.024f - chin * 0.056f + hingeAmount * 0.022f
    val headNy = neckNy - 0.076f - max(0f, chin) * 0.010f + min(0f, chin) * 0.012f
    val headPitchDeg = -chin * 14f + hingeAmount * 16f - max(0f, spine) * 6f

    val shNx = thoracicNx + 0.008f - max(0f, spine) * 0.018f
    val shNy = neckNy + 0.042f + avgDrop * 0.046f

    // Posterior clasp behind hip vs desk reach vs occiput cradle vs lap rest
    val isPosteriorClasp = spine > 0.32f && armDeg in 25f..48f
    val isDeskReach = armDeg in 50f..82f
    val isOcciputCradle = armDeg >= 90f

    val (elNx, elNy, handNx, handNy, wristDeg) = when {
        isPosteriorClasp -> {
            val claspPull = spine.coerceIn(0f, 1f)
            floatArrayOf(
                shNx - 0.075f - claspPull * 0.028f,
                shNy + 0.115f,
                pelvisNx - 0.095f - claspPull * 0.042f,
                pelvisNy - 0.035f + avgDrop * 0.020f,
                -22f
            )
        }
        isOcciputCradle -> {
            floatArrayOf(
                headNx + 0.085f,
                headNy + 0.015f,
                headNx - 0.052f,
                headNy + 0.012f,
                -35f
            )
        }
        isDeskReach -> {
            floatArrayOf(
                shNx + 0.082f,
                shNy + 0.105f,
                0.715f,
                0.605f - wrist * 0.015f,
                wrist * 18f
            )
        }
        else -> {
            floatArrayOf(
                shNx + 0.035f + hingeAmount * 0.035f,
                shNy + 0.125f,
                0.555f + hingeAmount * 0.055f,
                0.725f + hingeAmount * 0.015f,
                8f
            )
        }
    }

    val pitchRad = Math.toRadians(headPitchDeg.toDouble()).toFloat()
    val headCenterPt = pt(headNx, headNy)
    val noseOffset = Offset(
        x = headCenterPt.x + (cos(pitchRad) * 0.064f - sin(pitchRad) * 0.008f) * s,
        y = headCenterPt.y + (sin(pitchRad) * 0.064f + cos(pitchRad) * 0.008f) * s
    )
    val glabellaPt = Offset(
        x = headCenterPt.x + cos(pitchRad) * 0.054f * s,
        y = headCenterPt.y - 0.018f * s + sin(pitchRad) * 0.045f * s
    )

    val legShift = bilateralSide * 0.016f
    return SagittalKinematics(
        pelvis = pt(pelvisNx, pelvisNy),
        lumbarMid = pt(lumbarNx, lumbarNy),
        thoracicMid = pt(thoracicNx, thoracicNy),
        neckBase = pt(neckNx, neckNy),
        headCenter = headCenterPt,
        noseTip = noseOffset,
        glabella = glabellaPt,
        shoulder = pt(shNx, shNy),
        knee = pt(0.655f + legShift, 0.742f - max(0, bilateralSide) * 0.022f),
        ankle = pt(0.675f + legShift, 0.875f),
        arm = ArmJoints(
            shoulder = pt(shNx, shNy),
            elbow = pt(elNx, elNy),
            hand = pt(handNx, handNy),
            wristAngleDeg = wristDeg,
            isPosterior = isPosteriorClasp
        ),
        headPitchDeg = headPitchDeg,
        hasDeskProp = isDeskReach
    )
}

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

private fun DrawScope.drawEyesFreeSagittalFigure(
    skin: SongxiSkin,
    kin: SagittalKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    bilateralSide: Int,
    tunicPath: Path,
    armPath: Path
) {
    val glowStroke = 6.0.dp.toPx()
    val crispStroke = 2.0.dp.toPx()
    val glowColor = skin.palette.accentPrimaryGlow.copy(alpha = 0.22f)
    val lineColor = skin.palette.accentPrimary.copy(alpha = 0.74f)

    buildSagittalTorsoPath(tunicPath, kin, s)
    drawPath(tunicPath, color = glowColor, style = Stroke(width = glowStroke, join = StrokeJoin.Round))
    drawPath(tunicPath, color = lineColor, style = Stroke(width = crispStroke, join = StrokeJoin.Round))

    val headR = 0.064f * s
    drawCircle(color = glowColor, radius = headR, center = kin.headCenter, style = Stroke(width = glowStroke))
    drawCircle(color = lineColor, radius = headR, center = kin.headCenter, style = Stroke(width = crispStroke))

    // Articulated S-curve spinal meridian
    armPath.reset()
    armPath.moveTo(kin.pelvis.x, kin.pelvis.y)
    armPath.cubicTo(
        kin.lumbarMid.x,
        kin.lumbarMid.y,
        kin.thoracicMid.x,
        kin.thoracicMid.y,
        kin.neckBase.x,
        kin.neckBase.y
    )
    drawPath(
        armPath,
        color = skin.palette.accentSecondary.copy(alpha = 0.62f),
        style = Stroke(
            width = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    )

    armPath.reset()
    armPath.moveTo(kin.arm.shoulder.x, kin.arm.shoulder.y)
    armPath.quadraticTo(kin.arm.elbow.x, kin.arm.elbow.y, kin.arm.hand.x, kin.arm.hand.y)
    drawPath(armPath, color = glowColor, style = Stroke(width = glowStroke, cap = StrokeCap.Round))
    drawPath(armPath, color = lineColor, style = Stroke(width = crispStroke, cap = StrokeCap.Round))

    if (bilateralSide != 0) {
        val sideArcX = if (bilateralSide < 0) kin.pelvis.x - 0.08f * s else kin.knee.x + 0.05f * s
        drawCircle(
            color = skin.palette.accentPrimary.copy(alpha = 0.55f),
            radius = 0.032f * s,
            center = Offset(sideArcX, kin.pelvis.y - 0.04f * s),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
    drawSagittalRegionWarmth(skin = skin, kin = kin, s = s, highlightedRegions = highlightedRegions)
}

private fun DrawScope.drawSagittalEditorialFigure(
    skin: SongxiSkin,
    keyframe: PoseKeyframe,
    kin: SagittalKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    bilateralSide: Int,
    pt: (Float, Float) -> Offset,
    tunicPath: Path,
    armPath: Path,
    detailPath: Path
) {
    val ill = skin.illustration
    val contourW = (ill.contourStrokeWidthDp.finiteClamp(0.5f, 6f, 1.6f) * 0.86f).dp.toPx()

    // 1. Minimalist Song wooden meditation chair ("宋式禅椅") & soft floor horizon
    val woodStroke = skin.palette.borderHairline.copy(alpha = 0.88f)
    // Floor horizon
    drawLine(
        color = skin.palette.accentSecondary.copy(alpha = 0.26f),
        start = pt(0.20f, 0.892f),
        end = pt(0.78f, 0.892f),
        strokeWidth = 0.95.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Chair legs (subtle tapered mortise-and-tenon stance)
    drawLine(
        color = woodStroke.copy(alpha = 0.58f),
        start = pt(0.275f, 0.792f),
        end = pt(0.262f, 0.890f),
        strokeWidth = 1.35.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = woodStroke.copy(alpha = 0.58f),
        start = pt(0.545f, 0.792f),
        end = pt(0.556f, 0.890f),
        strokeWidth = 1.35.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Gently curved backrest + cushioned seat rail
    detailPath.reset()
    detailPath.moveTo(pt(0.246f, 0.535f).x, pt(0.246f, 0.535f).y)
    detailPath.quadraticTo(pt(0.262f, 0.665f).x, pt(0.262f, 0.665f).y, pt(0.270f, 0.790f).x, pt(0.270f, 0.790f).y)
    drawPath(
        path = detailPath,
        color = woodStroke.copy(alpha = 0.72f),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = skin.palette.surfaceCardSubtle,
        topLeft = pt(0.252f, 0.778f),
        size = Size(0.325f * s, 0.018f * s),
        cornerRadius = CornerRadius(0.009f * s, 0.009f * s)
    )
    drawLine(
        color = woodStroke,
        start = pt(0.248f, 0.792f),
        end = pt(0.582f, 0.792f),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    if (kin.hasDeskProp) {
        drawLine(
            color = skin.palette.accentSecondary.copy(alpha = 0.45f),
            start = pt(0.67f, 0.625f),
            end = pt(0.83f, 0.625f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 2. Posterior arm (behind-back clasp) if behind torso
    if (kin.arm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.arm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }

    // 3. Tapered Song seated trousers + slender ankle + soft cloth slipper ("素履")
    if (bilateralSide > 0) {
        detailPath.reset()
        detailPath.moveTo(kin.pelvis.x + 0.02f * s, kin.pelvis.y - 0.01f * s)
        detailPath.quadraticTo(
            (kin.pelvis.x + kin.knee.x) * 0.52f,
            kin.pelvis.y - 0.088f * s,
            kin.knee.x - 0.02f * s,
            kin.knee.y - 0.026f * s
        )
        drawPath(
            path = detailPath,
            color = ill.garmentShadow,
            style = Stroke(width = 0.048f * s, cap = StrokeCap.Round)
        )
    }

    val cuffY = kin.ankle.y - 0.022f * s
    // Slender skin ankle + cloud-toe Song cloth slipper resting on floor
    drawLine(
        color = ill.skinToneFill,
        start = Offset(kin.ankle.x - 0.004f * s, cuffY - 0.004f * s),
        end = Offset(kin.ankle.x, kin.ankle.y + 0.002f * s),
        strokeWidth = 0.022f * s,
        cap = StrokeCap.Round
    )
    detailPath.reset()
    val heelX = kin.ankle.x - 0.016f * s
    val soleY = kin.ankle.y + 0.015f * s
    val toeX = kin.ankle.x + 0.056f * s
    detailPath.moveTo(heelX, kin.ankle.y - 0.002f * s)
    detailPath.quadraticTo(kin.ankle.x + 0.016f * s, kin.ankle.y - 0.006f * s, toeX - 0.010f * s, kin.ankle.y + 0.001f * s)
    detailPath.quadraticTo(toeX + 0.006f * s, kin.ankle.y + 0.006f * s, toeX - 0.002f * s, soleY)
    detailPath.lineTo(heelX + 0.004f * s, soleY)
    detailPath.quadraticTo(heelX - 0.004f * s, soleY, heelX, kin.ankle.y - 0.002f * s)
    detailPath.close()
    drawPath(path = detailPath, color = ill.garmentShadow)
    drawPath(
        path = detailPath,
        color = ill.contourStroke,
        style = Stroke(width = contourW * 0.82f, join = StrokeJoin.Round)
    )

    // Sculpted tapered trouser silhouette (hip -> rounded knee -> tapered cuff)
    armPath.reset()
    armPath.moveTo(kin.pelvis.x + 0.01f * s, kin.pelvis.y - 0.024f * s)
    armPath.quadraticTo(
        (kin.pelvis.x + kin.knee.x) * 0.54f,
        kin.knee.y - 0.036f * s,
        kin.knee.x + 0.028f * s,
        kin.knee.y - 0.012f * s
    )
    armPath.quadraticTo(
        kin.knee.x + 0.034f * s,
        (kin.knee.y + cuffY) * 0.48f,
        kin.ankle.x + 0.020f * s,
        cuffY
    )
    armPath.quadraticTo(kin.ankle.x - 0.002f * s, cuffY + 0.006f * s, kin.ankle.x - 0.024f * s, cuffY - 0.002f * s)
    armPath.quadraticTo(
        kin.knee.x - 0.022f * s,
        (kin.knee.y + cuffY) * 0.54f,
        kin.knee.x - 0.026f * s,
        kin.knee.y + 0.028f * s
    )
    armPath.lineTo(kin.pelvis.x - 0.01f * s, kin.pelvis.y + 0.032f * s)
    armPath.close()

    drawPath(path = armPath, color = ill.garmentFill)
    drawPath(
        path = armPath,
        color = ill.contourStroke,
        style = Stroke(width = contourW * 0.92f, join = StrokeJoin.Round)
    )
    // Subtle inner knee drapery crease
    drawLine(
        color = ill.garmentShadow.copy(alpha = 0.72f),
        start = Offset(kin.knee.x - 0.024f * s, kin.knee.y + 0.022f * s),
        end = Offset(kin.knee.x - 0.004f * s, kin.knee.y + 0.005f * s),
        strokeWidth = 0.95.dp.toPx(),
        cap = StrokeCap.Round
    )

    if (bilateralSide < 0) {
        val crossedKnee = Offset(kin.pelvis.x + 0.145f * s, kin.pelvis.y - 0.055f * s)
        val crossedAnkle = Offset(kin.knee.x - 0.035f * s, kin.knee.y - 0.018f * s)
        drawLine(
            color = ill.garmentShadow,
            start = Offset(kin.pelvis.x + 0.02f * s, kin.pelvis.y),
            end = crossedKnee,
            strokeWidth = 0.044f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = skin.palette.accentPrimary.copy(alpha = 0.62f),
            start = crossedKnee,
            end = crossedAnkle,
            strokeWidth = 2.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    } else if (bilateralSide > 0) {
        drawCircle(
            color = skin.palette.accentPrimary.copy(alpha = 0.52f),
            radius = 0.028f * s,
            center = Offset(kin.knee.x - 0.02f * s, kin.knee.y - 0.03f * s),
            style = Stroke(width = 1.25.dp.toPx())
        )
    }

    // 4. Moonlit / ink rim-light aura
    buildSagittalTorsoPath(tunicPath, kin, s)
    if (ill.figureStyle == FigureStyle.MOONLIT_CONTOUR) {
        drawPath(
            path = tunicPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.22f),
            style = Stroke(width = 3.8.dp.toPx(), join = StrokeJoin.Round)
        )
    } else if (ill.figureStyle == FigureStyle.INK_BRUSH_WASH) {
        drawPath(
            path = tunicPath,
            color = ill.contourStroke.copy(alpha = 0.14f),
            style = Stroke(width = 3.2.dp.toPx(), join = StrokeJoin.Round)
        )
    }

    // 5. Slender anatomical neck column
    detailPath.reset()
    detailPath.moveTo(kin.headCenter.x - 0.028f * s, kin.headCenter.y + 0.038f * s)
    detailPath.lineTo(kin.headCenter.x + 0.014f * s, kin.headCenter.y + 0.044f * s)
    detailPath.lineTo(kin.neckBase.x + 0.020f * s, kin.neckBase.y + 0.028f * s)
    detailPath.lineTo(kin.neckBase.x - 0.022f * s, kin.neckBase.y + 0.022f * s)
    detailPath.close()
    drawPath(path = detailPath, color = ill.skinToneFill)
    drawLine(
        color = ill.contourStroke.copy(alpha = 0.62f),
        start = Offset(kin.headCenter.x - 0.027f * s, kin.headCenter.y + 0.040f * s),
        end = Offset(kin.neckBase.x - 0.021f * s, kin.neckBase.y + 0.020f * s),
        strokeWidth = contourW * 0.82f,
        cap = StrokeCap.Round
    )

    // 6. Draped side-profile Song tunic, curved lapel & internal spinal drapery seam
    drawPath(path = tunicPath, color = ill.garmentFill)

    // Soft lower hem shadow wash
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.064f * s, kin.pelvis.y + 0.008f * s)
    detailPath.quadraticTo(kin.pelvis.x + 0.012f * s, kin.pelvis.y + 0.042f * s, kin.pelvis.x + 0.084f * s, kin.pelvis.y + 0.014f * s)
    detailPath.lineTo(kin.pelvis.x + 0.088f * s, kin.pelvis.y + 0.026f * s)
    detailPath.quadraticTo(kin.pelvis.x + 0.010f * s, kin.pelvis.y + 0.052f * s, kin.pelvis.x - 0.068f * s, kin.pelvis.y + 0.026f * s)
    detailPath.close()
    drawPath(path = detailPath, color = ill.garmentShadow.copy(alpha = 0.72f))

    // Subtle anatomical S-curve spinal seam along posterior tunic
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.038f * s, kin.pelvis.y - 0.02f * s)
    detailPath.cubicTo(
        kin.lumbarMid.x - 0.042f * s,
        kin.lumbarMid.y,
        kin.thoracicMid.x - 0.044f * s,
        kin.thoracicMid.y,
        kin.neckBase.x - 0.020f * s,
        kin.neckBase.y + 0.015f * s
    )
    drawPath(
        path = detailPath,
        color = skin.palette.accentPrimary.copy(alpha = 0.34f),
        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
    )

    // Curved Song side collar trim ("曲领右衽") + white inner under-collar edge
    val collarColor = when (ill.figureStyle) {
        FigureStyle.EDITORIAL_MINERAL_FLAT -> Color(0xFFB88655)
        FigureStyle.MOONLIT_CONTOUR -> skin.palette.accentSecondary
        FigureStyle.BOTANICAL_GOUACHE -> skin.palette.accentSecondary
        FigureStyle.INK_BRUSH_WASH -> ill.garmentShadow
    }
    detailPath.reset()
    detailPath.moveTo(kin.neckBase.x - 0.022f * s, kin.neckBase.y + 0.008f * s)
    detailPath.quadraticTo(
        kin.neckBase.x + 0.018f * s,
        kin.neckBase.y + 0.024f * s,
        kin.thoracicMid.x + 0.062f * s,
        kin.thoracicMid.y + 0.026f * s
    )
    drawPath(
        path = detailPath,
        color = skin.palette.surfaceCard.copy(alpha = 0.85f),
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = detailPath,
        color = collarColor,
        style = Stroke(width = 1.45.dp.toPx(), cap = StrokeCap.Round)
    )

    drawPath(
        path = tunicPath,
        color = ill.contourStroke,
        style = Stroke(width = contourW, join = StrokeJoin.Round)
    )

    // 7. Sculpted Song side-profile head, hair chignon & soft East Asian facial contour
    rotate(degrees = kin.headPitchDeg, pivot = kin.headCenter) {
        val bunCenter = Offset(kin.headCenter.x - 0.066f * s, kin.headCenter.y - 0.030f * s)
        // Double-looped Song chignon
        drawOval(
            color = ill.hairFill,
            topLeft = Offset(bunCenter.x - 0.030f * s, bunCenter.y - 0.036f * s),
            size = Size(0.058f * s, 0.068f * s)
        )

        when (ill.figureStyle) {
            FigureStyle.EDITORIAL_MINERAL_FLAT -> {
                drawLine(
                    color = Color(0xFFB88655),
                    start = Offset(bunCenter.x - 0.024f * s, bunCenter.y + 0.024f * s),
                    end = Offset(bunCenter.x + 0.022f * s, bunCenter.y - 0.032f * s),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = skin.palette.accentPrimary,
                    radius = 2.0.dp.toPx(),
                    center = Offset(bunCenter.x + 0.022f * s, bunCenter.y - 0.032f * s)
                )
            }
            FigureStyle.BOTANICAL_GOUACHE -> {
                val ornament = Offset(bunCenter.x + 0.012f * s, bunCenter.y - 0.020f * s)
                drawCircle(color = Color(0xFFE6A598), radius = 2.8.dp.toPx(), center = ornament)
                drawCircle(color = skin.palette.accentPrimary, radius = 1.2.dp.toPx(), center = ornament)
            }
            else -> Unit
        }

        val headRx = 0.058f * s
        val headRy = 0.066f * s
        val cx = kin.headCenter.x
        val cy = kin.headCenter.y

        // Unified organic cranial + soft profile silhouette (smooth brow -> rounded nose -> gentle chin -> jaw)
        detailPath.reset()
        detailPath.moveTo(cx, cy - headRy)
        // Forehead to glabella
        detailPath.cubicTo(
            cx + headRx * 0.65f, cy - headRy,
            cx + headRx * 0.94f, cy - headRy * 0.56f,
            cx + headRx * 0.92f, cy - headRy * 0.16f
        )
        // Soft curved nasal bridge & rounded nose tip (zero sharp Pinocchio corner!)
        detailPath.cubicTo(
            cx + headRx * 1.08f, cy - headRy * 0.02f,
            cx + headRx * 1.10f, cy + headRy * 0.14f,
            cx + headRx * 0.95f, cy + headRy * 0.22f
        )
        // Subtle philtrum, calm lips & rounded chin
        detailPath.cubicTo(
            cx + headRx * 0.98f, cy + headRy * 0.36f,
            cx + headRx * 0.88f, cy + headRy * 0.82f,
            cx + headRx * 0.24f, cy + headRy * 0.94f
        )
        // Jawline & occiput back to crown
        detailPath.cubicTo(
            cx - headRx * 0.58f, cy + headRy * 0.98f,
            cx - headRx * 1.02f, cy + headRy * 0.50f,
            cx - headRx * 1.00f, cy - headRy * 0.06f
        )
        detailPath.cubicTo(
            cx - headRx * 0.98f, cy - headRy * 0.66f,
            cx - headRx * 0.56f, cy - headRy,
            cx, cy - headRy
        )
        detailPath.close()

        drawPath(path = detailPath, color = ill.skinToneFill)
        drawPath(
            path = detailPath,
            color = ill.contourStroke,
            style = Stroke(width = contourW * 0.86f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Flowing Song crown hair & delicate sideburn temple lock ("鬓发")
        armPath.reset()
        armPath.moveTo(cx + headRx * 0.78f, cy - headRy * 0.46f)
        armPath.cubicTo(
            cx + headRx * 0.25f, cy - headRy * 1.16f,
            cx - headRx * 0.82f, cy - headRy * 1.06f,
            cx - headRx * 1.02f, cy - headRy * 0.10f
        )
        armPath.cubicTo(
            cx - headRx * 1.06f, cy + headRy * 0.48f,
            cx - headRx * 0.62f, cy + headRy * 0.68f,
            cx - headRx * 0.34f, cy + headRy * 0.56f
        )
        armPath.quadraticTo(
            cx - headRx * 0.24f, cy + headRy * 0.10f,
            cx - headRx * 0.04f, cy - headRy * 0.06f
        )
        armPath.quadraticTo(
            cx + headRx * 0.28f, cy - headRy * 0.36f,
            cx + headRx * 0.78f, cy - headRy * 0.46f
        )
        armPath.close()
        drawPath(path = armPath, color = ill.hairFill)

        // Delicate auricle curve
        detailPath.reset()
        detailPath.moveTo(cx - headRx * 0.16f, cy + headRy * 0.02f)
        detailPath.quadraticTo(cx - headRx * 0.30f, cy + headRy * 0.18f, cx - headRx * 0.14f, cy + headRy * 0.34f)
        drawPath(
            path = detailPath,
            color = ill.contourStroke.copy(alpha = 0.50f),
            style = Stroke(width = contourW * 0.65f, cap = StrokeCap.Round)
        )

        // Tapered willow eyebrow ("远山黛"), calm closed eye & serene lip corner
        val featureColor = skin.palette.inkPrimary.copy(alpha = 0.78f)
        val fineStroke = max(1.0.dp.toPx(), contourW * 0.64f)
        val eyeX = cx + headRx * 0.48f
        val eyeY = cy - headRy * 0.05f

        detailPath.reset()
        detailPath.moveTo(eyeX - 0.015f * s, eyeY - 0.015f * s)
        detailPath.quadraticTo(eyeX, eyeY - 0.022f * s, eyeX + 0.014f * s, eyeY - 0.013f * s)
        drawPath(
            path = detailPath,
            color = featureColor.copy(alpha = 0.56f),
            style = Stroke(width = fineStroke * 0.80f, cap = StrokeCap.Round)
        )

        if (keyframe.eyesClosed) {
            detailPath.reset()
            detailPath.moveTo(eyeX - 0.011f * s, eyeY)
            detailPath.quadraticTo(eyeX + 0.001f * s, eyeY + 0.008f * s, eyeX + 0.013f * s, eyeY - 0.001f * s)
            drawPath(
                path = detailPath,
                color = featureColor,
                style = Stroke(width = fineStroke * 0.92f, cap = StrokeCap.Round)
            )
        } else {
            drawCircle(
                color = featureColor,
                radius = max(1.4.dp.toPx(), 0.0055f * s),
                center = Offset(eyeX + 0.002f * s, eyeY + 0.002f * s)
            )
        }

        // Subtle serene lip line
        detailPath.reset()
        detailPath.moveTo(cx + headRx * 0.68f, cy + headRy * 0.42f)
        detailPath.quadraticTo(cx + headRx * 0.80f, cy + headRy * 0.46f, cx + headRx * 0.90f, cy + headRy * 0.41f)
        drawPath(
            path = detailPath,
            color = featureColor.copy(alpha = 0.55f),
            style = Stroke(width = fineStroke * 0.75f, cap = StrokeCap.Round)
        )
    }

    // 8. Foreground wide-sleeve & anatomical hand
    if (!kin.arm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.arm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }

    // 9. Soft ink-wash meridian warmth highlights (zero hard red rivets!)
    drawSagittalRegionWarmth(skin = skin, kin = kin, s = s, highlightedRegions = highlightedRegions)
}

private fun buildSagittalTorsoPath(
    path: Path,
    kin: SagittalKinematics,
    s: Float
) {
    val backHip = Offset(kin.pelvis.x - 0.068f * s, kin.pelvis.y + 0.025f * s)
    val frontHip = Offset(kin.pelvis.x + 0.088f * s, kin.pelvis.y + 0.022f * s)
    val backNeck = Offset(kin.neckBase.x - 0.028f * s, kin.neckBase.y + 0.010f * s)
    val frontNeck = Offset(kin.neckBase.x + 0.028f * s, kin.neckBase.y + 0.020f * s)

    path.reset()
    path.moveTo(backNeck.x, backNeck.y)
    path.quadraticTo(kin.neckBase.x, kin.neckBase.y + 0.018f * s, frontNeck.x, frontNeck.y)
    path.cubicTo(
        kin.thoracicMid.x + 0.080f * s,
        kin.thoracicMid.y - 0.008f * s,
        kin.lumbarMid.x + 0.082f * s,
        kin.lumbarMid.y + 0.004f * s,
        frontHip.x,
        frontHip.y
    )
    path.quadraticTo(kin.pelvis.x + 0.01f * s, kin.pelvis.y + 0.050f * s, backHip.x, backHip.y)
    path.cubicTo(
        kin.lumbarMid.x - 0.066f * s,
        kin.lumbarMid.y,
        kin.thoracicMid.x - 0.066f * s,
        kin.thoracicMid.y,
        backNeck.x,
        backNeck.y
    )
    path.close()
}

private fun DrawScope.drawSagittalRegionWarmth(
    skin: SongxiSkin,
    kin: SagittalKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>
) {
    if (highlightedRegions.isEmpty()) return
    for (region in highlightedRegions) {
        when (region) {
            BodyRegion.EYE_BROW -> drawSoftQiWarmthAura(skin, kin.glabella, s)
            BodyRegion.POSTERIOR_NECK -> drawSoftQiWarmthAura(
                skin,
                Offset(kin.neckBase.x - 0.016f * s, kin.neckBase.y - 0.016f * s),
                s
            )
            BodyRegion.TRAPEZIUS_SHOULDER -> drawSoftQiWarmthAura(skin, kin.shoulder, s)
            BodyRegion.THORACIC_CHEST -> drawSoftQiWarmthAura(skin, kin.thoracicMid, s)
            BodyRegion.LOWER_LUMBAR -> drawSoftQiWarmthAura(skin, kin.lumbarMid, s)
            BodyRegion.MOUSE_WRIST -> drawSoftQiWarmthAura(skin, kin.arm.hand, s)
        }
    }
}

private fun DrawScope.drawSagittalBiomechanicalGuide(
    skin: SongxiSkin,
    keyframe: PoseKeyframe,
    kin: SagittalKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    detailPath: Path
) {
    val axisColor = skin.palette.accentSecondary.copy(alpha = 0.56f)
    val vectorColor = skin.palette.sealVermilion.copy(alpha = 0.78f)
    val fascialColor = skin.palette.accentPrimary.copy(alpha = 0.68f)

    // 1. Plumb vertical alignment reference passing through sit-bone (百会—坐骨铅垂线)
    val plumbTop = Offset(kin.pelvis.x, kin.pelvis.y - 0.62f * s)
    val plumbBottom = Offset(kin.pelvis.x, kin.pelvis.y + 0.05f * s)
    drawLine(
        color = axisColor,
        start = plumbTop,
        end = plumbBottom,
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f)
    )

    // 2. Sit-bone & scapular anti-compensation anchor rings (防代偿锚点)
    drawAnchorTargetRing(center = kin.pelvis, radius = 0.026f * s, color = vectorColor)
    drawAnchorTargetRing(center = kin.shoulder, radius = 0.020f * s, color = axisColor)

    // 3. Articulated sagittal spinal fascial tract highlight
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.022f * s, kin.pelvis.y - 0.01f * s)
    detailPath.cubicTo(
        kin.lumbarMid.x - 0.026f * s,
        kin.lumbarMid.y,
        kin.thoracicMid.x - 0.028f * s,
        kin.thoracicMid.y,
        kin.neckBase.x - 0.016f * s,
        kin.neckBase.y - 0.03f * s
    )
    drawPath(
        path = detailPath,
        color = fascialColor,
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // 4. Biomechanical directional vector arrows by primary target region
    when {
        BodyRegion.POSTERIOR_NECK in highlightedRegions -> {
            if (keyframe.chinTuckRatio >= 0f) {
                // Horizontal posterior chin-tuck glide + crown axial lift
                drawGuideVectorArrow(
                    start = Offset(kin.headCenter.x + 0.085f * s, kin.headCenter.y + 0.025f * s),
                    end = Offset(kin.headCenter.x + 0.010f * s, kin.headCenter.y + 0.025f * s),
                    color = vectorColor,
                    s = s
                )
                drawGuideVectorArrow(
                    start = Offset(kin.headCenter.x, kin.headCenter.y - 0.075f * s),
                    end = Offset(kin.headCenter.x, kin.headCenter.y - 0.135f * s),
                    color = fascialColor,
                    s = s
                )
            } else {
                // Gentle occiput cradle arc
                drawGuideVectorArrow(
                    start = Offset(kin.headCenter.x, kin.headCenter.y - 0.075f * s),
                    end = Offset(kin.headCenter.x - 0.048f * s, kin.headCenter.y - 0.115f * s),
                    color = vectorColor,
                    s = s
                )
            }
        }
        BodyRegion.THORACIC_CHEST in highlightedRegions -> {
            val dir = if (keyframe.spineArcRatio >= 0f) 1f else -1f
            drawGuideVectorArrow(
                start = Offset(kin.thoracicMid.x + dir * 0.035f * s, kin.thoracicMid.y),
                end = Offset(kin.thoracicMid.x + dir * 0.115f * s, kin.thoracicMid.y - dir * 0.035f * s),
                color = vectorColor,
                s = s
            )
        }
        else -> {
            val dir = if (keyframe.spineArcRatio >= 0f) 1f else -1f
            drawGuideVectorArrow(
                start = Offset(kin.lumbarMid.x - 0.055f * s, kin.lumbarMid.y),
                end = Offset(kin.lumbarMid.x - (0.055f - dir * 0.065f) * s, kin.lumbarMid.y - 0.045f * s),
                color = vectorColor,
                s = s
            )
        }
    }
}

private fun DrawScope.drawCoronalBiomechanicalGuide(
    skin: SongxiSkin,
    keyframe: PoseKeyframe,
    kin: FigureKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>,
    bilateralSide: Int,
    detailPath: Path
) {
    val axisColor = skin.palette.accentSecondary.copy(alpha = 0.56f)
    val vectorColor = skin.palette.sealVermilion.copy(alpha = 0.78f)
    val fascialColor = skin.palette.accentPrimary.copy(alpha = 0.68f)

    // 1. Plumb vertical alignment reference (百会—坐骨中轴线)
    drawLine(
        color = axisColor,
        start = Offset(kin.pelvis.x, kin.pelvis.y - 0.64f * s),
        end = Offset(kin.pelvis.x, kin.pelvis.y + 0.04f * s),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f)
    )
    drawAnchorTargetRing(center = kin.pelvis, radius = 0.025f * s, color = axisColor)

    // 2. Region-specific myofascial tension band & directional elongation vector
    when {
        BodyRegion.TRAPEZIUS_SHOULDER in highlightedRegions || BodyRegion.POSTERIOR_NECK in highlightedRegions -> {
            val anchorShoulder = if (keyframe.headTiltDeg >= 0f) kin.leftShoulder else kin.rightShoulder
            val earAnchor = Offset(
                x = kin.headCenter.x + (if (keyframe.headTiltDeg >= 0f) -0.042f else 0.042f) * s,
                y = kin.headCenter.y + 0.022f * s
            )
            drawAnchorTargetRing(center = anchorShoulder, radius = 0.022f * s, color = vectorColor)

            // Upper trapezius / levator scapulae fascial band
            detailPath.reset()
            detailPath.moveTo(earAnchor.x, earAnchor.y)
            detailPath.quadraticTo(
                kin.neck.x + (if (keyframe.headTiltDeg >= 0f) -0.032f else 0.032f) * s,
                kin.neck.y + 0.015f * s,
                anchorShoulder.x,
                anchorShoulder.y
            )
            drawPath(
                path = detailPath,
                color = fascialColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Downward shoulder-melt anti-shrug vector
            drawGuideVectorArrow(
                start = Offset(anchorShoulder.x, anchorShoulder.y + 0.018f * s),
                end = Offset(anchorShoulder.x, anchorShoulder.y + 0.082f * s),
                color = vectorColor,
                s = s
            )
        }

        BodyRegion.THORACIC_CHEST in highlightedRegions -> {
            drawLine(
                color = fascialColor,
                start = kin.leftShoulder,
                end = kin.rightShoulder,
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawGuideVectorArrow(
                start = Offset(kin.chest.x - 0.04f * s, kin.chest.y - 0.03f * s),
                end = Offset(kin.leftShoulder.x - 0.035f * s, kin.leftShoulder.y),
                color = vectorColor,
                s = s
            )
            drawGuideVectorArrow(
                start = Offset(kin.chest.x + 0.04f * s, kin.chest.y - 0.03f * s),
                end = Offset(kin.rightShoulder.x + 0.035f * s, kin.rightShoulder.y),
                color = vectorColor,
                s = s
            )
        }

        BodyRegion.LOWER_LUMBAR in highlightedRegions -> {
            val activeSign = if (bilateralSide > 0) 1f else -1f
            val hipAnchor = Offset(kin.pelvis.x + activeSign * 0.11f * s, kin.pelvis.y)
            val ribTarget = Offset(kin.chest.x + activeSign * 0.12f * s, kin.chest.y - 0.06f * s)
            drawAnchorTargetRing(center = hipAnchor, radius = 0.022f * s, color = vectorColor)
            drawLine(
                color = fascialColor,
                start = hipAnchor,
                end = ribTarget,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawGuideVectorArrow(
                start = ribTarget,
                end = Offset(ribTarget.x - activeSign * 0.035f * s, ribTarget.y - 0.075f * s),
                color = vectorColor,
                s = s
            )
        }

        BodyRegion.MOUSE_WRIST in highlightedRegions -> {
            val activeArm = if (bilateralSide > 0) kin.rightArm else kin.leftArm
            drawAnchorTargetRing(center = activeArm.elbow, radius = 0.019f * s, color = axisColor)
            drawLine(
                color = fascialColor,
                start = activeArm.elbow,
                end = activeArm.hand,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawGuideVectorArrow(
                start = activeArm.hand,
                end = Offset(activeArm.hand.x, activeArm.hand.y - 0.062f * s),
                color = vectorColor,
                s = s
            )
        }

        BodyRegion.EYE_BROW in highlightedRegions -> {
            drawCircle(
                color = fascialColor,
                radius = 0.048f * s,
                center = kin.glabella,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                )
            )
            drawGuideVectorArrow(
                start = Offset(kin.glabella.x - 0.020f * s, kin.glabella.y),
                end = Offset(kin.glabella.x - 0.065f * s, kin.glabella.y),
                color = vectorColor,
                s = s
            )
            drawGuideVectorArrow(
                start = Offset(kin.glabella.x + 0.020f * s, kin.glabella.y),
                end = Offset(kin.glabella.x + 0.065f * s, kin.glabella.y),
                color = vectorColor,
                s = s
            )
        }
    }
}

private fun DrawScope.drawAnchorTargetRing(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
    )
    drawCircle(
        color = color,
        radius = (radius * 0.34f).coerceAtLeast(2f),
        center = center
    )
}

private fun DrawScope.drawGuideVectorArrow(
    start: Offset,
    end: Offset,
    color: Color,
    s: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLen = (0.024f * s).coerceAtLeast(4f)
    val wingSpread = 0.52f
    val leftWing = Offset(
        x = end.x - cos(angle - wingSpread) * headLen,
        y = end.y - sin(angle - wingSpread) * headLen
    )
    val rightWing = Offset(
        x = end.x - cos(angle + wingSpread) * headLen,
        y = end.y - sin(angle + wingSpread) * headLen
    )
    drawLine(color = color, start = end, end = leftWing, strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = end, end = rightWing, strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
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
    val contourW = (ill.contourStrokeWidthDp.finiteClamp(0.5f, 6f, 1.6f) * 0.86f).dp.toPx()

    // 1. Woven rush meditation mat ("蒲团素席") & subtle horizon line
    val matTopLeft = pt(0.24f, 0.778f)
    val matSize = Size(0.52f * s, 0.068f * s)
    drawOval(
        color = skin.palette.surfaceCardSubtle.copy(alpha = 0.78f),
        topLeft = matTopLeft,
        size = matSize
    )
    drawOval(
        color = skin.palette.borderHairline.copy(alpha = 0.85f),
        topLeft = matTopLeft,
        size = matSize,
        style = Stroke(width = 1.05.dp.toPx())
    )
    drawLine(
        color = skin.palette.accentSecondary.copy(alpha = 0.28f),
        start = pt(0.31f, 0.862f),
        end = pt(0.69f, 0.862f),
        strokeWidth = 0.9.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Moonlit rim-light aura for DUSK_PINE (MOONLIT_CONTOUR)
    buildTorsoTunicPath(tunicPath, kin, s)
    if (ill.figureStyle == FigureStyle.MOONLIT_CONTOUR) {
        drawPath(
            path = tunicPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.22f),
            style = Stroke(width = 3.8.dp.toPx(), join = StrokeJoin.Round)
        )
    }

    // 3. Posterior arms (occiput cradle / Fengchi press) behind hair & head
    if (kin.leftArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.leftArm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }
    if (kin.rightArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.rightArm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }

    // 4. Sculpted Song hair chignon ("宋式雅髻") & posterior hair silhouette
    val tiltDeg = keyframe.headTiltDeg.coerceIn(-30f, 30f)
    rotate(degrees = tiltDeg, pivot = kin.headCenter) {
        val bunCenter = Offset(kin.headCenter.x, kin.headCenter.y - 0.084f * s)
        // Cloud-shaped Song chignon
        drawOval(
            color = ill.hairFill,
            topLeft = Offset(bunCenter.x - 0.036f * s, bunCenter.y - 0.026f * s),
            size = Size(0.072f * s, 0.052f * s)
        )
        // Style-specific classical hair ornament ("玉簪 / 花钿")
        when (ill.figureStyle) {
            FigureStyle.EDITORIAL_MINERAL_FLAT -> {
                drawLine(
                    color = Color(0xFFB88655),
                    start = Offset(bunCenter.x - 0.048f * s, bunCenter.y + 0.005f * s),
                    end = Offset(bunCenter.x + 0.050f * s, bunCenter.y - 0.006f * s),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = skin.palette.accentPrimary,
                    radius = 2.2.dp.toPx(),
                    center = Offset(bunCenter.x + 0.050f * s, bunCenter.y - 0.006f * s)
                )
            }

            FigureStyle.BOTANICAL_GOUACHE -> {
                val ornament = Offset(bunCenter.x + 0.028f * s, bunCenter.y - 0.004f * s)
                drawCircle(color = Color(0xFFE6A598), radius = 3.0.dp.toPx(), center = ornament)
                drawCircle(color = skin.palette.accentPrimary, radius = 1.3.dp.toPx(), center = ornament)
            }

            FigureStyle.INK_BRUSH_WASH -> {
                // Wooden hairpin for Ink Mist scholar poise
                drawLine(
                    color = ill.garmentShadow,
                    start = Offset(bunCenter.x - 0.044f * s, bunCenter.y + 0.003f * s),
                    end = Offset(bunCenter.x + 0.044f * s, bunCenter.y - 0.004f * s),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            else -> Unit
        }

        val hairRx = 0.066f * s
        val hairRy = 0.076f * s
        drawOval(
            color = ill.hairFill,
            topLeft = Offset(kin.headCenter.x - hairRx, kin.headCenter.y - hairRy * 1.04f),
            size = Size(hairRx * 2f, hairRy * 1.86f)
        )
    }

    // 5. Slender anatomical neck column & soft V-clavicle skin base
    detailPath.reset()
    detailPath.moveTo(kin.headCenter.x - 0.020f * s, kin.headCenter.y + 0.045f * s)
    detailPath.lineTo(kin.headCenter.x + 0.020f * s, kin.headCenter.y + 0.045f * s)
    detailPath.lineTo(kin.neck.x + 0.028f * s, kin.neck.y + 0.036f * s)
    detailPath.lineTo(kin.chest.x, kin.neck.y + 0.076f * s)
    detailPath.lineTo(kin.neck.x - 0.028f * s, kin.neck.y + 0.036f * s)
    detailPath.close()
    drawPath(path = detailPath, color = ill.skinToneFill)

    // 6. Sculpted Song tunic ("交领右衽宽衫") & seated cross-legged skirt drapery ("交趺裳褶")
    if (ill.figureStyle == FigureStyle.INK_BRUSH_WASH) {
        drawPath(
            path = tunicPath,
            color = ill.contourStroke.copy(alpha = 0.14f),
            style = Stroke(width = 3.2.dp.toPx(), join = StrokeJoin.Round)
        )
    }
    drawPath(path = tunicPath, color = ill.garmentFill)

    // Cross-legged seated skirt folds ("裳裾叠褶")
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.176f * s, kin.pelvis.y - 0.012f * s)
    detailPath.quadraticTo(
        kin.pelvis.x - 0.02f * s,
        kin.pelvis.y + 0.042f * s,
        kin.pelvis.x + 0.176f * s,
        kin.pelvis.y - 0.012f * s
    )
    detailPath.lineTo(kin.pelvis.x + 0.182f * s, kin.pelvis.y + 0.022f * s)
    detailPath.quadraticTo(
        kin.pelvis.x,
        kin.pelvis.y + 0.058f * s,
        kin.pelvis.x - 0.182f * s,
        kin.pelvis.y + 0.022f * s
    )
    detailPath.close()
    drawPath(path = detailPath, color = ill.garmentShadow.copy(alpha = 0.80f))

    // Delicate interior knee-fold gossamer arcs
    detailPath.reset()
    detailPath.moveTo(kin.pelvis.x - 0.135f * s, kin.pelvis.y - 0.008f * s)
    detailPath.quadraticTo(kin.pelvis.x - 0.020f * s, kin.pelvis.y + 0.024f * s, kin.pelvis.x + 0.055f * s, kin.pelvis.y + 0.016f * s)
    detailPath.moveTo(kin.pelvis.x + 0.130f * s, kin.pelvis.y - 0.006f * s)
    detailPath.quadraticTo(kin.pelvis.x + 0.040f * s, kin.pelvis.y + 0.022f * s, kin.pelvis.x - 0.025f * s, kin.pelvis.y + 0.032f * s)
    drawPath(
        path = detailPath,
        color = ill.contourStroke.copy(alpha = 0.34f),
        style = Stroke(width = max(0.85.dp.toPx(), contourW * 0.54f), cap = StrokeCap.Round)
    )

    // Authentic double-layered Song Y-collar ("中衣素领 + 交领右衽") & high-waist sash ("素绦")
    val collarColor = when (ill.figureStyle) {
        FigureStyle.EDITORIAL_MINERAL_FLAT -> Color(0xFFB88655)
        FigureStyle.MOONLIT_CONTOUR -> skin.palette.accentSecondary
        FigureStyle.BOTANICAL_GOUACHE -> skin.palette.accentSecondary
        FigureStyle.INK_BRUSH_WASH -> ill.garmentShadow
    }
    val leftCollarPt = Offset(kin.neck.x - 0.028f * s, kin.neck.y + 0.016f * s)
    val rightCollarPt = Offset(kin.neck.x + 0.028f * s, kin.neck.y + 0.016f * s)
    val lapelCrossPt = Offset(kin.chest.x + 0.044f * s, kin.chest.y + 0.036f * s)
    val underCrossPt = Offset(kin.chest.x - 0.004f * s, kin.chest.y - 0.004f * s)

    // Inner ivory under-collar ("素色护领")
    detailPath.reset()
    detailPath.moveTo(leftCollarPt.x + 0.005f * s, leftCollarPt.y)
    detailPath.lineTo(underCrossPt.x, underCrossPt.y)
    detailPath.lineTo(rightCollarPt.x - 0.005f * s, rightCollarPt.y)
    drawPath(
        path = detailPath,
        color = skin.palette.surfaceCard.copy(alpha = 0.88f),
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Right-under lapel trim
    drawLine(
        color = collarColor.copy(alpha = 0.78f),
        start = rightCollarPt,
        end = underCrossPt,
        strokeWidth = 1.55.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Left-over-right primary lapel ("右衽")
    detailPath.reset()
    detailPath.moveTo(leftCollarPt.x, leftCollarPt.y)
    detailPath.quadraticTo(
        kin.chest.x - 0.002f * s,
        kin.chest.y + 0.002f * s,
        lapelCrossPt.x,
        lapelCrossPt.y
    )
    drawPath(
        path = detailPath,
        color = collarColor,
        style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
    )

    // Subtle Song waist sash & flowing ribbon tie ("素绦垂绅")
    val sashY = (kin.chest.y + kin.pelvis.y) * 0.48f
    detailPath.reset()
    detailPath.moveTo(kin.chest.x - 0.118f * s, sashY - 0.004f * s)
    detailPath.quadraticTo(kin.chest.x, sashY + 0.012f * s, kin.chest.x + 0.118f * s, sashY - 0.004f * s)
    drawPath(
        path = detailPath,
        color = collarColor.copy(alpha = 0.55f),
        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
    )
    // Tiny sash knot streamer on right flank
    detailPath.reset()
    detailPath.moveTo(kin.chest.x + 0.044f * s, sashY + 0.007f * s)
    detailPath.quadraticTo(kin.chest.x + 0.056f * s, sashY + 0.036f * s, kin.chest.x + 0.048f * s, sashY + 0.062f * s)
    drawPath(
        path = detailPath,
        color = collarColor.copy(alpha = 0.50f),
        style = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round)
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

    // 8. Calm rotated head, classical parted locks & willow-brow facial poise
    rotate(degrees = tiltDeg, pivot = kin.headCenter) {
        val faceRx = 0.056f * s
        val faceRy = 0.068f * s
        val cx = kin.headCenter.x
        val cy = kin.headCenter.y

        // Delicate earlobes
        val earR = 0.011f * s
        drawCircle(color = ill.skinToneFill, radius = earR, center = Offset(cx - faceRx * 0.98f, cy + 0.006f * s))
        drawCircle(color = ill.skinToneFill, radius = earR, center = Offset(cx + faceRx * 0.98f, cy + 0.006f * s))

        // Sculpted oval-chin classical face contour
        detailPath.reset()
        detailPath.moveTo(cx, cy - faceRy)
        detailPath.cubicTo(
            cx + faceRx * 0.72f, cy - faceRy,
            cx + faceRx * 1.02f, cy - faceRy * 0.42f,
            cx + faceRx * 0.96f, cy + faceRy * 0.14f
        )
        detailPath.cubicTo(
            cx + faceRx * 0.88f, cy + faceRy * 0.68f,
            cx + faceRx * 0.42f, cy + faceRy * 0.98f,
            cx, cy + faceRy
        )
        detailPath.cubicTo(
            cx - faceRx * 0.42f, cy + faceRy * 0.98f,
            cx - faceRx * 0.88f, cy + faceRy * 0.68f,
            cx - faceRx * 0.96f, cy + faceRy * 0.14f
        )
        detailPath.cubicTo(
            cx - faceRx * 1.02f, cy - faceRy * 0.42f,
            cx - faceRx * 0.72f, cy - faceRy,
            cx, cy - faceRy
        )
        detailPath.close()

        drawPath(path = detailPath, color = ill.skinToneFill)
        drawPath(
            path = detailPath,
            color = ill.contourStroke,
            style = Stroke(width = contourW * 0.88f, join = StrokeJoin.Round)
        )

        // Parted Song fringe & soft temple wisps ("云鬓")
        detailPath.reset()
        detailPath.moveTo(cx - faceRx * 0.98f, cy - faceRy * 0.12f)
        detailPath.quadraticTo(
            cx - faceRx * 0.42f,
            cy - faceRy * 0.88f,
            cx,
            cy - faceRy * 0.58f
        )
        detailPath.quadraticTo(
            cx + faceRx * 0.42f,
            cy - faceRy * 0.88f,
            cx + faceRx * 0.98f,
            cy - faceRy * 0.12f
        )
        detailPath.quadraticTo(
            cx + faceRx * 0.90f,
            cy - faceRy * 1.12f,
            cx,
            cy - faceRy * 1.14f
        )
        detailPath.quadraticTo(
            cx - faceRx * 0.90f,
            cy - faceRy * 1.12f,
            cx - faceRx * 0.98f,
            cy - faceRy * 0.12f
        )
        detailPath.close()
        drawPath(path = detailPath, color = ill.hairFill)

        // Botanical gouache warm cheek blushes
        if (ill.figureStyle == FigureStyle.BOTANICAL_GOUACHE) {
            val blushColor = Color(0xFFE6A598).copy(alpha = 0.34f)
            val blushR = 0.014f * s
            drawCircle(blushColor, blushR, Offset(cx - 0.028f * s, cy + 0.014f * s))
            drawCircle(blushColor, blushR, Offset(cx + 0.028f * s, cy + 0.014f * s))
        }

        // Tapered willow-leaf brows ("远山蛾眉") & serene eyes
        val featureColor = skin.palette.inkPrimary.copy(alpha = 0.78f)
        val eyeY = cy - 0.003f * s
        val eyeDx = 0.022f * s
        val eyeHalfW = 0.0115f * s
        val fineStroke = max(0.95.dp.toPx(), contourW * 0.62f)

        for (sign in intArrayOf(-1, 1)) {
            val bx = cx + sign * eyeDx
            detailPath.reset()
            detailPath.moveTo(bx - sign * eyeHalfW * 0.85f, eyeY - 0.017f * s)
            detailPath.quadraticTo(
                bx,
                eyeY - 0.023f * s,
                bx + sign * eyeHalfW * 1.08f,
                eyeY - 0.014f * s
            )
            drawPath(
                path = detailPath,
                color = featureColor.copy(alpha = 0.56f),
                style = Stroke(width = fineStroke * 0.78f, cap = StrokeCap.Round)
            )
        }

        if (keyframe.eyesClosed) {
            for (sign in intArrayOf(-1, 1)) {
                val ex = cx + sign * eyeDx
                detailPath.reset()
                detailPath.moveTo(ex - eyeHalfW, eyeY)
                detailPath.quadraticTo(ex, eyeY + 0.009f * s, ex + eyeHalfW, eyeY)
                drawPath(
                    path = detailPath,
                    color = featureColor,
                    style = Stroke(width = fineStroke * 0.90f, cap = StrokeCap.Round)
                )
            }
        } else {
            for (sign in intArrayOf(-1, 1)) {
                val ex = cx + sign * eyeDx
                drawCircle(
                    color = featureColor,
                    radius = max(1.45.dp.toPx(), 0.006f * s),
                    center = Offset(ex, eyeY + 0.002f * s)
                )
            }
        }

        // Subtle nasal tip hint + calm serene smile
        drawLine(
            color = featureColor.copy(alpha = 0.30f),
            start = Offset(cx, cy + 0.010f * s),
            end = Offset(cx, cy + 0.015f * s),
            strokeWidth = fineStroke * 0.65f,
            cap = StrokeCap.Round
        )
        val mouthY = cy + 0.028f * s
        detailPath.reset()
        detailPath.moveTo(cx - 0.010f * s, mouthY)
        detailPath.quadraticTo(cx, mouthY + 0.0065f * s, cx + 0.010f * s, mouthY)
        drawPath(
            path = detailPath,
            color = featureColor.copy(alpha = 0.65f),
            style = Stroke(width = fineStroke * 0.80f, cap = StrokeCap.Round)
        )
    }

    // 9. Anterior billowing wide-sleeves & lotus-petal hands
    if (!kin.leftArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.leftArm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }
    if (!kin.rightArm.isPosterior) {
        drawSleeveAndHand(skin = skin, arm = kin.rightArm, s = s, contourW = contourW, armPath = armPath, detailPath = detailPath)
    }

    // 10. Soft ink-wash Qi warmth highlights on target body regions
    drawHighlightedRegionWarmth(skin = skin, kin = kin, s = s, highlightedRegions = highlightedRegions)
}

private fun buildTorsoTunicPath(
    path: Path,
    kin: FigureKinematics,
    s: Float
) {
    val leftNeckCollar = Offset(kin.neck.x - 0.028f * s, kin.neck.y + 0.016f * s)
    val rightNeckCollar = Offset(kin.neck.x + 0.028f * s, kin.neck.y + 0.016f * s)
    val waistY = (kin.chest.y + kin.pelvis.y) * 0.50f
    val waistHalfW = 0.122f * s
    val hipHalfW = 0.184f * s
    val leftHip = Offset(kin.pelvis.x - hipHalfW, kin.pelvis.y + 0.020f * s)
    val rightHip = Offset(kin.pelvis.x + hipHalfW, kin.pelvis.y + 0.020f * s)

    path.reset()
    // Sloping left collarbone to shoulder
    path.moveTo(leftNeckCollar.x, leftNeckCollar.y)
    path.quadraticTo(
        (leftNeckCollar.x + kin.leftShoulder.x) * 0.52f,
        leftNeckCollar.y + 0.008f * s,
        kin.leftShoulder.x,
        kin.leftShoulder.y
    )
    // Left shoulder -> subtle tapered waist -> flared seated robe skirt ("裳裾")
    path.cubicTo(
        kin.chest.x - waistHalfW - 0.010f * s,
        (kin.leftShoulder.y + waistY) * 0.52f,
        kin.chest.x - waistHalfW,
        waistY,
        kin.pelvis.x - waistHalfW - 0.018f * s,
        (waistY + leftHip.y) * 0.52f
    )
    path.quadraticTo(
        leftHip.x - 0.006f * s,
        leftHip.y - 0.028f * s,
        leftHip.x,
        leftHip.y
    )
    // Seated lap hem curve
    path.quadraticTo(kin.pelvis.x, kin.pelvis.y + 0.056f * s, rightHip.x, rightHip.y)
    // Right skirt flare -> waist -> right shoulder
    path.quadraticTo(
        rightHip.x + 0.006f * s,
        rightHip.y - 0.028f * s,
        kin.pelvis.x + waistHalfW + 0.018f * s,
        (waistY + rightHip.y) * 0.52f
    )
    path.cubicTo(
        kin.chest.x + waistHalfW,
        waistY,
        kin.chest.x + waistHalfW + 0.010f * s,
        (kin.rightShoulder.y + waistY) * 0.52f,
        kin.rightShoulder.x,
        kin.rightShoulder.y
    )
    // Right shoulder up to right collarbone + V-neck dip
    path.quadraticTo(
        (rightNeckCollar.x + kin.rightShoulder.x) * 0.52f,
        rightNeckCollar.y + 0.008f * s,
        rightNeckCollar.x,
        rightNeckCollar.y
    )
    path.quadraticTo(
        kin.chest.x,
        kin.neck.y + 0.068f * s,
        leftNeckCollar.x,
        leftNeckCollar.y
    )
    path.close()
}

private fun DrawScope.drawSleeveAndHand(
    skin: SongxiSkin,
    arm: ArmJoints,
    s: Float,
    contourW: Float,
    armPath: Path,
    detailPath: Path
) {
    val ill = skin.illustration

    // Compute smooth unit tangents along shoulder -> elbow -> hand
    var dx1 = arm.elbow.x - arm.shoulder.x
    var dy1 = arm.elbow.y - arm.shoulder.y
    val len1 = hypot(dx1, dy1)
    if (len1 > 0.001f) {
        dx1 /= len1
        dy1 /= len1
    } else {
        dx1 = 0f
        dy1 = 1f
    }

    var dx2 = arm.hand.x - arm.elbow.x
    var dy2 = arm.hand.y - arm.elbow.y
    val len2 = hypot(dx2, dy2)
    if (len2 > 0.001f) {
        dx2 /= len2
        dy2 /= len2
    } else {
        dx2 = dx1
        dy2 = dy1
    }

    var mdx = dx1 + dx2
    var mdy = dy1 + dy2
    val mlen = hypot(mdx, mdy)
    if (mlen > 0.001f) {
        mdx /= mlen
        mdy /= mlen
    } else {
        mdx = dx2
        mdy = dy2
    }

    // Consistent topological normal (-ty, tx) multiplied by ONE uniform sign across the whole arm -> 0% twist!
    val baseMidNx = -mdy
    val baseMidNy = mdx
    val drapeSign = if (baseMidNy > 0.12f) {
        1f
    } else if (baseMidNy < -0.12f) {
        -1f
    } else {
        // Nearly vertical arm: billowing sleeve drapes outward away from body midline
        val isLeftSide = arm.shoulder.x <= size.width * 0.5f
        if ((isLeftSide && baseMidNx < 0f) || (!isLeftSide && baseMidNx > 0f)) 1f else -1f
    }

    val nSh = Offset(-dy1 * drapeSign, dx1 * drapeSign)
    val nEl = Offset(-mdy * drapeSign, mdx * drapeSign)
    val nHa = Offset(-dy2 * drapeSign, dx2 * drapeSign)

    val shUpper = 0.022f * s
    val shLower = 0.024f * s
    val elUpper = 0.021f * s
    val elLower = 0.036f * s // Graceful billowing Song wide-sleeve drape at elbow
    val haUpper = 0.016f * s
    val haLower = 0.025f * s

    // Pull cuff slightly back from palm center so delicate wrist & lotus-hand emerge cleanly
    val cuffCenter = Offset(
        x = arm.hand.x - dx2 * 0.013f * s,
        y = arm.hand.y - dy2 * 0.013f * s
    )

    val shTop = Offset(arm.shoulder.x - nSh.x * shUpper, arm.shoulder.y - nSh.y * shUpper)
    val shBot = Offset(arm.shoulder.x + nSh.x * shLower, arm.shoulder.y + nSh.y * shLower)
    val elTop = Offset(arm.elbow.x - nEl.x * elUpper, arm.elbow.y - nEl.y * elUpper)
    val elBot = Offset(arm.elbow.x + nEl.x * elLower, arm.elbow.y + nEl.y * elLower)
    val cuTop = Offset(cuffCenter.x - nHa.x * haUpper, cuffCenter.y - nHa.y * haUpper)
    val cuBot = Offset(cuffCenter.x + nHa.x * haLower, cuffCenter.y + nHa.y * haLower)

    // 1. Delicate tapered lotus-petal hand emerging from inside sleeve cuff
    val forearmDeg = Math.toDegrees(atan2(dy2.toDouble(), dx2.toDouble())).toFloat()
    rotate(degrees = forearmDeg + arm.wristAngleDeg * 0.30f, pivot = arm.hand) {
        val hx = arm.hand.x
        val hy = arm.hand.y
        detailPath.reset()
        detailPath.moveTo(hx - 0.015f * s, hy - 0.008f * s)
        detailPath.quadraticTo(hx - 0.002f * s, hy - 0.014f * s, hx + 0.010f * s, hy - 0.009f * s)
        detailPath.quadraticTo(hx + 0.025f * s, hy - 0.005f * s, hx + 0.028f * s, hy + 0.001f * s)
        detailPath.quadraticTo(hx + 0.024f * s, hy + 0.010f * s, hx + 0.004f * s, hy + 0.011f * s)
        detailPath.quadraticTo(hx - 0.008f * s, hy + 0.010f * s, hx - 0.015f * s, hy + 0.007f * s)
        detailPath.close()

        drawPath(path = detailPath, color = ill.skinToneFill)
        drawPath(
            path = detailPath,
            color = ill.contourStroke,
            style = Stroke(width = contourW * 0.76f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // 2. Closed wide-sleeve fill polygon (merges seamlessly into Song 连肩通袖 shoulder)
    armPath.reset()
    armPath.moveTo(shTop.x, shTop.y)
    armPath.quadraticTo(elTop.x, elTop.y, cuTop.x, cuTop.y)
    armPath.quadraticTo(
        cuffCenter.x + dx2 * 0.007f * s,
        cuffCenter.y + dy2 * 0.007f * s,
        cuBot.x,
        cuBot.y
    )
    armPath.quadraticTo(elBot.x, elBot.y, shBot.x, shBot.y)
    armPath.quadraticTo(
        arm.shoulder.x - dx1 * 0.018f * s,
        arm.shoulder.y - dy1 * 0.018f * s,
        shTop.x,
        shTop.y
    )
    armPath.close()

    if (ill.figureStyle == FigureStyle.MOONLIT_CONTOUR) {
        drawPath(
            path = armPath,
            color = skin.palette.accentPrimary.copy(alpha = 0.20f),
            style = Stroke(width = 3.4.dp.toPx(), join = StrokeJoin.Round)
        )
    }

    drawPath(path = armPath, color = ill.garmentFill)

    // Subtle lower sleeve drape shadow + interior gossamer fold line ("衣纹游丝")
    detailPath.reset()
    detailPath.moveTo(
        arm.elbow.x + nEl.x * (elLower * 0.34f),
        arm.elbow.y + nEl.y * (elLower * 0.34f)
    )
    detailPath.quadraticTo(
        (arm.elbow.x + cuffCenter.x) * 0.5f + nEl.x * (elLower * 0.62f),
        (arm.elbow.y + cuffCenter.y) * 0.5f + nEl.y * (elLower * 0.62f),
        cuBot.x - dx2 * 0.005f * s,
        cuBot.y - dy2 * 0.005f * s
    )
    drawPath(
        path = detailPath,
        color = ill.garmentShadow.copy(alpha = 0.75f),
        style = Stroke(width = max(0.9.dp.toPx(), contourW * 0.56f), cap = StrokeCap.Round)
    )

    // Curved cuff border ribbon ("袖缘")
    val cuffTrimColor = when (ill.figureStyle) {
        FigureStyle.EDITORIAL_MINERAL_FLAT -> Color(0xFFB88655).copy(alpha = 0.82f)
        FigureStyle.MOONLIT_CONTOUR -> skin.palette.accentSecondary.copy(alpha = 0.72f)
        FigureStyle.BOTANICAL_GOUACHE -> skin.palette.accentSecondary.copy(alpha = 0.72f)
        FigureStyle.INK_BRUSH_WASH -> ill.garmentShadow.copy(alpha = 0.85f)
    }
    detailPath.reset()
    detailPath.moveTo(cuTop.x - dx2 * 0.003f * s, cuTop.y - dy2 * 0.003f * s)
    detailPath.quadraticTo(
        cuffCenter.x + dx2 * 0.004f * s,
        cuffCenter.y + dy2 * 0.004f * s,
        cuBot.x - dx2 * 0.003f * s,
        cuBot.y - dy2 * 0.003f * s
    )
    drawPath(
        path = detailPath,
        color = cuffTrimColor,
        style = Stroke(width = max(1.05.dp.toPx(), contourW * 0.72f), cap = StrokeCap.Round)
    )

    // Open-shoulder 连肩通袖 outer contour: strokes from shoulder-top -> cuff -> elbow-drape -> armpit without cutting across deltoid!
    detailPath.reset()
    detailPath.moveTo(shTop.x, shTop.y)
    detailPath.quadraticTo(elTop.x, elTop.y, cuTop.x, cuTop.y)
    detailPath.quadraticTo(
        cuffCenter.x + dx2 * 0.007f * s,
        cuffCenter.y + dy2 * 0.007f * s,
        cuBot.x,
        cuBot.y
    )
    detailPath.quadraticTo(
        elBot.x,
        elBot.y,
        Offset(
            x = arm.shoulder.x + dx1 * 0.022f * s + nSh.x * (shLower * 0.88f),
            y = arm.shoulder.y + dy1 * 0.022f * s + nSh.y * (shLower * 0.88f)
        ).x,
        Offset(
            x = arm.shoulder.x + dx1 * 0.022f * s + nSh.x * (shLower * 0.88f),
            y = arm.shoulder.y + dy1 * 0.022f * s + nSh.y * (shLower * 0.88f)
        ).y
    )
    drawPath(
        path = detailPath,
        color = ill.contourStroke,
        style = Stroke(width = contourW * 0.90f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
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
    val ankleStart = Offset(kin.pelvis.x + sign * 0.145f * s, kin.pelvis.y + 0.052f * s)
    val kneeTarget = Offset(kin.pelvis.x - sign * 0.065f * s, kin.pelvis.y + 0.012f * s)
    drawLine(
        color = skin.palette.accentPrimary.copy(alpha = 0.50f),
        start = ankleStart,
        end = kneeTarget,
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Active-hand meridian emphasis ring
    val activeHand = if (bilateralSide < 0) kin.leftArm.hand else kin.rightArm.hand
    drawCircle(
        color = skin.palette.accentPrimary.copy(alpha = 0.44f),
        radius = 0.036f * s,
        center = activeHand,
        style = Stroke(width = 1.15.dp.toPx())
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
        color = skin.palette.accentPrimary.copy(alpha = 0.42f),
        style = Stroke(
            width = 1.35.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 6f), 0f)
        )
    )
}

/**
 * Multi-layer Xuan ink-wash Qi warmth diffusion ("气韵水墨温灸晕染"):
 * replaces hard red button-dots with a luminous mist wash and gossamer crescent.
 */
private fun DrawScope.drawSoftQiWarmthAura(
    skin: SongxiSkin,
    pt: Offset,
    s: Float
) {
    val outerRadius = (0.072f * s).coerceAtLeast(6f)
    val innerRadius = (0.036f * s).coerceAtLeast(3.5f)

    drawCircle(
        brush = Brush.radialGradient(
            0.0f to skin.palette.accentPrimary.copy(alpha = 0.22f),
            0.55f to skin.palette.accentPrimary.copy(alpha = 0.07f),
            1.0f to Color.Transparent,
            center = pt,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = pt
    )
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to skin.palette.sealVermilion.copy(alpha = 0.22f),
            0.65f to skin.palette.sealVermilion.copy(alpha = 0.05f),
            1.0f to Color.Transparent,
            center = pt,
            radius = innerRadius
        ),
        radius = innerRadius,
        center = pt
    )
    val ringR = (0.019f * s).coerceAtLeast(2.2.dp.toPx())
    drawArc(
        color = skin.palette.sealVermilion.copy(alpha = 0.34f),
        startAngle = -45f,
        sweepAngle = 235f,
        useCenter = false,
        topLeft = Offset(pt.x - ringR, pt.y - ringR),
        size = Size(ringR * 2f, ringR * 2f),
        style = Stroke(width = 0.85.dp.toPx(), cap = StrokeCap.Round)
    )
    drawCircle(
        color = skin.palette.sealVermilion.copy(alpha = 0.40f),
        radius = 1.15.dp.toPx(),
        center = pt
    )
}

private fun DrawScope.drawHighlightedRegionWarmth(
    skin: SongxiSkin,
    kin: FigureKinematics,
    s: Float,
    highlightedRegions: Set<BodyRegion>
) {
    if (highlightedRegions.isEmpty()) return

    for (region in highlightedRegions) {
        when (region) {
            BodyRegion.EYE_BROW -> {
                drawSoftQiWarmthAura(skin, kin.glabella, s)
                // Bilateral Cuanzhu (攒竹) inner-brow acupoint pearls + orbital mist crescent
                val cuanzhuOffset = 0.015f * s
                val acupointRadius = max(1.2.dp.toPx(), 0.0045f * s)
                drawCircle(
                    color = skin.palette.sealVermilion.copy(alpha = 0.56f),
                    radius = acupointRadius,
                    center = Offset(kin.glabella.x - cuanzhuOffset, kin.glabella.y - 0.004f * s)
                )
                drawCircle(
                    color = skin.palette.sealVermilion.copy(alpha = 0.56f),
                    radius = acupointRadius,
                    center = Offset(kin.glabella.x + cuanzhuOffset, kin.glabella.y - 0.004f * s)
                )
                drawArc(
                    color = skin.palette.accentPrimary.copy(alpha = 0.38f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(kin.glabella.x - 0.048f * s, kin.glabella.y - 0.032f * s),
                    size = Size(0.096f * s, 0.052f * s),
                    style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            BodyRegion.POSTERIOR_NECK -> drawSoftQiWarmthAura(skin, kin.neck, s)
            BodyRegion.TRAPEZIUS_SHOULDER -> {
                drawSoftQiWarmthAura(skin, kin.leftShoulder, s)
                drawSoftQiWarmthAura(skin, kin.rightShoulder, s)
            }
            BodyRegion.THORACIC_CHEST -> drawSoftQiWarmthAura(skin, kin.chest, s)
            BodyRegion.LOWER_LUMBAR -> drawSoftQiWarmthAura(skin, kin.pelvis, s)
            BodyRegion.MOUSE_WRIST -> {
                drawSoftQiWarmthAura(skin, kin.leftArm.hand, s)
                drawSoftQiWarmthAura(skin, kin.rightArm.hand, s)
                // Articulated finger brush strokes + forearm median-nerve glide hairlines
                for (arm in listOf(kin.leftArm, kin.rightArm)) {
                    drawLine(
                        color = skin.palette.accentPrimary.copy(alpha = 0.38f),
                        start = arm.elbow,
                        end = arm.hand,
                        strokeWidth = 1.05.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                    rotate(degrees = arm.wristAngleDeg, pivot = arm.hand) {
                        val fingerColor = skin.illustration.contourStroke.copy(alpha = 0.68f)
                        val fingerW = 0.95.dp.toPx()
                        val palmTipX = arm.hand.x + 0.022f * s
                        for (fIdx in -1..1) {
                            val fy = arm.hand.y + fIdx * 0.006f * s
                            drawLine(
                                color = fingerColor,
                                start = Offset(palmTipX, fy),
                                end = Offset(palmTipX + 0.013f * s, fy + fIdx * 0.003f * s),
                                strokeWidth = fingerW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
