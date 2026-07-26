# Subagenten absturzsicher bauen (KRITISCH)

> Fuer JEDEN Agent/Worker. Strikt verlustfrei (`lossless-context-principle.md`).

## Die eine Ungleichung
Crash ("Prompt is too long") ⟺ Start-Sockel (70-120k) + Runtime-Wachstum > Modell-Limit. Subagents haben
KEIN Auto-Compact → laufen bis zum Crash. Parallel-Anzahl ist NICHT die Ursache.

## Die 5 Prinzipien (verlustfrei)
1. **Start-Sockel schlank** — `tools:`-Whitelist im Frontmatter; `ENABLE_TOOL_SEARCH`.
2. **Output-Disziplin** — Grep mit `head_limit`/`count`/`files_with_matches`; Read nur mit Ranges, nie
   ganze >500-Zeilen-Dateien. NIE echte Treffer kappen.
3. **File-as-Memory** — grosse Daten in Datei, nur Pfad + Summary im Kontext.
4. **Enger Scope** — Worker der 142 Dateien grept = aufteilen.
5. **Self-Monitoring** — schlank halten, sofort auslagern, bei Limit-Naehe Teilstand sichern + Folge-Worker.

## Umsetzung
Ziel-Dateien per Python `open/read/write`, NICHT Read-Tool (= Crash). Buckets nach **Bytes** (`wc -c`).
**4 Resilienz-Schichten:** Praevention · Checkpointing (VOR jedem teuren Schritt) · Orchestrator-Resume
(Crash `tokens==0` → Checkpoint lesen → Folge-Worker kleinerer Scope) · Byte-Selbst-Stopp. Der
`subagent-context`-Hook injiziert die Kurzregel in JEDEN Subagent.

## Was NIEMALS
- Worker ohne `tools:`-Whitelist · sich auf Selbst-Stopp verlassen · nach Crash aufgeben · grosse Dateien
  per Read-Tool laden · `head_limit` so dass Treffer verloren gehen · Parallel-Anzahl senken statt Worker schlank machen.
