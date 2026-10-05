package com.example.util.simpletimetracker.feature_todos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.utils.InsetConfiguration
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.hintBig.createHintBigAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.createTimetableTodoAdapterDelegate
import com.example.util.simpletimetracker.feature_dialogs.api.OptionsListDialogListener
import com.example.util.simpletimetracker.feature_dialogs.api.TextInputListener
import com.example.util.simpletimetracker.feature_todos.model.TimetableTodoSubjectAction
import com.example.util.simpletimetracker.feature_todos.viewModel.TodosViewModel
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import com.example.util.simpletimetracker.feature_todos.databinding.TodosFragmentLayoutBinding as Binding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodosFragment : BaseFragment<Binding>(), OptionsListDialogListener, TextInputListener {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    override var insetConfiguration: InsetConfiguration =
        InsetConfiguration.ApplyToView { binding.root }

    private val viewModel: TodosViewModel by viewModels()

    private val todosAdapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createLoaderAdapterDelegate(),
            createHintBigAdapterDelegate(),
            createTimetableTodoAdapterDelegate(viewModel::onTodoClick),
        )
    }

    override fun initUi(): Unit = with(binding) {
        rvTodosList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todosAdapter
        }
        btnTodosAdd.setOnClick(viewModel::onAddTodoClick)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onVisible()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onHidden()
    }

    override fun initViewModel() {
        with(viewModel) {
            todos.observe(todosAdapter::replace)
        }
    }

    override fun onOptionsItemClick(id: OptionsListParams.Item.Id) {
        if (!isResumed) return
        if (id is TimetableTodoSubjectAction) {
            viewModel.onSubjectSelected(id.eventId)
        }
    }

    override fun onTextInput(text: String, tag: String?) {
        if (!isResumed) return
        viewModel.onTodoTextInput(text, tag)
    }

    companion object {
        fun newInstance(): TodosFragment = TodosFragment()
    }
}
