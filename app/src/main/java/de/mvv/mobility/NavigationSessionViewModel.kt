package de.mvv.mobility

import android.content.SharedPreferences
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import de.mvv.mobility.data.JourneyOption
import de.mvv.mobility.data.JourneyStep
import de.mvv.mobility.data.MvvJourneyRepository
import de.mvv.mobility.data.StopPosition
import de.mvv.mobility.data.WalkingRoute
import de.mvv.mobility.data.WalkingTurn
import de.mvv.mobility.data.RouteFilterMode
import de.mvv.mobility.ui.components.CockpitTab

/**
 * Activity-scoped owner of the navigation session.
 *
 * ViewModels outlive Activity recreation, so configuration, selected journey,
 * planned route, active navigation flags and request generations survive a
 * rotation. Like [NavigationTraceViewModel] this state is deliberately not
 * persisted across process death and is never written to a saved-state Bundle:
 * it can contain location-derived data.
 */
class NavigationSessionViewModel(
    preferences: SharedPreferences,
    fixture: Boolean
) : ViewModel() {

    var journeyTimingMode by mutableStateOf<String?>(null)
    var plannedArrival by mutableStateOf(false)
    var plannedAtMillis by mutableStateOf(System.currentTimeMillis() + 60 * 60_000L)
    var selectedAppointmentStart by mutableStateOf<Long?>(null)

    var confirmedDestination by mutableStateOf("")
    var addressSuggestions by mutableStateOf<List<String>>(emptyList())
    var lookupStatus by mutableStateOf("")
    var lookupRequest by mutableIntStateOf(0)
    var mvvCandidates by mutableStateOf<List<MvvJourneyRepository.StopCandidate>>(emptyList())

    var arrivalBufferMinutes by mutableIntStateOf(
        preferences.getInt("arrival_buffer_minutes", 10).coerceIn(5, 60)
    )
    var walkingSpeed by mutableFloatStateOf(preferences.getFloat("walking_speed_kmh", 3.0f))
    var routeFilterMode by mutableStateOf(
        runCatching {
            RouteFilterMode.valueOf(
                preferences.getString("route_filter_mode", RouteFilterMode.ANY.name)
                    ?: RouteFilterMode.ANY.name
            )
        }.getOrDefault(RouteFilterMode.ANY)
    )
    var routeFilterQuery by mutableStateOf(preferences.getString("route_filter_query", "") ?: "")
    var voiceMuted by mutableStateOf(preferences.getBoolean("walking_voice_muted", false))

    var routeStatus by mutableStateOf("")
    var journeys by mutableStateOf<List<JourneyOption>>(emptyList())
    var expandedJourney by mutableStateOf<Int?>(null)
    var dashboardJourney by mutableStateOf<Int?>(null)
    var walkingRoute by mutableStateOf<WalkingRoute?>(null)
    var resolvedWalkingTarget by mutableStateOf<Pair<String, StopPosition>?>(null)
    var walkingRouteStatus by mutableStateOf("")
    var walkingOnlyFallback by mutableStateOf(false)
    var walkingOnlyDashboard by mutableStateOf(false)
    var walkingNavigationOpen by mutableStateOf(false)
    var journeyLoading by mutableStateOf(false)

    // Generations must survive recreation; otherwise an old callback can match
    // a freshly reset counter and overwrite a newer route or journey list.
    var walkingRequestId by mutableIntStateOf(0)
    var searchGeneration by mutableIntStateOf(0)
    var offRouteFixes by mutableIntStateOf(0)
    var lastOffRouteFixTime by mutableLongStateOf(0L)
    var lastRerouteAt by mutableLongStateOf(0L)
    var rerouting by mutableStateOf(false)

    var selectedTab by mutableStateOf(CockpitTab.START)
    var activeSetupStep by mutableIntStateOf(0)
    var lastSpokenTurnKey by mutableStateOf<String?>(null)
    var speechReady by mutableStateOf(false)

    var location by mutableStateOf<Location?>(
        if (fixture) Location("test").apply {
            latitude = 48.137154
            longitude = 11.576124
        } else null
    )
    var locationStatus by mutableStateOf("Noch kein Standort abgerufen")
    var locationPromptFinished by mutableStateOf(fixture)

    init {
        if (fixture) {
            // Deterministic navigation fixture: Compose instrumentation tests
            // must see a selected connection, a planned route and a destination
            // without any network or runtime permission prompts.
            journeyTimingMode = "calendar"
            arrivalBufferMinutes = 10
            confirmedDestination = "Olympiapark München"
            val boarding = StopPosition(48.1775, 11.5536, "de:09162:600", "2")
            val walking = JourneyStep("", "", "Start", "Olympiapark West",
                "09:00", "09:10", true, 10, null, boarding)
            val transit = JourneyStep("U3", "U-Bahn", "Olympiapark West", "Marienplatz",
                "09:10", "09:30", false, 20, boarding, null)
            journeys = listOf(
                JourneyOption("09:10", "09:30", "U3 → Marienplatz", 0, 10,
                    listOf("U3"), listOf("U-Bahn"), listOf(walking, transit))
            )
            expandedJourney = 0
            dashboardJourney = 0
            walkingRouteStatus = "Testfußweg geladen"
            walkingOnlyFallback = true
            walkingRoute = WalkingRoute(
                turns = listOf(WalkingTurn("Geradeaus", "↑", 48.137154, 11.576124, 120)),
                totalMeters = 120,
                path = listOf(48.137154 to 11.576124, 48.138154 to 11.576124),
                destination = boarding
            )
            walkingNavigationOpen = true
        }
    }

    private var routeForEvidenceReset: WalkingRoute? = null
    private var voiceResetKey: Triple<WalkingRoute?, Boolean, Boolean>? = null

    /**
     * Clear off-route evidence only for a genuinely new route. A recomposition
     * after rotation must not reset evidence that was collected before it.
     */
    fun noteRouteForEvidenceReset(route: WalkingRoute?) {
        if (route !== routeForEvidenceReset) {
            routeForEvidenceReset = route
            offRouteFixes = 0
            lastOffRouteFixTime = 0L
        }
    }

    /** Re-announce only when the active navigation session actually changed. */
    fun noteVoiceSession(route: WalkingRoute?, walkingOnly: Boolean, transitWalking: Boolean) {
        val key = Triple(route, walkingOnly, transitWalking)
        if (key != voiceResetKey) {
            voiceResetKey = key
            lastSpokenTurnKey = null
        }
    }
}
