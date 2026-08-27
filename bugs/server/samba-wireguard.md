# Bekannte Bugs & Fallen: Samba/SMB-Dateifreigabe (Linux-Server ↔ Windows ueber WireGuard)

> **PFLICHT-LESEN vor dem Einrichten einer Samba-Freigabe**, die ueber WireGuard von einem
> Windows-Client (Netzlaufwerk im Explorer) erreicht werden soll. Fokus: der Linux-Samba-Server
> (Ubuntu 24.04, smb.conf) + der Windows-11-SMB-Client + die WireGuard-Besonderheiten dazwischen.
> Loesungen sind funktionserhaltend (Direktive #3 — nie "Feature weglassen").
>
> **Zweite Seite der Medaille:** `best-practices/server/samba-wireguard.md` (wie man es von
> vornherein richtig macht). Verwandt: [`wireguard.md`](wireguard.md) (das VPN selbst),
> [`vps-hosting.md`](vps-hosting.md).
> **Stand:** recherchiert am **2026-06-22** (Firecrawl + MiniMax M3, quellentreu); erweitert **2026-06-24** um
> zwei live diagnostizierte Windows-Client-Bugs (§9: net use haengt im elevated/hidden Task; §10: EnableLinkedConnections);
> erweitert **2026-06-25** um den Persistent-Login-Race / "mehrere Benutzernamen" (Fehler 1219) bei mehreren Shares vom
> SELBEN VPS (§11); erweitert **2026-08-27** um den Durchsatz-Bug (§14: SMB serialisiert ueber Latenz — bei VIELEN KLEINEN Dateien rund Faktor 20, bei grossen Dateien kein Unterschied; inkl. Bufferbloat-Messfalle und ausgeschlossener Ursachen).
> **Anker:** Samba **4.19.5** (Ubuntu 24.04, Paket `2:4.19.5+dfsg-4ubuntu9.6`) · Windows **11** (SMB 3.1.1) · WireGuard (wg0).
> **Changelog-/Security-Abgleich 2026-06-22:** Der **Upstream-4.19.x-Zweig ist EOL** — letzter Upstream-Security-Release war
> 4.19.1 (Okt 2023); neuere CVEs (CVE-2025-10230/9640 Okt 2025; mehrere im Mai 2026) wurden nur fuer 4.21–4.24 gepatcht.
> Auf Ubuntu 24.04 kommen Fixes daher NUR ueber die Ubuntu-Paketpflege (USN/`apt`) — siehe **§8** (Patching-Pflicht).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ SSH/Ping ueber WireGuard geht, SMB **nicht** erreichbar | Samba bindet sich NICHT automatisch an `wg0` (kein BROADCAST). In `smb.conf` die **konkrete Host-IP mit Maske**: `interfaces = lo 10.8.0.1/24` + `bind interfaces only = yes` — die **Netz-Adresse** `10.8.0.0/24` reicht NICHT (lauscht dann nur auf `lo`; live verifiziert 2026-06-23). ODER `bind interfaces only = no`. | §1 |
| 2 | Windows 11: "Benutzername oder Passwort ist falsch" trotz korrekter Daten | In `smb.conf` **`protocol = SMB3`** (server-seitig) erzwingen — `client max protocol = SMB3` allein reicht oft NICHT. Credential-Manager-Altlasten loeschen (§5). | §4, §5 |
| 3 | Windows: "Zugriff durch Sicherheitsrichtlinie blockiert (Gastzugriff)" | Registry `HKLM\SYSTEM\CurrentControlSet\Services\LanmanWorkstation\Parameters` → DWORD `AllowInsecureGuestAuth=1` (nur fuer privaten PC; lockert Sicherheit). | §6 |
| 4 | SMB ueber WireGuard sehr langsam (wenige Mbit/s) | MTU-Problem: WireGuard-MTU auf **1350** senken (`ip link set mtu 1350 dev wg0`) + MSS-Clamping fuer TCP. | §3 |
| 5 | Port-Freigabe fuer SMB im Tunnel | SMB-Ports (445/tcp, 139/tcp) NUR ueber `wg0` zulassen: `ufw allow in on wg0 to any port 445` (NICHT oeffentlich!). Einziger oeffentlicher Port bleibt UDP 51820. | §2 |
| 6 | Netzlaufwerk verschwindet/rotes X nach Reboot/Login | Persistentes Mapping per `New-SmbMapping -Persistent` (nicht `New-PSDrive`); Credential-Manager-Eintrag pro Servername UND IP; "auf Netzwerk warten"-GPO; PIN-Login kann stoeren. | §5 |
| 9 | ⭐ Laufwerke fehlen ganz, aber Ping+Port 445 OK & manuelles `net use` klappt | Auto-Reconnect-Gate prueft falschen Port (fremder Dienst statt SMB **445**) → kommt nie bis zum Mapping. Gate IMMER auf 445; Skript-Log lesen (Fehler sonst geschluckt). | §5 |
| 10 | ⭐⭐ Reconnect-Skript laeuft als geplante Aufgabe (elevated/hidden) → `net use` HAENGT ewig, Laufwerke nie da | `net use ... /persistent:yes` OHNE explizite Credentials promptet im **elevated** Token (Tresor token-getrennt!) interaktiv nach dem Benutzernamen → im hidden/wscript-Kontext kein Eingeber → Endlos-Hang + net.exe-Prozess-Leak. FIX: per **WNetAddConnection2 (mpr.dll) mit EXPLIZITEN Credentials** mappen (ohne `CONNECT_INTERACTIVE` → nie ein Prompt). | §9 |
| 11 | ⭐⭐ Geplante Aufgabe (RunLevel Highest) mappt erfolgreich, aber Explorer zeigt nichts | UAC-Token-Isolation: im **elevated** Token gemappte Netzlaufwerke sind im **nicht-elevated** Explorer unsichtbar. FIX: `EnableLinkedConnections=1` (HKLM\…\Policies\System, DWORD) → wirksam ab naechstem Login. Wirkt NICHT bei UAC "Prompt for credentials" (→ "Prompt for consent") und NICHT fuer Dienste (UNC nutzen). **Sauberer:** gleich NICHT-elevated mappen (Microsoft `MapDrives.ps1`-Linie). | §10 |
| 13 | ⭐⭐ Nach Reboot hat EIN Laufwerk (von mehreren vom selben VPS) ein rotes X; Klick → "mehrere Benutzernamen nicht zulaessig" (**1219**), Skript-Log sagt aber "reconnect OK" | **Persistent-Login-Race:** persistente `HKCU:\Network`-Mappings → Windows reconnectet beim Login VOR dem Tunnel → totes Mapping im nicht-elevated Token, kollidiert mit dem (elevated) Skript-Mapping. FIX: persistente `HKCU:\Network`-Eintraege ENTFERNEN; Reconnect-Skript mappt **NICHT-persistent** (Flag 0) als einzige tunnel-bewusste Quelle; bei 1219 alle `10.8.0.1`-Sitzungen abraeumen + neu. Sofort von Hand: `net use Z: /delete /yes` + neu verbinden. | §11 |
| 12 | `.ps1` startet nicht / Parse-Fehler "schliessende } fehlt", nur via geplanter Aufgabe | **Windows PowerShell 5.1** liest `.ps1` OHNE BOM als **cp1252** → ein Em-Dash (—) in einem String wird zu `â€"` und zerschiesst Quotes/Klammern. FIX: `.ps1` als **UTF-8 mit BOM** speichern UND keine typografischen Zeichen (Em-Dash/Smart-Quotes) im Code. | §9 |
| 7 | "Brauche ich `ip_forward`/NAT fuer SMB ueber WireGuard?" | **NEIN** — der Dienst laeuft AUF dem VPS, an `wg0` gebunden → lokale Zustellung, kein Forwarding noetig (siehe `wireguard.md` §1). | §1 |
| 8 | ⭐ Samba 4.19.x ungepatcht? (Upstream EOL) | Upstream-4.19-Zweig ist **EOL** (letzter Upstream-Fix 4.19.1). Auf Ubuntu 24.04 kommen Security-Fixes NUR per **`apt`/USN** ins `2:4.19.5+dfsg-…ubuntuX.Y`-Paket → **`unattended-upgrades` aktivieren** bzw. regelmaessig `apt upgrade`. NICHT auf den Upstream-Versionsstring schauen. | §8 |
| 14 | ⭐⭐ macOS: Laufwerke weg; Tunnel pingt mit Verlust, aber Internet + public VPS-IP 0 % & Server gesund | Client-IP/NAT hat gewechselt (dynamische Leitung) → WireGuard-Endpoint veraltet → Mounts fallen ab. ERST Endpoint-Wechsel pruefen (`tail /var/log/wg-endpoint-monitor.log`), NICHT Mac/Server verdaechtigen. FIX: Stale-Mount-Erkennung (perl-alarm) + `/etc/nsmb.conf` soft + Endpoint-Monitor + Keepalive 15. | §13 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/samba-wireguard.md`) |
|----------------------|-----------------------------------------------------------|
| §1 Interface-Bindung | §1 smb.conf-Grundgeruest (Bind an VPN-IP) |
| §2 UFW/Ports | §2 Firewall nur ueber wg0 |
| §3 MTU/Performance | §3 Performance-Tuning |
| §14 Durchsatz/Parallelitaet | §14 rclone statt Finder |
| §4–§6 Windows-Client | §4 Windows-Mount (persistent, Credentials) |
| §9 net use haengt (elevated/hidden) | §7 Auto-Reconnect-Task robust (WNetAddConnection2 + explizite Credentials) |
| §10 EnableLinkedConnections | §7 Auto-Reconnect-Task robust (elevated Mappings sichtbar machen) |
| §11 Persistent-Login-Race / 1219 | §7 Auto-Reconnect-Task robust (nicht-persistent mappen, Skript als einzige Quelle) |

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
**FIX (funktionserhaltend):** In `/etc/samba/smb.conf` im `[global]`-Block die **konkrete Host-IP des
Tunnels mit Maske** angeben (NICHT die Netz-Adresse `…0/24`, NICHT nur den Interface-Namen):
```ini
[global]
   interfaces = lo 10.8.0.1/24
   bind interfaces only = yes
```
**⭐ Live verifiziert 2026-06-23 (eigener Vorfall beim Gehirn-Server):** Mit der **Netz-Adresse**
`interfaces = lo 10.8.0.0/24` lauschte `smbd` NUR auf `127.0.0.1:445` — NICHT auf `10.8.0.1`
(`ss -tlnp | grep :445` zeigte nur lo). Erst die **konkrete Host-IP** `interfaces = lo 10.8.0.1/24`
brachte `smbd` dazu, auf `10.8.0.1:445` zu lauschen. Grund: Bei einem POINTOPOINT/NOARP-Interface
(`ip link show wg0` → `<POINTOPOINT,NOARP,…>`) matcht Samba die Netz-CIDR nicht zuverlaessig gegen die
lokale Interface-IP. Also IMMER die **exakte `10.8.0.1/24`** statt `10.8.0.0/24`. `eth0` kann man hier
sogar weglassen (SMB soll ohnehin nur im Tunnel + lokal lauschen → strenger + sicherer, smbd lauscht dann
gar nicht erst auf der oeffentlichen IP).
Alternative (wenn die Bindung weiter zickt): `bind interfaces only = no` — dann lauscht Samba auf allen
Interfaces, und die Zugriffs-Beschraenkung kommt allein ueber die Firewall (§2). Pruefen mit
`ip link` (Interface-Flags) und `ss -tlnp | grep :445` bzw. `netstat -tulpen | grep smbd` (lauscht es auf 10.8.0.1?).
**Quelle:** Unix StackExchange (Samba over WireGuard) · eigene Recherche 2026-06-22 · **live verifiziert 2026-06-23**.

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
- **Persistentes Mapping:** **`net use Z: \\10.8.0.1\share "PASS" /user:sambauser /persistent:yes`** ist der
  **zuverlaessige** Weg. ⭐ **Live verifiziert 2026-06-23:** `New-SmbMapping -LocalPath Z: -RemotePath … -UserName …
  -Password … -Persistent $true -SaveCredentials` warf **„Falscher Parameter"** (die Kombination
  `-UserName/-Password` + `-Persistent $true` wird abgelehnt) → auf `net use` zurueckgefallen, das klappte sofort.
  Wer `New-SmbMapping` nutzen will: vorher `cmdkey /add:10.8.0.1 /user:sambauser /pass:…` setzen und OHNE
  `-Password`/`-UserName` mappen. NICHT `New-PSDrive -Persist` (laut MS-Doku nicht fuer SMB gedacht). Vorher alte
  Sessions: `net use \\10.8.0.1 /delete`.
- **Freien Laufwerksbuchstaben pruefen (sonst Systemfehler 85):** `net use Y: …` scheitert mit
  **„Systemfehler 85 — der lokale Geraetename wird bereits verwendet"**, wenn `Y:` schon belegt ist (echte
  Partition/USB/anderes Netzlaufwerk). Vor dem Mappen mit `Get-PSDrive -PSProvider FileSystem` bzw.
  `Get-Volume | ? DriveLetter` die freien Buchstaben pruefen — NICHT annehmen, ein „sprechender" Buchstabe sei frei
  (live 2026-06-23: `G:` war Franks Steam-Platte). Test schrieb sonst still auf das FALSCHE lokale Laufwerk.
- **Netzwerk beim Login noch nicht bereit:** rotes X bis Klick. GPO „Computer immer auf das Netzwerk
  warten lassen" aktivieren; sicherstellen, dass der WireGuard-Tunnel beim Login schon steht.
- **PIN-Login (Win 11 Pro)** kann gespeicherte Mappings stoeren → mit Microsoft-Konto-Passwort anmelden.
- **VPN-Credentials ueberschreiben SMB-Credentials** (wenn VPN-User == AD-User mit anderem Passwort):
  in der VPN-`.pbk` `UseRasCredentials=1` → `0` setzen, oder GPO „Speicherung von Passwoertern für
  Netzwerkauthentifizierung nicht zulassen".
- ⭐ **Auto-Reconnect-Gate prueft den FALSCHEN Port (live 2026-06-24):** Ein Reconnect-Skript (geplante
  Aufgabe, mappt Z:/Y: nach Login/Standby) hatte einen TCP-Gate-Check „ist der Tunnel oben?" gegen Port
  **8000 (brain-api)** statt gegen den **SMB-Port 445**. Folge: Beim Neustart von brain-api (Deploy) war 8000
  kurz weg → der Gate schlug an → das Skript kam NIE bis zur Mapping-Schleife (Log endete jedes Mal bei
  „Status: Running", KEIN „reconnect"/„ist OK") → Z:/Y: fehlten komplett im Explorer, obwohl `ping 10.8.0.1`
  UND `Test-NetConnection -Port 445` erfolgreich waren und ein manuelles `net use Z: \\10.8.0.1\gedanken
  /persistent:yes` SOFORT klappte. **Regel:** Der Gate-Check eines Laufwerks-Reconnects MUSS genau den Port
  testen, von dem die Laufwerke abhaengen (SMB **445**) — NIE einen fremden Dienst (brain-api, Web-App), dessen
  Neustart sonst das Mapping blockiert. Zusaetzlich robust: `$c.Connected` pruefen (nicht nur `.Wait()`-Rueckgabe)
  und den Socket im `finally` disposen. Diagnose-Schluessel war das Log des Skripts (Fehler werden sonst mit
  `2>$null`/`SilentlyContinue` geschluckt). **FIX:** `wg-drive-reconnect.ps1` `WgUp()` → Port 445 (#47132).
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

**Belegte Ubuntu-USNs/CVEs fuer das noble-Paket (Eskalations-Recherche 2026-06-22):**
| USN | Paketversion nach Update | CVEs |
|-----|--------------------------|------|
| **USN-7826-1** | `2:4.19.5+dfsg-4ubuntu9.4` | CVE-2025-9640, CVE-2025-10230 (*critical*) |
| **USN-8306-1** | `2:4.19.5+dfsg-4ubuntu9.6` | CVE-2026-2340, CVE-2026-3012, CVE-2026-3238, CVE-2026-4408, CVE-2026-4480 |
Die **aktuell gepatchte noble-Version ist `2:4.19.5+dfsg-4ubuntu9.6`** (USN-8306-1) — das ist der Soll-Stand; mit
`apt-cache policy samba` gegen das eigene System abgleichen. (Eine vollstaendige USN-Liste seit 2024 war nicht
ermittelbar — es koennen aeltere USNs existieren; `unattended-upgrades` deckt sie automatisch ab.)
**Quelle:** ubuntu.com/security/notices USN-7826-1 + USN-8306-1, samba.org/samba/history (4.19 nicht mehr in Upstream-Security-Releases), vulners.com (USN-8306-1) · Recherche 2026-06-22 (Firecrawl + OpenRouter-Eskalation).

## 9. ⭐⭐ `net use` haengt EWIG im elevated/hidden Reconnect-Task (DER Reboot-Bug, live 2026-06-24)
**Symptom:** Ein Reconnect-Skript als **geplante Aufgabe** (RunLevel Highest, unsichtbar via `wscript`/`powershell -WindowStyle Hidden`)
mappt die Laufwerke nach Neustart NIE. Im Skript-Log steht nur "WireGuard-Dienst Status: Running", danach nichts mehr.
`ping 10.8.0.1` UND `Test-NetConnection -Port 445` sind erfolgreich, der Tresor hat sogar einen Eintrag fuer `10.8.0.1`,
und ein **manuelles** `net use` im normalen Fenster klappt sofort. Es sammeln sich mit jedem Trigger haengende `net.exe`-Prozesse
an (Prozess-Leak; 11 Stueck gemessen). Erst `Stop-Process` auf die `net.exe` loest den Hang auf und das Log zeigt dann:
`Z: reconnect FEHLGESCHLAGEN: Geben Sie den Benutzernamen fuer "10.8.0.1" ein:`.
**Ursache (zwei Schichten):**
1. **Token-getrennter Credential-Tresor:** Die Aufgabe laeuft **elevated** (Highest). Ein im normalen (nicht-elevated) Kontext
   gespeicherter Credential `Domain:target=10.8.0.1` ist im **elevated** Token NICHT sichtbar (UAC-Token-Isolation gilt auch fuer
   den Credential-Manager). `net use ... /persistent:yes` ohne explizite Credentials findet also keinen Tresor-Eintrag.
2. **Prompt im fensterlosen Kontext = Endlos-Hang:** Ohne Credential will `net use` INTERAKTIV nach dem Benutzernamen fragen.
   Im `wscript`/Hidden-Kontext gibt es kein Eingabefenster und kein Konsolen-Stdin → der Prompt wird nie beantwortet → der
   `net.exe`-Prozess haengt unbegrenzt (der `/net use /delete` davor laeuft durch, weil er keine Credentials braucht).
   Interaktiv (nicht-elevated) trat der Bug NICHT auf (Tresor sichtbar) → er war im normalen Test unsichtbar.
**Versionen:** Windows 10/11, jede geplante Aufgabe mit RunLevel Highest, die `net use` ohne explizite Credentials nutzt.
**FIX (funktionserhaltend, Poka-Yoke Stufe 3 — Prompt KONZEPTIONELL unmoeglich):** Mappen NICHT per `net use`, sondern per
**`WNetAddConnection2` (mpr.dll)** mit **explizit uebergebenen** Credentials (User + Passwort aus einer Datei AUSSERHALB des Repos,
hier `~/SK/second-brain/samba.env`). Ohne das Flag `CONNECT_INTERACTIVE` kann diese API NIEMALS einen Dialog oeffnen — sie liefert
stattdessen einen Win32-Fehlercode (z.B. 1326 = falsche Credentials). Kein Prompt, kein Hang, kein Prozess-Leak — unabhaengig von
Token/Tresor. `CONNECT_UPDATE_PROFILE` (0x1) macht das Mapping persistent. Kurzform in PowerShell:
```powershell
Add-Type @'
using System;using System.Runtime.InteropServices;
public class Mpr{
 [StructLayout(LayoutKind.Sequential,CharSet=CharSet.Unicode)] public struct NETRESOURCE{
  public int dwScope;public int dwType;public int dwDisplayType;public int dwUsage;
  public string lpLocalName;public string lpRemoteName;public string lpComment;public string lpProvider;}
 [DllImport("mpr.dll",CharSet=CharSet.Unicode)] public static extern int WNetAddConnection2(ref NETRESOURCE r,string pw,string usr,int f);
 [DllImport("mpr.dll",CharSet=CharSet.Unicode)] public static extern int WNetCancelConnection2(string n,int f,bool force);}
'@
[Mpr]::WNetCancelConnection2('Z:',0,$true) | Out-Null
$nr = New-Object Mpr+NETRESOURCE; $nr.dwType=1; $nr.lpLocalName='Z:'; $nr.lpRemoteName='\\10.8.0.1\gedanken'
$code = [Mpr]::WNetAddConnection2([ref]$nr, $pass, $user, 0x1)   # 0x1 = CONNECT_UPDATE_PROFILE (persistent)
```
**Falls man doch `net use` nehmen will (weniger robust):** IMMER mit explizitem Passwort + `/user:` aufrufen
(`net use Z: \\10.8.0.1\gedanken "$pass" /user:frank /persistent:yes`) — dann promptet es nicht. Trotzdem kann ein
falsch konfiguriertes net use haengen; ein harter Timeout-Guard ist Pflicht. WNetAddConnection2 ist vorzuziehen.
**Encoding-Falle (gleicher Vorfall):** Das per geplanter Aufgabe gestartete Skript lief mit **Windows PowerShell 5.1**
(`powershell.exe`), das `.ps1` OHNE BOM als **cp1252** liest. Ein **Em-Dash (—)** in einem String wurde zu `â€"` und zerschoss
die Quote-/Klammer-Logik → Parse-Fehler "schliessende } fehlt" → das Skript startete GAR NICHT (kein Log). Der PowerShell-7-Parser
(`PSParser`) meldete "Syntax OK" — die Falle. **Regel:** `.ps1`-Skripte als **UTF-8 mit BOM** speichern und **keine typografischen
Zeichen** (Em-Dash, Smart-Quotes) im Code verwenden (ASCII-only ist am sichersten).
**Quelle:** Eigener Vorfall + Live-Diagnose 2026-06-24 (Trace-Sonden zeigten Hang exakt bei `net use ... /persistent:yes`).

## 10. ⭐⭐ Im elevated Task gemappte Laufwerke sind im Explorer UNSICHTBAR (EnableLinkedConnections)
**Symptom:** Die geplante Aufgabe (RunLevel Highest) mappt Z:/Y: erfolgreich (`Get-SmbMapping` im elevated Kontext zeigt Status OK),
aber Franks **Explorer** (laeuft nicht-elevated) zeigt die Laufwerke NICHT. `Get-SmbMapping` im normalen Fenster ist leer.
**Ursache:** **UAC-Token-Isolation fuer Netzlaufwerke.** Ein elevated und ein nicht-elevated Prozess desselben Benutzers haben
GETRENNTE Laufwerks-Namespaces. Was die elevated Aufgabe mappt, existiert nur im elevated Token; der nicht-elevated Explorer
sieht es nicht (und umgekehrt). Das ist by-design, nicht behebbar durch besseres Mapping.
**Versionen:** Windows 10/11 mit aktivem UAC (Standard).
**FIX (Microsoft-dokumentiert):** Registry-Wert setzen, der die beiden Token verknuepft:
```
HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System
  EnableLinkedConnections = 1   (DWORD)
```
Setzen braucht Admin-Rechte (die elevated Aufgabe kann es selbst idempotent tun). **Wirksam erst ab dem naechsten Login/Reboot.**
Danach sehen elevated- und nicht-elevated-Prozesse dieselben Mappings. **Achtung:** Verknuepft bewusst die Token — auf einem
privaten Single-User-PC unkritisch, in Hochsicherheitsumgebungen abwaegen.
**Alternative (ohne Registry-Tweak):** Das Mapping im **nicht-elevated** Kontext durchfuehren (z.B. eine zweite, nicht-erhoehte
Aufgabe nur furs Mapping; die erhoehte nur fuer den Dienst-Start). Mehr Aufwand, dafuer ohne token-uebergreifende Verknuepfung.
**⭐ Ergaenzung (Recherche 2026-06-25, Microsoft KB 3035277) - wann `EnableLinkedConnections=1` TROTZDEM nicht wirkt:**
Steht die UAC-Richtlinie *"Behavior of the elevation prompt for administrators in Admin Approval Mode"* auf
**"Prompt for credentials"** (Passwort-Abfrage) statt "Prompt for consent", erzeugt eine Erhoehung eine **DRITTE**
Logon-Session, in der die symbolischen Laufwerks-Links NICHT vorhanden sind → die Mappings bleiben im erhoehten
Kontext unsichtbar, obwohl `EnableLinkedConnections=1`. **FIX:** UAC auf **"Prompt for consent"** stellen. Zudem:
`EnableLinkedConnections` gilt **NICHT fuer Windows-Dienste** — die muessen Netzwerkressourcen ueber **UNC-Pfade**
(`\\server\share`) ansprechen, nicht ueber Laufwerksbuchstaben. `gpupdate /force` macht den Wert ohne Neustart wirksam.
**Quelle:** Microsoft KB (EnableLinkedConnections / KB 3035277) + ServerFault + eigener Vorfall 2026-06-24 (live verifiziert: ohne den Wert 0 Laufwerke im
nicht-elevated Get-SmbMapping trotz "OK" im elevated Task) + Recherche 2026-06-25 (Firecrawl+MiniMax).

## 11. ⭐⭐ "Mehrere Benutzernamen" (Fehler 1219) bei mehreren Shares vom SELBEN VPS - der Persistent-Login-Race (live 2026-06-25)
**Symptom:** Mehrere Netzlaufwerke zeigen auf denselben Server (`\\10.8.0.1\daten` = Y:, `\\10.8.0.1\gedanken` = Z:). Nach
**jedem Neustart** ist EINES verbunden (Y: OK), das andere hat ein **rotes X** ("Nicht verfuegbar"). Klick darauf:
*"Mehrfache Verbindungen zu einem Server oder einer freigegebenen Ressource von demselben Benutzer unter Verwendung
mehrerer Benutzernamen sind nicht zulaessig"* (Win32 **1219**, ERROR_SESSION_CREDENTIAL_CONFLICT). Das Reconnect-Skript-Log
zeigt aber **"reconnect OK" fuer BEIDE** - laut Skript sind also beide verbunden, im Explorer trotzdem das X. Manuelles
`net use Z: /delete` + neu verbinden klappt sofort (war der wiederkehrende Symptom-Fix ueber "mehrere Sessions" hinweg).
**Ursache (zwei Schichten):**
1. **Persistent-Login-Race:** Y:/Z: stehen als PERSISTENTE Mappings in `HKCU:\Network`. Windows verbindet persistente
   Netzlaufwerke SOFORT beim Login - der WireGuard-Tunnel ist da aber noch nicht oben -> die Verbindung scheitert -> ein
   TOTES "Nicht verfuegbar"-Mapping bleibt im **nicht-elevated** Token (Franks Explorer) zurueck.
2. **Ein-Credential-pro-Server-Regel + Token-Trennung:** Windows erlaubt pro Server nur EINE Sitzung/EINEN Credential-Satz.
   Das Reconnect-Skript (elevated Task) mappt danach korrekt im **elevated** Token - aber das tote nicht-elevated Mapping
   bleibt bestehen. Beim Anklicken kollidieren die zwei Sitzungen zum selben Server `10.8.0.1` -> 1219. `EnableLinkedConnections=1`
   (§10) verknuepft zwar die Token, raeumt aber das bereits entstandene TOTE Login-Mapping NICHT ab.
**Beweis (live 2026-06-25):** Skript-Log `10:35:21 Z: reconnect OK` UND `Y: reconnect OK`, trotzdem sah Frank Z: mit rotem X;
ein nicht-elevatetes `net use` zeigte Z: als "Nicht verfuegbar" - also Token-getrennte Sicht auf dasselbe Laufwerk.
**Versionen:** Windows 10/11, jedes Setup mit >=2 persistenten Shares vom selben Server (typisch ueber VPN/WireGuard).
**FIX (funktionserhaltend, Poka-Yoke Stufe 3 - Race konzeptionell unmoeglich):**
1. **Persistente `HKCU:\Network`-Eintraege der betroffenen Laufwerke ENTFERNEN** (`Remove-Item HKCU:\Network\Z -Recurse -Force`),
   damit Windows beim Login GAR NICHT mehr voreilig (vor dem Tunnel) verbindet -> kein totes Mapping mehr.
2. Das tunnel-bewusste Reconnect-Skript wird die **EINZIGE** Mapping-Quelle und mappt **NICHT-persistent**
   (`WNetAddConnection2` mit Flag **0** statt `CONNECT_UPDATE_PROFILE 0x1`) - so entsteht kein neuer HKCU-Eintrag, der das
   Race beim naechsten Login reproduzieren wuerde. Das Skript laeuft bei Login + alle 5 Min und mappt erst, wenn Port 445 oben ist.
3. **1219 explizit abfangen:** Tritt der Konflikt doch auf, ALLE Sitzungen zum Server hart abraeumen
   (`WNetCancelConnection2` auf jeden Laufwerksbuchstaben + den nackten Server-UNC `\\10.8.0.1` + `\\10.8.0.1\IPC$`),
   dann mit den expliziten Credentials neu mappen. Das ist exakt der manuelle `net use \\10.8.0.1 /delete`-Fix in Skriptform.
**Soforthilfe von Hand (akutes rotes X):** `net use Z: /delete /yes` dann `net use Z: \\10.8.0.1\gedanken /persistent:yes`
(nutzt den Credential-Manager-Eintrag, baut die Sitzung sauber neu auf). ACHTUNG: das `/persistent:yes` legt wieder einen
HKCU-Eintrag an -> fuer die DAUERHAFTE Loesung das Skript laufen lassen (raeumt ihn beim naechsten Lauf weg).
**Abgrenzung zu §9/§10:** §9 = `net use` haengt (Prompt im hidden Task); §10 = elevated Mapping im Explorer unsichtbar
(EnableLinkedConnections); §11 = das Mapping klappt, aber ein TOTES persistentes Login-Mapping kollidiert (1219). Die drei
zusammen ergeben den robusten Reconnect (`second-brain-server/windows/wg-drive-reconnect.ps1`).
**Quelle:** Eigener Vorfall + Live-Diagnose 2026-06-25 (Skript-Log bewies "reconnect OK" trotz rotem X -> Token-getrennte Sicht; Fix #47187).

## 12. ⭐ macOS-Client: Freigaben einbinden (live verifiziert 2026-06-29)
**Kontext:** Den Gehirn-Server (`10.8.0.1`, Shares `gedanken`=Z, `daten`=Y) auf einem Mac dauerhaft
im Finder einbinden — das macOS-Pendant zum Windows-`net use`/`wg-drive-mount.ps1`.
**Erkenntnisse (live 2026-06-29):**
- **WireGuard-Tunnel:** `wg-quick` via Homebrew, conf in `~/SK/second-brain/wireguard/…conf`. Start
  braucht `sudo` (`wg-quick up <conf>`). Split-Tunnel (`AllowedIPs = 10.8.0.0/24`) → andere VPNs
  (utun0–3) bleiben unberührt; der Gehirn-Tunnel kommt als eigenes `utun` mit `inet 10.8.0.2`.
- **Mount ohne sudo (der Trick):** `/Volumes` ist auf macOS `drwxr-xr-x root` → `mkdir /Volumes/x`
  und `mount_smbfs //…/x /Volumes/x` brauchen **root**. Stattdessen über **automountd**:
  `osascript -e 'mount volume "smb://user:pass@10.8.0.1/gedanken"'` (oder `open smb://…`) — legt
  `/Volumes/gedanken` selbst an, **kein sudo**, erscheint im Finder unter „Speicherorte". `open` ist
  async + im Skript-Kontext unzuverlaessig (legt manchmal nur ein leeres `/Volumes/x` an) →
  `osascript mount volume` ist der scriptbare Weg.
- **Passwort nicht auslesbar → eigener Mac-User statt Reset:** Das Samba-Passwort steht nur gehasht
  in `passdb.tdb`, ist NICHT wiederherstellbar und in keinem Skript. Das `frank`-Passwort neu zu
  setzen wuerde den **Windows-Mount brechen**. FIX (funktionserhaltend, Direktive #3): **separaten
  Samba-User pro Plattform** anlegen — `useradd -M -N -g frank -s /usr/sbin/nologin frankmac` (primäre
  Gruppe `frank` → Schreibrechte via `0775`/`0664`), `smbpasswd -s -a frankmac`, `smbpasswd -e`, in
  BEIDEN Shares `valid users = frank` → `frank frankmac`, `systemctl reload smbd`. Windows (`frank`)
  bleibt unberührt.
- **SSH-Henne-Ei (neuer Client):** Ein frisch erzeugter Mac-SSH-Key ist am Server noch nicht in
  `/root/.ssh/authorized_keys` → `Permission denied (publickey)`, auch über den Tunnel
  (`Match`-unabhängig). PasswordAuth ist aus. Lösung ohne den schon-autorisierten Rechner:
  **Hostinger-Browser-Konsole** (root) → Pubkey in `/root/.ssh/authorized_keys` eintragen.
- **Gate auf Port 445** (wie Windows §5): Auto-Mount-Skript erst mounten, wenn `nc -z 10.8.0.1 445`
  klappt — nie gegen einen Dienst-Port (brain-api 8000) prüfen.
- **Persistenz:** LaunchDaemon (root, Boot) für den Tunnel + LaunchAgent (user, Login + `StartInterval 300`)
  für den Mount. Setup: `second-brain-server/macos/` (`wg-drive-mount.sh`, `wireguard-up.sh`,
  `setup-macos.sh`, zwei `.plist`). **Anker:** macOS 15+, `wireguard-tools` (Homebrew), SMB 3.1.1.
**Quelle:** Eigener Vorfall + Live-Einrichtung 2026-06-29.

---

## 13. ⭐⭐ macOS: Laufwerke verschwinden nach Client-IP-Wechsel (Endpoint-Flapping, live 2026-06-29)
**Symptom:** Die Gehirn-Laufwerke (gedanken/daten) sind ploetzlich aus dem Finder weg. `ping 10.8.0.1`
zeigt teils **hohen Paketverlust** (60 %), Port 445 mal offen, mal zu — aber `ping 1.1.1.1` und der Ping
zur **public VPS-IP** sind 0 % Verlust. Auf dem VPS ist NICHTS auffaellig.
**Root Cause (live diagnostiziert):** Der Paketverlust entsteht NICHT auf dem Server (per `sar` belegt:
CPU idle 98 %, `eth0` 0 Fehler/Drops, kein OOM, kein Lastspike) und NICHT auf dem lokalen Internet —
sondern auf der Strecke Mac↔VPS, weil die **oeffentliche Client-IP/das NAT gewechselt hat** (dynamische
Leitung, z. B. Telekom). Der Endpoint-Monitor belegte einen Wechsel `109.41.115.177:4740` →
`62.23.250.218:59175` in ~40 Min. Nach so einem Wechsel kennt der Server kurz nur den **alten** Endpoint
→ Server→Client-Pakete verloren, bis das Client-Keepalive den neuen Endpoint etabliert. In dieser Zeit
wirft macOS die SMB-Mounts ab; das alte Mount-Skript meldete dann blind „bereits gemountet — ok" und
wartete bis zu 5 Min.
**Diagnose-Reihenfolge (NICHT den Mac/Server verdaechtigen):** (1) `ping -c 10 10.8.0.1` Verlust? (2)
`ping 1.1.1.1` + Ping zur public VPS-IP — beide 0 %? → Problem nur im Tunnel. (3) `wg show` Server:
`latest handshake` frisch + `transfer` laeuft, aber Verlust → Endpoint/Strecke. (4) `sar -u/-q/-n EDEV`
auf dem VPS → Server gesund? → Ursache ist der Client-Endpoint-Wechsel.
**FIX (funktionserhaltend, Fehlerklasse statt Symptom — Direktive #3):**
- **Stale-Mount-Erkennung** in `wg-drive-mount.sh`: aktive Leseprobe mit hartem Timeout (perl-alarm 4 s,
  da macOS kein `timeout` hat) auf jeden gemounteten Share; toter Mount → `diskutil unmount force` +
  sofort neu mounten, statt „bereits gemountet" zu glauben.
- **SMB soft mounts** via **`/etc/nsmb.conf`** (`soft=yes`, `notify_off=yes`, `max_resp_timeout=30`):
  Finder friert bei Aussetzer nicht ein; toter Mount haengt nicht ewig. WICHTIG: die user-
  `~/Library/Preferences/nsmb.conf` greift NICHT fuer automountd-Mounts (laufen als root) → `/etc/nsmb.conf`.
- **VPS-Endpoint-Monitor** (`wg-endpoint-monitor.{sh,service,timer}`, systemd-Timer 30 s): loggt jeden
  Endpoint-Wechsel nach `/var/log/wg-endpoint-monitor.log` → naechster Vorfall ist beweisbar.
- **`PersistentKeepalive = 25 → 15`** (Client-conf): schnellere NAT-Heilung nach IP-Wechsel.
- `mount_smbfs` als Mount-Weg scheidet aus (braucht sudo fuer `/Volumes`-mkdir + nicht im Finder) — der
  `osascript mount volume`-Weg aus §12 bleibt; die Haertung kommt aus nsmb.conf, nicht aus dem Mount-Befehl.
**Quelle:** Eigener Vorfall + Live-Diagnose 2026-06-29 (commit #47317).

---

---

## 14. ⭐⭐ SMB "extrem langsam" ueber den Tunnel — es kommt auf die DATEIGROESSE an (live gemessen 2026-08-27)

**Symptom:** Kopieren auf die Tunnel-Laufwerke dauert ewig; bei vielen kleinen Dateien brechen
Transfers zusaetzlich mit `Operation timed out` / `Not a directory` ab.

**Messaufbau:** macOS-Client -> Hostinger-VPS Paris ueber WireGuard. Leitung 60 Mbit/s hoch /
114 Mbit/s runter, Grundlatenz **48 ms** (bei leerer Leitung, mit `ping` gegengeprueft).

### Der entscheidende Unterschied: kleine vs. grosse Dateien

| Fall | Finder/SMB | rclone (parallel) | Faktor |
|------|-----------|-------------------|--------|
| **200 Dateien x 50 KB** (Fotos, Dokumente) | **1,5 Dateien/s** — und brach mit Timeouts ab | **29,8 Dateien/s** (64 gleichzeitig) | **rund 20x** |
| **Wenige grosse Dateien** (24 MB, leere Leitung) | 33,1 Mbit/s | 36,4 Mbit/s | 1,1x (egal) |

**Root Cause:** SMB arbeitet pro Datei-Handle **seriell** — jede Operation (Open, Write, Close,
Metadaten) wartet auf die Antwort der Gegenseite. Der Engpass ist damit die **Latenz, nicht die
Bandbreite**:

* Bei **vielen kleinen** Dateien dominieren die Round-Trips. 200 Dateien x mehrere Round-Trips
  x 48 ms = Minuten, waehrend die Leitung praktisch leer bleibt. Nur **viele gleichzeitige**
  Uebertragungen helfen.
* Bei **wenigen grossen** Dateien amortisiert sich der Round-Trip ueber viele MB Nutzdaten —
  hier ist die Leitung ohnehin der Engpass, und SMB ist voellig in Ordnung.

### Wie stark Parallelitaet bei kleinen Dateien wirkt (gemessen, 200 x 50 KB)

| gleichzeitige Uebertragungen | 8 | 32 | 48 | 64 | 128 |
|------------------------------|---|----|----|----|-----|
| Dateien/s | 6,2 | 21,0 | 26,2 | **29,8** | 38,6 |

Ab etwa 64 flacht der Gewinn ab und die Messwerte schwanken. **64 ist der empfohlene Wert** —
mehr belastet den (kleinen) VPS ohne verlaesslichen Zusatznutzen.

**FIX (funktionserhaltend — der SMB-Mount bleibt, es kommt ein schneller Weg DANEBEN):**
`second-brain-server/macos/cortex-copy.sh` bzw. `windows/cortex-copy.ps1`. Die Skripte waehlen
das Profil **automatisch** anhand der mittleren Dateigroesse:

```bash
# unter 2 MB Schnitt -> latenzgebunden -> massiv parallel:
rclone copy <quelle> <ziel> --transfers 64 --checkers 64 --buffer-size 32M
# darueber -> Leitung ist der Engpass -> wenige Transfers, dafuer Datei aufteilen:
rclone copy <quelle> <ziel> --transfers 8 --checkers 8 \
  --multi-thread-streams 8 --multi-thread-cutoff 16M --buffer-size 32M
```

`--multi-thread-streams` bei grossen Dateien nicht vergessen: ohne den Schalter bleibt EINE
grosse Datei bei rund 25 Mbit/s, weil auch dort serialisiert wird.

### ⚠️ Messfalle, die zuerst zur Fehldiagnose fuehrte

Die ersten Messungen dieser Session entstanden, waehrend im Hintergrund ein grosser Download
lief. Der saettigte die Leitung und trieb per **Bufferbloat** die Latenz hoch — **48 ms -> 218 ms**
(Spitzen 300 ms), Responsiveness 1,2 s. Unter diesen Bedingungen brach SMB auf **5,3 Mbit/s** ein
(rclone hielt 42,6 Mbit/s), und ein 50-MB-`cp` starb nach 14 s an `max_resp_timeout=30`
(siehe `nsmb.conf`). Daraus entstand faelschlich der Schluss "SMB ist generell 8x langsamer".

**Regel daraus:** Vor JEDER Durchsatzmessung die Leitung leerraeumen und das mit
`netstat -ib` (macOS) bzw. dem Ressourcenmonitor gegenpruefen; zusaetzlich `ping` mitlaufen
lassen — steigt die Latenz deutlich ueber den Ruhewert, misst man Bufferbloat, nicht SMB.
Der Nutzen von rclone bleibt unter Last aber real: es haelt durch, wo SMB abbricht.

**Was NICHT die Ursache war (jeweils gegengemessen — spart die naechste Fehldiagnose):**

| Verdacht | Messung | Urteil |
|----------|---------|--------|
| WLAN zu schwach | 802.11ax, 5 GHz, -45 dBm, 816 Mbit/s Linkrate | ❌ nein |
| MTU / Path-MTU-Black-Hole (§3) | `ping -D`: volle 1500 im Netz, Tunnel-MTU 1420 mit Reserve | ❌ nein |
| WireGuard-Overhead | Tunnel kostet rund 19 ms gegenueber der direkten Route | ❌ nein |
| Der Mobilfunk-Anschluss "an sich" | 60/114 Mbit/s bei 48 ms — voellig brauchbar | ❌ nein |
| Doppelte Verschluesselung (SMB AES-GCM **in** WireGuard) | Apple Silicon macht AES mit GB/s | ❌ nein |

**Nebenfund:** Ein per Timeout abgebrochener SMB-Transfer laesst serverseitig ein offenes Handle
zurueck — die betroffene Datei ist danach weder ueber den Mount noch per rclone loeschbar
(`Resource busy` / `share access flags are incompatible`), auch ein Neu-Einbinden des Mounts
loest das nicht. Sie verschwindet erst, wenn Samba die Session serverseitig aufraeumt. Nicht mit
Gewalt nachhelfen.

**Grenze, die kein Tuning aufhebt:** Der Durchsatz ist durch den **Uplink** gedeckelt. Bei
60 Mbit/s dauert **1 TB rund 37 Stunden** — selbst perfekt parallelisiert. Fuer grosse
Erstbefuellungen ist ein physischer Datentraeger schneller als jede Leitung; danach nur noch
Deltas per `rclone sync` schieben.

**Quelle:** eigene Live-Messung 2026-08-27 (macOS 26.6.2, rclone 1.75.0, Samba ueber WireGuard,
VPS Hostinger Paris).

---

## Pflicht-Checkliste vor Samba-ueber-WireGuard
- [ ] `smb.conf`: `interfaces = lo eth0 10.8.0.0/24` (mit Maske!) + `bind interfaces only = yes` (oder `= no`)?
- [ ] `netstat -tulpen | grep smbd` zeigt `smbd` auf `10.8.0.1:445`?
- [ ] UFW: 445/139 NUR `in on wg0`, NICHT oeffentlich? Einziger oeffentlicher Port UDP 51820?
- [ ] `protocol = SMB3` gesetzt (gegen Win11-Auth-Zicken)?
- [ ] Echter Samba-User (`smbpasswd -a`) statt Gastzugriff?
- [ ] Win-Mount persistent via `New-SmbMapping -Persistent` + sauberer Credential-Manager-Eintrag?
- [ ] Bei Langsamkeit: WireGuard-MTU 1350 + MSS getestet?
- [ ] Bei Langsamkeit: erst die Leitung leergeraeumt (Bufferbloat!), dann parallele Streams (`rclone --transfers 8`) statt Finder/Explorer probiert? (§14 - meist DIE Ursache)
- [ ] Kein unnoetiges `ip_forward`/MASQUERADE (Split-Tunnel-Dienst braucht es nicht)?
- [ ] **`unattended-upgrades` aktiv / `apt upgrade` regelmaessig** (4.19.x ist upstream EOL — Fixes nur ueber Ubuntu-Paket, §8)?

---

## Quellen (Stand 2026-06-22)
Firecrawl + MiniMax M3 (quellentreu), 3 Recherche-Laeufe: Unix/Server-Fault/Ask-Ubuntu (Interface-Bind,
UFW), MikroTik-Forum (MTU/SMB-Performance), MS TechCommunity + Win-Admin-Quellen (Guest-Auth, Signing,
persistente Mappings). Die exakte End-to-End-Kombination „Ubuntu 24.04 ↔ WireGuard ↔ Win11-Netzlaufwerk"
war in keiner Einzelquelle vollstaendig — dieser Almanach setzt die belegten Teilaspekte zusammen.
