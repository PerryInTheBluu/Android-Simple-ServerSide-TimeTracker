package com.example.util.simpletimetracker.feature_settings.syncSettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.data_sync.BuildConfig
import com.example.util.simpletimetracker.data_sync.api.ServerUrlValidator
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.domain.statistics.interactor.SettingsDataUpdateInteractor
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val credentialStore: SyncCredentialStore,
    private val settingsDataUpdateInteractor: SettingsDataUpdateInteractor,
) : BaseViewModel() {

    lateinit var extra: SyncSettingsDialogParams

    val value: LiveData<String> = MutableLiveData()
    val errorVisible: LiveData<Boolean> = MutableLiveData(false)
    val dismiss: LiveData<Unit> = MutableLiveData()

    fun onStart() {
        value.set(
            when (extra.field) {
                SyncSettingsField.SERVER -> credentialStore.serverUrl
                SyncSettingsField.USERNAME -> credentialStore.username
                SyncSettingsField.TOKEN -> ""
            },
        )
    }

    fun onValueChange(text: String) {
        value.set(text)
        errorVisible.set(false)
    }

    fun onSaveClick() = viewModelScope.launch {
        val newValue = value.value.orEmpty()
        val valid = when (extra.field) {
            SyncSettingsField.SERVER -> ServerUrlValidator.normalize(
                raw = newValue,
                allowHttp = BuildConfig.ALLOW_HTTP_SERVER_URL,
            ).also { normalized ->
                if (normalized != null) credentialStore.serverUrl = normalized
            } != null
            SyncSettingsField.USERNAME -> {
                credentialStore.username = newValue.trim()
                true
            }
            SyncSettingsField.TOKEN -> {
                val token = newValue.trim()
                if (token.isNotEmpty()) {
                    credentialStore.apiToken = token
                    true
                } else {
                    false
                }
            }
        }

        if (valid) {
            settingsDataUpdateInteractor.send()
            dismiss.set(Unit)
        } else {
            errorVisible.set(true)
        }
    }
}
