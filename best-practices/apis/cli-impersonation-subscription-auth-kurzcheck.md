# CLI-Impersonation / Subscription-Auth Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Abo-OAuth in eigener App | Legalitaet/Anwendungsfall zuerst klaeren | §1 |
| 2 | OAuth in Headless/CI | Device-Code-Flow bzw. `claude setup-token` nutzen | §2 |
| 3 | Token-Refresh speichern | Immer mergen, refresh_token nie ueberschreiben | §3 |
| 4 | Tokens ablegen | Wie Passwort; Keyring > Klartext, nie committen | §4 |
| 5 | Header/Client-Identitaet | Header-Imitation ist keine Dauerstrategie | §5 |
| 6 | Mehrere Credentials gesetzt | Praezedenz beachten — sonst stiller 401 | §6 |
| 7 | 401/403 behandeln | 401 = Refresh; 403-Client-Block = umstellen | §7 |
| 8 | Abo-Limits respektieren | Cachen statt hammern; API-Key-Fallback einbauen | §8 |
