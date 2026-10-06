package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.engine.SyncStatus
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.views.SettingsBottomViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsHintViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextColor
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTopViewData
import com.example.util.simpletimetracker.resources.R as resourcesR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsSyncViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val credentialStore: SyncCredentialStore,
    private val syncConflictDao: SyncConflictDao,
) {

    suspend fun execute(status: SyncStatus): List<ViewHolderType> = withContext(Dispatchers.IO) {
        val serverUrl = credentialStore.serverUrl
        val apiToken = credentialStore.apiToken
        val isConfigured = credentialStore.isConfigured
        // After a fresh app start the in-memory engine status resets to
        // NOT_CONFIGURED although a previous sync happened; derive the
        // display status from the stored sync marker in that case.
        val effectiveStatus = if (
            status == SyncStatus.NOT_CONFIGURED &&
            isConfigured &&
            credentialStore.lastSyncMarker.isNotEmpty()
        ) {
            SyncStatus.SYNCED
        } else {
            status
        }
        val result = mutableListOf<ViewHolderType>()

        result += SettingsTopViewData(
            block = SettingsBlock.SyncTop,
        )

        result += SettingsTextViewData(
            block = SettingsBlock.SyncServer,
            title = resourceRepo.getString(resourcesR.string.settings_sync_server),
            subtitle = serverUrl.ifEmpty {
                resourceRepo.getString(resourcesR.string.settings_sync_server_hint)
            },
        )

        result += SettingsTextViewData(
            block = SettingsBlock.SyncUsername,
            title = resourceRepo.getString(resourcesR.string.settings_sync_username),
            subtitle = credentialStore.username.ifEmpty {
                resourceRepo.getString(resourcesR.string.settings_sync_unavailable)
            },
            layoutIsClickable = false,
        )

        result += SettingsTextViewData(
            block = SettingsBlock.SyncToken,
            title = resourceRepo.getString(resourcesR.string.settings_sync_token),
            subtitle = if (apiToken.isEmpty()) {
                resourceRepo.getString(resourcesR.string.settings_sync_unavailable)
            } else {
                "••••••••"
            },
            layoutIsClickable = false,
        )

        result += SettingsTextViewData(
            block = SettingsBlock.SyncNow,
            title = resourceRepo.getString(resourcesR.string.settings_sync_now),
            subtitle = if (isConfigured) {
                ""
            } else {
                resourceRepo.getString(resourcesR.string.settings_sync_not_configured)
            },
            layoutIsClickable = isConfigured,
        )

        val statusTextRes = when (effectiveStatus) {
            SyncStatus.SYNCED -> resourcesR.string.settings_sync_status_synced
            SyncStatus.PENDING -> resourcesR.string.settings_sync_status_pending
            SyncStatus.OFFLINE -> resourcesR.string.settings_sync_status_offline
            SyncStatus.ERROR -> resourcesR.string.settings_sync_status_error
            SyncStatus.NOT_CONFIGURED -> resourcesR.string.settings_sync_status_not_configured
        }
        result += SettingsHintViewData(
            block = SettingsBlock.SyncStatus,
            text = resourceRepo.getString(statusTextRes),
            textColor = SettingsTextColor.Success,
        )

        val conflictsCount = syncConflictDao.count()
        val conflictsSubtitle = if (conflictsCount > 0) {
            resourceRepo.getString(resourcesR.string.settings_sync_conflicts_count, conflictsCount)
        } else {
            resourceRepo.getString(resourcesR.string.settings_sync_conflicts_none)
        }

        result += SettingsTextViewData(
            block = SettingsBlock.SyncConflicts,
            title = resourceRepo.getString(resourcesR.string.settings_sync_conflicts),
            subtitle = conflictsSubtitle,
            layoutIsClickable = true,
        )

        result += SettingsBottomViewData(
            block = SettingsBlock.SyncBottom,
        )

        result
    }
}
