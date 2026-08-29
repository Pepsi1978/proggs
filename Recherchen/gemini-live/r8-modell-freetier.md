# R8 — Gemini 3.5 Transcribe / Transcribe-Live: Modell, Kontingente, Preise, Qualität

Stand der Recherche: 29.08.2026. Die Modelle wurden erst am **26.08.2026** angekündigt (öffentliche Preview) — die Informationslage ist entsprechend frisch und teils lückenhaft.

## a) Was ist über die Modelle bekannt

**BESTÄTIGT** — Google hat am 26.08.2026 zwei getrennte Modelle vorgestellt:

- **`gemini-3.5-transcribe`** — für vorab aufgenommene Audiodateien ("pre-recorded audio processing"), läuft über die **Interactions API** (Unary/Batch-Aufruf, kein Streaming). Unterstützt Sprecher-Zuordnung (Diarization) und wortgenaue Zeitstempel.
- **`gemini-3.5-transcribe-live`** — für "continuous, bidirectional streaming with sub-second latency" über die **Live API** (WebSocket). Gedacht für interaktive Sprachanwendungen, Live-Untertitel, schnelle Diktierfunktionen.

Quelle: [Intelligent transcription with Gemini 3.5 Transcribe (Google-Blog)](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/)

**Sprachen (BESTÄTIGT):** Automatische Erkennung und Transkription in **85+ Sprachen**, inklusive regionaler Akzente/Dialekte. Deutsch ist ausdrücklich in der Sprachliste beider Modelle (mit BCP-47-Code) enthalten. Quelle: [blog.google](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/), [ai.google.dev/gemini-api/docs/live-api/live-transcribe](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe). Eine sprachspezifische WER-Aufschlüsselung für Deutsch war in keiner der geprüften Quellen zu finden (siehe "Offen/unbelegt").

**Genauigkeit / WER (BESTÄTIGT, gemessen von Artificial Analysis):**
- `gemini-3.5-transcribe` (non-streaming): **2,6 %** durchschnittlicher WER über 85+ Sprachen; FLEURS-Benchmark 5,04 % WER.
- `gemini-3.5-transcribe-live` (streaming): **4,0 %** durchschnittlicher WER; FLEURS-Benchmark 5,50 % WER.
- Latenzverbesserung ggü. Vorgänger Chirp 3: 70 % schneller bis zur finalen Transkription.

Quellen: [blog.google](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/), [MarkTechPost](https://www.marktechpost.com/2026/08/27/google-ai-releases-gemini-3-5-transcribe-a-speech-to-text-model-reporting-2-6-average-wer-across-85-languages/)

**Vergleich zu Whisper (BESTÄTIGT, Artificial Analysis Leaderboard, Stand 29.08.2026):**

| Modell | Anbieter | WER | Speed-Faktor | Preis/1000 Min. |
|---|---|---|---|---|
| Gemini 3.5 Transcribe | Google | 2,6 % | 89,7× | $5,00 |
| Scribe v2 | ElevenLabs | 2,2 % | 52,6× | $3,67 |
| Smallest AI Pulse Pro | Smallest.ai | 2,4 % | 273,1× | $4,00 |
| Universal-3 Pro | AssemblyAI | 3,1 % | 100,2× | $3,50 |
| Whisper Large v3 Turbo | Groq | 4,6 % | 115,6× | $0,67 |
| Nova-3 | Deepgram | 5,2 % | 530,6× | $4,30 |

Quelle: [Artificial Analysis Speech-to-Text Leaderboard](https://artificialanalysis.ai/speech-to-text)

Damit ist Gemini 3.5 Transcribe im WER klar besser als Groq/Whisper Large v3 Turbo (2,6 % vs. 4,6 %), aber Groq bleibt bei reiner Rohgeschwindigkeit (Speed-Faktor) und vor allem beim Preis (13. des Preises) konkurrenzfähig. **Wichtige Einschränkung:** Die Werte sind ein "audio-duration-weighted average" über überwiegend englischsprachige Datensätze — keine gesicherte Aussage für Deutsch speziell (Lücke, siehe unten).

Eine Analyse (orcarouter.ai) ordnet das so ein: *"Whisper Large v3 Turbo remains the right default for English batch transcription at scale, for anything that must run on your own infrastructure, and for teams that want a frozen, auditable artifact. Gemini 3.5 Transcribe is the right call when the transcript has to be more than words — speaker labels, clean formatting, domain vocabulary, multilingual coverage, or an agent hook."* Quelle: [orcarouter.ai](https://www.orcarouter.ai/blog/gemini-3-5-transcribe-vs-whisper-large-v3-turbo) (Sekundärquelle/Analyse, keine Google-Primärquelle — als Einordnung, nicht als Fakt zu werten).

## b) Free Tier und Preise

**BESTÄTIGT** (offizielle Pricing-Seite, ai.google.dev/gemini-api/docs/pricing):

- **Free Tier: Für beide Modelle "Free of charge"** — sowohl Input (Audio) als auch Output (Text) sind im kostenlosen Tier ohne Gebühr. Dies gilt für die öffentliche Preview über Google AI Studio.
- **Paid Tier `gemini-3.5-transcribe`:** Input $2,00 / Mio. Audio-Token bzw. $0,003/Min. Audio; Output $12,00 / Mio. Text-Token bzw. $0,002/Min. Text. (Eine sekundäre Quelle — CloudPrice/Andere — nennt geringfügig andere Zahlen von $2,50/Mio.; die offizielle Pricing-Seite mit $2,00/Mio. ist hier maßgeblich.)
- **Paid Tier `gemini-3.5-transcribe-live`:** Input $3,50 / Mio. Audio-Token bzw. $0,005/Min. Audio; Output $21,00 / Mio. Text-Token bzw. $0,004/Min. Text.

Quelle: [ai.google.dev/gemini-api/docs/pricing](https://ai.google.dev/gemini-api/docs/pricing)

**RPM/RPD/gleichzeitige Sessions im Free Tier: NICHT ÖFFENTLICH DOKUMENTIERT** (siehe "Offen/unbelegt" unten) — die generische Rate-Limits-Seite (ai.google.dev/gemini-api/docs/rate-limits) listet nur Batch-Limits für Text-/Multimodal-Modelle, verweist aber für aktuelle, projektspezifische Limits auf das eingeloggte Dashboard `aistudio.google.com/rate-limit`, das ohne Authentifizierung nicht abrufbar war. Da die Modelle erst 3 Tage alt sind (Stand dieser Recherche), könnten sich Zahlen zudem noch ändern.

**Datei-Limits (BESTÄTIGT):**
- Maximale Datei über Files API: 2 GB pro Datei, 20 GB Gesamtspeicher pro Projekt, Lebensdauer 48 Stunden (automatisches Löschen). Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i) (inoffizieller, aber von einem Google-AI-Account veröffentlichter Guide — als BESTÄTIGT markiert, aber nicht die Primär-Docs-Seite selbst).
- Maximale Audiolänge: **bis zu 1 Stunde pro Anfrage** im Standard-Modus; **auf 30 Minuten begrenzt**, sobald Diarization oder Word-Level-Timestamps aktiviert sind. Quelle: [ai.google.dev/gemini-api/docs/transcribe](https://ai.google.dev/gemini-api/docs/transcribe).
- Live-API-Sessions: **maximal 10 Minuten durchgehendes Streaming pro Session.** Quelle: [ai.google.dev/gemini-api/docs/live-api/live-transcribe](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe).

## c) Empfohlener Anwendungsfall je Modell — und der synchrone Weg im Detail

**BESTÄTIGT, wörtliches Zitat:** Der Live-Transcribe-Guide verweist ausdrücklich auf das synchrone Modell für nicht-streamende Dateien:

> „Read the Gemini Transcribe documentation for non-streaming audio files."

— [ai.google.dev/gemini-api/docs/live-api/live-transcribe](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe)

Umgekehrt verweist die Transcribe-Dokumentation (synchron) bei Bedarf an Echtzeit-Streaming zurück auf die Live-API:

> „For real-time, low-latency streaming speech recognition from a microphone or live audio stream, see Live transcription using the Live API and `gemini-3.5-transcribe-live`."

— [ai.google.dev/gemini-api/docs/transcribe](https://ai.google.dev/gemini-api/docs/transcribe)

**Für deinen Anwendungsfall (fertige WAV-Datei, 15–90 s, kein Streaming nötig) ist damit klar `gemini-3.5-transcribe` über die Interactions API das von Google selbst empfohlene Modell — nicht die Live-Variante.**

### Vollständiger, belegter Ablauf des synchronen Wegs

**1. Datei hochladen (Files API)** — empfohlen für alles außer sehr kurzen Clips:

> „Use the Files API for large recordings: For files longer than a few seconds, upload the file using `client.files.upload` and pass the returned file URI to the model."

— [ai.google.dev/gemini-api/docs/transcribe](https://ai.google.dev/gemini-api/docs/transcribe)

Für 15–90-Sekunden-WAV-Dateien ist das eindeutig der empfohlene Weg (nicht Base64-inline).

**2. Python-Beispiel (offizielle Doku):**

```python
from google import genai

client = genai.Client()
audio_file = client.files.upload(file="path/to/sample.mp3")

interaction = client.interactions.create(
    model="gemini-3.5-transcribe",
    input=[
        {
            "type": "audio",
            "uri": audio_file.uri,
            "mime_type": audio_file.mime_type,
        }
    ],
)
print(interaction.output_text)
```

**3. REST/cURL-Beispiel (offizielle Doku):**

```bash
curl -X POST "https://generativelanguage.googleapis.com/v1beta/interactions" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.5-transcribe",
    "input": [
      {
        "type": "audio",
        "uri": "YOUR_FILE_URI",
        "mime_type": "audio/mp3"
      }
    ]
  }'
```

Endpunkt: `https://generativelanguage.googleapis.com/v1beta/interactions` (POST). Für WAV wäre `mime_type` entsprechend `"audio/wav"` zu setzen.

**4. Antwortformat:** Der transkribierte Text steht in `interaction.output_text`. Bei aktivierten Zusatzfeatures (Diarization, Word-Timestamps) liegen die Detail-Annotationen in `interaction.steps[].content[].annotations[]`, jedes Wort-Objekt mit `text`, `start_offset`, `end_offset`, optional `speaker` (z. B. `"spk:0"`).

**5. Optionale Konfiguration** (Custom Vocabulary, Smart- vs. Verbatim-Modus) — Beispiel aus dem googleai-Guide (dev.to, siehe unten für Einordnung als sekundär-aber-autoritativ):

```python
interaction = client.interactions.create(
    model="gemini-3.5-transcribe",
    input=[{"type": "audio", "uri": audio_file.uri}],
    generation_config={
        "transcription_config": {
            "mode": {"type": "smart"},
            "custom_vocabulary": ["ScaNN", "Qdrant", "Cilium"]
        }
    },
)
```

Bis zu **1.000 Custom-Vocabulary-Begriffe** pro Anfrage (Eigennamen, Fachbegriffe, Akronyme — keine Phrasen/Kontextregeln). Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i).

**6. Maximale Dateigröße/-länge:** siehe oben (b) — 2 GB / bis 1 h (30 Min. mit Diarization/Timestamps aktiv), für dein 15–90-s-Diktat also weit im grünen Bereich.

**Free Tier für den synchronen Weg:** Ja — laut offizieller Pricing-Seite ist `gemini-3.5-transcribe` (wie `-live`) im Free Tier für Input und Output kostenlos (siehe b). Konkrete RPM/RPD-Zahlen für den Free Tier sind nicht öffentlich dokumentiert (Lücke).

## d) Bekannte Schwächen

**BESTÄTIGT (Kernfunktion, kein Bug):** Das Modell ist explizit darauf ausgelegt, Füllwörter zu entfernen:

> „The model filters out background noise, removes filler words like 'um' and 'ah,' and cleans up accidental speech mistakes as people talk."

— [9to5google.com](https://9to5google.com/2026/08/26/gemini-3-5-transcribe/), inhaltsgleich im [Google-Blog](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/)

Das gilt aber nur für den **Smart Mode** (Standard). Im **Verbatim-Modus** bleiben Füllwörter ("uh, um, you know") ausdrücklich erhalten — für dein Diktier-Overlay ist vermutlich der Smart Mode (Füllwörter raus) die richtige Wahl, sofern kein wortwörtliches Protokoll gewünscht ist. Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i).

**Fachbegriffe/Eigennamen (BESTÄTIGT als bekannte Schwäche, mit Gegenmaßnahme):** Ohne `custom_vocabulary` können Fachbegriffe verfälscht werden (Beispiel aus dem Guide: „Qdrant" → „quadrant"). Google adressiert das explizit über die Custom-Vocabulary-Funktion (bis 1.000 Begriffe) statt es zu verschweigen. Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i).

**Zeichensetzung:** Automatische Formatierung im Smart Mode „kann minimal vom Original abweichen" bzw. Inhalte „leicht umformulieren oder weglassen" — als expliziter Trade-off benannt, nicht als Detailspezifikation einer Interpunktionslogik. Für Verbatim-Anforderungen (Jura, Medizin, Untertitel) rät der Guide ausdrücklich vom Smart Mode ab. Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i).

**Stille / Halluzinationen: NICHT SPEZIFISCH BELEGT.** Keine der geprüften Quellen (Google-Blog, offizielle Docs, dev.to-Guide, Presseartikel) äußert sich konkret dazu, wie `gemini-3.5-transcribe`/`-live` mit Stille umgehen oder ob es zu ASR-typischen Halluzinationen bei Stille/Rauschen neigt. Eine gefundene Kennzahl („91 % Halluzinationsrate" für Gemini 3 Flash im Artificial-Analysis-Omniscience-Benchmark) bezieht sich auf **allgemeines Faktenwissen des LLM**, nicht auf ASR-Transkriptionsverhalten bei Stille — das ist nicht übertragbar und wird hier ausdrücklich NICHT als Beleg gewertet. Diese Frage bleibt offen (siehe unten).

**Weitere dokumentierte Einschränkung:** Sprecher-Zuordnung (Diarization) ist auf bis zu 3 Sprecher offiziell unterstützt (laut Guide bis 8, laut Blog-Berichterstattung „3+ experimental") — für dein Ein-Personen-Diktat irrelevant, aber als Grenze der Funktion notiert. Quelle: [dev.to/googleai Guide](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i) vs. [WebFetch-Zusammenfassung des Google-Blogs](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/) (Widerspruch zwischen Quellen — als Unsicherheit markiert).

## Offen / unbelegt

- **RPM/TPM/RPD des Free Tier** für `gemini-3.5-transcribe` und `gemini-3.5-transcribe-live` sind nirgends öffentlich in Textform dokumentiert; die offizielle Rate-Limits-Seite verweist nur auf das (auth-geschützte) AI-Studio-Dashboard. Da die Modelle erst seit 26.08.2026 in Preview sind, ist unklar, ob überhaupt schon stabile, dokumentierte Zahlen existieren.
- **Maximale Anzahl gleichzeitiger Live-Sessions** im Free Tier: nicht gefunden.
- **Sprachspezifischer WER für Deutsch:** Die 2,6 %/4,0 %-Werte sind Durchschnittswerte über 85+ Sprachen bzw. ein englischlastiger Artificial-Analysis-Datensatz; keine Quelle nennt einen isolierten Deutsch-WER.
- **Verhalten bei Stille/Hintergrundrauschen (Halluzinationsneigung):** nicht dokumentiert, siehe (d).
- **Exakte unterstützte Audio-Formate/Encodings** für den synchronen Endpunkt (WAV 16 kHz/16 bit mono wurde nirgends explizit als getestet/unterstützt bestätigt, auch wenn Standard-Formate wie WAV allgemein als unterstützt gelten) — sollte vor Produktiveinsatz mit einem echten Testaufruf verifiziert werden.
- Geringfügige Preis-Diskrepanz zwischen der offiziellen Pricing-Seite ($2,00/Mio. Input bei `gemini-3.5-transcribe`) und einer Sekundärquelle (CloudPrice: $2,50/Mio.) — die offizielle Seite wurde hier als maßgeblich übernommen, aber nicht per zweitem unabhängigen Snapshot gegengeprüft.

## Quellenliste

- [Intelligent transcription with Gemini 3.5 Transcribe — Google-Blog](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/)
- [ai.google.dev/gemini-api/docs/transcribe — Audio transcription (Interactions API)](https://ai.google.dev/gemini-api/docs/transcribe)
- [ai.google.dev/gemini-api/docs/live-api/live-transcribe — Live transcription mit Live API](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe)
- [ai.google.dev/gemini-api/docs/pricing — Pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe — Modellseite](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe)
- [ai.google.dev/gemini-api/docs/rate-limits — Rate Limits (allgemein, ohne modellspezifische Werte für Transcribe)](https://ai.google.dev/gemini-api/docs/rate-limits)
- [dev.to/googleai — Stop Wrestling with ASR: The Complete Guide to Gemini 3.5 Transcribe](https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i)
- [9to5google.com — Google launches Gemini 3.5 Transcribe](https://9to5google.com/2026/08/26/gemini-3-5-transcribe/)
- [MarkTechPost — Google AI Releases Gemini 3.5 Transcribe: 2.6% Average WER](https://www.marktechpost.com/2026/08/27/google-ai-releases-gemini-3-5-transcribe-a-speech-to-text-model-reporting-2-6-average-wer-across-85-languages/)
- [Artificial Analysis — Speech to Text (ASR) Leaderboard](https://artificialanalysis.ai/speech-to-text)
- [orcarouter.ai — Gemini 3.5 Transcribe vs Whisper Large v3 Turbo](https://www.orcarouter.ai/blog/gemini-3-5-transcribe-vs-whisper-large-v3-turbo)
- [CloudPrice — Gemini 3.5 Transcribe pricing & specs (Sekundärquelle, leichte Preis-Diskrepanz)](https://cloudprice.net/models/google-gemini-3-5-transcribe)
