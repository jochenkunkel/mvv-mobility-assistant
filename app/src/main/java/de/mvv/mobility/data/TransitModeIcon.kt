package de.mvv.mobility.data

import java.util.Locale

/** Display transport mode from MVV's product field, with conservative line fallback. */
object TransitModeIcon {
    fun symbol(product: String, line: String, walking: Boolean = false): String {
        if (walking) return "🚶"
        val mode = product.trim().lowercase(Locale.ROOT)
        val route = line.trim().uppercase(Locale.ROOT)
        return when {
            mode.contains("s-bahn") || mode.contains("s bahn") ||
                mode.contains("suburban") || mode.contains("cityrail") -> "Ⓢ"
            mode.contains("u-bahn") || mode.contains("u bahn") ||
                mode.contains("subway") || mode.contains("metro") -> "🚇"
            mode.contains("tram") || mode.contains("straßenbahn") ||
                mode.contains("strassenbahn") || mode.contains("streetcar") -> "🚋"
            mode.contains("bus") -> "🚌"
            route.matches(Regex("S\\d{1,2}[A-Z]?")) -> "Ⓢ"
            route.matches(Regex("U\\d{1,2}[A-Z]?")) -> "🚇"
            // Unknown product must not be presented as a bus.
            else -> "🚏"
        }
    }

    fun label(product: String, line: String, walking: Boolean = false): String {
        if (walking) return "Fußweg"
        return when (symbol(product, line)) {
            "Ⓢ" -> "S-Bahn"
            "🚇" -> "U-Bahn"
            "🚋" -> "Tram"
            "🚌" -> "Bus"
            "🚏" -> "ÖPNV (Typ unbekannt)"
            else -> "ÖPNV"
        }
    }
}
