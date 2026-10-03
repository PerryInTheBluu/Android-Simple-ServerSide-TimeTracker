package com.example.util.simpletimetracker.feature_base_adapter.timetableTodo

import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemTimetableTodoLayoutBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.TimetableTodoViewData as ViewData

fun createTimetableTodoAdapterDelegate(
    onCheckClick: (ViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        itemTimetableTodoName.text = item.name
        itemTimetableTodoDate.text = item.dateText
        itemTimetableTodoType.text = item.typeLabel
        itemTimetableTodoCheck.isChecked = item.done
        itemTimetableTodoCheck.jumpDrawablesToCurrentState()

        // The checkbox itself toggles on click; undo that and let the
        // view model write the state and re-render the list.
        itemTimetableTodoCheck.setOnClickListener {
            itemTimetableTodoCheck.isChecked = item.done
            onCheckClick(item)
        }
        root.setOnClickListener { onCheckClick(item) }
    }
}
