package de.mvv.mobility.data

import kotlin.math.*

/**
 * Projects a position onto a walking route polyline, not onto the nearest vertex.
 * Coordinates are (latitude, longitude). Distances are approximate metres on a local
 * equirectangular plane; this is appropriate for short urban walking routes.
 *
 * This is stateless geometry. The caller must separately handle GPS accuracy,
 * off-route detection and monotonic progress across successive location fixes.
 */
internal object RouteProgress {
    data class Projection(val alongMeters: Double, val lateralMeters: Double, val segmentIndex: Int)

    fun project(path: List<Pair<Double, Double>>, latitude: Double, longitude: Double): Projection? {
        if (path.size < 2 || !latitude.isFinite() || !longitude.isFinite()) return null
        val cosLatitude = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
        fun xy(point: Pair<Double, Double>): Pair<Double, Double> =
            (point.second - longitude) * 111_320.0 * cosLatitude to
                (point.first - latitude) * 111_320.0

        var accumulated = 0.0
        var best: Projection? = null
        for (index in 0 until path.lastIndex) {
            val (ax, ay) = xy(path[index])
            val (bx, by) = xy(path[index + 1])
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            if (lengthSquared < 0.0001) continue
            val fraction = (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val px = ax + fraction * dx
            val py = ay + fraction * dy
            val lateral = hypot(px, py)
            val length = sqrt(lengthSquared)
            val candidate = Projection(accumulated + fraction * length, lateral, index)
            if (best == null || candidate.lateralMeters < best.lateralMeters) best = candidate
            accumulated += length
        }
        return best
    }

    /** Route tangent at the closest projected segment, in compass degrees. */
    fun heading(path: List<Pair<Double, Double>>, latitude: Double, longitude: Double,
        maxOffRouteMeters: Double = 45.0): Float? {
        val projection = project(path, latitude, longitude) ?: return null
        if (projection.lateralMeters > maxOffRouteMeters) return null
        val start = path[projection.segmentIndex]
        val end = path[projection.segmentIndex + 1]
        val north = (end.first - start.first) * 111_320.0
        val east = (end.second - start.second) * 111_320.0 *
            cos(Math.toRadians(latitude))
        if (hypot(north, east) < 0.01) return null
        return ((Math.toDegrees(atan2(east, north)) + 360.0) % 360.0).toFloat()
    }

    /** Returns the next route-ordered maneuver, or null if geometry cannot be trusted. */
    fun nextTurn(
        route: WalkingRoute,
        latitude: Double,
        longitude: Double,
        maxOffRouteMeters: Double = 45.0
    ): Pair<WalkingTurn, Int>? {
        val here = project(route.path, latitude, longitude) ?: return null
        if (here.lateralMeters > maxOffRouteMeters) return null
        // Depart maneuvers at the beginning of the route are informational.
        // Do not skip a genuine upcoming turn simply because it is <25m away.
        val candidates = route.turns.mapIndexedNotNull { index, turn ->
            val projected = project(route.path, turn.latitude, turn.longitude) ?: return@mapIndexedNotNull null
            if (projected.lateralMeters > 35.0) return@mapIndexedNotNull null
            Triple(index, turn, projected.alongMeters)
        }.sortedWith(compareBy<Triple<Int, WalkingTurn, Double>> { it.third }.thenBy { it.first })
        val next = candidates.firstOrNull { (index, turn, along) ->
            val isDeparture = index == 0 && turn.instruction == "Geradeaus" && along < 15.0
            !isDeparture && along >= here.alongMeters - 8.0
        } ?: return null
        return next.second to (next.third - here.alongMeters).coerceAtLeast(0.0).roundToInt()
    }
}
