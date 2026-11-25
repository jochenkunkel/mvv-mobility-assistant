# Navigation trace Activity-scoped integration — 2026-11-25 22:20 CET

## Implemented
- Commit `5f5ee534b82e44d085b2431ef78029faf64a22b0` wires `NavigationTraceViewModel` via `ViewModelProvider(this)` in `MainActivity.onCreate`, before `setContent`.
- The GPS callback now calls `navigationTraceViewModel.acceptFix(fix)` instead of modifying a Compose `remember` trace.
- Both `WalkingMiniMap` call sites read segments from the same ViewModel-owned `walkedTrace`.
- Existing `WalkedTrace.accept(...).retainLast(1500)` behavior remains in the ViewModel. No GPS subscription, signing, secrets, or CI gates were changed.

## Limits / remaining work
- This protects GPS trace state against ordinary configuration-driven Activity recreation only. It does **not** preserve the track across process death.
- Active route, selected journey, destination, tab, and other Compose `remember` state still require separate lifecycle migration. Issues #13 and #14 must remain open until full acceptance tests pass.
- JVM and Android instrumented tests have **not** been executed in this run; no green build or install authorization is implied.
- Required next test: create a nonempty GPS trace and selected route, call `ActivityScenario.recreate()`, then portrait→landscape→portrait; verify track segments, route, selected journey and arrival buffer. Also test GPS gaps and rerouting.
- Prior Actions #548 failed at preflight with no visible steps. Investigate runner setup/annotations/policy/quota rather than changing workflow blindly.
- Full independent Main/Schedule chat transcripts are not available here; no unseen requirements were inferred.
