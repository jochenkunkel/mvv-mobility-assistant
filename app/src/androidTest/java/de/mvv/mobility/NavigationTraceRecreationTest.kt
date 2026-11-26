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
                // A new fix after recreation must append to the retained history.
                restored.acceptFix(TraceFix(48.1002, 11.5000, 23_000L, 5f))
                assertEquals(3, restored.walkedTrace.fixes.size)
            }
            // A second recreation must not silently start a fresh GPS session.
            scenario.recreate()
            scenario.onActivity { activity ->
                val restoredAgain = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertSame(original, restoredAgain)
                assertEquals(3, restoredAgain.walkedTrace.fixes.size)
                assertEquals(fixes, restoredAgain.walkedTrace.fixes.take(2))
            }
        }
    }

    @Test fun explicitSessionEndClearsRetainedTraceAcrossRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val owner = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                owner.acceptFix(TraceFix(48.1000, 11.5000, 1_000L, 5f))
                assertEquals(1, owner.walkedTrace.fixes.size)
                owner.clearSession()
                assertEquals(emptyList<TraceFix>(), owner.walkedTrace.fixes)
                assertEquals(0, owner.walkedTrace.segments.size)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val owner = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(emptyList<TraceFix>(), owner.walkedTrace.fixes)
                owner.acceptFix(TraceFix(48.2000, 11.6000, 500L, 5f))
                assertEquals(1, owner.walkedTrace.fixes.size)
                assertEquals(500L, owner.walkedTrace.fixes.single().timestampMillis)
            }
        }
    }
}
