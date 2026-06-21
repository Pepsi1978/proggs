# Hostinger als Second-Brain-/KI-Agenten-Server — Best Practices

> Synthese aus 10-Researcher-Recherche 2026-06-21 (minimax-m3:online). Roh-/Volltext + alle Quellen: `hostinger-rohergebnisse-2026-06-21.md`. Faktenstand muss vor Kauf auf hostinger.com verifiziert werden (Preise/Specs schwanken zwischen Quellen).

---

## ⚡ Kurzcheck (Stufe A)

| # | Situation | Sofort-Regel/Fakt |
|---|-----------|-------------------|
| 1 | Welcher Hosting-Typ taugt ueberhaupt? | NUR **KVM VPS**. Shared = keine Root-Kontrolle, Cloud = keine belegten Specs, **Dedicated bietet Hostinger gar nicht an**. ([crazyegg.com](https://www.crazyegg.com/blog/hostinger-review/)) |
| 2 | Lokale grosse LLMs auf Hostinger? | **Nein** — Hostinger hat **keine GPU**. CPU-only. Grosse Modell-Inferenz extern (RunPod/HF/AWS) oder per API. ([fast.io](https://fast.io/resources/best-ai-agent-hosting-platforms/)) |
| 3 | OS-Template waehlen | **Ubuntu 24.04 clean image**. Das **AI-Assistant-Template (Ollama + Open WebUI) MEIDEN** fuer autonome Agenten — das ist ein Chat-UI-Stack, kein Agent-Stack. ([openclawlaunch.com](https://openclawlaunch.com/guides/hermes-agent-hostinger-vps)) |
| 4 | OS spaeter wechseln | **OS-Wechsel LOESCHT ALLE DATEN.** OS-Wahl ist eine Festlegung, kein Experiment. ([hostinger.com/uk/tutorials](https://www.hostinger.com/uk/tutorials/getting-started-with-vps-hosting)) |
| 5 | Welcher KVM-Tier? | **KVM 2 (8 GB) Minimum**, **KVM 4 (16 GB) produktiv**, **KVM 8 (32 GB)** fuer mehrere Instanzen + grosse Memory-DB. ([cybernews.com](https://cybernews.com/vps/best-llm-vps-hosting/)) |
| 6 | Disk-/CPU-Limits beachten | **300 MB/s I/O-Limit** (alle Tiers) + **shared CPU** (kein dedizierter Kern). Kann bei grossen Vektor-Indizes zum Bottleneck werden. ([hostinger.com/support](https://www.hostinger.com/support/6976044-parameters-and-limits-of-hosting-plans-in-hostinger/)) |
| 7 | SSH absichern | **SSH-Key in hPanel VOR der Provisionierung** hinzufuegen, **Root-Passwort-Login danach deaktivieren**. ([openclawlaunch.com](https://openclawlaunch.com/guides/hermes-agent-hostinger-vps)) |
| 8 | API/MCP von aussen erreichbar | **Reverse Proxy (Caddy oder Nginx) + TLS** vorne, Backend nur auf `127.0.0.1:PORT`. Nach aussen nur Port 80/443. ([gelembjuk.com](https://gelembjuk.com/blog/post/securing-remote-mcp-server-ssl-nginx/)) |
| 9 | Mehrere Dienste parallel | **Ja, eine `compose.yaml`** kombiniert Agent + Memory + Vektor-DB + MCP. Man muss sich NICHT auf ein Tool festlegen. ([docker.com](https://www.docker.com/blog/build-ai-agents-with-docker-compose/)) |
| 10 | Vektor-DB-Wahl (Second Brain ≤1 Mio Notizen) | **pgvector** (wenn Postgres laeuft) ODER **Qdrant** (max Filter/Hybrid, $30–50/Mo Self-Host). Beide bei <10 Mio Vektoren mehr als ausreichend. ([ranksquire.com](https://ranksquire.com/2026/02/27/best-self-hosted-vector-database-2026/)) |
| 11 | RAM ist der Hauptfaktor | Fuer Vektor-Index/In-Memory-Suche zaehlt **RAM** am meisten. KVM 4/8 fuer mittlere Indizes, KVM 1/2 nur experimentell. ([cybernews.com](https://cybernews.com/vps/best-llm-vps-hosting/)) |
| 12 | Kosten-Falle Nr. 1 | **Promo- vs. Renewal-Preis**: nach Erstlaufzeit teils 2–3× teurer. Vor Kauf den Renewal-Preis pruefen. ([checkthat.ai](https://checkthat.ai/brands/hostinger/pricing)) |
| 13 | Backups | **Verschluesselt, dedupliziert, off-host, Key-getrennt** — eine kompromittierte VPS darf ihren eigenen Recovery-Pfad nicht loeschen koennen. ([webnestify.cloud](https://webnestify.cloud/insights/cybersecurity-hardening/hermes-agent-deployment/)) |
| 14 | Memory-Sicherheit | **Source-/Provenance-Tag** auf jedem Memory-Eintrag + Audit-Logging (OpenTelemetry). Schuetzt gegen Memory-Poisoning und macht Lecks auffindbar. ([micheallanham.substack.com](https://micheallanham.substack.com/p/mitigating-ai-agent-data-leak-risks)) |

---

## §1 Plattform-Eignung & Plan-Wahl

**Quellen:** [crazyegg.com](https://www.crazyegg.com/blog/hostinger-review/), [hostinger.com/vps-hosting](https://www.hostinger.com/vps-hosting), [hostinger.com/support — Limits](https://www.hostinger.com/support/6976044-parameters-and-limits-of-hosting-plans-in-hostinger/), [cybernews.com — best LLM VPS](https://cybernews.com/vps/best-llm-vps-hosting/)

Hostinger (litauischer Anbieter, RZ u.a. in DE/FR/NL/UK/USA/SG) bietet 2026: **Shared/Web Hosting, Managed WordPress, Cloud Hosting, KVM VPS, Reseller/Agency, Website Builder**. **Kein Dedicated Server** (laut Crazy Egg ausdruecklich nicht im Portfolio). Cybernews kuert Hostinger zur **„Editor's #1 choice for KVM VPS"** und **„best overall for LLM hosting"**.

**Warum nur KVM VPS taugt:**
- **Shared Hosting** ❌ — Ressourcen-Sharing, keine Root-Kontrolle, kein dauerhafter Memory-Server-Betrieb.
- **Cloud Hosting** ⚠️ — Mittelweg zwischen Shared und VPS, aber in **keiner Quelle** mit konkreten CPU/RAM/Storage-Specs belegt → Eignung nicht beurteilbar.
- **KVM VPS** ✅ — isolierte Ressourcen (KVM-Virtualisierung), Root-Zugriff fuer eigenen Stack (Agent-Framework + Vektor-DB), dedizierte IP, woechentliche Backups, Docker-Manager.
- **Grenzen:** keine GPU (→ keine grosse lokale LLM-Inferenz), keine MicroVM/Firecracker-Sandbox auf App-Ebene (Container teilen den Host-Kernel), keine bare-metal-Isolation. Wer das braucht → spezialisierte Anbieter (Northflank, Modal, RunPod, Vertex AI Agent Builder), **nicht** Hostinger.

**KVM-Spezifikationen (offiziell, linear hochskaliert):**

| Parameter | KVM 1 | KVM 2 | KVM 4 | KVM 8 |
|---|---|---|---|---|
| **vCPU** | 1 | 2 | 4 | 8 |
| **RAM** | 4 GB | 8 GB | 16 GB | 32 GB |
| **NVMe** | 50 GB | 100 GB | 200 GB | 400 GB |
| **Bandbreite** | 4 TB | 8 TB | 16 TB | 32 TB |
| **I/O-Durchsatz** | 300 MB/s | 300 MB/s | 300 MB/s | 300 MB/s |
| **CPU-Typ** | shared (Intel Xeon / AMD EPYC, gemischt) | shared | shared | shared |

**LLM-Modellgroessen je Tier** (Cybernews): KVM 1 = 3B-Modelle (Prototyp), KVM 2 = 7–9B quantisiert, KVM 4 = 13–24B, KVM 8 = 24B (mit starker Quantisierung einige 70B-Klasse). Hinweis: gilt fuer CPU-Inferenz — langsam, fuer ein API-basiertes Second Brain meist irrelevant.

**Konkrete Tier-Empfehlung:**
- **KVM 2 (8 GB, 2 vCPU, 100 GB)** — Minimum: Agent-Runtime + leichtgewichtige Memory-DB. Ein OpenClaw-Agent verbraucht idle 200–400 MB, unter Last 0,5–1 GB → ~7 GB „Luft".
- **KVM 4 (16 GB, 4 vCPU, 200 GB)** — produktiv: Agent + Vektor-DB mit groesserem Index.
- **KVM 8 (32 GB)** — mehrere Agent-Instanzen + grosse Memory-DB parallel.

**Preis-Widerspruch zwischen Quellen (vor Kauf verifizieren!):**
- Hostinger.com selbst: KVM 1 ab ~$6,49/Mo (Promo) → $19,49 (regulaer); KVM 8 ~$25,99 → $73,99.
- Checkthat.ai/Crazy Egg (Promo): KVM 1 $4,99, KVM 8 $19,99.
- VPSBenchmarks (vermutlich Renewal): KVM 1 $19,49, KVM 8 $73,99.
- comparevps.com: regulaer $9,99–50,99/Mo, Schnitt ~$25/Mo.
→ Unterschiede = Promo- vs. Renewal-Preis. Nur 12-/24-Monats-Tarife, keine 48-Monats-Option.

---

## §2 OS & Grund-Setup

**Quellen:** [openclawlaunch.com — Hermes-Guide](https://openclawlaunch.com/guides/hermes-agent-hostinger-vps), [hostaccent.com — Distro-Vergleich](https://www.hostaccent.com/blog/best-linux-distro-for-vps-hosting-2026), [hostinger.com — verfuegbare OS](https://www.hostinger.com/support/1583571-what-are-the-available-operating-systems-for-vps-at-hostinger/), [hostinger.com/uk/tutorials](https://www.hostinger.com/uk/tutorials/getting-started-with-vps-hosting)

**Empfehlung (belastbarste Quelle, Hermes-Agent-Guide): Ubuntu 24.04 als Clean-OS-Image.**
- **AI-Assistant-Template MEIDEN** fuer autonome Agenten: Es kommt mit Ollama + Open WebUI vorinstalliert — nuetzlich fuer ein Chat-UI, „aber nicht nuetzlich, wenn man einen plattformuebergreifenden autonomen Agenten will". Fuer einen Agenten braucht man Docker auf einem sauberen System.
- **Ubuntu-Vorteil:** breiteste Drittanbieter-Kompatibilitaet — Installer und Control Panels „testen typischerweise zuerst auf Ubuntu". Pfad des geringsten Widerstands fuer neue Agent-/Memory-Frameworks.
- **Debian-Alternative:** geringerer RAM-Footprint (nur bei knappem RAM 1–2 GB messbar), kein Snap, maximale Stabilitaet — sinnvoll, wenn unerwartetes Verhalten inakzeptabel ist.
- **AlmaLinux/CentOS:** als CentOS-Nachfolger LTS-faehig, aber **kein Quellenbeleg** fuer einen KI-/Agent-Vorteil.

**Verfuegbare Distros bei Hostinger** (Support-Liste + externe Reviews, teils widerspruechlich): Alpine (Standard), CentOS 9/10 Stream, CloudLinux, Debian 11/12/13, Fedora Cloud 42, Kali, openSUSE Leap, Rocky 8/9/10 — plus laut externen Reviews Ubuntu, AlmaLinux, Arch (insgesamt „elf Distributionen", 70+ Templates inkl. Panels/Apps). Image-Kategorien: (1) Operating System = minimal, (2) Control Panel, (3) Application (OS + App), (4) Container (Docker).

**Grund-Setup-Pflicht:**
- **SSH-Key in hPanel VOR der Provisionierung** hinzufuegen; **Passwort-Login fuer Root danach deaktivieren**.
- **OS-Wechsel loescht alle Daten** → OS-Wahl ist eine Festlegung. Vor jedem Wechsel Backup.
- Update-Befehle: Ubuntu/Debian `apt update && apt upgrade`; AlmaLinux/Rocky/CentOS `dnf update -y`.

---

## §3 1-Klick-Templates

**Quellen:** [hostinger.com/at/vps](https://www.hostinger.com/at/vps), [hostinger.com/applications/open-webui](https://www.hostinger.com/applications/open-webui), [hostinger.com/applications/agent-zero](https://www.hostinger.com/applications/agent-zero), [hostinger.com/tutorials/n8n-ollama-integration](https://www.hostinger.com/tutorials/n8n-ollama-integration)

| Anwendung | Als 1-Klick-Template belegt? |
|---|---|
| **Docker** | ✅ ja (als „Beliebt" markiert) |
| **n8n** | ✅ ja |
| **Open WebUI** | ✅ ja (1-Klick-Docker-Template) |
| **Claude Code** | ✅ gelistet auf VPS-Seite |
| **Codex CLI** | ✅ gelistet auf VPS-Seite |
| **Agent Zero** | ✅ 1-Klick-Docker-Vorlage |
| **OpenClaw** | ✅ 1-Klick (€5,49/Mo, Integrationen WhatsApp/Telegram/Slack/Discord) |
| **Hermes Agent** | ✅ 1-Klick via Docker Manager |
| **Ollama** | ⚠️ NICHT als eigenes 1-Klick-Template — nur ueber Docker-Tutorial |
| **Coolify** | ❌ nicht belegt (nur in gesponsertem Drittvideo) |
| **CapRover** | ❌ in keiner Quelle erwaehnt |
| **eigenes Ollama-Template** | ❌ nicht belegt |

**Mehrere gleichzeitig:** Quellen beschreiben **nicht** das parallele Auswaehlen mehrerer Templates bei der Ersteinrichtung, sondern das Muster **„ein Template installieren → weitere Apps manuell ergaenzen"** (z.B. n8n + Ollama im selben Docker-Container).

**Template wechseln:** Es gibt einen eigenen Support-Artikel dafuer, ist aber im Kern eine **Neuinstallation des OS** → typischerweise destruktiv (Daten weg). Vor Wechsel verifizieren/Backup.

---

## §4 KI-Agent-Frameworks

**Quellen:** [hostinger.com/openclaw](https://www.hostinger.com/openclaw), [hostinger.com/tutorials/hermes-agent-vs-openclaw](https://www.hostinger.com/ph/tutorials/hermes-agent-vs-openclaw), [mindstudio.ai](https://www.mindstudio.ai/blog/hermes-agent-vs-openclaw-self-hosted-ai-agent-comparison), [openclawlaunch.com](https://openclawlaunch.com/hermes-alternatives)

**1-Klick bei Hostinger verfuegbar:** OpenClaw, Hermes Agent, Agent Zero (alle ueber Docker Application Catalog — App auswaehlen, Umgebungsvariablen/API-Keys eintragen, deployen).

- **Hermes Agent:** Software frei (MIT-Lizenz), Betrieb $5–80/Mo je nach Modell. Unterstuetzt **OpenAI Codex OAuth** → bestehendes ChatGPT-Abo nutzbar statt separater API-Rechnung. Setup ~30 Min. Pro Agent eigene `.env`, eigene Keys, eigener Speicher (Credentials nicht geteilt); Secrets via `hermes config set GITHUB_TOKEN [token]`.
- **OpenClaw:** 1-Klick €5,49/Mo, Multi-Channel (Telegram, Discord, WhatsApp, WeChat, Web). Community-Hinweis: Konfigurationsprobleme beim Modellwechsel und bei Telegram-Integration berichtet.

**Selbst aufsetzen (kein Hostinger-1-Klick belegt):** Letta, CrewAI, AutoGPT — in den Quellen keine Hostinger-spezifische 1-Klick-Loesung. Plausibel via Docker auf normalem VPS, aber **nicht quellenbelegt**.

**1-Klick vs. normaler VPS:** 1-Klick uebernimmt Infrastruktur, haelt die Instanz auf der aktuellen stabilen Version und fuegt Sicherheits-Layer hinzu. Normaler VPS = volle Kontrolle, aber Setup/Updates/Sicherheit in Eigenverantwortung.

---

## §5 Second-Brain/Memory selbst bauen (Vektor-DBs)

**Quellen:** [ranksquire.com](https://ranksquire.com/2026/02/27/best-self-hosted-vector-database-2026/), [medium.com/@alxkm](https://medium.com/@alxkm/vector-databases-explained-the-missing-piece-in-your-ai-stack-ddc25be57232), [digitalapplied.com](https://www.digitalapplied.com/blog/vector-databases-for-ai-agents-pinecone-qdrant-2026), [abhid.substack.com](https://abhid.substack.com/p/i-built-an-ai-powered-second-brain)

**Anforderungsprofil Second Brain:** typisch 10k–1 Mio. Notizen → <10 Mio. Vektoren, read-heavy, sub-100ms Latenz im Single-User-Betrieb. Bei <10 Mio. Vektoren sind **alle vier Kandidaten ausreichend** — Self-Host schliesst managed SaaS (Pinecone) praktisch aus.

| DB | Staerken | Schwaechen / Hinweise | Self-Host-Kosten |
|----|----------|-----------------------|------------------|
| **pgvector** | Ein System (ein Backup, ein Monitoring), ACID/Transaktionen mit Metadaten, HNSW+IVF, real erprobtes Second-Brain-Beispiel. 470 qps @ 99% Recall auf 50 Mio Vektoren | Bei sehr hoher Concurrency bis 10× weniger QPS als Milvus | $0 (wenn Postgres laeuft) |
| **Qdrant** | Rust, Latenz-Edge unter OSS, Filter waehrend (nicht nach) der Suche, dense+sparse+Metadaten in einer Query | — | $30–50/Mo (1 Mio Vektoren) |
| **Weaviate** | Modulare Architektur (Embedding-Module OpenAI/Cohere/HF einsteckbar), natives Hybrid (BM25+Vector), exzellente Doku | „Performance gut, aber nicht die absolut beste fuer reine Vektor-Ops" | RAM-abhaengig (nicht beziffert) |
| **Milvus** | Skaliert auf >1 Mrd Vektoren, write-heavy stark, breite Index-Unterstuetzung | Praxisstimme: „over complicated"; mehr Ops-Aufwand (Distributed/K8s) | „similar" zu Qdrant, hoeherer Aufwand |

**Eignungs-Baender:** <10 Mio = alle vier; 10 Mio–1 Mrd = Qdrant/Weaviate/Milvus self-host; >1 Mrd = Milvus distributed.

**Architektur-Schnellauswahl:** Hybrid (Vector+Keyword+Filter) → Weaviate/Qdrant; Write-heavy → Milvus/Qdrant; Read-heavy → pgvector; ACID → pgvector/evtl. Weaviate.

**Embeddings (duenne Quellenlage):** Open-Stack-Empfehlung Qwen (Embeddings/multilingual), Gemma 4 (kleine effiziente Modelle), Llama 4 Scout/Maverick. Distanzmetriken: Cosine/Euclidean/Manhattan. **Luecke:** kein Quellen-Vergleich konkreter Embedding-Modelle (bge-m3, nomic-embed, arctic-embed, mxbai) oder Dimensions-Empfehlungen.

**Minimal-Architektur (synthetisiert):** Markdown/Web/PDF → Ingestion-Skript (Cron/manuell, Chunking + Embedding + Metadaten) → Vektor-DB → Query-Pipeline (optional Hybrid-Retrieval, Pre-/Post-Filter, Reranker) → LLM (lokal Ollama/llama.cpp oder API) → Antwort + Quellen → UI. Die einzige in den Quellen **konkret durchgebaute** Variante nutzt **pgvector**.

**Empfehlung Second Brain (≤1 Mio Notizen, Single-User):** **pgvector** wenn Postgres ohnehin laeuft (ein System, ACID, kostenlos), sonst **Qdrant** fuer maximale Filter-/Hybrid-Power auf kleinem VPS.

---

## §6 Self-hosted Memory-Stacks (fertige Layer)

**Quellen:** [mem0.ai/blog/self-host-mem0-docker](https://mem0.ai/blog/self-host-mem0-docker), [github.com/letta-ai/letta](https://github.com/letta-ai/letta), [valkey.io](https://valkey.io/blog/ai-agent-memory-with-valkey-and-mem0/), [redis.io](https://redis.io/blog/build-smarter-ai-agents-manage-short-term-and-long-term-memory-with-redis/), [supermemory.ai](https://supermemory.ai/blog/best-memory-apis-stateful-ai-agents/)

> **Wichtige Einschraenkung:** Researcher 7 (gezielt zu Such-/Retrieval-Werkzeugen von Mem0/Letta/Zep/supermemory) lieferte **keine ausgewertete Substanz** — nur eine Liste von 16 Quellen, die Web-Searches blieben unausgewertet. Die folgenden Punkte stammen aus anderen Researchern (6, 9, 10) und sind entsprechend duenn.

- **Mem0:** komplette Self-Host-Compose-Anleitung (API + Postgres+pgvector + Neo4j). Empfiehlt Start mit kleiner Instanz (t3.medium, 4 GB RAM). Memory-Layer kann Token-Kosten „um bis zu 90% senken". Sechs Spar-Techniken mit Kennzahlen (Importance-Based Eviction mit Ebbinghaus-Decay −59%, Token Budgeting −75%, Hierarchical Summarization −59%).
- **Letta/MemGPT:** Open Source (GitHub), tiered Memory (Core + Archival), als Memory-Framework etabliert — aber **keine ausgewerteten Retrieval-Details** in dieser Recherche.
- **Zep:** in der Recherche nur als Quellenname (Graphiti/Graph-Memory) — **nicht ausgewertet**.
- **supermemory:** nur als Quellenname — **nicht ausgewertet**.
- **Redis / Valkey Agent Memory Server:** Open-Source-Container mit REST + MCP, Vektor-Suche, In-Memory-Storage, flexible Datenstrukturen, Eviction-Policies — laeuft per MCP neben Agents.
- **Engram:** Memory-Server mit lokalen Embeddings (MiniLM 384-dim, in-process, kein API-Key).

**→ Architektur-Entscheidung offen:** fertiger Memory-Layer (Mem0/Letta/Redis) vs. selbst gebaute Vektor-DB-Pipeline (§5). Wegen duenner Quellenlage vor Festlegung gezielt nachrecherchieren (siehe §11).

---

## §7 MCP-Server & API von aussen sicher erreichbar

**Quellen:** [gelembjuk.com](https://gelembjuk.com/blog/post/securing-remote-mcp-server-ssl-nginx/), [fast.io/resources/mcp-server-proxy](https://fast.io/resources/mcp-server-proxy/), [medium.com/@richardhightower](https://medium.com/@richardhightower/securing-mcp-from-vulnerable-to-fortified-building-secure-http-based-ai-integrations-b706b0281e73), [insightfinder.com](https://insightfinder.com/blog/mcp-server-security-guide/)

**Grundarchitektur:** Reverse Proxy vorne, MCP-Server/API hinten auf lokalem Port, **TLS-Termination am Proxy**. TLS NICHT direkt im FastMCP/Uvicorn-Server implementieren. „Front-Door"-Schichten: outer firewall → auth/rate-limiting → HTTPS → Reverse Proxy → Monitoring.

**Caddy vs. Nginx:**

| Kriterium | Caddy | Nginx |
|---|---|---|
| TLS automatisch | ✅ Standard (ACME + Rotation) | ❌ Certbot noetig |
| Konfiguration | sehr kompakt | granular |
| Streaming/MCP | Defaults passen | mehr Kontrolle |

→ **Caddy fuer Einfachheit** (TLS out-of-the-box), **Nginx fuer bestehende Infrastruktur / feines Traffic-Management**.

Caddy-Minimalbeispiel:
```
example.com {
    reverse_proxy localhost:8000
}
```

**Authentifizierung:**
- Caddy `basic_auth` direkt im Caddyfile (einfach).
- OAuth/JWT-Pattern (typisch fuer MCP): OAuth-Server stellt JWT aus, Proxy terminiert TLS, MCP-Server verifiziert JWT-Signatur + prueft Session-Cache (Redis).
- Mutual TLS / Client-Certs moeglich.
- **Cloud Security Alliance Best Practice:** jeder Tool-Aufruf zusaetzlich vom Agenten signiert (Public Key beim MCP-Server registriert) — Message-Level-Signing gegen MITM **nach** TLS-Termination.

**Ports:** nach aussen NUR 80 (→443-Redirect) und 443. Backend NUR auf `127.0.0.1:PORT` binden, **niemals public**. Subdomain-Pattern: `example.com` Hauptapp, `api.example.com` API.

**Absicherung:** Rate-Limiting (Caddy `rate_limit / 100 1m`); Intrusion Prevention (CrowdSec modern / Fail2Ban klassisch); JSON-Access-Logging.

---

## §8 Mehrere Dienste gleichzeitig (Docker Compose)

**Quellen:** [docker.com/blog/build-ai-agents-with-docker-compose](https://www.docker.com/blog/build-ai-agents-with-docker-compose/), [mem0.ai/blog/self-host-mem0-docker](https://mem0.ai/blog/self-host-mem0-docker), [deployhq.com](https://www.deployhq.com/blog/deploy-first-ai-agent-vps-docker), [stackoverflow.com (deploy-Limits)](https://stackoverflow.com/questions/42345235/how-to-specify-memory-cpu-limit-in-docker-compose-version-3)

**Man muss sich NICHT festlegen.** Agent-Server + Memory-Server + LLM-Inference + MCP-Tools lassen sich in **einer `compose.yaml`** kombinieren (`docker compose up`). Praxisbeispiel: Agent + Memory + Postgres + Redis + Minio + Traefik + Ollama + Frontend auf einem 32-GB/8-vCPU-VPS.

**Zwei kritische Engpaesse:**

1. **Port-Trennung** — jeder Container braucht einen eindeutigen Host-Port. Fuer mehrere Web-Dienste hinter 443 → Reverse Proxy (Traefik/Nginx/Caddy) mit Let's Encrypt. Backend nur lokal binden: `ports: "127.0.0.1:8000:8000"`.

2. **Ressourcen-Limits (Stolperfalle!)** — der moderne `deploy.resources.limits`-Key greift **nur bei `docker stack deploy` (Swarm/K8s), NICHT bei plain `docker compose up`**. Compose zeigt dann nur eine Warnung und ignoriert die Limits. Legacy-Felder (`mem_limit`, `cpuset`) funktionieren bei plain Compose, werden aber bei v3 teils ignoriert — ein verbindlicher Workaround ist in den Quellen **nicht eindeutig** dokumentiert. **Reale Doku-Luecke.** Praxis-Regel: bei lokalen Inference-Services zusaetzlich Memory-Reservierungen setzen, damit ein Modell-Ladevorgang nicht den ganzen Host-RAM frisst.

**Compose-Bausteine:** Agent-Runtime (Ollama / API-Container) · Memory-Server (Redis `agent-memory-server`, REST+MCP) · Memory-Stack (Mem0: API + Postgres+pgvector + Neo4j) · MCP-Tools (mehrere MCP-Server nebeneinander, Volume-Mounts + Docker Secrets) · lokale Embeddings ohne API-Key (Engram, MiniLM 384-dim).

**Dimensionierung:** Mem0 nennt 4 GB RAM Untergrenze (Neo4j + pgvector + API); andere Quellen gehen erst ab 32 GB/8 vCPU produktiv. Eine verbindliche Untergrenze pro Service-Paarung (Agent + Memory) ist **nicht belegt**.

---

## §9 Best Practices & Sicherheit

**Quellen:** [digitalapplied.com — AI Agent Security 2025](https://www.digitalapplied.com/blog/ai-agent-security-best-practices-2025), [webnestify.cloud](https://webnestify.cloud/insights/cybersecurity-hardening/hermes-agent-deployment/), [micheallanham.substack.com](https://micheallanham.substack.com/p/mitigating-ai-agent-data-leak-risks), [dev.to/bobrenze](https://dev.to/bobrenze/why-ai-agent-memory-systems-fail-in-production-and-how-i-fixed-mine-141d), [github.com/supabase](https://github.com/orgs/supabase/discussions/39820)

- **Schlanker Host:** minimaler OS-Footprint (clean image, headless) → kleinere Angriffsflaeche, weniger Patch-Last, mehr CPU/RAM fuer die Dienste.
- **Produktion in Produktion:** ein Produktiv-Agent gehoert auf einen VPS, **nicht auf den Entwickler-Laptop**.
- **Memory-Layer von Anfang an:** verwandelt ein stateless LLM in einen Assistenten mit Gedaechtnis — kann Token-Kosten bis 90% senken, Antworten <2s.
- **Audit-Logging & Observability:** OpenTelemetry-konform — Prompts (sanitisiert), Responses, Tool-Calls, Entscheidungspfade loggen; mit SIEM verbinden. Ohne Logging keine Forensik bei Memory-Poisoning/Exfiltration.
- **Tiered Memory mit Provenance:** jeder Memory-Eintrag bekommt ein **`source`-Feld** → Memory wird vom Black-Box zum durchsuchbaren Log. Groesse/Alter/Qualitaet monitoren.
- **Checkpointing:** vor jeder Kontext-Kompaktierung checkpointen; **proaktiv** zusammenfassen, nicht reaktiv; kritische Constraints in **System-Instructions**, nicht in den Kontext (Context ueberlebt Compaction nicht).
- **Multi-Tenant-Isolation auf DB-Ebene:** jeder Agent sieht nur eigene Daten (z.B. Supabase RLS + JWT mit `agent_id`). **Vergessene RLS-Policies sind unsichtbare Fehler.**
- **Backups im Security-Modell:** verschluesselt, dedupliziert, off-host, Key-getrennt (Borg-Disziplin) — eine kompromittierte VPS darf ihren Recovery-Pfad nicht loeschen koennen.
- **Sicherheitsfeatures aktiv lassen:** bei blockierten Tasks Prompts verfeinern/whitelisten statt Schutz global abzuschalten (Deaktivieren entfernt die Schicht, die 73% der Angriffe faengt).
- **Budget-VPS-Falle:** Budget-Hoster liefern keine WAF/IDS — die muss man selbst schichten.

---

## §10 Fallen & was man NICHT tun sollte (Bug-Teil)

| # | Falle | Quelle |
|---|-------|--------|
| 1 | **OS-Wechsel loescht ALLE Daten** — OS-Wahl als Experiment behandeln | [hostinger.com/uk/tutorials](https://www.hostinger.com/uk/tutorials/getting-started-with-vps-hosting) |
| 2 | **AI-Assistant-Template (Ollama + Open WebUI)** fuer autonome Agenten nutzen — das ist ein Chat-UI-Stack, kein Agent-Stack | [openclawlaunch.com](https://openclawlaunch.com/guides/hermes-agent-hostinger-vps) |
| 3 | **Grosse lokale LLMs erwarten** — Hostinger hat **keine GPU**, nur CPU; grosse Modelle extern/per API | [fast.io](https://fast.io/resources/best-ai-agent-hosting-platforms/) |
| 4 | **300 MB/s I/O-Limit** ignorieren — kann bei sehr grossen Vektor-Indizes zum Bottleneck werden (nicht gemessen, aber plausibel) | [hostinger.com/support](https://www.hostinger.com/support/6976044-parameters-and-limits-of-hosting-plans-in-hostinger/) |
| 5 | **Shared CPU** vergessen — kein dedizierter Kern, rechenintensive Embedding-Generierung stark eingeschraenkt | [vpsbenchmarks.com](https://www.vpsbenchmarks.com/hosters/hostinger/plans/kvm-2) |
| 6 | **Promo- vs. Renewal-Preisfalle** — nach Erstlaufzeit teils 2–3× teurer; nur 12-/24-Monats-Tarife | [checkthat.ai](https://checkthat.ai/brands/hostinger/pricing) |
| 7 | **`deploy.resources.limits` bei plain `docker compose up`** — wird ignoriert (nur Swarm/K8s), RAM-Limits greifen nicht wie erwartet | [stackoverflow.com](https://stackoverflow.com/questions/42345235/how-to-specify-memory-cpu-limit-in-docker-compose-version-3) |
| 8 | **Memory-Poisoning** — injizierte Instruktionen persistieren im Session-Memory und exfiltrieren spaeter (Unit-42-Demo auf Bedrock Agents). Gegenmittel: Provenance + Audit-Logging | [medium.com](https://medium.com/data-unlocked/the-memory-problem-in-ai-agents-is-half-solved-heres-the-other-half-ebbf218ae4d5) |
| 9 | **Shared Memory ueber Tenant-Grenzen** — Agents lesen still fremde Daten (Asana-„Oops"-Fall) | [towardsdatascience.com](https://towardsdatascience.com/the-mcp-security-survival-guide-best-practices-pitfalls-and-real-world-lessons/) |
| 10 | **Sicherheitsfeatures global deaktivieren** — entfernt die Schicht, die 73% der Angriffe faengt | [digitalapplied.com](https://www.digitalapplied.com/blog/ai-agent-security-best-practices-2025) |
| 11 | **Kein Audit-Logging** — keine Forensik bei Incidents, keine Erkennung von Poisoning/Exfiltration | [digitalapplied.com](https://www.digitalapplied.com/blog/ai-agent-security-best-practices-2025) |
| 12 | **AI-generierten Code/Tool-Calls ungeprueft uebernehmen** | [digitalapplied.com](https://www.digitalapplied.com/blog/ai-agent-security-best-practices-2025) |
| 13 | **Over-Provisioning** — ~28% Cloud-Spend gehen an idle/ueberdimensionierte Ressourcen; Agents laufen in Bursts | [branch8.com](https://branch8.com/posts/ai-agent-vps-deployment-cost-optimization-guide) |
| 14 | **Backups on-host/unverschluesselt** — kompromittierte VPS loescht ihren eigenen Recovery-Pfad | [webnestify.cloud](https://webnestify.cloud/insights/cybersecurity-hardening/hermes-agent-deployment/) |
| 15 | **„Infinite Memory" als Feature** — in Enterprise-Kontext ein Bug; gutes Memory weiss, wann es loslassen muss | [micheallanham.substack.com](https://micheallanham.substack.com/p/mitigating-ai-agent-data-leak-risks) |
| 16 | **Kritische Constraints in den Kontext statt System-Instructions** — Context ueberlebt Compaction nicht | [dev.to/bobrenze](https://dev.to/bobrenze/why-ai-agent-memory-systems-fail-in-production-and-how-i-fixed-mine-141d) |
| 17 | **Agent auf dem Laptop** + **API-Keys auf dem Laptop** — haeufige Key-Leak-Quelle | [lobsterfarm.ai](https://www.lobsterfarm.ai/guides/5-mistakes-self-hosting-ai/) |

---

## §11 Offene Fragen / Luecken in den Quellen (vor dem Bauen klaeren)

Diese Punkte konnte die Recherche **nicht belastbar** beantworten — vor der Architektur-Festlegung gezielt nachrecherchieren oder live testen:

1. **Cloud-Hosting-Specs** (RAM/CPU/Storage pro Tier) — von Hostinger nicht oeffentlich beziffert; Eignung nicht beurteilbar.
2. **Vektor-DB-Performance auf Hostinger KVM** — in keiner Quelle gemessen. Ob das 300-MB/s-I/O-Limit bei realen Indizes bremst, ist offen.
3. **Live-Upgrade des KVM-Tiers ohne Neuinstallation** (KVM 2 → KVM 4 zur Laufzeit?) — in keiner Quelle behandelt. Horizontale Skalierung (Cluster) ebenfalls nicht.
4. **Root-Zugang woertlich bestaetigt** — nur „self-managed/unmanaged Linux" impliziert ihn; keine direkte Quelle (bei unmanaged KVM aber branchenueblich).
5. **VPS-Sizing-Rezepte** (CPU/RAM/Swap/Disk pro Agent + Session) — kein Artikel liefert ein konkretes Modell.
6. **Backup-Frequenz / RPO / RTO / Tool-Vergleich** (Borg vs. Restic vs. rsnapshot) — nur generische Borg-Empfehlung.
7. **Storage-IOPS/Throughput** fuer Memory-Backends (Redis/Valkey AOF/RDB) — keine Disk-Benchmarks.
8. **Embedding-Modell-Empfehlung** je Sprachraum/Hardware (bge-m3, nomic-embed, arctic-embed, mxbai) + Dimensions-Wahl (384/768/1024/1536) — keine Quelle.
9. **Memory-Stack-Retrieval-Details** (Mem0/Letta/Zep/supermemory) — Researcher 7 blieb unausgewertet; gezielter Re-Run noetig.
10. **RAM-/Disk-Footprint pro 1 Mio Vektoren** @768/1536-dim fuer alle vier DBs — einzige belegte Zahl: pgvector 470 qps @99% Recall auf 50 Mio Vektoren.
11. **Hard-Memory-Limits unter plain `docker compose up`** — kein verbindlicher Workaround dokumentiert (reale Doku-Luecke).
