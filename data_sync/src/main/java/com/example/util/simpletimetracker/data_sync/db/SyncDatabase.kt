package com.example.util.simpletimetracker.data_sync.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Singleton

@Database(
    entities = [SyncQueueDBO::class, SyncConflictDBO::class],
    version = 1,
    exportSchema = false,
)
@Singleton
abstract class SyncDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncConflictDao(): SyncConflictDao

    companion object {
        fun build(context: Context): SyncDatabase {
            return Room.databaseBuilder(
                context,
                SyncDatabase::class.java,
                "sync_queue.db",
            ).build()
        }
    }
}
