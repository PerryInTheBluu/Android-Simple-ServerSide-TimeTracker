package com.example.util.simpletimetracker.feature_todos.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.hintBig.HintBigViewData
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.TimetableTodoViewData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Builds the todos tab content: open preparation and general todos and
 * all follow-up todos. Done preparations disappear, done follow-ups stay
 * visible with a set checkmark.
 */
class TodosViewDataInteractor @Inject constructor(
    private val timetableRepo: TimetableRepo,
    private val resourceRepo: ResourceRepo,
) {

    suspend fun getViewData(): List<ViewHolderType> {
        val todos = timetableRepo.getAllTodos()
        if (todos.isEmpty()) return listOf(mapToEmpty())

        val events = timetableRepo.getAllEvents().associateBy { it.id }

        val items = todos
            .filter { todo ->
                when (todo.type) {
                    TimetableTodo.Type.PREPARATION,
                    TimetableTodo.Type.GENERAL,
                    -> !todo.done
                    TimetableTodo.Type.FOLLOW_UP -> true
                }
            }
            .sortedWith(
                compareBy<TimetableTodo> { it.date.orEmpty() }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.id },
            )
            .mapNotNull { todo ->
                val event = events[todo.eventId] ?: return@mapNotNull null
                TimetableTodoViewData(
                    id = todo.id,
                    name = event.name,
                    dateText = todo.date?.let(::formatDate).orEmpty(),
                    typeLabel = typeLabel(todo.type),
                    done = todo.done,
                )
            }

        return items.ifEmpty { listOf(mapToEmpty()) }
    }

    suspend fun setDone(todoId: Long, done: Boolean) {
        timetableRepo.setTodoDone(todoId, done)
        LocalDataChangedBus.publish()
    }

    private fun typeLabel(type: TimetableTodo.Type): String {
        return when (type) {
            TimetableTodo.Type.PREPARATION -> resourceRepo.getString(R.string.timetable_todo_preparation)
            TimetableTodo.Type.FOLLOW_UP -> resourceRepo.getString(R.string.timetable_todo_follow_up)
            TimetableTodo.Type.GENERAL -> resourceRepo.getString(R.string.timetable_todo_general)
        }
    }

    private fun formatDate(date: String): String {
        return runCatching {
            LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EE, d.M.", Locale.getDefault()))
        }.getOrDefault(date)
    }

    private fun mapToEmpty(): HintBigViewData {
        return HintBigViewData(
            text = resourceRepo.getString(R.string.todos_empty_hint),
            infoIconVisible = true,
            closeIconVisible = false,
        )
    }
}
