# Bekannte Bugs & Fallen: Strava API (v3)

> **PFLICHT-LESEN vor Arbeit an der Strava-Integration** (EntropieReductor: `data/remote/strava/`,
> `StravaRepository`, OAuth via AppAuth). Entstanden aus dem realen Vorfall 2026-07-03
> ("Trainings kommen nicht mehr an") + Firecrawl-Recherche (Engine A, 9 Quellen).
> **Stand:** 2026-07-03.
> **Anker:** strava-api=v3  <!-- maschinenlesbar fuer check-version-anchor.py -->
> Quellen: communityhub.strava.com (offizielle Community-Manager-Antworten),
> developers.strava.com/docs (API-Agreement-Update 2026-06-01), forum.intervals.icu.
> Best-Practices-Gegenseite: [`best-practices/apis/strava-api.md`](../../best-practices/apis/strava-api.md).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`).
> Volltext bei JEDEM Fehler im Bereich (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ HTTP 403 `{"resource":"Application","field":"Status","code":"Inactive"}` | Seit **30.06.2026** braucht der App-BESITZER ein Strava-Abo (Standard-Tier) — App im API-Dashboard MANUELL reaktivieren, regulaeres Abo reicht (keine Extra-API-Gebuehr); 3-Monats-Gratis-Code fuer Bestands-Entwickler pruefen | A1 |
| 2 | HTTP 403/4xx ohne verwertbare Meldung im Log | Fehler-Body IMMER loggen (`errorBody().string()` genau EINMAL) — ohne Body ist Strava-403 nicht diagnostizierbar | A2 |
| 3 | 403 mit `"field":"activity:read_permission"` | Consent-Haekchen beim Strava-Login sind ABWAEHLBAR — App trennen + neu verbinden, alle Haekchen setzen lassen | A3 |
| 4 | 429 statt 403 | Rate-Limit (200/15 min, 2.000/Tag Standard) — X-ReadRateLimit-Header getrennt vom Gesamt-Limit auswerten | A4 |

---

## A) Zugriff & Autorisierung

### A1. Application Status "Inactive" — Abo-Pflicht seit 30.06.2026   ⭐ EIGENER VORFALL 2026-07-03
**Symptom:** Sync liefert ploetzlich `HTTP 403` auf ALLE Endpunkte; Fehler-Body:
`{"message":"Forbidden","errors":[{"resource":"Application","field":"Status","code":"Inactive"}]}`.
Token gueltig, Scopes korrekt, Code unveraendert.
**Ursache:** Strava verlangt seit **30.06.2026** fuer alle Standard-Tier-API-Anwendungen ein
aktives Strava-Abo des App-BESITZERS. Ohne Abo setzt Strava die App serverseitig auf "Inactive"
(Banner auf strava.com/settings/api: "Wir aktualisieren den API-Zugriff, sodass dieser nur noch
fuer Mitglieder verfuegbar ist").
**Versionen:** API v3, Policy ab 2026-06-30 (API-Agreement-Update 2026-06-01).
**FIX (funktionserhaltend):** (1) Mit dem Konto einloggen, dem die App gehoert. (2) Regulaeres
Strava-Abo abschliessen — **keine zusaetzliche API-Gebuehr** (offizielle FAQ: "The Strava
subscription provides you access to the API — there is no additional fee"). Bestands-Entwickler:
3-Monats-Gratis-Code `d0a2074c43` versuchen (Quelle: forum.intervals.icu, Stand 2026-06).
(3) App im API-Settings-Dashboard **manuell reaktivieren** (passiert NICHT automatisch;
offizielle Antwort Emily_A, Community Hub). Client-ID/Tokens/verbundene Athleten bleiben laut
Praxisfall erhalten ("Back online" am selben Tag). Kein Code-Eingriff noetig.
**Quelle:** communityhub.strava.com "Code 'Inactive' on all new requests" · developers.strava.com/docs/rate-limits

### A2. 403-Fehler-Body wird verworfen → Ursache unsichtbar   ⭐ EIGENER VORFALL 2026-07-03
**Symptom:** Log zeigt nur "HTTP 403" — Scope-Fehler, Inactive-App und andere Ursachen sind
nicht unterscheidbar; tagelanges Raten.
**Ursache:** `HttpException.message` enthaelt den Body nicht; `errorBody()` muss explizit
gelesen werden (genau EINMAL — zweiter `.string()`-Aufruf liefert leer, retrofit-Almanach S5).
**FIX (funktionserhaltend):** Im zentralen Fehler-Handler den Body loggen + auf bekannte Muster
matchen (`"code":"Inactive"` → Abo-Hinweis; `activity:read_permission` → Consent-Hinweis).
Umgesetzt in EntropieReductor `StravaRepository.handleSyncFailure` (#47451) — Ursache war damit
in Minuten bewiesen.
**Quelle:** eigener Vorfall #47451/#47452 · `bugs/apis/api-integration-general.md` §G1

### A3. Consent-Haekchen abwaehlbar → Token ohne `activity:read`
**Symptom:** 403 mit Body `{"resource":"AccessToken","field":"activity:read_permission","code":"missing"}`
direkt nach einer (Neu-)Verbindung.
**Ursache:** Der Strava-OAuth-Consent-Screen zeigt die angeforderten Scopes als ABWAEHLBARE
Checkboxen — der Nutzer kann "Aktivitaeten ansehen" abwaehlen, die App bekommt dann einen
Token ohne den Scope (Strava-Doku: Apps muessen "partial scope grants" behandeln).
**FIX (funktionserhaltend):** App trennen + neu verbinden, Haekchen gesetzt lassen; im Code den
gewaehrten Scope aus der Token-Antwort (`scope`-Feld) pruefen und bei Fehlbestand klar melden.
**Quelle:** developers.strava.com/docs/authentication (scope handling)

### A4. Zwei getrennte Rate-Limits (Gesamt + Lese)
**Symptom:** 429 obwohl das Gesamt-Limit (X-RateLimit-Usage) harmlos aussieht.
**Ursache:** Strava fuehrt ZWEI Limits mit eigenen Headern: `X-RateLimit-*` (gesamt) und
`X-ReadRateLimit-*` (nur Reads, oft niedriger). Standard-Tier: 200/15 min + 2.000/Tag gesamt,
100/15 min + 1.000/Tag non-upload. Tages-Limits resetten um Mitternacht UTC.
**FIX (funktionserhaltend):** Beide Header auswerten; bei erschoepftem TAGES-Limit bis
Mitternacht UTC pausieren statt 15-Min-Cooldown (umgesetzt in EntropieReductor 2026-05-23).
**Quelle:** developers.strava.com/docs/rate-limits

---

## Fix-Status

| Falle | Status |
|-------|--------|
| A1 Abo-Pflicht | per Design (Strava-Policy seit 2026-06-30) — kein "Fix" durch Strava zu erwarten |
| A2/A3/A4 | app-seitig umgesetzt in EntropieReductor (#47451, 2026-05-23) |
