# Second Brain — selbstgehostetes persönliches Memory-System (Best-Practices-Index, Stand 2026-06-21)

> Wissensbasis für Franks Ziel: EIN serverseitiges "zweites Gehirn" für ALLES (nicht nur Programmieren) —
> von überall erreichbar (Claude Code/OpenCode via MCP + eigene Apps via REST, z. B. Sprach-App im Auto),
> mit einem lokalen Orchestrator-Agenten als "Dirigent". Recherchiert am 2026-06-21 (Engine A→B,
> Firecrawl+MiniMax; Engine-B-OpenRouter-Läufe fielen aus → alles über Firecrawl repariert, siehe
> `bugs/apis/openrouter-api.md`). Projekt-Memory: `project_second_brain_memory_server`.
> Verwandt/teils überlappend: `best-practices/opencode/self-hosted-memory-server.md` (supermemory-Bauplan),
> `best-practices/server/` (VPS, WireGuard).

---

## Die Dateien dieses Ordners

| Datei | Inhalt |
|-------|--------|
| [[server-infrastruktur]] | VPS-Wahl (Hetzner CX32), Dimensionierung, Docker/Qdrant, Reverse-Proxy/TLS, Cloud-Firewall, Sicherheit, DSGVO, Backup |
| [[memory-backends]] | Vergleich Mem0/Letta/Zep/Cognee/supermemory; Empfehlung **Mem0**; Auflösung des supermemory-Widerspruchs |
| [[datenmodell]] | Drei-Speicher-Muster (Graph+Memory+Chunk), Pflicht-Metadaten, Scopes/Tags, konkretes Record-Schema für Frank |
| [[orchestrator-und-suche]] | Router-Agent (Intent/Routing) + hybride Suche (RRF, Reranking, Chunking) + **welche Suche wann** |
| [[schreibpfad-ingestion]] | "Speicher das"-Pfad: extrahieren statt roh, klassifizieren, Dedup, Konflikt, Voice-Flow |
| [[qualitaet-pflege]] | Memory-Hygiene über Jahre: Entity-Resolution≠Dedup, Konsolidierung, Decay, Re-Embedding, Evaluation |
| [[multi-client-zugriff]] | EIN Server, viele Clients: MCP (Tools/Resources/Prompts) + REST + Auth/Scoping + Remote-MCP-Fallen |
| [[referenz-architekturen]] | Architektur-Sprache: 4 Memory-Typen, Memory≠Context, OS-Tiered, Frameworks+Benchmarks, Anti-Patterns |

## Das Gesamtbild — 3 Schichten (deckt sich mit OS-Tiered & Franks eigener Idee)

```
┌─ KERN (lokal, immer dabei, klein) ──────────── = Core Memory / "RAM"
│   Verhalten/Betriebsregeln (3 Direktiven etc.) — NIE vom Server-Recall abhängig
│   + winziger Orchestrator-Router (Semantic-/Regel-Router, billig)
│
├─ DIRIGENT (lokaler Agent) ──────────────────── wählt Such-Art + Kategorie, ruft Server, schreibt zurück
│
└─ SERVER-MEMORY (remote, self-hosted, groß) ── = Recall + Archival / "Cache + Disk"
    Wissen für ALLES: Programmieren · Personen-Kontext · Inventar · Aufgaben/Ideen/Journal
    Mem0 (MCP+REST) → Qdrant (Vektor) [+ Graphiti für temporale Fakten]
```
**Wichtige Grenze:** **Wissen = Server, Verhalten = lokaler Kern.** Das Memory liegt komplett auf dem Server
(überall erreichbar); die paar verhaltenskritischen Regeln bleiben lokal und dürfen nie von einem Server-Abruf
abhängen. Bei Server-Ausfall greift der Dirigent auf lokale Datei-Quellen zurück (Graceful Degradation).

## Empfohlener Stack (Synthese — Bausteine belegt, Komposition ist Vorschlag)

| Schicht | Empfehlung | Warum |
|---------|-----------|-------|
| VPS | **Hetzner CX32** (~6,80 €/Mo, EU/DSGVO) | bestes Preis/Leistung, NVMe, Cloud-Firewall inkl. |
| Memory-Backend | **Mem0** (Apache-2.0) | MCP-nativ (Claude Code/OpenCode) + REST (eigene Apps) + Multi-Scope, unstrittig OSS |
| Vektor-DB | **Qdrant** (Docker) | Single-Binary, Sub-30ms, <1 Mio. Vektoren |
| Graph (optional) | Graphiti | temporale Fakten (Adresse/Job ändern sich) |
| Embeddings | FastEmbed / BGE-M3 (DE/EN) | lokal, Privacy — vor Einsatz testen |
| Suche | Hybrid Vektor+BM25, RRF (k=60) + Reranking | siehe [[orchestrator-und-suche]] |
| Reverse-Proxy | Caddy (Auto-TLS) + Bearer/OAuth | DB-Port nie öffentlich |
| Frontends | Web (Vercel AI SDK), Voice (ElevenLabs/LiveKit), CLI (Mem0 CLI), Mobile (REST-Wrapper) | |

## Übergreifende Prinzipien (die roten Fäden)
1. **Memory ≠ Context** — Memory ist das Substrat, der Dirigent zeigt dem Modell nur die minimale nützliche Auswahl.
2. **Extrahieren statt roh speichern** — Rohtext = verrauschtes Retrieval; erst zu Fakten/Entitäten destillieren.
3. **Welche Suche wann** — Ort/Inventar→strukturiert, Name/Code→BM25, Konzept→semantisch, Beziehung→Graph, sonst Hybrid.
4. **Qualität über Wachstum** — Entity-Resolution≠Dedup trennen, Type-Gating, Konsolidieren, Recency-Weighting; sonst "verrottet der Graph leise".
5. **Pflicht-Metadaten** — timestamp, source, category, confidence (0-1), valid_from/until (bi-temporal), tags.
6. **Mehr Kontext ≠ besser** (Context Rot) — schlanker Kern + gezieltes Nachladen schlägt den großen Klotz.
7. **Sicherheit/DSGVO ernst** — EU-Hosting, per-User-Key fürs Vergessen, 3-2-1-Backup mit Offsite (Embeddings!), Restore testen.

## Kategorienweise starten (Empfehlung)
Nicht alles auf einmal. Zuerst die Kategorie **Programmieren** (täglich genutzt = sofortiges Feedback, ob das
Retrieval gut ist), das Record-Schema ([[datenmodell]] §4.1) so bauen, dass Persönliches/Inventar/Journal später
über `fields` andocken. Erst lokale Schicht + ein Backend solide, dann Frontends ergänzen.

## Offene Nacharbeiten (vor der Umsetzung)
- supermemory Lizenz/MCP-Self-Host LIVE verifizieren ([[memory-backends]] §3).
- Mem0 lokale Ollama-Extraktion + OSS-Graph (vs. Pro-Tier) prüfen.
- Embedding-Modell (BGE-M3 vs. FastEmbed) an Franks realen DE/EN-Daten testen.
- Eigener kleiner Eval-Satz typischer Anfragen (für RRF-/Router-Tuning + Langzeit-Qualität).
- Mobile- und vollständige Voice-Pipeline (STT→klassifizieren→speichern→TTS-Bestätigung) sind nicht
  quellenbelegt — App-spezifisch über die REST-API bauen.
