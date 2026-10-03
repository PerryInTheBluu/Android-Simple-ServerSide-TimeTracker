package com.example.util.simpletimetracker.data_sync.auth

import com.example.util.simpletimetracker.data_sync.api.LoginRequest
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import timber.log.Timber

enum class SyncLoginResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    RATE_LIMITED,
    NETWORK_ERROR,
    SERVER_ERROR,
}

/**
 * Stores the server url and logs in with the given credentials.
 * On success the returned long lived api token is stored encrypted.
 * On failure the previously stored token stays unchanged.
 */
@Singleton
class SyncLoginInteractor @Inject constructor(
    private val syncApi: SyncApi,
    private val credentialStore: SyncCredentialStore,
) {

    suspend fun execute(
        url: String,
        username: String,
        password: String,
    ): SyncLoginResult {
        credentialStore.serverUrl = url
        if (url.isBlank()) {
            credentialStore.username = ""
            credentialStore.apiToken = ""
            return SyncLoginResult.SUCCESS
        }
        if (username.isEmpty() || password.isEmpty()) {
            // Url only update: keep the existing credentials.
            return SyncLoginResult.SUCCESS
        }
        return try {
            val response = syncApi.login(LoginRequest(username, password))
            credentialStore.username = username
            credentialStore.apiToken = response.access_token
            SyncLoginResult.SUCCESS
        } catch (e: HttpException) {
            Timber.w(e, "Sync login failed with http code %s", e.code())
            when (e.code()) {
                401 -> SyncLoginResult.INVALID_CREDENTIALS
                429 -> SyncLoginResult.RATE_LIMITED
                else -> SyncLoginResult.SERVER_ERROR
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync login failed")
            SyncLoginResult.NETWORK_ERROR
        }
    }
}
