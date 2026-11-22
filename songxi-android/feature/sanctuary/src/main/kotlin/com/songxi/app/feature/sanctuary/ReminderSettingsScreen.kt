package com.songxi.app.feature.sanctuary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.model.ReminderPreferences

@Composable
fun ReminderSettingsScreen(
    viewModel: SanctuaryViewModel,
    onRequestSplashPreview: () -> Unit,
    onQuickStartFromInAppBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayIso = viewModel.clockProvider.currentDateIso()

    ReminderSettingsScreen(
        preferences = uiState.reminderPreferences,
        isMutedToday = uiState.reminderPreferences.muteTodayDateIso == todayIso,
        personalizedSplashEnabled = uiState.personalizedSplashEnabled,
        showReminderInAppBanner = uiState.showReminderInAppBanner,
        onEnableChanged = viewModel::setReminderEnabled,
        onSelectInterval = viewModel::selectReminderInterval,
        onLunchQuietChanged = viewModel::setLunchQuietEnabled,
        onNightQuietChanged = viewModel::setNightQuietEnabled,
        onWeekendQuietChanged = viewModel::setWeekendSilentEnabled,
        onMuteTodayChanged = viewModel::setMuteTodayEnabled,
        onTriggerReminderPreview = viewModel::triggerReminderPreview,
        onQuickStartBannerClick = {
            viewModel.dismissReminderInAppBanner()
            onQuickStartFromInAppBanner()
        },
        onPersonalizedSplashChanged = viewModel::setPersonalizedSplashAds,
        onTriggerSplashPreview = onRequestSplashPreview,
        modifier = modifier
    )
}

@Suppress("ModifierParameter")
@Composable
fun ReminderSettingsScreen(
    preferences: ReminderPreferences,
    isMutedToday: Boolean,
    personalizedSplashEnabled: Boolean,
    showReminderInAppBanner: Boolean,
    onEnableChanged: (Boolean) -> Unit,
    onSelectInterval: (Int) -> Unit,
    onLunchQuietChanged: (Boolean) -> Unit,
    onNightQuietChanged: (Boolean) -> Unit,
    onWeekendQuietChanged: (Boolean) -> Unit,
    onMuteTodayChanged: (Boolean) -> Unit,
    onTriggerReminderPreview: () -> Unit,
    onQuickStartBannerClick: () -> Unit,
    onPersonalizedSplashChanged: (Boolean) -> Unit,
    onTriggerSplashPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("settings_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "低打扰伴随 · 克制守界",
                    fontSize = 12.sp,
                    color = skin.palette.inkSecondary
                )
                Text(
                    text = "微风设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = skin.meta.titleLetterSpacingSp.sp,
                    color = skin.palette.inkPrimary
                )
            }
            ClassicalSealBadge(skin = skin, size = 32.dp)
        }

        if (showReminderInAppBanner) {
            Spacer(modifier = Modifier.height(8.dp))
            SanctuarySurfaceButton(
                onClick = onQuickStartBannerClick,
                shape = cardShape,
                backgroundColor = skin.palette.accentPrimarySoft,
                borderColor = skin.palette.accentPrimary,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                testTag = "reminder_inapp_banner"
            ) {
                Text(
                    text = "微风提醒已送达 · 点此免开屏直达 90 秒叹息帖",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = skin.palette.accentPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Splash privacy & manual preview placed near top so manual preview works immediately at t=0ms
        SanctuarySwitchRow(
            title = "个性化开屏器物推荐",
            subtitle = "默认关闭；仅展示东方茶席、线香与木作画心，禁用摇一摇跳转",
            checked = personalizedSplashEnabled,
            onCheckedChange = onPersonalizedSplashChanged,
            testTag = "splash_personalized_switch"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SanctuarySurfaceButton(
            onClick = onTriggerSplashPreview,
            shape = CircleShape,
            backgroundColor = skin.palette.surfaceCard,
            borderColor = skin.palette.borderHairline,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
            testTag = "splash_preview_trigger_button"
        ) {
            Text(
                text = "预览开屏雅鉴（3 秒画心展示）",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = skin.palette.inkPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SanctuarySwitchRow(
            title = "开启微风久坐提醒",
            subtitle = "单日最多轻唤 ${preferences.dailyMaxCount} 次，绝不连环催促",
            checked = preferences.enabled,
            onCheckedChange = onEnableChanged,
            testTag = "reminder_enable_switch"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Interval selector card (45 / 60 / 90 / 120m)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(skin.palette.surfaceCard, cardShape)
                .border(1.dp, skin.palette.borderHairline, cardShape)
                .padding(12.dp)
        ) {
            Text(
                text = "久坐间隔周期",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skin.palette.inkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderPreferences.SUPPORTED_INTERVALS.forEach { mins ->
                    val isSelected = preferences.intervalMinutes == mins
                    SanctuarySurfaceButton(
                        onClick = { onSelectInterval(mins) },
                        shape = CircleShape,
                        backgroundColor = if (isSelected) skin.palette.accentPrimary else skin.palette.canvasPrimary,
                        borderColor = if (isSelected) skin.palette.accentPrimary else skin.palette.borderHairline,
                        selectedState = isSelected,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp),
                        testTag = "reminder_interval_$mins"
                    ) {
                        Text(
                            text = "${mins}分",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) skin.palette.onAccent else skin.palette.inkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SanctuarySwitchRow(
            title = "午休静默（12:00–13:30）",
            subtitle = "午间小憩时段自动隐去提醒",
            checked = preferences.lunchQuietEnabled,
            onCheckedChange = onLunchQuietChanged,
            testTag = "reminder_lunch_quiet_switch"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SanctuarySwitchRow(
            title = "夜间静默（20:00–09:00）",
            subtitle = "下班与睡眠时段零打扰",
            checked = preferences.nightQuietEnabled,
            onCheckedChange = onNightQuietChanged,
            testTag = "reminder_night_quiet_switch"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SanctuarySwitchRow(
            title = "周末自动静默",
            subtitle = "周六、周日全天留白不发提醒",
            checked = preferences.weekendSilent,
            onCheckedChange = onWeekendQuietChanged,
            testTag = "reminder_weekend_quiet_switch"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SanctuarySwitchRow(
            title = "今日勿扰",
            subtitle = "仅对今日生效，次日清晨自动恢复微风守护",
            checked = isMutedToday,
            onCheckedChange = onMuteTodayChanged,
            testTag = "reminder_mute_today_switch"
        )

        Spacer(modifier = Modifier.height(10.dp))

        SanctuarySurfaceButton(
            onClick = onTriggerReminderPreview,
            shape = CircleShape,
            backgroundColor = skin.palette.surfaceCard,
            borderColor = skin.palette.borderHairline,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp),
            testTag = "reminder_preview_trigger_button"
        ) {
            Text(
                text = "模拟微风提醒 · 测试通知直达",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = skin.palette.accentPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}
