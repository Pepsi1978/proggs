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
