# Google Gemini API — Best Practices (Stand 2026-06-09)

> Gegenstueck zu `bugs/apis/google-gemini-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09.)

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

## 9. Embeddings: Batchen statt seriell (Stand 2026-07-02)
- `embed_content` nimmt MEHRERE `contents` in EINEM Call entgegen; `resp.embeddings` kommt in Eingabe-Reihenfolge zurueck. N Texte (z.B. Dokument-Chunks) NIE seriell je Text embedden — ein 150-Chunk-Dokument macht sonst 150 Round-Trips (Minuten) statt ~10 Batches (Sekunden). Identische Vektoren (gleiche Inputs, gleiches `task_type`, gleiche `output_dimensionality`). Quelle: ai.google.dev/gemini-api/docs/embeddings (Batch-Beispiele) · offiziell; live verifiziert 2026-07-02 (brain-api 1.20.0 `embed_many`, 3-Chunk-Store 0,66s, 1:1-Roundtrip + Suche ok)
- Batch-Groesse konservativ halten (z.B. 16 je Request) und die Vektor-ANZAHL gegen die Eingabe-Anzahl pruefen — bei Mismatch HART abbrechen statt Vektoren still falsch zuzuordnen.

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
