package com.songxi.app.core.sensory

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

object BreathingHapticEnvelopeBuilder {

    fun buildInhaleWave(maxAmplitude: Int = 50): HapticWaveformSpec {
        val clampedMax = maxAmplitude.coerceIn(1, 255)
        val steps = 20
        val timings = LongArray(steps) { 200L }
        val amplitudes = IntArray(steps)
        amplitudes[0] = 0
        amplitudes[steps - 1] = 0

        for (i in 1..18) {
            val u = i.toDouble() / 19.0
            val env = if (u <= 0.75) {
                0.5 * (1.0 - cos(PI * (u / 0.75)))
            } else {
                0.5 * (1.0 + cos(PI * ((u - 0.75) / 0.25)))
            }
            amplitudes[i] = (clampedMax * env).roundToInt().coerceIn(1, 255)
        }

        // Exact sum = 4000L (0 + 18 + 982 + 26 + 974 + 34 + 966 + 24 + 976 = 4000)
        val fallbackTimings = longArrayOf(0L, 18L, 982L, 26L, 974L, 34L, 966L, 24L, 976L)
        return HapticWaveformSpec(
            timings = timings,
            amplitudes = amplitudes,
            fallbackOffOnTimings = fallbackTimings
        )
    }

    fun buildExhaleWave(maxAmplitude: Int = 50): HapticWaveformSpec {
        val clampedMax = maxAmplitude.coerceIn(1, 255)
        val crestMax = (clampedMax * 0.86).roundToInt().coerceIn(1, 255)
        val steps = 24
        val timings = LongArray(steps) { 250L }
        val amplitudes = IntArray(steps)
        amplitudes[0] = 0
        amplitudes[22] = 0
        amplitudes[23] = 0

        for (i in 1..21) {
            val u = i.toDouble() / 23.0
            val env = if (u <= 0.20) {
                0.5 * (1.0 - cos(PI * (u / 0.20)))
            } else {
                (0.5 * (1.0 + cos(PI * ((u - 0.20) / 0.80)))).pow(1.35)
            }
            amplitudes[i] = (crestMax * env).roundToInt().coerceIn(0, 255)
        }

        // Exact sum = 6000L (0 + 28 + 1472 + 20 + 1780 + 14 + 2686 = 6000)
        val fallbackTimings = longArrayOf(0L, 28L, 1472L, 20L, 1780L, 14L, 2686L)
        return HapticWaveformSpec(
            timings = timings,
            amplitudes = amplitudes,
            fallbackOffOnTimings = fallbackTimings
        )
    }

    /**
     * Distinct gentle double-tap cue for Eyes-Free bilateral halfway transitions ("温和换边").
     */
    fun buildHalfwaySwitchDoubleTap(maxAmplitude: Int = 58): HapticWaveformSpec {
        val clamped = maxAmplitude.coerceIn(8, 255)
        val firstTap = (clamped * 0.82).roundToInt().coerceIn(8, 255)
        return HapticWaveformSpec(
            timings = longArrayOf(0L, 42L, 76L, 52L),
            amplitudes = intArrayOf(0, firstTap, 0, clamped),
            fallbackOffOnTimings = longArrayOf(0L, 38L, 78L, 46L)
        )
    }

    /**
     * Soft single settling pulse signaling the final 5s safe-return stage ("缓慢回正").
     */
    fun buildSafeReturnSoftPulse(maxAmplitude: Int = 44): HapticWaveformSpec {
        val clamped = maxAmplitude.coerceIn(6, 255)
        return HapticWaveformSpec(
            timings = longArrayOf(0L, 64L),
            amplitudes = intArrayOf(0, clamped),
            fallbackOffOnTimings = longArrayOf(0L, 48L)
        )
    }
}
