package com.example.util.simpletimetracker.feature_settings.syncServer.model

interface SyncServerDialogListener {
    fun onSyncServerSaved(url: String, username: String, password: String)
}
