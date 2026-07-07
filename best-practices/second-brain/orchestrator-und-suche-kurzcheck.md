# Orchestrator-Agent & hybride Suche Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Anfrage = "wo ist X / was ist in Schublade Y" (Inventar, exakter Ort) | **Strukturierte Metadaten-Abfrage** (SQL-artig auf Feldern), NICHT Embedding-Ähnlichkeit |
| Anfrage = exakter Name / Fehlercode / Versions-Nr / seltener Fachbegriff | **Keyword/BM25** (lexikalisch) |
| Anfrage = Konzept / Idee / paraphrasiert / "irgendwas über …" | **Vektor/semantisch** |
| Anfrage = Beziehung / "wer/was hängt mit X zusammen" / Multi-Hop | **Graph-Traversierung** |
| Unklar / gemischt (Standardfall) | **Hybrid: Vektor + BM25, fusioniert mit RRF (k=60), danach Cross-Encoder-Reranking** |
| Router-Wahl | **Semantic-/Embedding-Router als Default** (1 Vektorvergleich, billig); LLM-Router nur bei echter Mehrdeutigkeit; Regel-Router für klare Stichwort-Fälle |
| Latenz/Kosten-Falle | LLM für jede Routing-Entscheidung = teuer + langsam. Mehr Kontext ≠ besser (Rauschen senkt Trefferqualität — Context Rot) |
| Embedding-Modell DE/EN | mehrsprachiges, lokal lauffähiges Modell (z. B. BGE-M3) — `extern`/abgeleitet, vor Einsatz an EIGENEM Korpus testen |

**Merksatz:** Der Router entscheidet *welche* Suche, die Suche entscheidet *welche Treffer*. Beide
klein und billig halten — der teure LLM-Verstand gehört in die Aufgabe, nicht in die Türsteher-Frage.
