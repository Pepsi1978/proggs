# DeepSeek API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | R1/`deepseek-reasoner` History 400 ⭐ | `reasoning_content` aus History strippen | §A |
| 2 | V3.2/V4 Thinking+Tool-Use 400 ⭐ | Umkehr: `reasoning_content` MUSS zurückgeschickt werden | §A |
| 3 | Tool-Calling nötig | `deepseek-chat`/V3 nutzen — reasoner kann kein `tool_choice` (400) | §B |
| 4 | Sampling beim Reasoner | `temperature`/`top_p`/penalties weglassen; `logprobs` wirft Error | §C |
| 5 | Antwort abgeschnitten | `max_tokens` explizit/dynamisch hoch, `finish_reason:length` prüfen | §D |
| 6 | 402 wirkt wie Auth-Fehler | 402 (Guthaben leer) ≠ 401; Backoff bei 429/503 | §E |
| 7 | Streaming Reasoner | `delta.reasoning_content` + `delta.content` getrennt akkumulieren | §F |
