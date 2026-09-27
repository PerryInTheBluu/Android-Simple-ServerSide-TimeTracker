package com.example.util.simpletimetracker.data_sync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    suspend fun getAll(): List<SyncQueueDBO>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun countFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueDBO)

    @Query("DELETE FROM sync_queue WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun remove(entityType: String, entityId: String)

    @Query("UPDATE sync_queue SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}

@Dao
interface SyncConflictDao {

    @Query("SELECT * FROM sync_conflict_log ORDER BY created_at DESC LIMIT 100")
    fun getAllFlow(): Flow<List<SyncConflictDBO>>

    @Insert
    suspend fun insert(conflict: SyncConflictDBO)

    @Query("DELETE FROM sync_conflict_log")
    suspend fun clear()
}
