package com.example.util.simpletimetracker.feature_base_adapter.timetableEvent

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

/**
 * A timetable slot shown between the records of a day: shows the name,
 * time, room and whether the user attended it.
 */
data class TimetableViewData(
    val id: Long,
    val name: String,
    val time: String,
    val room: String,
    val comment: String,
    val state: State,
    val color: Int,
) : ViewHolderType {

    enum class State {
        UPCOMING,
        ATTENDED,
        MISSED,
    }

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is TimetableViewData
}
