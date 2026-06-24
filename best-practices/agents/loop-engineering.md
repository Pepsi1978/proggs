# Loop Engineering — Best Practices (Stand 2026-06-24)

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
> Aufgebaut per Firecrawl+MiniMax-Recherche (5 Researcher, Runde 1) am 2026-06-24.

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

> Detail-Luecken aus Runde 1 (Reflexion-Mechanik, Generative Agents Memory Stream, Voyager
> Skill-Library, MemGPT, Experience-Replay, Self-Distillation) werden in Runde 2 nachrecherchiert.

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

## Quellen (Runde 1, 2026-06-24)
- Data Science Dojo — „10 Loop Engineering Design Patterns Every AI Developer Should Know (2026)" (`extern`)
- Oracle Blogs — „The Agent Loop Decoded" (`extern`)
- Medium — „Loop Engineering: Building Self-Improving AI Agents with Four Nested Loops" (`extern`)
- arXiv 2510.04618 / OpenReview ICLR 2026 — ACE: Agentic Context Engineering (`offiziell`)
- Manus — Context-Engineering-Lessons (`extern`)
- Hostinger / OMC Cloud — VPS-Deployment fuer autonome Agenten (`extern`)
- Produktions-Checkliste „deploying AI agents in production" + Failure-Mode-Quellen (`extern`)
