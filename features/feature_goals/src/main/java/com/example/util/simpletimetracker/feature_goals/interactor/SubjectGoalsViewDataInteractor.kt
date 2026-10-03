package com.example.util.simpletimetracker.feature_goals.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.category.interactor.CategoryInteractor
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor.GetParam
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.timetable.model.SubjectGoal
import com.example.util.simpletimetracker.domain.timetable.model.SubjectGoal.Companion.ECTS_HOURS_PER_POINT
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.domain.timetable.repo.SubjectGoalRepo
import com.example.util.simpletimetracker.feature_base_adapter.subjectGoal.SubjectGoalViewData
import java.util.Locale
import javax.inject.Inject

/**
 * The hours per subject section of the goals tab: one progress bar row
 * per Uni activity, filled with the tracked hours against the target
 * hours (from ECTS or manual).
 */
class SubjectGoalsViewDataInteractor @Inject constructor(
    private val subjectGoalRepo: SubjectGoalRepo,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val categoryInteractor: CategoryInteractor,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val recordInteractor: RecordInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val colorMapper: ColorMapper,
    private val timeMapper: TimeMapper,
    private val resourceRepo: ResourceRepo,
) {

    suspend fun getSubjectGoals(): List<SubjectGoalViewData> {
        val uniTypeIds = getUniTypeIds()
        if (uniTypeIds.isEmpty()) return emptyList()

        val isDarkTheme = prefsInteractor.getDarkMode()
        val types = recordTypeInteractor.getAll().filter { it.id in uniTypeIds }
        val goals = subjectGoalRepo.getAll().associateBy(SubjectGoal::activityTypeId)

        return types.map { type ->
            val goal = goals[type.id]?.takeIf { it.targetSeconds > 0 }
            val hasTarget = goal != null
            val trackedSeconds = trackedSeconds(type.id)
            val percent = if (goal != null) {
                (trackedSeconds * 100 / goal.targetSeconds).toInt().coerceIn(0, 100)
            } else {
                0
            }
            SubjectGoalViewData(
                id = type.id,
                name = type.name,
                color = colorMapper.mapToColorInt(type.color, isDarkTheme),
                percent = percent,
                progressText = if (goal != null) {
                    resourceRepo.getString(
                        R.string.subject_goal_progress,
                        formatHours(trackedSeconds),
                        formatHours(goal.targetSeconds),
                    )
                } else {
                    ""
                },
                ectsText = goal?.ects?.takeIf { it > 0.0 }?.let(::formatEcts).orEmpty(),
                hasTarget = hasTarget,
            )
        }
    }

    suspend fun getSubjectGoal(activityTypeId: Long): SubjectGoal? {
        return subjectGoalRepo.get(activityTypeId)
    }

    suspend fun setTarget(
        activityTypeId: Long,
        targetSeconds: Long,
        ects: Double?,
    ) {
        subjectGoalRepo.set(
            SubjectGoal(
                activityTypeId = activityTypeId,
                targetSeconds = targetSeconds,
                ects = ects,
            ),
        )
        LocalDataChangedBus.publish()
    }

    private suspend fun trackedSeconds(typeId: Long): Long {
        return recordInteractor
            .getWithParams(GetParam.Type(setOf(typeId)))
            .sumOf { (it.timeEnded - it.timeStarted) / 1000 }
    }

    private suspend fun getUniTypeIds(): Set<Long> {
        val uniCategory = categoryInteractor
            .getAll()
            .firstOrNull { it.name.equals(UNI_CATEGORY_NAME, ignoreCase = true) }
            ?: return emptySet()
        return recordTypeCategoryInteractor.getTypes(categoryId = uniCategory.id)
    }

    private fun formatHours(seconds: Long): String {
        return timeMapper.formatDuration(seconds)
    }

    private fun formatEcts(ects: Double): String {
        val text = if (ects == Math.floor(ects)) {
            ects.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", ects)
        }
        return resourceRepo.getString(R.string.subject_goal_ects, text)
    }

    companion object {
        private const val UNI_CATEGORY_NAME = "Uni"

        // Both values live on the duration wheel: one ECTS point is
        // ECTS_HOURS_PER_POINT hours.
        const val ECTS_TO_SECONDS = ECTS_HOURS_PER_POINT * 3600
    }
}
