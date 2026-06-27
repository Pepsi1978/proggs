# Lokale OpenAI-kompatible LLM-Server (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 404 / connection refused ⭐ | `base_url` mit `/v1`; `127.0.0.1` statt `localhost` | §1, §2 |
| 2 | SDK crasht "Missing credentials" ⭐ | API-Key nie leer — Platzhalter `"ollama"` setzen | §4 |
| 3 | Langer Prompt abgeschnitten ⭐ | Ollama ignoriert `num_ctx`: Modelfile/`OLLAMA_CONTEXT_LENGTH` | §5 |
| 4 | Erster Request timeout ⭐ | Timeout ≥ 60 s; Modell vorladen; `keep_alive` bewusst | §8 |
| 5 | vLLM Tool-Calls ignoriert ⭐ | `--enable-auto-tool-choice` UND `--tool-call-parser` | §9 |
| 6 | `logprobs`/`n` fehlen | Ueber `/v1` still verworfen → native API | §14, §15 |
| 7 | "model not found" | Modell pullen/laden; vLLM `--served-model-name`, `/v1/models` | §7, §18 |
| 8 | json_schema+grammar-Fehler | llama.cpp: nur EINS angeben, nicht beides | §13 |
