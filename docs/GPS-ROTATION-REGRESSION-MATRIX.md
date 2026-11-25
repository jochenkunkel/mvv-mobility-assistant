# Regression matrix: active navigation state and GPS track (#13 / #14)

Status: **Abnahmematrix mit partieller Testimplementierung**, aktualisiert 2026-11-25. Es existieren inzwischen instrumentierte Activity-Recreation-Smoke-Tests (siehe `docs/PROJECT_STATUS.md`, Commits `3503ec4` und frühere); sie belegen weder vollständige ROT-01-Abdeckung noch einen erfolgreichen Testlauf. Die GPS-01–06-Szenarien bleiben als vollständige End-to-End-Abnahme offen. Related decision: [ADR-010](DECISIONS.md).

## Invariants
- The selected itinerary, active navigation state and settings are not reset by an Activity recreation caused by rotation.
- The accepted **actual GPS trace** is independent of the OSRM/OSM route polyline and the route-progress projection. A route recalculation must never replace the trace with the new planned route.
- Rejecting an inaccurate, stale, implausible or missing fix does not delete previously accepted trace points. A long gap must not be rendered as an invented straight segment.
- A Compose recomposition or map redraw is presentation work, not a new navigation session.
- Trace data remain transient: no implicit disk history or background tracking is introduced by a fix. Explicit end-of-navigation clears session data; process death requires a separately agreed restoration policy.

## Deterministic acceptance scenarios
| ID | Setup / action | Observable assertion |
| --- | --- | --- |
| ROT-01 | Start a route with changed home/buffer/speed settings; rotate portrait → landscape → portrait | Settings remain unchanged; selected itinerary, destination, navigation mode and already accepted track points remain identical |
| ROT-02 | During active route trigger a UI redraw/recomposition | No new route selection or track reset; map shows the same accepted points |
| GPS-01 | Feed 3 valid timestamped fixes in order | Exactly 3 ordered accepted trace points; drawn line follows actual fixes, not projected route |
| GPS-02 | Feed valid fix, inaccurate/stale fix, valid fix | Invalid fix not appended; first accepted fix preserved; trace remains consistent |
| GPS-03 | Feed valid fix, large time gap, valid fix | Both accepted points retained where policy allows; **no connecting line** across the gap |
| GPS-04 | Feed plausible fix followed by impossible location jump | Jump rejected or separated by explicit gap policy; no teleport line and no clearing of earlier trace |
| GPS-05 | Feed accepted fixes then reroute to new OSRM geometry | Actual trace retains original coordinates and count; planned polyline changes independently |
| GPS-06 | Temporarily lose permission/provider, then restore and feed valid fix | Visible degraded state during loss; existing trace preserved; new fixes append according to gap policy |
| LIFE-01 | Explicitly end navigation | Transient trace and active navigation session cleared intentionally; persisted user settings retained |
| LIFE-02 | Kill process and relaunch | Behavior matches an explicitly documented process-death policy; do not claim recovery unless implemented |

## Implementation order
1. Identify the single owner of navigation session state and the accepted-fix/segment data model. Record actual source locations in the implementing PR; do not assume Activity fields survive recreation.
2. Extract pure state transitions for accepting/rejecting fixes and segment boundaries; write JVM unit tests for GPS-01 through GPS-05 before changing UI rendering.
3. Bind map rendering to accepted trace segments, separate from OSRM planned route and route progress; test redraw and reroute.
4. Add an Android instrumentation rotation test (ROT-01) using ActivityScenario.recreate / orientation changes and assertions against the actual screen/state. Validate permissions and lifecycle scenarios on an emulator and S25.
5. Run full CI and inspect actual screenshots/track behavior. **No APK installation approval until current gates are green and evidence exists.**

## Current evidence limits
The historical [Actions run #491](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45100034723) has a failed preflight and skipped downstream jobs; this does not identify the exact failing command. The GitHub commit-workflow lookup available in this session only covers PR runs and cannot establish current push CI. Run `python3 -u scripts/test-emulator-navigation.py`, `bash -x scripts/preflight.sh` and `python3 scripts/test-ci-concurrency.py` on an accessible checkout; obtain real exit codes and latest push Actions logs before attributing a CI cause.

## Abgleich 25.11.2026 – nächstes ausführbares Paket

- `main=502f86e0e639d01112813f362a1a8f5bd8885b2f`; Issues #13 und #14 offen, keine offenen PRs beim Abgleich.
- Vorhandene instrumentierte `ActivityScenario.recreate()`-Tests prüfen Dashboard und Ankunftspuffer im Fixture, **nicht** eine nichtleere akzeptierte GPS-Spur über einen echten Portrait/Landscape-Wechsel. Die ergänzten RouteProgress-JVM-Tests prüfen geplante Routengeometrie, **nicht** GPS-Trackpersistenz.
- **Nächstes kleines Codepaket:** Im vorhandenen Fixture eine deterministische akzeptierte Dreipunkt-Spur A/B/C und separate geplante Polyline bereitstellen; vor und nach `recreate()` Anzahl, Reihenfolge und unveränderte Koordinaten der akzeptierten Spur prüfen. Erst danach Orientation-Transitions und GPS-03/05 implementieren. Keine synthetische Route als tatsächliche Spur ausgeben.
- **Test-Gate:** CI auf `main` zuletzt ohne nachgewiesenen grünen Build; kombinierter Commit-Status für `502f86e` lieferte `statuses=[]` (kein Erfolgsnachweis). Letzter dokumentierter Actions-Run #525 fehlgeschlagen; Live-Actions-Joblogs konnten über den verfügbaren GitHub-Connector nicht neu abgefragt werden. Zuerst Runner-/Setup-Fehlermeldung administrativ ermitteln, dann JDK-17-/SDK-35-Tests und Emulator ausführen. Keine APK-Freigabe.
- Andere Projekt-Chats sind nicht vollständig live lesbar; keine zusätzlichen Anforderungen ohne überprüfbare Quelle behaupten.

## Testdesign 25.11.2026 – überprüfbarer ROT-01/GPS-05-Vertrag

**Bestandsprüfung:** In `MeinWegComposeUiTest.kt` existieren `activeWalkingDashboardSurvivesActivityRecreation` und `arrivalBufferSelectionSurvivesActivityRecreation`. Beide verwenden `ActivityScenario.recreate()`; keiner injiziert GPS-Fixes oder prüft Trackkoordinaten. Die bestehenden Semantics-Tags `walking_dashboard`, `journey_summary`, `tab_journey` zeigen UI-Zustand, nicht Trackdaten. Deshalb wäre ein zusätzlicher `assertExists()`-Test kein ausreichender Fix für #13/#14.

**Minimaler implementierbarer Vertrag (vor dem nächsten Test-Commit im Code verifizieren):**

1. Ein ausschließlich im Testmodus aktivierter Fixture-Injector liefert eine geplante Route R und unabhängig davon akzeptierte Fixes A/B/C mit festen Koordinaten, Zeitstempeln und monotoner Reihenfolge. Keine Live-MVV-/GPS-Abhängigkeit und keine Produktions-Sonderlogik außerhalb der Fixture-Grenze.
2. Ein auslesbarer Test-State-Snapshot enthält `sessionId`, `destinationId`, `acceptedFixes` (Koordinaten, Timestamp, Segment-ID), `plannedRoute` und `arrivalBufferMinutes`. Bei fehlendem Test-Hook zunächst einen reinen JVM-StateHolder-Test schreiben; keine Koordinaten aus Bildpixeln ableiten.
3. Vor `recreate()` Snapshot S0 aufnehmen; danach S1. Verbindliche Assertions: gleiche Session/Zielauswahl, identische drei akzeptierte Fixes in gleicher Reihenfolge, unveränderte Segmentgrenzen und unveränderter Puffer. Die geplante Route ist getrennt vom Track zu prüfen.
4. Danach Route R durch R2 ersetzen und S2 prüfen: `plannedRoute` ändert sich, `acceptedFixes` und Segment-IDs bleiben bytegleich (GPS-05). Erst im nächsten Schritt reale Portrait→Landscape→Portrait-Transitions mit denselben Assertions ergänzen.
5. Ein fehlgeschlagener Test ist als reproduzierter Red-Test zu dokumentieren, nicht durch Abschwächen der Assertions zu umgehen. Keine Behauptung, dass der StateHolder bereits existiert oder Rotation behoben ist.

**CI-Grenze:** Keine dieser Assertions wurde in diesem Dokumentationslauf ausgeführt. Der letzte bekannte Push-Run #525 scheiterte vor auswertbaren Testschritten; Runner-Setup/Billing/Policy bleiben unbestätigte Ursachen. APK nicht freigeben.


## Implementierungsstand 25.11.2026, 05:20 MEZ

- Neues **isoliertes, noch nicht an die UI angebundenes** Modell `data/WalkedTrace.kt` (Commit `7bd2b83`) hält akzeptierte GPS-Fixes unveränderlich in Segmenten; es ist unabhängig von der geplanten OSRM-Route. Es verwirft ungültige Koordinaten, ungenaue und nicht-monotone Fixes und trennt Segmente bei Zeitlücken >120 s.
- `WalkedTraceTest.kt` (Commit `8d75626`) enthält vier JVM-Regressionen für Reihenfolge, Ablehnung, Zeitlücke und Routenunabhängigkeit. **Nicht ausgeführt**; CI ist weiterhin ohne bestätigten grünen Lauf. Dies ist ein erster Baustein für GPS-01/02/03/05, keine End-to-End-Abnahme.
- **Offen und wichtig:** Geschwindigkeitssprünge/Teleportation (GPS-04), Provider-/Berechtigungsverlust (GPS-06), Zeitstempel-Frische relativ zur aktuellen Uhrzeit, StateHolder-/Activity-Recreation-Persistenz (#13), tatsächliche Overlay-Segmentdarstellung und Rerouting-Integration. `WalkingMiniMap` nimmt derzeit eine flache `walkedTrack`-Liste entgegen; bei Segmentintegration dürfen Lücken nicht wieder verbunden werden.
- **Nächstes Paket:** erst die GPS-04-Sprungprüfung deterministisch mit Tests ergänzen; anschließend den tatsächlichen Track-Eigentümer in `MainActivity` identifizieren und die UI-Anbindung inklusive Segmentgrenzen und Rotationstest vornehmen. Keine Freigabe ohne grüne CI.


## Entwicklungsabgleich 25.11.2026, 06:20 MEZ – GPS-04

- Repository vor Änderung: `ba1820c`; elf offene Issues, keine offenen PRs. README, Roadmap, Projektstatus, Matrix, GPS-Modell und Tests, Android-Workflow und letzte Commits abgeglichen.
- **Code:** `d1fc83f` ergänzt eine Haversine-basierte Geschwindigkeitsschranke (Standard 15 m/s) für kurze Intervalle zwischen akzeptierten Fixes. Unrealistische Sprünge werden verworfen, ohne die bisherige Spur zu löschen. Bei Zeitlücken über 120 s beginnt weiterhin ein neues Segment; dadurch wird keine Verbindungslinie über eine lange GPS-Lücke behauptet. Die Schranke ist ein vorläufiger Modell-Default, keine validierte Fußgänger-Kalibrierung.
- **Tests:** `141dca9` aktualisiert den Dreipunkt-Test auf plausible Abstände und ergänzt Regressionen für Sprungablehnung, konfigurierbare Geschwindigkeit und Segmenttrennung. Tests **nicht ausgeführt**; CI-Erfolg nicht belegt. Das Modell ist weiterhin nicht an den produktiven Track-StateHolder bzw. die Karte angeschlossen.
- **Nächste Schritte:** JDK-17-/SDK-35-JVM-Tests ausführen, die produktive GPS-Integration prüfen und erst danach Rotation/Overlay und Provider-Ausfälle absichern. Runner-Setup-Ursache der bekannten fehlgeschlagenen Actions-Preflights bleibt offen. Keine APK-Freigabe, keine Änderungen an Signing/Secrets.
- Andere Projekt-Chats sind nicht vollständig live abrufbar; keine unbelegten Anforderungen übernommen.
