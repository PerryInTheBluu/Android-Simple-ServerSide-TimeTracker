package com.example.util.simpletimetracker.feature_todos.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.TimetableTodoViewData
import com.example.util.simpletimetracker.feature_todos.interactor.TodosViewDataInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TodosViewModel @Inject constructor(
    private val todosViewDataInteractor: TodosViewDataInteractor,
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

    private fun update() {
        viewModelScope.launch {
            todos.set(todosViewDataInteractor.getViewData())
        }
    }
}
