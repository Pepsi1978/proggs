# Lokale OpenAI-kompatible Server Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Endpunkt | `base_url` mit `/v1`; LAN auf `0.0.0.0`; lokal `127.0.0.1` | §1 |
| 2 | Auth | API-Key nie leer (Platzhalter); echte Auth exakt matchen | §2 |
| 3 | Modell-Laden | `keep_alive`/TTL bewusst; Alias + `/v1/models` verifizieren | §3 |
| 4 | Kontextfenster | Ollama Modelfile/`OLLAMA_CONTEXT_LENGTH`; llama.cpp `--ctx-size` | §4 |
| 5 | Tool-Calling | vLLM 2 Flags; llama.cpp `--jinja`; defensiv parsen | §5 |
| 6 | Structured Output | json_schema ODER grammar (nie beides); pro Server-Format | §6 |
| 7 | Streaming/Embeddings | Stream am `[DONE]` schliessen; `--embeddings`/`--parallel` | §7 |
| 8 | Timeouts/Drift | First-Request ≥ 60 s; Compat „best-effort", Doku pruefen | §8 |
