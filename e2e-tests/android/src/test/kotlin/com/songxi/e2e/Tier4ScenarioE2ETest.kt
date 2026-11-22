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
class Tier4ScenarioE2ETest {

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

    // T4-01: First-Time Desk Worker Full Sanctuary Journey
    @Test
    fun t4_01_first_time_desk_worker_onboarding_skin_custom_stretch_flip_pause_and_journal() {
        val morning = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 30)
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = morning,
            resetState = true
        )
        // 1. Zero splash on first launch, gentle onboarding -> Home
        assertFalse(SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen"))
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "onboarding_card")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "onboarding_enter_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "home_screen")

        // 2. Switch to flagship 青绿山居 skin
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "home_active_skin_badge")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "skins_screen")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "skin_card_qinglv_celadon")

        // 3. Compose 2-minute neck + wrist desk prescription
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_zone_wrist")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_duration_2")

        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "bodymap_compose_start_button")
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
        composeRule.mainClock.autoAdvance = false

        // 4. Colleague approaches -> flip phone face-down -> silent pause -> lift phone -> finish
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, -9.5f)
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_paused_by_flip_banner")
        SongxiAndroidE2EHarness.dispatchAccelerometer(composeRule, 0f, 0f, +9.5f)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_light")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        // 5. Verify Journal records 1 session & 肩颈轻快
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val journalText = SongxiAndroidE2EHarness.collectAllScreenText(composeRule)
        assertTrue(journalText.contains("肩颈轻快"))
        assertTrue(SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary").contains("1"))
    }

    // T4-02: Returning Afternoon User Splash Gate + Sedentary Reminder Quick-Start Loop
    @Test
    fun t4_02_afternoon_return_splash_skip_and_sedentary_notification_quickstart_journey() {
        val morning = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 9, 0)
        val first = SongxiAndroidE2EHarness.launchToHome(composeRule, resetState = true, simulatedEpochMs = morning)
        first.close()

        // Afternoon cold launch (+5.5h): Splash Ad shows with >=48dp skip, 0 motion listeners
        val afternoon = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 18, 14, 30)
        val second = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = afternoon,
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        assertEquals(0, SongxiAndroidE2EHarness.activeSensorListenerCount())
        SongxiAndroidE2EHarness.dismissSplashIfPresent(composeRule)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        // Scheduled WorkManager nudge fires at 14:30 -> posts quick-start notification
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertTrue(SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule) >= 1)
        val notif = SongxiAndroidE2EHarness.postedNotifications().first()
        val quickIntent = SongxiAndroidE2EHarness.extractNotificationQuickStartIntents(notif).first()
        assertTrue(quickIntent.getBooleanExtra(SongxiAndroidE2EHarness.EXTRA_FROM_REMINDER_QUICK_START, false))
        second.close()

        // Tapping notification launches directly into 90s sigh player bypassing splash
        activeScenario = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            fromReminderQuickStart = true,
            simulatedEpochMs = afternoon + 120_000L,
            resetState = false
        )
        assertFalse(SongxiAndroidE2EHarness.tagExists(composeRule, "splash_ad_screen"))
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "player_screen")
    }

    // T4-03: Multi-Day Splash Policy & Quiet-Hour Governance Journey
    @Test
    fun t4_03_multi_day_splash_cap_cooldown_and_lunch_weekend_quiet_governance() {
        val d1Morning = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 20, 8, 0) // Friday
        )
        d1Morning.close()

        // Friday 12:20 (>4h): Splash shows (#1), while Lunch Quiet Hour suppresses reminder
        val friLunch = SongxiAndroidE2EHarness.launchMain(
            composeRule = composeRule,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 20, 12, 20),
            resetState = false,
            autoAdvanceClock = false
        )
        SongxiAndroidE2EHarness.assertTagDisplayed(composeRule, "splash_ad_screen")
        SongxiAndroidE2EHarness.dismissSplashIfPresent(composeRule)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertEquals(0, SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule))
        friLunch.close()

        // Saturday 11:00: Splash shows (new day), while Weekend Auto-Silence suppresses reminder
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 21, 11, 0)
        )
        SongxiAndroidE2EHarness.clearPostedNotifications()
        assertEquals(0, SongxiAndroidE2EHarness.triggerScheduledReminderWork(composeRule))
    }

    // T4-04: Evening Wind-Down, Auto-Night Dusk Pine, Booklet Progression & Streak-Free Rest Days
    @Test
    fun t4_04_evening_auto_night_booklet_ritual_and_non_consecutive_rest_day_journal_share() {
        val nov2 = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = true,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 2, 21, 15)
        )
        // Auto-night defaults ON -> 21:15 activates 暮山听松
        val eveningBadge = SongxiAndroidE2EHarness.readNodeText(composeRule, "home_active_skin_badge")
        assertTrue(eveningBadge.contains("暮山"))

        // Practice evening breath booklet (#2)
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_2")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_feeling_calm")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")
        nov2.close()

        // Rest for 3 days; return Nov 6 evening and practice again -> 2 sessions, 0 streak guilt
        activeScenario = SongxiAndroidE2EHarness.launchToHome(
            composeRule = composeRule,
            resetState = false,
            simulatedEpochMs = SongxiAndroidE2EHarness.epochAtLocal(2026, 11, 6, 20, 45)
        )
        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_bodymap")
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "booklet_card_2")
        SongxiAndroidE2EHarness.advanceRoutineToCompletion(composeRule)
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "completion_finish_button")

        SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_journal")
        val summary = SongxiAndroidE2EHarness.readNodeText(composeRule, "journal_monthly_summary")
        assertTrue(summary.contains("2"))
        for (shame in SongxiAndroidE2EHarness.FORBIDDEN_STREAK_SHAME_PATTERNS) {
            assertFalse(SongxiAndroidE2EHarness.collectAllScreenText(composeRule).contains(shame))
        }

        // Open and dispatch 松息日笺 share card
        SongxiAndroidE2EHarness.scrollToAndClick(composeRule, "journal_share_card_button")
        val shareMatcher = SemanticsMatcher("share_btn") { node ->
            node.config.contains(SemanticsActions.OnClick) &&
                (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.contains("分享") == true)
        }
        composeRule.onAllNodes(shareMatcher, useUnmergedTree = false)[0].performClick()
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
        val intent = SongxiAndroidE2EHarness.peekDispatchedShareIntent(activeScenario!!)
        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent!!.action)
    }

    // T4-05: Full 4-Skin Visual & Contrast Tour Across Sanctuary Surfaces
    @Test
    fun t4_05_all_four_skins_render_distinct_high_contrast_canvases_and_roborazzi_snapshots() {
        SongxiAndroidE2EHarness.assertValidAndDistinctSkinPngs()
        activeScenario = SongxiAndroidE2EHarness.launchToHome(composeRule = composeRule)

        val backdropHashes = LinkedHashSet<String>()
        for (skinTag in SongxiAndroidE2EHarness.ALL_SKIN_CARD_TAGS) {
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_skins")
            SongxiAndroidE2EHarness.scrollToAndClick(composeRule, skinTag)
            SongxiAndroidE2EHarness.navigateToTab(composeRule, "nav_tab_home")

            val stats = SongxiAndroidE2EHarness.analyzeBitmapColors(
                SongxiAndroidE2EHarness.captureTagBitmap(activeScenario!!, composeRule, "home_shanshui_backdrop")
            )
            assertTrue(stats.distinctRgbCount >= 4)
            backdropHashes.add(stats.pixelHashHex)
        }
        assertEquals(4, backdropHashes.size)
    }

    // T4-06: Production Release APK Purity, Signing, Permissions & Zero-Paywall Audit
    @Test
    fun t4_06_release_and_debug_apk_integrity_permissions_and_zero_vip_audit() {
        SongxiAndroidE2EHarness.assertValidSignedReleaseAndDebugApks()
        val releaseApk = SongxiAndroidE2EHarness.releaseApkFile()
        SongxiAndroidE2EHarness.assertCleanManifestPermissions(releaseApk)
        val forbiddenHits = SongxiAndroidE2EHarness.scanApkForForbiddenStrings(releaseApk)
        assertTrue("Expected 0 forbidden VIP/IAP/ad strings in release APK, found: $forbiddenHits", forbiddenHits.isEmpty())
    }
}
