# Code-Review und Reparaturplan – 16.11.2026

**Review-Basis:** `main` bei `1d185a85ea042ceb02bae1211b71a67315ced4a3`. Review anhand der GitHub-Quellen, keine lokale Android-SDK-Kompilierung, kein Emulator und kein echter Gerätetest in dieser Sitzung. **Keine Freigabe.** Befunde sind als *nachgewiesen im Code*, *Risiko* oder *noch zu prüfen* klassifiziert. Frühere Audit-Befunde sind nicht ungeprüft als weiterhin offen übernommen worden.

## Verbindliche Qualitätsmaßstäbe
- Android Developers: Location updates, Location accuracy, foreground/background lifecycle: https://developer.android.com/develop/sensors-and-location/location/request-updates
- Android Developers: Architecture recommendations (UI state, ViewModel, testability): https://developer.android.com/topic/architecture/recommendations
- Android Developers: Testing in Jetpack Compose: https://developer.android.com/develop/ui/compose/testing
- Android Developers: App signing: https://developer.android.com/studio/publish/app-signing
- OSRM Route API / steps / geometry: https://project-osrm.org/docs/v5.24.0/api/#route-service
- OpenStreetMap tile usage policy: https://operations.osmfoundation.org/policies/tiles/
- OWASP MASVS: https://mas.owasp.org/MASVS/

## Kritische Befunde, sortiert nach Priorität

| ID | Priorität | Nachweis / Datei | Diagnose | Reparatur und Abnahme |
|---|---|---|---|---|
| NAV-01 | P0 vor Navigationsfreigabe | `data/WalkingNavigation.kt::nextTurn` | **Code-Nachweis:** Der geografisch nächste Manöverpunkt wird gewählt und bei <25 m sofort übersprungen. Das ist kein monotoner Fortschritt entlang der Route; vor der Kreuzung, bei Schleifen, Gegenrichtung und GPS-Sprüngen kann die falsche Anweisung erscheinen. | Route entlang der Polylinie auf Segmente projizieren, entlang-Route-Distanz und Fortschrittszustand modellieren, Off-Route-Erkennung und Hysterese ergänzen. Tests für Kreuzung davor/danach, parallele Wege, U-Turn, GPS-Sprung, Schleife. |
| NAV-02 | P1 | `ui/components/WalkingMiniMap.kt` | **Code-Nachweis:** Heading-Fallback nimmt das nächstgelegene *Startvertex* eines Segments, nicht die nächste Segmentprojektion. Der gewählte Richtungsvektor kann an Knicken/Schleifen unzutreffend sein. Rotation, Kamera und Pfeil werden nicht durch instrumentierte Tests belegt. | Gemeinsame Route-Progress-Engine für Karte und Abbiegehinweis, deterministische Geometrie-Tests und Emulator-Screenshots bei 0/90/180/270 Grad; Karte nur bei gültiger Position als aktuell kennzeichnen. |
| NAV-03 | P1 | `data/WalkingNavigation.kt::fetch` | **Code-Nachweis:** Routing-Ziel wird erneut aus dem *Haltestellennamen* geokodiert, statt die Koordinaten des tatsächlich gewählten MVV-Einstiegs zu verwenden. Erster Geocoder-Treffer kann eine andere Haltestelle sein. | Koordinaten und eindeutige Stop-ID aus gewähltem JourneyStep/Stop-Objekt übergeben; Abweichung vom Einstiegspunkt prüfen; Tests mit gleichnamigen Haltestellen. |
| TIME-01 | P1 | `data/MvvJourneyRepository.kt` | **Code-Nachweis:** `JourneyOption` speichert Abfahrt/Ankunft als `HH:mm`; `selectAppointmentJourneys` rekonstruiert den Tag aus dem Termin. Mitternacht und Sommerzeit sind nicht vollständig repräsentierbar. | Geplante und prognostizierte Instants mit Zeitzone aus API modellieren; Formatierung erst in UI; Tests für Tageswechsel, DST-Lücke, DST-Doppelstunde und Verspätung. |
| LOC-01 | P1 | `data/LocationFreshness.kt`, `CurrentLocationRepository.kt` | **Code-Nachweis:** Fix-Alter basiert auf `Location.time`/Wall-Clock. Für robuste Altersbestimmung bei Uhrzeitänderung sollte auf Android `elapsedRealtimeNanos` bevorzugt werden. Standortabonnements fordern 5 s an, garantieren keine 5 s. | Alter über monotone Uhr, separate Genauigkeits-/Provider-/Timeout-Zustände, Tests für Uhrzeitwechsel und abgelaufene Fixes; alte Position sichtbar, aber nicht als live. |
| ARCH-01 | P1 | `MainActivity.kt` | **Code-Nachweis:** 1.640 Zeilen, u.a. Berechtigungen, Compose-UI, Netzwerk-Callbacks, Preferences, Navigation. Hohes Kopplungs-/Regressionrisiko. | StateHolder/ViewModel, Repository-Interfaces, klarer UI-State, Coroutine-Cancellation; Extraktion in kleinen getesteten Schritten. |
| CI-01 | P1 | `.github/workflows/android.yml` | **Code-Nachweis:** Bei gewöhnlichem Push laufen Preflight und Unit-/Build-Jobs; Lint, Live-Probes und Emulator sind auf Release-Dispatch/Tags beschränkt. Deshalb ist ein grüner Push keine vollständige Release-Validierung. | Branch-Schutz mit Pflichtjobs; für PRs zumindest Lint + Compose-Instrumentierung oder explizites Gate; vor APK-Freigabe kompletter Release-Lauf und Sichtprüfung. |
| TEST-01 | P1 | `.github/workflows/android.yml` | **Code-Nachweis:** Drei verschiedene Screenshot-Dateien werden per Hash verglichen; das erkennt weder visuelle Fehler noch Übereinstimmung mit Designvorlagen. | Golden-Screenshot-Vergleich, Accessibility-Checks, große Schrift, 360dp, Offline, Berechtigungsablehnung, echte GPS-Strecke. |
| DOC-01 | P2 | `docs/IMPLEMENTATION-ROADMAP.md`, `docs/PROJECT-STATUS-2026-11-15.md` | **Nachgewiesene Inkonsistenz:** Phasen 3–5 werden als offen oder ohne echte Fußweggeometrie beschrieben, obwohl inzwischen `WalkingNavigation.kt` und `WalkingMiniMap.kt` existieren. Historische Angaben bleiben datiert, dürfen aber nicht als aktueller Status gelten. | Dieses Audit als aktuellen Status verlinken; Roadmap-Status erst nach Tests auf „implementiert und verifiziert“ setzen. |
| DOC-02 | P2 | `docs/CODE-AUDIT-2026-11-15.md` | **Nachgewiesene historische Überholung:** Dort wird der fehlende Request-Generation-Guard als offen bezeichnet; `SearchResponseGuard.kt` und der entsprechende Import in `MainActivity.kt` existieren mittlerweile. Ebenso zählt `parseTrips` jetzt alle Fahrzeug-Legs, auch ohne Liniennummer. | Alte Audit-Datei als historischen Snapshot belassen; neue Befunde nicht ohne erneute Codeprüfung übernehmen. Integration des Guards in alle Callback-Pfade gesondert prüfen. |
| UX-01 | P2 | `docs/COCKPIT-V2-PLAN.md`, `design/UI_TESTPLAN.md` | **Risiko:** Drei-Screen-Design, Ein-Klick-Start und die Richtung-oben-Minikarte sind ohne aktuelle Referenz-Screenshot-Abnahme nicht als fertig belegbar. | Referenzbilder versionieren, reale Screenshots für Start/Verbindungen/Fahrt/Settings und Mini-Map vergleichen; Benutzerabnahme dokumentieren. |
| PRIV-01 | P2 | `ui/components/WalkingMiniMap.kt`, `data/WalkingNavigation.kt` | **Prüfpflicht:** OSM-Tiles, Routing- und Geocoder-Dienste erhalten Netzwerkanfragen mit Standort-/Zielbezug; Drittanbieter-Nutzungsbedingungen und Datenschutzhinweise müssen geprüft werden. | Dienst-/Attributions-/Cache-/Rate-Limit-Policy dokumentieren, Datenschutzinformation und Fehler-/Offline-Fallback testen. |

## Dokumentation vs. Implementierung – Statusmatrix

| Anforderung | Implementierung im Quellcode | Nachweisstand | Dokumentationskorrektur |
|---|---|---|---|
| Drei Tabs | Vorhanden | Emulator-Tests im Repo, aktuelle Ausführung nicht bestätigt | „Implementiert, letzte CI prüfen“ |
| Kalender-Ziel und automatische GPS-Vorbereitung | Vorhanden | Geräte-/Ablehnungstests nicht nachgewiesen | Nicht als gerätevalidiert markieren |
| MVV-Suche und Filter | Vorhanden | Unit- und Integrationsansätze vorhanden | Edge-Cases und API-Schema offen |
| OSM-Fußwegroute mit Geometrie | Vorhanden | Parser-Unit-Test vorhanden; echte Abbiegeführung nicht nachgewiesen | Phase 5 von „nicht vorhanden“ auf „Prototyp, nicht freigegeben“ |
| Richtung-oben-Minikarte | Vorhanden | Keine aktuelle visuelle Freigabe | „Implementiert, Geometrie-/Orientierungstests offen“ |
| Zuverlässige Navigation | Nicht nachgewiesen | NAV-01 bis NAV-03 blockieren Freigabe | Nicht „fertig“ nennen |
| APK-Release und Updates | Workflow vorhanden | Signierter N→N+1-Gerätetest nicht nachgewiesen | Release-Status offen |

## Reparaturreihenfolge mit Gates

1. **R0 – Baseline sichern:** HEAD, GitHub-Actions-Läufe, APK-Artefakte und Screenshots erfassen; keine Freigabe ohne belegten grünen *Release*-Lauf.
2. **R1 – Navigationskorrektheit (P0):** NAV-01/02/03 gemeinsam mit geometrischer Progress-Engine beheben; zuerst deterministische Tests, dann GPS-Replay und realer Fußweg.
3. **R2 – Zeitmodell und Suche:** TIME-01 und Callback-/Generation-Races, Abfahrts-/Ankunftszeit, DST und Haltestellenidentität testen.
4. **R3 – Standort und Lifecycle:** LOC-01, Accuracy, stale/offline, Start/Stop/Resume, Batterie und Datenschutz.
5. **R4 – Architektur:** MainActivity in testbare Zustandsverwaltung und getrennte Screens/Repositories extrahieren; nach jedem Schritt CI.
6. **R5 – Qualitätsgates:** CI-01/TEST-01, Golden Screenshots, TalkBack, Schriftgröße, kleine Geräte, reale Samsung-Tests, signiertes Upgrade.
7. **R6 – Dokumentation und Freigabe:** README/Roadmap/Entscheidungen mit überprüfbaren Commit- und Workflow-IDs aktualisieren; alte Berichte als historische Snapshots kennzeichnen.

**Definition of Done je Ticket:** reproduzierbarer Fehlertest → kleiner Fix → Unit-/Instrumentierungstest → kompletter Release-CI-Lauf → Screenshot-/Geräteprüfung (wo relevant) → Dokumentation mit Commit-SHA und Testergebnis. Bis dahin Status „offen“ oder „implementiert, nicht verifiziert“.

**Review-Grenze:** Dies ist ein quellenbasierter Review der zentralen Navigation, Standort-, MVV-, UI-, CI- und Dokumentationspfade, kein Beleg einer lückenlosen Prüfung jeder Repository-Datei und kein ausgeführter Testlauf.
