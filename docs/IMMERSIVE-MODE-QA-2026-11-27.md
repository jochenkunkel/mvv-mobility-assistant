# Immersive-Mode: Abnahmevertrag und Fehlerrisiken (#24)

Stand: 27.11.2026, 14:20 MEZ. **Analyse und Testplanung, keine Implementierung.**

## Ausgangslage
- Nutzer-Screenshot zeigt in „Meine Verbindung“ trotz ausgeblendeter App-Tab-Leiste die **Android-Drei-Tasten-Navigation** unten und teilweise überdeckte Warnhinweise.
- Die ältere Aussage in `docs/CHAT-START.md`, Android-Systemleisten blieben sichtbar, ist ein **historischer Implementierungsstand**. Neuere verbindliche Nutzerentscheidung: #24 verlangt Immersive Mode für das Vollbild-Dashboard.
- #23 betrifft zusätzlich überzeichnete Kartenkacheln und untere Navigationsbuttons. Die neue `clipToBounds()`-Änderung aus `d3de319c` adressiert nur das Karten-Clipping, **nicht** das Systemleistenproblem.

## Kleines, testbares Folgepaket (keine blinde globale Änderung)
1. Aktuellen Besitzer von `fullscreenMode` in `NavigationSessionViewModel` und das Activity-Window-Setup ermitteln. `DASHBOARD` und ggf. `NAVIGATION` getrennt spezifizieren: #24 fordert ausdrücklich das Dashboard; Navigation nicht ohne weitere Prüfung global umstellen.
2. WindowInsetsController(Compat) an den Screen-Lifecycle koppeln: nur `navigationBars()` verstecken, `statusBars()` nach Produktvorgabe sichtbar lassen; `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` verwenden. Auf Verlassen/Dispose zuverlässig wieder anzeigen. Systemdialoge, IME und Activity-Pause berücksichtigen.
3. Inset-Layout unabhängig davon absichern: bei transient eingeblendeter Navigation dürfen Warnung und Aktionen nicht verdeckt werden. `navigationBarsPadding()`/safeDrawing und Scaffold-/Compose-Inset-Verbrauch nur **einmal** anwenden, keine doppelten Abstände.
4. Regressionstests für `NONE → DASHBOARD → NONE`, Rotation, App-Wechsel/Rückkehr, Berechtigungsdialog, Gesten- und Drei-Tasten-Modus; manuelle S25-Screenshots bei normalem und transientem Systemleistenzustand.

## Verbindliche Prüfpunkte
- [ ] Dashboard: Android-Navigationsbuttons standardmäßig verborgen, Statusleiste gemäß Design sichtbar.
- [ ] Swipe zeigt Navigation temporär; Android-Back/Home bleiben nutzbar.
- [ ] Zurück zu normalem Screen: Systemnavigation wieder sichtbar.
- [ ] Keine Überdeckung der unteren Warnung oder Aktionsbuttons, auch nach Rotation.
- [ ] Kartenoverlay #23 und Fahrtstatus #22 nicht regressiert.
- [ ] CI für **exakte** Änderung-SHA und S25-Abnahme dokumentiert.

## Nachweislage dieser Analyse
Repository-HEAD vor dieser Datei: `11086626729cfc78858d967923709198318f3957`. Fünf offene Issues #21–#25, keine in dieser Abfrage sichtbaren PRs (PR-Suche separat nachholen). README, Projektstatus, Roadmap, Entscheidungen, Chat-Handoff, Clipping-QA und letzte Commits geprüft. Für `1108662` lieferte die GitHub-Statusabfrage keine Checks; die commitbezogene Workflow-Abfrage sieht nur PR-Runs und lieferte keine Runs. **Push-CI damit nicht nachgewiesen**, nicht als fehlgeschlagen oder grün interpretieren. Kein Gradle-/Emulator-/S25-Test ausgeführt. Keine APK-Freigabe.

Die Chats „Main“ und „Schedule“ sind nicht vollständig direkt lesbar; neue Anforderungen sind nur aus den hier vorliegenden Nutzerangaben und GitHub-Issues übernommen.
