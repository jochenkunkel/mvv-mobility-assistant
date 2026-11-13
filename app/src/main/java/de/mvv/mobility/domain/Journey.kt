package de.mvv.mobility.domain

data class Journey(
    val appointment: String,
    val destination: String?,
    val minutesUntilDeparture: Int,
    val remainingMinutes: Int,
    val transfers: Int,
    val walkingMinutes: Int
)
enum class RoutePriority { COMFORT, FASTEST, LESS_WALKING }
data class TravelPreferences(
    val routePriority: RoutePriority = RoutePriority.COMFORT,
    val walkingSpeedMetersPerSecond: Double? = null
)
object JourneyRules {
    fun needsDestinationConfirmation(journey: Journey): Boolean = journey.destination.isNullOrBlank()
    fun walkingDurationMinutes(meters: Int, speedMetersPerSecond: Double): Int {
        require(meters >= 0) { "Distance must be non-negative" }
        require(speedMetersPerSecond > 0 && speedMetersPerSecond.isFinite()) { "Speed must be positive and finite" }
        return kotlin.math.ceil(meters / speedMetersPerSecond / 60.0).toInt()
    }
    fun comfortScore(transfers: Int, walkingMinutes: Int, durationMinutes: Int): Int {
        require(transfers >= 0 && walkingMinutes >= 0 && durationMinutes >= 0)
        return transfers * 15 + walkingMinutes * 3 + durationMinutes
    }
}
