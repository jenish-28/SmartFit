package com.example.smartfit.storage

import com.example.smartfit.model.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Pure date/aggregation helpers shared by the dashboard and history screens. */

fun Workout.localDate(): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.endOfWeek(): LocalDate = with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

fun List<Workout>.between(start: LocalDate, endInclusive: LocalDate): List<Workout> =
    filter { val d = it.localDate(); !d.isBefore(start) && !d.isAfter(endInclusive) }

fun List<Workout>.inMonth(month: YearMonth): List<Workout> =
    filter { YearMonth.from(it.localDate()) == month }

/** Consecutive days (ending today, or yesterday if today has no workout yet) with at least one workout. */
fun List<Workout>.currentStreakDays(today: LocalDate = LocalDate.now()): Int {
    val dates = mapTo(HashSet()) { it.localDate() }
    var cursor = if (today in dates) today else today.minusDays(1)
    var streak = 0
    while (cursor in dates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun List<Workout>.totalDurationMinutes(): Int = sumOf { it.duration }
