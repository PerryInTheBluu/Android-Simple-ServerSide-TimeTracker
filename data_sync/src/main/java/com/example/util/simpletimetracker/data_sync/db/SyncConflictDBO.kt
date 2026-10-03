package com.example.util.simpletimetracker.data_sync.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_state",
    indices = [Index(value = ["entity_type", "entity_id"], unique = true)],
)
data class SyncStateDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "content_hash")
    val contentHash: String,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long,
)

/**
 * Stable sync ids per local entity. Locally allocated auto increment ids
 * would collide between devices, so every entity gets a server wide unique
 * sync id (a uuid, or the legacy "a<id>"/"e<id>" for entities synced before
 * this table existed - the v3 migration seeds those from the mirror).
 */
@Entity(
    tableName = "sync_id_map",
    indices = [
        Index(value = ["entity_type", "local_id"], unique = true),
        Index(value = ["entity_type", "sync_id"], unique = true),
    ],
)
data class SyncIdMapDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "local_id")
    val localId: Long,
    @ColumnInfo(name = "sync_id")
    val syncId: String,
)

@Entity(
    tableName = "sync_conflict_log",
    indices = [Index(value = ["created_at"])],
)
data class SyncConflictDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "resolution")
    val resolution: String,
    @ColumnInfo(name = "detail")
    val detail: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
