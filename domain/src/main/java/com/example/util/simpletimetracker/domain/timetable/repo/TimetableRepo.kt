package com.example.util.simpletimetracker.domain.timetable.repo

import com.example.util.simpletimetracker.domain.timetable.model.TimetableDay
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo

interface TimetableRepo {

    suspend fun getAllEvents(): List<TimetableEvent>

    suspend fun getEvents(dayOfWeek: Int): List<TimetableEvent>

    suspend fun addEvent(event: TimetableEvent): Long

    suspend fun removeEvent(id: Long)

    suspend fun clearEvents()

    // Clears events, overrides, days and todos.
    suspend fun clearAll()

    suspend fun getOverrides(date: String): List<TimetableEventOverride>

    suspend fun addOverride(override: TimetableEventOverride): Long

    suspend fun removeOverride(id: Long)

    suspend fun getDays(): List<TimetableDay>

    suspend fun addDay(day: TimetableDay): Long

    suspend fun removeDay(id: Long)

    suspend fun getTodos(eventId: Long): List<TimetableTodo>

    suspend fun getAllTodos(): List<TimetableTodo>

    suspend fun addTodo(todo: TimetableTodo): Long

    suspend fun setTodoDone(id: Long, done: Boolean)

    suspend fun removeTodo(id: Long)
}
