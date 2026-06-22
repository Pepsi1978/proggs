# Samba/SMB-Freigabe ueber WireGuard — Best Practices (wie man es richtig macht)

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/server/samba-wireguard.md`: dort steht
> *was schiefgeht*, hier *wie man eine Samba-Freigabe (Linux-Server → Windows-Netzlaufwerk) ueber
> WireGuard von vornherein richtig aufsetzt*. Quellen: offizielle Samba/UFW-Doku + Recherche 2026-06-22.
> **Anker:** Samba 4.19.5 (Ubuntu 24.04) · Windows 11 (SMB 3.1.1) · WireGuard wg0.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Bind an den Tunnel | `interfaces = lo eth0 10.8.0.0/24` (mit Maske!) + `bind interfaces only = yes` | §1 |
| 2 | Nur im VPN erreichbar | UFW `allow in on wg0 to any port 445`; 445 NIE oeffentlich; nur UDP 51820 offen | §2 |
| 3 | Win11-Kompatibilitaet | `protocol = SMB3` server-seitig; echter User (`smbpasswd -a`) statt Gast | §3 |
| 4 | Windows-Mount stabil | `New-SmbMapping -Persistent` + sauberer Credential-Manager-Eintrag | §4 |
| 5 | Performance | bei Langsamkeit WireGuard-MTU 1350 + MSS-Clamping testen | §5 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/samba-wireguard.md`) |
|----------------------|--------------------------------------------------|
| §1 smb.conf-Bind | §1 Interface-Bind-Falle |
| §2 Firewall | §2 UFW/Ports |
| §3 Win11 | §4 SMB3 · §6 Guest/Signing |
| §4 Mount | §5 persistente Mappings |
| §5 Performance | §3 MTU/MSS |

---

## §1 smb.conf-Grundgeruest (an die VPN-IP binden)
Im `[global]`-Block IMMER das WireGuard-**Subnetz mit Maske** angeben (nicht nur den Interface-Namen `wg0`,
den Samba mangels Broadcast nicht bindet):
```ini
[global]
   interfaces = lo eth0 10.8.0.0/24
   bind interfaces only = yes
   protocol = SMB3
   server min protocol = SMB3
   map to guest = never        # echter User-Login, kein Gast
   smb encrypt = required      # SMB ist sonst UNVERSCHLUESSELT (abhoerbar) — im Tunnel doppelt sicher
[gehirn-docs]
   path = /srv/samba/docs
   valid users = sambauser
   read only = no
   create mask = 0664
   directory mask = 0775
```
Echten Samba-User anlegen: `sudo smbpasswd -a sambauser`. Pruefen, dass `smbd` auf der VPN-IP lauscht:
`netstat -tulpen | grep smbd` → `10.8.0.1:445`.

## §2 Firewall — nur ueber den Tunnel
SMB-Ports ausschliesslich ueber `wg0` zulassen, oeffentlich verweigern:
```
sudo ufw allow in on wg0 to any port 445 proto tcp
sudo ufw allow in on wg0 to any port 139 proto tcp
```
Einziger oeffentlicher Port bleibt **UDP 51820** (WireGuard). `ufw status verbose` kontrollieren. Kein
`ip_forward`/MASQUERADE noetig (Dienst laeuft AUF dem Host, an wg0 gebunden → lokale Zustellung).

## §3 Windows-11-Kompatibilitaet
`protocol = SMB3` server-seitig erzwingen (verhindert die haeufigen Win11-"Passwort falsch"-Probleme).
Echten User statt Gastzugriff nutzen → dann ist KEIN `AllowInsecureGuestAuth`-Registry-Tweak noetig (der
lockert nur die Sicherheit). SMB1 bleibt aus (unsicher, loest die Probleme ohnehin nicht).

## §4 Windows-Netzlaufwerk dauerhaft einbinden
PowerShell (empfohlen):
```powershell
New-SmbMapping -LocalPath 'Z:' -RemotePath '\\10.8.0.1\gehirn-docs' -Persistent:$true -SaveCredentials
```
ODER `net use Z: \\10.8.0.1\gehirn-docs /user:sambauser /persistent:yes`. Credential-Manager-Eintrag pro
Servername UND IP sauber halten (`cmdkey /add:10.8.0.1 /user:sambauser /pass:…`). Sicherstellen, dass der
WireGuard-Tunnel beim Login schon steht (sonst rotes X bis Klick). Test: `Test-NetConnection 10.8.0.1 -Port 445`.

## §5 Performance ueber den Tunnel
Bei langsamen Transfers WireGuard-MTU senken (`MTU = 1350` im `[Interface]` der wg0.conf) + TCP-MSS-Clamping.
Mehrere Werte testen (1380/1350/1280) — MTU-Tuning ist nicht immer eine 100%-Loesung (siehe Almanach §3).

---

## Quellen
Offizielle Samba-Doku (smb.conf, interfaces/bind interfaces only), UFW-Doku, MS-Mount-Doku · Recherche 2026-06-22 (Firecrawl+MiniMax).
