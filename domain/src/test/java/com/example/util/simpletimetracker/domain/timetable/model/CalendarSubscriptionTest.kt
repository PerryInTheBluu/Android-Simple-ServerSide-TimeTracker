package com.example.util.simpletimetracker.domain.timetable.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSubscriptionTest {

    @Test
    fun serializesAndDeserializesSubscriptions() {
        val subscriptions = listOf(
            CalendarSubscription(
                id = "sub-1",
                name = "Uni Nextcloud",
                url = "https://nextcloud.example.com/remote.php/dav/calendars/pius/uni?export",
                color = "#4CAF50",
                enabled = true,
                lastFetched = 1728200000000L,
            ),
            CalendarSubscription(
                id = "sub-2",
                name = "Sport Kalender",
                url = "webcal://example.com/sport.ics",
                color = "#FF9800",
                enabled = false,
                lastFetched = 0L,
            ),
        )

        val serialized = CalendarSubscription.serialize(subscriptions)
        val deserialized = CalendarSubscription.deserialize(serialized)

        assertEquals(2, deserialized.size)
        assertEquals(subscriptions[0], deserialized[0])
        assertEquals(subscriptions[1], deserialized[1])
    }

    @Test
    fun handlesEmptyOrMalformedSerialization() {
        assertTrue(CalendarSubscription.deserialize("").isEmpty())
        assertTrue(CalendarSubscription.deserialize("   ").isEmpty())
        assertTrue(CalendarSubscription.deserialize("invalid|||data").isEmpty())
    }
}
