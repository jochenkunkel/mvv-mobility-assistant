# MeinWeg2 – zentraler Projektstatus

**Stand:** 16.11.2026 · **Verbindliche Codebasis:** GitHub `main`

> **Startpunkt für alle Chats/Agenten:** Diese Datei zuerst lesen, anschließend
> `main`, offene GitHub-Issues und CI-Läufe live abgleichen. Diese Datei ist
> eine Übergabe und kein Ersatz für den tatsächlichen Repository-Status.

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

**Offen (9):**

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

Issue-Übersicht: https://github.com/jochenkunkel/mvv-mobility-assistant/issues

## Nächste Schritte (priorisiert)

1. CI von `main` prüfen, insbesondere neue Lint-Pflicht und GPS-Tests;
   Fehler sofort reparieren.
2. #4: Einstiegshaltestelle anhand der tatsächlich ausgewählten MVV-Etappe
   bestimmen; Geocoding-Fallback nicht als exakte Haltestelle ausgeben.
3. #5 und #6: Zeit-/GPS-Korrektheit einschließlich Randfalltests.
4. #3 und #9: Minikarten-Ausrichtung und visuelle Geräte-Abnahme.
5. #8, #7, #11, #10: CI, Architektur, Datenschutz und Dokumentation.

## Abnahme und Aktualisierungsprotokoll

Für jede Änderung: **Commit/Dateien**, **Testbefehl + echtes Ergebnis**,
**Issue-Status**, **bekannte Risiken** und **nächster Schritt** festhalten.
Keine erfundenen CI-Ergebnisse oder pauschale „fertig“-Meldung.

**Stand dieses Eintrags:** Basierend auf dem in GitHub gelesenen `main`
und neun offenen Issues am 16.11.2026. Nachfolgende Commits/Issues können
diesen Snapshot überholen und müssen vor der nächsten Arbeit erneut
abgeglichen werden.

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
