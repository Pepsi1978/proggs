# Kurzcheck Best Practices: adb über WLAN (Stand 23.09.2026, adb 37.0.0, Android 17)
Volltext: `best-practices/android/adb-wlan-debugging.md` · Bugs: `bugs/android/adb-wlan-debugging.md`

- Verbinden nur über `Werkzeuge/adb-wlan/adb-wlan.ps1` (Mac: `.sh`), Ergebnis-Serial `IP:5555`.
- Wachhund `adb-wlan-wachhund`: bei Anmeldung, nach Netzbeitritt (Ereignis 10000), alle 2 Min; nie `kill-server`.
- An `ro.serialno` binden, mDNS-Dienste `adb-<serial>-*` filtern.
- Eine adb (SDK) für PATH, scrcpy (`ADB`), Gradle; Android Studio „Use existing manually managed server“.
- Handy: „ADB-Autorisierungstimeout deaktivieren“ an, „Auf Debugger warten“ aus; Heim-WLAN „Telefon-MAC“ + DHCP-Reservierung; „Zu mobilen Daten wechseln“ aus.
- UpdateStation: `pm grant WRITE_SECURE_SETTINGS` + `cmd deviceidle whitelist +de.frank.updatestation` (Skript macht beides stündlich).
- Einschalten per App: WorkManager + WLAN mit IPv4, nachlesen, max. 5 Wiederholungen, einmal pro `networkHandle`.
- Fremdes WLAN: einmal „Immer zulassen“; Firmennetz mit Isolation → Tailscale/ZeroTier.
