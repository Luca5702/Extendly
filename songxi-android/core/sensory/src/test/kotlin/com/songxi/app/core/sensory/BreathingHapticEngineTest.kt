package com.songxi.app.core.sensory

import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreathingHapticEngineTest {

    private lateinit var context: Context
    private lateinit var shadowVibrator: ShadowVibrator
    private lateinit var engine: AndroidBreathingHapticEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val vibrator = context.getSystemService(Vibrator::class.java)
        shadowVibrator = Shadows.shadowOf(vibrator)
        shadowVibrator.setHasVibrator(true)
        engine = AndroidBreathingHapticEngine(context)
    }

    @Test
    fun amplitudeControlledWaveformsMatch4sInhaleAnd6sExhalePhysics() {
        shadowVibrator.setHasAmplitudeControl(true)

        engine.pulseInhaleWave(maxAmplitude = 54, enabled = true)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(4_000L, shadowVibrator.milliseconds)
        assertEquals(20, shadowVibrator.pattern.size)
        assertEquals(4_000L, shadowVibrator.pattern.sum())
        assertEquals(-1, shadowVibrator.repeat)

        engine.pulseExhaleWave(maxAmplitude = 38, enabled = true)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(6_000L, shadowVibrator.milliseconds)
        assertEquals(24, shadowVibrator.pattern.size)
        assertEquals(6_000L, shadowVibrator.pattern.sum())
        assertEquals(-1, shadowVibrator.repeat)

        val lowClamped = BreathingHapticEnvelopeBuilder.buildInhaleWave(-10)
        assertEquals(0, lowClamped.amplitudes.first())
        assertEquals(0, lowClamped.amplitudes.last())
        assertEquals(1, lowClamped.amplitudes.max())

        val highClamped = BreathingHapticEnvelopeBuilder.buildInhaleWave(500)
        assertEquals(0, highClamped.amplitudes.first())
        assertEquals(0, highClamped.amplitudes.last())
        assertEquals(255, highClamped.amplitudes.max())
    }

    @Test
    fun nonAmplitudeControlledFallbackPreservesExact4sAnd6sDurations() {
        shadowVibrator.setHasAmplitudeControl(false)

        engine.pulseInhaleWave(maxAmplitude = 50, enabled = true)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(4_000L, shadowVibrator.milliseconds)
        assertTrue(shadowVibrator.pattern.size > 1)
        assertEquals(4_000L, shadowVibrator.pattern.sum())

        engine.pulseExhaleWave(maxAmplitude = 50, enabled = true)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(6_000L, shadowVibrator.milliseconds)
        assertTrue(shadowVibrator.pattern.size > 1)
        assertEquals(6_000L, shadowVibrator.pattern.sum())
    }

    @Test
    fun disabledNoHardwareAndCancelBehaveCleanly() {
        shadowVibrator.setHasAmplitudeControl(true)

        engine.pulseInhaleWave(maxAmplitude = 50, enabled = false)
        assertFalse(shadowVibrator.isVibrating)

        shadowVibrator.setHasVibrator(false)
        engine.pulseInhaleWave(maxAmplitude = 50, enabled = true)
        assertFalse(shadowVibrator.isVibrating)

        shadowVibrator.setHasVibrator(true)
        engine.pulseInhaleWave(maxAmplitude = 50, enabled = true)
        assertTrue(shadowVibrator.isVibrating)

        engine.cancel()
        assertTrue(shadowVibrator.isCancelled)
        assertFalse(shadowVibrator.isVibrating)
    }
}
