package com.example.util.simpletimetracker.data_sync.engine

import com.example.util.simpletimetracker.data_sync.api.ActivityDto
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.api.SyncPushItem
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.example.util.simpletimetracker.data_sync.api.TimeEntryDto
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDBO
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncStateDBO
import com.example.util.simpletimetracker.data_sync.db.SyncStateDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
 * Mirror based sync engine (v1.1).
 *
 * Push: only entities changed since the last successful push are sent, plus
 * tombstones for entities deleted locally since then. This propagates local
 * deletions to the server instead of resurrecting them on the next pull.
 *
 * Pull: server deletions are applied locally, server edits are applied to
 * entities that did not change locally since the last push, and entities
 * unknown locally are imported.
 *
 * Entities changed locally since the last push always win and are re-pushed.
 * The running timer is never touched by sync.
 *
 * Known limitations: entity ids map to local database ids ("a<id>" for
 * activities, "e<id>" for time entries), so entities created on the server
 * (uuid ids) are ignored by the pull, and activity renames coming from the
 * server are not applied because the activity repository has no update.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val syncApi: SyncApi,
    private val credentialStore: SyncCredentialStore,
    private val recordTypeRepo: RecordTypeRepo,
    private val recordRepo: RecordRepo,
    private val syncStateDao: SyncStateDao,
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
            _status.value = if (offline) SyncStatus.OFFLINE else SyncStatus.ERROR
        }
    }

    suspend fun clearConflicts() = syncConflictDao.clear()

    private suspend fun pushLocalChanges() {
        val candidates = buildCandidates()
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
        replaceMirror(candidates)
    }

    private fun buildPushItems(delta: SyncDeltaCalculator.Delta): List<SyncPushItem> {
        val now = nowIso()
        val upserts = delta.upserts.map { candidate ->
            SyncPushItem(
                entity_type = candidate.entityType,
                data = candidate.payload + ("updated_at" to now),
            )
        }
        val tombstones = delta.tombstones.map { candidate ->
            SyncPushItem(
                entity_type = candidate.entityType,
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

    private suspend fun buildCandidates(): List<SyncDeltaCalculator.Candidate> {
        val typeCandidates = recordTypeRepo.getAll().map { it.toCandidate() }
        val recordCandidates = recordRepo.getAll().map { it.toCandidate() }
        return typeCandidates + recordCandidates
    }

    private fun RecordType.toCandidate(): SyncDeltaCalculator.Candidate = SyncDeltaCalculator.Candidate(
        entityType = ENTITY_ACTIVITY,
        entityId = "a$id",
        payload = mapOf(
            "id" to "a$id",
            "name" to name,
            "icon" to icon,
            "color" to color.colorInt,
            "archived" to hidden,
        ),
    )

    private fun Record.toCandidate(): SyncDeltaCalculator.Candidate = SyncDeltaCalculator.Candidate(
        entityType = ENTITY_TIME_ENTRY,
        entityId = "e$id",
        payload = toPayloadContent(),
    )

    /**
     * Wire payload without updated_at. Also used for hash comparisons
     * between local records, the mirror and server entries.
     */
    private fun Record.toPayloadContent(): Map<String, Any?> = mapOf(
        "id" to "e$id",
        "activity_id" to "a$typeId",
        "started_at" to format(timeStarted),
        "ended_at" to if (timeEnded > 0) format(timeEnded) else null,
        "duration_seconds" to ((timeEnded - timeStarted) / 1000).toInt(),
        "comment" to comment,
    )

    private suspend fun pullServerChanges() {
        val since = credentialStore.lastSyncMarker.takeIf { it.isNotEmpty() }
        val pulled = syncApi.pull(since)
        val mirror = syncStateDao.getAll().associateBy {
            deltaCalculator.key(it.entityType, it.entityId)
        }
        pulled.activities.forEach { activity -> applyServerActivity(activity, mirror) }
        pulled.time_entries.forEach { entry -> applyServerEntry(entry, mirror) }
        credentialStore.lastSyncMarker = pulled.server_time ?: nowIso()
    }

    private suspend fun applyServerActivity(
        activity: ActivityDto,
        mirror: Map<String, SyncStateDBO>,
    ) {
        val localId = activity.id.removePrefix("a").toLongOrNull() ?: return
        val local = recordTypeRepo.get(localId) ?: return

        if (activity.deleted_at != null) {
            // The server deleted the activity: remove it locally together
            // with its records. The removed records propagate to the server
            // as tombstones on the next push.
            recordRepo.removeByType(localId)
            recordTypeRepo.remove(localId)
            syncStateDao.remove(ENTITY_ACTIVITY, activity.id)
            return
        }

        val mirrorHash = mirror[deltaCalculator.key(ENTITY_ACTIVITY, activity.id)]?.contentHash
        val localHash = deltaCalculator.contentHash(local.toCandidate().payload)
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

    private suspend fun applyServerEntry(
        entry: TimeEntryDto,
        mirror: Map<String, SyncStateDBO>,
    ) {
        val localId = entry.id.removePrefix("e").toLongOrNull() ?: return
        val local = recordRepo.get(localId)

        if (entry.deleted_at != null) {
            if (local != null) {
                recordRepo.remove(localId)
                syncStateDao.remove(ENTITY_TIME_ENTRY, entry.id)
            }
            return
        }
        if (local == null) {
            importEntry(entry, localId)
            return
        }

        val serverRecord = entry.toRecord(localId) ?: run {
            Timber.w("Skipping time entry %s: unparsable activity id or timestamps", entry.id)
            return
        }
        val mirrorHash = mirror[deltaCalculator.key(ENTITY_TIME_ENTRY, entry.id)]?.contentHash
        val localHash = deltaCalculator.contentHash(local.toPayloadContent())
        // Apply the server version only if the local record did not change
        // since the last push; otherwise the local edit wins and is pushed again.
        if (mirrorHash == null || mirrorHash != localHash) return
        val serverHash = deltaCalculator.contentHash(serverRecord.toPayloadContent())
        if (serverHash == localHash) return
        applyServerRecord(local, serverRecord)
        syncStateDao.updateHash(ENTITY_TIME_ENTRY, entry.id, serverHash)
    }

    private suspend fun importEntry(entry: TimeEntryDto, localId: Long) {
        val record = entry.toRecord(localId) ?: return
        if (recordTypeRepo.get(record.typeId) == null) return
        recordRepo.add(record)
        syncStateDao.insertAll(
            listOf(
                SyncStateDBO(
                    entityType = ENTITY_TIME_ENTRY,
                    entityId = entry.id,
                    contentHash = deltaCalculator.contentHash(record.toPayloadContent()),
                    syncedAt = System.currentTimeMillis(),
                ),
            ),
        )
    }

    private suspend fun applyServerRecord(local: Record, server: Record) {
        if (server.typeId != local.typeId || server.comment != local.comment) {
            recordRepo.update(
                recordId = local.id,
                typeId = server.typeId,
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

    private fun TimeEntryDto.toRecord(localId: Long): Record? {
        val typeId = activity_id.removePrefix("a").toLongOrNull() ?: return null
        val timeStarted = parseEpochMilli(started_at) ?: return null
        val timeEnded = ended_at?.let { parseEpochMilli(it) ?: return null } ?: 0L
        return Record(
            id = localId,
            typeId = typeId,
            timeStarted = timeStarted,
            timeEnded = timeEnded,
            comment = comment,
            tags = emptyList(),
        )
    }

    private fun parseEpochMilli(iso: String): Long? = runCatching {
        // The server sends timestamps with an explicit offset (+00:00)
        // while the app pushes Instant.toString with Z; accept both.
        OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(iso).toEpochMilli()
    }.getOrNull()

    companion object {
        private const val PUSH_BATCH = 200
        private const val ENTITY_ACTIVITY = "activity"
        private const val ENTITY_TIME_ENTRY = "time_entry"
        private const val RESOLUTION_LOCAL_KEPT_NAME = "local_kept_name"
        private const val RESOLUTION_LOCAL_KEPT_START = "local_kept_start_time"

        private fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        private fun format(epochMilli: Long): String =
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMilli))
    }
}
