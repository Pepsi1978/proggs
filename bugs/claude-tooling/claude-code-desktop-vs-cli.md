# 🐛 Claude Code — Desktop-App (Code-Tab) vs. CLI — Bug-Almanach

> **PFLICHT-LESE-HINWEIS:** Vor dem Programmieren mit Claude Code in der Desktop-App
> zuerst den **⚡ Kurzcheck (Stufe A)** lesen. Bei einem konkreten Fehler den passenden
> §-Abschnitt im Volltext aufschlagen.
>
> **Stand:** recherchiert am **2026-06-13** für die Claude **Desktop-App** (Code-Tab),
> Desktop-Redesign 14.04.2026; Claude-Code-Engine **2.1.x** (lokal 2.1.170 ermittelt),
> Desktop-App-Versionsschema **1.x** (Pane-Features ab v1.2581.0). Quelle der
> Grundwahrheit: offizielle Doku `code.claude.com/docs/en/desktop` + `support.claude.com`;
> Issues aus `github.com/anthropics/claude-code`. Gegenseite: `best-practices/claude-tooling/claude-code-desktop-vs-cli.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Code-Tab startet auf Windows nicht ("Git is required") | Git for Windows installieren → App neu starten | §A1 |
| 2 | Push scheitert: "Git LFS is required" / pre-push-Hook | git-lfs installieren + `git lfs install`; nur Markdown-Commit notfalls `--no-verify` | §A2 |
| 3 | Tool "nicht gefunden" obwohl installiert | Neues Terminal / App komplett neu starten (PATH-Snapshot veraltet) | §B1 |
| 4 | Build/Server scheitert an fehlender Env-Variable | Variable in den **lokalen Environment-Editor** (Local → Zahnrad), NICHT ins Shell-Profil | §B2 |
| 5 | macOS: npm/node/brew nicht gefunden beim Dock-Start | PATH (inkl. `/opt/homebrew/bin`) im Environment-Editor setzen oder Tools mit absolutem Pfad | §B3 |
| 6 | Hook (PostToolUse/SessionStart) feuert in der App nicht | Für hook-kritische Arbeit in die **CLI**; Hook-Befehl mit absoluten Pfaden | §C1 |
| 7 | Jeder Tool-Call fragt trotz bypassPermissions | Desktop liest `defaultMode` nicht zuverlässig → CLI mit `--dangerously-skip-permissions` | §D1 |
| 8 | MCP-Server verbindet nicht / Hammer-Icon fehlt | Absolute Pfade in der Config, App neu starten; im Zweifel MCP über die CLU | §E1 |
| 9 | "Wo sind meine Änderungen?" / `.claude/worktrees/` wuchert | Jede Session hat eigenen Worktree; `.claude/worktrees/` in `.gitignore`, committen+pushen | §F1 |
| 10 | Computer Use tut nichts trotz Toggle (macOS) | Accessibility **und** Screen Recording erteilen, dann App neu starten; nach Update neu erteilen | §G1 |
| 11 | Preview blockt `http://localhost:PORT` | Build-Regression — Link in Safari öffnen / auf Patch warten; Server-Config NICHT ändern | §H1 |
| 12 | Cloud-Session ignoriert CLAUDE.md/Skills/MCP, kein Terminal | Kontext-/Skill-/Terminal-Arbeit nur in **lokaler** (oder SSH-)Session | §I1 |
| 13 | `/agents` `/doctor` `/config` `/permissions` → "isn't available" | Settings-Datei direkt editieren oder Befehl in der eigenständigen CLI | §J3 |
| 14 | Skript/CI/Pipe gewünscht (`-p`, `--output-format`) | Geht im Desktop NICHT — bewusst in die CLI/Agent SDK wechseln | §J1 |
| 15 | macOS: "Claude is damaged" / schwarzer Hauptbereich | Notarisierungs-/Asar-Problem; App updaten, sonst CLI; `xattr` hilft oft nicht | §A8 |
| 16 | CLI-Verknüpfung nach Windows-Neustart: Fenster blitzt auf + schließt sofort (5-6 Klicks nötig) | NIE per Vorab-Heuristik lösen — Launcher muss NACH dem Start verifizieren + automatisch wiederholen (`Start-WtCliRobust` in `~/start-wt-common.ps1`) | §O1 |

---

## §A — Installation & Start (Plattform-Grundlagen)

### §A1. Git for Windows fehlt — Code-Tab startet nicht  [⭐ HÄUFIG]
**Symptom:** Auf Windows lässt sich im Code-Tab keine lokale Session starten ("Git is required"); der Tab bleibt funktionslos.
**Ursache:** Der Code-Tab braucht Git zwingend für die Session-Isolation (Git-Worktrees). Windows hat kein vorinstalliertes Git.
**Versionen:** Windows (x64+ARM64), alle App-Versionen — per Design.
**FIX:** Git for Windows (git-scm.com/downloads/win) installieren, App **vollständig neu starten**. `git --version` prüfen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §A2. Git LFS fehlt — Push/Checkout scheitert (pre-push-Hook)  [⭐ HÄUFIG — selbst erlebt]
**Symptom:** "Git LFS is required by this repository but is not installed"; bzw. ein `pre-push`-Hook bricht `git push` ab, weil `git-lfs` nicht im PATH ist.
**Ursache:** Das Repo trackt große Binärdateien über LFS; ohne installiertes Git LFS schlägt Checkout/Push fehl.
**Versionen:** Windows + macOS, alle Repos mit LFS.
**FIX:** Git LFS (git-lfs.com) installieren + `git lfs install`, App neu starten. **Selbst erlebt in diesem Repo:** Beim Push aus einer Umgebung ohne git-lfs blockiert der `pre-push`-Hook; wenn der konkrete Commit nachweislich KEINE LFS-Objekte enthält (z. B. reiner Markdown-Commit), ist `git push --no-verify` der funktionserhaltende Ausweg — niemals LFS-getrackte Binärdateien ungeprüft pushen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell) + eigener Vorfall 2026-06-13.

### §A3. PATH nach Installation nicht aktualisiert (Windows)
**Symptom:** Direkt nach einer Installation findet ein offenes Terminal das neue Tool nicht.
**Ursache:** PATH-Änderungen wirken nur in **neu** gestarteten Prozessen.
**Versionen:** Windows, alle Versionen.
**FIX:** Neues Terminalfenster öffnen bzw. App komplett neu starten.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §A4. "another installation in progress" / Cowork fehlt ohne Adminrechte (Windows)
**Symptom:** Installer meldet eine laufende Installation, obwohl keine läuft; oder Cowork fehlt nach Installation.
**Ursache:** Hängender Installer-Lock; volle Feature-Installation (inkl. Cowork) braucht Adminrechte. Cowork benötigt zusätzlich die Windows **Virtual Machine Platform**.
**Versionen:** Windows 10/11.
**FIX:** Installer **als Administrator** ausführen (UAC bestätigen); `Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart`.
**Quelle:** support.claude.com/.../deploy-claude-desktop-for-windows (offiziell).

### §A5. Enterprise-Blocker: AppLocker (MSIX) / WDAC (`spawn UNKNOWN`, errno -4094)
**Symptom:** Zentral verteiltes MSIX startet nicht; oder Cowork scheitert sofort mit `errno -4094, code 'UNKNOWN', syscall 'spawn'`.
**Ursache:** AppLocker blockiert paketierte MSIX-Apps standardmäßig; WDAC (Kernel-Mode) blockiert das Prozess-Spawnen, das Cowork braucht.
**Versionen:** Windows Enterprise (Intune/SCCM/GPO).
**FIX:** Claude-Desktop-Executables (Pfad unter `C:\Program Files\WindowsApps\Claude_<ver>_x64__...`) in AppLocker/WDAC explizit zulassen (signatur-/pfadbasiert, zentral via Intune). Reiner Code-Tab kann je nach Policy weiter laufen.
**Quelle:** support.claude.com Enterprise-Doku + github #56341 (Status unklar — vermutlich offen).

### §A6. Antivirus / SmartScreen blockiert den Installer
**Symptom:** SmartScreen-Warnung ("Windows protected your PC") oder AV blockiert den .exe-Installer.
**Ursache:** Reputationsheuristik bei neuen Builds. (Kein eigener offizieller Fall; verwandter dokumentierter Fall ist AppLocker bei MSIX.)
**Versionen:** Windows 10/11.
**FIX:** Bei verifiziertem Download von claude.com/download "Weitere Informationen → Trotzdem ausführen"; AV-Pfad whitelisten. Für MSIX: AppLocker anpassen.
**Quelle:** support.claude.com (offiziell, AppLocker) — SmartScreen ist generisches Windows-Verhalten.

### §A7. macOS: Git-Shim fordert Command Line Tools nach
**Symptom:** Auf frischem Mac öffnet jeder Git-Aufruf den Dialog "command line developer tools"; bis dahin scheitert die Worktree-Isolation.
**Ursache:** `/usr/bin/git` ist nur ein Shim für die Xcode Command Line Tools.
**Versionen:** macOS ohne installierte CLT.
**FIX:** `xcode-select --install`, danach App neu starten.
**Quelle:** code.claude.com/docs/en/desktop + developer.apple.com (offiziell).

### §A8. macOS: "Claude is damaged" / schwarzer Hauptbereich (Notarisierung/Gatekeeper)
**Symptom:** Sidebar rendert, Hauptbereich bleibt schwarz; nach Clean-Reinstall "‚Claude' is damaged and can't be opened".
**Ursache:** Beschädigtes `.asar`-Preload-Skript (IPC-Bridge bricht) und/oder von macOS-26-Gatekeeper abgelehnte Code-Signatur/Notarisierung.
**Versionen:** macOS 26.x Tahoe, Desktop-Build 1.5354.0 (2026-04-29); offen (von Anthropic als "invalid"/App-Thema gelabelt).
**FIX:** `xattr`-Quarantäne-Entfernung hilft bei diesem Build oft NICHT. Ausweg: claude.ai im Browser oder die CLI nutzen; saubere Lösung braucht einen für macOS 26 korrekt notarisierten Build (App updaten).
**Quelle:** github #56234 (Status: offen).

### §A9. macOS: Quarantäne auf `disclaimer`-Helper blockiert lokale Sessions
**Symptom:** Spinner hängt beim ersten Prompt, dann "Failed to load session"; die CLI funktioniert. Log: `disclaimer exited with code 128: Unable to read current working directory: Operation not permitted`.
**Ursache:** `com.apple.quarantine` auf `Claude.app/Contents/Helpers/disclaimer`, wegen SIP nicht entfernbar.
**Versionen:** macOS 26.2, Desktop v1.1.4498 / VM-SDK 2.1.51; CLI v2.1.68 OK.
**FIX:** Lokal die CLI nutzen; App aus frisch geladenem DMG neu installieren (`Help → Clear cache & reset app data`, `tccutil reset All com.anthropic.claudefordesktop`). Sonst neu signierter Build nötig.
**Quelle:** github #30798 (Status unklar — vermutlich offen).

### §A10. Blank / hängender Bildschirm beim Start
**Symptom:** App öffnet, zeigt leeren/nicht reagierenden Bildschirm.
**Ursache:** Hängender Render-Zustand oder abgebrochenes Auto-Update beim Launch.
**Versionen:** Windows + macOS.
**FIX:** App neu starten; auf Updates prüfen; Windows: Crash-Logs in Ereignisanzeige → Windows-Protokolle → Anwendung.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §A11. App lässt sich nicht beenden (beide Plattformen)
**Symptom:** Fenster schließt, Prozess läuft weiter; Cmd+Q hängt.
**Ursache:** Electron-Lifecycle-Bug: `app.quit()` wird nach Schließen aller Fenster nicht aufgerufen; nur das Dock-Kontextmenü-Quit nutzt einen funktionierenden Pfad.
**Versionen:** macOS (#16088, #14951 u. a.); Windows.
**FIX:** macOS: Cmd+Q, sonst Force Quit (Cmd+Option+Esc) oder Dock-Kontextmenü "Beenden". Windows: Task-Manager (Ctrl+Shift+Esc) → Claude-Prozess beenden.
**Quelle:** code.claude.com/docs/en/desktop (offiziell) + github #16088 (offen).

### §A12. 403 / Authentifizierungsfehler im Code-Tab
**Symptom:** "Error 403: Forbidden"; CLI geht, Desktop nicht.
**Ursache:** Abgelaufene Anmeldedaten, kein aktives Abo, oder halb beendeter App-Prozess.
**Versionen:** Windows + macOS.
**FIX (Reihenfolge):** 1) Ab-/anmelden. 2) Pro/Max/Team/Enterprise verifizieren. 3) App **komplett** beenden, neu öffnen, anmelden. 4) Proxy/Verbindung prüfen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

---

## §B — Umgebung / PATH / Env-Variablen (die stillen Killer)

### §B1. Desktop erbt NICHT die volle Shell-Umgebung  [⭐ HÄUFIG]
**Symptom:** In `~/.zshrc`/`.bashrc` exportierte Variablen (außer PATH) fehlen in lokalen Sessions/Dev-Servern; Builds scheitern still.
**Ursache:** Beim Start aus Dock/Finder liest macOS-App nur `PATH` + feste Claude-Variablen aus dem Profil; Windows erbt User-/System-Env, liest aber **keine PowerShell-Profile**.
**Versionen:** macOS + Windows, lokale Sessions — per Design.
**FIX:** Variablen im **lokalen Environment-Editor** setzen (Environment-Dropdown → über **Local** schweben → Zahnrad; verschlüsselt, gilt für Sessions + Dev-Server). Alternativ `env`-Block in `~/.claude/settings.json` (erreicht nur Sessions, nicht Dev-Server).
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §B2. Session findet installierte Tools nicht (npm/node)
**Symptom:** Claude meldet `npm`/`node` nicht gefunden, obwohl installiert.
**Ursache:** Veralteter/minimaler Env-Stand der App.
**Versionen:** Windows + macOS.
**FIX:** Tool im normalen Terminal verifizieren; Shell-Profil-PATH prüfen; App **vollständig neu starten** (Env neu laden).
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §B3. macOS: Dock/Finder-Start erbt nur minimalen System-PATH
**Symptom:** `which python3` → `/usr/bin/python3`; Homebrew-Tools fehlen; aus dem Terminal gestartet geht alles.
**Ursache:** Kein Login-Shell-Kontext beim GUI-Start → `brew shellenv` läuft nie.
**Versionen:** macOS Apple Silicon, v2.1.92 (als Duplikat geschlossen — bekanntes Verhalten).
**FIX:** PATH inkl. `/opt/homebrew/bin:/usr/local/bin` im Environment-Editor setzen oder Tools per absolutem Pfad aufrufen.
**Quelle:** github #44649 (geschlossen/Duplikat) + offizielle Troubleshooting-Doku.

### §B4. macOS: `env.PATH` aus settings.json / launchctl wird hart überschrieben
**Symptom:** Read-Tool findet `pdftoppm`/poppler nicht; PATH bleibt bei 4 Einträgen trotz settings.json-`env.PATH`, `launchctl setenv`, Symlinks. Dieselbe settings.json funktioniert in der VS-Code-Extension.
**Ursache:** Eine Electron-"Utility-Process"-Schicht ersetzt PATH gezielt durch einen minimalen Wert (nur PATH, andere Vars bleiben sichtbar).
**Versionen:** macOS 15.7.2, Desktop v1.2.234; **offen** (Label `area:desktop`).
**FIX:** Tools per absolutem Pfad über Bash aufrufen (z. B. `/opt/homebrew/bin/pdftoppm`); für PATH-Auflösung den lokalen Environment-Editor nutzen.
**Quelle:** github #42248 (offen).

### §B5. Windows: PATH wird mitten in der Session nicht Windows→POSIX übersetzt
**Symptom:** Plötzlich `command not found` für `gh`, `node`, `rm`, `ls`, `head` — nur `git` geht weiter.
**Ursache:** Nach Harness-State-Wechsel wird der MSYS-bash-Subprozess mit un-übersetztem Windows-PATH (`;`, `C:\...`) neu gestartet; bash parst das nicht.
**Versionen:** Windows 11, Claude Code 2.1.161; offen.
**FIX:** Session neu starten (frischer bash-Spawn übersetzt korrekt); in der laufenden Session Binaries mit vollem Pfad aufrufen oder `export PATH="/c/Program Files/nodejs:/usr/bin:$PATH"`.
**Quelle:** github #65883 (offen).

### §B6. Windows: `\u`-Pfad-Korruption bei Usernamen `C:\Users\u...`
**Symptom:** Bei Usernamen wie `uabcdef` zeigen alle Datei-Tools still auf einen falschen Pfad.
**Ursache:** `\u` im Pfad wird als Unicode-Escape interpretiert (`u[0-9a-fA-F]{4,}`).
**Versionen:** Windows; **geschlossen (gefixt)**.
**FIX:** Pfad über PowerShell-Variable bauen (`$u="uabcdef"; $dest="C:\Users\$u\..."`); oder POSIX-Pfade `/c/Users/...`; doppelte Backslashes/Forward-Slashes.
**Quelle:** github #54583 (geschlossen).

### §B7. Windows: Lange Pfade — `EINVAL`/`stat` beim Session-Resume
**Symptom:** "Failed to resume session: EINVAL: invalid argument, stat '…\.claude\projects\…jsonl'".
**Ursache:** Sehr tiefe Session-Pfade unter `%APPDATA%\Claude\local-agent-mode-sessions\` überschreiten MAX_PATH (260).
**Versionen:** Windows.
**FIX:** `LongPathsEnabled=1` (`HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem`) + Neustart; kurzen Installationspfad wählen; notfalls `local-agent-mode-sessions` löschen.
**Quelle:** github #55107 (Status unklar).

### §B8. Windows: OneDrive `EXDEV: cross-device link not permitted`
**Symptom:** Cowork-Rename scheitert, wenn das Projekt unter OneDrive liegt.
**Ursache:** Cross-Device-Link über die OneDrive-Grenze.
**Versionen:** Windows; offen.
**FIX:** Projektordner außerhalb von OneDrive legen.
**Quelle:** github #45178 (offen).

---

## §C — Hooks & Settings (besonders relevant für hook-getriebene Workflows)

> **Offizielle Grundwahrheit:** Hooks und Skills aus den Settings gelten laut Doku in
> beiden Umgebungen (Desktop + CLI). Die folgenden Einträge sind die **Praxis-Ausnahmen/Bugs**,
> bei denen die Hook-Ausführung in der App tatsächlich nicht greift — wichtig für jeden,
> dessen Automatisierung auf Hooks baut.

### §C1. PostToolUse-Hooks feuern in der Desktop-App nicht  [⭐ HÄUFIG]
**Symptom:** `/hooks` zeigt den Hook als geladen, aber nach dem Edit feuert er nie — kein Fehler, keine Ausgabe. Derselbe Befehl manuell im Terminal geht.
**Ursache:** Die Desktop-App führt Claude Code im stream-json-Server/API-Modus aus, in dem die PostToolUse-Hook-Ausführung nicht ausgelöst wird (Hooks greifen primär im interaktiven CLI-Modus). Regression.
**Versionen:** macOS, Desktop App v1.2.234; **offen**.
**FIX:** Hook-kritische Arbeit in der **CLI** erledigen; alternativ Formatter/Type-Check explizit als Skill/Slash-Command nach dem Edit anstoßen oder auf einen `Stop`-Hook setzen (greift eher). Config nicht ändern — sie ist korrekt.
**Quelle:** github #42336 (offen).

### §C2. Hooks werden gar nicht geladen — `/hooks` zeigt "No hooks configured yet"
**Symptom:** `/hooks` meldet keine Hooks, obwohl die Datei gewatcht und gültig ist; `mcpServers`/`permissions` derselben Datei laden korrekt.
**Ursache:** Parser-Regression: die `hooks`-Sektion wird nicht geparst.
**Versionen:** macOS, eröffnet 2025-11-13; **geschlossen**.
**FIX:** Auf gefixte Version updaten; bis dahin Hooks auf Projektebene (`.claude/settings.json`) definieren und `--setting-sources=user,project,local` prüfen.
**Quelle:** github #11544 (geschlossen).

### §C3. Windows: Hook-Befehle werden gematcht, aber nie ausgeführt
**Symptom:** Hook wird gematcht, der Command läuft nie an (Windows Desktop App).
**Ursache:** Desktop-App-spezifischer Ausführungspfad (verwandt mit §C1), Windows-Command-Invocation greift nicht.
**Versionen:** Windows, Claude Code v2.1.51; Status unklar (vermutlich offen).
**FIX:** CLU nutzen; Hook-Befehl als vollständigen Shell-Aufruf mit absoluten Pfaden formulieren.
**Quelle:** github #29560 (Status unklar).

### §C4. Windows: SessionStart-Hooks feuern still nicht (v2.1.141-Regression)
**Symptom:** `SessionStart`-Hooks laufen auf Windows/PowerShell nie — auch bei Kaltstart; das Skript läuft bei direktem Aufruf fehlerfrei.
**Ursache:** Windows-Regression im Hook-Dispatch, eingeführt in v2.1.141 (v2.1.140 OK).
**Versionen:** Windows 10/11, PowerShell, v2.1.141; **geschlossen**.
**FIX:** Auf gefixte Version aktualisieren (v2.1.140 feuerte korrekt); Hook-Logik übergangsweise manuell auslösen.
**Quelle:** github #59072 (geschlossen).

### §C5. Cowork-Sessions ignorieren User-Hooks und Managed Settings
**Symptom:** In Cowork (Desktop) greifen weder `~/.claude/settings.json`-Hooks (UserPromptSubmit, Stop, SessionStart) noch Managed Settings.
**Ursache:** Die Cowork-Sandbox läuft auf einer anderen Plattform (Linux-Mount) als der Host → Settings-Pfad-Auflösung scheitert.
**Versionen:** Desktop / Cowork; Status unklar (vermutlich offen).
**FIX:** Hook-/Settings-abhängige Workflows in der CLI ausführen; Cowork nur für Aufgaben ohne User-Hook-Abhängigkeit.
**Quelle:** github #40495 (Status unklar).

### §C6. Managed Settings per Admin-Konsole erreichen den Desktop anders
**Symptom:** Remote gepushte Managed Settings greifen in CLI/IDE, aber nicht im Desktop.
**Ursache:** Remote-Push erreicht aktuell nur CLI/IDE-Sessions; Desktop braucht die Managed-Settings-Datei per MDM auf Disk oder die Admin-Console-Controls. Asymmetrie: `sshHostAllowlist` liest nur der Desktop; `managedMcpServers` nur in 3P-Desktop-Deployments.
**Versionen:** Team/Enterprise.
**FIX:** Managed-Settings-Datei via MDM ausrollen (macOS `com.anthropic.claudefordesktop`; Windows Registry `SOFTWARE\Policies\Claude`) oder Admin-Console-Controls nutzen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

---

## §D — Permissions

### §D1. macOS: `bypassPermissions` wird ignoriert — jeder Tool-Call fragt  [⭐ HÄUFIG]
**Symptom:** `defaultMode: "bypassPermissions"` ist korrekt gesetzt, trotzdem promptet jeder Bash/Edit/Write/WebSearch-Call — 30+ Prompts pro Routine.
**Ursache:** Die Desktop-App liest `permissions.defaultMode` nicht in den Resolver ein; `--dangerously-skip-permissions` lässt sich nicht an die App übergeben.
**Versionen:** macOS 15.3, Opus 4.6, Desktop App; **geschlossen ("not planned", stale)**.
**FIX:** Granulare `permissions.allow`-Regeln pflegen (greifen teils) und für echten Bypass in der **CLU** mit `--dangerously-skip-permissions` arbeiten. Sicherheits-Denylist (`Bash(rm -rf *)`, `Bash(sudo *)`) beibehalten.
**Quelle:** github #38662 (geschlossen/not planned); verwandt #29026, #37192.

### §D2. macOS: Bypass-Modus lässt sich nicht aktivieren
**Symptom:** Umschalten auf "Bypass Permissions" scheitert ("couldn't be changed"), fällt auf "Accept Edits" zurück.
**Ursache:** Desktop-UI persistiert den Bypass-State nicht.
**Versionen:** macOS Desktop v2.1.148; Status unklar (vermutlich offen).
**FIX:** Im Terminal mit `--dangerously-skip-permissions` starten; in der App mit Accept-Edits + Allow-Regeln arbeiten.
**Quelle:** github #61415 (Status unklar).

### §D3. Windows/WSL: Permission-Config wird ignoriert
**Symptom:** Bei WSL-Projekten promptet die Desktop-App jede Dateioperation.
**Ursache:** Pfad-/Plattform-Mismatch beim Auflösen der Settings über die WSL-Grenze.
**Versionen:** Windows + WSL; Status unklar.
**FIX:** Claude Code direkt **innerhalb der WSL-Distribution per CLI** starten (nicht über die Windows-Desktop-App).
**Quelle:** github #37192 (Status unklar).

### §D4. `dontAsk`-Mode fehlt im Desktop
**Symptom:** Der restriktive `dontAsk`-Mode (Deny-by-default) ist im Mode-Selector nicht wählbar.
**Ursache:** Offiziell nur in der CLI verfügbar.
**Versionen:** alle.
**FIX:** Für locked-down-Läufe `claude -p "..." --permission-mode dontAsk`; im Desktop nur "Ask permissions" + harte `ask`-Rules.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §D5. `--allowedTools`/`--disallowedTools` ohne Pro-Session-Äquivalent
**Symptom:** Keine Möglichkeit, Tools nur für **eine** Session einzuschränken.
**Ursache:** Desktop kennt nur globale Permission-Rules aus den Settings.
**Versionen:** alle.
**FIX:** `permissions.allow`/`ask`/`deny` in den Settings; pro-Aufruf-Differenzierung nur in der CLI.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

---

## §E — MCP-Server

### §E1. macOS Tahoe: MCP-Server trennen sofort nach `initialize`
**Symptom:** Jeder MCP-Server (auch offizieller `server-filesystem`) trennt ~33 ms nach `initialize`; Hammer-Icon erscheint nie.
**Ursache:** macOS-26.2-Tahoe-spezifischer Transport-/Handshake-Bruch in der Desktop-App; serverunabhängig.
**Versionen:** macOS 26.2 Tahoe Beta, Claude Desktop 1.1.7714; Status unklar (vermutlich offen).
**FIX:** Auf stabile macOS-Version statt Tahoe-Beta; MCP über die **CLI** nutzen (Handshake läuft nicht über die Desktop-Transport-Schicht). Config nicht ändern.
**Quelle:** github #36818 (Status unklar).

### §E2. GitHub-MCP trennt bei Multi-File-Operationen, kein Auto-Reconnect
**Symptom:** Mitten in Multi-File-Sessions bricht die GitHub-MCP-Verbindung; stdio-MCP-Server reconnecten nie.
**Ursache:** Parent/Child-Prozess-Kopplung + fehlender stdio-Auto-Reconnect in der Desktop-App.
**Versionen:** Desktop App seit v1.1.5749; Status unklar.
**FIX:** Cmd+Q + App-Neustart, oder MCP über die CLI betreiben (`/mcp` reconnect); große Operationen in kleinere Batches splitten.
**Quelle:** github #38395, Reconnect-Feature #54136 (Status unklar).

### §E3. Windows: MCP-Server verbinden nicht / Toggles reagieren nicht
**Symptom:** MCP-Toggles reagieren nicht oder Server verbinden nicht; Webview hängt beim Session-Wechsel mit gleichzeitigen MCP-Operationen.
**Ursache:** Fehlkonfiguration, hängender Prozess, oder app-interne Inkonsistenz.
**Versionen:** Windows (#43177 geschlossen; #51649 Webview-Hang App 1.3561.0).
**FIX:** Config prüfen (absolute Pfade, `claude_desktop_config.json` + `~/.claude.json` + `.mcp.json`); App neu starten; im Task-Manager Server-Prozess prüfen; nicht viele Sessions mit vielen MCP-Servern gleichzeitig warmlaufen lassen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell) + github #43177, #51649.

### §E4. MCP-Config-Trennung Desktop-Chat ↔ CLI (absolute Pfade nötig)  [⭐ HÄUFIG]
**Symptom:** Ein per `claude mcp add` (CLI) eingerichteter Server taucht im Desktop nicht auf (und umgekehrt); "mal geht's, mal nicht".
**Ursache:** CLI liest `~/.claude.json`, die Desktop-**Chat**-App liest `~/Library/Application Support/Claude/claude_desktop_config.json` — getrennte Registries. Zusätzlich erbt der Desktop den Shell-PATH nicht → nackte Befehlsnamen (`uvx`) laufen ins Leere. **Abgrenzung:** Der Code-Tab lädt zusätzlich `claude_desktop_config.json`; Hooks/Skills/Plugins teilen sich die settings.json — getrennt sind v. a. MCP-Registry + PATH.
**Versionen:** macOS/Windows.
**FIX:** Server in **beiden** Configs eintragen; in der Desktop-Config **absolute Pfade** statt nackter Befehlsnamen; `claude mcp add-from-claude-desktop` (macOS/WSL) zum Importieren; per `/mcp` prüfen, was die Umgebung sieht. (Deckt sich mit der CLAUDE.md-Regel "MCP-Configs mit absoluten Pfaden".)
**Quelle:** github #33732 (extern) + danyuchn.github.io-Blog (extern) + offizielle MCP-Import-Doku.

### §E5. Chrome-MCP bindet an die falsche App, wenn Desktop + Code installiert sind
**Symptom:** Die claude-in-chrome-Extension verbindet sich mit Claude Desktop statt Claude Code; Code-seitige Chrome-Tools bleiben getrennt.
**Ursache:** Native-Messaging-Host-Routing wählt den falschen Empfänger.
**Versionen:** macOS, beide Apps installiert; Status unklar.
**FIX:** Nur eine App gleichzeitig laufen lassen; ggf. `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1` setzen.
**Quelle:** github #20887, #38475 (Status unklar).

---

## §F — Worktrees & parallele Sessions

### §F1. Worktrees landen im Repo-Root; CLAUDE.md/Hook-Block wird ignoriert  [⭐ HÄUFIG]
**Symptom:** Jede neue Desktop-Session legt `<repo-root>/<random-slug>/` an; `git status` verschmutzt, Cleanups nötig.
**Ursache:** Die App hardcodiert `worktreePath = cwd + "/" + slug` ohne Override; `PreToolUse`-Bash-Hooks feuern nicht (nativer Pfad statt Bash-Tool); `WorktreeCreate`-Block greift zu spät.
**Versionen:** macOS, Claude Desktop; **geschlossen (Duplikat)**, verwandt #31896/#50109/#57484/#52051.
**FIX:** Slug-Dirs/`.claude/worktrees/` in `.gitignore`; Worktree-Speicherort + Branch-Präfix unter Settings → Claude Code → "Worktree location"; für strikte Konventionen Session aus dem Terminal in einem manuell angelegten Sibling-Worktree starten.
**Quelle:** github #49986 (geschlossen/Duplikat).

### §F2. Parallele Sessions kollidieren auf demselben Working-Tree
**Symptom:** Zwei Sessions auf demselben Repo: uncommittete Edits kollidieren, Branch-Switch der einen bricht die andere.
**Ursache:** Fehlende automatische Worktree-Isolation, wenn nicht aktiv genutzt.
**Versionen:** Desktop + CLI; Status unklar.
**FIX:** Jede parallele Session in eigenem `git worktree add` mit eigenem Branch; nie zwei Sessions auf demselben Working-Tree.
**Quelle:** github #52051 (Status unklar).

### §F3. Gitignorierte `.env` fehlt im Worktree → Build/Run scheitert  [⭐ HÄUFIG]
**Symptom:** In einer parallelen Session fehlen `.env`, Credentials, `master.key` — App startet nicht, Tests rot.
**Ursache:** Worktrees duplizieren nur **getrackte** Dateien; alles in `.gitignore` fehlt.
**Versionen:** macOS/Windows; offiziell bestätigt.
**FIX:** `.worktreeinclude` im Projekt-Root (umgekehrte gitignore-Syntax) — kopiert gematchte+gitignorete Dateien in jeden neuen Worktree. Keine Secrets ins Repo.
**Quelle:** code.claude.com/docs/en/desktop + /worktrees (offiziell).

### §F4. "Wo sind meine Änderungen?" — `.claude/worktrees/` wuchert
**Symptom:** Änderungen im Hauptordner nicht auffindbar; immer mehr Arbeitsverzeichnisse unter `.claude/worktrees/`.
**Ursache:** Jede Session bekommt standardmäßig einen eigenen Worktree (Isolation), nicht offensichtlich kommuniziert.
**Versionen:** macOS/Windows, primär Desktop.
**FIX:** `.claude/worktrees/` in `.gitignore`; Speicherort/Branch-Präfix in den Settings anpassen. Subagent-Worktrees werden nur entfernt, wenn älter als `cleanupPeriodDays` UND ohne uncommittete/ungepushte Änderungen → committen+pushen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell) + Feature-Requests #31896/#39563 (extern).

---

## §G — Computer Use (macOS)

### §G1. Computer Use tut nichts trotz Toggle — Accessibility + Screen Recording fehlen  [⭐ HÄUFIG]
**Symptom:** Computer Use ist eingeschaltet, aber Claude steuert/sieht den Bildschirm nicht; Screenshots schwarz.
**Ursache:** Auf macOS sind zwei TCC-Rechte Pflicht: **Accessibility** + **Screen Recording**; macOS prüft erst beim Launch neu.
**Versionen:** macOS, Research Preview (Pro/Max, nicht Team/Enterprise).
**FIX:** Settings → General → Computer use an; beide Systemrechte erteilen (Badge klickt zur System-Settings-Pane); App **vollständig beenden (Cmd+Q)** + neu starten.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §G2. Computer-Use-Rechte gehen bei jedem Update verloren
**Symptom:** Nach Auto-Update bricht Computer Use mit "permission denied"; in Accessibility stapeln sich "Geister"-Einträge alter Versionen.
**Ursache:** macOS koppelt TCC-Rechte an Pfad+Inode der Binärdatei; Claude installiert jede Version unter versionsspezifischem Pfad → gilt als neue, nie-freigegebene App.
**Versionen:** macOS Apple Silicon, ab v2.1.99/2.1.101; **geschlossen (Duplikat)**.
**FIX:** Nach jedem Update Geister-Eintrag entfernen, neue Binär per "+" neu hinzufügen, Toggle neu setzen, App neu starten.
**Quelle:** github #46859 (geschlossen).

### §G3. "permission(s) not yet granted" trotz vollständiger Grants
**Symptom:** `request_access`/`list_granted_applications` melden fehlende Rechte, obwohl alles erteilt ist; persistiert über Neustarts.
**Ursache:** Das Gate prüft einen internen `.verified`-Sentinel unter `…/claude-code/<VERSION>/`, nicht den echten TCC-Status; Auto-Update (2.1.111→2.1.114) hat das Verzeichnis nicht neu provisioniert.
**Versionen:** macOS 26.5 Tahoe, CLI v2.1.114; **offen**.
**FIX:** `/mcp disable computer-use` → `/mcp enable computer-use`; bei Bedarf verwaistes `…/claude-code/<alte-Version>/` entfernen oder CLI frisch installieren.
**Quelle:** github #50735 (offen).

### §G4. Computer Use läuft auf falschem Gerät und umgeht Permission-Gates (sicherheitsrelevant)
**Symptom:** Computer Use wird auf einer Maschine ausgeführt, auf der es nicht aktiviert ist.
**Ursache:** Geräte-/Session-Routing-Fehler — keine Validierung gegen den lokalen Gate-Zustand des ausführenden Geräts.
**Versionen:** Desktop, geräteübergreifend; Status unklar.
**FIX:** Computer Use systemweit deaktivieren bis gepatcht; nur auf dem freigeschalteten Gerät arbeiten, Cross-Device/Cloud meiden.
**Quelle:** github #38473 (Status unklar).

---

## §H — Preview-Pane

### §H1. Preview blockiert legitime `http://localhost`-URLs
**Symptom:** Preview verweigert `http://localhost:PORT/...` ("Link to localhost was blocked"); Screenshot-Pipeline scheitert (SIGTERM 143). In Safari lädt dieselbe URL.
**Ursache:** Regression in der Navigations-Allowlist von `PreviewContext` (Build 1.6608.0); striktes URL-Parsing erkennt `localhost` nicht.
**Versionen:** macOS 26.3.1, Desktop App 1.6608.0, CLI 2.1.77; **offen**.
**FIX:** Geklickte Links in Safari; für die Screenshot-Pipeline Downgrade auf Pre-1.6608.0 oder auf Patch warten. Server (Port/Host) nicht ändern.
**Quelle:** github #57253 (offen).

### §H2. Preview liest keine `~/Desktop`-Dateien trotz Full Disk Access
**Symptom:** Preview kann Dateien in `~/Desktop` nicht lesen.
**Ursache:** TCC-Schutz von `~/Desktop`; die Preview-Sandbox erbt den FDA-Grant der App nicht.
**Versionen:** macOS; Status unklar.
**FIX:** Projekt aus nicht-TCC-geschütztem Ordner servieren (z. B. `~/proggs/`, `~/Developer/`), nicht aus `~/Desktop`/`~/Documents`/`~/Downloads`.
**Quelle:** github #51312 (Status unklar).

### §H3. Preview-Overlay verdeckt Diff-Bar und Permission-Prompts
**Symptom:** Im Code-Tab überdeckt das Preview-Overlay Diff-Leiste und Berechtigungs-Prompts.
**Ursache:** Z-Index-/Layout-Fehler.
**Versionen:** macOS, Desktop App v1.7196.0; Status unklar.
**FIX:** Preview-Pane schließen/einklappen; Fenster vergrößern; Pane-Layout umschalten.
**Quelle:** github #58496 (Status unklar).

---

## §I — Sessions, Cloud, SSH, Persistenz

### §I1. Cloud-Session: kein CLAUDE.md, keine Skills/MCP, kein lokaler Zugriff, kein Terminal  [⭐ HÄUFIG]
**Symptom:** In Cloud-/Web-Sessions "vergisst" Claude Projektkonventionen, kennt eigene Skills nicht, kein integriertes Terminal.
**Ursache:** Cloud-Sessions starten frisch auf Anthropic-Infrastruktur ohne lokale Umgebung; Terminal nur lokal; nur GitHub (kein Azure DevOps).
**Versionen:** Claude Code on the web / Cloud-Sessions.
**FIX:** Kontext-/Skill-/MCP-/Datei-abhängige Aufgaben in **lokaler** (oder SSH-)Session fahren; Cloud nur für isolierte GitHub-Tasks.
**Quelle:** code.claude.com/docs/en/claude-code-on-the-web (offiziell).

### §I2. "Branch doesn't exist yet" beim Öffnen in der CLI
**Symptom:** Eine in einer Cloud-Session erstellte Branch lässt sich lokal nicht öffnen.
**Ursache:** Cloud-Branches liegen auf der Cloud-Infrastruktur, lokal noch nicht gefetcht.
**Versionen:** Übergang Cloud → lokale CLI.
**FIX:** Branch-Namen in der Toolbar kopieren, dann `git fetch origin <branch>` + `git checkout <branch>`.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §I3. "Failed to load session"
**Symptom:** Beim Öffnen einer Session "Failed to load session".
**Ursache:** Gewählter Ordner existiert nicht mehr, Repo braucht nicht installiertes Git LFS, oder Dateiberechtigungen.
**Versionen:** Windows + macOS.
**FIX:** Anderen (existierenden) Ordner wählen oder App neu starten; LFS nachinstallieren (§A2).
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §I4. Windows: Nach Auto-Update Nachrichteninhalt weg
**Symptom:** Sessions noch in der Sidebar, aber "No messages yet." — Reinstall + Cache-Löschen hilft nicht.
**Ursache:** Auto-Update-Regression in der Session-Persistenz (Inhalt nicht in JSONL geschrieben).
**Versionen:** Windows; offen.
**FIX:** Kein vollständiger Recovery bekannt. Mitigation: Auto-Updates per Policy steuern (`disableAutoUpdates`), wichtige Ergebnisse vor Updates extern sichern.
**Quelle:** github #53717 (offen).

### §I5. SSH-Session ignoriert `Port` aus `~/.ssh/config`
**Symptom:** Die Desktop-App zieht den in `~/.ssh/config` definierten Port nicht heran; unklarer Host-Verification-Fehler.
**Ursache:** Der SSH-Connector parst `~/.ssh/config` nicht vollständig.
**Versionen:** Desktop SSH-Sessions; Status unklar.
**FIX:** Port explizit im Host-Feld der App angeben; oder Session aus dem Terminal per `ssh` öffnen und dort Claude Code starten.
**Quelle:** github #26809 (Status unklar).

### §I6. Windows: CLI/Extension stirbt still nach "Git remote URL: null"
**Symptom:** CLI/Extension sterben ~60–90 ms nach `Git remote URL: null`; CLI kehrt kommentarlos zum Prompt zurück (Exit 0).
**Ursache:** Nicht abschließend geklärt; korreliert mit leerer Git-Remote-URL + Windows-Query-Abbruch.
**Versionen:** Windows 11, CLI 2.1.108–2.1.112; offen.
**FIX:** Kein bestätigter Fix. Prüfen, ob das Repo eine gültige Remote hat (`git remote -v`), ggf. setzen; saubere Neuinstallation.
**Quelle:** github #49522 (offen).

---

## §J — Fehlende Features (vs. CLI) — die "es fehlt"-Fallen

### §J1. Kein Headless/Scripting — `--print`, `--output-format`, Agent SDK  [⭐ HÄUFIG]
**Symptom:** `claude -p "..." | jq` / CI-Job / maschinenlesbare Ausgabe — im Desktop kein Knopf dafür.
**Ursache:** Desktop ist **rein interaktiv** ("Not available. Desktop is interactive only.").
**Versionen:** alle.
**FIX:** In die **CLI**: `claude -p "..." --output-format json`, `--json-schema`, `--bare`, oder Agent SDK. Desktop-Ersatz für Wiederkehr: nur **Scheduled Tasks** (kein echtes Scripting).
**Quelle:** code.claude.com/docs/en/desktop + /headless (offiziell).

### §J2. Agent-Teams (Sessions, die sich Nachrichten schicken) fehlen
**Symptom:** Kein Team-Lead + Teammates mit direkter Kommunikation/geteilter Task-Liste im Desktop.
**Ursache:** Offiziell CLI-only (experimentell, `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`, CLI ≥ v2.1.32).
**Versionen:** alle.
**FIX:** Desktop-Ersatz **Dynamic Workflows** (Multi-Agent **in einer** Session). Für echte Teammate-Kommunikation in die CLI. (Betrifft Franks `TeamCreate`-Workflow.)
**Quelle:** code.claude.com/docs/en/desktop + /agent-teams (offiziell).

### §J3. `/permissions`, `/config`, `/agents`, `/doctor` → "isn't available in this environment"  [⭐ HÄUFIG]
**Symptom:** Diese Slash-Befehle antworten mit der Fehlermeldung statt dem Dialog.
**Ursache:** Sie öffnen ein interaktives Terminal-Panel, das der Code-Tab nicht hat.
**Versionen:** alle.
**FIX:** Settings-Dateien direkt editieren oder die GUI nutzen (Mode-Selector, Connectors-UI, Plugin-Manager); echter Dialog nur in der eigenständigen CLI.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §J4. Inline-Code-Vorschläge (Autocomplete) fehlen
**Symptom:** Keine Copilot-artigen Inline-Suggestions.
**Ursache:** Desktop ist konversationell/diff-basiert.
**Versionen:** alle.
**FIX:** Für Inline-Completion die IDE-Extension; aus dem Desktop per Rechtsklick → "Open in" (VS Code/Cursor/Zed).
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §J5. Kein Linux
**Symptom:** Keine Desktop-App auf Linux.
**Ursache:** Desktop nur macOS + Windows.
**Versionen:** alle.
**FIX:** Auf Linux die CLI; alternativ SSH-Session vom Mac/Windows-Desktop auf einen Linux-Host.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §J6. Drittanbieter (Bedrock/Vertex/Foundry) standardmäßig nicht
**Symptom:** Code-Tab-Session über Bedrock/Vertex/Foundry/eigenen Gateway geht standardmäßig nicht.
**Ursache:** Desktop spricht standardmäßig Anthropics API; nur Enterprise kann Vertex/Gateway via Managed Settings; Bedrock/Foundry nur über Cowork-on-3P-Preview. (Nebeneffekt: `/desktop` nicht mit API-Key oder auf 3P.)
**Versionen:** alle.
**FIX:** Für Bedrock/Foundry in die CLI; im Desktop Enterprise-Managed-Settings (Vertex/Gateway) oder Cowork-on-3P-Preview.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §J7. @mention, integriertes Terminal, File-Pane nicht in Cloud-Sessions
**Symptom:** In Cloud-Sessions kein `@mention`, kein integriertes Terminal, kein manuelles Datei-Editieren (File-Pane); Side-Chat `/btw` nur local+SSH.
**Ursache:** Offizielle Einschränkung der Cloud-Umgebung.
**Versionen:** Cloud-Sessions.
**FIX:** Diese Funktionen in **lokaler oder SSH-Session**; in Cloud Claude die Änderung im Prompt auftragen.
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

### §J8. Interactive-Mode-Shortcuts (`Shift+Tab`) gelten nicht im Code-Tab
**Symptom:** `Shift+Tab` (Mode-Wechsel) u. ä. tun im Code-Tab nichts.
**Ursache:** Desktop hat ein eigenes Shortcut-Set.
**Versionen:** alle.
**FIX:** Desktop-Shortcuts nutzen (`Cmd/Ctrl+/` zeigt alle; `Cmd/Ctrl+Shift+M` Permission-Mode, `Ctrl+O` View-Modes).
**Quelle:** code.claude.com/docs/en/desktop (offiziell).

---

## §K — Performance, UI, Kosmetik

### §K1. Desktop frisst CPU/RAM; Code-Tab crasht bei großer History
**Symptom:** Mac wird träge (Renderer ~68 % CPU, Peaks bis 367 % auch idle); Code-Tab crasht beim Anklicken; bei großer Projekt-History startet der Prozess nicht.
**Ursache:** Electron-Overhead + Claude lädt die **gesamte** Projekt-Chat-History beim Start in den Speicher. (Vergleich: ~20 parallele CLI-Sessions ≈ 7 % CPU.)
**Versionen:** macOS, u. a. v1.2773.0 / M4.
**FIX:** Für ernsthafte Parallelarbeit (5–6 Sessions) die **CLI**; `.gitignore` früh pflegen; bei Crash `claude --safe-mode`; Projekt-History beschneiden.
**Quelle:** github #32012, #49055 (extern) + offizielle Troubleshooting-Doku.

### §K2. Plan-Nutzung: Desktop blockt, während CLI weiterläuft (Sync-Bug)
**Symptom:** Windows-Desktop zeigt "Usage limit reached … Upgrade" und sperrt das Eingabefeld, während die CLI auf demselben Account "Now using extra usage" zeigt und weiterarbeitet.
**Ursache:** Offiziell ist die Nutzung **geteilt**; beobachtet wird ein Client-/Backend-Sync-Bug (Desktop honoriert das extra-usage-Flag nicht), **kein** separates Kontingent.
**Versionen:** Windows-Desktop v1.3883.0 vs. CLI v2.1.62; **geschlossen ("invalid/not planned")**.
**FIX:** Bei Desktop-Blockade trotz Guthaben auf die CLI ausweichen (gleicher Account). App-Neustart half laut Report nicht.
**Quelle:** support.claude.com (offiziell: Nutzung geteilt) + github #52467 (extern).

### §K3. Auto-Compaction-Timing/Trigger divergiert zwischen Desktop und CLI
**Symptom:** Auto-Compact triggert teils früher als erwartet, teils gar nicht (bleibt bei 100 % stehen).
**Ursache:** Compaction-Logik mehrfach geändert; Desktop hinkt der CLI bei Updates 2–4 Wochen hinterher → versionsbedingte Divergenz.
**Versionen:** beide; Drift trifft v. a. Desktop.
**FIX:** Bei vollem Kontext früh manuell `/compact`; wichtige Fakten in CLAUDE.md/Memory persistieren; für aktuellstes Verhalten die CLI.
**Quelle:** code.claude.com/docs/en/desktop (offiziell) + github #66144 (extern).

### §K4. Inline-LaTeX rendert nicht im Code-Tab
**Symptom:** `$...$`-Mathe bleibt Rohtext (anders als im Chat-Tab).
**Ursache:** Der Code-Tab-Markdown-Renderer hat keinen LaTeX/KaTeX-Pass.
**Versionen:** macOS + Windows; Status unklar.
**FIX:** Formeln als Code-Block/Unicode; gerenderte Mathe im Chat-Tab.
**Quelle:** github #36742 (Status unklar).

### §K5. Multi-File-Drag&Drop/Paste hängt nur die letzte Datei an
**Symptom:** Beim Ziehen mehrerer Dateien wird nur die letzte angehängt.
**Ursache:** Drop-/Paste-Handler verarbeitet nur das letzte Item.
**Versionen:** macOS; Status unklar.
**FIX:** Dateien einzeln anhängen oder per `@`-Mention/`/add` referenzieren.
**Quelle:** github #50707 (Status unklar).

### §K6. Windows: AltGr-Sonderzeichen / pixeliges Icon / CRLF-Edits (kosmetisch/klein)
**Symptom:** AltGr-abhängige Zeichen lassen sich in der nativen Windows-CLI nicht tippen (#53451, geschlossen); unscharfes Taskbar-Icon (#59477, geschlossen); Edits scheitern an CRLF-Zeilenenden (#27718, geschlossen).
**FIX:** AltGr-Zeichen per Copy-Paste; CRLF: `old_string` mit korrekten Zeilenenden matchen.
**Quelle:** github #53451 / #59477 / #27718 (geschlossen).

---

## §L — Fix-Status (Schritt 3 — ehrlich getrennt)

| Bug | Status | Beleg |
|-----|--------|-------|
| §B6 `\u`-Pfad-Korruption (Windows) | **belegt GESCHLOSSEN/gefixt** | github #54583 |
| §C2 Hooks nicht geladen (`/hooks` leer) | **belegt GESCHLOSSEN** | github #11544 |
| §C4 SessionStart-Hooks Windows v2.1.141 | **belegt GESCHLOSSEN** (v2.1.140 OK, spätere Version gefixt) | github #59072 |
| §D1 bypassPermissions ignoriert (macOS) | **GESCHLOSSEN als "not planned"** (Bug bleibt praktisch bestehen → CLI nutzen) | github #38662 |
| §F1 Worktrees im Repo-Root | **GESCHLOSSEN als Duplikat** (Verhalten besteht; Override offen) | github #49986 |
| §K2 Plan-Nutzung-Sync (Windows) | **GESCHLOSSEN als "invalid/not planned"** | github #52467 |
| §K6 AltGr / Icon / CRLF | **belegt GESCHLOSSEN** | github #53451/#59477/#27718 |
| §C1 PostToolUse-Hooks feuern nicht | **OFFEN** | github #42336 |
| §B4 env.PATH überschrieben (macOS) | **OFFEN** | github #42248 |
| §G3 Computer-Use "not granted" trotz Grants | **OFFEN** | github #50735 |
| §H1 Preview blockt localhost | **OFFEN** | github #57253 |
| §I4 Nachrichteninhalt weg nach Update | **OFFEN** | github #53717 |
| §A8 "Claude is damaged" (macOS 26) | **OFFEN** (von Anthropic als App-/Notarisierungs-Thema gelabelt) | github #56234 |

**Noch NICHT gefixt / Workaround bleibt aktiv:** §C1, §B4, §G3, §G4, §H1, §I4, §A8, §B8, §I6 sowie alle offiziellen Design-Einschränkungen in §J (kein Headless, keine Agent-Teams, `/agents` etc. — das sind **bewusste** Grenzen, keine Bugs).

**Ehrlichkeits-Hinweis zur Methodik:** Die mit "belegt" markierten Status stammen aus direktem Issue-Abruf der Researcher. Einträge mit "Status unklar (vermutlich offen)" stammen nur aus der Suchergebnis-Ebene und sollten vor kritischer Nutzung am jeweiligen Issue gegengeprüft werden. `gh issue view` war im Cowork-Sandbox nicht authentifiziert verfügbar — daher wurde nichts ohne Beleg als "gefixt" deklariert. Im Zweifel gilt ein Bug als **offen**.

---

## §M — Bezug zur Best-Practices-Gegenseite

Gegenseite: `best-practices/claude-tooling/claude-code-desktop-vs-cli.md`.

| Bug-Abschnitt (hier) | Best-Practice-Abschnitt (dort) |
|----------------------|-------------------------------|
| §A (Installation/Start) | §1 (Tabs/Voraussetzungen), §6.8 (Env-Editor) |
| §B (PATH/Env) | §6.8 (lokaler Environment-Editor) |
| §C (Hooks/Settings) | §2 (gemeinsame Configs), §5 (Frank-Mapping: Hooks/Agent-Teams) |
| §D (Permissions) | §4 (`dontAsk`/Bypass nur CLI), §6.1 (Plan-Mode) |
| §E (MCP) | §2 (MCP-Import `add-from-claude-desktop`) |
| §F (Worktrees) | §3.2 (Worktrees), §6.2 (Worktrees nutzen) |
| §G (Computer Use) | §3.10 (Computer Use) |
| §H (Preview) | §3.7, §6.3 (Preview + Auto-Verify) |
| §I (Cloud/SSH/Session) | §3.12 (Environments), §6.6 (Remote/Cloud) |
| §J (Feature-Lücken) | §4 (komplette "NICHT im Desktop"-Tabelle), §5 (Frank-Mapping) |
| §K (Performance/UI) | §6.9 (für Skript/Batch bewusst CLI) |

---

## §N — Pflicht-Checkliste (vor dem Programmieren im Desktop)

1. **Windows:** Git for Windows installiert, App danach neu gestartet? (§A1) Git LFS bei LFS-Repos? (§A2)
2. **Env:** Nicht-PATH-Variablen im **lokalen Environment-Editor** gesetzt (nicht nur im Shell-Profil)? (§B1)
3. **macOS:** Homebrew-PATH (`/opt/homebrew/bin`) im Editor, falls Tools nicht gefunden werden? (§B3)
4. **Hooks-kritisch?** Wenn dein Workflow auf PostToolUse/SessionStart-Hooks baut → in der **CLI** arbeiten oder Hook-Auslösung verifizieren. (§C1, §C4)
5. **Permissions:** `bypassPermissions` gebraucht? → CLI mit `--dangerously-skip-permissions` statt Desktop. (§D1)
6. **MCP:** Absolute Pfade in der Config; bei Bedarf `claude mcp add-from-claude-desktop`. (§E4)
7. **Worktrees:** `.claude/worktrees/` in `.gitignore`; `.worktreeinclude` für `.env`; committen+pushen, sonst bleiben Worktrees liegen. (§F1, §F3, §F4)
8. **Computer Use (macOS):** Accessibility + Screen Recording erteilt, App neu gestartet — und nach jedem Update neu? (§G1, §G2)
9. **Cloud-Session?** Kein CLAUDE.md/Skills/MCP/Terminal — kontextkritische Arbeit lokal. (§I1)
10. **Skript/CI/Batch?** Bewusst in die CLI — Desktop ist interaktiv. (§J1)
11. **Stabilität:** Bei vielen parallelen Sessions oder riesiger History eher CLI; bei Crash `claude --safe-mode`. (§K1)

---

## §O — CLI-Start-Verknüpfungen (Windows Terminal, selbst erlebt)

### §O1. Kaltstart-Race: CLI-Verknüpfung blitzt auf und schließt sofort (Monarch-Handshake)  [⭐ HÄUFIG — selbst erlebt, 3. Fix-Generation]
**Symptom:** Nach einem Windows-Neustart öffnet der Klick auf eine CLI-Verknüpfung (Claude/OpenCode/Codex/Gemini via `wt.exe new-tab`) kurz ein Terminal-Fenster, das sich sofort wieder schließt; erst nach 5-6 Klicks startet das CLI. Sobald EINMAL ein gesundes Fenster existiert, funktioniert danach jeder Start — bis zum nächsten Reboot. Ein mitstartendes Overlay (TVO-Mikrofonknopf) erscheint/verschwindet mit dem Fenster — reiner Zeuge, kein Täter.
**Ursache (Root Cause, bewiesen 2026-07-03):** Windows Terminal verwaltet Fenster über eine Monarch/Peasant-COM-Architektur. Beim Boot existiert oft ein halbtoter WT-Prozess; jeder Andock-Versuch (`-w 0` ODER Setting `windowingBehavior: useAnyExisting` — das dockt auch OHNE `-w 0` an!) geht an den Sterbenden und das neue Fenster stirbt mit. Beweis im Launcher-Log: bei 5 Klicks in 14 s hatte der einzige WT-Prozess jedes Mal eine andere PID (Prozesse starben fortlaufend); kein Crash im Ereignisprotokoll (Fenster schließen "regulär").
**Warum 2 Fix-Generationen scheiterten:** (1) "Prozess läuft?"-Check (2026-06-11) und (2) "MainWindowHandle != 0?"-Check (2026-06-24) versuchen VORHER zu erraten, ob das Andocken klappt — ein sterbender Prozess kann aber ein gültiges Fensterhandle haben, und `useAnyExisting` machte den "frisches Fenster"-Fallback (ohne `-w`) wirkungslos.
**Versionen:** Windows 11, Windows Terminal 1.24.x; architekturbedingt (Monarch-Election-Races sind in microsoft/terminal mehrfach dokumentiert).
**FIX (Architekturwechsel — prüfen statt raten, 2026-07-03):** Zentrale Funktion `Start-WtCliRobust` in `C:\Users\barwa\start-wt-common.ps1` (Repo-Spiegel: `claude-code-setup/launcher/`), von allen vier Launchern (`start-{claude,opencode,codex,gemini}-wt.ps1`) dot-sourced. Drei Schichten: (a) präventiv Zombie-WT-Prozesse ohne Fenster (>15 s alt) beenden; (b) nach jedem Start bis 8 s verifizieren, ob das innere CLI-pwsh existiert (CIM `Win32_Process`, CommandLine-Match + CreationDate-Filter) und 3 s stabil bleibt — sonst automatisch neu versuchen, ab Versuch 2 mit `-w new` (überschreibt `useAnyExisting` wirklich); (c) nach 3 Fehlversuchen garantierter Fallback in ein klassisches Konsolenfenster (pwsh direkt, kein Monarch). Ein Klick genügt damit immer — das Mehrfach-Klicken übernimmt das Skript.
**Merksatz:** Die Gesundheit des WT-Monarchen ist von außen NICHT zuverlässig erkennbar — Erfolg lässt sich nur NACH dem Start verifizieren.
**Quelle:** eigener Vorfall + Log-Beweis 2026-07-03 (`start-claude-wt.log` 12:11 Uhr); microsoft/terminal Monarch/Peasant-Architektur (offiziell).
