# Referenz-Architekturen & Muster Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
