package com.songxi.app.core.data

import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashAdLedgerRepositoryTest {

    private fun newTempStoreFile(): File {
        val baseTmp = File(System.getProperty("java.io.tmpdir") ?: error("java.io.tmpdir unset"))
        return File(baseTmp, "songxi_splash_test/splash_${UUID.randomUUID()}.preferences_pb")
    }

    @Test
    fun freshDefaultsSequentialLaunchesImpressionsPrivacyToggleAndReset() =
        runTest(StandardTestDispatcher()) {
            val store = SongxiDataStoreFactory.createPreferenceDataStore(newTempStoreFile(), backgroundScope)
            val repo = DataStoreSplashAdLedgerRepository(store)

            assertEquals(0, repo.totalAppLaunches.first())
            assertFalse(repo.personalizedAdsEnabled.first())
            assertNull(repo.lastImpressionEpochMs.first())
            assertEquals(0, repo.impressionsOn("2026-11-16").first())
            assertEquals(0, repo.totalSkips.first())
            assertEquals(0, repo.totalCtaClicks.first())

            assertEquals(1, repo.incrementAppLaunch())
            assertEquals(2, repo.incrementAppLaunch())
            assertEquals(3, repo.incrementAppLaunch())
            assertEquals(3, repo.totalAppLaunches.first())

            repo.recordImpression("creative_celadon", 1_794_794_400_000L, "2026-11-16")
            repo.recordImpression("creative_incense", 1_794_816_000_000L, "2026-11-16")
            repo.recordImpression("creative_wood", 1_794_902_400_000L, "2026-11-17")

            assertEquals(2, repo.impressionsOn("2026-11-16").first())
            assertEquals(1, repo.impressionsOn("2026-11-17").first())
            assertEquals(1_794_902_400_000L, repo.lastImpressionEpochMs.first())

            repo.recordSkip("creative_celadon", 850L)
            repo.recordCtaClick("creative_incense")
            repo.setPersonalizedAdsEnabled(true)

            assertEquals(1, repo.totalSkips.first())
            assertEquals(1, repo.totalCtaClicks.first())
            assertTrue(repo.personalizedAdsEnabled.first())

            repo.resetAll()
            assertEquals(0, repo.totalAppLaunches.first())
            assertFalse(repo.personalizedAdsEnabled.first())
            assertNull(repo.lastImpressionEpochMs.first())
            assertEquals(0, repo.impressionsOn("2026-11-16").first())
            assertEquals(0, repo.totalSkips.first())
            assertEquals(0, repo.totalCtaClicks.first())
        }
}
