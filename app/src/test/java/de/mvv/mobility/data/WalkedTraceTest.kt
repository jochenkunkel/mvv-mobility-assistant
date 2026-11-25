package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WalkedTraceTest {
    private fun fix(time: Long, lat: Double = 48.1, accuracy: Float = 5f) =
        TraceFix(lat, 11.5, time, accuracy)

    @Test fun orderedFixesStayInOneSegment() {
        val trace = WalkedTrace().accept(fix(1000)).accept(fix(2000, 48.2)).accept(fix(3000, 48.3))
        assertEquals(listOf(48.1, 48.2, 48.3), trace.fixes.map { it.latitude })
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
}
