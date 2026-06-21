# Memory-Backends (self-hosted) im Vergleich — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Welches self-hostbare Memory-Backend für "ein Server, viele Clients
> (Claude Code/OpenCode via MCP + eigene Apps via REST) + heterogene persönliche Daten"? Quellen: `extern`
> (Backend-Vergleiche, Mem0/Letta/Zep-Doku 2026). **Löst einen Widerspruch zum bestehenden
> [[../opencode/self-hosted-memory-server]] auf (supermemory-Status).** Schwester: [[referenz-architekturen]], [[datenmodell]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Frage | Antwort |
|-------|---------|
| **Primär-Empfehlung** | **Mem0** — Apache-2.0, self-hostbar, **MCP-nativ**, REST/SDK (Python+JS), Multi-Tenant (user/agent/run/app), größte Community, AWS-Memory-Standard. Erfüllt ALLE Anforderungen unstrittig |
| **Runner-up** | **Zep / Graphiti** — temporaler Knowledge-Graph (beste Benchmarks 94,7 LoCoMo), MCP-nativ; ABER nur **Graphiti** ist OSS, das volle Zep ist Managed |
| **OSS-Alternative** | **Letta** (MemGPT) — Apache-2.0, OS-Tiered-Memory, CLI; ABER **kein MCP** + Agent-Runtime-Lock-in (bringt eigenen Loop mit → passt schlecht zu Claude Code/OpenCode) |
| **supermemory** | **Widerspruch ungeklärt** (siehe §3): A2-Lauf sagt "nicht OSS, kein MCP"; bestehender Bauplan sagt "self-hostbar Single-Binary + MCP". Vor Wahl Lizenz/MCP LIVE verifizieren |
| Vektor-Backend darunter | **Qdrant** (Mem0 unterstützt 20 Backends) |
| Embeddings lokal | FastEmbed (on-device, Privacy) bzw. BGE-M3 (DE/EN) — vor Einsatz testen |
| Ollama-Extraktion lokal? | für KEINES der Tools in den Quellen sauber belegt — vor Datenhoheits-Anspruch prüfen |

---

## 1. Anforderungs-Matrix (`extern`, A2-Lauf)
Für Franks Fall (1 Server, viele Clients, heterogene Personendaten, self-host):

| Anforderung | Mem0 | Letta | Zep | Cognee | Supermemory |
|-------------|------|-------|-----|--------|-------------|
| Self-hostbar | ✅ Apache-2.0 | ✅ Apache-2.0 | ⚠️ nur Graphiti OSS | ✅ OSS | ⚠️ strittig (§3) |
| Native MCP | ✅ | ❌ | ✅ | ❌ | ⚠️ strittig (§3) |
| REST/SDK für Apps | ✅ (Python+JS) | ❌ (Plattform, kein Memory-Layer) | implizit | nicht belegt | ✅ API (Bauplan) |
| Multi-Tenant-Scopes | ✅ user/agent/run/app | nicht beschrieben | User-Profile | nicht beschrieben | User-Profile |
| Reife/Community | ✅ größte (~41-48k Stars, AWS) | ~23k Stars | ~27k Stars | ~17,6k Stars | klein, jung |

## 2. Steckbriefe (`extern`)
- **Mem0:** "drop-in memory layer", zweiphasig (Extraktion + Update, ADD-only), Hybrid aus Vektor+Graph+KV,
  Entity-Linking eingebaut, 20 Vektor-Backends, 3 Voice-Integrationen (ElevenLabs/LiveKit/Pipecat). Benchmarks
  92,5 LoCoMo / 94,4 LongMemEval. **Caveats:** Graph-Memory (Multi-Hop-Beziehungen) im Managed-Pro auf 249 $/Mo
  beschränkt — OSS-Graph ist einfacher; in der Vergangenheit Reife-Probleme gemeldet (7-10 s Latenz, "week-long
  500 errors"); kein natives bi-temporales Modell.
- **Zep/Graphiti:** temporaler KG (Graphiti-Engine), Fakten mit Gültigkeits-Fenstern — stärkste Modellierung
  für sich ändernde Personendaten (Adresse/Job/Beziehungen). Benchmarks 94,7 LoCoMo / 90,2 LongMemEval. **Caveat:**
  nur Graphiti OSS; Latenz/Immediate-Retrieval-Probleme gemeldet (Graph-Bau läuft im Hintergrund).
- **Letta (MemGPT):** OS-Tiered (Core/Recall/Archival), Agent self-edits via Tools, ADE-Debug-Tool. **Caveat:**
  kein MCP, ist ein Agent-Runtime → Claude Code/OpenCode bringen ihren eigenen Loop mit (Lock-in).
- **Cognee:** semantischer KG on-the-fly, Poly-Store, remember/recall/forget/improve. **Caveat:** kein MCP.
- **(mcp-memory-service, basic-memory, txtai):** im A2-Lauf nicht abgedeckt; basic-memory + mcp-memory-service
  sind im bestehenden [[../opencode/self-hosted-memory-server]] §1 als schlanke Alternativen gelistet (SQLite-vec/
  ONNX, MCP) — gut für Claude-Code-only, schwächer für "viele Clients + eigene Apps".

## 3. ⚠️ Der supermemory-Widerspruch — ehrlich aufgelöst
Zwei Recherchen widersprechen sich:
- **A2-Lauf (2026-06-21, Firecrawl):** "supermemory ist **nicht Open Source** und hat **kein natives MCP**"
  (basiert auf einer generischen Vergleichstabelle, Quelle 7).
- **Bestehender Bauplan [[../opencode/self-hosted-memory-server]] (2026-06-19, 6-Researcher-Schwarm):**
  "supermemory **self-hosted = Single-Binary (MIT)**, **MCP-nativ** (`/mcp`), `npx supermemory local`, Port 6767";
  Plugin `opencode-supermemory` verlangt Pro-Plan → remote-MCP-Weg.
- **Blueprint-Lauf (2026-06-21):** supermemory = "Memory-API + Graph, user-profiles + RAG über einen Pool"
  (kein OSS/MCP-Detail).

**Wahrscheinliche Auflösung:** Der dedizierte 6-Researcher-Bauplan ist spezifischer und neuer zu genau diesem
Tool; die generische A2-Vergleichstabelle meint vermutlich supermemorys **Cloud-Produkt** (das ist proprietär)
und nicht den **self-hosted-Fork** (MIT). Die Begriffe "supermemory" decken also zwei Dinge ab: gehostetes
SaaS (proprietär) vs. self-host-Binary (MIT). **Aber: ungeklärt → NICHT blind glauben.** Vor einer Wahl von
supermemory die npm-/Repo-Lizenz UND den MCP-Self-Host-Pfad LIVE verifizieren (Nacharbeit, siehe unten).

**Konsequenz für Frank:** Unabhängig vom supermemory-Streit ist **Mem0 die sichere Wahl** — sein OSS-Status
(Apache-2.0) + MCP + REST + Multi-Scope ist von MEHREREN Quellen unstrittig belegt.

## 4. Empfehlung
1. **Mem0** als zentraler Memory-Server (Apache-2.0, MCP für Claude Code/OpenCode, REST für eigene Apps,
   user/agent/run/app-Scopes für Franks Kategorien). Darunter **Qdrant** als Vektor-Backend.
2. **Zep/Graphiti dazunehmen**, falls temporale Tiefe wichtig wird (Fakten, die sich über Zeit ändern).
3. **supermemory** nur, wenn das einfachere Single-Binary-Self-Hosting den Ausschlag gibt UND §3 vorher
   verifiziert ist.
4. **Letta/Cognee** eher nicht für diesen Multi-Client-Fall (kein MCP / Runtime-Lock-in).

## 5. Embeddings & Ollama-Frage (`extern`)
- **Lokale Embeddings:** FastEmbed (on-device, kein API-Call) für Privacy; BGE-M3 für DE/EN (siehe
  [[orchestrator-und-suche]] §2.6, dort als zu-testen markiert).
- **Lokale LLM-Extraktion (Ollama):** für KEINES der Tools sauber quellenbelegt. Der bestehende Bauplan
  ([[../opencode/self-hosted-memory-server]]) zeigt für supermemory den Ollama-Umweg (`OPENAI_BASE_URL` auf
  lokales Ollama) — analog bei Mem0 prüfen, wenn die Extraktion lokal laufen soll (sonst läuft sie per Default
  über eine externe LLM-API).

## Nacharbeit (offene Verifikation)
- supermemory Lizenz + MCP-Self-Host LIVE prüfen (npm `supermemory`, GitHub-Repo, Doku) → §3 auflösen.
- Mem0 lokale Ollama-Extraktion + OSS-Graph-Fähigkeit (vs. Pro-Tier) verifizieren.
- Ollama-Embedding-/Extraktions-Pfad pro Tool testen (Datenhoheit).

## Quellen (`extern`, 2025-2026)
Backend-Vergleich A2-Lauf (Mem0/Letta/Zep/Cognee/Supermemory-Matrix); Mem0-Blog "State of AI Agent Memory 2026";
Vectorize "Mem0 vs Letta"; EverMind "Best OSS Agent Memory 2026". Ergänzend/teils widersprechend:
`best-practices/opencode/self-hosted-memory-server.md` (supermemory-Self-Host-Bauplan, 2026-06-19).
