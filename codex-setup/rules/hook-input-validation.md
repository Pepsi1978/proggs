# Hook-Input-Validation: Echte Events von Phantom-Events unterscheiden (KRITISCH)

> Quelle: Vorfall 2026-04-15/18 — SubagentStop-Hook Endlosschleife (50+ Fires pro Session ohne echten Subagent)
> Direktive: #3 Resilient Bugfixing — Poka-Yoke Stufe 3 (Eliminierung)

---

## Grundregel

Jeder Hook der auf **SubagentStop**, **SubagentStart** oder **PostToolUse** reagiert
MUSS am Anfang pruefen ob der Hook-Input die erwarteten Felder enthaelt.
Wenn nicht: SOFORT `exit 0` ohne Output — kein Logging, kein Schreiben, keine Seiteneffekte.

Ohne diesen Guard laufen Hooks unkontrolliert bei jedem Stop-Event mit, auch wenn
kein echter Trigger vorlag.

---

## Warum das noetig ist (Vorfall 2026-04-15/18)

- `memory-watchdog.ps1` und `writeback-enforcer.ps1` sind als SubagentStop-Hooks registriert
- Sie pruefen NICHT ob tatsaechlich ein Subagent zurueckgekehrt ist
- Codex sendet bei jedem Stop-Event eine Hook-Invokation — auch ohne echten Subagent
- Ergebnis: 50+ Hooks pro Session, 50+ "Write-Back nicht erfolgt" Spam-Eintraege

Der Input enthaelt das Feld `agent_id` NUR bei echtem SubagentStop.
Bei normalem Stop fehlt es komplett.

---

## Pflicht-Pattern pro Event-Typ

| Event | Pflicht-Feld | Wenn leer/fehlt |
|-------|-------------|-----------------|
| `SubagentStop` | `agent_id` | `exit 0` ohne Output |
| `SubagentStart` | `agent_id` + `agent_type` | `exit 0` ohne Output |
| `PostToolUse` | `tool_name` | `exit 0` ohne Output |
| `PostToolUseFailure` | `tool_name` + `error` | `exit 0` ohne Output |
| `Stop` | keins (laeuft immer) | kein Guard noetig |
| `SessionStart` | `session_id` | `exit 0` ohne Output |

### AUSNAHME: Passive Context-Injection-Hooks

Hooks die **nur JSON-Kontext ausgeben** und **keine Side-Effects** haben (kein Whiteboard-Write,
kein Spam, keine Logs) brauchen KEINEN Guard. Im Gegenteil: Ein Guard wuerde legitime
Kontext-Injection verhindern wenn das Feld mal fehlt.

Beispiele:
- `subagent-context.ps1` / `.sh` — injiziert statische Projekt-Regeln bei jedem SubagentStart
- Andere reine Prompt-Injection-Hooks

**Faustregel:** Guard nur wenn der Hook WRITES oder SIDE-EFFECTS hat. Bei passivem Output-Only: kein Guard.

---

## PowerShell-Guard (Pflicht-Snippet)

```powershell
# Input guard — nur bei echten Events laufen
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin -and $stdin.Trim() -ne "") {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.agent_id -or [string]::IsNullOrWhiteSpace($parsed.agent_id)) {
            exit 0
        }
    } else {
        exit 0
    }
} catch {
    exit 0
}
```

## Bash-Guard (Pflicht-Snippet)

```bash
stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi
agent_id=$(echo "$stdin_input" | python3 -c "import sys,json
try:
    d=json.load(sys.stdin)
    print(d.get('agent_id','') or '')
except Exception:
    print('')
" 2>/dev/null)
if [ -z "$agent_id" ]; then exit 0; fi
```

---

## Wichtig: Platzierung im Hook

**Richtig:**
1. Shebang (`#!/usr/bin/env pwsh` oder `#!/usr/bin/env bash`)
2. Kommentar-Header
3. `param()` Block (PowerShell)
4. Dot-Source von Bibliotheken (`hook-log`, `whiteboard-insert`)
5. **DANN der Input-Guard** (nicht frueher, sonst fehlen Log-Funktionen)
6. Eigentliche Hook-Logik

**Falsch:**
- Guard VOR dem Dot-Source (Log-Funktionen nicht verfuegbar bei Debug)
- Guard mit `exit` in dot-sourced Bibliothek (killt den Aufrufer!)

---

## Hooks die den Guard AKTUELL haben (Stand 2026-04-20)

- `memory-watchdog.ps1` + `memory-watchdog.sh`
- `writeback-enforcer.ps1` + `writeback-enforcer.sh`
- `subagent-stop-summarizer.ps1` (prueft redundant 3 Felder, funktioniert aber)

---

## Was NIEMALS passieren darf

- ❌ SubagentStop-Hook schreiben der `agent_id` nicht prueft
- ❌ PostToolUse-Hook schreiben der `tool_name` nicht prueft
- ❌ Guard als `exit 1` / `exit 2` formulieren (blockiert Codex)
- ❌ Guard-Logik in dot-sourced Bibliothek einbauen (verschiebt den Fehler nur)
- ❌ Den Guard ueberspringen "weil der Hook klein ist" — JEDER Hook mit Event-spezifischen Feldern braucht ihn

---

## Poka-Yoke-Stufe

**Stufe 3 (Eliminierung):** Wenn der Guard als erstes Snippet in jedem Hook steht,
kann der Phantom-Fire-Fehler konzeptionell nicht mehr auftreten. Der Hook weiss
immer am Anfang ob er tatsaechlich aufgerufen werden sollte.

## Automatische Pruefung

Bei jedem `/self-improve` Stufe 6: Alle Hooks in `~/.codex/hooks/` pruefen die auf
`SubagentStop`, `SubagentStart` oder `PostToolUse` reagieren. Fehlt der Guard:
OFFEN-Eintrag ins Whiteboard + Fix-Vorschlag.
