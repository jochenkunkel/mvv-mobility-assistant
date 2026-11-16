package de.mvv.mobility.data

/** Only the latest MVV request may update the displayed journey state. */
internal fun isCurrentSearchResponse(requestId: Int, currentGeneration: Int): Boolean =
    requestId == currentGeneration
