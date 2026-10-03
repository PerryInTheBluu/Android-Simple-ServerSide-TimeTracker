package com.example.util.simpletimetracker.feature_base_adapter.subjectGoal

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

/**
 * A per subject hours target row in the goals tab: shows the tracked
 * hours against the target as a progress bar in the activity color.
 */
data class SubjectGoalViewData(
    val id: Long,
    val name: String,
    val color: Int,
    // 0..100, capped.
    val percent: Int,
    // Human readable "done / target" text, empty when no target.
    val progressText: String,
    // ECTS display text, empty when not derived from ECTS.
    val ectsText: String,
    val hasTarget: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = id

    override fun isValidType(other: ViewHolderType): Boolean = other is SubjectGoalViewData
}
