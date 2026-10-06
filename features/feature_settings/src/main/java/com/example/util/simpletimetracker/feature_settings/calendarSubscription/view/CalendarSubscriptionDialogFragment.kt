package com.example.util.simpletimetracker.feature_settings.calendarSubscription.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.util.simpletimetracker.core.base.BaseBottomSheetFragment
import com.example.util.simpletimetracker.core.extension.findListeners
import com.example.util.simpletimetracker.core.extension.setFullScreen
import com.example.util.simpletimetracker.core.extension.setSkipCollapsed
import com.example.util.simpletimetracker.core.utils.fragmentArgumentDelegate
import com.example.util.simpletimetracker.feature_settings.calendarSubscription.model.CalendarSubscriptionDialogListener
import com.example.util.simpletimetracker.feature_settings.databinding.SettingsCalendarSubscriptionFragmentBinding as Binding
import com.example.util.simpletimetracker.navigation.params.screen.ARGS_PARAMS
import com.example.util.simpletimetracker.navigation.params.screen.CalendarSubscriptionDialogParams
import com.example.util.simpletimetracker.resources.R as resourcesR
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class CalendarSubscriptionDialogFragment : BaseBottomSheetFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    private val params: CalendarSubscriptionDialogParams by fragmentArgumentDelegate(
        key = ARGS_PARAMS,
        default = CalendarSubscriptionDialogParams.Empty,
    )

    private var listeners: List<CalendarSubscriptionDialogListener> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listeners = context.findListeners()
    }

    override fun initDialog() {
        setSkipCollapsed()
        setFullScreen()
    }

    override fun initUi() = with(binding) {
        etCalendarSubscriptionName.setText(params.initialName)
        etCalendarSubscriptionUrl.setText(params.initialUrl)
        cbCalendarSubscriptionEnabled.isChecked = params.isEnabled

        if (params.id.isNotBlank()) {
            btnCalendarSubscriptionDelete.visibility = View.VISIBLE
        } else {
            btnCalendarSubscriptionDelete.visibility = View.GONE
        }
    }

    override fun initUx() = with(binding) {
        etCalendarSubscriptionUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSaveClicked()
                true
            } else {
                false
            }
        }
        btnCalendarSubscriptionSave.setOnClickListener { onSaveClicked() }
        btnCalendarSubscriptionDelete.setOnClickListener { onDeleteClicked() }
    }

    private fun onSaveClicked() = with(binding) {
        val url = etCalendarSubscriptionUrl.text?.toString().orEmpty().trim()
        if (url.isBlank()) {
            inputCalendarSubscriptionUrl.error =
                getString(resourcesR.string.settings_calendar_subscription_invalid_url)
            return
        }

        val isValidScheme = url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("webcal://", ignoreCase = true) ||
            url.startsWith("webdav://", ignoreCase = true) ||
            url.startsWith("webdavs://", ignoreCase = true)

        if (!isValidScheme) {
            inputCalendarSubscriptionUrl.error =
                getString(resourcesR.string.settings_calendar_subscription_invalid_url)
            return
        }
        inputCalendarSubscriptionUrl.error = null

        val name = etCalendarSubscriptionName.text?.toString().orEmpty().trim()
            .ifBlank { "Uni" }
        val id = params.id.ifBlank { UUID.randomUUID().toString() }
        val enabled = cbCalendarSubscriptionEnabled.isChecked

        listeners.forEach {
            it.onCalendarSubscriptionSaved(
                id = id,
                name = name,
                url = url,
                color = params.initialColor,
                enabled = enabled,
            )
        }
        dismiss()
    }

    private fun onDeleteClicked() {
        if (params.id.isNotBlank()) {
            listeners.forEach { it.onCalendarSubscriptionDeleted(params.id) }
        }
        dismiss()
    }

    companion object {
        fun createBundle(data: CalendarSubscriptionDialogParams): Bundle = Bundle().apply {
            putParcelable(ARGS_PARAMS, data)
        }
    }
}
