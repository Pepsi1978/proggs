# OpenRouter (Custom-API-Provider) in der Claude Code CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Grundprinzip | Claude Code spricht NUR die Anthropic-API; OpenRouter-"Anthropic Skin" unter `openrouter.ai/api`. `ANTHROPIC_BASE_URL`=WOHIN + `ANTHROPIC_*_MODEL`=WELCHES — beide setzen | §1 |
| 2 | Base-URL | OHNE `/v1` (`https://openrouter.ai/api`) — `/v1` ist die OpenAI-URL, falsch fuer die CLI | §3 |
| 3 | `ANTHROPIC_API_KEY` | explizit auf `""` leeren (nicht nur unset) — sonst Auth-Konflikt / Fallback zu Anthropic | §3 |
| 4 | Modell-Tiers | ALLE drei (Haiku/Sonnet/Opus) `DEFAULT_*_MODEL` + `SUBAGENT_MODEL` mappen — sonst 404 im Hintergrund/Subagent | §2, §3 |
| 5 | Beta-Header | `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1` — sonst 400 durch Anthropic-exklusive Beta-Header am Gateway | §3 |
| 6 | Caching/Kosten | Prompt-Caching wirkt NUR am nativen Anthropic-Endpoint; fuer reines Claude oft Anthropic-direkt billiger | §3 |
| 7 | Modell wechseln | Laufzeit nur `/model <slug>` (Env greift erst nach Neustart); parallel je Terminal `claude --model <slug>` | §4 |
| 8 | Wahrheits-Check | `/status`: Base URL = `openrouter.ai/api`, Auth via `ANTHROPIC_AUTH_TOKEN` | §2 |
| 9 | Task→Modell-Routing | Bordmittel koennen KEIN Task-Routing → `claude-code-router` (CCR); Reasoning-Modelle nur auf Rolle `think` (sonst 400) | §5 |
| 10 | Settings schreiben | UTF-8 OHNE BOM; Token in `settings.local.json` (gitignored), Config an EINER Stelle | §2 |
| 11 | Zurueck zu Anthropic | `ANTHROPIC_BASE_URL` + `ANTHROPIC_AUTH_TOKEN` WIRKLICH entfernen (nicht nur `/logout`), neue Konsole | §6 |
