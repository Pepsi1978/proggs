# Orchestrator-/Boss-Agent — Best Practices (Stand 2026-07-02)

> Die **Praeventions-Seite** zum Bug-Almanach `bugs/agents/orchestrator-agent.md`. Der Almanach
> sagt *was schiefgeht und wie man es umgeht*; diese Datei sagt *wie man einen Haupt-/Boss-/
> Orchestrator-Agenten von vornherein so baut, dass die Bugs gar nicht erst entstehen*.
> Vor der Arbeit beide lesen — **erst Almanach, dann Best Practices**.
> Quell-Flag pro Empfehlung: `offiziell` = Hersteller-Doku (Anthropic/OpenAI/Framework), `extern` = Community/Paper/Blog.
> Aktualisiert per Best-Practices-Recherche (7 Researcher, offizielle Quellen zuerst) am 2026-06-09, Re-Recherche 2026-07-02.
> Update 2026-07-02: Keine neuen belegten Framework-Bugs seit dem Stand; weiter auf einfache Single-Agent-Loops, typisierte Zustandsuebergaenge, Tool-Budget und programmgesteuerte Verifikation setzen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Boss-Agent neu bauen | Erst einfach (Single-Agent + gute Tools), Multi-Agent nur bei Bedarf | §Grundhaltung |
| 2 | Intent absichern | Plan/Read-back vor Aktion; bei Unsicherheit AskUserQuestion | §1 |
| 3 | Mehrdeutige Anfrage | Staerkstes Modell (Opus) + Scope als Constraint | §1 |
| 4 | Nach >2 Fehlversuchen | STOP statt variieren-und-raten (3-Iterationen-Stop) | §1 |
| 5 | Routing aufsetzen | Typisieren (`Literal[...]`); echte Stop-Bedingung, Limit nur Netz | §2 |
| 6 | Parallele State-Keys | Reducer (`Annotated[list, operator.add]`) gegen Update-Verlust | §2 |
| 7 | Sub-Agent spawnen | tools-Whitelist + schlanker Prompt; nur Pfad+1-2k-Summary zurueck | §3 |
| 8 | Worker-Crash moeglich | Orchestrator-Resume (Subagents haben KEIN Auto-Compact) | §3 |
| 9 | Tool-Calling | Detaillierte Descriptions, <20 Tools/Turn, `strict:true`, Args validieren | §4 |
| 10 | Opus 4.8 | KEINE Sampling-Parameter; robustes Parsen fuer legacy-XML tool_use | §4 |
| 11 | „done"-Meldung des Modells | Tool-Wirkung rules-based verifizieren (Build/Lint/Test) | §4 |
| 12 | Langer Dialog/Voice | Persona re-injizieren; Recap+Snowball; satzweise streamen, Barge-In <60ms | §5 |
| 13 | Reliability/Security | Verifier-Agent + typed-schema; Budget/Circuit-Breaker im Gateway | §6 |
| 14 | Fremd-/Tool-Content | Nur in `tool_result`, als untrusted; MCP Least-Privilege | §6 |
| 15 | From-scratch Loop | `stop_reason` pruefen; max_iterations+Timeout; tool_use/result-Paare halten | §7a |

---

## Frameworks / Versionen (Stand Juni 2026)

| Framework | Version | Notiz |
|---|---|---|
| Claude Code | 2.1.169 | Agent-Tool (frueher „Task", umbenannt v2.1.63), Workflow-Tool, Subagents |
| Claude Agent SDK | aktiv | Subagents mit isolierten Kontexten, `maxTurns`/`maxBudgetUsd`, Memory-Tool, Context-Editing |
| LangGraph | ~1.0 / 0.4 | Supervisor/Subgraph/Checkpointer; `Annotated`-Reducer |
| CrewAI | ~0.105+ | sequential/hierarchical Process |
| AutoGen / AG2 | 1.0 GA (Maintenance) | Magentic-One; Dev wandert zu MS Agent Framework |
| Microsoft Agent Framework | aktiv (Py+.NET) | sequential/concurrent/handoff/group-chat/magentic + Checkpointing |
| OpenAI Agents SDK | ~0.7 | handoffs, agents-as-tools, Guardrails, `nest_handoff_history` (opt-in) |

---

## Grundhaltung: Erst einfach, dann eskalieren — Verstehen, Delegieren, Verifizieren

Ein Boss-Agent, der „alles versteht und alles kann", entsteht NICHT durch ein riesiges Modell oder
einen Mega-System-Prompt, sondern durch **Struktur um das Modell herum**. Anthropics zentrale
Empfehlung (`offiziell`): **mit dem Einfachsten anfangen** — Single-Agent mit guten Tools — und Multi-
Agent/Sub-Agenten NUR hinzufuegen, wenn es nachweislich besser ist. Multi-Agent kostet ca. **15x mehr
Tokens** als ein Einzel-Agent und lohnt nur bei breiter, **parallelisierbarer** Arbeit, deren Info ein
Kontextfenster sprengt. Empirisch (MAST, `extern`) scheitern Multi-Agent-Systeme zu **44% an System-
Design** und **32% an Inter-Agent-Misalignment** — fast nie am Modell.
Quellen: Anthropic „Building Effective Agents", „How we built our multi-agent research system" (`offiziell`).

---

## 1. Intent zuerst absichern (bevor irgendetwas passiert)

Der zuverlaessigste Pfad „versteht alles + tut genau das Gemeinte" ist eine Kette:
**detaillierte Tool-Beschreibungen + schema-strenge Slot-Extraktion → Multiple-Choice-Klaerfragen statt
Raten → Plan/Akzeptanzkriterien vor Aktion zurueckspiegeln → Intent-Routing statt Mega-Prompt →
Guardrails/Stop-Conditions gegen Scope-Drift.**

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Plan/Read-back vor Ausfuehrung**: erst Kontext explorieren + Fragen stellen, dann Plan zur Freigabe zurueckspiegeln | „gather requirements before making changes" — Anforderungen werden geklaert statt geraten | Claude Agent SDK Plan-Mode (`offiziell`) |
| **Sprint-Contract**: Generator+Evaluator handeln „done" aus, BEVOR gebaut wird | vage Spec → messbares Ziel; verhindert „falsche Loesung" | Anthropic „Demystifying evals" (`offiziell`) |
| **Bei Unsicherheit nachfragen** via `AskUserQuestion` (1–4 Fragen, je 2–4 Optionen, `header`<=12 Zeichen) | strukturierte Wahl ist klick- statt tippbar, niedrige Reibung, eindeutig. **Grenze:** NICHT in Subagents verfuegbar → der Orchestrator muss selbst fragen | Claude Agent SDK user-input (`offiziell`) |
| **Staerkstes Modell (Opus) fuer mehrdeutige Anfragen** | „handles multiple tools better and seeks clarification"; schwaechere Modelle raten Slots statt nachzufragen | Claude Tool-Use-Doku (`offiziell`) |
| **Scope als expliziten Constraint** („NUR Datei X"; „what parameters it must operate within") | gegen Scope-Creep („macht zu viel") | Anthropic Building-Effective-Agents (`offiziell`) |
| **Multi-Aufgaben dekomponieren** (Orchestrator-Worker) + am Ende gegen Original-Liste verifizieren | flexibel fuer Aufgaben, deren Teilschritte vorab unbekannt sind | Anthropic (`offiziell`) |
| **3-Iterationen-Stop**: nach >2 Fehlversuchen anhalten statt variieren-und-raten | gegen Reasoning-Action-Mismatch-Spiralen | Hausregel |

---

## 2. Orchestrierung: erst Topologie waehlen, dann typisiertes Routing + harte Termination

**Workflow vs. Agent (zuerst entscheiden):** Ist der Ausfuehrungspfad vorhersehbar → **Workflow**
(LLM/Tools ueber vordefinierte Code-Pfade, vorhersagbar/konsistent). Pfad nicht hardcodierbar →
**Agent** (LLM steuert selbst, flexibel). Aufgabenteilung (Anthropic Research-System): LLM fuer
Planung/Strategie/Graceful-Degradation; deterministischer **Code** fuer State-Persistenz, Checkpoints,
Retry, Artifact-Storage. (`offiziell`)

**Topologie-Patterns — wann welches:**
- **Orchestrator-Worker / Supervisor**: Lead plant, zerlegt dynamisch, delegiert, synthetisiert. Best
  wenn Subtasks NICHT vorhersehbar. Supervisor macht kaum Annahmen ueber Sub-Agenten → flexibel, auch
  fuer Third-Party-Agenten. (`offiziell`)
- **Routing**: Input klassifizieren → spezialisierter Handler. Best bei klar trennbaren Kategorien.
- **Parallelization** (Sectioning/Voting): unabhaengige Subtasks gleichzeitig oder gleiche Aufgabe
  mehrfach fuer Robustheit.
- **Prompt Chaining**: feste, sauber zerlegbare Schritte in Reihe.
- **Swarm/Handoff**: schlaegt Supervisor leicht + spart Token (kein „Telefon"-Translation), verlangt
  aber dass jeder Agent alle anderen kennt (LangChain-Benchmark, `offiziell`).
- **Magentic-One** (AutoGen) als Stop-/Replan-Design: Outer-Loop **Task-Ledger** (Plan+Fakten),
  Inner-Loop **Progress-Ledger** (Self-Reflection; bei Stillstand neu planen). (`offiziell`)

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Routing typisieren** (`Literal["a","b","FINISH"]`) | faengt halluzinierte Knotennamen beim Parsen | LangGraph (`offiziell`) |
| **3-Tier-Routing-Cascade**: Regel (10–50ms) → semantisch/Embedding (50–200ms) → LLM (500–2000ms), Exit bei Confidence >=0.8 | billigste Methode zuerst; die meisten Requests in ms, harte Faelle genau | extern |
| **Echte Stop-Bedingung im Prompt**; Recursion/`max_iter` NUR als Sicherheitsnetz | gegen Endlos-Delegation | Anthropic (`offiziell`) |
| **Manager ohne Eigen-Tools**, nur Delegate/AskQuestion | erzwingt Delegation statt Selbst-Erledigen | CrewAI (`extern`) |
| **Parallele State-Keys mit Reducer** (`Annotated[list, operator.add]`) | gegen Update-Verlust bei Fan-Out (InvalidUpdateError) | LangGraph (`offiziell`) |
| **Distinkte Handoff-Bedingungen** + `RECOMMENDED_PROMPT_PREFIX` | gegen Handoff-Ping-Pong | OpenAI Agents SDK (`offiziell`) |
| **Zentral orchestrieren** statt frei-dezentrales Mesh | begrenzt Error-Amplifikation 17,2x → 4,4x | extern (Zartis/TDS) |
| **Timeout/Abort/Resume + Zeiterwartung** pro Sub-Agent | gegen 12h-Hangs | Hausregel |

---

## 3. Sub-Agenten bauen/spawnen: schlanker Sockel, enger Scope, Resume

**Pattern-Wahl:** *Agent-as-Tool* (Boss behaelt Kontrolle ueber die finale Antwort, Spezialist liefert
nur einen gebundenen Subtask zu — entspricht dem Claude-Code-`Agent`-Tool) vs. *Handoff* (Routing ist
selbst Teil des Workflows, Spezialist uebernimmt die naechste Interaktion) vs. *Subgraph* (parallele
Tasks mit isoliertem State + Namespace-Isolation). Kombinierbar: Triage-Handoff → Spezialist ruft
andere als Tools. (OpenAI Agents SDK, LangGraph — `offiziell`/`extern`)

**Kontext-Isolation (das Kernprinzip, offiziell belegt):** Ein Sub-Agent erbt NUR seinen eigenen
System-Prompt + den Agent-Tool-Prompt-String + Project-CLAUDE.md + Tool-Definitionen. Er erbt NICHT:
Parent-Conversation-History, Parent-Tool-Ergebnisse, Parent-System-Prompt, vorgeladene Skills. **Der
EINZIGE Kanal Parent→Sub ist der Prompt-String** — alle Pfade/Fehler/Entscheidungen explizit
hineinschreiben. (Claude Agent SDK „Subagents" — `offiziell`)

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **`tools`-Whitelist im Frontmatter** (Read-only=`Read,Grep,Glob`; Test=`Bash,Read,Grep`; Mod=`Read,Edit,Write,Grep,Glob`) | weniger Risiko + schlankerer Start-Sockel; ohne Whitelist erbt der Sub ALLE Tools inkl. MCP | Claude Agent SDK (`offiziell`) |
| **Modell pro Sub-Agent** (`opus` fuer High-Stakes, sonst guenstiger) + `maxTurns`/`effort` | Kosten/Stabilitaet gezielt | Claude Agent SDK (`offiziell`) |
| **Rueckgabe = 1.000–2.000-Token destillierte Summary** (+ Pfad / Artifact), nicht Rohdaten | Sub arbeitet im eigenen Fenster (zehntausende Token), Parent bleibt schlank | Anthropic Research-System (`offiziell`) |
| **File-as-Memory / Artifact-Pattern**: Zwischenergebnisse in Dateien, gezielt zurueckholen | Ordner-Struktur ist Context-Engineering; entlastet den Koordinator | Claude Agent SDK Blog (`offiziell`) |
| **3–5 Sub-Agenten parallel** spawnen (nicht seriell), Sub nutzt 3+ Tools parallel | bis zu **90% weniger Zeit** bei komplexen Queries | Anthropic Research-System (`offiziell`) |
| **Sub-Agent-Brief**: Objective + Output-Format + Tool/Quellen-Guidance + klare Boundaries | vage Briefs → Duplikate/Luecken/verfehlte Quellen | Anthropic Research-System (`offiziell`) |
| **Datei-Ownership / git worktrees** pro Sub-Agent | gegen parallele Write-Konflikte | offiziell |
| **Orchestrator-Resume** via `agentId`-Trailer + `resume: sessionId` (Sub-Transkripte ueberleben Compaction) | Aufgabe ueberlebt Worker-Crash (Subagents haben KEIN Auto-Compact) | Claude Agent SDK (`offiziell`) + Hausregel |
| **Skalierung**: 1 Agent / 3–10 Calls fuer Fakten; 2–4 fuer Vergleiche; >10 nur fuer komplexe Recherche | frueher Fehler: 50 Subagents fuer triviale Query | Anthropic Research-System (`offiziell`) |

**Wichtige offizielle Detail-Fakten (fuer den eigenen Harness relevant):**
- **Sub-Agenten koennen KEINE eigenen Sub-Agenten spawnen** — `Agent` nicht in deren `tools`. Orchestrator spawnt, Worker arbeiten.
- **Bei dutzenden/hunderten Agenten**: NICHT turn-by-turn-Subagents, sondern das **Workflow-Tool** (Orchestrierung als Script ausserhalb des Conversation-Kontexts).
- **Windows-Falle**: sehr lange Inline-Sub-Agent-Prompts scheitern am 8191-Zeichen-Command-Line-Limit → filesystem-Agenten (`.claude/agents/`) statt langer Inline-Prompts.

---

## 4. Tool-Calling robust machen

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Extrem detaillierte Tool-Beschreibungen** (>=3–4 Saetze: was, wann/wann nicht, Parameter, Limits; `input_examples`) | „by far the most important factor in tool performance" | Anthropic „Writing tools for agents" (`offiziell`) |
| **Wenige, konsolidierte Tools** (`action`-Parameter statt create/review/merge), Service-Namespacing (`github_list_prs`) | „Fewer, more capable tools reduce selection ambiguity" | Anthropic (`offiziell`) |
| **<20 Tools zum Turn-Start**; Rest via Tool-Search / **„code execution with MCP"** (Tools als Code-Dateien, on-demand gelesen) | **98,7% Token-Reduktion** im Beispiel (150k→2k); gegen 43%→2%-Auswahleinbruch | Anthropic „Code execution with MCP", OpenAI function-calling (`offiziell`) |
| **`strict: true`** + `additionalProperties:false` + alle Felder `required` (optionale via `null`-Type) | Tool-Calls folgen verlaesslich dem Schema statt „best effort" | OpenAI Structured Outputs (`offiziell`) |
| **Args VOR Ausfuehrung validieren** + 3-Stufen-Check (Refusal → Length/Truncation → schema-konform) | faengt malformte Args frueh, graceful Error-Handling | OpenAI (`offiziell`) |
| **Natuerlichsprachliche Identifier statt roher UUIDs** zurueckgeben | Agent halluziniert weniger, versteht Kontext | Anthropic (`offiziell`) |
| **Alle Tool-Results vollstaendig + in Reihenfolge** zuruecksenden bevor naechster Request | Modell synthetisiert konsistent; gegen „tool_call_id missing response" | OpenAI (`offiziell`) |
| **Parallel deaktivieren bei striktem Structured-Output / fester Reihenfolge** (`parallel_tool_calls:false` / `disable_parallel_tool_use:true`) | Structured Outputs ist NICHT kompatibel mit parallelen Calls | OpenAI/Anthropic (`offiziell`) |
| **Klare SUCCESS-Signale** aus Tools (`SUCCESS: Booking HT79265 confirmed`) statt ambig | Modell erkennt Abschluss, stoppt Retries (senkte Calls 14→2) | extern |
| **Hard Backstop**: `maxTurns` (zaehlt Tool-Use-Turns) + `maxBudgetUsd`; Termination ueber `subtype` pruefen, nicht ueber Text | zuverlaessige „fertig vs. abgebrochen"-Erkennung | Claude Agent SDK (`offiziell`) |
| **Tool-Wirkung rules-based verifizieren** (Build/Lint/Test), nie dem „done" vertrauen | gegen „claimed but didn't do" + Opus-4.8-false-green | Anthropic (`offiziell`) |
| **Opus 4.8: KEINE Sampling-Parameter** senden (temperature/top_p/top_k/Prefill → HTTP 400); robustes Parsen fuer legacy-XML tool_use | aktuelle Regression #63604/#63364/#64129/#63861 | extern + CC-Issues |

---

## 5. Menschlicher Dialog: aktiv gepflegt, nicht „eingestellt"

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Persona im System-Prompt** (Rolle/Ton/Vokabular, konversationelle Sprache) + periodisch **re-injizieren** | stabiler, menschlicher Ton ueber viele Turns; gegen Persona-Drift | Anthropic „keep in character" (`offiziell`) |
| **Ehrlichkeit aktiv promten** („don't be a sycophant, no softening, name the flaw") | Default-Gefaelligkeit muss ueberschrieben werden; glaubwuerdiger | Anthropic Prompting + Claude-4-System-Prompt (`offiziell`) |
| **Verbosity ueber Ton steuern, NICHT hart kappen** (Anthropic nahm Wort-Hardlimits zurueck — senkten Qualitaet) | natuerlich-knapp ohne Substanzverlust | Anthropic April-Postmortem (`offiziell`) |
| **Recap + Snowball** (per-Turn Anforderungen rekapitulieren); bei wichtigen Pfaden **Concat-and-Retry** | gegen „Lost in Conversation" (~39% Verlust); Concat bringt Top-Modelle >90% zurueck | arXiv 2505.06120 (`extern`) |
| **Off-Track → neue Session mit konsolidiertem Summary** statt endlos weiter-klaeren | bricht die Unreliability-Spirale | extern |
| **Voice-Architektur waehlen**: Speech-to-Speech (natuerlich, <500ms) vs. chained STT→LLM→TTS (auditierbar, Compliance) | Match an Use-Case | OpenAI Voice-Agents (`offiziell`) |
| **Latenz-Budget**: e2e <1s (chained) / <500ms (S2S); Audio 16-bit PCM 24kHz; Inference + Media ko-lokalisieren | Gespraech fuehlt sich natuerlich an | OpenAI/LiveKit (`offiziell`/`extern`) |
| **Satzweise an TTS streamen**, Mindest-Satzlaenge erzwingen, Rest am Stream-Ende flushen; `previous/next text` fuer Prosodie | niedrige First-Audio-Latenz, natuerliche Prosodie | Pipecat/ElevenLabs (`offiziell`) |
| **Barge-In**: AEC clientseitig Pflicht; bei User-Speech sofort `response.cancel` + Client-Buffer leeren; TTS-Stop <60ms | natuerliches Unterbrechen; >60ms wirkt wie Ignorieren | OpenAI Realtime (`offiziell`) |
| **Model-based Turn-Detection** (SmartTurn/TurnDetector) statt VAD allein; Silero lokal, `stop_secs` 0.2, `start_secs` tunen | erkennt echte Turn-Grenzen, unterbricht nicht mitten im Gedanken | Pipecat/LiveKit (`offiziell`/`extern`) |
| **SSML** (breaks/emphasis/prosody) + Pronunciation-Dictionary; TTS-TTFB <200ms | korrekte Aussprache von Eigennamen, keine Roboterstimme | ElevenLabs (`offiziell`) |
| **Tools am RealtimeAgent wie beim Text-Agent**; Session managed Interrupt mid-tool-call | Tool-Ergebnisse fliessen nahtlos in den Sprachfluss | OpenAI Voice-Agents (`offiziell`) |

---

## 6. State, Reliability, Evals & Security

**Context-Engineering (Anthropic 3-Primitive-Diagnose, `offiziell`):** erst diagnostizieren, dann
waehlen — **Clearing** (billig, fuer re-fetchbare Tool-Outputs, `clear_tool_uses_20250919`),
**Compaction** (Primaerstrategie fuer Dialog/Reasoning, `compact_20260112`), **Memory-Tool**
(Cross-Session, `memory_20250818`); Header `context-management-2025-06-27`. Benchmark: **84% Token-
Ersparnis, +39% Performance**. Sub-Agent-Isolation (eigenes Fenster, nur 1–2k-Summary zurueck)
validiert die Hausregeln `lossless-context-principle` + `subagent-crash-proofing` 1:1.

**Evals (Anthropic „Demystifying evals", `offiziell`):** Eval-Datasets aus echten Prod-Fehlern (20–50
Tasks reichen zum Start). Zwei Suites: **Capability** (niedrige Pass-Rate, treibt Verbesserung) +
**Regression** (~100%, faengt Degradation bei Modell-Upgrades). **Outcome/State graden, nicht die
Schritt-Sequenz** (Agent darf valide Umwege finden); Konsistenz via pass@k / pass^k. Grader mischen
(code-based deterministisch, model-based mit Rubrik+„Unknown"-Escape, human nur Kalibrierung).

| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Sub-Kontexte isolieren** (nur sauberes Ergebnis zurueck) + **History trim/summarize verlustfrei** | gegen Context-Rot / „lost in the middle" | Anthropic Context-Engineering (`offiziell`) |
| **Checkpointing/Persistence** je Deployment (InMemory/Sqlite/Postgres), `thread_id` als Namespace, Idempotenz beim Resume | zuverlaessiges Resume ohne doppelte Ausfuehrung | LangGraph (`offiziell`) |
| **Verifier-/Critic-Agent** mit **drei verschiedenen Modellen** (Generator/Critic/Judge) + deterministischer Watchdog | gegen Error-Amplifikation (bis 17,2x); gleiches Modell reviewt sonst nur sich selbst | extern (MindStudio/TDS) |
| **OTel GenAI nested spans**: `invoke_agent` → `chat` → `execute_tool`; step-level Cost via `gen_ai.usage.*` + Cache-Token | einheitliches Debugging von Handoffs/Tool-Calls (Conventions noch experimentell) | OpenTelemetry (`offiziell`) |
| **Budget/Circuit-Breaker im GATEWAY** (Spend-Caps, 402 bei Cap; Trigger: cost-velocity/repeated-prompts/error-rate/growing-context) | Observability ist PASSIV — stoppt Loops NICHT; Enforcement muss in die Infra | LangChain/MLflow/TrueFoundry (`offiziell`/`extern`) |
| **Guardrails mit Tripwire** (Input vor Ausfuehrung, Output nach finaler Ausgabe; `run_in_parallel=False` bei Side-Effects) | blockiert vor dem teuren Modell, kein Token-/Tool-Verbrauch bei Verstoss | OpenAI Agents SDK (`offiziell`) |
| **Human-in-the-Loop** bei destruktiven/finanziellen Aktionen — volle Tool-Call-Parameter zeigen (keine Summary), nie Auto-Approve in Multi-Server | gegen Confused-Deputy + stille Schadensaktionen | OWASP MCP / OpenAI (`offiziell`) |
| **Fremd-/Tool-Content NUR in `tool_result`-Bloecke**, gelabelt als untrusted; eigene Instruktionen NIE in tool_results | Claude unterscheidet untrusted Content zuverlaessig von Instruktionen | Anthropic Mitigate-Jailbreaks (`offiziell`) |
| **Tool-Description-Pinning** (SHA-256 ueber name+description+schema, vor jedem Call re-hashen) | stoppt Rug-Pull/Tool-Poisoning (OWASP ASI01) | OWASP MCP (`offiziell`) |
| **Identity-Propagation** (`<user_id>:<session_id>`), short-lived Credentials, JIT-Access, Kontext zwischen Tasks wipen | gegen Confused-Deputy / Privilege-Bleed | OWASP MCP / CSA (`offiziell`) |
| **Least-Privilege OAuth-Scopes** pro MCP-Server (`mail.readonly` statt `full_access`), nie Credentials zwischen Servern teilen | begrenzt Blast-Radius eines kompromittierten Servers | OWASP MCP (`offiziell`) |

---

## 7. Externe / selbst gebaute Boss-Agenten (eigene App, ausserhalb Claude Code)

> Die Sektionen 1–6 gelten plattform-/framework-uebergreifend. Hier die Praxis fuer Agenten, die du
> SELBST in einem eigenen Tool baust (z.B. VoiceAgent in C#/.NET) — framework-basiert ODER from-scratch.

### 7a. From-scratch Agent-Loop gegen rohe LLM-APIs (das VoiceAgent-Muster)
| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Loop = `while stop_reason == "tool_use"`**: Tool ausfuehren → `tool_result` im naechsten user-Turn → wiederholen; Loop endet bei jedem anderen `stop_reason` | kanonisches minimales Muster, kein Framework noetig | Anthropic „How tool use works" (`offiziell`) |
| **Hartes `max_iterations` (10–25) + Wall-Clock-Timeout (30–60s) + Loop-Erkennung** (gleicher Call+Args) | gegen Endlosschleife/Kosten — du baust den Stop selbst | Anthropic Building-Effective-Agents (`offiziell`) |
| **`stop_reason` nach JEDER Antwort pruefen**; bei `max_tokens` NICHT parsen → `max_tokens` hoch + retry | abgeschnittenes tool_use-JSON nie ans Tool geben | Anthropic „Handling stop reasons" (`offiziell`) |
| **tool_use/tool_result-Paare beim Trimmen zusammen halten**; `tool_result` = erstes content-Item im user-Turn; jede ID genau ein Result; Modell-IDs durchreichen (nie neu vergeben) | gegen „400: tool_use without tool_result" und Duplicate-ID-Fehler | claude-code #8004/#29598/#21089 (`extern`) |
| **SSE**: `partial_json` pro content_block-Index akkumulieren, erst bei `content_block_stop` parsen; nach `\n\n` trennen | gegen mid-token-JSON-Crash | Anthropic Streaming (`offiziell`) |
| **Tool-Fehler als `tool_result` mit `is_error:true`** (kein Auto-Retry!); **Instruktionen NIE in tool_result** | Modell korrigiert sich selbst; Injection-Schutz | Anthropic „Troubleshooting tool use" (`offiziell`) |
| **OpenAI Responses-API**: `function_call_output` mit passender `call_id`, alle parallelen Outputs gemeinsam senden; stateful-by-default spart eigenes State-Mgmt | sauberes Loop-Design | OpenAI (`offiziell`) |
| **429 in derselben Iteration mit Backoff retrien**, Iterations-Counter nur bei Erfolg hoch | keine „falsch verbrauchten" Iterationen | extern |

### 7b. C#/.NET (Semantic Kernel / Microsoft Agent Framework)
| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **MS Agent Framework (1.0 GA 2026-04-02) fuer NEUE .NET-Projekte**; SK fuer Bestandscode | „SK v2", kombiniert Agent-Abstraktion + Workflows | learn.microsoft.com (`offiziell`) |
| **Magentic-Orchestrierung** fuer offene Aufgaben (`MagenticWorkflowBuilder`) + `.WithMaxRounds(10).WithMaxStalls(3).WithMaxResets(2)` | Boss plant/delegiert/synthetisiert mit hartem Budget | MS Agent Framework (`offiziell`) |
| **`.RequirePlanSignoff(false)` setzen** wenn kein HITL gewuenscht (.NET-Default ist `true`!) | sonst pausiert der Workflow ungewollt | MS Agent Framework .NET (`offiziell`) |
| **`FunctionChoiceBehavior.Auto()/.Required()/.None()`**; Funktionen explizit per Liste begrenzen | `.Required()` wird nur im 1. Request advertised → kein Loop | Semantic Kernel (`offiziell`) |
| **Parallele Tool-Calls**: `AllowParallelCalls=true` UND `AllowConcurrentInvocation=true` (beide!) | sonst sequentiell | Semantic Kernel (`offiziell`) |
| **Provider-agnostisch via `Microsoft.Extensions.AI`** (`IChatClient` + `.UseFunctionInvocation().UseOpenTelemetry()`) | ein Client fuer OpenAI/Anthropic/Azure + Telemetrie als Middleware | learn.microsoft.com (`offiziell`) |
| **`[Description]` auf jede Function + jeden Parameter** | steuert Tool-Auswahl | SK/Extensions.AI (`offiziell`) |
| **SK >= 1.71.0** (CVE-2026-25592); `AgentGroupChat.IsComplete=false` vor Reuse; ResponseFormat NICHT mit Function Calling kombinieren; Migration-Guide vor Updates | bekannte .NET-Fallen (alle gefixt, aber Version/Reihenfolge beachten) | SK-Issues/CVE (`offiziell`) |

### 7c. Voice-First Boss-Agent (Orchestrierung im Gespraech)
| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Supervisor-Pattern statt Handoff**: Boss behaelt Session, delegiert an kurzlebige Tasks mit **typed results** (Dataclasses), State via `chat_ctx` | kein Agent-Swap-Overhead, keine Routing-Latenz, kein fragiles NLP-Parsen | LiveKit (`offiziell`) |
| **Lange Tool-Calls async** (`cancel_on_interruption=False`, `timeout_secs`, Zwischenstaende `is_final=False`) + **Result-Stashing** (Platzhalter-Output sofort in Kontext) | Gespraech friert nicht ein; Tool-Result bei Interrupt nicht verloren | Pipecat (`offiziell`) |
| **Kurze Preambles/Filler direkt vor dem Tool-Call** („ich schaue nach") — kein Chain-of-Thought vorlesen | wirkt responsiv, senkt empfundene Latenz | OpenAI Realtime-Prompting (`offiziell`) |
| **Routing als Klartext-Regeln + Beispielphrasen** (nicht Pseudocode); Clarification nur bei niedriger Confidence | Realtime-Modell folgt natuerlicher Sprache zuverlaessiger; jede Rueckfrage kostet einen Turn | OpenAI/LiveKit (`offiziell`) |
| **Bei Handoff den parallelen Reply-Task NICHT starten**; Preemptive-Gen blockieren solange Tool in-flight | gegen Handoff-Race + halluziniertes Tool-Ergebnis | livekit #5150 / agents-js #1365 (`extern`) |
| **>20 Exchanges: async zusammenfassen** vor komplexen Multi-Task-Workflows; Persona frueh „verriegeln" | gegen Token-Explosion + Persona-Drift (Voice hat keinen visuellen Kontext) | LiveKit (`offiziell`) |

### 7d. Lokale / Multi-Provider Boss-Agenten
| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Ollama `num_ctx` pro Request explizit setzen** (Default 2048/4096) | gegen stille Context-Truncation im Loop | Ollama-Docs (`offiziell`) |
| **LiteLLM/Gateway als Provider-Abstraktion** + getrennte `context_window_fallbacks` vs. `fallbacks` | ein Codepfad; lokal-zu-klein → Auto-Eskalation zur Cloud statt Hard-Fail | LiteLLM (`offiziell`) |
| **Router: lokal/billig zuerst, Eskalation bei Bedarf** (RouteLLM ~95% GPT-4-Qualitaet bei 26% teurer Calls) | Kosten runter ohne Qualitaetsverlust | RouteLLM arXiv (`extern`) |
| **Tool-Arg-Format pro Provider normalisieren** (OpenAI=JSON-String, Anthropic/Google=Objekt) auf EINE Struktur | haeufigster Migrationsbug | extern |
| **Tool-faehiges Modell waehlen** (Llama 3.1 8B-Instruct/Mistral 7B); kleine/quantisierte brechen Tool-Calling; vLLM hat kein `strict` → Schema im Orchestrator validieren | weniger fehlerhafte Tool-Calls | Ollama/vLLM-Docs (`offiziell`) |

### 7e. JavaScript / TypeScript (Vercel AI SDK / Mastra / OpenAI Agents JS)
| Praxis | Warum / Vorteil | Quelle (Flag) |
|---|---|---|
| **Loop-Kontrolle via `stopWhen: stepCountIs(n)`** (Default 20 Runaway-Schutz), nicht altes `maxSteps` | praeziser, kombinierbarer Stopp | ai-sdk.dev (`offiziell`) |
| **`prepareStep`** fuer Kontext-Pruning + Modell-Routing pro Step | haelt lange Loops im Token-Limit | ai-sdk.dev (`offiziell`) |
| **Tools mit Zod-`inputSchema` + `execute`** (v6: `inputSchema`, nicht `parameters`!) | typsicher; Tool ohne `execute` = Handoff-Signal | ai-sdk.dev (`offiziell`) |
| **`abortSignal` durchreichen + Cleanup in `onAbort`** (nicht `onFinish`); Abort NICHT mit resumable streams | saubere Terminierung bis zur API | ai-sdk.dev (`offiziell`) |
| **Mastra** fuer deterministische Graph-Orchestrierung (`.then()/.branch()/.parallel()`); **OpenAI Agents JS** `handoff` vs. `agent.asTool()` (braucht Zod v4) | explizite Kontrolle statt reines LLM-Loop-Steuern | mastra.ai / OpenAI (`offiziell`) |

---

## Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach-Abschnitt

| Best-Practice (hier) | Bug-Almanach-Abschnitt (`bugs/agents/orchestrator-agent.md`) |
|---|---|
| 1. Intent zuerst absichern | 1. Intent-Verstehen (1.1–1.9) |
| 2. Orchestrierung/Routing/Termination | 2. Orchestrierung/Delegation/Routing (2.1–2.7) |
| 3. Sub-Agenten bauen/spawnen | 3. Dynamisches Sub-Agent-Spawnen (3.1–3.8) |
| 4. Tool-Calling robust | 4. Tool-Calling & Task-Mapping (4.1–4.11) |
| 5. Menschlicher Dialog | 5. Conversational Naturalness (5.1–5.8) |
| 6. State/Reliability/Evals/Security | 6. State/Memory/Kontext + 7. Reliability/Observability/Security |
| 7. Externe / selbst gebaute Boss-Agenten (7a from-scratch · 7b .NET · 7c Voice · 7d lokal/Multi-Provider · 7e TS/JS) | 8. Externe / selbst gebaute Boss-Agenten (8.1–8.5) |

## Wichtigste Quellen (offiziell)
- Anthropic: „Building Effective Agents", „How we built our multi-agent research system",
  „Writing effective tools for agents", „Code execution with MCP", „Effective context engineering",
  „Demystifying evals for AI agents", „Mitigate jailbreaks", „keep Claude in character".
- Claude Agent SDK: Subagents, Agent-Loop, user-input (AskUserQuestion), Tool-Use/strict.
- OpenAI: Agents SDK (Orchestration/Handoffs/Guardrails), function-calling, Structured Outputs, Voice-Agents/Realtime.
- LangGraph: Subgraphs, Short-term-memory, INVALID_CONCURRENT_GRAPH_UPDATE, Checkpointer.
- OpenTelemetry GenAI Semantic Conventions; OWASP MCP Security Cheat Sheet / Agentic Top 10.
