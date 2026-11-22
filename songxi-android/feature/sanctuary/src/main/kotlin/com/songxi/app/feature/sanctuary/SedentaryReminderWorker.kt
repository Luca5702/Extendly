package com.songxi.app.feature.sanctuary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.songxi.app.core.data.ReminderPreferencesRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.ReminderPreferences
import com.songxi.app.core.model.SystemClockProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

interface SongxiReminderWorkerDepsProvider {
    val reminderPreferencesRepository: ReminderPreferencesRepository
    val clockProvider: ClockProvider
}

object SedentaryNotificationHelper {
    const val CHANNEL_ID: String = "songxi_breeze_reminder_channel"
    const val NOTIFICATION_ID: Int = 2026
    const val EXTRA_FROM_REMINDER_QUICK_START: String = "com.songxi.app.extra.FROM_REMINDER_QUICK_START"
    private const val MAIN_ACTIVITY_CLASS: String = "com.songxi.app.MainActivity"

    fun postGentleReminderNotification(context: Context) {
        val appCtx = context.applicationContext
        val notificationManager =
            appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "松息 · 微风提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "低打扰久坐舒展提醒，尊重午休、夜间与周末静默"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val quickStartIntent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(appCtx, MAIN_ACTIVITY_CLASS)
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra(EXTRA_FROM_REMINDER_QUICK_START, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val quickStartPendingIntent = PendingIntent.getActivity(
            appCtx,
            9001,
            quickStartIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("松息 · 微风轻语")
            .setContentText("久坐良久，不妨闭目九十秒，随双吸长叹把双肩轻轻放下。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(quickStartPendingIntent)
            .addAction(0, "开始 90 秒叹息帖", quickStartPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

object SedentaryReminderScheduler {
    const val UNIQUE_WORK_NAME: String = "songxi_sedentary_breeze_periodic"

    fun syncSchedule(context: Context, preferences: ReminderPreferences) {
        val workManager = runCatching { WorkManager.getInstance(context.applicationContext) }
            .getOrNull() ?: return

        if (!preferences.enabled) {
            val cancelOp = workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            runCatching { cancelOp.result.get(2, TimeUnit.SECONDS) }
            return
        }

        val safeIntervalMinutes = preferences.intervalMinutes
            .takeIf { it in ReminderPreferences.SUPPORTED_INTERVALS }
            ?: 60

        val periodicRequest = PeriodicWorkRequestBuilder<SedentaryReminderWorker>(
            safeIntervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .addTag(UNIQUE_WORK_NAME)
            .build()

        val enqueueOp = workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
        runCatching { enqueueOp.result.get(2, TimeUnit.SECONDS) }
    }
}

class SedentaryReminderWorker @JvmOverloads constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val injectedRepository: ReminderPreferencesRepository? = null,
    private val injectedClockProvider: ClockProvider? = null
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val provider = applicationContext as? SongxiReminderWorkerDepsProvider
        val repository = injectedRepository ?: provider?.reminderPreferencesRepository ?: return Result.success()
        val clock = injectedClockProvider ?: provider?.clockProvider ?: SystemClockProvider

        val prefs = repository.preferences.first()
        if (!prefs.enabled) {
            return Result.success()
        }

        val localNow = clock.currentLocalDateTime()
        if (prefs.isMutedOrQuietAt(localNow)) {
            return Result.success()
        }

        val todayIso = localNow.toLocalDate().toString()
        val sentCount = repository.notificationsSentOn(todayIso).first()
        if (sentCount >= prefs.dailyMaxCount) {
            return Result.success()
        }

        SedentaryNotificationHelper.postGentleReminderNotification(applicationContext)
        repository.recordNotificationSent(todayIso)
        return Result.success()
    }
}

class SongxiReminderWorkerFactory(
    private val repositoryProvider: () -> ReminderPreferencesRepository,
    private val clockProvider: () -> ClockProvider
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == SedentaryReminderWorker::class.java.name) {
            SedentaryReminderWorker(
                appContext = appContext,
                workerParams = workerParameters,
                injectedRepository = repositoryProvider(),
                injectedClockProvider = clockProvider()
            )
        } else {
            null
        }
    }
}
