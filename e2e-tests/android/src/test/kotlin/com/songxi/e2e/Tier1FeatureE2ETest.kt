package com.songxi.e2e

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
class Tier1FeatureE2ETest {

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
    fun t1_f01_01_fresh_launch_displays_gentle_onboarding_and_sanctuary_promise() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0),
            resetState = true
        )
        assertFalse(
            "First cold launch must not display splash_ad_screen",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_card")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_enter_button")

        val copy = SongxiAndroidE2EHarness.readNodeText(composeRule, "onboarding_card")
        assertTrue(
            "Onboarding card must mention local-first / zero-account / splash-ad-only sanctuary promise, got: $copy",
            copy.contains("本地") || copy.contains("开屏") || copy.contains("松息") || copy.contains("无账号") || copy.contains("离线")
        )
    }

    @Test
    fun t1_f01_02_onboarding_enter_transitions_directly_to_home_without_login() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 5),
            resetState = true
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_enter_button")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "onboarding_enter_button")

        assertFalse(
            "onboarding_card must dismiss after clicking onboarding_enter_button",
            SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")

        val allText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (loginTerm in listOf("手机号", "验证码", "输入密码", "注册登录", "微信登录")) {
            assertFalse("Sanctuary must never prompt login wall ('$loginTerm')", allText.contains(loginTerm))
        }
    }

    @Test
    fun t1_f01_03_onboarding_seen_persists_across_activity_recreate_and_relaunch() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 10)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = t0
        )
        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        assertFalse(
            "onboarding_card must not reappear after Activity.recreate()",
            SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")

        activeScenario!!.close()
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = t0 + 60_000L,
            resetState = false
        )
        assertFalse(
            "onboarding_card must not reappear on subsequent cold relaunch",
            SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
    }

    @Test
    fun t1_f01_04_all_five_navigation_tabs_reachable_in_one_tap_offline() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)

        val tabToScreen = listOf(
            "nav_tab_bodymap" to "bodymap_screen",
            "nav_tab_skins" to "skins_screen",
            "nav_tab_journal" to "journal_screen",
            "nav_tab_settings" to "settings_screen",
            "nav_tab_home" to "home_screen"
        )
        for ((tabTag, expectedScreenTag) in tabToScreen) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, tabTag)
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, expectedScreenTag)
        }
    }

    // =========================================================================
    // F2 — Editorial Framed Splash Ad UI (开屏雅鉴) & Safe CTA (4 tests)
    // =========================================================================

    @Test
    fun t1_f02_01_force_splash_preview_renders_framed_stage_and_ad_disclosure_badge() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_backdrop")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_badge")

        val badgeText = SongxiAndroidE2EHarness.readNodeText(composeRule, "splash_ad_badge")
        assertTrue("splash_ad_badge must explicitly disclose '广告', got: '$badgeText'", badgeText.contains("广告"))
    }

    @Test
    fun t1_f02_02_splash_brand_footer_renders_seal_and_sanctuary_78_22_proportion() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_brand_footer")

        val rootBounds = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_screen")
            .fetchSemanticsNode().boundsInRoot
        val footerBounds = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_brand_footer")
            .fetchSemanticsNode().boundsInRoot

        val footerRatio = footerBounds.height / rootBounds.height.coerceAtLeast(1f)
        assertTrue(
            "Footer must sit in bottom sanctuary band (top=${footerBounds.top}, rootH=${rootBounds.height})",
            footerBounds.top >= rootBounds.top + rootBounds.height * 0.62f
        )
        assertTrue(
            "Footer height ratio ($footerRatio) must approximate ~22% sanctuary band [0.14..0.32]",
            footerRatio in 0.14f..0.32f
        )
        val footerText = SongxiAndroidE2EHarness.readNodeText(composeRule, "splash_ad_brand_footer")
        assertTrue("Brand footer must include '松息', got: '$footerText'", footerText.contains("松息"))
    }

    @Test
    fun t1_f02_03_splash_skip_button_immediately_dismisses_to_sanctuary() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val warm = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        warm.close()

        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = t0 + 10_000L,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_skip_button")
        val skipText = SongxiAndroidE2EHarness.readNodeText(composeRule, "splash_ad_skip_button")
        assertTrue("Skip button must display '跳过', got: '$skipText'", skipText.contains("跳过"))

        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_skip_button").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertFalse(
            "splash_ad_screen must dismiss immediately after tapping skip",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
    }

    @Test
    fun t1_f02_04_splash_bounded_cta_opens_safe_confirmation_sheet_without_silent_jump() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_cta_button")
        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_cta_button").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_confirm_sheet")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_confirm_dismiss")

        activeScenario!!.onActivity { activity ->
            val started = Shadows.shadowOf(activity).peekNextStartedActivity()
            assertNull("Tapping CTA must open confirmation sheet first without silent external jump", started)
        }
    }

    // =========================================================================
    // F3 — 7-Gate Calm Splash-Ad Policy & Privacy Controls (4 tests)
    // =========================================================================

    @Test
    fun t1_f03_01_gate1_first_launch_grace_bypasses_splash_ad() {
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0),
            resetState = true,
            autoAdvanceClock = false
        )
        assertFalse(
            "Gate 1 first-launch grace must bypass splash_ad_screen",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        assertTrue(
            "First launch must mount onboarding_card or home_screen directly",
            SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card") ||
                SongxiAndroidE2EHarness.tagExists(composeRule, "home_screen")
        )
    }

    @Test
    fun t1_f03_02_second_cold_launch_after_four_hours_displays_splash_ad() {
        val t1 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val first = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t1)
        first.close()

        val t2 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 13, 30)
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = t2,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    @Test
    fun t1_f03_03_gate3_reminder_quick_start_deep_link_bypasses_splash_to_player() {
        val t1 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val first = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t1)
        first.close()

        val t2 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 14, 0)
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            fromReminderQuickStart = true,
            simulatedEpochMs = t2,
            resetState = false,
            autoAdvanceClock = false
        )
        assertFalse(
            "Gate 3 reminder quick-start deep-link must bypass splash_ad_screen even when >4h eligible",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    @Test
    fun t1_f03_04_settings_personalized_toggle_persists_and_manual_preview_opens_splash() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        val initialState = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "splash_personalized_switch")
        SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "splash_personalized_switch")
        val toggledState = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "splash_personalized_switch")
        assertNotEquals("splash_personalized_switch must toggle its boolean state", initialState, toggledState)

        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        assertEquals(
            "splash_personalized_switch state must persist across Activity.recreate()",
            toggledState,
            SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "splash_personalized_switch")
        )

        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "splash_preview_trigger_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
    }

    // =========================================================================
    // F4 — Zero-Membership & 100% Unlocked Content Invariant (3 tests)
    // =========================================================================

    @Test
    fun t1_f04_01_all_four_skins_selectable_free_without_paywall_or_rewarded_ad() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (skinTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            val screenCopy = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
            for (forbidden in SongxiAndroidE2EHarness.FORBIDDEN_VIP_UI_PATTERNS) {
                assertFalse("Skin '$skinTag' must never trigger paywall '$forbidden'", screenCopy.contains(forbidden))
            }
        }
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val activeBadge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue("Final selected skin spring_blossom ('春涧') must be active, got: $activeBadge", activeBadge.contains("春"))
    }

    @Test
    fun t1_f04_02_all_four_booklets_and_all_soundscapes_startable_free() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_0")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_selector")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_silent")
        val playerCopy = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        for (forbidden in SongxiAndroidE2EHarness.FORBIDDEN_VIP_UI_PATTERNS) {
            assertFalse("Booklet/soundscape must be 100% free without '$forbidden'", playerCopy.contains(forbidden))
        }
    }

    @Test
    fun t1_f04_03_multi_zone_custom_composer_unlimited_beyond_three_daily_runs() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_neck")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_shoulder")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_wrist")

        repeat(4) { runIdx ->
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
            assertFalse(
                "player_screen must close cleanly after custom run #${runIdx + 1}",
                SongxiAndroidE2EHarness.tagExists(composeRule, "player_screen")
            )
            if (!SongxiAndroidE2EHarness.tagExists(composeRule, "bodymap_screen")) {
                SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
            }
        }
    }

    // =========================================================================
    // F5 — 5-Dimensional Illustration × Shan-Shui Skin Engine (3 tests)
    // =========================================================================

    @Test
    fun t1_f05_01_native_skia_shanshui_and_figure_canvases_render_multicolor_layers() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        val homeBackdropBmp = SongxiAndroidE2EHarness.captureTagBitmap(
            activeScenario!!,
            composeRule,
            "home_shanshui_backdrop"
        )
        val homeStats = SongxiAndroidE2EHarness.analyzeBitmapColors(homeBackdropBmp)
        assertTrue(
            "home_shanshui_backdrop must paint layered multi-color Skia pixels, got ${homeStats.distinctRgbCount}",
            homeStats.distinctRgbCount >= 4
        )

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_figure_canvas")
        val figureBmp = SongxiAndroidE2EHarness.captureTagBitmap(
            activeScenario!!,
            composeRule,
            "player_figure_canvas"
        )
        val figStats = SongxiAndroidE2EHarness.analyzeBitmapColors(figureBmp)
        assertTrue(
            "player_figure_canvas must paint multi-color illustrated figure pixels, got ${figStats.distinctRgbCount}",
            figStats.distinctRgbCount >= 4
        )
    }

    @Test
    fun t1_f05_02_skin_switch_updates_surface_palette_and_shanshui_backdrop_pixels() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_ink_mist")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val inkStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        )

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val qinglvStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        )

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_dusk_pine")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val duskStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
        )

        assertEquals(
            "ink_mist, qinglv_celadon, and dusk_pine must render 3 distinct pixel hashes",
            3,
            setOf(inkStats.pixelHashHex, qinglvStats.pixelHashHex, duskStats.pixelHashHex).size
        )
        assertTrue(
            "Night skin dusk_pine luminance (${duskStats.averageLuminance}) must be < 0.25 and darker than ink_mist (${inkStats.averageLuminance})",
            duskStats.averageLuminance < 0.25 && duskStats.averageLuminance < inkStats.averageLuminance
        )
    }

    @Test
    fun t1_f05_03_skin_switch_updates_poetic_greeting_seal_badge_and_sensory_chime() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        val expectedTokens = listOf(
            "skin_card_ink_mist" to "水墨",
            "skin_card_qinglv_celadon" to "青绿",
            "skin_card_dusk_pine" to "暮山",
            "skin_card_spring_blossom" to "春涧"
        )
        val homeSnapshots = LinkedHashSet<String>()
        for ((skinTag, expectedKeyword) in expectedTokens) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
            val homeText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
            assertTrue(
                "Home screen under $skinTag must reflect '$expectedKeyword', got: $homeText",
                homeText.contains(expectedKeyword)
            )
            homeSnapshots.add(homeText)
        }
        assertEquals("All 4 skins must produce 4 distinct Home editorial text snapshots", 4, homeSnapshots.size)
    }

    // =========================================================================
    // F6 — 4 Free Built-In Skins, Live Gallery & Auto-Night Pairing (4 tests)
    // =========================================================================

    @Test
    fun t1_f06_01_skin_gallery_displays_all_four_skins_and_auditions_chime() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "skins_screen")

        for (skinCardTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, skinCardTag)
        }

        audioProbe.reset()
        SongxiAndroidE2EHarness.resetHapticLedger()
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_chime_preview_button")
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 300L)
        assertTrue(
            "Auditioning skin chime must emit non-silent PCM audio or gentle haptic feedback",
            audioProbe.nonSilentChunks.get() > 0 || SongxiAndroidE2EHarness.hasRecordedHapticPulse()
        )
    }

    @Test
    fun t1_f06_02_tapping_skin_hot_swaps_live_without_activity_restart() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        var initialIdentity = 0
        activeScenario!!.onActivity { activity -> initialIdentity = System.identityHashCode(activity) }

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_spring_blossom")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")

        var afterIdentity = 0
        activeScenario!!.onActivity { activity -> afterIdentity = System.identityHashCode(activity) }
        assertEquals("Hot-swapping skin must NOT recreate/restart Activity", initialIdentity, afterIdentity)

        val badgeText = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue("Active skin badge must reflect 春涧花影, got: '$badgeText'", badgeText.contains("春"))
    }

    @Test
    fun t1_f06_03_selected_skin_persists_across_activity_recreate() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")

        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val badgeText = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue(
            "Selected skin qinglv_celadon ('青绿') must persist across Activity.recreate(), got: '$badgeText'",
            badgeText.contains("青绿")
        )
    }

    @Test
    fun t1_f06_04_auto_night_pairing_switch_toggles_and_persists() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")

        val before = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")
        SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "skin_auto_night_switch")
        val after = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")
        assertNotEquals("skin_auto_night_switch must toggle state", before, after)

        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        assertEquals(
            "skin_auto_night_switch state must persist across recreate()",
            after,
            SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")
        )
    }

    // =========================================================================
    // F7 — Context-Aware Home (今日一息) & Quick Micro-Shortcuts (4 tests)
    // =========================================================================

    @Test
    fun t1_f07_01_home_renders_greeting_active_skin_badge_and_hero_prescription() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_shanshui_backdrop")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_active_skin_badge")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_hero_card")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_start_hero_button")

        val heroText = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")
        assertTrue("Hero prescription card must contain non-empty Chinese copy, got: '$heroText'", heroText.length >= 4)
    }

    @Test
    fun t1_f07_02_switching_posture_scenes_dynamically_updates_hero_card() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_desk")
        val deskHero = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_tea")
        val teaHero = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_bed")
        val bedHero = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")

        assertEquals(
            "desk, tea, and bed posture scenes must yield 3 distinct hero prescriptions",
            3,
            setOf(deskHero, teaHero, bedHero).size
        )
    }

    @Test
    fun t1_f07_03_one_tap_hero_start_launches_stretch_player_immediately() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_figure_canvas")
    }

    @Test
    fun t1_f07_04_all_four_quick_relief_shortcuts_launch_targeted_micro_routines() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (idx in 0..3) {
            val shortcutTag = "home_quick_routine_$idx"
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, shortcutTag)
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")
        }
    }

    // =========================================================================
    // F8 — 6-Zone Body Tension Map & Unlimited Custom Composer (身体寻音) (4 tests)
    // =========================================================================

    @Test
    fun t1_f08_01_bodymap_renders_illustrated_canvas_and_all_six_zones() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "bodymap_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "bodymap_canvas")

        val canvasBounds = SongxiAndroidE2EHarness.findNodeByTag(composeRule, "bodymap_canvas")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "bodymap_canvas must have non-trivial dimensions, got $canvasBounds",
            canvasBounds.width > 40f && canvasBounds.height > 40f
        )

        for (zoneTag in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS) {
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, zoneTag)
        }
    }

    @Test
    fun t1_f08_02_selecting_one_to_three_zones_updates_selection_state() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        // Ensure clean selection of neck + shoulder + wrist (<= 3 zones)
        for (z in SongxiAndroidE2EHarness.ALL_BODYMAP_ZONE_TAGS) {
            if (SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, z)) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, z)
            }
        }
        for (target in listOf("bodymap_zone_neck", "bodymap_zone_shoulder", "bodymap_zone_wrist")) {
            if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, target)) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, target)
            }
        }
        val activeCount = SongxiAndroidE2EHarness.countSelectedBodyMapZones(composeRule)
        assertTrue("Expected 1..3 selected body zones, got $activeCount", activeCount in 1..3)
    }

    @Test
    fun t1_f08_03_duration_pills_2_4_6_minutes_selectable_and_reflected() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        for (durTag in listOf("bodymap_duration_2", "bodymap_duration_4", "bodymap_duration_6")) {
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, durTag)
            assertTrue(
                "Duration option '$durTag' must reflect active selection or text",
                SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, durTag) ||
                    SongxiAndroidE2EHarness.readNodeText(composeRule, durTag).isNotEmpty()
            )
        }
    }

    @Test
    fun t1_f08_04_compose_start_button_launches_custom_multi_zone_flow_in_player() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_neck")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_lumbar")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_duration_4")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_figure_canvas")
    }

    // =========================================================================
    // F9 — Free Classical Curated Booklets (四时雅集) (3 tests)
    // =========================================================================

    @Test
    fun t1_f09_01_all_four_classical_booklet_cards_displayed_with_series_titles() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        val titles = LinkedHashSet<String>()
        for (i in 0..3) {
            val tag = "booklet_card_$i"
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, tag)
            val txt = SongxiAndroidE2EHarness.readNodeText(composeRule, tag)
            assertTrue("Booklet card $tag must render non-empty classical title", txt.length >= 4)
            titles.add(txt)
        }
        assertEquals("All 4 booklet cards must display distinct series titles", 4, titles.size)
    }

    @Test
    fun t1_f09_02_tapping_each_booklet_starts_guided_stretch_routine() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        for (i in 0..3) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_$i")
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        }
    }

    @Test
    fun t1_f09_03_completing_booklet_session_increments_calm_local_progress() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val initialCardText = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_0")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_0")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val updatedCardText = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_0")
        assertNotEquals(
            "Completing booklet_card_0 session must increment calm local progress text",
            initialCardText,
            updatedCardText
        )
        for (shame in SongxiAndroidE2EHarness.FORBIDDEN_STREAK_SHAME_PATTERNS) {
            assertFalse("Booklet progress must contain zero streak-shame copy ('$shame')", updatedCardText.contains(shame))
        }
    }

    // =========================================================================
    // F10 — Guided Stretch Player (4s/6s Halo & 闭眼余光态) (4 tests)
    // =========================================================================

    @Test
    fun t1_f10_01_player_renders_figure_halo_phase_label_and_sets_keep_screen_on() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_figure_canvas")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_breathing_halo")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_breath_phase_label")

        val phase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("Initial breath phase at t=0ms must indicate '吸气', got: '$phase'", phase.contains("吸气"))
        assertTrue(
            "Active player must enable FLAG_KEEP_SCREEN_ON / keepScreenOn",
            SongxiAndroidE2EHarness.hasKeepScreenOnFlag(activeScenario!!)
        )
    }

    @Test
    fun t1_f10_02_breath_cycle_transitions_4s_inhale_to_6s_exhale() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        val t0Phase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("Expected inhale ('吸气') at start, got '$t0Phase'", t0Phase.contains("吸气"))

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 4_200L)
        val exhalePhase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("Expected exhale ('呼气') after 4.2s, got '$exhalePhase'", exhalePhase.contains("呼气"))

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 6_000L)
        val nextInhalePhase = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_breath_phase_label")
        assertTrue("Expected next inhale ('吸气') after 10.2s cycle, got '$nextInhalePhase'", nextInhalePhase.contains("吸气"))
    }

    @Test
    fun t1_f10_03_eyes_free_toggle_switches_peripheral_luminance_mode() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        val beforeStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureDecorBitmap(activeScenario!!, composeRule)
        )
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_eyes_free_toggle")
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 400L)
        val eyesFreeStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureDecorBitmap(activeScenario!!, composeRule)
        )
        assertNotEquals(
            "Toggling player_eyes_free_toggle must update peripheral glow visual stage",
            beforeStats.pixelHashHex,
            eyesFreeStats.pixelHashHex
        )

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_eyes_free_toggle")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    @Test
    fun t1_f10_04_pause_resume_prev_next_and_close_controls_operate_cleanly() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_pause_resume_button")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_pause_resume_button")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_next_pose_button")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_prev_pose_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")
        assertFalse(
            "player_screen must exit cleanly after tapping player_close_button",
            SongxiAndroidE2EHarness.tagExists(composeRule, "player_screen")
        )
    }

    // =========================================================================
    // F11 — Procedural Harmonic Audio, Gentle Haptics & Flip-to-Pause (4 tests)
    // =========================================================================

    @Test
    fun t1_f11_01_player_sound_selector_cycles_harmonic_profiles_and_silent_mode() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        val beforeSound = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_sound_selector")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_selector")
        val afterSound = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_sound_selector")
        assertTrue(
            "Sound selector must expose or cycle harmonic soundscapes",
            beforeSound != afterSound || SongxiAndroidE2EHarness.collectAllScreenText(composeRule).let {
                it.contains("钵") || it.contains("磬") || it.contains("琴") || it.contains("雨")
            }
        )

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_silent")
        assertTrue(
            "player_sound_silent must activate office-quiet mode",
            SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "player_sound_silent") ||
                SongxiAndroidE2EHarness.collectAllScreenText(composeRule).contains("静默")
        )
    }

    @Test
    fun t1_f11_02_active_player_emits_gentle_breath_haptic_waveform() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.resetHapticLedger()
        composeRule.mainClock.autoAdvance = false

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 4_200L)

        assertTrue(
            "Active stretch player must emit gentle breath VibrationEffect waveform across inhale/exhale",
            SongxiAndroidE2EHarness.hasRecordedHapticPulse()
        )
    }

    @Test
    fun t1_f11_03_face_down_accelerometer_triggers_silent_pause_and_banner() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, -9.5f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")
    }

    @Test
    fun t1_f11_04_face_up_accelerometer_clears_flip_pause_or_enables_clean_resume() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")

        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, -9.5f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")

        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, +9.5f)
        if (SongxiAndroidE2EHarness.tagExists(composeRule, "player_paused_by_flip_banner")) {
            // If implementation prompts user confirmation to resume after lifting phone
            runCatching { SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_paused_by_flip_banner") }
            if (SongxiAndroidE2EHarness.tagExists(composeRule, "player_paused_by_flip_banner")) {
                SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_pause_resume_button")
            }
        }
        assertFalse(
            "Flip-pause banner must clear once phone is face-up / resumed",
            SongxiAndroidE2EHarness.tagExists(composeRule, "player_paused_by_flip_banner")
        )
    }

    // =========================================================================
    // F12 — Completion Water-Ripple Ritual & Somatic Check-In (一息成) (4 tests)
    // =========================================================================

    @Test
    fun t1_f12_01_finishing_final_pose_enters_completion_ripple_stage() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "completion_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "completion_ripple_canvas")

        val rippleStats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "completion_ripple_canvas")
        )
        assertTrue(
            "completion_ripple_canvas must paint concentric water-ripple colors, got ${rippleStats.distinctRgbCount}",
            rippleStats.distinctRgbCount >= 3
        )
    }

    @Test
    fun t1_f12_02_selecting_somatic_feeling_tags_highlights_choice() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        val feelingExpectations = listOf(
            "completion_feeling_light" to "轻快",
            "completion_feeling_warm" to "舒展",
            "completion_feeling_calm" to "安宁"
        )
        for ((tag, keyword) in feelingExpectations) {
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, tag)
            val label = SongxiAndroidE2EHarness.readNodeText(composeRule, tag)
            assertTrue("Feeling chip $tag must contain '$keyword', got '$label'", label.contains(keyword))
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, tag)
        }
    }

    @Test
    fun t1_f12_03_completion_finish_persists_session_with_feeling_to_journal() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_light")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val journalText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        assertTrue(
            "Journal must reflect recorded somatic feeling '肩颈轻快', got: $journalText",
            journalText.contains("轻快")
        )
    }

    @Test
    fun t1_f12_04_completion_finish_returns_cleanly_to_sanctuary_navigation() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")
        assertFalse(
            "completion_screen must close after tapping completion_finish_button",
            SongxiAndroidE2EHarness.tagExists(composeRule, "completion_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "nav_tab_home")
    }

    // =========================================================================
    // F13 — Low-Intrusion Sedentary Reminder Scheduler (微风提醒) (4 tests)
    // =========================================================================

    @Test
    fun t1_f13_01_settings_displays_reminder_switches_and_four_intervals() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "settings_screen")

        val expectedTags = listOf(
            "reminder_enable_switch",
            "reminder_interval_45",
            "reminder_interval_60",
            "reminder_interval_90",
            "reminder_interval_120",
            "reminder_lunch_quiet_switch",
            "reminder_night_quiet_switch",
            "reminder_weekend_quiet_switch",
            "reminder_mute_today_switch",
            "reminder_preview_trigger_button"
        )
        for (tag in expectedTags) {
            SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, tag)
        }
    }

    @Test
    fun t1_f13_02_selecting_interval_updates_scheduled_workmanager_periodicity() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_enable_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_enable_switch")
        }
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_interval_90")

        val scheduled = SongxiAndroidE2EHarness.activeScheduledWorkInfos()
        assertTrue("Enabling reminder must enqueue WorkManager request(s)", scheduled.isNotEmpty())
        assertTrue(
            "Expected periodic WorkRequest with repeatIntervalMillis == 90m (${90 * 60_000L}ms), got: ${scheduled.map { it.periodicityInfo?.repeatIntervalMillis }}",
            scheduled.any { it.periodicityInfo?.repeatIntervalMillis == 90L * 60_000L }
        )
    }

    @Test
    fun t1_f13_03_reminder_preview_trigger_posts_notification_with_quickstart_deeplink() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        SongxiAndroidE2EHarness.clearPostedNotifications()

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_preview_trigger_button")
        val posted = SongxiAndroidE2EHarness.postedNotifications()
        assertTrue(
            "Tapping reminder_preview_trigger_button must post Android notification",
            posted.isNotEmpty()
        )
        val quickStartIntents = posted.flatMap { SongxiAndroidE2EHarness.extractNotificationQuickStartIntents(it) }
        assertTrue(
            "Notification action/contentIntent must carry EXTRA_FROM_REMINDER_QUICK_START = true",
            quickStartIntents.any { it.getBooleanExtra(SongxiAndroidE2EHarness.EXTRA_FROM_REMINDER_QUICK_START, false) }
        )
    }

    @Test
    fun t1_f13_04_reminder_interval_and_quiet_switches_persist_across_recreate() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_interval_45")
        val beforeWeekend = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_weekend_quiet_switch")
        SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_weekend_quiet_switch")
        val afterWeekend = SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_weekend_quiet_switch")
        assertNotEquals("reminder_weekend_quiet_switch must toggle", beforeWeekend, afterWeekend)

        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        assertEquals(
            "reminder_weekend_quiet_switch must persist across recreate()",
            afterWeekend,
            SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_weekend_quiet_switch")
        )
        assertTrue(
            "45m interval must persist in WorkManager schedule across recreate()",
            SongxiAndroidE2EHarness.activeScheduledWorkInfos()
                .any { it.periodicityInfo?.repeatIntervalMillis == 45L * 60_000L }
        )
    }

    // =========================================================================
    // F14 — Streak-Free Water-Ripple Journal & Shareable Card (4 tests)
    // =========================================================================

    @Test
    fun t1_f14_01_journal_renders_monthly_calendar_grid_and_calm_summary() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_calendar_grid")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_monthly_summary")

        val summary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertTrue(
            "Monthly summary must render calm poetic copy ('息' / '分钟' / '留白' / '舒展'), got: '$summary'",
            summary.contains("息") || summary.contains("分钟") || summary.contains("留白") || summary.contains("舒展")
        )
    }

    @Test
    fun t1_f14_02_completed_session_increments_monthly_summary_and_today_ripple() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val initialSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_warm")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val updatedSummary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertNotEquals(
            "Completing a routine must update journal_monthly_summary",
            initialSummary,
            updatedSummary
        )
        assertTrue("Updated summary must reflect >= 1 completed session ('1'), got '$updatedSummary'", updatedSummary.contains("1"))
    }

    @Test
    fun t1_f14_03_practiced_day_displays_recorded_somatic_feeling_note() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_0")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_calm")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val allJournalCopy = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        assertTrue(
            "Practiced day must surface recorded feeling '心神安宁' in journal, got: $allJournalCopy",
            allJournalCopy.contains("安宁")
        )
    }

    @Test
    fun t1_f14_04_share_card_button_opens_preview_dialog_and_dispatches_action_send() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "journal_share_card_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_share_preview_dialog")

        if (SongxiAndroidE2EHarness.peekDispatchedShareIntent(activeScenario!!) == null) {
            // Click explicit share confirmation CTA inside preview dialog
            val shareActionMatcher = SemanticsMatcher("share_confirm_cta") { node ->
                node.config.contains(SemanticsActions.OnClick) &&
                    run {
                        val txt = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
                            ?.joinToString("") { it.text } ?: ""
                        (txt.contains("分享") || txt.contains("发送") || txt.contains("导出")) &&
                            !txt.contains("取消") && !txt.contains("关闭")
                    }
            }
            val matches = composeRule.onAllNodes(shareActionMatcher, useUnmergedTree = false)
            if (matches.fetchSemanticsNodes().isNotEmpty()) {
                matches[0].performClick()
                ShadowLooper.idleMainLooper()
                composeRule.waitForIdle()
            }
        }

        val shareIntent = SongxiAndroidE2EHarness.peekDispatchedShareIntent(activeScenario!!)
        assertNotNull("Expected Intent.ACTION_SEND dispatched from 松息日笺 share card", shareIntent)
        assertEquals(Intent.ACTION_SEND, shareIntent!!.action)
    }

    // =========================================================================
    // F15 — Standalone Android APK Packaging, Visual Captures & Suite (3 tests)
    // =========================================================================

    @Test
    fun t1_f15_01_debug_and_release_apks_exist_and_pass_aapt2_badging_contract() {
        val debugApk = SongxiAndroidE2EHarness.debugApkFile()
        val releaseApk = SongxiAndroidE2EHarness.releaseApkFile()
        assertTrue("Expected app-debug.apk > 100KB at ${debugApk.absolutePath}", debugApk.isFile && debugApk.length() > 100_000L)
        assertTrue("Expected signed app-release.apk > 100KB at ${releaseApk.absolutePath}", releaseApk.isFile && releaseApk.length() > 100_000L)
        SongxiAndroidE2EHarness.assertValidSignedReleaseAndDebugApks()
    }

    @Test
    fun t1_f15_02_release_apk_passes_apksigner_cryptographic_verification() {
        val releaseApk = SongxiAndroidE2EHarness.releaseApkFile()
        assertTrue("Expected signed app-release.apk at ${releaseApk.absolutePath}", releaseApk.isFile)
        val res = SongxiAndroidE2EHarness.runSdkTool(
            "apksigner",
            "verify",
            "--verbose",
            "--print-certs",
            releaseApk.absolutePath
        )
        assertEquals("apksigner verify failed: ${res.stderr}", 0, res.exitCode)
        assertTrue(
            "Release APK must verify using APK Signature Scheme v2 or v3",
            res.stdout.contains("Verified using v2 scheme (APK Signature Scheme v2): true") ||
                res.stdout.contains("Verified using v3 scheme (APK Signature Scheme v3): true")
        )
    }

    @Test
    fun t1_f15_03_roborazzi_four_skin_png_captures_exist_and_are_non_blank() {
        val skinPngs = SongxiAndroidE2EHarness.discoverFourSkinPngs()
        assertEquals("Must discover all 4 built-in skin Roborazzi PNGs", 4, skinPngs.size)
        for ((skinKey, file) in skinPngs) {
            assertTrue("Roborazzi PNG for '$skinKey' must exist and be > 1KB", file.isFile && file.length() > 1024L)
        }
        SongxiAndroidE2EHarness.assertValidAndDistinctSkinPngs()
    }

    // =========================================================================
    // F8 (Extended) — Clinical Kinesiology, Biomechanics Guide & Body Relief Atlas
    // =========================================================================

    @Test
    fun t1_f08_05_clinical_prescription_asymmetry_biomechanics_sheet_and_relief_atlas() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")

        // 1. Select asymmetric right-side weighting (6:4) and verify 3-stage prescription preview
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_asymmetry_right")
        val previewText = SongxiAndroidE2EHarness.readNodeText(composeRule, "bodymap_prescription_preview")
        assertTrue(
            "Expected 3-stage prescription preview on BodyMapScreen, got '$previewText'",
            previewText.contains("三阶处方序")
        )

        // 2. Compose & launch custom routine -> verify timeline stage badge, biomechanics toggle & clinical guide sheet
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_timeline_stage_badge")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_biomechanics_toggle")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_biomechanics_toggle")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_clinical_guide_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_clinical_guide_sheet")
        val guideCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_clinical_guide_sheet")
        assertTrue("Expected clinical RPE/safety guidance in sheet, got '$guideCopy'", guideCopy.contains("RPE") || guideCopy.contains("靶向肌群"))

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_clinical_guide_close")

        // 3. Complete session and verify streak-free Monthly Body Relief Atlas on PracticeJournalScreen
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_light")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val atlasCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_body_relief_atlas")
        assertTrue(
            "Expected Monthly Body Relief Atlas to reflect cared-for zones, got '$atlasCopy'",
            atlasCopy.contains("月度身体释压图谱") && atlasCopy.contains("重点温养")
        )
        for (forbidden in SongxiAndroidE2EHarness.FORBIDDEN_STREAK_SHAME_PATTERNS) {
            assertFalse(
                "Forbidden streak/shame pattern '$forbidden' found in journal_body_relief_atlas: $atlasCopy",
                atlasCopy.contains(forbidden)
            )
        }
    }
}
