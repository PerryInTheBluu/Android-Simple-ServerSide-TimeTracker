package com.example.util.simpletimetracker.data_sync.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state")
    suspend fun getAll(): List<SyncStateDBO>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncStateDBO>)

    @Query("UPDATE sync_state SET content_hash = :hash WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun updateHash(entityType: String, entityId: String, hash: String)

    @Query("DELETE FROM sync_state WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun remove(entityType: String, entityId: String)

    @Query("DELETE FROM sync_state")
    suspend fun clear()
}

@Dao
interface SyncIdMapDao {

    @Query("SELECT * FROM sync_id_map")
    suspend fun getAll(): List<SyncIdMapDBO>

    @Query("SELECT * FROM sync_id_map WHERE entity_type = :entityType AND local_id = :localId")
    suspend fun getByLocalId(entityType: String, localId: Long): SyncIdMapDBO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncIdMapDBO)

    @Query("DELETE FROM sync_id_map WHERE entity_type = :entityType AND local_id = :localId")
    suspend fun removeByLocalId(entityType: String, localId: Long)

    @Query("DELETE FROM sync_id_map WHERE entity_type = :entityType AND sync_id = :syncId")
    suspend fun removeBySyncId(entityType: String, syncId: String)

    @Query("DELETE FROM sync_id_map")
    suspend fun clear()
}

@Dao
interface SyncConflictDao {

    @Query("SELECT * FROM sync_conflict_log ORDER BY created_at DESC LIMIT 100")
    fun getAllFlow(): Flow<List<SyncConflictDBO>>

    @Query("SELECT * FROM sync_conflict_log ORDER BY created_at DESC LIMIT 100")
    suspend fun getAll(): List<SyncConflictDBO>

    @Query("SELECT COUNT(*) FROM sync_conflict_log")
    suspend fun count(): Int

    @Insert
    suspend fun insert(conflict: SyncConflictDBO)

    @Query("DELETE FROM sync_conflict_log")
    suspend fun clear()
}
