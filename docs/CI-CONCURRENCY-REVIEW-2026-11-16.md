# CI-Concurrency: Release-Läufe vor Push-Abbruch schützen

Stand: 16.11.2026, 21:16 MEZ. **Die Workflow-Änderung wurde mit Commit `6b2b55f` auf `main` umgesetzt; eine erfolgreiche Actions-Ausführung ist weiterhin nicht nachgewiesen.**

## Bestätigter Befund

Vor der Korrektur galt in `.github/workflows/android.yml`:

```yaml
concurrency:
  group: single-active-ci
  cancel-in-progress: true
```

Alle Pushes, Pull Requests, manuellen `test_level=release`-Läufe und `v*`-Tags teilen damit dieselbe Concurrency-Gruppe. **Ein neuer Push kann einen laufenden vollständigen Release-Test abbrechen**, auch wenn er auf einer anderen SHA oder einem anderen Branch läuft. Dadurch kann eine Freigabeprüfung ohne technischen Testfehler verloren gehen. Dies ist ein statischer Konfigurationsbefund, kein beobachteter Abbruch.

## Kleines, überprüfbares Arbeitspaket (Issue #8)

1. Concurrency-Gruppen nach Ref und Ereignis trennen, damit Änderungen auf anderen Branches einen Release-Lauf nicht abbrechen.
2. Manuelle `release`- und Tag-Läufe grundsätzlich **nicht automatisch abbrechen**; Push-/PR-Zwischenstände desselben Branches dürfen weiterhin ältere Zwischenläufe ersetzen.
3. Mit zwei absichtlich überlappenden Läufen prüfen: `workflow_dispatch release` auf `main`, danach Push auf Feature-Branch; Release muss weiterlaufen. Zweiter Push auf denselben Feature-Branch darf nur den alten Zwischenlauf abbrechen.
4. Ergebnis anhand tatsächlicher Run-IDs, Status, SHAs und Artefakten protokollieren. Keine APK-Freigabe allein aufgrund der Concurrency-Korrektur.

**Mögliche Konfiguration zur Implementierung und Prüfung:**

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event_name }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'push' && !startsWith(github.ref, 'refs/tags/v') }}
```

Hinweis: Bei zwei manuellen Release-Läufen auf derselben Ref kann GitHub trotz `cancel-in-progress: false` einen zweiten Lauf als pending halten; das ist gewollt. Die Änderung sollte mit einem Workflow-Linter und echten Actions-Läufen geprüft werden.

## Weiterhin offene Nachweise

- Für `42a7fd9` liefert die Commit-Status-Abfrage nur `statuses: []`; der verfügbare Commit-Workflow-Connector erfasst Push-Läufe nicht vollständig. CI-Ergebnis daher **unbekannt**, nicht grün.
- Die neue Concurrency-Regel ist in `main` seit `6b2b55f` implementiert; die Wirkung ist **noch nicht mit überlappenden Actions-Läufen bestätigt**. Wichtig: `cancel-in-progress: false` verhindert das Abbrechen eines *laufenden* manuellen Jobs, aber GitHub kann bei mehreren wartenden Läufen derselben Gruppe einen älteren **pending**-Lauf durch einen neueren ersetzen. Release-Läufe derselben Ref sind daher nicht garantiert alle ausführbar; vor einer Gerätefreigabe stets die konkrete Run-ID prüfen.
- Andere Projekt-Chats sind in diesem Durchlauf nicht vollständig zugänglich. Repository und bekannte Chat-Anforderungen bleiben maßgeblich.
- Nächste Priorität: CI-Schutz mit realen überlappenden Workflow-Run-IDs validieren, dann GPS-/Rerouting-Regressionstests und vollständigen Release-Workflow verifizieren.

## Prüfprotokoll 16.11.2026, 21:16 MEZ

- Repository-Abgleich: `main` endet nach verfügbarer Commit-Suche bei `6b2b55fb1ff971717a948aedc2aca212f2b8e2e0`; `.github/workflows/android.yml` enthält die getrennte Concurrency-Gruppe tatsächlich.
- Commitbezogene Actions-Abfrage für `6b2b55f`: `workflow_runs: []`. Diese Schnittstelle liefert nur PR-bezogene Läufe; **Push-Buildstatus unbekannt**.
- Regressionstest als manueller Nachweis: Release-Dispatch `test_level=release` starten und Run-ID notieren; parallelen Branch-Push starten; Release-Run muss `success` oder weiterhin `in_progress` bleiben, nicht `cancelled`. Zwei Branch-Pushes nacheinander müssen nur ihre eigene Push-Gruppe beeinflussen. Abschließend zwei Dispatches auf derselben Ref prüfen und pending-Ersetzung ausdrücklich protokollieren.
- Freigabe-Gate: Kein grüner Android-Build, keine Emulator-/Live-Probes und kein APK-Artefakt für diese SHA nachgewiesen; keine Installationsfreigabe.
