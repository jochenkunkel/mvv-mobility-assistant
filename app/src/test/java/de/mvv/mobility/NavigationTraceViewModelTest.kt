package de.mvv.mobility

import de.mvv.mobility.data.TraceFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTraceViewModelTest {
    @Test fun retainsTrackAcrossRerouteAndClearsOnlyExplicitly() {
        val state = NavigationTraceViewModel()
        val a = TraceFix(48.1000, 11.5000, 1_000L, 5f)
        val b = TraceFix(48.1001, 11.5000, 12_000L, 5f)
        state.acceptFix(a)
        state.acceptFix(b)
        val beforeReroute = state.walkedTrace
        // A reroute has no destructive API on the trace owner.
        assertEquals(beforeReroute, state.walkedTrace)
        assertEquals(2, state.walkedTrace.fixes.size)
        state.clearSession()
        assertTrue(state.walkedTrace.fixes.isEmpty())
    }

    @Test fun invalidFixDoesNotEraseRecordedHistory() {
        val state = NavigationTraceViewModel()
        state.acceptFix(TraceFix(48.1000, 11.5000, 1_000L, 5f))
        state.acceptFix(TraceFix(48.1001, 11.5000, 12_000L, 5f))
        val before = state.walkedTrace
        state.acceptFix(TraceFix(48.1002, 11.5000, 11_000L, 5f))
        assertEquals(before, state.walkedTrace)
    }
}
