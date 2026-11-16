package de.mvv.mobility.data

import android.content.Context
import android.location.Geocoder
import android.location.Location
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class WalkingTurn(val instruction: String, val arrow: String, val latitude: Double,
    val longitude: Double, val distanceMeters: Int)
data class WalkingRoute(val turns: List<WalkingTurn>, val totalMeters: Int, val path: List<Pair<Double, Double>> = emptyList())

/** OSRM foot routing; never substitute a straight-line route for missing turn data. */
object WalkingNavigation {
    fun fetch(context: Context, origin: Location, stopName: String, callback: (Result<WalkingRoute>) -> Unit) {
        Thread {
            val result = runCatching {
                @Suppress("DEPRECATION")
                val matches = Geocoder(context).getFromLocationName(MobilityProviderConfig.geocodingQuery(stopName), 3)
                    ?: emptyList()
                val destination = matches.firstOrNull()
                    ?: error("Haltestelle konnte nicht lokalisiert werden")
                val url = URL("https://routing.openstreetmap.de/routed-foot/route/v1/driving/" +
                    "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}" +
                    "?overview=full&geometries=geojson&steps=true")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "MeinWeg2/0.2 Android foot navigation")
                }
                val body = try {
                    check(connection.responseCode in 200..299) { "Routingdienst nicht erreichbar" }
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally { connection.disconnect() }
                parse(body)
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    fun parse(json: String): WalkingRoute {
        val root = JSONObject(json)
        check(root.optString("code") == "Ok") { "Keine Fußgängerroute gefunden" }
        val route = root.getJSONArray("routes").getJSONObject(0)
        val legs = route.getJSONArray("legs")
        val turns = mutableListOf<WalkingTurn>()
        for (i in 0 until legs.length()) {
            val steps = legs.getJSONObject(i).getJSONArray("steps")
            for (j in 0 until steps.length()) {
                val step = steps.getJSONObject(j)
                val maneuver = step.getJSONObject("maneuver")
                val coordinates = maneuver.getJSONArray("location")
                val type = maneuver.optString("type")
                val modifier = maneuver.optString("modifier")
                val (arrow, label) = when {
                    type == "arrive" -> "✓" to "Ziel erreicht"
                    modifier.contains("left") -> "↰" to "Links abbiegen"
                    modifier.contains("right") -> "↱" to "Rechts abbiegen"
                    else -> "↑" to "Geradeaus"
                }
                turns.add(WalkingTurn(label, arrow, coordinates.getDouble(1), coordinates.getDouble(0),
                    step.optDouble("distance", 0.0).roundToInt().coerceAtLeast(0)))
            }
        }
        check(turns.isNotEmpty()) { "Keine Abbiegeanweisungen vorhanden" }
        val path = mutableListOf<Pair<Double, Double>>()
        val coordinates = route.optJSONObject("geometry")?.optJSONArray("coordinates")
        if (coordinates != null) {
            for (i in 0 until coordinates.length()) {
                val point = coordinates.optJSONArray(i) ?: continue
                if (point.length() >= 2) path.add(point.getDouble(1) to point.getDouble(0))
            }
        }
        return WalkingRoute(turns, route.optDouble("distance", 0.0).roundToInt(), path)
    }

    fun nextTurn(route: WalkingRoute, position: Location): Pair<WalkingTurn, Int>? {
        val turns = route.turns
        if (turns.isEmpty()) return null
        // Progress by proximity to turn points; avoid showing a fake precise instruction
        // when the GPS fix is inaccurate.
        if (position.hasAccuracy() && position.accuracy > 60f) return null
        val distances = turns.map {
            val results = FloatArray(1)
            Location.distanceBetween(position.latitude, position.longitude, it.latitude, it.longitude, results)
            results[0].toInt()
        }
        val nearest = distances.indices.minByOrNull { distances[it] } ?: return null
        val next = if (distances[nearest] < 25 && nearest < turns.lastIndex) nearest + 1 else nearest
        return turns[next] to distances[next]
    }
}
