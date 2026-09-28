package com.example.util.simpletimetracker.feature_settings.syncCredentials.model

interface SyncCredentialsDialogListener {

    fun onSyncUsernameSaved(username: String)

    fun onSyncTokenSaved(token: String)
}
