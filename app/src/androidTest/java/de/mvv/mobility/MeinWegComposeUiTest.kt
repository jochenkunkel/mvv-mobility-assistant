package de.mvv.mobility

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
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
}
