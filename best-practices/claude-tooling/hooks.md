# Hooks — Best Practices (Stand 2026-06-15, Claude Code 2.1.177)

> Quellen: Offizielle Claude Code Dokumentation (code.claude.com/docs) + Changelog.
> Alle Einträge ohne "extern"-Label sind offiziell bestätigt.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Hook soll blockieren | `exit 2` (nie `exit 1`); Write/Edit via `permissionDecision:"deny"` | Exit-Codes |
| 2 | Kontext an Claude geben | verschachteltes `hookSpecificOutput.{hookEventName,additionalContext}` (PS `-Depth 5`) | JSON-Schema |
| 3 | Side-Effect-Hook (SubagentStop/Stop) | Input-Guard, sonst Phantom-Fires; passive Injection: KEIN Guard | Input-Guard |
| 4 | Stop-Hook | `stop_hook_active` zuerst pruefen (Endlosschleife) | Stop-Loop-Schutz |
| 5 | matcher | exakt `Edit\|Write`, MCP mit `.*`-Suffix | Hook-Events |
| 6 | Standalone-Hook-Ende | `exit 0`; FAIL-OPEN (interner Fehler → `exit 0`) | Hook-Kategorien |
| 7 | Windows/Bash/Cross-Platform | `pwsh`, BOM-frei, LF, `+x`; `.ps1`+`.sh` paritaetisch; dot-sourced libs ohne top-level `exit` | Sicherheit / Kategorien |
| 8 | Tiefen-Praevention | dedizierte Gegenseite: `claude-hooks.md` + Almanach (Stufe C) | — |

---

## Überblick: Hook-Typen

- **Was:** Claude Code unterstützt 5 Handler-Typen: `command` (Shell-Skript), `http` (HTTP POST), `mcp_tool` (MCP-Server-Tool), `prompt` (Claude-Modell bewertet), `agent` (experimenteller Subagent mit Tools).
- **Best Practice:** `command`-Hooks für deterministische Regeln (Linting, Guards). `prompt`-Hooks nur für Entscheidungen die Urteilsvermögen brauchen — sie sind langsamer und kosten Tokens. `agent`-Hooks sind experimentell und können sich ändern.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-25

---

## Alle Hook-Events (32 Events, Stand v2.1.152+)

- **Was:** Claude Code feuert 32 verschiedene Events über den Session-Lifecycle (32 seit v2.1.152 mit MessageDisplay):
  - **Session:** `SessionStart`, `Setup`, `SessionEnd`
  - **Turn:** `UserPromptSubmit`, `UserPromptExpansion`, `Stop`, `StopFailure`
  - **Tool-Loop:** `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `PostToolBatch`, `PermissionRequest`, `PermissionDenied`
  - **Subagents:** `SubagentStart`, `SubagentStop`, `TeammateIdle`
  - **Tasks:** `TaskCreated`, `TaskCompleted`
  - **Kontext:** `InstructionsLoaded`, `ConfigChange`, `CwdChanged`, `FileChanged`, `PreCompact`, `PostCompact`
  - **Worktrees:** `WorktreeCreate`, `WorktreeRemove`
  - **MCP:** `Elicitation`, `ElicitationResult`
  - **Anzeige (NEU v2.1.152):** `MessageDisplay`
  - **Sonstiges:** `Notification`
- **Best Practice:** Hooks ohne `agent_id`-Prüfung bei `SubagentStop` feuern bei JEDEM Stop-Event, auch ohne echten Subagent — daher immer einen Input-Guard einbauen (siehe Abschnitt "Input-Guard").
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell)
- **Stand:** 2026-05-28

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

## NEU (v2.1.152, 26. Mai 2026): `SessionStart` — `reloadSkills`

- **Was:** `SessionStart`-Hooks können jetzt `reloadSkills: true` im `hookSpecificOutput` zurückgeben. Das löst einen neuen Skill-Directory-Scan aus, sodass Skills die der Hook in `~/.claude/skills/` oder `.claude/skills/` geschrieben hat, noch **in derselben Session** (ab dem ersten Prompt) verfügbar sind.

  **Hintergrund:** Skill-Discovery läuft normalerweise VOR dem SessionStart-Hook — ohne `reloadSkills` tauchen neu geschriebene Skills erst in der nächsten Session auf.

  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "SessionStart",
      "reloadSkills": true
    }
  }
  ```

  **Vollständiges Beispiel** — Shared Team-Skills synchronisieren:
  ```bash
  #!/bin/bash
  # Synchronisiert Team-Skills und aktiviert sie in der aktuellen Session
  git -C ~/.claude/skills/team-skills pull --quiet 2>/dev/null || \
    git clone --quiet https://git.example.com/your-org/team-skills.git ~/.claude/skills/team-skills
  
  echo '{"hookSpecificOutput": {"hookEventName": "SessionStart", "reloadSkills": true}}'
  ```

  Ergänzend: Manuell kann der Skill-Scan auch per `/reload-skills`-Command ausgelöst werden (neu in v2.1.152).

- **Best Practice:** `reloadSkills: true` nur dann setzen, wenn der Hook tatsächlich Skills-Dateien geschrieben hat — sonst unnötiger Re-Scan beim jedem Session-Start. Das Flag kann mit `sessionTitle` und `additionalContext` kombiniert werden (alle im gleichen `hookSpecificOutput`-Objekt).
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell) + [Changelog v2.1.152](https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/) (offiziell)
- **Stand:** 2026-05-26

---

## NEU (v2.1.152, 26. Mai 2026): `SessionStart` — `sessionTitle`

- **Was:** `SessionStart`-Hooks können beim Start oder Resume automatisch den Session-Titel setzen — hat denselben Effekt wie der `/rename`-Command.

  **Wann aktiv:** Nur bei `source = "startup"` oder `source = "resume"`. Wird ignoriert bei `"clear"` und `"compact"`.

  **Input-Feld zum Prüfen:** Der Hook-Input enthält `session_title` — damit lässt sich prüfen ob der Benutzer bereits einen eigenen Titel gesetzt hat (nicht überschreiben).

  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "SessionStart",
      "sessionTitle": "auth-refactor"
    }
  }
  ```

  **Vollständiges Beispiel** — Session aus Git-Branch benennen:
  ```bash
  #!/bin/bash
  # Titel aus aktuellem Git-Branch ableiten, nur wenn noch kein Titel gesetzt
  INPUT=$(cat)
  EXISTING_TITLE=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('session_title','') or '')" 2>/dev/null)
  
  if [ -z "$EXISTING_TITLE" ]; then
    BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    if [ -n "$BRANCH" ]; then
      echo "{\"hookSpecificOutput\": {\"hookEventName\": \"SessionStart\", \"sessionTitle\": \"$BRANCH\"}}"
    fi
  fi
  ```

  **Kombiniertes Beispiel** — Alle drei SessionStart-Felder gleichzeitig:
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "SessionStart",
      "additionalContext": "Current branch: feat/auth-refactor\nActive issue: #4211",
      "sessionTitle": "auth-refactor",
      "reloadSkills": true
    }
  }
  ```

- **Best Practice:** Immer zuerst `session_title` aus dem Input lesen — wenn der Benutzer bereits einen eigenen Titel vergeben hat, nicht überschreiben. Für automatische Benennung aus Git-Branch, Worktree-Name oder CWD-Ordnernamen besonders nützlich.
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell) + [Changelog v2.1.152](https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/) (offiziell)
- **Stand:** 2026-05-26

---

## NEU (v2.1.152, 26. Mai 2026): `MessageDisplay` — Anzeige-Text transformieren

- **Was:** Ein neuer Hook-Event `MessageDisplay` der feuert, während Assistant-Message-Text auf dem Bildschirm angezeigt wird. Hooks können den **angezeigten** Text ersetzen oder verbergen — Transkript und Claude's internes Kontext bleiben unverändert.

  **Kritischer Unterschied:**
  - `displayContent` → ändert was der **Benutzer sieht**
  - Transcript-Datei → behält **Original** (unverändert)
  - Claude's Kontext → sieht **Original** (unverändert)
  - Kein Matcher-Support — feuert bei JEDEM Occurrence

  **Input-Schema:**
  ```json
  {
    "session_id": "abc123",
    "transcript_path": "/path/to/transcript.jsonl",
    "cwd": "/current/working/directory",
    "hook_event_name": "MessageDisplay",
    "message_text": "The full assistant message text",
    "message_index": 0
  }
  ```

  **Output-Schema (hookSpecificOutput):**
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "MessageDisplay",
      "displayContent": "Ersetzter Text der auf dem Bildschirm erscheint"
    }
  }
  ```

  **Streaming-Felder** (laut Search-Ergebnissen — als `extern` markiert bis offiziell bestätigt):
  - `turn_id` — Identifier für den Conversation-Turn
  - `message_id` — Identifier für die Nachricht
  - `index` — Position innerhalb der Nachricht (bei gestreamten Blöcken)
  - `final` — Boolean, ob dies der letzte Block ist
  - `delta` — Der neue Text-Delta der gerade angezeigt wird
  *(Quelle: [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) — extern-Hinweis: Streaming-Felder aus Docs, aber delta/final/index noch nicht in offiziellem Schema-Beispiel der Hauptdoku bestätigt)*

  **Anwendungsfälle:**
  1. **Secrets-Redaktion:** API-Keys, Tokens, Passwörter aus der Anzeige entfernen bevor der Benutzer sie sieht
  2. **Formatierung:** Rohen Code-Output für bessere Lesbarkeit formatieren
  3. **Übersetzung:** Display-Text in andere Sprache übersetzen (Claude denkt auf Englisch, Benutzer sieht Deutsch)
  4. **Compliance:** Interne Informationen die Claude erwähnt für den Benutzer-Screen redigieren
  5. **Debugging:** Intern-Tags aus der Anzeige entfernen die für Claude gedacht waren

  **Einfaches Beispiel** — API-Keys aus Anzeige entfernen:
  ```bash
  #!/bin/bash
  INPUT=$(cat)
  MESSAGE=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message_text',''))" 2>/dev/null)
  
  # API-Keys redaktieren (Pattern: sk-xxx... oder ghp_xxx...)
  REDACTED=$(echo "$MESSAGE" | sed 's/sk-[A-Za-z0-9]\{20,\}/sk-[REDACTED]/g' | sed 's/ghp_[A-Za-z0-9]\{30,\}/ghp_[REDACTED]/g')
  
  if [ "$MESSAGE" != "$REDACTED" ]; then
    python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput': {'hookEventName': 'MessageDisplay', 'displayContent': sys.argv[1]}}))" "$REDACTED"
  fi
  ```

- **Best Practice:**
  1. `MessageDisplay` nur für echte Anzeige-Transformation verwenden — es beeinflusst NICHT was Claude tut oder schreibt
  2. Wenn `displayContent` nicht gesetzt wird (kein Output oder kein `hookSpecificOutput`), bleibt der originale Text unverändert
  3. Für Security-Redaktion besonders wertvoll: Der Hook kann sensitives Material ausblenden ohne Claude's Reasoning zu beeinflussen
  4. Kein Matcher-Support bedeutet: jeder MessageDisplay-Hook läuft für ALLE Assistant-Messages — Performance beachten (keine schweren Operationen)
  5. Da Transcript und Claude-Kontext unverändert bleiben: nur für rein visuelle Transformationen geeignet, nicht für faktische Korrekturen
- **Quelle:** [https://code.claude.com/docs/en/hooks](https://code.claude.com/docs/en/hooks) (offiziell) + [Changelog v2.1.152](https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/) (offiziell)
- **Stand:** 2026-05-26

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
      "updatedInput": { "command": "sicherere Version" },
      "additionalContext": "Kontext für Claude"
    }
  }
  ```
  - `allow` — sofort genehmigen
  - `deny` — blockieren (Claude sieht den Grund)
  - `ask` — Benutzer fragen (überschreibt Auto-Mode)
  - `defer` — normalen Permission-Flow laufen lassen
- **Best Practice:** Immer `permissionDecisionReason` setzen — Claude nutzt diesen Text um zu verstehen warum eine Aktion blockiert wurde und vermeidet wiederholte Versuche. Beim Modifizieren des Inputs (`updatedInput`) MUSS `permissionDecision: "allow"` oder `"ask"` mitgegeben werden — sonst wird die Modifikation ignoriert.
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

*Erstellt: 2026-05-25 | Aktualisiert: 2026-05-28 | Stand: Claude Code v2.1.153 | Hauptquellen: code.claude.com/docs (offiziell)*

---

### Update 2026-06-05 (Claude Code 2.1.165) — Hooks

Alle drei Aenderungen kamen mit **2.1.163** (2026-06-04). Die hooks-Doku ist noch nicht aktualisiert (Doku-Lag) — der Changelog ist die Grundwahrheit.

**1. Stop-/SubagentStop-Hooks: `hookSpecificOutput.additionalContext`**
- **Was:** Stop- und SubagentStop-Hooks koennen jetzt `hookSpecificOutput.additionalContext` zurueckgeben, um Claude Feedback zu geben und den Turn fortzusetzen — OHNE als Hook-Fehler gewertet zu werden.
- **Best Practice:** `additionalContext` nutzen, wenn der Hook Feedback *injizieren* soll (metacognitive Erinnerung, Verifikations-Nachfass), der Turn aber weiterlaeuft. `decision:"block"` nur, wenn Claude am Stoppen gehindert werden soll (fehlschlagende Tests). Exit 2 ist ein harter Fehler — fuer Feedback NIE verwenden. Schema:
```json
{ "hookSpecificOutput": { "hookEventName": "Stop", "additionalContext": "Bitte verifiziere ob alle Tasks committed sind." } }
```
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**2. `if: "Bash(...)"`-Conditions — False-Positive-Fix**
- **Was:** `if: "Bash(...)"`-Bedingungen feuerten faelschlich bei JEDEM Bash-Command mit `$()` oder `$VAR`; das Muster matcht jetzt korrekt auch Commands in Subshells/Backticks.
- **Best Practice:** `if`-Conditions spezifisch gegen das Kommando schreiben (`"if": "Bash(git push *)"`), nicht gegen Variablen. Nach Update eigene Hooks mit `if: "Bash(...)"` auf False-Positives re-testen.
- **Quelle:** code.claude.com/docs/en/hooks `[offiziell]`

**3. Deny-Rules auf Home-Pfaden — `$HOME` vs. `~/`**
- **Was:** Deny-Rules wie `Read(~/Desktop/**)` blockierten keine Bash-Commands, die den Pfad ueber `$HOME` referenzierten — jetzt aequivalent behandelt.
- **Best Practice:** Deny-Rules auf sensible Home-Verzeichnisse (`~/.ssh`, `~/.gnupg`, `~/.claude`) sind jetzt zuverlaessiger; bisher noetige Doppel-Eintraege (`~/path` UND `$HOME/path`) koennen entfallen. Defense-in-Depth: Deny = Stufe 1, PreToolUse-Guard = Stufe 2 bleibt.
- **Quelle:** code.claude.com/docs/en/permissions `[offiziell]`

**Betrifft eigene Werkzeuge:** Ja — `hyperagent-stop`, `writeback-enforcer`, `memory-watchdog` (Stop/SubagentStop) koennen statt blossem `exit 0` jetzt `hookSpecificOutput.additionalContext` fuer Feedback nutzen, ohne `decision:block` zu brauchen.

---

### Update 2026-06-15 (Claude Code 2.1.177) — Re-Recherche-Welle, gh-hart verifiziert

> Quelle: 7-Researcher-Schwarm + harte `gh issue view`-Pruefung. Gegenstueck im Bug-Almanach:
> `bugs/claude-tooling/claude-hooks.md` §16. Korrektur: Feld heisst **`updatedInput`**, NICHT
> `modifiedInput` (oben durchgehend korrigiert; gh #39814 + offizielle hooks-Doku).

**1. Hook-Output IMMER strikt spec-konform bauen (sonst CLI-Hard-Crash)**
- **Was:** Non-spec `hookSpecificOutput` (falscher Typ, fehlendes `hookEventName`, unerwartete Struktur)
  crasht die GANZE Session mit TypeError, ohne Recovery (#57483, NOT_PLANNED → won't fix).
- **Best Practice:** Jeder eigene Hook validiert seinen JSON-Output, BEVOR er ihn ausgibt. PS:
  `ConvertTo-Json -Depth 5` (nie `@{}`-Strings); Bash: nur valides JSON auf stdout, alles andere nach
  stderr. Direkt relevant fuer `subagent-context.{ps1,sh}` und jeden additionalContext-Hook.
- **Quelle:** github.com/anthropics/claude-code/issues/57483 `[extern, gh-verifiziert]` · Version: bis 2.1.177

**2. stdin NIE mit `jq` parsen (Security-Bypass durch Control-Chars)**
- **Was:** stdin-JSON kann literale Control-Chars U+0000–U+001F enthalten (Paste aus PDF, mehrzeilige
  Prompts) → `jq` lehnt ab → ein Security-PreToolUse-Hook bricht STILL ab → Aktion laeuft ungeprueft
  durch (#53463, NOT_PLANNED).
- **Best Practice:** stdin mit `python3 -c "import sys,json; d=json.loads(sys.stdin.read())"` parsen
  (toleranter); bei Parse-Fehler **fail-closed** (deny / `exit 2`), NIE still durchwinken. Deckt sich
  mit der bestehenden "jq vermeiden"-Regel und macht sie sicherheitskritisch.
- **Quelle:** github.com/anthropics/claude-code/issues/53463 `[extern, gh-verifiziert]` · Version: bis 2.1.177

**3. PreToolUse-Block ueber exit 0 + JSON `permissionDecision:"deny"`, NICHT `exit 2`**
- **Was:** Ein `exit 2`-Block wird vom Modell (ab Opus 4.6) wie ein User-"Deny" behandelt → Claude
  beendet oft den Turn statt das Feedback zu nutzen. Das Client-Issue #24327 ist zwar COMPLETED, das
  MODELL-Verhalten bleibt. Ausserdem wird JSON bei `exit 2` ignoriert.
- **Best Practice:** Block via `exit 0` + `{"hookSpecificOutput":{"hookEventName":"PreToolUse",
  "permissionDecision":"deny","permissionDecisionReason":"<konkret>"}}`. Claude liest die Felder als
  Steuersignal und kann nachbessern. `permissionDecisionReason` immer setzen (verhindert Wiederholversuche).
- **Quelle:** github.com/anthropics/claude-code/issues/24327 + hooks-Doku `[offiziell/extern]` · Version: 2.1.x

**4. Tool-Hooks feuern NICHT fuer Tool-Calls innerhalb von Subagents**
- **Was:** PreToolUse/PostToolUse greifen nur in der Haupt-Tool-Loop, nicht fuer Tool-Calls, die ein
  Subagent (Agent-Tool) intern macht (#34692, NOT_PLANNED).
- **Best Practice:** Hook-abhaengige Policy nicht im Subagent erwarten. Subagent-Kontext via
  `SubagentStart`/`SubagentStop`; harte Policy zusaetzlich an der Spawn-Stelle verankern.
- **Quelle:** github.com/anthropics/claude-code/issues/34692 `[extern, gh-verifiziert]` · Version: bis 2.1.177

**5. Versions-Anker fuer Hook-Autoren (Mindestversionen)**
- Stop/SubagentStop-`additionalContext`-Feedback: **ab v2.1.163**. `if:"Bash(...)"`-False-Positive-Fix
  (`$()`/`$VAR`): **v2.1.163**. Windows expliziter-bash-Aufruf-Fix: **v2.1.161**. if-Pfad-Matcher
  (`Edit(src/**)`, `Read(.env)`): erst ab **v2.1.176** zuverlaessig. `--safe-mode`/`CLAUDE_CODE_SAFE_MODE`
  (deaktiviert auch Hooks, zum Isolieren beim Debuggen): **v2.1.169**. `claude plugin update` erhaelt
  jetzt `+x` (#40280 COMPLETED) — Cloud-Sync-Verlust des Execute-Bits bleibt aber (`find … -exec chmod +x`).
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]` · Version: 2.1.161–2.1.177

---

*Re-Recherche 2026-06-15: Stand auf Claude Code 2.1.177 gehoben. Gegenstueck: bugs/claude-tooling/claude-hooks.md §16.*
