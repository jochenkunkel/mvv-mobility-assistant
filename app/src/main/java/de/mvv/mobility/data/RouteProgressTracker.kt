package de.mvv.mobility.data

import kotlin.math.abs
import kotlin.math.max

/**
 * Keeps GPS projections on a plausible branch of the active route. Stateless
 * nearest-segment projection can jump to a nearby parallel leg of a hairpin.
 */
internal class RouteProgressTracker {
    private var routeKey: Any? = null
    private var previous: RouteProgress.Projection? = null
    private var previousClock: Long? = null
    private var previousUsesMonotonicClock: Boolean? = null
    private var previousAccuracyMeters = 0f

    fun project(
        route: Any,
        path: List<Pair<Double, Double>>,
        latitude: Double,
        longitude: Double,
        timestampMillis: Long,
        elapsedRealtimeNanos: Long,
        accuracyMeters: Float?
    ): RouteProgress.Projection? {
        if (routeKey !== route) reset(route)
        val monotonic = elapsedRealtimeNanos > 0L
        val clock = if (monotonic) elapsedRealtimeNanos else timestampMillis
        val lastClock = previousClock
        val lastMonotonic = previousUsesMonotonicClock
        if (lastClock != null) {
            if (lastMonotonic != monotonic || clock <= lastClock) return previous
        }

        val candidates = RouteProgress.candidates(path, latitude, longitude)
            .filter { it.lateralMeters <= MAX_OFF_ROUTE_METERS }
        if (candidates.isEmpty()) return null
        val best = candidates.minByOrNull { it.lateralMeters } ?: return null
        val last = previous
        val selected = if (last == null || lastClock == null) {
            best
        } else {
            val seconds = if (monotonic) {
                (clock - lastClock) / 1_000_000_000.0
            } else {
                (clock - lastClock) / 1000.0
            }.coerceAtLeast(0.0)
            val accuracyAllowance = (accuracyMeters ?: DEFAULT_ACCURACY_METERS) +
                previousAccuracyMeters
            val plausibleDelta = max(MIN_PROGRESS_DELTA_METERS,
                seconds * MAX_WALKING_SPEED_METERS_PER_SECOND + accuracyAllowance)
            candidates
                .filter { abs(it.alongMeters - last.alongMeters) <= plausibleDelta }
                .minByOrNull { it.lateralMeters }
                ?: return previous
        }

        previous = selected
        previousClock = clock
        previousUsesMonotonicClock = monotonic
        previousAccuracyMeters = accuracyMeters ?: DEFAULT_ACCURACY_METERS
        return selected
    }

    fun clear() {
        routeKey = null
        previous = null
        previousClock = null
        previousUsesMonotonicClock = null
        previousAccuracyMeters = 0f
    }

    private fun reset(route: Any) {
        clear()
        routeKey = route
    }

    private companion object {
        const val MAX_OFF_ROUTE_METERS = 35.0
        const val MIN_PROGRESS_DELTA_METERS = 12.0
        const val MAX_WALKING_SPEED_METERS_PER_SECOND = 5.0
        const val DEFAULT_ACCURACY_METERS = 15f
    }
}
