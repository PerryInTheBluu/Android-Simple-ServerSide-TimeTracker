package com.example.util.simpletimetracker.data_local.timetable

import com.example.util.simpletimetracker.domain.timetable.model.TimetableDay
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import javax.inject.Inject

class TimetableDataLocalMapper @Inject constructor() {

    fun map(dbo: TimetableEventDBO): TimetableEvent = TimetableEvent(
        id = dbo.id,
        name = dbo.name,
        dayOfWeek = dbo.dayOfWeek,
        startTime = dbo.startTime,
        endTime = dbo.endTime,
        room = dbo.room,
        type = mapEventType(dbo.type),
        comment = dbo.comment,
        activityTypeId = dbo.activityTypeId,
    )

    fun map(item: TimetableEvent): TimetableEventDBO = TimetableEventDBO(
        id = item.id,
        name = item.name,
        dayOfWeek = item.dayOfWeek,
        startTime = item.startTime,
        endTime = item.endTime,
        room = item.room,
        type = mapEventType(item.type),
        comment = item.comment,
        activityTypeId = item.activityTypeId,
    )

    fun map(dbo: TimetableEventOverrideDBO): TimetableEventOverride = TimetableEventOverride(
        id = dbo.id,
        date = dbo.date,
        eventId = dbo.eventId,
        room = dbo.room,
        startTime = dbo.startTime,
        endTime = dbo.endTime,
        cancelled = dbo.cancelled,
        note = dbo.note,
    )

    fun map(item: TimetableEventOverride): TimetableEventOverrideDBO = TimetableEventOverrideDBO(
        id = item.id,
        date = item.date,
        eventId = item.eventId,
        room = item.room,
        startTime = item.startTime,
        endTime = item.endTime,
        cancelled = item.cancelled,
        note = item.note,
    )

    fun map(dbo: TimetableDayDBO): TimetableDay = TimetableDay(
        id = dbo.id,
        date = dbo.date,
        freeDay = dbo.freeDay,
        note = dbo.note,
    )

    fun map(item: TimetableDay): TimetableDayDBO = TimetableDayDBO(
        id = item.id,
        date = item.date,
        freeDay = item.freeDay,
        note = item.note,
    )

    fun map(dbo: TimetableTodoDBO): TimetableTodo = TimetableTodo(
        id = dbo.id,
        eventId = dbo.eventId,
        date = dbo.date,
        text = dbo.text,
        done = dbo.done,
        type = mapTodoType(dbo.type),
    )

    fun map(item: TimetableTodo): TimetableTodoDBO = TimetableTodoDBO(
        id = item.id,
        eventId = item.eventId,
        date = item.date,
        text = item.text,
        done = item.done,
        type = mapTodoType(item.type),
    )

    private fun mapEventType(code: Int): TimetableEvent.Type = when (code) {
        EVENT_TYPE_LECTURE -> TimetableEvent.Type.LECTURE
        EVENT_TYPE_EXERCISE -> TimetableEvent.Type.EXERCISE
        else -> TimetableEvent.Type.TUTORIUM
    }

    private fun mapEventType(type: TimetableEvent.Type): Int = when (type) {
        TimetableEvent.Type.LECTURE -> EVENT_TYPE_LECTURE
        TimetableEvent.Type.EXERCISE -> EVENT_TYPE_EXERCISE
        TimetableEvent.Type.TUTORIUM -> EVENT_TYPE_TUTORIUM
    }

    private fun mapTodoType(code: Int): TimetableTodo.Type = when (code) {
        TODO_TYPE_PREPARATION -> TimetableTodo.Type.PREPARATION
        TODO_TYPE_FOLLOW_UP -> TimetableTodo.Type.FOLLOW_UP
        else -> TimetableTodo.Type.GENERAL
    }

    private fun mapTodoType(type: TimetableTodo.Type): Int = when (type) {
        TimetableTodo.Type.PREPARATION -> TODO_TYPE_PREPARATION
        TimetableTodo.Type.FOLLOW_UP -> TODO_TYPE_FOLLOW_UP
        TimetableTodo.Type.GENERAL -> TODO_TYPE_GENERAL
    }

    companion object {
        private const val EVENT_TYPE_LECTURE = 0
        private const val EVENT_TYPE_EXERCISE = 1
        private const val EVENT_TYPE_TUTORIUM = 2
        private const val TODO_TYPE_PREPARATION = 0
        private const val TODO_TYPE_FOLLOW_UP = 1
        private const val TODO_TYPE_GENERAL = 2
    }
}
