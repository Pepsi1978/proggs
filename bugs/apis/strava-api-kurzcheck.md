# Strava API (v3) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ HTTP 403 `{"resource":"Application","field":"Status","code":"Inactive"}` | Seit **30.06.2026** braucht der App-BESITZER ein Strava-Abo (Standard-Tier) — App im API-Dashboard MANUELL reaktivieren, regulaeres Abo reicht (keine Extra-API-Gebuehr); 3-Monats-Gratis-Code fuer Bestands-Entwickler pruefen | A1 |
| 2 | HTTP 403/4xx ohne verwertbare Meldung im Log | Fehler-Body IMMER loggen (`errorBody().string()` genau EINMAL) — ohne Body ist Strava-403 nicht diagnostizierbar | A2 |
| 3 | 403 mit `"field":"activity:read_permission"` | Consent-Haekchen beim Strava-Login sind ABWAEHLBAR — App trennen + neu verbinden, alle Haekchen setzen lassen | A3 |
| 4 | 429 statt 403 | Rate-Limit (200/15 min, 2.000/Tag Standard) — X-ReadRateLimit-Header getrennt vom Gesamt-Limit auswerten | A4 |
