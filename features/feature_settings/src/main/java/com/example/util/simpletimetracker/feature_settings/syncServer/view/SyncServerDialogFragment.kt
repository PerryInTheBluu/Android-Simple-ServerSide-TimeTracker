package com.example.util.simpletimetracker.feature_settings.syncServer.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setFullScreen
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.data_sync.api.SyncUrlValidator
import com.example.util.simpletimetracker.feature_settings.databinding.SettingsSyncServerFragmentBinding as Binding
import com.example.util.simpletimetracker.feature_settings.syncServer.model.SyncServerDialogListener
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.SyncServerDialogParams
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncServerDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val params: SyncServerDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = SyncServerDialogParams.Empty,
    )

    private var listeners: List<SyncServerDialogListener> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners()
    }

    override fun initDialog() {
        setSkipCollapsed()
        setFullScreen()
    }

    override fun initUi() = with(binding) {
        etSettingsSyncServer.setText(params.initialUrl)
        etSettingsSyncServer.setSelection(etSettingsSyncServer.text?.length ?: 0)
        etSettingsSyncUsername.setText(params.initialUsername)
        etSettingsSyncUsername.setSelection(etSettingsSyncUsername.text?.length ?: 0)
    }

    override fun initUx() = with(binding) {
        etSettingsSyncServer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSaveClicked()
                true
            } else {
                false
            }
        }
        btnSettingsSyncServerSave.setOnClickListener { onSaveClicked() }
    }

    private fun onSaveClicked() = with(binding) {
        val input = etSettingsSyncServer.text?.toString().orEmpty()
        if (input.isBlank()) {
            inputSettingsSyncServer.error = null
            listeners.forEach { it.onSyncServerSaved(url = "", username = "", password = "") }
            dismiss()
            return
        }
        val normalized = SyncUrlValidator.normalizeOrNull(input)
        if (normalized == null) {
            inputSettingsSyncServer.error =
                getString(resourcesR.string.settings_sync_server_invalid)
            return
        }
        inputSettingsSyncServer.error = null
        val username = etSettingsSyncUsername.text?.toString().orEmpty().trim()
        val password = etSettingsSyncPassword.text?.toString().orEmpty()
        listeners.forEach { it.onSyncServerSaved(url = normalized, username = username, password = password) }
        dismiss()
    }

    companion object {
        fun createBundle(data: SyncServerDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
