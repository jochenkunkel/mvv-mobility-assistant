package de.mvv.mobility.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.ConnectionCheckState
import de.mvv.mobility.data.UpcomingConnection

/** Design 5: compact fullscreen journey dashboard, no app bottom navigation. */
@Composable
fun FullscreenJourneyDashboard(
    countdown: String,
    leaveAt: String,
    line: String,
    departure: String,
    boarding: String,
    platform: String,
    walking: String,
    distance: String,
    buffer: String,
    arrival: String,
    warnings: List<String>,
    upcoming: UpcomingConnection?,
    checkState: ConnectionCheckState?,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onMissed: () -> Unit,
    onNewSearch: () -> Unit,
    onNavigate: () -> Unit,
    onEndSession: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7F5FC)).verticalScroll(rememberScrollState())
            .testTag("fullscreen_dashboard"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)
                .testTag("fullscreen_dashboard_back")) { Text("← Zurück") }
            Text("Meine Verbindung", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(warnings.firstOrNull { it.startsWith("GPS") } ?: "GPS-Status",
                style = MaterialTheme.typography.labelSmall)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5635B1))) {
            Column(Modifier.padding(18.dp)) {
                Text("NÄCHSTER SCHRITT", color = Color.White,
                    style = MaterialTheme.typography.labelMedium)
                Text(countdown, color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("fullscreen_countdown"))
            }
        }
        InfoCard("LOSGEHEN", leaveAt, "fullscreen_leave")
        InfoCard("LINIE", line, "fullscreen_line")
        InfoCard("ABFAHRT", departure, "fullscreen_departure")
        InfoCard("EINSTIEG", if (platform.isBlank()) boarding else "$boarding · $platform", "fullscreen_boarding")
        InfoCard("FUSSWEG", "$walking · $distance", "fullscreen_walking")
        InfoCard("PUFFER", buffer, "fullscreen_buffer")
        InfoCard("ANKUNFT / ZIEL", arrival, "fullscreen_arrival")

        if (upcoming != null) {
            Text("Anschluss: ${upcoming.step.line} → ${upcoming.step.to}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("fullscreen_upcoming_connection"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCheckIn, enabled = checkState == null,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag("connection_checkin")) {
                    Text(if (checkState == ConnectionCheckState.CHECKED_IN) "✓ Check-in" else "Anschluss erreicht")
                }
                OutlinedButton(onClick = onMissed, enabled = checkState == null,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag("connection_missed")) {
                    Text(if (checkState == ConnectionCheckState.MISSED) "Verpasst" else "Nicht erreicht")
                }
            }
            if (newSearchOffered(checkState)) {
                Button(onClick = onNewSearch, modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 52.dp).testTag("new_connection_search")) {
                    Text("Neue Verbindung suchen")
                }
            }
        }
        warnings.forEach { warning ->
            Text(warning, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("fullscreen_warning"))
        }
        Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .testTag("fullscreen_navigate")) { Text("Zum MVV-Einstiegspunkt navigieren") }
        TextButton(onClick = onEndSession, modifier = Modifier.testTag("fullscreen_end_session")) {
            Text("Navigation beenden und Spur löschen")
        }
    }
}

private fun newSearchOffered(state: ConnectionCheckState?) = state == ConnectionCheckState.MISSED

@Composable
private fun InfoCard(label: String, value: String, tag: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF62577D))
            Text(value.ifBlank { "unbekannt" }, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(tag))
        }
    }
}
