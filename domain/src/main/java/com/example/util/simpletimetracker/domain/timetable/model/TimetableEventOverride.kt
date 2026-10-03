package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A per date exception for a timetable event: room change, moved times
 * or a single cancellation. The date is stored as yyyy-MM-dd.
 */
data class TimetableEventOverride(
    val id: Long = 0,
    val date: String,
    val eventId: Long,
    val room: String,
    val startTime: Int,
    val endTime: Int,
    val cancelled: Boolean,
    val note: String,
)
