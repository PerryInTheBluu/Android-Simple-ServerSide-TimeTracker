package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.engine.SyncStatus
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            .first { it.block == SettingsBlock.SyncServer }
        assertEquals("", serverBlock.subtitle)

        val syncNowBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncNow }
        assertEquals(false, syncNowBlock.layoutIsClickable)
    }

    @Test
    fun savedUsernameIsShownAfterReload() = runTest {
        whenever(credentialStore.username).thenReturn("alice")

        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val usernameBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncUsername }
        assertEquals("alice", usernameBlock.subtitle)
        assertEquals(true, usernameBlock.layoutIsClickable)
    }

    @Test
    fun emptyUsernameIsShownAsNotSet() = runTest {
        whenever(credentialStore.username).thenReturn("")

        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val usernameBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncUsername }
        assertEquals("", usernameBlock.subtitle)
        assertEquals(true, usernameBlock.layoutIsClickable)
    }

    @Test
    fun savedTokenIsNeverShownInClearText() = runTest {
        val token = "secret-token-value"
        whenever(credentialStore.apiToken).thenReturn(token)

        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val tokenBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncToken }
        assertEquals("", tokenBlock.subtitle)
        assertFalse(data.toString().contains(token))
    }

    @Test
    fun emptyTokenIsShownAsNotSet() = runTest {
        whenever(credentialStore.apiToken).thenReturn("")

        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val tokenBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncToken }
        assertEquals("", tokenBlock.subtitle)
    }

    @Test
    fun syncNowAndConflictsStayNotAvailable() = runTest {
        val data = interactor.execute(SyncStatus.NOT_CONFIGURED)

        val syncNowBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncNow }
        assertEquals(false, syncNowBlock.layoutIsClickable)

        val conflictsBlock = data.filterIsInstance<SettingsTextViewData>()
            .first { it.block == SettingsBlock.SyncConflicts }
        assertEquals(false, conflictsBlock.layoutIsClickable)
    }
}
