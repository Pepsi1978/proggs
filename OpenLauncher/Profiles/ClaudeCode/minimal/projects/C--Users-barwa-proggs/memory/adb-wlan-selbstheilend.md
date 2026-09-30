---
name: adb-wlan-selbstheilend
description: WLAN-adb fiel nach PC-Neustart aus; Fix = mDNS + TLS→5555 + UpdateStation-WorkManager + Wachhund-Aufgabe; Fallen dazu
metadata:
  node_type: memory
  type: project
  originSessionId: adb57b23-b7a9-40f3-845e-f88ed3b5ff1a
  modified: 2026-09-23T18:07:12.819Z
---

WLAN-adb zum Handy fiel nach PC-Neustart aus (23.09.2026). Root Cause: neuer adb-Server verbindet nichts von selbst (mDNS-Auto-Connect ist ereignisgesteuert); altes adb-wlan.ps1 kannte ohne Kabel nur gespeicherte IP + Port 5555 (beides flüchtig). TLS-Port von "Debugging über WLAN" wechselt bei jedem adbd-Neustart.

Fix: adb-wlan.ps1/.sh (mDNS, Serial-Bindung, Lebenszeichen, TLS→5555, Pflege über WLAN), UpdateStation ≥1.0.11 (WorkManager setzt adb_wifi_enabled bei WLAN mit IP, prüft nach), Aufgabe `adb-wlan-wachhund` (Anmeldung, Netzbeitritt, alle 2 Min), eine SDK-adb für alles (PATH + Benutzervariable ADB). Details: best-practices/android/adb-wlan-debugging.md, bugs/android/adb-wlan-debugging.md.

**Why:** Nutzer will über Wochen ohne Kabel installieren, auch nach Neustarts beider Geräte.
**How to apply:** WLAN-adb weg → erst `%LOCALAPPDATA%\adb-wlan\adb-wlan.log` und `Get-ScheduledTask adb-wlan-wachhund` prüfen. Nie TLS-Port speichern, im Wachhund nie kill-server. Auf dem Handy nie `registerNetworkCallback(…, PendingIntent)` als Trigger (Android 17 räumt ihn nach ~5 s ab; Neu-Anmelden im Empfänger = Endlosschleife 760/s) — nach jeder Hintergrund-Logik CPU per `adb shell top -b -n 1` prüfen.
