# Mein Weg 2.0 – Projektstand, Zielbild und nächste Schritte
**Stand:** 15.11.2026. **Quelle:** aktueller Repository-Code, [Entscheidungen](DECISIONS.md), [UX-Anforderungen](COCKPIT-V2-PLAN.md), [Roadmap](IMPLEMENTATION-ROADMAP.md) und Projektabsprachen. Der Stand unterscheidet ausdrücklich Implementierung, CI und Geräte-/Sichtprüfung.

## Unser Ziel
Eine Android-App für München, die den nächsten Kalendertermin als Ausgangspunkt nimmt, das Ziel vorbereitet, die aktuelle Position während der sichtbaren App-Nutzung bereithält, passende MVV-Verbindungen findet und eine Fahrt als kompaktes, ehrliches Cockpit begleitet. **So wenig Klicks wie möglich; keine behaupteten Live-Werte ohne echte Daten.**

## Drei schematische Ansichten (keine Emulator-Screenshots)
Die folgenden drei Wireframes zeigen die **beabsichtigte Informationshierarchie**, nicht den bereits abgenommenen Ist-Zustand. Die Werte sind Platzhalter.

### 1. Start – automatisch vorbereitet
```text
┌────────────────────────────────┐
│ MEIN WEG                  ⚙    │
│ Nächster Termin                │
│ ┌────────────────────────────┐ │
│ │ Termin · Datum · Uhrzeit   │ │
│ │ Ziel aus Kalender         │ │
│ └────────────────────────────┘ │
│ Start  ✓ Standort aktuell      │
│ Ziel   ✓ geprüft / Auswahl     │
│ Wunsch · Puffer · Verkehrsm.   │
│                                │
│     [ Verbindung finden ]      │
│ Start   Verbindungen   Fahrt   │
└────────────────────────────────┘
```

### 2. Verbindungen – Alternativen vergleichen
```text
┌────────────────────────────────┐
│ VERBINDUNGEN                   │
│ ┌────────────────────────────┐ │
│ │ Abfahrt  →  Ankunft       │ │
│ │ [Linie] · Umstieg · Fußweg │ │
│ │ Losgehen: Uhrzeit         │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Alternative · Zeiten      │ │
│ └────────────────────────────┘ │
│ Start   Verbindungen   Fahrt   │
└────────────────────────────────┘
```

### 3. Meine Fahrt – Status statt Fließtext
```text
┌────────────────────────────────┐
│ MEINE FAHRT                    │
│ ┌────────────┐ ┌─────────────┐ │
│ │ Fußweg     │ │ Abfahrt     │ │
│ │ m / Min    │ │ Zeit/Linie  │ │
│ └────────────┘ └─────────────┘ │
│ ┌────────────┐ ┌─────────────┐ │
│ │ Losgehen   │ │ Ankunft     │ │
│ │ Countdown  │ │ Zeit        │ │
│ └────────────┘ └─────────────┘ │
│ Status: später / bald / jetzt  │
│ [ Navigation / nächster Schritt]│
│ Start   Verbindungen   Fahrt   │
└────────────────────────────────┘
```

## Was wir bereits umgesetzt haben
| Thema | Im Code | Automatischer Nachweis | Noch offen |
|---|---|---|---|
| Drei Tabs und progressive Startmodule | Vorhanden | Frühere CI-/Emulatorläufe | Visueller Abgleich mit Referenz |
| Kalenderfreigabe beim Start, ganze Terminkarte antippbar | `6b4471e7` | Spätere CI grün | Ablehnung/Terminwechsel auf Gerät prüfen |
| Kalenderziel sichtbar vorbefüllen und automatisch auflösen | `b0ef8030` | CI-Lauf `43739423593` erfolgreich | Mehrdeutigkeit, Offline und Geocoder-Grenzen prüfen |
| Automatischer GPS-Start statt Pflichtbutton | `f78a0266` | CI-Lauf `43739794657` erfolgreich | Berechtigungsdialoge und GPS auf Gerät prüfen |
| Standort bei aktiver App etwa alle 5 s abonnieren, bei Hintergrund stoppen | `7afdfab3` | **5/5 Jobs grün**, Lauf `43739947286` | Empfang, Akku, Lifecycle, echte Aktualisierungsrate prüfen |
| Settings aus allen Tabs erreichbar; Berechtigungsstatus sichtbar | `2a27794b` | CI-Lauf `43744222066` zum Zeitpunkt dieser Dokumentation **noch in Bearbeitung** | Persistenz-/Navigationstests, UI-Sichtprüfung |
| Zuhause, Gehgeschwindigkeit, Ankunftspuffer | Schon vorhanden | Allgemeine CI vorhanden | Neustart-/Grenzwerttests, konsistente Wirkung |
| MVV-Verbindungssuche, Linienfilter, erstes Dashboard | Schon vorhanden | MVV-/Tür-zu-Tür-Probes und Emulator vorhanden | Kompakte Karten, Live-/Schätz-Trennung |

**Wichtig:** Die Android-Standortabonnements fordern Updates mit 5.000 ms Mindestintervall und 3 m Mindestdistanz an. Android garantiert keine Position alle fünf Sekunden. Keine Hintergrundortung, keine Speicherung eines Standortverlaufs. Das aktive GPS ist noch keine meter- oder abbiegegenaue Fußweg-Navigation.

## Nächste Phasen / Reihenfolge
1. **Phase 2 fertigstellen:** Einstellungsbereich CI-grün bekommen, Zuhause und Parameter nach Neustart prüfen; wirksame Linien-/Verkehrsmittelpräferenzen sauber speichern. Die Sprachwahl ist **noch keine Übersetzung**.
2. **Phase 1 abschließen:** Kalender-/Standortablehnung, Terminwechsel, Zielmehrdeutigkeit, fehlendes GPS, einheitliche primäre Suche und UI-Regressionen gezielt testen. Phase 1 ist **weit fortgeschritten, nicht vollständig abgenommen**.
3. **Phase 3:** Verbindungen als kleine, vergleichbare Karten mit Abfahrt, Ankunft, Linien, Umstiegen, Gehzeit und sinnvollen Leerzuständen.
4. **Phase 4:** Meine Fahrt als Kachel-Dashboard; zeitabhängige Zustände und Countdown; geplante/geschätzte/aktuelle Werte unterscheiden.
5. **Phase 5:** MVV-Aktualisierungen im sichtbaren Betrieb, Positionsbezug erst mit echter Weggeometrie; kein ungefragtes Hintergrundtracking.
6. **Phase 6:** CI, Emulatorbilder tatsächlich ansehen, Tests auf echtem Android-Gerät, Signierung/Updatepfad, erst dann APK-Empfehlung.

## Nachtrag Phase 2B (15.11.2026)
- Commit `74bc95f4`: Linienfilter-Modus und Linienkennung werden nun in Android Preferences gespeichert und beim App-Neustart wiederhergestellt. Die Einstellungen und die Startansicht verwenden dieselben Werte; Filteränderungen leeren veraltete Verbindungsergebnisse.
- Der Verkehrsmittel-Filter ist **nicht** implementiert und wird ausdrücklich nicht als wirksame Einstellung ausgegeben. Die vorhandene Linienfilterfunktion ist der aktuell unterstützte Präferenzumfang.
- CI und Gerätetest für `74bc95f4` sind zum Zeitpunkt dieses Nachtrags noch offen.

## Risiken und offene Entscheidungen
- **Doppelter Android-Berechtigungsdialog beim ersten Start:** Kalender und GPS werden derzeit beide beim Start angestoßen. Reihenfolge und Ablehnungsfall auf echtem Gerät prüfen und ggf. sequenzieren.
- **Geocoder:** Automatische Adressauflösung ist implementiert, aber kein garantierter Online-Dienst; Fehler und 0 Treffer sind noch nicht sauber differenziert.
- **Standortgenauigkeit:** 'Standort aktuell' allein garantiert keine Genauigkeit; Alter, Genauigkeit und Ausfallzustand später explizit zeigen.
- **Settings:** Noch keine eigenständige Architekturkomponente; Hauptaktivität bleibt groß. Sprache ist nur als Präferenz gespeichert.
- **Referenzbilder:** Die fünf vom Nutzer besprochenen Screenshots liegen als Gesprächsreferenz vor; sie wurden nicht als binäre Bilder ins Repository übernommen. Die Wireframes oben sind **keine** Ersatz-Golden-Tests.
- **Qualitäts-Gate:** Grün in GitHub Actions ist **nicht** gleichbedeutend mit freigegebenem Design oder praxiserprobter APK.

## Verbindlicher Arbeitsmodus
GitHub-HEAD prüfen → kleines Paket implementieren → CI inklusive Emulator → echte Screenshots begutachten → Dokumentation aktualisieren → erst dann Test-APK anbieten. Status immer mit Commit und konkretem CI-Nachweis kommunizieren.
