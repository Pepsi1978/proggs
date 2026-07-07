# OpenRouter API — Best Practices (Stand 2026-06-18)

> Gegenstueck zu `bugs/apis/openrouter-api.md`. Offiziell empfohlen (Quellen).
> Basis-Recherche 2026-06-09 (§1–§8, rohe API-Integration). **Erweiterung 2026-06-17**
> (§9–§15): Schwerpunkt **OpenRouter fuer CLI-/Coding-Harnesses** (Claude Code & Co.),
> **schnelle/Cloud-Modelle** und **Cloud-vs-lokal** — Researcher-Schwarm-Recherche.
> **Erweiterung 2026-06-18 (§16–§19):** Schwerpunkt **OpenCode (SST) — Franks tatsaechliche
> Umgebung** (Voll-Konfiguration, Routing-Durchreichung, Agents/Modell-pro-Aufgabe), eine
> **Modellwahl-Entscheidungshilfe nach Aufgabentyp**, eine **Account-/Dashboard-Einstellungs-
> Checkliste** und **Parameter-Korrekturen seit 06-17** (Message-Transforms, `:online`, `usage`,
> neue Felder) — 5-Researcher-Schwarm.

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
- **Server-Tools (Beta) — die offizielle Übersichtsseite listet SECHS** (verifiziert 2026-06-20), je `{"type":"openrouter:<name>"}` im `tools`-Array, vom Modell selbst aufrufbar: **`web_search`** (agentisch, ersetzt `:online`/`web`-Plugin; Engines auto/native/exa/parallel/perplexity/firecrawl; Exa/Parallel $0.005/Suche bis 10 Treffer +$0.001/Treffer; `max_results` 1–25 Default 5, `max_total_results` als Kosten-Cap über ALLE Suchen), **`web_fetch`** (vollen Seiteninhalt einer URL holen — Engine **`openrouter` = KOSTENLOS**, Exa/Parallel je $0.001/Fetch; `max_content_tokens` gegen Kontext-Überlauf), **`datetime`**, **`image_generation`**, **`apply_patch`** (NUR Responses API), **`fusion`** (Modell-Panel + Judge). Eine ältere Notiz (06-17) nannte zusätzlich `subagent`/`advisor` — die stehen 06-20 NICHT mehr auf der offiziellen Seite (vor Nutzung verifizieren). Modell braucht Tool-Calling-Support. Alle Beta → API kann sich ändern. Quelle: https://openrouter.ai/docs/guides/features/server-tools · offiziell · verifiziert 2026-06-20
- **Multimodal** über die einheitliche API: PDF (`file`-Type), Bild (`image_url`), Audio (`input_audio`), Video; plus dedizierte Endpoints TTS/STT/Video-Gen und **Embeddings** (`POST /api/v1/embeddings`, OpenAI-kompatibel). Quelle: https://openrouter.ai/docs/guides/overview/multimodal/overview · offiziell
- **PDF/`file-parser`-Plugin** (`plugins:[{"id":"file-parser","pdf":{"engine":..}}]`) — Engines: `cloudflare-ai` (**kostenlos**, PDF→Markdown), `native` (als Input-Tokens des Modells, produziert **KEINE** `annotations`), `mistral-ocr` ($2/1000 Seiten, OCR-Kosten **auch bei BYOK** — OR nutzt eigenen Mistral-Key; max 8 Bilder/PDF), `pdf-text` (deprecated → cloudflare-ai). Default: native, sonst mistral-ocr. **Spar-Trick:** die zurückgegebenen `annotations` (stabiler `file.hash`) im Folge-Request an der vorherigen Assistant-Message mitsenden → OR überspringt das erneute Parsen (kein Re-Parse, keine erneuten OCR-Kosten; auch im Error-Pfad unter `error.metadata.file_annotations`). Datei als `{"type":"file","file":{"filename":..,"file_data":"data:application/pdf;base64,.."}}` (Feldname `file_data` Python / `fileData` TS-SDK — Docs inkonsistent). Quelle: https://openrouter.ai/docs/guides/features/plugins · offiziell · verifiziert 2026-06-20

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
| 16 OpenCode-Voll-Konfiguration | G24, I38, I39, I40 |
| 17 Modellwahl nach Aufgabentyp | C7, H27 |
| 18 Account-/Dashboard-Einstellungen | E13, E14, E15, F18 |
| 19 Parameter-Korrekturen 06-18 | D9, H33 |

---

# Teil III — OpenCode (SST), Modellwahl & Account-Settings (Erweiterung 2026-06-18)

> Anlass: Frank nutzt **OpenCode** (sst/opencode, neues Repo `anomalyco/opencode`, Docs `opencode.ai`)
> mit OpenRouter und will alles optimal einstellen. Teil II war Claude-Code-zentriert; dieser Teil ist
> die OpenCode-Tiefe + eine Modellwahl-Entscheidungshilfe + die Dashboard-Settings-Checkliste.

## 16. OpenRouter in OpenCode — vollstaendige Konfiguration ⭐ (Franks Kern-Thema)

### 16.1 Anbindung & Auth (zuerst klaeren — haeufigste Fehlerquelle)
- **Login NICHT per `opencode auth login`** — diesen Befehl gibt es in der OpenCode-Doku NICHT. Der dokumentierte Weg ist der TUI-Befehl **`/connect`** → **OpenRouter** waehlen → Key (`sk-or-…`) einfuegen → dann **`/models`** zum Modell waehlen. Quelle: https://openrouter.ai/docs/cookbook/coding-agents/opencode-integration · https://opencode.ai/docs/providers · offiziell · 2026-06-18
- **auth.json-Speicherort:** `~/.local/share/opencode/auth.json`. Per `/connect` hinterlegte Keys landen genau hier; manuell eintragbar als `{"openrouter":{"type":"api","key":"sk-or-…"}}`. Pruefen ob Credentials sitzen: **`opencode auth list`** (der einzige dokumentierte `opencode auth …`-Befehl). Quelle: https://opencode.ai/docs/troubleshooting · offiziell · 2026-06-18
- **`OPENROUTER_API_KEY`-Env existiert in der OpenCode-Doku NICHT** als Standard-Hebel. Wer den Key per Config statt `/connect` setzen will, nutzt die generische `{env:VAR}`-Syntax: `provider.openrouter.options.apiKey: "{env:OPENROUTER_API_KEY}"` (Schluss aus dem Custom-Provider-Schema, nicht woertlich aus einem OR-Beispiel). Quelle: https://opencode.ai/docs/config · offiziell (abgeleitet) · 2026-06-18
- **Modell-String = `openrouter/<vendor>/<model>`** (z. B. `openrouter/anthropic/claude-sonnet-4.6`). `openrouter` ist hier die OpenCode-Provider-ID, `<vendor>/<model>` der OpenRouter-Slug. **Wichtig:** INNERHALB von `provider.openrouter.models` ist der *Schluessel* nur `<vendor>/<model>` (ohne `openrouter/`-Praefix). Quelle: https://opencode.ai/docs/models · openrouter.ai Cookbook · offiziell · 2026-06-18

### 16.2 Config-Datei, Speicherorte & Praezedenz
- **Format:** JSON oder JSONC (Kommentare erlaubt). IMMER `"$schema": "https://opencode.ai/config.json"` setzen (Autocomplete + Validierung). Quelle: https://opencode.ai/docs/config · offiziell · 2026-06-18
- **Speicherorte (spaetere ueberschreiben fruehere, aber gemerged, nicht ersetzt):** (1) Remote `.well-known/opencode` → (2) **Global `~/.config/opencode/opencode.json`** → (3) Custom-Pfad (`OPENCODE_CONFIG`) → (4) **Projekt-lokal `opencode.json`/`opencode.jsonc`** (hoechste Standard-Praezedenz) → (5) `.opencode/`-Verzeichnisse → (6) Inline (`OPENCODE_CONFIG_CONTENT`) → (7) Managed (Admin/MDM). Quelle: https://opencode.ai/docs/config · offiziell · 2026-06-18
- **OpenRouter-Modelle „vorladen"** (minimal):
  ```json
  {
    "$schema": "https://opencode.ai/config.json",
    "provider": {
      "openrouter": {
        "models": {
          "anthropic/claude-sonnet-4.6": {},
          "google/gemini-2.5-flash": {}
        }
      }
    }
  }
  ```
  Ein leeres Objekt als Wert reicht, um ein Modell verfuegbar zu machen. Quelle: https://openrouter.ai/docs/cookbook/coding-agents/opencode-integration · offiziell · 2026-06-18
- OpenCode nutzt intern das **Vercel AI SDK** + **Models.dev** (75+ Provider). OpenRouter ist eingebaut → fuer den eingebauten Provider sind `npm`/`baseURL`/`apiKey` NICHT noetig (nur fuer selbst definierte Custom-Provider). Quelle: https://opencode.ai/docs/models · offiziell · 2026-06-18

### 16.3 Globale Modellwahl: `model` + `small_model`
- `model` = Hauptmodell (Format `provider/model`), `small_model` = separates, leichtes Modell fuer Nebenaufgaben (Titel-Generierung, Zusammenfassungen, Compaction). Ohne `small_model` versucht OpenCode automatisch ein guenstigeres Modell desselben Providers, sonst faellt es aufs Hauptmodell zurueck. **Empfehlung:** `small_model` auf ein billiges schnelles Modell setzen (Haiku/Flash/Nano-Tier) — spart spuerbar.
  ```json
  {
    "$schema": "https://opencode.ai/config.json",
    "model": "openrouter/anthropic/claude-sonnet-4.6",
    "small_model": "openrouter/google/gemini-2.5-flash-lite"
  }
  ```
  Modell-Lade-Prioritaet beim Start: (1) CLI-Flag `--model`/`-m` → (2) `model`-Key → (3) zuletzt benutzt → (4) interne Default-Prioritaet. Interaktiv wechseln: **`/models`**. Quelle: https://opencode.ai/docs/config · https://opencode.ai/docs/models · offiziell · 2026-06-18

### 16.4 OpenRouter-Routing DURCH OpenCode durchreichen ⭐ (der entscheidende Hebel)
- OpenCode reicht **alles unter `provider.openrouter.models.<model>.options.provider` 1:1 als OpenRouter-`provider`-Routing-Objekt** durch. Damit funktionieren ALLE OR-Routing-Felder (in **snake_case**) — nicht nur `order`/`allow_fallbacks` (die einzigen, die die OpenCode-Doku woertlich zeigt). Offiziell belegtes Muster:
  ```json
  {
    "$schema": "https://opencode.ai/config.json",
    "provider": {
      "openrouter": {
        "models": {
          "anthropic/claude-sonnet-4.6": {
            "options": {
              "provider": {
                "order": ["anthropic"],
                "allow_fallbacks": true,
                "require_parameters": true,
                "data_collection": "deny",
                "sort": "latency",
                "max_price": { "prompt": 3, "completion": 15 },
                "preferred_max_latency": { "p90": 3 }
              }
            }
          }
        }
      }
    }
  }
  ```
  Quelle: https://opencode.ai/docs/providers · https://openrouter.ai/docs/guides/routing/provider-selection · offiziell (Felder) + abgeleitet (Kombination) · 2026-06-18
- **Praktisch fuer Frank:** Damit holt man die ganze §2–§4/§11-Mechanik (Determinismus, Datenschutz, Speed, Preisdeckel) direkt in OpenCode. Beispiel-Rezepte: interaktives Tippgefuehl → `"sort":"latency"`; Determinismus → `"order":[…]`+`"allow_fallbacks":false`; Compliance → `"data_collection":"deny"`+`"require_parameters":true`; Garbage-Schutz → `"quantizations":["fp16","bf16"]`. Die `:nitro`/`:floor`-Suffixe am Modell-Slug entsprechen `sort:"throughput"` bzw. `sort:"price"`.
- **Nicht belegt:** ein provider-WEITER Options-Block (`provider.openrouter.options.provider`) ist in der OpenCode-Doku nicht dokumentiert — das einzige sichere Muster ist **pro Modell** unter `models.<m>.options.provider`. Quelle: https://opencode.ai/docs/providers · offiziell · 2026-06-18

### 16.5 Reasoning & Modell-Optionen pro Modell
- Modell-Optionen liegen unter `provider.<id>.models.<model>.options` und werden an den Provider durchgereicht. Zwei Wege fuer OpenRouter-Reasoning:
  - **(a) OpenCode-normalisiert:** `reasoningEffort`, `textVerbosity`, `maxTokens`, `temperature` direkt im Options-Block.
  - **(b) rohen OR-`reasoning`-Parameter durchreichen** (zuverlaessigster Weg, wenn das Modell den Effort „nicht respektiert"): `"options": { "reasoning": { "effort": "high" } }`. Werte: `xhigh|high|medium|low|minimal|none` oder exakt `"reasoning": { "max_tokens": 4096 }`.
  Quelle: https://opencode.ai/docs/models · https://openrouter.ai/docs/guides/best-practices/reasoning-tokens · offiziell + extern (gist.github.com/lkoelman) · 2026-06-18
- **Variants** = mehrere Einstellungen fuer dasselbe Modell ohne Duplikate; per Keybind `variant_cycle` (TUI ctrl+t) bzw. CLI `--variant` umschaltbar; `"disabled": true` schaltet eine Variante ab. Eingebaute Defaults: Anthropic `high`/`max`; OpenAI `none/minimal/low/medium/high/xhigh`; Google `low/high`. Quelle: https://opencode.ai/docs/models · offiziell · 2026-06-18

### 16.6 Agents & Modell-pro-Aufgabe ⭐ (Kosten/Qualitaet steuern)
- OpenCode kennt **Primary Agents** (Tab-umschaltbar: `build` = alle Tools, `plan` = `edit`/`bash` auf `deny`/`ask`) und **Subagents** (per `@mention` oder automatisch uebers Task-Tool: eingebaut `general`, `explore` (read-only Suche), `scout` (read-only Doku/Dependencies)). Versteckte System-Agents: `compaction`, `title`, `summary`. Quelle: https://opencode.ai/docs/agents · offiziell · 2026-06-18
- **Pro Agent ein eigenes `model`** → teures Modell nur fuer schwere Arbeit, billiges fuer Routine:
  ```json
  {
    "$schema": "https://opencode.ai/config.json",
    "model": "openrouter/anthropic/claude-sonnet-4.6",
    "small_model": "openrouter/google/gemini-2.5-flash-lite",
    "agent": {
      "build": {
        "mode": "primary",
        "model": "openrouter/anthropic/claude-opus-4.7",
        "permission": { "edit": "allow", "bash": "allow" }
      },
      "plan": {
        "mode": "primary",
        "model": "openrouter/google/gemini-2.5-flash",
        "permission": { "edit": "deny", "bash": "deny" }
      },
      "code-reviewer": {
        "description": "Reviews code for security, performance, maintainability",
        "mode": "subagent",
        "model": "openrouter/anthropic/claude-sonnet-4.6",
        "temperature": 0.1,
        "permission": { "edit": "deny" }
      }
    }
  }
  ```
  Quelle: https://opencode.ai/docs/agents · offiziell · 2026-06-18
- **Agent-Felder:** `description` (Pflicht bei eigenen Agents — steuert Auto-Invocation), `mode` (`primary|subagent|all`), `model`, `temperature`/`top_p`, `prompt` (`{file:./…}` oder Inline), `steps` (max. Iterationen = Kostenbremse), `disable`, `hidden`, `permission` (inkl. `permission.task` = welche Subagents per Task aufrufbar), `color`. Jeder **zusaetzliche** Key wird direkt als Modell-Option an den Provider durchgereicht (z. B. `reasoningEffort`/`textVerbosity` auf Agent-Ebene). Ohne `model` erbt ein Subagent das Modell des aufrufenden Primary-Agents. Quelle: https://opencode.ai/docs/agents · offiziell · 2026-06-18
- **Markdown-Agents** als Alternative: Dateien in `~/.config/opencode/agents/` (global) bzw. `.opencode/agents/` (Projekt), Frontmatter = die gleichen Felder, Dateiname = Agent-Name. Erstellen per `opencode agent create`, auflisten per `opencode agent list`. (Verzeichnisse jetzt **plural**: `agents/`, `commands/`, `plugins/`; Singular bleibt rueckwaertskompatibel.) Quelle: https://opencode.ai/docs/agents · https://opencode.ai/docs/cli · offiziell · 2026-06-18
- **Orchestrator-Muster (Community-Konsens):** teures Modell (Opus/GPT-5) plant/orchestriert, spawnt per Subagent guenstige/spezialisierte Modelle (Gemini-Flash, Grok-Code-Fast, Kimi) fuer Implementierung/Recherche/Boilerplate. „Stop using one model for everything." Quelle: https://www.glukhov.org/ai-devtools/opencode/oh-my-opencode-agents/ · https://amirteymoori.com/opencode-multi-agent-setup-specialized-ai-coding-agents/ · extern · 2026-06-18

### 16.7 Permissions (gefaehrliche Aktionen einschraenken)
- `permission` (ersetzt das deprecatete `tools`-Boolean) mit drei Aktionen: `allow` (ohne Rueckfrage), `ask` (Bestaetigung), `deny` (blockiert). Keys u. a.: `read`, `edit` (= write+edit+apply_patch), `bash`, `task`, `webfetch`, `websearch`, `external_directory`, `doom_loop`. Granulare Objekt-Syntax mit **„letzte passende Regel gewinnt"** (Catch-all `"*"` zuerst):
  ```json
  { "permission": {
      "bash": { "*": "ask", "git *": "allow", "npm *": "allow", "rm *": "deny" },
      "edit": { "*": "deny", "src/**": "allow" }
  }}
  ```
  Sichere Defaults: meiste Permissions `allow`, aber `doom_loop`/`external_directory` = `ask`, und **`.env`-Lesen ist per Default `deny`**. Quelle: https://opencode.ai/docs/permissions · offiziell · 2026-06-18

### 16.8 Kontext-/Token-Management & Caching (Kosten niedrig halten)
- **Compaction (Autocompact):** `"compaction": { "auto": true, "prune": false, "reserved": 10000 }`. **`prune: true` zum Sparen** (entfernt alte Tool-Outputs). Abschaltbar per `OPENCODE_DISABLE_AUTOCOMPACT=1`, Pruning per `OPENCODE_DISABLE_PRUNE=1`. Quelle: https://opencode.ai/docs/config · offiziell · 2026-06-18
- **Kosten beobachten:** `opencode stats` (`--days`, `--tools`, `--models`, `--project`). Neue, abgegrenzte Aufgaben in **neuen Sessions** starten (lange Sessions = wachsender Kontext = mehr Tokens). `steps` pro Agent begrenzen. Quelle: https://opencode.ai/docs/cli · offiziell · 2026-06-18
- **MCP kostet Kontext:** jeder MCP-Server fuegt Tools = Tokens hinzu; manche (GitHub-MCP) „can easily exceed the context limit". → MCP sparsam, ggf. global aus + pro Agent gezielt an: `"tools": { "my-mcp*": false }` global, im Agent `"tools": { "my-mcp*": true }`. Quelle: https://opencode.ai/docs/mcp-servers · offiziell · 2026-06-18
- **Prompt-Caching:** Provider-Option `"setCacheKey": true` erzwingt Cache-Key; zusaetzlich `timeout`/`chunkTimeout` gegen haengende Streams. OR-seitig: die meisten Provider cachen automatisch, **Anthropic braucht `cache_control` und Auto-Caching nur ueber Anthropic-1P** (nicht Bedrock/Vertex) — siehe §12. **Vorsicht:** offene OpenCode-Issues (#1245, #5416) berichten, dass Anthropic-Caching via OpenRouter nicht immer greift → in der OR-Activity pruefen, ob `cached_tokens` wirklich erscheinen. Quelle: https://opencode.ai/docs/config · https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell + extern (GitHub-Issues) · 2026-06-18

### 16.9 MCP-Server, Regeln & dauerhafter Kontext
- **MCP** im `mcp`-Block (`type:"local"` mit `command`-Array, oder `type:"remote"` mit `url`+`headers`); OAuth automatisch (Token in `~/.local/share/opencode/mcp-auth.json`), manuell `opencode mcp auth/list/logout/debug`. Doku-Beispiele: Context7 (Doku-Suche), Grep by Vercel (GitHub-Code-Suche), Sentry. Quelle: https://opencode.ai/docs/mcp-servers · offiziell · 2026-06-18
- **Regeln/Instructions:** dauerhafter Projektkontext in **`AGENTS.md`** (Root, per `/init` erzeugen/aktualisieren); global `~/.config/opencode/AGENTS.md`. **Claude-Code-kompatibel:** faellt auf `CLAUDE.md` bzw. `~/.claude/CLAUDE.md` + `~/.claude/skills/` zurueck (abschaltbar `OPENCODE_DISABLE_CLAUDE_CODE=1`) — fuer Frank praktisch, da seine Regeln in `~/proggs/CLAUDE.md` liegen. Mehrere Regeldateien via `instructions`-Array (Globs + Remote-URLs). Quelle: https://opencode.ai/docs/rules · offiziell · 2026-06-18

## 17. Modellwahl nach Aufgabentyp — Entscheidungshilfe ⭐

> **VOLATILITAETS-WARNUNG:** Modellnamen/-versionen drehen sich monatlich (Claude 4.6→4.7→4.8,
> MiniMax M2.5→M2.7, DeepSeek V3.2→V4 …). Die **Heuristik** (unten je Kategorie zuerst) ist das
> Stabile; die konkreten Namen sind **Momentaufnahme Juni 2026** und veralten. Slugs vor Nutzung auf
> `openrouter.ai/models` bzw. per `/models` in OpenCode gegenpruefen. SWE-bench-Zahlen sind grossteils
> selbstberichtet — mit Vorsicht lesen. Quelle: https://openrouter.ai/collections/programming · https://artificialanalysis.ai · offiziell + extern · 2026-06-18

**Stabile Gesamt-Heuristik (vor jeder Wahl 4 Fragen):** (1) Zaehlt Tool-Call-Zuverlaessigkeit/Instruction-Following mehr als rohe Intelligenz? → Agentic-Schiene. (2) Ist die Aufgabe einfach, Latenz/Preis dominant? → kleines/schnelles Modell. (3) Tiefe Denkkette noetig (Architektur, harter Bug)? → Reasoning-Flaggschiff mit hohem Thinking-Effort. (4) Wie viel Kontext WIRKLICH? → 1M-Fenster lohnt erst ab echten >200K Tokens. **Kernprinzip: nicht ein Modell — Routing.** OpenCode ist genau dafuer gebaut (Modell pro Agent/Modus, §16.6).

| Aufgabentyp | Heuristik (stabil) | Momentaufnahme Juni 2026 (Slug) |
|---|---|---|
| **Agentic Coding** (lange Tool-Ketten, autonome Tasks) | Tool-Call-Zuverlaessigkeit + Multi-Turn-Instruction-Following + Stabilitaet > Benchmark-Score; `:exacto` nutzen | `anthropic/claude-sonnet-4.6` (Preis/Leistung), `anthropic/claude-opus-4.7` (haerteste Tasks), `moonshotai/kimi-k2.6` (sehr tool-stabil, guenstig — in OpenCode bewaehrt), `minimax/minimax-m2.7` (sehr guenstig) |
| **Schnell/guenstig** (Boilerplate, kleine Edits, Commit-Msgs) | Latenz + Output-Preis dominieren; Reasoning AUS (`reasoning.enabled=false`/`minimal`) | `google/gemini-2.5-flash-lite`, `x-ai/grok-4.1-fast` (gutes Tool-Calling, 2M Ctx), `deepseek/deepseek-v3.2` |
| **Deep Reasoning / Architektur / harte Bugs** | hoechster Reasoning-Index, lange Denkketten, Reasoning AN (`high`/`xhigh`); Preis/Latenz zweitrangig | `anthropic/claude-opus-4.7`, `openai/gpt-5.4` (bzw. `gpt-5.x-codex`), `google/gemini-3-pro`, `deepseek/deepseek-v4-pro` (bestes Preis/Reasoning offen — Slug pruefen) |
| **Langer Kontext** (grosse Codebasen) | nutzbares Fenster × Input-Preis; auf Long-Context-Aufschlaege achten | `x-ai/grok-4.1-fast` (2M, billig), `google/gemini-3-flash-preview` (1M, billig), `anthropic/claude-*-4.x` (1M ohne Aufschlag, teuer), `qwen/qwen3-coder` (1M, guenstig) |
| **Vision / Multimodal** (Screenshots, PDFs, Diagramme) | native Bild/PDF-Eingabe; visuelles Reasoning + Kontext fuer UI-Debug, billige VL fuer reine OCR | `google/gemini-3-flash-preview` (Text+Bild+Audio+Video+PDF, gut+billig), `anthropic/claude-sonnet-4.6`, `openai/gpt-5.4`, offen: `z-ai/glm-4.5v` |
| **Kostenlos (`:free`)** | nur Hobby/Lernen/leichte Edits — harte Limits, keine Uptime-Garantie, ggf. Daten-Training, oft kein Tool-Use → NICHT fuer produktive Agenten | `qwen/qwen3-coder:free` (staerkstes Free-Coding, 1M), `deepseek/…flash:free` als Fallback-Paar |

**Konkrete OpenCode-Empfehlung (Momentaufnahme):** Build/Edit → `claude-sonnet-4.6` oder `kimi-k2.6`; Architekt/`plan` schwer → `claude-opus-4.7`/`gpt-5.4`; schneller Modus/`small_model` → `gemini-2.5-flash-lite`/`grok-4.1-fast`; Riesen-Codebase → `grok-4.1-fast` (2M); Vision → `gemini-3-flash-preview`. **Achtung Modellfamilien-Grenze:** Claude folgt checklisten-/mechanikgetriebenen Prompts, GPT-5.x knappen prinzipiengetriebenen — beim Modellwechsel eines Agents den System-Prompt mitdenken. Quelle: https://www.glukhov.org/ai-devtools/opencode/oh-my-opencode-agents/ · extern · 2026-06-18

## 18. Account-/Dashboard-Einstellungen (openrouter.ai/settings) — Checkliste ⭐

> OpenRouter ist seit 2026 auf **Workspaces** umgestellt (Routing/Privacy/BYOK/Observability pro
> Workspace; Billing/Credits/Activity/Management-Keys/Org account-global). Quelle: https://openrouter.ai/docs/cookbook/administration/organization-management · offiziell · 2026-06-18

1. **Credits (`/settings/credits`):** **≥ $10 einmalig** kaufen (laeuft nie ab) → schaltet `:free`-Limit von **50 auf 1000 Requests/Tag** frei und senkt Latenz (niedriges Guthaben triggert Extra-DB-Checks + aggressiveres Cache-Verfallen). **Auto-Topup** mit Schwelle ~$5 / Betrag ~$10–20 aktivieren. Negatives Guthaben → `402` (auch bei Free). Mehr Keys/Accounts umgehen Rate-Limits NICHT (global gemessen). Quelle: https://openrouter.ai/docs/api/reference/limits · https://openrouter.ai/docs/guides/best-practices/latency-and-performance · offiziell · 2026-06-18
2. **Privacy/Training (`/settings/privacy`):** drei getrennte Hebel — (a) **„Use of Inputs/Outputs"** (OR darf Daten zur Produktverbesserung nutzen, gibt **1% Rabatt**; Default AUS), (b) **„Enable providers that may train on inputs"** (getrennt fuer paid/free; AN = mehr Modellauswahl v. a. bei `:free`, AUS = Datenschutz), (c) **Data-Policy-Filter** (account-weites Gegenstueck zu `data_collection:"deny"`). **Empfehlung:** max. Auswahl → Training-Toggle AN; Datenschutz → AUS + ggf. ZDR. OpenRouter selbst hat Zero-Data-Retention (nur Metadaten), ausser man optet ein. Quelle: https://openrouter.ai/docs/guides/privacy/data-collection · https://openrouter.ai/docs/guides/privacy/provider-logging · offiziell · 2026-06-18
3. **Provider-Praeferenzen (account-weit):** **Allowed/Ignored Providers** (Whitelist/Blacklist) — nur bei echtem Grund setzen (zu viel = weniger Fallback/Uptime). `only`/`ignore` mergen **additiv** mit per-Request; `zdr` per **OR**. Ein festes **Default-Routing** (sort/order) ist NICHT als Dashboard-Setting dokumentiert → ueber einen **Preset** loesen (Punkt 6). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell · 2026-06-18
4. **API-Keys (`/keys` + `/settings/management-keys`):** **Inference-Keys** (Modell-Requests) vs. **Management-Keys** (nur Provisioning/Analytics, koennen keine Inference → 403). Pro Key: `limit` + `limit_reset` (`daily/weekly/monthly`, Reset Mitternacht UTC), `disabled`, `include_byok_in_limit`. **Pro App/Umgebung ein eigener Key mit Limit** (Leak-Isolation + Kostenzuordnung), quartalsweise rotieren (neu anlegen → umstellen → nach Activity-Check alten loeschen). Quelle: https://openrouter.ai/docs/guides/overview/auth/management-api-keys · https://openrouter.ai/docs/cookbook/administration/api-key-rotation · offiziell · 2026-06-18
5. **Activity/Analytics (`/activity`, `/logs`):** Spend/Tokens/Requests, Filter nach Modell/Provider/Key, CSV/PDF-Export; **„View Raw Metadata" → `provider_responses`** zeigt HTTP-Status je Provider-Versuch (zentral fuers Debugging, v. a. BYOK 401/403/429). Beta-Analytics-API (Management-Key) fuer Drilldowns. Quelle: https://openrouter.ai/docs/cookbook/administration/analytics-cost-control · offiziell · 2026-06-18
6. **Presets (`/settings/presets`):** server-seitig gespeicherte Config (Modell+Fallbacks+Routing+System-Prompt+Parameter), referenziert via `model:"@preset/slug"`, `preset`-Feld oder `model@preset/slug`. **Trennt LLM-Config vom Code**, versioniert mit Rollback — der saubere Weg, ein Default-Modell/-Routing zentral festzunageln. Request-Params ueberschreiben (shallow-merge). Quelle: https://openrouter.ai/docs/guides/features/presets · offiziell · 2026-06-18
7. **BYOK (`/settings/integrations`):** eigene Provider-Keys hinterlegen, **5% Fee** (erste 1 Mio Requests/Monat gratis); Sektionen **Prioritized** (vor OR-Endpoints) / **Fallback**; „Always use" = kein OR-Fallback. **BYOK-Endpoints werden immer ZUERST versucht** (ueberstimmt `provider.order`). Nur sinnvoll bei eigenen Provider-Vertraegen/Rabatten/Compliance. Quelle: https://openrouter.ai/docs/guides/overview/auth/byok · offiziell · 2026-06-18
8. **ZDR (`/settings/privacy`):** vier getrennte Toggles `enforce_zdr_anthropic|_openai|_google|_other` (Legacy `enforce_zdr` deprecated); nur die noetigen setzen, um Frontier-1P-Modelle nutzbar zu halten. EU-Datenresidenz: Base-URL `https://eu.openrouter.ai` (Enterprise). Quelle: https://openrouter.ai/docs/guides/features/zdr · offiziell · 2026-06-18
9. **Org/Workspaces/Budgets (Teams):** Shared Credit Pool, Rollen (Admin/Member), Workspace- + Guardrail-Budgets (taeglich/woechentlich/monatlich/lifetime) als Spend-Limits. Fuer einen Einzelnutzer wie Frank Overkill — per-Key-`limit` reicht. Quelle: https://openrouter.ai/docs/cookbook/administration/organization-management · offiziell + extern · 2026-06-18

## 19. Parameter-Korrekturen & neue Felder (seit 2026-06-17) — Update zu §11–§13

> Verifikation der API-Parameter-Referenz (Researcher #3, alle Quellen offiziell openrouter.ai).
> Diese Punkte **aktualisieren/praezisieren** aeltere Aussagen in §11–§13.

- **Message-Transforms umgestellt:** Das alte `transforms: ["middle-out"]`-Top-Level-Feld ist in der aktuellen Doku durch das Plugin **`plugins: [{ "id": "context-compression" }]`** abgeloest. **Auto-aktiv fuer alle Endpoints mit ≤ 8.192 Token Kontext**; abschalten via `{"id":"context-compression","enabled":false}`. Quelle: https://openrouter.ai/docs/guides/features/message-transforms · offiziell · 2026-06-18
- **`:online` ist DEPRECATED** → stattdessen das Server-Tool **`openrouter:web_search`** (OR „hoistet" ein vorhandenes `web_search`-Tool automatisch; Suffix gefahrlos entfernbar). `:thinking` (Extended Reasoning) und `:extended` (groesseres Kontextfenster) sind weiterhin aktiv. Quelle: https://openrouter.ai/docs/guides/routing/model-variants/online · offiziell · 2026-06-18
- **`usage: {include:true}` ist wirkungslos/deprecated** — vollstaendige Usage (inkl. `cached_tokens`, Kosten, Upstream-Cost, native Tokenizer) kommt **immer automatisch** (beim Streaming im letzten Chunk). Quelle: https://openrouter.ai/docs/use-cases/usage-accounting · offiziell · 2026-06-18
- **`max_tokens` ist deprecated → `max_completion_tokens`** nutzen (manche Provider Minimum 16). Quelle: https://openrouter.ai/docs/api/reference/parameters · offiziell · 2026-06-18
- **Neues Provider-Feld `enforce_distillable_text: true`** — routet nur zu Modellen, deren Autor Text-Distillation erlaubt. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell · 2026-06-18
- **`sort.partition`:** `"model"` (Default, Primaermodell vorn) vs. `"none"` (Endpoints GLOBAL ueber alle Fallback-Modelle sortieren — „nimm das aktuell billigste/schnellste, egal welches Modell"). `max_price` hat zusaetzlich `request` + `image`. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell · 2026-06-18
- **Quantisierungs-Werte (vollstaendig):** `int4, int8, fp4, fp6, fp8, fp16, bf16, fp32, unknown`. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell · 2026-06-18
- **Neuer `verbosity`-Parameter** (`low|medium|high|xhigh|max`, OpenAI-Stil; bei Anthropic → `output_config.effort`). **Reasoning-Effort um `xhigh` und `minimal` erweitert.** Quelle: https://openrouter.ai/docs/api/reference/parameters · offiziell · 2026-06-18
- **Auto Exacto laeuft jetzt by-default bei JEDEM Request mit `tools`** (reordnet Provider nach Tool-Call-Erfolgsrate/Throughput/Benchmarks statt Preis; Tool-Call-Validierung gegen JSON-Schema Draft 7). Opt-out: explizit `sort:"price"` / `:floor` / Account-Default Preis. **Auto-Router** (`openrouter/auto`) jetzt mit `cost_quality_tradeoff` (0–10, Default 7) + `allowed_models`-Wildcards via `plugins:[{id:"auto-router"}]`. Quelle: https://openrouter.ai/docs/guides/routing/auto-exacto · https://openrouter.ai/docs/guides/routing/routers/auto-router · offiziell · 2026-06-18
- **Neue Request-Felder:** `session_id` (≤256 Zeichen, Sticky-Routing; Body schlaegt Header `x-session-id`), `service_tier` (`auto|default|flex|priority`; `flex` ~50% billiger gegen Latenz), `cache_control`, `metadata` (max 16 Paare), `trace` (Observability), `stop_server_tools_when`, `image_config`, `modalities`. Quelle: https://openrouter.ai/docs/api/api-reference/chat/send-chat-completion-request · offiziell · 2026-06-18
- **Anthropic-Beta-Header-Passthrough** `x-anthropic-beta`: `interleaved-thinking-2025-05-14` (Reasoning interleaved) und `structured-outputs-2025-11-13` (**ohne diesen Header strippt OR `strict:true` bei Tools/Schema**); mehrere kommasepariert. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell · 2026-06-18

## 🔗 Bezug zum Bug-Almanach (Erweiterung 2026-06-18)
| Best-Practice (Teil III) | Bug-Abschnitt (`bugs/apis/openrouter-api.md`) |
|---|---|
| 16 OpenCode-Voll-Konfiguration | I38 (`/connect`, kein `auth login`), I39 (`:`-Modell-IDs / `:free`+Tool-Use), I40 (stale Modell-Liste / `openrouter/auto` TUI / Caching) |
| 17 Modellwahl nach Aufgabentyp | C7 (Modell-String/404), H27 (`:nitro`≠Latenz) |
| 18 Account-/Dashboard-Einstellungen | E13/E14/E15 (Limits/Credits/402), F18 (Key-Limit) |
| 19 Parameter-Korrekturen 06-18 | D9 (SSE), H33 (Feature-Drift) |
