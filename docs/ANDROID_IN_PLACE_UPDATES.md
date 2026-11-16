# Android-APK-Updates ohne Deinstallation – Mein Weg 2.0

Stand: 16.11.2026. Implementierung in `main`; **kein erfolgreicher Geräte-Update-Test nachgewiesen**.

## Paket, Version und Signierung
- Unveränderliche Application-ID: `de.mvv.mobility`.
- Der CI-Build übergibt `-PmeinWegVersionCode=${{ github.run_number }}`. Bei Workflow-Neuanlage, anderem Build-Kanal oder einer bereits installierten APK mit höherem Code ist die Monotonie separat zu prüfen.
- CI-Debug-APKs werden **nur mit vollständiger stabiler Signierung** als `mein-weg-signed-update-apk` angeboten. Normale Debug-APKs heißen `mein-weg-debug-apk-not-update-guaranteed`.
- Release-Signierung über `MEIN_WEG_KEYSTORE_PATH` ist davon unabhängig.

## Vier GitHub-Actions-Secrets
In Repository Settings → Secrets and variables → Actions:
1. `MEIN_WEG_CI_SIGNING_KEY_B64` – Base64 des privaten Keystores.
2. `MEIN_WEG_CI_SIGNING_STORE_PASSWORD`
3. `MEIN_WEG_CI_SIGNING_KEY_ALIAS`
4. `MEIN_WEG_CI_SIGNING_KEY_PASSWORD`

Keystore **außerhalb** des Repositories erzeugen, z. B.:
```bash
keytool -genkeypair -v -keystore mein-weg-ci.keystore -alias mein-weg-ci -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 mein-weg-ci.keystore
```
Keystore und Passwörter niemals committen. Existiert bereits ein vertrauenswürdiger bisheriger Signierschlüssel, möglichst **denselben** verwenden; ein neu erzeugter Schlüssel ist nicht kompatibel mit bereits anders signierten Installationen.

## Verifikation
GitHub Actions führt `apksigner verify --print-certs` und `aapt dump badging` aus. SHA-256-Fingerabdruck zweier aufeinanderfolgender APKs vergleichen und sicherstellen, dass der zweite `versionCode` höher ist.

Echter Update-Test: Build A installieren, Einstellungen/Profil und lokale Daten anlegen; Build B ohne Deinstallation installieren (`adb install -r app-debug.apk`), danach Daten prüfen. Für Signaturkonflikte `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, für niedrigeren Versionscode `INSTALL_FAILED_VERSION_DOWNGRADE` beachten.

Ein Update mit gleicher Application-ID, kompatibler Signatur und höherem Code erhält regulär private App-Daten. **Keine Deinstallation ohne vorherige Sicherung empfehlen.** Die tatsächliche Erhaltung von Daten, einschließlich SQLite soweit vorhanden, ist am Gerät zu testen.

## Offene Freigabekriterien
- Die Existenz der vier Secrets kann aus dem Quelltext nicht bestätigt werden.
- Signaturkompatibilität mit bereits installierten APKs ist unbekannt.
- Zwei aufeinanderfolgende Builds und ein In-Place-Update auf einem echten Gerät sind noch nicht bestätigt.
- Eine APK darf erst nach diesen Nachweisen als praktisch updatefähig bezeichnet werden.
