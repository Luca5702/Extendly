package com.songxi.app.core.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class PracticeSessionRecord(
    val id: String,
    val completedAtEpochMs: Long,
    val dateIso: String,
    val routineId: String,
    val routineTitle: String,
    val durationSeconds: Int,
    val skinId: SkinId,
    val feeling: SomaticFeeling? = null,
    val targetRegions: Set<BodyRegion> = emptySet(),
    val asymmetry: LateralAsymmetry = LateralAsymmetry.BALANCED
)

@Serializable
data class ReminderPreferences(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val dailyMaxCount: Int = 2,
    val lunchQuietEnabled: Boolean = true,
    val nightQuietEnabled: Boolean = true,
    val weekendSilent: Boolean = true,
    val muteTodayDateIso: String? = null
) {
    fun isMutedOrQuietAt(dateTime: LocalDateTime): Boolean {
        if (!enabled) return true
        val dateIso = dateTime.toLocalDate().toString()
        if (muteTodayDateIso == dateIso) return true
        if (weekendSilent && (dateTime.dayOfWeek == DayOfWeek.SATURDAY || dateTime.dayOfWeek == DayOfWeek.SUNDAY)) {
            return true
        }
        val time = dateTime.toLocalTime()
        if (lunchQuietEnabled && !time.isBefore(LUNCH_QUIET_START) && time.isBefore(LUNCH_QUIET_END)) {
            return true
        }
        if (nightQuietEnabled && (!time.isBefore(NIGHT_QUIET_START) || time.isBefore(NIGHT_QUIET_END))) {
            return true
        }
        return false
    }

    companion object {
        val SUPPORTED_INTERVALS: List<Int> = listOf(45, 60, 90, 120)
        val DAILY_MAX_RANGE: IntRange = 1..3
        val LUNCH_QUIET_START: LocalTime = LocalTime.of(12, 0)
        val LUNCH_QUIET_END: LocalTime = LocalTime.of(13, 30)
        val NIGHT_QUIET_START: LocalTime = LocalTime.of(20, 0)
        val NIGHT_QUIET_END: LocalTime = LocalTime.of(9, 0)
    }
}

@Serializable
data class SplashAdCreative(
    val id: String,
    val sponsorName: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val landingSummary: String,
    val landingUrl: String,
    val artworkTheme: SplashArtworkStyle,
    val durationSeconds: Int = 3
)
