# Audio-Transkription mit Groq (OpenAI Whisper large-v3 / turbo) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Floskel bei Stille ("Vielen Dank") | Inhaerentes Whisper-Verhalten, nicht abschaltbar — Abwehr-Kette noetig | §1.1 |
| 2 | "Nichts gesagt"-Clip | Sprachgehalt-Vorfilter VOR dem Senden, nicht erst Confidence-Gate | §2.1, §2.3 |
| 3 | Response-Format waehlen | `verbose_json` (bei Groq gratis vs. `text`) fuer Confidence-Felder | §3.1 |
| 4 | Confidence-Gate bauen | Verwerfen nur `no_speech_prob>0.6` UND `avg_logprob<-1.0` — nie ODER | §2.3 |
| 5 | Halluziniertes Wort NACH echtem Satz | Schicht 3: Segment-Zeitfenster gegen RMS-Timeline abgleichen | §2.3 |
| 6 | Floskel-Blocklist | Nur bei kurz + exaktem Match + Stille-Kontext (echte Aussage bleibt) | §2.4 |
| 7 | `temperature=0` gesetzt | Reicht NICHT gegen Stille-Halluzination — Kette ergaenzen | §1.2 |
| 8 | `prompt` als Anti-Halluzination | Nur Stil/Vokabular, kurz, keine Befehle (sonst Leakage) | §1.3 |
| 9 | `language` setzen | ISO-639-1 (`de`), nicht `german`/`de-DE` | §3.4 |
| 10 | Extra-Parameter senden | Nur dokumentierte Groq-Felder (sonst 400) | §3.6 |
| 11 | 429 Rate-Limit | `retry-after`-Header lesen; 413/422 NICHT retryen | §4.2, §4.5 |
| 12 | Viele kurze Clips | Min-Abrechnung 10 s/Clip — buendeln/Vorfilter | §4.1 |
