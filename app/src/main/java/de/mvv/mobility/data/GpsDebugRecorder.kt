package de.mvv.mobility.data

import android.location.Location
import de.mvv.mobility.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Foreground-only, bounded, opt-in GPS diagnostics for debug builds.
 * No automatic disk writes, uploads or background tracking.
 */
object GpsDebugRecorder {
    private const val MAX_EVENTS = 2400
    @Volatile var enabled: Boolean = false
    private val events = ArrayDeque<JSONObject>()
    private var startedAt = System.currentTimeMillis()

    @Synchronized fun clear() {
        events.clear()
        startedAt = System.currentTimeMillis()
    }

    @Synchronized fun event(
        type: String,
        fix: Location? = null,
        reason: String = "",
        navigationActive: Boolean = false,
        fixAgeMs: Long? = null
    ) {
        if (!BuildConfig.DEBUG || !BuildConfig.ENABLE_GPS_DEBUG_EXPORT || !enabled) return
        val item = JSONObject()
            .put("timestampMs", System.currentTimeMillis())
            .put("event", type)
            .put("reason", reason)
            .put("navigationActive", navigationActive)
        if (fixAgeMs != null) item.put("fixAgeMs", fixAgeMs)
        if (fix != null) {
            item.put("provider", fix.provider ?: "unknown")
                .put("fixTimestampMs", fix.time)
                .put("latitude", fix.latitude)
                .put("longitude", fix.longitude)
                .put("accuracyMeters", if (fix.hasAccuracy()) fix.accuracy else JSONObject.NULL)
                .put("speedMetersPerSecond", if (fix.hasSpeed()) fix.speed else JSONObject.NULL)
                .put("bearingDegrees", if (fix.hasBearing()) fix.bearing else JSONObject.NULL)
        }
        if (events.size >= MAX_EVENTS) events.removeFirst()
        events.addLast(item)
    }

    @Synchronized fun snapshot(): String? {
        if (!BuildConfig.DEBUG || !BuildConfig.ENABLE_GPS_DEBUG_EXPORT || !enabled) return null
        return JSONObject()
            .put("schemaVersion", 1)
            .put("startedAtMs", startedAt)
            .put("exportedAtMs", System.currentTimeMillis())
            .put("eventCount", events.size)
            .put("events", JSONArray().apply { events.forEach { put(it) } })
            .toString(2)
    }

    @Synchronized fun count(): Int = events.size
}
