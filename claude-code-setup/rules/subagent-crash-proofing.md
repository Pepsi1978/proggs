# Subagenten absturzsicher bauen — systemweit (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-05-31. Gilt fuer JEDEN Agent/Worker in
> JEDEM Plugin/Workflow/Team — nicht nur fuer das finale-Plugin (dort schon via FIN-048/051
> umgesetzt). Verallgemeinert FIN-048 auf alle Agenten. Strikt verlustfrei: siehe
> [[lossless-context-principle]]. Hintergrund/Belege: Memory `reference_subagent_crash_root_cause`.

---

## Die eine Ungleichung

> **Crash ("Prompt is too long") ⟺ Start-Sockel + Runtime-Wachstum > Modell-Limit.**

Mehr ist es nicht. Ein Subagent erbt einen Start-Sockel (System-Prompt + Tool-Schemas +
Skills-Liste + CLAUDE.md + Regeln, oft 70-120k Token) und waechst dann durch Tool-Outputs.
**Wichtig: Subagents haben KEIN Auto-Compact** (das laeuft nur im Hauptthread). Ein Worker
laeuft also bis er crasht — niemand rettet ihn zur Laufzeit. Die Disziplin muss ins Design.

Parallel-Anzahl ist NICHT die Ursache: Wenn ein einzelner Worker sauber durchlaeuft, laeuft
auch ein Schwarm durch. 7 parallele Worker sind fine. Entscheidend ist das Design des Einzelnen.

## Die 5 Prinzipien (alle verlustfrei)

1. **Start-Sockel schlank halten** — tools-Whitelist im Agent-Frontmatter (nur was gebraucht
   wird; MCP/Tools bleiben global verfuegbar, der eine Agent laedt nur weniger). Bei eigenen
   Agenten ggf. `memory:`-Scope. Tool Search (`ENABLE_TOOL_SEARCH`) deferred MCP-Schemas.
2. **Output-Disziplin** — Grep mit `head_limit`/`output_mode: count`/`files_with_matches`,
   erst breit-vollstaendig erfassen, DANN gezielt `content` nur fuer relevante Dateien.
   Read nur mit Ranges, nie ganze >500-Zeilen-Dateien. NIE blindes Kappen echter Treffer.
3. **File-as-Memory** — grosse Daten in Datei schreiben, nur Pfad + kompakte Zusammenfassung
   (≤1500-2000 Token) im Kontext halten/zurueckgeben. Details jederzeit per Pfad nachladbar.
4. **Engen Scope pro Agent** — ein Worker, der 142 Dateien grept, ist falsch dimensioniert:
   Arbeit auf mehr Worker aufteilen ODER gezielter scannen. Gesamtabdeckung bleibt identisch.
5. **Self-Monitoring im Prompt** — weil kein Auto-Compact greift: dem Worker mitgeben
   "halte deinen Kontext schlank, behalte nichts Grosses, lagere sofort in Datei aus, bei
   Annaeherung ans Limit Teilstand sichern + Folge-Worker". Ziel-Read-Budget pro Worker
   konservativ (~40-50k Token), da der Sockel den Rest frisst.

## Dateien grosse Apps/Daten NUR per Python anfassen
Worker manipulieren Ziel-Dateien per `open/read/write` in Python, NICHT mit dem Read-Tool
(Read laedt in den LLM-Kontext = Crash-Gefahr; Python-IO nicht). Buckets nach **Bytes**
(`wc -c`), nicht Zeilen — 180 dichte Zeilen koennen 31k Token sein.

## Funktionalitaet bleibt 100%
Jedes Prinzip ist verlustfrei (siehe Tabelle in [[lossless-context-principle]]). Vor breitem
Einsatz: Baseline + Regressionstest (Direktive #3) — Capability darf nicht sinken.

## Was NIEMALS passieren darf
- Einen Worker ohne tools-Whitelist mit vollem Tool-Erbe spawnen wenn er nur wenige braucht
- Grosse Dateien per Read-Tool in einen Worker laden statt per Python
- head_limit/Truncation so setzen dass echte Treffer/Findings verloren gehen (lossy!)
- Parallel-Anzahl senken statt den einzelnen Worker schlank zu machen
- Annehmen ein Subagent habe Auto-Compact — hat er nicht
