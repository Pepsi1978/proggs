# OpenRouter-Integration & Modellauswahl — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Quellen: `offiziell` (opencode.ai/docs, openrouter.ai/docs) bzw. `extern`. **Preise sind ungefähr und
> ändern sich häufig — vor Einsatz auf openrouter.ai/models prüfen** (`opencode models openrouter --refresh`).
> Setup ist auf **Windows und macOS identisch** (nur Speicherort der Config/Credentials unterscheidet sich).

OpenRouter ist ein einheitliches API-Gateway: mit **einem** Key + Endpoint Zugriff auf hunderte Modelle
(Anthropic, OpenAI, Google, xAI, Meta, DeepSeek, Qwen, Moonshot, Z.ai/GLM …). **OpenCode hat OpenRouter als
eingebauten, vorkonfigurierten Provider** — kein npm-Paket, keine baseURL, kein eigener Provider-Block nötig,
nur Key hinterlegen und Modelle wählen. `offiziell`

---

## 1. OpenRouter einrichten

**Variante A — interaktiv (empfohlen, identisch Windows/macOS):** `offiziell`
1. Im OpenRouter-Dashboard unter **openrouter.ai/keys** einen Key erzeugen (beginnt mit `sk-or-...`).
2. OpenCode im Projekt starten (`opencode`), in der TUI `/connect` → **OpenRouter** wählen.
3. Key einfügen.
4. `/models` ausführen — viele OpenRouter-Modelle sind vorgeladen; gewünschtes wählen.

> Aktuelle Doku nutzt **`/connect`** (nicht mehr `opencode auth login`). Key landet in
> `~/.local/share/opencode/auth.json`:
```json
{ "openrouter": { "type": "api", "key": "sk-or-your-key-here" } }
```
(Windows: analog im User-Profil.)

**Variante B — env `OPENROUTER_API_KEY`:** offiziell nicht dokumentiert (nur `/connect`/`auth.json`). Wer den
env-Weg will, baut OpenRouter als Custom-Provider mit `"apiKey": "{env:OPENROUTER_API_KEY}"` (OpenCodes
`{env:VAR}`-Syntax). `offiziell`

**Komplettes `opencode.json`-Beispiel** (offizielles Cookbook — KEIN `npm`/`baseURL`, weil eingebaut): `offiziell`
```json
{ "$schema": "https://opencode.ai/config.json",
  "provider": {
    "openrouter": {
      "models": { "anthropic/claude-sonnet-4": {}, "google/gemini-2.5-flash": {} }
    } } }
```
Der `models`-Block schaltet zusätzliche/nicht vorgeladene OpenRouter-Modelle frei; `{}` übernimmt Defaults.

---

## 2. Modell auswählen + ID-Format

**TUI:** `/models` öffnet die Auswahlliste. **Config-Default:**
```json
{ "$schema": "https://opencode.ai/config.json",
  "model": "openrouter/anthropic/claude-sonnet-4",
  "small_model": "openrouter/deepseek/deepseek-v3.2" }
```

**Modell-ID-Format:** `provider_id/model_id`. Für OpenRouter ist `provider_id` = `openrouter` und
`model_id` = die OpenRouter-`<author>/<model>`-Notation → **dreiteilig**: `offiziell`
- **Global** (`model`/`small_model`/`--model`): `openrouter/<author>/<model>` (z.B. `openrouter/qwen/qwen3-coder`).
- **In `provider.openrouter.models`**: nur `<author>/<model>` ohne `openrouter/`-Präfix (`"qwen/qwen3-coder": {}`).

**`small_model`:** kleines Modell für Nebenaufgaben (Session-Titel). Default `gpt-5-nano` (OpenCode Zen),
überschreibbar auf günstiges OpenRouter-Modell. `offiziell`

**Lade-Priorität beim Start:** `--model`-Flag → `model`-Feld → zuletzt benutztes → erstes nach Priorität.

**OpenCode-eigene Coding-Empfehlung** (gut bei Code-Generierung UND Tool-Calling): GPT 5.2, GPT 5.1 Codex,
Claude Opus 4.5, Claude Sonnet 4.5, Minimax M2.1, Gemini 3 Pro. Warnung: *„there are only a few of them that
are good at both generating code and tool calling."* `offiziell`

---

## 3. OpenRouter Provider Routing (Kernthema)

OpenRouter routet jede Anfrage über mehrere Upstream-Provider. Das Verhalten steuert das `provider`-Objekt im
Request-Body — **OpenCode reicht es 1:1 durch** über `provider.openrouter.models.<model>.options.provider`. `offiziell`

```json
{ "$schema": "https://opencode.ai/config.json",
  "provider": {
    "openrouter": {
      "models": {
        "anthropic/claude-sonnet-4": {
          "options": { "provider": { "order": ["anthropic"], "allow_fallbacks": true } } },
        "moonshotai/kimi-k2": {
          "options": { "provider": { "order": ["baseten"], "allow_fallbacks": false } } }
      } } } }
```
> Es gibt **zwei `options`-Ebenen:** provider-weit (`provider.<id>.options` für baseURL/apiKey/headers) und
> pro Modell (`provider.<id>.models.<model>.options` — hier wird `provider{…}` an OpenRouter durchgereicht).
> Der durchgereichte Block heißt schlicht `options.provider` (nicht `extra_body`/`extraBody`). `offiziell`

**Alle `provider`-Preference-Felder** (snake_case): `offiziell` (openrouter.ai/docs/.../provider-selection)

| Feld | Bedeutung |
|---|---|
| `order` | Provider-Slugs in Prioritätsreihenfolge; **deaktiviert Load-Balancing** |
| `allow_fallbacks` | `false` = nur Top-Provider; scheitert er, scheitert die Anfrage (Default `true`) |
| `sort` | `"price"`/`"throughput"`/`"latency"` oder `{by, partition}`; **deaktiviert Load-Balancing** |
| `only` / `ignore` | nur diese Provider / diese überspringen (mit Account-Listen gemerged) |
| `require_parameters` | nur Provider, die alle Request-Parameter unterstützen (wichtig für Tool-Calling!) |
| `data_collection` | `"deny"` = nur Provider, die keine Daten speichern/trainieren |
| `quantizations` | Filter (int4/int8/fp8/fp16/bf16 …) |
| `max_price` | harter Preis-Filter `{prompt, completion}` ($/M); bricht Request ab, wenn nichts darunter |
| `zdr` | nur Zero-Data-Retention-Endpoints |
| `preferred_min_throughput` / `preferred_max_latency` | deprioritisiert nur, schließt nicht aus |

**Default-Routing (ohne `order`/`sort`):** preisgewichtetes Load-Balancing (inverses Quadrat des Preises);
Provider mit jüngsten Ausfällen werden gemieden. Sobald `sort`/`order` gesetzt ist, **schaltet Load-Balancing
ab** und der Router probiert streng in Reihenfolge.

**Shortcuts am Modell-Slug:** `:nitro` == `sort:"throughput"`; `:floor` == `sort:"price"`; außerdem `:free`,
`:extended`, `:thinking`.

**Slug-Matching:** Basis-Slug (`"google-vertex"`) matcht alle Regionen; voller Slug mit Suffix
(`"deepinfra/turbo"`) zielt auf eine Variante.

---

## 4. Prompt Caching `offiziell` (openrouter.ai/docs/.../prompt-caching)

Caching senkt Kosten für wiederholte Prompt-Teile drastisch. Manche Provider cachen **automatisch**, andere
brauchen `cache_control` pro Nachricht. Bei aktivem Caching nutzt OpenRouter **Provider-Sticky-Routing**, um
den Cache warm zu halten (nicht bei manuellem `provider.order`).

| Anbieter | Aktivierung | Cache-Read | Min-Tokens |
|---|---|---|---|
| OpenAI | automatisch | 0.25×–0.5× | 1024 |
| **Anthropic Claude** | `cache_control` | **0.1×** | 1024 (Sonnet) / 4096 (Opus/Haiku 4.5) |
| **DeepSeek** | **automatisch** | **0.1×** | – |
| **Google Gemini 2.5** | **implizit automatisch** | 0.25× | 1024 (Flash) / 4096 (Pro) |
| Grok / Moonshot (Kimi) | automatisch | 0.25× | – |
| Groq (Kimi K2) | automatisch | 0.5× | – |
| Alibaba Qwen | explizit `cache_control` | **0.1×** | – |

- **Automatisch (kein Zutun):** OpenAI, Grok, Moonshot, Groq, DeepSeek, Gemini 2.5 (implizit).
- **Manuell (`cache_control`):** Anthropic + Alibaba Qwen.
- **Nutzt OpenCode das automatisch?** Für die automatisch cachenden Provider greift Caching ohne Zutun. Für
  Anthropic gibt es die Provider-Option `"setCacheKey": true` (Cache-Key erzwingen). Cache-Wirkung sichtbar im
  `usage.prompt_tokens_details` (`cached_tokens`, `cache_write_tokens`) bzw. `cache_discount`.
- **Konsistenz-Trick:** den Anfang der Nachrichten konstant halten, Variationen ans Ende → höhere Cache-Hit-Rate
  → deshalb lohnt eine **stabile, kurze** AGENTS.md/System-Schicht.

---

## 5. Gute & günstige Coding-Modelle über OpenRouter (Stand Juni 2026)

> Preise **ungefähr, $/Mio Tokens, Stand prüfen.** Tool-Calling ist für OpenCode-Agents **kritisch** — Modelle
> ohne zuverlässiges Tool-Calling taugen nicht als Hauptmodell. Filtern auf openrouter.ai/models nach
> `supported_parameters=tools`.

| Modell (OpenRouter-ID) | Stärke | Preis In/Out (ca.) | Rolle | Tool-Calling |
|---|---|---|---|---|
| `qwen/qwen3-coder` | Stärkstes günstiges Coding-MoE, agentic, großer Kontext | ~$0.22 / ~$1.80 | **Haupt (Budget)** + Subagent | Ja |
| `z-ai/glm-4.6` | Starkes Reasoning + Tool-Use, Long-Horizon | ~$0.43 / ~$1.74 | **Haupt (Budget)** | Ja (stark) |
| `deepseek/deepseek-v3.2` | Solider Allrounder, sehr günstig, Cache 0.1× | ~$0.23 / ~$0.34 | **Haupt (Budget)** + Small | Ja |
| `deepseek/deepseek-v4-flash` | Effizienz-MoE, schnell, billigstes „Small", 1M-Kontext | ~$0.09–0.14 / ~$0.18–0.28 | **Small** / Subagent | Ja |
| `moonshotai/kimi-k2` | End-to-End-Coding über lange Kontexte | ~$0.68 / ~$3.41 | **Haupt** | Ja |
| `google/gemini-2.5-flash` | Multimodal, großer Kontext, implizites Caching | günstig | **Small** | Ja |
| `google/gemini-3-pro` | Premium-Haupt | Premium | **Haupt (Premium)** | Ja |
| `anthropic/claude-sonnet-4.5` | Top-Coding + bestes agentisches Tool-Calling | Premium (Mid) | **Haupt (Premium)** | Ja (exzellent) |
| `anthropic/claude-opus-4.5` | Stärkstes Reasoning/Coding | Premium (hoch) | **Haupt (komplex)** | Ja (exzellent) |
| `openai/gpt-5.2` / `gpt-5.1-codex` | Top-Coding, Codex-optimiert | Premium | **Haupt (Premium)** | Ja |
| `minimax/minimax-m2.1` | Gut bei Code + Tool-Calling (OpenCode-empfohlen) | Mid | Haupt | Ja |

**Einordnung:**
- **Bestes Preis-Leistung als Budget-Haupt:** `qwen/qwen3-coder` oder `z-ai/glm-4.6` (für agentic Coding +
  Tool-Use gebaut). `extern`
- **Günstigstes `small_model`/Subagent:** `deepseek/deepseek-v4-flash` oder `deepseek/deepseek-v3.2`. `extern`
- **Premium-Haupt (Qualität vor Preis):** Claude Sonnet/Opus 4.5, GPT 5.2 / 5.1 Codex, Gemini 3 Pro. `offiziell`
- **Tool-Calling-Warnung:** sehr kleine/Free-Modelle (z.B. Llama 3.1 8B) für reine Titel/Klassifikation okay,
  für echte Agent-Loops mit Tools unzuverlässig.

---

## 6. Kosten / Limits / Free / BYOK `offiziell`/`extern`

- **Credits:** Pay-as-you-go; Kaufgebühr 5.5 % (min. $0.80). Negativer Saldo → `402` (auch bei Free).
- **Rate Limits:** mehr Accounts/Keys erhöhen Limits **nicht**. Last über mehrere Modelle verteilen hilft.
  Key-Status via `GET https://openrouter.ai/api/v1/key`.
- **Free (`:free`):** 20 Anfragen/Min; **50/Tag** bei < 10 Credits, **1000/Tag** ab einmalig ≥ 10 gekauften
  Credits. Free-Hosting kann gedrosselt/pausiert werden → **nicht produktionssicher**, kein SLA. Standouts:
  `qwen/qwen3-coder:free`, `deepseek/deepseek-r1:free`, `meta-llama/llama-3.3-70b-instruct:free`,
  `google/gemini-flash:free`.
- **BYOK:** eigene Provider-Keys über OpenRouter routen; erste 1.000.000 BYOK-Requests/Monat gratis, darüber
  5 % des normalen Preises als Routing-Gebühr.
- **Privacy:** OpenRouter loggt Code-Prompts nicht (außer Opt-in). `data_collection:"deny"` / `zdr:true` für
  vertraulichen Code.

---

## 7. Best Practices: günstiges, gutes Coding-Harness über OpenRouter

Empfohlenes `opencode.json` (Budget-Setup mit Routing-Optimierung):
```json
{ "$schema": "https://opencode.ai/config.json",
  "model": "openrouter/qwen/qwen3-coder",
  "small_model": "openrouter/deepseek/deepseek-v4-flash",
  "provider": {
    "openrouter": {
      "models": {
        "qwen/qwen3-coder": {
          "options": { "provider": { "sort": "throughput", "allow_fallbacks": true,
            "data_collection": "deny", "max_price": { "prompt": 0.5, "completion": 2 } } } },
        "deepseek/deepseek-v4-flash": {
          "options": { "provider": { "sort": "price", "allow_fallbacks": true } } },
        "anthropic/claude-sonnet-4": {
          "options": { "provider": { "order": ["anthropic"], "allow_fallbacks": true } } }
      } } } }
```

1. **Modell-Tiering:** Hauptmodell für echte Arbeit (`qwen3-coder`/`glm-4.6`/Premium Sonnet 4.5), günstiges
   `small_model` (`deepseek-v4-flash`). Premium nur für komplexes Reasoning.
2. **Tool-Calling zuerst prüfen:** nur Modelle mit `supported_parameters=tools` als Haupt/Subagent.
3. **Routing für Kosten:** `:floor` / `sort:"price"` + `max_price`-Obergrenze; für Latenz `:nitro`/`sort:"throughput"`.
4. **Caching ausnutzen:** möglichst automatisch cachende Modelle (DeepSeek/Gemini/OpenAI). `provider.order`
   nicht zu eng setzen (sonst kein Sticky-Routing/Cache-Warmhaltung).
5. **Resilienz:** `allow_fallbacks:true` lassen (Provider-Ausfälle automatisch umgeleitet).
6. **Free zum Prototyping, Paid zur Produktion** (ab ernsthafter Nutzung $10 Credits → Tageslimit 50→1000).
7. **Privacy bei Firmencode:** `data_collection:"deny"` + ggf. `zdr:true`.

## Quellen
**Offiziell:** opencode.ai/docs/providers, /models; openrouter.ai/docs cookbook/coding-agents/opencode-integration,
guides/routing/provider-selection, guides/best-practices/prompt-caching, api/reference/limits.
**Extern (Preise prüfen):** klymentiev.com/blog/openrouter-free-tier; openrouter.ai/models-Seiten (qwen3-coder,
glm-4.6, deepseek-v3.2, deepseek-v4-flash, kimi); simonwillison.net; morphllm/teamday/costgoat (Coding-Rankings).
