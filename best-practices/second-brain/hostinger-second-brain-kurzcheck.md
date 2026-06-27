# Hostinger als Second-Brain-/KI-Agenten-Server Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A)

| # | Situation | Sofort-Regel/Fakt |
|---|-----------|-------------------|
| 1 | Welcher Hosting-Typ taugt ueberhaupt? | NUR **KVM VPS**. Shared = kein Root, Cloud = nur ~1,5–2 GB RAM/vCPU (statt 4 bei KVM), **kein Dedicated im Portfolio**. ([Opus/Firecrawl 2026-06-21]) |
| 2 | Lokale grosse LLMs auf Hostinger? | **Nein** — **keine GPU**, CPU-only, max. 32 GB RAM. 7B@Q4 ~4–6 GB, 13B ~16 GB, 70B braucht 48 GB+. Architektur: **Memory-Server lokal + LLM via Cloud-API**. ([Opus R1/R5]) |
| 3 | **NVMe-Specs sind UMSTRITTEN** (NEU) | Alte 50/100/200/400 GB sind **veraltet** (2025). 2026 widerspruechlich: DE-Seite **30/60/120/240 GB**, US/LLM-Seite **40/80/160/320 GB**. **Vor Kauf die Live-Seite der eigenen Region pruefen.** ([Firecrawl R1/R2]) |
| 4 | OS-Template waehlen | **Ubuntu 24.04 clean image**. AI-Assistant-/Ollama-Template MEIDEN fuer autonome Agenten (Chat-UI, kein Agent-Stack). ([Opus R4]) |
| 5 | OS spaeter wechseln | **OS-Wechsel LOESCHT ALLE DATEN.** Festlegung, kein Experiment. Vor Wechsel Backup. |
| 6 | Welcher KVM-Tier? | **KVM 2 (8 GB)** Start (Memory-/MCP-Server, LLM via API), **KVM 4 (16 GB)** produktiv mit Vektor-DB, **KVM 8 (32 GB)** mehrere Instanzen + grosse Memory-DB. ([Opus R1/R2]) |
| 7 | Vektor-DB-Wahl (≤1 Mio Notizen) | **pgvector** wenn Postgres schon laeuft (ein System, ACID), sonst **Qdrant** (Apache 2.0, ein Container, ACORN-In-Graph-Filter). Self-Host @1M: Qdrant ~850 QPS/p95 8 ms, Weaviate ~380/18 ms, pgvector ~220/48 ms. ([Opus R6]) |
| 8 | Memory-Stack-Wahl (NEU) | Staerkste Retrieval-Suche: **Zep/Graphiti** (LongMemEval **63,8%** vs Mem0 49,0%). Einfachster Direktzugriff: **Mem0** `search()`. **Letta erlaubt KEINEN direkten externen Such-Zugriff** (nur Agenten-Tool-Calls) — wichtiges Auswahlkriterium. ([Opus R7]) |
| 9 | self-hosted Embeddings (NEU) | **BGE-M3 / nomic-embed-text-v1.5** matchen OpenAI text-embedding-3-large auf ~1,5 Punkte bei **~1/40 Kosten** + null Datenabfluss. Quantisierung spart **bis 97% RAM** (Qdrant) bzw. 75% (8-bit). ([Opus R6]) |
| 10 | MCP von aussen erreichbar | MCP-Spec verlangt **OAuth 2.1 + PKCE(S256) + Resource Indicators (RFC 8707)**, TLS 1.3. Nur Port 443 offen, Backend an `127.0.0.1`. **Caddy** (Auto-TLS) empfohlen. ([Opus R8]) |
| 11 | Cloudflare Tunnel? (NEU) | **NEIN fuer privates Gehirn** — CF strippt TLS in seinem Netz und **sieht Klartext**. Bei persoenlichen Daten: eigener TLS-Proxy (Caddy) ODER **Pangolin/WireGuard**. ([Opus R8]) |
| 12 | Mehrere Dienste parallel | **Ja, eine `compose.yaml`.** Pro Service `deploy.resources.limits` + `reservations` (gegen OOM-Killer). Mem0 = 3 Container (Ports 8888/8432/8474). RAM-Faustregel: 8 GB API-LLM, 16 GB+ lokales LLM. ([Opus R9]) |
| 13 | RAM ist der Hauptfaktor | HNSW-Index lebt im **RAM** (nicht Disk). 1M/384-dim float32 ≈ 1,5 GB; 50M/768-dim grob 150 GB+ → dann Quantisierung/pgvectorscale. Bei RAM-Erschoepfung killt der **OOM-Killer** den hungrigsten Prozess (oft die DB). ([Opus R6/R10]) |
| 14 | Kosten-Falle #1 | **Promo- vs. Renewal-Preis**: Verlaengerung ~+100% (KVM 8 ~22 €/$26 → ~50). Dauerbetrieb mit Renewal rechnen. Nur 12-/24-Monats-Terms. ([Opus R1]) |
| 15 | Kosten-Falle #2 (NEU) | **Runaway-Agent-Loop** kostete real **47.000 USD** (264 h). Alerts ≠ Enforcement → harte Caps: 50 $/Tag soft, 100 $/Tag hard, 1000 $/Monat Ceiling. ([Opus R10]) |
| 16 | Backups | **3-2-1-1-0** (verschluesselt, off-host, **immutable/air-gapped**, **monatlicher Restore-Test**). Kompromittierte VPS darf Recovery-Pfad nicht loeschen koennen. ([Opus R10]) |
| 17 | Memory-Sicherheit | **Source-/Provenance-Tag** + Audit-Logging (OpenTelemetry) je Eintrag → gegen Memory-Poisoning, macht Lecks auffindbar. Multi-Tenant: RLS/`agent_id`. ([Opus R10/online]) |
