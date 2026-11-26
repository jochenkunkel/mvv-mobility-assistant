# Datenschutzhinweis für Mein Weg

Diese Anwendung läuft vollständig auf deinem Gerät. Es gibt keinen eigenen Server und keine Nutzerkonten.

## Welche Daten die App verarbeitet

- **Kalendertermine:** Titel, Beginn und Zielort werden nur lokal gelesen (nach Berechtigung) und nicht an eigene Server gesendet.
- **Standort:** Aktuelle Position wird im Vordergrund für Routen und Navigation genutzt. Der zurückgelegte Weg wird nur während einer aktiven Sitzung im Arbeitsspeicher gehalten (maximal 1500 Punkte) und nicht dauerhaft gespeichert.
- **Zieladressen:** Bei der Auflösung kann der Gerät-Geocoder einen Online-Dienst des Geräteherstellers abfragen. Zieltexte können dabei an diesen Anbieter übertragen werden.
- **MVV-Fahrplan:** Start- und Zielkoordinaten werden an die öffentliche MVV-EFA-Schnittstelle (`efa.mvv-muenchen.de`) gesendet.
- **Fußwegrouting:** Koordinaten werden an den öffentlichen OSRM-Dienst (`routing.openstreetmap.de`) gesendet.
- **Kartenkacheln:** Kartenbilder kommen von OpenStreetMap; es gilt „© OpenStreetMap contributors".

## Speicherung und Löschung

Einstellungen (Ankunftspuffer, Gehgeschwindigkeit, Filter, Heimadresse, Sprachwahl) liegen lokal in den App-Preferences. Die Navigation kann jederzeit über **„Navigation beenden und Spur löschen"** beendet werden; dabei wird die aufgezeichnete GPS-Spur verworfen. Ein App-Neustart nach Prozessende stellt die Spur nicht wieder her.

Debug-Builds bieten einen ausdrücklich vom Nutzer ausgelösten Export von GPS- und MVV-Rohdaten. Release-Builds enthalten diese Exportfunktion nicht.

## Berechtigungen

`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` und `READ_CALENDAR` werden nur für die beschriebenen Funktionen verwendet und können jederzeit in den Android-Einstellungen entzogen werden. Ohne Berechtigung funktioniert die automatische Routenplanung nicht.

Details und Anbieterabgleich: [PRIVACY-AND-PROVIDER-POLICY.md](PRIVACY-AND-PROVIDER-POLICY.md).
