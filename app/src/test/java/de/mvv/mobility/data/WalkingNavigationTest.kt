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

    @Test fun rejectsMissingRoutes() {
        try {
            WalkingNavigation.parse("""{"code":"NoRoute","routes":[]}""")
            throw AssertionError("NoRoute must be rejected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("Keine Fußgängerroute"))
        }
    }
}
