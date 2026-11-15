package de.mvv.mobility.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag

/**
 * Reusable cockpit overview: navigation actions are supplied by the host screen.
 * No calendar, GPS or MVV repository dependency lives in this presentation component.
 */
@Composable
fun CockpitOverview(
    appointment: String?,
    destination: String?,
    journeyCount: Int,
    hasActiveJourney: Boolean,
    onAppointment: () -> Unit,
    onConnections: () -> Unit,
    onJourney: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("cockpit_overview")) {
        Text("Dein Weg auf einen Blick", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = Color(0xFF302060))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CockpitTile("01", "Termin", appointment ?: "Termin wählen",
                Modifier.weight(1f).testTag("cockpit_appointment"), onAppointment)
            CockpitTile("02", "Verbindungen", if (journeyCount > 0) "$journeyCount gefunden" else "Route planen",
                Modifier.weight(1f).testTag("cockpit_connections"), onConnections)
            CockpitTile("03", "Meine Fahrt", if (hasActiveJourney) "Aktiv" else "Noch keine Fahrt",
                Modifier.weight(1f).testTag("cockpit_journey"), onJourney)
        }
        if (!destination.isNullOrBlank()) {
            Text("Ziel: $destination", style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49309B), maxLines = 2)
        }
    }
}

@Composable
private fun CockpitTile(
    number: String,
    title: String,
    detail: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0ECFF))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6841B2))
            Text(title, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = Color(0xFF35236D))
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5C5470), maxLines = 2)
        }
    }
}
