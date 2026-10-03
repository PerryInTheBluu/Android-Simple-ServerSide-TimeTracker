package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A per subject hours target: the semester workload derived from ECTS
 * (one ECTS is ECTS_HOURS_PER_POINT hours) or entered manually.
 */
data class SubjectGoal(
    val activityTypeId: Long,
    // Target duration in seconds; 0 means no target.
    val targetSeconds: Long,
    // Optional ECTS value the target was derived from.
    val ects: Double?,
) {

    companion object {
        const val ECTS_HOURS_PER_POINT = 30L
    }
}
