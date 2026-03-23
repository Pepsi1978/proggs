# Claude Code Setup — Plattformuebergreifende Programmierumgebung

Zentrale Konfiguration fuer Claude Code auf macOS und Windows.
Dieses Verzeichnis ist die **Single Source of Truth** fuer die gesamte Umgebung.

## Quick Start (neuer Rechner)

### Windows (PowerShell als Administrator)
```powershell
cd ~/proggs/claude-code-setup
.\setup-windows.ps1
```

### macOS (Terminal)
```bash
cd ~/proggs/claude-code-setup
chmod +x setup-macos.sh
./setup-macos.sh
```

### Nach dem Setup: Settings manuell kopieren
Die `settings.json` liegt ausserhalb des Repos bei `~/.claude/settings.json`.
Sie wird NICHT automatisch deployed (Claude Code erwartet sie an diesem festen Ort).

**Windows:**
```powershell
Copy-Item ~/proggs/claude-code-setup/settings-reference.json ~/.claude/settings.json
```

**macOS:**
```bash
cp ~/proggs/claude-code-setup/settings.json ~/.claude/settings.json
```

## Verzeichnisstruktur

| Verzeichnis | Inhalt | Dateien |
|-------------|--------|---------|
| `rules/` | Regeln und Direktiven (19 Dateien) | Superintelligenz, Selbstbeobachtung, Resilient Bugfixing, Sprach-Regeln, Intent-Tracking, etc. |
| `agents/` | Custom Opus/Sonnet Agenten (15 Stueck) | architect, batch-reviewer, challenger, code-reviewer, coder, debugger, env-checker, evolution-analyst, intelligence-researcher, mar-reviewer, optimizer, quality-gate, researcher, tester, ui-polisher |
| `commands/` | Slash-Commands | `/self-improve` (v5.19), `/tool-check`, `/claudeception` |
| `skills/` | Custom Skills (6 Stueck) | auto-verify-iterate, cross-platform, tampermonkey-version, undo-changes, android-clean-architecture, android-ninja |
| `hooks/` | Hook-Skripte (.ps1 + .sh Paare) | 22 Hooks fuer 17 Event-Typen |
| `bridges/` | CLI-Bruecken (read-only Sync) | codex-delta-bridge, gemini-delta-bridge |
| `state/` | Delta-State-Tracking | codex-delta-state.json, gemini-delta-state.json |
| `scripts/` | Hilfs-Skripte | self-improve-sync, semantic-search-healthcheck |
| `settings-reference.json` | 1:1 Kopie der Windows settings.json | Referenz fuer Cross-Platform-Sync |
| `settings.json` | macOS-Version der settings.json | Gleiche Plugins/Permissions, andere Pfade |
| `settings.local.json` | Vorlage fuer lokale Overrides | bypassPermissions |
| `environment-fixes.md` | Fehler-Fix-Datenbank | Fuer Cross-Tool-Lernen (Claude Code, Codex CLI, Gemini CLI) |

## Die 3 Direktiven (Prioritaets-Hierarchie)

| Rang | Direktive | Datei | Bedeutung |
|------|-----------|-------|-----------|
| **#1** | Superintelligenz | `rules/superintelligence.md` | Exponentielle Intelligenzsteigerung, Compound Effect, 8 Dimensionen |
| **#2** | Selbstbeobachtung | `rules/self-observation.md` | Sich selbst bei der Arbeit beobachten, Fehler/Umwege/Muster erkennen |
| **#3** | Resilient Bugfixing | `rules/resilient-bugfixing.md` | Jeder Fehler nur einmal, 5-Schritte-Pflicht, Fix-Induced-Failure-Pruefung |

## Konfiguration (aktueller Stand 2026-03-23)

| Eigenschaft | Wert |
|-------------|------|
| Plugins | 91 (Windows) / 92 (macOS) — 88 aktiv |
| Marketplaces | 12 extra |
| Hook-Events | 17 |
| Permissions | 105 (inkl. 43 GitHub MCP) |
| Agenten | 15 custom |
| Effort Level | high (IMMER) |
| Permission Mode | bypassPermissions (IMMER) |
| Subagent Model | sonnet |
| Agent Teams | aktiviert |
| Auto-Updates | latest channel |

## Cross-Tool-Bruecken

Claude Code hat Bruecken zu Codex CLI und Gemini CLI. Diese funktionieren read-only —
jedes Tool liest bei den anderen, schreibt aber NUR in seinen eigenen Workspace.

| Bruecke | Trigger | Quell-Ordner |
|---------|---------|-------------|
| Codex-Delta | "starte die Bruecke zu Codex" | `codex-setup/**` (read-only) |
| Gemini-Delta | "starte die Bruecke zu Gemini" | `Gemini-Setup/**` (read-only) |

Die Bruecken praesentieren Aenderungen als nummerierte Vorschlagsliste (A1, B1, C1, D1, E1).
Der Benutzer entscheidet welche Punkte uebernommen werden. NIEMALS autonom uebernehmen.

## Fehler-Fix-Datenbanken (Cross-Tool-Lernen)

Alle drei CLI-Umgebungen dokumentieren ihre Umgebungs-Fixes in eigenen Datenbanken:

| Tool | Pfad im Repo | Format |
|------|-------------|--------|
| Claude Code | `claude-code-setup/environment-fixes.md` | Markdown |
| Codex CLI | `codex-setup/state/environment-fixes.json` | JSON |
| Gemini CLI | `Gemini-Setup/agent-memory/shared/MEMORY.md` | Markdown |

## Settings-Synchronisation (3-Dateien-Regel)

`~/.claude/settings.json` liegt AUSSERHALB des Repos. Bei Aenderungen:

1. `settings-reference.json` — 1:1 Kopie der Windows-Settings (im Repo)
2. `settings.json` — macOS-Version mit gleichen Features, anderen Pfaden (im Repo)
3. `settings.local.json` — Vorlage fuer lokale Overrides (im Repo)

Hooks verwenden plattform-spezifische Kommandos:
- macOS: `bash ~/.claude/hooks/*.sh`
- Windows: `pwsh -NoProfile -ExecutionPolicy Bypass -File "$USERPROFILE/.claude/hooks/*.ps1"`

## Bekannte Plattform-Fallen (aus environment-fixes.md)

- **Python Encoding**: Windows = cp1252, IMMER `encoding='utf-8'` angeben
- **grep -P**: Crasht auf Windows Git Bash, stattdessen awk/sed verwenden
- **Atomares Schreiben**: Nie `open('w')` fuer kritische Dateien, immer temp+rename
- **Temp-Pfade**: macOS = `/tmp/`, Windows = `$env:TEMP/`
- **Bun-Pfad**: macOS = `/opt/homebrew/bin/bun`, Windows = `$USERPROFILE/.bun/bin/bun.exe`
