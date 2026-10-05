package com.example.util.simpletimetracker.feature_dialogs.timetableSlot.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_dialogs.R
import com.example.util.simpletimetracker.feature_dialogs.api.TimetableSlotDialogListener
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.feature_views.extension.visible
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.TimetableSlotDialogParams
import dagger.hilt.android.AndroidEntryPoint
import com.example.util.simpletimetracker.feature_dialogs.databinding.TimetableSlotDialogFragmentBinding as Binding

/**
 * Detail dialog of a timetable slot: slot information, the todos of
 * the event on that date (tap toggles the done state) and an optional
 * button to add the attendance record afterwards.
 */
@AndroidEntryPoint
class TimetableSlotDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val params: TimetableSlotDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = TimetableSlotDialogParams(),
    )

    private var listeners: List<TimetableSlotDialogListener> = emptyList()
    private val todos: MutableList<TimetableSlotDialogParams.Todo> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners<TimetableSlotDialogListener>()
    }

    override fun initDialog() {
        setSkipCollapsed()
    }

    override fun initUi(): Unit = with(binding) {
        tvTimetableSlotTitle.text = params.title
        tvTimetableSlotInfo.text = params.info
        btnTimetableSlotNachtragen.visible = params.canNachtragen
        btnTimetableSlotNachtragen.text = params.btnNachtragen
        todos.clear()
        todos += params.todos
        renderTodos()
    }

    override fun initUx(): Unit = with(binding) {
        btnTimetableSlotNachtragen.setOnClick {
            params.slot?.let { slot -> listeners.forEach { it.onSlotNachtragen(slot) } }
            dismiss()
        }
        btnTimetableSlotOk.setOnClick { dismiss() }
    }

    private fun renderTodos(): Unit = with(binding) {
        llTimetableSlotTodos.removeAllViews()
        tvTimetableSlotTodosTitle.visibility = if (todos.isEmpty()) View.GONE else View.VISIBLE
        todos.forEach { todo ->
            val row = layoutInflater
                .inflate(R.layout.item_timetable_slot_todo, llTimetableSlotTodos, false)
                as CheckedTextView
            row.text = todo.text
            row.isChecked = todo.done
            row.setOnClick { toggleTodo(todo.id) }
            llTimetableSlotTodos.addView(row)
        }
    }

    private fun toggleTodo(todoId: Long) {
        val index = todos.indexOfFirst { it.id == todoId }
        if (index < 0) return
        listeners.forEach { it.onSlotTodoToggle(todoId) }
        todos[index] = todos[index].copy(done = !todos[index].done)
        renderTodos()
    }

    companion object {
        fun createBundle(data: TimetableSlotDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
