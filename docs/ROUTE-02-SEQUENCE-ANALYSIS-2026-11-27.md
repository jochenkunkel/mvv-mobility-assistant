# ROUTE-02: GPS-Sequenz und Haarnadel – Implementierungsanalyse

Stand: 27.11.2026, 02:25 MEZ. **Codeanalyse, kein implementierter Fix, kein Testlauf.**

## Verifizierte Codepfade
- `data/RouteProgress.kt`: `project(path, latitude, longitude)` wählt ausschließlich das Segment mit minimalem seitlichem Abstand; Gleichstand gewinnt das erste Segment. Weder letzter Fortschritt noch Zeitstempel, Genauigkeit, Geschwindigkeit oder Richtung werden übergeben. `heading` und `nextTurn` rufen diese zustandslose Projektion separat auf.
- `data/WalkingNavigation.kt`: `nextTurn(route, position)` unterdrückt ungenaue Fixes (>35 m), delegiert danach aber direkt an `RouteProgress.nextTurn`. Eine Grenze für **Sprünge entlang der Route** ist dort nicht vorhanden. Für „Ziel erreicht“ gilt zusätzlich die 12-m-Genauigkeitsgrenze.
- `data/LocationFreshness.kt`: monotone Altersprüfung vorhanden; **Freshness ist nicht gleich Reihenfolgeprüfung** zweier nacheinander empfangener GPS-Callbacks.
- `app/src/test/java/de/mvv/mobility/data/RouteProgressTest.kt`: neue Tests für Haarnadel und exakt überlappende Segmente sind vorhanden. Sie testen Einzelpositionen, keine Sequenz und keinen realen Navigationszustand.

## Konkretes reproduzierbares Risiko
Bei einer Haarnadel mit zwei parallelen Schenkeln (Abstand ca. 11 m) können GPS-Fixes mit 8–15 m Ungenauigkeit abwechselnd auf den Hin- und Rückweg projiziert werden. Dadurch springen `alongMeters`, Kartenrichtung und die Auswahl der nächsten Abbiegeanweisung, obwohl die Person auf einem Schenkel weitergeht. Ein präziserer Einzel-Fix kann trotzdem auf dem falschen Schenkel landen. Die vorhandenen Einzel-Fix-Tests verhindern das nicht.

## Nächstes kleines Codepaket (zuerst untersuchen, dann ändern)
1. In `MainActivity.kt`, `NavigationSessionViewModel.kt` und `WalkingMiniMap.kt` alle Aufrufer von `RouteProgress.project`, `heading` und `WalkingNavigation.nextTurn` lokalisieren. **Einen** zentralen Fortschrittszustand pro aktiver Route festlegen, nicht getrennte Filter für Pfeil, Karte und Puffer.
2. Deterministische Testsequenz mit synthetischer Haarnadel: Fixes am Hinweg bei 0/25/50 m, dann Jitter nahe dem Rückweg, anschließend echte Wendung. Erwartung: kein spontaner 100+-m-Sprung; legitimer Wechsel nach der Wendung bleibt möglich.
3. Separater Test: später eintreffender Callback mit **älterem** `elapsedRealtimeNanos` wird nicht zum neuen Navigationsfortschritt; frischer, präziser Fix nach GPS-Lücke darf wieder angenommen werden.
4. Eine routegebundene Hysterese mit begrenzter plausibler Fortschrittsänderung und definiertem Reset bei Reroute implementieren. Keine sture Monotonie: echtes Zurückgehen zulassen. Bei Ambiguität Fortschritt/ETA als unsicher kennzeichnen, statt Präzision vorzutäuschen.
5. Tests `gradle :app:testDebugUnitTest`, ggf. Compose/Emulator und zugehörigen GitHub-Actions-Lauf **für denselben SHA** dokumentieren; danach echte S25-Fußwegabnahme.

## Abgrenzung
Diese Analyse ändert keinen Produktionscode, beweist keine GPS-Stabilität und gibt keine APK frei. Weitere Chats Main/Schedule sind nicht vollständig direkt lesbar; keine zusätzlichen Anforderungen daraus angenommen.
