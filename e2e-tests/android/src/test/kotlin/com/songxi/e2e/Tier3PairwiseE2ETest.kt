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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tier3PairwiseE2ETest {

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

    // T3-01: F2 Splash Ad × F6 Skin Selection (Splash adopts dark dusk_pine palette)
    @Test
    fun t3_01_dusk_pine_skin_adapts_splash_ad_stage_without_glare() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0)
        val setup = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_dusk_pine")
        setup.close()

        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            forceSplashPreview = true,
            simulatedEpochMs = t0 + 60_000L,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        val stats = SongxiAndroidE2EHarness.analyzeBitmapColors(
            SongxiAndroidE2EHarness.captureDecorBitmap(activeScenario!!, composeRule)
        )
        assertTrue(
            "Splash stage under dusk_pine must render low-glare dark ground (lum=${stats.averageLuminance})",
            stats.averageLuminance < 0.35
        )
    }

    // T3-02: F6 Skin Selection × F10/F11 Player Figure, Halo & Harmonic Soundscape
    @Test
    fun t3_02_qinglv_skin_propagates_to_player_figure_and_default_bamboo_chime() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        val soundLabel = SongxiAndroidE2EHarness.readNodeText(composeRule, "player_sound_selector")
        assertTrue("qinglv_celadon player must default to bamboo chime ('磬'), got: $soundLabel", soundLabel.contains("磬"))
    }

    // T3-03: F13 Reminder Deep-Link × F3 Splash Gate × F12 Completion × F14 Journal
    @Test
    fun t3_03_reminder_quickstart_bypasses_splash_and_logs_completed_session_to_journal() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val init = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        init.close()

        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            fromReminderQuickStart = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 15, 0),
            resetState = false
        )
        assertFalse(SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen"))
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_light")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val summary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertTrue("Reminder quick-start completion must log 1 session to journal, got: $summary", summary.contains("1"))
    }

    // T3-04: F8 Custom Multi-Zone Composer × F10 Eyes-Free Mode × F11 Silent Haptic Mode
    @Test
    fun t3_04_custom_composed_routine_supports_simultaneous_eyes_free_and_silent_haptic_mode() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_wrist")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_duration_2")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        composeRule.mainClock.autoAdvance = false

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_eyes_free_toggle")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_sound_silent")
        audioProbe.reset()
        SongxiAndroidE2EHarness.resetHapticLedger()

        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 4_200L)
        assertEquals(0, audioProbe.nonSilentChunks.get())
        assertTrue(SongxiAndroidE2EHarness.hasRecordedHapticPulse())
    }

    // T3-05: F6 Auto-Night Pairing × F7 Evening Bedside Scene
    @Test
    fun t3_05_evening_21_00_auto_night_dusk_pine_pairs_with_evening_bedside_hero() {
        val setup = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 12, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_ink_mist")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "skin_auto_night_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "skin_auto_night_switch")
        }
        setup.close()

        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 21, 0)
        )
        val badge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue("At 21:00 auto-night must activate 暮山听松, got: $badge", badge.contains("暮山"))

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_bed")
        val heroCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_hero_card")
        assertTrue("Bedside hero card must reflect evening routine ('晚间'), got: $heroCopy", heroCopy.contains("晚间"))
    }

    // T3-06: F5 Spring Blossom Skin × F7 Quick Relief Shortcut × F12 Completion Verse
    @Test
    fun t3_06_spring_blossom_skin_updates_quick_shortcut_completion_verse() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_spring_blossom")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_quick_routine_2")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)

        val completionCopy = SongxiAndroidE2EHarness.readNodeText(composeRule, "completion_screen")
        assertTrue(
            "Completion stage under spring_blossom must show spring verse ('春水' / '花影'), got: $completionCopy",
            completionCopy.contains("春水") || completionCopy.contains("花影")
        )
    }

    // T3-07: F9 Booklet Completion × F14 Journal Accumulation × F4 Zero Paywall
    @Test
    fun t3_07_completing_curated_booklet_updates_both_booklet_progress_and_monthly_journal() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule, resetState = true)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val beforeBooklet = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_3")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_3")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_warm")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        val afterBooklet = SongxiAndroidE2EHarness.readNodeText(composeRule, "booklet_card_3")
        assertNotEquals(beforeBooklet, afterBooklet)

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val journalText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        assertTrue(journalText.contains("温热舒展"))
    }

    // T3-08: F3 Settings Manual Splash Preview × F2 Safe CTA Sheet Dismiss → Returns to Settings
    @Test
    fun t3_08_manual_splash_preview_from_settings_handles_cta_sheet_and_returns_to_settings() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")

        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "splash_preview_trigger_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")

        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_cta_button").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_confirm_sheet")

        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_confirm_dismiss").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.advanceTimeByFrame()
        SongxiAndroidE2EHarness.findNodeByTag(composeRule, "splash_ad_skip_button").performClick()
        ShadowLooper.idleMainLooper()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "settings_screen")
    }

    // T3-09: F8 Custom Composer × F11 Flip-to-Pause Mid-Pose × Resume to Completion
    @Test
    fun t3_09_custom_routine_flip_to_pause_freezes_and_resumes_cleanly_to_completion() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_duration_2")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, -9.5f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")

        val frozenClock = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 6_000L)
        assertEquals(frozenClock, SongxiAndroidE2EHarness.collectAllScreenText(composeRule))

        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, +9.5f)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertFalse(SongxiAndroidE2EHarness.tagExists(composeRule, "player_paused_by_flip_banner"))

        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "completion_screen")
    }

    // T3-10: F11 Flip-Paused Player × F3 Gate 2 Warm Background Resume Immunity
    @Test
    fun t3_10_flip_paused_player_retains_gate2_splash_immunity_on_40m_warm_resume() {
        val t0 = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = t0)
        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, -9.5f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")

        SongxiAndroidE2EHarness.simulateWarmResumeAfter(
            scenario = activeScenario!!,
            composeRule = composeRule,
            baseEpochMs = t0,
            elapsedMillis = 40L * 60_000L
        )
        assertFalse(
            "Gate 2 must protect flip-paused stretch session from splash interruption",
            SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen")
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    // T3-11: F13 In-App Reminder Preview Banner × Direct Quick-Start Launch
    @Test
    fun t3_11_settings_inapp_reminder_banner_launches_90s_sigh_routine_directly() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_preview_trigger_button")

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "reminder_inapp_banner")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_inapp_banner")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    // T3-12: F6 Skin Switch × F14 Shareable Poetic Card (松息日笺 reflects active skin verse)
    @Test
    fun t3_12_journal_share_card_adapts_poetic_verse_to_selected_skin() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "journal_share_card_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_share_preview_dialog")

        val dialogText = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_share_preview_dialog")
        assertTrue(
            "Share preview card must reflect qinglv_celadon ('青绿' / '青山'), got: $dialogText",
            dialogText.contains("青绿") || dialogText.contains("青山")
        )

        val shareMatcher = SemanticsMatcher("share_cta") { node ->
            node.config.contains(SemanticsActions.OnClick) &&
                (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.contains("分享") == true)
        }
        composeRule.onAllNodes(shareMatcher, useUnmergedTree = false)[0].performClick()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        val dispatched = SongxiAndroidE2EHarness.peekDispatchedShareIntent(activeScenario!!)
        assertNotNull(dispatched)
        assertEquals(Intent.ACTION_SEND, dispatched!!.action)
        val sharedBody = dispatched.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue("Dispatched share text must include 青绿山居 verse, got: $sharedBody", sharedBody.contains("青山"))
    }

    // T3-13: F1 Onboarding Persistence × F6 Skin Persistence × Activity Recreation on Journal Tab
    @Test
    fun t3_13_recreating_activity_on_journal_tab_preserves_tab_skin_and_onboarding_state() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule, resetState = true)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_spring_blossom")
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")

        activeScenario!!.recreate()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_screen")
        assertFalse(SongxiAndroidE2EHarness.tagExists(composeRule, "onboarding_card"))
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")
        val badge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue(badge.contains("春涧"))
    }

    // T3-14: F15 Narrow 360dp Viewport × F8 BodyMap & F14 Journal Unclipped Navigation
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun t3_14_compact_360dp_viewport_navigates_bodymap_and_journal_cleanly() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "bodymap_canvas")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "player_close_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "journal_monthly_summary")
    }

    // T3-15: F13 Custom 45m Interval + Mute Today × Next-Day Rollover Delivery
    @Test
    fun t3_15_forty_five_minute_interval_with_mute_today_resumes_cleanly_next_morning() {
        val day1 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 10, 0)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_settings")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "reminder_interval_45")
        if (!SongxiAndroidE2EHarness.isSwitchOrChipActive(composeRule, "reminder_mute_today_switch")) {
            SongxiAndroidE2EHarness.toggleSwitchByTag(composeRule, "reminder_mute_today_switch")
        }
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertEquals(0, SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule))
        day1.close()

        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 19, 10, 15)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertTrue(SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule) >= 1)
    }

    // T3-16: F7 Tea-Break Scene × F10 Pose Clock Auto-Progression → F12 Completion
    @Test
    fun t3_16_tea_break_routine_advances_automatically_by_clock_ticks_to_completion() {
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_scene_tea")

        composeRule.mainClock.autoAdvance = false
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_start_hero_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")

        // Tea-break hero is 150s (3 x 50s); advance 155s by clock ticks without clicking next
        SongxiAndroidE2EHarness.advanceAppClockBy(composeRule, 155_000L, stepMillis = 1_000L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "completion_screen")
    }
}
