package com.songxi.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

internal val SongxiDataJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object SongxiDataStoreFactory {
    const val DEFAULT_STORE_NAME: String = "songxi_sanctuary"

    private val activeStores = ConcurrentHashMap<String, DataStore<Preferences>>()

    /**
     * Production and Robolectric safe singleton-per-canonical-path factory.
     * Prevents duplicate active DataStore instances for the same file across container recreations.
     */
    fun getOrCreate(
        context: Context,
        name: String = DEFAULT_STORE_NAME,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ): DataStore<Preferences> {
        val file = context.applicationContext.preferencesDataStoreFile(name)
        val canonical = file.canonicalPath
        return activeStores.computeIfAbsent(canonical) {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = { file }
            )
        }
    }

    /**
     * Isolated factory for JUnit + TestScope unit tests.
     * Automatically ensures the `.preferences_pb` extension required by PreferenceDataStoreFactory.
     */
    fun createPreferenceDataStore(
        file: File,
        scope: CoroutineScope
    ): DataStore<Preferences> {
        val target = if (file.name.endsWith(".preferences_pb")) {
            file
        } else {
            File(file.parentFile, "${file.name}.preferences_pb")
        }
        target.parentFile?.mkdirs()
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { target }
        )
    }

    /** Clears every key in the shared DataStore (used when MainActivity receives EXTRA_RESET_STATE = true). */
    suspend fun clearEntireDataStore(dataStore: DataStore<Preferences>) {
        dataStore.edit { it.clear() }
    }
}
