# Loop Engineering Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
