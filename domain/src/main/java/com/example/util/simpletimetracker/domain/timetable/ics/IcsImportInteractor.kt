package com.example.util.simpletimetracker.domain.timetable.ics

import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableIcsRepo
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import javax.inject.Inject

/**
 * Imports an ics file into the timetable. The import replaces all
 * existing timetable data (events, overrides, free days and todos).
 *
 * Events are linked to activities by name: the activity with the
 * longest name contained in the event name wins. The slot type is
 * derived from the ics CATEGORIES value first (e.g. HISinOne
 * exports like the FAU campo export), with a name based fallback.
 * Parallel groups of the same course (same name, day and time)
 * collapse into one slot. EXDATE values become cancelled overrides.
 */
class IcsImportInteractor @Inject constructor(
    private val parser: IcsParser,
    private val timetableRepo: TimetableRepo,
    private val recordTypeRepo: RecordTypeRepo,
    private val timetableIcsRepo: TimetableIcsRepo,
    private val timetableNotificationInteractor: TimetableNotificationInteractor,
) {

    data class ImportResult(
        val eventsAdded: Int,
        val unmatchedNames: List<String>,
    )

    suspend fun importFile(uriString: String): ImportResult {
        return import(timetableIcsRepo.readIcsFile(uriString))
    }

    suspend fun import(content: String): ImportResult {
        val imported = parser.parse(content)
        if (imported.isEmpty()) return ImportResult(0, emptyList())

        val types = recordTypeRepo.getAll()
        val unmatched = mutableSetOf<String>()
        timetableRepo.clearAll()
        var added = 0
        // Parallel groups differ only in the room; keep the first.
        val seenSlots = mutableSetOf<Triple<String, Int, Int>>()

        imported.forEach { event ->
            if (!seenSlots.add(Triple(event.name, event.daysOfWeek.first(), event.startTime))) return@forEach
            val activityTypeId = matchActivity(event.name, types)
            if (activityTypeId == null) unmatched.add(event.name)
            val type = deriveType(event.name, event.category)
            event.daysOfWeek.forEach { day ->
                val eventId = timetableRepo.addEvent(
                    TimetableEvent(
                        name = event.name,
                        dayOfWeek = day,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        room = event.room,
                        type = type,
                        comment = event.comment,
                        activityTypeId = activityTypeId,
                    ),
                )
                event.exDates.forEach { date ->
                    timetableRepo.addOverride(
                        TimetableEventOverride(
                            date = date,
                            eventId = eventId,
                            room = "",
                            startTime = 0,
                            endTime = 0,
                            cancelled = true,
                            note = "",
                        ),
                    )
                }
                added++
            }
        }

        timetableNotificationInteractor.rescheduleAll()
        return ImportResult(added, unmatched.toList())
    }

    private fun matchActivity(
        eventName: String,
        types: List<RecordType>,
    ): Long? {
        val name = eventName.lowercase()
        return types
            .filter { type ->
                val typeName = type.name.lowercase()
                typeName.isNotEmpty() && name.contains(typeName)
            }
            .maxByOrNull { it.name.length }
            ?.id
    }

    private fun deriveType(
        eventName: String,
        category: String,
    ): TimetableEvent.Type {
        val categoryLower = category.lowercase()
        if (categoryLower.isNotEmpty()) {
            return when {
                "vorlesung" in categoryLower -> TimetableEvent.Type.LECTURE
                "tutorium" in categoryLower -> TimetableEvent.Type.TUTORIUM
                "übung" in categoryLower -> TimetableEvent.Type.EXERCISE
                "praktikum" in categoryLower -> TimetableEvent.Type.EXERCISE
                else -> TimetableEvent.Type.LECTURE
            }
        }
        val name = eventName.lowercase()
        return when {
            "übung" in name || "exercise" in name -> TimetableEvent.Type.EXERCISE
            "tutorium" in name || "tutorial" in name -> TimetableEvent.Type.TUTORIUM
            else -> TimetableEvent.Type.LECTURE
        }
    }
}
