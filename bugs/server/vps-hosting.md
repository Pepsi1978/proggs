# Bekannte Bugs & Fallen: VPS-Hosting für selbst-gehostete DB/Dienste (Hostinger, Hetzner)

> **PFLICHT-LESEN vor der Wahl eines VPS** für eine selbst-gehostete Memory-/Vektor-DB (Postgres+pgvector,
> supermemory, mem0) oder einen Dienst hinter WireGuard. Kuratiert aus eigenem A/B-Research 2026-06-19
> (Firecrawl + deepseek-v4-pro, gegengeprüft mit Opus) + hostinger.com / vpsbenchmarks.com.
>
> **Zweite Seite der Medaille:** [`best-practices/server/vps-hosting.md`](../../best-practices/server/vps-hosting.md).
> Verwandt: [`best-practices/opencode/self-hosted-memory-server.md`](../../best-practices/opencode/self-hosted-memory-server.md),
> [`bugs/server/wireguard.md`](wireguard.md).
> **Stand:** 2026-06-19 (Preise/Benchmarks ändern sich — Datum beachten). **Anker:** Hostinger/Hetzner-Preise 2026-06.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ „Reicht Shared/Web-Hosting für eine eigene DB/Dienst?" | **NEIN** — Shared = kein root, kein Docker, keine eigenen Dienste. **Nur VPS** (KVM) hat root + Docker. | §1 |
| 2 | Hostinger-Einstiegspläne für KI-Workload | **Netzwerk-Benchmark Note E**, CPU Note E/D (KVM 1/2) laut vpsbenchmarks → für rechenintensive Embeddings (Ollama) ungeeignet, für reines pgvector ok. | §2 |
| 3 | Hostinger-Preis wirkt billig | **Verlängerungs-Falle:** Einführungspreis (z.B. $6.49) gilt nur für die Laufzeit; Verlängerung ~2× ($11.99), regulär noch höher ($19.49). | §3 |
| 4 | Backup bei Hostinger | Nur **wöchentliche** Backups gratis; **tägliche** kosten extra — riskant für eine oft schreibende DB. | §4 |
| 5 | RAM für den Stack | pgvector allein: kleinster VPS (4 GB) ok. **Lokale Ollama-Embeddings: 8–16 GB** (KVM 2/4). | §5 |
| 6 | EU-Datenschutz / flexible Abrechnung | **Hetzner** (DE/EU, stundengenaue Abrechnung, bessere Netzwerk-/CPU-Note) statt Hostinger, wenn Datenschutz/Performance zählt. | §6 |

---

## 🔗 Bezugs-Tabelle
| Bug-Abschnitt | Best-Practice (`best-practices/server/vps-hosting.md`) |
|---|---|
| §1 Shared ungeeignet | §1 Produktwahl (nur VPS/KVM) |
| §2–§4 Hostinger-Fallen | §2 Anbieter-Wahl + Dimensionierung |
| §5–§6 RAM / Hetzner | §2, §3 |

---

## 1. Shared/Web-Hosting ist für eigene DB/Dienste ungeeignet
**Symptom:** Auf Shared-Hosting lässt sich kein Postgres/Docker/MCP-Server betreiben.
**Ursache:** Shared-Umgebungen geben **keinen root-Zugriff** und **kein Docker** — nur vorgegebene Web-Stacks.
**FIX:** **VPS (KVM)** wählen. Bei Hostinger bewirbt nur die VPS-Linie root + integrierten Docker-Manager + PostgreSQL
(inkl. pgvector „pg_vector for AI embeddings", 1-Klick-Install). Cloud-Hosting ist dort kein eigenständiges DB-Produkt.

## 2. Hostinger Netzwerk-/CPU-Performance schwach (Benchmark Note E/D)
**Symptom:** Latenz/Durchsatz schwach bei API-/Embedding-Last.
**Ursache:** Unabhängige Benchmarks (vpsbenchmarks.com) geben Hostinger-VPS **Netzwerk Note E** (schlechtester Wert),
CPU Note E (KVM 1) bzw. D (KVM 2).
**FIX:** Für **reines pgvector** (externe Embeddings) ok. Für **lokale Ollama-Embeddings** (rechenintensiv) größeren
Plan (KVM 4) oder besser **Hetzner** (bessere Netzwerk-/CPU-Note) wählen.
**Versionen/Stand:** 2026-06.

## 3. Verlängerungspreis-Falle bei Hostinger
**Symptom:** „$6.49/Monat" entpuppt sich später als deutlich teurer.
**Ursache:** Einführungspreise gelten nur für die initiale Laufzeit (oft 2 Jahre). Beispiel KVM 1: Einführung $6.49 →
Verlängerung **$11.99** → regulär (ohne Bindung) **$19.49**.
**FIX:** Gesamtkosten über die geplante Laufzeit rechnen, nicht den Lockpreis. Hetzner hat keinen solchen Sprung
(stundengenau/monatlich, keine Bindung).

## 4. Hostinger: nur wöchentliche Gratis-Backups
**Symptom:** Datenverlust-Risiko bei einer oft schreibenden Memory-DB.
**Ursache:** Kostenlos nur **wöchentliche** Backups; tägliche = kostenpflichtiges Upgrade.
**FIX:** Eigenes `pg_dump`-Cron + Offsite-Kopie einrichten (siehe `best-practices/opencode/self-hosted-memory-server.md`),
nicht auf das Hoster-Backup allein verlassen.

## 5. RAM-Dimensionierung
**pgvector allein** (PostgreSQL-Erweiterung): kleinster VPS (4 GB, KVM 1) reicht für PoC/moderate Datenmengen.
**Lokale Ollama-Embeddings:** RAM-Bedarf steigt stark → realistisch **8 GB (KVM 2)**, besser **16 GB (KVM 4)**.
Bei supermemory-Single-Binary (WASM-Embeddings) ist der LLM-Step der RAM-Treiber — kleines Ollama-Modell wählen
(siehe Memory-Server-Bauplan).

## 6. Hostinger vs. Hetzner (Kurz)
| Kriterium | Hostinger (KVM 2: 2 vCPU/8 GB/100 GB) | Hetzner (CCX13: 2 vCPU/8 GB/80 GB) |
|---|---|---|
| Netzwerk/CPU-Benchmark | Netzwerk **E**, CPU **D** | besser (Netzwerk **C**), aber Stabilität wechselhaft |
| Abrechnung | Bindung + Verlängerungssprung | stundengenau, keine Bindung |
| Traffic | 8 TB | 20 TB |
| Backups | wöchentlich gratis, täglich extra | täglich verfügbar |
| Datenschutz | global (8 Standorte) | DE/EU-Fokus |
| Bedienung | KI-Assistent „Kodee", 1-Klick, einsteigerfreundlich | technischer, weniger Assistenz |
**Faustregel:** Hostinger = einsteigerfreundlich + günstiger Einstieg; **Hetzner = besser bei Netzwerk/CPU, EU-Datenschutz,
flexibler Abrechnung** → für einen Memory-Server mit (lokalen) Embeddings meist die robustere Wahl.

---

## Quellen (Stand 2026-06-19)
- hostinger.com/applications/postgresql · hostinger.com/compare/hostinger-vs-hetzner
- vpsbenchmarks.com/compare/hetzner_vs_hostinger
- Eigenes A/B-Research 2026-06-19 (Firecrawl + deepseek-v4-pro, gegengeprüft mit Opus).

---

## §7 Sicherheits-Audit 28.10.2026 (Hostinger-VPS, Ubuntu 24.04, Docker-Stack)

Quelle: eigenes Audit + Sonnet-5.5-Recherche-Schwarm (7 Researcher), Stand 28.10.2026. Je Eintrag Symptom · Ursache · Versionen · Fix · Quelle.

1. **Caddy-Image hat alte Go-Version** – Caddy 2.11.4 = go1.26.3, 2.11.7 = go1.26.8. Fernauslösbare DoS: crypto/tls CVE-2026-56862 (Go < 1.26.6), HTTP/2-Serverabsturz CVE-2026-97032 (Go < 1.26.9, steckt in KEINEM Caddy-Release bis 2.11.7). Fix: 2.11.7 + global `servers { protocols h1 h3 }`. 2.11.6 meiden (HTTP/2-Panic im reverse_proxy). `caddy build-info | grep ^go` prüft die Go-Version. https://pkg.go.dev/vuln/GO-2026-6617 · https://github.com/caddyserver/caddy/releases/tag/v2.11.7
2. **MinIO Community eingestellt** – Repo archiviert (2026), `minio/minio` auf Docker Hub gelöscht (11.09.2026, Sekundärquelle). RELEASE.2025-09-07 betroffen von CVE-2025-62506, CVE-2026-40344, CVE-2026-41145 (Schreiben in jeden Bucket mit gültigem Access-Key, CVSS 8.8), CVE-2026-34204, CVE-2026-39414 – kein Community-Fix. Fix: Image per `docker save` sichern, Wechsel auf Drop-in `pgsty/silo` (gepinnt) planen. https://github.com/minio/minio/security/advisories · https://github.com/pgsty/silo
3. **Redis ohne Passwort im Docker-Netz** – jeder Nachbar-Container gilt als „authentifiziert“; Redis-RCEs (RESTORE-OOB 8.8.1, ACL-Bypässe 8.8.2/8.8.3, Cluster-Bus 8.8.4) brauchen nur Befehlsrechte. Fix: `--requirepass` + `REDISCLI_AUTH` für den Healthcheck + `redis://:pw@redis:6379`, Rolling-Tag neu ziehen. https://github.com/redis/redis/releases/tag/8.8.4
4. **Rolling-Tags veralten still** – `redis:8-alpine`/`postgres:17-alpine` bekommen Fixe erst nach `docker compose pull` + Neustart (Postgres 17.10 → 17.11: über 20 CVEs). Bei `minio/minio:latest` im selben Compose: Pull **pro Dienst**, sonst bricht der ganze Pull ab. https://www.postgresql.org/docs/release/17.11/
5. **fail2ban 0.10.5–1.1.1: Fremd-Bann per präpariertem SSH-Benutzernamen** (CVE-2026-109786, Debian/Ubuntu ungefixt) → Admin kann ausgesperrt werden. Fix: `ignoreip = 127.0.0.1/8 ::1 10.8.0.0/24` in `/etc/fail2ban/jail.d/00-ignoreip.local`. https://bugzilla.redhat.com/show_bug.cgi?id=2550189
6. **aiohttp-Version im `Server`-Header** – der Relay verriet `Python/3.12 aiohttp/3.11.11` (Dez 2024). Fix: aiohttp auf aktuell pinnen (3.14.5) + in Caddy `header_down -Server`.
7. **cloud-init 26.2 Regression** – 26.2-0ubuntu1~24.04.1 am 23.10.2026 aus noble-updates gelöscht (LP #2171959); 24.1.3 hat nur lokale CVEs (CVE-2024-11584/-6174). Fix: `apt-mark hold cloud-init`, bis das Update sich bewährt hat; Update nur mit Snapshot. https://launchpad.net/ubuntu/+source/cloud-init/+changelog
8. **Docker < 29.9.2 / containerd < 2.4.2 / buildx < 0.37.2** – `docker cp`-Ausbruch CVE-2026-17106 (< 29.7.0), Registry-TLS-Fallback CVE-2026-92543 (< 29.8.2), containerd-Pull-DoS CVE-2026-95839, buildx-Bake CVE-2026-93325. runc ab 1.3.3 nötig (Ausbrüche vom Nov 2025). CRI-Plugin in reinen Docker-Hosts abgeschaltet lassen. https://github.com/moby/moby/security/advisories
9. **Kernel-KEV** – CVE-2026-53362 (KEV 27.08.2026) erst ab 6.8.0-147.147 behoben, CVE-2026-31431 „Copy Fail“ ab -117. Fix wirkt erst nach Reboot: `uname -r` gegen das installierte Paket prüfen. https://ubuntu.com/security/CVE-2026-53362
10. **Samba 4ubuntu9.8 ist KEIN Sicherheitsupdate** (nur FUSE-Panic-Fix); Ubuntu-Versionssprünge immer am Launchpad-Changelog prüfen. https://launchpad.net/ubuntu/noble/+source/samba/2:4.19.5+dfsg-4ubuntu9.8
