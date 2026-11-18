# CI concurrency: verified source-of-truth reconciliation (2026-11-18)

## Finding

`docs/DECISIONS.md`, ADR-008 (dated 2026-11-16), describes a **historical** global `single-active-ci` group with unconditional `cancel-in-progress: true`, including cancellation of running release jobs. **Do not implement that older policy without a new decision.** It conflicts with the current `.github/workflows/android.yml` and the executable `scripts/test-ci-concurrency.py` guard.

The current Android workflow instead uses:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'push' && !startsWith(github.ref, 'refs/tags/v') }}
```

This means ordinary push runs on the same ref can supersede older push runs; manual, PR and version-tag runs are not cancelled by this policy. The group also separates workflows, event types and refs. **This is a source inspection, not an observed scheduler test.** The release workflow must be inspected independently before making cross-workflow guarantees.

## Required follow-up

1. Treat the actual workflow and its regression guard as the implemented policy. Amend ADR-008 in a dedicated architecture-decision review so the outdated global-cancellation decision is either formally superseded or deliberately restored with explicit user approval.
2. Run `python3 scripts/test-ci-concurrency.py`, `bash scripts/preflight.sh`, and `python3 scripts/test-emulator-navigation.py` on a checkout of the same HEAD; capture exact output and exit codes.
3. Inspect GitHub Actions **push** runs directly (the available commit-workflow connector only reports PR-triggered runs). Record the run ID, commit SHA, preflight result and Android build result before any APK recommendation.
4. Continue with executable GPS-track (#14) and rotation (#13) regression tests. Neither issue is resolved by this documentation fix.

## Verification boundary

On 2026-11-18 the current workflow and guard were read directly through the connected GitHub repository. Local checkout failed with `Could not resolve host: github.com`; no tests were executed, no green build or APK approval is claimed.
