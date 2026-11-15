# Architektur- und Produktentscheidungen – Mein Weg 2.0

Stand: 2026-11-15. Dieses Dokument hält **beschlossene Ziele**, **bereits implementierte Schritte** und **offene Arbeiten** auseinander. Es ist kein Nachweis, dass alle geplanten Funktionen fertig oder getestet sind.

## Leitbild und Referenzdesign

Die bestehende, vom Auftraggeber bereitgestellte Drei-Screen-Vorlage bleibt der visuelle Maßstab: **Start**, **Verbindungen**, **Meine Fahrt**. Violett-weiße Gestaltung, klare Karten, große primäre Aktion und eine gut erkennbare Abfahrts-/Gehinformation. Ein Einstellungszugang über ein Zahnrad soll die Startansicht entlasten. Vor jeder APK-Freigabe echte Emulator-Screenshots gegen die Referenz prüfen. Screenshot-Datei- und Änderungsprüfungen allein sind **kein** Pixelvergleich.

### Warum?
Die App soll trotz Kalender, MVV, GPS und Routenfiltern auf einem Smartphone schnell und ohne Überforderung nutzbar bleiben. Die Nutzbarkeit im Alltag hat Vorrang vor dekorativen Elementen.

## Entscheidungsprotokoll (ADR)

### ADR-001 – Einstellungen über Zahnrad statt überfüllter Startseite
- **Status:** Erste Umsetzung am 15.11.2026, weitere UX-Arbeit offen.
- **Entscheidung:** Ein aufklappbarer Einstellungsbereich wird über eine gut sichtbare Aktion geöffnet. Langfristig eigene Settings-Komponente bzw. eigener Screen.
- **Grund:** Seltene Konfigurationen von der täglichen Verbindungssuche trennen.
- **Umgesetzt:** `settings_toggle`, `settings_panel` in `MainActivity.kt`, Commit `8c3c3961`.
- **Offen:** echtes Zahnrad-Icon statt Textsymbol; dauerhafte Navigation/Screenstruktur; Compose-UI-Tests und Barrierefreiheit.

### ADR-002 – Zuhause als lokal gespeichertes Ziel
- **Status:** Minimalfunktion umgesetzt; Kartennadel und Suche offen.
- **Entscheidung:** Adresse/Ort in Einstellungen hinterlegen, explizit speichern/löschen, Schnellaktion „Nach Hause“ auf Start.
- **Grund:** Häufiges Ziel ohne erneute Adresseingabe erreichbar machen.
- **Umgesetzt:** Lokale Speicherung in Android Preferences und Google-Maps-Transit-Deep-Link, Commit `8c3c3961`.
- **Wichtige Grenze:** Das ist **keine** MVV-integrierte Zuhause-Routenplanung und kein Karten-Pin-Picker. Der Link übergibt die Zieladresse an Google Maps. Kein Standort wird hierfür automatisch im Hintergrund abgerufen.
- **Offen:** DestinationLookup-Autocomplete wiederverwenden, optionalen Pin auf Karte auswählen, MVV-Suche mit Heimadresse, Eingabevalidierung, Datenschutz- und Löschtests.

### ADR-003 – Ankunftspuffer und Gehgeschwindigkeit persistent
- **Status:** Implementiert, neuer CI-Lauf noch zu bestätigen.
- **Entscheidung:** Ankunftspuffer (5–60 Minuten) und Gehgeschwindigkeit (2–5 km/h) lokal speichern und bei Start wiederherstellen.
- **Grund:** Persönliche Anforderungen sollen nicht vor jedem Termin neu konfiguriert werden.
- **Umgesetzt:** Preferences-Keys `arrival_buffer_minutes` und `walking_speed_kmh`; Änderungen am Puffer auch aus der Startansicht speichern.
- **Offen:** einheitliche Einstellungsquelle statt doppelter Slider, Tests für Neustart, Grenzwerte und Routen-Neuberechnung.

### ADR-004 – Internationalisierung als Architekturziel
- **Status:** Nur Speicherung der Sprachpräferenz, **noch keine wirksame Sprachumschaltung**.
- **Entscheidung:** System, Deutsch, Englisch, Französisch, Italienisch als anfängliche Optionen; später weitere Sprachen ergänzbar.
- **Grund:** Erweiterbarkeit und barriereärmere Nutzung für unterschiedliche Nutzer.
- **Umgesetzt:** `language_choice` lokal gespeichert, Commit `8c3c3961`.
- **Offen und zwingend:** Sichtbare UI-Texte aus Kotlin in Android `strings.xml` überführen, Sprachressourcen `values-de`, `values-en` usw. anlegen, App-Locale korrekt anwenden, Pluralformen und Datums-/Zeitformate lokalisieren. **Bis dahin ist die Auswahl nur eine vorbereitete Einstellung und darf nicht als Übersetzungsfunktion beworben werden.** Nicht übersetzte Sprachen nicht als fertig kennzeichnen.

### ADR-005 – Weitere sinnvolle Einstellungen
- **Status:** Ideen, nicht implementiert.
- **Kandidaten:** bevorzugte Verkehrsmittel/Linien, barrierearme Wege soweit MVV-Daten dies zuverlässig unterstützen, größere Schrift und Kontrast, Erinnerungen vor Gehbeginn mit expliziter Benachrichtigungserlaubnis, datensparsame Standortoptionen, Standard-Startpunkt, Standard-Karten-App.
- **Grund:** Alltagstauglichkeit und Zugänglichkeit.
- **Priorisierung:** Erst Kernnavigation und UI-Fertigstellung, dann Funktionserweiterungen. Keine „barrierefrei“-Garantie ohne verlässliche Daten.

### ADR-006 – Architektur und Qualitätsgrenzen
- **Status:** Laufende Arbeit.
- **Entscheidung:** Compose-Komponenten wiederverwenden; `MainActivity.kt` schrittweise in Start-, Verbindungs-, Fahrt- und Einstellungs-UI sowie Zustands-/Repository-Schichten zerlegen.
- **Grund:** Große Einzeldatei erschwert Wartung und Tests.
- **Bereits vorhanden:** `ArrivalBufferControl` und `RouteFilterControl`; CI mit Build, Unit-/Lint- und Emulatorprüfungen.
- **Offen:** klare Navigation und Zustandsmodell, echte visuelle Regression gegen freigegebene Referenzbilder, Tests für Zuhause und Einstellungen.
- **Qualitätsregel:** „CI grün“ bedeutet Build-/Test-Erfolg, **nicht** Designparität oder Freigabe der APK.

## Reihenfolge der nächsten Arbeiten
1. CI-Ergebnis für Commit `8c3c3961` prüfen; Fehler sofort beheben.
2. Einstellungen und Heimadresse auf eigene Compose-Komponenten verteilen, Tests für Persistenz und Home-Aktion ergänzen.
3. Startscreen nach Vorlage vereinheitlichen: Header, Termin, Puffer, Filter, Hauptaktion und Zahnrad.
4. Verbindungen und Meine Fahrt als klar getrennte Ansichten gestalten.
5. Internationalisierung wirklich implementieren, nicht nur Sprachauswahl speichern.
6. Heimadresse über Suche und optional Kartenpunkt vervollständigen.
7. Emulator-Screenshots mit Zielbild vergleichen; erst nach bestandenen Tests und manueller Sichtprüfung APK empfehlen.

## Nachvollziehbarkeit
- `6577b25a`: erster violetter Header und aufgewertete Terminkarte.
- `8c3c3961`: Einstellungsbereich, Heimadresse, persistenter Puffer, vorbereitete Sprachpräferenz.
- Dieses Protokoll wird bei Richtungswechseln ergänzt: Entscheidung, Motivation, Alternativen, Konsequenzen, Status und zugehöriger Commit.

## Datenschutz und Grenzen
Heimadresse ist personenbezogen und wird lokal in App-Preferences gespeichert; aktuell nicht verschlüsselt. Bei Navigation wird die Adresse an den externen Kartendienst übergeben. Keine Speicherung des GPS-Verlaufs und keine Standortverfolgung im Hintergrund als Produktziel. Eine spätere Karten-/Autocomplete-Lösung muss die Weitergabe an externe Dienste sichtbar machen.


## 2026-11-15 – UI-Politur und stabiler Dashboard-Test

- Die primäre Suchaktion wurde visuell verstärkt; Verbindungskarten und das Dashboard erhielten eine violette Hierarchie.
- Die erste Verbindung wird derzeit nur **visuell** als Empfehlung markiert. Eine echte Qualitätsbewertung und Rangfolge sind noch umzusetzen; bis dahin darf die Position nicht als fachliche Empfehlung gelten.
- Der Emulator-Test schlug nach der Umbenennung der Dashboard-Überschrift fehl, obwohl die Ansicht existierte. Die Prüfung verwendet jetzt den stabilen Semantik-Tag `walking_dashboard` statt eines übersetzten oder veränderlichen Texts.
- Der korrigierte Commit `2fc06a52` bestand alle fünf GitHub-Jobs einschließlich Emulator. Die Screenshots sind als Artefakt `emulator-test-results` in Lauf `43699900629` abgelegt. Eine visuelle Begutachtung bleibt von den automatischen Tests getrennt.
