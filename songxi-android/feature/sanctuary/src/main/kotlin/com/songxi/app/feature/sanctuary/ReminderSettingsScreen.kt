package com.songxi.app.feature.sanctuary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songxi.app.core.designsystem.ClassicalSealBadge
import com.songxi.app.core.designsystem.LocalSongxiSkin
import com.songxi.app.core.designsystem.SongxiTabularNumericStyle
import com.songxi.app.core.designsystem.songxiGentleStageReveal
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
        voiceGuidanceEnabled = uiState.voiceGuidanceEnabled,
        prepBufferEnabled = uiState.prepBufferEnabled,
        onVoiceGuidanceChanged = viewModel::setVoiceGuidanceEnabled,
        onPrepBufferChanged = viewModel::setPrepBufferEnabled,
        showBackupDialog = uiState.showBackupDialog,
        latestBackupJson = uiState.latestBackupJson,
        backupStatusMessage = uiState.backupStatusMessage,
        onExportBackup = viewModel::exportSanctuaryBackup,
        onOpenRestoreBackup = viewModel::openRestoreBackupDialog,
        onDismissBackupDialog = viewModel::dismissBackupDialog,
        onRestoreBackupJson = viewModel::restoreSanctuaryFromJson,
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
    voiceGuidanceEnabled: Boolean = false,
    prepBufferEnabled: Boolean = false,
    onVoiceGuidanceChanged: (Boolean) -> Unit = {},
    onPrepBufferChanged: (Boolean) -> Unit = {},
    showBackupDialog: Boolean = false,
    latestBackupJson: String = "",
    backupStatusMessage: String? = null,
    onExportBackup: () -> Unit = {},
    onOpenRestoreBackup: () -> Unit = {},
    onDismissBackupDialog: () -> Unit = {},
    onRestoreBackupJson: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skin = LocalSongxiSkin.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val cardRadiusDp = skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp
    val cardShape = RoundedCornerShape(cardRadiusDp)
    val subCardShape = RoundedCornerShape((skin.palette.cardCornerRadiusDp - 6).coerceIn(12, 24).dp)

    val requiresRuntimeNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotificationPerm by remember {
        mutableStateOf(
            if (requiresRuntimeNotifPerm) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val hasActivityResultOwner = LocalActivityResultRegistryOwner.current != null
    val permissionLauncher = if (requiresRuntimeNotifPerm && hasActivityResultOwner) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasNotificationPerm = granted
        }
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.palette.canvasPrimary)
    ) {
        Column(
            modifier = Modifier
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
                onCheckedChange = { nextEnabled ->
                    onEnableChanged(nextEnabled)
                    if (nextEnabled && requiresRuntimeNotifPerm && !hasNotificationPerm) {
                        runCatching { permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    }
                },
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
                                style = SongxiTabularNumericStyle,
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

            if (preferences.enabled && requiresRuntimeNotifPerm && !hasNotificationPerm) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(subCardShape)
                        .background(skin.palette.surfaceCardSubtle, subCardShape)
                        .border(1.dp, skin.palette.borderHairline, subCardShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("reminder_permission_guide_card"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "系统通知权限尚未开启，开启后微风方能按时轻唤",
                        fontSize = 11.sp,
                        color = skin.palette.inkSecondary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    SanctuarySurfaceButton(
                        onClick = {
                            runCatching { permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        },
                        shape = CircleShape,
                        backgroundColor = skin.palette.accentPrimary,
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp),
                        testTag = "reminder_request_permission_button"
                    ) {
                        Text(
                            text = "授权通知",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.onAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Blind-Operation & Settle-In Coaching Preferences
            Text(
                text = "跟练感官微调",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.palette.inkPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            SanctuarySwitchRow(
                title = "轻声语音口令导引（离线）",
                subtitle = "闭眼跟练时，在换式、半程换边与缓慢回正节点以 0.88x 慢语速轻声播报",
                checked = voiceGuidanceEnabled,
                onCheckedChange = onVoiceGuidanceChanged,
                testTag = "settings_voice_guide_switch"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SanctuarySwitchRow(
                title = "入席 3 秒安顿缓冲",
                subtitle = "点按开始后预留 3 秒放稳手机、沉落双肩，再正式开启第一息计时",
                checked = prepBufferEnabled,
                onCheckedChange = onPrepBufferChanged,
                testTag = "settings_prep_buffer_switch"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Offline Sanctuary Backup & Restore Card ("本地手札归档")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(skin.palette.surfaceCard, cardShape)
                    .border(1.dp, skin.palette.borderHairline, cardShape)
                    .padding(14.dp)
                    .testTag("settings_backup_card")
            ) {
                Text(
                    text = "本地手札归档与恢复",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.palette.inkPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "全量练习水纹、常备处方与画境偏好均纯离线存储，可随时导出 JSON 归档迁移设备。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = skin.palette.inkSecondary
                )

                if (!backupStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = backupStatusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = skin.palette.accentPrimary,
                        modifier = Modifier.testTag("settings_backup_status_text")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SanctuarySurfaceButton(
                        onClick = onExportBackup,
                        shape = CircleShape,
                        backgroundColor = skin.palette.accentPrimarySoft,
                        borderColor = skin.palette.accentPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp),
                        testTag = "settings_export_backup_button"
                    ) {
                        Text(
                            text = "导出本地归档",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skin.palette.accentPrimary
                        )
                    }

                    SanctuarySurfaceButton(
                        onClick = onOpenRestoreBackup,
                        shape = CircleShape,
                        backgroundColor = skin.palette.canvasPrimary,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp),
                        testTag = "settings_import_backup_button"
                    ) {
                        Text(
                            text = "恢复本地归档",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = skin.palette.inkPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (showBackupDialog) {
            BackupArchiveOverlay(
                initialJson = latestBackupJson,
                onDismiss = onDismissBackupDialog,
                onShareJson = { raw ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "松息本地手札归档")
                        putExtra(Intent.EXTRA_TEXT, raw)
                    }
                    val chooser = Intent.createChooser(send, "导出松息手札归档").apply {
                        if (context !is android.app.Activity) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(chooser)
                },
                onConfirmRestore = onRestoreBackupJson
            )
        }
    }
}

@Composable
private fun BackupArchiveOverlay(
    initialJson: String,
    onDismiss: () -> Unit,
    onShareJson: (String) -> Unit,
    onConfirmRestore: (String) -> Unit
) {
    val skin = LocalSongxiSkin.current
    val cardShape = RoundedCornerShape(skin.palette.cardCornerRadiusDp.coerceIn(14, 30).dp)
    var editableJson by remember(initialJson) { mutableStateOf(initialJson) }
    val dialogScroll = rememberScrollState()

    BackHandler(onBack = onDismiss)

    val backdropInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skin.palette.surfaceOverlay)
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardInteraction = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .songxiGentleStageReveal("backup_sheet")
                .clip(cardShape)
                .background(skin.palette.surfaceCard, cardShape)
                .border(1.dp, skin.palette.borderHairline, cardShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                )
                .verticalScroll(dialogScroll)
                .padding(18.dp)
                .testTag("settings_backup_dialog")
        ) {
            Text(
                text = "松息本地手札归档 (JSON)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.palette.inkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "可分享保存以下归档文本，或粘贴已有归档内容点击「确认恢复」。",
                fontSize = 12.sp,
                color = skin.palette.inkSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = editableJson,
                onValueChange = { editableJson = it },
                maxLines = 7,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("settings_backup_json_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SanctuarySurfaceButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    backgroundColor = skin.palette.surfaceCardSubtle,
                    borderColor = skin.palette.borderHairline,
                    modifier = Modifier
                        .weight(0.8f)
                        .defaultMinSize(minHeight = 44.dp),
                    testTag = "settings_backup_close_button"
                ) {
                    Text(
                        text = "关闭",
                        fontSize = 12.sp,
                        color = skin.palette.inkPrimary
                    )
                }

                if (editableJson.isNotBlank()) {
                    SanctuarySurfaceButton(
                        onClick = { onShareJson(editableJson) },
                        shape = CircleShape,
                        backgroundColor = skin.palette.canvasPrimary,
                        borderColor = skin.palette.borderHairline,
                        modifier = Modifier
                            .weight(0.95f)
                            .defaultMinSize(minHeight = 44.dp),
                        testTag = "settings_backup_share_button"
                    ) {
                        Text(
                            text = "分享文件",
                            fontSize = 12.sp,
                            color = skin.palette.accentPrimary
                        )
                    }
                }

                SanctuarySurfaceButton(
                    onClick = { onConfirmRestore(editableJson) },
                    shape = CircleShape,
                    backgroundColor = skin.palette.accentPrimary,
                    modifier = Modifier
                        .weight(1.1f)
                        .defaultMinSize(minHeight = 44.dp),
                    testTag = "settings_backup_confirm_restore_button"
                ) {
                    Text(
                        text = "确认恢复",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.palette.onAccent
                    )
                }
            }
        }
    }
}
