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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import de.mvv.mobility.ui.components.SetupStepHeader
import de.mvv.mobility.ui.components.CockpitOverview
import de.mvv.mobility.ui.components.CockpitNavigation
import de.mvv.mobility.ui.components.CockpitTab
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.CurrentLocationRepository
import de.mvv.mobility.data.LocationFreshness
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
        val visualFixture = fixture && intent.getBooleanExtra("mein_weg_visual_fixture", false)
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
                    LaunchedEffect(Unit) {
                        if (!fixture && !allowed) permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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
                    var confirmedDestination by remember(appointment?.startMillis) { mutableStateOf(appointment?.location.orEmpty()) }
                    var addressSuggestions by remember(appointment?.startMillis) { mutableStateOf<List<String>>(emptyList()) }
                    var lookupStatus by remember(appointment?.startMillis) { mutableStateOf("") }
                    var lookupRequest by remember(appointment?.startMillis) { mutableIntStateOf(0) }
                    LaunchedEffect(appointment?.startMillis, appointment?.location) {
                        val calendarDestination = appointment?.location?.trim().orEmpty()
                        if (!fixture && calendarDestination.isNotBlank()) {
                            lookupRequest++
                            val requestId = lookupRequest
                            lookupStatus = "Adresse wird automatisch geprüft …"
                            addressSuggestions = emptyList()
                            DestinationLookup.search(this@MainActivity, calendarDestination) { results ->
                                if (lookupRequest == requestId) {
                                    when (results.size) {
                                        0 -> lookupStatus = "Adresse nicht eindeutig prüfbar – Kalenderziel bleibt erhalten."
                                        1 -> {
                                            confirmedDestination = results.first()
                                            lookupStatus = "✓ Adresse geprüft"
                                        }
                                        else -> {
                                            addressSuggestions = results
                                            lookupStatus = "Mehrere Adressen gefunden – bitte passenden Ort wählen."
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                    var searchGeneration by remember { mutableIntStateOf(0) }
                    val settings = remember { getPreferences(MODE_PRIVATE) }
                    var routeFilterMode by remember {
                        mutableStateOf(
                            runCatching {
                                RouteFilterMode.valueOf(settings.getString("route_filter_mode", RouteFilterMode.ANY.name) ?: RouteFilterMode.ANY.name)
                            }.getOrDefault(RouteFilterMode.ANY)
                        )
                    }
                    var routeFilterQuery by remember {
                        mutableStateOf(settings.getString("route_filter_query", "") ?: "")
                    }
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
                    var locationPermissionGranted by remember {
                        mutableStateOf(CurrentLocationRepository.hasPermission(this@MainActivity))
                    }
                    var appForeground by remember { mutableStateOf(false) }
                    var locationClock by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(appForeground) {
                        while (appForeground) {
                            locationClock = System.currentTimeMillis()
                            delay(15_000L)
                        }
                    }
                    val usableLocation = if (fixture) location else location?.takeIf {
                        LocationFreshness.isFresh(it.time, locationClock)
                    }
                    val effectiveLocationStatus = if (!fixture && location != null && usableLocation == null)
                        "Standort veraltet – bitte aktualisieren" else locationStatus
                    var locationRetry by remember { mutableIntStateOf(0) }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        appForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) appForeground = true
                            if (event == Lifecycle.Event.ON_STOP) appForeground = false
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        locationPermissionGranted = grants.values.any { it }
                        locationStatus = if (locationPermissionGranted) "Standort wird laufend ermittelt …"
                            else "Standortberechtigung nicht erteilt"
                    }
                    LaunchedEffect(Unit) {
                        if (!fixture && !locationPermissionGranted) {
                            locationStatus = "Standortfreigabe wird angefragt …"
                            locationPermissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                    }
                    DisposableEffect(appForeground, locationPermissionGranted, fixture, locationRetry) {
                        if (!fixture && appForeground && locationPermissionGranted) {
                            locationStatus = "Standort wird laufend ermittelt …"
                            val stop = CurrentLocationRepository.observeForeground(this@MainActivity) { found ->
                                if (found != null) {
                                    location = found
                                    locationClock = System.currentTimeMillis()
                                    locationStatus = if (LocationFreshness.isFresh(found.time, locationClock))
                                        "✓ Standort aktuell" else "Standort veraltet – bitte aktualisieren"
                                } else {
                                    location = null
                                    locationStatus = "Standort nicht verfügbar. Standortdienste prüfen."
                                }
                            }
                            onDispose { stop() }
                        } else {
                            onDispose { }
                        }
                    }
                    val appointmentAnchor = remember { BringIntoViewRequester() }
                    val connectionsAnchor = remember { BringIntoViewRequester() }
                    val journeyAnchor = remember { BringIntoViewRequester() }
                    var selectedTab by remember { mutableStateOf(CockpitTab.START) }
                    var activeSetupStep by remember { mutableIntStateOf(0) }
                    Column(Modifier.fillMaxSize().background(Color(0xFFF7F6FB))) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFF49309B), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("MEIN WEG 2.0 · MÜNCHEN", color = Color(0xFFD9D1FF),
                                    style = MaterialTheme.typography.labelSmall)
                                Text("Mein Weg", modifier = Modifier.testTag("app_title"), color = Color.White,
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            TextButton(onClick = { settingsExpanded = !settingsExpanded },
                                modifier = Modifier.testTag("settings_toggle")) {
                                Text(if (settingsExpanded) "✕" else "⚙", color = Color.White,
                                    style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        if (fixture && !visualFixture && !settingsExpanded) CockpitOverview(
                            appointment = appointment?.title,
                            destination = confirmedDestination.ifBlank { appointment?.location.orEmpty() },
                            journeyCount = journeys.size,
                            hasActiveJourney = dashboardJourney != null,
                            onAppointment = { selectedTab = CockpitTab.START },
                            onConnections = { selectedTab = CockpitTab.CONNECTIONS },
                            onJourney = { selectedTab = CockpitTab.JOURNEY }
                        )
                        if (settingsExpanded) {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("settings_panel"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Einstellungen", style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { settingsExpanded = false }) { Text("Schließen") }
                                    }
                                    Text("Persönliche Ziele", style = MaterialTheme.typography.titleMedium)
                                    Text("Zuhause", style = MaterialTheme.typography.labelLarge)
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
                                    HorizontalDivider()
                                    Text("Bevorzugte Linien", style = MaterialTheme.typography.titleMedium)
                                    Text("Die Auswahl filtert die gefundenen MVV-Verbindungen.", style = MaterialTheme.typography.bodySmall)
                                    RouteFilterControl(routeFilterMode, routeFilterQuery,
                                        onModeChange = {
                                            routeFilterMode = it
                                            settings.edit().putString("route_filter_mode", it.name).apply()
                                            journeys = emptyList()
                                        },
                                        onQueryChange = {
                                            routeFilterQuery = it
                                            settings.edit().putString("route_filter_query", it).apply()
                                            journeys = emptyList()
                                        })
                                    Text("Verkehrsmittel werden derzeit über die MVV-Verbindungsauswahl bestimmt; ein separater Modusfilter ist noch nicht verfügbar.", style = MaterialTheme.typography.bodySmall)
                                    HorizontalDivider()
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
                                    HorizontalDivider()
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
                                    HorizontalDivider()
                                    Text("Standort und Kalender", style = MaterialTheme.typography.titleMedium)
                                    Text(if (locationPermissionGranted) "✓ Standortberechtigung erteilt" else "Standortberechtigung fehlt", style = MaterialTheme.typography.bodySmall)
                                    Text(if (allowed) "✓ Kalenderberechtigung erteilt" else "Kalenderberechtigung fehlt", style = MaterialTheme.typography.bodySmall)
                                    Text("Standortdaten werden nur bei geöffneter App aktualisiert und nicht dauerhaft gespeichert.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (!settingsExpanded && (selectedTab == CockpitTab.START || (fixture && !visualFixture))) {
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
                        Spacer(Modifier.height(1.dp).bringIntoViewRequester(appointmentAnchor))
                        SetupStepHeader(1, "Termin", appointment?.title ?: "Termin auswählen", activeSetupStep == 0, "setup_step_appointment") { activeSetupStep = 0 }
                        if (activeSetupStep == 0 || (fixture && !visualFixture)) {
                        if (fixture) Text("TESTMODUS: simulierte Kalender-, GPS- und MVV-Daten", color = MaterialTheme.colorScheme.error)
                        if (!allowed && !fixture) {
                            Text("Damit Mein Weg deine Termine anzeigen kann, erlaube den Kalenderzugriff. Die Daten bleiben auf deinem Gerät.")
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                                Text("Kalenderzugriff erlauben")
                            }
                        } else {
                            Text("Nächster Termin", style = MaterialTheme.typography.titleMedium)
                            if (appointment == null) {
                                Text("Keine bevorstehenden Termine mit Uhrzeit gefunden.")
                            } else {
                                Card(
                                    onClick = { appointmentPickerExpanded = !appointmentPickerExpanded },
                                    modifier = Modifier.fillMaxWidth().testTag("calendar_appointment"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(appointment.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                            Text(appointment.timeLabel(), style = MaterialTheme.typography.bodySmall)
                                            Text(appointment.location ?: "Zieladresse ergänzen", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                        Text(if (appointmentPickerExpanded) "⌃" else "⌄", color = Color(0xFF5635B1))
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

                            }
                        }
                        if (!(fixture && !visualFixture)) Button(onClick = { activeSetupStep = 1 }, modifier = Modifier.fillMaxWidth().testTag("setup_next_address")) { Text("Weiter · Start & Ziel →") }
                        }
                        SetupStepHeader(2, "Start & Ziel", confirmedDestination.ifBlank { appointment?.location ?: "Adresse festlegen" }, activeSetupStep == 1, "setup_step_address") { activeSetupStep = 1 }
                        if (activeSetupStep == 1 || (fixture && !visualFixture)) {
                        Text("Wohin?", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = confirmedDestination,
                            onValueChange = { lookupRequest++; confirmedDestination = it; addressSuggestions = emptyList(); lookupStatus = "" },
                            label = { Text("Zieladresse") },
                            placeholder = { Text("Ort oder Adresse eingeben") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_destination_input")
                        )
                        TextButton(
                            onClick = {
                                val query = confirmedDestination.trim().ifBlank { appointment?.location.orEmpty() }
                                lookupRequest++
                                val requestId = lookupRequest
                                addressSuggestions = emptyList()
                                lookupStatus = "Suche passende Adressen …"
                                DestinationLookup.search(this@MainActivity, query) { results ->
                                    if (requestId == lookupRequest) {
                                        addressSuggestions = results
                                        lookupStatus = if (results.isEmpty()) "Kein eindeutiger Treffer. Adresse direkt verwenden oder präzisieren." else "Passenden Treffer antippen:"
                                    }
                                }
                            },
                            enabled = confirmedDestination.isNotBlank() || !appointment?.location.isNullOrBlank(),
                            modifier = Modifier.testTag("setup_destination_lookup")
                        ) { Text("⌕ Adresse erneut prüfen") }
                        if (lookupStatus.isNotBlank()) Text(lookupStatus, style = MaterialTheme.typography.bodySmall)
                        addressSuggestions.take(4).forEach { suggestion ->
                            TextButton(
                                onClick = {
                                    lookupRequest++
                                    confirmedDestination = suggestion
                                    addressSuggestions = emptyList()
                                    lookupStatus = "✓ Ziel übernommen"
                                },
                                modifier = Modifier.fillMaxWidth().testTag("setup_destination_suggestion")
                            ) { Text(suggestion) }
                        }
                        Text("Von wo?", style = MaterialTheme.typography.titleMedium)
                        Text(effectiveLocationStatus, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("setup_location_status"))
                        if (location == null && !fixture) {
                            TextButton(onClick = {
                                if (!CurrentLocationRepository.hasPermission(this@MainActivity)) {
                                    locationPermissionLauncher.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ))
                                } else {
                                    // Re-register the foreground subscription after a location-services change.
                                    locationRetry++
                                }
                            }, modifier = Modifier.testTag("setup_location_retry")) { Text("Standort erneut versuchen") }
                        }
                        Text("Standort wird bei geöffneter App ungefähr alle 5 Sekunden aktualisiert; keine Hintergrundverfolgung.", style = MaterialTheme.typography.bodySmall)
                        if (!(fixture && !visualFixture)) Button(onClick = { activeSetupStep = 2 }, modifier = Modifier.fillMaxWidth().testTag("setup_next_preferences")) { Text("Weiter · Fahrtwunsch →") }
                        }
                        SetupStepHeader(3, "Fahrtwunsch", "Verkehrsmittel & Ankunft", activeSetupStep == 2, "setup_step_preferences") { activeSetupStep = 2 }
                        if (activeSetupStep == 2 || (fixture && !visualFixture)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Verkehrsmittel", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { advancedExpanded = !advancedExpanded }) {
                                Text(if (advancedExpanded) "Weniger" else "Mehr Optionen")
                            }
                        }
                        RouteFilterControl(routeFilterMode, routeFilterQuery,
                            onModeChange = {
                                routeFilterMode = it
                                settings.edit().putString("route_filter_mode", it.name).apply()
                                journeys = emptyList()
                            },
                            onQueryChange = {
                                routeFilterQuery = it
                                settings.edit().putString("route_filter_query", it).apply()
                                journeys = emptyList()
                            })
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

                        }
                        Spacer(Modifier.height(1.dp).bringIntoViewRequester(connectionsAnchor))
                        }
                        val destination = confirmedDestination.trim().ifBlank { appointment?.location?.trim().orEmpty() }
                        LaunchedEffect(destination, appointment?.startMillis, arrivalBufferMinutes, routeFilterMode, routeFilterQuery, walkingSpeed) {
                            searchGeneration++
                            journeyLoading = false
                        }
                        val searchConnections: () -> Unit = {
                                val start = usableLocation
                                if (start != null) {
                                val arrivalBy = appointment?.startMillis?.minus(arrivalBufferMinutes * 60000L)
                                val requestId = ++searchGeneration
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
                                        if (requestId != searchGeneration) return@runOnUiThread
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
                        }

                        if (!settingsExpanded && selectedTab == CockpitTab.START && activeSetupStep == 2 && !(fixture && !visualFixture)) {
                            Button(
                                onClick = {
                                    selectedTab = CockpitTab.CONNECTIONS
                                    searchConnections()
                                },
                                enabled = usableLocation != null && destination.isNotBlank() && !journeyLoading &&
                                    (routeFilterMode == RouteFilterMode.ANY || routeFilterQuery.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("setup_find_connections"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5635B1)),
                                shape = RoundedCornerShape(18.dp)
                            ) { Text(if (journeyLoading) "Suche läuft …" else "Verbindung finden →") }
                        }
                        if (!settingsExpanded && (selectedTab == CockpitTab.CONNECTIONS || (fixture && !visualFixture))) {
                        Text("Verbindungen", style = MaterialTheme.typography.titleLarge)
                        Button(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("mvv_search_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5635B1)),
                            enabled = usableLocation != null && destination.isNotBlank() && !journeyLoading && (routeFilterMode == RouteFilterMode.ANY || routeFilterQuery.isNotBlank()),
                            onClick = searchConnections
                        ) { Text(if (journeyLoading) "Suche läuft …" else "Verbindung finden") }
                        mvvCandidates.forEach { candidate ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !journeyLoading,
                                onClick = {
                                    val start = usableLocation ?: return@OutlinedButton
                                    val requestId = ++searchGeneration
                                    journeyLoading = true
                                    mvvCandidates = emptyList()
                                    routeStatus = "Prüfe MVV-Haltestelle ${candidate.name} …"
                                    MvvJourneyRepository.search(start, destination, walkingSpeed, { result ->
                                        runOnUiThread {
                                            if (requestId != searchGeneration) return@runOnUiThread
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
                        Spacer(Modifier.height(1.dp).bringIntoViewRequester(journeyAnchor))
                        }
                        if (!settingsExpanded && (selectedTab == CockpitTab.JOURNEY || (fixture && !visualFixture))) {
                        Text("Meine Fahrt", style = MaterialTheme.typography.titleLarge)
                        if (dashboardJourney == null) TextButton(onClick = { selectedTab = CockpitTab.CONNECTIONS }) { Text("Verbindungen ansehen") }
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
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("walking_dashboard"),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECE7FF)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("MEINE FAHRT  ·  LIVE", color = Color(0xFF49309B), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Jetzt zur Haltestelle", color = Color(0xFF263B2E), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
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
                                                val start = usableLocation ?: return@Button
                                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
                                                    .appendQueryParameter("origin", "${start.latitude},${start.longitude}")
                                                    .appendQueryParameter("destination", boarding.from + ", München")
                                                    .appendQueryParameter("travelmode", "walking").build()
                                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }, modifier = Modifier.fillMaxWidth()) { Text("Fußweg und nächste Abbiegung öffnen") }
                                        }
                                        TextButton(onClick = { dashboardJourney = null; selectedTab = CockpitTab.CONNECTIONS }) { Text("Dashboard schließen") }
                                    }
                                }
                            }
                        }
                        }
                        if (!settingsExpanded && (selectedTab == CockpitTab.CONNECTIONS || (fixture && !visualFixture))) {
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = if (index == 0) Color(0xFFF0ECFF) else Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (index == 0) 4.dp else 2.dp)
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (index == 0) Text("ERSTE VERBINDUNG", color = Color(0xFF5635B1),
                                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("ABFAHRT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF62577D))
                                            Text(trip.departure, color = Color(0xFF35236D),
                                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                        }
                                        Text("→", color = Color(0xFF5635B1), style = MaterialTheme.typography.titleLarge)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("ANKUNFT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF62577D))
                                            Text(trip.arrival, color = Color(0xFF35236D),
                                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                                        }
                                    }
                                    if (firstBus != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF5635B1)
                                            ) {
                                                Text(
                                                    firstBus.line.ifBlank { "ÖPNV" },
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            Text(
                                                firstBus.from.ifBlank { "Einstiegshaltestelle unbekannt" },
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${trip.changes} Umstiege", style = MaterialTheme.typography.labelMedium)
                                        Text("Fußweg geschätzt: ca. ${trip.walkingMinutes} Min.",
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(trip.summary, modifier = Modifier.testTag("journey_summary"),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedButton(
                                        onClick = { dashboardJourney = index; selectedTab = CockpitTab.JOURNEY },
                                        modifier = Modifier.fillMaxWidth().testTag("open_walking_dashboard")
                                    ) { Text("Meine Fahrt öffnen") }
                                    if (leaveIn != null) {
                                        Text(if (leaveIn > 0) "Losgehen in ca. $leaveIn Minuten · ${trip.departure}"
                                            else if (leaveIn >= -5) "Jetzt losgehen · ${trip.departure}"
                                            else "Diese Verbindung liegt bereits in der Vergangenheit.",
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (firstBus != null && busIn != null && busIn > 0) {
                                        Text("Linie ${firstBus.line}: Abfahrt in ca. $busIn Minuten",
                                            style = MaterialTheme.typography.bodySmall)
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
                                                val start = usableLocation ?: return@OutlinedButton
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
                            enabled = usableLocation != null && destination.isNotBlank(),
                            onClick = {
                                val start = usableLocation ?: return@Button
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
                        if (usableLocation == null) Text("Bitte zuerst den aktuellen Standort ermitteln.")
                        if (destination.isBlank()) Text("Bitte einen Zielort im Kalender ergänzen oder oben eingeben.")
                        if (routeStatus.isNotBlank()) Text(routeStatus, modifier = Modifier.testTag("route_status"))
                        Text("Die Route öffnet sich zunächst in Google Maps mit ÖPNV-Auswahl. Die Einstellung für 3,0 km/h wird in Mein Weg gespeichert, aber vom externen Routenplaner noch nicht berücksichtigt. Eine eigene MVV-Routenbewertung folgt.")
                        }
                    }
                    CockpitNavigation(selectedTab) { tab -> selectedTab = tab }
                }
                }
            }
        }
    }
}
