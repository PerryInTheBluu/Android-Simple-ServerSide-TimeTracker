package com.example.util.simpletimetracker.domain.timetable.ics

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Minimal parser for calendar exports (ics) used by universities.
 * Extracts weekly recurring events: name (SUMMARY), days of week
 * (RRULE ... BYDAY), start and end time (DTSTART/DTEND, the TZID
 * parameter is respected), room (LOCATION) and a comment (DESCRIPTION).
 *
 * Pure jvm code without android dependencies, so it can be unit tested.
 */
class IcsParser @Inject constructor() {

    data class ImportedEvent(
        val name: String,
        // ISO convention: MONDAY = 1 .. SUNDAY = 7.
        val daysOfWeek: List<Int>,
        // Minutes of day.
        val startTime: Int,
        val endTime: Int,
        val room: String,
        val comment: String,
    )

    fun parse(content: String): List<ImportedEvent> {
        val lines = unfold(content.lines())
        val result = mutableListOf<ImportedEvent>()
        var inEvent = false
        var summary = ""
        var location = ""
        var description = ""
        var start: LocalDateTime? = null
        var end: LocalDateTime? = null
        var zone: ZoneId = ZoneId.systemDefault()
        var days = listOf<DayOfWeek>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            when {
                line.equals(BEGIN_EVENT, ignoreCase = true) -> {
                    inEvent = true
                    summary = ""
                    location = ""
                    description = ""
                    start = null
                    end = null
                    zone = ZoneId.systemDefault()
                    days = listOf()
                }
                line.equals(END_EVENT, ignoreCase = true) -> {
                    val eventStart = start
                    val eventEnd = end
                    if (inEvent && summary.isNotEmpty() && eventStart != null && eventEnd != null) {
                        val startMinutes = eventStart.toLocalTime().let { it.hour * 60 + it.minute }
                        val endMinutes = eventEnd.toLocalTime().let { it.hour * 60 + it.minute }
                        val eventDays = days.ifEmpty { listOf(eventStart.dayOfWeek) }
                        eventDays.forEach { day ->
                            result.add(
                                ImportedEvent(
                                    name = summary,
                                    daysOfWeek = listOf(day.value),
                                    startTime = startMinutes,
                                    endTime = endMinutes,
                                    room = location,
                                    comment = description,
                                ),
                            )
                        }
                    }
                    inEvent = false
                }
                inEvent && line.startsWith(PREFIX_SUMMARY, ignoreCase = true) -> {
                    summary = line.substringAfter(':', "").unescape()
                }
                inEvent && line.startsWith(PREFIX_LOCATION, ignoreCase = true) -> {
                    location = line.substringAfter(':', "").unescape()
                }
                inEvent && line.startsWith(PREFIX_DESCRIPTION, ignoreCase = true) -> {
                    description = line.substringAfter(':', "").unescape()
                }
                inEvent && line.startsWith(PREFIX_RRULE, ignoreCase = true) -> {
                    days = parseByDay(line)
                }
                inEvent && (line.startsWith(PREFIX_DTSTART, ignoreCase = true)) -> {
                    zone = parseZone(line).let { it ?: zone }
                    start = parseDateTime(line) ?: start
                }
                inEvent && (line.startsWith(PREFIX_DTEND, ignoreCase = true)) -> {
                    end = parseDateTime(line) ?: end
                }
            }
        }
        return result
    }

    private fun unfold(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        lines.forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.size - 1] = result.last() + line.substring(1)
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun parseZone(line: String): ZoneId? {
        val tzId = line.substringBefore(':').substringAfter("TZID=", "").trim()
        return if (tzId.isNotEmpty()) runCatching { ZoneId.of(tzId) }.getOrNull() else null
    }

    private fun parseDateTime(line: String): LocalDateTime? {
        val value = line.substringAfter(':', "").trim()
        val zone = parseZone(line)
        val local = run {
            // 20261013T101500Z (utc) or 20261013T101500 (local/floating).
            val cleaned = value.removeSuffix("Z")
            runCatching {
                LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
            }.getOrNull()
        } ?: return null
        return if (value.endsWith("Z")) {
            local.atZone(ZoneId.of("UTC")).withZoneSameInstant(zone ?: ZoneId.systemDefault()).toLocalDateTime()
        } else {
            local
        }
    }

    private fun parseByDay(line: String): List<DayOfWeek> {
        val byDayPart = line.split(';').firstOrNull { it.startsWith("BYDAY=", ignoreCase = true) } ?: return listOf()
        return byDayPart.substringAfter('=').split(',')
            .mapNotNull { token -> mapIcsDay(token.trim()) }
    }

    private fun mapIcsDay(token: String): DayOfWeek? = when (token.uppercase()) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun String.unescape(): String = this
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")

    companion object {
        private const val BEGIN_EVENT = "BEGIN:VEVENT"
        private const val END_EVENT = "END:VEVENT"
        private const val PREFIX_SUMMARY = "SUMMARY"
        private const val PREFIX_LOCATION = "LOCATION"
        private const val PREFIX_DESCRIPTION = "DESCRIPTION"
        private const val PREFIX_RRULE = "RRULE"
        private const val PREFIX_DTSTART = "DTSTART"
        private const val PREFIX_DTEND = "DTEND"
    }
}
