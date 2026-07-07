# API-Integration anbieterübergreifend — Best Practices (Stand 2026-07-02)

> Gegenstück zu `bugs/apis/api-integration-general.md`. Wie macht man Resilienz/Rate-Limiting/SSE/
> Timeouts/Pooling/Secrets von vornherein richtig. Quellen: AWS Builders' Library, Google SRE Book,
> Stripe, Microsoft Learn, IETF, Polly. (Researcher-Recherche 2026-06-08; Re-Recherche 2026-07-02.)

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Rate-Limiting | Token-Bucket, proaktiv per `RateLimit-Remaining` | §1 |
| 2 | Retry-Strategie | Capped Backoff+Jitter, Retry-Budget, nur 429/5xx | §2 |
| 3 | Idempotenz | Idempotency-Key pro Operation; nur idempotent retrien | §3 |
| 4 | Timeouts/Resilienz | Connect/Read/Total getrennt; Circuit Breaker | §4 |
| 5 | SSE robust | Zeilenpuffer, `[DONE]`, Proxy-Buffering aus | §5 |
| 6 | Pooling/Keepalive | .NET Pooling; Python/httpx bei NAT mit TCP-Keepalive | §6 |
| 7 | Secrets/Logging | Secrets redacten; Fehler-Body parsen | §7 |

## 1. Client-seitiges Rate-Limiting
- **Token-/Leaky-Bucket clientseitig**, proaktiv via `RateLimit-Remaining`-Header drosseln (z. B. <10 % Restbudget) BEVOR 429 kommt; adaptive Throttling (Google SRE „Handling Overload"). Quelle: https://sre.google/sre-book/handling-overload/ · offiziell
- `x-ratelimit-*` (RPM/TPM/RPD) getrennt beobachten, knappste Dimension drosseln. Quelle: https://http.dev/x-ratelimit-remaining · extern

## 2. Retry-Strategie
- **Capped exponential backoff + Jitter** (full/equal/decorrelated). Quelle: https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/ · offiziell
- **Retry-Budget** (Token-Bucket, max ~3, <10 % der Requests dürfen Retries sein); Retries nur auf EINER Ebene (kein Stacking). Quelle: AWS Builders' Library · offiziell
- Retry nur 429/5xx (+ Anthropic 529), NIE 4xx (außer 429). `Retry-After` BEIDE Formate (Sekunden ODER HTTP-Datum) parsen + respektieren (`wait=max(retry_after, backoff)`). Quelle: https://docs.stripe.com/rate-limits · offiziell

## 3. Idempotency
- **Idempotency-Keys** pro logischer Operation (Stripe-Pattern, 24 h); nur idempotente Ops retrien; bei 429-Validierungsfehler darf neuer Key nötig sein. Quelle: https://stripe.com/blog/idempotency · offiziell

## 4. Circuit Breaker / Hedging / Timeouts
- **Getrennte Connect/Read/Total-Timeouts** aus p99.9 ableiten; lange Streams NICHT am Read-Timeout sterben lassen (separater hoher/deaktivierter Read-Timeout für SSE).
- Circuit Breaker + Hedging gegen kaskadierende Ausfälle (Polly v8). Quelle: https://www.pollydocs.org/strategies/circuit-breaker.html · extern; https://learn.microsoft.com/en-us/dotnet/core/resilience/http-resilience · offiziell

## 5. SSE-Streaming robust
- **Zeilenpuffer** (Events splitten über TCP-Pakete), `data:`-Präfix + Doppel-Newline als Event-Grenze, `[DONE]` abfangen, Mid-Stream-`error`-Event behandeln. Nach `[DONE]` den HTTP-Transport sauber drainen, wenn SDK/Proxy sonst Chunked-Verbindungen zerstört; fehlendes `event:`-Feld defensiv aus `data` ableiten.
- Proxy: `X-Accel-Buffering: no`, `Content-Type: text/event-stream`, nach jedem Event flushen; Heartbeat-Kommentar (`: keepalive`) alle 5–15 s; Proxy-Idle-Timeouts hoch. Quelle: https://oneuptime.com/blog/post/2025-12-16-server-sent-events-nginx/view · extern

## 6. Connection-Pooling / Keepalive
- `HttpClient` als Singleton/`IHttpClientFactory` (nie `new` pro Request → Socket-Exhaustion); `SocketsHttpHandler.PooledConnectionLifetime` 2–15 min (DNS-Refresh); `HandlerLifetime` ggf. Infinite + PooledConnectionLifetime. Quelle: https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines · offiziell
- Bei langen Python/httpx-Non-Streaming-Calls hinter NAT eigenen Transport mit TCP-Keepalive setzen (`SO_KEEPALIVE`, Idle/Interval/Count), sonst können idle Verbindungen still gedroppt werden. Quelle: https://github.com/openai/openai-python/issues/3269 · extern

## 7. Observability & Secrets
- Loggen: Request-IDs, Rate-Limit-Header, Latenz-Perzentile — NIE Secrets (Authorization-Header redacten). Fehler-BODY parsen (nicht nur Statuscode): anbieterspezifisches `error.type/.message/.code`.
- Secrets nicht dauerhaft in Client-Code oder ungeprüft in Environment-Variablen lagern; CWE-526/OWASP werten Env-Secret-Leaks als reale Schwachstelle. Secret-Store/SK-Ordner und Runtime-Injection bevorzugen.
- `finish_reason:"length"` kann Tool-Call still droppen → partiellen State ausgeben + als truncated markieren. Quelle: https://github.com/google/adk-python/issues/4482 · extern

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/api-integration-general.md`) |
|---|---|
| 1–2 Rate-Limiting/Retry | A1–A8, B1–B3 |
| 5 SSE | C1–C10 |
| 4 Timeouts | D1–D4 |
| 6 Pooling/Keepalive | E1–E3 |
| 7 Secrets/Fehler | F1–F3, G1–G4 |
