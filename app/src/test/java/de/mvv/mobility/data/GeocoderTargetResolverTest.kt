package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocoderTargetResolverTest {
    @Test fun nearbyGeocoderHitsAreNotAmbiguous() {
        // Same stop with slightly different address points must still navigate.
        val matches = listOf(
            StopPosition(48.13715, 11.57612),
            StopPosition(48.13720, 11.57620),
            StopPosition(48.13710, 11.57605)
        )
        assertEquals(matches.first(), GeocoderTargetResolver.pick(matches))
    }

    @Test fun distantSameNameHitsFailClosed() {
        val matches = listOf(
            StopPosition(48.13715, 11.57612),
            StopPosition(48.22715, 11.67612)
        )
        assertNull(GeocoderTargetResolver.pick(matches))
    }

    @Test fun emptyAndInvalidInputsFailClosed() {
        assertNull(GeocoderTargetResolver.pick(emptyList()))
        assertNull(GeocoderTargetResolver.pick(
            listOf(StopPosition(48.1, 11.5)), maxSpreadMeters = -1f))
    }
}
