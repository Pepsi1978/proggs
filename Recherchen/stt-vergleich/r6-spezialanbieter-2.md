# R6 — Spezialisierte und neue STT-Anbieter Teil 2 (ElevenLabs, Mistral Voxtral, Alibaba/Qwen, Hoster)

Stand: 21.10.2026. Basislinie: Groq `whisper-large-v3-turbo` (Groq-Doku: 12 % WER ohne Sprachangabe, 0,04 $/Stunde, 25 MB Upload frei / 100 MB Dev-Tier; https://console.groq.com/docs/speech-to-text).
Alle Preise USD, ohne Steuern. „unbelegt“ = in keiner abgerufenen Primär- oder Sekundärquelle bestätigt.

Wichtigster Neufund: Mistral hat gestern (20.10.2026) die Familie **Voxtral 3** veröffentlicht (Small/Mini Transcribe 3, Mini Transcribe Realtime 3). Scribe v1 ist bei ElevenLabs als „deprecated“ geführt.

---

## 1. ElevenLabs Scribe

### 1.1 Scribe v2 (Batch)
- **Modell-ID:** `scribe_v2` (Realtime: `scribe_v2_realtime`; Medizin: `scribe_v2_medical`). Quelle: https://elevenlabs.io/docs/overview/models
- **Status:** aktiv, „State-of-the-art“. `scribe_v1` steht unter „Deprecated models“, Nachfolger `scribe_v2`. Auf OpenRouter seit 07.10.2026 gelistet (https://openrouter.ai/elevenlabs/scribe-v2/providers).
- **Deutsch-Qualität (WER):**
  - ElevenLabs selbst: Deutsch in der Stufe „Excellent (≤ 5 % WER)“ (https://elevenlabs.io/docs/overview/capabilities/speech-to-text). Eigene Landingpage Deutsch: FLEURS 3,1 % im Fließtext, in der Tabelle 2,7 % für **Scribe v1**; Common Voice 5,5 %; Vergleich dort: Gemini Flash 2 3,9 %, Whisper Large v3 4,5 %, Deepgram Nova 2 9,1 % (https://elevenlabs.io/speech-to-text/german). Herstellerangabe, Normalisierung unklar.
  - AssemblyAI-Benchmark (Konkurrent, eigener Lauf 25.08.–23.09.2026, normalisierte WER, FLEURS Deutsch): **Scribe v2 5,8 %**; Gemini 3.5 Live 4,7 %; AssemblyAI Universal-3.6 Pro 5,0 %; GPT-4o Transcribe 13,1 %; GPT-4o mini Transcribe 14,9 %. Whisper und Voxtral stehen dort nicht in der Deutsch-Tabelle (https://www.assemblyai.com/benchmarks).
  - Voxtral-Paper (Mistral, Tabelle 4/5/6, **Scribe ohne Versionsangabe**, vermutlich v1): FLEURS-de 4,78; Common Voice-de 3,52; MLS-de 9,81 (https://arxiv.org/html/2507.13264). Im gleichen Paper Whisper large-v3: 5,46 / 6,25 / 5,72.
  - Mistral Voxtral-3-Blog (Hersteller): FLEURS 45 Sprachen Makro-WER Scribe v2 4,9 % (https://mistral.ai/news/voxtral-3, laut Zweitseite https://ai-tldr.dev/models/voxtral-transcribe-3/).
  - Artificial Analysis (englisch-lastig, keine Sprachaufschlüsselung): AA-WER Scribe v2 2,2 %, 3,67 $/1000 min; Groq Whisper Large v3 Turbo 4,6 %, 0,67 $/1000 min (https://artificialanalysis.ai/speech-to-text/non-streaming).
  - Unabhängig, Deutsch-Englisch-Codeswitching: Scribe v2 führt in allen vier Sprachpaaren, mittlere WER 13,2 % (arXiv 2605.19069 laut Suchtreffer, nicht selbst gelesen).
- **Freikontingent:** Free-Plan = 10.000 Credits/Monat, laut ElevenLabs-Preistabelle **12 Minuten Scribe v2 pro Monat** (https://elevenlabs.io/speech-to-text). Drittquellen rechnen mit 330 Credits/Min, also ca. 30 Min – widersprüchlich; die offizielle Tabelle sagt 12 Min. Monatlich, kein Guthaben-Übertrag belegt. Keine Kreditkarte laut Scribe-Seite („10.000 Credits, no credit card“, Suchtreffer https://join.elevenlabs.io/api/speech-to-text – unbelegt auf Primärseite). Free-Plan ohne kommerzielle Lizenz. Concurrency Free: 8 (Standard-STT), 6 (Realtime) (https://elevenlabs.io/docs/overview/models).
- **Preis je Audiostunde:** 0,22 $/h (https://elevenlabs.io/pricing/api); Keyterm-Aufschlag +0,05 $/h (API-Referenz: +20 %). Landingpage nennt „ab 0,40 $/h“ – widersprüchlich. Über fal: 0,008 $/Min = 0,48 $/h (Suchtreffer fal.ai/elevenlabs, unbelegt). Über OpenRouter 0,000061 $/s = 0,22 $/h.
- **Rate Limits:** Concurrency je Tarif (Free 8 … Scale/Business 60). Kein RPM-Limit belegt.
- **Max. Datei:** 3 GB (Produktseite) bzw. <5 GB (API-Referenz `file`), Dauer bis 10 h. Dateien >8 Min werden in vier Segmente parallel transkribiert.
- **Streaming/Echtzeit:** `scribe_v2_realtime`, WebSocket `wss://api.elevenlabs.io/v1/speech-to-text/realtime`, ca. 150 ms; PCM 8–48 kHz oder μ-law; Commit `manual`/`vad`; Auth `xi-api-key` oder Single-Use-Token fürs Clientseitige (https://elevenlabs.io/docs/api-reference/speech-to-text/v-1-speech-to-text-realtime). 0,39 $/h. In Mistrals Realtime-Test FLEURS-35 Makro-WER 8,7 %; im Voxtral-Realtime-Paper Deutsch: FLEURS 4,31, Common Voice 16,60 (https://arxiv.org/html/2602.11298v1) – Common Voice auffällig schlecht.
- **Vokabular/Prompting:** `keyterms` bis 1000 (Batch; je <50 Zeichen, max. 5 Wörter), Realtime bis 50 (je 20 Zeichen); `no_verbatim` entfernt Füllwörter (nur v2); Entity Detection (65 Typen); Diarisierung bis 32 Sprecher.
- **Halluzinationsverhalten:** Keine Berichte über Stille-Halluzinationen gefunden (nur negativ: „nichts gefunden“). Bekannt: `tag_audio_events` fügt Ereignis-Tags ein; Realtime-Berichte über mehrfach wiederholten `committed_transcript` (Drittblog, unbestätigt: https://mtools.sasshoes.com/blog/elevenlabs-scribe-v2-triplicated-text); Vapi-Forum: Turn-Taking-Probleme mit Scribe v2 Realtime.
- **Datenschutz/Datenresidenz:** Standardmäßig Speicherung nach Privacy Policy; Zero Retention (`enable_logging=false`) nur Enterprise; EU/Indien/Singapur-Isolierung nur Enterprise (https://elevenlabs.io/docs/overview/administration/data-residency; Realtime-Endpunkt `api.eu.residency.elevenlabs.io`). Training mit Free-Plan-Audio: **unbelegt**.
- **Latenz:** Batch kein Wert in Doku; Artificial Analysis Speed-Faktor 59,9×.
- **Android-Integration (REST):** `POST https://api.elevenlabs.io/v1/speech-to-text` (Pfad aus Realtime-URL und Doku „convert“ abgeleitet), Header `xi-api-key`, multipart: `model_id=scribe_v2`, `file`, `language_code=deu` (oder `de`), optional `keyterms`, `diarize`, `no_verbatim`, `tag_audio_events=false`. Antwort JSON: `text`, `words[]` mit `logprob`, `language_probability`.

### 1.2 Scribe v1
- `scribe_v1`, **deprecated**. fal-Preis 0,03 $/Min (unbelegt). Nicht mehr verwenden.

---

## 2. Mistral Voxtral (EU-Anbieter)

### 2.1 Voxtral 3 (neu, 20.10.2026, Public Preview)
- **Modell-IDs** (Zweitquellen übereinstimmend): `voxtral-small-transcribe-3-0`, `voxtral-mini-transcribe-3-0`, `voxtral-mini-transcribe-realtime-3-0` (https://ai-tldr.dev/models/voxtral-transcribe-3/; Doku-Slug bestätigt: https://docs.mistral.ai/models/voxtral-small-transcribe-3-0).
- **Status:** Public Preview, Premier-Modelle, proprietär, nur API (keine Gewichte), Self-Deployment auf Anfrage.
- **Deutsch-Qualität:** Mistral veröffentlicht nur **Makro-Durchschnitte über 45 FLEURS-Sprachen**, keinen Deutsch-Einzelwert: Small 4,0 % (Sprache vorgegeben) bzw. 4,1 % (auto) laut ai-tldr, die Zweitseite cellcog nennt 4,1 % / 4,0 % (Reihenfolge vertauscht – widersprüchlich); Mini 4,5 % / 4,6 %; Microsoft AI Transcribe 2 4,0 %; Scribe v2 4,9 %; Gemini 3.5 Transcribe 4,5 %; GPT Transcribe 5,6 %; **Whisper large-v3 14,9 %** (Quelle: https://mistral.ai/news/voxtral-3 bzw. https://ai-tldr.dev/models/voxtral-transcribe-3/, Hersteller). Artificial Analysis AA-WER: Small Transcribe 3 **2,1 %** (Platz 3), Mini Transcribe 3 2,2 % (Platz 5), Scribe v2 2,2 % (https://artificialanalysis.ai/speech-to-text/non-streaming; Hinweis auf Sprachaufschlüsselung: keine). Diarisierung cpWER: Small 18,4 %. **Kein unabhängiger Deutsch-Test gefunden.**
- **Freikontingent:** siehe 2.3.
- **Preis:** Mini 0,0035 $/Min = **0,21 $/h**; Small 0,004 $/Min = **0,24 $/h**; Realtime 0,008 $/Min = 0,48 $/h. Auf OpenRouter: Small 0,000067 $/s, Mini 0,000058 $/s (https://openrouter.ai/models?output_modalities=transcription).
- **Limits:** Kontextfenster 32K Tokens (Small/Mini). Max. Dateilänge/-größe für v3 **nicht belegt**; für v2 „bis 3 Stunden je Anfrage“.
- **Streaming:** Realtime-Modell per WebSocket, Verzögerung 80–2400 ms konfigurierbar, „sub-500 ms“; FLEURS-35 Makro-WER 5,7 % (2400 ms), 6,0 % (960), 6,4 % (480), 7,4 % (240). Weitere 35+ Sprachen (35 bzw. 40+ je nach Seite).
- **Vokabular:** Context Biasing (Schlüsselbegriffe und Freitext); Earnings-22 Keyphrase-Recall Small 81,4 % → 94,6 % (Englisch). Für v2 gilt: „optimiert für Englisch, andere Sprachen experimentell“ (Doku) – ob das für v3 aufgehoben ist: unbelegt.
- **Extras Small:** Übersetzung, gesprochenes Q&A, Zusammenfassung, Function Calling (Risiko: Instruktionen im Audio – unbelegt, siehe Halluzinationen).
- **Halluzinationen:** keine Berichte zu v3 gefunden. Simon Willison (2025, Voxtral v1): die dedizierte Transkriptions-API befolgte Anweisungen im Text nicht (https://feeds.simonwillison.net/2025/Jul/16/voxtral/).
- **Datenschutz:** Mistral ist EU-Anbieter; Voxtral-3-Seite nennt europäische, US- und globale Endpunkte, Kontrolle über Verarbeitungsort und Aufbewahrung für Organisationen (https://cellcog.ai/blog/mistral-voxtral-3/ – Zweitquelle).
- **Android (REST):** `POST https://api.mistral.ai/v1/audio/transcriptions`, `Authorization: Bearer <KEY>`, multipart: `model`, `file`, optional `language=de`, `context_bias[]`, `diarize`, `timestamp_granularities[]`, `stream` (https://docs.mistral.ai/api/endpoint/audio/transcriptions). **Achtung:** laut Doku ist `timestamp_granularities` nicht mit `language` kombinierbar. OpenAI-SDK-kompatibel für `file`, `language`, `timestamp_granularities`, `stream`; `diarize`/`context_bias` nur über `extra_body`; `file_url` nicht (https://docs.mistral.ai/capabilities/audio/speech_to_text/offline_transcription – Seite steht im Doku-Bereich „Deprecated“).

### 2.2 Voxtral Mini Transcribe V2 und Voxtral Realtime (Feb 2026)
- **IDs:** `voxtral-mini-latest` (läuft laut Doku noch auf Mini Transcribe V2), Realtime `voxtral-mini-transcribe-realtime-2602`. Realtime als Open Weights unter Apache 2.0 (`mistralai/Voxtral-Mini-4B-Realtime-2602`; die Seite nennt an anderer Stelle 3B – Inkonsistenz) (https://mistral.ai/news/voxtral-transcribe-2).
- **Deutsch-WER (Voxtral-Paper, https://arxiv.org/html/2507.13264, Tabellen 4–6 / Realtime-Paper https://arxiv.org/html/2602.11298v1 Tab. 7–8):**

| Modell | FLEURS-de | Common Voice-de | MLS-de |
|---|---|---|---|
| Whisper large-v3 | 5,46 | 6,25 | 5,72 |
| GPT-4o mini Transcribe | 3,76 | 6,72 | 5,44 |
| Gemini 2.5 Flash | 4,74 | 7,25 | 6,52 |
| Scribe (v1?) | 4,78 | 3,52 | 9,81 |
| Voxtral Mini (3B-2507, Open) | 4,40 | 6,07 | 7,09 |
| Voxtral Mini Transcribe | 3,54 | 4,85 | 5,64 |
| Voxtral Small | 3,38 | 3,74 | 5,57 |
| Voxtral Mini Transcribe V2 (Realtime-Paper) | 3,54 | 4,35 | – |
| Voxtral Realtime 2400 ms | 4,15 | 5,66 | – |
| Voxtral Realtime 960 ms | 4,87 | 6,85 | – |
| Voxtral Realtime 480 ms | 6,19 | 8,70 | – |
| Voxtral Realtime 240 ms | 8,15 | 11,13 | – |
| Scribe v2 Realtime | 4,31 | 16,60 | – |

  Alle Werte Herstellerpapiere (Mistral), Whisper-Zeile = large-v3, **nicht Turbo**. Eine Turbo-Deutsch-WER fand ich nicht (OpenAI: Turbo „ähnlich large-v2“; https://github.com/openai/whisper/discussions/2363 – Zahlen nur als Bild); Turbo-WER Deutsch daher **unbelegt**, vermutlich ≥ large-v3.
- **Preis:** Mini Transcribe V2 0,003 $/Min = **0,18 $/h**; Realtime 0,006 $/Min = 0,36 $/h. Auf DeepInfra Voxtral-Mini-3B-2507 0,001 $/Min (0,06 $/h), Voxtral-Small-24B-2507 0,003 $/Min (0,18 $/h) (https://deepinfra.com/models/automatic-speech-recognition) – das sind die alten Open-Modelle von 2025.
- **Limits:** bis 3 h je Anfrage (API); Playground bis 1 GB je Datei, mp3/wav/m4a/flac/ogg. Kontext-Biasing bis 100 Wörter/Phrasen.
- **Diarisierung:** ja; bei überlappender Sprache meist nur ein Sprecher.

### 2.3 Freikontingent La Plateforme / Mistral Studio
- Free Mode ist Standard für neue Konten, **keine Kreditkarte** (Studio-Quickstart laut https://www.aifreeapi.com/en/posts/mistral-api-free-tier, Drittquelle, Stand 09.10.2026). Telefon-Verifizierung: laut Drittquellen möglich, nicht bestätigt.
- Limits: drei Obergrenzen pro Modell (Anfragen/s, Tokens/Min, Tokens/Monat), Zahlen **nicht veröffentlicht** (Admin › API › Limits). Die frühere Angabe „1 Mrd. Tokens/Monat, 1 Anfrage/s“ gilt laut jüngster Quelle als veraltet. Die „10 $/Monat API-Guthaben“-Zeile wurde am 07.10.2026 von der Preisseite entfernt.
- **Audio im Free-Tier: unbelegt.** Weder Doku noch Drittquellen sagen, ob/wie viele Minuten Voxtral im Free Mode laufen (keine Token-Abrechnung bei Audio: Abrechnung per Minute). Test: ein einzelner curl-Aufruf mit Free-Key.
- **Datennutzung:** im Free Mode darf Mistral Eingaben/Ausgaben zum Training nutzen, Opt-out über Admin Panel › Privacy › „Anonymous improvement data“ (Drittquelle). Für „Labs/Preview Models“ erlauben die Commercial Terms (4.2/4.3) Training **ohne** Opt-out-Möglichkeit, auch ohne Zero Data Retention – Voxtral 3 ist **Public Preview**; ob es darunter fällt, ist unbestätigt, aber ein reales Risiko für Franks Diktatdaten.
- Pro-Hinweis: Pay-as-you-go schaltet Tier 1 frei; Tiers 2–4 nach Ausgaben ($20/$100/$500).

---

## 3. Alibaba / Qwen (DashScope / Model Studio International)

### 3.1 qwen3-asr-flash (und qwen-audio-3.0/3.1-asr-flash)
- **Modell-IDs:** `qwen3-asr-flash` (synchron, ≤5 Min / ≤10 MB), `qwen3-asr-flash-filetrans` (offline, ≤12 h / 2 GB), `qwen3-asr-flash-realtime`, neuer `qwen-audio-3.0-asr-flash` (30 Sprachen inkl. Deutsch, 5 Min) und `qwen-audio-3.1-asr-flash(-message/-streaming/-filetrans)`; `fun-asr`, `fun-asr-realtime`, `paraformer-v2`, `paraformer-realtime-v2` (Deutsch laut Modellübersicht: ja für fun-asr/fun-asr-realtime/paraformer-v2; nicht für `fun-asr-realtime-2026-02-28`) (https://help.aliyun.com/en/model-studio/asr-model/, https://www.alibabacloud.com/help/en/model-studio/qwen-audio-3-0-asr-flash).
- **Deutsch-Qualität (WER):** Qwen3-ASR-Technical-Report (arXiv 2601.21337, Tab. A.2): **Qwen3-ASR-Flash-1208: FLEURS-de 3,03; Common Voice-de 3,53; MLS-de 4,11**; Qwen3-ASR-1.7B (offen): 3,92 / 5,85 / 6,05; 0,6B: 6,48 / 9,44 / 9,52 (https://arxiv.org/html/2601.21337). Whisper-large-v3 dort nur als Sprachdurchschnitt (FLEURS 12 Sprachen 5,27 vs. Qwen3-ASR-1.7B 4,90; CommonVoice 10,77 vs. 9,18; MLS 8,62 vs. 8,55; https://github.com/QwenLM/Qwen3-ASR). Herstellerzahlen. Unabhängiger Kurztest (whispernotes.app): Qwen3-ASR 1.7B vs. Whisper Turbo auf Deutsch innerhalb der Fehlerbalken, nicht trennbar (Suchtreffer, nicht selbst gelesen). `qwen-audio-3.0`-Report (arXiv 2609.07549): 30 Sprachen inkl. Deutsch, aber **keine** Deutsch-Zahlen im Text. Ob der API-Endpunkt `qwen3-asr-flash` exakt dem „Flash-1208“-Stand entspricht: **unbelegt**.
- **Freikontingent:** Preisseite: Freikontingent für `qwen3-asr-flash`, `-realtime`, `fun-asr`: **nur China (Beijing), 36.000 s (10 h)**; „No free quota is available in other regions“; Paraformer ebenfalls Beijing 10 h/Monat (https://help.aliyun.com/en/model-studio/model-pricing). Singapur/International: **kein ASR-Freikontingent belegt**. Allgemeine Neukunden-Regel: 90 Tage ab Aktivierung, je Modell eigenes Kontingent (meist 1 Mio. Tokens, gilt für Text), Widerspruch zwischen „nur Beijing“ und „auch Singapur“ auf der Quota-Seite (https://help.aliyun.com/en/model-studio/new-free-quota). Kreditkarte: Seite nennt Alipay/UnionPay/Online-Banking; für Alibaba-International-Konto unbelegt.
- **Preis (Singapur):** `qwen3-asr-flash` und `fun-asr` 0,00026 CNY/s ≈ 0,000035 $/s = **ca. 0,13 $/h**; Realtime 0,00066 CNY/s ≈ 0,32 $/h; `qwen-audio-3.0-asr-flash` 0,000035 $/s (0,126 $/h). (CNY-Umrechnung näherungsweise; Alibaba nennt $0,000035/s direkt.)
- **Rate Limits:** `qwen3-asr-flash` 100 RPM; `qwen-audio-3.0-asr-flash` 600 RPM (Singapur).
- **Streaming:** Flash: `stream=true` (SSE); Realtime-Modelle per WebSocket. Open-Source Qwen3-ASR: Streaming nur mit vLLM-Backend.
- **Vokabular:** „Context Enhancement“ (Verlauf/Text als Kontext für Eigennamen) und Hotwords (https://help.aliyun.com/en/model-studio/qwen-speech-recognition); Qwen3-ASR nutzt System-Prompt-Kontext; ITN (`asr_options.enable_itn`).
- **Halluzinationen:** nichts belegt.
- **Datenschutz:** Standort Singapur (International-Scope); Datenaufbewahrung auf den ASR-Seiten **nicht angegeben**; kein EU-Standort belegt. Datenschutzrisiko für Frank höher als bei Mistral (außereuropäischer Anbieter).
- **Android (REST):** OpenAI-kompatibel (`/compatible-mode/v1/chat/completions`, `model=qwen3-asr-flash`, Message-Content `{"type":"input_audio","input_audio":{"data":"<URL oder data:audio/mpeg;base64,...>"}}`, `asr_options`); lokale Dateipfade nicht erlaubt, Base64 ≤10 MB; die Seite nennt nur die Beijing-URL, **die International-URL (`dashscope-intl.aliyuncs.com/compatible-mode/v1`) ist auf den abgerufenen Seiten nicht belegt**. Eigener Workspace/API-Key je Region nötig; ASR-Modelle müssen je Workspace freigeschaltet werden.

### 3.2 Fun-ASR / Paraformer
- Fun-ASR-Report (arXiv 2509.12508) und Alibaba: Fun-ASR-MLT-Nano 31 Sprachen mit Schwerpunkt Ost-/Südostasien; Deutsch laut Modellübersicht für den API-Dienst ja, in den Open-Modell-Sprachlisten nicht. Keine Deutsch-WER gefunden. Artificial Analysis führt `Fun-Realtime-ASR-preview` mit 1,7 % AA-WER (Platz 1), Preis „0,00“ (ungeklärt) – englisch-/chinesisch-lastig, nicht auf Deutsch übertragbar.
- Paraformer-v2: Deutsch ja (laut Übersicht), Keine WER.

### 3.3 Weitere chinesische/asiatische Anbieter
- Doubao/Tencent/Baidu: nur im Qwen-Audio-3.0-Report als Vergleich (Chinesisch/Englisch, Spanisch, Französisch). Keine Deutsch-Belege, daher nicht weiter verfolgt. Sarvam: nur indische Sprachen, kein Deutsch (nicht geprüft, außerhalb des Auftrags).

---

## 4. Hoster für Open-Modelle

| Anbieter | STT-Modelle (belegt) | Preis | Freikontingent |
|---|---|---|---|
| **DeepInfra** | Qwen3-ASR-0.6B/-1.7B, Whisper-large-v3 (-turbo), Voxtral-Mini-3B-2507/Small-24B-2507, Nemotron-3.5-ASR-Streaming-Multilingual | 0,0002–0,003 $/Min (0,012–0,18 $/h) | unbelegt, widersprüchlich (0 / 1,80 $ / 5 $ einmalig, 90 Tage) |
| **Together AI** | Whisper Large v3 (0,0015 $/Min), Streaming (0,0035), Parakeet TDT 0.6B v3 (0,0015), Nemotron 3/3.5 ASR; **kein Voxtral** | 0,09 $/h Batch | **Kein Free-Trial**, 5 $ Mindest-Guthaben (Together-Support, 01.06.2026: https://support.together.ai/articles/1862638756-changes-to-free-tier-and-billing-july-2025) |
| **Fireworks** | Whisper v3-large 0,0015 $/Min, v3-large-turbo 0,0009 $/Min (Stand 12/2024), Streaming ca. 200–300 ms, 0,0032 $/Min (Stand 01/2025, Sekundärquelle) | 0,054–0,09 $/h | „neue Konten erhalten Credits“ (Fireworks-FAQ), Betrag nur aus Drittquellen (1 $) – unbelegt |
| **Replicate** | Incredibly Fast Whisper (AA 1,49 $/1000 min), Whisper Large v3 (4,23 $/1000 min, AA-WER 10,1 %!) | 0,09–0,25 $/h | unbelegt |
| **fal.ai** | Whisper Large v3 (1,15 $/1000 min, AA-WER 4,1 %), Wizper (0,50 $/1000 min, 4,7 %), Scribe v2 0,008 $/Min (unbelegt) | 0,03–0,48 $/h | unbelegt |
| **OpenRouter** | `/api/v1/audio/transcriptions` seit Mai 2026; `openai/whisper-large-v3`, `gpt-4o-transcribe`, MAI-Transcribe 1.5/2, **ElevenLabs Scribe v2**, **Voxtral Small/Mini Transcribe 3** | Scribe v2 0,22 $/h, Voxtral Small 0,000067 $/s (0,24 $/h), Mini 0,21 $/h | **keine** kostenlosen STT-Modelle (kein `:free`) |

Details:
- Parakeet-TDT-0.6B-v3 (NVIDIA, 25 europäische Sprachen, CC BY 4.0): Deutsch FLEURS **5,04 %**, CoVoST 4,84 % (https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3) – kein Qualitätsgewinn gegenüber Voxtral/Qwen-Flash, nur billig (Together 0,0015 $/Min).
- AA-Preise Whisper-Hoster: Groq Turbo 0,67 $/1000 min (AA-WER 4,6 %), fal Wizper 0,50, Together Large v3 1,50 (https://artificialanalysis.ai/speech-to-text/models/whisper). Fireworks/DeepInfra dort ohne Whisper-Eintrag.
- OpenRouter-STT-Grenzen: Multipart 25 MB, Upstream-Timeout 60 s je Anfrage, `prompt` wird ignoriert, `verbose_json` nur bei OpenAI-kompatiblen Providern (https://openrouter.ai/docs/guides/overview/multimodal/stt). Für Android: OpenAI-kompatibler Multipart-Aufruf mit geänderter Basis-URL.
- Gehostete Whisper-Varianten haben dieselben bekannten Stille-Halluzinationen (z. B. deutsche Untertitel-Floskeln bei Stille; https://kieranhealy.org/blog/archives/2025/07/22/the-sound-of-silence). Hoster ändern daran nichts.
- Datenschutz Hoster: DeepInfra/Together/Fireworks/fal/Replicate sind US-Anbieter; EU-Residenz nicht belegt.

---

## 5. Tabelle „Kerndaten“

| Anbieter / Modell | Deutsch-WER (Benchmark, Quelle-Art) | Preis je Stunde | Freikontingent | Streaming | Vokabular | EU/Datenschutz |
|---|---|---|---|---|---|---|
| Groq whisper-large-v3-turbo (Basislinie) | Deutsch-Turbo unbelegt; large-v3 FLEURS-de 5,46 / CV 6,25 / MLS 5,72 (Voxtral-Paper) | 0,04 $ | (anderer Researcher) | nein (Batch) | Prompt | US |
| ElevenLabs Scribe v2 | FLEURS-de 5,8 (AssemblyAI); Scribe(v1?) 4,78 / CV 3,52 / MLS 9,81 (Mistral-Paper); eigene Angabe ≤5 % | 0,22 $ (+0,05 Keyterms) | 12 Min/Monat (10.000 Credits) | Realtime 0,39 $/h, ~150 ms | Keyterms 1000 | EU nur Enterprise; Training unbelegt |
| Mistral Voxtral Mini Transcribe V2 | FLEURS-de 3,54 / CV 4,35 (Mistral-Paper) | 0,18 $ | Free Mode, Audio unbelegt | Realtime 0,36 $/h | context_bias (Englisch optimiert) | EU-Anbieter |
| Mistral Voxtral Small (2507 / Transcribe) | FLEURS-de 3,38 / CV 3,74 / MLS 5,57 | 0,18–0,24 $ | wie oben | – | wie oben | EU |
| Mistral Voxtral Small Transcribe 3 | kein Deutsch-Einzelwert; 45-Sprachen-Makro 4,0–4,1 %; AA-WER 2,1 % | 0,24 $ | wie oben, Preview | Realtime 3: 0,48 $/h | context_bias | EU; Preview-Trainingsrisiko |
| Mistral Voxtral Mini Transcribe 3 | Makro 4,5 %; AA-WER 2,2 % | 0,21 $ | wie oben | – | context_bias | EU |
| Qwen3-ASR-Flash (DashScope) | FLEURS-de 3,03 / CV 3,53 / MLS 4,11 (Flash-1208, Hersteller) | ca. 0,13 $ | International: keines belegt (Beijing 10 h) | ja (SSE/WS) | Kontext + Hotwords | Singapur, keine EU |
| Qwen3-ASR-1.7B (offen/DeepInfra) | FLEURS-de 3,92 / CV 5,85 / MLS 6,05 | 0,027 $ (DeepInfra) | unbelegt | vLLM | Kontext | US/offen |
| Parakeet-TDT-0.6B-v3 | FLEURS-de 5,04 | 0,09 $ (Together) | kein Trial | Nemotron-Streaming | – | US |
| Whisper Large v3 bei Hostern | FLEURS-de 4,5–5,5 | 0,054–0,25 $ | unbelegt | teilw. | Prompt | US |

---

## 6. Fazit: Besser als Groq Turbo für Deutsch?

- **Mistral Voxtral: ja (Genauigkeit), aber Kostenlos-Frage offen.** Im Mistral-Paper liegt Voxtral Mini Transcribe/Small auf Deutsch bei FLEURS 3,4–3,5 % gegenüber 5,46 % für Whisper large-v3 (Turbo vermutlich nicht besser als large-v3), auch Common Voice 3,7–4,9 vs. 6,25. Kritik: Herstellerzahlen, Whisper-Zeile ist nicht Turbo, kein unabhängiger Deutsch-Test, keine Eigennamen-/Zahlen-Auswertung. Voxtral 3 (gestern) ist noch nicht auf Deutsch belegt, nur im 45-Sprachen-Makro besser (4,0–4,5 % vs. 14,9 % Whisper-v3) und AA-WER 2,1–2,2 % vs. 4,6 % (Groq Turbo) – englisch-lastig. Preis 0,18–0,24 $/h, ca. 5× Turbo, bei Franks Nutzung (angenommen ≤1 h/Tag) wenige Dollar im Monat. **Freikontingent im API-Free-Mode für Audio unbelegt, Datennutzung im Free Mode (Training, Opt-out) und Preview-Klausel beachten.** EU-Anbieter = Datenschutzvorteil. Empfehlung: Free-Key anlegen, einen curl-Test mit `voxtral-mini-latest` und `voxtral-small-transcribe-3-0` (language=de, `context_bias` für Eigennamen) fahren.
- **Qwen3-ASR-Flash: ja/unklar.** Herstellerzahlen sehr gut (FLEURS-de 3,03; Common Voice-de 3,53), Preis ca. 0,13 $/h, aber International **kein Freikontingent belegt**, Limit 5 Min/10 MB je Anfrage (Diktat reicht), Alibaba Singapur ohne EU-Standort, International-URL nicht aus Primärquelle bestätigt.
- **ElevenLabs Scribe v2: unklar / eher nein für „kostenlos“.** Deutsch gut (Herstellerangabe ≤5 %, AssemblyAI 5,8 % FLEURS-de), aber nicht nachweislich besser als Voxtral; Free-Plan nur **12 Min/Monat**; Keyterms-Aufschlag; Preis 0,22 $/h. Nur sinnvoll als Bezahlmodell mit starken Keyterms (1000) und Zero-Retention (Enterprise).
- **Hoster (DeepInfra, Together, Fireworks, fal, Replicate):** bieten dieselbe Whisper-Qualität bzw. Qwen3-ASR/Parakeet; **kein dauerhaftes Freikontingent** (Together: kein Trial, 5 $ Mindest-Einzahlung). Nur DeepInfra mit Qwen3-ASR-1.7B (0,027 $/h) ist preislich interessant, aber ohne belegtes kostenloses Guthaben.
- **OpenRouter:** bietet STT (seit Mai 2026), auch Scribe v2 und Voxtral 3, aber ohne kostenlose Modelle. Kein Vorteil gegenüber Direktanbindung.
- **Gesamt:** Für „bessere Qualität“ ist Voxtral (Mini/Small) der aussichtsreichste Kandidat dieser Gruppe, für „möglichst großes kostenloses Kontingent“ gibt es hier **keinen belegten Gewinner**; Mistral Free Mode ist der einzige Kandidat, der ohne Kreditkarte nutzbar sein könnte, und braucht einen Praxistest (429-Verhalten, Minutenlimit, Training).

---

## 7. Offene Punkte / nicht belegt
- Mistral: Free-Mode-Limits für Audio, max. Dateilänge/-größe v3, Wirkung des Preview-Trainingsvorbehalts, Deutsch-Einzelwerte Voxtral 3, Gültigkeit der `context_bias`-Einschränkung („Englisch optimiert“) in v3.
- Turbo-Deutsch-WER (Basislinie) mit gleichem Protokoll.
- Qwen International: Endpunkt-URL, Freikontingent, Aufbewahrung, ob Kreditkarte/Identitätsprüfung nötig.
- ElevenLabs: Free-Plan-Trainingsnutzung; Scribe-v2-Einzelwert Deutsch Common Voice/MLS.
- Fireworks-Preise aus 12/2024 bzw. 01/2025; aktuelle Preisseite enthält keine STT-Zeile.
- Keine gleichprotokollierte, unabhängige Deutsch-Messung (Eigennamen, Zahlen) für Voxtral 3, Qwen3-ASR-Flash und Scribe v2 – empfehlenswert: eigener Testsatz mit Franks Diktat.

BEST-PRACTICES-KANDIDATEN:
- Mistral Transkription-API: `timestamp_granularities` ist nicht mit `language` kombinierbar; `context_bias` bis 100 Begriffe (Englisch optimiert); `diarize`/`context_bias` bei OpenAI-SDK nur über `extra_body` — https://docs.mistral.ai/capabilities/audio/speech_to_text/offline_transcription (Seite im Deprecated-Bereich, Stand v2 / Voxtral Mini Transcribe 2; Voxtral 3 vom 20.10.2026 hat eigene Modell-IDs `voxtral-*-transcribe-3-0`).
- ElevenLabs Scribe v2: `language_code` mitgeben, `keyterms` (bis 1000, +20 %), `no_verbatim`, `enable_logging=false` nur Enterprise — https://elevenlabs.io/docs/api-reference/speech-to-text/convert (scribe_v2).
- Qwen3-ASR-Flash: synchron ≤10 MB/5 Min, Base64-Data-URL, kein lokaler Pfad, `asr_options.enable_itn` — https://help.aliyun.com/en/model-studio/qwen-speech-recognition (qwen3-asr-flash, Stand 21.10.2026).
- OpenRouter STT: Multipart 25 MB, 60-s-Timeout, `prompt` ignoriert — https://openrouter.ai/docs/guides/overview/multimodal/stt (Stand 21.10.2026).

BUG-KANDIDATEN:
- ElevenLabs Scribe v2 Realtime: `committed_transcript` liefert Text mehrfach (Drittbericht, unbestätigt) — https://mtools.sasshoes.com/blog/elevenlabs-scribe-v2-triplicated-text (scribe_v2_realtime, Stand Okt 2026); Vapi-Forum: Turn-Taking-Probleme — https://support.vapi.ai/t/33255300/scribe-v2-scribe-v2-realtime-is-totally-unusable-on-vapi
- Mistral Transkription: `timestamp_granularities` + `language` Inkompatibilität — https://docs.mistral.ai/capabilities/audio/speech_to_text/offline_transcription (voxtral-mini-latest = Mini Transcribe V2).
- Mistral Free Mode: Preview-/Labs-Modelle ohne Trainings-Opt-out (Commercial Terms 4.2/4.3, Drittquelle) — https://www.aifreeapi.com/en/posts/mistral-api-free-tier (Stand 09.10.2026).
- Whisper-Stille-Halluzinationen in Deutsch (Untertitel-Floskeln) — https://kieranhealy.org/blog/archives/2025/07/22/the-sound-of-silence (Whisper-Familie, gilt auch für Groq/Hoster).
