# adb über WLAN – dauerhaft zuverlässig

Stand: 23.09.2026 · adb 37.0.0 · Android 17 (Samsung SM-F971B) · Windows 11

## Kurzfassung
Werkzeug: `Werkzeuge/adb-wlan/adb-wlan.ps1` (macOS: `adb-wlan.sh`). Immer das Skript nutzen, nie nur `adb connect <alte IP>`.
Ergebnis ist stets die Serial `IP:5555`.

## Warum es früher nach PC-Neustart ausfiel (Root Cause)
- Ein neuer adb-Server (PC-Neustart, `kill-server`, fremdes Tool mit anderer adb-Version) verbindet **nichts** automatisch neu – auch nicht mit `ADB_MDNS_AUTO_CONNECT` (getestet).
- Das alte Skript kannte ohne Kabel nur einen Weg: gespeicherte IP + Port 5555. IP wechselt mit dem Netz/DHCP, Port 5555 (`service.adb.tcp.port`) stirbt bei jedem adbd-Neustart (Handy-Neustart, `adb usb`).
- Der TLS-Port von "Debugging über WLAN" wechselt bei jedem adbd-Neustart (36313 → 34103 → 44649 beobachtet). **Nie speichern, immer per mDNS suchen.**

## Aufbau (4 Schichten)
1. **mDNS** (`adb mdns services`): findet `_adb._tcp` (5555) und `_adb-tls-connect._tcp` mit aktueller IP in jedem Netz.
2. **TLS → 5555 ohne Kabel**: ist nur "Debugging über WLAN" erreichbar (z. B. nach Handy-Neustart), macht das Skript `adb -s <tls> tcpip 5555`. WLAN-Debugging bleibt dabei an (getestet). Voraussetzung: PC einmal gekoppelt (`adb pair`), Schlüssel liegt in `~/.android/adbkey`.
3. **Handy-Seite (UpdateStation ≥ 1.0.11)**: `WlanDebugReceiver` plant bei Boot, App-Update und jedem Prozessstart eine einmalige WorkManager-Aufgabe (`NetworkType.UNMETERED`, `ExistingWorkPolicy.KEEP`). Sie setzt `adb_wifi_enabled=1` erst bei WLAN mit IPv4, liest nach 3 s nach und wiederholt bei 0 höchstens 5-mal. Braucht `WRITE_SECURE_SETTINGS`; das Skript vergibt es (plus Doze-Ausnahme) bei Kabel sofort, über WLAN stündlich.
4. **Wachhund**: Aufgabe `adb-wlan-wachhund` (bei Anmeldung, nach jedem Netzbeitritt = Ereignis 10000 im Log NetworkProfile/Operational, alle 2 Min; `conhost --headless`, `-Leise`). Einrichten mit `wachhund-einrichten.ps1`. Log: `%LOCALAPPDATA%\adb-wlan\adb-wlan.log` (Pflicht, weil `conhost --headless` Exit-Codes verschluckt). Im Wachhund nie `kill-server` (würde andere Sitzungen/Emulatoren trennen).

## Logikfallen (Review 23.09.2026, alle behoben)
- **Boot vor WLAN:** `BOOT_COMPLETED` kommt meist vor der WLAN-Verbindung; Android setzt `adb_wifi_enabled` ohne WLAN-IP binnen ~1 s auf 0. → WorkManager mit Netzbedingung, IPv4 prüfen, nachlesen, begrenzt wiederholen. **Kein** `registerNetworkCallback(…, PendingIntent)`: Android 17 räumt die Anmeldung ~5 s nach Prozessstart ab (gemessen), und Neu-Anmelden im Empfänger erzeugt eine Endlosschleife (gemessen: 760/s).
- **Nachfrage-Spam:** Pro WLAN-Verbindung (`Network.networkHandle`) höchstens ein Einschaltversuch – sonst wiederholt Android in fremden Netzen die Nachfrage, und ein manuelles Ausschalten wird überstimmt.
- **Tote Verbindung sieht aus wie `device`:** Lebenszeichen mit Zeitlimit prüfen (`shell getprop ro.serialno`, 8 s), sonst repariert der Wachhund nie.
- **Fremde Geräte:** An `ro.serialno` binden (`~/.adb-wlan-serial`, gesetzt bei Kabelverbindung). mDNS-Dienste heißen `adb-<serial>-<zufall>` → danach filtern.
- **`connected to` ≠ freigegeben:** danach auf Zustand `device` warten; bei `unauthorized` Hinweis ausgeben.
- **tcpip über TLS:** danach bis 12 s auf Port 5555 warten, nicht fest 4 s.
- **Mehrere adb-Versionen:** Gradle nutzt SDK-adb. PATH-Reihenfolge: SDK-platform-tools zuerst, Benutzervariable `ADB` → SDK-adb (scrcpy nutzt sie), Skripte wählen fest `$env:ADB` → SDK → PATH. Sonst killt nach einem Update eine adb die andere und alle WLAN-Verbindungen sind weg.

## Getestet
- WLAN am Handy aus/an: TLS und 5555 bleiben.
- `adb usb` (= Port 5555 weg wie nach Handy-Neustart): Skript holt 5555 ohne Kabel über TLS zurück.
- `adb_wifi_enabled=0` + Empfänger auslösen → wieder 1.
- adb-Server-Neustart: Wachhund verbindet binnen ≤ 2 Min von selbst (nach Netzbeitritt ~20 s).
- UpdateStation 1.0.11: neue WLAN-Verbindung → wieder 1; gleiche Verbindung nach manuellem Aus → bleibt 0; CPU 0 %.

## Recherche 23.09.2026 (Sonnet-5-Schwarm, 6 Researcher)
- **Android 17 „adb Wi-Fi 2.0“:** vertraute Netze nach SSID+BSSID, System schaltet dort selbst wieder ein und verbindet mit Android Studio (Quail 3+) automatisch neu. Unsere App ergänzt das (ältere Versionen, Boot-Timing) und kollidiert nicht: ist der Wert schon 1, merkt sie sich nur die Verbindung. (extern: https://www.androidauthority.com/android-wireless-adb-auto-reconnect-3624945/)
- **Auto-Connect des adb-Servers ist ereignisgesteuert**, fragt nicht aktiv nach → aktiver Wachhund ist Best Practice, kein Notbehelf. Ab adb 37.0.0 mDNS-Backend `libadbmdns`; Diagnose `ADB_TRACE=mdns`, testweise `ADB_MDNS_OPENSCREEN=1`. (offiziell: https://developer.android.com/tools/releases/platform-tools)
- **Android Studio:** Settings → Build, Execution, Deployment → Debugger → „ADB server lifecycle management“ → „Use existing manually managed server“, sonst startet Studio den Server neu. (https://issuetracker.google.com/issues/256232162) **Auf diesem PC gesetzt (23.09.2026)** per Datei, bei geschlossenem Studio: `%APPDATA%\Google\AndroidStudio2026.1.1\options\other.xml`, Komponente `PropertyService` → `keyToString`: `"AdbOptionsService.use.user.managed.adb": "true"`, `"AdbOptionsService.user.managed.adb.port": "5037"` (Schlüssel aus `plugins/android/lib/android.jar`, Klasse `AdbOptionsService`). Sicherung: `other.xml.vor-adb-server.bak`. Folge: Studio startet keinen eigenen adb-Server mehr – den hält der Wachhund am Leben (`start-server` bei Anmeldung + alle 2 Min). Neue Studio-Version mit neuem Konfig-Ordner: prüfen, ob die Einstellung übernommen wurde.
- **Samsung:** zufällige MAC rotiert ~24 h → für DHCP-Reservierung im Heim-WLAN „Telefon-MAC“; „Intelligentes WLAN → Zu mobilen Daten wechseln“ aus; Auto Blocker sperrt nur USB-Debugging. (extern)
- **Sicherheit:** Offener Port 5555 ist Botnetz-Ziel (ADB.Miner), aber nur von freigegebenen Schlüsseln nutzbar. Endloses Auto-Einschalten in jedem Netz ist ein Risiko (KeepADB-Issue) → bei uns einmal pro Verbindung, fremde Netze fragt Android selbst nach.
- **Firmennetz mit Client-Isolation:** nur Mesh-VPN (Tailscale/ZeroTier) mit fester IP hilft.
- Vergleichsprojekte: https://github.com/m00sfett/KeepADB (Foreground-Service + BSSID-Liste), https://github.com/mouldybread/adb-auto-enable (wartet nach Boot auf WLAN + 30 s).

## Grenzen
- Fremdes WLAN (Arbeit): Android fragt einmal "Debugging über WLAN in diesem Netzwerk zulassen?" → **"Immer zulassen"** ankreuzen. Gäste-WLAN mit Client-Isolation oder PC/Handy in verschiedenen Netzen: kein WLAN-adb möglich.
- Unbenutzte adb-Schlüssel verfallen nach 7 Tagen → Entwickleroption **"ADB-Autorisierungstimeout deaktivieren"** einschalten.
- Android Studio startet seinen adb-Server selbst mit der SDK-adb – passt, solange PATH und `ADB` auf dieselbe zeigen. Prüfen: `(Get-Process adb).Path`.
