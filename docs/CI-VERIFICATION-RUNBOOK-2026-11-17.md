# CI-Verifikation: reproduzierbarer Ablauf (17.11.2026)

## Zweck
Der Release-Gate-Code in `.github/workflows/android.yml` und der Guard in
`scripts/test-ci-concurrency.py` sind committed. **Ein Commit und eine
statische Codeprüfung sind kein Nachweis eines erfolgreichen Actions-Laufs.**
Dieser Ablauf ist der noch fehlende operative Nachweis für Issue #8.

## Lokale, SDK-unabhängige Prüfung
Vom Repository-Root mit Python 3 und Bash:

```bash
bash scripts/preflight.sh
python3 scripts/test-ci-concurrency.py
python3 scripts/test-emulator-navigation.py
```

Der Concurrency-/Release-Guard führt 52 synthetische Release-Gate-Fälle aus.
Er validiert nicht den tatsächlichen GitHub-Scheduler oder die Android-App.
Ausführung, Exit-Code und Commit-SHA müssen im Prüfprotokoll erfasst werden.

## GitHub-Actions-Prüfung (GitHub CLI mit Repository-Zugriff)
```bash
gh run list -R jochenkunkel/mvv-mobility-assistant --workflow android.yml --limit 15
gh run view RUN_ID -R jochenkunkel/mvv-mobility-assistant --json headSha,event,status,conclusion,jobs,url
gh run view RUN_ID -R jochenkunkel/mvv-mobility-assistant --log-failed
```

Für einen vollständigen Release-Test auf `main` kann ein berechtigter
Operator den bestehenden Workflow manuell mit `test_level=release` starten.
**Nicht** einen gewöhnlichen Push-Lauf mit einem Release-Lauf verwechseln:
Live-Probes und Emulator sind bei normalen Pushes absichtlich übersprungen.
Ein Release-Nachweis erfordert `success` für `preflight`,
`build-and-test`, `emulator-smoke-test`, `mvv-live-probe`,
`door-to-door-live-probe` **und** `release-acceptance`.

## Prüfprotokoll (auszufüllen, nicht vorausgefüllt)
- Git-HEAD / Run-ID / Run-URL:
- Event und `test_level`:
- Ergebnis aller sechs Jobs:
- Emulator-Screenshots visuell geprüft von / am:
- APK-Signatur, Paketname und Versionscode:
- Bekannte Fehler / verlinkte Issues:
- Entscheidung: **keine Installationsfreigabe**, solange Nachweise fehlen.

## Stand dieser Prüfung
Am 17.11.2026 war `693093cb` der jüngste per GitHub-Connector
sichtbare Commit. Der Guard-Quelltext wurde gelesen, aber in diesem Lauf
**nicht ausgeführt**. Ein lokaler Git-Clone scheiterte an fehlender
DNS-Auflösung für github.com. Die verfügbaren Connector-Abfragen liefern
keinen vollständigen verifizierten Push-/Release-Run. Somit bleiben
CI-Ergebnis, APK und Geräteabnahme **unbestätigt**.

Nächster Schritt: obige Befehle in einer GitHub-fähigen Umgebung ausführen,
Run-ID und sechs Job-Ergebnisse dokumentieren; bei Fehlern zuerst den
konkreten Job reparieren. Danach GPS-Track-/Rerouting-Regressionstests.
