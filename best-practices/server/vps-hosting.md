# Best Practices: VPS-Hosting für selbst-gehostete Memory-/Vektor-DB (Hostinger, Hetzner)

> **Zweite Seite der Medaille** zum Almanach [`bugs/server/vps-hosting.md`](../../bugs/server/vps-hosting.md):
> den richtigen VPS für eine selbst-gehostete Memory-DB (Postgres+pgvector, supermemory, mem0) wählen und
> dimensionieren. Verwandt: [`best-practices/opencode/self-hosted-memory-server.md`](../opencode/self-hosted-memory-server.md),
> [`best-practices/server/wireguard.md`](wireguard.md). **Stand 2026-06-19, Sicherheits-Routine §7 am 2026-10-28.**

---

## ⚡ Kurzcheck

| # | Frage | Best Practice |
|---|-------|---------------|
| 1 | Produktklasse | **Nur VPS (KVM)** — root + Docker. Shared/Web-Hosting scheidet aus. |
| 2 | Anbieter | **Hetzner** für Netzwerk/CPU/EU-Datenschutz/flexible Abrechnung; **Hostinger** für einsteigerfreundlich + günstigen Einstieg (aber Verlängerungssprung beachten). |
| 3 | Größe | pgvector allein: 4 GB ok. **Lokale Ollama-Embeddings: 8–16 GB.** |
| 4 | Backup | Nie nur auf Hoster-Backup verlassen — eigenes `pg_dump`-Cron + Offsite. |
| 5 | Zugriff absichern | Dienst **nicht** öffentlich exponieren → WireGuard-Split-Tunnel (siehe `server/wireguard.md`). |
| 6 | Sicherheits-Routine | Patchstand, öffentliche Kante, Caddy-Go-Version, interne DB-Auth, Image-Pins, Ausgabenlimits: §7. |

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

---

## §7 Sicherheits-Routine für den Docker-VPS (Stand 28.10.2026)

Ergebnis des Audits vom 28.10.2026 (Gegenseite: `bugs/server/vps-hosting.md` §7). `offiziell` = Hersteller/Ubuntu/CISA, `extern` = Fachpresse.

1. **Patchstand maschinell prüfen** (offiziell): Ubuntu-CVE-JSON `https://ubuntu.com/security/cves/CVE-XXXX.json`, Notices `https://ubuntu.com/security/notices.json?release=noble`, CISA-KEV-Feed `https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json`. Ein Kernel-Fix gilt erst nach dem Reboot.
2. **Öffentliche Kante minimal** (offiziell): nur 443 (Caddy), 22 (nur Schlüssel) und 51820/udp offen; alles andere an 10.8.0.1/127.0.0.1 binden, weil veröffentlichte Docker-Ports UFW umgehen. https://docs.docker.com/engine/network/packet-filtering-firewalls/
3. **Caddy** (offiziell): Patch-Version pinnen, Go-Version mit `caddy build-info` prüfen, `admin off`, `header_down -Server`, bei offenen Go-HTTP/2-Lücken `protocols h1` (h3 nur mit veröffentlichtem UDP 443). Rate-Limit nur per xcaddy-Modul `mholt/caddy-ratelimit`. https://caddyserver.com/docs/caddyfile/options
4. **Interne Datenbanken trotzdem mit Auth** (offiziell): Redis `requirepass`/ACL (`-@scripting -restore -debug -config`), Qdrant-API-Key `QDRANT__SERVICE__API_KEY`. https://qdrant.tech/documentation/security/
5. **Images**: kein `:latest` von Drittanbietern; Patch-Version pinnen, Projektstatus (archiviert?) prüfen, wichtige Images per `docker save` sichern. Rolling-Tags monatlich pro Dienst `pull` + `up -d`.
6. **SSH/fail2ban** (offiziell): `PasswordAuthentication no`, `X11Forwarding no` per `/etc/ssh/sshd_config.d/00-haertung.conf` (der erste Wert gewinnt), `sshd -t` vor dem Reload; fail2ban `ignoreip` mit dem VPN-Netz.
7. **API-Schlüssel-Diebstahl** (extern, SANS ISC 07/2026): Scanner suchen `/mcp`, `/sse`, `/.env`, `/.claude/.credentials.json`, `/v1/models`, `/api/tags`. Relay: Token-Pflicht je Route (constant-time), unbekannte Pfade 404. Wichtigster Schutz: **harte Ausgabenlimits** pro Projekt bei OpenAI/Anthropic; Google-Keys auf die nötigen APIs beschränken. https://isc.sans.edu/diary/33150 · https://developers.openai.com/api/docs/guides/spend-limits.md
8. **Secret-Scanning**: GitHub Push Protection blockiert Anthropic-Keys seit 13.10.2026 standardmäßig; zusätzlich gitleaks als pre-commit und einmal über die Historie. https://github.blog/changelog/2026-10-13-secret-scanning-blocks-anthropic-keys-by-default-adds-salesforce
9. **WireGuard**: AllowedIPs je Peer /32, eigener PresharedKey je Peer, nie benutzte Peers prüfen und entfernen. https://manpages.ubuntu.com/manpages/noble/man8/wg.8.html
