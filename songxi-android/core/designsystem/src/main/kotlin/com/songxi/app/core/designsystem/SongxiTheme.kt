package com.songxi.app.core.designsystem

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songxi.app.core.model.SkinId

/**
 * Bundled subset of SIL Open Font License 1.1 "Noto Serif CJK SC" (思源宋体),
 * guaranteeing authentic Song-dynasty brush-serif Chinese typography across all Android ROMs.
 */
val SongxiEditorialSerifFamily: FontFamily = FontFamily(
    Font(resId = R.font.songxi_songti_regular, weight = FontWeight.Normal),
    Font(resId = R.font.songxi_songti_regular, weight = FontWeight.Medium),
    Font(resId = R.font.songxi_songti_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.songxi_songti_bold, weight = FontWeight.Bold)
)

fun loadSongxiEditorialTypeface(context: Context?, bold: Boolean = false): Typeface {
    val fallbackStyle = if (bold) Typeface.BOLD else Typeface.NORMAL
    if (context == null) return Typeface.create("serif", fallbackStyle)
    val resId = if (bold) R.font.songxi_songti_bold else R.font.songxi_songti_regular
    return runCatching<Typeface> { context.resources.getFont(resId) }
        .getOrNull() ?: Typeface.create("serif", fallbackStyle)
}

val LocalSongxiSkin = staticCompositionLocalOf<SongxiSkin> {
    BuiltInSkinRegistry.skinFor(SkinId.INK_MIST)
}

@Composable
fun SongxiTheme(
    skin: SongxiSkin = BuiltInSkinRegistry.skinFor(SkinId.INK_MIST),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(skin) { buildSongxiColorScheme(skin) }
    val shapes = remember(skin.palette.cardCornerRadiusDp) { buildSongxiShapes(skin) }
    val typography = remember(skin.meta.titleLetterSpacingSp) { buildSongxiTypography(skin) }

    CompositionLocalProvider(LocalSongxiSkin provides skin) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}

private fun buildSongxiColorScheme(skin: SongxiSkin): ColorScheme {
    val p = skin.palette
    val nonBlackScrim = Color(0xFF121715).copy(alpha = 0.52f)
    val onTertiaryColor = if (p.isDark) p.onAccent else p.surfaceCard
    val errorColor = p.sealVermilion
    val onErrorColor = if (p.isDark) p.onAccent else p.surfaceCard

    return if (p.isDark) {
        darkColorScheme(
            primary = p.accentPrimary,
            onPrimary = p.onAccent,
            primaryContainer = p.accentPrimarySoft,
            onPrimaryContainer = p.inkPrimary,
            secondary = p.accentSecondary,
            onSecondary = p.onAccent,
            secondaryContainer = p.accentSecondarySoft,
            onSecondaryContainer = p.inkPrimary,
            tertiary = p.sealVermilion,
            onTertiary = onTertiaryColor,
            tertiaryContainer = p.accentSecondarySoft,
            onTertiaryContainer = p.inkPrimary,
            background = p.canvasPrimary,
            onBackground = p.inkPrimary,
            surface = p.surfaceCard,
            onSurface = p.inkPrimary,
            surfaceVariant = p.surfaceCardSubtle,
            onSurfaceVariant = p.inkSecondary,
            surfaceTint = Color.Transparent,
            outline = p.borderHairline,
            outlineVariant = p.borderHairline.copy(alpha = 0.6f),
            scrim = nonBlackScrim,
            error = errorColor,
            onError = onErrorColor,
            surfaceContainerLowest = p.canvasPrimary,
            surfaceContainerLow = p.surfaceCardSubtle,
            surfaceContainer = p.surfaceCard,
            surfaceContainerHigh = p.surfaceCard,
            surfaceContainerHighest = p.accentPrimarySoft
        )
    } else {
        lightColorScheme(
            primary = p.accentPrimary,
            onPrimary = p.onAccent,
            primaryContainer = p.accentPrimarySoft,
            onPrimaryContainer = p.inkPrimary,
            secondary = p.accentSecondary,
            onSecondary = p.onAccent,
            secondaryContainer = p.accentSecondarySoft,
            onSecondaryContainer = p.inkPrimary,
            tertiary = p.sealVermilion,
            onTertiary = onTertiaryColor,
            tertiaryContainer = p.accentSecondarySoft,
            onTertiaryContainer = p.inkPrimary,
            background = p.canvasPrimary,
            onBackground = p.inkPrimary,
            surface = p.surfaceCard,
            onSurface = p.inkPrimary,
            surfaceVariant = p.surfaceCardSubtle,
            onSurfaceVariant = p.inkSecondary,
            surfaceTint = Color.Transparent,
            outline = p.borderHairline,
            outlineVariant = p.borderHairline.copy(alpha = 0.6f),
            scrim = nonBlackScrim,
            error = errorColor,
            onError = onErrorColor,
            surfaceContainerLowest = p.canvasPrimary,
            surfaceContainerLow = p.surfaceCardSubtle,
            surfaceContainer = p.surfaceCard,
            surfaceContainerHigh = p.surfaceCard,
            surfaceContainerHighest = p.accentPrimarySoft
        )
    }
}

private fun buildSongxiShapes(skin: SongxiSkin): Shapes {
    val cardRadius = skin.palette.cardCornerRadiusDp.coerceIn(4, 48)
    return Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(cardRadius.dp),
        extraLarge = RoundedCornerShape((cardRadius + 4).dp)
    )
}

private fun buildSongxiTypography(skin: SongxiSkin): Typography {
    val rawSpacing = skin.meta.titleLetterSpacingSp
    val safeSpacing = if (rawSpacing.isFinite()) rawSpacing.coerceIn(-2f, 8f) else 1.4f
    val titleSpacing = safeSpacing.sp
    return Typography(
        displayLarge = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            lineHeight = 46.sp,
            letterSpacing = titleSpacing
        ),
        displayMedium = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 40.sp,
            letterSpacing = titleSpacing
        ),
        displaySmall = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp,
            lineHeight = 36.sp,
            letterSpacing = titleSpacing
        ),
        headlineLarge = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 34.sp,
            letterSpacing = titleSpacing
        ),
        headlineMedium = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 30.sp,
            letterSpacing = titleSpacing
        ),
        headlineSmall = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = titleSpacing
        ),
        titleLarge = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 28.sp,
            letterSpacing = titleSpacing
        ),
        titleMedium = TextStyle(
            fontFamily = SongxiEditorialSerifFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (safeSpacing * 0.75f).sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.4.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.3.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.35.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        )
    )
}
