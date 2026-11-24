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

    @Test fun destinationAheadDoesNotMeanArrived() {
        val result = requireNotNull(RouteProgress.nextTurn(route, 48.1003, 11.5010))
        assertEquals("Zum Ziel weitergehen", result.first.instruction)
        assertTrue(result.second > 60)
    }

    @Test fun arrivalOnlyNearActualRouteEndpoint() {
        val near = requireNotNull(RouteProgress.nextTurn(route, 48.10095, 11.5010))
        assertEquals("Ziel erreicht", near.first.instruction)
        assertTrue(near.second <= 20)
    }

    @Test fun doesNotArriveThirtyMetersBeforeStop() {
        val before = requireNotNull(RouteProgress.nextTurn(route, 48.10073, 11.5010))
        assertEquals("Zum Ziel weitergehen", before.first.instruction)
        assertTrue(before.second >= 25)
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
    @Test fun mapHeadingFollowsProjectedSegment() {
        assertEquals(90.0f, requireNotNull(RouteProgress.heading(path, 48.1000, 11.5005)), 1.0f)
        assertEquals(0.0f, requireNotNull(RouteProgress.heading(path, 48.1005, 11.5010)), 1.0f)
    }

    @Test fun mapHeadingIsUnavailableFarOffRoute() {
        assertNull(RouteProgress.heading(path, 48.1050, 11.5050))
    }

    @Test fun projectionHandlesDuplicateVerticesWithoutLosingDistance() {
        val withDuplicate = listOf(
            48.1000 to 11.5000,
            48.1000 to 11.5000,
            48.1000 to 11.5010,
            48.1000 to 11.5010,
            48.1010 to 11.5010
        )
        val projected = requireNotNull(RouteProgress.project(withDuplicate, 48.1005, 11.5010))
        assertEquals(3, projected.segmentIndex)
        assertTrue(projected.lateralMeters < 1.0)
        assertTrue(projected.alongMeters > 120.0)
    }

    @Test fun projectionRejectsNonFiniteLongitudeAndSinglePointRoute() {
        assertNull(RouteProgress.project(path, 48.1000, Double.POSITIVE_INFINITY))
        assertNull(RouteProgress.project(path.take(1), 48.1000, 11.5000))
    }

    @Test fun headingRejectsPositionBeyondOffRouteThreshold() {
        assertNull(RouteProgress.heading(path, 48.1050, 11.5050, maxOffRouteMeters = 10.0))
    }

    @Test fun rejectsNonFiniteRouteVerticesInsteadOfReturningNaNProjection() {
        val invalidLatitude = listOf(48.1000 to 11.5000, Double.NaN to 11.5010)
        val invalidLongitude = listOf(48.1000 to 11.5000, 48.1000 to Double.POSITIVE_INFINITY)
        assertNull(RouteProgress.project(invalidLatitude, 48.1000, 11.5005))
        assertNull(RouteProgress.project(invalidLongitude, 48.1000, 11.5005))
        assertNull(RouteProgress.heading(invalidLatitude, 48.1000, 11.5005))
    }


}
