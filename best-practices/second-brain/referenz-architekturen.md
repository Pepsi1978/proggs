# Referenz-Architekturen & Muster — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Die Architektur-Sprache + bewährten Muster + Anti-Patterns für agentische
> Memory-Systeme. Quellen: `extern` (Mem0 "State of AI Agent Memory 2026", Graphlit, JobsByCulture
> Engineering-Guide 2026, EverMind, Wasowski — alle 05-06/2026). Schwester: [[memory-backends]], [[datenmodell]], [[../../README]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Memory-Grundtypen | **working · episodic · semantic · procedural** (aus der Kognitionswissenschaft) |
| Wichtigste Trennung | **Memory ≠ Context.** Memory = dauerhaftes Substrat; Context = die minimale nützliche Auswahl, die das Modell JETZT sieht. Context Engineering ist das eigentliche Produkt |
| Bestes Grundmuster | **OS-Tiered (MemGPT/Letta):** Core (RAM, immer im Kontext, <1k Token) + Recall (Cache, suchbar) + Archival (Disk, Vektor, on-demand) — der Agent swappt selbst |
| Genau Franks Idee | Tiered = Kern lokal immer dabei + Rest on-demand vom Server. Bestätigt durch die Quellen |
| Anti-Pattern #1 | **"Alles in den Kontext stopfen"** — teuer, "lost in the middle", Context Rot. NICHT großen Kontext erzwingen |
| Anti-Pattern #2 | "Matched score" statt "last updated" gewichten — alter Fakt matcht semantisch, schadet aber. **Recency-Weighting** nutzen |
| Anti-Pattern #3 | Löschen ("forget me") als Query behandeln — ist eine **Architekturfrage** (per-User-Key, siehe [[server-infrastruktur]]) |
| Klar OSS-Kandidaten | Mem0 (Apache-2.0), Letta (Apache-2.0); Zep nur via Graphiti OSS — siehe [[memory-backends]] |
| Retrieval | Multi-Signal: semantisch + Keyword + Entity parallel, Scores fusionieren ([[orchestrator-und-suche]]) |

---

## 1. Architektur-Layer (`extern`)
### 1.1 Vier Memory-Typen
working (aktueller Prompt + letzte Nachrichten) · episodic (zeitgestempelte Ereignisse) · semantic
(extrahierte Fakten/Präferenzen) · procedural (wie der Agent arbeitet — System-Prompt + Heuristiken).
Graphlit erweitert um session/entity/temporal-graph/operational-context — warnt aber, dass "Memory" dadurch
mehrere inkompatible Abstraktionen meint.

### 1.2 Memory ≠ Context (Kernprinzip)
| Layer | Frage | Inhalt |
|-------|-------|--------|
| **Memory** | Was soll persistieren? | Präferenzen, Fakten, Events, Prozeduren |
| **Retrieval** | Was soll gefunden werden? | Dokumente, Chunks, Entitäten, Graph-Pfade |
| **Context** | Was sieht das Modell jetzt? | Minimale nützliche Auswahl aus Instructions + Evidence + Tools |
"Memory ist das dauerhafte Substrat. Context Engineering macht daraus einen nützlichen Prompt." → exakt
Franks Drei-Schichten-Denken (Kern + Profil + On-Demand).

## 2. Referenz-Architekturen (belegt)
### 2.1 OS-Tiered (MemGPT → Letta) — am ausführlichsten belegt
"Behandle das LLM wie ein Betriebssystem." Drei Tiers:
- **Core Memory** — immer im Kontext (RAM): User-/Agent-Persona, aktueller Task. Klein, heiß.
- **Recall Memory** — Konversationshistorie (Cache): suchbar, swap-in/out.
- **Archival Memory** — externer Vektor-Store (Disk): massiv, billig, on-demand via Embedding-Suche.
Der Agent steuert den Swap via Tools (`memory.insert/search/swap`). MemGPT-Benchmark: 93,4 % Deep Memory Retrieval.
**→ Für Frank: Das ist die Blaupause.** Core = lokaler Kern, Archival = Server-Memory, Agent = Dirigent.

### 2.2 Multi-Signal Retrieval (Mem0)
Drei Scoring-Pässe parallel (semantisch + Keyword + Entity), fusioniert — schlägt jedes Einzelsignal.
### 2.3 Temporal Knowledge Graph (Zep/Graphiti)
modelliert Entitäten, Beziehungen, Fakten, Episoden + **Gültigkeits-Fenster**; Trend zu Built-in Entity-Linking.
### 2.4 6-Komponenten-Blueprint "zum Kopieren" (JobsByCulture, chat-basiert)
1. **Core memory:** 2-4 editierbare Blöcke (<1k Token), immer im Kontext.
2. **Working buffer:** letzte 20-40 Messages, bei Schwelle summarisiert → Summary nach Episodic.
3. **Episodic store:** Vektor-Events + Timestamps/Metadata, Retrieval = Semantik + Recency-Weighting.
4. **Semantic store:** extrahierte Fakten mit **Provenance** (welche Episode) + **Confidence**, Update per Background-Process.
5. **Procedural memory:** System-Prompt mit append-only Heuristiken, Self-Edit via Tool + **Rollback-Log**.
6. **Forgetting policy:** Episodic-TTL (z. B. 90 Tage).

## 3. Bewährte Design-Patterns (`extern`)
Tiered Memory · Multi-Signal Retrieval · Built-in Entity-Linking · Single-Pass/ADD-only Extraction · Temporal
Validity Windows · Self-Editing Memory (+ Rollback) · Provenance-Anker (+ Confidence) · Recency-Weighting ·
**Context Engineering vor Memory-Größe** · Async Memory Writes · User-ID aus App-Auth · Hybrid Vector+Graph ·
Background Memory Manager.

## 4. Anti-Patterns (`extern`, JobsByCulture)
| Anti-Pattern | Warum schlecht |
|--------------|----------------|
| "Alles in den Kontext stopfen" | teuer; "lost in the middle"; Agenten laufen Monate → >1M Token (Context Rot) |
| Memory-Hygiene ignorieren | aggressives Schreiben → aufgeblähte, widersprüchliche, veraltete Fakten → Retrieval verfällt |
| "Matched score" statt "last updated" | 18 Monate alter Fakt matcht semantisch, schadet aber |
| Privacy = Retrieval verwechseln | Löschung ist Architekturfrage (per-User-Key), nicht Query-Problem (DSGVO) |
| Embedding-Model-Drift | Re-Embedding ohne Re-Index mischt Modell-Vektoren → "zerstört Vektorsuche leise" |
| Latenz im Tool-Call-Loop | 3-5 Memory-Calls/Turn × Round-Trip = reale Latenz → aggressiv cachen |
| Eval mit statischen Prompts | Memory-Agent lässt sich nicht per Prompt-Reset zurückspielen → Session-Szenarien bauen |
| Vendor-Lock-in | Memory-Layer an ein Framework gebunden = wird nicht breit adoptiert |
| Chat-History = Memory | Alt: Transkripte. Neu: Fakten/Präferenzen/Entitäten/Episoden/Prozeduren/Pläne getrennt |

## 5. Frameworks (belegt, mit Benchmarks/Lizenz) (`extern`)
| Framework | Kern | Lizenz / Reife | Benchmark |
|-----------|------|----------------|-----------|
| **Mem0** | universeller Memory-Layer, 20 Vektor-Backends, 3 Voice-Integrationen | Apache-2.0, ~41-48k Stars, AWS-Standard | 92,5 LoCoMo / 94,4 LongMemEval |
| **Letta** (MemGPT) | OS-Tiered, Memory-Blocks, CLI "Letta Code" | Apache-2.0, ~23k Stars | 93,4 Deep Memory Retrieval |
| **Zep + Graphiti** | Temporal KG, Validity-Dates | nur Graphiti OSS, ~27k Stars | 94,7 LoCoMo / 90,2 LongMemEval |
| **LangMem** | LangGraph-nativ, hot-path + background | MIT, ~1,5k Stars | — |
| **Cognee** | "memory control plane", remember/recall/forget/improve, Graph+Vector | OSS, ~17,6k Stars | — |
| **Supermemory** | Memory-API + Graph, user-profiles + RAG über einen Pool | (OSS-Status strittig — [[memory-backends]]) | — |
| **ReMe** (nicht Reor!) | file-based + Vektor, editierbare Markdown-Memory, BM25+Vektor | ~3k Stars | — |

## 6. Vektor-Stores (`extern`)
Self-host/OSS: **Qdrant** (3 ms / 99,2 % Recall, 2026-Benchmark), Chroma, Weaviate, Milvus, PGVector, Redis,
Elasticsearch, FAISS, Kuzu (Graph). On-Device-Embeddings: FastEmbed (kein API-Call, Privacy). → Qdrant ist
auch die VPS-Empfehlung ([[server-infrastruktur]]).

## 7. Frontend-Integrationen (`extern`)
- **Voice:** ElevenLabs (async `addMemories`/`retrieveMemories`), LiveKit, Pipecat — "einer der wichtigsten
  aufkommenden Use-Cases" (relevant für Franks Auto-App).
- **Web/TS:** Vercel AI SDK (`@mem0/vercel-ai-provider`), Mastra (TypeScript-first).
- **CLI:** Letta Code, Mem0 CLI.
- **Mobile:** NICHT direkt belegt — App-spezifisch (z. B. React-Native-Wrapper um die Mem0-REST-API).

## 8. Benchmarks (`extern`)
LoCoMo (1.540 Fragen, single/multi-hop/open-domain/temporal); LongMemEval (500 Fragen, 6 Kategorien);
BEAM (1M+10M Token, inkl. Abstention/Contradiction/Event-Ordering); MemoryBench (continual learning).

## 9. Synthese-Blueprint für Frank (⚠️ KOMPONIERT, kein Quellen-Wortlaut)
Die Bausteine sind je belegt, die Komposition ist meine Synthese:
- **Memory-Server (remote, self-hosted):** Mem0 (Apache-2.0) als zentraler Service — siehe [[memory-backends]].
- **Vektor-Backend:** Qdrant (oder PGVector ohne Extra-Stack).
- **Graph (optional):** Graphiti, falls temporale Tiefe gewünscht.
- **Lokaler Orchestrator:** eigener Agent-Loop mit Tiered-Memory-Tools (§2.1) — der "Dirigent".
- **Frontends:** Web (Vercel AI SDK), Voice (ElevenLabs/LiveKit), CLI (Mem0 CLI), Mobile (REST-Wrapper).
- **Embeddings:** FastEmbed on-device (Privacy).

## 10. Widersprüche zwischen Quellen (ehrlich)
- "None wins" (Wasowski) vs. klare Use-Case-Empfehlungen (JobsByCulture) vs. EverMind platziert EIGENES EverOS
  auf Platz 1 (nicht neutral).
- Tiered (Letta) vs. flach + reiches Retrieval (Mem0) — orthogonale Ansätze, nicht identisch.
- Graph als opt-in (Mem0) vs. Graph als Default (Zep/Graphiti).

## Offene Lücken
Cortex/Khoj/Reor in den Quellen dieses Laufs NICHT erwähnt (Cortex kam aus dem Datenmodell-Lauf, [[datenmodell]]);
keine End-to-End-Blueprint über alle 4 Frontends als kohärente Quelle; Mobile-Muster + Self-Host-Deployment-Topologie
nicht belegt. §9 ist Synthese — vor Umsetzung prüfen.

## Quellen (`extern`, 2025-2026)
Mem0 "State of AI Agent Memory 2026" (mem0.ai/blog, 01.04.2026); Graphlit "Memory vs Context" (20.05.2026);
JobsByCulture "AI Agent Memory Systems: 2026 Engineering Guide"; EverMind "Best OSS Agent Memory 2026"
(09.06.2026, Anbieter-gefärbt); Wasowski "5 Memory Systems compared" (Medium, 13.05.2026).
