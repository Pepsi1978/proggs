# Bug-Almanach: adb über WLAN (Wireless debugging, tcpip 5555, mDNS)

Stand: 23.09.2026 · Anker: platform-tools/adb 37.0.0 (Windows 11 26200), Android 17 (SDK 37, Samsung SM-F971B, One UI), UpdateStation 1.0.11
Gegenseite: `best-practices/android/adb-wlan-debugging.md` · Werkzeug: `Werkzeuge/adb-wlan/`
Quelle der Einträge: eigene Messungen am Gerät (23.09.2026) + Sonnet-5-Recherche (6 Researcher).

## 1. Nach PC-Neustart / kill-server ist das Handy weg
- **Symptom:** `adb devices` leer, obwohl Handy im WLAN und gepaart; `adb mdns services` zeigt `_adb-tls-connect`.
- **Ursache:** Der adb-Server merkt sich keine `adb connect`-Geräte. mDNS-Auto-Connect ist rein ereignisgesteuert (EndpointCreated/Updated) und greift bei schon laufend beworbenen Diensten nach Serverstart nicht zuverlässig – auch nicht mit `ADB_MDNS_AUTO_CONNECT=adb-tls-connect` (selbst gemessen). Ab 37.0.0 ist `libadbmdns` statt openscreen das Standard-Backend.
- **Versionen:** alle; gemessen mit 37.0.0.
- **Fix:** Wachhund, der aktiv `adb mdns services` abfragt und verbindet (`adb-wlan.ps1 -Leise`, Aufgabe `adb-wlan-wachhund`). Diagnose: `ADB_TRACE=mdns`, testweise `ADB_MDNS_OPENSCREEN=1`.
- **Quelle:** https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/client/transport_mdns.cpp · https://developer.android.com/tools/releases/platform-tools (offiziell)

## 2. Tote WLAN-Verbindung steht weiter als `device`
- **Symptom:** `adb devices` meldet `device`, Befehle hängen; ein Wachhund, der nur den Status liest, repariert nie.
- **Ursache:** TCP-Transport hat kein Trenn-Signal wie USB; adb erkennt den Abriss erst bei einem fehlschlagenden Befehl.
- **Fix:** Lebenszeichen mit Zeitlimit (`shell getprop ro.serialno`, 8 s), tote Einträge `adb disconnect`.
- **Quelle:** eigene Beobachtung; https://dev.to/hiyoyok/wi-fi-adb-lies-to-you-the-silent-disconnect-problem-no-one-talks-about-ii3 (extern)

## 3. Unterschiedliche adb-Versionen reißen alle WLAN-Verbindungen ab
- **Symptom:** "adb server version (X) doesn't match this client (Y); killing..." – danach alle `IP:port`-Geräte weg.
- **Ursache:** Ein Client anderer Version beendet den Server auf Port 5037; der neue Server kennt die TCP-Geräte nicht. Hier: winget-adb, SDK-adb (Gradle), scrcpy-adb.
- **Fix:** Eine adb für alles: SDK-platform-tools vorn im Benutzer-PATH, Benutzervariable `ADB` → SDK-adb (scrcpy), Skripte wählen fest `$env:ADB` → SDK → PATH; Android Studio: "ADB server lifecycle management" → "Use existing manually managed server".
- **Quelle:** https://github.com/Genymobile/scrcpy/issues/2856 · https://raw.githubusercontent.com/Genymobile/scrcpy/master/FAQ.md · https://issuetracker.google.com/issues/256232162

## 4. Port 5555 nach Handy-Neustart weg
- **Symptom:** `IP:5555` nicht erreichbar, TLS-Dienst ggf. schon.
- **Ursache:** `adb tcpip` gilt nur bis zum nächsten adbd-Neustart; `persist.adb.tcp.port` ohne Root nicht setzbar.
- **Fix:** Über die gepaarte TLS-Verbindung `adb -s <tls> tcpip 5555` (ohne Kabel, getestet), danach bis 12 s auf 5555 warten. TLS-Port wechselt dabei – nie speichern.
- **Quelle:** https://developer.android.com/tools/adb (offiziell) · https://xdaforums.com/t/how-to-make-adb-listen-to-tcpip-5555-after-reboot.1825359/

## 5. Debugging über WLAN nach Neustart aus
- **Symptom:** Nach Handy-Neustart kein `_adb-tls-connect` mehr.
- **Ursache:** Zustand in `AdbDebuggingManager` nur im RAM (by design); Android 17 „adb Wi-Fi 2.0“ schaltet in vertrauten Netzen (SSID+BSSID) wieder ein, ältere Versionen nicht.
- **Fix:** UpdateStation (WRITE_SECURE_SETTINGS per `pm grant`) setzt `adb_wifi_enabled=1` per WorkManager-Aufgabe mit Netzbedingung.
- **Quelle:** https://github.com/GrapheneOS/os-issue-tracker/issues/5807 · https://www.androidauthority.com/android-wireless-adb-auto-reconnect-3624945/

## 6. `adb_wifi_enabled=1` wird sofort wieder 0
- **Symptom:** App setzt den Wert beim Boot, kurz danach ist er 0.
- **Ursache:** Ohne WLAN-Verbindung mit IP setzt das Framework den Wert binnen ~1 s zurück; BOOT_COMPLETED kommt meist vor dem WLAN.
- **Fix:** Erst bei WLAN mit IPv4 schreiben, nach 3 s nachlesen, bei 0 begrenzt wiederholen (WorkManager `Result.retry()`, max. 5).
- **Quelle:** eigene Analyse + https://github.com/mouldybread/adb-auto-enable (extern)

## 7. PendingIntent-NetworkCallback verschwindet nach 5 s (Android 17, Samsung)
- **Symptom:** `registerNetworkCallback(request, PendingIntent)` erscheint in `dumpsys connectivity`, wird ~5 s nach Prozessstart RELEASEd; gleichzeitig Log "Destroyed live tcp sockets for uids".
- **Ursache:** Hängt mit dem Cached-Apps-Freezer (Android 14+) zusammen; genauer Mechanismus nicht belegt.
- **Fix:** Keinen NetworkCallback als Dauer-Trigger verwenden – WorkManager mit Netzbedingung.
- **Quelle:** eigene Messung 23.09.2026; https://source.android.com/docs/core/perf/cached-apps-freezer (offiziell, Kontext)

## 8. Endlosschleife durch Neu-Anmelden im Empfänger
- **Symptom:** ~760 Anmeldungen/s, App 57 % CPU, system_server 183 %.
- **Ursache:** Anmeldung eines Netz-Callbacks bei bestehendem Netz löst sofort den Empfänger aus; meldet der sich neu an → Schleife.
- **Fix:** Nie aus dem ausgelösten Empfänger heraus neu anmelden/planen; nur aus `Application.onCreate`. Nach Einbau immer CPU per `top` prüfen.
- **Quelle:** eigene Messung 23.09.2026

## 9. `unregisterNetworkCallback(pi)` vor `register` löscht die neue Anmeldung
- **Symptom:** REGISTER, 13 ms später RELEASE derselben ID.
- **Ursache:** Das Lösen wird asynchron verarbeitet und kommt nach dem Anmelden an.
- **Fix:** Nicht vorher lösen – gleiche PendingIntent ersetzt die alte Anmeldung ohnehin.
- **Quelle:** eigene Messung 23.09.2026

## 10. Autorisierung läuft nach 7 Tagen ab
- **Symptom:** Nach Urlaub `unauthorized`, Kabel + Tippen nötig.
- **Ursache:** `adb_allowed_connection_time` = 7 Tage; betrifft vermutlich auch WLAN-Verbindungen.
- **Fix:** Entwickleroption „ADB-Autorisierungstimeout deaktivieren“ (ab Android 12). Skript meldet `unauthorized` gezielt.
- **Quelle:** https://developer.android.com/tools/adb (offiziell)

## 11. Gerätesuche scheitert im fremden Netz
- **Symptom:** `adb mdns services` leer, kein Verbinden möglich.
- **Ursache:** Client-/AP-Isolation, Gäste-WLAN, blockiertes Multicast (UDP 5353); Windows-Firewall-Regel fehlt für die benutzte adb.exe.
- **Fix:** Anderes Netz oder Mesh-VPN (Tailscale/ZeroTier) mit fester IP; Firewall-Regel für die SDK-adb prüfen.
- **Quelle:** https://tailscale.com/compare/zerotier · https://keystoneintegration.us/blog/multicast-mdns-igmp-home-network/ (extern)
