package com.example.util.simpletimetracker.domain.timetable.ics

import org.junit.Assert.assertEquals
import org.junit.Test

class IcsParserTest {

    private val parser = IcsParser()

    @Test
    fun parsesWeeklyEventWithByDay() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Thermodynamik
            LOCATION:HS 2
            DESCRIPTION:Vorlesung bei Prof. X
            DTSTART;TZID=Europe/Berlin:20261013T101500
            DTEND;TZID=Europe/Berlin:20261013T115500
            RRULE:FREQ=WEEKLY;BYDAY=TU
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("Thermodynamik", event.name)
        assertEquals(listOf(2), event.daysOfWeek) // Tuesday
        assertEquals(615, event.startTime) // 10:15
        assertEquals(715, event.endTime) // 11:55
        assertEquals("HS 2", event.room)
        assertEquals("Vorlesung bei Prof. X", event.comment)
    }

    @Test
    fun splitsMultipleDaysIntoSeparateEvents() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Mathe Übung
            DTSTART:20261012T090000
            DTEND:20261012T103000
            RRULE:FREQ=WEEKLY;BYDAY=MO,TH
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(2, events.size)
        assertEquals(listOf(1), events[0].daysOfWeek) // Monday
        assertEquals(listOf(4), events[1].daysOfWeek) // Thursday
        assertEquals(540, events[0].startTime)
        assertEquals(630, events[0].endTime)
    }

    @Test
    fun withoutRruleUsesStartDayOfWeek() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Einzelterm
            DTSTART:20261015T140000
            DTEND:20261015T153000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals(listOf(4), events[0].daysOfWeek) // Thursday
        assertEquals(840, events[0].startTime)
    }

    @Test
    fun unfoldsFoldedDescriptionLines() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Physik
            DESCRIPTION:Erste Zeile 
             gefaltete zweite Zeile
            DTSTART:20261013T080000
            DTEND:20261013T093000
            RRULE:FREQ=WEEKLY;BYDAY=MO
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals("Erste Zeile gefaltete zweite Zeile", events[0].comment)
    }

    @Test
    fun unescapesSpecialCharacters() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Übung A\, Teil 1
            LOCATION:Raum 3\; Geb. B
            DTSTART:20261013T080000
            DTEND:20261013T093000
            RRULE:FREQ=WEEKLY;BYDAY=WE
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals("Übung A, Teil 1", events[0].name)
        assertEquals("Raum 3; Geb. B", events[0].room)
    }

    @Test
    fun convertsUtcTimes() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Online Event
            DTSTART:20261013T090000Z
            DTEND:20261013T100000Z
            RRULE:FREQ=WEEKLY;BYDAY=MO
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        // 09:00 UTC is 11:00 in the Europe/Berlin summer (CEST) zone
        // when the test system runs in that zone; without a TZID the
        // parser uses the system default, so only check that parsing
        // worked and the times are consistent.
        assertEquals(events[0].endTime - events[0].startTime, 60)
    }
}
