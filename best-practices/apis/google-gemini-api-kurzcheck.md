# Google Gemini API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | SDK & Client | Nur `google-genai`; Key per `x-goog-api-key` | §1 |
| 2 | Modellwahl | Stabile IDs pinnen, nie `-latest`; flash default | §2 |
| 3 | Thinking-Budget | `maxOutputTokens` hoch; einfache Tasks niedrig | §3 |
| 4 | Structured Output | `responseMimeType` + `responseSchema`, beide noetig | §4 |
| 5 | Function Calling | OpenAPI-Subset, ~10-20 Tools, klare Description | §5 |
| 6 | Antwortpruefung | 200 OK ≠ Text: block/finishReason erst pruefen | §6 |
| 7 | Caching/Token | Wiederkehrendes an Prompt-Anfang; `system_instruction` | §7 |
| 8 | Streaming/Limits | `?alt=sse`; Backoff bei 429; Billing aktiv | §8 |
| 9 | Embeddings (mehrere Texte) | `embed_content` BATCHEN: `contents=[t1,t2,…]` -> eine embeddings-Liste in Eingabe-Reihenfolge; nie seriell je Text | §9 |
| 10 | Sprache-zu-Text, fertige Datei | `gemini-3.5-transcribe` ueber `POST /v1beta/interactions`, Audio inline; NICHT das `-live`-Modell (4,4 s statt 15,1 s, WER 2,6 % statt 4,0 %) | §10 |
| 11 | Transkript soll wortgetreu sein | `transcription_config.mode` = `verbatim`; `smart` laesst Woerter weg (gleich schnell) | §10 |
| 12 | Fachbegriffe/Eigennamen | `custom_vocabulary` (Batch) bzw. `inputAudioTranscription.customVocabulary` (Live), bis ~100 Begriffe | §10 |
| 13 | Live-Transkription noetig | VAD abschalten + `activityStart`/`activityEnd`, sonst Abbruch bei jeder Denkpause; `languageCodes` ist ein ARRAY | §10 |
| 14 | STT-Ausfallsicherheit | Bei 429/Netzfehler auf Zweitanbieter ausweichen — aber NIE bei stiller Aufnahme (eigene Ausnahmeklasse) | §10 |
