package com.example.util.simpletimetracker.feature_base_adapter.timetableTodo

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

/**
 * A timetable todo row on the todos tab: preparation and follow-up
 * questions per date with a checkmark to mark them done.
 */
data class TimetableTodoViewData(
    val id: Long,
    val name: String,
    val dateText: String,
    // PREPARATION, FOLLOW_UP or GENERAL.
    val typeLabel: String,
    val done: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is TimetableTodoViewData
}
