# Subagenten absturzsicher bauen — systemweit (KRITISCH)

> Fuer JEDEN Agent/Worker in JEDEM Plugin/Workflow. Strikt verlustfrei (`lossless-context-principle.md`).

## Die eine Ungleichung
Crash ("Prompt is too long") ⟺ Start-Sockel (System-Prompt + Tool-Schemas + Skills + Regeln, 70-120k) +
Runtime-Wachstum > Modell-Limit. Subagents haben KEIN Auto-Compact → laufen bis zum Crash. Parallel-Anzahl
ist NICHT die Ursache (laeuft einer sauber, laeuft auch ein Schwarm).

## Die 5 Prinzipien (verlustfrei)
1. **Start-Sockel schlank** — `tools:`-Whitelist im Frontmatter; `ENABLE_TOOL_SEARCH` (deferred MCP-Schemas).
2. **Output-Disziplin** — Grep mit `head_limit`/`count`/`files_with_matches`; Read nur mit Ranges, nie
   ganze >500-Zeilen-Dateien. NIE echte Treffer kappen.
3. **File-as-Memory** — grosse Daten in Datei, nur Pfad + Summary (≤1500-2000 Token) im Kontext.
4. **Enger Scope** — ein Worker der 142 Dateien grept ist falsch dimensioniert: aufteilen.
5. **Self-Monitoring** — schlank halten, sofort auslagern, bei Limit-Naehe Teilstand sichern + Folge-Worker.

## Dateien nur per Python
Ziel-Dateien per Python `open/read/write`, NICHT Read-Tool (laedt in Kontext = Crash). Buckets nach
**Bytes** (`wc -c`), nicht Zeilen (180 dichte Zeilen ≈ 31k Token).

## Die 4 Resilienz-Schichten
1. **Praevention** (5 Prinzipien). 2. **Checkpointing** — Fortschritt VOR jedem teuren Schritt in Datei.
3. **Orchestrator-Resume** — Crash erkennen (Fehler/`tokens==0`), Checkpoint lesen, Folge-Worker mit
kleinerem Scope. 4. **Selbst-Stopp via Proxy** — Bytes zaehlen, stoppen bei ~40-50k (nur Zusatz). Der
`subagent-context`-Hook injiziert die Kurzregel in JEDEN Subagent (SubagentStart).

## Was NIEMALS
- Worker ohne `tools:`-Whitelist mit vollem Tool-Erbe · sich auf Selbst-Stopp verlassen · nach Crash
  aufgeben · grosse Dateien per Read-Tool laden · `head_limit` so dass Treffer verloren gehen ·
  Parallel-Anzahl senken statt den Worker schlank zu machen.
