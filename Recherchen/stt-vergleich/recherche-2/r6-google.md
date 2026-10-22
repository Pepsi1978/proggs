# R6 – Google-Spracherkennung (Stand 22.10.2026)

## Kernbefund: „Gemini 3.8 Flash Lite Transcribe" existiert nicht
- Die offizielle Modellliste (https://ai.google.dev/gemini-api/docs/models, „Last updated 2026-10-21 UTC") führt KEIN „Gemini 3.8 Flash-Lite Transcribe" und kein anderes 3.8-Transkriptionsmodell. Einziges Transkriptionsmodell: **Gemini 3.5 Transcribe** (`gemini-3.5-transcribe` für Dateien, `gemini-3.5-transcribe-live` für Streaming).
- „Gemini 3.8 Flash-Lite" gibt es nur als **TTS** (`gemini-3.8-flash-lite-tts`, Sprachausgabe, Nachfolger von gemini-3.1-flash-tts-preview) – vermutlich die Quelle der Verwechslung. https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash-lite-tts
- Auch Googles Blog vom 15.09.2026 („Build real-time voice applications with Gemini 3.8 Live and 3.5 Transcribe") nennt kein Flash-Lite-Transkriptionsmodell. Ein Text-Modell „3.8 Flash-Lite" fehlt in der Preisliste (dort: gemini-3.5-flash-lite, gemini-3.1-flash-lite).
- Falls ein konkretes Modell gemeint ist, das der Nutzer irgendwo gesehen hat: nicht belegbar. Nächstliegende Kandidaten: `gemini-3.5-transcribe` (Datei), `gemini-3.5-transcribe-live` (Streaming), `gemini-3.5-flash-lite` (Allzweck-LLM mit Audio-Eingang, nicht auf AA gelistet).

## Vergleichstabelle

| Modell | Modell-ID | Status | AA-WER | WER Deutsch | Geschwindigkeit | Preis | Free Tier |
|---|---|---|---|---|---|---|---|
| Gemini 3.5 Transcribe (Datei) | `gemini-3.5-transcribe` | Blog 26.08.2026: „public preview"; Doku-Modellseite: „Stable" (widersprüchlich, kein „-preview" in der ID) | **2.6 %** (AA-WER nicht-streaming, Rang 5) | nicht veröffentlicht. FLEURS-Mittel über Top-Sprachen: 5.04 % | AA Median Speed Factor **89.5** (89,5 s Audio pro Sekunde Rechenzeit); „time to final" −70 % ggü. Chirp 3 | Preisseite: $2.00/1M Audio-Token (=$0.003/min bei 25 Token/s) + $12/1M Text-Out (≈$0.002/min) ⇒ ≈ **$0.005/min = $0.30/h** (AA: $5.00/1000 min) | „Free of charge" (Input+Output), Limits nicht veröffentlicht |
| Gemini 3.5 Transcribe Live | `gemini-3.5-transcribe-live` | wie oben | **4.0 %** (AA-WER Streaming) | nicht veröffentlicht. FLEURS: 5.50 % | Streaming (WebSocket/Live API), Finaltext ca. **0.4 s nach Sprachende** (AA); max. 10 min/Session | Input $3.50/1M (=$0.005/min), Output $21/1M (≈$0.004/min) ⇒ ≈ **$0.009/min = $0.54/h** | „Free of charge", Limits nicht veröffentlicht |
| Gemini 3.8 Flash (Audio) | `gemini-3.8-flash` | Stable | nicht von AA gelistet | nicht bekannt | keine Latenzangaben, keine Audio-Token/s angegeben | $0.75/1M Input bis 31.12.2026 (danach $1.50), Output $3.75 (danach $7.50). Preisseite trennt Audio nicht aus; bei 32 Token/s ≈ $0.086/h Input (reine Schätzung) + Textausgabe | „Free of charge", Limits nicht veröffentlicht |
| Gemini 3.8 Live (Realtime-Audio) | `gemini-3.8-live`, `gemini-3.8-live-extended-thinking` | Stable („New Stable"), veröffentlicht 15.09.2026 | nicht auf AA-STT gelistet (AA: #1 Speech-to-Speech für Extended Thinking) | 97+ Sprachen, kein WER | Live API, Sprache-zu-Sprache, kein reines Transkript-Modell | Audio-In $3.00/1M (=$0.005/min), Audio-Out $12/1M (=$0.018/min) | „Free of charge", Limits nicht veröffentlicht |
| Google Cloud Chirp 3 | `chirp_3` (Speech-to-Text v2) | GA | **4.3 %** (AA) | nicht veröffentlicht (FLEURS Streaming 7.32 %, Drittzitat) | AA Speed Factor **28.1** | **$0.016/min = $0.96/h** (AA: $16/1000 min; Staffel bis $0.004/min ab 2 Mio min/Monat) | GCP-eigenes Kontingent, nicht Gemini API (nicht neu geprüft) |
| Vergleich: Gemini 3.1 Flash-Lite Preview (Minimal) | `gemini-3.1-flash-lite` | – | 3.4 % | – | Speed Factor 90.0 | AA $5.83/1000 min | „Free of charge" |
| Vergleich: Gemini 2.5 Flash-Lite | `gemini-2.5-flash-lite` | – | 5.2 % | – | 75.7 | AA $6.56/1000 min | „Free of charge" |

AA = Artificial Analysis. AA-WER = Mix aus AA-AgentTalk, VoxPopuli-Cleaned-AA, Earnings22-Cleaned-AA; **keine Sprachaufschlüsselung, daher kein Deutsch-WER**. Die Messung wurde laut Drittquelle (winzheng) von Google beauftragt, AA selbst ist unabhängig.

## Freikontingent Gemini API Free Tier
- Die offizielle Seite https://ai.google.dev/gemini-api/docs/rate-limits („last updated 2026-10-15 UTC") nennt **keine** RPM/TPM/RPD-Werte für das Free Tier; Transcribe/Live fehlen in den Tabellen. Limits seien „in Google AI Studio" einsehbar (aistudio.google.com/rate-limit, Login nötig, nicht abrufbar). Google: angegebene Limits sind nicht garantiert.
- Preisseite: Free Tier = „Free of charge" für Input und Output bei gemini-3.5-transcribe, -live, gemini-3.8-live, gemini-3.8-flash. Im Free Tier werden Daten zur Produktverbesserung genutzt („Used to improve our products: Yes").
- Drittquellen (nicht belastbar): ~10 RPM / ~1.500 RPD für „3.x Flash allgemein" (Community, benchlm.ai, laut klymentiev.com); andere Blogs nennen 5 RPM / 20 RPD für 3.8 Flash; ein Blog nennt für Transcribe Live „unbegrenzt RPM, 20K Input-TPM". Widersprüchlich, nur indikativ.
- Fazit: genaue Werte nur im eigenen AI-Studio-Konto (nach Login) ablesbar. Offizielle Zahlen waren NICHT zu beschaffen.

## Vokabular / Kontext
- Custom Vocabulary: bis **1.000 Begriffe** (Google empfiehlt ~100), auf beiden Endpunkten; nicht kombinierbar mit Diarization/Timestamps. Ein freier Kontext-Prompt ist nicht dokumentiert.
- Smart Transcription (Füllwörter weg, Selbstkorrekturen, Auto-Formatierung), Code-Switching, 85+ Sprachen (`de-DE` unterstützt).
- Datei: bis 1 h (30 min mit Timestamps/Diarization); Diarization bis 8 Sprecher; Live: keine Timestamps/Diarization. Kein Batch/Flex/Priority, kein Caching.

## Rechnung: 30 Wörter ≈ 12 s Audio
- Token: 12 s × 25 = **300 Audio-Token** (Preisseite) bzw. 12 × 32 = 384 (Gemini-Allgemeinwert). Kosten Transcribe: ≈ $0.0001 Input + ≈ $0.00004 Output, also etwa **$0.0001–0.0002 pro Diktat** (im Free Tier $0).
- **gemini-3.5-transcribe (Datei)**: Speed Factor 89.5 ⇒ 12 / 89.5 ≈ 0.13 s reine Modellzeit. Dazu Upload (komprimiertes Audio ~30–60 KB, ~0.1–0.3 s), TLS/Verbindungsaufbau, API-Overhead ⇒ geschätzt **0.6–1.5 s** nach Sprachende. Der Speed Factor stammt von AA-Testdateien; bei kurzen Clips dominiert fester Overhead. Schätzung, nicht gemessen.
- **gemini-3.5-transcribe-live (Streaming)**: Audio wird schon beim Sprechen gestreamt; Final laut AA ≈ **0.4 s nach Sprachende** (+ Netz-Jitter) ⇒ geschätzt **0.4–0.8 s** (Voraussetzung: offene Sitzung, ≤ 10 min).
- **Chirp 3**: Speed Factor 28.1 ⇒ 12 / 28.1 ≈ 0.43 s + Netz ⇒ **0.9–1.8 s**.
- **gemini-3.8-flash (LLM mit Audio)**: kein Speed-Faktor veröffentlicht; ähnliche Gemini-Flash-Modelle liegen bei AA zwischen 21 und 90 ⇒ geschätzt 1–3 s; für reines Diktat nicht empfohlen.

## Quellen (Abruf 22.10.2026)
- https://ai.google.dev/gemini-api/docs/models (Seitenstand 2026-10-21)
- https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe (Update Aug 2026)
- https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash (2026-09-02)
- https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash-lite-tts
- https://ai.google.dev/gemini-api/docs/pricing
- https://ai.google.dev/gemini-api/docs/rate-limits (2026-10-15)
- https://artificialanalysis.ai/speech-to-text/non-streaming (Abruf 22.10.2026)
- https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/ (26.08.2026)
- https://blog.google/innovation-and-ai/technology/developers-tools/build-real-time-voice-applications-gemini-audio/ (15.09.2026)
- https://www.winzheng.com/en/article/google-gemini-3-5-transcribe-api-launch-analysis (27.08.2026, Drittquelle)
- https://9to5google.com/2026/08/26/gemini-3-5-transcribe/ (26.08.2026)
- https://www.memetik.ai/guides/gemini-api-free-tier-limits (01.10.2026, Drittquelle)
- https://klymentiev.com/blog/gemini-api-free-tier (04.10.2026, Drittquelle)
- https://cloud.google.com/speech-to-text/pricing (Chirp 3 $0.016/min laut Websuche, Seite selbst nicht vollständig abrufbar); https://openrouter.ai/google/chirp-3

## Offene Punkte / Widersprüche
- Status 3.5 Transcribe: Blog „public preview" vs. Modellliste „Stable".
- Ein Drittartikel zitiert den AI-Studio-Guide mit 3.6 % WER; alle anderen Quellen 2.6 % (nicht verifiziert, Guide-Seite war nicht abrufbar).
- Kein Deutsch-WER öffentlich; aussagekräftig ist nur ein Test mit eigenem Audio.
- Die AA-Streaming-Tabelle (Time to Final je Modell) war per Fetch nicht lesbar; der Wert 0.40 s stammt aus AA-Meldung via Drittquellen.

BEST-PRACTICES-KANDIDATEN:
- Custom Vocabulary bei gemini-3.5-transcribe: max. 1.000 Begriffe, Empfehlung ca. 100, nicht mit Diarization/Timestamps kombinierbar (https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe, Stand Aug 2026, gemini-3.5-transcribe)
- Live-Endpunkt gemini-3.5-transcribe-live: Sitzung auf 10 min begrenzt, danach Sitzung neu aufbauen (gleiche Quelle, gemini-3.5-transcribe-live)
- Kostenschätzung der Transcribe-Modelle basiert auf 25 Audio-Token/s (https://ai.google.dev/gemini-api/docs/pricing, 22.10.2026, gemini-3.5-transcribe)

BUG-KANDIDATEN:
- KEINE
