# Bekannte Bugs: API-Integration anbieterübergreifend (Client-seitig)

> PFLICHT-LESEN vor Arbeit an JEDER HTTP-/LLM-API-Integration (Rate-Limiting, Retries, SSE-Streaming,
> Timeouts, Connection-Pooling, Secret-Handling, Fehlerbehandlung). Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax).
> Anbieterspezifika: siehe die jeweilige Datei in `bugs/apis/`.
> Zweite Seite: `best-practices/apis/api-integration-general.md`.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 429 / Backoff bauen | `Retry-After` beide Formate; Jitter immer | §A1, §A3 |
| 2 | Retry-Whitelist | Nur 429+5xx(+529), nie 4xx; POST=Idempotency-Key | §B1, §B2 |
| 3 | ⭐ SSE-Event ueber TCP-Pakete | Zeilenpuffer, erst bei Doppel-Newline parsen | §C4, §C1 |
| 4 | ⭐ `[DONE]`-Marker im Stream | Vor `JSON.parse` abfangen + Stream schliessen | §C2 |
| 5 | ⭐ Langer Stream stirbt (~5 min) | Read-Timeout separat/hoch, getrennt von Total | §D1 |
| 6 | ⭐ .NET HttpClient pro Request | Singleton/`IHttpClientFactory` + PooledConnLifetime | §E1, §E2 |
| 7 | ⭐ API-Key im Client (Mobile/Browser) | Nie einbetten — BFF-Pattern, Key serverseitig | §F1 |
| 8 | Fehler behandeln | Fehler-Body parsen, nicht nur Statuscode | §G1 |
| 9 | ⭐ OpenAI-Python-SDK hinter NAT hängt | Eigenen `http_client` mit TCP-Keepalive setzen | §E3 |
| 10 | ⭐ OpenAI-Python-SDK-Streaming + Proxy | Nach `[DONE]` Chunked-Terminator drainen, nicht sofort schließen | §C7 |
| 11 | ⭐ SSE ohne `event:`-Feld | Event-Typ defensiv aus `data` ableiten | §C9 |

---

## A — Rate-Limiting / 429

### A1. `Retry-After` als HTTP-Datum nicht geparst
- **Ursache:** `Retry-After` darf `120` (Sekunden) ODER ein HTTP-Datum sein.
- **FIX:** beide Formate parsen (erst Integer, sonst Datum→Delta), negatives Delta auf 0 clampen.
- **Quelle:** https://howhttpworks.com/headers/retry-after

### A2. Backoff ignoriert `Retry-After` komplett
- **FIX:** `wait = max(retry_after_value, calculated_backoff)` — nie früher als der Server vorgibt.

### A3. Thundering Herd bei parallelen Retries ⭐
- **Ursache:** Backoff ohne Jitter — alle retrien synchron.
- **FIX:** full jitter `sleep = random(0, min(cap, base*2^n))`.
- **Quelle:** https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/

### A4. RPM, TPM, RPD feuern gleichzeitig — falsches Limit beobachtet
- **FIX:** alle `x-ratelimit-*`-Header beobachten, das knappste Budget drosseln. (RPM bremst Chat, TPM lange Docs, RPD Free-Tier.)

### A5. `x-ratelimit-remaining` nicht ausgewertet (reaktiv statt proaktiv)
- **FIX:** Header lesen und clientseitig drosseln wenn `remaining` gegen 0 (proaktiver Token-Bucket).

### A6. Kein clientseitiger Token-Bucket → Burst-429
- **FIX:** Token-Bucket der RPM/TPM-Budget trackt; optional Batch-API (oft rate-limit-exempt) für Nicht-Echtzeit.

### A7. Anbieter-Limits ändern sich ohne Code-Fehler
- **Symptom:** Client drosselt zu stark oder zu schwach, obwohl die Integration selbst unverändert ist.
- **Ursache:** Provider ändern Rate-Limits laufend; Anthropic erhöhte am 06.05.2026 u. a. 5-Stunden- und Opus-API-Limits. GitHub dokumentiert parallel sekundäre Limits, die REST und GraphQL gemeinsam betreffen (100 concurrent Requests, Punktelimits pro Minute; Stand API-Version 2026-03-10).
- **FIX:** Limits nicht dauerhaft hardcoden; Doku-/Headerwerte regelmäßig abgleichen, `x-ratelimit-*` getrennt nach Dimension tracken und REST/GraphQL-Concurrency zusammen begrenzen.
- **Quellen:** https://www.anthropic.com/news · https://docs.github.com/rest/using-the-rest-api/rate-limits-for-the-rest-api · https://docs.github.com/en/graphql/overview/rate-limits-and-query-limits-for-the-graphql-api

### A8. Azure/OpenAI 429 mit `retry-after: 86400` fehlinterpretiert
- **Symptom:** Client wartet 24 Stunden, obwohl das eigentliche TPM/RPM-Fenster früher frei wird.
- **Ursache:** Azure-OpenAI-Antworten können `retry-after` auf 86.400 Sekunden deckeln; die echte Drosselung bleibt minutenbasiert.
- **FIX:** `Retry-After` lesen, aber zusätzlich das konkrete Limit-Bucket im Fehler-Body/Header auswerten und bei 86400 nicht blind einen Tag schlafen.
- **Quelle:** Microsoft Q&A zu Azure OpenAI 429 / `retry-after` cap.

---

## B — Retries & Idempotenz

### B1. 4xx (außer 429) wird retried → endlose Fehlversuche
- **FIX:** nur 429 + 5xx (500/502/503), Anthropic zusätzlich **529**, retrien; 400/401/403/422 NIE.

### B2. Nicht-idempotenter POST doppelt ausgeführt nach Retry ⭐
- **Symptom:** doppelte Bezahlung/Erstellung (Antwort verloren, Aktion war erfolgreich).
- **FIX:** Idempotency-Key pro logischer Operation (Stripe-Pattern); Server gibt bei gleichem Key das erste Ergebnis zurück.
- **Quelle:** https://docs.stripe.com/api/errors

### B3. 529/Overloaded nicht als retrybar erkannt
- **FIX:** 529 explizit in die Retry-Whitelist (Backoff+Jitter).

---

## C — SSE-Streaming

### C1. `data:`-Präfix oder Doppel-Newline fehlt → onmessage feuert nie
- **FIX:** auf `data:`-Zeilen + `\n\n` als Event-Grenze achten; beim Server-Proxy beides erhalten.

### C2. `[DONE]`-Marker nicht behandelt → Stream hängt / JSON-Crash ⭐
- **FIX:** vor `JSON.parse` auf `[DONE]` prüfen und Stream schließen.

### C3. Proxy-Buffering verzögert/blockt Stream
- **Ursache:** Reverse-Proxy puffert Response; fehlendes Flushing.
- **FIX:** Buffering deaktivieren (nginx `X-Accel-Buffering: no`), `Content-Type: text/event-stream`, nach jedem Event flushen.

### C4. Ein SSE-Event über zwei TCP-Pakete gesplittet → JSON.parse wirft ⭐
- **FIX:** Zeilenpuffer — unvollständigen Rest bis zum nächsten Chunk aufheben, erst bei kompletter Zeile/Doppel-Newline parsen.
- **Quelle:** https://www.aha.io/engineering/articles/streaming-ai-responses-incomplete-json

### C5. Mid-Stream `error`-Event verschluckt → stille Truncation
- **FIX:** `type === "error"` explizit behandeln und als Exception propagieren.

### C6. Partielles JSON pro Chunk geparst → Dauer-Crash
- **FIX:** Parse-State zwischen Chunks halten; oder Streaming-JSON-Repair-Lib (`partialjson`) statt Batch-Repair.

### C7. OpenAI-Python-SDK schließt nach `[DONE]`, bevor HTTP-Chunked sauber gedraint ist ⭐
- **Symptom:** Reverse-Proxies loggen `downstream_remote_disconnect`; Clients sehen gelegentlich `httpx/httpcore.RemoteProtocolError: incomplete chunked read`, Verbindungspool verliert Keep-Alive.
- **Ursache:** Regression in `openai-python`: nach dem SSE-`[DONE]`-Event wird geschlossen, bevor der HTTP/1.1-Chunked-Terminator (`0\r\n\r\n`) konsumiert wurde; in Issue #3440 bis 2.44.0 beschrieben.
- **FIX:** Stream nach `[DONE]` vollständig drainen, dann erst schließen; bis Upstream-Fix eigenen Wrapper oder gepinnte SDK-Version prüfen.
- **Quelle:** https://github.com/openai/openai-python/issues/3440

### C8. Mid-Stream-SSE-Error mit HTTP-200-Status falsch klassifiziert
- **Symptom:** Retry-Logik sieht `status_code=200`, obwohl im SSE-Body ein `overloaded_error`/`rate_limit_error` steht.
- **Ursache:** Anthropic-Python-SDK #1258 übergab beim SSE-`error`-Event die ursprüngliche HTTP-200-Response an die Fehlererzeugung.
- **FIX:** SDK ≥ 0.87.0 nutzen; eigene Stream-Parser müssen den SSE-Body auswerten, nicht nur den HTTP-Status.
- **Quelle:** https://github.com/anthropic/anthropic-sdk-python/issues/1258

### C9. SSE-Proxy liefert nur `data:`, kein `event:`-Feld ⭐
- **Symptom:** Stream wirkt leer oder endet mit `No generation chunks were returned`.
- **Ursache:** Einige Bedrock-/Adapter-Proxies senden JSON nur in `data:`-Zeilen; Parser, die zwingend `event:` erwarten, überspringen alles.
- **FIX:** Event-Typ defensiv aus dem `data`-JSON ableiten, wenn `event` fehlt; Adapter gezielt testen.
- **Quelle:** https://github.com/langchain-ai/langchain/issues/35442

### C10. Terminales `output: null` im Responses-Stream nicht abgefangen
- **Symptom:** `TypeError: 'NoneType' object is not iterable` am Stream-Ende.
- **Ursache:** OpenAI-Python-Responses-Parser erwartete iterierbares `output`, obwohl Backends terminal `null` liefern können.
- **FIX:** Terminale Nullfelder vor Iteration abfangen; SDK-Fixstatus pro Version prüfen.
- **Quelle:** OpenAI-Python Issues #3459/#3325/#3321/#3314/#3313/#3312.

---

## D — Timeouts

### D1. Read-Timeout killt lange Streams (~5 min) ⭐
- **Ursache:** `read_timeout` (Zeit zwischen zwei Bytes) trifft lange Event-Pausen; Default oft 5 min.
- **FIX:** für Streams separaten, hohen/deaktivierten Read-Timeout (NICHT Total-Timeout); Connect/Read/Total getrennt.
- **Quelle:** https://github.com/jlowin/fastmcp/issues/937

### D2. Inkonsistente Timeout-Typen (float Sekunden vs. timedelta)
- **FIX:** Timeout-Typen pro Client-Funktion prüfen/normalisieren.

### D3. Infrastruktur-Idle-Timeout schließt „stille" Streams
- **Ursache:** Envoy/Istio `stream_idle_timeout` sieht Stream als idle, weil der CLIENT nichts sendet.
- **FIX:** `stream_idle_timeout` für SSE-Routen erhöhen/deaktivieren bzw. Heartbeat-Kommentare (`: keepalive\n\n`).

### D4. Hohe `max_tokens` erzwingen plötzlich Streaming
- **Symptom:** Non-Streaming-Call wirft `Streaming is required for operations that may take longer than 10 minutes.`
- **Ursache:** Anthropic erzwingt bei sehr hohen Output-Budgets (z. B. ≥ ca. 21k Tokens in pydantic-ai #4491) einen Streaming-Pfad.
- **FIX:** Clients, die hohe `max_tokens` setzen, müssen SSE unterstützen oder automatisch auf Streaming fallbacken.
- **Quelle:** https://github.com/pydantic/pydantic-ai/issues/4491

---

## E — Connection-Pooling / Keep-Alive (.NET)

### E1. `HttpClient` in `using` pro Request → Socket-Exhaustion ⭐
- **Symptom:** `SocketException`/`Cannot assign requested address` unter Last; Ports in `TIME_WAIT`.
- **FIX:** `HttpClient` als Singleton/static ODER `IHttpClientFactory`. NICHT pro Request `new HttpClient()` + dispose.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines

### E2. Singleton-`HttpClient` cached DNS → veraltete IP
- **FIX:** `SocketsHttpHandler.PooledConnectionLifetime` (z. B. 2 min) — recycelt Verbindungen + löst DNS neu auf. `IHttpClientFactory` macht das automatisch.

### E3. OpenAI-Python-SDK ohne TCP-Keepalive hängt hinter NAT ⭐
- **Symptom:** Lange Non-Streaming-Calls hängen endlos, obwohl der Anbieter die Antwort fertig erzeugt hat.
- **Ursache:** Default-`httpx`-Transport setzt kein TCP-Keepalive; NAT-Gateways droppen idle Verbindungen nach 60-350 s, ohne dass Client oder Server es merken.
- **FIX:** Eigenen `http_client` mit Socket-Optionen (`SO_KEEPALIVE`, `TCP_KEEPIDLE`/macOS-Äquivalent, `TCP_KEEPINTVL`, `TCP_KEEPCNT`) übergeben; nicht einfach den Timeout verkürzen.
- **Quelle:** https://github.com/openai/openai-python/issues/3269

---

## F — Secret-Handling

### F1. API-Key im Client (Mobile/Browser/Desktop) eingebettet = öffentlich ⭐
- **Ursache:** Frontend-Clients sind public; jedes eingebettete Secret ist extrahierbar.
- **FIX:** Backend-for-Frontend (BFF) — Key lebt nur serverseitig, Client ruft eigenes Backend, das den Drittanbieter aufruft. (Für Frank relevant: Android/Desktop-Apps NIE mit eingebettetem LLM-Key ausliefern — siehe auch `secrets-in-sk-folder`-Regel.)
- **Quelle:** https://blog.gitguardian.com/stop-leaking-api-keys-the-backend-for-frontend-bff-pattern-explained/

### F2. Key-Leak in Logs/Stacktraces/Telemetrie
- **FIX:** API-Nutzung loggen ohne das Secret — Authorization-Header/Key-Felder vor dem Logging redacten; Keys aus Secret-Store ins Runtime-Env, nie in CI-Logs.

### F3. Secrets in Environment-Variablen sind ein anerkanntes Risiko
- **Ursache:** Env-Vars landen leicht in Prozesslisten, Dumps, Logs oder Child-Prozessen; CWE-526 und OWASP Secrets Management warnen explizit davor.
- **FIX:** Secrets bevorzugt aus Secret-Store/SK-Ordner bzw. Runtime-Injection lesen; Env nur als letzte, redaktierte Übergabeschicht nutzen.
- **Quellen:** https://cwe.mitre.org/data/definitions/526.html · https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html

---

## G — Fehlerbehandlung & Tool-Calls

### G1. Nur Statuscode geprüft, Fehler-Body ignoriert
- **Ursache:** Anbieter liefern strukturiertes Error-Schema (`error.type`/`.message`/`.code`).
- **FIX:** Fehler-Body parsen und `type`/`message` für Logging + Retry-Entscheidung nutzen.

### G2. `finish_reason: "length"` droppt Tool-Call still
- **Ursache:** Tool-Call-Chunks werden erst am Stream-Ende aggregiert; bei `finish_reason=="length"` wird die aggregierte Antwort nie emittiert.
- **FIX:** partiellen Tool-Call-State auch bei `length` ausgeben + als „truncated" markieren; `max_tokens` erhöhen / Tool-Schema kürzen.
- **Quelle:** https://github.com/google/adk-python/issues/4482

### G3. HTTP-200 mit Fehler-Body kommt in Production-APIs weiter vor
- **Symptom:** Client behandelt Antwort als Erfolg, obwohl der Body ein `error`-/`errors`-Objekt enthält.
- **Ursache:** Nicht alle Anbieter folgen sauber der 4xx/5xx-Semantik; einzelne SDKs mussten 200-Fehlerantworten nachträglich abfangen.
- **FIX:** Auch bei 2xx Response-Body auf Fehler-Schema prüfen, besonders bei LLM-/Proxy-/Gateway-Anbietern.
- **Quellen:** https://www.postman.com/api-platform/api-design/ · https://developer.mozilla.org/en-US/docs/Web/HTTP/Status

### G4. Go-HTTP-Client-Semantik nicht blind auf Retry-Libs übertragen
- **Symptom:** Code erwartet bei non-2xx keinen Fehler, bekommt nach ausgeschöpften Retries aber doch einen Error.
- **Ursache:** Go `http.Client` wirft bei non-2xx keinen Fehler; `go-retryablehttp` kann nach Retry-Erschöpfung abweichend reagieren.
- **FIX:** Semantik der Retry-Lib explizit prüfen; bei gewünschtem Standardverhalten `retryablehttp.PassthroughErrorHandler` setzen.
- **Quelle:** https://github.com/hashicorp/go-retryablehttp/issues/200

---

## Fix-Status (Stand 2026-07-02)

Anbieterübergreifende Engineering-Patterns bleiben dauerhaft gültig. Die Re-Recherche am 2026-07-02 fand keine veraltete Regel, aber konkrete SDK-/Provider-Fallen: OpenAI-Python #3269/#3440, Anthropic-Python #1258, LangChain #35442 und pydantic-ai #4491. Offene OpenAI-PR-/Issue-Fixstände müssen bei konkreter SDK-Version separat geprüft werden.

---

## Pflicht-Checkliste vor JEDER API-Integration

- [ ] 429: `Retry-After` (beide Formate) respektiert, Backoff mit Jitter, RPM/TPM unterschieden?
- [ ] Retries nur 429+5xx(+529), POST mit Idempotency-Key?
- [ ] SSE: Zeilenpuffer, `data:`+Doppel-Newline, `[DONE]`, Mid-Stream-`error`, Kommentarzeilen?
- [ ] Read-Timeout für Streams separat/hoch (Connect/Read/Total getrennt)?
- [ ] .NET: `HttpClient` Singleton/`IHttpClientFactory` + `PooledConnectionLifetime`?
- [ ] Python/OpenAI hinter NAT: eigener `http_client` mit TCP-Keepalive gesetzt?
- [ ] SSE-Parser: `[DONE]` behandelt UND Transport sauber gedraint; fehlendes `event:` toleriert?
- [ ] Kein API-Key im Client (BFF), keine Secrets in Logs?
- [ ] Fehler-Body geparst (nicht nur Statuscode)?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/api-integration-general.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [google-gemini-api](google-gemini-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
