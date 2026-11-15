# Mein Weg 2.0 – Geräte-, Installations-, Update- und Store-Freigabeplan
Stand: 15.11.2026. **Qualitätsanforderung für jede künftige Version; kein Nachweis einer bereits erfolgten Store-Freigabe.**

## 1. Darstellung auf verschiedenen Geräten
| Klasse | Mindestprüfung | Akzeptanz |
|---|---|---|
| Kompaktes Smartphone (ca. 360 dp Breite) | Start, Settings, Verbindungen, Fahrt | Kein abgeschnittener Text/überlappende Controls, Navigation erreichbar |
| Standard-Smartphone (ca. 393–412 dp) | Vollständiger End-to-End-Ablauf | Karten und Zeiten gut lesbar, wichtige Aktion ohne Sucherei |
| Großes Smartphone (ca. 480 dp) | Alle drei Tabs | Flächen sinnvoll genutzt, keine gestreckten Fließtextblöcke |
| Tablet/Querformat (ab 600 dp) | Navigation, Scrollen, Settings | Keine blockierten Controls; Layoutanpassung nach Befund |
| Große Schrift (1,3× und 2×) | Lange Adressen, Karten, Buttons | Kein verdeckter Inhalt, nutzbare Touchflächen |
| Dark Mode/hoher Kontrast | Grundansichten | Ausreichender Kontrast und erkennbarer Status |

**Ist-Zustand:** Emulator-UI-Test und drei Screenshot-Artefakte existieren, aber kein belegter Matrixlauf über alle Geräteklassen, kein automatischer Golden-Master-Vergleich. Daher keine visuelle Freigabe. Nach jeder wesentlichen UI-Änderung Screenshots je Größe erstellen und **wirklich ansehen**.

## 2. Installation und Deinstallation
1. Frisches Testgerät/Emulator ohne `de.mvv.mobility`: Release-APK installieren; Launcher, Berechtigungen, Start und MVV-Suche prüfen.
2. App-Daten in Einstellungen ändern (Zuhause, Gehgeschwindigkeit, Ankunftspuffer, Linienfilter).
3. App beenden/neustarten: Einstellungen bleiben; keine ungewollten Berechtigungsdialoge.
4. App deinstallieren: keine eigene Hintergrundaktivität oder Tracking; Android entfernt lokale App-Daten (keine Sicherung über App-Backup: `allowBackup=false`).
5. Neuinstallation: App verhält sich wie Erststart; Daten werden **nicht** wiederhergestellt. Dies ausdrücklich kommunizieren.

## 3. Update ohne Datenverlust
- `applicationId` bleibt `de.mvv.mobility`.
- Gleicher **dauerhafter Release-Signaturschlüssel** und **höherer `versionCode`** bei jedem Release zwingend.
- Test: signierte Release-Version N installieren, Einstellungen setzen, Release N+1 **darüber** installieren, Einstellungen und Funktion prüfen.
- Ein Debug-Build mit anderem Schlüssel lässt sich nicht zuverlässig in-place auf Release aktualisieren. Einmaliger Wechsel kann Deinstallation erfordern.
- **Keystore niemals committen.** Einmalig offline erzeugen, sicher und redundant außerhalb von GitHub sichern; nur benötigte GitHub-Secrets hinterlegen.
- Vorhandener Workflow: `.github/workflows/release.yml` („Signed Android Release“), dokumentierte Secrets im README. Release-APK und AAB prüfen, APK-Signatur und monotone Versionsnummer nachweisen.
- **Noch nicht belegt:** Vorhandensein der benötigten Secrets, erfolgreicher signierter Release-Build, Update-Test N→N+1. Keine automatische In-App-Updatefunktion.

## 4. Store-Readiness / Google Play
- Release-App-Bundle (AAB) mit korrekter Signierung; Play App Signing beim tatsächlichen Play-Release berücksichtigen.
- Ziel-SDK und Play-Richtlinien zum **Zeitpunkt der Veröffentlichung** erneut prüfen; aktueller Code: `targetSdk=35`, `minSdk=28`.
- Datenschutzerklärung mit Kalender-, Standort-, MVV- und Geocoding-Datenflüssen; Zweck, Weitergabe an externe Dienste, Speicherung und Löschung.
- Google-Play-Daten-Sicherheit und Berechtigungsangaben anhand realen Datenflusses ausfüllen, nicht raten.
- App-Name, Icon, Screenshots für verschiedene Geräte, Kurz-/Langbeschreibung, Supportkontakt, Alters-/Inhaltsangaben.
- Stabilität: Crash-/ANR-Prüfung, Offline-Fälle, Kalender/GPS abgelehnt, keine Verbindung, lange Adressen, Tageswechsel.
- Accessibility: TalkBack, ausreichender Kontrast, Touch-Ziele, große Schrift; Internationalisierung erst bewerben, wenn wirklich vorhanden.
- Vor Veröffentlichung interner Testtrack und reale Geräte; Store-Review und gegebenenfalls weitere Testanforderungen einplanen.

## 5. Automatische Gates
- GitHub: Preflight, MVV-Probe, Tür-zu-Tür-Probe, Build/Unit/Lint, Emulator-Smoke-Test.
- Bestehende Screenshot-Dateien: `01-start.png`, `02-verbindungen.png`, `03-dashboard.png` (Testartefakte, keine geprüften Goldens).
- Neu: Compose-Test `settingsOpenFromEveryTabAndCloseCleanly` (Commit `2d466e9f`); Ergebnis erst nach CI bewerten.
- Künftig: Settings-Persistenz nach Activity-Neustart, 360/412/600-dp-Emulator-Matrix, Schrift-Skalierung, signierter Install/Upgrade/Uninstall-Test.
- **Freigaberegel:** Kein „Store-fertig“ und keine Installations-Empfehlung allein auf Basis grüner CI. Für einen Release müssen alle genannten manuellen und automatisierten Gates belegt sein.

## Nächste Umsetzung
1. Phase-2B-CI und neuen UI-Test prüfen, Fehler beheben.
2. Echte visuelle Geräte-Matrix in CI einrichten und Screenshots auswerten.
3. Signierten Release-Build mit gesichertem Keystore konfigurieren und Updatepfad testen.
4. Phase 3 Verbindungskarten mit mehreren Displaygrößen gleichzeitig gestalten.
