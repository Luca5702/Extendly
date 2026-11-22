package com.songxi.app.core.sensory

import android.media.AudioFormat
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioTrack

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HarmonicAudioEngineTest {

    private val writtenBuffers = CopyOnWriteArrayList<ByteArray>()
    private val audioListener = ShadowAudioTrack.OnAudioDataWrittenListener { _, data, format ->
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, format.encoding)
        assertEquals(HarmonicPcmSynthesizer.DEFAULT_SAMPLE_RATE_HZ, format.sampleRate)
        writtenBuffers.add(data.copyOf())
    }

    @Before
    fun setUp() {
        writtenBuffers.clear()
        ShadowAudioTrack.addAudioDataListener(audioListener)
    }

    @After
    fun tearDown() {
        ShadowAudioTrack.removeAudioDataListener(audioListener)
        writtenBuffers.clear()
    }

    @Test
    fun dspSynthesizerProducesClickFreeWaveformsAtExactFundamentalFrequencies() {
        val sampleRate = HarmonicPcmSynthesizer.DEFAULT_SAMPLE_RATE_HZ
        val profileExpectedFreq = mapOf(
            SoundProfileId.SINGING_BOWL_432 to 432.0,
            SoundProfileId.BAMBOO_CHIME_528 to 528.0,
            SoundProfileId.GUQIN_PINE_396 to 396.0,
            SoundProfileId.SPRING_RAIN_440 to 440.0
        )
        val candidateFreqs = listOf(396.0, 432.0, 440.0, 528.0)

        for ((profile, expectedF0) in profileExpectedFreq) {
            for (cue in HarmonicCueType.entries) {
                val pcm = HarmonicPcmSynthesizer.synthesizeChordPcm(profile, cue, muted = false, sampleRateHz = sampleRate)
                assertEquals(cue.durationMs * 24, pcm.size)
                assertEquals("Zero-click start required for $profile/$cue", 0.toShort(), pcm.first())
                assertEquals("Zero-click end required for $profile/$cue", 0.toShort(), pcm.last())

                var peak = 0
                var sumSq = 0.0
                for (s in pcm) {
                    val a = abs(s.toInt())
                    if (a > peak) peak = a
                    val norm = s.toDouble() / 32768.0
                    sumSq += norm * norm
                }
                val rms = sqrt(sumSq / pcm.size)
                assertTrue("Peak $peak out of serene range for $profile/$cue", peak in 3_500..28_000)
                assertTrue("Expected audible RMS > 0.01 for $profile/$cue, got $rms", rms > 0.01)

                val bestFreq = candidateFreqs.maxByOrNull { freq ->
                    goertzelMagnitude(pcm, 0, minOf(12_000, pcm.size), freq, sampleRate)
                }
                assertEquals("Dominant fundamental mismatch for $profile/$cue", expectedF0, bestFreq!!, 0.1)
            }
        }
    }

    @Test
    fun cueVoicingsAndSpringRainMistShowDistinctHarmonicSignatures() {
        val sr = HarmonicPcmSynthesizer.DEFAULT_SAMPLE_RATE_HZ
        val bowlInhale = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SINGING_BOWL_432,
            HarmonicCueType.INHALE,
            muted = false,
            sampleRateHz = sr
        )
        val bowlExhale = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SINGING_BOWL_432,
            HarmonicCueType.EXHALE,
            muted = false,
            sampleRateHz = sr
        )

        // Over 250ms..1100ms, INHALE adds rising 1.5*f0 (648 Hz) whereas EXHALE adds warm 0.75*f0 (324 Hz)
        val startIdx = (0.25 * sr).toInt()
        val endIdx = (1.10 * sr).toInt()
        val inhaleFifth = goertzelMagnitude(bowlInhale, startIdx, endIdx, 432.0 * 1.5, sr)
        val exhaleFifth = goertzelMagnitude(bowlExhale, startIdx, endIdx, 432.0 * 1.5, sr)
        assertTrue("Expected INHALE fifth ($inhaleFifth) > EXHALE fifth ($exhaleFifth)", inhaleFifth > exhaleFifth)

        val exhaleLowerFourth = goertzelMagnitude(bowlExhale, startIdx, endIdx, 432.0 * 0.75, sr)
        val inhaleLowerFourth = goertzelMagnitude(bowlInhale, startIdx, endIdx, 432.0 * 0.75, sr)
        assertTrue(
            "Expected EXHALE lower-fourth ($exhaleLowerFourth) > INHALE ($inhaleLowerFourth)",
            exhaleLowerFourth > inhaleLowerFourth * 3.0
        )

        // POSE_TRANSITION introduces ascending 1.25*f0 (540 Hz) after 260ms
        val transition = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SINGING_BOWL_432,
            HarmonicCueType.POSE_TRANSITION,
            muted = false,
            sampleRateHz = sr
        )
        val earlyNote2 = goertzelMagnitude(transition, 0, (0.22 * sr).toInt(), 432.0 * 1.25, sr)
        val lateNote2 = goertzelMagnitude(transition, (0.30 * sr).toInt(), (0.85 * sr).toInt(), 432.0 * 1.25, sr)
        assertTrue("Expected strong second pentatonic note after 260ms", lateNote2 > earlyNote2 * 4.0)

        // SPRING_RAIN_440 determinism and low-pass rain mist presence
        val rainA = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SPRING_RAIN_440,
            HarmonicCueType.SKIN_PREVIEW,
            muted = false,
            sampleRateHz = sr
        )
        val rainB = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SPRING_RAIN_440,
            HarmonicCueType.SKIN_PREVIEW,
            muted = false,
            sampleRateHz = sr
        )
        assertArrayEquals(rainA, rainB)

        val bowlPreview = HarmonicPcmSynthesizer.synthesizeChordPcm(
            SoundProfileId.SINGING_BOWL_432,
            HarmonicCueType.SKIN_PREVIEW,
            muted = false,
            sampleRateHz = sr
        )
        val rainMidMist = goertzelMagnitude(rainA, 2_400, 16_800, 317.0, sr) +
            goertzelMagnitude(rainA, 2_400, 16_800, 583.0, sr)
        val bowlInterHarmonicFloor = goertzelMagnitude(bowlPreview, 2_400, 16_800, 317.0, sr) +
            goertzelMagnitude(bowlPreview, 2_400, 16_800, 583.0, sr)
        assertTrue("Expected warm rain mist energy above pure tonal floor", rainMidMist > bowlInterHarmonicFloor * 1.5)

        val rainHighHiss = goertzelMagnitude(rainA, 2_400, 16_800, 7_500.0, sr) +
            goertzelMagnitude(rainA, 2_400, 16_800, 9_500.0, sr)
        assertTrue("Expected low-pass filter to attenuate high-frequency hiss", rainMidMist > rainHighHiss * 4.0)
    }

    @Test
    fun audioTrackWritesBytesWhenAudibleAndSuppressesWritesWhenMutedOrSilent() {
        val dispatcher = UnconfinedTestDispatcher()
        val testScope = TestScope(dispatcher)
        val engine = AndroidHarmonicAudioEngine(
            externalScope = testScope,
            audioDispatcher = dispatcher
        )

        // 2-arg overload audible playback
        engine.playInhaleChord(SkinId.INK_MIST, muted = false)
        assertEquals(1, writtenBuffers.size)
        assertEquals(1800 * 24 * 2, writtenBuffers[0].size)
        assertTrue(writtenBuffers[0].any { it.toInt() != 0 })

        // Skin preview note
        engine.playSkinPreviewNote(SkinId.QINGLV_CELADON)
        assertEquals(2, writtenBuffers.size)
        assertEquals(1200 * 24 * 2, writtenBuffers[1].size)

        // 3-arg overload with explicit profile
        engine.playExhaleChord(SkinId.DUSK_PINE, SoundProfileId.GUQIN_PINE_396, muted = false)
        assertEquals(3, writtenBuffers.size)
        assertEquals(2200 * 24 * 2, writtenBuffers[2].size)

        // Muted and SILENT_HAPTIC_ONLY must write 0 additional buffers
        engine.playInhaleChord(SkinId.INK_MIST, muted = true)
        engine.playPoseTransitionChime(SkinId.INK_MIST, SoundProfileId.SILENT_HAPTIC_ONLY, muted = false)
        assertEquals(3, writtenBuffers.size)

        // Release and subsequent session reuse
        engine.release()
        engine.playPoseTransitionChime(SkinId.SPRING_BLOSSOM, muted = false)
        assertEquals(4, writtenBuffers.size)
        assertEquals(1100 * 24 * 2, writtenBuffers[3].size)
        engine.release()
    }

    private fun goertzelMagnitude(
        samples: ShortArray,
        startInclusive: Int,
        endExclusive: Int,
        targetFreqHz: Double,
        sampleRateHz: Int
    ): Double {
        val n = (endExclusive - startInclusive).coerceAtLeast(1)
        val omega = 2.0 * PI * targetFreqHz / sampleRateHz.toDouble()
        val coeff = 2.0 * cos(omega)
        var sPrev = 0.0
        var sPrev2 = 0.0
        for (i in startInclusive until endExclusive) {
            val x = samples[i].toDouble() / 32768.0
            val s = x + coeff * sPrev - sPrev2
            sPrev2 = sPrev
            sPrev = s
        }
        val real = sPrev - sPrev2 * cos(omega)
        val imag = sPrev2 * sin(omega)
        return sqrt(real * real + imag * imag) / n.toDouble()
    }
}
