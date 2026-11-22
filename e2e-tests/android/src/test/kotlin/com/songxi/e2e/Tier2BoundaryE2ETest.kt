package com.songxi.e2e

import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tier2BoundaryE2ETest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val audioProbe = SongxiAndroidE2EHarness.PcmAudioProbe()
    private var activeScenario: ActivityScenario<Activity>? = null

    @Before
    fun setUp() {
        SongxiAndroidE2EHarness.prepareEnvironment(audioProbe)
    }

    @After
    fun tearDown() {
        SongxiAndroidE2EHarness.tearDownEnvironment(activeScenario, composeRule, audioProbe)
        activeScenario = null
    }

    // =========================================================================
    // F1 — Zero-Login Local-First Sanctuary & Gentle Onboarding (4 tests)
    // =========================================================================

    @Test
    fun t2_f01_01_repeated_recreate_after_onboarding_never_reprompts_card() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule, resetState = true)
        repeat(3) { cycle ->
            activeScenario!!.recreate()
            ShadowLooper.idleMainLooper()
            composeRule.waitForIdle()
            assertFalse(
                "onboarding_card must never reappear on recreate cycle #${cycle + 1}",
                SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card")
            )
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
        }
    }

    @Test
    fun t2_f01_02_reset_state_extra_cleanly_restores_fresh_onboarding_state() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val initial = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_dusk_pine")
        initial.close()

        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = t0 + 180_000L,
            resetState = true
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_card")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "onboarding_enter_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
        val badgeText = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue(
            "RESET_STATE=true must restore default ink_mist ('水墨') skin, got: '$badgeText'",
            badgeText.contains("水墨")
        )
    }

    @Test
    fun t2_f01_03_rapid_double_tap_on_onboarding_enter_is_idempotent() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 15),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_enter_button")
        val btn = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "onboarding_enter_button")
        runCatching { btn.performClick() }
        runCatching { btn.performClick() }

        ShadowLooper.idleMainLooper()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertFalse(
            "onboarding_card must close cleanly after rapid double-tap",
            SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
    }

    @Test
    fun t2_f01_04_back_press_on_onboarding_or_home_root_handles_gracefully_without_exception() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 20),
            resetState = true
        )
        activeScenario!!.onActivity { activity ->
            @Suppress("DEPRECATION")
            activity.onBackPressed()
            assertNull(
                "Back-press must never spawn OAuth/AccountManager/WebView login intents",
                Shadows.shadowOf(activity).peekNextStartedActivity()
            )
        }
        ShadowLooper.idleMainLooper()
    }

    // =========================================================================
    // F2 — Editorial Framed Splash Ad UI (开屏雅鉴) & Safe CTA (4 tests)
    // =========================================================================

    @Test
    fun t2_f02_01_skip_button_clickable_at_t0ms_with_at_least_48dp_touch_target() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        // Strict t = 0ms assertion before any clock advancement
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_skip_button")
        SongxiAndroidE2EHarness.assertMinTouchBounds(
            composeRule = composeRule,
            tag = "splash_ad_skip_button",
            minWidth = 48.dp,
            minHeight = 48.dp
        )
        val skipNode = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_skip_button")
            .fetchSemanticsNode()
        assertTrue(
            "splash_ad_skip_button must expose clickable action at t=0ms",
            skipNode.config.contains(SemanticsActions.OnClick)
        )
    }

    @Test
    fun t2_f02_02_tapping_splash_backdrop_is_strictly_inert_anti_hotzone_trap() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_backdrop")

        // Tap upper-left artwork area away from bounded CTA pill and top-right skip button
        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_backdrop")
            .performTouchInput {
                click(Offset(width * 0.22f, height * 0.30f))
            }
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        assertFalse(
            "Tapping splash_ad_backdrop must NEVER open confirmation sheet (anti-full-screen-hotzone)",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_confirm_sheet")
        )
        activeScenario!!.onActivity { activity ->
            assertNull(
                "Tapping splash_ad_backdrop must NEVER trigger external browser intent",
                Shadows.shadowOf(activity).peekNextStartedActivity()
            )
        }
    }

    @Test
    fun t2_f02_03_zero_motion_sensor_listeners_and_muted_audio_during_splash() {
        audioProbe.reset()
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")

        // Hardware accelerometer & gyroscope were registered in setUp(), so 0 listeners is 100% non-vacuous!
        assertEquals(
            "Splash ad MUST register strictly 0 motion sensor listeners (禁用摇一摇)",
            0,
            SongxiAndroidE2EHarness.activeSensorListenerCount()
        )

        // Dispatch violent shake events and verify zero external jump or CTA sheet
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 18f, -15f, 12f, sampleCount = 3, intervalMs = 50L)
        assertFalse(
            "Shaking device during splash must NEVER trigger confirmation sheet",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_confirm_sheet")
        )
        activeScenario!!.onActivity { activity ->
            assertNull(
                "Shaking device during splash must NEVER start any Activity",
                Shadows.shadowOf(activity).peekNextStartedActivity()
            )
        }

        // Advance through full splash duration and verify 0 non-silent PCM chunks
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 2_900L)
        assertEquals(
            "Splash ad must stay strictly muted (0 non-silent PCM audio chunks)",
            0,
            audioProbe.nonSilentChunks.get()
        )
        assertTrue(
            "AudioManager music stream must be inactive during splash",
            SongxiAndroidE2EHarness.isAudioMutedOrInactive()
        )
    }

    @Test
    fun t2_f02_04_countdown_3000ms_auto_dismisses_and_confirm_sheet_dismiss_cancels_safely() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val setup = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        setup.close()

        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = t0 + 10_000L,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_cta_button").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_confirm_sheet")

        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_confirm_dismiss").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()
        assertFalse(
            "Dismissing confirmation sheet must close splash_ad_confirm_sheet",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_confirm_sheet")
        )
        activeScenario!!.onActivity { activity ->
            assertNull(
                "Dismissing confirmation sheet must NOT start any external Activity",
                Shadows.shadowOf(activity).peekNextStartedActivity()
            )
        }

        // Advance past 3000ms countdown and verify automatic transition to Home sanctuary
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 3_400L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertFalse(
            "splash_ad_screen must auto-dismiss after 3000ms countdown",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
    }

    // =========================================================================
    // F3 — 7-Gate Calm Splash-Ad Policy & Privacy Controls (4 tests)
    // =========================================================================

    @Test
    fun t2_f03_01_gate5_daily_cap_allows_three_impressions_blocks_fourth_resets_next_day() {
        // Launch #1 at 01:00 on 2026-11-18: Gate 1 grace bypasses splash
        val s1 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 1, 0)
        )
        s1.close()

        // Launches #2 (05:05), #3 (09:10), #4 (13:15) on same day (>4h apart): impressions #1, #2, #3
        val eligibleHours = listOf(5 to 5, 9 to 10, 13 to 15)
        for ((idx, hm) in eligibleHours.withIndex()) {
            val sc = SongxiAndroidE2EHarness.launchMain(
                composeRule = composeRule,
                simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, hm.first, hm.second),
                resetState = false,
                autoAdvanceClock = false
            )
            assertTrue(
                "Expected daily splash impression #${idx + 1} at ${hm.first}:${hm.second}",
                SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
            )
            SongxiAndroidE2EHarness.dismissSplashIfPresent(composeRule)
            composeRule.mainClock.autoAdvance = true
            composeRule.waitForIdle()
            sc.close()
        }

        // Launch #5 at 17:20 on same day (>4h since 13:15): MUST bypass due to Gate 5 (<= 3/day cap)!
        val capped = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 17, 20),
            resetState = false,
            autoAdvanceClock = false
        )
        assertFalse(
            "4th splash on same calendar day (17:20) MUST be blocked by Gate 5 <= 3/day cap",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        capped.close()

        // Launch #6 next calendar day (2026-11-19T09:30): daily cap resets and splash shows again!
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 9, 30),
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    @Test
    fun t2_f03_02_gate6_cooldown_boundary_3h59m_bypasses_vs_4h01m_shows() {
        val s1 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 3, 0)
        )
        s1.close()

        // Impression #1 at 08:00
        val imp1 = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 8, 0),
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        SongxiAndroidE2EHarness.dismissSplashIfPresent(composeRule)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        imp1.close()

        // +3h59m (11:59): MUST bypass due to Gate 6 (>= 4h cooldown)
        val at3h59m = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 11, 59),
            resetState = false,
            autoAdvanceClock = false
        )
        assertFalse(
            "Launch at +3h59m (11:59) must bypass splash_ad_screen due to 4h cooldown",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        at3h59m.close()

        // +4h01m (12:01): MUST show splash
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 12, 1),
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    @Test
    fun t2_f03_03_gate4_warm_background_switch_29m_suppresses_vs_31m_eligible() {
        // Launch #1 at 08:00 (0 prior splash impressions, so Gate 6 4h cooldown is NOT active!)
        val t0800 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 8, 0)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = t0800
        )

        // Warm resume after 29 minutes (< 30m): ONLY Gate 4 suppresses splash!
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.simulateWarmResumeAfter(
            scenario = activeScenario!!,
            composeRule = composeRule,
            baseEpochMs = t0800,
            elapsedMillis = 29L * 60_000L
        )
        assertFalse(
            "Gate 4 (< 30m warm resume at 29m) MUST suppress splash_ad_screen",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )

        // Warm resume after 31 minutes (>= 30m) from 08:29: Gate 4 allows splash!
        val t0829 = t0800 + 29L * 60_000L
        SongxiAndroidE2EHarness.simulateWarmResumeAfter(
            scenario = activeScenario!!,
            composeRule = composeRule,
            baseEpochMs = t0829,
            elapsedMillis = 31L * 60_000L
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    @Test
    fun t2_f03_04_gate2_active_or_paused_player_immunity_and_force_preview_override() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 8, 0)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = t0
        )
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        // Warm background for 45 minutes (> 30m & 0 prior impressions) while player_screen is active
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.simulateWarmResumeAfter(
            scenario = activeScenario!!,
            composeRule = composeRule,
            baseEpochMs = t0,
            elapsedMillis = 45L * 60_000L
        )
        assertFalse(
            "Gate 2 MUST protect active/paused player session from splash interruption",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        // Also verify FORCE_SPLASH_PREVIEW overrides policy gates
        activeScenario!!.close()
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = t0 + 46L * 60_000L,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    // =========================================================================
    // F4 — Zero-Membership & 100% Unlocked Content Invariant (3 tests)
    // =========================================================================

    @Test
    fun t2_f04_01_full_five_tab_ui_semantics_sweep_has_zero_vip_or_pricing_strings() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (tabTag in SongxiAndroidE2EHarness.ALL_NAV_TABS) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, tabTag)
            val text = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
            for (forbidden in SongxiAndroidE2EHarness.FORBIDDEN_VIP_UI_PATTERNS) {
                assertFalse(
                    "Tab '$tabTag' contains forbidden VIP/paywall/legacy string '$forbidden' in: $text",
                    text.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun t2_f04_02_apk_binary_dex_and_manifest_zero_billing_or_inapp_ad_classes() {
        val releaseApk = SongxiAndroidE2EHarness.releaseApkFile()
        SongxiAndroidE2EHarness.assertCleanManifestPermissions(releaseApk)
        val hits = SongxiAndroidE2EHarness.scanApkForForbiddenStrings(releaseApk)
        assertTrue(
            "Release APK DEX/resources must contain 0 VIP/billing/interstitial/rewarded strings, found: $hits",
            hits.isEmpty()
        )
    }

    @Test
    fun t2_f04_03_repeated_booklet_and_dark_skin_combinations_have_zero_trial_expiry() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0)
        val init = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        init.close()

        // Advance +30 days (verifying zero "7-day free trial expired" timebombs)
        val tPlus30d = SongxiAndroidE2EHarness.epochAtLocal(2026, 12, 18, 10, 0)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = tPlus30d
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_dusk_pine")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_3")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    // =========================================================================
    // F5 — 5-Dimensional Illustration × Shan-Shui Skin Engine (3 tests)
    // =========================================================================

    @Test
    fun t2_f05_01_all_four_skins_ban_pure_white_and_pure_black_canvas_grounds() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (skinTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")

            val stats = SongxiAndroidE2EHarness.analyzeBitmapColors(
                SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
            )
            assertNotEquals("Skin '$skinTag' dominant ground must NOT be pure #FFFFFF", 0xFFFFFF, stats.dominantRgb)
            assertNotEquals("Skin '$skinTag' dominant ground must NOT be pure #000000", 0x000000, stats.dominantRgb)
        }
    }

    @Test
    fun t2_f05_02_all_four_skins_maintain_wcag_aa_contrast_across_home_stage() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (skinTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")

            val fullBmp = SongxiAndroidE2EHarness.captureDecorBitmap(activeScenario!!, composeRule)
            val stats = SongxiAndroidE2EHarness.analyzeBitmapColors(fullBmp)
            val inkRgb = if (skinTag == "skin_card_dusk_pine") stats.lightestRgb else stats.darkestRgb
            val contrast = SongxiAndroidE2EHarness.wcagContrastRatio(inkRgb, stats.dominantRgb)
            assertTrue(
                "Skin '$skinTag' primary ink vs paper ground contrast ($contrast) must satisfy WCAG AA >= 4.5:1",
                contrast >= 4.5
            )
        }
    }

    @Test
    fun t2_f05_03_all_custom_canvases_maintain_non_zero_bounds_and_valid_cjk_labels() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.assertMinTouchBounds(composeRule, "home_shanshui_backdrop", 24.dp, 24.dp)
        assertFalse(
            "Home text must not contain replacement char \\uFFFD",
            SongxiAndroidE2EHarness.collectAllScreenText(composeRule).contains('\uFFFD')
        )

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.assertMinTouchBounds(composeRule, "bodymap_canvas", 24.dp, 24.dp)

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.assertMinTouchBounds(composeRule, "player_figure_canvas", 24.dp, 24.dp)
        SongxiAndroidE2EHarness.assertMinTouchBounds(composeRule, "player_breathing_halo", 24.dp, 24.dp)

        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.assertMinTouchBounds(composeRule, "completion_ripple_canvas", 24.dp, 24.dp)
    }

    // =========================================================================
    // F6 — 4 Free Built-In Skins, Live Gallery & Auto-Night Pairing (4 tests)
    // =========================================================================

    @Test
    fun t2_f06_01_auto_night_on_at_19_59_keeps_day_skin_vs_20_01_activates_dusk_pine() {
        val setup = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 14, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "skin_auto_night_switch")
        }
        setup.close()

        // 19:59 -> daytime qinglv_celadon stays active
        val at1959 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 19, 59)
        )
        val lum1959 = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(at1959, composeRule, "home_shanshui_backdrop")
        ).averageLuminance
        assertTrue("At 19:59 auto-night must keep light daytime skin (lum=$lum1959 > 0.50)", lum1959 > 0.50)
        at1959.close()

        // 20:01 -> evening dusk_pine activates automatically
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 20, 1)
        )
        val lum2001 = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        ).averageLuminance
        assertTrue("At 20:01 auto-night must switch to dark dusk_pine (lum=$lum2001 < 0.25)", lum2001 < 0.25)
    }

    @Test
    fun t2_f06_02_auto_night_on_early_morning_06_30_stays_night_and_09_30_restores_day() {
        val setup = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 14, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "skin_auto_night_switch")
        }
        setup.close()

        val at0630 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 6, 30)
        )
        val lum0630 = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(at0630, composeRule, "home_shanshui_backdrop")
        ).averageLuminance
        assertTrue("At 06:30 pre-dawn auto-night must keep dark dusk_pine (lum=$lum0630 < 0.25)", lum0630 < 0.25)
        at0630.close()

        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 9, 30)
        )
        val lum0930 = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        ).averageLuminance
        assertTrue("At 09:30 daytime qinglv_celadon must restore (lum=$lum0930 > 0.50)", lum0930 > 0.50)
    }

    @Test
    fun t2_f06_03_auto_night_off_at_21_30_preserves_manual_daytime_skin() {
        val setup = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 11, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_spring_blossom")
        if (SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "skin_auto_night_switch")
        }
        setup.close()

        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 21, 30)
        )
        val lum2130 = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        ).averageLuminance
        assertTrue(
            "With skin_auto_night_switch OFF at 21:30, manual spring_blossom light skin must remain active (lum=$lum2130 > 0.50)",
            lum2130 > 0.50
        )
    }

    @Test
    fun t2_f06_04_rapid_twelve_tap_four_skin_cycling_stress_causes_zero_crash() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")

        repeat(3) {
            for (skinTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            }
        }

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val badge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue("After 12 rapid skin switches, spring_blossom ('春') must be active, got: '$badge'", badge.contains("春"))
    }

    // =========================================================================
    // F7 — Context-Aware Home (今日一息) & Quick Micro-Shortcuts (4 tests)
    // =========================================================================

    @Test
    fun t2_f07_01_home_stage_maintains_calm_single_hero_layout_and_zero_ad_tags() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        assertEquals(
            "Home must present exactly 1 focal home_hero_card",
            1,
            SongxiAndroidE2EHarness.countNodesWithTag(composeRule, "home_hero_card")
        )
        assertFalse(
            "Home sanctuary must contain zero splash_ad_screen nodes",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        assertFalse(
            "Home sanctuary must contain zero splash_ad_badge nodes",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_badge")
        )
    }

    @Test
    fun t2_f07_02_rapid_scene_cycling_desk_tea_bed_desk_restores_deterministic_hero() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_desk")
        val initialDesk = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_tea")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_bed")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_desk")
        val restoredDesk = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        assertEquals("Cycling desk -> tea -> bed -> desk must deterministically restore desk hero card", initialDesk, restoredDesk)
    }

    @Test
    fun t2_f07_03_exiting_player_via_close_preserves_selected_posture_scene() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_bed")
        val bedHero = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
        val afterCloseHero = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")
        assertEquals("Returning via player_close_button must preserve selected bed scene", bedHero, afterCloseHero)
    }

    @Test
    fun t2_f07_04_tapping_home_active_skin_badge_reaches_skin_switch_within_two_taps() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        val initialBadge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")

        // Tap #1: home_active_skin_badge either cycles skin directly or navigates to skins_screen
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_active_skin_badge")
        if (SongxiAndroidE2EHarness.tagExists(composeRule, "skins_screen")) {
            // Tap #2: select qinglv_celadon on skins_screen
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        }
        val updatedBadge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertNotEquals("home_active_skin_badge must switch/reach skin change within <= 2 taps", initialBadge, updatedBadge)
    }

    // =========================================================================
    // F8 — 6-Zone Body Tension Map & Unlimited Custom Composer (身体寻音) (4 tests)
    // =========================================================================

    @Test
    fun t2_f08_01_fourth_zone_selection_enforces_max_three_zones_invariant() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        for (z in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS) {
            if (SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, z)) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, z)
            }
        }

        for (z in listOf("bodymap_zone_neck", "bodymap_zone_shoulder", "bodymap_zone_lumbar", "bodymap_zone_wrist")) {
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, z)
        }

        val activeCount = SongxiAndroidE2EHarness.countSelectedBodyMapZones(composeRule)
        assertTrue("Selecting a 4th zone must cap/shift active zones to <= 3 (got $activeCount)", activeCount in 1..3)

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    @Test
    fun t2_f08_02_zero_zone_deselection_boundary_guards_against_empty_pose_crash() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        for (z in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS) {
            if (SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, z)) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, z)
            }
        }

        // Attempt compose start: must either keep >=1 default zone, disable button, or fallback safely without crash
        runCatching { SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button") }
        assertTrue(
            "0-zone boundary must remain stable on bodymap_screen or launch safe fallback player_screen",
            SongxiAndroidE2EHarness.tagExists(composeRule, "bodymap_screen") ||
                SongxiAndroidE2EHarness.tagExists(composeRule, "player_screen")
        )
    }

    @Test
    fun t2_f08_03_toggling_same_zone_twice_cleanly_selects_and_deselects() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "bodymap_zone_neck")) {
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_neck")
        }
        val beforeEye = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "bodymap_zone_eye")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_eye")
        val toggledEye = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "bodymap_zone_eye")
        assertNotEquals("First tap on bodymap_zone_eye must toggle state", beforeEye, toggledEye)

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_eye")
        val revertedEye = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "bodymap_zone_eye")
        assertEquals("Second tap on bodymap_zone_eye must revert selection cleanly", beforeEye, revertedEye)
    }

    @Test
    fun t2_f08_04_all_six_individual_zones_across_durations_compose_valid_routines() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        val durations = listOf("bodymap_duration_2", "bodymap_duration_4", "bodymap_duration_6")

        for ((idx, zoneTag) in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS.withIndex()) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
            for (z in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS) {
                if (z != zoneTag && SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, z)) {
                    SongxiAndroidE2EHarness.scrollToAndClick(composeRule, z)
                }
            }
            if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, zoneTag)) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, zoneTag)
            }
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, durations[idx % durations.size])
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")

            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
            val playerCopy = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
            assertTrue("Composed routine for $zoneTag must render non-empty pose cue", playerCopy.length >= 4)
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        }
    }

    // =========================================================================
    // F9 — Free Classical Curated Booklets (四时雅集) (3 tests)
    // =========================================================================

    @Test
    fun t2_f09_01_aborting_booklet_mid_session_does_not_falsely_increment_completion_or_scold() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val beforeText = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_1")

        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_1")
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 5_000L)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val afterText = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_1")
        assertEquals("Aborting booklet mid-session must NOT falsely increment progress", beforeText, afterText)

        val screenText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (scold in listOf("半途而废", "未完成惩罚", "打卡失败")) {
            assertFalse("Aborting routine must show zero punitive scolding ('$scold')", screenText.contains(scold))
        }
    }

    @Test
    fun t2_f09_02_repracticing_completed_booklet_remains_open_and_repeatable() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_2")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_2")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    @Test
    fun t2_f09_03_booklet_progress_persists_across_browsing_other_booklets_and_recreate() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val initial0 = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_0")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val completed0 = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_0")
        assertNotEquals("booklet_card_0 progress must update after completion", initial0, completed0)

        // Browse booklet_card_3 mid-way and recreate Activity
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_3")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        assertEquals(
            "booklet_card_0 progress must persist across browsing other booklets and recreate()",
            completed0,
            SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_0")
        )
    }

    // =========================================================================
    // F10 — Guided Stretch Player (4s/6s Halo & 闭眼余光态) (4 tests)
    // =========================================================================

    @Test
    fun t2_f10_01_exact_4000ms_6000ms_phase_boundaries_3900_inhale_4100_exhale_10100_inhale() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        // t = 3900ms -> still in 4000ms inhale window
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 3_900L)
        val at3900 = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("At t=3900ms phase must still be '吸气', got '$at3900'", at3900.contains("吸气"))

        // t = 4100ms -> transitioned into 6000ms exhale window
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 200L)
        val at4100 = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("At t=4100ms phase must be '呼气', got '$at4100'", at4100.contains("呼气"))

        // t = 9900ms -> still in exhale window
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 5_800L)
        val at9900 = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("At t=9900ms phase must still be '呼气', got '$at9900'", at9900.contains("呼气"))

        // t = 10100ms -> wrapped cleanly into next 4000ms inhale cycle
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 200L)
        val at10100 = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("At t=10100ms phase must wrap back to '吸气', got '$at10100'", at10100.contains("吸气"))
    }

    @Test
    fun t2_f10_02_prev_pose_on_first_pose_clamps_safely_and_next_on_last_pose_completes() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")

        // Tap prev twice at Pose 0: must clamp safely without IndexOutOfBoundsException
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_prev_pose_button")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_prev_pose_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        // Tap next through all poses to completion_screen
        for (step in 0 until 12) {
            if (SongxiAndroidE2EHarness.tagExists(composeRule, "completion_screen")) break
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_next_pose_button")
        }
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "completion_screen")
    }

    @Test
    fun t2_f10_03_pausing_player_freezes_breath_and_timer_across_10s_clock_advance() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 1_500L)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_pause_resume_button")
        val pausedPhase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        val pausedScreenSnapshot = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 10_000L)
        val after10sPhase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        val after10sScreenSnapshot = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)

        assertEquals("Breath phase must stay frozen while paused", pausedPhase, after10sPhase)
        assertEquals("Countdown/pose state must stay frozen across 10s while paused", pausedScreenSnapshot, after10sScreenSnapshot)
    }

    @Test
    fun t2_f10_04_gentle_cue_conciseness_and_keep_screen_on_cleared_on_exit() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        assertTrue(
            "FLAG_KEEP_SCREEN_ON must be enabled during active player",
            SongxiAndroidE2EHarness.hasKeepScreenOnFlag(activeScenario!!)
        )

        val playerText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (harsh in listOf("坚持住别放弃", "快点完成", "燃烧卡路里")) {
            assertFalse("Player copy must never shout harsh fitness drill copy ('$harsh')", playerText.contains(harsh))
        }

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
        assertFalse(
            "FLAG_KEEP_SCREEN_ON must be cleared/released upon exiting player_screen",
            SongxiAndroidE2EHarness.hasKeepScreenOnFlag(activeScenario!!)
        )
    }

    // =========================================================================
    // F11 — Procedural Harmonic Audio, Gentle Haptics & Flip-to-Pause (4 tests)
    // =========================================================================

    @Test
    fun t2_f11_01_accelerometer_listener_strictly_isolated_to_active_player_lifecycle() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (tab in listOf("nav_tab_home", "nav_tab_bodymap", "nav_tab_skins", "nav_tab_journal", "nav_tab_settings")) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, tab)
            assertEquals(
                "Sensor listener count MUST be 0 on non-player tab '$tab'",
                0,
                SongxiAndroidE2EHarness.activeSensorListenerCount()
            )
        }

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        assertTrue(
            "Entering player_screen must register >= 1 accelerometer listener for flip-to-pause",
            SongxiAndroidE2EHarness.activeSensorListenerCount() >= 1
        )

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        assertEquals(
            "Closing player_screen must unregister accelerometer listener immediately",
            0,
            SongxiAndroidE2EHarness.activeSensorListenerCount()
        )
    }

    @Test
    fun t2_f11_02_accelerometer_listener_unregistered_automatically_on_completion_screen() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        assertTrue(
            "Player must have active accelerometer listener during routine",
            SongxiAndroidE2EHarness.activeSensorListenerCount() >= 1
        )

        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        assertEquals(
            "Reaching completion_screen must automatically unregister accelerometer listener",
            0,
            SongxiAndroidE2EHarness.activeSensorListenerCount()
        )
    }

    @Test
    fun t2_f11_03_upright_desk_tilt_minus_3g_does_not_false_trigger_flip_pause() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        // Slight desk/hand tilt (z = -3.0 m/s^2) must NOT trigger flip pause
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 9.2f, -3.0f)
        assertFalse(
            "Upright tilt (z = -3.0) must NOT false-trigger player_paused_by_flip_banner",
            SongxiAndroidE2EHarness.tagExists(composeRule, "player_paused_by_flip_banner")
        )

        // True face-down onto desk (z = -9.4 m/s^2) MUST trigger flip pause
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0.2f, -9.4f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")
    }

    @Test
    fun t2_f11_04_silent_haptic_only_mode_mutes_pcm_audio_while_preserving_haptic_wave() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_silent")
        audioProbe.reset()
        SongxiAndroidE2EHarness.resetHapticLedger()

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 4_300L)
        assertEquals(
            "静默仅微震 (player_sound_silent) must emit 0 non-silent PCM audio chunks",
            0,
            audioProbe.nonSilentChunks.get()
        )
        assertTrue(
            "静默仅微震 must preserve gentle breath haptic waveform while audio is muted",
            SongxiAndroidE2EHarness.hasRecordedHapticPulse()
        )
    }

    // =========================================================================
    // F12 — Completion Water-Ripple Ritual & Somatic Check-In (一息成) (4 tests)
    // =========================================================================

    @Test
    fun t2_f12_01_finishing_without_selecting_feeling_tag_saves_session_cleanly() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val initialSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        // Do NOT tap any completion_feeling_* chip — feeling check-in must be optional!
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val afterSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")

        assertNotEquals("Finishing without selecting a feeling chip must still save session", initialSummary, afterSummary)
        assertTrue("Journal summary must record 1 session, got '$afterSummary'", afterSummary.contains("1"))
    }

    @Test
    fun t2_f12_02_rapid_double_tap_on_completion_finish_records_single_session_only() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        composeRule.mainClock.autoAdvance = false
        val finishBtn = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "completion_finish_button")
        runCatching { finishBtn.performClick() }
        runCatching { finishBtn.performClick() }
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val summary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertTrue("Rapid double-tap on finish must log session ('1'), got '$summary'", summary.contains("1"))
        assertFalse(
            "Rapid double-tap on finish must NOT double-count as 2 sessions ('2 息' / '2息'), got '$summary'",
            Regex("2\\s*息|2\\s*次").containsMatchIn(summary)
        )
    }

    @Test
    fun t2_f12_03_aborting_player_before_completion_records_zero_false_sessions() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val baselineSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 5_000L)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val afterAbortSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertEquals("Aborting routine before completion must record 0 false sessions", baselineSummary, afterAbortSummary)
    }

    @Test
    fun t2_f12_04_switching_skin_updates_completion_poetic_verse_on_ripple_stage() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_ink_mist")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        val inkCompletionCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "completion_screen")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_dusk_pine")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        val duskCompletionCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "completion_screen")

        assertNotEquals(
            "Completion poetic verse must adapt to active SongxiSkin (ink_mist vs dusk_pine)",
            inkCompletionCopy,
            duskCompletionCopy
        )
    }

    // =========================================================================
    // F13 — Low-Intrusion Sedentary Reminder Scheduler (微风提醒) (4 tests)
    // =========================================================================

    @Test
    fun t2_f13_01_lunch_quiet_hours_12_00_to_13_30_suppress_vs_11_55_and_13_35_allow() {
        // 12:30 Wednesday (inside 12:00..13:30 lunch quiet window) -> 0 notifications
        val lunch = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 12, 30)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_enable_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_enable_switch")
        }
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_lunch_quiet_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_lunch_quiet_switch")
        }
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertEquals(
            "12:30 lunch quiet hour MUST suppress sedentary reminder notification",
            0,
            SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule)
        )
        lunch.close()

        // 11:55 pre-lunch -> allowed (>= 1 notification)
        val preLunch = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 11, 55)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertTrue(
            "11:55 weekday must allow gentle sedentary reminder",
            SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule) >= 1
        )
        preLunch.close()

        // 13:35 post-lunch next day -> allowed (>= 1 notification)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 13, 35)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertTrue(
            "13:35 post-lunch weekday must allow gentle sedentary reminder",
            SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule) >= 1
        )
    }

    @Test
    fun t2_f13_02_night_quiet_hours_20_00_to_09_00_and_weekend_auto_silence_suppress() {
        val init = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        for (sw in listOf("reminder_enable_switch", "reminder_night_quiet_switch", "reminder_weekend_quiet_switch")) {
            if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, sw)) {
                SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, sw)
            }
        }
        init.close()

        val suppressedEpochs = listOf(
            "Wed 22:15 night" to SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 22, 15),
            "Thu 07:30 early morning" to SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 7, 30),
            "Sat 15:00 weekend" to SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 21, 15, 0)
        )
        for ((label, epoch) in suppressedEpochs) {
            val sc = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = false, simulatedEpochMs = epoch)
            SongxiAndroidE2EHarness.clearPostedNotifications()
            assertEquals(
                "Quiet window ($label) MUST suppress reminder notifications",
                0,
                SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule)
            )
            sc.close()
        }

        // Thu 09:05 & Thu 19:55 -> allowed
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 9, 5)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertTrue(
            "Weekday 09:05 must allow gentle reminder",
            SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule) >= 1
        )
    }

    @Test
    fun t2_f13_03_mute_today_suppresses_current_date_only_and_daily_cap_blocks_excess() {
        val wed15 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 15, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_enable_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_enable_switch")
        }
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_mute_today_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_mute_today_switch")
        }
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertEquals(
            "reminder_mute_today_switch ON must suppress nudges for today's ISO date",
            0,
            SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule)
        )
        wed15.close()

        // Next day (2026-11-19T15:00): mute_today expired, trigger 5 times -> capped at 1..3
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 15, 0)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        var cumulativeDeliveries = 0
        repeat(5) {
            SongxiAndroidE2EHarness.clearPostedNotifications()
            cumulativeDeliveries += SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule)
        }
        assertTrue(
            "Next day must resume nudges (>=1) while enforcing gentle daily cap (<=3), got $cumulativeDeliveries",
            cumulativeDeliveries in 1..3
        )
    }

    @Test
    fun t2_f13_04_disabling_reminder_enable_switch_cancels_scheduled_work() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        if (SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_enable_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_enable_switch")
        }
        val activeWork = SongxiAndroidE2EHarness.activeScheduledWorkInfos()
        assertTrue(
            "Turning reminder_enable_switch OFF must cancel scheduled periodic WorkManager tasks, found: $activeWork",
            activeWork.isEmpty()
        )
    }

    // =========================================================================
    // F14 — Streak-Free Water-Ripple Journal & Shareable Card (4 tests)
    // =========================================================================

    @Test
    fun t2_f14_01_non_consecutive_days_1_and_4_display_zero_streak_or_broken_shame_copy() {
        // Practice on Nov 1
        val day1 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 1, 10, 0)
        )
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")
        day1.close()

        // Skip Nov 2 & Nov 3; practice on Nov 4
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 4, 10, 0)
        )
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val journalCopy = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (shame in SongxiAndroidE2EHarness.FORBIDDEN_STREAK_SHAME_PATTERNS) {
            assertFalse(
                "Journal must NEVER display streak/broken-streak guilt copy ('$shame'), got: $journalCopy",
                journalCopy.contains(shame)
            )
        }
    }

    @Test
    fun t2_f14_02_fresh_empty_month_renders_calm_xuan_paper_state_with_zero_nan() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule, resetState = true)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_calendar_grid")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_monthly_summary")
        val text = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (corrupt in listOf("NaN", "Infinity", "undefined", "null")) {
            assertFalse("Empty journal month must render cleanly without '$corrupt'", text.contains(corrupt))
        }
    }

    @Test
    fun t2_f14_03_multiple_sessions_same_day_accumulate_ripples_and_sum_minutes() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule, resetState = true)
        repeat(2) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_1")
            SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")
        }

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val summary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertTrue(
            "Completing 2 routines on same day must accumulate session count ('2') in summary, got: '$summary'",
            summary.contains("2")
        )
    }

    @Test
    fun t2_f14_04_dismissing_share_preview_dialog_returns_cleanly_to_journal() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "journal_share_card_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_share_preview_dialog")

        val dismissMatcher = SemanticsMatcher("share_dialog_dismiss") { node ->
            node.config.contains(SemanticsActions.OnClick) &&
                run {
                    val txt = (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: "") +
                        (node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("") ?: "")
                    txt.contains("关闭") || txt.contains("取消") || txt.contains("返回") || txt.contains("收起")
                }
        }
        val nodes = composeRule.onAllNodes(dismissMatcher, useUnmergedTree = false)
        if (nodes.fetchSemanticsNodes().isNotEmpty()) {
            nodes[0].performClick()
        } else {
            activeScenario!!.onActivity { activity ->
                @Suppress("DEPRECATION")
                activity.onBackPressed()
            }
        }
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        assertFalse(
            "journal_share_preview_dialog must dismiss cleanly",
            SongxiAndroidE2EHarness.tagExists(composeRule, "journal_share_preview_dialog")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_screen")
    }

    // =========================================================================
    // F15 — Standalone Android APK Packaging, Visual Captures & Suite (3 tests)
    // =========================================================================

    @Test
    fun t2_f15_01_apk_manifest_permissions_strictly_minimal_zero_invasive_trackers() {
        SongxiAndroidE2EHarness.assertCleanManifestPermissions(SongxiAndroidE2EHarness.releaseApkFile())
        SongxiAndroidE2EHarness.assertCleanManifestPermissions(SongxiAndroidE2EHarness.debugApkFile())
    }

    @Test
    fun t2_f15_02_roborazzi_four_skin_pngs_have_four_pairwise_distinct_pixel_hashes() {
        val pngMap = SongxiAndroidE2EHarness.discoverFourSkinPngs()
        assertEquals(
            "Expected all 4 skin keys (ink_mist, qinglv_celadon, dusk_pine, spring_blossom)",
            setOf("ink_mist", "qinglv_celadon", "dusk_pine", "spring_blossom"),
            pngMap.keys
        )
        SongxiAndroidE2EHarness.assertValidAndDistinctSkinPngs()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun t2_f15_03_narrow_360dp_viewport_keeps_home_start_and_bottom_nav_unclipped() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "bottom_nav_bar")

        var screenWidthPx = 0
        var screenHeightPx = 0
        activeScenario!!.onActivity { activity ->
            screenWidthPx = activity.window.decorView.width
            screenHeightPx = activity.window.decorView.height
        }

        val startBounds = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "home_start_hero_button")
            .fetchSemanticsNode().boundsInRoot
        val navBounds = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "bottom_nav_bar")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(
            "home_start_hero_button ($startBounds) must fit horizontally within 360dp screen ($screenWidthPx px)",
            startBounds.left >= -1f && startBounds.right <= screenWidthPx + 1f && startBounds.height > 20f
        )
        assertTrue(
            "bottom_nav_bar ($navBounds) must remain visible and unclipped on 360x640dp viewport ($screenWidthPx x $screenHeightPx)",
            navBounds.height > 24f && navBounds.bottom <= screenHeightPx + 2f
        )
    }
}
