package de.mvv.mobility.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarlierConnectionsTest {
    private fun option(departure: String, arrival: String, line: String) = JourneyOption(
        departure, arrival, line, 0, 5, listOf(line), listOf("U-Bahn"),
        listOf(JourneyStep(line, "U-Bahn", "A", "B", departure, arrival, false, 10))
    )

    @Test fun earlierWindowsAreMergedSortedAndDeduplicated() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 15, 30)
        val previous = listOf(option("15:15", "15:54", "U3"))
        val loaded = listOf(option("14:45", "15:24", "U3"), option("15:15", "15:54", "U3"))
        val merged = EarlierConnectionsResolver.merge(previous, loaded, now)
        assertEquals(2, merged.size)
        assertEquals("14:45", merged[0].option.departure)
        assertEquals("15:15", merged[1].option.departure)
    }

    @Test fun pastDeparturesAreMarked() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 15, 30)
        val merged = EarlierConnectionsResolver.merge(emptyList(),
            listOf(option("15:15", "15:54", "U3")), now)
        assertTrue(merged.single().isPast)
    }

    @Test fun futureDepartureIsNotMarkedPast() {
        val now = EfaTime.resolve(java.time.LocalDate.of(2026, 11, 26), 14, 0)
        val merged = EarlierConnectionsResolver.merge(emptyList(),
            listOf(option("15:15", "15:54", "U3")), now)
        assertFalse(merged.single().isPast)
    }
}
