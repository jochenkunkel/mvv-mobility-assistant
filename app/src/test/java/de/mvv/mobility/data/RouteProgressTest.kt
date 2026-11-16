package de.mvv.mobility.data

import org.junit.Assert.*
import org.junit.Test

class RouteProgressTest {
    private val path = listOf(
        48.1000 to 11.5000,
        48.1000 to 11.5010,
        48.1010 to 11.5010
    )
    private val route = WalkingRoute(
        turns = listOf(
            WalkingTurn("Geradeaus", "↑", 48.1000, 11.5000, 75),
            WalkingTurn("Rechts abbiegen", "↱", 48.1000, 11.5010, 111),
            WalkingTurn("Ziel erreicht", "✓", 48.1010, 11.5010, 0)
        ),
        totalMeters = 186,
        path = path
    )

    @Test fun projectsOntoSegmentRatherThanNearestVertex() {
        val p = requireNotNull(RouteProgress.project(path, 48.1000, 11.5005))
        assertEquals(0, p.segmentIndex)
        assertTrue(p.lateralMeters < 1.0)
        assertTrue(p.alongMeters in 30.0..45.0)
    }

    @Test fun upcomingTurnIsNotSkippedTwentyMetersBeforeCorner() {
        val result = requireNotNull(RouteProgress.nextTurn(route, 48.1000, 11.50075))
        assertEquals("Rechts abbiegen", result.first.instruction)
        assertTrue(result.second in 15..23)
    }

    @Test fun switchesToArrivalAfterPassingCorner() {
        val result = requireNotNull(RouteProgress.nextTurn(route, 48.1003, 11.5010))
        assertEquals("Ziel erreicht", result.first.instruction)
        assertTrue(result.second > 60)
    }

    @Test fun rejectsPositionFarOffRoute() {
        assertNull(RouteProgress.nextTurn(route, 48.1050, 11.5050))
    }

    @Test fun rejectsMissingGeometry() {
        assertNull(RouteProgress.nextTurn(route.copy(path = emptyList()), 48.1000, 11.5000))
    }

    @Test fun invalidCoordinatesAreRejected() {
        assertNull(RouteProgress.project(path, Double.NaN, 11.5))
    }
}
