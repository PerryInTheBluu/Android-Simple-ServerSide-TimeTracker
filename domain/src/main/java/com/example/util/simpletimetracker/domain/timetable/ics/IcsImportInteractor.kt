package com.example.util.simpletimetracker.domain.timetable.ics

import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableIcsRepo
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import java.time.LocalDate
import javax.inject.Inject

/**
 * Imports an ics file into the timetable.
 *
 * Events are linked to activities by name first: the activity with the
 * longest name contained in the event name wins. If name matching fails,
 * the ICS CATEGORIES values are matched against activity names and
 * category groupings. The slot type is derived from CATEGORIES first,
 * with a name based fallback. Parallel groups of the same course
 * collapse into one slot. EXDATE values and RECURRENCE-ID overrides
 * become event overrides.
 */
class IcsImportInteractor @Inject constructor(
    private val parser: IcsParser,
    private val timetableRepo: TimetableRepo,
    private val recordTypeRepo: RecordTypeRepo,
    private val timetableIcsRepo: TimetableIcsRepo,
    private val timetableNotificationInteractor: TimetableNotificationInteractor,
    private val categoryRepo: CategoryRepo,
    private val recordTypeCategoryRepo: RecordTypeCategoryRepo,
) {

    data class ImportResult(
        val eventsAdded: Int,
        val overridesAdded: Int = 0,
        val unmatchedNames: List<String> = emptyList(),
    )

    suspend fun importFile(uriString: String): ImportResult {
        return import(timetableIcsRepo.readIcsFile(uriString))
    }

    suspend fun import(
        content: String,
        clearExisting: Boolean = true,
    ): ImportResult {
        val parseResult = parser.parse(content)
        val imported = parseResult.events
        if (imported.isEmpty() && parseResult.overrides.isEmpty()) {
            return ImportResult(0, 0, emptyList())
        }

        val types = recordTypeRepo.getAll()
        val categories = categoryRepo.getAll()
        val typeToCategoryMap = recordTypeCategoryRepo.getAll()
        val unmatched = mutableSetOf<String>()

        if (clearExisting) {
            timetableRepo.clearAll()
        }

        var addedEvents = 0
        var addedOverrides = 0
        // Parallel groups differ only in the room; keep the first.
        val seenSlots = mutableSetOf<Triple<String, Int, Int>>()

        // Map from (UID, dayOfWeek) to generated eventId
        val eventIdMap = mutableMapOf<Pair<String, Int>, Long>()
        val uidToFirstEventId = mutableMapOf<String, Long>()

        imported.forEach { event ->
            if (!seenSlots.add(Triple(event.name, event.daysOfWeek.first(), event.startTime))) return@forEach
            val activityTypeId = matchActivity(
                eventName = event.name,
                category = event.category,
                types = types,
                categories = categories,
                typeCategories = typeToCategoryMap,
            )
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
                if (event.uid.isNotEmpty()) {
                    eventIdMap[event.uid to day] = eventId
                    uidToFirstEventId.putIfAbsent(event.uid, eventId)
                }

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
                    addedOverrides++
                }
                addedEvents++
            }
        }

        // Apply RECURRENCE-ID overrides matching the event's UID
        parseResult.overrides.forEach { override ->
            val overrideDayOfWeek = runCatching {
                LocalDate.parse(override.recurrenceDate).dayOfWeek.value
            }.getOrNull()

            val targetEventId = if (override.uid.isNotEmpty()) {
                (if (overrideDayOfWeek != null) eventIdMap[override.uid to overrideDayOfWeek] else null)
                    ?: uidToFirstEventId[override.uid]
            } else {
                null
            }

            if (targetEventId != null) {
                timetableRepo.addOverride(
                    TimetableEventOverride(
                        date = override.recurrenceDate,
                        eventId = targetEventId,
                        room = override.room,
                        startTime = override.startTime,
                        endTime = override.endTime,
                        cancelled = override.cancelled,
                        note = override.note,
                    ),
                )
                addedOverrides++
            }
        }

        timetableNotificationInteractor.rescheduleAll()
        LocalDataChangedBus.publish()
        return ImportResult(
            eventsAdded = addedEvents,
            overridesAdded = addedOverrides,
            unmatchedNames = unmatched.toList(),
        )
    }

    private fun matchActivity(
        eventName: String,
        category: String,
        types: List<RecordType>,
        categories: List<Category>,
        typeCategories: List<RecordTypeCategory>,
    ): Long? {
        val name = eventName.lowercase()
        // 1. Direct activity name match (longest matching name wins)
        val nameMatch = types
            .filter { type ->
                val typeName = type.name.lowercase()
                typeName.isNotEmpty() && name.contains(typeName)
            }
            .maxByOrNull { it.name.length }
            ?.id
        if (nameMatch != null) return nameMatch

        // 2. Category matching via CATEGORIES tokens
        val categoryTokens = category
            .split(',', ';')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        for (token in categoryTokens) {
            // 2a. Match CATEGORIES token against activity names
            val typeByCategoryName = types.firstOrNull { type ->
                val typeName = type.name.lowercase()
                typeName.isNotEmpty() && (token == typeName || token.contains(typeName) || typeName.contains(token))
            }?.id
            if (typeByCategoryName != null) return typeByCategoryName

            // 2b. Match CATEGORIES token against Category entities
            val matchingCat = categories.firstOrNull { cat ->
                val catName = cat.name.lowercase()
                catName.isNotEmpty() && (token == catName || token.contains(catName) || catName.contains(token))
            }
            if (matchingCat != null) {
                val typesInCat = typeCategories
                    .filter { it.categoryId == matchingCat.id }
                    .map { it.recordTypeId }
                if (typesInCat.isNotEmpty()) {
                    // Prefer a type in that category whose name appears in eventName
                    val bestTypeInCat = types
                        .filter { it.id in typesInCat && name.contains(it.name.lowercase()) }
                        .maxByOrNull { it.name.length }
                        ?.id
                    return bestTypeInCat ?: typesInCat.first()
                }
            }
        }

        return null
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
