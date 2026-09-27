package com.example.util.simpletimetracker.data_sync.di

import android.content.Context
import com.example.util.simpletimetracker.data_sync.api.SyncApi
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
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient,
        credentialStore: SyncCredentialStore,
    ): SyncApi {
        val baseUrl = credentialStore.serverUrl
            .trimEnd('/') + "/"
        val authClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val token = credentialStore.apiToken
                val request = chain.request().newBuilder()
                    .apply { if (token.isNotEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SyncApi::class.java)
    }
}
