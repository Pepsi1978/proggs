# Orchestrator-/Boss-Agent — Best Practices (Stand 2026-06-09)

> Die **Praeventions-Seite** zum Bug-Almanach `bugs/agents/orchestrator-agent.md`. Der Almanach
> sagt *was schiefgeht und wie man es umgeht*; diese Datei sagt *wie man einen Haupt-/Boss-/
> Orchestrator-Agenten von vornherein so baut, dass die Bugs gar nicht erst entstehen*.
> Vor der Arbeit beide lesen — **erst Almanach, dann Best Practices**.
> Anker: Claude Code 2.1.169 (Opus 4.x) + LangGraph/CrewAI/AutoGen/OpenAI-Agents-SDK/Claude-Agent-SDK (Juni 2026).
> Quell-Flag pro Empfehlung: `offiziell` = Hersteller-Doku, `extern` = Community/Paper/Blog.

---

## Grundhaltung: Verstehen, Delegieren, Verifizieren — in dieser Reihenfolge

Ein Boss-Agent, der „alles versteht und alles kann", entsteht NICHT durch ein gigantisches
Modell oder einen riesigen System-Prompt, sondern durch **Struktur um das Modell herum**: einen
schmalen, klaren Orchestrator, der (1) die Absicht zuerst absichert, (2) eng gescopte Spezialisten
delegiert und (3) deren Ergebnisse verifiziert, bevor er sie als Wahrheit nimmt. Die empirische
Basis (MAST, arXiv 2503.13657, `extern`): MAS scheitern zu **44% an System-Design** und **32% an
Inter-Agent-Misalignment** — also an der Architektur, fast nie am Modell.

---

## 1. Intent zuerst absichern (bevor irgendetwas passiert)

| Praxis | Warum | Flag |
|---|---|---|
| **Plan/Read-back vor Ausfuehrung**: Agent formuliert verstandenes Ziel + betroffene Artefakte, dann erst handeln | Faengt ueber-woertliche/-liberale Auslegung (Bug 1.3/1.4) | offiziell (Claude Code Plan-Mode) |
| **Bei Unsicherheit nachfragen statt raten** — Multiple-Choice (AskUserQuestion) statt offener Frage | niedrige Reibung, faengt Fail-to-Clarify (1.2) | offiziell |
| **Scope als expliziten Constraint** in den Prompt („NUR Datei X") | gegen Scope-Creep (1.4) | extern (CC #41707) |
| **Multi-Aufgaben dekomponieren** + am Ende gegen die Original-Liste verifizieren | gegen teilweises Erledigen (1.9) | extern (MAST) |
| **3-Iterationen-Stop**: nach >2 Fehlversuchen anhalten statt variieren-und-raten | gegen Reasoning-Action-Mismatch-Spiralen (1.6) | extern + Hausregel |
| **Schema-gebundene Slot-Extraktion** (JSON-Schema) statt freie NL-Args | gegen falsche Parameter (1.7) | extern |
| Fixe A-vs-B-Logik per **Regel statt LLM** routen | spart Latenz, vermeidet Misclassification (1.1) | extern (A2A #773) |

---

## 2. Orchestrierung: schmaler Supervisor, typisiertes Routing, harte Termination

| Praxis | Warum | Flag |
|---|---|---|
| **Routing typisieren** (`Literal["a","b","FINISH"]`) | faengt halluzinierte Knotennamen beim Parsen (2.1) | offiziell (LangGraph) |
| **Echte Stop-Bedingung im Prompt**; Recursion/`max_iter` NUR als Sicherheitsnetz | gegen Endlos-Delegation (2.1) | offiziell |
| **Manager ohne Eigen-Tools**, nur Delegate/AskQuestion | erzwingt Delegation statt Selbst-Erledigen (2.3/2.4) | extern (CrewAI) |
| **Parallele State-Keys mit Reducer** (`Annotated[list, operator.add]`) | gegen Update-Verlust bei Fan-Out (2.5) | offiziell (LangGraph) |
| **Distinkte Handoff-Bedingungen** + `RECOMMENDED_PROMPT_PREFIX` | gegen Handoff-Ping-Pong (2.2) | offiziell (OpenAI Agents SDK) |
| **Zentral orchestrieren** statt frei-dezentrales Mesh | begrenzt Error-Amplifikation 17x→4,4x (7.1) | extern (Zartis) |
| **Timeout/Abort/Resume + Zeiterwartung** pro Sub-Agent ansagen | gegen 12h-Hangs (2.7) | Hausregel (`agent-and-researcher-rules`) |

---

## 3. Sub-Agenten bauen/spawnen: schlanker Sockel, enger Scope, Resume

| Praxis | Warum | Flag |
|---|---|---|
| **`tools`-Whitelist im Agent-Frontmatter** (nur was gebraucht wird) | gegen MCP-Schema-Overflow >200k (3.2) | offiziell (CC) |
| **Tool Search / deferred MCP-Schemas** (`ENABLE_TOOL_SEARCH`) | schlanker Start-Sockel (3.1/3.2) | offiziell |
| **Schlanker eigener System-Prompt** statt Voll-Kontext erben | gegen 0-Token-Spawn-Crash (3.1) | extern (CC #55712) |
| **Rueckgabe = Pfad + <=1500-Token-Summary** (File-as-Memory) | gegen Parent-Overflow (3.3) | Hausregel (`subagent-crash-proofing`) |
| **Allen Kontext explizit in den Spawn-Prompt** (Agenten erben keine Historie) | gegen „Sub-Agent kennt Ziel nicht" (3.6) | offiziell (CC) |
| **Tiefen-/Anzahl-Budget** (Fork-Bomb-Schutz); max ~7 parallele Researcher | gegen Endlos-Spawnen/RPM-Crash (3.5) | Hausregel |
| **git worktrees / Datei-Ownership** | gegen parallele Write-Konflikte (3.7) | offiziell |
| **Orchestrator-Resume bei Crash** (Subagents haben KEIN Auto-Compact) | Aufgabe ueberlebt Worker-Crash (3.3) | Hausregel |

---

## 4. Tool-Calling robust machen

| Praxis | Warum | Flag |
|---|---|---|
| **Args VOR Ausfuehrung gegen striktes Schema validieren** + Retry-mit-Fehlerkontext (max 2) | faengt kaputtes JSON + Schema-Mismatch (4.2/4.3) | extern (TokenMix) |
| **Opus 4.8**: bei tool_use-Parse-Fehler interleaved-thinking abschalten + retry; defensiv XML+JSON akzeptieren | aktuelle Regression #63604/#64658 | extern (CC) |
| **<20 Tools pro Call / progressive disclosure** | gegen 43%→2%-Auswahleinbruch (4.7) | extern (BFCL) |
| **Flache Schemas** (max 3 Ebenen, keine Arrays von Objekten bei Anthropic) | gegen Deep-Nesting-Fehler (4.3) | extern |
| **temperature <=0.2** in Produktion | gegen intermittierende Arg-Fehler (4.3) | extern |
| **Hash-Loop-Detection** (3x gleicher Call = Flag) + **klare SUCCESS-Outputs** | gegen Tool-Loops (4.6) | extern |
| **Tool-Wirkung verifizieren** (nicht „claimed but didn't do" glauben) | gegen 4.5 | extern (Composio) |
| **Alle tool_results vollstaendig + in Reihenfolge** zuruecksenden | gegen Session-Block bei parallelen Calls (4.8) | extern (codex #8479) |

---

## 5. Menschlicher Dialog: aktiv gepflegt, nicht „eingestellt"

| Praxis | Warum | Flag |
|---|---|---|
| **Persona-Anker periodisch re-injizieren** (300-Token-Reminder alle N Turns) | gegen Persona-Drift (5.1) | extern |
| **History per Recap + Snowball mitfuehren** (nicht nur truncaten) | gegen „Lost in Conversation" ~39% (5.5) | extern (arXiv 2505.06120) |
| **Ehrlichkeit > Sycophancy** im Prompt verankern | gegen Zustimmungs-Bias (5.3) | extern |
| **Hedging/Verbosity begrenzen** (frequency_penalty, Laengen-Vorgabe, keine Floskeln/Em-Dashes) | natuerlicher Ton (5.4) | Hausregel (`feedback_no_emdashes_in_text`) |
| **Voice: satzweise streamen** an TTS; Antwort <200ms | gegen Latenz-Bruch (5.6) | extern |
| **Voice: Barge-In <150ms** (TTS-flush + AbortController), 3-Signal-VAD | gegen Drueberreden/False-Barge-In (5.7) | extern |
| **Voice: Tool-Result bei Interrupt stashen**, nur Mutationen canceln | gegen Datenverlust (5.8) | extern |
| **SSML** fuer Prosodie/Aussprache | gegen roboterhafte Stimme (5.8) | offiziell |

---

## 6. State, Reliability, Security

| Praxis | Warum | Flag |
|---|---|---|
| **Lange Laeufe: Sub-Kontexte isolieren**, nur sauberes Ergebnis zurueck | gegen Context-Rot (6.1) | offiziell (Claude Agent SDK) |
| **History trimmen/summarizen** verlustfrei (auslagern, nicht wegwerfen) | gegen Bloat-Overflow (6.2) | offiziell + Hausregel (`lossless-context-principle`) |
| **Fehlerhafte Zwischenstaende NICHT im Hot-Context** lassen | gegen Self-Conditioning (6.3) | extern |
| **Verifier-Agent + typed-schema zwischen Agenten** | stoppt Error-Amplifikation (7.1) | extern (STRATUS) |
| **Distributed Tracing (OTel GenAI) + nested spans** | gegen stille Fehler (7.3) | offiziell |
| **Budget/Circuit-Breaker im Gateway** (ausserhalb Agent-Code) | gegen Kosten-Runaway (7.4) | extern |
| **Inter-Agent- und Tool-Output als untrusted** behandeln (Daten, nicht Instruktion) | gegen Prompt-Infection/indirekte Injection (7.5/7.6) | extern (COLM 2025) |
| **MCP Least-Privilege**: per-User Tool-Scope, Identity-Propagation, Tool-Description pinnen | gegen Tool-Poisoning/Confused-Deputy (7.7) | extern (CVE-2025-54136) |

---

## Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach-Abschnitt

| Best-Practice (hier) | Bug-Almanach-Abschnitt (`bugs/agents/orchestrator-agent.md`) |
|---|---|
| 1. Intent zuerst absichern | 1. Intent-Verstehen (1.1–1.9) |
| 2. Orchestrierung/Routing/Termination | 2. Orchestrierung/Delegation/Routing (2.1–2.7) |
| 3. Sub-Agenten bauen/spawnen | 3. Dynamisches Sub-Agent-Spawnen (3.1–3.8) |
| 4. Tool-Calling robust | 4. Tool-Calling & Task-Mapping (4.1–4.9) |
| 5. Menschlicher Dialog | 5. Conversational Naturalness (5.1–5.8) |
| 6. State/Reliability/Security | 6. State/Memory/Kontext + 7. Reliability/Observability/Security |
