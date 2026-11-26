# Security Policy – Mein Weg (MeinWeg2)

## Supported versions

This repository is under active development. Security fixes are applied to the
current `main` branch. There is no long-term support branch yet.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security findings.

Report vulnerabilities privately via GitHub's
[“Report a vulnerability”](https://github.com/jochenkunkel/mvv-mobility-assistant/security/advisories/new)
form, or contact the repository owner directly through GitHub.

Include: affected commit or version, reproduction steps, impact, and — if
available — a suggested fix. Please do **not** attach real calendar entries,
GPS traces, address books, credentials, or signed keystores.

You can expect an initial acknowledgement within a few days. Once the issue is
confirmed, we will work on a fix and coordinate disclosure with you.

## Secrets and signing keys

- The Android release and CI signing keys are **not** part of this repository
  and must never be committed. They are supplied only through GitHub Actions
  secrets (see [README.md](../README.md) and
  [docs/ANDROID_IN_PLACE_UPDATES.md](../docs/ANDROID_IN_PLACE_UPDATES.md)).
- Never paste secret values into issues, pull requests, logs, or workflow
  outputs. GitHub masks registered secrets in logs, but derived values or
  decoded files may not be masked.
- If a credential is suspected to be exposed, rotate it and inform the
  repository owner. Do not attempt to rewrite Git history without an explicit
  owner approval.

## Privacy

The app requests calendar and location permissions. Debug builds can export
MVV responses and GPS traces through explicit user actions. Such exports must
never be committed or attached to public issues. Test fixtures contain
synthetic coordinates only.

The current pre-publication assessment is documented in
[docs/PUBLIC_RELEASE_READINESS.md](../docs/PUBLIC_RELEASE_READINESS.md).
