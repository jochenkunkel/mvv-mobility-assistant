package de.mvv.mobility.data

/** GPS fixes older than two minutes must not be used for route searches. */
object LocationFreshness {
    const val MAX_AGE_MILLIS = 120_000L

    fun elapsedAgeMillis(fixElapsedNanos: Long, nowElapsedNanos: Long): Long? =
        if (fixElapsedNanos <= 0L || nowElapsedNanos < fixElapsedNanos) null
        else (nowElapsedNanos - fixElapsedNanos) / 1_000_000L

    fun wallAgeMillis(fixTimeMillis: Long, nowMillis: Long): Long? =
        if (fixTimeMillis <= 0L || nowMillis < fixTimeMillis) null
        else nowMillis - fixTimeMillis

    /**
     * Monotonic age check: unlike wall-clock timestamps this survives manual
     * clock changes and network time corrections.
     */
    fun isFreshElapsed(fixElapsedNanos: Long, nowElapsedNanos: Long,
        maxAgeMillis: Long = MAX_AGE_MILLIS): Boolean {
        return maxAgeMillis >= 0L && elapsedAgeMillis(fixElapsedNanos, nowElapsedNanos)?.let { it <= maxAgeMillis } == true
    }

    fun isFresh(fixTimeMillis: Long, nowMillis: Long): Boolean =
        fixTimeMillis > 0L && fixTimeMillis <= nowMillis &&
            nowMillis - fixTimeMillis <= MAX_AGE_MILLIS
}
