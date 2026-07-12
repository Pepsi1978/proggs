# Serverseitige autonome KI-Agenten (eigene Tool-Loop / Pydantic-AI / LangGraph) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Agent ruft Tools endlos / "max iterations" / Re-Execution nach Erfolg | HARTER, deterministischer Hard-Stop (max Turns/Zeit) als Notbremse — UNABHAENGIG vom LLM. Echtes "done"-Kriterium definieren | §1.1 |
| 2 | Selbe Tool-Kette wiederholt sich | Schleifen-Erkennung: Duplicate-Chain + No-Progress (N Schritte ohne Fortschritt) → abbrechen | §1.2 |
| 3 | ⭐ Kosten/Token laufen weg ("nur Alert") | Cap muss STOPPEN, nicht mailen. Token-Budget/Session, harter Tages-/Monats-Cap (siehe self-hosted §2.1) | §2.1 |
| 4 | Kontext waechst bis Limit, Agent "vergisst"/bricht ab | Jeder Schritt sendet vollen Verlauf → superlinear. History trimmen (Sliding-Window/Summarize), Tool-Output per Pointer auslagern | §2.2 |
| 5 | ⭐ Blockierender sync-Call in `async def` friert GANZEN Server ein | `run_in_threadpool`/`asyncio.to_thread` fuer sync httpx/DB/CPU; nie blocking direkt im Event-Loop | §3.1 |
| 6 | Tool wirft Exception → ganzer Lauf bricht ab | Exception FANGEN und als tool_result-Fehlertext ans LLM zurueck (es kann retryen), nicht crashen. Timeout pro Tool | §3.2 |
| 7 | ⭐ HTTP 400 `tool_use ... without tool_result` nach Tool-Call | tool_use/tool_result strikt 1:1 paaren; bei History-Trimming/Compaction Paare ATOMAR behandeln; abgebrochene Tool-Calls sauber abschliessen | §4.1 |
| 8 | Parallele Sessions schreiben denselben State/Memory | shared mutable state = Locking-Pflicht; DB-backed State statt naivem In-Memory-Dict | §5.1 |
| 9 | ⭐ Schreib-Tool doppelt ausgefuehrt bei Retry | Idempotency-Key pro Write + DB-Unique-Constraint/SELECT FOR UPDATE; von Anfang an einbauen | §5.2 |
| 10 | ⭐ Gespeicherter Memory-Inhalt manipuliert den Agent (Lethal Trifecta) | Eingaben UND gespeicherte Memory-Inhalte als UNTRUSTED behandeln; Trifecta brechen; kein 100%-Fix → Defense-in-Depth | §6.1 |
| 11 | 429 → alle Retries gleichzeitig (thundering herd) | Exponential Backoff + FULL Jitter, `Retry-After`-Header respektieren, `isRetryable` (nur 429/5xx), maxRetries | §6.2 |
| 12 | Secret im Log/Traceback/Error-Response | Secrets nie in den LLM-Kontext/Prompt; Redaction vor dem Loggen; FastAPI-500 gibt im Debug Stacktrace im Body → in Prod aus | §6.3 |
| 13 | Pydantic-AI: `Exceeded maximum retries for ... validation` | Structured-Output-Validation-Loop (#1192/#734 NOT_PLANNED/COMPLETED); JSON-Mode erzwingen, `UsageLimits` setzen | §7 |
| 14 | LangGraph: `operator.add`-Reducer dupliziert / Liste ueberschrieben | Reducer bewusst waehlen; bei paralleln Writes kein blindes `add` (exponentielle Duplikation); GraphRecursionError = echtes Loop-Symptom | §8 |
| 15 | In-Memory-State bei mehreren uvicorn-Workern weg | Worker = eigener Prozess, KEIN geteilter RAM. Session/State in DB/Redis, nicht in Modul-Dict | §5.3 |
| 16 | TTS liest URLs/Quellen, die im finalen Chattext fehlen | Keine ungeprueften Modelldeltas an TTS; erst finalisieren, dann EINEN kanonischen Reply fuer Anzeige, Persistenz und Sprache verteilen | §9.1 |
