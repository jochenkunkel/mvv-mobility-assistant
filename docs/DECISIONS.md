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

## 2026-11-15 – Cockpit statt langer Testoberfläche (Migration)

**Problem:** Die ursprüngliche `MainActivity` enthält Darstellung, Berechtigungen, Standort, Kalender und Routensuche in einer sehr langen scrollbaren Compose-Spalte. Eine reine kosmetische Anpassung reproduziert nicht die gewünschte kompakte Drei-Bereiche-Bedienung.

**Entscheidung:** Schrittweise Migration zu einer komponentenbasierten Compose-Cockpit-Architektur, ohne den funktionierenden MVV-Datenpfad sofort neu zu schreiben. `ui/components/CockpitOverview.kt` ist der erste eigenständige Präsentationsbaustein mit drei interaktiven Einstiegspunkten. Er bekommt nur Daten und Callbacks, keine Repository-Abhängigkeiten. Die Einstiegspunkte navigieren zunächst per `BringIntoViewRequester` zu bestehenden Abschnitten.

**Nächste Schritte:** Separates `Scaffold` mit NavigationBar und echten Start-/Verbindungen-/Meine-Fahrt-Screens; Zustandsverwaltung in einem ViewModel/UiState, Extraktion von Berechtigungs- und Netzwerk-Callbacks, Settings als eigener Screen, Screenshot-Tests je Screen. Die aktuelle Cockpit-Komponente ist ein Übergang, nicht die fertige Navigation. Jede Stufe muss den Emulator-Test bestehen; erst dann erfolgt eine APK-Freigabe.


### ADR-006 – Verkehrsprovider und Region zentral konfigurieren (16.11.2026)
- **Beschlossen:** Zunächst ausschließlich MVV in München produktiv entwickeln und testen. Region/Provider als zentrale Konfiguration abbilden; UI, Reise-Cockpit, Fußwegführung und später die Etappen-State-Machine möglichst providerunabhängig halten.
- **Erste Umsetzung:** `MobilityProviderConfig.kt` enthält `MobilityProvider.MVV`, Stadt München, Land DE, Zeitzone Europe/Berlin und Geocoding-Kontext München/Deutschland. `WalkingNavigation.fetch` verwendet den konfigurierten Geocoding-Kontext statt eines hartcodierten München-Strings. Commits `d016bd46`, `3d8eb19e`.
- **Architektur-Skizze:**
```text
                     MeinWeg2 UI / Cockpit / Etappenführung
                                      |
                          Reiseplanung (neutrales Modell)
                             /                  \
                TransitProvider               Fußweg/OSRM
                      |                          |
                MVV-Adapter                  RegionConfig
                      |
              MVV API (München)

     Zukunft (NICHT implementiert):
              BVG/VBB-Adapter (Berlin) -- neue API + Mapping + Tests
```
- **Geplante Schnittstelle:** `TransitProvider` mit Suche nach Verbindungen, Auflösung von Haltestellen, Umstiegs-/Etappenmodell und klarer Kennzeichnung geplanter bzw. echter Live-Zeiten. Adapter-spezifische APIs und IDs bleiben innerhalb der Adapter.
- **Wichtige Grenze:** Ein Wechsel der Stadt in der Konfiguration aktiviert **keine** Berlin-Unterstützung. Dafür braucht es einen echten BVG/VBB-Adapter, Geocoding-/Routingtests, regionale Tarif-/Zeitregeln und eigene Geräteabnahme. Der aktuelle MVV-Repository-Code ist noch nicht in ein Interface extrahiert.
- **Reihenfolge:** Erst München stabilisieren (Busausstieg, Anschluss-Fußweg, reine Fußwege, CI, Geräteabnahme); danach Provider-Interface und MVV-Adapter schrittweise extrahieren; erst dann Berlin.
- **Teststatus:** Diese Vorbereitungen sind committed, aber nicht als grüner Android-Build oder vollständig getestete Provider-Abstraktion bestätigt.

### ADR-007 – Debug-Funktionen über zentrale Build-Konfiguration
- **Status:** Vorbereitet, CI/Android-Build noch zu prüfen (16.11.2026).
- **Entscheidung:** Debug-Export bleibt im Source Code und wird über die BuildConfig-Konstante `ENABLE_MVV_DEBUG_EXPORT` gesteuert; in `debug` true, in `release` false. Zusätzlich muss `BuildConfig.DEBUG` gelten.
- **Umgesetzt:** `app/build.gradle.kts` aktiviert `buildConfig` und definiert beide Build-Typen; `MainActivity.kt` zeigt den Export nur bei aktivem Flag; `DebugResponseCapture.kt` prüft denselben Schutz bei Aufnahme und Abruf der Rohantwort. Commits `5f7d1117`, `a586f1eb`, `13ac8ed6`.
- **Sicherheit:** Rohantwort enthält möglicherweise Standort- und Zielinformationen; keine automatische Übertragung und kein Release-Export. Nutzer exportiert über Android-Dateiauswahl. **Noch offen:** Build/Unit-Test und UI-Test für beide Varianten, Prüfung von gespeicherten Rohdaten und Exportformat.

### ADR-008 – CI-Konkurrenz nach Ereignis und Ref (16.11.2026; korrigiert 18.11.2026)
- **Aktuelle Entscheidung:** Die ursprüngliche globale Gruppe `single-active-ci` mit bedingungslosem Abbruch ist **historisch und überholt**. Sie darf nicht wiederhergestellt werden, weil sie laufende manuelle Prüfungen und signierte Releases unterbrechen könnte. Siehe [CI-CONCURRENCY-CURRENT.md](CI-CONCURRENCY-CURRENT.md).
- **Implementierter Stand in `.github/workflows/android.yml`:**
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'push' && !startsWith(github.ref, 'refs/tags/v') }}
```
- **Wirkung:** Nur ältere gewöhnliche Branch-Push-Läufe derselben Workflow-/Ereignis-/Ref-Gruppe können ersetzt werden. PR-, Tag- und manuelle Läufe werden durch diese Regel nicht aktiv abgebrochen. Es ist **keine** globale Serialisierung aller Jobs oder Workflows.
- **Geltungsgrenze:** Die Aussage ist anhand des Android-Workflow-Quelltexts geprüft. Für `release.yml` sind eigene Concurrency- und Freigaberegeln gesondert zu verifizieren; keine workflowübergreifende Release-Garantie aus der Android-Regel ableiten.
- **Abnahme offen:** `python3 scripts/test-ci-concurrency.py` sowie Preflight und Android-CI auf identischem HEAD ausführen, echte Push-Läufe samt Run-ID prüfen und kontrollierte Abbruch-/Nicht-Abbruch-Szenarien testen. Quelltextprüfung ist kein erfolgreicher CI-Nachweis.

### ADR-009 – Regionale Android-App-Icons (16.11.2026)
- Ein einziger Build-Parameter `-PmeinWegRegion=munich|berlin|frankfurt` steuert **sowohl** das adaptive Launcher-Icon als auch `BuildConfig.MOBILITY_REGION` und damit `MobilityProviderConfig.region`.
- Standard ist `munich`. Die drei Vektor-Iconvarianten verwenden das violette Design, das stilisierte Röntgen-Schulter-/Platten-/Schraubenmotiv und eine regionstypische Skyline (München/Frauenkirche, Berlin/Brandenburger Tor, Frankfurt/Skyline). Die Vektorgrafiken sind eine vereinfachte Android-Umsetzung des freigegebenen Bildkonzepts; nicht die fotorealistische PNG-Originalgrafik.
- Das Launcher-Icon wird **zur Build-Zeit** ausgewählt. Eine Änderung der Region erfordert eine neu gebaute und installierte APK; Android erlaubt keinen zuverlässigen sofortigen Wechsel des App-Icons allein durch einen Laufzeit-Konfigurationswert.
- **Funktionsgrenze:** Nur München/MVV ist implementiert. Berlin und Frankfurt sind vorbereitete Branding-/Regionsprofile ohne eigene ÖPNV-Provider-Adapter. Ein anderer Regions-Build ist **keine** funktionsfähige Berlin-/Frankfurt-Fahrplanauskunft; vor produktiver Verwendung muss ein passender Adapter implementiert werden.
- Abnahme offen: Android-CI, Launcher-Icon auf Gerät und maskierte adaptive Icons, drei Gradle-Varianten, Provider-Abgrenzung.

### ADR-010 – Flüchtige GPS-Spur und Navigationslebenszyklus (23.11.2026)
- **Status:** Anforderungen präzisiert; Implementierung und Tests nicht bestätigt. Referenzen: [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13), [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14).
- **Entscheidung:** Die tatsächlich gegangene Strecke ist eine eigenständige, flüchtige GPS-Spur der aktiven Navigation und darf nicht aus dem projizierten Fortschritt auf der Sollroute rekonstruiert werden. Die ältere Datenschutzfestlegung „keine Speicherung des GPS-Verlaufs“ meint keine **dauerhafte** Speicherung und kein stilles Hintergrundtracking; die vorübergehende Haltung der Spur für die aktive Anzeige ist davon zu unterscheiden.
- **Zustand:** Karten-Neuzeichnung, GPS-Ausfall, Re-Routing und Rotation dürfen akzeptierte Punkte nicht löschen. Bei ungültigen Fixes wird nur das Anhängen pausiert. Bei großen Fix-Lücken keine künstliche Verbindungslinie zeichnen. Prozess-Tod und explizites Beenden der Navigation brauchen gesonderte, dokumentierte Lösch-/Wiederherstellungsregeln; keine unbelegte Persistenzgarantie.
- **Regressionen:** Gute/schlechte Fixes, Aussetzer, Positionssprung, Re-Routing, Compose-Neuzeichnung und Portrait–Landscape–Portrait mit aktiver Route testen. Sollroute, projizierter Fortschritt und echte Spur müssen erkennbar verschieden sein.
- **Freigabe:** Diese ADR ist kein Codefix oder Testnachweis. CI-Preflight-Blockade zuerst mit reproduzierbaren Exitcodes klären; danach #13/#14 implementieren und auf Gerät abnehmen.
