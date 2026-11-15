package de.mvv.mobility

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.RouteFilterMode
import de.mvv.mobility.data.RoutePreference
import de.mvv.mobility.data.filterJourneys
import de.mvv.mobility.data.selectAppointmentJourneys
import de.mvv.mobility.data.JourneyOption
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

    @Test fun parsesRealEfaModePointsAndTimes() {
        val response = """{"trips":[{"legs":[
          {"timeMinute":"6","mode":{"product":"Fussweg","number":""},"points":[
            {"dateTime":{"time":"10:00"}},{"dateTime":{"time":"10:06"}}]},
          {"timeMinute":"12","mode":{"product":"U-Bahn","number":"U3"},"points":[
            {"dateTime":{"time":"10:08","rtTime":"10:09"}},
            {"dateTime":{"time":"10:20","rtTime":"10:21"}}]},
          {"timeMinute":"4","mode":{"product":"Fussweg","number":""},"points":[
            {"dateTime":{"time":"10:21"}},{"dateTime":{"time":"10:25"}}]}
        ]}]}"""
        val option = MvvJourneyRepository.parseTrips(response, 3.5f).single()
        assertEquals("10:00", option.departure)
        assertEquals("10:25", option.arrival)
        assertEquals("U3", option.summary)
        assertEquals(10, option.walkingMinutes)
        assertEquals(0, option.changes)
    }

    @Test fun routePreferenceIncludesOrExcludesTransitLines() {
        val json = """{"trips":[
          {"legs":[{"mode":{"product":"Fussweg"},"timeMinute":"3"},
                   {"mode":{"product":"Bus","number":"166"},"points":[{"dateTime":{"time":"10:00"}},{"dateTime":{"time":"10:10"}}]},
                   {"mode":{"product":"Fussweg"},"timeMinute":"2"}]},
          {"legs":[{"mode":{"product":"Bus","number":"166"},"points":[{"dateTime":{"time":"10:00"}},{"dateTime":{"time":"10:10"}}]},
                   {"mode":{"product":"U-Bahn","number":"U3"},"points":[{"dateTime":{"time":"10:12"}},{"dateTime":{"time":"10:20"}}]}]},
          {"legs":[{"mode":{"product":"U-Bahn","number":"U6"},"points":[{"dateTime":{"time":"10:00"}},{"dateTime":{"time":"10:10"}}]}]}
        ]}"""
        val trips = MvvJourneyRepository.parseTrips(json, 3f)
        assertEquals(3, trips.size)
        assertEquals(2, filterJourneys(trips, RoutePreference(RouteFilterMode.CONTAINS, "166")).size)
        assertEquals(1, filterJourneys(trips, RoutePreference(RouteFilterMode.ONLY_LINE_WITH_WALK, "166")).size)
        assertEquals(2, filterJourneys(trips, RoutePreference(RouteFilterMode.CONTAINS, "U-Bahn")).size)
        assertTrue(filterJourneys(trips, RoutePreference(RouteFilterMode.ONLY_LINE_WITH_WALK, "U3")).isEmpty())
        assertEquals(3, filterJourneys(trips, RoutePreference()).size)
    }

    @Test fun boardingStopAndWalkingStepsAreParsed() {
        val json = """{"trips":[{"legs":[
          {"mode":{"product":"Fussweg"},"timeMinute":"7","points":[{"name":"Start","dateTime":{"time":"07:00"}},{"name":"Haltestelle A","dateTime":{"time":"07:07"}}]},
          {"mode":{"product":"Bus","number":"134"},"points":[{"name":"Haltestelle A","dateTime":{"time":"07:10"}},{"name":"Haltestelle B","dateTime":{"time":"07:25"}}]},
          {"mode":{"product":"Fussweg"},"timeMinute":"5","points":[{"name":"Haltestelle B","dateTime":{"time":"07:25"}},{"name":"Ziel","dateTime":{"time":"07:30"}}]}
        ]}]}"""
        val trip = MvvJourneyRepository.parseTrips(json, 3f).single()
        assertEquals(3, trip.steps.size)
        assertTrue(trip.steps.first().walking)
        assertEquals("134", trip.steps[1].line)
        assertEquals("Haltestelle A", trip.steps[1].from)
        assertEquals("Haltestelle B", trip.steps[1].to)
        assertEquals("07:10", trip.steps[1].departure)
        assertTrue(trip.steps.last().walking)
    }

    @Test fun appointmentShowsOnlyThreeClosestArrivalsNotNightBuses() {
        val zone = java.util.TimeZone.getTimeZone("Europe/Berlin")
        val target = java.util.Calendar.getInstance(zone).apply {
            set(2026, java.util.Calendar.NOVEMBER, 16, 7, 50, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val arrivals = listOf("01:10", "01:30", "06:50", "07:10", "07:30", "07:50", "08:10")
        val journeys = arrivals.map { clock ->
            JourneyOption("06:00", clock, "134", 0, 10)
        }
        val selected = selectAppointmentJourneys(journeys, target)
        assertEquals(listOf("07:50", "07:30", "07:10"), selected.map { it.arrival })
        assertTrue(selectAppointmentJourneys(listOf(journeys.first()), target).isEmpty())
    }

}
