package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A weekly recurring timetable slot, for example a lecture or an exercise.
 *
 * The day of the week uses the ISO convention
 * (MONDAY = 1 .. SUNDAY = 7, like java.time.DayOfWeek). Start and end are minutes of day,
 * so 10:15 is 615. Per date exceptions (room changes, single
 * cancellations) are stored as TimetableEventOverride, whole free days
 * as TimetableDay.
 */
data class TimetableEvent(
    val id: Long = 0,
    val name: String,
    val dayOfWeek: Int,
    val startTime: Int,
    val endTime: Int,
    val room: String,
    val type: Type,
    val comment: String,
    // Optional link to a trackable activity, so records can be matched.
    val activityTypeId: Long? = null,
) {

    enum class Type {
        LECTURE,
        EXERCISE,
        TUTORIUM,
    }
}
