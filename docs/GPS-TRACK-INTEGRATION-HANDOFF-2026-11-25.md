# GPS trace integration handoff — 2026-11-25 10:20 MEZ

## Verified source observations
- `app/src/main/java/de/mvv/mobility/MainActivity.kt`: `val walkedTrack = remember { mutableStateListOf<Pair<Double, Double>>() }`. This state is composition-owned and is not `WalkedTrace`.
- The walking-only dashboard invokes `WalkingMiniMap(route, location, walkedTrack)`; the UI still passes a flat list of pairs.
- `app/src/main/java/de/mvv/mobility/ui/components/WalkingMiniMap.kt`: the public composable accepts `List<Pair<Double, Double>>`; the map rebuilds overlays during `AndroidView(update=...)`.
- `app/src/main/java/de/mvv/mobility/data/WalkedTrace.kt`: immutable `segments: List<List<TraceFix>>` with validity, accuracy, ordering, gap (>120 s), and speed (>15 m/s) guards. It does **not** persist across Activity recreation or automatically connect to the UI.
- Issues [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) and [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14) remain open.

## Minimal safe implementation sequence
1. Locate every mutation of `walkedTrack` and the accepted-location callback in `MainActivity`; record exact session start/end and reroute reset points before changing code. Avoid accepting the same fix twice via recomposition.
2. Introduce a single navigation-session owner (e.g. ViewModel) retaining `WalkedTrace` across Activity recreation. Use immutable state updates, and explicitly clear it only on end of navigation. Do not introduce disk persistence or background tracking.
3. Convert accepted `Location` to `TraceFix(latitude, longitude, time, accuracy)` only after confirming the location timestamp and accuracy are valid. Keep planned OSRM geometry separate.
4. Change the map API to take `List<List<GeoPoint>>` or `WalkedTrace.segments`, drawing **one polyline per segment**; do not flatten across gaps. Maintain distinct styling for planned route and actual trace.
5. Write JVM tests for accepted-fix retention across rerouting and segment boundaries; instrument `ActivityScenario.recreate()` with a three-fix test fixture. Check session identity, fix coordinates, route selection, and arrival buffer before and after recreation.
6. Verify JDK 17/SDK 35 unit tests, lint, emulator screenshots, and current GitHub Actions push run before any APK installation recommendation.

## Release/CI evidence
This handoff is documentation only: no source-code integration or tests are claimed. The previous known Actions preflight failure (#525) has no established root cause from available logs. A missing combined commit status is not a successful workflow. No signing/secrets changes and no APK release authorization.

## Chat boundary
Current conversation and repository documents were available; complete independent live access to all Main/Schedule project chat transcripts was not established. Do not infer new requirements from unseen chats.

## 11:20 MEZ follow-up — verified callback and CI #539
- `MainActivity.kt` near lines 392–406: accepted location callback appends `found.latitude to found.longitude` to `walkedTrack` only while walking navigation/dashboard is active, accuracy <=35 m, and distance from the previous point is 3–60 m. It truncates at 1500 points. This **distance-only** logic does not encode timestamp gaps or reject physically implausible speed; it is not equivalent to `WalkedTrace.accept`.
- `walkedTrack` is created by `remember { mutableStateListOf<...>() }` near line 263, and both walking map call sites (near 1434 and 1723) pass the flat list. The route-change effect around line 435 resets off-route counters, not the GPS trace. Review explicit navigation-end semantics before changing trace retention.
- **Next bounded code change:** move acceptance to a single state owner with timestamp/accuracy guards and a trace-specific regression test; then pass segments to both map call sites and draw separate polylines. Preserve the existing 1500-point memory bound in the new owner or define an equivalent bounded retention policy. Avoid flattening segments for drawing.
- **Latest verified push CI:** [Android CI #539](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45584208419) for `f8980ad`, created 2026-11-25 09:18 UTC, completed **failure**. [Job listing](https://api.github.com/repos/jochenkunkel/mvv-mobility-assistant/actions/runs/45584208419/jobs) shows `preflight` failure with zero reported steps and five skipped dependent jobs. Root cause still not established. This supersedes #525 as latest observed run. No test pass or APK release is inferred.
- This checkpoint is a source/CI analysis, **not** the functional integration; no local Android test or emulator execution occurred.

## 12:20 MEZ — bounded retention building block

- Implemented `WalkedTrace.retainLast(maxFixes)` in the pure data model, preserving GPS segment boundaries while retaining the newest fixes. A zero limit returns an empty trace, a negative limit throws, and a limit greater than the current size returns the same immutable instance.
- Added three JVM regression tests: trimming across segment boundaries, empty/unchanged histories, and invalid limit. Commits: `c8108d3` (model), `6700ef4` (tests).
- **Not yet integrated:** `MainActivity.walkedTrack` is still a Compose-owned flat list with a 1500-point cap; `WalkingMiniMap` still draws one flat polyline. On integration, use `trace.accept(fix).retainLast(1500)` and render each segment separately; avoid implying that the new helper alone fixes #13/#14.
- Tests were committed but **not executed** in this run. CI remains without a confirmed green build. No APK installation authorization.

## 14:20 MEZ — segmented map overlay API (partial integration)

- Commit `7de121e` extends `WalkingMiniMap` with optional `walkedSegments: List<List<Pair<Double, Double>>>?` and draws **one polyline per segment**. Segments with fewer than two fixes do not create a line. Existing callers remain source-compatible and use their legacy flat `walkedTrack` until explicitly migrated.
- **Important:** The two production call sites in `MainActivity` still supply a flat list, so #13/#14 are **not fixed** by this change. No claim of GPS trace retention, gap handling in the active UI, or lifecycle safety.
- **Next:** Make the active navigation session own `WalkedTrace`, pass `trace.segments.map { segment -> segment.map { it.latitude to it.longitude } }` to **both** `WalkingMiniMap` call sites, and test rotation, rerouting and gap rendering. Keep route geometry separate and cap trace at 1500 accepted fixes.
- **Validation:** No local Android SDK/emulator test executed in this run; verify compile, lint, and instrumented map overlays before any APK installation. Do not treat empty commit statuses as green CI.


## 15:20 MEZ — production trace feed wired (partial, unverified)

- Commit `c88d42f` replaces the flat Compose `walkedTrack` list with a `WalkedTrace` state value. Accepted location callbacks now pass provider timestamps and accuracy into `accept(...).retainLast(1500)` instead of a 3–60 m distance-only check.
- Both production `WalkingMiniMap` call sites now pass `walkedSegments`, preserving segment boundaries for rendering. Planned route geometry remains independent.
- **Remaining #13 risk:** the trace is still owned by `remember` in `MainActivity`, so Activity recreation can reset it. Move to a session-scoped ViewModel before closing #13/#14; add rotation, reroute, and GPS-gap instrumentation tests.
- **Timestamp caveat:** fixes without a positive provider `Location.time` are not appended. Check on-device behavior and prefer a monotonic elapsed-realtime timestamp policy if needed; never manufacture stale points.
- No local Android SDK tests or emulator runs executed. CI must be checked for this exact commit before any APK approval.
