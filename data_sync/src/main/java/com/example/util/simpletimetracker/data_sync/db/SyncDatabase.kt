package com.example.util.simpletimetracker.data_sync.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Singleton

@Database(
    entities = [SyncStateDBO::class, SyncIdMapDBO::class, SyncConflictDBO::class],
    version = 3,
    exportSchema = false,
)
@Singleton
abstract class SyncDatabase : RoomDatabase() {

    abstract fun syncStateDao(): SyncStateDao
    abstract fun syncIdMapDao(): SyncIdMapDao
    abstract fun syncConflictDao(): SyncConflictDao

    companion object {
        /**
         * The sync database is a pure cache of the last pushed state.
         * Losing it only causes one extra full push on the next sync,
         * but the v2 -> v3 migration is real (not destructive) so the
         * seeded id mappings survive and entities keep their sync ids.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_id_map` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entity_type` TEXT NOT NULL, " +
                        "`local_id` INTEGER NOT NULL, " +
                        "`sync_id` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_id_map_entity_type_local_id` " +
                        "ON `sync_id_map` (`entity_type`, `local_id`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_id_map_entity_type_sync_id` " +
                        "ON `sync_id_map` (`entity_type`, `sync_id`)",
                )
                // Entities synced before the id map existed used the
                // legacy "a<id>"/"e<id>" scheme; seed their mappings from
                // the mirror so they keep their server side ids.
                db.execSQL(
                    "INSERT OR IGNORE INTO `sync_id_map` (`entity_type`, `local_id`, `sync_id`) " +
                        "SELECT `entity_type`, CAST(substr(`entity_id`, 2) AS INTEGER), `entity_id` " +
                        "FROM `sync_state`",
                )
            }
        }

        fun build(context: Context): SyncDatabase {
            return Room.databaseBuilder(
                context,
                SyncDatabase::class.java,
                "sync_queue.db",
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
