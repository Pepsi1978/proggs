# VPS-Hosting für selbst-gehostete DB/Dienste (Hostinger, Hetzner) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ „Reicht Shared/Web-Hosting für eine eigene DB/Dienst?" | **NEIN** — Shared = kein root, kein Docker, keine eigenen Dienste. **Nur VPS** (KVM) hat root + Docker. | §1 |
| 2 | Hostinger-Einstiegspläne für KI-Workload | **Netzwerk-Benchmark Note E**, CPU Note E/D (KVM 1/2) laut vpsbenchmarks → für rechenintensive Embeddings (Ollama) ungeeignet, für reines pgvector ok. | §2 |
| 3 | Hostinger-Preis wirkt billig | **Verlängerungs-Falle:** Einführungspreis (z.B. $6.49) gilt nur für die Laufzeit; Verlängerung ~2× ($11.99), regulär noch höher ($19.49). | §3 |
| 4 | Backup bei Hostinger | Nur **wöchentliche** Backups gratis; **tägliche** kosten extra — riskant für eine oft schreibende DB. | §4 |
| 5 | RAM für den Stack | pgvector allein: kleinster VPS (4 GB) ok. **Lokale Ollama-Embeddings: 8–16 GB** (KVM 2/4). | §5 |
| 6 | EU-Datenschutz / flexible Abrechnung | **Hetzner** (DE/EU, stundengenaue Abrechnung, bessere Netzwerk-/CPU-Note) statt Hostinger, wenn Datenschutz/Performance zählt. | §6 |
