# R5 – ElevenLabs Scribe vs. Deepgram vs. AssemblyAI (Stand 22.10.2026)

Recherche-Datum: 22.10.2026. Alle Zahlen mit Quelle. "Herstellerangabe" = vom Anbieter selbst gemessen, nicht unabhängig.

## 1. Modellstand (was existiert tatsächlich)

- ElevenLabs: Scribe v1 (alt), Scribe v2 (Batch, Start laut Sekundärquelle 09.01.2026), Scribe v2 Realtime (Blog 13.11.2025), Scribe v2 Medical. Kein neueres Scribe (v3) gefunden. Quelle: https://elevenlabs.io/docs/overview/capabilities/speech-to-text
- Deepgram: Nova-3 (aktuelles Hauptmodell; Changelog bis 20.10.2026 erwähnt nichts Neueres, **Nova-3.5 existiert laut Changelog nicht**), Flux (English `flux-general-en`, Multilingual `flux-general-multi`, GA 29.04.2026, 10 Sprachen inkl. Deutsch). Quellen: https://developers.deepgram.com/changelog/speech-to-text-changelog , https://deepgram.com/learn/deepgram-launches-flux-multilingual-press-release
- AssemblyAI: Universal-3.5 Pro (Async, Release 07.07.2026, 18 Sprachen inkl. Deutsch), Universal-2 (99 Sprachen), Universal-3.5 Pro Streaming/Realtime und **Universal-3.6 Pro Realtime** (Preisliste, API-Wert `universal-3-6-pro`), Universal-Streaming Multilingual (6 Sprachen, de dabei) und English. Benennung uneinheitlich: Preisseite nennt "3.6 Pro Realtime", Modellseite "3.5 Pro Streaming". Quellen: https://www.assemblyai.com/pricing.md , https://www.assemblyai.com/docs/getting-started/models.md , https://www.assemblyai.com/blog/universal-3-5-pro-async

## 2. Übersichtstabelle

| Modell | Modus | AA-WER (Artificial Analysis, Englisch-lastig, Batch) | Speed Factor AA (Batch) | Streaming-Latenz | Preis USD/h | Deutsch-WER FLEURS de (Herstellerbenchmark AssemblyAI, Läufe 25.08.–23.09.2026) |
|---|---|---|---|---|---|---|
| ElevenLabs Scribe v2 | Batch | 2,2 % | 69,0 | – | 0,22 (+0,05 Keyterms) | 5,8 % (Tabelle 10; Zeile heißt "Scribe v2", unklar ob Batch oder Realtime); Common Voice de 7,1 % |
| ElevenLabs Scribe v2 Realtime | Streaming | nicht gelistet | – | 0,141 s bis Final-Transkript nach Sprachende (AA-Streaming, gelesen 09.09.2026); 281 ms Median / 348 ms P95 (Pipecat); Hersteller "~150 ms", Blog 30–80 ms | 0,39 (+0,08 Keyterms) | siehe oben (Code-Switching EN/DE 12,29 %) |
| Deepgram Nova-3 | Batch/Streaming | 5,2 % | 546,2 | 0,066 s (AA-Streaming); 247 ms Median / 298 ms P95 (Pipecat) | Batch 0,26 mono / 0,31 multi; Streaming 0,29 mono / 0,35 multi (PAYG, Aktionspreis) | 13,3 %; Common Voice de 12,8 % |
| Deepgram Flux (EN/Multi) | nur Streaming | nicht gelistet | – | 0,021 s (AA-Streaming); End-of-Turn < 400 ms (Hersteller) | 0,39 EN / 0,47 Multi (PAYG, Aktionspreis), Keyterms +0,08 | 10,2 %; Common Voice de 9,9 % (Variante EN/Multi unklar) |
| AssemblyAI Universal-3.5 Pro | Batch | 3,0 % (U3 Pro: 3,1 %) | 102,4 (U3 Pro: 103,2) | – | 0,21 (+0,05 Keyterms, +0,05 Prompting) | 5,3 %; Common Voice 5,0 %; "WER by Language" 9,07 % (anderes Set, undatiert) |
| AssemblyAI Universal-3.5/3.6 Pro Realtime | Streaming | nicht gelistet | – | 0,19–0,20 s (AA, U3.5); 282 ms (U3.5) bzw. 307 ms (U3.6) Median (Pipecat); Hersteller "sub-300 ms" | 0,45 (Keyterms inklusive, 100 Stück) | U3.6: 5,0 %, Common Voice 3,6 %; U3.5: 5,3 % / 5,0 % |
| AssemblyAI Universal-Streaming Multilingual | Streaming | – | – | ~300 ms (Hersteller, English) | 0,15 (Keyterms inklusive) | nicht gefunden |
| AssemblyAI Universal (Universal-2) | Batch | 3,8 % | 105,4 | – | 0,15 (AA: 2,50 $/1000 min) | nicht gefunden |

Hinweis AA-WER: Index v2, gewichteter Mix aus drei Datensätzen, überwiegend Englisch; sagt über Deutsch wenig. Quelle: https://artificialanalysis.ai/speech-to-text (Seite undatiert, Speed Factor = Median der letzten 7 Tage, abgerufen 22.10.2026). Preise dort in USD/1000 min; Stundenpreis = x 0,06.

## 3. Deutsch (WER)

- Unabhängiger Deutsch-Test mit allen drei Anbietern: **keiner gefunden**. Alle Deutsch-Zahlen oben stammen aus dem AssemblyAI-Benchmark (Herstellerquelle mit Konkurrenzvergleich) https://www.assemblyai.com/benchmarks (Tabellen 10–13, Läufe 25.08.–23.09.2026). Die Reihenfolge AssemblyAI/Scribe vor Deepgram ist plausibel, die absoluten Werte nicht neutral.
- ElevenLabs selbst: Deutsch in der Stufe "Excellent (≤ 5 % WER)", ohne Einzelwert. https://elevenlabs.io/docs/overview/capabilities/speech-to-text . Deutsche Landingpage nennt 3,1 % FLEURS (Tabelle dort: Scribe v1 2,7 %, Deepgram Nova 2 9,1 %). https://elevenlabs.io/speech-to-text/german (Modell Scribe v1, undatiert).
- Voxtral-Paper (unabhängig, Juli 2025, vermutlich Scribe v1): FLEURS de Scribe 4,78 %. https://arxiv.org/pdf/2507.13264
- Code-Switching EN/DE (AssemblyAI-Benchmark Tabelle 14): U3.6 Pro 6,13 %, U3.5 Pro 7,84 %, Scribe v2 Realtime 12,29 %, Nova-3 Multi 18,01 %, Flux EN 52,33 %. Async-Blog 07.07.2026, deutscher Anteil: U3.5 Pro 6,61, Scribe v2 7,04, Nova-3 Multi 11,99. https://www.assemblyai.com/blog/universal-3-5-pro-async
- Pipecat-Agent-Benchmark (Hersteller, 24.08.2026, Englisch): U3.5 Pro Realtime 1,22 %, Flux 1,62 %, Scribe v2 3,12 %. https://www.assemblyai.com/blog/universal-3-5-pro-realtime-vapi
- arXiv-Studie Code-Switching Arabisch/Persisch/Deutsch (2605.19069): laut Suchzusammenfassung führt Scribe v2; PDF nicht lesbar, Zahlen unbestätigt.

## 4. Geschwindigkeit und geschätzte Antwortzeit für 12 s Audio (~30 Wörter)

Batch: 12 s / Speed Factor (AA-Messung, ohne eigenes Netz) + Netz/Upload (ca. 0,2–0,6 s, geschätzt).
- Scribe v2: 12/69 = 0,17 s + Netz, also **ca. 0,4–1 s** (Schätzung)
- Nova-3: 12/546 = 0,02 s + Netz, also **ca. 0,3–0,7 s** (schnellster Batch)
- Universal-3.5 Pro: 12/102 = 0,12 s + Netz, rechnerisch **ca. 0,4–1 s**; in der Praxis bei AssemblyAI Upload + Job + Abfrage (Polling) oft mehrere Sekunden, nicht gemessen

Streaming (Audio wird live gesendet): Wartezeit nach Sprachende = Latenz bis Final + Netz (ca. 50–150 ms, geschätzt). Wird die fertige Aufnahme schneller als Echtzeit gesendet, entfällt die Wartezeit auf die 12 s.
- Scribe v2 Realtime: 0,14–0,28 s + Netz, also **ca. 0,25–0,45 s** nach Sprachende
- Nova-3: 0,07–0,25 s + Netz, also **ca. 0,15–0,4 s**
- Flux: 0,02 s Final (AA) bis < 0,4 s Turn-Ende + Netz, also **ca. 0,15–0,5 s**
- AssemblyAI U3.5/3.6 Pro Realtime: 0,19–0,31 s + Netz, also **ca. 0,3–0,45 s**

Ergebnis: Alle Wege liegen unter ca. 1 s nach Sprachende; die Unterschiede (±0,2 s) sind kleiner als Netzschwankungen. Die Genauigkeit auf Deutsch entscheidet.

Quellen Latenz: AA-Streaming-Werte (gelesen 09.09.2026) und Pipecat-Tabelle 19 (gelesen 28.09.2026) wie zitiert in https://www.assemblyai.com/benchmarks (Herstellerquelle). Coval-Test (Mai/Sep. 2026) mit TTFT (Zeit bis erstes Token, nicht Final): Scribe v2 2.080 ms, Nova-3 992 ms, AssemblyAI Streaming 1.061 ms, Sprache unklar. https://gradium.ai/content/stt-api-benchmark-2026-latency-accuracy (Quelle ist ein Konkurrent, daher nur Randnotiz).

## 5. Preise und Freikontingent

- ElevenLabs API: Scribe v2 0,22 $/h, Realtime 0,39 $/h, Keyterms +0,05 bzw. +0,08 $/h, Preis über alle Pläne gleich; in Plänen enthalten: Starter 4,5 h / 2,5 h (v2 / Realtime), Creator 27 h / 15 h, Pro 100 h / 56 h. https://elevenlabs.io/pricing/api
- ElevenLabs Free-Plan: 10.000 Credits/Monat, Speech-to-Text enthalten; ca. 330 Credits/Minute, also **ca. 30 Min/Monat** (gemeinsamer Pool, keine eigene Scribe-Quote; Rechnung nach Seite). https://elevenlabs.io/pricing . Free: keine kommerzielle Lizenz.
- Deepgram: **200 $ Startguthaben** bei Pay-as-you-go bestätigt. Streaming/h PAYG (Aktionspreise): Flux EN 0,39, Flux Multi 0,47, Nova-3 mono 0,29, Nova-3 multi 0,35; Batch/h: Nova-3 mono 0,26, multi 0,31, Whisper 0,29. Keyterm-Aufpreis 0,08 $/h. https://deepgram.com/pricing
- AssemblyAI: **50 $ Startguthaben ohne Kreditkarte**; Free-Tier 5 neue Streams/min. Async U3.5 Pro 0,21 $/h, Universal-2 0,15, Streaming Multilingual/English 0,15, U3.6 Pro Realtime 0,45. Streaming wird nach Sitzungsdauer inkl. Leerlauf abgerechnet. https://www.assemblyai.com/pricing.md

## 6. Vokabular / Keyterms / Besonderheiten

- ElevenLabs: Keyterm-Prompting Batch bis 1.000 Begriffe à 50 Zeichen, Realtime bis 50 Begriffe à 20 Zeichen (Aufpreis oben); Entity Detection 65 Typen; Diarisierung, Audio-Events, Zeitstempel; 90+ Sprachen; Datei bis 3 GB. https://elevenlabs.io/docs/overview/capabilities/speech-to-text
- Deepgram: Keyterm-Prompting (Nova-3, Flux), seit 02./20.10.2026 Keyterm-Updates mitten im Stream (alle Nova-3-Streaming-Modelle); Überlimit-Update schließt Stream nach ca. 30 s mit 1011. Flux: Turn-Erkennung (`ForceEndTurn`), `language_hint`, neue Engine-Runtime 20.10.2026 (nur `.dgv2`-Modelle). Nova-3 Multilingual: Code-Switching über 10 Sprachen. https://developers.deepgram.com/changelog/speech-to-text-changelog
- AssemblyAI: Keyterms Async U3.5 Pro bis 1.000 (+0,05 $/h), Realtime bis 100 (inklusive, mid-stream änderbar); Prompting +0,05 $/h; Diarisierung Realtime +0,12 $/h; Code-Switching über 18 Sprachen. https://www.assemblyai.com/pricing.md

## 7. Einordnung (Diktat, ca. 12 s, Deutsch)

Beste Genauigkeit laut allen verfügbaren Zahlen: AssemblyAI Universal-3.5/3.6 Pro und ElevenLabs Scribe v2 auf Augenhöhe; Deepgram auf Deutsch laut Herstellerbenchmark deutlich schlechter (WER ca. doppelt). Nova-3/Flux gewinnen nur bei Latenz/Preis. Kostenlos testen: Deepgram 200 $, AssemblyAI 50 $ reichen für sehr viele Stunden; ElevenLabs Free nur ca. 30 Min.

Lücken: kein unabhängiger deutscher Vergleich der drei Anbieter; FLEURS-de-Wert für Scribe v2 Batch nicht separat; Flux-Multi-de nicht separat; AA-Streaming-Seite ohne lesbare Zahlen (nur über AssemblyAI zitiert).

BEST-PRACTICES-KANDIDATEN:
- Deepgram Keyterm-Updates mitten im Stream: Überlimit schließt Stream nach ca. 30 s mit Code 1011 ohne Error-Meldung; Limits vorab prüfen. https://developers.deepgram.com/changelog/speech-to-text-changelog (Eintrag 08.10.2026, Nova-3)
- Deepgram Flux Engine-Runtime 20.10.2026 lädt nur `.dgv2`-Modelldateien (Self-Hosted). Quelle wie oben.
- AssemblyAI Streaming wird nach Sitzungsdauer inkl. Leerlauf abgerechnet, Verbindung sofort schließen. https://www.assemblyai.com/pricing.md
- AssemblyAI Modellnamen: Legacy `best`/`nano` routen auf `universal-3-5-pro`/`universal-2`; Realtime-API-Wert `universal-3-6-pro`. https://www.assemblyai.com/pricing.md

BUG-KANDIDATEN:
- Deepgram Nova-3 Streaming: Keyterm-Update über Limit stoppt die Transkription und schließt den Stream mit 1011 nach ca. 30 s, ohne Error-Event (Nova-3, Changelog-Korrektur 08.10.2026). https://developers.deepgram.com/changelog/speech-to-text-changelog
- Deepgram: API und Engine müssen für Nova-3 Mid-Stream-Keyterms/Numerals gemeinsam aktualisiert werden (Eintrag 20.10.2026). Quelle wie oben.
