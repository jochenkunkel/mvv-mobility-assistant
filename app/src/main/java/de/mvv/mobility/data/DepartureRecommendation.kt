package de.mvv.mobility.data

import java.util.Calendar
import java.util.TimeZone

/** Times from MVV are local clock values; anchor them to the appointment date. */
fun journeyClockMillis(clock: String, appointmentTargetMillis: Long): Long? {
    val parts = clock.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
        timeInMillis = appointmentTargetMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (calendar.timeInMillis > appointmentTargetMillis + 30 * 60_000L) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    return calendar.timeInMillis
}

/**
 * For a door-to-door journey, the initial walking leg already belongs to the
 * itinerary. Only subtract the safety buffer from its start, never the walk twice.
 * If MVV provides no first walking leg, use the first vehicle minus estimated
 * access walk time. Without a vehicle, fall back to journey departure.
 */
fun recommendedLeaveMillis(
    journey: JourneyOption,
    appointmentTargetMillis: Long,
    safetyBufferMinutes: Int = 3
): Long? {
    require(safetyBufferMinutes >= 0)
    val firstVehicle = journey.steps.firstOrNull { !it.walking }
    val firstWalking = journey.steps.firstOrNull()?.takeIf { it.walking }
    val accessWalkMinutes = journey.steps.takeWhile { it.walking }
        .sumOf { it.minutes.coerceAtLeast(0) }
        .takeIf { it > 0 } ?: journey.walkingMinutes.coerceAtLeast(0)
    val firstVehicleDeparture = firstVehicle?.let {
        journeyClockMillis(it.departure, appointmentTargetMillis)
    }
    val firstWalkingDeparture = firstWalking?.let {
        journeyClockMillis(it.departure, appointmentTargetMillis)
    }
    val journeyDeparture = journeyClockMillis(journey.departure, appointmentTargetMillis)
    // Prefer the actual walking departure when provided. If it is absent, calculate
    // from the first vehicle departure rather than mistaking the bus clock for
    // the departure of the entire door-to-door journey.
    val start = when {
        firstWalkingDeparture != null -> firstWalkingDeparture
        firstVehicleDeparture != null -> firstVehicleDeparture - accessWalkMinutes * 60_000L
        journeyDeparture != null -> journeyDeparture
        else -> null
    }
    return start?.minus(safetyBufferMinutes * 60_000L)
}
