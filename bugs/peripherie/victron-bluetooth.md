# Victron Instant Readout per Bluetooth am Rechner (Windows und macOS)

> Stand: 13.11.2026 22:00. Selbst erlebt mit SmartSolar MPPT und BMV-712 Smart, bleak unter Python 3.15 (Homebrew) auf
> macOS 27 und Windows 11. Code: `Jarvis/werkzeuge/pc-bruecke/jarvis_victron.py`. Best Practices:
> `best-practices/peripherie/victron-bluetooth.md`.

## ⚡ Kurzcheck

| # | Signal | Sofort-Regel |
|---|--------|--------------|
| V1 | macOS: kein Gerät wird erkannt, obwohl beide funken | CoreBluetooth liefert keine MAC, nur eine zufällige UUID je Rechner: am Prüfbyte (erstes Schlüsselbyte, Paketbyte 7) und Schlüssel erkennen |
| V2 | Paket beginnt nicht mit `0x10` | Victron sendet abwechselnd verschiedene Werbepakete; nur `0x10` (Instant Readout) entschlüsseln, den Rest überspringen |
| V3 | `curl.exe`, `-o NUL`, `creationflags` | Windows-eigen; unter macOS `curl`, `/dev/null`, ohne creationflags (sonst ValueError) |

## V1. macOS nennt keine MAC-Adressen

- **Erlebt:** `BleakScanner` meldet unter macOS `CB5167BB-…` statt `xx:xx:…`. Die Zuordnung über `victron.json → mac` fand nichts.
- **Fix:** Kandidaten sind alle Geräte, deren Schlüssel mit dem Prüfbyte des Pakets beginnt; entschlüsseln, bis `auswerten()` Werte liefert.
  Windows bleibt beim schnellen Weg über die MAC.

## V2. Bluetooth-Freigabe unter launchd

- **Erlebt:** Ein Scan aus einem LaunchAgent mit `/opt/homebrew/bin/python3` lief ohne Abfrage durch und sah beide Geräte
  (13.11.2026). Hängt ein Scan trotzdem, steht eine Freigabe aus: Systemeinstellungen → Datenschutz & Sicherheit → Bluetooth → Python erlauben.
