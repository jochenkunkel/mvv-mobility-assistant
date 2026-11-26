# License decision – pending owner approval

**Status: NO LICENSE HAS BEEN CHOSEN.** The repository intentionally contains
no `LICENSE` file yet. The owner must decide before publication; this document
only prepares the decision and must not be read as a license grant.

Current repository metadata (checked 26.11.2026): `licenseInfo: null`.
Until a license is chosen, the code is **all rights reserved** and may not be
reused, copied or distributed by third parties.

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

## Decision required from the owner

- [ ] Choose one: Apache-2.0 / MIT / GPL-3.0 / other / keep all rights reserved
- [ ] Confirm copyright holder name and year for the license text
- [ ] Confirm whether an existing CONTRIBUTING/CLA requirement is desired

After the decision, add the chosen `LICENSE` file (and `NOTICE` if required),
update the README section “License”, and re-run the release readiness check.
No license file is added in this round.
