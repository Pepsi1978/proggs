# Hooks — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quellen: Offizielle Claude Code Dokumentation (code.claude.com/docs) + Changelog.
> Alle Einträge ohne "extern"-Label sind offiziell bestätigt.

---

## Überblick: Hook-Typen

- **Was:** Claude Code unterstützt 5 Handler-Typen: `command` (Shell-Skript), `http` (HTTP POST), `mcp_tool` (MCP-Server-Tool), `prompt` (Claude-Modell bewertet), `agent` (experimenteller Subagent mit Tools).
- **Best Practice:** `command`-Hooks für deterministische Regeln (Linting, Guards). `prompt`-Hooks nur für Entscheidungen die Urteilsvermögen brauchen — sie sind langsamer und kosten Tokens. `agent`-Hooks sind experimentell und können sich ändern.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## Alle Hook-Events (31 Events, Stand v2.1.x)

- **Was:** Claude Code feuert 31 verschiedene Events über den Session-Lifecycle:
  - **Session:** `SessionStart`, `Setup`, `SessionEnd`
  - **Turn:** `UserPromptSubmit`, `UserPromptExpansion`, `Stop`, `StopFailure`
  - **Tool-Loop:** `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `PostToolBatch`, `PermissionRequest`, `PermissionDenied`
  - **Subagents:** `SubagentStart`, `SubagentStop`, `TeammateIdle`
  - **Tasks:** `TaskCreated`, `TaskCompleted`
  - **Kontext:** `InstructionsLoaded`, `ConfigChange`, `CwdChanged`, `FileChanged`, `PreCompact`, `PostCompact`
  - **Worktrees:** `WorktreeCreate`, `WorktreeRemove`
  - **MCP:** `Elicitation`, `ElicitationResult`
  - **Sonstiges:** `Notification`
- **Best Practice:** Hooks ohne `agent_id`-Prüfung bei `SubagentStop` feuern bei JEDEM Stop-Event, auch ohne echten Subagent — daher immer einen Input-Guard einbauen (siehe Abschnitt "Input-Guard").
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## Exit-Codes — Das wichtigste Grundwissen

- **Was:** Command-Hooks kommunizieren über Exit-Codes:

  | Exit-Code | Bedeutung | Verhalten |
  |-----------|-----------|-----------|
  | **0** | Erfolg | stdout als JSON parsen, Aktion fortsetzen |
  | **1** | Nicht-blockierender Fehler | Erste stderr-Zeile im Transcript zeigen, Ausführung fortsetzen |
  | **2** | Blockierender Fehler | Aktion blockieren, stderr an Claude zeigen |
  | **Andere** | Nicht-blockierender Fehler | Nur ins Debug-Log, Ausführung fortsetzen |

- **Best Practice:** NIEMALS `exit 1` für Policy-Enforcement verwenden — nur `exit 2` blockiert tatsächlich. `exit 1` ist der häufigste Hook-Bug. Hooks die blockieren sollen MÜSSEN `exit 2` zurückgeben. Wichtige Ausnahme: `WorktreeCreate` — hier blockiert JEDER non-zero Exit-Code die Worktree-Erstellung.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## JSON-Input/Output Schema

- **Was:** Alle Events liefern ein gemeinsames Basis-Input-Schema:
  ```json
  {
    "session_id": "abc123",
    "transcript_path": "/path/to/transcript.jsonl",
    "cwd": "/current/working/directory",
    "permission_mode": "default|plan|acceptEdits|auto|dontAsk|bypassPermissions",
    "hook_event_name": "PreToolUse",
    "effort": { "level": "low|medium|high|xhigh|max" }
  }
  ```
  Subagent-Events haben zusätzlich `agent_id` und `agent_type`.

  Universeller JSON-Output:
  ```json
  {
    "continue": true,
    "stopReason": "Grund",
    "suppressOutput": false,
    "systemMessage": "Meldung für Benutzer",
    "terminalSequence": "\033]777;notify;Titel;Body\007",
    "hookSpecificOutput": {
      "hookEventName": "EventName",
      "additionalContext": "Kontext für Claude"
    }
  }
  ```
- **Best Practice:** `hookSpecificOutput` MUSS immer `hookEventName` enthalten — ohne dieses Feld wird der Output still ignoriert (verifizierter Bug, 2026-05-24). Für Subagent-Kontext-Injection das verschachtelte Schema verwenden:
  ```json
  { "hookSpecificOutput": { "hookEventName": "SubagentStart", "additionalContext": "..." } }
  ```
  Flaches `{ "additionalContext": "..." }` ohne `hookSpecificOutput` wird von Claude Code still ignoriert.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## NEU (v2.1.141, 13. Mai 2026): `terminalSequence`

- **Was:** Hooks können Terminal-Escape-Sequenzen zurückgeben, die Claude Code über seinen eigenen Terminal-Write-Pfad ausgibt. Ermöglicht Desktop-Notifications, Window-Titel und Bells ohne direkten Terminal-Zugriff — funktioniert in tmux, GNU screen und Windows.
  ```json
  { "terminalSequence": "\033]777;notify;Claude Code;Build fertig\007" }
  ```
  **Erlaubte Sequenzen:** OSC 0/1/2 (Window/Icon-Titel), OSC 9 (iTerm2/ConEmu/Windows Terminal/WezTerm Notifications), OSC 99 (Kitty), OSC 777 (urxvt/Ghostty/Warp), BEL.
  **Abgelehnt (Sicherheit):** CSI-Sequenzen, OSC 8 (Hyperlinks), OSC 52 (Clipboard), OSC 1337.
- **Best Practice:** Für Cross-Platform-Notifications OSC 9 verwenden — unterstützt Windows Terminal, iTerm2, WezTerm. NICHT für OSC 8 versuchen — aus Sicherheitsgründen geblockt. Erfordert v2.1.141+.
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.141)
- **Stand:** 2026-05-13

---

## NEU (v2.1.139, 11. Mai 2026): `args`-Feld (Exec Form)

- **Was:** Command-Hooks können `args: string[]` statt Shell-Tokenisierung verwenden:
  ```json
  {
    "type": "command",
    "command": "python3",
    "args": [
      "${CLAUDE_PLUGIN_ROOT}/scripts/validate.py",
      "--max-size=1000",
      "${tool_input.file_path}"
    ]
  }
  ```
  Jedes `args`-Element ist exakt ein Argument — keine Shell-Interpretierung.
- **Best Practice:** Exec-Form immer bevorzugen wenn Pfade mit Leerzeichen oder Sonderzeichen (`$`, Backticks, Anführungszeichen) vorkommen. Für Windows besonders empfohlen — vermeidet PowerShell-Escaping-Probleme. Erst ab v2.1.139 verfügbar (auch v2.1.113 laut Changelog — möglicherweise zweistufiger Rollout).
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.139)
- **Stand:** 2026-05-11

---

## NEU (v2.1.139, 11. Mai 2026): `continueOnBlock` für PostToolUse

- **Was:** Neue Config-Option `continueOnBlock: true` für `PostToolUse`-Hooks. Wenn ein PostToolUse-Hook `exit 2` zurückgibt (blockiert), wird der Ablehnungsgrund an Claude zurückgefüttert und der Turn fortgesetzt — statt abzubrechen.
  ```json
  {
    "matcher": "Write",
    "hooks": [{ "type": "command", "command": "./lint.sh" }],
    "continueOnBlock": true
  }
  ```
- **Best Practice:** Für Code-Review- oder Linting-Hooks verwenden, die Claude korrigieren sollen statt den Turn zu beenden. Standard (`continueOnBlock: false`): ein blockierender PostToolUse bricht den aktuellen Turn ab. Mit `continueOnBlock: true` kann Claude den Fehler lesen und einen neuen Versuch starten.
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.139)
- **Stand:** 2026-05-11

---

## `asyncRewake` — Hintergrund-Hooks die Claude aufwecken

- **Was:** Hooks mit `asyncRewake: true` laufen im Hintergrund ohne zu blockieren. Wenn sie mit Exit-Code 2 enden, wird Claude mit dem stderr als System-Reminder aufgeweckt.
  ```json
  {
    "type": "command",
    "command": "./long-running-check.sh",
    "asyncRewake": true
  }
  ```
  `asyncRewake: true` impliziert automatisch `async: true`.
- **Best Practice:** Für lang laufende Validierungen (CI-Status, Test-Suites) die nicht blockieren sollen, aber bei Fehler Claude informieren sollen. Normaler `async: true` ohne `asyncRewake` wird bei Fehler still ignoriert — `asyncRewake` macht Hintergrund-Fehler für Claude sichtbar.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## MCP-Tool-Hooks (`type: "mcp_tool"`)

- **Was:** Hooks können direkt MCP-Server-Tools aufrufen ohne Shell-Skript-Wrapper:
  ```json
  {
    "type": "mcp_tool",
    "server": "my_server",
    "tool": "security_scan",
    "input": {
      "file_path": "${tool_input.file_path}",
      "scan_type": "deep"
    }
  }
  ```
  Matcher-Pattern für MCP-Tools:
  - `mcp__server__tool` — spezifisches Tool
  - `mcp__server__.*` — alle Tools eines Servers
  - `mcp__.*__write.*` — alle Write-Tools aller Server
- **Best Practice:** MCP-Tool-Hooks sind die sauberste Integration wenn ein MCP-Server bereits läuft. Input-Substitution mit `${tool_input.field}` direkt in der Konfiguration nutzen — kein Wrapper-Skript nötig.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## `PreToolUse` — Permission-Kontrolle mit 4 Entscheidungen

- **Was:** `PreToolUse` unterstützt granulare Permission-Entscheidungen:
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "PreToolUse",
      "permissionDecision": "allow|deny|ask|defer",
      "permissionDecisionReason": "Warum",
      "modifiedInput": { "command": "sicherere Version" },
      "additionalContext": "Kontext für Claude"
    }
  }
  ```
  - `allow` — sofort genehmigen
  - `deny` — blockieren (Claude sieht den Grund)
  - `ask` — Benutzer fragen (überschreibt Auto-Mode)
  - `defer` — normalen Permission-Flow laufen lassen
- **Best Practice:** Immer `permissionDecisionReason` setzen — Claude nutzt diesen Text um zu verstehen warum eine Aktion blockiert wurde und vermeidet wiederholte Versuche. Beim Modifizieren des Inputs (`modifiedInput`) MUSS `permissionDecision: "allow"` oder `"ask"` mitgegeben werden — sonst wird die Modifikation ignoriert.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## `PostToolUse` — Tool-Output ersetzen (v2.1.121, April 2026)

- **Was:** PostToolUse-Hooks können den Tool-Output für ALLE Tools ersetzen (seit v2.1.121, vorher nur MCP):
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "PostToolUse",
      "updatedToolOutput": "Ersetzter Output",
      "additionalContext": "Zusätzlicher Kontext"
    }
  }
  ```
  Seit v2.1.119/v2.1.121 enthält der Input auch `duration_ms` (Ausführungszeit des Tools in Millisekunden).
- **Best Practice:** `updatedToolOutput` für Output-Sanitierung nutzen (Secrets aus Logs entfernen, sensible Daten maskieren). `additionalContext` für Anreicherung ohne Ersetzung. Unterschied: `additionalContext` wird zu Claude's Kontext addiert, `updatedToolOutput` ersetzt was Claude sieht.
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.121)
- **Stand:** 2026-04-28

---

## `InstructionsLoaded` — CLAUDE.md Audit

- **Was:** Feuert wenn CLAUDE.md oder `.claude/rules/*.md` geladen wird:
  ```json
  {
    "file_path": "/project/CLAUDE.md",
    "memory_type": "Project",
    "load_reason": "session_start|nested_traversal|path_glob_match|include|compact",
    "globs": ["src/**/*.ts"],
    "trigger_file_path": "/project/src/auth.ts",
    "parent_file_path": "/project/CLAUDE.md"
  }
  ```
- **Best Practice:** Für Compliance-Logging und Observability. Kann erkennen welche Rules bei welchem Trigger geladen werden — nützlich um zu debuggen warum bestimmte Regeln aktiv sind.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## `PreCompact` — Kompaktierung kontrollieren

- **Was:** Feuert bevor der Kontext komprimiert wird. Matcher-Werte: `"manual"` oder `"auto"`. Exit-Code 2 blockiert die Kompaktierung.
  ```json
  {
    "hooks": {
      "PreCompact": [{
        "matcher": "manual",
        "hooks": [{ "type": "command", "command": "backup-transcript.sh" }]
      }]
    }
  }
  ```
- **Best Practice:** Transcript-Backups VOR Kompaktierung anlegen. `"manual"`-Matcher für interaktive Sessions, `"auto"`-Matcher für CI-Pipelines. Mit `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=100` feuert `auto` erst bei 100% Kontext.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## Input-Guard — Phantom-Events verhindern (KRITISCH)

- **Was:** SubagentStop/SubagentStart-Hooks feuern bei JEDEM Stop-Event, auch ohne echten Subagent. Das `agent_id`-Feld ist nur bei echten Subagent-Events vorhanden.
- **Best Practice:**

  PowerShell-Guard (Pflicht für SubagentStop/SubagentStart mit Side-Effects):
  ```powershell
  $stdin = [Console]::In.ReadToEnd()
  try {
      $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
      if (-not $parsed.agent_id -or [string]::IsNullOrWhiteSpace($parsed.agent_id)) {
          exit 0
      }
  } catch { exit 0 }
  ```

  Bash-Äquivalent:
  ```bash
  stdin_input=$(cat)
  agent_id=$(echo "$stdin_input" | python3 -c "import sys,json; print(json.load(sys.stdin).get('agent_id','') or '')" 2>/dev/null)
  if [ -z "$agent_id" ]; then exit 0; fi
  ```

  **Ausnahme:** Hooks die nur passiv JSON-Kontext ausgeben (kein Write, keine Side-Effects) brauchen keinen Guard — ein Guard würde legitime Kontext-Injection verhindern.
- **Quelle:** Lokaler Vorfall 2026-04-15/18 — `memory-watchdog` feuerte 50+ mal pro Session ohne echten Subagent. Auch in `~/.claude/rules/hook-input-validation.md` dokumentiert.
- **Stand:** 2026-04-20

---

## Stop-Hook Endlos-Loop-Schutz (v2.1.143, 15. Mai 2026)

- **Was:** Ab v2.1.143 endet ein Turn mit Warnung wenn Stop-Hooks mehr als 8 Mal hintereinander blockieren. Überschreibbar via `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP=N`.
- **Best Practice:** Stop-Hooks sollten idempotent sein — nach Claude's Reaktion auf einen Block sollte die Blockier-Bedingung sich verändert haben. Endlos-Loops entstehen wenn die Bedingung statisch bleibt (z.B. eine Datei fehlt und Claude sie nicht anlegen kann).
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.143)
- **Stand:** 2026-05-15

---

## `hook if`-Bedingungen — PowerShell-Bug gefixt (v2.1.147, 21. Mai 2026)

- **Was:** Bis v2.1.147 funktionierte `if: "PowerShell(git push*)"` nicht korrekt — nur `if: "PowerShell(*)"` matchte zuverlässig.
- **Best Practice:** Ab v2.1.147 funktionieren alle `if`-Bedingungen korrekt. Bei älteren Versionen (<2.1.147): Wildcard-Pattern als Workaround verwenden und die spezifische Bedingung im Skript selbst prüfen. Update auf aktuelle Version wird dringend empfohlen.
- **Quelle:** [https://code.claude.com/docs/en/changelog](https://code.claude.com/docs/en/changelog) (offiziell, v2.1.147)
- **Stand:** 2026-05-21

---

## `CLAUDE_ENV_FILE` — Environment-Persistenz

- **Was:** `SessionStart`, `Setup`, `CwdChanged`, `FileChanged`-Hooks können Umgebungsvariablen dauerhaft für die gesamte Session setzen:
  ```bash
  if [ -n "$CLAUDE_ENV_FILE" ]; then
    echo 'export NODE_ENV=production' >> "$CLAUDE_ENV_FILE"
    echo 'export DEBUG=true' >> "$CLAUDE_ENV_FILE"
  fi
  ```
- **Best Practice:** Für Projekt-spezifische Umgebungsvariablen die bei jedem Bash-Befehl der Session verfügbar sein sollen. Alternative zu hardcodierten env-Einträgen in `settings.json`. Hinweis: MCP stdio-Server erhalten seit v2.1.139 ebenfalls `CLAUDE_PROJECT_DIR` in ihrer Umgebung.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## Sicherheits-Best-Practices

- **Was:** Offizielle Sicherheitsregeln für Hook-Skripte.
- **Best Practice:**
  1. **Shell-Variablen immer quoten:** `"$FILE_PATH"` statt `$FILE_PATH` (verhindert Path-Traversal)
  2. **Absolute Pfade verwenden:** `${CLAUDE_PROJECT_DIR}` für Projekt-Pfade, nie relative Pfade
  3. **Sensitive Dateien überspringen:** `.env`, `.git/`, private Keys aus Hook-Verarbeitung ausschließen
  4. **Input validieren:** `tool_input`-Felder prüfen bevor sie als Shell-Argumente verwendet werden
  5. **Exit 0 am Ende:** Standalone Non-Blocking-Hooks MÜSSEN `exit 0` am Ende haben
  6. **Dot-Source-Bibliotheken niemals `exit` aufrufen lassen:** In geladenen Hilfsskripten beendet `exit` den Aufrufer
  7. **Exec-Form (`args`) für externe Eingaben:** Verhindert Shell-Injection durch Sonderzeichen
- **Quelle:** [https://code.claude.com/docs/en/hooks-guide](https://code.claude.com/docs/en/hooks-guide) (offiziell)
- **Stand:** 2026-05-25

---

## Hook-Kategorien: Dot-Source vs. Blocker vs. Standalone

- **Was:** Drei Hook-Kategorien mit unterschiedlichen Exit-Code-Regeln (projektspezifisch):

  | Kategorie | Erkennung | Exit-Code-Regel |
  |-----------|-----------|-----------------|
  | **Dot-Source-Bibliotheken** | Andere Hooks laden sie mit `. "$PSScriptRoot/..."` | NIEMALS `exit` verwenden — beendet den Aufrufer |
  | **Blocker/Guard-Hooks** | Enthalten `exit 2` für Fehlerfälle | `exit 2` bei Verstoß, `exit 0` als Default am Ende |
  | **Standalone Non-Blocking** | Direkt aufgerufen, keine Blockierung | MUSS `exit 0` am Ende haben |

- **Best Practice:** VOR jedem Hook-Edit prüfen: wird die Datei per Dot-Source von anderen geladen? (`grep -rn 'dateiname.ps1' ~/.claude/hooks/`). Wenn ja: kein `exit` verwenden. Bekannte Bibliotheken in diesem Projekt: `hook-log.ps1`, `whiteboard-insert.ps1`.
- **Quelle:** Lokal dokumentiert in `~/.claude/rules/platform-and-paths.md` (Vorfall 2026-04-04: `exit 0` blind in 5 Bibliotheken eingefügt — 15 Hooks still abgeschaltet)
- **Stand:** 2026-04-04

---

## Hooks via Agent-SDK (TypeScript/Python)

- **Was:** Hooks können programmatisch über den Agent-SDK registriert werden als Callback-Funktionen statt Shell-Skripte.
  ```typescript
  for await (const message of query({
    prompt: "...",
    options: {
      hooks: {
        PreToolUse: [{ matcher: "Write|Edit", hooks: [myCallback] }]
      }
    }
  })) { ... }
  ```
  TypeScript unterstützt alle Events. Python-SDK fehlen: `SessionStart`, `SessionEnd`, `Setup`, `TeammateIdle`, `TaskCompleted`, `ConfigChange`, `WorktreeCreate`, `WorktreeRemove` — für diese Shell-Command-Hooks in settings.json verwenden.
- **Best Practice:** Mehrere Hooks für dasselbe Event laufen parallel. Bei Permission-Entscheidungen gewinnt die restriktivste: ein einziges `deny` blockiert unabhängig von anderen Hook-Resultaten. Matchers möglichst präzise halten — kein Matcher = Hook läuft für JEDES Occurrence des Events.
- **Quelle:** [https://code.claude.com/docs/en/agent-sdk/hooks](https://code.claude.com/docs/en/agent-sdk/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## `SessionStart` — Kontext für alle Subagents injizieren

- **Was:** `SubagentStart`-Hooks (nicht `SessionStart`) können `additionalContext` für Subagents injizieren:
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "SubagentStart",
      "additionalContext": "Such-Reflex: Grep wenn Name bekannt, semantisch wenn nur Konzept."
    }
  }
  ```
- **Best Practice:** Das verschachtelte `hookSpecificOutput`-Schema ist Pflicht. Flaches `{ "additionalContext": "..." }` ohne `hookSpecificOutput` wird von Claude Code still ignoriert — verifizierter Bug/Design, 2026-05-24 (#1049). In PowerShell: `ConvertTo-Json -Depth 5` verwenden (ohne `-Depth 5` werden verschachtelte Objekte als `@{...}` serialisiert).
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell) + lokaler Vorfall #1049 (2026-05-24)
- **Stand:** 2026-05-24

---

## Bekannte Bugs & Workarounds (Changelog-basiert)

| Bug | Version gefixt | Workaround |
|-----|---------------|------------|
| `if`-Bedingungen wie `PowerShell(git push*)` matchen nie | v2.1.147 | Auf v2.1.147+ updaten |
| Stop-Hooks loopen endlos bei wiederholtem Block | v2.1.143 | `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP` setzen |
| Async PostToolUse-Hooks schreiben leere Transcript-Einträge | v2.1.137 | Auf v2.1.137+ updaten |
| Hooks kriegen non-existenten `transcript_path` nach `EnterWorktree` | v2.1.141 | Auf v2.1.141+ updaten |
| `prompt`-Hooks feuern erneut für Tool-Calls des Verifier-Subagents | v2.1.122 | Auf v2.1.122+ updaten |
| Subagent-Kontext-Injection: flaches `additionalContext` wird ignoriert | Kein Fix — Design | Nested `hookSpecificOutput`-Schema verwenden |
| Hooks schreiben direkt ins Terminal — korrupt interaktives Prompt | v2.1.119 | Auf v2.1.119+ updaten (kein tty-Zugriff mehr) |
| PowerShell prefix/wildcard allow rules funktionieren nicht korrekt | v2.1.149 | Auf v2.1.149+ updaten |

---

*Erstellt: 2026-05-25 | Recherchiert für Claude Code v2.1.150 | Hauptquellen: code.claude.com/docs (offiziell)*
