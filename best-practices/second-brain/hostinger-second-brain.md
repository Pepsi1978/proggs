# Hostinger als Second-Brain-/KI-Agenten-Server — Best Practices

> Synthese aus DREI Recherche-Engines (2026-06-21), konsolidiert: (1) `:online`-Lauf (minimax-m3:online, 10 Researcher), (2) **Firecrawl + MiniMax M3** (10 Researcher, volle Seiten, aktuellste Produkt-Specs), (3) **Opus-Schwarm** (10 Researcher, tiefste Quellenarbeit — gewann 8/10 Themen). Roh-/Volltexte: `hostinger-rohergebnisse-2026-06-21.md` (:online), `hostinger-firecrawl-rohergebnisse-2026-06-21.md`, `hostinger-opus-rohergebnisse-2026-06-21.md`. Engine-Vergleich: `recherche-engine-vergleich-2026-06-21.md`.
> **Faktenstand vor Kauf auf hostinger.com live verifizieren** — Preise UND NVMe-Specs schwanken zwischen Quellen und Region (siehe §1, Spec-Widerspruch).

---

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

---

## §1 Plattform-Eignung & Plan-Wahl

**Quellen (3 Engines):** Opus R1/R2 (hostinger.com/vps-hosting, hostinger.com/vps/llm-hosting, vpsbenchmarks.com), Firecrawl R1/R2 (DE- + US-Produktseiten 2026), :online (crazyegg.com, cybernews.com).

Hostinger (litauischer Anbieter, RZ in Nordamerika/Europa/Asien/Suedamerika) bietet 2026: **Shared/Web Hosting, Managed Cloud Hosting, KVM VPS, Reseller/Agency, Website Builder**. **Kein Dedicated Server.** Cybernews kuert Hostinger zur „Editor's #1 choice for KVM VPS" und „best overall for LLM hosting".

**Warum nur KVM VPS taugt:**
- **Shared Hosting** ❌ — kein Root, kein Docker, kein dauerhafter Daemon-Betrieb.
- **Cloud Hosting** ⚠️ — jetzt teilbelegt (NEU, Firecrawl R1): „Managed VPS", Pläne Cloud Startup/Professional/Enterprise, **~10,83–12,50 $/vCPU**, aber nur **1,5–2,0 GB RAM/vCPU** (KVM hat 4 GB/vCPU) und **Shared CPU** → fuer einen Memory-/Agenten-Server **schlechteres RAM-Verhaeltnis** und kein Root. Konkrete Plan-Specs weiterhin nicht voll beziffert.
- **KVM VPS** ✅ — isolierte Ressourcen (KVM, eigener Kernel), **voller Root-Zugang** (Firecrawl R2 bestaetigt woertlich), Docker-Compose-Manager, dedizierte IP, Public API, woechentliche Backups, DDoS-Schutz (Wanguard), Kodee AI Web Terminal.
- **Grenzen:** keine GPU (→ keine grosse lokale LLM-Inferenz), Container teilen den Host-Kernel (keine MicroVM/bare-metal-Isolation). Wer das braucht → spezialisierte Anbieter (Northflank, Modal, RunPod), **nicht** Hostinger.

### KVM-Spezifikationen 2026 — mit **Spec-Widerspruch** (NEU, vor Kauf live verifizieren!)

**RAM/vCPU/Bandbreite sind in ALLEN Engines identisch** (verlaesslich). **NVMe widerspricht sich** — die alten 50/100/200/400 GB (2025, hostings.info) sind laut Firecrawl **veraltet**:

| Plan | vCPU | RAM | NVMe (DE-Seite, Firecrawl Q1) | NVMe (US/LLM-Seite, Firecrawl Q3) | NVMe (alt 2025 — VERALTET) | Bandbreite |
|---|---|---|---|---|---|---|
| **KVM 1** | 1 | 4 GB | **30 GB** | **40 GB** | ~~50 GB~~ | 4 TB |
| **KVM 2** (meistgewaehlt) | 2 | 8 GB | **60 GB** | **80 GB** | ~~100 GB~~ | 8 TB |
| **KVM 4** | 4 | 16 GB | **120 GB** | **160 GB** | ~~200 GB~~ | 16 TB |
| **KVM 8** (Top) | 8 | 32 GB | **240 GB** | **320 GB** | ~~400 GB~~ | 32 TB |

> **Spec-Konflikt (3 Engines uneinig):** Die deutsche Hostinger-Hauptseite zeigt die kleineren NVMe-Werte (30/60/120/240 GB, EUR-Preise), die englische/LLM-Seite + Blog + vpsbenchmarks die groesseren (40/80/160/320 GB, USD-Preise). Das alte Opus-/`:online`-Bild (50/100/200/400 GB) ist die **2025er-Generation und gilt nicht mehr**. **Vor Kauf NVMe auf der Live-Seite der eigenen Region pruefen** — nur vCPU/RAM/Bandbreite sind sicher.

- **Hardware:** AMD EPYC (Benchmark KVM 8 Jan 2026: **EPYC 9354P 32-Core**; CPU-Modell aber **nicht garantiert/standortabhaengig** — auch EPYC 7543P / Xeon Silver 4214 gesehen). NVMe ~3.500 MB/s Lese / ~2.100 MB/s Schreib (>3.000 MiB/s im Benchmark). 1 Gbps Netzwerk (KVM-8-Benchmark-Note Netzwerk = **D**, schwaechste Einzelnote — bei viel gleichzeitigem Traffic beachten). Bei Bandbreiten-Ueberschreitung: Drosselung auf 10 Mbps, keine Overage-Gebuehr.

### Plan-Empfehlung (3 Engines einig)
- **KVM 2 (8 GB, 2 vCPU)** — Start fuer reinen Memory-/MCP-/API-Server, LLM via Cloud-API (Opus nennt das den dokumentierten Sweet-Spot, ~8,99 $ Aktion).
- **KVM 4 (16 GB)** — produktiver Sweet-Spot fuer Agenten-Backend + Vektor-DB mit mehreren 100k Eintraegen + Container-Stack.
- **KVM 8 (32 GB)** — komfortabel fuer groessere Vektor-Indexe, parallele Agenten, mehrere Dienste.

**Preis-Widerspruch (vor Kauf verifizieren):** Aktion KVM 1 ~$6,49 (offiziell) bzw. $4,99 (Promo-Reviews); KVM 8 ~$25,99 → Renewal ~$49,99 (smarthostfinder) bzw. $73,99 (vpsbenchmarks Liste). Ursache = Promo- vs. Renewal-Preis × Laufzeit (24/48 Mon.) × Coupon. **Fuer den Dauerbetrieb den Verlaengerungspreis ansetzen** (z.B. KVM 2 real eher ~$13–18/Mo).

**Skalierung:** Vertikales Upgrade „jederzeit auf hoeheren Plan" (laut Firecrawl Q5 „no downtime, no data migration" — Mechanik mit/ohne Reboot in den Opus-Quellen nicht eindeutig belegt). Kein KVM 3/5/6/7, nur vier Stufen. Downgrade nicht beworben. Kein granulares „RAM/vCPU einzeln dazubuchen", Skalierung ueber KVM 8 hinaus (Cluster/Sharding) nicht dokumentiert.

---

## §2 OS & Grund-Setup

**Quellen:** Opus R4 (Distro-Vergleich, Support-Zeitraeume), :online (openclawlaunch.com — Hermes-Guide), Firecrawl R4.

**Empfehlung (3 Engines einig): Ubuntu 24.04 als Clean-OS-Image.**
- **AI-Assistant-/Ollama-Template MEIDEN** fuer autonome Agenten: kommt mit Ollama + Open WebUI (Chat-UI), „aber nicht nuetzlich, wenn man einen plattformuebergreifenden autonomen Agenten will". Fuer einen Agenten braucht man Docker auf sauberem System.
- **Ubuntu-Vorteil:** breiteste Drittanbieter-Kompatibilitaet — Installer/Panels „testen typischerweise zuerst auf Ubuntu". Pfad des geringsten Widerstands fuer neue Agent-/Memory-Frameworks.
- **Debian-Alternative:** geringerer RAM-Footprint (nur bei knappem RAM 1–2 GB messbar), kein Snap, maximale Stabilitaet.
- **Alpine:** minimaler Footprint („every megabyte matters") — aber Aktualitaet von KI-Paketen (Ollama/LLM-Versionen) kann eingeschraenkt sein, ggf. manuell nachziehen (Firecrawl R4).
- **AlmaLinux/Rocky/CentOS:** LTS-faehig, aber **kein Quellenbeleg** fuer KI-/Agent-Vorteil.

**Verfuegbare Distros bei Hostinger** (teils widerspruechlich): Alpine, CentOS 9/10 Stream, CloudLinux, Debian 11/12/13, Fedora Cloud 42, Kali, openSUSE Leap, Rocky 8/9/10, plus Ubuntu, AlmaLinux, Arch („elf Distributionen", 70+ Templates inkl. Panels/Apps). Image-Kategorien: (1) Operating System (minimal), (2) Control Panel, (3) Application (OS + App), (4) Container (Docker).

**Grund-Setup-Pflicht:**
- **SSH-Key in hPanel VOR der Provisionierung** hinzufuegen; **Passwort-Login fuer Root danach deaktivieren** (PasswordAuthentication no, Root-Login aus).
- **OS-Wechsel loescht alle Daten** → OS-Wahl ist eine Festlegung. Vor jedem Wechsel Backup.
- Update-Befehle: Ubuntu/Debian `apt update && apt upgrade`; AlmaLinux/Rocky/CentOS `dnf update -y`.
- **Non-root-Betrieb:** Agent NIE als root laufen lassen — dedizierter System-User mit minimalen Rechten (Opus R10).

---

## §3 1-Klick-Templates

**Quellen:** Opus R3/R5 (Hostinger Application-/VPS-Seiten), Firecrawl R3, :online.

| Anwendung | Als 1-Klick-Template belegt? |
|---|---|
| **Docker** | ✅ ja (als „Beliebt" markiert) |
| **n8n** | ✅ ja (Hostingers Template **Nr. 1**: >50.000 Installs seit 01/2025, ~800/Mo) |
| **Open WebUI** | ✅ ja |
| **Claude Code** | ✅ gelistet auf VPS-Seite |
| **Codex CLI** | ✅ gelistet auf VPS-Seite |
| **MCP** | ✅ gelistet (Firecrawl R3) |
| **Agent Zero** | ✅ 1-Klick-Docker-Vorlage (~1 GB RAM Min.) |
| **OpenClaw** | ✅ 1-Klick (+ „Managed OpenClaw"), Multi-Channel (WhatsApp/Telegram/Slack/Discord/Signal/iMessage/Teams) |
| **Hermes Agent / Hermes Workspace** | ✅ 1-Klick (Web-UI: Chat, Memory, 100+ Skills, Terminal, Conductor fuer Sub-Agenten) |
| **AnythingLLM, OpenHuman, Hollama** | ✅ gelistet |
| **Ollama** | ⚠️ als App-Template gelistet, aber kein eigenstaendiger Agent-Stack |
| **Coolify / CapRover** | ❌ nicht belegt (Firecrawl: zu CapRover „keine belastbare Aussage") |

**Mehrere gleichzeitig:** Zwei Ebenen (Opus R3) — **Ebene A = OS-Template (genau EINS pro VPS)**, **Ebene B = Docker-Katalog-Apps (VIELE gleichzeitig)**. Man installiert EIN OS-/Container-Template und ergaenzt weitere Apps manuell via Docker (z.B. n8n + Ollama im selben Stack).

**Template wechseln:** im Kern eine **Neuinstallation des OS** → typischerweise destruktiv (Daten weg). Vor Wechsel Backup. (Nicht-destruktiver Wechsel in keiner Quelle belegt.)

**1-Klick vs. normaler VPS:** **1-Klick spart Zeit, nicht Geld** — laeuft auf denselben KVM-Plaenen. Normaler VPS = volle Kontrolle (eigenes DB-Tuning, Reverse-Proxy, Backups), Setup/Updates/Sicherheit selbst.

---

## §4 KI-Agent-Frameworks

**Quellen:** Opus R5 (Hostinger-App-Seiten, Letta/RamNode-Docs, Canadian Web Hosting), :online, Firecrawl R5.

**Kernbefund (NEU, Opus R5):** Ein **normaler KVM-VPS + Docker reicht fuer ALLE gaengigen Frameworks** (Hermes, OpenClaw, Letta, CrewAI, AutoGPT, Agent Zero). 1-Klick ist reiner Komfort, kein technisches Muss. **Das Framework selbst ist leicht (1–4 GB RAM)** — die 16–32 GB/GPU braucht man NUR bei lokalem LLM, nicht fuers Agenten-Framework.

| Framework | RAM-Min | Praktisch | Hostinger 1-Klick? | Hinweis |
|-----------|---------|-----------|--------------------|---------|
| **Hermes Agent** | ~4 GB | 8 GB (KVM 2) | **Ja** | MIT-Lizenz frei; **OpenAI Codex OAuth** → ChatGPT-Abo nutzbar statt API-Rechnung. Pro Agent eigene `.env`/Keys. |
| **OpenClaw** | 4 GB (Test 2 vCPU/4 GB) | 8 GB / 4 vCPU (Prod) | **Ja** (+Managed) | KVM Pflicht (Docker); Modellwechsel/Telegram-Konfig teils fehleranfaellig (Community). |
| **Letta** (Second-Brain-relevant) | 2 GB | 4 GB+ (skaliert mit Memory) | Nein (Docker selbst) | **Technisch am besten passend** fuers Memory: gebuendeltes Postgres + pgvector, stateful, Port **8283**, Bearer-Token-Auth, pg_dump-Backups. DB waechst +50–200 MB/Monat/Agent. Erste Alembic-Migration auf 2-GB-VPS dauert 2–3 Min (kein Haenger — abwarten). |
| **CrewAI** | ~1 GB | 4 GB | Nein | Standard-VPS via Docker/pip. |
| **AutoGPT** | ~2 GB | 4 GB | Nein | `docker compose up`, externer API-Key noetig. |
| **Agent Zero** | ~1 GB | 4 GB | **Ja** | — |

**Empfehlung:** Fuer ein reines Second Brain (Memory + LLM via Cloud-API) ist **KVM 2 (8 GB)** der Sweet-Spot. Fuer persistente Memory ist **Letta** technisch am passendsten (Postgres+pgvector, stateful), erfordert aber manuelles Docker-Compose. RAM-Reserve fuer Memory-Wachstum einplanen. Agenten immer in Docker isolieren, Netzwerk einschraenken, **nie Root geben**.

---

## §5 Second-Brain/Memory selbst bauen (Vektor-DBs)

**Quellen:** Opus R6 (Encore, Kalvium Labs, Tiger Data, Vectorize, Zylos), Firecrawl R6, :online.

**Anforderungsprofil Second Brain:** 10k–1 Mio. Notizen → <10 Mio. Vektoren, read-heavy, Single-User, sub-100 ms. Bei <10 Mio. sind **alle vier Kandidaten ausreichend** — Self-Host schliesst managed SaaS (Pinecone) aus.

### Self-Host-Vergleich mit konkreten Zahlen (NEU — Opus R6)

| DB | Lizenz | RAM @1M | QPS / p95 @1M (768-dim, HNSW) | Filterung | Hybrid | Komplexitaet |
|----|--------|---------|-------------------------------|-----------|--------|--------------|
| **pgvector** | Postgres (permissiv) | ~1,4 GB | **~220 QPS / p95 ~48 ms** (4 Worker: ~360/~58 ms) | Post-Filter | via `pg_bm25` | **Niedrig** (in bestehendem Postgres) |
| **Qdrant** | **Apache 2.0** | ~1,4 GB | **~850 QPS / p95 ~8 ms** | **In-Graph (ACORN)** | **Nativ** (dense+sparse) | **Niedrig** (1 Container) |
| **Weaviate** | **BSD-3** | ~2,1 GB | **~380 QPS / p95 ~18 ms** | Payload-indiziert | **Nativ** (+ eingebaute Vectorizer OpenAI/Cohere/HF) | Mittel (Schema vorab) |
| **Milvus** | **Apache 2.0** | hoeher | — (skaliert >1 Mrd) | mehrere Indizes | nativ | **Hoch** (etcd + Object-Store + Queue) |

- **pgvector-Skalierungsfalle:** oberhalb **5–10M Vektoren** spuerbar langsamer (HNSW-Index muss in RAM passen); ab ~2M Index-Build > 20 Min. Abhilfe: **pgvectorscale** (471 QPS @99% Recall auf 50M, 11,4× Qdrants 41 QPS bei dieser Skala). Bei 50M/768-dim grob **150 GB+ RAM**.
- **Qdrant:** ACORN-Filter laufen IM HNSW-Traversal (gefilterte Queries bleiben schnell, selbst wenn der Filter 99% eliminiert). **Quantisierung reduziert RAM um bis zu 97%** → laeuft mit Millionen Vektoren auf 4-GB-Instanz; 100k Vektoren sogar auf 512 MB–1 GB.
- **Benchmark-Vorsicht:** QPS-Zahlen verschiedener Blogs sind NICHT 1:1 vergleichbar (1M: Qdrant ~850 vs pgvector ~220; 50M: pgvectorscale 471 vs Qdrant 41) — Skala/Hardware/Index unterschiedlich. Kein echter Widerspruch, sondern skalenabhaengig.

**Empfehlung (3 Engines einig):** **„pgvector wenn du schon Postgres hast, Qdrant wenn nicht"** (Encore). pgvector = Unified Store (Vektoren + SQL-Metadaten + Volltext, ein Backup, ACID); Qdrant = dediziert, schlank, beste Filtered-Search, ein Container.

### Embeddings (NEU — Opus R6, vorher reine Luecke)

| Modell | Typ | RAG-Genauigkeit | Dim | Kosten | Besonderheit |
|--------|-----|-----------------|-----|--------|--------------|
| OpenAI text-embedding-3-large | API | **80,5%** | bis 3072 (Matryoshka) | $0,13/1M Tok | beste Qualitaet, Daten verlassen den Server |
| OpenAI text-embedding-3-small | API | 75,8% | bis 1536 | ~$0,02–0,03/1M | bestes API-Preis/Qualitaet |
| **BGE-large-en-v1.5** | lokal | 71,5% | 1024 | nur Compute | null Datenabfluss |
| **nomic-embed-text-v1.5** | lokal | 71,0% | bis 768 | nur Compute | **8192-Token-Kontext** (lange Notizen) |
| **BGE-M3** | lokal | (empfohlen) | — | nur Compute | fuer Datensouveraenitaet |

- **Kern-Fakt:** BGE-large / nomic-embed matchen OpenAI text-embedding-3-large **innerhalb 1,5 Punkten bei ~1/40 der Kosten und null Datenabfluss** → fuer ein privates Gehirn ist self-hosted die Default-Wahl.
- **Matryoshka-Embeddings** (OpenAI/Nomic): Dimensionen gegen Speicher tauschen OHNE Re-Embedding (z.B. 1536 erzeugen, bei Speichernot auf 768 kuerzen).
- **Faustregel Dim:** 768–1536 reicht fuer die meisten Faelle.
- **Quantisierung:** 32→8-bit spart ~75% Speicher bei minimalem Recall-Verlust (Firecrawl R6).

### Retrieval-Architektur (NEU — Opus R6)
- **Fact-Level-Chunking verdoppelt die Retrieval-Qualitaet** gegenueber Session-Level → einzelne Fakten als eigene Einheiten speichern, nicht ganze Gespraeche. „Chunking + Embedding-Qualitaet sind entscheidender als die DB-Wahl."
- **Hybrid (BM25 + dense Vektor) schlaegt reine Vektorsuche**, optional + Graph-Layer (Graphiti-Muster), danach **Reranking**. Ziel: **P95 ~300 ms ohne LLM-Call zur Query-Zeit**.
- **Daten als „chunks + provenance" modellieren** (immutable chunk IDs, Quell-Doc-Version, Offsets, Pipeline-Version) → erleichtert Re-Chunking/Re-Embedding.
- **Trend Unified Storage:** PostgreSQL + pgvector fuer Vektorsuche + Standard-SQL — eine DB fuer Vektoren, Metadaten, Volltext.

**Minimal-Architektur:** Markdown/Web/PDF → Ingestion (Chunking Fact-Level + Embedding self-hosted + Provenance-Metadaten) → Vektor-DB (pgvector/Qdrant) → Hybrid-Retrieval + Reranking → LLM (Cloud-API) → Antwort + Quellen → UI.

---

## §6 Self-hosted Memory-Stacks (fertige Layer)

**Quellen (NEU, jetzt voll ausgewertet — Opus R7: particula, atlan, vectorize, supermemory-Docs):** Im `:online`-Lauf blieb dieser Researcher unausgewertet — der Opus-Schwarm hat die Substanz geliefert.

### Vergleichsmatrix Such-/Retrieval-Faehigkeiten (NEU)

| Stack | Hybrid-Suche | Graph | Temporal | Reranking | **Direkter externer Such-Zugriff** | OSS | LongMemEval | Lizenz/Sterne |
|-------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Mem0** | ✅ (V+G+KV) | ✅ (Pro) | ❌ | k.A. | ✅ `search()` | ✅ | 49,0 % | Apache 2.0 / ~47–48K |
| **Letta** | teils | ✅ (Free) | ✅ | k.A. | **❌ nur Agenten-Tool-Calls** | ✅ | n/a | Apache 2.0 / ~13K |
| **Zep/Graphiti** | ✅ (Embed+BM25+Graph) | ✅ | ✅ | implizit (kein LLM @query) | ✅ Graph-Query | ⚠️ (Feature-Luecke) | **63,8 %** | OSS / ~5K |
| **supermemory** | ✅ (Sem+BM25+Graph+temporal) | ✅ | ✅ | ✅ Cross-Encoder | ✅ `/v4/search` | ⚠️ widerspruechlich | self-reported | **unklar (s.u.)** |
| **Cognee** | ✅ | ✅ (Neo4j/Falkor/Kuzu) | teils | k.A. | ✅ `.search()` | ✅ (100% lokal) | k.A. | OSS / ~7K |

**Die wichtigsten Auswahlkriterien (NEU):**
- **Staerkste Retrieval-Suche:** **Zep/Graphiti** — Hybrid (Embeddings + BM25 + Graph) + temporaler Wissensgraph („was ist jetzt wahr / was war im Maerz wahr?"), **kein LLM zur Query-Zeit**, P95 ~300 ms. **LongMemEval 63,8 % vs Mem0 49,0 %** — also +15 Punkte, NICHT die Marketing-94,4 %. Preis: hoeherer Self-Hosting-Aufwand, Graphiti-OSS ≠ Feature-Paritaet mit Zep Cloud.
- **Einfachster Direktzugriff:** **Mem0** — `add()`/`search()`, Apache 2.0, voll self-hostbar (3 Container, s. §8). Schwaeche: **kein temporales Fact-Modeling** (Fakten werden ueberschrieben statt versioniert → „Was war wann wahr?" schlaegt fehl).
- **⚠️ Letta erlaubt KEINEN direkten externen Such-Zugriff** — Retrieval laeuft NUR ueber Tool-Calls des Agenten (Core/Recall/Archival). **Wichtiges Auswahlkriterium:** Wenn mehrere Clients (CLIs, Auto-Sprach-App) direkt suchend zugreifen sollen, ist Letta die falsche Wahl. Letta ist stark, wenn der **Agent selbst** seinen Speicher steuert.
- **supermemory:** fertige `/v4/search` (Hybrid + Cross-Encoder-Reranking, lokale Embeddings, MCP fuer Claude Code/OpenCode, `localhost:6767`), sub-300 ms. **ABER kritischer Lizenz-Widerspruch:** offizielle Docs/atlan nennen es „open source"; ein Such-Snippet behauptet, das **Backend sei closed-source** (oeffentliches Repo nur Frontend+SDK, offizielles Self-Hosting enterprise-only) — es gibt eine MIT-Dritt-Reimplementierung (`s11ngh/supermemory-selfhosted`, Postgres+pgvector). **Vor produktiver Nutzung am offiziellen Repo verifizieren** — nicht abschliessend geklaert.
- **Cognee:** Poly-Store (Vektor + austauschbare Graph-DBs), 100% lokal, OSS. Schwaeche: kein Managed Cloud, keine SOC2/HIPAA.

**Empfehlung:** Fuer einen Second-Brain-Server mit **direktem Such-Zugriff mehrerer Clients** und sauber OSS: **Mem0** (einfachster Direktzugriff, Apache 2.0) oder **Zep/Graphiti** (staerkste Suche, dafuer mehr Ops). **supermemory nur**, wenn der OSS-/Self-Hosting-Status am offiziellen Repo bestaetigt ist. **Letta** nur, wenn der Agent selbst sein Memory verwaltet.

---

## §7 MCP-Server & API von aussen sicher erreichbar

**Quellen (NEU, tief — Opus R8):** modelcontextprotocol.io, systemprompt.io, Obsidian Security, mangohost/onidel (Caddy vs Nginx), vps.do (UFW/Fail2Ban). Plus :online (gelembjuk.com).

**Grundarchitektur:** App-Prozesse (MCP-Server, API) lauschen NUR auf `127.0.0.1`; davor ein Reverse Proxy als einziger offener Port **443 (HTTPS, TLS-Termination am Proxy)**.
```
Internet ──443/TLS──> Reverse Proxy (Caddy/Nginx) ──localhost──> MCP-Server (:3000)
                                                  └─localhost──> API (:8080)
```

**MCP-Spec-Pflichten (NEU — verbindlich):**
- **TLS 1.3** fuer alles ausser Localhost; Forward-Secrecy-Ciphers (`TLS_AES_256_GCM_SHA384`, `TLS_CHACHA20_POLY1305_SHA256`); HSTS auf allen Responses.
- **OAuth 2.1 + PKCE (RFC 7636, S256 Pflicht ab MCP-Spec 11/2025)** fuer HTTP-Transports. MCP-Server = OAuth Resource Server.
- **Resource Indicators (RFC 8707):** Client gibt Ziel-MCP-URL als `resource`-Parameter → **audience-gebundenes** Token, das nicht gegen einen anderen MCP-Server abgespielt werden kann.
- **`.well-known`-Endpoints:** `/.well-known/oauth-protected-resource` + `/.well-known/oauth-authorization-server`, Pflichtfeld `code_challenge_methods_supported: ["S256"]`.
- **Token-Lifecycle:** kurze Expiry (~1 h), Refresh-Rotation mit Reuse-Detection, `/oauth/revoke`.

**Reverse Proxy — Caddy vs Nginx (NEU, mit Zahlen):**

| Kriterium | Caddy | Nginx |
|---|---|---|
| Auto-TLS / Renewal | **Ja, eingebaut** (Let's Encrypt, OCSP, Wildcards) | Nein — Certbot + Cron noetig |
| Config-Aufwand | 1–2 Zeilen/Site | mehrzeilig |
| Idle-RAM | 15–25 MB | **2–8 MB** |
| grosse Files/Streaming | langsamer | **17% schneller, 38% weniger RAM** |
| HTTP/3 (QUIC) | nativ, Caddy vorne | vorhanden |

→ **Caddy als Default** (Second-Brain-Last ist gering, Auto-TLS spart Wartung). Caddyfile-Minimal:
```caddyfile
api.second-brain.example { reverse_proxy 127.0.0.1:8080 }
```
> **Falle:** Caddy `rate_limit` ist KEIN Core-Modul → braucht Community-Plugin via `xcaddy` (leicht zu uebersehen).

**Firewall & Haertung (NEU — Opus R8):**
- **UFW:** `default deny incoming`, `allow 443/tcp`, SSH (22) **key-only + auf eigene IP beschraenkt**.
- **Fail2Ban:** log-basierter Brute-Force-Bann (ergaenzt Webserver-`limit_req`).
- **Rate-Limiting doppelt:** im Proxy UND in der App.
- **MCP-Haertung:** Least-Privilege-Scopes (`tools/list` nach Auth filtern), parametrisierte Queries (nie Raw-SQL vom Modell), **Read- und Write-Tools auf getrennten Servern** (Prompt-Injection), Audit-Logging je Tool-Call (Identity/Args/Ergebnis/Client-IP).

**⚠️ Cloudflare Tunnel — NEIN fuer ein privates Gehirn (NEU, wichtig):** `cloudflared` braucht keinen offenen Port und liefert DDoS-Schutz, ABER **terminiert TLS bei Cloudflare → CF kann den Klartext sehen** (keine E2E-Verschluesselung). Plus 100-MB-Limit/Item, ToS-Limits. **Bei persoenlichen Daten → eigener TLS-Reverse-Proxy (Caddy) auf dem VPS ODER Pangolin/WireGuard** (self-hosted Tunnel ohne CF-Privacy-Nachteil). CF-Tunnel nur, wenn DDoS-Komfort > Vertraulichkeit.

**Haeufige MCP-OAuth-Fehler:** OAuth-State nicht an User-Session gebunden → CSRF / **One-Click Account Takeover**; CORS `Access-Control-Allow-Origin: "*"` (nie Wildcard); `invalid_grant` (Code abgelaufen ~60 s / PKCE-Mismatch).

---

## §8 Mehrere Dienste gleichzeitig (Docker Compose)

**Quellen (NEU, tief — Opus R9):** docker.com/blog, Docker Deploy-Spec, mem0.ai, DeployHQ, DEV (4 Patterns). Plus :online.

**Man muss sich NICHT festlegen — kombinieren ist der dokumentierte Standard.** Agent-Server + Memory-Server + (optional LLM) + MCP-Tools in **einer `compose.yaml`**, jeder Dienst eigener Container.

**Konkrete Multi-Service-Belege (NEU):**
- **Mem0 = 3 Container** mit verschobenen Host-Ports (damit Standard-Ports frei bleiben): `mem0` 8888→8000 (FastAPI), `postgres`(pgvector) 8432→5432, `neo4j` 8474→7474 + 8687→7687. Interner Verkehr ueber Bridge-Netz, **Host-Ports an `127.0.0.1` binden**.
- **Reverse-Proxy fuer mehrere Web-Dienste:** Traefik (dynamisch via Container-Labels, Routing nach Host/Pfad) oder Caddy/Nginx — nur 443 nach aussen.

**Ressourcen-Limits (NEU — die zentrale Stolperfalle):**
```yaml
services:
  memory-server:
    deploy:
      resources:
        limits:        { cpus: "1.0", memory: 512M }   # harte Obergrenze
        reservations:  { cpus: "0.5", memory: 256M }   # weiche Garantie (darf bursten)
```
- **`reservations` NEBEN `limits` setzen** → ein Inference-/Memory-Dienst frisst beim Modell-Laden nicht den ganzen Host-RAM und killt nicht per **OOM-Kill** die anderen Container (oft trifft es PostgreSQL = DB weg mitten im Lauf).
- **Healthchecks + `depends_on: condition: service_healthy`** (`start_period: 120s` fuers Modell-Laden) gegen Race-Conditions beim Start.

> **Hinweis (frueher als Luecke notiert, jetzt geklaert):** `deploy.resources.limits` greift bei `docker compose up` (nicht nur Swarm) — die DEV/Docker-Docs zeigen es als Standard-Pattern fuer Compose-Stacks. (Aeltere SO-Quelle bezog sich auf v3-Verhalten; aktuelle Compose-Spec respektiert `deploy.resources`.)

**Typische Limit-Werte (aus Quellen):** Mem0-API 512M/1.0 CPU; Neo4j (schwerster Container) ~2 GB; einfacher Agent 512M/1.0; Agent Zero Limit 2G/1.5 + Reservation 1G/1.0; lokales LLM Limit 16G/Reservation 8G; Basis-PostgreSQL ~300–500 MB.

**RAM-Faustregel (NEU — der einzige echte Engpass):**

| Szenario | RAM |
|----------|-----|
| Agent, externe LLM-API, ohne Vektor-DB | ab **4 GB** |
| Agent + Vektor-DB + Redis + PostgreSQL, externe LLM-API | **8 GB** (solide) |
| zusaetzlich **lokales LLM** auf demselben Host | **16 GB+** |

Mem0-Memory-Server allein: t3.medium (2 vCPU, 4 GB) Minimum, t3.large (8 GB) empfohlen. Quantisiertes 7B-Modell (Q4_K_M) ~4–6 GB.

---

## §9 Best Practices & Sicherheit

**Quellen:** Opus R10 (virtua.cloud, bluehost, ranksquire), :online (digitalapplied, webnestify, micheallanham).

- **Schlanker Host:** minimaler OS-Footprint (clean image, headless) → kleinere Angriffsflaeche, mehr CPU/RAM fuer die Dienste.
- **Produktion gehoert auf den VPS, nicht auf den Entwickler-Laptop** (haeufige Key-Leak-Quelle).
- **Memory-Layer von Anfang an:** verwandelt stateless LLM in einen Assistenten mit Gedaechtnis, senkt Token-Kosten bis ~90% (sechs Spar-Techniken: Importance-Based Eviction mit Ebbinghaus-Decay −59%, Token Budgeting −75%, Hierarchical Summarization −59%).
- **Sandbox-Hierarchie (NEU):** **MicroVM (Firecracker/Kata) > gVisor > gehaerteter Container** (`--read-only`, `--no-new-privileges`, Capability-Dropping). **Standard-Docker ist KEINE Sicherheitsgrenze** (Container teilen den Host-Kernel).
- **Lethal Trifecta (NEU):** private Daten + untrusted Inhalt + externe Kommunikation + persistentes Memory = die gefaehrliche Kombination fuer Datenabfluss. Agent-Eingaben grundsaetzlich als untrusted behandeln.
- **Secrets:** API-Keys nie im Code/Klartext — geschuetzte Env-Datei mit **600-Permissions** ueber systemd `EnvironmentFile`.
- **Audit-Logging & Provenance:** OpenTelemetry-konform (Prompts sanitisiert, Responses, Tool-Calls, Entscheidungspfade); **`source`-Feld auf jedem Memory-Eintrag** → Memory wird vom Black-Box zum durchsuchbaren Log; schuetzt gegen Memory-Poisoning.
- **Multi-Tenant-Isolation auf DB-Ebene:** jeder Agent sieht nur eigene Daten (Supabase RLS + JWT mit `agent_id`). Vergessene RLS-Policies sind unsichtbare Fehler.
- **Day-1-Monitoring (NEU):** Alarme ab Tag 1 — Query-Latenz >100 ms für >60 s, RAM >85%, fehlgeschlagene Index-Snapshots; CPU-`steal`-Wert beobachten (CPU-Steal/„Ghost Load" durch Overselling).
- **Backups im Security-Modell:** verschluesselt, dedupliziert, off-host, Key-getrennt — kompromittierte VPS darf ihren Recovery-Pfad nicht loeschen koennen.
- **Sicherheitsfeatures aktiv lassen:** bei blockierten Tasks Prompts verfeinern/whitelisten statt Schutz global abzuschalten (entfernt die Schicht, die 73% der Angriffe faengt).
- **Skill-Supply-Chain (NEU):** Anfang 2026 war grob **jedes achte Paket** in einem Agent-Skill-Marktplatz boesartig (341/2.857) — nie ungeprueafte Skills/Plugins installieren.

---

## §10 Fallen & was man NICHT tun sollte (Bug-Teil)

| # | Falle | Engine |
|---|-------|--------|
| 1 | **OS-Wechsel loescht ALLE Daten** — OS-Wahl als Experiment behandeln | online/Opus |
| 2 | **AI-Assistant-/Ollama-Template** fuer autonome Agenten — Chat-UI, kein Agent-Stack | Opus R4 |
| 3 | **Grosse lokale LLMs erwarten** — keine GPU, max 32 GB; 70B braucht 48 GB+ → extern/API | Opus R1/R5 |
| 4 | **Veraltete NVMe-Specs glauben** (50/100/200/400 GB sind 2025) — aktuell 30/60/120/240 (DE) bzw. 40/80/160/320 (US); vor Kauf live pruefen | **Firecrawl R1/R2** |
| 5 | **Cloud-Hosting fuer Memory-Server** — nur ~1,5–2 GB RAM/vCPU + Shared CPU + kein Root | **Firecrawl R1** |
| 6 | **Promo- vs. Renewal-Preisfalle** — Verlaengerung ~+100%; nur 12-/24-Mon.-Terms | Opus R1 |
| 7 | **OOM-Killer ohne `reservations`** — ein Dienst frisst Host-RAM, OOM killt die DB; immer `limits`+`reservations` + Healthchecks | **Opus R9** |
| 8 | **Runaway-Agent-Loop ohne Budget-Ceiling** — real 47.000 USD (264 h, zwei Agenten ping-pongen); Alerts ≠ Enforcement → harte Caps | **Opus R10** |
| 9 | **Auth-Bypass bei exponierten Agent-Servern** — Scan Feb 2026: 93,4% von 42.665 Instanzen verwundbar (offene Ports, Auth aus) | **Opus R10** |
| 10 | **Vector-DB „Persistenz = Disk" glauben** — Suche laeuft im RAM nach Voll-Hydratisierung → Swapping/OOM bei RAM-Erschoepfung | **Opus R10** |
| 11 | **Cloudflare Tunnel fuer persoenliche Daten** — strippt TLS in seinem Netz (sieht Klartext) + 100 MB/Item; stattdessen Caddy-TLS / Pangolin | **Opus R8** |
| 12 | **MCP+OAuth: State nicht an Session gebunden** → CSRF / One-Click Account Takeover; CORS nie `"*"` | **Opus R8** |
| 13 | **Letta fuer direkten externen Such-Zugriff** — geht nicht, nur Agenten-Tool-Calls | **Opus R7** |
| 14 | **Mem0 fuer „Was war wann wahr?"** — kein temporales Fact-Modeling (Fakten ueberschrieben statt versioniert) | **Opus R7** |
| 15 | **supermemory blind als OSS annehmen** — Lizenz/Self-Host-Status widerspruechlich (evtl. closed Backend / enterprise-only) | **Opus R7** |
| 16 | **Standard-Docker als Sicherheitsgrenze** — teilt Host-Kernel; untrusted Agent-Code braucht MicroVM/gVisor | **Opus R10** |
| 17 | **Bandbreiten-Overage stuendlich** — ein Spike (Traffic/DDoS/Sync) loest sofort Overage aus (175 $ bei 2 TB) | **Opus R10** |
| 18 | **CPU-Steal „Ghost Load"** — 20–30% CPU angezeigt, dennoch Timeouts durch Overselling; `steal`-Wert beobachten | **Opus R10** |
| 19 | **Memory-Poisoning** — injizierte Instruktionen persistieren und exfiltrieren spaeter; Provenance + Audit-Logging | online |
| 20 | **Shared Memory ueber Tenant-Grenzen** — Agents lesen still fremde Daten; RLS + `agent_id` | online |
| 21 | **Backup nie per Restore getestet** — „Backup vorhanden" ≠ „wiederherstellbar"; monatlicher Restore-Test (3-2-1-1-0) | **Opus R10** |
| 22 | **Sicherheitsfeatures global deaktivieren** — entfernt die Schicht, die 73% der Angriffe faengt | online |
| 23 | **Ungeprueafte Agent-Skills** — ~jedes 8. Marktplatz-Paket boesartig (Supply-Chain) | **Opus R10** |
| 24 | **Kritische Constraints in den Kontext statt System-Instructions** — Context ueberlebt Compaction nicht | online |
| 25 | **Letta-Migration auf 2-GB-VPS haengt scheinbar** — erste Alembic-Migration dauert 2–3 Min (abwarten) | **Opus R5** |

---

## §11 Offene Fragen / Luecken (vor dem Bauen klaeren)

Durch den Opus-/Firecrawl-Lauf wurden mehrere fruehere Luecken **geschlossen** (Embeddings, Memory-Stack-Retrieval, Cloud-Specs, Docker-Limits) — diese Restpunkte bleiben:

1. **NVMe-Spec-Konflikt** (NEU als oberste Luecke): DE-Seite 30/60/120/240 GB vs. US/LLM-Seite 40/80/160/320 GB. **Vor Kauf auf der Live-Seite der eigenen Region verifizieren.**
2. **Vektor-DB-Performance auf Hostinger KVM** — in keiner Quelle direkt auf Hostinger-Hardware gemessen (die QPS-Zahlen stammen von anderen Setups). Ob NVMe-Throughput bei realen Indizes bremst, ist offen (NVMe ~3.500 MB/s spricht eher dagegen).
3. **Live-Upgrade-Mechanik** (KVM 2 → 4 zur Laufzeit, mit/ohne Reboot, Datenerhalt) — Firecrawl Q5 sagt „no downtime/no data migration", Opus-Quellen bestaetigen die genaue Mechanik nicht. Vor Upgrade Support fragen + Backup.
4. **Cloud-Hosting-Plan-Details** — Produkttyp + grobes RAM-Verhaeltnis jetzt belegt (1,5–2 GB/vCPU), aber Plannamen mit exakten vCPU/RAM/Storage/Preis je Stufe weiterhin nicht beziffert. (Fuer das Projekt ohnehin irrelevant, da kein Root.)
5. **supermemory-Lizenz/Self-Host-Status** — widerspruechlich, am offiziellen Repo verifizieren, bevor man darauf baut.
6. **Milvus minimale RAM-Anforderung** (verteilte Variante) — exakte Minimal-Specs in den Quellen abgeschnitten; offizielle Milvus-Doku pruefen. (Fuer ein Single-User-Gehirn ohnehin Overkill.)
7. **Backup-Tool-Vergleich** (Borg vs. restic vs. rsnapshot) — generische Empfehlung (restic + Backblaze B2), kein Detailvergleich.
8. **API-Auth-Feinheiten** (API-Keys vs. JWT vs. mTLS) — gaengige Branchenpraxis, aber nicht jede Einzelaussage 1:1 belegt; Framework-Doku konsultieren.
