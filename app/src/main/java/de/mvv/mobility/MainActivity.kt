package de.mvv.mobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.DemoJourneyRepository
import de.mvv.mobility.domain.JourneyRules

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val journey = DemoJourneyRepository.nextJourney
                    var destination by remember { mutableStateOf("") }
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Mein Weg", style = MaterialTheme.typography.headlineMedium)
                        Text("Phase 1 · DEMODATEN", color = MaterialTheme.colorScheme.primary)
                        Text(journey.appointment, style = MaterialTheme.typography.titleLarge)
                        Text("Abfahrt in ${journey.minutesUntilDeparture} Minuten")
                        Text("Restreisezeit: ${journey.remainingMinutes} Minuten")
                        Text("${journey.transfers} Umstieg · ${journey.walkingMinutes} Minuten Fußweg")
                        if (JourneyRules.needsDestinationConfirmation(journey)) {
                            Text("Für diesen Termin fehlt noch ein Zielort.")
                            OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Zielort eingeben") })
                            if (destination.isNotBlank()) Text("Ziel erfasst (nur Demo): $destination")
                        }
                        Text("Routenpräferenz: Komfort · Gehgeschwindigkeit: individuell")
                        Text("Noch keine Live-Navigation oder MVV-Abfrage.")
                    }
                }
            }
        }
    }
}
