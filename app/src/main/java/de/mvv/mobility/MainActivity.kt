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
                    var arrivalBufferMinutes by remember { mutableIntStateOf(10) }
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
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Mein Weg", modifier = Modifier.testTag("app_title"), style = MaterialTheme.typography.headlineMedium)
                        Text("Dein Weg zum nächsten Termin", color = MaterialTheme.colorScheme.primary)
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
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = routeFilterMode == RouteFilterMode.ANY,
                                onClick = { routeFilterMode = RouteFilterMode.ANY; journeys = emptyList() },
                                label = { Text("Alle") }, modifier = Modifier.testTag("route_filter_mode_ANY"))
                            FilterChip(selected = routeFilterMode == RouteFilterMode.ONLY_LINE_WITH_WALK,
                                onClick = { routeFilterMode = RouteFilterMode.ONLY_LINE_WITH_WALK; journeys = emptyList() },
                                label = { Text("Nur Linie") }, modifier = Modifier.testTag("route_filter_mode_ONLY_LINE_WITH_WALK"))
                            FilterChip(selected = routeFilterMode == RouteFilterMode.CONTAINS,
                                onClick = { routeFilterMode = RouteFilterMode.CONTAINS; journeys = emptyList() },
                                label = { Text("Mit Linie") }, modifier = Modifier.testTag("route_filter_mode_CONTAINS"))
                        }
                        if (routeFilterMode != RouteFilterMode.ANY) {
                            OutlinedTextField(value = routeFilterQuery, onValueChange = { routeFilterQuery = it; journeys = emptyList() },
                                label = { Text("Linie, z. B. 134 oder U3") },
                                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("route_filter_query"))
                        }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Ankunftspuffer", modifier = Modifier.weight(1f))
                                Text("$arrivalBufferMinutes Min. früher", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium)
                            }
                            Slider(value = arrivalBufferMinutes.toFloat(), onValueChange = {
                                val updated = (it / 5f).toInt().coerceIn(1, 12) * 5
                                if (updated != arrivalBufferMinutes) {
                                    arrivalBufferMinutes = updated
                                    journeys = emptyList()
                                }
                            }, valueRange = 5f..60f, steps = 10,
                                modifier = Modifier.testTag("arrival_buffer_slider"))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("5 Min.", style = MaterialTheme.typography.labelSmall)
                                Text("30 Min.", style = MaterialTheme.typography.labelSmall)
                                Text("60 Min.", style = MaterialTheme.typography.labelSmall)
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
