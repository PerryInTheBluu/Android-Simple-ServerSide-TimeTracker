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
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
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

    @Inject
    lateinit var timetableRepo: com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo

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
                    ACTION_SEED_TIMETABLE -> seedTimetable()
                    else -> Timber.w("DebugReceiver: unknown action %s", intent.action)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun seedTimetable() {
        val uniCategory = categoryRepo.get("Uni").firstOrNull()
        val uniCategoryId = uniCategory?.id
            ?: categoryRepo.add(Category(id = 0, name = "Uni", color = AppColor(colorId = 10, colorInt = ""), note = ""))

        // Uniform test activities in the green palette range, all in the Uni category.
        val seedActivities = listOf(
            "Thermo VL" to 9, // teal
            "Thermo Übung" to 10, // green
            "Mathe VL" to 11, // light green
            "Mathe Übung" to 10, // green
            "Physik Tutorium" to 12, // lime
        )
        val activityIds = mutableMapOf<String, Long>()
        seedActivities.forEach { (name, colorId) ->
            val existing = recordTypeRepo.get(name).firstOrNull()
            val id = existing?.id ?: recordTypeRepo.add(
                RecordType(
                    id = 0,
                    name = name,
                    icon = "",
                    color = AppColor(colorId = colorId, colorInt = ""),
                    defaultDuration = 0,
                    note = "",
                ),
            )
            activityIds[name] = id
            if (uniCategoryId != null) {
                recordTypeCategoryRepo.addTypes(uniCategoryId, listOf(id))
            }
        }

        timetableRepo.clearEvents()

        // Fictional timetable: Mo - Fr, lectures, exercises and a tutorium.
        val events = listOf(
            TimetableEvent(name = "Thermo VL", dayOfWeek = 1, startTime = 8 * 60 + 15, endTime = 9 * 60 + 45, room = "HS 1", type = TimetableEvent.Type.LECTURE, comment = "", activityTypeId = activityIds["Thermo VL"]),
            TimetableEvent(name = "Mathe VL", dayOfWeek = 1, startTime = 10 * 60 + 15, endTime = 11 * 60 + 45, room = "HS 2", type = TimetableEvent.Type.LECTURE, comment = "Serie 3 abgeben", activityTypeId = activityIds["Mathe VL"]),
            TimetableEvent(name = "Thermo Übung", dayOfWeek = 2, startTime = 12 * 60, endTime = 13 * 60 + 30, room = "R 2.104", type = TimetableEvent.Type.EXERCISE, comment = "", activityTypeId = activityIds["Thermo Übung"]),
            TimetableEvent(name = "Mathe VL", dayOfWeek = 2, startTime = 14 * 60 + 15, endTime = 15 * 60 + 45, room = "HS 2", type = TimetableEvent.Type.LECTURE, comment = "", activityTypeId = activityIds["Mathe VL"]),
            TimetableEvent(name = "Mathe Übung", dayOfWeek = 3, startTime = 8 * 60 + 15, endTime = 9 * 60 + 45, room = "R 1.012", type = TimetableEvent.Type.EXERCISE, comment = "Rechner algebra aktiv", activityTypeId = activityIds["Mathe Übung"]),
            TimetableEvent(name = "Thermo VL", dayOfWeek = 3, startTime = 10 * 60 + 15, endTime = 11 * 60 + 45, room = "HS 1", type = TimetableEvent.Type.LECTURE, comment = "", activityTypeId = activityIds["Thermo VL"]),
            TimetableEvent(name = "Physik Tutorium", dayOfWeek = 4, startTime = 16 * 60, endTime = 17 * 60 + 30, room = "R 0.201", type = TimetableEvent.Type.TUTORIUM, comment = "", activityTypeId = activityIds["Physik Tutorium"]),
            TimetableEvent(name = "Mathe Übung", dayOfWeek = 5, startTime = 10 * 60 + 15, endTime = 11 * 60 + 45, room = "R 1.012", type = TimetableEvent.Type.EXERCISE, comment = "", activityTypeId = activityIds["Mathe Übung"]),
            // Saturday slots for testing today: one in the past, one upcoming.
            TimetableEvent(name = "Thermo VL", dayOfWeek = 6, startTime = 8 * 60 + 15, endTime = 9 * 60 + 45, room = "HS 1", type = TimetableEvent.Type.LECTURE, comment = "", activityTypeId = activityIds["Thermo VL"]),
            TimetableEvent(name = "Mathe Übung", dayOfWeek = 6, startTime = 22 * 60, endTime = 23 * 60 + 30, room = "R 1.012", type = TimetableEvent.Type.EXERCISE, comment = "Testslot", activityTypeId = activityIds["Mathe Übung"]),
        )
        events.forEach { event ->
            val id = timetableRepo.addEvent(event)
            if (event.type == TimetableEvent.Type.LECTURE) {
                timetableRepo.addTodo(
                    TimetableTodo(eventId = id, date = null, text = "Vorbereitung: Skript lesen", done = false, type = TimetableTodo.Type.PREPARATION),
                )
                timetableRepo.addTodo(
                    TimetableTodo(eventId = id, date = null, text = "Nachbereitung: Zusammenfassung", done = false, type = TimetableTodo.Type.FOLLOW_UP),
                )
            } else {
                timetableRepo.addTodo(
                    TimetableTodo(eventId = id, date = null, text = "Vorbereitung: Aufgabenblatt", done = false, type = TimetableTodo.Type.PREPARATION),
                )
            }
        }
        Timber.i("DebugReceiver: seeded %d timetable events with %d activities", events.size, seedActivities.size)
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
        const val ACTION_SEED_TIMETABLE = "de.piusdischinger.timetracker.debug.SEED_TIMETABLE"
        const val EXTRA_URL = "url"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_HOST = "host"
        const val EXTRA_NAME = "name"
    }
}
