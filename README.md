# Mein Weg – Android MVV-Begleiter

Android-App mit Kalenderterminen, automatischer Standortermittlung und Vordergrund-Aktualisierung, MVV-Tür-zu-Tür-Verbindungen, Linienfilter, Ankunftspuffer und Geh-Dashboard.

## Veröffentlichungs- und Sicherheitsstatus

Das Repository ist derzeit **privat**. Die vollständige Sicherheitsprüfung inklusive Git-Historie steht in **[docs/PUBLIC_RELEASE_READINESS.md](docs/PUBLIC_RELEASE_READINESS.md)**. Eine Umstellung auf `public` erfolgt **nur mit ausdrücklicher Freigabe des Eigentümers**.

## Lizenz

Dieses Projekt steht unter der **Apache License 2.0** ([LICENSE](LICENSE), [NOTICE](NOTICE)); Entscheidung des Eigentümers vom 26.11.2026. Drittanbieter-Lizenzen und Datenquellen sind in `NOTICE` und in [docs/PRIVACY-AND-PROVIDER-POLICY.md](docs/PRIVACY-AND-PROVIDER-POLICY.md) dokumentiert.

Sicherheitshinweise (auch für die Geheimnisvergabe) siehe [.github/SECURITY.md](.github/SECURITY.md); Regeln für Beiträge in [CONTRIBUTING.md](CONTRIBUTING.md) und [AGENTS.md](AGENTS.md).

## Neuer Chat? Hier starten

**[docs/CHAT-START.md](docs/CHAT-START.md)** enthält den vollständigen
Einstieg für neue Chats/Agenten: Auftrag, Pflichtlektüre, Architektur,
letzte Arbeiten, offene Issues, Prioritäten, Test- und Übergaberegeln
sowie einen direkt kopierbaren Start-Prompt.

## Zentraler Projektstatus (für alle Chats und Agenten)

**[docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)** ist die gemeinsame
Übergabedatei: aktuelle Commits, offene Issues, geprüfte/nicht geprüfte
Änderungen und nächste Schritte. **Vor jeder Entwicklungsarbeit zuerst
diese Datei, den aktuellen `main`-Branch und die GitHub-Actions-Läufe
lesen.** Nach relevanten Änderungen den Status aktualisieren.
Andere Chatverläufe werden nicht automatisch synchronisiert.

## Aktueller Code-Review (16.11.2026)

**[Quellcode-Audit, dokumentierte Inkonsistenzen und priorisierter Reparaturplan](docs/CODE-REVIEW-2026-11-16.md)**. Der Bericht trennt Codebefunde von Risiken und ungeprüften Geräte-/CI-Nachweisen. Insbesondere die Fußweg-Navigation ist **nicht für zuverlässige Abbiegeführung freigegeben**. Ältere Status- und Auditdateien bleiben als datierte historische Dokumente erhalten.

## Produktentscheidungen und Designziel

Die begründeten Architektur-, UX- und Funktionsentscheidungen inklusive Umsetzungsstatus, Risiken und nächster Schritte stehen in [docs/DECISIONS.md](docs/DECISIONS.md). Maßstab ist die abgestimmte Drei-Screen-Designvorlage (Start, Verbindungen, Meine Fahrt) mit zusätzlichem Einstellungszugang. Ein erfolgreicher CI-Lauf belegt nicht automatisch visuelle Übereinstimmung. Die Sprachwahl ist derzeit nur vorbereitet; echte Übersetzungen folgen.

## Abgestimmter Cockpit-Umbau (15.11.2026)

Die verbindlichen Anforderungen aus dem Praxistest mit fünf Android-Screenshots, einschließlich automatischer Kalender-/Ziel-/Standortvorbereitung, kompakter Verbindungskarten, Settings und ehrlicher Live-Anzeigen, stehen in **[docs/COCKPIT-V2-PLAN.md](docs/COCKPIT-V2-PLAN.md)**. Dort sind Umsetzungsstand, Reihenfolge und Abnahmekriterien getrennt dokumentiert. Geplant bedeutet nicht implementiert.

Der **[systematische Entwicklungs- und Freigabeplan](docs/IMPLEMENTATION-ROADMAP.md)** definiert sechs Phasen, konkrete Arbeitspakete, Test-Gates und den aktuellen Fortschritt.

## Aktueller Projektstand und drei Wireframes

**[Projektstatus vom 15.11.2026: Was umgesetzt ist, was noch geprüft werden muss und die nächsten Phasen](docs/PROJECT-STATUS-2026-11-15.md)**. Enthält drei schematische Bildschirmansichten für Start, Verbindungen und Meine Fahrt. Die Skizzen sind keine echten Emulator-Screenshots. Die [Roadmap](docs/IMPLEMENTATION-ROADMAP.md) enthält das aktualisierte Fortschrittsprotokoll.

## App-Store-Reife und Gerätequalität

Der verbindliche [Geräte-, Installations-, Update- und Store-Freigabeplan](docs/STORE-READINESS.md) legt Prüfungen für kleine/große Smartphones, Tablets, große Schrift, Erstinstallation, Deinstallation, signierte Updates und spätere Google-Play-Veröffentlichung fest. **Die Kriterien sind noch nicht vollständig nachgewiesen; keine Store-Freigabe behauptet.**

## Entwickeln und testen
JDK 17, Android SDK 35 und Gradle 8.13. Lokal:

```bash
python3 scripts/test-ci-concurrency.py
python3 scripts/test-emulator-navigation.py
bash scripts/preflight.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Instrumentierte Tests benötigen einen API-35-Emulator bzw. ein Gerät: `gradle :app:connectedDebugAndroidTest`. Ein Commit oder ein gestarteter Build ist kein Testnachweis; dokumentiere tatsächlich ausgeführte Befehle und Ergebnisse.

GitHub Actions führt MVV-Live-Probes, Android-Tests und Emulator-UI-Tests aus. Debug-APKs sind **nur zum Testen**; ihre Signatur kann zwischen GitHub-Runnern wechseln. Eine neue Debug-APK kann daher nicht garantiert über eine bereits installierte Debug-APK installiert werden.

## Updates ohne Neuinstallation
Android erlaubt ein Update nur bei identischer `applicationId`, kompatibler Signatur und höherem `versionCode`. Die App-ID bleibt `de.mvv.mobility`.

Für dauerhafte Updates **einmalig** einen sicheren Release-Keystore erstellen und extern sichern. In den GitHub-Actions-Secrets hinterlegen:

- `MEIN_WEG_KEYSTORE_BASE64`: Base64 des Keystore-Dateiinhalts
- `MEIN_WEG_KEYSTORE_PASSWORD`: Keystore-Passwort
- `MEIN_WEG_KEY_ALIAS`: Schlüsselalias
- `MEIN_WEG_KEY_PASSWORD`: Schlüsselpasswort

Danach den Workflow **Signed Android Release** manuell starten. Er baut eine signierte APK und AAB, prüft die APK-Signatur und setzt eine monoton steigende Versionsnummer aus der Workflow-Run-Nummer. **Keystore und Passwörter niemals ins Repository committen.** Den einmaligen Wechsel von einer anders signierten Debug-APK auf die Release-APK kann Android nicht als In-Place-Update durchführen; dafür muss die alte Testinstallation deinstalliert werden. Danach sind Updates mit demselben Release-Schlüssel möglich.

Es gibt noch **keine automatische In-App-Updatefunktion**. Die Release-APK kann nach dem Download manuell als Update installiert werden. Eine Play-Store-Veröffentlichung erfordert zusätzliche Store-Metadaten, Richtlinienprüfung und gegebenenfalls Play App Signing.

## Design und Qualität
Siehe `design/MEIN_WEG_2_SKIZZE.md` und `design/UI_TESTPLAN.md`. Die neue Launcher-Grafik ist ein vereinfachtes Weg-Symbol. Medizinische Bilddaten werden nicht in der App gespeichert.
