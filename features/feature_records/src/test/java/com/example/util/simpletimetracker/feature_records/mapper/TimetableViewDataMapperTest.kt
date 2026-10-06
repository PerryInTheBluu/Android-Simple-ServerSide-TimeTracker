package com.example.util.simpletimetracker.feature_records.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimetableViewDataMapperTest {

    private val mapper = TimetableViewDataMapper()

    @Test
    fun timestampOf_acrossDaylightSavingSpringTransition() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))

            // 2024-03-31 is the spring DST transition day in Germany (clock jumped 02:00 -> 03:00)
            val cal = Calendar.getInstance()
            cal.set(2024, Calendar.MARCH, 31, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val midnight = cal.timeInMillis

            // 10:00 AM (minute 600 = 10 * 60)
            val timestamp = mapper.timestampOf(midnight, 600)

            val checkCal = Calendar.getInstance()
            checkCal.timeInMillis = timestamp
            assertEquals(2024, checkCal.get(Calendar.YEAR))
            assertEquals(Calendar.MARCH, checkCal.get(Calendar.MONTH))
            assertEquals(31, checkCal.get(Calendar.DAY_OF_MONTH))
            assertEquals(10, checkCal.get(Calendar.HOUR_OF_DAY))
            assertEquals(0, checkCal.get(Calendar.MINUTE))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun timestampOf_acrossDaylightSavingAutumnTransition() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))

            // 2024-10-27 is the autumn DST transition day in Germany (clock jumped back 03:00 -> 02:00)
            val cal = Calendar.getInstance()
            cal.set(2024, Calendar.OCTOBER, 27, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val midnight = cal.timeInMillis

            // 10:00 AM (minute 600)
            val timestamp = mapper.timestampOf(midnight, 600)

            val checkCal = Calendar.getInstance()
            checkCal.timeInMillis = timestamp
            assertEquals(2024, checkCal.get(Calendar.YEAR))
            assertEquals(Calendar.OCTOBER, checkCal.get(Calendar.MONTH))
            assertEquals(27, checkCal.get(Calendar.DAY_OF_MONTH))
            assertEquals(10, checkCal.get(Calendar.HOUR_OF_DAY))
            assertEquals(0, checkCal.get(Calendar.MINUTE))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
