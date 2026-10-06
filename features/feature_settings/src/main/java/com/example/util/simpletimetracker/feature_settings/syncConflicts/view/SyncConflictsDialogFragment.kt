package com.example.util.simpletimetracker.feature_settings.syncConflicts.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setFullScreen
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_settings.databinding.ItemSyncConflictBinding
import com.example.util.simpletimetracker.feature_settings.databinding.SettingsSyncConflictsFragmentBinding as Binding
import com.example.util.simpletimetracker.feature_settings.syncConflicts.model.SyncConflictsDialogListener
import com.example.util.simpletimetracker.feature_settings.syncConflicts.viewModel.SyncConflictItemViewData
import com.example.util.simpletimetracker.feature_settings.syncConflicts.viewModel.SyncConflictsViewModel
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.SyncConflictsDialogParams
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncConflictsDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val viewModel: SyncConflictsViewModel by viewModels()

    private val params: SyncConflictsDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = SyncConflictsDialogParams.Empty,
    )

    private var listeners: List<SyncConflictsDialogListener> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners()
    }

    override fun initDialog() {
        setSkipCollapsed()
        setFullScreen()
    }

    override fun initUi() {
        viewModel.load()
    }

    override fun initUx() = with(binding) {
        btnSyncConflictsClose.setOnClickListener {
            dismiss()
        }
        btnSyncConflictsClear.setOnClickListener {
            viewModel.clear {
                Toast.makeText(
                    requireContext(),
                    getString(resourcesR.string.settings_sync_conflicts_cleared_toast),
                    Toast.LENGTH_SHORT,
                ).show()
                listeners.forEach { it.onSyncConflictsCleared() }
            }
        }
    }

    override fun initViewModel() {
        viewModel.conflicts.observe(viewLifecycleOwner, ::bindConflicts)
    }

    private fun bindConflicts(conflicts: List<SyncConflictItemViewData>) = with(binding) {
        llSyncConflictsList.removeAllViews()

        if (conflicts.isEmpty()) {
            tvSyncConflictsEmpty.visibility = View.VISIBLE
            btnSyncConflictsClear.visibility = View.GONE
        } else {
            tvSyncConflictsEmpty.visibility = View.GONE
            btnSyncConflictsClear.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(requireContext())
            conflicts.forEach { conflict ->
                val itemBinding = ItemSyncConflictBinding.inflate(inflater, llSyncConflictsList, false)
                itemBinding.tvConflictTime.text = conflict.time
                itemBinding.tvConflictEntity.text = conflict.entity
                itemBinding.tvConflictResolution.text = conflict.resolution
                if (conflict.detail.isNotBlank()) {
                    itemBinding.tvConflictDetail.visibility = View.VISIBLE
                    itemBinding.tvConflictDetail.text = conflict.detail
                } else {
                    itemBinding.tvConflictDetail.visibility = View.GONE
                }
                itemBinding.tvConflictId.text = conflict.id
                llSyncConflictsList.addView(itemBinding.root)
            }
        }
    }

    companion object {
        fun createBundle(params: SyncConflictsDialogParams): Bundle =
            Bundle().apply {
                putParcelable(ARGS_PARAMS, params)
            }
    }
}
