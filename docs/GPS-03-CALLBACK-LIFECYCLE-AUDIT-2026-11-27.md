# GPS-03 – Callback-Lifecycle: Quellcodebefund und umsetzbarer Test-Schnitt

Stand: 27.11.2026, 09:20 MEZ. Grundlage: `main` bei `5eb29b0`, `MainActivity.kt`, `CurrentLocationRepository.kt`, `docs/GPS-03-ROTATION-CALLBACK-ACCEPTANCE-2026-11-27.md`, Roadmap und Projektstatus.

## Tatsächlich im Code gesehen

- `MainActivity.onCreate` bezieht `NavigationTraceViewModel` und `NavigationSessionViewModel` über `ViewModelProvider(this)`. Das unterstützt Erhalt über Activity-Recreation; Prozessneustart ist eine andere Frage.
- `CurrentLocationRepository.observeForeground` registriert **einen** `LocationListener` für GPS und Netzwerk, sofern verfügbar, und gibt einen Stop-Callback zurück, der `removeUpdates(listener)` aufruft.
- Nach Registrierung wird gegebenenfalls ein frischer gecachter Fix unmittelbar über `onResult(cached)` geliefert. Somit können sowohl Cache- als auch Provider-Callbacks im selben Beobachtungszyklus auftreten. Das ist nicht per se ein Bug.
- `CurrentLocationRepository.isNewer` vergleicht monotone Zeitstempel strikt, alternativ Wall-Clock strikt, und lehnt gemischte Zeitbasen ab. Die bestehende Funktion soll nicht durch einen zweiten unabhängigen Guard dupliziert werden.

## Noch **nicht** nachgewiesen

Ob ein bereits in der Main-Looper-Queue befindlicher Callback nach STOP/Recreation die alte Session beeinflussen kann; ob der Cache-Fix nach Recreation als Duplikat in die Spur gelangt; und ob bei einem aktiven Route-/Dashboard-Zustand alle UI-Eigenschaften erhalten bleiben. Diese Aussagen benötigen reproduzierbare Instrumentierung, nicht nur Quellcodeinspektion.

## Nächstes kleines Codepaket (P0)

1. Vor dem Test den tatsächlichen `observeForeground`-Aufruf und dessen `DisposableEffect`/`LifecycleEventObserver` in `MainActivity` lokalisieren. Den vorhandenen `isNewer`-Guard und die ViewModel-Trackannahme im selben Datenpfad verfolgen.
2. Falls der Callback nicht kontrolliert injizierbar ist: einen **minimalen** debug-/testgebundenen Fix-Emitter am existierenden Akzeptanzpfad vorsehen; keine neue Produktions-Zeitstempelregel, keine globale Test-Hintertür im Release-Build.
3. Instrumentierter API-35-Test mit synthetischen Fixes: A/B akzeptieren; `ActivityScenario.recreate()`; gecachten B erneut zustellen (keine neue Spur); alten A zustellen (keine neue Spur); C zustellen (genau ein neuer Trackpunkt). Danach aktiven Navigationszustand und Vollbildmodus verifizieren.
4. STOP/START separat prüfen: Listener wird abgemeldet, neu registriert, alte Callback-Quelle kann keine nachträgliche Trackänderung auslösen. Berechtigungsentzug separat absichern.
5. Tests/CI nur als erfolgreich markieren, wenn der konkrete SHA, Lauf und tatsächliche Testausführung nachgewiesen sind. Reale S25-Abnahme bleibt zusätzlich erforderlich.

## Grenzen und Übergabe

Dies ist eine **Codeanalyse und ein präzisiertes Testpaket**, keine implementierte Fehlerbehebung. Keine lokalen Gradle-/Emulatortests ausgeführt. GitHub-Connector für commitbezogene Workflows liefert nur PR-getriggerte Läufe; Push-CI für `5eb29b0` damit nicht bestätigt. Keine APK-Installationsfreigabe. Andere vollständige Chats Main/Schedule sind nicht direkt abrufbar; nur hier bekannter und im Repository persistierter Kontext verwendet.
