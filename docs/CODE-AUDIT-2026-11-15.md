# Mein Weg 2.0 – Quellcode- und Dokumentationsaudit (15.11.2026)

**Basis:** Repository-HEAD nach Phase 3B, Dateibaum mit 74 Einträgen (einschließlich Verzeichnissen); Kotlin-Quellen, Testdateien, Workflows, Shell-/Python-Skripte und Projekt-/Design-Dokumentation wurden durchgesehen. Keine lokale Android-Kompilierung, keine Emulator-Ausführung und keine visuelle Screenshot-Abnahme in diesem Audit. Neue Commits verwenden `[skip ci]` wegen des Build-Budgets.

## Priorität P1 – tatsächliche Fehler / potenzielle Datenfehler

1. **Settings über Verbindungen:** `MainActivity.kt` zeigte die Verbindungskarten bei `selectedTab == CONNECTIONS` selbst dann, wenn `settingsExpanded` wahr war. **Behoben** mit Commit `41fde200`; noch nicht getestet.
2. **Asynchrone MVV-Suche kann veraltete Resultate überschreiben:** `searchConnections` startet `searchAddress` auf einem Thread und setzt `journeys` im Callback ohne Request-ID/Abgleich mit aktuellem Termin, Ziel, Filter und Standort. Während einer Suche können Termin/Filter wechseln und ein alter Callback die neue Anzeige überschreiben. Abhilfe: Request-Generation und Cancel-/Ignore-Strategie; Instrumentierungstest mit vertauschter Antwortreihenfolge.
3. **Haltestellen-Fallback nutzt abweichenden Datenpfad:** `MvvJourneyRepository.searchAddress` verwendet geokodierte Koordinaten, Ankunftszeit und `RoutePreference`. Der separate `search`-Pfad für die Auswahl von `mvvCandidates` verwendet dagegen keine Ankunftszeit und keine Präferenz; im UI wird das Ergebnis ungefiltert übernommen. Auf ein einheitliches Suchmodell konsolidieren; Fallback prüfen, ob bei `searchAddress` überhaupt `AmbiguousDestination` entstehen kann.
4. **Uhrzeit ohne Datum:** `JourneyOption` speichert Abfahrt/Ankunft als `HH:mm`; `selectAppointmentJourneys` und UI-Countdown rekonstruieren Datum aus dem Termin. Verbindungen über Mitternacht/DST und Zeitabweichungen können falsch zugeordnet werden. Epoch-/ISO-Zeitstempel samt Zeitzone aus der MVV-Antwort übernehmen und Grenzfälle testen.
5. **MVV-Antworten mit fehlender Liniennummer:** In `parseTrips` zählt `vehicles` nur Fahrten mit nichtleerem `number`; die Umstiegszahl kann bei unvollständigen MVV-Daten zu niedrig sein. Fahrzeugtyp/Leg separat zählen; fehlende Nummer kenntlich machen.
6. **Standortstatus:** Ein zuletzt bekannter Standort bleibt bei späterem GPS-Ausfall in `location` erhalten; UI kann weiterhin „Standort aktuell“ anzeigen. Zeitstempel/Genauigkeit prüfen und bei Alterung sichtbar als veraltet markieren.
7. **Berechtigungen beim Start:** Kalender und Standort werden automatisch angefragt; auf realem Samsung Galaxy S25 sind Reihenfolge und Ablehnung/Wiederholung sowie One-UI-Verhalten zu testen.

## Priorität P2 – Tests und Gerätequalität

8. **Screenshot-Navigation per Koordinaten:** `.github/workflows/android.yml` tippt feste Pixelpositionen (`540 2250`, `900 2250`). Das ist geräte- und skalierungsabhängig; semantische Tags bzw. Compose-Tests verwenden. Die zusätzliche S25-Viewport-Simulation beweist keine echte One-UI-Kompatibilität.
9. **Alte Shell-Smoke-Tests:** `scripts/emulator-smoke-test.sh` erwartet „Gehgeschwindigkeit“ direkt nach Scrollen auf Start, obwohl der Wert nun im Settings-Panel liegt. `scripts/emulator-fixture-test.sh` sucht einen älteren MVV-Buttontext. Beide Skripte sind derzeit **nicht im Android-CI-Workflow aufgerufen**; vor Wiederverwendung aktualisieren oder nach Prüfung entfernen.
10. **UI-Test Settings:** Der zuletzt eingeführte `settingsOpenFromEveryTabAndCloseCleanly` war in CI fehlgeschlagen; nach Commit `0b97283d` noch nicht grün nachgewiesen. Test-Fix darf nicht einen echten Bedienfehler verdecken.
11. **Screenshot-Suite:** Drei Haupt-Screens und zwei Viewports als Artefakte, aber kein visueller Golden-Master-Vergleich, keine größere Schrift/Tablet-Matrix und keine echte S25-Abnahme.
12. **Updatepfad:** Release-Workflow signiert APK/AAB mit externen Secrets; keine bestätigte erfolgreiche Release-Signierung und kein N→N+1-Upgrade-Test mit Datenerhalt. Release-Keys niemals löschen/neu erzeugen, sobald an Nutzer verteilt.

## Priorität P3 – Altlasten, Struktur und Dokumentation

13. **Demo-Code:** `data/DemoJourneyRepository.kt` ist historisch und wird in den geprüften produktiven Hauptpfaden nicht verwendet. `domain/Journey.kt` / `JourneyRules` haben jedoch eigene Unit-Tests; **nicht blind löschen**. Zunächst Referenzsuche und Abhängigkeiten bestätigen, dann Demo entfernen oder explizit als Testfixture kennzeichnen.
14. **Große MainActivity:** ~788 Zeilen; UI, Persistenz, Lifecycle, Netzwerk-Callbacks und Berechtigungen sind eng gekoppelt. Extraktion in Start-/Verbindungen-/Fahrt-/Settings-Komponenten und ein testbares State-/ViewModel-Modell schrittweise vornehmen, nicht als Big-Bang-Rewrite.
15. **Doppelte Einstellungen:** `RouteFilterControl` ist auf Start und Settings eingebunden; `ArrivalBufferControl` ebenfalls. Gemeinsame State-Quelle ist vorhanden, aber UI-Redundanz und mögliche verwirrende Nutzerführung prüfen.
16. **Unvollständige Spracheinstellung:** Sprachpräferenz wird gespeichert, aber Strings sind überwiegend hartcodiert; keine wirksame Übersetzung. Auswahl deutlich als noch nicht aktiv markieren.
17. **Veraltete Statusabschnitte:** `docs/IMPLEMENTATION-ROADMAP.md` bezeichnet Phase 3 weiterhin als offen und nennt alte CI-Stände; `docs/DECISIONS.md` enthält historische „nächste Schritte“. Historie beibehalten, aber einen aktuellen Statuskopf ergänzen; nicht rückwirkend verfälschen.
18. **Datenmodell/Filter:** `filterJourneys` akzeptiert leere Query als „Alle“, während UI bei Nicht-ANY leerer Query die Suche deaktiviert. Verhalten vereinheitlichen; Eingaben validieren.
19. **Dokumentation vs. reale UI:** Wireframes sind Soll-Zustand, keine Screenshots; dies ist in den Dokumenten bereits korrekt markiert. Keine Designfreigabe aus grüner CI ableiten.

## Sicherer Aufräumplan ohne unnötige Builds

1. **Jetzt:** Nur eindeutig lokalisierte, risikoarme Korrekturen als `[skip ci]` committen (Settings-Sichtbarkeit bereits korrigiert). Kein Löschen von Dateien auf Verdacht.
2. **Nächster Code-Schritt:** Asynchrones Request-Generation-Guarding und Tests für Termin-/Filterwechsel; danach Datum-/Zeitmodell korrigieren.
3. **Testpflege:** Veraltete Shell-Skripte entweder an neue Semantik anpassen oder aus dem aktiven Bestand entfernen, falls keine Referenzen bestehen; Screenshot-Taps durch Tags ersetzen.
4. **Struktur:** Komponenten aus `MainActivity` isolieren und Persistenz-/Fehlerzustände testbar machen.
5. **Wenn Build-Budget wieder da:** **Ein** gezielter vollständiger CI-Lauf für alle `[skip ci]`-Commits, Fehler beheben, echte Screenshot-Artefakte sichten, anschließend realen Galaxy-S25-Test. Bis dahin keine APK-Freigabe.

**Einordnung:** „Verdacht“ und „potenziell“ sind bewusst von nachgewiesenen Fehlern getrennt. Das Audit ist eine statische Quelltextprüfung, kein Laufzeitnachweis.
