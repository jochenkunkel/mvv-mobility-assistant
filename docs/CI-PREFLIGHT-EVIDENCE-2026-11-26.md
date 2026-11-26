# CI-Preflight: belegbarer Befund und Admin-Checkliste (26.11.2026, 05:15 MEZ)

## Evidenz (nicht mit einem Build-Erfolg verwechseln)

- Android CI [Run #548](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45632319389), Job `139268505628`: `preflight=failure`, `steps=null`, `logs_url=null`; die fünf anderen Jobs sind `skipped`.
- Die erneute GitHub-Actions-Artefaktabfrage für Run #548 liefert `[]`. Insbesondere liegt kein `android-preflight-failure`-Artefakt vor. Das beweist **nicht**, dass ein bestimmtes Skript fehlgeschlagen ist: der Runner könnte vor Ausführung der Diagnoseschritte ausgefallen sein.
- Die verfügbare Commit-Workflow-Abfrage für HEAD `564a425` liefert `[]`, erfasst aber **nur PR-triggered Runs**; der Workflow wird auf Branch-Pushes ausgelöst. Damit ist der aktuelle Push-CI-Status **nicht verifiziert**.
- Der Workflow `.github/workflows/android.yml` führt auf gewöhnlichen Pushes `preflight` und anschließend JVM-/Lint-/Debug-Build aus. API-35-Instrumentierung wird ausschließlich für manuelle `release`-Runs oder `v*`-Tags gestartet. Neue GPS-Recreation-Tests sind deshalb **nicht** durch normale Push-CI abgenommen.

## Nächster reproduzierbarer Diagnosepfad

1. Im GitHub-Actions-UI den **neuesten Push-Run auf main** öffnen; dessen Run-ID, Commit, Trigger, Jobstatus und Zeitpunkt protokollieren. Nicht den historischen #548 als aktuellen Run ausgeben.
2. Beim fehlgeschlagenen `preflight`-Job zuerst **Set up job**, Job-Annotations und Runner-Zuweisung lesen. Fehlen sämtliche Steps/Logs, Actions-Policy, Runner-Verfügbarkeit und Kontingente/Billing durch Repo-Admin kontrollieren; bei GitHub-Support die konkrete Run- und Job-ID nennen.
3. Sobald Steps verfügbar sind, den **ersten** fehlgeschlagenen Befehl aus dem Joblog ermitteln. Lokal mit JDK 17, Android SDK 35 und Repo-Checkout: `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py`. Exitcodes und Umgebung dokumentieren.
4. Erst nach grünem Preflight und Build einen manuellen `workflow_dispatch`-Run mit `test_level=release` ausführen und die API-35-Instrumentierung (inklusive `NavigationTraceRecreationTest`) sowie MVV-Probes kontrollieren.
5. Für #13/#14 zusätzlich reale Portrait–Landscape–Portrait-Wechsel mit aktiver Route, ausgewählter Verbindung, Ziel, Puffer und nichtleerer GPS-Spur testen; `ActivityScenario.recreate()` allein ersetzt diesen Nachweis nicht.

**Sperre:** Keine Signing-/Secrets-Änderung, kein Abschalten von Gates, keine Installationsfreigabe ohne dokumentierte grüne Tests und Build. Vollständige andere Projekt-Chats sind über den GitHub-Zugriff nicht einsehbar.
