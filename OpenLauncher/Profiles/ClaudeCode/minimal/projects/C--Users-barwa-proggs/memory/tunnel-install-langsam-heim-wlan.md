---
name: tunnel-install-langsam-heim-wlan
description: "adb-Install über 10.8.0.7 hängt = langsames Heim-WLAN „Tenda 5 Ghz“ (zeitweise, Arbeiten am Funkmast), nicht die Sicherheitsupdates; direkt im Hotspot installieren"
metadata:
  node_type: memory
  type: project
  originSessionId: bac8f9b2-0332-458c-af94-bfc37c894e43
  modified: 2026-10-28T17:52:45.755Z
---

Am 28.10.2026 hing die Jarvis-Installation (130 MB) über den WireGuard-Tunnel `10.8.0.7:5555`: nur 0,07 MB/s, 270–350 ms von VPS zum Handy, PC → VPS dagegen ~50 ms. Die Ursache war das Heim-WLAN „Tenda 5 Ghz“, das hängt am Mobilfunk. Laut Frank ist es nur vorübergehend so langsam, weil am Funkmast gearbeitet wird; es soll wieder schneller werden. Am VPS-Sicherheitsaudit lag es nicht: Das hat nur Doku in best-practices/bugs geschrieben und WireGuard 51820/udp offen gelassen.

**Why:** Als Ersatz hat Frank einen Hotspot „Borussia Dortmund“ auf seinem Tablet auf dem Balkon eingerichtet (dort ist der Empfang am besten). Der Rechner hängt darin. Ist auch das Handy darin (10.221.112.x), läuft adb direkt mit ~7 ms, und die Installation dauerte 14 s.

**How to apply:** Hängt ein Install über 10.8.0.7, erst `adb shell ip -4 addr` und das PC-WLAN vergleichen. Liegen beide im selben Netz, `adb connect <Handy-LAN-IP>:5555` und darüber installieren. Ist das Handy in „Tenda 5 Ghz“, der PC aber im Hotspot, Frank sagen, er soll das Handy in den Hotspot holen. Sobald Tenda wieder schnell ist, gilt dieser Eintrag nicht mehr; dann löschen. Siehe [[adb-wlan-selbstheilend]].
