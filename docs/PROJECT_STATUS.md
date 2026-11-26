## ABSCHLUSSRUNDE – 26.11.2026 (alle Tickets geschlossen)

- **Commit:** `4e95f8d` auf `main` (lokaler HEAD = `origin/main`). Die vorherige CI-Gate-Änderung ist in `035bcdb` enthalten.
- **Issues:** Alle 11 offenen Tickets (#3, #4, #5, #6, #7, #8, #9, #10, #11, #13, #14) wurden nach Abschlusskommentar geschlossen. Live-Abfrage: **0 offene Issues**.
- **Vollständiger GitHub-Actions-Nachweis:** [Android CI #45813033231](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45813033231) (`workflow_dispatch`, `test_level=release`) auf `4e95f8d` = **success**; alle sechs Jobs `success`: `preflight` (9 Schritte), `build-and-test` (15 Schritte), `mvv-live-probe`, `door-to-door-live-probe`, `emulator-smoke-test` (inkl. Golden-Vergleich), `release-acceptance`.
- **Artefakte:** `mein-weg-debug-apk-not-update-guaranteed`, `mein-weg-signed-update-apk` (stabil signiert mit `CN=MeinWeg2`), `emulator-test-results`. Version: `versionCode=597` (`github.run_number`), `versionName=0.2.0_build597` – per `aapt dump badging` aus dem heruntergeladenen Artefakt geprüft.
- **Signierter Release-Workflow:** [Lauf #45814284873](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45814284873) – das neue `gate`-Job war **success** (nachgewiesene `release-acceptance` für denselben Commit). Der anschließende Signierungsschritt scheiterte ausschließlich an **nicht konfigurierten `MEIN_WEG_KEYSTORE_*`-Secrets**. Kein Keystore und keine Secrets wurden angelegt, überschrieben oder veröffentlicht. Für eine signierte AAB/Release-APK ist eine einmalige Eigentümerkonfiguration erforderlich.
- **Lokale Prüfungen:** 97 Unit-Tests, 32 Instrumentierungstests, Lint 0 Errors/41 Warnings, Preflight-Guards, Golden-Vergleich – alle grün. Physisches Gerät (SM-G970F/Android 31) und echte Fußwegabnahme bleiben offen; die Ticket-Akzeptanzkriterien wurden über API-35-Emulator und deterministische Tests erfüllt.
- **Nächste Schritte (außerhalb des vereinbarten Umfangs):** Release-Keystore-Secrets konfigurieren und signierten AAB-Lauf wiederholen; Geräte-/Fußwegabnahme und Store-Freigabe.

---

## Entwicklungsrunde – 26.11.2026, Tickets [01]–[11] (Umsetzung)

- **Neuer State-Owner:** `NavigationSessionViewModel.kt` hält Konfiguration, Ziel, Auswahl, Route, Tab, Request-Generations und Off-Route-Evidenz Activity-scoped; `MainActivity` nutzt ihn über delegierte Properties. Beide ViewModels überleben Rotation; Prozessende bleibt bewusst nicht persistiert. Explizites Session-Ende löscht die Spur (`end_walking_session`).
- **GPS-Spur (#14/#6):** `TraceFix` hat optionale monotone `elapsedRealtimeNanos`; `WalkedTrace` nutzt sie für Ordnung/Lücken und lehnt gemischte Zeitbasen ab. `CurrentLocationRepository.request()` verwirft alte getCurrentLocation-Fixes, `isNewer` fail-closed bei gemischten Zeitbasen. Karte zeigt Einzelfix-Punkte statt sie zu verwerfen, kein künstliches Verbinden über Lücken.
- **Navigation (#3/#4):** Karte bekommt nur `navigationLocation`; Heading/Kamera ohne gültige Richtung kein 70-m-Offset; `RouteProgress.Projection.fraction` wird von Karte und Abbiegungen gemeinsam genutzt; 180°/270°-Tests. EFA-Kandidaten tragen Koordinaten/Stop-ID, `search()` reicht sie ans Routing durch; Geocoder-Fallback lehnt mehrere gleichnamige Stopps ab.
- **Zeit (#5):** `EfaTime` löst DST-Lücke/Doppelstunde via `ZoneRules`, EFA-`rtTime` vor `time`, absolute Instants in `JourneyOption`, Mitternacht/Tageswechsel in Auswahl- und Jetzt-Filtern.
- **Architektur/UI/Test/Doku (#7/#9/#10/#11):** Golden-Screenshots unter `design/golden/`, `scripts/compare-golden.py` im Release-Workflow, Datenschutzhinweis, Roadmap-Statuskopf, OSM-Attribution.
- **Lokale Verifikation (ABI api35-Emulator, JDK 17, Gradle 8.13):** `:app:testDebugUnitTest` **97 Tests, 0 Fehler, 0 Errors, 0 Skipped**; `:app:lintDebug` **0 Errors / 41 Warnings**; `:app:assembleDebug` erzeugte APK; `:app:connectedDebugAndroidTest` **32 Tests, 0 Fehler** (inkl. Rotation-/Session-State, OSM-Attribution, explizites Session-Ende, monotone GPS-Zeit). `python3 scripts/test-ci-concurrency.py` und `bash scripts/preflight.sh` grün; Golden-Vergleich gegen `design/golden/` lokal grün. **Lokale Ergebnisse, kein GitHub-Actions-Nachweis.**
- **Bekannte Grenze:** Physisches Gerät (SM-G970F, Android 31) und echte Fußwege sind nicht abgenommen; Golden-Screenshots stammen aus dem API-35-Emulator. Ein Release-Lauf mit `test_level=release` steht nach diesem Push aus.

---

## Entwicklungsrunde – 26.11.2026, Ticket [02] CI-01

- **Ursache:** `release.yml` konnte unabhängig von den Android-Release-Gates signieren und nutzte eine veraltete globale Concurrency-Gruppe mit Abbruch laufender Läufe.
- **Fix:** Neuer `gate`-Job prüft per Check-Runs-API eine erfolgreiche `release-acceptance` **für exakt den zu signierenden Commit**; `signed-release` hat `needs: gate`. Concurrency jetzt events-/ref-basiert mit `cancel-in-progress: false`. `permissions: actions: read` ergänzt.
- **Branch-Schutz:** `main` erfordert jetzt erfolgreiche Checks `preflight` und `build-and-test` für PRs (`strict: true`); Force-Pushes und Löschen gesperrt.
- **Guard:** `scripts/test-ci-concurrency.py` prüft den neuen Release-Vertrag statisch (4 neue Assertions); lokal grün. YAML validiert.
- **Laufender Nachweis:** Release-Lauf `45803904803` (`workflow_dispatch`, `test_level=release`) – `preflight`, `mvv-live-probe`, `door-to-door-live-probe` bereits `success`; `build-and-test`/`emulator-smoke-test` liefen zum Dokumentationszeitpunkt noch.
- **Keine** Änderung an Signierungs-Secrets oder Keystore-Inhalten.

---

## Entwicklungsabgleich – 26.11.2026, 11:37 MEZ (grüner CI-Lauf nach Veröffentlichung)

- **Live-Befund:** GitHub meldet `visibility=public` und `license=Apache-2.0`. Der vorherige 2-Sekunden-Preflight-Abbruch mit `steps=[]` ist nach der Veröffentlichung **nicht** mehr reproduzierbar: [Android CI #45800089593](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45800089593) auf `c323364` = **success** (5m 8s).
- **Jobs:** `preflight=success` mit **9 Schritten**, `build-and-test=success` mit **15 Schritten**; `mvv-live-probe`, `door-to-door-live-probe`, `emulator-smoke-test` und `release-acceptance` sind bei einem normalen Push erwartungsgemäß `skipped`. Die historische Annahme „kein erfolgreicher Actions-Nachweis" ist damit überholt.
- **Artefakt:** `mein-weg-debug-apk-not-update-guaranteed` (10.620.066 Byte) heruntergeladen nach `.work/artifacts/45800089593/`; `apksigner verify --print-certs` bestanden. Signatur: `CN=MeinWeg2, OU=Android, O=MeinWeg2`, SHA-256 `7240b34b…a0534ed`. Damit sind die stabilen CI-Signierungs-Secrets funktionsfähig.
- **Freigabegrenze:** Ein grüner Push-Build ist **keine** Release-/Installationsfreigabe. Instrumentierte Tests, Emulator-Smoke-Test, Live-Probes und die Release-Akzeptanz liefen in diesem Lauf nicht; #13/#14 und die visuelle OSM-Attribution bleiben offen.
- **Nächster Schritt:** Release-Lauf (`workflow_dispatch`, `test_level=release`) oder Versionstag ausführen, um Emulator-, Live-Probe- und Signatur-Gates vollständig abzudecken; danach #13/#14 auf API 35.

---

## Entwicklungsabgleich – 26.11.2026, 11:18 MEZ (öffentliche Sichtbarkeit und Freigabegrenzen)

- **Live-Repository:** GitHub meldet `visibility=public` auf `main`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. Letzter vor diesem Lauf abgefragter HEAD: `b9b47c7` (nach History-Rewrite). Alte Commit-SHAs aus vorangegangenen Berichten können durch den Rewrite ungültig sein.
- **Konsistenzfix:** README behauptete weiterhin „privat“; in `fe44305d` an die aktuellen Repository-Metadaten angepasst. Die Sicherheitsprüfung in `PUBLIC_RELEASE_READINESS.md` bleibt als historischer Prüfbericht bestehen. **Public ist keine Release-/APK-Freigabe.**
- **Tests und CI:** Nur Dokumentation geändert, keine Tests ausgeführt. Commit-Status-Abfrage für `b9b47c7` lieferte `statuses=[]`, also **keinen** erfolgreichen CI-Nachweis. Letzter dokumentierter Actions-Run `45795582864` scheiterte vor auslesbaren Schritten; Runner-/Billing-Ursache administrativ zu klären. Lokale Tests aus früherem Audit sind keine aktuelle Actions-Freigabe. Keine APK-Installationsfreigabe.
- **Nächste Priorität:** #8 Actions-Runner/Setup anhand eines aktuellen Runs diagnostizieren, dann #13/#14 aktive Navigation mit Route/Ziel/Tab/Puffer und GPS-Spur bei echter Rotation auf API-35-Emulator testen. Andere Main-/Schedule-Chats nicht als vollständige Live-Verläufe abrufbar.

---

## Entwicklungsrunde – 26.11.2026 (History-Rewrite abgeschlossen)

- **Freigabe umgesetzt:** Die alte private E-Mail-Adresse wurde mit `git-filter-repo` in **396** Commit-Metadaten durch `354472984+jochenkunkel@users.noreply.github.com` ersetzt. Zusätzlich wurden verbliebene Vorkommen in historischen Dokumenten ersetzt.
- **Umfang:** 597 Commits über `main`, `audit/2026-11-16-review` und `fix/route-progress-navigation` neu geschrieben. Stand unmittelbar nach dem Rewrite: `51c2e60` (vor Dokumentationsanpassung).
- **SHA-Mapping:** 21 Docs mit `commit-map` aktualisiert; alle 13 Commit-Links in `docs/` auf existierende neue SHAs geprüft (0 Fehler). Alte SHAs sind nicht mehr verlässlich gültig.
- **Verifikation:** Alle Autor-/Committer-Adressen in allen Refs sind Noreply-Adressen; `git grep` über alle Branches findet keine alte Adresse. Keine Änderung an Secrets, Signierung oder Lizenz.
- **Nächster Schritt:** Force-Push aller drei Refs, danach Sichtprüfung auf GitHub und `public`-Umsstellung durch den Eigentümer. Details: `docs/PUBLIC_RELEASE_READINESS.md` §12.

---

## Entwicklungsrunde – 26.11.2026 (Eigentümerfreigaben umgesetzt)

- **Freigaben:** Apache-2.0 gewählt → `LICENSE` + `NOTICE` angelegt, README/Decision-Doc aktualisiert. History-Rewrite der alten privaten E-Mail-Adresse → GitHub-Noreply-Adresse freigegeben (Ausführung folgt als separater, dokumentierter Schritt). OSM-Attribution/Datenschutz (Issue #11) und Einschränkung signierter Artefakte ebenfalls freigegeben.
- **Code/UI:** `WalkingMiniMap` blendet jetzt sichtbar „© OpenStreetMap contributors" ein (`testTag("osm_attribution")`); instrumentierter Regressionstest `walkingMiniMapShowsOpenStreetMapAttribution` ergänzt (nicht ausgeführt). Neue Policy-Dokumentation `docs/PRIVACY-AND-PROVIDER-POLICY.md`.
- **CI-Artefakte:** `mein-weg-signed-update-apk` wird nur noch bei manuellem Release/Versionstag hochgeladen; statischer Guard in `scripts/test-ci-concurrency.py` ergänzt. Debug-APK bleibt.
- **Verifiziert:** `python3 scripts/test-ci-concurrency.py` exit 0 (neuer Guard grün), `bash scripts/preflight.sh` exit 0, `python3 scripts/test-emulator-navigation.py` exit 0, YAML valide. `.work/tools/gradle-8.13/bin/gradle :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` = `BUILD SUCCESSFUL in 59s`. **Kein Emulator-Lauf, keine Gerätefreigabe.**
- **Nächster Schritt:** Freigegebenen History-Rewrite auf separatem Pfad ausführen, Commit-SHA-Mapping prüfen, Docs verlinken, force-pushen und anschließend `public`-Umsstellung veranlassen.

---

## Entwicklungsrunde – 26.11.2026 (Release-Readiness, CI-Blocker, Offline-Tests)

- **Baseline:** Frisch geklont, `main=96065c9`, Working Tree sauber, 115 Dateien, 3 Remote-Branches, keine Tags, Repo `PRIVATE`, `licenseInfo=null`. README, CHAT-START,PROJECT_STATUS, Issues (11 offen), Workflows, Build-Dateien, Manifest und jüngste Commits geprüft.
- **Security-Audit:** Gitleaks 8.30.1 über **590 Commits** (Worktree + Historie): 1 Treffer, ein False Positive (`diagnose=failure` im Runbook), Text umformuliert. Keine Keystores, Tokens, Passwörter, `.env`-Dateien, APKs/AABs oder Binär-Secrets in Historie oder Baum. Ein Adressbeispiel im Cockpit-Plan durch Musteradresse ersetzt. History-Rewrite ursprünglich für Secrets nicht nötig; später jedoch freigegeben zur Entfernung der privaten E-Mail-Adresse (siehe Eintrag oben). Bericht: `docs/PUBLIC_RELEASE_READINESS.md`.
- **CI-Ursache gefunden:** `scripts/test-emulator-navigation.py:39` hatte seit `5df2a0fb` (17.11.) eine fehlende schließende Klammer → SyntaxError im Preflight-Job. Korrigiert. Damit ist erstmals ein **lokal reproduzierbarer** Preflight-Blocker belegt; historische Actions-Job-Logs bleiben nicht abrufbar.
- **Ausgeführte lokale Tests (JDK 17, Gradle 8.13, SDK 35):** `python3 scripts/test-ci-concurrency.py` exit 0; `python3 scripts/test-emulator-navigation.py` exit 0 (30 Tests grün); `bash scripts/preflight.sh` exit 0; `python3 -m unittest discover -s scripts -p 'test_*.py'` exit 0 (6 Tests). **Lokaler Gradle-Lauf (Gradle 8.13, JDK 17, SDK 35):** `:app:testDebugUnitTest` 85 Tests, 0 Fehler, 0 Errors, 0 Skipped (14 XML-Reports); `:app:assembleDebug` erzeugte `app-debug.apk` (29,6 MB); `apksigner verify --print-certs` bestanden (Debug-Zertifikat `C=US, O=Android, CN=Android Debug`, erwartungsgemäß **keine** stabile Update-Signatur). `gradle --no-daemon :app:lintDebug` ebenfalls erfolgreich (`BUILD SUCCESSFUL in 11m 31s`, 53 Tasks, **0 Lint-Errors / 37 Warnings**). Alle lokalen Werkzeuge und Scan-Reports liegen persistent unter `.work/` (git-ignoriert). **Keine GitHub-Actions-Bestätigung, kein Emulator-/Gerätelauf, keine Installations-/Updatefreigabe.**
- **Neuer CI-Nachweis nach Push:** [Android CI #45795582864](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45795582864) auf `df9bb04` = `failure`; `preflight` startete 09:49:43 UTC und endete 09:49:45 UTC mit **`steps=[]`**, alle fünf Folgejobs `skipped`. Der unabhängige Diagnose-Workflow lief ebenfalls fehl (Run `45795582767`). Ein 2-Sekunden-Fehler ohne Schritte ist **nicht** der behobene Syntaxfehler (dieser hätte ausführbare Schritte erzeugt); die externe Runner-/Billing-Blockade besteht weiterhin. Das Repo wurde **nicht** auf `public` umgestellt.
- **Rechts-/Datenschutzprüfung:** Kein `LICENSE`, GitHub `license: null` → **Blocker**, Eigentümerentscheidung (Apache-2.0 empfohlen). Eine private E-Mail-Adresse stand in **396** historischen Commits → Privatsphärenentscheidung nötig (History-Rewrite nur mit Freigabe). Keine echten Kalender-/GPS-/Adressdaten im Repo; ein Adressbeispiel anonymisiert. OSM-Attribution fehlt in `WalkingMiniMap` (`TileSourceFactory.MAPNIK`), OSRM-/Geocoder-Nutzungsbedingungen offen (Issue #11). Details in `docs/PUBLIC_RELEASE_READINESS.md` §8a.
- **Signierung:** Unverändert. Keystore/Passwörter ausschließlich in Actions Secrets; Fork-PRs erhalten keine Signierung. Bestehende Secrets nicht gelöscht oder überschrieben.
- **Öffentliche Struktur neu:** `.gitignore` gehärtet, `.gitattributes`, `.github/SECURITY.md`, Issue-/PR-Templates, `CONTRIBUTING.md`, `AGENTS.md`, `docs/LICENSE-DECISION.md`; README/CHAT-START verlinkt.
- **Offene Freigaben:** (1) Lizenzentscheidung des Eigentümers (Empfehlung Apache-2.0, kein `LICENSE` angelegt); (2) ausdrückliche Zustimmung zur Umstellung auf `public` – **nicht erfolgt**; (3) Entscheidung zur öffentlichen Sichtbarkeit signierter Artefakte; (4) Actions-Budget/Runner administrativ prüfen.
- **Nächste Schritte:** Nach Freigabe `public` schalten, echten CI-Status prüfen, vollständige Kette Preflight→Kotlin→Unit→Lint→Debug-APK→Instrumentierung→Emulator→Signatur ausführen; danach #13/#14 mit aktiver Route bei Rotation.

---

## Entwicklungsabgleich – 26.11.2026, 10:20 MEZ (ungenaue GPS-Fixes nach Recreation)

- **Baseline:** `main=5b90d31` vor Änderung; README, Roadmap, Status, Testcode, GPS-Filter, Android-Workflow, Issues/PRs und jüngste Commits geprüft. Elf offene Issues, keine offenen PRs. Vollständige Main-/Schedule-Chats nicht live zugänglich.
- **Neuer Regressionstest:** [3f3e0e8](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/3f3e0e8e160bc911b0e13f9c8e46cbf257e5b179) ergänzt `rejectedGpsFixAfterRecreationDoesNotEraseRecordedHistory`. Nach Activity-Neuerstellung wird ein GPS-Fix mit 500 m Genauigkeit abgewiesen (Filterlimit 35 m); die zuvor akzeptierte Spur muss auch nach einer zweiten Recreation unverändert bleiben. Das testet nur GPS-Historie, **nicht** echte Rotation, aktive Route oder Navigationseinstellungen.
- **CI:** Test nicht ausgeführt; Commit-Workflow-Abfrage liefert `[]`, ist jedoch auf PR-Runs beschränkt und sagt nichts über Push-CI. Historischer Run #548: Preflight fehlgeschlagen, keine Jobschritte verfügbar. Kein grüner Buildnachweis, keine APK-Freigabe.
- **Nächste Priorität:** Neuesten Push-Run in Actions-UI samt Runner-Setup untersuchen; API-35-Instrumentierung tatsächlich ausführen. Anschließend #13 mit aktiver Route, Verbindung, Ziel, Tab und Puffer bei Portrait–Landscape–Portrait absichern.

---

## Entwicklungsabgleich – 26.11.2026, 09:25 MEZ (GPS-History-Limit bei Recreation)

- **Baseline:** main vor Änderung `80da79d`; README, Implementierungs-Roadmap, Status, Android-Workflow, GPS-ViewModel/WalkedTrace, Instrumentierung, letzte Commits und Issues/PRs geprüft. Elf offene Issues, keine offenen PRs. Andere Main-/Schedule-Chats nicht vollständig live zugänglich.
- **Testpaket:** [7f9c25f](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/7f9c25fbb39543f772984605afb85339cd9dcb5f) ergänzt `boundedGpsHistorySurvivesRecreationWithoutRestoringDiscardedFixes`. Nach 1501 akzeptierten, zeitlich getrennten Fixes darf das ViewModel nur die letzten 1500 behalten; `ActivityScenario.recreate()` darf den verworfenen ersten Fix nicht zurückholen. Dies prüft die produktive `retainLast(1500)`-Grenze, nicht echte Rotation oder Route/Tab/Puffer.
- **CI:** Test nicht ausgeführt. Die verfügbare Commit-Workflow-Abfrage erfasst nur PR-Läufe und eignet sich nicht als Push-CI-Nachweis; letzter dokumentierter Run #548 scheiterte im Preflight ohne auslesbare Schritte. Kein grüner Build, keine APK-Freigabe.
- **Nächste Priorität:** neuesten Push-Run und Runner-Setup über GitHub-Actions-UI prüfen, API-35-Instrumentierung ausführen; anschließend aktiven Navigationszustand bei echter Rotation absichern. Keine spekulativen CI-/Signing-Änderungen.

---

## Entwicklungsabgleich – 26.11.2026, 08:22 MEZ (GPS-Lücke nach Activity-Recreation)

- **Repo-Abgleich:** main vor Änderung `dc92b28`; README, Implementierungs-Roadmap, zentraler Status, Android-Workflow, GPS-Instrumentierung, Issues und letzte Commits geprüft. Elf offene Issues, keine offenen PRs. Andere Projekt-Chats nicht vollständig live abrufbar.
- **Neues Testpaket:** [9b555ce](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/9b555ce4cd41b9d2c9d320564bde72e939f46a63) ergänzt `gpsGapAfterRecreationStartsNewSegmentWithoutLosingHistory`. Ein Fix vor `ActivityScenario.recreate()` und ein zeitlich weit entfernter Fix danach müssen getrennte Segmente bilden; eine zweite Recreation muss beide erhalten. Ergänzt den bestehenden Fall, in dem beide Fixes *vor* der Recreation eintreffen.
- **Tests/CI:** Instrumentierung nicht ausgeführt; kein API-35-Emulator oder grüner Push-Build über die verfügbaren Abfragen bestätigt. Historischer Lauf #548 bleibt fehlgeschlagen, dessen Ursache nicht belegbar; aktuelle Push-Runs konnten hier nicht zuverlässig abgefragt werden. Keine APK-Installationsfreigabe.
- **Nächste Priorität:** Tatsächliche API-35-Ausführung und CI-Runner-Setup prüfen; anschließend aktive Route, Verbindung, Ziel und Puffer bei echter Portrait–Landscape–Portrait-Rotation absichern. Keine blind vorgenommenen CI-/Signing-Änderungen.

---

## Entwicklungsabgleich – 26.11.2026, 07:16 MEZ (GPS-Segmentgrenzen bei Recreation)

- **Live-Baseline:** main vor Änderung `f485fe0`; README, Implementierungs-Roadmap, Projektstatus, Android-Workflow, Testmatrix, MainActivity, offene Issues und letzte Commits geprüft. Elf offene Issues, keine offenen PRs. Andere Projekt-Chats Main/Schedule nicht vollständig live zugänglich.
- **Kleines Testpaket:** [17a3b36](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/17a3b36ab56aec4cab8c52dcdacd31f69b474959) erweitert `NavigationTraceRecreationTest`: zwei valide GPS-Fixes mit mehr als 120 Sekunden Abstand müssen vor und nach `ActivityScenario.recreate()` zwei getrennte Segmente bleiben. Damit wird neben der Punktpersistenz auch verhindert, dass eine Rotation eine erfundene Verbindung über eine GPS-Lücke erzeugt. Keine echte Orientation-Transition und keine aktive Journey im Test.
- **CI:** Android-CI #548 erneut über Jobs abgefragt: `preflight=failure`, `steps=null`, fünf Folgejobs übersprungen; konkrete Ursache nicht belegbar. Für den neuen Commit liegt kein ausgeführter JVM-/Emulatortest oder grüner Actions-Lauf vor. Keine APK-Installationsfreigabe.
- **Nächste Priorität:** API-35-Instrumentierung tatsächlich ausführen, danach ROT-01 mit aktiver Journey, Ziel, Tab und Puffer erweitern. Runner-/Setup-Fehler im neuesten Actions-Push-Lauf administrativ ermitteln; keine blinden Workflow-/Signing-/Secrets-Änderungen.

---

## Entwicklungsabgleich – 26.11.2026, 03:20 MEZ (explizites Session-Ende)

- **Repo-Baseline:** main vor Änderung `16477c7`; README, Roadmap, Lifecycle-Vertrag, bestehende Android-Tests, elf offene Issues, keine offenen PRs geprüft. Vollständige Main-/Schedule-Chats nicht live abrufbar.
- **Testpaket:** [60d855a](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/60d855a734d73f2b6497e6bbdeaa578efccce685) ergänzt einen instrumentierten Test: aufgezeichnete GPS-Spur explizit löschen, Activity neu erstellen, leere Spur verifizieren und einen neuen Fix mit zurückgesetztem Zeitstempel akzeptieren. Dies sichert den Unterschied zwischen Rotation (Spur behalten) und explizitem Session-Ende (Spur löschen) ab. Kein echter Orientierungstest, keine aktive Route/Verbindung geprüft.
- **CI:** Commit-Statusabfrage für `60d855a` ergab `statuses=[]`; kein positiver Buildnachweis. Instrumentierung nicht ausgeführt, bisher dokumentierter Android-CI-Preflight #548 fehlgeschlagen. Keine APK-Freigabe.
- **Nächste Priorität:** API-35-Instrumentierung ausführen; danach echte Portrait–Landscape–Portrait-Transitions mit aktiver Route/Ziel/Puffer. Runner-Setup-Annotations zur Preflight-Ursache prüfen, keine Workflow-/Secrets-Änderung auf Verdacht.

## Entwicklungsabgleich – 26.11.2026, 02:22 MEZ (wiederholte Activity-Recreation)

- **Live-Prüfung:** main vor Änderung `7282ffc`; README, Roadmap, Testmatrix, NavigationTraceViewModel, Instrumentierungstest, Issues und PRs geprüft. Elf offene Issues, keine offenen PRs; Main-/Schedule-Chats nicht vollständig live zugänglich.
- **Codepaket:** [0ab1a56](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/0ab1a56489b731380afaf730f6c1a1544c964fc8) erweitert `NavigationTraceRecreationTest`: Nach erster Activity-Neuerstellung einen dritten GPS-Fix anhängen und nach zweiter Neuerstellung die fortlaufende Spur sowie die ursprüngliche ViewModel-Instanz prüfen. Dies testet wiederholte Recreation, **nicht** echte Orientierungswechsel, Route/Ziel/Puffer oder Prozessneustart.
- **Tests/CI:** Instrumentierungstest nicht ausgeführt. Letzter Commit-Status vor Änderung: `statuses=[]`, kein Buildnachweis. Zuletzt dokumentierter Android-CI-Lauf #548 mit Preflight-Fehler; Ursache weiterhin nicht belegt. Keine APK-Freigabe.
- **Nächste Priorität:** Test auf API-35-Emulator ausführen und Portrait→Landscape→Portrait mit aktiver Route, Ziel, Puffer und Spur ergänzen. Runner-Setup-Annotations prüfen; keine Workflow-/Signing-/Secrets-Änderung auf Verdacht.

## Entwicklungsabgleich – 26.11.2026, 00:24 MEZ (GPS-Regressionen erweitert)

- **Live-Baseline:** main vor Änderung `816a3ec`; README, Roadmap, Status, NavigationTraceViewModel, WalkedTrace, Testdatei, Issues und PRs geprüft. Elf offene Issues, keine offenen PRs. Main-/Schedule-Chats nicht vollständig live verfügbar.
- **Kleines Codepaket:** Commit [df860a8](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/df860a831f8985b114453ccd6411fc440df7d6b0) ergänzt zwei JVM-Regressionen im ViewModel: lange GPS-Unterbrechung erzeugt separate Segmente und ein ungültiger Fix verbindet sie nicht; expliziter Session-Reset erlaubt anschließend einen neuen Zeitstempel-Ursprung. Bestehende Tests bleiben erhalten.
- **Test-/CI-Grenze:** Tests nicht ausgeführt. Commit-Status-API liefert keine verifizierte Android-Actions-/Emulator-Abnahme; bisheriger dokumentierter Preflight-Lauf #548 fehlgeschlagen. Keine APK-Installationsfreigabe.
- **Nächste Priorität:** Instrumentierter ActivityScenario.recreate()-Test mit tatsächlich nichtleerem GPS-Track, danach Rotation/Rerouting und Lifecycle-Erhalt von Route, Tab, Ziel und Zeitpuffer. Runner-Setup-/Policy-Fehler anhand der Actions-UI/Annotations prüfen, nicht durch spekulative Workflow-Änderungen.

## Entwicklungsabgleich – 25.11.2026, 23:20 MEZ (GPS-ViewModel angebunden; Lifecycle-Abnahme offen)

- **Repository geprüft:** neuester Commit auf `main` ist [244dd77](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/244dd77200cb343e89525664f5d4ffd5cc41c42a) (22:18 MEZ), davor [b27f563](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/b27f56312bea5f5241a4985d715f11b563170a24) mit der produktiven Anbindung des Activity-scoped `NavigationTraceViewModel`. README, Roadmap, Chat-Start, Navigation-Session-Vertrag, offene Issues/PRs und Commitliste abgeglichen. Elf offene Issues, keine offenen PRs. Main-/Schedule-Chats nicht vollständig live abrufbar; keine unbekannten Anforderungen unterstellt.
- **Wichtige Korrektur des älteren Status:** Die Aussage unten, die ViewModel-Anbindung sei noch ausstehend, ist seit `b27f563` überholt. Der GPS-Track wurde laut Commit und Übergabe in den ViewModel-Pfad verlegt; eine **ausgeführte** Rotation-/Rerouting-Abnahme ist weiterhin nicht nachgewiesen. Die übrigen Compose-gebundenen Navigationswerte (Route, ausgewählte Verbindung, Tab, Ziel und Puffer) sind weiterhin separat zu überprüfen; #13 und #14 bleiben offen.
- **Test-/CI-Gate:** Für `244dd77` meldet die GitHub-Commit-Status-API `statuses=[]`; dies ist **kein** grüner Build. Zuletzt dokumentierter CI-Lauf [#548](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45632319389) mit Preflight-Fehler und übersprungenen Folgejobs. Keine JVM-/Instrumentierungstests in diesem Abgleich ausgeführt. Keine APK-Freigabe.
- **Nächstes kleines Paket (P0):** Instrumentierten `ActivityScenario.recreate()`-Test mit nichtleerem GPS-Track ergänzen, der nach Recreation dieselben Segmente prüft; anschließend Rotation und Rerouting. Unabhängig davon Route/Verbindung/Tab/Ziel/Puffer gezielt auf Lifecycle-Verlust untersuchen und den gemeinsamen Navigationszustand in kleinen Schritten migrieren. CI-Runner-Setup/Policy anhand der Job-Annotations klären, ohne Workflow-/Signing-/Secrets-Änderungen auf Verdacht.
- **Arbeitsgrenze:** Dieser Lauf aktualisiert den gemeinsamen Entwicklungsplan; kein Produktcode geändert. GitHub-Commit-Statuschecks ersetzen keine Actions-Run-Abfrage und kein Android-Testprotokoll.

## Entwicklungsabgleich – 25.11.2026, 20:20 MEZ (Lifecycle-Integration noch ausstehend)

- **Live-Abgleich:** jüngster sichtbarer Commit auf `main`: [c9031ba](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/c9031baa36324ac48f70ed21db3e3e9aa27eacd1) (19:26 MEZ). README, Roadmap, Navigation-Session-Vertrag, Android-Workflow, offene Issues und letzte Commits erneut gelesen. #13 (Rotation) und #14 (Spurverlust) sind weiterhin offen; die Issue-Suche liefert insgesamt elf offene Einträge. Andere Main-/Schedule-Chatverläufe nicht vollständig live abrufbar.
- **Stand:** `NavigationTraceViewModel` und JVM-Tests wurden laut Commits `7347e89`, `553b71d`, `ac407f9` hinzugefügt. Die vorherige Übergabe `c9031ba` beschreibt sie ausdrücklich als **noch nicht in MainActivity integriert**. Keine Belege für eine bereits erfolgte Activity-scoped Einbindung; die produktive Lifecycle-Reparatur darf nicht als abgeschlossen gelten.
- **CI-Nachprüfung:** [Android CI #548](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45632319389): `preflight=failure`, Job-ID `139268505628`, `steps=null`, `logs_url=null`; fünf abhängige Jobs übersprungen. Die PR-begrenzte Commit-Workflow-Abfrage für `c9031ba` lieferte keine Läufe und beweist **nicht**, dass kein Push-Workflow existiert. Kein neuer grüner Build nachgewiesen; kein APK-Go.
- **Nächstes kleines Codepaket:** In `MainActivity` genau einen Activity-scoped `NavigationTraceViewModel` beziehen und den bisherigen `remember`-Trace vollständig durch dessen State ersetzen. GPS-Callback soll ausschließlich `accept(...).retainLast(1500)` im StateHolder verwenden; beide `WalkingMiniMap`-Aufrufe sollen dieselben Segmente erhalten. Keine Neuanlage des ViewModel bei Rekombination und kein Track-Reset bei Rerouting. Danach Instrumentierung mit nichtleerer Spur und `ActivityScenario.recreate()` sowie echter Rotation; Journey-/Route-State separat migrieren. Erst nach ausgeführten Tests Issues schließen.
- **Blockade/Abhilfe:** Für die CI-Ursache fehlen Job-Setup-/Runner-Annotations bzw. administrativer Einblick in Actions-Policy, Runner-Zuweisung und Kontingente. Keine Spekulation über Gradle, keine Workflow-/Signing-/Secrets-Änderung auf Verdacht. JVM-/Android-Tests weiterhin nicht ausgeführt nachgewiesen.

## Entwicklungsabgleich – 24.11.2026, 23:20 MEZ

- **Vor Änderung:** main=ab175f77; elf offene Issues, keine offenen PRs; README, Roadmap, Projektstatus, Regression-Matrix, Workflow, RouteProgress-Code und Tests geprüft. Andere Projekt-Chats nicht vollständig live abrufbar.
- **CI:** jüngster verifizierter Android-CI-Lauf [#523](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45469315567) = failure; der Preflight hat null Schritte und Folgejobs sind übersprungen. Kein belegter Kotlin-Testfehler; keine APK-Freigabe.
- **Kleines Testpaket:** Commit `514dbe3` ergänzt JVM-Regressionen für eine ausschließlich aus Duplikaten bestehende Route (keine Projektion/Heading/Turn) und für ein nahezu null-langes erstes Segment vor einem gültigen Segment. Ziel ist deterministisches Verhalten bei degenerierter OSRM-Geometrie; die Tests sind **nicht ausgeführt**.
- **Nächster Schritt:** GitHub-Actions-Runner-Zuweisung anhand Setup-/Billing-/Policy-Meldungen klären, danach `gradle :app:testDebugUnitTest :app:lintDebug` auf JDK 17/SDK 35 ausführen. Anschließend #13/#14 mit echter GPS-Spur und Rotation instrumentiert prüfen. Keine Signing-/Secrets-Änderungen.

## Entwicklungsabgleich – 24.11.2026, 20:20 MEZ

- **Repository vor Änderung:** `main=abf3f8a6d3cbeb81c3b590b6fb3745a2ed6d05d9`; elf offene Issues, keine offenen PRs. README, Roadmap, Projektstatus, Android-Workflow, RouteProgress-Code und -Tests sowie letzte Actions-Läufe geprüft. Andere Projekt-Chats nicht als vollständige Live-Verläufe abrufbar.
- **CI:** [Android #520](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45447585407) auf `abf3f8a` = `failure`; Preflight-Job `138635494499` hat null abrufbare Schritte, fünf Folgejobs `skipped`. Direkter Job-Logabruf: GitHub `404 BlobNotFound`. Ursache der Jobinitialisierung nicht belegt; keine Änderungen an Runner/Signing/Secrets oder CI-Gates auf Verdacht.
- **Codebefund und Fix:** `RouteProgress.project` prüfte zwar die GPS-Position auf endliche Koordinaten, nicht jedoch die Polyline-Stützpunkte. Ein NaN/Infinity-Stützpunkt kann zu einer nichtendlichen Projektion führen und Folgeentscheidungen für Heading/Fortschritt verfälschen. In `1912046` werden Routen mit nichtendlichen Koordinaten früh verworfen; in `1080772` Regressionen für NaN-Breitengrad, Infinity-Längengrad und Heading ergänzt. Kleiner, isolierter Fail-closed-Fix; **keine Behauptung einer vollständigen GPS- oder OSM-Korrektur**.
- **Testgrenze:** Die Kotlin-Tests sind committed, aber nicht ausgeführt nachgewiesen. Nach dem Commit CI-Status prüfen; bei weiter fehlendem Runner lokal `gradle :app:testDebugUnitTest :app:lintDebug` auf einem Android-SDK-35/JDK-17-Checkout ausführen. Keine APK-Installationsfreigabe ohne grünes Gate.
- **Nächste Priorität:** Runner-Setup-Meldung im GitHub-Actions-UI bzw. durch Repo-Admin prüfen; fachlich #13/#14: Rotation mit nichtleerem Track und GPS-Lücken/Rerouting instrumentiert verifizieren.

## Entwicklungsabgleich – 24.11.2026, 16:20 MEZ

- **Verifiziert vor Änderung:** `main=c43e9911a813fd9bc0cd2009d8f9767ffc135e84`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Projektstatus, Preflight-Runbook, Android-Workflow, letzte Commits und Actions-Läufe erneut abgeglichen. Vollständige andere Projekt-Chats nicht live abrufbar; keine unbestätigten neuen Anforderungen übernommen.
- **Neuester belastbarer CI-Befund:** [Android CI #517](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45413145878), Push auf `c43e991` um 15:21 MEZ, Ergebnis `failure`. [Job-API](https://api.github.com/repos/jochenkunkel/mvv-mobility-assistant/actions/runs/45413145878/jobs): `preflight` Job-ID `138512069879`, `failure`, **0 zurückgelieferte Schritte**; fünf Folgejobs `skipped`. Vorheriger [#516](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45413127466) ebenfalls `failure`. Aus null Schritten folgt **nicht**, welcher Befehl scheiterte oder ob ein Runner überhaupt initialisiert wurde. Insbesondere kein belegter Fehler der neu hinzugefügten UI-Tests.
- **Gezielte Diagnose statt spekulativem Fix:** In der Actions-Weboberfläche zuerst die Job-Setup-Fehlermeldung/Annotations für Job `138512069879` prüfen. Wenn keine Steps/Logs verfügbar sind, Runner-Zuteilung, Actions-Kontingente/Billing und Repository-Policy durch Berechtigte prüfen. Nur wenn tatsächliche Steps vorliegen, Exitcode und ersten fehlerhaften Befehl isolieren. Keine Änderungen an Workflow-Gates, Signing oder Secrets ohne Befund.
- **Nächste fachliche Arbeit:** Den vorhandenen #13-Test `arrivalBufferSelectionSurvivesActivityRecreation` und Dashboard-Test auf API-35-Emulator ausführen, bevor ein Fix behauptet wird; danach eine nichtleere Route plus drei akzeptierte GPS-Punkte und echte Orientation-Transitions für #13/#14 ergänzen. Die vorhandenen Tests sind committed, aber **nicht erfolgreich ausgeführt nachgewiesen**.
- **Freigabe:** Kein bestätigter grüner Build, kein erfolgreiches Instrumentierungsergebnis und keine APK-Installationsfreigabe.

## Entwicklungsabgleich – 24.11.2026, 15:20 MEZ

- **Live-Stand vor Änderung:** `main=b35a19d35cf02fdb169a1d6c0becf34704d04b06`; elf offene Issues, keine offenen PRs. README, Roadmap, aktuelle Instrumentierungstests und letzte Commits geprüft. Andere Chats nicht vollständig live abrufbar.
- **Implementiert:** Instrumentierter Regressionstest `arrivalBufferSelectionSurvivesActivityRecreation()` in `MeinWegComposeUiTest.kt` (Commit `34d3228`). Er stellt im Fixture 60 Minuten Ankunftspuffer ein, erzwingt `ActivityScenario.recreate()` und verlangt anschließend weiterhin „60 Min. früher“. Der Test isoliert den Konfigurationsverlust aus #13 zusätzlich zum bestehenden Dashboard-Smoke-Test; er deckt **noch nicht** Route/GPS-Spur/echte Orientation-Transitions ab.
- **Nachweisgrenze:** Test nicht ausgeführt; ein Scheitern ist angesichts der dokumentierten `remember`-Zustände möglich und wäre ein reproduzierbarer Red-Test, keine Regression durch die Testergänzung. Der zuletzt bekannte Android-CI-Lauf #514 scheiterte im Preflight mit null abrufbaren Schritten. Kein grüner Build, keine APK-Freigabe.
- **Nächste Arbeit:** Emulator-Test ausführen und Befund protokollieren; bei reproduziertem Verlust Ankunftspuffer über saveable State oder einen geeigneten StateHolder retten. Danach Fixture mit nichtleerer GPS-Spur/Route für #13/#14 und Runner-Setup-Logs untersuchen. Keine Änderungen an Signing/Secrets.

## Entwicklungsabgleich – 24.11.2026, 13:20 MEZ

- **Live-Abgleich:** `main=48120b6bdbc8e94fbb14a4e857d2a51283594061`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Lifecycle-/GPS-Regression-Spezifikation, aktuellen Instrumentierungstest und letzte Commits gelesen. Andere Projekt-Chats nicht als vollständige Live-Verläufe abrufbar; bekannte Anforderungen #13/#14 stammen aus Issues und vorhandener Übergabe.
- **Gezielte Codeprüfung des neuen Tests:** `activeWalkingDashboardSurvivesActivityRecreation()` startet per `mein_weg_test_fixture` eine MVV-Testverbindung, öffnet das Dashboard, wechselt auf `tab_journey`, ruft `ActivityScenario.recreate()` auf und prüft anschließend die Existenz von `walking_dashboard` und `journey_summary`. **Wichtige Abdeckungslücke:** Der Test prüft weder eine nachweislich nichtleere OSRM-Route noch akzeptierte GPS-Punkte A/B/C, Geometrie, Fortschritt, Konfiguration oder einen tatsächlichen Portrait→Landscape→Portrait-Wechsel. Er ist ein erster UI-Lifecycle-Smoke-Test, **kein** vollständiger ROT-01-/Issue-#13-Nachweis.
- **CI-Nachweisgrenze:** Die GitHub-Commit-Status-Abfrage für `48120b6` lieferte `statuses=[]`; dies belegt weder grüne noch ausgebliebene GitHub-Actions-Checks. Die zuletzt dokumentierten Android-Läufe #510/#511 scheiterten im Preflight ohne abrufbare Jobschritte. Keine Tests hier ausgeführt, kein grüner Build, keine APK-Freigabe.
- **Nächstes implementierbares Paket:** Fixture um eine nichtleere Route und mindestens drei akzeptierte Trackpunkte erweitern; vor/nach `recreate()` auf Modell- und UI-Ebene stabile IDs, Trackreihenfolge und Fortschritt prüfen. Danach echte Orientation-Transitions und GPS-Ausfall/Reroute-Regressionen ergänzen. Den aktuellen Smoke-Test nicht als behobenen Rotationsfehler markieren. CI-Runner-/Setup-Logs unabhängig davon untersuchen.

## Entwicklungsabgleich – 24.11.2026, 11:20 MEZ

- **Live-Repository:** Vor Änderung `main=575d12959cc5eb037e60123748964fbaa3f93aa4`; offene PRs: 0; #13 und #14 weiter offen. README, Roadmap, Status, Lifecycle-State-Contract, GPS-Rotations-Matrix, letzte Commits und Actions geprüft. Andere Projekt-Chats nur im verfügbaren Kontext, nicht vollständig live lesbar.
- **Neue CI-Nachweise:** [Android CI #510](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45364347987) auf `617928a` und [#511](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45364369275) auf `575d129` sind beide `failure`. Bei #511: Preflight-Job `138342151819` startete 09:21:10 UTC und endete 09:21:12 UTC; die API liefert **null Schritte**; alle fünf Folgejobs `skipped`. Kein Beweis für einen Fehler in einem bestimmten Preflight-Kommando, kein grüner Build.
- **Konkrete CI-Triage:** In GitHub Actions für #511 zuerst Job-/Setup-Annotations, Runner-Zuweisung und ggf. Billing/Runner-Verfügbarkeit prüfen. Falls Step-Logs existieren, den ersten fehlerhaften Befehl mit Exitcode isolieren; bei weiterhin null Schritten keine spekulativen Änderungen an CI-Skripten/Gates. Neue Kotlin-Geometrietests von `617928a` sind weiterhin **nicht ausgeführt nachgewiesen**.
- **Fachliche Priorität:** #13 `ActivityScenario.recreate()` mit aktiver Route und nichtleerem Track; danach #14 deterministische Track-Segmenttests bei Fix-Ausfall/Sprung/Rerouting. Keine Änderung an Signing/Secrets, keine APK-Installationsfreigabe.

## Entwicklungsabgleich – 24.11.2026, 10:20 MEZ

- **Repository:** Vor Änderung `main=d565dc1`; elf offene Issues, keine offenen PRs. README, Roadmap/Status, RouteProgress und Tests geprüft. Bekannte Chat-Anforderungen #13 Rotation und #14 GPS-Spurverlust bleiben offen; vollständige andere Chatverläufe nicht live zugänglich.
- **Neuer CI-Nachweis:** [Android CI #509](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45355344537) auf `d565dc1` ist `failure`; Preflight-Job `138311658580` hat **0 zurückgelieferte Schritte**, fünf abhängige Jobs sind `skipped`. Ursache weiterhin nicht bewiesen; kein Android-Buildnachweis.
- **Arbeitspaket:** Drei neue Kotlin-JUnit-Regressionstests in `RouteProgressTest.kt` für doppelte OSM-Polyline-Stützpunkte, nichtendliche GPS-Längengrade/einpunktige Route und strikte Offroute-Heading-Schwelle. Code-Commit `617928a`. Diese Tests decken Geometrie-Randfälle ab, **nicht** Activity-Rotation oder GPS-Trackpersistenz.
- **Testgrenze:** Tests statisch gegen `RouteProgress.kt` geprüft, aber hier nicht ausführbar verifiziert. Kein grüner CI-/APK-Nachweis; keine Installationsfreigabe.
- **Nächste Schritte:** CI-Jobinitialisierung/Runner-Logs für #509 über Actions-UI untersuchen; unabhängig davon instrumentierten `ActivityScenario.recreate()`-Test mit aktiver Route und Track für #13 und anschließend deterministische GPS-Trackkontinuität für #14 implementieren.

## Entwicklungsabgleich – 24.11.2026, 00:20 MEZ

- **HEAD und Aufgaben:** `main=5882040f818ef3adb2984fbf67e9e808e70b0477`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Status und Emulator-Helfer/Regressionstests gelesen. #13 (Rotation) und #14 (GPS-Spurverlust) bleiben offen. Andere Chats nur über verfügbaren Kontext, nicht als vollständiger Live-Verlauf.
- **Neue gezielte CI-Prüfung:** Android-Push-Lauf [#504](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45268631001) hat den fehlgeschlagenen Preflight-Job **138022172035**; die fünf abhängigen Jobs sind `skipped`. Die Jobantwort enthält keine Schritte. Direkter Logabruf für den Job lieferte `404 BlobNotFound` (24.11., 00:20 MEZ). Die Ursache ist weiterhin **nicht** bewiesen; weder Runner-Start noch Skriptfehler lassen sich daraus ableiten.
- **Diagnose als nächstes:** Actions-Weboberfläche für Job 138022172035 aufrufen, die **Setup-Job-Logs und Runner-Zuweisung** prüfen; falls auch dort keine Schritte existieren, Repository-Einstellungen für Actions/Runner, Billing/Kontingent und GitHub-Status prüfen. Erst nach nachgewiesenen Step-Logs die drei Preflight-Kommandos separat ausführen. Zusätzlich `python3 -u scripts/test-emulator-navigation.py` auf einem echten Checkout ausführen; hier scheiterte `git ls-remote` an DNS-Auflösung von `github.com`.
- **Nachweise:** Keine Codeänderung, keine Tests ausgeführt, kein grüner Build und keine APK-Freigabe. Keine Änderung an Signing/Secrets/CI-Gates. Fachlich als nächstes ausführbare Regressionen für #13/#14, nicht weitere ungetestete ADB-Testvarianten.

## Entwicklungsabgleich – 23.11.2026, 21:24 MEZ

- **Repository-Abgleich:** Vor Änderung letzter Commit `4713b49`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Entscheidungen, Status und Preflight-Diagnoseworkflow geprüft. #13 (Rotation) und #14 (GPS-Spurverlust) weiterhin offen. Andere Projekt-Chats nicht vollständig live zugänglich; keine neuen Anforderungen erfunden.
- **Arbeitspaket:** Neuer Offline-Regressionstest `test_tap_timeout_propagates_without_retry` in `scripts/test-emulator-navigation.py` (Commit `5701f41`). Bei Timeout des abschließenden ADB-Taps wird `TimeoutExpired` weitergereicht; es erfolgt **kein** zweiter Tap, weil der erste trotz Timeout beim Gerät angekommen sein könnte.
- **Nachweisgrenze:** Testquelltext und `tap-emulator-navigation.py` statisch abgeglichen, aber Python-/Android-Tests nicht ausgeführt. Der Connector liefert für Commit-SHA nur PR-bezogene Workflow-Läufe, daher ist der aktuelle Push-CI-Status nicht nachgewiesen. Keine APK-Freigabe, keine Änderung an Signing/Secrets oder CI-Gates.
- **Nächster Schritt:** `python3 -u scripts/test-emulator-navigation.py` ausführen; aktuelle Push-Actions-Läufe und Diagnoselogs/Exitcodes über Actions-UI oder `gh` prüfen. Danach die konkrete CI-Blockade beheben und instrumentierte Regressionstests für #13/#14 priorisieren.

## Entwicklungsabgleich – 23.11.2026, 20:21 MEZ

- **Live-Abgleich:** Letzter Commit vor Änderung `bb36b5b`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Entscheidungen, Status, Diagnoseworkflow und Emulator-Helfer/Tests geprüft. #13 Rotation und #14 GPS-Spurverlust bleiben offen. Andere Chats nicht vollständig live abrufbar; keine zusätzlichen Anforderungen unterstellt.
- **Implementiert:** Zwei gezielte Regressionstests für ADB-Timeouts in `scripts/test-emulator-navigation.py`: Bei Timeout des UI-Dumps darf weder die Hierarchie gelesen noch getippt werden; bei Timeout des XML-Lesens darf kein Tap folgen. Commit `0b4514f`. Keine Produktcode-, Signing- oder Secrets-Änderung.
- **Verifikation:** Neuen Testcode gegen das unveränderte `tap-emulator-navigation.py` statisch abgeglichen; ausführbare Python-/Android-Tests in dieser Umgebung nicht durchgeführt. Die commitbezogene Workflow-Abfrage für `0b4514f` ergab keine PR-Läufe, belegt jedoch **nicht**, dass kein Push-Workflow lief. Kein grüner Build und keine APK-Freigabe.
- **Nächster Schritt:** `python3 -u scripts/test-emulator-navigation.py` auf dem neuen Head ausführen, aktuelle Push-Actions-/Diagnoseartefakte in Actions-UI/CLI überprüfen, dann CI-Preflight-Fehler anhand echter Exitcodes beheben. Fachlich #13/#14 mit instrumentierten Zustandsregressionen priorisieren.

## Entwicklungsabgleich – 23.11.2026, 19:19 MEZ

- **Repo:** Vor Änderung letzter gefundener Commit `da888e3`; elf offene Issues, keine offenen PRs. README, Roadmap, ADRs, Status, Emulator-Helfer und Diagnoseworkflow geprüft. #13/#14 weiterhin offen. Andere Chats nur über verfügbaren Kontext, nicht vollständig live zugänglich.
- **Verbesserung:** `scripts/tap-emulator-navigation.py` begrenzt nun alle drei ADB-Aufrufe (UI-Dump 30 s, XML-Lesen und Tap jeweils 15 s), damit ein blockierter Emulator Test-/CI-Läufe nicht unbegrenzt anhält. Commit `c14e26c`. Regressionstest prüft alle drei Timeout-Parameter (`0ebd95b`). Keine Änderungen an App-Produktcode, Signing oder Secrets.
- **CI-Nachweis:** Historischer Android-Lauf [#491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723) weiterhin Preflight fehlgeschlagen und fünf Folgejobs übersprungen; Artefaktliste leer. Die neuen `scripts/**`-Commits sollten den Diagnoseworkflow triggern, dessen tatsächlicher Start ist **noch nicht bestätigt**. Kein aktueller grüner Build oder APK-Freigabe.
- **Tests:** Quelltext statisch geprüft; Python-Tests und Android-Build hier nicht ausgeführt. Nächster Schritt: `python3 scripts/test-emulator-navigation.py` sowie neueste Actions-Push-Runs/Diagnoseartefakte prüfen; danach #13 Rotation und #14 GPS-Trackkontinuität instrumentiert absichern.

## Entwicklungsabgleich – 23.11.2026, 18:20 MEZ

- **Repository:** Vor Änderung `main=cdbcfd9`; elf offene Issues, keine offenen PRs. README, Roadmap, Entscheidungen, Status, Preflight-Diagnose und Emulator-Helfer abgeglichen. #13/#14 weiter offen. Andere Projekt-Chats nicht vollständig live zugänglich.
- **Implementiert:** Zusätzlicher Offline-Regressionstest in `scripts/test-emulator-navigation.py` prüft, dass ein fehlgeschlagener `adb shell input tap` den Fehler propagiert und nur ein Tap versucht wird. Commit [`fb270dc`](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/fb270dc905a380b9be8fc8de278e0d33a3580d02). Keine Änderung an Produktcode, Secrets oder Signing.
- **CI:** Bekannter Lauf [#491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723) weiterhin Preflight fehlgeschlagen, fünf Folgejobs übersprungen; keine auswertbaren Schritte. Der neue Test-Commit unter `scripts/**` sollte den separaten Diagnoseworkflow per Push triggern, **dessen Start/Ergebnis ist nicht nachgewiesen**. Die commitbezogene Connector-Abfrage deckt nur PR-Läufe ab und lieferte leer.
- **Nachweise:** Quelltext statisch mit `tap-emulator-navigation.py` abgeglichen; Python-/Android-Tests hier nicht ausgeführt, kein grüner Build und keine APK-Installationsfreigabe.
- **Nächster Schritt:** Aktuellen Push-Run des Diagnoseworkflows über Actions-UI/CLI samt Artefakt und Exitcodes prüfen, konkrete Preflight-Ursache beheben; danach instrumentierte Rotation-/GPS-Tracktests #13/#14.

## Entwicklungsabgleich – 23.11.2026, 16:16 MEZ

- **Live-Abgleich:** Jüngster gefundener Commit `9c5adf0`; elf offene Issues, keine offenen PRs. README, Roadmap, Status, Android-CI, unabhängigen Diagnoseworkflow und Preflight-Guard gelesen. #13 (Rotation) und #14 (GPS-Spurverlust) weiterhin offen. Vollständige Main-/Schedule-Chatverläufe hier nicht live lesbar; keine unbestätigten Anforderungen übernommen.
- **CI-Befund:** Actions-Run [#491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723) liefert über die Jobschnittstelle weiterhin `preflight=failure` und fünf `skipped`-Jobs. Die Jobantwort enthält keine verwertbaren Schrittangaben. Das belegt **nicht**, ob der Runner vor oder nach Checkout/Scriptstart scheiterte. Das aktuelle `android.yml` besitzt drei getrennte Preflight-Kommandos und einen Fehlerartefakt-Upload; `preflight-diagnostics.yml` enthält einen unabhängig auslösbaren Diagnoselauf. Ein solcher Diagnoselauf wurde hier **nicht** gestartet.
- **Konkreter nächster Schritt:** Unabhängigen Workflow `preflight-diagnostics.yml` per Actions-UI/CLI `gh workflow run preflight-diagnostics.yml --repo jochenkunkel/mvv-mobility-assistant --ref main` starten; Run-ID und Runner-/Checkoutstatus ermitteln, `preflight-diagnostics`-Artefakt auswerten. Bei erneut fehlenden Schritten/Artefakten zuerst GitHub-Runner-/Actions-Berechtigungen und Jobinitialisierung prüfen. Bei vorhandenem Exitcode den ersten reproduzierten Preflight-Fehler mit Test korrigieren. Danach #13/#14 instrumentiert absichern.
- **Ausführung/Freigabe:** Quelltext und Konfiguration statisch geprüft; keine Python-/Android-Tests ausgeführt, kein grüner Build nachgewiesen, keine APK-Installationsfreigabe. Keine Signing-, Secrets- oder Gate-Änderungen.

## Entwicklungsabgleich – 23.11.2026, 03:24 MEZ

- **Live-Repository:** `main` vor Änderung `053e7cf` (23.11.2026, 00:19 UTC); elf offene Issues, keine offenen PRs. README, Roadmap, Entscheidungen, CI und Emulator-Navigationstests geprüft. #13 Rotation und #14 GPS-Trackverlust offen. Andere Projekt-Chats nicht vollständig live lesbar; nur bekannte dokumentierte Anforderungen verwendet.
- **Neuer verifizierter CI-Befund:** Der zuvor nur als Warteschlange bekannte Push-Lauf [#490](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45088092833), Run-ID `45088092833`, ist abgeschlossen: `preflight=failure` (Job `137411172360`), fünf Folgejobs `skipped`. Die Jobschritt-Schnittstelle liefert eine leere Liste, Joblogs weiterhin HTTP 404 `BlobNotFound`; die Artefaktliste ist **leer**. Damit ist weder ein konkreter Preflight-Fehler noch ein erfolgreiches Build nachgewiesen. Der fehlende Diagnoseartefakt-Upload könnte an einem frühen Job-/Checkout-Problem liegen; das ist eine Hypothese, keine Feststellung.
- **Diagnose/Arbeitsplan:** Auf einem zugänglichen Checkout exakt `053e7cf` die drei unabhängigen Prüfungen `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` mit Exitcodes reproduzieren. Alternativ unabhängigen Diagnoseworkflow ausführen. Erst nach konkretem Traceback gezielten Fix committen. Danach #13 Activity-Neuerstellung und #14 GPS-Trackkontinuität instrumentiert testen. Keine Secrets/Signing/Gates geändert.
- **Test-/Freigabegrenze:** In diesem Lauf keine ausführbaren Tests, kein grüner Android-Build und keine APK-Installationsfreigabe.

## Entwicklungsabgleich – 22.11.2026, 22:24 MEZ

- **Repository:** Vor Änderung letzter Commit `e2be55c`; elf offene Issues, keine offenen PRs. README, Roadmap, Projektstatus, Android-Workflow und Navigationstests geprüft. #13 (Rotation) und #14 (GPS-Track) bleiben offen. Andere Projekt-Chats nicht vollständig live zugänglich; keine zusätzlichen Aussagen erfunden.
- **Neues Arbeitspaket:** Zwei End-to-End-Unit-Regressionen für `scripts/tap-emulator-navigation.py` ergänzt (Commit `8c18aa2`): gültige UI-Hierarchie löst genau einen Tap am berechneten Mittelpunkt aus; fehlgeschlagener `uiautomator dump` beendet die Aktion vor XML-Lesen und Tap. Keine Produktcode- oder Signing-Änderung.
- **Test-/CI-Grenze:** Quelltext und Aufrufreihenfolge gegen das aktuelle Hilfsskript geprüft, aber Python-Tests **nicht ausgeführt**. Aktueller Android-Push-Build/Exitcodes nicht verifiziert; keine APK-Installationsfreigabe.
- **Nächster Schritt:** `python3 -u scripts/test-emulator-navigation.py` auf Commit `8c18aa2` ausführen, danach aktuelle Push-Actions-Logs/Preflight-Artefakte prüfen. Fachlich #13 Rotation und #14 GPS-Trackpersistenz mit instrumentierten Zustandsregressionen vorziehen.

## Entwicklungsabgleich – 22.11.2026, 21:20 MEZ

- **GitHub-Abgleich:** Vor Änderung jüngster Commit `dd46231`; elf offene Issues, null offene PRs. README, Roadmap, Entscheidungen, Projektstatus, Navigationstest und Android-Workflow gelesen. #13 (Rotation) und #14 (GPS-Track) weiterhin offen; andere Projekt-Chats nur im verfügbaren bisherigen Kontext, nicht vollständig live abrufbar.
- **Implementiert:** Zwei Offline-Regressionstests sichern die End-to-End-Sicherheitsregel des Emulator-Tap-Helfers ab: Bei defektem UI-XML oder fehlender Zielbeschriftung darf `main()` niemals `adb shell input tap` ausführen. Commit `618dcf5`; keine Produktionscodeänderung.
- **Nachweise und Grenze:** Tests wurden nicht ausgeführt: lokaler Git-Checkout scheitert an DNS (`Could not resolve host: github.com`). Die commitbezogene Actions-API bildet Push-Runs nicht zuverlässig ab. Aktueller grüner Build, APK und Installationsfreigabe nicht nachgewiesen.
- **Nächster Schritt:** `python3 scripts/test-emulator-navigation.py` auf dem Commit ausführen und aktuellen Push-Preflight/Diagnoseartefakte prüfen; danach echte Rotation-/GPS-Zustandsregressionen zu #13/#14 priorisieren. Keine Änderungen an Secrets, Signing oder Gates.

## Entwicklungsabgleich – 22.11.2026, 20:20 MEZ

- **GitHub erneut geprüft:** Neuester vor diesem Arbeitspaket gefundener Commit `a321260`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Architekturentscheidungen, Status, Android-CI, Preflight- und Emulator-Navigationstests abgeglichen. Andere Projekt-Chats nicht vollständig live zugänglich; bekannte #13/#14-Anforderungen unverändert.
- **Kleines implementiertes Regressionstestpaket:** Zwei zusätzliche Offline-Tests in `scripts/test-emulator-navigation.py` für negative bzw. beschädigte Android-UI-Bounds: ungültige Duplikate überspringen, bei ausschließlich ungültigen Bounds sicher abbrechen statt blind zu tippen. Commit `55de07f`. Die Produktnavigation selbst wurde nicht verändert.
- **Verifikation:** Testcode und bestehender `center_for_label`-Parser statisch abgeglichen; keine tatsächliche Python-/Android-Testausführung. Commitbezogene Workflow-Run-Schnittstelle deckt Push-Läufe nicht zuverlässig ab. Aktueller grüner Build und APK-Freigabe **nicht** belegt.
- **Nächste Priorität:** Aktuellen Actions-Push-Lauf und Preflight-Exitcodes/Artefakte beschaffen; danach #13 Rotation und #14 GPS-Trackkontinuität mit echten Zustands-/Instrumentierungstests absichern. Keine Änderung an Signing, Secrets oder Release-Gates.

## Entwicklungsabgleich – 22.11.2026, 19:20 MEZ

- **Repository erneut verifiziert:** GitHub-Commitsuche meldet `40fa34f` (16:23:41 UTC) als jüngsten Commit; elf offene Issues und null offene PRs. README, Roadmap, Architekturentscheidungen, zentralen Status, Android-CI, Preflight-Skript, statische Regressionstests und Triage-Dokumentation gelesen. Die offenen Praxisfehler [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) und [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14) sind unverändert. Vollständige andere Projekt-Chats nicht live abrufbar; nur dokumentierte Anforderungen übernommen.
- **Neue unabhängige Verifikation:** GitHub-Jobschnittstelle liefert für den historischen Run `44940824673` erneut `preflight=failure`, fünf Folgejobs `skipped`. Die commitbezogene Run-Abfrage für `40fa34f` liefert eine leere Liste, ist aber auf PR-Runs beschränkt und belegt **keinen** fehlenden oder grünen Push-Build. Es liegen weiterhin weder aktuelle Preflight-Exitcodes noch ein vollständiger Push-Build-Nachweis vor. Keine Tests ausgeführt.
- **Konkreter Arbeitsplan:** (1) Mit `gh run list --repo jochenkunkel/mvv-mobility-assistant --workflow android.yml --branch main --limit 10` die neueste Run-ID und den Commit erfassen; (2) `gh run view <RUN_ID> --repo jochenkunkel/mvv-mobility-assistant --log-failed` und `gh run download <RUN_ID> --repo jochenkunkel/mvv-mobility-assistant -n android-preflight-failure` ausführen, falls verfügbar; (3) ersten nichtnulligen Exitcode samt Log und SHA festhalten; (4) gezielten Fix und reproduzierbaren Regressionstest implementieren, danach CI neu prüfen. Wenn Push-Logs fehlen, den dokumentierten unabhängigen Diagnose-Workflow nutzen. Parallel als risikoarmes fachliches Testpaket #13 zuerst Rotation/Activity-Neuerstellung mit aktivem Routenstatus, dann #14 Fix-Lücken/Neuzeichnung/Rerouting mit unveränderter GPS-Trackhistorie absichern. **Keine Änderung an Signing/Secrets/Gates und keine APK-Freigabe.**
- **Status dieses Laufs:** Ausschließlich dokumentierter, quellenbasierter Diagnose- und Testplan; kein behaupteter Codefix, keine lokale Testausführung, kein grüner Build.

## Entwicklungsabgleich – 22.11.2026, 17:23 MEZ

- **Live-Abgleich:** Neuester über GitHub-Commitsuche gefundener `main`-Commit weiterhin `67fbee7` (14:18:32 UTC); elf offene Issues und keine offenen PRs. README, Implementierungsroadmap, Architekturentscheidungen, Projektstatus, Android-Workflow, Preflight- und Navigationstests sowie Triage-Dokumentation gelesen. Bekannte Projektchat-Anforderungen #13 (Rotation) und #14 (GPS-Track) bleiben offen; vollständige andere Chatverläufe sind hier nicht live zugänglich.
- **Neuer konkreter Diagnosebefund:** Der historische Actions-Lauf `44940824673` bestätigt via Job-API: `preflight` Job-ID `136957405044` = `failure`; die fünf anderen Jobs = `skipped`. Der gezielte Abruf **dieses** Preflight-Joblogs endet mit HTTP 404 `BlobNotFound` (GitHub-Logredirect, 22.11.2026 ca. 17:23 MEZ). Dies belegt eine **nicht verfügbare Logdatei**, nicht die Ursache des Preflight-Fehlers. Die commitbezogene Workflow-Run-API listet nur PR-Runs und kann einen aktuellen Push-Build nicht bestätigen.
- **Quelltextbefund:** Die drei separaten Preflight-Schritte, Fehlerdiagnose und Artefakt-Upload sind im aktuellen `android.yml` vorhanden. Die Newline-Regression-Guards sind im Testquelltext vorhanden; **keine Tests ausgeführt**. Es gibt keine belastbare Grundlage für weitere blind vorgenommene CI-Fixes.
- **Exakter nächster Schritt:** Neueste Push-Run-ID und SHA über Actions-Weboberfläche bzw. `gh run list --workflow android.yml --branch main` ermitteln; Jobschritte und `android-preflight-failure`-Artefakt des **neuen** Laufs auswerten. Wenn Logs weiterhin fehlen: unabhängigen `preflight-diagnostics.yml`-Workflow ausführen oder die drei Einzelkommandos auf identischem SHA testen. Erst mit erstem Nonzero-Exitcode/Traceback gezielt reparieren; danach deterministische #13/#14-Tests. Keine Signing-/Secrets-/Release-Gate-Änderungen; keine APK-Freigabe.

## Entwicklungsabgleich – 22.11.2026, 15:18 MEZ

- **Live-Repositorium:** `main` vor diesem Abgleich `f1afc11` (neuester über GitHub-Commitsuche bestätigter Commit). README, Roadmap, Architekturentscheidungen, Handoff, #13/#14-Testplan, CI-Workflow, `scripts/preflight.sh`, `scripts/test-ci-concurrency.py` und bestehende Preflight-Triage erneut gelesen. Elf offene Issues, null offene PRs; seit `f1afc11` keine neueren Commits gefunden. Andere Projekt-Chats sind nicht vollständig live abrufbar; bekannte Rotation-/GPS-Anforderungen bleiben unverändert.
- **Gezielte technische Prüfung:** Die drei unabhängigen regulären Preflight-Kommandos und die Fehlerdiagnose inklusive Upload bei `failure()` sind im aktuellen Workflow vorhanden. Der statische Guard prüft außerdem Release-Gates, Exitcode-Zeilenumbrüche und ERR-Annotation. Dies ist ein **Quelltextabgleich**, keine erfolgreiche Ausführung; eine neue nachgewiesene Preflight-Fehlerursache liegt nicht vor. Weitere spekulative Workflow-Korrekturen unterbleiben.
- **Actions-/Testgrenze:** Ein aktueller vollständiger Push-Run-Status und die `android-preflight-failure`-Artefakte waren in diesem Lauf nicht abrufbar; ein externer Abruf der Actions-Seite schlug fehl. Der historische Lauf [#459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673) bleibt der letzte in den bisherigen Berichten mit Jobstatus belegte Fehl-Lauf, **nicht** ein Beleg für den aktuellen HEAD. Keine Tests lokal ausgeführt, kein grüner Build oder installierbare APK bestätigt.
- **Nächste ausführbare Priorität:** Im neuesten Android-CI-Push-Lauf auf `main` zuerst den Run-SHA und den Preflight-Job erfassen; `android-preflight-failure/results.txt` und `check-1.log` bis `check-3.log` sichern. Falls Artefakte fehlen, den unabhängigen `preflight-diagnostics.yml`-Workflow ausführen oder die drei dokumentierten Kommandos auf demselben SHA lokal testen. Erst nach einem konkreten Exitcode/Traceback einen gezielten Fix samt Regressionstest umsetzen; danach #13/#14. Keine Signing-/Secrets-/Release-Gate-Änderungen und keine Installationsfreigabe.

## Entwicklungsabgleich – 22.11.2026, 14:23 MEZ

- **Repo-Abgleich:** README, Roadmap, Architekturentscheidungen, Preflight-Workflow und Skripte, Issues und PRs geprüft; #13/#14 weiterhin offen. Vollständige Live-Verläufe anderer Projekt-Chats nicht zugänglich; bekannte Anforderungen berücksichtigt.
- **Arbeit:** Nachvollziehbare, nach Fehlerklasse gegliederte CI-Fehlerisolierung in [CI-PREFLIGHT-TRIAGE-2026-11-22.md](CI-PREFLIGHT-TRIAGE-2026-11-22.md) ergänzt (Commit `fd07c8d`). Beschreibt aktuelle Run-/Artefakt-Suche, lokale drei Einzelprüfungen, fehlende Belege und Freigabegrenzen. Keine spekulative Änderung am Produktionscode.
- **Tests/CI:** Dokumentation geändert; keine ausführbaren Tests oder neuen grünen Build nachgewiesen. Die Connector-Abfrage für Commits ist auf PR-Läufe beschränkt. Kein APK-Freigabenachweis. **Nächster Schritt:** ersten echten Nonzero-Exitcode aus aktuellem Preflight/Diagnose-Workflow sichern und Ursache beheben; anschließend #13/#14 testen.

## Entwicklungsabgleich – 22.11.2026, 12:19 MEZ

- **Repository:** README, Roadmap, Status, CI-Workflow, Preflight-Skript, Regressionstestmatrix, aktuelle Commits sowie offene Issues/PRs abgeglichen. Vor diesem Lauf HEAD `5874659`; elf offene Issues, keine offenen PRs. Bekannte Anforderungen #13/#14; andere Chatverläufe nicht live vollständig zugänglich.
- **Kleines Arbeitspaket:** Zusätzlichen Quelltext-Regressionstest für den Zeilenumbruch der GitHub-Actions-ERR-Annotation in `scripts/test-ci-concurrency.py` ergänzt (Commit `c721e1e`). Er schützt den zuletzt behobenen Escape-Fehler vor Wiedereinführung. Der Vergleich des frisch abgerufenen Testquelltexts mit `scripts/preflight.sh` ergibt eine Übereinstimmung. **Keine tatsächliche Python-Testausführung oder Android-CI-Bestätigung**; die ursprüngliche Preflight-Ursache bleibt unbekannt.
- **CI/APK:** Aktuelle Push-Workflow-Liste über den GitHub-Connector nicht vollständig abrufbar; bislang letzter belegter Fehl-Lauf [#459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673). Keine APK-Freigabe. Nächster Schritt: Preflight-Exitcodes aus einem aktuellen Lauf sichern und den ersten realen Fehler beheben; danach #13/#14 automatisieren.

## Entwicklungsabgleich – 22.11.2026, 11:17 MEZ

- **Repo erneut geprüft:** README, Implementierungsroadmap, Architekturentscheidungen, Projektstatus, Android-Workflow, Preflight-Skripte, fünf jüngste Commits, elf offene Issues und null offene PRs. Vollständige fremde Projekt-Chats nicht live abrufbar; bekannte Anforderungen #13/#14 bleiben offen.
- **Konkrete Korrektur:** Im ERR-Trap von `scripts/preflight.sh` verwendete die GitHub-Actions-Annotation ein doppelt escaptes `\\n`; `printf` schrieb deshalb ein literales `\n` statt eines echten Zeilenendes. Auf einfaches `\n` korrigiert (Commit `35b06c5`). Die Ausgabe lässt sich damit korrekt zeilenweise parsen. Die unbekannte ursprüngliche Preflight-Fehlerursache ist **nicht** behoben.
- **Prüfung:** Bash-`printf`-Verhalten lokal mit Byteausgabe verifiziert (doppelter Backslash: `61 5c 6e`, einfacher Backslash: `61 0a`); einfacher Shell-Syntax-Smokecheck durchgeführt. Kein vollständiger Repository-Checkout, kein Android-/GitHub-Runner-Test und kein grüner Build nachgewiesen. Push-Workflow-Läufe über die commitbezogene PR-Abfrage nicht zuverlässig sichtbar; bisher belegter Fehl-Lauf [#459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673). Keine APK-Freigabe.
- **Nächste Priorität:** Neueste Push-Actions-Läufe und `android-preflight-failure`-Artefakt prüfen, tatsächlichen Nonzero-Exitcode isolieren; anschließend #13 Rotation und #14 GPS-/Reroute-Regressionstests. Signing, Secrets und Gates unverändert.

## Entwicklungsabgleich – 22.11.2026, 09:22 MEZ

- **Repository geprüft:** README, Roadmap, Architekturentscheidungen, zentraler Status, Android-CI, Preflight- und Emulator-Testskripte, jüngste Commits, elf offene Issues und keine offenen PRs. Bekannte Projektchat-Anforderungen #13 (Rotation) und #14 (GPS-Spur/Rerouting) berücksichtigt; vollständige andere Chats sind nicht live zugänglich.
- **Implementiertes Arbeitspaket:** In `scripts/test-ci-concurrency.py` schützt ein zusätzlicher statischer Regressionstest die korrekte, newline-delimitierte Ausgabe der Preflight-Exitcodes in `results.txt` gegen den zuvor gefundenen doppelten Backslash. Commit `d949612`. Die eigentliche Ursache des Preflight-Fehlers ist weiterhin unbekannt.
- **CI-Belege und Grenzen:** Commitbezogene Workflow-Abfrage für `d949612` ergab keine PR-Workflow-Läufe (keine Aussage über Push-Läufe). Der frühere Fehl-Lauf #459 hat keine abrufbaren Artefakte. Kein neuer grüner Build oder tatsächliche Testausführung belegt; keine APK-Installationsfreigabe. Nächste Priorität: aktuelle Push-Actions-Läufe und `android-preflight-failure`-Artefakt beschaffen, ersten nachgewiesenen Nonzero-Exitcode beheben, danach #13/#14 implementieren. Signing, Secrets und Freigabegates unverändert.

## Entwicklungsabgleich – 22.11.2026, 08:25 MEZ

- **Repo-Abgleich:** README, Roadmap, Entscheidungen, Projektstatus, Android-Workflow, Preflight-Testskript, letzte fünf Commits, elf offene Issues und null offene PRs geprüft. Bekannte Main-/Schedule-Anforderungen #13/#14 berücksichtigt; vollständige andere Chats nicht live lesbar.
- **Konkreter Diagnosefehler behoben:** Im Android-Workflow enthielt `printf '%s => exit %s\\\\n'` zwei Backslashes vor `n`; Bash-`printf` schrieb dadurch einen literalen `\n` statt eines Zeilenumbruchs in `preflight-diagnostics/results.txt`. Korrigiert auf genau einen Backslash (`printf '%s => exit %s\\n'`), Commit `ba1f635`. Die Exitcodes sind künftig zeilenweise lesbar. Dies repariert **nur die Diagnoseausgabe**, nicht den bislang unbekannten ursprünglichen Preflight-Fehler.
- **Tests/CI:** Statischer Quelltextvergleich durchgeführt, keine lokale Testausführung und kein neuer grüner GitHub-Actions-Lauf belegt. Keine APK-Freigabe. Nächster Schritt: Diagnoseartefakt eines neuen Laufs abrufen, tatsächlichen Nonzero-Exitcode beheben, danach Rotation/GPS-Regressionstests #13/#14 implementieren.

## Entwicklungsabgleich – 22.11.2026, 07:16 MEZ

- **GitHub-Abgleich:** README, Roadmap, Architekturentscheidungen, Regressionstestmatrix, CI und Preflight-Skripte erneut gelesen. Letzte Commits vor diesem Lauf `5dc5169` und `ccc278e`; elf offene Issues, keine offenen PRs. Bekannte Anforderungen aus Main/Schedule (#13/#14) berücksichtigt; vollständige Chatverläufe nicht live zugänglich.
- **Kleines Testpaket:** `scripts/test-ci-concurrency.py` prüft jetzt zusätzlich, dass der reguläre Android-Preflight bei Fehlern weiterhin `android-preflight-failure` mit `preflight-diagnostics/` über `actions/upload-artifact@v4` hochlädt. Commits `a3f4c27` (Guard) und `fe64517` (Regex-Escape-Korrektur). Dies schützt die Beobachtbarkeit; die unbekannte eigentliche CI-Fehlerursache ist damit nicht behoben.
- **CI-Evidenz:** Run [#459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673) erneut abgefragt: `preflight=failure`, fünf weitere Jobs `skipped`, keine Artefakte. Commitbezogene Actions-Abfrage für `5dc5169` leer (erfasst Push-Läufe nicht zuverlässig). Kein neuer grüner Build verifiziert, keine lokalen Tests ausgeführt, keine APK-Freigabe.
- **Nächster Schritt:** Aktuellen Push-Lauf samt Fehlerartefakt abrufen, ersten tatsächlichen Nonzero-Exitcode isolieren und beheben; anschließend #13/#14 automatisiert testen. Signing/Secrets/Release-Gates unverändert.

## Entwicklungsabgleich – 22.11.2026, 06:22 MEZ

- **Repository:** HEAD vor Änderung `864356e`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, ADR, Regressionstestplan, Android-/Diagnoseworkflow und Preflight-Skripte geprüft. Bekannte Chat-Anforderungen berücksichtigt; vollständige andere Projekt-Chats nicht live zugänglich.
- **Konkrete CI-Verbesserung:** Regulärer Android-Preflight schreibt bei einem Fehler nun die drei unabhängigen Diagnosebefehle in getrennte `check-1.log` bis `check-3.log` sowie ihre Exitcodes in `results.txt` und lädt sie mit `if: failure()` als `android-preflight-failure` für 14 Tage hoch. Commit `ccc278e`. Bisherige Preflight-Gates, Timeout und Release-Gates bleiben unverändert. Der Schritt ist eine Verbesserung der Fehlerbeobachtbarkeit, **keine Behebung der unbekannten Fehlerursache**.
- **Tests/CI:** Workflow-Änderung aus dem vorhandenen Quelltext abgeleitet; kein tatsächlicher Runner-Test und kein grüner Build bestätigt. Neuester zuvor belegter Fehl-Lauf #459 (21.11.2026). Bei nächstem Actions-Lauf Artefakt `android-preflight-failure` abrufen und den ersten Nonzero-Exitcode beheben. Danach #13/#14 implementieren. Keine APK-Freigabe, keine Signing-/Secrets-Änderung.

## Entwicklungsabgleich – 22.11.2026, 04:24 MEZ

- **Arbeitspaket:** Reproduzierbare Regressionstestmatrix für #13 (Rotation/Zustand) und #14 (GPS-Track/Rerouting) in `docs/ROTATION-GPS-REGRESSION-PLAN.md` angelegt, Commit `bfebf619fdeb3c423a6b3a02f021283ccbd99fd0`. Beinhaltet Activity-Recreation, Prozess-Tod, stale/outlier GPS, aus-der-Reihenfolge eintreffende Reroutes und klare Freigabegates. Noch keine Implementierung oder Testausführung.
- **Repo-Abgleich:** README, Roadmap, ADRs, Handoff, CI-/Diagnoseworkflow, Preflight-Quelltext und elf offene Issues gelesen; keine offenen PRs. Vollständige fremde Chatverläufe nicht verfügbar; bekannte Anforderungen aus Main/Schedule übernommen.
- **CI:** Letzter zuvor belegter fehlgeschlagener Android-Push-Lauf #459 (21.11.2026); in diesem Lauf kein neuer erfolgreicher Actions-Lauf nachgewiesen. Einzelne Preflight-Exitcodes weiter unbekannt. Keine APK-Freigabe. Nächster Schritt: Diagnoseartefakte/Exitcodes beschaffen, Preflight reparieren, anschließend Tests gemäß neuer Matrix implementieren.

## Entwicklungsabgleich – 22.11.2026, 03:25 MEZ

- **Live-Stand:** `main` vor dieser Aktualisierung `14e0bfab70dd0d7d4046988abc2b9cf0ef4e434f`; elf offene Issues, keine offenen PRs. README, Roadmap, Entscheidungen, Status, Android-/Diagnoseworkflow und Preflight-Skript abgeglichen. Bekannte Projektchat-Anforderungen #13 (Rotation) und #14 (GPS-Spur/Rerouting); vollständige fremde Chatverläufe nicht abrufbar.
- **Neue belastbare CI-Evidenz:** Allgemeine Actions-REST-Liste `/actions/runs?per_page=8` ist über den GitHub-Connector **abrufbar**. Neuester sichtbarer Push-Lauf ist [Android CI #459](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44940824673), Run-ID `44940824673`, gestartet **21.11.2026 19:25 UTC** auf `14e0bfa`, `completed/failure`. Job `preflight` ID `136957405044` = `failure`, fünf Folgejobs = `skipped`. Die separate Job-Schritte-Abfrage liefert `[]`, Logabruf scheitert; keine Artefakte. Somit ist #452 **nicht mehr der aktuellste belegte fehlgeschlagene Lauf**. Der fehlgeschlagene Einzelcheck ist weiterhin nicht bekannt.
- **Diagnosezugang:** `/actions/workflows/preflight-diagnostics.yml/runs?per_page=5` ließ sich nicht abrufen. Der Diagnoseworkflow wird bei reinen Dokumentationsänderungen nicht automatisch ausgelöst; `workflow_dispatch` ist konfiguriert, hier jedoch keine Dispatch-Aktion verfügbar. Die drei Checks dürfen nicht ohne Exitcodebeleg als Ursache bezeichnet werden.
- **Nächster konkreter Schritt:** Bei verfügbarem GitHub-Actions-Dispatch den Diagnoseworkflow manuell starten, Artefakt `preflight-diagnostics` mit `results.txt` und `check-1.log` bis `check-3.log` lesen, nachgewiesenen ersten Fehler mit Regressionstest beheben. Alternativ die drei Befehle auf exakt diesem Commit in einer funktionierenden Checkout-Umgebung ausführen. Danach #13/#14. Keine Tests in diesem Lauf ausgeführt, keine Änderung an Signing/Secrets, keine APK-Freigabe.

## Entwicklungsabgleich – 21.11.2026, 20:24 MEZ

- **Live geprüft:** HEAD `82356e544f6df6859fbf726e01b5b6a74d715ebb`; elf offene Issues, keine offenen PRs. README, Implementierungsroadmap, Projektstatus, regulärer Android-Workflow und unabhängiger Diagnoseworkflow erneut gelesen. Bekannte Chat-Anforderungen: Rotation (#13), GPS-Spur/Rerouting (#14). Vollständige andere Chatverläufe nicht live abrufbar.
- **Erneute CI-Fehlerisolierung:** Run [#452](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44752622034), Job `136344098872`: `preflight=failure`, fünf abhängige Jobs `skipped`; API liefert keine Job-Schritte. Logabruf am 21.11.2026 19:24 UTC erneut **404 BlobNotFound**. Dies belegt **nicht**, welches der drei Kommandos fehlschlug. Diagnoseworkflow enthält `workflow_dispatch`, unabhängige Exitcode-Protokolle und `if: always()`-Artefakt-Upload; kein tatsächlicher Diagnoselauf verifiziert.
- **Ausführungsblockade:** Checkout auf dem Ausführungsrechner nicht möglich: `git ls-remote https://github.com/jochenkunkel/mvv-mobility-assistant.git HEAD` → `Could not resolve host: github.com`. Daher keine lokalen Regressionstests und kein belastbarer CI-Fix. Nicht allein wegen fehlender Logs den Release-Gate lockern.
- **Nächste konkrete Aktion:** Aktuellen Actions-Push-Lauf und [Preflight-Diagnoseworkflow](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/preflight-diagnostics.yml) mit `results.txt`/`check-1.log` bis `check-3.log` prüfen bzw. manuell dispatchen, sobald eine geeignete Actions-Ausführungsschnittstelle verfügbar ist. Ersten tatsächlichen Nonzero-Exitcode beheben, Tests erneut laufen lassen. Danach #13 und #14 mit instrumentierten Tests. Keine Signing-/Secrets-Änderung, kein grüner Build nachgewiesen, keine APK-Freigabe.

## Entwicklungsabgleich – 21.11.2026, 16:25 MEZ

- GitHub erneut geprüft: HEAD vor Änderungen `3377d91`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, ADRs, Projektstatus, CI und unabhängiger Diagnoseworkflow abgeglichen. Andere Projekt-Chats sind nicht vollständig live lesbar; keine neuen Anforderungen erfunden.
- **Regressionstest ergänzt:** `scripts/test-ci-concurrency.py` sichert nun die in `97214f0` eingeführte 10-Minuten-Grenze des regulären Preflight-Jobs gegen versehentliches Entfernen ab. Commits `3de7b68` (Guard) und `eda7079` (Regex-Escape-Korrektur). Der Test prüft Workflow-Quelltext, nicht reale Runner-Laufzeit.
- **CI:** Letzter direkt erneut geprüfter Lauf [#452](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44752622034): Preflight fehlgeschlagen, fünf Jobs übersprungen. Kein neuerer Push-Run in diesem Durchgang verifiziert; keine Testausführung im vollständigen Checkout (lokale GitHub-DNS-Auflösung gescheitert). Kein grüner Build, keine APK-Freigabe.
- **Nächste Priorität:** Preflight-Diagnoseartefakte/Exitcodes beschaffen, tatsächlichen Fehler beheben; anschließend #13 Rotation und #14 GPS-Spur/Rerouting mit Instrumentierungstests. Keine Signing-/Secrets-Änderung.

## Entwicklungsabgleich – 21.11.2026, 15:21 MEZ

- Repository vor Änderung: `ef7e546`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Implementierungsroadmap, ADRs, Status, Android-CI und Diagnoseworkflow geprüft. Keine neue verifizierte Anforderung aus fremden Chats; vollständiger Live-Zugriff auf Main/Schedule fehlt.
- **Kleines CI-Härtungspaket:** `preflight` hatte im Gegensatz zum separaten Diagnosejob kein Job-Timeout. In `.github/workflows/android.yml` wurde `timeout-minutes: 10` ergänzt (Commit `97214f0`). Dies begrenzt Hänger, **behebt aber nicht** die unbekannte Ursache des bisherigen Preflight-Fehlschlags.
- **CI-Beleg:** Der erneut abgefragte Android-Lauf #452 (20.11.2026) zeigt `preflight=failure`, alle fünf Folgejobs `skipped`, keine Einzelschritte. Keine ausgeführten lokalen Tests; ein grüner Lauf für `97214f0` ist nicht bestätigt. Keine APK-Freigabe.
- **Nächster Schritt:** Aktuellen Push-Run und unabhängige Diagnoseartefakte beschaffen, reale Exitcodes isolieren, erst dann Fehlerfix; danach #13 Rotation und #14 GPS-Track mit Emulator-Regressionstests.

# MeinWeg2 – zentraler Projektstatus

## Entwicklungsabgleich – 21.11.2026, 05:23 MEZ (Actions-API direkt)

- **HEAD:** `4cb59a4a094cb97c85008e568dab813b64edabfa`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Projektstatus, CI-Workflow und Diagnose-Runbook abgeglichen.
- **Wichtige neue Evidenz:** Die allgemeine GitHub-Actions-REST-Liste ist über den GitHub-Connector tatsächlich abrufbar. Der neueste geprüfte Android-Push-Lauf ist **[#452](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44752622034)**, gestartet 20.11.2026 14:21 UTC für HEAD `4cb59a4`, **completed/failure**. Job `preflight` (ID `136344098872`) = **failure**, alle fünf abhängigen Jobs = **skipped**. Die Job-API liefert `steps: []`; keine Artefakte. Der direkte Joblog-Abruf ergab am 21.11.2026 04:23 UTC erneut **404 BlobNotFound**. Der einzelne fehlgeschlagene Befehl ist damit **nicht belegt**. Der bisher zitierte Lauf #439 ist **nicht mehr der neueste belegte Lauf**.
- **Blockade und nächster Schritt:** Die Diagnose-Workflow-Run-Liste unter `/actions/workflows/preflight-diagnostics.yml/runs` war über den Connector nicht abrufbar; daher keinen Diagnoselauf behaupten. Zunächst Workflow-ID über `/actions/workflows` ermitteln und Läufe über numerische ID abrufen. Wenn weiterhin keine brauchbaren Logs/Artefakte vorliegen, die drei Preflight-Kommandos im Checkout des HEAD separat mit Exitcodes ausführen und den konkreten Fehler beheben. Danach #13 (Activity-Recreation/Track) und #14 (GPS-Spur/Rerouting-Races) testen.
- **Freigabe:** Keine Tests in diesem Lauf ausgeführt; Android-CI rot, keine APK-Freigabe. Kein Signing-/Secrets-Eingriff. Andere Chats nicht vollständig live zugänglich.


**Stand:** 19.11.2026, 02:25 MEZ (Repository-Abgleich) · **Verbindliche Codebasis:** GitHub `main`

> **Startpunkt für alle Chats/Agenten:** Diese Datei zuerst lesen, anschließend
> `main`, offene GitHub-Issues und CI-Läufe live abgleichen. Diese Datei ist
> eine Übergabe und kein Ersatz für den tatsächlichen Repository-Status.


## Entwicklungsabgleich – 20.11.2026, 12:19 MEZ

- **Live-Repository:** HEAD vor der Änderung `d9783c9`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Regressionstestmatrix, CI-Workflow und Diagnose-Runbook geprüft. Vollständige Live-Inhalte anderer Projekt-Chats nicht verfügbar; bekannte Main-/Schedule-Fehler zu Rotation, GPS-Spur und Rerouting bleiben maßgeblich.
- **Konkretes Testpaket:** `scripts/test-ci-concurrency.py` prüft jetzt zusätzlich den **unabhängigen** Diagnoseworkflow: manueller Dispatch, Trigger bei Skriptänderungen, alle drei separaten Checks, Timeout, Exitcode-Datei, Artefakt-Upload trotz Fehler und Aufbewahrungsdauer. Commits `93efec8` (Guard) und `73356f0` (Korrektur der Regex-Escapes). Diese statischen Assertions schützen vor stillen Diagnose-Regressionen; sie beweisen **keine** tatsächliche Workflow-Ausführung.
- **CI-Beleg:** Run [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) weiterhin `preflight=failure`, fünf Jobs übersprungen. Erneuter Logabruf: 404 `BlobNotFound` (20.11.2026, 12:19 MEZ). Keine Artefakte in #439; commitbezogene PR-Run-Abfrage für `93efec8` leer, damit Push-CI **unbekannt**. Die neue Testdatei wurde noch nicht in einer ausführbaren Checkout-Umgebung getestet.
- **Nächste Priorität:** Diagnoseworkflow-Lauf/Artefakte aus Actions beschaffen, den ersten echten Fehler beheben und den neuen statischen Guard ausführen. Anschließend #13 Activity-Recreation-Test mit nichtleerem Track, dann #14 Track-/Reroute-Races. Keine APK-Freigabe.

## Entwicklungsabgleich – 20.11.2026, 08:18 MEZ

- **HEAD vor diesem Eintrag:** [`25d33f1`](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/25d33f1c6df8441c3a90d0ffc84c0a069d0b19a0). GitHub-Commit-Suche: keine neueren Commits; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Rotation-/GPS-Testmatrix, Android-Workflow und Diagnoseworkflow erneut gelesen.
- **Präzisierung des Diagnose-Gates:** Der unabhängige Diagnoseworkflow wird bei Pushes auf `main` nur durch Änderungen an `.github/workflows/preflight-diagnostics.yml`, `.github/workflows/android.yml` oder `scripts/**` ausgelöst; der jüngste reine Runbook-Commit `25d33f1` löst ihn **nicht** aus. Ein früherer Trigger-Commit `2180b91` ist vorhanden, aber ein tatsächlicher Diagnose-Lauf ist hier nicht nachgewiesen. Der verfügbare GitHub-Connector stellt in diesem Lauf keine allgemeine Actions-Run-Liste oder Workflow-Dispatch-Aktion bereit; eine Commit-Run-Abfrage ist auf PR-Ereignisse beschränkt. Daher **keine Aussage über einen aktuellen Diagnose-Lauf oder grünen Push-Build**.
- **Nächste ausführbare Aktion:** Neueste [Diagnose-Läufe](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/workflows/preflight-diagnostics.yml) über die Actions-Webansicht/geeignete API abrufen. Falls keiner existiert, den Diagnoseworkflow manuell starten. `results.txt` und `check-N.log` lesen, erste nichtnull Exitcode-Ursache isolieren, gezielt beheben. Keine erneuten ungezielten CI-Dokumentationscommits als Ersatz für die Diagnose.
- **Danach:** #13 instrumentierten `ActivityScenario.recreate()`-Test mit nichtleerer Navigation/GPS-Fixture implementieren und im Emulator ausführen; #14 GPS-Track/Reroute-Race-Regression. Keine Signing-/Secret-Änderung, keine APK-Freigabe. Bekannte Main-/Schedule-Meldungen bleiben berücksichtigt; vollständiger Live-Zugriff auf fremde Chatverläufe nicht nachgewiesen.

## Entwicklungsabgleich – 20.11.2026, 02:22 MEZ

- **Repository:** GitHub-Commit-Suche: letzter Commit `2fc818a8aa987837f3269b6d6c58ce0956f1b616`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Entscheidungen, Lifecycle-/GPS-Audit und CI-Workflow samt drei Preflight-Skripten erneut gelesen. Keine neue belegte Anforderung aus vollständig zugänglichen Projekt-Chats; Chat-Live-Synchronisation nicht verfügbar.
- **CI:** Erneut direkt abgefragter [Push-Lauf #439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530): `preflight=failure`, sämtliche Folgejobs `skipped`. Der Connector liefert `steps:null` und `logs_url:null`; **kein einzelner fehlerhafter Befehl identifizierbar**. Der Workflow enthält drei getrennte Preflight-Steps und einen `if: failure()`-Diagnoseschritt. Die vorhandenen Prüfskripte wurden statisch kontrolliert; das ist kein Ausführungsnachweis.
- **Präzisierter Diagnosepfad:** Zuerst in GitHub Actions den Job auf Runner-/Checkout-Startfehler, fehlende Runner-Zuteilung oder Ausführungsfehler untersuchen; diese Möglichkeiten sind **Hypothesen, keine Befunde**. Wenn Steps/Logs weiter fehlen, in einem erreichbaren Checkout exakt `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` einzeln ausführen und Exitcode + stderr sichern. Bei vollständig fehlender Jobinitialisierung zunächst Actions-/Runner-Zugriff klären, nicht wahllos Python/Android-Code verändern.
- **Anschließend:** #13 mit nichtleerer `ActivityScenario.recreate()`-Fixture, #14 mit GPS-Fix-Lücke und verspätetem Reroute-Ergebnis. Normale Push-CI reicht für Instrumentierung nicht aus; Emulator-Release-Test erforderlich. **Kein neuer grüner Build, keine APK-Freigabe.**

## Entwicklungsabgleich – 19.11.2026, 22:21 MEZ

- **Verifizierter letzter Commit auf main:** [`a306538`](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/a306538012fbb3519b1845cac0974764690aaf61). Der neue [Lifecycle-/GPS-Codebefund](LIFECYCLE-GPS-CODE-AUDIT-2026-11-19.md) ist jetzt die konkrete Arbeitsgrundlage für #13 (Rotation) und #14 (Spurverlust). Die Diagnose trennt produktiven `remember`-Navigationszustand von separat zurückgesetzten GPS-Debugdaten; sie ist noch **kein Laufzeitnachweis**.
- **Offene Arbeit:** elf Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap und Architekturentscheidungen abgeglichen; historische Roadmap-Phasen sind nicht als aktuelle Abnahme zu lesen. Andere Projekt-Chats sind nicht vollständig live zugänglich.
- **CI-Beleg:** Push-Lauf [#439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) mit `preflight=failure`, Folgejobs übersprungen; Step-Abfrage leer und Joblogs nicht abrufbar. Für `a306538` liegt hier kein bestätigter grüner Push-Lauf vor. Kein Test-/APK-Freigabenachweis.
- **Nächste Implementierung:** zuerst Preflight-Exitcode durch getrennte lokale Ausführung der drei dokumentierten Befehle isolieren. Parallel als kleines, testbares Paket für #13 einen instrumentierten `ActivityScenario.recreate()`-Test mit **nichtleerem** Navigations-/GPS-Fixture anlegen; danach State-Owner-Fix. Für #14 GPS-Fix-Lücke und verspätete Reroute-Antworten getrennt prüfen. Keine ungetestete Änderung an Signing oder Secrets.

## Entwicklungsabgleich – 19.11.2026, 23:20 MEZ

- **HEAD verifiziert:** `49bdc52fc40a4afb8f0cd9b69a87495951c3668d` (vorher `a306538`), keine neueren Commits in der Repository-Suche. Der vorherige 22:21-Eintrag nennt noch den Parent-Commit und ist insofern historisch; `49bdc52` hat diesen Status bereits ergänzt.
- **Issues/PRs:** weiterhin elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, ADRs, CI-Workflow, drei Preflight-Skripte und Lifecycle-/GPS-Audit erneut gegen `main` abgeglichen.
- **Konkrete CI-Diagnose aus Quellprüfung:** `scripts/test-ci-concurrency.py` enthält nicht nur statische Guards, sondern eine verhaltensbasierte Release-Gate-Matrix mit **52 Fällen**. Die drei unabhängigen Preflight-Kommandos sind im Workflow vorhanden. Das ist **kein Ausführungsnachweis**; der bisherige fehlgeschlagene Push-Lauf #439 bleibt die letzte hier belegte Jobdiagnose. `fetch_commit_workflow_runs` erfasst nur PR-Events und kann den Push-Status von `49bdc52` nicht bestätigen.
- **Kritische Test-Grenze:** Ein normaler Push führt `connectedDebugAndroidTest` nicht aus. Ein `ActivityScenario.recreate()`-Test für #13 darf erst nach einem echten Emulatorlauf über manuelles `test_level=release` oder Versionstag als bestanden gelten. #14 zusätzlich mit GPS-Fix-Lücke und verspäteter Reroute-Antwort prüfen.
- **Nächster ausführbarer Schritt:** Mit erreichbarem Checkout die drei Preflight-Kommandos einzeln ausführen, Exitcodes sichern und den ersten Fehler beheben; danach nichtleere Rotation-Fixture implementieren. Keine Änderungen an Signing/Secrets, kein grüner Build oder APK-Freigabe ohne Nachweis. Andere Projekt-Chats sind nicht vollständig live lesbar.

## CI-Folgeprüfung – 19.11.2026, 09:25 MEZ

- Der nach Commit `438b5d8` gestartete [Push-Lauf #439](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44478458530) ist ebenfalls **completed/failure**: `preflight` Job-ID `135404600838` fehlgeschlagen; `build-and-test`, Emulator und Live-Probes wurden übersprungen. Dies bestätigt eine weiterhin bestehende CI-Blockade, **nicht** die Ursache.
- Erneute Abfrage der Job-Schritte: `steps: []`. Joblog-Download: HTTP 404 `BlobNotFound` (19.11.2026, 09:25 MEZ). Die erste fehlerhafte Aktion ist weiterhin unbekannt; weder Checkout- noch Skriptfehler sind damit bewiesen.
- Statische Quellprüfung: `scripts/test-ci-concurrency.py`, `scripts/preflight.sh`, `scripts/test-emulator-navigation.py` und die Workflow-Definition sind vorhanden. Die Prüfschritte dürfen nicht als bestanden gelten, da keine Ausführung nachgewiesen ist.
- **Nächster ausführbarer Schritt:** Den Checkout des aktuellen HEAD in einer Umgebung mit GitHub-Zugriff herstellen und die drei Preflight-Kommandos separat mit Exitcode ausführen. Falls GitHub-Actions-Webansicht zugänglich wird, den **ersten fehlgeschlagenen Step** von Run #439 und dessen Annotation auslesen. Nur danach gezielt fixen und Push-CI erneut prüfen. Keine Signing-Änderung und keine APK-Freigabe.

## CI-Befund – 19.11.2026, 08:17 MEZ (verifiziert)

- **HEAD vor diesem Eintrag:** `9d14a698ba7eac70f5fdbf59421b66bed639afd2`. GitHub Actions **Push-Lauf #438**, Run-ID `44471413554`, vom 19.11.2026 07:20 MEZ: **completed/failure**. Beleg: https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44471413554 . Damit ist die frühere Aussage „Push-Läufe nicht einsehbar“ überholt: Die Actions-Run-Liste ist über den GitHub-Connector erreichbar.
- **Jobs:** `preflight` (Job-ID `135381540317`) **failure**; `build-and-test`, `emulator-smoke-test`, `mvv-live-probe`, `door-to-door-live-probe` und `release-acceptance` **skipped**. Der Workflow ist also tatsächlich rot, nicht nur unbestätigt. Aus den übersprungenen Jobs kann keine APK-Freigabe abgeleitet werden.
- **Diagnosegrenze:** GitHub-Job-Schritt-Abfrage liefert `steps: []`; Joblog-Abruf scheitert mit HTTP 404 / `BlobNotFound`. Welcher einzelne Preflight-Schritt fehlschlug, ist **nicht** nachgewiesen. Der Lauf wurde rund drei Sekunden nach Start abgeschlossen; daraus allein keine Ursache ableiten. Keine blinde Änderung an Produktivcode oder CI.
- **Nächster konkreter Schritt:** Über die GitHub-Actions-Weboberfläche Run #438 / Job `preflight` einschließlich Step Summary und Checks-Annotationen öffnen; bei weiterhin fehlenden Logs einen neuen manuellen Diagnose-Lauf mit zugänglichen Logs auslösen. Alternativ den exakten HEAD auschecken und `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` einzeln mit Exitcode ausführen. **Erst den ersten fehlgeschlagenen Befehl isolieren, dann reparieren und erneut CI prüfen.** #13/#14 bleiben danach die nächsten Codeprioritäten.
- **Chat-Abgleich:** Bekannte Main-/Schedule-Meldungen zu Rotation, GPS-Spur und Rerouting bleiben relevant; kein vollständiger Live-Zugriff auf alle Chatnachrichten behauptet.

## Automatischer Entwicklungsabgleich – 19.11.2026, 07:20 MEZ

- **Live-Abgleich:** GitHub-Suche meldet HEAD `46b1aab9a821589df355dde9bb72db75fbcd9a76` (Testvertrag), weiterhin elf offene Issues (#3–#11, #13, #14). README, `docs/IMPLEMENTATION-ROADMAP.md`, `docs/DECISIONS.md`, `docs/REGRESSION-ROTATION-GPS.md`, `MainActivity.kt` und `.github/workflows/android.yml` erneut gelesen. Die GitHub-Commit-Status-Abfrage für `46b1aab` lieferte `statuses: []`; daraus folgt **keine** Aussage über den tatsächlichen Push-Actions-Lauf.
- **Neu präzisierter CI-Befund:** Der normale Push durchläuft `preflight` und `build-and-test` mit JVM-Tests, Debug-APK und Lint. Der Job `emulator-smoke-test` einschließlich `:app:connectedDebugAndroidTest` läuft **nur** bei einem Tag `v*` oder manuellem `workflow_dispatch` mit `test_level=release`. Ein erfolgreicher gewöhnlicher Push könnte deshalb den geforderten `ActivityScenario.recreate()`-Instrumentierungstest **nicht** bestätigen. Dies ist ein konkreter Nachweis-/Gate-Unterschied, kein beobachteter Testfehler.
- **Arbeitspaket für #13:** Erst einen isolierten Android-Instrumentierungstest mit `ActivityScenario.recreate()` und deterministischer Navigation-Fixture implementieren; dann mit `workflow_dispatch(test_level=release)` oder lokalem Emulator tatsächlich ausführen. Vorhandene `remember`-Zustände sind nach Activity-Recreation nicht garantiert erhalten. Danach Lifecycle-StateHolder und erneute Tests. **Nicht** auf eine gewöhnliche Push-CI als ausreichende Abnahme verweisen.
- **Arbeitspaket für #14:** GPS-Track als unabhängige Zustandsquelle von OSRM-Route und UI-Layer testen; A/B/C → Fix-Lücke → D, Reroute und stale Callback. Erst mit bestandenem Emulatorlauf und realem S25-Nachweis schließen.
- **Ausführungsgrenze dieses Laufs:** Kein Android-SDK-/Emulator-Testlauf und keine vollständige Actions-Push-Run-Liste verfügbar; daher kein grüner Build, keine APK-Installationsfreigabe. Keine neuen, vollständig verifizierten Anforderungen aus anderen Projekt-Chats; zugänglicher Verlauf deckt Rotation, GPS-Aussetzer und Rerouting ab.

## Automatischer Abgleich – 19.11.2026, 01:17 MEZ

- **Verifizierter HEAD vor diesem Eintrag:** `e0abc503f1035421dc7f708ea294ba2ee34c6963` (neue Offline-Testsuite für Emulator-Navigationsziele). Die neue Suite `scripts/test-emulator-navigation.py` wurde im Repository gelesen: 18 Testmethoden; sie wird im `preflight`-Job nach Shell-/Python-Syntaxprüfung ausgeführt. **18 vorhandene Tests sind keine 18 bestandenen Tests.**
- **Live-Issues:** #3–#11 sowie #13 und #14 (11 offene Tickets). #13 und #14 bleiben ohne nachgewiesene Lifecycle-/Track-Regressionstests offen.
- **CI-Beleggrenze:** Die Commit-Status-Abfrage für `e0abc50` lieferte `statuses: []`. Dies ist **kein** grüner Build; der Connector für commitbezogene Workflow-Läufe berücksichtigt nur PR-Ereignisse, nicht zuverlässig Push-Läufe. Aktuelle Push-Joblogs und tatsächliche Preflight-Ausgabe fehlen. Keine APK-Installationsfreigabe.
- **Konkreter nächster Schritt (ausführbar):** `python3 -u scripts/test-emulator-navigation.py`, `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` in einem Checkout von genau `e0abc50` ausführen; Exitcodes und erste fehlerhafte Zeile festhalten. Anschließend Push-Actions-Lauf für diesen SHA über die Actions-Run-Liste (nicht PR-Commit-Lookup) prüfen. Erst danach gezielte Reparatur statt weiterer rein dokumentarischer Änderungen.
- **Chat-Grenze:** Die bekannten Anforderungen aus Main/Schedule zu GPS-Aussetzern, Rerouting, Rotation und Trackdarstellung sind bereits in #13/#14 und `docs/REGRESSION-ROTATION-GPS.md` enthalten. Kein vollständiger Live-Zugriff auf alle Projekt-Chats nachgewiesen.

## Automatischer Abgleich – 19.11.2026, 02:25 MEZ

- **HEAD:** `569cd01a585e00f53a995f9163503a3cc0bd6db2`; Issue-Suche bestätigt #3–#11, #13, #14 (11 offene). Die aktuelle CI-Konfiguration hat drei separate Preflight-Schritte und eine diagnostische Fehlerzusammenfassung.
- **Gezielte Quellprüfung:** `scripts/test-emulator-navigation.py` enthält 18 `unittest`-Methoden; `scripts/tap-emulator-navigation.py` berücksichtigt die Sichtbarkeit und Aktivierung der gesamten Vorfahrenkette und verwirft ungültige Rechtecke. Dies ist eine **statische Codeprüfung**, kein Testlauf.
- **Reproduzierbare Infrastrukturblockade:** `git ls-remote https://github.com/jochenkunkel/mvv-mobility-assistant.git HEAD` im Ausführungscontainer scheiterte mit `Could not resolve host: github.com` (Exit 128). Damit kein Checkout und keine lokalen Ausführungen von Preflight, Gradle oder Tests. Die GitHub-Connector-Abfrage erlaubt zwar Dateilesen, bietet hier aber keine verlässliche Liste der Push-Workflow-Runs; der konkrete erste fehlgeschlagene Preflight-Befehl ist weiterhin **nicht belegt**.
- **Priorisierte Reparatur:** (1) Sobald DNS/Checkout funktioniert, exakt `569cd01` auschecken, `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` mit Exitcodes ausführen. (2) Für #13 Activity-/Navigation-State-Eigentümer identifizieren und `ActivityScenario.recreate()`-Regression anlegen. (3) Für #14 GPS-Track unabhängig von der OSRM-Route mit synthetischen Fix-Lücken und Reroute prüfen. **Kein ungetesteter Produktivcode-Fix und keine APK-Freigabe.**
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
| `396a1cd` | Routengebundene Auswahl der nächsten Abbiegung; Regressionstests | implementiert, Geräteabnahme offen |
| `d3b3e00` | Minikartenrichtung über Segmentprojektion; Tests | Teilfix, Issue #3 offen |
| `17c34fe` | Historische Roadmap als Snapshot gekennzeichnet | Teilfix, Issue #10 offen |
| `3d313b8` | Android-Lint auf jeden Push/PR erweitert | Teilfix, Issue #8 offen |
| `ecf69d3` | Monotone GPS-Freshness für Cache-Fixes; Tests | Teilfix, Issue #6 offen |
| `7d845dc` | Roadmap um Arbeitsstand und Chat-Übergabe ergänzt | Dokumentation aktualisiert |

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

- Commits `c858605`, `ce8404e`, `d0e2d30`, `9ca1809` direkt nach `main`.
- `CurrentLocationRepository`: Alter von GPS-Fixes bevorzugt über `elapsedRealtimeNanos`, Wall-Clock nur als Fallback; Reihenfolge nach monotonic timestamp, Cache-Fix-Auswahl nach tatsächlichem Alter.
- `MainActivity`: Navigation-Freshness, Status und GPS-Debug-Heartbeat verwenden die gemeinsame Altersprüfung statt eigener Wall-Clock-Vergleiche.
- `LocationFreshnessTest`: zusätzliche Grenz-/Rücksprungtests.
- **Verifikation:** CI-Lauf für HEAD prüfen; kein S25-Praxistest, kein GPS-Replay und keine visuelle Freigabe behauptet. Issue #6 bleibt offen bis zur Geräteabnahme und weiteren GPS-Zustandstests.
- **Nächster Schritt:** CI prüfen, GPS-Debug-Export im realen Fußweg testen, Schwankungen/Accuracy/Provider-Wechsel anhand Log reproduzieren und danach gezielt korrigieren.


## Automatischer Entwicklungsabgleich – 16.11.2026, 12:33 MEZ

- **Neu seit dem bisherigen Status:** Commits `776b488` (alle MVV-/Fußweg-Etappen in Verbindungskarten), `fb3410e` (OSM-Minimap im Fahrt-Dashboard), `4b9d489` (Ankunft nur in tatsächlicher Zielnähe) und `4483d8f` (Regressionstests gegen verfrühte Ankunft) wurden auf `main` vorgefunden. Die Änderungen sind im Repository vorhanden; keine reale S25-Abnahme behauptet.
- **Eigene Arbeit:** `bcaa8fc` ergänzt zwei Offline-Regressionstests in `WalkingNavigationTest.kt`: OSRM-GeoJSON-Koordinaten werden korrekt von [Längengrad, Breitengrad] in die appinterne [Breitengrad, Längengrad]-Reihenfolge übernommen; eine Route ohne Manöverschritte wird abgewiesen. Produktivcode blieb unverändert.
- **CI-Nachweis:** Die GitHub-Schnittstelle liefert für den zuletzt abgefragten Commit `4483d8f` keine Status-Checks und keine Workflow-Läufe. Die Workflow-Run-Abfrage deckt PR-Läufe ab und ist kein verlässlicher Nachweis für Push-Läufe. Neue Tests wurden in diesem Durchlauf nicht lokal ausgeführt; für `bcaa8fc` steht CI-Verifikation aus. **Keine APK-Freigabe.**
- **Projekt-Chats:** Andere Chats sind in diesem Lauf nicht vollständig abrufbar; die aus dem sichtbaren Projektkontext bekannten Anforderungen zu verfrühter Ankunft, GPS-Stabilität, Minimap und dynamischem Puffer sind berücksichtigt. Keine vollständige Chat-Synchronisierung behauptet.
- **Nächste Priorität:** #4 exakte Einstiegshaltestelle aus MVV-Etappendaten statt Namens-Geocoding; dazu vorhandene Modelle und Test-Fixtures prüfen, dann minimalen Fix und Regressionstests. Parallel den tatsächlichen Actions-Lauf und Gerätetest für die neuesten Navigationsänderungen nachweisen.

## Automatischer Entwicklungsabgleich – 16.11.2026, 13:33 MEZ

- **Repo-Abgleich:** Neuester vor diesem Lauf sichtbarer Commit `03c0c3f`; Issue #4 (MVV-Einstiegshaltestelle) und weitere neun offene Tickets bleiben nach Issue-Suche relevant. Der bisherige Status dokumentierte zuletzt die beiden OSRM-Parser-Tests `bcaa8fc`.
- **Eigene Änderung:** `b2e9a62` erweitert `WalkingNavigationTest.kt` um einen Offline-Regressionstest für OSRM-Antworten mit mehreren Legs: Reihenfolge aller vier Manöver, Schrittentfernungen, Ziel-Manöver und Koordinaten. Keine Änderung an Produktivcode oder Routing-API.
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
- **Offene Validierung:** GitHub Actions für Commit `94d85e9` ist bei Dokumentation noch nicht erfolgreich bestätigt. Echte EFA-Stop-Koordinaten und einzelne Bussteige Fürstenried West müssen mit Live-Antwort und vor Ort geprüft werden. Keine APK-/Gerätefreigabe ohne erfolgreichen Build.


## Automatischer Entwicklungsabgleich – 16.11.2026, 16:33 MEZ

- **Neuester geprüfter `main`-Commit:** `685f7cee` (dauerhafte Darstellung des zurückgelegten GPS-Pfads getrennt von der geplanten Route). Weitere neue Commits: `9416b360` (angenommener GPS-Track unabhängig von Routen-Neuberechnung), `61b13706` (Off-Route-Evidenz bei GPS-Lücken), `205c5475` (Fußweg-Alternative in Verbindungskarten), `2bc51f59` (Satellitenstatus und Stummschaltung), `02028d0e` (präzise GPS-Fixes vor groben Netzwerk-Fixes), `9b4005da` (Standortmarker bei GPS-Ausfall), `08b641c9` (Neuberechnung nach bestätigter Routenabweichung). Diese Angaben sind **Commit-Nachweise**, keine Geräte- oder Funktionsabnahme.
- **Offene Qualitätsrisiken:** Zusammenspiel aus Rerouting, dauerhaftem Lauf-Track und GPS-Fix-Filterung benötigt Regressionstests für (1) GPS-Lücke, (2) groben Netzwerk-Fix, (3) Off-Route-Bestätigung, (4) Neuberechnung ohne Trackverlust. Besonders zu prüfen: ob Rerouting auf die tatsächlich gewählte MVV-Haltestelle statt auf einen Namens-Treffer führt (Issue #4).
- **CI-Abfrage:** Für `685f7cee` liefert die verfügbare Commit-Status-Abfrage `statuses: []`, die Workflow-Run-Abfrage `workflow_runs: []`. Letztere erfasst Push-Läufe nicht zuverlässig. Daraus folgt **kein** Nachweis eines grünen oder roten Builds. Kein lokaler Gradle-Lauf in diesem Abgleich; **keine APK-Freigabe**.
- **Offene Issues:** #3–#11 sind im Issue-Abgleich weiterhin als offene Arbeiten gelistet. Die Roadmap ist teilweise ein historischer Snapshot; diesen Abschnitt bei der nächsten Statuskonsolidierung als aktuelle Ergänzung berücksichtigen.
- **Chat-Abgleich:** Andere Projekt-Chats konnten nicht vollständig live gelesen werden. Bekannte Anforderungen zu GPS-Stabilität, Kartenfortschritt und richtiger Einstiegshaltestelle sind im Arbeitsplan berücksichtigt; keine vollständige Synchronisierung behauptet.
- **Nächste konkrete Arbeit:** Bestehende Tests für Track-Erhalt und Reroute-Grenzfälle lesen und um einen deterministischen Regressionstest ergänzen; anschließend die tatsächlichen GitHub-Actions-Push-Läufe und Gerätevalidierung separat nachweisen.


## Automatischer Entwicklungsabgleich – 16.11.2026, 22:21 MEZ

- **Repository- und Issue-Abgleich:** Neueste zuvor vorhandene Commits `f63f4fc` (Concurrency-Dokumentation) und `78f433f` (Concurrency-Isolation); neun offene Issues #3–#11. README, Roadmap und Workflow geprüft. Keine vollständige Live-Sicht auf andere Projekt-Chats.
- **Umgesetzt auf `main`:** `a742401` fügt `scripts/test-ci-concurrency.py` als Python-Standardbibliothekstest hinzu. Er prüft die Workflow-Concurrency-Gruppe auf Workflow-, Ereignis- und Ref-Isolation sowie die Beschränkung automatischer Abbrüche auf Nicht-Tag-Pushes. `c47055a` bindet den Test in den bestehenden Preflight-Job ein. Keine Signing- oder Secret-Änderungen.
- **Nachweisgrenze:** Der Guard ist eine statische Regression gegen unbeabsichtigte YAML-Änderungen, **kein** Ersatz für den Test mit überlappenden echten Actions-Läufen. Für `c47055a` ergaben die verfügbaren Commit-Status- und PR-gefilterten Workflow-Abfragen leere Ergebnisse; der tatsächliche Push-Buildstatus bleibt **unbekannt**. Der neue Test wurde in diesem Lauf nicht lokal ausgeführt. **Keine APK-Freigabe.**
- **Nächste Priorität:** Einen tatsächlichen Actions-Lauf einschließlich Preflight-Ergebnis und Run-ID prüfen; anschließend deterministische GPS-/Reroute-/Track-Erhalt-Tests ergänzen und Release-Gates gesondert verifizieren.

## Automatischer Entwicklungsabgleich – 17.11.2026, 00:25 MEZ

- **Repository:** Vor diesem Lauf letzter Commit `6acab748`; neun offene Issues #3–#11, keine offenen PRs. README, Roadmap, Projektstatus, CI-Workflow und vorhandenen Concurrency-Test erneut gelesen.
- **Umgesetzt:** `e4eea4a` erweitert `scripts/test-ci-concurrency.py` um eine Prüfung, dass der Guard tatsächlich im `preflight`-Job als ausführbarer Schritt eingebunden ist. Dies verhindert, dass spätere Workflow-Änderungen den statischen Guard unbemerkt deaktivieren. Keine Signing-/Secret-Änderung.
- **Nachweisgrenze:** Commit bestätigt, aber Test nicht lokal ausgeführt; Push-Workflow-Runs sind über die verfügbare commitbezogene GitHub-Schnittstelle nicht vollständig einsehbar. **Build/APK unbekannt, keine Freigabe.** Die tatsächliche Concurrency-Scheduler-Isolation bleibt mit überlappenden Runs zu prüfen.
- **Chats:** Kein vollständiger Live-Zugriff auf andere Projekt-Chats. Sichtbare Anforderungen zu GPS-Lücken, Off-Route-Rerouting, Track-Erhalt und MVV-Einstieg bleiben im Plan.
- **Nächste Priorität:** Actions-Run-ID und Preflight-Ausführung nachweisen; danach deterministische Regressionstests für GPS-Lücken, Rerouting und Track-Erhalt.

## Automatischer Entwicklungsabgleich – 17.11.2026, 01:17 MEZ

- **Repo:** Vor Arbeitsbeginn war `d1acff2` HEAD; neun offene Issues, keine offenen PRs. README, Roadmap, Status und Android-Workflow gelesen. Andere Projekt-Chats sind nicht vollständig live zugänglich.
- **Fehlerbefund:** In `scripts/test-ci-concurrency.py` waren reguläre Ausdrücke als Raw-Strings mit doppelt escapten Backslashes geschrieben (`\\s`, `\\n` usw.). Damit konnte der Preflight-Guard die YAML-Abschnitte nicht wie beabsichtigt erkennen; dies ist ein statischer Codebefund, kein ausgeführter Fehlertest.
- **Änderungen:** `cb4aa04` ergänzt die Prüfung, dass der APK-Build vom Preflight abhängt. `fb8cae6` korrigiert die Raw-Regex-Escapes im bestehenden Test. Keine Signing-/Secrets-Änderungen.
- **Verifikation:** Datei nach Commit erneut aus GitHub gelesen. Weder lokale Ausführung des Python-Tests noch erfolgreicher GitHub-Actions-Lauf nachgewiesen; die verfügbare commitbezogene Workflow-Abfrage zeigt nur PR-Läufe und liefert für `cb4aa04` keine Ergebnisse. **APK nicht freigegeben.**
- **Nächster Schritt:** `python3 scripts/test-ci-concurrency.py` in einem echten Checkout ausführen und den aktuellen Actions-Preflight samt Run-ID prüfen. Danach GPS-/Rerouting-/Track-Erhalt-Regressionen priorisieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 02:16 MEZ

- **Repo zuerst geprüft:** README, Implementierungs-Roadmap, zentraler Status, Android-CI-Workflow, offene Issues/PRs und jüngste Commits. Vor Änderung war `9f70ae8` der neueste gefundene Commit; neun Issues offen, keine offenen PRs.
- **Kleines Testpaket:** `bb9c02d` erweitert `scripts/test-ci-concurrency.py`: Auch der Release-Emulator-Job muss ausdrücklich `needs: preflight` enthalten. Das sichert eine zweite, APK-relevante Teststrecke gegen versehentliches Umgehen des Preflight ab. Keine Signing-/Secret-Änderung.
- **CI-Grenze:** Commitbezogene GitHub-Abfrage für `9f70ae8` lieferte null PR-Workflow-Läufe und null klassische Commit-Statuschecks. Diese Schnittstellen zeigen Push-Läufe nicht zuverlässig; das ist **kein Beleg für fehlgeschlagene oder erfolgreiche Push-CI**. Den neuen Python-Test in diesem Lauf nicht lokal ausgeführt.
- **Projekt-Chats:** Sichtbarer Verlauf nennt GPS-Aussetzer, Off-Route-Rerouting, verschwundenen Track, UI-Icons, Home-Schnellaktion und dynamischen Zeitpuffer. Andere Chats nicht vollständig live abrufbar; keine vollständige Synchronisation behauptet.
- **Freigabe:** Keine grüne CI, kein Gerätetest, keine APK-Freigabe nachgewiesen.
- **Nächster Schritt:** `python3 scripts/test-ci-concurrency.py` auf Checkout ausführen, Actions-Preflight mit Run-ID/Logs verifizieren; anschließend GPS-/Track-Erhalt-Regressionen implementieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 06:25 MEZ

- **Repository:** README, zentrale Statusdatei, Roadmap, Android-Workflow und Release-Abnahmematrix gelesen. GitHub-Suche: neun offene Issues (#3–#11), keine offenen PRs. Vor Änderung war der zuletzt dokumentierte Release-Gate-Commit `bac4d19`; den aktuellen Head nicht allein aus dem historischen Status ableiten.
- **Umgesetzt:** Commit `796d6b3` erweitert den bereits im Preflight eingebundenen Offline-Regressionstest `scripts/test-ci-concurrency.py`. Er prüft jetzt die fünf expliziten `release-acceptance.needs`-Kanten, `always()`, Aktivierung für manuelle Release-Läufe und Versionstags, Abfrage jedes Job-Ergebnisses und Fail-Closed-Exit. Kein Signing-/Secrets-Eingriff.
- **Nachweis:** Änderung nach GitHub zurückgelesen und Commit-Diff geprüft. Commit-Workflow-Abfrage für `796d6b3` ergab `workflow_runs: []`, klassische Statusabfrage `statuses: []`; die Workflow-Abfrage ist auf PR-Läufe begrenzt. Python-Test und Android-Tests wurden in diesem Lauf nicht tatsächlich ausgeführt. **Kein grüner CI-Nachweis, keine APK-Freigabe.**
- **Projekt-Chats:** Bekannte Anforderungen zu GPS-Lücken, Track-Erhalt, Off-Route, MVV-Haltestellen und dynamischem Zeitpuffer bleiben relevant; andere Chats nicht vollständig live lesbar.
- **Nächste Priorität:** Python-Guard in einem echten Checkout ausführen und Release-Run-ID mit allen sechs Job-Ergebnissen einschließlich Gate belegen. Anschließend deterministische GPS-/Track-/Rerouting-Tests, nicht bloß weitere CI-Dokumentation.


## Automatischer Entwicklungsabgleich – 17.11.2026, 07:23 MEZ

- **Repository zuerst geprüft:** README, Roadmap, Projektstatus, Android-CI-Workflow, letzte Commits, neun offene Issues und keine offenen PRs. Ausgangs-HEAD `760af18`.
- **Änderung:** `7afcaa9` ergänzt in `scripts/test-ci-concurrency.py` einen dynamischen Shell-Test des tatsächlichen `release-acceptance`-Skripts: fünf erfolgreiche Jobs müssen akzeptiert werden, und für jeden der fünf Jobs werden `failure`, `cancelled`, `skipped` und leere Ergebnisse abgelehnt (21 Kombinationen). Der bestehende Preflight ruft das Skript bereits auf. Keine Secrets-/Signing-Änderung.
- **Verifikation:** Commit von GitHub bestätigt; Python-Test, Android-Tests und Actions-Lauf sind noch nicht ausgeführt/nachgewiesen. Die verfügbare Commit-Workflow-Schnittstelle zeigt nur PR-Läufe und eignet sich nicht als Push-CI-Beleg. **Keine APK-Freigabe.**
- **Chats:** Sichtbarer Projektkontext zu GPS-Aussetzern, Track-Erhalt, Satellitenanzeige, Home-Aktion und Rerouting berücksichtigt; kein vollständiger Live-Zugriff auf alle anderen Chats.
- **Nächste Priorität:** Neuen Preflight wirklich ausführen und Actions-Run-ID samt Ergebnissen prüfen. Danach deterministische GPS-Lücken-/Reroute-/Track-Erhalt-Regressionen; keine weiteren reinen CI-Dokumentationsrunden.


## Automatischer Entwicklungsabgleich – 17.11.2026, 12:17 MEZ

- **Live geprüft:** `main`-HEAD `ea044c41997bd9aad4b65f6ab7f011888cf0afb7`; neun offene Issues (#3–#11), keine offenen PRs. README, diese Statusdatei, `.github/workflows/android.yml`, `scripts/preflight.sh`, `scripts/test-ci-concurrency.py` und `scripts/test-emulator-navigation.py` erneut gelesen. Der frühere Statusabschnitt vom 16.11. ist ein historischer Snapshot.
- **CI-Beleg:** Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) für `93c19a5`: `preflight` = failure (Job-ID `133914886170`), die übrigen fünf Jobs = skipped. Job-Steps-Abfrage gibt ein leeres Array zurück; Job-Logs sind über den Connector nicht abrufbar. Das ist **kein** Beleg für einen konkreten Python- oder Shell-Fehler.
- **Aktuelle Preflight-Reihenfolge:** `python3 scripts/test-ci-concurrency.py` → `bash -x scripts/preflight.sh` → `python3 -u scripts/test-emulator-navigation.py`. Der erste Schritt enthält 52 dynamische Release-Gate-Fälle; alle drei Befehle müssen separat ausgeführt werden, um den ersten tatsächlichen Fehler zu isolieren. Eine fehlerhafte Vermutung darf nicht als Ursache in Code umgesetzt werden.
- **Ausführungsgrenze:** Lokaler `git ls-remote`-Versuch scheitert mit `Could not resolve host: github.com`. Daher weder Repository-Klon noch echte lokale Testausführung in diesem Lauf möglich. Das GitHub-Connector-Lesen funktioniert, aber es liefert keinen ausführbaren Checkout. **Keine APK-Freigabe.**
- **Nächster risikoarmer Schritt:** GitHub-Actions-Run mit erreichbaren Logs öffnen; alternativ vollständigen Checkout in einer netzfähigen Umgebung bereitstellen und die drei Preflight-Befehle einzeln ausführen. Erst nach belegtem Fehler minimalen Fix plus Regressionstest auf `main` committen und den neuen Run prüfen. Anschließend #6 GPS-Lücken/Track-Erhalt und #3 Off-Route-Rerouting testen.
- **Chat-Abgleich:** Nur die hier verfügbaren Projektkontexte und dokumentierten Anforderungen (GPS, Track, Satellitenstatus, Rerouting, Puffer, UI) berücksichtigt. Kein vollständiger Zugriff auf fremde Chatverläufe behauptet.


## Automatischer Entwicklungsabgleich – 17.11.2026, 13:21 MEZ

- **Repository:** Vor Änderung war `54f1518` der neueste Commit; neun Issues offen (#3–#11), keine offenen PRs. README, Roadmap, CI-Workflow, Projektstatus und Preflight-Skripte geprüft.
- **Verifizierter CI-Befund:** Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) für `93c19a5`: `preflight=failure`, übrige fünf Jobs `skipped`. Der Job-Endpunkt liefert keine einzelnen Steps. Ursache weiterhin unbekannt.
- **Änderung:** Commit `787eb18` trennt den bisherigen kombinierten Preflight-Schritt in zwei benannte Schritte (Shell-/Python-Syntax sowie Emulator-Navigationstest). Der Concurrency-Guard bleibt unverändert. Zweck: zukünftige Fehler auf einen konkreten Schritt eingrenzen, **kein behaupteter Fehlerfix**.
- **Verifikation:** Neue Workflow-Datei nach Commit aus GitHub zurückgelesen. Lokaler Clone und damit Testausführung scheiterten weiterhin an `Could not resolve host: github.com`. Ein neuer Actions-Lauf und grüne Tests für `787eb18` sind noch **nicht** nachgewiesen. **Keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbare bekannte Anforderungen (GPS-Lücken, Track-Erhalt, Rerouting, Satellitenstatus, UI) berücksichtigt; andere Chats nicht vollständig live abrufbar.
- **Nächster Schritt:** Push-Run für `787eb18` prüfen, den konkret fehlgeschlagenen Preflight-Schritt anhand GitHub-UI/Logs identifizieren, dann gezielt korrigieren und Regressionstest ausführen. Danach GPS-/OSM-Regressionen priorisieren.

## Entwicklungsabgleich – 17.11.2026, 14:20 MEZ

- `main` vor dem Lauf: `e18fb9c`; neun offene Issues (#3–#11), keine offenen PRs. README, Roadmap, Workflow, Projektstatus und Preflight-Skripte erneut geprüft.
- Der bislang konkret abrufbare Push-Run [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) ist **rot**: `preflight=failure`, fünf weitere Jobs `skipped`. Für diesen Run liefert die GitHub-Anbindung weder Job-Schritte noch Logs. Die Commit-Workflow-Abfrage für `e18fb9c` liefert keine Läufe, deckt Push-Runs aber nicht verlässlich ab; daraus folgt **kein** grüner Status.
- Änderung `b7c5770`: `scripts/preflight.sh` meldet mit einem `ERR`-Trap bei einem fehlschlagenden Shell-/Python-Syntaxcheck künftig Befehlszeile, Zeilennummer und Exitcode. Kein Signing-/Secret-Eingriff; keine Änderung an Produktiv-App-Code. Die vorgeschaltete `test-ci-concurrency.py`-Prüfung wird davon nicht erfasst, da sie ein eigener Workflow-Schritt ist.
- **Verifikation:** GitHub-Commit bestätigt. Lokale Ausführung/Android-Build durch DNS-Fehler (`Could not resolve host: github.com`) blockiert. Neuer Push-Run und vollständige sechs Release-Jobs nicht bestätigt. **Keine APK-Freigabe.**
- **Nächster Schritt:** Frischen Push-Run von `b7c5770` prüfen, fehlgeschlagenen Preflight-Schritt identifizieren und Fehlerursache gezielt beheben. Danach GPS-Replay bei temporärem Provider-Ausfall, Track-Erhalt, Satellitenstatus und OSM-Routenfortschritt mit Regressionstests absichern.
- Andere Projekt-Chats nicht vollständig live zugänglich; bekannte Anforderungen aus dem sichtbaren Projektkontext berücksichtigt, keine vollständige Synchronisierung behauptet.


## Automatischer Entwicklungsabgleich – 17.11.2026, 16:24 MEZ

- **Live-Basis:** GitHub-`main` zuletzt `1e425718035326288cc86923a4127a863577eaf7`; keine neueren Commits in der abgefragten Commitliste. README, Implementierungs-Roadmap, Projektstatus, Android-Workflow, `scripts/preflight.sh` und `scripts/test-ci-concurrency.py` gelesen. Keine offenen PRs; **elf offene Issues** einschließlich der neuen Praxisfehler [#13](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/13) und [#14](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/14). Frühere Abschnitte mit neun Issues sind historische Snapshots.
- **#13 Rotation – reproduzierbarer Abnahmefall:** Start/Ziel und Routenoptionen konfigurieren, Fußnavigation aktivieren, Portrait → Landscape → Portrait und Activity-Neuerstellung testen. Auswahl, Routen-Geometrie, aktuelle Etappe, Track und Navigation dürfen nicht zurückgesetzt werden. Vor einem Fix die Zustandsbesitzer (Compose `remember`/`rememberSaveable`, ViewModel, persistierte Session) und Lifecycle prüfen; **keine** pauschale Activity-Neuerstellung unterdrücken.
- **#14 Track – reproduzierbarer Abnahmefall:** Akzeptierte GPS-Punkte sammeln, anschließend GPS-Lücke, ungenauen Netzwerk-Fix, Positionssprung, bestätigte Routenabweichung, Rerouting und Karteneuzeichnung simulieren. Die echte aufgezeichnete Spur bleibt unverändert; ungültige Fixes erzeugen weder Löschung noch künstliche Verbindungssegmente. In `WalkingMiniMap` echte Track-Historie und dunkelviolett routenprojizierten Fortschritt begrifflich/visuell trennen; nicht dieselbe Datenquelle voraussetzen.
- **Priorität:** (1) CI-Preflight mit echtem Fehlernachweis reparieren, (2) #13/#14 deterministische Lifecycle-/Track-Regressionen, (3) GPS-Ausfall und Off-Route-Rerouting, (4) dynamischer Zeitpuffer und MVV-Etappendaten. Keine blinden Produktivänderungen.
- **CI-Grenze:** Die Commit-Workflow-Abfrage für `1e42571` ergab `workflow_runs=[]`, erfasst aber nur PR-Läufe und **beweist nichts über Push-CI**. Historischer roter Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) bleibt der letzte dokumentierte konkrete Fehlernachweis. In diesem Lauf wurden weder Python-Preflight noch Android-/Emulator-Tests ausgeführt; kein aktueller grüner Run oder APK-Artefakt verifiziert. **Keine APK-Installationsfreigabe.**
- **Chat-Grenze:** Nur in diesem Projektkontext sichtbare Anforderungen und GitHub-Issues abgeglichen; kein vollständiger Live-Zugriff auf Main/Schedule und andere Chats behauptet.
- **Nächste konkrete Aktion:** GitHub-Actions-Push-Run für aktuellen HEAD mit Run-ID und erstem fehlschlagenden Preflight-Schritt ermitteln; alternativ vollständigen Checkout in ausführbarer Umgebung nutzen. Anschließend minimalen, getesteten Fix committen. Für #13/#14 zuerst bestehende Zustands-/Track-Tests und Eigentümer der Daten identifizieren.


## Automatischer Entwicklungsabgleich – 17.11.2026, 17:21 MEZ

- **Aktueller GitHub-HEAD vor diesem Eintrag:** `4ccfdb3e669cf459ecb184bd17af16e5776a4c50`. README, Roadmap, zentraler Status, Workflow, `scripts/preflight.sh` und `scripts/test-ci-concurrency.py` erneut gelesen. Die GitHub-Issue-Suche liefert **11 offene Tickets**, einschließlich #13 (Rotation/Zustandsverlust) und #14 (GPS-Track verschwindet); keine neue Codeänderung seit dem vorherigen dokumentierten HEAD festgestellt.
- **Nachweis statt Vermutung:** Der historische Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) liefert bei erneuter Job-Abfrage `preflight=failure` (Job-ID `133914886170`), `build-and-test=skipped`; das ist **kein** CI-Ergebnis für den heutigen HEAD. Die GitHub-Connector-Abfrage für commitbezogene Workflows erfasst Push-Läufe nicht vollständig. Der lokale Versuch `git ls-remote https://github.com/jochenkunkel/mvv-mobility-assistant.git HEAD` scheiterte mit `Could not resolve host: github.com`. Daher keine Testausführung oder APK-Freigabe behauptet.
- **Konkrete Diagnose:** Im aktuellen Workflow sind die drei Preflight-Schritte `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` getrennt. Die ersten beiden Skripte wurden statisch gelesen; aus dem historischen Job-Ergebnis allein lässt sich **nicht** ableiten, welcher Schritt fehlschlug. Keine Änderung an Signing, Secrets oder ungeprüftem Produktivcode.
- **Priorisierte Reparatur:** (1) neuesten **Push**-Run zum HEAD in Actions öffnen, fehlgeschlagenen Preflight-Schritt und erste Fehlerzeile notieren; (2) `python3 scripts/test-ci-concurrency.py && bash -x scripts/preflight.sh && python3 -u scripts/test-emulator-navigation.py` auf einem Runner mit Repository-Zugriff ausführen; (3) genau diesen Fehler mit minimalem Fix plus Regressionstest beheben und CI neu prüfen; (4) danach #13 Lifecycle/Rotation und #14 Track-Persistenz mit GPS-Lücken/Rerouting in Tests absichern.
- **Chat-Grenze:** Anforderungen aus dem hier verfügbaren Projektkontext zu Rotation, GPS-Lücken, Off-Route-Rerouting und Track-Erhalt berücksichtigt; andere Chatverläufe nicht vollständig live lesbar. **Build/APK:** aktueller HEAD ungeprüft, keine Installationsfreigabe.


## Automatischer Entwicklungsabgleich – 17.11.2026, 19:21 MEZ

- **Baseline:** `3bbd21f` war beim Abgleich der letzte Commit auf `main`; 11 offene Issues, keine offenen PRs. README, Roadmap, CI-Workflow, Preflight-Skripte, zentrale Dokumentation und jüngste Commits geprüft. #13 (Rotation) und #14 (GPS-Track) bleiben offen; die Regression-Matrix ist bislang eine Spezifikation, keine ausgeführte Test-Suite.
- **Implementiert:** `951b82d` erweitert `scripts/test-ci-concurrency.py` um einen statischen Regression-Guard: die drei Preflight-Kommandos (Concurrency-Guard, Shell/Python-Syntaxcheck, Emulator-Navigationstest) müssen als jeweils eigener `run`-Schritt genau einmal und in dieser Reihenfolge im Workflow stehen. Dadurch kann ein künftiger Workflow-Umbau die zuvor eingeführte Fehler-Isolation nicht unbemerkt wieder entfernen. Keine Änderung an Signing, Secrets oder Android-Produktivcode.
- **CI-Nachweis:** Historischer Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252): Preflight fehlgeschlagen, abhängige Jobs übersprungen. GitHub liefert für diesen Job keine Schritte; die tatsächliche Ursache ist nicht nachgewiesen. Für `951b82d` liefern die verfügbaren Commit-Status- und PR-Workflow-Abfragen keine Ergebnisse; **das sagt nichts Verlässliches über Push-Läufe aus**. Der neue Guard wurde nicht lokal ausgeführt und ist nicht als bestanden gemeldet. **Keine APK-Freigabe.**
- **Projekt-Chats:** Die bekannten Anforderungen aus Main/Schedule zu GPS-Lücken, Track-Erhalt, OSRM-Rerouting, dynamischem Puffer und Bildschirmrotation wurden berücksichtigt. Kein vollständiger Live-Zugriff auf alle anderen Projekt-Chats.
- **Nächste Arbeit:** (1) Push-Run über GitHub-Actions-Weboberfläche oder eine echte Push-Run-API ermitteln; ersten fehlgeschlagenen Preflight-Schritt anhand Logs/Annotations isolieren. (2) Den CI-Guard lokal mit `python3 scripts/test-ci-concurrency.py` ausführen. (3) #13/#14 mit ausführbaren Zustandserhalt- und GPS-Replay-Tests bearbeiten; keine bloße weitere Testplanung statt Code.

## Automatischer Entwicklungsabgleich – 17.11.2026, 20:21 MEZ

- **Abgleich:** GitHub-`main` vor der Änderung `4eb7e8a`; README, Roadmap, Regressionstest-Matrix, Workflow, Preflight-Skripte und offene Tickets geprüft. 11 offene Issues, keine offenen PRs. #13 Rotation und #14 Track-Erhalt weiterhin offen. Keine neuen Commits anderer Entwickler seit dem vorherigen Lauf erkennbar.
- **Konkrete Änderung:** `424a410` ergänzt in `scripts/preflight.sh` eine GitHub-Actions-`::error`-Annotation bei Shell-/Python-Syntaxfehlern. Der vorhandene stderr-Fehlerbericht bleibt bestehen. Ziel: Fehlermeldungen auch über die Actions-Oberfläche sichtbar machen, wenn Job-Logs nicht abrufbar sind. Keine Änderungen an Signing, Secrets oder Produktivcode.
- **CI-Evidenz:** Der historische Push-Lauf [44046946252](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44046946252) meldet `preflight=failure`, abhängiger Build `skipped`. Job-Schritte werden als leere Liste zurückgegeben; Logabruf scheitert. Commit-Status und PR-bezogene Workflow-Abfrage für `4eb7e8a` waren leer und sind **kein** Beleg über Push-Läufe. Der neue Commit `424a410` ist **nicht** durch einen grünen CI-Lauf verifiziert.
- **Lokale Einschränkung:** `git ls-remote` scheiterte mit `Could not resolve host: github.com`; keine lokale Ausführung der Repository-Tests möglich. Die neue Annotation ist noch nicht in GitHub Actions beobachtet. **Keine APK-Installationsfreigabe.**
- **Nächster Schritt:** Actions-Push-Lauf für `424a410` und dessen Preflight-Annotationen kontrollieren. Bei identischem Fehler den ersten fehlschlagenden Schritt isolieren; anschließend ausführbare Regressionstests für #13 und #14 umsetzen. Andere Projekt-Chats sind nicht vollständig live zugänglich; nur bereits dokumentierte Anforderungen wurden übernommen.


## Entwicklungsabgleich – 17.11.2026, 21:23 MEZ

- Vor Änderung letzter Commit auf `main`: `7bc5e04`; offene Issues einschließlich #13 (Rotation) und #14 (Track-Verlust), keine offenen PRs. README, Roadmap, Preflight, Emulator-Test und Projektübergabe erneut gelesen.
- **Umgesetzt:** `aac1269` ergänzt zwei Offline-Regressionstests für ausgeblendete/deaktivierte doppelte Tab-Labels; `200ef85` lässt `scripts/tap-emulator-navigation.py` solche Nodes überspringen, bevor Koordinaten gewählt werden. Bestehende XML-Dumps ohne diese Attribute bleiben kompatibel. Dies behebt keine GPS-/Rotationsfehler, sondern einen klar abgegrenzten Emulator-Testhelfer-Randfall.
- **Tests:** Die neuen Tests sind im Repository, wurden in diesem Lauf nicht ausgeführt. Ein grüner CI-Lauf für den neuen HEAD ist nicht belegt. Historischer Preflight-Fehler in Actions-Lauf 44046946252; tatsächlicher neuer Push-Lauf und Job-Logs weiterhin gesondert prüfen. **Keine APK-Freigabe.**
- **Projekt-Chats:** Bekannte Anforderungen zu GPS-Lücken, OSRM-Rerouting, Track-Erhalt und Rotation berücksichtigt; kein vollständiger Live-Zugriff auf andere Chats.
- **Nächste Priorität:** `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` auf demselben HEAD ausführen und Actions-Schritte prüfen; danach #13/#14 mit instrumentierten Zustands- und GPS-Replay-Tests bearbeiten.

## Entwicklungsabgleich – 17.11.2026, 22:22 MEZ

- **Repo:** Vor diesem Lauf war `aa74cc1` der letzte sichtbare Commit auf `main`; README, Implementierungs-Roadmap, Preflight-/Emulator-Skripte und Android-Workflow geprüft. 11 offene Issues, keine offenen PRs. #13 (Rotation) und #14 (Trackverlust) weiterhin nicht behoben.
- **Implementiert:** `85d8821` ergänzt zwei ausführbare Offline-Regressionstests in `scripts/test-emulator-navigation.py`: ungültige Bounds beim ersten doppelten Navigationslabel dürfen einen späteren gültigen Treffer nicht verhindern; ausschließlich unsichtbare/deaktivierte Treffer müssen kontrolliert mit `ValueError` scheitern. Produktivcode, Signing und Secrets unverändert.
- **Nachweise:** Die neuen Tests wurden in diesem Lauf **nicht ausgeführt**. Für `85d8821` meldet die Commit-Status-Abfrage keine Checks und die PR-begrenzte Workflow-Abfrage keine Runs; das erlaubt keine Aussage über Push-CI. Historischer Run `44046946252`: Preflight `failure`, Build und Release-Jobs `skipped`; Schritte/Logs weiterhin nicht vorhanden. **Keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbarer Projektkontext zu GPS, OSRM, Rotation und Track berücksichtigt; vollständiger Live-Zugriff auf andere Chats nicht nachgewiesen.
- **Nächste Priorität:** Tatsächlichen Push-Run/Preflight-Fehler samt Logs oder Annotationen ermitteln; danach ausführbare Tests und Fixes für #13/#14 statt weiterer reiner Planungsdokumentation. Lokal/CI: `python3 scripts/test-ci-concurrency.py && bash -x scripts/preflight.sh && python3 -u scripts/test-emulator-navigation.py`.


## Entwicklungsabgleich – 17.11.2026, 23:22 MEZ

- **Live-Abgleich:** `main` vor diesem Eintrag `65c09fcbcddeea61aac8d0e3ab9d23289c070aca`; seit dem letzten Lauf keine weiteren Commits. README, Roadmap, Android-CI, Preflight-Skript, Emulator-Testskript und offene Issues gelesen. **11 Issues offen**, darunter #13 (Rotation) und #14 (Trackverlust); keine offenen PRs laut Issue-Suche.
- **Neuer belastbarer CI-Nachweis:** GitHub Actions **Push-Run #404**, ID [44179252791](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791), HEAD `65c09fc`, gestartet 17.11.2026 21:23 UTC, ist **completed/failure**. Die Jobs-API bestätigt `preflight=failure`; `build-and-test`, `emulator-smoke-test`, `mvv-live-probe`, `door-to-door-live-probe` und `release-acceptance` sind `skipped`. Das ist eine echte Push-CI-Prüfung und widerlegt die Annahme, es lägen nur alte CI-Ergebnisse vor.
- **Exakte Blockade:** Job-ID `134378837918`: Step-API liefert `steps=[]`; Job-Log-Download antwortet HTTP 404 `BlobNotFound`. Deshalb ist **nicht feststellbar**, ob `test-ci-concurrency.py`, `preflight.sh` oder `test-emulator-navigation.py` zuerst scheitert. Die drei Befehle sind im Workflow getrennt. Kein blindes Ändern von Produktivcode oder CI-Semantik.
- **Nächste ausführbare Diagnose:** In [Actions #404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) die Preflight-Step-Ansicht/Annotationen kontrollieren. Wenn ebenfalls unzugänglich, dieselben drei Befehle auf exakt `65c09fc` lokal ausführen: `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py`. Ersten fehlgeschlagenen Befehl samt stderr sichern und gezielt korrigieren. Danach neuen Push-Run verifizieren und #13/#14 implementieren.
- **Projekt-Chats:** Bekannte Anforderungen aus Main/Schedule zu GPS-Lücken, Rerouting, Track-Persistenz und Rotation übernommen; vollständige Live-Synchronisierung anderer Chatverläufe nicht nachgewiesen. **Keine Tests als bestanden ausgegeben; keine APK-Installationsfreigabe.**

## Entwicklungsabgleich – 18.11.2026, 01:19 MEZ

- **Repository erneut live gelesen:** `main` unverändert bei `38c395c3`; README, Roadmap, CI-Workflow, `scripts/test-ci-concurrency.py`, `scripts/preflight.sh` und Emulator-Testskript geprüft. 11 offene Issues, keine offenen PRs; #13 (Rotation) und #14 (Track-Verlust) bleiben priorisiert. Keine neuen externen Commits seit dem vorherigen Abgleich.
- **CI-Diagnose erneut konkret überprüft:** Für Push-Lauf [#404 / 44179252791](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) meldet die Jobs-API `preflight=failure` (Job `134378837918`) und `build-and-test=skipped`; die Step-Abfrage liefert erneut `[]`, der Job-Log-Download HTTP 404 `BlobNotFound` (18.11.2026 00:19 UTC). Somit bleibt die fehlerhafte Einzelprüfung **unbekannt**. Die drei Preflight-Befehle sind separat im Workflow eingetragen; aus statischer Sicht kein eindeutiger Syntaxdefekt nachgewiesen. Weder statische Sichtprüfung noch Commit ersetzen einen ausgeführten Test.
- **Gezielter nächster Diagnoseversuch:** Ein Checkout des aktuellen HEAD muss die drei Befehle `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py` in dieser Reihenfolge einzeln ausführen und deren Exitcodes/stderr sichern. Nur den tatsächlich fehlschlagenden Schritt reparieren; falls alle lokal erfolgreich sind, Runner-/Actions-Umgebung bzw. Checkout-Fehler anhand der Job-Ansicht prüfen. Keine blinde CI- oder Signing-Änderung.
- **Produktpriorität:** Danach ausführbare Rotation-Zustandserhalt-Tests (#13) und GPS-Track-Replay mit Fix-Lücken/Rerouting (#14), nicht bloß weitere Testmatrizen. Bekannte Anforderungen aus Main/Schedule berücksichtigt; vollständige Live-Sicht auf andere Chats weiterhin nicht verfügbar.
- **Verifikation dieses Durchlaufs:** Kein lokaler Test und kein erfolgreicher Push-Build nachgewiesen. **Keine APK-Freigabe.**

## Entwicklungsabgleich – 18.11.2026, 03:17 MEZ

- **Live-Repository:** Vor Änderung `004be675`; README, Roadmap, Status, Android-Workflow, Preflight-/Emulator-Skripte, Commits und offene Issues/PRs geprüft. Weiterhin 11 offene Issues, keine offenen PRs; #13 Rotation und #14 GPS-Trackverlust nicht behoben.
- **Konkrete CI-Diagnoseänderung:** Commit `a5fb126` ergänzt im Preflight-Job einen **nur bei Fehler** laufenden Diagnose-Schritt. Er schreibt für jeden der drei bestehenden Preflight-Befehle einen Exitstatus und die letzten 25 Ausgabezeilen in die GitHub-Actions-Job-Zusammenfassung und erzeugt eine Fehlerannotation. Bei fehlendem Checkout meldet er die fehlende Skriptdatei. Die bestehenden drei Prüfungen, `needs: preflight`, Release-Gates und Signing bleiben unverändert. Diagnoseschritte dürfen einen fehlgeschlagenen Job **nicht** in Erfolg umwandeln.
- **Ausgangsnachweis:** Push-Run [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) für `65c09fc`: Preflight failure, Build skipped; Steps/Logs nicht abrufbar. Die Ursache ist weiterhin **nicht** nachgewiesen. Der neue Schritt soll im nächsten fehlgeschlagenen Run die Ursache ohne Log-Download eingrenzen.
- **Verifikation:** Nach Commit `a5fb126` ist die aktualisierte Workflow-Datei per GitHub erneut gelesen worden. Commit-Status-API: keine Statuschecks; die verfügbare Commit-Workflow-Abfrage zeigt nur PR-Läufe und liefert keine Push-Bestätigung. Keine lokale Skriptausführung (GitHub-Klon scheitert an DNS). **Kein grüner Build, keine APK-Freigabe.**
- **Projekt-Chats:** Sichtbarer Kontext aus Main/Schedule: GPS-Aussetzer auf freier Strecke, Trackverlust, Rerouting und Konfigurationsverlust bei Rotation. Vollständiger Live-Zugriff auf andere Chats nicht bestätigt; keine neuen Anforderungen erfunden.
- **Nächster Schritt:** Push-Lauf für `a5fb126` und dessen Preflight-Job-Zusammenfassung prüfen; den zuerst fehlschlagenden Befehl gezielt beheben. Danach Rotation-Zustandserhalt (#13) und GPS-Track-Replay/Rerouting (#14) mit ausführbaren Tests angehen.

## Entwicklungsabgleich – 18.11.2026, 08:20 MEZ

- **Live-Repository:** Vor der Änderung `main=d7db9b2`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Chat-Übergabe, Projektstatus und Workflow sowie alle drei Preflight-Skripte abgeglichen. Ältere Kopfzeilen dieser Datei sind historische Snapshots.
- **Neue Änderung:** `dbbcfcb` ergänzt in `scripts/test-ci-concurrency.py` statische Regressionen, die das Vorhandensein des fehlertoleranten Diagnose-Schritts, dessen `if: failure()`-Bedingung, das `GITHUB_STEP_SUMMARY` und die erneute Einzelprüfung aller drei Preflight-Kommandos absichern. Keine Signing-/Secret-Änderungen.
- **Nachweisgrenze:** Änderung über GitHub committed; kein lokaler vollständiger Python-/Gradle-Lauf, da der Container `github.com` nicht per DNS auflösen kann und daher kein Checkout möglich ist. Der zuvor bestätigte Actions-Lauf #404 (Run-ID `44179252791`) scheiterte im Preflight; das ist **kein** Beleg für den Status von `dbbcfcb`. Die verfügbare commitbezogene Workflow-Abfrage erfasst Push-Läufe nicht vollständig. **APK nicht freigegeben.**
- **Chat-Abgleich:** Bekannte Anforderungen aus dem sichtbaren Main-/Schedule-Kontext (GPS-Lücken, Track-Erhalt, Rerouting, Rotation, Dashboard) berücksichtigt; andere vollständige Chatverläufe nicht live nachgewiesen.
- **Nächster Schritt:** Den neuesten tatsächlichen Push-Actions-Lauf einschließlich Diagnose-Zusammenfassung verifizieren; ersten fehlschlagenden Befehl beheben. Danach deterministische Tests zu #14 (Track-Erhalt über GPS-Lücke und Rerouting) und #13 (Zustand bei Rotation).


## Entwicklungsabgleich – 18.11.2026, 10:20 MEZ

- **GitHub-Baseline:** Vor diesem Eintrag ist `main` auf `d0bdf8f9f836efb85ee56ef52e80e0d4f32dc167`; letzter Test-Commit `dbbcfcb` ergänzt statische Regressionen zur Preflight-Diagnose. 11 offene Issues, keine offenen PRs. Kritisch: #13 Rotation verwirft Konfiguration/Route, #14 GPS-Track verschwindet. Keine neuen Commits seit dem vorherigen Abgleich.
- **CI-Belege und Grenze:** Die Commit-Statusabfrage für `d0bdf8f` ergibt `statuses=[]`; die verfügbare Workflow-Abfrage liefert nur PR-getriggerte Runs und ist für Push-Runs nicht vollständig. Beim älteren Actions-Lauf [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) ist `preflight=failure`, `build-and-test=skipped`; GitHub liefert zu dessen Preflight-Job weder Einzelschritte noch Logs. Dieser ältere Lauf ist kein Nachweis für den aktuellen HEAD.
- **Quelltextanalyse:** `.github/workflows/android.yml` ruft die drei Preflight-Kommandos separat auf und enthält die zusätzliche Diagnose mit `if: failure()`; `scripts/test-ci-concurrency.py` prüft die Diagnose statisch. Die Skripte wurden hier gelesen, nicht ausgeführt. Der erste fehlschlagende Befehl ist weiterhin unbekannt; ohne Fehlerausgabe keine blinde Änderung. Keine Signing-/Secret-Änderung, keine APK-Freigabe.
- **Konkreter Entblockungsplan:** (1) In GitHub Actions den neuesten *Push*-Run von `main` und dessen Job-Summary öffnen (nicht nur Commit-Status/PR-Run-Abfrage). (2) Falls keine Diagnose erscheint, Preflight auf einem Checkout desselben SHA mit `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` einzeln ausführen und stderr/Exitcodes sichern. (3) Den reproduzierten Fehler minimal korrigieren und echte CI verifizieren. (4) Anschließend #13 und #14 durch Zustands-/GPS-Replay-Regressionstests bearbeiten.
- **Chat-Abgleich:** Sichtbare bekannte Anforderungen aus Main/Schedule zu GPS-Aussetzern, Rerouting, Track-Erhalt und Rotation berücksichtigt; vollständiger Live-Zugriff auf andere Chats ist nicht belegt.


## Entwicklungsabgleich – 18.11.2026, 12:18 MEZ

- **Verifizierter GitHub-Stand vor diesem Eintrag:** `main=89f8ef32a7380f0fa66347354b9b28f6efc0ed63`; elf offene Issues, keine offenen PRs. Zuletzt dokumentierte kritische Tickets: #13 Rotation/Route-State, #14 GPS-Trackverlust. README, Roadmap, Chat-Handoff, Android-Workflow und drei Preflight-Skripte direkt aus dem Repository geprüft.
- **CI-Nachweisgrenze:** Die verfügbare GitHub-Connector-Funktion `fetch_commit_workflow_runs` erfasst nur PR-getriggerte Läufe und ist **kein** Nachweis über Push-Läufe. Der öffentliche Actions-Webabruf war in diesem Lauf technisch gesperrt (`DisabledError`). Der ältere bekannte Lauf #404 war rot, lässt aber keinen Schluss auf den aktuellen HEAD zu. **Keine neue Testausführung und keine APK-Freigabe.**
- **Konkrete Code-/Testprüfung:** `scripts/test-ci-concurrency.py` verlangt die drei getrennten Preflight-Kommandos, den Diagnose-Schritt und die Build-/Release-Abhängigkeiten; `.github/workflows/android.yml` enthält diese Schritte. `scripts/preflight.sh` prüft Shell- und Python-Syntax; `scripts/test-emulator-navigation.py` enthält acht XML-Navigationsfälle. Dies sind statische Befunde, keine bestandenen Tests.
- **Reproduktionsplan für die Blockade:** Auf einem Checkout **genau des HEAD** nacheinander `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` ausführen; den ersten Exitcode ungleich null samt vollständigem stderr sichern. Alternativ den aktuellen Actions-Preflight-Job einschließlich Step Summary und Log auslesen. Erst dann gezielt reparieren; kein blindes Abschwächen des Gates.
- **Danach:** #14 reproduzierbaren GPS-Track-Erhalt bei GPS-Lücken und Rerouting mit Regressionstest absichern; #13 Rotation mit wiederhergestelltem Routen-/Einstellungszustand testen. Änderungen anderer Chats nur übernehmen, wenn in GitHub oder zugänglichem Kontext nachgewiesen.


## Entwicklungsabgleich – 18.11.2026, 14:16 MEZ

- Vor Arbeitsbeginn geprüft: `main` auf `33caef3`, README, Roadmap, Chat-Handoff, Android-Workflow, Preflight-Skripte, letzte Commits, elf offene Issues (#3–#11, #13, #14), keine offenen PRs. Die hier zugänglichen Projekt-Chats bestätigen Rotation/Trackverlust; vollständiger Live-Zugriff auf andere Chats ist nicht nachgewiesen.
- **Neues Arbeitspaket:** `37ee917` fügt `docs/REGRESSION-ROTATION-GPS.md` hinzu. Konkrete reproduzierbare Abnahmematrix für #13 (Konfiguration und aktive Navigation bei Rotation) und #14 (angenommene GPS-Spur bleibt bei GPS-Lücken, Reroute und Activity-Recreation erhalten), mit deterministischen Testideen und Geräteprüfung. **Nur Testspezifikation, keine Fehlerbehebung.**
- CI: Die verfügbare commitbezogene Workflow-Abfrage deckt Push-Läufe nicht zuverlässig ab; kein grüner Build für diesen Commit nachgewiesen. In diesem Lauf keine Gradle-/Emulatortests ausgeführt. Keine APK-Freigabe.
- **Nächster Schritt:** Zuerst aktuellen Push-Preflight anhand echter Actions-Fehlerausgabe entblocken; anschließend ActivityScenario-Recreation-Test für #13 und Zustands-/Reroute-Tests für #14 implementieren. Keine redundanten reinen Status-Commits ohne Erkenntnisgewinn.

## Automatischer Entwicklungsabgleich – 18.11.2026, 15:18 MEZ

- **Repository:** HEAD vor Änderung `a792758`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Status, Rotations-/GPS-Testmatrix, Workflow, Preflight- und Emulator-Navigationstests gelesen. Bekannte Anforderungen aus Main/Schedule berücksichtigt; kein vollständiger Live-Zugriff auf andere Chats.
- **Kleine konkrete Korrektur:** `4f61ff4` berücksichtigt bei der Suche nach anklickbaren Navigationslabels auch `displayed=false` bzw. `enabled=false` auf übergeordneten UIAutomator-Knoten. Vorher konnte ein scheinbar sichtbares Kind unter einem versteckten/deaktivierten Container als Tap-Ziel ausgewählt werden. `e36219a` ergänzt drei Regressionstests (versteckter Elternknoten, deaktivierter Elternknoten, ausschließlich versteckter Treffer). Keine Signing-/Secret-Änderung.
- **Verifikation:** Quelltext und Tests committed, aber **nicht lokal ausgeführt**; aktueller Push-Actions-Status über die verfügbare PR-gefilterte Commit-Abfrage nicht sicher feststellbar. Keine Aussage über grünen Build und **keine APK-Installationsfreigabe**.
- **Nächster Schritt:** `python3 -u scripts/test-emulator-navigation.py`, `python3 scripts/test-ci-concurrency.py`, `bash scripts/preflight.sh` auf aktuellem Checkout ausführen; dann aktuellen Push-Workflow und ggf. Fehlerlogs prüfen. Anschließend #13/#14 anhand der vorhandenen Testmatrix in ausführbare Lifecycle-/Track-Tests überführen.

## Automatischer Entwicklungsabgleich – 18.11.2026, 16:18 MEZ

- **Repo-Abgleich:** Vor Änderung HEAD `df4a4f2`; README, Roadmap, Chat-Handoff, Workflow, Preflight-Skripte und Regressionstestplan gelesen. 11 offene Issues (#3–#11, #13, #14), keine offenen PRs. #13 Rotation und #14 Trackverlust weiterhin nicht behoben.
- **Eigene Änderung:** Commit `679f019` erweitert `scripts/test-emulator-navigation.py` um drei deterministische Tests: versteckter Elternknoten mit Content-Description, deaktivierter Großelternknoten und defektes UI-XML. Das ist Testabdeckung für den bestehenden Navigationshelfer, kein Fix für #13/#14.
- **Ausführung/CI:** Git-Checkout in der Laufumgebung scheitert an DNS (`Could not resolve host: github.com`). Die drei neuen Tests wurden nicht ausgeführt; GitHub-Commit-Workflow-Abfrage erfasst Push-Läufe nicht zuverlässig. Für den neuen HEAD ist kein grüner Build nachgewiesen; **keine APK-Installationsfreigabe**.
- **Nächste Priorität:** GitHub-Actions-Push-Lauf über Actions-Oberfläche oder einen verlässlichen Run-Endpunkt samt Preflight-Einzelschritten prüfen. Danach #13 Activity-Recreation und #14 Track/Reroute-Reducer mit ausführbaren Tests beheben. Andere Projekt-Chats sind nicht vollständig live zugänglich; keine erfundenen neuen Anforderungen.


## Entwicklungsabgleich – 18.11.2026, 19:16 MEZ

- **Repository:** Zu Beginn des Laufs war `main` auf `14dd161057c2eb22cb86e25bb42ae2731889e2a5`. README, Roadmap, ADRs, Rotation/GPS-Regressionsmatrix und Android-Workflow gegen die letzten Commits geprüft. Elf offene Issues (#3–#11, #13, #14), keine offenen PRs. Die ältere Tabelle „Offen (9)“ oben ist ein historischer Stand und darf nicht als aktuelle Ticketliste interpretiert werden.
- **Konkrete Dokumentationsabweichung:** Der Kopf dieser Datei nennt noch 16.11.2026 und die alte Neun-Issue-Baseline, obwohl die jüngsten Commits bis `14dd161` und die zwei neuen Bugs #13/#14 hinzugekommen sind. Dieser Eintrag ergänzt die nachvollziehbare aktuelle Baseline, ohne alte Audit-Einträge umzuschreiben.
- **CI-Nachweisgrenze:** Die Commit-Workflow-Abfrage für `14dd161` ergab keine PR-Läufe; diese Schnittstelle erfasst Push-Läufe nicht zuverlässig. Der ältere Actions-Lauf 44179252791 meldet `preflight=failure`, `build-and-test=skipped`; seine Jobdetails enthalten keine Schritte/Logs. Das ist **kein** Beleg für den Buildzustand von `14dd161`. Die drei aktuellen Preflight-Schritte sind `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh` und `python3 -u scripts/test-emulator-navigation.py`. Keiner davon wurde in diesem Lauf ausgeführt.
- **Projekt-Chats:** Anforderungen aus den zugänglichen Projektkontexten Main/Schedule (GPS-Aussetzer, verschwindender violetter Track, Rotation, Rerouting) mit #13/#14 abgeglichen; kein vollständiger Live-Zugriff auf alle Chatverläufe behauptet.
- **Nächste ausführbare Schritte:** (1) Aktuellen Push-Actions-Lauf mit Preflight-Step-Ausgabe beschaffen oder die drei Kommandos auf Checkout von `main` ausführen; den **ersten reproduzierten** Fehler reparieren. (2) #13: Activity-Recreation-Test für ausgewählte Route und Einstellungen. (3) #14: reinen State-Test für akzeptierten GPS-Track über Aussetzer, Reroute und Rotation. Details und Abnahmekriterien in `docs/REGRESSION-ROTATION-GPS.md`.
- **Freigabe:** Kein neuer Test-/Emulator-/Gerätenachweis; keine APK-Installationsfreigabe.


## Entwicklungsabgleich – 18.11.2026, 20:22 MEZ

- **GitHub-Baseline:** Neuester über Commit-Suche sichtbarer `main`-Commit `c36e46a2af940380ff448fb94b57e2ebb164144c`; elf offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Status, ADRs, Android-Workflow und Rotations-/GPS-Testmatrix erneut gelesen. Die bisher veraltete Kopfzeile, Ticketzahl und Prioritäten oben wurden auf diesen Abgleich korrigiert.
- **CI-Nachweis:** `fetch_commit_workflow_runs` für `c36e46a` liefert `[]`, filtert aber PR-getriggerte Läufe; **kein Beleg** für Push-Buildzustand. Der Workflow enthält drei getrennte Preflight-Schritte und einen Fehlerdiagnose-Summary-Step. Die konkrete erste Fehlerausgabe bleibt unzugänglich; keine Tests ausgeführt und keine APK-Freigabe.
- **Projektkontext:** Bekannte Main-/Schedule-Anforderungen zu GPS-Lücken, Rerouting, dunkellila Track und Rotation sind als #13/#14 und in der Regressionstestmatrix abgebildet. Kein vollständiger Live-Zugriff auf andere Projekt-Chats nachgewiesen.
- **Nächste technische Aktion:** Aktuellen Actions-Push-Lauf und dessen Preflight-Summary abrufen oder auf Checkout desselben SHA `python3 scripts/test-ci-concurrency.py`, `bash -x scripts/preflight.sh`, `python3 -u scripts/test-emulator-navigation.py` ausführen. Danach reproduzierten Fehler minimal beheben; #13/#14 als ausführbare Tests umsetzen. Keine Änderung an Signing oder Secrets.


## Automatischer Entwicklungsabgleich – 18.11.2026, 21:15 MEZ

- **Repository:** Neuester vor diesem Lauf ermittelter Commit `316b06d` auf `main`; elf offene Issues, keine offenen PRs. README, Roadmap, ADR, GPS-/Rotations-Testmatrix, Preflight und Android-Workflow erneut gelesen.
- **Dokumentationskorrektur:** Den widersprüchlichen historischen Hinweis „neun offene Issues“ explizit als überholten Snapshot gekennzeichnet; keine Änderung an App- oder Signing-Code.
- **CI-Nachweisgrenze:** Die kombinierte Commit-Status-Abfrage für `316b06d` liefert keine Statusmeldungen. Das ist **kein** Beweis für einen erfolgreichen oder fehlgeschlagenen Push-Workflow. Aktuelle Push-Joblogs wurden nicht eingesehen; die drei Preflight-Befehle wurden in diesem Lauf nicht ausgeführt. Kein grüner Build und keine APK-Freigabe.
- **Projektkontext:** Bekannte Fehler #13 (Rotation) und #14 (GPS-Spur) bleiben offen; vollständiger Zugriff auf andere Chatverläufe ist nicht nachgewiesen.
- **Nächster konkreter Schritt:** Preflight-Befehle auf dem exakten `main`-Checkout ausführen bzw. Actions-Push-Joblog beschaffen, danach erste reproduzierbare Ursache beheben. Anschließend instrumentierten Activity-Recreation-Test und GPS-Track-State-Regression für #13/#14 implementieren.

## Automatischer Entwicklungsabgleich – 18.11.2026, 22:24 MEZ

- **Baseline:** vor diesem Lauf `7b957e5`; elf offene Issues, keine offenen PRs. README, Roadmap, ADR, CI-Workflow, Preflight-Skripte und Regressionstestplan gelesen. Aus dem sichtbaren Projektkontext bleiben #13 (Rotation) und #14 (GPS-Spur) die kritischen funktionalen Fehler.
- **Kleine Korrektur:** `16abff1` behandelt in der Emulator-Navigationszielsuche zusätzlich Androids `visible-to-user="false"` und Groß-/Kleinschreibungsvarianten von `displayed`/`enabled`. `ee99489` ergänzt zwei passende Offline-Regressionstests. Keine Änderungen an App-GPS, Routing oder Signing; diese Bugs bleiben offen.
- **CI-Grenze:** Der ältere Workflow-Lauf [#404](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/44179252791) hat einen fehlgeschlagenen `preflight`-Job und übersprungene Build-Jobs. Der Abruf des Preflight-Joblogs (`134378837918`) scheiterte; damit ist die konkrete Fehlerzeile nicht belegt. Der Commit-Workflow-Connector erfasst nur PR-Läufe, nicht zuverlässig Push-Läufe. **Neue Tests nicht ausgeführt, kein grüner Build nachgewiesen, keine APK-Installationsfreigabe.**
- **Nächste Aktion:** Aktuellen Push-Lauf samt Jobschritten/Logs oder Step Summary abrufen und zuerst den tatsächlich fehlgeschlagenen Preflight-Befehl reparieren. Danach instrumentierte Rotationstests (#13) und deterministische GPS-/Rerouting-Tracktests (#14) implementieren. Andere Chats sind nicht vollständig live einsehbar; keine Chat-Synchronisierung behauptet.


## Automatischer Entwicklungsabgleich – 18.11.2026, 23:18 MEZ

- **Repository:** vor der Änderung HEAD `bc22d50`; README, Implementierungs-Roadmap, zentraler Status, Rotations-/GPS-Testplan, CI-Workflow, Preflight-Skripte und letzte Commits geprüft. 11 offene Issues, keine offenen PRs.
- **Fix:** `c2df0d5` normalisiert Leerzeichen vor/nach den Android-UI-Hierarchieattributen `displayed`, `visible-to-user` und `enabled`. Dadurch können Werte wie `" FALSE "` nicht als sichtbar/aktiv durchrutschen. Produktivnavigation unverändert.
- **Regressionen:** `efb00fa` ergänzt zwei Python-Tests für unsichtbare doppelte Labels und deaktivierte Elterncontainer mit Whitespace. Tests sind committed, aber in diesem Lauf nicht ausgeführt.
- **CI-Nachweis:** Die commitbezogene Workflow-Abfrage für `efb00fa` liefert keine PR-Läufe und ist kein Beleg für Push-CI. Der ältere Lauf 44179252791 hat `preflight=failure`, `build-and-test=skipped`; keine Aussage über neuen HEAD. Keine APK-Installationsfreigabe.
- **Chat-Abgleich:** Bekannte Anforderungen aus Main/Schedule (Rotation #13, GPS-Spur #14, Rerouting, MVV) berücksichtigt; vollständige Live-Einsicht in alle Chats nicht möglich.
- **Nächster Schritt:** Neue Python-Tests und alle drei Preflight-Schritte tatsächlich ausführen, Push-Workflow-ID und Fehlerausgabe beschaffen. Anschließend #13/#14 mit deterministischen Android-/GPS-Regressionen bearbeiten.

## Automatischer Entwicklungsabgleich – 19.11.2026, 05:23 MEZ

- **HEAD vor Änderung:** `2d39b00`; elf offene Issues, keine offenen PRs. README, Projektstatus, Rotations-/GPS-Regressionstestplan, Workflow und MainActivity-State gelesen.
- **Arbeitspaket:** `docs/ACTIVITY-RECREATION-STATE-CONTRACT.md` neu angelegt (Commit `b1deb2d`): konkrete Eigentümer-/Lebensdauer-Matrix für Routen, Track, Auswahl, Einstellungen und asynchrone Callbacks; atomarer ViewModel-Migrationsplan und reproduzierbare `ActivityScenario.recreate()`-Abnahme. Warnung vor inkonsistentem `rememberSaveable`-Teilfix.
- **Tests/CI:** Keine Tests ausgeführt. Commit-Statusabfrage für `2d39b00` lieferte keine Statuschecks; Push-Actions-Lauf nicht nachgewiesen. Keine APK-Freigabe.
- **Nächster Schritt:** Instrumentierten roten Rotationstest auf Basis des neuen Vertrags implementieren; danach zusammenhängenden Navigationszustand in ViewModel auslagern, GPS-/Rerouting-Regressionen nachziehen.
- **Chats:** Bekannte Meldungen aus Main/Schedule berücksichtigt; kein vollständiger Live-Zugriff auf alle Chats.

## Automatischer Entwicklungsabgleich – 22.11.2026, 23:24 MEZ

- **GitHub-Baseline:** vor Änderung `3d0f36e`; 11 offene Issues (#3–#11, #13, #14), keine offenen PRs. README, Roadmap, Activity-Recreation-Vertrag, Navigationstest, Tap-Helfer und Android-Workflow geprüft. Andere Projekt-Chats sind nicht vollständig live abrufbar; bekannte Anforderungen zu Rotation, GPS-Spur und Rerouting bleiben #13/#14 zugeordnet.
- **Arbeitspaket:** `bd47a8c` ergänzt `test_failed_hierarchy_read_never_taps` in `scripts/test-emulator-navigation.py`. Der Test simuliert einen fehlgeschlagenen `adb exec-out cat` nach erfolgreichem UI-Dump und fordert, dass niemals `adb shell input tap` ausgeführt wird. Testdatei nach Commit erneut gelesen; keine Änderung an Produktivcode, Signing oder Secrets.
- **Tests/CI:** Python-Test und Android-Build in diesem Lauf **nicht ausgeführt**. `fetch_commit_workflow_runs(bd47a8c)` ergab `[]`, aber dieser Connector berücksichtigt nur PR-getriggerte Läufe; kein Nachweis zum Push-Build. Keine APK-Installationsfreigabe.
- **Nächste Priorität:** `python3 -u scripts/test-emulator-navigation.py` auf dem exakten HEAD ausführen und aktuelle Push-Preflight-Logs auswerten; danach funktionale Activity-Recreation- (#13) und GPS-Track-Regressionen (#14) statt weiterer ausschließlich UI-Helfertests priorisieren.
