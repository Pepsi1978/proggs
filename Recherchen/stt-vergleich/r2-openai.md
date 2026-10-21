# R2 — OpenAI Speech-to-Text komplett (gpt-transcribe, gpt-live-transcribe, gpt-4o-*, whisper-1) und Azure OpenAI

Stand: 21.10.2026. Recherche für die STT-Vergleichsstudie (Basislinie: Groq whisper-large-v3-turbo, deutsches Diktat).
Kennzeichnung: **[offiziell]** = OpenAI-/Microsoft-Dokumentation, **[Vendor]** = Herstellerangabe aus Sekundärquelle, **[unabhängig]** = Drittmessung, **[Forum]** = Einzelbericht, **unbelegt** = keine Quelle gefunden.

## Kurzfassung

- Das neue Modellpaar vom 28./29.07.2026 (`gpt-transcribe` für Dateien, `gpt-live-transcribe` für Live) ersetzt laut OpenAI alle älteren Modelle. `whisper-1`, `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-4o-transcribe-diarize` und `/v1/audio/translations` werden am **26.02.2027** abgeschaltet (Ankündigung 26.08.2026). Franks „gpt-4o-mini-transcribe“ in Jarvis ist also ein Auslaufmodell.
- **Kein echtes Freikontingent** für die Transkriptions-Modelle gefunden. Die einzige Spur ist ein „Free"-Tier mit 3 Anfragen/Min und 200/Tag, und das nur auf der Modellseite von `whisper-1` (das selbst ausläuft). Das Daten-Teilen-Programm mit Gratis-Tokens nennt keine Audio-Modelle.
- **Deutsch-Qualität:** Für `gpt-transcribe` gibt es **keinen veröffentlichten deutschen WER-Wert** (weder OpenAI noch unabhängig). Indizien sprechen für „besser als Turbo", sind aber nicht deutsch-spezifisch (siehe Fazit). Preis: 0,27 USD/h gegen etwa 0,04 USD/h bei Groq Turbo.

---

## 1. gpt-transcribe (Datei-Transkription, neues Standardmodell)

- **Modell-ID:** `gpt-transcribe` (einziger Snapshot, kein datierter Snapshot zum Festpinnen). Azure: Version `2026-07-28`.
- **Status:** Veröffentlicht 28./29.07.2026. Von OpenAI als empfohlenes Modell für Dateien und „committed turns" in Realtime geführt. Kein Abschaltdatum. Modellseite nennt keinen Lifecycle-Status, Azure führt es als GA. Quellen: https://developers.openai.com/api/docs/models/gpt-transcribe, https://developers.openai.com/api/docs/guides/transcription.md, https://ai.azure.com/catalog/models/gpt-transcribe
- **Deutsch-Qualität:**
  - OpenAI-Launch-Benchmark **Common Voice, 22 Sprachen, Fehlerrate (aggregiert): gpt-transcribe 19,27 % gegen whisper-1 40,37 %** (rund 52 % weniger). **[Vendor]** Quelle: https://alphasignal.ai/news/openai-replaces-whisper-with-gpt-transcribe-slashing-errors-by-52, https://spokenly.app/blog/gpt-transcribe. Wichtig: Das ist ein Aggregat über 22 Sprachen, **kein deutscher Wert**; als Vergleich dient whisper-1, **nicht** Turbo. Den OpenAI-Ankündigungstext selbst konnte ich nicht lesen (openai.com liefert 403); die Forenfassung enthält nur Live-Zahlen: https://community.openai.com/t/gpt-live-transcribe-and-gpt-transcribe-two-new-transcription-models-in-the-api/1388318
  - **Real-World Audio, 9 Sprachen: 8,98 % gegen whisper-1 15,21 %.** **[Vendor]** (AlphaSignal, gleiche Quelle). Sprachen nicht benannt, ob Deutsch dabei ist: unbelegt.
  - **Context Aware ASR** (semantische Genauigkeit, neuer OpenAI-Benchmark): 41,6 % ohne, 45,2 % mit Freitext-Kontext (+3 bis 6 Punkte). **[Vendor]** (AlphaSignal; explainx: https://explainx.ai/blog/openai-gpt-live-transcribe-gpt-transcribe-july-2026)
  - **AA-WER v2 (Artificial Analysis, nicht-streaming): gpt-transcribe 3,3 %, gpt-4o-transcribe 4,0 %, gpt-4o-mini-transcribe 4,5 %, Whisper Large v3 Turbo (Groq) 4,6 %, Whisper Large v2 (OpenAI) 4,1 %, ElevenLabs Scribe v2 2,2 %.** **[unabhängig]** Datensätze: AA-AgentTalk 50 %, VoxPopuli-Cleaned 25 %, Earnings22-Cleaned 25 %; Sprachabdeckung auf der Seite nicht angegeben, vermutlich überwiegend Englisch (Schluss unbelegt). Quelle: https://artificialanalysis.ai/speech-to-text/non-streaming
  - Unabhängiger Test von Superwhisper (Stand 03.09.2026, nur englische Sets, kein Deutsch): Makro-WER 7,8 %, Antwortzeit 0,79 s. https://superwhisper.com/benchmarks/openai-gpt-transcribe
  - **FLEURS-Deutsch für gpt-transcribe: unbelegt.** Kein Hersteller- oder Drittwert gefunden.
  - Nächstliegender deutscher Fixpunkt (anderes Modell): **FLEURS de, WER: Whisper 5,46 %, GPT-4o mini Transcribe (Realtime-API) 4,07 %, Scribe v2 Realtime 4,31 %, Voxtral Mini Transcribe V2 3,54 %**; **Common Voice de: Whisper 6,25 %, GPT-4o mini 6,05 %**. Eigene Prüfung der Tabellen im PDF (Tabelle 7 und 8). „Whisper" ist dort nicht als large-v3 oder Turbo näher bezeichnet. Quelle: https://arxiv.org/pdf/2602.11298 (Voxtral Realtime, Mistral). Ein Suchergebnis nannte stattdessen 3,76 und 6,72; das stimmt mit dem PDF nicht überein und ist verworfen.
  - Whisper large-v3-turbo FLEURS de ca. 6,0 % (large-v3 4,6 %) in einem kleinen Fremdtest (50 Äußerungen je Sprache, Apple M3): https://vocova.app/blog/ai-transcription-accuracy-benchmark-2026. **[unabhängig, schwache Stichprobe]**
- **Freikontingent:** Keines. „Free" in den Quellen: Modellseite von `gpt-transcribe` listet nur Build/Launch/Grow, **keine Free-Zeile**. Spokenly: „no free tier for any hosted OpenAI transcription model". Siehe Abschnitt 8.
- **Preis:** 0,0045 USD/Min = **0,27 USD/Stunde**. Kein Batch-Preis. Quelle: https://developers.openai.com/api/docs/pricing
- **Rate Limits (Standard):** Build 5.000 RPM / 2.000.000 TPM; Launch 10.000 / 10.000.000; Grow 30.000 / 150.000.000. Quelle Modellseite (oben).
- **Max. Datei:** 25 MB. Formate mp3, mp4, mpeg, mpga, m4a, wav, webm. Längenlimit nicht genannt. Quelle: https://developers.openai.com/api/docs/guides/speech-to-text. Kein Batch-API-Zugang (https://developersdigest.tech/blog/whisper-1-gpt-4o-transcribe-deprecation, Sekundärquelle).
- **Streaming/Echtzeit:** `stream=true` streamt das Ergebnis einer fertigen Datei (`transcript.text.delta`, `transcript.text.done`). Das ist **kein** Live-Mikrofonstream. In Realtime-Sitzungen nur als „committed turn" über WebSocket (erst nach `input_audio_buffer.commit` wird transkribiert; nutzt frühere Turns automatisch als Kontext). Durchsatz laut Artificial Analysis rund 42 bis 56-fache Echtzeit, Buildfast nennt etwa 34-fach.
- **Vokabular/Prompting:** `prompt` (Freitext-Kontext), `keywords[]` (wörtliche Begriffe; nur Hinweise; eine Zeile je Keyword; verboten sind `<`, `>`, CR, LF, sonst wird die **ganze** Anfrage abgelehnt), `languages[]` (erwartete Sprachen, ersetzt `language`, beides zusammen verboten; ISO 639-1, ausgewählte 639-3, zh-Regionalcodes). Antwort liefert `languages` mit erkannten Sprachen (kann leer sein). Prompt-Längengrenze: nicht beziffert. Quelle: https://developers.openai.com/cookbook/examples/migrating_from_whisper_to_gpt_transcribe
- **Halluzinationsverhalten:** Keywords können nicht gesprochene Begriffe in den Text drücken; OpenAI rät ausdrücklich, auf „keyword hallucination" zu testen. **[offiziell]** Übermäßig spezifische Prompts können das Transkript verzerren (explainx). Ein Entwickler-Test (synthetische Stimmen, englisch-deutsch-englisch gemischt) fand bei zwei verschiedenen Stimmen, dass `gpt-transcribe` und `gpt-4o-transcribe` meist **nur den deutschen Mittelsatz** zurückgaben, whisper-1 und mini dagegen alles; bei einer Stimme perfekt. **[Forum/Einzelfall, synthetisch]** https://github.com/akaalias/quasi/issues/1. Eigene Halluzinationsrate für Stille/Rauschen: unbelegt.
- **Datenschutz/Datenresidenz:** Siehe Abschnitt 9.
- **Latenz:** Antwortzeit 0,79 s bei Kurzclips (Superwhisper, siehe oben). Sonst keine offiziellen Zahlen.
- **Android (REST):** `POST https://api.openai.com/v1/audio/transcriptions`, Header `Authorization: Bearer <KEY>`, multipart: `model=gpt-transcribe`, `file=@aufnahme.m4a`, `response_format=json` (nur JSON; `text`, `verbose_json`, `srt`, `vtt` entfallen), optional `prompt`, `keywords[]=…`, `languages[]=de`, `stream=true`. Die Schreibweise der Arrays im Multipart ist laut Cookbook eine Annahme; bei der Umsetzung gegen die API-Referenz prüfen. Keine Wort-/Segment-Zeitstempel, keine Sprecher, keine Übersetzung.

## 2. gpt-live-transcribe (Echtzeit-Transkription)

- **Modell-ID:** `gpt-live-transcribe` (einziger Snapshot). Azure Version `2026-07-29` (Katalog nennt 2026-07-28), dort als „current recommended model for realtime transcription".
- **Status:** Veröffentlicht 28./29.07.2026. Kein Abschaltdatum. Quelle: https://developers.openai.com/api/docs/models/gpt-live-transcribe
- **Deutsch-Qualität:** Kein deutscher Wert. OpenAI-Aggregate **[Vendor]**: Common Voice 22 Sprachen 19,70 % (gpt-realtime-whisper-1: 20,33 %); Real-World Audio 9 Sprachen 9,60 % (gegen 11,65 %); Context Aware ASR 38,5 % ohne, 44,6 % mit Kontext. Quelle: Forenankündigung (siehe 1). Als „57 Sprachen unterstützt" nennt es Slator (https://slator.com/openai-two-new-models-transcription/, für mich nicht abrufbar, nur über Suchauszug); OpenAI selbst veröffentlicht **keine** Sprachliste: **unbelegt**.
- **Freikontingent:** Keines (Modellseite ohne Free-Zeile).
- **Preis:** 0,017 USD/Min = **1,02 USD/Stunde** (gleich wie `gpt-realtime-whisper`, 3,8-mal so teuer wie `gpt-transcribe`).
- **Rate Limits:** Build 2.000 RPM / 210.000 TPM; Launch 10.000 / 600.000; Grow 10.000 / 780.000.
- **Max. Datei/Länge:** Keine Datei-Schnittstelle (Endpunkt nur `v1/realtime/transcription_sessions`). Sitzungsdauer für diesen Modus nicht genannt; Azure-Realtime allgemein 60 Minuten.
- **Streaming/Echtzeit:** Ja, WebSocket oder WebRTC. Audio PCM16, 24 kHz, Mono, Base64-Chunks (empfohlen 100 ms). `turn_detection` **muss `null` sein** (kein `server_vad`/`semantic_vad`; von OpenAI am 25.09.2026 bestätigt); Satzende per Client-VAD und `input_audio_buffer.commit`. Einstellung `delay`: `minimal`, `low`, `medium`, `high`, `xhigh` (niedrig = frühere Teiltexte, hoch = mehr Kontext und bessere WER). Events: `conversation.item.input_audio_transcription.delta` und `.completed`; Abschlüsse können in falscher Reihenfolge kommen, daher per `item_id` zuordnen. Quelle: https://developers.openai.com/api/docs/guides/realtime-transcription
- **Vokabular/Prompting:** Wie `gpt-transcribe`: `prompt`, `keywords`, `languages`, zusätzlich frühere Turns als Kontext. Liefert **keine** erkannte Sprache, keine Zeitstempel, keine Sprecher, keine Konfidenzen.
- **Halluzination/Fehlerverhalten:** Zwei offene Meldungen: (a) Deltas hören bei langen, nicht committeten Items (ca. 35 bis 60 s) ohne Fehler auf, danach gesprochene Audio wird nie transkribiert; Workaround: spätestens alle 15 s committen, oder nach 6 s ohne Delta committen und 1 s Audio erneut senden; OpenAI-Mitarbeiter bestätigte am 05.10.2026 die Reproduzierbarkeit, bis 12.10.2026 keine Lösung. Test mit Cantonesisch, `openai` Python SDK 2.52.0. Derselbe Melder sah am 03.10.2026 eine Zeichenfehlerrate von 0,48 statt 0,21 vom 17.08.2026 bei gleichem Code. **[Forum, Einzelbericht]** https://community.openai.com/t/gpt-live-transcribe-deltas-stop-mid-item-with-no-event-and-audio-appended-after-that-is-never-transcribed/1403323. (b) Ohne `commit` kommen nie `completed`-Events. https://community.openai.com/t/realtime-transcription-problem-gpt-live-transcribe-no-final-events-received/1397959
- **Datenschutz:** Siehe Abschnitt 9; regionale Verarbeitung (US und EU) für `gpt-live-transcribe` gelistet.
- **Latenz:** Keine veröffentlichte Millisekundenzahl (weder OpenAI noch unabhängig gefunden). OpenAI nennt nur „adjustable latency".
- **Android (Form):** Für Mobilgeräte empfiehlt OpenAI WebRTC; WebSocket geht mit **kurzlebigem Client-Secret** (Key nie in der App). WebSocket-URL laut Bugreport: `wss://api.openai.com/v1/realtime?intent=transcription`; danach `session.update` mit `session.type=transcription`, `audio.input.format={type:"audio/pcm",rate:24000}`, `audio.input.transcription={model:"gpt-live-transcribe", prompt, keywords, languages:["de"], delay:"low"}`, `turn_detection:null`. Die offizielle Seite nennt die URL nicht, daher beim Einbau prüfen.

## 3. gpt-realtime-whisper (Vorgänger der Live-Transkription)

- **Modell-ID:** `gpt-realtime-whisper` (Azure Version 2026-05-06). Wird nicht abgeschaltet, aber nicht mehr für neue Integrationen empfohlen. Preis 0,017 USD/Min. Limits in „Audio-Minuten pro Minute": Build 350, Launch 1.000, Grow 1.300. Nur Realtime-Transkription, kein Datei-Endpunkt. Quelle: https://developers.openai.com/api/docs/models/gpt-realtime-whisper
- Deutsch-Qualität: kein deutscher Wert; Vergleichswerte im Live-Abschnitt (20,33 % Common Voice 22 Sprachen, 11,65 % Real-World Audio).
- Eine Suchquelle behauptet, `gpt-realtime-whisper` habe die niedrigste Latenz; die OpenAI-Seiten bestätigen das nicht (unbelegt).

## 4. gpt-4o-transcribe

- **Modell-ID:** `gpt-4o-transcribe`. **Status:** deprecated, **Abschaltung 26.02.2027**, Ersatz `gpt-transcribe` oder `gpt-live-transcribe`. Quelle: https://developers.openai.com/api/docs/deprecations (Eintrag 2026-08-26). Nur ein Snapshot gelistet.
- **Deutsch-Qualität:** Kein eigener OpenAI-FLEURS-Deutschwert abrufbar (Ankündigungsseite von 03/2025 liefert 403, Diagramm ohnehin nur Bild). AA-WER v2 4,0 %. Code-Switching-Test (Vendor AssemblyAI): 44,58 % mittlere WER, bei einer anderen Studie 38,6 % über vier Sprachpaare (Arabisch, Persisch, Deutsch): https://arxiv.org/pdf/2605.19069. Forum: manche Nutzer fanden whisper-1 genauer.
- **Freikontingent:** keines. **Preis:** 2,50 USD Eingabe / 10 USD Ausgabe je 1 Mio. Audio-Tokens, etwa 0,006 USD/Min = 0,36 USD/h.
- **Rate Limits:** Build 2.000 RPM / 100.000 TPM; Launch 10.000 / 2.000.000; Grow 10.000 / 6.000.000.
- **Kontext:** 16.000 Tokens, max. Ausgabe 2.000 Tokens, Wissensstand 01.06.2024. Datei 25 MB. Endpunkte: Transkription und Realtime.
- **Streaming:** Datei-Streaming ja; Realtime ja.
- **Prompt:** `prompt` ja (kein `keywords`/`languages`).
- **Halluzination/Fehler:** Abschneiden der Ausgabe nach etwa 8 bis 12 Minuten (in der Antwort `output_tokens: 2048`, also vermutlich das Ausgabe-Limit), kein Fehler zurückgegeben, whisper-1 transkribierte dieselben Dateien vollständig. **[Forum, 08/2025]** https://community.openai.com/t/gpt-4o-transcribe-truncates-output-after-8-9-minutes-even-on-short-segments/1354656. Außerdem fehlende Wörter am Anfang und Ende kurzer Clips, Latenz 1,6 s gegen 0,86 s bei whisper-1 (Einzeltest): https://community.openai.com/t/gpt-4o-mini-transcribe-and-gpt-4o-transcribe-not-as-good-as-whisper/1153905
- **Datenresidenz:** `gpt-4o-transcribe` in Liste der regionalen Verarbeitung (US/EU). Azure: Preview, in Global Standard und DataZone Standard (Europa: francecentral, swedencentral).

## 5. gpt-4o-mini-transcribe

- **Modell-ID:** `gpt-4o-mini-transcribe`; Snapshots `gpt-4o-mini-transcribe-2025-03-20` (Entfernung laut Sekundärquelle am 20.01.2027, https://developersdigest.tech/blog/whisper-1-gpt-4o-transcribe-deprecation) und `-2025-12-15` (Standard). **Status:** deprecated, **Abschaltung 26.02.2027**.
- **Deutsch-Qualität:** **FLEURS de 4,07 % WER** (Realtime-API-Lauf, Mistral-Paper, Tabelle 7), Common Voice de 6,05 %. Gesamt FLEURS-Mittel 7,95 %, MCV-Mittel 12,85 %. https://arxiv.org/pdf/2602.11298. AA-WER v2: 4,5 %.
- **Preis:** 1,25 / 5 USD je 1 Mio. Audio-Tokens, etwa 0,003 USD/Min = **0,18 USD/h**. Limits: Build 2.000 RPM / 150.000 TPM; Launch 10.000 / 2.000.000; Grow 10.000 / 8.000.000. Kontext 16.000, Ausgabe 2.000. Datei 25 MB. Kein Freikontingent.
- Übrige Punkte wie `gpt-4o-transcribe` (Prompt ja, Datei- und Realtime-Streaming ja, Abschneide-Berichte gelten ebenfalls für mini).

## 6. gpt-4o-transcribe-diarize

- **Modell-ID:** `gpt-4o-transcribe-diarize`. **Status:** deprecated, Abschaltung 26.02.2027, **kein dokumentierter Ersatz** mit Sprechertrennung (Azure: GA, Abschaltung dort 16.04.2027 laut Forenbeitrag, unbestätigt). Preis wie `gpt-4o-transcribe` (2,50 / 10 USD je 1 Mio. Tokens, etwa 0,006 USD/Min). Limits: Build 5.000 RPM / 100.000 TPM; Launch 10.000 / 2.000.000; Grow 10.000 / 6.000.000.
- Nur Datei-Endpunkt, `response_format=diarized_json`, bei Audio über 30 s `chunking_strategy` = `"auto"` oder VAD-Konfiguration; bis zu 4 bekannte Sprecher (`known_speaker_names[]`, `known_speaker_references[]`, je 2 bis 10 s). Kein `prompt`. Nicht in Realtime.
- Für Diktat irrelevant. Bekanntes Problem: Auf Azure ignoriert die Realtime-Variante `language=de` und liefert unsinnige englische Texte, offline mit demselben Audio korrekt; keine Lösung (Microsoft Q&A, 04/05.2026): https://learn.microsoft.com/en-us/answers/questions/5864686/azure-openai-realtime-api-gpt-4o-transcribe-diariz

## 7. whisper-1

- **Modell-ID:** `whisper-1` (Whisper large-v2, gehostet). **Status:** deprecated, **Abschaltung 26.02.2027**. Preis **0,006 USD/Min = 0,36 USD/h**.
- **Rate Limits (Modellseite):** **Free 3 RPM / 200 RPD**, Build 2.500 RPM, Launch 7.500, Grow 10.000. Das ist die einzige Free-Zeile in der gesamten OpenAI-Audio-Dokumentation. Quelle: https://developers.openai.com/api/docs/models/whisper-1
- Funktionen, die als einzige noch `whisper-1` brauchen: Wort-/Segment-Zeitstempel (`timestamp_granularities[]`, `verbose_json`), SRT/VTT, Übersetzung nach Englisch. `prompt` höchstens 224 Tokens. Kein Streaming. Sprachen: 98 (Genauigkeit je Sprache verschieden). Datei 25 MB.
- Deutsch: Whisper-Gewichte FLEURS de 5,46 % (Voxtral-Paper, Variante nicht benannt). Bekannt für Halluzinationen bei Stille und Rauschen (z. B. angehängtes „Bye." im Forum), das ist generell Whisper-Verhalten.

## 8. Freikontingent, Konten, Tiers (alle OpenAI-STT-Modelle)

- **Tiers laut offizieller Rate-Limit-Seite:** Free (Standort in unterstützter Region, Monatslimit 100 USD), Build (5 USD Guthaben gekauft, 500 USD/Monat), Launch (100 USD, 5.000 USD), Grow (500 USD, 200.000 USD). Die Seite sagt nicht, welche Modelle im Free-Tier aufrufbar sind; die Tabellen der Audio-Modelle `gpt-transcribe`, `gpt-live-transcribe`, `gpt-4o-*` führen **nur Build/Launch/Grow**. Nur `whisper-1` hat eine Free-Zeile. https://developers.openai.com/api/docs/guides/rate-limits
- **Startguthaben:** keines. Eine Zusammenstellung vom 18.10.2026 sagt: Preisseite listet kein Free-Tier, keinen Test- oder Anmeldekredit; neue API-Konten müssen vor dem ersten Aufruf mindestens 5 USD aufladen (Sekundärquelle, Stand September 2026). https://aifree.dev/learn/free-openai-api-access. Ältere Seiten mit „5 USD Gratis für 3 Monate" sind überholt.
- **Daten-teilen-Programm („komplimentäre Tokens"):** Wer „Share inputs and outputs with OpenAI" einschaltet, bekommt tägliche Gratis-Tokens: große Modelle 250.000 (Tier 1 bis 2) bzw. 1 Mio. (Tier 3 bis 5), kleine Modelle 2,5 Mio. bzw. 10 Mio.; Zähler setzt 00:00 UTC zurück; positives Guthaben nötig; nicht für ZDR und Enterprise; Eingaben dürfen zum Training genutzt werden; 30 Tage Vorankündigung zum Ende. Die genannten Modellgruppen (gpt-5.x, o-Serie, gpt-4.1, gpt-4o, mini/nano) enthalten **keine Audio-/Transkriptions-Modelle**. Ob `gpt-transcribe` & Co. zählen: **unbelegt** (offizielle Hilfeseite ist für mich nicht abrufbar, 403). Quellen: https://rafaelpfister.ch/en/blog/up-to-10-million-free-tokens-per-day-using-openai-s-data-sharing-program-with-cost-guardrails (Stand 27.08.2026), https://pricepertoken.com/endpoints/openai/free. Praktischer Schluss: nicht damit planen. Das Programm ist ohnehin tokenbasiert, `gpt-transcribe` und `gpt-live-transcribe` werden nach Minuten abgerechnet.
- Startup-Programme (OpenAI for Startups) existieren, Betrag unveröffentlicht; für Frank nicht relevant.
- Fazit Gratis: **Gegen das Groq-Freikontingent ist OpenAI nicht konkurrenzfähig.** Echtes Gratis bei OpenAI-Audio nur theoretisch über `whisper-1` (3 RPM, 200/Tag), und das läuft aus.

## 9. Datenschutz, Retention, EU-Residenz (OpenAI direkt)

Quelle: https://developers.openai.com/api/docs/guides/your-data
- **Training:** API-Daten werden standardmäßig nicht zum Training genutzt (seit 01.03.2023), außer bei Opt-in.
- **Retention:** `/v1/audio/transcriptions`: standardmäßig keine Abuse-Monitoring-Speicherung und kein Anwendungszustand. `/v1/realtime`: Abuse-Monitoring-Logs 30 Tage. Beide **ZDR-fähig**.
- **EU-Datenresidenz** (`eu.api.openai.com`, EWR + Schweiz): Speicherung und **Verarbeitung** in der EU für Audio-Transkription, Realtime-Transkription und Live-Sitzungen. Modelle laut Residenz-Seite: Audio-Endpunkt `whisper-1`, `gpt-4o-transcribe`, `gpt-4o-mini-transcribe`, `gpt-transcribe`; Realtime-Transkription `gpt-realtime-whisper`, `gpt-live-transcribe`, `gpt-transcribe`. Voraussetzung: Projekt mit Region anlegen, **Freigabe durch OpenAI-Vertrieb**, ZDR oder Modified Abuse Monitoring oder Private Retention. Aufschlag **+10 %** für nach dem 05.03.2026 veröffentlichte, residenzfähige Modelle (die Preisseite bestätigt nicht, dass der Aufschlag für `gpt-transcribe` gilt). Realtime-Tracing ist nicht EU-konform. Für Frank (Privatperson ohne Vertriebsvertrag) praktisch kaum erreichbar. Quelle: https://developers.openai.com/api/docs/guides/your-data#data-residency-and-inference-residency
- OpenRouter: `openai/gpt-transcribe` ist **nicht** über EU-In-Region-Routing verfügbar. https://openrouter.ai/openai/gpt-transcribe

## 10. Azure OpenAI / Microsoft Foundry als Bezugsweg

- **Verfügbarkeit:** `gpt-transcribe` (2026-07-28) und `gpt-live-transcribe` (2026-07-29) GA, nur **Global Standard**. Abgerechnet wird je Stunde. Europa in der Regionstabelle: **francecentral** und **swedencentral** (Global Standard). Der Modellkatalog nennt für `gpt-transcribe` stattdessen Canada Central, France Central, India South („more coming soon"); Widerspruch, Regionstabelle (aktualisiert 19.10.2026) ist die jüngere Quelle. Quellen: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure-region-availability, https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure
- **Datenresidenz:** Global Standard heißt: Verarbeitung kann weltweit stattfinden (Speicherung ruhender Daten in der Geografie). **DataZone Standard (EU-Verarbeitung)** gibt es in der Tabelle nur für `gpt-4o-transcribe` (francecentral, swedencentral), nicht für `gpt-transcribe`/`gpt-live-transcribe`. `whisper` (001) Standard-regional: norwayeast, swedencentral, switzerlandnorth, westeurope. Eine echte EU-only-Verarbeitung für die neuen Modelle ist bei Azure damit **derzeit nicht belegt**.
- **Preis:** Ein Suchauszug des Microsoft-Foundry-Blogs nennt Global Standard Audio **0,27 USD/h (`gpt-transcribe`)** und **1,02 USD/h (`gpt-live-transcribe`)**, also dieselben Preise wie OpenAI direkt. Die Blogseite war nicht abrufbar, die Azure-Preisseite zeigt nur Platzhalter („$-"). https://techcommunity.microsoft.com/blog/azure-ai-foundry-blog/introducing-gpt-transcribe-and-gpt-live-transcribe-in-microsoft-foundry/4541740. Azure-Preis für `whisper` und `gpt-4o-transcribe`: unbelegt.
- **Technik:** Datei bis 25 MB über `POST /v1/audio/transcriptions` (`gpt-transcribe` unterstützt Sprachhinweise); Live über Realtime-API mit GA-Endpunkt `/openai/v1/...` (kein `api-version`), Deployment-Name statt Modell-ID; Sitzung maximal 60 Minuten; Eingabe 32.000 / Ausgabe 4.096 Tokens. Authentifizierung per API-Key oder Entra-ID. Azure empfiehlt ISO-639-1-Sprachhinweis. Quotas: auf der Quotas-Seite nachzuschlagen, hier nicht beziffert.
- **Auslauf:** `gpt-4o-transcribe`/`-mini` stehen bei Azure noch als Preview; ein Forenbeitrag nennt Abschaltung 01.06.2026, die Doku widerspricht dem nicht klar; unbestätigt.
- **Gratis:** Azure for Students/Startup-Guthaben möglich, kein modellspezifisches Freikontingent belegt.

## 11. Tabelle „Kerndaten"

| Modell | Status | Preis/Stunde | Rate Limit (Build) | Deutsch (WER) | Streaming | Prompt/Keywords | EU-Residenz |
|---|---|---|---|---|---|---|---|
| gpt-transcribe | GA seit 29.07.2026 | 0,27 USD | 5.000 RPM / 2 Mio. TPM | kein DE-Wert; CV-22-Aggregat 19,27 %; AA-WER 3,3 % | Datei-Stream, Realtime nur committed | prompt, keywords, languages | ja (OpenAI mit Freigabe), Azure nur Global |
| gpt-live-transcribe | GA seit 29.07.2026 | 1,02 USD | 2.000 RPM / 210.000 TPM | kein DE-Wert; CV-22-Aggregat 19,70 % | Live, WS/WebRTC, turn_detection=null | prompt, keywords, languages, delay | ja (OpenAI mit Freigabe), Azure nur Global |
| gpt-realtime-whisper | GA, nicht mehr empfohlen | 1,02 USD | 350 Audio-Min/Min | kein DE-Wert; CV-22 20,33 % | Live | prompt | ja |
| gpt-4o-transcribe | abgeschaltet 26.02.2027 | 0,36 USD | 2.000 RPM / 100.000 TPM | kein offizieller DE-Wert; AA-WER 4,0 % | Datei und Realtime | nur prompt | ja |
| gpt-4o-mini-transcribe | abgeschaltet 26.02.2027 | 0,18 USD | 2.000 RPM / 150.000 TPM | FLEURS de 4,07 %, CV de 6,05 % | Datei und Realtime | nur prompt | ja |
| gpt-4o-transcribe-diarize | abgeschaltet 26.02.2027 | 0,36 USD | 5.000 RPM / 100.000 TPM | kein DE-Wert | nur Datei | kein prompt | ja (Azure: Global) |
| whisper-1 | abgeschaltet 26.02.2027 | 0,36 USD | 2.500 RPM (Free: 3 RPM, 200/Tag) | FLEURS de ca. 5,46 % (Whisper, Variante offen) | nein | prompt (224 Tokens) | ja |
| Groq whisper-large-v3-turbo (Basislinie) | – | ca. 0,04 USD (AA: 0,67 USD/1000 Min) | – | AA-WER 4,6 %; FLEURS de ca. 6,0 % (kleiner Fremdtest) | – | – | – |

Alle Modelle: 25 MB Dateilimit (außer reine Realtime-Modelle). Kein Modell hat ein belegtes Freikontingent für Transkription.

## 12. Fazit: Besser als Groq Turbo für Deutsch?

**Unklar, mit Tendenz zu „ja" für Genauigkeit, aber „nein" für Kosten und Gratis.**

Für „besser":
- Artificial Analysis (unabhängig, überwiegend Englisch/Fachsprache): 3,3 % gegen 4,6 % WER, also etwa 28 % weniger Fehler als Turbo.
- OpenAI-Eigenmessung: Common-Voice-Fehlerrate über 22 Sprachen halbiert gegenüber whisper-1; `keywords` und `prompt` erlauben gezielte Eigennamen- und Fachwortlisten (bei Turbo nur `prompt` mit 224 Tokens). Das ist der für Frank wichtigste Vorteil bei Eigennamen.
- Das Vorläufermodell `gpt-4o-mini-transcribe` schlägt Whisper auf FLEURS Deutsch (4,07 % gegen 5,46 %), und `gpt-transcribe` ist laut AA nochmals besser als `gpt-4o-transcribe`.

Für „unklar" oder „nein":
- **Kein deutscher WER für `gpt-transcribe` veröffentlicht**, nicht von OpenAI, nicht unabhängig. Der Vergleichsmaßstab der OpenAI-Zahl ist whisper-1, nicht Turbo.
- Es gibt Berichte über Aussetzer: `gpt-transcribe` lieferte bei gemischtsprachigem synthetischem Audio nur den deutschen Teil; `gpt-live-transcribe` hat offene Fehler (Deltas brechen ab, 10/2026).
- **Kein Freikontingent**: 0,27 USD/h (Datei) bzw. 1,02 USD/h (Live) gegen Groq gratis oder etwa 0,04 USD/h. Zum Ziel „großes kostenloses Kontingent" passt OpenAI nicht.
- Tipp zur Entscheidung: Eine eigene A/B-Messung mit Franks echtem deutschem Diktat (Zahlen, Eigennamen) über `gpt-transcribe` gegen Turbo ist belastbarer als alle öffentlichen Zahlen. Kosten für 10 Stunden Testaudio: 2,70 USD.
- Für die Live-Auswahl in Jarvis: `gpt-4o-mini-transcribe` läuft am 26.02.2027 aus; das sollte vor dem Datum entfernt werden.

---

BEST-PRACTICES-KANDIDATEN:
- Realtime-Transkription mit `gpt-live-transcribe`: `turn_detection` auf `null`, Satzende per Client-VAD und `input_audio_buffer.commit`, Abschlüsse über `item_id` zuordnen, spätestens alle 15 s committen, Ausfall-Wächter (6 s ohne Delta committen und 1 s Audio nachsenden). https://developers.openai.com/api/docs/guides/realtime-transcription (Stand 21.10.2026, Modell `gpt-live-transcribe` 2026-07-29), https://community.openai.com/t/gpt-live-transcribe-deltas-stop-mid-item-with-no-event-and-audio-appended-after-that-is-never-transcribed/1403323
- Dateitranskription mit `gpt-transcribe`: `languages[]` statt `language`, `keywords[]` vorab validieren (kein `<`, `>`, CR, LF; eine ungültige Eingabe lässt die ganze Anfrage scheitern), auf „Keyword-Halluzination" testen, nur JSON-Antwort, Migration von whisper-1 vor dem 26.02.2027. https://developers.openai.com/cookbook/examples/migrating_from_whisper_to_gpt_transcribe (Stand 10/2026)
- Android: OpenAI-Schlüssel nie in der App; für Realtime kurzlebige Client-Secrets, Audio PCM16 24 kHz Mono in 100-ms-Chunks. https://developers.openai.com/api/docs/guides/realtime-websocket

BUG-KANDIDATEN:
- `gpt-live-transcribe`: Deltas stoppen bei langen, nicht committeten Items still, danach gesendete Audio wird nie transkribiert (Python SDK `openai` 2.52.0, 03./04.10.2026; OpenAI bestätigt Reproduzierbarkeit am 05.10.2026, kein Fix bis 12.10.2026). https://community.openai.com/t/gpt-live-transcribe-deltas-stop-mid-item-with-no-event-and-audio-appended-after-that-is-never-transcribed/1403323
- `gpt-live-transcribe` und `gpt-realtime-whisper`: ohne `input_audio_buffer.commit` kommen nur Deltas, nie `completed`; `server_vad`-Versuch liefert „Turn detection is not supported for this transcription model" (bestätigt 25.09.2026). https://community.openai.com/t/realtime-transcription-problem-gpt-live-transcribe-no-final-events-received/1397959
- `gpt-4o-transcribe`/`-mini`: stille Kürzung der Ausgabe nach etwa 8 bis 12 Minuten (Ausgabe-Limit 2.048 Tokens, 08/2025, Modellsnapshot 2025-03-20); whisper-1 transkribiert vollständig. https://community.openai.com/t/gpt-4o-transcribe-truncates-output-after-8-9-minutes-even-on-short-segments/1354656
- `gpt-transcribe`/`gpt-4o-transcribe` mit gemischtsprachigem synthetischem Audio zweier Stimmen: nur deutscher Satz zurückgegeben (Einzeltest 14.10.2026). https://github.com/akaalias/quasi/issues/1
- Azure `gpt-4o-transcribe-diarize` in Realtime ignoriert `language=de`, liefert englischen Unsinn (2026, ungelöst). https://learn.microsoft.com/en-us/answers/questions/5864686/azure-openai-realtime-api-gpt-4o-transcribe-diariz
