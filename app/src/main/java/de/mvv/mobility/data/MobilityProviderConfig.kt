package de.mvv.mobility.data

import de.mvv.mobility.BuildConfig

/**
 * One build-time region selector for launcher branding and region metadata.
 * MVV is the only implemented transit adapter. Berlin/Frankfurt builds are
 * branding previews, NOT functional public-transport integrations.
 */
enum class MobilityProvider { MVV, UNAVAILABLE }
enum class RegionId { MUNICH, BERLIN, FRANKFURT }

data class MobilityRegion(
    val city: String,
    val country: String,
    val timeZoneId: String,
    val geocodingContext: String
)

object MobilityProviderConfig {
    val regionId: RegionId = when (BuildConfig.MOBILITY_REGION) {
        "munich" -> RegionId.MUNICH
        "berlin" -> RegionId.BERLIN
        "frankfurt" -> RegionId.FRANKFURT
        else -> error("Unsupported region: ${BuildConfig.MOBILITY_REGION}")
    }

    val provider: MobilityProvider = when (regionId) {
        RegionId.MUNICH -> MobilityProvider.MVV
        RegionId.BERLIN, RegionId.FRANKFURT -> MobilityProvider.UNAVAILABLE
    }

    val region: MobilityRegion = when (regionId) {
        RegionId.MUNICH -> MobilityRegion("München", "DE", "Europe/Berlin", "München, Deutschland")
        RegionId.BERLIN -> MobilityRegion("Berlin", "DE", "Europe/Berlin", "Berlin, Deutschland")
        RegionId.FRANKFURT -> MobilityRegion("Frankfurt am Main", "DE", "Europe/Berlin", "Frankfurt am Main, Deutschland")
    }

    fun geocodingQuery(place: String): String =
        "${place.trim()}, ${region.geocodingContext}"
}
