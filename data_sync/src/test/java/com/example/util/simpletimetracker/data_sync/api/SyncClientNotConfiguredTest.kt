package com.example.util.simpletimetracker.data_sync.api

import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import javax.inject.Provider

/**
 * Regression test for the offline startup crash: the DI graph must be
 * constructible without a configured server URL, and sync calls must be
 * treated as "not configured" instead of throwing
 * IllegalArgumentException from Retrofit's baseUrl.
 */
class SyncClientNotConfiguredTest {

    private fun clientWithUrl(url: String): ConfiguredSyncClient {
        val credentialStore = mock(SyncCredentialStore::class.java)
        `when`(credentialStore.serverUrl).thenReturn(url)
        `when`(credentialStore.apiToken).thenReturn("")
        return ConfiguredSyncClient(
            credentialStore = credentialStore,
            okHttpClient = Provider { OkHttpClient() },
            moshi = Provider { Moshi.Builder().build() },
        )
    }

    // 1. No server URL stored: no Retrofit construction, controlled failure.
    @Test
    fun pushWithoutConfiguredServerFailsControlled() {
        val client = clientWithUrl(url = "")

        val result = runBlocking { client.push(SyncPushRequest(items = emptyList())) }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SyncNotConfiguredException)
    }

    // 2/3/4. Invalid stored URLs are treated as not configured, no crash.
    // Note: plain http is intentionally not listed here because the debug
    // build variant under test explicitly allows http (production/release
    // does not). http handling is covered by ServerUrlValidatorTest.
    @Test
    fun invalidStoredUrlIsTreatedAsNotConfigured() {
        val urls = listOf("", "   ", "server.local", "ftp://server.example")

        for (url in urls) {
            val client = clientWithUrl(url = url)

            val result = runBlocking { client.pull(since = null) }

            assertTrue(url, result.isFailure)
            assertTrue(url, result.exceptionOrNull() is SyncNotConfiguredException)
        }
    }

    // 5. Valid HTTPS URL: Retrofit is built lazily on first use; the call
    // itself fails offline as a normal Result failure, not a crash, and is
    // not reported as "not configured".
    @Test
    fun validHttpsUrlBuildsRetrofitLazily() {
        val client = clientWithUrl(url = "https://sync.example.invalid")

        val result = runBlocking { client.pull(since = null) }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() !is SyncNotConfiguredException)
    }
}
