# Entwicklungsabgleich – 27.11.2026, 12:22 MEZ

## Verifizierter Ausgangspunkt
- main vor dieser Änderung: `1c06509c06a24e5bff9a24063a86984008496df1` (Dokumentations-Triage für Dashboard, Kartenoverlay, Bus-GPS, Immersive Mode).
- Offene Issues: #21 Dashboard-Design, #22 Bus-GPS-Schätzung, #23 Kartenoverlay/Insets, #24 Android-Immersive-Mode, **#25 flexible Fahrtauswahl**. Keine offenen PRs.
- Neuester Push-CI für `1c06509`: [Run 45980866633](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45980866633), completed/success. Belegt keinen realen S25-Test.
- README, AGENTS.md, CHAT-START, PROJECT_STATUS, DECISIONS und IMPLEMENTATION-ROADMAP geprüft. Ältere Angaben „0 offene Issues“ und „Systemleisten bleiben sichtbar“ sind historische Snapshots; #24 fordert nun das Ausblenden der Android-Navigationsleiste.
- Weitere Chats „Main“ und „Schedule“ sind nicht vollständig direkt lesbar; als neue gesicherte Anforderung dient Issue #25 aus dem aktuellen Projektchat. Keine unbestätigten Chat-Inhalte ergänzt.

## Neue Arbeit: #25 – risikominimierter Umsetzungsplan
1. **FILTER-01 – Semantik & reine Filtertests:** `Start mit Linie` = erste ÖPNV-Etappe muss die gewählte Linie sein; vorheriger Fußweg und spätere Umstiege erlaubt. Bestehende Modi `Alle`, `Nur Linie`, `Mit Linie` unverändert lassen. Vor Implementierung reale Journey-Leg-Datenstruktur und Linien-Normalisierung prüfen. JVM-Tests: Fußweg+U6+Bus akzeptiert; Bus+U6 abgelehnt; fehlende Linie führt zu explizitem No-Match.
2. **WALK-01 – Manuelle Etappensubstitution:** Erst Datenvertrag/Fehlerzustände und deterministische Tests für einzelne ÖPNV-Etappe → OSRM/OSM-Fußweg; Anschlüsse und Gesamtreise neu prüfen, bei fehlenden Koordinaten nicht still schätzen. Undo und Persistenz über Rotation.
3. **WALK-02 – Optionale Kurzstreckenpräferenz:** Standard 500 m, einstellbar 200–1000 m; nur mit nachweisbarer Gesamtzeit-/Anschlussverbesserung. Kein Zwangsersatz, manuelle Wahl gewinnt.
4. **UI-01:** Kompakte Vierfach-Auswahl und Etappenaktion auf kleinen Displays, große Schrift, Rotation, Accessibility.

## Vorrangige Fehlerarbeit vor Feature-Rollout
- **P0 #23:** Karten-Tiles dürfen Header/Status nicht überzeichnen; Insets für untere Buttons; Regression auf S25.
- **P0 #22:** GPS im Bus: Status `Fahrt aktiv · Position geschätzt` bei kurzer Lücke, aber **kein falsches GPS LIVE** und keine garantierte Anschlusserreichbarkeit. Hysterese/Tests.
- **P1 #21/#24:** Freigegebene neue Dashboard-Bildreferenz statt veralteter SVG, Safe-Area und gezielter Immersive-Mode nur im Dashboard; systemkonforme temporäre Navigation.
- **P1 #25:** Umsetzung nach Stabilisierung der Navigation.

## Qualitätsgrenze
Dieser Commit ist **ausschließlich Dokumentation/Planung**. Kein Kotlin-Code geändert, keine Tests lokal ausgeführt. Der grüne Lauf für den vorherigen HEAD ist kein CI-Beleg für diesen Commit. Nach Push den neuen Actions-Lauf anhand der **exakten SHA** prüfen; keine APK-/Signing-/Installationsfreigabe ohne passende Gates und S25-Abnahme.
