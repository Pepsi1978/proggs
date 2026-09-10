# Second-Brain-Server ("Cortex") — Komplette Installations- & Portierungs-Anleitung

> **Stand:** 08.07.2026, 22.25 Uhr · Verfasst als plattformübergreifende Komplettanleitung, um den
> Second-Brain-Server (Cortex) und den Zugriff darauf auf **einem anderen Windows-Rechner** und **einem
> anderen macOS-Rechner** genauso stabil einzurichten wie auf dem Haupt-PC — **inklusive aller
> Laufwerk-Fixes**, die im Laufe der Zeit erarbeitet wurden.
>
> Diese Datei liegt bewusst in `claude-code-setup/`, damit sie mit dem Repo automatisch auf jeden neuen
> Rechner mitkommt (`git clone`). Server-Deploy-Details ergänzend in
> `second-brain-server/DEPLOY.md` und `second-brain-server/README.md`.

---

## 0. Das Wichtigste in drei Sätzen

1. Der **Server** ist ein Docker-Compose-Stack, der **einmal** auf dem Hostinger-VPS läuft
   (`/opt/second-brain`). Einen neuen Client-Rechner einrichten heißt **NICHT**, den Server neu zu
   installieren — der läuft schon.
2. Jeder **Client** (Windows/Mac) erreicht den Server **ausschließlich über einen WireGuard-VPN-Tunnel**
   (Server-Tunnel-IP `10.8.0.1`). Ohne Tunnel ist gar nichts erreichbar — der Server ist im Internet
   unsichtbar (nur UDP 51820 offen).
3. „Installieren" auf einem neuen Client = **(A)** WireGuard-Tunnel aufbauen, **(B)** die MCP-Config
   `.mcp.json` setzen (damit Claude Code das Gehirn als Werkzeuge sieht), **(C)** die Netzlaufwerke
   Z:/Y: (Windows) bzw. `gedanken`/`daten` (Mac) mit den Fixes automatisch einbinden.

---

## 1. Architektur-Überblick (was läuft wo)

### Server (VPS, Docker-Compose-Stack `/opt/second-brain`)

| Dienst (compose-Name) | Rolle | Port (an `10.8.0.1`) | selbst gebaut? |
|-----------------------|-------|----------------------|----------------|
| `qdrant` | Vektor-DB / „Such-Schrank" (semantische Suche) | `127.0.0.1:6333` (REST), `6334` (gRPC) | nein (Fertig-Image `v1.18.2`) |
| `brain-api` | **1:1-Dokument-Speicher** (Text rein/raus, kein LLM) | `10.8.0.1:8000` (`/health`) | ja (`--build`) |
| `mcp` (sb-mcp) | **MCP-Server** — macht das Gehirn als Werkzeuge für Claude Code verfügbar | `10.8.0.1:8001` (`/mcp`) | ja (`--build`) |
| `agent` | Bibliothekar-Agent (einordnen + 1:1 speichern + Logbuch) | `10.8.0.1:8002` (`/health`) | ja (`--build`) |
| `dashboard` | privates Web-Cockpit (Dark/Light) | `10.8.0.1:8003` (`/api/health`) | ja (`--build`) |
| `librarian` | Nachtschicht-Bibliothekar (Sleep-Time-Agent) | `10.8.0.1:8004` (`/health`) | ja (`--build`) |
| `caddy` | HTTPS-Proxy fürs Cockpit (secure context fürs Mikrofon) | `10.8.0.1:443` | nein (Fertig-Image `2.11.4-alpine`) |

- **KI im Speicher:** KEINE. Nur **Google Gemini Embeddings** (Cloud, `gemini-embedding-2` @ **3072 dim**)
  zum Wiederfinden. Text geht **wortwörtlich 1:1** rein und raus — kein mem0, kein LLM, keine
  Fakten-Extraktion. Aufbereitung passiert vorher client-seitig.
- **Collection:** `brain__e2` (3072). Die alte 1536er-Collection `brain` wurde am 2026-07-08 entfernt.

### Client → Server (der Zugangsweg)

```
Windows/Mac-Client
   │  WireGuard-Tunnel (UDP 51820 → VPS)        Client-IP 10.8.0.X/32
   ▼
Server-Tunnel-IP 10.8.0.1
   ├── :8001/mcp   → Claude Code MCP  (.mcp.json)         ← das "Gehirn als Werkzeuge"
   ├── :8000..8004 → REST/Dashboard   (nur über Tunnel)
   └── :445 (SMB)  → Netzlaufwerke:  \\10.8.0.1\gedanken (Z:) · \\10.8.0.1\daten (Y:)
```

### Server-Fakten (die „wichtigen Werte")

| Was | Wert |
|-----|------|
| VPS-IP (öffentlich) | `168.231.83.205` (Hostinger) |
| SSH-User / Key | `root` / `~/SK/second-brain/id_ed25519` (passwortlos) |
| App-Verzeichnis auf dem VPS | `/opt/second-brain` (**KEIN git-Repo** — Deploy = scp + `docker compose up -d --build`) |
| Lokale Quelle (Repo) | `~/proggs/second-brain-server/` |
| WireGuard-Server-Interface | `wg0` = `10.8.0.1/24`, öffentlicher Port **UDP 51820** |
| Samba-Freigaben | `/srv/samba/gedanken` (→ Z:), `/srv/samba/daten` (→ Y:) |
| Secrets (NIE im Repo) | `.env` auf dem VPS · Backup + Client-Configs in `~/SK/second-brain/` |

---

## 2. Was im Repo liegt (Datei-Landkarte)

```
second-brain-server/
├─ compose.yaml                 # der komplette Docker-Stack (Server)
├─ DEPLOY.md                    # wie man Server-Änderungen auf den VPS bringt (scp + --build)
├─ README.md                    # Server-Überblick, REST-Endpunkte, WireGuard-Server-Setup
├─ brain-api/  mcp-server/  agent/  librarian/  dashboard/   # die selbst gebauten Dienste
├─ windows/                     # ← Windows-CLIENT: Netzlaufwerke + WireGuard-Tasks
│  ├─ wg-setup-elevated.ps1     #   EINMALIGER Admin-Setup (registriert die 2 geplanten Aufgaben)
│  ├─ wg-drive-mount.ps1        #   PRIMÄRER, nicht-erhöhter Mount (sichtbar im Explorer)
│  ├─ wg-drive-mount.vbs        #   startet den Mount unsichtbar (kein Fenster)
│  ├─ wg-drive-reconnect.ps1    #   ERHÖHTER Backup-Weg (Dienst + EnableLinkedConnections)
│  └─ wg-drive-reconnect.vbs    #   startet den Reconnect unsichtbar
└─ macos/                       # ← macOS-CLIENT: Netzlaufwerke + WireGuard
   ├─ setup-macos.sh            #   Installer (User-Teil + --daemon-Teil)
   ├─ wg-drive-mount.sh         #   Mount mit Stale-Erkennung
   ├─ wireguard-up.sh           #   Tunnel beim Boot (LaunchDaemon)
   ├─ nsmb.conf.vorlage         #   /etc/nsmb.conf-Vorlage (soft mounts)
   └─ de.frank.secondbrain.*.plist   # LaunchAgent (Mount) + LaunchDaemon (Tunnel)

claude-code-setup/
├─ mcp-windows.json             # MCP-Config für Windows  → nach ~/proggs/.mcp.json
├─ mcp-macos.json               # MCP-Config für macOS    → nach ~/proggs/.mcp.json
└─ SECOND-BRAIN-SERVER-INSTALLATION.md   # DIESE Datei
```

**Was NICHT im Repo liegt (Secrets — müssen von Hand übertragen werden):** `id_ed25519` (SSH-Key),
`wireguard/*.conf` (Tunnel-Config mit privatem Schlüssel), `samba.env` (SMB-Login), `.env` (API-Keys).
Alle liegen zentral in `~/SK/second-brain/` (Regel `secrets-in-sk-folder`) und werden per USB-Stick /
verschlüsseltem Transfer auf den neuen Rechner gebracht — **niemals** über Git.

---

## 3. Gemeinsame Vorbereitung (Windows UND macOS)

### 3.1 Repo klonen
```bash
git clone https://github.com/Pepsi1978/proggs.git ~/proggs      # macOS/Linux
# Windows: git clone https://github.com/Pepsi1978/proggs.git C:\Users\<DU>\proggs
```
Damit sind CLAUDE.md, alle Skripte und die MCP-Config-Vorlagen sofort da.

### 3.2 SK-Ordner mit den Secrets anlegen
Struktur auf dem neuen Rechner (Home-Verzeichnis):
```
~/SK/second-brain/
├─ id_ed25519                       # SSH-Key zum VPS (nur nötig, wenn du deployen willst)
├─ wireguard/second-brain.conf      # WireGuard-Client-Config (siehe 3.3)
└─ samba.env                        # SMB-Login (siehe 3.4)
```

Weil der Key **nicht** in `~/.ssh/` liegt, finden ihn Werkzeuge, die nur dort suchen (OpenCode,
Cowork, manche Deploy-Skripte), nicht und melden „kein SSH-Key gefunden". Deshalb auf JEDEM Rechner
`~/.ssh/config` anlegen — damit gilt der Key für alle Projekte auf diesem VPS (Second Brain **und**
Werft Studio, beide `root@10.8.0.1`):

```
Host 10.8.0.1 168.231.83.205
    User root
    IdentityFile ~/SK/second-brain/id_ed25519
    IdentitiesOnly yes
```

Prüfen: `ssh -o BatchMode=yes root@10.8.0.1 'echo OK'` muss ohne `-i` durchlaufen.

### 3.3 Neuen WireGuard-Peer auf dem Server anlegen (PFLICHT bei jedem neuen Rechner)
Jeder Rechner braucht **eine eigene** Tunnel-IP `10.8.0.X` und einen eigenen `[Peer]` auf dem Server.
Auf dem **VPS** (per SSH, als root):
```bash
# 1) Client-Keypair erzeugen
umask 077; wg genkey | tee client_priv | wg pubkey > client_pub
cat client_pub    # <-- diesen PublicKey in /etc/wireguard/wg0.conf eintragen

# 2) In /etc/wireguard/wg0.conf einen neuen Peer ergänzen (freie IP wählen, z.B. .4):
#    [Peer]
#    PublicKey  = <client_pub>
#    AllowedIPs = 10.8.0.4/32

# 3) Server-Tunnel neu einlesen (ohne Verbindungsabbruch der anderen Peers):
wg syncconf wg0 <(wg-quick strip wg0)
```
Merke dir: die **Server-PublicKey**, die **öffentliche VPS-IP** `168.231.83.205:51820` und die
**neue Client-IP** `10.8.0.4` — die brauchst du gleich in der Client-`.conf`.

### 3.4 Client-`.conf` erstellen (`~/SK/second-brain/wireguard/second-brain.conf`)
```ini
[Interface]
PrivateKey = <client_priv aus 3.3>
Address    = 10.8.0.4/24
# DNS bewusst NICHT setzen (Split-Tunnel: nur das Gehirn-Netz geht durch den Tunnel)

[Peer]
PublicKey           = <Server-PublicKey>
Endpoint            = 168.231.83.205:51820
AllowedIPs          = 10.8.0.0/24
PersistentKeepalive = 15
```
> **Fix-relevant:** `PersistentKeepalive = 15` (nicht 25) → schnellere NAT-Heilung nach einem
> Client-IP-Wechsel (dynamische Leitungen). `AllowedIPs = 10.8.0.0/24` = **Split-Tunnel**: nur das
> Gehirn-Subnetz läuft durch den Tunnel, der restliche Internet-Verkehr bleibt normal.

### 3.5 samba.env (`~/SK/second-brain/samba.env`)
```
SAMBA_USER=frank          # Windows: der Samba-User "frank"
SAMBA_PASS=<passwort>
```
> macOS nutzt einen **eigenen** Samba-User `frankmac` (uid 1001), damit eine Passwort-Änderung auf
> einer Plattform die andere nicht bricht — auf dem Mac also `SAMBA_USER=frankmac`.

---

## 4. Windows-Client — Schritt für Schritt

### 4.1 WireGuard installieren + Tunnel importieren
1. WireGuard für Windows installieren (offizielle MSI von wireguard.com).
2. Die `~/SK/second-brain/wireguard/second-brain.conf` in WireGuard **importieren**.
3. **KRITISCH — Tunnel-Name:** Der importierte Tunnel muss `pc` heißen, denn die Skripte suchen den
   Windows-Dienst **`WireGuardTunnel$pc`**. Heißt der Tunnel anders, entweder umbenennen (Config-Datei
   in `pc.conf` umbenennen und neu importieren) **oder** in `wg-drive-reconnect.ps1` +
   `wg-setup-elevated.ps1` den Dienstnamen `WireGuardTunnel$pc` an deinen Tunnel-Namen anpassen.
4. Tunnel aktivieren und prüfen: `ping 10.8.0.1` muss antworten.

### 4.2 MCP-Config setzen (Claude Code sieht das Gehirn)
Die Vorlage ist identisch mit dem Ziel — einfach kopieren:
```powershell
Copy-Item C:\Users\<DU>\proggs\claude-code-setup\mcp-windows.json C:\Users\<DU>\proggs\.mcp.json
```
Inhalt (so soll `~/proggs/.mcp.json` aussehen):
```json
{
  "mcpServers": {
    "second-brain": { "type": "http", "url": "http://10.8.0.1:8001/mcp" }
  }
}
```
> Wenn der `auto-sync`-Hook aus `claude-code-setup/hooks/auto-sync.ps1` aktiv ist, wird `.mcp.json` beim
> Session-Start automatisch aus `mcp-windows.json` gespiegelt — dann ist der Copy-Schritt nur die
> Erst-Einrichtung. Nach dem Setzen Claude Code neu starten; testen mit dem Werkzeug `brain_health`.

### 4.3 Netzlaufwerke Z:/Y: einrichten (mit allen Fixes)
Genau **ein** Befehl, **einmalig als Administrator** (UAC einmal bestätigen):
```powershell
powershell -ExecutionPolicy Bypass -File `
  C:\Users\<DU>\proggs\second-brain-server\windows\wg-setup-elevated.ps1
```
Das registriert **zwei geplante Aufgaben** (der Kern der Robustheit, „Defense in Depth" — zwei
unabhängige Wege, die Laufwerke zu mounten):

| Aufgabe | Ebene | Was sie tut |
|---------|-------|-------------|
| **WG-Drive-Mount** | nicht-erhöht (Limited) | **PRIMÄRER, im Explorer sichtbarer** Mount (`wg-drive-mount.ps1`) — bei Login + alle 5 Min |
| **WG-Drive-Reconnect** | erhöht (Highest) | Backup: WireGuard-Dienst sicherstellen + `EnableLinkedConnections` + Mount als zweiter Weg |

Danach läuft alles unsichtbar automatisch. Z: (`\\10.8.0.1\gedanken`) und Y: (`\\10.8.0.1\daten`)
erscheinen im Explorer, sobald der Tunnel steht.

> ### ⚠️ WICHTIGSTE Portierungs-Anpassung auf einem ANDEREN Windows-Rechner
> `wg-setup-elevated.ps1` enthält den **hartcodierten Benutzer** `POWER-PC\barwa` (als
> `-UserId` / `-User` der geplanten Aufgaben) und die **hartcodierten Pfade**
> `C:\Users\barwa\proggs\second-brain-server\windows\*.vbs`. Auf einem neuen Rechner **vor** dem
> Ausführen anpassen:
> - **Rechner-/Benutzername:** `POWER-PC\barwa` → `<DEIN-PC>\<DEIN-USER>` (ermitteln mit
>   `whoami`, gibt z.B. `desktop-abc\max`).
> - **VBS-Pfade:** `C:\Users\barwa\proggs\...` → `C:\Users\<DU>\proggs\...`.
>
> `wg-drive-mount.ps1` selbst braucht **keine** Pfad-Anpassung (es liest `samba.env` relativ zu
> `$env:USERPROFILE` und nutzt keine hartcodierten User). Nur `wg-setup-elevated.ps1` ist rechnergebunden.

### 4.4 Die Windows-Laufwerk-Fixes (was da alles drinsteckt und WARUM)
Diese Fixes sind live erarbeitet worden — ohne sie „verschwinden" die Laufwerke:

1. **Port-445-Gate (Fix §9):** Der Mount wartet auf einen TCP-Test gegen **`10.8.0.1:445`** (den echten
   SMB-Port) — **nie** einen Dienst-Port wie 8000. Sonst blockiert ein Dienst-Neustart das Mapping.
2. **WNetAddConnection2 statt `net use` (Fix §9):** Gemappt wird über die `mpr.dll`-API mit **expliziten
   Credentials** und **ohne** `CONNECT_INTERACTIVE`. `net use /persistent:yes` **hängt** im
   erhöhten/versteckten Task ewig (fragt interaktiv nach dem Benutzernamen, den niemand eingibt) →
   net.exe-Prozess-Leak.
3. **Primär nicht-erhöht mappen (Fix §10/§11):** Im **erhöhten** Token gemappte Laufwerke sind im
   normalen Explorer **unsichtbar** (UAC-Token-Isolation). Deshalb ist der **nicht-erhöhte** Task der
   primäre, sichtbare Weg (Microsoft-`MapDrives.ps1`-Linie). `EnableLinkedConnections=1` (im erhöhten
   Task gesetzt) ist nur das Sicherheitsnetz — es versagt bei UAC „Prompt for credentials".
4. **Persistent-Login-Race / Fehler 1219 (Fix §11):** Persistente `HKCU:\Network`-Einträge lässt Windows
   beim Login **vor** dem Tunnel verbinden → totes „rotes X"-Mapping + Fehler **1219** („mehrere
   Benutzernamen nicht zulässig"). `wg-drive-mount.ps1` **entfernt** die HKCU-Einträge, mappt
   **nicht-persistent** (Flag 0) als einzige tunnel-bewusste Quelle, und räumt bei 1219 **alle**
   Sitzungen zu `10.8.0.1` (beide Buchstaben + `\\10.8.0.1` + `IPC$`) ab und mappt neu.
5. **UTF-8 mit BOM, keine Em-Dashes (Fix §12):** Windows PowerShell 5.1 liest `.ps1` **ohne** BOM als
   cp1252 → ein Em-Dash (—) in einem String zerschießt Klammern/Quotes („schließende } fehlt"). Die
   Skripte sind bewusst BOM-behaftet und ASCII-sauber im Code — **so lassen**.

**Troubleshooting Windows:** Log lesen unter `%LOCALAPPDATA%\wg-drive-mount.log` bzw.
`%LOCALAPPDATA%\wg-setup-result.txt`. „Rotes X"? → `net use Z: /delete /yes` + Task „WG-Drive-Mount"
manuell in der Aufgabenplanung starten. Tunnel prüfen: `Test-NetConnection 10.8.0.1 -Port 445`.

---

## 5. macOS-Client — Schritt für Schritt

### 5.1 Voraussetzungen
```bash
brew install wireguard-tools        # stellt wg / wg-quick bereit
```
- `~/SK/second-brain/wireguard/second-brain.conf` (aus 3.3/3.4, eigene `10.8.0.X`-IP)
- `~/SK/second-brain/samba.env` mit `SAMBA_USER=frankmac`

### 5.2 SMB soft mounts (einmalig, root) — Fix gegen Finder-Freeze
```bash
sudo cp ~/proggs/second-brain-server/macos/nsmb.conf.vorlage /etc/nsmb.conf
```
> Muss als **root** unter `/etc/nsmb.conf` liegen — die User-`~/Library/Preferences/nsmb.conf` greift
> **nicht** für die automountd-Mounts. Bewirkt: bei einem Tunnel-Aussetzer friert der Finder nicht ein,
> ein Zugriff gibt nach `max_resp_timeout` (30 s) auf statt ewig zu hängen.

### 5.3 Installer laufen lassen
```bash
# 1) User-Teil (KEIN sudo): Mount-LaunchAgent (Login + alle 5 Min) + Sofort-Mount
bash ~/proggs/second-brain-server/macos/setup-macos.sh

# 2) Boot-Tunnel (einmalig, mit sudo): WireGuard beim Systemstart hochfahren
sudo bash ~/proggs/second-brain-server/macos/setup-macos.sh --daemon
```
Danach erscheinen unter „Speicherorte" im Finder: `gedanken` (= Windows Z:) und `daten` (= Y:), gemountet
unter `/Volumes/gedanken` bzw. `/Volumes/daten`.

### 5.4 MCP-Config setzen
```bash
cp ~/proggs/claude-code-setup/mcp-macos.json ~/proggs/.mcp.json
```
(Inhalt identisch zur Windows-Variante: `http://10.8.0.1:8001/mcp`.) Claude Code neu starten, mit
`brain_health` testen.

### 5.5 Die macOS-Laufwerk-Fixes (Resilienz gegen IP-Wechsel / Tunnel-Aussetzer)
Vorfall-Ursache war ein **Wechsel der öffentlichen Client-IP** (dynamische Leitung) → WireGuard-Endpoint
veraltet → SMB-Mounts fallen ab. Die Fehlerklasse ist unschädlich gemacht:

1. **Stale-Mount-Erkennung** (`wg-drive-mount.sh`): Ein gemounteter Share wird per Leseprobe mit hartem
   Timeout (perl-alarm, 4 s) geprüft. Toter/hängender Mount → `diskutil unmount force` + sofort neu
   mounten, statt blind „bereits gemountet" zu melden.
2. **SMB soft mounts** (`/etc/nsmb.conf`, siehe 5.2).
3. **`PersistentKeepalive = 15`** in der Client-`.conf` → schnellere NAT-Heilung.
4. **Endpoint-Monitor auf dem VPS** (`wg-endpoint-monitor.*`): loggt alle 30 s jeden Endpoint-Wechsel nach
   `/var/log/wg-endpoint-monitor.log` — damit ist der nächste Vorfall sofort beweisbar.

> ### ⚠️ Portierungs-Anpassung auf einem ANDEREN Mac
> `wireguard-up.sh` enthält **hartcodierte Pfade** mit Benutzernamen `frank`:
> `CONF="/Users/frank/SK/second-brain/wireguard/second-brain.conf"` und
> `LOG="/Users/frank/Library/Logs/wg-tunnel.log"`. Heißt dein Mac-Home anders (z.B. `/Users/barwa`),
> die beiden Pfade **vor** dem `--daemon`-Setup anpassen. Ebenso die `-User`-Bezüge in den `.plist`,
> falls dein Kurzname abweicht.

**Troubleshooting macOS:** Laufwerke weg? **ZUERST** den Tunnel prüfen (häufigste Ursache, nicht der Mac):
`ping -c 10 10.8.0.1` (Paketverlust?) · `nc -z 10.8.0.1 445` (Port offen?) · `ifconfig | grep 10.8.0` (Tunnel aktiv?).
IP-Wechsel beweisen: auf dem VPS `tail /var/log/wg-endpoint-monitor.log`. Logs lokal:
`~/Library/Logs/wg-drive-mount.log` und `~/Library/Logs/wg-tunnel.log`. Mount erzwingen:
`bash ~/proggs/second-brain-server/macos/wg-drive-mount.sh`.

---

## 6. Server KOMPLETT neu aufsetzen (nur bei VPS-Umzug — sonst überspringen)

Nur nötig, wenn der Server selbst auf einen neuen Linux-Host zieht. Der Stack ist als Code/Config im Repo
gesichert und 1:1 reproduzierbar.

1. **Basis:** Ubuntu-Server, Docker + Docker-Compose-Plugin installieren.
2. **Code hochladen:** Inhalt von `~/proggs/second-brain-server/` nach `/opt/second-brain` kopieren
   (scp/rsync — der VPS ist **kein** git-Repo).
3. **Secrets einspielen:** `.env` aus `~/SK/second-brain/` nach `/opt/second-brain/.env` legen. Nötige
   Variablen: `QDRANT_API_KEY`, `GEMINI_API_KEY`, `GOOGLE_API_KEY` (= GEMINI_API_KEY), `SB_API_KEY`
   (`python -c "import secrets;print(secrets.token_urlsafe(48))"`), dazu `GROQ_API_KEY`,
   `OPENCODE_API_KEY`, `TAVILY_API_KEY` je nach genutztem Dienst.
4. **WireGuard-Server:** `/etc/wireguard/wg0.conf` (`Address 10.8.0.1/24`, `ListenPort 51820`, je ein
   `[Peer]` pro Client), `chmod 600`, `wg-quick up wg0` + `systemctl enable wg-quick@wg0`. UFW:
   `ufw allow 51820/udp`.
5. **Boot-Reihenfolge absichern:** systemd-drop-in
   `/etc/systemd/system/docker.service.d/wait-for-wireguard.conf`, damit `docker.service` **nach**
   `wg-quick@wg0` startet (sonst scheitert die Bindung an `10.8.0.1`).
6. **Samba (für die Laufwerke):** `smb.conf` `[global]` mit **`interfaces = lo 10.8.0.1/24`** (die
   **konkrete Host-IP mit Maske**, NICHT `10.8.0.0/24`!) + `bind interfaces only = yes` +
   `protocol = SMB3`. Freigaben `gedanken` → `/srv/samba/gedanken`, `daten` → `/srv/samba/daten`.
   UFW: `ufw allow in on wg0 to any port 445 proto tcp` (nur im Tunnel!). Samba-User `frank` (Windows)
   und `frankmac` (uid 1001, Mac) in `valid users` beider Freigaben.
7. **Ordner-Rechte (chown-Fallen):** `agent-data`/`agent-logs`, `librarian-data`/`librarian-logs`,
   `/srv/samba/gedanken/{Logbuch,Eval-Logs}` → `chown -R 1000:1000`. `brain-data` → uid **10001**.
   `dashboard-data` → uid **10002**. Sonst können die Container ihre Configs/Logs nicht schreiben.
8. **Starten:** `cd /opt/second-brain && docker compose up -d --build`.
9. **Daten übernehmen:** das Volume `qdrant-data/` vom alten Server kopieren (das ist das eigentliche
   Gedächtnis). Alternativ die Qdrant-Snapshots von Z: (`/srv/samba/gedanken`) restoren.

Server-Änderungen im Alltag: siehe `second-brain-server/DEPLOY.md` (Faustregel: lokal ändern → committen
+ pushen → `scp` auf den VPS → `docker compose up -d --build <dienst>` → Health prüfen; **`--build` ist
Pflicht**, der Code ist ins Image gebacken).

---

## 7. Verifikation (woran man sieht, dass alles läuft)

**Vom Client aus (Tunnel muss aktiv sein):**
```bash
# Tunnel steht?
ping 10.8.0.1
# Gehirn antwortet?
curl -s http://10.8.0.1:8000/health          # brain-api: status/version/points
# MCP in Claude Code: das Werkzeug brain_health aufrufen → status "ok", qdrant "reachable"
```
Erwartete gesunde Antwort von `brain_health` (Beispiel-Stand 08.07.2026):
```json
{ "status": "ok", "qdrant": "reachable", "collection": "brain__e2",
  "collection_status": "green", "ready": true, "points": 2398,
  "embed_model": "gemini-embedding-2", "embed_dims": 3072 }
```
**Laufwerke:** Windows — Z: und Y: im Explorer sichtbar. macOS — `gedanken`/`daten` unter „Speicherorte".

---

## 8. Die aktuelle CLAUDE.md auf einen neuen Rechner mitnehmen

**Kurzantwort:** Die maßgebliche, aktive Steuerdatei ist **`~/proggs/CLAUDE.md`** (im Repo-Wurzelordner).
Sie ist git-getrackt und kommt beim `git clone` **automatisch** mit — du musst dafür nichts extra tun.

**Wichtige Ergänzung:** In `claude-code-setup/` liegt zusätzlich eine **eigene** `CLAUDE.md`. Diese ist
eine Referenz-/Portabilitäts-Kopie und **weicht inhaltlich leicht von der aktiven Root-Datei ab** (andere
Intro-Formulierung, andere Struktur der Direktiven-Tabelle: „Volltext immer geladen" statt „auf Zuruf").
Für die Übernahme auf einen neuen Rechner ist das **egal** — es zählt die Root-`~/proggs/CLAUDE.md`, die
Claude Code beim Start in `~/proggs` lädt.

**Aber:** Die CLAUDE.md allein macht die Umgebung nicht komplett. Sie verweist auf die **Regel-Kerne** in
`~/.claude/rules/*.md` (die bei jeder Session automatisch geladen werden) und deren Volltexte in
`claude-code-setup/docs/rules/`. Diese `~/.claude/rules/` liegen **nicht** im Repo-Wurzelordner, sondern
werden aus `claude-code-setup/` in den Home-Ordner **gespiegelt**. Für eine voll funktionierende Umgebung
auf dem neuen Rechner also zusätzlich (siehe `harness-mirror-on-change.md`):

```
claude-code-setup/rules/     →  ~/.claude/rules/
claude-code-setup/hooks/     →  ~/.claude/hooks/
claude-code-setup/skills/    →  ~/.claude/skills/
claude-code-setup/settings-reference.json  →  ~/.claude/settings.json   (Windows-Variante)
```

> Falls gewünscht, kann die abweichende `claude-code-setup/CLAUDE.md` an die aktive Root-Datei angeglichen
> werden, damit beide identisch sind — kurz Bescheid geben, dann wird sie 1:1 synchronisiert.

---

## 9. Häufige Stolperfallen (Kurzliste)

| Symptom | Ursache / Fix |
|---------|---------------|
| `brain_health`/MCP nicht erreichbar | WireGuard-Tunnel nicht aktiv → `ping 10.8.0.1`. Tunnel-Name auf Windows muss `pc` sein. |
| Deploy/SSH zum VPS hängt | Full-Tunnel-VPN (z.B. Windscribe) blockiert SSH → **ausschalten** (WireGuard zum Gehirn darf bleiben). |
| Windows-Laufwerk „rotes X" / Fehler 1219 | Persistent-Login-Race → `net use Z: /delete /yes`, Task „WG-Drive-Mount" neu starten. |
| Laufwerk gemappt, aber im Explorer unsichtbar | UAC-Token-Isolation → nicht-erhöht mappen ist der primäre Weg; `EnableLinkedConnections=1` als Netz. |
| macOS-Laufwerke weg | Zuerst Tunnel/Endpoint-Wechsel prüfen (`wg-endpoint-monitor.log`), nicht Mac/Server verdächtigen. |
| SMB geht nicht, obwohl Ping/SSH ok | Server-`smb.conf`: `interfaces = lo 10.8.0.1/24` (konkrete Host-IP!), nicht `10.8.0.0/24`. |
| `.ps1` startet nicht („} fehlt") | PS 5.1 cp1252-Falle → `.ps1` als UTF-8 **mit BOM**, keine Em-Dashes im Code. |
| Neuer Code deployt, läuft aber alter | `--build` vergessen — der Code ist ins Docker-Image gebacken. |

---

*Quellen im Repo: `second-brain-server/README.md`, `second-brain-server/DEPLOY.md`,
`second-brain-server/compose.yaml`, `second-brain-server/windows/*`, `second-brain-server/macos/*`,
`bugs/server/samba-wireguard.md`, `bugs/server/wireguard.md`, `bugs/claude-tooling/mcp-server.md`.*
