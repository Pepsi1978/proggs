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

## Direktverbindung über den PC-Hotspot (seit 02.10.2026)
Fall: Handy in keinem WLAN, in einem anderen Netz oder in einem WLAN, das Geräte voneinander abschottet (öffentliches WLAN wie "Public WiFi SMB"). Dann schaltet `adb-wlan.ps1` als Schritt 6 den **Windows-Hotspot** ein (WinRT `NetworkOperatorTetheringManager`, nur Windows PowerShell 5.1). Das Handy tritt als Client bei, der PC ist `192.168.137.1`, das Handy z. B. `192.168.137.206`. Für adb ist das wie Kabel: install, logcat, shell, pull (Bilder: `adb pull /sdcard/DCIM/Camera`).
- **Handy einrichten:** `Pflege` speichert den Hotspot per `cmd wifi add-network '<SSID>' wpa2 '<Passwort>'` (Android 11+, ohne Root). Der Befehl gibt nichts aus, deshalb wird mit `cmd wifi list-networks` geprüft. SSID und Passwort kommen aus der Windows-Hotspot-Konfiguration und landen nicht im Log. Eine Prüfsumme in `%LOCALAPPDATA%\adb-wlan\hotspot-auf-handy` sorgt dafür, dass bei einer Änderung neu gespeichert wird.
- **Mit Kabel:** Das Skript schickt das Handy per `cmd wifi connect-network` in den Hotspot. Das offene WLAN, in dem das Handy nicht erreichbar war, lässt es per `cmd wifi forget-network <id>` **vergessen**. `cmd wifi add-network '<SSID>' open -d` (Auto-Verbinden aus) wirkt nicht: Das Handy trat trotzdem wieder bei (Android 17, Eintrag als open + owe^ unter derselben Id).
- **Ohne Kabel:** Android wechselt **nicht** von einem funktionierenden WLAN zum Hotspot. Er wird nur angenommen, wenn das Handy in keinem anderen bekannten WLAN ist.
- **Port 5555** überlebt den Netzwechsel. Nach einem Handy-Neustart läuft es über TLS. Der Hotspot (BSSID) ist seit 02.10.2026 als vertrautes Netz eingetragen (`dumpsys adb` → `is_trusted_network=true`), also ohne Nachfrage.
- **Falle mDNS:** Der adb-Server sucht per mDNS nur in Netzen, die **beim Serverstart** schon da waren. Gemessen: Hotspot nach dem Serverstart → `mdns services` leer; nach dem Neustart → `_adb` und `_adb-tls-connect` sichtbar. Deshalb startet die Direktverbindung den Server einmal neu, aber nur, wenn kein Gerät verbunden ist.
- **Ohne Netz am PC:** Gemessen 02.10.2026 bei getrenntem WLAN: `GetInternetConnectionProfile()` liefert den WireGuard-Tunnel. Der Hotspot startete über das erste Profil der Liste (Bluetooth-Netzwerkverbindung), das Handy trat nach 5 s bei. Das Skript probiert deshalb alle Profile durch. Ein laufender Hotspot bleibt an, wenn der PC das WLAN verliert.
- **Falle Pipe-Vererbung:** `& adb start-server` startet den Server mit den geerbten Handles des Aufrufers, und der Aufruf aus einer Agenten-Sitzung kehrt dann nie zurück. Deshalb `Start-Process -WindowStyle Hidden -PassThru` + `WaitForExit(20000)`, kein `-Wait` (wartet in PS 5.1 auf den Server).
- **Handy hängt in einem anderen WLAN:** Android wechselt nicht in den Hotspot. Fehlversuche am 02.10.2026 lagen genau daran und nicht an Standby oder Sperre. Für adb muss das Handy nicht entsperrt sein.
- **Hotspot-Name je PC:** "Direct Superpower <n>!", Nummern in `Werkzeuge/adb-wlan/hotspot-namen.json` (Schlüssel = `$env:COMPUTERNAME`, nicht der Name im alten Windows-Hotspot-Namen). Ein neuer PC nimmt sich im direkten Aufruf die nächste Nummer, committet und pusht die Liste. Umbenennen per `ConfigureAccessPointAsync`, das Passwort bleibt. Vor dem Umbenennen den neuen Namen auf dem Handy speichern, sonst findet es nicht zurück.
- **Standby:** Ein Handy, das schon länger ohne WLAN ist, trat dem Hotspot im Standby in 5 Minuten nicht bei (gemessen 02.10.2026). Direkt nach einem Verbindungsverlust waren es 5 bis 21 s.
- **Nur auf Zuruf:** Den Hotspot startet nur der direkte Aufruf, wenn eine Sitzung das Handy braucht (Installieren, Logcat, Screenshot). Der Wachhund (`-Leise`) startet ihn nie. Tritt in 90 s niemand bei, schaltet das Skript einen selbst gestarteten Hotspot wieder aus (Marke `hotspot-von-uns`). Einen Hotspot, den der Nutzer selbst eingeschaltet hat, fasst es nie an. Windows schaltet den Hotspot von selbst ab, wenn 5 Minuten lang kein Gerät verbunden ist.
- **Jeder PC:** Das Skript liegt im Repo. Der erste Aufruf auf einem neuen PC richtet den Wachhund ein, falls er fehlt. `Pflege` speichert den Hotspot dieses PCs auf dem Handy, sobald das Handy einmal über WLAN oder Kabel erreichbar ist. So kennt das Handy die Hotspots aller PCs.
- **Grenzen:**
  - Der WLAN-Adapter muss den Mobilen Hotspot unterstützen. Die Intel BE201 kann das, obwohl `netsh` „Gehostete Netzwerke: Nein“ meldet. Das ist der alte Hosted-Network-Weg und hier egal.
  - Ohne irgendein Netzwerkprofil am PC lässt Windows den Hotspot eventuell nicht zu. Das ist ungetestet.
  - Nur Windows. `adb-wlan.sh` (macOS) hat diesen Schritt nicht.

## Grenzen
- Fremdes WLAN (Arbeit): Android fragt einmal "Debugging über WLAN in diesem Netzwerk zulassen?" → **"Immer zulassen"** ankreuzen. Gäste-WLAN mit Client-Isolation oder PC/Handy in verschiedenen Netzen: kein WLAN-adb möglich.
- Unbenutzte adb-Schlüssel verfallen nach 7 Tagen → Entwickleroption **"ADB-Autorisierungstimeout deaktivieren"** einschalten.
- Android Studio startet seinen adb-Server selbst mit der SDK-adb – passt, solange PATH und `ADB` auf dieselbe zeigen. Prüfen: `(Get-Process adb).Path`.
