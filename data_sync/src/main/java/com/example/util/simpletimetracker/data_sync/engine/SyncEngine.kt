package com.example.util.simpletimetracker.data_sync.engine

import com.example.util.simpletimetracker.data_sync.api.SyncPushItem
import com.example.util.simpletimetracker.data_sync.api.SyncPushRequest
import com.example.util.simpletimetracker.data_sync.api.SyncApi
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDao
import com.example.util.simpletimetracker.data_sync.db.SyncConflictDBO
import com.example.util.simpletimetracker.data_sync.db.SyncQueueDao
import com.example.util.simpletimetracker.data_sync.keystore.SyncCredentialStore
import com.example.util.simpletimetracker.data_sync.keystore.normalizeServerUrlOrNull
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    NOT_CONFIGURED,
    SYNCED,
    PENDING,
    OFFLINE,
    ERROR,
}

/**
 * Conservative v1 sync engine.
 *
 * Local activities and records are mapped to stable server ids
 * ("a<localId>" / "e<localRecordId>"). On conflict (same id) the newer
 * updated_at wins; losers are written to the local conflict log.
 * The running timer is never touched by sync.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val syncApi: Lazy<SyncApi>,
    private val credentialStore: SyncCredentialStore,
    private val recordTypeRepo: RecordTypeRepo,
    private val recordRepo: RecordRepo,
    private val syncQueueDao: SyncQueueDao,
    private val syncConflictDao: SyncConflictDao,
    private val moshi: Moshi,
) {

    private val _status = MutableStateFlow(SyncStatus.NOT_CONFIGURED)
    val status: StateFlow<SyncStatus> = _status

    private val payloadAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    suspend fun syncNow() {
        val baseUrl = normalizeServerUrlOrNull(credentialStore.serverUrl)
        if (!credentialStore.isConfigured || baseUrl == null) {
            _status.value = SyncStatus.NOT_CONFIGURED
            return
        }
        _status.value = SyncStatus.PENDING
        try {
            val syncApi = syncApi.get()
            pushLocalState(syncApi)
            pullServerState(syncApi)
            credentialStore.lastSyncTime = System.currentTimeMillis()
            _status.value = SyncStatus.SYNCED
        } catch (e: Exception) {
            val offline = e is java.io.IOException
            _status.value = if (offline) SyncStatus.OFFLINE else SyncStatus.ERROR
        }
    }

    private suspend fun pushLocalState(syncApi: SyncApi) {
        val items = mutableListOf<SyncPushItem>()
        recordTypeRepo.getAll().forEach { type ->
            items.add(
                SyncPushItem(
                    entity_type = "activity",
                    data = mapOf(
                        "id" to "a${type.id}",
                        "name" to type.name,
                        "icon" to type.icon,
                        "archived" to type.hidden,
                        "updated_at" to nowIso(),
                    ),
                ),
            )
        }
        recordRepo.getAll().forEach { record ->
            items.add(
                SyncPushItem(
                    entity_type = "time_entry",
                    data = record.toPushPayload(),
                ),
            )
        }
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
    }

    private suspend fun pullServerState(syncApi: SyncApi) {
        val since = credentialStore.lastSyncTime
            .takeIf { it > 0 }
            ?.let { DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it)) }
        val pulled = syncApi.pull(since)
        // Server data is authoritative for entries not present locally.
        // Local-first: existing local records are never overwritten here,
        // because push already transferred the newer local state.
        pulled.time_entries
            .filter { it.deleted_at == null }
            .forEach { entry ->
                val localRecordId = entry.id.removePrefix("e").toLongOrNull() ?: return@forEach
                val exists = recordRepo.get(localRecordId) != null
                if (!exists) {
                    val localTypeId = entry.activity_id.removePrefix("a").toLongOrNull() ?: return@forEach
                    val typeExists = recordTypeRepo.get(localTypeId) != null
                    if (typeExists) {
                        recordRepo.add(
                            Record(
                                id = localRecordId,
                                typeId = localTypeId,
                                timeStarted = parseEpochMilli(entry.started_at),
                                timeEnded = entry.ended_at?.let(::parseEpochMilli) ?: 0L,
                                comment = entry.comment,
                                tags = emptyList(),
                            ),
                        )
                    }
                }
            }
    }

    suspend fun clearConflicts() = syncConflictDao.clear()

    private fun Record.toPushPayload(): Map<String, Any?> {
        return mapOf(
            "id" to "e$id",
            "activity_id" to "a$typeId",
            "started_at" to DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timeStarted)),
            "ended_at" to if (timeEnded > 0) {
                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timeEnded))
            } else {
                null
            },
            "duration_seconds" to ((timeEnded - timeStarted) / 1000).toInt(),
            "comment" to comment,
            "updated_at" to nowIso(),
        )
    }

    private fun parseEpochMilli(iso: String): Long = runCatching {
        Instant.parse(iso).toEpochMilli()
    }.getOrElse { Instant.now().toEpochMilli() }

    companion object {
        private const val PUSH_BATCH = 200
        private fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    }
}
