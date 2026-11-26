# GPS/OSM – reale Fußwegabnahme (S25)

Stand: 26.11.2026, 23:24 MEZ. **Prüfprotokoll, kein Nachweis einer erfolgreichen Geräteprüfung.**

## Zweck und Voraussetzungen

Nach den CI-/Emulatorprüfungen bleibt die reale Stabilität von GPS-Erfassung, OSRM/OSM-Routenfortschritt und dynamischem Restpuffer ungeprüft. Test mit bekannter, sicherer Fußwegstrecke bei Tageslicht; nicht während einer gefährlichen Straßenquerung bedienen. Nur anonymisierte Logs/Screenshots ins öffentliche Repository übernehmen. Build-SHA, Android-Version, Standortberechtigung und Energieoptimierung erfassen. Signing und Secrets nicht ändern.

## Prüfmatrix

| ID | Durchführung | Erwartung / Fehlerindikator |
| --- | --- | --- |
| GPS-01 | Route starten, 2 Minuten stehen bleiben, dann 300–500 m gehen | Standortaktualisierung ohne dauerhafte Sprünge; Stillstand erzeugt keinen erfundenen Routenfortschritt |
| GPS-02 | Display sperren/entsperren, App kurz in Hintergrund/Vordergrund | Zustand und bisherige Spur bleiben erhalten; Berechtigungs-/Foreground-Grenzen werden transparent angezeigt |
| GPS-03 | Standortberechtigung entziehen bzw. GPS ausschalten und wieder einschalten | Verständlicher Fehlerstatus; keine angeblich „live“ erfasste Position; Wiederaufnahme ohne irreführenden Restpuffer |
| GPS-04 | Schlechter Empfang (z. B. dichter Baumbestand) | Ungenaue oder veraltete Fixes werden nicht als präzise Navigation ausgegeben; kein wildes Umspringen von Abbiegehinweisen |
| OSM-01 | Abzweigung auf realer Route passieren | Nächster Schritt wechselt erst bei belastbarem Fortschritt; Distanz zur Abzweigung sinkt plausibel |
| OSM-02 | Bewusst kurz von der Route abweichen und zurückkehren | Off-route/Fallback nachvollziehbar; kein stilles Teleportieren zum nächsten Routensegment |
| OSM-03 | Karte verschieben, anschließend Rezentrieren | Manuelle Ansicht bleibt bis zur Rezentrier-Aktion; danach folgt die Karte wieder der Position |
| OSM-04 | Rotation und App-Wechsel während laufender Route | Aktive Route, Fortschritt, Auswahl und Karte bleiben konsistent |
| BUF-01 | Puffer vor Abfahrt bei veränderlicher Gehzeit beobachten | Berechnung nutzt korrekten Abfahrts-Instant und verbleibende, nicht gesamte Gehzeit; kein negativer „sicherer“ Puffer |
| BUF-02 | Anschluss ohne belastbare anschlussspezifische Gehzeit | „Unbekannt“ mit Grund statt einer aus der Gesamtgehzeit erfundenen Zahl |
| MVV-01 | Einstiegshaltestelle mit Steig und Koordinaten prüfen | Navigation führt zum Einstieg des ersten Fahrzeugsegments, nicht zum Ausstieg oder Reiseziel |

## Nachweis pro Test

- Zeitpunkt mit Zeitzone, App-Commit/Build-ID, Gerät/Android-Version, Berechtigungszustand.
- Erwarteter und tatsächlicher Zustand, Ergebnis PASS/FAIL/BLOCKED, anonymisierter Beleg.
- Bei FAIL: reproduzierbare Schritte und separates GitHub-Issue mit Referenz auf Test-ID; keine personenbezogenen GPS-Traces veröffentlichen.
- Bei BLOCKED: exakte fehlende Voraussetzung und nächster Schritt.

## Freigabegrenze

Grüne CI-Jobs oder Emulator-Screenshots ersetzen diese S25-Abnahme nicht. Bis GPS-01 bis OSM-04 und die Pufferfälle tatsächlich geprüft sind, **keine Aussage über zuverlässige reale Abbiegeführung und keine neue APK-Installationsfreigabe**. Verknüpft mit [PROJECT_STATUS.md](PROJECT_STATUS.md) und [DEVICE-ACCEPTANCE-ISSUES-19-20.md](DEVICE-ACCEPTANCE-ISSUES-19-20.md).
