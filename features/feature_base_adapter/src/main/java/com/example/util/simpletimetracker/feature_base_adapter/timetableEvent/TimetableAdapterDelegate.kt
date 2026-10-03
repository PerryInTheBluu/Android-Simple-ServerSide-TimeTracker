package com.example.util.simpletimetracker.feature_base_adapter.timetableEvent

import android.graphics.drawable.GradientDrawable
import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_base_adapter.R
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemTimetableEventLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.timetableEvent.TimetableViewData as ViewData

fun createTimetableAdapterDelegate() = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        // Rounded color dot identifying the linked activity.
        val dot = itemTimetableEventColor.background as? GradientDrawable
            ?: GradientDrawable().also { itemTimetableEventColor.background = it }
        dot.shape = GradientDrawable.OVAL
        dot.setColor(item.color)
        itemTimetableEventColor.invalidate()

        itemTimetableEventName.text = item.name
        itemTimetableEventTime.text = item.time
        itemTimetableEventRoom.text = item.room
        itemTimetableEventRoom.isVisible = item.room.isNotEmpty()
        itemTimetableEventComment.text = item.comment
        itemTimetableEventComment.isVisible = item.comment.isNotEmpty()

        val (stateText, stateColor) = when (item.state) {
            ViewData.State.ATTENDED -> R.string.timetable_state_attended to R.color.palette_green
            ViewData.State.MISSED -> R.string.timetable_state_missed to R.color.palette_red
            ViewData.State.UPCOMING -> R.string.timetable_state_upcoming to R.color.palette_blue_grey
        }
        itemTimetableEventState.setText(stateText)
        itemTimetableEventState.setTextColor(
            itemTimetableEventState.context.getColor(stateColor),
        )
    }
}
