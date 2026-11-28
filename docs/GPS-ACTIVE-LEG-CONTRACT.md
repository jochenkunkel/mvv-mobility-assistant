# P0: Active transit leg contract and regression matrix (2026-11-28)

Status: **design/test contract only; not implemented or validated**. Tracks issue #22 and PR #27. This document deliberately does not grant merge or APK approval.

## Ownership and meaning

The navigation session, not a display-only "next step" selector, owns an explicit `activeLegIndex: Int?` for the selected journey. Null means **unknown**; it must never be treated as leg zero. The index is meaningful only together with the identity of the selected journey; replacing the journey invalidates it. An upcoming step shown in the UI is not automatically the active step.

The current leg changes only on a deliberate, verified transition (for example a user check-in or a reliable transition signal). GPS proximity alone must not silently confirm boarding. At every leg change, invalidate the previous leg's GPS mode/hysteresis and ignore stale asynchronous location results from an older session/leg generation. Boarding confirmations are keyed by both journey identity and leg index, never just by "some transit ride confirmed".

The transit GPS jump guard is eligible **only** if all conditions hold: (1) selected journey identity matches the session; (2) active leg index is valid; (3) active leg is transit, not walking; (4) boarding for this exact leg is confirmed. Otherwise the guard must not infer that the user is riding, suppress valid walking GPS fixes, or show an unearned green ride state.

## Deterministic regression matrix

| Sequence / state | Expected guard | Expected UI/status |
| --- | --- | --- |
| Journey selected, active leg unknown | off | unknown, not confirmed riding |
| Bus A active, boarding A confirmed | on | A-specific status only |
| Switch A → walking leg | off immediately | A's GPS hysteresis cleared |
| Walking → bus B, B not confirmed (A confirmed) | off | B not riding/confirmed |
| Confirm boarding B | on | B-specific status |
| Change journey, same numeric leg index | off until new confirmation | no leaked boarding/hysteresis |
| Invalid/out-of-bounds active index | off | safe unknown state |
| Delayed GPS callback from A arrives while B active | cannot mutate B state | B state remains unchanged |
| Rotation with active B | preserve same selected journey and active leg | no automatic re-confirmation |
| Reset/end navigation | off | clear leg, confirmation, hysteresis |

## Small implementation sequence

1. Introduce a testable session-leg selector/state owner with explicit journey identity, active index and generation. Write JVM tests for the table above before wiring UI.
2. Reset `previousRideGpsMode` when the active leg changes, not only when the journey changes. Keep callback generation checks at the asynchronous boundary.
3. Replace first-transit-leg selection in the GPS guard, ride status and fullscreen dashboard with the same validated active-leg state. Do not substitute `displayConnection.stepIndex` without proving its semantics.
4. Run JVM tests, Android lint/build, API-35 instrumentation and relevant screenshot checks. Record exact run URLs, job conclusions and skipped gates in `docs/PROJECT_STATUS.md`. A fast green CI run does not satisfy release gates.

## Evidence boundary

The 2026-11-28 review of README, roadmap, ADR, status and workflow confirms `workflow_dispatch(test_level=release)` is configured, while routine push/PR CI does not run the release-only emulator and live probes. No such release run was executed as part of this document. The other project chats (Main/Schedule) are not directly readable in full; requirements above are grounded in the repository and already known issue context. Signing and secrets are out of scope.
