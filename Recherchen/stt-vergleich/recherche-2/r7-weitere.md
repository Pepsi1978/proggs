# R7 – Weitere STT-Anbieter mit Spitzen-WER (Stand 22.10.2026)

Recherchiert am 22.10.2026. Alle Zahlen mit Quelle. "Nicht gefunden" heisst: in den abgerufenen Quellen nicht belegt, nicht geraten.

## 0. Wichtigste Erkenntnisse (Kurz)

1. **Artificial Analysis (AA) misst NUR Englisch** (AA-WER = AA-AgentTalk 50 % + VoxPopuli 25 % + Earnings22 25 %, ca. 8 h). Es gibt dort KEINE Deutsch-Werte (Quelle: https://artificialanalysis.ai/speech-to-text/non-streaming, abgerufen 22.10.2026, Seite ohne Datum, "(c) 2026"). Ein AA-Platz sagt also wenig ueber Deutsch.
2. **Nicht-ausgeschlossene Modelle in den AA-Top-15** (nach Ausschluss von Microsoft, Mistral, Alibaba, ElevenLabs, Deepgram, AssemblyAI, Google): **Grok Voice Transcribe 2.0 (Rang 6, 2,3 %)**, **StepAudio 3 ASR (Rang 8, 2,5 %)**, **Zoom Scribe V2 Pro (Rang 12, 2,8 %)**. Direkt dahinter: Solaria-3 (17, 3,2 %), GPT Transcribe (18, 3,3 %), Smallest Pulse (19, 3,3 %), Pulse Pro/Resonant-1 (3,4 %), Soniox v5 Async (28, 3,8 %).
3. **Fuer Deutsch + Diktat (12 s, 30 Woerter) am interessantesten:** Grok Voice Transcribe 2.0 (sehr billig, Streaming, Deutsch dokumentiert), Soniox v5 (260 ms Streaming-Latenz, 0,10-0,12 USD/h), Groq Whisper v3 turbo (schnellster Batch, 0,04 USD/h, Gratis-Tier), OpenAI gpt-transcribe (0,27 USD/h). Reson8 hat in der einzigen Deutsch-Messung (FLEURS, Herstellertabelle) den besten Wert (5,3 %).
4. **Cartesia Ink-2 kann kein Deutsch im Streaming** (nur Englisch; Deutsch faellt auf ink-whisper zurueck) -> fuer dieses Projekt ungeeignet.
5. **Zoom Scribe "Pro" (zoom-scribe-en-pro) ist laut Doku fuer Englisch**; das AA-Ergebnis (2,8 %) gilt dem Pro-Modell, deutsche Nutzung liefe ueber `zoom-scribe-multilingual` (10 Sprachen, Liste nicht genannt).
6. Deutsche WER-Zahlen existieren fast nur als **Herstellertabellen** (AssemblyAI, Soniox, ElevenLabs, Famulor) und sind untereinander NICHT vergleichbar (andere Normalisierung/Datensaetze). Echte Entscheidung nur per eigenem Deutsch-Test.

## 1. Hauptergebnis-Tabelle

Legende: AA-WER = Artificial Analysis, Englisch (AA-WER v2, Non-Streaming-Board). SF = Speed Factor (AA, Median letzte 7 Tage; Sekunden Audio pro Sekunde Rechenzeit, Batch). Preis: USD/Stunde Audio (AA gibt USD/1000 min an; /1000 min x 0,06 = USD/h). Antwortzeit = Schaetzung, siehe Abschnitt 3.

| Modell (Anbieter) | AA-WER (AA-Rang) | WER Deutsch (falls bekannt, Quelle/Art) | Speed / Latenz | Echtzeit-Streaming | Preis USD/h | Freikontingent | Besonderheiten |
|---|---|---|---|---|---|---|---|
| **Grok Voice Transcribe 2.0** (SpaceXAI/xAI) | 2,3 % (Rang 6) | FLEURS ca. 10,9 (unverifiziert, Spaltenzuordnung unklar) bzw. Common Voice 11,7 (AssemblyAI-Herstellertabelle, Streaming) | SF 195,1; Streaming "time to final" 0,373 s (AA, gelesen 09.09.2026); Zwischenergebnisse ca. alle 500 ms | Ja (WebSocket wss://api.x.ai/v1/stt) | Batch 0,10 / Streaming 0,20 | Kein STT-spezifisches Gratis-Kontingent belegt; Berichte nennen 25 USD Startguthaben fuer neue xAI-Konten (30 Tage) und ein Daten-Teilen-Programm (widerspruechlich) | 38+ Sprachen, Deutsch (`de`) dokumentiert, Auto-Spracherkennung, `format=true` formatiert Zahlen/Einheiten auch auf Deutsch, Diarization/Timestamps/Key-Terms inklusive, nur API, keine offenen Gewichte; erschienen ca. 17.-19.09.2026 |
| **StepAudio 3 ASR (Max)** (StepFun) | 2,5 % (Rang 8) – Presseberichte nennen 1,7 % (siehe Hinweis) | nicht gefunden; Deutsch-Unterstuetzung nicht belegt | SF 64,2 | Nein (Non-Streaming laut Bericht) | AA: 6,67 USD/1000 min = 0,40 USD/h; chinesische Quelle 2,8 CNY/h (ca. 0,39 USD) | nicht gefunden | Release 22.09.2026; Sprachliste nicht veroeffentlicht, Fokus Chinesisch/Englisch; fuer Deutsch riskant |
| **Zoom Scribe V2 Pro** (Zoom) | 2,8 % (Rang 12) | Zoom Scribe (Live) FLEURS 6,5 (AssemblyAI-Herstellertabelle, Aug-Sep 2026) | SF 123,9 | Ja ("Live", WebSocket) | Batch 0,15 / Fast 0,20 / Live 0,25 (Pro: 0,30 / 0,35 / 0,40); AA: 0,20 | kein Free Tier; Prepaid-Guthaben, Playground nach Anmeldung | Modelle: `zoom-scribe-multilingual` (10 Sprachen, nicht benannt), `whisper` (+26 Locales), `zoom-scribe-en-pro` (Englisch); Zusatz Spracherkennung 0,048/h, Diarization 0,048/h |
| **GPT Transcribe** `gpt-transcribe` (OpenAI) | 3,3 % (Rang 18) | nicht gefunden (OpenAI nennt keine Sprachliste/WER); Vorgaenger gpt-4o-transcribe: FLEURS 13,1 (AssemblyAI-Tabelle, Realtime-Lauf) | SF 55,8; kein Live-Modell (Datei/gestreamte Datei/committed Turns) | Teilweise: Realtime-Sitzung nur nach committed Turn; echtes Live = gpt-live-transcribe | 0,0045 USD/min = **0,27** | kein generelles Freikontingent belegt | Release 28./29.07.2026; Kontext, Keywords, mehrere Sprachhinweise (`languages`), Spracherkennung (nur Datei) |
| **GPT Live Transcribe** `gpt-live-transcribe` (OpenAI) | nicht im AA-Board gefunden | Common Voice (22 Sprachen) 19,7 % gesamt, Real-World (9 Sprachen) 9,6 % gesamt (Hersteller; kein Deutsch einzeln) | einstellbare Latenz (minimal, low, medium, high, xhigh), keine festen ms-Werte; Vorgaenger gpt-realtime-whisper: TTFS 740 ms Median / 878 ms P95 (Pipecat, Aug 2026) | **Ja** (WebSocket/WebRTC) | 0,017 USD/min = **1,02** | kein Freikontingent belegt | keine Wort-Zeitstempel, keine Sprecher-Labels, keine Confidence; Completed-Events koennen ausser Reihenfolge ankommen |
| **GPT-4o Transcribe** (OpenAI) | 4,0 % (Rang 31) | FLEURS 13,1 (AssemblyAI-Tabelle) | SF 37,8; TTFS 637 / 965 ms (Pipecat) | Ja (Realtime) | AA 6,00/1000 min = 0,36 | – | Vorgaenger-Modell, weiter nutzbar |
| **GPT-4o Mini Transcribe** (OpenAI) | 4,5 % (Rang 40) | FLEURS 14,9 (AssemblyAI-Tabelle) | SF 51,0 | Ja | AA 3,00/1000 min = 0,18 | – | – |
| **Groq whisper-large-v3-turbo** | 4,6 % (Rang 42, AA) | Whisper v3 (OpenAI-Modell, anderer Host): FLEURS de 4,5 (ElevenLabs-Marketingseite, nicht neutral) | **SF 200,8 (AA)**, 216 (Groq-Doku); schnellster Batch-Weg | Nein (Datei-API, 10-s-Mindestabrechnung) | **0,04** | **Gratis-Tier: 20 Req/min, 2000 Req/Tag, 7200 Audio-Sek/h, 28800 Audio-Sek/Tag** (Developer-Plan-Basis laut Groq-Doku) | Groq-Doku selbst gibt 12 % WER (eigener Test), `language=de` setzen verbessert Genauigkeit und Latenz |
| **Groq whisper-large-v3** | 4,1 % bei fal.ai; Groq-Variante nicht im AA-Board | wie oben | SF 189 (Groq-Doku) | Nein | **0,111** | wie oben | Groq-Doku: 10,3 % WER, empfohlen fuer mehrsprachige Genauigkeit; Uebersetzung nach EN moeglich |
| **Soniox v5** (`stt-rt-v5`, Async) | 3,8 % (Rang 28, Async) | Famulor (13.10.2026, nur 161 Woerter!): 3,73 %; AssemblyAI FLEURS Realtime 6,9 % | Async SF 42,6; **Streaming TTFS 260 ms Median / 305 ms P95** (Pipecat, Aug 2026, Streaming-WER 1,34 % semantisch) | **Ja** | Async ca. 0,10, Streaming ca. 0,12 (Token-Abrechnung: 1,5 bzw. 2,0 USD / 1 Mio Audio-Tokens, ca. 30 000 Tokens/h) | **Unklar/widerspruechlich:** Blog 27.10.2025: keine Free Credits mehr fuer Neuanmeldungen (vorher 200 USD); Preisseite wirbt noch mit 200 USD ohne Datum; Quickstart nennt 300 Min | 60+ Sprachen, Uebersetzung (Aufpreis ca. 0,06/h), Diarization |
| **Gladia Solaria-3** | 3,2 % (Rang 17) | AssemblyAI FLEURS "Gladia Solaria": 10,9 %; Gladia selbst: Deutsch nur ca. +3 % relativ gegen Solaria-1 (Eigenmessung) | SF 89,8 (Solaria-1: 92,1); Async mit Polling | Ja (Realtime-Tarif, 30 gleichz. Sitzungen) | Gladia-Seite: Async Solaria-3 **0,16 EUR/h**, Solaria-1 0,27 EUR/h, Realtime 0,68 EUR/h; AA-Preis 10,16/1000 min = 0,61 USD/h weicht ab (AA vermutlich anderer Tarif/aelter) | **50 EUR Startguthaben, 1 Jahr gueltig** (ca. 312 h Async Solaria-3 oder 73 h Realtime) | Release 10.06.2026; Solaria-3 ist bei Multilingual (MLS) 36 % schlechter als Solaria-1 -> fuer Mehrsprachigkeit Solaria-1 |
| **Speechmatics Enhanced** | 4,0 % (Rang 30) | AssemblyAI: Pre-recorded 10,30 %, Realtime FLEURS 6,8 % (Herstellertabelle) | SF 66,2; Realtime TTFS 495 ms / 676 ms (Pipecat) | Ja | Realtime 0,80 / Batch 0,75 (mit Trainings-Rabatt 0,54 / 0,50) | **100 USD Startguthaben, keine Kreditkarte**; Free Tier: 2 gleichzeitige Realtime-Sitzungen; danach pausiert | Deutsch unterstuetzt (Enhanced Sprache pro Transkript waehlen) |
| **Speechmatics Melia 1** | 4,9 % (Rang 48) | nicht gefunden | **SF 175,2** | Ja | Realtime 0,40 / Batch **0,24** | wie oben | 56 Sprachen ohne Vorab-Sprachwahl, Sprachwechsel im Satz |
| **Speechmatics Standard** | 5,1 % | AssemblyAI FLEURS Realtime 8,2 % | SF 97,4 | Ja | 0,45 / 0,45 | wie oben | – |
| **Smallest AI Pulse / Pulse Pro** | 3,3 % (19) / 3,4 % (21) | Common Voice 11,3 bzw. FLEURS 11,6 (unverifiziert) (AssemblyAI) | SF 260,7 / 295,6; Streaming "time to final" 0,132 s (AA, 09.09.2026), TTFS 398 / 533 ms (Pipecat) | Ja (Pricing-Seite nennt nur Realtime) | Seite: ca. 0,009 USD/min = 0,54/h; AA: 5,00 bzw. 4,00 /1000 min = 0,30 / 0,24 | 10 USD Startguthaben (Pay-as-you-go) | Sprachliste inkl. Deutsch auf Preisseite nicht genannt |
| **Reson8 Resonant-1** | 3,4 % (Rang 22) | **FLEURS 5,3 % (beste gemessene Deutsch-Zahl dieser Liste, AssemblyAI-Herstellertabelle, nur <18 Sprachen)** | SF 331,7 | nicht belegt (Telnyx bietet "turn-based STT") | AA 3,60/1000 min = **0,22** | nicht gefunden | Europaeischer Anbieter (EUR 5 Mio Seed), 10-24 europaeische Sprachen, Deutsch dabei (Telnyx-Release-Notes Sep 2026) |
| **NVIDIA Nemotron 3.5 ASR** (multilingual, Open Weights) | nicht im Non-Streaming-Board; Pipecat (en-Datensatz): 4,54 % semantisch; Nemotron 3.0 (en): 1,90 % | kein Einzelwert; "transcription-ready"-Gruppe (inkl. Deutsch) durchschnittlich FLEURS 10,4 % | **TTFS 236 ms / 253 ms (Pipecat)**, AA-Final-Latenz 0,07-0,42 s je Chunk (80-1120 ms) | Ja (Streaming-Modell) | 0 pro Minute (Open Weights, OpenMDW-1.1), Hosting selbst; NIM-Container braucht GPU (>= 16 GB VRAM) | – | 40 Locales, 32 aktiv, Deutsch in den 19 "transcription-ready"; kein Fertig-API-Dienst |
| **NVIDIA Parakeet TDT 0.6B v3** | 4,5 % (Rang 39, via Together AI) | **FLEURS 5,04 %, CoVoST 4,84 %** (offizielle HF-Modellkarte; Greedy, ohne LM) | **SF 617,7 (Together AI)**; RTFx 3 333 (HF) | Nein (Batch, ggf. Chunking) | Together AI 1,50/1000 min = **0,09**; OpenRouter 0,0015 USD/min | Selbst hosten gratis (CC-BY-4.0) | 25 europaeische Sprachen, Auto-Erkennung; Canary Qwen 2.5B (AA 4,3 %) ist sehr langsam (SF 8,7) |
| **Cartesia Ink-2** (`ink-2`) | nicht im AA-Board; Pipecat (en): 1,47 %, TTFS 299 / 328 ms | **nicht moeglich: Ink-2 nur Englisch**; Deutsch nur ueber ink-whisper (Batch; FLEURS 12,7 %, AssemblyAI) | TTFS 299 ms (Pipecat) | Ja (WebSocket) | 3 Credits/s -> ca. 0,39 USD/h (Scale-Plan, Anbieter-Listing) bzw. 0,43 (Soniox-Rechnung); ink-whisper 1 Credit/s | Free-Plan 20 000 Credits/Monat (Drittquellen) = ca. 111 min Ink-2 | Stille wird mitberechnet; fuer Deutsch ungeeignet |
| **Rev AI (Reverb)** | 5,9 % (Rang 57) | nicht gefunden (Sonix: "auf Englisch optimiert") | SF 13,0 (langsam) | Ja (Streaming vorhanden) | Reverb 0,20 / Turbo 0,10 / Foreign-Language 0,30 (57 Sprachen), Whisper 0,005/min (Drittquellen); AA 3,33/1000 = 0,20 | 5 h Reverb-Credits laut usagepricing.com (nicht bei Rev selbst geprueft) | fuer Deutsch ueber "Foreign Language" |
| **Amazon Transcribe** | 4,1 % (Rang 32, via Bedrock) | AssemblyAI FLEURS Realtime 8,0 % | SF 35,6; Streaming TTFS 1136 ms / 1527 ms (Pipecat) | Ja | Batch 0,006/min = **0,36**; Streaming 0,01/min = **0,60** (AWS-Seite) | **60 min/Monat fuer 12 Monate** (Batch oder Streaming) | Abrechnung sekundengenau ohne Minimum; Deutsch auf der Seite nicht ausdruecklich genannt |

Hinweise zur Tabelle:
- **StepAudio 3 ASR 1,7 % vs. 2,5 %:** Presse (alphasignal/247wallst) meldet "1,7 %, Platz 1"; die am 22.10.2026 abgerufene AA-Seite zeigt Fun-Realtime-ASR-preview mit 1,7 % auf Platz 1 und StepAudio 3 ASR mit 2,5 %. Wahrscheinlich anderer Messstand/Methodikversion. Wert 2,5 % ist der aktuell abgerufene.
- **Inkling (Thinking Machines)**, Open Weights, AA-WER 3,5 %, laut AA-FAQ; nicht in der Tabelle, keine weiteren Daten gefunden.
- **Weitere AA-Eintraege, nicht von den ausgeschlossenen Anbietern, ausserhalb Top 15:** Inworld STT 1 (3,9 %, SF 143,5, 2,50 USD/1000 min = 0,15 USD/h), Cohere transcribe-03-2026 (4,6 %, Preis auf AA "0,00" = nicht belastbar), Modulate STT Batch English VFast (4,2 %, 0,42/1000 min), Whisper Large v3 fal.ai (4,1 %, 1,15/1000 min = 0,069 USD/h, SF 97,3), Wizper Large v3 fal.ai (4,7 %, 0,50/1000 min = 0,03 USD/h, SF 144,1), Whisper v3 Together AI (4,5 %, SF 285,4, 0,09 USD/h), Gradium (6,8 %, SF 2,3 – zu langsam). Zu diesen wurde keine Detailrecherche gemacht.
- **Preisabweichungen AA vs. Anbieterseite:** Gladia (AA 0,61 USD/h vs. Anbieter 0,16 EUR/h Async), Smallest Pulse (AA 0,30 vs. Anbieter ca. 0,54 USD/h). Vorrang hat die Anbieterseite.
- **Deepgram, AssemblyAI, ElevenLabs, Mistral, Google, Microsoft, Alibaba** sind laut Auftrag ausgeschlossen und hier nicht bewertet.

## 2. Deutsch-WER – was wirklich belegt ist (alles Herstellerangaben, nicht vergleichbar)

| Messung | Werte | Quelle / Datum |
|---|---|---|
| AssemblyAI "Realtime multilingual", FLEURS Deutsch (Laeufe 25.08.-23.09.2026) | Gemini 3.5 Live 4,7; AssemblyAI U3.6 Pro RT 5,0; Reson8 5,3; ElevenLabs Scribe v2 5,8; Meta Muse 6,4; Zoom Scribe Live 6,5; Speechmatics Enhanced 6,8; Soniox Realtime 6,9; AWS 8,0; Speechmatics Standard 8,2; Azure 9,1; Gladia Solaria 10,9; OpenAI GPT Realtime 11,2; Cartesia Ink-Whisper 12,7; OpenAI GPT-4o Transcribe 13,1; GPT-4o mini 14,9 | https://www.assemblyai.com/benchmarks (Herstellerseite, abgerufen 22.10.2026) |
| AssemblyAI Common Voice Deutsch (gleiche Laeufe) | xAI Grok Streaming 11,7; Smallest Pulse 11,3; Deepgram Flux 9,9; Nova-3 12,8; Mistral Voxtral Mini 8,8 | gleiche Seite |
| AssemblyAI Pre-recorded Deutsch | AssemblyAI U3.5 Pro 9,07; Speechmatics Enhanced 10,30 | gleiche Seite |
| NVIDIA Parakeet TDT 0.6B v3 | FLEURS 5,04; CoVoST 4,84 | https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3 (Modellkarte) |
| Famulor Voice-Agent-Benchmark, 13.10.2026 | Scribe v2 Realtime 0,62 %; Gemini 3.5 Transcribe Live 1,24 %; AssemblyAI U3 1,86 %; Soniox v5 3,73 % – aber nur 161 Referenzwoerter, Sprecher nur maennlich | https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026 |
| ElevenLabs-Marketing (nicht neutral) | FLEURS Deutsch: Scribe v1 2,7; Gemini Flash 2 3,9; Whisper Large v3 4,5; Deepgram Nova 2 9,1 | https://elevenlabs.io/speech-to-text/german |

Nicht bestaetigt: Soniox-Vergleichsseiten sollen laut einer Suchzusammenfassung "Soniox 1,25 % vs. OpenAI 3,24 % Deutsch (YouTube-Audio, 60 Sprachen, 2026)" nennen; die abgerufenen Seiten enthielten diese Zahlen NICHT. (3,24 % ist tatsaechlich der Pipecat-Englisch-Wert fuer gpt-4o-transcribe.) Nicht verwenden.

Fazit Deutsch: In den FLEURS-Deutschzahlen liegen Reson8 (5,3), Zoom (6,5), Speechmatics Enhanced (6,8), Soniox (6,9) deutlich vor OpenAI GPT-4o (13,1) und Gladia (10,9). Fuer gpt-transcribe und Grok 2.0 (neu, Sept./Juli 2026) fehlt ein sauberer Einzelwert. Parakeet v3 (5,04) ist als Open-Weights-Modell konkurrenzfaehig.

## 3. Antwortzeit-Schaetzung fuer 30 Woerter ≈ 12 s Audio

Methode: Batch-Dauer ≈ 12 s / Speed Factor + Netz-/Anfrage-Overhead (Upload ca. 150-400 KB, TLS, Queue, Antwort) von ca. 0,4-1,0 s. Der AA-Speed-Factor ist an laengeren Dateien gemessen; bei 12-s-Clips dominiert der Overhead, die Schaetzung ist deshalb eine Spanne und KEINE Messung. Bei Streaming gilt: Der Text ist fast fertig, wenn man aufhoert zu sprechen; die Wartezeit nach Sprechende ist der Endpunkt-/Finalisierungs-Zeitwert (Pipecat TTFS bzw. AA "time to final"), plus Netz ca. 0,05-0,15 s.

Uebersicht (Wartezeit ab Sprechende bis fertiger Text):

| Modell / Modus | Rechnung | Geschaetzte Wartezeit |
|---|---|---|
| Groq whisper-large-v3-turbo (Batch) | 12/200 = 0,06 s + Overhead | **ca. 0,4-0,9 s** |
| Groq whisper-large-v3 (Batch) | 12/189 = 0,06 s + Overhead | ca. 0,5-1,0 s |
| Parakeet TDT v3 via Together (Batch) | 12/618 = 0,02 s + Overhead | ca. 0,4-0,8 s |
| Grok Voice Transcribe 2.0 Batch | 12/195 = 0,06 s + Overhead | ca. 0,5-1,0 s |
| Grok Voice Transcribe 2.0 Streaming | AA time-to-final 0,373 s + Netz | **ca. 0,4-0,6 s** nach Sprechende |
| Soniox v5 Streaming | TTFS 260 ms (P95 305) + Netz | **ca. 0,3-0,45 s** |
| Soniox v5 Async | 12/42,6 = 0,28 s + Overhead (Upload, Job) | ca. 1,0-2,0 s |
| Nemotron 3.5 (selbst gehostet, Streaming) | TTFS 236 ms (P95 253) | ca. 0,25-0,4 s (eigene GPU) |
| Smallest Pulse Streaming | TTFS 398 ms (P95 533) bzw. AA 0,132 s | ca. 0,2-0,6 s |
| Reson8 Resonant-1 (Batch) | 12/332 = 0,04 s + Overhead | ca. 0,5-1,0 s |
| Zoom Scribe Fast | 12/124 = 0,10 s + Overhead | ca. 0,6-1,2 s |
| OpenAI gpt-transcribe (Datei) | 12/55,8 = 0,22 s + Overhead | ca. 0,8-1,6 s |
| OpenAI gpt-4o-transcribe | 12/37,8 = 0,32 s + Overhead | ca. 1,0-1,8 s |
| OpenAI gpt-4o-mini-transcribe | 12/51 = 0,24 s + Overhead | ca. 0,8-1,5 s |
| OpenAI gpt-live-transcribe / gpt-realtime-whisper | TTFS 740 ms (P95 878 ms, gpt-realtime-whisper); live-Modell einstellbar | ca. 0,7-1,0 s (live-Modell nicht gemessen) |
| StepAudio 3 ASR | 12/64 = 0,19 s + Overhead | ca. 0,7-1,4 s (Anbieter in China: Netz evtl. hoeher) |
| Gladia Solaria-3 Async | 12/90 = 0,13 s + Upload + Polling | ca. 1,5-4 s |
| Speechmatics Enhanced Batch | 12/66 = 0,18 s + Job + Polling | ca. 2-5 s |
| Speechmatics Streaming | TTFS 495 ms (P95 676 ms) | ca. 0,5-0,8 s |
| Cartesia Ink-2 | TTFS 299 ms (Englisch) | n/a fuer Deutsch |
| Amazon Transcribe Streaming | TTFS 1136 ms (P95 1527 ms) | ca. 1,1-1,6 s |
| Amazon Transcribe Batch | 12/35,6 = 0,34 s + Job + Polling | ca. 2-6 s |
| Rev AI (Reverb) | 12/13 = 0,92 s + Overhead | ca. 1,5-3 s |

Kosten pro Diktat (12 s = 0,00333 h): Groq turbo 0,00013 USD; Grok Batch 0,00033; Grok Streaming 0,00067; Soniox Async 0,00033 / Streaming 0,0004; gpt-transcribe 0,0009; gpt-live-transcribe 0,0034; Speechmatics Melia Batch 0,0008; Amazon Batch 0,0012; Rev Reverb 0,00067. Kosten sind bei allen Kandidaten vernachlaessigbar (alle unter 0,4 Cent).

## 4. Anbieter-Details (belegt)

**OpenAI (Stand Juli-Oktober 2026)**
- Neue Modelle seit 28./29.07.2026: `gpt-transcribe` (Datei, gestreamte Datei, committed Turns in Realtime-Sitzungen; 0,0045 USD/min) und `gpt-live-transcribe` (echtes Live-Streaming, 0,017 USD/min). Rate Limits gpt-transcribe: Build 5 000 RPM / 2 Mio TPM, Launch 10 000 / 10 Mio, Grow 30 000 / 150 Mio. Quellen: https://developers.openai.com/api/docs/models/gpt-transcribe, https://developers.openai.com/api/docs/models/gpt-live-transcribe, https://gigazine.net/gsc_news/en/20260729-gpt-live-transcribe, https://www.cometapi.com/gpt-transcribe-vs-gpt-live-transcribe/
- Bestehende Modelle `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-realtime-whisper` (1,02 USD/h) bleiben nutzbar.
- OpenAI veroeffentlicht keine Sprachliste und keine Deutsch-WER fuer die neuen Modelle auf den Modellseiten. Vom Hersteller: gpt-live-transcribe Common Voice (22 Sprachen) 19,70 % vs. 20,33 % bei gpt-realtime-whisper-1; Real-World (9 Sprachen) 9,60 % vs. 11,65 %.
- Freikontingent: keines belegt.

**Groq** – Doku https://console.groq.com/docs/speech-to-text und https://console.groq.com/docs/rate-limits: turbo 0,04 USD/h (SF 216, WER 12 %), v3 0,111 USD/h (SF 189, WER 10,3 %). Datei max. 25 MB (Free) / 100 MB (Dev), 10 s Mindestabrechnung, Formate flac/mp3/mp4/mpeg/mpga/m4a/ogg/wav/webm. Limits (Developer-Basis): 20 RPM, 2 000 RPD, 7 200 Audio-Sek./h, 28 800 Audio-Sek./Tag; fuer den Free-Plan nannten nur Drittquellen dieselben Zahlen (z. B. https://www.morphllm.com/groq-pricing). 7 200 Sek./h entsprechen 2 h Audio je Stunde, bei 12-s-Diktaten (mit 10-s-Minimum faktisch 12 s) rund 600 Diktate pro Stunde, aber RPM 20 begrenzt zuerst. Kein neues Groq-STT-Modell neben diesen zwei gefunden.

**Soniox** – https://soniox.com/pricing: v5 `stt-rt-v5` Async 1,50 USD/1 Mio Audio-Tokens (ca. 0,10 USD/h), Realtime 2,00 USD/1 Mio (ca. 0,12 USD/h), Preisstand Juni 2026. Free Credits: Blog https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api (27.10.2025) beendet Free Credits fuer Neuanmeldungen; Preisseite nennt weiter 200 USD (ohne Datum) -> vor Nutzung in der Konsole pruefen. Pipecat-Benchmark (August 2026, Englisch, https://soniox.com/benchmarks): Soniox v5 1,34 % semantische WER, TTFS 260 / 305 ms.

**Gladia** – https://www.gladia.io/pricing: Self-serve Async Solaria-3 0,16 EUR/h (Mengenrabatt bis 0,12), Solaria-1 0,27 EUR/h, Realtime 0,68 EUR/h; 50 EUR Startguthaben (1 Jahr); 25 gleichzeitige Async-Anfragen, 30 Realtime-Sitzungen. Solaria-3 Launch 10.06.2026 (https://www.actuia.com/kr/news/solaria-3-gladia/); eigene Messung 6,4 % auf Earnings22; MLS-Mehrsprachigkeit 36 % schlechter als Solaria-1 (https://www.actuia.com/nl/news/solaria-3-gladia-aan-kop-in-productie-audio-volgens-eigen-metingen/).

**Speechmatics** – https://www.speechmatics.com/pricing (22.10.2026, Seite im letzten Teil nicht vollstaendig gelesen): Preise Melia/Enhanced/Standard/Linden/Oak wie in Tabelle, 100 USD Startguthaben ohne Kreditkarte, danach API pausiert.

**xAI / Grok** – https://docs.x.ai/developers/model-capabilities/audio/speech-to-text (Deutsch `de`, 38+ Sprachen, Streaming ueber WebSocket, Endpointing Standard 400 ms, Zwischenergebnisse ca. alle 500 ms, 100-ms-Chunks empfohlen); Preise https://docs.x.ai/developers/models (Batch 0,10, Streaming 0,20 USD/h); Release/Benchmarks https://aiweekly.co/alerts/spacexai-ships-grok-voice-transcribe-20-with-2x-accuracy-at-same-price-tops (marktechpost, 19.09.2026): Kurzphrasen-WER (19 Sprachen) 20,6 % -> 6,8 %, englische Telefonate 3,9 % -> 2,7 %; Limits laut DataNorth: 10 Req/s REST, 10 Req/s Streaming, 100 gleichzeitige Streams pro Team (https://datanorth.ai/news/xai-adds-grok-voice-transcribe-2-0). Standardmodell bei fehlender Angabe: `grok-voice-transcribe-2.0`.

**NVIDIA** – Parakeet TDT 0.6B v3 (CC-BY-4.0; 25 EU-Sprachen) Modellkarte https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3; Nemotron 3.5 ASR Review https://wavect.io/de/blog/nvidia-nemotron-3-5-asr-production-review.md (40 Locales / 32 aktiv, 80 ms = Chunk-Groesse, nicht Gesamtlatenz, ca. 240 Streams auf einer H100, Break-even gegenueber Deepgram Nova-3 ca. 9-18 dauerhaft ausgelastete Streams bei 3 USD/GPU-h). Gehosteter API-Preis: Together AI 0,09 USD/h (AA), OpenRouter 0,0015 USD/min. Hosted NIM/Baseten-Preise und build.nvidia.com-Freikontingent: nicht gefunden.

**Cartesia** – https://docs.cartesia.ai/pricing.md: Ink-2 3 Credits/s, ink-whisper 1 Credit/s, Stille wird berechnet, Free-Plan nach Drittquellen 20 000 Credits/Monat. Ink-2 ist Englisch (Soniox-Vergleich https://soniox.com/compare-stt/soniox-vs-cartesia/german; Cartesia-Doku listet nur `en`).

**Rev AI / Amazon / Zoom / Smallest / Reson8** – Quellen siehe Tabelle: https://aws.amazon.com/transcribe/pricing/, https://www.zoom.com/en/products/ai-services/scribe-api/, https://developers.zoom.us/docs/ai-services/scribe, https://smallest.ai/pricing, https://artificialanalysis.ai/speech-to-text/models/reson8, https://usagepricing.com/blueprint/rev-ai (Rev nur ueber Drittquellen), https://telnyx.com/release-notes/reson8-turn-based-stt-voice-ai.

## 5. Einordnung fuer 12-s-Diktat auf Deutsch

- **Schnellster Weg (Gesamtzeit bis Text):** Soniox v5 Streaming (ca. 0,3-0,45 s nach Sprechende) und Grok Streaming (ca. 0,4-0,6 s), danach Groq turbo Batch (ca. 0,4-0,9 s). Alle drei haben Deutsch.
- **Billigster Weg:** Groq turbo 0,04 USD/h (plus Gratis-Tier), Grok Batch 0,10, Soniox Async 0,10; Kostenunterschiede bei 12-s-Diktaten unerheblich.
- **Beste belegte Deutsch-Genauigkeit unter den nicht ausgeschlossenen:** Reson8 (5,3), Parakeet v3 (5,04, aber Batch/selbst gehostet), Zoom (6,5), Speechmatics Enhanced (6,8), Soniox (6,9). Groq Whisper v3 gilt ueber ElevenLabs-Marketing (4,5 FLEURS) als solide, ist aber kein neutraler Wert.
- **Ungeeignet fuer Deutsch:** Cartesia Ink-2 (nur EN), StepAudio 3 (Deutsch nicht belegt), Rev (EN-optimiert), Zoom Pro-Variante (EN).
- **Empfehlung:** Eigener Deutsch-Test (Messplatz) mit Groq turbo/v3, Grok 2.0, Soniox v5, gpt-transcribe, Speechmatics Enhanced, Reson8; AA-Rang nicht fuer Deutsch verwenden.

## 6. Datum der Quellen

- Artificial Analysis Rangliste: abgerufen 22.10.2026, Seite selbst undatiert (Speed Factor = Median der letzten 7 Tage).
- AssemblyAI-Benchmarks: Messlaeufe 25.08.-23.09.2026; Latenzwerte "gelesen 09.09.2026" (AA) und "28.09.2026" (Pipecat).
- Pipecat STT-Benchmark (Soniox-Seite): Stand August 2026.
- OpenAI gpt-transcribe / gpt-live-transcribe: Launch 28./29.07.2026 (OpenRouter nennt 05.08.2026).
- Grok Voice Transcribe 2.0: 19.09.2026 (marktechpost).
- StepAudio 3 ASR: 22.09.2026. Gladia Solaria-3: 10.06.2026. Soniox v5-Preisstand: Juni 2026.
- Famulor: 13.10.2026. Soniox Free-Credits-Blog: 27.10.2025.

## BEST-PRACTICES-KANDIDATEN:

- AA-WER misst nur Englisch (AA-AgentTalk/VoxPopuli/Earnings22); fuer Deutsch immer FLEURS/Common Voice-Tabellen bzw. eigenen Test nutzen, Hersteller-Tabellen sind nicht untereinander vergleichbar. https://artificialanalysis.ai/speech-to-text/non-streaming (Stand 22.10.2026, AA-WER v2)
- Streaming-Latenz nicht aus Speed Factor ableiten: TTFS/"time to final" separat messen (Pipecat-Benchmark). https://soniox.com/benchmarks (Pipecat STT-Benchmark, August 2026)
- Groq Whisper: `language=de` setzen (bessere Genauigkeit/Latenz), 10 s Mindestabrechnung, 25 MB Free-Dateilimit, 7 200 Audio-Sek./h. https://console.groq.com/docs/speech-to-text (whisper-large-v3 / -turbo, Okt 2026)
- OpenAI gpt-live-transcribe: Delay-Stufe (minimal bis xhigh) selbst messen; Events ueber `item_id` zuordnen. https://www.cometapi.com/gpt-transcribe-vs-gpt-live-transcribe/ (gpt-live-transcribe, Juli 2026)
- xAI Grok STT: Sprache `de` zusaetzlich zu `format=true` setzen, 16-kHz-PCM/100-ms-Chunks, `smart_turn`/Endpointing 400 ms beachten. https://docs.x.ai/developers/model-capabilities/audio/speech-to-text (grok-voice-transcribe-2.0)
- Gladia: fuer Mehrsprachigkeit Solaria-1 statt Solaria-3 verwenden. https://www.actuia.com/nl/news/solaria-3-gladia-aan-kop-in-productie-audio-volgens-eigen-metingen/ (Solaria-3, Juni 2026)
- Soniox-Free-Credits vor Einplanung pruefen (Blog und Preisseite widersprechen sich). https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api (27.10.2025)

## BUG-KANDIDATEN:

- OpenAI Realtime-Transkription: Completed-Events verschiedener Turns kommen nicht garantiert in Reihenfolge; altes Feld `language` darf nicht zusammen mit `languages` gesendet werden. https://www.cometapi.com/gpt-transcribe-vs-gpt-live-transcribe/ (gpt-live-transcribe / gpt-transcribe, Juli 2026)
- Gladia Solaria-3: Regression bei Multilingual (MLS 36 % schlechter als Solaria-1; Deutsch nur ca. +3 % relativ). https://www.actuia.com/nl/news/solaria-3-gladia-aan-kop-in-productie-audio-volgens-eigen-metingen/ (Solaria-3, 10.06.2026)
- Cartesia Ink-2 unterstuetzt nur Englisch; Deutsch faellt auf ink-whisper (Batch) zurueck; Stille wird mit abgerechnet. https://docs.cartesia.ai/pricing.md (ink-2, Okt 2026)
- Artificial-Analysis-Preisangaben weichen von Anbieterseiten ab (Gladia 0,61 USD/h vs. 0,16 EUR/h; Smallest Pulse). https://artificialanalysis.ai/speech-to-text/non-streaming (Stand 22.10.2026)
