package com.songxi.app.core.sensory

import com.songxi.app.core.model.SoundProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlindOperationSensoryTest {

    @Test
    fun recordingVoiceCueEngineRespectsEnabledAndMutedFlags() {
        val voice = RecordingVoiceCueEngine()
        voice.speakCue("第 1 式，收颌微点头。", enabled = true, muted = false)
        voice.speakCue("静默时不播报", enabled = true, muted = true)
        voice.speakCue("关闭口令时不播报", enabled = false, muted = false)
        voice.speakCue("半程已至，请温和换边。", enabled = true, muted = false)

        assertEquals(
            listOf("第 1 式，收颌微点头。", "半程已至，请温和换边。"),
            voice.spokenCueHistory.value
        )

        voice.release()
        assertTrue(voice.spokenCueHistory.value.isEmpty())

        NoOpVoiceCueEngine.speakCue("无副作用口令", enabled = true, muted = false)
        assertTrue(NoOpVoiceCueEngine.spokenCueHistory.value.isEmpty())
    }

    @Test
    fun halfwayDoubleTapAndSafeReturnEnvelopesAndHarmonicChimesAreNonSilent() {
        val halfwayTap = BreathingHapticEnvelopeBuilder.buildHalfwaySwitchDoubleTap(maxAmplitude = 60)
        assertEquals(4, halfwayTap.timings.size)
        assertEquals(0, halfwayTap.amplitudes[0])
        assertEquals(0, halfwayTap.amplitudes[2])
        assertTrue(halfwayTap.amplitudes[1] in 8..255)
        assertTrue(halfwayTap.amplitudes[3] > halfwayTap.amplitudes[1])

        val safeReturnPulse = BreathingHapticEnvelopeBuilder.buildSafeReturnSoftPulse(maxAmplitude = 44)
        assertEquals(2, safeReturnPulse.timings.size)
        assertEquals(44, safeReturnPulse.amplitudes[1])

        val halfwayPcm = HarmonicPcmSynthesizer.synthesizeChordPcm(
            soundProfile = SoundProfileId.BAMBOO_CHIME_528,
            cueType = HarmonicCueType.HALFWAY_SWITCH,
            muted = false
        )
        val safeReturnPcm = HarmonicPcmSynthesizer.synthesizeChordPcm(
            soundProfile = SoundProfileId.GUQIN_PINE_396,
            cueType = HarmonicCueType.SAFE_RETURN,
            muted = false
        )
        assertTrue(halfwayPcm.any { it.toInt() != 0 })
        assertTrue(safeReturnPcm.any { it.toInt() != 0 })
    }
}
