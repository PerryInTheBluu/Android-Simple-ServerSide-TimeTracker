package com.example.util.simpletimetracker.data_local.timetable

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * A per subject hours target for the semester account: the target comes
 * either from ECTS (stored alongside for display) or is entered manually.
 */
@Entity(tableName = "subjectGoals")
data class SubjectGoalDBO(
    @PrimaryKey
    @ColumnInfo(name = "activity_type_id")
    val activityTypeId: Long,
    // Target duration in seconds; 0 means no target.
    @ColumnInfo(name = "target_seconds")
    val targetSeconds: Long,
    // Optional ECTS value the target was derived from.
    @ColumnInfo(name = "ects")
    val ects: Double?,
)

@Dao
interface SubjectGoalDao {

    @Query("SELECT * FROM subjectGoals")
    suspend fun getAll(): List<SubjectGoalDBO>

    @Query("SELECT * FROM subjectGoals WHERE activity_type_id = :activityTypeId")
    suspend fun get(activityTypeId: Long): SubjectGoalDBO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SubjectGoalDBO)

    @Query("DELETE FROM subjectGoals WHERE activity_type_id = :activityTypeId")
    suspend fun delete(activityTypeId: Long)
}
