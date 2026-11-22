package com.songxi.app.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongxiSkinContractAndThemeTest {

    @Test
    fun fourBuiltInSkins_alignWithSpecAndForbidPureWhiteOrPureBlack() {
        val skins = BuiltInSkinRegistry.allSkins
        assertEquals(4, skins.size)
        assertEquals(
            listOf(
                SkinId.INK_MIST,
                SkinId.QINGLV_CELADON,
                SkinId.DUSK_PINE,
                SkinId.SPRING_BLOSSOM
            ),
            skins.map { it.id }
        )

        // Exact calibrated token checks per SKIN_AND_SPLASH_SPEC.md section 1.2 & 1.3
        assertEquals(Color(0xFF5E6661), BuiltInSkinRegistry.InkMist.palette.inkTertiary)
        assertEquals(Color(0xFF536762), BuiltInSkinRegistry.QinglvCeladon.palette.inkTertiary)
        assertEquals(Color(0xFF98A59E), BuiltInSkinRegistry.DuskPine.palette.inkTertiary)
        assertEquals(Color(0xFF6E5F58), BuiltInSkinRegistry.SpringBlossom.palette.inkTertiary)

        assertEquals(Color(0xFFFAF0EC), BuiltInSkinRegistry.SpringBlossom.palette.accentPrimarySoft)
        assertEquals(
            listOf(1.0f, 1.50f, 2.00f, 3.00f),
            BuiltInSkinRegistry.DuskPine.sensory.harmonicRatios
        )
        assertEquals(SoundProfileId.SINGING_BOWL_432, BuiltInSkinRegistry.InkMist.sensory.defaultSoundProfile)
        assertEquals(SoundProfileId.BAMBOO_CHIME_528, BuiltInSkinRegistry.QinglvCeladon.sensory.defaultSoundProfile)
        assertEquals(SoundProfileId.GUQIN_PINE_396, BuiltInSkinRegistry.DuskPine.sensory.defaultSoundProfile)
        assertEquals(SoundProfileId.SPRING_RAIN_440, BuiltInSkinRegistry.SpringBlossom.sensory.defaultSoundProfile)

        assertEquals(0f, BuiltInSkinRegistry.InkMist.landscape.ridgeCrestStrokeWidthDp)
        assertEquals(0.9f, BuiltInSkinRegistry.QinglvCeladon.landscape.ridgeCrestStrokeWidthDp)
        assertEquals(0.7f, BuiltInSkinRegistry.DuskPine.landscape.ridgeCrestStrokeWidthDp)
        assertEquals(0.8f, BuiltInSkinRegistry.SpringBlossom.landscape.ridgeCrestStrokeWidthDp)

        val pureWhiteRgb = 0xFFFFFF
        val pureBlackRgb = 0x000000

        for (skin in skins) {
            assertEquals(skin, BuiltInSkinRegistry.skinFor(skin.id))
            assertEquals(skin.meta.titleCn, skin.displayNameCn)
            assertEquals(skin.palette.canvasPrimary, skin.backgroundColor)
            assertEquals(skin.palette.inkPrimary, skin.inkPrimaryColor)
            assertEquals(skin.palette.accentPrimary, skin.accentColor)
            assertEquals(4000, skin.sensory.inhaleDurationMs)
            assertEquals(6000, skin.sensory.exhaleDurationMs)
            assertTrue(skin.meta.greetingPrefix.isNotBlank())
            assertTrue(skin.meta.completionVerse.isNotBlank())
            assertTrue(skin.illustration.sealText.isNotBlank())

            val p = skin.palette
            val l = skin.landscape
            val il = skin.illustration
            val allColors = buildList {
                add("palette.canvasPrimary" to p.canvasPrimary)
                add("palette.canvasSkyWashTop" to p.canvasSkyWashTop)
                add("palette.surfaceCard" to p.surfaceCard)
                add("palette.surfaceCardSubtle" to p.surfaceCardSubtle)
                add("palette.surfaceOverlay" to p.surfaceOverlay)
                add("palette.borderHairline" to p.borderHairline)
                add("palette.inkPrimary" to p.inkPrimary)
                add("palette.inkSecondary" to p.inkSecondary)
                add("palette.inkTertiary" to p.inkTertiary)
                add("palette.accentPrimary" to p.accentPrimary)
                add("palette.accentPrimarySoft" to p.accentPrimarySoft)
                add("palette.accentPrimaryGlow" to p.accentPrimaryGlow)
                add("palette.accentSecondary" to p.accentSecondary)
                add("palette.accentSecondarySoft" to p.accentSecondarySoft)
                add("palette.sealVermilion" to p.sealVermilion)
                add("palette.onAccent" to p.onAccent)
                add("landscape.farRidgeColor" to l.farRidgeColor)
                add("landscape.midRidgeColor" to l.midRidgeColor)
                l.ridgeCrestStrokeColor?.let { add("landscape.ridgeCrestStrokeColor" to it) }
                add("landscape.mistColor" to l.mistColor)
                add("landscape.celestialColor" to l.celestialColor)
                add("illustration.garmentFill" to il.garmentFill)
                add("illustration.garmentShadow" to il.garmentShadow)
                add("illustration.skinToneFill" to il.skinToneFill)
                add("illustration.hairFill" to il.hairFill)
                add("illustration.contourStroke" to il.contourStroke)
            }

            for ((tokenName, color) in allColors) {
                val rgb24 = colorToRgb24(color)
                assertTrue(
                    rgb24 != pureWhiteRgb && rgb24 != pureBlackRgb,
                    "Skin ${skin.id} token $tokenName must not use pure #FFFFFF or #000000, got 0x${rgb24.toString(16)}"
                )
            }
        }
    }

    @Test
    fun wcagContrastRatios_meetAaAndAaaThresholdsAcrossAllFourSkins() {
        for (skin in BuiltInSkinRegistry.allSkins) {
            val p = skin.palette
            val id = skin.id.storageKey

            assertTrue(wcagContrastRatio(p.inkPrimary, p.canvasPrimary) >= 12.5, "$id inkPrimary vs canvasPrimary")
            assertTrue(wcagContrastRatio(p.inkPrimary, p.surfaceCard) >= 10.8, "$id inkPrimary vs surfaceCard")
            assertTrue(wcagContrastRatio(p.inkPrimary, p.surfaceCardSubtle) >= 11.2, "$id inkPrimary vs surfaceCardSubtle")
            assertTrue(wcagContrastRatio(p.inkSecondary, p.canvasPrimary) >= 5.3, "$id inkSecondary vs canvasPrimary")
            assertTrue(wcagContrastRatio(p.inkSecondary, p.surfaceCard) >= 5.6, "$id inkSecondary vs surfaceCard")
            assertTrue(wcagContrastRatio(p.inkTertiary, p.canvasPrimary) >= 5.2, "$id inkTertiary vs canvasPrimary")
            assertTrue(wcagContrastRatio(p.accentPrimary, p.canvasPrimary) >= 4.5, "$id accentPrimary vs canvasPrimary")
            assertTrue(wcagContrastRatio(p.accentPrimary, p.accentPrimarySoft) >= 4.5, "$id accentPrimary vs accentPrimarySoft")
            assertTrue(wcagContrastRatio(p.onAccent, p.accentPrimary) >= 5.0, "$id onAccent vs accentPrimary")
            assertTrue(wcagContrastRatio(p.sealVermilion, p.canvasPrimary) >= 4.5, "$id sealVermilion vs canvasPrimary")
        }
    }

    @Test
    fun resolveEffectiveSkin_enforces20To08AutoNightWindowAndModuloNormalization() {
        for (hour in 0..23) {
            val resolvedOn = BuiltInSkinRegistry.resolveEffectiveSkin(
                selected = SkinId.QINGLV_CELADON,
                autoNightDuskPine = true,
                hourOfDay = hour
            )
            val expectedOn = if (hour >= 20 || hour < 8) {
                BuiltInSkinRegistry.DuskPine
            } else {
                BuiltInSkinRegistry.QinglvCeladon
            }
            assertEquals(expectedOn, resolvedOn, "Hour $hour with autoNightDuskPine=true")

            val resolvedOff = BuiltInSkinRegistry.resolveEffectiveSkin(
                selected = SkinId.SPRING_BLOSSOM,
                autoNightDuskPine = false,
                hourOfDay = hour
            )
            assertEquals(BuiltInSkinRegistry.SpringBlossom, resolvedOff, "Hour $hour with autoNightDuskPine=false")
        }

        // Exact LocalDateTime boundaries (19:59, 20:01, 06:30, 08:00, 09:30)
        assertEquals(
            SkinId.QINGLV_CELADON,
            BuiltInSkinRegistry.resolveEffectiveSkin(
                SkinId.QINGLV_CELADON,
                true,
                LocalDateTime.of(2026, 11, 17, 19, 59)
            ).id
        )
        assertEquals(
            SkinId.DUSK_PINE,
            BuiltInSkinRegistry.resolveEffectiveSkin(
                SkinId.QINGLV_CELADON,
                true,
                LocalDateTime.of(2026, 11, 17, 20, 1)
            ).id
        )
        assertEquals(
            SkinId.DUSK_PINE,
            BuiltInSkinRegistry.resolveEffectiveSkin(
                SkinId.QINGLV_CELADON,
                true,
                LocalDateTime.of(2026, 11, 17, 6, 30)
            ).id
        )
        assertEquals(
            SkinId.QINGLV_CELADON,
            BuiltInSkinRegistry.resolveEffectiveSkin(
                SkinId.QINGLV_CELADON,
                true,
                LocalDateTime.of(2026, 11, 17, 8, 0)
            ).id
        )

        // Out-of-range hour modulo safety
        assertEquals(
            SkinId.DUSK_PINE,
            BuiltInSkinRegistry.resolveEffectiveSkin(SkinId.INK_MIST, true, -3).id
        )
        assertEquals(
            SkinId.INK_MIST,
            BuiltInSkinRegistry.resolveEffectiveSkin(SkinId.INK_MIST, true, 36).id
        )
    }

    @Test
    fun resolveBreathPhase_matches4sInhale6sExhaleAndExtremeInputGuards() {
        val t0 = resolveBreathPhase(0L)
        assertFalse(t0.isExhale)
        assertEquals(0f, t0.phaseProgress, 0.001f)
        assertEquals(0f, t0.expansion, 0.001f)
        assertEquals("吸气 · 轻展", t0.phaseLabelCn)

        val t3900 = resolveBreathPhase(3_900L)
        assertFalse(t3900.isExhale)
        assertEquals("吸气 · 轻展", t3900.phaseLabelCn)
        assertTrue(t3900.expansion > 0.99f)

        val t4000 = resolveBreathPhase(4_000L)
        assertTrue(t4000.isExhale)
        assertEquals(0f, t4000.phaseProgress, 0.001f)
        assertEquals(1f, t4000.expansion, 0.001f)
        assertEquals("呼气 · 沉肩", t4000.phaseLabelCn)

        val t4100 = resolveBreathPhase(4_100L)
        assertTrue(t4100.isExhale)
        assertEquals("呼气 · 沉肩", t4100.phaseLabelCn)

        val t10100 = resolveBreathPhase(10_100L)
        assertFalse(t10100.isExhale)
        assertEquals("吸气 · 轻展", t10100.phaseLabelCn)
        assertTrue(abs(t10100.phaseProgress - 0.025f) < 0.001f)

        // Negative elapsedMs, zero/negative durations, and Int.MAX_VALUE overflow protection
        val negState = resolveBreathPhase(-100L)
        assertTrue(negState.isExhale) // 9900ms into cycle
        val minLongState = resolveBreathPhase(Long.MIN_VALUE)
        assertTrue(minLongState.expansion.isFinite())
        val zeroDurState = resolveBreathPhase(500L, inhaleDurationMs = 0, exhaleDurationMs = -5)
        assertTrue(zeroDurState.expansion.isFinite())
        val maxIntDurState = resolveBreathPhase(1_000L, inhaleDurationMs = Int.MAX_VALUE, exhaleDurationMs = Int.MAX_VALUE)
        assertFalse(maxIntDurState.isExhale)
        assertTrue(maxIntDurState.expansion.isFinite())
    }

    @Test
    fun hitTestBodyTensionRegion_handlesPrimaryBilateralAndAdversarialInputs() {
        val canvasSize = IntSize(300, 380)
        for (region in BodyRegion.entries) {
            val primaryPt = bodyTensionZoneCenterPx(region, canvasSize, useBilateralMirror = false)
            assertEquals(region, hitTestBodyTensionRegion(primaryPt, canvasSize), "Primary anchor for $region")
        }

        // Bilateral mirror hotspots for shoulder and wrist
        val rightShoulder = bodyTensionZoneCenterPx(BodyRegion.TRAPEZIUS_SHOULDER, canvasSize, useBilateralMirror = true)
        assertEquals(BodyRegion.TRAPEZIUS_SHOULDER, hitTestBodyTensionRegion(rightShoulder, canvasSize))
        val rightWrist = bodyTensionZoneCenterPx(BodyRegion.MOUSE_WRIST, canvasSize, useBilateralMirror = true)
        assertEquals(BodyRegion.MOUSE_WRIST, hitTestBodyTensionRegion(rightWrist, canvasSize))

        // Far corner miss & degenerate/non-finite inputs
        assertNull(hitTestBodyTensionRegion(Offset(2f, 2f), canvasSize))
        assertNull(hitTestBodyTensionRegion(Offset(150f, 150f), IntSize(0, 0)))
        assertNull(hitTestBodyTensionRegion(Offset(150f, 150f), IntSize(-20, 100)))
        assertNull(hitTestBodyTensionRegion(Offset(Float.NaN, 120f), canvasSize))
        assertNull(hitTestBodyTensionRegion(Offset(120f, Float.POSITIVE_INFINITY), canvasSize))
        assertNull(hitTestBodyTensionRegion(Offset.Unspecified, canvasSize))
        assertNotNull(bodyTensionZoneCenterPx(BodyRegion.POSTERIOR_NECK, IntSize(0, 0)))
    }

    private fun colorToRgb24(color: Color): Int {
        val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (r shl 16) or (g shl 8) or b
    }
}
