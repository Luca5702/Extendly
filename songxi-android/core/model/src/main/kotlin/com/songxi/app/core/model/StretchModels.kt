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

/**
 * Evidence-informed physical-therapy & ergonomic metadata attached to every [StretchPose].
 * Keeps main player calm & uncluttered while providing deep anatomical guidance on demand.
 */
@Serializable
data class PoseClinicalMetadata(
    val modality: StretchModality = StretchModality.STATIC_FASCIAL,
    val phaseRole: PrescriptionPhaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
    val viewPlane: AnatomicalViewPlane = AnatomicalViewPlane.CORONAL_FRONT,
    val primaryMusclesCn: String = "斜方肌上束 · 颈后深层肌群",
    val targetSensationCn: String = "目标肌腹呈温热微胀感（强度约 3–4/10），呼吸始终深长顺畅。",
    val antiCompensationCn: String = "坐骨稳落椅面，双肩自然远离耳垂，切勿耸肩或憋气硬拉。",
    val redFlagWarningCn: String = "若出现放射性手麻、刺痛或眩晕，请立即减小幅度并缓慢回正。",
    val setupCueCn: String = "坐骨稳落椅面，百会轻提摆正脊柱基线。",
    val safeReturnCueCn: String = "随吸气缓慢承托回正，切勿猛然弹回。"
) {
    init {
        require(setupCueCn.isNotBlank() && setupCueCn.length <= StretchPose.MAX_GENTLE_CUE_CHARS) {
            "setupCueCn must be 1..${StretchPose.MAX_GENTLE_CUE_CHARS} chars"
        }
        require(safeReturnCueCn.isNotBlank() && safeReturnCueCn.length <= StretchPose.MAX_GENTLE_CUE_CHARS) {
            "safeReturnCueCn must be 1..${StretchPose.MAX_GENTLE_CUE_CHARS} chars"
        }
    }

    companion object {
        fun defaultFor(regions: Set<BodyRegion>): PoseClinicalMetadata {
            val lead = regions.firstOrNull() ?: BodyRegion.POSTERIOR_NECK
            return when (lead) {
                BodyRegion.POSTERIOR_NECK -> PoseClinicalMetadata(
                    modality = StretchModality.MICRO_PNF,
                    phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
                    viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
                    primaryMusclesCn = "枕下肌群 · 头半棘肌 · 项韧带"
                )
                BodyRegion.TRAPEZIUS_SHOULDER -> PoseClinicalMetadata(
                    modality = StretchModality.STATIC_FASCIAL,
                    phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
                    viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
                    primaryMusclesCn = "上斜方肌 · 肩胛提肌 · 菱形肌"
                )
                BodyRegion.THORACIC_CHEST -> PoseClinicalMetadata(
                    modality = StretchModality.MOBILITY_CARS,
                    phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
                    viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
                    primaryMusclesCn = "胸小肌 · 胸椎竖脊肌 · 肋间肌"
                )
                BodyRegion.LOWER_LUMBAR -> PoseClinicalMetadata(
                    modality = StretchModality.MOBILITY_CARS,
                    phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
                    viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
                    primaryMusclesCn = "腰方肌 · 多裂肌 · 梨状肌深筋膜"
                )
                BodyRegion.MOUSE_WRIST -> PoseClinicalMetadata(
                    modality = StretchModality.NERVE_GLIDING,
                    phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
                    viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
                    primaryMusclesCn = "指浅屈肌 · 旋前圆肌 · 正中神经滑道"
                )
                BodyRegion.EYE_BROW -> PoseClinicalMetadata(
                    modality = StretchModality.OCULOMOTOR_RELEASE,
                    phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
                    viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
                    primaryMusclesCn = "眼轮匝肌 · 睫状肌 · 眼颈反射枕下束"
                )
            }
        }
    }
}

/**
 * Resolved real-time coaching snapshot along the 4-stage intra-pose timeline.
 */
data class PoseTimelineGuidance(
    val stage: PoseTimelineStage,
    val activeCueCn: String,
    val isSecondSegment: Boolean,
    val activeBilateralSide: Int,
    val switchPointMs: Long
)

@Serializable
data class StretchPose(
    val id: String,
    val title: String,
    val gentleCue: String,
    val targetRegions: Set<BodyRegion>,
    val durationSeconds: Int = 45,
    val inhaleKeyframe: PoseKeyframe,
    val exhaleKeyframe: PoseKeyframe,
    val bilateralSwitchAtHalfway: Boolean = false,
    val clinical: PoseClinicalMetadata = PoseClinicalMetadata.defaultFor(targetRegions)
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

    /**
     * Evaluates the 4-stage intra-pose coaching timeline & asymmetric bilateral side allocation:
     * - [LateralAsymmetry.BALANCED]: 50% left (-1) -> 50% right (+1)
     * - [LateralAsymmetry.LEFT_TIGHTER]: 60% tighter left (-1) -> 40% right (+1)
     * - [LateralAsymmetry.RIGHT_TIGHTER]: 60% tighter right (+1) -> 40% left (-1)
     */
    fun resolveTimelineGuidance(
        elapsedMs: Long,
        asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED
    ): PoseTimelineGuidance {
        val totalMs = (durationSeconds.coerceAtLeast(1)) * 1_000L
        val clampedMs = elapsedMs.coerceIn(0L, totalMs)
        val setupWindowMs = minOf(5_000L, totalMs / 6L).coerceAtLeast(1_500L)
        val returnWindowMs = minOf(5_000L, totalMs / 6L).coerceAtLeast(1_500L)

        val switchMs = if (bilateralSwitchAtHalfway) {
            (totalMs * asymmetry.firstHalfRatio).toLong().coerceIn(1_000L, totalMs - 1_000L)
        } else {
            totalMs / 2L
        }
        val isSecondSegment = bilateralSwitchAtHalfway && clampedMs >= switchMs
        val activeSide = when {
            !bilateralSwitchAtHalfway -> 0
            !isSecondSegment -> asymmetry.primarySideSign
            else -> -asymmetry.primarySideSign
        }

        val stage = when {
            clampedMs < setupWindowMs -> PoseTimelineStage.SETUP
            clampedMs >= (totalMs - returnWindowMs) -> PoseTimelineStage.SAFE_RETURN
            bilateralSwitchAtHalfway && clampedMs in (switchMs - 2_000L)..(switchMs + 1_500L) ->
                PoseTimelineStage.HALFWAY_TRANSITION
            else -> PoseTimelineStage.DEEPEN_HOLD
        }

        val cue = when (stage) {
            PoseTimelineStage.SETUP -> clinical.setupCueCn
            PoseTimelineStage.HALFWAY_TRANSITION -> "随吸气先缓慢回正中立，呼气再柔和换向另一侧。"
            PoseTimelineStage.SAFE_RETURN -> clinical.safeReturnCueCn
            PoseTimelineStage.DEEPEN_HOLD -> gentleCue
        }

        return PoseTimelineGuidance(
            stage = stage,
            activeCueCn = cue,
            isSecondSegment = isSecondSegment,
            activeBilateralSide = activeSide,
            switchPointMs = switchMs
        )
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
    val poses: List<StretchPose>,
    val asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED
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

/**
 * User-saved custom BodyMap prescription ("我的常备方") for 1-tap quick launch on Home & BodyMap.
 */
@Serializable
data class SavedPrescriptionPreset(
    val id: String,
    val title: String,
    val regions: Set<BodyRegion>,
    val durationMinutes: Int,
    val asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED,
    val createdAtEpochMs: Long = 0L
) {
    init {
        require(id.isNotBlank()) { "SavedPrescriptionPreset.id must not be blank" }
        require(regions.isNotEmpty()) { "SavedPrescriptionPreset.regions must not be empty" }
        require(durationMinutes in 1..15) { "SavedPrescriptionPreset.durationMinutes must be in 1..15" }
    }

    companion object {
        const val MAX_SAVED_PRESETS: Int = 4

        fun buildPresetId(
            regions: Set<BodyRegion>,
            durationMinutes: Int,
            asymmetry: LateralAsymmetry
        ): String {
            val ordered = BodyRegion.entries.filter { it in regions }.ifEmpty {
                listOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER)
            }
            val regionSlug = ordered.joinToString("_") { it.key }
            return "preset_${regionSlug}_${durationMinutes}m_${asymmetry.key}"
        }

        fun buildDisplayTitle(
            regions: Set<BodyRegion>,
            durationMinutes: Int,
            asymmetry: LateralAsymmetry
        ): String {
            val ordered = BodyRegion.entries.filter { it in regions }.ifEmpty {
                listOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER)
            }
            val regionText = ordered.joinToString("·") { it.shortLabelCn }
            val asymSuffix = if (asymmetry == LateralAsymmetry.BALANCED) "" else " (${asymmetry.ratioBadgeCn})"
            return "$regionText · ${durationMinutes}分钟$asymSuffix"
        }
    }
}

