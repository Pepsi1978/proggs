# Qualität über Wachstum (Memory-Hygiene) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
