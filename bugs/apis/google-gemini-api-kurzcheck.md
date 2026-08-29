# Google Gemini API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
| 10 | Live-API: nur Anfang transkribiert | Sprechpausen-Erkennung schneidet ab → `automaticActivityDetection.disabled` + `activityStart`/`activityEnd` | §K26 |
| 11 | Live-API: Client haengt, kein Text | Feld heisst `interimInputTranscription` (kumulativ!); `inputTranscription` ist das laengere Endergebnis | §K27, §K28 |
| 12 | Setup stumm abgelehnt (kein setupComplete) | Close-Grund lesen! `speechConfig` existiert nicht; Sprache/Vokabular an `inputAudioTranscription.languageCodes` (Array) / `.customVocabulary` | §K29 |
| 13 | Fertige Audiodatei transkribieren | NICHT das `-live`-Modell: `gemini-3.5-transcribe` ueber `POST /v1beta/interactions` (4,4 s statt 15,1 s). `generateContent` liefert dort leeren Text mit STOP | §K30 |
| 14 | Woerter fehlen im Transkript | `transcription_config.mode` auf `verbatim` — `smart` formuliert um und laesst weg | §K31 |
| 15 | 429 mitten im Betrieb | Free-Tier-Limits undokumentiert; auf Zweitanbieter ausweichen, aber NUR bei technischen Fehlern (nicht bei stiller Aufnahme) | §K32 |
