# Hooks — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quelle: https://code.claude.com/docs/en/hooks (offiziell)
> Quelle: https://code.claude.com/docs/en/hooks-guide (offiziell)
> Quelle: https://code.claude.com/docs/en/changelog (offiziell)

---

## Grundprinzip

- **Was:** Hooks sind benutzerdefinierte Shell-Befehle (oder HTTP-Endpunkte/MCP-Tools/Prompts/Agents), die zu bestimmten Zeitpunkten im Claude-Code-Lebenszyklus automatisch ausgeführt werden.
- **Best Practice:** Hooks für deterministisches Verhalten nutzen (Lint, Guard, Log). Für Entscheidungen die Urteilsvermögen erfordern, `type: "prompt"` oder `type: "agent"` verwenden. Alles was Claude Code "immer" tun soll, gehört in einen Hook — nicht in CLAUDE.md oder Skills.
- **Quelle:** https://code.claude.com/docs/en/hooks-guide (offiziell)

---

## Hook-Events (vollständige Liste, Stand 2026-05)

### Session-Lifecycle
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `SessionStart` | Nein | `source`: startup/resume/clear/compact. `CLAUDE_ENV_FILE` für persistente Env-Vars |
| `Setup` | Nein | Nur bei `--init-only`, `--init`, `--maintenance` |
| `SessionEnd` | Nein | Matcher: clear/logout/other |

### Prompt-Verarbeitung
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `UserPromptSubmit` | Ja | Kann Prompt blockieren oder `additionalContext` injizieren |
| `UserPromptExpansion` | Ja | Feuert wenn Slash-Command oder MCP-Prompt expandiert wird |

### Tool-Ausführung
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `PreToolUse` | Ja | Wichtigstes Event für Guards. `permissionDecision`: deny/allow/ask/defer |
| `PostToolUse` | Nein | Für Linting, Logging nach erfolgreicher Tool-Ausführung |
| `PostToolUseFailure` | Nein | Automatisches Bug-Case-Logging hier |
| `PostToolBatch` | Ja | **NEU v2.1.139**: Feuert nach allen parallelen Tool-Aufrufen. Kann Agentic Loop stoppen. |
| `PermissionRequest` | Ja | Kann Permission-Regeln on-the-fly erstellen |
| `PermissionDenied` | Nein | Im Auto-Mode, wenn Permission abgelehnt |

### Stop-Events
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `Stop` | Ja | Kann Claude am Stoppen hindern. **NEU v2.1.143**: Endlosschleife bricht nach 8 Blocks ab (überschreibbar: `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP`) |
| `StopFailure` | Nein | API-Fehler beim Beenden. Matcher: rate_limit/authentication_failed |

### Subagents & Tasks
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `SubagentStart` | Nein | Passiv — ideal für Kontext-Injection (nur via JSON-Output, kein Blocker) |
| `SubagentStop` | Ja | **v2.1.149**: Neue Felder `background_tasks` und `session_crons` |
| `TaskCreated` | Ja | Task-Erstellung rückgängig machbar |
| `TaskCompleted` | Ja | Task-Markierung blockierbar |
| `TeammateIdle` | Ja | Teammate bleibt bei Exit 2 aktiv |

### Kompakt & Kontext
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `PreCompact` | Ja | Matcher: manual/auto. Felder: current_tokens, target_tokens |
| `PostCompact` | Nein | Nach Verdichtung |
| `InstructionsLoaded` | Nein | CLAUDE.md/Rules wurden geladen |

### Dateisystem & Config
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `FileChanged` | Nein | Matcher: Dateiname/Glob |
| `CwdChanged` | Nein | Arbeitsverzeichnis geändert |
| `ConfigChange` | Ja | Config-Änderung blockierbar |
| `WorktreeCreate` | Ja | Command-Hook: stdout = absoluter Worktree-Pfad |
| `WorktreeRemove` | Nein | |

### Benachrichtigungen & MCP
| Event | Blocker? | Besonderheit |
|-------|----------|-------------|
| `Notification` | Nein | Typ: permission_prompt/auth_success |
| `Elicitation` | Ja | **NEU**: MCP fordert Benutzereingabe. Hook kann accept/decline/cancel |
| `ElicitationResult` | Ja | Hook kann Antwort überschreiben |

---

## Exit-Codes

- **Was:** Exit-Codes steuern ob Claude blockiert wird oder nicht.
- **Best Practice:** IMMER `exit 0` am Ende jedes Standalone-Hooks. Bibliotheken/dot-sourced Skripte dürfen NIEMALS `exit` aufrufen (tötet den Aufrufer). Nur Blocker-Hooks nutzen `exit 2`.

| Code | Bedeutung | Wann nutzen |
|------|-----------|-------------|
| `0` | Erfolg | Standard. Stdout als JSON geparst. |
| `2` | Blocking Error | Wenn PreToolUse/Stop/etc. blockiert werden soll. Stderr wird Claude gezeigt. |
| Andere | Non-blocking Error | Stderr nur im Debug-Log |

- **Quelle:** https://code.claude.com/docs/en/hooks (offiziell)

---

## Hook-Typen

### `type: "command"` (Standard)
- **Was:** Shell-Skript oder Binärdatei, das Claude Code ausführt.
- **Best Practice:** Exec-Form mit `args: string[]` bevorzugen (kein Shell-Quoting-Problem). Shell-Form für einfache Einzeiler.

```json
{
  "type": "command",
  "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/validate.sh",
  "args": ["--strict"],
  "timeout": 60,
  "async": false
}
```

- **Quelle:** https://code.claude.com/docs/en/hooks (offiziell)
- **Stand:** v2.1.139 (Exec-Form mit `args` neu)

### `type: "http"` (HTTP-Endpunkt)
- **Was:** Hook ruft HTTP-Endpunkt auf — nützlich für externe Services.
- **Best Practice:** `allowedEnvVars` für Token-Übergabe nutzen, nie Token hardcoden.

### `type: "mcp_tool"` (MCP-Tool)
- **Was:** Hook ruft MCP-Tool direkt auf ohne Shell.
- **Best Practice:** Für Integration mit eigenen MCP-Servern statt Shell-Wrapper.
- **Stand:** NEU v2.1.139

### `type: "prompt"` (LLM-Entscheidung)
- **Was:** Claude-Modell wertet Bedingung aus.
- **ACHTUNG:** Nicht für `SessionStart`, `Setup`, `SubagentStart` — dort `type: "command"` verwenden (v2.1.142 gibt jetzt expliziten Fehler aus).
- **Best Practice:** Nur für Entscheidungen die Urteilsvermögen erfordern. Langsamer und teurer als command-Hooks.

### `type: "agent"` (Agent-Ausführung)
- **Was:** Startet Subagent für komplexe Hook-Logik.
- **Best Practice:** Nur wenn command- oder prompt-Hook nicht ausreicht.

---

## JSON-Input/Output

### Input (alle Events, Basisfelder)
```json
{
  "session_id": "abc123",
  "transcript_path": "/path/to/transcript.jsonl",
  "cwd": "/current/dir",
  "permission_mode": "bypassPermissions",
  "hook_event_name": "PreToolUse",
  "effort": { "level": "high" },
  "agent_id": "optional",
  "agent_type": "optional"
}
```
- **NEU v2.1.133:** `effort.level` und `$CLAUDE_EFFORT` Umgebungsvariable verfügbar
- **NEU v2.1.132:** `$CLAUDE_CODE_SESSION_ID` in Bash-Tool-Umgebung

### Output (universell)
```json
{
  "continue": true,
  "suppressOutput": false,
  "systemMessage": "Warnung für Claude",
  "terminalSequence": "\033]9;Notification\007",
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "additionalContext": "Kontext für Claude",
    "permissionDecision": "allow|deny|ask|defer"
  }
}
```
- **NEU v2.1.141:** `terminalSequence` für Desktop-Notifications ohne controlling terminal
- **KRITISCH für SubagentStart:** Schema muss `{hookSpecificOutput: {hookEventName: "SubagentStart", additionalContext: "..."}}` sein — flaches `{additionalContext: ...}` wird STILL ignoriert (verifiziert 2026-05-24 im Projekt)

---

## Matcher-Patterns

- **Was:** Matcher filtern für welche Tool-Namen oder Event-Quellen der Hook feuert.
- **Best Practice:** Spezifischer Matcher vor `"*"` — reduziert unnötige Ausführungen.

```json
{ "matcher": "Bash(git push *)" }   // Nur git push Befehle
{ "matcher": "Edit|Write" }          // Edit ODER Write
{ "matcher": "mcp__memory__.*" }     // Alle Tools vom memory-MCP-Server
{ "matcher": "startup" }             // Nur bei frischem Session-Start (nicht resume)
{ "matcher": "Edit(*.ts)" }          // Nur TypeScript-Dateien (NEU: if-Feld besser)
```

### `if`-Bedingung (Permission-Rule-Syntax)
```json
{
  "type": "command",
  "command": "./check.sh",
  "if": "Bash(rm *)"
}
```
- **Best Practice:** `if` für feinkörnige Filterung innerhalb eines Matchers. Ergänzt, ersetzt nicht den `matcher`.

---

## Neue Features seit v2.1.130

### asyncRewake (v2.1.139)
- **Was:** Async-Hook kann Claude nach Abschluss mit `exit 2` wieder aufwecken.
- **Best Practice:** Für lang laufende Hintergrundaufgaben die Claude danach Bescheid geben sollen.

```json
{ "type": "command", "command": "./long-task.sh", "asyncRewake": true }
```

### continueOnBlock auf PostToolUse (v2.1.139)
- **Was:** Wenn `continueOnBlock: true`, wird Ablehnungsgrund an Claude zurückgegeben und der Turn läuft weiter statt abzubrechen.
- **Best Practice:** Für PostToolUse-Hooks die Claude informieren aber nicht stoppen sollen.

### PostToolBatch (v2.1.139)
- **Was:** Feuert nach allen parallelen Tool-Aufrufen. Einzige Möglichkeit nach paralleler Tool-Batch zu intervenieren.
- **Best Practice:** Für Validierung nach mehreren gleichzeitigen Datei-Edits.

### Stop-Hook Endlosschutz (v2.1.143)
- **Was:** Nach 8 aufeinanderfolgenden Blocks bricht die Schleife mit Warning ab.
- **Best Practice:** `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP=N` setzen wenn mehr Runden nötig.

### background_tasks / session_crons in Stop-Events (v2.1.145/v2.1.149)
- **Was:** Stop und SubagentStop erhalten jetzt Felder `background_tasks` und `session_crons`.
- **Best Practice:** Prüfen ob noch Hintergrundaufgaben laufen bevor Hook reagiert.

---

## Konfiguration & Speicherort

```json
// ~/.claude/settings.json (personal, alle Projekte)
// .claude/settings.json (projekt-spezifisch, committbar)
// .claude/settings.local.json (lokal, gitignored)
// Plugin: <plugin>/hooks.json
// Skill/Agent Frontmatter: hooks-Block

{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/guard.sh",
            "timeout": 30,
            "if": "Bash(rm -rf *)"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "npm run lint --silent",
            "async": true
          }
        ]
      }
    ]
  },
  "disableAllHooks": false
}
```

- **Quelle:** https://code.claude.com/docs/en/hooks (offiziell)

---

## Pfad-Platzhalter

| Variable | Bedeutung |
|----------|-----------|
| `${CLAUDE_PROJECT_DIR}` | Projektverzeichnis |
| `${CLAUDE_PLUGIN_ROOT}` | Plugin-Root |
| `${CLAUDE_PLUGIN_DATA}` | Plugin-Datenverzeichnis |

---

## Input-Validation Guard (KRITISCH für SubagentStop/PostToolUse)

- **Was:** SubagentStop und ähnliche Events feuern auch bei normalen Stops ohne echten Subagent. Ohne Guard läuft der Hook unnötig 50+ Mal pro Session.
- **Best Practice (PowerShell):**

```powershell
try {
    $stdin = [Console]::In.ReadToEnd()
    if (-not $stdin -or $stdin.Trim() -eq "") { exit 0 }
    $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
    if (-not $parsed.agent_id -or [string]::IsNullOrWhiteSpace($parsed.agent_id)) { exit 0 }
} catch { exit 0 }
```

- **Ausnahme:** Passive Context-Injection-Hooks (nur JSON-Output, keine Side-Effects) brauchen KEINEN Guard — der Guard würde legitime Injection verhindern.
- **Quelle:** Eigenentwicklung + hook-input-validation.md (Projekt-Erfahrung 2026-04-15)

---

## Debugging

```bash
# Alle konfigurierten Hooks anzeigen:
/hooks

# Einzelnen Hook testen:
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"rm test"}}' | bash .claude/hooks/guard.sh

# Alle Hooks deaktivieren (temporär):
# settings.json: "disableAllHooks": true

# /doctor zeigt Konfigurationsfehler (z.B. fehlende command-Felder):
/doctor
```

---

## Häufige Fehler

| Fehler | Ursache | Fix |
|--------|---------|-----|
| Hook feuert nicht | `type: "prompt"` bei SessionStart | `type: "command"` verwenden |
| Endlosschleife bei Stop | Stop-Hook blockt immer | `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP` setzen oder Bedingung prüfen |
| SubagentStart-Kontext kommt nicht an | Flaches `{additionalContext: ...}` statt nested `{hookSpecificOutput: {hookEventName: ..., additionalContext: ...}}` | Nested Schema verwenden + `ConvertTo-Json -Depth 5` |
| PowerShell `if`-Bedingungen | `PowerShell(git push*)` funktionierte nicht | v2.1.147 gefixt |
| Symlink-Settings lösen `ConfigChange` aus | Spurious events | v2.1.140 gefixt |
| Hook schreibt ins Terminal → corrupted Prompt | Hooks hatten Terminal-Zugang | v2.1.141 gefixt: Hooks laufen ohne terminal access |
| Bibliotheks-Skript mit `exit` | Killt Aufrufer | NIEMALS `exit` in dot-sourced Bibliotheken |

---

## Changelog-Highlights (Hooks)

| Version | Datum | Änderung |
|---------|-------|---------|
| v2.1.149 | 2026-05-22 | Stop + SubagentStop: neue Felder `background_tasks`, `session_crons` |
| v2.1.147 | 2026-05-21 | PowerShell `if`-Bedingungen gefixt (z.B. `PowerShell(git push*)`) |
| v2.1.143 | 2026-05-15 | Stop-Hook Endlosschutz: bricht nach 8 Blocks ab; `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP` |
| v2.1.142 | 2026-05-14 | Prompt/Agent-Hooks für SessionStart/Setup/SubagentStart zeigen jetzt Fehler |
| v2.1.141 | 2026-05-13 | `terminalSequence` im JSON-Output; Hooks ohne terminal access |
| v2.1.140 | 2026-05-12 | Symlink-Settings-Regression gefixt (ConfigChange) |
| v2.1.139 | 2026-05-11 | Exec-Form mit `args[]`; `continueOnBlock`; `asyncRewake`; MCP-Tool-Hooks; `PostToolBatch` |
| v2.1.136 | 2026-05-08 | `CLAUDE_ENV_FILE` nach /resume gefixt |
| v2.1.133 | 2026-05-07 | `effort.level` im Input + `$CLAUDE_EFFORT` Env-Var |
| v2.1.132 | 2026-05-06 | `$CLAUDE_CODE_SESSION_ID` in Bash-Subprozessen |
