package com.songxi.app.core.model

import kotlinx.serialization.Serializable

/**
 * Parametric upper-body posture angles/ratios interpolated across 4s inhale <-> 6s exhale:
 * - [headTiltDeg]: [-28f..+28f] lateral neck tilt (<0 left ear, >0 right ear)
 * - [chinTuckRatio]: [-1f..+1f] sagittal head glide (>0 axial chin tuck, <0 gentle upward cradle)
 * - [leftShoulderDropRatio] / [rightShoulderDropRatio]: [0f..1f] (0f lifted, 0.5f neutral, 1f melted down)
 * - [spineArcRatio]: [-1f..+1f] (>0 open chest/extension, <0 rounded cat-back/gentle forward fold)
 * - [leftArmAngleDeg] / [rightArmAngleDeg]: [0f..165f] arm elevation from torso side
 * - [wristFlexRatio]: [-1f..+1f] (>0 palm-out extension, <0 gentle flexion/ulnar release)
 */
@Serializable
data class PoseKeyframe(
    val headTiltDeg: Float = 0f,
    val chinTuckRatio: Float = 0f,
    val leftShoulderDropRatio: Float = 0.5f,
    val rightShoulderDropRatio: Float = 0.5f,
    val spineArcRatio: Float = 0f,
    val leftArmAngleDeg: Float = 15f,
    val rightArmAngleDeg: Float = 15f,
    val wristFlexRatio: Float = 0f,
    val eyesClosed: Boolean = true
) {
    fun lerp(target: PoseKeyframe, fraction: Float): PoseKeyframe {
        val t = fraction.coerceIn(0f, 1f)
        fun mix(a: Float, b: Float): Float = a + (b - a) * t
        return PoseKeyframe(
            headTiltDeg = mix(headTiltDeg, target.headTiltDeg),
            chinTuckRatio = mix(chinTuckRatio, target.chinTuckRatio),
            leftShoulderDropRatio = mix(leftShoulderDropRatio, target.leftShoulderDropRatio),
            rightShoulderDropRatio = mix(rightShoulderDropRatio, target.rightShoulderDropRatio),
            spineArcRatio = mix(spineArcRatio, target.spineArcRatio),
            leftArmAngleDeg = mix(leftArmAngleDeg, target.leftArmAngleDeg),
            rightArmAngleDeg = mix(rightArmAngleDeg, target.rightArmAngleDeg),
            wristFlexRatio = mix(wristFlexRatio, target.wristFlexRatio),
            eyesClosed = if (t < 0.5f) eyesClosed else target.eyesClosed
        )
    }

    fun mirroredBilaterally(): PoseKeyframe = copy(
        headTiltDeg = -headTiltDeg,
        leftShoulderDropRatio = rightShoulderDropRatio,
        rightShoulderDropRatio = leftShoulderDropRatio,
        leftArmAngleDeg = rightArmAngleDeg,
        rightArmAngleDeg = leftArmAngleDeg
    )
}

@Serializable
data class StretchPose(
    val id: String,
    val title: String,
    val gentleCue: String,
    val targetRegions: Set<BodyRegion>,
    val durationSeconds: Int = 45,
    val inhaleKeyframe: PoseKeyframe,
    val exhaleKeyframe: PoseKeyframe,
    val bilateralSwitchAtHalfway: Boolean = false
) {
    init {
        require(id.isNotBlank()) { "StretchPose.id must not be blank" }
        require(title.isNotBlank()) { "StretchPose.title must not be blank" }
        require(gentleCue.isNotBlank() && gentleCue.length <= MAX_GENTLE_CUE_CHARS) {
            "StretchPose.gentleCue for '$id' must be 1..$MAX_GENTLE_CUE_CHARS chars, got ${gentleCue.length}"
        }
        require(targetRegions.isNotEmpty()) { "StretchPose.targetRegions for '$id' must not be empty" }
        require(durationSeconds > 0) { "StretchPose.durationSeconds for '$id' must be > 0" }
    }

    companion object {
        const val MAX_GENTLE_CUE_CHARS: Int = 36
    }
}

@Serializable
data class StretchRoutine(
    val id: String,
    val title: String,
    val subtitle: String,
    val scene: PostureScene,
    val eyesFreeCapable: Boolean = true,
    val seatedDiscreet: Boolean = true,
    val poses: List<StretchPose>
) {
    init {
        require(id.isNotBlank()) { "StretchRoutine.id must not be blank" }
        require(poses.isNotEmpty()) { "StretchRoutine.poses for '$id' must not be empty" }
    }

    val totalDurationSeconds: Int
        get() = poses.sumOf { it.durationSeconds }
}

@Serializable
data class BookletSeries(
    val id: String,
    val title: String,
    val subtitle: String,
    val totalDays: Int,
    val dailyMinutes: Int,
    val routines: List<StretchRoutine>
) {
    init {
        require(totalDays > 0) { "BookletSeries.totalDays must be > 0" }
        require(dailyMinutes > 0) { "BookletSeries.dailyMinutes must be > 0" }
        require(routines.isNotEmpty()) { "BookletSeries.routines must not be empty" }
    }

    fun routineForDay(dayIndex: Int): StretchRoutine =
        routines[dayIndex.coerceIn(0, routines.lastIndex)]
}
