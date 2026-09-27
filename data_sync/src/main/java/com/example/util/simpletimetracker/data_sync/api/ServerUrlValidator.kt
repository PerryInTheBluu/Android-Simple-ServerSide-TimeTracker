package com.example.util.simpletimetracker.data_sync.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Validates and normalizes the user configured sync server URL.
 *
 * The server URL is only ever used when it passed this validation, so an
 * empty or malformed value can never reach Retrofit's baseUrl and crash
 * the app (IllegalArgumentException: Expected URL scheme 'http' or 'https').
 */
object ServerUrlValidator {

    /**
     * @param raw user input from the settings screen.
     * @param allowHttp explicit opt-in for the debug build variant only;
     * release builds must use HTTPS.
     * @return the normalized absolute base URL with a trailing slash,
     * or null if the input is not a usable HTTP(S) URL.
     */
    fun normalize(raw: String, allowHttp: Boolean = false): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        if (!parsed.scheme.isAllowedScheme(allowHttp)) return null

        return parsed.toString().let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
    }

    private fun String.isAllowedScheme(allowHttp: Boolean): Boolean {
        return this == SCHEME_HTTPS || (allowHttp && this == SCHEME_HTTP)
    }

    private const val SCHEME_HTTPS = "https"
    private const val SCHEME_HTTP = "http"
}
