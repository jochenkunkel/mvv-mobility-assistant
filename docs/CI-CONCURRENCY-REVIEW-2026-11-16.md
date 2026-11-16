# CI-Concurrency: Release-Läufe vor Push-Abbruch schützen

Stand: 16.11.2026, 19:20 MEZ. **Code-Review und reproduzierbarer Änderungsvorschlag; noch keine Workflow-Änderung und kein bestandener Test.**

## Bestätigter Befund

In `.github/workflows/android.yml` gilt derzeit:

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
- Die neue Concurrency-Regel ist ein **Vorschlag**, nicht implementiert. Vor Änderung prüfen, ob die globale Serialisierung bewusst aus Ressourcengründen eingerichtet wurde.
- Andere Projekt-Chats sind in diesem Durchlauf nicht vollständig zugänglich. Repository und bekannte Chat-Anforderungen bleiben maßgeblich.
- Nächste Priorität: diesen CI-Schutz nach Klärung der Serialisierungsabsicht umsetzen, dann GPS-/Rerouting-Regressionstests und den vollständigen Release-Workflow verifizieren.
