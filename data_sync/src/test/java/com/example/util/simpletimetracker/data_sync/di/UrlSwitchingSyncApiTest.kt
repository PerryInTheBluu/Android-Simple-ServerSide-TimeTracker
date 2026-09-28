package com.example.util.simpletimetracker.data_sync.di

import com.example.util.simpletimetracker.data_sync.api.LoginRequest
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSwitchingSyncApiTest {

    private val api = UrlSwitchingSyncApi(
        baseUrlProvider = { "" },
        moshi = Moshi.Builder().build(),
        okHttpClient = OkHttpClient(),
    )

    @Test
    fun emptyUrlThrowsNotConfiguredInsteadOfCrashing() {
        val error = runBlocking {
            try {
                api.pull(null)
                null
            } catch (e: SyncNotConfiguredException) {
                e
            }
        }
        assertTrue(error is SyncNotConfiguredException)
    }

    @Test
    fun invalidUrlThrowsInvalidUrl() {
        val api = UrlSwitchingSyncApi(
            baseUrlProvider = { "not a url" },
            moshi = Moshi.Builder().build(),
            okHttpClient = OkHttpClient(),
        )
        val error = runBlocking {
            try {
                api.push(SyncPushRequest(items = emptyList()))
                null
            } catch (e: SyncInvalidUrlException) {
                e
            }
        }
        assertTrue(error is SyncInvalidUrlException)
        assertEquals("not a url", (error as SyncInvalidUrlException).url)
    }

    @Test(expected = SyncNotConfiguredException::class)
    fun loginWithEmptyUrlThrowsNotConfigured(): Unit = runBlocking {
        api.login(LoginRequest(username = "", password = ""))
    }
}
