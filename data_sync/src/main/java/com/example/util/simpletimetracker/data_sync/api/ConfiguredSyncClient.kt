package com.example.util.simpletimetracker.data_sync.api

import com.example.util.simpletimetracker.data_sync.BuildConfig
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Sync client for the configured server. The Retrofit instance is built
 * lazily on first use, and only if the stored server URL passes
 * [ServerUrlValidator], so baseUrl can never throw
 * IllegalArgumentException. If the stored URL becomes empty or invalid,
 * the cached instance is dropped and calls are treated as not configured.
 *
 * HTTP (instead of HTTPS) is only accepted in the debug build variant.
 */
@Singleton
class ConfiguredSyncClient @Inject constructor(
    private val credentialStore: SyncCredentialStore,
    private val okHttpClient: Provider<OkHttpClient>,
    private val moshi: Provider<Moshi>,
) : SyncClient {

    private val lock = Any()
    private var cachedForUrl: String? = null
    private var cachedForToken: String? = null
    private var cachedApi: SyncApi? = null

    override suspend fun push(request: SyncPushRequest): Result<SyncPushResponse> =
        call { it.push(request) }

    override suspend fun pull(since: String?): Result<SyncPullResponse> =
        call { it.pull(since) }

    override suspend fun login(request: LoginRequest): Result<TokenResponse> =
        call { it.login(request) }

    override suspend fun conflicts(): Result<List<SyncConflict>> =
        call { it.conflicts() }

    private suspend fun <T> call(block: suspend (SyncApi) -> T): Result<T> {
        val api = currentApi() ?: return Result.failure(SyncNotConfiguredException())
        return runCatching { block(api) }
    }

    private fun currentApi(): SyncApi? {
        val baseUrl = ServerUrlValidator.normalize(
            raw = credentialStore.serverUrl,
            allowHttp = BuildConfig.ALLOW_HTTP_SERVER_URL,
        ) ?: return null
        val token = credentialStore.apiToken

        synchronized(lock) {
            if (cachedForUrl == baseUrl && cachedForToken == token) return cachedApi
            val api = buildApi(baseUrl, token)
            cachedForUrl = baseUrl
            cachedForToken = token
            cachedApi = api
            return api
        }
    }

    private fun buildApi(baseUrl: String, token: String): SyncApi {
        val authClient = okHttpClient.get().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply { if (token.isNotEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi.get()))
            .build()
            .create(SyncApi::class.java)
    }
}
