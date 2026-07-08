# Hook-Input-Validation: Echte Events von Phantom-Events unterscheiden (KRITISCH)

> Vorfall 2026-04-15: SubagentStop-Hook Endlosschleife (50+ Fires/Session ohne echten Subagent). Poka-Yoke Stufe 3.

## Grundregel

Jeder Hook auf **SubagentStop**, **SubagentStart** oder **PostToolUse** MUSS am Anfang pruefen, ob der
Hook-Input die erwarteten Felder enthaelt — sonst SOFORT `exit 0` ohne Output (kein Logging, kein
Schreiben, keine Seiteneffekte). Claude Code sendet bei jedem Stop-Event eine Invokation, auch ohne
echten Subagent (`agent_id` fehlt dann).

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

Hooks die NUR JSON-Kontext ausgeben und KEINE Side-Effects haben (kein Write/Log) brauchen KEINEN Guard
(er wuerde legitime Kontext-Injection verhindern, z.B. `subagent-context`). Faustregel: Guard nur wenn
der Hook WRITES/SIDE-EFFECTS hat.

## Umsetzung

Guard NACH Shebang/Header/`param()`/Dot-Source der Bibliotheken platzieren (sonst fehlen Log-Funktionen).
PowerShell: stdin per `[Console]::In.ReadToEnd()`, `ConvertFrom-Json`, bei fehlendem/leerem Feld `exit 0`.
Bash: `stdin=$(cat)`, Feld per `python3 -c json` extrahieren, bei leer `exit 0`. NIE als `exit 1`/`exit 2`
(blockiert Claude Code); NIE in einer dot-sourced Bibliothek (killt den Aufrufer).

## Was NIEMALS

- SubagentStop-Hook der `agent_id` nicht prueft · PostToolUse-Hook der `tool_name` nicht prueft
- Guard als `exit 1/2` · Guard in dot-sourced Bibliothek · den Guard ueberspringen "weil der Hook klein ist"
