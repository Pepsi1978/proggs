# R8 — Deutsche Aussprache und unabhängige Qualitätsbelege

Recherchezeitpunkt: 21.10.2026. Alle Abrufe am 21.10.2026 (WebSearch/WebFetch, Engine C). Kein Hörtest, keine eigenen API-Aufrufe: alles hier ist aus Dokumentation, Ranglisten und Drittquellen belegt oder ausdrücklich als „unbelegt“ markiert.

Methodenhinweis: WebFetch verarbeitet Seiten mit einem kleinen Modell. Zahlen aus Ranglisten wurden, wo möglich, aus zwei getrennten Abrufen derselben Seite gegengeprüft (Provider-Voice-Arena: zwei Seiten, identische Werte). Rangzahlen können je nach Tabellenansicht um 1–3 Plätze (Gleichstände) abweichen. Die Artificial-Analysis-Seiten tragen kein „Zuletzt aktualisiert“-Datum; die jüngsten Einträge sind „Okt 2026“.

## 1. Ranglisten (Stand Oktober 2026)

### 1.1 Wichtigste Einordnung: Sind die Ranglisten englisch?

| Rangliste | Sprache | Beleg |
|---|---|---|
| Artificial Analysis „Speech Arena“ / Provider Voice Leaderboard | Nur Englisch-Akzente (US/UK), kein Sprachfilter, Prompts in Kategorien Kundenservice/Unterhaltung/Wissen/Assistenten | https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice (Filter: Accent US/UK, Kategorie, Open Weights; kein Sprachfilter) und https://artificialanalysis.ai/text-to-speech/methodology („US and UK accents“) — abgerufen 21.10.2026 |
| Artificial Analysis „Controlled Voice Arena“, Sprache English (US & UK) | Englisch | https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice — 21.10.2026 |
| Artificial Analysis „Controlled Voice Arena“, mehrsprachig | Spanisch, Französisch, **Deutsch**, Portugiesisch, Japanisch, Hindi, Mandarin, Arabisch, Vietnamesisch; Muttersprachler-Rater, je eine männliche und eine weibliche Referenzstimme, Modelle werden nur in unterstützten Sprachen getestet | Methodik: https://artificialanalysis.ai/text-to-speech/methodology — 21.10.2026 |
| Hugging Face TTS Arena V2 (TTS-AGI) | Nicht abrufbar (Tabelle lädt per JavaScript), Sprachabdeckung unbelegt | https://huggingface.co/spaces/TTS-AGI/TTS-Arena-V2 — 21.10.2026 |
| Voice Arena (voicearena.com) | 7 Sprachen: US-Englisch, Hindi, Japanisch, brasil. Portugiesisch, mexikan. Spanisch, Arabisch, Vietnamesisch. **Kein Deutsch.** | https://voicearena.com/tts-methodology — 21.10.2026 |
| Coval-TTS-Leaderboard (WER) | Englisch | über Gradium-Seite, s. u. |

Folge: Die Gesamt-ELO-Liste von Artificial Analysis sagt **nichts direkt über Deutsch**. Deutsch-spezifisch ist nur die mehrsprachige Controlled Voice Arena, deren deutsche Tabelle ich per WebFetch nicht vollständig abrufen konnte (die Seite lieferte nur die englische Ansicht). Siehe 1.3.

### 1.2 Artificial Analysis Provider Voice Arena (Gesamt-ELO, englische Akzente)

Quelle: https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice und https://artificialanalysis.ai/text-to-speech/leaderboard — beide abgerufen 21.10.2026. 98 gelistete Modelle.

Top 20:

| Rang | Modell | Anbieter | ELO | Release |
|---|---|---|---|---|
| 1 | Eleven v4 Turbo | ElevenLabs | 1335 | Sep 2026 |
| 2 | Eleven v4 | ElevenLabs | 1329 | Sep 2026 |
| 3 | Qwen-Audio-3.1-TTS-Plus | Alibaba | 1301 | Sep 2026 |
| 4 | Sonic 3.6 | Cartesia | 1289 | Aug 2026 |
| 5 | Gemini 3.8 Flash TTS | Google | 1278 | Sep 2026 |
| 6 | Realtime TTS-2 | Inworld | 1267 | Aug 2026 |
| 7 | Qwen-Audio-3.0-TTS-Plus | Alibaba | 1265 | Sep 2026 |
| 8 | Gemini 3.8 Flash-Lite TTS | Google | 1251 | Sep 2026 |
| 9 | Fish Audio Drama 3 Preview | Fish Audio | 1248 | Okt 2026 |
| 10 | Simba 3.2 | Speechify | 1245 | Jul 2026 |
| 11 | Breeze TTS 2 | BreezeBlue | 1225 | Aug 2026 |
| 12 | Voxtral 3 TTS | Mistral | 1218 | Okt 2026 |
| 13 | Realtime TTS-2 Flash | Inworld | 1218 | Aug 2026 |
| 14 | StepAudio 2.5 TTS | StepFun | 1216 | Aug 2026 |
| 15 | v3 Conversational | ElevenLabs | 1214 | Aug 2026 |
| 16 | Gemini 3.1 Flash TTS | Google | 1213 | Apr 2026 |
| 17 | Luna TTS | VUI Labs | 1210 | Jun 2026 |
| 18 | Sonic 3.5 | Cartesia | 1191 | Mai 2026 |
| 19 | Ming-3.0-tts | Ant Group | 1190 | Okt 2026 |
| 20 | Fish Audio S2.1 Pro | Fish Audio | 1189 | Okt 2026 |

Plätze aller Kandidaten (englischsprachige Rangliste, ELO in Klammern, wo abgerufen):

| Kandidat | Eintrag und Rang |
|---|---|
| Google Chirp 3 HD | **Rang 60 (1061)**, Release Mär 2025 |
| Gemini-TTS | gemini-3.8-flash-tts **5 (1278)**; gemini-3.8-flash-lite-tts **8 (1251)**; gemini-3.1-flash-tts-preview **16 (1213)**; 2.5 Flash Lite 49; 2.5 Flash (Dez 2025) 59; 2.5 Pro 72 |
| Azure | Azure HD 2.5 **30 (1142)**; Azure Neural 74. DragonHD/MAI-Voice-2 nicht als eigene Einträge gelistet (unbelegt, ob unter „Azure HD 2.5“ geführt) |
| Edge-TTS | nicht gelistet |
| ElevenLabs | v4 Turbo 1, v4 2, v3 Conversational 15, **v3 18–21 (1183)**, Turbo v2.5 42, Multilingual v2 43, **Flash v2.5 50** |
| Cartesia | Sonic 3.6 4, Sonic 3.5 18, Sonic 3 61 |
| Inworld | Realtime TTS-2 6, Realtime TTS-2 Flash 13. „Inworld TTS-2“ des Auftrags = vermutlich Realtime TTS-2 (Zuordnung nicht von der Seite bestätigt) |
| Hume | Octave 2 **63 (1055)**, Octave TTS 71 |
| OpenAI | TTS-1 HD 40, TTS-1 47, GPT-Realtime-2 53. **gpt-4o-mini-tts nicht gelistet** |
| Amazon Polly | Generative **56**, Long-Form 77, Neural 92, Standard 98 |
| Deepgram | **Aura-2 nicht gelistet**; nur „Flux TTS“ 65 (1052) |
| Mistral Voxtral | Voxtral 3 TTS **12 (1218)**; Voxtral TTS 45 |
| Murf | Falcon 2 **28 (1153)**; Speech Gen 2 82 |
| xAI Grok | nicht als „Grok“ gelistet; „SpaceXAI TTS“ 29 (1150) ist möglicher Treffer, Zuordnung unbelegt |
| Alibaba Qwen | Qwen-Audio-3.1-TTS-Plus 3, 3.0-TTS-Plus 7, Qwen3 TTS Flash 86 (der Auftrag nennt „qwen3-tts-flash“: Rang 86, weit hinten) |
| MiniMax | Speech 2.8 HD 24 (1174), 2.8 Turbo 26, 2.6 HD 33 |
| Fish Audio | Drama 3 Preview 9, S2.1 Pro 20, S2 Pro 37 |

Einordnung Jarvis: Chirp 3 HD (heute genutzt) liegt auf Rang 60 von 98, rund 220 ELO-Punkte hinter Gemini 3.8 Flash TTS. Das gilt für **englische** Hörer. ELO-Differenzen von wenigen Punkten sind statistisch nicht belastbar (die Controlled-Arena weist Ränge als Spannen aus).

### 1.3 Artificial Analysis Controlled Voice Arena

**Englisch (US & UK)**, abgerufen 21.10.2026, https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice (zwei Abrufe, identisch):

| Rang | Modell | ELO |
|---|---|---|
| 1–2 | HeyGen Voice | 1195 |
| 1–2 | Qwen-Audio-3.1-TTS-Plus | 1184 |
| 3–6 | Eleven v4 Turbo / Eleven v4 | 1156 |
| 3–6 | Sonic 3.6 | 1151 |
| 3–6 | Realtime TTS-2 (Inworld) | 1145 |
| 7–8 | Sonic 3.5 | 1116 |
| 10–15 | Eleven v3 | 1071 |
| 10–20 | Voxtral 3 TTS | 1064 |
| 12–20 | Gemini 3.8 Flash-Lite TTS | 1054 |
| 16–21 | Gemini 3.8 Flash TTS | 1047 |
| 27–32 | Multilingual v2 | 995 |
| 31–33 | Flash v2.5 | 986 |

Chirp 3 HD, Azure, OpenAI, Polly, Hume, Deepgram erscheinen in dieser (kontrollierten, Voice-Cloning-basierten) Arena nicht.

**Deutsch**: Die deutsche Tabelle war per WebFetch nicht abrufbar. Belegt ist nur, aus einer Zweitquelle:

- AlphaSignal (22.09.2026): Cartesia Sonic 3.6 steht in der deutschen Controlled Voice Arena auf Platz 1, Inworld Realtime TTS-2 auf Platz 2 mit 1.261 ELO, 22 Punkte dahinter (Sonic 3.6 daraus abgeleitet etwa 1.283, nicht genannt). Boson Higgs Audio V3 TTS ist das beste Open-Weights-Modell auf Deutsch. Pro Sprache 15–24 Modelle. Quelle: https://alphasignal.ai/news/cartesia-s-sonic-3-6-dominates-eight-of-nine-global-voice-ai-leaderboards — 21.10.2026.
- Dieselbe Quelle: „Eleven v3“-Familie in 7 von 9 Sprachen unter den Top 3 (Sprache nicht aufgeschlüsselt, ob Deutsch dabei ist: unbelegt). Die Überschrift („acht von neun“) und der Text (sieben genannte Sprachen) widersprechen sich; Quelle ist Zweitberichterstattung, Cartesia-nah in der Gewichtung. Cartesias eigene „bis 93 % Präferenz“-Zahl ist Herstellerangabe (https://www.plushcap.com/content/cartesia/blog/cartesia-introducing-sonic-36, 27.08.2026).
- Platz von Gemini 3.8, Chirp 3 HD, Azure, OpenAI, Polly auf Deutsch: **unbelegt**. Die Controlled Arena testet nur Modelle mit Voice-Cloning-Fähigkeit; ob Chirp 3 HD mit Standardstimmen dort teilnimmt, ist nicht belegt.

Empfehlung: Frank öffnet https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice im Browser und stellt dort „Language“ auf „German“ (ich konnte den Sprachwechsel per WebFetch nicht auslösen; Browser-Automation war laut Auftrag nicht vorgesehen).

### 1.4 Weitere Ranglisten und Messungen

| Quelle | Inhalt | Einordnung |
|---|---|---|
| MINT-Bench, arXiv 2604.17958 (April 2026), https://arxiv.org/html/2604.17958v1 — 21.10.2026 | Mehrsprachiger Benchmark für Instruction-Following-TTS, Deutsch als „Mini-Split“ (ca. 274 Items), 10 Muttersprachler-Rater je Sprache. Deutsch-„Perceptual Quality“ (PE, höher besser): **Gemini 2.5 Flash 3,96**, Gemini 2.5 Pro 3,72, **ElevenLabs v3 3,52**, MiniMax Speech 2.7 2,74, **GPT-4o-mini-TTS 1,77**; Open Source: Qwen3TTS-1.7B-VD 3,57. | Unabhängig (akademisch), deutschspezifisch, aber alte Modellstände (Gemini 2.5, nicht 3.8; ElevenLabs v3, nicht v4). Skala von PE nicht erläutert, nur relativ lesbar. Chirp 3 HD, Azure, Cartesia, Inworld, Polly nicht enthalten. Appendix D.1 (Vollergebnisse) nicht gelesen. |
| Gradium, MiniMax-Mehrsprachentest, https://gradium.ai/content/tts-wer-benchmark-2026 (29.04.2026) — 21.10.2026 | **Hersteller-Benchmark (Gradium verkauft TTS).** Deutsch-WER in % (Whisper-Basic-Normalizer, Qwen3-ASR): Qwen3 TTS 0,35; Cartesia Sonic-3 0,37; Gradium 0,54; ElevenLabs Flash v2.5 0,61; Mistral Voxtral 0,69; ElevenLabs Multilingual v2 0,72. | Misst Verständlichkeit per Spracherkennung, nicht Natürlichkeit und nicht Textnormalisierung (die Referenz-Normalizer glätten Zahlenschreibweisen). Alle Werte < 1 %, Unterschiede klein. Einzige gefundene Deutsch-Zahl zu Cartesia, Voxtral, ElevenLabs Flash. |
| Gradium Hard-Case-Test (28./31.08.2026), gleiche Seite | 500 Sätze, 100 pro Sprache (EN/FR/ES/PT/DE), Muttersprachler, Bestehensquote: Gradium 81,0 %, Cartesia Sonic 3.6 75,1 %, Eleven v3 Conversational 65,4 %, Inworld Realtime TTS-2 61,5 %, Fish S2.1 Pro 49,5 %. | **Herstellertest**, Gradium wählte die Vergleichsmodelle und den Testsatz. Nur über alle fünf Sprachen gemittelt, **keine Deutsch-Zahl**. Sätze (CC BY 4.0): gradium/tts-eval-customer-support-202608 auf Hugging Face. |
| Coval-TTS-Leaderboard (WER, Englisch), laut Gradium-Seite gelesen 08.09.2026 | Englisch: Soniox 4,0 %, Eleven v3 Conversational 4,3 %, Inworld TTS 2 4,5 %, Fish S2.1 Pro 4,7 %, OpenAI GPT-4o mini TTS 4,9 %, Deepgram Aura-2 5,0 %, Cartesia Sonic 3.6 5,3 %, Eleven Flash v2.5 6,5 %. | Unabhängige Plattform, aber **nur Englisch**. Nützlich nur als Indiz, dass OpenAI und Deepgram bei Zahlen/Wortgenauigkeit nicht schlechter sind als die Spitze. |
| Podonos, Gemini 2.5 Flash vs. ElevenLabs Multilingual v2, https://www.podonos.com/blog/gemini-vs-elevenlabs — 21.10.2026 (aus Suchzusammenfassung, Seite nicht geöffnet) | Beide hatten Probleme bei Adressen und Zahlen; ElevenLabs minimal vorn. | Englischfokus, 2025, alter Stand. |

Nicht gefunden: ein aktueller (2026) unabhängiger Blind-Hörtest speziell Deutsch, der Gemini 3.8, Chirp 3 HD, Azure und ElevenLabs zusammen vergleicht. Auch ein deutscher MOS-Wert für diese Kandidaten ist nicht belegt (Zahlen wie „Azure 4,4 / ElevenLabs v3 4,5“ aus einer Sammelseite hatten keine nachprüfbare Methodik und werden nicht übernommen).

## 2. Deutsche Bewertungen

| Quelle | Datum | Inhalt | Wert |
|---|---|---|---|
| c't „Sieben Anbieter von KI-Stimmen für Text-to-Speech“ (Heise-PDF), https://aktionen.heise.de/wp-content/uploads/2024/12/Sieben-Anbieter-von-KI-Stimmen-fuer-Text-to-Speech-im-Vergleich.pdf | Heft 15/2023 (PDF Dez 2024 hochgeladen) | Deutsch klingt oft holpriger als Englisch; Anbieter u. a. ElevenLabs, Murf, Revoicer, Beepbooply | Veraltet (2023), nur Hintergrund. Aus Suchzusammenfassung, PDF nicht geöffnet. |
| KugelAudio „KugelAudio vs. Voxtral vs. ElevenLabs“, https://www.kugelaudio.com/en/articles/kugelaudio-vs-voxtral-vs-elevenlabs-german-tts | 22.08.2026 | Verzichtet ausdrücklich auf eine Qualitätsrangfolge („keine gemeinsamen Blind-Hörtests“). Schlägt Testset für Deutsch vor (Datum, Währung, Telefon, Abkürzungen, Komposita, Deutsch-Englisch-Mischung). Voxtral-Gewichte CC BY-NC 4.0. | **Autor arbeitet bei KugelAudio (Hersteller)**. Quelle für Testset-Design, nicht für Qualität. |
| KugelAudio „German TTS in 2026: A Practical Buyer's Guide“ (Viktor Presber), https://www.kugelaudio.com/en/articles/best-german-dialect-tts-2026 | 18.08.2026 | Nennt typische Fehlerbilder: **Dezimalkomma-Zahlen, englisch geprägte Normalisierung, Kompositagrenzen, lange Nebensätze**. Beispieltests: 3.847,26 €, „22. November“, MESZ. Rät, die erwartete Sprechform vor dem Erzeugen aufzuschreiben. Keine anbieterbezogenen Fehlerergebnisse. | Hersteller-Seite; Fehlerbilder allgemein, nicht pro Anbieter belegt. |
| The Decoder, Gemini 3.8 Flash TTS, https://the-decoder.de/gemini-3-8-flash-tts-google-will-mit-ki-stimmen-hoerbuecher-spiele-und-podcasts-veraendern/ | 2026 (Datum nicht abgerufen) | Eigener Test mit Style-Prompt „genervter Berliner, der Englisch spricht“: Akzent und Intonation überzeugend, aber in beiden Durchläufen hohes Fiepen im Hintergrund, in einem Test wechselte die Stimme gegen Ende. | Fachpresse, kleine Stichprobe, **nicht zu Zahlen/Datum**. Aus Suchzusammenfassung. |
| Sammelratgeber (anyspeech.io, all-ai.de, kopfundstift.de, Mind-Verse) | 2025/2026 | ElevenLabs „am natürlichsten“; Mind-Verse empfiehlt eigenen Testkatalog mit Eigennamen, Abkürzungen, Zahlen, Fremdwörtern. | Überwiegend Affiliate-/Anbieter-nah, keine Blindtests. Nicht als Beleg verwenden. |
| Microsoft-Q&A „text-to-speech numbers normalization rules“, https://learn.microsoft.com/answers/questions/590466/text-to-speech-numbers-normalization-rules.html | 14.–26.10.2021 | Azure las im deutschen Satz 45646 als Zahl, 43609 aber ziffernweise. Microsoft: Entscheidung ist kontextabhängig („smart guesses“), Abhilfe `say-as`. | Alt (2021, Neural-Stimme, Anbieter unbenannt), aber einziger belegter deutscher Zahlenfehler eines Kandidaten. |
| Heise/Golem/t3n/Netzpolitik, Reddit r/LocalLLaMA und r/de | – | **Nichts Belastbares gefunden** (mehrere Suchen, keine Treffer zu Zahlen/Normalisierung deutscher TTS 2025/26). | Lücke. |
| Open-Source-Seite: Kokoro-82M-ONNX-German-Martin (Hugging Face / GitHub), Home-Assistant-„TTS Proxy“ | 2025/26 | Beide zeigen, dass Praktiker deutsche Normalisierung selbst vorschalten (Dezimalzahlen mit Einheit „2,5 kWh“, Datum als Ordinal: „31.10.1994 → einunddreißigster Oktober neunzehnhundertvierundneunzig“). | Praxis-Hinweis, kein Vergleich. |

## 3. Textnormalisierung Deutsch je Kandidat (laut Doku)

Wichtig: Für **keinen** Kandidaten habe ich eine veröffentlichte, deutschsprachige Prüfung gefunden, die Dezimalkomma, Tausenderpunkt, Datum, Uhrzeit, Ordinal, Einheiten, Akronyme und Fremdwörter zeigt. Die Spalte „Fehlerbilder“ enthält nur, was belegt ist. Das Fehlerbild „30,5 als dreißig, fünf gelesen“ ist für **keinen** Anbieter belegt; es ist ein plausibles Risiko (KugelAudio nennt „Dezimalkomma“ allgemein als Fehlerklasse), kein gemessener Befund. Die Studie muss das mit Testtext A hörbar prüfen.

| Kandidat | Normalisierung steuerbar? | Doku-Befund (Quelle, 21.10.2026) | Belegte Fehlerbilder / Hinweise |
|---|---|---|---|
| **Google Chirp 3 HD** (de-DE verfügbar) | Teilweise per SSML (Preview): `<say-as>`, `<sub>`, `<phoneme>`, `<p>`, `<s>`, `<break>`, `<audio>`, `<prosody>`, `<voice>`; Pausen auch per `markup`-Feld (`[pause]`, `[pause short]`, `[pause long]`). Eigene Aussprache per IPA/X-SAMPA. | https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd — **SSML nicht für Streaming-Anfragen**; Normalisierung von Zahlen/Datum auf der Seite **nicht dokumentiert**. Release Notes: SSML für Chirp 3 HD seit 17.10.2025 (https://docs.cloud.google.com/text-to-speech/docs/release-notes). Die allgemeine SSML-Seite (https://docs.cloud.google.com/text-to-speech/docs/ssml) nennt Chirp 3 HD nicht; `say-as` kennt date (format ymd/dmy usw.), time, cardinal, ordinal, characters/verbatim, fraction, unit, currency, telephone. Ob `say-as format="dmy"` bei Chirp 3 HD auf Deutsch zuverlässig wirkt: **unbelegt**. | Forum: Chirp 3 HD zerlegt französische Kontraktionen mit Apostroph („C'est → cé est“), https://discuss.google.dev/t/incorrect-pronunciation-of-french-contractions-with-chirp-3-hd-voices/271804. Forum: Custom Pronunciation (X-SAMPA) ohne hörbare Wirkung gemeldet, https://discuss.ai.google.dev/t/custom-pronunciations-not-working-with-chirp3-hd-voices/106259 (Antwort unbekannt). Apostroph in „Wie geht's?“/„Das war's“ daher gezielt testen. |
| **Gemini-TTS** (3.8 Flash, 3.8 Flash-Lite; Deutsch GA) | Gemini-API: Text wird als **wörtliches Transkript** gelesen; Stil über `speech_metadata.style`; Inline-Tags in spitzen Klammern; **keine** Dokumentation zu Zahlen/Datum, keine Phonem-Markup (https://ai.google.dev/gemini-api/docs/speech-generation). Cloud-TTS-API: Schalter `enable_textnorm` in `AdvancedVoiceOptions`, standardmäßig an, nur für Gemini-TTS (aus dem generierten PHP-Client-Reference, https://docs.cloud.google.cn/php/docs/reference/cloud-text-to-speech/latest/V1.AdvancedVoiceOptions; in der Hauptdoku nicht gegengelesen) und laut Cloud-Seite hilfreich für Datum und Geldbeträge (https://docs.cloud.google.com/text-to-speech/docs/gemini-tts). | Cloud-Seite: Tags wie `[sigh]`, `[short pause]`; „vokalisierte Adjektive“ (`[scared]`) werden **mitgesprochen**; neue Tags können unvorhersehbar wirken; Ausgabe über ~655 s wird abgeschnitten; bei Prompt+Text getrennte Felder (je 4.000 Byte). | Ältere Modelle `gemini-3.1-flash-tts-preview` und `gemini-2.5-pro-preview-tts`: laut Gemini-API-Seite weiterhin nutzbar, `gemini-3.8-flash-lite-tts` „ersetzt“ 3.1-Flash-Preview. Deutsche Zahlen-/Datumsfehler: **keine Berichte gefunden**. |
| **Azure Neural / DragonHD / HD Omni** | **Ja, am umfangreichsten.** `say-as` (cardinal/number, ordinal, number_digit, fraction, date mit format dmy/mdy/ymd…, time hms12/hms24, telephone, currency, unit, address, characters/spell-out, alphanumeric), `sub`, `phoneme`, `lexicon` (PLS, bei HD nur alias). Für alle deutschen Locales nennt die Doku `characters`/`spell-out` plus die übrigen `interpret-as`-Werte. | https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-synthesis-markup-pronunciation (Stand 25.02.2026, aktualisiert 05.06.2026). HD-Seite https://learn.microsoft.com/en-us/azure/ai-services/speech-service/high-definition-voices (aktualisiert 24.09.2026): **DragonHD** unterstützt `say-as`, `sub`, `phoneme`, `break`, `lang`; **Omni** unterstützt `say-as`, `sub`, kein `phoneme`; beide kein `prosody`/`emphasis`. Deutsche DragonHD-Stimmen: `de-DE-Florian`, `de-DE-Seraphina`. Omni: z. B. `de-DE-Conrad:DragonHDOmniLatestNeural`. Parameter `enhancePronunciation=true` für Akronyme, Abkürzungen, Eigennamen. | Die Doku-Beispiele sind englisch; deutsche Wirkung je `interpret-as` **nicht belegt**. Q&A 2021 (siehe Abschnitt 2): Ziffernlesen inkonsistent, Abhilfe `say-as`. `duration` nur Englisch/Spanisch. MAI-Voice-2: **keine Doku gefunden**. Edge-TTS: nicht recherchiert; ob die Bibliothek SSML durchreicht, unbelegt. |
| **ElevenLabs v3 / v4 / Flash v2.5** | `apply_text_normalization` = `on` / `off` / `auto`, plus `language_code` (ISO 639-1) damit deutsche Regeln greifen. Standard: an. Kostet Latenz. | https://elevenlabs.io/docs/help-center/product/core-capabilities/text-to-speech/why-are-numbers-dates-symbols-and-acronyms-not-properly-pronounced-or-spoken-in-the-correct-language und https://elevenlabs.io/docs/overview/capabilities/text-to-speech/best-practices. **Eleven v3 und v4 unterstützen keine SSML-Break-Tags**; v4 nutzt IPA in Schrägstrichen statt XML-Phonem-Tags; Alias-Tags/Wörterbücher für Multilingual v2 empfohlen. Doku rät ausdrücklich, Zahlen/Abkürzungen/Daten **vorab auszuschreiben**, Datumsformat und Uhrzeit explizit anzugeben. | ElevenLabs räumt selbst ein: größere Modelle lesen „$1,000,000“ richtig, **Flash v2.5 „one thousand thousand dollars“**. Deutsches Beispiel in der Hilfe: „11“ könne „Eleven/Once/Elf“ sein, daher `language_code`. |
| **OpenAI gpt-4o-mini-tts** | Kein SSML, keine Normalisierungsdoku. Nur `instructions` (Akzent, Ton, Tempo, Flüstern). | https://developers.openai.com/api/docs/guides/text-to-speech: Deutsch über Whisper-Sprachliste, „Stimmen für Englisch optimiert“. **`gpt-4o-mini-tts`, `tts-1`, `tts-1-hd` werden am 06.01.2027 abgeschaltet**; Migrationsempfehlung `gpt-realtime-2.1-mini` über die Realtime-API. | In der Rangliste nicht geführt; MINT-Bench Deutsch PE nur 1,77 (schwächstes kommerzielles Modell der Tabelle). **Wegen Abschaltdatum als Jarvis-Ersatz zweifelhaft.** |
| **Amazon Polly Generative** (de-DE: Daniel, Lennart, Vicki; de-CH: Sabrina) | `say-as` bei Generative „volle Verfügbarkeit“; `sub` voll; `phoneme` teilweise; `emphasis` nicht. | https://docs.aws.amazon.com/polly/latest/dg/supportedtags.html, https://docs.aws.amazon.com/polly/latest/dg/say-as-tag.html: `characters`, `cardinal`, `ordinal`, `digits`, `fraction`, `unit`, `date` (mit `format`, z. B. `dmy`), `time` (nur als Dauer Minuten/Sekunden!), `address`, `telephone` (de-DE belegt). Stimmen-Liste: https://docs.aws.amazon.com/polly/latest/dg/generative-voices.html (über Suche). | `characters` bei Neural: der Satz wird mit der Standardstimme gesprochen, aber als Neural abgerechnet. `time` ist laut Doku **keine Uhrzeit-Lesung** (1'21"). Uhrzeit 18:47 daher vorab selbst ausschreiben. |
| **Cartesia Sonic 3.x** | Feld `normalization`: `auto` (Standard), `off` oder Sprach-/Ortscode; folgt sonst `language`/`locale`. Pronunciation-Wörterbücher auch bei `off`. Kein SSML. | https://docs.cartesia.ai/build-with-cartesia/tts-models/latest, https://docs.cartesia.ai/build-with-cartesia/capability-guides/advanced-capabilities. Deutsch (`de`) unter 44 Sprachen, Snapshot `sonic-3.6-2026-08-27`; Hersteller: Datum/Zahlen „pro Locale korrekt“. | **Wörter in Großbuchstaben werden standardmäßig Buchstabe für Buchstabe gesprochen** (HRV, USB passt; **WLAN würde „W-L-A-N“ statt „Weh-Lan“**; `off` schaltet das Buchstabieren aller Großbuchstaben-Wörter ab und würde dann auch HRV/USB als Wörter lesen. Besser `normalization` auf `auto`/`de-DE` lassen und WLAN clientseitig zu „Weh-Lan“ umschreiben oder per Aussprache-Wörterbuch lösen). Deutsch-spezifische Regeln undokumentiert. |
| **Inworld TTS-2** | `applyTextNormalization`: `ON` / `OFF` / Standard (Dienst entscheidet je Anfrage); IPA-Inline in Schrägstrichen (englische IPA-Notation); Steuerklammern `[…]` nur bei `inworld-tts-2`, nicht bei `-flash`. | https://docs.inworld.ai/docs/tts/best-practices/generating-speech. Deutsch ausdrücklich nicht behandelt. | Bei „OFF“ muss die App alles ausschreiben. Mehrdeutige Daten und Fachabkürzungen braucht manuelle Hilfe. |
| **Deepgram Aura-2** | Keine Doku zu Zahlen, Daten, SSML. | https://developers.deepgram.com/docs/tts-models: Deutsch mit 7 Stimmen (Elara, Aurelia, Lara, Julius, Fabian, Kara, Viktoria; Modell `aura-2-<name>-de`). Changelog 30.04.2026: neue Aussprache-/Tempo-Steuerung hängt am **englischen** Voice-Pack (https://developers.deepgram.com/changelog/2026/4/30). | In Artificial-Analysis-Liste nicht als Aura-2 geführt. Deutsch-Normalisierung unbelegt. |
| **xAI Grok TTS** | Nur Drittquellen: Inline-Tags `[pause]`, `[laugh]` usw. und Wrap-Tags; verarbeitet geschriebene Zahlen/Währungen/Abkürzungen; bis 15.000 Zeichen; 17–20 Sprachen (Quellen uneins). | https://replicate.com/xai/grok-text-to-speech, https://www.atlascloud.ai/nl/models/xai/tts-v1 (über Suche). Offizielle xAI-Doku: URL lieferte 404. | Deutsch-Normalisierung unbelegt. |
| **Mistral Voxtral TTS 3** | Keine offizielle Doku zu Normalisierung gefunden. | 9 Sprachen inkl. Deutsch (The Decoder, über Suche); Gewichte CC BY-NC 4.0 laut KugelAudio-Artikel (Herstellerkonkurrent). | Unbelegt. |
| **MiniMax Speech** | `text_normalization`/`english_normalization` ist laut Wrapper-Doku (Pipecat, LiveKit) eine Chinesisch/Englisch-Funktion; `language_boost` Wert für Deutsch unbelegt. | https://docs.pipecat.ai/server/services/tts/minimax, https://docs.livekit.io/agents/models/tts/minimax.md (Drittdoku; Original-API-Seite nicht abgerufen). | Für Deutsch vermutlich **keine** Server-Normalisierung (Schluss aus der Doku-Lage, nicht getestet). |
| **Hume Octave 2** | Keine Doku gefunden. Deutsch laut Sekundärquellen unterstützt (11 bzw. 16+ Sprachen, widersprüchlich). | https://www.aiwiki.ai/wiki/hume_octave_2/raw u. a. | Unbelegt. |
| **Murf Falcon 2, Alibaba qwen3-tts-flash, Fish Audio** | Nichts Belastbares zu Deutsch gefunden. | – | Unbelegt. Fish: laut Gradium-Hard-Case schwächste Quote der fünf (49,5 %, Hersteller-Test, nicht deutschspezifisch). |

Zur Frage „abschaltbare/steuerbare Normalisierung“:

| Steuerbar | Wie |
|---|---|
| ElevenLabs | `apply_text_normalization` on/off/auto + `language_code` |
| Cartesia | `normalization` auto/off/Sprachcode |
| Inworld | `applyTextNormalization` ON/OFF/Standard |
| Gemini via Cloud-TTS-API | `enable_textnorm` (nur Gemini-TTS, nur Cloud-API, Quelle: PHP-Referenz) |
| Azure, Google Chirp 3 HD (nicht-Streaming), Polly | SSML `say-as`/`sub`/`phoneme` pro Textstelle; keine globale Abschaltung dokumentiert |
| Gemini-API (ai.google.dev), OpenAI, Deepgram, MiniMax (Deutsch), Voxtral, Grok | keine dokumentierte Steuerung |

## 4. Wörtlich gesprochene Satzzeichen (Testtext B)

Ergebnis: **Kein Anbieter dokumentiert eine zuverlässige „Satzzeichen mitsprechen“-Funktion.** Das ist die wichtigste Aussage dieses Abschnitts.

| Weg | Belegt? | Quelle / Anmerkung |
|---|---|---|
| SSML `say-as ... detail="punctuation"` | Standard-SSML kennt `detail`, definiert aber keine Werte; im Webtreffer nur für Zahlenketten (IBM), nicht für Zeichen wie Komma. Bei Azure ist `detail` als Attribut aufgeführt, „keine Standardwerte“. | https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-synthesis-markup-pronunciation; für Satzzeichen unbelegt. |
| SSML `say-as characters` / `spell-out` | Belegt für Buchstabieren (Azure: alle Locales; Polly: nur Standardstimme bei Neural; Google: characters/verbatim). Ob es „,“ als „Komma“ spricht: **unbelegt**. | Azure-Doku, Polly say-as-Doku, Google SSML. |
| SSML `sub alias="Komma"` um das Zeichen | Technisch dokumentiert für Wörter/Abkürzungen bei Azure (auch DragonHD/Omni), Polly (voll), Google (`sub`, Chirp 3 HD seit 17.10.2025). Für einzelne Satzzeichen **nicht belegt**; XML-Escaping und Satzgrenzen-Effekte unklar. | Quellen wie oben. |
| Stil-Prompt (Gemini `speech_metadata.style`, OpenAI `instructions`) | **Unbelegt.** OpenAI: Anweisungen steuern Akzent, Ton, Tempo, nicht Textinhalt. Gemini: Text wird als wörtliches Transkript gelesen; Inline-Tags nur für Laute/Stil/Pausen. | developers.openai.com-Guide; ai.google.dev-Seite. |
| ElevenLabs v3/v4 | Kein SSML-Break; Alias nur über Wörterbücher (Multilingual v2). Satzzeichen mitsprechen: unbelegt. | ElevenLabs Best Practices. |
| **Clientseitig ersetzen (empfohlen, eigene Ableitung)** | Funktioniert bei jedem Anbieter, weil danach nur normaler Text kommt: `,` → „Komma“, `!` → „Ausrufezeichen“, `?` → „Fragezeichen“, `:` → „Doppelpunkt“, `'` → „Apostroph“. Kein Beleg nötig, da es keine Anbieterfunktion voraussetzt. | Eigene Empfehlung, noch nicht gehört/geprüft. Nachteil: Intonation (Frage steigend) geht dabei verloren. |

Hinweis für die Studie: Für Testtext B reicht daher „Hörprobe, ob die Anweisung wirkt“ nur bei Gemini und OpenAI; sonst ist die Ersatzmethode der einzig verlässliche Weg. Eintrag in den Ergebnissen als „nicht dokumentiert, per Hörprobe prüfen“ führen.

## 5. Vorverarbeitung im Client (bewährtes Muster)

Belege für das Muster: ElevenLabs („Normalize text before it reaches the TTS model, either with LLM instructions or regex preprocessing“, Best Practices, 21.10.2026), Inworld („Bei OFF muss die Anwendung alles ausschreiben“), Practitioner-Projekte (Home-Assistant-TTS-Proxy, Kokoro-Deutsch-ONNX mit Normalisierungsschicht). KugelAudio (Hersteller) empfiehlt Erwartungstexte pro Normalisierungsfall.

| Baustein | Lizenz | Deutsch-Eignung | Android/Kotlin? |
|---|---|---|---|
| **num2words** (Python), https://github.com/savoirfairelinux/num2words | **LGPL** (PyPI-Metadaten, 0.5.14, hochgeladen 17.12.2024) | `de` gelistet; Ordinal-Konvertierung laut README „für manche Sprachen etwas fehlerhaft“; deutsche Besonderheiten (Währung `to='currency'`, Jahr) in der README nicht belegt | Nur Python. Für Jarvis nur sinnvoll, wenn die Vorverarbeitung serverseitig (z. B. Jarvis-Bridge) läuft |
| **NeMo-text-processing** (NVIDIA), https://github.com/NVIDIA/NeMo-text-processing | **Apache 2.0** (README) | Verzeichnis `text_normalization/de` mit `data/`, `taggers/`, `verbalizers/` vorhanden; welche Klassen im Detail (Dezimal, Datum, Uhrzeit, Maß, Geld): **nicht verifiziert** | Nur Python/Pynini, pip nur Linux x86_64; unter Windows/macOS conda nötig. Nicht für Android, nur Server/PC |
| **android.icu.text.RuleBasedNumberFormat** | Teil der Android-Plattform (ICU-Lizenz, Plattform-API) | Zahlenausschreibung (Spellout/Ordinal) mit Locale `de` in ICU grundsätzlich vorhanden. API-Level-Angabe und deutsche Regelnamen: **nicht belegt** (developer.android.com-Seite lieferte 404; die Aussage „seit API 24“ stammt aus Allgemeinwissen, nicht aus dieser Recherche) | Ja, ohne Zusatz-Abhängigkeit; liefert aber nur Zahlen, nicht Datum/Einheit/Abkürzung. ICU4J/ICU-Lizenz: nicht geprüft |
| **german_transliterate**, https://github.com/repodiac/german_transliterate | README nennt **CC BY 4.0**; Code-Lizenzdatei nicht gesehen | Zahlen, Ordinal, Datum (12.10.2019), Uhrzeit (13:15h), Akronyme, Geldbeträge (€, EUR, $), Abkürzungen; Wochentage/Monate laut README „experimentell/fehleranfällig“; 17 Commits, Abhängigkeit num2words | Python |
| **CaroTTS-DE `german_text_preprocessor.py`**, https://huggingface.co/spaces/Warholt/CaroTTS-DE/blob/main/german_text_preprocessor.py | **Keine Lizenzangabe** in der Datei | Kardinalzahlen, Jahre 1000–1999, Datum (DD.MM.YYYY, „D. Mon. YYYY“), Monatsabkürzungen, Dezimalzahlen („Komma“, danach jede Ziffer einzeln). **Keine Einheiten, keine Uhrzeit, keine sonstigen Abkürzungen.** Nur Standardbibliothek (`re`) | Python; als Vorlage lesbar, wegen fehlender Lizenz nicht kopieren |
| **LumenVox TTS1 German normalization** (Regelwerk als Spezifikation), https://lumenvox.capacity.com/article/762008/tts1-german-text-normalization | Dokumentation, kein Code | Regeln: 4,5 → „vier komma fünf“; 1.000 → „eintausend“; 20.3.2011 → „zwanzigste märz zweitausendelf“ (Datum standardmäßig Tag-Monat-Jahr); 13:00 → „dreizehn uhr“; Zahl+Punkt → Ordinal (am Satzende als Punkt); „z.B.“ → „zum beispiel“ (Punkt nach Abkürzung beendet keinen Satz); €5,27 → „fünf euro und siebenundzwanzig cent“. Einheiten und Prozent: nicht behandelt | Gute Checkliste für einen eigenen Kotlin-Normalisierer, besonders die Grenzfälle „Ordinalpunkt vs. Satzende“ und „Abkürzungspunkt vs. Satzende“ |
| Hersteller-eigene Rewrite-Schalter (zur Orientierung) | – | KugelAudio: `normalize` + `language` + `<spell>`; Gradium: `rewrite_rules` (`de` unter den Werten). Beides nicht in der Kandidatenliste | – |
| **PolyNorm** (arXiv 2511.03080), https://arxiv.org/html/2511.03080v1 | wissenschaftlich | Few-Shot-LLM-Normalisierung für TTS, überwiegend englisch | Idee: das Jarvis-LLM liefert gleich die Sprechform |

Empfohlene Dreistufigkeit für Jarvis (eigene Ableitung aus den Belegen, nicht getestet):

1. **LLM-Prompt**: Jarvis-Antworten gleich in Sprechform erzeugen lassen (ElevenLabs rät das ausdrücklich für LLM-gespeiste Agenten).
2. **Deterministischer Kotlin-Normalisierer** vor jedem TTS-Aufruf für die Muster aus Testtext A: Dezimalkomma, Tausenderpunkt, Prozent, Einheiten, Währung, Datum (Ordinalform mit Kasus nach Präposition: „dem 21.“ → „einundzwanzigsten“), Uhrzeit, Abkürzungsliste (ca., z. B., Dr., Nov.), Akronyme (WLAN → „Weh-Lan“-Schreibweise, HRV → „H R V“) und Fremdwörter (Schreibweise nicht ändern; wenn nötig Wörterbuch). Zahlen-Kernroutine über ICU `RuleBasedNumberFormat` plausibel, aber API/Regelname erst prüfen.
3. **Anbieterschalter** passend setzen: ElevenLabs `apply_text_normalization=off` oder `auto` mit `language_code=de`, Cartesia `normalization` (und für WLAN `off`/Wörterbuch), Inworld `applyTextNormalization`, Gemini `enable_textnorm` (Cloud-API). Bei „Aus“ muss Stufe 2 vollständig sein.

Zahlenbeispiele für die Prüfung der Stufe 2 (aus dem Testtext, erwartete Sprechform wie dort definiert): 30,5 ms → „dreißig Komma fünf Millisekunden“; 1.249,99 € → „eintausendzweihundertneunundvierzig Euro neunundneunzig“; 21.10.2026 → „einundzwanzigsten Zehnten zweitausendsechsundzwanzig“; 3. Nov. → „dritten November“; 18:47 → „achtzehn Uhr siebenundvierzig“.

## Lücken und Vorbehalte

- **Nicht recherchiert** (im Auftrag genannt, in dieser Sitzung nicht gesucht): Labelbox, Podonos-Deutsch-Ranglisten, Hersteller-Benchmarks von Inworld und ElevenLabs. Der Podonos-Blogvergleich Gemini 2.5 vs. ElevenLabs wurde nur aus einer Suchzusammenfassung zitiert. Das ist „nicht gesucht“, nicht „nichts gefunden“.

- Keine deutschsprachige Rangliste, die Gemini 3.8, Chirp 3 HD, Azure und OpenAI zugleich bewertet. Die deutsche Artificial-Analysis-Tabelle ließ sich nicht abrufen (im Browser nachsehen).
- Hugging Face TTS Arena V2: Tabelle nicht auslesbar.
- Die einzigen deutschspezifischen Zahlen sind: MINT-Bench (alte Modelle), Gradium-WER (Hersteller, Verständlichkeit) und die zweitquellige Meldung zu Cartesia/Inworld auf Platz 1/2 der deutschen Controlled Arena.
- Aussagen zu Edge-TTS, MAI-Voice-2, Hume, Murf, Qwen, Fish, Voxtral: keine eigene Doku zu deutscher Normalisierung gefunden.
- WebFetch lieferte für einige Seiten (OpenAI-Guide, Cartesia, Inworld, xAI) Zusammenfassungen eines kleinen Modells; Wortlaute wurden nicht unverändert zitiert.

BEST-PRACTICES-KANDIDATEN:
- Deutsche TTS nie mit englischen Ranglisten auswählen: Artificial-Analysis-Gesamt-ELO ist nur englische Akzente; für Deutsch die Controlled-Voice-Arena mit Sprache „German“ benutzen.
- Vor jedem TTS-Aufruf eine deutsche Textnormalisierung im Client (Dezimalkomma, Tausenderpunkt, Prozent, Einheiten, Datum mit Kasus, Uhrzeit, Abkürzungen, Akronyme); Anbieter-Normalisierung (ElevenLabs `apply_text_normalization` mit `language_code=de`, Cartesia `normalization`, Inworld `applyTextNormalization`) bewusst setzen statt auf Auto zu vertrauen.
- Beim LLM-gespeisten Assistenten die Antwort schon in Sprechform erzeugen lassen (Zahlen, Daten, Abkürzungen ausgeschrieben).
- Für ein TTS-Testset die erwartete Sprechform je Normalisierungsfall vorab aufschreiben, damit Synthesefehler und Lesart-Meinungsverschiedenheiten getrennt werden.
- Bei SSML-Nutzung mit Google Chirp 3 HD: SSML ist Preview und nur für nicht-gestreamte Anfragen; bei Streaming Pausen über das `markup`-Feld.
- Acronym-Verhalten pro Anbieter prüfen: Cartesia buchstabiert Großschreibung standardmäßig (WLAN als Akronym-Wort: `normalization` auf auto/de-DE lassen und clientseitig zu „Weh-Lan“ umschreiben oder Wörterbuch; `off` würde auch HRV/USB als Wörter lesen).
- Lizenz-Check bei Normalisierungsbibliotheken: num2words ist LGPL, NeMo-text-processing Apache 2.0, german_transliterate unklar (CC BY 4.0 für Daten, Code unklar).

BUG-KANDIDATEN:
- OpenAI `gpt-4o-mini-tts`, `tts-1`, `tts-1-hd` werden am 06.01.2027 abgeschaltet (Migration auf `gpt-realtime-2.1-mini`); nicht als langfristiger Jarvis-Ersatz einplanen.
- Google Chirp 3 HD: SSML wird bei Streaming-Anfragen nicht unterstützt (Preview); Custom Pronunciations (X-SAMPA) laut Forum teils ohne Wirkung; Apostroph-Aussprache (Französisch) fehlerhaft gemeldet, für „geht's/war's“ deutsch prüfen.
- Gemini-TTS: vokalisierte Adjektiv-Tags (z. B. `[scared]`) werden mitgesprochen; Bühnenanweisungen im `text`-Feld werden vorgelesen, gehören in `speech_metadata`; Ausgabe über ca. 655 s wird abgeschnitten; The Decoder meldete hohes Fiepen und Stimmenwechsel bei einem Gemini-3.8-Test.
- Amazon Polly Neural: `say-as characters` schaltet den Satz auf die Standardstimme um, wird aber als Neural abgerechnet; Polly-`say-as time` liest Dauer (Minuten/Sekunden), keine Uhrzeit.
- Azure Neural (2021-Q&A): deutsche Fünfstellige Zahlen teils als Zahl, teils ziffernweise gelesen; Abhilfe `say-as`.
- ElevenLabs Flash v2.5 liest große Zahlen schlechter („one thousand thousand dollars“); Eleven v3/v4 unterstützen keine SSML-Break-Tags; Normalisierung kostet Latenz.
- Cartesia: GROSSBUCHSTABEN-Wörter werden standardmäßig buchstabiert (WLAN falsch für „Weh-Lan“; `normalization: off` ist keine Lösung, weil dann auch HRV/USB nicht mehr buchstabiert werden).
- Rangliste-Fallen: Artificial Analysis nennt „Chirp 3: HD“ Rang 60, „gpt-4o-mini-tts“ und „Deepgram Aura-2“ fehlen; Zweitberichte zu Rängen widersprechen sich (Sonic 3.6 „acht von neun“ vs. sieben genannte Sprachen).
