package com.example.util.simpletimetracker.data_sync.di

import android.content.Context
import com.example.util.simpletimetracker.data_sync.api.LoginRequest
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.api.SyncConflict
import com.example.util.simpletimetracker.data_sync.api.SyncPullResponse
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.example.util.simpletimetracker.data_sync.api.SyncPushResponse
import com.example.util.simpletimetracker.data_sync.api.SyncUrlValidator
import com.example.util.simpletimetracker.data_sync.api.TokenResponse
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncDatabase
import com.example.util.simpletimetracker.data_sync.db.SyncQueueDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSyncModule {

    @Provides
    @Singleton
    fun provideSyncDatabase(@ApplicationContext context: Context): SyncDatabase {
        return SyncDatabase.build(context)
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: SyncDatabase): SyncQueueDao = database.syncQueueDao()

    @Provides
    @Singleton
    fun provideSyncConflictDao(database: SyncDatabase): SyncConflictDao = database.syncConflictDao()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncApi(
        moshi: Moshi,
        okHttpClient: OkHttpClient,
        credentialStore: SyncCredentialStore,
    ): SyncApi {
        val authClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val token = credentialStore.apiToken
                val request = chain.request().newBuilder()
                    .apply { if (token.isNotEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                chain.proceed(request)
            }
            .build()
        return UrlSwitchingSyncApi(
            baseUrlProvider = { credentialStore.serverUrl },
            moshi = moshi,
            okHttpClient = authClient,
        )
    }
}

class UrlSwitchingSyncApi(
    private val baseUrlProvider: () -> String,
    moshi: Moshi,
    private val okHttpClient: OkHttpClient,
) : SyncApi {

    private val moshiConverterFactory = MoshiConverterFactory.create(moshi)

    private fun api(): SyncApi {
        val raw = baseUrlProvider().trim()
        if (raw.isEmpty()) throw SyncNotConfiguredException()
        val normalized = SyncUrlValidator.normalizeOrNull(raw)
            ?: throw SyncInvalidUrlException(raw)
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
        return retrofit.create(SyncApi::class.java)
    }

    override suspend fun login(body: LoginRequest): TokenResponse = api().login(body)

    override suspend fun push(body: SyncPushRequest): SyncPushResponse = api().push(body)

    override suspend fun pull(since: String?): SyncPullResponse = api().pull(since)

    override suspend fun conflicts(): List<SyncConflict> = api().conflicts()
}

class SyncNotConfiguredException : IllegalStateException("Sync server is not configured")

class SyncInvalidUrlException(val url: String) : IllegalStateException("Invalid sync server URL")
