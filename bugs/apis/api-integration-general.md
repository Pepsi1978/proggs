# Bekannte Bugs: API-Integration anbieterübergreifend (Client-seitig)

> PFLICHT-LESEN vor Arbeit an JEDER HTTP-/LLM-API-Integration (Rate-Limiting, Retries, SSE-Streaming,
> Timeouts, Connection-Pooling, Secret-Handling, Fehlerbehandlung). Stand: zuletzt recherchiert am
> 2026-06-08. Anbieterspezifika: siehe die jeweilige Datei in `bugs/apis/`.
> Zweite Seite: noch keine `best-practices-api-integration-general.md`.

## TL;DR — die 7 wichtigsten Regeln

1. **429-Backoff:** `Retry-After` BEIDE Formate parsen (Sekunden ODER HTTP-Datum) und respektieren; `wait = max(retry_after, exp_backoff)`; immer Jitter (gegen Thundering Herd). RPM ≠ TPM ≠ RPD.
2. **Retries nur für 429 + 5xx (+ Anthropic 529)** — NIE 400/401/403/422. Nicht-idempotente POSTs nur mit Idempotency-Key wiederholen.
3. **SSE korrekt parsen:** Zeilenpuffer (Events splitten über TCP-Pakete), `data:`-Präfix + Doppel-Newline, `[DONE]` abfangen, Mid-Stream-`error`-Event behandeln, Kommentarzeilen (`:`) überspringen.
4. **Timeouts getrennt:** Connect/Read/Total — lange Streams dürfen NICHT am Read-Timeout sterben.
5. **.NET `HttpClient` als Singleton/`IHttpClientFactory`** (nie `new` pro Request → Socket-Exhaustion); `PooledConnectionLifetime` gegen stale DNS.
6. **Secrets:** API-Key nie im Client (Mobile/Browser/Desktop) — BFF-Pattern; nie in Logs/Stacktraces/Telemetrie.
7. **Fehler-Body parsen** (nicht nur Statuscode) — anbieterspezifisches `error.type`/`.message`/`.code`.

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

---

## E — Connection-Pooling / Keep-Alive (.NET)

### E1. `HttpClient` in `using` pro Request → Socket-Exhaustion ⭐
- **Symptom:** `SocketException`/`Cannot assign requested address` unter Last; Ports in `TIME_WAIT`.
- **FIX:** `HttpClient` als Singleton/static ODER `IHttpClientFactory`. NICHT pro Request `new HttpClient()` + dispose.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines

### E2. Singleton-`HttpClient` cached DNS → veraltete IP
- **FIX:** `SocketsHttpHandler.PooledConnectionLifetime` (z. B. 2 min) — recycelt Verbindungen + löst DNS neu auf. `IHttpClientFactory` macht das automatisch.

---

## F — Secret-Handling

### F1. API-Key im Client (Mobile/Browser/Desktop) eingebettet = öffentlich ⭐
- **Ursache:** Frontend-Clients sind public; jedes eingebettete Secret ist extrahierbar.
- **FIX:** Backend-for-Frontend (BFF) — Key lebt nur serverseitig, Client ruft eigenes Backend, das den Drittanbieter aufruft. (Für Frank relevant: Android/Desktop-Apps NIE mit eingebettetem LLM-Key ausliefern — siehe auch `secrets-in-sk-folder`-Regel.)
- **Quelle:** https://blog.gitguardian.com/stop-leaking-api-keys-the-backend-for-frontend-bff-pattern-explained/

### F2. Key-Leak in Logs/Stacktraces/Telemetrie
- **FIX:** API-Nutzung loggen ohne das Secret — Authorization-Header/Key-Felder vor dem Logging redacten; Keys aus Secret-Store ins Runtime-Env, nie in CI-Logs.

---

## G — Fehlerbehandlung & Tool-Calls

### G1. Nur Statuscode geprüft, Fehler-Body ignoriert
- **Ursache:** Anbieter liefern strukturiertes Error-Schema (`error.type`/`.message`/`.code`).
- **FIX:** Fehler-Body parsen und `type`/`message` für Logging + Retry-Entscheidung nutzen.

### G2. `finish_reason: "length"` droppt Tool-Call still
- **Ursache:** Tool-Call-Chunks werden erst am Stream-Ende aggregiert; bei `finish_reason=="length"` wird die aggregierte Antwort nie emittiert.
- **FIX:** partiellen Tool-Call-State auch bei `length` ausgeben + als „truncated" markieren; `max_tokens` erhöhen / Tool-Schema kürzen.
- **Quelle:** https://github.com/google/adk-python/issues/4482

---

## Fix-Status (Stand 2026-06-08)

Anbieterübergreifende Engineering-Patterns — dauerhaft gültige Korrektheitsregeln, keine versionsabhängigen Bugs. Belegt durch maßgebliche Quellen (AWS Builders' Library, Stripe, MDN/RFC zu SSE, Microsoft Learn zu HttpClient).

---

## Pflicht-Checkliste vor JEDER API-Integration

- [ ] 429: `Retry-After` (beide Formate) respektiert, Backoff mit Jitter, RPM/TPM unterschieden?
- [ ] Retries nur 429+5xx(+529), POST mit Idempotency-Key?
- [ ] SSE: Zeilenpuffer, `data:`+Doppel-Newline, `[DONE]`, Mid-Stream-`error`, Kommentarzeilen?
- [ ] Read-Timeout für Streams separat/hoch (Connect/Read/Total getrennt)?
- [ ] .NET: `HttpClient` Singleton/`IHttpClientFactory` + `PooledConnectionLifetime`?
- [ ] Kein API-Key im Client (BFF), keine Secrets in Logs?
- [ ] Fehler-Body geparst (nicht nur Statuscode)?
