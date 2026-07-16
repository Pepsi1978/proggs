# OpenCode Launcher — Einrichtung auf einem neuen Rechner (Windows + macOS)

> Diese Datei beschreibt, **wie die Profile aktuell laufen** und **was auf einem neuen Rechner
> nötig ist**, damit alles identisch funktioniert. Du kannst einer KI auf dem neuen Rechner
> einfach sagen: *„Richte den OpenCode Launcher genau nach `OpenCodeLauncher/SETUP.md` ein."*

Stand: v1.17.23 (16.07.2026, 12.45 Uhr)

---

## 0. Zwei Bausteine — nicht verwechseln

Das Gesamtsystem besteht aus **zwei** Teilen, die zusammenspielen:

1. **Die `~/.claude`-Basis** (Login, Hooks, Skills, Statusline, `settings.json`) — wird über
   **`~/proggs/claude-code-setup/`** eingerichtet (dessen `setup-windows.ps1` / `setup-macos.sh`).
   Das ist die „Single Source of Truth" der Claude-Code-Umgebung. **Diese Basis ist Voraussetzung**
   für den Launcher (Minimal blendet `~/.claude/skills` ein, Strikt nutzt `~/.claude/hooks`).
2. **Der OpenCode Launcher** (dieses Verzeichnis) — die WPF-App, die Claude Code / OpenCode mit
   gewähltem Modell **und Profil** startet. Windows-only; macOS-Variante ist in Vorbereitung (§4).

**Reihenfolge auf einem neuen Rechner:** zuerst `claude-code-setup` (Baustein 1), dann dieser
Launcher (Baustein 2).

---

## 1. Was kommt per `git`, was muss pro Rechner eingerichtet werden

| Kommt per `git pull` (im Repo) | Pro Rechner (NICHT im Repo) |
|---|---|
| Launcher-Quellcode (`OpenCodeLauncher/`) | .NET 8 SDK (Windows-Build) |
| **Alle Profil-Inhalte** (Claude: `skills/rules/agents/commands` + `settings.json`; OpenCode: `AGENTS.md`) | `~/.claude`-Basis via `claude-code-setup` (Login, Hooks, Skills) |
| `create_shortcut.ps1`, `update-launcher.ps1` | OpenCode-Installation (`npm i -g opencode-ai`) + `opencode.jsonc` |
| Diese `SETUP.md` | API-Keys als Umgebungsvariablen (aus `~/SK`) |
| | Claude-Login-Token (`.credentials.json`) — wird lokal in die Profile kopiert |

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
| **minimal** | nur **Skills**, per Verzeichnis-Junction `skills → ~/.claude/skills` (der Launcher legt sie beim Start via `mklink /J` an, kein Admin nötig) | ❌ | regelfrei — keine Rules/Hooks/Memory |
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

2. **Baustein 1 — `~/.claude`-Basis** (PowerShell als Administrator)
   ```powershell
   cd ~/proggs/claude-code-setup
   .\setup-windows.ps1
   Copy-Item ~/proggs/claude-code-setup/settings-reference.json ~/.claude/settings.json
   ```
   Danach **Claude Code installieren + einloggen** (Login-Token landet in `~/.claude/.credentials.json`).
   Prüfen, dass `~/.claude/skills` und `~/.claude/hooks` vorhanden sind (Minimal + Strikt brauchen sie).

3. **.NET 8 SDK installieren** — https://dotnet.microsoft.com/download

4. **Launcher bauen**
   ```powershell
   dotnet build ~/proggs/OpenCodeLauncher/OpenCodeLauncher.csproj -c Release
   ```
   Ergebnis: `OpenCodeLauncher/bin/Release/net8.0-windows10.0.19041.0/win-x64/OpenCodeLauncher.exe`

5. **Desktop-Verknüpfung**
   ```powershell
   pwsh ~/proggs/OpenCodeLauncher/create_shortcut.ps1
   ```
   Updates später: `pwsh ~/proggs/OpenCodeLauncher/update-launcher.ps1` (schließt laufenden
   Launcher, baut Release, startet neu).

6. **OpenCode installieren + Config** (nur für den OpenCode-Teil)
   ```powershell
   npm install -g opencode-ai
   ```
   Globale `~/.config/opencode/opencode.jsonc` (Provider/Modelle, `"lsp": true`, MCP) von einem
   eingerichteten Rechner übernehmen — **nicht im Repo** (kann Keys via `{env:...}` referenzieren).

7. **API-Keys als Umgebungsvariablen** (z. B. `OPENROUTER_API_KEY`, `OPENAI_API_KEY`) aus `~/SK`.

8. **Erster Start:** Launcher öffnen → Profil (Minimal/Standard/Strikt) + Modell wählen → starten.
   Beim ersten Minimal-Start entsteht die Skills-Junction automatisch; der Login-Token wird bei
   Bedarf lokal in den Profil-Ordner kopiert.

> **Voraussetzung Windows-Portabilität:** Die Profile funktionieren rechnerübergreifend, solange
> derselbe Benutzername verwendet wird (gleiche `C:\Users\<name>`-Pfade). Strikts Hooks zeigen über
> `$USERPROFILE/.claude/hooks` (portabel), brauchen aber die von `claude-code-setup` deployten Hooks.

---

## 4. Einrichtung macOS (Schritt für Schritt)

> **Status:** Der WPF-Launcher läuft **nicht** auf macOS (Windows-only). Die macOS-Launcher-Variante
> (Swift/AppKit) ist in Vorbereitung. Bis dahin startest du Claude Code mit den macOS-Profilen
> **manuell** über `CLAUDE_CONFIG_DIR` — die Profile selbst funktionieren identisch.

1. **Repo holen**
   ```bash
   git clone <proggs-Remote> ~/proggs      # oder: cd ~/proggs && git pull
   ```

2. **Baustein 1 — `~/.claude`-Basis**
   ```bash
   cd ~/proggs/claude-code-setup
   chmod +x setup-macos.sh && ./setup-macos.sh
   cp ~/proggs/claude-code-setup/settings.json ~/.claude/settings.json
   brew install jq            # für die Statusline
   ```
   Claude Code installieren + einloggen. Prüfen, dass `~/.claude/skills` und `~/.claude/hooks`
   vorhanden sind (die macOS-Hooks liegen in `claude-code-setup/hooks-macos.json`).

3. **macOS-Profile befüllen** — der Bereich `OpenCodeLauncher/Profiles/ClaudeCodeMac/<id>` ist ein
   Gerüst (READMEs + `.gitignore`). Einmalig mit macOS-Inhalten füllen:
   ```bash
   cd ~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac
   for p in standard strict; do
     for d in skills rules agents commands; do
       rm -rf "$p/$d"; cp -R "$HOME/.claude/$d" "$p/$d"
     done
   done
   ```
   Dann pro Profil eine `settings.json` mit **macOS-Pfaden** anlegen (Statusline/Hooks als
   `bash`/`zsh` unter `/Users/<name>/.claude/...`, plus `claudeMdExcludes: ["**/.claude/rules/**"]`).
   **Kein `GITHUB_PERSONAL_ACCESS_TOKEN` und keine Secrets** ins Repo. Committen + pushen.

4. **Minimal-Skills auf macOS** — statt der Windows-Junction ein Symlink (nicht versioniert):
   ```bash
   ln -s "$HOME/.claude/skills" ~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac/minimal/skills
   ```

5. **Manueller Start (bis die macOS-App existiert)** — je Profil:
   ```bash
   export CLAUDE_CONFIG_DIR=~/proggs/OpenCodeLauncher/Profiles/ClaudeCodeMac/standard
   cp ~/.claude/.credentials.json "$CLAUDE_CONFIG_DIR/.credentials.json"   # Login lokal, einmalig
   cp ~/proggs/OpenCodeLauncher/Profiles/ClaudeCode/sources/standard.md "$CLAUDE_CONFIG_DIR/CLAUDE.md"
   claude --model <modell>
   ```
   (Für `strict`/`minimal` analog mit dem jeweiligen Ordner + Quelle.)

---

## 5. Was der Launcher zur Laufzeit selbst erzeugt (nicht sichern)
- `~/proggs/AGENTS.md` (aktives OpenCode-Profil) — ignoriert.
- `Profiles/ClaudeCode/<id>/CLAUDE.md` (aktive Claude-Regeln) — ignoriert.
- `Profiles/ClaudeCode/<id>/.credentials.json` (lokaler Login) — ignoriert, **Secret**.
- `Profiles/ClaudeCode/minimal/skills` (Junction) — lokal, ignoriert.
- Sessions unter `%LOCALAPPDATA%/OpenCodeLauncher/sessions/` — temporär.

## 6. Profile ändern
- **Kontext/Regeln:** `Profiles/ClaudeCode/sources/<id>.md` (bzw. OpenCode: `Profiles/OpenCode/<id>/AGENTS.md`).
- **Skills/Rules/Agents/Commands (Standard/Strikt):** direkt in `Profiles/ClaudeCode/<id>/` bearbeiten.
- Danach `git add/commit/push` — andere Rechner bekommen es per `git pull`.
