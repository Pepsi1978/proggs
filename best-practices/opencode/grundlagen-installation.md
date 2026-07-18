# Grundlagen & Installation — Best Practices (Stand 2026-07-18, OpenCode CLI 1.18.3)

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

#### ChatGPT-OAuth Fast/Priority korrekt behandeln

Bei ChatGPT-OAuth ist Fast kein separates Modell: Ein lokaler Fast-Alias zeigt auf das Basismodell und
setzt `options.serviceTier:"priority"`. Der offizielle Codex-Katalog nennt diesen Tier `Fast` und beschreibt
ihn als `1.5x speed, increased usage`; Sol, Terra, Luna und GPT-5.5 unterstützen ihn. `offiziell`

Das finale Responses-Feld `service_tier:"default"` darf bei OAuth **nicht** wie bei API-Key-Requests als
Beweis für Standardrouting interpretiert werden. Ein OpenAI-Contributor erklärt, Codex-Fast werde in diesem
Modus serverseitig geroutet und sei über das finale Payloadfeld nicht zuverlässig verifizierbar. Deshalb:

1. Auth-Art aus `auth.json` bestimmen.
2. Bei OAuth die konfigurierte Requestoption `priority` und den Fast-fähigen Modellkatalog prüfen.
3. Angeforderten Tier und rohes Response-Tier getrennt protokollieren.
4. Fast als `Fast (OAuth-Routing)` anzeigen, nicht als durch das Responsefeld bestätigten API-Tier.
5. Bei API-Key-Auth weiterhin den tatsächlich gemeldeten Response-Tier verwenden.

Quellen: `offiziell` [Codex #32191](https://github.com/openai/codex/issues/32191),
[Codex #30413](https://github.com/openai/codex/issues/30413),
[Codex PR #19053](https://github.com/openai/codex/pull/19053),
[OpenCode #7511](https://github.com/anomalyco/opencode/issues/7511).

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

### 4.6 Windows-Hybridmaus: Copy-on-select, Rechtsklick-Paste und TUI-Klicks gemeinsam

Für native Windows-Terminal-Sessions `"mouse": true` beibehalten. `"mouse": false` ist kein vollständiger
Clipboard-Fix: Es gibt die Maus an das Terminal zurück, entfernt aber zugleich Mausrad-Scrolling und
anklickbare Confirm-/Allow-Dialoge aus OpenCode. Die beste bestätigte Lösung für OpenCode 1.17.18 ist ein
kleiner, versionsgebundener Quellpatch mit klar getrennten Zuständigkeiten:

1. **Auswahlende statt Mouse-up:** `CliRenderEvents.SELECTION` ist das verlässliche Ende einer echten
   Auswahl. Dort `Selection.copy(...)` ausführen und den Listener beim Cleanup wieder entfernen.
2. **Rechtsklick nur im Prompt:** Über die Bildschirmgrenzen des Prompt-Inputs prüfen, ob der Klick dort
   liegt. Nur dann direkt `PromptRef.paste()` aufrufen und das Event konsumieren. Kein künstliches
   `keymap.dispatchCommand("prompt.paste")` ohne Keyboard-Event-Kontext verwenden.
3. **Windows-Textclipboard nativ lesen:** Erst den bestehenden Bild-Paste-Pfad versuchen, danach Text über
   PowerShell `Get-Clipboard -Raw` mit expliziter UTF-8-Ausgabe lesen. So bleiben Bilder erhalten und
   Unicode-Text wird korrekt eingefügt.
4. **OpenCode behält die übrige Maus:** `"mouse": true` sorgt weiter für Mausrad und TUI-Schaltflächen;
   Rechtsklick außerhalb des Prompt-Felds wird nicht pauschal umgedeutet.

Der Root Cause besteht aus vier Teilen: Windows deaktiviert OpenCodes Copy-on-select standardmäßig;
bubbled `onMouseUp` signalisiert das Auswahlende nicht zuverlässig; Command-Dispatch braucht einen
Tastatur-Event-Kontext; der bisherige Windows-Clipboard-Pfad liefert für Text in dieser Konstellation
keinen Inhalt. Nur die Kombination der vier Korrekturen liefert den vollständigen Hybridbetrieb.

**Update- und Installationsstrategie:** Der Launcher prüft höchstens täglich die stabile npm-Version. Das
Buildskript bindet den Patch an den exakten Git-Tag, validiert die stabile SemVer, führt Typechecks, fokussierte
Tests, Build und Smoke-Test aus und installiert jedes Ergebnis unveränderlich unter
`~/.local/share/opencode-mousefix/versions/<customVersion>/opencode.exe`. Erst nach allen grünen Gates wird
`current.json` atomar aktiviert. Laufende Binaries werden nie überschrieben. Bei Konflikt bleibt der aktive
Pointer unverändert; der Launcher versucht `active`, `previous`, `current.json.bak`, das Legacy-Binary und
zuletzt das offizielle `opencode` im PATH. Ein exklusiver Lock verhindert parallele Builds.

**Pflicht-Verifikation:** Typechecks der geänderten Pakete, Clipboard-Tests, Single-Binary-Build,
`--version`-Smoke-Test und ein realer TUI-Test mit allen vier Funktionen: Auswahl kopiert sofort,
Rechtsklick fügt Text in den Prompt ein, Mausrad scrollt, Permission-Schaltflächen sind anklickbar.
Zusätzlich Bild-Paste und Rechtsklick außerhalb des Prompts prüfen, weil beide denselben Eingabepfad
bzw. dieselbe globale Mausbehandlung berühren.

`lokal verifiziert` (OpenCode 1.17.18, Windows Terminal 1.24.11321.0, Patchrevision windowsfix.6,
2026-07-10). Fehlerdetails und Root Cause: `bugs/opencode/opencode-cli.md` §2 #10a.

### 4.7 Windows-Stabilitätsbuild: Full-Repaint und prozesslokale Modellvariante

Der reproduzierbare Windows-Build bündelt drei voneinander unabhängige Korrekturen in einem Binary:

1. Der bestätigte Hybridmaus-Patch aus §4.6 erhält Copy, Paste, Mausrad und TUI-Klicks gemeinsam.
2. OpenTUI 0.4.3 setzt nach einer nativen Renderablehnung `forceFullRepaintRequested = true`. Dadurch heilt
   der nächste Renderzyklus einen desynchronisierten Diff-Puffer automatisch; ein manueller Resize bleibt
   nur der Fallback für ältere Binaries.
3. Die Rich-TUI akzeptiert `--variant <name>` als prozesslokalen Startwert. Der Wert gilt nur für das mit
   `--model` gestartete Modell und wird bei einer bewussten Variantenauswahl innerhalb der TUI freigegeben.
   Dadurch können parallele Sitzungen mit verschiedenen Reasoning-Stufen laufen, ohne sich über die globale
   `~/.local/state/opencode/model.json` gegenseitig zu überschreiben.

Der Launcher übergibt `--variant` ausschließlich einem per Pointer als grün ausgewiesenen Custom-Binary.
Die automatische Hintergrundprüfung blockiert weder Launcher-UI noch OpenCode-Start. Bei jedem Upstream-
Update laufen Quellpatch, OpenTUI-Recovery-Prüfung, vier Typechecks, fokussierte Tests, Binary-Smoke-Test und
Promotion-Gate erneut. Ein inkompatibles Update wird zurückgestellt, niemals ungepatcht aktiviert.

### 4.8 Vollständige Windows-Parität auf einem zweiten Rechner

`opencode-setup/install.ps1` ist der idempotente Bootstrap. Nach Repo-Clone und Installation der Build-
Voraussetzungen kopiert er Config, globale Regeln, Agents, Skills, Sounds und das komplette Sidebar-Plugin,
installiert dessen Laufzeitabhängigkeiten, baut den aktuellen Windows-Fix und den Launcher und erzeugt die
Desktop-Verknüpfung. Damit bleiben Session-Datum/-Uhrzeit, drei Arbeitsmodi, Modellpreise/Kosten,
Theme-Auswahl und Hell/Dunkel identisch. Secrets, Provider-Auth, `OPENROUTER_API_KEY` und WireGuard werden
bewusst nicht versioniert und müssen pro Rechner separat gesetzt werden. Unter Windows immer über den
Launcher starten; `opencode` im PATH bleibt nur der offizielle Fallback.

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
11. **Für vollständige Mausbedienung den Hybridpatch verwenden:** `~/.config/opencode/tui.json` auf
    `"mouse": true` lassen. Die Windows-Terminal-Werte `"copyOnSelect": true`,
    `"experimental.rightClickContextMenu": false`, `"selectionBackground": "#808080"` und
    `"copyFormatting": "none"` können bestehen bleiben. Für OpenCode 1.17.18 aktiviert der bestätigte
    Patch Copy-on-select über das Selection-End-Event und Text-Paste per Rechtsklick nur im Prompt,
    während Mausrad und Confirm-/Allow-Klicks erhalten bleiben. Patch, reproduzierbarer Build,
    atomare Installation, Versionsprüfung und offizieller Binary-Fallback gehören zusammen; Details
    siehe §4.6. `lokal verifiziert` (2026-07-10), TUI-/Terminal-Optionen `offiziell`.

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
