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

## Resume-Hand-off: was passiert, wenn ein Worker TROTZDEM ans Limit kommt (empirisch validiert 2026-05-31)

Live getestet auf BestJournalAndroid (142 .kt-Dateien). Empirische Fakten:
- **Read-Tool blockt bei 256 KB pro Aufruf** (eingebauter Schutz pro Read — eine 282-KB-Datei wird abgelehnt).
- Ein undisziplinierter general-purpose-Worker crasht schon nach **~6 Tool-Calls** (4 Voll-Reads
  grosser Dateien) mit "Prompt is too long", `subagent_tokens: 0`, **0 Output**.
- Der Crash ist fuer den **Orchestrator sichtbar** (Fehlermeldung + `agentId` + `tool_uses`).
- **Selbst-Disziplin allein ist unzuverlaessig**: der Worker kam nicht mal dazu, seinen ersten
  Checkpoint zu schreiben — der Crash kam ohne Vorwarnung mitten in der Read-Welle.
- Ein **disziplinierter Folge-Worker** (Grep/wc/Ranges) erledigt DIESELBE Aufgabe (inkl. der
  Crash-Dateien) sauber durch, Fuellstand gruen, 18 Tool-Calls, volle Ausgabe.

### Die 4 Resilienz-Schichten (kein Worker-Crash kostet die Aufgabe)

1. **Praevention (Worker-Design)** — verhindert die meisten Crashes: nie grosse Dateien ins
   LLM-Kontext (Python-IO/Grep/Ranges), enger Scope. Siehe 5 Prinzipien oben.
2. **Inkrementelles Checkpointing** — Fortschritt **VOR** jedem teuren Schritt in eine Datei
   schreiben (nicht danach! sonst beim Crash leer). Macht Resume verlustfrei.
3. **Orchestrator-Resume (Sicherheitsnetz)** — der Orchestrator erkennt den Crash am
   Fehler/0-Output, liest den letzten Checkpoint bzw. kennt den zugewiesenen Scope, und
   spawnt einen Folge-Worker mit **kleinerem + diszipliniertem** Scope ab dem Checkpoint.
   Pflicht-Pattern:
   ```
   res = spawn(scope_X)
   if res == "Prompt is too long" or res.tokens == 0:
       done = read_checkpoint(scope_X)        # was schon gesichert ist (kann leer sein)
       rest = scope_X - done
       for sub in split_smaller(rest):        # kleinere Haeppchen, diszipliniert
           spawn_disciplined(sub)             # Python/Grep statt Voll-Read
   ```
4. **Selbst-Stopp via Proxy (Bonus, Fruehwarnung)** — Worker zaehlt gelesene Bytes/Tool-Calls
   und stoppt geordnet bei ~40-50k gelesenen Bytes ODER ~15 content-Reads (Checkpoint +
   "HANDOFF"-Signal). Allein unzuverlaessig (siehe oben) — nur Zusatz zu 1-3.

**Kernaussage:** Die "automatische Umschaltung vor dem Limit" passiert NICHT im Worker
(er kann sich nicht zuverlaessig selbst messen/stoppen), sondern durch Schicht 1 (selten so
weit kommen) + Schicht 3 (Orchestrator faengt den Crash auf und setzt fort). Die AUFGABE
ueberlebt, auch wenn ein einzelner Worker stirbt. Das finale-Plugin hat den Ansatz in
FIN-004/005/048 — diese Regel macht ihn systemweit und ergaenzt das Orchestrator-Resume.

## Systemweite Verankerung (wo dieser Schutz ueberall greift)

Damit das 4-Schichten-Protokoll fuer ALLE Agenten gilt — nicht nur finale:
- **subagent-context-Hook** (`~/.claude/hooks/subagent-context.{sh,ps1}`) injiziert die
  Kontext-Schutz-Kurzregel automatisch in JEDEN Subagent (SubagentStart) — ohne dass der Agent
  diese Regel kennen muss. **Das ist der systemweite Traeger** (deckt alle Plugins/Skills/Agenten
  auf einen Schlag, statt dutzende Agent-Dateien einzeln zu aendern — fehleranfrei + wartbar).
- **finale-Plugin** hat zusaetzlich die erzwungenen Ablaeufe FIN-052 (Orchestrator-Resume),
  FIN-053 (inkrementelles Checkpointing), FIN-054 (Byte-Waechter), FIN-055 (Resume-Counter)
  + die Skripte `scripts/scope-splitter.py` (generisch) und `scripts/count-resumes.sh`.
- **Andere Plugins/Workflows mit Orchestrator/Worker-Pattern** uebernehmen: (1) `scope-splitter.py`
  vor jedem Spawn (beliebige Pfade, nicht finale-spezifisch), (2) das Resume-Pattern aus FIN-052
  als Pseudocode (Crash erkennen → Checkpoint lesen → kleiner+diszipliniert neu spawnen → nie aufgeben).

## Was NIEMALS passieren darf
- Einen Worker ohne tools-Whitelist mit vollem Tool-Erbe spawnen wenn er nur wenige braucht
- Sich darauf verlassen, dass der Worker sich selbst rechtzeitig stoppt (tut er empirisch nicht)
- Nach einem Worker-Crash die ganze Aufgabe aufgeben statt per Orchestrator-Resume fortzusetzen
- Grosse Dateien per Read-Tool in einen Worker laden statt per Python
- head_limit/Truncation so setzen dass echte Treffer/Findings verloren gehen (lossy!)
- Parallel-Anzahl senken statt den einzelnen Worker schlank zu machen
- Annehmen ein Subagent habe Auto-Compact — hat er nicht
