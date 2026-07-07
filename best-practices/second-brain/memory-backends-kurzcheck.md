# Memory-Backends (self-hosted) im Vergleich Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
