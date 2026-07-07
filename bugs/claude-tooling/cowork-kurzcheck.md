# Cowork (Claude Desktop App) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
