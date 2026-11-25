package de.mvv.mobility.data

import kotlin.math.*

/**
 * Immutable accepted GPS trace, separate from the planned OSRM polyline.
 * A segment boundary is recorded when the interval between accepted fixes is large.
 * This model does not start tracking or persist location data.
 */
data class TraceFix(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float
)

data class WalkedTrace(
    val segments: List<List<TraceFix>> = emptyList()
) {
    val fixes: List<TraceFix> get() = segments.flatten()

    /**
     * Retain at most [maxFixes] recent accepted fixes without joining GPS gaps.
     * Useful for the navigation session's bounded in-memory history.
     */
    fun retainLast(maxFixes: Int): WalkedTrace {
        require(maxFixes >= 0) { "maxFixes must not be negative" }
        val count = segments.sumOf { it.size }
        if (count <= maxFixes) return this
        var toDrop = count - maxFixes
        val retained = buildList {
            for (segment in segments) {
                if (toDrop >= segment.size) {
                    toDrop -= segment.size
                    continue
                }
                val remaining = segment.drop(toDrop)
                toDrop = 0
                if (remaining.isNotEmpty()) add(remaining)
            }
        }
        return WalkedTrace(retained)
    }

    fun accept(
        fix: TraceFix,
        maxAccuracyMeters: Float = 35f,
        gapMillis: Long = 120_000L,
        maxSpeedMetersPerSecond: Double = 15.0
    ): WalkedTrace {
        if (!fix.latitude.isFinite() || !fix.longitude.isFinite() ||
            fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0 ||
            !fix.accuracyMeters.isFinite() || fix.accuracyMeters < 0f ||
            !maxAccuracyMeters.isFinite() || maxAccuracyMeters < 0f ||
            fix.accuracyMeters > maxAccuracyMeters || fix.timestampMillis < 0L ||
            !maxSpeedMetersPerSecond.isFinite() || maxSpeedMetersPerSecond <= 0.0 ||
            gapMillis < 0L) return this

        val previous = segments.lastOrNull()?.lastOrNull()
        if (previous != null && fix.timestampMillis <= previous.timestampMillis) return this
        if (previous == null) return WalkedTrace(listOf(listOf(fix)))

        val elapsedMillis = fix.timestampMillis - previous.timestampMillis
        // A long outage starts a new segment; never interpolate across the gap.
        if (elapsedMillis > gapMillis) return WalkedTrace(segments + listOf(listOf(fix)))

        // Reject impossible jumps without deleting accepted history or drawing teleport lines.
        val lat1 = Math.toRadians(previous.latitude)
        val lat2 = Math.toRadians(fix.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(fix.longitude - previous.longitude)
        val a = sin(dLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        val meters = 2.0 * 6_371_000.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        if (meters / (elapsedMillis / 1000.0) > maxSpeedMetersPerSecond) return this

        return WalkedTrace(segments.dropLast(1) + listOf(segments.last() + fix))
    }
}
