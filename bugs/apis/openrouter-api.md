# Bekannte Bugs: OpenRouter API (Integration)

> PFLICHT-LESEN vor Arbeit an einer OpenRouter-Integration (Aggregator/Gateway, EIN OpenAI-kompatibler
> Endpunkt für viele Anbieter). Stand: zuletzt recherchiert am 2026-06-08.
> Endpoint: `https://openrouter.ai/api/v1`. Zweite Seite: `best-practices/apis/openrouter-api.md`.
> Stand-Erweiterung 2026-06-17: §G (Claude Code / CLI-Harness-Anbindung) + §H (neuere Features) ergänzt.
> Stand-Erweiterung 2026-06-18: §I (OpenCode-spezifische Fallen — Franks Umgebung) ergänzt. Gegenstück-BP §16–§19.
> Stand-Erweiterung 2026-06-21: §41 (MiniMax M3 + `web_search` → leere Antwort / Tool-Call-Leak) — eigener Vorfall bei der Second-Brain-Recherche.
> Stand-Erweiterung 2026-06-21: §42 (`:online` leakt agentische Folge-Tool-Calls als JSON-Text → keine Antwort) — Hostinger-A/B-Test, vom adversarischen Vergleich aufgedeckt.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Antwort erfolgreich aber `content` leer ⭐ | `HTTP-Referer` + `X-Title` im Client-Konstruktor immer senden | §A |
| 2 | Schwankende Qualität / anderes Backend ⭐ | `provider.order` + `allow_fallbacks:false` für Determinismus | §B |
| 3 | Garbled/CJK-Output | `provider.quantizations:["fp16","bf16"]` als Allowlist | §B |
| 4 | `response_format`/`stop` wirkt nicht | `provider.require_parameters:true` setzen | §B |
| 5 | SSE-Parser bricht ⭐ | `:`-Kommentarzeilen überspringen, `[DONE]` + Mid-Stream-`error` (HTTP 200) | §D |
| 6 | 429 / 402 | `:free`=50 RPD; 402 ≠ 429; `error.metadata.provider_name` auswerten | §E/§F |
| 7 | Modell-String / 404 | `anbieter/modell`, Liste via `/api/v1/models`, Fallback-`models`-Array | §F |
| 8 | Claude Code anbinden ⭐ | Base-URL `…/api` (NICHT `/api/v1`!), `ANTHROPIC_API_KEY=""`, Key in `ANTHROPIC_AUTH_TOKEN`; Anthropic 1P als Top-Provider | §G |
| 9 | CLI-Agent Modell-String | LiteLLM-Tools `openrouter/<v>/<m>`, Eigenbau nacktes `<v>/<m>`; Cursor braucht `/cursor`-Suffix | §G |
| 10 | Caching/Reasoning/neu | Auto-Caching nur Anthropic 1P; `provider.order` killt Sticky-Routing; `reasoning_details` unverändert zurück; `:nitro`≠Latenz | §H |
| 11 | **OpenCode** ⭐ | Login = `/connect` (kein `opencode auth login`/`OPENROUTER_API_KEY`-Env); `:free`/`:`-IDs crashen + oft kein Tool-Use; Routing pro Modell unter `options.provider` | §I |
| 12 | **Server-Tools / PDF** ⭐ | 6 Server-Tools (web_search/web_fetch/datetime/image_generation/apply_patch[Responses]/fusion); web_fetch-Engine `openrouter`=gratis; `file-parser`-OCR kostet auch bei BYOK, `native` ohne `annotations`; non-streaming web_search → Keep-Alive bricht JSON (§D9) | §35 |
| 13 | **`web_search` unter PARALLELITÄT → leer/kaputt** ⭐ | Last-Fehler (intermittent): bei vielen gleichzeitigen Requests leere Response/JSONDecodeError/Timeout/Provider-Routing-Müll — NICHT modellspezifisch (M3 läuft einzeln sauber). FIX: max 2 parallel (Continuous-Spawning) + Retry+Backoff + alle Exceptions fangen (`or-research.py` #47034/35) | §41 |
| 14 | **`:online` liefert keine Antwort, nur JSON-Tool-Calls** ⭐ | `<modell>:online` leakt agentische FOLGE-Suchen als Text (`{"name": "web_search", "input": {"query": …}}`) statt sie auszuführen → KEINE Antwort. Auch ohne Last. Detektor MUSS JSON-Tool-Call-Shape erkennen (Regex `\{"name":\s*"\w+",\s*"(input\|arguments)":`), nicht nur XML-Marker. Bei Mehrfachsuche-Themen lieber `web_search`-Server-Tool/Firecrawl | §42 |

---

## A. Header / Attribution

### 1. Content verschwindet still ohne `HTTP-Referer`/`X-Title` ⭐
- **Symptom:** Request erfolgreich (`finish_reason:"stop"`), aber `message.content` leer. Kein Fehler.
- **Ursache:** bei Non-localhost-Keys erwartet OpenRouter `HTTP-Referer` (Site) + `X-Title` (App-Name); ohne strippt der Proxy den Content.
- **FIX:** beide Header IMMER mitsenden; lokal `HTTP-Referer: localhost`.
- **Quelle:** https://openrouter.ai/docs/app-attribution · https://blog.gerardbeckerleg.com/posts/openrouter-api-response-missing-text-in-json-payload/

### 2. Header im Wrapper gesetzt, aber nicht im echten Request
- **Ursache:** OpenAI-SDK-Client wird erstellt BEVOR der Wrapper läuft, oder Header downstream überschrieben.
- **FIX:** Header beim Client-Konstruktor (`default_headers`), nicht per Request-Override; Wire-Request prüfen.

---

## B. Provider-Routing / stille Fallbacks

### 3. Stille Provider-Substitution mit abweichendem Verhalten ⭐
- **Symptom:** gleicher `model`-String → mal anderes Tool-Call-Format/Tokenizing, schwankende Qualität.
- **Ursache:** Default-Load-Balancing (gewichtet nach inversem Preis-Quadrat); bei Ausfall springt es mit `allow_fallbacks:true` (Default) auf anderes Backend.
- **FIX:** `provider.order` + `allow_fallbacks:false` für deterministisches Routing; `provider.only`/`quantizations` einschränken.
- **Quelle:** https://openrouter.ai/docs/guides/routing/provider-selection

### 4. Quantisierung (FP4/Int4) → garbled / CJK-Encoding-Fehler
- **FIX:** `provider.quantizations:["fp16","bf16"]` (oder fp8) als Allowlist.

### 5. `:nitro`/`:floor`-Suffixe ändern Routing
- **Ursache:** `:nitro`=`sort:throughput`, `:floor`=`sort:price`; beide deaktivieren Default-Balancing.
- **FIX:** Suffix bewusst; für stabile Qualität `provider.order` explizit.

### 6. `require_parameters:false` → Params werden still ignoriert
- **Symptom:** `response_format`/JSON-Mode/`stop` wirken nicht, keine Fehlermeldung.
- **FIX:** `provider.require_parameters:true` → nur Provider, die ALLE Request-Parameter unterstützen.

---

## C. Tool / Function Calling

### 7. `No endpoints found that support the provided 'tool_choice' value` (404)
- **Ursache:** nicht jedes Backend kann Tools; OpenRouter filtert auf Tool-fähige Provider — bleibt keiner, 404.
- **FIX:** Tool-fähiges Modell/Provider (`provider.order`); `tool_choice:"auto"` statt erzwungen; ggf. `openrouter/auto`.

### 8. Tool-Call-Argumente leer `{}` durch SSE-Fragmentierung
- **FIX:** Argument-Strings pro `tool_calls[].index` über alle Chunks akkumulieren, erst bei Stream-Ende parsen.

---

## D. Streaming / SSE

### 9. `: OPENROUTER PROCESSING`-Kommentarzeile bricht Parser ⭐
- **Symptom:** `unexpected end of JSON input`/Stream-Abbruch — ODER bei `stream:false` ein `json.JSONDecodeError: Expecting value: line N` mitten in der Antwort.
- **Ursache:** OpenRouter sendet SSE-Kommentarzeilen (mit `:`) gegen Timeouts. **Tritt AUCH bei non-streaming auf**, wenn die Verarbeitung lange dauert (agentische `web_search`/`web_fetch` mit mehreren Suchen): Keep-Alive-Zeilen `: OPENROUTER PROCESSING` kommen VOR dem JSON-Body, sodass `json.loads(ganze_antwort)` bricht. (Eigener Vorfall 2026-06-20 in `or-research.py` bei 16 agentischen Suchen.)
- **FIX:** Streaming → per SSE-Spec alle `:`-Zeilen überspringen (`eventsource-parser`/OpenAI-SDK). Non-streaming → erst `json.loads` versuchen, bei `JSONDecodeError` ab dem ersten `{` parsen: `json.JSONDecoder().raw_decode(raw[raw.find('{'):])[0]` (überspringt die führenden Keep-Alive-Kommentare; `raw_decode` ignoriert evtl. Trailing).
- **Quelle:** https://openrouter.ai/docs/api/reference/streaming · offiziell + eigener Vorfall/Fix 2026-06-20 (`proggs/or-research.py` + `mm-research.py`)

### 10. `data: [DONE]` als JSON geparst
- **FIX:** vor JSON-Parse auf `[DONE]` prüfen.

### 11. Mid-Stream-Fehler mit HTTP 200
- **Ursache:** Header schon gesendet → Fehler kommt als SSE `{"error":{...}}` mit `finish_reason:"error"`.
- **FIX:** in jedem Chunk auf `error`-Feld + `finish_reason:"error"` prüfen.

### 12. Duplicate `finish_reason`-Chunks → „empty response"
- **FIX:** nur ersten `finish_reason` werten; akkumulierten Content behalten.

---

## E. Credits / Billing / Rate Limits

### 13. `:free`-Modelle hart limitiert (50 RPD) ⭐
- **Symptom:** 429 trotz Guthaben bei `:free`-Modell.
- **Ursache:** `:free` = 50 Req/Tag bei <10 USD je gekauft; nach Kauf ≥10 USD → 1000 RPD. Free generell 20 RPM.
- **FIX:** Backoff + Jitter, `Retry-After` honorieren, einmalig ≥10 USD Credits kaufen, oder bezahltes Modell.
- **Quelle:** https://openrouter.zendesk.com/hc/en-us/articles/39501163636379

### 14. 429 mit zwei Quellen (OpenRouter vs. Backend)
- **FIX:** `error.metadata.provider_name` prüfen — vorhanden = Backend-Limit; sonst OpenRouter-Limit. `Retry-After` (auch bei 503) beachten.

### 15. 402 bei leerem Guthaben
- **FIX:** 402 separat behandeln (nicht als Rate-Limit retrien); Credits aufladen; Monitoring per `/api/v1/credits`.

### 16. BYOK: trotz eigenem Key abgerechnet (5 % Aufschlag)
- **Ursache:** BYOK kostet 5 % aus OpenRouter-Credits (erste 1M Req/Monat frei).
- **FIX:** einplanen; falls Key ignoriert: BYOK-Aktivierung pro Provider in Settings prüfen.

---

## F. Modell-String / Auth / Fehler-Schema

### 17. `no endpoints for this model found` (404) bei deprecated/umbenanntem Modell
- **FIX:** Modell-Liste per `/api/v1/models` re-fetchen; Fallback via `models`-Array; nicht hardcoden (`:free` besonders flüchtig).

### 18. OpenRouter- vs. Provider-Fehler verwechselt + 401
- **Ursache:** Schema `{error:{code,message,metadata?}}`. `code==HTTP-Status` = OpenRouter-Ebene; HTTP 200 + `metadata.provider_name`/`.raw` = durchgereichter Provider-Fehler. 401 = ungültiger `sk-or-...`-Key.
- **FIX:** `error.metadata.provider_name`/`.raw`/`.reasons`/`.patterns` auslesen; 502 = Modell down (retry/Fallback), 503 = kein Provider erfüllt Routing (Routing lockern).
- **Quelle:** https://openrouter.ai/docs/api/reference/errors-and-debugging

---

## G. Claude Code & andere CLI-Coding-Agenten (Anbindung)

> Recherche 2026-06-17. Quelle für §G überwiegend offiziell: https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration (sowie codex-cli/opencode/cursor-Cookbooks). Gegenstück: `best-practices/apis/openrouter-api.md` §9–§10.

### 19. Claude Code: falsche Base-URL `/api/v1` statt `/api` ⭐
- **Symptom:** native „Anthropic Skin" greift nicht; Auth-/Format-Fehler obwohl OpenRouter konfiguriert.
- **Ursache:** Claude Code spricht das Anthropic-`/v1/messages`-Format. Die native Skin liegt auf `https://openrouter.ai/api` — die OpenAI-kompatible `/api/v1` ist die falsche URL dafür.
- **FIX:** `ANTHROPIC_BASE_URL="https://openrouter.ai/api"` (OHNE `/v1`). Verifizieren mit `/status`.
- **Quelle:** https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · offiziell

### 20. Claude Code: `ANTHROPIC_API_KEY` unset statt Leerstring → Fallback auf Anthropic-Direkt-Auth
- **Symptom:** Claude Code versucht trotz OpenRouter-Config die Anthropic-Server zu erreichen, Auth-Fehler.
- **Ursache:** Ist `ANTHROPIC_API_KEY` nur unset (null) statt explizit leer, fällt Claude Code auf Direkt-Auth zurück. OR-Key gehört in `ANTHROPIC_AUTH_TOKEN`.
- **FIX:** `export ANTHROPIC_API_KEY=""` + `export ANTHROPIC_AUTH_TOKEN="$OPENROUTER_API_KEY"`.
- **Quelle:** gleiche Cookbook-Seite · offiziell

### 21. Claude Code: Env-Vars in projekt-`.env` werden ignoriert
- **Ursache:** der native Installer liest keine projekt-lokale `.env`.
- **FIX:** Variablen ins Shell-Profil (`~/.bashrc`/`~/.zshrc` bzw. Windows-User-Env), Terminal neu starten.
- **Quelle:** gleiche Cookbook-Seite · offiziell

### 22. Claude Code: `model-not-found` für OpenRouter-Modelle
- **Ursache:** Auth-Konflikt beim Start — entweder gecachte Anthropic-OAuth-Session ODER ein echter `ANTHROPIC_API_KEY` noch in der Shell.
- **FIX:** (a) gecachtes OAuth: `/logout`, dann `claude` neu starten; (b) `ANTHROPIC_API_KEY` in Shell: `/logout` hilft NICHT → `ANTHROPIC_API_KEY=""` + Terminal neu. Mit `/status` prüfen.
- **Quelle:** gleiche Cookbook-Seite · offiziell

### 23. Nicht-Anthropic-Modelle über Claude Code → leere Antworten / Crash / kein Failover ⭐
- **Symptom:** nach Tool-Call leere Follow-up-Antwort (0 Tokens); CLI-Crash beim ersten Tool-Use; bei Provider-Fehler mitten im Stream kein Failover.
- **Ursache:** Claude Code ist auf Anthropic-Modelle optimiert; OpenRouter garantiert die Kompatibilität nur über den Anthropic-First-Party-Provider. Sobald das erste Token gestreamt ist, ist der HTTP-Status committed → kein stiller Provider-Failover mehr.
- **FIX:** Anthropic-Modelle durchreichen + Anthropic 1P als Top-Priority-Provider. Fremdmodelle (GLM/Qwen/DeepSeek/Kimi) nur mit Bewusstsein der Bruchgefahr; ggf. claude-code-router `enhancetool`-Transformer.
- **Quelle:** https://openrouter.ai/docs/cookbook/coding-agents/claude-code-integration (offizielle Kompatibilitätswarnung) · openclaw Issue #1622 (extern)

### 24. Modell-String-Format-Falle bei anderen CLI-Agenten
- **Symptom:** „model not found" obwohl Key korrekt.
- **Ursache:** LiteLLM-basierte Tools (aider, OpenHands) erwarten `openrouter/<vendor>/<model>` (doppeltes Präfix); `openai/<model>` gegen OR funktioniert NICHT. Eigenbau-Tools (Codex, Goose, Cline, Roo, Kilo, Continue) wollen das nackte OR-Slug `<vendor>/<model>`.
- **FIX:** Vor Konfiguration klären, ob das Tool LiteLLM nutzt; Modell-String entsprechend wählen.
- **Quelle:** https://docs.litellm.ai/docs/providers/openrouter · https://docs.openhands.dev/openhands/usage/llms/openrouter · offiziell

### 25. Cursor: Tool-Calls brechen ohne `/cursor`-Suffix in der Base-URL
- **Ursache:** Cursor braucht die spezielle Base-URL `https://openrouter.ai/api/v1/cursor`, nicht `/api/v1`.
- **FIX:** „Override OpenAI Base URL" auf `…/api/v1/cursor` + OR-Key.
- **Quelle:** https://openrouter.ai/docs/cookbook/coding-agents/cursor-integration · offiziell

### 26. Codex CLI: falsche `wire_api` / Präfix
- **Ursache:** Codex unterstützt OR nur über manuellen `[model_providers.openrouter]`-Block; braucht `wire_api="chat"` (nicht `responses`) und das nackte Slug OHNE `openrouter/`-Präfix.
- **FIX:** `~/.codex/config.toml` mit `base_url="https://openrouter.ai/api/v1"`, `env_key="OPENROUTER_API_KEY"`, `wire_api="chat"`; Modell = `<vendor>/<model>`.
- **Quelle:** https://openrouter.ai/docs/cookbook/coding-agents/codex-cli · offiziell

### 26b. SICHERHEIT: LiteLLM-Versionen 1.82.7 / 1.82.8 mit Credential-Stealing-Malware
- **FIX:** diese Versionen meiden, saubere Version pinnen, bei Installation Credentials rotieren (Projektregel „Sicherheit bei externem Code").
- **Quelle:** Anthropic-Warnung via morphllm.com/claude-code-litellm · extern — vor LiteLLM-Einsatz verifizieren.

### 26c. OpenCode: Crash bei Modell-IDs mit `:` (z.B. `:free`-Varianten)
- **Symptom:** OpenCode bricht bei OpenRouter-Modell-IDs mit `:` im Identifier ab.
- **Ursache:** Parser-Problem mit dem `:`-Zeichen im Modell-Slug (GitHub Issue #749).
- **FIX:** bei `:free`/Variant-Slugs prüfen ob gefixt; sonst Voll-Slug ohne Variant nutzen.
- **Quelle:** github.com/sst/opencode Issue #749 · extern (nicht abschließend verifiziert)

### 26d. aider: leere/abgelehnte Antworten ohne passende OpenRouter-Privacy-Settings
- **Symptom:** aider bekommt keine Antwort / Fehler obwohl Key korrekt.
- **Ursache:** manche OR-Routen verlangen unter openrouter.ai/settings/privacy „enable providers that may train on inputs"; ist das aus und kein konformer Provider verfügbar → kein Endpoint → Fehler.
- **FIX:** bewusst entscheiden — entweder das Privacy-Setting aktivieren ODER `provider.data_collection:"deny"` setzen und Provider wählen, die ohne Training liefern.
- **Quelle:** aider.chat/docs/llms/openrouter.html · offiziell

---

## H. Caching / Reasoning / neuere Plattform-Features

> Recherche 2026-06-17. Quellen überwiegend offiziell (openrouter.ai/docs). Gegenstück: `best-practices/apis/openrouter-api.md` §11–§13.

### 27. `:nitro` ≠ niedrige Latenz (Throughput vs. TTFT)
- **Symptom:** interaktives Coding-CLI fühlt sich trotz `:nitro` träge an.
- **Ursache:** `:nitro` == `sort:"throughput"` (Token/s bei voller Generierung), nicht TTFT. Für interaktive Nutzung zählt Time-to-First-Token.
- **FIX:** `provider.sort:"latency"` bzw. `preferred_max_latency:{p90:…}` statt `:nitro`.
- **Quelle:** https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

### 28. `max_price` blockiert den Request, `preferred_*` nicht
- **Symptom:** unerwarteter Request-Fehler statt langsamerer/teurerer Antwort.
- **Ursache:** `max_price` schließt aus → kein Anbieter im Limit = Fehler. `preferred_min_throughput`/`preferred_max_latency` depriorisieren nur (blockieren nie).
- **FIX:** harte Kostengrenze = `max_price`; weiche Performance-Präferenz = `preferred_*` (Percentile p50/p75/p90/p99, 5-Min-Fenster).
- **Quelle:** https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

### 29. Auto-Caching (Top-Level `cache_control`) pinnt unbemerkt nur auf Anthropic 1P
- **Symptom:** kein Cache-Hit über Bedrock/Vertex; unerwartete Provider-Einschränkung.
- **Ursache:** Top-Level-`cache_control` bei Claude wird nur von Anthropic 1P unterstützt → OR schließt Bedrock/Vertex aus.
- **FIX:** für Cross-Provider-Fallback explizite Per-Block-Breakpoints (max. 4) statt Top-Level.
- **Quelle:** https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell

### 30. `provider.order` deaktiviert Sticky-Routing → Cache-Verlust
- **Ursache:** manuelles Ordering schaltet das automatische Cache-Warm-Sticky-Routing ab.
- **FIX:** für Cache-Effizienz `session_id` (Body) bzw. Header `x-session-id` (max. 256 Z.) statt `order`.
- **Quelle:** https://openrouter.ai/docs/guides/best-practices/prompt-caching · offiziell

### 31. Reasoning-Faden bricht beim Tool-Calling
- **Symptom:** Reasoning-Modell verliert Kontext/Fehler zwischen Tool-Call und Tool-Result.
- **Ursache:** `reasoning_details`-Array wird verändert/umsortiert/weggelassen zurückgeschickt.
- **FIX:** `message.reasoning_details` unverändert und in Reihenfolge mitschicken.
- **Quelle:** https://openrouter.ai/docs/guides/best-practices/reasoning-tokens · offiziell

### 32. `usage.include:true` / `stream_options.include_usage` deprecated & wirkungslos
- **Ursache:** Usage kommt jetzt immer automatisch (letzter SSE-Chunk).
- **FIX:** Feld entfernen; `usage.cost`/`prompt_tokens_details.cached_tokens` direkt lesen.
- **Quelle:** https://openrouter.ai/docs/cookbook/administration/usage-accounting · offiziell

### 33. Response-Caching (Beta) ignoriert `temperature`/`seed`, Body-Reihenfolge ändert Cache-Key
- **Symptom:** identische verbatim-Antwort trotz geänderter Sampling-Params; oder unerwartete Cache-Misses.
- **Ursache:** Cache-Key = API-Key + Modell + Endpoint + Stream-Modus + SHA-256(Body); Hit gibt verbatim zurück, ignoriert `temperature`/`seed`. JSON-Property-Reihenfolge/explizite Defaults ändern den Key. Bei Account-ZDR deaktiviert.
- **FIX:** Response-Caching nur für deterministisch-gewünschte Fälle (Tests, Crash-Resume) via `X-OpenRouter-Cache`; Body stabil halten.
- **Quelle:** https://openrouter.ai/docs/guides/features/response-caching · offiziell

### 34. BYOK überschreibt `provider.order`
- **Symptom:** trotz Kosten-`order` landet der Request zuerst auf dem BYOK-Endpoint.
- **Ursache:** BYOK-Endpoints werden IMMER zuerst versucht (auch wenn in `order` hinten).
- **FIX:** über BYOK-Key-Sektion/Filter steuern; „Always use" nur bewusst (verhindert Shared-Fallback → Rate-Limit-Risiko). BYOK-Fee 5% (erste 1 Mio Req/Monat frei).
- **Quelle:** https://openrouter.ai/docs/guides/overview/auth/byok · offiziell

### 35. `:online` / `web`-Plugin deprecated; SECHS Server-Tools (Beta); `file-parser`-Plugin-Fallen ⭐
- **Ursache:** Web-Suche läuft jetzt über das Server-Tool `openrouter:web_search` (Modell entscheidet selbst, 0–N Suchen); `:online`/`web`-Plugin abgelöst (auch der `:online`-Suffix).
- **Es gibt SECHS Server-Tools** (offizielle Übersichtsseite, verifiziert 2026-06-20), je im `tools`-Array als `{"type":"openrouter:<name>"}`:
  `web_search` (agentische Suche), `web_fetch` (vollen Seiteninhalt einer URL — Engine **"OpenRouter" = KOSTENLOS**, Exa/Parallel je $0.001/Fetch, `max_content_tokens` gegen Kontext-Überlauf), `datetime`, `image_generation`, `apply_patch` (**nur Responses API**), `fusion`.
  Kosten `web_search`: Exa/Parallel **$0.005**/Suche (bis 10 Treffer) +$0.001/Treffer; `native`=Provider-Preis; `firecrawl`=BYOK. Steuerung: `max_results` (1–25, Default 5), `max_total_results` (Cap über ALLE Suchen = Kostenbremse in Agent-Loops). **Achtung:** eine ältere Notiz (06-17) nannte zusätzlich `subagent`/`advisor` — die stehen 06-20 NICHT mehr auf der offiziellen server-tools-Seite → vor Nutzung verifizieren. Modell braucht Tool-Calling-Support, sonst nur das alte (deprecatete) `web`-Plugin (genau 1 Suche/Request).
- **`file-parser`-Plugin (PDF/Doku) — Fallen:** `plugins:[{"id":"file-parser","pdf":{"engine":...}}]`. Engines: `mistral-ocr` ($2/1000 Seiten, **OCR-Kosten fallen AUCH bei BYOK an** — OR nutzt eigenen Mistral-Key; max 8 Bilder/PDF), `cloudflare-ai` (kostenlos, PDF→Markdown), `native` (als Input-Tokens des Modells, produziert **KEINE** `annotations`), `pdf-text` (deprecated → cloudflare-ai). **Re-Parse-Kosten sparen:** die zurückgegebenen `annotations` (stabiler `file.hash`) im Folge-Request an der vorherigen Assistant-Message mitsenden → OR überspringt das erneute Parsen (auch im Error-Pfad unter `error.metadata.file_annotations`). Datei als `{"type":"file","file":{"filename":..,"file_data":"data:application/pdf;base64,.."}}`; Feldname `file_data` (Python) vs `fileData` (TS-SDK) ist in den Docs inkonsistent.
- **FIX:** auf `openrouter:web_search`/`web_fetch` umstellen, `max_total_results` als Kosten-Cap; web_fetch-Engine `openrouter` (gratis) für Volltext; Beta → API-Änderungen einplanen. PDF-Pipelines: `annotations` cachen, Engine bewusst wählen (cloudflare-ai gratis vs. mistral-ocr $2/1000 S.).
- **Quelle:** https://openrouter.ai/docs/guides/features/server-tools · …/server-tools/web-search · …/features/plugins (PDF inputs) · offiziell · verifiziert 2026-06-20 (Firecrawl+MiniMax A + or-research B)

### 41. `openrouter:web_search` unter PARALLELITÄT → leere/kaputte Antwort (Last-Fehler, intermittent) ⭐
- **Symptom:** Bei mehreren GLEICHZEITIGEN Requests (Schwarm) "gelingt" ein Teil scheinbar + kostet, aber `choices[0].message.content` ist (a) **leer**, (b) der Lauf scheitert mit **`JSONDecodeError: Expecting value: line 1 column 1`** (= komplett leere HTTP-Response), (c) **Timeout** (Request hängt), oder (d) seltener **Tool-Call-Leak als Text** (`]<]minimax[>[<invoke name="openrouter_web_search">…`). Einzelläufe und wenige parallele laufen dagegen sauber.
- **Ursache (LIVE diagnostiziert 2026-06-21, korrigiert frühere Fehldiagnose):** Es ist **NICHT primär modell-spezifisch** — der wahre Auslöser ist **LAST durch Parallelität**. Unter vielen gleichzeitigen Requests liefert OpenRouter intermittent **leere Responses / Timeouts**, und das **Provider-Routing variiert** (derselbe Slug landet bei wechselnden Anbietern, manche zerschießen die Tool-Antwort). **Beweis:** 7 parallele `minimax/minimax-m3`-Läufe → **3/7 kaputt** (2× JSONDecodeError/leer, 1× Timeout); **dieselben 7 Läufe als Einzelläufe ODER mit Retry → 7/7 sauber** (3 brauchten 1 Retry). Die 4 sauberen liefen alle über `provider=OpenAI`. M3 ist also **rehabilitiert** — die frühere „M3 nicht tool-stabil"-Diagnose war zu eng (sie verwechselte ein Last-Symptom mit einer Modell-Eigenschaft). Mitverstärkend: `or-research.py` fing früher **nur `HTTPError`** ab → leere Response/Timeout/Connection crashten ungefangen durch.
- **FIX (umgesetzt in `or-research.py`, #47034/#47035):** (1) **Retry mit Backoff** (`OR_RETRIES`, Default 3): bei leerer/kaputter Response, Timeout, Connection-Fehler ODER leerem/leaky `content` automatisch neu versuchen — der nächste Versuch erwischt fast immer einen guten Slot. (2) **ALLE** relevanten Exceptions fangen (`JSONDecodeError`, `URLError`, `TimeoutError`, `ConnectionError`, HTTP 408/429/5xx), nicht nur `HTTPError`. (3) **Leer-/Leak-Detektor** meldet einen final kaputten Lauf als Fehler (exit≠0) statt still „erfolgreich". (4) **Engine B auf max 2 parallel begrenzen** (Continuous-Spawning mit 2, wie Firecrawl) — empirisch läuft `minimax/minimax-m3` bei 2 parallel stabil, bei 7 gehen ~3/7 kaputt; der Retry fängt Restfälle. Modell-Wechsel ist NICHT nötig; MiniMax M3 ist bei niedriger Last zuverlässig (Frank-Entscheidung 2026-06-21: bestehendes System lassen, nur Eskalation auf max 2 parallel stabilisieren). Zusatz-Befund: Das OpenRouter-Workspace-**Web-Search-Plugin** lässt sich nicht ausschalten (nur Engine wählen) — als Kollisionsquelle live ausgeschlossen (Einzelläufe mit Workspace=Firecrawl liefen sauber).
- **Quelle:** eigener Vorfall + Live-Reproduktion 2026-06-21 (`or-research.py`, 7-parallel-Test mit/ohne Retry) · verwandt: §9 (SSE-Keep-Alive), §35 (Server-Tools/web_search), §7/§8 (Tool-Calling)

### 42. `:online`-Modus leakt agentische FOLGE-Tool-Calls als JSON-Text (keine Antwort) ⭐
- **Symptom:** Ein `<modell>:online`-Request "gelingt" (HTTP 200, kostet, `finish_reason:stop`), aber `message.content` enthält KEINE Antwort — nur eine Einleitung + rohe Tool-Call-JSONs, je Zeile `{"name": "web_search", "input": {"query": "..."}}`. Tritt auch bei SAUBERER Parallelität auf (kein Last-Crash): beim Second-Brain-A/B-Test fiel so **1/10** `:online`-Läufe aus (Researcher 7, Memory-Stacks — ein Thema mit vielen Kandidaten/Unterfragen). Der naive Leak-Detektor (nur XML-Marker `<tool_call>`/`]<]minimax[>`) wertete den Lauf fälschlich als "ok".
- **Ursache:** `:online` nutzt das alte web-**Plugin** (genau 1 implizite Suche), NICHT das agentische `web_search`-Server-Tool. Will das Modell MEHRFACH nachsuchen, hat es im `:online`-Modus keinen Ausführungskanal → die geplanten Tool-Calls landen als Text im `content`. Begünstigt durch Themen, die viele Einzelsuchen "einladen" (Stack-/Tool-Vergleiche mit N Kandidaten).
- **Unterschied zu §41:** §41 = LAST/Parallelität zerschießt das `web_search`-SERVER-TOOL (leer/Timeout/Routing-Müll). §42 = ein EINZELNER `:online`-Lauf liefert keine Antwort, weil agentische Folge-Calls leaken. Anderer Mechanismus, anderes Leak-Format (JSON-`{"name":...}` statt XML-`<tool_call>`).
- **FIX:** (1) Detektor MUSS den JSON-Tool-Call-Shape erkennen, nicht nur XML-Marker: Regex `\{"name":\s*"\w+",\s*"(input|arguments)":` (umgesetzt in `or-online-test.py` + `or-research.py`, #47046). (2) Erkannten Leak als Fehler behandeln (Retry ODER Lauf verwerfen), nie still als Erfolg werten. (3) Für Themen mit vielen Unterfragen eher das agentische `web_search`-Server-Tool (`or-research.py`) ODER Firecrawl (`mm-research.py`, volle Seiten) nehmen — beide liefern die Mehrfachsuche/Tiefe, die `:online` strukturell nicht kann.
- **Quelle:** eigener Vorfall 2026-06-21 (Hostinger-Second-Brain A/B-Test, `:online` 10×-parallel; R7 leer durch JSON-Tool-Call-Leak, vom adversarischen Qualitätsvergleich aufgedeckt). Verwandt: §41 (Last-Leak), §35 (Server-Tools), Kurzcheck #13/#14.

### 36. OpenRouter hostet NIE lokale Modelle; LM Studio braucht Dummy-`api_key`
- **Ursache:** OR ist reiner Cloud-Aggregator. „Lokal + OR mischen" braucht Router-Schicht (claude-code-router/LiteLLM) oder base_url-Umschaltung. Das OpenAI-SDK verlangt einen nicht-leeren `api_key`, auch wenn LM Studio (`localhost:1234/v1`) keinen braucht.
- **FIX:** Router davor ODER nur `base_url`+`api_key` tauschen (gleicher Code); für LM Studio Dummy-Key (`"lm-studio"`). Ollama v0.14 bietet native Anthropic-API für Claude Code (`ANTHROPIC_BASE_URL=http://localhost:11434`).
- **Quelle:** https://lmstudio.ai/docs/developer/openai-compat · https://ollama.com/blog/claude · offiziell

### 37. Rate-Limits global pro Account; Management-Key ≠ Inference-Key
- **Symptom:** mehr Keys/Accounts beheben 429 nicht; 403 bei Analytics mit Inference-Key.
- **Ursache:** Limits werden global pro Account verwaltet. Management Keys können keine Inference, Inference-Keys keine Verwaltung/Analytics.
- **FIX:** Last über verschiedene Modelle streuen; korrekten Key-Typ nutzen; pro Key ein `limit` setzen.
- **Quelle:** https://openrouter.ai/docs/api/reference/limits · https://openrouter.ai/docs/guides/overview/auth/management-api-keys · offiziell

---

## I. OpenCode (SST) — spezifische Fallen (Franks Umgebung)

> Recherche 2026-06-18. OpenCode-Repo jetzt `anomalyco/opencode` (vormals `sst/opencode`, leitet weiter).
> Gegenstück: `best-practices/apis/openrouter-api.md` §16. GitHub-Issue-Details aus Such-Snippets — bei Bedarf am Issue verifizieren.

### 38. OpenCode-Anbindung: `opencode auth login` und `OPENROUTER_API_KEY`-Env existieren NICHT ⭐
- **Symptom:** Anleitung mit `opencode auth login` oder gesetztem `OPENROUTER_API_KEY` schlägt fehl / Key wird nicht gefunden.
- **Ursache:** Beide kommen in der OpenCode-Doku nicht vor. Login läuft über den TUI-Befehl `/connect`; ein Key per Env geht nur über die generische `{env:VAR}`-Syntax im Config-Feld `provider.openrouter.options.apiKey`.
- **FIX:** `/connect` → OpenRouter → Key einfügen (landet in `~/.local/share/opencode/auth.json`); prüfen mit `opencode auth list`. Modell-String `openrouter/<vendor>/<model>`.
- **Quelle:** https://openrouter.ai/docs/cookbook/coding-agents/opencode-integration · https://opencode.ai/docs/providers · offiziell

### 39. OpenCode + `:free`/`:`-Varianten: Crash bzw. „No endpoints found that support tool use" ⭐
- **Symptom:** (a) OpenCode bricht bei OpenRouter-Modell-IDs mit `:` ab (Issue #749, siehe auch §26c); (b) bei `:free`/umbenannten Modellen `AI_APICallError: No endpoints found that support tool use` (Issues #1050, #1002, #10594).
- **Ursache:** Viele freie/günstige Provider unterstützen kein Tool-Use, das OpenCode aber braucht; Parser-Problem mit `:` im Slug; veraltete/umbenannte Modell-IDs.
- **FIX:** `provider.require_parameters:true` (routet nur zu tool-fähigen Providern) ODER `provider.order`/`only` auf einen tool-fähigen Provider; in den OpenRouter-Settings „Ignored Providers" leeren / „Allowed Providers" prüfen. `:free` NICHT für produktive Agenten — zahlendes Modell nehmen.
- **Quelle:** github.com/anomalyco/opencode Issues #749/#1050/#1002/#10594 · extern (nicht abschließend verifiziert) + https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

### 40. OpenCode: stale Modell-Liste, `openrouter/auto` greift nicht in TUI, Anthropic-Caching via OR unsicher
- **Symptom:** in `/models` wählbares Modell schlägt beim Senden fehl (Modell-Liste nicht neu geladen, #10594); `model:"openrouter/auto"` wird in der TUI ignoriert (#15225); Anthropic-Prompt-Caching erscheint nicht in der Abrechnung (#1245, #5416).
- **Ursache:** Modell-Cache nicht aktualisiert; Config-Anwendung in TUI fehlerhaft; Caching-Weiterreichung an OpenRouter+Anthropic greift nicht zuverlässig.
- **FIX:** Modell neu fetchen / `/models`; bei `openrouter/auto`-Problemen explizites Modell setzen; Caching in der OpenRouter-Activity prüfen (`cached_tokens`), `setCacheKey:true` setzen; Reasoning-Effort als rohen `options.reasoning.effort` durchreichen, falls `reasoningEffort` ignoriert wird.
- **Stealth-/Alpha-Modell fehlt im Modell-Picker trotz vorhandenem models.dev-Eintrag** (z.B. `openrouter/owl-alpha`, 2026-04-28, gratis, 1M Kontext, `tool_call:true`) ⭐ — **verifiziert 2026-06-18 in Franks Umgebung**. **Symptom:** `/models` bzw. `opencode models` zeigt das Modell nicht, auch `OWL`-Tippen im Switch-Model-Dialog findet nichts. **Root Cause (empirisch bewiesen):** models.dev KENNT das Modell (im Cache `~/.cache/opencode/models.json` unter `openrouter.models["openrouter/owl-alpha"]`), aber es trägt `status:"alpha"`. **OpenCode blendet `status:"alpha"`/`"deprecated"` aus dem Picker aus.** Ein Eintrag unter `provider.openrouter.models.<slug>` reicht NICHT — er wird mit dem models.dev-Eintrag GEMERGT (gleicher Provider-Name) und erbt `status:"alpha"` → bleibt unsichtbar (Cache-Patch `status` entfernen macht es sofort sichtbar = Beweis, aber nicht nachhaltig, Cache wird neu geladen). **FIX (nachhaltig):** EIGENEN Provider-Namen verwenden, den models.dev NICHT kennt → kein Merge, kein Status-Filter:
  ```jsonc
  "provider": { "owl": {
    "npm": "@openrouter/ai-sdk-provider",
    "options": { "baseURL": "https://openrouter.ai/api/v1", "apiKey": "{env:OPENROUTER_API_KEY}" },
    "models": { "openrouter/owl-alpha": { "name": "Owl Alpha (1M)" } } } }
  ```
  Wählbar als `owl/openrouter/owl-alpha`. Key per `{env:OPENROUTER_API_KEY}` (Custom-Provider erbt NICHT die `/connect`-Auth des eingebauten `openrouter`-Providers — env-Var aus `auth.json` setzen). OpenCode neu starten. End-to-End mit `opencode run --model owl/openrouter/owl-alpha "..."` verifizieren. **Datenschutz:** Owl Alpha & viele Gratis-Stealth-Modelle loggen Prompts/Completions fürs Training → NICHT für sensible/private App-Daten. **Quelle:** https://openrouter.ai/openrouter/owl-alpha · https://opencode.ai/docs/providers · https://ai.sulat.com/how-to-add-hidden-models-to-opencode-without-creating-a-new-provider-c102783f3cae
- **Quelle:** github.com/anomalyco/opencode Issues #10594/#15225/#1245/#5416 · extern (nicht abschließend verifiziert)

---

## Fix-Status (Stand 2026-06-18)

Im Wesentlichen per Design / Plattform-Verhalten — keine „gefixten" Einträge. `:free`-Slugs und Provider-Set ändern sich laufend → dynamisch beziehen. §G/§H (2026-06-17) dokumentieren überwiegend Konfigurations- und Plattform-Verhalten der CLI-Harness-Anbindung und neuerer Features.

**Ehrlichkeits-Hinweis:** Mehrere GitHub-Issue-Details (Bug 4/8/12/23) stammen aus Suchsnippets, nicht aus Volltext-Threads — bei Bedarf am Issue verifizieren. Der LiteLLM-Malware-Hinweis (26b) und das 1M-Context-Passthrough sind extern/unsicher und vor produktivem Einsatz zu verifizieren.

---

## Pflicht-Checkliste vor OpenRouter-Integration

- [ ] `HTTP-Referer` + `X-Title` im Client-Konstruktor gesetzt?
- [ ] `provider.order` + `allow_fallbacks:false` (deterministisch) + `require_parameters:true`?
- [ ] `quantizations`-Allowlist gegen Garbage-Output?
- [ ] SSE: `:`-Kommentarzeilen übersprungen, `[DONE]` + Mid-Stream-`error` behandelt?
- [ ] Modell-Liste via `/api/v1/models` (nicht hardcoded), Fallback-`models`-Array?
- [ ] 402 ≠ 429 unterschieden, `error.metadata.provider_name` ausgewertet?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/openrouter-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).

| Bug-Abschnitt (hier) | Best-Practice-Abschnitt (`best-practices/apis/openrouter-api.md`) |
|---|---|
| A (Header/Attribution) | §1 |
| B (Routing/Fallbacks) | §2, §3, §4 |
| C (Tool Calling) | §7 |
| D (Streaming/SSE) | §6 |
| E/F (Credits/Limits/Modell-String) | §5, §8, §15 |
| G19–G23 (Claude Code) | §9 |
| G24–G26 (andere CLI-Agenten) | §10 |
| H27–H28 (Speed-Routing) | §11 |
| H29–H32 (Caching/Reasoning/Usage) | §12 |
| H33–H35 (neue Features) | §13 |
| H36 (Cloud vs. lokal) | §14 |
| H37 (Account/Keys/Limits) | §15 |
| I38–I40 (OpenCode-Fallen) | §16, §17, §18, §19 |


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [google-gemini-api](google-gemini-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
