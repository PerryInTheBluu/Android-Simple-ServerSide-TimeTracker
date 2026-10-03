package com.example.util.simpletimetracker.data_sync.engine

import com.example.util.simpletimetracker.data_sync.api.ActivityDto
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.api.SyncPushItem
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.example.util.simpletimetracker.data_sync.api.TimeEntryDto
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDBO
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncIdMapDBO
import com.example.util.simpletimetracker.data_sync.db.SyncIdMapDao
import com.example.util.simpletimetracker.data_sync.db.SyncStateDBO
import com.example.util.simpletimetracker.data_sync.db.SyncStateDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.record.repo.RunningRecordRepo
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

enum class SyncStatus {
    NOT_CONFIGURED,
    SYNCED,
    PENDING,
    OFFLINE,
    ERROR,
}

/**
 * Mirror based sync engine (v1.2, id mapped, running timers synced).
 *
 * Every local entity has a stable server wide unique sync id stored in
 * sync_id_map (a uuid for new entities; entities synced before the id map
 * existed keep their legacy "a<id>"/"e<id>" ids, seeded by the v3 database
 * migration). Local auto increment ids never reach the server, so several
 * devices can sync against the same account without id collisions.
 *
 * Push: only entities changed since the last successful push are sent, plus
 * tombstones for entities deleted locally since then.
 *
 * Pull: server deletions are applied locally, server edits are applied to
 * entities that did not change locally since the last push, and unknown
 * entities (created on the server or on another device) are imported.
 *
 * Running timers sync as time entries with a null end time: a timer started
 * on one device appears as running on every device, and stopping it on any
 * device removes the running entry (the stopping device pushes the finished
 * entry). If a timer is stopped on two devices before one of them syncs,
 * both create a finished entry for the same time span.
 *
 * Known limitations: activity renames coming from the server are not
 * applied because the activity repository has no update (local name wins
 * on the next push); record start time changes from the server are logged
 * to the conflict log instead of applied; stop times use the stopping
 * device's clock, so clock skew between devices shifts durations.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val syncApi: SyncApi,
    private val credentialStore: SyncCredentialStore,
    private val recordTypeRepo: RecordTypeRepo,
    private val recordRepo: RecordRepo,
    private val runningRecordRepo: RunningRecordRepo,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val syncStateDao: SyncStateDao,
    private val syncIdMapDao: SyncIdMapDao,
    private val syncConflictDao: SyncConflictDao,
    private val deltaCalculator: SyncDeltaCalculator,
) {

    private val _status = MutableStateFlow(SyncStatus.NOT_CONFIGURED)
    val status: StateFlow<SyncStatus> = _status

    suspend fun syncNow() {
        if (!credentialStore.isConfigured) {
            _status.value = SyncStatus.NOT_CONFIGURED
            return
        }
        _status.value = SyncStatus.PENDING
        try {
            pushLocalChanges()
            pullServerChanges()
            _status.value = SyncStatus.SYNCED
        } catch (e: Exception) {
            val offline = e is java.io.IOException
            Timber.e(e, "Sync failed")
            _status.value = if (offline) SyncStatus.OFFLINE else SyncStatus.ERROR
        }
    }

    suspend fun clearConflicts() = syncConflictDao.clear()

    private suspend fun pushLocalChanges() {
        val mappings = loadMappings()
        val candidates = buildCandidates(mappings)
        val mirror = syncStateDao.getAll().associate {
            deltaCalculator.key(it.entityType, it.entityId) to it.contentHash
        }
        val delta = deltaCalculator.calculate(candidates, mirror)
        if (delta.upserts.isEmpty() && delta.tombstones.isEmpty()) return

        val items = buildPushItems(delta)
        items.chunked(PUSH_BATCH).forEach { batch ->
            val response = syncApi.push(SyncPushRequest(items = batch))
            response.conflicts.forEach { conflict ->
                syncConflictDao.insert(
                    SyncConflictDBO(
                        entityType = conflict.entity_type,
                        entityId = conflict.id,
                        resolution = conflict.resolution,
                        detail = "",
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        // The tombstoned entities no longer exist locally; drop their
        // mappings so a future local id is never reused for them.
        delta.tombstones.forEach {
            syncIdMapDao.removeBySyncId(it.entityType, it.entityId)
        }
        replaceMirror(candidates)
    }

    private fun buildPushItems(delta: SyncDeltaCalculator.Delta): List<SyncPushItem> {
        val now = nowIso()

        // Running records travel as time entries with a null end time; the
        // server keeps treating them like any other entry.
        fun wireType(entityType: String): String =
            if (entityType == ENTITY_RUNNING_RECORD) ENTITY_TIME_ENTRY else entityType

        val upserts = delta.upserts.map { candidate ->
            SyncPushItem(
                entity_type = wireType(candidate.entityType),
                data = candidate.payload + ("updated_at" to now),
            )
        }
        val tombstones = delta.tombstones.map { candidate ->
            SyncPushItem(
                entity_type = wireType(candidate.entityType),
                data = mapOf(
                    "id" to candidate.entityId,
                    "updated_at" to now,
                    "deleted_at" to now,
                ),
            )
        }
        return upserts + tombstones
    }

    private suspend fun replaceMirror(candidates: List<SyncDeltaCalculator.Candidate>) {
        val now = System.currentTimeMillis()
        syncStateDao.clear()
        syncStateDao.insertAll(
            candidates.map {
                SyncStateDBO(
                    entityType = it.entityType,
                    entityId = it.entityId,
                    contentHash = deltaCalculator.contentHash(it.payload),
                    syncedAt = now,
                )
            },
        )
    }

    private suspend fun buildCandidates(mappings: IdMappings): List<SyncDeltaCalculator.Candidate> {
        val typeCandidates = recordTypeRepo.getAll().map { type ->
            val syncId = mappings.syncIdOf(ENTITY_ACTIVITY, type.id)
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_ACTIVITY,
                entityId = syncId,
                payload = type.toPayloadContent(syncId),
            )
        }
        val recordCandidates = recordRepo.getAll().mapNotNull { record ->
            val syncId = mappings.syncIdOf(ENTITY_TIME_ENTRY, record.id)
            val typeSyncId = mappings.existingSyncId(ENTITY_ACTIVITY, record.typeId)
                ?: return@mapNotNull null.also {
                    Timber.w("Record %s references unknown activity %s", record.id, record.typeId)
                }
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_TIME_ENTRY,
                entityId = syncId,
                payload = record.toPayloadContent(syncId, typeSyncId),
            )
        }
        val runningCandidates = runningRecordRepo.getAll().mapNotNull { running ->
            val typeSyncId = mappings.existingSyncId(ENTITY_ACTIVITY, running.id)
                ?: return@mapNotNull null.also {
                    Timber.w("Running record %s references unknown activity", running.id)
                }
            val syncId = mappings.syncIdOf(ENTITY_RUNNING_RECORD, running.id)
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_RUNNING_RECORD,
                entityId = syncId,
                payload = runningPayloadContent(
                    syncId = syncId,
                    typeSyncId = typeSyncId,
                    timeStarted = running.timeStarted,
                    comment = running.comment,
                ),
            )
        }
        return typeCandidates + recordCandidates + runningCandidates
    }

    private suspend fun pullServerChanges() {
        val since = credentialStore.lastSyncMarker.takeIf { it.isNotEmpty() }
        val pulled = syncApi.pull(since)
        val mappings = loadMappings()
        val mirror = syncStateDao.getAll().associateBy {
            deltaCalculator.key(it.entityType, it.entityId)
        }
        pulled.activities.forEach { activity -> applyServerActivity(activity, mirror, mappings) }
        pulled.time_entries.forEach { entry -> applyServerEntry(entry, mirror, mappings) }
        credentialStore.lastSyncMarker = pulled.server_time ?: nowIso()
    }

    private suspend fun applyServerActivity(
        activity: ActivityDto,
        mirror: Map<String, SyncStateDBO>,
        mappings: IdMappings,
    ) {
        val localId = mappings.localIdOf(ENTITY_ACTIVITY, activity.id)

        if (activity.deleted_at != null) {
            if (localId != null) {
                // The server deleted the activity: remove it locally
                // together with its records and all related mappings.
                recordRepo.getByType(setOf(localId)).forEach { record ->
                    mappings.existingSyncId(ENTITY_TIME_ENTRY, record.id)?.let { recordSyncId ->
                        mappings.forget(ENTITY_TIME_ENTRY, recordSyncId)
                        syncStateDao.remove(ENTITY_TIME_ENTRY, recordSyncId)
                    }
                }
                recordRepo.removeByType(localId)
                recordTypeRepo.remove(localId)
                mappings.forget(ENTITY_ACTIVITY, activity.id)
                syncStateDao.remove(ENTITY_ACTIVITY, activity.id)
            }
            return
        }

        if (localId == null) {
            // Unknown activity: created on the server or another device.
            importActivity(activity, mappings)
            return
        }

        val local = recordTypeRepo.get(localId) ?: return
        val syncId = mappings.existingSyncId(ENTITY_ACTIVITY, localId) ?: return

        val mirrorHash = mirror[deltaCalculator.key(ENTITY_ACTIVITY, syncId)]?.contentHash
        val localHash = deltaCalculator.contentHash(local.toPayloadContent(syncId))
        if (mirrorHash != null && mirrorHash == localHash && activity.name != local.name) {
            // Activity renames from the server cannot be applied because the
            // repository has no update; the local name wins on the next push.
            syncConflictDao.insert(
                SyncConflictDBO(
                    entityType = ENTITY_ACTIVITY,
                    entityId = activity.id,
                    resolution = RESOLUTION_LOCAL_KEPT_NAME,
                    detail = "server name '${activity.name}' was not applied",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
        if (activity.archived && !local.hidden) {
            recordTypeRepo.archive(localId)
        } else if (!activity.archived && local.hidden) {
            recordTypeRepo.restore(localId)
        }
    }

    private suspend fun importActivity(
        activity: ActivityDto,
        mappings: IdMappings,
    ) {
        Timber.i("Importing activity %s from sync", activity.id)
        val localId = recordTypeRepo.add(
            RecordType(
                name = activity.name,
                icon = activity.icon,
                color = AppColor(colorId = 0, colorInt = activity.color),
                defaultDuration = 0,
                note = "",
                hidden = activity.archived,
            ),
        )
        mappings.remember(ENTITY_ACTIVITY, localId, activity.id)
    }

    private suspend fun applyServerEntry(
        entry: TimeEntryDto,
        mirror: Map<String, SyncStateDBO>,
        mappings: IdMappings,
    ) {
        // Entries without an end time are running timers started on
        // another device.
        if (entry.ended_at == null) {
            applyServerRunningEntry(entry, mappings)
            return
        }

        val localId = mappings.localIdOf(ENTITY_TIME_ENTRY, entry.id)

        if (entry.deleted_at != null) {
            if (localId != null) {
                recordRepo.remove(localId)
                mappings.forget(ENTITY_TIME_ENTRY, entry.id)
                syncStateDao.remove(ENTITY_TIME_ENTRY, entry.id)
            }
            return
        }

        if (localId == null) {
            // Unknown entry: created on the server or another device.
            importEntry(entry, mappings)
            return
        }

        val local = recordRepo.get(localId) ?: return
        val syncId = mappings.existingSyncId(ENTITY_TIME_ENTRY, localId) ?: return
        val typeSyncId = mappings.existingSyncId(ENTITY_ACTIVITY, local.typeId) ?: return

        val parsed = entry.toParsed() ?: run {
            Timber.w("Skipping time entry %s: unparsable activity id or timestamps", entry.id)
            return
        }
        val serverTypeId = mappings.localIdOf(ENTITY_ACTIVITY, entry.activity_id)
            ?: run {
                Timber.w("Skipping time entry %s: unknown activity %s", entry.id, entry.activity_id)
                return
            }
        val mirrorHash = mirror[deltaCalculator.key(ENTITY_TIME_ENTRY, syncId)]?.contentHash
        val localHash = deltaCalculator.contentHash(local.toPayloadContent(syncId, typeSyncId))
        // Apply the server version only if the local record did not change
        // since the last push; otherwise the local edit wins and is pushed again.
        if (mirrorHash == null || mirrorHash != localHash) return
        val serverHash = deltaCalculator.contentHash(
            parsed.toPayloadContent(syncId, entry.activity_id),
        )
        if (serverHash == localHash) return
        applyServerRecord(local, serverTypeId, parsed)
        syncStateDao.updateHash(ENTITY_TIME_ENTRY, syncId, serverHash)
    }

    private suspend fun importEntry(
        entry: TimeEntryDto,
        mappings: IdMappings,
    ) {
        val typeLocalId = mappings.localIdOf(ENTITY_ACTIVITY, entry.activity_id)
        if (typeLocalId == null) {
            Timber.w("Cannot import time entry %s: unknown activity %s", entry.id, entry.activity_id)
            return
        }
        val parsed = entry.toParsed() ?: run {
            Timber.w("Skipping time entry %s: unparsable activity id or timestamps", entry.id)
            return
        }
        Timber.i("Importing time entry %s from sync", entry.id)
        val localId = recordRepo.add(
            Record(
                id = 0, // Let the local database allocate an id.
                typeId = typeLocalId,
                timeStarted = parsed.timeStarted,
                timeEnded = parsed.timeEnded,
                comment = parsed.comment,
                tags = emptyList(),
            ),
        )
        mappings.remember(ENTITY_TIME_ENTRY, localId, entry.id)
        syncStateDao.insertAll(
            listOf(
                SyncStateDBO(
                    entityType = ENTITY_TIME_ENTRY,
                    entityId = entry.id,
                    contentHash = deltaCalculator.contentHash(
                        parsed.toPayloadContent(entry.id, entry.activity_id),
                    ),
                    syncedAt = System.currentTimeMillis(),
                ),
            ),
        )
    }

    private suspend fun applyServerRecord(
        local: Record,
        serverTypeId: Long,
        server: ParsedEntry,
    ) {
        if (serverTypeId != local.typeId || server.comment != local.comment) {
            recordRepo.update(
                recordId = local.id,
                typeId = serverTypeId,
                comment = server.comment,
                tags = local.tags,
            )
        }
        if (server.timeEnded != local.timeEnded) {
            recordRepo.updateTimeEnded(local.id, server.timeEnded)
        }
        if (server.timeStarted != local.timeStarted) {
            // The record repository cannot change a start time; keep the
            // local value and leave a trace in the conflict log.
            syncConflictDao.insert(
                SyncConflictDBO(
                    entityType = ENTITY_TIME_ENTRY,
                    entityId = "e${local.id}",
                    resolution = RESOLUTION_LOCAL_KEPT_START,
                    detail = "server start time was not applied",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun RecordType.toPayloadContent(syncId: String): Map<String, Any?> = mapOf(
        "id" to syncId,
        "name" to name,
        "icon" to icon,
        "color" to color.colorInt,
        "archived" to hidden,
    )

    private fun Record.toPayloadContent(
        syncId: String,
        typeSyncId: String,
    ): Map<String, Any?> = entryPayloadContent(
        syncId = syncId,
        typeSyncId = typeSyncId,
        timeStarted = timeStarted,
        timeEnded = timeEnded,
        comment = comment,
    )

    private fun ParsedEntry.toPayloadContent(
        syncId: String,
        typeSyncId: String,
    ): Map<String, Any?> = entryPayloadContent(
        syncId = syncId,
        typeSyncId = typeSyncId,
        timeStarted = timeStarted,
        timeEnded = timeEnded,
        comment = comment,
    )

    /**
     * Running timers sync as time entries with a null end time. A deletion
     * means another device stopped the timer (that device also pushed the
     * finished entry, which arrives through the regular entry path); the
     * local running record is then removed without creating a record.
     */
    private suspend fun applyServerRunningEntry(
        entry: TimeEntryDto,
        mappings: IdMappings,
    ) {
        val localTypeId = mappings.localIdOf(ENTITY_RUNNING_RECORD, entry.id)

        if (entry.deleted_at != null) {
            if (localTypeId != null) {
                if (runningRecordRepo.has(localTypeId)) {
                    removeRunningRecordMediator.remove(typeId = localTypeId)
                }
                mappings.forget(ENTITY_RUNNING_RECORD, entry.id)
                syncStateDao.remove(ENTITY_RUNNING_RECORD, entry.id)
            }
            return
        }

        if (localTypeId != null) {
            // Already running locally; the repository has no update for a
            // running start time, so keep the local value.
            return
        }

        val typeLocalId = mappings.localIdOf(ENTITY_ACTIVITY, entry.activity_id)
            ?: run {
                Timber.w("Cannot import running record %s: unknown activity %s", entry.id, entry.activity_id)
                return
            }
        val timeStarted = parseEpochMilli(entry.started_at) ?: return
        Timber.i("Importing running record %s from sync", entry.id)
        addRunningRecordMediator.startTimer(
            typeId = typeLocalId,
            tags = emptyList(),
            comment = entry.comment,
            timeStarted = AddRunningRecordMediator.StartTime.Timestamp(timeStarted),
            checkDefaultDuration = false,
        )
        mappings.remember(ENTITY_RUNNING_RECORD, typeLocalId, entry.id)
        syncStateDao.insertAll(
            listOf(
                SyncStateDBO(
                    entityType = ENTITY_RUNNING_RECORD,
                    entityId = entry.id,
                    contentHash = deltaCalculator.contentHash(
                        runningPayloadContent(
                            syncId = entry.id,
                            typeSyncId = entry.activity_id,
                            timeStarted = timeStarted,
                            comment = entry.comment,
                        ),
                    ),
                    syncedAt = System.currentTimeMillis(),
                ),
            ),
        )
    }

    private fun entryPayloadContent(
        syncId: String,
        typeSyncId: String,
        timeStarted: Long,
        timeEnded: Long,
        comment: String,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "activity_id" to typeSyncId,
        "started_at" to format(timeStarted),
        "ended_at" to if (timeEnded > 0) format(timeEnded) else null,
        "duration_seconds" to ((timeEnded - timeStarted) / 1000).toInt(),
        "comment" to comment,
    )

    private fun runningPayloadContent(
        syncId: String,
        typeSyncId: String,
        timeStarted: Long,
        comment: String,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "activity_id" to typeSyncId,
        "started_at" to format(timeStarted),
        "ended_at" to null,
        "duration_seconds" to 0,
        "comment" to comment,
    )

    private fun TimeEntryDto.toParsed(): ParsedEntry? {
        if (activity_id.isEmpty()) return null
        val timeStarted = parseEpochMilli(started_at) ?: return null
        val timeEnded = ended_at?.let { parseEpochMilli(it) ?: return null } ?: 0L
        return ParsedEntry(
            timeStarted = timeStarted,
            timeEnded = timeEnded,
            comment = comment,
        )
    }

    private fun parseEpochMilli(iso: String): Long? = runCatching {
        // The server sends timestamps with an explicit offset (+00:00)
        // while the app pushes Instant.toString with Z; accept both.
        OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(iso).toEpochMilli()
    }.getOrNull()

    private suspend fun loadMappings(): IdMappings {
        val mappings = IdMappings()
        syncIdMapDao.getAll().forEach { row ->
            mappings.byLocal[row.entityType to row.localId] = row.syncId
            mappings.bySync[row.entityType to row.syncId] = row.localId
        }
        return mappings
    }

    private inner class IdMappings {
        val byLocal = mutableMapOf<Pair<String, Long>, String>()
        val bySync = mutableMapOf<Pair<String, String>, Long>()

        suspend fun syncIdOf(entityType: String, localId: Long): String {
            return existingSyncId(entityType, localId) ?: UUID.randomUUID().toString().also { syncId ->
                remember(entityType, localId, syncId)
            }
        }

        fun existingSyncId(entityType: String, localId: Long): String? = byLocal[entityType to localId]

        fun localIdOf(entityType: String, syncId: String): Long? = bySync[entityType to syncId]

        suspend fun remember(entityType: String, localId: Long, syncId: String) {
            syncIdMapDao.insert(
                SyncIdMapDBO(
                    entityType = entityType,
                    localId = localId,
                    syncId = syncId,
                ),
            )
            byLocal[entityType to localId] = syncId
            bySync[entityType to syncId] = localId
        }

        suspend fun forget(entityType: String, syncId: String) {
            syncIdMapDao.removeBySyncId(entityType, syncId)
            bySync.remove(entityType to syncId)?.let { localId ->
                byLocal.remove(entityType to localId)
            }
        }
    }

    private data class ParsedEntry(
        val timeStarted: Long,
        val timeEnded: Long,
        val comment: String,
    )

    companion object {
        private const val PUSH_BATCH = 200
        private const val ENTITY_ACTIVITY = "activity"
        private const val ENTITY_TIME_ENTRY = "time_entry"
        private const val ENTITY_RUNNING_RECORD = "running_record"
        private const val RESOLUTION_LOCAL_KEPT_NAME = "local_kept_name"
        private const val RESOLUTION_LOCAL_KEPT_START = "local_kept_start_time"

        private fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        private fun format(epochMilli: Long): String =
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMilli))
    }
}
