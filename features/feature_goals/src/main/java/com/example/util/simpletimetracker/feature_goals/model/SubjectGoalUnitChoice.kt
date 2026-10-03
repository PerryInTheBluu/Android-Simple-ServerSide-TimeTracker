package com.example.util.simpletimetracker.feature_goals.model

import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import kotlinx.parcelize.Parcelize

/**
 * The unit the subject hours target is entered in: derived from ECTS
 * (one point is 30 hours) or manual hours.
 */
@Parcelize
data class SubjectGoalUnitChoice(
    val activityTypeId: Long,
    val isEcts: Boolean,
) : OptionsListParams.Item.Id
