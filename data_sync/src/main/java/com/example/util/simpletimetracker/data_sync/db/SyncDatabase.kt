package com.example.util.simpletimetracker.data_sync.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Singleton

@Database(
    entities = [SyncStateDBO::class, SyncConflictDBO::class],
    version = 2,
    exportSchema = false,
)
@Singleton
abstract class SyncDatabase : RoomDatabase() {

    abstract fun syncStateDao(): SyncStateDao
    abstract fun syncConflictDao(): SyncConflictDao

    companion object {
        fun build(context: Context): SyncDatabase {
            // The sync database is a pure cache of the last pushed state.
            // Losing it only causes one extra full push on the next sync.
            return Room.databaseBuilder(
                context,
                SyncDatabase::class.java,
                "sync_queue.db",
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
