package de.mvv.mobility.data

import de.mvv.mobility.domain.Journey

/** Phase 1: no real calendar, GPS, or transit API data is used. */
object DemoJourneyRepository {
    val nextJourney = Journey(
        appointment = "Demo: Physiotherapie",
        destination = null,
        minutesUntilDeparture = 18,
        remainingMinutes = 35,
        transfers = 1,
        walkingMinutes = 8
    )
}
