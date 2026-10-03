package com.example.util.simpletimetracker.data_local.timetable

import com.example.util.simpletimetracker.data_local.base.withLockedCache
import com.example.util.simpletimetracker.domain.timetable.model.TimetableDay
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class TimetableRepoImpl @Inject constructor(
    private val eventDao: TimetableEventDao,
    private val overrideDao: TimetableEventOverrideDao,
    private val dayDao: TimetableDayDao,
    private val todoDao: TimetableTodoDao,
    private val mapper: TimetableDataLocalMapper,
) : TimetableRepo {

    private var eventsCache: List<TimetableEvent>? = null
    private var daysCache: List<TimetableDay>? = null
    private val mutex: Mutex = Mutex()

    override suspend fun getAllEvents(): List<TimetableEvent> = mutex.withLockedCache(
        logMessage = "getAllEvents",
        accessCache = { eventsCache },
        accessSource = { eventDao.getAll().map(mapper::map) },
        afterSourceAccess = { eventsCache = it },
    )

    override suspend fun getEvents(dayOfWeek: Int): List<TimetableEvent> {
        return getAllEvents().filter { it.dayOfWeek == dayOfWeek }
    }

    override suspend fun addEvent(event: TimetableEvent): Long = mutex.withLockedCache(
        logMessage = "addEvent",
        accessSource = { eventDao.insert(event.let(mapper::map)) },
        afterSourceAccess = { eventsCache = null },
    )

    override suspend fun removeEvent(id: Long) = mutex.withLockedCache(
        logMessage = "removeEvent",
        accessSource = {
            eventDao.delete(id)
            todoDao.getByEvent(id).forEach { todoDao.delete(it.id) }
        },
        afterSourceAccess = { eventsCache = null },
    )

    override suspend fun clearEvents() = mutex.withLockedCache(
        logMessage = "clearEvents",
        accessSource = { eventDao.clear() },
        afterSourceAccess = { eventsCache = null },
    )

    override suspend fun clearAll() = mutex.withLockedCache(
        logMessage = "clearAll",
        accessSource = {
            eventDao.clear()
            overrideDao.clear()
            dayDao.clear()
            todoDao.clear()
        },
        afterSourceAccess = {
            eventsCache = null
            daysCache = null
        },
    )

    override suspend fun getOverrides(date: String): List<TimetableEventOverride> = mutex.withLockedCache(
        logMessage = "getOverrides",
        accessSource = { overrideDao.getByDate(date).map(mapper::map) },
    )

    override suspend fun addOverride(override: TimetableEventOverride): Long = mutex.withLockedCache(
        logMessage = "addOverride",
        accessSource = { overrideDao.insert(override.let(mapper::map)) },
    )

    override suspend fun removeOverride(id: Long) = mutex.withLockedCache(
        logMessage = "removeOverride",
        accessSource = { overrideDao.delete(id) },
    )

    override suspend fun getDays(): List<TimetableDay> = mutex.withLockedCache(
        logMessage = "getDays",
        accessCache = { daysCache },
        accessSource = { dayDao.getAll().map(mapper::map) },
        afterSourceAccess = { daysCache = it },
    )

    override suspend fun addDay(day: TimetableDay): Long = mutex.withLockedCache(
        logMessage = "addDay",
        accessSource = { dayDao.insert(day.let(mapper::map)) },
        afterSourceAccess = { daysCache = null },
    )

    override suspend fun removeDay(id: Long) = mutex.withLockedCache(
        logMessage = "removeDay",
        accessSource = { dayDao.delete(id) },
        afterSourceAccess = { daysCache = null },
    )

    override suspend fun getTodos(eventId: Long): List<TimetableTodo> = mutex.withLockedCache(
        logMessage = "getTodos",
        accessSource = { todoDao.getByEvent(eventId).map(mapper::map) },
    )

    override suspend fun addTodo(todo: TimetableTodo): Long = mutex.withLockedCache(
        logMessage = "addTodo",
        accessSource = { todoDao.insert(todo.let(mapper::map)) },
    )

    override suspend fun setTodoDone(id: Long, done: Boolean) = mutex.withLockedCache(
        logMessage = "setTodoDone",
        accessSource = { todoDao.setDone(id, done) },
    )

    override suspend fun removeTodo(id: Long) = mutex.withLockedCache(
        logMessage = "removeTodo",
        accessSource = { todoDao.delete(id) },
    )
}
