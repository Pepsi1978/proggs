# mem0 (KI-Memory-Layer) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | ⭐ Gehirn sauber halten | `custom_instructions` STRENG (nur bestaetigte Fakten), spaeter lockern | §1 |
| 2 | Themen steuern | `includes`/`excludes` + 2–3 `custom_categories` | §1 |
| 3 | Rauschen filtern | Confidence-Gate < 0.7 verwerfen; Such-`threshold` (0.1) | §1 |
| 4 | Feedback-Loop vermeiden | Abgerufene Memories NIE zurueck in `add(infer=True)` | §1 |
| 5 | Embedder/Vector-Store | `embedding_dims`==`embedding_model_dims`==Modell-Dim (1536) | §2 |
| 6 | Hybrid-Suche | `fastembed` installieren (sonst BM25 still aus) | §2 |
| 7 | Pflege | Feedback-API + periodisches Junk-Audit (manuell) | §3 |
