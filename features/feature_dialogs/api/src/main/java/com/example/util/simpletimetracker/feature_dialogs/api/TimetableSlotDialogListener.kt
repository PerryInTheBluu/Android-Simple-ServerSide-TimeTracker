package com.example.util.simpletimetracker.feature_dialogs.api

import android.os.Parcelable

interface TimetableSlotDialogListener {

    /** Adds the attendance record of the slot afterwards. */
    fun onSlotNachtragen(slot: Parcelable)

    /** Starts tracking the activity linked to the slot right away. */
    fun onSlotTrackNow(slot: Parcelable)

    /** Toggles the done state of a timetable todo. */
    fun onSlotTodoToggle(todoId: Long)
}
