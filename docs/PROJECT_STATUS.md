## Entwicklungsabgleich – 21.11.2026, 15:21 MEZ

- Repository vor Änderung: `8b82c75`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Implementierungsroadmap, ADRs, Status, Android-CI und Diagnoseworkflow geprüft. Keine neue verifizierte Anforderung aus fremden Chats; vollständiger Live-Zugriff auf Main/Schedule fehlt.
- **Kleines CI-Härtungspaket:** `preflight` hatte im Gegensatz zum separaten Diagnosejob kein Job-Timeout. In `.github/workflows/android.yml` wurde `timeout-minutes: 10` ergänzt (Commit `a455a2f`). Dies begrenzt Hänger, **behebt aber nicht** die unbekannte Ursache des bisherigen Preflight-Fehlschlags.
- **CI-Beleg:** Der erneut abgefragte Android-Lauf #452 (20.11.2026) zeigt `preflight=failure`, alle fünf Folgejobs `skipped`, keine Einzelschritte. Keine ausgeführten lokalen Tests; ein grüner Lauf für `a455a2f` ist nicht bestätigt. Keine APK-Freigabe.
- **Nächster Schritt:** Aktuellen Push-Run und unabhängige Diagnoseartefakte beschaffen, reale Exitcodes isolieren, erst dann Fehlerfix; danach #13 Rotation und #14 GPS-Track mit Emulator-Regressionstests.

# MeinWeg2 – zentraler Projektstatus

## Entwicklungsabgleich – 21.11.2026, 05:23 MEZ (Actions-API direkt)

- **HEAD:** `727c8269fe63cdc744d128585a4cc54540993e56`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Projektstatus, CI-Workflow und Diagnose-Runbook abgeglichen.
- **Wichtige neue Evidenz:** Die allgemeine GitHub-Actions-REST-Liste ist über den GitHub-Connector tatsächlich abrufbar. Der neueste geprüfte Android-Push-Lauf ist **[#452](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44752622034)**, gestartet 20.11.2026 14:21 UTC für HEAD `727c826`, **completed/failure**. Job `preflight` (ID `136344098872`) = **failure**, alle fünf abhängigen Jobs = **skipped**. Die Job-API liefert `steps: []`; keine Artefakte. Der direkte Joblog-Abruf ergab am 21.11.2026 04:23 UTC erneut **404 BlobNotFound**. Der einzelne fehlgeschlagene Befehl ist damit **nicht belegt**. Der bisher zitierte Lauf #439 ist **nicht mehr der neueste belegte Lauf**.
- **Blockade und nächster Schritt:** Die Diagnose-Workflow-Run-Liste unter `/actions/workflows/preflight-diagnostics.yml/runs` war über den Connector nicht abrufbar; daher keinen Diagnoselauf behaupten. Zunächst Workflow-ID über `/actions/workflows` ermitteln und Läufe über numerische ID abrufen. Wenn weiterhin keine brauchbaren Logs/Artefakte vorliegen, die drei Preflight-Kommandos im Checkout des HEAD separat mit Exitcodes ausführen und den konkreten Fehler beheben. Danach #13 (Activity-Recreation/Track) und #14 (GPS-Spur/Rerouting-Races) testen.
- **Freigabe:** Keine Tests in diesem Lauf ausgeführt; Android-CI rot, keine APK-Freigabe. Kein Signing-/Secrets-Eingriff. Andere Chats nicht vollständig live zugänglich.


**Stand:** 19.11.2026, 02:25 MEZ (Repository-Abgleich) · **Verbindliche Codebasis:** GitHub `main`

> **Startpunkt für alle Chats/Agenten:** Diese Datei zuerst lesen, anschließend
> `main`, offene GitHub-Issues und CI-Läufe live abgleichen. Diese Datei ist
> eine Übergabe und kein Ersatz für den tatsächlichen Repository-Status.


## Entwicklungsabgleich – 20.11.2026, 12:19 MEZ

- **Live-Repository:** HEAD vor der Änderung `e80fd9b`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Regressionstestmatrix, CI-Workflow und Diagnose-Runbook geprüft. Vollständige Live-Inhalte anderer Projekt-Chats nicht verfügbar; bekannte Main-/Schedule-Fehler zu Rotation, GPS-Spur und Rerouting bleiben maßgeblich.
- **Konkretes Testpaket:** `scripts/test-ci-concurrency.py` prüft jetzt zusätzlich den **unabhängigen** Diagnoseworkflow: manueller Dispatch, Trigger bei Skriptänderungen, alle drei separaten Checks, Timeout, Exitcode-Datei, Artefakt-Upload trotz Fehler und Aufbewahrungsdauer. Commits `868f906` (Guard) und `1280bab` (Korrektur der Regex-Escapes). Diese statischen Assertions schützen vor stillen Diagnose-Regressionen; sie beweisen **keine** tatsächliche Workflow-Ausführung.
- **CI-Beleg:** Run [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) weiterhin `preflight=failure`, fünf Jobs übersprungen. Erneuter Logabruf: 404 `BlobNotFound` (20.11.2026, 12:19 MEZ). Keine Artefakte in #439; commitbezogene PR-Run-Abfrage für `868f906` leer, damit Push-CI **unbekannt**. Die neue Testdatei wurde noch nicht in einer ausführbaren Checkout-Umgebung getestet.
- **Nächste Priorität:** Diagnoseworkflow-Lauf/Artefakte aus Actions beschaffen, den ersten echten Fehler beheben und den neuen statischen Guard ausführen. Anschließend #13 Activity-Recreation-Test mit nichtleerem Track, dann #14 Track-/Reroute-Races. Keine APK-Freigabe.

## Entwicklungsabgleich – 20.11.2026, 08:18 MEZ

- **HEAD vor diesem Eintrag:** [`4e34c09`](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/4e34c099bfb4c4563175982a806a19bc51637c8d). GitHub-Commit-Suche: keine neueren Commits; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Rotation-/GPS-Testmatrix, Android-Workflow und Diagnoseworkflow erneut gelesen.
- **Präzisierung des Diagnose-Gates:** Der unabhängige Diagnoseworkflow wird bei Pushes auf `main` nur durch Änderungen an `.github/workflows/preflight-diagnostics.yml`, `.github/workflows/android.yml` oder `scripts/**` ausgelöst; der jüngste reine Runbook-Commit `4e34c09` löst ihn **nicht** aus. Ein früherer Trigger-Commit `ee0e59e` ist vorhanden, aber ein tatsächlicher Diagnose-Lauf ist hier nicht nachgewiesen. Der verfügbare GitHub-Connector stellt in diesem Lauf keine allgemeine Actions-Run-Liste oder Workflow-Dispatch-Aktion bereit; eine Commit-Run-Abfrage ist auf PR-Ereignisse beschränkt. Daher **keine Aussage über einen aktuellen Diagnose-Lauf oder grünen Push-Build**.
- **Nächste ausführbare Aktion:** Neueste [Diagnose-Läufe](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/preflight-diagnostics.yml) über die Actions-Webansicht/geeignete API abrufen. Falls keiner existiert, den Diagnoseworkflow manuell starten. `results.txt` und `check-N.log` lesen, erste nichtnull Exitcode-Ursache isolieren, gezielt beheben. Keine erneuten ungezielten CI-Dokumentationscommits als Ersatz für die Diagnose.
- **Danach:** #13 instrumentierten `ActivityScenario.recreate()`-Test mit nichtleerer Navigation/GPS-Fixture implementieren und im Emulator ausführen; #14 GPS-Track/Reroute-Race-Regression. Keine Signing-/Secret-Änderung, keine APK-Freigabe. Bekannte Main-/Schedule-Meldungen bleiben berücksichtigt; vollständiger Live-Zugriff auf fremde Chatverläufe nicht nachgewiesen.

## Entwicklungsabgleich – 20.11.2026, 02:22 MEZ

- **Repository:** GitHub-Commit-Suche: letzter Commit `2596a249f3855354a3938bbf64d7dc54ff9c841a`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Lifecycle-/GPS-Audit und CI-Workflow samt drei Preflight-Skripten erneut gelesen. Keine neue belegte Anforderung aus vollständig zugänglichen Projekt-Chats; Chat-Live-Synchronisation nicht verfügbar.
- **CI:** Erneut direkt abgefragter [Push-Lauf #439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530): `preflight=failure`, sämtliche Folgejobs `skipped`. Der Connector liefert `steps:null` und `logs_url:null`; **kein einzelner fehlerhafter Befehl identifizierbar**. Der Workflow enthält drei getrennte Preflight-Steps und einen `if: failure()`-Diagnoseschritt. Die vorhandenen Prüfskripte wurden statisch kontrolliert; das ist kein Ausführungsnachweis.
- **Präzisierter Diagnosepfad:** Zuerst in GitHub Actions den Job auf Runner-/Checkout-Startfehler, fehlende Runner-Zuteilung oder Ausführungsfehler untersuchen; diese Möglichkeiten sind **Hypothesen, keine Befunde**. Wenn Steps/Logs weiter fehlen, in einem erreichbaren Checkout exakt `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` einzeln ausführen und Exitcode + stderr sichern. Bei vollständig fehlender Jobinitialisierung zunächst Actions-/Runner-Zugriff klären, nicht wahllos Python/Android-Code verändern.
- **Anschließend:** #13 mit nichtleerer `ActivityScenario.recreate()`-Fixture, #14 mit GPS-Fix-Lücke und verspätetem Reroute-Ergebnis. Normale Push-CI reicht für Instrumentierung nicht aus; Emulator-Release-Test erforderlich. **Kein neuer grüner Build, keine APK-Freigabe.**

## Entwicklungsabgleich – 19.11.2026, 22:21 MEZ

- **Verifizierter letzter Commit auf main:** [`32a0f09`](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/32a0f09d17195074c48f8e5eb7285320e4e3b645). Der neue [Lifecycle-/GPS-Codebefund](LIFECYCLE-GPS-CODE-AUDIT-2026-11-19.md) ist jetzt die konkrete Arbeitsgrundlage für #13 (Rotation) und #14 (Spurverlust). Die Diagnose trennt produktiven `remember`-Navigationszustand von separat zurückgesetzten GPS-Debugdaten; sie ist noch **kein Laufzeitnachweis**.
- **Offene Arbeit:** elf Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap und Architekturentscheidungen abgeglichen; historische Roadmap-Phasen sind nicht als aktuelle Abnahme zu lesen. Andere Projekt-Chats sind nicht vollständig live zugänglich.
- **CI-Beleg:** Push-Lauf [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) mit `preflight=failure`, Folgejobs übersprungen; Step-Abfrage leer und Joblogs nicht abrufbar. Für `32a0f09` liegt hier kein bestätigter grüner Push-Lauf vor. Kein Test-/APK-Freigabenachweis.
- **Nächste Implementierung:** zuerst Preflight-Exitcode durch getrennte lokale Ausführung der drei dokumentierten Befehle isolieren. Parallel als kleines, testbares Paket für #13 einen instrumentierten `ActivityScenario.recreate()`-Test mit **nichtleerem** Navigations-/GPS-Fixture anlegen; danach State-Owner-Fix. Für #14 GPS-Fix-Lücke und verspätete Reroute-Antworten getrennt prüfen. Keine ungetestete Änderung an Signing oder Secrets.

## Entwicklungsabgleich – 19.11.2026, 23:20 MEZ

- **HEAD verifiziert:** `e185c1141e07c2a9d45d2482893f653b1a14b9bb` (vorher `32a0f09`), keine neueren Commits in der Repository-Suche. Der vorherige 22:21-Eintrag nennt noch den Parent-Commit und ist insofern historisch; `e185c11` hat diesen Status bereits ergänzt.
- **Issues/PRs:** weiterhin elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, ADRs, CI-Workflow, drei Preflight-Skripte und Lifecycle-/GPS-Audit erneut gegen `main` abgeglichen.
- **Konkrete CI-Diagnose aus Quellprüfung:** `scripts/test-ci-concurrency.py` enthält nicht nur statische Guards, sondern eine verhaltensbasierte Release-Gate-Matrix mit **52 Fällen**. Die drei unabhängigen Preflight-Kommandos sind im Workflow vorhanden. Das ist **kein Ausführungsnachweis**; der bisherige fehlgeschlagene Push-Lauf #439 bleibt die letzte hier belegte Jobdiagnose. `fetch_commit_workflow_runs` erfasst nur PR-Events und kann den Push-Status von `e185c11` nicht bestätigen.
- **Kritische Test-Grenze:** Ein normaler Push führt `connectedDebugAndroidTest` nicht aus. Ein `ActivityScenario.recreate()`-Test für #13 darf erst nach einem echten Emulatorlauf über manuelles `test_level=release` oder Versionstag als bestanden gelten. #14 zusätzlich mit GPS-Fix-Lücke und verspäteter Reroute-Antwort prüfen.
- **Nächster ausführbarer Schritt:** Mit erreichbarem Checkout die drei Preflight-Kommandos einzeln ausführen, Exitcodes sichern und den ersten Fehler beheben; danach nichtleere Rotation-Fixture implementieren. Keine Änderungen an Signing/Secrets, kein grüner Build oder APK-Freigabe ohne Nachweis. Andere Projekt-Chats sind nicht vollständig live lesbar.

## CI-Folgeprüfung – 19.11.2026, 09:25 MEZ

- Der nach Commit `b3cefa5` gestartete [Push-Lauf #439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) ist ebenfalls **completed/failure**: `preflight` Job-ID `135404600838` fehlgeschlagen; `build-and-test`, Emulator und Live-Probes wurden übersprungen. Dies bestätigt eine weiterhin bestehende CI-Blockade, **nicht** die Ursache.
- Erneute Abfrage der Job-Schritte: `steps: []`. Joblog-Download: HTTP 404 `BlobNotFound` (19.11.2026, 09:25 MEZ). Die erste fehlerhafte Aktion ist weiterhin unbekannt; weder Checkout- noch Skriptfehler sind damit bewiesen.
- Statische Quellprüfung: `scripts/test-ci-concurrency.py`, `scripts/preflight.sh`, `scripts/test-emulator-navigation.py` und die Workflow-Definition sind vorhanden. Die Prüfschritte dürfen nicht als bestanden gelten, da keine Ausführung nachgewiesen ist.
- **Nächster ausführbarer Schritt:** Den Checkout des aktuellen HEAD in einer Umgebung mit GitHub-Zugriff herstellen und die drei Preflight-Kommandos separat mit Exitcode ausführen. Falls GitHub-Actions-Webansicht zugänglich wird, den **ersten fehlgeschlagenen Step** von Run #439 und dessen Annotation auslesen. Nur danach gezielt fixen und Push-CI erneut prüfen. Keine Signing-Änderung und keine APK-Freigabe.

## CI-Befund – 19.11.2026, 08:17 MEZ (verifiziert)

- **HEAD vor diesem Eintrag:** `1a2a2bc5e7a425ae3107ac48ce5e1c2a53f677f4`. GitHub Actions **Push-Lauf #438**, Run-ID `44471413554`, vom 19.11.2026 07:20 MEZ: **completed/failure**. Beleg: https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44471413554 . Damit ist die frühere Aussage „Push-Läufe nicht einsehbar“ überholt: Die Actions-Run-Liste ist über den GitHub-Connector erreichbar.
- **Jobs:** `preflight` (Job-ID `135381540317`) **failure**; `build-and-test`, `emulator-smoke-test`, `mvv-live-probe`, `door-to-door-live-probe` und `release-acceptance` **skipped**. Der Workflow ist also tatsächlich rot, nicht nur unbestätigt. Aus den übersprungenen Jobs kann keine APK-Freigabe abgeleitet werden.
- **Diagnosegrenze:** GitHub-Job-Schritt-Abfrage liefert `steps: []`; Joblog-Abruf scheitert mit HTTP 404 / `BlobNotFound`. Welcher einzelne Preflight-Schritt fehlschlug, ist **nicht** nachgewiesen. Der Lauf wurde rund drei Sekunden nach Start abgeschlossen; daraus allein keine Ursache ableiten. Keine blinde Änderung an Produktivcode oder CI.
- **Nächster konkreter Schritt:** Über die GitHub-Actions-Weboberfläche Run #438 / Job `preflight` einschließlich Step Summary und Checks-Annotationen öffnen; bei weiterhin fehlenden Logs einen neuen manuellen Diagnose-Lauf mit zugänglichen Logs auslösen. Alternativ den exakten HEAD auschecken und `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` einzeln mit Exitcode ausführen. **Erst den ersten fehlgeschlagenen Befehl isolieren, dann reparieren und erneut CI prüfen.** #13/#14 bleiben danach die nächsten Codeprioritäten.
- **Chat-Abgleich:** Bekannte Main-/Schedule-Meldungen zu Rotation, GPS-Spur und Rerouting bleiben relevant; kein vollständiger Live-Zugriff auf alle Chatnachrichten behauptet.

## Automatischer Entwicklungsabgleich – 19.11.2026, 07:20 MEZ

- **Live-Abgleich:** GitHub-Suche meldet HEAD `bc654330be22094bd6d95e8ded4351d9296a0171` (Testvertrag), weiterhin elf offene Issues (#3–#11, #13, #14). README, `docs/IMPLEMENTATION-ROADMAP.md`, `docs/DECISIONS.md`, `docs/REGRESSION-ROTATION-GPS.md`, `MainActivity.kt` und `.github/workflows/android.yml` erneut gelesen. Die GitHub-Commit-Status-Abfrage für `bc65433` lieferte `statuses: []`; daraus folgt **keine** Aussage über den tatsächlichen Push-Actions-Lauf.
- **Neu präzisierter CI-Befund:** Der normale Push durchläuft `preflight` und `build-and-test` mit JVM-Tests, Debug-APK und Lint. Der Job `emulator-smoke-test` einschließlich `:app:connectedDebugAndroidTest` läuft **nur** bei einem Tag `v*` oder manuellem `workflow_dispatch` mit `test_level=release`. Ein erfolgreicher gewöhnlicher Push könnte deshalb den geforderten `ActivityScenario.recreate()`-Instrumentierungstest **nicht** bestätigen. Dies ist ein konkreter Nachweis-/Gate-Unterschied, kein beobachteter Testfehler.
- **Arbeitspaket für #13:** Erst einen isolierten Android-Instrumentierungstest mit `ActivityScenario.recreate()` und deterministischer Navigation-Fixture implementieren; dann mit `workflow_dispatch(test_level=release)` oder lokalem Emulator tatsächlich ausführen. Vorhandene `remember`-Zustände sind nach Activity-Recreation nicht garantiert erhalten. Danach Lifecycle-StateHolder und erneute Tests. **Nicht** auf eine gewöhnliche Push-CI als ausreichende Abnahme verweisen.
- **Arbeitspaket für #14:** GPS-Track als unabhängige Zustandsquelle von OSRM-Route und UI-Layer testen; A/B/C → Fix-Lücke → D, Reroute und stale Callback. Erst mit bestandenem Emulatorlauf und realem S25-Nachweis schließen.
- **Ausführungsgrenze dieses Laufs:** Kein Android-SDK-/Emulator-Testlauf und keine vollständige Actions-Push-Run-Liste verfügbar; daher kein grüner Build, keine APK-Installationsfreigabe. Keine neuen, vollständig verifizierten Anforderungen aus anderen Projekt-Chats; zugänglicher Verlauf deckt Rotation, GPS-Aussetzer und Rerouting ab.

## Automatischer Abgleich – 19.11.2026, 01:17 MEZ

- **Verifizierter HEAD vor diesem Eintrag:** `d931e1bc2db3fdcc2a4934b2ebe50e35dc57cb92` (neue Offline-Testsuite für Emulator-Navigationsziele). Die neue Suite `scripts/test-emulator-navigation.py` wurde im Repository gelesen: 18 Testmethoden; sie wird im `preflight`-Job nach Shell-/Python-Syntaxprüfung ausgeführt. **18 vorhandene Tests sind keine 18 bestandenen Tests.**
- **Live-Issues:** #3–#11 sowie #13 und #14 (11 offene Tickets). #13 und #14 bleiben ohne nachgewiesene Lifecycle-/Track-Regressionstests offen.
- **CI-Beleggrenze:** Die Commit-Status-Abfrage für `d931e1b` lieferte `statuses: []`. Dies ist **kein** grüner Build; der Connector für commitbezogene Workflow-Läufe berücksichtigt nur PR-Ereignisse, nicht zuverlässig Push-Läufe. Aktuelle Push-Joblogs und tatsächliche Preflight-Ausgabe fehlen. Keine APK-Installationsfreigabe.
- **Konkreter nächster Schritt (ausführbar):** `python3 -u scripts/test-emulator-navigation.py`, `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` in einem Checkout von genau `d931e1b` ausführen; Exitcodes und erste fehlerhafte Zeile festhalten. Anschließend Push-Actions-Lauf für diesen SHA über die Actions-Run-Liste (nicht PR-Commit-Lookup) prüfen. Erst danach gezielte Reparatur statt weiterer rein dokumentarischer Änderungen.
- **Chat-Grenze:** Die bekannten Anforderungen aus Main/Schedule zu GPS-Aussetzern, Rerouting, Rotation und Trackdarstellung sind bereits in #13/#14 und `docs/REGRESSION-ROTATION-GPS.md` enthalten. Kein vollständiger Live-Zugriff auf alle Projekt-Chats nachgewiesen.

## Automatischer Abgleich – 19.11.2026, 02:25 MEZ

- **HEAD:** `b35bc1b435a9fa1bcb97f97faa725dfd227156e4`; Issue-Suche bestätigt #3–#11, #13, #14 (11 offene). Die aktuelle CI-Konfiguration hat drei separate Preflight-Schritte und eine diagnostische Fehlerzusammenfassung.
- **Gezielte Quellprüfung:** `scripts/test-emulator-navigation.py` enthält 18 `unittest`-Methoden; `scripts/tap-emulator-navigation.py` berücksichtigt die Sichtbarkeit und Aktivierung der gesamten Vorfahrenkette und verwirft ungültige Rechtecke. Dies ist eine **statische Codeprüfung**, kein Testlauf.
- **Reproduzierbare Infrastrukturblockade:** `git ls-remote https://github.com/jochenkunkel/mvv-mobility-assistant.git HEAD` im Ausführungscontainer scheiterte mit `Could not resolve host: github.com` (Exit 128). Damit kein Checkout und keine lokalen Ausführungen von Preflight, Gradle oder Tests. Die GitHub-Connector-Abfrage erlaubt zwar Dateilesen, bietet hier aber keine verlässliche Liste der Push-Workflow-Runs; der konkrete erste fehlgeschlagene Preflight-Befehl ist weiterhin **nicht belegt**.
- **Priorisierte Reparatur:** (1) Sobald DNS/Checkout funktioniert, exakt `b35bc1b` auschecken, `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` mit Exitcodes ausführen. (2) Für #13 Activity-/Navigation-State-Eigentümer identifizieren und `ActivityScenario.recreate()`-Regression anlegen. (3) Für #14 GPS-Track unabhängig von der OSRM-Route mit synthetischen Fix-Lücken und Reroute prüfen. **Kein ungetesteter Produktivcode-Fix und keine APK-Freigabe.**
- **Projekt-Chats:** Bekannte Anforderungen aus Main/Schedule sind durch #13/#14 und `docs/REGRESSION-ROTATION-GPS.md` abgedeckt; keine vollständige Live-Synchronisierung behauptet.

## Automatischer Entwicklungsabgleich – 19.11.2026, 04:19 MEZ

- **Codebefund für #13/#14 (statisch verifiziert):** `MainActivity.kt` hält `journeys` (Zeile 219), `dashboardJourney` (221), `walkingRoute` (222), `walkedTrack` (259) und `selectedTab` (423) ausschließlich in `remember`-Zustand. Diese Zustände überstehen eine reguläre Activity-Neuerstellung durch Bildschirmrotation nicht. Der Track wird als `mutableStateListOf` ebenfalls neu initialisiert. Das erklärt den gemeldeten Rotationsverlust auf Codeebene; ein Gerätetest steht aus. `walkingRoute` und `walkedTrack` sind außerdem unterschiedliche Zustände; die tatsächliche UI-Zeichnung und ihre Lebensdauer müssen separat geprüft werden.
- **Konkretes nächstes Arbeitspaket:** Zunächst einen instrumentierten `ActivityScenario.recreate()`-Test für aktive Navigation mit Route und Track schreiben (rot vor Fix). Danach Zustand in einen Activity-übergreifenden ViewModel-StateHolder auslagern; einfache Eingaben mit `rememberSaveable` sichern, komplexe Routen-/Trackobjekte nicht blind in Bundle serialisieren. Danach GPS-Aussetzer, Rerouting und die sichtbare Pfadlinie getrennt testen. Kein ungeprüfter Umbau von `MainActivity`.
- **CI-Grenze:** Die 18 Emulator-Navigationstests sind im Quellcode vorhanden, aber nicht in diesem Lauf ausgeführt. Push-Actions-Status und APK bleiben ungeprüft. Kein grüner Build behauptet.
- **Chats:** Die aus dem zugänglichen Verlauf bekannten Meldungen zu Rotation und dunkellila Track entsprechen #13/#14; kein vollständiger Live-Zugriff auf alle anderen Projekt-Chats.

## Ziel und Arbeitsweise

Android-Mobilitätsassistent für München mit Kalender-/GPS-Start, MVV-Verbindungen,
Fahrt-Dashboard und OSM-Fußwegnavigation. Der Nutzer wünscht **schnelle,
kleine Commits direkt nach `main`**. Trotzdem gilt: Nicht bestandene oder
nicht ausgeführte Tests dürfen nicht als bestanden gemeldet werden. Tickets
erst schließen, wenn alle Abnahmekriterien erfüllt sind.

**Chat-übergreifende Koordination:** GitHub ist der gemeinsame persistente
Bezugspunkt. Andere Chats können ihre eigenen Kontexte und Arbeiten haben;
eine automatische vollständige Echtzeit-Synchronisierung der Chatverläufe
existiert nicht. Jeder Agent muss vor Änderungen die aktuellen Dateien
und Issues lesen, Konflikte vermeiden und anschließend diesen Status
aktualisieren. Keine Behauptung, andere Chats seien vollständig synchronisiert.

## Zuletzt nach `main` geschriebene Änderungen

| Commit | Bereich | Status |
| --- | --- | --- |
| `f38ef6e` | Routengebundene Auswahl der nächsten Abbiegung; Regressionstests | implementiert, Geräteabnahme offen |
| `5a40a25` | Minikartenrichtung über Segmentprojektion; Tests | Teilfix, Issue #3 offen |
| `158a151` | Historische Roadmap als Snapshot gekennzeichnet | Teilfix, Issue #10 offen |
| `fe7ee3b` | Android-Lint auf jeden Push/PR erweitert | Teilfix, Issue #8 offen |
| `206e549` | Monotone GPS-Freshness für Cache-Fixes; Tests | Teilfix, Issue #6 offen |
| `ea479c8` | Roadmap um Arbeitsstand und Chat-Übergabe ergänzt | Dokumentation aktualisiert |

**Prüfgrenze:** Diese Commit-IDs sind dokumentierte Änderungen, keine
Aussage über einen vollständigen erfolgreichen CI-Lauf. Der letzte hier
dokumentierte Audit-Ausgangspunkt ist `1d185a85`; ältere Prüfberichte
sind datierte Befunde, keine aktuelle Build-Freigabe.

## Ticketstand beim Abgleich am 16.11.2026

**Geschlossen:** #2 NAV-01 (Codefix integriert; reale Navigationsabnahme
weiterhin wichtig).

**Offen (11; Abgleich 18.11.2026):**

| Ticket | Thema | Nächste konkrete Arbeit |
| --- | --- | --- |
| #3 NAV-02 | Minikarte/Kamera | Orientierung und Off-Route-Verhalten auf Gerät prüfen |
| #4 NAV-03 | Fußweg zur gewählten Einstiegshaltestelle | MVV-Etappenziel-ID/Koordinaten statt Geocoding nach Namen nutzen |
| #5 TIME-01 | Datum/Zeitzone | Absolute Zeitstempel für Verbindungen und Mitternacht/DST-Tests |
| #6 LOC-01 | GPS | Monotone Freshness in allen Aufrufern, Reihenfolge und Replay testen |
| #7 ARCH-01 | MainActivity | Zustandslogik schrittweise in testbare Komponenten auslagern |
| #8 CI-01 | CI-Gates | Lint-Ergebnisse prüfen, Emulator/Live-Probes für Release absichern |
| #9 TEST-01 | UI-Qualität | Golden Screenshots und Geräte-Matrix einführen |
| #10 DOC-01 | Dokumentation | Weitere Statusdateien und Aussagen gegen Code/CI abgleichen |
| #11 PRIV-01 | OSM/Datenschutz | Nutzungsrichtlinien, Attribution, Standortübertragung dokumentieren |
| #13 BUG | Bildschirmrotation | Aktive Route, Konfiguration und Navigation bei Activity-Recreation erhalten; instrumentierten Test ergänzen |
| #14 BUG | GPS-Trackverlust | Akzeptierte GPS-Spur bei Aussetzern, Rerouting und Rotation erhalten; deterministische Regressionstests ergänzen |

Issue-Übersicht: https://github.com/jochenkunkel/mvv-mobility-assistant/issues

## Nächste Schritte (priorisiert)

1. Aktuellen Push-Actions-Lauf von `main` samt Preflight-Einzelschritten verifizieren; ersten tatsächlich fehlgeschlagenen Befehl reproduzieren und reparieren. Die commitbezogene Connector-Abfrage erfasst nur PR-Läufe.
2. #13 und #14: Regressionstests und anschließend gezielte Lifecycle-/GPS-Track-Fixes gemäß `docs/REGRESSION-ROTATION-GPS.md`.
3. #4: Einstiegshaltestelle anhand der tatsächlich ausgewählten MVV-Etappe
   bestimmen; Geocoding-Fallback nicht als exakte Haltestelle ausgeben.
4. #5 und #6: Zeit-/GPS-Korrektheit einschließlich Randfalltests.
5. #3 und #9: Minikarten-Ausrichtung und visuelle Geräte-Abnahme.
6. #8, #7, #11, #10: CI, Architektur, Datenschutz und Dokumentation.

## Abnahme und Aktualisierungsprotokoll

Für jede Änderung: **Commit/Dateien**, **Testbefehl + echtes Ergebnis**,
**Issue-Status**, **bekannte Risiken** und **nächster Schritt** festhalten.
Keine erfundenen CI-Ergebnisse oder pauschale „fertig“-Meldung.

**Historischer Eintrag (16.11.2026):** Der damalige Abgleich erfasste neun offene Issues. Dieser Wert ist überholt; beim erneuten Abgleich am 18.11.2026 waren elf Issues offen (siehe aktuelle Tickettabelle oben). Alte Statusabschnitte sind datierte Momentaufnahmen und dürfen nicht als aktuelle Freigabe oder aktueller Issue-Zähler interpretiert werden.

## GPS-Reparaturrunde – 16.11.2026

- Commits `fb2e5e7`, `aa01800`, `dff574f`, `db0ab74` direkt nach `main`.
- `CurrentLocationRepository`: Alter von GPS-Fixes bevorzugt über `elapsedRealtimeNanos`, Wall-Clock nur als Fallback; Reihenfolge nach monotonic timestamp, Cache-Fix-Auswahl nach tatsächlichem Alter.
- `MainActivity`: Navigation-Freshness, Status und GPS-Debug-Heartbeat verwenden die gemeinsame Altersprüfung statt eigener Wall-Clock-Vergleiche.
- `LocationFreshnessTest`: zusätzliche Grenz-/Rücksprungtests.
- **Verifikation:** CI-Lauf für HEAD prüfen; kein S25-Praxistest, kein GPS-Replay und keine visuelle Freigabe behauptet. Issue #6 bleibt offen bis zur Geräteabnahme und weiteren GPS-Zustandstests.
- **Nächster Schritt:** CI prüfen, GPS-Debug-Export im realen Fußweg testen, Schwankungen/Accuracy/Provider-Wechsel anhand Log reproduzieren und danach gezielt korrigieren.


## Automatischer Entwicklungsabgleich – 16.11.2026, 12:33 MEZ

- **Neu seit dem bisherigen Status:** Commits `a6734ef` (alle MVV-/Fußweg-Etappen in Verbindungskarten), `2db9829` (OSM-Minimap im Fahrt-Dashboard), `8369fbd` (Ankunft nur in tatsächlicher Zielnähe) und `6ad05ee` (Regressionstests gegen verfrühte Ankunft) wurden auf `main` vorgefunden. Die Änderungen sind im Repository vorhanden; keine reale S25-Abnahme behauptet.
- **Eigene Arbeit:** `49f471c` ergänzt zwei Offline-Regressionstests in `WalkingNavigationTest.kt`: OSRM-GeoJSON-Koordinaten werden korrekt von [Längengrad, Breitengrad] in die appinterne [Breitengrad, Längengrad]-Reihenfolge übernommen; eine Route ohne Manöverschritte wird abgewiesen. Produktivcode blieb unverändert.
- **CI-Nachweis:** Die GitHub-Schnittstelle liefert für den zuletzt abgefragten Commit `6ad05ee` keine Status-Checks und keine Workflow-Läufe. Die Workflow-Run-Abfrage deckt PR-Läufe ab und ist kein verlässlicher Nachweis für Push-Läufe. Neue Tests wurden in diesem Durchlauf nicht lokal ausgeführt; für `49f471c` steht CI-Verifikation aus. **Keine APK-Freigabe.**
- **Projekt-Chats:** Andere Chats sind in diesem Lauf nicht vollständig abrufbar; die aus dem sichtbaren Projektkontext bekannten Anforderungen zu verfrühter Ankunft, GPS-Stabilität, Minimap und dynamischem Puffer sind berücksichtigt. Keine vollständige Chat-Synchronisierung behauptet.
- **Nächste Priorität:** #4 exakte Einstiegshaltestelle aus MVV-Etappendaten statt Namens-Geocoding; dazu vorhandene Modelle und Test-Fixtures prüfen, dann minimalen Fix und Regressionstests. Parallel den tatsächlichen Actions-Lauf und Gerätetest für die neuesten Navigationsänderungen nachweisen.

## Automatischer Entwicklungsabgleich – 16.11.2026, 13:33 MEZ

- **Repo-Abgleich:** Neuester vor diesem Lauf sichtbarer Commit `8ba8ace`; Issue #4 (MVV-Einstiegshaltestelle) und weitere neun offene Tickets bleiben nach Issue-Suche relevant. Der bisherige Status dokumentierte zuletzt die beiden OSRM-Parser-Tests `49f471c`.
- **Eigene Änderung:** `624f99e` erweitert `WalkingNavigationTest.kt` um einen Offline-Regressionstest für OSRM-Antworten mit mehreren Legs: Reihenfolge aller vier Manöver, Schrittentfernungen, Ziel-Manöver und Koordinaten. Keine Änderung an Produktivcode oder Routing-API.
- **Testgrenze:** Test ist im Repository committed, aber in diesem Lauf weder lokal ausgeführt noch durch einen erfolgreichen GitHub-Actions-Lauf nachgewiesen. Push-Läufe werden von der verfügbaren Commit-Workflow-Abfrage nicht vollständig geliefert. **Keine APK-/Gerätefreigabe.**
- **Issue #4 – konkrete Ursache:** `WalkingNavigation.fetch` nimmt weiterhin `stopName: String`, geokodiert den Namen und wählt `matches.firstOrNull()`. `JourneyStep` enthält derzeit nur Namen/Zeiten, keine eindeutige Stop-ID oder Koordinaten. Die saubere Korrektur erfordert das Durchreichen der tatsächlichen MVV-Einstiegskoordinaten über Parser, Modell und Aufrufer; keine unbewiesene Geocoder-Heuristik einbauen.
- **Projekt-Chats:** Andere Chatverläufe waren nicht vollständig abrufbar. Der dokumentierte Repository-Kontext bleibt der verlässliche gemeinsame Stand.
- **Nächster Schritt:** MVV-EFA-Fixture und Parser-Endpunktfelder auf echte Stop-IDs/Koordinaten prüfen; anschließend einen kleinen, testbaren Modell-/Parser-Schritt für Issue #4 umsetzen und zugehörige CI prüfen.


## Koordinatenbasierte Fußnavigation – 16.11.2026, 15 Uhr MEZ

- **In `main` implementiert:** `WalkingNavigation.fetch(context, origin, name, coordinates, callback)` priorisiert vorhandene Koordinaten und löst Namen ausschließlich bei fehlenden Koordinaten auf. Mehrdeutige Geocoder-Ergebnisse werden abgelehnt.
- **Sämtliche sechs produktiven Fußnavigations-Aufrufstellen** in `MainActivity.kt` sind auf die gemeinsame Koordinaten-zuerst-Strategie umgestellt (vier direkte Alternativ-/Fallback-Routen, Fußweg nach dem Aussteigen, Fußweg zum Einstieg).
- **Bestätigte Adresskoordinate:** `MvvJourneyRepository.searchAddress` gibt die bereits geocodierte Zielkoordinate mit dem Verbindungsergebnis zurück; die Activity bewahrt sie mit dem zugehörigen Zieltext auf und nutzt sie bei weiteren Fußwegen desselben Ziels. Bei Zieländerung wird sie verworfen. Persistenz über App-Neustarts ist noch nicht umgesetzt.
- **Nach dem Aussteigen:** GPS bleibt Start; Ziel ist die gespeicherte bestätigte Adresse, sonst die MVV-Koordinate des letzten Fahrtabschnitts, sonst der Name als markierter Rückfall. Die Zielkoordinate des letzten Abschnitts kann eine Haltestelle sein und ersetzt keine ausdrücklich angegebene Zieladresse.
- **Unsicherheitskennzeichnung:** Namens-Geocoding erzeugt `estimatedDestination=true` und zeigt einen Warnhinweis in der OSM-Fußwegansicht. Fehlende MVV-Koordinaten führen nicht mehr automatisch zum deaktivierten Einstiegsbutton.
- **Offene Validierung:** GitHub Actions für Commit `02161d3` ist bei Dokumentation noch nicht erfolgreich bestätigt. Echte EFA-Stop-Koordinaten und einzelne Bussteige Fürstenried West müssen mit Live-Antwort und vor Ort geprüft werden. Keine APK-/Gerätefreigabe ohne erfolgreichen Build.


## Automatischer Entwicklungsabgleich – 16.11.2026, 16:33 MEZ

- **Neuester geprüfter `main`-Commit:** `685f7cee` (dauerhafte Darstellung des zurückgelegten GPS-Pfads getrennt von der geplanten Route). Weitere neue Commits: `9416b360` (angenommener GPS-Track unabhängig von Routen-Neuberechnung), `61b13706` (Off-Route-Evidenz bei GPS-Lücken), `205c5475` (Fußweg-Alternative in Verbindungskarten), `2bc51f59` (Satellitenstatus und Stummschaltung), `02028d0e` (präzise GPS-Fixes vor groben Netzwerk-Fixes), `9b4005da` (Standortmarker bei GPS-Ausfall), `08b641c9` (Neuberechnung nach bestätigter Routenabweichung). Diese Angaben sind **Commit-Nachweise**, keine Geräte- oder Funktionsabnahme.
- **Offene Qualitätsrisiken:** Zusammenspiel aus Rerouting, dauerhaftem Lauf-Track und GPS-Fix-Filterung benötigt Regressionstests für (1) GPS-Lücke, (2) groben Netzwerk-Fix, (3) Off-Route-Bestätigung, (4) Neuberechnung ohne Trackverlust. Besonders zu prüfen: ob Rerouting auf die tatsächlich gewählte MVV-Haltestelle statt auf einen Namens-Treffer führt (Issue #4).
- **CI-Abfrage:** Für `685f7cee` liefert die verfügbare Commit-Status-Abfrage `statuses: []`, die Workflow-Run-Abfrage `workflow_runs: []`. Letztere erfasst Push-Läufe nicht zuverlässig. Daraus folgt **kein** Nachweis eines grünen oder roten Builds. Kein lokaler Gradle-Lauf in diesem Abgleich; **keine APK-Freigabe**.
- **Offene Issues:** #3–#11 sind im Issue-Abgleich weiterhin als offene Arbeiten gelistet. Die Roadmap ist teilweise ein historischer Snapshot; diesen Abschnitt bei der nächsten Statuskonsolidierung als aktuelle Ergänzung berücksichtigen.
- **Chat-Abgleich:** Andere Projekt-Chats konnten nicht vollständig live gelesen werden. Bekannte Anforderungen zu GPS-Stabilität, Kartenfortschritt und richtiger Einstiegshaltestelle sind im Arbeitsplan berücksichtigt; keine vollständige Synchronisierung behauptet.
- **Nächste konkrete Arbeit:** Bestehende Tests für Track-Erhalt und Reroute-Grenzfälle lesen und um einen deterministischen Regressionstest ergänzen; anschließend die tatsächlichen GitHub-Actions-Push-Läufe und Gerätevalidierung separat nachweisen.


## Automatischer Entwicklungsabgleich – 16.11.2026, 22:21 MEZ

- **Repository- und Issue-Abgleich:** Neueste zuvor vorhandene Commits `7b439f3` (Concurrency-Dokumentation) und `6b2b55f` (Concurrency-Isolation); neun offene Issues #3–#11. README, Roadmap und Workflow geprüft. Keine vollständige Live-Sicht auf andere Projekt-Chats.
- **Umgesetzt auf `main`:** `e514a12` fügt `scripts/test-ci-concurrency.py` als Python-Standardbibliothekstest hinzu. Er prüft die Workflow-Concurrency-Gruppe auf Workflow-, Ereignis- und Ref-Isolation sowie die Beschränkung automatischer Abbrüche auf Nicht-Tag-Pushes. `ebfc442` bindet den Test in den bestehenden Preflight-Job ein. Keine Signing- oder Secret-Änderungen.
- **Nachweisgrenze:** Der Guard ist eine statische Regression gegen unbeabsichtigte YAML-Änderungen, **kein** Ersatz für den Test mit überlappenden echten Actions-Läufen. Für `ebfc442` ergaben die verfügbaren Commit-Status- und PR-gefilterten Workflow-Abfragen leere Ergebnisse; der tatsächliche Push-Buildstatus bleibt **unbekannt**. Der neue Test wurde in diesem Lauf nicht lokal ausgeführt. **Keine APK-Freigabe.**
- **Nächste Priorität:** Einen tatsächlichen Actions-Lauf einschließlich Preflight-Ergebnis und Run-ID prüfen; anschließend deterministische GPS-/Reroute-/Track-Erhalt-Tests ergänzen und Release-Gates gesondert verifizieren.

## Automatischer Entwicklungsabgleich – 17.11.2026, 00:25 MEZ

- **Repository:** Vor diesem Lauf letzter Commit `6acab748`; neun offene Issues #3–#11, keine offenen PRs. README, Roadmap, Projektstatus, CI-Workflow und vorhandenen Concurrency-Test erneut gelesen.
- **Umgesetzt:** `96188dd` erweitert `scripts/test-ci-concurrency.py` um eine Prüfung, dass der Guard tatsächlich im `preflight`-Job als ausführbarer Schritt eingebunden ist. Dies verhindert, dass spätere Workflow-Änderungen den statischen Guard unbemerkt deaktivieren. Keine Signing-/Secret-Änderung.
- **Nachweisgrenze:** Commit bestätigt, aber Test nicht lokal ausgeführt; Push-Workflow-Runs sind über die verfügbare commitbezogene GitHub-Schnittstelle nicht vollständig einsehbar. **Build/APK unbekannt, keine Freigabe.** Die tatsächliche Concurrency-Scheduler-Isolation bleibt mit überlappenden Runs zu prüfen.
- **Chats:** Kein vollständiger Live-Zugriff auf andere Projekt-Chats. Sichtbare Anforderungen zu GPS-Lücken, Off-Route-Rerouting, Track-Erhalt und MVV-Einstieg bleiben im Plan.
- **Nächste Priorität:** Actions-Run-ID und Preflight-Ausführung nachweisen; danach deterministische Regressionstests für GPS-Lücken, Rerouting und Track-Erhalt.

## Automatischer Entwicklungsabgleich – 17.11.2026, 01:17 MEZ

- **Repo:** Vor Arbeitsbeginn war `d7faeae` HEAD; neun offene Issues, keine offenen PRs. README, Roadmap, Status und Android-Workflow gelesen. Andere Projekt-Chats sind nicht vollständig live zugänglich.
- **Fehlerbefund:** In `scripts/test-ci-concurrency.py` waren reguläre Ausdrücke als Raw-Strings mit doppelt escapten Backslashes geschrieben (`\\s`, `\\n` usw.). Damit konnte der Preflight-Guard die YAML-Abschnitte nicht wie beabsichtigt erkennen; dies ist ein statischer Codebefund, kein ausgeführter Fehlertest.
- **Änderungen:** `a31390b` ergänzt die Prüfung, dass der APK-Build vom Preflight abhängt. `f67f63a` korrigiert die Raw-Regex-Escapes im bestehenden Test. Keine Signing-/Secrets-Änderungen.
- **Verifikation:** Datei nach Commit erneut aus GitHub gelesen. Weder lokale Ausführung des Python-Tests noch erfolgreicher GitHub-Actions-Lauf nachgewiesen; die verfügbare commitbezogene Workflow-Abfrage zeigt nur PR-Läufe und liefert für `a31390b` keine Ergebnisse. **APK nicht freigegeben.**
- **Nächster Schritt:** `python3 scripts/test-ci-concurrency.py` in einem echten Checkout ausführen und den aktuellen Actions-Preflight samt Run-ID prüfen. Danach GPS-/Rerouting-/Track-Erhalt-Regressionen priorisieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 02:16 MEZ

- **Repo zuerst geprüft:** README, Implementierungs-Roadmap, zentraler Status, Android-CI-Workflow, offene Issues/PRs und jüngste Commits. Vor Änderung war `4a2848e` der neueste gefundene Commit; neun Issues offen, keine offenen PRs.
- **Kleines Testpaket:** `f2c6e57` erweitert `scripts/test-ci-concurrency.py`: Auch der Release-Emulator-Job muss ausdrücklich `needs: preflight` enthalten. Das sichert eine zweite, APK-relevante Teststrecke gegen versehentliches Umgehen des Preflight ab. Keine Signing-/Secret-Änderung.
- **CI-Grenze:** Commitbezogene GitHub-Abfrage für `4a2848e` lieferte null PR-Workflow-Läufe und null klassische Commit-Statuschecks. Diese Schnittstellen zeigen Push-Läufe nicht zuverlässig; das ist **kein Beleg für fehlgeschlagene oder erfolgreiche Push-CI**. Den neuen Python-Test in diesem Lauf nicht lokal ausgeführt.
- **Projekt-Chats:** Sichtbarer Verlauf nennt GPS-Aussetzer, Off-Route-Rerouting, verschwundenen Track, UI-Icons, Home-Schnellaktion und dynamischen Zeitpuffer. Andere Chats nicht vollständig live abrufbar; keine vollständige Synchronisation behauptet.
- **Freigabe:** Keine grüne CI, kein Gerätetest, keine APK-Freigabe nachgewiesen.
- **Nächster Schritt:** `python3 scripts/test-ci-concurrency.py` auf Checkout ausführen, Actions-Preflight mit Run-ID/Logs verifizieren; anschließend GPS-/Track-Erhalt-Regressionen implementieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 06:25 MEZ

- **Repository:** README, zentrale Statusdatei, Roadmap, Android-Workflow und Release-Abnahmematrix gelesen. GitHub-Suche: neun offene Issues (#3–#11), keine offenen PRs. Vor Änderung war der zuletzt dokumentierte Release-Gate-Commit `fd21b03`; den aktuellen Head nicht allein aus dem historischen Status ableiten.
- **Umgesetzt:** Commit `156e100` erweitert den bereits im Preflight eingebundenen Offline-Regressionstest `scripts/test-ci-concurrency.py`. Er prüft jetzt die fünf expliziten `release-acceptance.needs`-Kanten, `always()`, Aktivierung für manuelle Release-Läufe und Versionstags, Abfrage jedes Job-Ergebnisses und Fail-Closed-Exit. Kein Signing-/Secrets-Eingriff.
- **Nachweis:** Änderung nach GitHub zurückgelesen und Commit-Diff geprüft. Commit-Workflow-Abfrage für `156e100` ergab `workflow_runs: []`, klassische Statusabfrage `statuses: []`; die Workflow-Abfrage ist auf PR-Läufe begrenzt. Python-Test und Android-Tests wurden in diesem Lauf nicht tatsächlich ausgeführt. **Kein grüner CI-Nachweis, keine APK-Freigabe.**
- **Projekt-Chats:** Bekannte Anforderungen zu GPS-Lücken, Track-Erhalt, Off-Route, MVV-Haltestellen und dynamischem Zeitpuffer bleiben relevant; andere Chats nicht vollständig live lesbar.
- **Nächste Priorität:** Python-Guard in einem echten Checkout ausführen und Release-Run-ID mit allen sechs Job-Ergebnissen einschließlich Gate belegen. Anschließend deterministische GPS-/Track-/Rerouting-Tests, nicht bloß weitere CI-Dokumentation.


## Automatischer Entwicklungsabgleich – 17.11.2026, 07:23 MEZ

- **Repository zuerst geprüft:** README, Roadmap, Projektstatus, Android-CI-Workflow, letzte Commits, neun offene Issues und keine offenen PRs. Ausgangs-HEAD `19b73e5`.
- **Änderung:** `970bb47` ergänzt in `scripts/test-ci-concurrency.py` einen dynamischen Shell-Test des tatsächlichen `release-acceptance`-Skripts: fünf erfolgreiche Jobs müssen akzeptiert werden, und für jeden der fünf Jobs werden `failure`, `cancelled`, `skipped` und leere Ergebnisse abgelehnt (21 Kombinationen). Der bestehende Preflight ruft das Skript bereits auf. Keine Secrets-/Signing-Änderung.
- **Verifikation:** Commit von GitHub bestätigt; Python-Test, Android-Tests und Actions-Lauf sind noch nicht ausgeführt/nachgewiesen. Die verfügbare Commit-Workflow-Schnittstelle zeigt nur PR-Läufe und eignet sich nicht als Push-CI-Beleg. **Keine APK-Freigabe.**
- **Chats:** Sichtbarer Projektkontext zu GPS-Aussetzern, Track-Erhalt, Satellitenanzeige, Home-Aktion und Rerouting berücksichtigt; kein vollständiger Live-Zugriff auf alle anderen Chats.
- **Nächste Priorität:** Neuen Preflight wirklich ausführen und Actions-Run-ID samt Ergebnissen prüfen. Danach deterministische GPS-Lücken-/Reroute-/Track-Erhalt-Regressionen; keine weiteren reinen CI-Dokumentationsrunden.


## Automatischer Entwicklungsabgleich – 17.11.2026, 12:17 MEZ

- **Live geprüft:** `main`-HEAD `37faff1be99ffc8c8c792a17a0e8da52397bfcec`; neun offene Issues (#3–#11), keine offenen PRs. README, diese Statusdatei, `.github/workflows/android.yml`, `scripts/preflight.sh`, `scripts/test-ci-concurrency.py` und `scripts/test-emulator-navigation.py` erneut gelesen. Der frühere Statusabschnitt vom 16.11. ist ein historischer Snapshot.
- **CI-Beleg:** Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) für `dc87e65`: `preflight` = failure (Job-ID `133914886170`), die übrigen fünf Jobs = skipped. Job-Steps-Abfrage gibt ein leeres Array zurück; Job-Logs sind über den Connector nicht abrufbar. Das ist **kein** Beleg für einen konkreten Python- oder Shell-Fehler.
- **Aktuelle Preflight-Reihenfolge:** `python3 scripts/test-ci-concurrency.py` → `bash -x scripts/preflight.sh` → `python3 -u scripts/test-emulator-navigation.py`. Der erste Schritt enthält 52 dynamische Release-Gate-Fälle; alle drei Befehle müssen separat ausgeführt werden, um den ersten tatsächlichen Fehler zu isolieren. Eine fehlerhafte Vermutung darf nicht als Ursache in Code umgesetzt werden.
- **Ausführungsgrenze:** Lokaler `git ls-remote`-Versuch scheitert mit `Could not resolve host: github.com`. Daher weder Repository-Klon noch echte lokale Testausführung in diesem Lauf möglich. Das GitHub-Connector-Lesen funktioniert, aber es liefert keinen ausführbaren Checkout. **Keine APK-Freigabe.**
- **Nächster risikoarmer Schritt:** GitHub-Actions-Run mit erreichbaren Logs öffnen; alternativ vollständigen Checkout in einer netzfähigen Umgebung bereitstellen und die drei Preflight-Befehle einzeln ausführen. Erst nach belegtem Fehler minimalen Fix plus Regressionstest auf `main` committen und den neuen Run prüfen. Anschließend #6 GPS-Lücken/Track-Erhalt und #3 Off-Route-Rerouting testen.
- **Chat-Abgleich:** Nur die hier verfügbaren Projektkontexte und dokumentierten Anforderungen (GPS, Track, Satellitenstatus, Rerouting, Puffer, UI) berücksichtigt. Kein vollständiger Zugriff auf fremde Chatverläufe behauptet.


## Automatischer Entwicklungsabgleich – 17.11.2026, 13:21 MEZ

- **Repository:** Vor Änderung war `0d0f373` der neueste Commit; neun Issues offen (#3–#11), keine offenen PRs. README, Roadmap, CI-Workflow, Projektstatus und Preflight-Skripte geprüft.
- **Verifizierter CI-Befund:** Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) für `dc87e65`: `preflight=failure`, übrige fünf Jobs `skipped`. Der Job-Endpunkt liefert keine einzelnen Steps. Ursache weiterhin unbekannt.
- **Änderung:** Commit `d5948bb` trennt den bisherigen kombinierten Preflight-Schritt in zwei benannte Schritte (Shell-/Python-Syntax sowie Emulator-Navigationstest). Der Concurrency-Guard bleibt unverändert. Zweck: zukünftige Fehler auf einen konkreten Schritt eingrenzen, **kein behaupteter Fehlerfix**.
- **Verifikation:** Neue Workflow-Datei nach Commit aus GitHub zurückgelesen. Lokaler Clone und damit Testausführung scheiterten weiterhin an `Could not resolve host: github.com`. Ein neuer Actions-Lauf und grüne Tests für `d5948bb` sind noch **nicht** nachgewiesen. **Keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbare bekannte Anforderungen (GPS-Lücken, Track-Erhalt, Rerouting, Satellitenstatus, UI) berücksichtigt; andere Chats nicht vollständig live abrufbar.
- **Nächster Schritt:** Push-Run für `d5948bb` prüfen, den konkret fehlgeschlagenen Preflight-Schritt anhand GitHub-UI/Logs identifizieren, dann gezielt korrigieren und Regressionstest ausführen. Danach GPS-/OSM-Regressionen priorisieren.

## Entwicklungsabgleich – 17.11.2026, 14:20 MEZ

- `main` vor dem Lauf: `45a0f08`; neun offene Issues (#3–#11), keine offenen PRs. README, Roadmap, Workflow, Projektstatus und Preflight-Skripte erneut geprüft.
- Der bislang konkret abrufbare Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) ist **rot**: `preflight=failure`, fünf weitere Jobs `skipped`. Für diesen Run liefert die GitHub-Anbindung weder Job-Schritte noch Logs. Die Commit-Workflow-Abfrage für `45a0f08` liefert keine Läufe, deckt Push-Runs aber nicht verlässlich ab; daraus folgt **kein** grüner Status.
- Änderung `9d257ab`: `scripts/preflight.sh` meldet mit einem `ERR`-Trap bei einem fehlschlagenden Shell-/Python-Syntaxcheck künftig Befehlszeile, Zeilennummer und Exitcode. Kein Signing-/Secret-Eingriff; keine Änderung an Produktiv-App-Code. Die vorgeschaltete `test-ci-concurrency.py`-Prüfung wird davon nicht erfasst, da sie ein eigener Workflow-Schritt ist.
- **Verifikation:** GitHub-Commit bestätigt. Lokale Ausführung/Android-Build durch DNS-Fehler (`Could not resolve host: github.com`) blockiert. Neuer Push-Run und vollständige sechs Release-Jobs nicht bestätigt. **Keine APK-Freigabe.**
- **Nächster Schritt:** Frischen Push-Run von `9d257ab` prüfen, fehlgeschlagenen Preflight-Schritt identifizieren und Fehlerursache gezielt beheben. Danach GPS-Replay bei temporärem Provider-Ausfall, Track-Erhalt, Satellitenstatus und OSM-Routenfortschritt mit Regressionstests absichern.
- Andere Projekt-Chats nicht vollständig live zugänglich; bekannte Anforderungen aus dem sichtbaren Projektkontext berücksichtigt, keine vollständige Synchronisierung behauptet.


## Automatischer Entwicklungsabgleich – 17.11.2026, 16:24 MEZ

- **Live-Basis:** GitHub-`main` zuletzt `5ff0662e56c48ce90f41c876f322d22e17a9f71b`; keine neueren Commits in der abgefragten Commitliste. README, Implementierungs-Roadmap, Projektstatus, Android-Workflow, `scripts/preflight.sh` und `scripts/test-ci-concurrency.py` gelesen. Keine offenen PRs; **elf offene Issues** einschließlich der neuen Praxisfehler [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) und [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14). Frühere Abschnitte mit neun Issues sind historische Snapshots.
- **#13 Rotation – reproduzierbarer Abnahmefall:** Start/Ziel und Routenoptionen konfigurieren, Fußnavigation aktivieren, Portrait → Landscape → Portrait und Activity-Neuerstellung testen. Auswahl, Routen-Geometrie, aktuelle Etappe, Track und Navigation dürfen nicht zurückgesetzt werden. Vor einem Fix die Zustandsbesitzer (Compose `remember`/`rememberSaveable`, ViewModel, persistierte Session) und Lifecycle prüfen; **keine** pauschale Activity-Neuerstellung unterdrücken.
- **#14 Track – reproduzierbarer Abnahmefall:** Akzeptierte GPS-Punkte sammeln, anschließend GPS-Lücke, ungenauen Netzwerk-Fix, Positionssprung, bestätigte Routenabweichung, Rerouting und Karteneuzeichnung simulieren. Die echte aufgezeichnete Spur bleibt unverändert; ungültige Fixes erzeugen weder Löschung noch künstliche Verbindungssegmente. In `WalkingMiniMap` echte Track-Historie und dunkelviolett routenprojizierten Fortschritt begrifflich/visuell trennen; nicht dieselbe Datenquelle voraussetzen.
- **Priorität:** (1) CI-Preflight mit echtem Fehlernachweis reparieren, (2) #13/#14 deterministische Lifecycle-/Track-Regressionen, (3) GPS-Ausfall und Off-Route-Rerouting, (4) dynamischer Zeitpuffer und MVV-Etappendaten. Keine blinden Produktivänderungen.
- **CI-Grenze:** Die Commit-Workflow-Abfrage für `5ff0662` ergab `workflow_runs=[]`, erfasst aber nur PR-Läufe und **beweist nichts über Push-CI**. Historischer roter Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) bleibt der letzte dokumentierte konkrete Fehlernachweis. In diesem Lauf wurden weder Python-Preflight noch Android-/Emulator-Tests ausgeführt; kein aktueller grüner Run oder APK-Artefakt verifiziert. **Keine APK-Installationsfreigabe.**
- **Chat-Grenze:** Nur in diesem Projektkontext sichtbare Anforderungen und GitHub-Issues abgeglichen; kein vollständiger Live-Zugriff auf Main/Schedule und andere Chats behauptet.
- **Nächste konkrete Aktion:** GitHub-Actions-Push-Run für aktuellen HEAD mit Run-ID und erstem fehlschlagenden Preflight-Schritt ermitteln; alternativ vollständigen Checkout in ausführbarer Umgebung nutzen. Anschließend minimalen, getesteten Fix committen. Für #13/#14 zuerst bestehende Zustands-/Track-Tests und Eigentümer der Daten identifizieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 17:21 MEZ

- **Aktueller GitHub-HEAD vor diesem Eintrag:** `df939babd28a1ed85db5d89943dad2dad57c98d9`. README, Roadmap, zentraler Status, Workflow, `scripts/preflight.sh` und `scripts/test-ci-concurrency.py` erneut gelesen. Die GitHub-Issue-Suche liefert **11 offene Tickets**, einschließlich #13 (Rotation/Zustandsverlust) und #14 (GPS-Track verschwindet); keine neue Codeänderung seit dem vorherigen dokumentierten HEAD festgestellt.
- **Nachweis statt Vermutung:** Der historische Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) liefert bei erneuter Job-Abfrage `preflight=failure` (Job-ID `133914886170`), `build-and-test=skipped`; das ist **kein** CI-Ergebnis für den heutigen HEAD. Die GitHub-Connector-Abfrage für commitbezogene Workflows erfasst Push-Läufe nicht vollständig. Der lokale Versuch `git ls-remote https://github.com/jochenkunkel/mvv-mobility-assistant.git HEAD` scheiterte mit `Could not resolve host: github.com`. Daher keine Testausführung oder APK-Freigabe behauptet.
- **Konkrete Diagnose:** Im aktuellen Workflow sind die drei Preflight-Schritte `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` getrennt. Die ersten beiden Skripte wurden statisch gelesen; aus dem historischen Job-Ergebnis allein lässt sich **nicht** ableiten, welcher Schritt fehlschlug. Keine Änderung an Signing, Secrets oder ungeprüftem Produktivcode.
- **Priorisierte Reparatur:** (1) neuesten **Push**-Run zum HEAD in Actions öffnen, fehlgeschlagenen Preflight-Schritt und erste Fehlerzeile notieren; (2) `python3 scripts/test-ci-concurrency.py && bash -x scripts/preflight.sh && python3 -u scripts/test-emulator-navigation.py` auf einem Runner mit Repository-Zugriff ausführen; (3) genau diesen Fehler mit minimalem Fix plus Regressionstest beheben und CI neu prüfen; (4) danach #13 Lifecycle/Rotation und #14 Track-Persistenz mit GPS-Lücken/Rerouting in Tests absichern.
- **Chat-Grenze:** Anforderungen aus dem hier verfügbaren Projektkontext zu Rotation, GPS-Lücken, Off-Route-Rerouting und Track-Erhalt berücksichtigt; andere Chatverläufe nicht vollständig live lesbar. **Build/APK:** aktueller HEAD ungeprüft, keine Installationsfreigabe.


## Automatischer Entwicklungsabgleich – 17.11.2026, 19:21 MEZ

- **Baseline:** `3f3efb9` war beim Abgleich der letzte Commit auf `main`; 11 offene Issues, keine offenen PRs. README, Roadmap, CI-Workflow, Preflight-Skripte, zentrale Dokumentation und jüngste Commits geprüft. #13 (Rotation) und #14 (GPS-Track) bleiben offen; die Regression-Matrix ist bislang eine Spezifikation, keine ausgeführte Test-Suite.
- **Implementiert:** `ebf59dc` erweitert `scripts/test-ci-concurrency.py` um einen statischen Regression-Guard: die drei Preflight-Kommandos (Concurrency-Guard, Shell/Python-Syntaxcheck, Emulator-Navigationstest) müssen als jeweils eigener `run`-Schritt genau einmal und in dieser Reihenfolge im Workflow stehen. Dadurch kann ein künftiger Workflow-Umbau die zuvor eingeführte Fehler-Isolation nicht unbemerkt wieder entfernen. Keine Änderung an Signing, Secrets oder Android-Produktivcode.
- **CI-Nachweis:** Historischer Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252): Preflight fehlgeschlagen, abhängige Jobs übersprungen. GitHub liefert für diesen Job keine Schritte; die tatsächliche Ursache ist nicht nachgewiesen. Für `ebf59dc` liefern die verfügbaren Commit-Status- und PR-Workflow-Abfragen keine Ergebnisse; **das sagt nichts Verlässliches über Push-Läufe aus**. Der neue Guard wurde nicht lokal ausgeführt und ist nicht als bestanden gemeldet. **Keine APK-Freigabe.**
- **Projekt-Chats:** Die bekannten Anforderungen aus Main/Schedule zu GPS-Lücken, Track-Erhalt, OSRM-Rerouting, dynamischem Puffer und Bildschirmrotation wurden berücksichtigt. Kein vollständiger Live-Zugriff auf alle anderen Projekt-Chats.
- **Nächste Arbeit:** (1) Push-Run über GitHub-Actions-Weboberfläche oder eine echte Push-Run-API ermitteln; ersten fehlgeschlagenen Preflight-Schritt anhand Logs/Annotations isolieren. (2) Den CI-Guard lokal mit `python3 scripts/test-ci-concurrency.py` ausführen. (3) #13/#14 mit ausführbaren Zustandserhalt- und GPS-Replay-Tests bearbeiten; keine bloße weitere Testplanung statt Code.

## Automatischer Entwicklungsabgleich – 17.11.2026, 20:21 MEZ

- **Abgleich:** GitHub-`main` vor der Änderung `9e392fc`; README, Roadmap, Regressionstest-Matrix, Workflow, Preflight-Skripte und offene Tickets geprüft. 11 offene Issues, keine offenen PRs. #13 Rotation und #14 Track-Erhalt weiterhin offen. Keine neuen Commits anderer Entwickler seit dem vorherigen Lauf erkennbar.
- **Konkrete Änderung:** `43e3768` ergänzt in `scripts/preflight.sh` eine GitHub-Actions-`::error`-Annotation bei Shell-/Python-Syntaxfehlern. Der vorhandene stderr-Fehlerbericht bleibt bestehen. Ziel: Fehlermeldungen auch über die Actions-Oberfläche sichtbar machen, wenn Job-Logs nicht abrufbar sind. Keine Änderungen an Signing, Secrets oder Produktivcode.
- **CI-Evidenz:** Der historische Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) meldet `preflight=failure`, abhängiger Build `skipped`. Job-Schritte werden als leere Liste zurückgegeben; Logabruf scheitert. Commit-Status und PR-bezogene Workflow-Abfrage für `9e392fc` waren leer und sind **kein** Beleg über Push-Läufe. Der neue Commit `43e3768` ist **nicht** durch einen grünen CI-Lauf verifiziert.
- **Lokale Einschränkung:** `git ls-remote` scheiterte mit `Could not resolve host: github.com`; keine lokale Ausführung der Repository-Tests möglich. Die neue Annotation ist noch nicht in GitHub Actions beobachtet. **Keine APK-Installationsfreigabe.**
- **Nächster Schritt:** Actions-Push-Lauf für `43e3768` und dessen Preflight-Annotationen kontrollieren. Bei identischem Fehler den ersten fehlschlagenden Schritt isolieren; anschließend ausführbare Regressionstests für #13 und #14 umsetzen. Andere Projekt-Chats sind nicht vollständig live zugänglich; nur bereits dokumentierte Anforderungen wurden übernommen.


## Entwicklungsabgleich – 17.11.2026, 21:23 MEZ

- Vor Änderung letzter Commit auf `main`: `7800335`; offene Issues einschließlich #13 (Rotation) und #14 (Track-Verlust), keine offenen PRs. README, Roadmap, Preflight, Emulator-Test und Projektübergabe erneut gelesen.
- **Umgesetzt:** `5df2a0f` ergänzt zwei Offline-Regressionstests für ausgeblendete/deaktivierte doppelte Tab-Labels; `ef14141` lässt `scripts/tap-emulator-navigation.py` solche Nodes überspringen, bevor Koordinaten gewählt werden. Bestehende XML-Dumps ohne diese Attribute bleiben kompatibel. Dies behebt keine GPS-/Rotationsfehler, sondern einen klar abgegrenzten Emulator-Testhelfer-Randfall.
- **Tests:** Die neuen Tests sind im Repository, wurden in diesem Lauf nicht ausgeführt. Ein grüner CI-Lauf für den neuen HEAD ist nicht belegt. Historischer Preflight-Fehler in Actions-Lauf 44046946252; tatsächlicher neuer Push-Lauf und Job-Logs weiterhin gesondert prüfen. **Keine APK-Freigabe.**
- **Projekt-Chats:** Bekannte Anforderungen zu GPS-Lücken, OSRM-Rerouting, Track-Erhalt und Rotation berücksichtigt; kein vollständiger Live-Zugriff auf andere Chats.
- **Nächste Priorität:** `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` auf demselben HEAD ausführen und Actions-Schritte prüfen; danach #13/#14 mit instrumentierten Zustands- und GPS-Replay-Tests bearbeiten.

## Entwicklungsabgleich – 17.11.2026, 22:22 MEZ

- **Repo:** Vor diesem Lauf war `d316e2d` der letzte sichtbare Commit auf `main`; README, Implementierungs-Roadmap, Preflight-/Emulator-Skripte und Android-Workflow geprüft. 11 offene Issues, keine offenen PRs. #13 (Rotation) und #14 (Trackverlust) weiterhin nicht behoben.
- **Implementiert:** `bc290ff` ergänzt zwei ausführbare Offline-Regressionstests in `scripts/test-emulator-navigation.py`: ungültige Bounds beim ersten doppelten Navigationslabel dürfen einen späteren gültigen Treffer nicht verhindern; ausschließlich unsichtbare/deaktivierte Treffer müssen kontrolliert mit `ValueError` scheitern. Produktivcode, Signing und Secrets unverändert.
- **Nachweise:** Die neuen Tests wurden in diesem Lauf **nicht ausgeführt**. Für `bc290ff` meldet die Commit-Status-Abfrage keine Checks und die PR-begrenzte Workflow-Abfrage keine Runs; das erlaubt keine Aussage über Push-CI. Historischer Run `44046946252`: Preflight `failure`, Build und Release-Jobs `skipped`; Schritte/Logs weiterhin nicht vorhanden. **Keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbarer Projektkontext zu GPS, OSRM, Rotation und Track berücksichtigt; vollständiger Live-Zugriff auf andere Chats nicht nachgewiesen.
- **Nächste Priorität:** Tatsächlichen Push-Run/Preflight-Fehler samt Logs oder Annotationen ermitteln; danach ausführbare Tests und Fixes für #13/#14 statt weiterer reiner Planungsdokumentation. Lokal/CI: `python3 scripts/test-ci-concurrency.py && bash -x scripts/preflight.sh && python3 -u scripts/test-emulator-navigation.py`.


## Entwicklungsabgleich – 17.11.2026, 23:22 MEZ

- **Live-Abgleich:** `main` vor diesem Eintrag `c80ce362eaa8a91e586bb61f25621d7ff47d24a1`; seit dem letzten Lauf keine weiteren Commits. README, Roadmap, Android-CI, Preflight-Skript, Emulator-Testskript und offene Issues gelesen. **11 Issues offen**, darunter #13 (Rotation) und #14 (Trackverlust); keine offenen PRs laut Issue-Suche.
- **Neuer belastbarer CI-Nachweis:** GitHub Actions **Push-Run #404**, ID [44179252791](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791), HEAD `c80ce36`, gestartet 17.11.2026 21:23 UTC, ist **completed/failure**. Die Jobs-API bestätigt `preflight=failure`; `build-and-test`, `emulator-smoke-test`, `mvv-live-probe`, `door-to-door-live-probe` und `release-acceptance` sind `skipped`. Das ist eine echte Push-CI-Prüfung und widerlegt die Annahme, es lägen nur alte CI-Ergebnisse vor.
- **Exakte Blockade:** Job-ID `134378837918`: Step-API liefert `steps=[]`; Job-Log-Download antwortet HTTP 404 `BlobNotFound`. Deshalb ist **nicht feststellbar**, ob `test-ci-concurrency.py`, `preflight.sh` oder `test-emulator-navigation.py` zuerst scheitert. Die drei Befehle sind im Workflow getrennt. Kein blindes Ändern von Produktivcode oder CI-Semantik.
- **Nächste ausführbare Diagnose:** In [Actions #404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) die Preflight-Step-Ansicht/Annotationen kontrollieren. Wenn ebenfalls unzugänglich, dieselben drei Befehle auf exakt `c80ce36` lokal ausführen: `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py`. Ersten fehlgeschlagenen Befehl samt stderr sichern und gezielt korrigieren. Danach neuen Push-Run verifizieren und #13/#14 implementieren.
- **Projekt-Chats:** Bekannte Anforderungen aus Main/Schedule zu GPS-Lücken, Rerouting, Track-Persistenz und Rotation übernommen; vollständige Live-Synchronisierung anderer Chatverläufe nicht nachgewiesen. **Keine Tests als bestanden ausgegeben; keine APK-Installationsfreigabe.**

## Entwicklungsabgleich – 18.11.2026, 01:19 MEZ

- **Repository erneut live gelesen:** `main` unverändert bei `38c395c3`; README, Roadmap, CI-Workflow, `scripts/test-ci-concurrency.py`, `scripts/preflight.sh` und Emulator-Testskript geprüft. 11 offene Issues, keine offenen PRs; #13 (Rotation) und #14 (Track-Verlust) bleiben priorisiert. Keine neuen externen Commits seit dem vorherigen Abgleich.
- **CI-Diagnose erneut konkret überprüft:** Für Push-Lauf [#404 / 44179252791](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) meldet die Jobs-API `preflight=failure` (Job `134378837918`) und `build-and-test=skipped`; die Step-Abfrage liefert erneut `[]`, der Job-Log-Download HTTP 404 `BlobNotFound` (18.11.2026 00:19 UTC). Somit bleibt die fehlerhafte Einzelprüfung **unbekannt**. Die drei Preflight-Befehle sind separat im Workflow eingetragen; aus statischer Sicht kein eindeutiger Syntaxdefekt nachgewiesen. Weder statische Sichtprüfung noch Commit ersetzen einen ausgeführten Test.
- **Gezielter nächster Diagnoseversuch:** Ein Checkout des aktuellen HEAD muss die drei Befehle `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` in dieser Reihenfolge einzeln ausführen und deren Exitcodes/stderr sichern. Nur den tatsächlich fehlschlagenden Schritt reparieren; falls alle lokal erfolgreich sind, Runner-/Actions-Umgebung bzw. Checkout-Fehler anhand der Job-Ansicht prüfen. Keine blinde CI- oder Signing-Änderung.
- **Produktpriorität:** Danach ausführbare Rotation-Zustandserhalt-Tests (#13) und GPS-Track-Replay mit Fix-Lücken/Rerouting (#14), nicht bloß weitere Testmatrizen. Bekannte Anforderungen aus Main/Schedule berücksichtigt; vollständige Live-Sicht auf andere Chats weiterhin nicht verfügbar.
- **Verifikation dieses Durchlaufs:** Kein lokaler Test und kein erfolgreicher Push-Build nachgewiesen. **Keine APK-Freigabe.**

## Entwicklungsabgleich – 18.11.2026, 03:17 MEZ

- **Live-Repository:** Vor Änderung `004be675`; README, Roadmap, Status, Android-Workflow, Preflight-/Emulator-Skripte, Commits und offene Issues/PRs geprüft. Weiterhin 11 offene Issues, keine offenen PRs; #13 Rotation und #14 GPS-Trackverlust nicht behoben.
- **Konkrete CI-Diagnoseänderung:** Commit `1cf14d3` ergänzt im Preflight-Job einen **nur bei Fehler** laufenden Diagnose-Schritt. Er schreibt für jeden der drei bestehenden Preflight-Befehle einen Exitstatus und die letzten 25 Ausgabezeilen in die GitHub-Actions-Job-Zusammenfassung und erzeugt eine Fehlerannotation. Bei fehlendem Checkout meldet er die fehlende Skriptdatei. Die bestehenden drei Prüfungen, `needs: preflight`, Release-Gates und Signing bleiben unverändert. Diagnoseschritte dürfen einen fehlgeschlagenen Job **nicht** in Erfolg umwandeln.
- **Ausgangsnachweis:** Push-Run [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) für `c80ce36`: Preflight failure, Build skipped; Steps/Logs nicht abrufbar. Die Ursache ist weiterhin **nicht** nachgewiesen. Der neue Schritt soll im nächsten fehlgeschlagenen Run die Ursache ohne Log-Download eingrenzen.
- **Verifikation:** Nach Commit `1cf14d3` ist die aktualisierte Workflow-Datei per GitHub erneut gelesen worden. Commit-Status-API: keine Statuschecks; die verfügbare Commit-Workflow-Abfrage zeigt nur PR-Läufe und liefert keine Push-Bestätigung. Keine lokale Skriptausführung (GitHub-Klon scheitert an DNS). **Kein grüner Build, keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbarer Kontext aus Main/Schedule: GPS-Aussetzer auf freier Strecke, Trackverlust, Rerouting und Konfigurationsverlust bei Rotation. Vollständiger Live-Zugriff auf andere Chats nicht bestätigt; keine neuen Anforderungen erfunden.
- **Nächster Schritt:** Push-Lauf für `1cf14d3` und dessen Preflight-Job-Zusammenfassung prüfen; den zuerst fehlschlagenden Befehl gezielt beheben. Danach Rotation-Zustandserhalt (#13) und GPS-Track-Replay/Rerouting (#14) mit ausführbaren Tests angehen.

## Entwicklungsabgleich – 18.11.2026, 08:20 MEZ

- **Live-Repository:** Vor der Änderung `main=8718ee4`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Chat-Übergabe, Projektstatus und Workflow sowie alle drei Preflight-Skripte abgeglichen. Ältere Kopfzeilen dieser Datei sind historische Snapshots.
- **Neue Änderung:** `e5edcc9` ergänzt in `scripts/test-ci-concurrency.py` statische Regressionen, die das Vorhandensein des fehlertoleranten Diagnose-Schritts, dessen `if: failure()`-Bedingung, das `GITHUB_STEP_SUMMARY` und die erneute Einzelprüfung aller drei Preflight-Kommandos absichern. Keine Signing-/Secret-Änderungen.
- **Nachweisgrenze:** Änderung über GitHub committed; kein lokaler vollständiger Python-/Gradle-Lauf, da der Container `github.com` nicht per DNS auflösen kann und daher kein Checkout möglich ist. Der zuvor bestätigte Actions-Lauf #404 (Run-ID `44179252791`) scheiterte im Preflight; das ist **kein** Beleg für den Status von `e5edcc9`. Die verfügbare commitbezogene Workflow-Abfrage erfasst Push-Läufe nicht vollständig. **APK nicht freigegeben.**
- **Chat-Abgleich:** Bekannte Anforderungen aus dem sichtbaren Main-/Schedule-Kontext (GPS-Lücken, Track-Erhalt, Rerouting, Rotation, Dashboard) berücksichtigt; andere vollständige Chatverläufe nicht live nachgewiesen.
- **Nächster Schritt:** Den neuesten tatsächlichen Push-Actions-Lauf einschließlich Diagnose-Zusammenfassung verifizieren; ersten fehlschlagenden Befehl beheben. Danach deterministische Tests zu #14 (Track-Erhalt über GPS-Lücke und Rerouting) und #13 (Zustand bei Rotation).


## Entwicklungsabgleich – 18.11.2026, 10:20 MEZ

- **GitHub-Baseline:** Vor diesem Eintrag ist `main` auf `c19abb696f8d21eb7af542245f2e4f4d8a45fdd5`; letzter Test-Commit `e5edcc9` ergänzt statische Regressionen zur Preflight-Diagnose. 11 offene Issues, keine offenen PRs. Kritisch: #13 Rotation verwirft Konfiguration/Route, #14 GPS-Track verschwindet. Keine neuen Commits seit dem vorherigen Abgleich.
- **CI-Belege und Grenze:** Die Commit-Statusabfrage für `c19abb6` ergibt `statuses=[]`; die verfügbare Workflow-Abfrage liefert nur PR-getriggerte Runs und ist für Push-Runs nicht vollständig. Beim älteren Actions-Lauf [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) ist `preflight=failure`, `build-and-test=skipped`; GitHub liefert zu dessen Preflight-Job weder Einzelschritte noch Logs. Dieser ältere Lauf ist kein Nachweis für den aktuellen HEAD.
- **Quelltextanalyse:** `.github/workflows/android.yml` ruft die drei Preflight-Kommandos separat auf und enthält die zusätzliche Diagnose mit `if: failure()`; `scripts/test-ci-concurrency.py` prüft die Diagnose statisch. Die Skripte wurden hier gelesen, nicht ausgeführt. Der erste fehlschlagende Befehl ist weiterhin unbekannt; ohne Fehlerausgabe keine blinde Änderung. Keine Signing-/Secret-Änderung, keine APK-Freigabe.
- **Konkreter Entblockungsplan:** (1) In GitHub Actions den neuesten *Push*-Run von `main` und dessen Job-Summary öffnen (nicht nur Commit-Status/PR-Run-Abfrage). (2) Falls keine Diagnose erscheint, Preflight auf einem Checkout desselben SHA mit `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` einzeln ausführen und stderr/Exitcodes sichern. (3) Den reproduzierten Fehler minimal korrigieren und echte CI verifizieren. (4) Anschließend #13 und #14 durch Zustands-/GPS-Replay-Regressionstests bearbeiten.
- **Chat-Abgleich:** Sichtbare bekannte Anforderungen aus Main/Schedule zu GPS-Aussetzern, Rerouting, Track-Erhalt und Rotation berücksichtigt; vollständiger Live-Zugriff auf andere Chats ist nicht belegt.


## Entwicklungsabgleich – 18.11.2026, 12:18 MEZ

- **Verifizierter GitHub-Stand vor diesem Eintrag:** `main=b6eb4a2908ee44a200eb696589e61db6ca20f37d`; elf offene Issues, keine offenen PRs. Zuletzt dokumentierte kritische Tickets: #13 Rotation/Route-State, #14 GPS-Trackverlust. README, Roadmap, Chat-Handoff, Android-Workflow und drei Preflight-Skripte direkt aus dem Repository geprüft.
- **CI-Nachweisgrenze:** Die verfügbare GitHub-Connector-Funktion `fetch_commit_workflow_runs` erfasst nur PR-getriggerte Läufe und ist **kein** Nachweis über Push-Läufe. Der öffentliche Actions-Webabruf war in diesem Lauf technisch gesperrt (`DisabledError`). Der ältere bekannte Lauf #404 war rot, lässt aber keinen Schluss auf den aktuellen HEAD zu. **Keine neue Testausführung und keine APK-Freigabe.**
- **Konkrete Code-/Testprüfung:** `scripts/test-ci-concurrency.py` verlangt die drei getrennten Preflight-Kommandos, den Diagnose-Schritt und die Build-/Release-Abhängigkeiten; `.github/workflows/android.yml` enthält diese Schritte. `scripts/preflight.sh` prüft Shell- und Python-Syntax; `scripts/test-emulator-navigation.py` enthält acht XML-Navigationsfälle. Dies sind statische Befunde, keine bestandenen Tests.
- **Reproduktionsplan für die Blockade:** Auf einem Checkout **genau des HEAD** nacheinander `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` ausführen; den ersten Exitcode ungleich null samt vollständigem stderr sichern. Alternativ den aktuellen Actions-Preflight-Job einschließlich Step Summary und Log auslesen. Erst dann gezielt reparieren; kein blindes Abschwächen des Gates.
- **Danach:** #14 reproduzierbaren GPS-Track-Erhalt bei GPS-Lücken und Rerouting mit Regressionstest absichern; #13 Rotation mit wiederhergestelltem Routen-/Einstellungszustand testen. Änderungen anderer Chats nur übernehmen, wenn in GitHub oder zugänglichem Kontext nachgewiesen.


## Entwicklungsabgleich – 18.11.2026, 14:16 MEZ

- Vor Arbeitsbeginn geprüft: `main` auf `3ca3b56`, README, Roadmap, Chat-Handoff, Android-Workflow, Preflight-Skripte, letzte Commits, elf offene Issues (#3–#11, #13, #14), keine offenen PRs. Die hier zugänglichen Projekt-Chats bestätigen Rotation/Trackverlust; vollständiger Live-Zugriff auf andere Chats ist nicht nachgewiesen.
- **Neues Arbeitspaket:** `25c41ac` fügt `docs/REGRESSION-ROTATION-GPS.md` hinzu. Konkrete reproduzierbare Abnahmematrix für #13 (Konfiguration und aktive Navigation bei Rotation) und #14 (angenommene GPS-Spur bleibt bei GPS-Lücken, Reroute und Activity-Recreation erhalten), mit deterministischen Testideen und Geräteprüfung. **Nur Testspezifikation, keine Fehlerbehebung.**
- CI: Die verfügbare commitbezogene Workflow-Abfrage deckt Push-Läufe nicht zuverlässig ab; kein grüner Build für diesen Commit nachgewiesen. In diesem Lauf keine Gradle-/Emulatortests ausgeführt. Keine APK-Freigabe.
- **Nächster Schritt:** Zuerst aktuellen Push-Preflight anhand echter Actions-Fehlerausgabe entblocken; anschließend ActivityScenario-Recreation-Test für #13 und Zustands-/Reroute-Tests für #14 implementieren. Keine redundanten reinen Status-Commits ohne Erkenntnisgewinn.

## Automatischer Entwicklungsabgleich – 18.11.2026, 15:18 MEZ

- **Repository:** HEAD vor Änderung `61610fc`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Status, Rotations-/GPS-Testmatrix, Workflow, Preflight- und Emulator-Navigationstests gelesen. Bekannte Anforderungen aus Main/Schedule berücksichtigt; kein vollständiger Live-Zugriff auf andere Chats.
- **Kleine konkrete Korrektur:** `8d890dd` berücksichtigt bei der Suche nach anklickbaren Navigationslabels auch `displayed=false` bzw. `enabled=false` auf übergeordneten UIAutomator-Knoten. Vorher konnte ein scheinbar sichtbares Kind unter einem versteckten/deaktivierten Container als Tap-Ziel ausgewählt werden. `e17fa82` ergänzt drei Regressionstests (versteckter Elternknoten, deaktivierter Elternknoten, ausschließlich versteckter Treffer). Keine Signing-/Secret-Änderung.
- **Verifikation:** Quelltext und Tests committed, aber **nicht lokal ausgeführt**; aktueller Push-Actions-Status über die verfügbare PR-gefilterte Commit-Abfrage nicht sicher feststellbar. Keine Aussage über grünen Build und **keine APK-Installationsfreigabe**.
- **Nächster Schritt:** `python3 -u scripts/test-emulator-navigation.py`, `python3 scripts/test-ci-concurrency.py`, `bash scripts/preflight.sh` auf aktuellem Checkout ausführen; dann aktuellen Push-Workflow und ggf. Fehlerlogs prüfen. Anschließend #13/#14 anhand der vorhandenen Testmatrix in ausführbare Lifecycle-/Track-Tests überführen.

## Automatischer Entwicklungsabgleich – 18.11.2026, 16:18 MEZ

- **Repo-Abgleich:** Vor Änderung HEAD `6189290`; README, Roadmap, Chat-Handoff, Workflow, Preflight-Skripte und Regressionstestplan gelesen. 11 offene Issues (#3–#11, #13, #14), keine offenen PRs. #13 Rotation und #14 Trackverlust weiterhin nicht behoben.
- **Eigene Änderung:** Commit `9216411` erweitert `scripts/test-emulator-navigation.py` um drei deterministische Tests: versteckter Elternknoten mit Content-Description, deaktivierter Großelternknoten und defektes UI-XML. Das ist Testabdeckung für den bestehenden Navigationshelfer, kein Fix für #13/#14.
- **Ausführung/CI:** Git-Checkout in der Laufumgebung scheitert an DNS (`Could not resolve host: github.com`). Die drei neuen Tests wurden nicht ausgeführt; GitHub-Commit-Workflow-Abfrage erfasst Push-Läufe nicht zuverlässig. Für den neuen HEAD ist kein grüner Build nachgewiesen; **keine APK-Installationsfreigabe**.
- **Nächste Priorität:** GitHub-Actions-Push-Lauf über Actions-Oberfläche oder einen verlässlichen Run-Endpunkt samt Preflight-Einzelschritten prüfen. Danach #13 Activity-Recreation und #14 Track/Reroute-Reducer mit ausführbaren Tests beheben. Andere Projekt-Chats sind nicht vollständig live zugänglich; keine erfundenen neuen Anforderungen.


## Entwicklungsabgleich – 18.11.2026, 19:16 MEZ

- **Repository:** Zu Beginn des Laufs war `main` auf `c0a25a1f94aa42c3e0b1c2755d4ee6d39aa92d53`. README, Roadmap, ADRs, Rotation/GPS-Regressionsmatrix und Android-Workflow gegen die letzten Commits geprüft. Elf offene Issues (#3–#11, #13, #14), keine offenen PRs. Die ältere Tabelle „Offen (9)“ oben ist ein historischer Stand und darf nicht als aktuelle Ticketliste interpretiert werden.
- **Konkrete Dokumentationsabweichung:** Der Kopf dieser Datei nennt noch 16.11.2026 und die alte Neun-Issue-Baseline, obwohl die jüngsten Commits bis `c0a25a1` und die zwei neuen Bugs #13/#14 hinzugekommen sind. Dieser Eintrag ergänzt die nachvollziehbare aktuelle Baseline, ohne alte Audit-Einträge umzuschreiben.
- **CI-Nachweisgrenze:** Die Commit-Workflow-Abfrage für `c0a25a1` ergab keine PR-Läufe; diese Schnittstelle erfasst Push-Läufe nicht zuverlässig. Der ältere Actions-Lauf 44179252791 meldet `preflight=failure`, `build-and-test=skipped`; seine Jobdetails enthalten keine Schritte/Logs. Das ist **kein** Beleg für den Buildzustand von `c0a25a1`. Die drei aktuellen Preflight-Schritte sind `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py`. Keiner davon wurde in diesem Lauf ausgeführt.
- **Projekt-Chats:** Anforderungen aus den zugänglichen Projektkontexten Main/Schedule (GPS-Aussetzer, verschwindender violetter Track, Rotation, Rerouting) mit #13/#14 abgeglichen; kein vollständiger Live-Zugriff auf alle Chatverläufe behauptet.
- **Nächste ausführbare Schritte:** (1) Aktuellen Push-Actions-Lauf mit Preflight-Step-Ausgabe beschaffen oder die drei Kommandos auf Checkout von `main` ausführen; den **ersten reproduzierten** Fehler reparieren. (2) #13: Activity-Recreation-Test für ausgewählte Route und Einstellungen. (3) #14: reinen State-Test für akzeptierten GPS-Track über Aussetzer, Reroute und Rotation. Details und Abnahmekriterien in `docs/REGRESSION-ROTATION-GPS.md`.
- **Freigabe:** Kein neuer Test-/Emulator-/Gerätenachweis; keine APK-Installationsfreigabe.


## Entwicklungsabgleich – 18.11.2026, 20:22 MEZ

- **GitHub-Baseline:** Neuester über Commit-Suche sichtbarer `main`-Commit `49da9b1956f58dbaa38ccccdc1737b77e8338abb`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Status, ADRs, Android-Workflow und Rotations-/GPS-Testmatrix erneut gelesen. Die bisher veraltete Kopfzeile, Ticketzahl und Prioritäten oben wurden auf diesen Abgleich korrigiert.
- **CI-Nachweis:** `fetch_commit_workflow_runs` für `49da9b1` liefert `[]`, filtert aber PR-getriggerte Läufe; **kein Beleg** für Push-Buildzustand. Der Workflow enthält drei getrennte Preflight-Schritte und einen Fehlerdiagnose-Summary-Step. Die konkrete erste Fehlerausgabe bleibt unzugänglich; keine Tests ausgeführt und keine APK-Freigabe.
- **Projektkontext:** Bekannte Main-/Schedule-Anforderungen zu GPS-Lücken, Rerouting, dunkellila Track und Rotation sind als #13/#14 und in der Regressionstestmatrix abgebildet. Kein vollständiger Live-Zugriff auf andere Projekt-Chats nachgewiesen.
- **Nächste technische Aktion:** Aktuellen Actions-Push-Lauf und dessen Preflight-Summary abrufen oder auf Checkout desselben SHA `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` ausführen. Danach reproduzierten Fehler minimal beheben; #13/#14 als ausführbare Tests umsetzen. Keine Änderung an Signing oder Secrets.


## Automatischer Entwicklungsabgleich – 18.11.2026, 21:15 MEZ

- **Repository:** Neuester vor diesem Lauf ermittelter Commit `f3877c1` auf `main`; elf offene Issues, keine offenen PRs. README, Roadmap, ADR, GPS-/Rotations-Testmatrix, Preflight und Android-Workflow erneut gelesen.
- **Dokumentationskorrektur:** Den widersprüchlichen historischen Hinweis „neun offene Issues“ explizit als überholten Snapshot gekennzeichnet; keine Änderung an App- oder Signing-Code.
- **CI-Nachweisgrenze:** Die kombinierte Commit-Status-Abfrage für `f3877c1` liefert keine Statusmeldungen. Das ist **kein** Beweis für einen erfolgreichen oder fehlgeschlagenen Push-Workflow. Aktuelle Push-Joblogs wurden nicht eingesehen; die drei Preflight-Befehle wurden in diesem Lauf nicht ausgeführt. Kein grüner Build und keine APK-Freigabe.
- **Projektkontext:** Bekannte Fehler #13 (Rotation) und #14 (GPS-Spur) bleiben offen; vollständiger Zugriff auf andere Chatverläufe ist nicht nachgewiesen.
- **Nächster konkreter Schritt:** Preflight-Befehle auf dem exakten `main`-Checkout ausführen bzw. Actions-Push-Joblog beschaffen, danach erste reproduzierbare Ursache beheben. Anschließend instrumentierten Activity-Recreation-Test und GPS-Track-State-Regression für #13/#14 implementieren.

## Automatischer Entwicklungsabgleich – 18.11.2026, 22:24 MEZ

- **Baseline:** vor diesem Lauf `4a85cd3`; elf offene Issues, keine offenen PRs. README, Roadmap, ADR, CI-Workflow, Preflight-Skripte und Regressionstestplan gelesen. Aus dem sichtbaren Projektkontext bleiben #13 (Rotation) und #14 (GPS-Spur) die kritischen funktionalen Fehler.
- **Kleine Korrektur:** `d92373b` behandelt in der Emulator-Navigationszielsuche zusätzlich Androids `visible-to-user="false"` und Groß-/Kleinschreibungsvarianten von `displayed`/`enabled`. `2316363` ergänzt zwei passende Offline-Regressionstests. Keine Änderungen an App-GPS, Routing oder Signing; diese Bugs bleiben offen.
- **CI-Grenze:** Der ältere Workflow-Lauf [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) hat einen fehlgeschlagenen `preflight`-Job und übersprungene Build-Jobs. Der Abruf des Preflight-Joblogs (`134378837918`) scheiterte; damit ist die konkrete Fehlerzeile nicht belegt. Der Commit-Workflow-Connector erfasst nur PR-Läufe, nicht zuverlässig Push-Läufe. **Neue Tests nicht ausgeführt, kein grüner Build nachgewiesen, keine APK-Installationsfreigabe.**
- **Nächste Aktion:** Aktuellen Push-Lauf samt Jobschritten/Logs oder Step Summary abrufen und zuerst den tatsächlich fehlgeschlagenen Preflight-Befehl reparieren. Danach instrumentierte Rotationstests (#13) und deterministische GPS-/Rerouting-Tracktests (#14) implementieren. Andere Chats sind nicht vollständig live einsehbar; keine Chat-Synchronisierung behauptet.


## Automatischer Entwicklungsabgleich – 18.11.2026, 23:18 MEZ

- **Repository:** vor der Änderung HEAD `ae27c1a`; README, Implementierungs-Roadmap, zentraler Status, Rotations-/GPS-Testplan, CI-Workflow, Preflight-Skripte und letzte Commits geprüft. 11 offene Issues, keine offenen PRs.
- **Fix:** `5ba377e` normalisiert Leerzeichen vor/nach den Android-UI-Hierarchieattributen `displayed`, `visible-to-user` und `enabled`. Dadurch können Werte wie `" FALSE "` nicht als sichtbar/aktiv durchrutschen. Produktivnavigation unverändert.
- **Regressionen:** `296efce` ergänzt zwei Python-Tests für unsichtbare doppelte Labels und deaktivierte Elterncontainer mit Whitespace. Tests sind committed, aber in diesem Lauf nicht ausgeführt.
- **CI-Nachweis:** Die commitbezogene Workflow-Abfrage für `296efce` liefert keine PR-Läufe und ist kein Beleg für Push-CI. Der ältere Lauf 44179252791 hat `preflight=failure`, `build-and-test=skipped`; keine Aussage über neuen HEAD. Keine APK-Installationsfreigabe.
- **Chat-Abgleich:** Bekannte Anforderungen aus Main/Schedule (Rotation #13, GPS-Spur #14, Rerouting, MVV) berücksichtigt; vollständige Live-Einsicht in alle Chats nicht möglich.
- **Nächster Schritt:** Neue Python-Tests und alle drei Preflight-Schritte tatsächlich ausführen, Push-Workflow-ID und Fehlerausgabe beschaffen. Anschließend #13/#14 mit deterministischen Android-/GPS-Regressionen bearbeiten.

## Automatischer Entwicklungsabgleich – 19.11.2026, 05:23 MEZ

- **HEAD vor Änderung:** `78ea9e0`; elf offene Issues, keine offenen PRs. README, Projektstatus, Rotations-/GPS-Regressionstestplan, Workflow und MainActivity-State gelesen.
- **Arbeitspaket:** `docs/ACTIVITY-RECREATION-STATE-CONTRACT.md` neu angelegt (Commit `ac9b3e6`): konkrete Eigentümer-/Lebensdauer-Matrix für Routen, Track, Auswahl, Einstellungen und asynchrone Callbacks; atomarer ViewModel-Migrationsplan und reproduzierbare `ActivityScenario.recreate()`-Abnahme. Warnung vor inkonsistentem `rememberSaveable`-Teilfix.
- **Tests/CI:** Keine Tests ausgeführt. Commit-Statusabfrage für `78ea9e0` lieferte keine Statuschecks; Push-Actions-Lauf nicht nachgewiesen. Keine APK-Freigabe.
- **Nächster Schritt:** Instrumentierten roten Rotationstest auf Basis des neuen Vertrags implementieren; danach zusammenhängenden Navigationszustand in ViewModel auslagern, GPS-/Rerouting-Regressionen nachziehen.
- **Chats:** Bekannte Meldungen aus Main/Schedule berücksichtigt; kein vollständiger Live-Zugriff auf alle Chats.
