package de.mvv.mobility

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
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

    /** Fixture mode avoids a runtime permission dialog that would pause the Activity. */
    private fun launchFixture(block: (ActivityScenario<MainActivity>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("mein_weg_test_fixture", true)
        ActivityScenario.launch<MainActivity>(intent).use(block)
    }
    @Test fun nonEmptyGpsTraceSurvivesActivityRecreation() {
        launchFixture { scenario ->
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

    @Test fun boundedGpsHistorySurvivesRecreationWithoutRestoringDiscardedFixes() {
        launchFixture { scenario ->
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                // Separate fixes by a gap to avoid GPS speed filtering.
                repeat(1_501) { index ->
                    trace.acceptFix(TraceFix(48.1000, 11.5000, index * 121_000L, 5f))
                }
                assertEquals(1_500, trace.walkedTrace.fixes.size)
                assertEquals(121_000L, trace.walkedTrace.fixes.first().timestampMillis)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(1_500, trace.walkedTrace.fixes.size)
                assertEquals(121_000L, trace.walkedTrace.fixes.first().timestampMillis)
                assertEquals(1_500, trace.walkedTrace.segments.size)
            }
        }
    }

    @Test fun explicitSessionEndClearsRetainedTraceAcrossRecreation() {
        launchFixture { scenario ->
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
    @Test fun gpsGapSegmentsRemainSeparateAfterRecreation() {
        launchFixture { scenario ->
            val first = TraceFix(48.1000, 11.5000, 1_000L, 5f)
            val afterGap = TraceFix(48.1001, 11.5001, 200_000L, 5f)
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                trace.acceptFix(first)
                trace.acceptFix(afterGap)
                assertEquals(listOf(listOf(first), listOf(afterGap)), trace.walkedTrace.segments)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(listOf(first, afterGap), trace.walkedTrace.fixes)
                assertEquals(listOf(listOf(first), listOf(afterGap)), trace.walkedTrace.segments)
            }
        }
    }

    @Test fun gpsGapAfterRecreationStartsNewSegmentWithoutLosingHistory() {
        launchFixture { scenario ->
            val before = TraceFix(48.1000, 11.5000, 1_000L, 5f)
            val after = TraceFix(48.1001, 11.5001, 200_000L, 5f)
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                trace.acceptFix(before)
                assertEquals(listOf(listOf(before)), trace.walkedTrace.segments)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                trace.acceptFix(after)
                assertEquals(listOf(before, after), trace.walkedTrace.fixes)
                assertEquals(listOf(listOf(before), listOf(after)), trace.walkedTrace.segments)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(listOf(listOf(before), listOf(after)), trace.walkedTrace.segments)
            }
        }
    }

    @Test fun rejectedGpsFixAfterRecreationDoesNotEraseRecordedHistory() {
        launchFixture { scenario ->
            val accepted = TraceFix(48.1000, 11.5000, 1_000L, 5f)
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                trace.acceptFix(accepted)
                assertEquals(listOf(accepted), trace.walkedTrace.fixes)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                // An inaccurate fix must not replace the previously recorded track.
                trace.acceptFix(TraceFix(48.1001, 11.5001, 12_000L, 500f))
                assertEquals(listOf(accepted), trace.walkedTrace.fixes)
                assertEquals(listOf(listOf(accepted)), trace.walkedTrace.segments)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val trace = ViewModelProvider(activity)[NavigationTraceViewModel::class.java]
                assertEquals(listOf(accepted), trace.walkedTrace.fixes)
            }
        }
    }

}
