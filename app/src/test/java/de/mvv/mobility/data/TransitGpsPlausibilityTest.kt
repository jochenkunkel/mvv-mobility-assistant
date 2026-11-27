package de.mvv.mobility.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitGpsPlausibilityTest {
    @Test fun ordinaryTransitProgressAcrossGpsGapIsAccepted() {
        val beforeGap = fix(latitude = 48.1394, longitude = 11.5615, timeMillis = 10_000L)
        val afterGap = fix(latitude = 48.1466, longitude = 11.5615, timeMillis = 70_000L)

        assertTrue(TransitGpsPlausibility.isPlausible(beforeGap, afterGap))
    }

    @Test fun teleportingFixIsRejectedAndDoesNotPreventPlausibleRecovery() {
        val lastGood = fix(latitude = 48.1394, longitude = 11.5615, timeMillis = 10_000L)
        val teleport = fix(latitude = 48.1900, longitude = 11.5615, timeMillis = 15_000L)
        val recovered = fix(latitude = 48.1466, longitude = 11.5615, timeMillis = 70_000L)

        assertFalse(TransitGpsPlausibility.isPlausible(lastGood, teleport))
        // The caller retains lastGood on rejection; a later, plausible fix is measured from it.
        assertTrue(TransitGpsPlausibility.isPlausible(lastGood, recovered))
    }

    @Test fun onlyAppliesTransitJumpGuardAfterBoardingConfirmation() {
        val previous = fix(latitude = 48.1394, longitude = 11.5615, timeMillis = 10_000L)
        val teleport = fix(latitude = 48.1900, longitude = 11.5615, timeMillis = 15_000L)

        assertFalse(TransitGpsPlausibility.shouldReject(previous, teleport, boardingConfirmed = false))
        assertTrue(TransitGpsPlausibility.shouldReject(previous, teleport, boardingConfirmed = true))
    }

    @Test fun accuracyProvidesBoundedMarginButCannotHideLargeJump() {
        val accurate = fix(latitude = 48.1394, longitude = 11.5615, timeMillis = 1_000L, accuracy = 5f)
        val noisyNext = fix(latitude = 48.1399, longitude = 11.5615, timeMillis = 2_000L, accuracy = 35f)
        val impossible = fix(latitude = 48.1800, longitude = 11.5615, timeMillis = 2_000L, accuracy = 100f)

        assertTrue(TransitGpsPlausibility.isPlausible(accurate, noisyNext))
        assertFalse(TransitGpsPlausibility.isPlausible(accurate, impossible))
    }

    @Test fun rejectsInvalidCoordinatesAndNonIncreasingOrMixedClockBases() {
        val baseline = fix(latitude = 48.1394, longitude = 11.5615, timeMillis = 10_000L)
        assertFalse(TransitGpsPlausibility.isPlausible(baseline, baseline.copy(latitude = 91.0)))
        assertFalse(TransitGpsPlausibility.isPlausible(baseline, baseline.copy(wallTimeMillis = 9_999L)))

        val monotonic = baseline.copy(elapsedRealtimeNanos = 1_000_000_000L)
        assertFalse(
            TransitGpsPlausibility.isPlausible(
                monotonic,
                baseline.copy(wallTimeMillis = 11_000L, elapsedRealtimeNanos = 0L)
            )
        )
    }

    @Test fun monotonicTimeTakesPrecedenceOverWallClockChanges() {
        val first = fix(
            latitude = 48.1394,
            longitude = 11.5615,
            timeMillis = 100_000L,
            elapsedRealtimeNanos = 10_000_000_000L
        )
        val next = fix(
            latitude = 48.1400,
            longitude = 11.5615,
            timeMillis = 1_000L, // Wall clock moved backwards.
            elapsedRealtimeNanos = 20_000_000_000L
        )

        assertTrue(TransitGpsPlausibility.isPlausible(first, next))
    }

    private fun fix(
        latitude: Double,
        longitude: Double,
        timeMillis: Long,
        accuracy: Float = 10f,
        elapsedRealtimeNanos: Long = 0L
    ) = TransitGpsFix(latitude, longitude, timeMillis, elapsedRealtimeNanos, accuracy)
}
