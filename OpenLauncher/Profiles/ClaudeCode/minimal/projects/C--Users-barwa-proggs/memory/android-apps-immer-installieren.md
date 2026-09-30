---
name: android-apps-immer-installieren
description: Nach jedem erfolgreichen Android-Build die App ungefragt per adb auf das angeschlossene Handy installieren
metadata: 
  node_type: memory
  type: feedback
  originSessionId: cc0f9c4f-d65b-465c-8448-151128e6c92f
  modified: 2026-08-06T13:06:35.683Z
---

Nach einem erfolgreichen Android-Build (Cortex, Perfect Moment, BestJournal …) die Debug-APK
IMMER ungefragt per `adb install -r app/build/outputs/apk/debug/app-debug.apk` auf das
angeschlossene Gerät spielen — ohne vorher zu fragen.

**Why:** Frank testet jede Änderung sofort am Gerät; das Handy ist immer angeschlossen. Ein Build
ohne Installation heißt für ihn, dass er den Schritt selbst nachholen muss.

**How to apply:** Build → `adb devices` → `adb install -r <debug-apk>` → installierte Version mit
`adb shell dumpsys package <appId> | grep versionName` gegenprüfen und im Abschluss melden.
App danach NICHT aus der Tool-Shell starten (siehe [[apps-nie-aus-claude-shell-starten]]).
