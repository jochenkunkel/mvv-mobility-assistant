# CI-Release-Gate: unabhängige Jobs und APK-Artefakte (17.11.2026)

## Verifizierter statischer Befund

In `.github/workflows/android.yml` hängen `build-and-test` und `emulator-smoke-test` jeweils von `preflight` ab. Die beiden MVV-Live-Probes sind separate Jobs ohne `needs`-Abhängigkeit. `build-and-test` lädt bei eigenem Erfolg das Debug-APK-Artefakt hoch. **Damit kann ein APK-Artefakt verfügbar sein, obwohl ein Release-Emulatortest oder eine MVV-Probe fehlschlägt.** Ein vorhandenes APK ist folglich kein Nachweis für die gesamte Release-Matrix.

Der reguläre Push führt die Release-only-Jobs absichtlich nicht aus. Der Workflow unterscheidet `workflow_dispatch(test_level=release)` bzw. Versionstags von normalen Pushes.

## Sicherheitsentscheidung

Keine ungetestete Änderung an Signing, Upload- oder Release-Workflow. Die bestehende Artefaktbenennung und Upload-Logik bleibt zunächst erhalten. Eine Installationsfreigabe darf ausschließlich anhand des **gesamten** konkreten Release-Workflow-Runs erfolgen, nicht anhand eines einzelnen erfolgreichen Build-Jobs oder eines herunterladbaren Artefakts.

## Nächstes kleines Arbeitspaket für Issue #8

1. Einen echten `workflow_dispatch`-Lauf auf `main` mit `test_level=release` ausführen und Run-ID, HEAD-SHA und Abschlusszustand aller fünf Jobs erfassen.
2. Falls die Release-Matrix erfolgreich ist, einen separaten finalen Gate-Job erwägen: `needs: [preflight, build-and-test, emulator-smoke-test, mvv-live-probe, door-to-door-live-probe]`, mit expliziter Fehlerprüfung. **Vorher** die Semantik von übersprungenen Jobs für Push/PR berücksichtigen.
3. Ein Release-Artefakt erst nach erfolgreichem Gesamt-Gate als freigegeben kennzeichnen; bestehende Debug-Artefakte weiterhin als nicht freigegeben behandeln.
4. Statischen Regressionstest für die Abhängigkeiten ergänzen und einen manuellen Release-Lauf zur Laufzeitverifikation wiederholen.

## Nachweisgrenzen dieses Eintrags

Die YAML-Jobstruktur wurde am 17.11.2026 direkt aus GitHub `main` gelesen (HEAD vor diesem Eintrag: `75ece3f1dde0fc1bb6a55af851c83c512ca68e42`). Kein Android-Test, kein GitHub-Actions-Lauf und keine Geräteprüfung wurden in diesem Eintrag ausgeführt. Projekt-Chats sind nicht vollständig live abrufbar.

Verknüpfung: [Issue #8](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/8), [Workflow](../.github/workflows/android.yml).
