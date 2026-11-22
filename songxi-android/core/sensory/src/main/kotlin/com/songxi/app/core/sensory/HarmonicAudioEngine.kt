package com.songxi.app.core.sensory

import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId

enum class HarmonicCueType(val durationMs: Int) {
    INHALE(1800),
    EXHALE(2200),
    POSE_TRANSITION(1100),
    SKIN_PREVIEW(1200)
}

data class AudioPlaybackEvent(
    val cueType: HarmonicCueType,
    val skinId: SkinId,
    val effectiveProfile: SoundProfileId,
    val muted: Boolean,
    val samplesGenerated: Int,
    val sampleRateHz: Int
)

interface HarmonicAudioEngine {
    fun playInhaleChord(
        skinId: SkinId,
        soundProfile: SoundProfileId? = null,
        muted: Boolean = false
    )

    fun playInhaleChord(skinId: SkinId, muted: Boolean) =
        playInhaleChord(skinId = skinId, soundProfile = null, muted = muted)

    fun playExhaleChord(
        skinId: SkinId,
        soundProfile: SoundProfileId? = null,
        muted: Boolean = false
    )

    fun playExhaleChord(skinId: SkinId, muted: Boolean) =
        playExhaleChord(skinId = skinId, soundProfile = null, muted = muted)

    fun playPoseTransitionChime(
        skinId: SkinId,
        soundProfile: SoundProfileId? = null,
        muted: Boolean = false
    )

    fun playPoseTransitionChime(skinId: SkinId, muted: Boolean) =
        playPoseTransitionChime(skinId = skinId, soundProfile = null, muted = muted)

    fun playSkinPreviewNote(skinId: SkinId, muted: Boolean = false)

    fun release()
}

fun SkinId.defaultSoundProfile(): SoundProfileId = when (this) {
    SkinId.INK_MIST -> SoundProfileId.SINGING_BOWL_432
    SkinId.QINGLV_CELADON -> SoundProfileId.BAMBOO_CHIME_528
    SkinId.DUSK_PINE -> SoundProfileId.GUQIN_PINE_396
    SkinId.SPRING_BLOSSOM -> SoundProfileId.SPRING_RAIN_440
}
