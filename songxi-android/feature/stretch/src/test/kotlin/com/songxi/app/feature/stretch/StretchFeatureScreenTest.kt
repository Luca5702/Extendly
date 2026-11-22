package com.songxi.app.feature.stretch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.songxi.app.core.data.DataStorePracticeJournalRepository
import com.songxi.app.core.data.DataStoreStretchCatalogRepository
import com.songxi.app.core.data.DataStoreThemePreferencesRepository
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.designsystem.SongxiTheme
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.PracticeSessionRecord
import com.songxi.app.core.model.SkinId
import com.songxi.app.core.model.SoundProfileId
import com.songxi.app.core.sensory.BreathingHapticEngine
import com.songxi.app.core.sensory.FaceDownSensorMonitor
import com.songxi.app.core.sensory.HarmonicAudioEngine
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StretchFeatureScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeAudioEngine : HarmonicAudioEngine {
        var inhaleCount = 0
        var exhaleCount = 0
        var transitionCount = 0
        var lastMuted = false

        override fun playInhaleChord(skinId: SkinId, soundProfile: SoundProfileId?, muted: Boolean) {
            inhaleCount++
            lastMuted = muted
        }

        override fun playExhaleChord(skinId: SkinId, soundProfile: SoundProfileId?, muted: Boolean) {
            exhaleCount++
            lastMuted = muted
        }

        override fun playPoseTransitionChime(skinId: SkinId, soundProfile: SoundProfileId?, muted: Boolean) {
            transitionCount++
            lastMuted = muted
        }

        override fun playSkinPreviewNote(skinId: SkinId, muted: Boolean) = Unit
        override fun release() = Unit
    }

    private class FakeHapticEngine : BreathingHapticEngine {
        var inhalePulses = 0
        var exhalePulses = 0
        override fun pulseInhaleWave(maxAmplitude: Int, enabled: Boolean) {
            if (enabled) inhalePulses++
        }
        override fun pulseExhaleWave(maxAmplitude: Int, enabled: Boolean) {
            if (enabled) exhalePulses++
        }
        override fun cancel() = Unit
    }

    private class FakeFaceDownMonitor : FaceDownSensorMonitor {
        private val _isFaceDown = MutableStateFlow(false)
        override val isFaceDown: StateFlow<Boolean> = _isFaceDown
        var monitoringActive = false

        override fun startMonitoring() {
            monitoringActive = true
        }

        override fun stopMonitoring() {
            monitoringActive = false
            _isFaceDown.value = false
        }

        override fun simulateFaceDownForTest(faceDown: Boolean) {
            _isFaceDown.value = faceDown
        }
    }

    private fun buildViewModel(): Triple<StretchSessionViewModel, FakeAudioEngine, FakeFaceDownMonitor> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dsFile = File(tempFolder.newFolder(), "stretch_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { dsFile })
        val audio = FakeAudioEngine()
        val haptics = FakeHapticEngine()
        val faceDown = FakeFaceDownMonitor()
        val vm = StretchSessionViewModel(
            catalogRepository = DataStoreStretchCatalogRepository(dataStore),
            themePreferencesRepository = DataStoreThemePreferencesRepository(dataStore),
            journalRepository = DataStorePracticeJournalRepository(dataStore),
            audioEngine = audio,
            hapticEngine = haptics,
            faceDownMonitor = faceDown,
            clockProvider = ClockProvider { 1_795_000_000_000L },
            initialOnboardingSeen = true,
            externalScope = scope
        )
        return Triple(vm, audio, faceDown)
    }

    @Test
    fun home_and_player_transition_and_4s_6s_breath_cycle() {
        val (vm, audio, faceDown) = buildViewModel()

        composeRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.InkMist) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel = vm)
                    StretchPlayerOverlayHost(viewModel = vm)
                }
            }
        }
        composeRule.waitForIdle()
        vm.dismissOnboarding()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag("home_start_hero_button").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("player_screen").assertIsDisplayed()
        assertTrue(faceDown.monitoringActive)
        assertEquals(1, audio.inhaleCount)

        vm.advancePlaybackMillis(4_100L, SkinId.INK_MIST)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, audio.exhaleCount)

        faceDown.simulateFaceDownForTest(true)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("player_paused_by_flip_banner").assertIsDisplayed()

        composeRule.onNodeWithTag("player_close_button").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertFalse(faceDown.monitoringActive)
    }

    @Test
    fun bodymap_composer_and_completion_persist_journal_idempotently() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dsFile = File(tempFolder.newFolder(), "journal_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { dsFile })
        val recorded = mutableListOf<PracticeSessionRecord>()
        val baseJournal = DataStorePracticeJournalRepository(dataStore)
        val spyJournal = object : com.songxi.app.core.data.PracticeJournalRepository by baseJournal {
            override suspend fun recordCompletedSession(record: PracticeSessionRecord) {
                recorded.add(record)
                baseJournal.recordCompletedSession(record)
            }
        }
        val vm = StretchSessionViewModel(
            catalogRepository = DataStoreStretchCatalogRepository(dataStore),
            themePreferencesRepository = DataStoreThemePreferencesRepository(dataStore),
            journalRepository = spyJournal,
            audioEngine = FakeAudioEngine(),
            hapticEngine = FakeHapticEngine(),
            faceDownMonitor = FakeFaceDownMonitor(),
            clockProvider = ClockProvider { 1_795_000_000_000L },
            initialOnboardingSeen = true,
            externalScope = scope
        )

        composeRule.setContent {
            SongxiTheme(skin = BuiltInSkinRegistry.QinglvCeladon) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BodyMapScreen(viewModel = vm)
                    StretchPlayerOverlayHost(viewModel = vm)
                }
            }
        }

        composeRule.onNodeWithTag("bodymap_compose_start_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_screen").assertIsDisplayed()

        repeat(10) {
            if (vm.uiState.value.sessionStage !is StretchSessionStage.Completed) {
                vm.nextPose(SkinId.QINGLV_CELADON)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("completion_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("completion_feeling_calm").performClick()
        vm.finishCompletionAndSave(SkinId.QINGLV_CELADON)
        vm.finishCompletionAndSave(SkinId.QINGLV_CELADON) // Double-tap idempotency
        composeRule.waitForIdle()

        assertEquals(1, recorded.size)
        assertEquals("calm", recorded.first().feeling?.key)
    }
}
