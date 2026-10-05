package com.example.util.simpletimetracker.feature_records.model

import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import kotlinx.parcelize.Parcelize

/**
 * An action from the long press menu of a timetable slot or an empty
 * calendar area: per date exceptions for a slot or the free day toggle.
 */
@Parcelize
data class TimetableAction(
    val action: Type,
    // yyyy-MM-dd of the displayed day.
    val date: String,
    // Timetable event id, 0 for day level actions.
    val eventId: Long = 0L,
) : OptionsListParams.Item.Id {

    enum class Type {
        // Cancel the slot on this date.
        CANCEL_SLOT,

        // Remove the per date exception of the slot.
        RESTORE_SLOT,

        // Change the times of the slot on this date.
        CHANGE_TIME,

        // Change the room of the slot on this date.
        CHANGE_ROOM,

        // Mark the day as free of lectures.
        ADD_FREE_DAY,

        // Remove the free day mark.
        REMOVE_FREE_DAY,
    }
}
