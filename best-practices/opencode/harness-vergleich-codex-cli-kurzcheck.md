# Harness-Vergleich Codex CLI vs. OpenCode — Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | "Kostet dasselbe Modell in Codex mehr Tokens?" | Grundlast/Turn: Codex CLI ~2–5k, OpenCode ~7k (Claude Code ~33k). Codex ist also **sparsamer**, nicht teurer | §1 |
| 2 | Wo Tokens wirklich verbrennen | eigene AGENTS.md/CLAUDE.md (72 KB = +20k/Anfrage) und MCP-Server (5 Server = +5–7k). Das schlaegt den Harness-Unterschied um Laengen | §1 |
| 3 | Der echte Kostenhebel | **Prompt-Caching**: Codex baut ein stabiles Prefix (41–80 % Ersparnis, 13–31 % schnellerer Start). Im Fremd-Harness faellt der Cache-Vorteil teilweise weg | §2 |
| 4 | Cache kaputtmachen | jede Aenderung an Regeldatei/Tool-Liste/CLI-Version invalidiert das Prefix. `/usage` (Codex ab 0.55.0) zeigt Cache-Hits | §2 |
| 5 | Lange Sessions | Codex loescht bei Compaction das Original **physisch** (Detail weg); OpenCode **versteckt** nur (History bleibt im Storage) | §3 |
| 6 | Kontextfenster real | Codex kappt GPT-5.5 von 1.050k auf 400k, minus 128k Output, minus 5 % Headroom = **~258k nutzbar**. Nicht 1M | §3 |
| 7 | "Ist das Modell in Codex intelligenter?" | Ja, messbar — aber wegen des Harness, nicht des Modells: GPT-5.5 auf Terminal-Bench 83,4 % (Codex) vs. 78,2 % (anderer Harness) | §4 |
| 8 | Warum (Hauptgrund) | GPT-5.3-Codex+ ist auf **`apply_patch`/V4A-Diffs post-trained** (auch auf `rg` statt `grep`). Fremd-Harness erwartet String-Replace → Tool-Mismatch | §4 |
| 9 | Zweiter Grund | AGENTS.md-Qualitaet: von Hand geschrieben +4 %, LLM-generiert **-20 %** (ETH Zuerich). Der Harness entscheidet, was eingelesen wird | §4 |
| 10 | Nur Codex CLI | OS-Sandbox (greift auch auf Subprozesse), Approval-Policy, `/review`, Cloud-Tasks, `@codex` auf GitHub, First-Party-Websuche mit OpenAI-Index, ChatGPT-Abo statt API-Key | §5 |
| 11 | Nur OpenCode | 75+ Provider inkl. lokal (Codex kann kein lokales Llama), verlustfreie Compaction, LSP, Plugins, als MCP-Server exponierbar, reiches TUI, keine Telemetrie | §6 |
| 12 | Entscheidung | Ein OpenAI-Modell + Diff-Treue + Abo → **Codex CLI**. Modelle mischen, lange Sessions, eigenes Werkzeug-Oekosystem → **OpenCode** | §7 |
| 13 | Vorsicht bei den Zahlen | Grossteil der Vergleichswerte aus **einer** Codex-affinen Quelle; kein sauberer oeffentlicher Head-to-Head Codex vs. OpenCode existiert | §8 |
| 14 | `gpt-6-astra` | in keiner oeffentlichen Quelle belegt; alles hier gilt fuer GPT-5.x-Codex und ist nur uebertragen | §8 |
