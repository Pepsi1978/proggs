# OpenRouter (Custom-API-Provider) in der Claude Code CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter Arbeit
> hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der Schnell-
> Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Base-URL setzen | `https://openrouter.ai/api` — **OHNE `/v1`**, ohne trailing slash | §2.1 |
| 2 | „model not found" trotz korrektem Key | `ANTHROPIC_API_KEY=""` **explizit leeren** (nicht nur unset) | §3.1 |
| 3 | „model may not exist" mitten in Session | Background-Modell vergessen → alle 3 `ANTHROPIC_DEFAULT_*_MODEL` setzen | §4.1 |
| 4 | 400 beim Start mit MCP/Tool-Search | `tool_reference`-Bug → gefixt ab 2.1.70; sonst `ENABLE_TOOL_SEARCH` aus | §6.1 |
| 5 | 400 „context-management/prompt-caching-scope" Beta | `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1` | §6.2 |
| 6 | Kosten explodieren in langen Sessions | Prompt-Cache geht ueber OpenAI-Wire verloren → nativer Anthropic-Endpoint | §6.4 |
| 7 | Abbruch nach 5 Min bei langsamem Modell | `API_TIMEOUT_MS` hoch (gefixt 2.1.106) + `API_FORCE_IDLE_TIMEOUT=0` (2.1.169) | §7 |
| 8 | settings.json wird ignoriert (Windows) | UTF-8-**BOM** bricht Parse → BOM-frei speichern (`claude-config.md §3.2`) | §10.3 |
| 9 | OpenRouter-Slug nicht im `/model`-Picker | `ANTHROPIC_CUSTOM_MODEL_OPTION="<slug>"` setzen | §5.2 |
| 10 | Modellwechsel zur Laufzeit wirkt nicht | Env-Var greift erst nach Neustart; live nur per `/model` | §5.3 |
| 11 | Subagent laeuft auf falschem Modell | `CLAUDE_CODE_SUBAGENT_MODEL="<slug>"` setzen | §5.4 |
| 12 | Auto-Routing pro Aufgabe gewuenscht | Geht NUR mit Proxy (claude-code-router) — Bordmittel nur grob | §11 |
| 13 | Git Bash macht aus `/api` einen Pfad | `MSYS_NO_PATHCONV=1` ODER Vars als Windows-User-Env setzen | §10.7 |
| 14 | Zurueck zu Anthropic, aber Calls falsch | `/logout` reicht NICHT — `ANTHROPIC_BASE_URL` wirklich entfernen | §10.4 |
| 15 | `/fast` blockiert ueber OpenRouter | `CLAUDE_CODE_SKIP_FAST_MODE_ORG_CHECK=1` (ab 2.1.96), nur Opus 4.6+ | §5.7 |
