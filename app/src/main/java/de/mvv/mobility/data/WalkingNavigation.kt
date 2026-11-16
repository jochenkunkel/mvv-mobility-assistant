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
data class WalkingRoute(val turns: List<WalkingTurn>, val totalMeters: Int, val path: List<Pair<Double, Double>> = emptyList(),
    val destination: StopPosition? = null)

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
                // Free-text geocoding can return a street instead of the actual stop.
                val ambiguous = matches.drop(1).any { other ->
                    val first = Location("geocoder").apply {
                        latitude = destination.latitude
                        longitude = destination.longitude
                    }
                    val second = Location("geocoder").apply {
                        latitude = other.latitude
                        longitude = other.longitude
                    }
                    first.distanceTo(second) > 75f
                }
                check(!ambiguous) {
                    "Haltestelle nicht eindeutig lokalisiert – bitte Ziel prüfen"
                }
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

    /** Route directly to the EFA endpoint; never re-geocode a stop name. */
    fun fetch(origin: Location, stop: StopPosition, callback: (Result<WalkingRoute>) -> Unit) {
        Thread {
            val result = runCatching {
                val url = URL("https://routing.openstreetmap.de/routed-foot/route/v1/driving/" +
                    "${origin.longitude},${origin.latitude};${stop.longitude},${stop.latitude}" +
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
                parse(body).copy(destination = stop)
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
        // Never present a precise maneuver from an inaccurate or off-route GPS fix.
        if (position.hasAccuracy() && position.accuracy > 35f) return null
        val turn = RouteProgress.nextTurn(route, position.latitude, position.longitude)
        if (turn?.first?.instruction == "Ziel erreicht" &&
            (!position.hasAccuracy() || position.accuracy > 12f)) {
            return turn.first.copy(
                instruction = "Zielnähe – Haltestelle prüfen", arrow = "⌖"
            ) to turn.second
        }
        return turn
    }

}
