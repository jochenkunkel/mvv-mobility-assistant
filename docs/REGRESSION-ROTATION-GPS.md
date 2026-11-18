# Regression matrix: rotation and GPS-track continuity

Status: test specification, **not** a passed test or a verified fix. Issues [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) and [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14).

## #13 – Android configuration change (portrait ↔ landscape)

Precondition: saved home address, non-default walking speed and arrival buffer; a selected calendar destination, chosen MVV connection and active walking route. Record values and route/track before each change.

1. Rotate portrait → landscape → portrait during route selection. All editable preferences and selected destination remain unchanged; the active itinerary does not reset.
2. Rotate while walking with a non-empty accepted GPS trace. The route, already travelled trace, current navigation state and time buffer remain consistent; no duplicate track points are added solely by rotation.
3. Repeat with permissions denied, no location fix, and while a reroute request is pending. The UI must show honest loading/error state, not silently reset or duplicate requests.
4. Background/foreground and Android activity recreation with the developer option **Don't keep activities**. Distinguish saved settings (must survive process recreation) from ephemeral navigation state (explicit product decision and documented recovery required).

**Automation target:** ActivityScenario.recreate() with a fake location/route repository; assert persisted settings, selected itinerary, route progress and trace snapshot before/after. Instrumented rotation test on emulator; check that UI does not silently fall back to Start.

## #14 – accepted GPS trace must be independent of rerouting

Use synthetic monotonic timestamps, fixed coordinates and controlled accuracy/provider. Track accepted positions separately from the planned OSRM polyline.

1. Good GPS fixes A,B,C → temporary location outage → good fix D: previously accepted A–C remain visible; outage must not erase history.
2. Inject a coarse network fix between accurate GPS fixes: rejection or acceptance must follow documented accuracy policy; no teleportation of the rendered trace.
3. Trigger off-route evidence and a successful OSRM reroute: replace only the **planned route**; retain accepted track and distance history.
4. Trigger failed, empty and delayed reroute responses: preserve the last usable route and all accepted history; prevent an older response overwriting a newer route.
5. Rotate screen and switch tabs while tracking; no duplicate sampling, no cleared trace, no disappearing dark-purple path. Verify independently that the path is not merely hidden behind the new route layer.
6. Reproduce with an actual outdoor S25 walk, export timestamp/accuracy/provider diagnostics, and compare the accepted point sequence to the displayed path.

**Automation target:** pure state-reducer tests for GPS accepted/rejected, reroute requested/succeeded/failed, stale callbacks and activity recreation. UI screenshot tests must compare the accepted trace before/after reroute and rotation; a screenshot alone cannot establish underlying state continuity.

## Verification and release gates

Run targeted JVM tests, then `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. Run Android instrumented rotation tests on emulator, inspect real screenshots and GitHub Actions jobs. Record the exact commit SHA, workflow run, test results and any remaining real-device gaps in `docs/PROJECT_STATUS.md`. Do **not** approve APK installation while required CI checks are failing or unknown.

## Implementation sequence

1. Identify state ownership in `MainActivity.kt` versus persisted settings and route/trace models; write failing tests before changing lifecycle behavior.
2. Fix #13 state survival without changing signing or unrelated routing.
3. Extract accepted trace state from OSRM reroute state and cover #14 with deterministic tests.
4. Validate on emulator and S25; close each issue only when its acceptance criteria have evidence.
