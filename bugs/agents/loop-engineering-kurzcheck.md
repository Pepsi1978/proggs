# Loop Engineering (autonome Agenten-Schleifen) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
