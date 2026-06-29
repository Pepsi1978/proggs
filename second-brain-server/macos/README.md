# Gehirn-Laufwerke auf macOS automatisch einbinden

macOS-Pendant zu `../windows/wg-drive-*.ps1`. Bindet die beiden Samba-Freigaben des
Gehirn-Servers dauerhaft im Finder ein (erscheinen unter „Speicherorte"):

| Finder (`/Volumes/…`) | = Windows | Server-Pfad |
|-----------------------|-----------|-------------|
| `gedanken` | Z: | `/srv/samba/gedanken` |
| `daten` | Y: | `/srv/samba/daten` |

Erreichbar nur über den WireGuard-Tunnel (`10.8.0.1`, SMB-Port 445 ist nicht öffentlich).

## Voraussetzungen (einmalig)
- `wg`/`wg-quick` installiert (`brew install wireguard-tools`).
- WireGuard-Konfig liegt in `~/SK/second-brain/wireguard/second-brain.conf`.
- Samba-Zugangsdaten in `~/SK/second-brain/samba.env` (NICHT im Repo — secrets-in-sk-folder):
  ```
  SAMBA_USER=frankmac
  SAMBA_PASS=<passwort>
  ```
  `frankmac` ist ein **eigener Mac-Samba-User** (uid 1001, Gruppe `frank`). Der Windows-Zugang
  läuft weiter über `frank` — beide stehen in `valid users` beider Freigaben, sind aber getrennt,
  damit eine Passwort-Änderung auf einer Plattform die andere nicht bricht.

## Einrichtung
```bash
# 1) Benutzer-Teil (KEIN sudo): Mount-LaunchAgent (Login + alle 5 Min) + Sofort-Mount
bash ~/proggs/second-brain-server/macos/setup-macos.sh

# 2) Boot-Tunnel (einmalig, mit sudo): WireGuard beim Systemstart hochfahren
sudo bash ~/proggs/second-brain-server/macos/setup-macos.sh --daemon
```

## Bestandteile
| Datei | Zweck | Ebene |
|-------|-------|-------|
| `wg-drive-mount.sh` | mountet gedanken/daten, sobald SMB-445 erreichbar ist (Gate auf 445, Almanach §5) | User |
| `de.frank.secondbrain.drivemount.plist` | LaunchAgent: ruft das Mount-Skript bei Login + alle 5 Min | User |
| `wireguard-up.sh` | fährt den WireGuard-Tunnel idempotent hoch | root |
| `de.frank.secondbrain.wireguard.plist` | LaunchDaemon: Tunnel beim Boot | System |
| `setup-macos.sh` | Installer für beide Teile | — |

## Troubleshooting
- **Laufwerke fehlen:** `mount | grep 10.8.0.1` (gemountet?) · `nc -z 10.8.0.1 445` (Tunnel oben?) ·
  `ifconfig | grep 10.8.0.2` (WireGuard aktiv?).
- **Logs:** `~/Library/Logs/wg-drive-mount.log` (Mount) · `~/Library/Logs/wg-tunnel.log` (Tunnel).
- **Tunnel manuell:** `sudo wg-quick up ~/SK/second-brain/wireguard/second-brain.conf` /
  `sudo wg-quick down …`.
- **Mount erzwingen:** `bash ~/proggs/second-brain-server/macos/wg-drive-mount.sh`.
- **Gate immer auf Port 445** prüfen, nie auf einen Dienst-Port (brain-api 8000 etc.) — sonst
  blockiert ein Dienst-Neustart das Mapping (siehe `bugs/server/samba-wireguard.md` §5).
