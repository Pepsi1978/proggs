# VPS-Hosting für selbst-gehostete Memory-/Vektor-DB (Hostinger, Hetzner) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck

| # | Frage | Best Practice |
|---|-------|---------------|
| 1 | Produktklasse | **Nur VPS (KVM)** — root + Docker. Shared/Web-Hosting scheidet aus. |
| 2 | Anbieter | **Hetzner** für Netzwerk/CPU/EU-Datenschutz/flexible Abrechnung; **Hostinger** für einsteigerfreundlich + günstigen Einstieg (aber Verlängerungssprung beachten). |
| 3 | Größe | pgvector allein: 4 GB ok. **Lokale Ollama-Embeddings: 8–16 GB.** |
| 4 | Backup | Nie nur auf Hoster-Backup verlassen — eigenes `pg_dump`-Cron + Offsite. |
| 5 | Zugriff absichern | Dienst **nicht** öffentlich exponieren → WireGuard-Split-Tunnel (siehe `server/wireguard.md`). |
