package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyTimeTest {
    private fun option(clock: String, absolute: Long? = null) = JourneyOption(
        clock, "13:25", "56 / U6", 0, 10,
        listOf("56", "U6"), listOf("Bus", "U-Bahn"),
        listOf(JourneyStep("56", "Bus", "A", "B", clock, "13:25", false, 10)),
        departureInstantMillis = absolute
    )

    @Test fun clockOnlyUsesSelectedSearchDayNotToday() {
        val thursdayEvening = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 19, 42)
        val saturday = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 28), 12, 34)
        val departure = JourneyTime.departureMillis(option("12:34"), saturday)
        assertEquals(saturday, departure)
        assertFalse(departure!! < thursdayEvening)
        assertTrue(departure > thursdayEvening)
    }

    @Test fun absoluteEfaInstantWinsOverClock() {
        val absolute = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 28), 12, 34)
        assertEquals(absolute, JourneyTime.departureMillis(
            option("23:59", absolute), EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 19, 42)))
    }

    @Test fun pastStatusComparesFullInstants() {
        val thursday = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 19, 42)
        val saturday = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 28), 12, 34)
        assertFalse(JourneyTime.departureMillis(option("12:34"), saturday)!! < thursday)
    }

    @Test fun futureSearchDayIsNotMarkedPastAgainstTodaysClock() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 19, 42)
        val futureSearchDay = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 28), 9, 15)
        assertFalse(JourneyTime.isPast(option("09:15"), now, futureSearchDay))
    }

    @Test fun selectedDayDoesNotRollEarlyClockToTomorrow() {
        val selectedDay = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 21, 0)
        val expectedSameDay = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 9, 15)
        assertEquals(expectedSameDay, JourneyTime.departureMillis(option("09:15"), selectedDay))
        assertTrue(JourneyTime.isPast(option("09:15"), selectedDay, selectedDay))
    }
}
