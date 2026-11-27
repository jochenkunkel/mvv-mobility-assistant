package de.mvv.mobility

import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * #24: verify the actual window insets, not merely the absence of app bottom navigation.
 * Run on API-35 emulator and repeat manually on Samsung three-button/gesture navigation.
 */
@RunWith(AndroidJUnit4::class)
class ImmersiveDashboardInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun barsVisible(activity: MainActivity, type: Int): Boolean? =
        ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(type)

    @Test fun dashboardHidesOnlySystemNavigationAndRestoresItOnExit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
            .putExtra("mein_weg_test_fullscreen", "dashboard")

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()
            compose.waitUntil(10_000) {
                var hidden = false
                scenario.onActivity { activity ->
                    hidden = barsVisible(activity, WindowInsetsCompat.Type.navigationBars()) == false
                }
                hidden
            }
            scenario.onActivity { activity ->
                assertFalse(barsVisible(activity, WindowInsetsCompat.Type.navigationBars()) ?: true)
                assertTrue(barsVisible(activity, WindowInsetsCompat.Type.statusBars()) ?: false)
            }

            // Recreated Activities must reapply immersive state, not just retain Compose UI.
            scenario.recreate()
            compose.onNodeWithTag("fullscreen_dashboard").assertExists()
            compose.waitUntil(10_000) {
                var hidden = false
                scenario.onActivity { activity ->
                    hidden = barsVisible(activity, WindowInsetsCompat.Type.navigationBars()) == false
                }
                hidden
            }

            compose.onNodeWithTag("fullscreen_dashboard_back").performClick()
            compose.onNodeWithTag("fullscreen_dashboard").assertDoesNotExist()
            compose.waitUntil(10_000) {
                var visible = false
                scenario.onActivity { activity ->
                    visible = barsVisible(activity, WindowInsetsCompat.Type.navigationBars()) == true
                }
                visible
            }
        }
    }
}
