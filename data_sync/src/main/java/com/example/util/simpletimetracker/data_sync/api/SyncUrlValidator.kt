package com.example.util.simpletimetracker.data_sync.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Validates and normalizes the user configured sync server URL.
 * Retrofit requires an http(s) base URL; an invalid or empty value must never
 * reach Retrofit.Builder.baseUrl, because the eager singleton provider runs
 * at app start and would crash the app before sync is even configured.
 */
object SyncUrlValidator {

    /**
     * Returns a normalized http(s) base URL with trailing slash, or null when
     * [rawUrl] is not a usable http(s) URL.
     */
    fun normalizeOrNull(rawUrl: String): String? {
        val url = rawUrl.trim()
        if (url.isEmpty()) return null
        val candidate = if (url.contains("://")) url else "https://$url"
        val httpUrl = candidate.toHttpUrlOrNull() ?: return null
        if (httpUrl.scheme != "http" && httpUrl.scheme != "https") return null
        if (httpUrl.host.isEmpty()) return null
        val withoutQuery = httpUrl.newBuilder()
            .query(null)
            .fragment(null)
            .build()
        return withoutQuery.toString().trimEnd('/') + "/"
    }
}
