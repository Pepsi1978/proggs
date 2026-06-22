# Bekannte Bugs & Fallen: Samba/SMB-Dateifreigabe (Linux-Server ↔ Windows ueber WireGuard)

> **PFLICHT-LESEN vor dem Einrichten einer Samba-Freigabe**, die ueber WireGuard von einem
> Windows-Client (Netzlaufwerk im Explorer) erreicht werden soll. Fokus: der Linux-Samba-Server
> (Ubuntu 24.04, smb.conf) + der Windows-11-SMB-Client + die WireGuard-Besonderheiten dazwischen.
> Loesungen sind funktionserhaltend (Direktive #3 — nie "Feature weglassen").
>
> **Zweite Seite der Medaille:** `best-practices/server/samba-wireguard.md` (wie man es von
> vornherein richtig macht). Verwandt: [`wireguard.md`](wireguard.md) (das VPN selbst),
> [`vps-hosting.md`](vps-hosting.md).
> **Stand:** recherchiert am **2026-06-22** (Firecrawl + MiniMax M3, quellentreu).
> **Anker:** Samba **4.19.5** (Ubuntu 24.04, Paket `2:4.19.5+dfsg-4ubuntu9.6`) · Windows **11** (SMB 3.1.1) · WireGuard (wg0).
> **Changelog-/Security-Abgleich 2026-06-22:** Der **Upstream-4.19.x-Zweig ist EOL** — letzter Upstream-Security-Release war
> 4.19.1 (Okt 2023); neuere CVEs (CVE-2025-10230/9640 Okt 2025; mehrere im Mai 2026) wurden nur fuer 4.21–4.24 gepatcht.
> Auf Ubuntu 24.04 kommen Fixes daher NUR ueber die Ubuntu-Paketpflege (USN/`apt`) — siehe **§8** (Patching-Pflicht).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ SSH/Ping ueber WireGuard geht, SMB **nicht** erreichbar | Samba bindet sich NICHT automatisch an `wg0` (kein BROADCAST). In `smb.conf`: `interfaces = lo eth0 10.8.0.0/24` (mit **Subnetzmaske/IP**, nicht nur Interface-Name) + `bind interfaces only = yes`. ODER `bind interfaces only = no`. | §1 |
| 2 | Windows 11: "Benutzername oder Passwort ist falsch" trotz korrekter Daten | In `smb.conf` **`protocol = SMB3`** (server-seitig) erzwingen — `client max protocol = SMB3` allein reicht oft NICHT. Credential-Manager-Altlasten loeschen (§5). | §4, §5 |
| 3 | Windows: "Zugriff durch Sicherheitsrichtlinie blockiert (Gastzugriff)" | Registry `HKLM\SYSTEM\CurrentControlSet\Services\LanmanWorkstation\Parameters` → DWORD `AllowInsecureGuestAuth=1` (nur fuer privaten PC; lockert Sicherheit). | §6 |
| 4 | SMB ueber WireGuard sehr langsam (wenige Mbit/s) | MTU-Problem: WireGuard-MTU auf **1350** senken (`ip link set mtu 1350 dev wg0`) + MSS-Clamping fuer TCP. | §3 |
| 5 | Port-Freigabe fuer SMB im Tunnel | SMB-Ports (445/tcp, 139/tcp) NUR ueber `wg0` zulassen: `ufw allow in on wg0 to any port 445` (NICHT oeffentlich!). Einziger oeffentlicher Port bleibt UDP 51820. | §2 |
| 6 | Netzlaufwerk verschwindet/rotes X nach Reboot/Login | Persistentes Mapping per `New-SmbMapping -Persistent` (nicht `New-PSDrive`); Credential-Manager-Eintrag pro Servername UND IP; "auf Netzwerk warten"-GPO; PIN-Login kann stoeren. | §5 |
| 7 | "Brauche ich `ip_forward`/NAT fuer SMB ueber WireGuard?" | **NEIN** — der Dienst laeuft AUF dem VPS, an `wg0` gebunden → lokale Zustellung, kein Forwarding noetig (siehe `wireguard.md` §1). | §1 |
| 8 | ⭐ Samba 4.19.x ungepatcht? (Upstream EOL) | Upstream-4.19-Zweig ist **EOL** (letzter Upstream-Fix 4.19.1). Auf Ubuntu 24.04 kommen Security-Fixes NUR per **`apt`/USN** ins `2:4.19.5+dfsg-…ubuntuX.Y`-Paket → **`unattended-upgrades` aktivieren** bzw. regelmaessig `apt upgrade`. NICHT auf den Upstream-Versionsstring schauen. | §8 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/samba-wireguard.md`) |
|----------------------|-----------------------------------------------------------|
| §1 Interface-Bindung | §1 smb.conf-Grundgeruest (Bind an VPN-IP) |
| §2 UFW/Ports | §2 Firewall nur ueber wg0 |
| §3 MTU/Performance | §3 Performance-Tuning |
| §4–§6 Windows-Client | §4 Windows-Mount (persistent, Credentials) |

---

## 1. ⭐ Samba bindet sich NICHT an das WireGuard-Interface (DER Kern-Bug)
**Symptom:** SSH und `ping` ueber die WireGuard-IP des Servers (`10.8.0.1`) funktionieren, aber der
Windows-Client kann die SMB-Freigabe nicht mounten / `smbclient` von aussen scheitert. `smbstatus`
zeigt keine Verbindungen. `netstat -tulpen | grep smbd` zeigt `smbd` NICHT auf der wg0-IP lauschend.
**Ursache:** Bei `interfaces = …` + `bind interfaces only = yes` bindet Samba ein Interface nur, wenn
**Adresse/Maske explizit** angegeben sind. WireGuard (`wg0`) ist ein Point-to-Point-Interface
(`<POINTOPOINT,NOARP,UP,LOWER_UP>` statt `<BROADCAST,MULTICAST,…>`) und hat **kein Broadcast** — Samba
kann es ueber den blossen Interface-Namen `wg0` nicht korrekt binden und ignoriert es still.
**Versionen:** Samba-uebergreifend, per Design der Interface-Bindung (verifiziert Ubuntu/Samba 4.x).
**FIX (funktionserhaltend):** In `/etc/samba/smb.conf` im `[global]`-Block das WireGuard-**Subnetz mit
Maske** (nicht den Namen) angeben:
```ini
[global]
   interfaces = lo eth0 10.8.0.0/24
   bind interfaces only = yes
```
Alternative (wenn die Bindung weiter zickt): `bind interfaces only = no` — dann lauscht Samba auf allen
Interfaces, und die Zugriffs-Beschraenkung kommt allein ueber die Firewall (§2). Pruefen mit
`ip link` (Interface-Flags) und `netstat -tulpen | grep smbd` (lauscht es auf 10.8.0.1?).
**Quelle:** Unix StackExchange (Samba over WireGuard) · eigene Recherche 2026-06-22.

## 2. SMB-Ports NUR ueber WireGuard freigeben (nicht oeffentlich)
**Ziel:** SMB (445/tcp, optional 139/tcp) darf NICHT oeffentlich erreichbar sein — nur im Tunnel.
**FIX:**
1. Samba an die WireGuard-IP binden (§1).
2. UFW: SMB nur ueber `wg0` zulassen, oeffentlich verweigern:
   ```
   sudo ufw allow in on wg0 to any port 445 proto tcp
   sudo ufw allow in on wg0 to any port 139 proto tcp
   ```
   Es gibt auch das App-Profil `sudo ufw allow Samba` (oeffnet 137/udp,138/udp,139/tcp,445/tcp) —
   aber das ist NICHT auf wg0 beschraenkt; fuer den VPN-only-Fall die `in on wg0`-Form bevorzugen.
3. Einziger oeffentlicher Port bleibt **UDP 51820** (WireGuard). 445 NIE oeffentlich.
**Hinweis:** Die exakte `ufw allow in on wg0 …`-Syntax ist Standard-UFW (in den gefundenen Tutorials
nicht 1:1 gezeigt, aber UFW-dokumentiert) — verifiziert per `ufw status verbose`.
**Quelle:** Ask Ubuntu (UFW + Samba) · UpCloud/LinuxBlog (WireGuard UFW) · eigene Recherche 2026-06-22.

## 3. SMB ueber WireGuard ist langsam — MTU/MSS
**Symptom:** SMB-Transfers ueber den Tunnel brechen auf wenige Mbit/s ein (z.B. 5,5 Mbit/s), obwohl
die Leitung mehr kann.
**Ursache:** MTU-Problem — WireGuard-Overhead + zu grosse MTU → Fragmentierung/Path-MTU-Black-Hole.
**FIX:** WireGuard-MTU senken (Default 1420 → **1350**): `ip link set mtu 1350 dev wg0` bzw. `MTU = 1350`
im `[Interface]` der `wg0.conf`. Zusaetzlich TCP-**MSS-Clamping** auf der Forward-Kette setzen
(Router/Server). **Achtung (ehrlich):** In der Quelle (MikroTik) brachte das anfangs Besserung, fiel
spaeter aber wieder ab — MTU-Tuning ist nicht immer eine 100%-Loesung; im Zweifel verschiedene
MTU-Werte (1380/1350/1280) testen.
**Versionen:** WireGuard-uebergreifend.
**Quelle:** MikroTik-Forum (SMB over WireGuard very slow) · eigene Recherche 2026-06-22.

## 4. Windows 11: SMB3-Protokoll server-seitig erzwingen
**Symptom:** Windows 11 kann die Freigabe nicht verbinden / „Benutzername oder Passwort ist falsch",
obwohl `smbclient` lokal auf dem Server klappt und die Credentials stimmen.
**Ursache:** Protokoll-/Dialekt-Aushandlung zwischen Win11 und Samba scheitert; `client max protocol`
allein greift nicht.
**FIX:** In `smb.conf` `[global]` **`protocol = SMB3`** (bzw. `server min protocol = SMB3`) setzen.
SMB1 NICHT als Workaround aktivieren (loest das Problem laut Quelle nicht und ist unsicher).
**Versionen:** Win 11 + Samba 4.x.
**Quelle:** Unix/Server-Fault-Threads · eigene Recherche 2026-06-22.

## 5. Windows 11: Netzlaufwerk persistent + Credentials sauber
**Symptom:** Mapping verschwindet nach Reboot/Sleep (rotes X), fragt staendig nach Passwort, oder
verbindet erst nach manuellem Klick.
**Ursachen & FIX (laut Quellen):**
- **Credential-Manager-Altlasten:** Pro Server koennen mehrere Eintraege existieren (`\\NAS`,
  `\\NAS.local`, `\\10.8.0.1`) — Windows behandelt sie getrennt. Alte loeschen, sauber neu anlegen
  (Anmeldeinformationsverwaltung → Windows-Anmeldeinformationen), oder `cmdkey /add:10.8.0.1
  /user:sambauser /pass:…`.
- **Persistentes Mapping:** `net use Z: \\10.8.0.1\share /user:sambauser PASS /persistent:yes`
  ODER PowerShell **`New-SmbMapping -RemotePath \\10.8.0.1\share -Persistent:$true -SaveCredentials`**
  (NICHT `New-PSDrive -Persist` — laut MS-Doku nicht fuer SMB gedacht). Vorher alte Sessions:
  `net use \\10.8.0.1 /delete`.
- **Netzwerk beim Login noch nicht bereit:** rotes X bis Klick. GPO „Computer immer auf das Netzwerk
  warten lassen" aktivieren; sicherstellen, dass der WireGuard-Tunnel beim Login schon steht.
- **PIN-Login (Win 11 Pro)** kann gespeicherte Mappings stoeren → mit Microsoft-Konto-Passwort anmelden.
- **VPN-Credentials ueberschreiben SMB-Credentials** (wenn VPN-User == AD-User mit anderem Passwort):
  in der VPN-`.pbk` `UseRasCredentials=1` → `0` setzen, oder GPO „Speicherung von Passwoertern für
  Netzwerkauthentifizierung nicht zulassen".
- **Test:** `Test-NetConnection -ComputerName 10.8.0.1 -Port 445` (→ `TcpTestSucceeded: True`),
  `Get-SmbConnection`, `Get-SmbMapping`.
**Versionen:** Win 10/11.
**Quelle:** mehrere Win-Admin-Quellen · eigene Recherche 2026-06-22.

## 6. Windows 11: Gastzugriff & SMB-Signing
**Symptom A:** „Sie koennen nicht auf diesen freigegebenen Ordner zugreifen, weil die
Sicherheitsrichtlinien Ihrer Organisation den nicht authentifizierten Gastzugriff blockieren."
**FIX A:** NUR auf dem privaten PC: Registry `HKLM\SYSTEM\CurrentControlSet\Services\LanmanWorkstation\
Parameters` → DWORD `AllowInsecureGuestAuth = 1`. **Besser** ist allerdings ein echter Samba-User
(`smbpasswd -a`) statt Gastzugriff — dann ist dieser Reg-Tweak gar nicht noetig.
**Symptom B:** `System error 3227320323` o.ae. — SMB-Signing-/Cipher-Mismatch.
**FIX B:** GPO „Microsoft-Netzwerk (Client): Kommunikation digital signieren (immer)" → Disabled,
ODER server-seitig Signing passend konfigurieren. Vorsicht: lockert Sicherheit — nur bewusst.
**Versionen:** Win 10/11.
**Quelle:** MS TechCommunity (SMB signing & guest auth) · eigene Recherche 2026-06-22.

## 7. Kein IP-Forwarding/NAT noetig (haeufiger Irrtum)
Viele WireGuard-Tutorials setzen `net.ipv4.ip_forward=1` + MASQUERADE. **Fuer eine Samba-Freigabe AUF
dem VPS (an `wg0` gebunden) ist das NICHT noetig** — das Paket an `10.8.0.1:445` wird lokal an `smbd`
zugestellt, nicht weitergeleitet. IP-Forwarding nur, wenn der VPS Clients ins Internet tunnelt
(Full-Tunnel). Siehe [`wireguard.md`](wireguard.md) §1.
**Quelle:** `bugs/server/wireguard.md` · eigene Recherche 2026-06-22.

## 8. ⭐ Samba 4.19.x ist upstream EOL — Patches kommen nur ueber die Ubuntu-Paketpflege
**Symptom:** Man fuehrt Samba **4.19.5** und prueft die Upstream-Security-Seite (samba.org) — fuer den 4.19-Zweig
ist dort seit **4.19.1** (Okt 2023) KEIN Security-Release mehr gelistet. Es entsteht der falsche Eindruck, 4.19.5
sei „aktuell genug" oder es gaebe keine relevanten CVEs.
**Ursache:** Der Upstream-**4.19-Zweig ist End-of-Life**. Samba pflegt nur noch neuere Zweige — die in 2025/2026
veroeffentlichten CVEs wurden ausschliesslich fuer **4.21–4.24** gepatcht (z.B. CVE-2025-10230, CVE-2025-9640 vom
15.10.2025; CVE-2026-4408/4480/2340/3012/3238/1933 vom 26.05.2026). Ubuntu 24.04 liefert aber bewusst 4.19.5 und
**backportet** Security-Fixes in sein eigenes Paket (`2:4.19.5+dfsg-4ubuntu9.6` — das `ubuntuX.Y`-Suffix steigt mit
jedem USN). Der nackte Upstream-Versionsstring `4.19.5` sagt daher NICHTS ueber den Patch-Stand aus.
**Versionen:** Samba 4.19.x auf Ubuntu 24.04 (Noble).
**FIX (funktionserhaltend) — Patch-Stand ueber die Distro sicherstellen, gerade fuer einen exponierten Server:**
- **`unattended-upgrades` aktivieren** (`sudo apt install unattended-upgrades && sudo dpkg-reconfigure -plow unattended-upgrades`)
  ODER regelmaessig `sudo apt update && sudo apt upgrade` — so kommen die Ubuntu-Backport-Fixes ins `samba`-Paket.
- Installierten Patch-Stand pruefen: `apt-cache policy samba` / `dpkg -l | grep samba` (auf das `ubuntuX.Y`-Suffix achten,
  nicht auf `4.19.5`). Offene USNs: `pro security-status` bzw. ubuntu.com/security/notices?package=samba.
- **NICHT** auf einen Upstream-Zweig wechseln/selbst kompilieren, nur um eine hoehere Versionsnummer zu sehen — das Ubuntu-
  Paket ist der gepatchte Pfad. Wer wirklich 4.21+ braucht, geht ueber ein gepflegtes PPA/Backport, nicht per Hand-Build.
- **Mildernd in unserem Setup:** SMB ist nur ueber WireGuard erreichbar (445 nie oeffentlich, §2) + `smb encrypt = required` →
  die Angriffsflaeche ist klein. Das ersetzt das Patchen aber NICHT (Defense in Depth).
- **AD-DC/Kerberos-CVEs aus 4.24** (z.B. CVE-2026-20833, KDC-Enctypes/PAC) betreffen NUR Samba als Domain Controller —
  fuer einen **reinen SMB-File-Server** (unser Fall) NICHT relevant.
**Quelle:** samba.org/samba/history/security.html, cybersecurity-help.cz (Samba 4.19.5), Ubuntu-Paket Noble · Recherche 2026-06-22.

---

## Pflicht-Checkliste vor Samba-ueber-WireGuard
- [ ] `smb.conf`: `interfaces = lo eth0 10.8.0.0/24` (mit Maske!) + `bind interfaces only = yes` (oder `= no`)?
- [ ] `netstat -tulpen | grep smbd` zeigt `smbd` auf `10.8.0.1:445`?
- [ ] UFW: 445/139 NUR `in on wg0`, NICHT oeffentlich? Einziger oeffentlicher Port UDP 51820?
- [ ] `protocol = SMB3` gesetzt (gegen Win11-Auth-Zicken)?
- [ ] Echter Samba-User (`smbpasswd -a`) statt Gastzugriff?
- [ ] Win-Mount persistent via `New-SmbMapping -Persistent` + sauberer Credential-Manager-Eintrag?
- [ ] Bei Langsamkeit: WireGuard-MTU 1350 + MSS getestet?
- [ ] Kein unnoetiges `ip_forward`/MASQUERADE (Split-Tunnel-Dienst braucht es nicht)?
- [ ] **`unattended-upgrades` aktiv / `apt upgrade` regelmaessig** (4.19.x ist upstream EOL — Fixes nur ueber Ubuntu-Paket, §8)?

---

## Quellen (Stand 2026-06-22)
Firecrawl + MiniMax M3 (quellentreu), 3 Recherche-Laeufe: Unix/Server-Fault/Ask-Ubuntu (Interface-Bind,
UFW), MikroTik-Forum (MTU/SMB-Performance), MS TechCommunity + Win-Admin-Quellen (Guest-Auth, Signing,
persistente Mappings). Die exakte End-to-End-Kombination „Ubuntu 24.04 ↔ WireGuard ↔ Win11-Netzlaufwerk"
war in keiner Einzelquelle vollstaendig — dieser Almanach setzt die belegten Teilaspekte zusammen.
