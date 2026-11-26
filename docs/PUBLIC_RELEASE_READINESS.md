# Public Release Readiness – Mein Weg (MeinWeg2)

**Prüfdatum:** 26.11.2026
**Geprüfter Stand:** `main` bei `a796545` („docs: record inaccurate GPS recreation regression and unverified CI"), Working Tree vor den hier dokumentierten Änderungen sauber
**Repository:** `jochenkunkel/mvv-mobility-assistant` (zum Prüfzeitpunkt **privat**, `licenseInfo: null`)
**Zweck:** Technische und sicherheitliche Vorbereitung einer öffentlichen Veröffentlichung. **Dieses Dokument ist keine Freigabe zur Umstellung auf `public`.**

---

## 1. Zusammenfassung

| Frage | Ergebnis |
| --- | --- |
| Secrets im aktuellen Branch gefunden | **Nein** |
| Secrets in der Git-Historie gefunden | **Nein** (590 Commits gescannt) |
| Signierungsschlüssel im Repository | **Nein** – ausschließlich GitHub Actions Secrets |
| Personenbezogene Daten in Testdaten | **Nein** (synthetische Koordinaten; ein Adressbeispiel in der Doku anonymisiert) |
| Private E-Mail-Adresse in Git-Historie | **Ja** – 396 Commits; Freigabe für einen ggf. nötigen Rewrite einholen |
| History-Rewrite erforderlich | **Nein** |
| Technische Hindernisse für `public` | Keine Hard-Blocker gefunden |
| Lizenz entschieden | **Nein** – Eigentümerentscheidung erforderlich |
| Freigabe zur Veröffentlichung | **NOCH NICHT ERTEILT** |

**Empfehlung:** Das Repository ist nach den hier dokumentierten Korrekturen technisch weitgehend veröffentlichungsfähig. Die Umstellung auf `public` darf erst nach (a) ausdrücklicher Freigabe durch den Eigentümer und (b) Lizenzentscheidung erfolgen. Vor der Umstellung sind die in Abschnitt 10 genannten Punkte zu klären, insbesondere die Sichtbarkeit signierter Artefakte.

---

## 2. Durchgeführte Prüfungen

| Prüfung | Methode | Ergebnis |
| --- | --- | --- |
| Repository-Stand | `git clone`, `git status`, `git log`, `gh repo view` | `main` sauber, 115 getrackte Dateien, 3 Remote-Branches, keine Tags |
| Secret-Scan (Historie + Worktree) | Gitleaks 8.30.1, `gitleaks detect --source .` | 590 Commits, ~892,53 KB, **1 Treffer** (False Positive, siehe §3) |
| Dateinamen-Scan (Historie) | `git log --all --name-only --diff-filter=A` | Keine `.jks`, `.keystore`, `.p12`, `.pem`, `.env`, `.apk`, `.aab` oder Secret-Dateien |
| Binärartefakte in Historie | `git rev-list --objects --all` + Größen-/Inhaltsprüfung | Größte Blobs sind `MainActivity.kt` und `PROJECT_STATUS.md`; keine Keystore-Signaturen |
| Inhaltsscan | Regex-Suche nach API-Keys, Tokens, Passwörtern, privaten IPs, E-Mails, Telefonnummern, Adressen | Keine Zugangsdaten; Treffer ausschließlich Dokumentation über Secrets und öffentliche MVV-URLs |
| Signierungskonfiguration | Review `app/build.gradle.kts`, `android.yml`, `release.yml` | Keystore/Passwörter nur aus Umgebungsvariablen / Actions Secrets |
| Berechtigungen & Exporte | Review `AndroidManifest.xml`, `GpsDebugRecorder.kt`, `DebugResponseCapture.kt`, `MainActivity.kt` | Debug-Exporte nutzen SAF und sind explizit user-getriggert; keine automatische Persistenz |
| Test-/CI-Skripte | Ausführung der Offline-Guards (siehe §9) | Alle drei Preflight-Prüfungen **lokal grün** nach Korrektur des Syntaxfehlers |
| Öffentliche Struktur | Bestandsaufnahme `.github/`, README, Doku | Security Policy, Templates, Contribution Guidelines und `.gitattributes` ergänzt |

### Werkzeuge und Grenzen

- Die in dieser Runde verwendeten lokalen Werkzeuge und Scan-Reports liegen persistent unter `.work/tools/` und `.work/reports/` (durch `.gitignore` ausgeschlossen, nicht Teil des Repositories). Gitleaks 8.30.1 kann dort als `.work/tools/gitleaks` erneut ausgeführt werden; Gradle 8.13 liegt unter `.work/tools/gradle-8.13/`.
- Gitleaks erkennt Muster und Entropie; es ersetzt keine manuelle Prüfung und keine Rotationsprüfung bei externen Diensten.
- GitHub-Actions-Job-Logs waren über die CLI nicht abrufbar (`gh run view … --log-failed` lieferte keine Ausgabe). CI-Ergebnisse wurden daher **nicht** als erfolgreicher Nachweis verwendet. Der neue Lauf `45795582864` auf `2b13c4a` bestätigt jedoch erneut einen Preflight-Abbruch nach ~2 s mit `steps=[]` und fünf übersprungenen Jobs – ein externes Runner-/Kontingentproblem, kein Kotlin- oder Skriptfehler.
- Andere Chatverläufe sind nicht live zugänglich; es wurden keine unbekannten Anforderungen unterstellt.

---

## 3. Ergebnis der Secret-Scans

Gitleaks 8.30.1 scannte alle 590 Commits und den Arbeitsbaum.

**Ein Treffer, kein Secret:**

- Datei: `docs/PREFLIGHT-DIAGNOSTICS-RUNBOOK.md`, Zeile 33
- Commit: `8f8cb49` (23.11.2026)
- Regel: `generic-api-key`
- Gematchter Text: `API: \`diagnose=failure\``
- Bewertung: Die Regex interpretierte die Beschreibung eines fehlgeschlagenen Actions-Jobs als API-Key. Der Wert `\`diagnose=failure\`` ist kein Zugangsdatum.

**Maßnahme:** Der Satz wurde im aktuellen Branch umformuliert („Job status: the `diagnose` job failed …"), sodass der Regex-Trigger im Worktree verschwindet, ohne einen breit gefassten Allowlist-Eintrag einzuführen. Der historische Commit `8f8cb49` enthält den alten Text unverändert; ein erneuter Historie-Scan meldet daher weiterhin genau diesen einen False Positive (Fingerprint `8f8cb49:docs/PREFLIGHT-DIAGNOSTICS-RUNBOOK.md:generic-api-key:33`). Ein Worktree-Scan ohne Git-Historie ist sauber. Da es sich nicht um ein Secret handelt, ist kein History-Rewrite und keine Allowlist erforderlich.

**Nicht gefunden:** API-Keys, Passwörter, Tokens (`gho_`/`ghp_`/`github_pat_`), private Schlüssel, Keystores, `.env`-Dateien, Base64-Schlüsselblobs, interne IPs, Debug-Logs mit sensiblen Inhalten, eingecheckte APK/AAB.

---

## 4. Gefundene Risiken

| ID | Risiko | Schwere | Status |
| --- | --- | --- | --- |
| R-01 | Syntaxfehler in `scripts/test-emulator-navigation.py` blockiert den Preflight-Job (seit `5df2a0fb`, 17.11.) | Hoch (CI) | **Behoben**, lokal verifiziert |
| R-02 | Ein Adressbeispiel in `docs/COCKPIT-V2-PLAN.md`, das aus einem Praxistest-Screenshot stammen könnte | Niedrig (PII) | **Behoben**, durch Musteradresse ersetzt |
| R-03 | Gitleaks-False-Positive erschwert saubere wiederholte Scans | Niedrig | **Behoben**, Text umformuliert |
| R-04 | Fehlende `.gitignore`-Regeln für Keystores, Secrets und Debug-Exporte | Mittel | **Behoben**, `.gitignore` gehärtet |
| R-05 | Kein `LICENSE` – Public Repository wäre nicht open source | Hoch (rechtlich) | **Offen**, Eigentümerentscheidung |
| R-06 | Öffentliche Artefakte: signierte Update-APK/AAB und Debug-APK sind in GitHub Actions für Betrachter des Runs herunterladbar | Mittel–Hoch | **Offen**, Eigentümerentscheidung; keine Änderung vorgenommen |
| R-07 | Keine Security Policy, keine Issue-/PR-Templates, keine Contribution Guidelines | Niedrig | **Behoben** |
| R-08 | CI-Job bricht nach ~2 s mit 0 Schritten ab; Runner-/Billing-Problem bleibt bestehen | Mittel (Nachweis) | **Offen**, administrativ zu klären; nicht durch den Syntaxfix behoben |
| R-09 | Keine Lizenz-/CLA-Regel für externe Beiträge | Niedrig | **Offen**, Teil der Lizenzentscheidung |

**Hinweis zu R-06:** Die Workflows laden aktuell `mein-weg-debug-apk-not-update-guaranteed`, `mein-weg-signed-update-apk` und `mein-weg-signed-release` als Artefakte hoch. In einem öffentlichen Repository sind Actions-Artefakte und Logs für Dritte sichtbar. Die Logs enthalten keine Secret-Werte; ein heruntergeladenes signiertes Paket ist jedoch ein gültig signiertes Update-APK. Eine Änderung der Upload- oder Signierungspolitik wurde **nicht** eigenmächtig vorgenommen, da die Funktionsweise der bestehenden Secrets vollständig geklärt bleiben muss.

---

## 5. Behobene Probleme (in dieser Runde)

1. **CI-Blocker:** Fehlende schließende Klammer in `scripts/test-emulator-navigation.py:39` korrigiert. `python3 scripts/test-emulator-navigation.py` läuft jetzt mit 30 bestandenen Tests; `bash scripts/preflight.sh` und `python3 scripts/test-ci-concurrency.py` ebenfalls grün. Das ist ein **lokales** Ergebnis, kein GitHub-Actions-Nachweis.
2. **PII:** Adressbeispiel in `docs/COCKPIT-V2-PLAN.md` anonymisiert.
3. **Scan-Hygiene:** Gitleaks-Trigger im Runbook umformuliert.
4. **Härtung:** `.gitignore` um Keystore-, Secret-, Env- und Debug-Export-Muster erweitert; `.gitattributes` hinzugefügt.
5. **Öffentliche Struktur:** `.github/SECURITY.md`, Issue-/PR-Templates, `CONTRIBUTING.md`, `AGENTS.md` und `docs/LICENSE-DECISION.md` ergänzt.
6. **Dokumentation:** README, `docs/CHAT-START.md` und `docs/PROJECT_STATUS.md` auf den neuen Stand gebracht.

---

## 6. Status der Git-Historie

- **590 Commits** mit Gitleaks gescannt; keine echten Secrets gefunden.
- Keine Datei mit Keystore-/Secret-Dateinamen wurde jemals hinzugefügt (`git log --all --diff-filter=A`).
- Keine ungewöhnlichen Binärblobs; größte Objekte sind Quelltext und Statusdokumentation.
- **Fazit:** Ein History-Rewrite ist **nicht erforderlich**. Es wurde kein Rewrite durchgeführt und keine Freigabe dafür angefordert.
- Die Historie enthält viele datierte Status-/CI-Dokumente mit veralteten Aussagen. Das ist ein Konsistenz-, kein Sicherheitsproblem; die aktuelle Datei `docs/PROJECT_STATUS.md` ist maßgeblich.

---

## 7. Status der Android-Signierung

| Aspekt | Befund |
| --- | --- |
| Release-Signierung | `release.yml` nutzt `MEIN_WEG_KEYSTORE_BASE64`, `MEIN_WEG_KEYSTORE_PASSWORD`, `MEIN_WEG_KEY_ALIAS`, `MEIN_WEG_KEY_PASSWORD` |
| CI-Debug-Signierung (stabile Updates) | `android.yml` nutzt `MEIN_WEG_CI_SIGNING_KEY_B64`, `MEIN_WEG_CI_SIGNING_STORE_PASSWORD`, `MEIN_WEG_CI_SIGNING_KEY_ALIAS`, `MEIN_WEG_CI_SIGNING_KEY_PASSWORD` |
| Im Repository | Keine Keystore-Datei, kein Passwort, kein Base64-Blob |
| Fork-PR-Schutz | Signierung wird in `android.yml` für `pull_request` übersprungen; `release.yml` ist nur `workflow_dispatch` |
| Least privilege | Alle Workflows mit `permissions: contents: read` |
| Änderungen in dieser Runde | **Keine** an Secrets, Workflows oder Signierungskonfiguration |
| Freigabegrenze | Bestehende Secrets wurden nicht gelöscht, überschrieben oder rotiert |

**Offen vor Veröffentlichung:** Klärung, ob die hochgeladenen signierten Artefakte (R-06) auch öffentlich herunterladbar sein sollen. Bis zur Entscheidung bleibt die Konfiguration unverändert.

---

## 8. Lizenzstatus

**Keine Lizenz vorhanden** (`licenseInfo: null`, kein `LICENSE`). Eine Lizenz wurde nicht eigenmächtig gewählt. `docs/LICENSE-DECISION.md` bereitet Apache-2.0 (Empfehlung), MIT, GPL-3.0 und „all rights reserved" als Optionen vor und dokumentiert die offenen Entscheidungen. Ohne Entscheidung ist das Repository nach Veröffentlichung nicht_open_source_ im rechtlichen Sinn.

---

## 8a. Third-Party- und Datenschutzprüfung (fokussierte Nachprüfung)

| Prüfpunkt | Befund | Bewertung |
| --- | --- | --- |
| `LICENSE` / GitHub-Lizenzmetadaten | Kein `LICENSE`; API liefert `license: null` | **Blocker** für Open-Source-Veröffentlichung; Eigentümerentscheidung erforderlich |
| Persönliche E-Mail in Git-Historie | `[alte private E-Mail-Adresse]` in **396** Commits (neben GitHub-Noreply-Adresse) | **Privatsphärenrisiko** bei `public`. GitHub kann die Adresse in der Weboberfläche verbergen, ein `git clone` enthält sie jedoch weiterhin im Klartext. Entfernung erfordert History-Rewrite → **nur mit ausdrücklicher Freigabe** |
| Echte Kalender-/GPS-/Adressdaten im Repository | Keine. Alle Koordinaten in Tests sind synthetisch (z. B. 48.1000/11.5000); Fallback ist Marienplatz als öffentliches Wahrzeichen | **Sauber** |
| Adressbeispiel in Dokumentation | Aus Screenshot-Kontext, durch Musteradresse ersetzt | Behoben |
| Debug-Exporte | `GpsDebugRecorder` und `DebugResponseCapture` nur in Debug-Builds, opt-in, im Speicher begrenzt (2400 Events / 512 KB), Export über Android SAF, keine automatischen Schreib- oder Upload-Vorgänge | **Sauber**, aber bei Weitergabe exportierter Dateien Vorsicht geboten |
| OSM-Kartenkacheln | `WalkingMiniMap` nutzt `TileSourceFactory.MAPNIK` (OpenStreetMap-Standardkacheln); **keine sichtbare Attribution im Code gefunden** | **Offen** – OSM-Tile-Nutzungsbedingungen verlangen „© OpenStreetMap contributors". Siehe Issue #11 |
| Routing | `WalkingNavigation` nutzt öffentlichen OSRM-Server `routing.openstreetmap.de/routed-foot` | **Offen** – Nutzungsbedingungen/Rate-Limits und User-Agent zu prüfen (Issue #11) |
| Geocoding | `DestinationLookup` nutzt Android `Geocoder`, der je nach Gerät einen Online-Dienst abfragen kann; Zieltexte können dabei an Anbieter übertragen werden | **Offen** – Datenschutzhinweis/Transparenz erforderlich (Issue #11) |
| MVV EFA | Öffentlicher Endpunkt `efa.mvv-muenchen.de`, keine API-Keys im Repo | Kein Secrets-Risiko; Nutzungsbedingungen nicht dokumentiert |
| Google Maps Link | Reiner `dir/?api=1`-Deeplink ohne API-Key | Unkritisch |
| Drittanbieter-Lizenzen | Compose/Activity/Glance/osmdroid (Apache-2.0), `org.json` laut POM Public Domain, JUnit 4.13.2 (EPL-1.0, nur Test) | Kein Lizenzkonflikt erkannt; nach Wahl der Hauptlizenz trotzdem `NOTICE`/Abhängigkeitsliste empfehlenswert |

**Fazit:** Inhaltlich liegen keine echten personenbezogenen Daten im Repository. Zwei Punkte sind vor einer öffentlichen Veröffentlichung rechtlich relevant und zu klären: **(1) die fehlende Hauptlizenz** und **(2) die 396 historischen Commits mit privater E-Mail-Adresse**. Drittanbieter-Attribution und Datenschutztransparenz sind offen und in Issue #11 erfasst; sie blockieren die Code-Veröffentlichung nicht, sollten aber vor einer Store-/Produktfreigabe geklärt werden.

---

## 9. Testergebnisse dieser Runde (lokal)

Folgende Befehle wurden **lokal auf diesem Checkout ausgeführt** (JDK 17, Gradle 8.13, Android SDK 35):

| Befehl | Ergebnis |
| --- | --- |
| `python3 scripts/test-ci-concurrency.py` | exit 0 – alle Guards bestanden |
| `python3 scripts/test-emulator-navigation.py` | exit 0 – **30 Tests bestanden** (nach Syntaxfix) |
| `bash scripts/preflight.sh` | exit 0 – PASS: shell and Python syntax |
| `python3 -m unittest discover -s scripts -p 'test_*.py'` | exit 0 – 6 Tests bestanden |
| `gitleaks detect --source . --no-git` (nach Bereinigung) | exit 0 – keine Funde im Worktree |
| `gitleaks detect --source .` (Historie, nach Bereinigung) | 1 bekannter historischer False Positive aus `8f8cb49`, keine Secrets |

| `gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug` (lokal) | 85 Unit-Tests bestanden (0 Fehler/errors/skipped), Debug-APK erzeugt |
| `apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk` | bestanden; Signatur ist das übliche Android-Debug-Zertifikat, **keine** stabile Update-Signatur |

Android-Lint lief bei Erstellung dieses Berichts noch; das Ergebnis ist in `docs/PROJECT_STATUS.md` nachzutragen. **Ein lokaler Erfolg ersetzt keinen GitHub-Actions-Lauf und keine Geräteabnahme.**

---

## 10. Noch offene Sicherheitsfragen / Entscheidungen des Eigentümers

1. **Lizenz:** Welche Lizenz gilt? (Empfehlung: Apache-2.0). Copyright-Inhaber und Jahr festlegen.
2. **Veröffentlichungsfreigabe:** Ausdrückliche Zustimmung zur Umstellung `private` → `public` einholen. Diese Runde hat das Repository **nicht** umgestellt.
3. **Artefakte (R-06):** Sollen signierte Update-APKs/AABs in öffentlich sichtbaren Actions-Runs als Artefakt verfügbar bleiben? Falls nein: Upload auf manuelle/tag-basierte Läufe beschränken oder Entfernung alter Artefakte prüfen. Keine Änderung ohne Freigabe.
4. **Actions-Kontingent/Billing:** Die historische Blockade war laut dokumentierter Fehlermeldung ein Abrechnungs-/Budgetproblem. Ob `public` dieses Problem tatsächlich löst, ist **nicht** automatisch anzunehmen; nach Freigabe real prüfen.
5. **CI-Logs:** Runner-/Policy-Diagnose administrativ klären, da Logs nicht abrufbar waren.
6. **Persönliche E-Mail-Adresse:** `[alte private E-Mail-Adresse]` steht in 396 Commits. Soll sie vor `public` aus der Historie entfernt werden? Das erfordert einen History-Rewrite (z. B. `git filter-repo`) mit anschließendem Force-Push und **ausdrücklicher Freigabe**. Alternativ genügt ggf. die GitHub-Einstellung „E-Mail-Adressen privat halten" für die Weboberfläche – ein Clone bleibt davon unberührt.
7. **OSM-/Routing-Attribution (Issue #11):** Soll die sichtbare „© OpenStreetMap contributors"-Attribution noch vor Veröffentlichung ergänzt werden? Sie ist nicht nur für die Veröffentlichung des Codes, sondern für die Kartennutzung selbst relevant.
8. **Rotationsprüfung:** Da keine Secrets gefunden wurden, besteht kein rotierender Bedarf. Falls der Eigentümer außerhalb des Repos vermutet, dass ein Key je exponiert war, ist er extern zu rotieren.

---

## 11. Abschließende Veröffentlichungsempfehlung

**Technisch: bedingt bereit.** Die Repository-Inhalte enthalten keine Secrets, keine privaten Schlüssel und keine personenbezogenen Testdaten; die Historie benötigt keinen Rewrite; die grundlegende öffentliche Struktur ist vorhanden; der lokal reproduzierbare CI-Blocker wurde behoben.

**Freigabe: NOCH NICHT ERTEILT.** Die Umstellung auf `public` setzt voraus:

- [ ] ausdrückliche Freigabe des Eigentümers,
- [ ] Lizenzentscheidung und `LICENSE`-Datei,
- [ ] Entscheidung zur privaten E-Mail-Adresse in 396 historischen Commits,
- [ ] Entscheidung zur Sichtbarkeit signierter Artefakte (R-06).

Erst nach diesen Schritten darf das Repository öffentlich geschaltet werden. Danach ist der tatsächliche CI-Status erneut zu prüfen und die vollständige Build-/Testkette zu dokumentieren; ein erfolgreicher GitHub-Actions-Lauf liegt zum Zeitpunkt dieses Berichts **nicht** vor.
