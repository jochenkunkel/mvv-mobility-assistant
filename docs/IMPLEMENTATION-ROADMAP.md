# Mein Weg 2.0 – Entwicklungs- und Freigabeplan

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

## Fortschrittsprotokoll (bei jeder Phase aktualisieren)
| Phase | Status | Letzter Nachweis | Nächster Schritt |
|---|---|---|---|
| 0 Baseline | Prüfen | CI/Screenshot-Workflow existiert | HEAD-CI kontrollieren |
| 1 Automatisierung | In Arbeit | Commit `6b4471e7` (Teil 1A) | 1B Zieladresse |
| 2 Einstellungen | Offen | Teilweise Preferences vorhanden | Nach Phase 1 |
| 3 Verbindungen | Offen | Basissuche vorhanden | Nach Phase 2 |
| 4 Fahrt-Cockpit | Offen | Basis-Dashboard vorhanden | Nach Phase 3 |
| 5 Live/Navigation | Offen | Schätzwerte vorhanden | Nach Phase 4 |
| 6 Freigabe | Offen | CI-Infrastruktur vorhanden | Nach Phase 5 bzw. Zwischenfreigaben |

## Kommunikationsstandard
- **WEITERMACHEN:** Paket abgeschlossen, Tests grün und nächstes Paket sinnvoll.
- **WARTEN:** Workflow läuft; Commit und URL nennen.
- **FEHLER BEHEBEN:** Fehler und konkrete Korrektur nennen; keine Freigabe.
- **TESTVERSION BEREIT:** CI grün, Screenshots geprüft, APK-Artefakt verifiziert; gezielte manuelle Testfälle nennen.
- Bei jeder Rückmeldung: Commit, CI-Status, tatsächlich umgesetzter Umfang und verbleibende Arbeit.
