package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A todo attached to a timetable event, for example preparation or
 * follow up work. Preparation todos are queried automatically before
 * lectures and exercises, follow up todos after lectures.
 */
data class TimetableTodo(
    val id: Long = 0,
    val eventId: Long,
    // The date the todo belongs to, as yyyy-MM-dd; null repeats weekly.
    val date: String?,
    val text: String,
    val done: Boolean,
    val type: Type,
) {

    enum class Type {
        PREPARATION,
        FOLLOW_UP,
        GENERAL,
    }
}
