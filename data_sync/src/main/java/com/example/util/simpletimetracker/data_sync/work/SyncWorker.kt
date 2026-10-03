package com.example.util.simpletimetracker.data_sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background sync. Runs even when the app is closed, but never
 * blocks the running timer because all work happens off the UI thread
 * against the local database.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncEngine.syncNow()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        const val PERIODIC_INTERVAL_HOURS = 1L
    }
}
