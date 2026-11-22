package com.songxi.app.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun interface ClockProvider {
    fun nowEpochMillis(): Long

    fun currentZoneId(): ZoneId = ZoneId.systemDefault()

    fun currentLocalDateTime(): LocalDateTime =
        Instant.ofEpochMilli(nowEpochMillis())
            .atZone(currentZoneId())
            .toLocalDateTime()

    fun currentLocalDate(): LocalDate =
        currentLocalDateTime().toLocalDate()

    fun currentDateIso(): String =
        currentLocalDate().toString()
}

object SystemClockProvider : ClockProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

object BreathRhythm {
    const val INHALE_DURATION_MS: Long = 4_000L
    const val EXHALE_DURATION_MS: Long = 6_000L
    const val CYCLE_DURATION_MS: Long = INHALE_DURATION_MS + EXHALE_DURATION_MS
    const val CYCLE_DURATION_SECONDS: Int = 10
}
