---
name: tunnel-install-langsam-heim-wlan
description: "adb-Install über 10.8.0.7 hängt = langsames Heim-WLAN „Tenda 5 Ghz“ (zeitweise, Arbeiten am Funkmast), nicht die Sicherheitsupdates; direkt im Hotspot installieren"
metadata:
  node_type: memory
  type: project
  originSessionId: bac8f9b2-0332-458c-af94-bfc37c894e43
  modified: 2026-11-07T13:54:35.294Z
---

28.10.2026 hing die Jarvis-Installation (130 MB) über den WireGuard-Tunnel `10.8.0.7:5555`: 0,07 MB/s, 270–350 ms VPS → Handy, PC → VPS dagegen ~50 ms. Ursache: Heim-WLAN „Tenda 5 Ghz“, hängt am Mobilfunk; laut Frank nur vorübergehend so langsam wegen Arbeiten am Funkmast, soll wieder schneller werden. Nicht das VPS-Sicherheitsaudit: das schrieb nur Doku in best-practices/bugs und ließ WireGuard 51820/udp offen.

**Why:** Ersatz: Hotspot „Borussia Dortmund“ auf Franks Tablet auf dem Balkon (dort bester Empfang); der Rechner hängt darin. Ist auch das Handy darin (10.221.112.x), läuft adb direkt mit ~7 ms, Installation 14 s.

**How to apply:**
- Install über 10.8.0.7 hängt → erst `adb shell ip -4 addr` mit dem PC-WLAN vergleichen.
- Beide im selben Netz: `adb connect <Handy-LAN-IP>:5555`, darüber installieren.
- Handy in „Tenda 5 Ghz“, PC im Hotspot: Frank sagen, er soll das Handy in den Hotspot holen.
- Sobald Tenda wieder schnell ist, gilt der Eintrag nicht mehr: löschen. Siehe [[adb-wlan-selbstheilend]].
