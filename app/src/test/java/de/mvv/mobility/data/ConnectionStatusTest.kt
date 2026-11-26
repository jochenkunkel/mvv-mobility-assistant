package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionStatusTest {
    private fun busStep(index: Int, clock: String) = JourneyStep(
        "U3", "U-Bahn", "Haltestelle $index", "Ziel", clock, "10:00", false, 10
    )

    @Test fun onlyUpcomingTransitStepIsOffered() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 9, 50)
        val steps = listOf(
            JourneyStep("", "Fußweg", "Start", "Haltestelle", "09:40", "09:50", true, 10),
            busStep(0, "09:45"), // already departed
            busStep(1, "10:05")
        )
        val upcoming = ConnectionStatusResolver.upcoming(steps, now, emptyMap())
        assertEquals(2, upcoming?.stepIndex)
    }

    @Test fun checkedInAndMissedStepsAreSkipped() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 9, 50)
        val steps = listOf(busStep(0, "10:05"), busStep(1, "10:20"))
        assertNull(ConnectionStatusResolver.upcoming(steps, now, mapOf(0 to ConnectionCheckState.CHECKED_IN))?.stepIndex?.let { if (it == 0) it else null })
        assertEquals(1, ConnectionStatusResolver.upcoming(
            steps, now, mapOf(0 to ConnectionCheckState.MISSED))?.stepIndex)
    }

    @Test fun noUpcomingConnectionForWalkingOnlyJourney() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 9, 50)
        val steps = listOf(JourneyStep("", "Fußweg", "Start", "Ziel", "09:50", "10:10", true, 20))
        assertNull(ConnectionStatusResolver.upcoming(steps, now, emptyMap()))
    }
}
