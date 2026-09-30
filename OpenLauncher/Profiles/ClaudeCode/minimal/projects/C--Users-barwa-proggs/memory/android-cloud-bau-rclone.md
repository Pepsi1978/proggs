---
name: android-cloud-bau-rclone
description: "Android-Cloud-Bau lädt per rclone nach Drive; eigener Google-OAuth-Client (Projekt update-upload, In Produktion) seit 27.09.2026, Konfig in SK\\rclone"
metadata:
  node_type: memory
  type: project
  originSessionId: a1ed434a-e11c-43fb-aef7-13c392a9baba
  modified: 2026-09-27T10:27:06.425Z
---

Der Android-Cloud-Bau (`.github/workflows/android-cloud-build.yml`, eingerichtet 26.09.2026) lädt über rclone-Remote `gdrive` (Scope drive, Konto barwandt@gmail.com) nach `Dokumente/Updates/<Projekt>`. Lokale Konfig: `C:\Users\barwa\SK\rclone\gdrive.conf`; als Secret `RCLONE_CONFIG` im Environment `android-signing`.

Seit 27.09.2026 mit **eigenem OAuth-Client** statt der geteilten rclone-client_id: Google-Cloud-Projekt `update-upload` („Update-Upload“), Client-Typ Desktop, Daten in `SK\rclone\google-oauth-client.txt`. Die App steht auf **In Produktion** (sonst läuft der Refresh-Token nach 7 Tagen ab). Pflicht-Links im Branding zeigen auf `https://pepsi1978.github.io/proggs/update-upload/` (Repo `docs/update-upload/index.html`, Domain `pepsi1978.github.io` autorisiert). Alte Konfig als Rückfall: `SK\rclone\gdrive.alt-geteilte-kennung.conf`.

**Why:** Die geteilte rclone-client_id wird 2026 abgeschaltet; im Test-Modus würde der Upload nach 7 Tagen still ausfallen.
**How to apply:** Scheitert „Drive-Ordner holen“ mit Auth-Fehler: `rclone config reconnect gdrive: --config $HOME\SK\rclone\gdrive.conf` (Browser-Freigabe, Warnung „App nicht überprüft“ → Erweitert), dann `gh secret set RCLONE_CONFIG --env android-signing < gdrive.conf`. Nie „Zurück zum Test“ klicken.
