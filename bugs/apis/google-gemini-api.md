# Bekannte Bugs: Google Gemini API (Integration)

> PFLICHT-LESEN vor Arbeit an einer Gemini-API-Integration (Client-seitig).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Versions-Anker: NEUES einheitliches SDK `google-genai`
> (altes `google-generativeai`/`@google/generative-ai` deprecated seit 30.11.2025), Modelle Gemini
> 2.5/3.x. Zweite Seite: `best-practices/apis/google-gemini-api.md`.

> **Update 2026-07-02:** Keine neuen belastbaren `google-genai`-/`thinkingConfig`-/`finishReason`-Bugs seit 2026-06-08 gefunden. Bestaetigt bleibt der Deprecation-/Shutdown-Druck: alte 1.0/1.5- und Preview-Modelle sind weg; weitere 2.5-/Embedding-Varianten laufen 2026 aus. Modell-IDs weiter pinnen und Deprecations vor Releases pruefen.

> **Update 2026-08-29 (NEU Abschnitt K — Live API & Transcribe):** Neun-Researcher-Recherche plus
> eigene Messungen zu `gemini-3.5-transcribe-live` / `gemini-3.5-transcribe` (beide Preview seit
> 26.08.2026). Sechs selbst getroffene Fallen: (K26) die Sprechpausen-Erkennung schneidet fertig
> aufgenommenes Audio ab — Automatik muss AUS; (K27) das Transkript kommt als
> `interimInputTranscription`, kumulativ, und `inputTranscription` ist das laengere Endergebnis;
> (K28) `finished` ist serverseitig kaputt; (K29) `speechConfig` existiert nicht → stiller
> Verbindungsabbruch, Sprache/Vokabular gehoeren an `inputAudioTranscription`; (K30) fuer fertige
> Dateien ist `gemini-3.5-transcribe` ueber die **Interactions API** das richtige Modell (4,4 s
> statt 15,1 s, WER 2,6 % statt 4,0 %); (K31) der Smart-Modus laesst Woerter weg.

> **Update 2026-07-08 (NEU Abschnitt J — Embeddings):** Umstieg `gemini-embedding-001` → `gemini-embedding-2` (GA ~30.04.2026) hat drei echte Migrations-Fallen: (J23) eine Liste `contents=[…]` liefert bei Embedding 2 EINEN aggregierten Vektor statt N; (J24) `task_type` ist entfernt → Text-Präfixe; (J25) Dimensionswechsel (1536→3072) erzwingt eine neue Qdrant-Collection. Recherche 2026-07-08 (Firecrawl+MiniMax, offizielle Google-Doku).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ 200 OK + leerer Text / Crash | Vor `response.text`: blockReason + finishReason | §D10, §B5 |
| 2 | ⭐ `finishReason: MAX_TOKENS`, leer | Thinking frisst Budget — `maxOutputTokens` hoch | §B4 |
| 3 | SDK-Init bricht | Neues `google-genai`, `genai.Client(...)` | §A1, §A2 |
| 4 | 400 INVALID_ARGUMENT bei Tools | Schema auf OpenAPI-Subset, Nesting ≤ 32 | §E11, §E12 |
| 5 | ⭐ 404 NOT_FOUND auf Modell | IDs pinnen (nicht `-latest`); Deprecations | §G17, §G18 |
| 6 | 403 PERMISSION_DENIED trotz Key | Billing + API aktivieren, Key→Projekt | §C6 |
| 7 | Streaming liefert Muell | `?alt=sse` anhaengen, zeilenweise parsen | §I21 |
| 8 | API-Key uebergeben | Header `x-goog-api-key`, nie `?key=` Query | §C8 |
| 9 | ⭐ Migration `-001`→`gemini-embedding-2`: Liste liefert 1 statt N Vektoren | `contents=[…]` AGGREGIERT bei Embedding 2 → pro Text 1 Call; `task_type` entfernt → Präfixe; Dim 3072 → neue Collection | §J23-J25 |

---

## A. SDK-Wechsel (deprecated → google-genai)

### 1. Altes SDK ist tot
- **Symptom:** neue Features (Live API, thinkingConfig) fehlen, keine Bugfixes.
- **Ursache:** `google-generativeai` (Py) + `@google/generative-ai` (JS) deprecated seit 30.11.2025.
- **FIX:** auf `google-genai`/`@google/genai` migrieren (GA seit Mai 2025); Go: `google.golang.org/genai`.
- **Quelle:** https://ai.google.dev/gemini-api/docs/libraries

### 2. Client-Init geändert
- **Symptom:** Code mit `genai.GenerativeModel(...)`/`vertexai.init()` bricht.
- **FIX:** `client = genai.Client(api_key=...)` bzw. `genai.Client(vertexai=True, project=..., location=...)`. Config-Parsing für function calling/multi-turn/response separat durchgehen.
- **Quelle:** https://docs.cloud.google.com/vertex-ai/generative-ai/docs/deprecations/genai-vertexai-sdk

### 3. Vertex `generative_models` wird entfernt (24.06.2026)
- **FIX:** früh auf `google-genai` mit `vertexai=True` umstellen.

---

## B. Thinking-Tokens / Generation Config (KRITISCH bei 2.5/3)

### 4. Leere Antwort mit `finishReason: MAX_TOKENS` ⭐
- **Symptom:** 200 OK, leerer Text, finishReason MAX_TOKENS.
- **Ursache:** Gemini 2.5/3 Flash haben Thinking DEFAULT AN; thinking-Tokens zählen gegen `maxOutputTokens` (anders als OpenAI). Kleines Limit → Denken frisst alles, 0 Output.
- **Versionen:** gemini-2.5-flash/-pro, gemini-3-*-preview — per Design.
- **FIX:** `generationConfig.thinkingConfig.thinkingBudget = 1024` + `maxOutputTokens = 8192`; wo kein Reasoning nötig: `thinkingBudget: 0`. Im OpenAI-Endpoint `reasoning_effort: "none"` (geht NICHT bei 2.5 Pro / 3).
- **Quelle:** https://github.com/googleapis/python-genai/issues/782 · https://discuss.ai.google.dev/t/finishreason-max-tokens-but-text-is-empty/81874

### 5. Fehlendes/stilles `finishReason` bei Limit
- **Symptom:** Antwort still abgeschnitten, kein finishReason im letzten Chunk.
- **FIX:** IMMER `candidate.finishReason` UND Längenheuristik prüfen; bei MAX_TOKENS als unvollständig behandeln, ggf. mit höherem Budget neu.
- **Quelle:** https://discuss.ai.google.dev/t/gemini-2-5-api-bug-missing-finishreason-when-max-token-limit-is-reached/75837

---

## C. Auth & Quota

### 6. 403 PERMISSION_DENIED trotz neuem Key
- **Ursache:** Billing NICHT aktiviert (auch für Free-Tier nötig) ODER `generativelanguage.googleapis.com` nicht aktiviert ODER Key nicht zum Projekt verknüpft.
- **FIX:** Billing aktivieren, API einschalten, Key dem richtigen Projekt zuordnen.
- **Quelle:** https://discuss.ai.google.dev/t/gemini-api-returns-403-permission-denied-even-with-a-new-paid-project-and-new-api-key/166965

### 7. 429 → eskaliert zu 403
- **FIX:** clientseitiges Rate-Limiting + Backoff; Quota in GCP erhöhen. Free default ~60 RPM/Modell.

### 8. API-Key-Übergabe sicher
- **FIX:** Key per `x-goog-api-key`-Header (nicht `?key=` im Query — landet in Logs/Referer); Quota-Projekt ggf. `x-goog-user-project`.

---

## D. Safety / Content Filtering

### 9. `BLOCK_NONE` blockt trotzdem
- **Ursache:** senkt nur die Schwelle der konfigurierbaren Kategorien; harte, nicht abschaltbare Schicht (CSAM etc.) bleibt.
- **FIX:** akzeptieren, dass manche Prompts nie generiert werden; defensiv behandeln (#10).
- **Quelle:** https://ai.google.dev/gemini-api/docs/safety-settings

### 10. 200 OK + leerer `text` (kein Error) ⭐
- **Symptom:** `response.text` wirft ValueError (Py) / ist undefined (JS).
- **Ursache:** finishReason SAFETY/RECITATION → keine Candidates mit Text.
- **FIX:** vor Textzugriff `promptFeedback.blockReason` + `candidate.finishReason` + `safetyRatings` prüfen; bei Block sauberen Fallback liefern.

---

## E. Function Calling / Tools

### 11. 400 INVALID_ARGUMENT bei Schema
- **Ursache:** nur OpenAPI-Subset. Erlaubt: `type, nullable, required, format, description, properties, items, enum`. NICHT: `default, optional, maximum, oneOf`; `anyOf` z. T. nicht (2.0 Flash).
- **FIX:** Schema auf Subset reduzieren; volle JSON-Schema-Features in `description` beschreiben.
- **Quelle:** https://ai.google.dev/gemini-api/docs/function-calling

### 12. Schema zu groß/tief
- **Ursache:** max Nesting 32, `$defs`-Rekursion auf 2 begrenzt.
- **FIX:** Property-Namen kürzen, Verschachtelung/Anzahl reduzieren.

### 13. FunctionCalling- vs. responseSchema-Divergenz
- **FIX:** Schemas getrennt validieren, nicht 1:1 wiederverwenden.

---

## F. OpenAI-Kompatibilitäts-Endpunkt (`/v1beta/openai/`)

### 14. `/responses` fehlt
- **Ursache:** Kompat bietet nur `/chat/completions`, nicht OpenAIs Responses API.
- **FIX:** auf Chat-Completions-Pfad festlegen.

### 15. `usage` in JEDEM Stream-Chunk (statt nur letztem)
- **FIX:** Client tolerant machen, nur letzten Wert nehmen.

### 16. Reasoning nicht abschaltbar bei 2.5 Pro / 3
- **FIX:** höheres `max_tokens` einplanen; `thinking_config` via `extra_body`.

---

## G. Modellnamen / Deprecations

### 17. 404 NOT_FOUND auf altem Modell ⭐
- **Ursache:** 1.0/1.5 abgeschaltet; 2.0-flash(-lite) Shutdown 01.06.2026.
- **FIX:** auf `gemini-2.5-flash(-lite)` migrieren; 2.5-flash Shutdown 16.10.2026 → ggf. direkt `gemini-3.5-flash`.
- **Quelle:** https://ai.google.dev/gemini-api/docs/deprecations

### 18. Preview-Modelle verschwinden
- **FIX:** in Produktion stabile, versionierte IDs pinnen (nicht `-latest`); Deprecation-Seite überwachen.

---

## H. Multimodal / File API

### 19. Inline-Bild scheitert über Größe
- **FIX:** ab ~20 MB die File API nutzen (bis 2 GB/Datei, 48 h gespeichert), nicht inline base64.
- **Quelle:** https://ai.google.dev/gemini-api/docs/file-input-methods

### 20. Fehlender/falscher `mimeType`
- **FIX:** bei inline-Daten korrekten MIME-Type explizit setzen.

---

## I. Streaming

### 21. SSE vs. JSON-Array ⭐
- **Symptom:** Stream-Parsing liefert Müll/einen Block.
- **Ursache:** `streamGenerateContent` gibt per Default ein fortlaufendes JSON-Array; nur mit `?alt=sse` echte SSE.
- **FIX:** `?alt=sse` anhängen und zeilenweise parsen, sonst JSON-Array-Streaming-Parser.

### 22. Abgeschnittene Streaming-Antwort
- **FIX:** letzten Chunk auf finishReason prüfen, Chunks akkumulieren, bei MAX_TOKENS Budget erhöhen.

---

## J. Embeddings (`gemini-embedding-2` Migrations-Fallen)

> Kontext: Umstieg eines produktiven RAG-Speichers von `gemini-embedding-001` (Text, 1536) auf
> `gemini-embedding-2` (multimodal, 3072, GA ~30.04.2026). Recherche 2026-07-08 (offizielle Google-Doku).

### 23. ⭐ Liste → EIN aggregierter Vektor (Batch-Falle)
- **Symptom:** Nach Umstieg auf `gemini-embedding-2` liefert `embed_content(contents=[t1,t2,t3])` nur 1 Vektor statt 3 → alle Chunks bekommen denselben (verschmolzenen) Vektor, ODER ein Anzahl-Check bricht hart ab (`N Vektoren fuer M Texte`) und das Speichern schlaegt fehl.
- **Ursache:** Offizielle Doku (NOTE): „While `gemini-embedding-001` lets you generate individual embeddings for a list of strings, **Gemini Embedding 2 produces a single aggregated embedding for multiple inputs**." Die Liste wird bei Embedding 2 zu EINEM fusionierten Vektor aggregiert (gedacht fuer multimodale Fusion Text+Bild+Audio), nicht zu N Einzelvektoren wie bei `-001`.
- **Versionen:** `gemini-embedding-2` (GA ~30.04.2026). Bei `-001` unveraendert N Vektoren pro Liste.
- **FIX (funktionserhaltend):** Fuer N separate Dokument-Vektoren pro Text EINEN `embed_content`-Call (parallelisierbar ueber ThreadPool) ODER die asynchrone Batch API (`models.asyncBatchEmbedContent`, ~halber Preis, hoher Durchsatz). Vektor-Anzahl weiter gegen die Eingabe pruefen (bei Mismatch HART abbrechen). NIE die `-001`-Batch-Logik (`embed_many`) ungeprueft auf Embedding 2 uebertragen.
- **Quelle:** ai.google.dev/gemini-api/docs/embeddings (NOTE in `embeddings.md.txt`) · offiziell; Recherche 2026-07-08.

### 24. `task_type` bei `gemini-embedding-2` entfernt (still ignoriert) → Praefixe
- **Symptom:** `EmbedContentConfig(task_type="RETRIEVAL_DOCUMENT"|"RETRIEVAL_QUERY")` wirkt bei `gemini-embedding-2` nicht (gleicher Vektor unabhaengig vom task_type); die asymmetrische Suche (Dokument ≠ Anfrage) verschlechtert sich unbemerkt.
- **Ursache:** `task_type` ist bei Embedding 2 deprecated/entfernt; die Absicht wird ueber Text-Praefixe ausgedrueckt (die Doku erwaehnt den Parameter nicht mehr, ein Bugreport meldet stilles Ignorieren).
- **FIX:** Dokument → `title: {Titel} | text: {Inhalt}` (ohne Titel `title: none`); Anfrage → `task: search result | query: {Suchtext}` (weitere Tasks: question answering / fact checking / code retrieval; symmetrisch: classification / clustering / sentence similarity). Bei `-001` bleibt `task_type` als API-Parameter gueltig → im Code modell-abhaengig verzweigen (nicht hart entfernen, damit der Fallback auf `-001` funktioniert).
- **Quelle:** ai.google.dev/gemini-api/docs/embeddings (Abschnitt „Task types with Embeddings 2") · offiziell; Recherche 2026-07-08.

### 25. Dimensionswechsel erzwingt neue Qdrant-Collection (+ 8192-Token-Chunks)
- **Symptom:** Nach `SB_EMBED_DIMS 1536→3072` bricht der Upsert (`could not broadcast … into shape`), obwohl nur die Env geaendert wurde.
- **Ursache:** Qdrant fixiert die Vektordimension bei Collection-Erstellung (bugs/server/qdrant.md §2). `_init_store()` legt eine Collection nur an, wenn sie NOCH NICHT existiert — ein Modell-/Dim-Wechsel ist daher keine reine Config-Aenderung.
- **FIX:** Neue Collection mit `size=3072` (Blau/Gruen: parallel befuellen, `points_count` verifizieren, Cutover per Env, alte Collection als Rueckweg behalten). Chunk-Groesse darf auf das 8192-Token-Limit von Embedding 2 angehoben werden (vs. 2048 bei `-001`) → weniger Chunks/Calls (gleicht die Einzel-Calls aus §J23 aus). 3072 = doppelter Vektor-RAM; bei wenigen Tausend Punkten unkritisch, sonst `on_disk`+Quantisierung (qdrant §1).
- **Quelle:** ai.google.dev/gemini-api/docs/embeddings + bugs/server/qdrant.md §1/§2 · Recherche 2026-07-08.

---

## K. Live API & Transcribe (Sprache-zu-Text, Stand 2026-08-29)

> Recherche 2026-08-29 (Engine C: Sonnet-5-Schwarm, 9 Researcher) PLUS eigene Messungen gegen die
> echte API mit `gemini-3.5-transcribe-live` / `gemini-3.5-transcribe`. Beide Modelle sind seit
> 26.08.2026 in Preview. Getroffen im Projekt `TerminalVoiceOverlay-Windows` (v1.7 → v1.10).

### 26. ⭐⭐ Live-API schneidet bei Sprechpausen ab — Automatik MUSS aus
**Symptom:** Eine fertig aufgenommene Datei wird am Stueck (schneller als Echtzeit) in die Live-API
geschoben; nach 2–3 s kommt `generationComplete` und nur der ANFANG des Textes. Bei einer 57-s-
Aufnahme kamen 27 Zeichen zurueck.
**Ursache:** Die Live-API ist auf Echtzeit-GESPRAECH ausgelegt. Ihre Sprechpausen-Erkennung (VAD)
schliesst den Zug bei der ersten Pause ab und verwirft alles Weitere. Der Standardwert fuer
`silenceDurationMs` ist **100 ms** — beim Einspeisen im Zeitraffer sieht die API jede Denkpause
sofort.
**Messung:** 36,7-s-Aufnahme mit 12 s Pause in der Mitte → mit Automatik 61 Zeichen, ohne Automatik
die vollen 330 Zeichen.
**Fix:** `setup.realtimeInputConfig.automaticActivityDetection.disabled = true`, danach Anfang und
Ende des Sprechens von Hand markieren: `{"realtimeInput":{"activityStart":{}}}` → Audio →
`{"realtimeInput":{"activityEnd":{}}}`.
**NICHT stattdessen** `silenceDurationMs` hochsetzen: laut js-genai#1467, cookbook#1263 und #1262
ignoriert der Server den konfigurierten Wert bei den 3.x-Live-Modellen teilweise und wendet einen
kuerzeren internen Timeout an. Nur das Abschalten umgeht die serverseitige Stille-Uhr wirklich.
**Doku-Widerspruch (beachten):** Laut API-Referenz schliessen `activityStart`/`activityEnd`
(nur bei ABgeschalteter Automatik) und `audioStreamEnd` (nur bei EINgeschalteter) einander aus.
Gemessen funktioniert das Senden beider; ob das dauerhaft unschaedlich ist, ist unbelegt.

### 27. ⭐ Falsches Transkript-Feld → Client haengt bis zum Timeout
**Symptom:** Setup klappt (`setupComplete` kommt), Audio geht raus, aber der Client sammelt nie Text
ein und laeuft in seinen Timeout; im UI bleibt der Verarbeiten-Zustand stehen.
**Ursache:** Das laufende Zwischenergebnis kommt als `serverContent.interimInputTranscription` —
NICHT als das naheliegende `inputTranscription`. Wer nur letzteres liest, sieht nichts.
**Zweite Falle:** Der Zwischenstand ist **kumulativ** (jedes Frame enthaelt den gesamten bisherigen
Text, teils mit rueckwirkend geaenderter Gross-/Kleinschreibung). Anhaengen vervielfacht den Text —
es muss ERSETZT werden.
**Dritte Falle:** `inputTranscription` (ohne "interim") ist das FERTIGE Ergebnis und faellt
regelmaessig laenger aus als der letzte Zwischenstand (gemessen 330 gegen 218 Zeichen). Beide in
denselben Topf zu werfen verschluckt Text. Getrennt halten: fertige Abschnitte aneinanderreihen,
Zwischenstand nur als Rueckfallebene.

### 28. `finished`-Flag der Transkription ist serverseitig kaputt
**Symptom:** Das `finished`-Feld am `Transcription`-Objekt ist immer `null`/`undefined`, obwohl es
im Schema steht (neben `text`, `languageCode`, `speakerLabel`, `words`).
**Belege:** python-genai#1504 (als "not planned" geschlossen), js-genai#1429 (offen).
**Fix:** Nicht auf `finished` warten. Als Abschluss `generationComplete` bzw. `turnComplete`
nehmen — und zusaetzlich ein Stille-Fenster als Sicherheitsnetz, weil es Faelle gibt, in denen
gar kein Schlusssignal kommt (eigene Messung ohne `activityEnd`: der Strom verstummt einfach).

### 29. `speechConfig` existiert bei Transcribe-Modellen nicht → stiller Verbindungsabbruch
**Symptom:** Das Setup wird abgelehnt, der Server schickt kein `setupComplete` und schliesst die
Verbindung — beim naiven Client sieht das aus wie ein Netzproblem.
**Ursache:** `speechConfig` gehoert zur Sprach-AUSGABE (TTS) und ist bei `*-transcribe-live` kein
gueltiges Setup-Feld. Der Close-Grund nennt es woertlich:
`Invalid JSON payload received. Unknown name "speechConfig" at 'setup': Cannot find field.`
Dieselbe Falle bei `inputAudioTranscription.languageCode` (Singular).
**Fix:** Den WebSocket-Close-Grund IMMER auslesen und loggen — er benennt das falsche Feld exakt.
Sprache und Vokabular gehoeren an `inputAudioTranscription`:
`{"inputAudioTranscription":{"languageCodes":["de-DE"],"customVocabulary":["Groq","Whisper"]}}`
(`languageCodes` ist ein ARRAY). `systemInstruction` wird zwar akzeptiert, ist laut Google aber
nicht Teil der Live-Transkription und blieb in der Messung wirkungslos.

### 30. Falsches Modell fuer fertige Aufnahmen (Live statt Batch)
**Symptom:** Umstaendlicher WebSocket-Code, Pausen-Probleme, verschluckte Woerter, langsam.
**Ursache:** Es gibt ZWEI Modelle. `gemini-3.5-transcribe-live` (nur `bidiGenerateContent`) ist
fuer Echtzeit-Streaming; `gemini-3.5-transcribe` ist fuer fertige Dateien und laeuft ueber die
**Interactions API** (`POST /v1beta/interactions`) — nicht ueber `generateContent`. Ein
`generateContent`-Aufruf gegen das Batch-Modell liefert `finishReason: STOP` mit LEEREM Text
(kein Fehler! — deshalb schwer zu erkennen).
**Messung an derselben 64,5-s-Aufnahme:** Live 15,1 s, Batch 4,4 s. Wortfehlerrate laut Artificial
Analysis 4,0 % gegen 2,6 % (Groq Whisper large-v3-turbo: 4,6 %).
**Fix:** Fuer fertige Aufnahmen immer das Batch-Modell. Text steht in `steps[].content[].text`
(ein `output_text` gibt es nur in den SDKs, nicht in der REST-Antwort).

### 31. Smart-Modus laesst Woerter weg
**Symptom:** Einzelne Woerter fehlen im Transkript, obwohl deutlich gesprochen.
**Ursache:** `transcription_config.mode = {"type":"smart"}` (Standard) entfernt Fuellwoerter, setzt
Absaetze — und formuliert dabei um. Gemessen wurde aus "Ich frage mich" ein "Frage mich".
**Fix:** Wo Wortgetreue zaehlt, `{"type":"verbatim"}` setzen. Kostet nichts an Tempo (gemessen
4,3 s gegen 4,4 s). Fuellwoerter kann eine nachgelagerte Textkorrektur entfernen.

### 32. Free-Tier-Kontingent ohne dokumentierte Grenze → 429 mitten im Betrieb
**Symptom:** `429 You exceeded your current quota` nach wenigen Aufrufen in kurzer Folge.
**Ursache:** RPM/RPD des Free Tier sind fuer die Transcribe-Modelle NICHT oeffentlich dokumentiert
(nur im auth-geschuetzten AI-Studio-Dashboard sichtbar). Beim Messen mit ~12 Aufrufen in wenigen
Minuten getroffen.
**Fix:** 429 abfangen und auf einen zweiten Anbieter ausweichen, statt die Aufnahme zu verlieren.
Wichtig: NUR bei technischen Fehlern ausweichen — eine stille Aufnahme ist ein gueltiges Ergebnis
und darf nicht an den Zweitanbieter gehen (sonst halluziniert der eine Floskel). Dafuer eine eigene
Ausnahmeklasse statt einer allgemeinen.

### 33. Weitere bestaetigte Live-API-Bugs (nicht selbst getroffen)
- **Leere/fehlende Transkriptionsfelder** trotz korrekter Konfiguration: js-genai#1212, #478,
  python-genai#1279 — meist nur durch Modellwechsel "geloest".
- **Vorzeitiger `turnComplete` mitten im Satz**, serverseitig: python-genai#2117, OFFEN, ~40
  betroffene Entwickler.
- **Wortweise zerhackte Transkripte** ueber viele Frames: cookbook#951, #1197 (bei Interrupts).
- **Halluzinationen bei Stille** und VAD-Turn-Thrashing: cookbook#1262 + Forum-Thread.
- **WebSocket 1007/1011** im Transkriptionskontext mehrfach dokumentiert.

### 34. Session-Limit 10 Minuten (Transcribe-Live)
`gemini-3.5-transcribe-live` hat ein hartes 10-Minuten-Limit pro Session — kuerzer als die
15 Minuten der Live-Dialogmodelle. Session Resumption/Context Window Compression sind fuer dieses
Modell nicht dokumentiert. Das Kontextfenster ist nicht der Engpass (25 Tokens/s Audio bei
131.072 Token Limit ≈ 87 Minuten). Bei laengeren Aufnahmen Session-Neuaufbau einplanen.

## Fix-Status (Stand 2026-06-08)

| Frueherer Zustand | Status | Bezug |
|---|---|---|
| `google-generativeai`/`@google/generative-ai` | **deprecated** 30.11.2025 | Bug 1 — auf `google-genai` migrieren |
| Gemini 1.0/1.5 | **abgeschaltet** | Bug 17 |
| Gemini 2.0-flash(-lite) | Shutdown **01.06.2026** | Bug 17 |

**Noch NICHT gefixt / per Design:** Thinking im Output-Budget (4), 200-OK-leerer-Text (10), OpenAPI-Subset-Schema (11), `?alt=sse`-Streaming (21), usage-pro-Chunk im Kompat-Layer (15).

**Ehrlichkeits-Hinweis:** Einige Fix-Details stammen aus GitHub-Issues/Foren der `python-genai`, nicht alle aus offizieller Doku.

---

## Pflicht-Checkliste vor Gemini-Integration

- [ ] Neues `google-genai`-SDK?
- [ ] `thinkingBudget` gesetzt + `maxOutputTokens` großzügig?
- [ ] Vor `response.text` blockReason + finishReason + safetyRatings geprüft (kein blinder Zugriff)?
- [ ] Function-Schema auf OpenAPI-Subset reduziert, Nesting ≤ 32?
- [ ] Modell-ID gepinnt (nicht `-latest`), Deprecations geprüft?
- [ ] Key per Header (nicht Query), Billing aktiviert?
- [ ] Streaming mit `?alt=sse`?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/google-gemini-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
