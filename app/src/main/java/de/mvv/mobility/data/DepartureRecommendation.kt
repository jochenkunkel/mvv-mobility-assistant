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
    val start = when {
        firstWalking != null -> journeyClockMillis(firstWalking.departure, appointmentTargetMillis)
            ?: journeyClockMillis(journey.departure, appointmentTargetMillis)
            // Some MVV responses omit the walking departure clock entirely.
            // Use the first vehicle time minus the summed access legs instead of "–".
            ?: firstVehicle?.let { vehicle ->
                journeyClockMillis(vehicle.departure, appointmentTargetMillis)?.minus(
                    journey.steps.takeWhile { it.walking }.sumOf { it.minutes.coerceAtLeast(0) } * 60_000L
                )
            }
        firstVehicle != null -> journeyClockMillis(firstVehicle.departure, appointmentTargetMillis)
            ?.minus(journey.walkingMinutes.coerceAtLeast(0) * 60_000L)
        else -> journeyClockMillis(journey.departure, appointmentTargetMillis)
    }
    return start?.minus(safetyBufferMinutes * 60_000L)
}
