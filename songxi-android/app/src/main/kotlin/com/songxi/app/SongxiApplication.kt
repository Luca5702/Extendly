package com.songxi.app

import android.app.Application
import androidx.work.Configuration
import com.songxi.app.core.data.ReminderPreferencesRepository
import com.songxi.app.core.model.ClockProvider
import com.songxi.app.feature.sanctuary.SongxiReminderWorkerDepsProvider
import com.songxi.app.feature.sanctuary.SongxiReminderWorkerFactory

class SongxiApplication : Application(), Configuration.Provider, SongxiReminderWorkerDepsProvider {

    lateinit var appContainer: SongxiAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = SongxiAppContainer(this)
    }

    override val reminderPreferencesRepository: ReminderPreferencesRepository
        get() = appContainer.reminderPreferencesRepository

    override val clockProvider: ClockProvider
        get() = appContainer.clockProvider

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                SongxiReminderWorkerFactory(
                    repositoryProvider = { appContainer.reminderPreferencesRepository },
                    clockProvider = { appContainer.clockProvider }
                )
            )
            .build()
}
