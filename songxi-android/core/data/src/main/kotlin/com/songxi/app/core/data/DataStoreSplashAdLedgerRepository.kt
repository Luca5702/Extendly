package com.songxi.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class DataStoreSplashAdLedgerRepository(
    private val dataStore: DataStore<Preferences>
) : SplashAdLedgerRepository {

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override val totalAppLaunches: Flow<Int> = safePreferences.map { prefs ->
        prefs[KEY_TOTAL_APP_LAUNCHES] ?: 0
    }

    override val personalizedAdsEnabled: Flow<Boolean> = safePreferences.map { prefs ->
        prefs[KEY_PERSONALIZED_ADS_ENABLED] ?: false
    }

    override val lastImpressionEpochMs: Flow<Long?> = safePreferences.map { prefs ->
        prefs[KEY_LAST_IMPRESSION_EPOCH_MS]
    }

    override val totalSkips: Flow<Int> = safePreferences.map { prefs ->
        prefs[KEY_TOTAL_SKIPS] ?: 0
    }

    override val totalCtaClicks: Flow<Int> = safePreferences.map { prefs ->
        prefs[KEY_TOTAL_CTA_CLICKS] ?: 0
    }

    override fun impressionsOn(dateIso: String): Flow<Int> = safePreferences.map { prefs ->
        decodeCounts(prefs[KEY_IMPRESSIONS_BY_DATE_JSON])[dateIso] ?: 0
    }

    override suspend fun incrementAppLaunch(): Int {
        var updatedCount = 0
        dataStore.edit { prefs ->
            updatedCount = (prefs[KEY_TOTAL_APP_LAUNCHES] ?: 0) + 1
            prefs[KEY_TOTAL_APP_LAUNCHES] = updatedCount
        }
        return updatedCount
    }

    override suspend fun recordImpression(creativeId: String, epochMs: Long, dateIso: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_IMPRESSION_EPOCH_MS] = epochMs
            prefs[KEY_LAST_CREATIVE_ID] = creativeId
            if (dateIso.isNotBlank()) {
                val counts = decodeCounts(prefs[KEY_IMPRESSIONS_BY_DATE_JSON]).toMutableMap()
                counts[dateIso] = (counts[dateIso] ?: 0) + 1
                val trimmed = trimLatestDates(counts, MAX_RETAINED_DATES)
                prefs[KEY_IMPRESSIONS_BY_DATE_JSON] = SongxiDataJson.encodeToString(trimmed)
            }
        }
    }

    override suspend fun recordSkip(creativeId: String, elapsedMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_TOTAL_SKIPS] = (prefs[KEY_TOTAL_SKIPS] ?: 0) + 1
            prefs[KEY_LAST_CREATIVE_ID] = creativeId
            prefs[KEY_LAST_SKIP_ELAPSED_MS] = elapsedMs.coerceAtLeast(0L)
        }
    }

    override suspend fun recordCtaClick(creativeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TOTAL_CTA_CLICKS] = (prefs[KEY_TOTAL_CTA_CLICKS] ?: 0) + 1
            prefs[KEY_LAST_CREATIVE_ID] = creativeId
        }
    }

    override suspend fun setPersonalizedAdsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PERSONALIZED_ADS_ENABLED] = enabled
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOTAL_APP_LAUNCHES)
            prefs.remove(KEY_PERSONALIZED_ADS_ENABLED)
            prefs.remove(KEY_LAST_IMPRESSION_EPOCH_MS)
            prefs.remove(KEY_TOTAL_SKIPS)
            prefs.remove(KEY_TOTAL_CTA_CLICKS)
            prefs.remove(KEY_IMPRESSIONS_BY_DATE_JSON)
            prefs.remove(KEY_LAST_CREATIVE_ID)
            prefs.remove(KEY_LAST_SKIP_ELAPSED_MS)
        }
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
        val KEY_TOTAL_APP_LAUNCHES = intPreferencesKey("splash.total_app_launches")
        val KEY_PERSONALIZED_ADS_ENABLED = booleanPreferencesKey("splash.personalized_ads_enabled")
        val KEY_LAST_IMPRESSION_EPOCH_MS = longPreferencesKey("splash.last_impression_epoch_ms")
        val KEY_TOTAL_SKIPS = intPreferencesKey("splash.total_skips")
        val KEY_TOTAL_CTA_CLICKS = intPreferencesKey("splash.total_cta_clicks")
        val KEY_IMPRESSIONS_BY_DATE_JSON = stringPreferencesKey("splash.impressions_by_date_json")
        val KEY_LAST_CREATIVE_ID = stringPreferencesKey("splash.last_creative_id")
        val KEY_LAST_SKIP_ELAPSED_MS = longPreferencesKey("splash.last_skip_elapsed_ms")
    }
}
