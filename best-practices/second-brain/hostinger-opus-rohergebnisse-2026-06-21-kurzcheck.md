# Hostinger Second-Brain — OPUS-Rohergebnisse 2026-06-21 Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## 0. Kurzcheck (Sofort-Empfehlung)

| Frage | Empfehlung | Begruendung |
|-------|-----------|-------------|
| Reverse Proxy? | **Caddy** als Default (Nginx wenn maximale Throughput-Optimierung/Erfahrung) | Caddy macht TLS + Renewal automatisch (1-2 Zeilen Config); Nginx braucht Certbot + Cron |
| TLS? | **HTTPS Pflicht, TLS 1.3** (MCP-Spec verlangt es fuer Remote) | Kein Klartext-HTTP ausser Localhost |
| MCP-Auth? | **OAuth 2.1 + PKCE (S256)** — seit MCP-Spec verbindlich | Token audience-bound (RFC 8707) gegen Replay |
| API-Auth? | API-Keys (einfach) **oder** OAuth/JWT (skalierbar) **oder mTLS** (Maschine-zu-Maschine) | Je nach Client-Typ |
| Ports offen? | **Nur 443** (+22 SSH gesperrt/Key-only). Alles andere `localhost` | App-Ports nie direkt ans Internet |
| Firewall? | **UFW** (deny incoming default) + **Fail2Ban** (Brute-Force-Bann) | Defense in Depth |
| Rate-Limiting? | Im Proxy (`limit_req` / Caddy `rate_limit`) **und** in der App | Application- + Network-Layer |
| Ports ganz zu? | Optional **Cloudflare Tunnel** / **Pangolin** (keine offenen Ports) | Aber: TLS-Termination bei CF, Trade-offs (siehe §7) |
