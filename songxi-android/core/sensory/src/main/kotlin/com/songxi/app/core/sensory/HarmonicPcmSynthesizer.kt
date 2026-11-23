package com.songxi.app.core.sensory

import com.songxi.app.core.model.SoundProfileId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object HarmonicPcmSynthesizer {
    const val DEFAULT_SAMPLE_RATE_HZ: Int = 24_000
    private const val TWO_PI: Double = 2.0 * PI
    private const val MASTER_GAIN: Double = 0.36
    private const val RAIN_SEED: Int = 0x534F4E47

    private class AcousticVoice(
        val fundamentalHz: Double,
        val ratios: DoubleArray,
        val weights: DoubleArray,
        val decayTausSec: DoubleArray,
        val baseAttackMs: Int
    )

    private fun voiceFor(profile: SoundProfileId): AcousticVoice? = when (profile) {
        SoundProfileId.SINGING_BOWL_432 -> AcousticVoice(
            fundamentalHz = 432.0,
            ratios = doubleArrayOf(1.0, 1.5, 2.74),
            weights = doubleArrayOf(1.0, 0.42, 0.18),
            decayTausSec = doubleArrayOf(1.45, 1.05, 0.65),
            baseAttackMs = 200
        )
        SoundProfileId.BAMBOO_CHIME_528 -> AcousticVoice(
            fundamentalHz = 528.0,
            ratios = doubleArrayOf(1.0, 2.0, 3.16),
            weights = doubleArrayOf(1.0, 0.36, 0.22),
            decayTausSec = doubleArrayOf(0.85, 0.42, 0.20),
            baseAttackMs = 85
        )
        SoundProfileId.GUQIN_PINE_396 -> AcousticVoice(
            fundamentalHz = 396.0,
            ratios = doubleArrayOf(1.0, 1.5, 2.0, 3.0),
            weights = doubleArrayOf(1.0, 0.48, 0.34, 0.16),
            decayTausSec = doubleArrayOf(1.55, 1.20, 0.90, 0.55),
            baseAttackMs = 110
        )
        SoundProfileId.SPRING_RAIN_440 -> AcousticVoice(
            fundamentalHz = 440.0,
            ratios = doubleArrayOf(1.0, 1.68, 2.5),
            weights = doubleArrayOf(1.0, 0.38, 0.19),
            decayTausSec = doubleArrayOf(1.25, 0.80, 0.48),
            baseAttackMs = 130
        )
        SoundProfileId.SILENT_HAPTIC_ONLY -> null
    }

    fun synthesizeChordPcm(
        soundProfile: SoundProfileId,
        cueType: HarmonicCueType,
        muted: Boolean = false,
        sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ
    ): ShortArray {
        if (muted || sampleRateHz <= 0) return ShortArray(0)
        val voice = voiceFor(soundProfile) ?: return ShortArray(0)

        val totalSamples = (cueType.durationMs.toLong() * sampleRateHz / 1000L).toInt()
        if (totalSamples <= 4) return ShortArray(0)

        val pcm = ShortArray(totalSamples)
        val dt = 1.0 / sampleRateHz.toDouble()
        val f0 = voice.fundamentalHz

        val globalAttackMs = when (cueType) {
            HarmonicCueType.INHALE -> min(250, voice.baseAttackMs + 30)
            HarmonicCueType.EXHALE -> voice.baseAttackMs
            HarmonicCueType.POSE_TRANSITION -> min(120, voice.baseAttackMs)
            HarmonicCueType.SKIN_PREVIEW -> voice.baseAttackMs
            HarmonicCueType.HALFWAY_SWITCH -> min(65, voice.baseAttackMs)
            HarmonicCueType.SAFE_RETURN -> min(95, voice.baseAttackMs)
        }
        val attackSamples = ((globalAttackMs * sampleRateHz) / 1000).coerceIn(1, totalSamples / 2)
        val releaseSamples = ((120 * sampleRateHz) / 1000).coerceIn(1, totalSamples / 2)
        val releaseStart = totalSamples - releaseSamples

        // 1-pole 950 Hz RC low-pass filter coefficient for SPRING_RAIN_440 mist layer
        val rc = 1.0 / (TWO_PI * 950.0)
        val lpAlpha = dt / (rc + dt)
        var rainState = RAIN_SEED
        var lpPrev = 0.0
        var dcBlockerPrevIn = 0.0
        var dcBlockerPrevOut = 0.0

        val weightNorm = voice.weights.sum().coerceAtLeast(1.0)

        for (i in 0 until totalSamples) {
            val t = i * dt

            // Global click-free onset & tail raised-cosine windows
            val globalEnv = when {
                i == 0 || i == totalSamples - 1 -> 0.0
                i < attackSamples -> 0.5 * (1.0 - cos(PI * i.toDouble() / attackSamples.toDouble()))
                i >= releaseStart -> {
                    val relIdx = (i - releaseStart).toDouble() / (releaseSamples - 1).coerceAtLeast(1).toDouble()
                    0.5 * (1.0 + cos(PI * relIdx.coerceIn(0.0, 1.0)))
                }
                else -> 1.0
            }

            // Base harmonic partials
            var harmonicSum = 0.0
            for (p in voice.ratios.indices) {
                val freq = f0 * voice.ratios[p]
                val decay = exp(-t / voice.decayTausSec[p])
                val phaseMod = if (soundProfile == SoundProfileId.GUQIN_PINE_396 && t > 0.20) {
                    val vibEnv = ((t - 0.20) / 0.25).coerceIn(0.0, 1.0)
                    0.018 * vibEnv * sin(TWO_PI * 4.2 * (t - 0.20))
                } else {
                    0.0
                }
                val ampShimmer = if (soundProfile == SoundProfileId.SINGING_BOWL_432 && p <= 1) {
                    1.0 + 0.08 * sin(TWO_PI * 1.6 * t)
                } else {
                    1.0
                }
                harmonicSum += voice.weights[p] * decay * ampShimmer * sin(TWO_PI * freq * t + phaseMod)
            }
            var sample = harmonicSum / weightNorm

            // Cue-specific musical voicing
            when (cueType) {
                HarmonicCueType.INHALE -> {
                    if (t > 0.16) {
                        val dtCue = t - 0.16
                        val swell = raisedCosineOnset(dtCue, 0.24)
                        val fifth = 0.34 * swell * exp(-dtCue / 1.35) * sin(TWO_PI * (1.5 * f0) * dtCue)
                        sample += fifth / weightNorm
                    }
                }
                HarmonicCueType.EXHALE -> {
                    if (t > 0.12) {
                        val dtCue = t - 0.12
                        val sigh = raisedCosineOnset(dtCue, 0.22)
                        val lowerFourth = 0.38 * sigh * exp(-dtCue / 1.75) * sin(TWO_PI * (0.75 * f0) * dtCue)
                        sample += lowerFourth / weightNorm
                    }
                }
                HarmonicCueType.POSE_TRANSITION -> {
                    if (t > 0.26) {
                        val dtNote2 = t - 0.26
                        val note2Env = raisedCosineOnset(dtNote2, 0.085) * exp(-dtNote2 / 0.72)
                        val note2 = 0.85 * note2Env * (
                            sin(TWO_PI * (1.25 * f0) * dtNote2) +
                                0.32 * sin(TWO_PI * (2.5 * f0) * dtNote2)
                            )
                        sample += note2 / weightNorm
                    }
                }
                HarmonicCueType.SKIN_PREVIEW -> {
                    val previewFifth = 0.28 * exp(-t / 1.10) * sin(TWO_PI * (1.5 * f0) * t)
                    sample += previewFifth / weightNorm
                }
                HarmonicCueType.HALFWAY_SWITCH -> {
                    // Gentle double wood-chime tap: second ascending fourth note at +0.18s
                    if (t > 0.18) {
                        val dtTap2 = t - 0.18
                        val tap2Env = raisedCosineOnset(dtTap2, 0.045) * exp(-dtTap2 / 0.36)
                        val tap2 = 0.78 * tap2Env * (
                            sin(TWO_PI * (1.333 * f0) * dtTap2) +
                                0.25 * sin(TWO_PI * (2.666 * f0) * dtTap2)
                            )
                        sample += tap2 / weightNorm
                    }
                }
                HarmonicCueType.SAFE_RETURN -> {
                    // Warm settling sub-octave bell signaling calm return to neutral
                    val settleBell = 0.42 * exp(-t / 0.55) * sin(TWO_PI * (0.5 * f0) * t)
                    sample += settleBell / weightNorm
                }
            }

            // Deterministic filtered valley rain mist for SPRING_RAIN_440
            if (soundProfile == SoundProfileId.SPRING_RAIN_440) {
                rainState = xorshift32(rainState)
                val white = rainState.toDouble() / Int.MAX_VALUE.toDouble()
                lpPrev += lpAlpha * (white - lpPrev)
                // Gentle 60 Hz high-pass DC blocker so rain mist stays warm mid-low band (250-950 Hz)
                val hpOut = lpPrev - dcBlockerPrevIn + 0.985 * dcBlockerPrevOut
                dcBlockerPrevIn = lpPrev
                dcBlockerPrevOut = hpOut
                val mistEnv = exp(-t / 2.2)
                sample += 0.16 * mistEnv * hpOut
            }

            val scaled = (sample * globalEnv * MASTER_GAIN).coerceIn(-0.98, 0.98)
            pcm[i] = (scaled * 32767.0).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        pcm[0] = 0
        pcm[totalSamples - 1] = 0
        return pcm
    }

    private fun raisedCosineOnset(elapsedSec: Double, rampSec: Double): Double {
        if (elapsedSec <= 0.0) return 0.0
        if (elapsedSec >= rampSec) return 1.0
        return 0.5 * (1.0 - cos(PI * (elapsedSec / rampSec)))
    }

    private fun xorshift32(state: Int): Int {
        var x = if (state == 0) RAIN_SEED else state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        return x
    }
}
