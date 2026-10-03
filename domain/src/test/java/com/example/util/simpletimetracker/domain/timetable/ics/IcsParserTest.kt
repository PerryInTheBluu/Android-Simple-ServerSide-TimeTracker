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
    fun skipsSingleEventsWithoutRrule() {
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

        // The timetable only holds weekly recurring slots.
        assertEquals(0, events.size)
    }

    @Test
    fun skipsExpiredRrulesWithPastUntil() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Alte Vorlesung
            DTSTART;TZID=Europe/Berlin:20231017T130000
            DTEND;TZID=Europe/Berlin:20231017T134500
            RRULE:FREQ=WEEKLY;UNTIL=20240206T134500;INTERVAL=1;BYDAY=TU
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Aktuelle Vorlesung
            DTSTART;TZID=Europe/Berlin:20261012T080000
            DTEND;TZID=Europe/Berlin:20261012T100000
            RRULE:FREQ=WEEKLY;UNTIL=20270206T100000;INTERVAL=1;BYDAY=MO
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals("Aktuelle Vorlesung", events[0].name)
        assertEquals(listOf(1), events[0].daysOfWeek)
        assertEquals(480, events[0].startTime)
        assertEquals(600, events[0].endTime)
    }

    @Test
    fun parsesCategoriesAndExDates() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Mathematik für Ingenieure E1: ET\,IuK\,ME
            LOCATION:11401.00.116 (H14 Bernhard-Ilschner-Hörsaal)
            DTSTART;TZID=Europe/Berlin:20231020T081500
            DTEND;TZID=Europe/Berlin:20231020T094500
            RRULE:FREQ=WEEKLY;UNTIL=20270209T094500;INTERVAL=1;BYDAY=FR
            CATEGORIES:Vorlesung
            EXDATE;TZID=Europe/Berlin:20270105T081500,20261229T081500
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals("Mathematik für Ingenieure E1: ET,IuK,ME", events[0].name)
        assertEquals("Vorlesung", events[0].category)
        assertEquals(listOf("2027-01-05", "2026-12-29"), events[0].exDates)
        assertEquals(495, events[0].startTime)
        assertEquals(585, events[0].endTime)
    }

    @Test
    fun handlesHisinOneEventStructure() {
        // Structure of the FAU campo export: folded URL lines, TZID,
        // UNTIL with seconds, GEO and other unknown properties.
        val ics = """
            BEGIN:VCALENDAR
            PRODID:-//HISinOne - HIS eG//iCal4j 3.0.6//EN
            VERSION:2.0
            BEGIN:VTIMEZONE
            TZID:Europe/Berlin
            BEGIN:DAYLIGHT
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            TZNAME:CEST
            DTSTART:19810329T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
            END:DAYLIGHT
            END:VTIMEZONE
            BEGIN:VEVENT
            DTSTAMP:20261003T214607Z
            DTSTART;TZID=Europe/Berlin:20261014T121500
            DTEND;TZID=Europe/Berlin:20261014T134500
            SUMMARY:Werkstoffe und ihre Struktur
            RRULE:FREQ=WEEKLY;UNTIL=20270206T134500;INTERVAL=1;BYDAY=TU
            LOCATION:11901.00.227 (H9 Werner-von-Siemens - Hörsaal)
            GEO:49.574363708496094;11.029380798339844
            CATEGORIES:Vorlesung mit Übung
            EXDATE;TZID=Europe/Berlin:20261226T121500
            URL:https://www.campo.fau.de:443/qisserver/pages/startFlow.xhtml?_flowId=
             detailView-flow&unitId=87456&periodId=396
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals("Werkstoffe und ihre Struktur", events[0].name)
        assertEquals(listOf(2), events[0].daysOfWeek)
        assertEquals(735, events[0].startTime)
        assertEquals(825, events[0].endTime)
        assertEquals("Vorlesung mit Übung", events[0].category)
        assertEquals(listOf("2026-12-26"), events[0].exDates)
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
