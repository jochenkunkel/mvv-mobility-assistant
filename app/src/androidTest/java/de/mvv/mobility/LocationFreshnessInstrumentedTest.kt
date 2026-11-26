package de.mvv.mobility

import android.location.Location
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.mvv.mobility.data.CurrentLocationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android Location timestamps; JVM unit tests cannot set elapsedRealtimeNanos. */
@RunWith(AndroidJUnit4::class)
class LocationFreshnessInstrumentedTest {
    private fun fix(elapsedNanos: Long, wallMillis: Long) = Location("test").apply {
        latitude = 48.1
        longitude = 11.5
        time = wallMillis
        elapsedRealtimeNanos = elapsedNanos
    }

    @Test fun ageUsesMonotonicClockWhenAvailable() {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val fix = fix(nowNanos - 5_000_000_000L, System.currentTimeMillis() - 86_400_000L)
        val age = requireNotNull(CurrentLocationRepository.ageMillis(
            fix, nowNanos, System.currentTimeMillis()))
        assertTrue(age in 4_900L..5_200L)
        assertFalse(CurrentLocationRepository.isFreshFix(fix, 1_000L))
    }

    @Test fun mixedClockBasesAreNotCompared() {
        val monotonic = fix(SystemClock.elapsedRealtimeNanos(), 1_000L)
        val wallOnly = Location("test").apply { time = 99_000L }
        assertFalse(CurrentLocationRepository.isNewer(wallOnly, monotonic))
        assertFalse(CurrentLocationRepository.isNewer(monotonic, wallOnly))
    }

    @Test fun monotonicNewerWinsOverWallClock() {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val older = fix(nowNanos - 10_000_000_000L, System.currentTimeMillis())
        val newer = fix(nowNanos, 1L)
        assertTrue(CurrentLocationRepository.isNewer(newer, older))
        assertFalse(CurrentLocationRepository.isNewer(older, newer))
        assertEquals(10_000L, CurrentLocationRepository.ageMillis(
            older, nowNanos, System.currentTimeMillis()))
    }
}
