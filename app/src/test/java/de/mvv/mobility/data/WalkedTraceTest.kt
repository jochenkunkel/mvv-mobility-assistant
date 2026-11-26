package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WalkedTraceTest {
    private fun fix(time: Long, lat: Double = 48.1, accuracy: Float = 5f) =
        TraceFix(lat, 11.5, time, accuracy)

    @Test fun monotonicElapsedTimeSurvivesWallClockChange() {
        val first = TraceFix(48.1, 11.5, 1_000L, 5f, 1_000_000_000L)
        // Wall clock jumps backwards (DST/manual change); monotonic clock still advances.
        val second = TraceFix(48.10001, 11.5, 500L, 5f, 12_000_000_000L)
        val trace = WalkedTrace().accept(first).accept(second)
        assertEquals(2, trace.fixes.size)
        assertEquals(1, trace.segments.size)
    }

    @Test fun mixedClockBasesAreRejectedInsteadOfMisordered() {
        val first = TraceFix(48.1, 11.5, 1_000L, 5f, 1_000_000_000L)
        val secondNoElapsed = TraceFix(48.10001, 11.5, 2_000L, 5f)
        val trace = WalkedTrace().accept(first)
        assertSame(trace, trace.accept(secondNoElapsed))
    }

    @Test fun gapBoundaryUsesMonotonicElapsedTime() {
        val first = TraceFix(48.1, 11.5, 1_000L, 5f, 1_000_000_000L)
        val afterGap = TraceFix(48.1001, 11.5, 100L, 5f, 200_000_000_000L)
        val trace = WalkedTrace().accept(first).accept(afterGap)
        assertEquals(2, trace.segments.size)
    }

    @Test fun orderedFixesStayInOneSegment() {
        val trace = WalkedTrace().accept(fix(1000)).accept(fix(2000, 48.10001)).accept(fix(3000, 48.10002))
        assertEquals(listOf(48.1, 48.10001, 48.10002), trace.fixes.map { it.latitude })
        assertEquals(1, trace.segments.size)
    }

    @Test fun rejectedFixDoesNotEraseAcceptedHistory() {
        val trace = WalkedTrace().accept(fix(1000))
        assertSame(trace, trace.accept(fix(2000, accuracy = 90f)))
        assertSame(trace, trace.accept(fix(500)))
        assertSame(trace, trace.accept(fix(2000, lat = Double.NaN)))
        assertEquals(1, trace.fixes.size)
    }

    @Test fun longGapStartsNewSegmentWithoutConnectingLine() {
        val trace = WalkedTrace().accept(fix(1000)).accept(fix(2000))
            .accept(fix(200_001, 48.3))
        assertEquals(listOf(2, 1), trace.segments.map { it.size })
        assertEquals(3, trace.fixes.size)
    }

    @Test fun plannedRouteChangesCannotMutateTrace() {
        val trace = WalkedTrace().accept(fix(1000)).accept(fix(2000))
        val plannedRoute = mutableListOf(48.0 to 11.0)
        plannedRoute.clear()
        plannedRoute.add(49.0 to 12.0)
        assertEquals(listOf(48.1, 48.1), trace.fixes.map { it.latitude })
        assertEquals(2, trace.fixes.size)
    }

    @Test fun impossibleJumpIsRejectedWithoutResettingTrace() {
        val first = WalkedTrace().accept(fix(1000))
        val rejected = first.accept(fix(2000, 48.2))
        assertSame(first, rejected)
        val resumed = rejected.accept(fix(3000, 48.10001))
        assertEquals(listOf(48.1, 48.10001), resumed.fixes.map { it.latitude })
        assertEquals(1, resumed.segments.size)
    }

    @Test fun speedThresholdIsStrictAndConfigurable() {
        val first = WalkedTrace().accept(fix(1000))
        val next = fix(2000, 48.1001) // approximately 11.1 m in one second
        assertEquals(2, first.accept(next).fixes.size)
        assertSame(first, first.accept(next, maxSpeedMetersPerSecond = 5.0))
    }

    @Test fun longGapDoesNotDrawTeleportLine() {
        val trace = WalkedTrace().accept(fix(1000))
            .accept(fix(200_000, 49.0))
        assertEquals(listOf(1, 1), trace.segments.map { it.size })
    }
    @Test fun exactGapBoundaryStaysConnectedWhenMovementIsPlausible() {
        val first = WalkedTrace().accept(fix(1_000))
        val trace = first.accept(fix(121_000, 48.10001))
        assertEquals(listOf(2), trace.segments.map { it.size })
    }

    @Test fun firstMillisecondBeyondGapStartsSeparateSegment() {
        val trace = WalkedTrace().accept(fix(1_000))
            .accept(fix(121_001, 48.10001))
        assertEquals(listOf(1, 1), trace.segments.map { it.size })
    }

    @Test fun rejectedJumpDoesNotMoveGapAnchor() {
        val first = WalkedTrace().accept(fix(1_000))
        val rejected = first.accept(fix(60_000, 49.0))
        assertSame(first, rejected)
        val resumed = rejected.accept(fix(122_000, 48.10001))
        assertEquals(listOf(1, 1), resumed.segments.map { it.size })
    }

    @Test fun boundaryCoordinatesAndAccuracyAreValidated() {
        val first = WalkedTrace().accept(fix(1_000))
        assertSame(first, first.accept(TraceFix(90.0001, 11.5, 2_000, 5f)))
        assertSame(first, first.accept(TraceFix(48.1, -180.0001, 2_000, 5f)))
        assertSame(first, first.accept(TraceFix(48.1, 11.5, 2_000, Float.NaN)))
        assertSame(first, first.accept(TraceFix(48.1, 11.5, 2_000, -1f)))
        assertSame(first, first.accept(TraceFix(48.1, 11.5, 2_000, 35.01f)))
        assertEquals(2, first.accept(TraceFix(48.1, 11.5, 2_000, 35f)).fixes.size)
    }

    @Test fun invalidFilterConfigurationDoesNotAlterTrace() {
        val first = WalkedTrace().accept(fix(1_000))
        val next = fix(2_000)
        assertSame(first, first.accept(next, maxAccuracyMeters = Float.NaN))
        assertSame(first, first.accept(next, maxSpeedMetersPerSecond = Double.POSITIVE_INFINITY))
        assertSame(first, first.accept(next, maxSpeedMetersPerSecond = 0.0))
        assertSame(first, first.accept(next, gapMillis = -1))
    }

    @Test fun duplicateTimestampDoesNotAppendOrResetTrace() {
        val first = WalkedTrace().accept(fix(1_000))
        assertSame(first, first.accept(fix(1_000, 48.10001)))
        assertEquals(1, first.fixes.size)
    }

    @Test fun antimeridianCrossingUsesShortestGeodesicDistance() {
        // A 0.0002-degree crossing of +/-180 is only about 22 m at the equator,
        // not a 40,000 km teleport. The speed filter must use spherical distance.
        val start = TraceFix(0.0, 179.9999, 1_000L, 5f)
        val crossing = TraceFix(0.0, -179.9999, 4_000L, 5f)
        val trace = WalkedTrace().accept(start).accept(crossing)
        assertEquals(2, trace.fixes.size)
        assertEquals(1, trace.segments.size)
        assertEquals(-179.9999, trace.fixes.last().longitude, 0.0)
    }


    @Test fun boundedRetentionKeepsNewestFixesWithoutBridgingGaps() {
        val trace = WalkedTrace()
            .accept(fix(1_000))
            .accept(fix(2_000, 48.10001))
            .accept(fix(200_000, 48.2))
            .accept(fix(201_000, 48.20001))
        val retained = trace.retainLast(3)
        assertEquals(listOf(1, 2), retained.segments.map { it.size })
        assertEquals(listOf(2_000L, 200_000L, 201_000L), retained.fixes.map { it.timestampMillis })
        assertEquals(4, trace.fixes.size)
    }

    @Test fun boundedRetentionHandlesEmptyZeroAndUnchangedHistory() {
        val trace = WalkedTrace().accept(fix(1_000)).accept(fix(2_000))
        assertSame(trace, trace.retainLast(2))
        assertEquals(emptyList<TraceFix>(), trace.retainLast(0).fixes)
        assertEquals(emptyList<TraceFix>(), WalkedTrace().retainLast(0).fixes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundedRetentionRejectsNegativeLimit() {
        WalkedTrace().retainLast(-1)
    }


    @Test fun boundedTraceContinuesAfterTrimmingWithoutJoiningSegments() {
        val original = WalkedTrace()
            .accept(fix(1_000))
            .accept(fix(2_000, 48.10001))
            .accept(fix(200_000, 48.2))
        val trimmed = original.retainLast(2)
        val continued = trimmed.accept(fix(201_000, 48.20001)).retainLast(2)
        assertEquals(listOf(200_000L, 201_000L), continued.fixes.map { it.timestampMillis })
        assertEquals(listOf(2), continued.segments.map { it.size })
        assertEquals(listOf(1, 1), trimmed.segments.map { it.size })
        assertEquals(3, original.fixes.size)
    }

}
