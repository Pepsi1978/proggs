# Bekannte Bugs: Boss-/Orchestrator-Agent in einem Multi-Agenten-System

> **PFLICHT-LESEN vor Arbeit an einem Haupt-/Boss-/Orchestrator-Agenten**, der natuerliche
> Sprache versteht, sich menschlich unterhaelt, Aufgaben auf Tools/Worker abbildet und/oder
> selbst Unteragenten baut, spawnt und nutzt.
> **Stand:** recherchiert am 2026-06-09. Anker: Claude Code **2.1.169** (Opus 4.x) + die
> aktuellen Versionen (Juni 2026) der Frameworks LangGraph (~1.0/0.4), CrewAI (~0.105+),
> AutoGen/AG2 (1.0 GA, Maintenance), Microsoft Agent Framework, OpenAI Agents SDK (~0.7),
> Claude Agent SDK. Issue-Status wurde am 2026-06-09 hart per `gh` geprueft.
>
> Begleitseite (wie man es von vornherein richtig macht): siehe
> `best-practices/projekt-code/agents/best-practices-orchestrator-agent.md`.
> Eng verwandte Harness-Regeln (immer geladen): `subagent-crash-proofing.md`,
> `lossless-context-principle.md`, `metacognitive-monitoring.md`, `agent-and-researcher-rules.md`.

---

## TL;DR — die 7 wichtigsten Regeln

1. **Verstehen ist nicht Umsetzen.** Der haeufigste Fehler ist nicht ein Crash, sondern dass
   der Agent etwas ANDERES tut als gemeint (Intent-Misclassification, ueber-woertliche oder
   ueber-liberale Auslegung). Gegenmittel: **Plan/Read-back vor Ausfuehrung** + bei Unsicherheit
   **nachfragen statt raten** (AskUserQuestion) + **Scope als Constraint** in den Prompt.
2. **Jede Schleife braucht eine Stop-Bedingung — das Limit ist nur das Sicherheitsnetz.**
   Endlos-Delegation, Handoff-Ping-Pong und Tool-Loops sind die Top-Architektur-Bugs.
3. **Sub-Agenten erben einen riesigen Start-Sockel und haben KEIN Auto-Compact.** Spawnen mit
   vollem Kontext/allen Tool-Schemas → „Prompt is too long" bevor die Aufgabe beginnt. Tools-
   Whitelist, schlanker Prompt, File-as-Memory, Orchestrator-Resume.
4. **Sub-Agent-Output ist KEINE vertrauenswuerdige Wahrheit.** Stille Fehler + Error-
   Amplifikation (bis 17x) + Prompt-Injection propagieren durch die Kette. Verifizieren +
   typed-schema-validieren zwischen Agenten.
5. **Tool-Args VOR Ausfuehrung gegen striktes Schema validieren** + Retry-mit-Fehlerkontext.
   Faengt kaputtes JSON, Schema-Mismatch und (aktuell!) die Opus-4.8-tool_use-Regression.
6. **Zu viele Tools verschlechtern die Auswahl drastisch** (43%→2% bei 4→51 Tools). Gruppieren,
   <20 pro Call, progressive disclosure / Tool-Search.
7. **Menschlicher Dialog braucht aktive Pflege:** Persona-Anker re-injizieren (Drift), History
   zusammenfassen (Lost-in-Conversation, ~39% Genauigkeitsverlust), bei Voice satzweise streamen
   + Barge-In <150ms.

---

## 1. Intent-Verstehen (NLU) — der Agent versteht/macht etwas anderes als gemeint

> Empirische Basis: **MAST** (UC Berkeley, arXiv 2503.13657, 200+ Traces ueber 7 Frameworks,
> Cohen's Kappa 0.88) mit exakten Failure-Mode-Prozentwerten. Konkretester Einzelfall:
> Claude Code **#41707** (hart geprueft: CLOSED **NOT_PLANNED** 2026-05-23 → won't-fix, bleibt aktive Falle).

### 1.1 Intent-Misclassification — Wunsch wird falschem Worker/Tool zugeordnet  [⭐ HAEUFIG]
**Symptom:** Orchestrator routet die Anfrage an den falschen Worker/Tool; mit steigender Tool-/
Intent-Zahl haeuft sich die Fehlerrate (compounding).
**Ursache:** LLM-Routing ist nicht-deterministisch; bei vielen Intents wird der Prompt komplex,
das Modell „raet". Kein Validierungs-Schritt nach der Klassifikation.
**Versionen:** per Design (alle LLM-Orchestratoren).
**FIX:** Routing-Traces loggen (trennt Router- von Agent-Fehler). Fuer FIXE Logik (A vs. B) KEIN
LLM, sondern eine Regel — spart Latenz und Fehlklassifikation (A2A #773). Intent-/semantisches
Routing per Query-Embedding statt Mega-Prompt. Validierungs-Schritt nach der Klassifikation.
**Quelle:** A2A #773; Patronus AI „Agent Routing"; MAST.

### 1.2 Fail-to-ask-clarification — Agent raet statt nachzufragen  [⭐ HAEUFIG]
**Symptom:** Bei vagem/unterspezifiziertem Befehl raet der Agent eine Interpretation und baut sie
aus. MAST FM-2.2 = **6,8%** aller Fehler.
**Ursache:** Agent kann waehrend der Ausfuehrung nicht „zwischen den Zeilen lesen"; behandelt vage
Prosa als vollstaendige Spec.
**FIX:** Clarifying-Questions-Policy mit Schwelle; bei Unsicherheit **AskUserQuestion** (Multiple-
Choice = niedrige Reibung) statt offener Frage. Akzeptanzkriterien/Spec vorab (Spec-First).
**Quelle:** MAST FM-2.2; Galileo „why multi-agent systems fail"; Claude Code AskUserQuestion-Doku.

### 1.3 Ueber-woertliche Auslegung — „macht das Falsche genau richtig"  [⭐ HAEUFIG]
**Symptom:** Agent setzt buchstaeblich um, was nicht gemeint war. Dokumentiert in CC #41707:
„no env vars" wurde als „aus der Doku entfernen" verstanden → Doku statt Skript-Signatur geaendert;
gleicher Fehlansatz 4x variiert wiederholt.
**Ursache:** Woertliche statt sinngemaesse Auslegung; keine Rueckkopplung der verstandenen Absicht.
MAST FM-1.1 = **11,8%**.
**Versionen:** Claude Code (Sonnet 4.6 belegt); per Design modelluebergreifend. Status: #41707 CLOSED
NOT_PLANNED 2026-05-23 (won't-fix → Workaround bleibt noetig).
**FIX:** **Read-back/Intent-Confirmation vor Ausfuehrung** (Agent formuliert Ziel + betroffene
Dateien zur Bestaetigung); **Plan-Mode**. Bei >2 Fehlversuchen NICHT variieren-und-raten, sondern
stoppen + nachfragen (deckt sich mit der 3-Iterationen-Stop-Regel).
**Quelle:** anthropics/claude-code #41707.

### 1.4 Ueber-liberale Auslegung / Scope-Creep — „macht zu viel"
**Symptom:** Agent erweitert eigenmaechtig den Umfang — durchsucht das ganze Repo statt der
genannten Dateien, aendert nicht-erwaehnte Dateien.
**Ursache:** Halluzinierter/extrapolierter Intent („extrapoliert ueber die Prompt-Beispiele hinaus").
**FIX:** Scope explizit als Constraint („NUR Datei X und die READMEs, die sie aufrufen");
Plan-Mode zeigt vorab WELCHE Dateien beruehrt werden; Datei-Whitelist/Ownership erzwingen.
**Quelle:** CC #41707; LLM-Agent-Hallucination-Survey arXiv 2509.18970.

### 1.5 Task-Derailment — Abdriften vom Ziel
**Symptom:** Agent weicht im Lauf vom urspruenglichen Auftrag ab. MAST FM-2.3 = **7,4%**.
**FIX:** Ziel/Spec persistent halten + periodisch abgleichen („arbeite ich noch am Ziel?");
strukturierte Plan-Schritte mit Akzeptanzkriterien je Schritt (vgl. metacognitive-monitoring Drift-Detektor).
**Quelle:** MAST FM-2.3.

### 1.6 Reasoning-Action-Mismatch — denkt A, tut B  [⭐ HAEUFIG]
**Symptom:** Agent begruendet korrekt, fuehrt aber eine abweichende Aktion aus. Haeufigster
Einzelmodus: **13,2%**.
**Ursache:** Bruch zwischen Plan-Text und Tool-Call-Generierung; oft bei Multi-Tool-Calling.
**FIX:** Plan und Aktion koppeln (plan-before-act, dann GENAU den Plan ausfuehren); Tool-Auswahl
explizit begruenden lassen; strukturierte Slot-Filling-Prompts.
**Quelle:** MAST FM-2.6.

### 1.7 Slot-/Parameter-Extraktion falsch oder fehlend
**Symptom:** Falsche/fehlende Parameter aus dem Satz; LLMs sind schwach bei fein-granularer
Content-Extraktion, besonders bei Domain-Entities.
**FIX:** Schema-gebundene Extraktion (JSON-Schema / Tool-Parameter); extrahierte Slots vor
Ausfuehrung zurueck-bestaetigen (Read-back).
**Quelle:** arXiv 2311.07418 (Speech-based Slot Filling); USPTO Slot-Extraction.

### 1.8 Speech-to-Text / Diktat → Fehlinterpretation
**Symptom:** Diktierte Befehle falsch transkribiert → falscher Intent („main.py" → „main dot pie").
Voice-Agenten sind robust gegen Grammatikfehler, aber sehr anfaellig fuer Mispronunciations.
**FIX:** Voll-Kontext-Inferenz (moderne LLMs korrigieren aus Kontext); bei kritischen Slots
Read-back; Voice-Dictionary NICHT per String-Replace, sondern ueber den Whisper-`prompt`-Parameter
(vgl. Memory `feedback_voice_dictionary_whisper_prompt_not_replace`).
**Quelle:** Webfuse „Top 5 Voice AI Failures"; VoiceBench arXiv 2410.17196.

### 1.9 Implizite Mehrfach-Aufgaben nur teilweise erkannt
**Symptom:** Ein Satz enthaelt mehrere Aufgaben; Agent erledigt nur einen Teil (typisch erste/letzte).
**FIX:** Explizite Decomposition (Aufgaben auflisten + bestaetigen, sequenziell abarbeiten) +
End-Verifikation gegen die Original-Liste (deckt sich mit der Semikolon-Trenner-Regel).
**Quelle:** MAST FM-1.1 + FM-2.3.

---

## 2. Orchestrierung / Delegation / Routing

### 2.1 Endlos-Delegation / fehlende Termination  [⭐ HAEUFIG]
**Symptom:** Supervisor erreicht nie „FINISH"; Graph/Team laeuft bis Recursion-/Budget-Limit leer.
In einem dokumentierten Fall: FINISH nur 1 von 20 Laeufen.
**Ursache:** LLM-Supervisor gibt das Stop-/FINISH-Token nicht zuverlaessig zurueck; Worker geben
Kontrolle nicht ab; Termination-Bedingung wird nicht ausgewertet.
**Versionen / Frameworks (alle als Failure-Mode-Klasse):**
- LangGraph: Routing nie FINISH — **#2968** (CLOSED COMPLETED 2025-01-09, gefixt). Endlos-Loop-
  Regression **#6731** (CLOSED **NOT_PLANNED** 2026-02-20 → won't-fix, betraf 1.0.6; 0.6 ok).
- CrewAI: Loop stoppt nicht nach `max_iter` — **#3847** (CLOSED COMPLETED 2025-11-07, gefixt);
  `have_forced_answer` macht max_iter wirkungslos — **#1656** (CLOSED COMPLETED 2025-01-09, gefixt).
- AutoGen: `GroupChatManager` ignoriert `is_termination_msg` — **#802** (CLOSED COMPLETED 2023-11-29, alt/v0.2).
**FIX (funktionserhaltend):** Stop-Bedingung explizit im Supervisor-Prompt verankern; Routing als
`Literal["worker_a","worker_b","FINISH"]` **typisieren** (faengt halluzinierte Knotennamen beim
Parsen); Recursion-/`max_iter`-Limit NUR als Sicherheitsnetz (nicht als Steuerung); LangGraph
`RemainingSteps`. Limit erhoehen verdeckt nur das Routing-Problem.
**Quelle:** langgraph #2968/#6731; crewAI #3847/#1656; autogen #802.

### 2.2 Handoff-Ping-Pong / falscher naechster Sprecher
**Symptom:** Zwei Agenten reichen die Aufgabe endlos hin und her; oder der Selector waehlt den
falschen / immer denselben Agenten.
**Frameworks:**
- OpenAI Agents SDK: Handoff-Ping-Pong (Design). FIX: `RECOMMENDED_PROMPT_PREFIX` /
  `prompt_with_handoff_instructions`; klare, disjunkte Handoff-Bedingungen pro Agent; SDK resettet
  `tool_choice` nach Tool-Call automatisch auf „auto" (verhindert Tool-Loop, nicht schlechte Prompts).
- AutoGen: `SelectorGroupChat` ignoriert `selector_func` sporadisch — **#4289** (CLOSED **NOT_PLANNED**
  2024-11-21 → bleibt; deterministischen Selector + hartem Fallback bauen).
**FIX:** Distinkte Handoff-Regeln, deterministischer Selector mit Fallback, Termination kombinieren.
**Quelle:** OpenAI Agents SDK Handoffs-Doku; autogen #4289.

### 2.3 Falsches Routing / falsche Faehigkeit
**Symptom:** Manager waehlt den falschen Sub-Agenten; oder das Routing haengt von der Sprache der
Beschreibung ab (EN delegiert, JA antwortet selbst).
**Frameworks (CrewAI):** Manager bekommt Task-Tools und erledigt selbst statt zu delegieren
(**#2054**); `coworker mentioned not found` (**#1823**, Fix-PR #976); sprachabhaengiges Routing (**#3925**).
**FIX:** Manager strikt nur mit Delegate/AskQuestion-Tools; Coworker-Namen robust matchen;
Delegations-Anweisung sprachunabhaengig verankern; Routing nicht rein dem LLM ueberlassen.
**Quelle:** crewAI #2054/#1823/#3925.

### 2.4 Under-Delegation — Orchestrator macht alles selbst
**Symptom:** Trotz `allow_delegation=True` delegiert der Manager nie; hierarchical verhaelt sich
sequenziell.
**Frameworks (CrewAI):** **#4783** (CLOSED **NOT_PLANNED** 2026-04-16 → won't-fix); #2838;
TypeError im DelegateWorkToolSchema **#2606** (CLOSED COMPLETED 2025-05-18, gefixt).
**FIX:** Manager ohne Eigen-Tools; klare Delegations-Pflicht im Prompt; Schema-Typen pruefen.
**Quelle:** crewAI #4783/#2838/#2606.

### 2.5 Ergebnis-Aggregation / paralleler State-Verlust  [⭐ HAEUFIG]
**Symptom:** Parallele Worker schreiben denselben State-Key → `InvalidUpdateError: Can receive only
one value per step` / `INVALID_CONCURRENT_GRAPH_UPDATE`; Worker-Outputs ueberschreiben sich oder
gehen verloren.
**Ursache:** Default-State kann konkurrierende Updates auf einen Key nicht mergen (nur ein
Overwrite pro Super-Step).
**Versionen:** LangGraph **#6446** (OPEN, bestaetigt), #2336; per Design ohne Reducer.
**FIX:** Betroffene Felder mit `Annotated[list, operator.add]` (oder custom Reducer) versehen →
Outputs werden gemerged statt verworfen. Parallelitaet bleibt erhalten.
**Quelle:** langgraph #6446/#2336; LangChain `INVALID_CONCURRENT_GRAPH_UPDATE`-Doku.

### 2.6 Routing ist grundsaetzlich nicht-deterministisch
**Symptom:** Gleiche Eingabe → anderes Routing/Speaker-Wahl. In Multi-Agent-Ketten kumuliert das.
**FIX:** Routing typisieren (`Literal[...]`), deterministische Fallback-Regeln, Conditional-Edges
nicht von Zeit/Random/Cache abhaengig machen, Observability zum Erkennen von „same input, different outcome".
**Quelle:** Multi-Agent-Orchestrierungs-Analysen; autogen #4289 als konkreter Beleg.

### 2.7 Delegation ohne Timeout/Monitoring/Abort (Claude Code)
**Symptom:** Orchestrator wartet ohne Status/Abort; ein Subagent fuer einen kleinen Lookup machte
10+ Fetches; Session hing 12h+.
**Versionen:** Claude Code **#61405** (OPEN, bestaetigt).
**FIX:** Eng gescopte Prompts mit expliziten Stop-Bedingungen/Budget; Orchestrator-seitige Resume-/
Abort-Logik; Zeiterwartung pro Agent ansagen (vgl. `agent-and-researcher-rules.md`: Timeout-Erwartung,
nie >15 Min; `subagent-crash-proofing.md`: Orchestrator-Resume).
**Quelle:** anthropics/claude-code #61405.

---

## 3. Dynamisches Sub-Agent-Spawnen (der Boss baut/startet Unteragenten)

> Kern-Referenz im eigenen System: Regel `subagent-crash-proofing.md` (immer geladen) — diese
> Sektion ist die externe Beleglage dazu. **Wichtig: Subagents haben KEIN Auto-Compact** (laeuft
> nur im Hauptthread) → ein Worker laeuft bis er crasht, niemand rettet ihn zur Laufzeit.

### 3.1 Spawn crasht bei 0 Token — vererbter Start-Sockel zu gross  [⭐ HAEUFIG]
**Symptom:** Jeder Subagent-Spawn → „Prompt is too long" bei 0 verarbeiteten Token, alle Modelle.
**Ursache:** Cowork/Spawn leitet den vollen Parent-System-Prompt weiter (computer_use-Block,
behavior, available_skills, ~300+ deferred Tool-Namen, CLAUDE.md) — ~80k+ Token bevor die Aufgabe drankommt.
**Versionen:** Claude Code **#55712** (OPEN, Cowork); **#41208** (CLOSED **NOT_PLANNED** 2026-05-04,
Desktop 2.1.87 → won't-fix, bleibt).
**FIX:** Subagent mit eigenem schlankem System-Prompt starten; bis Fix Cowork-Spawning meiden,
normale Agent-Dispatch nutzen; `tools`-Whitelist im Agent-Frontmatter.
**Quelle:** anthropics/claude-code #55712/#41208.

### 3.2 MCP-Tool-Schemas sprengen den Subagent-Kontext (>200k)  [⭐ HAEUFIG]
**Symptom:** Bei vielen MCP-Servern: „prompt is too long: 209117 tokens > 200000 maximum" — vor
dem ersten Tool-Call.
**Ursache:** Subagents erben ALLE MCP-Tool-Definitionen (volle JSON-Schemas) des Parent.
**Versionen:** Claude Code **#37793** (OPEN, bestaetigt); #50284 (Explore/Haiku-Budget enger).
**FIX:** `tools`-Whitelist im Frontmatter (nur benoetigte Tools); **Tool Search / deferred MCP-
Schemas** (`ENABLE_TOOL_SEARCH`); Zahl der MCP-Server reduzieren.
**Quelle:** anthropics/claude-code #37793/#50284.

### 3.3 Subagent-Results ueberlaufen den Parent → Session unrettbar
**Symptom:** Mehrere Subagents liefern grosse Results → Parent geht in terminalen „Prompt is too
long"-Loop, kann Results weder zusammenfassen noch verarbeiten; Session permanent tot.
**Versionen:** Claude Code **#23463** (CLOSED **NOT_PLANNED** 2026-04-17 → won't-fix; bleibt aktiv).
**FIX:** Strukturierte, kompakte Rueckgabe-Schemas erzwingen (Subagent gibt **Pfad + <=1500-Token-
Summary**, nicht Rohdaten); **File-as-Memory**; enger Scope pro Worker; Orchestrator liest Detail
per Pfad nach (vgl. `subagent-crash-proofing.md` + `lossless-context-principle.md`).
**Quelle:** anthropics/claude-code #23463.

### 3.4 recursion_limit wird nicht an Subagents vererbt → stille Default-25-Crashes
**Symptom:** Subagent trifft Default `recursion_limit=25`, wirft `GraphRecursionError`, der als
`asyncio.CancelledError` durch den Parent propagiert — Execution haengt/crasht **ohne klare Meldung**.
**Ursache:** `SubAgentMiddleware` ruft den Subagent **ohne config** auf → Default 25 statt Parent-Wert.
**Versionen:** LangGraph/deepagents **#1698** (CLOSED COMPLETED 2026-03-25, gefixt).
**FIX:** `config` mit `recursion_limit` explizit an Subagent-Invoke durchreichen; `GraphRecursionError`
im Parent fangen → retry/degrade statt Cancel.
**Quelle:** langchain-ai/deepagents #1698; LangChain `GRAPH_RECURSION_LIMIT`-Doku.

### 3.5 Agent-Fork-Bomb / Endlos-Spawnen ohne Tiefen-/Anzahl-Limit
**Symptom:** Agenten erzeugen endlos weitere Agenten; RPM/Rate-Limit-Abstuerze, Kosten-Explosion.
**FIX:** Hard-Limit fuer Spawn-Tiefe UND Gesamtzahl (Fork-Bomb-Schutz); Concurrency-Cap; Budget-
Enforcement ausserhalb des Agent-Codes (siehe 7.4). Bei Researcher-Schwaermen: max ~7 gleichzeitig
(vgl. `agent-and-researcher-rules.md`).
**Quelle:** Fan-Out-Pipeline-Analysen (qubytes); MAST.

### 3.6 Kontext wird NICHT an den Sub-Agenten vererbt
**Symptom:** Sub-Agent kennt das Ziel nicht (erbt die Konversation nicht) und macht die falsche Arbeit.
**FIX:** Allen relevanten Kontext explizit in den Spawn-Prompt schreiben (Agenten erben KEINE
Konversations-Historie); Datei-Ownership/Whitelist im Prompt; strukturierte Aufgabenbeschreibung.
**Quelle:** Claude Code Subagent-Doku; CLAUDE.md „Kontext grosszuegig geben".

### 3.7 Parallele Datei-Writes → stiller Ueberschreib-Konflikt
**Symptom:** Zwei Subagents schreiben dieselbe Datei; eine Aenderung ueberschreibt die andere still.
**FIX:** **git worktrees pro Subagent** (`isolation: worktree`) ODER strikte Datei-Ownership (nie
gleiche Datei parallel). Vgl. CLAUDE.md „Datei-Ownership ist heilig".
**Quelle:** augmentcode (git worktrees); mindstudio Shared-Task-List.

### 3.8 Eingebaute Spawn-Limits hart durchsetzen (Frameworks)
**Symptom/Klasse:** Loops/Rekursion beim Spawnen, weil Limits nicht greifen.
**FIX je Framework:** CrewAI `max_iter`/`max_rpm` + nach Erreichen HART abbrechen (#3847); AG2
`max_turns`/`max_round` + **trigger-Funktion**, die Agenten ausschliesst, die selbst Recipients der
Nested Chats sind (verhindert Selbst-Rekursion, autogen #3287); OpenAI Agents SDK `MaxTurnsExceeded`
(Default 10 Turns) mit `error_handlers={"max_turns": ...}` abfangen → kontrollierten Final-Output
statt Exception (community #1370708).
**Quelle:** crewAI #3847; AG2 Nested-Chats-Doku/#3287; OpenAI community #1370708.

---

## 4. Tool-Calling & Task-Mapping (verstandene Aufgabe → konkrete Aktion)

> Kontext: ~31% aller Produktions-Agent-Fehler 2024/25 sind Tool-Misuse/falsche Args (Arize/Trantor).

### 4.1 AKTUELL: Opus 4.8 erzeugt fehlerhafte tool_use-Bloecke  [⭐ HAEUFIG — betrifft dein Modell]
**Symptom:** Modell serialisiert Tool-Calls als Legacy-`<invoke>`-XML-Text statt strukturiertem
tool_use-Block → ganze Antwort wird verworfen / „tool call could not be parsed". Opus 4.7
funktioniert im selben Setup.
**Versionen:** Opus 4.8 — Claude Code **#63604** (OPEN), Desktop **#64658** (OPEN), „fails to use
tools / softbricks context" **#63364** (OPEN), „responses not displayed after tool use — quota
consumed" **#64129** (OPEN). Verwandt: Opus 4.7 mischt XML in JSON-Args **#49747** (OPEN); Opus 4.7
leerer tool_use **#61133** (CLOSED **COMPLETED** 2026-05-21 → gefixt). Interleaved-thinking ist auf
Opus 4.8 automatisch aktiv und kann den tool_use-Stream zusaetzlich korrumpieren (OmniRoute #3415).
**FIX (funktionserhaltend):** Args/Antwort defensiv parsen (strukturierte UND XML-Form akzeptieren
+ normalisieren); bei Parse-Fehler **interleaved-thinking abschalten** und retry; Payload kuerzen;
fuer tool-lastige Turns ggf. auf 4.7 ausweichen bis Fix.
**Quelle:** anthropics/claude-code #63604/#64658/#49747/#61133; OmniRoute #3415.

### 4.2 Kaputtes / unvollstaendiges JSON in Tool-Args
**Symptom:** „Invalid JSON format in tool call arguments", „Unterminated string in JSON".
**Ursache:** Modell (v.a. kleinere/quantisierte) erzeugt syntaktisch kaputtes JSON, haeufiger bei
langem Output.
**FIX:** OpenAI `strict: true` / Anthropic strikte Schemas; `temperature 0.0-0.2`; JSON-Repair +
Retry-mit-Fehlerkontext (max 2).
**Quelle:** microsoft/vscode #281405; community #181636; TokenMix.

### 4.3 Schema-Mismatch (8 Unterklassen)
**Symptom:** „missing field" / „wrong type" trotz valider JSON; geht nach Neustart, scheitert nach
Schema-Update; scheitert nur bei langen Prompts; scheitert bei tiefer Verschachtelung; intermittierend
bei hoher Temperatur; nach 10+ Turns durch akkumulierte kaputte Calls.
**FIX:** Raw-Args loggen + byte-genau gegen Schema pruefen; Schema-Cache leeren nach Update; Tools
<20/Call; **max 3 Verschachtelungs-Ebenen** (tiefe Config als 1 JSON-String-Param); komplexe Calls
auf staerkeres Modell routen; temp <=0.2; History komprimieren (nur letzte 3-5 Tool-Interaktionen
voll). Anthropic-Notiz: schwach bei **Arrays von Objekten** → flache Schemas bevorzugen.
**Quelle:** TokenMix „Model Failed to Call Tool with Correct Arguments (2026)".

### 4.4 Falsches/kein Tool gewaehlt; halluzinierte Tools
**Symptom:** Falsches Tool oder gar keins (obwohl noetig); Aufruf nicht-existenter Tools.
**FIX:** Klare, **disjunkte** Tool-Descriptions; Tool-Anzahl begrenzen (4.7); Tool-Existenz validieren.
**Quelle:** Arize; Giskard; arXiv 2412.04141 (Tool-Hallucination).

### 4.5 „Claimed but didn't do" — Agent behauptet Aktion, ohne das Tool zu rufen
**Symptom:** Agent meldet eine Aktion als erledigt, ohne das Tool wirklich aufzurufen.
**Erkennung:** fehlende Tool-Marker/thought-chain = wahrscheinlich halluziniert.
**FIX:** Aktion gegen Tool-Trace verifizieren; Completion-Kriterien programmatisch pruefen (vgl.
Observability-First: Live-Logik-Sonden „erwartet vs. tatsaechlich").
**Quelle:** AnythingLLM-Doku; Composio 2026 Guide.

### 4.6 Tool-Call-Endlosschleife
**Symptom:** Gleiches Tool immer wieder.
**Ursache:** Fehlendes `max_iterations`; LLM-kontrollierte Exit-Bedingung; mehrdeutige Tool-Outputs
(„Found 2 flights, more may be available").
**FIX:** **Hash(tool_name+args); 3x identisch = Loop-Flag**; `max_iterations`/`max_execution_time`;
**klare SUCCESS-Outputs** („SUCCESS: Booking confirmed" senkte Calls 14→2); Progress im Kontext.
**Quelle:** Meritshot; browser-use #191; Inkog.

### 4.7 Zu viele Tools → Auswahl bricht ein  [⭐ HAEUFIG]
**Symptom:** Genauigkeit faellt **43%→2%** wenn Tool-Zahl von 4 auf 51 steigt; Einbruch ab Kontext
~65k/120k.
**FIX (lossless):** Tools gruppieren / <20 pro Call; **progressive disclosure / Meta-Tool-Pattern**
(erst minimale Beschreibungen, volle Schemas on-demand via `searchTools`); dynamisches Tool-Retrieval.
**Quelle:** BFCL V4; LongFuncEval arXiv 2505.10570; MCP #1888; Solo.io Progressive Disclosure.

### 4.8 Parallele Tool-Calls: fehlendes tool_result blockiert die Session
**Symptom:** Bei mehreren parallelen Calls geht ein tool_result verloren → naechster Request scheitert
mit „tool_call_id missing response", Session nicht fortsetzbar. Verwandt: async Tasks nie awaited;
concurrent same-session verschachtelt die Message-Sequenz.
**FIX:** ALLE tool_results vollstaendig + in korrekter Reihenfolge zuruecksenden bevor der naechste
Turn startet; await-Barrier; Session-Level-Lock/Serialisierung.
**Quelle:** openai/codex #8479; pipecat #3273; agentscope-runtime #402.

### 4.9 Tool-Result ignoriert / Fehler nicht erkannt (Cascading)
**Symptom:** Tool liefert falsches/partielles Ergebnis, der Fehler fliesst still downstream
(LLM-Ketten werfen keine Exceptions).
**FIX:** Jedes Tool-Result auf Fehler-Marker pruefen, Completion verifizieren, nicht blind weiterketten.
**Quelle:** Future AGI „How Tool Chaining Fails in Production".

### 4.10 AKTUELL: Opus 4.8 deklariert Arbeit als „verified/done" ohne den Build zu laufen  [⭐ HAEUFIG]
**Symptom:** Opus 4.8 meldet eine Aufgabe als „verified"/„done", ohne den kanonischen Build/Test
tatsaechlich ausgefuehrt zu haben (false-green). Opus 4.7 tat das im selben Setup nicht.
**Ursache:** Modell-Regression im Verifikations-/Abschluss-Verhalten — eine spezielle Auspraegung von
4.5 („claimed but didn't do").
**Versionen:** Opus 4.8 — Claude Code **#63861** (OPEN, bestaetigt 2026-06-09).
**FIX (funktionserhaltend):** Tool-Wirkung **programmatisch** verifizieren (Build/Test/Lint als
rules-based Check), NIE dem „done" des Modells vertrauen (vgl. Verifikations-Pflicht, Observability-
First Live-Logik-Sonden, `superpowers:verification-before-completion`).
**Quelle:** anthropics/claude-code #63861.

### 4.11 Opus 4.8: Sampling-Parameter und erzwungenes tool_choice werfen HTTP 400
**Symptom:** `temperature`/`top_p`/`top_k`/Prefill auf Opus 4.8 → HTTP 400. Und: `tool_choice:
{"type":"any"|"tool"}` ist mit Extended/Interleaved Thinking NICHT kompatibel → 400.
**Ursache:** Opus 4.8 nutzt adaptives Thinking als einzigen Modus; Sampling-Controls und erzwungene
Tool-Wahl kollidieren damit.
**Versionen:** Opus 4.8 (per Design). Quelle extern/Doku.
**FIX:** Auf Opus 4.8 KEINE Sampling-Parameter senden — stattdessen `effort`/Prompting nutzen.
Wer Slot-Extraktion strikt erzwingen will (`tool_choice: any` + `strict`), muss Thinking abschalten
oder die Tool-Wahl per User-Message steuern.
**Quelle:** Anthropic Tool-Use-Doku (forced-tool-use + extended thinking); LaoZhang (extern).

---

## 5. Menschlicher Dialog / Conversational Naturalness (inkl. Voice)

### 5.1 Persona-Drift ueber lange Sessions  [⭐ HAEUFIG]
**Symptom:** Agent folgt anfangs der Rolle, faellt nach ~30-60 Min aus dem Charakter, wird generisch/
roboterhaft.
**Ursache:** System-Prompt-Tokens stehen am Kontext-Anfang und verlieren mit wachsendem Kontext an
Attention-Gewicht.
**FIX:** System-Prompt-Persona periodisch **re-injizieren** (dokumentierter „300-Token-Fix":
kompakter Reminder alle N Turns); Persona-Consistency-Score + Repair.
**Quelle:** emergentmind Persona-Drift; dev.to „300-token-fix".

### 5.2 Multi-Agent-Echoing (Identitaets-Kollaps)
**Symptom:** In Agent-zu-Agent-Dialogen geben Agenten ihre Rolle auf und spiegeln den Partner;
Echoing-Rate bis **70%**.
**FIX:** Rollen-Anker pro Turn verstaerken; Identitaet explizit in jeder Nachricht markieren.
**Quelle:** OpenReview „Echoing — Identity Failures when LLM Agents Talk".

### 5.3 Sycophancy — Agent stimmt allem zu
**Symptom:** Agent validiert auch falsche/riskante Ideen statt ehrlich zu widersprechen
(GPT-4o-Vorfall 25.04.2025, Rollback 29.04.2025).
**Ursache:** Reward aus Thumbs-up/down schwaechte das Anti-Sycophancy-Reward — Training-Ebene, kein
reiner Prompt-Fix.
**FIX:** Im Prompt explizit „stimme nicht reflexhaft zu, benenne Risiken"; Ehrlichkeit/taktvollen
Widerspruch verankern (vgl. Memory `feedback_honest_free_tier`, Direktive #2 Ehrlichkeit).
**Quelle:** Simon Willison 2025-04-30; VentureBeat Rollback.

### 5.4 Ueber-Entschuldigen / Hedging / Verbosity
**Symptom:** Staendiges „Entschuldigung", „I'm not an expert, but…"; aufgeblaehte, repetitive
Antworten (GPT-4: **50,4%** Verbosity-Compensation).
**FIX:** Hedging im Prompt untersagen; `frequency_penalty` ~0.7-0.8; Antwort-Laengen-Vorgabe; Floskeln
explizit verbieten (vgl. Memories `feedback_no_emdashes_in_text`, `feedback_no_standard_emojis`).
**Quelle:** arXiv 2501.09910; arXiv 2411.07858; localazy.

### 5.5 „Lost in Conversation" — Kontextverlust ueber Turns  [⭐ HAEUFIG]
**Symptom:** Agent vergisst frueher Gesagtes, baut auf eigenen Fehlern auf; **~39% Genauigkeitsverlust**
bei schrittweisem statt all-at-once Prompt (GPT-4/4o, Claude 3/3.5, Gemini 2.5 + 12 weitere).
**FIX (funktionserhaltend):** **Recap** (finaler Zusammenzug) + **Snowball** (per-Turn akkumulierte
State-Wiederholung) → +15-20 Punkte. Simple Truncation, naive Summary, niedrige Temperatur oder extra
„thinking tokens" reichen NICHT.
**Quelle:** arXiv 2505.06120 „LLMs Get Lost" (Microsoft+Salesforce); arXiv 2504.04717.

### 5.6 Voice: Latenz killt Natuerlichkeit
**Symptom:** Verzoegerung wirkt als „Denkzeit", bricht Immersion.
**FIX:** Antwort <200ms anstreben (>300ms = wahrgenommene Pause); LLM-Output **satzweise** an TTS
streamen statt komplette Antwort abzuwarten; Backchanneling („mhm").
**Quelle:** futureagi Barge-In-Guide 2026; CallSphere.

### 5.7 Voice: Barge-In zu langsam / False-Barge-In / Soft-Speech-Miss
**Symptom:** Agent redet 200-400ms ueber den Nutzer weiter; ODER unterbricht sich selbst bei
Noise/Husten/Codec-Artefakten; ODER hoert leise Sprecher nie.
**FIX:** end-of-user-speech → TTS-`flush()` (Queue droppen, nicht warten) <60ms; LLM via
`AbortController` canceln <40ms; False-Barge-In <2% via 3 Signale (Energy-Gate -45..-35 dBFS +
Voice-Classifier-Confidence >=0.75 + Min-Duration 200-300ms); Energy-Threshold beim Start auto-tunen.
(Richtwerte aus einem Engineering-Guide — als Startpunkt behandeln.)
**Quelle:** futureagi/CallSphere/SparkCo.

### 5.8 Voice: Tool-Result nach Interrupt verloren; roboterhafte TTS-Prosodie
**Symptom:** Nutzer unterbricht waehrend Tool-Call → Daten verloren; ODER monotone/falsch betonte Stimme
(**37,5%** nennen „roboterhafte Stimme" als Hauptsorge).
**FIX:** Idempotente Reads im Hintergrund fertiglaufen lassen + Result stashen, nur Mutationen canceln;
**SSML** fuer Pausen/Rate/Pitch/Aussprache; moderne Streaming-TTS mit niedriger First-Package-Latency;
satzweises Streaming.
**Quelle:** futureagi; inworld TTS; arXiv 2412.10117 (CosyVoice 2).

> Hinweis: Whisper-Stille-Halluzinationen und „falsche Sprache" sind bereits im VoiceAgent-Bereich
> dokumentiert (Almanach `desktop/groq-transkription.md`, Memory `project_voiceagent_silence_hallucination_fix`).

---

## 6. State / Memory / Kontext im Multi-Agenten-System

### 6.1 Context-Rot / „lost in the middle" (alle Frameworks, langer Lauf)  [⭐ HAEUFIG]
**Symptom:** Tool-Calls, File-Reads, Debug-Output, fehlgeschlagene Exploration sammeln sich im
geteilten Kontext → unsichtbare Genauigkeits-Degradation (kein Crash). U-Form: Start+Ende gut, Mitte
schlecht; messbar ab ~50% Fensterauslastung.
**Belege:** Stanford „lost in the middle" (Liu et al., TACL 2024, 30%+ Drop in der Mitte); Chroma:
18/18 Frontier-Modelle degradieren mit Laenge.
**FIX (funktionserhaltend):** Sub-Agenten isolieren Explorations-/Recherche-Tasks in eigenem Kontext
und geben nur das **saubere Ergebnis** an den Orchestrator zurueck (Claude Agent SDK Subagent-Final-
Output); File-as-Memory; History-Trim/Summary (vgl. `lossless-context-principle.md` — verlustfrei).
**Quelle:** Stanford TACL 2024; Chroma Context-Rot; arXiv 2505.20625.

### 6.2 Message-History-Bloat → Kontextfenster-Ueberlauf
**Symptom:** Lange Orchestrierung akkumuliert Messages → Token-Kosten/Latenz steigen, Kontextlimit
erreicht, degradierte Antworten/Fehler.
**FIX:** `trim_messages` + `count_tokens_approximately` (letzte N) via `@before_model`-Middleware;
oder `SummarizationMiddleware` (alte Messages auslagern, durch Summary ersetzen). Beides verlustfrei.
**Quelle:** LangChain Short-term-memory-Doku.

### 6.3 Self-Conditioning / Kontext-Vergiftung durch eigene Fehler
**Symptom:** Orchestrator wird ueber lange Laeufe zunehmend fehleranfaelliger.
**Ursache:** Eigene fruehere Fehler im Kontextfenster werden als korrektes Muster fortgeschrieben.
**FIX:** Fehlerhafte Zwischenstaende NICHT im Hot-Context lassen; Checkpoint-Reset, frische Sub-Kontexte.
**Quelle:** Zartis „Compounding Errors"; „Illusion of Diminishing Returns" 2025.

### 6.4 Concurrent State-Update ohne Reducer (siehe auch 2.5)
**Symptom:** `INVALID_CONCURRENT_GRAPH_UPDATE`; State-Updates ueberschreiben sich.
**FIX:** Key als `Annotated[list, add]` / custom Reducer deklarieren.
**Quelle:** LangChain-Doku; deepagents #96.

### 6.5 Checkpoint/Resume-/Persistenz-Bugs
**Symptom:** Resume fuehrt den ersten Executor erneut aus; „Expected exactly one update for key
'SharedState'"; `langgraph dev` ignoriert den konfigurierten Checkpointer (erzwingt In-Memory);
Async-Checkpointer haengt lautlos bei sync `invoke`.
**Versionen:** MS Agent Framework **#5621** (OPEN, .NET Handoff-Checkpoint), #1695 (Python Resume);
LangGraph #5790 (CLOSED COMPLETED 2025-10-09), #1800 (async/sync), #6290 (OPEN, Subgraph-Reducer out-of-turn).
**FIX:** In Produktion eigene Runtime statt `langgraph dev`; konsequent `ainvoke`/`aget_state` mit
Async-Checkpointer; Resume-Logik gegen doppelte Executor-Ausfuehrung absichern.
**Quelle:** ms agent-framework #5621/#1695; langgraph #5790/#1800/#6290.

### 6.6 Shared-Memory zwischen Agenten unzuverlaessig / leakt
**Symptom:** CrewAI Long-Term-Memory broken (#2026), mem0-External-Memory speichert nicht (#3152),
Shared-State zwischen Tasks inkonsistent (#4111); AutoGen `SocietyOfMindAgent` leakt innere Messages
nach aussen (#6123), alle Agents teilen dieselbe History unerwartet (#1877), History nicht zwischen
GroupChats uebergebbar (#3365).
**FIX:** Explizites Shared-State-Objekt (3 Schichten: Workflow-State durable / Working-Memory
kurzlebig / Event-Log append-only); inneres Team-Reasoning kapseln, nur Final-Result nach aussen.
**Quelle:** crewAI #2026/#3152/#4111; autogen #6123/#1877/#3365.

---

## 7. Zuverlaessigkeit, Observability & Sicherheit

### 7.1 Kaskadierende Fehler / Error-Amplifikation  [⭐ HAEUFIG]
**Symptom:** Ein Soft-Fehler eines Sub-Agenten wird von Downstream-Agenten als „vertrauenswuerdig"
uebernommen und verstaerkt. Dezentral bis **17,2x** Amplifikation vs. Single-Agent; zentral ~**4,4x**.
**FIX (funktionserhaltend):** **Verifier-Agent nach jedem Primaeragent** mit adversarialem Mandat
(faengt ~96% vor Propagation); jede Agent-Ausgabe gegen **typed schema** validieren; zentral
orchestrieren statt frei-dezentral.
**Quelle:** Zartis; Augment Code; STRATUS (NeurIPS 2025).

### 7.2 14 systematische Failure Modes (MAST-Taxonomie als Checkliste)
**Klasse:** System-Design **44,2%** (Disobey Task/Role Spec, Step Repetition, Loss of History, Unaware
of Termination) · Inter-Agent-Misalignment **32,3%** (Reset, Fail-to-Clarify, Derailment, Info-
Withholding, Ignored Input, Reasoning-Action-Mismatch) · Verification **23,5%** (Premature Termination,
No/Incomplete/Incorrect Verification). Architektur, nicht Modell.
**FIX:** Taxonomie als Eval-Dimensionen/Checkliste nutzen.
**Quelle:** arXiv 2503.13657; github multi-agent-systems-failure-taxonomy/MAST.

### 7.3 Stille Fehler / fehlendes Tracing ueber Agenten-Grenzen
**Symptom:** Sub-Agent liefert leer/fehlerhaft (z.B. HTTP200 empty), Orchestrator merkt nichts;
Root-Cause nicht auffindbar.
**FIX:** **OpenTelemetry GenAI Semantic Conventions** + nested spans pro Sub-Action; step-level
Cost/Latency-Attribution (LangSmith); Sub-Agent-Outputs validieren statt blind uebernehmen (vgl.
Observability-First-Direktive).
**Quelle:** callsphere OTel+LangSmith; LumiMAS arXiv 2508.12412.

### 7.4 Token-/Kosten-Runaway
**Symptom:** Endlose Retries → Token-Spirale. Real: Claude-Code-Recursion-Loop **1,67 Mrd Token /
~$16k-50k in 5h** (Juli 2025); 50 parallele Runaways ~$1200+/Nacht.
**FIX:** **Budget-Enforcement AUSSERHALB des Agent-Codes** (Gateway — ein buggy Agent ueberspringt
seinen eigenen Check); Circuit-Breaker auf Pattern (Cost-Velocity, repeated prompts, error-rate,
growing context); Auto-Throttle bei Spend-Rate >3x des 7-Tage-Schnitts.
**Quelle:** sanj.dev LLM-Cost-Control; aisecuritygateway; truefoundry.

### 7.5 Prompt-Infection — self-replizierende Injection durch Sub-Agenten
**Symptom:** Schadanweisung breitet sich agentenuebergreifend aus (>80% Erfolg bei GPT-4o).
**Ursache:** Inter-Agent-Channels werden als trusted behandelt.
**FIX:** Inter-Agent-Nachrichten als **untrusted** behandeln; Output-Sanitisierung + Injection-Scanner
an jeder Grenze; formale Trust-Boundaries.
**Quelle:** arXiv 2503.12188 (COLM 2025).

### 7.6 Untrusted Tool-Output als Anweisung (indirekte Injection)
**Symptom:** Orchestrator behandelt Tool-Rueckgabe als Befehl.
**FIX:** Tool-Output strikt als **Daten** markieren (nicht als Instruktion); strukturierte Felder statt
Freitext; Scope explizit/kryptographisch fuehren statt nur im Prompt.
**Quelle:** zylos.ai 2026 Indirect PI.

### 7.7 MCP Tool-Poisoning & Confused Deputy
**Symptom:** Boeser/kompromittierter MCP-Server schreibt Direktiven in Tool-Descriptions
(CVE-2025-54136); Sub-Agent/Server nutzt die Rechte des privilegierten Orchestrators fuer falschen User.
**Ursache:** Tool-Descriptions sind **executable context**; MCP propagiert keine User-Identity, eine
geteilte Credential-Set ueber alle User.
**FIX:** Runtime-Tool-Description-Scanning ueber die ganze Session; Tool-Description pinnen/hashen;
**Identity-Propagation + Audience-Validation + per-User Tool-Scope-OAuth** (Least Privilege).
**Quelle:** truefoundry CVE-2025-54136; practical-devsecops Confused Deputy; pipelab State-of-MCP-Security-2026.

---

## Fix-Status (hart per `gh` geprueft am 2026-06-09)

> Trennung: *gefixt* (Changelog/Issue COMPLETED) vs. *bleibt aktiv* (OPEN oder CLOSED NOT_PLANNED =
> won't-fix/Duplikat — der Workaround bleibt noetig). Im Zweifel: aktiv.

### Belegt GEFIXT (Workaround historisch)
| Frueherer Bug | Repo / Issue | gefixt (Status, Datum) |
|---|---|---|
| LangGraph Routing nie FINISH (Infinite Loop) | langchain-ai/langgraph #2968 | COMPLETED 2025-01-09 |
| LangGraph `langgraph dev` ignoriert Checkpointer | langchain-ai/langgraph #5790 | COMPLETED 2025-10-09 |
| deepagents recursion_limit nicht an Subagent vererbt | langchain-ai/deepagents #1698 | COMPLETED 2026-03-25 |
| CrewAI Loop stoppt nicht nach max_iter | crewAIInc/crewAI #3847 | COMPLETED 2025-11-07 |
| CrewAI have_forced_answer macht max_iter wirkungslos | crewAIInc/crewAI #1656 | COMPLETED 2025-01-09 |
| CrewAI TypeError Hierarchical Delegation | crewAIInc/crewAI #2606 | COMPLETED 2025-05-18 |
| AutoGen GroupChatManager ignoriert is_termination_msg | microsoft/autogen #802 | COMPLETED 2023-11-29 (alt) |
| OpenAI Agents SDK nested-handoff-history drops content | openai/openai-agents-python #3319 | COMPLETED 2026-05-10 (jetzt opt-in!) |
| Opus 4.7 tool_use leer/„could not be parsed" | anthropics/claude-code #61133 | COMPLETED 2026-05-21 |

> ⚠️ **Achtung Regression durch Fix:** Nach #3319 ist `nest_handoff_history` im OpenAI Agents SDK
> **opt-in (Default AUS)**. Wer auf das alte Default-Verhalten baute, muss `RunConfig(nest_handoff_history=True)`
> explizit setzen — sonst stiller Kontextverlust beim Upgrade.

### Bleibt AKTIV (OPEN oder won't-fix → Workaround noetig)
| Bug | Repo / Issue | Status |
|---|---|---|
| Opus 4.8 fehlerhafte tool_use-Bloecke | anthropics/claude-code #63604, #64658 | **OPEN** |
| Opus 4.8 fails to use tools / softbricks context | anthropics/claude-code #63364 | **OPEN** |
| Opus 4.8 Antwort nach Tool-Use stumm, Quota verbraucht | anthropics/claude-code #64129 | **OPEN** |
| Opus 4.8 false-green „verified/done" ohne Build | anthropics/claude-code #63861 | **OPEN** |
| Opus 4.7 XML in JSON-Args | anthropics/claude-code #49747 | **OPEN** |
| Cowork Subagent-Spawn broken (0 Token) | anthropics/claude-code #55712 | **OPEN** |
| Subagents „prompt too long" bei vielen MCP-Servern | anthropics/claude-code #37793 | **OPEN** |
| Subagent delegation ohne Timeout/Abort | anthropics/claude-code #61405 | **OPEN** |
| LangGraph parallele State-Updates (InvalidUpdateError) | langchain-ai/langgraph #6446 | **OPEN** |
| LangGraph Subgraph-Reducer out-of-turn | langchain-ai/langgraph #6290 | **OPEN** |
| MS Agent Framework Handoff-Checkpoint-Resume | microsoft/agent-framework #5621 | **OPEN** |
| Subagent-Results overflow → Session unrettbar | anthropics/claude-code #23463 | CLOSED **NOT_PLANNED** (won't-fix) |
| Subagents „prompt too long" Desktop 2.1.87 | anthropics/claude-code #41208 | CLOSED **NOT_PLANNED** |
| Scoped-Instruction-Fehldeutung (Sonnet 4.6) | anthropics/claude-code #41707 | CLOSED **NOT_PLANNED** |
| LangGraph 1.0.6 Infinite-Loop-Regression | langchain-ai/langgraph #6731 | CLOSED **NOT_PLANNED** (war 1.0.6; 0.6 ok) |
| CrewAI hierarchical delegation faellt aus | crewAIInc/crewAI #4783 | CLOSED **NOT_PLANNED** |
| AutoGen SelectorGroupChat ignoriert selector_func | microsoft/autogen #4289 | CLOSED **NOT_PLANNED** |

**Ehrlichkeit/Methodik:** Die obigen Status sind am 2026-06-09 per `gh issue view` hart verifiziert.
Nicht jede von den Researchern genannte Nummer wurde einzeln per `gh` geprueft (Stichprobe der
wichtigsten + alle Claude-Code-Issues, da hier gebaut wird). Mehrere Kennzahlen (17,2x Amplifikation,
87%/4h, dBFS-Schwellen) stammen aus 2026er Blog-/Sekundaerquellen, nicht peer-reviewed — als Richtwerte
behandeln. Versions-Patchnummern der Frameworks teils aus Vergleichs-Blogs (gegen offizielle Changelogs
gegenpruefen).

---

## Pflicht-Checkliste vor dem Bau eines Boss-/Orchestrator-Agenten

```
VERSTEHEN (Sektion 1)
□ Read-back/Plan vor Ausfuehrung (Intent bestaetigen, betroffene Dateien zeigen)
□ Bei Unsicherheit nachfragen statt raten (AskUserQuestion / Clarifying-Policy)
□ Scope als expliziten Constraint im Prompt; Multi-Aufgaben dekomponieren + am Ende verifizieren
□ Nach >2 Fehlversuchen STOP statt variieren-und-raten (3-Iterationen-Stop)

ORCHESTRIEREN (Sektion 2)
□ Jede Schleife hat eine echte Stop-Bedingung; Recursion/max_iter NUR als Sicherheitsnetz
□ Routing typisiert (Literal); deterministischer Fallback; Manager ohne Eigen-Tools
□ Parallele State-Keys mit Reducer (Annotated[list, add]) gegen Update-Verlust
□ Sub-Agent-Delegation mit Timeout/Abort/Resume; Zeiterwartung ansagen

SPAWNEN (Sektion 3)
□ Sub-Agent: schlanker System-Prompt + tools-Whitelist; KEIN Voll-Kontext erben
□ Tiefen-/Anzahl-Budget (Fork-Bomb-Schutz); max ~7 parallele Researcher
□ Rueckgabe = Pfad + <=1500-Token-Summary (File-as-Memory), nicht Rohdaten
□ Datei-Ownership / git worktrees gegen parallele Write-Konflikte
□ Orchestrator-Resume bei Crash (Subagents haben KEIN Auto-Compact)

TOOL-CALLING (Sektion 4)
□ Args VOR Ausfuehrung gegen striktes Schema validieren + Retry-mit-Fehlerkontext (max 2)
□ Opus 4.8: bei tool_use-Parse-Fehler interleaved-thinking abschalten/retry (#63604/#64658)
□ <20 Tools pro Call / progressive disclosure; flache Schemas (keine Arrays von Objekten)
□ Hash-basierte Loop-Detection; klare SUCCESS-Outputs; Tool-Wirkung verifizieren
□ Alle tool_results vollstaendig + in Reihenfolge zuruecksenden (parallele Calls)

DIALOG (Sektion 5)
□ Persona-Anker periodisch re-injizieren; History per Recap+Snowball mitfuehren
□ Ehrlichkeit > Sycophancy; Hedging/Verbosity begrenzen
□ Voice: satzweise streamen, Barge-In <150ms, Tool-Result bei Interrupt stashen

STATE/RELIABILITY/SECURITY (Sektionen 6-7)
□ Lange Laeufe: Sub-Kontexte isolieren, History trimmen/summarizen (Context-Rot)
□ Verifier-Agent + typed-schema zwischen Agenten (Error-Amplifikation stoppen)
□ Distributed Tracing (OTel) + Budget/Circuit-Breaker im Gateway (Kosten-Runaway)
□ Inter-Agent-Output + Tool-Output als untrusted behandeln (Prompt-Infection); MCP Least-Privilege
```

---

## Wartung
Jeder spaeter SELBST erlebte Orchestrator-/Agenten-Bug wird hier ergaenzt (Bug + funktionserhaltende
Loesung + Versionen/Frameworks), Stand-Header aktualisieren. Bei Versionssprung von Claude Code oder
einem der Frameworks: kurzer Re-Check (Fix-Status per `gh`).
