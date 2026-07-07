# OAuth 2.0 (Device-Code, PKCE, Token-Refresh) — Best Practices (Stand 2026-07-02)

> Gegenstück zu `bugs/apis/oauth-device-code.md`. Offiziell: RFC 8252/8628/7636/9700/6749, oauth.net,
> Auth0/Okta/Google. (Researcher-Recherche 2026-06-08, Re-Recherche 2026-07-02.)
> Update 2026-07-02: Device-Flow bleibt protokollseitig unveraendert, ist aber operativ Hochrisiko fuer Phishing; nur fuer echte Geraete ohne Browser erlauben, sonst deaktivieren oder Conditional Access erzwingen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Flow waehlen | Browser → Code+PKCE; Headless → Device-Grant | §1 |
| 2 | PKCE bauen | S256, Base64url ohne Padding, Verifier 43–128 | §2 |
| 3 | Device-Polling | `pending`→weiter, `slow_down`→+5 s, dann stoppen | §3 |
| 4 | Token-Refresh | Single-Flight-Mutex, neuen Token sofort speichern | §4 |
| 5 | Tokens speichern | OS-Store (Keychain/DPAPI/Secret Service) | §5 |
| 6 | Loopback-Redirect | `127.0.0.1` + dyn. Port, `state` pruefen | §6 |
| 7 | OAuth 2.1 / RFC 9700 | PKCE ueberall, kein Implicit, Bearer nie in URL | §7 |

## 1. Flow-Wahl
- **Lokaler Browser → Authorization Code + PKCE** im System-Browser (nie embedded Webview!). **Headless/SSH/IoT → Device Authorization Grant** (RFC 8628). Quelle: RFC 8252 · https://datatracker.ietf.org/doc/html/rfc8252 · offiziell

## 2. PKCE korrekt
- Verifier aus CSPRNG (≥256 Bit → 43-Zeichen Base64url), Zeichensatz `[A-Za-z0-9-._~]` (43–128). `code_challenge=BASE64URL(SHA256(verifier))` OHNE Padding, `S256` (nie plain-Downgrade). Verifier an `state`/Session binden, bis Token-Request. Quelle: RFC 7636 · offiziell

## 3. Device-Code-Polling
- Mit `interval` pollen (Default 5 s); `authorization_pending`→weiter; `slow_down`→Intervall dauerhaft +5 s; Netzwerk-Timeout→exp. Backoff. Terminal: `access_denied`/`expired_token`→stoppen. `user_code` prominent + `verification_uri_complete` als QR. Quelle: RFC 8628 · offiziell

## 4. Token-Refresh-Design
- **Single-Flight-Mutex pro Account** (gegen Rotation-Race → sonst Token-Family-Revoke). Grace-Period nutzen (Okta default 30 s). Neuen Refresh-Token bei jeder Rotation SOFORT speichern (alten verwerfen). Proaktiv vor Ablauf erneuern (~80–90 % Lebensdauer). `invalid_grant`→Re-Auth (kein Loop). `offline_access`-Scope anfordern. Quelle: RFC 6749 §10.4 · https://developer.okta.com/docs/guides/refresh-tokens/main/ · https://github.com/modelcontextprotocol/typescript-sdk/issues/1760 · offiziell+extern

## 5. Sichere Token-Speicherung
- OS-nativer Store: macOS Keychain, Windows Credential Manager/DPAPI, Linux Secret Service. Wrapper: Python `keyring`, Node `keytar`, .NET DPAPI/MSAL-Cache. Etablierte Client-Libs: AppAuth, MSAL, google-auth (PKCE/Loopback/Refresh/Storage out-of-the-box). Linux-Fallback nur `chmod 600`. Quelle: https://developers.google.com/identity/protocols/oauth2/resources/best-practices · RFC 9700 · offiziell

## 6. Loopback-Redirect (RFC 8252)
- `127.0.0.1`/`[::1]` + dynamischer ephemerer Port (nicht `localhost`); `state` mit hoher Entropie + Pflicht-Prüfung; Port exklusiv binden, eine Antwort, dann Listener schließen. Quelle: RFC 8252 · offiziell

## 7. OAuth 2.1 / Security-BCP (RFC 9700) — heute Pflicht
- PKCE für ALLE Auth-Code-Flows; Exact-Match Redirect-URIs (außer Loopback-Port); KEIN Implicit/ROPC; Refresh-Token sender-constrained (mTLS/DPoP) ODER rotierend; Bearer NIE in URL-Query (nur Header); Authorization-Server-Metadata (RFC 8414) auswerten; Scope-Minimierung + Incremental Authorization. Quelle: https://datatracker.ietf.org/doc/html/rfc9700 · https://oauth.net/2.1/ · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/oauth-device-code.md`) |
|---|---|
| 3 Device-Polling | A1–A7 |
| 2 PKCE | B1–B5 |
| 6 Loopback | C1–C4 |
| 4 Refresh | D1–D5 |
| 5 Speicherung | E1–E2 |
| 7 OAuth 2.1 | F1–F5 |
