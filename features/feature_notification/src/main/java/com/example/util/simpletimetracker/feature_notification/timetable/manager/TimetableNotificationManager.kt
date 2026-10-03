package com.example.util.simpletimetracker.feature_notification.timetable.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.utils.PendingIntents
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.feature_notification.recevier.NotificationReceiver
import com.example.util.simpletimetracker.navigation.Router
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val router: Router,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun show(
        tag: String,
        title: String,
        text: String,
        eventId: Long,
        date: String,
        type: TimetableTodo.Type,
    ) {
        createNotificationChannel()
        if (!notificationManager.areNotificationsEnabled()) return

        val startIntent = router.getMainStartIntent().apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            startIntent,
            PendingIntents.getFlags(),
        )
        val doneIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_TIMETABLE_TODO_DONE
                data = "simpletimetracker://timetable-done/$tag".toUri()
                putExtra(NotificationReceiver.EXTRA_TIMETABLE_EVENT_ID, eventId)
                putExtra(NotificationReceiver.EXTRA_TIMETABLE_DATE, date)
                putExtra(NotificationReceiver.EXTRA_TIMETABLE_TODO_TYPE, type.ordinal)
            },
            PendingIntents.getFlags(),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(com.example.util.simpletimetracker.feature_notification.R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.timetable_notification_done), doneIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        notificationManager.notify(tag, NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.timetable_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                null,
            )
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "TIMETABLE"
        private const val NOTIFICATION_ID = 0

        fun notificationTag(
            type: TimetableTodo.Type,
            eventId: Long,
            date: String,
        ): String {
            return "timetable_${type.name.lowercase()}_$eventId" + "_$date"
        }
    }
}
