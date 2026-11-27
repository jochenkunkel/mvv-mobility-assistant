# GPS-03 – Activity-Recreation und Standort-Akzeptanz: ausführbarer Prüfauftrag

Stand: 27.11.2026, 08:22 MEZ. Grundlage: `main` bei `a3cf98cf9b665dffdefed00f90f471cda1b53cfc`; `docs/GPS-02-CALLBACK-INTEGRATION-AUDIT-2026-11-27.md`, `docs/PROJECT_STATUS.md`, `MainActivity.kt` und `docs/CHAT-START.md`.

## Bestätigte Ausgangslage
- `MainActivity.onCreate` bezieht `NavigationTraceViewModel` und `NavigationSessionViewModel` über `ViewModelProvider(this)`; deren Lebensdauer über Konfigurationswechsel ist beabsichtigt, **nicht** über Prozessende.
- `CurrentLocationRepository.isNewer` wird laut GPS-02-Audit bereits im produktiven Foreground-Callback eingesetzt. `LocationFixOrdering` ist noch nicht angebunden; keine redundante zweite Prüfung ergänzen.
- Die zwei instrumentierten Zeitstempel-Regressionen in `d63de38` wurden laut CI-Nachweis für `24a19cc` mit einem vollständigen grünen sechs-Job-Lauf geprüft. Das ist **kein** Nachweis für Rotation mit aktiver Navigation.
- ROUTE-02 (zustandslose Projektion und Sprünge bei parallelen Segmenten) bleibt ein eigenständiges Problem.

## Kleines nächstes Implementierungspaket: GPS-03

Ein instrumentierter API-35-Test soll mit `ActivityScenario` und ausschließlich synthetischen Koordinaten arbeiten. Test-Fixture darf nur in debuggable Builds aktiv sein.

1. Navigation mit bekannter synthetischer Route und Ziel aktivieren; in der App sichtbaren aktuellen Tab, Fullscreen-Modus, geplante Route und GPS-Track vor Rotation erfassen.
2. Zwei gültige GPS-Fixes mit streng steigender monotone Zeit einspeisen; verifizieren, dass Track und Navigation fortschreiten.
3. `scenario.recreate()` ausführen; verifizieren, dass Ziel, Route, Tab/Fullscreen und bisheriger Track erhalten bleiben. Nicht lediglich den Screen-Text prüfen, sondern den zuständigen State-Owner bzw. Semantik-Tags.
4. Danach einen **älteren** Fix mit höherer Genauigkeit und einen **gleich alten** Fix einspeisen. Beide müssen verworfen werden; Track-Länge und letzte akzeptierte Position dürfen sich nicht ändern.
5. Einen wirklich neueren Fix einspeisen: genau eine Ergänzung des Tracks; kein Duplikat durch erneute Subscription.
6. Standortberechtigung verweigert/entfallen: keine Phantomposition, eindeutige UI-Warnung, Navigation ohne erfundenen Fortschritt.
7. Prozessneustart separat testen: **nicht** die Erhaltung komplexer Route/Track behaupten, sofern keine Persistenz implementiert ist.

## Akzeptanz und Diagnose
- Test schlägt auf alter fehlerhafter Implementierung zuverlässig fehl und ist ohne Netzwerk/MVV-Liveantwort reproduzierbar.
- Keine Änderung an Signing, Secrets oder Release-Freigaben.
- Bei nicht injizierbarem GPS-Callback zuerst einen schmalen testbaren Akzeptanz-Adapter extrahieren, ohne die bestehende `isNewer`-Semantik zu duplizieren.
- Nach Implementierung `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` (API 35), Lint und zugehörigen GitHub-Actions-Lauf mit exakt getestetem SHA dokumentieren. Ohne belegte Ausführung Status **unbekannt**.
- Nach GPS-03: ROUTE-02 Sequenz-Hysterese, die GPS-Jitter unterdrückt und echtes Zurückgehen zulässt; reale S25-Fußwegabnahme bleibt Pflicht.

## Chat- und Freigabegrenze
Andere vollständige Projekt-Chats (Main/Schedule) sind nicht direkt lesbar; nur bereits im Repository dokumentierte Entscheidungen und der hier bekannte Projektverlauf berücksichtigt. Keine APK-Installationsfreigabe aus dieser Planung.
