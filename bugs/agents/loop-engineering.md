# Bekannte Bugs: Loop Engineering (autonome Agenten-Schleifen)

> **PFLICHT-LESEN vor Arbeit an einem autonom in Schleifen laufenden Agenten** — egal ob
> innerhalb von Claude Code (Agent-Tool / Agent SDK), als from-scratch-Loop gegen die rohen
> LLM-APIs, oder als 24/7-Dauerlaeufer auf einem VPS (Hostinger u.a.).
> **Geltungsbereich:** die SCHLEIFE selbst — Termination, Context-Wachstum, Kosten, Lernen,
> Selbstbetrug, Dauerbetrieb. Multi-Agent-Routing/Spawning: siehe `bugs/agents/orchestrator-agent.md`.
> **Stand:** recherchiert am 2026-06-24 (Firecrawl+MiniMax). Runde 1 (5 Researcher) + Runde 2
> (5 Researcher): Rate-Limit-Fehler, Sandbox-/Hardening-Fehler, Eval-Fallen ergaenzt.
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
| 16 | Retry-Storm / Kosten explodieren bei 429 | `Retry-After` parsen, Full-Jitter, Retry-Budget ≤10 %, Retry nur 1 Schicht (sonst 3×5=243 Calls) | §9 |
| 17 | Agent ignoriert Retry-After, hammert weiter | Circuit Breaker bei „consecutive 429s > N": 60s open mit exponential reopen | §9 |
| 18 | Secrets in der Sandbox / Container-Escape | Zero-Secrets-Sandbox (Control-Plane injiziert), Default-Deny-Egress, MicroVM statt nacktem Container (runc-CVEs) | §10 |
| 19 | „Done!" gemeldet, State ist falsch | State-Changes wirklich pruefen (Kalender/Code/DB); LLM-Judge ist angreifbar → deterministisch wo moeglich | §11 |
| 20 | Codex/OpenCode-Verbrauch scheint stark verschieden | Cache, Reasoning und gesamte Sitzung normalisieren; gleiche Modelle/Effort vergleichen. Stand 04.09.2026 | §12 |

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

## 9. Rate-Limit-Fehler: Retry-Storm & Thundering Herd (Runde 2)

**Symptom:** Bei 429/Provider-Fehlern explodieren Kosten und Latenz; „it's slow" fuer den Nutzer,
„it's expensive" auf der Rechnung. Ursachen: (a) **kein Jitter** → alle Clients retryen synchron
(„Thundering Herd"); (b) **Retry auf mehreren Schichten** → 3 Retries × 5 Service-Layer = **243
Backend-Calls** pro Request (~40 % der Cascading-Failures kommen daher); (c) **`Retry-After`-Header
ignoriert** → der Agent hammert weiter, statt die vorgegebene Wartezeit einzuhalten; (d) blindes
Retryen von 4xx (400/401/403 schlagen IMMER fehl).

**Fix (funktionserhaltend):**
- **`Retry-After`-Header parsen und einhalten** — zuverlaessiger als jede eigene Formel.
- **Full-Jitter-Backoff:** `sleep = random(0, min(cap, base·2^attempt))` (Cap 32-60s, max 3-5).
- **Retry-Budget:** globale Retries ≤ **10 %** aller Requests, sonst **fail-fast**.
- **Retry nur auf der aeussersten Schicht**; innere Layer propagieren Fehler sauber.
- **Nur 429/408/500-504 retryen**, nie 400/401/403/404.
- **Circuit Breaker** bei „consecutive 429s > N": 60s open mit exponential reopen (faengt Frameworks,
  die `Retry-After` ignorieren).
- **TPM ≠ RPM** getrennt deckeln; **immer `max_tokens`** setzen (eine Antwort kann das TPM-Budget leerfressen).

## 10. Sandbox-/Hardening-Fehler: Secret-Exfiltration & Container-Escape (Runde 2)

**Symptom:** Ein autonom laufender Agent fuehrt Code/Tool-Calls aus; bei Kompromittierung kann er
**API-Tokens exfiltrieren** (wenn sie in der Sandbox liegen und Egress offen ist) oder per
**Container-Escape** auf den Host ausbrechen (reale runc-CVEs: CVE-2019-5736, CVE-2024-21626).
Ohne Resource-Limits kann eine Agent-Session den **Host aushungern**, auch ohne Escape.

**Fix (funktionserhaltend):**
- **Zero-Secrets-Sandbox:** Credentials bleiben im **Control-Plane**, werden per Proxy injiziert —
  der Agent sieht nur Session-Token + Control-Plane-URL. Kurzlebige Credentials pro Session.
- **Default-Deny-Egress** (alles ausgehende blocken, nur noetige Endpunkte allowlisten) — schliesst
  den Exfiltrations-Pfad.
- **Container-Haertung:** Non-Root, `--cap-drop ALL`, `no-new-privileges`, seccomp + AppArmor,
  read-only Root + tmpfs.
- **Bei nicht-vertrautem Code: MicroVM statt Container** (Firecracker/gVisor/E2B) — Hardened-
  Container „work only when agents execute code you've reviewed and trust".
- **Resource-Limits (cgroup v2):** CPU/Memory-Hard-Cap, Disk-Quota, **Wall-Clock pro Tool-Call**.
- **Checkpoint vor destruktiven Aktionen** (~300 ms Snapshot) → Rollback statt Totalausfall.
> Hinweis Widerspruch in den Quellen: Egress „default-deny" vs. „opt-in" — bewusst entscheiden,
> sichere Wahl ist **default-deny**.

## 11. Eval-/Done-Fallen: „Done!" bei falschem State (Runde 2)

**Symptom:** Der Agent meldet Erfolg, aber das tatsaechliche Ergebnis stimmt nicht — die finale
Nachricht sagt „Meeting scheduled!", der Kalendereintrag fehlt aber (oder hat falsche Zeit). Eng
verwandt mit §7 (Premature/Wrong Termination), hier aus Eval-Sicht.
**Weitere Fallen:** LLM-as-Judge wird blind vertraut, obwohl er **angreifbar** ist („One token to
fool LLM-as-a-judge") und einen Threshold + Kalibrierung braucht; **Infra-Probleme**
(Timeouts/malformte Responses/stale Caches) werden als Reasoning-Fehler fehlinterpretiert.

**Fix (funktionserhaltend):**
- **State-Changes wirklich verifizieren:** bei „scheduled" den Kalender abfragen, bei Code den Code
  laufen lassen, bei DB-Update die Zeilen lesen. Drei Akzeptanz-Dimensionen: Response/Trajectory/State.
- **Deterministische Checks fuer Exaktes**, LLM-Judge nur fuer output-abhaengiges; Judge via Tracing
  + Human-Review kalibrieren.
- **Capability- von Regression-Evals trennen** (~100 % Regression-Gate schuetzt vor Backsliding).
- **Infra-Probleme zuerst ausschliessen**, bevor ein „Reasoning-Fehler" debuggt wird.
- **Eindeutiges „done":** „If two experts can't agree on pass/fail, the task needs refinement."

## 12. Messfallen beim Tokenvergleich von Codex CLI und OpenCode

**Stand:** 04.09.2026, 21:18 Uhr. Lokal Codex 0.153.2 / OpenCode 1.18.28. Methodische Fallen, keine Behauptung eines bestätigten Softwaredefekts. Recherche direkt mit Firecrawl, ohne MiniMax.

**Symptom:** Ein Programm erscheint deutlich sparsamer, obwohl nur unterschiedlich definierte Zähler oder ungleiche Aufgabenläufe verglichen wurden.

**Ursachen:** Kontextbelegung eines einzelnen Aufrufs wird mit kumuliertem Verbrauch verwechselt; Cache-Anteile fehlen oder werden doppelt addiert; Reasoning und Nebenagenten fehlen oder werden doppelt gezählt. Unterschiedliche Modellvarianten, Regeln, MCP-Tools und Kompaktierung erzeugen zusätzliche Unterschiede. Auch dieselbe Aufgabe garantiert nicht denselben Lösungsweg.

**Funktionserhaltende Korrektur:** Keine Werkzeuge blind deaktivieren. Zuerst gleiche Modell-ID, Effort, Ausgangsdateien und Erfolgskriterien festlegen. Alle Modellaufrufe einschließlich Hilfsaufgaben erfassen. Input/Cache/Output entsprechend der jeweiligen Provider-Semantik normalisieren. OpenAI-Cache ist eine Teilmenge des Inputs, Reasoning gehört zum API-Output. Kosten und Aboverbrauch getrennt beurteilen. Mehrere erfolgreiche Vergleichsläufe statt eines einzelnen Prozentsatzes verwenden.

**Konkreter Befund:** Ein veröffentlichter Einzelversuch mit Codex 0.144.3 / OpenCode 1.17.20, GPT-5.6 Terra medium, meldet 108.453 versus 69.864 Input-Tokens über zwei Programmieraufträge, also rund 55 % mehr bei Codex. Beide Lösungen bestanden laut Autor die unabhängigen Prüfungen. Diese Messung ist kein Beweis für denselben Faktor bei GPT-6 Astra oder neueren Clients. Kein eigener Benchmark durchgeführt.

**Versionsfalle:** Die am Prüftag abgerufene OpenCode-Konfigurationsdokumentation nennt `compaction.prune` standardmäßig false. Ältere lokale Juni-Notizen behaupten true. Auf diesem Rechner ist true ausdrücklich gesetzt; daraus folgt keine Aussage über andere Installationen.

**Quellen:** `offiziell` [OpenAI Prompt Caching](https://developers.openai.com/api/docs/guides/prompt-caching), [Reasoning](https://developers.openai.com/api/docs/guides/reasoning), [Codex-Nutzung](https://developers.openai.com/codex/pricing), [OpenCode-Konfiguration](https://opencode.ai/docs/config/); `extern, Primärbericht` [Benchmark vom Durchführenden](https://github.com/earendil-works/pi/discussions/6646).

**Hook-Abdeckung:** bestehender kuratierter Bereich `agents/loop-engineering`; rekursiver Index und beide Hint-Schreibweisen vorhanden. Guard-Erweiterung entfällt bei dieser bereichsübergreifenden Messmethodik.

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
| §9 Rate-Limit / Retry-Storm | §8 Rate-Limit-Resilienz (3-Schichten, Retry-After, Jitter) |
| §10 Sandbox / Secret-Exfiltration / Escape | §9 Hardening & Sandboxing (Zero-Secrets, MicroVM, cgroup) |
| §11 Eval-/Done-Fallen | §10 Definitions-of-done & Eval-Harness |

## Quellen (Runde 1, 2026-06-24)
- Data Science Dojo — „10 Loop Engineering Design Patterns" (Infinite Loops, Terminal-States)
- Produktions-Leitfaden „failure modes in production agents" (Loop-Breaker, Cost, Reward-Hacking, Drift, Dashboard)
- Anthropic — long-running agents / „demystifying" (Cold-Start-Amnesia, Progress-as-completion, False-E2E)
- Random Labs Slate (Working-Memory-Rot, Lossy-Compaction)
- IBM Research (Memory-Pointer: 20M→1.234 Tokens)
- VPS-Produktions-Checkliste (Container-uptime-≠-Agent-uptime)

## Quellen (Runde 2, 2026-06-24)
- Portkey / Truefoundry / tianpan.co / dev.to / Maxim AI — Rate-Limit-Resilienz (Retry-Storm, Thundering Herd, Circuit Breaker)
- Sandbox-Hardening-Guides (runc-CVEs, Zero-Secrets-Sandbox, Default-Deny-Egress, Firecracker/gVisor/E2B)
- LangChain / OpenAI-Cookbook / Confident-AI — Agent-Evals (State-Changes pruefen, LLM-Judge-Caveats, Regression-Gates)

---

## Nachtrag 2026-09-09 — Endlosschleifen und Selbstkorrektur (Recherche mit 21 Researchern)

> Ergaenzung aus der Recherche vom 2026-09-09 (drei Engines parallel). Neuer Nachbarbereich:
> `bugs/agents/multi-agent-interop.md` (Protokolle, Cross-Vendor, Schreibkonflikte).

### Endlosschleifen sind kein Randfall — sie sind haeufig

**Studie:** "When Agents Do Not Stop: Uncovering Infinite Agentic Loops in LLM Agents"
(arXiv 2607.01641) fand **68 bestaetigte Endlosschleifen-Fehler in 47 Projekten** aus 6.549
durchsuchten Repositories (91,9 % Precision der Analyse).

**Ursachenverteilung:**

| Ursache | Anteil |
|---------|--------|
| Tool-gesteuerte Retries ohne Grenze | 41,2 % |
| Modellgesteuerte Terminierung ohne harten Fallback | 38,2 % |
| Fehlende Exits | 33,8 % |
| Ungeprueufte Workflow-Zyklen | 30,9 % |

**Nach Framework:** LangGraph 33,8 %, AutoGen 32,4 %, LlamaIndex 8,8 %, LangChain 7,4 % — zwei
Drittel aller Faelle entfallen auf die ersten beiden.

**Folgen:** API-Kosten-Exzess (95,6 % der Faelle), faktischer Denial-of-Service (95,6 %),
Context-Window-Erschoepfung (27,9 %).

**Fix:** Das Abbruchkriterium gehoert **deterministisch in den Code**, nie ins Modell. Konkret:
- harte `max_iterations` bzw. Recursion-Limit im State, das unabhaengig vom Modell-Output greift
  ("force return to END no matter what the LLM wants"). Praxis-Default 10-15, LangChain-Default 15.
  Die Grenze nicht reflexhaft erhoehen, sondern erst pruefen, ob in den letzten Iterationen echter
  Fortschritt stattfand.
- Tool-Call-Limiter (z. B. max. 10 gleiche Aufrufe in Folge, 50 gesamt) und Timeout je Ausfuehrung.
- Stop-Bedingungen **binaer und messbar** formulieren ("Test-Coverage > 80 %", "JSON validiert"),
  nie subjektiv ("gut genug", "umfassend genug"). Begruendung aus der Praxisliteratur: LLMs wollen
  hilfsbereit klingen, und "lass uns hier aufhoeren" wirkt nicht wie eine hilfreiche Antwort.
- Bei Plan-Execute-Replan ein **Bounded-Recovery-Protokoll**: erst Retry, dann lokaler Patch, erst
  danach vollstaendiges Replanning. Verhindert das Muster, dass ein Modell immer neu plant ohne
  Fortschritt (arXiv 2603.11445).
- Als Konvergenz-Heuristik fuer Refine-Loops: Embedding-Aehnlichkeit zwischen aufeinanderfolgenden
  Ausgaben (~0,92 Schwelle) UND "die Kritik enthaelt keine neuen Punkte mehr". Der finale
  Kill-Switch muss deterministischer Code sein, nie ein Modellurteil.

### Selbstkorrektur ohne externes Feedback verschlechtert oft

**Symptom:** Ein Agent prueft sein eigenes Ergebnis nach, haelt es faelschlich fuer korrekt — oder
verschlechtert es sogar.

**Ursache und Beleglage:**
- Huang et al., "Large Language Models Cannot Self-Correct Reasoning Yet" (arXiv 2310.01798, ICLR
  2024): "LLMs struggle to self-correct their responses without external feedback, and at times,
  their performance even degrades after self-correction."
- Das Problem liegt im **Finden**, nicht im **Beheben**: "LLMs cannot find reasoning errors, but can
  correct them given the error location" (arXiv 2311.08516). Ein separat trainierter kleiner
  Klassifikator findet Fehler besser als das grosse Modell per Prompting.
- Strukturell: wenn Generator und Bewerter dasselbe Modell sind, teilen sie dieselben Fehlermodi —
  die Selbstbewertung ist dann kein Korrektheitssignal ("when generator and evaluator share failure
  modes, self-evaluation provides weak evidence of correctness").
- **Self-Preference-Bias** (arXiv 2410.21819): Bewerter-LLMs bevorzugen systematisch eigene Outputs,
  korreliert linear mit der Selbst-Erkennungsgenauigkeit und verstaerkt sich durch Fine-Tuning auf
  Selbst-Diskriminierung. Langes Chain-of-Thought vor dem Urteil reduziert den Bias.

**Fix:** Bewertung strukturell vom Generator trennen — deterministischer Test, externer Kritiker
oder ein Klassifikator mit anderen Fehlermodi. **Nicht** mehr Runden desselben Modells.
Ein spezialisiertes Kritiker-Modell brachte in einer Arbeit +46,03 % (Llama-3-70B) gegenueber
Diskriminator- und Selbstkritik-Baselines (arXiv 2503.16024).

**Wie viele Runden lohnen sich?** Sekundaerquellen zu Self-Refine nennen ein Plateau bei **2-4
Runden**; das arXiv-Original (2303.17651) liess sich nicht bis zu den Tabellen auslesen, die Zahl ist
also **nicht primaerquellenverifiziert** und aufgabenabhaengig. Multi-Agent-Debate konvergiert
typischerweise in **4-8 Runden**; ein adaptives Kriterium (Kolmogorov-Smirnov-Test zwischen den
Antwortverteilungen aufeinanderfolgender Runden, Schwelle < 0,05 ueber zwei Runden) spart Rechenzeit
bei −0,13 % bis 0,00 % Genauigkeitsverlust gegenueber fixen 10 Runden (arXiv 2510.12697).

**Gegenlaeufiger Befund, ehrlich vermerkt:** "LLaVA-Critic-R1" (arXiv 2509.00676) findet, dass
selbstkritik-basiertes Test-Time-Scaling in ihrem Setup die beste Leistung erzielt. Die Diskrepanz
zum Kritiker-Vorteil ist nicht aufgeloest und vermutlich stark task-/architekturabhaengig.

**Reflexion richtig gebaut:** Reflexion (arXiv 2303.11366) laeuft, "until the Evaluator deems the
trajectory to be correct" — das Abbruchkriterium ist also ausdruecklich ein **externer Evaluator**,
nicht das Modell, das sich selbst fuer fertig erklaert.

### Debate laeuft in die Echo-Kammer

**Symptom:** Mehrere Agenten diskutieren und einigen sich auf eine falsche Mehrheitsmeinung.

**Ursache:** "Multi-LLM debates are susceptible to 'echo chamber' effects, where a flawed majority
opinion arising from shared misconceptions can suppress correct minority viewpoints." Bereits ein
moderater Anfangs-Bias kann sich durch die Interaktion verstaerken (arXiv 2608.02827).

**Fix:** **Diversitaet ist der Hebel, nicht die Rundenzahl.** "Intrinsic reasoning strength and group
diversity are the dominant drivers of debate success, while structural parameters such as order or
confidence visibility offer limited gains." Praktisch: verschiedene Modelle oder verschiedene
Blickwinkel einsetzen, nicht dasselbe Modell mehrfach befragen.

Quellen: https://arxiv.org/html/2607.01641v1 · https://arxiv.org/abs/2310.01798 ·
https://arxiv.org/pdf/2311.08516 · https://arxiv.org/abs/2410.21819 · https://arxiv.org/html/2510.12697v1 ·
https://arxiv.org/pdf/2503.16814 · https://arxiv.org/html/2603.11445v2 · https://arxiv.org/abs/2303.11366 ·
https://www.mindstudio.ai/blog/agent-loops-verifiable-stop-conditions
