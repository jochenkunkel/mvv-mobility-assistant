package de.mvv.mobility.domain

import org.junit.Assert.*
import org.junit.Test

class JourneyRulesTest {
    @Test fun missingDestinationRequestsConfirmation() {
        val journey = Journey("Therapie", null, 10, 20, 1, 5)
        assertTrue(JourneyRules.needsDestinationConfirmation(journey))
        assertFalse(JourneyRules.needsDestinationConfirmation(journey.copy(destination = "München Hbf")))
    }
    @Test fun walkingDurationAdaptsToIndividualSpeed() {
        assertEquals(10, JourneyRules.walkingDurationMinutes(600, 1.0))
        assertEquals(5, JourneyRules.walkingDurationMinutes(600, 2.0))
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidSpeedRejected() { JourneyRules.walkingDurationMinutes(100, 0.0) }
    @Test fun comfortPrefersFewerTransfers() {
        val direct = JourneyRules.comfortScore(0, 5, 35)
        val twoTransfers = JourneyRules.comfortScore(2, 5, 25)
        assertTrue(direct < twoTransfers)
    }
    @Test fun defaultPreferenceIsComfortAndLearnsSpeedLater() {
        val preferences = TravelPreferences()
        assertEquals(RoutePriority.COMFORT, preferences.routePriority)
        assertNull(preferences.walkingSpeedMetersPerSecond)
    }
}
