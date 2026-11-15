# Mein Weg 2.0 – UI-Qualitätssicherung

## Automatisch in GitHub Actions
- Android-Kompilierung, Lint und JVM-Tests
- MVV-Live-Probes und Tür-zu-Tür-Abfrage
- Emulator (API 35): Fixture für Kalender, GPS und MVV, ohne externe Abhängigkeiten
- Compose-Interaktionen: Terminauswahl auf-/zuklappen und weitere zehn Termine laden
- Compose-Interaktionen: Ankunftspuffer-Slider von 10 auf 60 und 5 Minuten
- Compose-Interaktionen: Linien-Filterchips, Eingabefeld und erweiterte Einstellungen
- Compose-Interaktionen: Verbindung wählen, Geh-Dashboard öffnen/schließen
- Screenshots von Start, Verbindungsauswahl und Dashboard als CI-Artefakte zur Sichtkontrolle

## Vor der Installation zusätzlich erforderlich
Die erzeugten Screenshots müssen wirklich angesehen werden. Ein grüner Funktionstest bedeutet nicht, dass das Layout dem Mockup entspricht oder schön aussieht. Die Bilder aus dem Emulator sind **keine Golden-Master-Vergleichstests**.

## Nächste Qualitätsstufe
1. Separat testbare Compose-Screens mit festen Daten und echten Screenshot-Goldens (Referenzbildern) pro Bildschirm
2. Mehrere Bildschirmgrößen und Schriftgrößen (insbesondere große Schrift und Querformat)
3. Hell-/Dunkelmodus, Kontrast, Mindest-Touchflächen und TalkBack
4. Lange Adressen, fehlende Berechtigungen, leere Termine, kein GPS, keine MVV-Verbindungen, langsames Netz
5. Regressionstests für Tageswechsel und tatsächliche Ankunftszeiten

Die Android-Dokumentation empfiehlt Screenshot-Golden-Tests für visuelle Änderungen. Die offizielle AGP-Test-Suites-Integration benötigt derzeit AGP 9.5-alpha; das bestehende Projekt soll nicht ohne separaten Kompatibilitätstest umgestellt werden.
