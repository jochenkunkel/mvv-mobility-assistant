# CI-Nachweis und APK-Freigabe – Prüfprotokoll

Stand: 16.11.2026. Dieses Dokument ist ein **Prüfverfahren**, kein Nachweis eines bestandenen Laufs. Bezug: [Issue #8](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/8), [Android-CI](../.github/workflows/android.yml), [Store-Readiness](STORE-READINESS.md).

## Tatsächliche CI-Gates aus `.github/workflows/android.yml`

| Ereignis | Preflight | Unit-Tests + Debug-APK + Lint | MVV-Live-Probes | Emulator-Smoke |
|---|---|---|---|---|
| Push auf Branch / PR | vorgesehen | nach erfolgreichem Preflight | **übersprungen** | **übersprungen** |
| Manueller Workflow-Dispatch `test_level=fast` | vorgesehen | nach erfolgreichem Preflight | **übersprungen** | **übersprungen** |
| Manueller Workflow-Dispatch `test_level=release` | vorgesehen | nach erfolgreichem Preflight | vorgesehen | vorgesehen |
| Tag `v*` | vorgesehen | nach erfolgreichem Preflight | vorgesehen | vorgesehen |

**Wichtig:** Ein grüner Push-Lauf ist kein vollständiger Release-Test. `preflight` ist eine harte Voraussetzung für `build-and-test` und `emulator-smoke-test`; bei Fehlern zuerst den Preflight-Job prüfen. Live-Probes laufen unabhängig vom Preflight und benötigen externe MVV-Dienste. Ein externer Ausfall ist gesondert zu diagnostizieren, nicht als bestandener Test zu verbuchen.

## Reproduzierbare Prüfung eines konkreten Commits

1. In [GitHub Actions – Android CI](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/android.yml) einen **tatsächlich gestarteten** Lauf auf dem gewünschten `main`-SHA suchen. Nur den Namen eines Commits zu kennen genügt nicht.
2. Für Zwischenstände `preflight` und `build-and-test` einzeln auf `success` prüfen; auch die ausgeführte SHA und den Zeitpunkt notieren.
3. Für Gerätefreigabe **Workflow ausführen → Branch main → test_level: release**. Den Lauf und dessen Commit-SHA notieren. Danach Preflight, Build/Lint, beide MVV-Live-Probes und Emulator-Smoke **einzeln** bewerten.
4. Bei rotem Preflight zuerst den Schritt `Check shell and embedded Python before any build (verbose diagnostics)` samt `bash -x scripts/preflight.sh` und `scripts/test-emulator-navigation.py` lesen. Keine Buildfreigabe allein aufgrund eines grünen Einzeljobs.
5. Artefakte nur dem **gleichen erfolgreichen Workflow-Lauf** zuordnen. Debug-APK und `mein-weg-signed-update-apk` unterscheiden: Letzteres erscheint nur bei korrekt konfigurierter stabiler CI-Signatur. Keine Secrets oder Keystore-Daten in Issues/Logs übertragen.
6. Emulator-Screenshots und Logs ansehen; drei verschiedene Screenshot-Hashes beweisen **keine** visuelle Designparität. S25-Viewport im Emulator ist **kein** echter S25-Gerätetest.
7. Vor APK-Empfehlung auf dem realen Gerät Standortberechtigung, GPS-Ausfall, Off-Route-Rerouting, erhaltenen GPS-Track, Zielkoordinaten/Bussteig, Kalender und App-Update-Verhalten manuell abnehmen. Testergebnis, App-Version, SHA und Gerät dokumentieren.

## Ergebnisvorlage

- **Commit-SHA / Branch:**
- **Workflow-Run-ID und URL / Ereignis / test_level:**
- **Preflight / Unit / Lint / APK / Emulator / MVV-Probes:** jeweils success, failure, skipped oder unknown
- **Artefaktname und Downloadnachweis:**
- **Screenshots visuell geprüft:** ja/nein
- **Reales Samsung S25 getestet:** ja/nein; getestete Szenarien
- **APK-Status:** nicht freigegeben / Testversion freigegeben (mit Begründung)
- **Offene Risiken / nächster Schritt:**

Die GitHub-Connector-Abfrage für commitbezogene Workflow-Runs kann Push-Läufe auslassen. `workflow_runs: []` oder `statuses: []` bedeutet deshalb **unbekannt**, nicht grün oder rot. Bei fehlendem Zugriff auf tatsächliche Actions-Jobs die Verifikation als offen dokumentieren.
