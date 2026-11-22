package com.songxi.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivitySmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityLaunchesAndExposesExactIntentConstants() {
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(
            "com.songxi.app.extra.FROM_REMINDER_QUICK_START",
            SongxiIntents.EXTRA_FROM_REMINDER_QUICK_START
        )
        assertEquals(
            "com.songxi.app.extra.FORCE_SPLASH_PREVIEW",
            SongxiIntents.EXTRA_FORCE_SPLASH_PREVIEW
        )
        assertEquals(
            "com.songxi.app.extra.SIMULATED_EPOCH_MS",
            SongxiIntents.EXTRA_SIMULATED_EPOCH_MS
        )
        assertEquals(
            "com.songxi.app.extra.RESET_STATE",
            SongxiIntents.EXTRA_RESET_STATE
        )
    }
}
