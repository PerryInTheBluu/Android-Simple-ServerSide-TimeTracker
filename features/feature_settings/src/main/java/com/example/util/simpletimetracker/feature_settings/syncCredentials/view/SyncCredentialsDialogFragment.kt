package com.example.util.simpletimetracker.feature_settings.syncCredentials.view

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setFullScreen
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_settings.databinding.SettingsSyncCredentialsFragmentBinding as Binding
import com.example.util.simpletimetracker.feature_settings.syncCredentials.model.SyncCredentialsDialogListener
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsInputType
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncCredentialsDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val params: SyncCredentialsDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = SyncCredentialsDialogParams.Empty,
    )

    private var listeners: List<SyncCredentialsDialogListener> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners()
    }

    override fun initDialog() {
        setSkipCollapsed()
        setFullScreen()
    }

    override fun initUi() = with(binding) {
        val isToken = params.inputType == SyncCredentialsInputType.Token
        tvSettingsSyncCredentialsTitle.setText(
            if (isToken) {
                resourcesR.string.settings_sync_token
            } else {
                resourcesR.string.settings_sync_username
            },
        )
        etSettingsSyncCredentials.inputType = if (isToken) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }
        inputSettingsSyncCredentials.hint = getString(
            if (isToken) {
                resourcesR.string.settings_sync_token_hint
            } else {
                resourcesR.string.settings_sync_username_hint
            },
        )
        etSettingsSyncCredentials.setText(params.initialUsername)
        etSettingsSyncCredentials.setSelection(etSettingsSyncCredentials.text?.length ?: 0)
    }

    override fun initUx() = with(binding) {
        etSettingsSyncCredentials.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSaveClicked()
                true
            } else {
                false
            }
        }
        btnSettingsSyncCredentialsSave.setOnClickListener { onSaveClicked() }
    }

    private fun onSaveClicked() = with(binding) {
        val input = etSettingsSyncCredentials.text?.toString()?.trim().orEmpty()
        inputSettingsSyncCredentials.error = null
        if (params.inputType == SyncCredentialsInputType.Token) {
            listeners.forEach { it.onSyncTokenSaved(input) }
        } else {
            listeners.forEach { it.onSyncUsernameSaved(input) }
        }
        dismiss()
    }

    companion object {

        fun createBundle(data: SyncCredentialsDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
