package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsSyncViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncSettingsField
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsSyncViewModelDelegate @Inject constructor(
    private val settingsSyncViewDataInteractor: SettingsSyncViewDataInteractor,
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
    private val credentialStore: SyncCredentialStore,
    private val router: Router,
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
            SettingsBlock.SyncServer -> router.navigate(
                SyncSettingsDialogParams(field = SyncSettingsField.SERVER),
            )
            SettingsBlock.SyncUsername -> router.navigate(
                SyncSettingsDialogParams(field = SyncSettingsField.USERNAME),
            )
            SettingsBlock.SyncToken -> router.navigate(
                SyncSettingsDialogParams(field = SyncSettingsField.TOKEN),
            )
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
            else -> {
                // Do nothing
            }
        }
    }

    companion object : SettingsDelegate.Key
}
