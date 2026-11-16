package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReachableNowJourneysTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")
    private fun at(hour: Int, minute: Int): Long =
        Calendar.getInstance(zone).apply {
            set(2026, Calendar.NOVEMBER, 16, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun journey(departure: String, busDeparture: String) =
        JourneyOption(departure, "14:50", "56", 0, 9, steps = listOf(
            JourneyStep("", "Fußweg", "GPS", "Haltestelle", departure, busDeparture, true, 9),
            JourneyStep("56", "Bus", "Haltestelle", "Ziel", busDeparture, "14:50", false, 10)
        ))

    @Test fun rejectsAlreadyStartedWalkEvenIfBusHasNotLeft() {
        val options = listOf(journey("13:44", "13:53"), journey("14:00", "14:09"))
        assertEquals(listOf("14:00"), reachableNowJourneys(options, at(13, 52)).map { it.departure })
    }

    @Test fun requiresOneMinuteToPrepare() {
        assertEquals(emptyList<JourneyOption>(),
            reachableNowJourneys(listOf(journey("13:52", "14:01")), at(13, 52)))
        assertEquals(1, reachableNowJourneys(listOf(journey("13:53", "14:02")), at(13, 52)).size)
    }

    @Test fun rejectsMissingFirstDepartureRatherThanGuessing() {
        assertEquals(emptyList<JourneyOption>(),
            reachableNowJourneys(listOf(journey("–", "14:05")), at(13, 52)))
    }
}
