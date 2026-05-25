# Best Practices: NEUES & Horizont-Scan (Kategorie 11)

> Stand: 2026-05-25 | Claude Code v2.1.150 | Quelle: offizielle Anthropic-Dokumentation (What's New)
> Zeitraum: KW 13–20 2026 (ca. Dezember 2025 – Mai 2026)

---

## KW 20 (Mai 2026) — Neueste Features

### Agent View Dashboard (`claude agents`)

Neues Sub-Kommando für Echtzeit-Monitoring von Hintergrund-Agents:

```bash
claude agents          # Zeigt alle laufenden/beendeten Agents
claude agents --watch  # Live-Stream der Agent-Aktivität
```

- Agents erscheinen in einem TUI-Dashboard
- Status: running, completed, failed, waiting
- **Praktisch für**: Multi-Agent-Workflows bei denen man den Überblick behalten will

### `/goal` Command — Autonome Zielverfolgung

```
/goal "Implementiere komplettes Feature X mit Tests und Dokumentation"
```

- Claude formuliert eigene Sub-Tasks und arbeitet sie ab
- Verfolgt das Ziel über mehrere Tool-Calls und ggf. `/clear`-Zyklen
- **Unterschied zu normalem Prompt:** `/goal` ist persistenter, plant aktiv nach
- Für unbeaufsichtigte lange Operationen geeignet

### Fast Mode auf Opus 4.7

- **2,5x schneller** als normaler Opus 4.7
- Höhere Token-Kosten als regulärer Opus (aber schneller als warten)
- Aktivierung: `ANTHROPIC_FAST_MODE=1` oder über Model-Config
- **Wann sinnvoll:** Wenn Latenz wichtiger ist als Token-Kosten

### Hook-Verbesserungen KW 20

**`continueOnBlock`-Option (PostToolUse):**
```json
{
  "hooks": {
    "PostToolUse": [{
      "continueOnBlock": true,
      "matcher": "Edit|Write",
      "command": "pwsh -File validate.ps1"
    }]
  }
}
```
- Hook kann `exit 2` (Block) ausgeben — Claude fragt dann nach ob fortfahren
- Mit `continueOnBlock: true`: Claude fährt trotz Block automatisch fort

**`terminalSequence`-Feld:**
```json
{
  "terminalSequence": "]9;Aufgabe abgeschlossen"
}
```
- Hooks können Desktop-Notifications senden OHNE ein Terminal-Fenster zu brauchen
- Ideal für lange unbeaufsichtigte Runs

**Hooks sehen Effort-Level:** Hook-Event-Payload enthält `effort_level` — Hooks können unterschiedlich reagieren je nach `/effort`-Setting.

---

## KW 19 (April/Mai 2026)

### Plugin-Distribution via ZIP/URL

- Plugins können jetzt als `.zip`-Datei oder direkte URL installiert werden
- Kein GitHub-Repo mehr zwingend nötig
- `claude plugins install https://example.com/myplugin.zip`

### `worktree.baseRef` in Worktrees

```json
{
  "worktrees": {
    "baseRef": "main"
  }
}
```
- Worktrees werden von diesem Branch aus erstellt statt HEAD
- Wichtig für parallele Feature-Entwicklung ohne Merge-Konflikte

### Hard Deny Rules

```json
{
  "permissions": {
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force)"
    ]
  }
}
```
- Absolute Verbote die KEINE Ausnahmen kennen (im Gegensatz zu weichen Deny-Rules)
- Sicherheitsnetz für destruktive Operationen

---

## KW 18 (April 2026)

### Windows ohne Git Bash — Native PowerShell

- Claude Code läuft jetzt nativ in PowerShell ohne Git Bash
- Hooks: `.ps1`-Dateien werden direkt ausgeführt (kein Bash-Wrapper nötig)
- **Für Windows-Nutzer:** `pwsh -File hook.ps1` ist der empfohlene Weg
- Bash-Hooks (`.sh`) weiterhin unterstützt via Git Bash wenn installiert

### `claude ultrareview` für CI

```bash
# In GitHub Actions / CI-Pipeline:
claude ultrareview --pr $PR_NUMBER
```
- Startet Cloud-Multi-Agent Review als CLI-Befehl
- Gibt strukturierten Review-Report zurück (JSON oder Markdown)
- Integrierbar in CI/CD ohne manuellen Browser-Aufruf

---

## KW 17 (April 2026)

### `/ultrareview` — Öffentliche Preview

- Interaktiver Cloud-Multi-Agent Code Review
- Mehrere Claude-Instanzen reviewen gleichzeitig (Security, Performance, Style, Logic)
- Ergebnis: Konsolidierter Report mit Prioritäten
- **Aufruf:** `/ultrareview` in laufender Session oder `claude ultrareview`

### Session Recap (`/recap`)

```
/recap
```
- Fasst die bisherige Session zusammen (was wurde gemacht, was ist offen)
- Nützlich nach langer Session bevor man `/clear` macht
- Exportierbar: `/recap --export recap.md`

### Custom Themes

- Claude Code Terminal-UI: Theme-Unterstützung
- `~/.claude/theme.json` für eigene Farben
- Vorgefertigte Themes: `claude theme set dark-blue`

---

## KW 16 (März/April 2026)

### Opus 4.7 als neues Default

- Opus 4.7 ersetzt Opus 4.5 als Standard-Opus-Modell
- **Neues Feature: `xhigh` Effort-Level** (nur Opus 4.7)
  ```
  /model opus
  /effort xhigh
  ```
- Extended Thinking mit größerem Budget als `high`

### Routines — Terminierte Cloud-Agents

```
/routine create "täglich 09:00" "Prüfe offene PRs und erstelle Status-Report"
```
- Agents die nach Zeitplan laufen (Cron-ähnlich)
- Ergebnis landet in Notifications oder Webhook
- **Anwendungsfälle:** Tägliche Code-Qualitäts-Reports, Dependency-Checks, Backup-Verifikation

### Mobile Push Notifications

- Claude Code Desktop kann bei Abschluss einer langen Operation Push-Notification senden
- Konfiguration in App-Settings
- **Wichtig für:** Unbeaufsichtigte lange Runs (`/goal`, `--dangerously-skip-permissions`)

### Native CLI Binaries (kein Node.js mehr nötig)

- Claude Code als selbstständige Binary (kein globales `npm install -g` mehr)
- Download: `curl https://claude.ai/cli/install.sh | sh`
- Kleinerer Footprint, schnellerer Start

---

## KW 15 (März 2026)

### Ultraplan — Cloud-Planung mit Web-Editor

- Interaktive Web-Oberfläche für komplexe Planungsaufgaben
- Kein Terminal nötig — Planung im Browser
- Export als CLAUDE.md oder Markdown-Spec
- **URL:** `claude.ai/ultraplan`

### Monitor Tool — Hintergrund-Event-Streaming

```python
# In Hooks nutzbar:
# Monitor-Tool streamt Events aus laufenden Prozessen
```
- Neues internes Tool für Hooks: Liest Events aus Hintergrundprozessen
- Kein Polling mehr nötig für lange laufende Tools
- **Anwendungsfall:** Build-Logs in Echtzeit in Hooks verarbeiten

### `/loop` Command

```
/loop 10 "Führe Tests aus und fixe den nächsten Fehler"
```
- Wiederholt einen Prompt N-mal oder bis Abbruchbedingung
- Mit `--until-green`: Läuft bis alle Tests grün
- **Anwendungsfall:** Automatisches iteratives Debugging

### `/team-onboarding` und `/autofix-pr`

- `/team-onboarding`: Generiert Onboarding-Dokument aus Codebase (für neue Entwickler)
- `/autofix-pr`: Analysiert fehlgeschlagene PR-Checks und erstellt Fix-PR automatisch

---

## KW 14 (Februar/März 2026)

### Computer Use — CLI Research Preview

```bash
claude --computer-use "Erstelle einen Screenshot von localhost:3000 und beschreibe die UI"
```
- Claude kann Desktop-UI steuern: Klicks, Tastatur, Screenshots
- Research Preview — nicht für Produktion
- **Windows:** Hyper-V + Remote Desktop Integration
- **macOS:** Accessibility API

### 500K MCP Result Size

- MCP-Tool-Responses bis 500KB (vorher 100KB)
- Große Datei-Reads, API-Responses, DB-Dumps direkt via MCP zurückgebbar
- Kein manuelles Chunking mehr nötig

### `/powerup` Command

```
/powerup
```
- Lädt alle verfügbaren Plugins, MCPs und Skills neu
- Nützlich nach Plugin-Installation ohne Session-Neustart
- Entspricht einem "Hot-Reload" der Konfiguration

---

## KW 13 (Februar 2026)

### Auto Mode Preview (jetzt stabil in KW 16+)

- Interner Classifier auf Sonnet 4.6 entscheidet Read-only vs. Schreib-Ops
- 2x Shift+Tab zum Aktivieren
- Basis für `/goal` und autonome Workflows

### `if`-Hooks — Bedingte Hook-Ausführung

```json
{
  "hooks": {
    "PostToolUse": [{
      "if": "tool_name == 'Edit' && file.endsWith('.kt')",
      "command": "pwsh -File lint-kotlin.ps1"
    }]
  }
}
```
- Hooks laufen nur wenn Bedingung erfüllt
- Spart Hook-Overhead bei Ops die kein Lint brauchen

### Native PowerShell Tool (Windows)

- Claude Code nutzt PowerShell direkt statt Bash-Emulation
- `Bash`-Tool in Hooks heißt intern jetzt `Shell` (abwärtskompatibel)
- Pfad-Handling automatisch für Windows-Konventionen

---

## 1M Token Context Window — Neue Optionen

Ab KW 14+ verfügbar für Max/Team-Pläne:

```bash
/model opus[1m]    # Opus 4.7 + 1M Context
/model sonnet[1m]  # Sonnet 4.6 + 1M Context
```

**Wann sinnvoll:**
- Gesamte Codebase in einem Kontext (Mono-Repos)
- Sehr lange Konversationen ohne `/clear`
- Analyse von vollständigen Log-Files

**Wann NICHT:**
- Standard-Projekte (200K reicht für 95% der Fälle)
- Wenn 1M Context als Ersatz für `/clear` genutzt wird (teurer!)

---

## Env-Variablen Übersicht (Neu/Wichtig)

```bash
ANTHROPIC_FAST_MODE=1                    # Fast Mode Opus 4.7 (KW 20)
DISABLE_AUTOUPDATER=1                    # Automatische Updates deaktivieren
CLAUDE_CODE_SUBAGENT_MODEL=claude-haiku  # Subagents mit Haiku (sparsam)
ANTHROPIC_DEFAULT_OPUS_MODEL=...         # Custom Opus-Modell-ID
ENABLE_PROMPT_CACHING_1H=1              # 1h Cache-TTL für API-Keys
DISABLE_PROMPT_CACHING=1               # Cache deaktivieren (Debugging)
```

---

## Zusammenfassung: Was sofort praktisch nutzen?

| Feature | Sofort nutzbar | Aufwand |
|---------|----------------|---------|
| `claude agents` | ✅ Ja | Kein Setup |
| `/goal` für autonome Workflows | ✅ Ja | Kein Setup |
| `if`-Hooks | ✅ Ja | JSON-Edit |
| `continueOnBlock`-Option | ✅ Ja | JSON-Edit |
| `/recap` vor /clear | ✅ Ja | Kein Setup |
| `/loop --until-green` | ✅ Ja | Kein Setup |
| Routines für tägliche Checks | ✅ Ja | Setup nötig |
| Computer Use | ⚠️ Research Preview | Instabil |
| Ultraplan Web-Editor | ✅ Ja | Browser |
| 1M Context Window | ✅ Max/Team Plan | Plan nötig |
