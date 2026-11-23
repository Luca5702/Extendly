package com.songxi.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkinId(
    val storageKey: String,
    val labelCn: String
) {
    @SerialName("ink_mist")
    INK_MIST("ink_mist", "水墨远山"),

    @SerialName("qinglv_celadon")
    QINGLV_CELADON("qinglv_celadon", "青绿山居"),

    @SerialName("dusk_pine")
    DUSK_PINE("dusk_pine", "暮山听松"),

    @SerialName("spring_blossom")
    SPRING_BLOSSOM("spring_blossom", "春涧花影");

    companion object {
        val DEFAULT: SkinId = INK_MIST

        fun fromStorageKey(key: String?): SkinId =
            entries.firstOrNull {
                it.storageKey == key || it.name.equals(key, ignoreCase = true)
            } ?: DEFAULT
    }
}

@Serializable
enum class PostureScene(
    val key: String,
    val labelCn: String,
    val badgeCn: String
) {
    @SerialName("desk")
    DESK_SEATED("desk", "工位坐姿", "邻座无感 · 坐姿微幅"),

    @SerialName("tea")
    TEA_BREAK_STANDING("tea", "桌边茶歇", "茶歇桌边 · 免脱鞋"),

    @SerialName("bed")
    EVENING_BEDSIDE("bed", "晚间床沿", "晚间床沿 · 助眠柔舒");

    companion object {
        fun fromKey(key: String?): PostureScene =
            entries.firstOrNull {
                it.key == key || it.name.equals(key, ignoreCase = true)
            } ?: DESK_SEATED
    }
}

@Serializable
enum class BodyRegion(
    val key: String,
    val labelCn: String,
    val shortLabelCn: String
) {
    @SerialName("neck")
    POSTERIOR_NECK("neck", "后颈风池", "风池"),

    @SerialName("shoulder")
    TRAPEZIUS_SHOULDER("shoulder", "双肩斜方", "双肩"),

    @SerialName("thoracic")
    THORACIC_CHEST("thoracic", "胸椎圆肩", "胸椎"),

    @SerialName("lumbar")
    LOWER_LUMBAR("lumbar", "下腰久坐", "下腰"),

    @SerialName("wrist")
    MOUSE_WRIST("wrist", "手腕鼠标侧", "护腕"),

    @SerialName("eye")
    EYE_BROW("eye", "眼周眉心", "温目");

    companion object {
        fun fromKey(key: String?): BodyRegion? =
            entries.firstOrNull {
                it.key == key || it.name.equals(key, ignoreCase = true)
            }
    }
}

@Serializable
enum class SoundProfileId(
    val key: String,
    val labelCn: String
) {
    @SerialName("bowl")
    SINGING_BOWL_432("bowl", "温润颂钵"),

    @SerialName("bamboo")
    BAMBOO_CHIME_528("bamboo", "空谷竹磬"),

    @SerialName("guqin")
    GUQIN_PINE_396("guqin", "松风琴韵"),

    @SerialName("rain")
    SPRING_RAIN_440("rain", "春涧细雨"),

    @SerialName("silent")
    SILENT_HAPTIC_ONLY("silent", "静默仅微震");

    companion object {
        fun fromKey(key: String?): SoundProfileId =
            entries.firstOrNull {
                it.key == key || it.name.equals(key, ignoreCase = true)
            } ?: SINGING_BOWL_432

        fun defaultForSkin(skinId: SkinId): SoundProfileId = when (skinId) {
            SkinId.INK_MIST -> SINGING_BOWL_432
            SkinId.QINGLV_CELADON -> BAMBOO_CHIME_528
            SkinId.DUSK_PINE -> GUQIN_PINE_396
            SkinId.SPRING_BLOSSOM -> SPRING_RAIN_440
        }
    }
}

@Serializable
enum class SomaticFeeling(
    val key: String,
    val labelCn: String
) {
    @SerialName("light")
    LIGHT_NECK_SHOULDER("light", "肩颈轻快"),

    @SerialName("warm")
    WARM_OPEN("warm", "温热舒展"),

    @SerialName("calm")
    CALM_PEACEFUL("calm", "心神安宁");

    companion object {
        fun fromKey(key: String?): SomaticFeeling? =
            entries.firstOrNull {
                it.key == key || it.name.equals(key, ignoreCase = true)
            }
    }
}

@Serializable
enum class SplashArtworkStyle {
    CELADON_TEA,
    PINE_INCENSE,
    WOOD_DESK_STAND
}

/**
 * Physical-therapy & kinesiology mechanism classification for each micro-movement.
 */
@Serializable
enum class StretchModality(
    val key: String,
    val badgeCn: String,
    val mechanismSummaryCn: String
) {
    @SerialName("mobility")
    MOBILITY_CARS("mobility", "滑液唤醒", "关节微幅受控流动 · 温润滑膜避免冷拉"),

    @SerialName("pnf")
    MICRO_PNF("pnf", "微抗阻PNF", "吸气20%等长轻抗 · 呼气触发自发抑制反射"),

    @SerialName("fascial")
    STATIC_FASCIAL("fascial", "筋膜延展", "恒定微温低张力 · 唤醒高尔基腱柔和松解"),

    @SerialName("nerve_glide")
    NERVE_GLIDING("nerve_glide", "神经滑动", "节律温和滑移 · 疏解腕管与上肢神经紧绷"),

    @SerialName("oculomotor")
    OCULOMOTOR_RELEASE("oculomotor", "眼颈解耦", "视轴远近柔转 · 经眼颈反射松开枕下肌群")
}

/**
 * 3-stage rehabilitation prescription sequencing role:
 * 1. [WARMUP_UNBIND] -> synovial lubrication & breath unbinding
 * 2. [TARGET_RELEASE] -> focal PNF / myofascial / neural release
 * 3. [POSTURAL_INTEGRATION] -> antagonist stabilization & parasympathetic down-regulation
 */
@Serializable
enum class PrescriptionPhaseRole(
    val order: Int,
    val shortLabelCn: String,
    val stageTitleCn: String
) {
    @SerialName("warmup")
    WARMUP_UNBIND(0, "解绑", "一阶 · 滑液解绑"),

    @SerialName("release")
    TARGET_RELEASE(1, "深舒", "二阶 · 靶向松解"),

    @SerialName("integrate")
    POSTURAL_INTEGRATION(2, "归位", "三阶 · 归位沉降")
}

/**
 * Optimal anatomical camera plane so sagittal movements (chin tuck, cat-cow, pelvic tilt)
 * clearly display anterior-posterior spinal and cervical alignment.
 */
@Serializable
enum class AnatomicalViewPlane(
    val key: String,
    val labelCn: String
) {
    @SerialName("coronal")
    CORONAL_FRONT("coronal", "正视导引"),

    @SerialName("sagittal")
    SAGITTAL_SIDE("sagittal", "侧视矢状")
}

/**
 * Pre-session bilateral tension asymmetry preference:
 * allocates 60% : 40% dwell time starting on the tighter side for unilateral/bilateral poses.
 */
@Serializable
enum class LateralAsymmetry(
    val key: String,
    val labelCn: String,
    val ratioBadgeCn: String,
    val primarySideSign: Int,
    val firstHalfRatio: Float
) {
    @SerialName("balanced")
    BALANCED("balanced", "双侧均衡", "5:5", -1, 0.50f),

    @SerialName("left")
    LEFT_TIGHTER("left", "左侧偏紧", "左6:右4", -1, 0.60f),

    @SerialName("right")
    RIGHT_TIGHTER("right", "右侧偏紧", "右6:左4", +1, 0.60f);

    companion object {
        fun fromKey(key: String?): LateralAsymmetry =
            entries.firstOrNull {
                it.key == key || it.name.equals(key, ignoreCase = true)
            } ?: BALANCED
    }
}

/**
 * 4-stage safe intra-pose coaching timeline preventing abrupt rebound strain.
 */
enum class PoseTimelineStage(
    val labelCn: String
) {
    SETUP("入式摆位"),
    DEEPEN_HOLD("随息深化"),
    HALFWAY_TRANSITION("温和换边"),
    SAFE_RETURN("缓慢回正")
}
