# Geräteabnahme nach Datums- und Dashboard-Fix (#19/#20)

Stand: 26.11.2026, 20:20 MEZ. Grundlage: `main` `adb8fd8`, [Designvergleich](design/review/DASHBOARD-5-REVIEW.md), [Dashboard-Vorgabe](design/dashboard-design-5-kompakt.svg). **Keine Gerätefreigabe durch dieses Dokument.**

## Priorisierte reale Prüfschritte (Samsung S25)

| ID | Vorgehen | Erwartung | Status |
| --- | --- | --- | --- |
| DATE-01 | Donnerstagabend einen Termin für Samstagmittag wählen, Verbindung suchen | Suchdatum sichtbar: Samstag; Abfahrten nicht fälschlich Donnerstag | OFFEN |
| DATE-02 | Gleiche Suche mit „Frühere Verbindungen“ und „Erneut suchen“ | Suchdatum bleibt Samstag, nur das gewollte Zeitfenster ändert sich | OFFEN |
| DATE-03 | Freitagabend Termin Samstagmorgen; App drehen, zwischen Tabs wechseln | Datum/Uhrzeit und ausgewählte Verbindung bleiben erhalten | OFFEN |
| DATE-04 | Eine zukünftige Fahrt mit gleicher Uhrzeit wie bereits vergangene heutige Fahrt | Nicht fälschlich „ABGEFAHREN“; Vergleich absoluter Zeitpunkte | OFFEN |
| DATE-05 | Mitternachtsübergreifende Verbindung prüfen | Datum der einzelnen Abschnitte stimmt, kein rückwärts laufender Countdown | OFFEN |
| ICON-01 | Bus 56 und U-Bahn U6 in derselben Verbindung ansehen | Bus 56 hat Bus-Icon, U6 hat U-Bahn-Icon; Typ kommt aus MVV-Metadaten | OFFEN |
| ICON-02 | Unbekannten Verkehrsmitteltyp simulieren | Neutrales Icon, keine falsche Bus-/U-Bahn-Klassifizierung | OFFEN |
| UI-01 | „Meine Fahrt öffnen“; Screenshot mit SVG und Emulator-Vergleich vergleichen | Countdown dominant, kompakte Karten, Hauptaktion sichtbar, keine App-Tab-Leiste | OFFEN |
| UI-02 | Dashboard drehen, Schriftgröße erhöhen, zurück und wieder öffnen | Keine abgeschnittenen Aktionen, Fahrt und GPS-Spur bleiben erhalten | OFFEN |
| UI-03 | GPS deaktivieren, unklaren Steig / fehlende Koordinaten testen | Ehrlicher GPS-Status und Warnungen; kein erfundener Puffer | OFFEN |
| NAV-01 | Navigation zum Einstieg starten, GPS-Bewegung und Rezentrieren prüfen | Route bleibt stabil, keine springende/verschwindende Spur | OFFEN |

## Nachweisführung

- Screenshots nur nach Prüfung auf private Standort-, Adress- und Termindaten veröffentlichen; andernfalls anonymisierte Ausschnitte oder rein textuelle Testergebnisse.
- Je Test: Gerät/Android-Version, App-Commit und Build-ID, tatsächlicher Suchzeitpunkt mit Zeitzone, erwartetes/angezeigtes Datum, Ergebnis und ggf. Ticket vermerken.
- Aktuelle CI-Läufe für `adb8fd8`: [Push](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45882640638) und [manuell](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45882671572) waren bei Erstellung noch **in progress**. Der vorherige [Push-CI](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45874880421) war erfolgreich, deckt den neuen Fix aber nicht ab.
- Emulator: 38 Instrumentierungstests laut Projektstatus erfolgreich; physische S25-Abnahme weiterhin ausstehend. Bis zum vollständigen Nachweis keine neue APK-Installationsfreigabe.

## Weitere Priorität

1. CI für `adb8fd8` bis Abschluss prüfen; Fehlschläge mit Job-Logs dokumentieren und reparieren.
2. DATE-01 und ICON-01 auf dem S25 priorisieren, da das ursprüngliche Nutzerfeedback genau diese Fehler beschreibt.
3. UI-01 gegen echte S25-Aufnahme und Design 5 prüfen; #20 erst nach visueller Abnahme schließen.
4. Danach GPS-/OSM-Fortschritt und Puffer mit echten Fußwegen validieren.

**Chat-Grenze:** Vollständige externe Projekt-Chats „Main“/„Schedule“ sind hier nicht direkt auslesbar; berücksichtigt sind die im aktuellen Verlauf und Repository dokumentierten Nutzerentscheidungen, keine angenommenen zusätzlichen Anforderungen.
