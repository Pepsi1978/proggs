# OpenRouter-Integration & Modellauswahl Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Einrichten | OpenRouter ist EINGEBAUTER Provider (kein npm/baseURL/Block noetig): `/connect` → OpenRouter → Key (`sk-or-...`) → `/models`. Key in `~/.local/share/opencode/auth.json` | §1 |
| 2 | Modell-ID-Format | Global (`model`/`--model`): `openrouter/<author>/<model>` (DREITEILIG); in `provider.openrouter.models`: nur `<author>/<model>` ohne `openrouter/` | §2 |
| 3 | small_model | kleines Modell fuer Nebenaufgaben (Session-Titel) auf guenstiges OpenRouter-Modell setzen | §2 |
| 4 | Provider-Routing | `options.provider` wird 1:1 an OpenRouter durchgereicht. `order`/`sort` = strikt + Load-Balancing AUS; `allow_fallbacks:true` lassen (Resilienz) | §3 |
| 5 | Routing-Shortcuts | Slug-Suffix `:nitro`=throughput, `:floor`=price; harter `max_price`-Filter; `require_parameters` fuer Tool-Calling-faehige Provider | §3 |
| 6 | Tool-Calling KRITISCH | NUR Modelle mit `supported_parameters=tools` als Haupt/Subagent — kleine/Free-Modelle oft unzuverlaessig im Agent-Loop | §5, §7 |
| 7 | Caching | DeepSeek/Gemini/OpenAI cachen automatisch; Anthropic+Qwen brauchen `cache_control`. Stabile, KURZE AGENTS.md → hoehere Cache-Hit-Rate | §4 |
| 8 | Budget-Coding-Modelle | Haupt(Budget): `qwen3-coder` / `z-ai/glm-4.6`; Small: `deepseek-v4-flash` / `v3.2`; Premium: Sonnet/Opus 4.5, GPT 5.2, Gemini 3 Pro | §5 |
| 9 | Free vs Paid | `:free` = 50/Tag (<10 Credits) bzw. 1000/Tag (ab einmalig $10 Credits), 20/Min; NICHT produktionssicher, kein SLA | §6 |
| 10 | Privacy (Firmencode) | `data_collection:"deny"` + ggf. `zdr:true`; mehr Keys/Accounts erhoehen Rate-Limits NICHT | §6 |
| 11 | Preise | aendern haeufig → vor Einsatz `openrouter.ai/models` bzw. `opencode models openrouter --refresh` pruefen | Kopf |
