package com.example.util.simpletimetracker.data_sync.work

import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Runs a direct in process sync a short delay after local data changes
 * (timers, records, activities), so changes reach the server within a few
 * seconds. The periodic WorkManager sync remains as a background fallback;
 * this subscriber only runs while the app process is alive.
 *
 * The debounced collector never cancels a running sync: cancelling a sync
 * mid apply left partially imported data behind (duplicated timers, missing
 * id mappings). Events arriving while a sync runs trigger one more sync
 * afterwards, so the last change always converges.
 */
@Singleton
class SyncOnDataChangeInteractor @Inject constructor(
    private val syncEngine: SyncEngine,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun subscribe() {
        scope.launch {
            LocalDataChangedBus.events
                .debounce(SYNC_DEBOUNCE_MS)
                .collect {
                    runCatching { syncEngine.syncNow() }
                }
        }
    }

    companion object {
        private const val SYNC_DEBOUNCE_MS = 1_000L
    }
}
