package de.mvv.mobility.data

import org.junit.Assert.*
import org.junit.Test

class MvvBoardingCoordinatesTest {
    @Test fun stopCandidatesCarryEfaCoordinates() {
        val json = """
            {"destination":{"points":[
              {"stateless":"de:09162:1","name":"Olympiapark West","anyType":"stop",
               "quality":"300","coord":{"x":11.5536,"y":48.1775},"platform":"2"}
            ]}}
        """.trimIndent()
        val candidate = MvvJourneyRepository.candidates(json).single()
        val position = requireNotNull(candidate.position)
        assertEquals(48.1775, position.latitude, 0.000001)
        assertEquals(11.5536, position.longitude, 0.000001)
        assertEquals("de:09162:1", position.stopId)
        assertEquals("2", position.platform)
    }

    @Test fun retainsBoardingStopCoordinatesAndPlatform() {
        val json = """
            {"trips":{"trip":{"legs":[
              {"mode":{"product":"Fussweg"},"timeMinute":4,
               "points":[{"name":"Start","dateTime":{"time":"14:30"}},
                         {"name":"Fürstenried West","dateTime":{"time":"14:34"}}]},
              {"mode":{"product":"Bus","number":"56"},"timeMinute":10,
               "points":[{"name":"Fürstenried West","stateless":"stop-123",
                          "platform":"B","coord":{"x":11.4765,"y":48.0924},
                          "dateTime":{"time":"14:35"}},
                         {"name":"Ziel","dateTime":{"time":"14:45"}}]}
            ]}}}
        """.trimIndent()
        val journey = MvvJourneyRepository.parseTrips(json, 4f).single()
        val stop = requireNotNull(journey.steps.first { !it.walking }.fromStop)
        assertEquals(48.0924, stop.latitude, 0.000001)
        assertEquals(11.4765, stop.longitude, 0.000001)
        assertEquals("stop-123", stop.stopId)
        assertEquals("B", stop.platform)
    }

    @Test fun missingCoordinatesDoNotCreateGuessedStop() {
        val json = """
            {"trips":{"trip":{"legs":[
              {"mode":{"product":"Bus","number":"56"},
               "points":[{"name":"Fürstenried West","dateTime":{"time":"14:35"}},
                         {"name":"Ziel","dateTime":{"time":"14:45"}}]}
            ]}}}
        """.trimIndent()
        assertNull(MvvJourneyRepository.parseTrips(json, 4f).single().steps.single().fromStop)
    }
}
