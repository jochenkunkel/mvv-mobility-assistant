package de.mvv.mobility.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResponseGuardTest {
    @Test fun latestResponseCanUpdateJourneys() {
        assertTrue(isCurrentSearchResponse(requestId = 3, currentGeneration = 3))
    }

    @Test fun olderResponseCannotOverwriteNewerSearch() {
        // Request 1 completes after request 2: its callback must be ignored.
        assertFalse(isCurrentSearchResponse(requestId = 1, currentGeneration = 2))
        assertTrue(isCurrentSearchResponse(requestId = 2, currentGeneration = 2))
    }

    @Test fun changingDestinationFilterOrLocationInvalidatesPendingResponse() {
        // Any input change increments the generation, including a new GPS fix.
        assertFalse(isCurrentSearchResponse(requestId = 7, currentGeneration = 8))
    }
}
