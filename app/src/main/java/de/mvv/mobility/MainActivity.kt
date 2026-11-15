package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.CurrentLocationRepository
import de.mvv.mobility.data.DeviceCalendarRepository
import de.mvv.mobility.data.DestinationLookup
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.JourneyOption
import de.mvv.mobility.data.RouteFilterMode
import de.mvv.mobility.data.RoutePreference
import de.mvv.mobility.data.filterJourneys
import de.mvv.mobility.data.selectAppointmentJourneys
import de.mvv.mobility.data.UpcomingAppointment
import de.mvv.mobility.ui.components.ArrivalBufferControl
import de.mvv.mobility.ui.components.RouteFilterControl
import de.mvv.mobility.widget.JourneyWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fixture mode is strictly limited to debuggable APKs and is visibly labelled.
        val fixture = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
            intent.getBooleanExtra("mein_weg_test_fixture", false)
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
                    var visibleAppointments by remember { mutableIntStateOf(10) }
                    var appointmentPickerExpanded by remember { mutableStateOf(false) }
                    var advancedExpanded by remember { mutableStateOf(false) }
                    var selectedAppointmentStart by remember { mutableStateOf<Long?>(null) }
                    val fixtureBaseTime = remember { System.currentTimeMillis() }
                    val appointments = remember(allowed, refresh, visibleAppointments) {
                        if (fixture) (1..visibleAppointments).map { index ->
                            UpcomingAppointment("Testtermin $index", "Olympiapark München", fixtureBaseTime + index * 3600000L)
                        } else if (allowed) DeviceCalendarRepository.appointments(this, visibleAppointments) else emptyList()
                    }
                    val appointment = appointments.firstOrNull { it.startMillis == selectedAppointmentStart } ?: appointments.firstOrNull()
                    var confirmedDestination by remember(appointment?.startMillis) { mutableStateOf("") }
                    var addressSuggestions by remember(appointment?.startMillis) { mutableStateOf<List<String>>(emptyList()) }
                    var lookupStatus by remember(appointment?.startMillis) { mutableStateOf("") }
                    var lookupRequest by remember(appointment?.startMillis) { mutableIntStateOf(0) }
                    var walkingSpeed by remember { mutableFloatStateOf(getPreferences(MODE_PRIVATE).getFloat("walking_speed_kmh", 3.0f)) }
                    var routeStatus by remember { mutableStateOf("") }
                    var journeys by remember { mutableStateOf<List<JourneyOption>>(emptyList()) }
                    var expandedJourney by remember { mutableStateOf<Int?>(null) }
                    var dashboardJourney by remember { mutableStateOf<Int?>(null) }
                    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            clockMillis = System.currentTimeMillis()
                            delay(30000)
                        }
                    }
                    var journeyLoading by remember { mutableStateOf(false) }
                    var routeFilterMode by remember { mutableStateOf(RouteFilterMode.ANY) }
                    var routeFilterQuery by remember { mutableStateOf("") }
                    val settings = remember { getPreferences(MODE_PRIVATE) }
                    var settingsExpanded by remember { mutableStateOf(false) }
                    var homeAddress by remember { mutableStateOf(settings.getString("home_address", "") ?: "") }
                    var homeSaved by remember { mutableStateOf(homeAddress.isNotBlank()) }
                    var languageChoice by remember { mutableStateOf(settings.getString("language_choice", "system") ?: "system") }
                    var arrivalBufferMinutes by remember { mutableIntStateOf(settings.getInt("arrival_buffer_minutes", 10).coerceIn(5, 60)) }
                    var mvvCandidates by remember { mutableStateOf<List<MvvJourneyRepository.StopCandidate>>(emptyList()) }
                    var location by remember { mutableStateOf<Location?>(if (fixture) Location("test").apply {
                        latitude = 48.137154
                        longitude = 11.576124
                    } else null) }
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
                    Column(Modifier.fillMaxSize().background(Color(0xFFF7F6FB)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF49309B), RoundedCornerShape(24.dp))
                                .padding(horizontal = 22.dp, vertical = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("MEIN WEG 2.0  ·  MÜNCHEN", color = Color(0xFFD9D1FF), style = MaterialTheme.typography.labelMedium)
                            Text("Mein Weg", modifier = Modifier.testTag("app_title"), color = Color.White,
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
                            Text("Entspannt und pünktlich ans Ziel", color = Color(0xFFF0EBFF),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { settingsExpanded = !settingsExpanded },
                                modifier = Modifier.testTag("settings_toggle")) {
                                Text(if (settingsExpanded) "✕ Einstellungen schließen" else "⚙ Einstellungen")
                            }
                        }
                        if (settingsExpanded) {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("settings_panel"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Deine Einstellungen", style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                    Text("Zuhause", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(
                                        value = homeAddress,
                                        onValueChange = { homeAddress = it; homeSaved = false },
                                        label = { Text("Adresse oder Ort eingeben") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            settings.edit().putString("home_address", homeAddress.trim()).apply()
                                            homeSaved = homeAddress.isNotBlank()
                                        }, enabled = homeAddress.isNotBlank()) { Text("Zuhause speichern") }
                                        TextButton(onClick = {
                                            homeAddress = ""
                                            homeSaved = false
                                            settings.edit().remove("home_address").apply()
                                        }) { Text("Löschen") }
                                    }
                                    if (homeSaved) Text("Zuhause ist auf diesem Gerät gespeichert.")
                                    Text("Ankunftspuffer", style = MaterialTheme.typography.titleMedium)
                                    Text("$arrivalBufferMinutes Minuten vor dem Termin")
                                    Slider(
                                        value = arrivalBufferMinutes.toFloat(),
                                        onValueChange = {
                                            arrivalBufferMinutes = it.toInt().coerceIn(5, 60)
                                            settings.edit().putInt("arrival_buffer_minutes", arrivalBufferMinutes).apply()
                                            journeys = emptyList()
                                        },
                                        valueRange = 5f..60f, steps = 10
                                    )
                                    Text("Gehgeschwindigkeit", style = MaterialTheme.typography.titleMedium)
                                    Text("%.1f km/h".format(java.util.Locale.GERMANY, walkingSpeed))
                                    Slider(value = walkingSpeed, onValueChange = {
                                        walkingSpeed = it
                                        settings.edit().putFloat("walking_speed_kmh", it).apply()
                                        journeys = emptyList()
                                    }, valueRange = 2f..5f, steps = 5)
                                    Text("Sprache (Vorbereitung für Übersetzungen)",
                                        style = MaterialTheme.typography.titleMedium)
                                    listOf("system" to "Systemsprache", "de" to "Deutsch",
                                        "en" to "English", "fr" to "Français", "it" to "Italiano").forEach { (code, label) ->
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            RadioButton(selected = languageChoice == code, onClick = {
                                                languageChoice = code
                                                settings.edit().putString("language_choice", code).apply()
                                            })
                                            Text(label)
                                        }
                                    }
                                    Text("Die Sprachauswahl wird gespeichert. Weitere Übersetzungen der Oberfläche folgen.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (homeSaved) {
                            Button(
                                onClick = {
                                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
                                        .appendQueryParameter("destination", homeAddress.trim())
                                        .appendQueryParameter("travelmode", "transit").build()
                                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                modifier = Modifier.fillMaxWidth().testTag("navigate_home")
                            ) { Text("⌂ Nach Hause navigieren") }
                        }
                        if (fixture) Text("TESTMODUS: simulierte Kalender-, GPS- und MVV-Daten", color = MaterialTheme.colorScheme.error)
                        if (!allowed && !fixture) {
                            Text("Damit Mein Weg deine Termine anzeigen kann, erlaube den Kalenderzugriff. Die Daten bleiben auf deinem Gerät.")
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                                Text("Kalenderzugriff erlauben")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Nächster Termin", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    refresh++
                                    scope.launch { JourneyWidget().updateAll(this@MainActivity) }
                                }) { Text("Aktualisieren") }
                            }
                            if (appointment == null) {
                                Text("Keine bevorstehenden Termine mit Uhrzeit gefunden.")
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(appointment.title, style = MaterialTheme.typography.titleLarge)
                                        Text(appointment.timeLabel())
                                        Text(appointment.location ?: "Zieladresse bitte ergänzen")
                                        TextButton(onClick = { appointmentPickerExpanded = !appointmentPickerExpanded },
                                            modifier = Modifier.testTag("calendar_appointment")) {
                                            Text(if (appointmentPickerExpanded) "Terminauswahl schließen" else "Anderen Termin wählen")
                                        }
                                    }
                                }
                                if (appointmentPickerExpanded) {
                                    appointments.forEach { item ->
                                        OutlinedButton(modifier = Modifier.fillMaxWidth().testTag("calendar_appointment"), onClick = {
                                            selectedAppointmentStart = item.startMillis
                                            journeys = emptyList()
                                            mvvCandidates = emptyList()
                                            routeStatus = ""
                                            appointmentPickerExpanded = false
                                        }) { Text(item.timeLabel() + " · " + item.title + if (item.startMillis == appointment.startMillis) " ✓" else "") }
                                    }
                                    if (appointments.size == visibleAppointments) {
                                        OutlinedButton(modifier = Modifier.fillMaxWidth().testTag("calendar_more"), onClick = { visibleAppointments += 10 }) {
                                            Text("Weitere 10 Termine anzeigen")
                                        }
                                    }
                                }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Verkehrsmittel", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { advancedExpanded = !advancedExpanded }) {
                                Text(if (advancedExpanded) "Weniger" else "Mehr Optionen")
                            }
                        }
                        RouteFilterControl(routeFilterMode, routeFilterQuery,
                            onModeChange = { routeFilterMode = it; journeys = emptyList() },
                            onQueryChange = { routeFilterQuery = it; journeys = emptyList() })
                        if (advancedExpanded) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Gehgeschwindigkeit", style = MaterialTheme.typography.titleMedium)
                                    Text("%.1f km/h".format(java.util.Locale.GERMANY, walkingSpeed))
                                    Slider(value = walkingSpeed, onValueChange = {
                                        walkingSpeed = it
                                        getPreferences(MODE_PRIVATE).edit().putFloat("walking_speed_kmh", it).apply()
                                    }, valueRange = 2.0f..5.0f, steps = 5)
                                    Text("Standort wird nur auf Anfrage ermittelt, nicht dauerhaft verfolgt.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (appointment != null) {
                            HorizontalDivider()
                            Text("Ankunft zum Kalendertermin", style = MaterialTheme.typography.titleMedium)
                            Text("Terminbeginn: ${appointment.timeLabel()}")
                            ArrivalBufferControl(arrivalBufferMinutes) {
                                arrivalBufferMinutes = it
                                settings.edit().putInt("arrival_buffer_minutes", it).apply()
                                journeys = emptyList()
                            }
                            val arrivalTarget = java.util.Date(appointment.startMillis - arrivalBufferMinutes * 60000L)
                            val arrivalFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY)
                            arrivalFormat.timeZone = java.util.TimeZone.getTimeZone("Europe/Berlin")
                            Text("MVV-Suche: Ankunft spätestens ${arrivalFormat.format(arrivalTarget)}")
                        }
                        val destination = confirmedDestination.trim().ifBlank { appointment?.location?.trim().orEmpty() }
                        Button(
                            modifier = Modifier.testTag("mvv_search_button"),
                            enabled = location != null && destination.isNotBlank() && !journeyLoading && (routeFilterMode == RouteFilterMode.ANY || routeFilterQuery.isNotBlank()),
                            onClick = {
                                val start = location ?: return@Button
                                val arrivalBy = appointment?.startMillis?.minus(arrivalBufferMinutes * 60000L)
                                journeyLoading = true
                                journeys = emptyList()
                                mvvCandidates = emptyList()
                                routeStatus = "MVV-Ziel wird geprüft …"
                                if (fixture) {
                                    journeys = filterJourneys(MvvJourneyRepository.parseTrips("""{"trips":[{"legs":[{"origin":{"dateTime":{"hour":"09","minute":"10"}},"distance":300},{"transportation":{"number":"U3"},"destination":{"dateTime":{"hour":"09","minute":"30"}}}]}]}""", walkingSpeed), RoutePreference(routeFilterMode, routeFilterQuery)).take(3)
                                    journeyLoading = false
                                    routeStatus = "Testverbindung geladen"
                                } else MvvJourneyRepository.searchAddress(this@MainActivity, start, destination, walkingSpeed, callback = { result ->
                                    runOnUiThread {
                                        journeyLoading = false
                                        result.fold(
                                            onSuccess = {
                                                journeys = it.options
                                                routeStatus = if (it.options.isEmpty()) "Keine passende Verbindung innerhalb von zwei Stunden vor der gewünschten Ankunft gefunden." else "${it.options.size} passende Verbindungen nach ${it.destination.name} geladen"
                                            },
                                            onFailure = {
                                                if (it is MvvJourneyRepository.AmbiguousDestination) {
                                                    mvvCandidates = it.candidates
                                                    routeStatus = "Bitte die richtige MVV-Zielhaltestelle auswählen:"
                                                } else routeStatus = "MVV-Abfrage fehlgeschlagen: ${it.localizedMessage ?: "Unbekannter Fehler"}"
                                            }
                                        )
                                    }
                                }, preference = RoutePreference(routeFilterMode, routeFilterQuery), arrivalByMillis = arrivalBy)
                            }
                        ) { Text(if (journeyLoading) "Suche läuft …" else "Verbindung finden") }
                        mvvCandidates.forEach { candidate ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !journeyLoading,
                                onClick = {
                                    val start = location ?: return@OutlinedButton
                                    journeyLoading = true
                                    mvvCandidates = emptyList()
                                    routeStatus = "Prüfe MVV-Haltestelle ${candidate.name} …"
                                    MvvJourneyRepository.search(start, destination, walkingSpeed, { result ->
                                        runOnUiThread {
                                            journeyLoading = false
                                            result.fold(
                                                onSuccess = {
                                                    journeys = it.options
                                                    routeStatus = "${it.options.size} Verbindungen nach ${it.destination.name} geladen"
                                                },
                                                onFailure = { routeStatus = "MVV-Abfrage fehlgeschlagen: ${it.localizedMessage}" }
                                            )
                                        }
                                    }, selectedId = candidate.id)
                                }
                            ) { Text(candidate.name) }
                        }
                        if (dashboardJourney != null) {
                            val chosen = journeys.getOrNull(dashboardJourney!!)
                            if (chosen != null) {
                                val boarding = chosen.steps.firstOrNull { !it.walking }
                                val firstWalk = chosen.steps.takeWhile { it.walking }
                                val walkMinutes = firstWalk.sumOf { it.minutes }
                                val approximateMeters = (walkMinutes * walkingSpeed * 1000f / 60f).toInt()
                                val targetDay = appointment?.startMillis?.minus(arrivalBufferMinutes * 60000L)
                                fun countdown(clock: String): Long? {
                                    if (targetDay == null || !Regex("""\d{1,2}:\d{2}""").matches(clock)) return null
                                    val parts = clock.split(":")
                                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                                    cal.timeInMillis = targetDay
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(java.util.Calendar.MINUTE, parts[1].toInt())
                                    cal.set(java.util.Calendar.SECOND, 0)
                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                    if (cal.timeInMillis > targetDay + 30 * 60000L) cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                                    return (cal.timeInMillis - clockMillis) / 60000L
                                }
                                Card(Modifier.fillMaxWidth().testTag("walking_dashboard")) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Meine Fahrt · Dein Wegbegleiter", style = MaterialTheme.typography.titleMedium)
                                        Text("Jetzt zur Haltestelle", style = MaterialTheme.typography.headlineSmall)
                                        Text(boarding?.from?.ifBlank { "Haltestelle noch nicht übermittelt" } ?: "Kein Verkehrsmittel in dieser Verbindung")
                                        if (boarding != null) {
                                            Text("Linie ${boarding.line} · Abfahrt ${boarding.departure}", style = MaterialTheme.typography.titleLarge)
                                            val untilBus = countdown(boarding.departure)
                                            if (untilBus != null) Text(if (untilBus >= 0) "Bus/Bahn fährt in ca. $untilBus Min." else "Geplante Abfahrt ist vorbei.",
                                                style = MaterialTheme.typography.headlineSmall)
                                        }
                                        val untilLeave = countdown(chosen.departure)
                                        if (untilLeave != null) Text(if (untilLeave > 0) "Losgehen in ca. $untilLeave Min." else "Abfahrtszeit für den Fußweg erreicht")
                                        if (walkMinutes > 0) {
                                            Text("Fußweg zum Einstieg: ca. $walkMinutes Min. · grob $approximateMeters m (aus Gehzeit geschätzt)")
                                        }
                                        Text("Nächste Etappe: ${firstWalk.firstOrNull()?.from?.ifBlank { "Startpunkt" } ?: "Startpunkt"} → ${boarding?.from?.ifBlank { "Einstieg" } ?: "Ziel"}")
                                        Text("Für echte Abbiegungen und die verbleibenden Meter bitte die Fußweg-Navigation öffnen. Dieses Dashboard verfolgt deinen Standort nicht automatisch.")
                                        if (boarding?.from?.isNotBlank() == true && location != null) {
                                            Button(onClick = {
                                                val start = location ?: return@Button
                                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
                                                    .appendQueryParameter("origin", "${start.latitude},${start.longitude}")
                                                    .appendQueryParameter("destination", boarding.from + ", München")
                                                    .appendQueryParameter("travelmode", "walking").build()
                                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }, modifier = Modifier.fillMaxWidth()) { Text("Fußweg und nächste Abbiegung öffnen") }
                                        }
                                        TextButton(onClick = { dashboardJourney = null }) { Text("Dashboard schließen") }
                                    }
                                }
                            }
                        }
                        journeys.forEachIndexed { index, trip ->
                            val firstBus = trip.steps.firstOrNull { !it.walking }
                            val targetDay = appointment?.startMillis?.minus(arrivalBufferMinutes * 60000L)
                            fun minutesUntil(clock: String): Long? {
                                if (targetDay == null || !Regex("""\d{1,2}:\d{2}""").matches(clock)) return null
                                val parts = clock.split(":")
                                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                                cal.timeInMillis = targetDay
                                cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                                cal.set(java.util.Calendar.MINUTE, parts[1].toInt())
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                if (cal.timeInMillis > targetDay + 30 * 60000L) cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                                return (cal.timeInMillis - clockMillis) / 60000L
                            }
                            val leaveIn = minutesUntil(trip.departure)
                            val busIn = firstBus?.let { minutesUntil(it.departure) }
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${trip.departure} – ${trip.arrival}", style = MaterialTheme.typography.titleLarge)
                                    Text(trip.summary, modifier = Modifier.testTag("journey_summary"), color = MaterialTheme.colorScheme.primary)
                                    OutlinedButton(onClick = { dashboardJourney = index }, modifier = Modifier.testTag("open_walking_dashboard")) { Text("Meine Fahrt · Dashboard öffnen") }
                                    Text("${trip.changes} Umstiege · Fußweg geschätzt: ${trip.walkingMinutes} Min. bei %.1f km/h".format(java.util.Locale.GERMANY, walkingSpeed))
                                    if (leaveIn != null) {
                                        Text(if (leaveIn > 0) "Losgehen in ca. $leaveIn Minuten · ${trip.departure}"
                                            else if (leaveIn >= -5) "Jetzt losgehen · ${trip.departure}"
                                            else "Diese Verbindung liegt bereits in der Vergangenheit.",
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (firstBus != null) {
                                        Text("Einsteigen: ${firstBus.line} · ${firstBus.from.ifBlank { "Haltestelle nicht übermittelt" }} · ${firstBus.departure}")
                                        if (busIn != null && busIn > 0) Text("Abfahrt in ca. $busIn Minuten")
                                    }
                                    TextButton(onClick = { expandedJourney = if (expandedJourney == index) null else index },
                                        modifier = Modifier.testTag("journey_details")) {
                                        Text(if (expandedJourney == index) "Details schließen" else "Wegbeschreibung und Haltestellen")
                                    }
                                    if (expandedJourney == index) {
                                        trip.steps.forEachIndexed { stepIndex, step ->
                                            Text("${stepIndex + 1}. " + if (step.walking) "Zu Fuß" else "Linie ${step.line}")
                                            Text("${step.from.ifBlank { "Startpunkt" }} → ${step.to.ifBlank { "Zielpunkt" }}")
                                            Text("${step.departure} – ${step.arrival}" + if (step.walking && step.minutes > 0) " · ca. ${step.minutes} Min." else "")
                                        }
                                        if (firstBus?.from?.isNotBlank() == true && location != null) {
                                            OutlinedButton(onClick = {
                                                val start = location ?: return@OutlinedButton
                                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
                                                    .appendQueryParameter("origin", "${start.latitude},${start.longitude}")
                                                    .appendQueryParameter("destination", firstBus.from + ", München")
                                                    .appendQueryParameter("travelmode", "walking").build()
                                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }) { Text("Fußweg zur Einstiegshaltestelle öffnen") }
                                        }
                                        Text("Zeiten und Haltestellen aus MVV; die Fußweg-Navigation erfolgt in Google Maps.")
                                    }

                                }
                            }
                        }
                        Text("MVV-Zeiten können sich ändern. Gehzeiten sind Schätzungen.", style = MaterialTheme.typography.bodySmall)
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
                        if (routeStatus.isNotBlank()) Text(routeStatus, modifier = Modifier.testTag("route_status"))
                        Text("Die Route öffnet sich zunächst in Google Maps mit ÖPNV-Auswahl. Die Einstellung für 3,0 km/h wird in Mein Weg gespeichert, aber vom externen Routenplaner noch nicht berücksichtigt. Eine eigene MVV-Routenbewertung folgt.")
                    }
                }
            }
        }
    }
}
