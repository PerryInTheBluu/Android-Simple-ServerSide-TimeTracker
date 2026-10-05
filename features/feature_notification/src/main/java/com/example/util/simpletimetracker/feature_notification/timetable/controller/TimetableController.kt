package com.example.util.simpletimetracker.feature_notification.timetable.controller

import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import javax.inject.Inject

class TimetableController @Inject constructor(
    private val timetableNotificationInteractor: TimetableNotificationInteractor,
) {

    suspend fun onPreparationDue(
        eventId: Long,
        date: String,
    ) {
        timetableNotificationInteractor.onPreparationDue(eventId, date)
    }

    suspend fun onFollowUpDue(
        eventId: Long,
        date: String,
    ) {
        timetableNotificationInteractor.onFollowUpDue(eventId, date)
    }

    suspend fun onTodoDone(
        eventId: Long,
        date: String,
        type: TimetableTodo.Type,
    ) {
        timetableNotificationInteractor.onTodoDone(eventId, date, type)
    }

    suspend fun onVacationResume() {
        rescheduleAll()
    }

    suspend fun onBootCompleted() {
        rescheduleAll()
    }

    suspend fun onExactAlarmPermissionStateChanged() {
        rescheduleAll()
    }

    suspend fun onPackageReplaced() {
        rescheduleAll()
    }

    suspend fun onDateTimeChanged() {
        rescheduleAll()
    }

    private suspend fun rescheduleAll() {
        timetableNotificationInteractor.rescheduleAll()
    }
}
