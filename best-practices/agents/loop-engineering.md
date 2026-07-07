# Loop Engineering — Best Practices (Stand 2026-06-24, Runde 1+2)

> Die **Praeventions-Seite** zum Bug-Almanach `bugs/agents/loop-engineering.md`. Der Almanach
> sagt *was in autonomen Agenten-Loops schiefgeht und wie man es umgeht*; diese Datei sagt
> *wie man einen autonom in Schleifen laufenden Agenten von vornherein so baut, dass er
> reibungslos, guenstig und mit echtem Lerneffekt laeuft*. Vor der Arbeit beide lesen —
> **erst Almanach, dann Best Practices**.
>
> Quell-Flag pro Empfehlung: `offiziell` = Hersteller-/Paper-Primaerquelle (Anthropic/OpenAI/arXiv),
> `extern` = Community/Blog/Praxisbericht.
> Eng verwandt: `best-practices/agents/orchestrator-agent.md` (Boss-/Orchestrator-Agent) und
> `best-practices/server/ai-agent-frameworks.md` (Frameworks). Diese Datei fokussiert auf den
> **Loop selbst** (das WIE-laeuft-die-Schleife), nicht auf Multi-Agent-Routing.
>
> Aufgebaut per Firecrawl+MiniMax-Recherche am 2026-06-24: Runde 1 (5 Researcher) + Runde 2
> (5 Researcher, Luecken/Folgefragen): Reflexion-Mechanik, MemGPT/Letta, Rate-Limit-Resilienz,
> Container-Hardening/Sandboxing, Eval-Harness/Definitions-of-done.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektuere
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Loop-Agent neu bauen | Nicht der Prompt ist das Werk, sondern der **Harness** drumherum (Trigger, Stop, Feedback, Fehler-Kontrolle) | §1 |
| 2 | Loop-Architektur waehlen | Mit ReAct (Perceive→Reason→Plan→Act→Observe) starten; weitere Patterns nur bei Bedarf | §2 |
| 3 | Abbruch sicherstellen | IMMER 3 harte Grenzen: max Iterationen + max Token + max Wall-Time (Bounded Execution) | §3 |
| 4 | „Modell hat aufgehoert" | **Terminal-Message ≠ Ziel erreicht** — Harness prueft Goal separat (Predikat), nicht nur „keine Tool-Calls mehr" | §3 |
| 5 | Stagnation/Festhaengen | Circuit Breaker: gleicher Tool-Call 3x / Oszillation / kein Fortschritt in N Zyklen → loggen, stoppen, Mensch | §3 |
| 6 | Hoechste Zuverlaessigkeit | **Externer, deterministischer Validator** (Compiler/Linter/Tests) als Exit, nicht Selbst-Urteil (Ralph-Loop) | §2,§4 |
| 7 | Lange Laeufe | Context pro Iteration **resetten** ODER per Pointer auslagern — sonst silent Degradation | §4,§5 |
| 8 | State zwischen Iterationen | State **ausserhalb des Modells** persistieren (Datei/Repo/Key-Value) — „the repo doesn't forget" | §4 |
| 9 | Lerneffekt ueber Loops | **ACE grow-and-refine**: Learnings als wachsendes Playbook, inkrementelle Delta-Updates, NICHT neu schreiben | §5 |
| 10 | Learnings verdichten | Schutz vor **Brevity-Bias** (zu viel kuerzen) + **Context-Collapse** (Umschreiben erodiert Details) | §5 |
| 11 | Selbstbetrug verhindern | **Maker ≠ Judge** trennen; **„no receipt, no claim"**; Outcomes statt Completions bewerten | §6 |
| 12 | Goal-Drift | Goal-Restatement alle N Steps + Progress-Contract (Pflicht-Output-Schema) + Max-Step-Budget | §6 |
| 13 | System-Prompt im Loop | Dynamisch pro Iteration zusammensetzen: Basis-Prompt + akkumulierte „lessons" aus dem State | §4 |
| 14 | 24/7 auf VPS | „Container uptime ≠ Agent uptime": Persistent Workspace, Restart-Semantik, Logs+Replay, Resource-Limits, Human-Override | §7 |
| 15 | Kosten kontrollieren | Reasoning-Effort runter wo moeglich, Tool-Allowlist pro Task-Typ, Cost-per-successful-Outcome messen | §6,§7 |
| 16 | Iteratives Selbst-Lernen | Reflexion: Actor/Evaluator/Self-Reflection; verbale Reflexion als „semantic gradient" in episodic memory der naechsten Episode | §5 |
| 17 | Memory ueber Context-Limit | MemGPT/Letta: Core/Recall/Archival (RAM/Disk), recursive summarization, Sleep-Time-Compute (async Memory-Veredelung) | §5 |
| 18 | API-Rate-Limits / 429 | `Retry-After`-Header bevorzugen; Full-Jitter-Backoff; Retry-Budget ≤10 %; Retry nur 1 Schicht; `max_tokens` setzen | §8 |
| 19 | Agent laeuft fremden Code aus | Sandbox: Non-Root + `cap-drop ALL`, read-only FS + tmpfs, Default-Deny-Egress, Secrets NIE in der Sandbox, MicroVM (Firecracker/gVisor) | §9 |
| 20 | „Wann ist fertig?" definieren | 3 Akzeptanz-Dimensionen (Response/Trajectory/**State-Changes**); Capability- vs. Regression-Evals trennen; State wirklich pruefen | §10 |

---

## Frameworks / Begriffe (Stand Juni 2026)

| Begriff | Was | Quelle (Flag) |
|---|---|---|
| Loop Engineering | Design der **Ausfuehrungs-Umgebung** um den Agenten (Trigger, Stop, Feedback, Fehler-Kontrolle) — Abgrenzung zu Prompt Engineering | Data Science Dojo 2026 (`extern`) |
| ReAct | Reason+Act-Basisschleife (Perceive/Reason/Plan/Act/Observe); Industrie-Standard | ReAct-Paper 2022 (`offiziell`) |
| Ralph-/Ralph-Wiggum-Loop | Loop bis externer Validator (Tests/Compiler/Linter) gruen gibt; Context-Reset pro Iteration. Geoffrey Huntley, 7/2025 | github.com/snarktank/ralph (`extern`) |
| Claude Code `/goal` | Produktisierte Ralph-Loop-Variante (dokumentiert: 25h-Lauf, 30k LOC) | Data Science Dojo 2026 (`extern`) |
| ACE — Agentic Context Engineering | Kontext als wachsendes „Playbook" (Generator/Reflector/Curator, grow-and-refine, Delta-Updates) | arXiv 2510.04618, ICLR 2026 (`offiziell`) |
| Four Nested Loops | ReAct + Verification/Retry + Maker-Judge-Loop + Hill-Climbing (Self-Improvement) | Medium „Loop Engineering: …Four Nested Loops" (`extern`) |
| Memory-Pointer-Pattern | Grosse Tool-Outputs in State ablegen, dem Modell nur kurzen Pointer geben | IBM Research / DSDojo (`extern`) |
| OpenClaw | Open-Source-Agent (One-Click auf Hostinger, isolierter Docker) | Hostinger/OMC (`extern`) |

---

## 1. Grundhaltung: Loop Engineering ≠ Prompt Engineering

Der zentrale Paradigmenwechsel (`extern`, Data Science Dojo 2026; Boris Cherny/Anthropic zitiert):
**Die Qualitaet eines autonomen Agenten entsteht NICHT durch einen besseren Prompt, sondern
durch das Design der Schleife drumherum** — die Trigger, Stop-Bedingungen, Feedback-Mechanismen
und Fehler-Kontrollen, die bestimmen *wie* der Agent laeuft, seine Arbeit prueft und iteriert.

Ein Agent besteht aus **zwei trennbaren Schichten** (`extern`, Oracle „The Agent Loop Decoded"):
- **Model** — die Inferenz-Engine, die das Reasoning macht.
- **Harness** — der Code, der Kontext vorbereitet, Tool-Calls ausfuehrt, operative Grenzen
  durchsetzt und State persistiert.
> **Die meiste Agent-Engineering-Arbeit passiert im Harness, nicht im Modell.**

Ein nuetzlicher Agent braucht mindestens vier Dinge (`extern`, Oracle): **Instructions** (System-
Prompt/Ziel), **Memory** (Zugriff ueber die aktuelle Nachricht hinaus), **Actions** (Tool-Calls/
API/DB) und eine **Reasoning-Engine** (das LLM, das den naechsten Schritt entscheidet).

**Konsequenz fuer den Bau:** Zeit in die Schleifen-Mechanik investieren (Stop-Logik, Validatoren,
State-Persistenz, Kosten-Deckel) — nicht in immer laengere System-Prompts.

---

## 2. Loop-Architekturen (welche Schleife fuer welchen Zweck)

Die „10 Loop Engineering Design Patterns" (`extern`, Data Science Dojo 2026) — die wichtigsten:

| Pattern | Kern | Wann |
|---|---|---|
| **ReAct** (Basis) | Perceive→Reason→Plan→Act→Observe, bis Stop | Standard fuer fast alles; OpenAI/Anthropic/Google/MS konvergieren darauf |
| **Reflection** | Agent kritisiert eigenen Output, bis er eigene Kriterien besteht | Halluzinationen senken, Qualitaet > Latenz. **Schwaeche:** Agent ist sein eigener Richter |
| **Tool-Use** | Externe APIs/Tools im Loop fuer aktuelle/proprietaere Daten | Etabliertestes Produktions-Pattern |
| **Prompt-Chaining** | Output→Input in **fester, deterministischer Reihenfolge**; der **Code** entscheidet den naechsten Schritt, nicht der Agent | Klar zerlegbare Subtasks, hohe Auditierbarkeit |
| **Evaluator-Optimizer** | **Separater** Evaluator-Agent gibt strukturiertes Feedback; Primaer-Agent revidiert bis Freigabe | Code-Review, Doku, Daten-Extraktion. Kritik kommt von aussen → kein Selbst-Zustimmen |
| **Supervisor (Multi-Agent)** | Supervisor routet an spezialisierte Worker mit eigenem Loop | Breite, parallelisierbare Arbeit (siehe `orchestrator-agent.md`) |

**Architektur-Wahl-Regel (`extern`):** Autonomie an die Aufgabe anpassen. Prompt-Chaining =
hohe Vorhersagbarkeit / niedrige Autonomie; Supervisor/Evaluator-Optimizer = mehr Laufzeit-
Entscheidung. „Picking the wrong pattern is one of the most common reasons agentic systems
fail in production." Die haeufigsten Produktions-Fehler entstehen durch **Weglassen** der drei
Production-Hardening-Patterns (Circuit Breaker, Heartbeat, Bounded Execution — siehe §3).

> Hinweis Quellenehrlichkeit: „While-loop agents", „Reflexion" (das Paper) und
> „plan-execute-observe-replan" als benannte Architekturen waren in den Runde-1-Quellen NICHT
> belegt — nur die generische Reflection-Schleife. Detail-Nachrecherche folgt in Runde 2.

---

## 3. Termination & Abbruch (der wichtigste Teil eines Loops)

### Bounded Execution (Pattern 10) — PFLICHT
Jeder Loop bekommt **drei harte Grenzen**: **max Iterationen + max Token-Spend + max Wall-Time**
(`extern`, DSDojo; `extern`, Oracle). Ohne sie laeuft ein Loop irgendwann in ein ungeplantes
Limit (Kosten-Spike, Rate-Limit, Timeout). Referenz-Implementation (`extern`, Oracle):

```python
def call_agent(query, thread_id='1', max_iterations=10, max_execution_time_s=60.0):
    start_time = time.time()
    iteration = 0
    while iteration < max_iterations:
        if time.time() - start_time > max_execution_time_s:
            break  # Wall-clock timeout
        response = call_model(messages, tools)
        if not response.tool_calls:
            break  # terminal message — ABER: Goal separat pruefen (s.u.)
        # Tools ausfuehren, Outputs anhaengen, weiter
        iteration += 1
    return 'Max iterations reached; please refine the request.'
```

### Explizite Exit-Kriterien (`extern`, Oracle)
Ein gut designter Loop definiert mehrere Exits: (a) finale Antwort ohne Tool-Calls, (b) ein
**Goal-Completion-Check (objektspezifisches Predikat, NICHT nur Abwesenheit von Tool-Calls)**,
(c) max Iterationen, (d) Wall-Clock-Timeout, (e) nicht behebbarer Fehler, (f) Harness erkennt
Failure-Mode (Wiederholung ohne Fortschritt), (g) Agent setzt explizit ein Completion-Flag.

### Die wichtigste Einzel-Regel: Terminal-Message ≠ Ziel erreicht
> „A terminal message from the model … ends the agent's turn. **It does not mean the user's goal
> has been satisfied.**" (`extern`, Oracle)

Der Harness ist verantwortlich zu pruefen, ob das **Ziel** erfuellt ist — nicht nur, ob das
Modell aufgehoert hat, Tool-Calls zu emittieren. (Siehe Bug-Almanach „Premature/Wrong Termination".)

### Circuit Breaker (Pattern 8) gegen Stagnation
Fortschritts-Signal tracken (Dateien geaendert, Tests gruen, neue vs. wiederholte Fehler).
Stagnations-Bedingung definieren (kein Fortschritt in N Zyklen; gleicher Tool-Call mit
identischen Args 3x in Folge; Oszillation zwischen zwei States). Bei Ausloesung: **vollen State
loggen, Loop terminieren, Mensch alarmieren — Neustart erst nach menschlicher Pruefung** (`extern`).

---

## 4. Prompt-Design & State-Uebergabe im Loop

### System-Prompt dynamisch pro Iteration zusammensetzen
Der System-Prompt ist **nicht statisch**, sondern wird pro Iteration aus Basis-Prompt + den ueber
die Zeit gesammelten „lessons" aus dem State angereichert (`extern`, Medium „Four Nested Loops"):

```python
prompt = system_prompt
if state.get("lessons"):
    prompt += "\n\nLessons learned:\n" + "\n".join(f"- {l}" for l in state["lessons"])
msgs = [SystemMessage(content=prompt)] + list(state["messages"])
```

### State ausserhalb des Modells persistieren
> „Models are volatile; gains must 'stick' to the disk. **State must live in the repository
> because the repo doesn't forget, even if the model does.**" (`extern`, Medium)

In LangGraph-Praxis: `Annotated[list, add_messages]`-Reducer **haengt an** statt zu ersetzen →
voller Verlauf ueber alle Tool-Calls eines Laufs. State-Felder typisieren (TypedDict mit
`lessons`, `spend`, `_steps`, Entscheidung, Flags …). Shared State ueber einen **Control Plane**
(z.B. GitHub Issues als von Agenten editierbarer geteilter Zustand) ist ein bewaehrtes Muster
fuer Manager/Worker-Loops (`extern`, „Agent Loops"-Guide).

### Context-Assembly pro Zyklus (`extern`, Oracle)
Jeder Zyklus: (1) Kontext zusammenbauen (System-Instruktionen + Conversation-State + abgerufenes
Memory + Tool-Outputs + externe Daten), (2) Modell entscheidet naechsten Schritt, (3) handeln,
(4) Trace anhaengen, bis Termination-Check greift. Bei langen Laeufen Context pro Iteration
**resetten** (Ralph-Stil) ODER grosse Outputs per Pointer auslagern (siehe §5/Bug-Almanach).

### Disziplin: ein Loop, eine Verantwortung (`extern`, Medium)
> „Each loop has a single responsibility, a single failure mode it defends against, and a single
> feedback signal it responds to. **Do not mix concerns across loops.**"

---

## 5. Lerneffekt ueber viele Loops — Verdichten ohne Verlust (ACE)

Genau Franks Wunsch-Mechanismus („Loop 1 lernt, Loop 2 lernt, ueber 30-40 Loops werden die
Learnings verdichtet/relativiert/destilliert, der beste Best-Practice wird herausgezogen") ist
fachlich **ACE — Agentic Context Engineering** (`offiziell`, arXiv 2510.04618, ICLR 2026) plus
die **Hill-Climbing-Schicht** der Four Nested Loops.

### ACE-Kernidee: Kontext als wachsendes Playbook, nicht als Zusammenfassung
ACE behandelt den akkumulierten Kontext als **„evolving playbook"**, das Strategien ueber die
Zeit **akkumuliert, verfeinert und organisiert** — statt sie zu einer Zusammenfassung zu
komprimieren. Drei-Rollen-Architektur:
- **Generator** — erzeugt Trajektorien (loest die Aufgabe, produziert Erfahrungen).
- **Reflector** — erzeugt Feedback/Reflexion ueber den Kontext (was lief gut/schlecht, warum).
- **Curator** — integriert neue Erkenntnisse als **strukturierte, inkrementelle Delta-Updates**.

### Der entscheidende Mechanismus: grow-and-refine statt neu schreiben
ACE waechst den Kontext mit **inkrementellen Delta-Updates** statt mit monolithischem
Neuschreiben. Das verhindert die zwei Lern-Killer:
- **Brevity Bias** — zu aggressives Kuerzen verwirft domaenenspezifische Heuristiken.
- **Context Collapse** — iteratives Umschreiben erodiert ueber viele Runden die Details.

> Konsequenz fuer das „Relativieren/Destillieren" ueber 30-40 Loops: **NICHT** alle Learnings zu
> einem kurzen Absatz eindampfen (das ist Brevity-Bias). Stattdessen das Playbook **wachsen
> lassen** und per Delta verfeinern; relevante Strategien destilliert das LLM zur Laufzeit
> selbst heraus (LLMs sind „more effective when provided with long, detailed contexts").
> Das ist exakt das Verlustfrei-Prinzip aus `~/.claude/rules/lossless-context-principle.md`.

Empirie (`offiziell`): +10,6 % auf Agent-Benchmarks, +8,6 % auf Finanz-Benchmarks, **86,9 %
geringere Adaptations-Latenz**; getestet ueber DeepSeek-V3.1, GPT-OSS-120B, GPT-5.1,
Llama-3.3-70B. Lernen erfolgt **ohne gelabelte Supervision** aus „natural execution feedback",
moderat robust gegen Rauschen im Reflector-Feedback.

### Produktions-Praktiken fuer lange Loops (`extern`, Manus)
Alle **nicht-zerstoererisch** (gleicher Anti-Collapse-Gedanke wie ACE):

| Prinzip | Mechanismus |
|---|---|
| **KV-Cache-Hit-Rate maximieren** | Stabile Prompt-Praefixe, append-only Kontext, deterministische Serialisierung |
| **Mask, don't remove** | Tools logit-maskieren statt dynamisch entfernen → KV-Cache bleibt valide |
| **File-System als Kontext** | Unbegrenzter, persistenter Speicher; Agent liest/schreibt Dateien = externalisiertes Gedaechtnis |
| **Restorable Compression** | Nur kuerzen, wenn wiederherstellbar (URL statt Seiteninhalt behalten) |
| **Attention via Recitation** | `todo.md` laufend aktualisieren, um Aufmerksamkeit auf das Ziel zu lenken (Manus: Ø 50 Tool-Calls/Task) |

### Hill-Climbing (Self-Improvement ueber Zeit)
Die aeusserste der Four Nested Loops: ueber viele Laeufe hinweg die **Vorgehensweise selbst**
verbessern (`extern`, Medium). Voraussetzung sind die Disziplinen aus §6 (Maker/Judge, Outcomes
statt Completions, Train/Test-Split: „16 training applications, 4 held-out test").

### Reflexion — verbale Selbstkritik als „semantic gradient" (Runde 2)
**Reflexion** (`offiziell`, Shinn et al. 2023, arXiv 2303.11366, NeurIPS 2023) ist „verbal
reinforcement learning": der Agent verbessert sich **nicht durch Weight-Updates**, sondern durch
sprachliche Selbstkritik. Drei Modelle:
- **Actor** — generiert Aktionen (CoT/ReAct als Basis), erzeugt eine Trajektorie.
- **Evaluator** — bewertet die Trajektorie, gibt einen Reward (LLM-Judge, Heuristik oder
  Exact-Match je nach Task; bei Programming: selbstgeschriebene Unit-Tests).
- **Self-Reflection** — wandelt Reward + Trajektorie + persistenten Memory in **verbalen
  Reflexionstext** um, der im **episodic memory buffer** (Sliding-Window) landet.

> Mechanismus exakt wie von Frank beschrieben: **Reward → verbale Reflexion → in Memory anhaengen
> → naechste Episode nutzt die akkumulierten Lessons als Zusatzkontext**. Reflexion wirkt als
> „semantic gradient signal" — eine konkrete Verbesserungsrichtung statt nur einer Zahl
> (HumanEval 91 % pass@1, +20 % HotPotQA). **Bekannte Grenze:** der Sliding-Window-Memory
> skaliert nicht beliebig → fuer 30-40+ Loops Vektor-/DB-gestuetzten Speicher ergaenzen (genau
> da setzt ACE/MemGPT an).

### MemGPT / Letta — Memory ueber das Context-Limit hinaus (Runde 2)
**MemGPT/Letta** (`extern`, Letta-Blog 2025) behandelt das Context-Window wie RAM und baut eine
**OS-artige Speicherhierarchie**:
- **Core Memory (≈ RAM)** — im Kontext gepinnte, editierbare **Memory-Blocks** (Label/Description/
  Value/Limit). Der Agent kann sie via Tools **selbst umschreiben** → „context rewriting",
  konsolidiert wichtige Infos ueber die Zeit.
- **Recall Memory (≈ Disk)** — vollstaendige, auf Disk persistierte Konversationshistorie (durchsuchbar).
- **Archival Memory (≈ Disk)** — externes Wissen in Vector-/Graph-DBs, per Tool-Call abrufbar.

Konsolidierung: **Recursive Summarization** beim Evicten (alte Nachrichten werden mit bestehenden
Summaries zusammengefasst; nur ~70 % evicten fuer Kontinuitaet) und **Sleep-Time-Compute** —
spezialisierte Agenten veredeln den Memory **asynchron in Leerlaufphasen** (proaktiv statt lazy,
ohne die Live-Konversation zu verlangsamen). Merksatz aus der Quelle: **„Retrieval (RAG) ist ein
Werkzeug fuer Memory, aber nicht selbst Memory."**

> Die fuenf Memory-Mechanismus-Familien (`offiziell`, arXiv-Survey): context-resident compression,
> retrieval-augmented stores, reflective self-improvement, hierarchical virtual context,
> policy-learned management. ACE = reflective self-improvement; MemGPT = hierarchical virtual context.

> Noch offen (auch in Runde 2 nicht belegt): **Generative Agents Memory-Stream**
> (recency/importance/relevance-Scoring, Reflection-Trees) und **Voyager Skill-Library**
> (Automatic Curriculum, Code-as-Skills) wurden nur historisch erwaehnt; **Experience-Replay**
> und **Self-Distillation** fuer LLM-Agenten waren in keiner Quelle ausgefuehrt. Bei Bedarf
> gezielt die Originalpapers (Park et al. 2023; Wang et al. 2023) heranziehen.

---

## 6. Selbstbetrug verhindern (Reward-Hacking, Drift, falsche Termination)

Die staerksten Anti-Selbstbetrugs-Regeln (`extern`, mehrere Quellen — Details im Bug-Almanach):

- **Maker ≠ Judge trennen** — ein zweites Modell bewertet den Output. „First step in preventing
  the system from 'grading its own homework'."
- **Outcomes statt Completions bewerten** — nicht „hat der Agent fertig gemeldet?", sondern „wurde
  das Nutzer-Ziel wirklich erreicht?". „Ask for clarification" ist **kein** Failure.
- **„No receipt, no claim"** — fuer jeden Side-Effect ein Beleg (Ticket-ID, Message-ID, Record-URL),
  sonst gilt die Behauptung nicht.
- **E2E-Validierung statt nur Unit-Tests** — Unit-Test/curl gruen heisst nicht, dass der echte
  Nutzer-Pfad funktioniert.
- **Uncertainty-Reporting** — Annahmen explizit labeln (gegen Overconfidence bei duenner Evidenz).

### Goal-Drift aktiv gegensteuern
- **Goal-Restatement alle N Steps**: „What are we trying to achieve?" erneut in den Kontext.
- **Progress-Contract**: Pflicht-Output-Schema (z.B. „must return JSON with fields X").
- **Max-Step-Budget** mit sauberem „need clarification"-Exit.
- **Plan-Divergence-Score** (geplante vs. ausgefuehrte Steps) + „rising token spend without
  progress" als Fruehwarnsignale.

### Kosten-Disziplin
- **Tool-Allowlist pro Task-Typ**; Agent muss `why_this_tool` / `expected_result` /
  `stop_condition` ausgeben; **Safe-Mode** (read-only Tools) bis Confidence hoch.
- **Reasoning-Effort runter**, wo Qualitaet es zulaesst (z.B. OpenRouter Reasoning-Stufe „low").
- **Cost-per-successful-Outcome** messen, nicht Cost-per-Call.

### Early-Warning-Dashboard (`extern`)
Sichtbare Metriken machen Zuverlaessigkeit schnell besser: Success-Rate (echt, nicht
„completed"), Avg-Steps/Run, Tool-Calls/Run, **Loop-Rate**, **Unverified-Claim-Rate**,
Policy-Violation-Attempts, Cost-per-Successful-Outcome, Time-to-Resolution (Median + p95).

---

## 7. Autonomer 24/7-Betrieb auf VPS (Hostinger u.a.)

### Setup & Deployment
- **Hostinger**: One-Click-Deployment (z.B. OpenClaw) in isoliertem **Docker-Container**; eigene
  API-Keys (OpenAI/Anthropic/Google) ODER vorgebackene LLM-Credits im Dashboard. Beim ersten
  Install wird ein „high-complexity gateway token" erzeugt (Zugang von Anfang an abgesichert).
- **Eigener Python-Agent → VPS**: per SSH uebertragen, Git zur Versionierung, **Secrets in `.env`
  + `.gitignore`** (nie ins Repo — deckt sich mit `secrets-in-sk-folder.md`), per **Cron**
  zeitgesteuert starten, `print`/Logging fuer Sichtbarkeit.
- **OpenRouter** als zentrales LLM-Gateway (eine API fuer viele Modelle, Reasoning-Stufe einstellbar).

### Der wichtigste Betriebs-Satz: „Container uptime ≠ Agent uptime"
Der Container kann laufen, waehrend der Agent laengst haengt (`extern`, Produktions-Checkliste).
Typische Bruchstellen: Workspace-Persistenz-Verlust, Browser-Session-Recovery, **Tool-Call-Haenger**,
Memory-/Resource-Spikes, Upgrade-Drift.

### Pflicht-Runtime-Eigenschaften fuer 24/7
1. **Persistent Workspace** — State/Dateien ueberleben Neustarts.
2. **Restart-Semantik** — definierte Regeln, was nach Crash passiert.
3. **Logs & Replay** — vollstaendige Logs, Sessions reproduzierbar (deckt sich mit `observability-first`).
4. **Per-Agent-Resource-Limits** — ein Agent darf nicht alle Ressourcen fressen.
5. **Secret-Handling** — saubere Key-Verwaltung.
6. **Human-Override-Pfad** — manuelles Eingreifen jederzeit moeglich.

> Luecken Runde 1 (in Runde 2 zu fuellen): konkrete **Rate-Limit-Strategien** (Backoff/Jitter/
> Multi-Provider-Failover), **hartes Cost-Budgeting/Alerting**, **Container-Hardening** (Non-Root,
> read-only FS, seccomp/AppArmor, Network-Policies), Update-/Rollback-Strategien.

---

## 8. Rate-Limit-Resilienz fuer langlaufende Agenten (Runde 2)

Langlaufende Agenten feuern viele API-Calls — ohne Resilienz-Schicht killen Rate-Limits den
Loop. Die bewaehrte **3-Schichten-Architektur** (`extern`, mehrere Produktions-Quellen):

| Schicht | Funktion |
|---|---|
| **1. Token-Bucket pro Identity** | Volumen-Throttling pro `(user, repo, model)`; eigener Bucket; bei leer sofort lokales 429 statt Provider-Call. Tokens **vor** dem Call schaetzen (Tiktoken / `chars/4`) → proaktiv statt reaktiv |
| **2. Circuit Breaker** | closed→open→half-open; nach N Failures (z.B. 5) oeffnen, nach 60s ein Test-Request. LLM-spezifische Trigger: Cost/Request, Turn-Count >20, Quality-Score-Drop |
| **3. Deklarative Fallback-Chain** | primary → cheaper model → semantic cache → 503; Multi-Provider-Failover (sequenziell oder Parallel-Hedging) |

**Die wichtigsten Einzelregeln:**
- **`Retry-After`-Header bevorzugen** — zuverlaessiger als jede selbst berechnete Backoff-Formel.
- **Full-Jitter-Backoff** statt nacktem Exponential: `sleep = random(0, min(cap, base·2^attempt))`
  (verhindert „Thundering Herd"). Startwerte ~1/2/4s, Cap 32-60s, max 3-5 Versuche.
- **Retry-Budget**: globale Retries ≤ **10 %** aller Requests; darueber **fail-fast**.
- **Retry nur auf EINER Schicht** (aeusserste) — sonst Multiplikation (3 Retries × 5 Layer =
  243 Backend-Calls). ~40 % der Cascading-Failures kommen aus Retry-Logik.
- **Retryable:** 429, 408, 500-504. **Nie retryen:** 400, 401, 403, 404.
- **TPM und RPM getrennt behandeln**; **immer `max_tokens` setzen** (eine Antwort kann sonst das
  TPM-Budget leerfressen).

## 9. Hardening & Sandboxing fuer 24/7-Agenten (Runde 2)

Ein autonom laufender Agent, der Code ausfuehrt oder ins Netz greift, gehoert isoliert. Die
**Vier-Schichten-Sandbox** (`extern`, Sandbox-Produktions-Guides):

| Schicht | Massnahme |
|---|---|
| **Container-Haertung** | Non-Root-User, `--cap-drop ALL`, `--security-opt no-new-privileges`, seccomp- + AppArmor-Profile |
| **Filesystem** | `--read-only` Root + `--tmpfs /tmp` (Workspace verschwindet bei Sandbox-Zerstoerung) |
| **Netzwerk** | **Default-Deny-Egress** — alles ausgehende blockieren, nur noetige Endpunkte allowlisten |
| **Resource-Limits** | cgroup v2: CPU, Memory (Hard-Cap), Disk-Quota/IO, **Wall-Clock pro Tool-Call** (z.B. 30s) |

**Secrets verlassen NIE die Sandbox:** der Agent laeuft mit **zero secrets**; ein Control-Plane
haelt die Credentials und injiziert sie (z.B. ueber einen MITM-/Egress-Proxy). Sonst kann ein
kompromittierter Agent Tokens exfiltrieren. **Kurzlebige Credentials pro Session**, keine
langlebigen Secrets in die Umgebung.

**Staerkere Isolation als Container — MicroVMs** (Container-Escapes existieren real, z.B. runc
CVE-2024-21626): **Firecracker** (KVM-basiert, Boot ≤125 ms, ~5 MiB Overhead — Basis von E2B),
**gVisor** (User-Space-Kernel „Sentry", Escape braucht zwei Bugs; GPU via nvproxy), **E2B**
(Firecracker-Cloud, ~150 ms Cold-Start). Hinweis: Firecracker hat kein offizielles GPU-Passthrough.

**Update/Rollback fuer langlaufende Agenten — Checkpoint/Restore:** Disk-Snapshot in ~300 ms,
**vor destruktiven Aktionen** snapshotten, bei Fehler auf den letzten Known-Good-Punkt
zuruecksetzen (statt alles neu zu laufen). **Idle-Warm-Pools** (vorgewaermte Sandboxes) druecken
den Cold-Start gegen null. Session-Lifecycle: provision → execute → optional snapshot → on
completion destroy → on error rollback. Deckt sich mit `secrets-in-sk-folder.md` + `observability-first.md`.

## 10. Definitions-of-done & Eval-Harness fuer Loops (Runde 2)

Die nuetzlichsten Agenten sind nicht die autonomsten, sondern die mit den **klarsten Zielen,
staerksten Feedback-Signalen und striktesten „Definitions of done"**. Wie man das baut (`extern`,
LangChain/OpenAI/Confident-AI Eval-Guides):

### „Done" eindeutig definieren
- **Test der Eindeutigkeit:** „If two experts can't agree on pass/fail, the task needs refinement."
  Schlecht: „Summarize this well." Gut: „Extract the 3 main action items, each < 20 words, with owner."
- **Reference-Solution** je Task — beweist Loesbarkeit + liefert Grade-Baseline.
- **Drei Akzeptanz-Dimensionen** pro Agent-Turn: **Final-Response** (korrekt/nuetzlich?),
  **Trajectory** (vernuenftiger Pfad — nicht zwingend der exakt erwartete) und vor allem
  **State-Changes** (sind die Artefakte wirklich da?). „The final response can say 'Done!' while
  the actual state is wrong" → bei „Meeting scheduled" den Kalendereintrag pruefen, bei Code den
  Code laufen lassen, bei DB-Update die Zeilen abfragen.
- Typspezifisch: Coding → deterministische Test-Suites + Quality-Rubrik; Conversational →
  Completion + Interaktionsqualitaet; Research → Groundedness + Coverage.

### Eval-Harness als Diagnose-Stack
- **Einfachste Eval zuerst** (ein paar E2E-Tests = sofort Baseline), Komplexitaet nur bei Bedarf.
- **Drei Ebenen:** End-to-End (Outcome) → Trajectory (Pfad/Tool-Calls/Retries) → Component
  (einzelne Retriever/Tools/Sub-Agenten). „Outcome first, then path, then failing component."
- **Positive UND negative Faelle** testen (sonst optimiert man einen Agenten, der alles sucht).
- **N-1-Testing** fuer Multi-Turn: echte Prefixe der ersten N-1 Turns aus Produktion, Agent
  generiert nur den letzten Turn (vermeidet Compounding-Error synthetischer Dialoge).

### LLM-as-Judge — gezielt, nicht ueberall
- **Deterministische Checks fuer Exaktes** (Tool-Correctness), **LLM-Judge nur fuer output-
  abhaengiges** (Task-Completion, Plan-Quality, Argument-Correctness).
- Vorsicht: „sed quis custodiet ipsos custodes?" — der Judge braucht Threshold + Kalibrierung via
  Tracing + periodisches Human-Review (ist selbst angreifbar). **Eval-Ownership:** ein Domain-Experte
  als Quality-Arbiter fuer mehrdeutige Faelle (nicht „design by committee").

### Regression-Gates trennen
- **Capability-Evals** (niedrige Pass-Rate, „Hill to climb" — treiben Fortschritt) vs.
  **Regression-Evals** (~100 % Pass-Rate — schuetzen, was schon funktioniert). Beide noetig.
- **Pre-Deployment-Quality-Gate** vor Merge; erst „reviewed loop" (Mensch genehmigt Diff), mit
  wachsendem Vertrauen tiefere Automatisierung.
- **Infra-Probleme zuerst ausschliessen** — Timeouts/malformte Responses/stale Caches maskieren
  sich oft als Reasoning-Fehler.
- **Failure-Analyse = 60-80 % des Eval-Aufwands:** Traces sammeln → Open-Coding (Domain-Experte)
  → Failure-Taxonomie → iterieren, bis keine neuen Kategorien; dann **Fix nach Root-Cause routen**
  (Prompt-/Tool-Design-/Modell-Problem). Verwandt: Strukturelles Testing via OpenTelemetry-Traces +
  Mocking fuer reproduzierbares LLM-Verhalten.

> Auch in Runde 2 NICHT belegt: **Claude-Agent-SDK-Loop-Primitive** (`maxTurns`, `maxBudgetUsd`,
> Context-Editing, Memory-Tool) und konkrete **Multi-Agent-Reconciliation** paralleler Arbeit —
> bei Bedarf direkt aus der Anthropic-Doku / `orchestrator-agent.md` ziehen.

## Zusammenspiel mit dem eigenen Harness

| Konzept aus der Recherche | Entspricht im eigenen System |
|---|---|
| ACE grow-and-refine, restorable compression | `lossless-context-principle.md` (verlustfrei auslagern statt wegwerfen) |
| Logs & Replay, Sichtbarkeit pro Schritt | `observability-first.md` + `observability-live-logic-probes.md` |
| Circuit Breaker, Bounded Execution, Resume | `subagent-crash-proofing.md` (Orchestrator-Resume, Checkpoints) |
| Secrets in `.env`/ausserhalb Repo | `secrets-in-sk-folder.md` |
| Maker ≠ Judge, Outcomes statt Completions | Direktive #2 (Selbstbeobachtung) + quality-gate |
| Terminal-Message ≠ Ziel erreicht | Direktive #3 (Verifikation vor „fertig") |

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach-Abschnitt

| Best-Practice (hier) | Bug-Almanach-Abschnitt (`bugs/agents/loop-engineering.md`) |
|---|---|
| §1 Loop Engineering ≠ Prompt Engineering | (Grundhaltung — kein einzelner Bug) |
| §2 Loop-Architekturen | §1 Infinite Loops (falsche Architektur/Feedback) |
| §3 Termination & Abbruch | §1 Infinite Loops · §7 Premature/Wrong Termination |
| §4 Prompt-Design & State-Uebergabe | §2 Context Overflow · §4 Learning Degradation |
| §5 Lerneffekt ueber Loops (ACE) | §4 Learning Degradation / Forgetting |
| §6 Selbstbetrug verhindern | §3 Cost Explosion · §5 Reward Hacking · §6 Goal Drift · §7 Termination |
| §7 24/7-Betrieb auf VPS | §8 „Container uptime ≠ Agent uptime" |
| §8 Rate-Limit-Resilienz | §3 Cost Explosion (Cascading Retries) |
| §9 Hardening & Sandboxing | §8 VPS-Dauerbetrieb (Secret-Exfiltration, Container-Escape) |
| §10 Definitions-of-done & Eval-Harness | §7 Premature/Wrong Termination (State-Changes pruefen) |

## Quellen (Runde 1, 2026-06-24)
- Data Science Dojo — „10 Loop Engineering Design Patterns Every AI Developer Should Know (2026)" (`extern`)
- Oracle Blogs — „The Agent Loop Decoded" (`extern`)
- Medium — „Loop Engineering: Building Self-Improving AI Agents with Four Nested Loops" (`extern`)
- arXiv 2510.04618 / OpenReview ICLR 2026 — ACE: Agentic Context Engineering (`offiziell`)
- Manus — Context-Engineering-Lessons (`extern`)
- Hostinger / OMC Cloud — VPS-Deployment fuer autonome Agenten (`extern`)
- Produktions-Checkliste „deploying AI agents in production" + Failure-Mode-Quellen (`extern`)

## Quellen (Runde 2, 2026-06-24 — Luecken/Folgefragen)
- arXiv 2303.11366 — Reflexion: Language Agents with Verbal Reinforcement Learning (Shinn et al., NeurIPS 2023) (`offiziell`)
- Letta-Blog + DeepLearning.AI + arXiv-Memory-Survey — MemGPT/Letta (Core/Recall/Archival, Sleep-Time-Compute) (`extern`/`offiziell`)
- Portkey / Truefoundry / tianpan.co / dev.to / Maxim AI — LLM-Rate-Limit-Resilienz (Backoff+Jitter, Token-Bucket, Failover, Circuit Breaker) (`extern`)
- Sandbox-Hardening-Guides (Firecracker/gVisor/E2B, seccomp/AppArmor, Default-Deny-Egress, Checkpoint/Restore) (`extern`)
- LangChain / OpenAI-Cookbook / Confident-AI — Agent-Evals, Definitions-of-done, LLM-as-Judge, Regression-Gates (`extern`)
