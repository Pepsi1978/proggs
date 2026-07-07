# Groq API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | OpenAI-Drop-in | `base_url=.../openai/v1`; nicht unterstützte Felder gar nicht senden | §1 |
| 2 | Modell/Output-Tokens | Dynamisch von `/models`; `max_completion_tokens` ans Output-Limit | §2 |
| 3 | Rate-Limits | Alle Header überwachen, token-bewusst pre-throttlen; `retry-after` zuerst | §3 |
| 4 | Tool/Structured Output | Striktes Schema; strict-Mode nur gpt-oss-20b/-120b, sonst Repair-Loop | §4/§5 |
| 5 | Streaming/Chat | `stream=true`; `temperature` bewusst >0 (echte 0 → `1e-8`) | §6 |
| 6 | Whisper | 16 kHz Mono FLAC, chunken, `verbose_json`; min. 10 s Abrechnung | §7 |
| 7 | Massen-Workloads | Batch API (50 % Rabatt); native SDKs mit Retries nutzen | §8 |
