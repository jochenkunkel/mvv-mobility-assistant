package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.DeviceCalendarRepository
import de.mvv.mobility.widget.JourneyWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var allowed by remember { mutableStateOf(DeviceCalendarRepository.hasPermission(this)) }
                    var refresh by remember { mutableIntStateOf(0) }
                    val scope = rememberCoroutineScope()
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        allowed = granted
                        refresh++
                        scope.launch { JourneyWidget().updateAll(this@MainActivity) }
                    }
                    val appointment = remember(allowed, refresh) {
                        if (allowed) DeviceCalendarRepository.nextAppointment(this) else null
                    }
                    var confirmedDestination by remember(appointment?.startMillis) { mutableStateOf("") }
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Mein Weg", style = MaterialTheme.typography.headlineMedium)
                        Text("Phase 2 · Kalender", color = MaterialTheme.colorScheme.primary)
                        if (!allowed) {
                            Text("Damit Mein Weg deine Termine anzeigen kann, erlaube den Kalenderzugriff. Die Daten bleiben auf deinem Gerät.")
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                                Text("Kalenderzugriff erlauben")
                            }
                        } else {
                            Button(onClick = {
                                refresh++
                                scope.launch { JourneyWidget().updateAll(this@MainActivity) }
                            }) { Text("Termine aktualisieren") }
                            if (appointment == null) {
                                Text("Keine bevorstehenden Termine mit Uhrzeit in den nächsten 14 Tagen gefunden.")
                            } else {
                                Text(appointment.title, style = MaterialTheme.typography.titleLarge)
                                Text("Beginn: ${appointment.timeLabel()}")
                                if (appointment.location == null) {
                                    Text("Für diesen Termin fehlt ein Zielort. Bitte bestätigen oder ergänzen.")
                                } else {
                                    Text("Kalender-Ziel: ${appointment.location}")
                                    Text("Bitte prüfe den Zielort vor der Routenplanung.")
                                }
                                OutlinedTextField(
                                    value = confirmedDestination,
                                    onValueChange = { confirmedDestination = it },
                                    label = { Text("Zielort bestätigen oder korrigieren") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (confirmedDestination.isNotBlank()) {
                                    Text("Bestätigtes Ziel (nur für diese Ansicht): $confirmedDestination")
                                }
                            }
                        }
                        HorizontalDivider()
                        Text("MVV-Routen, GPS und automatische Abfahrtszeiten folgen in späteren Phasen.")
                    }
                }
            }
        }
    }
}
