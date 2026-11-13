package de.mvv.mobility.data

import android.location.Location
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JourneyOption(val departure: String, val arrival: String, val summary: String, val changes: Int, val walkingMinutes: Int)

object MvvJourneyRepository {
    private fun param(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun JSONObject.text(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
    private fun time(obj: JSONObject?): String {
        if (obj == null) return "–"
        val d = obj.optJSONObject("dateTime") ?: obj
        val hour = d.optString("hour")
        val minute = d.optString("minute")
        return if (hour.isNotBlank() && minute.isNotBlank()) "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}" else "–"
    }
    /** Deterministic parser shared by live MVV calls and emulator fixture tests. */
    fun parseTrips(json: String, walkingKmh: Float): List<JourneyOption> {
        require(walkingKmh > 0f) { "Walking speed must be positive" }
        val root = JSONObject(json)
        val trips = root.optJSONArray("trips") ?: root.optJSONObject("trip")?.optJSONArray("trips")
        val results = mutableListOf<JourneyOption>()
        if (trips != null) for (i in 0 until minOf(trips.length(), 6)) {
            val trip = trips.optJSONObject(i) ?: continue
            val legs = trip.optJSONArray("legs") ?: continue
            val names = mutableListOf<String>()
            var walkingMinutes = 0
            var vehicles = 0
            for (j in 0 until legs.length()) {
                val leg = legs.optJSONObject(j) ?: continue
                val transport = leg.optJSONObject("transportation")
                val name = transport?.text("number").orEmpty().ifBlank { transport?.text("name").orEmpty() }
                val mode = transport?.optJSONObject("product")?.text("name").orEmpty()
                if (name.isNotBlank()) { names.add(name); vehicles++ }
                else if (mode.contains("Fuß", true) || mode.contains("walk", true) || transport == null) {
                    val distance = leg.optDouble("distance", 0.0)
                    if (distance > 0) walkingMinutes += kotlin.math.ceil(distance / (walkingKmh * 1000.0 / 60.0)).toInt()
                }
            }
            val first = legs.optJSONObject(0)
            val last = legs.optJSONObject(legs.length() - 1)
            results.add(JourneyOption(
                departure = time(first?.optJSONObject("origin")),
                arrival = time(last?.optJSONObject("destination")),
                summary = names.joinToString(" → ").ifBlank { "Verbindung ohne Linienangabe" },
                changes = (vehicles - 1).coerceAtLeast(0),
                walkingMinutes = walkingMinutes
            ))
        }
        return results
    }
    fun search(origin: Location, destination: String, walkingKmh: Float, callback: (Result<List<JourneyOption>>) -> Unit) {
        Thread {
            try {
                val originCoords = "${origin.longitude}:${origin.latitude}:WGS84"
                val date = SimpleDateFormat("yyyyMMdd", Locale.GERMANY).format(Date())
                val clock = SimpleDateFormat("HHmm", Locale.GERMANY).format(Date())
                val url = "https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2?" +
                    "outputFormat=JSON&coordOutputFormat=WGS84&locationServerActive=1&" +
                    "type_origin=coord&name_origin=${param(originCoords)}&" +
                    "type_destination=any&name_destination=${param(destination)}&" +
                    "itdDate=$date&itdTime=$clock&itdTripDateTimeDepArr=dep&" +
                    "calcNumberOfTrips=4&useRealtime=1"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")
                try {
                    if (connection.responseCode !in 200..299) error("MVV HTTP ${connection.responseCode}")
                    val results = parseTrips(connection.inputStream.bufferedReader().use { it.readText() }, walkingKmh)
                    callback(Result.success(results))
                } finally { connection.disconnect() }
            } catch (e: Exception) { callback(Result.failure(e)) }
        }.start()
    }
}
