package com.songxi.app.core.sensory

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Calm, optional offline Mandarin voice coaching engine ("轻声语音口令导引").
 * Speaks concise setup, halfway-switch, and safe-return cues at 0.88x slow pace
 * so Eyes-Free users never need to open their eyes mid-stretch.
 */
interface VoiceCueEngine {
    val spokenCueHistory: StateFlow<List<String>>

    fun speakCue(
        text: String,
        enabled: Boolean = true,
        muted: Boolean = false
    )

    fun stop()

    fun release()
}

object NoOpVoiceCueEngine : VoiceCueEngine {
    private val empty = MutableStateFlow<List<String>>(emptyList())
    override val spokenCueHistory: StateFlow<List<String>> = empty.asStateFlow()
    override fun speakCue(text: String, enabled: Boolean, muted: Boolean) = Unit
    override fun stop() = Unit
    override fun release() = Unit
}

class RecordingVoiceCueEngine : VoiceCueEngine {
    private val _history = MutableStateFlow<List<String>>(emptyList())
    override val spokenCueHistory: StateFlow<List<String>> = _history.asStateFlow()

    override fun speakCue(text: String, enabled: Boolean, muted: Boolean) {
        val clean = text.trim()
        if (!enabled || muted || clean.isEmpty()) return
        _history.update { (it + clean).takeLast(64) }
    }

    override fun stop() = Unit

    override fun release() {
        _history.value = emptyList()
    }
}

class AndroidTextToSpeechVoiceEngine(
    context: Context
) : VoiceCueEngine {

    private val appContext: Context = context.applicationContext ?: context
    private val lock = Any()
    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false
    private var initAttempted: Boolean = false
    private var pendingUtterance: String? = null

    private val _spokenCueHistory = MutableStateFlow<List<String>>(emptyList())
    override val spokenCueHistory: StateFlow<List<String>> = _spokenCueHistory.asStateFlow()

    override fun speakCue(text: String, enabled: Boolean, muted: Boolean) {
        val clean = text.trim()
        if (!enabled || muted || clean.isEmpty()) {
            if (muted || !enabled) stop()
            return
        }

        _spokenCueHistory.update { (it + clean).takeLast(64) }

        synchronized(lock) {
            if (!initAttempted) {
                initAttempted = true
                pendingUtterance = clean
                initTtsLocked()
                return
            }
            val activeTts = tts
            if (isReady && activeTts != null) {
                runCatching {
                    activeTts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "songxi_cue_${System.nanoTime()}")
                }
            } else {
                pendingUtterance = clean
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            pendingUtterance = null
            runCatching { tts?.stop() }
        }
    }

    override fun release() {
        synchronized(lock) {
            pendingUtterance = null
            isReady = false
            initAttempted = false
            val current = tts
            tts = null
            runCatching {
                current?.stop()
                current?.shutdown()
            }
        }
    }

    private fun initTtsLocked() {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                synchronized(lock) {
                    val engine = tts
                    if (status == TextToSpeech.SUCCESS && engine != null) {
                        runCatching {
                            val langRes = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
                            if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                                engine.setLanguage(Locale.CHINESE)
                            }
                            engine.setSpeechRate(0.88f)
                            engine.setPitch(0.96f)
                        }
                        isReady = true
                        val queued = pendingUtterance
                        pendingUtterance = null
                        if (!queued.isNullOrBlank()) {
                            runCatching {
                                engine.speak(queued, TextToSpeech.QUEUE_FLUSH, null, "songxi_cue_init")
                            }
                        }
                    } else {
                        isReady = false
                    }
                }
            }
        }
    }
}
