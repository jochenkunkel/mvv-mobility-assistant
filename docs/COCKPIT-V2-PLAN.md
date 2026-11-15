# Mein Weg 2.0 – verbindlicher Cockpit- und Automatisierungsplan

**Abstimmung:** 15. November 2026, anhand von fünf echten Android-Screenshots.
**Status:** Produktanforderungen beschlossen; **nicht** gleichbedeutend mit fertig implementiert.
**Ziel:** Ein modernes, kompaktes Mobilitäts-Cockpit statt langer Fließtexte und unnötiger Klickketten. Standardfall: Termin auswählen → vorbereitete Route prüfen → Verbindung starten, möglichst ohne manuelle Zwischenschritte.

> **Fortschrittsnachtrag (15.11.2026):** Die unten stehende historische Umsetzungsreihenfolge ist nicht mehr der aktuelle Ist-Stand. Kalenderautomatik `6b4471e7`, Zielvorbelegung/-Lookup `b0ef8030`, GPS-Start `f78a0266` und Vordergrund-Standortupdates `7afdfab3` sind implementiert; CI für `7afdfab3` ist mit fünf erfolgreichen Jobs abgeschlossen. Settings aus allen Tabs `2a27794b` sind implementiert, CI beim Erfassen noch offen. **Keine vollständige UX-/Geräteabnahme.** Aktueller Status, Risiken und drei Wireframes: [Projektstatus](PROJECT-STATUS-2026-11-15.md).

## Verbindliche UX-Prinzipien
1. **Automatisch vorbereiten, nur bei Bedarf eingreifen.** Kalenderdaten, Ziel, letzte Präferenzen und (mit Berechtigung) Startposition vorbefüllen.
2. **Drei Module auf Start:** 1 Termin, 2 Start & Ziel, 3 Fahrtwunsch. Nur das aktive Modul groß; andere als schmale, antippbare Zusammenfassung. Keine zusätzliche doppelte Cockpit-Übersicht.
3. **Die ganze Karte ist die Aktion.** Kein unnötiges „Ändern“, „Aktualisieren“, „Prüfen“ oder „Weiter“, wenn dieselbe Aktion durch direkte Auswahl oder automatische Verarbeitung möglich ist.
4. **Informationshierarchie statt Fließtext:** Kleine, klare Kacheln mit Icon, Hauptwert und Einheit; sekundäre Erläuterungen dezent, lange technische Hinweise in Details/Hilfe.
5. **Korrektheit vor Optik:** Live, geplante und geschätzte Werte explizit unterscheiden. Fehlerzustände und Berechtigungsablehnung dürfen die App nicht blockieren.
6. **Datenschutz:** Standort nur nach Android-Berechtigung; keine heimliche Hintergrundverfolgung. Persistente Standortdaten nur nach gesonderter Produktentscheidung.

## P0 – Start ohne unnötige Klicks

### Kalender und Termin
- Beim ersten App-Start direkt Android-Kalenderberechtigung anfragen, sofern noch nicht erteilt; bei Ablehnung kompakte Wiederholen-/Einstellungen-Aktion und manuelle Alternative anbieten. Nicht bei jedem Start erneut aufdringlich fragen.
- Nächsten geeigneten Termin mit Datum, Uhrzeit und Ziel kompakt anzeigen.
- **Ganze Terminkarte antippen** → Terminauswahl; kein eigener „Ändern“-Button. Daten beim Öffnen/Zurückkehren angemessen aktualisieren, ohne dauerhaft sichtbaren Refresh-Button.
- Bei Terminwechsel Ziel, Validierung und Verbindungen konsistent neu berechnen; manuell überschriebenes Ziel nicht unbemerkt verwerfen.

### Zieladresse
- Kalender-Ziel **sichtbar vorausfüllen**, nicht nur als Placeholder/unterhalb eines leeren Eingabefeldes anzeigen.
- Wenn vorhanden, **automatisch und asynchron** online prüfen/auflösen; Ergebnisse direkt als Status („bestätigt“, „mehrdeutig“, „nicht gefunden“) darstellen.
- Bei eindeutiger Übereinstimmung automatisch übernehmen; bei mehreren Kandidaten kurze Auswahl anbieten; bei Fehler oder Offline-Betrieb editierbaren Originalwert behalten.
- Doppelte Adressbestandteile (Screenshot: „Wettersteinstraße 11 Wettersteinstraße 11“) vermeiden; Ort/PLZ nicht blind abschneiden. Keine Endlosschleifen durch automatische Lookup-Updates.
- Manuelle Prüfung nur als kleine Wiederholen-Aktion bei Fehlern oder auf ausdrücklichen Wunsch.

### Startposition
- Mit erteilter Standortberechtigung beim Einstieg in den Ablauf automatisch einmal aktuelle Position ermitteln; bei fehlender Berechtigung angemessen Android-Prompt auslösen.
- Kein Pflicht-Button „Aktuellen Standort verwenden“, wenn dies ohnehin die einzige Startoption ist.
- Standort-Status kompakt („Start ermittelt“, „wird ermittelt“, „nicht verfügbar“); **„Aktualisieren“ nur als Recovery/Refresh**, insbesondere bei veralteter Position.
- Bei Timeout, deaktiviertem GPS, Ablehnung oder fehlender Genauigkeit verständliche Fehlerbehandlung; gegebenenfalls manuelle Startadresse anbieten.
- Letzten bekannten Standort höchstens als klar gekennzeichneten **möglicherweise veralteten** Fallback verwenden; nicht stillschweigend als „aktuell“ ausgeben.

### Abschluss
- Nach gültigem Termin/Ziel/Start/Fahrtwunsch eine einzige klare Primäraktion „Verbindung finden“; keine zusätzlichen Zwischenseiten oder doppelte Suche.
- Während Suche Ladezustand, bei Fehler Wiederholen und konkrete Ursache anzeigen.

## P1 – Einstellungen / Zahnrad
- Zahnrad muss **sichtbare, echte, sinnvoll gruppierte Einstellungen** öffnen, nicht bloß ein scheinbar leeres Panel.
- Mindestens: Zuhause-Adresse, Gehgeschwindigkeit, Ankunftspuffer, bevorzugte Linien/Verkehrsmittel, Standort-/Kalenderstatus, manuelle Aktualisierung und Hinweise zum Datenschutz.
- Vorhandene lokale Preferences wiederverwenden; nur wirksame Funktionen als aktiv darstellen. Sprachwahl derzeit lediglich vorbereitet und **nicht** als funktionierende Übersetzung bewerben.
- Settings nicht als lange Untersektion der Startseite; eigener klarer Bereich/Dialog/Screen, Rückweg eindeutig.
- Vorlieben dauerhaft speichern und beim Neustart laden. Route nach relevanter Änderung neu bewerten.

## P1 – Verbindungen als echte Karten
- Pro Verbindung eine kompakte Karte mit **Abfahrt → Ankunft**, Linien-Badges, Umstiegen, Gehzeit und Abfahrts-/Losgehzeit.
- Erste/beste Verbindung optisch hervorheben; mehrere Alternativen gut vergleichbar. Kleine Abstände und präzise Schrift-Hierarchie statt langer Absätze.
- „Keine Verbindung“ nicht mit scheinbar widersprüchlichen Ergebnissen stehen lassen; Suchzustand, Datum, Filter, Fehler und Ergebnisanzahl sauber synchronisieren.
- „ÖPNV-Route anzeigen“ als sekundäre Aktion; externe Maps-Routen klar als solche kennzeichnen. Gehgeschwindigkeit externer Karten-Apps nicht als angewandt ausgeben, wenn sie nicht übertragen wird.
- Längere Wegbeschreibung und Haltestellen in aufklappbaren Details.

## P1/P2 – Meine Fahrt: visuelles, ehrliches Live-Cockpit
- Kachel-Raster mit gut lesbaren Hauptwerten, z. B. **Fußweg: 550 m / ca. 11 Min**, **Abfahrt: 07:05 / Bus 134**, **Losgehen: 06:54**, **Ankunft: 07:40**. Werte sind illustrative Screenshot-Daten, **keine aktuellen Live-Werte**.
- Dazu nächster Halt, nächster Schritt, Verkehrsmittel, Status und eine dominante Navigationsaktion. Keine großen Fließtextblöcke.
- **Zeit-Countdown** regelmäßig anhand aktueller Uhrzeit neu berechnen, wenn Dashboard sichtbar. Nicht bloß beim Öffnen einfrieren.
- **Positionsabhängige Meter, verbleibende Gehzeit und nächste Abbiegung** erst als „live“ ausweisen, wenn aktive Standortupdates **und** eine echte Fußweg-Geometrie verfügbar sind. Andernfalls ausdrücklich „geschätzt“/„Navigation extern“.
- Live-Verkehrsprognosen nur mit aktueller Datenquelle, Abfragezeit und robustem Offline-/Fehlerstatus kennzeichnen; Planzeiten von Prognosen unterscheiden.
- Hintergrund-Ortung, Benachrichtigungen und dauerhafte Navigation sind **separate** Aufgaben mit Android-Berechtigungs-, Akku- und Datenschutzkonzept.
- Kein irreführendes „Jetzt zur Haltestelle“, wenn Abfahrt erst in vielen Stunden liegt; klare Zustände „später“, „bald losgehen“, „jetzt losgehen“, „unterwegs“.

## Umsetzung und Priorisierung
- **Bereits in Git (Commit 6b4471e7):** automatische Kalenderberechtigungsanfrage beim Start; ganze Terminkarte antippbar; sichtbarer Refresh-/Ändern-Button im Terminmodul entfernt. Diese Änderungen müssen durch CI und echte Geräteprüfung.
- **Bereits zuvor:** drei progressive Setup-Module, einfache Zielübernahme als Placeholder/Fallback, manuell ausgelöste Adresssuche, manuell ausgelöste GPS-Abfrage, Verbindungssuche, erstes Fahrt-Dashboard, Settings-Panel und einige persistente Präferenzen. **Diese sind nicht gleichbedeutend mit den oben verlangten Automatisierungen oder dem neuen Kartendesign.**
- **Als Nächstes, in Reihenfolge:**
  1. Automatische Zielvorbelegung, Online-Validierung, Status- und Fehlerbehandlung.
  2. Automatischer Standortabruf mit Berechtigungs-/Timeout-/Fallback-Verhalten.
  3. Settings als echte Konfigurationszentrale.
  4. Verbindungskarten mit kompakten Zahlen, Badges und nachvollziehbaren Suchzuständen.
  5. Fahrt-Cockpit mit Kacheln und korrekten Zeit-/Live-Zuständen.
  6. Optional echte aktive Fußweg-Navigation und regelmäßige Live-Daten-Aktualisierung.

## Abnahmekriterien
- [ ] Kalenderberechtigung beim ersten Start ohne vorgeschalteten App-Button; Ablehnung bleibt bedienbar.
- [ ] Terminwechsel durch Tippen auf die gesamte Karte.
- [ ] Kalenderziel sofort sichtbar vorausgefüllt und automatisch validiert; Mehrdeutigkeit/Offline-Fall sauber.
- [ ] Standort bei Berechtigung automatisch ermittelt; kein Pflichtklick; veralteter Fallback klar gekennzeichnet.
- [ ] Start → Verbindung in möglichst einer primären Aktion, sofern Daten vorhanden.
- [ ] Zahnrad zeigt tatsächlich benutzbare, gespeicherte Einstellungen.
- [ ] Verbindungen sind kompakte, unterscheidbare Karten; keine widersprüchlichen Leerzustände.
- [ ] Meine Fahrt zeigt wichtige Werte in separaten Kacheln mit Einheiten, Icons und Hierarchie.
- [ ] Zeit-Countdown wird aktualisiert; „live“ nur für wirklich aktualisierte Daten.
- [ ] Unit-, Instrumentierungs- und Emulator-Tests grün.
- [ ] Echte Emulator-Screenshots von **Start, Start & Ziel, Verbindungen, Meine Fahrt** visuell geprüft.
- [ ] Erst nach CI **und** Sichtprüfung neue Test-APK empfehlen.

## Screenshots / Befunde vom 15.11.2026
1. Start: Kalender-Termin nimmt unnötig Platz ein; separate Aktionen; Karte statt Textzeile.
2. Start & Ziel: leeres Zielfeld trotz vorhandener Kalenderadresse; optionale manuelle Online-Prüfung; Pflicht-GPS-Button; zu viel Erklärungstext.
3. Verbindungen ohne Treffer: lange Fließtexte und externe Maps-Ausweichaktion.
4. Verbindung gefunden: große Textkarte mit Abfahrt 06:54–07:40, Linie 134, Geh-/Umsteigeinfos; Informationshierarchie fehlt.
5. Meine Fahrt: große Textfläche, „Jetzt zur Haltestelle“ trotz ~13 Stunden Wartezeit, Meter nur geschätzt; kein echter Live-Standort.

## Arbeitsregel
Bei jedem Commit im PR/Commit-Text klar unterscheiden: **implementiert**, **getestet**, **visuell geprüft**. Keine Funktion als „fertig“ melden, nur weil der Build grün ist. Dieses Dokument als verbindlichen UX-Backlog pflegen und erledigte Punkte erst nach Nachweis abhaken.
