package com.example.util.simpletimetracker.domain.timetable.notification

import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo

/**
 * Asks the user to prepare for an upcoming timetable slot and to
 * follow up after a lecture, via notifications. The answers are
 * stored as TimetableTodo entries per date.
 */
interface TimetableNotificationInteractor {

    // Schedules preparation and follow-up alarms for upcoming slots.
    suspend fun rescheduleAll()

    suspend fun onPreparationDue(
        eventId: Long,
        date: String,
    )

    suspend fun onFollowUpDue(
        eventId: Long,
        date: String,
    )

    suspend fun onTodoDone(
        eventId: Long,
        date: String,
        type: TimetableTodo.Type,
    )
}
