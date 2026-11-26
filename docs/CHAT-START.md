# MeinWeg2 – Chat-Einstiegspunkt / Agenten-Handoff

**Stand der Erstellung:** 16.11.2026. **Repo:** `jochenkunkel/mvv-mobility-assistant`.
**Branch:** `main`. Diese Datei ist ein **Startprotokoll**, kein eingefrorener
aktueller Codezustand. Bei jeder neuen Sitzung GitHub erneut lesen.

## 1. Auftrag und Arbeitsstil

Du bist Entwicklungsagent für **MeinWeg2**, eine Android-App für München:
Kalendertermin → GPS-Start → MVV-Verbindungen → Fahrt-Dashboard → präzise
OSM-Fußwegnavigation. Der Nutzer möchte eine im Alltag funktionierende,
optisch saubere App mit wenigen Klicks. Designziel: drei Tabs
**Start / Verbindungen / Meine Fahrt**, gut lesbare Kacheln, klarer
Start- und Zielzustand, kompakte in Gehrichtung orientierte Minikarte
(ungefähr 200–300 m voraus), Routenlinie und Bewegungsvektor.

**Verbindliche Nutzerpräferenz:** kleine, nachvollziehbare Änderungen
**direkt in `main`**; nicht unnötig PR-Branches eröffnen. Vor Commit
Quellen und CI-Status prüfen, bei Konflikten neu lesen. Jede Änderung
mit Tests und Dokumentation absichern. Keine unbelegten „fertig“-, „Build
bereit“- oder Gerätefreigabe-Aussagen. Tickets nur schließen, wenn
Akzeptanzkriterien erfüllt und Nachweise vorhanden sind.

**Keine automatische Hintergrundarbeit behaupten.** Andere Chats sind
nicht live synchronisiert. GitHub-Code, Issues, CI und diese Dateien sind
der gemeinsame, persistente Übergabepunkt.

## 2. Pflichtlektüre in dieser Reihenfolge

0. `docs/PUBLIC_RELEASE_READINESS.md` – Sicherheitsbewertung, Freigabegrenzen und offene Entscheidungen (Lizenz, Artefakte, `public`-Umsstellung).
1. `docs/PROJECT_STATUS.md` – aktueller Arbeitsstand, Tickets, letzte Commits.
2. `README.md` – Projektüberblick, Einstieg, Build-/Testhinweise.
3. `docs/DECISIONS.md` – bindende UX- und Architekturentscheidungen.
4. `docs/IMPLEMENTATION-ROADMAP.md` – Phasen und Abnahmekriterien;
   ältere Tabellen sind historische Snapshots.
5. `docs/CODE-REVIEW-2026-11-16.md` – datierter Audit und Risikokatalog,
   nicht automatisch der aktuelle Stand.
6. `docs/COCKPIT-V2-PLAN.md` – Produktanforderungen und UX-Referenz.
7. Aktuelle GitHub-Issues und GitHub-Actions-Runs **live** prüfen.

Weitere datierte Statusdateien dienen als historische Belege und
dürfen den aktuellen `main`-Stand nicht überschreiben.

## 3. Technische Orientierung

- Android/Kotlin, Jetpack Compose, Gradle; Workflow
  `.github/workflows/android.yml`.
- Einstieg/UI: `app/src/main/java/de/mvv/mobility/MainActivity.kt`.
- MVV-Verbindungssuche und EFA-Parsing:
  `app/src/main/java/de/mvv/mobility/data/MvvJourneyRepository.kt`.
- OSRM-Fußwegrouting:
  `app/src/main/java/de/mvv/mobility/data/WalkingNavigation.kt`.
- Route-Projektion/Abbiegungen: `data/RouteProgress.kt`.
- GPS: `data/CurrentLocationRepository.kt` und `data/LocationFreshness.kt`.
- OSM-Minimap: `ui/components/WalkingMiniMap.kt`.
- Tests: `app/src/test/`; Instrumentierung: `app/src/androidTest/`.
- CI: Unit-Tests, APK, Android Lint auf Push/PR; Emulator und
  Live-Probes nach Workflow-Konfiguration bei Release/Tags.
- **Achtung:** „Gradle erfolgreich“ ersetzt weder reale Navigation
  noch optische Sichtprüfung und Geräte-Tests.

## 4. Letzter dokumentierter Fortschritt

- Issue #2 NAV-01: Routengebundene Abbiegeauswahl (`f38ef6e`);
  geschlossen, reale Geräteabnahme weiterhin wichtig.
- #3 NAV-02: Segmentprojektion für Minikartenrichtung
  (`5a40a25`), **offen**.
- #6 LOC-01: monotone GPS-Freshness in Repository-Cache und Tests
  (`206e549`), **offen**.
- #8 CI-01: Lint auf jedem Push/PR (`fe7ee3b`), **offen**.
- #10 DOC-01: Roadmap-Abgleich (`158a151`, `ea479c8`),
  **offen**.
- Zentrale Statusdatei `docs/PROJECT_STATUS.md` erstellt
  (`bf40b50`), README-Verweis (`26ade2b`).

**Zuletzt live gesehene offene Issues (9):** #3 Minikarte, #4 exakte
Einstiegshaltestelle, #5 Datum/Zeitzone, #6 GPS, #7 Architektur,
#8 CI, #9 Golden Screenshots/Geräte-Matrix, #10 Dokumentation,
#11 OSM/Datenschutz. **Vor Arbeitsbeginn erneut prüfen**: diese
Aufzählung ist ein Snapshot.

**Nicht nachgewiesen:** vollständige grüne CI-/Lint-/Emulator-Matrix
und realer Gerätetest für alle jüngsten Änderungen. Keine
Produktionsfreigabe ableiten.

## 5. Priorisierte Weiterarbeit

1. GitHub `main`-HEAD, Issues, laufende/fehlgeschlagene Actions prüfen.
   Wenn CI rot: Fehler vor neuen Features beheben.
2. #4: Fußweg zur **tatsächlich gewählten MVV-Einstiegshaltestelle**
   anhand Etappenkoordinaten/Stop-ID statt Namens-Geocoding; Tests.
3. #5: absolute MVV-Zeitstempel inkl. Datum, Zeitzone,
   Mitternacht und DST; Tests.
4. #6 und #3: GPS-/Routengeometrie, Off-Route und Minikartenkamera
   vollständig absichern.
5. #8/#9: Release-Gates und echte visuelle Vergleiche;
   #7 Architektur; #11 Datenschutz; #10 Dokumentationskonsistenz.
6. Tickets erst nach nachgewiesener Abnahme schließen.

## 6. Standardablauf für jede neue Sitzung

1. **Lesen:** diese Datei + `docs/PROJECT_STATUS.md`, `README.md`,
   `docs/DECISIONS.md`; Issues und CI live.
2. **Abgleichen:** Was hat sich seit dem letzten dokumentierten Commit
   geändert? Welche Änderungen anderer Chats sind bereits in `main`?
3. **Planen:** ein klar begrenztes Ticket, konkrete Akzeptanztests.
4. **Implementieren:** kleine Commits direkt nach `main`,
   gezielte Unit-Tests, Build/Lint und bei UI-Arbeit Emulator-Screenshots.
5. **Validieren:** echte CI-Ergebnisse und Artefakte kontrollieren;
   fehlende Nachweise als offen markieren.
6. **Übergeben:** `docs/PROJECT_STATUS.md` aktualisieren mit Datum,
   Commit-SHA, geänderten Dateien, Tests und Ergebnissen, offenen Risiken,
   Issues und nächstem Schritt. Bei grundsätzlichen Entscheidungen
   `docs/DECISIONS.md` ergänzen.
7. **Berichten:** Nutzer auf Deutsch kurz informieren: was geändert,
   Commit/Build-Link, CI-Status, was noch offen ist.

## 7. Copy-Paste für einen neuen Chat

> Ich arbeite am GitHub-Projekt
> `jochenkunkel/mvv-mobility-assistant` (MeinWeg2). Bitte lies zuerst
> `docs/CHAT-START.md`, dann `docs/PROJECT_STATUS.md`, `README.md`,
> `docs/DECISIONS.md` und die aktuellen GitHub-Issues/Actions direkt
> aus `main`. Behandle die Dateien als Übergabe, aber verifiziere den
> aktuellen Code und CI-Stand selbst. Arbeite an den offenen Tickets
> priorisiert, committe kleine Änderungen direkt nach `main`, teste
> sie nachvollziehbar und aktualisiere nach jeder Arbeit
> `docs/PROJECT_STATUS.md`. Schließe Tickets erst nach nachgewiesener
> Abnahme. Berichte auf Deutsch mit Commit, Tests, Risiken und nächstem
> Schritt. Andere Chats sind nicht automatisch synchronisiert:
> GitHub ist die gemeinsame Quelle.
