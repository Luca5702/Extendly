package com.songxi.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.designsystem.BuiltInSkinRegistry
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiNavGlyph
import com.songxi.app.core.designsystem.SongxiNavIconCanvas
import com.songxi.app.core.designsystem.SongxiTheme
import com.songxi.app.core.designsystem.songxiGentleStageReveal
import com.songxi.app.core.designsystem.songxiTactilePress
import com.songxi.app.feature.sanctuary.PracticeJournalScreen
import com.songxi.app.feature.sanctuary.ReminderSettingsScreen
import com.songxi.app.feature.sanctuary.SanctuaryViewModel
import com.songxi.app.feature.sanctuary.SedentaryReminderScheduler
import com.songxi.app.feature.sanctuary.SkinGalleryScreen
import com.songxi.app.feature.splashad.SplashAdScreen
import com.songxi.app.feature.splashad.SplashGateDecision
import com.songxi.app.feature.stretch.BodyMapScreen
import com.songxi.app.feature.stretch.HomeScreen
import com.songxi.app.feature.stretch.StretchPlayerOverlayHost
import com.songxi.app.feature.stretch.StretchSessionStage
import com.songxi.app.feature.stretch.StretchSessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class SongxiNavTab(
    val tag: String,
    val labelCn: String,
    val glyph: SongxiNavGlyph
) {
    HOME("nav_tab_home", "一息", SongxiNavGlyph.HOME_ENSO),
    BODYMAP("nav_tab_bodymap", "寻音", SongxiNavGlyph.BODYMAP_MERIDIAN),
    SKINS("nav_tab_skins", "画境", SongxiNavGlyph.SKINS_SCROLL),
    JOURNAL("nav_tab_journal", "手札", SongxiNavGlyph.JOURNAL_RIPPLE),
    SETTINGS("nav_tab_settings", "微风", SongxiNavGlyph.SETTINGS_CHIME)
}

class MainActivity : ComponentActivity() {

    private val container: SongxiAppContainer by lazy {
        (application as SongxiApplication).appContainer
    }

    private val stretchViewModel: StretchSessionViewModel by viewModels {
        StretchSessionViewModel.provideFactory(
            catalogRepository = container.stretchCatalogRepository,
            themePreferencesRepository = container.themePreferencesRepository,
            journalRepository = container.practiceJournalRepository,
            audioEngine = container.audioEngine,
            hapticEngine = container.hapticEngine,
            faceDownMonitor = container.faceDownSensorMonitor,
            clockProvider = container.clockProvider,
            initialOnboardingSeen = runBlocking {
                container.themePreferencesRepository.onboardingSeen.first()
            },
            voiceEngine = container.voiceEngine
        )
    }

    private val sanctuaryViewModel: SanctuaryViewModel by viewModels {
        SanctuaryViewModel.provideFactory(
            appContext = applicationContext,
            themePreferencesRepository = container.themePreferencesRepository,
            journalRepository = container.practiceJournalRepository,
            reminderPreferencesRepository = container.reminderPreferencesRepository,
            splashAdLedgerRepository = container.splashAdLedgerRepository,
            audioEngine = container.audioEngine,
            hapticEngine = container.hapticEngine,
            catalogRepository = container.stretchCatalogRepository,
            clockProvider = container.clockProvider
        )
    }

    private var activeTab by mutableStateOf(SongxiNavTab.HOME)
    private var activeSplashShow by mutableStateOf<SplashGateDecision.Show?>(null)
    private var stoppedAtEpochMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        syncSimulatedClockFromIntent(intent)

        if (savedInstanceState == null && intent?.getBooleanExtra(SongxiIntents.EXTRA_RESET_STATE, false) == true) {
            container.resetAllStateSynchronously()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            val savedTabName = savedInstanceState.getString(KEY_ACTIVE_TAB)
            activeTab = SongxiNavTab.entries.firstOrNull { it.name == savedTabName } ?: SongxiNavTab.HOME
        } else {
            if (intent?.getBooleanExtra(SongxiIntents.EXTRA_RESET_STATE, false) == true) {
                stretchViewModel.resetSessionState(onboardingSeen = false)
                sanctuaryViewModel.resetTransientDialogs()
            } else {
                sanctuaryViewModel.refreshCurrentClockMonth()
            }
            val initialReminderPrefs = runBlocking {
                container.reminderPreferencesRepository.preferences.first()
            }
            SedentaryReminderScheduler.syncSchedule(applicationContext, initialReminderPrefs)
            handleLaunchIntent(intent, isColdCreate = true)
        }

        setContent {
            val sanctuaryState by sanctuaryViewModel.uiState.collectAsStateWithLifecycle()
            val stretchState by stretchViewModel.uiState.collectAsStateWithLifecycle()

            val effectiveSkin = BuiltInSkinRegistry.resolveEffectiveSkin(
                selected = sanctuaryState.selectedSkinId,
                autoNightDuskPine = sanctuaryState.autoNightSkinEnabled,
                localDateTime = container.clockProvider.currentLocalDateTime()
            )

            SongxiTheme(skin = effectiveSkin) {
                val skin = LocalSongxiSkin.current
                val splashShow = activeSplashShow
                val isOverlayPlayerOrCompletion = stretchState.sessionStage !is StretchSessionStage.Idle

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(skin.palette.canvasPrimary)
                ) {
                    if (splashShow != null) {
                        SplashAdScreen(
                            creative = splashShow.creative,
                            countdownSeconds = splashShow.countdownSeconds,
                            onSkipOrFinish = { activeSplashShow = null },
                            onUserSkipped = { elapsedMs ->
                                runBlocking {
                                    container.splashAdProvider.recordSkip(splashShow.creative.id, elapsedMs)
                                }
                            },
                            onCtaClicked = {
                                runBlocking {
                                    container.splashAdProvider.recordCtaClick(splashShow.creative.id)
                                }
                            },
                            onExternalUrlConfirmed = { url ->
                                activeSplashShow = null
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    } else if (isOverlayPlayerOrCompletion) {
                        StretchPlayerOverlayHost(
                            viewModel = stretchViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(skin.palette.canvasPrimary)
                                .statusBarsPadding()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .songxiGentleStageReveal(activeTab)
                            ) {
                                when (activeTab) {
                                    SongxiNavTab.HOME -> HomeScreen(
                                        viewModel = stretchViewModel,
                                        onNavigateToSkinGallery = { activeTab = SongxiNavTab.SKINS }
                                    )
                                    SongxiNavTab.BODYMAP -> BodyMapScreen(
                                        viewModel = stretchViewModel
                                    )
                                    SongxiNavTab.SKINS -> SkinGalleryScreen(
                                        viewModel = sanctuaryViewModel
                                    )
                                    SongxiNavTab.JOURNAL -> PracticeJournalScreen(
                                        viewModel = sanctuaryViewModel
                                    )
                                    SongxiNavTab.SETTINGS -> ReminderSettingsScreen(
                                        viewModel = sanctuaryViewModel,
                                        onRequestSplashPreview = { triggerManualSplashPreview() },
                                        onQuickStartFromInAppBanner = {
                                            stretchViewModel.startReminderQuickStart(effectiveSkin.id)
                                        }
                                    )
                                }
                            }

                            SongxiBottomNavigationBar(
                                currentTab = activeTab,
                                onSelectTab = { tab -> activeTab = tab }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        syncSimulatedClockFromIntent(intent)
        sanctuaryViewModel.refreshCurrentClockMonth()
        handleLaunchIntent(intent, isColdCreate = false)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            stoppedAtEpochMs = container.clockProvider.nowEpochMillis()
        }
    }

    override fun onStart() {
        super.onStart()
        val previousStopMs = stoppedAtEpochMs
        if (previousStopMs != null) {
            stoppedAtEpochMs = null
            syncSimulatedClockFromIntent(intent)
            sanctuaryViewModel.refreshCurrentClockMonth()
            val nowMs = container.clockProvider.nowEpochMillis()
            val warmElapsedMs = (nowMs - previousStopMs).coerceAtLeast(0L)
            val decision = runBlocking {
                container.splashAdPolicyEngine.evaluateLaunch(
                    isReminderDeepLink = false,
                    hasActiveOrPausedStretch = stretchViewModel.hasActiveOrPausedSession,
                    warmBackgroundDurationMs = warmElapsedMs,
                    forcePreview = false
                )
            }
            if (decision is SplashGateDecision.Show) {
                activeSplashShow = decision
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_ACTIVE_TAB, activeTab.name)
        super.onSaveInstanceState(outState)
    }

    private fun syncSimulatedClockFromIntent(targetIntent: Intent?) {
        if (targetIntent != null && targetIntent.hasExtra(SongxiIntents.EXTRA_SIMULATED_EPOCH_MS)) {
            val simEpoch = targetIntent.getLongExtra(SongxiIntents.EXTRA_SIMULATED_EPOCH_MS, -1L)
            if (simEpoch > 0L) {
                container.clockProvider.applySimulatedEpoch(simEpoch)
            }
        }
    }

    private fun handleLaunchIntent(targetIntent: Intent?, isColdCreate: Boolean) {
        val isReminderQuickStart =
            targetIntent?.getBooleanExtra(SongxiIntents.EXTRA_FROM_REMINDER_QUICK_START, false) == true
        val isForcePreview =
            targetIntent?.getBooleanExtra(SongxiIntents.EXTRA_FORCE_SPLASH_PREVIEW, false) == true

        val decision = runBlocking {
            val nowMs = container.clockProvider.nowEpochMillis()
            val launchesSoFar = container.splashAdLedgerRepository.totalAppLaunches.first()
            if (isColdCreate && !isForcePreview && launchesSoFar == 0 && container.firstLaunchEpochMs == null) {
                container.firstLaunchEpochMs = nowMs
            }
            val firstLaunchMs = container.firstLaunchEpochMs
            val effectiveWarmDurationMs: Long? = when {
                !isColdCreate -> 0L
                !isForcePreview && launchesSoFar >= 1 && firstLaunchMs != null &&
                    (nowMs - firstLaunchMs) in 0L until com.songxi.app.feature.splashad.SplashAdPolicyEngine.WARM_RESUME_SUPPRESSION_THRESHOLD_MS ->
                    nowMs - firstLaunchMs
                else -> null
            }

            container.splashAdPolicyEngine.evaluateLaunch(
                isReminderDeepLink = isReminderQuickStart,
                hasActiveOrPausedStretch = stretchViewModel.hasActiveOrPausedSession,
                warmBackgroundDurationMs = effectiveWarmDurationMs,
                forcePreview = isForcePreview
            )
        }

        if (isReminderQuickStart && !isForcePreview) {
            activeSplashShow = null
            val effectiveSkin = resolveCurrentEffectiveSkin()
            stretchViewModel.startReminderQuickStart(effectiveSkin.id)
            return
        }

        when (decision) {
            is SplashGateDecision.Show -> activeSplashShow = decision
            is SplashGateDecision.Bypass -> activeSplashShow = null
        }
    }

    private fun triggerManualSplashPreview() {
        val decision = runBlocking {
            container.splashAdPolicyEngine.evaluateLaunch(
                isReminderDeepLink = false,
                hasActiveOrPausedStretch = stretchViewModel.hasActiveOrPausedSession,
                warmBackgroundDurationMs = 0L,
                forcePreview = true
            )
        }
        if (decision is SplashGateDecision.Show) {
            activeSplashShow = decision
        }
    }

    private fun resolveCurrentEffectiveSkin() = runBlocking {
        val selected = container.themePreferencesRepository.selectedSkinId.first()
        val autoNight = container.themePreferencesRepository.autoNightSkinEnabled.first()
        BuiltInSkinRegistry.resolveEffectiveSkin(
            selected = selected,
            autoNightDuskPine = autoNight,
            localDateTime = container.clockProvider.currentLocalDateTime()
        )
    }

    private companion object {
        const val KEY_ACTIVE_TAB: String = "songxi_active_tab"
    }
}

@Composable
private fun SongxiBottomNavigationBar(
    currentTab: SongxiNavTab,
    onSelectTab: (SongxiNavTab) -> Unit
) {
    val skin = LocalSongxiSkin.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(skin.palette.surfaceCard)
            .navigationBarsPadding()
            .testTag("bottom_nav_bar")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(skin.palette.borderHairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongxiNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .songxiTactilePress(interactionSource = interactionSource, pressedScale = 0.94f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelectTab(tab) }
                        )
                        .semantics { selected = isSelected }
                        .testTag(tab.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SongxiNavIconCanvas(
                        glyph = tab.glyph,
                        selected = isSelected,
                        skin = skin,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.labelCn,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) skin.palette.accentPrimary else skin.palette.inkSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
