package com.example.util.simpletimetracker.feature_notification.recevier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.simpletimetracker.core.extension.goAsync
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_ADD_RECORD
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_AUTOMATIC_BACKUP
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_AUTOMATIC_EXPORT
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_CHANGE_RECORD
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_CREATE_RECORD_TAG
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_QUERY_ACTIVITIES
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_QUERY_RUNNING
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_RESCUE_RECORD
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_RESTART_ACTIVITY
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_START_ACTIVITY
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_STOP_ACTIVITY
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_STOP_ALL_ACTIVITIES
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_STOP_LONGEST_ACTIVITY
import com.example.util.simpletimetracker.core.utils.ACTION_EXTERNAL_STOP_SHORTEST_ACTIVITY
import com.example.util.simpletimetracker.core.utils.EXTRA_ACTIVITY_NAME
import com.example.util.simpletimetracker.core.utils.EXTRA_ANSWER_TYPE
import com.example.util.simpletimetracker.core.utils.EXTRA_FIND_RECORD_MODE
import com.example.util.simpletimetracker.core.utils.EXTRA_FIND_RECORD_WITH_ACTIVITY_NAME
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_COMMENT
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_COMMENT_MODE
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_DURATION_MINUTES
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_MINUTES
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_OFFSET_MINUTES
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_TAG_NAME
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_TIME_ENDED
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_TIME_STARTED
import com.example.util.simpletimetracker.core.utils.EXTRA_RECORD_TYPE_ICON
import com.example.util.simpletimetracker.feature_notification.automaticBackup.controller.AutomaticBackupBroadcastController
import com.example.util.simpletimetracker.feature_notification.automaticExport.controller.AutomaticExportBroadcastController
import com.example.util.simpletimetracker.feature_notification.external.NotificationExternalBroadcastController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExternalNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var externalController: NotificationExternalBroadcastController

    @Inject
    lateinit var automaticBackupController: AutomaticBackupBroadcastController

    @Inject
    lateinit var automaticExportController: AutomaticExportBroadcastController

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in EXTERNAL_ACTIONS) return
        goAsync { handleIntent(intent, action) }
    }

    private suspend fun handleIntent(intent: Intent, action: String) {
        when (action) {
            ACTION_EXTERNAL_AUTOMATIC_BACKUP -> {
                try {
                    automaticBackupController.onReminder()
                } finally {
                    automaticBackupController.onFinished()
                }
            }
            ACTION_EXTERNAL_AUTOMATIC_EXPORT -> {
                try {
                    automaticExportController.onReminder()
                } finally {
                    automaticExportController.onFinished()
                }
            }
            ACTION_EXTERNAL_START_ACTIVITY -> {
                val name = intent.getAnyStringExtra(EXTRA_ACTIVITY_NAME, "activity", "name")
                val comment = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT, "comment")
                val tagNames = intent.getAnyStringExtra(EXTRA_RECORD_TAG_NAME, "tag", "tags")
                    ?.splitTagNames().orEmpty()
                val timeStarted = intent.getAnyStringExtra(EXTRA_RECORD_TIME_STARTED, "time_started", "started")
                val offsetMinutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_OFFSET_MINUTES,
                    "extra_offset_minutes",
                    "offset_minutes",
                    "offset",
                    EXTRA_RECORD_MINUTES,
                    "minutes",
                )
                externalController.onActionExternalActivityStart(
                    name = name,
                    comment = comment,
                    tagNames = tagNames,
                    timeStarted = timeStarted,
                    offsetMinutes = offsetMinutes,
                )
            }
            ACTION_EXTERNAL_STOP_ACTIVITY -> {
                val name = intent.getAnyStringExtra(EXTRA_ACTIVITY_NAME, "activity", "name")
                val timeEnded = intent.getAnyStringExtra(EXTRA_RECORD_TIME_ENDED, "time_ended", "ended")
                val offsetMinutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_OFFSET_MINUTES,
                    "extra_offset_minutes",
                    "offset_minutes",
                    "offset",
                )
                val durationMinutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_DURATION_MINUTES,
                    "extra_duration_minutes",
                    "duration_minutes",
                    "duration",
                    EXTRA_RECORD_MINUTES,
                    "minutes",
                )
                externalController.onActionExternalActivityStop(
                    name = name,
                    timeEnded = timeEnded,
                    offsetMinutes = offsetMinutes,
                    durationMinutes = durationMinutes,
                )
            }
            ACTION_EXTERNAL_STOP_ALL_ACTIVITIES -> {
                externalController.onActionExternalActivityStopAll()
            }
            ACTION_EXTERNAL_STOP_SHORTEST_ACTIVITY -> {
                externalController.onActionExternalActivityStopShortest()
            }
            ACTION_EXTERNAL_STOP_LONGEST_ACTIVITY -> {
                externalController.onActionExternalActivityStopLongest()
            }
            ACTION_EXTERNAL_RESTART_ACTIVITY -> {
                val comment = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT, "comment")
                val tagNames = intent.getAnyStringExtra(EXTRA_RECORD_TAG_NAME, "tag", "tags")
                    ?.splitTagNames().orEmpty()
                externalController.onActionExternalActivityRestart(
                    comment = comment,
                    tagNames = tagNames,
                )
            }
            ACTION_EXTERNAL_ADD_RECORD -> {
                val name = intent.getAnyStringExtra(EXTRA_ACTIVITY_NAME, "activity", "name")
                val timeStarted = intent.getAnyStringExtra(EXTRA_RECORD_TIME_STARTED, "time_started", "started")
                val timeEnded = intent.getAnyStringExtra(EXTRA_RECORD_TIME_ENDED, "time_ended", "ended")
                val durationMinutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_DURATION_MINUTES,
                    "extra_duration_minutes",
                    "duration_minutes",
                    "duration",
                    EXTRA_RECORD_MINUTES,
                    "minutes",
                )
                val offsetMinutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_OFFSET_MINUTES,
                    "extra_offset_minutes",
                    "offset_minutes",
                    "offset",
                )
                val comment = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT, "comment")
                val tagNames = intent.getAnyStringExtra(EXTRA_RECORD_TAG_NAME, "tag", "tags")
                    ?.splitTagNames().orEmpty()
                externalController.onActionExternalRecordAdd(
                    name = name,
                    timeStarted = timeStarted,
                    timeEnded = timeEnded,
                    durationMinutes = durationMinutes,
                    offsetMinutes = offsetMinutes,
                    comment = comment,
                    tagNames = tagNames,
                )
            }
            ACTION_EXTERNAL_RESCUE_RECORD -> {
                val name = intent.getAnyStringExtra(EXTRA_ACTIVITY_NAME, "activity", "name")
                val minutes = intent.getAnyLongExtra(
                    EXTRA_RECORD_MINUTES,
                    "minutes",
                    EXTRA_RECORD_DURATION_MINUTES,
                    "extra_duration_minutes",
                    "duration_minutes",
                    "duration",
                )
                val comment = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT, "comment")
                val tagNames = intent.getAnyStringExtra(EXTRA_RECORD_TAG_NAME, "tag", "tags")
                    ?.splitTagNames().orEmpty()
                externalController.onActionExternalRescueRecord(
                    name = name,
                    minutes = minutes,
                    comment = comment,
                    tagNames = tagNames,
                )
            }
            ACTION_EXTERNAL_CHANGE_RECORD -> {
                val findMode = intent.getAnyStringExtra(EXTRA_FIND_RECORD_MODE)
                val name = intent.getAnyStringExtra(EXTRA_FIND_RECORD_WITH_ACTIVITY_NAME, EXTRA_ACTIVITY_NAME)
                val comment = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT, "comment")
                val commentMode = intent.getAnyStringExtra(EXTRA_RECORD_COMMENT_MODE)
                externalController.onActionExternalRecordChange(
                    findMode = findMode,
                    name = name,
                    comment = comment,
                    commentMode = commentMode,
                )
            }
            ACTION_EXTERNAL_CREATE_RECORD_TAG -> {
                val name = intent.getAnyStringExtra(EXTRA_RECORD_TAG_NAME, "tag", "name")
                val icon = intent.getAnyStringExtra(EXTRA_RECORD_TYPE_ICON, "icon")
                externalController.onActionExternalRecordTagAdd(
                    name = name,
                    icon = icon,
                )
            }
            ACTION_EXTERNAL_QUERY_ACTIVITIES -> {
                val answerType = intent.getAnyStringExtra(EXTRA_ANSWER_TYPE)
                externalController.onActionExternalQueryActivities(answerType)
            }
            ACTION_EXTERNAL_QUERY_RUNNING -> {
                val answerType = intent.getAnyStringExtra(EXTRA_ANSWER_TYPE)
                externalController.onActionExternalQueryRunning(answerType)
            }
        }
    }

    private fun Intent.getAnyStringExtra(key: String, vararg aliases: String): String? {
        val keys = listOf(key) + aliases
        val bundle = extras ?: return null
        for (k in keys) {
            val v = bundle.get(k) ?: continue
            when (v) {
                is String -> {
                    val s = v.trim()
                    if (s.isNotEmpty()) return s
                }
                is Number -> return v.toLong().toString()
                else -> {
                    val s = v.toString().trim()
                    if (s.isNotEmpty()) return s
                }
            }
        }
        return null
    }

    private fun Intent.getAnyLongExtra(key: String, vararg aliases: String): Long? {
        val keys = listOf(key) + aliases
        val bundle = extras ?: return null
        for (k in keys) {
            val v = bundle.get(k) ?: continue
            when (v) {
                is Number -> return v.toLong()
                is String -> {
                    val s = v.trim()
                    s.toLongOrNull()?.let { return it }
                    s.toDoubleOrNull()?.let { return it.toLong() }
                }
                else -> {
                    val s = v.toString().trim()
                    s.toLongOrNull()?.let { return it }
                    s.toDoubleOrNull()?.let { return it.toLong() }
                }
            }
        }
        return null
    }

    private fun String.splitTagNames(): List<String> {
        return split(',').map(String::trim)
    }

    private companion object {
        val EXTERNAL_ACTIONS = setOf(
            ACTION_EXTERNAL_START_ACTIVITY,
            ACTION_EXTERNAL_STOP_ACTIVITY,
            ACTION_EXTERNAL_STOP_ALL_ACTIVITIES,
            ACTION_EXTERNAL_STOP_SHORTEST_ACTIVITY,
            ACTION_EXTERNAL_STOP_LONGEST_ACTIVITY,
            ACTION_EXTERNAL_RESTART_ACTIVITY,
            ACTION_EXTERNAL_ADD_RECORD,
            ACTION_EXTERNAL_RESCUE_RECORD,
            ACTION_EXTERNAL_CHANGE_RECORD,
            ACTION_EXTERNAL_CREATE_RECORD_TAG,
            ACTION_EXTERNAL_AUTOMATIC_BACKUP,
            ACTION_EXTERNAL_AUTOMATIC_EXPORT,
            ACTION_EXTERNAL_QUERY_ACTIVITIES,
            ACTION_EXTERNAL_QUERY_RUNNING,
        )
    }
}
