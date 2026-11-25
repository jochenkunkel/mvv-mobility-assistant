# Navigation trace state — 25 Nov 2026, 19:25 CET

Baseline: main 7cfb34e. Reviewed README, roadmap, project status, lifecycle contract, MainActivity, WalkedTrace, issues (#13/#14), open PRs and recent commits.

## Delivered
- `NavigationTraceViewModel`: activity-scoped-compatible mutable Compose state wrapping immutable `WalkedTrace`, 1500-point retention, explicit clearSession only.
- `NavigationTraceViewModelTest`: plausible accepted fixes, stale fix ignored, reroute must not clear trace, explicit end clears.
- Commits: 4bfa362, b20eac6, 0a80d1a.
- These tests have **not been executed**. Combined status for b20eac6 returned empty statuses, not green CI.

## Still required before claiming #13/#14 fixed
1. In `MainActivity`, acquire owner via Activity `ViewModelProvider` or equivalent once per Activity and replace `remember { mutableStateOf(WalkedTrace()) }` with the owner's state.
2. Route GPS acceptance through `acceptFix`; keep both `WalkingMiniMap` segment inputs unchanged in semantics.
3. Move selected route/journey and buffer into lifecycle-safe ownership; handle stale async callbacks and explicit session end.
4. Run JVM tests, Android build/lint and `ActivityScenario.recreate()` instrumentation with nonempty GPS track. Test portrait/landscape/portrait and reroute.
5. Inspect current Actions preflight logs; earlier #548 failure is not diagnosed. Do not approve APK installation without full checks.

No complete live access to independent Main/Schedule chats; only repository and visible project context were used.
