# Bekannte Bugs: OpenRouter API (Integration)

> PFLICHT-LESEN vor Arbeit an einer OpenRouter-Integration (Aggregator/Gateway, EIN OpenAI-kompatibler
> Endpunkt für viele Anbieter). Stand: zuletzt recherchiert am 2026-06-08.
> Endpoint: `https://openrouter.ai/api/v1`. Zweite Seite: `best-practices/projekt-code/apis/best-practices-openrouter-api.md`.

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
- **Symptom:** `unexpected end of JSON input`/Stream-Abbruch.
- **Ursache:** OpenRouter sendet SSE-Kommentarzeilen (mit `:`) gegen Timeouts.
- **FIX:** per SSE-Spec alle `:`-Zeilen überspringen; `eventsource-parser`/OpenAI-SDK nutzen.
- **Quelle:** https://openrouter.ai/docs/api/reference/streaming

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

## Fix-Status (Stand 2026-06-08)

Im Wesentlichen per Design / Plattform-Verhalten — keine „gefixten" Einträge. `:free`-Slugs und Provider-Set ändern sich laufend → dynamisch beziehen.

**Ehrlichkeits-Hinweis:** Mehrere GitHub-Issue-Details (Bug 4/8/12) stammen aus Suchsnippets, nicht aus Volltext-Threads — bei Bedarf am Issue verifizieren.

---

## Pflicht-Checkliste vor OpenRouter-Integration

- [ ] `HTTP-Referer` + `X-Title` im Client-Konstruktor gesetzt?
- [ ] `provider.order` + `allow_fallbacks:false` (deterministisch) + `require_parameters:true`?
- [ ] `quantizations`-Allowlist gegen Garbage-Output?
- [ ] SSE: `:`-Kommentarzeilen übersprungen, `[DONE]` + Mid-Stream-`error` behandelt?
- [ ] Modell-Liste via `/api/v1/models` (nicht hardcoded), Fallback-`models`-Array?
- [ ] 402 ≠ 429 unterschieden, `error.metadata.provider_name` ausgewertet?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/projekt-code/apis/best-practices-openrouter-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


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
