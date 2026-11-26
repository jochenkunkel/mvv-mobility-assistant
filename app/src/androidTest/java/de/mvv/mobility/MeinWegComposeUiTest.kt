package de.mvv.mobility

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import de.mvv.mobility.ui.components.CockpitTab
import de.mvv.mobility.data.JourneyOption
import de.mvv.mobility.data.JourneyStep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native Compose end-to-end fixture test; no screen coordinates or external MVV calls. */
@RunWith(AndroidJUnit4::class)
class MeinWegComposeUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun fixtureCalendarGpsAndMvvRouteAreRendered() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<ComponentActivity>(intent).use {
            compose.onNodeWithTag("app_title").assertExists()
            compose.onNodeWithTag("mvv_search_button")
                .performScrollTo()
                .assertIsEnabled()
                .performClick()
            compose.onNodeWithTag("journey_summary").performScrollTo().assertExists()
            compose.onNodeWithTag("route_status")
                .performScrollTo()
                .assertTextContains("Testverbindung geladen")
        }
    }

    private fun launchFixture(block: () -> Unit) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { block() }
    }

    @Test fun locationStatusIsAccessibleAndFixtureRouteRemainsUsable() = launchFixture {
        compose.onNodeWithTag("setup_location_status").performScrollTo().assertExists()
        compose.onNodeWithTag("mvv_search_button").performScrollTo().assertIsEnabled()
    }

    @Test fun bottomNavigationKeepsThreeDestinationsAccessible() = launchFixture {
        compose.onNodeWithTag("cockpit_bottom_navigation").assertExists()
        compose.onNodeWithTag("tab_connections").performClick()
        compose.onNodeWithTag("tab_journey").performClick()
        compose.onNodeWithTag("tab_start").performClick()
        compose.onNodeWithTag("app_title").assertExists()
    }

    @Test fun cockpitProvidesThreeInteractiveEntryPoints() = launchFixture {
        compose.onNodeWithTag("cockpit_overview").assertExists()
        compose.onNodeWithTag("cockpit_appointment").assertExists().performClick()
        compose.onNodeWithTag("cockpit_connections").assertExists().performClick()
        compose.onNodeWithTag("cockpit_journey").assertExists().performClick()
        compose.onNodeWithTag("mvv_search_button").performScrollTo().assertExists()
    }

    @Test fun modernStartShowsCompactAppointmentAndExpandablePicker() = launchFixture {
        compose.onNodeWithTag("app_title").assertExists()
        compose.onNodeWithText("Nächster Termin").assertExists()
        compose.onNodeWithTag("calendar_appointment").performScrollTo().performClick()
        compose.onNodeWithTag("calendar_more").performScrollTo().assertExists()
        compose.onNodeWithTag("calendar_more").performClick()
        compose.onNodeWithText("Testtermin 11", substring = true).performScrollTo().assertExists()
    }

    @Test fun arrivalSliderIsInteractiveAndUpdatesLabel() = launchFixture {
        compose.onNodeWithTag("arrival_buffer_slider").performScrollTo().assertExists()
        compose.onNodeWithText("10 Min. früher").assertExists()
        compose.onNodeWithTag("arrival_buffer_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
        compose.onNodeWithText("10 Min. früher").assertDoesNotExist()
        compose.onNodeWithText("60 Min. früher").assertExists()
        compose.onNodeWithTag("arrival_buffer_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        compose.onNodeWithText("5 Min. früher").assertExists()
    }

    /** #13: an unsaved arrival-buffer edit must survive Activity recreation. */
    @Test fun arrivalBufferSelectionSurvivesActivityRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithTag("arrival_buffer_slider").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
            compose.onNodeWithText("60 Min. früher").assertExists()

            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithText("60 Min. früher").assertExists()
        }
    }

    @Test fun compactFilterChipsAndAdvancedSettingsWork() = launchFixture {
        compose.onNodeWithTag("route_filter_mode_ANY").performScrollTo().assertExists()
        compose.onNodeWithTag("route_filter_mode_ONLY_LINE_WITH_WALK").performClick()
        compose.onNodeWithTag("route_filter_query").assertExists()
        compose.onNodeWithTag("route_filter_mode_ANY").performClick()
        compose.onNodeWithTag("route_filter_query").assertDoesNotExist()
        compose.onNodeWithText("Optionen ▼").performScrollTo().performClick()
        compose.onNodeWithText("Gehgeschwindigkeit").assertExists()
        compose.onNodeWithText("Optionen ▲").performClick()
        compose.onNodeWithText("Gehgeschwindigkeit").assertDoesNotExist()
    }

    @Test fun routeCardOpensAndClosesWalkingDashboard() = launchFixture {
        compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
        compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
        compose.onNodeWithTag("walking_dashboard").performScrollTo().assertExists()
        compose.onNodeWithTag("walking_dashboard").assertExists()
        // Design 5: the visible fullscreen back action closes the dashboard.
        compose.onNodeWithTag("fullscreen_dashboard_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("fullscreen_dashboard").assertDoesNotExist()
        // Back returns to the regular app without discarding the active journey.
        compose.onNodeWithTag("walking_dashboard").assertExists()
    }

    /** #11: OSM tiles require visible attribution, also while navigating. */
    @Test fun walkingMiniMapShowsOpenStreetMapAttribution() = launchFixture {
        compose.onNodeWithTag("walking_dashboard").performScrollTo().assertExists()
        compose.onNodeWithTag("journey_walking_osm_minimap").performScrollTo().assertExists()
        compose.onNodeWithTag("osm_attribution").performScrollTo()
            .assertTextEquals("© OpenStreetMap contributors")
    }

    /** Regression for #13: recreation must not silently discard an active walking dashboard. */
    @Test fun activeWalkingDashboardSurvivesActivityRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
            compose.onNodeWithTag("journey_summary").performScrollTo().assertExists()
            compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
            compose.onNodeWithTag("walking_dashboard").assertExists()
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()

            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("walking_dashboard").assertExists()
            compose.onNodeWithTag("journey_summary").assertExists()
        }
    }

    /** #13/#14: configuration, selected connection, active route and trace survive recreation. */
    @Test fun activeRouteStateAndTraceSurviveRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
            compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
            compose.onNodeWithTag("walking_dashboard").assertExists()
            lateinit var before: NavigationSessionViewModel
            lateinit var traceBefore: List<Pair<Double, Double>>
            scenario.onActivity { activity ->
                val session = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                before = session
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                trace.acceptFix(de.mvv.mobility.data.TraceFix(48.1371, 11.5761, 1_000L, 5f))
                traceBefore = trace.walkedTrace.fixes.map { it.latitude to it.longitude }
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val after = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                assertSame(before, after)
                assertEquals("Olympiapark München", after.confirmedDestination)
                assertEquals(CockpitTab.JOURNEY, after.selectedTab)
                assertEquals(0, after.dashboardJourney)
                assertEquals(1, after.journeys.size)
                assertNotNull(after.walkingRoute)
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(traceBefore, trace.walkedTrace.fixes.map { it.latitude to it.longitude })
                // A fix arriving after recreation must still be recorded.
                trace.acceptFix(de.mvv.mobility.data.TraceFix(48.1372, 11.5762, 12_000L, 5f))
                assertEquals(traceBefore.size + 1, trace.walkedTrace.fixes.size)
            }
        }
    }

    /** Explicit navigation end clears the GPS trace; recreation alone must not. */
    @Test fun explicitSessionEndClearsTraceAcrossRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[NavigationTraceViewModel::class.java].acceptFix(
                    de.mvv.mobility.data.TraceFix(48.1371, 11.5761, 1_000L, 5f))
            }
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                    .fullscreenMode = FullscreenMode.DASHBOARD
            }
            compose.onNodeWithTag("fullscreen_end_session").performClick()
            scenario.recreate()
            compose.waitForIdle()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java].walkedTrace
                assertEquals(0, trace.fixes.size)
            }
        }
    }

    /** #20: capture the actually rendered fullscreen dashboard for design review. */
    @Test fun captureFullscreenDashboardForDesignReview() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                    .fullscreenMode = FullscreenMode.DASHBOARD
            }
            compose.waitForIdle()
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()
            val folder = File(context.getExternalFilesDir(null), "design-review").apply { mkdirs() }
            compose.onRoot().captureToImage().asAndroidBitmap()
                .let { bitmap -> File(folder, "fullscreen-dashboard.png")
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }

    /** #17: compact dashboard opens fullscreen without the app tab bar. */
    @Test fun fullscreenDashboardShowsDynamicJourneyState() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                s.fullscreenMode = FullscreenMode.DASHBOARD
            }
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()
            compose.onNodeWithTag("fullscreen_departure").assertExists()
            compose.onNodeWithTag("fullscreen_buffer").assertExists()
            compose.onNodeWithTag("fullscreen_buffer")
                .assertTextContains("Gehzeit zum Anschluss nicht bestimmt", substring = true)
            compose.onNodeWithTag("fullscreen_navigate").assertExists()
            compose.onNodeWithTag("cockpit_bottom_navigation").assertDoesNotExist()
            compose.onNodeWithTag("fullscreen_dashboard_back").performClick()
            compose.onNodeWithTag("cockpit_bottom_navigation").assertExists()
        }
    }

    /** #15: a real upcoming transfer can be checked in once; missed offers a new search. */
    @Test fun upcomingConnectionCheckInAndMissedAreIdempotent() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        val departure = SimpleDateFormat("HH:mm", Locale.GERMANY).apply {
                timeZone = java.util.TimeZone.getTimeZone("Europe/Berlin")
            }.format(Date(System.currentTimeMillis() + 10 * 60_000L))
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                s.journeys = listOf(JourneyOption(departure, "23:59", "U3", 0, 5,
                    listOf("U3"), listOf("U-Bahn"),
                    listOf(JourneyStep("U3", "U-Bahn", "A", "B", departure, "23:59", false, 10))))
                s.dashboardJourney = 0
                s.fullscreenMode = FullscreenMode.DASHBOARD
            }
            compose.onNodeWithTag("connection_checkin").performClick()
            compose.onNodeWithTag("connection_checkin").assertTextEquals("✓ Check-in")
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                s.resetConnectionChecksForNewJourney()
            }
            compose.onNodeWithTag("connection_missed").performClick()
            compose.onNodeWithTag("new_connection_search").assertExists()
            compose.onNodeWithTag("new_connection_search").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("fullscreen_dashboard").assertDoesNotExist()
        }
    }

    /** #18: map-focused fullscreen navigation can return without losing the journey. */
    @Test fun fullscreenNavigationBackKeepsSelectedJourney() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                s.dashboardJourney = 0
                s.fullscreenMode = FullscreenMode.NAVIGATION
            }
            compose.onNodeWithTag("fullscreen_navigation").assertExists()
            compose.onNodeWithTag("navigation_turn_banner").assertExists()
            compose.onNodeWithTag("fullscreen_nav_back").performClick()
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                assertEquals(0, s.dashboardJourney)
                assertEquals(FullscreenMode.DASHBOARD, s.fullscreenMode)
            }
        }
    }

    /** #16: earlier-connections action is visible and marks past departures. */
    @Test fun earlierConnectionsActionMarksPastDepartures() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithTag("earlier_connections_button").performScrollTo().assertExists()
            compose.onNodeWithTag("earlier_connections_button").performClick()
            compose.waitForIdle()

            // Avoid depending on the emulator's time of day: exercise the card
            // with an explicit past EFA instant after the earlier-search action.
            scenario.onActivity { activity ->
                val session = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                val departure = System.currentTimeMillis() - 60 * 60_000L
                session.journeys = listOf(JourneyOption(
                    departure = "09:10",
                    arrival = "09:30",
                    summary = "U3",
                    changes = 0,
                    walkingMinutes = 10,
                    lines = listOf("U3"),
                    products = listOf("U-Bahn"),
                    steps = listOf(JourneyStep("U3", "U-Bahn", "A", "B",
                        "09:10", "09:30", false, 20)),
                    departureInstantMillis = departure
                ))
            }
            compose.waitForIdle()
            compose.onNodeWithTag("journey_past_badge").performScrollTo().assertExists()
        }
    }

    /** #19: the selected appointment day must survive rotation and be visible. */
    @Test fun searchDateSurvivesActivityRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            val tomorrow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Berlin"))
                .apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            scenario.onActivity { activity ->
                val s = ViewModelProvider(activity)[NavigationSessionViewModel::class.java]
                s.journeyTimingMode = "planned"
                s.plannedAtMillis = tomorrow
            }
            compose.waitForIdle()
            compose.onNodeWithTag("search_time_label").assertExists()
            compose.onNodeWithTag("search_time_label").assertTextContains(
                java.text.SimpleDateFormat("dd.MM.", java.util.Locale.GERMANY).format(java.util.Date(tomorrow)), substring = true)
            scenario.recreate()
            compose.onNodeWithTag("search_time_label").assertTextContains(
                java.text.SimpleDateFormat("dd.MM.", java.util.Locale.GERMANY).format(java.util.Date(tomorrow)), substring = true)
        }
    }

    @Test fun settingsOpenFromEveryTabAndCloseCleanly() = launchFixture {
        for (tab in listOf("tab_start", "tab_connections", "tab_journey")) {
            compose.onNodeWithTag(tab).performClick()
            compose.onNodeWithTag("settings_toggle").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("settings_panel").assertExists()
            compose.onNodeWithText("Bevorzugte Linien").performScrollTo().assertExists()
            compose.onNodeWithText("Schließen").performScrollTo().performClick()
            compose.onNodeWithTag("settings_panel").assertDoesNotExist()
        }
    }

    @Test fun screenshotFixtureStartAndRouteAreSavedForVisualReview() {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
            .putExtra("mein_weg_visual_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ui-screenshots").apply { mkdirs() }
        fun save(name: String) {
            compose.waitForIdle()
            val image = compose.onRoot().captureToImage().asAndroidBitmap()
            File(folder, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithTag("cockpit_overview").assertDoesNotExist()
        compose.onNodeWithTag("setup_step_appointment").assertExists()
        compose.onNodeWithTag("setup_step_address").assertExists()
        compose.onNodeWithTag("setup_step_preferences").assertExists()
        compose.onNodeWithTag("setup_next_address").performScrollTo().performClick()
        compose.onNodeWithTag("setup_next_preferences").performScrollTo().performClick()
        compose.onNodeWithTag("setup_find_connections").performScrollTo().assertExists()
        compose.onNodeWithTag("tab_start").performClick()
        save("01-start.png")
        compose.onNodeWithTag("tab_connections").performClick()
        compose.onNodeWithTag("cockpit_overview").assertDoesNotExist()
        compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
        compose.onNodeWithTag("journey_summary").performScrollTo().assertExists()
        save("02-verbindungen.png")
        compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
        compose.onNodeWithTag("walking_dashboard").performScrollTo().assertExists()
        compose.onNodeWithTag("fullscreen_dashboard").assertExists()
        save("03-dashboard.png")
        org.junit.Assert.assertEquals(3, folder.listFiles()?.count { it.extension == "png" })
        }
    }

}
