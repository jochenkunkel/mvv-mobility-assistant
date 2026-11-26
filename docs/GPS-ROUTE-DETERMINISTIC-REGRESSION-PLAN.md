# GPS und OSM: deterministische Regressionen

Stand 27.11.2026. Testentwurf; keine Tests ausgeführt, keine Gerätefreigabe.

Ergänzung zur realen S25-Prüfmatrix GPS-OSM-REAL-WALK-ACCEPTANCE.md. Vor Implementierung vorhandene Tests und APIs in LocationFreshness.kt, RouteProgress.kt, WalkingNavigation.kt und NavigationSessionViewModel.kt lesen.

| ID | Szenario | Erwartung |
|---|---|---|
| GPS-01 | Fix t=100 s, Prüfung t=101 und t=150 (monotone Uhr) | Staler Fix nicht als live ausgeben |
| GPS-02 | Fixes t=100, t=102, verspäteter Callback t=101 | Keine unbegründete Rückwärtsbewegung durch Callback-Reihenfolge |
| GPS-03 | Genauigkeit 150 m, danach 8 m, dann 150 m | Ungenaue Fixes erzeugen keine falsche Routen-Completion |
| ROUTE-01 | Synthetische Gerade, Positionen bei 0/25/50/75/100 % | Distanz und Abbiegeschritt plausibel, kein Überspringen |
| ROUTE-02 | Haarnadel mit parallelen Segmenten, Fix nahe späterem Segment | Kein unbegründetes Vorwärtsspringen |
| ROUTE-03 | Stillstand an Abzweigung mit Positionsjitter | Kein Flackern der Abbiegehinweise |
| ROUTE-04 | 100 m Off-route, dann Rückkehr | Off-route kenntlich; keine fiktive präzise ETA |
| BUFFER-01 | Abfahrt 12:30, Gehrestzeit 10 min, jetzt 12:15 und 12:22 | Puffer +5 bzw. -2 min; negatives Ergebnis warnen |
| BUFFER-02 | Nur Gesamtgehzeit, keine Anschlussgehzeit | Anschlusspuffer unbekannt |
| STATE-01 | Aktive Route und ausgewählte Fahrt bei Activity-Recreation | Navigation, Auswahl und Countdown bleiben konsistent |

## Umsetzung

1. Vorhandene Tests auf Abdeckung prüfen; keine Duplikate anlegen. Erstes kleines Codepaket: GPS-02 oder ROUTE-02, falls noch nicht abgedeckt.
2. Synthetische Koordinaten und kontrollierbare Zeitquelle verwenden; keine privaten GPS-Spuren committen.
3. Keine künstliche Monotonie erzwingen, falls tatsächliches Zurückgehen auf der Route erlaubt ist. Zuerst Hysterese-Semantik festlegen.
4. Testdatei, exakten Testbefehl, Ergebnis und CI-URL in PROJECT_STATUS.md dokumentieren. Reale S25-Abnahme bleibt separat offen.
5. Keine Installationsfreigabe aufgrund dieser Planung oder allein aufgrund von Unit-Tests.
