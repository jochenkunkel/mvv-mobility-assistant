package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.CurrentLocationRepository
import de.mvv.mobility.data.DeviceCalendarRepository
import de.mvv.mobility.data.DestinationLookup
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.JourneyOption
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
                    var addressSuggestions by remember(appointment?.startMillis) { mutableStateOf<List<String>>(emptyList()) }
                    var lookupStatus by remember(appointment?.startMillis) { mutableStateOf("") }
                    var lookupRequest by remember(appointment?.startMillis) { mutableIntStateOf(0) }
                    var walkingSpeed by remember { mutableFloatStateOf(getPreferences(MODE_PRIVATE).getFloat("walking_speed_kmh", 3.0f)) }
                    var routeStatus by remember { mutableStateOf("") }
                    var journeys by remember { mutableStateOf<List<JourneyOption>>(emptyList()) }
                    var journeyLoading by remember { mutableStateOf(false) }
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
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Mein Weg", style = MaterialTheme.typography.headlineMedium)
                        Text("Phase 5 · MVV-Verbindungen (Test)", color = MaterialTheme.colorScheme.primary)
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
                                    Text("Du kannst den Ort automatisch suchen lassen und einen passenden Treffer auswählen.")
                                }
                                OutlinedTextField(
                                    value = confirmedDestination,
                                    onValueChange = { confirmedDestination = it; addressSuggestions = emptyList() },
                                    label = { Text("Zielort bestätigen oder korrigieren") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(onClick = {
                                    val query = confirmedDestination.trim().ifBlank { appointment.location.orEmpty() }
                                    lookupRequest++
                                    val requestId = lookupRequest
                                    addressSuggestions = emptyList()
                                    lookupStatus = "Suche passende Orte und Adressen …"
                                    DestinationLookup.search(this@MainActivity, query) { results ->
                                        if (requestId == lookupRequest) {
                                            addressSuggestions = results
                                            lookupStatus = if (results.isEmpty()) "Keine eindeutigen Treffer. Bitte den Suchtext ergänzen oder die Adresse selbst eingeben." else "Bitte den passenden Ort auswählen:"
                                        }
                                    }
                                }, enabled = confirmedDestination.isNotBlank() || !appointment.location.isNullOrBlank()) {
                                    Text("Adresse online suchen")
                                }
                                if (lookupStatus.isNotBlank()) Text(lookupStatus)
                                addressSuggestions.forEach { suggestion ->
                                    OutlinedButton(
                                        onClick = {
                                            confirmedDestination = suggestion
                                            addressSuggestions = emptyList()
                                            lookupStatus = "Ausgewähltes Ziel: $suggestion"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(suggestion) }
                                }
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
                        HorizontalDivider()
                        Text("Gehgeschwindigkeit", style = MaterialTheme.typography.titleMedium)
                        Text("%.1f km/h".format(java.util.Locale.GERMANY, walkingSpeed))
                        Slider(
                            value = walkingSpeed,
                            onValueChange = {
                                walkingSpeed = it
                                getPreferences(MODE_PRIVATE).edit().putFloat("walking_speed_kmh", it).apply()
                            },
                            valueRange = 2.0f..5.0f,
                            steps = 5
                        )
                        val destination = confirmedDestination.trim().ifBlank { appointment?.location?.trim().orEmpty() }
                        Button(
                            enabled = location != null && destination.isNotBlank() && !journeyLoading,
                            onClick = {
                                val start = location ?: return@Button
                                journeyLoading = true
                                journeys = emptyList()
                                routeStatus = "MVV-Verbindungen werden abgefragt …"
                                MvvJourneyRepository.search(start, destination, walkingSpeed) { result ->
                                    runOnUiThread {
                                        journeyLoading = false
                                        result.fold(
                                            onSuccess = {
                                                journeys = it
                                                routeStatus = if (it.isEmpty()) "Keine Verbindungen gefunden oder Antwortformat nicht unterstützt." else "${it.size} Verbindungen geladen (experimentell)"
                                            },
                                            onFailure = { routeStatus = "MVV-Abfrage fehlgeschlagen: ${it.localizedMessage ?: "Unbekannter Fehler"}" }
                                        )
                                    }
                                }
                            }
                        ) { Text(if (journeyLoading) "Suche läuft …" else "MVV-Verbindungen direkt anzeigen (Test)") }
                        journeys.sortedWith(compareBy<JourneyOption> { it.changes }.thenBy { it.walkingMinutes }).forEach { trip ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${trip.departure} – ${trip.arrival}", style = MaterialTheme.typography.titleMedium)
                                    Text(trip.summary)
                                    Text("${trip.changes} Umstiege · Fußweg geschätzt: ${trip.walkingMinutes} Min. bei %.1f km/h".format(java.util.Locale.GERMANY, walkingSpeed))
                                }
                            }
                        }
                        Text("Experimenteller MVV-Datenzugriff: Zeiten und Fußwege bitte mit der offiziellen MVV-Auskunft vergleichen. Die Fußweg-Schätzung berücksichtigt nur gemeldete Streckenlängen.")
                        Button(
                            enabled = location != null && destination.isNotBlank(),
                            onClick = {
                                val start = location ?: return@Button
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
                                    .appendQueryParameter("origin", "${start.latitude},${start.longitude}")
                                    .appendQueryParameter("destination", destination)
                                    .appendQueryParameter("travelmode", "transit")
                                    .build()
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    routeStatus = "ÖPNV-Routenplaner geöffnet"
                                } catch (_: android.content.ActivityNotFoundException) {
                                    routeStatus = "Kein Browser oder Routenplaner verfügbar"
                                }
                            }
                        ) { Text("ÖPNV-Route anzeigen") }
                        if (location == null) Text("Bitte zuerst den aktuellen Standort ermitteln.")
                        if (destination.isBlank()) Text("Bitte einen Zielort im Kalender ergänzen oder oben eingeben.")
                        if (routeStatus.isNotBlank()) Text(routeStatus)
                        Text("Die Route öffnet sich zunächst in Google Maps mit ÖPNV-Auswahl. Die Einstellung für 3,0 km/h wird in Mein Weg gespeichert, aber vom externen Routenplaner noch nicht berücksichtigt. Eine eigene MVV-Routenbewertung folgt.")
                    }
                }
            }
        }
    }
}
