# Android-CI Preflight: reproduzierbare Fehlerisolierung (22.11.2026)

## Neuer Befund – 23.11.2026, 08:20 MEZ (Actions #491)

- Der aktuelle Push-Lauf [Android CI #491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723), Run-ID `45100034723`, SHA `0b71b0b70dd02c9dbe5d3d8cff1b150aea64588f`, ist `completed/failure`. Preflight-Job `137448513229` = `failure`; die fünf anderen Jobs sind `skipped`.
- Die **Jobschritt-API liefert null Schritte**, der gezielte Logabruf liefert HTTP 404 `BlobNotFound`, und die Artefakt-API meldet `total_count: 0`. Daher gibt es **keinen Beleg, dass auch nur einer der drei Python-/Shell-Preflight-Befehle gestartet wurde**. Die GitHub-Klassifikation `preflight=failure` ist keine Diagnose eines fehlerhaften Skripts.
- **Diagnoseentscheidung:** Vor weiteren Änderungen an CI-Guards oder Anwendung zuerst prüfen, ob der Job überhaupt einen Runner erhalten hat (GitHub-Actions-Weboberfläche: Jobinitialisierung/Runner-Zuweisung, Repository-Actions-Einstellungen, Runner-Kapazität, gegebenenfalls Abrechnungs-/Nutzungslimits). Dies sind mögliche Ursachen, keine bestätigten Befunde. Wenn auch der unabhängige Diagnoseworkflow keine Schritte erzeugt, Infrastruktur-/Runnerproblem priorisieren statt Produktcode zu verändern.
- **Erst nach bestätigtem Runner-Start** die drei Befehle aus der untenstehenden Anleitung mit Exitcode und Traceback reproduzieren. Keine APK-Freigabe; keine Änderung an Signing, Secrets oder Release-Gates.

## Gesicherte Beobachtung
Der historische, zum Zeitpunkt der Erstfassung belegte fehlgeschlagene Lauf ist [Android CI #459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673): Preflight fehlgeschlagen, nachgelagerte Jobs übersprungen. Der aktuelle Push-Run-Status ist **nicht** über die commitbezogene Connector-Abfrage beweisbar, weil diese nur PR-Läufe abdeckt. Der ursprüngliche Fehlercode ist nicht bekannt. Die inzwischen ergänzten Diagnose- und Newline-Korrekturen sind **keine nachgewiesene Reparatur der Ursache**.

## Nächste Diagnose (keine APK-Freigabe)
1. Unter [Actions / Android CI](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/android.yml) den neuesten Lauf für `main` und dessen Commit-SHA feststellen. Nicht den historischen #459 als aktuellen Lauf behandeln.
2. Beim Job `preflight` den **ersten** fehlgeschlagenen Schritt und dessen vollständige Ausgabe lesen. Bei einem Fehlschlag das Artefakt `android-preflight-failure` öffnen: `results.txt` enthält drei unabhängige Exitcodes, `check-1.log` (CI-Policy), `check-2.log` (Shell/Python-Syntax), `check-3.log` (Emulator-Helfer). Bei Checkout-Fehlern kann das Artefakt fehlen.
3. Alternativ [Preflight diagnostics](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/preflight-diagnostics.yml) manuell auf `main` starten; das Artefakt `preflight-diagnostics` enthält `environment.txt`, `results.txt` und die drei Logs. Dieser Workflow erzeugt **keine APK**.
4. Lokal auf einem frischen Checkout mit Python 3 und Bash ausführen:
   ```bash
   python3 scripts/test-ci-concurrency.py
   bash -x scripts/preflight.sh
   python3 -u scripts/test-emulator-navigation.py
   ```
   Die **erste** abweichende Fehlermeldung mit Exitcode und Commit-SHA dokumentieren. Erst dann eine zielgerichtete Änderung vornehmen.
5. Danach einen neuen CI-Lauf auf dem Korrektur-Commit abwarten und alle verpflichtenden Jobs prüfen. Debug-APK oder Installation nicht allein wegen erfolgreichem Preflight freigeben.

## Fehlerklassen und konkrete Folgeschritte
- **Checkout/Runner:** Repository-Dateien fehlen; Runner-/Checkout-Logs und `environment.txt` prüfen, keine Codefixes raten.
- **Check 1:** Assertion und Workflow-Zeile aus `check-1.log` vergleichen; statischen Guard nicht lockern, nur um CI grün zu machen.
- **Check 2:** Den in `check-2.log` genannten Dateinamen und Python-/Bash-Syntaxfehler isolieren.
- **Check 3:** Traceback des Navigation-Helfertests mit seinem Fixture abgleichen; keinen produktiven Navigationscode ohne Reproduktion ändern.
- **Nur Timeout:** Auf Hänger, externe Abhängigkeiten und Runnerlast untersuchen; kein blindes Anheben der Limits.

## Nachgelagerte fachliche Prioritäten
Nach der belegten CI-Reparatur: #13 Rotation/Activity-Recreation mit Erhalt von Start, Ziel und laufender Route; #14 echte GPS-Trackhistorie unabhängig von der Routenprojektion, Erhalt bei Fix-Lücken, Re-Routing und Redraw. Dafür zunächst reproduzierbare Unit-/Instrumentierungstests, dann die kleinste notwendige Implementierung. Vollständige Live-Verläufe anderer Projekt-Chats sind nicht automatisch über GitHub abrufbar.
