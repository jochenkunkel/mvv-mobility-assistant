# Preflight diagnostics – operator runbook (20 November 2026)

## Why this exists
The last directly inspected Android Actions push run [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) reported `preflight=failure` and skipped dependent jobs. Its individual steps and logs were unavailable through the connector. **The failing command is not yet identified.** Do not infer a checkout, runner or script defect from that status alone.

## New independent workflow
[Preflight diagnostics](../.github/workflows/preflight-diagnostics.yml) is a separate **no-APK** workflow. It runs automatically on `main` pushes changing the workflow files or `scripts/**`, and can also be started with `workflow_dispatch`. Other changes do **not** trigger it automatically. It checks out the triggering commit and records runner/checkout metadata.

Its three checks execute independently, with a 120-second timeout per check:
1. `python3 scripts/test-ci-concurrency.py`
2. `bash -x scripts/preflight.sh`
3. `python3 -u scripts/test-emulator-navigation.py`

The workflow writes each command's exit status to `preflight-diagnostics/results.txt`, stores stdout/stderr in `check-1.log` through `check-3.log`, emits an error annotation on failure and uploads a `preflight-diagnostics` artifact even if a check fails. The artifact retention setting is 14 days.

## Diagnosis procedure
1. Open [diagnostic workflow runs](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/preflight-diagnostics.yml) and select the newest run for the current `main` SHA. Confirm that a run **actually exists**; a committed workflow alone proves nothing about its execution.
2. Read the job conclusion and artifact. If `results.txt` exists, identify the first nonzero exit status and inspect the corresponding `check-N.log`. A timeout exit code 124 is distinct from a test assertion failure.
3. If no artifact exists, inspect runner provisioning, checkout and artifact-upload steps in the Actions web UI. An absent artifact does **not** establish that all three checks failed; they may never have started.
4. Reproduce the failing command at the same commit locally. Preserve stdout, stderr and exit code. Apply the smallest fix to the demonstrated cause, then rerun the checks.
5. Recheck the **normal Android workflow** after the fix. A successful diagnostic run is **not** a successful Android build, emulator test, release acceptance or APK installation approval.

## Follow-up product work
After restoring a trustworthy CI baseline, prioritize [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) with a non-empty `ActivityScenario.recreate()` navigation/track fixture, then [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14) with accepted GPS trace continuity and stale reroute callbacks. Instrumentation must actually run on an emulator; ordinary push CI is insufficient.

**Verification boundary:** This document describes repository workflow source and an investigation procedure, not observed diagnostic-run results. Do not approve installation while required tests remain red or unknown.

## Verified Actions checkpoint — 2026-11-23 23:24 CET

Direct repository-wide Actions REST queries (not the PR-only commit-run lookup) establish:

- Latest normal push CI for `main` commit `18f03413`: [Android CI run #504](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45268631001), **failure**. Job API: `preflight=failure`, five downstream jobs `skipped`, `steps=[]`, no run artifacts.
- Independent [preflight diagnostics run](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45259023378) for `509e841`: **failure**. Job status: the `diagnose` job failed with `steps=[]` and no artifacts. This proves the separate workflow **did start** on an earlier scripts change, contradicting older checkpoints that left its execution unverified; it does not prove that any of its three commands started.
- Other recent diagnostic runs are also marked failed (for example [run 45248127273](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45248127273)); do not equate missing step metadata with an identified runner failure.

**Operational next action:** Inspect the GitHub Actions web UI for run 45259023378 (diagnose) and 45268631001 (normal preflight), including job initialization and raw logs. If the UI exposes no steps/logs, verify Actions runner allocation, organization/repository policy and billing/quota with an authorized maintainer before modifying preflight scripts. If the scripts ran, capture the exact first failing command, exit code and log; reproduce it at the same SHA. The diagnostic workflow is path-filtered, so a docs-only commit here is **not** expected to trigger it. Do not approve APK installation.
