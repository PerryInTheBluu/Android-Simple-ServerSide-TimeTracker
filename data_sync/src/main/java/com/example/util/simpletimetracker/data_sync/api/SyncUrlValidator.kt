package com.example.util.simpletimetracker.data_sync.api

import okhttp3.HttpUrl

/**
 * Validates and normalizes user entered sync server URLs.
 * An empty input is allowed and means "not configured / offline".
 */
object SyncUrlValidator {

    fun normalizeOrNull(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val url: HttpUrl = HttpUrl.parse(withScheme) ?: return null
        if (url.host().isBlank()) return null
        val result = url.newBuilder()
            .query(null)
            .fragment(null)
            .build()
            .toString()
        return if (result.endsWith("/")) result else "$result/"
    }

    fun isValid(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.isEmpty() || normalizeOrNull(trimmed) != null
    }
}
