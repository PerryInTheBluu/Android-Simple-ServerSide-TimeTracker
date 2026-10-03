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
import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.repo.RecordRepo
import com.example.util.simpletimetracker.domain.record.repo.RunningRecordRepo
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTagValueType
import com.example.util.simpletimetracker.domain.recordTag.repo.RecordTagRepo
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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
    private val categoryRepo: CategoryRepo,
    private val recordTypeCategoryRepo: RecordTypeCategoryRepo,
    private val recordTagRepo: RecordTagRepo,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val syncStateDao: SyncStateDao,
    private val syncIdMapDao: SyncIdMapDao,
    private val syncConflictDao: SyncConflictDao,
    private val deltaCalculator: SyncDeltaCalculator,
    private val moshi: Moshi,
) {

    private val _status = MutableStateFlow(SyncStatus.NOT_CONFIGURED)

    private val tagListAdapter by lazy {
        moshi.adapter<List<Map<String, Any?>>>(
            Types.newParameterizedType(
                List::class.java,
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
            ),
        )
    }

    private val stringListAdapter by lazy {
        moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java),
        )
    }
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
        val categoryCandidates = categoryRepo.getAll().map { category ->
            val syncId = mappings.syncIdOf(ENTITY_CATEGORY, category.id)
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_CATEGORY,
                entityId = syncId,
                payload = categoryPayloadContent(
                    syncId = syncId,
                    name = category.name,
                    color = category.color.colorInt,
                    colorId = category.color.colorId,
                    note = category.note,
                ),
            )
        }
        val tagCandidates = recordTagRepo.getAll().map { tag ->
            val syncId = mappings.syncIdOf(ENTITY_RECORD_TAG, tag.id)
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_RECORD_TAG,
                entityId = syncId,
                payload = tagPayloadContent(
                    syncId = syncId,
                    name = tag.name,
                    icon = tag.icon,
                    color = tag.color.colorInt,
                    colorId = tag.color.colorId,
                    iconColorSource = tag.iconColorSource,
                    note = tag.note,
                    archived = tag.archived,
                    valueType = tag.valueType.name,
                    valueSuffix = tag.valueSuffix,
                ),
            )
        }
        val typeCandidates = recordTypeRepo.getAll().map { type ->
            val syncId = mappings.syncIdOf(ENTITY_ACTIVITY, type.id)
            val categorySyncIds = recordTypeCategoryRepo.getCategoryIdsByType(type.id)
                .map { mappings.syncIdOf(ENTITY_CATEGORY, it) }
                .sorted()
            SyncDeltaCalculator.Candidate(
                entityType = ENTITY_ACTIVITY,
                entityId = syncId,
                payload = type.toPayloadContent(syncId, categorySyncIds),
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
                payload = record.toPayloadContent(syncId, typeSyncId, tagsPayload(record.tags, mappings)),
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
                    tags = tagsPayload(running.tags, mappings),
                ),
            )
        }
        return categoryCandidates + tagCandidates + typeCandidates + recordCandidates + runningCandidates
    }

    private suspend fun pullServerChanges() {
        val since = credentialStore.lastSyncMarker.takeIf { it.isNotEmpty() }
        val pulled = syncApi.pull(since)
        val mappings = loadMappings()
        val mirror = syncStateDao.getAll().associateBy {
            deltaCalculator.key(it.entityType, it.entityId)
        }
        // Categories and tags first: activities and entries reference them.
        pulled.categories.forEach { data -> applyServerCategory(data, mappings) }
        pulled.tags.forEach { data -> applyServerTag(data, mappings) }
        pulled.activities.forEach { activity -> applyServerActivity(activity, mirror, mappings) }
        pulled.time_entries.forEach { entry -> applyServerEntry(entry, mirror, mappings) }
        credentialStore.lastSyncMarker = pulled.server_time ?: nowIso()
    }

    private suspend fun applyServerCategory(
        data: Map<String, Any?>,
        mappings: IdMappings,
    ) {
        val syncId = data["id"] as? String ?: return
        val localId = mappings.localIdOf(ENTITY_CATEGORY, syncId)

        if (data["deleted_at"] != null) {
            if (localId != null) {
                recordTypeCategoryRepo.removeAll(categoryId = localId)
                categoryRepo.remove(localId)
                mappings.forget(ENTITY_CATEGORY, syncId)
                syncStateDao.remove(ENTITY_CATEGORY, syncId)
            }
            return
        }

        if (localId != null) {
            // Known category: the repository has no update, so the local
            // version wins and is re-pushed on the next sync.
            return
        }

        val name = data["name"] as? String ?: return
        val color = data["color"] as? String ?: ""
        val colorId = (data["color_id"] as? Double)?.toInt() ?: 0
        val note = data["note"] as? String ?: ""
        Timber.i("Importing category %s from sync", syncId)
        val newLocalId = categoryRepo.add(
            Category(
                name = name,
                color = AppColor(colorId = colorId, colorInt = color),
                note = note,
            ),
        )
        mappings.remember(ENTITY_CATEGORY, newLocalId, syncId)
        insertMirror(ENTITY_CATEGORY, syncId, categoryPayloadContent(syncId, name, color, colorId, note))
    }

    private suspend fun applyServerTag(
        data: Map<String, Any?>,
        mappings: IdMappings,
    ) {
        val syncId = data["id"] as? String ?: return
        val localId = mappings.localIdOf(ENTITY_RECORD_TAG, syncId)

        if (data["deleted_at"] != null) {
            if (localId != null) {
                recordTagRepo.remove(localId)
                mappings.forget(ENTITY_RECORD_TAG, syncId)
                syncStateDao.remove(ENTITY_RECORD_TAG, syncId)
            }
            return
        }

        if (localId != null) {
            // Known tag: the repository has no update, so the local
            // version wins and is re-pushed on the next sync.
            return
        }

        val name = data["name"] as? String ?: return
        val valueType = (data["value_type"] as? String)
            ?.let { type -> RecordTagValueType.entries.firstOrNull { it.name == type } }
            ?: RecordTagValueType.NONE
        Timber.i("Importing record tag %s from sync", syncId)
        val newLocalId = recordTagRepo.add(
            RecordTag(
                name = name,
                icon = data["icon"] as? String ?: "",
                color = AppColor(
                    colorId = (data["color_id"] as? Double)?.toInt() ?: 0,
                    colorInt = data["color"] as? String ?: "",
                ),
                iconColorSource = (data["icon_color_source"] as? Double)?.toLong() ?: 0L,
                note = data["note"] as? String ?: "",
                archived = data["archived"] as? Boolean ?: false,
                valueType = valueType,
                valueSuffix = data["value_suffix"] as? String ?: "",
            ),
        )
        mappings.remember(ENTITY_RECORD_TAG, newLocalId, syncId)
        insertMirror(
            ENTITY_RECORD_TAG,
            syncId,
            tagPayloadContent(
                syncId = syncId,
                name = name,
                icon = data["icon"] as? String ?: "",
                color = data["color"] as? String ?: "",
                colorId = (data["color_id"] as? Double)?.toInt() ?: 0,
                iconColorSource = (data["icon_color_source"] as? Double)?.toLong() ?: 0L,
                note = data["note"] as? String ?: "",
                archived = data["archived"] as? Boolean ?: false,
                valueType = valueType.name,
                valueSuffix = data["value_suffix"] as? String ?: "",
            ),
        )
    }

    private suspend fun insertMirror(
        entityType: String,
        syncId: String,
        payload: Map<String, Any?>,
    ) {
        syncStateDao.insertAll(
            listOf(
                SyncStateDBO(
                    entityType = entityType,
                    entityId = syncId,
                    contentHash = deltaCalculator.contentHash(payload),
                    syncedAt = System.currentTimeMillis(),
                ),
            ),
        )
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

        val localCategorySyncIds = recordTypeCategoryRepo.getCategoryIdsByType(localId)
            .mapNotNull { mappings.existingSyncId(ENTITY_CATEGORY, it) }
            .sorted()
        val localHash = deltaCalculator.contentHash(local.toPayloadContent(syncId, localCategorySyncIds))
        val mirrorHash = mirror[deltaCalculator.key(ENTITY_ACTIVITY, syncId)]?.contentHash
        if (mirrorHash != null && mirrorHash == localHash) {
            if (activity.name != local.name) {
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
            // Apply server side category assignment changes.
            val serverCategoryIds = decodeStringList(activity.category)
                .mapNotNull { mappings.localIdOf(ENTITY_CATEGORY, it) }
            val localCategoryIds = recordTypeCategoryRepo.getCategoryIdsByType(localId)
            val toAdd = (serverCategoryIds - localCategoryIds.toSet()).toList()
            val toRemove = (localCategoryIds - serverCategoryIds.toSet()).toList()
            if (toAdd.isNotEmpty()) recordTypeCategoryRepo.addCategories(localId, toAdd)
            if (toRemove.isNotEmpty()) recordTypeCategoryRepo.removeCategories(localId, toRemove)
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
                // Predefined app colors live in colorId; without it every
                // imported activity would fall back to the same fallback
                // color on the receiving device.
                color = AppColor(colorId = activity.color_id, colorInt = activity.color),
                defaultDuration = 0,
                note = "",
                hidden = activity.archived,
            ),
        )
        mappings.remember(ENTITY_ACTIVITY, localId, activity.id)
        val categoryIds = decodeStringList(activity.category)
            .mapNotNull { mappings.localIdOf(ENTITY_CATEGORY, it) }
        if (categoryIds.isNotEmpty()) recordTypeCategoryRepo.addCategories(localId, categoryIds)
        insertMirror(
            ENTITY_ACTIVITY,
            activity.id,
            mapOf(
                "id" to activity.id,
                "name" to activity.name,
                "icon" to activity.icon,
                "color" to activity.color,
                "color_id" to activity.color_id,
                "archived" to activity.archived,
                "category" to encodeStringList(decodeStringList(activity.category).sorted()),
            ),
        )
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
        val localHash = deltaCalculator.contentHash(
            local.toPayloadContent(syncId, typeSyncId, tagsPayload(local.tags, mappings)),
        )
        // Apply the server version only if the local record did not change
        // since the last push; otherwise the local edit wins and is pushed again.
        if (mirrorHash == null || mirrorHash != localHash) return
        val serverTags = localizeTags(parsed.tags, mappings)
        val serverHash = deltaCalculator.contentHash(
            parsed.toPayloadContent(syncId, entry.activity_id, parsed.tags),
        )
        if (serverHash == localHash) return
        applyServerRecord(local, serverTypeId, parsed, serverTags)
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
                tags = localizeTags(parsed.tags, mappings),
            ),
        )
        mappings.remember(ENTITY_TIME_ENTRY, localId, entry.id)
        syncStateDao.insertAll(
            listOf(
                SyncStateDBO(
                    entityType = ENTITY_TIME_ENTRY,
                    entityId = entry.id,
                    contentHash = deltaCalculator.contentHash(
                        parsed.toPayloadContent(entry.id, entry.activity_id, parsed.tags),
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
        serverTags: List<RecordBase.Tag>,
    ) {
        if (serverTypeId != local.typeId || server.comment != local.comment || serverTags != local.tags) {
            recordRepo.update(
                recordId = local.id,
                typeId = serverTypeId,
                comment = server.comment,
                tags = serverTags,
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

    private fun RecordType.toPayloadContent(
        syncId: String,
        categorySyncIds: List<String>,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "name" to name,
        "icon" to icon,
        "color" to color.colorInt,
        "color_id" to color.colorId,
        "archived" to hidden,
        // Stored in the server's free text category column as a json list.
        "category" to encodeStringList(categorySyncIds),
    )

    private fun Record.toPayloadContent(
        syncId: String,
        typeSyncId: String,
        tags: List<Map<String, Any?>>,
    ): Map<String, Any?> = entryPayloadContent(
        syncId = syncId,
        typeSyncId = typeSyncId,
        timeStarted = timeStarted,
        timeEnded = timeEnded,
        comment = comment,
        tags = tags,
    )

    private fun ParsedEntry.toPayloadContent(
        syncId: String,
        typeSyncId: String,
        tags: List<Map<String, Any?>>,
    ): Map<String, Any?> = entryPayloadContent(
        syncId = syncId,
        typeSyncId = typeSyncId,
        timeStarted = timeStarted,
        timeEnded = timeEnded,
        comment = comment,
        tags = tags,
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
        val parsedTags = decodeTags(entry.tags)
        val localTags = localizeTags(parsedTags, mappings)
        Timber.i("Importing running record %s from sync", entry.id)
        addRunningRecordMediator.startTimer(
            typeId = typeLocalId,
            tags = localTags,
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
                            tags = parsedTags,
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
        tags: List<Map<String, Any?>>,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "activity_id" to typeSyncId,
        "started_at" to format(timeStarted),
        "ended_at" to if (timeEnded > 0) format(timeEnded) else null,
        "duration_seconds" to ((timeEnded - timeStarted) / 1000).toInt(),
        "comment" to comment,
        // Stored in the server's free text tags column as a json list.
        "tags" to encodeTags(tags),
    )

    private fun runningPayloadContent(
        syncId: String,
        typeSyncId: String,
        timeStarted: Long,
        comment: String,
        tags: List<Map<String, Any?>>,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "activity_id" to typeSyncId,
        "started_at" to format(timeStarted),
        "ended_at" to null,
        "duration_seconds" to 0,
        "comment" to comment,
        // Stored in the server's free text tags column as a json list.
        "tags" to encodeTags(tags),
    )

    private fun categoryPayloadContent(
        syncId: String,
        name: String,
        color: String,
        colorId: Int,
        note: String,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "name" to name,
        "color" to color,
        "color_id" to colorId,
        "note" to note,
    )

    private fun tagPayloadContent(
        syncId: String,
        name: String,
        icon: String,
        color: String,
        colorId: Int,
        iconColorSource: Long,
        note: String,
        archived: Boolean,
        valueType: String,
        valueSuffix: String,
    ): Map<String, Any?> = mapOf(
        "id" to syncId,
        "name" to name,
        "icon" to icon,
        "color" to color,
        "color_id" to colorId,
        "icon_color_source" to iconColorSource,
        "note" to note,
        "archived" to archived,
        "value_type" to valueType,
        "value_suffix" to valueSuffix,
    )

    /**
     * Tag list for payloads: one entry per tag with its sync id and the
     * optional numeric value, sorted by sync id for stable hashes.
     */
    private suspend fun tagsPayload(
        tags: List<RecordBase.Tag>,
        mappings: IdMappings,
    ): List<Map<String, Any?>> {
        return tags.mapNotNull { tag ->
            mappings.syncIdOf(ENTITY_RECORD_TAG, tag.tagId).let { syncId ->
                mapOf("id" to syncId, "value" to tag.numericValue)
            }
        }.sortedBy { it["id"] as String }
    }

    private fun localizeTags(
        tags: List<Map<String, Any?>>,
        mappings: IdMappings,
    ): List<RecordBase.Tag> {
        return tags.mapNotNull { tag ->
            val syncId = tag["id"] as? String ?: return@mapNotNull null
            val localId = mappings.localIdOf(ENTITY_RECORD_TAG, syncId) ?: return@mapNotNull null
            RecordBase.Tag(
                tagId = localId,
                numericValue = (tag["value"] as? Double),
            )
        }
    }

    private fun TimeEntryDto.toParsed(): ParsedEntry? {
        if (activity_id.isEmpty()) return null
        val timeStarted = parseEpochMilli(started_at) ?: return null
        val timeEnded = ended_at?.let { parseEpochMilli(it) ?: return null } ?: 0L
        return ParsedEntry(
            timeStarted = timeStarted,
            timeEnded = timeEnded,
            comment = comment,
            tags = decodeTags(tags),
        )
    }

    private fun encodeTags(tags: List<Map<String, Any?>>): String {
        return tagListAdapter.toJson(tags)
    }

    private fun decodeTags(json: String): List<Map<String, Any?>> {
        if (json.isEmpty()) return emptyList()
        return runCatching { tagListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    private fun encodeStringList(values: List<String>): String {
        return stringListAdapter.toJson(values)
    }

    private fun decodeStringList(json: String): List<String> {
        if (json.isEmpty()) return emptyList()
        return runCatching { stringListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
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
        val tags: List<Map<String, Any?>> = emptyList(),
    )

    companion object {
        private const val PUSH_BATCH = 200
        private const val ENTITY_ACTIVITY = "activity"
        private const val ENTITY_TIME_ENTRY = "time_entry"
        private const val ENTITY_RUNNING_RECORD = "running_record"
        private const val ENTITY_CATEGORY = "category"
        private const val ENTITY_RECORD_TAG = "record_tag"
        private const val RESOLUTION_LOCAL_KEPT_NAME = "local_kept_name"
        private const val RESOLUTION_LOCAL_KEPT_START = "local_kept_start_time"

        private fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        private fun format(epochMilli: Long): String =
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMilli))
    }
}
