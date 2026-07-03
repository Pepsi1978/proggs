# Strava API (v3) — Best Practices

> **Zweite Seite der Medaille zum Bug-Almanach** [`bugs/apis/strava-api.md`](../../bugs/apis/strava-api.md):
> dort steht *was schiefgeht*, hier *wie man die Strava-Integration von vornherein robust baut*.
> **Stand:** 2026-07-03 (Firecrawl-Recherche, 9 Quellen: communityhub.strava.com mit offiziellen
> Antworten, developers.strava.com, forum.intervals.icu). Projekt: EntropieReductor
> (Polar-Trainings via Strava-Drittanbieter-Push).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neue/bestehende API-App betreiben | Seit 30.06.2026: App-Besitzer braucht regulaeres Strava-Abo (Standard-Tier, keine Extra-Gebuehr); Status auf strava.com/settings/api im Blick behalten | §1 |
| 2 | App wurde "Inactive" | Besitzer-Konto → Abo → MANUELL im API-Dashboard reaktivieren (nicht automatisch); Client-ID/Tokens bleiben | §1 |
| 3 | Fehlerbehandlung bauen | 403-Body IMMER lesen+loggen+pattern-matchen (Inactive vs. Scope) — Klartext-Hinweis fuer den Nutzer ableiten | §2 |
| 4 | OAuth-Verbindung | Gewaehrten Scope aus der Token-Antwort pruefen (Consent-Haekchen abwaehlbar → partial grants behandeln) | §2 |
| 5 | Nur eigene Daten (1 Athlet) | Single-Player-Mode reicht (Athlete Capacity 1); Self-Upgrade bis 10 Athleten ohne Review moeglich | §3 |
| 6 | Rate-Limits planen | 200/15 min + 2.000/Tag (Standard); Lese-Limit separat (X-ReadRateLimit-*); Tages-Reset Mitternacht UTC | §3 |

---

## 1. Abo-Pflicht & App-Lebenszyklus (seit 30.06.2026)

- **Policy:** Standard-Tier-API-Apps setzen ein aktives Strava-Abo des App-BESITZERS voraus
  (API-Agreement-Update 2026-06-01, wirksam 30.06.2026). Ohne Abo → App-Status "Inactive",
  alle Requests 403. `offiziell` (Community-Manager-Antwort + Settings-Banner)
- **Keine Extra-Gebuehr:** "The Strava subscription provides you access to the API — there is
  no additional fee." Die reguläre Mitgliedschaft reicht. `offiziell`
- **Reaktivierung:** IMMER manuell im API-Settings-Dashboard (strava.com/settings/api), mit dem
  Besitzer-Konto eingeloggt. Praxisfall: am selben Tag wieder online; Client-ID/Tokens/Athleten
  blieben erhalten. `offiziell/extern`
- **Bestands-Entwickler:** 3 Monate gratis (Code `d0a2074c43`, Stand 2026-06 — Gueltigkeit beim
  Einloesen pruefen). `extern` (forum.intervals.icu)
- **Betriebs-Empfehlung:** Abo-Ablauf des Besitzer-Kontos wie ein Infrastruktur-Ablaufdatum
  behandeln (Kalender-Reminder) — der Ausfall kommt sonst "aus dem Nichts".

## 2. Fehlerbehandlung & OAuth

- **403-Body ist die Diagnose:** `errorBody().string()` genau EINMAL lesen, loggen und auf die
  bekannten Muster matchen: `"code":"Inactive"` (Abo/App), `activity:read_permission` (Scope).
  Dem Nutzer einen Klartext-Hinweis geben statt stiller Leere. (Umgesetzt: EntropieReductor
  `StravaRepository.handleSyncFailure`, #47451.)
- **Partial Scope Grants behandeln:** Der Consent-Screen erlaubt Abwahl einzelner Scopes — das
  `scope`-Feld der Token-Antwort gegen die angeforderten Scopes pruefen und Fehlbestand sofort
  melden (sonst spaeter kryptische 403).
- **Token-Lifecycle:** Access-Token 6 h, Refresh-Token rotiert bei jedem Refresh — AuthState
  atomar persistieren.

## 3. Kapazitaet & Limits

- **Single-Player-Mode** (Athlete Capacity 1) fuer Eigenbedarfs-Apps; Self-Upgrade bis 10
  Athleten direkt im Dashboard (kein Review). Extended Tier nur fuer groessere Apps. `offiziell`
- **Limits Standard:** 200 Requests/15 min + 2.000/Tag gesamt; non-upload 100/15 min +
  1.000/Tag; Lese-Limit separat via `X-ReadRateLimit-*` ueberwachen; Tages-Reset 00:00 UTC.
- **Alternative ohne Strava-Abo** (nicht quellenbewertet, Stand 2026-07-03): Polar AccessLink
  API direkt anbinden — eigener Rechercheauftrag noetig, bevor man umbaut.
