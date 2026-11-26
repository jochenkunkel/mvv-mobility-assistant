package de.mvv.mobility.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * DST-safe resolution of EFA clock-only times.
 *
 * EFA responses usually carry `dateTime.date`, but older/fixture data can be
 * limited to `HH:mm`. A wall-clock `Calendar.set(HOUR_OF_DAY, ...)` silently
 * normalises non-existent spring-gap times and arbitrarily picks one offset in
 * autumn. This resolver uses zone rules explicitly and returns all plausible
 * neighbouring days so callers can apply their own before/after constraints.
 */
object EfaTime {
    val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")

    /** Resolve one local date/time; gap times shift by the transition, overlaps use the earlier offset. */
    fun resolve(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = BERLIN): Long {
        require(hour in 0..23 && minute in 0..59) { "Ungültige EFA-Uhrzeit" }
        val local = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
        val rules = zone.rules
        val valid = rules.getValidOffsets(local)
        val offset: ZoneOffset = when (valid.size) {
            1 -> valid[0]
            0 -> {
                // Spring-forward gap: move the local time forward by the gap
                // duration, then apply the offset after the transition.
                val transition = rules.getTransition(local)
                return local.plusSeconds(transition.duration.seconds).toInstant(transition.offsetAfter).toEpochMilli()
            }
            else -> valid[0] // autumn overlap: deterministic earlier (summer) offset
        }
        return local.toInstant(offset).toEpochMilli()
    }

    /** Same day and previous day instants for an arrival clock after midnight rollover. */
    fun arrivalCandidates(clock: String, referenceMillis: Long, zone: ZoneId = BERLIN): List<Long> {
        val parts = clockParts(clock) ?: return emptyList()
        val date = Instant.ofEpochMilli(referenceMillis).atZone(zone).toLocalDate()
        return listOf(-1L, 0L).map { delta ->
            resolve(date.plusDays(delta), parts.first, parts.second, zone)
        }
    }

    /** Single instant for "now" comparisons; roll to the next day only for a real rollover. */
    fun forNow(clock: String, nowMillis: Long, zone: ZoneId = BERLIN): Long? {
        val parts = clockParts(clock) ?: return null
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val sameDay = resolve(date, parts.first, parts.second, zone)
        if (sameDay < nowMillis - 12 * 60 * 60_000L) {
            return resolve(date.plusDays(1), parts.first, parts.second, zone)
        }
        return sameDay
    }

    private fun clockParts(clock: String): Pair<Int, Int>? {
        val match = Regex("""^([0-9]{1,2}):([0-9]{2})$""").matchEntire(clock.trim())
            ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour to minute else null
    }

    /** Absolute epoch from a full EFA `dateTime` object, using real time when present. */
    fun fromEfaPoint(obj: org.json.JSONObject?): Long? {
        val dt = obj?.optJSONObject("dateTime") ?: obj ?: return null
        val rawDate = dt.optString("date").takeIf { it.length >= 8 } ?: return null
        val date = runCatching {
            java.time.format.DateTimeFormatter.ofPattern(
                if (rawDate.contains('.')) "dd.MM.yyyy" else "yyyyMMdd"
            ).let { java.time.LocalDate.parse(rawDate, it) }
        }.getOrNull() ?: return null
        val parsedClock = parseClock(obj) ?: return null
        val clock = parsedClock.second.ifBlank { parsedClock.first }
        val parts = clock.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return runCatching { resolve(date, hour, minute) }.getOrNull()
    }

    /** Parse an EFA `dateTime` object, preferring the real-time value over the planned one. */
    fun parseClock(obj: org.json.JSONObject?): Pair<String, String>? {
        val dt = obj?.optJSONObject("dateTime") ?: obj ?: return null
        val planned = dt.optString("time").ifBlank {
            listOf(dt.optString("hour"), dt.optString("minute"))
                .takeIf { it.all { part -> part.isNotBlank() } }
                ?.joinToString(":") { it.padStart(2, '0') }.orEmpty()
        }
        if (planned.isBlank()) return null
        val realTime = dt.optString("rtTime")
        return planned to realTime
    }
}
