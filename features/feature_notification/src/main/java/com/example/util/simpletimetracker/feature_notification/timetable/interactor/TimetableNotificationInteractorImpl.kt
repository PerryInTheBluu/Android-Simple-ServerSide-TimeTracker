package com.example.util.simpletimetracker.feature_notification.timetable.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import com.example.util.simpletimetracker.feature_notification.timetable.manager.TimetableNotificationManager
import com.example.util.simpletimetracker.feature_notification.timetable.manager.TimetableNotificationManager.Companion.notificationTag
import com.example.util.simpletimetracker.feature_notification.timetable.scheduler.TimetableAlarmScheduler
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class TimetableNotificationInteractorImpl @Inject constructor(
    private val timetableRepo: TimetableRepo,
    private val alarmScheduler: TimetableAlarmScheduler,
    private val notificationManager: TimetableNotificationManager,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
    private val resourceRepo: ResourceRepo,
) : TimetableNotificationInteractor {

    override suspend fun rescheduleAll() {
        val events = timetableRepo.getAllEvents()
        if (events.isEmpty()) return
        val freeDays = timetableRepo.getDays().filter { it.freeDay }.map { it.date }.toSet()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val prepLead = prefsInteractor.getTimetablePrepLead()

        for (dayOffset in 0..SCHEDULE_DAYS) {
            val day = today.plusDays(dayOffset.toLong())
            val date = day.toString()
            if (date in freeDays) continue
            val overrides = timetableRepo.getOverrides(date).associateBy { it.eventId }
            events
                .filter { it.dayOfWeek == day.dayOfWeek.value }
                .forEach { event ->
                    val override = overrides[event.id]
                    if (override?.cancelled == true) return@forEach
                    val startMinutes = override?.startTime?.takeIf { it != 0 } ?: event.startTime
                    val endMinutes = override?.endTime?.takeIf { it != 0 } ?: event.endTime
                    val midnight = day.atStartOfDay(zone).toInstant().toEpochMilli()
                    val startTimestamp = midnight + startMinutes * MINUTE_MILLIS
                    val endTimestamp = midnight + endMinutes * MINUTE_MILLIS

                    val preparationTrigger = startTimestamp - prepLead
                    if (preparationTrigger > now) {
                        alarmScheduler.schedulePreparation(
                            eventId = event.id,
                            date = date,
                            triggerTimestamp = preparationTrigger,
                        )
                    }
                    if (event.type == TimetableEvent.Type.LECTURE && endTimestamp > now) {
                        alarmScheduler.scheduleFollowUp(
                            eventId = event.id,
                            date = date,
                            triggerTimestamp = endTimestamp,
                        )
                    }
                }
        }
    }

    override suspend fun onPreparationDue(eventId: Long, date: String) {
        val event = getEvent(eventId) ?: return
        ensureTodo(eventId, date, TimetableTodo.Type.PREPARATION)
        val startTime = parseDate(date)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + event.startTime * MINUTE_MILLIS
        notificationManager.show(
            tag = notificationTag(TimetableTodo.Type.PREPARATION, eventId, date),
            title = resourceRepo.getString(R.string.timetable_notification_prep_title, event.name),
            text = resourceRepo.getString(
                R.string.timetable_notification_prep_text,
                formatTime(startTime),
            ),
            eventId = eventId,
            date = date,
            type = TimetableTodo.Type.PREPARATION,
        )
    }

    override suspend fun onFollowUpDue(eventId: Long, date: String) {
        val event = getEvent(eventId) ?: return
        ensureTodo(eventId, date, TimetableTodo.Type.FOLLOW_UP)
        val endTime = parseDate(date)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + event.endTime * MINUTE_MILLIS
        notificationManager.show(
            tag = notificationTag(TimetableTodo.Type.FOLLOW_UP, eventId, date),
            title = resourceRepo.getString(R.string.timetable_notification_follow_title, event.name),
            text = resourceRepo.getString(
                R.string.timetable_notification_follow_text,
                formatTime(endTime),
            ),
            eventId = eventId,
            date = date,
            type = TimetableTodo.Type.FOLLOW_UP,
        )
    }

    override suspend fun onTodoDone(
        eventId: Long,
        date: String,
        type: TimetableTodo.Type,
    ) {
        val todo = timetableRepo.getTodos(eventId)
            .firstOrNull { it.date == date && it.type == type }
        if (todo != null) {
            timetableRepo.setTodoDone(todo.id, true)
            LocalDataChangedBus.publish()
        } else {
            timetableRepo.addTodo(
                TimetableTodo(
                    eventId = eventId,
                    date = date,
                    text = todoText(type),
                    done = true,
                    type = type,
                ),
            )
        }
    }

    // Creates the todo when the question is asked, so an unanswered
    // question stays visible as an open todo.
    private suspend fun ensureTodo(
        eventId: Long,
        date: String,
        type: TimetableTodo.Type,
    ) {
        val exists = timetableRepo.getTodos(eventId).any { it.date == date && it.type == type }
        if (!exists) {
            timetableRepo.addTodo(
                TimetableTodo(
                    eventId = eventId,
                    date = date,
                    text = todoText(type),
                    done = false,
                    type = type,
                ),
            )
        }
    }

    private suspend fun getEvent(eventId: Long): TimetableEvent? {
        return timetableRepo.getAllEvents().firstOrNull { it.id == eventId }
    }

    private fun todoText(type: TimetableTodo.Type): String {
        return when (type) {
            TimetableTodo.Type.PREPARATION -> resourceRepo.getString(R.string.timetable_todo_preparation)
            TimetableTodo.Type.FOLLOW_UP -> resourceRepo.getString(R.string.timetable_todo_follow_up)
            TimetableTodo.Type.GENERAL -> resourceRepo.getString(R.string.timetable_todo_general)
        }
    }

    private suspend fun formatTime(timestamp: Long): String {
        return timeMapper.formatTime(
            time = timestamp,
            useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat(),
            showSeconds = false,
        )
    }

    private fun parseDate(date: String): LocalDate {
        return LocalDate.parse(date)
    }

    companion object {
        private const val MINUTE_MILLIS = 60_000L
        private const val SCHEDULE_DAYS = 7L
    }
}
