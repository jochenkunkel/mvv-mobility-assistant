# Lifecycle regression checkpoint — 26 November 2026 01:25 CET

Baseline: main 25aa4c2; README, roadmap, project status, Android workflow, issues, PRs, recent commits, ViewModel, JVM tests and MainActivity inspected. Eleven open issues, no open PRs. Other Main/Schedule conversations not fully accessible live.

## Change
Commit 800c584 adds `NavigationTraceRecreationTest` using `ActivityScenario.launch(MainActivity::class.java)`, two accepted GPS fixes, `scenario.recreate()`, and checks for identical Activity-scoped ViewModel, preserved fixes and preserved segment. This is a **nonempty** track test, but does **not** seed a real route or verify UI navigation state, orientation in both directions, rerouting, or process death. Issues #13/#14 remain open.

## Validation gate
Test is committed but **not executed**; no Android emulator or local Gradle build was available in this run. GitHub combined commit status on previous main returned `statuses=[]` (not green). Previously documented Android Actions #548 failed before visible preflight steps; root cause remains unverified. Do not approve APK installation.

Next: execute `gradle :app:testDebugUnitTest :app:assembleDebugAndroidTest` with JDK17/SDK35; run `gradle :app:connectedDebugAndroidTest` on API35 emulator; then extend fixture to real route/selected journey/arrival buffer and portrait-landscape-portrait, diagnose runner setup annotations before CI changes. No signing, secrets or workflow changes.
