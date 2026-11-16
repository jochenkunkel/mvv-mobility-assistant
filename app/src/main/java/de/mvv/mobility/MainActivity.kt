package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.view.View
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
import de.mvv.mobility.data.routeFailureMessage
import de.mvv.mobility.data.recommendedLeaveMillis
import de.mvv.mobility.data.WalkingNavigation
import de.mvv.mobility.data.WalkingRoute
import de.mvv.mobility.data.DeviceCalendarRepository
import de.mvv.mobility.data.DestinationLookup
import de.mvv.mobility.data.GpsDebugRecorder
import de.mvv.mobility.data.DebugResponseCapture
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.isCurrentSearchResponse
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
        // Keep the system status bar separate from the app header on edge-to-edge Android.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(248, 247, 252)
        }
        // Fixture mode is strictly limited to debuggable APKs and is visibly labelled.
        val fixture = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
            intent.getBooleanExtra("mein_weg_test_fixture", false)
        val visualFixture = fixture && intent.getBooleanExtra("mein_weg_visual_fixture", false)
        val debugApk = BuildConfig.DEBUG && BuildConfig.ENABLE_MVV_DEBUG_EXPORT
        DebugResponseCapture.enabled = debugApk
        DebugResponseCapture.clear()
        val gpsDebugEnabled = BuildConfig.DEBUG && BuildConfig.ENABLE_GPS_DEBUG_EXPORT
        GpsDebugRecorder.enabled = gpsDebugEnabled
        GpsDebugRecorder.clear()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    var debugExportStatus by remember { mutableStateOf("") }
                    val debugExportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/json")
                    ) { uri ->
                        if (uri != null && debugApk) {
                            val payload = DebugResponseCapture.snapshot()
                            if (payload != null) {
                                runCatching {
                                    contentResolver.openOutputStream(uri)?.use { stream ->
                                        stream.write(payload.toByteArray(Charsets.UTF_8))
                                    } ?: error("Datei konnte nicht geöffnet werden")
                                }.onSuccess { debugExportStatus = "MVV-Debugantwort gespeichert." }
                                    .onFailure { debugExportStatus = "Export fehlgeschlagen: ${it.localizedMessage}" }
                            } else debugExportStatus = "Keine MVV-Antwort vorhanden."
                        }
                    }
                    var gpsExportStatus by remember { mutableStateOf("") }
                    val gpsExportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("application/json")
                    ) { uri ->
                        if (uri != null && gpsDebugEnabled) {
                            val payload = GpsDebugRecorder.snapshot()
                            if (payload != null) {
                                runCatching {
                                    contentResolver.openOutputStream(uri)?.use {
                                        it.write(payload.toByteArray(Charsets.UTF_8))
                                    } ?: error("Datei konnte nicht geöffnet werden")
                                }.onSuccess { gpsExportStatus = "GPS-Protokoll gespeichert." }
                                    .onFailure { gpsExportStatus = "GPS-Export fehlgeschlagen: ${it.localizedMessage}" }
                            }
                        }
                    }
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
                    var arrivalBufferMinutes by remember { mutableIntStateOf(getPreferences(MODE_PRIVATE).getInt("arrival_buffer_minutes", 10).coerceIn(5, 60)) }
                    // The calendar must never influence "Jetzt" or "Geplant".
                    var journeyTimingMode by remember { mutableStateOf<String?>(null) }
                    var plannedArrival by remember { mutableStateOf(false) }
                    var plannedAtMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60000L) }
                    val appointment = if (journeyTimingMode == "calendar")
                        (appointments.firstOrNull { it.startMillis == selectedAppointmentStart } ?: appointments.firstOrNull())
                        else null
                    val plannedCalendar = java.util.Calendar.getInstance().apply { timeInMillis = plannedAtMillis }
                    val plannedLabel = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY)
                        .format(java.util.Date(plannedAtMillis))
                    val searchTimeMillis: Long? = when (journeyTimingMode) {
                        "calendar" -> appointment?.startMillis?.minus(arrivalBufferMinutes * 60000L)
                        "planned" -> plannedAtMillis
                        else -> null
                    }
                    var confirmedDestination by remember { mutableStateOf("") }
                    var addressSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
                    var lookupStatus by remember { mutableStateOf("") }
                    var lookupRequest by remember { mutableIntStateOf(0) }
                    LaunchedEffect(journeyTimingMode, appointment?.startMillis, appointment?.location) {
                        val calendarDestination = appointment?.location?.trim().orEmpty()
                        if (!fixture && journeyTimingMode == "calendar" && calendarDestination.isNotBlank()) {
                            confirmedDestination = calendarDestination
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
                    var walkingRoute by remember { mutableStateOf<WalkingRoute?>(null) }
                    var walkingOnlyFallback by remember { mutableStateOf(false) }
                    var walkingOnlyDashboard by remember { mutableStateOf(false) }
                    var walkingRouteStatus by remember { mutableStateOf("") }
                    var walkingNavigationOpen by remember { mutableStateOf(false) }
                    var walkingRequestId by remember { mutableIntStateOf(0) }
                    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            clockMillis = System.currentTimeMillis()
                            delay(5_000L)
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
                    // Retain the last accepted fix for display, but never treat an old
                    // position as live navigation input.
                    val usableLocation = if (fixture) location else location?.takeIf {
                        LocationFreshness.isFresh(it.time, locationClock)
                    }
                    val navigationLocation = usableLocation?.takeIf {
                        fixture || (locationClock - it.time <= 45_000L &&
                            (!it.hasAccuracy() || it.accuracy <= 80f))
                    }
                    val locationAgeSeconds = location?.takeIf { !fixture && it.time > 0L }
                        ?.let { ((locationClock - it.time).coerceAtLeast(0L) / 1000L) }
                    LaunchedEffect(gpsDebugEnabled, appForeground, walkingNavigationOpen, walkingOnlyDashboard) {
                        if (gpsDebugEnabled && appForeground) {
                            while (true) {
                                val fix = location
                                val now = System.currentTimeMillis()
                                val age = fix?.let { now - it.time }
                                val state = when {
                                    fix == null -> "no_fix"
                                    age == null || age < 0 -> "invalid_time"
                                    age <= 15_000L && (!fix.hasAccuracy() || fix.accuracy <= 80f) -> "live"
                                    age <= 45_000L && (!fix.hasAccuracy() || fix.accuracy <= 80f) -> "recent_not_live"
                                    age <= LocationFreshness.MAX_AGE_MILLIS -> "not_navigation_fresh"
                                    else -> "stale"
                                }
                                GpsDebugRecorder.event("navigation_heartbeat", fix, state,
                                    walkingNavigationOpen || walkingOnlyDashboard, age)
                                delay(5_000L)
                            }
                        }
                    }
                    val effectiveLocationStatus = if (!fixture && location != null && usableLocation == null)
                        "Standort wird automatisch neu ermittelt …" else locationStatus
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        appForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) {
                                locationPermissionGranted = CurrentLocationRepository.hasPermission(this@MainActivity)
                                appForeground = true
                            }
                            if (event == Lifecycle.Event.ON_STOP) appForeground = false
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    var locationPromptFinished by remember {
                        mutableStateOf(fixture || locationPermissionGranted)
                    }
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        locationPermissionGranted = grants.values.any { it }
                        locationPromptFinished = true
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
                    // Android only displays one runtime permission dialog reliably at a time.
                    // Request calendar access after the initial location decision.
                    LaunchedEffect(locationPromptFinished, allowed) {
                        if (!fixture && locationPromptFinished && !allowed) {
                            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }
                    DisposableEffect(appForeground, locationPermissionGranted, fixture) {
                        if (!fixture && appForeground && locationPermissionGranted) {
                            locationStatus = "Standort wird laufend ermittelt …"
                            val stop = CurrentLocationRepository.observeForeground(this@MainActivity) { found ->
                                if (gpsDebugEnabled) {
                                    val now = System.currentTimeMillis()
                                    val previous = location
                                    val reason = when {
                                        found == null -> "provider_no_fix"
                                        !LocationFreshness.isFresh(found.time, now) -> "stale_or_future"
                                        previous != null && found.time <= previous.time -> "out_of_order"
                                        found.hasAccuracy() && found.accuracy > 100f -> "accuracy_over_100m"
                                        else -> "accepted"
                                    }
                                    GpsDebugRecorder.event("provider_update", found, reason,
                                        walkingNavigationOpen || walkingOnlyDashboard,
                                        found?.let { now - it.time })
                                }

                                if (found != null) {
                                    val now = System.currentTimeMillis()
                                    val previous = location
                                    // Ignore stale, out-of-order or implausibly inaccurate fixes.
                                    // Do not overwrite a good fix with an older cached result.
                                    val acceptable = LocationFreshness.isFresh(found.time, now) &&
                                        (previous == null || found.time > previous.time) &&
                                        (!found.hasAccuracy() || found.accuracy <= 100f)
                                    if (acceptable) {
                                        location = found
                                        locationClock = now
                                        locationStatus = "✓ Standort aktualisiert"
                                    } else {
                                        locationStatus = "GPS-Messung verworfen – letzte Position bleibt erhalten"
                                    }
                                } else {
                                    // Keep last fix through brief provider gaps; freshness still expires it.
                                    locationStatus = "GPS-Signal unterbrochen – letzte Position wird geprüft"
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
                                .background(Color(0xFF49309B), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("MEIN WEG 2.0 · MÜNCHEN", color = Color(0xFFD9D1FF),
                                    style = MaterialTheme.typography.labelSmall)
                                Text("Mein Weg", modifier = Modifier.testTag("app_title"), color = Color.White,
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = { settingsExpanded = !settingsExpanded },
                                modifier = Modifier.testTag("settings_toggle")) {
                                Text(if (settingsExpanded) "✕" else "⚙", color = Color.White,
                                    style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (!fixture && !settingsExpanded) {
                            Text(
                                if (usableLocation != null) "● Standort aktiv · automatisch für Verbindungen"
                                else if (!locationPermissionGranted) "○ Standortfreigabe in Android erforderlich"
                                else "○ Standort wird automatisch ermittelt",
                                modifier = Modifier.fillMaxWidth().testTag("global_location_status"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (usableLocation != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        if (homeSaved && journeyTimingMode != null) {
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
                        SetupStepHeader(1, "Wann fahren?", when (journeyTimingMode) {
                            "now" -> "Jetzt"
                            "planned" -> "Geplant · $plannedLabel"
                            "calendar" -> appointment?.title ?: "Kalendertermin auswählen"
                            else -> "Jetzt, Geplant oder Kalender auswählen"
                        }, activeSetupStep == 0, "setup_step_appointment") { activeSetupStep = 0 }
                        if (activeSetupStep == 0 || (fixture && !visualFixture)) {
                        if (fixture) Text("TESTMODUS: simulierte Kalender-, GPS- und MVV-Daten", color = MaterialTheme.colorScheme.error)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("now" to "Jetzt", "planned" to "Geplant", "calendar" to "Kalender").forEach { (mode, title) ->
                                val selected = journeyTimingMode == mode
                                OutlinedButton(
                                    onClick = {
                                        if (journeyTimingMode != mode) {
                                            journeyTimingMode = mode
                                            selectedAppointmentStart = null
                                            confirmedDestination = ""
                                            lookupRequest++
                                            lookupStatus = ""
                                            addressSuggestions = emptyList()
                                            journeys = emptyList()
                                            dashboardJourney = null
                                            walkingOnlyFallback = false
                                            walkingOnlyDashboard = false
                                            walkingRoute = null
                                            routeStatus = ""
                                            activeSetupStep = 0
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("timing_$mode"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) Color(0xFFE6DFFF) else Color.White
                                    )
                                ) { Text(title, maxLines = 1) }
                            }
                        }
                        if (journeyTimingMode == "now") {
                            Text("Sofort starten. Ziel im nächsten Schritt wählen – ohne Kalendertermin.")
                        }
                        if (journeyTimingMode == "planned") {
                            Text("Datum und Uhrzeit: $plannedLabel", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    android.app.DatePickerDialog(this@MainActivity,
                                        { _, year, month, day ->
                                            val updated = java.util.Calendar.getInstance().apply {
                                                timeInMillis = plannedAtMillis
                                                set(year, month, day)
                                            }
                                            plannedAtMillis = updated.timeInMillis
                                        },
                                        plannedCalendar.get(java.util.Calendar.YEAR),
                                        plannedCalendar.get(java.util.Calendar.MONTH),
                                        plannedCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) { Text("Datum") }
                                OutlinedButton(onClick = {
                                    android.app.TimePickerDialog(this@MainActivity,
                                        { _, hour, minute ->
                                            val updated = java.util.Calendar.getInstance().apply {
                                                timeInMillis = plannedAtMillis
                                                set(java.util.Calendar.HOUR_OF_DAY, hour)
                                                set(java.util.Calendar.MINUTE, minute)
                                                set(java.util.Calendar.SECOND, 0)
                                            }
                                            plannedAtMillis = updated.timeInMillis
                                        },
                                        plannedCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                                        plannedCalendar.get(java.util.Calendar.MINUTE), true
                                    ).show()
                                }) { Text("Uhrzeit") }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = !plannedArrival, onClick = { plannedArrival = false })
                                Text("Abfahrt")
                                RadioButton(selected = plannedArrival, onClick = { plannedArrival = true })
                                Text("Ankunft")
                            }
                        }
                        if (journeyTimingMode == "calendar") {
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
                        }
                        if (journeyTimingMode != null && (journeyTimingMode != "calendar" || appointment != null) && !(fixture && !visualFixture)) {
                            Button(onClick = { activeSetupStep = 1 }, modifier = Modifier.fillMaxWidth().testTag("setup_next_address")) {
                                Text("Weiter · Start & Ziel →")
                            }
                        }
                        }
                        if (journeyTimingMode != null) {
                        SetupStepHeader(2, "Start & Ziel", if (usableLocation == null) "Standort fehlt · Route noch nicht bereit" else confirmedDestination.ifBlank { appointment?.location ?: "Adresse festlegen" }, activeSetupStep == 1, "setup_step_address") { activeSetupStep = 1 }
                        if (activeSetupStep == 1 || (fixture && !visualFixture)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Wohin?", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = {
                                    val home = homeAddress.trim()
                                    lookupRequest++
                                    val requestId = lookupRequest
                                    confirmedDestination = home
                                    addressSuggestions = emptyList()
                                    lookupStatus = "Zuhause wird automatisch geprüft …"
                                    DestinationLookup.search(this@MainActivity, home) { results ->
                                        if (requestId == lookupRequest && confirmedDestination == home) {
                                            when (results.size) {
                                                0 -> lookupStatus = "Zuhause konnte nicht eindeutig geprüft werden. Bitte Adresse kontrollieren."
                                                1 -> {
                                                    confirmedDestination = results.first()
                                                    lookupStatus = "✓ Zuhause automatisch geprüft"
                                                }
                                                else -> {
                                                    addressSuggestions = results
                                                    lookupStatus = "Mehrere Adressen gefunden – bitte passende auswählen."
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = homeAddress.isNotBlank(),
                                modifier = Modifier.testTag("setup_destination_home")
                            ) { Text("⌂ Zuhause") }
                        }
                        OutlinedTextField(
                            value = confirmedDestination,
                            onValueChange = { lookupRequest++; confirmedDestination = it; addressSuggestions = emptyList(); lookupStatus = "" },
                            label = { Text("Zieladresse") },
                            placeholder = { Text("Ort oder Adresse eingeben") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_destination_input")
                        )
                        Row(Modifier.fillMaxWidth().testTag("destination_status_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (lookupStatus.startsWith("✓")) "✓" else "?",
                                    color = if (lookupStatus.startsWith("✓")) Color(0xFF237A45)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold)
                                Text(if (lookupStatus.startsWith("✓")) "Ziel geprüft"
                                    else if (confirmedDestination.isNotBlank()) "Ziel eingetragen"
                                    else "Ziel fehlt", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = {
                                    val query = confirmedDestination.trim().ifBlank { appointment?.location.orEmpty() }
                                    lookupRequest++
                                    val requestId = lookupRequest
                                    addressSuggestions = emptyList()
                                    lookupStatus = "Adresse wird geprüft …"
                                    DestinationLookup.search(this@MainActivity, query) { results ->
                                        if (requestId == lookupRequest) {
                                            addressSuggestions = results
                                            lookupStatus = if (results.isEmpty()) "Kein eindeutiger Treffer"
                                                else "Treffer auswählen"
                                        }
                                    }
                                },
                                enabled = confirmedDestination.isNotBlank() || !appointment?.location.isNullOrBlank(),
                                modifier = Modifier.testTag("setup_destination_lookup")
                            ) { Text("Prüfen ↻", style = MaterialTheme.typography.labelMedium) }
                        }
                        if (lookupStatus.isNotBlank() && !lookupStatus.startsWith("✓")) {
                            Text(lookupStatus, style = MaterialTheme.typography.labelSmall)
                        }
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
                        Row(Modifier.fillMaxWidth().testTag("setup_location_status"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Start · GPS", style = MaterialTheme.typography.titleMedium)
                            Text(if (usableLocation != null) "✓ Bereit"
                                else if (!locationPermissionGranted) "✕ Freigabe fehlt"
                                else "◌ Suche …",
                                color = if (usableLocation != null) Color(0xFF237A45)
                                    else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        if (!locationPermissionGranted && !fixture) {
                            Text("Standortzugriff in Android für Mein Weg erlauben. Ohne Standort ist keine Verbindungssuche möglich.",
                                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!(fixture && !visualFixture)) Button(onClick = { activeSetupStep = 2 }, modifier = Modifier.fillMaxWidth().testTag("setup_next_preferences")) { Text("Weiter · Fahrtwunsch →") }
                        }
                        SetupStepHeader(3, "Fahrtwunsch", "Verkehrsmittel & Ankunft", activeSetupStep == 2, "setup_step_preferences") { activeSetupStep = 2 }
                        if (activeSetupStep == 2 || (fixture && !visualFixture)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Verkehrsmittel", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { advancedExpanded = !advancedExpanded },
                                modifier = Modifier.testTag("advanced_options_toggle")) {
                                Text(if (advancedExpanded) "Optionen ▲" else "Optionen ▼",
                                    style = MaterialTheme.typography.labelMedium)
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
                                    Text("Standort wird bei geöffneter App automatisch aktualisiert.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (journeyTimingMode == "calendar" && appointment != null) {
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
                        }
                        val destination = confirmedDestination.trim().ifBlank { if (journeyTimingMode == "calendar") appointment?.location?.trim().orEmpty() else "" }
                        LaunchedEffect(destination, journeyTimingMode, searchTimeMillis, plannedArrival, arrivalBufferMinutes, routeFilterMode, routeFilterQuery, walkingSpeed) {
                            searchGeneration++
                            journeyLoading = false
                        }
                        val startWalkingFallback: (String) -> Unit = { reason ->
                            val start = usableLocation
                            if (start != null && destination.isNotBlank()) {
                                walkingOnlyFallback = true
                                walkingNavigationOpen = true
                                walkingRoute = null
                                val walkingId = ++walkingRequestId
                                walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                routeStatus = reason
                                WalkingNavigation.fetch(this@MainActivity, start, destination) { walkResult ->
                                    if (walkingId == walkingRequestId && walkingOnlyFallback) {
                                        walkingRoute = walkResult.getOrNull()
                                        walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage ?: ""
                                    }
                                }
                            } else {
                                routeStatus = reason + " Für Fußnavigation werden GPS und Ziel benötigt."
                            }
                        }
                        val searchConnections: () -> Unit = {
                                val start = usableLocation
                                if (start != null) {
                                val arrivalBy = if (journeyTimingMode == "planned" && !plannedArrival) null else searchTimeMillis
                                val requestId = ++searchGeneration
                                journeyLoading = true
                                journeys = emptyList()
                                mvvCandidates = emptyList()
                                walkingOnlyFallback = false
                                walkingNavigationOpen = false
                                routeStatus = "MVV-Ziel wird geprüft …"
                                if (fixture) {
                                    journeys = filterJourneys(MvvJourneyRepository.parseTrips("""{"trips":[{"legs":[{"origin":{"dateTime":{"hour":"09","minute":"10"}},"distance":300},{"transportation":{"number":"U3"},"destination":{"dateTime":{"hour":"09","minute":"30"}}}]}]}""", walkingSpeed), RoutePreference(routeFilterMode, routeFilterQuery)).take(3)
                                    journeyLoading = false
                                    routeStatus = "Testverbindung geladen"
                                } else MvvJourneyRepository.searchAddress(this@MainActivity, start, destination, walkingSpeed, callback = { result ->
                                    runOnUiThread {
                                        if (!isCurrentSearchResponse(requestId, searchGeneration)) return@runOnUiThread
                                        journeyLoading = false
                                        result.fold(
                                            onSuccess = {
                                                journeys = it.options
                                                if (it.options.isEmpty()) {
                                                    startWalkingFallback("Keine passende ÖPNV-Verbindung. Fußweg zum Ziel wird geprüft.")
                                                } else {
                                                    walkingOnlyFallback = false
                                                    routeStatus = "${it.options.size} passende Verbindungen nach ${it.destination.name} geladen"
                                                }
                                            },
                                            onFailure = {
                                                if (it is MvvJourneyRepository.AmbiguousDestination) {
                                                    mvvCandidates = it.candidates
                                                    routeStatus = "Bitte die richtige MVV-Zielhaltestelle auswählen:"
                                                } else {
                                                    startWalkingFallback("MVV-Suche derzeit nicht verfügbar: ${routeFailureMessage(it)}. Fußweg als Alternative.")
                                                }
                                            }
                                        )
                                    }
                                }, preference = RoutePreference(routeFilterMode, routeFilterQuery), arrivalByMillis = arrivalBy,
                                    departureAtMillis = if (journeyTimingMode == "planned" && !plannedArrival) plannedAtMillis else null)
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
                        // Start already launched the search before switching tabs. Do not
                        // demand a second identical primary click on the results screen.
                        // Keep an explicit retry action after completion or changed inputs.
                        if (journeyLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("mvv_search_loading"))
                            Text("Verbindungen werden gesucht …", style = MaterialTheme.typography.labelMedium)
                        } else if (routeStatus.isNotBlank() || journeys.isNotEmpty()) {
                            TextButton(
                                modifier = Modifier.testTag("mvv_search_button"),
                                enabled = usableLocation != null && destination.isNotBlank() &&
                                    (routeFilterMode == RouteFilterMode.ANY || routeFilterQuery.isNotBlank()),
                                onClick = searchConnections
                            ) { Text("↻ Verbindung erneut suchen") }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("mvv_search_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5635B1)),
                                enabled = usableLocation != null && destination.isNotBlank() &&
                                    (routeFilterMode == RouteFilterMode.ANY || routeFilterQuery.isNotBlank()),
                                onClick = searchConnections
                            ) { Text("Verbindung finden") }
                        }
                        if (walkingOnlyFallback) {
                            Card(modifier = Modifier.fillMaxWidth().testTag("walking_only_fallback")) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🚶 Nur zu Fuß", style = MaterialTheme.typography.titleLarge)
                                    Text("Fußweg als Alternative ab aktuellem Standort zum Ziel: $destination")
                                    if (walkingRoute != null) {
                                        Text("Fußroute: ca. ${walkingRoute!!.totalMeters} m")
                                        val next = navigationLocation?.let { WalkingNavigation.nextTurn(walkingRoute!!, it) }
                                        if (next != null) {
                                            Text("${next.first.arrow} ${next.first.instruction} · ${next.second} m",
                                                style = MaterialTheme.typography.titleMedium)
                                        } else Text("Für Abbiegehinweise wird ein aktueller GPS-Standort benötigt.")
                                        Button(
                                            modifier = Modifier.fillMaxWidth().testTag("open_walking_only_dashboard"),
                                            onClick = {
                                                walkingOnlyDashboard = true
                                                walkingNavigationOpen = true
                                                selectedTab = CockpitTab.JOURNEY
                                            }
                                        ) { Text("🚶 Als meine Fahrt öffnen") }
                                        Text("OSM-Fußwegführung ist experimentell. Bitte Weg und Ziel prüfen.",
                                            style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        if (walkingRouteStatus.startsWith("Fußweg zum Ziel wird")) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
                                        Text(walkingRouteStatus.ifBlank { "Fußroute nicht verfügbar." })
                                    }
                                }
                            }
                        }
                        if (gpsDebugEnabled) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth().testTag("export_gps_debug"),
                                onClick = { gpsExportLauncher.launch("meinweg-gps-debug.json") }
                            ) { Text("📍 GPS-Debugprotokoll exportieren") }
                            Text("GPS-Ereignisse werden nur bei geöffneter App im Arbeitsspeicher gesammelt. Export enthält genaue Standortkoordinaten.",
                                style = MaterialTheme.typography.labelSmall)
                            if (gpsExportStatus.isNotBlank()) Text(gpsExportStatus)
                        }
                        if (debugApk) {
                            OutlinedButton(
                                modifier = Modifier.testTag("export_mvv_debug_response"),
                                enabled = DebugResponseCapture.snapshot() != null && !journeyLoading,
                                onClick = { debugExportLauncher.launch("meinweg-mvv-debug.json") }
                            ) { Text("🐞 MVV-Debugantwort speichern") }
                            Text("Nur Debug-APK. Die Datei kann genaue Standort- und Zieladressen enthalten. Vor dem Teilen bitte prüfen.",
                                style = MaterialTheme.typography.labelSmall)
                            if (debugExportStatus.isNotBlank()) Text(debugExportStatus)
                        }
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
                                            if (!isCurrentSearchResponse(requestId, searchGeneration)) return@runOnUiThread
                                            journeyLoading = false
                                            result.fold(
                                                onSuccess = {
                                                    journeys = it.options
                                                    if (it.options.isEmpty()) {
                                                        walkingOnlyFallback = true
                                                        walkingNavigationOpen = true
                                                        walkingRoute = null
                                                        val walkingId = ++walkingRequestId
                                                        walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                                        routeStatus = "Keine passende ÖPNV-Verbindung. Fußweg zum Ziel wird geprüft."
                                                        WalkingNavigation.fetch(this@MainActivity, start, destination) { walkResult ->
                                                            if (walkingId == walkingRequestId && walkingOnlyFallback) {
                                                                walkingRoute = walkResult.getOrNull()
                                                                walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage ?: ""
                                                            }
                                                        }
                                                    } else {
                                                        walkingOnlyFallback = false
                                                        routeStatus = "${it.options.size} Verbindungen nach ${it.destination.name} geladen"
                                                    }
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
                        if (gpsDebugEnabled) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth().testTag("export_gps_debug_journey"),
                                onClick = { gpsExportLauncher.launch("meinweg-gps-debug.json") }
                            ) { Text("📍 GPS-Protokoll exportieren") }
                            if (gpsExportStatus.isNotBlank()) Text(gpsExportStatus)
                        }
                        if (walkingOnlyDashboard && walkingRoute != null) {
                            val route = walkingRoute!!
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("walking_only_dashboard"),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECE7FF))
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("MEINE FAHRT · FUSSWEG", style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF49309B), fontWeight = FontWeight.Bold)
                                    Text("🚶 Nur zu Fuß", style = MaterialTheme.typography.headlineMedium)
                                    Text("Ziel: $destination", style = MaterialTheme.typography.bodyMedium)
                                    Text("Fußroute: ca. ${route.totalMeters} m", style = MaterialTheme.typography.titleLarge)
                                    val next = navigationLocation?.let { WalkingNavigation.nextTurn(route, it) }
                                    if (next != null) {
                                        Text(next.first.arrow, style = MaterialTheme.typography.displayLarge,
                                            color = Color(0xFF49309B))
                                        Text("${next.second} m", style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold)
                                        Text(next.first.instruction, style = MaterialTheme.typography.titleLarge)
                                    } else {
                                        Text("Aktueller GPS-Standort fehlt oder ist zu alt. Route bleibt erhalten; Abbiegehinweise warten auf GPS.")
                                    }
                                    Text("OSM-Fußwegführung ist experimentell. Weg und Ziel bitte prüfen.",
                                        style = MaterialTheme.typography.labelSmall)
                                    OutlinedButton(onClick = {
                                        walkingOnlyDashboard = false
                                        selectedTab = CockpitTab.CONNECTIONS
                                    }) { Text("Zurück zu Verbindungen") }
                                }
                            }
                        }
                        if (dashboardJourney == null && !walkingOnlyDashboard) TextButton(onClick = { selectedTab = CockpitTab.CONNECTIONS }) { Text("Verbindungen ansehen") }
                        if (dashboardJourney != null) {
                            val chosen = journeys.getOrNull(dashboardJourney!!)
                            if (chosen != null) {
                                val boarding = chosen.steps.firstOrNull { !it.walking }
                                val firstWalk = chosen.steps.takeWhile { it.walking }
                                val walkMinutes = firstWalk.sumOf { it.minutes }
                                val approximateMeters = (walkMinutes * walkingSpeed * 1000f / 60f).toInt()
                                val targetDay = if (journeyTimingMode == "planned" && !plannedArrival) null else searchTimeMillis
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
                                        // GPS describes location freshness only, never MVV real-time departures.
                                        val gpsLabel = when {
                                            navigationLocation != null && (locationAgeSeconds ?: 0L) <= 15L -> "GPS LIVE"
                                            navigationLocation != null -> "GPS ${locationAgeSeconds ?: 0}s · letzte Position"
                                            location != null -> "GPS ${locationAgeSeconds ?: 0}s"
                                            locationPermissionGranted && location != null -> "GPS ALT"
                                            locationPermissionGranted -> "GPS …"
                                            else -> "GPS AUS"
                                        }
                                        val gpsColor = when {
                                            navigationLocation != null -> Color(0xFF247A43)
                                            locationPermissionGranted -> Color(0xFF6B7280)
                                            else -> Color(0xFFB3261E)
                                        }
                                        Row(Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("MEINE FAHRT  ·  FAHRPLAN", color = Color(0xFF49309B),
                                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f))
                                            Surface(shape = RoundedCornerShape(50.dp),
                                                color = gpsColor.copy(alpha = 0.12f)) {
                                                Text("⌖ $gpsLabel", color = gpsColor,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                            }
                                        }
                                        val recommendedLeave = targetDay?.let { recommendedLeaveMillis(chosen, it) }
                                        val untilLeave = recommendedLeave?.let { (it - clockMillis) / 60000L }
                                        val leaveClock = recommendedLeave?.let {
                                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).apply {
                                                timeZone = java.util.TimeZone.getTimeZone("Europe/Berlin")
                                            }.format(java.util.Date(it))
                                        }
                                        val untilBus = boarding?.let { countdown(it.departure) }
                                        // OSRM step lengths approximate remaining walking distance during navigation.
                                        val liveTurn = if (walkingNavigationOpen && navigationLocation != null && walkingRoute != null)
                                            WalkingNavigation.nextTurn(walkingRoute!!, navigationLocation!!) else null
                                        val remainingWalkMeters = liveTurn?.let { (turn, metersToTurn) ->
                                            val turns = walkingRoute!!.turns
                                            val index = turns.indexOf(turn)
                                            if (index < 0) null else (metersToTurn +
                                                turns.drop(index).dropLast(1).sumOf { it.distanceMeters }).coerceAtLeast(0)
                                        }
                                        // Use measured walking speed only when Android reports a plausible
                                        // value and the GPS fix is accurate. Otherwise use configured pace.
                                        val measuredWalkingKmh = usableLocation?.takeIf {
                                            it.hasSpeed() && it.hasAccuracy() && it.accuracy <= 30f &&
                                                it.speed in 0.5f..2.2f
                                        }?.speed?.times(3.6f)
                                        val estimatedWalkingKmh = measuredWalkingKmh ?: walkingSpeed
                                        val remainingWalkMinutes = remainingWalkMeters?.let {
                                            kotlin.math.ceil(it / (estimatedWalkingKmh * 1000.0 / 60.0)).toInt()
                                        }
                                        val dynamicBufferMinutes = if (remainingWalkMinutes != null && untilBus != null)
                                            untilBus - remainingWalkMinutes else null
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("🚶 LOSGEHEN", style = MaterialTheme.typography.labelSmall)
                                                    Text(leaveClock ?: "–", style = MaterialTheme.typography.headlineMedium,
                                                        fontWeight = FontWeight.Bold)
                                                    Text(when {
                                                        untilLeave == null -> "Zeit unbekannt"
                                                        untilLeave > 0 -> "in ca. $untilLeave Min."
                                                        untilLeave >= -5 -> "Jetzt losgehen"
                                                        else -> "Zeit vorbei"
                                                    }, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                            Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("🚌 BUS ${boarding?.line.orEmpty()}", style = MaterialTheme.typography.labelSmall)
                                                    Text(boarding?.departure ?: "–", style = MaterialTheme.typography.headlineMedium,
                                                        fontWeight = FontWeight.Bold)
                                                    Text(when {
                                                        untilBus == null -> "Zeit unbekannt"
                                                        untilBus >= 0 -> "in ca. $untilBus Min."
                                                        else -> "Abfahrt vorbei"
                                                    }, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                                            Column(Modifier.fillMaxWidth().padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("🚏 EINSTIEG", style = MaterialTheme.typography.labelSmall)
                                                Text(boarding?.from?.ifBlank { "Haltestelle unbekannt" }
                                                    ?: "Kein Verkehrsmittel", fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Text("👟 FUSSWEG", style = MaterialTheme.typography.labelSmall)
                                                    Text(if (remainingWalkMinutes != null) "ca. $remainingWalkMinutes Min."
                                                             else if (walkMinutes > 0) "ca. $walkMinutes Min." else "–",
                                                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                    Text(if (remainingWalkMeters != null) "GPS-Schätzung: ca. $remainingWalkMeters m"
                                                             else if (walkMinutes > 0) "Planwert: ca. $approximateMeters m"
                                                             else "Keine Angabe",
                                                        style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Text("⏱ PUFFER", style = MaterialTheme.typography.labelSmall)
                                                    val bufferColor = when {
                                                        dynamicBufferMinutes == null -> Color(0xFF6B7280)
                                                        dynamicBufferMinutes >= 5 -> Color(0xFF247A43)
                                                        dynamicBufferMinutes >= 3 -> Color(0xFF9A6700)
                                                        else -> Color(0xFFB3261E)
                                                    }
                                                    Text(dynamicBufferMinutes?.let { "ca. $it Min." } ?: "3 Min.",
                                                        color = bufferColor, fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium)
                                                    Text(when {
                                                        dynamicBufferMinutes == null -> "Geplante Reserve · keine GPS-Prognose"
                                                        dynamicBufferMinutes >= 5 -> "Gut in der Zeit · geschätzt"
                                                        dynamicBufferMinutes >= 3 -> "Reserve knapp · geschätzt"
                                                        else -> "Beeilen · Schätzung!"
                                                    }, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        Text("Fahrplanzeiten · Puffer = Abfahrt minus GPS-Restgehzeit (bei aktiver OSM-Navigation) · ohne Live-Verspätungen",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (boarding != null && countdown(boarding.departure)?.let { it < 0 } == true) {
                                            Text("Die geplante Busabfahrt liegt zurück. Deine Reise ist damit nicht automatisch beendet. Wenn du ausgestiegen bist, starte den Fußweg zum Reiseziel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            TextButton(onClick = { dashboardJourney = null; selectedTab = CockpitTab.CONNECTIONS }) {
                                                Text("Neue Verbindung suchen")
                                            }
                                        }
                                        // Do not navigate back to the boarding stop after the scheduled bus departure.
                                        // The user explicitly confirms they have left the bus; timetable alone
                                        // cannot establish their actual position in the journey.
                                        val busDeparturePassed = boarding?.let { countdown(it.departure)?.let { minutes -> minutes < 0 } } == true
                                        val finalWalkingDestination = destination.takeIf { it.isNotBlank() }
                                            ?: chosen.steps.lastOrNull()?.to?.takeIf { it.isNotBlank() }
                                        if (busDeparturePassed && finalWalkingDestination != null) {
                                            Button(
                                                onClick = {
                                                    val start = usableLocation ?: return@Button
                                                    val requestId = ++walkingRequestId
                                                    walkingRoute = null
                                                    walkingNavigationOpen = true
                                                    walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                                    WalkingNavigation.fetch(this@MainActivity, start, finalWalkingDestination) { result ->
                                                        if (walkingRequestId == requestId) {
                                                            walkingRoute = result.getOrNull()
                                                            walkingRouteStatus = result.exceptionOrNull()?.localizedMessage ?: ""
                                                        }
                                                    }
                                                },
                                                enabled = usableLocation != null,
                                                modifier = Modifier.fillMaxWidth().testTag("continue_walking_after_bus")
                                            ) { Text("🚶 Ausgestiegen – zum Ziel navigieren") }
                                            Text("Ziel: $finalWalkingDestination · OSM-Fußweg ab aktuellem GPS-Standort. Bitte Zieladresse und Weg prüfen.",
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (!busDeparturePassed && boarding?.from?.isNotBlank() == true && usableLocation != null) {
                                            Button(onClick = {
                                                val start = usableLocation ?: return@Button
                                                walkingRequestId++
                                                val requestId = walkingRequestId
                                                walkingRoute = null
                                                walkingNavigationOpen = true
                                                walkingRouteStatus = "Fußweg wird über OpenStreetMap berechnet …"
                                                WalkingNavigation.fetch(this@MainActivity, start, boarding.from) { result ->
                                                    if (walkingRequestId == requestId) {
                                                        walkingRoute = result.getOrNull()
                                                        walkingRouteStatus = result.exceptionOrNull()?.localizedMessage
                                                            ?: ""
                                                    }
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text("🚶 Fußweg-Navigation starten") }
                                        }
                                        if (walkingNavigationOpen) {
                                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("FUSSWEG-NAVIGATION · OSM", fontWeight = FontWeight.Bold)
                                                    val fix = navigationLocation
                                                    val route = walkingRoute
                                                    val next = if (fix != null && route != null)
                                                        WalkingNavigation.nextTurn(route, fix) else null
                                                    if (next != null) {
                                                        Text(next.first.arrow, style = MaterialTheme.typography.displayLarge,
                                                            color = Color(0xFF49309B))
                                                        Text("${next.second} m", style = MaterialTheme.typography.headlineLarge,
                                                            fontWeight = FontWeight.Bold)
                                                        Text(next.first.instruction, style = MaterialTheme.typography.titleLarge)
                                                        Text("Route: ca. ${route?.totalMeters ?: 0} m · GPS aktualisiert die Entfernung",
                                                            style = MaterialTheme.typography.labelSmall)
                                                    } else {
                                                        Text(if (route != null) "GPS zu ungenau – bitte freien Himmel abwarten"
                                                            else walkingRouteStatus.ifBlank { "Route wird geladen …" })
                                                    }
                                                    TextButton(onClick = {
                                                        walkingNavigationOpen = false
                                                        walkingRequestId++
                                                    }) { Text("Navigation schließen") }
                                                }
                                            }
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
                            val recommendedLeave = targetDay?.let { recommendedLeaveMillis(trip, it) }
                            val leaveIn = recommendedLeave?.let { (it - clockMillis) / 60000L }
                            val leaveClock = recommendedLeave?.let {
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("Europe/Berlin")
                                }.format(java.util.Date(it))
                            }
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
                                    val initialWalk = trip.steps.takeWhile { it.walking }.sumOf { it.minutes.coerceAtLeast(0) }
                                    val finalWalk = trip.steps.takeLastWhile { it.walking }.sumOf { it.minutes.coerceAtLeast(0) }
                                    val totalWalk = trip.walkingMinutes.coerceAtLeast(0)
                                    val knownWalk = initialWalk + finalWalk
                                    val accessWalk = if (knownWalk > 0) initialWalk else 0
                                    val exitWalk = if (knownWalk > 0) finalWalk else 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth().testTag("route_legs_tiles"),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White
                                        ) {
                                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🚶 Hinweg", style = MaterialTheme.typography.labelSmall)
                                                Text(if (knownWalk > 0) "ca. $accessWalk Min." else "–",
                                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF5635B1)
                                        ) {
                                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(firstBus?.line?.ifBlank { "ÖPNV" } ?: "Fußweg",
                                                    color = Color.White, fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium)
                                                Text("${trip.changes} Umstiege", color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White
                                        ) {
                                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🚶 Zielweg", style = MaterialTheme.typography.labelSmall)
                                                Text(if (knownWalk > 0) "ca. $exitWalk Min." else "–",
                                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                    if (firstBus != null) {
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White,
                                            modifier = Modifier.fillMaxWidth().testTag("journey_stop_card")) {
                                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("🚏", style = MaterialTheme.typography.titleMedium)
                                                    Column {
                                                        Text("ERSTE HALTESTELLE", style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(firstBus.from.ifBlank { "Einstieg noch unbekannt" },
                                                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                                if (firstBus.to.isNotBlank()) {
                                                    HorizontalDivider()
                                                    Row(verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("🚌", style = MaterialTheme.typography.titleMedium)
                                                        Column {
                                                            Text("LINIE ${firstBus.line} · NÄCHSTER AUSSTIEG",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(firstBus.to, fontWeight = FontWeight.Bold,
                                                                style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (totalWalk > knownWalk) {
                                        Text("🚶 Weitere Fußwege: ca. ${totalWalk - knownWalk} Min.",
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { dashboardJourney = index; selectedTab = CockpitTab.JOURNEY },
                                        modifier = Modifier.fillMaxWidth().testTag("open_walking_dashboard")
                                    ) { Text("Meine Fahrt öffnen") }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (leaveIn != null) {
                                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                                color = Color.White) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("LOS GEHEN", style = MaterialTheme.typography.labelSmall)
                                                    Text(leaveClock ?: "–", fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleLarge)
                                                    Text(if (leaveIn > 0) "in ca. $leaveIn Min."
                                                        else if (leaveIn >= -5) "Jetzt" else "Zeit vorbei",
                                                        style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (firstBus != null && busIn != null) {
                                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                                color = Color.White) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("LINIE ${firstBus.line}", style = MaterialTheme.typography.labelSmall)
                                                    Text(firstBus.departure, fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleLarge)
                                                    Text(if (busIn > 0) "in ca. $busIn Min." else "Abfahrt prüfen",
                                                        style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    TextButton(onClick = { expandedJourney = if (expandedJourney == index) null else index },
                                        modifier = Modifier.testTag("journey_details")) {
                                        Text(if (expandedJourney == index) "Details ausblenden  ⌃" else "Alle Etappen ansehen  ⌄",
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                    if (expandedJourney == index) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("journey_detail_cards")) {
                                            trip.steps.forEachIndexed { stepIndex, step ->
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color.White,
                                                    modifier = Modifier.fillMaxWidth().testTag("journey_detail_step_${stepIndex}")
                                                ) {
                                                    Column(Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            Surface(shape = RoundedCornerShape(12.dp),
                                                                color = if (step.walking) Color(0xFFECE8FA) else Color(0xFF5635B1)) {
                                                                Text(if (step.walking) "🚶" else "🚌",
                                                                    modifier = Modifier.padding(10.dp),
                                                                    style = MaterialTheme.typography.titleMedium)
                                                            }
                                                            Column(Modifier.weight(1f)) {
                                                                Text(if (step.walking) "ETAPPE ${stepIndex + 1} · FUSSWEG"
                                                                    else "ETAPPE ${stepIndex + 1} · LINIE ${step.line.ifBlank { "ÖPNV" }}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                Text(if (step.walking) "Zu Fuß" else "Linie ${step.line.ifBlank { "ÖPNV" }}",
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold)
                                                            }
                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(step.departure.ifBlank { "–" },
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold)
                                                                Text("bis ${step.arrival.ifBlank { "–" }}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                        HorizontalDivider(color = Color(0xFFF0EDF6))
                                                        Row(verticalAlignment = Alignment.Top,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("●\n│\n●", color = Color(0xFF7659AE),
                                                                style = MaterialTheme.typography.labelSmall)
                                                            Column(Modifier.weight(1f),
                                                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text(step.from.ifBlank { "Startpunkt" },
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Medium)
                                                                Text(step.to.ifBlank { "Zielpunkt" },
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                        if (step.walking && step.minutes > 0) {
                                                            Text("⏱  ca. ${step.minutes} Min. Fußweg",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = Color(0xFF604993))
                                                        }
                                                    }
                                                }
                                            }
                                            if (firstBus?.from?.isNotBlank() == true && usableLocation != null) {
                                                Button(onClick = {
                                                    dashboardJourney = index
                                                    selectedTab = CockpitTab.JOURNEY
                                                }, modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(14.dp)) {
                                                    Text("🚶  Fußweg-Navigation öffnen")
                                                }
                                            }
                                            Text("MVV-Fahrplanzeiten · Fußwege geschätzt",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
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
