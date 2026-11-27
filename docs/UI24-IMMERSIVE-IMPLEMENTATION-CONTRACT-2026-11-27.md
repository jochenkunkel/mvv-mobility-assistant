# UI-24 – Immersive Dashboard: Implementierungs- und Regressionstestvertrag

Stand: 27.11.2026, 15:20 MEZ. Quelle: Issue #24, README, CHAT-START, PROJECT_STATUS, DECISIONS und aktueller MainActivity-Einstieg. **Spezifikation, keine implementierte Reparatur.**

## Beobachtung und Soll

Auf Samsung S25 bleibt im aktiven Vollbild-Fahrtdashboard die Android-Dreifachnavigation sichtbar und überlagert die letzte Warnmeldung. Das Entfernen der *App-eigenen* Tab-Leiste ist davon unabhängig. Die Statusleiste oben soll gemäß freigegebenem Entwurf sichtbar bleiben; nur die Android-Navigationsleiste wird während DASHBOARD verborgen. Eine Wischgeste darf sie temporär wieder anzeigen.

## Kleines Implementierungspaket (Issue #24)

1. Vor Änderung den tatsächlichen Lifecycle-Pfad von `NavigationSessionViewModel.fullscreenMode` über `MainActivity` und `WindowInsets` verfolgen. `DASHBOARD` ist Vollbild, `NAVIGATION` nur nach separater UX-Prüfung; `NONE` zeigt normale Systemnavigation.
2. Einen **einzigen** lifecycle-gebundenen Effekt am Activity-Window einführen: bei `DASHBOARD` `WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.navigationBars())`, `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; bei Verlassen `show(navigationBars())`. Statusleiste **nicht** verstecken. Vorhandene Insets-/Window-Flags prüfen, statt sie parallel zu überschreiben.
3. Der Compose-Inhalt muss bei temporärer Systemleisten-Einblendung lesbar bleiben. Kein Negativ-Padding, kein überlagerter CTA; bestehendes `statusBarsPadding()` in `MainActivity` berücksichtigen. Navigation nach Activity-Recreation und Rückkehr aus Hintergrund wiederherstellen.
4. App-Back und Android-Back dürfen weiter funktionieren; die Wiederherstellung muss beim Wechsel zu `NONE`, Activity-Ende und externer UI erfolgen.

## Deterministische Prüfmatrix

| Fall | Ablauf | Erwartung |
| --- | --- | --- |
| UI24-01 | Normaler Start, `NONE` | Android-Systemnavigation sichtbar |
| UI24-02 | Fahrt starten, `DASHBOARD` | Systemnavigation verborgen, Statusleiste sichtbar |
| UI24-03 | Im Dashboard von unten wischen | Systemnavigation temporär sichtbar, Warnungen/Buttons nicht abgeschnitten |
| UI24-04 | Dashboard verlassen | Systemnavigation wieder dauerhaft sichtbar |
| UI24-05 | Portrait → Landscape → Portrait | Kein falscher Leistenstatus; Konfiguration/Route erhalten |
| UI24-06 | App in Hintergrund und zurück | Systemleisten entsprechen aktueller Fahrtphase |
| UI24-07 | Berechtigungsdialog / externe Activity | Kein dauerhaft verstecktes System-UI außerhalb Dashboard |
| UI24-08 | Samsung 3-Tasten und Gestenmodus, große Schrift | Keine überlagerten Bedienelemente |

**Testschichten:** Ein reiner Zustands-/Policy-Test für `NONE` vs. `DASHBOARD` und ein instrumentierter Activity-/WindowInsets-Test; tatsächliche Sichtbarkeit und Swipe-Verhalten müssen zusätzlich am S25 durch Vorher-/Nachher-Screenshots geprüft werden. Keine Mock-Inset-Abfrage als alleinigen Gerätenachweis akzeptieren.

## Abhängigkeiten und Grenzen

- #21: Das zuletzt freigegebene Dashboard-Bild ist maßgeblich; die alte SVG ist keine bestätigte Referenz.
- #23: Karten-Clipping und sichere untere Buttons getrennt verifizieren. Immersive Mode ersetzt **keine** Clipping-/Insets-Reparatur.
- #22: GPS-Fahrtstatus ist eine andere Zustandsmaschine und bleibt unabhängig.
- #25: Neue Fahrtauswahl darf die Vollbild-Systemleisten nicht unabsichtlich übernehmen.
- Letzter bekannter HEAD vor diesem Dokument: `8f0184a`. Die commitbezogene GitHub-Workflow-Abfrage liefert nur PR-Runs; für diesen HEAD keine zugeordneten Läufe, kombinierter Commit-Status leer. **Daraus folgt kein grüner oder roter Push-Build.**
- Main/Schedule und weitere vollständige Chatverläufe sind hier nicht direkt auslesbar; keine unbekannten Anforderungen behauptet.

## Freigabekriterium

Erst Codeänderung + automatisierte Regression + grüner CI-Nachweis für den konkreten SHA + reale S25-Sichtprüfung erlauben eine Abschlussbewertung von #24. Bis dahin Ticket offen und keine neue APK-Freigabe.
