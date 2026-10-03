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
