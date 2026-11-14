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
    data class StopCandidate(val id: String, val name: String, val quality: Int)
    data class ResolvedJourney(val options: List<JourneyOption>, val destination: StopCandidate)
    class AmbiguousDestination(val candidates: List<StopCandidate>) :
        IllegalStateException("Mehrere MVV-Ziele gefunden. Bitte die passende Haltestelle auswählen.")

    private fun request(origin: Location, destination: String, type: String): String {
        val originCoords = "${origin.longitude}:${origin.latitude}:WGS84"
        val now = Date()
        val date = SimpleDateFormat("yyyyMMdd", Locale.GERMANY).format(now)
        val clock = SimpleDateFormat("HHmm", Locale.GERMANY).format(now)
        val url = "https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2?" +
            "outputFormat=JSON&tripReductionMacro=1&coordOutputFormat=WGS84&locationServerActive=1&" +
            "type_origin=coord&name_origin=${param(originCoords)}&" +
            "type_destination=$type&name_destination=${param(destination)}&" +
            "itdDate=$date&itdTime=$clock&itdTripDateTimeDepArr=dep&" +
            "calcNumberOfTrips=4&useRealtime=1"
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
