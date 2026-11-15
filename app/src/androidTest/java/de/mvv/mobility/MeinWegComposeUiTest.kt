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

    @Test fun modernStartShowsCompactAppointmentAndExpandablePicker() = launchFixture {
        compose.onNodeWithTag("app_title").assertExists()
        compose.onNodeWithText("Nächster Termin").assertExists()
        compose.onNodeWithText("Anderen Termin wählen").performScrollTo().performClick()
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

    @Test fun screenshotFixtureStartAndRouteAreSavedForVisualReview() = launchFixture {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ui-screenshots").apply { mkdirs() }
        fun save(name: String) {
            compose.waitForIdle()
            val image = compose.onRoot().captureToImage().asAndroidBitmap()
            File(folder, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        save("01-start.png")
        compose.onNodeWithTag("mvv_search_button").performScrollTo().performClick()
        compose.onNodeWithTag("journey_summary").performScrollTo().assertExists()
        save("02-verbindungen.png")
        compose.onNodeWithTag("open_walking_dashboard").performScrollTo().performClick()
        compose.onNodeWithTag("walking_dashboard").performScrollTo().assertExists()
        save("03-dashboard.png")
        org.junit.Assert.assertEquals(3, folder.listFiles()?.count { it.extension == "png" })
    }

}
