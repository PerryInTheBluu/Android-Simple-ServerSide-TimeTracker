package com.example.util.simpletimetracker.feature_notification.timetable.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.util.simpletimetracker.core.utils.PendingIntents
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import java.time.LocalDate
import java.time.ZoneId
import com.example.util.simpletimetracker.feature_notification.core.AlarmManagerController
import com.example.util.simpletimetracker.feature_notification.recevier.NotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TimetableAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManagerController: AlarmManagerController,
) {

    /**
     * Cancels all timetable alarms of the scheduling window; used by the
     * vacation mode to pause the timetable.
     */
    fun cancelAll(
        events: List<TimetableEvent>,
        days: Int,
    ) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (dayOffset in 0..days) {
            val date = today.plusDays(dayOffset.toLong()).toString()
            events.forEach { event ->
                TimetableTodo.Type.entries.forEach { type ->
                    alarmManagerController.cancelSchedule(
                        pendingIntent = getPendingIntent(
                            action = actionOf(type),
                            type = type,
                            eventId = event.id,
                            date = date,
                        ),
                    )
                }
            }
        }
    }

    private fun actionOf(type: TimetableTodo.Type): String {
        return when (type) {
            TimetableTodo.Type.PREPARATION -> NotificationReceiver.ACTION_TIMETABLE_PREP_DUE
            TimetableTodo.Type.FOLLOW_UP -> NotificationReceiver.ACTION_TIMETABLE_FOLLOWUP_DUE
            TimetableTodo.Type.GENERAL -> NotificationReceiver.ACTION_TIMETABLE_PREP_DUE
        }
    }

    fun schedulePreparation(
        eventId: Long,
        date: String,
        triggerTimestamp: Long,
    ) {
        schedule(
            action = NotificationReceiver.ACTION_TIMETABLE_PREP_DUE,
            type = TimetableTodo.Type.PREPARATION,
            eventId = eventId,
            date = date,
            triggerTimestamp = triggerTimestamp,
        )
    }

    fun scheduleFollowUp(
        eventId: Long,
        date: String,
        triggerTimestamp: Long,
    ) {
        schedule(
            action = NotificationReceiver.ACTION_TIMETABLE_FOLLOWUP_DUE,
            type = TimetableTodo.Type.FOLLOW_UP,
            eventId = eventId,
            date = date,
            triggerTimestamp = triggerTimestamp,
        )
    }

    private fun schedule(
        action: String,
        type: TimetableTodo.Type,
        eventId: Long,
        date: String,
        triggerTimestamp: Long,
    ) {
        alarmManagerController.scheduleAtTime(
            timestamp = triggerTimestamp,
            pendingIntent = getPendingIntent(
                action = action,
                type = type,
                eventId = eventId,
                date = date,
            ),
        )
    }

    private fun getPendingIntent(
        action: String,
        type: TimetableTodo.Type,
        eventId: Long,
        date: String,
    ): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            data = getUniqueIntentData(type, eventId, date).toUri()
            putExtra(NotificationReceiver.EXTRA_TIMETABLE_EVENT_ID, eventId)
            putExtra(NotificationReceiver.EXTRA_TIMETABLE_DATE, date)
        }
        return PendingIntent.getBroadcast(
            context,
            0, // Intents are unique because of data.
            intent,
            PendingIntents.getFlags(),
        )
    }

    // Each alarm gets a unique stable Intent.data URI so alarms do not
    // overwrite each other and re-scheduling the same slot is idempotent.
    private fun getUniqueIntentData(
        type: TimetableTodo.Type,
        eventId: Long,
        date: String,
    ): String {
        return "simpletimetracker://timetable/${type.name.lowercase()}/$eventId/$date"
    }
}
