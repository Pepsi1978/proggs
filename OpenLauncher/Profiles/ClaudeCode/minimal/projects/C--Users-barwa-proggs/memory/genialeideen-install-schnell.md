---
name: genialeideen-install-schnell
description: "GenialeIdeen aufs Handy immer als Build-Typ \"schnell\" (assembleSchnell), nicht den Debug-Build"
metadata:
  node_type: memory
  type: project
  originSessionId: 6071058c-42e2-43d2-81c3-4e48bfc984ef
  modified: 2026-10-14T11:26:24.322Z
---

GenialeIdeen hat den Build-Typ `schnell` (`GenialeIdeen/app/build.gradle.kts`: `initWith(release)`, signiert mit dem Debug-Key). Installieren: `gradlew assembleSchnell`, APK aus `app/build/outputs/apk/schnell/`.

**Why:** Der Debug-Build ruckelt spürbar; Frank will auf dem Handy die flüssige Fassung.

**How to apply:** Nach Änderungen an GenialeIdeen `assembleSchnell` bauen und diese APK per WLAN installieren; apk-update läuft unabhängig davon über sein Skript. Siehe [[adb-wlan-selbstheilend]].
