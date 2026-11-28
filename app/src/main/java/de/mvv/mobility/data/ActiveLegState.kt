package de.mvv.mobility.data

/**
 * Pure, immutable active-leg state for a navigation session.
 * The UI must explicitly select the active leg; displaying an upcoming step
 * or receiving a nearby GPS fix is not a transition or boarding confirmation.
 */
data class ActiveLegState(
    val journeyKey: String? = null,
    val activeLegIndex: Int? = null,
    val generation: Long = 0L,
    val confirmedLegIndex: Int? = null
) {
    fun selectJourney(key: String?): ActiveLegState =
        if (key == journeyKey) this else ActiveLegState(
            journeyKey = key,
            generation = generation + 1
        )

    fun activateLeg(index: Int?, legCount: Int): ActiveLegState {
        val validIndex = index?.takeIf { journeyKey != null && it >= 0 && it < legCount }
        return if (validIndex == activeLegIndex) this else copy(
            activeLegIndex = validIndex,
            confirmedLegIndex = null,
            generation = generation + 1
        )
    }

    fun confirmBoarding(index: Int, isTransit: Boolean): ActiveLegState =
        if (isTransit && activeLegIndex == index && journeyKey != null)
            copy(confirmedLegIndex = index)
        else this

    fun guardEnabled(isTransit: Boolean, callbackGeneration: Long): Boolean =
        journeyKey != null && isTransit && activeLegIndex != null &&
            confirmedLegIndex == activeLegIndex && callbackGeneration == generation

    fun reset(): ActiveLegState = ActiveLegState(generation = generation + 1)
}
