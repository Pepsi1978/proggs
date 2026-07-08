# Hook-Input-Validation: Echte Events von Phantom-Events unterscheiden (KRITISCH)

> Vorfall 2026-04-15/18: SubagentStop-Hook Endlosschleife (50+ Fires/Session ohne echten Subagent).
> Poka-Yoke Stufe 3.

## Grundregel

Jeder Hook auf **SubagentStop**, **SubagentStart** oder **PostToolUse** MUSS am Anfang pruefen, ob der
Hook-Input die erwarteten Felder enthaelt. Wenn nicht: SOFORT `exit 0` ohne Output — kein Logging, kein
Schreiben, keine Seiteneffekte. (Claude Code sendet bei jedem Stop-Event eine Invokation, auch ohne
echten Subagent; `agent_id` fehlt dann.)

## Pflicht-Feld pro Event

| Event | Pflicht-Feld | wenn leer/fehlt |
|-------|-------------|-----------------|
| SubagentStop | `agent_id` | `exit 0` ohne Output |
| SubagentStart | `agent_id` + `agent_type` | `exit 0` |
| PostToolUse | `tool_name` | `exit 0` |
| PostToolUseFailure | `tool_name` + `error` | `exit 0` |
| Stop | keins | kein Guard noetig |
| SessionStart | `session_id` | `exit 0` |

## AUSNAHME: Passive Context-Injection-Hooks

Hooks die NUR JSON-Kontext ausgeben und KEINE Side-Effects haben (kein Whiteboard-Write, kein Log)
brauchen KEINEN Guard (er wuerde legitime Kontext-Injection verhindern). Beispiel:
`subagent-context.{ps1,sh}`. **Faustregel:** Guard nur wenn der Hook WRITES/SIDE-EFFECTS hat.

## PowerShell-Guard (Snippet)

```powershell
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin -and $stdin.Trim() -ne "") {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.agent_id -or [string]::IsNullOrWhiteSpace($parsed.agent_id)) { exit 0 }
    } else { exit 0 }
} catch { exit 0 }
```

## Bash-Guard (Snippet)

```bash
stdin_input=$(cat); if [ -z "$stdin_input" ]; then exit 0; fi
agent_id=$(echo "$stdin_input" | python3 -c "import sys,json
try: print(json.load(sys.stdin).get('agent_id','') or '')
except Exception: print('')" 2>/dev/null)
if [ -z "$agent_id" ]; then exit 0; fi
```

## Platzierung

NACH Shebang/Header/`param()`/Dot-Source der Bibliotheken (`hook-log`, `whiteboard-insert`) — DANN der
Guard (nicht frueher, sonst fehlen Log-Funktionen). Guard NIE als `exit 1`/`exit 2` (blockiert Claude
Code); NIE in einer dot-sourced Bibliothek (killt den Aufrufer).

## Was NIEMALS passieren darf

- SubagentStop-Hook der `agent_id` nicht prueft · PostToolUse-Hook der `tool_name` nicht prueft
- Guard als `exit 1/2` · Guard in dot-sourced Bibliothek · den Guard ueberspringen "weil der Hook klein ist"
