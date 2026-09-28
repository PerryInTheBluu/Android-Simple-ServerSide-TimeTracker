package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsSyncViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsInputType
import com.example.util.simpletimetracker.navigation.params.screen.SyncServerDialogParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsSyncViewModelDelegate @Inject constructor(
    private val router: Router,
    private val settingsSyncViewDataInteractor: SettingsSyncViewDataInteractor,
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
    private val credentialStore: SyncCredentialStore,
) : SettingsDelegate, ViewModelDelegate() {

    private var parent: SettingsParent? = null

    override fun init(parent: SettingsParent) {
        this.parent = parent
    }

    override suspend fun getViewData(): SettingsDelegate.ViewData {
        return SettingsDelegate.ViewData(
            key = Companion,
            data = settingsSyncViewDataInteractor.execute(syncEngine.status.value),
        )
    }

    override fun onBlockClicked(block: SettingsBlock) {
        when (block) {
            SettingsBlock.SyncNow -> {
                syncScheduler.syncNow()
                delegateScope.launch {
                    parent?.updateContent()
                }
            }
            SettingsBlock.SyncConflicts -> {
                delegateScope.launch {
                    syncEngine.clearConflicts()
                    parent?.updateContent()
                }
            }
            SettingsBlock.SyncServer -> {
                delegateScope.launch {
                    val serverUrl = withContext(Dispatchers.IO) { credentialStore.serverUrl }
                    router.navigate(
                        SyncServerDialogParams(
                            initialUrl = serverUrl,
                        ),
                    )
                }
            }
            SettingsBlock.SyncUsername -> {
                delegateScope.launch {
                    val username = withContext(Dispatchers.IO) { credentialStore.username }
                    router.navigate(
                        SyncCredentialsDialogParams(
                            inputType = SyncCredentialsInputType.Username,
                            initialUsername = username,
                        ),
                    )
                }
            }
            SettingsBlock.SyncToken -> {
                delegateScope.launch {
                    router.navigate(
                        SyncCredentialsDialogParams(
                            inputType = SyncCredentialsInputType.Token,
                            initialUsername = "",
                        ),
                    )
                }
            }
            else -> {
                // Do nothing
            }
        }
    }

    fun onSyncServerSaved(url: String) {
        delegateScope.launch {
            withContext(Dispatchers.IO) {
                credentialStore.serverUrl = url
            }
            parent?.updateContent()
        }
    }

    fun onSyncUsernameSaved(username: String) {
        delegateScope.launch {
            withContext(Dispatchers.IO) {
                credentialStore.username = username
            }
            parent?.updateContent()
        }
    }

    fun onSyncTokenSaved(token: String) {
        delegateScope.launch {
            withContext(Dispatchers.IO) {
                credentialStore.apiToken = token
            }
            parent?.updateContent()
        }
    }

    companion object : SettingsDelegate.Key
}
