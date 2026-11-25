# NavigationTraceViewModel wiring checklist — 2026-11-25 21:20 CET

## Verified baseline
- Latest main before this note: `ed1707340dc337bbe0d765bf2f837b9ed4ae84d5`.
- `app/src/main/java/de/mvv/mobility/NavigationTraceViewModel.kt` already contains an Activity-compatible `ViewModel` with Compose-observable `walkedTrace`, `acceptFix(TraceFix)`, and `clearSession()`. `acceptFix` applies `WalkedTrace.accept(...).retainLast(1500)`.
- The implementation contract and project status still identify `MainActivity`'s `remember`-owned trace as the integration gap. This note is **not** evidence of a code fix or executed tests.
- Issues #13 and #14 remain open. GitHub search shows eleven open issues and no open PRs.

## Minimal integration, with safeguards
1. Read the current `MainActivity.kt` immediately before editing; do not assume line numbers from older audit notes. Identify all reads/writes of `walkedTrace`, the GPS callback, both map calls, and explicit session-end actions.
2. Obtain **one** Activity-scoped `NavigationTraceViewModel` through the Activity's `ViewModelStore` / lifecycle integration. Do not instantiate it with a plain constructor inside a Composable or `remember` block.
3. Replace the local trace writer with `viewModel.acceptFix(fix)` in the accepted GPS callback; replace both map inputs with `viewModel.walkedTrace` segments. Remove any redundant local trace writer to prevent diverging history.
4. Do not call `clearSession()` on orientation changes, recomposition, temporary GPS loss, map tab switches, or rerouting. Invoke only at explicit session end/new session, after defining the intended user behavior.
5. Do not move GPS listeners into the ViewModel without separately auditing lifecycle and permission handling. Configuration survival is not process-death persistence. Route, selected journey, arrival buffer, and selected tab remain separate lifecycle gaps until explicitly migrated.

## Required verification before closing #13/#14
- JVM: A/B/C accepted fixes, rejected bad fix, long-gap segment break, cap at 1500, reroute retains trace, explicit clear resets it.
- Android: start a nonempty route/trace; verify before and after `ActivityScenario.recreate()` and portrait-landscape-portrait. Assert both route/selection and trace continuity, not merely that the dashboard exists.
- Execute tests on JDK 17 / Android SDK 35; record exact commands, exit codes and commit SHA. A committed but unexecuted test is not a pass.
- Actions #548 preflight previously failed with no returned steps and five skipped jobs. Check job setup/runner annotations, policy and quota before modifying workflow. No signing/secrets changes, no APK install approval without green evidence.

## Cross-chat limitation
The current conversation's known Main/Schedule requirements were considered; independent full chat transcripts were not accessible in this run. Do not infer new requirements from unseen chats.
