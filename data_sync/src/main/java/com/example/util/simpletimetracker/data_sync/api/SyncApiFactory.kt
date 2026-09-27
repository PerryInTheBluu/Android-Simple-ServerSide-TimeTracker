package com.example.util.simpletimetracker.data_sync.api

/**
 * Creates the [SyncApi] lazily so that an unconfigured or invalid server URL
 * cannot crash the app during eager dependency graph construction. The factory
 * is only invoked when a sync attempt actually needs the API.
 */
fun interface SyncApiFactory {

    fun create(): SyncApi
}
