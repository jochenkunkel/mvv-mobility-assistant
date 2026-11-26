package de.mvv.mobility.data

import java.time.ZoneId

/** Explicit, manual connection confirmation. No GPS-derived auto check-in. */
enum class ConnectionCheckState { CHECKED_IN, MISSED }

data class UpcomingConnection(
    val stepIndex: Int,
    val step: JourneyStep,
    val departureMillis: Long
)

/** Determines whether a journey currently has a genuinely upcoming transfer. */
object ConnectionStatusResolver {
    private const val RECENT_WINDOW_MILLIS = 2 * 60_000L
    private const val UPCOMING_WINDOW_MILLIS = 120 * 60_000L

    fun upcoming(
        steps: List<JourneyStep>,
        nowMillis: Long,
        checks: Map<Int, ConnectionCheckState>,
        zone: ZoneId = EfaTime.BERLIN
    ): UpcomingConnection? {
        steps.forEachIndexed { index, step ->
            if (step.walking || step.departure.isBlank()) return@forEachIndexed
            if (checks.containsKey(index)) return@forEachIndexed
            val departure = EfaTime.forNow(step.departure, nowMillis, zone) ?: return@forEachIndexed
            if (departure >= nowMillis - RECENT_WINDOW_MILLIS &&
                departure <= nowMillis + UPCOMING_WINDOW_MILLIS) {
                return UpcomingConnection(index, step, departure)
            }
        }
        return null
    }
}
