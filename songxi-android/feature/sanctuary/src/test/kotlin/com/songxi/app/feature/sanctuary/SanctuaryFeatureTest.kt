package com.songxi.app.feature.sanctuary

import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.songxi.app.core.data.DataStoreReminderPreferencesRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.core.model.ReminderPreferences
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SanctuaryFeatureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @After
    fun tearDown() {
        runCatching { WorkManagerTestInitHelper.closeWorkDatabase() }
    }

    @Test
    fun scheduler_enqueues_periodic_work_and_cancels_cleanly_when_disabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val dsFile = File(tempFolder.newFolder(), "rem.preferences_pb")
        val repo = DataStoreReminderPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { dsFile })
        )
        var epoch = LocalDateTime.of(2026, 11, 18, 10, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val clock = ClockProvider { epoch }

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .setWorkerFactory(SongxiReminderWorkerFactory({ repo }, { clock }))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Shadows.shadowOf(nm).setNotificationsEnabled(true)

        SedentaryReminderScheduler.syncSchedule(context, ReminderPreferences(enabled = true, intervalMinutes = 90))
        val query = WorkQuery.fromStates(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)
        val scheduled = WorkManager.getInstance(context).getWorkInfos(query).get(2, TimeUnit.SECONDS)
        assertTrue(scheduled.any { it.periodicityInfo?.repeatIntervalMillis == 90L * 60_000L })

        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        scheduled.forEach {
            driver.setAllConstraintsMet(it.id)
            driver.setPeriodDelayMet(it.id)
        }
        Thread.sleep(50L)
        assertEquals(1, Shadows.shadowOf(nm).allNotifications.size)

        // Lunch quiet hour 12:30 -> suppressed
        nm.cancelAll()
        epoch = LocalDateTime.of(2026, 11, 18, 12, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        scheduled.forEach { driver.setPeriodDelayMet(it.id) }
        Thread.sleep(50L)
        assertEquals(0, Shadows.shadowOf(nm).allNotifications.size)

        // Disabling cancels periodic work
        SedentaryReminderScheduler.syncSchedule(context, ReminderPreferences(enabled = false))
        val afterDisable = WorkManager.getInstance(context).getWorkInfos(query).get(2, TimeUnit.SECONDS)
        assertTrue(afterDisable.isEmpty())
    }
}
