package com.example.util.simpletimetracker.data_sync.keystore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the server URL, username and API token locally, encrypted with
 * a key held in the Android Keystore. Nothing leaves the device except
 * requests to the configured private tailnet server.
 */
@Singleton
class SyncCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sync_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var apiToken: String
        get() = prefs.getString(KEY_API_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_TOKEN, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    val isConfigured: Boolean
        get() = apiToken.isNotEmpty() && normalizeServerUrlOrNull(serverUrl) != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_LAST_SYNC = "last_sync"
    }
}
