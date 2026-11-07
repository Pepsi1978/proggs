---
name: newskompass-release-installieren
description: "NewsKompass aufs Handy immer als Release-APK aus dem apk-update-Lauf installieren, nicht den Debug-Build"
metadata:
  node_type: memory
  type: feedback
  originSessionId: f46177b8-483a-4d59-921a-bef39a00297e
  modified: 2026-11-07T13:55:08.923Z
---

NewsKompass nach dem apk-update-Lauf mit der signierten Release-APK aus `Meine Ablage\Dokumente\Updates\NewsKompass\` installieren (`adb -s <IP>:5555 install -r …`), nicht `app-debug.apk`.

**Why:** Frank meldete am 27.09.2026 Ruckeln in den Einstellungen; der Compose-Debug-Build ist deutlich langsamer. Paket und Schlüssel sind gleich, Wechsel ohne Datenverlust.

**How to apply:** Build, Commit, Push, apk-update, danach die Release-APK installieren. Muster wie [[genialeideen-install-schnell]].
