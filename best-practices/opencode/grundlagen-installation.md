# Grundlagen & Installation — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Tool: **OpenCode** von SST/Anomaly (GitHub `anomalyco/opencode`, früher `sst/opencode`; Website
> `opencode.ai`). NICHT verwechseln mit der Klon-Domain `open-code.ai` — im Zweifel ist `opencode.ai`
> autoritativ. Quellen je Aussage als `offiziell` (opencode.ai/docs, GitHub) bzw. `extern` markiert.

---

## 1. Was ist OpenCode? Architektur & Abgrenzung

OpenCode ist ein quelloffener KI-Coding-Agent, der primär im Terminal läuft — verfügbar als **TUI**
(Terminal-Oberfläche), **Desktop-App** (Beta) und **IDE-Extension**. `offiziell` (opencode.ai/docs)

Kernunterschiede zu Claude Code (laut offizieller FAQ): `offiziell` (GitHub README)
- **100 % Open Source.**
- **Provider-agnostisch** — nicht an einen Anbieter gekoppelt. Nutzbar mit Anthropic (Claude), OpenAI,
  Google, OpenRouter oder lokalen Modellen. Modell-Liste von **models.dev**.
- **Eingebauter, opt-in LSP-Support** (Code-Intelligenz).
- **TUI-Fokus**, gebaut von Neovim-Nutzern.
- **Client/Server-Architektur:** Der Server kann separat laufen (`opencode serve` / `opencode web`),
  ein TUI hängt sich per `opencode attach <url>` dran — auch an einen entfernten Rechner.

Eingebaute Agents (Tab-umschaltbar): `build` (Vollzugriff, Standard) und `plan` (read-only, Analyse).
Subagents: `general`, `explore`, `scout`. `offiziell` (opencode.ai/docs/agents) — Details siehe `agents-modes.md`.

---

## 2. Installation

### 2.1 Universell (alle Plattformen)

**Install-Skript (empfohlen):** `offiziell` (opencode.ai/docs)
```bash
curl -fsSL https://opencode.ai/install | bash
```
Zielverzeichnis-Priorität: `$OPENCODE_INSTALL_DIR` → `$XDG_BIN_DIR` → `$HOME/bin` → `$HOME/.opencode/bin`.

**Über Node.js — Paketname ist `opencode-ai`, NICHT `opencode`:** `offiziell`
```bash
npm install -g opencode-ai@latest
bun add -g opencode-ai
pnpm add -g opencode-ai
yarn global add opencode-ai
```
Weitere Wege: `mise use -g opencode`, `nix run nixpkgs#opencode`.

> **Vor der Installation alte Versionen < 0.1.x entfernen.** `offiziell` (README TIP)

**Verifizieren:** `opencode --version`

### 2.2 macOS / Linux via Homebrew
```bash
brew install anomalyco/tap/opencode     # empfohlen (immer aktuell)
brew install opencode                   # offizielle Community-Formel (seltener aktualisiert)
brew install --cask opencode-desktop    # Desktop-App
```
Die Doku empfiehlt ausdrücklich den **OpenCode-Tap** für die aktuellsten Releases. `offiziell`

### 2.3 Windows

> **Offizielle Empfehlung: WSL benutzen** — bessere Dateisystem-Performance, voller Terminal-Support,
> volle Feature-Kompatibilität. Native Windows-Installation geht, ist aber die Quelle der meisten Probleme.
> `offiziell` (opencode.ai/docs/windows-wsl)

**Native Windows-Wege:** `offiziell`
```powershell
scoop install opencode        # am unkompliziertesten (setzt PATH automatisch)
choco install opencode        # Chocolatey
npm install -g opencode-ai    # fehleranfällig auf nativem Windows (siehe §7)
mise use -g github:anomalyco/opencode
docker run -it --rm ghcr.io/anomalyco/opencode
```
> **Bun-Installation auf Windows ist noch in Arbeit** (Stand Juni 2026). `offiziell`

Desktop-App (Scoop): `scoop bucket add extras` + `scoop install extras/opencode-desktop`.

**Praxis:** **Scoop** wird empfohlen (PATH automatisch, umgeht npm-Probleme). Ein Maintainer empfiehlt
**Chocolatey** (`choco install opencode`), solange npm-Wrapper-Probleme bestehen. `extern` (GitHub #2447)

> Der `curl ... | bash`-Installer läuft NICHT in PowerShell/CMD (dort ist `curl` ein Alias für
> `Invoke-WebRequest`). Nur in **Git Bash** oder **WSL**. `extern`

### 2.4 WSL-Setup im Detail (empfohlener Windows-Weg) `offiziell`
```bash
# Im WSL-Terminal:
curl -fsSL https://opencode.ai/install | bash
cd /mnt/c/Users/DeinName/project     # Windows-Laufwerke: C: -> /mnt/c/, D: -> /mnt/d/
opencode
```
- **Tipp:** Repo besser **ins WSL-Dateisystem** klonen (z.B. `~/code/`) statt über `/mnt/c/` (schneller).
- Config/Sessions liegen im WSL-Linux-Home unter `~/.local/share/opencode/`.

Desktop-App + WSL-Server kombinieren:
```bash
OPENCODE_SERVER_PASSWORD=dein-passwort opencode serve --hostname 0.0.0.0 --port 4096
# Desktop-App mit http://localhost:4096 verbinden (falls localhost nicht geht: `hostname -I` -> WSL-IP)
```
> **Sicherheit:** Bei `--hostname 0.0.0.0` immer `OPENCODE_SERVER_PASSWORD` setzen. `offiziell`

### 2.5 Arch Linux
```bash
sudo pacman -S opencode      # Stable
paru -S opencode-bin         # Latest (AUR)
```

### 2.6 Upgrade & Deinstallation `offiziell`
```bash
opencode upgrade                 # neueste Version
opencode upgrade v0.1.48         # bestimmte Version
opencode upgrade --method npm    # Methode: curl|npm|pnpm|bun|brew
opencode uninstall               # Flags: --keep-config (-c), --keep-data (-d), --dry-run, --force (-f)
```

---

## 3. Erste Schritte (CLI)

### 3.1 Empfohlene Terminals
WezTerm, Alacritty (cross-platform), Ghostty, Kitty (Linux/macOS). `offiziell`

### 3.2 Authentifizierung / Provider verbinden
**(a) In der TUI:** `/connect` → Provider wählen → API-Key einfügen. Für Einsteiger empfiehlt OpenCode
**OpenCode Zen** (kuratierte Modelle). `offiziell`

**(b) Per CLI:** `offiziell`
```bash
opencode auth login          # interaktiver Login (--provider/-p, --method/-m)
opencode auth list           # authentifizierte Provider zeigen
opencode auth logout         # Credentials löschen
```
Credentials liegen in `~/.local/share/opencode/auth.json`. Beim Start lädt OpenCode Provider aus dieser
Datei sowie aus Umgebungsvariablen / `.env` im Projekt.

### 3.3 OpenCode starten
```bash
opencode                     # TUI im aktuellen Verzeichnis
opencode /pfad/zum/projekt   # TUI für ein bestimmtes Verzeichnis
```

### 3.4 Non-interaktiv / headless (`run`) `offiziell`
```bash
opencode run "Explain how closures work in JavaScript"
# An laufenden Server hängen (vermeidet MCP-Kaltstart bei jedem Lauf):
opencode serve                                              # Terminal 1
opencode run --attach http://localhost:4096 "..."          # Terminal 2
```
Wichtige `run`-Flags: `--continue`/`-c`, `--session`/`-s`, `--fork`, `--share`, `--model`/`-m`,
`--agent`, `--file`/`-f`, `--format default|json`, `--title`, `--attach`, `--variant`, `--thinking`,
`--dangerously-skip-permissions` (gefährlich — Berechtigungen auto-bestätigen).

### 3.5 Hilfe & globale Flags `offiziell`
```bash
opencode --help / -h
opencode --version / -v
```
Globale Flags: `--print-logs` (Logs nach stderr), `--log-level DEBUG|INFO|WARN|ERROR`, `--pure` (ohne Plugins).

### 3.6 Wichtigste Unterbefehle `offiziell` (opencode.ai/docs/cli)

| Befehl | Zweck |
|--------|-------|
| `opencode [project]` | TUI starten |
| `opencode run [message..]` | non-interaktiver Lauf |
| `opencode auth [login\|list\|logout]` | Provider-Credentials |
| `opencode models [provider]` | Modelle als `provider/model` listen (`--refresh`, `--verbose`) |
| `opencode serve` / `opencode web` | headless Server / + Web-UI |
| `opencode attach [url]` | TUI an laufenden Server (auch remote) |
| `opencode agent [create\|list]` | Agenten anlegen/listen |
| `opencode mcp [add\|list\|auth\|logout\|debug]` | MCP-Server verwalten |
| `opencode session [list\|delete]` | Sessions verwalten |
| `opencode stats` | Token-Verbrauch & Kosten (`--days`, `--models`, `--project`) |
| `opencode export [id]` / `opencode import <file\|url>` | Session JSON exportieren/importieren (`--sanitize`) |
| `opencode pr <number>` | GitHub-PR-Branch auschecken + OpenCode starten |
| `opencode github [install\|run]` | GitHub-Agent für Repo-Automation |
| `opencode upgrade` / `opencode uninstall` | Update / Deinstallation |
| `opencode plugin <module>` (Alias `plug`) | Plugin installieren (`-g`, `-f`) |
| `opencode debug paths` | **(undokumentiert, sehr nützlich)** zeigt exakte Pfade je OS |

---

## 4. TUI-Bedienung

### 4.1 Eingabe-Präfixe `offiziell` (opencode.ai/docs/tui)
- **`@`** — Fuzzy-Dateisuche; Dateiinhalt wird automatisch dem Gespräch hinzugefügt.
- **`!`** — Nachricht mit `!` beginnen → Shell-Befehl ausführen (`!ls -la`), Ausgabe als Tool-Ergebnis.
- **`/`** — Slash-Befehle (siehe §5).
- **Bilder** per Drag-and-drop ins Terminal ziehen.

### 4.2 Leader-Key
OpenCode nutzt einen **Leader-Key** (Default **`ctrl+x`**): erst Leader, dann Buchstabe. Neue Session =
`ctrl+x` dann `n`. `leader_timeout` Default 2000 ms. `offiziell` (opencode.ai/docs/keybinds)

### 4.3 Wichtigste Standard-Tastenkürzel (`<leader>` = `ctrl+x`) `offiziell`

**Agent/Modus & Modell:** Agent durchwechseln `tab` (rückwärts `shift+tab`); Agent-Liste `<leader>a`;
Modell-Liste `<leader>m`; Provider-Liste `ctrl+a`; Modell-Favorit `ctrl+f`; Modell-Varianten/Reasoning
`ctrl+t`.

**Session:** neu `<leader>n`; Liste `<leader>l`; Timeline `<leader>g`; umbenennen `ctrl+r`; löschen
`ctrl+d`; kompaktieren `<leader>c`; unterbrechen `escape`; exportieren `<leader>x`.

**Subagent-Navigation (ohne Leader):** erste Child-Session `<leader>down`; nächste `right`; vorherige
`left`; zurück zum Parent `up`.

**Nachrichten:** Undo `<leader>u`; Redo `<leader>r`; kopieren `<leader>y`; Seite hoch/runter
`pageup`/`pagedown`.

**App/UI:** Kommando-Palette `ctrl+p`; Editor `<leader>e`; Themes `<leader>t`; Sidebar `<leader>b`;
Hilfe `<leader>h`; beenden `ctrl+c`/`<leader>q`; „Which-Key"-Overlay `ctrl+alt+k`.

### 4.4 Maus & Themes
Maus standardmäßig an (`OPENCODE_DISABLE_MOUSE` zum Deaktivieren). Themes per `/themes` oder `<leader>t`
(Default `"opencode"`). Diff-Darstellung `diff_style`: `"auto"` oder `"stacked"`. `offiziell`

### 4.5 TUI konfigurieren (`tui.json`)
Die TUI wird über `tui.json`/`tui.jsonc` konfiguriert — **getrennt** von `opencode.json`. `keybinds`
wird mit Defaults gemerged (nur Abweichungen angeben). Custom-Pfad via `OPENCODE_TUI_CONFIG`. `offiziell`
```jsonc
{
  "$schema": "https://opencode.ai/tui.json",
  "theme": "opencode",
  "leader_timeout": 2000,
  "keybinds": { "leader": "ctrl+x", "command_list": "ctrl+p" },
  "scroll_speed": 3,
  "diff_style": "auto",
  "mouse": true,
  "attention": { "enabled": true, "notifications": true, "sound": true, "volume": 0.4 }
}
```
Keybind deaktivieren: `"none"`/`false`. Attention-Feature (Benachrichtigungen/Sounds bei Fragen/Fehlern/
fertigen Sessions) ist per Default aus. View-Einstellungen über `ctrl+p` (persistieren).

---

## 5. Sessions & Slash-Befehle

### 5.1 Slash-Befehle in der TUI `offiziell`

| Befehl | Aliase | Funktion | Keybind |
|--------|--------|----------|---------|
| `/help` | | Hilfe | |
| `/init` | | AGENTS.md anlegen/aktualisieren | |
| `/new` | `/clear` | neue Session | `ctrl+x n` |
| `/sessions` | `/resume`, `/continue` | Sessions listen & wechseln | `ctrl+x l` |
| `/compact` | `/summarize` | Session kompaktieren | `ctrl+x c` |
| `/models` | | Modelle listen | `ctrl+x m` |
| `/connect` | | Provider hinzufügen / API-Key | |
| `/undo` | | letzte Nachricht + Dateiänderungen rückgängig | `ctrl+x u` |
| `/redo` | | wiederherstellen | `ctrl+x r` |
| `/share` / `/unshare` | | Session teilen / Teilen beenden | |
| `/editor` | | externen `$EDITOR` öffnen | `ctrl+x e` |
| `/export` | | Konversation als Markdown | `ctrl+x x` |
| `/themes` | | Themes listen | `ctrl+x t` |
| `/details` | | Tool-Details ein-/ausblenden | |
| `/thinking` | | nur Anzeige der Thinking-Blöcke toggeln | |
| `/exit` | `/quit`, `/q` | beenden | `ctrl+x q` |

> **Undo/Redo brauchen Git:** verwalten Dateiänderungen intern über Git — das Projekt **muss ein
> Git-Repository sein.** `/undo` mehrfach ausführbar. `offiziell`

### 5.2 Sessions: CLI `offiziell`
```bash
opencode session list                 # -n/--max-count, --format table|json
opencode session delete <sessionID>
opencode --continue                   # -c  letzte Session fortsetzen
opencode --session <id>               # -s  bestimmte Session
opencode --fork ...                   # Session forken
```

### 5.3 Teilen (Share) `offiziell` (opencode.ai/docs/share)
- `/share` erzeugt öffentliche URL `opncd.ai/s/<id>` (in Zwischenablage). Standardmäßig **NICHT** geteilt.
- Modi in `opencode.json` via `"share"`: `"manual"` (Default), `"auto"`, `"disabled"`. Env `OPENCODE_AUTO_SHARE`.
- **Datenschutz:** Geteilte Konversationen sind für jeden mit Link öffentlich. Keine sensiblen Daten teilen.
  Für Teams `"share": "disabled"` projektweit committen.

### 5.4 Export/Import `offiziell`
```bash
opencode export [sessionID] [--sanitize]    # JSON; --sanitize redigiert sensible Daten
opencode import session.json
opencode import https://opncd.ai/s/abc123
```

---

## 6. Täglicher Workflow — Best Practices

### 6.1 Projekt initialisieren: `/init` → AGENTS.md
Nach `cd /projekt` und `opencode` zuerst `/init`. Analysiert das Projekt und erstellt/aktualisiert
**`AGENTS.md`** (Build/Lint/Test-Befehle, Architektur, Konventionen). **Ins Git committen.** `offiziell`
Details siehe `agents-md-memory.md`.

### 6.2 Plan- vs. Build-Modus `offiziell`
- **Plan (read-only):** Codebase erkunden + Änderungen planen, ohne Code anzufassen (Edits/Bash auf `ask`).
- **Build (Vollzugriff, Default):** eigentliche Implementierung.
- **Umschalten mit `Tab`** mitten in der Session.

Empfohlener Feature-Workflow: (1) Mit `Tab` in **Plan**, Feature ausführlich beschreiben (wie einem
Junior-Entwickler). (2) Auf dem Plan iterieren. (3) Mit `Tab` zu **Build** und „Go ahead." Einfache
Änderungen direkt im Build.

Subagents für spezialisierte Arbeit: `@general`, `@explore` (schnell, read-only), `@scout` (externe Docs).

### 6.3 Diffs, Undo/Redo, Kontext
- Diffs werden direkt in der TUI gerendert; Tool-Details per `/details`.
- Undo/Redo (`<leader>u`/`<leader>r`) revertieren Nachrichten *und* Dateiänderungen über Git.
- Kontext kompaktieren mit `/compact`; Auto-Kompaktierung abschaltbar via `OPENCODE_DISABLE_AUTOCOMPACT`.
- Kosten im Blick mit `opencode stats`.
- Modellreferenzen immer als `provider/model`; `opencode models` listet sie.

---

## 7. Windows-Stolperfallen (und wie man sie umgeht)

> **Goldene Regel:** Wenn möglich **WSL** oder zumindest **Git Bash** statt nativem PowerShell/CMD. `offiziell`

1. **curl-Installer scheitert in PowerShell/CMD** (`curl` = `Invoke-WebRequest`). Fix: Git Bash / WSL. `extern`
2. **npm-Wrapper läuft nicht out-of-the-box** auf nativem Windows (`/bin/sh.exe not recognized` in PowerShell,
   `cannot execute binary file` in Git Bash). Fix: gebündelte `.exe` direkt aufrufen, oder pragmatisch
   **`scoop install opencode`** / **`choco install opencode`**. `extern` (GitHub #2447)
3. **`opencode` nicht im PATH:** `C:\Users\USERNAME\.opencode\bin` zur User-`Path`-Variable hinzufügen. `extern`
4. **Git-Bash-Pfad falsch erkannt:** Pfad zur `bash.exe` explizit per `OPENCODE_GIT_BASH_PATH` setzen. `offiziell`
5. **Bash-Syntax leckt nach PowerShell** (Quoting/`=`): Pfade in `"..."` setzen; bevorzugt Git Bash/WSL als
   Bash-Tool-Shell. `extern`
6. **Desktop-App nimmt WSL-SHELL** statt `OPENCODE_GIT_BASH_PATH` (priorisiert `process.env.SHELL`). `extern`
7. **Shift+Enter sendet keinen Zeilenumbruch:** In Windows Terminal `settings.json` eine `sendInput`-Action
   mit `"[13;2u"` + Keybinding `shift+enter` ergänzen. `offiziell`
8. **Windows-Keybind-Defaults:** `input_undo` enthält `ctrl+z`; `terminal_suspend` ist `none`. `offiziell`
9. **Node.js erforderlich** für den npm-Weg (`node -v` prüfen). `extern`
10. **Desktop-App braucht WebView2-Runtime** (blankes Fenster → WebView2 installieren). `offiziell`
11. **OpenCode ohne Tastatur kopieren/einfügen:** OpenCode fängt mit dem Default `"mouse": true` die
    Maus ab. In `~/.config/opencode/tui.json` zuerst `"mouse": false` setzen; dadurch bleibt laut
    offizieller Doku die native Terminalauswahl erhalten. Dann in Windows-Terminal-`settings.json`
    `"copyOnSelect": true` und `"experimental.rightClickContextMenu": false` setzen. Unter
    `profiles.defaults` macht `"selectionBackground": "#808080"` die native Auswahl deutlich grau
    sichtbar. Mouse-Up kopiert, Rechtsklick fügt ein; `"copyFormatting": "none"` hält Klartext.
    Trade-off: OpenCodes eigene Mausfunktionen sind aus, Tastatur und native Terminal-Maus bleiben.
    `offiziell` (OpenCode TUI Options; Microsoft Learn Windows Terminal Interaction und Appearance)

---

## 8. Speicherorte, Logs & Debugging (Cheat-Sheet) `offiziell` (opencode.ai/docs/troubleshooting)

- **Daten/Sessions:** macOS/Linux `~/.local/share/opencode/`; Windows `%USERPROFILE%\.local\share\opencode`.
  Enthält `auth.json`, `log/`, `project/`.
- **Logs:** `~/.local/share/opencode/log/`. Mehr Detail: `opencode --log-level DEBUG`; `opencode --print-logs`.
- **Provider-Cache** (bei `AI_APICallError`): `rm -rf ~/.cache/opencode` und neu starten.
- **`ProviderInitError`:** Config-Verzeichnis löschen und per `/connect` neu authentifizieren.
- **Globale Config:** macOS/Linux `~/.config/opencode/opencode.jsonc`; Windows
  `%USERPROFILE%\.config\opencode\opencode.jsonc`.
- **Pfade verlässlich:** `opencode debug paths` (zeigt home, data, bin, log, cache, config, state).
- **Bugs/Community:** `github.com/anomalyco/opencode/issues`, `opencode.ai/discord`.

---

## Quellen
**Offiziell:** opencode.ai/docs (Intro/Install), /cli, /tui, /keybinds, /windows-wsl, /troubleshooting,
/share, /rules, /agents; GitHub anomalyco/opencode README; GitHub-Issue #2447 (npm-Wrapper Windows).
**Extern:** Lufti Nur „How to Install OpenCode AI on Windows"; GitHub-Issues #10871, #8396, #11330, #4379,
#16479 (Windows-Shell-Fallen); Medium „Opencode CLI on Windows Fix (EPERM…)".

> Versionshinweis: Docs-Stand Juni 2026. Bun-Installation auf Windows „in Arbeit". Mehrere
> `OPENCODE_EXPERIMENTAL_*`-Env-Variablen sind experimentell und können sich ändern.
