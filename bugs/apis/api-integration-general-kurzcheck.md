# API-Integration anbieterübergreifend (Client-seitig) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
