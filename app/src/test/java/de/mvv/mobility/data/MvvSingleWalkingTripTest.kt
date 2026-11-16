package de.mvv.mobility.data

import org.junit.Assert.*
import org.junit.Test

class MvvSingleWalkingTripTest {
    // Reduced, anonymized structural fixture from real MVV EFA response 2026-11-16.
    private val walkingResponse = """
        {
          "parameters":[{"name":"serverID","value":"TEST"}],
          "origin":{"points":{"point":{"type":"coord"}}},
          "destination":{"points":{"point":{"type":"coord"}}},
          "trips":{"trip":{
            "distance":"197","duration":"00:02",
            "legs":[{
              "timeMinute":"2",
              "points":[
                {"name":"Start","dateTime":{"time":"07:47"}},
                {"name":"Ziel","dateTime":{"time":"07:49"}}
              ],
              "mode":{"product":"Fussweg","type":"100"}
            }]
          }}
        }
    """.trimIndent()

    @Test fun singleTripObjectIsParsedAsWalkingConnection() {
        val result = MvvJourneyRepository.parseTrips(walkingResponse, 3f)
        assertEquals(1, result.size)
        assertEquals("Nur zu Fuß", result.single().summary)
        assertEquals(0, result.single().changes)
        assertEquals("07:47", result.single().departure)
        assertEquals("07:49", result.single().arrival)
        assertTrue(result.single().steps.single().walking)
    }

    @Test fun multipleTripsArrayIsParsed() {
        val response = walkingResponse.replace(
            "\"trip\":{", "\"trip\":[{"
        ).replace("\"type\":\"100\"}\n            }]\n          }}", "\"type\":\"100\"}\n            }]\n          }]}")
        assertEquals(1, MvvJourneyRepository.parseTrips(response, 3f).size)
    }
}
