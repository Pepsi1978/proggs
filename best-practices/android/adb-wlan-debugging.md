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
3. **Handy-Seite (UpdateStation ≥ 1.0.5)**: `WlanDebugReceiver` setzt `Settings.Global adb_wifi_enabled=1` bei Boot, App-Update und jedem Prozessstart (PruefWorker). Braucht `WRITE_SECURE_SETTINGS` – vergibt das Skript bei jeder Kabelverbindung per `pm grant` (übersteht App-Updates, nicht Deinstallation).
4. **Wachhund**: Aufgabe `adb-wlan-wachhund` (bei Anmeldung + alle 5 Min, `conhost --headless`, `-Leise`). Einrichten mit `wachhund-einrichten.ps1`. Log: `%LOCALAPPDATA%\adb-wlan\adb-wlan.log`. Im Wachhund nie `kill-server` (würde andere Sitzungen/Emulatoren trennen).

## Getestet
- WLAN am Handy aus/an: TLS und 5555 bleiben.
- `adb usb` (= Port 5555 weg wie nach Handy-Neustart): Skript holt 5555 ohne Kabel über TLS zurück.
- `adb_wifi_enabled=0` + Empfänger auslösen → wieder 1.
- adb-Server-Neustart: Wachhund verbindet binnen ≤ 5 Min von selbst.

## Grenzen
- Fremdes WLAN (Arbeit): Android fragt einmal "Debugging über WLAN in diesem Netzwerk zulassen?" → **"Immer zulassen"** ankreuzen. Gäste-WLAN mit Client-Isolation oder PC/Handy in verschiedenen Netzen: kein WLAN-adb möglich.
- Unbenutzte adb-Schlüssel verfallen nach 7 Tagen → Entwickleroption **"ADB-Autorisierungstimeout deaktivieren"** einschalten.
- Mehrere adb.exe (WinGet, SDK, scrcpy) müssen dieselbe Version haben, sonst beenden sie sich gegenseitig den Server. Prüfen: `Get-Command adb -All` und je `adb version`.
