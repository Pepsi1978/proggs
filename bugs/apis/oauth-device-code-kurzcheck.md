# OAuth 2.0 — Device-Code-Flow, PKCE & Token-Refresh (eigene Software/CLIs) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ KRITISCH: paralleler Token-Refresh | Single-Flight-Mutex pro Account, sonst Family-Revoke | §D1 |
| 2 | ⭐ `slow_down` beim Polling | `interval += 5` kumulativ, nie ignorieren | §A1 |
| 3 | ⭐ `authorization_pending` | KEIN Fehler — weiterpollen | §A2 |
| 4 | ⭐ `code_challenge` bauen | Base64url OHNE Padding, `S256` | §B1, §B4 |
| 5 | Refresh liefert `invalid_grant` | Tokens verwerfen + Re-Auth, kein Retry-Loop | §D5 |
| 6 | Neuen Refresh-Token nach Rotation | Atomar speichern, alten verwerfen | §D2 |
| 7 | `grant_type` setzen | Volle URN `urn:...:device_code` | §A4 |
| 8 | Loopback-Redirect / Token-Ablage | Dyn. Port + `state`; Tokens nur in OS-Keychain | §C1, §C3, §E1 |
| 9 | Device-Flow aktivieren | Nur fuer echte input-constrained Geraete (Phishing) | §A8 |
