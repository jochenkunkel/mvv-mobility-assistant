package de.mvv.mobility.data

import java.time.ZoneId

/**
 * Absolute journey instants.
 *
 * EFA delivers a full date when available; older/fixture payloads only carry
 * HH:mm. In that case the clock is resolved against the *selected search day*,
 * never against "today" — otherwise a Saturday appointment searched on
 * Thursday is displayed as an already-departed Thursday connection.
 */
object JourneyTime {
    fun departureMillis(
        option: JourneyOption,
        referenceMillis: Long,
        zone: ZoneId = EfaTime.BERLIN
    ): Long? {
        option.departureInstantMillis?.let { return it }
        val clock = option.steps.firstOrNull { !it.walking }?.departure
            ?.takeIf { it.isNotBlank() }
            ?: option.departure
        return EfaTime.forNow(clock, referenceMillis, zone)
    }

    fun arrivalMillis(
        option: JourneyOption,
        referenceMillis: Long,
        zone: ZoneId = EfaTime.BERLIN
    ): Long? = option.arrivalInstantMillis ?: EfaTime.forNow(option.arrival, referenceMillis, zone)
}
