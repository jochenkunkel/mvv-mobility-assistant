package de.mvv.mobility.data

/**
 * Opt-in, in-memory capture of the latest MVV response for debug APKs.
 * Never persists automatically; the user explicitly exports through SAF.
 * Responses can contain precise locations and destination addresses.
 */
object DebugResponseCapture {
    @Volatile var enabled: Boolean = false
    @Volatile private var lastResponse: String? = null

    fun record(response: String) {
        if (enabled) lastResponse = response.take(512_000)
    }

    fun snapshot(): String? = if (enabled) lastResponse else null

    fun clear() { lastResponse = null }
}
