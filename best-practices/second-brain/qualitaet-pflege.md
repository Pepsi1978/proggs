# Qualität über Wachstum (Memory-Hygiene) — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Behandelt das schwierigste Dauerthema: Wie bleibt ein "immer wachsendes"
> Memory über Jahre nützlich statt verrauscht? Dedup, Stale-Erkennung, Widersprüche, Decay, Konsolidierung,
> Re-Embedding, Evaluation. Quellen: `extern` (Entity-Resolution-Praxis, Google Always-On Memory Agent 03/2026,
> Memory-Survey + Benchmarks). Schwester: [[schreibpfad-ingestion]], [[datenmodell]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Dedup-Kernregel | **Entity Resolution ≠ Deduplication trennen.** Resolution = "wie nennen wir es" (Tippfehler/Aliase); Dedup = "ist das dieselbe Entität" (echte Merges) |
| Dedup-Routing | Drei Wege: **merge · für Mensch flaggen · neuen Knoten anlegen** (nicht blind mergen) |
| Sicherheitsnetz | **Type-Gating** (PERSON nie mit ORGANISATION mergen) + Alias-Listen pro Knoten; sonst "verrottet der Graph leise" |
| Stale/veraltet | Confidence-Scoring + Konsolidierungs-Drift beobachten (halluzinierte Insights propagieren sonst als "Fakt") |
| Widerspruch | alten Fakt invalidieren (bi-temporal, [[datenmodell]] §3) statt überschreiben; LLM-as-Judge bei Bedarf |
| Vergessen/Decay | TTL/Decay nach Alter+Zugriff; **"learned forgetting" ist offiziell ein offenes Problem** — pragmatisch lösen |
| Konsolidierung | periodischer Hintergrund-Job (z. B. alle 30 Min): Verbindungen finden, Entitäten extrahieren, **Synthese statt Summary** |
| Re-Embedding | bei Embedding-Modell-Wechsel ALLES neu einbetten (sonst inkompatibler Vektorraum) — Quellen-Lücke, aber zwingend |
| Evaluation | Retrieval-Qualität über Zeit messen (Benchmarks: MemBench, MemoryAgentBench, MemoryArena) |

---

## 1. Deduplizierung — zwei getrennte Schritte (`extern`)
Häufigste Verwechslung, die zu stiller Korruption führt:
- **Entity Resolution** ("wie nennen wir es?"): Tippfehler, Akronyme, Oberflächenformen. **Short-Circuit-Kette:**
  exaktes Match → fuzzy Match → semantisches Match (leichte Embeddings, NUR über den Namen). Vergleich nur
  gegen **gleich-typige** Knoten (Type-Gating).
- **Deduplizierung** ("ist das dieselbe Entität?"): Identitätsebene mit echten Merges. Embeddings über den
  **vollen** Entitäts-Kontext (Name+Typ+Attribute+Metadaten). Drei Routing-Entscheidungen: **merge / flag for
  human review / add new node**.

**Drei Sicherheitsnetze**, die die meisten Tutorials überspringen:
1. Naming und Identity trennen (verhindert silent corruption).
2. **Type-Gating** (z. B. "Jensen Huang, NVIDIA-CEO" nicht mit gleichnamigem Arzt in Taipeh mergen).
3. Alias-Listen pro Knoten (schnellere künftige Lookups).

## 2. Stale-Erkennung (`extern`, dünn)
Kaum direkt behandelt. Nächstes: **Konsolidierungs-Drift** — halluziniert das LLM in einem Konsolidierungs-
Zyklus eine Verbindung und speichert sie als "Memory", referenziert sie der nächste Zyklus als Fakt → Fehler
kumulieren. **Mitigation: Confidence-Scoring** (LLM flaggt unsichere Verbindungen). Kein TTL/Verfallsdatum-
Konzept in den Quellen — für Frank über `valid_until` + Decay (siehe [[datenmodell]]) lösen.

## 3. Widerspruchsauflösung (`extern`, dünn)
"Contradiction handling" wird als Engineering-Realität genannt, aber ohne konkrete Strategie. Konsolidierungs-
Agenten KÖNNEN widersprüchliche Befunde aufdecken, lösen sie aber nicht spezifiziert auf. Für Frank ableitbar:
bi-temporale Invalidierung ([[datenmodell]] §3) + bei echtem Konflikt LLM-as-Judge oder Mensch-Flag (§1-Routing).

## 4. Decay / Vergessen (`extern`)
**"Learned forgetting" ist explizit ein offenes Forschungsproblem** (neben continual consolidation, causally
grounded retrieval, trustworthy reflection, multimodal memory). Keine fertigen Decay-Funktionen/Vergessenskurven
in den Quellen. Pragmatisch für Frank: Alters- + Zugriffs-basierte Gewichtung, TTL für Flüchtiges, jüngere
Memories höher ranken (deckt sich mit [[schreibpfad-ingestion]] §7).

## 5. Konsolidierung / Synthese (`extern`, gut belegt)
**Google Always-On Memory Agent (03/2026)** — die detaillierteste Referenz, <1000 LOC, modell-agnostisch:
- **IngestAgent:** erfasst Text/Bild/Audio/Video/PDF in Echtzeit, speichert in SQLite **ohne Embeddings/
  Preprocessing** (nur Timestamps + Metadaten).
- **ConsolidateAgent:** Hintergrund-Job (Default alle 30 Min), liest unkonsolidierte Memories, findet
  thematische Verbindungen, extrahiert Entitäten, generiert Insights. **"Das ist keine Zusammenfassung, das
  ist Synthese."**
- **QueryAgent:** liest konsolidierte Memories direkt, KEIN Vektor-Retrieval.
- Tauscht Retrieval-Präzision gegen Synthese-Tiefe (emergente Muster). **Hybrid empfohlen:** RAG für statische
  Dokumente, Always-On-Memory für sich entwickelnden Kontext.
- Offene Fragen: Wie oft konsolidieren? Alte Konsolidierungen re-konsolidieren? Drift früh erkennen?

## 6. Re-Embedding bei Modellwechsel (`extern`, Lücke)
Nicht direkt belegt (nur KG-Embedding-Lifelong-Learning, anderes Thema: maskierter KG-Autoencoder,
Embedding-Transfer, Regularisierung gegen katastrophales Vergessen). **Aber zwingend für Frank:** Wechselt
das Embedding-Modell, ist der alte Vektorraum inkompatibel → der gesamte Bestand muss neu eingebettet werden.
Deshalb das Modell früh sorgfältig wählen ([[orchestrator-und-suche]] §2.6) und Rohtext-Chunks behalten
(Re-Embedding braucht das Original — Drei-Speicher-Muster [[datenmodell]] §1 deckt das ab).

## 7. Evaluation der Retrieval-Qualität (`extern`)
Verschiebung von statischen Recall-Benchmarks zu **multi-session agentic tests** (verschränken Memory mit
Entscheidungen). Benchmarks: **MemBench** (2025), **MemoryAgentBench** (2025), **MemoryArena** (2026). Konkrete
Langzeit-Metriken nicht aufgelistet. Für Frank: einen kleinen eigenen Eval-Satz typischer Anfragen pflegen
(siehe [[orchestrator-und-suche]] — RRF/Tuning braucht ohnehin ein Eval-Set).

## 8. Context Rot / "der Graph verrottet leise" (`extern`)
Der Begriff "context rot" fällt hier nicht, das Pendant ist **"the graph quietly rots"**: Bei schlechter Dedup
(falsche Merges) verliert man Vertrauen in den Graph, der ganze Memory-Layer wird ungenutzt — "der Fehler ist
unsichtbar, bis er teuer rückgängig zu machen ist". Genau deshalb §1 (Resolution/Identity trennen) so ernst nehmen.

## Offene Lücken
mem0/Zep-Interna (Dedup-Schwellen, Decay-Algorithmen, Conflict-Resolution) nicht beschrieben; konkrete
Stale-/Decay-Mechanik, Re-Embedding-Vorgehen und Langzeit-Metriken nicht belegt — oben als "abgeleitet"/
"pragmatisch" markiert.

## Quellen (`extern`, 2025-2026)
Entity-Resolution-vs-Dedup-Praxisartikel (Type-Gating, Short-Circuit, "graph quietly rots"); Google Always-On
Memory Agent (03/2026, GitHub-Referenz-Impl); Memory-Survey + Benchmarks (MemBench/MemoryAgentBench/MemoryArena);
LKGE (AAAI 2023, KG-Embedding-Lifelong-Learning — als Analogie, nicht als Beleg).
