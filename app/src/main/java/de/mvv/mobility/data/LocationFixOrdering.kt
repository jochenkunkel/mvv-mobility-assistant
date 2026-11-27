package de.mvv.mobility.data

/**
 * Rejects out-of-order location callbacks using Android's monotonic fix timestamp.
 *
 * A zero/negative timestamp is not comparable and must not overwrite an accepted
 * fix. Equal timestamps are treated as duplicates. Reset this guard when a new
 * navigation session starts; wall-clock timestamps are deliberately not used.
 *
 * This pure state helper is not yet wired to the Android location callback.
 */
internal class LocationFixOrdering {
    private var latestAcceptedNanos: Long = 0L

    fun accept(elapsedRealtimeNanos: Long): Boolean {
        if (elapsedRealtimeNanos <= 0L || elapsedRealtimeNanos <= latestAcceptedNanos) return false
        latestAcceptedNanos = elapsedRealtimeNanos
        return true
    }

    fun reset() {
        latestAcceptedNanos = 0L
    }
}
