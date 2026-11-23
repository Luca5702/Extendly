package com.songxi.app.core.data

import com.songxi.app.core.model.AnatomicalViewPlane
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.PoseClinicalMetadata
import com.songxi.app.core.model.PoseKeyframe
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.PrescriptionPhaseRole
import com.songxi.app.core.model.StretchModality
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine

internal object SongxiStretchCatalog {

    private fun kf(
        tilt: Float,
        chin: Float,
        lDrop: Float,
        rDrop: Float,
        spine: Float,
        lArm: Float,
        rArm: Float,
        wrist: Float,
        closed: Boolean
    ): PoseKeyframe = PoseKeyframe(
        headTiltDeg = tilt,
        chinTuckRatio = chin,
        leftShoulderDropRatio = lDrop,
        rightShoulderDropRatio = rDrop,
        spineArcRatio = spine,
        leftArmAngleDeg = lArm,
        rightArmAngleDeg = rArm,
        wristFlexRatio = wrist,
        eyesClosed = closed
    )

    // --- 1..4: POSTERIOR_NECK (后颈风池) ---
    val neckChinTuck = StretchPose(
        id = "neck_chin_tuck",
        title = "收颌微点头",
        gentleCue = "视线平视，下颌水平微收半寸，随呼气让紧绷的后颈柔和变长。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.15f, 0.45f, 0.45f, 0.15f, 14f, 14f, 0f, false),
        exhaleKeyframe = kf(0f, 0.85f, 0.85f, 0.85f, 0.25f, 12f, 12f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MICRO_PNF,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "颈深屈肌（激活） · 枕下肌群 · 头半棘肌",
            targetSensationCn = "颅底发际处呈温和纵向牵开感（约 3/10），咽喉松弛不紧绷。",
            antiCompensationCn = "视线始终平视正前，水平后移下颌而非低头压迫喉部。",
            redFlagWarningCn = "若向手臂放射麻木、电击感或头晕目眩，请立即停止并回正。",
            setupCueCn = "坐骨稳落椅面，目光平视前方松开牙关。",
            safeReturnCueCn = "随吸气松开收颌张力，让头颈轻盈归于中立。"
        )
    )

    val neckOcciputCradle = StretchPose(
        id = "neck_occiput_cradle",
        title = "双手相扣颈后温托",
        gentleCue = "掌心轻托后颈风池，下颌微扬半寸，借头部自重缓缓靠入温热掌心。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.35f, 0.50f, 0.50f, 0.45f, 118f, 118f, 0f, false),
        exhaleKeyframe = kf(0f, -0.65f, 0.82f, 0.82f, 0.30f, 112f, 112f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MICRO_PNF,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "头后大直肌 · 斜方肌上束 · 胸锁乳突肌浅筋膜",
            targetSensationCn = "后颈被掌心稳稳承托，前颈与胸口轻柔舒展（约 3/10）。",
            antiCompensationCn = "以掌根托住枕骨下缘而非推压颈椎棘突，腰肋保持内收不塌腰。",
            redFlagWarningCn = "避免大幅后仰挤压下颈椎；若感脑后胀晕请减小仰角。",
            setupCueCn = "十指轻扣托于脑后枕骨缘，双肩先自然沉落。",
            safeReturnCueCn = "借双掌轻轻承托头部先行回正，再缓放双臂。"
        )
    )

    val neckFengchiNod = StretchPose(
        id = "neck_fengchi_nod",
        title = "左右风池微倾松解",
        gentleCue = "摘下耳机松开牙关，头向一侧微倾停留，在绵长呼气里等颈侧变软。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 45,
        inhaleKeyframe = kf(8f, 0.20f, 0.55f, 0.55f, 0.10f, 15f, 15f, 0f, true),
        exhaleKeyframe = kf(22f, 0.35f, 0.95f, 0.75f, 0.05f, 10f, 12f, 0f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "中前斜角肌 · 肩胛提肌 · 风池枕下筋膜",
            targetSensationCn = "对侧耳后至锁骨上方呈绵密温热延展感（约 3–4/10）。",
            antiCompensationCn = "对侧肩头自然下沉作锚点，切勿耸肩迎头或用手强行扳压。",
            redFlagWarningCn = "若锁骨窝或手指出现麻刺感，请立即减小倾角。",
            setupCueCn = "摘下耳机松开下颌，双肩自然垂落远离耳垂。",
            safeReturnCueCn = "先收下颌微蓄力，随吸气将头颅缓缓扶正。"
        )
    )

    val neckThumbFengchiPress = StretchPose(
        id = "neck_thumb_fengchi_press",
        title = "指腹轻倚风池温养",
        gentleCue = "双肘轻支桌面，拇指落于脑后发际凹处，随长呼气卸下低头整日的重量。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.EYE_BROW),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.15f, 0.50f, 0.50f, 0.05f, 98f, 98f, 0f, true),
        exhaleKeyframe = kf(0f, -0.45f, 0.85f, 0.85f, 0.15f, 94f, 94f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.OCULOMOTOR_RELEASE,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "头夹肌 · 枕下三角肌群 · 枕大神经浅层滑道",
            targetSensationCn = "风池凹陷处酸温散开，连带眼底紧涩随之轻化（约 3/10）。",
            antiCompensationCn = "以头部微倾自重轻倚指腹，手指不主动大力抠按。",
            redFlagWarningCn = "避开颈侧搏动血管区；若有搏动性酸胀请轻移指位。",
            setupCueCn = "双肘稳支桌面，拇指轻落脑后发际风池凹处。",
            safeReturnCueCn = "先松开指腹轻托力，随吸气抬头平视前方。"
        )
    )

    // --- 5..8: TRAPEZIUS_SHOULDER (双肩斜方) ---
    val shoulderEarMelt = StretchPose(
        id = "shoulder_ear_melt",
        title = "坐姿侧耳寻肩",
        gentleCue = "左手轻搭左腿，不必用力掰头，像轻叹一口气，让肩膀慢慢沉向地面。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.POSTERIOR_NECK),
        durationSeconds = 45,
        inhaleKeyframe = kf(10f, 0.10f, 0.60f, 0.55f, 0.10f, 16f, 16f, 0f, true),
        exhaleKeyframe = kf(24f, 0.20f, 1.00f, 0.80f, 0.05f, 8f, 12f, 0f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "上斜方肌横束 · 肩胛提肌 · 颈浅筋膜侧线",
            targetSensationCn = "颈侧至肩峰连线呈柔和细长牵拉感（约 3–4/10），无挤压痛。",
            antiCompensationCn = "躯干保持垂直不随之侧歪，对侧手轻扶椅沿稳固肩胛骨。",
            redFlagWarningCn = "倾斜侧颈根若出现卡压刺痛，请改小幅度并微收下颌。",
            setupCueCn = "双足平踏地面，双手轻搭大腿稳定肩胛基座。",
            safeReturnCueCn = "随吸气由颈侧柔缓托回中线，停一息再松肩。"
        )
    )

    val shoulderScapulaSigh = StretchPose(
        id = "shoulder_scapula_sigh",
        title = "肩胛微收长叹息",
        gentleCue = "吸气时双肩微微向后展开，呼气时顺着脊柱自然垂落，眉心也随之松开。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.10f, 0.20f, 0.20f, 0.40f, 28f, 28f, 0f, true),
        exhaleKeyframe = kf(0f, 0.25f, 0.98f, 0.98f, 0.10f, 10f, 10f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MICRO_PNF,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "中下斜方肌（激活） · 菱形肌 · 上斜方肌（交互抑制）",
            targetSensationCn = "吸气背部微温收拢，呼气双肩如冰雪融落般轻快（约 3/10）。",
            antiCompensationCn = "向后下方微收肩胛下角，避免向上耸肩挤脖子。",
            redFlagWarningCn = "肩缝若有弹响伴锐痛，请缩小后展弧度保持无痛范围。",
            setupCueCn = "脊柱自然舒展，手肘微屈垂于体侧准备呼吸。",
            safeReturnCueCn = "随最后一声长呼让肩胛静静落回中立胸廓。"
        )
    )

    val shoulderDoubleInhaleSigh = StretchPose(
        id = "shoulder_double_inhale_sigh",
        title = "生理性双吸长叹息",
        gentleCue = "鼻吸两小口填满胸臆，随后极慢地长呼而出，让耸起的双肩瞬间卸力。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.0f, 0.15f, 0.15f, 0.35f, 22f, 22f, 0f, true),
        exhaleKeyframe = kf(0f, 0.20f, 1.00f, 1.00f, 0.0f, 8f, 8f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "膈肌肋部 · 斜角肌辅助呼吸束 · 上斜方肌",
            targetSensationCn = "胸廓充分舒张后随慢呼迅速卸除交感紧绷，肩颈松沉。",
            antiCompensationCn = "第二小口补吸轻巧柔和，切勿猛力吸气引发头晕。",
            redFlagWarningCn = "若有呼吸急促或头晕感，即刻恢复自然平缓鼻呼吸。",
            setupCueCn = "轻阖双目松开腹部，双掌自然安放于膝上。",
            safeReturnCueCn = "回归平缓自然鼻息，静候双肩余温慢慢散开。"
        )
    )

    val shoulderWaterCircle = StretchPose(
        id = "shoulder_water_circle",
        title = "双肩温水画圆归位",
        gentleCue = "想象肩头浸在温水里，随吸气轻柔前升，随长呼气向后向下缓缓落定。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.05f, 0.18f, 0.18f, 0.30f, 32f, 32f, 0f, true),
        exhaleKeyframe = kf(0f, 0.15f, 0.92f, 0.92f, 0.18f, 12f, 12f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "盂肱关节囊 · 胸锁与肩锁关节滑膜 · 前锯肌",
            targetSensationCn = "肩关节四周呈温润滑顺流动感，僵滞咔哒感渐消（约 2–3/10）。",
            antiCompensationCn = "画圆轨迹重在向后向下沉落，头颈稳居原位不随之前探。",
            redFlagWarningCn = "若肩峰下方出现夹击刺痛，请放低前升高度。",
            setupCueCn = "双臂自然垂顺，想象肩头浸入微温泉水之中。",
            safeReturnCueCn = "画完最后一圈向后轻落，稳稳停在放松肩位。"
        )
    )

    // --- 9..12: THORACIC_CHEST (胸椎圆肩) ---
    val thoracicDeskCatCow = StretchPose(
        id = "thoracic_desk_cat_cow",
        title = "扶桌猫牛微浪",
        gentleCue = "指尖轻扶桌沿，吸气轻展胸口，呼气将背部柔和拱起，逐节唤醒胸椎。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST, BodyRegion.LOWER_LUMBAR),
        durationSeconds = 50,
        inhaleKeyframe = kf(0f, -0.25f, 0.70f, 0.70f, 0.75f, 58f, 58f, 0.15f, false),
        exhaleKeyframe = kf(0f, 0.65f, 0.85f, 0.85f, -0.75f, 64f, 64f, 0.10f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "胸椎多裂肌 · 竖脊肌胸段 · 前纵与后纵韧带滑移",
            targetSensationCn = "两肩胛之间胸椎逐节温热流动，背部板结感柔和化开（约 3/10）。",
            antiCompensationCn = "动作聚焦于中上背胸椎段微浪，避免全靠下腰大幅折弯代偿。",
            redFlagWarningCn = "急性腰背扭伤或骨质脆弱者请保持极微幅无痛摆动。",
            setupCueCn = "坐稳椅前段，指尖轻扶桌沿作轻巧支撑锚点。",
            safeReturnCueCn = "随吸气逐节叠起脊柱，柔和回归端坐中轴。"
        )
    )

    val thoracicClaspChestOpen = StretchPose(
        id = "thoracic_clasp_chest_open",
        title = "背后十指轻扣开胸",
        gentleCue = "双手在身后轻握，顺着吸气让锁骨向两侧微笑展开，不必刻意挺腰。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 50,
        inhaleKeyframe = kf(0f, -0.15f, 0.78f, 0.78f, 0.82f, 42f, 42f, 0.10f, true),
        exhaleKeyframe = kf(0f, 0.10f, 0.95f, 0.95f, 0.45f, 30f, 30f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MICRO_PNF,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "胸大肌锁骨束 · 胸小肌 · 肱二头肌长头腱膜",
            targetSensationCn = "锁骨下方与胸前筋膜呈开阔温热感（约 3–4/10），呼吸顺畅。",
            antiCompensationCn = "下肋微收稳住核心，避免挺肚子顶腰假性开胸；手肘保留微弯。",
            redFlagWarningCn = "若前肩关节囊疼痛或手腕不适，改为双手扶后侧椅沿即可。",
            setupCueCn = "稍移坐向椅前三分之一，双手于身后温和相拢。",
            safeReturnCueCn = "先微屈双肘卸开胸前张力，再轻轻松开十指。"
        )
    )

    val thoracicChairClavicleSmile = StretchPose(
        id = "thoracic_chair_clavicle_smile",
        title = "椅侧锁骨微笑展开",
        gentleCue = "双手轻扶椅侧向后微展，让内扣一天的肩头温和归位，呼吸直达胸臆。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.10f, 0.72f, 0.72f, 0.70f, 46f, 46f, 0.15f, true),
        exhaleKeyframe = kf(0f, 0.10f, 0.90f, 0.90f, 0.35f, 34f, 34f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "胸小肌 · 锁骨下肌 · 前侧深层臂丛通道筋膜",
            targetSensationCn = "双侧锁骨如微笑般向两侧横向铺展，呼吸自然沉入胸底（约 3/10）。",
            antiCompensationCn = "颈部保持修长微收下颌，切勿在开胸时仰头挤压后颈。",
            redFlagWarningCn = "若指端发麻请放低扶椅位置并减小肩外旋幅度。",
            setupCueCn = "双手轻搭椅侧边缘，掌心朝前温和外旋肩头。",
            safeReturnCueCn = "随呼气缓缓收回双手安放膝面，锁骨余留开阔。"
        )
    )

    val thoracicSeatedGentleTwist = StretchPose(
        id = "thoracic_seated_gentle_twist",
        title = "坐姿扶椅温和扭转",
        gentleCue = "一手轻扶椅背，随长呼气从胸口温和转向一侧，让肋间重获轻盈空间。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST, BodyRegion.LOWER_LUMBAR),
        durationSeconds = 45,
        inhaleKeyframe = kf(4f, 0.10f, 0.55f, 0.65f, 0.35f, 35f, 55f, 0f, false),
        exhaleKeyframe = kf(12f, 0.15f, 0.80f, 0.88f, 0.55f, 42f, 68f, 0f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "腹内外斜肌筋膜 · 肋间肌 · 胸腰筋膜螺旋线",
            targetSensationCn = "中背与侧肋呈温和螺旋舒展感，每一次呼气肋廓更松软（约 3/10）。",
            antiCompensationCn = "双膝朝正前对齐不随骨盆歪斜，先延展脊柱再从胸口轻转，勿用蛮力扳椅。",
            redFlagWarningCn = "椎间盘急性不适期请避免末端施压扭转，仅做极微幅呼吸转向。",
            setupCueCn = "双膝与髋同宽朝正前，吸气先轻轻立起胸口。",
            safeReturnCueCn = "先松开扶椅手劲，随吸气由胸椎逐节旋回正中。"
        )
    )

    // --- 13..16: LOWER_LUMBAR (下腰久坐) ---
    val lumbarPelvicRock = StretchPose(
        id = "lumbar_pelvic_rock",
        title = "椅沿骨盆前后微倾",
        gentleCue = "坐于椅前三分之一，吸气轻立腰椎，呼气微卷尾骨，唤醒腰骶温和流动。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.05f, 0.60f, 0.60f, 0.55f, 20f, 20f, 0f, true),
        exhaleKeyframe = kf(0f, 0.35f, 0.85f, 0.85f, -0.55f, 18f, 18f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "腰方肌深层 · 腰段多裂肌 · 骶髂周围韧带滑膜",
            targetSensationCn = "坐骨在椅面轻柔滚动，下腰僵硬酸滞随节律慢慢松活（约 2–3/10）。",
            antiCompensationCn = "头顶保持平稳悬浮不前后大幅晃动，专注骨盆像盛水浅碗般微倾。",
            redFlagWarningCn = "若前倾或后卷某侧出现锐刺痛，请缩小至完全舒适区间。",
            setupCueCn = "移坐椅前三分之一，双脚稳踏地面与髋同宽。",
            safeReturnCueCn = "轻巧停在坐骨正中的中立骨盆位，深长一呼。"
        )
    )

    val lumbarWillowSideBend = StretchPose(
        id = "lumbar_willow_side_bend",
        title = "坐姿柳枝脊柱侧弯",
        gentleCue = "一手轻撑椅面，另一臂如柳枝顺呼吸向头顶延展，舒展久坐紧缩的侧腰。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR, BodyRegion.THORACIC_CHEST),
        durationSeconds = 50,
        inhaleKeyframe = kf(8f, 0.0f, 0.45f, 0.75f, 0.30f, 138f, 24f, 0.10f, false),
        exhaleKeyframe = kf(16f, 0.10f, 0.65f, 0.92f, 0.50f, 156f, 18f, 0.20f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "腰方肌 · 背阔肌侧缘 · 肋间外肌体侧线",
            targetSensationCn = "举臂侧从盆骨上缘至腋下呈连绵温润拉开感（约 3–4/10）。",
            antiCompensationCn = "延展侧坐骨稳稳压住椅面不翘起，重在向上拔长而非塌陷折腰。",
            redFlagWarningCn = "肩关节活动受限者可将上举手轻搭耳侧或肩头，无需勉强过顶。",
            setupCueCn = "一手稳扶椅边作支点，两侧坐骨均匀落稳椅面。",
            safeReturnCueCn = "撑椅手轻推助立躯干回正，再顺呼吸缓落上臂。"
        )
    )

    val lumbarSeatedFigureFour = StretchPose(
        id = "lumbar_seated_figure_four",
        title = "坐姿搭膝臀腰轻舒",
        gentleCue = "一踝轻搭对侧膝上，保持背部舒展随呼气微微前倾，化开臀腰深处酸沉。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.10f, 0.60f, 0.60f, 0.25f, 26f, 26f, 0f, true),
        exhaleKeyframe = kf(0f, 0.35f, 0.88f, 0.88f, -0.45f, 34f, 34f, 0f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MICRO_PNF,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "梨状肌 · 臀中肌后束 · 胸腰筋膜下缘",
            targetSensationCn = "搭腿侧臀部深处呈饱满温热酸舒感（约 3–4/10），下腰随之轻松。",
            antiCompensationCn = "从髋关节折页处保持长背微倾，避免弓背低头代偿，切勿用力按压膝盖。",
            redFlagWarningCn = "若膝关节内侧挤压不适或大腿后侧放射麻痛，请立即放平双脚。",
            setupCueCn = "一侧脚踝轻搭对侧膝上，微勾脚尖保护膝关节。",
            safeReturnCueCn = "先随吸气扶直上身躯干，再双手轻托脚踝放回。"
        )
    )

    val lumbarBedsideButterflyFold = StretchPose(
        id = "lumbar_bedside_butterfly_fold",
        title = "床沿蝴蝶轻俯释腰",
        gentleCue = "脚心相对松散安放，上身随绵长呼气自然微倾，给下腰留出温柔呼吸余地。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR),
        durationSeconds = 60,
        inhaleKeyframe = kf(0f, 0.15f, 0.65f, 0.65f, 0.15f, 28f, 28f, 0f, true),
        exhaleKeyframe = kf(0f, 0.50f, 0.95f, 0.95f, -0.65f, 38f, 38f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.SAGITTAL_SIDE,
            primaryMusclesCn = "胸腰筋膜后浅层 · 竖脊肌腰段 · 大收肌近端",
            targetSensationCn = "下腰两侧深层筋膜随长呼气柔和摊开，伴随安宁困意（约 3/10）。",
            antiCompensationCn = "双脚略远离身体成宽松菱形，手肘轻支大腿分担上身自重，不悬空硬吊。",
            redFlagWarningCn = "腰椎间盘突出急性期请保持直背微倾，避免深幅弯腰圆背。",
            setupCueCn = "脚心相对距身一尺半成宽菱形，双手轻扶胫前。",
            safeReturnCueCn = "双手轻撑大腿，从下腰逐节缓缓卷起最后抬头。"
        )
    )

    // --- 17..20: MOUSE_WRIST (手腕鼠标侧) ---
    val wristPalmForwardSilk = StretchPose(
        id = "wrist_palm_forward_silk",
        title = "前臂掌心朝外轻展",
        gentleCue = "一臂平伸掌心朝外，另一手轻拢四指向回微引，舒展鼠标侧前臂内侧筋膜。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.10f, 0.65f, 0.60f, 0.10f, 68f, 78f, 0.45f, false),
        exhaleKeyframe = kf(0f, 0.15f, 0.85f, 0.82f, 0.05f, 64f, 82f, 0.92f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.NERVE_GLIDING,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "桡侧与尺侧腕屈肌 · 指浅屈肌 · 正中神经腕管段",
            targetSensationCn = "前臂掌侧呈清晰温润延展感（约 3/10），指端无发麻刺痛。",
            antiCompensationCn = "肩膀下沉微屈肘关节5度留出神经余量，仅轻引四指不猛扳拇指。",
            redFlagWarningCn = "若掌心或前三指出现电麻感，立即微屈手肘并减小背伸角。",
            setupCueCn = "双肩先沉落，一臂向体前轻伸并保留手肘微润。",
            safeReturnCueCn = "先松开引指手劲，让手腕自然垂软轻晃两下。"
        )
    )

    val wristWarmWaterWave = StretchPose(
        id = "wrist_warm_water_wave",
        title = "十指温水波浪舒张",
        gentleCue = "像指尖拂过温水表面，随呼吸逐根舒展指节再轻柔松落，唤回指端温热。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.05f, 0.60f, 0.60f, 0.15f, 56f, 56f, 0.70f, true),
        exhaleKeyframe = kf(0f, 0.15f, 0.88f, 0.88f, 0.05f, 42f, 42f, -0.55f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.MOBILITY_CARS,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "指总伸肌与屈肌腱鞘 · 蚓状肌 · 腕骨间滑膜",
            targetSensationCn = "十指指节与掌心微热回流，敲击键盘后的僵木感消融（约 2/10）。",
            antiCompensationCn = "动作像在温水中拂过般连绵柔顺，避免指节僵直用力绷紧。",
            redFlagWarningCn = "指关节若有腱鞘弹响疼痛，请缩小屈伸幅度温和滑移。",
            setupCueCn = "双肘轻靠体侧，前臂悬抬于胸腹之间松软手腕。",
            safeReturnCueCn = "十指自然半弯松落于腿面，感受掌心微温回流。"
        )
    )

    val wristPrayerLotusTurn = StretchPose(
        id = "wrist_prayer_lotus_turn",
        title = "胸前合掌腕节轻转",
        gentleCue = "掌心于胸前温和相合，随呼气将前臂轻轻下沉半寸，松开敲字后的酸胀。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.10f, 0.55f, 0.55f, 0.20f, 72f, 72f, 0.50f, true),
        exhaleKeyframe = kf(0f, 0.20f, 0.90f, 0.90f, 0.10f, 60f, 60f, 0.88f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "旋前圆肌 · 掌长肌筋膜 · 腕横韧带浅层",
            targetSensationCn = "双侧腕根掌侧对称温和舒张，前臂酸胀渐趋平缓（约 3/10）。",
            antiCompensationCn = "掌根保持轻贴不强压，双肩自然垂落不因合掌而耸起。",
            redFlagWarningCn = "腕管敏感或撑地痛者请抬高合掌位置至锁骨前，减小折角。",
            setupCueCn = "双掌于胸口正前温和相合，指尖自然向上舒展。",
            safeReturnCueCn = "前臂轻轻回升卸除折角，双掌缓缓分开落膝。"
        )
    )

    val wristThumbTendonRelease = StretchPose(
        id = "wrist_thumb_tendon_release",
        title = "握拳包拇尺侧轻舒",
        gentleCue = "四指轻拢拇指成柔拳，手腕向小指侧微倾，顺长呼气解开腕侧细微紧绷。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.08f, 0.60f, 0.60f, 0.12f, 52f, 52f, -0.25f, false),
        exhaleKeyframe = kf(0f, 0.15f, 0.85f, 0.85f, 0.05f, 48f, 48f, -0.80f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.NERVE_GLIDING,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "拇长展肌 · 拇短伸肌腱鞘 · 桡神经浅支滑道",
            targetSensationCn = "大拇指根部腕侧呈温和细腻牵引感（约 3/10），切忌锐痛。",
            antiCompensationCn = "四指仅轻柔拢住拇指如握雀卵，向小指侧微倾即可，严禁顿挫猛甩。",
            redFlagWarningCn = "桡骨茎突腱鞘炎急性红肿刺痛期请跳过尺偏，改做温水波浪。",
            setupCueCn = "前臂平放或轻悬，拇指轻收掌心由四指温柔拢住。",
            safeReturnCueCn = "先将手腕回平中立，再缓缓舒展四指与大拇指。"
        )
    )

    // --- 21..24: EYE_BROW (眼周眉心) ---
    val eyeWarmPalming = StretchPose(
        id = "eye_warm_palming",
        title = "搓热掌心温敷双目",
        gentleCue = "搓热双掌轻轻拢在眼眶周围，不压眼球，在温热静暗里听三声绵长呼吸。",
        targetRegions = setOf(BodyRegion.EYE_BROW),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.15f, 0.60f, 0.60f, 0.05f, 108f, 108f, 0.10f, true),
        exhaleKeyframe = kf(0f, 0.28f, 0.88f, 0.88f, -0.08f, 102f, 102f, 0.0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.OCULOMOTOR_RELEASE,
            phaseRole = PrescriptionPhaseRole.POSTURAL_INTEGRATION,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "眼轮匝肌 · 睫状肌（暗适应放松） · 三叉神经眶上支",
            targetSensationCn = "眼眶被掌心暖意包裹，视觉噪点渐隐，眉心与后颈同步松弛。",
            antiCompensationCn = "掌心微拱如小碗轻扣眶骨边缘，绝不直接按压眼球本身。",
            redFlagWarningCn = "佩戴隐形眼镜或眼部不适时更须悬空虚拢，零压迫眼球。",
            setupCueCn = "轻轻搓暖双掌，手肘稳支桌面避免肩臂悬空劳累。",
            safeReturnCueCn = "先在掌心暗处轻眨双目，再缓缓移开手掌迎光。"
        )
    )

    val eyeSlowOrbitMist = StretchPose(
        id = "eye_slow_orbit_mist",
        title = "闭目凝神极慢转睛",
        gentleCue = "轻轻合上双眼，眼球如浮云般极慢画圆两周，把久盯屏幕的干涩缓缓化开。",
        targetRegions = setOf(BodyRegion.EYE_BROW),
        durationSeconds = 40,
        inhaleKeyframe = kf(-3f, 0.05f, 0.65f, 0.65f, 0.15f, 14f, 14f, 0f, true),
        exhaleKeyframe = kf(3f, 0.18f, 0.90f, 0.90f, 0.08f, 10f, 10f, 0f, true),
        bilateralSwitchAtHalfway = true,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.OCULOMOTOR_RELEASE,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "眼外六肌（上下内外直肌与斜肌） · 眼颈协同反射枕下群",
            targetSensationCn = "眼球后方紧缩感随慢转轻柔散开，泪膜均匀湿润眼表（约 2/10）。",
            antiCompensationCn = "眼睑始终轻柔闭合不挤眼，转动速度极慢且幅度留有三分余地。",
            redFlagWarningCn = "高度近视、视网膜脆弱或眩晕敏感者请改做闭目静息不转睛。",
            setupCueCn = "端坐轻阖双目，先松开紧咬的后槽牙与眉心。",
            safeReturnCueCn = "目光在闭合眼帘内静归正中，深呼一息再微睁。"
        )
    )

    val eyeBrowCuanzhuSmoothing = StretchPose(
        id = "eye_brow_cuanzhu_smoothing",
        title = "指腹轻拂攒竹眉心",
        gentleCue = "双手指腹由眉心向两侧太阳穴轻柔拂开，随呼气松开不自觉蹙起的眉头。",
        targetRegions = setOf(BodyRegion.EYE_BROW, BodyRegion.POSTERIOR_NECK),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.10f, 0.58f, 0.58f, 0.10f, 112f, 112f, 0.15f, true),
        exhaleKeyframe = kf(0f, 0.22f, 0.85f, 0.85f, 0.05f, 104f, 104f, 0.05f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.STATIC_FASCIAL,
            phaseRole = PrescriptionPhaseRole.TARGET_RELEASE,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "皱眉肌 · 降眉间肌 · 帽状腱膜额腹浅筋膜",
            targetSensationCn = "眉头攒竹至太阳穴呈温润舒展感，久思凝神后的额前紧箍感消散。",
            antiCompensationCn = "指腹贴肤轻柔滑移如拂宣纸，避免指甲刮擦或用力揉搓眼皮。",
            redFlagWarningCn = "太阳穴区域仅做羽毛般轻拂，切勿重按深压。",
            setupCueCn = "食中二指并拢温热，轻落于双眉内侧攒竹微凹处。",
            safeReturnCueCn = "指腹于太阳穴轻停一息，顺双颊缓落回膝面。"
        )
    )

    val eyeHorizonSoftGaze = StretchPose(
        id = "eye_horizon_soft_gaze",
        title = "远山凝眸与柔眨润目",
        gentleCue = "目光越过屏幕投向远处窗外天际，轻眨三次后闭目小憩，让眼底重归清润。",
        targetRegions = setOf(BodyRegion.EYE_BROW, BodyRegion.POSTERIOR_NECK),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, -0.10f, 0.62f, 0.62f, 0.22f, 15f, 15f, 0f, false),
        exhaleKeyframe = kf(0f, 0.12f, 0.90f, 0.90f, 0.10f, 12f, 12f, 0f, true),
        bilateralSwitchAtHalfway = false,
        clinical = PoseClinicalMetadata(
            modality = StretchModality.OCULOMOTOR_RELEASE,
            phaseRole = PrescriptionPhaseRole.WARMUP_UNBIND,
            viewPlane = AnatomicalViewPlane.CORONAL_FRONT,
            primaryMusclesCn = "睫状肌（远眺解痉） · 睑板腺（完整轻眨促泌） · 提上睑肌",
            targetSensationCn = "近距痉挛的晶状体调节肌悄然松开，眼表重获清润微凉感。",
            antiCompensationCn = "目光柔和漫散地投向六米外远景，不眯眼费力辨认细小字迹。",
            redFlagWarningCn = "避免直视刺眼强光源或正午反光玻璃。",
            setupCueCn = "将视线从近处屏幕温柔移开，投向窗外最远天际。",
            safeReturnCueCn = "完整轻柔闭合上下眼睑两次，温润回归当下视线。"
        )
    )

    val allPoses: List<StretchPose> = listOf(
        neckChinTuck,
        neckOcciputCradle,
        neckFengchiNod,
        neckThumbFengchiPress,
        shoulderEarMelt,
        shoulderScapulaSigh,
        shoulderDoubleInhaleSigh,
        shoulderWaterCircle,
        thoracicDeskCatCow,
        thoracicClaspChestOpen,
        thoracicChairClavicleSmile,
        thoracicSeatedGentleTwist,
        lumbarPelvicRock,
        lumbarWillowSideBend,
        lumbarSeatedFigureFour,
        lumbarBedsideButterflyFold,
        wristPalmForwardSilk,
        wristWarmWaterWave,
        wristPrayerLotusTurn,
        wristThumbTendonRelease,
        eyeWarmPalming,
        eyeSlowOrbitMist,
        eyeBrowCuanzhuSmoothing,
        eyeHorizonSoftGaze
    )

    val primaryPoolByRegion: Map<BodyRegion, List<StretchPose>> = mapOf(
        BodyRegion.POSTERIOR_NECK to listOf(neckChinTuck, neckOcciputCradle, neckFengchiNod, neckThumbFengchiPress),
        BodyRegion.TRAPEZIUS_SHOULDER to listOf(shoulderWaterCircle, shoulderEarMelt, shoulderScapulaSigh, shoulderDoubleInhaleSigh),
        BodyRegion.THORACIC_CHEST to listOf(thoracicDeskCatCow, thoracicClaspChestOpen, thoracicChairClavicleSmile, thoracicSeatedGentleTwist),
        BodyRegion.LOWER_LUMBAR to listOf(lumbarPelvicRock, lumbarWillowSideBend, lumbarSeatedFigureFour, lumbarBedsideButterflyFold),
        BodyRegion.MOUSE_WRIST to listOf(wristWarmWaterWave, wristPalmForwardSilk, wristPrayerLotusTurn, wristThumbTendonRelease),
        BodyRegion.EYE_BROW to listOf(eyeSlowOrbitMist, eyeBrowCuanzhuSmoothing, eyeWarmPalming, eyeHorizonSoftGaze)
    )

    val heroRoutinesByScene: Map<PostureScene, StretchRoutine> = mapOf(
        PostureScene.DESK_SEATED to StretchRoutine(
            id = "hero_desk_seated",
            title = "午后伏案 · 沉肩松颈帖",
            subtitle = "不必起身换装，伴随温润泛音与微震，把耸起的双肩轻轻放下。",
            scene = PostureScene.DESK_SEATED,
            eyesFreeCapable = true,
            seatedDiscreet = true,
            poses = listOf(
                shoulderWaterCircle.copy(durationSeconds = 60),
                shoulderEarMelt.copy(durationSeconds = 60),
                neckOcciputCradle.copy(durationSeconds = 60)
            )
        ),
        PostureScene.TEA_BREAK_STANDING to StretchRoutine(
            id = "hero_tea_break",
            title = "接水间隙 · 胸椎开阔帖",
            subtitle = "趁站在桌边或茶歇片刻，温和打开整日内扣的胸口与侧肋。",
            scene = PostureScene.TEA_BREAK_STANDING,
            eyesFreeCapable = true,
            seatedDiscreet = false,
            poses = listOf(
                thoracicDeskCatCow.copy(durationSeconds = 50),
                thoracicClaspChestOpen.copy(durationSeconds = 50),
                lumbarWillowSideBend.copy(durationSeconds = 50)
            )
        ),
        PostureScene.EVENING_BEDSIDE to StretchRoutine(
            id = "hero_evening_bedside",
            title = "晚间沉香 · 腰背解绑帖",
            subtitle = "坐于床沿随四秒吸气、六秒长呼气，把白日思绪与腰背酸沉一并放下。",
            scene = PostureScene.EVENING_BEDSIDE,
            eyesFreeCapable = true,
            seatedDiscreet = true,
            poses = listOf(
                lumbarPelvicRock.copy(durationSeconds = 60),
                lumbarWillowSideBend.copy(durationSeconds = 60),
                thoracicSeatedGentleTwist.copy(durationSeconds = 60),
                neckFengchiNod.copy(durationSeconds = 60),
                lumbarBedsideButterflyFold.copy(durationSeconds = 60),
                eyeWarmPalming.copy(durationSeconds = 60)
            )
        )
    )

    val quickMeetingSighRoutine = StretchRoutine(
        id = "quick_meeting_sigh",
        title = "连会余韵叹息",
        subtitle = "90秒 · 摘下耳机松开眉心与双肩",
        scene = PostureScene.DESK_SEATED,
        eyesFreeCapable = true,
        seatedDiscreet = true,
        poses = listOf(
            neckFengchiNod.copy(durationSeconds = 45),
            shoulderDoubleInhaleSigh.copy(durationSeconds = 45)
        )
    )

    val quickReliefRoutines: List<StretchRoutine> = listOf(
        quickMeetingSighRoutine,
        StretchRoutine(
            id = "quick_screen_warm_eyes",
            title = "久盯屏幕温目",
            subtitle = "2分钟 · 风池穴与眼周干涩舒缓",
            scene = PostureScene.DESK_SEATED,
            eyesFreeCapable = true,
            seatedDiscreet = true,
            poses = listOf(
                eyeSlowOrbitMist.copy(durationSeconds = 40),
                neckThumbFengchiPress.copy(durationSeconds = 40),
                eyeWarmPalming.copy(durationSeconds = 40)
            )
        ),
        StretchRoutine(
            id = "quick_mouse_wrist_relief",
            title = "鼠标手解缚",
            subtitle = "2分钟 · 前臂筋膜与十指温润回流",
            scene = PostureScene.DESK_SEATED,
            eyesFreeCapable = true,
            seatedDiscreet = true,
            poses = listOf(
                wristWarmWaterWave.copy(durationSeconds = 40),
                wristPalmForwardSilk.copy(durationSeconds = 40),
                wristPrayerLotusTurn.copy(durationSeconds = 40)
            )
        ),
        StretchRoutine(
            id = "quick_sedentary_lumbar_wake",
            title = "久坐松腰唤醒",
            subtitle = "3分钟 · 椅沿骨盆与侧腰温和舒展",
            scene = PostureScene.DESK_SEATED,
            eyesFreeCapable = true,
            seatedDiscreet = true,
            poses = listOf(
                lumbarPelvicRock.copy(durationSeconds = 45),
                lumbarSeatedFigureFour.copy(durationSeconds = 45),
                lumbarWillowSideBend.copy(durationSeconds = 45),
                thoracicChairClavicleSmile.copy(durationSeconds = 45)
            )
        )
    )

    private fun buildDailyRoutines(
        bookletId: String,
        dayTitles: List<String>,
        scene: PostureScene,
        pool: List<StretchPose>,
        posesPerDay: Int,
        secondsPerPose: Int
    ): List<StretchRoutine> = dayTitles.mapIndexed { dayIndex, dayTitle ->
        val rawDayPoses = (0 until posesPerDay).map { slot ->
            pool[(dayIndex * 2 + slot) % pool.size].copy(durationSeconds = secondsPerPose)
        }
        val dayPoses = rawDayPoses.sortedBy { it.clinical.phaseRole.order }
        StretchRoutine(
            id = "${bookletId}_day_${dayIndex}",
            title = dayTitle,
            subtitle = "含：" + dayPoses.joinToString(" · ") { it.title },
            scene = scene,
            eyesFreeCapable = true,
            seatedDiscreet = dayPoses.none {
                it.id == "thoracic_desk_cat_cow" || it.id == "thoracic_clasp_chest_open"
            },
            poses = dayPoses
        )
    }

    val curatedBooklets: List<BookletSeries> = listOf(
        BookletSeries(
            id = "booklet_neck_thoracic",
            title = "《伏案颈胸开阔记》",
            subtitle = "温和改善圆肩含胸与长期低头后的颈后紧绷，随时中断亦无妨。",
            totalDays = 7,
            dailyMinutes = 5,
            routines = buildDailyRoutines(
                bookletId = "booklet_neck_thoracic",
                dayTitles = listOf(
                    "第一日 · 松肩初醒",
                    "第二日 · 风池微风",
                    "第三日 · 锁骨微笑",
                    "第四日 · 肩胛轻叹",
                    "第五日 · 温托归位",
                    "第六日 · 肋间生风",
                    "第七日 · 颈胸清朗"
                ),
                scene = PostureScene.DESK_SEATED,
                pool = listOf(
                    neckChinTuck,
                    shoulderEarMelt,
                    thoracicChairClavicleSmile,
                    neckOcciputCradle,
                    shoulderScapulaSigh,
                    thoracicClaspChestOpen,
                    neckFengchiNod,
                    shoulderWaterCircle
                ),
                posesPerDay = 5,
                secondsPerPose = 60
            )
        ),
        BookletSeries(
            id = "booklet_lumbar_pelvis",
            title = "《久坐腰骶归位帖》",
            subtitle = "针对久坐臀腰酸沉与骨盆僵滞的椅沿及桌边温和舒展。",
            totalDays = 7,
            dailyMinutes = 6,
            routines = buildDailyRoutines(
                bookletId = "booklet_lumbar_pelvis",
                dayTitles = listOf(
                    "第一日 · 尾闾轻摇",
                    "第二日 · 柳枝侧展",
                    "第三日 · 臀腰化沉",
                    "第四日 · 胸腰微浪",
                    "第五日 · 肋侧轻旋",
                    "第六日 · 蝴蝶释腰",
                    "第七日 · 腰脊安稳"
                ),
                scene = PostureScene.DESK_SEATED,
                pool = listOf(
                    lumbarPelvicRock,
                    lumbarWillowSideBend,
                    lumbarSeatedFigureFour,
                    thoracicDeskCatCow,
                    thoracicSeatedGentleTwist,
                    lumbarBedsideButterflyFold,
                    shoulderDoubleInhaleSigh,
                    thoracicChairClavicleSmile
                ),
                posesPerDay = 6,
                secondsPerPose = 60
            )
        ),
        BookletSeries(
            id = "booklet_evening_breath",
            title = "《晚间呼吸解绑》",
            subtitle = "以四吸六呼长节律配合全身轻柔松解，洗去日间疲惫安然入眠。",
            totalDays = 7,
            dailyMinutes = 6,
            routines = buildDailyRoutines(
                bookletId = "booklet_evening_breath",
                dayTitles = listOf(
                    "第一夜 · 沉肩卸甲",
                    "第二夜 · 眉心化雪",
                    "第三夜 · 蝴蝶听息",
                    "第四夜 · 风池温养",
                    "第五夜 · 柳影慢摇",
                    "第六夜 · 掌心静暗",
                    "第七夜 · 松风入眠"
                ),
                scene = PostureScene.EVENING_BEDSIDE,
                pool = listOf(
                    shoulderWaterCircle,
                    neckFengchiNod,
                    lumbarBedsideButterflyFold,
                    eyeWarmPalming,
                    lumbarWillowSideBend,
                    neckOcciputCradle,
                    eyeBrowCuanzhuSmoothing,
                    shoulderDoubleInhaleSigh
                ),
                posesPerDay = 6,
                secondsPerPose = 60
            )
        ),
        BookletSeries(
            id = "booklet_eyes_wrist_care",
            title = "《屏幕温目护腕册》",
            subtitle = "专为高频敲字与盯屏人群调配的眉眼清润及手腕前臂养护专册。",
            totalDays = 5,
            dailyMinutes = 4,
            routines = buildDailyRoutines(
                bookletId = "booklet_eyes_wrist_care",
                dayTitles = listOf(
                    "第一日 · 掌心温目",
                    "第二日 · 指尖温水",
                    "第三日 · 攒竹拂云",
                    "第四日 · 合掌轻沉",
                    "第五日 · 眸清指轻"
                ),
                scene = PostureScene.DESK_SEATED,
                pool = listOf(
                    eyeWarmPalming,
                    wristPalmForwardSilk,
                    eyeSlowOrbitMist,
                    wristWarmWaterWave,
                    eyeBrowCuanzhuSmoothing,
                    wristPrayerLotusTurn,
                    eyeHorizonSoftGaze,
                    wristThumbTendonRelease,
                    neckThumbFengchiPress
                ),
                posesPerDay = 4,
                secondsPerPose = 60
            )
        )
    )
}
