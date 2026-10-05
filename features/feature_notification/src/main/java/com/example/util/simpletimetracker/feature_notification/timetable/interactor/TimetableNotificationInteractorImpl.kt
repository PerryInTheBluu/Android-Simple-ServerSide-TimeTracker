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
        // Vacation mode pauses the timetable: cancel everything, also
        // inexact alarms that are still queued from before.
        if (prefsInteractor.getVacationMode()) {
            alarmScheduler.cancelAll(events = events, days = SCHEDULE_DAYS.toInt())
            return
        }
        val freeDays = timetableRepo.getDays().filter { it.freeDay }.map { it.date }.toSet()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val prepLead = prefsInteractor.getTimetablePrepLead()

        // Alarms for days inside a planned vacation period that were
        // scheduled earlier have to be cancelled explicitly.
        val vacationPeriods = prefsInteractor.getVacationPeriods()
        vacationPeriods.forEach { period ->
            var day = if (period.start.isBefore(today)) today else period.start
            while (!day.isAfter(period.end)) {
                alarmScheduler.cancelDay(events = events, date = day.toString())
                day = day.plusDays(1)
            }
        }

        for (dayOffset in 0..SCHEDULE_DAYS) {
            val day = today.plusDays(dayOffset.toLong())
            val date = day.toString()
            if (date in freeDays) continue
            // Planned vacation periods pause the timetable for their
            // days only; days before and after keep their alarms.
            if (prefsInteractor.isVacationDay(day)) continue
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
        scheduleVacationResumeAlarms(now = now, prepLead = prepLead)
    }

    // Keeps a single alarm that re-schedules the regular timetable
    // shortly before the first day after a vacation period. Without
    // it the alarms for the days after the vacation would be missing
    // if the app was not opened during the vacation.
    private suspend fun scheduleVacationResumeAlarms(
        now: Long,
        prepLead: Long,
    ) {
        alarmScheduler.cancelVacationResume()
        val zone = ZoneId.systemDefault()
        prefsInteractor.getVacationPeriods()
            .mapNotNull { period ->
                val firstDayAfter = period.end.plusDays(1)
                val trigger = firstDayAfter.atStartOfDay(zone).toInstant().toEpochMilli() - prepLead
                if (trigger > now) trigger else null
            }
            .minOrNull()
            ?.let(alarmScheduler::scheduleVacationResume)
    }

    override suspend fun onPreparationDue(eventId: Long, date: String) {
        if (prefsInteractor.isVacationDay(parseDate(date))) return
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
        if (prefsInteractor.isVacationDay(parseDate(date))) return
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
