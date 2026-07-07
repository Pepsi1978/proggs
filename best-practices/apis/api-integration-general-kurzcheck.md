# API-Integration anbieterübergreifend Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
| 6 | .NET Pooling | Singleton/`IHttpClientFactory` + PooledConnLifetime | §6 |
| 7 | Secrets/Logging | Secrets redacten; Fehler-Body parsen | §7 |
