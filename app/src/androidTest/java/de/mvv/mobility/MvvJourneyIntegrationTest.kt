package de.mvv.mobility

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.mvv.mobility.data.MvvJourneyRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MvvJourneyIntegrationTest {
    private val sample = """
        {"trips":[{"legs":[
          {"origin":{"dateTime":{"hour":"09","minute":"10"}},"destination":{"dateTime":{"hour":"09","minute":"15"}},"distance":300},
          {"transportation":{"number":"U3","product":{"name":"U-Bahn"}},"origin":{"dateTime":{"hour":"09","minute":"15"}},"destination":{"dateTime":{"hour":"09","minute":"30"}}},
          {"transportation":{"number":"Bus 54","product":{"name":"Bus"}},"origin":{"dateTime":{"hour":"09","minute":"35"}},"destination":{"dateTime":{"hour":"09","minute":"50"}}},
          {"origin":{"dateTime":{"hour":"09","minute":"50"}},"destination":{"dateTime":{"hour":"09","minute":"55"}},"distance":150}
        ]}]}
    """.trimIndent()

    @Test fun parsesLineChangesAndTimes() {
        val trip = MvvJourneyRepository.parseTrips(sample, 3f).single()
        assertEquals("09:10", trip.departure)
        assertEquals("09:55", trip.arrival)
        assertEquals("U3 → Bus 54", trip.summary)
        assertEquals(1, trip.changes)
        assertEquals(9, trip.walkingMinutes)
    }

    @Test fun slowerWalkingRequiresMoreTime() {
        val slow = MvvJourneyRepository.parseTrips(sample, 2f).single()
        val fast = MvvJourneyRepository.parseTrips(sample, 5f).single()
        assertTrue(slow.walkingMinutes > fast.walkingMinutes)
    }

    @Test fun emptyAndMalformedResponseHandled() {
        assertTrue(MvvJourneyRepository.parseTrips("""{"trips":[]}""", 3f).isEmpty())
        assertThrows(IllegalStateException::class.java) {
            MvvJourneyRepository.parseTrips("""{}""", 3f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MvvJourneyRepository.parseTrips(sample, 0f)
        }
    }
    @Test fun ambiguousDestinationRequiresSelection() {
        val json = """{"destination":{"points":[
          {"stateless":"91000306","name":"München, Olympiapark West","anyType":"stop","quality":"310"},
          {"stateless":"91000325","name":"München, Olympiapark Ost","anyType":"stop","quality":"275"}
        ]}}"""
        val stops = MvvJourneyRepository.candidates(json)
        assertEquals(2, stops.size)
        assertNull(MvvJourneyRepository.selectCandidate("Olympiapark München", stops))
        assertEquals("91000306", MvvJourneyRepository.selectCandidate("Olympiapark West", stops)?.id)
        assertEquals("91000325", MvvJourneyRepository.selectCandidate("Olympiapark Ost", stops)?.id)
    }

}
