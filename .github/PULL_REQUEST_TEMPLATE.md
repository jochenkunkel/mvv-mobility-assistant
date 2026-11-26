## What changed

<!-- Short summary and motivation. Link the related issue(s). -->

## Verification actually performed

- [ ] `python3 scripts/test-ci-concurrency.py`
- [ ] `python3 scripts/test-emulator-navigation.py`
- [ ] `bash scripts/preflight.sh`
- [ ] `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- [ ] Instrumented tests on an emulator (state API level and result)
- [ ] Linked green GitHub Actions run and commit SHA

Mark only checks you really ran. A commit or a started build is not test
evidence. Paste the run URL and the commit SHA below.

## Checklist

- [ ] No secrets, keystores, tokens or personal data added
- [ ] Regression test added or updated for bug fixes
- [ ] `docs/PROJECT_STATUS.md` updated with commit, results and open risks
- [ ] Documentation updated where behaviour changed
- [ ] No unrelated changes bundled in this PR

## Open risks / follow-up

<!-- What is still unverified? Which approval is required? -->
