package com.example.util.simpletimetracker.feature_todos.model

import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import kotlinx.parcelize.Parcelize

/**
 * The subject selection of the add todo flow: the timetable event the
 * manually created general todo belongs to.
 */
@Parcelize
data class TimetableTodoSubjectAction(
    // Timetable event id used as the subject the todo is attached to.
    val eventId: Long,
) : OptionsListParams.Item.Id
