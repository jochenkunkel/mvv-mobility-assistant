package de.mvv.mobility

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.mvv.mobility.data.TraceFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression for the Activity-scoped trace owner (issue #14).
 * This checks recreation retention only, not route/selection UI state (#13).
 */
@RunWith(AndroidJUnit4::class)
class NavigationTraceRecreationTest {
    @Test fun nonEmptyGpsTraceSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val fixes = listOf(
                TraceFix(48.1000, 11.5000, 1_000L, 5f),
                TraceFix(48.1001, 11.5000, 12_000L, 5f)
            )
            lateinit var original: NavigationTraceViewModel
            scenario.onActivity { activity ->
                original = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                fixes.forEach(original::acceptFix)
                assertEquals(fixes, original.walkedTrace.fixes)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val restored = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertSame(original, restored)
                assertEquals(fixes, restored.walkedTrace.fixes)
                assertEquals(listOf(fixes), restored.walkedTrace.segments)
            }
        }
    }
}
