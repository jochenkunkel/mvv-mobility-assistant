package de.mvv.mobility.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFreshnessTest {
    private val now = 1_800_000_000_000L

    @Test fun currentFixIsFresh() {
        assertTrue(LocationFreshness.isFresh(now, now))
    }

    @Test fun twoMinuteBoundaryIsStillFresh() {
        assertTrue(LocationFreshness.isFresh(now - 120_000L, now))
    }

    @Test fun olderFixIsRejected() {
        assertFalse(LocationFreshness.isFresh(now - 120_001L, now))
    }

    @Test fun futureTimestampIsRejected() {
        assertFalse(LocationFreshness.isFresh(now + 1L, now))
    }

    @Test fun unsetTimestampIsRejected() {
        assertFalse(LocationFreshness.isFresh(0L, now))
    }
    @Test fun monotonicFreshnessAcceptsBoundary() {
        assertTrue(LocationFreshness.isFreshElapsed(1_000_000_000L, 121_000_000_000L))
    }

    @Test fun monotonicFreshnessRejectsOldAndFutureFixes() {
        assertFalse(LocationFreshness.isFreshElapsed(1_000_000_000L, 121_001_000_000L))
        assertFalse(LocationFreshness.isFreshElapsed(3_000_000_000L, 2_000_000_000L))
    }

    @Test fun elapsedAgeUsesMonotonicClock() {
        assertTrue(LocationFreshness.elapsedAgeMillis(1_000_000_000L, 2_500_000_000L) == 1_500L)
        assertTrue(LocationFreshness.elapsedAgeMillis(3_000_000_000L, 2_000_000_000L) == null)
    }

    @Test fun wallClockFallbackRejectsFutureAndMissingTimes() {
        assertTrue(LocationFreshness.wallAgeMillis(now - 500L, now) == 500L)
        assertTrue(LocationFreshness.wallAgeMillis(now + 1L, now) == null)
        assertTrue(LocationFreshness.wallAgeMillis(0L, now) == null)
    }

    @Test fun monotonicAgeIsUnaffectedByWallClockJump() {
        val fixNanos = 5_000_000_000L
        val nowNanos = 15_000_000_000L
        assertTrue(LocationFreshness.isFreshElapsed(fixNanos, nowNanos, 45_000L))
        assertTrue(LocationFreshness.elapsedAgeMillis(fixNanos, nowNanos) == 10_000L)
    }
}
