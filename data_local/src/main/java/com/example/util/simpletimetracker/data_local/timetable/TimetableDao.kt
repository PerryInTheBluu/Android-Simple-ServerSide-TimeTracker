package com.example.util.simpletimetracker.data_local.timetable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimetableEventDao {

    @Query("SELECT * FROM timetableEvents ORDER BY day_of_week, start_time")
    suspend fun getAll(): List<TimetableEventDBO>

    @Query("SELECT * FROM timetableEvents WHERE day_of_week = :dayOfWeek ORDER BY start_time")
    suspend fun getByDay(dayOfWeek: Int): List<TimetableEventDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TimetableEventDBO): Long

    @Query("DELETE FROM timetableEvents WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM timetableEvents")
    suspend fun clear()
}

@Dao
interface TimetableEventOverrideDao {

    @Query("SELECT * FROM timetableEventOverrides WHERE date = :date")
    suspend fun getByDate(date: String): List<TimetableEventOverrideDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TimetableEventOverrideDBO): Long

    @Query("DELETE FROM timetableEventOverrides WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TimetableDayDao {

    @Query("SELECT * FROM timetableDays ORDER BY date")
    suspend fun getAll(): List<TimetableDayDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TimetableDayDBO): Long

    @Query("DELETE FROM timetableDays WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TimetableTodoDao {

    @Query("SELECT * FROM timetableTodos WHERE event_id = :eventId ORDER BY date, id")
    suspend fun getByEvent(eventId: Long): List<TimetableTodoDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TimetableTodoDBO): Long

    @Query("UPDATE timetableTodos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM timetableTodos WHERE id = :id")
    suspend fun delete(id: Long)
}
