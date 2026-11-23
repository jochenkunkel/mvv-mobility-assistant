# Regression matrix: active navigation state and GPS track (#13 / #14)

Status: **test specification only**, 2026-11-23. No executable Android test or device acceptance is implied. Related decision: [ADR-010](DECISIONS.md).

## Invariants
- The selected itinerary, active navigation state and settings are not reset by an Activity recreation caused by rotation.
- The accepted **actual GPS trace** is independent of the OSRM/OSM route polyline and the route-progress projection. A route recalculation must never replace the trace with the new planned route.
- Rejecting an inaccurate, stale, implausible or missing fix does not delete previously accepted trace points. A long gap must not be rendered as an invented straight segment.
- A Compose recomposition or map redraw is presentation work, not a new navigation session.
- Trace data remain transient: no implicit disk history or background tracking is introduced by a fix. Explicit end-of-navigation clears session data; process death requires a separately agreed restoration policy.

## Deterministic acceptance scenarios
| ID | Setup / action | Observable assertion |
| --- | --- | --- |
| ROT-01 | Start a route with changed home/buffer/speed settings; rotate portrait → landscape → portrait | Settings remain unchanged; selected itinerary, destination, navigation mode and already accepted track points remain identical |
| ROT-02 | During active route trigger a UI redraw/recomposition | No new route selection or track reset; map shows the same accepted points |
| GPS-01 | Feed 3 valid timestamped fixes in order | Exactly 3 ordered accepted trace points; drawn line follows actual fixes, not projected route |
| GPS-02 | Feed valid fix, inaccurate/stale fix, valid fix | Invalid fix not appended; first accepted fix preserved; trace remains consistent |
| GPS-03 | Feed valid fix, large time gap, valid fix | Both accepted points retained where policy allows; **no connecting line** across the gap |
| GPS-04 | Feed plausible fix followed by impossible location jump | Jump rejected or separated by explicit gap policy; no teleport line and no clearing of earlier trace |
| GPS-05 | Feed accepted fixes then reroute to new OSRM geometry | Actual trace retains original coordinates and count; planned polyline changes independently |
| GPS-06 | Temporarily lose permission/provider, then restore and feed valid fix | Visible degraded state during loss; existing trace preserved; new fixes append according to gap policy |
| LIFE-01 | Explicitly end navigation | Transient trace and active navigation session cleared intentionally; persisted user settings retained |
| LIFE-02 | Kill process and relaunch | Behavior matches an explicitly documented process-death policy; do not claim recovery unless implemented |

## Implementation order
1. Identify the single owner of navigation session state and the accepted-fix/segment data model. Record actual source locations in the implementing PR; do not assume Activity fields survive recreation.
2. Extract pure state transitions for accepting/rejecting fixes and segment boundaries; write JVM unit tests for GPS-01 through GPS-05 before changing UI rendering.
3. Bind map rendering to accepted trace segments, separate from OSRM planned route and route progress; test redraw and reroute.
4. Add an Android instrumentation rotation test (ROT-01) using ActivityScenario.recreate / orientation changes and assertions against the actual screen/state. Validate permissions and lifecycle scenarios on an emulator and S25.
5. Run full CI and inspect actual screenshots/track behavior. **No APK installation approval until current gates are green and evidence exists.**

## Current evidence limits
The historical [Actions run #491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723) has a failed preflight and skipped downstream jobs; this does not identify the exact failing command. The GitHub commit-workflow lookup available in this session only covers PR runs and cannot establish current push CI. Run `python3 -u scripts/test-emulator-navigation.py`, `bash -x scripts/preflight.sh` and `python3 scripts/test-ci-concurrency.py` on an accessible checkout; obtain real exit codes and latest push Actions logs before attributing a CI cause.
