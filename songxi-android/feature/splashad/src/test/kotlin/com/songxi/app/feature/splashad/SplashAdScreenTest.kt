package com.songxi.app.feature.splashad

import android.content.Context
import android.hardware.SensorManager
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.designsystem.SongxiTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAudioTrack

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SplashAdScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun defaultCompact320x470Viewport_renders78To22FramingAnd48dpSkipAcrossAllFourSkins() {
        val creative = CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES.first()
        var activeSkin by mutableStateOf(BuiltInSkinRegistry.InkMist)

        // Uses default autoAdvance = true (matching opaque-box E2E ComposeTestRule behavior)
        composeTestRule.setContent {
            SongxiTheme(skin = activeSkin) {
                SplashAdScreen(
                    creative = creative,
                    countdownSeconds = 3,
                    onSkipOrFinish = {},
                    onExternalUrlConfirmed = {}
                )
            }
        }

        for (skin in BuiltInSkinRegistry.allSkins) {
            composeTestRule.runOnIdle { activeSkin = skin }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("splash_ad_screen").assertIsDisplayed()
            composeTestRule.onNodeWithTag("splash_ad_backdrop").assertIsDisplayed()
            composeTestRule.onNodeWithTag("splash_ad_badge")
                .assertIsDisplayed()
                .assertTextContains("广告", substring = true)

            val skipNode = composeTestRule.onNodeWithTag("splash_ad_skip_button")
                .assertIsDisplayed()
                .assertHasClickAction()
                .assertTextContains("跳过", substring = true)
            val skipBounds = skipNode.fetchSemanticsNode().boundsInRoot
            val density = composeTestRule.density
            val skipWidthDp = with(density) { skipBounds.width.toDp() }
            val skipHeightDp = with(density) { skipBounds.height.toDp() }
            assertTrue(
                skipWidthDp >= 48.dp && skipHeightDp >= 48.dp,
                "Skip button at t=0ms must be >= 48dp x 48dp for ${skin.id}, got $skipWidthDp x $skipHeightDp"
            )

            composeTestRule.onNodeWithTag("splash_ad_cta_button").assertIsDisplayed().assertHasClickAction()
            composeTestRule.onNodeWithTag("splash_ad_brand_footer").assertIsDisplayed()

            val backdropH = composeTestRule.onNodeWithTag("splash_ad_backdrop").fetchSemanticsNode().boundsInRoot.height
            val footerH = composeTestRule.onNodeWithTag("splash_ad_brand_footer").fetchSemanticsNode().boundsInRoot.height
            val ratio = backdropH / (backdropH + footerH)
            assertTrue(
                abs(ratio - 0.78f) < 0.02f,
                "Expected 78% / 22% vertical framing for ${skin.id}, got backdrop ratio $ratio"
            )
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun mobile390x844Viewport_rendersAllThreeEasternCraftCreativesCleanly() {
        var creative by mutableStateOf(CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES[0])

        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.QinglvCeladon) {
                SplashAdScreen(
                    creative = creative,
                    countdownSeconds = 3,
                    onSkipOrFinish = {},
                    onExternalUrlConfirmed = {}
                )
            }
        }

        for (houseCreative in CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES) {
            composeTestRule.runOnIdle { creative = houseCreative }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("splash_ad_backdrop").assertIsDisplayed()
            composeTestRule.onNodeWithTag("splash_ad_cta_button")
                .assertIsDisplayed()
                .assertTextContains(houseCreative.ctaLabel, substring = true)

            val backdropH = composeTestRule.onNodeWithTag("splash_ad_backdrop").fetchSemanticsNode().boundsInRoot.height
            val footerH = composeTestRule.onNodeWithTag("splash_ad_brand_footer").fetchSemanticsNode().boundsInRoot.height
            assertTrue(abs(backdropH / (backdropH + footerH) - 0.78f) < 0.02f)
        }
    }

    @Test
    fun backdropIsStrictlyInert_andSkipButtonFiresSingleFinishWithElapsedTime() {
        val creative = CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES[1]
        var finishCount = 0
        var confirmedUrls = 0
        var skippedElapsedMs = -1L

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.DuskPine) {
                SplashAdScreen(
                    creative = creative,
                    countdownSeconds = 3,
                    onSkipOrFinish = { finishCount++ },
                    onExternalUrlConfirmed = { confirmedUrls++ },
                    onUserSkipped = { elapsed -> skippedElapsedMs = elapsed }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Center click and multiple off-CTA taps on splash_ad_backdrop MUST be strictly inert
        composeTestRule.onNodeWithTag("splash_ad_backdrop").performClick()
        composeTestRule.onNodeWithTag("splash_ad_backdrop").performTouchInput {
            click(Offset(width * 0.25f, height * 0.45f))
            click(Offset(width * 0.75f, height * 0.45f))
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        assertEquals(0, finishCount)
        assertEquals(0, confirmedUrls)
        assertEquals(0, composeTestRule.onAllNodesWithTag("splash_ad_confirm_sheet").fetchSemanticsNodes().size)

        // Advance 500ms and tap immediate skip button
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("splash_ad_skip_button").performClick()
        composeTestRule.onNodeWithTag("splash_ad_skip_button").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        assertEquals(1, finishCount, "Single-fire finish guard must invoke onSkipOrFinish exactly once")
        assertTrue(skippedElapsedMs in 400L..600L, "Expected ~500ms elapsed skip time, got $skippedElapsedMs")
        assertEquals(0, confirmedUrls)
    }

    @Test
    fun countdown3sProgression_ctaConfirmSheetPausesTimerAndDismissSafelyResumes() {
        val creative = CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES[2]
        var finishCount = 0
        var skipCallbackCount = 0
        var ctaClickCount = 0
        val confirmedUrls = mutableListOf<String>()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val shadowSensorManager = Shadows.shadowOf(sensorManager)

        // Run with default autoAdvance = true + advanceTimeBy to mirror real Compose UI test usage
        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.SpringBlossom) {
                SplashAdScreen(
                    creative = creative,
                    countdownSeconds = 3,
                    onSkipOrFinish = { finishCount++ },
                    onExternalUrlConfirmed = { url -> confirmedUrls.add(url) },
                    onUserSkipped = { skipCallbackCount++ },
                    onCtaClicked = { ctaClickCount++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify zero motion sensors registered at t=0ms
        assertTrue(shadowSensorManager.listeners.isEmpty(), "Splash screen must register 0 SensorManager listeners")

        composeTestRule.onNodeWithTag("splash_ad_skip_button").assertTextContains("跳过 3s", substring = true)

        // t = 1000ms -> 跳过 2s
        composeTestRule.mainClock.advanceTimeBy(1_050L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_ad_skip_button").assertTextContains("跳过 2s", substring = true)
        assertEquals(0, finishCount)

        // Open bounded CTA confirmation sheet -> directly discoverable in merged semantics tree
        composeTestRule.onNodeWithTag("splash_ad_cta_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, ctaClickCount)
        composeTestRule.onNodeWithTag("splash_ad_confirm_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_ad_confirm_dismiss").assertIsDisplayed().assertHasClickAction()

        // Advancing 5,000ms while confirmation sheet is open MUST keep countdown paused
        composeTestRule.mainClock.advanceTimeBy(5_000L)
        composeTestRule.waitForIdle()
        assertEquals(0, finishCount, "Countdown must pause while confirmation sheet is open")
        assertTrue(confirmedUrls.isEmpty())

        // Dismiss confirmation sheet safely -> resumes remaining ~2,000ms without external jump
        composeTestRule.onNodeWithTag("splash_ad_confirm_dismiss").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag("splash_ad_confirm_sheet").fetchSemanticsNodes().size)
        assertTrue(confirmedUrls.isEmpty())

        // +1,000ms -> 跳过 1s
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_ad_skip_button").assertTextContains("跳过 1s", substring = true)
        assertEquals(0, finishCount)

        // Final +1,000ms (total >= 3,000ms active) -> auto-finishes cleanly
        composeTestRule.mainClock.advanceTimeBy(1_050L)
        composeTestRule.waitForIdle()
        assertEquals(1, finishCount)
        assertEquals(0, skipCallbackCount, "Auto-expiration must not count as manual skip")
        assertTrue(confirmedUrls.isEmpty())

        // Confirm 0 motion sensor listeners throughout entire lifecycle
        assertTrue(shadowSensorManager.listeners.isEmpty(), "Zero SensorManager listeners allowed during splash")
    }

    @Test
    fun confirmProceedButton_confirmsHttpsUrlOnceAndMaintainsSilentAudioInvariant() {
        val creative = CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES[0]
        val confirmedUrls = mutableListOf<String>()
        var finishCount = 0

        val audioTracksBefore = countActiveShadowAudioTracks()

        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.InkMist) {
                SplashAdScreen(
                    creative = creative,
                    countdownSeconds = 3,
                    onSkipOrFinish = { finishCount++ },
                    onExternalUrlConfirmed = { url -> confirmedUrls.add(url) }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("splash_ad_cta_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_ad_confirm_proceed").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(creative.landingUrl), confirmedUrls)
        assertEquals(1, finishCount)
        assertEquals(audioTracksBefore, countActiveShadowAudioTracks(), "SplashAdScreen must write 0 audio tracks")
    }

    @Test
    fun abnormalCountdownSeconds_zeroNegativeAnd99NormalizeTo3s_andEncodedApkUrlIsSuppressed() {
        val baseCreative = CuratedHouseSplashAdProvider.BUILT_IN_CREATIVES[0]
        var requestedCountdown by mutableStateOf(0)
        var currentCreative by mutableStateOf(baseCreative)
        val confirmedUrls = mutableListOf<String>()
        var finishCount = 0

        composeTestRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.InkMist) {
                SplashAdScreen(
                    creative = currentCreative,
                    countdownSeconds = requestedCountdown,
                    onSkipOrFinish = { finishCount++ },
                    onExternalUrlConfirmed = { url -> confirmedUrls.add(url) }
                )
            }
        }
        composeTestRule.waitForIdle()

        // countdownSeconds = 0, -5, 99 must all normalize to 3 ("跳过 3s"); valid 2 stays "跳过 2s"
        for ((index, candidateSeconds) in listOf(0, -5, 99).withIndex()) {
            composeTestRule.runOnIdle {
                currentCreative = baseCreative.copy(id = "norm_case_$index")
                requestedCountdown = candidateSeconds
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("splash_ad_skip_button")
                .assertIsDisplayed()
                .assertTextContains("跳过 3s", substring = true)
        }

        composeTestRule.runOnIdle {
            currentCreative = baseCreative.copy(id = "valid_2s_case")
            requestedCountdown = 2
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_ad_skip_button")
            .assertIsDisplayed()
            .assertTextContains("跳过 2s", substring = true)

        // Direct SplashAdScreen invocation with double-percent-encoded .apk URL must suppress external jump
        val finishBeforeApkAttempt = finishCount
        composeTestRule.runOnIdle {
            currentCreative = baseCreative.copy(
                id = "direct_apk_guard_case",
                landingUrl = "https://example.com/download/app%252eapk"
            )
            requestedCountdown = 3
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("splash_ad_cta_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_ad_confirm_proceed").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertTrue(confirmedUrls.isEmpty(), "Double-encoded .apk URL must never fire onExternalUrlConfirmed")
        assertEquals(0, composeTestRule.onAllNodesWithTag("splash_ad_confirm_sheet").fetchSemanticsNodes().size)
        assertEquals(finishBeforeApkAttempt, finishCount)
    }

    private fun countActiveShadowAudioTracks(): Int {
        return runCatching {
            val field = ShadowAudioTrack::class.java.getDeclaredField("audioTrackInstances")
            field.isAccessible = true
            (field.get(null) as? Collection<*>)?.count {
                (it as? AudioTrack)?.playState == AudioTrack.PLAYSTATE_PLAYING
            } ?: 0
        }.getOrDefault(0)
    }
}
