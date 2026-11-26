# Geräteabnahme nach #15–#18 – reproduzierbares Prüfprotokoll

Stand: 26.11.2026. **Status: NICHT auf einem physischen Gerät ausgeführt.** Diese Vorlage ist kein Freigabebeleg.

## Belegte technische Basis (vor dieser Dokumentation)
- Feature-Commit: [a44c9db](https://github.com/jochenkunkel/mvv-mobility-assistant/commit/a44c9db62e403a15e825c707afa8b64eb58821d3); #15–#18 laut Repo geschlossen.
- [Release-CI 45869130850](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45869130850): sechs Jobs erfolgreich (inkl. Emulator).
- [Signed Release 45869989480](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45869989480): gate und signed-release erfolgreich; laut PROJECT_STATUS Version 0.2.4 / versionCode 1000004.
- [Push-CI 45870658112](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45870658112): success auf Dokumentations-HEAD bce4a5d.
- Diese CI-Nachweise **ersetzen keine echte GPS-/MVV-/OSM-Abnahme**.

## Prüfmatrix: S25 primär, S10 ergänzend
Für jeden Fall Datum, Gerät, Android/One UI, Commit/App-Version, Berechtigungen, anonymisierte Soll-/Ist-Beobachtung, Ergebnis (PASS/FAIL/BLOCKED) und ggf. Issue-Link erfassen. Keine persönlichen Termine, Standortkoordinaten, GPX-Tracks oder API-Antworten ins öffentliche Repository übernehmen.

| ID | Schritte | Erwartetes Ergebnis | Ergebnis |
| --- | --- | --- | --- |
| G01 | Frische Installation, Kalender- und Standortfreigabe getrennt testen | Kein Absturz; verständlicher Berechtigungs-Fallback | NICHT GETESTET |
| G02 | Ziel und MVV-Verbindung suchen, „Meine Verbindung“ öffnen | Design 5 Vollbild ohne App-Tab-Leiste, Android-Statusleiste sichtbar | NICHT GETESTET |
| G03 | Dashboard mit aktivem GPS von Portrait nach Landscape und zurück drehen | Auswahl, Countdown, Fußweg und Puffer bleiben konsistent | NICHT GETESTET |
| G04 | „Navigation zum Einstieg“ starten; GPS-Routenlinie, Position und Abbiegehinweis auf echtem Fußweg prüfen | Design 1: echte Manöver statt Luftlinienhinweise; Strecke reduziert sich plausibel | NICHT GETESTET |
| G05 | Karte verschieben, zoomen, rezentrieren; zurück zum Dashboard | Kartenbedienung korrekt; Fahrt bleibt ausgewählt | NICHT GETESTET |
| G06 | GPS temporär deaktivieren / schlechte Ortung simulieren und wiederherstellen | Veraltete Position gekennzeichnet, keine fiktiven Sprünge oder spurvernichtende Resets | NICHT GETESTET |
| G07 | Haltestelle ohne eindeutigen Steig / fehlende MVV-Koordinaten | Warnung und Haltestellennamen-Fallback sichtbar, keine vorgetäuschte Präzision | NICHT GETESTET |
| G08 | Bekannte Gehzeit + Abfahrt, danach fehlende Eingangsgröße | Puffer korrekt berechnet bzw. ausdrücklich „unbekannt“, keine unbelegten Live-Verspätungen | NICHT GETESTET |
| G09 | Tatsächlichen Umstieg erreichen, manuell Check-in drücken, zweimal tippen | Einmaliger Check-in nur für diesen Umstieg | NICHT GETESTET |
| G10 | Anschluss verpassen, „Neue Suche“ auslösen | Aktuelles Ziel erhalten; neue Route erst nach Auswahl aktiv | NICHT GETESTET |
| G11 | „Frühere Verbindungen“ mehrfach aufrufen | Frühere Abfahrten chronologisch, dedupliziert, vergangene gekennzeichnet | NICHT GETESTET |
| G12 | Fahrt zurück/verlassen, erneut öffnen; Navigation explizit beenden | Zurück löscht nicht ungewollt; Beenden löscht gezielt | NICHT GETESTET |
| G13 | Display 360dp / 393–412dp, Schrift 1,3× und 2×, Querformat | Kein Clipping, alle Warnungen und primären Aktionen erreichbar | NICHT GETESTET |
| G14 | Signiertes Update von installierter Vorversion auf 0.2.4 | Signatur gleich, versionCode höher, lokale Einstellungen bleiben erhalten | NICHT GETESTET |

## Entscheidungsgate
Bei einem FAIL reproduzierbare Schritte und anonymisierte Belege als GitHub-Issue erfassen; bei BLOCKED die fehlende Berechtigung/Hardware/API präzise dokumentieren. Eine APK darf erst nach den jeweils einschlägigen technischen und realen Abnahmen zur Installation empfohlen werden. Store-Freigabe benötigt zusätzlich die Kriterien aus [STORE-READINESS.md](STORE-READINESS.md).

## Nächster Schritt
S25-Testlauf G01–G12 auf realer Strecke durchführen; S10 ergänzend und G13/G14 separat. Bis dahin bleiben die vier Feature-Issues zwar code-/CI-seitig geschlossen, die **Geräteabnahme aber ausdrücklich offen**.
