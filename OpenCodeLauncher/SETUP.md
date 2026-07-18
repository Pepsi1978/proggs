# OpenCode Launcher — Einrichtung auf einem neuen Rechner (Windows + macOS)

> Diese Datei beschreibt, **wie das Gesamtsystem aktuell läuft** und **was auf einem neuen Rechner
> nötig ist**, damit alles identisch funktioniert. Du kannst einer KI auf dem neuen Rechner
> einfach sagen: *„Richte alles genau nach `OpenCodeLauncher/SETUP.md` ein."*

Stand: v1.17.32 (18.07.2026, 11:56 Uhr)

---

## 0. Drei Bausteine — nicht verwechseln

Das Gesamtsystem besteht aus **drei** Teilen, jeder mit eigenem Setup:

1. **Claude-Code-Basis** (`~/.claude`) — via **`~/proggs/claude-code-setup/`**
   (`setup-windows.ps1` / `setup-macos.sh`). Login, Hooks, Skills, Statusline, `settings.json`.
   **Voraussetzung** für den Launcher (Minimal blendet `~/.claude/skills` ein, Strikt nutzt `~/.claude/hooks`).
2. **OpenCode-Basis** — via **`~/proggs/opencode-setup/`** (`install.ps1` / `install.sh`).
   Das OpenCode-CLI, die globale `~/.config/opencode/opencode.jsonc`, die **TUI-Seitenleiste**
   (`token-cost-sidebar`-Plugin), Agents/Skills, Sounds. **Windows zusätzlich:** ein selbst gebautes,
   gepatchtes Binary („Mousefix" — s. §6). Auf **macOS entfällt der Mousefix** (stock `opencode` via Homebrew).
3. **OpenCode Launcher** (dieses Verzeichnis) — die WPF-App (Windows-only), die Claude Code / OpenCode
   mit gewähltem Modell **und Profil** startet. macOS-Variante ist in Vorbereitung (§5).

**Reihenfolge auf neuem Rechner:** Bausteine 1 + 2 (Basen), dann 3 (Launcher, nur Windows).
**Wichtig:** `claude-code-setup` und `opencode-setup` sind **getrennt** — nicht vermischen.

---

## 1. Was kommt per `git`, was muss pro Rechner eingerichtet werden

| Kommt per `git pull` (im Repo) | Pro Rechner (NICHT im Repo) |
|---|---|
| Launcher-Quellcode (`OpenCodeLauncher/`) | .NET 8 SDK (nur Windows-Build) |
| **Alle Claude-Profil-Inhalte** (`skills/rules/agents/commands` + `settings.json`) | Claude-Code-Basis via `claude-code-setup` (Login, Hooks, Skills) |
| OpenCode-Profil-Quellen (`Profiles/OpenCode/<id>/AGENTS.md`) | OpenCode-Basis via `opencode-setup` (CLI, `opencode.jsonc`, TUI-Sidebar) |
| `opencode-setup/`, `claude-code-setup/` (Setup-Skripte) | API-Keys als Umgebungsvariablen (aus `~/SK`) |
| Diese `SETUP.md` | Claude-Login-Token (`.credentials.json`) — wird lokal in die Profile kopiert |

**Secrets NIEMALS ins Repo** (Regel: liegen in `~/SK`). Insb. das `GITHUB_PERSONAL_ACCESS_TOKEN`
aus `~/.claude/settings.json` ist bewusst NICHT in den Repo-Profilen.

---

## 2. So laufen die Profile (aktuelle Architektur)

### Claude-Code-Profile — **jedes Profil hat seinen EIGENEN Config-Ordner**

`CLAUDE_CONFIG_DIR` zeigt je Profil auf `OpenCodeLauncher/Profiles/ClaudeCode/<id>` (`id` =
`minimal` | `standard` | `strict`). Der Launcher setzt das beim Start; der Kontext ist damit
**versioniert und auf jedem Rechner gleich**.

| Profil | Skills / Rules / Agents / Commands | Hooks | Isolation |
|---|---|---|---|
| **minimal** | nur **Skills**, per Verzeichnis-Junction `skills → ~/.claude/skills` (Launcher legt sie beim Start via `mklink /J` an, kein Admin nötig) | ❌ | regelfrei — keine Rules/Hooks/Memory |
| **standard** | **versioniert im Repo** (aus `~/.claude` kopiert, frei bearbeitbar) | ❌ (für eigene Hooks reserviert) | eigener Kontext |
| **strict** | **versioniert im Repo** | ✅ **Modus A**: `settings.json` aktiviert die `~/.claude/hooks` (laufen aus der lebenden Installation, immer aktuell) | voller Kontext + Absicherung |

Weitere Details:
- **Aktive `CLAUDE.md`:** wird pro Start aus `Profiles/ClaudeCode/sources/<id>.md` in den Profil-Ordner
  geschrieben (untracked Laufzeitdatei; versioniert ist nur die `sources/`-Quelle).
- **`settings.json` je Profil** ist versioniert und enthält `claudeMdExcludes: ["**/.claude/rules/**"]`
  (verhindert doppeltes Laden der Rules aus Repo **und** `~/.claude`). Strikt zusätzlich die
  bereinigte Hook-Konfiguration (ohne Token, ohne Plugin-Sektionen).
- **Login:** Der Launcher kopiert `.credentials.json` bei Bedarf **einmalig lokal** aus `~/.claude`
  in den Profil-Ordner (per `.gitignore` nie versioniert) → kein erneutes Anmelden je Profil.
- **Abhängigkeit:** Minimal braucht `~/.claude/skills`, Strikt braucht `~/.claude/hooks` — beides
  liefert `claude-code-setup` (Baustein 1).

### OpenCode-Profile

- Quelle je Profil: `OpenCodeLauncher/Profiles/OpenCode/<id>/AGENTS.md`.
- Beim Start schreibt der Launcher diese Quelle in **`~/proggs/AGENTS.md`** (Arbeitsverzeichnis);
  OpenCode liest die `AGENTS.md` dort immer.
- Der Launcher setzt `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1` pro Session → keine `CLAUDE.md` als
  Prompt-Fallback. **Skills (`~/.claude/skills`) und MCP bleiben nutzbar** (bewusst NICHT der volle
  `OPENCODE_DISABLE_CLAUDE_CODE`). Die globale `~/.config/opencode/AGENTS.md` wird leer gehalten.
- `~/proggs/AGENTS.md` ist eine Laufzeitdatei (`.gitignore`, wird immer neu erzeugt).

### macOS-Profile

- Eigener Bereich `OpenCodeLauncher/Profiles/ClaudeCodeMac/<id>` (getrennt von Windows, weil die
  Windows-Profile absolute `C:\`-Pfade + PowerShell-Hooks tragen). Aktuell **Gerüst** (siehe §4).

---

## 3. Einrichtung Windows (Schritt für Schritt)

1. **Repo holen**
   ```powershell
   git clone <proggs-Remote> ~/proggs      # oder: cd ~/proggs; git pull
   ```

2. **Baustein 1 — Claude-Code-Basis** (PowerShell als Administrator)
   ```powershell
   cd ~/proggs/claude-code-setup
   .\setup-windows.ps1
   Copy-Item ~/proggs/claude-code-setup/settings-reference.json ~/.claude/settings.json
   ```
   Danach **Claude Code installieren + einloggen**. Prüfen: `~/.claude/skills` und `~/.claude/hooks`
   vorhanden (Minimal + Strikt brauchen sie).

3. **Baustein 2 — OpenCode-Basis**
   ```powershell
   cd ~/proggs/opencode-setup
   .\install.ps1        # installiert/baut das gepatchte Binary (Mousefix, §6), opencode.jsonc, TUI-Sidebar
   ```
   Danach `opencode auth login`. API-Keys (`OPENROUTER_API_KEY` …) als Umgebungsvariablen aus `~/SK`.
   (Details: `opencode-setup/README.md`.)

4. **.NET 8 SDK installieren** — https://dotnet.microsoft.com/download

5. **Launcher bauen**
   ```powershell
   dotnet build ~/proggs/OpenCodeLauncher/OpenCodeLauncher.csproj -c Release
   ```
   Ergebnis: `OpenCodeLauncher/bin/Release/net8.0-windows10.0.19041.0/win-x64/OpenCodeLauncher.exe`

6. **Desktop-Verknüpfung**
   ```powershell
   pwsh ~/proggs/OpenCodeLauncher/create_shortcut.ps1
   ```
   Updates später: `pwsh ~/proggs/OpenCodeLauncher/update-launcher.ps1` (schließt laufenden
   Launcher, baut Release, startet neu).

7. **Erster Start:** Launcher öffnen → Profil (Minimal/Standard/Strikt) + Modell wählen → starten.
   Beim ersten Minimal-Start entsteht die Skills-Junction automatisch; der Login-Token wird bei
   Bedarf lokal in den Profil-Ordner kopiert.

> **Windows-Portabilität:** Die Profile funktionieren rechnerübergreifend, solange **derselbe
> Benutzername** verwendet wird (gleiche `C:\Users\<name>`-Pfade). Strikts Hooks zeigen über
> `$USERPROFILE/.claude/hooks` (portabel), brauchen aber die von `claude-code-setup` deployten Hooks.

---

## 4. Einrichtung macOS (Schritt für Schritt)

> **Status:** Der WPF-Launcher läuft **nicht** auf macOS. Bis die macOS-Launcher-Variante existiert
> (§5), startest du Claude Code mit den macOS-Profilen **manuell** über `CLAUDE_CONFIG_DIR` und
> OpenCode direkt per `opencode`. Die Profile selbst funktionieren identisch.

1. **Repo holen**
   ```bash
   git clone <proggs-Remote> ~/proggs      # oder: cd ~/proggs && git pull
   ```

2. **Baustein 1 — Claude-Code-Basis**
   ```bash
   cd ~/proggs/claude-code-setup
   chmod +x setup-macos.sh && ./setup-macos.sh
   cp ~/proggs/claude-code-setup/settings.json ~/.claude/settings.json
   brew install jq                          # für die Statusline
   ```
   Claude Code installieren + einloggen. Prüfen: `~/.claude/skills` und `~/.claude/hooks` vorhanden.

3. **Baustein 2 — OpenCode-Basis**
   ```bash
   brew install anomalyco/tap/opencode      # stock OpenCode-CLI (KEIN Mousefix nötig, §6)
   bash ~/proggs/opencode-setup/install.sh  # opencode.jsonc (shell->bash), TUI-Sidebar, Agents, Skills, Sounds
   opencode auth login
   ```
   Voraussetzungen: SK-Keys / `OPENROUTER_API_KEY`, ggf. WireGuard-Tunnel (10.8.0.1) für das
   second-brain-MCP. **Vor dem ersten Lauf `opencode-setup/install.sh` prüfen** — s. §6 (TUI-Dep-Fix).

4. **macOS-Profile befüllen** — der Bereich `Profiles/ClaudeCodeMac/<id>` ist ein Gerüst
   (READMEs + `.gitignore`). Einmalig mit macOS-Inhalten füllen:
   ```bash
   cd ~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac
   for p in standard strict; do
     for d in skills rules agents commands; do
       rm -rf "$p/$d"; cp -R "$HOME/.claude/$d" "$p/$d"
     done
   done
   ```
   Dann pro Profil eine `settings.json` mit **macOS-Pfaden** (Statusline/Hooks als `bash`/`zsh` unter
   `$HOME/.claude/...`, plus `claudeMdExcludes: ["**/.claude/rules/**"]`). **Keine Secrets ins Repo.**
   Committen + pushen.

5. **Minimal-Skills auf macOS** — Symlink statt Windows-Junction (nicht versioniert):
   ```bash
   ln -s "$HOME/.claude/skills" ~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac/minimal/skills
   ```

6. **Manueller Claude-Start (bis die macOS-App existiert)** — je Profil:
   ```bash
   export CLAUDE_CONFIG_DIR=~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac/standard
   cp ~/.claude/.credentials.json "$CLAUDE_CONFIG_DIR/.credentials.json"   # Login lokal, einmalig
   cp ~/proggs/OpenCodeLauncher/Profiles/ClaudeCode/sources/standard.md "$CLAUDE_CONFIG_DIR/CLAUDE.md"
   claude --model <modell>
   ```
   OpenCode startest du auf macOS direkt: `cd ~/proggs && opencode` (kein Launcher nötig, §6).

---

## 5. Die macOS-Launcher-App bauen (Bau-Spezifikation)

> Die Windows-App ist **WPF** (Windows-only) — NICHT 1:1 nach macOS kopierbar. Für macOS eine
> **native App** bauen. Dieser Abschnitt beschreibt, **was die App tun muss** (identisch zu Windows)
> und **was auf macOS anders ist** (v. a. Pfade), damit die Profile identisch funktionieren.

### 5.1 Technologie-Wahl
- **Avalonia UI (C#)** — maximale Wiederverwendung der bestehenden Services
  (`InstructionProfileService`, `OpenCodeLauncherService`); nur UI + plattformabhängige Teile neu.
  Build: `dotnet build -c Release -r osx-arm64` (bzw. `osx-x64`) → `.app`-Bundle.
- **Alternativ Swift/AppKit** — komplette Neuimplementierung der Logik; dann ist die Spec unten die Vorlage.

### 5.2 Was die App tun muss (identisch zu Windows)
1. Modell + Profil (`minimal`/`standard`/`strict`) + Effort wählen.
2. Beim Start `CLAUDE_CONFIG_DIR` auf den macOS-Profil-Ordner (`Profiles/ClaudeCodeMac/<id>`) setzen.
3. Aktive `CLAUDE.md` in den Profil-Ordner schreiben — **Quelle:** `Profiles/ClaudeCode/sources/<id>.md`
   (der Regeltext ist plattformneutral; nur der Config-Ordner ist macOS-spezifisch).
4. Login-Token bei Bedarf lokal kopieren.
5. Minimal: Skills per Symlink einblenden.
6. Claude Code / OpenCode in einem Terminal starten (mit gesetztem `CLAUDE_CONFIG_DIR`).

### 5.3 Was auf macOS ANDERS ist (kritisch)
| Aspekt | Windows | macOS |
|---|---|---|
| Profil-Config-Ordner | `Profiles/ClaudeCode/<id>` | `Profiles/ClaudeCodeMac/<id>` |
| Home / Env | `C:\Users\<name>` · `$env:USERPROFILE` | `/Users/<name>` · `$HOME` |
| Skills einblenden (Minimal) | Junction `mklink /J` | Symlink `ln -s "$HOME/.claude/skills" <id>/skills` |
| Terminal-Start | Windows Terminal (`wt`) + `pwsh -File` | `Terminal.app`/iTerm2 via `osascript`/`open`, `zsh`/`bash` |
| Hooks (Strikt) | `pwsh … .ps1`, `$USERPROFILE/.claude/hooks` | `bash/zsh … .sh`, `$HOME/.claude/hooks`; Quelle `claude-code-setup/hooks-macos.json` |
| Login-Token kopieren | `Copy-Item` | `cp "$HOME/.claude/.credentials.json" …` |
| Build-Ziel | `win-x64` → `.exe` | `osx-arm64`/`osx-x64` → `.app` |
| Statusline | `statusline.ps1` | `statusline.sh` (braucht `jq`) |
| OpenCode-Binary | gepatchtes Mousefix-Binary | stock `opencode` (Homebrew) — kein Fix |

### 5.4 Portierungs-Referenz (welcher Windows-Code was macht)
- `InstructionProfileService.ResolveClaudeConfigDir(id)` → Profil-Ordner-Pfad → **macOS:** `ClaudeCodeMac/<id>`.
- `InstructionProfileService.EnsureClaudeConfigDir(id)` → `CLAUDE.md` schreiben + Login kopieren + (Minimal) Skills einblenden.
- `InstructionProfileService.EnsureSkillsJunction` → **macOS:** Symlink-Anlage statt Junction (idempotent).
- `InstructionProfileService.EnsureLoginToken` → `.credentials.json` aus `~/.claude` kopieren.
- `OpenCodeLauncherService.BuildClaudeCodeStartScript` → statt `$env:CLAUDE_CONFIG_DIR=…; & claude …`
  (pwsh) → `export CLAUDE_CONFIG_DIR=…; claude …` (zsh/bash).
- `OpenCodeLauncherService.LaunchClaudeCode` / `Launch` → statt `wt new-tab …` ein `osascript`-Aufruf,
  der Terminal.app/iTerm2 mit dem Start-Skript öffnet.
- `ResolveOpenCodeExecutable` → **macOS:** einfach `opencode` (kein Mousefix-Pointer), **oder** den
  OpenCode-Teil weglassen und direkt `opencode` starten lassen.

### 5.5 Strikt-Hooks auf macOS
Eine **eigene** `Profiles/ClaudeCodeMac/strict/settings.json` erzeugen — mit den Hook-Einträgen aus
`claude-code-setup/hooks-macos.json` (bash/zsh, `$HOME/.claude/hooks/*.sh`) und
`claudeMdExcludes: ["**/.claude/rules/**"]`. **Token + Plugin-Sektionen NIEMALS ins Repo.**

### 5.6 Verifikation nach dem Bau
- Jedes Profil starten, `/context` prüfen: **Minimal** (Skills da, regelfrei), **Standard**
  (versionierte Rules/Skills, **keine** `~/.claude/rules`-Doppelladung), **Strikt** (zusätzlich Hooks aktiv).
- Login greift ohne erneutes Anmelden. „Messages" in `/context` bleibt klein bei Nulleingabe
  (geladener Kontext steht unter *Memory files / Skills / Custom agents*, nicht *Messages*).

---

## 6. Für macOS: OpenCode-CLI, TUI-Sidebar und der Windows-Mousefix

Diese Punkte betreffen **OpenCode** (Baustein 2), nicht den Launcher — sie müssen auf macOS bedacht werden:

### 6.1 Der Windows-Mousefix entfällt auf macOS
Unter Windows läuft ein **selbst gebautes, gepatchtes `opencode.exe`** (Build:
`opencode-setup/build-install-windows-mousefix.ps1`, ausgelöst von `install.ps1`; Binary unter
`~/.local/share/opencode-mousefix/`). Es fixt **Windows-Terminal-spezifische** Probleme:
Maus-Copy-on-select + Clipboard (`Get-Clipboard`), Full-Repaint-Recovery, TUI-`--variant`,
den **stderr/TUI-Crash-Handler („windowsfix.9")** und einen Runtime-Plugin-Toggle.

**macOS braucht das NICHT.** Der Patch macht Windows nur zu dem, was macOS von Haus aus tut
(Copy-on-select, `pbpaste`, stabiles Rendering). Auf macOS läuft das **stock `opencode`** aus Homebrew.
→ **Kein macOS-Mousefix bauen.** (Einzige bekannte macOS-Eigenheit, unabhängig davon: der Keybind-Leader
`ctrl+x` wird oft vom Terminal abgefangen — ggf. in `opencode.jsonc` umbelegen.)

### 6.2 Die TUI-Seitenleiste ist cross-platform — aber ein Dep-Fix nötig
Die rechte Seitenleiste ist das Plugin **`token-cost-sidebar`** (`opencode-setup/plugins/token-cost-sidebar/`,
reines TypeScript/TSX). Es wird auf macOS von `opencode-setup/install.sh` **mitinstalliert** und über
`opencode-setup/tui.json` (`"plugin": ["./plugins/token-cost-sidebar"]`) geladen — es ist **nicht** „fehlend".

**⚠️ Bekannter Stolperstein:** `install.sh` pinnt für die Sidebar **veraltete** OpenTUI-Versionen
(`@opentui/core@0.3.4`, `@opentui/solid@0.4.0`, `@opencode-ai/plugin@1.17.7`), während die Plugin-
`package.json` und `install.ps1` (Windows) **`0.4.3` / `1.17.15`** verlangen. Auf macOS kann die
Sidebar dadurch anders/kaputt aussehen. **Fix:** in `opencode-setup/install.sh` die npm-Install-Zeile
an die `0.4.3`-Versionen der Plugin-`package.json` angleichen.

### 6.3 Kein Launcher auf macOS → direkter OpenCode-Start
Der WPF-Launcher patcht unter Windows die `opencode.jsonc` **vor** dem Start (Provider-Routing ohne
Fallback, Reasoning-Effort). Auf macOS gibt es (noch) keinen Launcher — man startet direkt
`cd ~/proggs && opencode`. Provider-/Effort-Steuerung dort **manuell** in `~/.config/opencode/opencode.jsonc`
oder OpenCode-nativ (`/models`, `Ctrl+T`-Variant). Sobald die macOS-Launcher-App (§5) existiert, kann
sie diese Config-Patch-Logik übernehmen (Code-Vorlage: `OpenCodeLauncherService.ConfigureProvider`/`PatchProvider`).

---

## 7. Was der Launcher zur Laufzeit selbst erzeugt (nicht sichern)
- `~/proggs/AGENTS.md` (aktives OpenCode-Profil) — ignoriert.
- `Profiles/ClaudeCode(Mac)/<id>/CLAUDE.md` (aktive Claude-Regeln) — ignoriert.
- `Profiles/ClaudeCode(Mac)/<id>/.credentials.json` (lokaler Login) — ignoriert, **Secret**.
- `Profiles/ClaudeCode(Mac)/minimal/skills` (Junction/Symlink) — lokal, ignoriert.
- Sessions unter `%LOCALAPPDATA%/OpenCodeLauncher/sessions/` — temporär.

## 8. Profile ändern
- **Kontext/Regeln:** `Profiles/ClaudeCode/sources/<id>.md` (bzw. OpenCode: `Profiles/OpenCode/<id>/AGENTS.md`).
- **Skills/Rules/Agents/Commands (Standard/Strikt):** direkt in `Profiles/ClaudeCode/<id>/` (bzw. `…Mac/<id>/`) bearbeiten.
- Danach `git add/commit/push` — andere Rechner bekommen es per `git pull`.
