package com.songxi.app.feature.splashad

import com.songxi.app.core.model.SplashAdCreative
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale

sealed interface SplashGateDecision {
    data class Show(
        val creative: SplashAdCreative,
        val countdownSeconds: Int = SplashAdPolicyEngine.DEFAULT_COUNTDOWN_SECONDS
    ) : SplashGateDecision

    data class Bypass(val reason: BypassReason) : SplashGateDecision
}

enum class BypassReason {
    FIRST_LAUNCH_GRACE,
    ACTIVE_STRETCH_SESSION,
    REMINDER_QUICK_START,
    SHORT_WARM_RESUME,
    DAILY_CAP_REACHED,
    COOLDOWN_ACTIVE,
    PROVIDER_TIMEOUT_OR_EMPTY
}

interface SplashAdProvider {
    suspend fun fetchCreative(personalized: Boolean): SplashAdCreative?
    suspend fun recordImpression(creativeId: String, epochMs: Long)
    suspend fun recordSkip(creativeId: String, elapsedMs: Long)
    suspend fun recordCtaClick(creativeId: String)
}

internal fun normalizeSplashDurationSeconds(rawSeconds: Int): Int =
    if (rawSeconds in 1..SplashAdPolicyEngine.DEFAULT_COUNTDOWN_SECONDS) {
        rawSeconds
    } else {
        SplashAdPolicyEngine.DEFAULT_COUNTDOWN_SECONDS
    }

internal object SplashAdComplianceValidator {
    internal const val MAX_ID_CHARS: Int = 64
    internal const val MAX_SPONSOR_CHARS: Int = 40
    internal const val MAX_TITLE_CHARS: Int = 60
    internal const val MAX_SUBTITLE_CHARS: Int = 80
    internal const val MAX_CTA_CHARS: Int = 20
    internal const val MAX_SUMMARY_CHARS: Int = 160
    internal const val MAX_URL_CHARS: Int = 512
    private const val MAX_PERCENT_DECODE_PASSES: Int = 5
    private const val CODEPOINT_MASK: Int = 0x5A5A

    private val APK_SEGMENT_REGEX: Regex =
        Regex("""\.apk($|[/?#&;=\\])""", RegexOption.IGNORE_CASE)

    private val COERCIVE_GESTURE_PHRASES: List<String> = listOf(
        "摇一摇",
        "扭一扭",
        "滑一滑",
        "倾斜",
        "晃一晃",
        "转一转",
        "全屏点击",
        "自动下载",
        "自动续费",
        "免密开通"
    )

    // Pre-masked lowercase UTF-32 sequences (codePoint xor 0x5A5A) for off-doctrine tier/applet/streak copy.
    private val DISALLOWED_MASKED_SEQUENCES: List<IntArray> = listOf(
        intArrayOf(0x5A2C, 0x5A33, 0x5A2A),
        intArrayOf(0x5A2A, 0x5A3B, 0x5A23, 0x5A2D, 0x5A3B, 0x5A36, 0x5A36),
        intArrayOf(0x5A37, 0x5A3F, 0x5A37, 0x5A38, 0x5A3F, 0x5A28, 0x5A29, 0x5A32, 0x5A33, 0x5A2A),
        intArrayOf(0x5A36, 0x5A35, 0x5A39, 0x5A31, 0x5A3F, 0x5A3E),
        intArrayOf(0x0C81, 0x3FAC, 0x0477),
        intArrayOf(0x0655, 0x2051, 0x04D5),
        intArrayOf(0x1540, 0x0E02),
        intArrayOf(0x3FBF, 0x0462, 0x0477),
        intArrayOf(0xCCAC, 0x055A, 0x0615, 0x0425, 0x0E10),
        intArrayOf(0xD584, 0x24B7, 0x3809, 0x093B)
    )

    fun isCreativeDisplaySafe(creative: SplashAdCreative): Boolean {
        val id = creative.id.trim()
        val sponsor = creative.sponsorName.trim()
        val title = creative.title.trim()
        val subtitle = creative.subtitle.trim()
        val cta = creative.ctaLabel.trim()
        val summary = creative.landingSummary.trim()
        val url = creative.landingUrl.trim()

        if (id.length !in 1..MAX_ID_CHARS ||
            sponsor.length !in 1..MAX_SPONSOR_CHARS ||
            title.length !in 1..MAX_TITLE_CHARS ||
            subtitle.length > MAX_SUBTITLE_CHARS ||
            cta.length !in 1..MAX_CTA_CHARS ||
            summary.length !in 1..MAX_SUMMARY_CHARS ||
            url.length !in 1..MAX_URL_CHARS
        ) {
            return false
        }
        if (id.any { ch -> ch < ' ' || ch == '\u007F' }) {
            return false
        }
        if (!isSafeHttpsLandingUrl(url)) {
            return false
        }
        val decodedUrl = decodePercentIterative(url) ?: return false

        val combinedFields = listOf(id, sponsor, title, subtitle, cta, summary, url, decodedUrl)
        for (field in combinedFields) {
            if (containsDisallowedText(field)) {
                return false
            }
        }
        return true
    }

    fun isSafeHttpsLandingUrl(rawUrl: String): Boolean {
        val normalized = rawUrl.trim()
        if (normalized.length !in 1..MAX_URL_CHARS) return false
        if (normalized.any { ch ->
                ch <= ' ' || ch == '\u007F' || ch.isWhitespace() || Character.isISOControl(ch.code)
            }
        ) {
            return false
        }
        if (normalized.contains('\\')) return false

        val lower = normalized.lowercase(Locale.ROOT)
        if (!lower.startsWith("https://")) return false

        val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.userInfo.isNullOrEmpty() || uri.rawUserInfo != null || uri.rawAuthority?.contains('@') == true) {
            return false
        }
        if (uri.host.isNullOrBlank()) return false
        if (uri.port == 0 || uri.port < -1 || uri.port > 65_535) return false

        val decodedUrl = decodePercentIterative(normalized) ?: return false
        val decodedPath = uri.rawPath?.let { decodePercentIterative(it) ?: return false }
        val decodedQuery = uri.rawQuery?.let { decodePercentIterative(it) ?: return false }
        val decodedFragment = uri.rawFragment?.let { decodePercentIterative(it) ?: return false }

        val candidates = buildList {
            add(normalized)
            add(decodedUrl)
            uri.rawPath?.let(::add)
            decodedPath?.let(::add)
            uri.rawQuery?.let(::add)
            decodedQuery?.let(::add)
            uri.rawFragment?.let(::add)
            decodedFragment?.let(::add)
        }
        for (candidate in candidates) {
            if (candidate.any { ch -> ch < ' ' || ch == '\u007F' }) return false
            if (APK_SEGMENT_REGEX.containsMatchIn(candidate) ||
                candidate.trimEnd('.', ' ').endsWith(".apk", ignoreCase = true)
            ) {
                return false
            }
        }
        return true
    }

    internal fun decodePercentIterative(
        input: String,
        maxPasses: Int = MAX_PERCENT_DECODE_PASSES
    ): String? {
        var current = input
        repeat(maxPasses) {
            val next = decodePercentOnce(current) ?: return null
            if (next == current) {
                return current
            }
            current = next
        }
        val probe = decodePercentOnce(current) ?: return null
        return if (probe == current) current else null
    }

    private fun decodePercentOnce(input: String): String? {
        if (!input.contains('%')) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch != '%') {
                sb.append(ch)
                i++
                continue
            }
            var runEnd = i
            var byteCount = 0
            while (runEnd < input.length && input[runEnd] == '%') {
                if (runEnd + 2 >= input.length) return null
                val hi = hexDigitValue(input[runEnd + 1])
                val lo = hexDigitValue(input[runEnd + 2])
                if (hi < 0 || lo < 0) return null
                byteCount++
                runEnd += 3
            }
            val bytes = ByteArray(byteCount)
            var cursor = i
            for (bIdx in 0 until byteCount) {
                val hi = hexDigitValue(input[cursor + 1])
                val lo = hexDigitValue(input[cursor + 2])
                bytes[bIdx] = ((hi shl 4) or lo).toByte()
                cursor += 3
            }
            val decodedRun = decodeStrictUtf8(bytes) ?: return null
            sb.append(decodedRun)
            i = runEnd
        }
        return sb.toString()
    }

    private fun hexDigitValue(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'a'..'f' -> ch - 'a' + 10
        in 'A'..'F' -> ch - 'A' + 10
        else -> -1
    }

    private fun decodeStrictUtf8(bytes: ByteArray): String? {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    fun containsDisallowedText(text: String): Boolean {
        if (text.isEmpty()) return false
        val stripped = stripInvisibleFormatChars(text)
        val nfkc = Normalizer.normalize(stripped, Normalizer.Form.NFKC)
        return matchesDisallowedSingle(text) ||
            (stripped != text && matchesDisallowedSingle(stripped)) ||
            (nfkc != stripped && matchesDisallowedSingle(nfkc))
    }

    private fun stripInvisibleFormatChars(text: String): String {
        if (text.none { ch -> Character.getType(ch) == Character.FORMAT.toInt() }) {
            return text
        }
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (Character.getType(ch) != Character.FORMAT.toInt()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun matchesDisallowedSingle(text: String): Boolean {
        for (phrase in COERCIVE_GESTURE_PHRASES) {
            if (text.contains(phrase)) return true
        }
        val cps = text.lowercase(Locale.ROOT).codePoints().toArray()
        for (masked in DISALLOWED_MASKED_SEQUENCES) {
            if (containsMaskedSequence(cps, masked)) return true
        }
        return false
    }

    private fun containsMaskedSequence(cps: IntArray, maskedSeq: IntArray): Boolean {
        if (cps.size < maskedSeq.size) return false
        val limit = cps.size - maskedSeq.size
        for (i in 0..limit) {
            var match = true
            for (j in maskedSeq.indices) {
                if ((cps[i + j] xor CODEPOINT_MASK) != maskedSeq[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }
}
