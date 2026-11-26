package de.mvv.mobility.data

import java.time.ZoneId

data class EarlierJourney(
    val option: JourneyOption,
    val departureMillis: Long?,
    val isPast: Boolean
)

/**
 * Chronological, duplicate-free merge of earlier search windows.
 *
 * The MVV request window shifts backwards; the UI keeps already loaded
 * results. Past departures remain visible but are explicitly marked so they
 * are never presented as still reachable.
 */
object EarlierConnectionsResolver {
    private fun key(option: JourneyOption) = listOf(
        option.departure.trim(), option.arrival.trim(),
        option.lines.joinToString("|"), option.summary.trim()
    )

    fun merge(
        previous: List<JourneyOption>,
        loaded: List<JourneyOption>,
        nowMillis: Long,
        zone: ZoneId = EfaTime.BERLIN
    ): List<EarlierJourney> {
        val seen = LinkedHashSet<List<String>>()
        val all = (previous + loaded).filter { seen.add(key(it)) }
        return all.map { option ->
            val first = option.steps.firstOrNull { !it.walking }?.departure ?: option.departure
            val departure = option.departureInstantMillis ?: EfaTime.forNow(first, nowMillis, zone)
            EarlierJourney(option, departure, departure != null && departure < nowMillis)
        }.sortedWith(
            compareBy<EarlierJourney> { it.departureMillis ?: Long.MAX_VALUE }
                .thenBy { it.option.arrival }
        )
    }
}
