package com.example.util.simpletimetracker.data_sync.di

import android.content.Context
import com.example.util.simpletimetracker.data_sync.api.ConfiguredSyncClient
import com.example.util.simpletimetracker.data_sync.api.SyncClient
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncDatabase
import com.example.util.simpletimetracker.data_sync.db.SyncQueueDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
}

@Module
@InstallIn(SingletonComponent::class)
interface DataSyncBindModule {

    /**
     * SyncClient never builds Retrofit eagerly. If no valid server URL is
     * stored, every call fails in a controlled way with
     * SyncNotConfiguredException instead of crashing on an invalid baseUrl.
     */
    @Binds
    @Singleton
    fun bindSyncClient(implementation: ConfiguredSyncClient): SyncClient
}
