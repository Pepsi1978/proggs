# OpenRouter API — Best Practices (Stand 2026-06-17)

> Gegenstueck zu `bugs/apis/openrouter-api.md`. Offiziell empfohlen (Quellen).
> Basis-Recherche 2026-06-09 (§1–§8, rohe API-Integration). **Erweiterung 2026-06-17**
> (§9–§15): Schwerpunkt **OpenRouter fuer CLI-/Coding-Harnesses** (Claude Code & Co.),
> **schnelle/Cloud-Modelle** und **Cloud-vs-lokal** — Researcher-Schwarm-Recherche.

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

## 1. Unified Gateway & Attribution-Header
- Base-URL `https://openrouter.ai/api/v1`, Auth via `Authorization: Bearer <OPENROUTER_API_KEY>`; als OpenAI-Drop-in den Base-URL umstellen, sonst offizielle SDKs (`@openrouter/sdk`, `openrouter`) fuer Typsicherheit. Quelle: https://openrouter.ai/docs/quickstart · offiziell
- `HTTP-Referer` (deine Site) + `X-Title` (App-Name) bei jedem Request mitsenden — aktiviert Leaderboard/Attribution; ohne werden Antworten bei Non-localhost-Keys still leer. Header im Client-Konstruktor (`default_headers`) setzen, nicht per Request-Override. `X-OpenRouter-Title` ist der neue Anzeigename-Header, `X-Title` nur noch rückwärtskompatibel. Quelle: https://openrouter.ai/docs/quickstart · https://openrouter.ai/docs/app-attribution · offiziell

## 2. Provider-Routing bewusst steuern
- Fuer deterministisches Verhalten `provider.order: ["anthropic","openai"]` + `provider.allow_fallbacks: false` setzen — verhindert stille Substitution auf abweichendes Backend. Provider-Slugs per Copy-Button von der Modell-Seite holen (inkl. Varianten wie `deepinfra/turbo`). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.require_parameters: true`, sobald `response_format`, Tools, `stop` o.ae. genutzt werden — routet nur zu Providern, die ALLE Parameter unterstuetzen (sonst werden sie still ignoriert). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.only`/`provider.ignore` als Allow-/Blocklist; sie mergen mit den Account-weiten Einstellungen. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 3. Qualitaet & Datenschutz absichern
- `provider.quantizations: ["fp16","bf16"]` (oder `fp8`) als Allowlist gegen Garbage-/Encoding-Output durch Int4/FP4-Backends. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.data_collection: "deny"` schliesst Provider aus, die Daten speichern/trainieren — fuer Compliance mit `require_parameters: true` kombinieren. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 4. Kosten- & Geschwindigkeits-Routing
- `:floor`-Suffix (= guenstigster Provider) fuer kostensensible Produktion mit `allow_fallbacks: true`; `:nitro`-Suffix bzw. `provider.sort: "throughput"` fuer geschwindigkeitskritische Pfade. Beide Suffixe/`sort` deaktivieren das Default-Load-Balancing. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.max_price: {prompt: 1, completion: 2}` als Preisdeckel ($/M Token); blockt zu teure Provider, gut mit `sort` kombinierbar. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 5. Modell-Routing & Fallback-Array
- Modell-String `anbieter/modell`; Modell-Liste dynamisch via `/api/v1/models` beziehen statt hardcoden (Slugs, besonders `:free`, verschwinden). Quelle: https://openrouter.ai/docs/quickstart · offiziell
- Fuer Resilienz `models`-Array (Fallback-Kette) angeben; `openrouter/auto` (Auto-Router) waehlt bei Tool-Calls per Tool-Call-Error-Rate ("Auto Exacto") zuverlaessige Provider. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell

## 6. Streaming / SSE robust verarbeiten
- SSE-Kommentarzeilen (`: OPENROUTER PROCESSING`) per Spec ueberspringen, `[DONE]`-Sentinel vor JSON-Parse abfangen; etablierten Parser nutzen (`eventsource-parser`, OpenAI-SDK, Vercel AI SDK). Quelle: https://openrouter.ai/docs/api/reference/streaming · offiziell
- Mid-Stream-Fehler kommen als SSE-Event mit Top-Level-`error` + `finish_reason: "error"` bei HTTP 200 — in jedem Chunk pruefen. Usage-Stats stehen im letzten Chunk (`usage`-Feld). Cancel via AbortController stoppt Abrechnung bei unterstuetzten Providern. Quelle: https://openrouter.ai/docs/api/reference/streaming · offiziell

## 7. Tool Calling
- OpenAI-kompatible Shape: `tools` in JEDEM Request mitsenden; `tool_choice` `"auto"` (Default) statt erzwungen, `parallel_tool_calls: false` fuer sequentielle, abhaengige Aufrufe. Beschreibende Funktionsnamen + ausfuehrliche Descriptions. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell
- Beim Streaming Tool-Call-Argumente pro `tool_calls[].index` ueber alle Chunks akkumulieren und erst bei `finish_reason: "tool_calls"` parsen. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell

## 8. Structured Output, Usage-Accounting & Limits
- `response_format: {type: "json_schema", json_schema: {...}}` mit `strict: true`, `additionalProperties: false`, klaren Property-Descriptions + `required`; funktioniert mit `stream: true`. Fuer Non-Streaming Response-Healing-Plugin gegen kaputtes JSON. Quelle: https://openrouter.ai/docs/guides/features/structured-outputs · offiziell
- Usage-Accounting im `usage`-Feld der Response (native Tokenizer = Abrechnungsgrundlage); spaeter via `/api/v1/generation?id=...` auditierbar. `:free` = 20 RPM, 50 RPD (<10 USD gekauft) bzw. 1000 RPD (>=10 USD). Limits/Credits per `GET /api/v1/key` pruefen; 402 (leeres Guthaben) NICHT als 429 retrien. Quelle: https://openrouter.ai/docs/api/reference/limits · offiziell

---

# Teil II — OpenRouter fuer CLI-/Coding-Harnesses (Erweiterung 2026-06-17)

## 9. Claude Code an OpenRouter anbinden ⭐ (das Kern-Thema)

**Wichtigste Neuerung (Ende 2025/Anfang 2026):** OpenRouter hat eine **native „Anthropic Skin"** — einen Endpunkt, der Claude Codes eigenes `/v1/messages`-Protokoll direkt akzeptiert (nicht nur den OpenAI-kompatiblen `/api/v1`-Endpunkt). **Damit ist KEIN Proxy/Router mehr nötig.** Das alte `y-router`-Projekt ist seit Jan 2026 archiviert und verweist explizit auf die offizielle Integration. Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell · 2026-06-17

- **Direkter nativer Weg (empfohlen für Frank, Cloud-first).** Ins Shell-Profil (`~/.bashrc`/`~/.zshrc` bzw. unter Windows die User-Env), NICHT in eine projekt-lokale `.env` (die liest der Installer nicht):
  ```bash
  export OPENROUTER_API_KEY="<dein-openrouter-key>"
  export ANTHROPIC_BASE_URL="https://openrouter.ai/api"   # OHNE /v1 — sonst greift die Skin nicht!
  export ANTHROPIC_AUTH_TOKEN="$OPENROUTER_API_KEY"        # OR-Key gehört hierhin
  export ANTHROPIC_API_KEY=""                              # MUSS Leerstring sein (nicht unset!)
  ```
  Verifikation: `/status` in Claude Code → muss `Auth token: ANTHROPIC_AUTH_TOKEN` und `Anthropic base URL: https://openrouter.ai/api` zeigen; zusätzlich erscheinen Requests live im OpenRouter-Activity-Dashboard. Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **Modell-Rollen getrennt zuweisen** (das `~`-Präfix wählt „latest"):
  ```bash
  export ANTHROPIC_DEFAULT_OPUS_MODEL="~anthropic/claude-opus-latest"      # komplexe Tasks
  export ANTHROPIC_DEFAULT_SONNET_MODEL="~anthropic/claude-sonnet-latest"  # allg. Coding
  export ANTHROPIC_DEFAULT_HAIKU_MODEL="~anthropic/claude-haiku-latest"    # Background/Small-Fast
  export CLAUDE_CODE_SUBAGENT_MODEL="~anthropic/claude-opus-latest"        # Subagent-Tasks
  ```
  `ANTHROPIC_DEFAULT_HAIKU_MODEL` ist der korrekte Hebel fürs Background-/Small-Fast-Model (löst das alte `ANTHROPIC_SMALL_FAST_MODEL` ab). Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **Anthropic-Modelle durchreichen + Anthropic 1P als Top-Provider = garantierte Kompatibilität.** OpenRouter warnt offiziell: „Claude Code with OpenRouter is only guaranteed to work with the Anthropic first-party provider." Claude Code ist auf Anthropic-Modelle optimiert; Fremdmodelle (GLM, Qwen-Coder, DeepSeek, Kimi) gehen, riskieren aber Brüche bei Tool-Use/Streaming. Für Frank: Opus/Sonnet/Haiku via OpenRouter durchreichen ist der sichere Weg. Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **Prompt-Caching bleibt erhalten** (siehe §12): Multi-Turn-Coding profitiert vom automatischen Caching über Anthropic 1P; OpenRouter hält per Sticky-Routing den Cache warm. Quelle: https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell
- **Kostenkontrolle:** zentrale Budget-Limits + Activity-Dashboard + Cost-Tracking-Statusline (Skript aus `OpenRouterTeam/openrouter-examples`, eingetragen in `~/.claude/settings.json` unter `statusLine`). Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **GitHub Action mit OpenRouter:**
  ```yaml
  - uses: anthropics/claude-code-action@v1
    with:
      anthropic_api_key: ${{ secrets.OPENROUTER_API_KEY }}
    env:
      ANTHROPIC_BASE_URL: https://openrouter.ai/api
  ```
  Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **Fast Mode (`/fast`) wird unterstützt** (nur Opus 4.6/4.7/4.8). `export CLAUDE_CODE_SKIP_FAST_MODE_ORG_CHECK=1` (Claude Code v2.1.96+); OpenRouter routet `speed:"fast"` automatisch zum passenden `*-fast`-Modell über Anthropic 1P. Bei nicht unterstützten Modellen wird der Parameter still verworfen (kein Fehler). Quelle: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell
- **Router/Proxy NUR für Spezialfälle** (gezielt Nicht-Anthropic-Modelle mischen, lokal+Cloud, Routing nach Task-Typ):
  - **claude-code-router** (musistudio, ~34k Stars, MIT, sehr aktiv): `~/.claude-code-router/config.json` mit `Providers`-Array (OpenRouter-Eintrag: `api_base_url: "https://openrouter.ai/api/v1/chat/completions"`, `transformer: {"use":["openrouter"]}`) + `Router`-Block für Rollen-Mapping (`default`/`background`/`think`/`longContext`/`longContextThreshold`/`webSearch`). Start `ccr code`, Wechsel im Chat per `/model openrouter,modell`. Cross-Platform via npm. **Externer Code → vor Installation Prompt-Injection-Check (Projektregel).** Quelle: https://github.com/musistudio/claude-code-router · extern (hohe Reputation)
  - **LiteLLM Proxy**: nativer Anthropic-`/v1/messages`-Endpunkt + `/anthropic`-Passthrough; gut für Budgets/Virtual-Keys/Cost-Tracking. `ANTHROPIC_BASE_URL` auf den Proxy, `ANTHROPIC_AUTH_TOKEN=$LITELLM_MASTER_KEY`, Modell-Picker via `CLAUDE_CODE_ENABLE_GATEWAY_MODEL_DISCOVERY=1` (Claude Code v2.1.129+). Quelle: https://docs.litellm.ai/docs/tutorials/claude_non_anthropic_models · offiziell. **⚠️ SICHERHEIT: LiteLLM-Versionen 1.82.7 / 1.82.8 enthielten Credential-Stealing-Malware — meiden, saubere Version pinnen.**

## 10. Andere CLI-/Terminal-Coding-Agenten an OpenRouter anbinden

**Wichtigste Regel: das Modell-String-Format zuerst klären** — das ist die häufigste Fehlerquelle. Tools, die intern **LiteLLM** nutzen, wollen `openrouter/<vendor>/<model>` (doppeltes Präfix); Eigenbau-Tools wollen das nackte OpenRouter-Slug `<vendor>/<model>`.

| Tool | OR nativ? | Konfig-Muster (Key + Modell-String + Datei/Flag) | Quelle · Flag |
|------|-----------|--------------------------------------------------|---------------|
| **aider** | Ja (LiteLLM darunter) | `OPENROUTER_API_KEY`; `aider --model openrouter/<v>/<m>`; Routing in `.aider.model.settings.yml`; `--weak-model` + `--editor-model` splitten; `--cache-prompts` (nur Anthropic-Modelle) | aider.chat/docs/llms/openrouter.html · offiziell |
| **OpenCode** (sst) | Ja (Vercel AI SDK, kein LiteLLM) | `/connect` → OpenRouter oder `auth.json`; Modell `openrouter/<v>/<m>` (hier ist `openrouter` die OpenCode-ProviderID); `opencode.json` → `provider.openrouter` | openrouter.ai/docs/cookbook/coding-agents/opencode-integration · offiziell |
| **Codex CLI** (OpenAI) | Inoffiziell, via custom block | `~/.codex/config.toml`: `model_provider="openrouter"`, `[model_providers.openrouter]` mit `base_url="https://openrouter.ai/api/v1"`, `env_key="OPENROUTER_API_KEY"`, **`wire_api="chat"`**; Modell = nacktes Slug (KEIN `openrouter/`-Präfix) | openrouter.ai/docs/cookbook/coding-agents/codex-cli · offiziell |
| **Goose** (Block) | Ja (nativ) | `GOOSE_PROVIDER=openrouter`, `GOOSE_MODEL=<v>/<m>`, `OPENROUTER_API_KEY`; Env-Weg robuster als interaktive UI | goose-docs.ai/docs/getting-started/providers · offiziell |
| **Cline / Roo Code / Kilo Code** (VS Code) | Ja (OR zentral) | GUI: Provider „OpenRouter" + Key; Modell-Liste auto-gefetcht; native OR-Slugs, kein Präfix | docs.cline.bot / docs.roocode.com / kilo.ai · offiziell |
| **Continue.dev** | Ja (nativer Provider) | `config.yaml`: `provider: openrouter`, `model: <v>/<m>`, `apiBase: https://openrouter.ai/api/v1`, `apiKey`; ggf. `capabilities:[tool_use]` | docs.continue.dev/.../openrouter · offiziell |
| **OpenHands** (ex-OpenDevin) | Ja (LiteLLM) | `LLM_MODEL=openrouter/<v>/<m>` (LiteLLM-Slug!), `LLM_API_KEY=$OPENROUTER_API_KEY`; `openai/...` gegen OR funktioniert NICHT | docs.openhands.dev/.../openrouter · offiziell |
| **Cursor** | Teilweise | Base-URL-Override = `https://openrouter.ai/api/v1/cursor` (**mit `/cursor`-Suffix**, sonst brechen Tool-Calls) + OR-Key als OpenAI-Key | openrouter.ai/docs/cookbook/coding-agents/cursor-integration · offiziell |
| **Zed** | Inoffiziell | Nur via OpenAI-Compat-`api_url`-Override (Community-Workaround, fragil, kein nativer Provider) | github.com/zed-industries/zed Discussions · extern (unsicher) |

**Tool-übergreifende Best Practices:** (1) `OPENROUTER_API_KEY` einmal global setzen deckt fast alle CLI-Tools. (2) Modell-String-Format zuerst klären (LiteLLM vs. nativ). (3) Provider-Routing (`provider.order`/`data_collection:deny`) auch hier nutzen, um teure/trainierende Provider auszuschließen. (4) `weak-/editor-model`-Splitting (aider) spart Kosten erheblich — z.B. günstiges Modell für Commit-Msgs/Summaries, starkes fürs Editieren. Quelle: aider.chat/docs/usage/caching.html · offiziell

## 11. Schnelle Modelle & Speed-Routing ⭐ (Cloud, Frank-Schwerpunkt)

**Die Mechanik ist das Stabile (Modell-/Provider-Namen sind volatil — siehe Momentaufnahme unten):**

- **`provider.sort` ist der zentrale Hebel**, drei Werte: `"price"` (billigster), `"throughput"` (höchster Token-Durchsatz t/s), `"latency"` (niedrigste TTFT/Time-to-First-Token). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- **`sort` auch als Objekt mit `partition` (für Multi-Modell-Fallbacks):** `sort:{by:"throughput", partition:"none"}` sortiert Endpoints GLOBAL über ALLE Fallback-Modelle („nimm das aktuell schnellste Modell, egal welches"); `partition:"model"` (Default) hält das Primärmodell vorn. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- **Für interaktives Coding-CLI zählt TTFT, nicht Throughput** → `provider.sort: "latency"` ist meist besser als `:nitro`. `:nitro` == `sort:"throughput"` (nur sinnvoll bei langen, gepufferten Generierungen, die erst komplett gefüllt werden). Throughput vs. Latency: „wie schnell geschrieben wird" vs. „wann der erste Token erscheint". In Agent-Loops mit vielen LLM-Calls pro Turn summiert sich TTFT. Quelle: https://openrouter.ai/docs/faq · offiziell; Einordnung https://www.digitalapplied.com/blog/ai-model-latency-benchmarks-2026-ttft-throughput · extern
- **Variant-Suffixe (an den Modell-Slug anhängen):** `:nitro` (Throughput), `:floor` (Preis), `:free` ($0, strenge Limits), `:online` (Web-Suche — **deprecated**, siehe §13), `:exacto` (qualitäts-/**tool-calling-zuverlässigkeits**-orientiertes Routing — relevant fürs Coding mit Function-Calling). `sort`/`order`/Suffix **deaktivieren das Default-Load-Balancing**. Quelle: https://openrouter.ai/docs/guides/routing/model-variants/nitro · https://openrouter.ai/docs/guides/routing/model-variants/exacto · offiziell
- **Percentil-basierte Performance-Schwellen (NEU, weich — blockieren NIE):** `preferred_max_latency` und `preferred_min_throughput`, jeweils Zahl ODER Objekt `{p50,p75,p90,p99}`. OpenRouter trackt Latenz/Throughput pro Provider als Percentile über ein rollierendes 5-Minuten-Fenster. Empfehlung: **p90/p99-Latenz** für user-facing/interaktiv (Worst-Case-Konsistenz), p50-Throughput für Batch. Bsp.: `preferred_max_latency: {p90: 3}`. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- **Gezielt schnellsten Anbieter wählen:** Auf der Modellseite zeigt der „Providers"-Tab Latenz (TTFT) + Throughput (t/s) + Uptime pro Anbieter (sortierbar). Den gewünschten Provider-Slug dann in `provider.order`/`only` setzen. Vollständigen Slug nehmen (`deepinfra/turbo`), Basis-Slug (`deepinfra`) matcht auch langsamere Default-Endpoints. Quelle: https://openrouter.ai/docs/faq · offiziell
- **`max_price` als harter Deckel, kombiniert mit Speed-Sort:** „höchster Durchsatz, solange ≤ $x/M" → `sort:"throughput"` + `max_price:{prompt,completion}`. **Achtung:** `max_price` KANN den Request blockieren, wenn kein Anbieter im Limit liegt (die `preferred_*`-Schwellen tun das nicht). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- **Niedrige Latenz halten:** Guthaben ≥ $10–20 + Auto-Topup (niedriges Guthaben triggert Extra-DB-Checks → mehr Latenz); OpenRouter selbst (Cloudflare-Edge) fügt nur minimale Latenz hinzu. Quelle: https://openrouter.ai/docs/guides/best-practices/latency-and-performance · offiziell

**Schnelle Inferenz-Provider — MOMENTAUFNAHME Mitte 2026 (volatil, nur grobe Einordnung):**

| Provider | Speed-Profil | Über OR? |
|---|---|---|
| **Cerebras** | König des **Throughput** (Wafer-Scale), ~1.800–3.000 t/s | Ja |
| **Groq** | König der **niedrigen Latenz** (LPU), sub-100 ms TTFT | Ja |
| **SambaNova / Fireworks / Together / DeepInfra (`/turbo`) / Baseten** | hoher Throughput, breiter Katalog | Ja |

Quellen: https://artificialanalysis.ai · https://openrouter.ai/provider/groq · extern/offiziell. **Schnelle Coding-Modelle** (Momentaufnahme, über OR): klein/schnell für Autocomplete (Codestral, Diffusion-Modelle wie Mercury), starke MoE für Agentic Coding (GLM-Reihe, Qwen3-Coder, Kimi, DeepSeek, MiniMax). Diese Namen ändern sich monatlich — **nie hardkodieren**, lieber Laufzeit-Schwellen (`preferred_*`) + `/api/v1/models`.

**Stabiler Kern fürs Coding-CLI:** Modell wählen → `:exacto` falls Tool-Calling wichtig → für Tippgefühl `sort:"latency"` (oder `preferred_max_latency:{p90:…}`) statt `:nitro` → optional `max_price` als Deckel → Fallbacks AN lassen für Uptime.

## 12. Prompt-Caching, Reasoning & Usage (Tiefe, 2026)

- **Anthropic braucht explizite `cache_control`-Breakpoints — zwei Modi:** (a) **Automatic** — ein einzelnes `cache_control:{type:"ephemeral"}` auf Top-Level, Breakpoint wandert mit wachsendem Verlauf mit (ideal für Multi-Turn-Coding, was Claude Code macht); (b) **explizit** pro Content-Block (max. 4) für große stabile Blöcke (Repo-Dump, System-Prompt). Cache-Read = 0,1x, Write 1,25x (5-Min-TTL) / 2x (1-Std-TTL). Min. cacheable: Opus & Haiku 4.5 = 4096 Tokens, Sonnet 4.6/4.5 = 1024. **Automatic (Top-Level) funktioniert NUR über Anthropic 1P** (Bedrock/Vertex nicht). Quelle: https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell
- **OpenAI, Grok, DeepSeek, Moonshot, Groq cachen automatisch** (kein `cache_control`); Gemini 2.5 hat implizites Auto-Caching, für explizit nutzt OR nur den **letzten** Breakpoint. Qwen-Coder-Modelle unterstützen explizites Caching. Quelle: https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell
- **Sticky-Routing maximiert Cache-Hits automatisch.** Für agentische Multi-Turn-Workflows `session_id` (Body) bzw. Header `x-session-id` (max. 256 Zeichen) setzen → Sticky ab dem ersten erfolgreichen Request. **Manuelles `provider.order` deaktiviert Sticky-Routing** — für Cache-Effizienz lieber `session_id` als `order`. Quelle: https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell
- **`usage.include:true` ist DEPRECATED/wirkungslos** — Usage (inkl. `cost`, `cached_tokens`, `cache_write_tokens`, `reasoning_tokens`) kommt jetzt **immer** automatisch (beim Streaming im letzten Chunk). Cache-Ersparnis: `usage.prompt_tokens_details.cached_tokens` + `cache_discount`; `cost_details.upstream_inference_cost` = echte Provider-Kosten (nur bei BYOK gefüllt). Quelle: https://openrouter.ai/docs/cookbook/administration/usage-accounting · offiziell
- **Reasoning-Tokens (für Reasoning-Modelle):** unified `reasoning`-Parameter — `effort` (`xhigh|high|medium|low|minimal|none`) ODER `max_tokens`, plus `exclude:true` (intern nutzen, nicht zurückgeben). Die alte `:thinking`-Variante für Anthropic wird NICHT mehr unterstützt; `include_reasoning` ist deprecated. **Kritisch fürs Coding mit Tools:** `reasoning_details`-Array beim Zurückgeben der Assistant-Message **unverändert + in Reihenfolge** mitschicken, sonst verliert das Modell den Reasoning-Faden zwischen Tool-Call und Tool-Result. Quelle: https://openrouter.ai/docs/guides/best-practices/reasoning-tokens · offiziell
- **Modell-spezifische Reasoning-Details:** Anthropic — `budget_tokens = max(min(max_tokens × effort_ratio, 128000), 1024)` (effort_ratio 0,95…0,1 von xhigh→minimal); `max_tokens` MUSS strikt > Reasoning-Budget sein, Reasoning zählt als Output-Tokens. Gemini 3 nutzt `thinkingLevel` statt `thinkingBudget` → `reasoning.max_tokens` gibt KEINE präzise Token-Kontrolle (Google mappt intern auf Level). Quelle: https://openrouter.ai/docs/guides/best-practices/reasoning-tokens · offiziell

## 13. Neuere Plattform-Features (2025/2026) — Horizont-Scan

- **Presets** = server-seitig gespeicherte, benannte Config (Modell + Fallbacks + Provider-Routing + System-Prompt + Parameter), referenziert via `model:"@preset/slug"`, `preset`-Feld oder `model@preset/slug`. **Trennt LLM-Config vom Code** → Modell/Prompt/Routing ändern **ohne Code-Deploy** (robust gegen verschwindende Modell-Slugs). Versioniert mit Rollback. Erzeugbar direkt aus einem funktionierenden Request via `POST /api/v1/presets/{slug}/chat/completions`. Request-Params überschreiben Preset (shallow-merge). Quelle: https://openrouter.ai/docs/guides/features/presets · offiziell
- **BYOK (Bring Your Own Key):** eigene Provider-Keys hinterlegen (`/workspaces/default/byok`), Fee = 5% des OR-Modellpreises, erste 1 Mio BYOK-Requests/Monat gratis. **BYOK-Endpoints werden IMMER zuerst versucht — auch wenn sie in `provider.order` hinten stehen.** Pro Key: Model-/API-Key-/Member-Filter (mehrere Keys pro Provider mit Drag-&-Drop-Priorität). Debugging: Activity → „View Raw Metadata" → `provider_responses` zeigt den HTTP-Status je Provider-Versuch (401/403/429 …). `upstream_inference_cost` in der Usage ist nur bei BYOK-Requests gefüllt. Quelle: https://openrouter.ai/docs/guides/overview/auth/byok · offiziell
- **ZDR (Zero Data Retention) ist jetzt pro Provider-Gruppe granular:** `enforce_zdr_anthropic|_openai|_google|_other` (Legacy `enforce_zdr` deprecated). Per-Request `provider.zdr:true` (wirkt als OR mit Account-Settings, kann nur einschalten). ZDR-Endpoint-Liste: `GET /api/v1/endpoints/zdr`. Implizites Prompt-Caching gilt NICHT als „Retention". Quelle: https://openrouter.ai/docs/guides/features/zdr · offiziell
- **Response Caching (Beta)** — Request-Level-Cache VOR dem Provider (getrennt vom Prompt-Caching). Header `X-OpenRouter-Cache:true` (+ `-Cache-TTL`, Default 300s; `-Cache-Clear`). **Cache-Hits komplett kostenlos** (Usage = 0, zählen nicht gegen Rate-Limits). Nutzen fürs Coding: Agent-Workflows nach Crash kostenlos fortsetzen, reproduzierbare Tests. **Achtung:** ignoriert `temperature`/`seed` beim Hit; Body-Änderungen (auch JSON-Reihenfolge) ändern den Cache-Key. Quelle: https://openrouter.ai/docs/guides/features/response-caching · offiziell
- **`service_tier`-Parameter:** Cost/Latency-Tradeoff pro Request (OpenAI: `auto|default|flex|priority`; `flex` ~50% günstiger gegen höhere Latenz). Quelle: https://openrouter.ai/docs/guides/features/service-tiers · offiziell
- **Server-Tools (Beta)** — vom Modell selbst aufrufbar: **`openrouter:web_search`** (ersetzt das deprecatete `:online`/`web`-Plugin; Engines auto/native/exa/firecrawl/parallel; `max_results`/`max_total_results` als Kosten-Cap), **`openrouter:subagent`** (Teilaufgaben an kleineres/billigeres Modell delegieren — spart Frontier-Tokens), **`openrouter:advisor`**, **`openrouter:fusion`** (Modell-Panel + Judge), `web_fetch`, `datetime`, `image-generation`. Alle Beta → API kann sich ändern. Quelle: https://openrouter.ai/docs/guides/features/server-tools · offiziell
- **Multimodal** über die einheitliche API: PDF (`file`-Type, Text gratis/OCR bezahlt), Bild (`image_url`), Audio (`input_audio`), Video; plus dedizierte Endpoints TTS/STT/Video-Gen und **Embeddings** (`POST /api/v1/embeddings`, OpenAI-kompatibel). Quelle: https://openrouter.ai/docs/guides/overview/multimodal/overview · offiziell

- **Weitere erwähnenswerte Features (für Coding-Workflows):** **Body Builder**-Router (wandelt Natural-Language in ein strukturiertes Request-Objekt), **Broadcast** (Observability-Export an Datadog/Langfuse/Sentry/S3 u.a.), **Guardrails** (Prompt-Injection-/PII-Erkennung), **Sovereign AI** (Datenresidenz/Compliance), **Zero Completion Insurance** (keine Abrechnung bei leerer Antwort), **Message Transforms**, **Fusion-API** (mehrere Modelle kombiniert, ~halber Preis), **Embeddings** (`POST /api/v1/embeddings`, OpenAI-kompatibel, eigene Rankings). Quelle: https://openrouter.ai/docs/guides/features/ · offiziell

## 14. Cloud vs. lokal — kombinieren (Frank: Cloud-first)

- **Grundtatsache:** OpenRouter ist ein **reiner Cloud-Aggregator** — lokale Modelle laufen NIE auf OR-Servern. Wer lokal + OR in EINEM Workflow mischen will, braucht eine **Router-Schicht davor** (claude-code-router oder LiteLLM, siehe §9) ODER schaltet getrennt per base_url um. Quelle: https://openrouter.ai/docs/guides/overview/models · offiziell
- **Schlüsselprinzip — gleicher Code, nur base_url tauschen:** OpenRouter (`https://openrouter.ai/api/v1`), Ollama (`http://localhost:11434/v1`), LM Studio (`http://localhost:1234/v1`) und llama.cpp-Server sprechen alle das OpenAI-Chat-Completions-Schema. Lokal↔Cloud wird zur Ein-Zeilen-Umschaltung (`base_url` + `api_key`). Quelle: https://lmstudio.ai/docs/developer/openai-compat · https://openrouter.ai/docs/guides/community/openai-sdk · offiziell
- **LM Studio als lokaler OpenAI-Server (Frank nutzt LM Studio offline):** Modell laden → „Start Server" → `http://localhost:1234/v1` (akzeptiert `/v1/models`, `/v1/chat/completions`, `/v1/embeddings`). **Fallstrick:** das OpenAI-SDK verlangt einen nicht-leeren `api_key` — Dummy übergeben (`"lm-studio"`/`"not-needed"`), sonst Fehler. Kein Netz-Egress nötig → ideal offline. Quelle: https://lmstudio.ai/docs/developer/openai-compat/chat-completions · offiziell
- **Native Ollama↔Claude-Code-Anbindung ohne Proxy (Ollama v0.14, Jan 2026):** Ollama exponiert eine Anthropic-kompatible Messages-API auf `localhost:11434` — exakt das Protokoll von Claude Code. Setup: `ollama launch claude`, oder `ANTHROPIC_AUTH_TOKEN=ollama` + `ANTHROPIC_BASE_URL=http://localhost:11434`. Lokaler Notnagel für Claude Code ist damit trivial. Quelle: https://ollama.com/blog/claude · offiziell
- **Wann Cloud-first über OpenRouter (Franks Präferenz):** kein lokaler GPU-/RAM-Verbrauch, Zugriff auf Frontier-Modelle, oft schneller als Consumer-Hardware, einfaches Provider-Failover. **Wann lokal trotzdem:** echter Datenschutz (Code verlässt die Maschine nie), Offline, Nulltarif, kein Rate-Limit; Routine-Tasks (Completion/Refactor/Erklären) gehen gut lokal. Quelle: https://www.kdnuggets.com/pairing-claude-code-with-local-models · extern

## 15. Account / Keys / Ops

- **Key-Typen trennen:** Inference-Keys (`sk-or-v1-…`, decken Modellkosten) vs. **Management Keys** (früher „Provisioning Keys", nur `/api/v1/keys`-Verwaltung — können KEINE Inference; Inference-Keys können keine Analytics → 403). Für einen einzelnen CLI-Nutzer ist ein Management Key meist Overkill; lohnt erst bei Auto-Rotation/mehreren Projekten. Quelle: https://openrouter.ai/docs/guides/overview/auth/management-api-keys · offiziell
- **Pro Key ein Credit-`limit`** (optional `limit_reset: daily|weekly|monthly`) setzen → ein durchgedrehter CLI-Loop kann nicht das ganze Guthaben verbrennen. Live-Check via `GET /api/v1/key` (`limit_remaining`, `usage`). Quelle: https://openrouter.ai/docs/api/reference/limits · offiziell
- **Rate-Limits sind GLOBAL pro Account** — mehr Keys/Accounts helfen NICHT, nur Modell-Streuung verteilt Last. Paid-Modelle haben keine harten OR-Limits (nur Cloudflare-DDoS-Schutz); `:free` ist fürs ernsthafte Coding unzuverlässig (20 RPM + Tageslimit, häufige 429 vom Upstream). Quelle: https://openrouter.ai/docs/api/reference/limits · offiziell
- **Sicherheit:** Keys in Env/Secret-Store, NIE hardcoden/committen (`.env` in `.gitignore`); OpenRouter ist GitHub-Secret-Scanning-Partner (exponierte Keys werden erkannt → löschen + neu erstellen). Pro Projekt ein separater Key (Leak-Isolation + saubere Kostenzuordnung). Zero-Downtime-Rotation: neuen Key anlegen → umstellen → Traffic-Migration im Dashboard prüfen → alten löschen. Quelle: https://openrouter.ai/docs/api/reference/authentication · https://openrouter.ai/docs/cookbook/administration/api-key-rotation · offiziell
- **Analytics:** Activity-Dashboard (Spend/Tokens/Requests, Filter, CSV/PDF-Export) als primäres Kostentool; Beta-Analytics-API (`POST /api/v1/analytics/query`, braucht Management Key) für Drilldowns. Quelle: https://openrouter.ai/docs/cookbook/administration/analytics-cost-control · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/openrouter-api.md`) |
|---|---|
| 1 Unified Gateway & Attribution-Header | A1, A2 |
| 2 Provider-Routing bewusst steuern | B3, B6 |
| 3 Qualitaet & Datenschutz absichern | B4 |
| 4 Kosten- & Geschwindigkeits-Routing | B5 |
| 5 Modell-Routing & Fallback-Array | C7, F17 |
| 6 Streaming / SSE robust verarbeiten | D9, D10, D11, D12 |
| 7 Tool Calling | C7, C8 |
| 8 Structured Output, Usage-Accounting & Limits | B6, E13, E14, E15, F18 |
| 9 Claude Code anbinden (Anthropic Skin) | G19, G20, G21, G22, G23 |
| 10 Andere CLI-Coding-Agenten | G24, G25, G26 |
| 11 Schnelle Modelle & Speed-Routing | B5, H27, H28 |
| 12 Prompt-Caching / Reasoning / Usage | H29, H30, H31, H32 |
| 13 Neuere Plattform-Features | H33, H34, H35 |
| 14 Cloud vs. lokal | G24, H36 |
| 15 Account / Keys / Ops | E15, F18, H37 |
