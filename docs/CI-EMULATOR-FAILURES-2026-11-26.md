# API-35 release-gate failure triage — 2026-11-26 12:24 CET

## Verified evidence
- [Release-level Android CI run 45803904803](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45803904803), job `emulator-smoke-test` (139854067167): **failure**; `release-acceptance`: **failure**. Preflight, build-and-test, MVV live probe and door-to-door live probe all **success**.
- Job log explicitly reports **27 instrumented tests, 16 failures** on emulator-5554 / API 35, with `:app:connectedDebugAndroidTest FAILED`. The app and androidTest APKs **built successfully** before test execution.
- Ten failures in `MeinWegComposeUiTest`: seven `Action performScrollTo() failed`, two `assertExists` failures, one `is enabled` assertion failure. Affected tests: arrivalBufferSelectionSurvivesActivityRecreation, modernStartShowsCompactAppointmentAndExpandablePicker, walkingMiniMapShowsOpenStreetMapAttribution, activeWalkingDashboardSurvivesActivityRecreation, arrivalSliderIsInteractiveAndUpdatesLabel, locationStatusIsAccessibleAndFixtureRouteRemainsUsable, fixtureCalendarGpsAndMvvRouteAreRendered, screenshotFixtureStartAndRouteAreSavedForVisualReview, compactFilterChipsAndAdvancedSettingsWork, routeCardOpensAndClosesWalkingDashboard.
- Six failures in `NavigationTraceRecreationTest` (rejectedGpsFixAfterRecreationDoesNotEraseRecordedHistory, explicitSessionEndClearsRetainedTraceAcrossRecreation, gpsGapAfterRecreationStartsNewSegmentWithoutLosingHistory, nonEmptyGpsTraceSurvivesActivityRecreation, boundedGpsHistorySurvivesRecreationWithoutRestoringDiscardedFixes, gpsGapSegmentsRemainSeparateAfterRecreation). All report `Activity never becomes requested state "[RESUMED]" (last lifecycle transition = "PAUSED")` at `ActivityScenario.waitForActivityToBecomeAnyOf`.
- Diagnostic artifact [emulator-test-results](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45803904803/artifacts/15109280693) uploaded (48 files).
- Separate [normal push run 45800089593](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45800089593) is **green** for preflight, unit tests, lint and debug APK; emulator and release gates were skipped. The historical runner/preflight failure is no longer the active blocker.

## Interpretation and next smallest steps
1. **P0:** Inspect the uploaded XML test reports and UI screenshots/logcat. Determine whether `performScrollTo` targets no longer have a scrollable ancestor, are hidden by a changed screen/tab, or fail due to semantics/tree setup. Do not change production UI merely to satisfy an unverified test expectation.
2. **P0:** Reproduce one `NavigationTraceRecreationTest` in isolation on API 35 and inspect Activity lifecycle/logcat and any interfering overlays/permission dialogs. The identical PAUSED symptom across six tests suggests a shared fixture/environment or lifecycle problem; it does **not** yet prove loss of GPS state.
3. Fix the first proven root cause with a small regression test, rerun the affected tests, then rerun the full release-level workflow. Keep #13 and #14 open until real orientation changes with active navigation and state retention are verified.
4. **Release policy:** no signed-release or APK installation approval while `emulator-smoke-test` and `release-acceptance` are red. No signing/secret changes are indicated by this evidence.

## Scope
This is a direct GitHub Actions job-log diagnosis, not a local emulator reproduction. Other project chats (Main/Schedule) were not available as complete live transcripts; no new chat requirements inferred.
