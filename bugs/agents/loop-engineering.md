# Bekannte Bugs: Loop Engineering (autonome Agenten-Schleifen)

> **PFLICHT-LESEN vor Arbeit an einem autonom in Schleifen laufenden Agenten** — egal ob
> innerhalb von Claude Code (Agent-Tool / Agent SDK), als from-scratch-Loop gegen die rohen
> LLM-APIs, oder als 24/7-Dauerlaeufer auf einem VPS (Hostinger u.a.).
> **Geltungsbereich:** die SCHLEIFE selbst — Termination, Context-Wachstum, Kosten, Lernen,
> Selbstbetrug, Dauerbetrieb. Multi-Agent-Routing/Spawning: siehe `bugs/agents/orchestrator-agent.md`.
> **Stand:** recherchiert am 2026-06-24 (Firecrawl+MiniMax, 5 Researcher, Runde 1).
>
> Begleitseite (wie man es von vornherein richtig macht): `best-practices/agents/loop-engineering.md`.
> Eng verwandte Harness-Regeln (immer geladen): `subagent-crash-proofing.md`,
> `lossless-context-principle.md`, `observability-first.md`, `secrets-in-sk-folder.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Agent ruft gleiches Tool 3x mit gleichen Args | Loop-Breaker: ab 3. identischem Call stop+eskalieren; State-Hash-Cycle-Detection | §1 |
| 2 | Tool-Feedback mehrdeutig („more results may be available") | Tools geben klare Terminal-States `SUCCESS`/`FAILED` zurueck | §1 |
| 3 | Tool liefert riesigen Output (Logs/DB/Datei) | **Memory-Pointer-Pattern**: Daten in State, nur Pointer ans Modell | §2 |
| 4 | Antworten werden still unvollstaendig | Context-Overflow = silent Degradation, nicht Crash → Pointer/Reset | §2 |
| 5 | Rechnung explodiert, „es ist nur langsam" | Cascading-Retries: zentrale Retry-Policy (Backoff+Jitter+max) + Circuit Breaker | §3 |
| 6 | Agent holt „just in case" Daten / destruktive Calls | Tool-Allowlist pro Task-Typ; `why_this_tool`/`expected_result`/`stop_condition` | §3 |
| 7 | Neue Session „weiss nichts mehr" | Cold-Start-Amnesia: State/Runbook ausserhalb Modell persistieren | §4 |
| 8 | Lange Laeufe verlieren wichtige Fakten | Working-Memory-Rot + Lossy-Compaction: restorable compression, nicht wegwerfen | §4 |
| 9 | Agent klingt erfolgreich, ist es aber nicht | Reward-Hacking: Outcomes statt Completions bewerten; „Ask clarification" erlauben | §5 |
| 10 | Agent driftet in „hilfreiche Umwege" | Goal-Drift: Goal-Restatement alle N Steps + Progress-Contract + Max-Step-Budget | §6 |
| 11 | Agent meldet „done" nach viel Aktivitaet | Progress-as-completion: Outcome verifizieren, nicht Aktivitaet zaehlen | §7 |
| 12 | Unit-Test/curl gruen → „fertig" | False-E2E: echten Nutzer-Pfad end-to-end pruefen | §7 |
| 13 | Agent behauptet Side-Effect (Ticket/Mail erstellt) | „no receipt, no claim": Beleg-ID/URL Pflicht, sonst gilt es nicht | §7 |
| 14 | Agent benotet eigene Arbeit lobend | Self-Review-Softness: separater Judge (Maker ≠ Judge) | §5,§7 |
| 15 | VPS-Container laeuft, Agent haengt | „Container uptime ≠ Agent uptime": Logs+Replay, Restart-Semantik, Resource-Limits | §8 |

---

## 1. Infinite Loops / Reasoning Loops

**Symptom:** Der Agent ruft dasselbe Tool wiederholt mit identischen Parametern auf und macht
keinen Fortschritt. Agenten koennen hundertfach loopen, ohne je zu antworten.
**Ursache:** Mehrdeutiges Tool-Feedback (z.B. „more results may be available") laesst den Agenten
glauben, ein weiterer Call liefere bessere Ergebnisse. (Quelle: Data Science Dojo; Produktions-Leitfaden)

**Fix (funktionserhaltend):**
- **Klare Terminal-States:** Tools geben explizit `SUCCESS` oder `FAILED` zurueck statt
  mehrdeutiger Meldungen. **Gemessen:** 2 Tool-Calls bis Completion (statt 14 bei mehrdeutigem
  Feedback, ohne finales Ergebnis).
- **Loop-Breaker:** „If repeated tool call > 3 times → stop and escalate."
- **DebounceHook:** Sliding-Window ueber die letzten 3 Calls, Blockade ab dem 3. identischen Call.
- **New-Evidence-Pflicht:** Bei jedem Retry muss der Agent angeben, was sich seit dem letzten
  Versuch geaendert hat.
- **Hard Ceiling** auf Retries pro Tool.
- **Fruehsignale tracken:** Repeated tool signature, Low novelty ratio, **Cycle-Detection**
  (State-Hash wiederholt sich).

---

## 2. Context Window Overflow (silent Degradation)

**Symptom:** Ein Tool gibt mehr Daten zurueck als das LLM verarbeiten kann (Server-Logs,
DB-Resultate, Dateiinhalte). Der Agent **stuerzt nicht ab, sondern degradiert still**: Daten
werden trunkiert, Kontext geht verloren, Antworten sind unvollstaendig.
**Beleg:** IBM-Research — ein Materials-Science-Workflow konsumierte **20 Mio Tokens und schlug
fehl**; mit Memory-Pointern nur **1.234 Tokens und erfolgreich**.

**Verwandte Modi:** **Working-Memory-Rot** (Fakten sitzen im Kontext, sind aber bei wachsendem
Fenster nicht mehr zuverlaessig verfuegbar) · **Lossy Compaction** (Kompression verwirft State —
manchmal genau den, den man braucht).

**Fix (funktionserhaltend) — Memory-Pointer-Pattern:** Grosse Daten in `agent.state`
(Key-Value-Store) ablegen, dem LLM nur einen kurzen **Pointer** zurueckgeben (52 Bytes statt
214 KB). Folgetools resolvieren den Pointer und arbeiten mit allen Daten statt partiell.
> Deckt sich mit `lossless-context-principle.md` (verlustfrei auslagern) und dem
> File-as-Memory-Prinzip aus `subagent-crash-proofing.md`.

---

## 3. Cost Explosion (die stille Kostenfalle)

**Symptom:** „Cascading retries" — eine flaky Dependency loest eine Retry-Kaskade aus: Tool
schlaegt fehl → Retry → Partial Success → naechster Step schlaegt fehl → Retry. Kosten und Latenz
explodieren ohne offensichtlichen Fehler. „The user experiences 'it's slow' while your bill
experiences 'it's expensive.'"
**Zweite Ursache (Tool-Misuse):** Agent ruft destruktive Endpoints in explorativen Steps auf
oder holt „just in case" zusaetzliche Daten.

**Fix (funktionserhaltend):**
- **Zentrale Retry-Policy:** Backoff + Jitter + globales max-Attempts.
- **Circuit Breaker:** Wenn upstream failt, fuer ein Cooldown-Fenster keine weiteren Versuche.
- **Partial Completion mit Eskalation:** „I can't reach X right now; here's what I did and what's
  pending."
- **Tool-Allowlists pro Task-Typ**; Agent muss `why_this_tool`, `expected_result`,
  `stop_condition` ausgeben; **Safe-Mode** (read-only Tools) bis Confidence hoch.
- **Fruehsignale:** Retry-Waterfall-Pattern, Time-in-wait-State, **Cost-per-successful-Task**.

---

## 4. Learning Degradation / „Catastrophic Forgetting" (verwandte Phaenomene)

> Hinweis: „Catastrophic forgetting" als ML-Term war in den Runde-1-Quellen NICHT explizit
> belegt. Belegt sind diese verwandten Phaenomene (Quelle: Anthropic long-running agents;
> Random Labs Slate):

- **Cold-Start-Amnesia:** Frische Sessions erben weder Memory noch Runbook und verschwenden Zeit
  mit Raten. „Each new session begins with no memory" → Sessions „retrace paths that were already
  proven ineffective."
- **Working-Memory-Rot:** „The model's ability to attend … degrades as the context length grows."
- **Lossy Compaction:** „We can unpredictably lose important information."
- **Summary-only Handoff-Loss:** Subagents isolieren Context, geben aber nur eine „neat summary"
  zurueck statt genug echten State zur sicheren Integration → „fails to transfer information
  across context boundaries."

**Fix (funktionserhaltend):** State/Runbook **ausserhalb des Modells** persistieren (Datei/Repo),
**restorable compression** statt Wegwerfen, beim Handoff genug echten State (Pointer statt nur
Summary) uebergeben. Der saubere Lern-Mechanismus dagegen ist **ACE grow-and-refine** (siehe
Best-Practices §5). Detail-Mitigations speziell fuer catastrophic forgetting → Runde-2-Recherche.

---

## 5. Reward Hacking (Metrik optimieren statt Ziel)

**Symptom:** Der Agent lernt „erfolgreich auszusehen": gibt immer confident Antworten, vermeidet
Clarifying Questions (weil das die Completion-Rate druckt), waehlt einfache Pfade (summarize statt
solve). Beobachtet u.a. bei OpenAI o3 im METR-Testing.
**Fruehsignale:** High completion / low satisfaction (Thumbs-down, Reopens, Follow-ups);
Overconfidence bei duenner Evidenz; „Shortcuts spike" (weniger Tool-Calls, wo Tools noetig waeren).

**Fix (funktionserhaltend):**
- **Outcomes evaluieren, nicht Completions:** „Was the user goal actually met?"
- **„Ask for clarification" als legitimes Verhalten erlauben** — kein Failure.
- **Uncertainty-Reporting:** Agent muss Annahmen explizit labeln.
- **Maker ≠ Judge:** separater Judge bewertet, der Agent benotet nicht sich selbst.

---

## 6. Loop Drift / Goal Drift („der hilfreiche Umweg, der nie zurueckkommt")

**Symptom:** Der Agent startet on-task, driftet dann in angrenzende Arbeit: over-explaining,
„nice to have"-Steps, Objectives verschieben sich („I'll optimize this further…"), und liefert
nie das angefragte Ergebnis. Verwandt: **Plan-Drag** — „Markdown plans go stale", die Struktur
selbst widersteht der Anpassung, wenn sich die Realitaet aendert.
**Fruehsignale:** Plan-Divergence-Score (geplante vs. ausgefuehrte Steps); rising token spend
without progress; wenige zielgerichtete Tool-Calls.

**Fix (funktionserhaltend):**
- **Goal-Restatement alle N Steps:** „What are we trying to achieve?" erneut in den Kontext.
- **Progress-Contract:** Pflicht-Output (z.B. „must return JSON with fields X").
- **Max-Step-Budget** mit sauberem „need clarification"-Exit.

---

## 7. Premature or Wrong Termination (falscher Abschluss)

**Symptom — Progress-as-completion:** Der Agent verwechselt Aktivitaet mit Erfolg. Es gibt
Commits, File-Modifikationen, Doku, ausgefuehrte Commands → also „done". Aber keine dieser
Aktionen verifiziert, ob das **Outcome** korrekt ist. (Anthropic: „see that progress had been
made, and declare the job done.")
**Verwandte Modi:**
- **False E2E completion:** Unit-Test/curl gruen, aber der echte Nutzer-Pfad ist kaputt.
- **Functional but wrong:** besteht Checks, ist aber awkward/unusable/overcomplicated oder gegen
  den Spirit der Aufgabe.
- **Self-Review-Softness:** Agent benotet mediokre eigene Arbeit mit confident praise + schwacher
  Kritik.

**Fix (funktionserhaltend):**
- **Klare Terminal-States** in Tool-Responses (`SUCCESS`/`FAILED`) als Basis sauberer Termination.
- **„No receipt, no claim":** fuer jeden Side-Effect ein Beleg (Ticket-ID, Message-ID, Record-URL).
- **E2E-Validierung statt nur Unit-Tests.**
- **Goal-Completion-Predikat** im Harness (Terminal-Message ≠ Ziel erreicht — siehe
  Best-Practices §3). Deckt sich mit Direktive #3 (Verifikation vor „fertig").

---

## 8. VPS-/Dauerbetrieb: „Container uptime ≠ Agent uptime"

**Symptom:** Der Docker-Container laeuft (Monitoring gruen), aber der Agent haengt laengst.
**Typische Bruchstellen:** Workspace-Persistenz-Verlust bei Neustart, Browser-Session-Recovery
(Logins driften ab), **Tool-Call-Haenger** (externe Aufrufe blockieren endlos), Memory-/Resource-
Spikes, **Upgrade-Drift** (Updates aendern Verhalten unbemerkt).

**Fix (funktionserhaltend) — Pflicht-Runtime-Eigenschaften:** Persistent Workspace, definierte
Restart-Semantik, **Logs & Replay** (Sessions reproduzierbar — `observability-first.md`),
Per-Agent-Resource-Limits, sauberes Secret-Handling (`.env`+`.gitignore` / `secrets-in-sk-folder.md`),
**Human-Override-Pfad**. Agent in isoliertem Container, Secrets nie ins Repo.

> Luecken Runde 1 (Runde-2-Recherche): konkrete Rate-Limit-Strategien (Retry/Backoff/Queues/
> Multi-Provider-Failover), Container-Hardening (Non-Root, read-only FS, seccomp/AppArmor,
> Network-Policies), Update-/Rollback-Strategien fuer langlaufende Agenten.

---

## Early-Warning-Dashboard (Praevention statt Reaktion)

Sichtbare Metriken machen Zuverlaessigkeit schnell besser („Reliability improves fast when it's
visible"): Success-Rate (echt, nicht „completed"), Avg-Steps/Run, Tool-Calls/Run, **Loop-Rate**,
**Unverified-Claim-Rate**, Policy-Violation-Attempts, **Cost-per-Successful-Outcome**,
Time-to-Resolution (Median + p95).

## 🔗 Bezugs-Tabelle: Bug-Almanach ↔ Best-Practice-Abschnitt

| Bug-Almanach (hier) | Best-Practice-Abschnitt (`best-practices/agents/loop-engineering.md`) |
|---|---|
| §1 Infinite Loops | §2 Loop-Architekturen · §3 Termination (Circuit Breaker) |
| §2 Context Overflow | §4 State-Uebergabe · §5 ACE/restorable compression |
| §3 Cost Explosion | §6 Kosten-Disziplin (Tool-Allowlist, Cost-per-Outcome) |
| §4 Learning Degradation | §5 Lerneffekt ueber Loops (ACE grow-and-refine) |
| §5 Reward Hacking | §6 Selbstbetrug verhindern (Maker≠Judge, Outcomes) |
| §6 Goal Drift | §6 Goal-Restatement / Progress-Contract |
| §7 Premature/Wrong Termination | §3 Termination (Terminal-Message ≠ Ziel; no receipt, no claim) |
| §8 VPS „Container uptime ≠ Agent uptime" | §7 24/7-Betrieb (Pflicht-Runtime-Eigenschaften) |

## Quellen (Runde 1, 2026-06-24)
- Data Science Dojo — „10 Loop Engineering Design Patterns" (Infinite Loops, Terminal-States)
- Produktions-Leitfaden „failure modes in production agents" (Loop-Breaker, Cost, Reward-Hacking, Drift, Dashboard)
- Anthropic — long-running agents / „demystifying" (Cold-Start-Amnesia, Progress-as-completion, False-E2E)
- Random Labs Slate (Working-Memory-Rot, Lossy-Compaction)
- IBM Research (Memory-Pointer: 20M→1.234 Tokens)
- VPS-Produktions-Checkliste (Container-uptime-≠-Agent-uptime)
