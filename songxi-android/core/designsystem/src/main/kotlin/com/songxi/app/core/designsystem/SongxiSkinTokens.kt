package com.songxi.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import kotlin.math.max
import kotlin.math.min

enum class RidgeStyle {
    INK_WASH_MIST,
    QINGLV_MINERAL_GOLD_RIM,
    DUSK_INDIGO_LAYER,
    SPRING_SLOPE_STREAM
}

enum class CelestialType {
    HAZY_SUN,
    GOLDEN_MIST_ORB,
    CRESCENT_MOON,
    WARM_MORNING_GLOW
}

enum class BotanicalMotif {
    PINE_BRANCH,
    BAMBOO_AND_ROCK,
    NIGHT_PINE_NEEDLES,
    PEACH_BLOSSOM_TWIG
}

enum class FigureStyle {
    INK_BRUSH_WASH,
    EDITORIAL_MINERAL_FLAT,
    MOONLIT_CONTOUR,
    BOTANICAL_GOUACHE
}

enum class HaloGeometry {
    INK_CONCENTRIC_RIPPLE,
    JADE_CLOUD_AURA,
    LUNAR_TIDE_RING,
    PETAL_BLOOM_HALO
}

enum class SealShape {
    SQUARE_ZHUWEN,
    OBLONG_BAIWEN,
    GOURD_XIANZHANG,
    ROUND_ZHUWEN
}

@Immutable
data class SkinPaletteTokens(
    val isDark: Boolean,
    val canvasPrimary: Color,
    val canvasSkyWashTop: Color,
    val surfaceCard: Color,
    val surfaceCardSubtle: Color,
    val surfaceOverlay: Color,
    val borderHairline: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val accentPrimary: Color,
    val accentPrimarySoft: Color,
    val accentPrimaryGlow: Color,
    val accentSecondary: Color,
    val accentSecondarySoft: Color,
    val sealVermilion: Color,
    val onAccent: Color,
    val substrateGrainAlpha: Float,
    val cardCornerRadiusDp: Int
)

@Immutable
data class SkinLandscapeTokens(
    val ridgeStyle: RidgeStyle,
    val farRidgeColor: Color,
    val midRidgeColor: Color,
    val ridgeCrestStrokeColor: Color?,
    val mistColor: Color,
    val mistOpacity: Float,
    val celestialType: CelestialType,
    val celestialColor: Color,
    val botanicalMotif: BotanicalMotif
) {
    val ridgeCrestStrokeWidthDp: Float
        get() = when (ridgeStyle) {
            RidgeStyle.INK_WASH_MIST -> 0f
            RidgeStyle.QINGLV_MINERAL_GOLD_RIM -> 0.9f
            RidgeStyle.DUSK_INDIGO_LAYER -> 0.7f
            RidgeStyle.SPRING_SLOPE_STREAM -> 0.8f
        }
}

@Immutable
data class SkinIllustrationTokens(
    val figureStyle: FigureStyle,
    val garmentFill: Color,
    val garmentShadow: Color,
    val skinToneFill: Color,
    val hairFill: Color,
    val contourStroke: Color,
    val contourStrokeWidthDp: Float,
    val haloGeometry: HaloGeometry,
    val sealShape: SealShape,
    val sealText: String
)

@Immutable
data class SkinEditorialTokens(
    val titleCn: String,
    val subtitleCn: String,
    val fusionBadge: String,
    val greetingPrefix: String,
    val completionVerse: String,
    val titleLetterSpacingSp: Float
)

@Immutable
data class SkinSensoryTokens(
    val defaultSoundProfile: SoundProfileId,
    val baseFrequencyHz: Float,
    val harmonicRatios: List<Float>,
    val inhaleDurationMs: Int = 4000,
    val exhaleDurationMs: Int = 6000,
    val hapticAmplitude: Int
)

@Immutable
data class SongxiSkin(
    val id: SkinId,
    val meta: SkinEditorialTokens,
    val palette: SkinPaletteTokens,
    val landscape: SkinLandscapeTokens,
    val illustration: SkinIllustrationTokens,
    val sensory: SkinSensoryTokens
) {
    // Compatibility bridge properties for M1/M5 app shell consumers.
    val displayNameCn: String get() = meta.titleCn
    val backgroundColor: Color get() = palette.canvasPrimary
    val inkPrimaryColor: Color get() = palette.inkPrimary
    val accentColor: Color get() = palette.accentPrimary
}

/**
 * Computes WCAG 2.1 relative luminance contrast ratio `(L_max + 0.05) / (L_min + 0.05)`.
 */
fun wcagContrastRatio(foreground: Color, background: Color): Double {
    val fgLum = foreground.luminance().toDouble()
    val bgLum = background.luminance().toDouble()
    val lighter = max(fgLum, bgLum)
    val darker = min(fgLum, bgLum)
    return (lighter + 0.05) / (darker + 0.05)
}
