# Lifecycle-/GPS-Codebefund – 19.11.2026, 21:21 MEZ

**Status:** Statische Quellprüfung von `app/src/main/java/de/mvv/mobility/MainActivity.kt` auf `main` (HEAD vor dieser Dokumentation: `84bfac880bb9e1e70e95c378bc4e0da77e477195`). Keine Laufzeitreproduktion, kein bestandener Test. Zugehörige Issues: [#13](../issues/13) und [#14](../issues/14).

## Direkt nachprüfbare Beobachtungen

- `onCreate()` ruft `DebugResponseCapture.clear()` (Zeile 89) und `GpsDebugRecorder.clear()` (Zeile 92) auf. Bei einer Activity-Neuerstellung können **Debug-Aufzeichnungen** dadurch zurückgesetzt werden. Das beweist ausdrücklich **nicht**, dass der produktive GPS-Track auf demselben Weg gelöscht wird.
- `dashboardJourney` (Zeile 221), `walkingRoute` (222), `walkedTrack` (259) und `selectedTab` (423) sind mit Compose-`remember` gehalten. Ohne separate Wiederherstellung überstehen diese Werte eine reguläre Activity-Neuerstellung nicht zuverlässig. Das ist eine plausible, konkrete Ursache für den in #13 gemeldeten Verlust der aktiven Navigation.
- Der produktive Track wird bei akzeptierten Positionsupdates erweitert (Zeilen 392–402) und auf 1500 Punkte begrenzt. Seine Lebensdauer ist derzeit an die Composition gebunden. Die Begrenzung ist **kein** Beleg für dauerhafte Speicherung.
- Ein asynchrones Reroute-Ergebnis kann `walkingRoute` setzen (Zeilen 461–466). Vor einem Fix muss getestet werden, ob verspätete Antworten ältere Routen wiederherstellen können. Eine tatsächlich beobachtete Race Condition wird hier nicht behauptet.

## Kleinstes überprüfbares Arbeitspaket

1. Einen deterministischen Instrumentierungstest mit **nicht leerer** Navigation (gewählte Verbindung, OSRM-Polyline und drei akzeptierte GPS-Punkte) anlegen; Zustand vor/nach `ActivityScenario.recreate()` auf Modell- und UI-Ebene vergleichen. Ohne Fixture wäre ein Test mit leerem Startbildschirm nicht aussagekräftig.
2. Danach einen lifecycle-fähigen Navigation-State-Owner einführen. `rememberSaveable` ist für eine unbegrenzt wachsende GPS-Spur im Bundle ungeeignet. Geplante Route und akzeptierte Spur getrennt halten; Wiederherstellung bei Process Death gesondert definieren.
3. Mit Fixfolge A/B/C → GPS-Lücke → D sowie Reroute R1/R2 (R1 kommt zuletzt) testen: A/B/C bleiben erhalten, D wird genau einmal ergänzt, verspätete R1 überschreibt R2 nicht.
4. Zunächst gezielte JVM-Tests, dann Android-Instrumentierung auf Emulator und S25. Normale Push-CI führt den Emulatorjob laut `.github/workflows/android.yml` nicht aus; für die Abnahme `workflow_dispatch(test_level=release)` oder einen Tag-Lauf verwenden.

## CI-Blockade / Freigabe

Der zuletzt belegte Push-Lauf [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) hat einen fehlgeschlagenen `preflight`-Job; nachgelagerte Jobs wurden übersprungen. Die erste fehlerhafte Aktion ist mangels zugänglicher Logs nicht nachgewiesen. In einem Checkout des aktuellen HEAD zuerst getrennt ausführen:

```bash
python3 scripts/test-ci-concurrency.py
bash -x scripts/preflight.sh
python3 -u scripts/test-emulator-navigation.py
```

Exitcodes und erste Fehlerausgabe sichern, **danach** gezielt reparieren. Kein grüner Build und keine APK-Installationsfreigabe aus dieser statischen Prüfung ableiten.

Andere Projekt-Chats wurden nicht vollständig live gelesen; nur bereits bekannter Projektkontext zu Rotation, GPS-Aussetzern und Rerouting wurde berücksichtigt.
