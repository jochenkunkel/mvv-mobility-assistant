# Release-Matrix: überprüfbare Abnahme (17.11.2026, 04:20 MEZ)

## Bestätigter Repository-Zustand
GitHub `main` HEAD vor diesem Eintrag: `c2a07f8e0721a5da34cea83e0694eec02f4e71a2`. Neun offene Issues, keine offenen Pull Requests. Die fünf Jobs des Release-Pfads sind `preflight`, `build-and-test`, `emulator-smoke-test`, `mvv-live-probe` und `door-to-door-live-probe`. Die beiden Probes laufen nur für `workflow_dispatch` mit `test_level=release` oder Tags `v*`. Der Emulator-Smoke-Test ist ebenfalls releasegebunden. Ein normaler Push kann deshalb keine Release-Abnahme ersetzen.

## Konkrete Abnahme-Checkliste für Issue #8
1. HEAD-SHA von `main` unmittelbar vor dem manuellen Lauf erfassen; `workflow_dispatch` auf genau dieser Revision mit `test_level=release` ausführen.
2. Run-ID, URL, Event, SHA und Abschlusszustand **jedes** der fünf Jobs aus GitHub Actions erfassen. `success` ist für alle fünf erforderlich; `skipped`, `cancelled`, `neutral`, `failure` oder fehlende Ergebnisse sind **keine** Freigabe.
3. Unit-Tests, Android-Lint, APK-Signaturprüfung, beide Live-Probes, Instrumentation/Emulator und tatsächliche Screenshots mit den konkreten Job-Logs belegen. Emulator-Screenshots sind kein S25-Gerätetest.
4. Nur Artefakte desselben nachweislich erfolgreichen Run/SHA als Kandidaten betrachten. Debug-APK-Artefakt und signiertes Update-Artefakt sind nicht automatisch Release-Freigaben; Signing/Secrets unverändert lassen.
5. Nach Codeänderungen vollständige Matrix erneut ausführen. Wenn der Actions-Zugriff nur Commit-Statuses statt Workflow-Runs liefert, bleibt das Gate **unverifiziert**.

## Technische Randfälle
- `build-and-test` hängt nur von `preflight` ab. Ein Artefakt kann trotz fehlgeschlagenem Emulator oder Probe existieren.
- Die Concurrency-Gruppierung nach Event/Ref verhindert nicht zwingend das Ersetzen wartender Runs derselben Gruppe. Einen Lauf mit Run-ID kontrollieren.
- Für normale Pushes sind Release-only-Jobs absichtlich übersprungen. Ein zukünftiger Gate-Job muss diese Semantik explizit behandeln, statt `skipped` pauschal als Erfolg zu interpretieren.
- `get_commit_combined_status` liefert klassische Commit-Statuses, nicht zwingend GitHub-Actions-Check-Runs; leere Statuslisten beweisen weder Erfolg noch Misserfolg.

## Nächster Umsetzungsschritt
Nach belegter Release-Baseline einen **separaten** finalen Gate-Job und einen statischen Regressionstest für die `needs`-Kanten entwerfen; das Hochladen bisheriger Debug-Artefakte nicht ohne abgestimmte Freigabesemantik verändern. Anschließend GPS-Lücken/Track-Erhalt und Off-Route-Rerouting mit deterministischen Tests absichern.

**Verifikation dieses Eintrags:** Statische Prüfung der aktuellen Workflow-Datei und Repository-Metadaten; kein Android-Test und kein Actions-Release-Lauf ausgeführt. Keine APK-Freigabe. Andere Projekt-Chats nicht vollständig live zugänglich.
