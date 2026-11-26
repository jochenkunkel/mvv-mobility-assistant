package de.mvv.mobility.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fail-closed selection for name-only geocoding fallbacks.
 *
 * Several geocoder hits for the same stop are normal (platform entrances,
 * slightly different address points). Only genuinely distant hits are treated
 * as ambiguous. Exact EFA stop coordinates always take priority before this
 * resolver is called, so the selected boarding point is never re-geocoded.
 */
object GeocoderTargetResolver {
    const val DEFAULT_MAX_SPREAD_METERS = 75f

    fun pick(matches: List<StopPosition>, maxSpreadMeters: Float = DEFAULT_MAX_SPREAD_METERS): StopPosition? {
        val first = matches.firstOrNull() ?: return null
        if (maxSpreadMeters < 0f || !maxSpreadMeters.isFinite()) return null
        if (matches.drop(1).any { distanceMeters(first, it) > maxSpreadMeters }) return null
        return first
    }

    private fun distanceMeters(a: StopPosition, b: StopPosition): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2.0 * 6_371_000.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
