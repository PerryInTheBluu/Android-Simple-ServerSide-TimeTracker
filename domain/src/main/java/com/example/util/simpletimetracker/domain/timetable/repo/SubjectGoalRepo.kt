package com.example.util.simpletimetracker.domain.timetable.repo

import com.example.util.simpletimetracker.domain.timetable.model.SubjectGoal

interface SubjectGoalRepo {

    suspend fun getAll(): List<SubjectGoal>

    suspend fun get(activityTypeId: Long): SubjectGoal?

    suspend fun set(goal: SubjectGoal)

    suspend fun remove(activityTypeId: Long)

    suspend fun clear()
}
