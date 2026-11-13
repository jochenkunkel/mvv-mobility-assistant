package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.CurrentLocationRepository
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
                    var location by remember { mutableStateOf<Location?>(null) }
                    var locationStatus by remember { mutableStateOf("Noch kein Standort abgerufen") }
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        if (grants.values.any { it }) {
                            locationStatus = "Standort wird ermittelt …"
                            CurrentLocationRepository.request(this@MainActivity) { found ->
                                location = found
                                locationStatus = if (found == null) "Standort nicht verfügbar. Standortdienste prüfen." else "Aktueller Startpunkt ermittelt"
                            }
                        } else locationStatus = "Standortberechtigung nicht erteilt"
                    }
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Mein Weg", style = MaterialTheme.typography.headlineMedium)
                        Text("Phase 3 · Kalender + Standort", color = MaterialTheme.colorScheme.primary)
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
                        Text("Startpunkt", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = {
                            if (CurrentLocationRepository.hasPermission(this@MainActivity)) {
                                locationStatus = "Standort wird ermittelt …"
                                CurrentLocationRepository.request(this@MainActivity) { found ->
                                    location = found
                                    locationStatus = if (found == null) "Standort nicht verfügbar. Standortdienste prüfen." else "Aktueller Startpunkt ermittelt"
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        }) { Text("Aktuellen Standort verwenden") }
                        Text(locationStatus)
                        location?.let {
                            Text("Breite: %.5f · Länge: %.5f".format(java.util.Locale.GERMANY, it.latitude, it.longitude))
                        }
                        Text("Die Position wird nur auf Anfrage ermittelt, nicht gespeichert und nicht im Hintergrund verfolgt.")
                        Text("MVV-Verbindung, Komfort-Routen und Abfahrtszeiten folgen als nächster Schritt.")
                    }
                }
            }
        }
    }
}
