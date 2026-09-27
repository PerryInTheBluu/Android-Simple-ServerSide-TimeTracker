package com.example.util.simpletimetracker.core.utils

// Same values written in manifest.
const val SHORTCUT_NAVIGATION_KEY = "shortcutNavigationTab"
const val SHORTCUT_NAVIGATION_RECORDS = "recordsTab"
const val SHORTCUT_NAVIGATION_STATISTICS = "statisticsTab"
const val SHORTCUT_NAVIGATION_SETTINGS = "settingsTab"

// Same values written in manifest.
const val ACTION_EXTERNAL_START_ACTIVITY = "de.piusdischinger.timetracker.ACTION_START_ACTIVITY"
const val ACTION_EXTERNAL_STOP_ACTIVITY = "de.piusdischinger.timetracker.ACTION_STOP_ACTIVITY"
const val ACTION_EXTERNAL_STOP_ALL_ACTIVITIES = "de.piusdischinger.timetracker.ACTION_STOP_ALL_ACTIVITIES"
const val ACTION_EXTERNAL_STOP_SHORTEST_ACTIVITY = "de.piusdischinger.timetracker.ACTION_STOP_SHORTEST_ACTIVITY"
const val ACTION_EXTERNAL_STOP_LONGEST_ACTIVITY = "de.piusdischinger.timetracker.ACTION_STOP_LONGEST_ACTIVITY"
const val ACTION_EXTERNAL_RESTART_ACTIVITY = "de.piusdischinger.timetracker.ACTION_RESTART_ACTIVITY"
const val ACTION_EXTERNAL_ADD_RECORD = "de.piusdischinger.timetracker.ACTION_ADD_RECORD"
const val ACTION_EXTERNAL_CHANGE_RECORD = "de.piusdischinger.timetracker.ACTION_CHANGE_RECORD"
const val ACTION_EXTERNAL_CREATE_RECORD_TAG = "de.piusdischinger.timetracker.ACTION_CREATE_TAG"
const val ACTION_EXTERNAL_AUTOMATIC_BACKUP = "de.piusdischinger.timetracker.ACTION_EXTERNAL_AUTOMATIC_BACKUP"
const val ACTION_EXTERNAL_AUTOMATIC_EXPORT = "de.piusdischinger.timetracker.ACTION_EXTERNAL_AUTOMATIC_EXPORT"
const val ACTION_EXTERNAL_QUERY_ACTIVITIES = "de.piusdischinger.timetracker.ACTION_QUERY_ACTIVITIES"
const val ACTION_EXTERNAL_QUERY_RUNNING = "de.piusdischinger.timetracker.ACTION_QUERY_RUNNING"

// Sent by the app in response to ACTION_EXTERNAL_QUERY_ACTIVITIES / ACTION_EXTERNAL_QUERY_RUNNING.
const val ACTION_EXTERNAL_RESPONSE_ACTIVITIES = "de.piusdischinger.timetracker.ACTION_RESPONSE_ACTIVITIES"
const val ACTION_EXTERNAL_RESPONSE_RUNNING = "de.piusdischinger.timetracker.ACTION_RESPONSE_RUNNING"

const val EVENT_STARTED_ACTIVITY = "de.piusdischinger.timetracker.EVENT_STARTED_ACTIVITY"
const val EVENT_STOPPED_ACTIVITY = "de.piusdischinger.timetracker.EVENT_STOPPED_ACTIVITY"
const val EVENT_COMPLETED_GOAL = "de.piusdischinger.timetracker.EVENT_COMPLETED_GOAL"

const val EXTRA_ACTIVITY_NAME = "extra_activity_name"
const val EXTRA_CATEGORY_NAME = "extra_category_name"
const val EXTRA_RECORD_COMMENT = "extra_record_comment"
const val EXTRA_RECORD_TAG_NAME = "extra_record_tag"
const val EXTRA_RECORD_TYPE_NOTE = "extra_record_type_note"
const val EXTRA_RECORD_TYPE_ICON = "extra_record_type_icon"
const val EXTRA_RECORD_TIME_STARTED = "extra_record_time_started"
const val EXTRA_RECORD_TIME_ENDED = "extra_record_time_ended"
const val EXTRA_RECORD_COMMENT_MODE = "extra_record_comment_mode" // set, append, prefix
const val EXTRA_FIND_RECORD_MODE = "extra_find_record_mode" // current_or_last, current, last
const val EXTRA_FIND_RECORD_WITH_ACTIVITY_NAME = "extra_find_record_with_activity_name"
const val EXTRA_GOAL_TYPE = "extra_goal_type" // duration, count
const val EXTRA_GOAL_VALUE = "extra_goal_value"
const val EXTRA_ANSWER_TYPE = "extra_answer_type" // simple, json
const val EXTRA_DATA = "data"

const val DELAY_DATA_LOAD_MS = 300L // Same as @integer/screen_animation_time