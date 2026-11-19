# Activity recreation: state ownership and safe implementation contract

Verified against `app/src/main/java/de/mvv/mobility/MainActivity.kt` on `main` at `78ea9e0` (19 Nov 2026). This is a code-level design note, **not a fix or passing test**. Tracks [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13), [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14), and architecture issue #7.

## Concrete state inventory

| Current Compose state | Current owner | Required lifetime |
| --- | --- | --- |
| `journeys`, `dashboardJourney`, `expandedJourney` | `remember` inside Activity composition | Activity recreation; restore consistent selection together |
| `walkingRoute`, `resolvedWalkingTarget`, `walkingNavigationOpen` | `remember` | Activity recreation; planned route and target must remain associated |
| `walkedTrack` | `remember { mutableStateListOf(...) }` | Activity recreation and OSRM rerouting; never clear when only planned route changes |
| `walkingRequestId`, `searchGeneration` | `remember` | Invalidate stale async responses across recreated observers; IDs alone are insufficient without cancellation/ownership |
| `homeAddress`, `walkingSpeed`, `routeFilterMode`, `routeFilterQuery`, `languageChoice` | `remember` initialized from SharedPreferences | Durable settings: persisted values should be authoritative after recreation |
| `location`, `locationStatus`, `locationPermissionGranted` | `remember` | Re-subscribe to location on lifecycle changes; old fix must never be represented as live |
| `selectedTab`, `settingsExpanded` | `remember` | Restore screen without navigating to a nonexistent journey |

## Minimal safe change sequence

1. Add a failing instrumented `ActivityScenario.recreate()` test starting with an active selected journey and accepted A/B/C GPS fixes. Capture the selected journey ID, planned route ID/geometry, accepted trace and current screen before recreation. Assert all four remain logically consistent after recreation. Use fake data sources; do not depend on MVV/OSRM/network/GPS availability.
2. Introduce a lifecycle-aware ViewModel/state holder for **one coherent navigation session** (selected journey, route, accepted trace, current progress). Keep the ViewModel at Activity scope so it survives configuration recreation. Do not store `Location` subscriptions, Activity, Context or View references in the holder.
3. Move callback ownership/cancellation into the holder; reject late results from older route requests. Distinguish a configuration recreation from process death. Use `SavedStateHandle` only for small identifiers/primitive UI state; avoid serializing entire polylines and GPS histories into Android Bundles.
4. Verify `A,B,C -> GPS outage -> D -> OSRM reroute -> rotate` leaves accepted trace `A,B,C,D` intact, while replacing only planned geometry. Verify tabs, background/foreground and a failed reroute separately.
5. Record exact test command, emulator result and Actions run for the tested SHA before closing either issue. Real S25 GPS/display validation remains required.

**Implementation guard:** Simply changing `selectedTab` to `rememberSaveable` while leaving `journeys` and `dashboardJourney` in `remember` can restore a Dashboard screen with no selected journey. Do not apply this partial fix. A green Python preflight is not Android lifecycle validation.

## Current evidence boundary

Repository review identifies likely recreation loss in Compose state ownership. The CI status API returned no statuses for `78ea9e0`; no passing instrumented rotation test, push workflow or installable APK has been established. The full contents of other project chats were not live accessible; the known reports of disappearing purple track and lost route after rotation are the requirements covered here.
