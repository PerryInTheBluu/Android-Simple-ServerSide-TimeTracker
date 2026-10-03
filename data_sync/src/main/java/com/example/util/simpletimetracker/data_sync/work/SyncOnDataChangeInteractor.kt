package com.example.util.simpletimetracker.data_sync.work

import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs a direct in process sync a short delay after local data changes
 * (timers, records, activities), so changes reach the server within a
 * few seconds. The periodic WorkManager sync remains as a background
 * fallback; this subscriber only runs while the app process is alive.
 */
@Singleton
class SyncOnDataChangeInteractor @Inject constructor(
    private val syncEngine: SyncEngine,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSync: Job? = null

    fun subscribe() {
        scope.launch {
            LocalDataChangedBus.events.collect {
                pendingSync?.cancel()
                pendingSync = scope.launch {
                    delay(SYNC_DEBOUNCE_MS)
                    runCatching { syncEngine.syncNow() }
                }
            }
        }
    }

    companion object {
        private const val SYNC_DEBOUNCE_MS = 2_000L
    }
}
