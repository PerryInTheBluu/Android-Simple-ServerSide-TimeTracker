package com.example.util.simpletimetracker.domain.timetable.model

import java.time.LocalDate

/**
 * A date range in which the timetable is paused: no preparation or
 * follow up alarms fire and the calendar hides the lecture bands.
 */
data class VacationPeriod(
    val start: LocalDate,
    val end: LocalDate,
) {

    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(start) && !date.isAfter(end)
    }

    companion object {

        private const val PERIOD_SEPARATOR = ";"
        private const val DATE_SEPARATOR = "|"

        fun serialize(periods: List<VacationPeriod>): String {
            return periods.joinToString(PERIOD_SEPARATOR) { period ->
                "${period.start}$DATE_SEPARATOR${period.end}"
            }
        }

        fun deserialize(value: String): List<VacationPeriod> {
            return value.split(PERIOD_SEPARATOR).mapNotNull { entry ->
                val parts = entry.split(DATE_SEPARATOR)
                if (parts.size != 2) return@mapNotNull null
                runCatching {
                    val start = LocalDate.parse(parts[0])
                    val end = LocalDate.parse(parts[1])
                    if (end.isBefore(start)) return@mapNotNull null
                    VacationPeriod(start = start, end = end)
                }.getOrNull()
            }
        }
    }
}
