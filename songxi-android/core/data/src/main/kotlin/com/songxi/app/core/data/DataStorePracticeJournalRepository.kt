package com.songxi.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.songxi.app.core.model.PracticeSessionRecord
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class DataStorePracticeJournalRepository(
    private val dataStore: DataStore<Preferences>
) : PracticeJournalRepository {

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override val allRecords: Flow<List<PracticeSessionRecord>> = safePreferences.map { prefs ->
        decodeRecords(prefs[KEY_RECORDS_JSON])
            .sortedWith(
                compareByDescending<PracticeSessionRecord> { it.completedAtEpochMs }
                    .thenByDescending { it.id }
            )
    }

    override fun observeMonthRipples(month: YearMonth): Flow<Map<LocalDate, List<PracticeSessionRecord>>> =
        allRecords.map { records ->
            val grouped = linkedMapOf<LocalDate, MutableList<PracticeSessionRecord>>()
            for (record in records) {
                val date = runCatching { LocalDate.parse(record.dateIso) }.getOrNull() ?: continue
                if (YearMonth.from(date) == month) {
                    grouped.getOrPut(date) { mutableListOf() }.add(record)
                }
            }
            grouped.mapValues { (_, list) -> list.toList() }
        }

    override fun observeMonthSummary(month: YearMonth): Flow<MonthlyRippleSummary> =
        observeMonthRipples(month).map { monthMap ->
            val allMonthSessions = monthMap.values.flatten()
            MonthlyRippleSummary(
                month = month,
                totalSessions = allMonthSessions.size,
                totalDurationSeconds = allMonthSessions.sumOf { it.durationSeconds },
                practicedDaysCount = monthMap.keys.size
            )
        }

    override suspend fun recordCompletedSession(record: PracticeSessionRecord) {
        dataStore.edit { prefs ->
            val current = decodeRecords(prefs[KEY_RECORDS_JSON]).toMutableList()
            val existingIdx = current.indexOfFirst { it.id == record.id }
            if (existingIdx >= 0) {
                current[existingIdx] = record
            } else {
                current.add(record)
            }
            prefs[KEY_RECORDS_JSON] = SongxiDataJson.encodeToString(current)
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RECORDS_JSON)
        }
    }

    private fun decodeRecords(raw: String?): List<PracticeSessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            SongxiDataJson.decodeFromString<List<PracticeSessionRecord>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_RECORDS_JSON = stringPreferencesKey("journal.records_json")
    }
}
