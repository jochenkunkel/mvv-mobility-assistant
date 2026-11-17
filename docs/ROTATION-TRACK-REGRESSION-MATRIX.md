# Regression matrix: Activity rotation and GPS track continuity

Status: test specification, **not** an implementation or passed test. Created 2026-11-17.

## Issue #13 — rotation must not reset an active journey

Prerequisite: start an active journey with a selected calendar destination, MVV connection, walking route, and a visible track containing at least three accepted positions.

1. Rotate portrait → landscape → portrait while navigation is active. Assert selected destination, chosen connection, current walking leg, route geometry, navigation progress, arrival buffer, and user settings remain unchanged.
2. Rotate during an asynchronous MVV lookup or OSRM response. Assert no duplicate request side effects, stale callback overriding newer selection, or loss of loading/error state.
3. Rotate with GPS permission denied, then grant permission. Assert permission state is reflected correctly without clearing the journey.
4. Simulate Activity recreation with 'Don't keep activities'. Assert durable user preferences persist; transient navigation state must be restored explicitly or show a truthful recovery state, never silently restart at the initial screen.
5. Rotate on the settings screen after editing the home address or walking speed. Assert field edits and persisted values are consistent.

Suggested test split: JVM tests for serializable journey state and reducer transitions; Android instrumentation for Activity recreation and visual state preservation. Do not assert that a ViewModel alone survives process death.

## Issue #14 — accepted GPS track must survive rerouting

Prerequisite: deterministic replay with timestamp, accuracy, provider, lat/lon, and monotonic elapsed time for each position. Keep an **accepted location history** separate from planned OSRM geometry and route-progress projection.

1. Feed three good fixes, then a GPS gap. Assert the drawn accepted track remains unchanged while freshness becomes stale.
2. Feed a low-accuracy network fix between good GPS fixes. Assert it does not erase, replace, or reorder accepted track points.
3. Feed a delayed/out-of-order fix. Assert history ordering is monotonic and the track does not jump backwards.
4. Trigger confirmed off-route rerouting and replace OSRM route geometry. Assert all previously accepted track points remain; only planned route and projected progress change.
5. Fail an OSRM request. Assert the existing track stays visible and the app reports rerouting failure without resetting the journey.
6. Rotate while the route is being recalculated. Assert accepted track is preserved exactly once (no duplicated or dropped points).
7. Resume after foreground/background transition and GPS gap. Assert retained track remains visible; no straight line is fabricated across an unobserved gap unless clearly represented as discontinuity.
8. Test empty, one-point, and long histories for stable rendering and bounded memory.

## Acceptance evidence

Record test name, device/emulator, build commit SHA, input fixture, expected/observed result, screenshots for rotation and track rendering, and Actions run URL. Mark issues complete only after automated tests and S25 field validation. No installation approval from this document.

## CI dependency

The historical Actions run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) reports preflight failure; dependent Android tests did not run. Available GitHub job metadata does not expose failing steps. First reproduce `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, and `python3 -u scripts/test-emulator-navigation.py` on the **same commit** before changing production code. Push CI success must be verified separately because commit-associated workflow lookup is PR-only.

## Sources

- [Issue #13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13)
- [Issue #14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14)
- [Central project status](PROJECT_STATUS.md)
