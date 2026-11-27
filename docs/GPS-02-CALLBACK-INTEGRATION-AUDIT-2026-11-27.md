# GPS-02: Callback-Integration – Bestandsabgleich (27.11.2026, 05:22 MEZ)

## Verifizierter Ist-Zustand
In `MainActivity.kt` erfolgt die produktive Annahme der GPS-Fixes bereits innerhalb von `CurrentLocationRepository.observeForeground`. Die Bedingung `acceptable` verlangt **gleichzeitig** `isFreshFix(found)`, `previous == null || isNewer(found, previous)`, eine Genauigkeit von höchstens 100 m (falls vorhanden) und den Schutz vor einem deutlich schlechteren Fix nach einem kürzlich präzisen Fix. Erst danach wird `location = found` gesetzt und gegebenenfalls die GPS-Spur ergänzt.

Damit ist die Aussage „es gibt keinerlei Reihenfolgeprüfung im Android-Callback“ **falsch**. Die neu eingeführte, bisher ungenutzte Hilfsklasse `LocationFixOrdering` darf nicht blind als zweiter, potenziell abweichender Guard eingebaut werden. Zuerst muss die Semantik von `CurrentLocationRepository.isNewer` (insbesondere monotone Zeitstempel, 0-Werte, gleiche Zeitstempel und Neustart/Rotation) mit `LocationFixOrdering.accept` verglichen werden.

## Verbleibende Risiken und nächstes kleines Paket
1. `CurrentLocationRepository.isNewer` und zugehörige Tests lesen: Gibt es ein Fallback auf Wall-Clock-Zeit, und was geschieht bei ungültigen `elapsedRealtimeNanos`? Einheiten und Gleichstand dokumentieren.
2. Deterministische Regression **am tatsächlich verwendeten Akzeptanzpfad** ergänzen: zuerst Fix A, danach älterer Fix B (auch mit besserer Genauigkeit), danach Fix C mit gleichem Zeitstempel; A darf nicht überschrieben werden. Rotation und erneute Subscription getrennt prüfen.
3. Nur bei belegter Lücke eine einzige kanonische Vergleichslogik schaffen; keine zweite unabhängige Zustandsmaschine, die beim `DisposableEffect`-Restart ihren Zustand verliert. Bestehende `NavigationSessionViewModel.location` überlebt Rotation.
4. Separat ROUTE-02 bearbeiten: `WalkingNavigation.nextTurn` und `RouteProgress.project` bleiben zustandslos und können bei Haarnadeln trotz korrekt sortierter GPS-Fixes springen. Dafür ist eine routegebundene Hysterese mit Sequenztests nötig.

## Qualitätsstatus
Dies ist eine **Codepfad-Analyse**, keine GPS-Fehlerbehebung. Keine lokalen Gradle-/Emulator-Tests in diesem Lauf; kein CI-Nachweis für diesen Dokumentationscommit, keine APK-Installationsfreigabe. Frühere grüne CI-Läufe beziehen sich auf ältere Commits. Andere Chats (Main/Schedule) waren nicht vollständig live zugänglich; keine unbekannten Anforderungen übernommen.
