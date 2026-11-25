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
