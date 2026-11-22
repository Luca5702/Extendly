package com.songxi.app.core.sensory

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidHarmonicAudioEngine(
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val audioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val sampleRateHz: Int = HarmonicPcmSynthesizer.DEFAULT_SAMPLE_RATE_HZ
) : HarmonicAudioEngine {

    private val trackGuard = Any()
    private var activePlaybackJob: Job? = null
    private var activeTrack: AudioTrack? = null

    private val _playbackHistory = MutableStateFlow<List<AudioPlaybackEvent>>(emptyList())
    val playbackHistory: StateFlow<List<AudioPlaybackEvent>> = _playbackHistory.asStateFlow()

    fun synthesizeChordPcm(
        soundProfile: SoundProfileId,
        cueType: HarmonicCueType,
        muted: Boolean = false,
        sampleRate: Int = sampleRateHz
    ): ShortArray = HarmonicPcmSynthesizer.synthesizeChordPcm(
        soundProfile = soundProfile,
        cueType = cueType,
        muted = muted,
        sampleRateHz = sampleRate
    )

    override fun playInhaleChord(
        skinId: SkinId,
        soundProfile: SoundProfileId?,
        muted: Boolean
    ) {
        triggerCue(HarmonicCueType.INHALE, skinId, soundProfile, muted)
    }

    override fun playExhaleChord(
        skinId: SkinId,
        soundProfile: SoundProfileId?,
        muted: Boolean
    ) {
        triggerCue(HarmonicCueType.EXHALE, skinId, soundProfile, muted)
    }

    override fun playPoseTransitionChime(
        skinId: SkinId,
        soundProfile: SoundProfileId?,
        muted: Boolean
    ) {
        triggerCue(HarmonicCueType.POSE_TRANSITION, skinId, soundProfile, muted)
    }

    override fun playSkinPreviewNote(skinId: SkinId, muted: Boolean) {
        triggerCue(HarmonicCueType.SKIN_PREVIEW, skinId, null, muted)
    }

    override fun release() {
        synchronized(trackGuard) {
            activePlaybackJob?.cancel()
            activePlaybackJob = null
            stopAndReleaseActiveTrackGuarded()
        }
    }

    private fun triggerCue(
        cueType: HarmonicCueType,
        skinId: SkinId,
        soundProfile: SoundProfileId?,
        muted: Boolean
    ) {
        val effectiveProfile = soundProfile ?: skinId.defaultSoundProfile()
        val effectivelyMuted = muted || effectiveProfile == SoundProfileId.SILENT_HAPTIC_ONLY

        synchronized(trackGuard) {
            activePlaybackJob?.cancel()
            activePlaybackJob = null
            stopAndReleaseActiveTrackGuarded()
        }

        if (effectivelyMuted) {
            recordEvent(
                AudioPlaybackEvent(
                    cueType = cueType,
                    skinId = skinId,
                    effectiveProfile = effectiveProfile,
                    muted = true,
                    samplesGenerated = 0,
                    sampleRateHz = sampleRateHz
                )
            )
            return
        }

        val job = externalScope.launch(audioDispatcher) {
            val pcmShorts = HarmonicPcmSynthesizer.synthesizeChordPcm(
                soundProfile = effectiveProfile,
                cueType = cueType,
                muted = false,
                sampleRateHz = sampleRateHz
            )
            ensureActive()
            if (pcmShorts.isEmpty()) return@launch

            val rawBytes = ByteArray(pcmShorts.size * 2)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.nativeOrder()).asShortBuffer().put(pcmShorts)

            val minBuf = runCatching {
                AudioTrack.getMinBufferSize(
                    sampleRateHz,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            }.getOrDefault(1024)

            if (minBuf <= 0) {
                recordEvent(
                    AudioPlaybackEvent(
                        cueType = cueType,
                        skinId = skinId,
                        effectiveProfile = effectiveProfile,
                        muted = false,
                        samplesGenerated = pcmShorts.size,
                        sampleRateHz = sampleRateHz
                    )
                )
                return@launch
            }

            val alignedSize = maxOf(rawBytes.size, minBuf).let { if (it % 2 == 0) it else it + 1 }
            val pcmBytes = if (alignedSize > rawBytes.size) {
                rawBytes.copyOf(alignedSize)
            } else {
                rawBytes
            }

            var createdTrack: AudioTrack? = null
            try {
                val track = runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRateHz)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(pcmBytes.size)
                        .build()
                }.getOrNull()

                if (track == null) {
                    recordEvent(
                        AudioPlaybackEvent(
                            cueType = cueType,
                            skinId = skinId,
                            effectiveProfile = effectiveProfile,
                            muted = false,
                            samplesGenerated = pcmShorts.size,
                            sampleRateHz = sampleRateHz
                        )
                    )
                    return@launch
                }

                createdTrack = track
                val written = track.write(pcmBytes, 0, pcmBytes.size, AudioTrack.WRITE_BLOCKING)
                if (written > 0 && track.state == AudioTrack.STATE_INITIALIZED && isActive) {
                    synchronized(trackGuard) {
                        stopAndReleaseActiveTrackGuarded()
                        activeTrack = track
                    }
                    track.play()
                    recordEvent(
                        AudioPlaybackEvent(
                            cueType = cueType,
                            skinId = skinId,
                            effectiveProfile = effectiveProfile,
                            muted = false,
                            samplesGenerated = pcmShorts.size,
                            sampleRateHz = sampleRateHz
                        )
                    )
                    delay(cueType.durationMs.toLong() + 120L)
                } else {
                    safeReleaseTrack(track)
                }
            } finally {
                val target = createdTrack
                if (target != null) {
                    synchronized(trackGuard) {
                        if (activeTrack === target) {
                            activeTrack = null
                            safeReleaseTrack(target)
                        }
                    }
                }
            }
        }

        synchronized(trackGuard) {
            if (job.isActive) {
                activePlaybackJob = job
            }
        }
    }

    private fun stopAndReleaseActiveTrackGuarded() {
        val current = activeTrack ?: return
        activeTrack = null
        safeReleaseTrack(current)
    }

    private fun safeReleaseTrack(track: AudioTrack) {
        runCatching {
            if (track.state == AudioTrack.STATE_INITIALIZED &&
                track.playState == AudioTrack.PLAYSTATE_PLAYING
            ) {
                track.pause()
                track.flush()
                track.stop()
            }
        }
        runCatching { track.release() }
    }

    private fun recordEvent(event: AudioPlaybackEvent) {
        _playbackHistory.update { history ->
            (history + event).takeLast(MAX_HISTORY_EVENTS)
        }
    }

    private companion object {
        const val MAX_HISTORY_EVENTS = 32
    }
}
