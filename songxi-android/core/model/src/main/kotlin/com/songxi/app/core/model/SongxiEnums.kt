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
