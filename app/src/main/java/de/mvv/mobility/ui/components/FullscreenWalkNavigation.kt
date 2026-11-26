package de.mvv.mobility.ui.components

import android.location.Location
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
import de.mvv.mobility.data.WalkingRoute
import de.mvv.mobility.data.WalkingTurn

/** Design 1: map-focused fullscreen pedestrian navigation. */
@Composable
fun FullscreenWalkNavigation(
    route: WalkingRoute,
    position: Location?,
    walkedSegments: List<List<Pair<Double, Double>>>,
    turn: Pair<WalkingTurn, Int>?,
    distanceText: String,
    walkingTimeText: String,
    departureText: String,
    bufferText: String,
    boardingText: String,
    warnings: List<String>,
    onBack: () -> Unit,
    onEnd: () -> Unit,
    onRecenter: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F5FC)).testTag("fullscreen_navigation")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)
                .testTag("fullscreen_nav_back")) { Text("← Fahrt") }
            Text("Navigation zum Einstieg", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onRecenter, modifier = Modifier.heightIn(min = 52.dp)
                .testTag("recenter_map")) { Text("Zentrieren") }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5635B1))) {
            Text(
                text = if (turn != null) "In ${turn.second} m ${turn.first.instruction.lowercase()}"
                    else "Keine Abbiegeanweisung verfügbar",
                color = Color.White, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
                    .testTag("navigation_turn_banner")
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
            WalkingMiniMap(route = route, position = position,
                walkedSegments = walkedSegments,
                modifier = Modifier.fillMaxSize().testTag("fullscreen_navigation_map"))
        }
        Column(Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow("Reststrecke", distanceText, "nav_remaining_distance")
            InfoRow("Gehzeit", walkingTimeText, "nav_remaining_time")
            InfoRow("Abfahrt", departureText, "nav_departure")
            InfoRow("Puffer", bufferText, "nav_buffer")
            InfoRow("Einstieg", boardingText, "nav_boarding")
            warnings.forEach {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("navigation_warning"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag("fullscreen_nav_back_footer")) { Text("Zurück zur Fahrt") }
                Button(onClick = onEnd,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag("fullscreen_nav_end")) { Text("Navigation beenden") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value.ifBlank { "unbekannt" }, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(tag))
    }
}
