package com.songxi.app.core.sensory

data class HapticWaveformSpec(
    val timings: LongArray,
    val amplitudes: IntArray,
    val fallbackOffOnTimings: LongArray
)

interface BreathingHapticEngine {
    fun pulseInhaleWave(maxAmplitude: Int = 50, enabled: Boolean = true)
    fun pulseExhaleWave(maxAmplitude: Int = 50, enabled: Boolean = true)
    fun cancel()
}
