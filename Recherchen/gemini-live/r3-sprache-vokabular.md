# R3 — Sprache festlegen und eigenes Vokabular bei Gemini Live-Transkription (`gemini-3.5-transcribe-live`)

Stand der Recherche: 29.08.2026. Quellen: offizielle Google-AI-Doku (ai.google.dev), Live-API-Referenz, Vergleich mit js-genai/python-genai-Suche.

## Kernbefund vorab

Die im Auftrag gemessene Ablehnung erklärt sich direkt aus einer offiziellen Vergleichstabelle: **`gemini-3.5-transcribe-live` ("Live Transcription") ist ein eigenes, bewusst eingeschränktes Live-API-Profil, das sich von den normalen Dialog-/Agent-Modellen ("Live Agent") unterscheidet — und `systemInstruction` gehört explizit NICHT zu seinem Funktionsumfang.** `speechConfig` taucht in der gesamten Live-Transcribe-Dokumentation kein einziges Mal auf; es ist laut Live-API-Guide ausschließlich an `responseModalities: ["AUDIO"]` (Sprachausgabe/TTS) gekoppelt. Beide Felder sind damit für dieses Modell nicht dokumentiert — welches der beiden serverseitig konkret die Verbindung killt, lässt sich aus der Doku allein nicht trennen (siehe "Offen/unbelegt").

## a) Sprache der Eingangs-Transkription festlegen

**BESTÄTIGT:** Die Eingangssprache wird NICHT über `speechConfig.languageCode` gesetzt. Das dafür zuständige Feld ist `languageCodes` (Plural, Array!) direkt auf `inputAudioTranscription` bzw. dem Typ `AudioTranscriptionConfig`:

```json
"inputAudioTranscription": {
  "languageCodes": ["de-DE"]
}
```

Leeres Array `[]` (oder das Feld ganz weglassen) bedeutet automatische Spracherkennung inkl. Codemischung innerhalb einer Session ("mid-session code-mixing", 85+ Sprachen laut Modellseite). BCP-47-Codes wie `de-DE`, `es-ES`, `fr-FR` grenzen die Erkennung auf eine oder mehrere Sprachen ein.
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe, https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe

**BESTÄTIGT:** `speechConfig` ist laut Live-API-Capabilities-Guide ein Feld zur Konfiguration der Sprachausgabe (Stimme/Voice-Name plus Ausgabesprache), gekoppelt an `responseModalities: ["AUDIO"]`. Alle Beispiele im Guide zeigen `speechConfig` ausschließlich zusammen mit AUDIO-Ausgabe, nie mit einem reinen TEXT/Transkriptions-Setup. Es steuert also die Sprache der Modell-SPRACHAUSGABE (TTS), nicht die Erkennung der Eingangs-Sprache.
Quelle: https://ai.google.dev/gemini-api/docs/live-guide

**BESTÄTIGT (indirekt, per Fehlen):** Auf der Live-Transcribe-Seite kommt der Begriff `speechConfig` an keiner Stelle vor — weder im Parameter-Referenzabschnitt noch im vollständigen Setup-Beispiel. Das stützt die Einordnung, dass `speechConfig` für dieses Modell kein sinnvolles/unterstütztes Feld ist.
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

→ Für Deutsch als Eingangssprache: `inputAudioTranscription.languageCodes: ["de-DE"]` setzen, **kein** `speechConfig` verwenden.

## b) Eigenes Vokabular / Fachbegriffe vorgeben

**BESTÄTIGT:** Es gibt ein dediziertes Feature, funktional das Äquivalent zu PhraseSet/Boost bei Google Cloud Speech-to-Text v2: `customVocabulary` (bzw. `custom_vocabulary` in Python), ebenfalls ein Feld direkt auf `inputAudioTranscription`:

```json
"inputAudioTranscription": {
  "languageCodes": ["de-DE"],
  "customVocabulary": ["Groq", "Whisper", "Playwright"]
}
```

- Bis zu 1000 Begriffe erlaubt, beste Ergebnisse laut Doku aber typischerweise mit bis zu 100 Begriffen.
- Empfehlung der Doku: nur unübliche/eigenständige Begriffe eintragen (Marken, Eigennamen, Fachjargon) — keine Alltagswörter, weil das die Erkennung eher verschlechtert als verbessert.
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe, https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe

**BESTÄTIGT:** `systemInstruction` ist für dieses Modell explizit NICHT vorgesehen. Die Live-Transcribe-Doku enthält eine Vergleichstabelle "Live agent versus live Transcription" mit dem Wortlaut:

> Feature | Live Agent | Live Transcription
> Supported features | Function calling, Google Search, system instructions. | Speech biasing (`custom_vocabulary`), language detection, manual & hybrid VAD, Smart transcription.

D.h. Google selbst stellt `system instructions` als Live-Agent-exklusives Feature den transkriptionsspezifischen Features (u.a. `custom_vocabulary`) gegenüber — genau das Muster "Vokabular ja, Prompting nein" wie bei klassischen Speech-to-Text-APIs.
Quelle: https://ai.google.dev/gemini-api/docs/live-api/live-transcribe

→ Fachbegriffe/Eigennamen gehören NICHT in `systemInstruction`, sondern ausschließlich in `inputAudioTranscription.customVocabulary`.

**Abgrenzung zur Batch/REST-Transkription (nicht Live, zur Vermeidung von Verwechslung):** Bei der nicht-live REST-Variante (`gemini-3.5-transcribe` über `generateContent`) sitzt das gleiche Feature an anderer Stelle im JSON, nämlich unter `generation_config.transcription_config.custom_vocabulary` — nicht unter `inputAudioTranscription`. Für die Live-API (WebSocket, `bidiGenerateContent`) gilt aber die oben gezeigte Struktur direkt unter `inputAudioTranscription`.
Quelle: https://ai.google.dev/gemini-api/docs/transcribe (Batch-Variante) vs. https://ai.google.dev/gemini-api/docs/live-api/live-transcribe (Live-Variante)

## c) Welche Setup-Felder akzeptiert `*-transcribe-live`, welche nicht?

**BESTÄTIGT als unterstützt** (aus dem vollständigen Beispiel + Parameterreferenz der Live-Transcribe-Seite):
- `model`
- `generationConfig.responseModalities: ["TEXT"]`
- `inputAudioTranscription` mit den Unterfeldern `languageCodes`, `customVocabulary`, `mode` (u.a. `SMART`), und in `realtimeInputConfig` die VAD-Steuerung (`automaticActivityDetection.disabled` etc. — Detail liegt bei R1/R2)

**BESTÄTIGT als NICHT unterstützt / Live-Agent-exklusiv laut derselben Vergleichstabelle:**
- `systemInstruction`
- Function Calling / `tools`
- Google-Search-Grounding

**BESTÄTIGT als nicht unterstützt für die LIVE-Variante speziell** (im Unterschied zur Batch-Variante `gemini-3.5-transcribe`, laut Modellübersichtsseite):
- Word-level Timestamps ("Not Supported" für live, obwohl `AudioTranscriptionConfig` laut API-Referenz generell ein `wordTimestamp`-Feld kennt — vermutlich vom Live-Endpunkt ignoriert statt hart abgelehnt, siehe "Offen/unbelegt")
- Speaker Diarization (gleiche Einschränkung)
- Max. Audiodauer: 10 Minuten pro Session

**VERMUTUNG (nicht abschließend belegt):** Nicht dokumentierte/nicht passende Felder wie `systemInstruction` und `speechConfig` führen serverseitig nicht zu einer Fehlermeldung, sondern zum stillen Verbindungsabbruch ohne `setupComplete` — exakt das im Auftrag beschriebene Verhalten. Das deckt sich mit dem allgemeinen Live-API-Verhalten bei ungültigen Setups (in anderen Kontexten dokumentiert als Close-Code 1007 "invalid argument", teils aber auch als Abbruch ganz ohne Fehlertext), ist für die transcribe-live-Kombination speziell aber nicht mit einer offiziellen Fehlermeldungs-Referenz belegt.

## Empfohlenes, belegtes Setup-JSON (Deutsch + eigenes Vokabular)

```json
{
  "setup": {
    "model": "models/gemini-3.5-transcribe-live",
    "generationConfig": {
      "responseModalities": ["TEXT"]
    },
    "inputAudioTranscription": {
      "languageCodes": ["de-DE"],
      "customVocabulary": ["Groq", "Whisper", "Playwright"],
      "mode": "SMART"
    },
    "realtimeInputConfig": {
      "automaticActivityDetection": {
        "disabled": true
      }
    }
  }
}
```
`systemInstruction` und `speechConfig` bewusst weggelassen — beide gehören laut Doku nicht zum unterstützten Funktionsumfang von Live Transcription.

## Offen / unbelegt

- Nicht sicher belegt: OB `systemInstruction` allein, `speechConfig` allein, oder erst die Kombination beider Felder den Verbindungsabbruch im gemessenen Fall auslöst. Beide sind laut Doku "nicht Teil von Live Transcription" — ein sauberer A/B-Test (nur `systemInstruction` weglassen, `speechConfig` behalten und umgekehrt) wäre nötig, um serverseitig zu trennen, welches Feld tatsächlich zum harten Abbruch führt vs. nur ignoriert würde. Das konnte im Rahmen dieser Doku-Recherche nicht abschließend geklärt werden.
- Nicht belegt: der exakte Wortlaut/Inhalt einer Serverfehlermeldung speziell für diese Feldkombination bei `gemini-3.5-transcribe-live` (kein GitHub-Issue oder Forumsbeitrag mit genau diesem Repro gefunden).
- Nicht per Primärquelle (Proto-Referenz auf ai.google.dev/api/live) doppelt gegengeprüft, ob `wordTimestamp`/`diarization` bei einem Live-Transcribe-Setup zu einem harten Fehler führen oder nur stillschweigend ignoriert werden — Modellseite sagt nur "Not Supported", ohne Verhalten bei Angabe zu spezifizieren.
- Keine eigene Live-Verbindung getestet (reine Dokumentenrecherche) — alle Aussagen beruhen auf ai.google.dev, nicht auf eigenem Repro.

## Quellenliste

- Live Transcription mit Gemini Live API (Haupt-Referenz, inkl. Vergleichstabelle "Live agent versus live Transcription"): https://ai.google.dev/gemini-api/docs/live-api/live-transcribe
- Gemini 3.5 Transcribe Modellseite (Live- vs. Batch-Feature-Matrix): https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe
- Live API Capabilities Guide (speechConfig/Voice, System Instructions allgemein): https://ai.google.dev/gemini-api/docs/live-guide
- Audio-Transkription (Batch/REST-Variante, `transcription_config.custom_vocabulary`): https://ai.google.dev/gemini-api/docs/transcribe
- Live API WebSockets-Referenz (BidiGenerateContentSetup-Felder, AudioTranscriptionConfig): https://ai.google.dev/api/live
- Live translation mit Gemini Live API (zum Abgrenzen von `translationConfig.targetLanguageCode`, nicht Gegenstand dieses Auftrags): https://ai.google.dev/gemini-api/docs/live-api/live-translate
