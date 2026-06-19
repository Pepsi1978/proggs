# Best Practices: VPS-Hosting für selbst-gehostete Memory-/Vektor-DB (Hostinger, Hetzner)

> **Zweite Seite der Medaille** zum Almanach [`bugs/server/vps-hosting.md`](../../bugs/server/vps-hosting.md):
> den richtigen VPS für eine selbst-gehostete Memory-DB (Postgres+pgvector, supermemory, mem0) wählen und
> dimensionieren. Verwandt: [`best-practices/opencode/self-hosted-memory-server.md`](../opencode/self-hosted-memory-server.md),
> [`best-practices/server/wireguard.md`](wireguard.md). **Stand 2026-06-19.**

---

## ⚡ Kurzcheck

| # | Frage | Best Practice |
|---|-------|---------------|
| 1 | Produktklasse | **Nur VPS (KVM)** — root + Docker. Shared/Web-Hosting scheidet aus. |
| 2 | Anbieter | **Hetzner** für Netzwerk/CPU/EU-Datenschutz/flexible Abrechnung; **Hostinger** für einsteigerfreundlich + günstigen Einstieg (aber Verlängerungssprung beachten). |
| 3 | Größe | pgvector allein: 4 GB ok. **Lokale Ollama-Embeddings: 8–16 GB.** |
| 4 | Backup | Nie nur auf Hoster-Backup verlassen — eigenes `pg_dump`-Cron + Offsite. |
| 5 | Zugriff absichern | Dienst **nicht** öffentlich exponieren → WireGuard-Split-Tunnel (siehe `server/wireguard.md`). |

---

## 1. Produktwahl: nur VPS (KVM)
Für eine eigene DB/Dienst zwingend **VPS mit root + Docker**. Shared/Web-Hosting kann das prinzipiell nicht.
Hostinger: nur die KVM-VPS-Linie (root, Docker-Manager, PostgreSQL+pgvector 1-Klick). Hetzner: CX/CCX-Cloud-VPS.

## 2. Anbieter-Wahl + Dimensionierung
| Kriterium | Empfehlung |
|-----------|-----------|
| Netzwerk-/CPU-Performance, EU-Datenschutz, stundengenaue Abrechnung | **Hetzner** (z.B. CX33: 4 vCPU/8 GB/80 GB ~6,49 €/Mon, DE/EU) |
| Einsteigerfreundlich, KI-Assistent, günstiger Einstiegspreis | **Hostinger KVM 2** (2 vCPU/8 GB/100 GB) — aber Gesamtkosten über Laufzeit rechnen (Verlängerungssprung!) |
| Reines pgvector (externe Embeddings) | kleinster VPS (4 GB) genügt |
| Lokale Ollama-Embeddings | **8 GB (KVM 2 / CX33)**, besser 16 GB; kleines Embedding-/LLM-Modell wählen |

**Faustregel:** Für Franks Memory-Server (supermemory Single-Binary + ggf. lokales Ollama für den LLM-Step) ist
ein **8-GB-VPS** der Sweet Spot — Hetzner CX33 bietet hier bestes Preis/Leistung + EU-Datenschutz; Hostinger KVM 2
ist die einsteigerfreundliche Alternative.

## 3. Betrieb, Backup, Sicherheit
- **Betrieb:** docker-compose `restart: unless-stopped` oder systemd (neustart-fest).
- **Backup:** eigenes `pg_dump -Fc` per Cron + Offsite (S3/Storage-Box) + getesteter Restore — Hoster-Backups (bei
  Hostinger nur wöchentlich gratis) reichen nicht. Bei supermemory: das `./.supermemory`-Verzeichnis sichern.
- **Sicherheit:** Dienst **nicht** öffentlich exponieren. Bevorzugt **WireGuard-Split-Tunnel** (nur deine Geräte
  erreichen den Dienst, kein öffentlicher Port außer UDP 51820) — siehe `best-practices/server/wireguard.md`.
  Alternativ Reverse-Proxy (Caddy TLS + Bearer-Token) + Cloud-Firewall (Docker-UFW-Falle beachten).

## 🔗 Bezugs-Tabelle
| Best-Practice | Bug-Abschnitt (`bugs/server/vps-hosting.md`) |
|---|---|
| §1 Produktwahl | §1 |
| §2 Anbieter/Dimensionierung | §2, §3, §5, §6 |
| §3 Backup/Sicherheit | §4 |

## Quellen
- hostinger.com/applications/postgresql · /compare/hostinger-vs-hetzner · vpsbenchmarks.com · A/B-Research 2026-06-19.
