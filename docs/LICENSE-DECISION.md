# License decision – resolved

**Status: Apache-2.0, decided by the owner on 26.11.2026.** A canonical
`LICENSE` (Apache License 2.0) and a `NOTICE` file were added to the repository
root. The decision replaced the earlier state (`licenseInfo: null`, no
`LICENSE`) documented in previous readiness snapshots.

Copyright holder: Jochen Kunkel, year 2026 (derived from repository ownership;
no other holder was specified). Third-party notices and data sources are listed
in `NOTICE` and in `docs/PRIVACY-AND-PROVIDER-POLICY.md`.

This document is retained as the decision record and must not be read as a
replacement for the `LICENSE` file.

## Why a decision is needed

A public repository without a license is not open source. Readers cannot know
whether they may use, modify or redistribute the code, and contributions are
legally ambiguous.

## Options

| Option | Effect | Pros | Cons |
| --- | --- | --- | --- |
| **Apache-2.0 (recommended)** | Permissive, explicit patent grant | Common in Android/Kotlin ecosystems; patent protection for contributors and users; compatible with GPLv3 | Requires retaining notices / stating changes; slightly longer text |
| **MIT** | Permissive, minimal | Very short and widely understood; minimal obligations | No explicit patent grant; weaker protection against patent claims |
| **GPL-3.0** | Strong copyleft | Derivative works must stay under GPL-3.0; protects openness | Requires source disclosure for distributed derivatives; can conflict with proprietary distribution / Play policies |
| **All rights reserved** | No reuse permitted | Maximum control | Not open source; contradicts the goal of a public open-source release; external contributions remain unclear |

## Recommendation

**Apache-2.0** fits an Android app that uses third-party open-source
libraries and may later be distributed through a store. It grants users
clarity and includes a patent clause, while not forcing derivative apps to be
published under the same license.

## Decision record

- [x] Owner chose **Apache-2.0** on 26.11.2026
- [x] Copyright holder/year: Jochen Kunkel, 2026
- [x] `LICENSE` and `NOTICE` added; README links both
- [ ] Optional: CLA requirement for external contributions – not requested

The license blocker for a public release is resolved. Other publication gates
(private email in Git history, artifact visibility) are tracked in
`docs/PUBLIC_RELEASE_READINESS.md`.
