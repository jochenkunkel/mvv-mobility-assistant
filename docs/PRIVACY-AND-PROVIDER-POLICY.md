# Datenschutz- und Anbieterrichtlinien-Abgleich

**Stand:** 26.11.2026. Grundlage für Issue #11 (PRIV-01). Dies ist eine technische Dokumentation des tatsächlichen Codeverhaltens, keine Rechtsberatung.

## Datenflüsse

| Daten | Quelle | Ziel | Zeitpunkt | Speicherung |
| --- | --- | --- | --- | --- |
| Kalendertermine (Titel, Zeit, Zieltext) | Android Calendar Provider, nur mit Berechtigung | ausschließlich lokal im App-Prozess | Bei Start/Refresh | Keine serverseitige Speicherung; `allowBackup=false` |
| GPS-Position | Android LocationManager | lokal für Navigation, Anzeige und Spur | Vordergrund, während der Navigation | Nur im Speicher (`WalkedTrace`, begrenzt auf 1500 Punkte) |
| Zieltext aus dem Termin | Kalender | Android `Geocoder` (`DestinationLookup`) – je nach Gerät/Anbieter online | Automatische Zielauflösung | Keine dauerhafte Speicherung durch die App |
| Start-/Zielkoordinaten | GPS / Geocoder | MVV EFA (`https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2`) | Bei Verbindungsuche | Keine API-Keys; Antwort nur temporär im Speicher |
| Koordinaten für Fußweg | GPS / EFA-Haltestelle | OSRM-Demo (`https://routing.openstreetmap.de/routed-foot`) | Bei Routenberechnung | Keine Speicherung durch die App |
| Kartenkacheln | Osmdroid (`TileSourceFactory.MAPNIK`) | OpenStreetMap-Standard-Tile-Server | Beim Kartenaufbau | Osmdroid-Cache auf dem Gerät; sichtbare Attribution „© OpenStreetMap contributors" |
| Zielkoordinaten | Benutzeraktion | Google-Maps-Deeplink (`dir/?api=1`) | Nur bei explizitem „In Maps öffnen" | Kein API-Key, keine Übertragung durch die App selbst |

## Anbieterpflichten und Umsetzung

- **OpenStreetMap / MAPNIK-Kacheln:** Karten- und Tile-Daten stehen unter ODbL bzw. den OSM-Tile-Nutzungsbedingungen. Die Attribution wird seit dieser Runde als sichtbarer Compose-Text in `WalkingMiniMap` eingeblendet (`testTag("osm_attribution")`), unabhängig von Kartenrotation und Routenfortschritt. Zusätzlich steht der Hinweis in `NOTICE`.
- **OSRM-Demo-Server:** `WalkingNavigation.fetch` setzt einen `User-Agent` (`MeinWeg2/0.2 Android foot navigation`) sowie Connect-/Read-Timeouts von je 8 Sekunden. Die Nutzung erfolgt als Demo-Verkehr; bei höherem Aufkommen ist ein eigener Routingdienst oder ein kommerzieller Anbieter erforderlich.
- **MVV EFA:** Öffentlicher Endpunkt ohne Zugangsdaten im Repository. Nutzungsbedingungen sind nicht dokumentiert; offener Punkt für eine spätere Produktfreigabe.
- **Android Geocoder:** Der tatsächliche Anbieter hängt vom Gerät ab; Zieltexte können an einen Online-Geocoder übertragen werden. Die App speichert keine Geocoder-Ergebnisse dauerhaft und bietet manuelle Zielkorrektur an.
- **Google Maps:** Reiner Deeplink ohne API-Key; die App überträgt nichts eigenständig.

## Offline- und Fehlverhalten

- Routing- und EFA-Anfragen laufen in Hintergrund-Threads mit Timeouts; Fehler werden als `Result.failure` an die UI gemeldet.
- `RouteErrorMessage` erkennt `IOException`-Ketten und zeigt verständliche Offline-Meldungen an; zwischengespeicherte Fahrpläne werden nicht als Live-Verbindungen ausgegeben. Entsprechende Regressionstests liegen in `app/src/test/java/de/mvv/mobility/data/OfflineRouteErrorTest.kt`.
- Bereits zwischengespeicherte Kartenkacheln bleiben ohne Netz sichtbar; die Route selbst wird lokal aus der OSRM-Antwort gezeichnet und nicht nachgeladen.
- Ein fehlgeschlagener oder ungenauer GPS-Fix pausiert die Spurfortschreibung, löscht aber keine bereits aufgezeichnete Strecke.

## Debug-Exporte (nur Debug-Builds)

`GpsDebugRecorder` und `DebugResponseCapture` sind `opt-in`, auf 2400 Ereignisse bzw. 512 KB begrenzt und speichern nichts automatisch auf Platte. Exporte erfolgen ausschließlich über den Android Storage Access Framework (`CreateDocument`) nach aktiver Nutzeraktion. Solche Exporte können GPS- und Zielinformationen enthalten und dürfen nicht in Issues, PRs oder öffentliche Artefakte hochgeladen werden.

## Offene Punkte

1. Verbindliche Nutzungsbedingungen des MVV-EFA-Endpunkts für eine Produktiv-/Store-Nutzung klären.
2. Kapazität und Lizenz des öffentlichen OSRM-Demo-Servers vor höherem Nutzungsaufkommen prüfen.
3. Datenschutzhinweis für die Veröffentlichung im Store ergänzen (Kalender-/Standortzwecke, Geocoder-Übertragung).
4. Instrumentierte Sichtprüfung der Attribution auf einem API-35-Emulator ausführen; der zugehörige Test ist implementiert, aber in dieser Runde **nicht ausgeführt**.
