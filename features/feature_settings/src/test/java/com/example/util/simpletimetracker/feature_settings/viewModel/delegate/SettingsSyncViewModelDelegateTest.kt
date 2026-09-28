package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.data_sync.engine.SyncEngine
import com.example.util.simpletimetracker.data_sync.engine.SyncStatus
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsSyncViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.SyncCredentialsInputType
import com.example.util.simpletimetracker.navigation.params.screen.SyncServerDialogParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.after
import org.mockito.Mockito.timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsSyncViewModelDelegateTest {

    private val router: Router = mock()
    private val syncEngine: SyncEngine = mock()
    private val syncScheduler: SyncScheduler = mock()
    private val credentialStore: SyncCredentialStore = mock()
    private val resourceRepo: ResourceRepo = mock()
    private val interactor: SettingsSyncViewDataInteractor = mock()

    private lateinit var delegate: SettingsSyncViewModelDelegate

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(resourceRepo.getString(any<Int>())).thenReturn("")
        whenever(syncEngine.status).thenReturn(MutableStateFlow(SyncStatus.NOT_CONFIGURED))
        whenever(credentialStore.serverUrl).thenReturn("")
        whenever(credentialStore.username).thenReturn("")
        whenever(credentialStore.apiToken).thenReturn("")

        delegate = SettingsSyncViewModelDelegate(
            router = router,
            settingsSyncViewDataInteractor = interactor,
            syncEngine = syncEngine,
            syncScheduler = syncScheduler,
            credentialStore = credentialStore,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onSyncUsernameSavedStoresUsername() {
        delegate.onSyncUsernameSaved("alice")

        verify(credentialStore, timeout(5000)).username = "alice"
    }

    @Test
    fun onSyncUsernameSavedWithEmptyValueStoresEmptyUsername() {
        delegate.onSyncUsernameSaved("")

        verify(credentialStore, timeout(5000)).username = ""
    }

    @Test
    fun onSyncTokenSavedStoresToken() {
        delegate.onSyncTokenSaved("secret-token")

        verify(credentialStore, timeout(5000)).apiToken = "secret-token"
    }

    @Test
    fun onSyncTokenSavedWithEmptyValueClearsToken() {
        delegate.onSyncTokenSaved("")

        verify(credentialStore, timeout(5000)).apiToken = ""
    }

    @Test
    fun usernameClickNavigatesToUsernameDialogWithStoredValue() {
        whenever(credentialStore.username).thenReturn("alice")

        delegate.onBlockClicked(SettingsBlock.SyncUsername)

        val captor = argumentCaptor<SyncCredentialsDialogParams>()
        verify(router, timeout(5000)).navigate(captor.capture(), anyOrNull())
        assertEquals(SyncCredentialsInputType.Username, captor.firstValue.inputType)
        assertEquals("alice", captor.firstValue.initialUsername)
    }

    @Test
    fun tokenClickNavigatesToTokenDialogWithoutStoredValue() {
        whenever(credentialStore.apiToken).thenReturn("secret-token")

        delegate.onBlockClicked(SettingsBlock.SyncToken)

        val captor = argumentCaptor<SyncCredentialsDialogParams>()
        verify(router, timeout(5000)).navigate(captor.capture(), anyOrNull())
        assertEquals(SyncCredentialsInputType.Token, captor.firstValue.inputType)
        assertEquals("", captor.firstValue.initialUsername)
    }

    @Test
    fun serverClickNavigatesToServerDialog() {
        whenever(credentialStore.serverUrl).thenReturn("https://example.ts.net")

        delegate.onBlockClicked(SettingsBlock.SyncServer)

        val captor = argumentCaptor<SyncServerDialogParams>()
        verify(router, timeout(5000)).navigate(captor.capture(), anyOrNull())
        assertEquals("https://example.ts.net", captor.firstValue.initialUrl)
    }

    @Test
    fun conflictsClickDoesNotNavigateToCredentialsDialogs() {
        delegate.onBlockClicked(SettingsBlock.SyncConflicts)

        verify(
            router,
            after(500).never(),
        ).navigate(any<SyncCredentialsDialogParams>(), anyOrNull())
    }
}
