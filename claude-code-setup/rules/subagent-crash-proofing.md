# Subagenten absturzsicher bauen — systemweit (KRITISCH)

> Gilt fuer JEDEN Agent/Worker in JEDEM Plugin/Workflow/Team. Strikt verlustfrei
> ([[lossless-context-principle]]).

## Die eine Ungleichung

> **Crash ("Prompt is too long") ⟺ Start-Sockel + Runtime-Wachstum > Modell-Limit.**

Ein Subagent erbt einen Start-Sockel (System-Prompt + Tool-Schemas + Skills + CLAUDE.md + Regeln,
oft 70-120k) und waechst durch Tool-Outputs. **Subagents haben KEIN Auto-Compact** (nur der Hauptthread)
→ ein Worker laeuft bis er crasht. Disziplin muss ins Design. Parallel-Anzahl ist NICHT die Ursache
(laeuft ein Worker sauber, laeuft auch ein Schwarm).

## Die 5 Prinzipien (alle verlustfrei)

1. **Start-Sockel schlank** — `tools:`-Whitelist im Agent-Frontmatter (MCP/Tools bleiben global
   verfuegbar); `ENABLE_TOOL_SEARCH` deferred MCP-Schemas.
2. **Output-Disziplin** — Grep mit `head_limit`/`output_mode: count`/`files_with_matches`; Read nur mit
   Ranges, nie ganze >500-Zeilen-Dateien. NIE blindes Kappen echter Treffer.
3. **File-as-Memory** — grosse Daten in Datei, nur Pfad + Summary (≤1500-2000 Token) im Kontext.
4. **Enger Scope pro Agent** — ein Worker, der 142 Dateien grept, ist falsch dimensioniert: aufteilen.
5. **Self-Monitoring im Prompt** — dem Worker mitgeben: Kontext schlank halten, sofort in Datei auslagern,
   bei Annaeherung ans Limit Teilstand sichern + Folge-Worker. Ziel-Read-Budget ~40-50k Token.

## Dateien nur per Python anfassen

Worker manipulieren Ziel-Dateien per Python `open/read/write`, NICHT mit dem Read-Tool (Read laedt in
den LLM-Kontext = Crash-Gefahr). Buckets nach **Bytes** (`wc -c`), nicht Zeilen (180 dichte Zeilen ≈ 31k Token).

## Die 4 Resilienz-Schichten (kein Worker-Crash kostet die Aufgabe)

1. **Praevention** (Worker-Design, 5 Prinzipien) — verhindert die meisten Crashes.
2. **Inkrementelles Checkpointing** — Fortschritt VOR jedem teuren Schritt in Datei schreiben (nicht
   danach — sonst beim Crash leer).
3. **Orchestrator-Resume (Sicherheitsnetz)** — Orchestrator erkennt Crash (Fehler / `tokens==0`), liest
   Checkpoint, spawnt Folge-Worker mit kleinerem + diszipliniertem Scope ab dem Checkpoint:
   ```
   res = spawn(scope_X)
   if res == "Prompt is too long" or res.tokens == 0:
       done = read_checkpoint(scope_X); rest = scope_X - done
       for sub in split_smaller(rest): spawn_disciplined(sub)
   ```
4. **Selbst-Stopp via Proxy (Bonus)** — Worker zaehlt Bytes/Tool-Calls, stoppt geordnet bei ~40-50k
   gelesenen Bytes (allein unzuverlaessig — nur Zusatz).

**Empirisch (2026-05-31):** Read-Tool blockt bei 256 KB/Aufruf. Undisziplinierter Worker crasht nach
~6 Tool-Calls mit 0 Output; Crash ist fuer den Orchestrator sichtbar. Disziplinierter Folge-Worker
(Grep/wc/Ranges) erledigt dieselbe Aufgabe sauber.

## Systemweite Verankerung

`subagent-context`-Hook injiziert die Kontext-Schutz-Kurzregel in JEDEN Subagent (SubagentStart) — der
systemweite Traeger. finale-Plugin hat zusaetzlich FIN-052/053/054/055 + `scope-splitter.py`.

## Was NIEMALS passieren darf

- Worker ohne `tools:`-Whitelist mit vollem Tool-Erbe (wenn er wenige braucht)
- Sich auf Selbst-Stopp verlassen (tut er empirisch nicht) · nach Worker-Crash die Aufgabe aufgeben
- Grosse Dateien per Read-Tool in einen Worker laden · head_limit so setzen dass Treffer verloren gehen
- Parallel-Anzahl senken statt den einzelnen Worker schlank zu machen · annehmen ein Subagent habe Auto-Compact
