package de.mvv.mobility.data

/** GPS fixes older than two minutes must not be used for route searches. */
object LocationFreshness {
    const val MAX_AGE_MILLIS = 120_000L

    fun isFresh(fixTimeMillis: Long, nowMillis: Long): Boolean =
        fixTimeMillis > 0L && fixTimeMillis <= nowMillis &&
            nowMillis - fixTimeMillis <= MAX_AGE_MILLIS
}
