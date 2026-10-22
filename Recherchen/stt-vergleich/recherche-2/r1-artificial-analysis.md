# R1 – Artificial Analysis Speech-to-Text Leaderboard (Stand der Quelle: abgerufen 22.10.2026)

Quelle Rangliste (Hauptseite, "AA-WER v2", Non-Streaming): https://artificialanalysis.ai/speech-to-text und https://artificialanalysis.ai/speech-to-text/non-streaming
Die Seite nennt kein Veröffentlichungsdatum; Speed Factor = Median der letzten 7 Tage. Die Seite zählt inkonsistent (Tabelle 62 Zeilen; Text nennt auch "59" und "75 Modelle"). Alle 62 Zeilen sind unten erfasst.

## 1. WICHTIGSTE ERKENNTNIS FÜR DEUTSCH

- Die AA-WER-Messung ist **rein englisch**: Methodikseite "about 8 hours of English audio" (https://artificialanalysis.ai/speech-to-text/methodology). AA-AgentTalk (469 Samples, ca. 250 Min., 17 Akzentgruppen, aber Englisch), VoxPopuli-Cleaned-AA (englische Teilmenge, 628 Samples, ca. 119 Min.), Earnings22-Cleaned-AA (6 Telefonkonferenzen, ca. 115 Min.). Der Normalizer baut auf dem englischen Whisper-Normalizer auf (https://artificialanalysis.ai/articles/aa-wer-v2).
- Es gibt **keine** sprachspezifische oder multilinguale Auswertung (kein Deutsch) auf dem Leaderboard; weder Hauptseite noch Methodik noch der AA-WER-v2-Artikel erwähnen Mehrsprachigkeit. Zwei Suchen fanden keine Deutsch-Zahlen bei AA.
- Folge: Die AA-WER-Zahlen sagen NICHTS direkt über deutsche Genauigkeit aus. Sie taugen für Speed/Preis und als grobe Englisch-Qualität. Für Deutsch braucht es eine eigene Messung (Messplatz).

## 2. Methodik (Kurzfassung)

Quelle: https://artificialanalysis.ai/speech-to-text/methodology, https://artificialanalysis.ai/articles/aa-wer-v2 (17.02.2026)
- AA-WER = gewichtetes Mittel: 50 % AA-AgentTalk (proprietär, zurückgehalten, Sprache an Voice Agents), 25 % VoxPopuli-Cleaned-AA, 25 % Earnings22-Cleaned-AA. Innerhalb jedes Datensatzes nach Audiodauer gewichtet.
- WER = (Substitutionen + Einfügungen + Löschungen) / Wörter in Referenz (Levenshtein); Referenz = verifizierte menschliche Transkripte; Whisper-Normalizer plus Zusatzregeln (Zahlen, IDs, Telefonnummern, buchstabierte Buchstaben). Prompt "verbatim transkribieren", wo möglich.
- Speed Factor (Batch) = Audiodauer / API-Antwortzeit, inkl. Netzwerklatenz; Balken = Median der letzten 7 Tage über 10-Minuten-Clips (laut Hauptseite). Ein Wert von 2,0 heißt 10 Min. Audio in 5 Min.
- Preis: USD pro 1000 Minuten, normalisiert (Dauer-, Zeit-, Token- oder Abo-basiert). "0.00" erscheint bei 4 Modellen (Qwen3.5 Omni Flash/Plus, Cohere transcribe-03-2026, Parakeet TDT 0.6B V2) – ohne Erklärung der Seite (vermutlich kostenlos/Open Weights bzw. kein Preis hinterlegt).
- Streaming ist ein SEPARATES Board "AA-WER Streaming" (gestartet 01.06.2026): https://artificialanalysis.ai/speech-to-text/streaming und https://artificialanalysis.ai/articles/new-streaming-speech-to-text-benchmark-aa-wer-streaming – gleiche ca. 8 h, Latenz ab Sprachende per SileroVAD. Pro-Modell-Zahlen der Streaming-Seite stehen nur in interaktiven Charts (per Fetch nicht lesbar; 47 Modelle laut Seite).

## 3. Vollständige Rangliste (alle 62 Zeilen, sortiert nach AA-WER aufsteigend)

Spalten: Rang nach WER | Modell | Anbieter | AA-WER % | Speed Factor | USD/1000 Min | Rechenzeit für 12 s Audio (= 30 Wörter bei ca. 150 Wörter/Min.) = 12 s / Speed Factor | Streaming/Echtzeit.
Hinweis Rechenzeit: Der Speed Factor wurde an 10-Min.-Clips gemessen; bei 12 s Audio dominieren Netz-Overhead und Anlaufzeit, die Rechenzeit unten ist daher eine **Untergrenze**, nicht die reale Wartezeit. Eine belastbare AA-Zahl für typischen Netz-Overhead pro Request wurde nicht gefunden.
Streaming-Spalte: "ja" nur wo eine Quelle ein Echtzeit-Produkt belegt; "?" = die AA-Hauptseite macht dazu keine Angabe; "nein" = Batch/LLM.

| # | Modell | Anbieter | AA-WER % | Speed Factor | USD/1000 Min | 12 s Audio in | Streaming |
|---|---|---|---|---|---|---|---|
| 1 | Fun-Realtime-ASR-preview | Alibaba Cloud | 1.7 | 20.3 | 2.82 | 0.59 s | ja (Realtime per WebSocket) |
| 2 | MAI-Transcribe-2 | Microsoft AI | 2.0 | 307.0 | 1.67 | 0.04 s | Batch; Streaming-Variante MAI-Transcribe-2-Streaming separat (2.5 % Streaming-WER) |
| 3 | Voxtral Small Transcribe 3 | Mistral | 2.1 | 64.9 | 4.00 | 0.18 s | ? |
| 4 | Scribe v2 | ElevenLabs | 2.2 | 69.0 | 3.67 | 0.17 s | ja (Scribe v2 Realtime separat, 6.50 USD/1000 min) |
| 5 | Voxtral Mini Transcribe 3 | Mistral | 2.2 | 76.1 | 3.50 | 0.16 s | ? (Voxtral Mini Transcribe Realtime separat) |
| 6 | Grok Voice Transcribe 2.0 | SpaceXAI | 2.3 | 195.1 | 1.67 | 0.06 s | ja (Streaming ca. 2.73 % laut Drittquelle) |
| 7 | MAI-Transcribe-1.5 | Microsoft AI | 2.4 | 80.4 | 6.00 | 0.15 s | ? |
| 8 | StepAudio 3 ASR | StepFun | 2.5 | 64.2 | 6.67 | 0.19 s | ? |
| 9 | Gemini 3.5 Transcribe | Google | 2.6 | 89.5 | 5.00 | 0.13 s | ? |
| 10 | Gemini 3.1 Pro Preview (High) | Google | 2.8 | 8.2 | 18.15 | 1.46 s | nein (LLM-Batch) |
| 11 | Voxtral Small | Mistral | 2.8 | 55.2 | 4.00 | 0.22 s | ? |
| 12 | Zoom Scribe V2 Pro | Zoom | 2.8 | 123.9 | 3.33 | 0.10 s | ? |
| 13 | Gemini 2.5 Pro | Google | 2.9 | 12.3 | 11.39 | 0.98 s | nein (LLM-Batch) |
| 14 | Gemini 3 Flash (High) | Google | 2.9 | 21.2 | 13.70 | 0.57 s | nein (LLM-Batch) |
| 15 | Universal-3.5 Pro | AssemblyAI | 3.0 | 102.4 | 3.50 | 0.12 s | ? (U3 Realtime Pro separat im Streaming-Board) |
| 16 | Universal-3 Pro | AssemblyAI | 3.1 | 103.2 | 3.50 | 0.12 s | ? (U3 Realtime Pro separat im Streaming-Board) |
| 17 | Solaria-3 (Gladia) | Gladia | 3.2 | 89.8 | 10.16 | 0.13 s | ? |
| 18 | GPT Transcribe | OpenAI | 3.3 | 55.8 | 4.50 | 0.22 s | ? (GPT Realtime Whisper separat) |
| 19 | Smallest AI Pulse | Smallest.ai | 3.3 | 260.7 | 5.00 | 0.05 s | ? |
| 20 | Gemini 3.1 Flash-Lite Preview (Minimal) | Google | 3.4 | 90.0 | 5.83 | 0.13 s | nein (LLM-Batch) |
| 21 | Resonant-1 | Reson8 | 3.4 | 331.7 | 3.60 | 0.04 s | ? |
| 22 | Smallest AI Pulse Pro | Smallest.ai | 3.4 | 295.6 | 4.00 | 0.04 s | ? |
| 23 | Qwen3.5 Omni Plus | Alibaba Cloud | 3.5 | 137.3 | 0.00 | 0.09 s | ? |
| 24 | Gemini 3.1 Pro Preview (Low) | Google | 3.6 | 7.9 | 7.72 | 1.52 s | nein (LLM-Batch) |
| 25 | Voxtral Mini Transcribe 2 | Mistral | 3.6 | 84.4 | 3.00 | 0.14 s | ? |
| 26 | Soniox v5 Async | Soniox | 3.8 | 42.6 | 1.66 | 0.28 s | nein (Async; Soniox Realtime separat) |
| 27 | Universal | AssemblyAI | 3.8 | 105.4 | 2.50 | 0.11 s | ? |
| 28 | Voxtral Mini | DeepInfra | 3.8 | 50.6 | 1.00 | 0.24 s | ? |
| 29 | Inworld STT 1 | Inworld | 3.9 | 143.5 | 2.50 | 0.08 s | ? |
| 30 | GPT-4o Transcribe | OpenAI | 4.0 | 37.8 | 6.00 | 0.32 s | ? |
| 31 | Speechmatics Enhanced | Speechmatics | 4.0 | 66.2 | 12.50 | 0.18 s | ? |
| 32 | Amazon Transcribe | Amazon Bedrock | 4.1 | 35.6 | 6.00 | 0.34 s | ? |
| 33 | Solaria-1 (Gladia) | Gladia | 4.1 | 92.1 | 10.17 | 0.13 s | ? |
| 34 | Whisper Large v2 | OpenAI | 4.1 | 28.8 | 6.00 | 0.42 s | ? |
| 35 | Whisper Large v3 (fal.ai) | fal.ai | 4.1 | 97.3 | 1.15 | 0.12 s | ? |
| 36 | Modulate STT Batch English VFast | Modulate | 4.2 | 47.0 | 0.42 | 0.26 s | nein (Batch, nur Englisch) |
| 37 | Canary Qwen 2.5B | Replicate | 4.3 | 8.7 | 0.74 | 1.38 s | ? |
| 38 | Chirp 3 | Google | 4.3 | 28.1 | 16.00 | 0.43 s | ja (Chirp 3 Streaming im Streaming-Board) |
| 39 | GPT-4o Mini Transcribe | OpenAI | 4.5 | 51.0 | 3.00 | 0.24 s | ? |
| 40 | Parakeet TDT 0.6B V3 | Together AI | 4.5 | 617.7 | 1.50 | 0.02 s | ? |
| 41 | Whisper Large v3 (Together AI) | Together AI | 4.5 | 285.4 | 1.50 | 0.04 s | ? |
| 42 | Whisper Large v3 Turbo (Groq) | Groq | 4.6 | 200.8 | 0.67 | 0.06 s | ? |
| 43 | transcribe-03-2026 | Cohere | 4.6 | 105.9 | 0.00 | 0.11 s | ? |
| 44 | StepAudio 2.5 ASR | StepFun | 4.7 | 87.1 | 0.37 | 0.14 s | ? |
| 45 | Wizper Large v3 | fal.ai | 4.7 | 144.1 | 0.50 | 0.08 s | ? |
| 46 | Nova-3 (Telnyx) | Telnyx | 4.8 | 471.5 | 7.40 | 0.03 s | ? |
| 47 | Nova 2 Pro | Amazon Bedrock | 4.9 | 23.9 | 3.10 | 0.50 s | ? |
| 48 | Speechmatics Melia | Speechmatics | 4.9 | 175.2 | 4.00 | 0.07 s | ? |
| 49 | Gemini 2.5 Flash | Google | 5.1 | 67.0 | 6.66 | 0.18 s | nein (LLM-Batch) |
| 50 | Nova-2 (Telnyx) | Telnyx | 5.1 | 462.1 | 7.40 | 0.03 s | ? |
| 51 | Speechmatics Standard | Speechmatics | 5.1 | 97.4 | 7.50 | 0.12 s | ? |
| 52 | Gemini 2.5 Flash Lite | Google | 5.2 | 75.7 | 6.56 | 0.16 s | nein (LLM-Batch) |
| 53 | Nova-3 | Deepgram | 5.2 | 546.2 | 4.30 | 0.02 s | ja (Nova-3 Realtime im Streaming-Board) |
| 54 | Parakeet RNNT 1.1B | Replicate | 5.4 | 6.4 | 1.91 | 1.88 s | ? |
| 55 | Whisper Large v3 Turbo (Telnyx) | Telnyx | 5.5 | 37.3 | 15.00 | 0.32 s | ? |
| 56 | Incredibly Fast Whisper | Replicate | 5.7 | 53.1 | 1.49 | 0.23 s | ? |
| 57 | Rev AI | Rev AI | 5.9 | 13.0 | 3.33 | 0.92 s | ? |
| 58 | Parakeet TDT 0.6B V2 | NVIDIA | 6.4 | 95.1 | 0.00 | 0.13 s | ? |
| 59 | Gradium Speech-to-Text | Gradium | 6.8 | 2.3 | 13.00 | 5.22 s | ? (Gradium im Streaming-Board gelistet) |
| 60 | Whisper Large v3 (Replicate) | Replicate | 10.1 | 2.9 | 4.23 | 4.14 s | ? |
| 61 | Qwen3.5 Omni Flash | Alibaba Cloud | 13.5 | 69.7 | 0.00 | 0.17 s | ? |
| 62 | Chirp | Google | 31.2 | 14.2 | 16.00 | 0.85 s | ? |

## 4. Die vom Nutzer genannten Modelle

| Genannt | Was AA tatsächlich listet | WER | Speed | USD/1000 Min |
|---|---|---|---|---|
| MAI-Transcribe 1.5 | MAI-Transcribe-1.5 (Microsoft AI) | 2.4 | 80.4 | 6.00 |
| MAI-Transcribe 2 | MAI-Transcribe-2 (Microsoft AI) | 2.0 | 307.0 | 1.67 |
| Voxtral Transcribe | Voxtral Small Transcribe 3 / Voxtral Mini Transcribe 3 / Voxtral Mini Transcribe 2 / Voxtral Small / Voxtral Mini (DeepInfra) | 2.1 / 2.2 / 3.6 / 2.8 / 3.8 | 64.9 / 76.1 / 84.4 / 55.2 / 50.6 | 4.00 / 3.50 / 3.00 / 4.00 / 1.00 |
| Fun-ASR Realtime Preview | "Fun-Realtime-ASR-preview" (Alibaba Cloud), Platz 1 | 1.7 | 20.3 | 2.82 |
| ElevenLabs Scribe v2 | Scribe v2 | 2.2 | 69.0 | 3.67 |
| Gemini | Gemini 3.5 Transcribe 2.6 (Speed 89.5; 5.00 USD); Gemini 3.1 Pro Preview High 2.8 / Low 3.6; Gemini 3 Flash (High) 2.9; Gemini 2.5 Pro 2.9; Gemini 3.1 Flash-Lite Preview (Minimal) 3.4; Gemini 2.5 Flash 5.1; 2.5 Flash Lite 5.2; Chirp 3 4.3; Chirp 31.2 | siehe Tabelle | | |
| "...3.5 omni..." | Sehr wahrscheinlich **Qwen3.5 Omni Plus** (Alibaba Cloud, WER 3.5, Speed 137.3, Preis 0.00) bzw. **Qwen3.5 Omni Flash** (WER 13.5, Speed 69.7, Preis 0.00). Weitere "3.5"-Modelle: Universal-3.5 Pro (AssemblyAI, WER 3.0, Speed 102.4, 3.50 USD) und Gemini 3.5 Transcribe – passen aber nicht zu "omni". | | | |

Fun-Realtime-ASR-preview: Platz 1 bei WER, aber nur Speed Factor 20,3 (12 s Audio in ca. 0,59 s im Batch-Maßstab) und **Deutsch ungeklärt**: Alibabas Doku listet für den Realtime-Snapshot fun-asr-realtime-2025-09-15 nur Chinesisch und Englisch (Suchtreffer: https://help.aliyun.com/en/model-studio/fun-asr-realtime-python-sdk und https://www.alibabacloud.com/help/ja/model-studio/asr-model/); die allgemeine Fun-ASR-Seite nennt 30 Sprachen inkl. Deutsch (https://www.alibabacloud.com/help/en/model-studio/fun-asr), ein Drittbericht ebenfalls 30 Sprachen (https://www.remio.ai/post/fun-asr-realtime-launches-single-model-for-30-languages-and-16-dialects). Für den aktuellen Realtime-Snapshot nicht bestätigt – vor Nutzung prüfen.

## 5. Streaming-Board (AA-WER Streaming) – soweit lesbar

Quelle: https://artificialanalysis.ai/articles/new-streaming-speech-to-text-benchmark-aa-wer-streaming (01.06.2026, Launch-Stand; spätere Werte können abweichen)

| Modell | WER final | Latenz final (ab Sprachende) | WER First Partial | Preis/1000 Min |
|---|---|---|---|---|
| Cartesia Ink-2 (semantic endpoints) | 3.59 % | 0.21 s | – | – |
| ElevenLabs Scribe v2 Realtime | 3.64 % | 0.14 s | 3.65 % (0.13 s) | 6.50 USD |
| Cartesia Ink-2 (external endpoints) | 3.66 % | 0.09 s | 4.33 % (0.07 s) | 4 USD |
| AssemblyAI U3 Realtime Pro | – | – | 4.46 % (0.47 s) | – |
| Deepgram Nova-3 Realtime | – | 0.06 s | 6.69 % (0.057 s) | – |
| Deepgram Flux | 7.36 % | 0.020 s | 7.36 % | – |
| Soniox Realtime | – | 0.06 s | – | 2 USD (billigster) |
| Voxtral Mini Transcribe Realtime | 5.3 % | – | 9.6 % | – |
| OpenAI GPT Realtime Whisper | 5.1 % | – | 7.5 % | – |

Später: MAI-Transcribe-2-Streaming 2.5 % WER final und First Partial, Capture 01.10.2026 (https://benchlm.ai/voice-benchmarks/aa-wer-streaming-mai-transcribe-2; Latenz 0.13 s nur laut Vendor-/Drittquelle https://www.orcarouter.ai/blog/mai-transcribe-2-streaming-release). Grok Voice Transcribe 2.0 ca. 2.73 % bei 0.49 s und Scribe v2 Realtime 3.59 % bei 0.14 s laut Drittquelle (Suchsnippet, nicht verifiziert). MAI-Transcribe-2(-Streaming) unterstützt laut Drittquelle 60 Sprachen (https://pasqualepillitteri.it/en/news/19840/microsoft-mai-transcribe-2-streaming; Deutsch dort nicht ausdrücklich bestätigt); MAI-Transcribe-1.5: 43 Sprachen (https://artificialanalysis.ai/articles/mai-transcribe-1-5-new-speech-to-text-model-leading-the-accuracy-speed-pareto-frontier, 02.06.2026; dort 276x Speed, 6 USD).

## 6. Auffälligkeiten / Widersprüche

- Speed-Factor-Schwankung: MAI-Transcribe-1.5 war im AA-Artikel (02.06.2026) "ca. 276x", heute 80.4; MAI-Transcribe-2 heute 307.0, eine Drittquelle nennt 410.7. Speed ist ein 7-Tage-Median und schwankt stark.
- AA-WER-Version: Hauptseite "v2"; ein AA-Post zu Voxtral 3 spricht laut Suchsnippet von "v2.5". Zahlen nur innerhalb einer Version vergleichen. Beim Launch von v2 (17.02.2026): Scribe v2 2.3 % (jetzt 2.2 %).
- Ausreißer: Qwen3.5 Omni Flash 13.5 %, Chirp (v1) 31.2 %, Whisper Large v3 bei Replicate 10.1 % vs. 4.1 % bei fal.ai (Konfiguration).
- Tabelle enthält 62 Zeilen, Seitentexte nennen 59 bzw. 75 Modelle.
- Die Streaming-Spalte in Abschnitt 3 ist nur teilweise belegt; AA zeigt Streaming auf einem getrennten Board mit anderen Modellvarianten.

## Quellen (alle am 22.10.2026 abgerufen)
- https://artificialanalysis.ai/speech-to-text und /speech-to-text/non-streaming (Tabelle, 62 Zeilen)
- https://artificialanalysis.ai/speech-to-text/methodology
- https://artificialanalysis.ai/articles/aa-wer-v2 (17.02.2026)
- https://artificialanalysis.ai/articles/new-streaming-speech-to-text-benchmark-aa-wer-streaming (01.06.2026)
- https://artificialanalysis.ai/articles/mai-transcribe-1-5-new-speech-to-text-model-leading-the-accuracy-speed-pareto-frontier (02.06.2026)
- https://artificialanalysis.ai/speech-to-text/streaming (nur Methodik, Zahlen in Charts)
- https://x.com/ArtificialAnlys/status/2112587905222709432 (Voxtral 3: Small Transcribe 3 auf Platz 3; nur Suchsnippet, Fetch gab 402)
- https://benchlm.ai/voice-benchmarks/aa-wer-streaming-mai-transcribe-2
- https://www.alibabacloud.com/help/en/model-studio/fun-asr ; https://help.aliyun.com/en/model-studio/fun-asr-realtime-python-sdk (Suchtreffer)
- https://pasqualepillitteri.it/en/news/19840/microsoft-mai-transcribe-2-streaming (Drittquelle)

BEST-PRACTICES-KANDIDATEN:
- AA-WER ist ein reiner Englisch-Benchmark (https://artificialanalysis.ai/speech-to-text/methodology, AA-WER v2, Stand 22.10.2026): für die Deutsch-STT-Auswahl nicht allein verwenden, eigene Deutsch-Messung nötig.
- Speed Factor von AA gilt für 10-Min.-Clips inkl. Netz; für kurze Diktate (12 s) eigene Latenzmessung statt 12 s / Speed Factor (https://artificialanalysis.ai/speech-to-text/methodology).

BUG-KANDIDATEN:
- KEINE
