# Strava API (v3) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neue/bestehende API-App betreiben | Seit 30.06.2026: App-Besitzer braucht regulaeres Strava-Abo (Standard-Tier, keine Extra-Gebuehr); Status auf strava.com/settings/api im Blick behalten | §1 |
| 2 | App wurde "Inactive" | Besitzer-Konto → Abo → MANUELL im API-Dashboard reaktivieren (nicht automatisch); Client-ID/Tokens bleiben | §1 |
| 3 | Fehlerbehandlung bauen | 403-Body IMMER lesen+loggen+pattern-matchen (Inactive vs. Scope) — Klartext-Hinweis fuer den Nutzer ableiten | §2 |
| 4 | OAuth-Verbindung | Gewaehrten Scope aus der Token-Antwort pruefen (Consent-Haekchen abwaehlbar → partial grants behandeln) | §2 |
| 5 | Nur eigene Daten (1 Athlet) | Single-Player-Mode reicht (Athlete Capacity 1); Self-Upgrade bis 10 Athleten ohne Review moeglich | §3 |
| 6 | Rate-Limits planen | 200/15 min + 2.000/Tag (Standard); Lese-Limit separat (X-ReadRateLimit-*); Tages-Reset Mitternacht UTC | §3 |
