package de.mvv.mobility.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A location sample used only to reject impossible jumps during a confirmed transit leg. */
data class TransitGpsFix(
    val latitude: Double,
    val longitude: Double,
    val wallTimeMillis: Long,
    val elapsedRealtimeNanos: Long,
    val accuracyMeters: Float?
)

object TransitGpsPlausibility {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val MAX_TRANSIT_SPEED_METERS_PER_SECOND = 45.0
    private const val DEFAULT_ACCURACY_METERS = 25.0
    private const val MAX_ACCURACY_MARGIN_METERS = 100.0

    fun shouldReject(
        previous: TransitGpsFix,
        candidate: TransitGpsFix,
        boardingConfirmed: Boolean
    ): Boolean = boardingConfirmed && !isPlausible(previous, candidate)

    /**
     * Rejects a fix only when it would require implausibly fast transit movement.
     * Uses reported accuracy as a margin and never modifies or fabricates a position.
     */
    fun isPlausible(previous: TransitGpsFix, candidate: TransitGpsFix): Boolean {
        if (!validCoordinate(previous.latitude, previous.longitude) ||
            !validCoordinate(candidate.latitude, candidate.longitude)) return false

        val previousHasMonotonicTime = previous.elapsedRealtimeNanos > 0L
        val candidateHasMonotonicTime = candidate.elapsedRealtimeNanos > 0L
        if (previousHasMonotonicTime != candidateHasMonotonicTime) return false

        val elapsedSeconds = if (previousHasMonotonicTime) {
            (candidate.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
        } else {
            (candidate.wallTimeMillis - previous.wallTimeMillis) / 1000.0
        }
        if (!elapsedSeconds.isFinite() || elapsedSeconds <= 0.0) return false

        val allowedMeters = MAX_TRANSIT_SPEED_METERS_PER_SECOND * elapsedSeconds +
            accuracyMargin(previous.accuracyMeters) + accuracyMargin(candidate.accuracyMeters)
        return distanceMeters(previous, candidate) <= allowedMeters
    }

    private fun validCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun accuracyMargin(accuracyMeters: Float?): Double =
        accuracyMeters?.takeIf { it.isFinite() && it >= 0f }
            ?.toDouble()
            ?.coerceAtMost(MAX_ACCURACY_MARGIN_METERS)
            ?: DEFAULT_ACCURACY_METERS

    private fun distanceMeters(a: TransitGpsFix, b: TransitGpsFix): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val haversine = sin(deltaLat / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }
}
