# Android CI preflight: evidence and reproducible next action (2026-11-26 04:22 CET)

## Verified evidence
- Reviewed README, implementation roadmap, regression matrix, `.github/workflows/android.yml`, recent commits, open issues and PRs. Before this document the most recent commit returned by commit search was `cc49b9d8cf93dbae0ace41aadebab7cf8c5c1d49`; 11 open issues and no open PRs.
- Existing workflow run [45632319389](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45632319389) reports `preflight=failure` (job 139268505628). The dependent build, MVV probes, emulator and release acceptance jobs were skipped. **This is an older run; it does not prove the current HEAD fails.**
- The job-steps API returned an empty array for job 139268505628. Fetching its logs returned GitHub 404 BlobNotFound on 2026-11-26. Neither identifies which command failed.
- Combined status for `cc49b9d8` returned `statuses=[]`. The available commit-workflow endpoint filters to pull-request runs and cannot certify push CI. **No green build or APK installation approval.**
- The checked-in `NavigationTraceRecreationTest` contains ActivityScenario recreation tests for trace retention and explicit session reset. They are **not yet evidenced as executed**. It does not assert preservation of selected route, tab, journey or buffer (#13).
- Workflow `emulator-smoke-test` is gated to manually dispatched release-level runs or version tags, not ordinary pushes. Thus an ordinary push build cannot validate these instrumented tests.

## First reproducible CI triage, no blind changes
1. Open the **current main push run**, not only the older run above, in GitHub Actions; record run ID, head SHA, event and the first failing preflight step. If logs are inaccessible, download the `android-preflight-failure` artifact when available; inspect `results.txt` and `check-1.log` through `check-3.log`. Do not infer the failed command from skipped downstream jobs.
2. On a checkout of that **exact SHA**, run in order: `python3 scripts/test-ci-concurrency.py`; `bash -x scripts/preflight.sh`; `python3 -u scripts/test-emulator-navigation.py`. Record exit codes and first error. Repair only the reproduced failure and add a regression.
3. Once preflight passes, run `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`; then API-35 `connectedDebugAndroidTest` including `NavigationTraceRecreationTest`. Release-level workflow/emulator and screenshot review remain separate gates.
4. Extend #13 tests with seeded route/selected journey/tab/arrival buffer across recreation and actual portrait-landscape-portrait. Extend #14 with pending/failed/stale reroute and visible accepted-trace continuity. A ViewModel-only test is not UI proof.

## Limitations and release policy
No Android emulator or Gradle test was run in this investigation; no current push-run success was independently confirmed. Other project chat histories are not fully accessible live; no new requirements are attributed to them. Do not alter signing/secrets, close #13/#14, or authorize APK installation without executed test evidence.
