# CI preflight: verifizierter Befund und Diagnose – 17.11.2026 11:24 MEZ

## Verifizierte Fakten
- GitHub Actions Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252), Commit `dc87e654509a8c82166025b437b389bc17f7327f`: `preflight=failure`; `build-and-test`, `emulator-smoke-test`, beide MVV-Probes und `release-acceptance` = `skipped`.
- Der GitHub-Connector liefert für Job `133914886170` keine Step-Einträge (`steps=[]`). Der Log-Abruf endet mit HTTP 404 / `BlobNotFound`. Damit ist **nicht** belegt, welcher Schritt fehlschlug.
- Die derzeitige Preflight-Reihenfolge in `.github/workflows/android.yml`: Checkout → `python3 scripts/test-ci-concurrency.py` → `bash -x scripts/preflight.sh` → `python3 -u scripts/test-emulator-navigation.py`.
- Die Testdatei `scripts/test-ci-concurrency.py` enthält eine statische Workflow-Prüfung und 52 simulierte Release-Gate-Fälle. **Die 52 Fälle wurden nicht erfolgreich ausgeführt/nachgewiesen.**
- Ein lokaler Clone-Versuch vom 17.11.2026 scheiterte an `Could not resolve host: github.com`; das ist ein DNS-/Netzwerkhindernis der Ausführungsumgebung und kein Beweis für einen Codefehler.

## Diagnose ohne Spekulation
1. Im Browser den [Run](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) und dessen `preflight`-Job öffnen; zuerst feststellen, ob Checkout, Concurrency-Guard oder Syntax-/Emulatortest fehlschlug.
2. Falls die Logs weiter nicht verfügbar sind, auf einem Rechner mit Repository-Zugriff exakt ausführen:
   ```bash
   python3 scripts/test-ci-concurrency.py
   bash -x scripts/preflight.sh
   python3 -u scripts/test-emulator-navigation.py
   ```
   Fehlertext und Exitcode protokollieren. Keinen hypothetischen Regex- oder Workflow-Fix ohne Reproduktion committen.
3. Erst nach lokaler Reproduktion einen minimalen Fix mit Regressionstest implementieren und den neuen Actions-Push-Run prüfen.
4. Für eine **Release**-Abnahme einen expliziten `workflow_dispatch`-Lauf mit `test_level=release` prüfen. Erforderlich sind sechs erfolgreiche Jobs inklusive `release-acceptance`. Push-Läufe mit übersprungenen Release-Jobs sind kein Freigabenachweis.

## Offene Prioritäten
- **P0:** Ursache des roten Preflight mit vollständigem Log oder reproduzierbarem Exitcode feststellen und beheben.
- **P1:** GPS-Ausfälle und Track-Erhalt auf echtem Gerät reproduzieren; danach deterministische Regressionstests.
- **P1:** OSM-Off-Route/Routenfortschritt und dynamischen Zeitpuffer mit Testdaten absichern.
- **Freigabe:** Bis zum vollständigen grünen Nachweis **keine APK-Installationsfreigabe**.

## Projekt-Chats
Bekannte Anforderungen aus Main/Schedule: GPS-Status und Satellitenindikator erhalten, Track bei GPS-Lücken nicht löschen, Off-Route-Rerouting reparieren, UI-Details mit Icons und klaren Zeiten, Home-Shortcut. Andere Chats waren nicht vollständig live zugänglich; keine neue Chat-Entscheidung wird unterstellt.
