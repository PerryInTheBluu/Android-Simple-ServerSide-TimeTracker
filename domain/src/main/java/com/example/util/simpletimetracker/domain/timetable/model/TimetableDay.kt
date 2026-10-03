package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A whole timetable day exception, for example a public holiday or a
 * lecture free day. The date is stored as yyyy-MM-dd.
 */
data class TimetableDay(
    val id: Long = 0,
    val date: String,
    val freeDay: Boolean,
    val note: String,
)
