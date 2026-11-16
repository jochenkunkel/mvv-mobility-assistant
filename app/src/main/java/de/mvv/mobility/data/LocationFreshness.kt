package de.mvv.mobility.data

/** GPS fixes older than two minutes must not be used for route searches. */
object LocationFreshness {
    const val MAX_AGE_MILLIS = 120_000L

    /**
     * Monotonic age check: unlike wall-clock timestamps this survives manual
     * clock changes and network time corrections.
     */
    fun isFreshElapsed(fixElapsedNanos: Long, nowElapsedNanos: Long,
        maxAgeMillis: Long = MAX_AGE_MILLIS): Boolean {
        if (fixElapsedNanos <= 0L || nowElapsedNanos < fixElapsedNanos || maxAgeMillis < 0L) return false
        return (nowElapsedNanos - fixElapsedNanos) / 1_000_000L <= maxAgeMillis
    }

    fun isFresh(fixTimeMillis: Long, nowMillis: Long): Boolean =
        fixTimeMillis > 0L && fixTimeMillis <= nowMillis &&
            nowMillis - fixTimeMillis <= MAX_AGE_MILLIS
}
