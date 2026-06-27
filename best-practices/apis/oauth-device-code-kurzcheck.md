# OAuth 2.0 (Device-Code, PKCE, Token-Refresh) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
