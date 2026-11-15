package de.mvv.mobility.data

import java.io.IOException
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fast offline-only routing tests; no MVV server or Android emulator required. */
class OfflineRouteErrorTest {
    @Test fun networkFailureExplainsRetry() {
        val message = routeFailureMessage(IOException("Connection refused"))
        assertTrue(message.contains("Internet prüfen"))
        assertTrue(message.contains("erneut suchen"))
    }

    @Test fun nestedNetworkFailureIsRecognized() {
        val message = routeFailureMessage(IllegalStateException("request failed", IOException("offline")))
        assertTrue(message.contains("Netzwerkverbindung"))
    }

    @Test fun NonNetworkFailureKeepsUsefulDetails() {
        val message = routeFailureMessage(IllegalArgumentException("Zieladresse fehlt"))
        assertTrue(message.contains("Zieladresse fehlt"))
    }
}
