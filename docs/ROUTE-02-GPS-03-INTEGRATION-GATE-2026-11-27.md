# ROUTE-02 / GPS-03 – Integrations- und Freigabecheck nach Fix 1094066

Stand: 27.11.2026, 10:16 MEZ. Abgleich gegen `main` bei `97b5173`, README, Roadmap, DECISIONS, CHAT-START, PROJECT_STATUS, GPS-03-Lifecycle-Audit, offene Issues/PRs und letzte Commits.

## Neuer Stand gegenüber der vorherigen Automation

Ein anderer Entwickler hat `1094066` (`fix(nav): stabilize progress across nearby route legs`) und `97b5173` (Status/Tests) gepusht. Laut aktuellem PROJECT_STATUS ist der routengebundene, zustandsbehaftete Fortschritt in `NavigationSessionViewModel` nun produktiv integriert; Jitter bei parallelen Abschnitten, Rückwärtsgehen, verspätete Fixes und Routenwechsel besitzen Sequenztests. Deshalb **keine zweite Hysterese implementieren**, ohne diesen Code und seine Tests zu prüfen.

Dokumentierter lokaler Nachweis aus dem Entwicklerstatus: 122 JVM-Tests erfolgreich, Lint 0 Fehler/40 Warnungen, API-35-Emulator 40/40 erfolgreich. Der übergreifende `connectedDebugAndroidTest`-Task endete wegen `INSTALL_FAILED_VERSION_DOWNGRADE` auf einem zusätzlich erkannten S10 **nicht grün**; auf dem S10 liefen 0 Tests. Diese Zahlen sind Entwicklerangaben im Repository, keine in diesem Lauf erneut ausgeführten Tests.

GitHub-Connector: Commit-Workflow-Abfrage für `1094066` liefert keine PR-getriggerten Runs; sie kann Push-Runs nicht vollständig nachweisen. **Kein bestätigter GitHub-CI-Erfolg für 1094066/97b5173.** Kein APK-Go.

## Nächste kleine Pakete – Reihenfolge

1. **P0 CI-Gate für 1094066:** Exakten Push-/Release-Run mit SHA, Jobliste und Schlussstatus prüfen. Abbruch/Fehler ausdrücklich vom grünen Emulator-Teillauf trennen. Keine APK-Freigabe vor belastbarem Gate und Geräteabnahme.
2. **P0 GPS-03 Integrationstest:** Den produktiven `observeForeground`-Callback über `MainActivity` und beide ViewModels verfolgen. Mit synthetischen Fixes und `ActivityScenario.recreate()` prüfen: A/B akzeptiert, Cache-B und alter A nach Rotation ignoriert, C genau einmal angehängt, aktiver Routenzustand und Fullscreen bleiben erhalten. Nur bei Bedarf testgebundene Injektion; keinen zweiten `isNewer`-Guard einführen.
3. **P1 ROUTE-02 Geräteabnahme:** Auf S25 mit realem GPS Haarnadel/parallele Abschnitte, tatsächliches Zurückgehen, Rerouting, längere GPS-Lücke und Abbiegehinweise prüfen. S10-Installationskonflikt separat behandeln; keine Versions-Downgrade-Installation erzwingen.
4. **P1 UX/MVV:** DATE-01 (Samstagstermin), Design-5-Vollbild, Navigationsdesign 1, Anschluss-Check-in und dynamischen Puffer mit echten MVV-Daten verifizieren.

## Grenzen

Keine lokalen Tests oder Gerätezugriffe in diesem Lauf. Keine Secrets/Signing-Änderung. Andere vollständige Main-/Schedule-Chats sind nicht live abrufbar; nur bereits hier bekannter und im Repo persistierter Kontext verwendet. Dieser Abgleich ist eine Nachweis-/Priorisierungsdatei, kein neuer Produktionsfix.
