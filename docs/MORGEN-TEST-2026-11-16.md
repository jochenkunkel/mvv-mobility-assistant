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
