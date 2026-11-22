package com.songxi.app.feature.splashad

import com.songxi.app.core.data.DataStoreSplashAdLedgerRepository
import com.songxi.app.core.data.SongxiDataStoreFactory
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.SplashAdCreative
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class SplashAdPolicyEngineTest {

    private val shanghaiZone: ZoneId = ZoneId.of("Asia/Shanghai")

    private class MutableClock(
        var epochMs: Long,
        private val zone: ZoneId
    ) : ClockProvider {
        override fun nowEpochMillis(): Long = epochMs
        override fun currentZoneId(): ZoneId = zone
    }

    private fun epochAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(shanghaiZone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun forcePreview_onFreshStore_showsCreativeWithoutMutatingLaunchCountOrGrace() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/preview_fresh_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 9, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            val previewDecision = engine.evaluateLaunch(forcePreview = true)
            assertIs<SplashGateDecision.Show>(previewDecision)
            assertEquals(3, previewDecision.countdownSeconds)
            assertEquals(0, ledger.totalAppLaunches.first())
            assertEquals(0, ledger.impressionsOn("2026-11-17").first())
            assertNull(ledger.lastImpressionEpochMs.first())

            // Subsequent genuine first cold launch still receives first-launch sanctuary grace
            val firstCold = engine.evaluateLaunch()
            assertEquals(SplashGateDecision.Bypass(BypassReason.FIRST_LAUNCH_GRACE), firstCold)
            assertEquals(1, ledger.totalAppLaunches.first())
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun forcePreview_overridesDailyCapAndCooldownWithoutIncrementingImpressions() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/preview_cap_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 6, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            // Launch 1: grace
            assertEquals(SplashGateDecision.Bypass(BypassReason.FIRST_LAUNCH_GRACE), engine.evaluateLaunch())

            // 3 eligible cold launches spaced 4h apart
            for (hour in listOf(10, 14, 18)) {
                clock.epochMs = epochAt(2026, 11, 17, hour, 0)
                assertIs<SplashGateDecision.Show>(engine.evaluateLaunch())
            }
            assertEquals(3, ledger.impressionsOn("2026-11-17").first())

            // 5 minutes after 3rd impression: normal launch hits DAILY_CAP_REACHED, forcePreview shows
            clock.epochMs = epochAt(2026, 11, 17, 18, 5)
            assertEquals(SplashGateDecision.Bypass(BypassReason.DAILY_CAP_REACHED), engine.evaluateLaunch())
            val forced = engine.evaluateLaunch(forcePreview = true)
            assertIs<SplashGateDecision.Show>(forced)
            assertEquals(3, ledger.impressionsOn("2026-11-17").first())
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun gate2AndGate3_activeStretchAndReminderDeepLink_bypassCleanly() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/gate23_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 11, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            // Fresh store checks
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.ACTIVE_STRETCH_SESSION),
                engine.evaluateLaunch(hasActiveOrPausedStretch = true, warmBackgroundDurationMs = 2_000_000L)
            )
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.REMINDER_QUICK_START),
                engine.evaluateLaunch(isReminderDeepLink = true)
            )

            // Post-grace checks
            assertIs<SplashGateDecision.Show>(engine.evaluateLaunch())
            clock.epochMs += 5L * 60L * 60L * 1_000L
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.ACTIVE_STRETCH_SESSION),
                engine.evaluateLaunch(hasActiveOrPausedStretch = true)
            )
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.REMINDER_QUICK_START),
                engine.evaluateLaunch(isReminderDeepLink = true)
            )
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun gate4_warmResumeBoundaries_suppressBelow30MinutesAndAllowAtOrAbove30Minutes() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/gate4_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 9, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            // 1st cold launch -> grace (totalAppLaunches = 1)
            assertEquals(SplashGateDecision.Bypass(BypassReason.FIRST_LAUNCH_GRACE), engine.evaluateLaunch())

            // 29m and 29m59.999s warm resumes -> SHORT_WARM_RESUME without incrementing totalAppLaunches
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.SHORT_WARM_RESUME),
                engine.evaluateLaunch(warmBackgroundDurationMs = 29L * 60L * 1_000L)
            )
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.SHORT_WARM_RESUME),
                engine.evaluateLaunch(warmBackgroundDurationMs = 1_799_999L)
            )
            assertEquals(1, ledger.totalAppLaunches.first())

            // Exact 30m00s (1,800,000ms) warm resume -> Show (not swallowed by FIRST_LAUNCH_GRACE!)
            val showAt30m = engine.evaluateLaunch(warmBackgroundDurationMs = 1_800_000L)
            assertIs<SplashGateDecision.Show>(showAt30m)
            assertEquals(1, ledger.totalAppLaunches.first())

            // +4h01m later, 31m warm resume -> Show
            clock.epochMs += 4L * 60L * 60L * 1_000L + 60_000L
            assertIs<SplashGateDecision.Show>(engine.evaluateLaunch(warmBackgroundDurationMs = 31L * 60L * 1_000L))
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun gate5AndGate6_dailyCapCooldownExactBoundariesAndNextDayRollover() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/gate56_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 6, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            // Cold launch #1: grace
            assertEquals(SplashGateDecision.Bypass(BypassReason.FIRST_LAUNCH_GRACE), engine.evaluateLaunch())

            // Impression #1 at 09:00
            clock.epochMs = epochAt(2026, 11, 17, 9, 0)
            val firstShow = engine.evaluateLaunch()
            assertIs<SplashGateDecision.Show>(firstShow)
            assertEquals(1, ledger.impressionsOn("2026-11-17").first())
            assertEquals(1, provider.recordedImpressionCount)

            // +3h59m and +4h00m - 1ms -> COOLDOWN_ACTIVE
            val tFirst = clock.epochMs
            clock.epochMs = tFirst + (3L * 60L + 59L) * 60L * 1_000L
            assertEquals(SplashGateDecision.Bypass(BypassReason.COOLDOWN_ACTIVE), engine.evaluateLaunch())
            clock.epochMs = tFirst + 14_399_999L
            assertEquals(SplashGateDecision.Bypass(BypassReason.COOLDOWN_ACTIVE), engine.evaluateLaunch())

            // Negative clock skew -> COOLDOWN_ACTIVE
            clock.epochMs = tFirst - 30_000L
            assertEquals(SplashGateDecision.Bypass(BypassReason.COOLDOWN_ACTIVE), engine.evaluateLaunch())

            // Exact +4h00m (14,400,000ms) -> Impression #2
            clock.epochMs = tFirst + 14_400_000L // 13:00
            assertIs<SplashGateDecision.Show>(engine.evaluateLaunch())
            assertEquals(2, ledger.impressionsOn("2026-11-17").first())

            // +4h01m -> Impression #3 (17:01)
            clock.epochMs += 14_460_000L
            assertIs<SplashGateDecision.Show>(engine.evaluateLaunch())
            assertEquals(3, ledger.impressionsOn("2026-11-17").first())

            // 4th eligible launch same day (+4h30m -> 21:31) -> DAILY_CAP_REACHED
            clock.epochMs += 4L * 60L * 60L * 1_000L + 30L * 60_000L
            assertEquals(SplashGateDecision.Bypass(BypassReason.DAILY_CAP_REACHED), engine.evaluateLaunch())

            // Next local calendar day (2026-11-18 07:00, >4h since 17:01) -> resets daily cap and Shows!
            clock.epochMs = epochAt(2026, 11, 18, 7, 0)
            assertEquals(LocalDate.of(2026, 11, 18), clock.currentLocalDate())
            assertIs<SplashGateDecision.Show>(engine.evaluateLaunch())
            assertEquals(1, ledger.impressionsOn("2026-11-18").first())
            assertEquals(3, ledger.impressionsOn("2026-11-17").first())
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun gate7_timeoutEmptyExceptionAndOuterCancellationPropagation() = runTest(StandardTestDispatcher()) {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/gate7_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 10, 0), shanghaiZone)

            // Prime store past first-launch grace
            ledger.incrementAppLaunch()

            // 1. Slow provider exceeding 1200ms -> PROVIDER_TIMEOUT_OR_EMPTY with 0 impressions
            val slowProvider = object : SplashAdProvider {
                override suspend fun fetchCreative(personalized: Boolean): SplashAdCreative {
                    delay(1_500L)
                    return CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES.first()
                }
                override suspend fun recordImpression(creativeId: String, epochMs: Long) = Unit
                override suspend fun recordSkip(creativeId: String, elapsedMs: Long) = Unit
                override suspend fun recordCtaClick(creativeId: String) = Unit
            }
            val slowEngine = SplashAdPolicyEngine(ledger, slowProvider, clock, fetchTimeoutMillis = 1_200L)
            val slowDeferred = async { slowEngine.evaluateLaunch() }
            advanceTimeBy(1_200L)
            runCurrent()
            assertEquals(SplashGateDecision.Bypass(BypassReason.PROVIDER_TIMEOUT_OR_EMPTY), slowDeferred.await())
            assertEquals(0, ledger.impressionsOn("2026-11-17").first())

            // 2. Provider throwing runtime error -> PROVIDER_TIMEOUT_OR_EMPTY
            val throwingProvider = object : SplashAdProvider {
                override suspend fun fetchCreative(personalized: Boolean): SplashAdCreative? {
                    error("Network unreachable")
                }
                override suspend fun recordImpression(creativeId: String, epochMs: Long) = Unit
                override suspend fun recordSkip(creativeId: String, elapsedMs: Long) = Unit
                override suspend fun recordCtaClick(creativeId: String) = Unit
            }
            val throwingEngine = SplashAdPolicyEngine(ledger, throwingProvider, clock)
            assertEquals(
                SplashGateDecision.Bypass(BypassReason.PROVIDER_TIMEOUT_OR_EMPTY),
                throwingEngine.evaluateLaunch()
            )

            // 3. Outer coroutine cancellation rethrows CancellationException
            val cancellableJob = async {
                slowEngine.evaluateLaunch()
            }
            runCurrent()
            cancellableJob.cancel(CancellationException("Caller cancelled"))
            assertFailsWith<CancellationException> {
                cancellableJob.await()
            }
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun gate6_extremeClockSkewLongOverflow_bypassesCleanlyWithoutSignedUnderflow() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/skew_overflow_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            val clock = MutableClock(epochAt(2026, 11, 17, 10, 0), shanghaiZone)
            val provider = CuratedHouseSplashAdProvider(ledger, clock)
            val engine = SplashAdPolicyEngine(ledger, provider, clock)

            // Prime past first-launch grace and persist corrupted/future Long.MAX_VALUE impression timestamp
            ledger.incrementAppLaunch()
            ledger.recordImpression(
                creativeId = "house_celadon_tea",
                epochMs = Long.MAX_VALUE,
                dateIso = "2099-01-01"
            )

            // Negative / pre-1970 clock values where (nowEpochMs - Long.MAX_VALUE) would wrap positive in signed 64-bit math
            for (skewedNow in listOf(-1_000L, Long.MIN_VALUE, 0L, 1_700_000_000_000L, Long.MAX_VALUE - 1L)) {
                clock.epochMs = skewedNow
                assertEquals(
                    SplashGateDecision.Bypass(BypassReason.COOLDOWN_ACTIVE),
                    engine.evaluateLaunch(),
                    "Expected COOLDOWN_ACTIVE for skewedNow=$skewedNow vs lastEpochMs=Long.MAX_VALUE"
                )
            }

            // Conversely, if lastImpressionEpochMs is Long.MIN_VALUE and now is modern epoch, elapsed > 4h must Show
            val storeFileMin = File("build/tmp/splash_policy_test/skew_min_${System.nanoTime()}.preferences_pb")
            val ledgerMin = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFileMin, dsScope)
            )
            ledgerMin.incrementAppLaunch()
            ledgerMin.recordImpression(
                creativeId = "house_celadon_tea",
                epochMs = Long.MIN_VALUE,
                dateIso = "1900-01-01"
            )
            clock.epochMs = epochAt(2026, 11, 17, 12, 0)
            val showAfterHugePositiveSpan = SplashAdPolicyEngine(ledgerMin, provider, clock).evaluateLaunch()
            assertIs<SplashGateDecision.Show>(showAfterHugePositiveSpan)
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun evaluateLaunch_normalizesAbnormalCreativeDurationsTo3sInPreviewAndStandardShow() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val storeFile = File("build/tmp/splash_policy_test/duration_norm_${System.nanoTime()}.preferences_pb")
            val ledger = DataStoreSplashAdLedgerRepository(
                SongxiDataStoreFactory.createPreferenceDataStore(storeFile, dsScope)
            )
            ledger.incrementAppLaunch() // Past first-launch grace
            var rawDuration = 0
            val customProvider = object : SplashAdProvider {
                override suspend fun fetchCreative(personalized: Boolean): SplashAdCreative =
                    CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES.first().copy(durationSeconds = rawDuration)
                override suspend fun recordImpression(creativeId: String, epochMs: Long) = Unit
                override suspend fun recordSkip(creativeId: String, elapsedMs: Long) = Unit
                override suspend fun recordCtaClick(creativeId: String) = Unit
            }

            var currentDay = 17
            val clock = MutableClock(epochAt(2026, 11, currentDay, 10, 0), shanghaiZone)
            val engine = SplashAdPolicyEngine(ledger, customProvider, clock)

            val cases = listOf(0 to 3, -5 to 3, 99 to 3, 1 to 1, 2 to 2, 3 to 3)
            for ((inputSec, expectedSec) in cases) {
                rawDuration = inputSec

                val previewShow = engine.evaluateLaunch(forcePreview = true)
                assertIs<SplashGateDecision.Show>(previewShow)
                assertEquals(expectedSec, previewShow.countdownSeconds, "forcePreview countdown for raw=$inputSec")
                assertEquals(expectedSec, previewShow.creative.durationSeconds)

                clock.epochMs = epochAt(2026, 11, currentDay, 10, 0)
                val standardShow = engine.evaluateLaunch()
                assertIs<SplashGateDecision.Show>(standardShow)
                assertEquals(expectedSec, standardShow.countdownSeconds, "standard countdown for raw=$inputSec")
                assertEquals(expectedSec, standardShow.creative.durationSeconds)
                currentDay++
            }
        } finally {
            dsScope.cancel()
        }
    }
}
