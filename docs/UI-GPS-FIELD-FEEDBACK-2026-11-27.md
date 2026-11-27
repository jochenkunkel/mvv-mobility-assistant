# UI/GPS Feldfeedback – Integrationsplan 27.11.2026, 11:16 MEZ

## Quellen und Nachweisgrenze
Abgleich mit README, AGENTS, CHAT-START, PROJECT_STATUS, DECISIONS, IMPLEMENTATION-ROADMAP, ROUTE-02/GPS-03-Gate, offenen GitHub-Issues/PRs, Commitliste und GitHub Actions. Neuere Anforderungen aus den im laufenden Projektchat sichtbaren Screenshots wurden als Issues #21–#24 erfasst. Andere vollständige Chats (Main/Schedule) sind nicht direkt live lesbar; keine weiteren Inhalte unterstellt.

**Repo-HEAD vor dieser Datei:** 2ce032bd1bb8706649f95ce6973c9e78732d2d99 (27.11. 10:16 MEZ). **Offen:** #21–#24, 0 PRs. Letzter GitHub-Actions-Lauf für diesen HEAD: #45972375563 completed/success; der unmittelbar vorherige Lauf #45971966056 war cancelled. Der erfolgreiche Lauf ist ein CI-Nachweis für den dokumentierten HEAD, nicht für die vier neu gemeldeten UI-/GPS-Fehler und keine Geräteabnahme.

## Priorisierte kleine Arbeitspakete

1. **P0 #23 Kartenoverlay/Insets:** Karten-View/Compose-Hierarchie und Clipping prüfen; erst reproduzierbaren Screenshot-/UI-Test für Header, Kartencontainer und Bottom-Actions schreiben. Fehlerbild: rotierte/ausbrechende Karte verdeckt Header. Separate System-Inset-Problematik mit #24 abstimmen. **Keine blinde zIndex-Korrektur.**
2. **P0 #21 Dashboard visuell:** Verbindlich ist die **zuletzt vom Nutzer freigegebene neue Visualisierung** (violetter Countdown, drei weiße Icon-Karten, orange Warnleiste, zwei Check-in-Aktionen), **nicht** das ältere SVG `docs/design/dashboard-design-5-kompakt.svg`. Bild wurde im Chat als `dashboard-approved-2026-11-27.svg` (PNG eingebettet) vorbereitet, aber ein Repository-Upload ist **nicht bestätigt**. Vor Umsetzung exakte Datei ins Repo übernehmen und visuelle S25-Abnahme mit Vorher/Nachher.
3. **P1 #22 GPS im Bus:** Fahrtphase und GPS-Freshness trennen; für kurze Lücken plausibel interpolierte Busfahrt mit Status `Fahrt aktiv · Position geschätzt`, **nicht** fälschlich `GPS LIVE`. Bestehende monotone Fix-Reihenfolge/ROUTE-02 respektieren; zuerst deterministische Lücken-/Jittertests, dann Code. Längere Ausfälle und echte Risiken weiter warnen.
4. **P1 #24 Immersive Systemnavigation:** Nutzer will auf dem Fahrtdashboard auch die Samsung-3-Tasten-Systemleiste ausblenden; bisheriges CHAT-START dokumentiert bewusst sichtbare Android-Systemleisten und ist daher durch diese **neue Produktentscheidung für Dashboard** überholt. Statusleiste oben weiterhin sichtbar, Android-Systemnavigation durch Wischen temporär erreichbar; WindowInsetsControllerCompat und Lifecycle prüfen. Insets müssen auch bei temporärer Einblendung stimmen.
5. **P1 GPS-03 und ROUTE-02:** Recreate-/Callback-Regression und reale S25-Fußwegabnahme aus `ROUTE-02-GPS-03-INTEGRATION-GATE-2026-11-27.md` bleiben offen.

## Akzeptanz und nächste Entwicklung
Nächstes kleines Codepaket: #23 durch UI-Reproduktion eingrenzen und Map-Container-Clipping/Layer-Reihenfolge gezielt reparieren; Tests für Rotation, Kartengesten und Insets. Danach #21 mit **richtigem** Bild abgleichen. #22 getrennt von Fußgänger-Routing implementieren. #24 braucht Android-Systemleisten-Regression auf S25 mit 3-Tasten- und Gestenmodus.

**Freigabe:** Kein APK-/Installations-Go aufgrund des grünen CI-Laufs allein. Keine Signing-/Secret-Änderungen. Screenshots enthalten ggf. Ortsinformationen: keine privaten GPS-Traces ins Repo committen.
