package com.example.util.simpletimetracker.data_local.timetable

import com.example.util.simpletimetracker.data_local.base.withLockedCache
import com.example.util.simpletimetracker.domain.timetable.model.SubjectGoal
import com.example.util.simpletimetracker.domain.timetable.repo.SubjectGoalRepo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class SubjectGoalRepoImpl @Inject constructor(
    private val subjectGoalDao: SubjectGoalDao,
) : SubjectGoalRepo {

    private val mutex: Mutex = Mutex()

    override suspend fun getAll(): List<SubjectGoal> = mutex.withLockedCache(
        logMessage = "getAll",
        accessSource = { subjectGoalDao.getAll().map(::map) },
    )

    override suspend fun get(activityTypeId: Long): SubjectGoal? = mutex.withLockedCache(
        logMessage = "get",
        accessSource = { subjectGoalDao.get(activityTypeId)?.let(::map) },
    )

    override suspend fun set(goal: SubjectGoal) = mutex.withLockedCache(
        logMessage = "set",
        accessSource = {
            subjectGoalDao.insert(
                SubjectGoalDBO(
                    activityTypeId = goal.activityTypeId,
                    targetSeconds = goal.targetSeconds,
                    ects = goal.ects,
                ),
            )
        },
    )

    override suspend fun remove(activityTypeId: Long) = mutex.withLockedCache(
        logMessage = "remove",
        accessSource = { subjectGoalDao.delete(activityTypeId) },
    )

    private fun map(dbo: SubjectGoalDBO): SubjectGoal {
        return SubjectGoal(
            activityTypeId = dbo.activityTypeId,
            targetSeconds = dbo.targetSeconds,
            ects = dbo.ects,
        )
    }
}
