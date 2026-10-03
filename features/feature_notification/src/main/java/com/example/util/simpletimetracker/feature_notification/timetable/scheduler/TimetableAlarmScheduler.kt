package com.example.util.simpletimetracker.feature_notification.timetable.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.util.simpletimetracker.core.utils.PendingIntents
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.feature_notification.core.AlarmManagerController
import com.example.util.simpletimetracker.feature_notification.recevier.NotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TimetableAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManagerController: AlarmManagerController,
) {

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
