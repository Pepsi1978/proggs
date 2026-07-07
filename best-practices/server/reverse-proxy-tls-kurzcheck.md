# Reverse-Proxy + TLS (Caddy) & Linux-VPS-Betrieb Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Caddy erstmals aufsetzen | ZUERST ACME-Staging (`acme_ca` staging), `email` global, dann Prod | §1 |
| 2 | Zerts persistent | `/data`-Volume (Docker) bzw. `caddy`-User-Owner; Storage NIE als Cache | §1 |
| 3 | reverse_proxy zu lokalem Dienst | an `127.0.0.1`/`10.8.0.1` binden; `handle_path` fuer Subpath; `route {}` wenn Reihenfolge zaehlt | §2 |
| 4 | MCP/SSE durchreichen | `flush_interval -1` + `encode` von der SSE-Route per Matcher ausnehmen | §3 |
| 5 | Echte Client-IP / kein Redirect-Loop | `trusted_proxies` global; uvicorn `--proxy-headers --forwarded-allow-ips="<subnetz>"` (nie `*`) | §4 |
| 6 | systemd-Start-Reihenfolge | `After=/Wants=network-online.target` (+ `wg-quick@wg0`); Docker-Drop-in `After=wg-quick@wg0` | §5 |
| 7 | Dienst nach Reboot | `systemctl enable` (nicht nur start) + `[Install] WantedBy=multi-user.target` | §5 |
| 8 | SSH absichern | Key-only (`PasswordAuthentication no`), `PermitRootLogin prohibit-password`/no, `sshd -t` + 2. Session | §6 |
| 9 | Firewall | nur SSH+80+443+wg-UDP offen; `ufw allow OpenSSH` VOR `enable`; Docker-Ports an 127.0.0.1 binden | §6 |
| 10 | fail2ban | `backend = systemd`, `ignoreip` eigene IP, hinter Caddy/Docker Real-IP + `DOCKER-USER`-Chain | §6 |
| 11 | Auto-Security-Updates | unattended-upgrades NUR Security, needrestart `restart='l'`, Auto-Reboot fix/aus | §7 |
| 12 | Logs/Disk | journald `SystemMaxUse=`, docker `json-file max-size`, `apt autoremove`, `df -i` im Monitoring | §7 |
| 13 | Zeit | `timedatectl set-ntp true`; VMs → chrony (Post-Suspend-Skew) | §7 |
| 14 | Speicher | Swapfile (4 GB) + `vm.swappiness=10`; pro Dienst mem_limit, Reserve fuer Host | §7 |
| 15 | Monitoring | externer Uptime-/Cert-Ablauf-Check + Push-Heartbeat (healthchecks.io-Muster); Disk/RAM-Alarm | §8 |
| 16 | Backup | 3-2-1: lokal + offsite + verschluesselt; Qdrant-Snapshot + Volume; **monatlicher Restore-Test** | §9 |

> **Goldene Ops-Regel:** Bei JEDEM Eingriff an sshd/ufw/fail2ban eine zweite SSH-Session offen halten
> ODER die Provider-VNC/Rescue-Konsole bereit. Erst `sshd -t`/`ufw allow OpenSSH`/`caddy validate`, dann anwenden.
