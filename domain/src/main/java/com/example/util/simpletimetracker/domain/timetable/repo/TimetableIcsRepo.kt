package com.example.util.simpletimetracker.domain.timetable.repo

interface TimetableIcsRepo {

    suspend fun readIcsFile(uriString: String): String
}
