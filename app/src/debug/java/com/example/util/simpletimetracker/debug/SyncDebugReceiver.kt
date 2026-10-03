package com.example.util.simpletimetracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.util.simpletimetracker.data_sync.db.SyncIdMapDao
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.record.repo.RunningRecordRepo
import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
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
    lateinit var recordRepo: RecordRepo

    @Inject
    lateinit var runningRecordRepo: RunningRecordRepo

    @Inject
    lateinit var recordTypeRepo: RecordTypeRepo

    @Inject
    lateinit var syncStateDao: SyncStateDao

    @Inject
    lateinit var syncIdMapDao: SyncIdMapDao

    @Inject
    lateinit var credentialStore: SyncCredentialStore

    @Inject
    lateinit var categoryRepo: CategoryRepo

    @Inject
    lateinit var recordTypeCategoryRepo: RecordTypeCategoryRepo

    @Inject
    lateinit var addRunningRecordMediator: AddRunningRecordMediator

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
                    ACTION_WIPE_LOCAL -> {
                        Timber.i("DebugReceiver: wiping local data for a clean first sync")
                        recordRepo.clear()
                        runningRecordRepo.clear()
                        recordTypeRepo.clear()
                        syncStateDao.clear()
                        syncIdMapDao.clear()
                        Timber.i("DebugReceiver: local data wiped")
                    }
                    ACTION_ADD_UNI_TEST_DATA -> addUniTestData()
                    ACTION_START_TIMER -> startTimer(intent.getStringExtra(EXTRA_NAME).orEmpty())
                    ACTION_STOP_ALL_TIMERS -> stopAllTimers()
                    ACTION_DUMP_RUNNING -> dumpRunning()
                    else -> Timber.w("DebugReceiver: unknown action %s", intent.action)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun startTimer(name: String) {
        val type = recordTypeRepo.get(name).firstOrNull()
        if (type == null) {
            Timber.w("DebugReceiver: unknown activity %s", name)
            return
        }
        addRunningRecordMediator.startTimer(
            typeId = type.id,
            tags = emptyList<RecordBase.Tag>(),
            comment = "",
            timeStarted = AddRunningRecordMediator.StartTime.TakeCurrent,
        )
        Timber.i("DebugReceiver: started %s (id=%d)", name, type.id)
    }

    private suspend fun stopAllTimers() {
        val running = runningRecordRepo.getAll()
        running.forEach { runningRecordRepo.remove(it.id) }
        Timber.i("DebugReceiver: stopped %d timers (without records)", running.size)
    }

    private suspend fun dumpRunning() {
        val running = runningRecordRepo.getAll()
        val names = mutableListOf<String>()
        running.forEach { r ->
            names.add(recordTypeRepo.get(r.id)?.name ?: "?")
        }
        Timber.i("DebugReceiver: running=%d [%s]", running.size, names.joinToString())
    }

    private suspend fun addUniTestData() {
        val existing = categoryRepo.get("Uni").firstOrNull()
        val categoryId = existing?.id ?: categoryRepo.add(
            Category(
                id = 0,
                name = "Uni",
                color = AppColor(colorId = 5, colorInt = ""),
                note = "",
            ),
        )
        val assigned = recordTypeCategoryRepo.getTypeIdsByCategory(categoryId)
        recordTypeRepo.getAll()
            .filter { it.name in listOf("Vorlesung", "Lernen") && it.id !in assigned }
            .forEach { recordTypeCategoryRepo.addTypes(categoryId, listOf(it.id)) }
        val typeIds = recordTypeCategoryRepo.getTypeIdsByCategory(categoryId)
        Timber.i("DebugReceiver: uni category id=%d types=%s", categoryId, typeIds)
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
        const val ACTION_WIPE_LOCAL = "de.piusdischinger.timetracker.debug.WIPE_LOCAL"
        const val ACTION_ADD_UNI_TEST_DATA = "de.piusdischinger.timetracker.debug.ADD_UNI_TEST_DATA"
        const val ACTION_START_TIMER = "de.piusdischinger.timetracker.debug.START_TIMER"
        const val ACTION_STOP_ALL_TIMERS = "de.piusdischinger.timetracker.debug.STOP_ALL_TIMERS"
        const val ACTION_DUMP_RUNNING = "de.piusdischinger.timetracker.debug.DUMP_RUNNING"
        const val EXTRA_URL = "url"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_HOST = "host"
        const val EXTRA_NAME = "name"
    }
}
