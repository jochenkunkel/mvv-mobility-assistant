package de.mvv.mobility.data

import org.junit.Assert.*
import org.junit.Test

class ActiveLegStateTest {
    @Test fun unknownAndWalkingLegNeverEnableGuard() {
        val initial = ActiveLegState().selectJourney("journey-a")
        assertFalse(initial.guardEnabled(true, initial.generation))
        val walking = initial.activateLeg(1, 3)
        assertFalse(walking.guardEnabled(false, walking.generation))
        assertEquals(walking, walking.confirmBoarding(1, false))
    }

    @Test fun boardingDoesNotLeakAcrossTransfers() {
        val a = ActiveLegState().selectJourney("journey-a").activateLeg(0, 3)
            .confirmBoarding(0, true)
        assertTrue(a.guardEnabled(true, a.generation))
        val walk = a.activateLeg(1, 3)
        assertFalse(walk.guardEnabled(false, walk.generation))
        val b = walk.activateLeg(2, 3)
        assertFalse(b.guardEnabled(true, b.generation))
        assertFalse(b.guardEnabled(true, a.generation))
        val confirmedB = b.confirmBoarding(2, true)
        assertTrue(confirmedB.guardEnabled(true, confirmedB.generation))
    }

    @Test fun invalidIndexAndDifferentJourneyInvalidateConfirmation() {
        val a = ActiveLegState().selectJourney("a").activateLeg(0, 2)
            .confirmBoarding(0, true)
        val invalid = a.activateLeg(9, 2)
        assertNull(invalid.activeLegIndex)
        assertFalse(invalid.guardEnabled(true, invalid.generation))
        val changed = a.selectJourney("b").activateLeg(0, 2)
        assertFalse(changed.guardEnabled(true, changed.generation))
        assertFalse(changed.guardEnabled(true, a.generation))
    }

    @Test fun staleCallbackAndResetAreRejected() {
        val a = ActiveLegState().selectJourney("a").activateLeg(0, 2)
            .confirmBoarding(0, true)
        val next = a.activateLeg(1, 2).confirmBoarding(1, true)
        assertFalse(next.guardEnabled(true, a.generation))
        assertTrue(next.guardEnabled(true, next.generation))
        val reset = next.reset()
        assertFalse(reset.guardEnabled(true, next.generation))
        assertNull(reset.journeyKey)
    }
}
