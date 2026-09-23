# Kurzcheck: adb über WLAN (Stand 23.09.2026, adb 37.0.0, Android 17)
Volltext: `bugs/android/adb-wlan-debugging.md`

- PC-Neustart/kill-server → nichts verbindet sich selbst, mDNS-Auto-Connect unzuverlässig → aktiver Wachhund.
- `device` heißt nicht lebendig → Lebenszeichen mit Zeitlimit prüfen.
- Mehrere adb-Versionen → Server-Kill trennt ALLE WLAN-Geräte → eine SDK-adb (PATH + `ADB`), Studio „manually managed server“.
- Port 5555 stirbt mit adbd → über TLS `tcpip 5555` zurückholen; TLS-Port nie speichern.
- WLAN-Debugging nach Neustart aus; `adb_wifi_enabled=1` ohne WLAN-IP springt in ~1 s auf 0 → erst mit IP schreiben, nachlesen, begrenzt wiederholen.
- Android 17: PendingIntent-NetworkCallback nach ~5 s weg → WorkManager nutzen.
- Nie aus einem Netz-Empfänger heraus neu anmelden (Endlosschleife); kein `unregister` direkt vor `register`.
- 7-Tage-Autorisierung → „ADB-Autorisierungstimeout deaktivieren“.
- Gäste-WLAN/Client-Isolation → kein mDNS → Mesh-VPN.
