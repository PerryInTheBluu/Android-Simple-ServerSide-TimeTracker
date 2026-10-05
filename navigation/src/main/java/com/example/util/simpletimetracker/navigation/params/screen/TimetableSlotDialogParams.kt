package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Detail dialog of a timetable slot: information about the slot, the
 * todos of the event on that date and an optional button to add the
 * attendance record afterwards. The slot itself is passed as the
 * generic parcelable so this module stays independent of the
 * feature module that defines the slot.
 */
@Parcelize
data class TimetableSlotDialogParams(
    val slot: Parcelable? = null,
    val title: String = "",
    val info: String = "",
    val canNachtragen: Boolean = false,
    val btnNachtragen: String = "",
    val canTrackNow: Boolean = false,
    val btnTrackNow: String = "",
    val todos: List<Todo> = emptyList(),
) : Parcelable, ScreenParams {

    @Parcelize
    data class Todo(
        val id: Long,
        val text: String,
        val done: Boolean,
    ) : Parcelable
}
