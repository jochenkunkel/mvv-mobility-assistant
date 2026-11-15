package de.mvv.mobility.data

import android.location.Location
import android.location.Geocoder
import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class JourneyOption(
    val departure: String, val arrival: String, val summary: String,
    val changes: Int, val walkingMinutes: Int,
    val lines: List<String> = emptyList(), val products: List<String> = emptyList(),
    val steps: List<JourneyStep> = emptyList()
)
data class JourneyStep(val line: String, val product: String, val from: String, val to: String,
    val departure: String, val arrival: String, val walking: Boolean, val minutes: Int)
enum class RouteFilterMode { ANY, CONTAINS, ONLY_LINE_WITH_WALK }
data class RoutePreference(val mode: RouteFilterMode = RouteFilterMode.ANY, val query: String = "")
fun filterJourneys(options: List<JourneyOption>, preference: RoutePreference): List<JourneyOption> {
    if (preference.mode == RouteFilterMode.ANY || preference.query.isBlank()) return options
    val target = preference.query.trim().lowercase(Locale.GERMANY)
    fun matches(line: String, product: String): Boolean {
        val l = line.lowercase(Locale.GERMANY).trim()
        val p = product.lowercase(Locale.GERMANY).trim()
        return l == target || "$p $l" == target || p == target ||
            (target == "bus" && p.contains("bus")) ||
            (target == "u-bahn" && p.contains("u-bahn")) ||
            (target == "s-bahn" && p.contains("s-bahn")) ||
            (target == "tram" && (p.contains("tram") || p.contains("straßenbahn")))
    }
    return options.filter { journey ->
        val matched = journey.lines.indices.map { i ->
            matches(journey.lines[i], journey.products.getOrElse(i) { "" })
        }
        when (preference.mode) {
            RouteFilterMode.ANY -> true
            RouteFilterMode.CONTAINS -> matched.any { it }
            RouteFilterMode.ONLY_LINE_WITH_WALK -> matched.isNotEmpty() && matched.all { it }
        }
    }
}


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
        // A successful HTTP response can still have an unsupported EFA schema.
        // Do not confuse parsing failure with a valid search returning zero trips.
        val trips = root.optJSONArray("trips") ?: root.optJSONObject("trip")?.optJSONArray("trips")
        if (trips == null) {
            val keys = root.keys().asSequence().take(12).joinToString(", ")
            val error = root.optString("error").take(160)
                .ifBlank { root.optString("message").take(160) }
            val detail = if (error.isBlank()) "" else "; Hinweis: $error"
            throw IllegalStateException(
                "MVV-Antwort nicht lesbar (Felder: $keys)$detail"
            )
        }
        val results = mutableListOf<JourneyOption>()
        if (trips != null) for (i in 0 until minOf(trips.length(), 24)) {
            val trip = trips.optJSONObject(i) ?: continue
            val legs = trip.optJSONArray("legs") ?: continue
            val names = mutableListOf<String>()
            var walkingMinutes = 0
            var vehicles = 0
            val products = mutableListOf<String>()
            val steps = mutableListOf<JourneyStep>()
            fun endpoint(leg: JSONObject?, departure: Boolean): JSONObject? {
                if (leg == null) return null
                val points = leg.optJSONArray("points")
                return if (points != null && points.length() > 0)
                    points.optJSONObject(if (departure) 0 else points.length() - 1)
                else leg.optJSONObject(if (departure) "origin" else "destination")
            }
            fun efaTime(point: JSONObject?): String {
                val dt = point?.optJSONObject("dateTime")
                val live = dt?.optString("rtTime").orEmpty()
                val planned = dt?.optString("time").orEmpty()
                return live.ifBlank { planned }.ifBlank { time(point) }
            }

            for (j in 0 until legs.length()) {
                val leg = legs.optJSONObject(j) ?: continue
                // Real MVV EFA uses mode + points, unlike the legacy fixture schema.
                val mode = leg.optJSONObject("mode")
                val transport = leg.optJSONObject("transportation")
                val product = mode?.optString("product").orEmpty()
                    .ifBlank { transport?.optJSONObject("product")?.optString("name").orEmpty() }
                val number = mode?.text("number").orEmpty()
                    .ifBlank { transport?.text("number").orEmpty() }
                    .ifBlank { transport?.text("name").orEmpty() }
                val walking = product.contains("Fuss", true) || product.contains("Fuß", true) ||
                    product.contains("walk", true) || (mode == null && transport == null)
                val from = endpoint(leg, true)
                val to = endpoint(leg, false)
                steps.add(JourneyStep(number, product, from?.optString("name").orEmpty(),
                    to?.optString("name").orEmpty(), efaTime(from), efaTime(to), walking,
                    leg.optInt("timeMinute", 0)))
                if (walking) {
                    val distance = leg.optDouble("distance", 0.0)
                    if (distance > 0) {
                        walkingMinutes += kotlin.math.ceil(distance / (walkingKmh * 1000.0 / 60.0)).toInt()
                    } else {
                        walkingMinutes += leg.optInt("timeMinute", 0)
                    }
                } else if (number.isNotBlank()) {
                    names.add(number)
                    products.add(product)
                    vehicles++
                }
            }
            val first = legs.optJSONObject(0)
            val last = legs.optJSONObject(legs.length() - 1)
            if (names.isEmpty() && vehicles == 0 && walkingMinutes == 0) continue
            results.add(JourneyOption(
                departure = efaTime(endpoint(first, true)),
                arrival = efaTime(endpoint(last, false)),
                summary = names.joinToString(" → ").ifBlank { "Verbindung ohne Linienangabe" },
                changes = (vehicles - 1).coerceAtLeast(0),
                walkingMinutes = walkingMinutes,
                lines = names.toList(), products = products.toList(), steps = steps.toList()
            ))
        }
        return results
    }
    data class StopCandidate(val id: String, val name: String, val quality: Int)
    data class ResolvedJourney(val options: List<JourneyOption>, val destination: StopCandidate)
    class AmbiguousDestination(val candidates: List<StopCandidate>) :
        IllegalStateException("Mehrere MVV-Ziele gefunden. Bitte die passende Haltestelle auswählen.")

    private fun request(origin: Location, destination: String, type: String, arrivalByMillis: Long? = null, directOnly: Boolean = false): String {
        val originCoords = "${origin.longitude}:${origin.latitude}:WGS84"
        val requested = Date(arrivalByMillis ?: System.currentTimeMillis())
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val date = SimpleDateFormat("yyyyMMdd", Locale.GERMANY).apply { timeZone = berlin }.format(requested)
        val clock = SimpleDateFormat("HHmm", Locale.GERMANY).apply { timeZone = berlin }.format(requested)
        val url = "https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2?" +
            "outputFormat=JSON&tripReductionMacro=1&coordOutputFormat=WGS84&locationServerActive=1&" +
            "type_origin=coord&name_origin=${param(originCoords)}&" +
            "type_destination=$type&name_destination=${param(destination)}&" +
            "itdDate=$date&itdTime=$clock&itdTripDateTimeDepArr=${if (arrivalByMillis == null) "dep" else "arr"}&" +
            "calcNumberOfTrips=12&useRealtime=1" +
            (if (directOnly) "&itOptionsActive=1&maxChanges=0" else "")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode !in 200..299) error("MVV HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }

    /** Resolve a name through EFA first. Never silently select one of several destinations. */
    fun candidates(json: String): List<StopCandidate> {
        val destination = JSONObject(json).optJSONObject("destination") ?: return emptyList()
        val raw = destination.opt("points")
        val points = when (raw) {
            is org.json.JSONArray -> raw
            is JSONObject -> org.json.JSONArray().put(raw)
            else -> return emptyList()
        }
        val found = mutableListOf<StopCandidate>()
        for (i in 0 until points.length()) {
            val point = points.optJSONObject(i) ?: continue
            val id = point.optString("stateless")
            val name = point.optString("name")
            if (id.isNotBlank() && name.isNotBlank() &&
                (point.optString("anyType") == "stop" || point.optString("type") == "stop")) {
                found.add(StopCandidate(id, name, point.optInt("quality", 0)))
            }
        }
        return found.distinctBy { it.id }.sortedByDescending { it.quality }
    }

    private fun normalize(value: String) = java.text.Normalizer.normalize(
        value.lowercase(Locale.GERMANY), java.text.Normalizer.Form.NFD
    ).replace(Regex("""\p{M}+"""), "").replace(Regex("[^a-z0-9]+"), " ").trim()

    fun selectCandidate(query: String, options: List<StopCandidate>): StopCandidate? {
        if (options.size == 1) return options.single()
        // Only auto-select an unambiguous exact stop name, never merely the highest score.
        val exact = options.filter {
            val target = normalize(query)
            val name = normalize(it.name)
            name == target || name.endsWith(" $target")
        }
        return exact.singleOrNull()
    }

    /**
     * EFA can calculate public-transport access and egress directly from coordinates.
     * This avoids prematurely choosing the nearest stop (which might be slower).
     * Geocoding is done off the main thread and is never treated as a stop identifier.
     */
    fun searchAddress(
        context: Context,
        origin: Location,
        address: String,
        walkingKmh: Float,
        callback: (Result<ResolvedJourney>) -> Unit,
        preference: RoutePreference = RoutePreference(),
        arrivalByMillis: Long? = null
    ) {
        Thread {
            try {
                require(address.isNotBlank()) { "Zieladresse fehlt." }
                @Suppress("DEPRECATION")
                val places = Geocoder(context, Locale.GERMANY).getFromLocationName(address, 5).orEmpty()
                if (places.isEmpty()) error("Zieladresse konnte nicht geografisch bestimmt werden.")
                // Multiple geocoding matches must not silently route to an unrelated location.
                val first = places.first()
                val ambiguous = places.drop(1).any { other ->
                    val a = Location("geocode").apply { latitude = first.latitude; longitude = first.longitude }
                    val b = Location("geocode").apply { latitude = other.latitude; longitude = other.longitude }
                    a.distanceTo(b) > 1000f
                }
                if (ambiguous) error("Mehrdeutige Zieladresse. Bitte vollständige Straße, Hausnummer und Ort angeben.")
                val coordinates = "${first.longitude}:${first.latitude}:WGS84"
                val response = request(origin, coordinates, "coord", arrivalByMillis)
                var trips = filterJourneys(parseTrips(response, walkingKmh), preference)
                if (trips.isEmpty() && preference.mode == RouteFilterMode.ONLY_LINE_WITH_WALK) {
                    val alternate = request(origin, coordinates, "coord", arrivalByMillis, directOnly = true)
                    trips = filterJourneys(parseTrips(alternate, walkingKmh), preference)
                }
                val destination = StopCandidate(coordinates, first.getAddressLine(0) ?: address, 0)
                callback(Result.success(ResolvedJourney(trips, destination)))
            } catch (e: Exception) { callback(Result.failure(e)) }
        }.start()
    }

    fun search(
        origin: Location,
        destination: String,
        walkingKmh: Float,
        callback: (Result<ResolvedJourney>) -> Unit,
        selectedId: String? = null
    ) {
        Thread {
            try {
                val lookup = request(origin, destination, "any")
                val options = candidates(lookup)
                if (options.isEmpty()) error("MVV konnte keine passende Zielhaltestelle auflösen.")
                val selected = if (selectedId == null) selectCandidate(destination, options)
                    ?: throw AmbiguousDestination(options)
                else options.singleOrNull { it.id == selectedId }
                    ?: error("Ausgewählte Haltestelle ist nicht mehr gültig. Bitte erneut auswählen.")
                // Second request with validated stop ID. Re-check returned destination identity.
                val response = request(origin, selected.id, "stop")
                val returned = candidates(response)
                if (returned.isNotEmpty() && returned.none { it.id == selected.id }) {
                    error("MVV hat eine andere Zielhaltestelle zurückgegeben.")
                }
                val trips = parseTrips(response, walkingKmh)
                callback(Result.success(ResolvedJourney(trips, selected)))
            } catch (e: Exception) { callback(Result.failure(e)) }
        }.start()
    }
}
