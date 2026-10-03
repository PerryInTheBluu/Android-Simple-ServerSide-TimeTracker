package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.auth.SyncLoginInteractor
import com.example.util.simpletimetracker.data_sync.auth.SyncLoginResult
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsSyncViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.notification.ToastParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncServerDialogParams
import com.example.util.simpletimetracker.resources.R as resourcesR
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsSyncViewModelDelegate @Inject constructor(
    private val router: Router,
    private val settingsSyncViewDataInteractor: SettingsSyncViewDataInteractor,
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
    private val credentialStore: SyncCredentialStore,
    private val syncLoginInteractor: SyncLoginInteractor,
    private val resourceRepo: ResourceRepo,
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
                router.navigate(
                    SyncServerDialogParams(
                        initialUrl = credentialStore.serverUrl,
                        initialUsername = credentialStore.username,
                    ),
                )
            }
            else -> {
                // Do nothing
            }
        }
    }

    fun onSyncServerSaved(url: String, username: String, password: String) {
        delegateScope.launch {
            val result = syncLoginInteractor.execute(
                url = url,
                username = username,
                password = password,
            )
            val messageRes = when (result) {
                SyncLoginResult.INVALID_CREDENTIALS -> resourcesR.string.settings_sync_login_invalid
                SyncLoginResult.RATE_LIMITED -> resourcesR.string.settings_sync_login_rate_limited
                SyncLoginResult.NETWORK_ERROR,
                SyncLoginResult.SERVER_ERROR,
                -> resourcesR.string.settings_sync_login_failed
                SyncLoginResult.SUCCESS -> null
            }
            if (messageRes != null) {
                router.show(
                    ToastParams(message = resourceRepo.getString(messageRes)),
                )
            }
            parent?.updateContent()
        }
    }

    companion object : SettingsDelegate.Key
}
