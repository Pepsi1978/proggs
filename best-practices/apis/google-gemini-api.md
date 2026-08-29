# Google Gemini API — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/google-gemini-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
> Update 2026-07-02: Keine neuen belegten SDK-/Thinking-/FinishReason-Regeln; Deprecation-Seite weiter aktiv vor jedem Release pruefen und stabile Modell-IDs pinnen.
> Update 2026-07-08: §9 Embeddings modell-differenziert — `gemini-embedding-2` (GA ~30.04.2026) aggregiert eine Liste zu EINEM Vektor (NICHT N), `task_type` ist entfernt (→ Text-Präfixe), Default 3072 Dim / 8192 Token. Fallen im Bug-Almanach §J. (Recherche 2026-07-08, Firecrawl+MiniMax, offizielle Google-Doku.)

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
| 9 | Embeddings (mehrere Texte) | **Modell-abhängig!** `-001`: `contents=[…]` batchen → N Vektoren. `gemini-embedding-2`: Liste → 1 AGGREGIERTER Vektor → pro Text 1 Call; `task_type` weg → Text-Präfixe | §9 |

## 1. SDK & Client
- Ausschliesslich das einheitliche SDK `google-genai` (Py) / `@google/genai` (JS) / `google.golang.org/genai` (Go) verwenden; Init ueber `client = genai.Client(api_key=...)` bzw. `genai.Client(vertexai=True, project=..., location=...)`. Altes SDK ist deprecated. Quelle: https://ai.google.dev/gemini-api/docs/libraries · offiziell
- API-Key per Header `x-goog-api-key` uebergeben (nicht `?key=` im Query — landet in Logs); Quota-Projekt ggf. via `x-goog-user-project`. Quelle: https://ai.google.dev/gemini-api/docs/libraries · offiziell

## 2. Modelle & Versionierung
- In Produktion stabile, versionierte Modell-IDs pinnen (z. B. `gemini-2.5-flash`, `gemini-3.5-flash`), NIE `-latest`; Deprecation-Seite ueberwachen (1.0/1.5 weg, 2.0-flash Shutdown 01.06.2026). Quelle: https://ai.google.dev/gemini-api/docs/deprecations · offiziell
- Default-Wahl: `flash`-Modelle fuer hohen Durchsatz/niedrige Kosten, `pro` nur fuer komplexes Reasoning. Quelle: https://ai.google.dev/gemini-api/docs/interactions/whats-new-gemini-3.5 · offiziell

## 3. Thinking / Reasoning (Budget-Disziplin)
- Gemini 3.x: `thinkingLevel` (`minimal`/`low`/`medium`/`high`) statt `thinkingBudget`; Gemini 2.5: `thinkingBudget` (Tokenzahl). Bei einfachen Tasks (Fact/Klassifikation) niedrig/`thinkingBudget:0`, bei Math/Code hoch. Quelle: https://ai.google.dev/gemini-api/docs/thinking · offiziell
- Thinking-Tokens zaehlen gegen die Antwort → `maxOutputTokens` grosszuegig setzen, sonst `finishReason: MAX_TOKENS` + leerer Text. `thoughtsTokenCount` fuer Kostenkontrolle pruefen. Quelle: https://ai.google.dev/gemini-api/docs/thinking · offiziell
- Gemini 3: Thought-Signatures bei Multi-Turn unveraendert komplett zurueckspielen (Parts nicht zusammenfuehren/mergen). Quelle: https://ai.google.dev/gemini-api/docs/thinking · offiziell

## 4. Structured Output (JSON)
- `responseMimeType: "application/json"` + `responseSchema` setzen (beide noetig). Bei einfachen Schemata `responseSchema`, bei komplexen mit `$ref`/Nesting `responseJsonSchema`. Quelle: https://ai.google.dev/gemini-api/docs/structured-output · offiziell
- Starke Typen nutzen (`integer`/`string`/`enum`), `enum` bei fixen Wertemengen; `description` je Property pflegen; bei Gemini 2.5+ wird Key-Reihenfolge bewahrt (2.0 braucht explizites `propertyOrdering`). Quelle: https://ai.google.dev/gemini-api/docs/structured-output · offiziell
- Python `Pydantic` / JS `Zod` direkt als Schema-Quelle nutzen (Auto-Konvertierung); Werte trotz syntaktisch korrektem JSON serverseitig semantisch validieren. Quelle: https://blog.google/innovation-and-ai/technology/developers-tools/gemini-api-structured-outputs/ · offiziell

## 5. Function Calling / Tools
- Funktions-`description` extrem klar/spezifisch, starke Typen + `enum` fuer fixe Werte. Function-Calling-Mode bewusst waehlen: `AUTO` (Default), `ANY` (Funktion erzwingen), `NONE` (aus), `VALIDATED` (Default bei Multi-Tool). Quelle: https://ai.google.dev/gemini-api/docs/function-calling · offiziell
- Nur relevante Tools mitgeben (ideal 10-20 max) fuer hoehere Trefferquote; Schema nur OpenAPI-Subset (kein `default`/`oneOf`), Nesting begrenzt halten. Quelle: https://ai.google.dev/gemini-api/docs/function-calling · offiziell
- Gemini 3: `temperature` auf Default 1.0 lassen (niedrigere Werte koennen Verhalten stoeren); 3.5+ unterstuetzt parallele und kompositionelle Calls (IDs zum Zurueckmappen). Quelle: https://ai.google.dev/gemini-api/docs/function-calling · offiziell

## 6. Safety Settings & robuste Antwortpruefung
- 200 OK ≠ Text: VOR `response.text` `promptFeedback.blockReason`, `candidate.finishReason` (SAFETY/RECITATION/MAX_TOKENS) und `safetyRatings` pruefen, sauberen Fallback liefern. Quelle: https://ai.google.dev/gemini-api/docs/safety-settings · offiziell
- Schwellen pro Kategorie (Harassment/HateSpeech/SexuallyExplicit/Dangerous) bewusst setzen; harte Child-Safety-Schicht bleibt immer aktiv (auch bei `BLOCK_NONE`/`OFF`). Quelle: https://ai.google.dev/gemini-api/docs/safety-settings · offiziell

## 7. Context Caching & Token-Effizienz
- Grosse, wiederkehrende Inhalte (System-Instructions, Dokumente, Code) AN DEN PROMPT-ANFANG stellen → erhoeht Implicit-Cache-Trefferquote (2.5+ automatisch, Schwelle ~2.048-4.096 Tokens je Modell, 3.5 Flash: 4.096). Quelle: https://ai.google.dev/gemini-api/docs/caching · offiziell
- Bei garantierter Ersparnis Explicit Caching mit TTL nutzen; `usage_metadata`/`cachedContentTokenCount` monitoren. `system_instruction` fuer stabile Rolle/Regeln statt Wiederholung im User-Prompt. Quelle: https://ai.google.dev/gemini-api/docs/caching · offiziell

## 8. Streaming, Rate-Limits & Resilienz
- `streamGenerateContent` mit `?alt=sse` aufrufen und zeilenweise parsen (Default ist fortlaufendes JSON-Array); letzten Chunk auf `finishReason` pruefen. Quelle: https://ai.google.dev/gemini-api/docs/deprecations · offiziell
- Clientseitiges Rate-Limiting + Exponential-Backoff bei 429 (Free ~60 RPM/Modell); Billing aktivieren (auch fuer Free-Tier) und Timeout/Retry im SDK konfigurieren. Quelle: https://ai.google.dev/gemini-api/docs/libraries · offiziell

## 9. Embeddings: Batch-Verhalten ist MODELL-ABHÄNGIG (Stand 2026-07-08)
> **KRITISCH:** `gemini-embedding-001` und `gemini-embedding-2` verhalten sich bei einer Liste GEGENSÄTZLICH. Nie die Batch-Logik ungeprüft vom einen aufs andere übertragen (Aggregations-Falle → `bugs/apis/google-gemini-api.md §J23`).
- **`gemini-embedding-001` (Text, 1536 default):** `embed_content` nimmt MEHRERE `contents` in EINEM Call; `resp.embeddings` kommt in Eingabe-Reihenfolge zurück (N Vektoren). N Chunks NIE seriell — ein 150-Chunk-Dokument macht sonst 150 Round-Trips (Minuten) statt ~10 Batches (Sekunden). `task_type` (RETRIEVAL_DOCUMENT/RETRIEVAL_QUERY) als API-Parameter. Batch-Größe konservativ (z.B. 16), Vektor-ANZAHL gegen Eingabe prüfen, bei Mismatch HART abbrechen. Quelle: ai.google.dev/gemini-api/docs/embeddings · offiziell; live verifiziert 2026-07-02 (brain-api 1.20.0 `embed_many`).
- **`gemini-embedding-2` (multimodal, 3072 default, GA seit ~30.04.2026):** eine Liste `contents=[…]` liefert EINEN AGGREGIERTEN Vektor (NICHT N!) — für N separate Vektoren pro Text EINEN Call (parallelisierbar über ThreadPool) ODER die asynchrone Batch API (`asyncBatchEmbedContent`, ~halber Preis). `task_type` ist ENTFERNT → Absicht als Text-Präfix: Dokument `title: {Titel} | text: {Inhalt}` (ohne Titel `title: none`), Query `task: search result | query: {Suchtext}` (weitere: question answering / fact checking / code retrieval; symmetrisch: classification / clustering / sentence similarity). Default 3072 (empf. 768/1536/3072, MRL); <3072 wird NICHT auto-normalisiert (bei Cosine egal — Qdrant normalisiert intern). Input-Limit 8192 Token (4× `-001`). Dimensionswechsel erzwingt neue Vektor-Collection (bugs/server/qdrant.md §2). Quelle: ai.google.dev/gemini-api/docs/embeddings (NOTE + „Task types with Embeddings 2") · offiziell; Recherche 2026-07-08.

## 10. Sprache-zu-Text: Modellwahl und Live-API (Stand 2026-08-29)

**Erst das richtige Modell waehlen — das entscheidet mehr als jede Feineinstellung.**

| Aufgabe | Modell | Weg |
|---|---|---|
| Fertige Aufnahme (Datei liegt vor) | `gemini-3.5-transcribe` | `POST /v1beta/interactions` |
| Echtzeit ab Mikrofon, Live-Untertitel | `gemini-3.5-transcribe-live` | WebSocket `bidiGenerateContent` |

Google verweist im Live-Guide selbst weg vom Streaming: *"Read the Gemini Transcribe documentation
for non-streaming audio files."* Gemessen an derselben 64,5-s-Aufnahme: **4,4 s gegen 15,1 s**,
Wortfehlerrate **2,6 % gegen 4,0 %** (Groq Whisper large-v3-turbo: 4,6 %, aber ~7× guenstiger).
Ein Diktier-Overlay hat beim Transkribieren IMMER eine fertige Datei — dort ist der Live-Weg das
falsche Werkzeug und handelt sich nur dessen Pausen-Probleme ein.

**Batch-Weg (`interactions`) — die Regeln:**
- Audio **inline** als `data` (Base64) mitschicken statt ueber die Files-API: spart einen Roundtrip
  (4,4 s statt 5,7 s) und die Aufnahmen landen nicht fuer 48 h auf Google-Servern. Files-API erst
  bei grossen Dateien.
- `transcription_config.mode` auf `verbatim`, wenn Wortgetreue zaehlt — `smart` entfernt zwar
  Fuellwoerter und setzt Absaetze, laesst aber auch Woerter weg. Tempo ist identisch.
- Fachbegriffe ueber `custom_vocabulary` (bis 1000 Begriffe, laut Google beste Ergebnisse bis ~100).
- Antworttext steht in `steps[].content[].text`; `output_text` gibt es nur in den SDKs.

**Live-Weg — die Regeln, falls Streaming wirklich noetig ist:**
- Sprechpausen-Erkennung abschalten und Aktivitaet selbst markieren, sonst schneidet die API bei
  jeder Denkpause ab (Almanach §K26). Nicht auf `silenceDurationMs` verlassen.
- Sprache und Vokabular an `inputAudioTranscription`: `languageCodes` (ARRAY, BCP-47) und
  `customVocabulary`. `speechConfig` gibt es dort nicht.
- Zwischenstand (`interimInputTranscription`, kumulativ) und Endergebnis (`inputTranscription`)
  getrennt halten. `finished` ist kaputt — auf `generationComplete`/`turnComplete` plus
  Stille-Fenster setzen.
- 16 kHz / 16 bit / mono PCM ist das native Format; hoehere Abtastraten werden intern
  heruntergerechnet und bringen nichts.

**Ausfallsicherheit:** Free-Tier-Limits sind fuer diese Modelle nicht dokumentiert; 429 kann
jederzeit kommen. Einen zweiten Anbieter als Rueckfall vorsehen — aber nur bei TECHNISCHEN Fehlern.
Eine Aufnahme ohne Sprachinhalt ist ein gueltiges Ergebnis und darf nicht weitergereicht werden,
sonst liefert der Zweitanbieter eine halluzinierte Floskel. Dafuer eine eigene Ausnahmeklasse.

**Audioqualitaet:** Google nennt keinen dBFS-Zielbereich, nur qualitativ — Stoergeraeusche niedrig
halten, Mikro naeher ans Gesicht, Uebersteuerung vermeiden ("avoid severe clipping"). Reine
Lautstaerke-Normalisierung hebt Nutzsignal UND Rauschen und bringt wenig; echte Rauschunterdrueckung
bzw. AGC vor der Aufnahme ist der eigentliche Hebel.

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/google-gemini-api.md`) |
|---|---|
| 1 SDK & Client | A1, A2, A3, C8 |
| 2 Modelle & Versionierung | G17, G18 |
| 3 Thinking / Reasoning | B4, B5, F16 |
| 4 Structured Output | E13 |
| 5 Function Calling / Tools | E11, E12, E13 |
| 6 Safety & Antwortpruefung | D9, D10, B5 |
| 7 Context Caching & Token-Effizienz | (praeventiv, kein Bug) |
| 8 Streaming, Rate-Limits & Resilienz | I21, I22, C7, F15, H19, H20 |
| 9 Embeddings (modell-abhängig) | J23, J24, J25 |
