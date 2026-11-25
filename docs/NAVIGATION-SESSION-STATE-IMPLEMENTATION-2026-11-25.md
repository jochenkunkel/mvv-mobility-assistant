# Navigation session lifecycle implementation contract — 25 Nov 2026, 18:20 CET

## Verified baseline
- `main` at inspection: `2cdc0650e7a78f4c42972ea4db4e295cfcb74f3d`.
- `MainActivity.kt` holds `walkedTrace` as `remember { mutableStateOf(WalkedTrace()) }`; therefore Activity recreation can discard the recorded trace.
- Existing segmented `WalkedTrace` and `WalkingMiniMap` rendering do not themselves solve lifecycle ownership.
- Open acceptance issues: #13 (rotation resets configuration/route), #14 (walked trace disappears).
- Current CI is not green: previously verified run #548 failed at preflight without exposed steps. No root cause proven. No APK authorization.

## Smallest safe code migration (next run)
1. Introduce an Activity-scoped `ViewModel` that owns the navigation-session trace as immutable `WalkedTrace`, plus the active journey/route selection needed for rotation. Obtain the ViewModel once from the Activity's ViewModelStore; do **not** create it inside a changing Composable key.
2. Preserve current `accept(...).retainLast(1500)` behavior and explicit segment boundaries. Mutate the trace only from the accepted GPS callback; keep location subscription tied to the existing lifecycle.
3. Preserve the same session when rerouting or changing orientation; clear trace only on an explicit new navigation session/end. Avoid serializing full GPS tracks or complex route geometry into `rememberSaveable` bundles.
4. Verify both walking map call sites receive the same retained segments. Keep route geometry and actual walked history distinct.
5. Add JVM state-owner tests: A/B/C accepted, bad fix ignored, long gap starts new segment, reroute retains A/B/C, cap at 1500, explicit end clears.
6. Add Android test with a nonempty trace and selected journey: `ActivityScenario.recreate()` then portrait→landscape→portrait; assert track, route, selected tab, destination and arrival buffer remain. Do not mark #13/#14 fixed without executed tests.

## CI unblock independent of feature work
Inspect Actions #548 preflight setup/runner annotations, repository Actions policy and runner/billing limits. Zero returned steps does not identify a failing Gradle command. Run JDK 17/SDK 35 unit tests and lint when a runner or local Android toolchain is available. No signing/secrets changes, no green-build claims, no install approval.

## Cross-chat boundary
Only current conversation context and repository artifacts were used. Complete independent live Main/Schedule transcripts were not accessible, so no unseen chat requirements are asserted.
