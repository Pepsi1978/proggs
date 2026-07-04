# Kurzcheck: Second Brain Memory-Evolution (Kurzzeit/Langzeit, Konsolidierung, Proaktivität)

> Stand: 2026-07-04 · Volltext: `memory-evolution-2026.md` · Plan: `second-brain-server/LEVEL2-FEATURES-PLAN.md`
> Version-Anker: Qdrant 1.18.x, gemini-embedding-001, FastAPI, Claude Code CLI (2026-07)

| # | Sofort-Regel | Warum / Quelle |
|---|--------------|----------------|
| 1 | Kurzzeitgedächtnis = **Kern-Blöcke** (kleine, immer im Kontext liegende Textblöcke mit Zeichen-Limit), Langzeit = Qdrant-Archiv | Letta/MemGPT Core vs. Archival Memory |
| 2 | Konsolidierung **asynchron** in einem eigenen Sleep-Time-Agent — nie im Antwort-Pfad; darf ein stärkeres Modell nutzen | Letta Sleep-Time (Zwei-Agenten-Muster) |
| 3 | Vergessen als Decay-Score (Alter + Abrufhäufigkeit + Relevanz), **nie auf null** — jeder Abruf verstärkt (+Strength, Zeit-Reset) | MemoryBank / arXiv 2404.00573 |
| 4 | Fakten-Updates mit **ADD/UPDATE/DELETE/NOOP** gegen den Bestand prüfen statt duplizieren; Widerspruch → Rückfrage | mem0-Muster |
| 5 | Historie erhalten: **bi-temporale Felder** (gültig-ab/gültig-bis + gewusst-seit) — alte Fakten stempeln, nicht löschen | Zep/Graphiti |
| 6 | **KEIN Voll-Knowledge-Graph/GraphRAG** für hunderte Einträge — mem0 v3 hat den Graph-Layer entfernt; leichtes Entity-Linking (2. Collection, Hub-and-Spoke) reicht | mem0 PR #4805, LOCOMO-Benchmarks |
| 7 | Retrieval-Upgrade Nr. 1: **Hybrid-Suche** (dense + BM25-sparse, RRF-Fusion) — nativ in Qdrant, EIN query_points-Call | Qdrant-Doku |
| 8 | Proaktivität nur als **Notify / Question / Review** + harte **Drossel** (max. Meldungen/Tag, Ruhezeiten) | LangChain Ambient, ProAgent (+38,9 % Zufriedenheit) |
| 9 | Unterbewusstsein-Gefühl = **Per-Turn-Kontext-Injektion**: vor jeder Antwort still passendes Wissen beilegen; Display-Layer ≠ System-Layer ("knows more than it shows") | Hermes/NeuroLoop-Muster |
| 10 | Antworten brauchen **Provenance + Confidence** (Quelle + Sicherheit nennen), sonst kein Vertrauen (+30 % Zeitersparnis NUR damit) | Enterprise-KM-Befund |
| 11 | Session-Mitlernen: SessionEnd-Hook schreibt „gemacht/entschieden/gelernt" ins Gehirn — der **Rückfluss-Loop** ist die Lücke, nicht die Speichertechnik | claude-engram, claude_progress, 100-Sessions-Analyse |
| 12 | Beim Transkript-Mining einen **Schema-Canary** einbauen (Log-Format-Änderungen von Claude Code erkennen und warnen) | claude-engram |
| 13 | Selbstorganisation nur als **Vorschlag mit Bestätigung** (Dubletten-Merge, Kategorien-Umbau) — nie stille Auto-Umbauten (Serendipität + 1:1-Prinzip schützen) | CombiningMinds-Warnung |
| 14 | Governance-Lücke aller Frameworks: Diff-and-Approve vor Long-Term-Writes gibt es nirgends fertig — **Eigenbau einplanen** | memorywire (arXiv 2606.01138) |
