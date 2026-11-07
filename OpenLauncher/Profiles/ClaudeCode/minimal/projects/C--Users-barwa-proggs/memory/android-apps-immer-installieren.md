---
name: android-apps-immer-installieren
description: Nach jedem erfolgreichen Android-Build die App ungefragt aufs Handy installieren (Weg nach Profilregel 18)
metadata: 
  node_type: memory
  type: feedback
  originSessionId: cc0f9c4f-d65b-465c-8448-151128e6c92f
  modified: 2026-11-07T13:55:01.326Z
---

Nach erfolgreichem Android-Build (Cortex, Perfect Moment, BestJournal …) die Debug-APK (`app/build/outputs/apk/debug/app-debug.apk`) IMMER ungefragt aufs Handy spielen, per WLAN nach Profilregel 18 (Jarvis nur über jarvis-install).

**Why:** Frank testet jede Änderung sofort am Gerät. Build ohne Installation heißt, er muss den Schritt selbst nachholen.

**How to apply:** Build → `adb devices` → `adb -s IP:5555 install -r <debug-apk>` (Kabel nur als Notnagel) → installierte Version mit `adb shell dumpsys package <appId> | grep versionName` gegenprüfen und im Abschluss melden. App danach NICHT aus der Tool-Shell starten (siehe [[apps-nie-aus-claude-shell-starten]]).
