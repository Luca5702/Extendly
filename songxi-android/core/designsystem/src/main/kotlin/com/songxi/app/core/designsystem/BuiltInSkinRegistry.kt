package com.songxi.app.core.designsystem

import androidx.compose.ui.graphics.Color
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import java.time.LocalDateTime

interface SkinPackRegistry {
    val allSkins: List<SongxiSkin>
    fun skinFor(id: SkinId): SongxiSkin
    fun resolveEffectiveSkin(
        selected: SkinId,
        autoNightDuskPine: Boolean,
        hourOfDay: Int
    ): SongxiSkin
}

object BuiltInSkinRegistry : SkinPackRegistry {

    val InkMist: SongxiSkin = SongxiSkin(
        id = SkinId.INK_MIST,
        meta = SkinEditorialTokens(
            titleCn = "水墨远山",
            subtitleCn = "素宣写意",
            fusionBadge = "六分半宋人水墨 · 三分半现代插画",
            greetingPrefix = "远山淡影 · 宜沉肩缓息",
            completionVerse = "一息墨韵散，肩头万事轻。",
            titleLetterSpacingSp = 1.6f
        ),
        palette = SkinPaletteTokens(
            isDark = false,
            canvasPrimary = Color(0xFFF5F1E8),
            canvasSkyWashTop = Color(0xFFEBE5D8),
            surfaceCard = Color(0xFFFAF7F1),
            surfaceCardSubtle = Color(0xFFECE6DA),
            surfaceOverlay = Color(0xFFF5F1E8).copy(alpha = 0.94f),
            borderHairline = Color(0xFFDCD5C7),
            inkPrimary = Color(0xFF262B28),
            inkSecondary = Color(0xFF5C645F),
            inkTertiary = Color(0xFF5E6661),
            accentPrimary = Color(0xFF486356),
            accentPrimarySoft = Color(0xFFDCE5DF),
            accentPrimaryGlow = Color(0xFF84A192),
            accentSecondary = Color(0xFF91674B),
            accentSecondarySoft = Color(0xFFEDE2D8),
            sealVermilion = Color(0xFFB5493A),
            onAccent = Color(0xFFFAF7F1),
            substrateGrainAlpha = 0.035f,
            cardCornerRadiusDp = 24
        ),
        landscape = SkinLandscapeTokens(
            ridgeStyle = RidgeStyle.INK_WASH_MIST,
            farRidgeColor = Color(0xFFCFD6D0),
            midRidgeColor = Color(0xFF9FB0A6),
            ridgeCrestStrokeColor = null,
            mistColor = Color(0xFFF7F4EC),
            mistOpacity = 0.72f,
            celestialType = CelestialType.HAZY_SUN,
            celestialColor = Color(0xFFE3C89E),
            botanicalMotif = BotanicalMotif.PINE_BRANCH
        ),
        illustration = SkinIllustrationTokens(
            figureStyle = FigureStyle.INK_BRUSH_WASH,
            garmentFill = Color(0xFFE6DFD1),
            garmentShadow = Color(0xFFD2C8B6),
            skinToneFill = Color(0xFFEEDBCE),
            hairFill = Color(0xFF2D322F),
            contourStroke = Color(0xFF262B28),
            contourStrokeWidthDp = 2.2f,
            haloGeometry = HaloGeometry.INK_CONCENTRIC_RIPPLE,
            sealShape = SealShape.SQUARE_ZHUWEN,
            sealText = "松息"
        ),
        sensory = SkinSensoryTokens(
            defaultSoundProfile = SoundProfileId.SINGING_BOWL_432,
            baseFrequencyHz = 432f,
            harmonicRatios = listOf(1.0f, 1.50f, 2.74f),
            inhaleDurationMs = 4000,
            exhaleDurationMs = 6000,
            hapticAmplitude = 50
        )
    )

    val QinglvCeladon: SongxiSkin = SongxiSkin(
        id = SkinId.QINGLV_CELADON,
        meta = SkinEditorialTokens(
            titleCn = "青绿山居",
            subtitleCn = "汝瓷重彩",
            fusionBadge = "五分青绿山水 · 五分现代插画",
            greetingPrefix = "青绿山居 · 宜开胸纳气",
            completionVerse = "身如青山立，清风入袖来。",
            titleLetterSpacingSp = 1.4f
        ),
        palette = SkinPaletteTokens(
            isDark = false,
            canvasPrimary = Color(0xFFEEF3EE),
            canvasSkyWashTop = Color(0xFFDFECE6),
            surfaceCard = Color(0xFFF7FAF7),
            surfaceCardSubtle = Color(0xFFE3EDE7),
            surfaceOverlay = Color(0xFFEEF3EE).copy(alpha = 0.94f),
            borderHairline = Color(0xFFCBDAD2),
            inkPrimary = Color(0xFF1E2E2B),
            inkSecondary = Color(0xFF4F635E),
            inkTertiary = Color(0xFF536762),
            accentPrimary = Color(0xFF35685C),
            accentPrimarySoft = Color(0xFFD6E7E1),
            accentPrimaryGlow = Color(0xFF72A89B),
            accentSecondary = Color(0xFF2F5669),
            accentSecondarySoft = Color(0xFFDAE6EC),
            sealVermilion = Color(0xFFB84435),
            onAccent = Color(0xFFF7FAF7),
            substrateGrainAlpha = 0.028f,
            cardCornerRadiusDp = 20
        ),
        landscape = SkinLandscapeTokens(
            ridgeStyle = RidgeStyle.QINGLV_MINERAL_GOLD_RIM,
            farRidgeColor = Color(0xFF9EC0B5),
            midRidgeColor = Color(0xFF5F9486),
            ridgeCrestStrokeColor = Color(0xFFB88655),
            mistColor = Color(0xFFF2F7F4),
            mistOpacity = 0.66f,
            celestialType = CelestialType.GOLDEN_MIST_ORB,
            celestialColor = Color(0xFFD8B074),
            botanicalMotif = BotanicalMotif.BAMBOO_AND_ROCK
        ),
        illustration = SkinIllustrationTokens(
            figureStyle = FigureStyle.EDITORIAL_MINERAL_FLAT,
            garmentFill = Color(0xFFCFE2DC),
            garmentShadow = Color(0xFFB3CEC5),
            skinToneFill = Color(0xFFF0DDD0),
            hairFill = Color(0xFF22302D),
            contourStroke = Color(0xFF1E2E2B),
            contourStrokeWidthDp = 1.5f,
            haloGeometry = HaloGeometry.JADE_CLOUD_AURA,
            sealShape = SealShape.OBLONG_BAIWEN,
            sealText = "山居清息"
        ),
        sensory = SkinSensoryTokens(
            defaultSoundProfile = SoundProfileId.BAMBOO_CHIME_528,
            baseFrequencyHz = 528f,
            harmonicRatios = listOf(1.0f, 2.00f, 3.16f),
            inhaleDurationMs = 4000,
            exhaleDurationMs = 6000,
            hapticAmplitude = 54
        )
    )

    val DuskPine: SongxiSkin = SongxiSkin(
        id = SkinId.DUSK_PINE,
        meta = SkinEditorialTokens(
            titleCn = "暮山听松",
            subtitleCn = "沉香夜境",
            fusionBadge = "五分半暮山烟岚 · 四分半微光插画",
            greetingPrefix = "暮山听松 · 宜敛神松腰",
            completionVerse = "松风吹解带，夜静息自宁。",
            titleLetterSpacingSp = 1.8f
        ),
        palette = SkinPaletteTokens(
            isDark = true,
            canvasPrimary = Color(0xFF19201E),
            canvasSkyWashTop = Color(0xFF212C29),
            surfaceCard = Color(0xFF232D2A),
            surfaceCardSubtle = Color(0xFF1D2623),
            surfaceOverlay = Color(0xFF161C1A).copy(alpha = 0.95f),
            borderHairline = Color(0xFF33413D),
            inkPrimary = Color(0xFFE6E1D6),
            inkSecondary = Color(0xFFA6B2AB),
            inkTertiary = Color(0xFF98A59E),
            accentPrimary = Color(0xFF86AA96),
            accentPrimarySoft = Color(0xFF2B3B35),
            accentPrimaryGlow = Color(0xFF587D6A),
            accentSecondary = Color(0xFFC79A7A),
            accentSecondarySoft = Color(0xFF38302A),
            sealVermilion = Color(0xFFD47566),
            onAccent = Color(0xFF161D1B),
            substrateGrainAlpha = 0.040f,
            cardCornerRadiusDp = 24
        ),
        landscape = SkinLandscapeTokens(
            ridgeStyle = RidgeStyle.DUSK_INDIGO_LAYER,
            farRidgeColor = Color(0xFF263531),
            midRidgeColor = Color(0xFF334842),
            ridgeCrestStrokeColor = Color(0xFF4B655D),
            mistColor = Color(0xFF293733),
            mistOpacity = 0.52f,
            celestialType = CelestialType.CRESCENT_MOON,
            celestialColor = Color(0xFFE4CFA8),
            botanicalMotif = BotanicalMotif.NIGHT_PINE_NEEDLES
        ),
        illustration = SkinIllustrationTokens(
            figureStyle = FigureStyle.MOONLIT_CONTOUR,
            garmentFill = Color(0xFF2E403B),
            garmentShadow = Color(0xFF22302C),
            skinToneFill = Color(0xFFD9C5B4),
            hairFill = Color(0xFF141B19),
            contourStroke = Color(0xFF86AA96),
            contourStrokeWidthDp = 1.8f,
            haloGeometry = HaloGeometry.LUNAR_TIDE_RING,
            sealShape = SealShape.GOURD_XIANZHANG,
            sealText = "听松"
        ),
        sensory = SkinSensoryTokens(
            defaultSoundProfile = SoundProfileId.GUQIN_PINE_396,
            baseFrequencyHz = 396f,
            harmonicRatios = listOf(1.0f, 1.50f, 2.00f, 3.00f),
            inhaleDurationMs = 4000,
            exhaleDurationMs = 6000,
            hapticAmplitude = 38
        )
    )

    val SpringBlossom: SongxiSkin = SongxiSkin(
        id = SkinId.SPRING_BLOSSOM,
        meta = SkinEditorialTokens(
            titleCn = "春涧花影",
            subtitleCn = "暖陶芳菲",
            fusionBadge = "三分半春涧山水 · 六分半草木插画",
            greetingPrefix = "春涧花影 · 宜舒腕温目",
            completionVerse = "眉间春水暖，指下花影轻。",
            titleLetterSpacingSp = 1.2f
        ),
        palette = SkinPaletteTokens(
            isDark = false,
            canvasPrimary = Color(0xFFF8F1EA),
            canvasSkyWashTop = Color(0xFFF2E5DA),
            surfaceCard = Color(0xFFFDF9F5),
            surfaceCardSubtle = Color(0xFFEFE4DA),
            surfaceOverlay = Color(0xFFF8F1EA).copy(alpha = 0.94f),
            borderHairline = Color(0xFFE2D3C6),
            inkPrimary = Color(0xFF322926),
            inkSecondary = Color(0xFF685A54),
            inkTertiary = Color(0xFF6E5F58),
            accentPrimary = Color(0xFF9B5A49),
            accentPrimarySoft = Color(0xFFFAF0EC),
            accentPrimaryGlow = Color(0xFFCE9281),
            accentSecondary = Color(0xFF4E7266),
            accentSecondarySoft = Color(0xFFDCE8E3),
            sealVermilion = Color(0xFFB54639),
            onAccent = Color(0xFFFDF9F5),
            substrateGrainAlpha = 0.032f,
            cardCornerRadiusDp = 26
        ),
        landscape = SkinLandscapeTokens(
            ridgeStyle = RidgeStyle.SPRING_SLOPE_STREAM,
            farRidgeColor = Color(0xFFD7DFD8),
            midRidgeColor = Color(0xFFAEC4B8),
            ridgeCrestStrokeColor = Color(0xFFC99B84),
            mistColor = Color(0xFFFBF6F1),
            mistOpacity = 0.70f,
            celestialType = CelestialType.WARM_MORNING_GLOW,
            celestialColor = Color(0xFFEBBFA9),
            botanicalMotif = BotanicalMotif.PEACH_BLOSSOM_TWIG
        ),
        illustration = SkinIllustrationTokens(
            figureStyle = FigureStyle.BOTANICAL_GOUACHE,
            garmentFill = Color(0xFFEDD5C9),
            garmentShadow = Color(0xFFDBBCAE),
            skinToneFill = Color(0xFFF2DFD3),
            hairFill = Color(0xFF3A2F2C),
            contourStroke = Color(0xFF4A3B36),
            contourStrokeWidthDp = 1.7f,
            haloGeometry = HaloGeometry.PETAL_BLOOM_HALO,
            sealShape = SealShape.ROUND_ZHUWEN,
            sealText = "春山"
        ),
        sensory = SkinSensoryTokens(
            defaultSoundProfile = SoundProfileId.SPRING_RAIN_440,
            baseFrequencyHz = 440f,
            harmonicRatios = listOf(1.0f, 1.68f, 2.50f),
            inhaleDurationMs = 4000,
            exhaleDurationMs = 6000,
            hapticAmplitude = 48
        )
    )

    override val allSkins: List<SongxiSkin> = listOf(
        InkMist,
        QinglvCeladon,
        DuskPine,
        SpringBlossom
    )

    override fun skinFor(id: SkinId): SongxiSkin = when (id) {
        SkinId.INK_MIST -> InkMist
        SkinId.QINGLV_CELADON -> QinglvCeladon
        SkinId.DUSK_PINE -> DuskPine
        SkinId.SPRING_BLOSSOM -> SpringBlossom
    }

    override fun resolveEffectiveSkin(
        selected: SkinId,
        autoNightDuskPine: Boolean,
        hourOfDay: Int
    ): SongxiSkin {
        val normalizedHour = Math.floorMod(hourOfDay, 24)
        return if (autoNightDuskPine && (normalizedHour >= 20 || normalizedHour < 8)) {
            DuskPine
        } else {
            skinFor(selected)
        }
    }

    fun resolveEffectiveSkin(
        selected: SkinId,
        autoNightDuskPine: Boolean,
        localDateTime: LocalDateTime
    ): SongxiSkin = resolveEffectiveSkin(selected, autoNightDuskPine, localDateTime.hour)
}
