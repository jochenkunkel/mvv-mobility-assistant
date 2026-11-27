package de.mvv.mobility.data

/**
 * A deliberately coarse timetable estimate. It describes a leg phase, not a
 * fabricated GPS coordinate or a guarantee that a connection will be reached.
 */
data class TransitProgressEstimate(
    val line: String,
    val from: String,
    val to: String,
    val phase: String
) {
    val description: String
        get() = "$line · $phase · $from → $to (Fahrplan)"
}

object TransitProgressEstimator {
    fun estimate(step: JourneyStep, nowMillis: Long): TransitProgressEstimate? {
        if (step.walking || step.departure.isBlank() || step.arrival.isBlank() ||
            step.from.isBlank() || step.to.isBlank()) return null
        val start = step.departureInstantMillis ?: EfaTime.forNow(step.departure, nowMillis) ?: return null
        val end = step.arrivalInstantMillis ?: EfaTime.forNow(step.arrival, start) ?: return null
        if (end <= start || nowMillis !in start..end) return null
        val fraction = (nowMillis - start).toDouble() / (end - start).toDouble()
        val phase = when {
            fraction < 1.0 / 3.0 -> "früher Abschnitt"
            fraction < 2.0 / 3.0 -> "mittlerer Abschnitt"
            else -> "letzter Abschnitt"
        }
        return TransitProgressEstimate(step.line, step.from, step.to, phase)
    }
}

enum class RideGpsMode { LIVE, ESTIMATED, RECENT, STALE, UNAVAILABLE }

data class RideGpsStatus(val mode: RideGpsMode, val label: String) {
    val isGreen: Boolean get() = mode == RideGpsMode.LIVE || mode == RideGpsMode.ESTIMATED
}

object RideGpsStatusResolver {
    const val LIVE_AGE_MILLIS = 15_000L
    const val ESTIMATE_MAX_AGE_MILLIS = 60_000L
    const val RECENT_MAX_AGE_MILLIS = 45_000L
    private const val LIVE_MAX_ACCURACY_METERS = 35f
    private const val ESTIMATE_MAX_ACCURACY_METERS = 50f
    private const val RECENT_MAX_ACCURACY_METERS = 80f

    fun resolve(
        hasFix: Boolean,
        fixAgeMillis: Long?,
        accuracyMeters: Float?,
        boardingConfirmed: Boolean,
        progressEstimate: TransitProgressEstimate?,
        previousMode: RideGpsMode? = null
    ): RideGpsStatus {
        if (!hasFix) {
            return if (boardingConfirmed) RideGpsStatus(RideGpsMode.UNAVAILABLE, "Fahrt bestätigt · GPS fehlt")
            else RideGpsStatus(RideGpsMode.UNAVAILABLE, "GPS veraltet")
        }
        val age = fixAgeMillis
        if (age == null || age < 0L) return RideGpsStatus(RideGpsMode.STALE, "GPS-Zeit ungültig")
        val accuracy = accuracyMeters ?: Float.POSITIVE_INFINITY
        val liveAccuracyLimit = if (previousMode == RideGpsMode.LIVE) {
            LIVE_HYSTERESIS_ACCURACY_METERS
        } else LIVE_MAX_ACCURACY_METERS
        if (age <= LIVE_AGE_MILLIS && accuracy <= liveAccuracyLimit) {
            return RideGpsStatus(RideGpsMode.LIVE, "GPS LIVE")
        }
        val estimateAccuracyLimit = if (previousMode == RideGpsMode.ESTIMATED) {
            ESTIMATE_HYSTERESIS_ACCURACY_METERS
        } else ESTIMATE_MAX_ACCURACY_METERS
        if (boardingConfirmed && progressEstimate != null &&
            age <= ESTIMATE_MAX_AGE_MILLIS && accuracy <= estimateAccuracyLimit) {
            return RideGpsStatus(RideGpsMode.ESTIMATED, "Fahrt aktiv · Position geschätzt")
        }
        val recentAccuracyLimit = if (previousMode == RideGpsMode.RECENT) {
            RECENT_HYSTERESIS_ACCURACY_METERS
        } else RECENT_MAX_ACCURACY_METERS
        if (age <= RECENT_MAX_AGE_MILLIS && accuracy <= recentAccuracyLimit) {
            return RideGpsStatus(RideGpsMode.RECENT, "GPS ${age / 1000L}s · letzte Position")
        }
        return RideGpsStatus(
            RideGpsMode.STALE,
            if (boardingConfirmed) "Fahrt bestätigt · GPS veraltet" else "GPS veraltet"
        )
    }

    // Accuracy-only hysteresis prevents status flicker around quality thresholds.
    // Age limits remain hard caps so a stale fix or estimate cannot be prolonged.
    private const val LIVE_HYSTERESIS_ACCURACY_METERS = 45f
    private const val ESTIMATE_HYSTERESIS_ACCURACY_METERS = 60f
    private const val RECENT_HYSTERESIS_ACCURACY_METERS = 90f
}
