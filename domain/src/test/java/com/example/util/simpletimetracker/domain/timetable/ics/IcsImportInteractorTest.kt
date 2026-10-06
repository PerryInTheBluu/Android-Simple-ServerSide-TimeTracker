package com.example.util.simpletimetracker.domain.timetable.ics

import com.example.util.simpletimetracker.domain.category.model.Category
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.category.repo.CategoryRepo
import com.example.util.simpletimetracker.domain.category.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.repo.RecordTypeRepo
import com.example.util.simpletimetracker.domain.timetable.model.TimetableDay
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableIcsRepo
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IcsImportInteractorTest {

    private val parser = IcsParser()
    private val fakeTimetableRepo = FakeTimetableRepo()
    private val fakeRecordTypeRepo = FakeRecordTypeRepo()
    private val fakeCategoryRepo = FakeCategoryRepo()
    private val fakeRecordTypeCategoryRepo = FakeRecordTypeCategoryRepo()
    private val fakeIcsRepo = FakeTimetableIcsRepo()
    private val fakeNotificationInteractor = object : TimetableNotificationInteractor {
        override suspend fun rescheduleAll() {}
        override suspend fun onPreparationDue(eventId: Long, date: String) {}
        override suspend fun onFollowUpDue(eventId: Long, date: String) {}
        override suspend fun onTodoDone(eventId: Long, date: String, type: TimetableTodo.Type) {}
    }

    private lateinit var interactor: IcsImportInteractor

    @Before
    fun setUp() {
        interactor = IcsImportInteractor(
            parser = parser,
            timetableRepo = fakeTimetableRepo,
            recordTypeRepo = fakeRecordTypeRepo,
            timetableIcsRepo = fakeIcsRepo,
            timetableNotificationInteractor = fakeNotificationInteractor,
            categoryRepo = fakeCategoryRepo,
            recordTypeCategoryRepo = fakeRecordTypeCategoryRepo,
        )
    }

    @Test
    fun matchesDirectActivityName() = runBlocking {
        fakeRecordTypeRepo.types += RecordType(
            id = 101,
            name = "Thermo",
            icon = "",
            color = AppColor(1, ""),
            defaultDuration = 0,
            note = "",
        )

        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:thermo-1@campo.fau.de
            SUMMARY:Thermodynamik I
            DTSTART:20261013T101500
            DTEND:20261013T115500
            RRULE:FREQ=WEEKLY;BYDAY=TU
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = interactor.import(ics)

        assertEquals(1, result.eventsAdded)
        assertEquals(0, result.unmatchedNames.size)
        val event = fakeTimetableRepo.events.first()
        assertEquals(101L, event.activityTypeId)
        assertEquals(TimetableEvent.Type.LECTURE, event.type)
    }

    @Test
    fun matchesCategoriesTokenToActivityNameWhenEventNameDiffers() = runBlocking {
        fakeRecordTypeRepo.types += RecordType(
            id = 202,
            name = "Uni",
            icon = "",
            color = AppColor(2, ""),
            defaultDuration = 0,
            note = "",
        )

        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:course-99@campo.fau.de
            SUMMARY:Höhere Experimentalphysik
            CATEGORIES:Vorlesung, Uni
            DTSTART:20261014T141500
            DTEND:20261014T154500
            RRULE:FREQ=WEEKLY;BYDAY=WE
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = interactor.import(ics)

        assertEquals(1, result.eventsAdded)
        val event = fakeTimetableRepo.events.first()
        // Event name doesn't contain "Uni", but CATEGORIES contains "Uni"
        assertEquals(202L, event.activityTypeId)
    }

    @Test
    fun matchesCategoriesTokenToCategoryGroup() = runBlocking {
        fakeCategoryRepo.categories += Category(
            id = 10,
            name = "Studium",
            color = AppColor(3, ""),
            note = "",
        )
        fakeRecordTypeRepo.types += RecordType(
            id = 303,
            name = "Informatik",
            icon = "",
            color = AppColor(3, ""),
            defaultDuration = 0,
            note = "",
        )
        fakeRecordTypeCategoryRepo.links += RecordTypeCategory(
            recordTypeId = 303,
            categoryId = 10,
        )

        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:algo-1@campo.fau.de
            SUMMARY:Algorithmen und Datenstrukturen
            CATEGORIES:Studium, Pflichtfach
            DTSTART:20261015T081500
            DTEND:20261015T094500
            RRULE:FREQ=WEEKLY;BYDAY=TH
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = interactor.import(ics)

        assertEquals(1, result.eventsAdded)
        val event = fakeTimetableRepo.events.first()
        // Matched via Category "Studium" -> activity 303
        assertEquals(303L, event.activityTypeId)
    }

    @Test
    fun appliesRecurrenceIdOverridesWithUid() = runBlocking {
        fakeRecordTypeRepo.types += RecordType(
            id = 101,
            name = "Thermo",
            icon = "",
            color = AppColor(1, ""),
            defaultDuration = 0,
            note = "",
        )

        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:thermo-series@campo.fau.de
            SUMMARY:Thermodynamik
            LOCATION:HS 1
            DTSTART:20261013T101500
            DTEND:20261013T115500
            RRULE:FREQ=WEEKLY;BYDAY=TU
            END:VEVENT
            BEGIN:VEVENT
            UID:thermo-series@campo.fau.de
            RECURRENCE-ID;TZID=Europe/Berlin:20261020T101500
            SUMMARY:Thermodynamik Raumwechsel
            LOCATION:HS 2
            DTSTART:20261020T103000
            DTEND:20261020T120000
            END:VEVENT
            BEGIN:VEVENT
            UID:thermo-series@campo.fau.de
            RECURRENCE-ID;TZID=Europe/Berlin:20261027T101500
            STATUS:CANCELLED
            SUMMARY:Thermodynamik Fällt aus
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = interactor.import(ics)

        assertEquals(1, result.eventsAdded)
        assertEquals(2, result.overridesAdded)

        val overrides = fakeTimetableRepo.overrides
        assertEquals(2, overrides.size)

        val roomChange = overrides.first { it.date == "2026-10-20" }
        assertEquals("HS 2", roomChange.room)
        assertEquals(630, roomChange.startTime)
        assertEquals(720, roomChange.endTime)
        assertEquals(false, roomChange.cancelled)

        val cancelled = overrides.first { it.date == "2026-10-27" }
        assertEquals(true, cancelled.cancelled)
    }

    private class FakeTimetableRepo : TimetableRepo {
        val events = mutableListOf<TimetableEvent>()
        val overrides = mutableListOf<TimetableEventOverride>()
        private var nextEventId = 1L
        private var nextOverrideId = 1L

        override suspend fun getAllEvents(): List<TimetableEvent> = events
        override suspend fun getEvents(dayOfWeek: Int): List<TimetableEvent> = events.filter { it.dayOfWeek == dayOfWeek }
        override suspend fun addEvent(event: TimetableEvent): Long {
            val id = nextEventId++
            events.add(event.copy(id = id))
            return id
        }
        override suspend fun removeEvent(id: Long) { events.removeAll { it.id == id } }
        override suspend fun clearEvents() { events.clear() }
        override suspend fun clearAll() { events.clear(); overrides.clear() }
        override suspend fun getOverrides(date: String): List<TimetableEventOverride> = overrides.filter { it.date == date }
        override suspend fun getAllOverrides(): List<TimetableEventOverride> = overrides
        override suspend fun addOverride(override: TimetableEventOverride): Long {
            val id = nextOverrideId++
            overrides.add(override.copy(id = id))
            return id
        }
        override suspend fun removeOverride(id: Long) { overrides.removeAll { it.id == id } }
        override suspend fun getDays(): List<TimetableDay> = emptyList()
        override suspend fun addDay(day: TimetableDay): Long = 1L
        override suspend fun removeDay(id: Long) {}
        override suspend fun getTodos(eventId: Long): List<TimetableTodo> = emptyList()
        override suspend fun getAllTodos(): List<TimetableTodo> = emptyList()
        override suspend fun addTodo(todo: TimetableTodo): Long = 1L
        override suspend fun setTodoDone(id: Long, done: Boolean) {}
        override suspend fun removeTodo(id: Long) {}
    }

    private class FakeRecordTypeRepo : RecordTypeRepo {
        val types = mutableListOf<RecordType>()
        override suspend fun getAll(): List<RecordType> = types
        override suspend fun get(id: Long): RecordType? = types.find { it.id == id }
        override suspend fun get(name: String): List<RecordType> = types.filter { it.name == name }
        override suspend fun add(recordType: RecordType): Long { types.add(recordType); return recordType.id }
        override suspend fun archive(id: Long) {}
        override suspend fun restore(id: Long) {}
        override suspend fun remove(id: Long) { types.removeAll { it.id == id } }
        override suspend fun clear() { types.clear() }
    }

    private class FakeCategoryRepo : CategoryRepo {
        val categories = mutableListOf<Category>()
        override suspend fun getAll(): List<Category> = categories
        override suspend fun get(id: Long): Category? = categories.find { it.id == id }
        override suspend fun get(name: String): List<Category> = categories.filter { it.name == name }
        override suspend fun add(category: Category): Long { categories.add(category); return category.id }
        override suspend fun remove(id: Long) { categories.removeAll { it.id == id } }
        override suspend fun clear() { categories.clear() }
    }

    private class FakeRecordTypeCategoryRepo : RecordTypeCategoryRepo {
        val links = mutableListOf<RecordTypeCategory>()
        override suspend fun getAll(): List<RecordTypeCategory> = links
        override suspend fun getCategoryIdsByType(typeId: Long): Set<Long> = links.filter { it.recordTypeId == typeId }.map { it.categoryId }.toSet()
        override suspend fun getTypeIdsByCategory(categoryId: Long): Set<Long> = links.filter { it.categoryId == categoryId }.map { it.recordTypeId }.toSet()
        override suspend fun add(recordTypeCategory: RecordTypeCategory) { links.add(recordTypeCategory) }
        override suspend fun addCategories(typeId: Long, categoryIds: List<Long>) {}
        override suspend fun removeCategories(typeId: Long, categoryIds: List<Long>) {}
        override suspend fun addTypes(categoryId: Long, typeIds: List<Long>) {}
        override suspend fun removeTypes(categoryId: Long, typeIds: List<Long>) {}
        override suspend fun removeAll(categoryId: Long) { links.removeAll { it.categoryId == categoryId } }
        override suspend fun removeAllByType(typeId: Long) { links.removeAll { it.recordTypeId == typeId } }
        override suspend fun clear() { links.clear() }
    }

    private class FakeTimetableIcsRepo : TimetableIcsRepo {
        override suspend fun readIcsFile(uriString: String): String = ""
        override suspend fun fetchIcsFromUrl(urlString: String): String = ""
    }
}
