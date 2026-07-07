# OpenRouter API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Attribution | `HTTP-Referer` + `X-Title` im Client-Konstruktor (sonst Content leer) | §1 |
| 2 | Routing | `provider.order` + `allow_fallbacks:false` für Determinismus | §2 |
| 3 | Qualität/Datenschutz | `quantizations`-Allowlist, `data_collection:"deny"` für Compliance | §3 |
| 4 | Kosten/Speed | `:floor`/`:nitro` bzw. `sort`; `max_price` als Preisdeckel | §4 |
| 5 | Modell-Routing | `anbieter/modell`, Liste via `/api/v1/models`, `models`-Fallback-Array | §5 |
| 6 | Streaming/SSE | `:`-Kommentare überspringen, `[DONE]` + Mid-Stream-`error` prüfen | §6 |
| 7 | Tool Calling | OpenAI-Shape; Args pro `index` über Chunks akkumulieren | §7 |
| 8 | Output/Limits | `json_schema` + `strict:true`; `:free`=20 RPM/50 RPD; 402 ≠ 429 | §8 |
| 9 | **Claude Code anbinden** ⭐ | `ANTHROPIC_BASE_URL=https://openrouter.ai/api` (NICHT `/api/v1`!), `ANTHROPIC_API_KEY=""`, Key in `ANTHROPIC_AUTH_TOKEN`; **kein Proxy nötig** | §9 |
| 10 | Andere CLI-Agenten | Modell-String-Format zuerst klären: LiteLLM-Tools `openrouter/<v>/<m>`, Eigenbau-Tools nacktes `<v>/<m>` | §10 |
| 11 | **Schnelle Modelle** ⭐ | Interaktives Coding → `sort:"latency"` (TTFT), nicht `:nitro` (=Throughput); `:exacto` für Tool-Calling | §11 |
| 12 | Caching/Reasoning | Anthropic braucht `cache_control`-Breakpoints; `reasoning_details` beim Tool-Calling unverändert zurückgeben | §12 |
| 13 | Neue Features | Presets (`@preset/slug`), BYOK (5% Fee), ZDR pro Provider, Response-Caching, `openrouter:web_search` | §13 |
| 14 | Cloud vs. lokal | OR hostet NIE lokal; mischen via claude-code-router/LiteLLM; lokal = nur base_url-Tausch (gleicher Code) | §14 |
| 15 | Account/Keys/Ops | Inference- vs. Management-Key; per-Key-`limit`; Rate-Limits GLOBAL pro Account | §15 |
| 16 | **OpenCode anbinden** ⭐ | `/connect`→OpenRouter (Key in `~/.local/share/opencode/auth.json`); Modell `openrouter/<v>/<m>`; **kein `opencode auth login`/`OPENROUTER_API_KEY`** | §16 |
| 17 | **OpenCode Routing durchreichen** ⭐ | `provider.openrouter.models.<m>.options.provider` = OR-`provider`-Objekt 1:1 (snake_case: `order`,`sort`,`data_collection`…) | §16 |
| 18 | **Modell pro Aufgabe** ⭐ | Pro `agent` eigenes `model`; teuer (Opus/GPT-5) für Architektur, billig (Flash/Haiku/Grok-Fast) für Recherche/Boilerplate; `small_model` billig | §16/§17 |
| 19 | **Account-Settings** ⭐ | ≥$10 Guthaben (1000 statt 50 `:free`-RPD) + Auto-Topup; Training-Toggle bewusst; Per-Key-Limit; Preset fürs Default-Routing | §18 |
| 20 | Param-Updates 06-18 | `transforms`→Plugin `context-compression` (auto ≤8k); `:online` deprecated; `usage.include` wirkungslos (immer an); `max_tokens`→`max_completion_tokens` | §19 |
