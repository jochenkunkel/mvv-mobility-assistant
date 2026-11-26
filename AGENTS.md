# Binding rules for coding agents – Mein Weg

This file is a short, authoritative index. Details are in the linked
documents; do not duplicate or contradict them.

1. **Source of truth:** live GitHub `main`, issues and Actions runs. Read
   `docs/CHAT-START.md` → `docs/PROJECT_STATUS.md` → `README.md` →
   `docs/DECISIONS.md` first. Documentation can be stale; verify code and CI.
2. **Working style:** small, reversible changes directly on `main`. Use a
   separate branch with explicit owner approval for history rewrites, signing
   changes or other risky operations.
3. **Evidence:** actually execute tests and record results. Never claim a
   green build from a commit or a started job. Mark local results as local.
4. **Tests:** add a regression test for every bug fix. Close tickets only
   after acceptance criteria are met and tests passed.
5. **Security:** never commit secrets, keystores, tokens, `.env` files, debug
   exports or personal data. Use GitHub Actions secrets for signing. Report
   vulnerabilities privately (`.github/SECURITY.md`).
6. **Publication gate:** the repository may be prepared for public release,
   but must not be switched to `public` without explicit owner approval. The
   license is also an owner decision (see `docs/LICENSE-DECISION.md`).
7. **Handoff:** update `docs/PROJECT_STATUS.md` after each relevant round with
   commit SHA, tests, CI status, open issues, blockers and next steps.
8. **Language:** report to the owner in German, concise and factual.

See [CONTRIBUTING.md](CONTRIBUTING.md) and
[docs/PUBLIC_RELEASE_READINESS.md](docs/PUBLIC_RELEASE_READINESS.md).
