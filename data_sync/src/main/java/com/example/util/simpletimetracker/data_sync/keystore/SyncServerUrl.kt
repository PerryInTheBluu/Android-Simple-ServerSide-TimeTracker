package com.example.util.simpletimetracker.data_sync.keystore

import java.net.URI

internal fun normalizeServerUrlOrNull(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host?.lowercase() ?: return null
    if (host.isEmpty()) return null
    val port = if (uri.port > 0) ":${uri.port}" else ""
    val path = (uri.path ?: "").trimEnd('/')
    return "$scheme://$host$port$path/"
}
