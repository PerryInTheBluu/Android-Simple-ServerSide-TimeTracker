package com.example.util.simpletimetracker.data_sync.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["entity_type", "entity_id"], unique = true)],
)
data class SyncQueueDBO(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
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
