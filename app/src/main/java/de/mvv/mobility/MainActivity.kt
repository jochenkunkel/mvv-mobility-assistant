package de.mvv.mobility

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.view.View
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.location.Location
import android.speech.tts.TextToSpeech
import java.util.Locale
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
import de.mvv.mobility.ui.components.WalkingMiniMap
import de.mvv.mobility.ui.components.CockpitTab
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import de.mvv.mobility.data.ConnectionCheckState
import de.mvv.mobility.data.ConnectionStatusResolver
import de.mvv.mobility.data.EarlierConnectionsResolver
import de.mvv.mobility.data.EfaTime
import de.mvv.mobility.data.JourneyTime
import de.mvv.mobility.ui.components.FullscreenJourneyDashboard
import de.mvv.mobility.ui.components.FullscreenWalkNavigation
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.mvv.mobility.data.DebugResponseCapture
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.isCurrentSearchResponse
import de.mvv.mobility.data.TransitModeIcon
import de.mvv.mobility.data.JourneyOption
import de.mvv.mobility.data.RouteFilterMode
import de.mvv.mobility.data.RoutePreference
import de.mvv.mobility.data.reachableNowJourneys
import de.mvv.mobility.data.filterJourneys
import de.mvv.mobility.data.selectAppointmentJourneys
import de.mvv.mobility.data.UpcomingAppointment
import de.mvv.mobility.ui.components.ArrivalBufferControl
import de.mvv.mobility.ui.components.RouteFilterControl
import de.mvv.mobility.widget.JourneyWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private fun String?.orBlank(): String = this ?: ""

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
        val navigationTraceViewModel = ViewModelProvider(this)[NavigationTraceViewModel::class.java]
        val sessionFactory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NavigationSessionViewModel(getPreferences(MODE_PRIVATE), fixture) as T
            }
        }
        val session = ViewModelProvider(this, sessionFactory)[NavigationSessionViewModel::class.java]
        // QA hook for reproducible design-review screenshots; ignored in normal use.
        when (intent.getStringExtra("mein_weg_test_fullscreen")) {
            "dashboard" -> session.fullscreenMode = FullscreenMode.DASHBOARD
            "navigation" -> session.fullscreenMode = FullscreenMode.NAVIGATION
        }
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
                    var selectedAppointmentStart by session::selectedAppointmentStart
                    val fixtureBaseTime = remember { System.currentTimeMillis() }
                    val appointments = remember(allowed, refresh, visibleAppointments) {
                        if (fixture) (1..visibleAppointments).map { index ->
                            UpcomingAppointment("Testtermin $index", "Olympiapark München", fixtureBaseTime + index * 3600000L)
                        } else if (allowed) DeviceCalendarRepository.appointments(this, visibleAppointments) else emptyList()
                    }
                    var arrivalBufferMinutes by session::arrivalBufferMinutes
                    // The calendar must never influence "Jetzt" or "Geplant".
                    var journeyTimingMode by session::journeyTimingMode
                    var plannedArrival by session::plannedArrival
                    var plannedAtMillis by session::plannedAtMillis
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
                    var confirmedDestination by session::confirmedDestination
                    var addressSuggestions by session::addressSuggestions
                    var lookupStatus by session::lookupStatus
                    var lookupRequest by session::lookupRequest
                    val destinationFocus = LocalFocusManager.current
                    LaunchedEffect(confirmedDestination) {
                        // Fixture mode must stay deterministic and offline: no
                        // online geocoder may rewrite the golden-test target.
                        if (fixture) return@LaunchedEffect
                        val query = confirmedDestination.trim()
                        if (query.length < 3 || lookupStatus.startsWith("✓")) return@LaunchedEffect
                        delay(10_000L)
                        lookupRequest++
                        val requestId = lookupRequest
                        lookupStatus = "Adresse wird geprüft …"
                        DestinationLookup.search(this@MainActivity, query) { results ->
                            if (requestId == lookupRequest && confirmedDestination.trim() == query) {
                                addressSuggestions = results.take(4)
                                when (results.size) {
                                    0 -> lookupStatus = "Kein Treffer – bitte Adresse präzisieren"
                                    1 -> {
                                        confirmedDestination = results.first()
                                        addressSuggestions = emptyList()
                                        lookupStatus = "✓ Ziel automatisch geprüft"
                                    }
                                    else -> lookupStatus = "Passendes Ziel auswählen"
                                }
                            }
                        }
                    }

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
                    var walkingSpeed by session::walkingSpeed
                    var routeStatus by session::routeStatus
                    var journeys by session::journeys
                    var expandedJourney by session::expandedJourney
                    var dashboardJourney by session::dashboardJourney
                    var walkingRoute by session::walkingRoute
                    var resolvedWalkingTarget by session::resolvedWalkingTarget
                    var walkingOnlyFallback by session::walkingOnlyFallback
                    var walkingOnlyDashboard by session::walkingOnlyDashboard
                    var walkingRouteStatus by session::walkingRouteStatus
                    var walkingNavigationOpen by session::walkingNavigationOpen
                    var walkingRequestId by session::walkingRequestId
                    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            clockMillis = System.currentTimeMillis()
                            delay(5_000L)
                        }
                    }
                    var fullscreenMode by session::fullscreenMode
                    // Only the journey dashboard is immersive; keep the status bar visible.
                    // Restore Android navigation when leaving it or disposing this Activity.
                    DisposableEffect(fullscreenMode) {
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        if (fullscreenMode == FullscreenMode.DASHBOARD) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.navigationBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.navigationBars())
                        }
                        onDispose {
                            controller.show(WindowInsetsCompat.Type.navigationBars())
                        }
                    }
                    var connectionChecks by session::connectionChecks
                    var newSearchOffered by session::newSearchOffered
                    var pendingNewSearch by session::pendingNewSearch
                    var earlierSearchOffsetMinutes by session::earlierSearchOffsetMinutes
                    val selectedJourney = journeys.getOrNull(dashboardJourney ?: expandedJourney ?: 0)
                    val upcomingConnection = selectedJourney?.steps?.let { steps ->
                        ConnectionStatusResolver.upcoming(steps, clockMillis, connectionChecks)
                    }
                    // Keep the confirmed connection visible after the action so
                    // the user gets stable feedback instead of a vanishing button.
                    val displayConnection = selectedJourney?.steps?.let { steps ->
                        ConnectionStatusResolver.upcoming(steps, clockMillis, emptyMap())
                    }
                    var journeyLoading by session::journeyLoading
                    var searchGeneration by session::searchGeneration
                    val settings = remember { getPreferences(MODE_PRIVATE) }
                    var routeFilterMode by session::routeFilterMode
                    var routeFilterQuery by session::routeFilterQuery
                    var settingsExpanded by remember { mutableStateOf(false) }
                    var homeAddress by remember { mutableStateOf(settings.getString("home_address", "") ?: "") }
                    var homeSaved by remember { mutableStateOf(homeAddress.isNotBlank()) }
                    var languageChoice by remember { mutableStateOf(settings.getString("language_choice", "system") ?: "system") }

                    var mvvCandidates by session::mvvCandidates
                    var location by session::location
                    var locationStatus by session::locationStatus
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
                        CurrentLocationRepository.isFreshFix(it)
                    }
                    val navigationLocation = usableLocation?.takeIf {
                        fixture || ((CurrentLocationRepository.ageMillis(it)?.let { age -> age <= 45_000L } == true) &&
                            (!it.hasAccuracy() || it.accuracy <= 80f))
                    }
                    val gpsIndicatorText = when {
                        navigationLocation != null -> "🛰 GPS aktiv"
                        location != null -> "🛰 GPS wartet"
                        else -> "🛰 Kein GPS"
                    }
                    val gpsIndicatorColor = if (navigationLocation != null)
                        Color(0xFF237A45) else Color(0xFF9A661B)
                    val locationAgeSeconds = location?.takeIf { !fixture }
                        ?.let { CurrentLocationRepository.ageMillis(it)?.div(1000L) }
                    LaunchedEffect(gpsDebugEnabled, appForeground, walkingNavigationOpen, walkingOnlyDashboard) {
                        if (gpsDebugEnabled && appForeground) {
                            while (true) {
                                val fix = location
                                val now = System.currentTimeMillis()
                                val age = fix?.let { CurrentLocationRepository.ageMillis(it) }
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
                                        !CurrentLocationRepository.isFreshFix(found) -> "stale_or_future"
                                        previous != null && !CurrentLocationRepository.isNewer(found, previous) -> "out_of_order"
                                        found.hasAccuracy() && found.accuracy > 100f -> "accuracy_over_100m"
                                        previous != null && previous.hasAccuracy() && previous.accuracy <= 25f &&
                                            CurrentLocationRepository.isFreshFix(previous, 15_000L) &&
                                            (!found.hasAccuracy() || found.accuracy > previous.accuracy + 35f) -> "worse_than_recent_gps"
                                        else -> "accepted"
                                    }
                                    GpsDebugRecorder.event("provider_update", found, reason,
                                        walkingNavigationOpen || walkingOnlyDashboard,
                                        found?.let { CurrentLocationRepository.ageMillis(it) })
                                }

                                if (found != null) {
                                    val now = System.currentTimeMillis()
                                    val previous = location
                                    // Ignore stale, out-of-order or implausibly inaccurate fixes.
                                    // Do not overwrite a good fix with an older cached result.
                                    val recentPreciseFix = previous != null &&
                                        previous.hasAccuracy() && previous.accuracy <= 25f &&
                                        CurrentLocationRepository.isFreshFix(previous, 15_000L)
                                    val substantiallyWorse = recentPreciseFix &&
                                        (!found.hasAccuracy() || found.accuracy > previous!!.accuracy + 35f)
                                    val acceptable = CurrentLocationRepository.isFreshFix(found) &&
                                        (previous == null || CurrentLocationRepository.isNewer(found, previous)) &&
                                        (!found.hasAccuracy() || found.accuracy <= 100f) &&
                                        !substantiallyWorse
                                    if (acceptable) {
                                        location = found
                                        if ((walkingOnlyDashboard || walkingNavigationOpen) &&
                                            found.hasAccuracy() && found.accuracy <= 35f) {
                                            // Keep real GPS history independent of planned route geometry.
                                             // A missing provider timestamp must not be fabricated.
                                             val elapsed = found.elapsedRealtimeNanos
                                             if (found.time > 0L || elapsed > 0L) {
                                                 val fix = de.mvv.mobility.data.TraceFix(
                                                     latitude = found.latitude,
                                                     longitude = found.longitude,
                                                     timestampMillis = found.time,
                                                     accuracyMeters = found.accuracy,
                                                     elapsedRealtimeNanos = elapsed.takeIf { it > 0L }
                                                 )
                                                 navigationTraceViewModel.acceptFix(fix)
                                             }
                                        }
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
                    var selectedTab by session::selectedTab

                    // Re-route only after three distinct, reliable off-route GPS fixes.
                    // A cooldown prevents repeated network requests in narrow streets.
                    var offRouteFixes by session::offRouteFixes
                    var lastOffRouteFixTime by session::lastOffRouteFixTime
                    var lastRerouteAt by session::lastRerouteAt
                    var rerouting by session::rerouting
                    LaunchedEffect(walkingRoute) { session.noteRouteForEvidenceReset(walkingRoute) }
                    LaunchedEffect(navigationLocation?.time, walkingRoute, walkingOnlyDashboard,
                        walkingNavigationOpen, selectedTab, appForeground) {
                        val fix = navigationLocation
                        val route = walkingRoute
                        if (!appForeground || selectedTab != CockpitTab.JOURNEY ||
                            !(walkingOnlyDashboard || walkingNavigationOpen) ||
                            route == null || route.destination == null ||
                            route.path.size < 2 || rerouting) {
                            offRouteFixes = 0
                        } else if (fix == null || !fix.hasAccuracy() || fix.accuracy > 35f) {
                            // A temporary GPS gap must not erase confirmed off-route evidence.
                        } else if (fix.time != lastOffRouteFixTime) {
                            lastOffRouteFixTime = fix.time
                            val lateral = session.routeProgress(route, fix)?.lateralMeters
                            // 45m minimum; add GPS uncertainty to avoid false reroutes.
                            if (lateral != null && lateral > 25.0 + fix.accuracy) {
                                offRouteFixes++
                            } else {
                                offRouteFixes = 0
                            }
                            if (offRouteFixes >= 3 &&
                                System.currentTimeMillis() - lastRerouteAt >= 30_000L) {
                                offRouteFixes = 0
                                lastRerouteAt = System.currentTimeMillis()
                                rerouting = true
                                walkingRouteStatus = "Route wird ab aktuellem Standort neu berechnet …"
                                val requestId = ++walkingRequestId
                                WalkingNavigation.fetch(fix, route.destination) { result ->
                                    if (requestId == walkingRequestId) {
                                        result.onSuccess { walkingRoute = it }
                                        walkingRouteStatus = result.exceptionOrNull()?.let {
                                            "Neuberechnung fehlgeschlagen: ${it.localizedMessage}"
                                        } ?: "Route aktualisiert"
                                    }
                                    rerouting = false
                                }
                            }
                        }
                    }

                    var voiceMuted by session::voiceMuted
                    var speechReady by session::speechReady
                    val speech = remember { arrayOfNulls<TextToSpeech>(1) }
                    DisposableEffect(Unit) {
                        val engine = TextToSpeech(this@MainActivity) { status ->
                            if (status == TextToSpeech.SUCCESS) speechReady = true
                        }
                        speech[0] = engine
                        onDispose {
                            engine.stop()
                            engine.shutdown()
                            speech[0] = null
                        }
                    }
                    val spokenTurn = if ((walkingOnlyDashboard || walkingNavigationOpen) &&
                        selectedTab == CockpitTab.JOURNEY && navigationLocation != null && walkingRoute != null)
                        WalkingNavigation.nextTurn(walkingRoute!!, navigationLocation!!,
                            session.routeProgress(walkingRoute!!, navigationLocation!!),
                            useTrackedProgress = true) else null
                    val spokenTurnKey = spokenTurn?.first?.let {
                        "${it.latitude},${it.longitude},${it.instruction}"
                    }
                    var lastSpokenTurnKey by session::lastSpokenTurnKey
                    LaunchedEffect(walkingRoute, walkingOnlyDashboard, walkingNavigationOpen) {
                        session.noteVoiceSession(walkingRoute, walkingOnlyDashboard, walkingNavigationOpen)
                    }
                    LaunchedEffect(spokenTurnKey, voiceMuted, speechReady, selectedTab) {
                        if (!voiceMuted && speechReady && spokenTurnKey != null &&
                            spokenTurnKey != lastSpokenTurnKey && spokenTurn != null) {
                            val engine = speech[0]
                            if (engine != null) {
                                engine.language = Locale.GERMANY
                                val germanMale = engine.voices?.firstOrNull { voice ->
                                    voice.locale.language == "de" &&
                                        (voice.name.contains("male", ignoreCase = true) ||
                                            voice.name.contains("männlich", ignoreCase = true))
                                }
                                if (germanMale != null) engine.voice = germanMale
                                val announcement = if (spokenTurn.first.instruction == "Ziel erreicht")
                                    "Ziel erreicht"
                                else "In ${spokenTurn.second} Metern ${spokenTurn.first.instruction.lowercase(Locale.GERMANY)}"
                                engine.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, spokenTurnKey)
                                lastSpokenTurnKey = spokenTurnKey
                            }
                        }
                    }

                    var activeSetupStep by session::activeSetupStep
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
                                            activeSetupStep = if (mode == "now") 1 else 0
                                        } else if (mode == "now") {
                                            // Selecting "Jetzt" is already the user's confirmation.
                                            activeSetupStep = 1
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
                        if (journeyTimingMode != null && journeyTimingMode != "now" &&
                            (journeyTimingMode != "calendar" || appointment != null) && !(fixture && !visualFixture)) {
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
                        Column(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = confirmedDestination,
                            onValueChange = {
                                lookupRequest++
                                confirmedDestination = it
                                addressSuggestions = emptyList()
                                lookupStatus = ""
                            },
                            label = { Text("Zieladresse") },
                            placeholder = { Text("Ort oder Adresse eingeben") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { destinationFocus.clearFocus() }),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        destinationFocus.clearFocus()
                                        val query = confirmedDestination.trim()
                                        if (query.isNotBlank()) {
                                            lookupRequest++
                                            val requestId = lookupRequest
                                            addressSuggestions = emptyList()
                                            lookupStatus = "Adresse wird geprüft …"
                                            DestinationLookup.search(this@MainActivity, query) { results ->
                                                if (requestId == lookupRequest) {
                                                    addressSuggestions = results.take(4)
                                                    when (results.size) {
                                                        0 -> lookupStatus = "Kein Treffer – bitte Adresse präzisieren"
                                                        1 -> {
                                                            confirmedDestination = results.first()
                                                            addressSuggestions = emptyList()
                                                            lookupStatus = "✓ Ziel geprüft"
                                                        }
                                                        else -> lookupStatus = "Passendes Ziel auswählen"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = confirmedDestination.isNotBlank(),
                                    modifier = Modifier.testTag("setup_destination_lookup")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Zieladresse prüfen",
                                        tint = if (lookupStatus.startsWith("✓")) Color(0xFF237A45)
                                            else MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("setup_destination_input")
                        )
                            if (addressSuggestions.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth()
                                        .testTag("setup_destination_suggestions_panel"),
                                    shape = RoundedCornerShape(
                                        topStart = 0.dp, topEnd = 0.dp,
                                        bottomStart = 14.dp, bottomEnd = 14.dp
                                    ),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 2.dp,
                                    shadowElevation = 2.dp
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        addressSuggestions.take(4).forEachIndexed { index, suggestion ->
                                            if (index > 0) HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clickable {
                                                        lookupRequest++
                                                        confirmedDestination = suggestion
                                                        addressSuggestions = emptyList()
                                                        lookupStatus = "✓ Ziel geprüft"
                                                        destinationFocus.clearFocus()
                                                    }
                                                    .testTag("setup_destination_suggestion")
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("⌖", color = MaterialTheme.colorScheme.primary)
                                                Text(suggestion, modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium)
                                                Text("✓", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                            if (lookupStatus.isNotBlank() && addressSuggestions.isEmpty()) {
                                Text(
                                    if (lookupStatus.startsWith("✓")) "✓ Ziel bestätigt"
                                    else lookupStatus,
                                    color = if (lookupStatus.startsWith("✓")) Color(0xFF237A45)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                )
                            }
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
                        // Keep the heading and radio options as one compact group.
                        // A default Material TextButton adds a 48dp touch row and previously
                        // made the gap above the radio options look much larger.
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Verkehrsmittel", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (advancedExpanded) "Optionen ▲" else "Optionen ▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B4CA0),
                                modifier = Modifier.testTag("advanced_options_toggle")
                                    .clickable { advancedExpanded = !advancedExpanded }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
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
                        } // compact heading + transport radio group
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
                            if (resolvedWalkingTarget?.first != destination) resolvedWalkingTarget = null
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
                                WalkingNavigation.fetch(this@MainActivity, start, destination,
                                    resolvedWalkingTarget?.takeIf { it.first == destination }?.second) { walkResult ->
                                    if (walkingId == walkingRequestId && walkingOnlyFallback) {
                                        walkingRoute = walkResult.getOrNull()
                                        walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage
                                            ?: if (walkResult.getOrNull()?.estimatedDestination == true) "⚠ Zielposition anhand des Namens geschätzt – Karte prüfen" else ""
                                    }
                                }
                            } else {
                                routeStatus = reason + " Für Fußnavigation werden GPS und Ziel benötigt."
                            }
                        }
                        val searchConnections: () -> Unit = {
                                val start = usableLocation
                                if (start != null) {
                                // Capture a fresh departure instant for EVERY "Jetzt" search,
                                // including retries after the user has been delayed.
                                val nowMillis = System.currentTimeMillis()
                                val searchBaseMillis = if (earlierSearchOffsetMinutes > 0)
                                    nowMillis - earlierSearchOffsetMinutes * 60_000L else nowMillis
                                val nowDepartureMillis = if (journeyTimingMode == "now") searchBaseMillis else null
                                val arrivalBy = if (journeyTimingMode == "planned" && !plannedArrival) null else searchTimeMillis
                                val requestId = ++searchGeneration
                                val previousJourneys = journeys
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
                                                it.destinationCoordinates?.let { point -> resolvedWalkingTarget = destination to point }
                                                journeys = if (earlierSearchOffsetMinutes > 0) {
                                                    EarlierConnectionsResolver.merge(
                                                        previousJourneys, it.options, nowMillis,
                                                        searchTimeMillis ?: nowMillis).map { entry -> entry.option }
                                                } else if (journeyTimingMode == "now") {
                                                    reachableNowJourneys(it.options, nowDepartureMillis ?: nowMillis)
                                                } else it.options
                                                if (journeys.isEmpty()) {
                                                    startWalkingFallback("Keine aktuell noch erreichbare ÖPNV-Verbindung. Bitte Suche erneut starten oder Fußweg zum Ziel prüfen.")
                                                } else {
                                                    // Always calculate a direct walking alternative in parallel
                                                    // with successful public-transport results.
                                                    walkingOnlyFallback = true
                                                    walkingOnlyDashboard = false
                                                    walkingRoute = null
                                                    walkingNavigationOpen = false
                                                    val walkingId = ++walkingRequestId
                                                    walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                                    WalkingNavigation.fetch(this@MainActivity, start, destination,
                                    resolvedWalkingTarget?.takeIf { it.first == destination }?.second) { walkResult ->
                                                        if (walkingId == walkingRequestId &&
                                                            requestId == searchGeneration && walkingOnlyFallback) {
                                                            walkingRoute = walkResult.getOrNull()
                                                            walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage ?: ""
                                                        }
                                                    }
                                                    routeStatus = "${journeys.size} aktuell erreichbare Verbindungen nach ${it.destination.name} geladen"
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
                                    departureAtMillis = nowDepartureMillis ?: if (journeyTimingMode == "planned" && !plannedArrival) plannedAtMillis else null,
                                    startNowMillis = nowDepartureMillis)
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
                        val searchDateLabel = searchTimeMillis?.let {
                            java.text.SimpleDateFormat("EEE, dd.MM. HH:mm", java.util.Locale.GERMANY)
                                .apply { timeZone = java.util.TimeZone.getTimeZone("Europe/Berlin") }
                                .format(java.util.Date(it))
                        } ?: "Jetzt"
                        Text("Suchzeitpunkt: $searchDateLabel",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.testTag("search_time_label"))
                        LaunchedEffect(pendingNewSearch) {
                            if (pendingNewSearch) {
                                pendingNewSearch = false
                                session.resetConnectionChecksForNewJourney()
                                searchConnections()
                            }
                        }
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
                            TextButton(
                                modifier = Modifier.testTag("earlier_connections_button"),
                                enabled = usableLocation != null && destination.isNotBlank(),
                                onClick = {
                                    earlierSearchOffsetMinutes += 30
                                    routeStatus = "Frühere Verbindungen werden geladen …"
                                    searchConnections()
                                }
                            ) { Text("← Frühere Verbindungen") }
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
                        // Compare the direct route against door-to-door public transport time.
                        // For 'now' include time until the departure; never recommend a
                        // scheduled bus that has already departed.
                        val directWalkMinutes = walkingRoute?.let {
                            kotlin.math.ceil(it.totalMeters.toDouble() / (walkingSpeed * 1000.0 / 60.0)).toInt()
                        }
                        fun transitMinutes(trip: de.mvv.mobility.data.JourneyOption): Int? {
                            val dep = trip.departure.split(":").mapNotNull { it.toIntOrNull() }
                            val arr = trip.arrival.split(":").mapNotNull { it.toIntOrNull() }
                            if (dep.size != 2 || arr.size != 2) return null
                            val depMinute = dep[0] * 60 + dep[1]
                            val arrMinute = arr[0] * 60 + arr[1]
                            val rideMinutes = (arrMinute - depMinute + 1440) % 1440
                            if (rideMinutes == 0) return null
                            val nowCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                            nowCal.timeInMillis = clockMillis
                            val nowMinute = nowCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                                nowCal.get(java.util.Calendar.MINUTE)
                            val waitMinutes = if (journeyTimingMode == "now") {
                                val delta = (depMinute - nowMinute + 1440) % 1440
                                // An old same-day departure must not become tomorrow's option.
                                if (delta > 360) return null
                                delta
                            } else 0
                            return rideMinutes + waitMinutes
                        }
                        val bestTransitMinutes = journeys.mapNotNull { transitMinutes(it) }.minOrNull()
                        val walkingFaster = directWalkMinutes != null &&
                            (journeys.isEmpty() || bestTransitMinutes != null &&
                                directWalkMinutes < bestTransitMinutes)
                        if (walkingOnlyFallback && (journeys.isEmpty() || walkingFaster)) {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("walking_only_fallback"),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECE7FF))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(if (walkingFaster && journeys.isNotEmpty())
                                        "🚶 ZU FUSS SCHNELLER" else "🚶 FUSSWEG-ALTERNATIVE",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold, color = Color(0xFF49309B))
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("🚶", style = MaterialTheme.typography.headlineLarge)
                                        Column {
                                            Text("Nur zu Fuß", style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold)
                                            Text("Direkt zum Ziel", style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF665B79))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()) {
                                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
                                            modifier = Modifier.weight(1f)) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text("◷  GEHZEIT", style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF665B79))
                                                Text(directWalkMinutes?.let { "$it Min." } ?: "–",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
                                            modifier = Modifier.weight(1f)) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text("↗  STRECKE", style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF665B79))
                                                Text(walkingRoute?.let { "${it.totalMeters} m" } ?: "–",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (bestTransitMinutes != null) {
                                        Text("🚌 ÖPNV im Vergleich: ca. $bestTransitMinutes Min.",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text("📍 ZIEL", style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF665B79), fontWeight = FontWeight.Bold)
                                            Text(destination, style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 3)
                                        }
                                    }
                                    if (walkingRoute != null) {
                                        val next = navigationLocation?.let {
                                            WalkingNavigation.nextTurn(walkingRoute!!, it,
                                                session.routeProgress(walkingRoute!!, it),
                                                useTrackedProgress = true)
                                        }
                                        Surface(shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFDCD2FB), modifier = Modifier.fillMaxWidth()) {
                                            Row(Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Text(next?.first?.arrow ?: "⌖",
                                                    style = MaterialTheme.typography.headlineLarge,
                                                    color = Color(0xFF49309B))
                                                Column {
                                                    Text("NÄCHSTER SCHRITT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF49309B))
                                                    Text(next?.first?.instruction ?: "Warte auf GPS",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold)
                                                    Text(next?.let { "In ${it.second} m" }
                                                        ?: "Aktueller Standort erforderlich",
                                                        style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                        Button(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                                .testTag("open_walking_only_dashboard"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5635B1)),
                                            onClick = {
                                                walkingOnlyDashboard = true
                                                walkingNavigationOpen = true
                                                selectedTab = CockpitTab.JOURNEY
                                                fullscreenMode = FullscreenMode.NAVIGATION
                                            }
                                        ) { Text("🚶 Navigation starten  →") }
                                        Text("OSM-Fußwegführung ist experimentell. Bitte Weg und Ziel prüfen.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF665B79))
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
                                                    it.destinationCoordinates?.let { point -> resolvedWalkingTarget = destination to point }
                                                    journeys = it.options
                                                    if (it.options.isEmpty()) {
                                                        walkingOnlyFallback = true
                                                        walkingNavigationOpen = true
                                                        walkingRoute = null
                                                        val walkingId = ++walkingRequestId
                                                        walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                                        routeStatus = "Keine passende ÖPNV-Verbindung. Fußweg zum Ziel wird geprüft."
                                                        WalkingNavigation.fetch(this@MainActivity, start, destination,
                                    resolvedWalkingTarget?.takeIf { it.first == destination }?.second) { walkResult ->
                                                            if (walkingId == walkingRequestId && walkingOnlyFallback) {
                                                                walkingRoute = walkResult.getOrNull()
                                                                walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage ?: ""
                                                            }
                                                        }
                                                    } else {
                                                        walkingOnlyFallback = true
                                                        walkingOnlyDashboard = false
                                                        walkingRoute = null
                                                        walkingNavigationOpen = false
                                                        val walkingId = ++walkingRequestId
                                                        walkingRouteStatus = "Fußweg zum Ziel wird berechnet …"
                                                        WalkingNavigation.fetch(this@MainActivity, start, destination,
                                    resolvedWalkingTarget?.takeIf { it.first == destination }?.second) { walkResult ->
                                                            if (walkingId == walkingRequestId &&
                                                                requestId == searchGeneration && walkingOnlyFallback) {
                                                                walkingRoute = walkResult.getOrNull()
                                                                walkingRouteStatus = walkResult.exceptionOrNull()?.localizedMessage ?: ""
                                                            }
                                                        }
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
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("MEINE FAHRT · FUSSWEG", style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF49309B), fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f))
                                        Text(gpsIndicatorText, color = gpsIndicatorColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.testTag("walking_gps_status"))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Text("🚶 Nur zu Fuß", style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            voiceMuted = !voiceMuted
                                            settings.edit().putBoolean("walking_voice_muted", voiceMuted).apply()
                                            if (voiceMuted) speech[0]?.stop()
                                        }, modifier = Modifier.testTag("walking_voice_toggle")) {
                                            Text(if (voiceMuted) "🔇 Stumm" else "🔊 Ton an")
                                        }
                                    }
                                    Text("Ziel: $destination", style = MaterialTheme.typography.bodyMedium)
                                    Text("Fußroute: ca. ${route.totalMeters} m", style = MaterialTheme.typography.titleLarge)
                                    val next = navigationLocation?.let {
                                        WalkingNavigation.nextTurn(route, it, session.routeProgress(route, it),
                                            useTrackedProgress = true)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (next != null) {
                                                Text(next.first.arrow, style = MaterialTheme.typography.displayLarge,
                                                    color = Color(0xFF49309B))
                                                Text("${next.second} m", style = MaterialTheme.typography.headlineLarge,
                                                    fontWeight = FontWeight.Bold)
                                                Text(next.first.instruction, style = MaterialTheme.typography.titleMedium)
                                            } else {
                                                Text("GPS fehlt oder ist zu alt. Abbiegehinweise warten auf GPS.")
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f).height(205.dp)
                                                .testTag("walking_osm_minimap"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White
                                        ) {
                                            WalkingMiniMap(
                                                                      route, navigationLocation,
                                                                      walkedSegments = navigationTraceViewModel.walkedTrace.segments.map { segment ->
                                                                          segment.map { it.latitude to it.longitude }
                                                                      },
                                                                      progressProjection = navigationLocation?.let {
                                                                          session.routeProgress(route, it)
                                                                      },
                                                                      useTrackedProgress = true
                                                                  )
                                        }
                                    }
                                    Text("Karte: © OpenStreetMap-Mitwirkende · Violett: Fußweg · Standort violett",
                                        style = MaterialTheme.typography.labelSmall)
                                    Text("OSM-Fußwegführung ist experimentell. Weg und Ziel bitte prüfen.",
                                        style = MaterialTheme.typography.labelSmall)
                                    OutlinedButton(onClick = {
                                        // Explicit session end is the only operation that clears recorded GPS history.
                                        navigationTraceViewModel.clearSession()
                                        walkingRouteStatus = ""
                                        walkingOnlyDashboard = false
                                        walkingNavigationOpen = false
                                        selectedTab = CockpitTab.CONNECTIONS
                                    }, modifier = Modifier.fillMaxWidth().testTag("end_walking_session")) {
                                        Text("Navigation beenden und Spur löschen")
                                    }
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
                                val targetDay: Long = searchTimeMillis ?: clockMillis
                                fun countdown(clock: String): Long? {
                                    if (!Regex("""\d{1,2}:\d{2}""").matches(clock)) return null
                                    val parts = clock.split(":")
                                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                                    cal.timeInMillis = targetDay
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(java.util.Calendar.MINUTE, parts[1].toInt())
                                    cal.set(java.util.Calendar.SECOND, 0)
                                    cal.set(java.util.Calendar.MILLISECOND, 0)
                                    if (cal.timeInMillis < targetDay - 12 * 60 * 60000L) cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                                    if (cal.timeInMillis > targetDay + 12 * 60 * 60000L) cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
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
                                            WalkingNavigation.nextTurn(walkingRoute!!, navigationLocation!!,
                                                session.routeProgress(walkingRoute!!, navigationLocation!!),
                                                useTrackedProgress = true) else null
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
                                                    Text(dynamicBufferMinutes?.let { "$it Min." } ?: "–",
                                                        color = bufferColor, fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium)
                                                    Text(when {
                                                        dynamicBufferMinutes == null -> "Abfahrt oder Gehzeit unbekannt"
                                                        dynamicBufferMinutes >= 5 -> "Gut in der Zeit · geschätzt"
                                                        dynamicBufferMinutes >= 3 -> "Reserve knapp · geschätzt"
                                                        dynamicBufferMinutes < 0 -> "Bus voraussichtlich nicht erreichbar"
                                                         else -> "Sofort losgehen · kaum Reserve"
                                                    }, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        Text("Restpuffer = Zeit bis Abfahrt minus verbleibende Gehzeit (GPS oder Planwert) · ohne Live-Verspätungen",
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
                                                    WalkingNavigation.fetch(this@MainActivity, start, finalWalkingDestination,
                                                        resolvedWalkingTarget?.takeIf { it.first == finalWalkingDestination }?.second
                                                            ?: chosen.steps.lastOrNull()?.toStop) { result ->
                                                        if (walkingRequestId == requestId) {
                                                            walkingRoute = result.getOrNull()
                                                            walkingRouteStatus = result.exceptionOrNull()?.localizedMessage
                                                                ?: if (result.getOrNull()?.estimatedDestination == true) "⚠ Zielposition anhand des Namens geschätzt – Karte prüfen" else ""
                                                        }
                                                    }
                                                },
                                                enabled = usableLocation != null,
                                                modifier = Modifier.fillMaxWidth().testTag("continue_walking_after_bus")
                                            ) { Text("🚶 Ausgestiegen – zum Ziel navigieren") }
                                            Text("Ziel: $finalWalkingDestination · OSM-Fußweg ab aktuellem GPS-Standort. Bitte Zieladresse und Weg prüfen.",
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (!busDeparturePassed && boarding != null) {
                                            val stop = boarding.fromStop
                                            val stopLabel = stop?.platform?.let { " · Steig $it" }
                                                ?: " · Steig nicht eindeutig"
                                            Text("Einstieg: ${boarding.from}$stopLabel",
                                                style = MaterialTheme.typography.labelMedium)
                                            if (stop == null) {
                                                Text("MVV-Koordinaten fehlen. Rückfall: Suche nach Haltestellenname; Ergebnis vor dem Losgehen auf der Karte prüfen.",
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.labelSmall)
                                            }
                                            Button(onClick = {
                                                val start = usableLocation ?: return@Button
                                                val fallbackName = boarding.from.takeIf { it.isNotBlank() }
                                                if (stop == null && fallbackName == null) return@Button
                                                val requestId = ++walkingRequestId
                                                walkingRoute = null
                                                walkingNavigationOpen = true
                                                fullscreenMode = FullscreenMode.NAVIGATION
                                                walkingRouteStatus = if (stop != null) "Fußweg zum MVV-Einstiegspunkt wird berechnet …" else "⚠ Position anhand des Haltestellennamens geschätzt – Karte prüfen"
                                                val onRoute: (Result<de.mvv.mobility.data.WalkingRoute>) -> Unit = { result ->
                                                    if (walkingRequestId == requestId) {
                                                        walkingRoute = result.getOrNull()
                                                        walkingRouteStatus = result.exceptionOrNull()?.localizedMessage
                                                            ?: if (result.getOrNull()?.estimatedDestination == true) "⚠ Position anhand des Haltestellennamens geschätzt – Karte prüfen" else ""
                                                    }
                                                }
                                                WalkingNavigation.fetch(this@MainActivity, start, fallbackName.orEmpty(), stop, onRoute)
                                            }, enabled = usableLocation != null && (stop != null || boarding.from.isNotBlank()),
                                                modifier = Modifier.fillMaxWidth()) {
                                                Text("🚶 Zum MVV-Einstiegspunkt navigieren")
                                            }
                                        }
                                        if (walkingNavigationOpen) {
                                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()) {
                                                        Text("FUSSWEG-NAVIGATION · OSM", fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(1f))
                                                        Text(gpsIndicatorText, color = gpsIndicatorColor,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.testTag("journey_walking_gps_status"))
                                                        TextButton(onClick = {
                                                            voiceMuted = !voiceMuted
                                                            settings.edit().putBoolean("walking_voice_muted", voiceMuted).apply()
                                                            if (voiceMuted) speech[0]?.stop()
                                                        }, modifier = Modifier.testTag("journey_walking_voice_toggle")) {
                                                            Text(if (voiceMuted) "🔇 Stumm" else "🔊 Ton an")
                                                        }
                                                    }
                                                    val fix = navigationLocation
                                                    val route = walkingRoute
                                                    val next = if (fix != null && route != null)
                                                        WalkingNavigation.nextTurn(route, fix, session.routeProgress(route, fix),
                                                            useTrackedProgress = true) else null
                                                    Row(modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically) {
                                                        Column(Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            if (next != null) {
                                                                Text(next.first.arrow,
                                                                    style = MaterialTheme.typography.displayLarge,
                                                                    color = Color(0xFF49309B))
                                                                Text("${next.second} m",
                                                                    style = MaterialTheme.typography.headlineLarge,
                                                                    fontWeight = FontWeight.Bold)
                                                                Text(next.first.instruction,
                                                                    style = MaterialTheme.typography.titleLarge)
                                                            } else {
                                                                Text(if (route != null) "GPS zu ungenau – bitte freien Himmel abwarten"
                                                                    else walkingRouteStatus.ifBlank { "Route wird geladen …" })
                                                            }
                                                        }
                                                        if (route != null) {
                                                            Surface(modifier = Modifier.weight(1f).height(205.dp)
                                                                .testTag("journey_walking_osm_minimap"),
                                                                shape = RoundedCornerShape(14.dp),
                                                                color = Color(0xFFF2EFFA)) {
                                                                WalkingMiniMap(
                                                                     route, navigationLocation,
                                                                     walkedSegments = navigationTraceViewModel.walkedTrace.segments.map { segment ->
                                                                         segment.map { it.latitude to it.longitude }
                                                                     },
                                                                     progressProjection = navigationLocation?.let {
                                                                         session.routeProgress(route, it)
                                                                     },
                                                                     useTrackedProgress = true
                                                                  )
                                                            }
                                                        }
                                                    }
                                                    if (route != null) {
                                                        if (route.estimatedDestination) Text("⚠ Zielposition geschätzt (Namenssuche) – Karte und Einstieg prüfen",
                                                            color = MaterialTheme.colorScheme.error)
                                                        Text("Route: ca. ${route.totalMeters} m · GPS aktualisiert die Entfernung",
                                                            style = MaterialTheme.typography.labelSmall)
                                                        Text("Karte: © OpenStreetMap-Mitwirkende · Violett: Fußweg · Blau: Position",
                                                            style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    TextButton(onClick = {
                                                        walkingNavigationOpen = false
                                                        walkingRequestId++
                                                    }) { Text("Navigation schließen") }
                                                }
                                            }
                                        }
                                        TextButton(onClick = {
                                            navigationTraceViewModel.clearSession()
                                            dashboardJourney = null
                                            selectedTab = CockpitTab.CONNECTIONS
                                        }, modifier = Modifier.testTag("end_walking_session")) { Text("Navigation beenden und Spur löschen") }
                                        TextButton(onClick = { dashboardJourney = null; fullscreenMode = FullscreenMode.NONE; selectedTab = CockpitTab.CONNECTIONS }) { Text("Dashboard schließen") }
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
                                modifier = Modifier.fillMaxWidth().testTag("journey_summary"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = if (index == 0) Color(0xFFF0ECFF) else Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (index == 0) 4.dp else 2.dp)
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val pastDeparture = JourneyTime.isPast(
                                        trip, clockMillis, searchTimeMillis ?: clockMillis
                                    )
                                    if (pastDeparture) Text("ABGEFAHREN – nicht mehr erreichbar",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.testTag("journey_past_badge"))
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
                                    // Show the entire journey at a glance, including every transfer and walk.
                                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White,
                                        modifier = Modifier.fillMaxWidth().testTag("journey_all_legs_summary")) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            trip.steps.forEachIndexed { stepIndex, step ->
                                                if (stepIndex > 0) HorizontalDivider(color = Color(0xFFF0EDF6))
                                                Row(verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("journey_summary_leg_${stepIndex}")) {
                                                    Text(TransitModeIcon.symbol(step.product, step.line, step.walking),
                                                        color = if (TransitModeIcon.symbol(step.product, step.line) == "Ⓢ") Color(0xFF198754) else Color.Unspecified,
                                                        style = MaterialTheme.typography.titleMedium)
                                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Text(if (step.walking) "FUSSWEG · ca. ${step.minutes.coerceAtLeast(0)} Min."
                                                            else "${TransitModeIcon.label(step.product, step.line).uppercase()} ${step.line.ifBlank { "" }}".trim(),
                                                            color = Color(0xFF62577D),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold)
                                                        Text("${step.from.ifBlank { "Startpunkt" }} → ${step.to.ifBlank { "Zielpunkt" }}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium)
                                                    }
                                                    if (step.departure.isNotBlank() || step.arrival.isNotBlank()) {
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(step.departure.ifBlank { "–" },
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold)
                                                            Text(step.arrival.ifBlank { "–" },
                                                                style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { dashboardJourney = index; selectedTab = CockpitTab.JOURNEY; fullscreenMode = FullscreenMode.DASHBOARD },
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
                                                                Text(TransitModeIcon.symbol(step.product, step.line, step.walking),
                                                                    color = if (!step.walking && TransitModeIcon.symbol(step.product, step.line) == "Ⓢ") Color(0xFF198754) else Color.Unspecified,
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
                    fun walkingNavigationLocationTurn(): Pair<de.mvv.mobility.data.WalkingTurn, Int>? {
                        val fix = navigationLocation ?: return null
                        val route = walkingRoute ?: return null
                        return de.mvv.mobility.data.WalkingNavigation.nextTurn(
                            route, fix, session.routeProgress(route, fix), useTrackedProgress = true
                        )
                    }
                    if (fullscreenMode == FullscreenMode.NONE) {
                        CockpitNavigation(selectedTab) { tab -> selectedTab = tab }
                    }
                    if (fullscreenMode != FullscreenMode.NONE) {
                        BackHandler {
                            fullscreenMode = if (fullscreenMode == FullscreenMode.NAVIGATION)
                                FullscreenMode.DASHBOARD else FullscreenMode.NONE
                        }
                        Box(Modifier.fillMaxSize().background(Color(0xFFF7F5FC))) {
                            if (fullscreenMode == FullscreenMode.DASHBOARD) {
                                val chosen = journeys.getOrNull(dashboardJourney ?: 0)
                                val upcoming = displayConnection
                                val departureClock = upcoming?.step?.departure
                                    ?: chosen?.departure.orEmpty()
                                val boardingName = chosen?.steps?.firstOrNull { !it.walking }?.from.orEmpty()
                                val boardingStop = chosen?.boardingStop()
                                val platform = boardingStop?.platform.orEmpty()
                                val boardingDistance = walkingRoute
                                    ?.takeIf { it.destination != null && it.destination == boardingStop }
                                    ?.totalMeters
                                // JourneyOption.walkingMinutes is the total for the whole
                                // itinerary, not a live estimate to the upcoming transfer.
                                val bufferText = "unbekannt – Gehzeit zum Anschluss nicht bestimmt"
                                val departureInstant = chosen?.let {
                                    JourneyTime.departureMillis(it, searchTimeMillis ?: clockMillis)
                                }
                                val nextInstant: Long? = listOfNotNull<Long>(
                                    upcoming?.departureMillis, departureInstant)
                                    .filter { it >= clockMillis }.minOrNull()
                                val countdownLabel = when {
                                    upcoming != null && upcoming.departureMillis >= clockMillis ->
                                        "Noch ${((upcoming.departureMillis - clockMillis) / 60_000L).coerceAtLeast(0)} Min. bis Anschluss"
                                    nextInstant != null ->
                                        "Noch ${((nextInstant - clockMillis) / 60_000L).coerceAtLeast(0)} Min. bis Abfahrt"
                                    departureInstant != null -> "Abfahrt vorbei"
                                    else -> "Zeitpunkt unbekannt"
                                }
                                val countdownContext = when {
                                    upcoming != null && upcoming.departureMillis >= clockMillis ->
                                        "${upcoming.step.line} · ${upcoming.step.departure} · ${upcoming.step.to}"
                                    else -> "${chosen?.lines?.joinToString(", ").orBlank()} · ${chosen?.departure.orBlank()}"
                                }
                                val warningsFull = buildList {
                                    val hasVehicleLeg = chosen?.steps?.any { !it.walking } == true
                                    if (hasVehicleLeg && boardingStop == null) add("⚠ MVV-Einstiegskoordinaten fehlen – Einstieg auf Karte prüfen")
                                    if (navigationLocation == null) add("⚠ GPS nicht aktuell – Anzeige kann veraltet sein")
                                    if (hasVehicleLeg && boardingStop?.platform.isNullOrBlank()) add("⚠ Steig nicht eindeutig")
                                }
                                FullscreenJourneyDashboard(
                                    countdown = countdownLabel,
                                    countdownContext = countdownContext,
                                    gpsStatus = if (navigationLocation != null) "GPS LIVE" else "GPS veraltet",
                                    leaveAt = chosen?.steps?.firstOrNull { it.walking }?.departure.orBlank(),
                                    line = chosen?.lines?.joinToString(", ").orBlank(),
                                    departure = departureClock,
                                    boarding = boardingName,
                                    platform = platform,
                                    walking = chosen?.walkingMinutes?.let { "$it Min. gesamt" }.orBlank(),
                                    distance = boardingDistance?.let { "ca. $it m" }.orBlank(),
                                    buffer = bufferText,
                                    arrival = chosen?.arrival.orBlank(),
                                    warnings = warningsFull,
                                    upcoming = upcoming,
                                    checkState = displayConnection?.stepIndex?.let { connectionChecks[it] },
                                    onBack = { fullscreenMode = FullscreenMode.NONE },
                                    onCheckIn = { upcoming?.let { session.checkIn(it.stepIndex) } },
                                    onMissed = { upcoming?.let { session.markConnectionMissed(it.stepIndex) } },
                                    onNewSearch = {
                                        session.resetConnectionChecksForNewJourney()
                                        fullscreenMode = FullscreenMode.NONE
                                        selectedTab = CockpitTab.CONNECTIONS
                                        pendingNewSearch = true
                                    },
                                    onNavigate = {
                                        // A previously loaded walking alternative may route to the
                                        // journey's final destination, not this boarding stop.
                                        if (chosen?.hasRouteToBoardingStop(walkingRoute) != true) {
                                            walkingRoute = null
                                            val start = usableLocation
                                            val stop = boardingStop
                                            val fallback = chosen?.steps?.firstOrNull { !it.walking }?.from
                                            if (start != null && (stop != null || !fallback.isNullOrBlank())) {
                                                val requestId = ++walkingRequestId
                                                walkingRouteStatus = "Fußweg wird berechnet …"
                                                WalkingNavigation.fetch(this@MainActivity,
                                                    start, fallback.orEmpty(), stop) { result ->
                                                    if (walkingRequestId == requestId) {
                                                        walkingRoute = result.getOrNull()
                                                        walkingRouteStatus = result.exceptionOrNull()?.localizedMessage.orEmpty()
                                                    }
                                                }
                                            }
                                        }
                                        fullscreenMode = FullscreenMode.NAVIGATION
                                    },
                                    onEndSession = {
                                        navigationTraceViewModel.clearSession()
                                        fullscreenMode = FullscreenMode.NONE
                                    }
                                )
                            } else {
                                val route = walkingRoute
                                if (route != null) {
                                    val next = walkingNavigationLocationTurn()
                                    FullscreenWalkNavigation(
                                        route = route,
                                        position = navigationLocation,
                                        walkedSegments = navigationTraceViewModel.walkedTrace.segments.map { segment ->
                                            segment.map { it.latitude to it.longitude }
                                        },
                                        turn = next,
                                        distanceText = next?.second?.let { "$it m" }.orBlank(),
                                        walkingTimeText = "GPS-basiert",
                                        departureText = selectedJourney?.departure.orBlank(),
                                        bufferText = "unbekannt – Gehzeit nicht belegt",
                                        boardingText = route.destination?.platform?.let { "Steig $it" }
                                            ?: "Haltestelle prüfen",
                                        warnings = buildList {
                                            if (navigationLocation == null) add("⚠ GPS nicht aktuell – Position veraltet")
                                            if (route.estimatedDestination) add("⚠ Ziel nur anhand des Namens geschätzt")
                                        },
                                        onBack = { fullscreenMode = FullscreenMode.DASHBOARD },
                                        onEnd = {
                                            navigationTraceViewModel.clearSession()
                                            fullscreenMode = FullscreenMode.NONE
                                            walkingNavigationOpen = false
                                            walkingOnlyDashboard = false
                                        },
                                        onRecenter = { }
                                    )
                                } else {
                                    Text("Fußwegroute wird geladen …",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.testTag("fullscreen_navigation_loading"))
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
