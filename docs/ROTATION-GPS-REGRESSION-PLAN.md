# Regressionstestplan: Rotation und GPS-Track (Issues #13 und #14)

Stand: 22.11.2026. **Testentwurf, keine ausgeführten Tests und keine Freigabe.**
Dieser Plan ergänzt [PROJECT_STATUS.md](PROJECT_STATUS.md). Die CI-Preflight-Blockade hat Vorrang vor einer APK-Abnahme.

## #13 – Activity-Recreation und Konfigurationspersistenz

**Testaufbau:** Emulator API 35, definierter Kalendertermin, manuell gesetztes Zuhause, Gehgeschwindigkeit und Ankunftspuffer, eine gewählte MVV-Verbindung und eine aktive Fußwegroute. App über den echten Startscreen öffnen, keine ausschließlich künstlichen Screenshots als Nachweis.

| Aktion | Erwartetes Verhalten |
| --- | --- |
| Portrait → Landscape → Portrait bei ausgefülltem Start/Ziel | Eingaben, aufgelöste Zielkoordinaten und gewählter Termin bleiben identisch; keine doppelte Geocoding-/MVV-Anfrage durch bloße Rotation |
| Rotation während MVV-Abfrage | Keine zweite unkontrollierte Anfrage; nur die zuletzt gültige Antwort darf UI-Zustand verändern |
| Rotation bei ausgewählter Verbindung | Verbindungs-ID, Etappen, Abfahrtszeit und aktueller Tab bleiben erhalten |
| Rotation bei aktiver Navigation | Route, Track und Fortschritt bleiben erhalten; GPS-Listener nicht doppelt registriert |
| Rotation in Einstellungen | Persistierte Werte unverändert; vorübergehende Editierwerte werden entweder erhalten oder bewusst verworfen, nie unbemerkt durch Defaults ersetzt |
| Prozessbeendigung durch Android nach Hintergrundwechsel | Persistente Einstellungen bleiben; laufende Navigation wird entweder belastbar wiederhergestellt oder mit eindeutigem Hinweis beendet, niemals als frische Position ausgegeben |

**Automatisierung:** Compose-Instrumentierung mit ActivityScenario.recreate() plus Zustandsassertions; für Prozess-Tod separater Geräte-/ADB-Test. Network-Repository faken und Aufrufanzahl prüfen. Zustandsbesitz (rememberSaveable, ViewModel, SavedStateHandle, Preferences) nach tatsächlichem Codebefund festlegen.

## #14 – Trackkontinuität, GPS-Ausreißer und Rerouting

**Testaufbau:** deterministische Zeit-/Positionsquelle; bekannte OSRM-Polyline und Track mit mindestens sechs Punkten. Anzeige des dunkellila bereits gegangenen Pfads von der noch verbleibenden Route getrennt halten.

| Eingabe/Ereignis | Erwartetes Verhalten |
| --- | --- |
| Folge valider GPS-Fixes entlang Route | Zurückgelegter Pfad wächst kontinuierlich; Fortschritt entlang Route bleibt monoton, abgesehen von ausdrücklich dokumentierter Neuberechnung |
| Ein GPS-Fix ohne Genauigkeit / mit veralteter Zeit | Kein Löschen oder Zurücksetzen des bisherigen Tracks; keine sprunghafte Fortschrittsänderung |
| Einzelner starker Positionsausreißer und danach normaler Fix | Ausreißer nicht als gelaufene Strecke übernehmen; Track bleibt sichtbar |
| GPS-Signalverlust und Wiederkehr | Bisheriger Track bleibt sichtbar; Live-Status wird als veraltet/ungenau gekennzeichnet |
| Reroute-Antwort trifft nach neuerem Reroute ein | Ältere Antwort darf neuere Routengeometrie und Track nicht überschreiben (Generation/Request-ID) |
| Reroute schlägt fehl / Timeout / Offline | Bereits gegangener Track bleibt; letzte gültige Route bleibt als solche gekennzeichnet, kein falscher Erfolg |
| Rotation während Reroute | Track und zuletzt gültiger Fortschritt bleiben; kein doppelter paralleler Reroute-Lauf |
| Route bewusst neu gestartet oder Ziel gewechselt | Track wird nur aufgrund eines expliziten neuen Navigationskontexts zurückgesetzt, nicht durch GPS-/Compose-Recomposition |

**Automatisierung:** Unit-Tests für Track-Akkumulator, GPS-Freshness, Route-Generation und out-of-order Responses; Instrumentierung für Track-Sichtbarkeit bei Rotation. Fake-Uhr und Fake-Location-Stream statt echter GPS-Hardware für reproduzierbare Assertions.

## Freigabegate

1. Aktuellen `main`-Commit und CI-Run erfassen; Preflight-Fehler anhand realer Exitcodes beheben.
2. Unit-/Instrumentierungstests für obige Fälle implementieren, ausführen und Ergebnisse verlinken.
3. Android CI inklusive Lint und Release-Emulator-Smoke-Tests muss für **denselben Commit** erfolgreich sein; Screenshots der beiden Orientierungen und des Tracks sichten.
4. Erst nach separatem Gerätetest auf dem S25 und belegter Signatur-/Versionskompatibilität eine APK zur Installation empfehlen.

**Offen:** Dieser Plan enthält keine Implementierung und beweist keine Fehlerursache. Änderungen an Signing, Secrets oder Release-Gates sind nicht Teil des Pakets.
