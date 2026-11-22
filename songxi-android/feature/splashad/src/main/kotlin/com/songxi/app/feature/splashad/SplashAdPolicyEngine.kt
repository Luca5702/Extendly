package com.songxi.app.feature.splashad

import com.songxi.app.core.data.SplashAdLedgerRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.SplashAdCreative
import com.songxi.app.core.model.SystemClockProvider
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class SplashAdPolicyEngine(
    private val ledgerRepository: SplashAdLedgerRepository,
    private val adProvider: SplashAdProvider,
    private val clockProvider: ClockProvider = SystemClockProvider,
    private val fetchTimeoutMillis: Long = DEFAULT_FETCH_TIMEOUT_MS
) {

    suspend fun evaluateLaunch(
        isReminderDeepLink: Boolean = false,
        hasActiveOrPausedStretch: Boolean = false,
        warmBackgroundDurationMs: Long? = null,
        forcePreview: Boolean = false
    ): SplashGateDecision {
        val personalized = ledgerRepository.personalizedAdsEnabled.first()

        // Priority 0: Manual / preview trigger — zero mutations to launch count, daily cap, or cooldown.
        if (forcePreview) {
            val previewCreative = fetchCreativeBounded(personalized)
                ?: return SplashGateDecision.Bypass(BypassReason.PROVIDER_TIMEOUT_OR_EMPTY)
            val safeSeconds = normalizeSplashDurationSeconds(previewCreative.durationSeconds)
            return SplashGateDecision.Show(
                creative = if (previewCreative.durationSeconds == safeSeconds) {
                    previewCreative
                } else {
                    previewCreative.copy(durationSeconds = safeSeconds)
                },
                countdownSeconds = safeSeconds
            )
        }

        // Capture a single atomic instant so date rollover and impression timestamp stay consistent.
        val nowEpochMs = clockProvider.nowEpochMillis()
        val todayIso = Instant.ofEpochMilli(nowEpochMs)
            .atZone(clockProvider.currentZoneId())
            .toLocalDate()
            .toString()

        // Increment cold-launch count exclusively on genuine cold launches (warmBackgroundDurationMs == null).
        val launchCount = if (warmBackgroundDurationMs == null) {
            ledgerRepository.incrementAppLaunch()
        } else {
            ledgerRepository.totalAppLaunches.first()
        }

        // Gate 2: Active or face-down-paused stretch session immunity.
        if (hasActiveOrPausedStretch) {
            return SplashGateDecision.Bypass(BypassReason.ACTIVE_STRETCH_SESSION)
        }

        // Gate 3: Gentle sedentary reminder quick-start deep-link bypass.
        if (isReminderDeepLink) {
            return SplashGateDecision.Bypass(BypassReason.REMINDER_QUICK_START)
        }

        // Gate 1: First-launch sanctuary grace (cold launch 0 -> 1).
        if (warmBackgroundDurationMs == null && launchCount <= 1) {
            return SplashGateDecision.Bypass(BypassReason.FIRST_LAUNCH_GRACE)
        }

        // Gate 4: Short warm-resume suppression (< 30 minutes in background).
        if (warmBackgroundDurationMs != null && warmBackgroundDurationMs < WARM_RESUME_SUPPRESSION_THRESHOLD_MS) {
            return SplashGateDecision.Bypass(BypassReason.SHORT_WARM_RESUME)
        }

        // Gate 5: Daily natural-day cap (<= 3 impressions per local YYYY-MM-DD).
        val impressionsToday = ledgerRepository.impressionsOn(todayIso).first()
        if (impressionsToday >= MAX_DAILY_IMPRESSIONS) {
            return SplashGateDecision.Bypass(BypassReason.DAILY_CAP_REACHED)
        }

        // Gate 6: Minimum 4-hour cooldown between impressions (including overflow-safe negative skew guard).
        val lastEpochMs = ledgerRepository.lastImpressionEpochMs.first()
        if (lastEpochMs != null &&
            (nowEpochMs < lastEpochMs || (nowEpochMs - lastEpochMs) in 0L until MIN_IMPRESSION_COOLDOWN_MS)
        ) {
            return SplashGateDecision.Bypass(BypassReason.COOLDOWN_ACTIVE)
        }

        // Gate 7: Non-delaying 1200ms fetch timeout & display-safety check.
        val creative = fetchCreativeBounded(personalized)
            ?: return SplashGateDecision.Bypass(BypassReason.PROVIDER_TIMEOUT_OR_EMPTY)

        ledgerRepository.recordImpression(
            creativeId = creative.id,
            epochMs = nowEpochMs,
            dateIso = todayIso
        )
        try {
            adProvider.recordImpression(creative.id, nowEpochMs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Provider telemetry failure must never disrupt creative display after ledger commit.
        }

        val safeSeconds = normalizeSplashDurationSeconds(creative.durationSeconds)
        return SplashGateDecision.Show(
            creative = if (creative.durationSeconds == safeSeconds) {
                creative
            } else {
                creative.copy(durationSeconds = safeSeconds)
            },
            countdownSeconds = safeSeconds
        )
    }

    private suspend fun fetchCreativeBounded(personalized: Boolean): SplashAdCreative? {
        if (fetchTimeoutMillis <= 0L) return null
        val fetched = try {
            withTimeoutOrNull(fetchTimeoutMillis) {
                adProvider.fetchCreative(personalized)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
        return fetched?.takeIf { SplashAdComplianceValidator.isCreativeDisplaySafe(it) }
    }

    companion object {
        const val DEFAULT_COUNTDOWN_SECONDS: Int = 3
        const val WARM_RESUME_SUPPRESSION_THRESHOLD_MS: Long = 30L * 60L * 1_000L // 1,800,000 ms (30m)
        const val MAX_DAILY_IMPRESSIONS: Int = 3
        const val MIN_IMPRESSION_COOLDOWN_MS: Long = 4L * 60L * 60L * 1_000L       // 14,400,000 ms (4h)
        const val DEFAULT_FETCH_TIMEOUT_MS: Long = 1_200L
    }
}
