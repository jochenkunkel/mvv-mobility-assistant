# Mein Weg 2.0 – Entwicklungs- und Freigabeplan

## CI-Triage – 24.11.2026, 09:20 MEZ

- **Neu verifiziert:** [Android-CI #507](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45304391960), Job `138144525407`: `preflight=failure`, fünf weitere Jobs `skipped`. Der Job-Schritt-Endpunkt liefert `[]`, der Jobdatensatz `steps=null` und `logs_url=null`. Dies belegt **keinen** Skript-Exitcode und **keinen** bestimmten Runner-Fehler. Keine grüne CI und keine APK-Freigabe.
- **Diagnoseentscheidung:** Nicht weitere Änderungen an `preflight.sh` oder CI-Gates auf Verdacht. Zuerst in Actions-UI den fehlgeschlagenen Job öffnen und nach Setup-Job-/Runner-Zuweisungsfehlern und Annotations suchen. Falls weiterhin keine Schritte vorhanden sind: Actions-/Runner-Einstellungen, Abrechnung/Kontingente und GitHub-Status prüfen. Nur bei vorhandenen Schrittlogs zuerst den konkreten fehlschlagenden Befehl reproduzieren.
- **Unabhängige fachliche Arbeit:** #13 benötigt einen Instrumentierungstest mit *aktiver* Navigation und `ActivityScenario.recreate()` (Route, Ziel, Tab, GPS-Spur vor/nach Rotation); #14 benötigt JVM-Tests für Track-Lücken und ungültige Fixes. Diese Tests dürfen unabhängig von der CI-Triage auf einem eigenen Branch entwickelt werden; ohne ausführbaren Testnachweis nicht mergen oder zur Installation freigeben.
- **Zugriffsgrenze:** Andere Projekt-Chats sind nicht als vollständige Live-Verläufe zugänglich. Keine neuen Anforderungen aus unbestätigten Chat-Aussagen übernommen.

## Aktuelle Prioritäten – 24.11.2026, 02:20 MEZ

Dieser Abschnitt ergänzt die historischen Phasenstände unten; maßgeblich sind `main`, [PROJECT_STATUS.md](PROJECT_STATUS.md), [GPS-/Rotations-Testmatrix](GPS-ROTATION-REGRESSION-MATRIX.md) und die Issues.

1. **P0 – CI-Nachweis wiederherstellen:** Der zuletzt belegte Push-Lauf [#504](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45268631001) scheiterte im Preflight, Folgejobs wurden übersprungen. Jobschritte fehlen in der verfügbaren Antwort; ein Logabruf ergab `404 BlobNotFound`. Die Fehlerursache ist **unbekannt**. Über Actions-Weboberfläche Setup-/Runner-Logs, Actions-Berechtigungen und ggf. Kontingente prüfen; erst dann gezielt reparieren. Lokal die drei Preflight-Kommandos mit Exitcodes ausführen. Kein APK-Go.
2. **P0 – #13 Rotationsverlust:** In [LIFECYCLE-GPS-CODE-AUDIT](LIFECYCLE-GPS-CODE-AUDIT-2026-11-19.md) sind `dashboardJourney`, `walkingRoute`, `walkedTrack` und `selectedTab` als Compose-`remember`-Zustände dokumentiert. **Nächstes ausführbares Paket:** instrumentierten Test mit `ActivityScenario.recreate()` und aktiver Route/Track (ROT-01) erstellen, zunächst erwarteten Fehler belegen; dann einen klaren Navigation-Session-StateHolder mit definiertem Lifecycle implementieren. Keine ungetestete Bundle-Serialisierung komplexer Routen.
3. **P0 – #14 GPS-Spur:** Track und geplante Route getrennt halten. Reine JVM-Tests für GPS-01 bis GPS-05 (Fix-Akzeptanz, Lücken, Sprünge, Rerouting) vor UI-Änderung; anschließend echte Tracksegmente rendern, keine Verbindungslinien über Lücken. In `WalkingMiniMap` ist `walkedTrack` als Eingabe vorhanden, das belegt aber **nicht** deren korrekte Lebensdauer oder Zeichnung.
4. **P1 – Zeitpuffer, MVV und UX:** Erst nach den P0-Regressionsnachweisen dynamischen Puffer, MVV-Zielkoordinaten und Interaktion gegen bestehende Issues prüfen; keine neuen Behauptungen ohne Code-/Testnachweis.

**Stand dieser Prüfung:** `main` zuletzt `0c41be9`, elf offene Issues, keine offenen PRs; keine ausführbaren Tests in diesem Lauf. Andere Projekt-Chats sind nur über den vorhandenen Kontext zugänglich, nicht vollständig live. Diese Roadmap-Aktualisierung ist **kein** bestandener Build und keine Gerätefreigabe.


> **Aktueller Abgleich (16.11.2026):** Die Phasenstände und die Tabelle unten sind
> historische Planungs-Snapshots vom 15.11.2026, **kein aktueller Nachweis**
> des Repository-Zustands. Inzwischen existieren OSRM-Fußwegrouting,
> Abbiegeanweisungen, eine OSM-Minimap sowie erste Routengeometrie-Korrekturen
> in `main` (Commits `f38ef6e` und `5a40a25`). Das ist **implementiert**,
> aber ohne vollständige CI-, Screenshot- und Geräte-Abnahme **nicht freigegeben**.
> Verbindliche offene Fehler und Abnahmekriterien stehen in
> [CODE-REVIEW-2026-11-16.md](CODE-REVIEW-2026-11-16.md) und den
> [GitHub-Issues](https://github.com/jochenkunkel/mvv-mobility-assistant/issues).
> Insbesondere Phase 5 „echte Navigation offen“ bedeutet heute:
> *Prototyp vorhanden, Zuverlässigkeit und Freigabe offen*.

## Aktueller Arbeitsstand und Chat-Übergabe (16.11.2026)

**GitHub `main` ist die einzige verbindliche technische Quelle.** Andere Chats im
Projekt können auf denselben Code und diese Dokumentation Bezug nehmen; es
besteht jedoch **keine garantierte automatische Echtzeit-Synchronisation**
ihrer Nachrichten, internen Pläne oder parallelen Änderungen. Vor Änderungen
immer `main`, Issues und CI neu prüfen. Keine Chat-Aussage ersetzt einen
Commit oder Testnachweis.

**Nach dem ursprünglichen Audit auf `main` umgesetzt:**
- NAV-01: routenrelative Abbiegeauswahl mit Regressionstests
  (`f38ef6e`; Issue #2 geschlossen, Geräteabnahme weiterhin erforderlich).
- NAV-02: Segmentprojektion für die Kartenrichtung samt Unit-Tests
  (`5a40a25`; Issue #3 offen).
- DOC-01: historische Roadmap als Snapshot gekennzeichnet
  (`158a151`; Issue #10 offen).
- CI-01: Android-Lint bei jedem Push/PR statt nur beim Release
  (`fe7ee3b`; Issue #8 offen).
- LOC-01: monotone GPS-Freshness für zwischengespeicherte Positionen und Tests
  (`206e549`; Issue #6 offen).

**Nicht als abgeschlossen bestätigt:** NAV-03 (#4), TIME-01 (#5),
ARCH-01 (#7), TEST-01 (#9), PRIV-01 (#11), sowie die übrigen
Abnahmekriterien der oben genannten Teilfixes. Der Code-Review vom
16.11.2026 ist ein **historischer Audit-Befund**; seine damalige
Prioritätenliste darf nicht ungeprüft als aktueller Ticketstatus gelesen werden.

**Freigabestatus:** Für die oben genannten jüngsten Commits ist hier kein
vollständiger grüner CI-Lauf, Emulator-Screenshotvergleich oder realer
Gerätetest nachgewiesen. Deshalb keine pauschale Produktionsfreigabe.

Stand: 15.11.2026. **Ausführungsplan** zum verbindlichen Anforderungskatalog [COCKPIT-V2-PLAN.md](COCKPIT-V2-PLAN.md). Dieser Plan dokumentiert Vorgehen und Abnahmekriterien, keine Zusage autonomer Hintergrundentwicklung.

## Ziel und Arbeitsregeln
- Produktziel: Ein kompaktes, visuelles, zuverlässiges Drei-Tab-Cockpit (Start, Verbindungen, Meine Fahrt) mit minimaler Klickzahl.
- **Ein Arbeitspaket pro Commit/kleiner Commit-Gruppe**; Änderungen möglichst lokal und rücksetzbar.
- Vor jedem Paket: GitHub-HEAD, laufende Workflows und letzte Fehler prüfen; keine parallelen widersprüchlichen Änderungen.
- Pro Paket: implementieren → gezielte Tests → vollständige CI → Emulator-Screenshots → Sichtprüfung → Status und offene Risiken dokumentieren.
- **Grün bedeutet technisch geprüft, nicht automatisch UX-freigegeben.** APK erst nach CI und Sichtprüfung empfehlen.
- Bei fehlschlagenden Tests zuerst Fehler beheben. Kein „fertig“ ohne belegte Resultate.
- GitHub-Commits erfolgen nur bei aktiver Bearbeitung oder explizit eingerichteter Automation; kein unbelegtes Versprechen kontinuierlicher Hintergrundarbeit.

## Phase 0 – Baseline und Absicherung
**Arbeitspakete:** Bestehenden Build-Status des HEAD prüfen; Screenshot-Suite für Start, geöffnetes Start-&-Ziel-Modul, Verbindungen mit/ohne Treffer und Meine Fahrt vervollständigen; Testdaten von echtem Live-Betrieb unterscheiden. Screenshot-Sollzustände und bekannte Probleme festhalten.
**Abnahme:** Alle Tests grün und nachvollziehbare Screenshot-Artefakte; keine Regression beim bestehenden Routenabruf.
**Stand:** Bestehende CI-/Screenshot-Infrastruktur vorhanden; nach letztem Code-Commit neu zu verifizieren.

## Phase 1 – Automatischer Start (höchste Priorität)
**1A Kalender:** Android-Berechtigung beim ersten Start, Ablehnung/Wiederholung; automatisch gewählten Termin anzeigen; ganze Karte öffnet Auswahl; Wechsel setzt abhängige Route zurück. Test: Erlaubt/abgelehnt/kein Termin/Terminwechsel.
**1B Ziel:** Kalenderadresse als tatsächlicher Feldwert; asynchrones Lookup automatisch und genau einmal pro relevantem Ziel; eindeutigen Treffer übernehmen, Mehrdeutigkeit/Offline/Fehler zeigen; keine doppelte Adresse. Test: eindeutige/mehrdeutige/leere/ungültige Ziele, Race Conditions.
**1C Start:** Standort mit Berechtigung automatisch abrufen; fehlende Berechtigung, Timeout, deaktivierte Dienste und veraltete Position abfangen; „Standort aktualisieren“ nur bei Bedarf; optional manueller Start. Test: Erfolg/Fehler/Ablehnung/stale.
**1D Abschluss:** Wenn Daten vollständig, eine Primäraktion „Verbindung finden“; klarer Lade-/Fehlerzustand, keine doppelten Klickketten. Test: echter Ablauf und Fixture.
**Abnahme:** Mit vorhandenem Kalendertermin und erteilter Berechtigung keine separaten Adressprüfungs-/GPS-Pflichtklicks; Fehler bleiben korrigierbar.
**Stand:** Automatische Kalenderanfrage und antippbare Terminkarte in Commit `6b4471e7` implementiert, CI/UX noch nachzuweisen. Rest offen.

## Phase 2 – Echte Einstellungen
**2A:** Zahnrad öffnet klaren eigenständigen Settings-Bereich statt scheinbar leerem Panel; sinnvolle Navigation zurück.
**2B:** Zuhause, Gehgeschwindigkeit, Ankunftspuffer und unterstützte Linien-/Verkehrsmittelpräferenzen mit wirksamer Persistenz und Validierung.
**2C:** Berechtigungsstatus und Datenschutz transparent; nicht implementierte Sprache/Optionen nicht als wirksam darstellen.
**Abnahme:** Änderung → App-Neustart → Wert bleibt und beeinflusst entsprechende Funktion. Tests für Grenzwerte/Neustart.
**Stand:** Einige Preferences und Panel vorhanden; UX-Überarbeitung offen.

## Phase 3 – Verbindungs-Cockpit
**3A:** Einheitliches Kartenmodell: Abfahrt–Ankunft, Linie(n), Umstiege, Gehzeit, Losgehzeit, Zielzeit.
**3B:** Beste Verbindung hervorheben, Alternativen vergleichen; lange Details aufklappbar.
**3C:** Ergebnis-/Leer-/Fehler-/Ladezustände sauber trennen, Datum und Filter sichtbar; externe Google-Maps-Aktion sekundär.
**Abnahme:** Screenshots mit 0/1/mehreren Treffern und lange Linien-/Haltestellennamen; keine widersprüchlichen Texte.
**Stand:** Suchfunktion vorhanden; Kartendesign offen.

## Phase 4 – Fahrt-Dashboard
**4A:** Kachelraster für Fußweg (Meter/Minuten), Abfahrt, Losgehen, Ankunft; große Werte, kleine Labels, Icons.
**4B:** Statusmaschine „später“ / „bald losgehen“ / „jetzt losgehen“ / „unterwegs“ / „abgeschlossen“, ohne irreführendes „Jetzt“ bei Stunden Vorlauf.
**4C:** Sichtbarer Countdown anhand aktueller Zeit neu berechnen; geplante und geschätzte Angaben markieren.
**Abnahme:** Countdown und Statusübergänge mit kontrollierter Uhrzeit testen; Screenshots für unterschiedliche Zeitabstände.
**Stand:** Basis-Dashboard vorhanden; echtes Kachel- und Statuskonzept offen.

## Phase 5 – Verlässliche Live-Daten und optionale Navigation
**5A:** MVV-Datenquelle und Aktualisierungszeitpunkt explizit; regelmäßige Aktualisierung nur bei aktiver Ansicht, mit Fehler-/Offline-Fallback.
**5B:** Aktive Positionsaktualisierung nur mit Android-Berechtigung und klar erkennbarem Nutzungszustand; Gehstrecken-Geometrie vor meter- oder abbiegegenauer Live-Navigation.
**5C:** Batterie-, Datenschutz-, Hintergrund- und Lebenszyklusverhalten prüfen. Hintergrundtracking separat entscheiden, nicht stillschweigend aktivieren.
**Abnahme:** Nachweis, welche Werte live aktualisiert werden; keine als „live“ bezeichneten statischen Schätzwerte.
**Stand:** Derzeit überwiegend statische/geschätzte Wegdaten; echte Navigation offen.

## Phase 6 – Stabilisierung und Gerätefreigabe
**6A:** Android-Unit-/Instrumentierungs-/Emulator-Tests, MVV-Live-Probes und visuelle Vergleiche.
**6B:** Echter Test mit Kalendertermin, Standort, Verbindung, Fahrt; Ablehnung von Berechtigungen, offline, veraltete Daten, kleine Displays.
**6C:** Dokumentation, nachvollziehbare Versionsnummer/Artefakt, Installations-/Signaturhinweise; erst dann APK empfehlen.
**Abnahme:** Keine Blocker, alle CI-Jobs grün, Screenshots geprüft, konkrete Testanleitung für Nutzer.
**Stand:** Offen für die nächste UX-Version.

## Fortschrittsprotokoll (aktualisiert 15.11.2026)
**Ausführlicher Stand mit drei Wireframes:** [PROJECT-STATUS-2026-11-15.md](PROJECT-STATUS-2026-11-15.md). Die historischen Stand-Sätze oben dokumentieren den Planungszeitpunkt; die Tabelle hier ist der aktuelle Status.

| Phase | Status | Nachweis | Nächster Schritt |
|---|---|---|---|
| 0 Baseline | Technische CI vorhanden | 5/5 grün für `7afdfab3`, Lauf `43739947286` | Echte Screenshots mit Zielbild vergleichen |
| 1 Automatisierung | Weit fortgeschritten, Geräte-Abnahme offen | Kalender `6b4471e7`; Ziel `b0ef8030`; GPS `f78a0266`; Vordergrund-Updates `7afdfab3` | Berechtigungen, Fehlerfälle und Primäraktion prüfen |
| 2 Einstellungen | In Arbeit | Alle-Tabs-Settings `2a27794b`; Lauf `43744222066` bei Erfassung noch laufend | CI, Persistenz und Präferenzen |
| 3 Verbindungen | Offen | Basissuche vorhanden | Kompakte Karten und Leerzustände |
| 4 Fahrt-Cockpit | Offen | Basis-Dashboard vorhanden | Kacheln und Zeitstatus |
| 5 Live/Navigation | Teilgrundlage vorhanden | Foreground-Standortupdates `7afdfab3` | MVV-Refresh, echte Fußweg-Geometrie |
| 6 Freigabe | Offen | CI-Infrastruktur vorhanden | Visuelle Prüfung und echter Gerätetest |

## Kommunikationsstandard
- **WEITERMACHEN:** Paket abgeschlossen, Tests grün und nächstes Paket sinnvoll.
- **WARTEN:** Workflow läuft; Commit und URL nennen.
- **FEHLER BEHEBEN:** Fehler und konkrete Korrektur nennen; keine Freigabe.
- **TESTVERSION BEREIT:** CI grün, Screenshots geprüft, APK-Artefakt verifiziert; gezielte manuelle Testfälle nennen.
- Bei jeder Rückmeldung: Commit, CI-Status, tatsächlich umgesetzter Umfang und verbleibende Arbeit.

## Aktueller Nachtrag – 16.11.2026, 17:22 MEZ

> **Aktueller Stand statt historischer Momentaufnahme:** Die Phasenbeschreibungen und die Tabelle mit Stand 15.11.2026 oben bleiben als ursprüngliche Planung erhalten. Für den aktuellen Implementierungsstand sind [PROJECT_STATUS.md](PROJECT_STATUS.md), die neuesten Commits und die tatsächlichen GitHub-Actions-Läufe maßgeblich. Ein Commit belegt keine bestandene Prüfung.

| Bereich | Im Code / Repository nachgewiesen | Noch nicht nachgewiesen / offen |
|---|---|---|
| Verbindung und Navigation (Phasen 3/5) | Fußweg-Alternativen in Verbindungskarten (`205c5475`); koordinatenorientierte Fußnavigation und Unsicherheitskennzeichnung laut aktuellem Projektstatus | Echte MVV-Einstiegskoordinate/Bussteig, End-to-End-Navigation auf Gerät, eindeutige Zielerkennung (Issue #4) |
| GPS und Routenneuberechnung (Phase 5) | GPS-Filterung, Off-Route-Evidenz und Rerouting (`61b13706`); angenommener Track getrennt von Reroute (`9416b360`); Trackdarstellung (`685f7cee`) | Deterministische Regressionstests für GPS-Lücke, groben Netzwerk-Fix, Reroute und Track-Erhalt; Praxistest |
| CI und Freigabe (Phase 6) | CI-Diagnose-/Preflight-Fixes (`a8c9a656`, `af8de847`); Concurrency-Isolation (`6b2b55f`); statischer Concurrency-Regressionstest (`e514a12`), im Preflight eingebunden (`ebfc442`) | Erfolgreicher Workflow einschließlich Ausführung des neuen Guards auf aktuellem `main` nicht bestätigt; überlappende Actions-Läufe, Lint, Emulator, MVV-Live-Probe und APK-Artefakt separat nachweisen (Issue #8) |
| Dokumentationsqualität | Zentraler Status und Audit vorhanden | Veraltete Momentaufnahmen weiter eindeutig kennzeichnen und Phasen mit überprüfbaren Nachweisen aktualisieren (Issue #10) |

**Nächste priorisierte Schritte (Abgleich 16.11.2026, 23:19 MEZ):** (1) Tatsächlichen Actions-Push-Lauf auf aktuellem `main` einschließlich Preflight-Guard, Commit-SHA und Run-ID prüfen; die commitbezogene Workflow-Abfrage liefert nur PR-bezogene Läufe und kann einen Push-Erfolg nicht bestätigen. (2) Concurrency-Isolation mit überlappenden Release-/Push-Läufen praktisch validieren. (3) Deterministische GPS-Lücken-/Reroute-/Track-Erhalt-Regressionstests ergänzen. (4) Reale MVV-Einstiegskoordinaten und Bussteigzuordnung prüfen, anschließend S25-Geräteabnahme. **Keine APK-Freigabe ohne vollständige Nachweise.**
