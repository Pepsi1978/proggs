# Agents Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Custom-Agent starten | `subagent_type:"general-purpose"` + Prompt, NIE Custom-`subagent_type` | Agent-Tool |
| 2 | Subagent-Modell | via `CLAUDE_CODE_SUBAGENT_MODEL` (überschreibt Frontmatter) → Policy `opus[1m]` | Modell-Aufloesung |
| 3 | Parallelitaet | 3–5 Subagents Sweet-Spot; Workflows max 16 gleichzeitig / 1000 total | Parallelisierung |
| 4 | Subagent-Crash (`Prompt too long`) | `tools:`-Whitelist + `ENABLE_TOOL_SEARCH`; kein Auto-Compact → Orchestrator-Resume | Frontmatter / Modell |
| 5 | Agent Teams | nur wenn Teammates kommunizieren (3–4x teurer) | Agent Teams |
| 6 | Dynamic Workflows | deterministische Orchestrierung; Agenten erben Session-Modell | Dynamic Workflows |
| 7 | Memory | `memory`-Feld nur fuer Agents, die ueber Sessions lernen sollen | Memory-Persistenz |
