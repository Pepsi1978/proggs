# Bekannte Bugs & Fallen: Cowork (Claude Desktop App)

> **PFLICHT-LESEN vor JEDER Arbeit im Cowork-Modus der Claude-Desktop-App** (macOS/Windows):
> bevor du Ordner verbindest, Connectors/MCP nutzt, eigene Skills/Plugins hochlädst, geplante
> Aufgaben anlegst, Live-Artefakte baust oder Computer-Use/Chrome einschaltest.
>
> Kuratiert aus offizieller Anthropic-Doku/Support, dem offiziellen Issue-Tracker
> `github.com/anthropics/claude-code` (Label `area:cowork`) und `anthropics/claude-ai-mcp`,
> sowie externer Sicherheitsforschung (PromptArmor, Embrace The Red). Lösungen sind
> funktionserhaltend (nie „Feature weglassen").
>
> **Stand:** recherchiert am **2026-06-13** mit 7 parallelen Researchern. Cowork hat **keinen
> klassischen Versions-Changelog** — Anker ist der Stand der offiziellen Anthropic-Support-/Doku-Seiten
> (Quellenstand Januar–Juni 2026) plus die in den Issues genannten Desktop-App-Versionen (z. B.
> 1.1.x / Claude-Code-im-VM 2.1.x). Cowork startete Januar 2026 als Research Preview (Windows ab
> 10.02.2026), ist inzwischen für alle Bezahlpläne verfügbar; Computer-Use bleibt Research Preview (nur Pro/Max).
>
> **Quellen-Rangordnung:** offiziell (support.claude.com, claude.com/docs, code.claude.com,
> anthropic.com) = Grundwahrheit. GitHub-Issues im offiziellen Repo zählen als nahe-offiziell
> (Anthropic-Tracker), sind aber Nutzer-Einreichungen. Community/Presse = `extern` (sekundär).
>
> **Gegenseite (Best Practices):** `best-practices/claude-tooling/cowork.md`
> (wechselseitige Bezugstabelle ganz unten).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre (`Read` mit `limit=80`).
> Volltext bei JEDEM Fehler im Bereich (Stufe B) und vor Hochrisiko-Arbeit (Stufe C) — besonders vor
> Datei-Massenoperationen (§4.1) und vor dem Verbinden sensibler Ordner (§9.1).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Datei-Massenoperation in iCloud-/OneDrive-Ordner | VORHER Dateien „Download Now" + Backup; sonst Datenverlust durch `cp`+`rm` auf 0-Byte-Stubs | §4.1 ⭐KRITISCH |
| 2 | Eigene Skills/Plugins „aktiviert", aber nicht nutzbar | Marketplace-Mount-Bug → Plugin als **ZIP** exportieren + hochladen | §6.1 ⭐KRITISCH |
| 3 | Windows: „VM service not running" | `Start-Service CoworkVMService` (Admin); MSIX-Neuinstall von claude.com/download | §1.1 |
| 4 | Windows: „EXDEV: cross-device link" | Speicherort auf `C:\` zurück; App-Bug (luafv/MSIX-VFS) → ggf. auf Fix warten | §1.3 |
| 5 | Windows: „Virtualization not enabled" trotz Hyper-V | VM Platform+Hypervisor Platform+WSL2 aktivieren; Anti-Cheat reaktiviert VT-x | §1.4 |
| 6 | macOS: Workspace lädt nicht nach OS-Update | macOS-26.3-vsock-Regression / VM-SDK-Checksum → Datei-/Web-Tools laufen weiter | §2.1 |
| 7 | macOS: Ordner in `~/Documents` „Failed to load session" | Projekt aus `~/Documents` rausziehen (+ Symlink zurück) | §3.3 |
| 8 | „Always allow" fragt trotzdem bei jeder Operation | Bekannte Regression; bei Scheduled Tasks „Run now" + pro Tool always-allow | §3.1 / §7.9 |
| 9 | Connector „Connected", aber keine Tools / leer | App neu starten + unter Settings→Capabilities neu authentifizieren | §5.7 |
| 10 | Eigener Remote-MCP aus Cowork unerreichbar | Läuft über **Anthropics Cloud** → Anthropic-IPs in Firewall allowlisten | §5.1 |
| 11 | Lokaler/stdio-MCP oder DXT-Extension fehlt in Cowork | Per Design nicht eingebunden → als Remote-HTTP-Connector neu aufsetzen | §5.5 |
| 12 | Plugin-Upload „validation failed" (ohne Detail) | `<…>` oder URL im `description`-Feld ersetzen; nur `.zip`, nicht `.plugin` | §6.4 |
| 13 | Geplante Aufgabe läuft nicht / zur falschen Zeit | Nur bei wachem Rechner+offener App; nur 1 Catch-up → Zeit-Guardrails in Prompt | §7.1 / §7.2 |
| 14 | App friert bei jedem Start ein nach High-Freq-Cron | `scheduled-tasks.json` umbenennen → bootet wieder; danach moderate Frequenz | §7.3 ⭐KRITISCH |
| 15 | Live-Artefakt: MCP-Daten fehlen/leer | Im Artefakt-Code BEIDE Felder lesen: `structuredContent ?? content`; Refresh-Button | §8.2 |
| 16 | Word/Doc aus fremder Quelle im Ordner | Prompt-Injection real (PromptArmor) → keine sensiblen Ordner verbinden, „Ask before acting" | §9.1 ⭐KRITISCH |
| 17 | Computer Use einschalten | KEINE Sandbox, echter Desktop; Links aus Mail/Doku NIE per Computer-Use klicken | §9.4 |
| 18 | Aufgabe stoppt mittendrin | App muss offen + Rechner wach bleiben (per Design) | §10.1 |
| 19 | Usage überraschend schnell aufgebraucht | Cowork verbraucht viel mehr als Chat → Einfaches im Chat, Verwandtes bündeln | §10.5 |
| 20 | Datei landet „irgendwo" / „Location not available" | Zielordner explizit verbinden + vollen Pfad angeben (kein temp-Scratchpad) | §4.3 |
| 21 | `git push` scheitert: „could not read Username for github.com" | Kein Git-Credential-Manager in der VM; Fix nur sitzungsweit → für DAUERHAFT: Token in `.git/credentials` + `credential.helper store --file=.git/credentials` (relativ, Remote NICHT ändern) | §10a ⭐HÄUFIG |
| 22 | Auth steht, aber `git commit/push` aus der VM hängt an `.lock`-Dateien (Windows) | FIX: git-dir auf die VM-Platte legen → Wrapper `cowork-git.sh` (`bash cowork-git.sh push "msg"`); Fallback: aus dem Windows-Terminal pushen; `.git/claude-multi-session.lock` NIE löschen | §10a.5 / §10a.6 ⭐KRITISCH |
| 23 | `git add -A` aus Cowork bricht ab / bläht Commit auf | 4 Mount-Artefakte: Symlink-I/O-Fehler, 0755-Modus, untrackte Build-Bäume, LFS-Vollinhalt → alle in `cowork-git.sh` abgefangen (skip-worktree, `core.fileMode false`, build-Ignores); langer Push in EINEM VM-Aufruf | §10a.7 ⭐KRITISCH |

---

## 1. VM-/Workspace-Start — Windows (größte Fehlerklasse) ⭐ HÄUFIG

> Architektur: Cowork führt Shell/Code unter Windows in einer Linux-VM (Hyper-V/HCS) aus, verwaltet
> vom Dienst **CoworkVMService** (`cowork-svc.exe`). Datei-/Web-Tools laufen nativ. Fast alle Windows-
> Startfehler hängen an dieser VM-Schicht. Diagnose-Logs: `%APPDATA%\Claude\logs\cowork_vm_node.log`
> (Direkt-Install) bzw. `%LOCALAPPDATA%\Packages\Claude_*\LocalCache\Roaming\Claude\logs\` (MSIX/Store).

### 1.1 „VM service not running. The service failed to start." ⭐ HÄUFIG
**Symptom:** Beim Start einer Cowork-Aufgabe (oder „Run now" bei Scheduled Task) Banner
„Failed to start Claude's workspace — VM service not running." Oft begleitet von „Can't reach the
Claude API from Claude's workspace" oder „sdk-daemon not connected". Reiner Chat läuft weiter.
**Ursache:** CoworkVMService startet nicht. Häufigste Gründe: (a) Installation über den alten
`.exe`/Squirrel-Installer statt MSIX (§1.9); (b) Dienst hat Starttyp **Manual** und bleibt nach
Reboot/Update/Sleep/Crash gestoppt; (c) Windows-Virtualisierungs-Features nicht aktiviert (§1.4).
Dieselbe Meldung deckt mind. 6 verschiedene Ursachen ab.
**Versionen:** Windows 10/11 Pro & Home; v1.1.3918 bis 1.1.7714+; bis Mai/Juni 2026 kein dauerhafter Fix.
**FIX (offiziell zuerst):**
1. Neu installieren von **claude.com/download** (erzwingt MSIX).
2. Dienst manuell starten (Admin-PowerShell): `Start-Service CoworkVMService` (prüfen: `Get-Service CoworkVMService`). Einzeiler nach App-Hänger: `Stop-Process -Name "Claude" -Force -ErrorAction SilentlyContinue; Start-Service CoworkVMService`.
3. Falls weiter Fehler: Windows-Features aktivieren — **Virtual Machine Platform**, **Windows Subsystem for Linux**, **Windows Hypervisor Platform** —, WSL2 installieren, neu starten (volles Hyper-V nicht nötig). VT-x/AMD-V im BIOS aktiv.
Starttyp auf „Automatic" umstellen ist per Rechten blockiert → Workaround: Verknüpfung „als Admin" mit `Start-Service CoworkVMService`.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell, Troubleshooting); GitHub anthropics/claude-code #27010/#27801/#24918; brycewatson.com/blog (extern).

### 1.2 CoworkVMService Exit Code 1066 — startet, crasht nach 2–5 s
**Symptom:** Dienst läuft 2–5 s, beendet sich mit **Exit Code 1066** („Incorrect function"). Event-Log
System ID 7024. `cowork_vm_node.log`: „Module loaded successfully", dann nichts. `main.log`: alle ~60 s „VM service not running".
**Ursache:** Fehler **innerhalb** `cowork-svc.exe` selbst — KEIN DCOM-, Hyper-V- oder Stale-State-Problem
(alle verifiziert funktional). Das Binary beendet sich absichtlich ohne Crashdump/Log; außerhalb der
MSIX-Sandbox nicht ausführbar → externes Debugging unmöglich.
**Versionen:** v1.1.7203–1.1.7714 (MSIX), Win11 Pro 25H2; „never worked". **Offen** (Status unklar, kein Beleg für Fix).
**FIX:** Kein verlässlicher Nutzer-Fix. Bundle-Nuke + Redownload (§1.6), Service-Neustart, Reinstall reproduzieren identisch → Anthropic-seitiger Fix nötig. Bis dahin Datei-/Web-only-Aufgaben nutzen (§2.4).
**Quelle:** GitHub #36801 (extern eingereicht, offizielles Repo); verwandt #30179/#27801/#25206.

### 1.3 „EXDEV: cross-device link not permitted"
**Symptom:** Cowork startet nicht; `EXDEV: cross-device link not permitted, rename '…\tmp\wvm-XXX\rootfs.vhdx' -> '…\vm_bundles\claudevm.bundle\rootfs.vhdx'`. Download bricht bei ~80 % ab, `vm_bundles` wird nie erstellt.
**Ursache:** Claude lädt das VM-Bundle nach `%TEMP%` und versucht `fs.rename()` nach `vm_bundles`;
`rename()` scheitert, wenn Quelle/Ziel als verschiedene logische Geräte gelten. Drei Varianten:
(a) TEMP/TMP auf anderem Laufwerk als `%AppData%`; (b) **MSIX-VFS** virtualisiert AppData\Roaming in
separaten Namespace; (c) häufigster Fall: **luafv** (UAC-File-Virtualization-Treiber) + Sandbox-Prozesse
im tmp (Acrobat, OneDrive/CldFlt, Steam, WebView2) lassen `rename()` eine Virtualisierungsgrenze sehen,
obwohl beide Pfade physisch auf C: liegen.
**Versionen:** v1.1.4498–1.1.8308 (MSIX und Direkt-.exe), Win11 Pro 25H2; „never worked". Issue #39029
von Anthropic als „invalid" gelabelt (umstritten), **offen**.
**FIX:** Offiziell: Windows-Speicherort (Einstellungen > System > Speicher > „Wo neue Inhalte gespeichert
werden") auf **C:** zurücksetzen, deinstallieren, neu installieren, updaten. ACHTUNG: Bei der luafv-Variante
(c) helfen Nutzer-Workarounds (Junctions, APPDATA-Override, Defender-Ausnahmen, Admin-Start, robocopy)
**nicht** — dann App-seitiger Fix nötig (EXDEV-Fallback `copyFile`+`unlink`).
**Quelle:** support.claude.com/en/articles/13345190 (offiziell, C:-Reset); GitHub #39029/#25476/#36642/#41240/#32186 (extern, luafv).

### 1.4 „Virtualization is not enabled" trotz aktiviertem Hyper-V
**Symptom:** Sofort beim Klick auf den Cowork-Tab: „Claude's workspace requires hardware virtualization
(Hyper-V). Enable virtualization in your BIOS/UEFI…", obwohl `systeminfo` „A hypervisor has been detected" zeigt.
**Ursache:** (a) Fehlerhafte App-seitige Plattformerkennung (echter Bug, auch bei voll aktiviertem Hyper-V) —
eng verwandt mit „yukonSilver not supported" (§1.5); (b) **Windows Home:** Voll-Hyper-V (`vmms`) gar nicht
verfügbar; (c) **Gaming-Anti-Cheat** (Vanguard etc.) deaktiviert VT-x/SVM still auf Hardware-Ebene.
**Versionen:** v1.1.3918+; Win10 Pro, Win11 Home/Pro/Education; #27316 (Anthropic-assigned bug/oncall) bis Mai 2026 **offen**.
**FIX:** BIOS: Intel VT-x / AMD SVM + VT-d aktivieren. Windows-Features: Virtual Machine Platform, Windows
Hypervisor Platform, WSL2 (Home), ggf. Hyper-V (Pro), neu starten. Bei Anti-Cheat im BIOS VMX/SVM reaktivieren.
Bei Variante (a) hilft nur ein App-Fix. Min. 16 GB RAM, 10 GB frei empfohlen. Vorab den offiziellen
**Cowork-Readiness-Check** (claude.ai-Download) laufen lassen.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell, Readiness-Check); GitHub #27316/#27384/#45883 (extern); cybersecurityforme.com (extern).

### 1.5 „yukonSilver not supported" — fehlerhafte Plattform-Klassifizierung
**Symptom:** Log `[cleanupVMBundleIfUnsupported] yukonSilver not supported (status=unsupported)…`. VM lädt nie, Cowork-Tab evtl. ganz weg.
**Ursache:** Claude-interne Plattformerkennung markiert ein voll kompatibles System fälschlich als inkompatibel. Reiner App-Bug.
**Versionen:** bis v1.1.5368 nicht gefixt; Regression bei 1.1.5749; März–Mai 2026 **offen**.
**FIX:** Kein Nutzer-Fix; Clean-Reinstall (§1.6) hilft manchmal. App-seitiger Fix nötig.
**Quelle:** GitHub #25136/#32004/#32837 (extern); cybersecurityforme.com (extern).

### 1.6 „Reinstall workspace"-Button repariert das Bundle NICHT
**Symptom:** Eingebauter „Reinstall workspace"-Button scheint nichts zu tun; Fehler bleibt. Log:
`[deleteVMBundle] Reinstall files deleted (sessiondata.img and rootfs.img.zst preserved)`.
**Ursache:** Der Button bewahrt ausgerechnet `sessiondata.img` und `rootfs.img.zst` — die zwei am ehesten korrupten Dateien. Kein echter Frisch-Download.
**Versionen:** v1.1.7714; auch macOS (#24070). **Offen.**
**FIX (Workaround, funktionserhaltend):**
```
Get-Process -Name "*claude*","*cowork*" -ErrorAction SilentlyContinue | Stop-Process -Force
Remove-Item -Recurse -Force "$env:APPDATA\Claude\vm_bundles"
```
Claude neu starten → Bundle wird frisch geladen (`cowork_vm_node.log -Tail 15` bis „All files ready"), dann `Start-Service CoworkVMService`.
**Quelle:** brycewatson.com (extern); GitHub #24070 (extern).

### 1.7 Race Condition / Dirty Shutdown — Dienst crasht beim Neustart
**Symptom:** Nach unsauberem Beenden crasht der Dienst beim nächsten Start („terminated unexpectedly", kein Auto-Retry). Oft `Failed: Error: Request timed out: stopVM`.
**Ursache:** Claude startet CoworkVMService sofort beim Launch, bevor das Bundle fertig extrahiert ist;
30-s-VM-Stop-Timeout lässt bei Überschreitung Hyper-V-Ressourcen (VM, Netzadapter, NAT) ungesäubert →
nächster Start crasht. **Verstärkt durch Force-Kill von `cowork-svc.exe` im Task-Manager.**
**Versionen:** v1.1.7714; **offen.**
**FIX:** Sauberes Shutdown: File > Exit, **mind. 60 s warten** (`cowork-svc.exe` darf laufen bleiben), NICHT
im Task-Manager killen. Bei Persistenz: Bundle-Nuke (§1.6); optional Scheduled Task `Start-Sleep 30; Start-Service CoworkVMService` bei Logon.
**Quelle:** brycewatson.com (extern, mit fertigem Script).

### 1.8 MSIX-Install/-Update scheitert mit HRESULT 0x80073CF6
**Symptom:** Installer/Update bricht ab: `AddPackage failed with HRESULT 0x80073CF6`. Paket „wedged"; Ordner in `C:\Program Files\WindowsApps\` selbst als Admin nicht löschbar.
**Ursache:** **CoworkVMService läuft noch** während des Updates → AppX-Installer kann gelockte Dateien
nicht löschen. Der Installer sollte CoworkVMService + `cowork-svc`/`parsecd`/`chrome-native-host` vorher stoppen, tut es aber nicht zuverlässig. Zusätzlich: Sideloading/„Trusted App Installs" nötig.
**Versionen:** April–Mai 2026, Win11 Pro; **offen**, von Anthropic untersucht.
**FIX:** Claude voll deinstallieren; `sc.exe stop CoworkVMService` (notfalls `sc.exe delete`, ggf. Safe Mode/Autoruns); Reste löschen (`vm_bundles`, `%LOCALAPPDATA%\claude-code-vm`, `%TEMP%\claude*`); neu starten; frischen MSIX von claude.com/download **als Administrator** installieren.
**Quelle:** GitHub #56949/#49540/#49917 (extern); cybersecurityforme.com (extern).

### 1.9 Alter Squirrel-/.exe-Installer — Cowork-Tab fehlt, „newer installation"-Banner
**Symptom:** Kein Cowork-Tab (nur Chat); Banner „Cowork requires a newer installation" mit „Reinstall"-Button, der nichts tut; „Check for Updates" meldet fälschlich aktuelle Version.
**Ursache:** Installation von vor dem 10.02.2026 = altes **Squirrel**-Paket; der Squirrel→MSIX-Upgrade-Pfad ist defekt. Cowork braucht den MSIX-Build.
**Versionen:** Pre-10.02.2026-Installs; **offen** (MSIX-Reinstall kann Tab dauerhaft verlieren, #31516).
**FIX:** Voll deinstallieren, Reste entfernen (§1.8), frischen **MSIX** von claude.com/download als Admin installieren. WARNUNG: kann lokale MCP-Configs/Desktop-Extensions stören → vorher sichern.
**Quelle:** cybersecurityforme.com (extern); GitHub #31516/#52032/#29428 (extern).

### 1.10 VM startet, aber kein Internet (WinNAT/VPN/Proxy)
**Symptom:** Dienst läuft, VM bootet, aber „Can't reach the Claude API from Claude's workspace" / Timeout nach ~60 s. `Get-NetNat` liefert nichts.
**Ursache:** (a) WinNAT-Regel fehlt; (b) **Subnetz-Kollision** — Cowork nutzt hartcodiert `172.16.0.0/24`,
Konflikt mit Firmennetz/VPN; (c) **VPN-Inkompatibilität** (Hyper-V-NAT ignoriert Split-Tunnel); (d)
**Corporate-NTLM-Proxy** — die auf macOS vorhandene lokale Proxy-Brücke fehlt auf Windows strukturell;
(e) Win11 Home ohne volle WinNAT-Unterstützung („invalid class" bei `Get-NetNat`).
**Versionen:** **offen**; NTLM-Proxy-Lücke strukturell (#33946/#29367).
**FIX:** NAT manuell: `New-NetNat -Name "cowork-vm-nat" -InternalIPInterfaceAddressPrefix "172.16.0.0/24"`
(nicht reboot-fest → Scheduled Task). VPN während Cowork **ganz beenden** (nicht nur disconnecten).
WSL2-Netz-Reset: `wsl --shutdown; netsh winsock reset; netsh int ip reset; ipconfig /flushdns` + Reboot.
NTLM-Proxy: Test im Nicht-Firmennetz / IT um Bypass bitten. Firewall: Claude für Private+Public erlauben.
**Quelle:** brycewatson.com / cybersecurityforme.com / elliotsegler.com (extern); GitHub #33946/#29367 (extern).

### 1.11 DCOM 10016 blockiert CoworkVMService (oft nach Home→Pro-Upgrade)
**Symptom:** „VM service not running" + Event-Log **DCOM 10016** (CLSID `{2593F8B9-4EAF-457C-B68A-50F6B8EA6B54}`, APPID `{15C20B67-12E7-4BB6-92BB-7AFF07997402}`).
**Ursache:** MSIX-Container kann das Hyper-V-COM-Objekt nicht aktivieren — fehlende Launch-/Activation-Rechte
für ALL APPLICATION PACKAGES auf der APPID. (Hinweis: in #36801 war die Permission korrekt und es crashte
trotzdem → DCOM ist nicht immer die Ursache.)
**Versionen:** **offen** (#30179).
**FIX:** DCOM-Launch-/Activation-Permissions für die APPID per `dcomcnfg` prüfen/setzen; Bundle-Nuke + Reinstall (§1.6).
**Quelle:** brycewatson.com (extern); GitHub #30179 (extern).

---

## 2. VM-/Workspace-Start — macOS

> Architektur: macOS nutzt Apple Virtualization.framework für die Shell/Code-VM; Datei-Operationen laufen
> nativ. Diagnose-Logs: `~/Library/Logs/Claude/cowork_vm_node.log`, `cowork_vm_swift.log`, `coworkd.log`,
> `main.log`; VM-Bundles unter `~/Library/Application Support/Claude/claude-code-vm/<version>/`.

### 2.1 VM hängt nach macOS-Update (vsock-Timeout-Schleife)
**Symptom:** Workspace lädt nicht mehr. VM bootet, hängt 60 s an `guest_vsock_connect`, Timeout, endlose
Wiederholung. Gescheiterte Retries stoppen die vorige VM nicht sauber; vmnet-Gateway-IP zählt hoch (67.1, 68.1, 69.1…).
**Ursache:** Regression durch macOS 26.3 (Beta 3 und final) — Inkompatibilität zwischen Virtualization.framework/vsock und Coworks VM-Verbindungsaufbau.
**Versionen:** macOS 26.3, Claude Code v2.1.33, Apple Silicon. Issue #23830 als „not planned/stale" → **offiziell ungelöst**.
**FIX:** Datei-/Web-Tools laufen ohne VM weiter (§2.4) — nur Shell/Code fällt aus. Auf macOS- bzw.
Cowork-Update warten; Mac-Neustart löst es laut Berichten nicht dauerhaft.
**Quelle:** GitHub #23830 (offizielles Repo).

### 2.2 VM-SDK „Download failed" trotz erreichbarem Artefakt (kein Fallback)
**Symptom:** „Failed to start Claude's workspace - Download failed. Check your internet connection…",
schlägt schnell (~3,5 s) und identisch auf verschiedenen Netzen fehl. `claude-code-vm/2.1.163` wird bei jedem Start leer neu angelegt.
**Ursache:** Kein Netzproblem, sondern Manifest-/Checksum-Verifikation des serverseitig hochgestuften
VM-Runtimes 2.1.163 (Schritt `download_and_sdk_prepare`). Der Client fällt bei Prepare-Fehler **nicht** auf
das installierte, funktionierende 2.1.161 zurück, sondern brickt den Workspace und zeigt eine irreführende Netz-Meldung.
**Versionen:** macOS Apple Silicon, Desktop 1.11187.2, VM-SDK 2.1.163 (kaputt) vs. 2.1.161 (letzte gute). Windows-Pendant #60660. **Offen.**
**FIX:** Clientseitig nicht behebbar (serverseitiges Re-Publish/Rollback nötig). Hilft NICHT: Reinstall,
`vm_bundles` löschen, „Reinstall workspace", Netzwechsel. Auf neueres Cowork-Update warten.
**Quelle:** GitHub #65649 / #60660 (offizielles Repo).

### 2.3 Weitere macOS-VM-Startfehler (gleiche Symptomklasse)
**Symptom/Ursache (kurz):** „VM service not running" (#27801); „macOS isn't providing a network connection
to Claude's workspace" — fehlende `libmnl.so.0` im VM-Image nach Update v1.1.1520 (#22330); „Session
couldn't be created" bei Cowork-Dispatch (#43991); Workspace-Init scheitert mit `ERR_QUIC_PROTOCOL_ERROR` (#25497).
**FIX:** Reinstall der Workspace (§1.6-Analogon: `~/Library/Application Support/Claude/vm_bundles` löschen),
Neustart, Debug-Logs teilen. Datei-/Web-Tools laufen ohne VM weiter.
**Versionen:** macOS-spezifisch, jeweils **offen**. **Quelle:** GitHub #27801/#22330/#43991/#25497.

### 2.4 „workspace unavailable" — Teil-Degradierung (per Design, KEIN Bug)
**Symptom:** Datei- und Web-Tools laufen, aber Shell-Befehle/Code-Ausführung melden „workspace unavailable".
**Ursache:** Per Design — der Agent-Loop (inkl. Datei-Lesen/-Schreiben, Web) läuft nativ; nur Code-Ausführung
läuft in der isolierten VM. Fällt die VM aus, degradiert nur die Code-Schicht.
**Versionen:** plattformübergreifend, **per Design**.
**FIX:** Erwartungsmanagement — Datei-Aufgaben laufen weiter; für Shell/Code VM-Recovery abwarten (§1, §2.1–2.3).
**Quelle:** support.claude.com/en/articles/14479288 (offiziell, Architektur-Übersicht).

---

## 3. macOS-Permissions / TCC (App-Permission-Layer)

### 3.1 „Always allow" persistiert nicht — Prompt bei jeder Datei-Operation ⭐ HÄUFIG
**Symptom:** Bei JEDER Datei-Lese-/Schreib-Operation ein Permission-Prompt; „Always allow" unterdrückt nichts (auch innerhalb derselben Session, Reads UND Writes).
**Ursache:** Bug im App-Permission-Layer — die „Always allow"-Entscheidung wird nicht für die Session
gespeichert. NICHT die macOS-System-Permission (Privacy & Security → Files and Folders ist korrekt erteilt).
**Versionen:** macOS, Desktop 1.1617.0 (2026-04-09). **Regression, offen** (verwandt #37814).
**FIX:** Kein echter Nutzer-Fix bekannt. Workaround: Anzahl der Operationen reduzieren (Aufgaben bündeln); auf Fix warten.
**Quelle:** GitHub #46205 / #37814 (offizielles Repo).

### 3.2 Scheduled Tasks ignorieren „Always allow" — Prompt bei jedem Lauf
→ Siehe §7.9 (Scheduled-Tasks-Permissions). Kernpunkt: Task-Runner erbt weder die „Always allow"-Auswahl noch `~/.claude/settings.json`-Allow-Rules.

### 3.3 „Failed to load session" — `~/Documents` trotz Full Disk Access nicht lesbar
**Symptom:** Sessions/Projektordner in `~/Documents/` scheitern sofort mit „Failed to load session". Der
`disclaimer`-Sandbox-Wrapper endet mit Code 128: `fatal: Unable to read current working directory: Operation not permitted`. Ordner außerhalb `~/Documents/` (Desktop, Projects) funktionieren.
**Ursache:** Der Helper-Binary `/Applications/Claude.app/Contents/Helpers/disclaimer` hat nicht die nötigen
Entitlements für TCC-geschützte Ordner (Documents/Desktop/Downloads). Full Disk Access hilft nicht, weil
die TCC-Prüfung über den Code-Signature-Pfad läuft.
**Versionen:** macOS Apple Silicon, Claude Code v2.1.64 (~2026-03-09). **Regression, offen.**
**FIX (Workaround):** Projektordner aus `~/Documents/` rausziehen, optional zurück-symlinken:
```
mkdir -p ~/Projects/MeinProjekt && mv ~/Documents/MeinProjekt ~/Projects/MeinProjekt
ln -s ~/Projects/MeinProjekt ~/Documents/MeinProjekt
```
Hilft NICHT: Logout, Reinstall, `tccutil reset`, Full Disk Access für alle Binaries, Reboot.
**Quelle:** GitHub #32584 (offizielles Repo).

### 3.4 Wiederkehrende TCC-Dialoge bei jedem Update/Start
**Symptom:** macOS-TCC-Popup („… would like to access data from other apps") bei jedem Start/erster Nachricht und nach jedem Update erneut; auch „Network volume permission prompt on every update".
**Ursache:** TCC-Grants sind an den versionierten Binary-Pfad gebunden; jedes Update legt eine neue Version
an, der Symlink wird vor der TCC-Prüfung aufgelöst → macOS sieht jede Version als neue App. Embedded CLI erbt Full Disk Access nicht von Claude.app.
**Versionen:** macOS, mehrere Versionen, **offen**.
**FIX:** Nach Updates Full Disk Access erneut erteilen. Kein dauerhafter Fix.
**Quelle:** GitHub #36832/#63130/#53703/#36675/#24162 (offizielles Repo).

---

## 4. Datei-/Ordner-Arbeit & Datenverlust

### 4.1 ⭐ KRITISCH — Datenverlust bei iCloud-/Cloud-ausgelagerten Dateien (`cp`+`rm` auf 0-Byte-Stubs)
**Symptom:** Bei einer Reorganisations-Aufgabe (~110 Dateien aus `~/Documents`) werden Dateien **dauerhaft
zerstört**. iCloud-„Recently Deleted" enthält sie nicht; nur ein separates Time-Machine-Backup rettete die Daten.
**Ursache:** Bei iCloud „Optimize Mac Storage" (analog OneDrive Files-On-Demand) liegen ausgelagerte
Dateien lokal nur als **0-Byte-Platzhalter** vor. Native Apps triggern beim Zugriff den Download — die
Cowork-Linux-VM sieht über den Mount aber nur den 0-Byte-Stub und kann den Download nicht auslösen.
`cp -a` kopiert still 0-Byte-Dateien (kein Fehler); ein folgendes `rm -rf` auf die Originale löscht die
Stubs, was iCloud als bewusste Löschung interpretiert und die Cloud-Originale entfernt. **Stiller, ggf. irreversibler Datenverlust.**
**Versionen:** macOS Sequoia, iCloud „Optimize Mac Storage" an. Cowork Research Preview. Severity: Critical. **Offen.**
**FIX (präventiv, funktionserhaltend):**
- VOR Cowork-Datei-Operationen in Cloud-Ordnern: in Finder alle Dateien markieren → Rechtsklick → **„Download Now"**, Downloads abwarten.
- **Time Machine / Backup** aktuell halten; im Zweifel Aufgabe auf eine Kopie außerhalb des Cloud-Ordners richten.
- Claude anweisen: „move" nur per atomarem `mv`, NIE `cp`+`rm`; vor jeder Löschung Copy-Integrität (Größe ≠ 0) prüfen; `rm -rf` auf Nutzerdaten nur mit expliziter Bestätigung; Dry-Run zuerst.
**Quelle:** GitHub #32637 (offizielles Repo); HN „Cowork Deleted 11GB of files" (extern).

### 4.2 Stale-Mount — Cowork liest veraltete Datei-Inhalte UND -Metadaten
**Symptom:** Cowork liest einen alten Snapshot einer Datei, während die Claude-Code-Tab derselben App die
aktuelle Version liest (gleiche Maschine, gleiche Datei). Auch `stat`/`cat`/`ls -la` melden alte Größe + mtime
→ die Veralterung ist von innen **nicht erkennbar** (stiller Fehler). Überlebt Session-Neustart und App-Reinstall.
**Ursache:** Die VM greift über virtiofs (`/mnt/.virtiofs-root/shared/…`), re-exponiert via FUSE, zu. Diese
Bridge cached Inhalt UND Metadaten und invalidiert nicht bei externen Schreibvorgängen (Obsidian, Word, Explorer, andere Session).
**Folgen:** False Positives bei Scans; destruktive Teil-Überschreibungen (Edit auf Basis eines Stale-Read);
Cowork-eigene Edits lassen sich nicht verifizieren (Post-Edit-Read liest Write-Through-Cache).
**Versionen:** primär Windows reproduziert, Mount-Layer plattform-agnostisch; CC im VM 2.1.92; „never worked". **Offen** (als Duplikat geschlossen).
**FIX:** Für extern geänderte Dateien die Claude-Code-Tab nutzen; vor Cowork-Arbeit externe Editoren schließen; Cowork-Edits extern verifizieren (Session-Neustart hilft laut Bericht nicht zuverlässig).
**Quelle:** GitHub #45433 (offizielles Repo).

### 4.3 Dateien landen im falschen Ordner (tiefe interne outputs-Pfade, „Location not available")
**Symptom:** Output-Dateien jeder neuen Session landen in einem tief verschachtelten internen Pfad
(`…\Claude\local-agent-mode-sessions\…\outputs` bzw. macOS-Pendant). „View in folder" wirft oft „Location
is not available", weil der Pfad zwischen Sessions aufgeräumt wird → gespeicherte Dateien werden unzugänglich.
**Ursache:** Cowork mountet pro Session einen temporären Scratchpad/outputs-Bereich; ohne verbundenen
Zielordner landet alles dort und wird aufgeräumt. Kein persistenter Default-Output-Ordner.
**Versionen:** plattformübergreifend; Feature-Request für Default-Output **offen** (#47179).
**FIX:** Zielordner explizit verbinden und **vollen** Pfad angeben („Save to /Users/<name>/Desktop/output.xlsx",
nicht relative `~/Documents`). Dedizierten Workspace-Ordner anlegen und zu Session-Beginn verbinden. Prüfen,
dass der Ordner wirklich gemountet ist und nicht als „Scratchpad" gilt. Bei Permission-Hängern Claude Desktop
aus „Files and Folders" entfernen und neu hinzufügen.
**Quelle:** GitHub #47179 (offizielles Repo); claudecowork.im/troubleshooting (extern).

### 4.4 Cloud-Storage-Ordner via Symlink nach Update blockiert (macOS)
**Symptom:** Nach einem Update kann Cowork per Symlink ins Home verlinkte Cloud-Ordner (z. B. OneDrive) nicht mehr nutzen.
**Ursache:** Symlinks werden aufgelöst; Cowork sieht das Ziel unter `~/Library`/`~/Library/CloudStorage` und blockt aus Sicherheitsgründen (Verschärfung in neuerer Version).
**Versionen:** macOS, Claude for Mac 1.569.0 (2026-04-02). **Regression, offen.**
**FIX:** Arbeitsordner außerhalb von `~/Library`/CloudStorage halten, bis Overrides erlaubt sind.
**Quelle:** GitHub #44710 (offizielles Repo).

### 4.5 „Request too large (max 32MB). Try with a smaller file."
**Symptom:** Schon nach 2–3 Nachrichten in einem neuen Chat: „Request too large (max 32MB)…" — auch bei kleinen Dateien (Variante „max 20MB" bei 99-KB-PNG nach langen Sessions).
**Ursache:** Zwei Spielarten: (1) echtes Datei-/Request-Größenlimit (~30 MB pro Datei; dichte Text-PDFs
erzeugen viele Tokens); (2) **False Positive** — nach langen Sessions mit Kontext-Compaction wird der
gesamte Request zu groß (Context-Overflow), nicht die Datei.
**Versionen:** macOS, Claude Code 1.5354; als Regression gemeldet, `needs-repro`.
**FIX:** Bei echter Größe Datei aufteilen/verkleinern. Bei False Positive (kleine Datei, lange Session) **neue/frische Session** starten.
**Quelle:** GitHub #56674/#34751/#46655 (offizielles Repo).

---

## 5. Connectors & MCP

> Kernarchitektur: Cowork brokert **Remote-Connectors über Anthropics Cloud**, nicht über das lokale Netz.
> Größte Fehlerklasse: OAuth-/Token-Bindung. Generischer Workaround bei „connected ≠ funktioniert":
> App neu starten + unter **Settings → Capabilities** neu authentifizieren — hilft aber NICHT bei den
> Scope-/Token-Pfad-Bugs (§5.2/§5.3), wo der Ausweichpfad „denselben Connector im Web-Chat/CLI nutzen" ist.

### 5.1 Eigener Remote-MCP-Server aus Cowork unerreichbar (Anthropic-IP-Allowlist nötig) — per Design
**Symptom:** Eigener MCP-Server verbindet nicht aus Cowork, obwohl von der eigenen Maschine erreichbar (VPN/Firmennetz/Firewall).
**Ursache:** **Per Design** — die Verbindung kommt aus **Anthropics Cloud-Infrastruktur**, nicht vom lokalen Netz. Der Server muss öffentlich per HTTPS aus Anthropics IP-Ranges erreichbar sein.
**Versionen:** per Design, alle Clients inkl. Cowork.
**FIX:** Anthropics IP-Adressbereiche (aus dem Support-Artikel) in der Firewall **allowlisten**; Server öffentlich per HTTPS bereitstellen.
**Quelle:** support.claude.com/en/articles/11503834 + /11175166 (offiziell).

### 5.2 Alle claude.ai-gehosteten Connectors liefern NULL Tools (fehlender OAuth-Scope)
**Symptom:** In jeder Cowork-Session liefern Gmail/Drive/Calendar/Web-Search **null Tools**; jeder Aufruf
abgelehnt. Dieselbe Account/Maschine funktioniert in Claude Code CLI und claude.ai-Web. Log:
`[claudeai-mcp] Missing user:mcp_servers scope (scopes=user:inference)`.
**Ursache:** Der Cowork-OAuth-Pfad fordert den Master-Scope `user:mcp_servers` **nicht** an. Da der Scope
Teil des OAuth-Cache-Keys ist, hilft kein Token-Flush, Sign-out oder Reinstall.
**Versionen:** Claude.app 1.1.381, macOS, Plan Max; ~2026-05-21. **Offen** (`area:auth`).
**FIX:** Kein Nutzer-Workaround. Übergangsweise dieselben Connectors in **Claude Code CLI oder Web-Chat** nutzen. Fix muss von Anthropic kommen.
**Quelle:** GitHub #62556 (offizielles Repo).

### 5.3 Custom Remote-Connector authentifiziert, Tool-Calls gehen aber OHNE Token
**Symptom:** Eigener Remote-MCP zeigt nach OAuth „connected" mit allen Tools; sobald der Chat ein Tool nutzt,
erreicht der Call den Server **ohne `Authorization`-Header** → 401. Der angebotene In-Chat-OAuth-Flow schließt nie ab („no OAuth flow in progress").
**Ursache:** Connector-Auth ist nicht an die Chat-Session/Tool-Calls gekoppelt; der In-Chat-Flow nutzt fälschlich `http://localhost:<port>/callback` statt `https://claude.ai/api/mcp/auth_callback`.
**Versionen:** Cowork Desktop, ~2026-06-08. **Offen** (Klasse wie #52565/#52549).
**FIX:** Kein verlässlicher Nutzer-Fix; denselben Server in claude.ai (Web) verwenden.
**Quelle:** GitHub anthropics/claude-ai-mcp #412 (offizielles Repo).

### 5.4 Custom-Connector verliert OAuth nach ~1–2 h / überlebt Neustart nicht
**Symptom:** Connector verliert Auth nach 1–2 h (manuelle Neu-Auth nötig); OAuth-Tokens überleben App-Neustart nicht; Menü-„Connect" scheitert still (In-Chat-Auth funktioniert).
**Ursache:** Refresh-Token wird nicht still genutzt; Token-Persistenz/Menü-Connect-Pfad defekt.
**Versionen:** **offen** (#36933, #52565).
**FIX:** Bei Ablauf manuell reconnecten; statt Menü-„Connect" die **In-Chat-Authentifizierung** nutzen.
**Quelle:** GitHub #36933 / #52565 (offizielles Repo).

### 5.5 Lokale/stdio-MCP-Server & Desktop-Extensions (DXT) NICHT in Cowork
**Symptom:** Als „local dev" oder via `.mcpb`/DXT hinzugefügte Tools funktionieren in Cowork nicht (in Chat
schon). Im Code-Tab macht der Connector-Toggle still nichts (kein Fehler). Tool-Liste zeigt nur HTTP-MCPs.
**Ursache:** Cowork bindet lokale/stdio-MCPs nicht ein (nur Remote/HTTP-Brokering über Cloud). Der Code-Tab
liest nur `~/.claude/mcp_settings.json`, das Desktop-Extensions nicht enthält → Pfad-Mismatch. Zusätzlich
können Admins per MDM `isLocalDevMcpEnabled=false` / `isDesktopExtensionEnabled=false` setzen.
**Versionen:** teils Regression, teils „never worked"; Teilfixes je Build uneinheitlich. **Überwiegend offen.**
**FIX:** Lokalen MCP als **Remote-HTTP-Connector** neu aufsetzen. Auf verwalteten Geräten Admin die MDM-Keys auf `true` setzen lassen.
**Quelle:** GitHub #20377/#23424/#42453/#28775 (offizielles Repo); support.claude.com/en/articles/14479288 (offiziell, MDM).

### 5.6 MCP-Verbindung nach erstem Prompt verloren — Tools ab 2. Nachricht weg
**Symptom:** Erster Prompt: Tools funktionieren. Ab dem zweiten: `Error: No such tool available: mcp__[uuid]__…`.
Die Registry zeigt weiter `connected: true`; App-Neustart hilft nicht.
**Ursache:** Cowork hält die MCP-Verbindung nicht über die Session; Registry-Status und reale Verfügbarkeit driften auseinander (maschinenspezifisch).
**Versionen:** Cowork Desktop, macOS, Stand Januar 2026. **Regression, offen.**
**FIX (Workaround):** Pro Operation einen **neuen Task** starten (der jeweils erste Prompt funktioniert).
**Quelle:** GitHub #18680 (offizielles Repo).

### 5.7 Connector „Connected", exponiert aber keine Tools / leere Antworten ⭐ HÄUFIG
**Symptom:** Connector zeigt „Connected", bietet aber keine Tools an bzw. Tools liefern still **leere Antworten**.
**Ursache:** Leere MCP-Antworten bedeuten meist, dass der MCP-Server die Verbindung verloren hat, obwohl die Registry „connected" anzeigt.
**Versionen:** diverse Connectors/Plattformen, **offen**.
**FIX (offiziell-nah):** Claude Desktop **neu starten** und betroffene MCP-Server unter **Settings → Capabilities** neu authentifizieren.
**Quelle:** GitHub #57589 (offizielles Repo); Cowork-Troubleshooting (extern).

### 5.8 Kontextlimit bei ~10–11 Connectors; „Load tools when needed" greift nicht
**Symptom:** Ab dem ~11. Connector Kontextlimit-Fehler schon **vor der ersten Nachricht**. „Load tools when needed"/„On demand" deferred die Schemata nicht zuverlässig.
**Ursache:** Connector-Schemata sind token-intensiv; der „On demand"-Schalter persistiert/deferred nicht
zuverlässig. (Tool-Search reduziert MCP-Kontext z. B. von 51.000 auf 8.500 Tokens, wenn er greift.)
**Versionen:** claude.ai/Desktop, **offen** (Persistenz-Bug). „On demand ab 10 Connectors" ist offiziell empfohlen.
**FIX:** Bei 10+ Connectors **On demand** aktivieren (Manage Claude's tool access); nicht benötigte Connectors deaktivieren; wenn On demand nicht persistiert, Connector-Zahl manuell reduzieren.
**Quelle:** GitHub #62175/#25892 (offizielles Repo); support.claude.com/en/articles/13730515 (offiziell).

### 5.9 Cloud-Scheduled-Tasks haben keine MCP-Connectors
**Symptom:** Geplante Cloud-Tasks haben keine MCP-Tools in der Session.
**Ursache:** Connectors/Tools werden nicht in die Scheduled-Task-Session injiziert.
**Versionen:** **offen** (#43397).
**FIX:** MCP-abhängige Schritte interaktiv statt geplant ausführen.
**Quelle:** GitHub #43397 (offizielles Repo).

### 5.10 Gmail-Connector kann nur Entwürfe, nicht senden — per Design
**Symptom:** Claude kann `create_draft`, aber **nicht senden**; kein `send`-Tool, kein Attachment-Support.
**Ursache:** **Per Design (Sicherheit)** — nur Entwürfe, um versehentlichen Versand zu verhindern.
**Versionen:** aktueller Stand, per Design (Erweiterung als Feature-Request offen).
**FIX:** Entwurf manuell in Gmail senden.
**Quelle:** support.claude.com/en/articles/10166901 (offiziell); GitHub #28575.

---

## 6. Skills & Plugins

> Roter Faden: Skill-**Metadaten** werden im System-Prompt registriert (Plugin erscheint im Menü), die
> **SKILL.md-Dateien** aber nicht in den Container gemountet → stiller Fehler. Universeller Workaround für
> fast alle Skill-Störungen: **Plugin als ZIP exportieren und manuell hochladen.**

### 6.1 ⭐ KRITISCH — Personal-/Marketplace-Plugin-Skills werden im Cowork-Container nicht gemountet
**Symptom:** Plugin erscheint als installiert/aktiviert (Toggle ON), aber der Skill ist im Container nicht
vorhanden. Claude meldet „It looks like there isn't a [plugin:skill] skill available" oder der Spinner hängt
endlos. Im Skill-Pfad liegen nur die Anthropic-Builtins (docx, pdf, pptx, schedule, skill-creator, xlsx).
**Ursache:** Die Connector-Pipeline des Plugins funktioniert, die Skill-Pipeline registriert nur Metadaten,
mountet die SKILL.md-Dateien aber **nicht** ins Container-Dateisystem. Stiller Fehler ohne UI-Meldung.
**Versionen:** macOS + Windows, seit Launch (Feb 2026), aktiv März/Mai 2026 reproduziert. **Regression, offen.**
**FIX (Workaround):** Plugin als ZIP exportieren und über **Plugins → Add plugin → Upload** hochladen (umgeht
die defekte Marketplace-Pipeline). Beispiel: `cd ~/.claude/plugins/cache/<marketplace>/<plugin> && zip -r ~/plugin-export.zip . -x "*/node_modules/*" -x "*/.DS_Store"`, dann hochladen.
**Quelle:** GitHub #31542/#26254/#26131/#39400 (offizielles Repo).

### 6.2 Windows: Cowork sucht Plugin-Skills am falschen Ort
**Symptom:** Plugin-Skills werden unter Windows nicht gefunden, obwohl aktiviert.
**Ursache:** Fehlerhafte Pfad-Traversierung (`…/mnt/.local-plugins/../../../../../../Local/Packages/`), während die Skills tatsächlich unter `…/mnt/.local-plugins/` liegen.
**Versionen:** Windows 11, seit Launch 10.02.2026, **offen**.
**FIX:** ZIP-Upload (§6.1); ansonsten auf Fix warten.
**Quelle:** GitHub #24859/#26998 (offizielles Repo).

### 6.3 ZIP-Struktur: Ordnername muss = `name`-Feld, Skill-Ordner im ZIP-Root
**Symptom:** Upload schlägt fehl oder Skill wird nach Upload nicht erkannt („File not found" trotz korrekt aussehender Struktur).
**Ursache:** Der Skill-Ordnername muss exakt dem `name`-Feld der SKILL.md-Frontmatter entsprechen, und das ZIP muss den Skill-Ordner als Wurzel enthalten (nicht verschachtelt).
**Versionen:** per Design / Anforderung.
**FIX:** Aus dem Skill-Ordner heraus zippen: `cd skill-ordner && zip -r ../skill.zip .`. Ordnername = `name`-Feld sicherstellen.
**Quelle:** support.claude.com/en/articles/12512198 (offiziell); GitHub #16625.

### 6.4 Upload „Plugin validation failed" (ohne Detail) bei `<…>` oder URL in `description`
**Symptom:** Plugin-/Skill-Upload wird mit generischem „Plugin validation failed" abgelehnt — keine Detailmeldung, kein Hinweis auf Datei/Feld.
**Ursache:** Der Validator HTML/XML-sanitisiert das `description:`-Feld und interpretiert spitze Klammern
`<x>` als Tag (natürlich bei CLI-Platzhaltern wie `<name>`, `<prompt>`). Auch eine **URL** im description-Feld
triggert die stille Ablehnung. Dieselbe SKILL.md akzeptiert die Claude Code CLI problemlos.
**Versionen:** macOS, Claude Code 2.1.153 (2026-05-28), **offen**.
**FIX:** Im `description`-Feld `<…>` durch `[…]` ersetzen und URLs entfernen/umschreiben; ZIP neu bauen; erneut hochladen.
**Quelle:** GitHub #63081/#56517 (offizielles Repo).

### 6.5 Upload akzeptiert nur `.zip` — `.plugin`-Dateien werden abgewiesen
**Symptom:** Datei-Picker zeigt `.plugin`-Dateien, der Upload-Handler akzeptiert aber nur `.zip`.
**Ursache:** Upload-Handler whitelistet ausschließlich die `.zip`-Extension.
**Versionen:** Claude Desktop, **offen**.
**FIX:** Plugin als `.zip` (nicht `.plugin`) verpacken.
**Quelle:** GitHub #40414 (offizielles Repo).

### 6.6 `name`-Feld: nur `[a-z0-9-]`, keine reservierten Wörter
**Symptom:** Skill wird beim Upload/Laden mit Validierungsfehler abgelehnt.
**Ursache:** `name` muss `[a-z0-9-]` folgen (1–64 Zeichen, keine Großbuchstaben/Unterstriche/Leerzeichen/Sonderzeichen, keine doppelten Bindestriche), exakt = Ordnername, keine XML-Tags oder reservierten Wörter („anthropic", „claude").
**Versionen:** per Design.
**FIX:** `name` an das Muster anpassen, Ordnernamen abgleichen, reservierte Wörter vermeiden.
**Quelle:** platform.claude.com/docs (offiziell, Skill best practices).

### 6.7 SKILL.md-Beschreibung > 200 Zeichen wird auf Claude.ai abgeschnitten → triggert nicht
**Symptom:** Skill bleibt „sichtbar", triggert aber nicht mehr zuverlässig — Trigger-Keywords aus der hinteren Hälfte gehen still verloren.
**Ursache:** Claude.ai limitiert die Beschreibung auf **200 Zeichen** (Agent-Skills-Spec erlaubt 1024). In
Claude Code wurde das Listing-Limit versionsabhängig geändert (250 → 1.536 ab v2.1.105; ab 2.1.129 werden
Beschreibungen wenig genutzter Skills ganz verworfen). Truncation passiert teils still.
**Versionen:** Claude.ai-200-Cap per Design; Claude Code versionsabhängig.
**FIX:** Beschreibung kurz halten und die wichtigsten Trigger-Keywords **front-loaden** (nach vorne); auf Claude.ai unter 200 Zeichen bleiben.
**Quelle:** claude.com/docs/skills/how-to (offiziell); GitHub #47627/#40121.

### 6.8 Skill triggert nicht / triggert falsch (System-Prompt-Budget, Überlappung)
**Symptom:** Claude ignoriert einen passenden Skill, oder nutzt den falschen von zwei ähnlichen.
**Ursache:** (a) zu lange/vage Beschreibungen ohne Front-Loading; (b) zu viele Skills/zu lange Beschreibungen
sprengen das System-Prompt-Budget (Gesamtlimit ab Claude Code 2.0.70 standardmäßig 15.000 Zeichen) → Claude
erfährt von Skills nicht; (c) zwei Skills decken dieselbe Aufgabe ab → Kollision.
**Versionen:** laufend, teils per Design (Budget).
**FIX:** Beschreibung präzise + front-loaded („Use when…" + konkrete Trigger-Wörter); überlappende Skills entfernen; offiziellen `skill-creator` mit Description-Optimization nutzen.
**Quelle:** GitHub #20986 (offizielles Repo); platform.claude.com/docs (offiziell).

### 6.9 Aktivierungs-Voraussetzung: „Code execution and file creation" + Cowork + Skills org-weit
**Symptom:** Plugins/Skills lassen sich nicht aktivieren/hochladen, obwohl man in Cowork arbeitet.
**Ursache:** Um Plugins zu nutzen, müssen Skills aktiv sein; dafür muss die Org **„Cloud Code Execution and
File Creation"** einschalten (kontraintuitiv als „nur Chat" gelabelt). Zusätzlich müssen **Cowork und Skills** org-weit aktiviert sein.
**Versionen:** per Design.
**FIX:** In Org-Settings „Code execution and file creation" + Cowork + Skills aktivieren.
**Quelle:** support.claude.com/en/articles/13837433 + /13837440 (offiziell).

### 6.10 Plugin-Hooks & Sub-Agents nur in Cowork — in Chat ausgegraut (+ echter Hook-Bug)
**Symptom:** Hooks/Sub-Agents eines Plugins erscheinen in Chat ausgegraut. Zusätzlich: Plugin-Hooks aus `hooks/hooks.json` feuern auch in Cowork-Desktop-Sessions nie.
**Ursache:** Per Design laufen Hooks/Sub-Agents nur in Cowork; nur **Skills + Connectors** wirken über alle
drei Oberflächen. Plugin-Sub-Agents unterstützen aus Sicherheit `hooks`/`mcpServers`/`permissionMode` nicht.
Echter Bug daneben: `--setting-sources user` schließt den Plugin-Scope aus bzw. Sandbox-Plattform-Mismatch → Hooks werden nicht discovert.
**Versionen:** Ausgrauen per Design; Hook-Bug **offen** (#27398/#40495).
**FIX:** Ausgegraut in Chat = erwartet → in Cowork arbeiten. Für den Hook-Bug auf Fix warten (kein sauberer Workaround).
**Quelle:** support.claude.com/en/articles/13837440 (offiziell); GitHub #27398 (offizielles Repo).

### 6.11 Windows: Plugins komplett ausgegraut / `plugins.claude.ai` 404
**Symptom:** In Cowork Windows ist der gesamte Plugin-Bereich ausgegraut; Install scheitert. Auf macOS Plugin-Install mit HTTP 404. Auch „Internal server error" beim Skill-Upload (Web + Desktop).
**Ursache:** `plugins.claude.ai` löst nicht auf bzw. serverseitiger Fehler im Upload-Endpoint.
**Versionen:** Windows (#51605) + macOS (#26951), **offen**.
**FIX:** Lokale Plugins per **ZIP-Upload** (§6.1) installieren; Upload erneut versuchen; vorher §6.3–6.6 ausschließen.
**Quelle:** GitHub #51605/#26951/#26310 (offizielles Repo).

---

## 7. Scheduled Tasks (geplante Aufgaben)

> Kernmuster: **Lokale Desktop-Tasks sind client-/fokus-/wach-gebunden** (kein serverseitiger Trigger). Für
> Robustheit ist die **Cloud-Routine** die offizielle Alternative (läuft auf Anthropic-Infrastruktur).
> Jeder Lauf startet **frisch ohne Erinnerung** → Prompt muss selbst-enthaltend sein.

### 7.1 Task läuft nur bei wachem Rechner + offener App; verpasste Läufe feuern gebündelt — per Design
**Symptom:** Cron-Tasks feuern nicht zur geplanten Zeit, wenn die App geschlossen / der Rechner im Sleep ist; beim nächsten App-Start feuern ausstehende Tasks quasi gleichzeitig.
**Ursache:** Der Scheduler prüft „jede Minute, solange App läuft + Rechner wach". Kein Server-Trigger.
**Versionen:** macOS/Windows, **per Design** (gebündeltes Nachfeuern überschneidet sich mit §7.2).
**FIX:** Settings → Desktop app → General → **„Keep computer awake"** aktivieren (zugeklappter Laptop schläft
trotzdem). Für Läufe bei ausgeschaltetem Rechner stattdessen eine **Remote Routine (Cloud)** anlegen.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (offiziell); GitHub #44128 (extern).

### 7.2 Catch-up-Fallstrick: nur EIN verpasster Lauf, evtl. zur falschen Uhrzeit — per Design
**Symptom:** Ein für 9 Uhr geplanter Task läuft erst um 23 Uhr; ein 6 Tage verpasster Task läuft beim Aufwachen nur **einmal**, ältere Läufe werden verworfen.
**Ursache:** Beim App-Start/Aufwachen prüft Desktop die letzten 7 Tage und startet **genau einen** Catch-up-Lauf für die zuletzt verpasste Zeit — zur falschen Tageszeit, ohne das zu „wissen".
**Versionen:** **per Design** (Doku „Missed runs"), kein Fix geplant.
**FIX:** Zeit-Guardrails in den Prompt schreiben, z. B.: „Only review today's commits. If it's after 5pm, skip and just summarize what was missed." Für zeitkritische Tasks Cloud-Routine.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (offiziell).

### 7.3 ⭐ KRITISCH — High-Frequency-Cron → unrecoverable Startup-Freeze
**Symptom:** Nach einem Task mit häufigem Cron (z. B. `0 21,22,23,0,1,2,3,4,5,6 * * *`) friert Claude Desktop
bei **jedem** Start ein — keine UI, stiller Hang. Cache löschen / VM neu bauen / PC-Neustart helfen **nicht**.
**Ursache:** Der Catch-up-Mechanismus (§7.2) erkennt beim Start einen verpassten Lauf und hängt in der
Initialisierung **bevor** die UI rendert → Boot-Loop. High-Frequency-Cron garantiert bei jedem Start einen Catch-up-Trigger.
**Versionen:** v2.1.71, Windows (#32213) + macOS (#32167/#32125), **offen**.
**FIX (funktionserhaltend):** Task-Config umbenennen, damit die App wieder bootet —
Windows: `Rename-Item "$env:APPDATA\Claude\local-agent-mode-sessions\<session-uuid>\<task-uuid>\scheduled-tasks.json" "scheduled-tasks.json.bak"`; macOS: analog. Danach **moderate** Cron-Frequenz nutzen.
**Quelle:** GitHub #32213/#32167 (offizielles Repo).

### 7.4 Tasks feuern nur, wenn der Cowork-Tab aktiv fokussiert ist
**Symptom:** Ein für 15:00 geplanter Task feuert erst ~16:20, sobald man manuell in den Cowork-Tab klickt. App war offen, aber auf Chat-Ansicht.
**Ursache:** Der Scheduler scheint aktiv fokussierten Cowork-View zu brauchen (widerspricht der Doku „regardless of active view" → Bug, nicht Design).
**Versionen:** macOS + Win11, Stand März 2026, **offen**.
**FIX:** Cowork-Tab geöffnet/fokussiert lassen; für verlässliche Läufe Cloud-Routine.
**Quelle:** GitHub #36131 (offizielles Repo).

### 7.5 Task dispatcht, führt aber nie aus → „Running" mit 0 Turns → permanente Skip-Cascade
**Symptom:** Task dispatcht termingerecht, aber der Agent-Turn startet nie. Entweder leere Session (0 Turns)
oder die Session bleibt unbegrenzt „Running" (25+ Min) → alle folgenden Slots werden **Skipped**. Ein Zombie = dauerhafter DoS für den Task; Löschen+Neuanlegen hilft nicht.
**Ursache:** Dispatcher und Agent-Runtime entkoppelt — Dispatch erfolgreich, Worker markiert „Running", führt aber weder aus noch gibt frei. Kein Watchdog.
**Versionen:** v1.2278.0, Win11 Pro, **Regression, offen**.
**FIX:** App komplett neu starten; betroffene Tasks pausieren/löschen; zeitweise auf Cloud-Routine ausweichen.
**Quelle:** GitHub #47899 (offizielles Repo).

### 7.6 „Failed to create scheduled task" bei UNC-umgeleitetem Documents-Ordner
**Symptom:** Task-Erstellung schlägt zu 100 % fehl mit „Failed to create scheduled task." Reproduzierbar bei Documents-Ordner per Group-Policy-Folder-Redirection auf UNC (`\\server\share`).
**Ursache:** Der Task-Anlege-Pfad kommt mit UNC-Pfaden (statt lokalem Laufwerksbuchstaben) nicht zurecht.
**Versionen:** Claude Code 2.1.123, Windows, **offen** (Mai 2026).
**FIX:** Working-Folder des Tasks auf einen **lokalen** Pfad setzen; Folder-Redirection für den genutzten Ordner umgehen.
**Quelle:** GitHub #56001 (offizielles Repo).

### 7.7 `/schedule` scheitert: `create_scheduled_task`-Tool nicht im Session-Kontext (Windows)
**Symptom:** In einem Cowork-Task meldet Claude, `create_scheduled_task` sei nicht in der Tool-Liste, und versucht erfolglose Bash-Fallbacks (`crontab`, `gh`). Es entsteht kein Task.
**Ursache:** Das MCP-Tool wird in diesem Kontext nicht in den Session-Tool-Kontext injiziert.
**Versionen:** Claude 1.1.4328, Windows, **Regression, offen**.
**FIX:** Task per UI anlegen (Routines → New routine → Local) oder in einer normalen Desktop-Session per Klartext bitten.
**Quelle:** GitHub #29022 (offizielles Repo).

### 7.8 „Cannot create scheduled tasks from within a scheduled task session" — per Design
**Symptom:** Ein laufender Scheduled-Task kann keinen Folge-Task anlegen (Fehlermeldung, kein Workaround genannt).
**Ursache:** Bewusste Sperre gegen Rekursion/Task-Explosion.
**Versionen:** v2.1.74; aktuell **per Design** (Issue fordert Lockerung).
**FIX:** Der Task darf seinen **eigenen** Zeitplan/Prompt per `update_scheduled_task` ändern (offiziell unterstützt, z. B. Self-Reschedule). Echte Folge-Tasks außerhalb der Task-Session anlegen.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (offiziell); GitHub #34931 (extern).

### 7.9 Permissions werden bei jedem Lauf neu abgefragt; Run stallt im „Ask"-Mode
**Symptom:** Im Ask-Mode stallt der Lauf bis zur manuellen Freigabe; „Always allow" wird nicht automatisch über alle Tools geerbt (verwandt §3.1).
**Ursache:** Jeder Task hat einen eigenen Permission-Mode; nicht abgedeckte Tools blockieren im Ask-Mode. Der Task-Runner erbt `~/.claude/settings.json`-Allow-Rules nicht zuverlässig.
**Versionen:** überwiegend per Design (offizielle Doku); Vererbungs-Lücke als Bug gemeldet (#47180/#40470).
**FIX:** Nach Anlegen **„Run now"** klicken, bei jedem Permission-Prompt **„always allow"** wählen → künftige
Läufe dieses Tasks genehmigen dieselben Tools. Genehmigungen pro Task auf der Detailseite reviewbar.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (offiziell); GitHub #47180/#40470 (offizielles Repo).

### 7.10 Jeder Lauf startet frisch ohne Erinnerung — per Design
**Symptom:** Tasks „erinnern" sich nicht an vorherige Läufe; können auf veraltete/uncommittete Working-Directory-Zustände treffen.
**Ursache:** Per Design — jeder fällige Task startet eine frische, unabhängige Session gegen den aktuellen (auch uncommitteten) Zustand.
**Versionen:** **per Design**.
**FIX:** Prompt vollständig selbst-enthaltend formulieren (Connectors, Format, Präferenzen, Zeit-Guardrails). Beim Anlegen den **Worktree-Toggle** für einen isolierten Git-Worktree pro Lauf aktivieren.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (offiziell).

---

## 8. Live-Artefakte

> Kernmuster: **lokal-only**, Sandbox-beschränkt, Connector-Nutzung **ohne Rückfrage**; MCP-Response-Shape
> muss beidseitig (`content` + `structuredContent`) behandelt werden. Reads sind gecached → Refresh-Button.

### 8.1 `callMcpTool` erreicht lokale stdio-MCP-Server beim Cold Start nicht (400)
**Symptom:** Artefakt schlägt nach Cold Start mit `Tool call failed: 400` fehl, wenn es lokale stdio-MCP-Server aufruft; der lokale Server zeigt nur `initialize`+`tools/list`, nie `tools/call`.
**Ursache:** `callMcpTool()` routet über das claude.ai-Remote-MCP-Relay und setzt den **Config-Key als Name**
(z. B. `macbook-mcp`) in den Pfad, wo das API eine **UUID** erwartet → 400. Lazy-Registration-Bug: lokale stdio-Server werden beim Start nicht beim Relay registriert.
**Versionen:** Desktop 1.5354.0, macOS, **offen**.
**FIX (Workaround):** Vor dem Öffnen/Refresh des Artefakts in einer normalen Chat-Session **einmal** ein Tool dieses MCP-Servers direkt aufrufen → danach funktioniert der Artefakt-Pfad für den Rest der Session.
**Quelle:** GitHub #55788 (offizielles Repo).

### 8.2 MCP-Tool-Response-Shape: `content` wird ignoriert, nur `structuredContent` gezeigt ⭐ HÄUFIG
**Symptom:** Gibt ein MCP-Tool sowohl `content` (Nutzdaten) als auch `structuredContent` (Metadaten) zurück, wird nur `structuredContent` angezeigt — die echten Daten verschwinden (z. B. nur Pagination statt Liste).
**Ursache:** Die Rendering-Logik priorisiert ausschließlich `structuredContent`. Relevant für Artefakte, da
`callMcpTool` denselben `{content, structuredContent, isError}`-Shape liefert. (Spiegel-Bug #4427: anderswo wird `structuredContent` umgekehrt ignoriert.)
**Versionen:** v2.0.67, darwin, **offen**.
**FIX (funktionserhaltend):** Im Artefakt-Code **beide** Felder defensiv lesen: `r.structuredContent ?? JSON.parse(r.content[0].text)`. Alternativ das Tool so bauen, dass die Nutzdaten auch in `structuredContent` liegen.
**Quelle:** GitHub #15412/#4427 (offizielles Repo).

### 8.3 Live-Artefakte nutzen Connectors OHNE Rückfrage — per Design (Sicherheit)
**Symptom:** Anders als normale Sessions fragen Artefakte **nicht** um Erlaubnis, bevor sie Connectors verwenden — ein schreibfähiger Connector kann ohne Bestätigung Daten verändern.
**Ursache:** Per Design — Artefakte dürfen die bei Erstellung/Update genehmigten Connectors zur Laufzeit ohne Rückfrage nutzen.
**Versionen:** **per Design** (Doku „Current limitations").
**FIX:** Beim Erstellen/Updaten möglichst nur **Read-only-Connectors** einbinden; genehmigte Connectors minimal halten.
**Quelle:** support.claude.com/en/articles/14729249 (offiziell).

### 8.4 Live-Artefakte sind (noch) nicht teilbar / nicht geräteübergreifend — per Design
**Symptom:** Artefakt lässt sich nicht teilen; auf anderem Gerät nicht verfügbar.
**Ursache:** Per Design zum Launch — Artefakte leben **lokal** (nicht Cloud); Sharing ist „on the roadmap".
**Versionen:** **per Design** seit Launch (April 2026).
**FIX:** Für portable Ergebnisse den generierten Output (Datei/Export) weitergeben.
**Quelle:** support.claude.com/en/articles/14729249 (offiziell).

### 8.5 Caching von Reads + kein localStorage/sessionStorage
**Symptom:** Artefakt zeigt kurz gecachte (leicht veraltete) Connector-Daten; Artefakt-Code mit `localStorage`/`sessionStorage` funktioniert nicht / persistiert nicht.
**Ursache:** Per Design — kurzer Read-Cache für schnelles Laden; Browser-Storage-APIs in der Artefakt-Sandbox gesperrt.
**Versionen:** **per Design**.
**FIX:** Für frische Daten den **Refresh-Button** im Artefakt-Header nutzen. Zustand über Komponenten-State (React `useState`) halten; für Persistenz Daten in Dateien/Connectors speichern lassen. Visualisierung nur mit Chart.js/Grid.js/Mermaid (per CDN erlaubt).
**Quelle:** support.claude.com/en/articles/14729249 (offiziell); coworkhow.com (extern).

### 8.6 Desktop-MCP-Client cached Manifest/Tools — neue Tools nicht dynamisch sichtbar
**Symptom:** Geänderte/neu hinzugefügte Tools eines MCP-Servers werden nicht dynamisch übernommen — ein Artefakt, das ein frisch geändertes Tool aufrufen soll, sieht es nicht.
**Ursache:** Tool-Definitionen werden beim Session-Start gelesen; Config-Änderungen mitten in der Session greifen nicht.
**Versionen:** Desktop-MCP-Client, **offen**.
**FIX:** Claude Desktop neu starten, damit MCP-Config/Tools neu gelesen werden, bevor man das Artefakt nutzt.
**Quelle:** GitHub #7519 (offizielles Repo).

---

## 9. Sicherheit & Prompt-Injection ⭐ KRITISCH

> Anthropic sagt ausdrücklich: das Restrisiko ist **„non-zero"**. Schutzschichten (RL-Training,
> Content-Classifiers, Lösch-Bestätigung, Per-App-Permissions) sind real, aber explizit keine Garantie.

### 9.1 ⭐ KRITISCH — Datei-Exfiltration via Files API (PromptArmor-PoC, belegt)
**Symptom/Risiko:** Ein bösartiges Dokument im verbundenen Ordner bringt Cowork dazu, die sensibelste Datei
(PoC: Kreditunterlagen mit Teil-SSN) per `curl` an die Anthropic Files API hochzuladen — ins Anthropic-Konto
des **Angreifers**, **ohne jede menschliche Bestätigung**.
**Ursache:** Die Cowork-VM sperrt fast allen Egress, aber `api.anthropic.com` ist allowgelistet (für
Package-Manager). Der Angreifer legt seinen eigenen API-Key in die Injection; die Datei geht an sein Konto.
Tarnung: `.docx`, das sich als Claude-„Skill" ausgibt, Injection in 1-pt-Schrift, weiß-auf-weiß. Funktioniert
auch über Web (Claude in Chrome) und MCP-Server. Ursprungslücke „Claude Pirate" (Embrace The Red, Okt 2025)
wurde unverändert von Cowork geerbt.
**Versionen:** Cowork macOS/Windows, Research Preview; PoC ~2 Tage nach Launch. Auch **Opus 4.5 erfolgreich manipuliert**. **Per Design / nicht gefixt** zum Recherchezeitpunkt.
**FIX/Gegenmaßnahme:** Keine sensiblen lokalen Ordner verbinden; **dedizierten** Arbeitsordner statt breitem
Zugriff; **„Ask before acting"** als Standard; „Act without asking" meiden; Tasks überwachen und bei
unerwartetem Datei-/Netzzugriff sofort stoppen; hochgeladene `.docx`-„Skills" sind verdächtig (kanonische Skills sind Markdown).
**Quelle:** promptarmor.com/resources/claude-cowork-exfiltrates-files (extern); embracethered.com „Claude Pirate" (extern); support.claude.com/en/articles/13364135 (offiziell, Gegenmaßnahmen).

### 9.2 DoS durch fehlformatierte Dateien
**Symptom/Risiko:** Eine Datei, deren Endung nicht zum echten Typ passt (z. B. `.pdf`, real Text), löst nach einem Lesversuch in **jedem folgenden Chat derselben Konversation** wiederholte API-Fehler aus.
**Ursache:** Die API verarbeitet Typ-Mismatch nicht robust; der Fehler propagiert über die Konversation. Gezielte Ausnutzung per Injection plausibel.
**Versionen:** Cowork Research Preview, beobachtet Januar 2026; Status unklar/vermutlich offen.
**FIX:** Betroffene Konversation neu starten; Dateitypen vor dem Lesen prüfen.
**Quelle:** promptarmor.com (extern).

### 9.3 „Act without asking" erhöht Injection-Risiko massiv — per Design
**Symptom/Risiko:** Claude arbeitet ohne Zwischenfreigaben; bei erfolgreicher Injection keine Stopp-Chance mitten im Task.
**Ursache:** Per Design — der Modus überspringt Genehmigungen → größere Angriffsfläche.
**FIX:** Nur bei aktiver Überwachung + vertrauten Dateien/Sites/Tools nutzen; im Zweifel „Ask before acting". (Löschen fragt in beiden Modi immer nach.)
**Quelle:** support.claude.com/en/articles/13364135 (offiziell).

### 9.4 Computer Use hat KEINE Sandbox — direkte Desktop-Kontrolle ⭐ KRITISCH
**Symptom/Risiko:** Claude klickt/tippt/navigiert direkt auf dem echten Desktop (Screenshots sehen alles
Sichtbare inkl. PII). Ein Klick auf einen Link in App A öffnet ihn in Chrome, **auch ohne Chrome-Freigabe**.
**Ursache:** Per Design — anders als Datei-Ops (Permission-Checks) und Code (VM-Sandbox) gibt es **keine** Sandbox zwischen Claude und Apps.
**Versionen:** **Research Preview**, nur **Pro/Max** (Team/Enterprise kein Zugriff), macOS + Windows. Tier-Modell: Browser/Trading=read, Terminal/IDE=click, Rest=full.
**FIX:** Sensible Apps (Banking/Healthcare/Government) auf die Blocklist (Investment/Krypto default geblockt);
sensible Fenster vorher schließen; mit Low-Stakes-Aufgaben starten; überwachen. **Links aus Mail/Doku NIE per Computer-Use anklicken** — URL bewusst über die Chrome-MCP öffnen, volle URL vorher prüfen.
**Quelle:** support.claude.com/en/articles/14128542 (offiziell).

### 9.5 Claude in Chrome — JavaScript-Ausführung & Session-Zugriff
**Symptom/Risiko:** Claude führt JS auf Seiten aus → Zugriff auf alles, was der Browser auf der Seite kann
(Login-Sessions, Cookies, gespeicherte Site-Daten). Bei Injection potenziell Credential-Leak / Aktionen in eingeloggten Sessions. Token-Output-Filter sind **keine** Sicherheitsgrenze.
**Ursache:** Per Design — Primärschutz ist das **Per-Domain-Permission-System** (JS braucht pro Domain separate Freigabe).
**Versionen:** Beta, alle Bezahlpläne, **nur Google Chrome**. Anthropic nennt ~1 % Attack-Success-Rate (Opus 4.5) — explizit nicht null. Default geblockt: Banking, Trading, Adult, Krypto, Piraterie.
**FIX:** Separates Browser-Profil ohne sensible Accounts; nur vertrauenswürdige Sites; Aktionen vor Freigabe prüfen. Team/Enterprise: Allow-/Blocklists.
**Quelle:** support.claude.com/en/articles/12902428 (offiziell).

### 9.6 Datenschutz-/Compliance-Lücken (per Design)
**Symptom/Risiko:** Cowork-Aktivität ist **nicht** in Compliance API / Audit-Logs / Daten-Exporten; EDR sieht
nicht in die VM; **Netzwerk-Egress-Permissions gelten NICHT für web fetch/web search/MCPs** (inkl. Claude in
Chrome); Verlauf liegt lokal (nicht zentral verwalt-/exportierbar); Cross-App-Datenfluss (Excel↔PowerPoint-Add-ins) ohne explizite Anweisung.
**Ursache:** Per Design / aktueller Stand der Architektur.
**FIX:** Team/Enterprise: **OpenTelemetry**-Monitoring (kein Ersatz für Audit-Logging; Collector-Domain
allowlisten, sonst still verworfen). Web-Search org-weit abschaltbar (Org Settings → Capabilities). Keine
sensiblen Daten in Excel/PowerPoint-Add-ins bei aktivem Cowork. Lokale Daten in Backup-/MDM-Strategie einbeziehen.
**Quelle:** support.claude.com/en/articles/13364135 + /14479288 + /13455879 (offiziell).

---

## 10. Per-Design-Grenzen & Usage (dauerhafte Fallen, kein Bug)

### 10.1 Aufgabe stoppt, wenn App geschlossen wird oder Rechner schläft
**Ursache:** Per Design — App muss offen bleiben, Rechner wach; sonst endet die Session (auch geplante Tasks). Mobile (Pro/Max) nur als Fernsteuerung des aktiven Desktops.
**FIX:** App offen lassen; „Keep computer awake" bzw. Sleep für lange Aufgaben deaktivieren.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell).

### 10.2 Kein Gedächtnis zwischen Standalone-Sessions
**Ursache:** Per Design — Memory gibt es nur **innerhalb von Projects**, nicht über Standalone-Sessions.
**FIX:** Wiederkehrende Arbeit in einem **Project** organisieren; globale Vorgaben via Settings → Cowork + Folder-Instruktionen.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell).

### 10.3 Kein Teilen von Sessions; nur Desktop (kein Web/Mobile-Eigenbetrieb)
**Ursache:** Per Design — kein Chat-/Artefakt-Sharing; Cowork läuft nur in der Desktop-App (Code lokal in VM).
**FIX:** Ergebnisse als fertige Dateien exportieren und teilen; Desktop-App nutzen.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell).

### 10.4 „API Error: Rate limit reached" trotz freiem Kontingent
**Symptom:** Banner „Rate limit reached", obwohl das Konto weit unter dem Limit ist.
**Ursache:** Transienter API-/Backend-Fehler (nicht echtes Kontingent-Ende).
**FIX:** Kurz warten und Aufgabe erneut anstoßen; ggf. App neu starten. Bei echtem Limit in normalen Chat ausweichen.
**Quelle:** support.claude.com/en/articles/12466728 (offiziell, Fehlertexte); extern.

### 10.5 Cowork verbraucht deutlich mehr Usage als Chat (Limit-Wand) ⭐ HÄUFIG
**Symptom:** Usage-Limit wird in Cowork überraschend schnell erreicht — auch Max-Nutzer berichten harte Wände.
**Ursache:** Per Design — mehrstufige Aufgaben (Sub-Agenten, viele Tool-Calls) sind rechenintensiv; Verbrauch skaliert mit Task-Komplexität.
**FIX:** Verwandte Arbeit in einzelne Sessions bündeln; einfache Aufgaben im normalen Chat; Verbrauch unter Settings → Usage beobachten.
**Quelle:** support.claude.com/en/articles/13345190 (offiziell).

### 10.6 Team: Cowork ist alles-oder-nichts (keine granulare Steuerung)
**Ursache:** Per Design — der Cowork-Toggle ist org-weit; granulare Kontrolle nur Enterprise via Gruppen/Custom Roles; Projects haben keine Admin-Restriktion.
**FIX:** Enterprise + Gruppen/Custom Roles für selektive Aktivierung; auf Team nur global an/aus.
**Quelle:** support.claude.com/en/articles/13455879 (offiziell).

---

## 10a. Git / GitHub-Push aus Cowork — committen geht, push scheitert ⭐ HÄUFIG

> **➜ Vollständiger, kuratierter Almanach für diesen Bereich:** [`cowork-git-push.md`](cowork-git-push.md) — 22 Einträge (Lock, fileMode, Symlinks, Git-LFS, CRLF, Mount-Truncation/Datenverlust-Wächter, non-fast-forward/Plumbing, GIT_DIR/Work-Tree, Performance) inkl. Fix-Status. Dieser §10a-Block ist die Kurzfassung; Details dort.

> Recherchiert 2026-06-15 (7 Researcher: offizielle Anthropic-/GitHub-Doku, GitHub-Issues,
> Reverse-Engineering-Analysen). Gegenseite: best-practices-cowork.md §3a. Frank-zugewandte
> Schritt-Anleitung: `~/proggs/COWORK-GIT-PUSH-SETUP.md`.

### 10a.1 `git push` scheitert mit „could not read Username for github.com" ⭐ HÄUFIG — selbst erlebt
**Symptom:** In Cowork lässt sich committen, aber `git push` bricht ab mit
`could not read Username for github.com` (bzw. fragt nach Username/Passwort). `git fetch`/`ls-remote`
auf ein **öffentliches** Repo geht anonym, Push aber nicht.
**Ursache:** Die Cowork-Linux-VM hat **keinen Git-Credential-Manager, kein `gh` CLI und keinen
GitHub-Push-Proxy**. Der GitHub-Connector der Desktop-App ist eine reine **API**-Integration
(Dateien lesen, Issues/PRs) und liefert **keine** Git-Push-Credentials. Der scoped Push-Proxy
(übersetzt eine Sandbox-Credential in den echten Token) existiert nur in **„Claude Code on the web"**,
NICHT in der Cowork-Desktop-VM.
**Versionen:** Cowork Desktop 2026 (macOS+Windows), z. B. App v1.8555.2.0. Offen, kein nativer Fix.
**FIX (funktionserhaltend, sitzungsweit):** Token-Auth in der VM hinterlegen —
`git config --global credential.helper store` + `~/.git-credentials` mit
`https://Pepsi1978:<TOKEN>@github.com`. **Hält aber nur die laufende Session** (siehe 10a.2).
**Quelle:** support.claude.com/en/articles/10167454 (offiziell, Connector=nur API);
code.claude.com/docs/en/claude-code-on-the-web (offiziell, Push-Proxy nur Web);
GitHub anthropics/claude-code #27344/#13212 (Git-Auth-Proxy), #2911 (SSH ohne Prompt unmöglich).

### 10a.2 ⭐ KRITISCH — der Push-Fix hält nur EINE Session (VM-Home ist ephemer)
**Symptom:** Nach erfolgreichem Setup (10a.1) funktioniert Push — beim nächsten Cowork-Start
ist die Anmeldung wieder weg, Push scheitert erneut.
**Ursache:** Die Cowork-VM startet bei jeder Session mit **frischem Dateisystem** (Session-Disk
wird pro Boot neu ext4-formatiert; „clean VM state" ist offiziell Absicht). `~/.git-credentials`,
`~/.gitconfig`, `~/.ssh` im **VM-Heimverzeichnis** überleben NICHT. **Persistent ist nur, was im
gemounteten Host-Ordner liegt** (VirtioFS-Mount = der echte Windows/macOS-Ordner). Zusätzlich sind
die VM-**Mount-Pfade nicht-deterministisch** zwischen Sessions (Issue #54483) → absolute Pfade brechen.
**Versionen:** Cowork 2026 (macOS ephemer; Windows `sessiondata.vhdx` teils persistent, aber
korruptionsanfällig → unzuverlässig).
**FIX (DAUERHAFT, funktionserhaltend):** Zugangsdaten in `.git/` des gemounteten Repos ablegen —
überlebt jede Session und wird nie committet:
```bash
# im proggs-Repo (Remote bleibt HTTPS, NICHT ändern):
git config credential.helper 'store --file=.git/credentials'   # LOKAL, nicht --global
printf 'https://Pepsi1978:%s@github.com\n' '<TOKEN>' > .git/credentials
chmod 600 .git/credentials
git ls-remote origin -h refs/heads/main   # Test, verändert nichts
```
- **Relativer** Pfad `.git/credentials` (umgeht die wechselnden Mount-Pfade).
- `credential.helper` **lokal** (nicht `--global` → das läge im flüchtigen VM-Home).
- `.git/` wird per Definition NIE committet → Token landet nie auf GitHub.
**Quelle:** git-scm.com/docs/git-credential-store (offiziell); GitHub anthropics/claude-code
#54483 (Mount-Pfade nicht-deterministisch); blog.pluto.security / pvieito.com (VM ephemer, Reverse-Eng.).

### 10a.3 FALLE — `.git/config` ist mit dem Host-Terminal geteilt
**Symptom:** Nach `git remote set-url origin git@github.com:...` (SSH) oder `https://<TOKEN>@github.com/...`
in Cowork funktioniert plötzlich auch das **normale Windows-/macOS-Terminal** nicht mehr wie vorher.
**Ursache:** Der gemountete Ordner ist physisch derselbe — `.git/config` (inkl. Remote-URL,
`core.sshCommand`) ist **zwischen Cowork-VM und Host-CLI geteilt**. Eine Remote-/SSH-Umstellung in
Cowork ändert das Setup auch für das Terminal (das den VM-Key/SSH-Pfad nicht hat).
**Versionen:** Cowork 2026, jedes Repo in einem gemounteten Ordner.
**FIX:** Remote-URL NICHT ändern. Den **additiven** `credential.helper store --file=.git/credentials`
nutzen (10a.2) — der ergänzt nur einen zweiten Auth-Weg und lässt die Host-CLI (Windows Credential
Manager) unberührt. SSH-Deploy-Key (liefe nie ab) NICHT empfehlen, solange `.git/config` geteilt ist.
**Quelle:** git-scm.com (credential.helper additiv); Recherche 2026-06-15.

### 10a.4 Token-Hygiene für den Dauer-Token (privates Repo!)
**Symptom/Risiko:** Dauerhaft gespeicherter Token in `.git/credentials` (Klartext auf der Host-Platte).
**Ursache/Fakten:** (a) Fine-grained PAT max. **366 Tage** Laufzeit → muss danach erneuert werden
(kein „nie ablaufen" ohne Policy-Ausnahme). (b) **`proggs` ist PRIVAT** → GitHub-Secret-Scanning
sperrt ein geleaktes Token **NICHT automatisch** (Auto-Revoke nur bei öffentlichen Repos) → bei Leak
**manuell** widerrufen. (c) Reines Push braucht **Contents: Read and write** + **Metadata: Read**
(Metadata wird leicht vergessen → sonst `403`).
**FIX:** Fine-grained PAT, nur Repo `proggs`, nur Contents:RW (+Metadata:R), kürzeste praktikable
Laufzeit + Rotation. Datei `chmod 600`. In Cowork keine fremden/sensiblen Ordner verbinden
(Prompt-Injection-Risiko, §9) — minimaler Token-Scope begrenzt den Schaden.
**Quelle:** docs.github.com (PAT-Permissions, Token-Expiration, Secret-Scanning private vs public);
cloudsecurityalliance.org (Least-Privilege gegen Prompt-Injection).

### 10a.5 ⭐ KRITISCH — Auth steht, aber `git commit`/`push` AUS der VM hängt an nicht-löschbaren `.lock`-Dateien (Windows)
**Symptom:** Die dauerhafte Push-Anmeldung (10a.2) funktioniert — ein echter Tag-Push lief durch
(`* [new tag] …`, Rückgabecode 0). ABER: Jede Git-Schreibaktion aus der Cowork-VM hinterlässt eine
`.lock`-Datei im gemounteten `.git`, die die VM nicht wieder entfernen kann (`rm` und Umbenennen →
`Operation not permitted`). Die nächste Schreibaktion blockiert daran. Zusätzlich zeigt die VM einen
**Phantom-`index.lock`** im Mount-Cache, der auf dem Windows-Host gar nicht existiert (`del` →
„Cannot find path").
**Ursache:** virtiofs/CBFS-Mount auf Windows — die VM kann auf dem gemounteten Host-`.git` Dateien
**anlegen, aber nicht zuverlässig löschen/umbenennen**; dazu Cache-Inkohärenz (Phantom-Locks). `git
commit` braucht `index.lock` (anlegen → atomar umbenennen) → scheitert aus der VM. Gehört zur
Mount-Bug-Klasse #66006/#54483. Per Design der Cowork-Windows-Einbindung, kein Nutzer-Fehler.
**Versionen:** Cowork Windows 2026 (live bestätigt 2026-06-15). macOS evtl. nicht betroffen (anderer Mount).
**FIX (dauerhaft, funktionserhaltend → §10a.6):** Das Lock-Problem wird gelöst, indem das **git-dir auf
die VM-eigene Platte** gelegt wird (dort funktioniert Löschen), während der Arbeitsbaum im Mount bleibt
— gekapselt im Wrapper `cowork-git.sh` (siehe §10a.6). Fallback ohne Wrapper: aus dem **Windows-Host-Terminal**
committen/pushen (die Auth aus 10a.2 gilt dort dank geteilter `.git/config` auch), Lock-Reste per
`rm .git/*.lock` vom Host wegräumen. **NIE `.git/claude-multi-session.lock` löschen** (Hook-Lock, kein
Git-Lock). Phantom-`index.lock` auf Windows nicht anfassen (existiert dort nicht).
**Quelle:** live bestätigt 2026-06-15 (Schreibtest + Sonde); GitHub anthropics/claude-code #66006
(Sandbox kann auf CBFS-/Virtual-Drive-Mounts nicht enumerieren/löschen), #54483 (Mount-Pfade/-Verhalten).

### 10a.6 ⭐ FIX — zuverlässig aus der VM pushen: git-dir auf die VM-Platte (Wrapper `cowork-git.sh`)
**Lösung des Lock-Problems aus 10a.5.** Kernidee: Gits Lock-/Schreib-Operationen (Index, refs,
packed-refs, logs, objects) entstehen ALLE im **git-dir**. Legt man das git-dir auf das VM-eigene ext4
(wo Löschen funktioniert) und lässt nur den **Arbeitsbaum** im gemounteten Ordner, entstehen auf dem
Mount keine `.lock`-Dateien mehr → commit/push aus der VM laufen sauber.
**Mechanismus (verifiziert 2026-06-15 per `git clone --separate-git-dir`-Test):** Arbeitsbaum enthält
keinen `.git`-Schreibpfad; ref-Update + Index landen im separaten git-dir. Für Cowork via Umgebungs-
trennung statt gitlink, damit das vorhandene Mount-`.git` (Windows-Terminal) UNBERÜHRT bleibt:
```bash
GITDIR=$HOME/.cowork-gitdir/proggs          # VM-ext4 (Locks löschbar)
git --git-dir="$GITDIR" --work-tree="$REPO" init -q -b main
git --git-dir="$GITDIR" --work-tree="$REPO" remote add origin https://github.com/Pepsi1978/proggs.git
cp "$REPO/.git/credentials" "$GITDIR/cowork-credentials"   # Token vom Mount nur LESEN
git --git-dir="$GITDIR" --work-tree="$REPO" config credential.helper "store --file=$GITDIR/cowork-credentials"
git --git-dir="$GITDIR" --work-tree="$REPO" fetch -q origin main
git --git-dir="$GITDIR" --work-tree="$REPO" update-ref refs/heads/main FETCH_HEAD
git --git-dir="$GITDIR" --work-tree="$REPO" reset --mixed -q main   # Index=origin, Arbeitsdateien unberührt
# danach: add -A / commit / push origin HEAD:main
```
**Im Repo gekapselt:** `~/proggs/cowork-git.sh` (`bash cowork-git.sh push "#NNN - text"` bzw. `setup`).
Das git-dir ist ephemer (überlebt VM-Neustart nicht) — das Skript baut es bei jedem Lauf frisch aus
GitHub auf (Quelle der Wahrheit = origin/main). Mount-`.git` und Host-Terminal bleiben getrennt/intakt.
**Wichtige Punkte:** git ≥2.28 für `init -b` (Cowork-VM = Ubuntu 22.04/git 2.34 ✓); `reset --mixed`
lässt den Arbeitsbaum unangetastet; Token-Datei wird vom Mount nur gelesen, Arbeitskopie + alle Schreib-
vorgänge liegen im VM-git-dir.
**Versionen:** Cowork Windows 2026; git ≥2.28. **Quelle:** git-scm.com (GIT_DIR/GIT_WORK_TREE,
git-reset, separate-git-dir, git-credential-store, offiziell); lokal verifizierter Mechanismus 2026-06-15.

### 10a.7 ⭐ KRITISCH — `git add -A` aus Cowork: vier weitere Mount-Artefakte (live 2026-06-15)
Beim ERSTEN echten Push aus Cowork über das VM-git-dir (§10a.6) traten vier weitere, vom Windows-Mount
verursachte Probleme auf. Alle sind dauerhaft in `~/proggs/cowork-git.sh` abgefangen:
1. **Unlesbare Symlinks** — getrackte Windows-Symlinks (z. B. die finale-Plugin-Skill-Symlinks) liefern
   über den Mount `readlink: Input/output error`; `git add -A` bricht mit `unable to index file … fatal:
   updating files failed` ab. **FIX:** getrackte Symlinks (mode `120000`), die sich nicht lesen lassen,
   per `git update-index --skip-worktree` ausnehmen (sie liegen unverändert in origin). Muss nach jedem
   `reset --mixed` erneut laufen (das setzt skip-worktree zurück) → im Skript `guard_unreadable_symlinks`.
2. **Datei-Modus immer 0755** — der Mount meldet ALLE Dateien als ausführbar → git wertet bei JEDER
   getrackten Datei einen 644→755-Moduswechsel als Änderung (Commit mit Tausenden Schein-Änderungen).
   **FIX:** `git config core.fileMode false`.
3. **22.881 untrackte Build-Artefakte** — projektweite Build-Ordner waren nicht ignoriert
   (`EntropieReductor/app/build` ~15.294, `BestJournalFrank/app/build` ~4.423, `NEMS/app/build`,
   `node_modules/`, `.gradle/`) → `add -A` will alle stagen (zu langsam fürs VM-Sandbox-Fenster + bläht
   den Commit). **FIX:** `**/build/`, `**/.gradle/`, `**/node_modules/` in `.gitignore` (→ 22.881 auf ~91).
4. **Git-LFS-Dateien als Vollinhalt** — LFS-getrackte Dateien (`.gitattributes`: `whisper/*.onnx`, `*.aar`)
   erscheinen im Mount als voller Inhalt (bis **262 MB**!) statt als ~130-Byte-Zeiger; `add -A` würde die
   Zeiger durch echte Riesendateien ersetzen → GitHub lehnt ab (100-MB-Limit) + Repo bläht dauerhaft auf.
   **FIX:** getrackte LFS-Dateien (Muster aus `.gitattributes` mit `filter=lfs`) per `skip-worktree`
   ausnehmen → im Skript `guard_lfs_pointers`. LFS-Pflege erfolgt vom Windows-Rechner, nicht aus der VM.

**Zusätzliches Mount-/Sandbox-Verhalten:** (a) Hintergrundprozesse überleben die Grenze zwischen zwei
VM-Tool-Aufrufen NICHT (jeder Aufruf = frische Sandbox, alter Prozess wird gekillt) → ein langer Push
muss in EINEM Aufruf (≤ Sandbox-Timeout) durchlaufen; das Ignorieren der Build-Bäume (Punkt 3) macht
`add -A` schnell genug. (b) Der Mount kann beim Schreiben einer Datei eine **abgeschnittene/veraltete**
Version zeigen (Sync-Inkohärenz Windows↔VM) — größere Dateien VM-seitig vollständig schreiben und prüfen.
**Status:** GELÖST — erster echter Push lief durch (`… -> main`, Exit 0). Alle Guards in `cowork-git.sh`.
**Quelle:** live in Cowork bestätigt 2026-06-15.

---

## 11. Fix-Status (was ist belegt behoben?)

> **Ehrlichkeits-Hinweis zur Methodik:** Eine harte Verifikation per GitHub-CLI (`gh issue view … --json
> state`) war in dieser Cowork-Umgebung **nicht möglich** (kein `gh`/kein Token). Die Status-Angaben
> stammen aus der Researcher-Auswertung der Issue-Threads/Labels und der offiziellen Doku. Wo kein Beleg
> für einen Fix vorliegt, gilt der Bug konservativ als **offen**.

**Belegt „per Design" (dauerhaft, kein Fix zu erwarten — Workaround bleibt aktiv):**
§2.4 (workspace unavailable), §3.2/§7.9 (Task-Permissions teils), §5.1 (IP-Allowlist), §5.10 (Gmail nur
Entwürfe), §7.1 (wach+offen), §7.2 (1 Catch-up), §7.8 (kein Self-Scheduling), §7.10 (frische Session),
§8.3 (Artefakt-Connectors ohne Rückfrage), §8.4 (nicht teilbar), §8.5 (Cache/kein localStorage),
§9.3–9.6 (Sicherheits-Designgrenzen), §10.1–10.6 (Usage/Plattform-Grenzen), §6.3/§6.6/§6.9 (Skill-/Plugin-Anforderungen).

**Status unklar / überwiegend offen (Stand 2026-06-13, kein Fix-Beleg):**
Praktisch alle Windows-VM-Startfehler (§1.1–§1.11), die macOS-VM-/TCC-Bugs (§2.1–§2.3, §3.1, §3.3, §3.4),
die Datei-/Mount-Bugs (§4.1 KRITISCH, §4.2, §4.3, §4.4, §4.5), die OAuth-/MCP-Bugs (§5.2–§5.9), die
Skill-Mount-/Upload-Bugs (§6.1 KRITISCH, §6.2, §6.4, §6.5, §6.7, §6.8, §6.10, §6.11), die Scheduled-Task-Bugs
(§7.3 KRITISCH, §7.4–§7.7) und die Artefakt-Bugs (§8.1, §8.2, §8.6). Einige Issues sind als „stale",
„not planned" oder „invalid" gelabelt — das ist **kein** Beleg für einen Fix, nur für fehlende Bearbeitung.

**Teil-Hinweise auf Besserung (ohne harten Beleg):** §5.5 (lokale MCPs) — einzelne Builds meldeten Teilfixes
(Docker-Gateway in 1.1.1890, Python-Server in 1.1.2156), von anderen Nutzern widersprochen → weiter unsicher.

---

## 12. Pflicht-Checkliste vor der Cowork-Arbeit

- [ ] **Datei-Sicherheit zuerst:** Arbeite ich in einem iCloud-/OneDrive-/Cloud-Ordner? → Dateien „Download Now" + Backup (§4.1). Zielordner explizit verbinden, vollen Pfad angeben (§4.3).
- [ ] **Vertrauen der Quellen:** Liegen fremde Dokumente im Ordner? → „Ask before acting", keine sensiblen Ordner verbinden (§9.1).
- [ ] **Windows-Start klemmt?** → CoworkVMService prüfen (§1.1), MSIX statt Squirrel (§1.9), C:-Speicherort (§1.3), Virtualisierungs-Features (§1.4).
- [ ] **macOS-Start klemmt?** → Datei-/Web-Tools laufen weiter (§2.4); Projekt nicht in `~/Documents` (§3.3).
- [ ] **Eigene Skills/Plugins?** → Mount-Bug erwarten → ZIP-Upload (§6.1); `description` ohne `<…>`/URL (§6.4); Ordnername = `name` (§6.3).
- [ ] **Connectors?** → „connected ≠ funktioniert": Neustart + Re-Auth (§5.7); eigener MCP braucht IP-Allowlist (§5.1); lokale MCPs/DXT nicht in Cowork (§5.5); ab 10 Connectors „On demand" (§5.8).
- [ ] **Geplante Aufgaben?** → Prompt selbst-enthaltend + Zeit-Guardrails (§7.2/§7.10); „Keep computer awake" (§7.1); High-Freq-Cron meiden (§7.3); „Run now" + always-allow (§7.9).
- [ ] **Live-Artefakte?** → `structuredContent ?? content` beidseitig lesen (§8.2); nur Read-only-Connectors (§8.3); Refresh-Button (§8.5).
- [ ] **Computer Use/Chrome?** → keine Sandbox (§9.4); Links nie per Computer-Use klicken; sensible Apps/Sites blocken (§9.4/§9.5).
- [ ] **Usage im Blick:** Einfaches im Chat, Verwandtes in einer Session bündeln (§10.5).
- [ ] Bei JEDEM Fehler im Bereich: **Volltext** des betroffenen §-Abschnitts lesen (Stufe B).

---

## 13. Bezugstabelle ↔ Best Practices

`best-practices/claude-tooling/cowork.md` ist die Gegenseite (wie man es von
vornherein richtig macht). Wechselseitige Zuordnung:

| Bug-Abschnitt (dieser Almanach) | Best-Practice-Abschnitt (best-practices-cowork.md) |
|---------------------------------|----------------------------------------------------|
| §1/§2 VM-Start (Win/macOS) | §1 Überblick & Einrichtung (Architektur, Voraussetzungen) + Stolpersteine |
| §3 macOS-Permissions/TCC | §1 Berechtigungsmodi |
| §4 Datei-Arbeit & Datenverlust | §4 Datei-Arbeit & Ergebnis-Dokumente (Mount-Modi, dedizierter Ordner) |
| §5 Connectors & MCP | §3 Connectors & MCP (IP-Allowlist, Remote vs. Desktop-Extensions) |
| §6 Skills & Plugins | §2 Skills & Plugins (ZIP-Upload, 200-Zeichen-Limit, name-Feld) |
| §7 Scheduled Tasks | §5 Geplante Aufgaben (selbst-enthaltend, Catch-up-Guardrails) |
| §8 Live-Artefakte | §5 Live-Artefakte (Connectors ohne Rückfrage, CDN-Libs) |
| §9 Sicherheit/Prompt-Injection | §6 Computer-Steuerung & Browser + §7 Grenzen/Datenschutz |
| §10 Per-Design-Grenzen & Usage | §1 Voraussetzungen + §7 Grenzen (Compliance, höherer Verbrauch) |


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [claude-config](claude-config.md)
