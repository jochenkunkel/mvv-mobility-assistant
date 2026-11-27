# Karten-Clipping – Umsetzung und QA (27.11.2026, 13:20 MEZ)

## Reproduktionshinweis
Nutzer-Screenshot aus MeinWeg2-Projektchat: rotierte OSM-Kacheln ragen diagonal aus der Fußwegkarte über Header/Status (#23). Untere Aktionsbuttons liegen zusätzlich im Bereich der Android-Systemnavigation (#23/#24).

## Codebefund
`WalkingMiniMap.kt` bettet eine osmdroid-`MapView` via `AndroidView` in eine Compose-`Box` ein und setzt die Kartenorientierung über `view.setMapOrientation(-lastHeading.floatValue)`. Vor dem Fix hatten weder der Eltern-Container noch die `AndroidView` eine explizite Clip-Grenze. Dies ist eine **plausible**, noch nicht durch einen Gerätetest bestätigte Ursache des diagonal herausragenden Kartenbildes.

## Kleines Codepaket
Commit `d3de319c49dceacea87b4a8755c651037b5a7a26` ergänzt `clipToBounds()` an der Karten-`Box` und der `AndroidView`. Die Kartendaten, GPS-/Routenlogik und das Attribution-Overlay bleiben unverändert.

## Abnahme / offene Risiken
- **Nicht als behoben markieren**, bevor auf einem Samsung S25 bei gedrehter Karte, GPS-Updates, Zoom und Portrait/Landscape ein Vorher-/Nachher-Screenshot geprüft wurde.
- Prüfen, ob osmdroid intern eine weitere View-/Canvas-Ebene außerhalb des Compose-Clippings rendert. Falls weiterhin fehlerhaft, die tatsächlichen Parent-View-Bounds, MapView-Transformation und Tile-Overlay-Clipping auf dem Gerät instrumentieren, statt blind `zIndex` zu erhöhen.
- Systemleisten-/Button-Inset ist **separat offen** (#24 und Teil von #23).
- CI: GitHub-Connector lieferte für `d3de319c` bei kombinierter Statusabfrage keine Status-Checks und bei Commit-Workflow-Abfrage keine PR-getriggerten Runs. Push-Workflow-Ergebnis daher **nicht verifiziert**; keine lokalen Gradle-/Emulator-Tests in diesem Durchlauf.
- Keine APK-/Installationsfreigabe.

## Folgeprioritäten
1. CI/Emulator-Screenshot für `d3de319c` verifizieren und reale Kartenabnahme (#23).
2. Systemleisten-/Insets-Verhalten (#24).
3. GPS-Positionsschätzung im Bus mit expliziter Unsicherheitsanzeige (#22).
4. Freigegebenes Dashboard-Bild korrekt im Repository hinterlegen und #21 visuell abnehmen; danach #25 in kleine Implementierungsschritte überführen.

Vollständige andere Chats (Main/Schedule) sind nicht direkt live abrufbar. Die vorliegenden Nutzer-Screenshots und GitHub-Issues wurden als Quellen verwendet.
