# Morgen-Test: Mein Weg 2.0 – Übergabe und nächste Schritte
Stand: 15.11.2026. Ziel: brauchbare Android-Testversion für den Termin am 16.11.2026 morgens.

## Verifizierter technischer Stand
- Letzter grüner CI-Build vor diesem Dokument: GitHub Actions #43765445404, Commit `ceb7fe0d`. Vorprüfungen und `:app:testDebugUnitTest :app:assembleDebug` erfolgreich.
- Die davor fehlgeschlagene CI #43763939918 wurde durch die Ergänzung von `testImplementation("org.json:json:20240303")` für die JVM-Tests korrigiert.
- Release-only-Probes (MVV live, Tür-zu-Tür, Emulator, Lint) sind bei normalen Pushes bewusst übersprungen; **grüne CI ist keine erfolgreiche reale GPS-/MVV-Abnahme**.
- APK: Im erfolgreichen Actions-Lauf unter Artifacts / `mein-weg-debug-apk`. Debug-Signatur kann wechseln; bei notwendiger Neuinstallation können lokale Einstellungen verloren gehen.

## Stand der Funktionen
- Drei kompakte Tabs Start, Verbindungen, Meine Fahrt; Kacheln statt Fließtext.
- Kalendertermin und Ziel, Android-Standortberechtigung beim App-Start vor Kalenderfreigabe, GPS-Aktualisierung bei geöffneter App; keine Hintergrundortung.
- MVV-Routensuche mit Linienfilter, Losgehzeit und geplantem Abfahrtscountdown; **nicht automatisch MVV-Echtzeit**.
- Fußweg-Navigation in Meine Fahrt: öffnet keinen Google-Maps-Intent mehr, sondern fragt den öffentlichen OSRM-Fußgänger-Endpunkt `routing.openstreetmap.de/routed-foot` an, löst die Haltestelle via Android-Geocoder auf und zeigt OSM-Abbiegeanweisungen mit GPS-Abstand an.

## Bekannte Risiken – für morgen besonders wichtig
1. **OSM-Fußweg-Navigation ist experimentell.** Keine End-to-End-Liveprüfung des externen Routingdiensts und keine Geräteabnahme. Bei Dienst-/Geocoder-Ausfall keine Anweisung; die Funktion ist **nicht** verlässliche Navigationsgrundlage für den morgigen Termin.
2. **Abbiegefortschritt:** `WalkingNavigation.nextTurn` verwendet aktuell den räumlich nächsten Manöverpunkt statt ein echtes Map-Matching entlang der Liniengeometrie. An Schleifen, Kreuzungen und Parallelwegen kann die Anweisung falsch sein. Vor produktivem Einsatz Geometrie, Fortschrittsindex, Distanz entlang der Route und Abweichungserkennung ergänzen.
3. **Haltestellenziel:** Geocoder-Suche nach Stationsnamen kann die falsche Position oder den falschen Bahnsteig liefern. Langfristig MVV-Haltestellenkoordinaten direkt übergeben.
4. **Live-Zeiten:** Abfahrtscountdown basiert auf Fahrplan-/Abfragewerten, nicht kontinuierlich aktualisierten MVV-Verspätungen.
5. **Erststart/GPS:** Berechtigungsdialoge und Standortaktualisierung auf Samsung Galaxy S25 nach der jüngsten Korrektur noch nicht persönlich abgenommen.
6. **Termin-/Losgehzeit:** Vor dem Termin die vorgeschlagene Ankunft und Puffer mit einer offiziellen MVV-Quelle abgleichen. Bei Fehler oder fehlender Verbindung nicht allein auf die App verlassen.

## Nächste Schritte in Prioritätsreihenfolge
1. CI für aktuellen HEAD prüfen, Build-Artifact nachweisen, keine unbestätigte APK als fertig bezeichnen.
2. Einen gezielten **Release-Testlauf** (Workflow `Android CI`, manueller `test_level=release`) vor einer echten Freigabe durchführen, wenn GitHub-Laufzeit und Dienste verfügbar sind; Fehlersignale beheben.
3. Auf Galaxy S25: Erststart / GPS-Berechtigung / Kalender / Ziel / Verbindung / Losgehzeit / Meine Fahrt testen; Fotos und Fehlermeldungen festhalten.
4. OSM-Navigation mit echter Weggeometrie, routenbezogenem Fortschritt, verifizierter Haltestellenposition und Offline-/Netzfehlerzuständen absichern.
5. Erst danach echte MVV-Liveupdates und Sensorsignal/Kompass-Ausrichtung einbauen.
6. Dieses Dokument bei jedem weiteren Arbeitspaket mit Commit, CI-Link, Testergebnis und offenen Punkten fortschreiben.

## Arbeitsregel
Keine Hintergrundentwicklung ohne explizit eingerichtete Automation. Änderungen und Entscheidungen im Repository dokumentieren. Für Zwischenstände schnelle Unit-Tests und Build; schwere Tests für Release. Kein Anspruch auf produktive Echtzeitnavigation allein durch einen grünen Build.

## UI-Nachtrag: Verbindungsdetails (Commit `70790706`)
- Anlass: Geräte-Screenshot zeigt übergroße ungegliederte Textblöcke und veralteten Google-Maps-Hinweis in der aufgeklappten Etappenansicht.
- Umsetzung: kompakte weiße Etappenkarten mit Fuß-/Bus-Icon, Abfahrt rechts, Ankunft darunter, Start/Ziel und Fußwegdauer; Navigation führt zur integrierten Meine-Fahrt-Ansicht.
- **Noch offen:** Android-CI für diesen Commit und Sichtprüfung der tatsächlich gerenderten Etappenkarten auf dem Gerät; keine UI-Freigabe allein durch Codeänderung.

## UI-Nachtrag: kompakter GPS-Status (Commit `851f44fd`)
- Anlass: Der Nutzer-Screenshot vom 15.11. zeigt eine übergroße GPS-Statuskarte über den Losgehen-/Bus-Kacheln.
- Umsetzung: Die separate GPS-Karte in „Meine Fahrt“ entfällt. Stattdessen erscheint rechts im Kopf ein kompakter GPS-Indikator: grün bei frischer Position, gelb bei erteilter Berechtigung ohne frische Position, rot ohne Berechtigung.
- Der Indikator bezeichnet nur Standortdaten, **keine MVV-Echtzeitabfahrten**. Die bestehende Freshness-Prüfung bleibt erhalten.
- Prüfung offen: GitHub-Actions-Status des Pushs, Emulator-Screenshot und Sichtprüfung auf dem Galaxy S25. Keine Behauptung eines erfolgreichen Builds vor CI-Nachweis.

## Verbindliche Aufgabenliste für Montag, 16.11.2026 (aktualisiert am Vorabend)

Diese Liste bündelt die noch offenen Punkte aus `IMPLEMENTATION-ROADMAP.md`, `CODE-AUDIT-2026-11-15.md`, `COCKPIT-V2-PLAN.md` und den letzten UI-Änderungen. Die älteren Statusabschnitte bleiben als Historie erhalten. **Kein Punkt gilt allein wegen eines Commits als getestet.**

### P0 – Vor dem ersten Termin: belastbare Testversion
- [ ] **Aktuellen HEAD und Actions prüfen.** Commits `851f44fd` (GPS-Kompaktanzeige) und `a9842d8d` (Dokumentation) samt nachfolgenden Änderungen auf Build-/Teststatus prüfen. Der letzte hier dokumentierte grüne Build `43765445404` ist älter. Prüfen, ob ein Push-Build lief; falls nicht, `Android CI` bewusst starten. Kein CI-Erfolg behauptet, solange kein Lauf nachgewiesen ist.
- [ ] **Build-Gate:** Unit-Tests und Debug-APK, danach gezielt manueller `test_level=release` mit Emulator/Lint/MVV-Probes, soweit verfügbar. Fehler vor neuen Features beheben. APK-Artefakt und Signatur/Installationshinweise dokumentieren.
- [ ] **Visuelle Abnahme:** Echte Emulator-Screenshots der drei Tabs und Verbindungsdetails ansehen. Speziell prüfen: GPS-Status rechts in der Kopfzeile statt eigener Kachel; keine abgeschnittene Überschrift auf Galaxy-S25-Breite und bei größerer Schrift; grün/gelb/rot verständlich, kein irreführendes MVV-„LIVE“.
- [ ] **Galaxy-S25-Praxistest:** Erststart, Standort- und Kalenderberechtigungen, Termin/Ziel, MVV-Suche, Verbindungsdetails, Losgehzeit, Fahrt-Cockpit, Fußweg-Navigation und Rücknavigation. Screenshots und reproduzierbare Fehler sammeln. Vor realer Fahrt Ankunftszeit mit offizieller MVV-Auskunft abgleichen.
- [ ] **Sicherheitsgrenze:** OSM-Fußwegführung ist experimentell; bei Fehler/Offline/zweifelhaftem Abbiegehinweis nicht als zuverlässige alleinige Navigation empfehlen.

### P1 – Nächste Codepakete, jeweils einzeln testen und committen
- [ ] **Asynchrone MVV-Suche absichern (Audit #2):** Request-Generation/Ignore-Stale-Callbacks bei Termin-, Ziel-, Filter- und Standortwechsel; Tests mit vertauschter Antwortreihenfolge.
- [ ] **Routen- und Zeitkorrektheit (Audit #3–5):** Haltestellen-Fallback auf denselben Ankunftszeit-/Präferenzpfad bringen; Datum/Zeitzone statt bloß `HH:mm` (Mitternacht/Sommerzeit); Umstiege trotz fehlender Liniennummer korrekt zählen.
- [ ] **GPS-Freshness und Berechtigungen (Audit #6–7):** Alter und Genauigkeit der Position prüfen; Ausfall/Veralten erkennbar machen, ohne „aktuell“ vorzutäuschen; Berechtigungsreihenfolge und Ablehnungsfall testen.
- [ ] **Fußweg-Navigation absichern:** Routengeometrie und Fortschritt entlang der Route statt räumlich nächstem Manöver; Abweichung/Neuberechnung, Haltestellenkoordinaten statt unsicherem Namens-Geocoding; Offline-/Fehlerfälle testen.
- [ ] **Meine Fahrt und Verbindungsdetails polieren:** Kachelhierarchie, Zeiten/Icons, Status „später/bald/jetzt“, Countdown-Grenzen und geplante vs. geschätzte vs. echte Live-Werte; keine übergroßen Textblöcke.

### P2 – Qualität, Wartbarkeit und spätere Freigabe
- [ ] Screenshot-Navigation von festen Pixel-Taps auf Semantik-Tags/Compose-Tests umstellen; alte Smoke-Skripte prüfen und aktualisieren; Settings-UI-Test und größere Schrift/Tablet-Matrix ergänzen.
- [ ] Einstellungen und Startablauf: Persistenz Zuhause/Gehgeschwindigkeit/Puffer/Linienfilter nach Neustart; Ziel-Mehrdeutigkeit/Offline/Terminwechsel; Verkehrsmittel- und Sprachwahl nur als funktionierend anzeigen, wenn wirklich implementiert.
- [ ] MainActivity schrittweise in Komponenten und testbaren State zerlegen; Demo-Dateien erst nach Referenzprüfung aufräumen.
- [ ] MVV-Echtzeitrefresh nur mit verlässlicher Quelle und korrekter Kennzeichnung; kein Hintergrundtracking. Release-Signierung und N→N+1-Update mit Datenerhalt gesondert nachweisen.

### Arbeits- und Übergaberegel
1. Vor jeder Änderung HEAD/Workflow prüfen, keine parallelen widersprüchlichen Arbeiten.
2. Ein kleines Paket umsetzen → gezielte Tests → CI → Screenshot-Sichtprüfung → Commit/Status mit Link und offenen Risiken.
3. Nach jedem Paket diese Liste mit **Commit, CI-Lauf, Ergebnis und nächstem Schritt** fortschreiben; erledigte Punkte erst nach Nachweis abhaken.
4. Ohne explizit eingerichtete Automation erfolgt keine eigenständige Entwicklung über Nacht.
