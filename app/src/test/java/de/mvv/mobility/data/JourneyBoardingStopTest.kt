package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyBoardingStopTest {
    @Test fun resolvesFirstVehicleOriginInsteadOfJourneyDestination() {
        val boarding = StopPosition(48.1, 11.5, "boarding", "2")
        val journeyDestination = StopPosition(48.2, 11.6, "destination", "8")
        val option = JourneyOption(
            departure = "09:00",
            arrival = "10:00",
            summary = "U3",
            changes = 0,
            walkingMinutes = 35,
            lines = listOf("U3"),
            products = listOf("U-Bahn"),
            steps = listOf(
                JourneyStep("", "Fußweg", "Start", "Haltestelle", "08:50", "09:00",
                    true, 10, toStop = boarding),
                JourneyStep("U3", "U-Bahn", "Haltestelle", "Ziel", "09:00", "10:00",
                    false, 60, fromStop = boarding, toStop = journeyDestination)
            )
        )

        assertEquals(boarding, option.boardingStop())
        assertEquals("2", option.boardingStop()?.platform)
        assertFalse(option.hasRouteToBoardingStop(
            WalkingRoute(emptyList(), 900, destination = journeyDestination)
        ))
        assertTrue(option.hasRouteToBoardingStop(
            WalkingRoute(emptyList(), 150, destination = boarding)
        ))
    }

    @Test fun journeyWithoutVehicleHasNoBoardingStop() {
        val option = JourneyOption(
            "08:00", "08:30", "Nur zu Fuß", 0, 30,
            steps = listOf(JourneyStep("", "Fußweg", "Start", "Ziel",
                "08:00", "08:30", true, 30))
        )

        assertNull(option.boardingStop())
    }
}
