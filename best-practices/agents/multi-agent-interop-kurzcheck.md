# Multi-Agent-Interop Kurzcheck (Best Practices)

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT
> (`best-practices/agents/multi-agent-interop.md`), nicht nur diese Kurzfassung.
> Stand: 2026-09-09. Gegenseite: `bugs/agents/multi-agent-interop.md`.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Agent braucht Werkzeuge/Daten | **MCP** — die vertikale Achse. Etabliert, Spec 2026-07-28, unter der Agentic AI Foundation (Anthropic + OpenAI + Block) | §1 |
| 2 | Agent soll anderen Agenten beauftragen | **A2A** — die horizontale Achse. v1.0/1.0.1, 150+ Organisationen. ABER: Anthropic fehlt in der Traegerliste | §2 |
| 3 | "ACP" in einer Quelle gelesen | Zwei Protokolle mit dem Kuerzel (Zed = Editor, IBM/BeeAI = Agent↔Agent) — erst klaeren welches | §3 |
| 4 | OpenAI-Handoffs fuer Cross-Vendor geplant | Geht nicht. SDK-intern, bleibt "within a single run", keine Aussage zu Claude/Gemini | §4 |
| 5 | Claude ↔ GPT am selben Projekt | 2026 belastbarster Weg: gemeinsames Dateisystem/Queue, KEIN Protokoll | §5 |
| 6 | Dateibasierte Uebergabe bauen | Atomic Claim (`O_CREAT\|O_EXCL`) + Output-Existenzcheck. Ledger allein bricht bei Parallelitaet | §5 |
| 7 | "Ledger" in einer Anleitung | Kein Industriestandard — jede Quelle meint etwas anderes. Immer dazusagen was gemeint ist | §5 |
| 8 | Heterogene CLI-Agenten orchestrieren | Headless-Subprocess (`claude -p`, `codex exec`) + Queue mit atomaren Claims. Vorbild: NEEDLE | §6 |
| 9 | Claude Code von aussen ansprechen | `claude mcp serve` exponiert seine Werkzeuge ueber MCP — "Agent im Agent" | §6 |
| 10 | Parallel im selben Repo arbeiten | Git-Worktrees (`isolation: worktree`), flache Geschwister-Ordner, Grenze 8-10 | §6 |
| 11 | Schreibpfade zwischen Agenten trennen | `PreToolUse`-Hook blockt Schreibzugriffe ausserhalb der eigenen Worktree | §6 |
| 12 | OpenCode-Subagent soll Session-Modell erben | Genau das ist der Default — Modell nur setzen, wenn man es wirklich abweichen lassen will | §6 |
| 13 | Kosten schaetzen | 3-Agenten-Pipeline ≈ 3x Tokens; Tool-Schemas 60-80 % des Verbrauchs; im Lasttest messen | §7 |
| 14 | Fremden MCP-Server einbinden | Strikt whitelisten + Tool-Beschreibungen sanitizen. Tool Poisoning ist real und ungeloest | §8 |
| 15 | A2A-Tokens vergeben | Auf einzelne Skills scopen, kurze Ablaufzeit selbst erzwingen — die Spec tut es nicht | §8 |

**Merksatz:** MCP verbindet einen Agenten **nach unten** mit seinen Werkzeugen, A2A **zur Seite** mit
anderen Agenten. Wer die beiden verwechselt, baut die falsche Bruecke.

**Zweiter Merksatz:** Fuer Anthropic ↔ OpenAI gibt es 2026 **kein abgesichertes Protokoll**. Das
gemeinsame Dateisystem mit atomaren Claims ist nicht die elegante, aber die tragfaehige Loesung.
