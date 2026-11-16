package de.mvv.mobility.data

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DepartureRecommendationTest {
    private fun at(day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, day, hour, minute)
        }.timeInMillis

    private fun journey(departure: String, steps: List<JourneyStep>, walk: Int = 0) =
        JourneyOption(departure, "09:45", "U3", 0, walk, steps = steps)

    @Test fun walkingLegStartIsNotSubtractedTwice() {
        val walking = JourneyStep("", "Fußweg", "Start", "Haltestelle", "09:10", "09:20", true, 10)
        val vehicle = JourneyStep("U3", "U-Bahn", "Haltestelle", "Ziel", "09:20", "09:45", false, 25)
        val leave = recommendedLeaveMillis(journey("09:10", listOf(walking, vehicle), 10), at(16, 10, 0))
        assertEquals(at(16, 9, 7), leave)
    }

    @Test fun fallbackSubtractsAccessWalkAndSafetyBuffer() {
        val vehicle = JourneyStep("U3", "U-Bahn", "Haltestelle", "Ziel", "09:20", "09:45", false, 25)
        val leave = recommendedLeaveMillis(journey("09:20", listOf(vehicle), 8), at(16, 10, 0))
        assertEquals(at(16, 9, 9), leave)
    }

    @Test fun departureAcrossMidnightUsesPreviousDay() {
        val leave = recommendedLeaveMillis(journey("23:50", emptyList()), at(16, 0, 15))
        assertEquals(at(15, 23, 47), leave)
    }
    @Test fun missingWalkingStartUsesBusTimeMinusAccessWalk() {
        val walking = JourneyStep("", "Fußweg", "GPS", "Bellinzonastraße", "", "14:53", true, 4)
        val bus = JourneyStep("56", "Bus", "Bellinzonastraße", "Ziel", "14:53", "15:10", false, 17)
        val leave = recommendedLeaveMillis(journey("", listOf(walking, bus), 4), at(16, 14, 32))
        assertEquals(at(16, 14, 46), leave)
    }

}
