# R8 — Unabhängige Ranglisten und Benchmarks für deutsche Spracherkennung (Stand 21.10.2026)

Auftrag: Gibt es belastbare, unabhängige Zahlen, dass ein Modell Deutsch besser versteht als Groq `whisper-large-v3-turbo` (Basislinie "Turbo")? Recherchiert am 21.10.2026. Jede Zahl trägt Quelle und Herkunft. Herkunft: **unabhängig** = vom Leaderboard-Betreiber oder von Dritten gemessen. **Hersteller** = vom Anbieter selbst veröffentlicht. **Unbelegt** = nur über Suchtreffer-Ausschnitt oder widersprüchlich, Seite nicht prüfbar.

Kurzbefund vorab:
1. Die einzige Rangliste mit sauberen, unabhängig gemessenen Deutsch-Zahlen für viele Modelle ist die **Hugging-Face-Open-ASR-Leaderboard-Multilingual-Tabelle** (Deutsch: FLEURS und Common Voice). Alle Zahlen unten in Abschnitt 1 stammen direkt aus der Rohdatei vom 19.10.2026.
2. Das **Artificial-Analysis-Leaderboard ist rein englisch** und taugt für Deutsch nicht (Abschnitt 2).
3. Zu Eigennamen, Zahlen und Anglizismen im Deutschen gibt es **keinen** unabhängigen Modellvergleich mit Zahlen (Abschnitt 5).

---

## 1. Hugging Face Open ASR Leaderboard, Multilingual-Tab, Deutsch

**Quelle (Rohdaten):** https://huggingface.co/datasets/hf-audio/multilingual_evals/blob/main/multilingual_de.csv, Commit `3589b3c077cb16c935e52bdd15c7fb0a3f175f7a` vom 19.10.2026. Dieser Commit wird vom Leaderboard-Code geladen (https://huggingface.co/spaces/hf-audio/open_asr_leaderboard, `init.py`). Der Space ist laut README "Open ASR Leaderboard", Paper: https://arxiv.org/abs/2510.06961 (v4, 30.03.2026).

**Herkunft: unabhängig.** Alle Ergebniscommits stammen vom HF-Maintainer `bezzam` (Eric Bezzam), nicht von den Herstellern. Beweis: Commit-Liste https://huggingface.co/api/datasets/hf-audio/multilingual_evals/commits/main (alle Autoren `bezzam`). Das gilt auch für die Proprietären (Azure, Scribe v2, Universal, gpt-transcribe, Soniox, Speechmatics).

**Datensätze (Deutsch):** FLEURS (vorgelesene Wikipedia-Sätze, ca. 2-3,5 h) und Mozilla Common Voice, "MCV" (vorgelesene Crowd-Sätze, Handy-/Headset-Mikrofone, ca. 5-23 h). Quelle: `constants.py` im Space. Beides ist **Read Speech**, kein spontanes Diktat. Im Paper steht für den Multilingual-Track statt MCV auch CoVoST-2 (arXiv 2510.06961v4). Die ältere Datei `multilingual_latest.csv` nutzt noch `de_covost`/`de_fleurs` (Turbo dort: 8,64 / 3,78; large-v3: 5,25 / 3,27). Das ist das alte Format, ab hier nicht mehr verwendet. Die Aussage bleibt dieselbe: Turbo ist auf dem Crowd-Satz-Set rund 3,5 Punkte schlechter als large-v3.

**Normalisierung (wichtig für Deutsch):** Der Code für nicht-englische Texte ist `BasicMultilingualTextNormalizer` (https://github.com/huggingface/open_asr_leaderboard/blob/main/normalizer/normalizer.py). Er macht Kleinschreibung, entfernt Klammerinhalte und Satzzeichen **und entfernt Diakritika** (NFKD, Kategorie Mn raus, `ß` wird `ss`). Das habe ich im Quelltext geprüft. Folgen: (a) Zeichensetzung und Groß-/Kleinschreibung zählen **nicht**, (b) **Umlaute sind unsichtbar** (ä gleich a, ü gleich u), (c) **es gibt keine Zahlen-Normalisierung** für Deutsch (die `EnglishNumberNormalizer` ist englisch-only). "20" gegen "zwanzig" zählt also als Fehler, sofern es der Referenztext hergibt. Dass dieser Normalisierer für den Multilingual-Track verwendet wird, bestätigt das NVIDIA-Canary-Paper (https://arxiv.org/html/2509.14128: "multilingual normalizer from the Hugging Face Open ASR Leaderboard ... lowercases, removes punctuation, ... strips diacritics"). Die ausdrückliche Zuordnung im Leaderboard-Code ist von mir nicht gesondert nachgeprüft.

**Rohdaten Deutsch (alle 41 Zeilen, sortiert nach Mittel aus FLEURS und MCV; WER in Prozent, kleiner ist besser):**

| Rang | Modell | FLEURS | MCV | Mittel |
|---|---|---|---|---|
| 1 | microsoft/azure-speech-06-2026 | 1,93 | 1,88 | 1,90 |
| 2 | microsoft/azure-speech-07-2026 | 1,83 | 2,01 | 1,92 |
| 3 | elevenlabs/scribe_v2 | 2,30 | 2,19 | 2,25 |
| 4 | assemblyai/universal-3-5-pro | 2,17 | 2,69 | 2,43 |
| 5 | sophea/asr-k1 (preview) | 2,74 | 2,33 | 2,54 |
| 6 | assemblyai/universal-3-pro | 2,42 | 2,76 | 2,59 |
| 7 | sprag/symphony | 2,41 | 3,02 | 2,71 |
| 8 | speechmatics/enhanced | 3,56 | 1,99 | 2,77 |
| 9 | reson8/resonant-1 (und -flash, identisch) | 2,56 | 3,01 | 2,79 |
| 11 | openai/gpt-transcribe | 2,34 | 3,34 | 2,84 |
| 12 | mistralai/Voxtral-Small-24B-2507 | 2,61 | 3,19 | 2,90 |
| 13 | TheStageAI/thewhisper-large-v3-turbo (optimierter Turbo, nicht bei Groq) | 2,91 | 3,17 | 3,04 |
| 14 | CohereLabs/cohere-transcribe-03-2026 | 3,33 | 2,87 | 3,10 |
| 15 | modulate/multilingual | 4,02 | 2,87 | 3,44 |
| 16 | soniox/stt-async-v5 | 2,66 | 4,62 | 3,64 |
| 17 | HojoAI/Hojo-ASR-Multi-V1 | 4,03 | 3,81 | 3,92 |
| 18 | Qwen/Qwen3-ASR-1.7B-hf | 3,35 | 4,60 | 3,97 |
| 19 | **openai/whisper-large-v3** | 3,20 | 4,79 | 4,00 |
| 20 | nvidia/parakeet-tdt-0.6b-v3 (fast-gpu-asr) | 4,09 | 3,95 | 4,02 |
| 21 | nvidia/canary-1b-v2 | 3,43 | 4,69 | 4,06 |
| 22 | nvidia/parakeet-tdt-0.6b-v3 | 4,16 | 4,07 | 4,12 |
| 23 | microsoft/Phi-4-multimodal-instruct | 3,99 | 4,25 | 4,12 |
| 24 | ibm-granite/granite-speech-4.1-2b-nar | 4,87 | 3,78 | 4,33 |
| 25 | meta/muse-voice-transcribe | 3,35 | 5,30 | 4,33 |
| 26 | facebook/omniASR-LLM-7B-v2 | 3,72 | 5,06 | 4,39 |
| 27 | mistralai/Voxtral-Mini-3B-2507 | 3,64 | 5,29 | 4,46 |
| 28 | facebook/omniASR-LLM-3B-v2 | 4,63 | 6,48 | 5,55 |
| 29 | facebook/omniASR-CTC-7B-v2 | 4,89 | 7,01 | 5,95 |
| **30** | **openai/whisper-large-v3-turbo (BASISLINIE)** | **3,67** | **8,58** | **6,12** |
| 31 | facebook/omniASR-CTC-3B-v2 | 5,04 | 7,30 | 6,17 |
| 32 | facebook/omniASR-LLM-1B-v2 | 5,92 | 6,60 | 6,26 |
| 33 | mistralai/Voxtral-Mini-4B-Realtime-2602 | 4,87 | 7,81 | 6,34 |
| 34 | Qwen/Qwen3-ASR-0.6B-hf | 5,41 | 7,97 | 6,69 |
| 35 | google/gemma-4-E4B-it | 5,75 | 9,49 | 7,62 |
| 36 | OpenMOSS-Team/MOSS-Transcribe-Diarize | 6,26 | 9,78 | 8,02 |
| 37 | facebook/omniASR-CTC-1B-v2 | 7,20 | 9,64 | 8,42 |
| 38 | nvidia/nemotron-3.5-asr-streaming-0.6b | 8,46 | 9,46 | 8,96 |
| 39 | facebook/omniASR-LLM-300M-v2 | 7,78 | 10,31 | 9,04 |
| 40 | microsoft/VibeVoice-ASR-HF | 7,44 | 20,97 | 14,21 |
| 41 | facebook/omniASR-CTC-300M-v2 | 14,06 | 18,89 | 16,48 |

(Rang 10 ist die zweite Resonant-Zeile; "Mittel" habe ich selbst berechnet, das Leaderboard rechnet im Deutsch-Filter ähnlich als Makro-Durchschnitt, laut Changelog vom 24.07.2026.)

**Was in der Tabelle fehlt:** Es gibt **keine** Zeilen für Deepgram Nova-3, Google Chirp 3 / Gemini 3.5 Transcribe, Gladia Solaria, **gpt-4o-transcribe** (Ergebnisse wurden am 02.10.2026 als PR #61 eingecheckt, in der Deutsch-Datei ist aber nur `openai/gpt-transcribe` enthalten) und keine Groq-gehostete Variante. Whisper large-v3 und Turbo sind die **originalen HF-Checkpoints**, nicht die Groq-API. Groq-Abweichungen sind damit nicht gemessen (siehe Abschnitt 4, Casablanca: gleiche Modelle bei verschiedenen Hostern bis 6 Punkte Unterschied).

**Auffälligkeit (zentral für Frank):** Turbo ist auf FLEURS nur mäßig schlechter als large-v3 (3,67 gegen 3,20), auf Common Voice aber stark schlechter (8,58 gegen 4,79). Das passt zur Vermutung, dass der auf vier Decoder-Schichten gekürzte Turbo bei Deutsch und unsauberem Mikrofon überproportional verliert. Weitere Belege in Abschnitt 4 (primeLine-Modellkarte: Turbo 3,649 gegen large-v3 3,279 gemittelt; Tuda-De 8,30 gegen 7,88).

**Paper-Tabelle (Gegencheck):** arXiv 2510.06961v4, Tabelle 4 (Stand 27.03.2026, Auszug): Scribe v2 2,27; Universal-3 Pro 2,34; Speechmatics Enhanced 2,84; Voxtral Small 3,01; Cohere Transcribe 3,84; Phi-4 3,96; Canary-1B-v2 4,10; Qwen3-ASR-1.7B 4,12; Parakeet v3 4,20; **Whisper large-v3 4,26**; Omnilingual 4,55 bis 6,05. Turbo steht nicht in diesem Auszug. Reihenfolge deckt sich mit dem Live-CSV. Quelle: https://arxiv.org/html/2510.06961v4.

---

## 2. Artificial Analysis Speech-to-Text Leaderboard (AA-WER)

**Quellen:** https://artificialanalysis.ai/speech-to-text/batch (Tabelle, 62 Modelle, abgerufen 21.10.2026 über die deutsche Seitenversion https://artificialanalysis.ai/de/speech-to-text/non-streaming) und Methodik https://artificialanalysis.ai/methodology/speech-to-text.

**Ergebnis: keine Deutsch-Auswertung.** AA-WER v2 = dauergewichtetes Mittel aus AA-AgentTalk (50 %, proprietär, Voice-Agent-Sprache, 469 Samples), VoxPopuli-Cleaned-AA (25 %, laut Methodikseite "Subset: English") und Earnings22-Cleaned-AA (25 %). Normalisiert wird mit dem Whisper-Normalizer plus AA-eigenen **englischen** Regeln (US/UK-Schreibweisen, englische Formate). Alle Hörbeispiele sind englisch. Eine Sprachwahl oder Pro-Sprache-Auswertung gibt es nicht. Die "Deutsch"-Auswahl auf der Seite ist nur die Seitensprache.

**Was trotzdem brauchbar ist (nur Preis und Geschwindigkeit, WER ist englisch):**

| Modell | AA-WER (englisch) | Preis je 1.000 min | Geschwindigkeit (x Echtzeit) |
|---|---|---|---|
| Whisper Large v3 Turbo (Groq) | 4,6 | 0,67 USD | 200,8 |
| Whisper Large v3 (fal.ai) | 4,1 | 1,15 USD | 95,5 |
| Whisper Large v3 (Together AI) | 4,5 | 1,50 USD | 294,0 |
| Whisper Large v3 (Replicate) | 10,1 | 4,23 USD | 2,8 |
| GPT Transcribe (OpenAI) | 3,3 | 4,50 USD | 56,1 |
| GPT-4o Transcribe | 4,0 | 6,00 USD | 37,8 |
| Scribe v2 (ElevenLabs) | 2,2 | 3,67 USD | 63,4 |
| Voxtral Small Transcribe 3 (Mistral) | 2,1 | 4,00 USD | 67,8 |
| Voxtral Mini Transcribe 3 | 2,2 | 3,50 USD | 72,4 |
| MAI-Transcribe-2 (Microsoft AI) | 2,0 | 1,67 USD | 311,9 |
| Universal-3.5 Pro (AssemblyAI) | 3,0 | 3,50 USD | 105,5 |
| Gemini 3.5 Transcribe (Google) | 2,6 | 5,00 USD | 89,5 |
| Chirp 3 (Google) | 4,3 | 16,00 USD | 28,4 |
| Nova-3 (Deepgram) | 5,2 | 4,30 USD | 586,6 |
| Soniox v5 Async | 3,8 | 1,66 USD | 42,3 |
| Speechmatics Enhanced | 4,0 | 12,50 USD | 69,7 |
| Parakeet TDT 0.6B V3 (Together) | 4,5 | 1,50 USD | 567,1 |
| Fun-Realtime-ASR-preview (Alibaba) | 1,7 | 0,00 | 20,2 |
| Grok Voice Transcribe 2.0 (SpaceXAI) | 2,3 | 1,67 USD | 186,2 |

Herkunft: unabhängig (AA misst selbst), aber **englisch**. Die Reihenfolge der englischen Liste sagt über Deutsch nichts verlässlich. Beispiel: Deepgram Nova-3 steht auf AA mittig (5,2), hat aber in Abschnitt 4 ein anderes Bild.

**Erkenntnis aus dem Vergleich:** Gleiches Modell, anderer Hoster: Whisper Large v3 liegt bei Replicate 10,1, bei fal.ai 4,1 (Differenz 6 Punkte), siehe auch Casablanca in Abschnitt 4. Der Hoster zählt.

Die Preis-Spalte zeigt zudem, dass Groq-Turbo (0,67 USD/1.000 min) fast unschlagbar günstig ist. Kostenlose Kontingente stehen in den Berichten der anderen Researcher.

---

## 3. Herstellerangaben mit Deutsch-Zahlen und ihre Vergleichbarkeit

Alle Zahlen dieses Abschnitts sind **Herstellerangaben**, soweit nicht anders vermerkt. Sie sind untereinander und gegenüber Abschnitt 1 **nicht vergleichbar**.

| Anbieter | Aussage / Zahl | Datensatz | Quelle | Anmerkung |
|---|---|---|---|---|
| ElevenLabs | Deutsch FLEURS: Scribe(v1) 2,7 %; Gemini Flash 2: 3,9 %; Whisper large-v3: 4,5 %; Deepgram Nova 2: 9,1 %. Fließtext der Seite nennt 3,1 % FLEURS und 5,5 % Common Voice | FLEURS, Common Voice | https://elevenlabs.io/speech-to-text/german | Tabelle (2,7) und Text (3,1) widersprechen sich. Normalisierung nicht beschrieben. Version v1, nicht v2. |
| Mistral | Voxtral Mini Transcribe V2: "ca. 4 % WER" über die 10 häufigsten FLEURS-Sprachen. Keine Deutsch-Zahl auf der Seite | FLEURS | https://mistral.ai/news/voxtral-transcribe-2 | Vergleich mit GPT-4o mini, Gemini 2.5 Flash, Universal, Nova nur als Aussage, Werte im Diagramm. Kontext-Bias bis 100 Begriffe, nur für Englisch optimiert, andere Sprachen "experimentell". |
| Mistral (Paper) | FLEURS/CV Deutsch siehe Tabelle Abschnitt 6 (Whisper-v3 5,46; Voxtral Small 3,38; Scribe 4,78 ...) | FLEURS, Common Voice | https://arxiv.org/html/2507.13264 (Voxtral-Paper), https://arxiv.org/pdf/2602.11298 (Voxtral Realtime, Tabellen 7 und 8) | Vom Hersteller selbst gerechnet, Normalisierung nicht beschrieben. |
| AssemblyAI | Deutsch FLEURS: Universal-3.5 Pro 4,88 %, Universal-2 6,22 % (Stand Januar 2026). Fehlt: Entity-Fehlerraten für Deutsch (nur Gesamtwerte, englisch ausgerichtet: Namen/Orte 8,6 bis 17 % Missed-Entity-Rate) | FLEURS | https://www.assemblyai.com/docs/pre-recorded-audio/benchmarks.md | **Gleiches Modell steht bei HF mit 2,17 FLEURS.** Unterschied rund 2,7 Punkte allein durch Normalisierung ("standard text normalization", nicht beschrieben). Beleg für Nichtvergleichbarkeit. |
| Deepgram | Deutsch, 26 h echte Anrufe/Meetings/Drive-Through plus kuratierte Aufnahmen, Batch: Nova-3 Multi **8,8 %** (an anderer Stelle 10,5 %), ElevenLabs 11,4 %, Whisper 19,9 % (Produktion) bzw. 8,4 % (kuratiert). Streaming: Nova-3 12,0 %, Soniox 14,5 %, AssemblyAI 20,4 %, ElevenLabs 22,8 % | eigener Satz, ohne FLEURS/CV, Referenz von Drittteam | https://deepgram.com/learn/german-benchmarks (05./25.03.2026) | Eigener Satz nicht öffentlich, Normalisierung nicht beschrieben, 8,8 gegen 10,5 intern widersprüchlich. Zeigt aber: **echte Telefon-/Meeting-Audios haben 2- bis 3-fach höhere WER als Lesesprache.** |
| Soniox | "Deutsch 1,25 % gegen OpenAI 3,24 %, Deepgram 1,71 %, AssemblyAI 1,74 %" (YouTube-Studie, 60 Sprachen, 2026) und andernorts "7 % / 10,4 % / 12,5 % / 16,1 %" ohne Datum | YouTube, undokumentiert | Suchtreffer-Ausschnitt zu https://soniox.com/compare/soniox-vs-openai/german und .../soniox-vs-deepgram/german. **Beim Abruf enthielten die Seiten die Zahlen nicht** | **Unbelegt.** Widersprüchlich, keine Methodik. |
| Soniox / Pipecat | semantische WER (ignoriert Zeichensetzung, Groß-/Kleinschreibung, Zahlenformate, Füllwörter), 1.000 Samples, August 2026: Azure 1,21; Soniox v5 1,34; Speechmatics 1,40; Deepgram 1,71; AssemblyAI 1,74; Gemini 3.5 Transcribe Live 2,24; gpt-realtime-whisper 2,92; Scribe v2 Realtime 3,16; Voxtral Realtime 4,44 | Pipecat-Smart-Turn-Daten | https://soniox.com/benchmarks | Keine Deutsch-Auswertung, Streaming-Modelle, Ground Truth von Gemini erzeugt. Nur Randhinweis. |
| Gladia | Solaria-3 gegenüber Solaria-1 Deutsch: -3 % relativ (echte Kundenaudios), -13 % (Common Voice 24). Kein Absolutwert | Gladia-intern, CV 24 | https://www.gladia.io/solaria-3 | Keine Konkurrenzwerte für Deutsch. Auf MLS +36 % schlechter (8,0 gegen 5,9). |
| Google | Gemini 3.5 Transcribe: FLEURS (Sprachmittel) 5,04 % (Batch) / 5,50 % (Streaming), Chirp 3: 7,32 %. Deutsch nicht ausgewiesen | FLEURS Sprachmix | https://eesel.ai/blog/gemini-3-5-transcribe-review und https://borncity.com/news/gemini-3-5-transcribe-googles-neue-ki-transkribiert-70-schneller/ (Sekundärquellen, Google-Primärseite nicht abgerufen) | Deutsch-Wert **unbelegt**. Hinweis: Der Gemini-1-Report räumt FLEURS-Trainingsdaten ein (Kontamination möglich, https://arxiv.org/pdf/2312.11805). |
| OpenAI | gpt-4o-transcribe "besser als Whisper v2/v3 auf FLEURS", kein Deutsch-Wert. Blog nicht abrufbar (HTTP 403) | FLEURS | https://community.openai.com/t/reproducing-gpt-4o-transcribe-fleurs-results/1151187 | Nutzer reproduziert englisch 2,93 gegen berichtet 2,46. Abweichung 0,4 bis 0,6 Punkte, vermutlich Normalisierung. **Deutsch: keine Zahl gefunden.** |
| OpenAI | gpt-transcribe (Juli 2026): AA-WER 3,31 %, 0,0045 USD/min; "mehrere Eingabesprachen", Text-Kontext und Keywords möglich | AA (englisch) | https://the-decoder.com/gpt-transcribe-improves-on-its-predecessor-but-cant-catch-elevenlabs-google-or-mistral-on-error-rates/ | Deutsch nicht erwähnt. Die HF-Messung (2,34/3,34) ist die einzige Deutsch-Zahl. |
| Alibaba (Qwen) | Qwen3-ASR-Flash-1208 Deutsch FLEURS 3,03 / CV 3,53; 1,7B 3,92 / 5,85; 0,6B 6,48 / 9,44 | FLEURS, Common Voice | https://arxiv.org/html/2601.21337 (Tabelle) | Hersteller-Paper. Whisper large-v3 ohne Deutsch-Wert (nur Sprachmittel: CV 10,77, FLEURS 5,27). Normalisierung nicht beschrieben. |
| NVIDIA | Parakeet-TDT-0.6B-v3 Deutsch FLEURS 5,04, CoVoST 4,84 (Modellkarte). Canary-1B-v2 FLEURS 4,40, Whisper large-v3 4,30 laut Suchtreffer zum Paper (nicht gegengeprüft) | FLEURS, CoVoST | https://wz-it.com/en/blog/local-speech-to-text-gdpr/ (Zitat Modellkarte), https://arxiv.org/pdf/2509.14128 | Paper-Appendix nicht abgerufen. Canary-Whisper-Wert **unbelegt**. Im Paper-Mittel über sechs "Common"-Sprachen: Canary 5,2 / Parakeet 5,3 / Whisper-v3 5,8. |

**Vergleichbarkeit, harte Belege:** Dasselbe Modell Whisper large-v3 hat auf Deutsch-FLEURS je nach Quelle diese Werte:

| Quelle | WER FLEURS de | Normalisierung |
|---|---|---|
| HF Open ASR (unabhängig) | 3,20 | Multilingual-Normalizer, Diakritika raus |
| handy.computer, Q8-Quantisierung (Projektseite) | 4,13 | unbekannt |
| NVIDIA-Canary-Paper (Suchtreffer, unbelegt) | 4,30 | HF-Normalizer |
| ElevenLabs-Seite (Hersteller) | 4,5 | nicht beschrieben |
| Voxtral-Paper (Mistral) | 5,46 | nicht beschrieben |

Spanne 2,3 Prozentpunkte für **dasselbe** Modell. Genauso AssemblyAI Universal-3.5 Pro: 2,17 (HF) gegen 4,88 (eigene Seite). **Regel: nur Rangfolgen innerhalb einer Quelle vergleichen, nie Rohwerte über Quellen hinweg.**

---

## 4. Unabhängige Tests, Forschung, Blogs und Gegenchecks 2025 bis 2026

### 4.1 Reproduzierbare Dritt-Messungen auf FLEURS-de (Gegencheck zu HF)

**handy.computer (transcribe.cpp), FLEURS-de, Q8_0** — https://models.handy.computer/languages/de (Projektseite, kein Datum, kein Hersteller der Modelle, Herkunft: unabhängig):
Voxtral-Small-24B 3,29; **whisper-large-v3 4,13**; Voxtral-Mini-4B-Realtime 4,24; Qwen3-ASR-1.7B 4,25; canary-1b-v2 4,46; whisper-large-v2 4,53; **whisper-large-v3-turbo 4,54**; parakeet-ultra 4,58; Voxtral-Mini-3B 4,71; cohere-transcribe 5,06; parakeet-tdt-0.6b-v3 5,24; Qwen3-ASR-0.6B 6,80; whisper-medium 6,23; whisper-small 9,86.
Befund: Auf FLEURS allein liegt Turbo (4,54) nur 0,41 hinter large-v3 (4,13). **Auf FLEURS ist der Abstand klein; auf Common Voice groß (HF).** Quantisierung Q8_0 verfälscht leicht.

**whispernotes.app (Hersteller einer Mac-App, daher nicht neutral), FLEURS-de, 150 Sätze, M5-MacBook** — https://whispernotes.app/blog/qwen3-asr-vs-whisper: Qwen3-ASR-1.7B **2,85** gegen Whisper large-v3 Turbo **4,05**. Keine Normalisierungsangabe. Eigenmessung, aber gleiche Clips für beide Modelle, Bootstrap-Fehlerbalken.

### 4.2 Whisper-Familie, Deutsch (Turbo gegen large-v3)

**primeLine / Florian Zimmermeister (Modellkarte, Eigenangabe, Daten öffentlich)** — https://huggingface.co/primeline/whisper-large-v3-turbo-german (Evaluationstabelle, WER %):

| Datensatz | Turbo (OpenAI) | large-v3 (OpenAI) | primeline large-v3 German | CrisperWhisper large | primeline Turbo German |
|---|---|---|---|---|---|
| Tuda-De | 8,300 | 7,884 | 7,711 | 5,148 | 6,441 |
| Common Voice 19.0 | 3,849 | 3,484 | 3,215 | 1,927 | 3,200 |
| MLS | 3,203 | 2,832 | 2,129 | 2,815 | 2,070 |
| Gesamt | 3,649 | 3,279 | 2,734 | 2,662 | 2,628 |

Turbo ist hier durchgehend schlechter als large-v3 (Gesamt +11 % relativ). Interessant: Deutsch-Fine-Tunes (CrisperWhisper, primeline) sind deutlich besser, aber **nicht bei Groq hosted** (unbelegt, nicht geprüft).

**Hoster-Streuung:** Casablanca (Forschungsgruppe, Österreich) — https://research.casablanca.at/post/why-your-asr-benchmark-isnt-telling-you-the-whole-truth: Whisper large-v3 bei Together 4,5, fal.ai 4,1, **Replicate 10,1** (AA-Daten, englisch); Entfernen von Zeichensetzung senkt die WER bis zu **5 Punkte**; gleiche Modelle bei verschiedenen Hostern weichen bis zu ca. 6 Punkte ab (Vor-/Nachverarbeitung der Hoster undokumentiert). Deutsche VoxForge-1.000-Sample-Spalte (EVADE) ist in der Seite verstümmelt (Scribe v2 Deutsch 10,1 gleich dem Replicate-Wert, wahrscheinlich Kopierfehler); ich verwende sie nicht. Gut verwertbar: Nova-3 5,2 (Deutsch), Voxtral Mini (DeepInfra) 3,8 laut Seite, aber Tabelle unleserlich, daher **unbelegt**.

**VexaScribe** (https://vexascribe.com/how-accurate-is-whisper) hat laut eigener Seite ein Benchmark vom Juli 2026 zurückgezogen; das aktuelle nutzt Replicate large-v2 und Groq Turbo gemischt ohne Einzelwerte. **Nicht verwendet.**

### 4.3 Wissenschaftliche Arbeiten zu deutschem ASR

- **Benchmarking von ASR-Modellen im deutschen medizinischen Kontext** (Hochschule Pforzheim/XPACE, 23.01.2026): https://arxiv.org/abs/2601.19945. 29 Modelle, Datensatz "Med-De-Anamnese" (simulierte Arzt-Patienten-Gespräche, YouTube). Volle Ergebnisse nur für sechs Modelle (WER-Mittel): **AssemblyAI Universal 3,0 %**, Voxtral Small 7,1 %, Voxtral Mini 8,5 %, Whisper Large-v3 (Distilled) 12,6 %, Deepgram Nova-2 14,3 %, WhisperX-Base 21,5 %. Fachbegriffe und Dialekt als Fehlertreiber, aber **ohne Zahlen je Begriff**. Umlaut-/Zahlenbehandlung: Referenz hat Zahlen ausgeschrieben; Auswertung kleingeschrieben, ohne Zeichensetzung. Herkunft: unabhängig. Hinweis: Das Whisper-Modell ist "Distilled", nicht Turbo. Quelle: https://arxiv.org/html/2601.19945v1.
- **Qwen3-ASR Technical Report**: Hersteller, siehe Abschnitt 3.
- **OpenWER** (Stuttgart Media University / Tübingen, 19.06.2026): https://arxiv.org/abs/2606.21237. Open-Source-WER mit sprachspezifischer Normalisierung und **Erkennung von Komposita**. Bis zu 20 % WER-Senkung bei manchen Sprachen allein durch Kompositum-Erkennung (z. B. "Wetter Bericht" gegen "Wetterbericht"), insgesamt bis zu 25 % gegenüber üblichen Bibliotheken (52 Sprachen, Common Voice 17, Whisper large-v3 und SeamlessM4T). Aussage: **Ein Teil der "Fehler" im Deutschen sind Schreibweise-Varianten von Zusammen-/Getrenntschreibung, nicht Hörfehler.** Die Deutsch-Einzelwerte stehen im PDF-Diagramm, nicht im Text.
- **Cross-Check Schweizerdeutsch** (https://arxiv.org/pdf/2606.07608): behandelt Dialekt, nicht Hochdeutsch-Diktat; Hinweis auf Benchmark-Kontamination (Whisper ohne Schweizerdeutsch-Training schafft 13,9 % auf dem Testsatz). Relevanz für Frank: gering.
- Ältere Arbeit zu deutschen Fehlertypen: https://arxiv.org/pdf/2204.05617 (2022, nicht ausgewertet, zu alt für Modelle von 2026).

### 4.4 Blogs und Praxisberichte

- **Famulor Labs** (Voice-Agent-Anbieter), Benchmark vom 13.10.2026: https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026. Deutsch-STT-WER auf **acht FLEURS-Clips, 161 Referenzwörter, nur männliche Sprecher**: Scribe v2 Realtime 0,62; Gemini 3.5 Transcribe Live 1,24; AssemblyAI U3 RT Pro / U3.5 Pro / U3.6 Pro je 1,86; Speechmatics Linden-1 2,48; **Soniox v5 3,73**; xAI STT 1/2 je 3,73; Deepgram Nova-3 4,35; Deepgram Flux 6,21; Deepgram Nova-2 7,45; Cartesia Ink Whisper 8,07. Ein einziger Fehler = 0,62 Punkte. Autoren selbst: "Rangfolge innerhalb einer Sprache", Stichprobe klein. **Nur anekdotisch**. Whisper/Turbo/gpt-transcribe sind nicht dabei.
- **Hacker-News-Stimmen zu Gemini 3.5 Transcribe** (über eesel.ai, https://eesel.ai/blog/gemini-3-5-transcribe-review): ein Nutzer nennt es das beste Diktatmodell, ein anderer sieht Scribe vorn, ein Team ging für Zeitstempel zu Whisper zurück. Anekdotisch, ohne Deutsch-Bezug.
- **Deutsche Medien (c't, heise, Golem):** Trotz gezielter Suche **kein Vergleich mit Wortfehlerraten** gefunden. Nur ein c't-Ratgeber 2023 zur Whisper-Nutzung (Anleitung, nicht geprüft). Blog-Aussagen wie "95 bis 97 % Genauigkeit" (cloudmagazin.com) sind unbelegt. **KEIN unabhängiger deutscher Medientest 2025/2026 mit Zahlen gefunden.**
- **Reddit r/LocalLLaMA:** Der konkrete Thread wurde nicht gefunden, keine Aussage ableitbar.
- **Local-Vergleich WZ-IT** (https://wz-it.com/en/blog/local-speech-to-text-gdpr/, 25.09.2026): zitiert Modellkarten, kein Eigentest.

---

## 5. Eigennamen, Zahlen, Diktat-Stil, Code-Switching Deutsch-Englisch

**Gesamtbild: Kein unabhängiger Modellvergleich mit Zahlen für deutsche Eigennamen, deutsche Zahlen oder Diktat-Stil gefunden.** Folgende Teilbelege gibt es:

1. **Code-Switching Deutsch-Englisch, kommerzielle APIs** (Perle AI, Mai 2026, v3): https://arxiv.org/abs/2605.19069 (Tabelle 4, PDF geprüft). 300 Samples je Paar, Deutsch-Englisch, ausgewählt per GPT-4o/Gemini-1.5-Pro-Ensemble. Gesamt (4 Paare) WER: Scribe v2 13,2 %; gpt-4o-transcribe 38,6 %; Chirp 3 39,4 %; Azure 43,6 %; Deepgram Nova-3 nur Deutsch 5,0 % (nicht vergleichbar). **Deutsch-Englisch ist das einfachste Paar, "alle bis auf ein System unter 10 % WER"**; Azure mit Standard-Spracherkennung 20,5 %, mit `LanguageIdMode=Continuous` 17,1 %. Scribe v2 ist in allen Paaren Erster. Die Pro-Modell-Werte für Deutsch stehen nur im Diagramm. Herkunft: Forschungsgruppe, Selbstdarstellung "Self-reported". Quelle: https://benchmarklist.com/benchmarks/asr_code_switch_benchmark/.
2. **CSFleurs / Whisper-Zusatztraining** (https://arxiv.org/abs/2606.21990): Whisper Turbo als Basis: WER auf Deutsch-Englisch-CSFleurs **11,49 %**, PIER (Fehlerrate auf eingestreuten Fremdwörtern) **26,59 %**; zum Vergleich Deutsch-Common-Voice-14 8,53 % (einsprachig). **Anglizismen sind für Whisper-Turbo etwa 2- bis 3-mal schwerer als normales Deutsch.** Im Paper nur Turbo-Basis, keine anderen Modelle.
3. **MUSCAT** (KIT, 2026): https://arxiv.org/html/2604.15929v2. Deutsche Gespräche (OWL-Aufnahmen): Whisper WER 12,22, Phi-4 15,72, wav2vec2 27,93. PIER für Code-Switching-Wörter: Whisper 39,29, SALMONN 57,14, Phi-4 64,29. 54 englische Einschübe in deutscher Sprache. Kleine Stichprobe (wenige Minuten). Alle Modelle schlechter auf Code-Switch-Wörtern. Kein Turbo/large-v3-Unterschied.
4. **Entity-Fehlerrate (AssemblyAI, Hersteller, Englisch)**: Missed Entity Rate Universal-3.5 Pro: Namen von Organisationen 17,0 %, Orte 8,6 %, E-Mail-Adressen 33,8 %, Telefonnummern 13,1 %, Kreditkartennummern 21,8 % (https://www.assemblyai.com/docs/pre-recorded-audio/benchmarks.md). Zeigt: Auch das beste Modell verfehlt bei Eigennamen jedes sechste bis siebte Mal. Deutsch nicht ausgewiesen.
5. **Zahlen:** Die HF-Rangliste normalisiert **keine** deutschen Zahlen (siehe Abschnitt 1). Der Vergleich "zwanzig" gegen "20" fließt dort als Fehler ein, falls das Referenzformat abweicht. Auch bei AA werden Zahlen nur nach englischer Logik angeglichen. Über die Güte der Zahlenerkennung (Datum, Uhrzeit, Telefonnummern) auf Deutsch sagt **keine** Rangliste etwas.
6. **Diktat-Stil (Zeichensetzung, Groß-/Kleinschreibung, Komposita):** Alle großen Ranglisten **ignorieren** Zeichensetzung und Großschreibung (HF-Normalizer; Casablanca: Zeichensetzung allein bis 5 Punkte WER). Das heißt: Wie gut Turbo gegen Alternativen **Satzzeichen und Großschreibung** setzt, misst niemand unabhängig. Das ist für Franks Diktat aber der sichtbarste Fehlertyp.
7. **Kontext-/Keyword-Funktion für Eigennamen:** gpt-transcribe akzeptiert Text-Kontext und Keywords (Decoder-Artikel), Voxtral Kontext-Bias bis 100 Begriffe (nur Englisch gut, Deutsch "experimentell", Mistral-Seite), AssemblyAI Key-Terms (Dokumentation). Wirksamkeit auf Deutsch nirgends unabhängig gemessen.
8. **Gradium-Blog "word-error-rate-evaluations"** (https://gradium.ai/blog/word-error-rate-evaluations) betrifft **TTS** (Qwen3-ASR als Prüfer), nicht STT. Seine Deutsch-Werte (0,35 bis 0,72) gehören **nicht** in einen STT-Vergleich. Einzig brauchbare Aussage: Die Autoren sagen selbst, dass einfache Sätze die WER-Metrik "saturieren" und harte Tests für Zahlen, Entitäten und Code-Switching fehlen.

---

## 6. Konsolidierte Tabelle "Deutsch-WER je Modell je Benchmark"

Spalten: **HF-FL / HF-MCV** = Open-ASR-Leaderboard Deutsch (FLEURS / Common Voice), Abschnitt 1, unabhängig (HF). **handy** = handy.computer FLEURS-de Q8, unabhängig. **Voxtral-P** = Voxtral-Paper (Mistral, FLEURS / CV). **Qwen-P** = Qwen3-ASR-Paper. **Andere** = sonstige Quellen. Alle Werte WER in Prozent. "—" = keine Deutsch-Zahl gefunden. Herkunft in Klammern: u = unabhängig, H = Hersteller.

| Modell | HF-FL (u) | HF-MCV (u) | handy (u) | Voxtral-P (H) | Qwen-P (H) | Andere (Quelle, Herkunft) |
|---|---|---|---|---|---|---|
| **Whisper large-v3 Turbo** (BASIS) | 3,67 | 8,58 | 4,54 | — | — | whispernotes 4,05 FLEURS (H/Eigenmessung); primeLine: Tuda 8,30, CV19 3,85, MLS 3,20, gesamt 3,65 (Eigenangabe); AA englisch 4,6 (Groq, u) |
| Whisper large-v3 | 3,20 | 4,79 | 4,13 | 5,46 / 6,25 | (nur Mittel) | primeLine: Tuda 7,88, CV19 3,48, MLS 2,83, gesamt 3,28; ElevenLabs-Seite FLEURS 4,5 (H); Canary-Paper 4,30 (unbelegt); Med-De (Distilled) 12,6 (u) |
| gpt-4o-transcribe | — | — | — | — | — | Code-Switch-Paper: Deutsch-Englisch unter 10 % (u, nur Diagramm); AA englisch 4,0 (u) |
| gpt-4o-mini-transcribe | — | — | — | 3,76 / 6,72 (Voxtral-Paper); 4,07 / 6,05 (Realtime-Paper) | — | AA englisch 4,5 |
| **gpt-transcribe** (OpenAI, Juli 2026) | 2,34 | 3,34 | — | — | — | AA englisch 3,3 |
| Gemini 2.5 Flash | — | — | — | 4,74 / 7,25 | — | ElevenLabs-Seite: Gemini Flash 2 FLEURS 3,9 (H) |
| Gemini 3.5 Transcribe (Chirp 3 Nachfolger) | — | — | — | — | — | Famulor (Live-Variante, 161 Wörter): 1,24 (anekdotisch); Google FLEURS-Sprachmittel 5,04 (H, Deutsch unbelegt); Code-Switch (Chirp 3): unter 10 % |
| Google Chirp 3 | — | — | — | — | — | Sprachmittel FLEURS 7,32 (unbelegt); Code-Switch-Paper: Deutsch-Englisch unter 10 % |
| Azure Speech (06/07-2026) | 1,93 / 1,83 | 1,88 / 2,01 | — | — | — | Code-Switch-Paper: Deutsch-Englisch 20,5 (Standard) / 17,1 (Continuous) (u); Pipecat (englisch) 1,21 |
| Deepgram Nova-3 (Multi) | — | — | — | — | — | Deepgram: 8,8 oder 10,5 auf echten Anrufen (H); Famulor 4,35 (anekdotisch); Casablanca VoxForge 5,2 (u, Tabelle verstümmelt); Code-Switch-Paper Deutsch-Englisch 5,0 (u); ElevenLabs-Seite Nova 2: 9,1 (H) |
| AssemblyAI Universal-3.5 Pro | 2,17 | 2,69 | — | — | — | eigene Seite FLEURS 4,88 (H); Famulor 1,86 (anekdotisch); Med-De "Universal" 3,0 (u) |
| AssemblyAI Universal-3 Pro | 2,42 | 2,76 | — | — | — | Paper v4 Tab. 4: 2,34 |
| ElevenLabs Scribe v2 | 2,30 | 2,19 | — | Scribe v2 Realtime: 4,31 / 16,60 (Mistral-Messung, Streaming!) | — | Famulor Realtime 0,62 (anekdotisch); Code-Switch-Paper: bestes Modell (u); Scribe(v1) im Voxtral-Paper 4,78 / 3,52; ElevenLabs-Seite v1 2,7 / 3,1 (H) |
| Speechmatics Enhanced | 3,56 | 1,99 | — | — | — | Famulor Linden-1 2,48 (anekdotisch); Pipecat englisch 1,40; Soniox-Studie 2023 (H, alt) |
| Soniox v5 | 2,66 | 4,62 | — | — | — | Famulor 3,73; Soniox-Eigenangaben 1,25 bis 7 % (unbelegt, widersprüchlich) |
| Mistral Voxtral Small 24B (2507) | 2,61 | 3,19 | 3,29 | 3,38 / 3,74 | — | Med-De "Voxtral Small Latest" 7,1 (u); AA englisch 2,8 |
| Voxtral Mini 3B (2507) | 3,64 | 5,29 | 4,71 | 4,40 / 6,07 | — | Med-De Mini 8,5 |
| Voxtral Mini Transcribe (V2) | — | — | — | 3,54 / 4,35 / 4,85 (je nach Paper) | — | Mistral-Seite: ca. 4 % Mittel über 10 Sprachen (H) |
| Voxtral Mini 4B Realtime (2602) | 4,87 | 7,81 | 4,24 | — | — | Modellkarte (über WZ-IT): 6,19 FLEURS bei 480 ms |
| Parakeet TDT 0.6B v3 | 4,16 (fast-gpu: 4,09) | 4,07 (3,95) | 5,24 | — | — | NVIDIA-Karte: FLEURS 5,04, CoVoST 4,84 (H) |
| Canary-1B-v2 | 3,43 | 4,69 | 4,46 | — | — | Paper-Mittel 5,2 über 6 Sprachen (H) |
| Qwen3-ASR-1.7B | 3,35 | 4,60 | 4,25 | — | 3,92 / 5,85 (H) | whispernotes 2,85 FLEURS (Eigenmessung) |
| Qwen3-ASR-Flash-1208 | — | — | — | — | 3,03 / 3,53 (H) | — |
| Qwen3-ASR-0.6B | 5,41 | 7,97 | 6,80 | — | 6,48 / 9,44 | — |
| Cohere Transcribe 03-2026 | 3,33 | 2,87 | 5,06 | — | — | Paper v4 Tab. 4: 3,84 |
| Microsoft Phi-4-multimodal | 3,99 | 4,25 | — | — | — | Paper v4: 3,96 |
| Meta Omnilingual LLM 7B v2 | 3,72 | 5,06 | — | — | — | Paper v4: 4,55 |
| Gladia Solaria-1/3 | — | — | — | — | — | nur relativ: Solaria-3 gegen -1: -3 % / -13 % (H) |
| Whisper large-v3 deutsch fein-getunt (primeLine) | — | — | — | — | — | CV19 3,215, MLS 2,129, Tuda 7,711 (Eigenangabe) |
| TheStage "thewhisper-large-v3-turbo" (optimiert) | 2,91 | 3,17 | — | — | — | (nicht bei Groq) |

---

## 7. Was die Zahlen für Franks Diktat bedeuten

1. **Turbo ist auf der besten unabhängigen Deutsch-Rangliste keine gute Wahl.** Platz 30 von 41 im Mittel. Der Grund ist die Common-Voice-Spalte (8,58 gegen 4,79 bei large-v3). Common Voice besteht aus kurzen, einzeln gesprochenen Sätzen mit gewöhnlichen Mikrofonen. Das ähnelt **dem Diktatfall** (kurzer Satz, Handy- oder Headset-Mikro) deutlich mehr als FLEURS (Studio-nahe, vorgelesene Wikipedia). Auf FLEURS ist Turbo mit 3,67 noch nahe an großen Modellen. Hinweis: Das HF-Turbo ist der Original-Checkpoint, Groq kann abweichen (Hoster-Streuung bis ca. 6 Punkte bei Casablanca, AA zeigt es für large-v3 bei Replicate).
2. **Gleiche Modellfamilie, mehr Qualität:** large-v3 (nicht Turbo) halbiert den Abstand auf Common Voice, ist laut handy, HF und primeLine durchgängig besser auf Deutsch (Turbo spart Decoder-Schichten, das kostet Deutsch überproportional). Ob Groq `whisper-large-v3` im Kostenlos-Kontingent verfügbar ist, klärt ein anderer Bericht.
3. **Echte Telefon-/Meeting-Audios** liegen 2- bis 3-mal höher als Lesesprache (Deepgram: Whisper 19,9 produktiv gegen 8,4 kuratiert, Hersteller). Erwarten Sie für Diktat mit Hintergrundgeräuschen deutlich höhere Zahlen als in jeder Tabelle oben.
4. **Eigennamen und Zahlen:** Weder HF noch AA misst sie auf Deutsch. HF zählt "zwanzig" gegen "20" als Fehler, ignoriert aber Umlaute und Zeichensetzung. **Ein Modell mit 2 % HF-WER kann trotzdem Eigennamen falsch schreiben.** Hier hilft nur ein eigener Test mit Franks Namensliste (die messung/eigennamen.txt im Projektordner) oder Funktionen wie Keyword-/Kontext-Bias (gpt-transcribe, Scribe, AssemblyAI).
5. **Anglizismen** ("Update", "Meeting"): Mit Whisper Turbo rund doppelte bis dreifache WER gegenüber reinem Deutsch (CSFleurs 11,49 gegen 8,53 einsprachig, PIER 26,59). Bei kommerziellen APIs ist Deutsch-Englisch das "einfachste" Paar (unter 10 %), Scribe v2 führt. Für Nutzerdiktat sind das die realistischen Zahlen.
6. **Zeichensetzung und Groß-/Kleinschreibung** fließen in keine der Ranglisten ein. Wenn Frank sich über fehlende Satzzeichen ärgert, ist das mit diesen Zahlen nicht prüfbar.

---

## 8. Fazit: Welche Modelle sind belastbar besser als Turbo?

**Kriterium** (festgelegt, damit "belastbar" überprüfbar bleibt): Das Modell schlägt Turbo in **beiden** HF-Deutsch-Spalten (gleiche Daten, gleicher Normalizer, unabhängige Messung) **und** wird von **mindestens einer weiteren** Quelle gestützt (andere Messung, Dritt-Test oder Paper).

**Belastbar besser (beide Kriterien erfüllt):**
- **Whisper large-v3** (selbst): HF 3,20/4,79 gegen 3,67/8,58; handy 4,13 gegen 4,54; primeLine gesamt 3,28 gegen 3,65. Deutlichster Abstand auf Common Voice. Kleinster Schritt (gleiches Modell, anderes Gewicht).
- **ElevenLabs Scribe v2**: HF 2,30/2,19; Famulor 0,62 (anekdotisch); Code-Switch-Paper führend; Casablanca-Spalte unlesbar. Stark, aber Streaming-Variante schnitt in Mistrals Messung schlecht ab (16,60 auf CV), also Batch-Modell verwenden.
- **AssemblyAI Universal-3.5 Pro / 3 Pro**: HF 2,17/2,69; Med-De 3,0 % (bestes aller sechs Modelle); Famulor 1,86.
- **Mistral Voxtral Small 24B**: HF 2,61/3,19; handy 3,29 (bester dort); Voxtral-Paper 3,38/3,74. (Medizin-Test: 7,1 %, schlechter als AssemblyAI.)
- **Qwen3-ASR-1.7B**: HF 3,35/4,60 (FLEURS besser, MCV nicht klar besser als large-v3, aber besser als Turbo); handy 4,25 gegen 4,54; whispernotes 2,85 gegen 4,05 (Eigenmessung). Offen verfügbar.
- **Cohere Transcribe 03-2026**: HF 3,33/2,87 besser als Turbo; handy 5,06 aber schlechter als Turbo (4,54), also nicht belegt auf FLEURS in Dritt-Messung, **eingeschränkt**.

**Wahrscheinlich besser, aber nur durch eine Quelle belegt (HF):**
- **Microsoft Azure Speech 06/07-2026**: HF Platz 1 (1,83 bis 2,01). Keine zweite Deutsch-Messung. Im Code-Switch-Paper dagegen das **schwächste** System (17,1 bis 20,5 %) bei Deutsch-Englisch. Für Franks Anglizismen ein Warnsignal.
- **gpt-transcribe** (2,34/3,34), **Speechmatics Enhanced** (3,56/1,99), **Soniox v5** (2,66/4,62; FLEURS stark, CV schwach), **Parakeet v3** (4,16/4,07; auf CV besser als Turbo, FLEURS leicht schlechter, handy 5,24 schlechter), **Canary-1B-v2** (3,43/4,69).

**Nicht belegt oder nicht bewertbar:** gpt-4o-transcribe (keine Deutsch-Zahl), Google Gemini 3.5 Transcribe / Chirp 3 (keine unabhängige Deutsch-Zahl), Deepgram Nova-3 (nicht in HF, nur Herstellerzahlen und eine kleine Dritt-Zahl), Gladia (nur relativ).

**Wichtigste Einschränkung:** Alle belastbaren Zahlen messen **vorgelesene Sätze**. Sie sagen nichts über Eigennamen, Zahlenformate, Anglizismen im Fließtext oder Satzzeichen. Wer wirklich entscheiden will, braucht den eigenen Test mit Franks Aufnahmen (Ordner `messung` existiert bereits). Die Rangliste taugt als **Vorauswahl**: Whisper large-v3, Scribe v2, Universal-3.5 Pro, Voxtral Small und Qwen3-ASR-1.7B.

---

BEST-PRACTICES-KANDIDATEN:
- Primärquelle für deutsche ASR-Rangfolgen ist die Rohdatei `multilingual_de.csv` des HF Open ASR Leaderboards, nicht die Hersteller- oder AA-Seiten. URL: https://huggingface.co/datasets/hf-audio/multilingual_evals/blob/main/multilingual_de.csv, Commit `3589b3c077cb16c935e52bdd15c7fb0a3f175f7a` (19.10.2026). Dazu: Quelle zum Normalizer https://github.com/huggingface/open_asr_leaderboard/blob/main/normalizer/normalizer.py (Stand 21.10.2026).
- STT-Vergleiche nur innerhalb einer Quelle und mit identischer Normalisierung lesen; Spanne desselben Modells bis 2,3 Punkte (Whisper-v3 FLEURS-de) und 2,7 Punkte (AssemblyAI). URLs: https://huggingface.co/datasets/hf-audio/multilingual_evals/blob/main/multilingual_de.csv (3,20), https://arxiv.org/html/2507.13264 (5,46), https://www.assemblyai.com/docs/pre-recorded-audio/benchmarks.md (4,88 gegen 2,17).
- Zeichensetzung entfernen senkt WER bis zu 5 Punkte; gleiches Modell bei verschiedenen Hostern bis ca. 6 Punkte Unterschied: https://research.casablanca.at/post/why-your-asr-benchmark-isnt-telling-you-the-whole-truth (Stand 21.10.2026).
- Kompositum-Erkennung im WER-Tool (OpenWER, bis 20 % WER-Senkung je Sprache): https://arxiv.org/abs/2606.21237 (19.06.2026), Code https://github.com/shuffle-project/openwer.
- Eigener Test mit eigenen Aufnahmen und eigener Namensliste ist nötig, weil keine Rangliste Eigennamen, Zahlen oder Anglizismen auf Deutsch misst (Abschnitt 5).

BUG-KANDIDATEN:
- **Whisper large-v3-turbo: überproportionale Qualitätseinbuße auf Deutsch gegenüber large-v3**, auf Common Voice-de 8,58 gegen 4,79 WER (HF-Messung, 19.10.2026). Bestätigt durch primeLine-Evaluation (Turbo schlechter auf Tuda-De, CV19, MLS). URLs: https://huggingface.co/datasets/hf-audio/multilingual_evals/blob/main/multilingual_de.csv (Commit `3589b3c077cb16c935e52bdd15c7fb0a3f175f7a`), https://huggingface.co/primeline/whisper-large-v3-turbo-german. Modellversion: openai/whisper-large-v3-turbo, HF-Checkpoint; Groq-Variante nicht geprüft.
- **HF-Open-ASR-Normalizer ist für Deutsch umlaut-blind und zahlenblind:** `BasicMultilingualTextNormalizer(remove_diacritics=True)` wandelt ä/ö/ü in a/o/u und ß in ss um, kennt keine deutsche Zahlen-Normalisierung. Folge: Verwechslungen wie "schon/schön" oder "Mutter/Mütter" werden nicht als Fehler gezählt, "20" gegen "zwanzig" dagegen schon. URL: https://github.com/huggingface/open_asr_leaderboard/blob/main/normalizer/normalizer.py (Funktion `remove_symbols_and_diacritics`, Stand 21.10.2026). Das ist ein Mess-Artefakt, kein Software-Bug im Modell.
- **Hoster-Abweichung bei Whisper large-v3:** Replicate 10,1 gegen fal.ai 4,1 AA-WER (gleiches Modell). URL: https://artificialanalysis.ai/speech-to-text/batch (Stand 21.10.2026). Für Groq-Turbo (4,6) nicht auffällig, aber nur englisch gemessen.
- **Azure Speech bei Deutsch-Englisch-Code-Switching:** 20,5 % WER im Standard-Modus der Sprach-ID, 17,1 % mit `SpeechServiceConnection_LanguageIdMode=Continuous`. URL: https://arxiv.org/abs/2605.19069 (v3, Mai 2026).
- Herstellerseiten mit internen Widersprüchen (als unbelegt, keine Software-Bugs): ElevenLabs 2,7 gegen 3,1 % (https://elevenlabs.io/speech-to-text/german), Deepgram 8,8 gegen 10,5 % (https://deepgram.com/learn/german-benchmarks).
