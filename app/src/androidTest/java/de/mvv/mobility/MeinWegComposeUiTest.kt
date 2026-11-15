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
            compose.onNodeWithTag("journey_summary")
                .performScrollTo()
                .assertTextContains("U3")
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

    @Test fun compactFilterChipsAndAdvancedSettingsWork() = launchFixture {
        compose.onNodeWithTag("route_filter_mode_ANY").performScrollTo().assertExists()
        compose.onNodeWithTag("route_filter_mode_ONLY_LINE_WITH_WALK").performClick()
        compose.onNodeWithTag("route_filter_query").assertExists()
        compose.onNodeWithTag("route_filter_mode_ANY").performClick()
        compose.onNodeWithTag("route_filter_query").assertDoesNotExist()
        compose.onNodeWithText("Mehr Optionen").performScrollTo().performClick()
        compose.onNodeWithText("Gehgeschwindigkeit").assertExists()
        compose.onNodeWithText("Weniger").performClick()
        compose.onNodeWithText("Gehgeschwindigkeit").assertDoesNotExist()
    }

    @Test fun routeCardOpensAndClosesWalkingDashboard() = launchFixture {
        compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
        compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
        compose.onNodeWithTag("walking_dashboard").performScrollTo().assertExists()
        compose.onNodeWithTag("walking_dashboard").assertExists()
        compose.onNodeWithText("Dashboard schließen").performScrollTo().performClick()
        compose.onNodeWithTag("walking_dashboard").assertDoesNotExist()
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
        compose.onNodeWithTag("tab_journey").performClick()
        compose.onNodeWithTag("walking_dashboard").assertExists()
        save("03-dashboard.png")
        org.junit.Assert.assertEquals(3, folder.listFiles()?.count { it.extension == "png" })
        }
    }

}
