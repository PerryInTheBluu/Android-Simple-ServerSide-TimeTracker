package com.example.util.simpletimetracker.feature_todos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.utils.InsetConfiguration
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.hintBig.createHintBigAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.loader.createLoaderAdapterDelegate
import com.example.util.simpletimetracker.feature_base_adapter.timetableTodo.createTimetableTodoAdapterDelegate
import com.example.util.simpletimetracker.feature_todos.viewModel.TodosViewModel
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.feature_todos.databinding.TodosFragmentLayoutBinding as Binding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodosFragment : BaseFragment<Binding>() {

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

    companion object {
        fun newInstance(): TodosFragment = TodosFragment()
    }
}
