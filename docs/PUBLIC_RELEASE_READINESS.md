# Public Release Readiness – Mein Weg (MeinWeg2)

**Prüfdatum:** 26.11.2026
**Geprüfter Stand:** `main` bei `96065c9` („docs: record inaccurate GPS recreation regression and unverified CI"), Working Tree vor den hier dokumentierten Änderungen sauber
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
| Private E-Mail-Adresse in Git-Historie | Ursprünglich 396 Commits; Rewrite durch Eigentümer freigegeben und ausgeführt (Abschnitt 12) |
| History-Rewrite erforderlich | **Ja** – ausgeführt, keine Secrets betroffen |
| Technische Hindernisse für `public` | Keine Hard-Blocker gefunden |
| Lizenz entschieden | **Ja** – Apache-2.0, `LICENSE` und `NOTICE` vorhanden |
| Freigabe zur `public`-Umsstellung | **ERTEILT und umgesetzt** (26.11.2026, `visibility=public`) |

**Empfehlung umgesetzt:** Nach den dokumentierten Korrekturen und dem verifizierten History-Rewrite (Abschnitt 12) wurde das Repository mit ausdrücklicher Eigentümerfreigabe auf `public` umgestellt. GitHub erkennt `Apache-2.0`; der erste anschließende CI-Lauf war grün. Der Bericht bleibt als Historie der Prüfung erhalten.

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
- GitHub-Actions-Job-Logs waren über die CLI nicht abrufbar (`gh run view … --log-failed` lieferte keine Ausgabe). CI-Ergebnisse wurden daher **nicht** als erfolgreicher Nachweis verwendet. Der neue Lauf `45795582864` auf `df9bb04` bestätigt jedoch erneut einen Preflight-Abbruch nach ~2 s mit `steps=[]` und fünf übersprungenen Jobs – ein externes Runner-/Kontingentproblem, kein Kotlin- oder Skriptfehler.
- Andere Chatverläufe sind nicht live zugänglich; es wurden keine unbekannten Anforderungen unterstellt.

---

## 3. Ergebnis der Secret-Scans

Gitleaks 8.30.1 scannte alle 590 Commits und den Arbeitsbaum.

**Ein Treffer, kein Secret:**

- Datei: `docs/PREFLIGHT-DIAGNOSTICS-RUNBOOK.md`, Zeile 33
- Commit: `5882040` (23.11.2026)
- Regel: `generic-api-key`
- Gematchter Text: `API: \`diagnose=failure\``
- Bewertung: Die Regex interpretierte die Beschreibung eines fehlgeschlagenen Actions-Jobs als API-Key. Der Wert `\`diagnose=failure\`` ist kein Zugangsdatum.

**Maßnahme:** Der Satz wurde im aktuellen Branch umformuliert („Job status: the `diagnose` job failed …"), sodass der Regex-Trigger im Worktree verschwindet, ohne einen breit gefassten Allowlist-Eintrag einzuführen. Der historische Commit `5882040` enthält den alten Text unverändert; ein erneuter Historie-Scan meldet daher weiterhin genau diesen einen False Positive (Fingerprint `5882040:docs/PREFLIGHT-DIAGNOSTICS-RUNBOOK.md:generic-api-key:33`). Ein Worktree-Scan ohne Git-Historie ist sauber. Da es sich nicht um ein Secret handelt, ist kein History-Rewrite und keine Allowlist erforderlich.

**Nicht gefunden:** API-Keys, Passwörter, Tokens (`gho_`/`ghp_`/`github_pat_`), private Schlüssel, Keystores, `.env`-Dateien, Base64-Schlüsselblobs, interne IPs, Debug-Logs mit sensiblen Inhalten, eingecheckte APK/AAB.

---

## 4. Gefundene Risiken

| ID | Risiko | Schwere | Status |
| --- | --- | --- | --- |
| R-01 | Syntaxfehler in `scripts/test-emulator-navigation.py` blockiert den Preflight-Job (seit `5df2a0fb`, 17.11.) | Hoch (CI) | **Behoben**, lokal verifiziert |
| R-02 | Ein Adressbeispiel in `docs/COCKPIT-V2-PLAN.md`, das aus einem Praxistest-Screenshot stammen könnte | Niedrig (PII) | **Behoben**, durch Musteradresse ersetzt |
| R-03 | Gitleaks-False-Positive erschwert saubere wiederholte Scans | Niedrig | **Behoben**, Text umformuliert |
| R-04 | Fehlende `.gitignore`-Regeln für Keystores, Secrets und Debug-Exporte | Mittel | **Behoben**, `.gitignore` gehärtet |
| R-05 | Kein `LICENSE` – Public Repository wäre nicht open source | Hoch (rechtlich) | **Behoben**: Apache-2.0 mit `LICENSE`/`NOTICE` |
| R-06 | Öffentliche Artefakte: signierte Update-APK/AAB in Actions sichtbar | Mittel–Hoch | **Behoben**: Upload nur noch bei manuellem Release/Versionstag; statischer Guard ergänzt |
| R-07 | Keine Security Policy, keine Issue-/PR-Templates, keine Contribution Guidelines | Niedrig | **Behoben** |
| R-08 | CI-Job bricht nach ~2 s mit 0 Schritten ab; Runner-/Billing-Problem bleibt bestehen | Mittel (Nachweis) | **Offen**, administrativ zu klären; nicht durch den Syntaxfix behoben |
| R-09 | Keine Lizenz-/CLA-Regel für externe Beiträge | Niedrig | **Offen**, Teil der Lizenzentscheidung |

**Hinweis zu R-06:** Das Debug-APK-Artefakt bleibt bestehen (keine stabile Update-Signatur). Das signierte Update-APK wird nur noch bei `workflow_dispatch` mit `test_level=release` oder bei Versionstags hochgeladen; der Upload von `mein-weg-signed-release` in `release.yml` war bereits auf manuelle Läufe beschränkt. `scripts/test-ci-concurrency.py` sichert diese Bedingung statisch ab. Die Signierungs-Secrets selbst wurden nicht verändert.

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
- **Fazit für Secrets:** Wegen Secrets war kein Rewrite nötig. Der später freigegebene Rewrite diente ausschließlich der Entfernung der privaten E-Mail-Adresse und ist in Abschnitt 12 dokumentiert.
- Die Historie enthält viele datierte Status-/CI-Dokumente mit veralteten Aussagen. Das ist ein Konsistenz-, kein Sicherheitsproblem; die aktuelle Datei `docs/PROJECT_STATUS.md` ist maßgeblich.

---

## 7. Status der Android-Signierung

| Aspekt | Befund |
| --- | --- |
| Release-Signierung | `release.yml` nutzt die vorhandenen Secrets `MEIN_WEG_CI_SIGNING_KEY_B64`, `MEIN_WEG_CI_SIGNING_STORE_PASSWORD`, `MEIN_WEG_CI_SIGNING_KEY_ALIAS`, `MEIN_WEG_CI_SIGNING_KEY_PASSWORD` |
| CI-Debug-Signierung (stabile Updates) | `android.yml` nutzt `MEIN_WEG_CI_SIGNING_KEY_B64`, `MEIN_WEG_CI_SIGNING_STORE_PASSWORD`, `MEIN_WEG_CI_SIGNING_KEY_ALIAS`, `MEIN_WEG_CI_SIGNING_KEY_PASSWORD` |
| Im Repository | Keine Keystore-Datei, kein Passwort, kein Base64-Blob |
| Fork-PR-Schutz | Signierung wird in `android.yml` für `pull_request` übersprungen; `release.yml` ist nur `workflow_dispatch` |
| Least privilege | Alle Workflows mit `permissions: contents: read` |
| Änderungen in dieser Runde | **Keine** an Secrets, Workflows oder Signierungskonfiguration |
| Freigabegrenze | Bestehende Secrets wurden nicht gelöscht, überschrieben oder rotiert |
| Funktionsnachweis (nach `public`) | CI-Artefakt aus Lauf `45800089593` mit `apksigner verify --print-certs` geprüft: `CN=MeinWeg2, OU=Android, O=MeinWeg2`, SHA-256 `7240b34b…a0534ed`. Die stabilen CI-Signierungs-Secrets sind vorhanden und wirksam. |

**Entscheidung umgesetzt (R-06):** Das signierte Update-Artefakt wird nur noch bei Release-Läufen/Tags hochgeladen; das Debug-APK aus normalen Pushes bleibt verfügbar. Die Signierungs-Secrets selbst blieben unverändert.

---

## 8. Lizenzstatus

**Gelöst:** Der Eigentümer hat am 26.11.2026 **Apache-2.0** gewählt. Ein kanonischer `LICENSE`-Text und eine `NOTICE`-Datei mit Drittanbieter- und Datenquellenhinweisen liegen im Repository-Root. Die Entscheidungsvorlage und der Beschluss stehen in `docs/LICENSE-DECISION.md`. Es bestehen keine Lizenzkonflikte mit den verwendeten Abhängigkeiten.

---

## 8a. Third-Party- und Datenschutzprüfung (fokussierte Nachprüfung)

| Prüfpunkt | Befund | Bewertung |
| --- | --- | --- |
| `LICENSE` / GitHub-Lizenzmetadaten | Kein `LICENSE`; API liefert `license: null` | **Blocker** für Open-Source-Veröffentlichung; Eigentümerentscheidung erforderlich |
| Persönliche E-Mail in Git-Historie | Ursprünglich eine private E-Mail-Adresse in **396** Commits | **Behoben**: freigegebener Rewrite ersetzt sie durch die GitHub-Noreply-Adresse (Abschnitt 12) |
| Echte Kalender-/GPS-/Adressdaten im Repository | Keine. Alle Koordinaten in Tests sind synthetisch (z. B. 48.1000/11.5000); Fallback ist Marienplatz als öffentliches Wahrzeichen | **Sauber** |
| Adressbeispiel in Dokumentation | Aus Screenshot-Kontext, durch Musteradresse ersetzt | Behoben |
| Debug-Exporte | `GpsDebugRecorder` und `DebugResponseCapture` nur in Debug-Builds, opt-in, im Speicher begrenzt (2400 Events / 512 KB), Export über Android SAF, keine automatischen Schreib- oder Upload-Vorgänge | **Sauber**, aber bei Weitergabe exportierter Dateien Vorsicht geboten |
| OSM-Kartenkacheln | `WalkingMiniMap` nutzt `TileSourceFactory.MAPNIK`; sichtbare Attribution „© OpenStreetMap contributors" mit `testTag("osm_attribution")` ergänzt | **Behoben**; Sichtprüfung auf Emulator noch offen |
| Routing | `WalkingNavigation` nutzt öffentlichen OSRM-Server mit User-Agent und 8-s-Timeouts | Dokumentiert in `docs/PRIVACY-AND-PROVIDER-POLICY.md`; Kapazitätsfrage für Produktivbetrieb offen |
| Geocoding | `DestinationLookup` nutzt Android `Geocoder`, der je nach Gerät einen Online-Dienst abfragen kann | Datenfluss und fehlende Speicherung in `docs/PRIVACY-AND-PROVIDER-POLICY.md` dokumentiert |
| MVV EFA | Öffentlicher Endpunkt `efa.mvv-muenchen.de`, keine API-Keys im Repo | Kein Secrets-Risiko; Nutzungsbedingungen nicht dokumentiert |
| Google Maps Link | Reiner `dir/?api=1`-Deeplink ohne API-Key | Unkritisch |
| Drittanbieter-Lizenzen | Compose/Activity/Glance/osmdroid (Apache-2.0), `org.json` laut POM Public Domain, JUnit 4.13.2 (EPL-1.0, nur Test) | Kein Lizenzkonflikt erkannt; nach Wahl der Hauptlizenz trotzdem `NOTICE`/Abhängigkeitsliste empfehlenswert |

**Fazit:** Inhaltlich liegen keine echten personenbezogenen Daten im Repository. Die fehlende Hauptlizenz ist behoben, die historische E-Mail-Adresse wird mit Freigabe entfernt. Drittanbieter-Attribution und Datenschutztransparenz sind nun dokumentiert und technisch umgesetzt; die visuelle Emulator-Prüfung der Attribution sowie die verbindlichen MVV-/OSRM-Nutzungsbedingungen bleiben für eine Store-/Produktfreigabe offen (Issue #11).

---

## 9. Testergebnisse dieser Runde (lokal)

Folgende Befehle wurden **lokal auf diesem Checkout ausgeführt** (JDK 17, Gradle 8.13, Android SDK 35):

| Befehl | Ergebnis |
| --- | --- |
| `python3 scripts/test-ci-concurrency.py` | exit 0 – alle Guards bestanden |
| `python3 scripts/test-emulator-navigation.py` | exit 0 – **30 Tests bestanden** (nach Syntaxfix) |
| `bash scripts/preflight.sh` | exit 0 – PASS: shell and Python syntax |
| `python3 -m unittest discover -s scripts -p 'test_*.py'` | exit 0 – 6 Tests bestanden |
| `gitleaks detect --source . --no-git` (nach Bereinigung) | keine Funde in getrackten Dateien; 4 Treffer ausschließlich in den git-ignorierten, historischen Scan-Reports unter `.work/reports/` |
| `gitleaks detect --source .` (Historie, nach Bereinigung) | 1 bekannter historischer False Positive aus `5882040`, keine Secrets |

| `gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug` (lokal) | 85 Unit-Tests bestanden (0 Fehler/errors/skipped), Debug-APK erzeugt |
| `apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk` | bestanden; Signatur ist das übliche Android-Debug-Zertifikat, **keine** stabile Update-Signatur |

| `gradle --no-daemon :app:lintDebug` (lokal) | `BUILD SUCCESSFUL` – 0 Errors, 37 Warnings |

Der vollständige lokale Lauf dauerte 11m 31s (53 Tasks) und endete mit `BUILD SUCCESSFUL`. **Ein lokaler Erfolg ersetzt keinen GitHub-Actions-Lauf und keine Geräteabnahme.**

---

## 10. Entscheidungen des Eigentümers und verbleibende Punkte

**Vom Eigentümer am 26.11.2026 entschieden:**

1. **Lizenz:** Apache-2.0 – `LICENSE` und `NOTICE` hinzugefügt.
2. **Persönliche E-Mail-Adresse:** History-Rewrite freigegeben; Ersatz durch `354472984+jochenkunkel@users.noreply.github.com` (Abschnitt 12).
3. **OSM-/Datenschutz (Issue #11):** Attribution und Policy-Dokumentation jetzt umsetzen – erledigt; instrumentierte Sichtprüfung steht noch aus.
4. **Artefakte:** Signierte Update-Pakete nur bei Release-Läufen/Tags – umgesetzt und statisch abgesichert.

**Weiterhin offen (keine Freigabe, sondern technische/adminstrative Nachweise):**

5. **Actions-Kontingent/Billing:** Die Blockade ist nach dokumentierter Fehlermeldung ein Abrechnungs-/Compute-Problem. Ob `public` es löst, ist **nicht** automatisch anzunehmen; nach Umstellung real prüfen.
6. **CI-Logs:** Runner-/Policy-Diagnose administrativ klären, da Logs nicht abrufbar waren.
7. **MVV-EFA-/OSRM-Nutzungsbedingungen:** Für einen Produktiv-/Store-Betrieb verbindlich klären; für die Code-Veröffentlichung kein Blocker.
8. **Emulator-/Geräteabnahme:** Attribution und Navigation müssen visuell auf API 35 geprüft werden. Der Attributionstest ist implementiert, aber nicht ausgeführt.
9. **Rotationsprüfung:** Da keine Secrets gefunden wurden, kein Bedarf. Falls außerhalb des Repos ein Key exponiert wurde, extern rotieren.

---

## 11. Abschließende Veröffentlichungsempfehlung

**Technisch und rechtlich: bereit.** Die Repository-Inhalte enthalten keine Secrets, keine privaten Schlüssel und keine echten personenbezogenen Daten. Die Lizenz ist geklärt, die historische E-Mail-Adresse wird entfernt, die Actions-Artefakte sind eingeschränkt, und die lokale Build-/Testkette ist grün. Der CI-Blocker ist administrativer Natur (Compute/Billing).

**Nachtrag zur Veröffentlichung:** Der Eigentümer hat alle Entscheidungen getroffen und die Umstellung auf `public` ausdrücklich freigegeben; GitHub meldet seit 26.11.2026 `visibility=public` und `license=Apache-2.0`. Der anschließende [Android CI #45800089593](https://github.com/jochenkunkel/mvv-mobility-assistant/actions/runs/45800089593) auf `c323364` lief vollständig grün (`preflight` 9 Schritte, `build-and-test` 15 Schritte) und lieferte ein signiertes Debug-APK-Artefakt. Die Instrumentierungs-, Emulator-, Live-Probe- und Release-Gates liefen dabei nicht und bleiben offen. Ein Push-Build ist weiterhin keine APK-Installations- oder Store-Freigabe.

---

## 12. History-Rewrite (E-Mail-Adresse)

**Freigabe:** Eigentümer am 26.11.2026 ausdrücklich erteilt. Die alte private
Adresse wurde durch `354472984+jochenkunkel@users.noreply.github.com` ersetzt.

**Durchführung:** In einem separaten frischen Clone (`.work/history-rewrite/`,
git-ignoriert) mit `git-filter-repo` (Version `31ebad4c8fb3`):

- `--mailmap`: Ersetzung von Autor- und Committer-E-Mail in **allen** Commits.
- `--replace-text`: Ersetzung verbliebener Vorkommen in Dateiinhalten.
- Betroffen: **396** Commit-Metadaten; **597** Commits über alle Refs neu geschrieben.
- Refs: `main`, `audit/2026-11-16-review`, `fix/route-progress-navigation`.
- `git filter-repo` schreibt ein `commit-map` (alt → neu); damit wurden 21
  Dokumentationsdateien aktualisiert. Alle **13** `commit/…`-Links in `docs/`
  wurden auf existierende neue SHAs geprüft (`git cat-file -e`: 0 Fehler).

**Verifikation nach dem Rewrite:**

- `git log --all --format='%ae|%ce' | sort -u` → ausschließlich
  `354472984+jochenkunkel@users.noreply.github.com` (sowie `noreply@github.com`
  als Committer einzelner Commits).
- `git grep` nach der alten privaten Adresse über alle lokalen Branches → keine Treffer.
- Keine Secrets, Keystores oder Signierungsdaten verändert; einzige
  Inhaltsänderung war der Ersatz der E-Mail-Zeichenkette in historischen
  Dokumenten.

**Folgen:** Alle Commit-SHAs haben sich geändert; alte SHA-URLs sind nicht mehr
verlässlich auflösbar. Ein Force-Push aller drei Branches ist erforderlich.
GitHub kann alte Objekte vorübergehend über Caches, PR-Refs oder Reflogs
vorhalten; eine vollständige Entfernung auf der Serverseite liegt nicht in der
Hand des Repositories. Lizenz, Tags und Signierungs-Secrets blieben unberührt.
