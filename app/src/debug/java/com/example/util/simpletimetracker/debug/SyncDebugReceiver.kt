package com.example.util.simpletimetracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.util.simpletimetracker.data_sync.db.SyncIdMapDao
import com.example.util.simpletimetracker.data_sync.db.SyncStateDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.work.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.net.InetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Debug only helper (shipped in the debug build type only): lets adb
 * trigger a sync or dump the sync state without touching the UI.
 *
 * Trigger a sync:
 *   adb shell am broadcast -a de.piusdischinger.timetracker.debug.SYNC_NOW \
 *     -n de.piusdischinger.timetracker.dev/com.example.util.simpletimetracker.debug.SyncDebugReceiver
 *
 * Dump sync state:
 *   adb shell am broadcast -a de.piusdischinger.timetracker.debug.DUMP_STATE \
 *     -n de.piusdischinger.timetracker.dev/com.example.util.simpletimetracker.debug.SyncDebugReceiver
 */
@AndroidEntryPoint
class SyncDebugReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var syncStateDao: SyncStateDao

    @Inject
    lateinit var syncIdMapDao: SyncIdMapDao

    @Inject
    lateinit var credentialStore: SyncCredentialStore

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_SYNC_NOW -> {
                        Timber.i("DebugReceiver: sync requested")
                        syncScheduler.syncNow()
                    }
                    ACTION_DUMP_STATE -> dumpState()
                    ACTION_DNS_TEST -> {
                        val host = intent.getStringExtra(EXTRA_HOST)
                            ?: Uri.parse(credentialStore.serverUrl).host.orEmpty()
                        if (host.isNotEmpty()) {
                            Timber.i("DebugReceiver: dns test for %s", host)
                            try {
                                val addresses = InetAddress.getAllByName(host)
                                Timber.i("DebugReceiver: dns ok: %s", addresses.joinToString())
                            } catch (e: Exception) {
                                Timber.e(e, "DebugReceiver: dns FAILED for %s", host)
                            }
                            // Raw connect to the tailnet ip bypassing dns.
                            try {
                                java.net.Socket().use { socket ->
                                    socket.connect(java.net.InetSocketAddress("100.64.0.9", 443), 3000)
                                    Timber.i("DebugReceiver: raw connect to 100.64.0.9:443 ok")
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "DebugReceiver: raw connect to 100.64.0.9:443 FAILED")
                            }
                        }
                    }
                    ACTION_SET_CREDENTIALS -> {
                        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
                        val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
                        if (url.isNotEmpty() && username.isNotEmpty() && token.isNotEmpty()) {
                            credentialStore.serverUrl = url
                            credentialStore.username = username
                            credentialStore.apiToken = token
                            Timber.i("DebugReceiver: credentials stored for %s", username)
                            syncScheduler.syncNow()
                        } else {
                            Timber.w("DebugReceiver: set credentials called with missing extras")
                        }
                    }
                    else -> Timber.w("DebugReceiver: unknown action %s", intent.action)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun dumpState() {
        val mirror = syncStateDao.getAll()
        val mappings = syncIdMapDao.getAll()
        Timber.i(
            "DebugReceiver state: url=%s user=%s marker=%s mirrorEntries=%d idMappings=%d",
            credentialStore.serverUrl,
            credentialStore.username,
            credentialStore.lastSyncMarker,
            mirror.size,
            mappings.size,
        )
        mirror.take(50).forEach {
            Timber.i("  mirror %s %s hash=%s", it.entityType, it.entityId, it.contentHash.take(10))
        }
    }

    companion object {
        const val ACTION_SYNC_NOW = "de.piusdischinger.timetracker.debug.SYNC_NOW"
        const val ACTION_DUMP_STATE = "de.piusdischinger.timetracker.debug.DUMP_STATE"
        const val ACTION_SET_CREDENTIALS = "de.piusdischinger.timetracker.debug.SET_CREDENTIALS"
        const val ACTION_DNS_TEST = "de.piusdischinger.timetracker.debug.DNS_TEST"
        const val EXTRA_URL = "url"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_HOST = "host"
    }
}
