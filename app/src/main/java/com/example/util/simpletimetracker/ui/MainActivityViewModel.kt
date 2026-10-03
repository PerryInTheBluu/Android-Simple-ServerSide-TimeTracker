package com.example.util.simpletimetracker.ui

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.repo.AutomaticBackupRepo
import com.example.util.simpletimetracker.core.repo.AutomaticExportRepo
import com.example.util.simpletimetracker.core.repo.DataEditRepo
import com.example.util.simpletimetracker.core.repo.FileWorkRepo
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import com.example.util.simpletimetracker.domain.extension.orFalse
import com.example.util.simpletimetracker.domain.recordType.interactor.InitialActivitiesInteractor
import com.example.util.simpletimetracker.feature_settings.viewModel.delegate.SettingsFileWorkDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val dataEditRepo: DataEditRepo,
    private val automaticBackupRepo: AutomaticBackupRepo,
    private val automaticExportRepo: AutomaticExportRepo,
    private val fileWorkRepo: FileWorkRepo,
    private val settingsFileWorkDelegate: SettingsFileWorkDelegate,
    private val initialActivitiesInteractor: InitialActivitiesInteractor,
    private val syncCredentialStore: SyncCredentialStore,
    private val syncScheduler: SyncScheduler,
) : BaseViewModel() {

    val progressVisibility: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(automaticBackupRepo.inProgress) { updateProgress() }
        addSource(automaticExportRepo.inProgress) { updateProgress() }
        addSource(dataEditRepo.inProgress) { updateProgress() }
        addSource(fileWorkRepo.inProgress) { updateProgress() }
    }

    init {
        viewModelScope.launch {
            // With a configured sync server the activities arrive from the
            // server; creating defaults here would duplicate them. The
            // encrypted preferences must not be touched on the main thread
            // (their first access also writes the prefs file).
            val isSyncConfigured = withContext(Dispatchers.IO) {
                syncCredentialStore.isConfigured
            }
            if (!isSyncConfigured) {
                initialActivitiesInteractor.executeIfEmpty()
            }
        }
    }

    fun onVisible() {
        settingsFileWorkDelegate.onAppVisible()
        // Pick up remote changes quickly when the app comes to the front;
        // the engine skips the run if a sync is already in progress.
        syncScheduler.syncNow()
    }

    private fun updateProgress() {
        val visible = dataEditRepo.inProgress.value.orFalse() ||
            automaticBackupRepo.inProgress.value.orFalse() ||
            automaticExportRepo.inProgress.value.orFalse() ||
            fileWorkRepo.inProgress.value.orFalse()

        progressVisibility.set(visible)
        // Here to check that if automatic update finishes with error while app is opened.
        settingsFileWorkDelegate.onFileWork()
    }
}
