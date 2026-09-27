package com.example.util.simpletimetracker.data_sync.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the crash:
 * FATAL EXCEPTION: IllegalArgumentException: Expected URL scheme
 * 'http' or 'https' but no colon was found
 * (DataSyncModule.provideRetrofit with an empty stored server URL).
 */
class ServerUrlValidatorTest {

    // 1. No server URL stored / empty input: never a valid base URL.
    @Test
    fun emptyUrlIsInvalid() {
        assertNull(ServerUrlValidator.normalize(""))
    }

    // 2. Whitespace-only URL: configuration error, not a crash.
    @Test
    fun whitespaceOnlyUrlIsInvalid() {
        assertNull(ServerUrlValidator.normalize("   \t "))
    }

    // 3. URL without scheme: configuration error, not a crash.
    @Test
    fun urlWithoutSchemeIsInvalid() {
        assertNull(ServerUrlValidator.normalize("server.local"))
    }

    // 4. Non-HTTP(S) scheme: configuration error, not a crash.
    @Test
    fun ftpSchemeIsInvalid() {
        assertNull(ServerUrlValidator.normalize("ftp://server.example"))
        assertNull(ServerUrlValidator.normalize("file:///data/something"))
    }

    // 5. Valid HTTPS URL without trailing slash: normalized with slash.
    @Test
    fun httpsUrlWithoutSlashIsNormalizedWithSlash() {
        val result = ServerUrlValidator.normalize("https://sync.example.invalid")
        assertNotNull(result)
        assertEquals("https://sync.example.invalid/", result)
    }

    // 6. Valid HTTPS URL with trailing slash: works unchanged.
    @Test
    fun httpsUrlWithSlashIsValid() {
        val result = ServerUrlValidator.normalize("https://sync.example.invalid/")
        assertNotNull(result)
        assertEquals("https://sync.example.invalid/", result)
    }

    // 7. HTTP only allowed in debug variant (allowHttp), never by default.
    @Test
    fun httpUrlIsInvalidByDefault() {
        assertNull(ServerUrlValidator.normalize("http://sync.example.invalid"))
    }

    @Test
    fun httpUrlIsAllowedOnlyWithExplicitOptIn() {
        val result = ServerUrlValidator.normalize(
            raw = "http://sync.example.invalid",
            allowHttp = true,
        )
        assertNotNull(result)
        assertEquals("http://sync.example.invalid/", result)
    }

    // Whitespace around a valid URL is trimmed, inner path is preserved.
    @Test
    fun surroundingWhitespaceIsTrimmed() {
        val result = ServerUrlValidator.normalize("  https://sync.example.invalid  ")
        assertNotNull(result)
        assertEquals("https://sync.example.invalid/", result)
    }

    @Test
    fun urlWithPathKeepsPathAndSlash() {
        val result = ServerUrlValidator.normalize("https://sync.example.invalid/api")
        assertNotNull(result)
        assertEquals("https://sync.example.invalid/api/", result)
    }

    // Retrofit compatibility: baseUrl must end with '/'.
    @Test
    fun normalizedUrlIsRetrofitCompatible() {
        val result = ServerUrlValidator.normalize("https://sync.example.invalid")
        assertNotNull(result)
        assert(result!!.endsWith("/"))
    }
}
