package de.mvv.mobility.data

/**
 * Central, build-time mobility provider configuration.
 *
 * MVV / Munich is the only implemented provider. Changing the city alone
 * does not activate another network: a matching provider adapter is required.
 * Keep transit API specifics in provider implementations, not UI code.
 */
enum class MobilityProvider { MVV }

data class MobilityRegion(
    val city: String,
    val country: String,
    val timeZoneId: String,
    val geocodingContext: String
)

object MobilityProviderConfig {
    val provider: MobilityProvider = MobilityProvider.MVV
    val region: MobilityRegion = MobilityRegion(
        city = "München",
        country = "DE",
        timeZoneId = "Europe/Berlin",
        geocodingContext = "München, Deutschland"
    )

    /** Add an adapter for each new provider before enabling a new region. */
    fun geocodingQuery(place: String): String =
        "${place.trim()}, ${region.geocodingContext}"
}
