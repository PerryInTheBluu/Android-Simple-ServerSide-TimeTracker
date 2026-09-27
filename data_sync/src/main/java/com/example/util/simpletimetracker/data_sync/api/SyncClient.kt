package com.example.util.simpletimetracker.data_sync.api

/**
 * Single access point for sync network calls.
 *
 * The implementation ([ConfiguredSyncClient]) builds Retrofit lazily and
 * only when a valid server URL is stored. While sync is not set up every
 * call fails in a controlled way with [SyncNotConfiguredException], so the
 * DI graph can always be created and the app works fully offline.
 */
interface SyncClient {
    suspend fun push(request: SyncPushRequest): Result<SyncPushResponse>
    suspend fun pull(since: String?): Result<SyncPullResponse>
    suspend fun login(request: LoginRequest): Result<TokenResponse>
    suspend fun conflicts(): Result<List<SyncConflict>>
}

/** Signaled when sync is used while no valid server URL is configured. */
class SyncNotConfiguredException : IllegalStateException(MESSAGE) {
    companion object {
        const val MESSAGE = "Sync server is not configured"
    }
}
