# Contributing to Mein Weg

Thanks for helping to make the app reliable in everyday use. This document
defines the development rules; the current state lives in
[docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md), the agent/handoff entry point
in [docs/CHAT-START.md](docs/CHAT-START.md).

## Before you start

1. Read `docs/PROJECT_STATUS.md`, `README.md`, `docs/DECISIONS.md` and the
   current GitHub issues. Documentation may be older than the code — always
   verify against `main`.
2. Check the latest Actions runs. A red build takes priority over new features.
3. Work directly on `main` for small, small, reviewable changes. Use a separate
   branch and ask for explicit approval for risky changes, especially anything
   that rewrites Git history or touches signing material.

## Build and test

Requirements: JDK 17, Android SDK 35, Gradle 8.13 (CI installs this version).

```bash
python3 scripts/test-ci-concurrency.py
python3 scripts/test-emulator-navigation.py
bash scripts/preflight.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Instrumented tests require an API 35 emulator/device:

```bash
gradle :app:connectedDebugAndroidTest
```

Report exactly which commands you ran and their results. A commit is not test
evidence, and a started build is not a successful build. Mark emulator and
device checks as unverified if you could not execute them.

## Code and test rules

- Every bug fix needs a regression test that fails without the fix.
- GPS traces, route progress, and navigation state must survive Activity
  recreation; do not reset recorded history on a rejected/inaccurate fix.
- Keep planned route and actually walked trace separate.
- Do not log or export personal data by default. Debug exports are
  user-triggered and must never be committed.
- Close an issue only when its acceptance criteria are met and the required
  tests passed.

## Security and privacy

- Never commit keystores, passwords, tokens, `.env` files, debug exports,
  screenshots with real calendar data, or captured GPS traces.
- Signing secrets stay in GitHub Actions secrets; see `.github/SECURITY.md`.
- Report security findings privately, not in public issues.

## Handoff

After each relevant round, update `docs/PROJECT_STATUS.md` with the commit SHA,
changed files, executed tests, CI status, open issues, blockers and next step.
Keep other docs consistent; do not add conflicting parallel status files.
