package com.example.util.simpletimetracker.data_local.timetable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetableEvents")
data class TimetableEventDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "name")
    val name: String,
    // ISO convention: MONDAY = 1 .. SUNDAY = 7.
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    // Minutes of day.
    @ColumnInfo(name = "start_time")
    val startTime: Int,
    @ColumnInfo(name = "end_time")
    val endTime: Int,
    @ColumnInfo(name = "room")
    val room: String,
    // Lecture 0, exercise 1, tutorium 2.
    @ColumnInfo(name = "type")
    val type: Int,
    @ColumnInfo(name = "comment")
    val comment: String,
    // Optional link to a record type, NULL if not linked.
    @ColumnInfo(name = "activity_type_id")
    val activityTypeId: Long?,
)

@Entity(tableName = "timetableEventOverrides")
data class TimetableEventOverrideDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    // yyyy-MM-dd.
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "room")
    val room: String,
    @ColumnInfo(name = "start_time")
    val startTime: Int,
    @ColumnInfo(name = "end_time")
    val endTime: Int,
    @ColumnInfo(name = "cancelled")
    val cancelled: Boolean,
    @ColumnInfo(name = "note")
    val note: String,
)

@Entity(tableName = "timetableDays")
data class TimetableDayDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    // yyyy-MM-dd.
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "free_day")
    val freeDay: Boolean,
    @ColumnInfo(name = "note")
    val note: String,
)

@Entity(tableName = "timetableTodos")
data class TimetableTodoDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    // yyyy-MM-dd or NULL for weekly todos.
    @ColumnInfo(name = "date")
    val date: String?,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "done")
    val done: Boolean,
    // Preparation 0, follow up 1, general 2.
    @ColumnInfo(name = "type")
    val type: Int,
)
