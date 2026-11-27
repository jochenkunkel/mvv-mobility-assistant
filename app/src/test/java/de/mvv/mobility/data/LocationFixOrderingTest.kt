package de.mvv.mobility.data

import org.junit.Assert.*
import org.junit.Test

class LocationFixOrderingTest {
    @Test fun delayedCallbackCannotReplaceNewerFix() {
        val ordering = LocationFixOrdering()
        assertTrue(ordering.accept(100_000_000_000L))
        assertTrue(ordering.accept(102_000_000_000L))
        assertFalse(ordering.accept(101_000_000_000L))
        assertFalse(ordering.accept(102_000_000_000L))
        assertTrue(ordering.accept(103_000_000_000L))
    }

    @Test fun rejectsUnknownTimestampAndResetsBetweenSessions() {
        val ordering = LocationFixOrdering()
        assertFalse(ordering.accept(0L))
        assertFalse(ordering.accept(-1L))
        assertTrue(ordering.accept(500L))
        assertFalse(ordering.accept(400L))
        ordering.reset()
        assertTrue(ordering.accept(400L))
    }

    @Test fun longGpsGapDoesNotPreventLaterFix() {
        val ordering = LocationFixOrdering()
        assertTrue(ordering.accept(1_000_000_000L))
        assertTrue(ordering.accept(600_000_000_000L))
    }
}
