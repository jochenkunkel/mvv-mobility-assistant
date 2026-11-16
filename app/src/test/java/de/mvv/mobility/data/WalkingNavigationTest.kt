package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkingNavigationTest {
    @Test fun parsesFootTurnStepsOffline() {
        val json = """{"code":"Ok","routes":[{"distance":170.4,"legs":[{"steps":[
            {"distance":100,"maneuver":{"type":"depart","modifier":"straight","location":[11.5,48.1]}},
            {"distance":70.4,"maneuver":{"type":"turn","modifier":"left","location":[11.501,48.101]}}
        ]}]}]}"""
        val route = WalkingNavigation.parse(json)
        assertEquals(170, route.totalMeters)
        assertEquals(2, route.turns.size)
        assertEquals("Links abbiegen", route.turns[1].instruction)
        assertEquals(100, route.turns[0].distanceMeters)
    }


    @Test fun preservesGeoJsonFootPathCoordinatesInLatitudeLongitudeOrder() {
        // OSRM GeoJSON uses [longitude, latitude], while Android Location uses latitude/longitude.
        val json = """{"code":"Ok","routes":[{"distance":120.2,
            "geometry":{"type":"LineString","coordinates":[[11.5001,48.1002],[11.5013,48.1014]]},
            "legs":[{"steps":[
                {"distance":120.2,"maneuver":{"type":"depart","modifier":"straight","location":[11.5001,48.1002]}}
            ]}]}]}"""
        val route = WalkingNavigation.parse(json)
        assertEquals(120, route.totalMeters)
        assertEquals(2, route.path.size)
        assertEquals(48.1002, route.path[0].first, 0.0000001)
        assertEquals(11.5001, route.path[0].second, 0.0000001)
        assertEquals(48.1014, route.path[1].first, 0.0000001)
        assertEquals(11.5013, route.path[1].second, 0.0000001)
    }

    @Test fun rejectsEmptyManeuverListInsteadOfDisplayingFakeRoute() {
        val json = """{"code":"Ok","routes":[{"distance":0,"legs":[{"steps":[]}]}]}"""
        try {
            WalkingNavigation.parse(json)
            throw AssertionError("A route without turn steps must be rejected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("Keine Abbiegeanweisungen"))
        }
    }

    @Test fun rejectsMissingRoutes() {
        try {
            WalkingNavigation.parse("""{"code":"NoRoute","routes":[]}""")
            throw AssertionError("NoRoute must be rejected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("Keine Fußgängerroute"))
        }
    }
}
