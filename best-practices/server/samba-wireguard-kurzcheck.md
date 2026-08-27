# Samba/SMB-Freigabe ueber WireGuard Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Bind an den Tunnel | konkrete Host-IP: `interfaces = lo 10.8.0.1/24` (NICHT die Netz-Adresse `…0/24`!) + `bind interfaces only = yes` | §1 |
| 2 | Nur im VPN erreichbar | UFW `allow in on wg0 to any port 445`; 445 NIE oeffentlich; nur UDP 51820 offen | §2 |
| 3 | Win11-Kompatibilitaet | `protocol = SMB3` server-seitig; echter User (`smbpasswd -a`) statt Gast | §3 |
| 4 | Windows-Mount stabil | `New-SmbMapping -Persistent` + sauberer Credential-Manager-Eintrag | §4 |
| 5 | Performance | **Dateigroesse entscheidet.** Viele KLEINE Dateien -> `rclone --transfers 64` (gemessen 29,8 statt 1,5 Dateien/s = rund 20x). Wenige GROSSE Dateien -> SMB ist ok, aber `--multi-thread-streams 8` setzen (sonst 25 statt 36 Mbit/s). MTU 1350/MSS erst pruefen, wenn `ping -D` ein echtes Path-MTU-Problem zeigt. Vor jeder Messung die Leitung leerraeumen (Bufferbloat treibt 48 -> 218 ms). | §5 |
| 6 | Patch-Stand | 4.19.x ist upstream EOL → `unattended-upgrades`/`apt upgrade` (Ubuntu backportet Fixes ins Paket) | §6 |
| 7 | Auto-Reconnect-Task (nach Reboot) | In einem ELEVATED/hidden Task NIE `net use` ohne Credentials (haengt am Prompt) → `WNetAddConnection2` mit expliziten Credentials; `EnableLinkedConnections=1` macht das Mapping im Explorer sichtbar; `.ps1` als UTF-8-BOM, ASCII-only; bei MEHREREN Shares vom selben VPS **nicht-persistent** mappen (Flag 0) + persistente `HKCU:\Network`-Eintraege entfernen (sonst Boot-Race → Fehler 1219), 1219 abfangen | §7 |
| 8 | macOS: Tunnel + Laufwerke sollen nach jedem Neustart automatisch da sein | Tunnel-Skript als **Watchdog-Dauerschleife** + `KeepAlive=true` im LaunchDaemon — ein Einmal-Job (`RunAtLoad` allein) laesst launchd beim Exit `wireguard-go` mit der Prozessgruppe toeten. Gesundheit ueber **zwei** Proben (ICMP oder Port 445), erst nach 4 Fehlschlaegen neu aufbauen, danach `launchctl kickstart` (ohne `-k`) fuer den Mount-Agent; Mount-Agent wartet begrenzt auf Port 445. | §8 |
