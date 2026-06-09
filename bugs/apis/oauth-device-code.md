# Bekannte Bugs: OAuth 2.0 — Device-Code-Flow, PKCE & Token-Refresh (eigene Software/CLIs)

> PFLICHT-LESEN vor dem Einbau von OAuth-Login in eigene Software/CLIs (Schwerpunkt Device
> Authorization Grant RFC 8628 + PKCE + Token-Refresh). Stand: zuletzt recherchiert am 2026-06-08.
> Verwandt: das „als offizielle CLI ausgeben"-Muster siehe `cli-impersonation-subscription-auth.md`.
> Zweite Seite: `best-practices/projekt-code/apis/best-practices-oauth-device-code.md`.

## TL;DR — die 6 wichtigsten Regeln

1. **`slow_down` → `interval += 5` (kumulativ)**, niemals ignorieren. `authorization_pending` ist KEIN Fehler — weiterpollen.
2. **`grant_type` = volle URN** `urn:ietf:params:oauth:grant-type:device_code` (nicht `device_code`).
3. **PKCE:** `code_challenge = BASE64URL(SHA256(verifier))` **ohne Padding**, `S256`, Verifier 43–128 Zeichen, zwischen Auth- und Token-Request persistieren.
4. **Refresh = Single-Flight-Mutex pro Account** — sonst Token-Family-Revoke bei paralleler Rotation. Neuen Refresh-Token speichern. `invalid_grant` = Re-Auth, kein blindes Retry.
5. **Loopback:** dynamischer Port + `state` (CSRF) + exaktes Redirect-Matching; `SO_EXCLUSIVEADDRUSE` (Windows).
6. **Tokens nur in OS-Keychain/DPAPI/Secret-Service** — nie Klartext/JSON, nie in URL/Logs. `offline_access` für Refresh-Token.

---

## A. Device Authorization Grant (RFC 8628)

### A1. `slow_down` ignoriert → Polling gedrosselt/gebannt ⭐
- **Ursache:** `slow_down` verlangt `interval += 5` (kumulativ, für ALLE folgenden Requests). Wer im alten Takt weiterpollt, sammelt weitere `slow_down`.
- **FIX:** bei jedem `slow_down` Intervall dauerhaft um 5 s erhöhen.
- **Quelle:** https://www.rfc-editor.org/rfc/rfc8628

### A2. `authorization_pending` als Fehler behandelt → Flow bricht ab ⭐
- **Ursache:** Normalzustand während des Wartens — kein Abbruch-Grund.
- **FIX:** bei `authorization_pending` weiterpollen; nur `access_denied`/`expired_token`/echte HTTP-Fehler sind terminal.

### A3. Polling startet ohne/zu kurzes `interval` → sofort `slow_down`
- **FIX:** vor jedem Poll mind. `interval` Sekunden warten (5 s Default wenn nicht geliefert).

### A4. Falscher `grant_type` → `unsupported_grant_type`
- **FIX:** volle URN `urn:ietf:params:oauth:grant-type:device_code` + `device_code` + `client_id`.

### A5. `expired_token` nicht abgefangen → Endlos-Polling
- **FIX:** lokalen `expires_in`-Timer führen; bei `expired_token` Flow beenden + neuen Device-Code anfordern.

### A6. Netzwerk-Timeout ≠ `slow_down` behandeln
- **FIX:** bei aufeinanderfolgenden Netzwerkfehlern Intervall verdoppeln (eigener Backoff, getrennt von `slow_down`).

### A7. `user_code` Case/Format falsch angezeigt → Eingabe scheitert
- **FIX:** Code exakt wie vom Server anzeigen; wenn vorhanden `verification_uri_complete` (Code in URL/QR) anbieten.

### A8. Device-Code-Phishing (Social Engineering)
- **Symptom:** Angreifer startet selbst einen Device-Flow, schickt Opfer echten `user_code`+`verification_uri`; Opfer autorisiert → Angreifer erhält Tokens. (APT29 gegen 340+ M365-Orgs.)
- **Ursache:** läuft auf legitimer IdP-Infrastruktur — URL-Inspektion greift nicht.
- **FIX:** Device-Flow NUR für echte input-constrained Geräte aktivieren; sonst deaktivieren/per Conditional Access einschränken; kurze Code-Lebensdauer; deutliche Consent-Anzeige (App + Scopes).
- **Quelle:** https://labs.cloudsecurityalliance.org/research/csa-research-note-oauth-device-code-phishing-m365-20260325-c/

---

## B. PKCE (RFC 7636)

### B1. Base64url-Encoding-Bug bei `code_challenge` → stilles `invalid_grant` ⭐
- **Ursache:** Standard-Base64 (`+`/`/`/`=`) statt Base64url (`-`/`_`, kein Padding); Server-Hash-Vergleich divergiert lautlos.
- **FIX:** Base64url OHNE Padding (`+`→`-`, `/`→`_`, `=` entfernen).
- **Quelle:** https://www.rfc-editor.org/rfc/rfc7636

### B2. `code_verifier` außerhalb 43–128 Zeichen → `invalid_grant`
- **FIX:** kryptografisch zufälligen Verifier, Zeichensatz `[A-Za-z0-9-._~]`, Länge 43–128 (z. B. 32 Random-Bytes → Base64url = 43).

### B3. `code_verifier` nicht persistiert zwischen Auth- und Token-Request
- **FIX:** Verifier sicher zwischenspeichern (an `state`/Session gebunden) bis Token-Request fertig.

### B4. `plain` statt `S256` / Downgrade
- **FIX:** immer `code_challenge_method=S256`.

### B5. `code_challenge_method` fehlt/falsch → Server fällt auf `plain` zurück
- **FIX:** `code_challenge_method=S256` explizit (exakte Schreibweise).

---

## C. Authorization Code Flow für Desktop/CLI (RFC 8252)

### C1. `redirect_uri`-Mismatch → `redirect_uri_mismatch`
- **Ursache:** OAuth 2.1 verlangt exaktes Matching; dynamischer Loopback-Port kollidiert mit starrer URI.
- **FIX:** Loopback-Sonderregel — bei `127.0.0.1`/`localhost` darf der Port variieren; Pfad exakt registrieren.
- **Quelle:** https://datatracker.ietf.org/doc/html/rfc8252

### C2. `state`-Parameter fehlt → CSRF / Code-Injection
- **FIX:** zufälligen `state` erzeugen, im Callback exakt prüfen.

### C3. Loopback-Port-Race / fremde App bindet Port
- **FIX:** `SO_EXCLUSIVEADDRUSE` (Windows), dynamischer freier Port, genau diesen in `redirect_uri`. `http` für Loopback OK.

### C4. PKCE bei Public Clients weggelassen
- **Ursache:** Native/CLI-Clients haben kein sicheres Secret — Code allein abfangbar.
- **FIX:** PKCE (S256) ist Pflicht (RFC 8252 / OAuth 2.1).

---

## D. Token-Refresh & Rotation

### D1. Refresh-Token-Rotation-Race → ganze Token-Family revoked ⭐ KRITISCH
- **Symptom:** parallele Prozesse refreshen gleichzeitig mit demselben Token; der erste gewinnt, die anderen → `invalid_grant`, Reuse-Detection widerruft die GESAMTE Family. Verbindung kaputt bis manuelle Neu-Autorisierung. (Genau so in claude-code #43392 / MCP-SDK #1760.)
- **Ursache:** rotierende Refresh-Tokens invalidieren den alten sofort; der zweite Refresher sieht aus wie Replay.
- **FIX:** **Single-Flight pro Account** — Mutex/Lock, nur EIN Refresh gleichzeitig; parallele Aufrufer warten und bekommen dasselbe Ergebnis. „Wenn dein Refresh-Pfad nicht single-flight pro Account ist, hast du kein Refresh-System."
- **Quelle:** https://nango.dev/blog/concurrency-with-oauth-token-refreshes/ · https://github.com/anthropics/claude-code/issues/43392

### D2. Neuen Refresh-Token nach Rotation nicht gespeichert
- **FIX:** nach jedem Refresh den (ggf. neuen) Refresh-Token atomar speichern, bevor er verwendet wird.

### D3. Clock-Skew → Token „abgelaufen"/„noch nicht gültig"
- **FIX:** Token proaktiv VOR Ablauf erneuern (30–60 s Puffer); bei JWT-Validierung kleine Leeway (~60 s).

### D4. Kein `offline_access`-Scope → gar kein Refresh-Token
- **FIX:** `offline_access` (bzw. Google `access_type=offline` + `prompt=consent`) anfordern.

### D5. Refresh-Token-Widerruf nicht behandelt → Crash-Loop
- **FIX:** bei `invalid_grant` im Refresh lokale Tokens verwerfen + frischen Login-Flow auslösen — kein blindes Retry.

---

## E. Sichere Token-Speicherung (Cross-Platform)

### E1. Tokens im Klartext / JSON-Datei abgelegt ⭐
- **FIX:** OS-native Secure-Storage — macOS **Keychain**, Windows **Credential Manager / DPAPI**, Linux **Secret Service (libsecret)**.
- **Quelle:** https://github.com/git-ecosystem/git-credential-manager/blob/main/docs/credstores.md

### E2. DPAPI-Falle: an User/Maschine gebunden
- **FIX:** Scope bewusst (CurrentUser); Tokens als „nicht migrierbar" behandeln, bei Fehlschlag Re-Auth.

---

## F. Allgemeine OAuth-Fallen

### F1. Implicit Flow / Token im URL-Fragment (veraltet)
- **FIX:** immer Authorization Code Flow MIT PKCE; Tokens nie in Query/Fragment.

### F2. Token in Logs / URLs geleakt
- **FIX:** OAuth 2.1 verbietet Tokens in Query-Strings — nur `Authorization`-Header/POST-Body; Tokens vor dem Logging redacten.

### F3. Zu breite Scopes / falsche Audience
- **FIX:** Least Privilege; `audience`/`resource` passend zur Ziel-API setzen.

### F4. Fehlende Token-Validierung (JWT)
- **FIX:** Signatur gegen JWKS, `iss`/`aud`/`exp` validieren, `alg=none` ablehnen, Clock-Skew-Leeway.

### F5. `redirect_uri` mit Wildcard/Open-Redirect
- **FIX:** exaktes Redirect-Matching (OAuth 2.1), keine Wildcards; bei Loopback nur Port variabel.

---

## Fix-Status (Stand 2026-06-08)

Standards-basiert (RFC 8628/7636/8252) — die Einträge sind dauerhaft gültige Korrektheits-/Sicherheitsregeln, keine versionsabhängigen Bugs. Der Refresh-Race (D1) ist durch echte Issues (claude-code #43392, MCP-SDK #1760) + Anbieter-Doku (Auth0/Okta/Nango) belegt. Trend: OAuth 2.1 macht PKCE verbindlich und entfernt Implicit Flow.

---

## Pflicht-Checkliste vor OAuth-Einbau

- [ ] Device-Flow: `slow_down`→`interval+=5`, `authorization_pending` weiterpollen, volle URN, `expired_token` terminal?
- [ ] PKCE: S256, Base64url ohne Padding, Verifier 43–128, persistiert?
- [ ] Loopback: dynamischer Port + `state` + exaktes Matching + `SO_EXCLUSIVEADDRUSE`?
- [ ] Refresh: Single-Flight-Mutex, neuen Token speichern, `invalid_grant`=Re-Auth, `offline_access`?
- [ ] Tokens in OS-Keychain/DPAPI/Secret-Service (nicht Klartext), nie in URL/Logs?
- [ ] Device-Flow nur für echte input-constrained Geräte (Phishing-Risiko)?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/projekt-code/apis/best-practices-oauth-device-code.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).
