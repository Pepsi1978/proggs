---
name: hook-forge
description: "Nutze diesen Skill IMMER wenn ein neuer Claude Code Hook erstellt oder ein bestehender Hook grundlegend umgeschrieben wird. Auch wenn Claude selbst einen Hook erstellt (z.B. via /hookify, /self-improve, oder manuell) MUSS dieser Skill VORHER geladen werden. Trigger: 'Hook erstellen', 'neuer Hook', 'Hook bauen', 'Hook fixen' (bei Rewrite), oder wenn Claude intern Write/Edit auf eine .ps1/.sh Hook-Datei ausfuehrt."
---

# Hook-Forge: Resiliente Hook-Erstellung nach den 3 Direktiven

Dieser Skill stellt sicher, dass JEDER Hook in dieser Programmierumgebung:
- Niemals Hook-Fehler erzeugt (exit 0 PFLICHT)
- Auf BEIDEN Plattformen funktioniert (.ps1 + .sh)
- Den 3 Hauptdirektiven folgt (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing)
- Ins Setup-Repo kopiert und registriert wird

## Pflicht-Ablauf (Checkliste)

### Phase 1: Design

1. **Event bestimmen**: Welches Hook-Event? (SessionStart, UserPromptSubmit, PreToolUse, PostToolUse, Stop, SubagentStop, SessionEnd, etc.)
2. **Typ bestimmen**: `command` (Skript ausfuehren) oder `prompt` (Text injizieren)?
   - ACHTUNG: `prompt`-Typ funktioniert NICHT bei SessionStart und SessionEnd! (kein ToolUseContext)
   - Bei SessionStart IMMER `command` verwenden.
3. **Timeout bestimmen**: Standard 60000ms. SessionStart-Hooks: max 30000ms (blockieren den Start).
4. **Name waehlen**: Beschreibender Kebab-Case-Name (z.B. `disk-guard`, `intent-anker`, `auto-format`).

### Phase 2: Template anwenden

Lies die passenden Templates:
- **PowerShell**: `references/ps1-template.md` — fuer Windows
- **Bash**: `references/sh-template.md` — fuer macOS/Linux

BEIDE Varianten MUESSEN erstellt werden. Keine Ausnahme.

Jeder Hook MUSS diese Elemente enthalten:

| Element | Warum | Direktive |
|---------|-------|-----------|
| `$ErrorActionPreference = "Stop"` (PS1) / `set -e` (SH) | Fehler frueh erkennen | #3 Resilient |
| `try { ... } catch { ... }` (PS1) / `trap` (SH) | Fehler abfangen statt propagieren | #3 Resilient |
| `exit 0` am ENDE (IMMER) | Hook darf NIEMALS non-zero returnen | #3 Resilient |
| `exit 0` in JEDEM catch/trap-Block | Auch bei Fehlern graceful beenden | #3 Resilient |
| `. "$PSScriptRoot/hook-log.ps1"` / `. "$(dirname "$0")/hook-log.sh"` | Zentrale Fehler-Protokollierung | #2 Selbstbeobachtung |
| Hook-Log bei JEDEM Fehler | Fehler werden ins Log geschrieben, nicht verschluckt | #2 Selbstbeobachtung |
| Whiteboard-Eintrag bei SCHWEREN Fehlern | Sichtbar fuer /self-improve | #1 Superintelligenz |

### Phase 3: Implementierung

1. **PowerShell-Version erstellen**: `~/.claude/hooks/[name].ps1`
2. **Bash-Version erstellen**: Die SH-Version muss FUNKTIONAL IDENTISCH sein.
   - Gleiche Logik, gleiche Checks, gleiche Log-Messages
   - Plattform-spezifische Unterschiede beruecksichtigen (Pfade, Tools, Syntax)
3. **Testen**: Beide Versionen syntax-pruefen:
   - PS1: `pwsh -NoProfile -Command "& { $null = [System.Management.Automation.Language.Parser]::ParseFile('PFAD', [ref]$null, [ref]$null) }"`
   - SH: `bash -n PFAD`

### Phase 4: Integration

1. **Setup-Repo kopieren**: Beide Versionen nach `~/proggs/claude-code-setup/hooks/` kopieren.
2. **settings.json registrieren** (wenn noetig):
   - Hook in `~/.claude/settings.json` unter dem richtigen Event eintragen
   - Format: `{"hooks": {"[Event]": [{"type": "command", "command": "pwsh -NoProfile -File ..."}]}}`
   - ACHTUNG: Auch die macOS-Version in `claude-code-setup/settings.json` eintragen!
   - Und `settings-reference.json` aktualisieren (3-Dateien-Regel)!
3. **Whiteboard-Eintrag**: Neuen Hook unter "Systemzustand" im Whiteboard erwaehnen (Hook-Zaehler aktualisieren).

### Phase 5: Verifikation (PFLICHT — kein Hook ohne diese Pruefung)

- [ ] PS1-Version hat `exit 0` am Ende?
- [ ] SH-Version hat `exit 0` am Ende?
- [ ] PS1-Version hat `try/catch` oder `$ErrorActionPreference`?
- [ ] SH-Version hat `set -e` oder `trap`?
- [ ] Kein `exit 1` in SessionStart-Hooks?
- [ ] Hook-Log wird bei Fehlern aufgerufen?
- [ ] Beide Versionen syntax-geprueft?
- [ ] Ins Setup-Repo kopiert?
- [ ] In settings.json registriert (wenn noetig)?
- [ ] 3-Dateien-Regel fuer Settings eingehalten?

## Verbotene Muster

| Muster | Warum verboten | Stattdessen |
|--------|---------------|-------------|
| `exit 1` in SessionStart | Erzeugt sichtbaren Hook-Fehler | `exit 0` + Fehler ins Whiteboard loggen |
| Kein `exit 0` am Ende | PowerShell gibt Exit-Code des letzten Befehls zurueck | Explizites `exit 0` am Skript-Ende |
| `type: "prompt"` bei SessionStart | Kein ToolUseContext verfuegbar, crasht | `type: "command"` verwenden |
| Nur .ps1 ODER nur .sh | Andere Plattform hat keinen Hook | IMMER beide erstellen |
| Fehler still verschlucken | Verstoesst gegen Direktive #2 | Hook-Log + ggf. Whiteboard-Eintrag |
| Endlosschleife / langer Timeout | Blockiert Session-Start oder Tool-Nutzung | Timeout setzen, async ausfuehren |
| Hardcoded absolute Pfade | Bricht auf anderer Plattform | `$PSScriptRoot`, `$HOME`, `$(dirname "$0")` |

## Spezialfall: Async-Hooks

Hooks die lange dauern (Reindexierung, Downloads, Builds) MUESSEN async sein:
- PS1: `Start-Process pwsh -ArgumentList "-NoProfile", "-File", $scriptPath -WindowStyle Hidden`
- SH: `nohup bash "$script" &>/dev/null &` + `disown`

## Referenz-Dateien

- `references/ps1-template.md` — Vollstaendiges PowerShell-Hook-Template
- `references/sh-template.md` — Vollstaendiges Bash-Hook-Template
