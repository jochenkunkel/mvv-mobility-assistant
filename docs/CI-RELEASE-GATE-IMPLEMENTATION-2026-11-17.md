# CI Release-Gate – Implementierung 17.11.2026, 05:17 MEZ

## Ausgangspunkt
Vor Änderung war `main` bei `99f473f`; neun offene Issues, keine offenen PRs. Die fünf Release-Jobs waren nicht durch einen gemeinsamen Abschluss-Check verbunden. Ein Debug-APK-Artefakt aus `build-and-test` kann existieren, obwohl Emulator oder Live-Probes scheitern.

## Änderung
Commit `f0654957b0e90c2d1d51e4560743b561efc35f04` ergänzt `.github/workflows/android.yml` um `release-acceptance`. Der Job startet nur bei manuell angefordertem `test_level=release` oder Versionstags `v*`, verwendet `always()` und hängt von `preflight`, `build-and-test`, `emulator-smoke-test`, `mvv-live-probe` und `door-to-door-live-probe` ab. Er schlägt fehl, wenn auch nur einer der fünf Ergebnisse nicht `success` lautet (einschließlich `skipped`, `cancelled` und fehlgeschlagen).

Die bestehenden Debug- und Update-Artefakt-Uploads wurden **nicht** verändert. Das Gate ist ein zusätzlicher eindeutiger Release-Check, **keine** automatische APK-Freigabe.

## Verifikation und offene Punkte
- Änderung nach `main` geschrieben und Workflow-Text erneut aus GitHub gelesen.
- Keine lokale Ausführung, kein Android-/Emulatortest. Die verfügbare Commit-Workflow-Abfrage liefert für den neuen Commit keine Läufe und deckt Push-Events nicht vollständig ab.
- **Unverifiziert:** GitHub-Actions-YAML-Parsing und tatsächliche Ausführung des neuen Gate-Jobs. Keine Installationsfreigabe.
- **Nächste Schritte:** (1) GitHub Actions Run-ID für manuellen Release-Lauf oder Tag ermitteln, alle sechs Job-Ergebnisse prüfen; (2) deterministischen statischen Regressionstest für `release-acceptance` ergänzen; (3) GPS-Lücken, Off-Route und Track-Erhalt absichern.
- Projekt-Chats: Nur im verfügbaren Kontext belegte Anforderungen berücksichtigt; andere Chatverläufe nicht vollständig live abrufbar. Keine erfundene Synchronisierung.

Issue: #8 (CI-01). Siehe auch `docs/CI-RELEASE-ACCEPTANCE-MATRIX-2026-11-17.md`.
