package de.mvv.mobility.data

import java.io.IOException

/** Clear offline feedback; never pretend that cached timetables are live connections. */
fun routeFailureMessage(error: Throwable): String {
    val networkFailure = generateSequence(error) { it.cause }.any { it is IOException }
    return if (networkFailure) {
        "Keine Netzwerkverbindung oder MVV nicht erreichbar. Bitte Internet prüfen und Verbindung erneut suchen."
    } else {
        "MVV-Abfrage fehlgeschlagen: ${error.localizedMessage ?: "Unbekannter Fehler"}"
    }
}
