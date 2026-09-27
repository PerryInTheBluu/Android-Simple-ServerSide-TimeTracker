package com.example.util.simpletimetracker.feature_settings.syncSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.observeOnce
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClick
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsField
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.AndroidEntryPoint
import com.example.util.simpletimetracker.feature_settings.databinding.SyncSettingsInputFragmentBinding as Binding

@AndroidEntryPoint
class SyncSettingsDialogFragment :
    BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val viewModel: SyncSettingsViewModel by viewModels()

    private val params: SyncSettingsDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = SyncSettingsDialogParams(),
    )

    override fun initDialog() {
        setSkipCollapsed()
    }

    override fun initUi() {
        binding.tvSyncSettingsInputTitle.setText(titleRes)
    }

    override fun initUx(): Unit = with(binding) {
        etSyncSettingsValue.doAfterTextChanged { viewModel.onValueChange(it.toString()) }
        btnSyncSettingsSave.setOnClick(viewModel::onSaveClick)
    }

    override fun initViewModel(): Unit = with(viewModel) {
        extra = params
        onStart()
        value.observe { text ->
            if (etSyncSettingsValue.text?.toString() != text) {
                etSyncSettingsValue.setText(text)
            }
        }
        errorVisible.observe {
            tvSyncSettingsValidationError.visibility = if (it) View.VISIBLE else View.GONE
        }
        dismiss.observeOnce(viewLifecycleOwner) { dismissAllowingStateLoss() }
    }

    private val titleRes: Int
        get() = when (params.field) {
            SyncSettingsField.SERVER -> resourcesR.string.settings_sync_server
            SyncSettingsField.USERNAME -> resourcesR.string.settings_sync_username
            SyncSettingsField.TOKEN -> resourcesR.string.settings_sync_token
        }

    companion object {
        fun createBundle(data: SyncSettingsDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
