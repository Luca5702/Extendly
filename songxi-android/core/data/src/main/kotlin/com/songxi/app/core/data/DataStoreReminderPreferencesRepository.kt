package com.songxi.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.songxi.app.core.model.ReminderPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class DataStoreReminderPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : ReminderPreferencesRepository {

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override val preferences: Flow<ReminderPreferences> = safePreferences.map { prefs ->
        val raw = prefs[KEY_PREFERENCES_JSON]
        if (raw.isNullOrBlank()) {
            ReminderPreferences()
        } else {
            runCatching {
                sanitize(SongxiDataJson.decodeFromString<ReminderPreferences>(raw))
            }.getOrDefault(ReminderPreferences())
        }
    }

    override suspend fun updatePreferences(preferences: ReminderPreferences) {
        val normalized = sanitize(preferences)
        dataStore.edit { prefs ->
            prefs[KEY_PREFERENCES_JSON] = SongxiDataJson.encodeToString(normalized)
        }
    }

    override fun notificationsSentOn(dateIso: String): Flow<Int> = safePreferences.map { prefs ->
        decodeCounts(prefs[KEY_SENT_COUNTS_JSON])[dateIso] ?: 0
    }

    override suspend fun recordNotificationSent(dateIso: String) {
        if (dateIso.isBlank()) return
        dataStore.edit { prefs ->
            val current = decodeCounts(prefs[KEY_SENT_COUNTS_JSON]).toMutableMap()
            current[dateIso] = (current[dateIso] ?: 0) + 1
            val trimmed = trimLatestDates(current, MAX_RETAINED_DATES)
            prefs[KEY_SENT_COUNTS_JSON] = SongxiDataJson.encodeToString(trimmed)
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PREFERENCES_JSON)
            prefs.remove(KEY_SENT_COUNTS_JSON)
        }
    }

    private fun sanitize(input: ReminderPreferences): ReminderPreferences {
        val safeInterval = if (input.intervalMinutes in ReminderPreferences.SUPPORTED_INTERVALS) {
            input.intervalMinutes
        } else {
            input.intervalMinutes.coerceIn(15, 240)
        }
        val safeMaxCount = input.dailyMaxCount.coerceIn(ReminderPreferences.DAILY_MAX_RANGE)
        return input.copy(
            intervalMinutes = safeInterval,
            dailyMaxCount = safeMaxCount
        )
    }

    private fun decodeCounts(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            SongxiDataJson.decodeFromString<Map<String, Int>>(raw)
        }.getOrDefault(emptyMap())
    }

    private fun trimLatestDates(map: Map<String, Int>, limit: Int): Map<String, Int> {
        if (map.size <= limit) return map
        return map.entries
            .sortedByDescending { it.key }
            .take(limit)
            .associate { it.key to it.value }
    }

    private companion object {
        const val MAX_RETAINED_DATES = 30
        val KEY_PREFERENCES_JSON = stringPreferencesKey("reminder.preferences_json")
        val KEY_SENT_COUNTS_JSON = stringPreferencesKey("reminder.sent_counts_json")
    }
}
