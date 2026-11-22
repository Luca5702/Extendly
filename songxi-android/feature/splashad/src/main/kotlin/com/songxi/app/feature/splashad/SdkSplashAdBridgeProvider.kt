package com.songxi.app.feature.splashad

import com.songxi.app.core.data.SplashAdLedgerRepository
import com.songxi.app.core.model.SplashAdCreative
import com.songxi.app.core.model.SplashArtworkStyle
import kotlinx.coroutines.CancellationException

data class SdkRawSplashPayload(
    val id: String,
    val sponsorName: String,
    val title: String,
    val subtitle: String = "",
    val ctaLabel: String,
    val landingSummary: String,
    val landingUrl: String,
    val artworkTheme: SplashArtworkStyle = SplashArtworkStyle.CELADON_TEA,
    val durationSeconds: Int = SplashAdPolicyEngine.DEFAULT_COUNTDOWN_SECONDS,
    val requiresMotionSensor: Boolean = false,
    val requiresAutoPlayAudio: Boolean = false,
    val isFullScreenHotzone: Boolean = false,
    val autoDownloadsPackage: Boolean = false,
    val hasCommercialTierOrExternalApplet: Boolean = false
)

fun interface SplashAdSdkClient {
    suspend fun requestStaticSplash(personalized: Boolean): SdkRawSplashPayload?
    suspend fun reportImpression(creativeId: String, epochMs: Long) {}
    suspend fun reportSkip(creativeId: String, elapsedMs: Long) {}
    suspend fun reportCtaClick(creativeId: String) {}
}

class SdkSplashAdBridgeProvider(
    private val sdkClient: SplashAdSdkClient,
    private val fallbackProvider: SplashAdProvider? = null,
    private val ledgerRepository: SplashAdLedgerRepository? = null
) : SplashAdProvider {

    override suspend fun fetchCreative(personalized: Boolean): SplashAdCreative? {
        val rawPayload = try {
            sdkClient.requestStaticSplash(personalized)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }

        val sanitized = rawPayload?.let(::sanitizeSdkPayload)
        if (sanitized != null) {
            return sanitized
        }
        return fallbackProvider?.fetchCreative(personalized)
    }

    override suspend fun recordImpression(creativeId: String, epochMs: Long) {
        try {
            sdkClient.reportImpression(creativeId, epochMs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            fallbackProvider?.recordImpression(creativeId, epochMs)
        }
    }

    override suspend fun recordSkip(creativeId: String, elapsedMs: Long) {
        ledgerRepository?.recordSkip(creativeId, elapsedMs)
        try {
            sdkClient.reportSkip(creativeId, elapsedMs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Ignore external SDK reporting errors after local ledger persistence.
        }
    }

    override suspend fun recordCtaClick(creativeId: String) {
        ledgerRepository?.recordCtaClick(creativeId)
        try {
            sdkClient.reportCtaClick(creativeId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Ignore external SDK reporting errors after local ledger persistence.
        }
    }

    private fun sanitizeSdkPayload(payload: SdkRawSplashPayload): SplashAdCreative? {
        if (payload.requiresMotionSensor ||
            payload.requiresAutoPlayAudio ||
            payload.isFullScreenHotzone ||
            payload.autoDownloadsPackage ||
            payload.hasCommercialTierOrExternalApplet
        ) {
            return null
        }

        val candidate = SplashAdCreative(
            id = payload.id.trim(),
            sponsorName = payload.sponsorName.trim(),
            title = payload.title.trim(),
            subtitle = payload.subtitle.trim(),
            ctaLabel = payload.ctaLabel.trim(),
            landingSummary = payload.landingSummary.trim(),
            landingUrl = payload.landingUrl.trim(),
            artworkTheme = payload.artworkTheme,
            durationSeconds = normalizeSplashDurationSeconds(payload.durationSeconds)
        )

        return candidate.takeIf { SplashAdComplianceValidator.isCreativeDisplaySafe(it) }
    }
}
