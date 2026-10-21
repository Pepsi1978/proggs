# STT-Provider (Speech-to-Text, Deutsch)

Stand: 21.10.2026 (Systemzeit 22:20) · Versions-Anker: Groq `whisper-large-v3-turbo`/`whisper-large-v3`, Gemini `gemini-3.5-transcribe`, `gemini-3.8-flash`, `gemini-3.5-flash-lite`, OpenAI `gpt-transcribe` (29.07.2026), AssemblyAI Universal-3.5 Pro, ElevenLabs Scribe v2, Mistral Voxtral (3 seit 20.10.2026), Parakeet TDT 0.6B v3, Canary-1B-v2.
Herkunft: Studie `Recherchen/stt-vergleich` (Berichte r1–r10 mit Quelle je Aussage, Seite `index.html`, Messplatz `messung/`). Markierung: **[offiziell]** Herstellerdoku, **[extern]** Dritte, **[gemessen]** eigener Messplatz.

Verwandt: `best-practices/desktop/groq-transkription.md`, `best-practices/desktop/whisper-stt-lokal.md`, `best-practices/apis/tts-provider.md`, `bugs/apis/stt-provider.md`.

## §1 Deutsch-Qualität richtig beurteilen
- Einzige unabhängige Deutsch-Rangliste: HF Open ASR Leaderboard, Rohdatei https://huggingface.co/datasets/hf-audio/multilingual_evals/blob/main/multilingual_de.csv (Commit `3589b3c…` vom 19.10.2026), gemessen vom HF-Maintainer **[extern]**. Spalten FLEURS (vorgelesen) und MCV (Common Voice, kurze Crowd-Sätze — näher am Diktat).
- Auszug (FLEURS/MCV): Azure Speech 07-2026 1,83/2,01 · Scribe v2 2,30/2,19 · AssemblyAI U3.5 Pro 2,17/2,69 · gpt-transcribe 2,34/3,34 · Voxtral Small 24B 2,61/3,19 · Whisper large-v3 3,20/4,79 · Parakeet v3 4,16/4,07 · **Whisper Turbo 3,67/8,58 (Platz 30/41)**.
- Normalizer ignoriert Umlaute, Satzzeichen und deutsche Zahlen → sagt nichts über Eigennamen/Zahlen. Artificial Analysis (https://artificialanalysis.ai/speech-to-text) ist englischlastig. Derselbe Datensatz streut je Quelle bis 2,7 Punkte → nie Zahlen verschiedener Quellen mischen.
- Nicht in der Rangliste: Deepgram, Google Chirp 3/Gemini, Gladia, Groq-gehostete Varianten.

## §2 Groq Turbo → large-v3
- Gleiche API, gleicher Filter, eigene Limit-Zeile mit identischen Werten (20 RPM, 2.000 RPD, 7.200 ASH, 28.800 ASD) **[offiziell]** https://console.groq.com/docs/rate-limits. Preis 0,111 statt 0,04 $/h.
- Kann pro Organisation gesperrt sein (`model_permission_blocked_org`) → Freischalten unter https://console.groq.com/settings/limits **[gemessen]**.
- `distil-whisper-large-v3-en` ist seit 23.08.2025 abgeschaltet.

## §3 Freikontingente (in Audiostunden)
| Anbieter | Art | Menge |
|---|---|---|
| Groq Turbo/large-v3 | dauerhaft | 8 h/Tag ≈ 240 h/Monat |
| Cloudflare `@cf/openai/whisper-large-v3-turbo` | dauerhaft | 10.000 Neurons/Tag ≈ 214 Min./Tag (dasselbe Modell) |
| Azure F0 | dauerhaft | 5 h/Monat, nur Echtzeit-Standardmodell, 1 parallele Anfrage |
| Gemini 3.5 Transcribe | Free Tier | 3 RPM / 25 RPD **[gemessen]** |
| ElevenLabs Free | dauerhaft klein | ≈ 12 Min./Monat |
| Deepgram | einmalig | 200 $ ≈ 640–775 h, ohne Karte, ohne Ablauf |
| AssemblyAI | einmalig | 50 $ ≈ 111 h Sync / 185–238 h Async |
| Speechmatics | einmalig | 100 $ Neukunden, 12 Monate (Monatsstunden seit 01.08.2026 abgeschafft) |
| Gladia | einmalig | 50 €, 1 Jahr |
| OpenAI, Soniox, Chirp 3 (v2), Qwen international | keins | — |
| Mistral Free Mode | unbelegt für Audio | Training erlaubt |
Quelle: r10, r5, r4, r3.

## §4 Gemini als STT
- `gemini-3.5-transcribe` über `POST /v1beta/interactions`, Audio inline Base64, `transcription_config.mode.type = "verbatim"` (smart lässt Wörter weg), `custom_vocabulary` bis 1.000 (empfohlen ~100) **[offiziell/gemessen]**.
- Free Tier 3 RPM und **25 RPD** (429-Text: „limit: 25 requests per day on Free Tier“) **[gemessen 21.10.2026]**. Bezahlt ≈ 0,30 $/h (25 Audio-Tokens/s).
- Messplatz: mit Vokabular 3,5 % WER Testtext / 2,4 % Eigennamen, bei Stille leer, ≈ 4 s je Aufruf **[gemessen]**.
- Allgemeine Modelle (`gemini-3.8-flash`, `-3.5-flash-lite`) per `generateContent` mit Audio: genau, aber LLM-Risiken (§6). 32 Audio-Tokens/s.

## §5 Vokabular / Eigennamen
- Messplatz: Turbo ohne Prompt 11,9 % WER auf dem Eigennamen-Satz, mit Prompt („Fachbegriffe: …“) 4,8 % **[gemessen]**. „Groq“ erkannte nur Gemini mit Vokabelliste.
- Mechanismen: Whisper `prompt` (224 Tokens) · Gemini `custom_vocabulary` · OpenAI `gpt-transcribe` `keywords` + `prompt` · AssemblyAI `keyterms_prompt` (100 Sync/1.000 Async) · Deepgram `keyterm` (je Begriff ein Parameter, ≤ 500 Tokens) · ElevenLabs `keyterms` (1.000) · Voxtral `context_bias` (100, englisch optimiert) · Azure Phrase List.
- Gemeinsame Quelle der Liste: `SK/VoiceOverlays/personal-vocabulary.txt`.

## §6 Halluzinationen absichern
- Messplatz (12 s leises Rauschen): Turbo → „...“; Turbo+Prompt → gibt den Prompt aus; Gemini 3.8 Flash → erfindet ganze Sätze (mit und ohne Vokabelliste); Gemini 3.5 Transcribe, Flash-Lite, Parakeet, Canary → leer **[gemessen]**.
- Regel: VAD-Sperre VOR dem Upload für jeden Anbieter; LLM-Ausgaben bei kurzer/leiser Aufnahme verwerfen; leere 200-Antwort = „keine Sprache“.

## §7 Einbau (Jarvis M1.4, Overlays)
- Datei-Upload mit einem REST-Aufruf (Groq, Gemini Transcribe, AssemblyAI Sync, Deepgram REST, OpenAI) — Streaming spart beim Diktat kaum, weil 4,5 s Stille am Ende dominieren; besser vorgezogene Datei-Transkription nach ~1,2 s Stille.
- M1.4: Schnittstelle `Transkribierer`, `Diktat` nimmt die Schnittstelle, `Anbindung.kt` wählt primär/Rückfall. Änderungen nur in der Modul-Bibliothek (Skill `modul-erstellen`), dann nachziehen.
- Rückfall auf Groq bei Netz/429/5xx/leerer 200; 401/403 melden.

## §8 Eigener Messplatz
- `Recherchen/stt-vergleich/messung/messen.py [anbieter …]` (Schlüssel zur Laufzeit aus `~/SK`, Wiederholung bei 429/503, nur fehlende Einträge), `messung/lokal.py` (onnx-asr), `seite/bauen.py` (WER mit Kanonformen für Datum, Uhrzeit, Kommazahlen, Euro, Abkürzungen, Komposita).
- Groq blockt den Python-Standard-User-Agent mit 403 → eigenen User-Agent setzen.
- Synthetische TTS-Sprache misst Zahlen/Eigennamen, nicht echte Diktatgenauigkeit → für die Entscheidung 30–50 eigene Diktate nehmen.

## §9 Datenschutz
- Groq: kein Training, Speicherung/Verarbeitung USA. Gemini API: bezahlt kein Training; Free-Tier-Datenregeln für EWR wie Paid, aber Apps an Dritte nur Paid (https://ai.google.dev/gemini-api/terms). Deepgram: Training standardmäßig → `mip_opt_out=true`. AssemblyAI EU-Endpunkt: kein Training. OpenAI: kein Training, EU-Residenz nur mit Freigabe. Mistral Free Mode: Training erlaubt.

## §10 Lokal
- Parakeet TDT 0.6B v3 int8: onnx-asr (Windows), sherpa-onnx (Android), FluidAudio (Mac). Messplatz: Testtext 13,2 % (Turbo 10,1 %), Rauschen 45 %, Zahlen wie „30 5 Millisekunden“; Laptop-CPU ≈ 12 s für 40 s Audio **[gemessen]**.
- Canary-1B-v2 deckt 25 Sprachen ab, im Messplatz schwächstes Modell.
- Deutsch-Finetunes (primeline Turbo German −28 %, parakeet-primeline −19 %) nur Eigenangaben, keine gehostete API.
