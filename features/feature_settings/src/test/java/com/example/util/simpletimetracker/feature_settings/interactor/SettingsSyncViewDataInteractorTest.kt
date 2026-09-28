package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.engine.SyncStatus
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SettingsSyncViewDataInteractorTest {

    private val resourceRepo: ResourceRepo = mock()
    private val credentialStore: SyncCredentialStore = mock()

    private val interactor = SettingsSyncViewDataInteractor(
        resourceRepo = resourceRepo,
        credentialStore = credentialStore,
    )

    private val readThreads = mutableSetOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(resourceRepo.getString(any<Int>())).thenReturn("")
        whenever(resourceRepo.getString(any<Int>(), any())).thenReturn("")
        whenever(credentialStore.serverUrl).thenAnswer {
            readThreads.add(Thread.currentThread().name)
            ""
        }
        whenever(credentialStore.username).thenAnswer {
            readThreads.add(Thread.currentThread().name)
            ""
        }
        whenever(credentialStore.apiToken).thenAnswer {
            readThreads.add(Thread.currentThread().name)
            ""
        }
        whenever(credentialStore.isConfigured).thenAnswer {
            readThreads.add(Thread.currentThread().name)
            false
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun credentialStoreIsNeverReadOnMainThread() = runTest {
        interactor.execute(SyncStatus.NOT_CONFIGURED)

        assertTrue(readThreads.isNotEmpty())
        readThreads.forEach { threadName ->
            assertTrue(
                "SyncCredentialStore was read on thread $threadName",
                !threadName.contains("main", ignoreCase = true),
            )
        }
    }

    @Test
    fun viewDataContainsServerBlockWithHintWhenUrlIsEmpty() = runTest {
        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val serverBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == com.example.util.simpletimetracker.feature_settings.api.SettingsBlock.SyncServer }
        assertEquals("", serverBlock.subtitle)

        val syncNowBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == com.example.util.simpletimetracker.feature_settings.api.SettingsBlock.SyncNow }
        assertEquals(false, syncNowBlock.layoutIsClickable)
    }
}
