---
name: perfectmoment-version-bump-pflicht
description: "PerfectMoment — bei jeder Änderung versionCode/versionName/VERSION_BUMPED_AT in app/build.gradle.kts bumpen, sonst hält Frank die Installation für fehlgeschlagen"
metadata:
  node_type: memory
  type: project
  originSessionId: b31a6dd0-be7d-4711-a015-61403c35f109
  modified: 2026-08-07T10:25:18.589Z
---

PerfectMoment zeigt `VERSION_BUMPED_AT` aus `app/build.gradle.kts` an – **von Hand gepflegter Text**, keine echte Build-Zeit, ändert sich beim Bauen nicht. Frank prüft an genau diesem Feld, ob die neue Fassung auf dem Handy ist; bleibt es stehen, meldet er "ich sehe keine Veränderung", obwohl die Installation klappte.

**Why:** 07.08.2026: Fix seit 12:21 auf dem Gerät (per `adb install`, `lastUpdateTime` bestätigt), aber der alte Zeitstempel "06.08.2026, 13:49 Uhr" ließ ihn als nicht installiert erscheinen.

**How to apply:** Zu jeder Änderung an PerfectMoment in `app/build.gradle.kts` (wie in der gesamten Git-Historie): `versionCode` +1, `versionName` Patch +1, `VERSION_BUMPED_AT` auf aktuelle Uhrzeit (`date +"%d.%m.%Y, %H:%M Uhr"`). Erst danach bauen und per [[android-apps-immer-installieren]] aufs Gerät schieben.
