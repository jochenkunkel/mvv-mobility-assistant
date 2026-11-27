# GPS-Regression: aktive ÖPNV-Etappe statt erste ÖPNV-Etappe

Stand: 2026-11-28. Arbeitsauftrag zu [#22](https://github.com/jochenkunkel/mvv-mobility-assistant/issues/22) und [PR #27](https://github.com/jochenkunkel/mvv-mobility-assistant/pull/27). **Spezifikation, kein Testnachweis.**

## Risiko
Die Auswahl der ersten ÖPNV-Etappe als Plausibilitätsanker ist bei mehreren Fahrzeug-Etappen nicht gleichbedeutend mit der *aktuell bestätigten* Fahrt. Eine pauschale GPS-Sprungfilterung über die ganze Reise kann Positionsupdates auf dem Fußweg und nach dem Umstieg falsch verwerfen. Ein grüner Status darf keine sichere Anschlusslage suggerieren.

## Minimaler Fix
1. GPS-Filter erhält die ID/den Index der **aktiven, bestätigten** ÖPNV-Etappe aus dem Navigationszustand; keine implizite Suche nach der ersten Fahrzeug-Etappe.
2. Beim Wechsel Bus A → Fußweg wird die Fahrzeugfilterung beendet, ohne die akzeptierte Positionshistorie zu löschen. Beim Check-in Bus B wird mit dessen Strecken-/Haltestellenkontext neu gestartet.
3. Ohne eindeutig bestätigte aktive Fahrzeug-Etappe nur die allgemeinen Genauigkeits- und Zeitordnungsregeln anwenden; keine geschätzte Live-Position als Messung deklarieren.
4. Verspätete Callbacks von Bus A dürfen die aktive Etappe B nicht überschreiben. Status „Position geschätzt“ ist von „GPS LIVE“ zu trennen.

## Deterministische Regressionen
- **Bus A / Lücke:** Guter Fix → kurze Lücke → unplausibler Teleport: letzter akzeptierter Fix bleibt, Fahrzeugstatus nur mit Kennzeichnung geschätzt.
- **Umstieg:** Bus A → Fußweg → Bus B: während Fußweg keine Bus-A-Sprungschwelle; nach Check-in nur Bus-B-Kontext.
- **Verspätete Events:** Ein GPS-/Routen-Callback von Bus A nach Bus-B-Aktivierung darf weder Route noch Status zurücksetzen.
- **Ohne Check-in:** Die bloße Existenz einer Busetappe darf den Fahrzeugfilter nicht aktivieren.
- **Zeit/Genauigkeit:** Doppelte und rückwärts laufende Zeitstempel sowie grobe Fixes dürfen keinen Fortschritt rückwärts erzeugen.
- **Anschlussrisiko:** GPS-Schätzung hält ggf. die Fahrtanzeige stabil, überschreibt aber keine Warnung vor einem tatsächlich unsicheren Anschluss.

## Abnahme-Gates
Zuerst JVM-Tests mit synthetischen Koordinaten und monotoner Uhr, danach API-35-Instrumentierung und vollständige CI für den konkreten PR-HEAD; anschließend S25-Test mit echter Mehr-Etappen-Fahrt. Keine Merge-/APK-Freigabe allein aufgrund dieses Dokuments. PR #26 separat auf Konflikte mit Statusdateien prüfen.
