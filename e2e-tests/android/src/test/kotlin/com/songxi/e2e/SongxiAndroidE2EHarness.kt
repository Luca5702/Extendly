package com.songxi.e2e

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.robolectric.Shadows
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.ShadowVibrator
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

typealias SongxiE2EHarness = SongxiAndroidE2EHarness

object SongxiAndroidE2EHarness {

    // Public Opaque-Box Entry Point & Intent Extra Keys (PROJECT.md §4)
    const val APP_PACKAGE = "com.songxi.app"
    const val MAIN_ACTIVITY_CLASS = "com.songxi.app.MainActivity"
    const val EXTRA_FROM_REMINDER_QUICK_START = "com.songxi.app.extra.FROM_REMINDER_QUICK_START"
    const val EXTRA_FORCE_SPLASH_PREVIEW = "com.songxi.app.extra.FORCE_SPLASH_PREVIEW"
    const val EXTRA_SIMULATED_EPOCH_MS = "com.songxi.app.extra.SIMULATED_EPOCH_MS"
    const val EXTRA_RESET_STATE = "com.songxi.app.extra.RESET_STATE"

    val ALL_NAV_TABS = listOf(
        "nav_tab_home",
        "nav_tab_bodymap",
        "nav_tab_skins",
        "nav_tab_journal",
        "nav_tab_settings"
    )

    val ALL_SKIN_CARD_TAGS = listOf(
        "skin_card_ink_mist",
        "skin_card_qinglv_celadon",
        "skin_card_dusk_pine",
        "skin_card_spring_blossom"
    )

    val ALL_BODYMAP_ZONE_TAGS = listOf(
        "bodymap_zone_neck",
        "bodymap_zone_shoulder",
        "bodymap_zone_thoracic",
        "bodymap_zone_lumbar",
        "bodymap_zone_wrist",
        "bodymap_zone_eye"
    )

    // Negation-safe forbidden legacy v1 / VIP / paywall / WeChat MP patterns
    val FORBIDDEN_VIP_UI_PATTERNS = listOf(
        "四时席",
        "开通会员",
        "升级会员",
        "付费解锁",
        "订阅解锁",
        "会员专享",
        "VIP专享",
        "连续包月",
        "连续包年",
        "¥16",
        "¥88",
        "¥98",
        "¥118",
        "¥168",
        "微信小程序",
        "零开屏广告"
    )

    val FORBIDDEN_APK_BINARY_PATTERNS = FORBIDDEN_VIP_UI_PATTERNS + listOf(
        "openMembershipModal",
        "viewport-mp",
        "com.android.vending.BILLING",
        "com.android.billingclient",
        "InterstitialAd",
        "RewardedAd",
        "RewardedInterstitialAd",
        "BannerAdView"
    )

    val FORBIDDEN_STREAK_SHAME_PATTERNS = listOf(
        "连续打卡",
        "已中断",
        "断签",
        "打卡失败",
        "击败了",
        "击败"
    )

    data class BitmapColorStats(
        val width: Int,
        val height: Int,
        val distinctRgbCount: Int,
        val dominantRgb: Int,
        val darkestRgb: Int,
        val lightestRgb: Int,
        val averageLuminance: Double,
        val maxContrastRatio: Double,
        val pixelHashHex: String
    )

    data class CmdResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    class PcmAudioProbe : ShadowAudioTrack.OnAudioDataWrittenListener {
        val totalWriteCalls = AtomicInteger(0)
        val nonSilentChunks = AtomicInteger(0)

        override fun onAudioDataWritten(
            audioTrack: ShadowAudioTrack,
            audioData: ByteArray,
            format: AudioFormat
        ) {
            totalWriteCalls.incrementAndGet()
            if (hasNonZeroPcmAmplitude(audioData, format.encoding)) {
                nonSilentChunks.incrementAndGet()
            }
        }

        fun reset() {
            totalWriteCalls.set(0)
            nonSilentChunks.set(0)
        }

        private fun hasNonZeroPcmAmplitude(bytes: ByteArray, encoding: Int): Boolean {
            if (bytes.isEmpty()) return false
            return if (encoding == AudioFormat.ENCODING_PCM_16BIT && bytes.size >= 2) {
                val shortBuf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                var nonZero = false
                while (shortBuf.hasRemaining()) {
                    if (abs(shortBuf.get().toInt()) > 8) {
                        nonZero = true
                        break
                    }
                }
                nonZero
            } else {
                bytes.any { it.toInt() != 0 }
            }
        }
    }

    fun appContext(): Context = ApplicationProvider.getApplicationContext()

    /**
     * Pre-registers hardware accelerometer + gyroscope sensors (making 0-listener splash checks
     * 100% non-vacuous and player face-down events genuine), enables vibrator amplitude control,
     * enables notifications, attaches PCM audio probe, and initializes WorkManager preserving any
     * custom Configuration.Provider WorkerFactory.
     */
    fun prepareEnvironment(audioProbe: PcmAudioProbe? = null): Sensor {
        val context = appContext()

        ShadowSensorManager.reset()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowSm = Shadows.shadowOf(sensorManager)
        val accel = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        val gyro = ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE)
        shadowSm.addSensor(accel)
        shadowSm.addSensor(gyro)

        ShadowVibrator.reset()
        val vibrator = resolveVibrator(context)
        Shadows.shadowOf(vibrator).apply {
            setHasVibrator(true)
            setHasAmplitudeControl(true)
        }

        ShadowNotificationManager.reset()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Shadows.shadowOf(notificationManager).setNotificationsEnabled(true)

        ShadowAudioTrack.resetTest()
        if (audioProbe != null) {
            audioProbe.reset()
            ShadowAudioTrack.addAudioDataListener(audioProbe)
        }

        val baseConfig = (context.applicationContext as? Configuration.Provider)
            ?.workManagerConfiguration
            ?: Configuration.Builder().build()
        val syncExecutor = SynchronousExecutor()
        val testConfig = Configuration.Builder(baseConfig)
            .setExecutor(syncExecutor)
            .setTaskExecutor(syncExecutor)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, testConfig)

        return accel
    }

    fun tearDownEnvironment(
        activeScenario: ActivityScenario<Activity>?,
        composeRule: ComposeTestRule? = null,
        audioProbe: PcmAudioProbe? = null
    ) {
        runCatching { composeRule?.mainClock?.autoAdvance = true }
        runCatching { activeScenario?.close() }
        if (audioProbe != null) {
            runCatching { ShadowAudioTrack.removeAudioDataListener(audioProbe) }
        }
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
    }

    fun resolveVibrator(context: Context = appContext()): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Timezone-safe epoch millisecond builder in system default zone (matching ClockProvider). */
    fun epochAtLocal(
        year: Int = 2026,
        month: Int = 11,
        dayOfMonth: Int = 18, // 2026-11-18 is a Wednesday
        hour: Int = 10,
        minute: Int = 0,
        second: Int = 0
    ): Long = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    fun buildMainIntent(
        fromReminderQuickStart: Boolean = false,
        forceSplashPreview: Boolean = false,
        simulatedEpochMs: Long? = null,
        resetState: Boolean = false
    ): Intent {
        val context = appContext()
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName(context, MAIN_ACTIVITY_CLASS)
            addCategory(Intent.CATEGORY_LAUNCHER)
            if (fromReminderQuickStart) putExtra(EXTRA_FROM_REMINDER_QUICK_START, true)
            if (forceSplashPreview) putExtra(EXTRA_FORCE_SPLASH_PREVIEW, true)
            if (simulatedEpochMs != null) putExtra(EXTRA_SIMULATED_EPOCH_MS, simulatedEpochMs)
            if (resetState) putExtra(EXTRA_RESET_STATE, true)
        }
    }

    fun launchMain(
        composeRule: ComposeTestRule,
        fromReminderQuickStart: Boolean = false,
        forceSplashPreview: Boolean = false,
        simulatedEpochMs: Long? = null,
        resetState: Boolean = false,
        autoAdvanceClock: Boolean = true
    ): ActivityScenario<Activity> {
        if (simulatedEpochMs != null) {
            SystemClock.setCurrentTimeMillis(simulatedEpochMs)
        }
        composeRule.mainClock.autoAdvance = autoAdvanceClock
        val intent = buildMainIntent(
            fromReminderQuickStart = fromReminderQuickStart,
            forceSplashPreview = forceSplashPreview,
            simulatedEpochMs = simulatedEpochMs,
            resetState = resetState
        )
        val scenario = ActivityScenario.launch<Activity>(intent)
        if (autoAdvanceClock) {
            ShadowLooper.idleMainLooper()
            composeRule.waitForIdle()
        } else {
            ShadowLooper.idleMainLooper()
            composeRule.mainClock.advanceTimeByFrame()
        }
        return scenario
    }

    /** Launches cleanly to HomeScreen, dismissing splash or first-launch onboarding if shown. */
    fun launchToHome(
        composeRule: ComposeTestRule,
        resetState: Boolean = true,
        simulatedEpochMs: Long = epochAtLocal(2026, 11, 18, 10, 0)
    ): ActivityScenario<Activity> {
        val scenario = launchMain(
            composeRule = composeRule,
            simulatedEpochMs = simulatedEpochMs,
            resetState = resetState,
            autoAdvanceClock = true
        )
        dismissSplashIfPresent(composeRule)
        dismissOnboardingIfPresent(composeRule)
        assertTagDisplayed(composeRule, "home_screen")
        return scenario
    }

    fun dismissSplashIfPresent(composeRule: ComposeTestRule) {
        if (tagExists(composeRule, "splash_ad_skip_button")) {
            findNodeByTag(composeRule, "splash_ad_skip_button").performClick()
            ShadowLooper.idleMainLooper()
            if (composeRule.mainClock.autoAdvance) {
                composeRule.waitForIdle()
            } else {
                composeRule.mainClock.advanceTimeByFrame()
            }
        }
    }

    fun dismissOnboardingIfPresent(composeRule: ComposeTestRule) {
        if (tagExists(composeRule, "onboarding_enter_button")) {
            scrollToAndClick(composeRule, "onboarding_enter_button")
            ShadowLooper.idleMainLooper()
            if (composeRule.mainClock.autoAdvance) {
                composeRule.waitForIdle()
            } else {
                composeRule.mainClock.advanceTimeByFrame()
            }
        }
    }

    /** Advances BOTH Robolectric Main Looper (ViewModel/SystemClock) AND Compose MainTestClock in lockstep. */
    fun advanceAppClockBy(
        composeRule: ComposeTestRule,
        totalMillis: Long,
        stepMillis: Long = 100L
    ) {
        var remaining = totalMillis
        while (remaining > 0L) {
            val step = min(remaining, stepMillis)
            SystemClock.setCurrentTimeMillis(System.currentTimeMillis() + step)
            ShadowLooper.shadowMainLooper().idleFor(Duration.ofMillis(step))
            composeRule.mainClock.advanceTimeBy(step)
            remaining -= step
        }
        ShadowLooper.idleMainLooper()
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    /** Simulates moving app to background for elapsedMillis and resuming warm. */
    fun simulateWarmResumeAfter(
        scenario: ActivityScenario<Activity>,
        composeRule: ComposeTestRule,
        baseEpochMs: Long,
        elapsedMillis: Long
    ) {
        scenario.moveToState(Lifecycle.State.CREATED)
        ShadowLooper.idleMainLooper()
        val resumedEpoch = baseEpochMs + elapsedMillis
        SystemClock.setCurrentTimeMillis(resumedEpoch)
        ShadowLooper.shadowMainLooper().idleFor(Duration.ofMillis(elapsedMillis))
        scenario.onActivity { activity ->
            activity.intent = Intent(activity.intent).putExtra(EXTRA_SIMULATED_EPOCH_MS, resumedEpoch)
        }
        scenario.moveToState(Lifecycle.State.RESUMED)
        ShadowLooper.idleMainLooper()
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    // ---- Semantics Lookup, Toggleable State, Resilient Scroll & Navigation Helpers ----

    fun tagExists(composeRule: ComposeTestRule, tag: String): Boolean {
        val mergedCount = runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes().size
        }.getOrDefault(0)
        if (mergedCount > 0) return true
        return runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
    }

    fun countNodesWithTag(composeRule: ComposeTestRule, tag: String): Int {
        val merged = runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes().size
        }.getOrDefault(0)
        if (merged > 0) return merged
        return runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size
        }.getOrDefault(0)
    }

    fun findNodeByTag(composeRule: ComposeTestRule, tag: String): SemanticsNodeInteraction {
        val mergedNodes = runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        return if (mergedNodes.isNotEmpty()) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false)[0]
        } else {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0]
        }
    }

    fun assertTagDisplayed(composeRule: ComposeTestRule, tag: String): SemanticsNodeInteraction {
        scrollToTagIfNeeded(composeRule, tag)
        return findNodeByTag(composeRule, tag).assertIsDisplayed()
    }

    /** Verifies touch target >= 48dp (allowing 0.5dp density float-rounding tolerance per explorer_3 Rule 10). */
    fun assertMinTouchBounds(
        composeRule: ComposeTestRule,
        tag: String,
        minWidth: Dp = 48.dp,
        minHeight: Dp = 48.dp
    ) {
        val node = findNodeByTag(composeRule, tag)
        node.assertWidthIsAtLeast((minWidth.value - 0.5f).dp)
        node.assertHeightIsAtLeast((minHeight.value - 0.5f).dp)
    }

    /** Handles both eager verticalScroll/horizontalScroll AND lazy LazyColumn/LazyRow containers (up + down). */
    fun scrollToTagIfNeeded(composeRule: ComposeTestRule, tag: String) {
        if (tagExists(composeRule, tag)) {
            val alreadyShown = runCatching {
                findNodeByTag(composeRule, tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (alreadyShown) return
            runCatching {
                findNodeByTag(composeRule, tag).performScrollTo()
                if (composeRule.mainClock.autoAdvance) composeRule.waitForIdle()
            }
            if (runCatching { findNodeByTag(composeRule, tag).assertIsDisplayed(); true }.getOrDefault(false)) {
                return
            }
        }

        val matcher: SemanticsMatcher = hasTestTag(tag)
        val scrollers = runCatching {
            composeRule.onAllNodes(hasScrollAction(), useUnmergedTree = false)
        }.getOrNull()
        val scrollerCount = runCatching { scrollers?.fetchSemanticsNodes()?.size ?: 0 }.getOrDefault(0)
        for (i in 0 until scrollerCount) {
            val scrolled = runCatching {
                scrollers!![i].performScrollToNode(matcher)
                if (composeRule.mainClock.autoAdvance) composeRule.waitForIdle()
                true
            }.getOrDefault(false)
            if (scrolled && tagExists(composeRule, tag)) return
        }

        for (attempt in 0 until 5) {
            if (tagExists(composeRule, tag) &&
                runCatching { findNodeByTag(composeRule, tag).assertIsDisplayed(); true }.getOrDefault(false)
            ) {
                return
            }
            if (scrollerCount > 0) {
                runCatching {
                    scrollers!![0].performTouchInput { swipeUp() }
                    if (composeRule.mainClock.autoAdvance) composeRule.waitForIdle()
                }
            }
        }

        for (attempt in 0 until 5) {
            if (tagExists(composeRule, tag) &&
                runCatching { findNodeByTag(composeRule, tag).assertIsDisplayed(); true }.getOrDefault(false)
            ) {
                return
            }
            if (scrollerCount > 0) {
                runCatching {
                    scrollers!![0].performTouchInput { swipeDown() }
                    if (composeRule.mainClock.autoAdvance) composeRule.waitForIdle()
                }
            }
        }
    }

    fun scrollToAndClick(composeRule: ComposeTestRule, tag: String) {
        scrollToTagIfNeeded(composeRule, tag)
        val clicked = runCatching {
            findNodeByTag(composeRule, tag).performClick()
            true
        }.getOrDefault(false)
        if (!clicked) {
            // Fallback if clickable/toggleable child is inside unmerged subtree
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].performClick()
        }
        ShadowLooper.idleMainLooper()
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    fun navigateToTab(composeRule: ComposeTestRule, tabTag: String) {
        assertTrue("Unknown tab tag: $tabTag", tabTag in ALL_NAV_TABS)
        scrollToAndClick(composeRule, tabTag)
    }

    /** Reads Switch/Checkbox ToggleableState or Selected boolean across merged and unmerged subtrees. */
    fun isSwitchOrChipActive(composeRule: ComposeTestRule, tag: String): Boolean {
        scrollToTagIfNeeded(composeRule, tag)
        val candidates = mutableListOf<SemanticsNode>()
        runCatching {
            candidates.addAll(composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes())
        }
        runCatching {
            candidates.addAll(composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes())
        }
        for (node in candidates) {
            if (inspectNodeActiveRecursive(node)) return true
        }
        return false
    }

    private fun inspectNodeActiveRecursive(node: SemanticsNode): Boolean {
        node.config.getOrNull(SemanticsProperties.ToggleableState)?.let {
            if (it == ToggleableState.On) return true
            if (it == ToggleableState.Off) return false
        }
        node.config.getOrNull(SemanticsProperties.Selected)?.let {
            if (it) return true
        }
        node.config.getOrNull(SemanticsProperties.StateDescription)?.let { desc ->
            val lower = desc.lowercase()
            if (lower.contains("selected") || lower.contains("on") || lower.contains("checked") || desc.contains("已选") || desc.contains("开启")) {
                return true
            }
        }
        for (child in node.children) {
            if (inspectNodeActiveRecursive(child)) return true
        }
        return false
    }

    /** Toggles a Switch/Toggleable tagged node reliably whether Modifier.testTag is on the Switch or its Row container. */
    fun toggleSwitchByTag(composeRule: ComposeTestRule, tag: String) {
        scrollToTagIfNeeded(composeRule, tag)
        val mergedNodes = runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        if (mergedNodes.isNotEmpty() && mergedNodes[0].config.contains(SemanticsActions.OnClick)) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = false)[0].performClick()
        } else {
            findNodeByTag(composeRule, tag).performClick()
        }
        ShadowLooper.idleMainLooper()
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    fun countSelectedBodyMapZones(composeRule: ComposeTestRule): Int {
        return ALL_BODYMAP_ZONE_TAGS.count { zoneTag ->
            tagExists(composeRule, zoneTag) && isSwitchOrChipActive(composeRule, zoneTag)
        }
    }

    /** Recursively reads all Text + ContentDescription + StateDescription strings under a tagged node. */
    fun readNodeText(composeRule: ComposeTestRule, tag: String): String {
        scrollToTagIfNeeded(composeRule, tag)
        val parts = LinkedHashSet<String>()
        runCatching {
            val m = composeRule.onAllNodesWithTag(tag, useUnmergedTree = false)[0].fetchSemanticsNode()
            collectNodeStrings(m, parts)
        }
        runCatching {
            val u = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].fetchSemanticsNode()
            collectNodeStrings(u, parts)
        }
        return parts.joinToString(" ").trim()
    }

    fun collectAllScreenText(composeRule: ComposeTestRule): String {
        val roots = composeRule.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true)
            .fetchSemanticsNodes()
        val parts = mutableListOf<String>()
        for (n in roots) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { parts.add(it.text) }
            n.config.getOrNull(SemanticsProperties.EditableText)?.let { parts.add(it.text) }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { parts.add(it) }
        }
        return parts.joinToString(" ")
    }

    private fun collectNodeStrings(node: SemanticsNode, sink: MutableCollection<String>) {
        node.config.getOrNull(SemanticsProperties.Text)?.forEach { sink.add(it.text) }
        node.config.getOrNull(SemanticsProperties.EditableText)?.let { sink.add(it.text) }
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { sink.add(it) }
        node.config.getOrNull(SemanticsProperties.StateDescription)?.let { sink.add(it) }
        for (child in node.children) {
            collectNodeStrings(child, sink)
        }
    }

    /** Advances poses until completion_screen is displayed (supporting next-button or clock-driven completion). */
    fun advanceRoutineToCompletion(composeRule: ComposeTestRule, maxSteps: Int = 16) {
        for (step in 0 until maxSteps) {
            if (tagExists(composeRule, "completion_screen")) return
            if (tagExists(composeRule, "player_next_pose_button")) {
                scrollToAndClick(composeRule, "player_next_pose_button")
            } else {
                advanceAppClockBy(composeRule, 50_000L, stepMillis = 1_000L)
            }
        }
        if (!tagExists(composeRule, "completion_screen")) {
            advanceAppClockBy(composeRule, 65_000L, stepMillis = 1_000L)
        }
        assertTagDisplayed(composeRule, "completion_screen")
    }

    // ---- Sensor, Haptic, Notification, Share Intent & Window Flag Inspectors ----

    fun activeSensorListenerCount(): Int {
        val sm = appContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return Shadows.shadowOf(sm).listeners.size
    }

    /** Dispatches sustained accelerometer samples (handling instant or debounced/smoothed monitors). */
    fun dispatchAccelerometer(
        composeRule: ComposeTestRule,
        x: Float,
        y: Float,
        z: Float,
        sampleCount: Int = 5,
        intervalMs: Long = 100L
    ) {
        val sm = appContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowSm: ShadowSensorManager = Shadows.shadowOf(sm)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER).also { shadowSm.addSensor(it) }
        var tsNs = System.nanoTime()
        repeat(sampleCount) {
            val event = SensorEventBuilder.newBuilder()
                .setSensor(accel)
                .setTimestamp(tsNs)
                .setValues(floatArrayOf(x, y, z))
                .build()
            shadowSm.sendSensorEventToListeners(event)
            tsNs += intervalMs * 1_000_000L
            advanceAppClockBy(composeRule, intervalMs, stepMillis = intervalMs)
        }
    }

    fun resetHapticLedger() {
        ShadowVibrator.reset()
        Shadows.shadowOf(resolveVibrator()).apply {
            setHasVibrator(true)
            setHasAmplitudeControl(true)
        }
    }

    fun hasRecordedHapticPulse(): Boolean {
        val sv = Shadows.shadowOf(resolveVibrator())
        return sv.isVibrating || sv.milliseconds > 0L || (sv.pattern != null && sv.pattern.isNotEmpty())
    }

    fun postedNotifications(): List<Notification> {
        val nm = appContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return Shadows.shadowOf(nm).allNotifications
    }

    fun clearPostedNotifications() {
        val nm = appContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
        ShadowNotificationManager.reset()
        Shadows.shadowOf(nm).setNotificationsEnabled(true)
    }

    fun extractNotificationQuickStartIntents(notification: Notification): List<Intent> {
        val intents = mutableListOf<Intent>()
        notification.contentIntent?.let { pi: PendingIntent ->
            Shadows.shadowOf(pi).savedIntent?.let { intents.add(it) }
        }
        notification.actions?.forEach { action ->
            action.actionIntent?.let { pi ->
                Shadows.shadowOf(pi).savedIntent?.let { intents.add(it) }
            }
        }
        return intents
    }

    /** Inspects started Activity Intent, unwrapping ACTION_CHOOSER if Intent.createChooser was used. */
    fun peekDispatchedShareIntent(scenario: ActivityScenario<Activity>): Intent? {
        var found: Intent? = null
        scenario.onActivity { activity ->
            val raw = Shadows.shadowOf(activity).peekNextStartedActivity()
            found = if (raw?.action == Intent.ACTION_CHOOSER) {
                @Suppress("DEPRECATION")
                raw.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent ?: raw
            } else {
                raw
            }
        }
        return found
    }

    fun hasKeepScreenOnFlag(scenario: ActivityScenario<Activity>): Boolean {
        var keepOn = false
        scenario.onActivity { activity ->
            keepOn = (activity.window.attributes.flags and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0 ||
                hasKeepScreenOnViewRecursive(activity.window.decorView)
        }
        return keepOn
    }

    private fun hasKeepScreenOnViewRecursive(view: View): Boolean {
        if (view.keepScreenOn) return true
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                if (hasKeepScreenOnViewRecursive(view.getChildAt(i))) return true
            }
        }
        return false
    }

    fun isAudioMutedOrInactive(): Boolean {
        val am = appContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return !am.isMusicActive
    }

    // ---- WorkManager Opaque-Box Inspection & Trigger Helpers ----

    fun activeScheduledWorkInfos(): List<WorkInfo> {
        val query = WorkQuery.fromStates(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)
        return WorkManager.getInstance(appContext()).getWorkInfos(query).get(5, TimeUnit.SECONDS)
    }

    fun triggerScheduledReminderWork(composeRule: ComposeTestRule): Int {
        val context = appContext()
        val testDriver = checkNotNull(WorkManagerTestInitHelper.getTestDriver(context)) {
            "WorkManager TestDriver not initialized"
        }
        val infos = activeScheduledWorkInfos()
        for (info in infos) {
            testDriver.setAllConstraintsMet(info.id)
            testDriver.setInitialDelayMet(info.id)
            if (info.periodicityInfo != null) {
                testDriver.setPeriodDelayMet(info.id)
            }
        }
        Thread.sleep(40L) // Allow CoroutineWorker Dispatchers.Default/IO hop if any
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
        return postedNotifications().size
    }

    // ---- Synchronous Skia Native Bitmap Capture & Color/Contrast Analysis ----

    fun captureDecorBitmap(
        scenario: ActivityScenario<Activity>,
        composeRule: ComposeTestRule
    ): Bitmap {
        if (composeRule.mainClock.autoAdvance) {
            composeRule.waitForIdle()
        } else {
            composeRule.mainClock.advanceTimeByFrame()
        }
        ShadowLooper.idleMainLooper()
        var full: Bitmap? = null
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val dm = activity.resources.displayMetrics
            if (decor.width <= 0 || decor.height <= 0) {
                decor.measure(
                    View.MeasureSpec.makeMeasureSpec(dm.widthPixels.coerceAtLeast(360), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dm.heightPixels.coerceAtLeast(640), View.MeasureSpec.EXACTLY)
                )
                decor.layout(0, 0, decor.measuredWidth, decor.measuredHeight)
            }
            val w = decor.width.coerceAtLeast(2)
            val h = decor.height.coerceAtLeast(2)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bmp))
            full = bmp
        }
        return checkNotNull(full)
    }

    fun captureTagBitmap(
        scenario: ActivityScenario<Activity>,
        composeRule: ComposeTestRule,
        tag: String
    ): Bitmap {
        scrollToTagIfNeeded(composeRule, tag)
        val bounds: Rect = findNodeByTag(composeRule, tag).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected positive bounds for Canvas/node '$tag', got $bounds",
            bounds.width > 1f && bounds.height > 1f
        )
        val full = captureDecorBitmap(scenario, composeRule)
        val left = bounds.left.roundToInt().coerceIn(0, full.width - 1)
        val top = bounds.top.roundToInt().coerceIn(0, full.height - 1)
        val width = bounds.width.roundToInt().coerceIn(1, full.width - left)
        val height = bounds.height.roundToInt().coerceIn(1, full.height - top)
        return Bitmap.createBitmap(full, left, top, width, height)
    }

    fun analyzeBitmapColors(bitmap: Bitmap): BitmapColorStats {
        val stepX = max(1, bitmap.width / 64)
        val stepY = max(1, bitmap.height / 64)
        val histogram = HashMap<Int, Int>()
        var minLum = 1.0
        var maxLum = 0.0
        var darkestRgb = 0x000000
        var lightestRgb = 0xFFFFFF
        var lumSum = 0.0
        var sampleCount = 0
        val digest = MessageDigest.getInstance("SHA-256")

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val argb = bitmap.getPixel(x, y)
                val rgb = argb and 0x00FFFFFF
                digest.update(
                    byteArrayOf(
                        ((rgb shr 16) and 0xFF).toByte(),
                        ((rgb shr 8) and 0xFF).toByte(),
                        (rgb and 0xFF).toByte()
                    )
                )
                histogram[rgb] = (histogram[rgb] ?: 0) + 1
                val lum = relativeLuminance(rgb)
                lumSum += lum
                sampleCount++
                if (lum < minLum) {
                    minLum = lum
                    darkestRgb = rgb
                }
                if (lum > maxLum) {
                    maxLum = lum
                    lightestRgb = rgb
                }
                x += stepX
            }
            y += stepY
        }

        val dominantRgb = histogram.maxByOrNull { it.value }?.key ?: 0
        val contrast = (maxLum + 0.05) / (minLum + 0.05)
        val hashHex = digest.digest().joinToString("") { "%02x".format(it) }
        return BitmapColorStats(
            width = bitmap.width,
            height = bitmap.height,
            distinctRgbCount = histogram.size,
            dominantRgb = dominantRgb,
            darkestRgb = darkestRgb,
            lightestRgb = lightestRgb,
            averageLuminance = if (sampleCount > 0) lumSum / sampleCount else 0.0,
            maxContrastRatio = contrast,
            pixelHashHex = hashHex
        )
    }

    fun relativeLuminance(rgb: Int): Double {
        fun ch(c: Int): Double {
            val s = (c and 0xFF) / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(Color.red(rgb)) + 0.7152 * ch(Color.green(rgb)) + 0.0722 * ch(Color.blue(rgb))
    }

    fun wcagContrastRatio(rgbA: Int, rgbB: Int): Double {
        val l1 = relativeLuminance(rgbA)
        val l2 = relativeLuminance(rgbB)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    // ---- APK Binary, Dalvik .dex MUTF-8, Manifest, Signing & Roborazzi Verifiers ----

    fun resolveSongxiAndroidRoot(): File {
        val direct = File("/home/antigravity/songxi-android")
        if (direct.exists()) return direct
        var cur: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (cur != null) {
            if (File(cur, "settings.gradle.kts").exists()) return cur
            cur = cur.parentFile
        }
        return direct
    }

    fun debugApkFile(androidRoot: File = resolveSongxiAndroidRoot()): File =
        File(androidRoot, "app/build/outputs/apk/debug/app-debug.apk")

    fun releaseApkFile(androidRoot: File = resolveSongxiAndroidRoot()): File =
        File(androidRoot, "app/build/outputs/apk/release/app-release.apk")

    fun unsignedReleaseApkFile(androidRoot: File = resolveSongxiAndroidRoot()): File =
        File(androidRoot, "app/build/outputs/apk/release/app-release-unsigned.apk")

    fun resolveBuildToolBinary(binaryName: String): File {
        val sdkRoot = File(
            System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT")
                ?: "/home/antigravity/.android-sdk"
        )
        val preferred = File(sdkRoot, "build-tools/35.0.0/$binaryName")
        if (preferred.canExecute()) return preferred
        val fallback = File(sdkRoot, "build-tools")
            .listFiles()
            ?.sortedByDescending { it.name }
            ?.map { File(it, binaryName) }
            ?.firstOrNull { it.canExecute() }
        assertNotNull(
            "Android SDK build-tools binary '$binaryName' not found under ${sdkRoot.absolutePath}",
            fallback
        )
        return fallback!!
    }

    /**
     * Executes an SDK build-tools CLI binary (aapt2 / apksigner) with strict non-/tmp environment
     * and filters Hotspot's `Picked up JAVA_TOOL_OPTIONS:` banner from stderr.
     */
    fun runSdkTool(binaryName: String, vararg args: String): CmdResult {
        val scratchTmp = System.getProperty("java.io.tmpdir")
            ?.takeIf { !it.startsWith("/tmp") }
            ?: "/home/antigravity/.gemini/jetski/scratch/teamwork_tmp/e2e_runner/tmp"
        File(scratchTmp).mkdirs()
        val pb = ProcessBuilder(listOf(resolveBuildToolBinary(binaryName).absolutePath) + args)
        pb.environment()["TMPDIR"] = scratchTmp
        pb.environment()["TMP"] = scratchTmp
        pb.environment()["TEMP"] = scratchTmp
        pb.environment()["JAVA_HOME"] = "/usr/lib/jvm/java-17-openjdk-amd64"
        pb.environment()["JAVA_TOOL_OPTIONS"] = "-Djava.io.tmpdir=$scratchTmp -XX:-UsePerfData"
        pb.environment()["JDK_JAVA_OPTIONS"] = "-Djava.io.tmpdir=$scratchTmp -XX:-UsePerfData"
        val proc = pb.start()
        val stdout = proc.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val stderr = proc.errorStream.bufferedReader(Charsets.UTF_8).readText()
            .lineSequence()
            .filterNot {
                it.startsWith("Picked up JAVA_TOOL_OPTIONS:") ||
                    it.startsWith("NOTE: Picked up JDK_JAVA_OPTIONS:")
            }
            .joinToString("\n")
            .trim()
        val exitCode = proc.waitFor()
        return CmdResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }

    /**
     * Parses every Dalvik classes*.dex string_ids MUTF-8 table directly + `aapt2 dump strings`
     * on compiled resources.arsc, asserting > 500 strings + MainActivity & 松息 non-vacuity.
     */
    fun extractAllApkUtf8Strings(apkFile: File): Set<String> {
        assertTrue("APK file does not exist: ${apkFile.absolutePath}", apkFile.isFile)
        val result = LinkedHashSet<String>()
        ZipFile(apkFile).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.matches(Regex("classes\\d*\\.dex")) }
                .toList()
            assertTrue("Expected at least 1 classes.dex inside ${apkFile.name}", dexEntries.isNotEmpty())
            for (entry in dexEntries) {
                val bytes = zip.getInputStream(entry).readBytes()
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val magic = String(bytes, 0, 4, Charsets.US_ASCII)
                assertEquals("Invalid DEX magic in ${entry.name}", "dex\n", magic)
                val stringIdsSize = buf.getInt(0x38)
                val stringIdsOff = buf.getInt(0x3C)
                for (i in 0 until stringIdsSize) {
                    var pos = buf.getInt(stringIdsOff + i * 4)
                    if (pos <= 0 || pos >= bytes.size) continue
                    // Skip ULEB128 utf16_size prefix
                    while (pos < bytes.size && (bytes[pos].toInt() and 0x80) != 0) pos++
                    pos++
                    val start = pos
                    while (pos < bytes.size && bytes[pos] != 0.toByte()) pos++
                    if (pos > start) {
                        result.add(String(bytes, start, pos - start, Charsets.UTF_8))
                    }
                }
            }
        }

        val resDump = runSdkTool("aapt2", "dump", "strings", apkFile.absolutePath)
        assertEquals("aapt2 dump strings failed: ${resDump.stderr}", 0, resDump.exitCode)
        resDump.stdout.lineSequence().forEach { line ->
            if (line.isNotBlank()) result.add(line)
        }

        assertTrue(
            "DEX/resource string extractor yielded too few strings (${result.size}) — vacuity check failed",
            result.size > 500
        )
        assertTrue(
            "Extracted APK strings must contain 'MainActivity' and '松息' (non-vacuity proof)",
            result.any { it.contains("MainActivity") } && result.any { it.contains("松息") }
        )
        return result
    }

    fun scanApkForForbiddenStrings(
        apkFile: File,
        forbiddenTerms: List<String> = FORBIDDEN_APK_BINARY_PATTERNS
    ): List<String> {
        val allStrings = extractAllApkUtf8Strings(apkFile)
        val hits = mutableListOf<String>()
        for (term in forbiddenTerms) {
            val matching = allStrings.firstOrNull { it.contains(term) }
            if (matching != null) {
                hits.add("$term (in: ${matching.take(80)})")
            }
        }
        return hits
    }

    /** F15: Validates app-debug.apk & signed app-release.apk badging + apksigner v2/v3 verification. */
    fun assertValidSignedReleaseAndDebugApks(androidRoot: File = resolveSongxiAndroidRoot()) {
        val dbg = debugApkFile(androidRoot)
        val rel = releaseApkFile(androidRoot)
        val unsignedRel = unsignedReleaseApkFile(androidRoot)

        assertTrue(
            "Missing or trivially small app-debug.apk at ${dbg.absolutePath}",
            dbg.isFile && dbg.length() > 100_000L
        )
        if (!rel.isFile && unsignedRel.isFile) {
            throw AssertionError(
                "Found app-release-unsigned.apk instead of signed app-release.apk; " +
                    "configure release signingConfig in :app/build.gradle.kts per PROJECT.md F15"
            )
        }
        assertTrue(
            "Missing or trivially small signed app-release.apk at ${rel.absolutePath}",
            rel.isFile && rel.length() > 100_000L
        )

        for ((label, apk) in listOf("debug" to dbg, "release" to rel)) {
            val badging = runSdkTool("aapt2", "dump", "badging", apk.absolutePath)
            assertEquals("aapt2 dump badging ($label) failed: ${badging.stderr}", 0, badging.exitCode)
            val out = badging.stdout
            if (label == "release") {
                assertTrue(
                    "Release APK package must be com.songxi.app, got:\n${out.lineSequence().firstOrNull()}",
                    out.contains("package: name='com.songxi.app'")
                )
            } else {
                assertTrue(
                    "Debug APK package must start with com.songxi.app",
                    out.contains("package: name='com.songxi.app")
                )
            }
            assertTrue("Expected minSdk 26 in $label APK badging", out.contains("sdkVersion:'26'"))
            assertTrue("Expected targetSdk 34 in $label APK badging", out.contains("targetSdkVersion:'34'"))
            assertTrue(
                "Expected launchable MainActivity in $label APK badging",
                out.contains("launchable-activity: name='com.songxi.app.MainActivity'")
            )
            assertTrue("Expected app label containing '松息' in $label APK", out.contains("松息"))
        }

        val signRes = runSdkTool("apksigner", "verify", "--verbose", "--print-certs", rel.absolutePath)
        assertEquals(
            "apksigner verify failed on app-release.apk: ${signRes.stderr}\n${signRes.stdout}",
            0,
            signRes.exitCode
        )
        assertTrue("Expected 'Verifies' in apksigner output", signRes.stdout.contains("Verifies"))
        assertTrue(
            "Expected APK Signature Scheme v2 or v3 true in apksigner output:\n${signRes.stdout}",
            signRes.stdout.contains("Verified using v2 scheme (APK Signature Scheme v2): true") ||
                signRes.stdout.contains("Verified using v3 scheme (APK Signature Scheme v3): true")
        )
        assertTrue(
            "Expected Signer #1 SHA-256 certificate digest in apksigner output",
            Regex("Signer #1 certificate SHA-256 digest:\\s*[0-9a-fA-F]{64}").containsMatchIn(signRes.stdout)
        )
    }

    /** F15 & F4: Jetpack-aware closed permission allowlist + invasive tracker/billing denylist. */
    fun assertCleanManifestPermissions(apkFile: File = releaseApkFile()) {
        assertTrue("APK file missing for permission audit: ${apkFile.absolutePath}", apkFile.isFile)
        val permRes = runSdkTool("aapt2", "dump", "permissions", apkFile.absolutePath)
        assertEquals("aapt2 dump permissions failed: ${permRes.stderr}", 0, permRes.exitCode)

        val permRegex = Regex("uses-permission(?:-sdk-23)?:\\s*name='([^']+)'")
        val declared = permRegex.findAll(permRes.stdout).map { it.groupValues[1] }.toSet()

        // Positive non-vacuity checks: gentle haptic & reminder notification permissions MUST exist
        assertTrue(
            "Manifest must declare android.permission.VIBRATE, found: $declared",
            "android.permission.VIBRATE" in declared
        )
        assertTrue(
            "Manifest must declare android.permission.POST_NOTIFICATIONS, found: $declared",
            "android.permission.POST_NOTIFICATIONS" in declared
        )

        val highRiskDenylist = setOf(
            "com.android.vending.BILLING",
            "com.google.android.gms.permission.AD_ID",
            "android.permission.INTERNET",
            "android.permission.READ_PHONE_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.READ_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.BODY_SENSORS"
        )
        val forbiddenHits = declared.intersect(highRiskDenylist)
        assertTrue(
            "APK declares forbidden billing/tracker/invasive permissions: $forbiddenHits",
            forbiddenHits.isEmpty()
        )

        val closedAllowlist = setOf(
            "android.permission.VIBRATE",
            "android.permission.POST_NOTIFICATIONS",
            // Auto-merged by official androidx.work:work-runtime:2.10.0 & androidx.core:1.15.0
            "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.FOREGROUND_SERVICE",
            "com.songxi.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            "com.songxi.app.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        )
        val unexpected = declared - closedAllowlist
        assertTrue(
            "APK declares unexpected permission(s) outside closed sanctuary allowlist: $unexpected",
            unexpected.isEmpty()
        )
    }

    /** F15: Discovers & validates Roborazzi 4-skin PNG header, dimensions, non-blank colors & RGBA-SHA256 hashes. */
    fun discoverFourSkinPngs(androidRoot: File = resolveSongxiAndroidRoot()): Map<String, File> {
        val skinKeys = listOf("ink_mist", "qinglv_celadon", "dusk_pine", "spring_blossom")
        val candidates = Files.walk(androidRoot.toPath()).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".png", ignoreCase = true) }
                .map { it.toFile() }
                .filter { f ->
                    val p = f.invariantSeparatorsPath.lowercase()
                    !p.contains("/src/main/res/") &&
                        (p.contains("roborazzi") || p.contains("snapshot") || p.contains("screenshot") || p.contains("skin"))
                }
                .toList()
        }
        val matched = LinkedHashMap<String, File>()
        for (skinKey in skinKeys) {
            val compact = skinKey.replace("_", "")
            val hit = candidates
                .filter { f ->
                    val norm = f.nameWithoutExtension.lowercase().replace("-", "_")
                    norm.contains(skinKey) || norm.replace("_", "").contains(compact)
                }
                .maxByOrNull { it.lastModified() }
            assertNotNull(
                "Missing Roborazzi PNG capture for skin '$skinKey' under ${androidRoot.absolutePath}",
                hit
            )
            matched[skinKey] = hit!!
        }
        return matched
    }

    fun assertValidAndDistinctSkinPngs(androidRoot: File = resolveSongxiAndroidRoot()) {
        val pngMagic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val skinFiles = discoverFourSkinPngs(androidRoot)
        val rgbaHashes = LinkedHashMap<String, String>()
        val avgLuminances = LinkedHashMap<String, Double>()

        for ((skinKey, file) in skinFiles) {
            val bytes = file.readBytes()
            assertTrue("PNG for $skinKey is trivially small (${bytes.size}B)", bytes.size > 1024)
            assertArrayEquals(
                "Invalid 8-byte PNG signature in ${file.name}",
                pngMagic,
                bytes.copyOfRange(0, 8)
            )
            assertEquals(
                "Expected IHDR chunk at offset 12 in ${file.name}",
                "IHDR",
                String(bytes, 12, 4, Charsets.US_ASCII)
            )
            val ihdr = ByteBuffer.wrap(bytes, 16, 8).order(ByteOrder.BIG_ENDIAN)
            val width = ihdr.int
            val height = ihdr.int
            assertTrue(
                "PNG $skinKey dimensions (${width}x${height}) must be >= 240x240",
                width >= 240 && height >= 240
            )

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull("BitmapFactory failed to decode ${file.absolutePath}", bitmap)
            val pixels = IntArray(bitmap!!.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val distinctColors = HashSet<Int>()
            val step = max(1, pixels.size / 2500)
            var lumSum = 0.0
            var lumCount = 0
            for (idx in pixels.indices step step) {
                val argb = pixels[idx]
                distinctColors.add(argb)
                lumSum += relativeLuminance(argb and 0x00FFFFFF)
                lumCount++
            }
            assertTrue(
                "Roborazzi PNG for $skinKey appears blank/solid (${distinctColors.size} colors)",
                distinctColors.size >= 4
            )

            val md = MessageDigest.getInstance("SHA-256")
            val pxBuf = ByteBuffer.allocate(pixels.size * 4)
            pxBuf.asIntBuffer().put(pixels)
            val hashHex = md.digest(pxBuf.array()).joinToString("") { "%02x".format(it) }
            rgbaHashes[skinKey] = hashHex
            avgLuminances[skinKey] = lumSum / lumCount.coerceAtLeast(1)
        }

        assertEquals(
            "All 4 skin Roborazzi PNGs must have pairwise distinct decoded RGBA hashes, got: $rgbaHashes",
            4,
            rgbaHashes.values.toSet().size
        )
        assertTrue(
            "Night skin dusk_pine luminance (${avgLuminances["dusk_pine"]}) must be darker than ink_mist (${avgLuminances["ink_mist"]})",
            avgLuminances.getValue("dusk_pine") < avgLuminances.getValue("ink_mist")
        )
    }
}
