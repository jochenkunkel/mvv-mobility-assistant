# Mein Weg – Android MVV-Begleiter

Android-App mit Kalenderterminen, aktueller Standortabfrage auf Anfrage, MVV-Tür-zu-Tür-Verbindungen, Linienfilter, Ankunftspuffer und Geh-Dashboard.

## Produktentscheidungen und Designziel

Die begründeten Architektur-, UX- und Funktionsentscheidungen inklusive Umsetzungsstatus, Risiken und nächster Schritte stehen in [docs/DECISIONS.md](docs/DECISIONS.md). Maßstab ist die abgestimmte Drei-Screen-Designvorlage (Start, Verbindungen, Meine Fahrt) mit zusätzlichem Einstellungszugang. Ein erfolgreicher CI-Lauf belegt nicht automatisch visuelle Übereinstimmung. Die Sprachwahl ist derzeit nur vorbereitet; echte Übersetzungen folgen.

## Abgestimmter Cockpit-Umbau (15.11.2026)

Die verbindlichen Anforderungen aus dem Praxistest mit fünf Android-Screenshots, einschließlich automatischer Kalender-/Ziel-/Standortvorbereitung, kompakter Verbindungskarten, Settings und ehrlicher Live-Anzeigen, stehen in **[docs/COCKPIT-V2-PLAN.md](docs/COCKPIT-V2-PLAN.md)**. Dort sind Umsetzungsstand, Reihenfolge und Abnahmekriterien getrennt dokumentiert. Geplant bedeutet nicht implementiert.

Der **[systematische Entwicklungs- und Freigabeplan](docs/IMPLEMENTATION-ROADMAP.md)** definiert sechs Phasen, konkrete Arbeitspakete, Test-Gates und den aktuellen Fortschritt.

## Entwickeln und testen
JDK 17, Android SDK 35 und Gradle 8.13. `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.

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
