## Aktueller Abnahmestand – 26.11.2026 (maßgeblich vor historischen Abschnitten)

- **Repository:** öffentlich, Apache-2.0; alle zuvor elf offenen Entwicklungs-Issues geschlossen (Live-Abfrage: 0 offene Issues, 0 offene PRs). Dies ersetzt keine Geräte- oder Store-Abnahme.
- **CI:** Vollständiger Android-Release-Testlauf [45813033231](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45813033231) ist im zentralen Projektstatus als grün mit sechs Jobs, 97 Unit- und 32 Instrumentierungstests dokumentiert. Das belegt keine physische S25-/S10-Prüfung.
- **Signing:** Der fehlgeschlagene signierte Release-Lauf [45814284873](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45814284873) bezog sich auf die *vorherige* Secret-Namenszuordnung. Commit [c8c81f6](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/c8c81f6121d89c0ed8493085048beb37c450921b) nutzt die vorhandenen stabilen `MEIN_WEG_CI_SIGNING_*`-Secrets. Ein **neuer erfolgreicher Signed-Release-Lauf nach diesem Fix ist hier nicht belegt**. Keine Secrets ändern oder veröffentlichen.
- **OSM:** Commit [a9787d2](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/a9787d226c90b52ac5391424878020ecac06c024) lockert die Geocoder-Näheprüfung auf 75 m; JVM-Tests und Kotlin-Kompilierung sind im Projektstatus als lokal erfolgreich dokumentiert. Der Fix ist **nicht** durch einen neuen vollständigen Release-Gate-Lauf oder einen echten Fußweg abgenommen.

### Nächste Freigabeschritte (in Reihenfolge)

1. Für **exakt den aktuellen HEAD** einen vollständigen Android-CI-Release-Testlauf mit `release-acceptance` nachweisen; bei Fehlschlag zuerst Ursache beheben.
2. Danach den separaten Signed-Release-Workflow für **denselben Commit** ausführen und erfolgreiche Signaturprüfung von APK und AAB sowie Versionsnummer und Artefakt nachweisen. Die bloße Änderung der Secret-Referenzen ist noch kein erfolgreicher Signierungsnachweis.
3. Primär auf dem **Samsung S25**, ergänzend S10: Erststart, Berechtigungen, Kalenderziel, echte MVV-Verbindung, OSM-Fußweg zur gewählten Haltestelle, Abbiege-/Off-Route-Verhalten, GPS-Lücken, Rotation während aktiver Navigation und dynamischen Ankunftspuffer protokollieren (Datum, Gerät/OS, App-Version, Testschritte, Soll/Ist, Screenshots ohne private Ortsdaten).
4. Installations-/Updatepfad N→N+1 mit identischer Signatur und höherem `versionCode` sowie Settings-Persistenz prüfen; anschließend Layout-/Schriftgrößenmatrix und Store-Policy abschließen.

**Freigabe:** Bis zum Nachweis aller einschlägigen Gates keine neue APK-Installations- oder Store-Empfehlung. Die folgenden Abschnitte sind historische Planungsstände und können überholte Statusaussagen enthalten; aktuelle Fakten stehen auch in [PROJECT_STATUS.md](PROJECT_STATUS.md).

---

# Mein Weg 2.0 – Geräte-, Installations-, Update- und Store-Freigabeplan
Stand: 15.11.2026. **Qualitätsanforderung für jede künftige Version; kein Nachweis einer bereits erfolgten Store-Freigabe.**

## Primäres Referenzgerät: Samsung Galaxy S25 (Standardmodell)
**Verbindliche Priorität:** Das reguläre Samsung Galaxy S25 ist das erste reale Abnahmegerät. Andere Smartphones, Tablets und Schriftgrößen bleiben zusätzliche Kompatibilitätsziele. S25 Ultra/Plus sind keine Ersatzprüfung für das Standard-S25.

**S25-Abnahme:** Installieren, erster Kalender-/Standort-Prompt, Kalendertermin auswählen, Adresse automatisch auflösen, GPS bei aktiver App aktualisieren und beim Verlassen stoppen, MVV-Verbindung suchen, Fahrt öffnen, Settings speichern, App beenden/neustarten, signiertes Update über bestehende Installation, anschließend Deinstallation und Neuinstallation. Alle drei Tabs und Settings bei Standard- und vergrößerter Schrift auf dem Gerät visuell kontrollieren. One-UI-Berechtigungsdialoge und Android-Energiesparverhalten ausdrücklich beachten.

**Status:** Noch **kein belegter Test auf einem echten Galaxy S25**; die bisherige CI nutzt einen Android-Emulator. Keine S25-Freigabe ohne realen Nachweis.

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

**CI-Erweiterung:** Commit `032ff546` erzeugt zusätzlich Emulator-Screenshots für ein **nur angenähertes** Galaxy-S25-Viewport (1080×2340, 420 dpi) und ein kompaktes 360-dp-Viewport. Diese Bildschirmkonfiguration ist kein Samsung-Gerät, kein One-UI-Test und kein Beweis für visuelle Fehlerfreiheit; Artefakte müssen manuell geprüft werden. Der reale S25-Test bleibt verpflichtend.

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

## Phase 3B – S25-Kartenlayout (ungeprüft)
- Commit `ba82d765` verwendet ein kompaktes Linien-Badge, einen flexibel umbrechenden Haltestellennamen und untereinander stehende Kennzahlen für Umstiege und geschätzte Gehzeit. Doppelte Einsteigeinformation wurde reduziert.
- Commit mit `[skip ci]` erstellt, um keine weiteren Builds zu verbrauchen. **Nicht kompiliert, nicht im Emulator ausgeführt und nicht visuell freigegeben.** Vor Installation nächsten regulären CI-Lauf und echte Screenshot-Sichtkontrolle abwarten.

## Nächste Umsetzung
1. Phase-2B-CI und neuen UI-Test prüfen, Fehler beheben.
2. Echte visuelle Geräte-Matrix in CI einrichten und Screenshots auswerten.
3. Signierten Release-Build mit gesichertem Keystore konfigurieren und Updatepfad testen.
4. Phase 3 Verbindungskarten mit mehreren Displaygrößen gleichzeitig gestalten.
