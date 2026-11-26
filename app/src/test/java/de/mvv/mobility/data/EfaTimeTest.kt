package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EfaTimeTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test fun springGapIsShiftedForwardInsteadOfSilentlyNormalised() {
        // 29.03.2026 02:30 does not exist in Europe/Berlin.
        val resolved = EfaTime.resolve(LocalDate.of(2026, 3, 29), 2, 30, berlin)
        val local = java.time.Instant.ofEpochMilli(resolved).atZone(berlin)
        assertEquals(3, local.hour)
        assertEquals(30, local.minute)
    }

    @Test fun autumnOverlapUsesTheEarlierOffsetDeterministically() {
        // 25.10.2026 02:30 exists twice; the first (summer) offset is selected.
        val resolved = EfaTime.resolve(LocalDate.of(2026, 10, 25), 2, 30, berlin)
        val local = java.time.Instant.ofEpochMilli(resolved).atZone(berlin)
        assertEquals(2, local.hour)
        assertEquals(30, local.minute)
        assertEquals("+02:00", local.offset.toString())
    }

    @Test fun clockCandidatesIncludeFollowingDayForMidnightArrival() {
        val reference = EfaTime.resolve(LocalDate.of(2026, 11, 27), 0, 5, berlin)
        val candidates = EfaTime.arrivalCandidates("23:50", reference, berlin)
        assertTrue(candidates.any { it < reference })
        assertTrue(candidates.none { it == Long.MIN_VALUE })
    }

    @Test fun appointmentSelectionAcceptsPreviousDayArrivalAtMidnight() {
        val appointment = EfaTime.resolve(LocalDate.of(2026, 11, 27), 0, 5, berlin)
        val trip = JourneyOption("23:35", "23:50", "S1", 0, 5)
        val selected = selectAppointmentJourneys(listOf(trip), appointment)
        assertEquals(listOf(trip), selected)
    }

    @Test fun nowSearchRollsOverToFollowingDay() {
        val now = EfaTime.resolve(LocalDate.of(2026, 11, 26), 23, 50, berlin)
        val departure = requireNotNull(EfaTime.forNow("00:05", now, berlin))
        assertTrue(departure > now)
    }

    @Test fun realTimeIsPreferredOverPlannedTime() {
        val json = org.json.JSONObject(
            """{"dateTime":{"date":"20261126","time":"09:10","rtTime":"09:25"}}"""
        )
        val resolved = EfaTime.fromEfaPoint(json)
        val planned = EfaTime.resolve(LocalDate.of(2026, 11, 26), 9, 10, berlin)
        assertEquals(planned + 15 * 60_000L, resolved)
    }
}
