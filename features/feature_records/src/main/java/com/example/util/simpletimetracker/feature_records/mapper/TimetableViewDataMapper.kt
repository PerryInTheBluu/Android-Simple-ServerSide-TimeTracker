package com.example.util.simpletimetracker.feature_records.mapper

import com.example.util.simpletimetracker.feature_base_adapter.timetableEvent.TimetableViewData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Maps timetable slots of a day into cards for the day view, including
 * the attendance state derived from the day's records.
 */
class TimetableViewDataMapper @Inject constructor() {

    fun map(
        eventId: Long,
        name: String,
        room: String,
        comment: String,
        slotStart: Long,
        slotEnd: Long,
        attended: Boolean,
        color: Int,
        useMilitaryTime: Boolean,
        now: Long,
    ): TimetableViewData {
        return TimetableViewData(
            id = eventId,
            name = name,
            time = formatRange(slotStart, slotEnd, useMilitaryTime),
            room = room,
            comment = comment,
            state = when {
                attended -> TimetableViewData.State.ATTENDED
                now > slotEnd -> TimetableViewData.State.MISSED
                else -> TimetableViewData.State.UPCOMING
            },
            color = color,
        )
    }

    /**
     * Local midnight of the day the timestamp belongs to; timetable
     * slots are minutes from this midnight.
     */
    fun midnightOf(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Converts minute of day (0..1439) on the day of the given midnight into an absolute
     * timestamp, taking daylight saving time (DST) transitions into account.
     */
    fun timestampOf(midnight: Long, minutesOfDay: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = midnight
        calendar.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        calendar.set(Calendar.MINUTE, minutesOfDay % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun dateString(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    }

    /**
     * ISO day of week (MONDAY = 1 .. SUNDAY = 7) of the timestamp.
     */
    fun isoDayOfWeek(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    }

    fun formatRange(
        start: Long,
        end: Long,
        useMilitaryTime: Boolean,
    ): String {
        val format = if (useMilitaryTime) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        return "${format.format(Date(start))} – ${format.format(Date(end))}"
    }
}
