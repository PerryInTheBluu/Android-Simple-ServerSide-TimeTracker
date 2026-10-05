package com.example.util.simpletimetracker.feature_dialogs.textInput.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_base_adapter.BaseRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_base_adapter.commentField.createCommentFieldAdapterDelegate
import com.example.util.simpletimetracker.feature_dialogs.api.TextInputListener
import com.example.util.simpletimetracker.feature_dialogs.viewModel.TextInputViewModel
import com.example.util.simpletimetracker.feature_views.R as ViewsR
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.TextInputDialogParams
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.util.simpletimetracker.feature_dialogs.databinding.TextInputDialogFragmentBinding as Binding

@AndroidEntryPoint
class TextInputDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val viewModel: TextInputViewModel by viewModels()

    @Inject lateinit var resourceRepo: ResourceRepo

    private val adapter: BaseRecyclerAdapter by lazy {
        BaseRecyclerAdapter(
            createCommentFieldAdapterDelegate(
                afterTextChange = viewModel::onTextChange,
                onKeyboardButtonClick = viewModel::onKeyboardButtonClick,
            ),
        )
    }
    private val params: TextInputDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = TextInputDialogParams(),
    )
    private var listeners: List<TextInputListener> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners<TextInputListener>()
    }

    override fun initDialog() {
        setSkipCollapsed()
    }

    override fun initUi() {
        binding.tvTextInputTitle.text = params.title

        binding.rvTextInputList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TextInputDialogFragment.adapter
        }
        adapter.replace(
            listOf(
                CommentFieldViewData(
                    id = 0L, // Only one at the time.
                    text = params.prefill,
                    marginTopDp = 0,
                    marginHorizontal = resourceRepo.getDimenInDp(ViewsR.dimen.edit_screen_margin_horizontal),
                    hint = params.hint,
                    valueType = CommentFieldViewData.ValueType.TextSingleLine,
                ),
            ),
        )
        // The text watcher of the comment field is attached after the
        // prefill is set, so the prefill never reaches the view model
        // through onTextChange; seed it explicitly.
        viewModel.onTextChange(params.prefill)
    }

    override fun initUx(): Unit = with(binding) {
        btnTextInputSave.setOnClick(viewModel::onSaveClick)
    }

    override fun initViewModel(): Unit = with(viewModel) {
        saveEvent.observe(::onSave)
    }

    private fun onSave(data: Pair<String, String?>) {
        listeners.forEach { it.onTextInput(data.first, data.second ?: params.tag) }
    }

    companion object {
        fun createBundle(data: TextInputDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
