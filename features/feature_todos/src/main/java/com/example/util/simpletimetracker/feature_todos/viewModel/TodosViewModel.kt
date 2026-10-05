package com.example.util.simpletimetracker.feature_todos.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.TimetableTodoViewData
import com.example.util.simpletimetracker.feature_todos.interactor.TodosViewDataInteractor
import com.example.util.simpletimetracker.feature_todos.model.TimetableTodoSubjectAction
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import com.example.util.simpletimetracker.navigation.params.screen.TextInputDialogParams
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TodosViewModel @Inject constructor(
    private val todosViewDataInteractor: TodosViewDataInteractor,
    private val timetableRepo: TimetableRepo,
    private val resourceRepo: ResourceRepo,
    private val router: Router,
) : ViewModel() {

    val todos: LiveData<List<ViewHolderType>> by lazy {
        MutableLiveData<List<ViewHolderType>>()
    }

    private var isVisible: Boolean = false

    fun onVisible() {
        isVisible = true
        update()
    }

    fun onHidden() {
        isVisible = false
    }

    fun onTodoClick(item: TimetableTodoViewData) {
        viewModelScope.launch {
            todosViewDataInteractor.setDone(item.id, !item.done)
            if (isVisible) update()
        }
    }

    // Opens the subject selection of the add todo flow; subjects are the
    // distinct names of the timetable events.
    fun onAddTodoClick() = viewModelScope.launch {
        val subjects = timetableRepo.getAllEvents()
            .distinctBy { it.name }
            .sortedBy { it.name }
        if (subjects.isEmpty()) return@launch
        val items = subjects.map { event ->
            OptionsListParams.Item(
                id = TimetableTodoSubjectAction(eventId = event.id),
                text = event.name,
                icon = null,
            )
        }
        router.navigate(OptionsListParams(items))
    }

    fun onSubjectSelected(eventId: Long) {
        router.navigate(
            TextInputDialogParams(
                tag = TAG_TODO_EVENT + eventId,
                title = resourceRepo.getString(R.string.todos_add_title),
                hint = resourceRepo.getString(R.string.todos_add_hint),
            ),
        )
    }

    fun onTodoTextInput(text: String, tag: String?) = viewModelScope.launch {
        if (!tag.orEmpty().startsWith(TAG_TODO_EVENT)) return@launch
        val eventId = tag.orEmpty().removePrefix(TAG_TODO_EVENT).toLongOrNull() ?: return@launch
        todosViewDataInteractor.addGeneralTodo(eventId, text)
        if (isVisible) update()
    }

    private fun update() {
        viewModelScope.launch {
            todos.set(todosViewDataInteractor.getViewData())
        }
    }

    companion object {
        private const val TAG_TODO_EVENT = "TODOS_EVENT_"
    }
}
