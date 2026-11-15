package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AppointmentJourneySelectionTest {
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(berlin).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun trip(arrival: String) = JourneyOption(
        departure = "23:00", arrival = arrival, summary = "U3",
        changes = 0, walkingMinutes = 0
    )

    @Test fun selectsArrivalBeforeMidnightForAppointmentAfterMidnight() {
        val result = selectAppointmentJourneys(
            listOf(trip("23:50")), at(2026, 11, 16, 0, 15)
        )
        assertEquals(1, result.size)
    }

    @Test fun excludesPreviousDayArrivalOutsideTwoHourWindow() {
        val result = selectAppointmentJourneys(
            listOf(trip("21:00")), at(2026, 11, 16, 0, 15)
        )
        assertTrue(result.isEmpty())
    }

    @Test fun excludesArrivalAfterAppointment() {
        val result = selectAppointmentJourneys(
            listOf(trip("00:30")), at(2026, 11, 16, 0, 15)
        )
        assertTrue(result.isEmpty())
    }

    @Test fun prefersLatestArrivalBeforeAppointment() {
        val result = selectAppointmentJourneys(
            listOf(trip("23:45"), trip("00:05"), trip("23:55")),
            at(2026, 11, 16, 0, 15)
        )
        assertEquals(listOf("00:05", "23:55", "23:45"), result.map { it.arrival })
    }
    @Test fun equalArrivalPrefersFewerTransfersThenShorterWalk() {
        val options = listOf(
            trip("09:45").copy(departure = "09:10", changes = 2, walkingMinutes = 3),
            trip("09:45").copy(departure = "09:15", changes = 0, walkingMinutes = 12),
            trip("09:45").copy(departure = "09:20", changes = 0, walkingMinutes = 4)
        )
        val result = selectAppointmentJourneys(options, at(2026, 11, 16, 10, 0))
        assertEquals(listOf("09:20", "09:15", "09:10"), result.map { it.departure })
    }

    @Test fun differentDeparturesWithSameLineAndArrivalRemainAvailable() {
        val options = listOf(
            trip("09:45").copy(departure = "09:10"),
            trip("09:45").copy(departure = "09:20")
        )
        val result = selectAppointmentJourneys(options, at(2026, 11, 16, 10, 0))
        assertEquals(2, result.size)
    }

}
