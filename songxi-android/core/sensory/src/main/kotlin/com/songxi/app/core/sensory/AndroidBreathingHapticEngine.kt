package com.songxi.app.core.sensory

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AndroidBreathingHapticEngine(context: Context) : BreathingHapticEngine {

    private val appContext: Context = context.applicationContext ?: context

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
                ?: appContext.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun pulseInhaleWave(maxAmplitude: Int, enabled: Boolean) {
        if (!enabled) return
        emitWaveform(BreathingHapticEnvelopeBuilder.buildInhaleWave(maxAmplitude))
    }

    override fun pulseExhaleWave(maxAmplitude: Int, enabled: Boolean) {
        if (!enabled) return
        emitWaveform(BreathingHapticEnvelopeBuilder.buildExhaleWave(maxAmplitude))
    }

    override fun pulseHalfwaySwitchCue(maxAmplitude: Int, enabled: Boolean) {
        if (!enabled) return
        emitWaveform(BreathingHapticEnvelopeBuilder.buildHalfwaySwitchDoubleTap(maxAmplitude))
    }

    override fun pulseSafeReturnCue(maxAmplitude: Int, enabled: Boolean) {
        if (!enabled) return
        emitWaveform(BreathingHapticEnvelopeBuilder.buildSafeReturnSoftPulse(maxAmplitude))
    }

    override fun cancel() {
        runCatching { vibrator?.cancel() }
    }

    private fun emitWaveform(spec: HapticWaveformSpec) {
        val vib = vibrator ?: return
        if (!runCatching { vib.hasVibrator() }.getOrDefault(false)) return
        val hasAmp = runCatching { vib.hasAmplitudeControl() }.getOrDefault(false)
        val effect = if (hasAmp) {
            VibrationEffect.createWaveform(spec.timings, spec.amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(spec.fallbackOffOnTimings, -1)
        }
        runCatching { vib.vibrate(effect) }
    }
}
