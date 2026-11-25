package de.mvv.mobility.data

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

    fun accept(fix: TraceFix, maxAccuracyMeters: Float = 35f, gapMillis: Long = 120_000L): WalkedTrace {
        if (!fix.latitude.isFinite() || !fix.longitude.isFinite() ||
            fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0 ||
            !fix.accuracyMeters.isFinite() || fix.accuracyMeters < 0f ||
            fix.accuracyMeters > maxAccuracyMeters || fix.timestampMillis < 0L) return this

        val previous = segments.lastOrNull()?.lastOrNull()
        if (previous != null && fix.timestampMillis <= previous.timestampMillis) return this
        if (previous == null) return WalkedTrace(listOf(listOf(fix)))

        // Avoid drawing a fictitious line through a GPS outage.
        return if (fix.timestampMillis - previous.timestampMillis > gapMillis) {
            WalkedTrace(segments + listOf(listOf(fix)))
        } else {
            WalkedTrace(segments.dropLast(1) + listOf(segments.last() + fix))
        }
    }
}
