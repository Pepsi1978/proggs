# Hook-Input-Validation: Echte Events von Phantom-Events unterscheiden (KRITISCH)

## Grundregel
Jeder Hook auf **SubagentStop/SubagentStart/PostToolUse** MUSS am Anfang pruefen, ob die erwarteten Felder
da sind -- sonst SOFORT `exit 0` ohne Output (kein Log/Schreiben/Seiteneffekt). Claude Code feuert
Stop-Events auch ohne echten Subagent. Poka-Yoke 3 (2026-04-15).

## Pflicht-Feld pro Event
| Event | Pflicht-Feld | wenn leer/fehlt |
|-------|-------------|-----------------|
| SubagentStop | `agent_id` | `exit 0` ohne Output |
| SubagentStart | `agent_id` + `agent_type` | `exit 0` |
| PostToolUse | `tool_name` | `exit 0` |
| PostToolUseFailure | `tool_name` + `error` | `exit 0` |
| SessionStart | `session_id` | `exit 0` |
| Stop | keins | kein Guard noetig |

## AUSNAHME: Passive Context-Injection-Hooks
Hooks die NUR JSON-Kontext ausgeben, KEINE Side-Effects (Write/Log), brauchen KEINEN Guard (z.B.
`subagent-context`). Guard nur bei Side-Effects.

## Umsetzung
Guard NACH Shebang/`param()`/Dot-Source platzieren. PS: stdin `[Console]::In.ReadToEnd()`,
`ConvertFrom-Json`, leeres Feld -> `exit 0`. Bash: `stdin=$(cat)`, Feld per `python3 -c json`, leer ->
`exit 0`. NIE `exit 1`/`exit 2` (blockiert CC); NIE in dot-sourced Bibliothek.

## Was NIEMALS
- SubagentStop ohne `agent_id`-Check; PostToolUse ohne `tool_name`-Check; Guard als `exit 1/2`; Guard in
  dot-sourced Bibliothek; den Guard ueberspringen "weil der Hook klein ist".
