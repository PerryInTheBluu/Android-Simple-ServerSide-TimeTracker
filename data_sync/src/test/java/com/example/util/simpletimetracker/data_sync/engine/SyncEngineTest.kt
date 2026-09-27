package com.example.util.simpletimetracker.data_sync.engine

import com.example.util.simpletimetracker.data_sync.api.LoginRequest
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.api.SyncApiFactory
import com.example.util.simpletimetracker.data_sync.api.SyncConflict
import com.example.util.simpletimetracker.data_sync.api.SyncPullResponse
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.example.util.simpletimetracker.data_sync.api.SyncPushResponse
import com.example.util.simpletimetracker.data_sync.api.TokenResponse
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncQueueDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SyncEngineTest {

    private val credentialStore: SyncCredentialStore = mock()
    private val recordTypeRepo: RecordTypeRepo = mock()
    private val recordRepo: RecordRepo = mock()
    private val syncQueueDao: SyncQueueDao = mock()
    private val syncConflictDao: SyncConflictDao = mock()
    private val moshi = Moshi.Builder().build()

    @Test
    fun doesNotCreateApiWhenNotConfigured(): Unit = runBlocking {
        // Given
        whenever(credentialStore.isConfigured).thenReturn(false)
        var factoryInvoked = false
        val syncApiFactory = SyncApiFactory {
            factoryInvoked = true
            mock<SyncApi>()
        }
        val subject = SyncEngine(
            syncApiFactory = syncApiFactory,
            credentialStore = credentialStore,
            recordTypeRepo = recordTypeRepo,
            recordRepo = recordRepo,
            syncQueueDao = syncQueueDao,
            syncConflictDao = syncConflictDao,
            moshi = moshi,
        )

        // When
        subject.syncNow()

        // Then
        assertEquals(SyncStatus.NOT_CONFIGURED, subject.status.value)
        assertFalse(factoryInvoked)
        verifyNoInteractions(recordTypeRepo)
        verifyNoInteractions(recordRepo)
    }

    @Test
    fun reportsErrorWhenApiCreationFails(): Unit = runBlocking {
        // Given
        whenever(credentialStore.isConfigured).thenReturn(true)
        val subject = SyncEngine(
            syncApiFactory = SyncApiFactory { throw IllegalArgumentException("Sync server URL is missing or invalid") },
            credentialStore = credentialStore,
            recordTypeRepo = recordTypeRepo,
            recordRepo = recordRepo,
            syncQueueDao = syncQueueDao,
            syncConflictDao = syncConflictDao,
            moshi = moshi,
        )

        // When
        subject.syncNow()

        // Then
        assertEquals(SyncStatus.ERROR, subject.status.value)
        verifyNoInteractions(recordTypeRepo)
        verifyNoInteractions(recordRepo)
    }

    @Test
    fun reportsOfflineWhenServerUnreachable(): Unit = runBlocking {
        // Given
        whenever(credentialStore.isConfigured).thenReturn(true)
        whenever(recordTypeRepo.getAll()).thenReturn(emptyList())
        whenever(recordRepo.getAll()).thenReturn(emptyList())
        val subject = SyncEngine(
            syncApiFactory = { OfflineSyncApi },
            credentialStore = credentialStore,
            recordTypeRepo = recordTypeRepo,
            recordRepo = recordRepo,
            syncQueueDao = syncQueueDao,
            syncConflictDao = syncConflictDao,
            moshi = moshi,
        )

        // When
        subject.syncNow()

        // Then
        assertEquals(SyncStatus.OFFLINE, subject.status.value)
    }

    private object OfflineSyncApi : SyncApi {
        override suspend fun login(body: LoginRequest): TokenResponse =
            throw java.io.IOException("offline")

        override suspend fun push(body: SyncPushRequest): SyncPushResponse =
            throw java.io.IOException("offline")

        override suspend fun pull(since: String?): SyncPullResponse =
            throw java.io.IOException("offline")

        override suspend fun conflicts(): List<SyncConflict> =
            throw java.io.IOException("offline")
    }
}
