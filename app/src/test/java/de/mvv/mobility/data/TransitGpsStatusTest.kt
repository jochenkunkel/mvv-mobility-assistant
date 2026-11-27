package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransitGpsStatusTest {
    private val start = EfaTime.resolve(LocalDate.of(2026, 11, 27), 11, 0)
    private val bus = JourneyStep(
        line = "134",
        product = "Bus",
        from = "Fürstenried West",
        to = "Bleibtreustraße",
        departure = "11:00",
        arrival = "11:30",
        walking = false,
        minutes = 30
    )

    @Test fun estimatesOnlyCoarseScheduledPhaseDuringActiveVehicleLeg() {
        val estimate = assertNotNullEstimate(bus, start + 15 * 60_000L)
        assertEquals("mittlerer Abschnitt", estimate.phase)
        assertEquals("Fürstenried West", estimate.from)
        assertEquals("Bleibtreustraße", estimate.to)
        assertTrue(estimate.description.contains("(Fahrplan)"))
        assertNull(TransitProgressEstimator.estimate(bus, start - 60_000L))
        assertNull(TransitProgressEstimator.estimate(bus.copy(walking = true), start + 60_000L))
    }

    @Test fun handlesTripAcrossMidnightWithoutInventingExactCoordinates() {
        val lateStart = EfaTime.resolve(LocalDate.of(2026, 11, 27), 23, 50)
        val lateBus = bus.copy(
            departure = "23:50",
            arrival = "00:10",
            departureInstantMillis = lateStart,
            arrivalInstantMillis = lateStart + 20 * 60_000L
        )
        val estimate = assertNotNullEstimate(lateBus, lateStart + 10 * 60_000L)
        assertEquals("mittlerer Abschnitt", estimate.phase)
    }

    @Test fun onlyFreshAccurateFixIsCalledLive() {
        val status = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 10_000L, accuracyMeters = 5f,
            boardingConfirmed = false, progressEstimate = null
        )
        assertEquals(RideGpsMode.LIVE, status.mode)
        assertEquals("GPS LIVE", status.label)
        assertTrue(status.isGreen)
    }

    @Test fun shortGapDuringConfirmedScheduledRideIsExplicitlyEstimated() {
        val estimate = assertNotNullEstimate(bus, start + 15 * 60_000L)
        val status = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 30_000L, accuracyMeters = 12f,
            boardingConfirmed = true, progressEstimate = estimate
        )
        assertEquals(RideGpsMode.ESTIMATED, status.mode)
        assertEquals("Fahrt aktiv · Position geschätzt", status.label)
        assertTrue(status.isGreen)
        assertFalse(status.label.contains("GPS LIVE"))
    }

    @Test fun noEstimateOrNoBoardingNeverGetsReassuringGreenState() {
        val noSchedule = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 30_000L, accuracyMeters = 12f,
            boardingConfirmed = true, progressEstimate = null
        )
        val notBoarded = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 30_000L, accuracyMeters = 12f,
            boardingConfirmed = false, progressEstimate = assertNotNullEstimate(bus, start + 15 * 60_000L)
        )
        assertEquals(RideGpsMode.RECENT, noSchedule.mode)
        assertEquals(RideGpsMode.RECENT, notBoarded.mode)
        assertFalse(noSchedule.isGreen)
        assertFalse(notBoarded.isGreen)
    }

    @Test fun estimateExpiresAtSixtySecondsAndCannotHideMissingGps() {
        val estimate = assertNotNullEstimate(bus, start + 15 * 60_000L)
        val expired = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 60_001L, accuracyMeters = 12f,
            boardingConfirmed = true, progressEstimate = estimate
        )
        val missing = RideGpsStatusResolver.resolve(
            hasFix = false, fixAgeMillis = null, accuracyMeters = null,
            boardingConfirmed = true, progressEstimate = estimate
        )
        assertEquals(RideGpsMode.STALE, expired.mode)
        assertEquals(RideGpsMode.UNAVAILABLE, missing.mode)
        assertFalse(expired.isGreen)
        assertFalse(missing.isGreen)
    }

    @Test fun accuracyThresholdsUseBoundedHysteresisWithoutExtendingAgeCaps() {
        val estimate = assertNotNullEstimate(bus, start + 15 * 60_000L)
        val establishedLive = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 10_000L, accuracyMeters = 40f,
            boardingConfirmed = false, progressEstimate = null,
            previousMode = RideGpsMode.LIVE
        )
        val newlyObservedLive = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 10_000L, accuracyMeters = 40f,
            boardingConfirmed = false, progressEstimate = null
        )
        val establishedEstimate = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 30_000L, accuracyMeters = 55f,
            boardingConfirmed = true, progressEstimate = estimate,
            previousMode = RideGpsMode.ESTIMATED
        )
        val expiredEstimate = RideGpsStatusResolver.resolve(
            hasFix = true, fixAgeMillis = 60_001L, accuracyMeters = 55f,
            boardingConfirmed = true, progressEstimate = estimate,
            previousMode = RideGpsMode.ESTIMATED
        )
        assertEquals(RideGpsMode.LIVE, establishedLive.mode)
        assertEquals(RideGpsMode.RECENT, newlyObservedLive.mode)
        assertEquals(RideGpsMode.ESTIMATED, establishedEstimate.mode)
        assertEquals(RideGpsMode.STALE, expiredEstimate.mode)
    }

    private fun assertNotNullEstimate(step: JourneyStep, now: Long): TransitProgressEstimate {
        val estimate = TransitProgressEstimator.estimate(step, now)
        assertNotNull(estimate)
        return estimate!!
    }
}
