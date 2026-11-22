package com.songxi.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.songxi.app.core.model.BodyRegion
import com.songxi.app.core.model.BookletSeries
import com.songxi.app.core.model.BreathRhythm
import com.songxi.app.core.model.PostureScene
import com.songxi.app.core.model.StretchPose
import com.songxi.app.core.model.StretchRoutine
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class DataStoreStretchCatalogRepository(
    private val dataStore: DataStore<Preferences>
) : StretchCatalogRepository {

    private val composedRoutineCache = ConcurrentHashMap<String, StretchRoutine>()

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override val allPoses: List<StretchPose> = SongxiStretchCatalog.allPoses

    override val quickReliefRoutines: List<StretchRoutine> = SongxiStretchCatalog.quickReliefRoutines

    override val curatedBooklets: List<BookletSeries> = SongxiStretchCatalog.curatedBooklets

    override val bookletCompletedDays: Flow<Map<String, Set<Int>>> = safePreferences.map { prefs ->
        decodeBookletProgress(prefs[KEY_BOOKLET_COMPLETED_DAYS_JSON])
    }

    override fun heroRoutineFor(scene: PostureScene): StretchRoutine =
        SongxiStretchCatalog.heroRoutinesByScene.getValue(scene)

    override fun reminderQuickStartRoutine(): StretchRoutine =
        SongxiStretchCatalog.quickMeetingSighRoutine

    override fun findRoutineById(routineId: String): StretchRoutine? {
        if (routineId.isBlank()) return null

        for (scene in PostureScene.entries) {
            val hero = heroRoutineFor(scene)
            if (hero.id == routineId) return hero
        }

        quickReliefRoutines.firstOrNull { it.id == routineId }?.let { return it }

        for (booklet in curatedBooklets) {
            booklet.routines.firstOrNull { it.id == routineId }?.let { return it }
        }

        composedRoutineCache[routineId]?.let { return it }

        val customMatch = CUSTOM_ROUTINE_ID_REGEX.matchEntire(routineId)
        if (customMatch != null) {
            val regionTokens = customMatch.groupValues[1].split("_")
            val parsedRegions = regionTokens.mapNotNull { BodyRegion.fromKey(it) }.toSet()
            val targetMinutes = customMatch.groupValues[2].toIntOrNull()
            if (parsedRegions.isNotEmpty() && targetMinutes != null) {
                return composeCustomRoutine(parsedRegions, targetMinutes)
            }
        }

        return null
    }

    override fun composeCustomRoutine(
        regions: Set<BodyRegion>,
        targetMinutes: Int
    ): StretchRoutine {
        val safeMinutes = targetMinutes.coerceIn(1, 15)
        val orderedRegions = BodyRegion.entries.filter { it in regions }.ifEmpty {
            listOf(BodyRegion.POSTERIOR_NECK, BodyRegion.TRAPEZIUS_SHOULDER)
        }

        val basePoseCount = when {
            safeMinutes <= 2 -> 3
            safeMinutes <= 4 -> 4
            else -> 6
        }
        val poseCount = maxOf(basePoseCount, orderedRegions.size)
        val selected = LinkedHashMap<String, StretchPose>()

        // Pass 1: Round-robin across orderedRegions' primary pools (depth 0..3)
        for (depth in 0 until 4) {
            for (region in orderedRegions) {
                if (selected.size >= poseCount) break
                val candidate = SongxiStretchCatalog.primaryPoolByRegion.getValue(region)[depth]
                selected.putIfAbsent(candidate.id, candidate)
            }
            if (selected.size >= poseCount) break
        }

        // Pass 2: Secondary catalog poses whose targetRegions intersect orderedRegions
        if (selected.size < poseCount) {
            val regionSet = orderedRegions.toSet()
            for (pose in allPoses) {
                if (selected.size >= poseCount) break
                if (pose.targetRegions.any { it in regionSet }) {
                    selected.putIfAbsent(pose.id, pose)
                }
            }
        }

        // Pass 3: Calm closing integration poses
        if (selected.size < poseCount) {
            val closingFallback = listOf(
                SongxiStretchCatalog.shoulderDoubleInhaleSigh,
                SongxiStretchCatalog.shoulderScapulaSigh,
                SongxiStretchCatalog.eyeWarmPalming,
                SongxiStretchCatalog.neckOcciputCradle
            ) + allPoses
            for (pose in closingFallback) {
                if (selected.size >= poseCount) break
                selected.putIfAbsent(pose.id, pose)
            }
        }

        val chosenList = selected.values.toList()
        val totalCycles = (safeMinutes * 60) / BreathRhythm.CYCLE_DURATION_SECONDS
        val baseCycles = totalCycles / chosenList.size
        val extraCycles = totalCycles % chosenList.size

        val scaledPoses = chosenList.mapIndexed { index, pose ->
            val cycles = baseCycles + if (index < extraCycles) 1 else 0
            pose.copy(durationSeconds = cycles * BreathRhythm.CYCLE_DURATION_SECONDS)
        }

        val regionTitlePrefix = when {
            orderedRegions.size <= 3 -> orderedRegions.joinToString("与") { it.shortLabelCn }
            else -> "${orderedRegions.first().shortLabelCn}等${orderedRegions.size}处"
        }
        val styleWord = when {
            safeMinutes <= 2 -> "救急轻方"
            safeMinutes <= 4 -> "温养小方"
            else -> "深舒长卷"
        }
        val routineId = "custom_${orderedRegions.joinToString("_") { it.key }}_${safeMinutes}m"
        val routine = StretchRoutine(
            id = routineId,
            title = "《$regionTitlePrefix · $styleWord（${safeMinutes}分钟）》",
            subtitle = "含：" + scaledPoses.joinToString(" · ") { it.title },
            scene = PostureScene.DESK_SEATED,
            eyesFreeCapable = true,
            seatedDiscreet = scaledPoses.none {
                it.id == "thoracic_desk_cat_cow" || it.id == "thoracic_clasp_chest_open"
            },
            poses = scaledPoses
        )
        composedRoutineCache[routineId] = routine
        return routine
    }

    override suspend fun markBookletDayCompleted(bookletId: String, dayIndex: Int) {
        if (bookletId.isBlank() || dayIndex < 0) return
        val maxDays = curatedBooklets.firstOrNull { it.id == bookletId }?.totalDays
        if (maxDays != null && dayIndex >= maxDays) return
        dataStore.edit { prefs ->
            val current = decodeBookletProgress(prefs[KEY_BOOKLET_COMPLETED_DAYS_JSON]).toMutableMap()
            val updatedDays = (current[bookletId] ?: emptySet()) + dayIndex
            current[bookletId] = updatedDays
            prefs[KEY_BOOKLET_COMPLETED_DAYS_JSON] = SongxiDataJson.encodeToString(current)
        }
    }

    override suspend fun resetAll() {
        composedRoutineCache.clear()
        dataStore.edit { prefs ->
            prefs.remove(KEY_BOOKLET_COMPLETED_DAYS_JSON)
        }
    }

    private fun decodeBookletProgress(raw: String?): Map<String, Set<Int>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            SongxiDataJson.decodeFromString<Map<String, Set<Int>>>(raw)
        }.getOrDefault(emptyMap())
    }

    private companion object {
        val CUSTOM_ROUTINE_ID_REGEX = Regex("^custom_([a-z_]+)_(\\d+)m$")
        val KEY_BOOKLET_COMPLETED_DAYS_JSON = stringPreferencesKey("catalog.booklet_completed_days_json")
    }
}
