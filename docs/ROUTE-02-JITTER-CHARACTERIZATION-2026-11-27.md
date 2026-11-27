# ROUTE-02 – GPS-Jitter-Charakterisierung (27.11.2026, 03:20 MEZ)

## Stand
Commit `7767994` ergänzt `hairpinGpsJitterCanCauseLargeStatelessProgressJump` in `RouteProgressTest.kt`. Der Test beschreibt **bewusst das aktuelle Fehlerrisiko**, keine Reparatur: Bei synthetischer Haarnadel mit parallelen Schenkeln (~11 m Abstand) springt die zustandslose Projektion durch ~9 m Positionsjitter vom Hin- auf den Rückweg und verändert `alongMeters` um mehr als 100 m.

Die beiden vorherigen Einzel-Fix-Tests bleiben bestehen. Diese Charakterisierung schützt davor, das Risiko durch bloßes Vorhandensein der bisherigen Tests als behoben zu bezeichnen.

## Nächste Implementierung
1. Aufrufer in `MainActivity.kt`, `NavigationSessionViewModel.kt` und `WalkingMiniMap.kt` auf gemeinsame Fortschrittsquelle prüfen.
2. Routegebundenen Zustand mit letzter akzeptierter Projektion und monotonem GPS-Zeitstempel einführen; verspätete Callbacks ignorieren, große Sprünge bei mehrdeutiger Geometrie nicht übernehmen. Echtes Zurückgehen und Wenden ermöglichen.
3. Neuen **positiven** Sequenz-Regressionstest für 0/25/50 m, Jitter, Wendung und Reroute schreiben; Charakterisierung danach aktualisieren.
4. `gradle :app:testDebugUnitTest`, Instrumentierung und CI für exakt getesteten SHA prüfen.

## Nachweise / Freigabe
Der Commit wurde auf `main` geschrieben. **Keine lokalen Gradle- oder Emulator-Tests ausgeführt.** Die verfügbare Commit-Workflow-Abfrage liefert nur PR-bezogene Läufe und für `7767994` keine Ergebnisse; das bedeutet **nicht**, dass ein Push-CI grün oder rot ist. Kein APK-Go, S25-Fußwegabnahme offen. Keine Signing-/Secret-Änderungen.

Andere Projekt-Chats Main/Schedule nicht vollständig live lesbar; nur im laufenden Chat und Repository belegte Anforderungen berücksichtigt.
