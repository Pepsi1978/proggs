---
name: perfectmoment-version-bump-pflicht
description: "PerfectMoment — bei jeder Aenderung versionCode/versionName/VERSION_BUMPED_AT in app/build.gradle.kts bumpen, sonst haelt Frank die Installation fuer fehlgeschlagen"
metadata: 
  node_type: memory
  type: project
  originSessionId: b31a6dd0-be7d-4711-a015-61403c35f109
  modified: 2026-08-07T10:25:18.589Z
---

In PerfectMoment zeigt die App das Feld `VERSION_BUMPED_AT` aus `app/build.gradle.kts` an. Das ist ein **von Hand gepflegter Text**, keine echte Build-Zeit — er aendert sich nicht automatisch beim Bauen.

Frank liest genau dieses Feld ab, um zu pruefen, ob eine neue Fassung wirklich auf dem Handy ist. Bleibt es stehen, meldet er "ich sehe keine Veraenderung", obwohl die Installation erfolgreich war.

**Why:** Am 07.08.2026 war ein Fix bereits seit 12:21 auf dem Geraet (per `adb install`, `lastUpdateTime` bestaetigt), aber der alte Zeitstempel "06.08.2026, 13:49 Uhr" liess ihn als nicht installiert erscheinen.

**How to apply:** Zu jeder Aenderung an PerfectMoment gehoert in `app/build.gradle.kts` — wie in der gesamten Git-Historie: `versionCode` +1, `versionName` Patch +1, `VERSION_BUMPED_AT` auf die aktuelle Uhrzeit (`date +"%d.%m.%Y, %H:%M Uhr"`). Erst danach bauen und per [[android-apps-immer-installieren]] aufs Geraet schieben.
