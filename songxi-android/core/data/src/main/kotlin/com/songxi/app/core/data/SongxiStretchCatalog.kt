package com.songxi.app.core.data

import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.PoseKeyframe
import com.songxi.app.core.model.PostureScene
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
        bilateralSwitchAtHalfway = false
    )

    val neckOcciputCradle = StretchPose(
        id = "neck_occiput_cradle",
        title = "双手相扣颈后温托",
        gentleCue = "掌心轻托后颈风池，下颌微扬半寸，借头部自重缓缓靠入温热掌心。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.35f, 0.50f, 0.50f, 0.45f, 118f, 118f, 0f, false),
        exhaleKeyframe = kf(0f, -0.65f, 0.82f, 0.82f, 0.30f, 112f, 112f, 0f, true),
        bilateralSwitchAtHalfway = false
    )

    val neckFengchiNod = StretchPose(
        id = "neck_fengchi_nod",
        title = "左右风池微倾松解",
        gentleCue = "摘下耳机松开牙关，头向一侧微倾停留，在绵长呼气里等颈侧变软。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 45,
        inhaleKeyframe = kf(8f, 0.20f, 0.55f, 0.55f, 0.10f, 15f, 15f, 0f, true),
        exhaleKeyframe = kf(22f, 0.35f, 0.95f, 0.75f, 0.05f, 10f, 12f, 0f, true),
        bilateralSwitchAtHalfway = true
    )

    val neckThumbFengchiPress = StretchPose(
        id = "neck_thumb_fengchi_press",
        title = "指腹轻倚风池温养",
        gentleCue = "双肘轻支桌面，拇指落于脑后发际凹处，随长呼气卸下低头整日的重量。",
        targetRegions = setOf(BodyRegion.POSTERIOR_NECK, BodyRegion.EYE_BROW),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.15f, 0.50f, 0.50f, 0.05f, 98f, 98f, 0f, true),
        exhaleKeyframe = kf(0f, -0.45f, 0.85f, 0.85f, 0.15f, 94f, 94f, 0f, true),
        bilateralSwitchAtHalfway = false
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
        bilateralSwitchAtHalfway = true
    )

    val shoulderScapulaSigh = StretchPose(
        id = "shoulder_scapula_sigh",
        title = "肩胛微收长叹息",
        gentleCue = "吸气时双肩微微向后展开，呼气时顺着脊柱自然垂落，眉心也随之松开。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.10f, 0.20f, 0.20f, 0.40f, 28f, 28f, 0f, true),
        exhaleKeyframe = kf(0f, 0.25f, 0.98f, 0.98f, 0.10f, 10f, 10f, 0f, true),
        bilateralSwitchAtHalfway = false
    )

    val shoulderDoubleInhaleSigh = StretchPose(
        id = "shoulder_double_inhale_sigh",
        title = "生理性双吸长叹息",
        gentleCue = "鼻吸两小口填满胸臆，随后极慢地长呼而出，让耸起的双肩瞬间卸力。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.0f, 0.15f, 0.15f, 0.35f, 22f, 22f, 0f, true),
        exhaleKeyframe = kf(0f, 0.20f, 1.00f, 1.00f, 0.0f, 8f, 8f, 0f, true),
        bilateralSwitchAtHalfway = false
    )

    val shoulderWaterCircle = StretchPose(
        id = "shoulder_water_circle",
        title = "双肩温水画圆归位",
        gentleCue = "想象肩头浸在温水里，随吸气轻柔前升，随长呼气向后向下缓缓落定。",
        targetRegions = setOf(BodyRegion.TRAPEZIUS_SHOULDER, BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.05f, 0.18f, 0.18f, 0.30f, 32f, 32f, 0f, true),
        exhaleKeyframe = kf(0f, 0.15f, 0.92f, 0.92f, 0.18f, 12f, 12f, 0f, true),
        bilateralSwitchAtHalfway = false
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
        bilateralSwitchAtHalfway = false
    )

    val thoracicClaspChestOpen = StretchPose(
        id = "thoracic_clasp_chest_open",
        title = "背后十指轻扣开胸",
        gentleCue = "双手在身后轻握，顺着吸气让锁骨向两侧微笑展开，不必刻意挺腰。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 50,
        inhaleKeyframe = kf(0f, -0.15f, 0.78f, 0.78f, 0.82f, 42f, 42f, 0.10f, true),
        exhaleKeyframe = kf(0f, 0.10f, 0.95f, 0.95f, 0.45f, 30f, 30f, 0f, true),
        bilateralSwitchAtHalfway = false
    )

    val thoracicChairClavicleSmile = StretchPose(
        id = "thoracic_chair_clavicle_smile",
        title = "椅侧锁骨微笑展开",
        gentleCue = "双手轻扶椅侧向后微展，让内扣一天的肩头温和归位，呼吸直达胸臆。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, -0.10f, 0.72f, 0.72f, 0.70f, 46f, 46f, 0.15f, true),
        exhaleKeyframe = kf(0f, 0.10f, 0.90f, 0.90f, 0.35f, 34f, 34f, 0f, true),
        bilateralSwitchAtHalfway = false
    )

    val thoracicSeatedGentleTwist = StretchPose(
        id = "thoracic_seated_gentle_twist",
        title = "坐姿扶椅温和扭转",
        gentleCue = "一手轻扶椅背，随长呼气从胸口温和转向一侧，让肋间重获轻盈空间。",
        targetRegions = setOf(BodyRegion.THORACIC_CHEST, BodyRegion.LOWER_LUMBAR),
        durationSeconds = 45,
        inhaleKeyframe = kf(4f, 0.10f, 0.55f, 0.65f, 0.35f, 35f, 55f, 0f, false),
        exhaleKeyframe = kf(12f, 0.15f, 0.80f, 0.88f, 0.55f, 42f, 68f, 0f, true),
        bilateralSwitchAtHalfway = true
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
        bilateralSwitchAtHalfway = false
    )

    val lumbarWillowSideBend = StretchPose(
        id = "lumbar_willow_side_bend",
        title = "坐姿柳枝脊柱侧弯",
        gentleCue = "一手轻撑椅面，另一臂如柳枝顺呼吸向头顶延展，舒展久坐紧缩的侧腰。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR, BodyRegion.THORACIC_CHEST),
        durationSeconds = 50,
        inhaleKeyframe = kf(8f, 0.0f, 0.45f, 0.75f, 0.30f, 138f, 24f, 0.10f, false),
        exhaleKeyframe = kf(16f, 0.10f, 0.65f, 0.92f, 0.50f, 156f, 18f, 0.20f, true),
        bilateralSwitchAtHalfway = true
    )

    val lumbarSeatedFigureFour = StretchPose(
        id = "lumbar_seated_figure_four",
        title = "坐姿搭膝臀腰轻舒",
        gentleCue = "一踝轻搭对侧膝上，保持背部舒展随呼气微微前倾，化开臀腰深处酸沉。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR),
        durationSeconds = 45,
        inhaleKeyframe = kf(0f, 0.10f, 0.60f, 0.60f, 0.25f, 26f, 26f, 0f, true),
        exhaleKeyframe = kf(0f, 0.35f, 0.88f, 0.88f, -0.45f, 34f, 34f, 0f, true),
        bilateralSwitchAtHalfway = true
    )

    val lumbarBedsideButterflyFold = StretchPose(
        id = "lumbar_bedside_butterfly_fold",
        title = "床沿蝴蝶轻俯释腰",
        gentleCue = "脚心相对松散安放，上身随绵长呼气自然微倾，给下腰留出温柔呼吸余地。",
        targetRegions = setOf(BodyRegion.LOWER_LUMBAR),
        durationSeconds = 60,
        inhaleKeyframe = kf(0f, 0.15f, 0.65f, 0.65f, 0.15f, 28f, 28f, 0f, true),
        exhaleKeyframe = kf(0f, 0.50f, 0.95f, 0.95f, -0.65f, 38f, 38f, 0f, true),
        bilateralSwitchAtHalfway = false
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
        bilateralSwitchAtHalfway = true
    )

    val wristWarmWaterWave = StretchPose(
        id = "wrist_warm_water_wave",
        title = "十指温水波浪舒张",
        gentleCue = "像指尖拂过温水表面，随呼吸逐根舒展指节再轻柔松落，唤回指端温热。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.05f, 0.60f, 0.60f, 0.15f, 56f, 56f, 0.70f, true),
        exhaleKeyframe = kf(0f, 0.15f, 0.88f, 0.88f, 0.05f, 42f, 42f, -0.55f, true),
        bilateralSwitchAtHalfway = false
    )

    val wristPrayerLotusTurn = StretchPose(
        id = "wrist_prayer_lotus_turn",
        title = "胸前合掌腕节轻转",
        gentleCue = "掌心于胸前温和相合，随呼气将前臂轻轻下沉半寸，松开敲字后的酸胀。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST, BodyRegion.TRAPEZIUS_SHOULDER),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.10f, 0.55f, 0.55f, 0.20f, 72f, 72f, 0.50f, true),
        exhaleKeyframe = kf(0f, 0.20f, 0.90f, 0.90f, 0.10f, 60f, 60f, 0.88f, true),
        bilateralSwitchAtHalfway = false
    )

    val wristThumbTendonRelease = StretchPose(
        id = "wrist_thumb_tendon_release",
        title = "握拳包拇尺侧轻舒",
        gentleCue = "四指轻拢拇指成柔拳，手腕向小指侧微倾，顺长呼气解开腕侧细微紧绷。",
        targetRegions = setOf(BodyRegion.MOUSE_WRIST),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.08f, 0.60f, 0.60f, 0.12f, 52f, 52f, -0.25f, false),
        exhaleKeyframe = kf(0f, 0.15f, 0.85f, 0.85f, 0.05f, 48f, 48f, -0.80f, true),
        bilateralSwitchAtHalfway = true
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
        bilateralSwitchAtHalfway = false
    )

    val eyeSlowOrbitMist = StretchPose(
        id = "eye_slow_orbit_mist",
        title = "闭目凝神极慢转睛",
        gentleCue = "轻轻合上双眼，眼球如浮云般极慢画圆两周，把久盯屏幕的干涩缓缓化开。",
        targetRegions = setOf(BodyRegion.EYE_BROW),
        durationSeconds = 40,
        inhaleKeyframe = kf(-3f, 0.05f, 0.65f, 0.65f, 0.15f, 14f, 14f, 0f, true),
        exhaleKeyframe = kf(3f, 0.18f, 0.90f, 0.90f, 0.08f, 10f, 10f, 0f, true),
        bilateralSwitchAtHalfway = true
    )

    val eyeBrowCuanzhuSmoothing = StretchPose(
        id = "eye_brow_cuanzhu_smoothing",
        title = "指腹轻拂攒竹眉心",
        gentleCue = "双手指腹由眉心向两侧太阳穴轻柔拂开，随呼气松开不自觉蹙起的眉头。",
        targetRegions = setOf(BodyRegion.EYE_BROW, BodyRegion.POSTERIOR_NECK),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, 0.10f, 0.58f, 0.58f, 0.10f, 112f, 112f, 0.15f, true),
        exhaleKeyframe = kf(0f, 0.22f, 0.85f, 0.85f, 0.05f, 104f, 104f, 0.05f, true),
        bilateralSwitchAtHalfway = false
    )

    val eyeHorizonSoftGaze = StretchPose(
        id = "eye_horizon_soft_gaze",
        title = "远山凝眸与柔眨润目",
        gentleCue = "目光越过屏幕投向远处窗外天际，轻眨三次后闭目小憩，让眼底重归清润。",
        targetRegions = setOf(BodyRegion.EYE_BROW, BodyRegion.POSTERIOR_NECK),
        durationSeconds = 40,
        inhaleKeyframe = kf(0f, -0.10f, 0.62f, 0.62f, 0.22f, 15f, 15f, 0f, false),
        exhaleKeyframe = kf(0f, 0.12f, 0.90f, 0.90f, 0.10f, 12f, 12f, 0f, true),
        bilateralSwitchAtHalfway = false
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
        BodyRegion.TRAPEZIUS_SHOULDER to listOf(shoulderEarMelt, shoulderScapulaSigh, shoulderDoubleInhaleSigh, shoulderWaterCircle),
        BodyRegion.THORACIC_CHEST to listOf(thoracicDeskCatCow, thoracicClaspChestOpen, thoracicChairClavicleSmile, thoracicSeatedGentleTwist),
        BodyRegion.LOWER_LUMBAR to listOf(lumbarPelvicRock, lumbarWillowSideBend, lumbarSeatedFigureFour, lumbarBedsideButterflyFold),
        BodyRegion.MOUSE_WRIST to listOf(wristPalmForwardSilk, wristWarmWaterWave, wristPrayerLotusTurn, wristThumbTendonRelease),
        BodyRegion.EYE_BROW to listOf(eyeWarmPalming, eyeSlowOrbitMist, eyeBrowCuanzhuSmoothing, eyeHorizonSoftGaze)
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
                shoulderEarMelt.copy(durationSeconds = 60),
                shoulderScapulaSigh.copy(durationSeconds = 60),
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
                lumbarBedsideButterflyFold.copy(durationSeconds = 60),
                lumbarWillowSideBend.copy(durationSeconds = 60),
                thoracicSeatedGentleTwist.copy(durationSeconds = 60),
                neckFengchiNod.copy(durationSeconds = 60),
                eyeWarmPalming.copy(durationSeconds = 60),
                shoulderDoubleInhaleSigh.copy(durationSeconds = 60)
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
            shoulderDoubleInhaleSigh.copy(durationSeconds = 45),
            neckFengchiNod.copy(durationSeconds = 45)
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
                neckThumbFengchiPress.copy(durationSeconds = 40),
                eyeSlowOrbitMist.copy(durationSeconds = 40),
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
                wristPalmForwardSilk.copy(durationSeconds = 40),
                wristWarmWaterWave.copy(durationSeconds = 40),
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
                thoracicSeatedGentleTwist.copy(durationSeconds = 45),
                lumbarWillowSideBend.copy(durationSeconds = 45)
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
        val dayPoses = (0 until posesPerDay).map { slot ->
            pool[(dayIndex * 2 + slot) % pool.size].copy(durationSeconds = secondsPerPose)
        }
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
                    shoulderDoubleInhaleSigh,
                    neckFengchiNod,
                    lumbarBedsideButterflyFold,
                    eyeWarmPalming,
                    lumbarWillowSideBend,
                    neckOcciputCradle,
                    eyeBrowCuanzhuSmoothing,
                    shoulderScapulaSigh
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
