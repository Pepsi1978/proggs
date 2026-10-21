# R5 — Spezialisierte STT-Anbieter Teil 1: Deepgram, AssemblyAI, Speechmatics, Gladia, Rev.ai, Soniox

Stand der Recherche: 21.10.2026. Basislinie: Groq `whisper-large-v3-turbo`. Alle Zahlen stammen aus Primärquellen (Herstellerseiten, Doku) oder aus den genannten Drittquellen. „Vendor" = vom Hersteller selbst gemessen, nicht unabhängig. „unbelegt" = ich habe keine belastbare Quelle gefunden.

## 0. Wichtigste Vorbemerkungen

1. **Es gibt keinen unabhängigen, großen Deutsch-Benchmark, der Groq-Turbo und diese Anbieter gemeinsam misst.** Alle deutschen WER-Zahlen unten sind entweder Vendor-Zahlen oder winzige Stichproben (Famulor: 161 Referenzwörter, 1 Fehler = 0,62 Prozentpunkte). Ein eigener A/B-Test mit Franks echten Diktaten ist unverzichtbar.
2. **Kein Anbieter dieser Gruppe hat noch ein dauerhaftes (monatlich erneuertes) Freikontingent.** Es gibt nur einmalige Startguthaben: Deepgram 200 $, Speechmatics 100 $, AssemblyAI 50 $, Gladia 50 €, Rev.ai 5 Std., Soniox seit 27.10.2025 nichts mehr für Neuanmeldungen. Frühere Monatskontingente (Speechmatics 8/40/50 Std., Gladia 10 Std., Soniox 200 $) sind abgeschafft bzw. widersprüchlich dokumentiert.
3. **Der Artificial-Analysis-Index (AA-WER) misst überwiegend Englisch/gemischt** (Datensätze AA-AgentTalk, VoxPopuli-Cleaned-AA, Earnings22-Cleaned-AA; Sprachen auf der Seite nicht ausgewiesen) und ist daher für Deutsch nur eine grobe Orientierung. Groq Whisper-Large-v3-Turbo liegt dort bei 4,6 % WER, 200,8× Echtzeit, 0,67 $/1000 Min. (= 0,04 $/Std.) — https://artificialanalysis.ai/speech-to-text
4. Neue Modellnamen 2026, die in älteren Quellen noch fehlen: AssemblyAI **Universal-3.5 Pro** (async + Sync-API), **Universal-3.6 Pro Realtime**; Soniox **v5** (`stt-async-v5`, `stt-rt-v5`); Gladia **Solaria-3**; Speechmatics **Melia 1**; Deepgram **Flux Multilingual** (`flux-general-multi`).

---

## 1. Deepgram

### Modelle
| Modell-ID | Status | Deutsch | Modus |
|---|---|---|---|
| `nova-3` / `nova-3-general` (`language=de`, `de-CH`) | aktuell, monolingual für Deutsch seit 04.09.2025; Batch-Modell für `de` am 16.07.2026 verbessert (ohne WER-Angabe) | ja | Batch + Streaming |
| `nova-3` mit `language=multi` (Nova-3 Multilingual) | aktuell, Update 05.02.2026 (bessere Codeswitching-WER); 12.05.2026 Zahlenformatierung („zwölf" → 12) auch für Deutsch | ja | Batch + Streaming |
| `flux-general-multi` (Flux Multilingual) | GA 15.04.2026 (Changelog) bzw. 29.04.2026 (Pressemeldung); 10 Sprachen inkl. `de`, `language_hint` | ja | nur Streaming (WebSocket `/v2/listen`) |
| `flux-general-en` | aktuell | nein (nur Englisch) | Streaming |
| `nova-2`, `enhanced`, `base` | Legacy | ja | — |

Quellen: https://developers.deepgram.com/docs/models-languages-overview , https://developers.deepgram.com/docs/flux/language-prompting , https://developers.deepgram.com/changelog/2026/7/16 , https://developers.deepgram.com/changelog/2026/2/5 , https://developers.deepgram.com/changelog/2026/5/12 . Ein „Nova-4" ist nicht belegt.

### Deutsch-Qualität (WER)
- **Vendor (Deepgram, 05./25.03.2026)**, ca. 26 Std. deutsches Audio (> 200 000 Wörter), Produktionsaudio (Telefon, Meetings, Drive-through, Contact Center) + kuratiertes Audio, Referenz durch Drittteam; FLEURS/Common Voice bewusst ausgeschlossen. Batch Produktion: **Nova-3 Multi 8,8 %**, ElevenLabs 11,4 %, **Whisper 19,9 %** (kuratiert 8,4 %; Whisper-Version nicht genannt). Streaming Produktion/kuratiert: Nova-3 12,0 / 6,1 %, Soniox 14,5 / 6,8 %, AssemblyAI 20,4 / 9,3 %, ElevenLabs 22,8 / 14,2 %. Auf der Seite stehen für Nova-3 Multi zwei Batch-Werte (8,8 % und 10,5 %) ohne Erklärung; Diagramme nicht lesbar. https://deepgram.com/learn/german-benchmarks
- **Vendor (AssemblyAI)**, Common Voice, Realtime-Tabelle: Deepgram Nova-3 **12,8 %**, Flux 9,9 %, Gladia Solaria 10,5 % (Tabellen unterschiedlicher Systemgruppen, nicht direkt vergleichbar). Globaler vorab aufgezeichneter Multilingual-WER Nova-3: 15,71 % (AssemblyAI Universal-3.5 Pro 7,84 %). https://www.assemblyai.com/benchmarks
- **Famulor Labs (13.10.2026, n = 161 Wörter, FLEURS, 8 Clips, nur männliche Sprecher, ohne Rauschen)**: Nova-3 **4,35 %**, Flux General Multi 6,21 %, Nova-2 7,45 %. https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026 — statistisch kaum belastbar.
- **Drittquelle VexaScribe (Juli 2026)**: Nova-3 8,0 % auf FLEURS-Deutsch, Universal-3.5 2,6 % (Methodik und Quelle unklar, hier nur als unbelegt-Hinweis). https://vexascribe.com/how-accurate-is-deepgram
- AA-WER (englisch-lastig): Nova-3 5,2 %, schnellstes Modell (586,6×), 4,30 $/1000 Min. Groq-Turbo 4,6 %. https://artificialanalysis.ai/speech-to-text

### Freikontingent
- **200 $ Startguthaben, einmalig, ohne Kreditkarte** (Pay-as-you-go). Guthaben läuft nicht nach 12 Monaten ab („available until you use it up"), nicht übertragbar. https://deepgram.com/pricing
- Entspricht (eigene Rechnung): Nova-3 mono Batch 0,26 $/Std. → ca. 770 Std.; mit Keyterms (0,34 $/Std.) → ca. 590 Std.; Multilingual Batch 0,31 $/Std. → ca. 645 Std.; Streaming 0,29 $/Std. → ca. 690 Std.
- **Das ist das größte Startguthaben dieser Gruppe nach Stunden.** Datennutzung für Training im Free-Tier: unbelegt (Parameter `mip_opt_out` existiert laut Deepgram-Blog zur AU-Region, https://deepgram.com/learn/deepgram-australia-endpoint-now-generally-available).

### Preis je Audiostunde (Pay-as-you-go)
- Nova-3 mono: Batch 0,26 $; Streaming 0,29 $ (laut Seite befristeter Aktionspreis). Nova-3 Multi: Batch 0,31 $; Streaming 0,35 $. Flux Multi Streaming 0,47 $. **Keyterm Prompting +0,08 $/Std.** Growth ab 4 000 $/Jahr mit bis zu 20 % Rabatt. https://deepgram.com/pricing
- Groq-Turbo-Vergleich: ca. 0,04 $/Std.

### Limits
- Max. Dateigröße **2 GB**; Verarbeitungs-Zeitlimit 10 Min. (Nova) → sonst 504. 100 gleichzeitige Anfragen je Projekt (Nova/Base/Enhanced), Whisper-Cloud 5 (PAYG). Seite zu `api-rate-limits` war nicht erreichbar → Detail für Flux unbelegt. https://developers.deepgram.com/docs/pre-recorded-audio
- Max. Audiolänge: nicht genannt.

### Streaming
Ja: Nova-3 (WebSocket `/v1/listen`) und Flux (`/v2/listen`, mit Turn-Detection, für Voice-Agents; für reines Diktat nicht nötig).

### Vokabular / Keyterms
- **Keyterm Prompting** für Nova-3 (mono + multi) und Flux. Parameter `keyterm=Begriff` wiederholt; Mehrwort-Begriffe mit `%20`/`+`; **maximal 500 Tokens** über alle Keyterms, empfohlen 20–50 Begriffe. Komma, Semikolon, Zeilenumbruch oder Gewichte erzeugen **keinen Fehler, boosten aber nichts** (stiller Ausfall). Bei Streaming mid-stream per `Configure` änderbar. https://developers.deepgram.com/docs/keyterm
- Ob Keyterms speziell für Deutsch gelten: Seite nennt keine Sprachliste (Ankündigung „Nova-3 Multilingual Keyterm Prompting", https://deepgram.com/learn/deepgram-expands-nova-3-with-10-new-languages-and-multilingual-keyterm-prompting); für `de` monolingual nicht explizit belegt. Der Launch-Artikel zu Nova-3 Deutsch (26.08./04.09.2025) nennt Keyterm Prompting als Nova-3-Feature mit deutschem Beispiel (Medikamentennamen), bestätigt es aber nicht ausdrücklich für das monolinguale Modell; seine WER-Gewinne zeigt er nur als Bilder (https://deepgram.com/learn/deepgram-expands-nova-3-with-german-dutch-swedish-and-danish-support).

### Halluzinationen
Keine unabhängigen Daten. Vendor: Einfügungsrate Batch Nova-3 Multi 1,21 % vs ElevenLabs 5,05 %, Soniox 4,40 % (Deepgram-Seite); AssemblyAI behauptet 73 % weniger Falsch-Ausgaben bei Rauschen als Nova-3 (Juni 2025, Vendor, ungeprüft). Whisper-typische Stille-Halluzinationen entfallen bei Nova-3 mangels Seq2seq-Generation (Vermutung, unbelegt).

### Datenschutz / Datenresidenz
- **EU-Endpoint `api.eu.deepgram.com` seit 03.12.2025 GA** (AWS-EU-Regionen; Land kann wechseln; fixe Länder nur bei Dedicated), STT ohne Whisper-Modelle, gleiche Keys. https://developers.deepgram.com/reference/regional-endpoints , https://deepgram.com/learn/deepgram-eu-endpoint-now-generally-available
- „Deepgram does not store transcripts" (Doku). Training/Aufbewahrung der Audiodaten im PAYG ohne `mip_opt_out`: unbelegt.

### Latenz
Schnellster Anbieter im AA-Index (586,6× Echtzeit; Groq-Turbo 200,8×). Praktisch für 5–30-s-Diktate unter 1 s.

### Android-Integration (REST)
`POST https://api.deepgram.com/v1/listen?model=nova-3&language=de&smart_format=true&keyterm=Anna%20M%C3%BCller` — Header `Authorization: Token <KEY>`, `Content-Type: audio/wav` (oder passend), Body = Binärdatei (`--data-binary`); Antwort: `results.channels[0].alternatives[0].transcript`. Ein Request, synchron. Für EU `api.eu.deepgram.com`. (Der Parameter `language=de` ist auf der Quickstart-Seite nicht gezeigt, Sprachcode `de` laut Modellseite.) https://developers.deepgram.com/docs/getting-started-with-pre-recorded-audio
- `smart_format` formatiert Zahlen/Währung; für Deutsch monolingual Zahlenformat nicht explizit belegt (Multi: seit 12.05.2026 ja).

---

## 2. AssemblyAI

### Modelle
| Modell-ID | Status | Deutsch | Preis/Std. |
|---|---|---|---|
| `universal-3-5-pro` (Doku nennt `universal-3-pro` im Sprachverzeichnis) | aktuelles Flaggschiff für vorab aufgezeichnet, 18 Sprachen inkl. **Deutsch** | ja | 0,21 $ |
| `universal-2` | 99 Sprachen, Deutsch in Stufe „High (≤ 10 % WER)" | ja | 0,15 $ |
| `universal-3-6-pro` (Universal-3.6 Pro Realtime) | gestartet September 2026, 32 Sprachen inkl. Deutsch | ja | 0,45 $ |
| `universal-streaming-multilingual` | 6 Sprachen (en, es, de, fr, pt, it) | ja | 0,15 $ |
| **Sync-API** `POST /v1/transcribe` (Modell-Header `universal-3-5-pro`) | für Diktat/kurze Clips, 32 Sprachen laut Doku | ja | 0,45 $ |
| `slam-1` | deprecated | — | — |

Quellen: https://www.assemblyai.com/pricing.md , https://www.assemblyai.com/llms/models.md , https://www.assemblyai.com/docs/pre-recorded-audio/supported-languages , https://www.assemblyai.com/languages/german
Abweichung: Doku nennt „18" vs. „19" Sprachen in einem Entscheidungsbaum; Deutsch ist überall aufgeführt. Universal-3 Pro (Vorgänger) laut VexaScribe „deprecated seit Juli 2026".

### Deutsch-Qualität (WER)
- **Vendor-Benchmark (AssemblyAI)**, vorab aufgezeichnet, WER nach Sprache: **Universal-3.5 Pro 9,07 %**, Speechmatics Enhanced 10,30 % (Datensatz nicht genannt, nur diese beiden Anbieter mit deutscher Zahl). Globaler Multilingual-WER: AssemblyAI 7,84 %, Speechmatics Enhanced 8,22 %, OpenAI GPT-4o Transcribe 9,52 %, Mistral Voxtral Mini 10,5 %, ElevenLabs Scribe V2 11,13 %, Whisper-1 14,39 %, Nova-3 15,71 %. https://www.assemblyai.com/benchmarks
- **Realtime FLEURS Deutsch (AssemblyAI, Läufe 25.08.–23.09.2026, Snapshot 16.09.2026)**: Gemini 3.5 Live 4,7; **Universal-3.6 Pro RT 5,0**; Reson8 5,3; ElevenLabs Scribe v2 5,8; Zoom 6,5; Meta 6,4; Speechmatics Enhanced 6,8; **Soniox Realtime 6,9**; AWS 8,0; Speechmatics Standard 8,2; Azure 9,1; **Gladia Solaria 10,9**; OpenAI GPT Realtime 11,2; GPT-4o Transcribe 13,1.
- Famulor (n = 161 Wörter): U3.5 Pro / U3.6 Pro / U3 RT Pro je **1,86 %** (bester Wert der Kommerziellen außer ElevenLabs 0,62 % und Gemini 1,24 %). Universal Streaming Multilingual: Lauf fehlerhaft („Duplicate final results"), nicht bewertet.
- Deepgram (Vendor) sieht AssemblyAI im **Streaming** bei 20,4 % Produktion / 9,3 % kuratiert (vermutlich ältere Streaming-Variante), deutlich hinter Nova-3 — Gegenaussage der Hersteller; stützt „selbst testen".
- AA-WER (englisch-lastig): Universal-3.5 Pro 3,0 %, Universal-3 Pro 3,1 %, Universal-2 3,8 %; 3,50 $/1000 Min. (Basispreis ohne Keyterms).

### Freikontingent
- **50 $ Startguthaben, ohne Kreditkarte, läuft nicht ab**, wird beim Upgrade übernommen. Gilt für Pre-recorded, Realtime, Voice Agent, Speech Understanding, Guardrails (nicht LLM Gateway). Danach Karte nötig, sonst „API access is paused". https://www.assemblyai.com/docs/billing-and-pricing , https://www.assemblyai.com/pricing.md
- Rechnerisch: Pre-recorded U3.5 Pro 0,21 $ → ca. 238 Std.; mit Keyterms (0,26 $) → ca. 192 Std.; Universal-2 0,15 $ → ca. 333 Std.; **Sync-API 0,45 $ → ca. 111 Std.** (Keyterms/Prompt im Sync-Preis enthalten? Preisseite listet für Sync keinen Zuschlag, aber auch keine Aussage → unbelegt.)
- „Default model selection can differ between free-tier and paid accounts, so set `speech_models` explicitly." (Pricing-Seite) → immer Modell explizit setzen.

### Preis je Audiostunde
Pre-recorded: U3.5 Pro 0,21 $; Universal-2 0,15 $; Keyterms +0,05 $ (bis 1000 Begriffe bei U3.5 Pro, 200 bei Universal-2). Streaming: U3.6 Pro RT 0,45 $, Universal-Streaming Multilingual 0,15 $ (Keyterms inklusive); abgerechnet wird die **Sitzungsdauer, Leerlauf zählt**. Sync-API: 0,45 $. https://www.assemblyai.com/pricing.md

### Limits
- Async: Datei max. **5 GB / 10 Std.**, Upload 2,2 GB. https://www.assemblyai.com/docs/faq/are-there-any-limits-on-file-size-or-file-duration-for-files-submitted-to-the-api
- **Sync-API: 20 Min. und 256 MB pro Request**, Serverfrist 30 s (sonst 504); `/v1/transcribe/live`: max. 120 s, nur WAV/PCM. https://www.assemblyai.com/docs/sync-stt/audio-requirements
- Gleichzeitig: Free 5 parallele Async-Jobs, bezahlt 200+; Streaming Free **5 neue Streams/Minute**, bezahlt 100/Min. Bei Guthaben < 0 sinkt das Limit auf 1. https://www.assemblyai.com/docs/pre-recorded-audio/rate-limits

### Streaming
Ja (WebSocket), plus „Dictation"-Produkt und Sync-API `/v1/transcribe/live` für kurze Diktate.

### Vokabular / Prompting / Keyterms
- Async U3.5 Pro: `keyterms_prompt` bis 1000 Begriffe (je ≤ 6 Wörter); allgemeines Prompting bis 1500 Wörter; **Prompt und Keyterms nicht zusammen** bei diesem Modell. Streaming U3.6 Pro RT: Keyterms bis 100 inklusive, mitten im Stream änderbar. https://www.assemblyai.com/llms/models.md
- **Sync-API:** `config`-JSON-Part mit `prompt` (≤ 6000 Zeichen) und `keyterms_prompt` (≤ 100 Begriffe, 8000 Zeichen), beides kombinierbar. https://www.assemblyai.com/docs/sync-stt/prompting-and-keyterms — Deutsch-Support dieser Parameter wird auf der Seite nicht explizit bestätigt; die Sprachseite markiert Keyterms für Deutsch bei allen fünf Modellen als unterstützt. https://www.assemblyai.com/languages/german

### Halluzinationsverhalten
Vendor: Universal-3 Pro Halluzinationsrate ca. 30 % unter Whisper; Universal-Streaming 73 % weniger Rausch-Fehlausgaben als Nova-3 (Juni 2025). Beide Vendor-Zahlen ungeprüft. Eine „Non-speech response rate" wird in der Methodik definiert, Ergebnisse habe ich im lesbaren Seitenteil nicht gefunden.

### Datenschutz / Residenz
- **EU-Endpoints**: Sync `https://sync.eu.assemblyai.com/v1/transcribe` (Frankfurt, Stockholm, Mailand, Spanien, Irland, Paris); US-Endpoint separat; globaler Standard routet zur nächsten Region. https://www.assemblyai.com/docs/sync-stt/endpoints-and-data-zones
- Training: Dateien sind vom Training ausgeschlossen bei BAA, **bei Nutzung der EU-Server** oder bei Opt-out; Standard sonst: Nutzung nach PII-Redaktion „as permitted by contract". Streaming/Sync/Dictation: **Zero Data Retention bei Opt-out**. Async: Audio nach 24–48 Std. gelöscht, Transkript nach 30 Tagen (TTL bis 1 Std. konfigurierbar). https://www.assemblyai.com/docs/data-retention-and-model-training

### Latenz
AA-Speed 105× Echtzeit (U3.5 Pro, Async). Die Sync-API ist für kurze Diktate gedacht (Serverfrist 30 s, `warm`-Endpoint zum Vorwärmen). Konkrete Millisekunden für 10-s-Clips: unbelegt.

### Android-Integration (REST)
Sync: `POST https://sync.eu.assemblyai.com/v1/transcribe`, Header `Authorization: <KEY>` und **`X-AAI-Model: universal-3-5-pro`**, Body `multipart/form-data`: Part `audio` (z. B. `audio/wav`, `audio/mp4`/m4a/mp3/ogg-opus/flac/webm erlaubt) + optionaler Part `config` (JSON: `language_codes: ["de"]`, `prompt`, `keyterms_prompt`, Platzierung von `language_codes` im Config-Part ist eine Annahme aus den SDK-Beispielen). Antwort-JSON: `text`, `words[]`, `confidence`, `audio_duration_ms`, `session_id`, `request_time_ms`. Optional `GET /v1/warm`. https://www.assemblyai.com/docs/sync-stt/getting-started/transcribe-a-short-audio-file
**Ein einziger Request, synchron — passt am besten zum bisherigen Groq-Muster (multipart-Upload, direkte Antwort).**

---

## 3. Speechmatics

### Modelle
| Modell | Batch / Realtime | Deutsch | Custom Dictionary |
|---|---|---|---|
| **Enhanced** | beide | ja (Sprachpaket) | ja |
| Standard | beide | ja | ja |
| **Melia 1** (seit 17.06.2026 „production preview") | Batch (Realtime früh/Preview), 55+ Sprachen mit Codeswitching | ja (multilingual) | noch nicht |
| Oak 1 | Batch, Medizin | — | noch nicht |
| Linden 1 | nur Agent-STT (`/v2/agent`) | unbelegt | unbelegt |

Quellen: https://docs.speechmatics.com/speech-to-text/models.md , https://speechmatics.com/company/articles-and-news/introducing-melia-multilingual-speech-to-text-model

### Deutsch-Qualität (WER)
- AssemblyAI-Vendor: pre-recorded Deutsch **Enhanced 10,30 %** (vs AssemblyAI 9,07 %); Realtime FLEURS Deutsch Enhanced 6,8 %, Standard 8,2 %. https://www.assemblyai.com/benchmarks
- Famulor (n = 161): „Linden-1" 2,48 % (Agent-STT, nicht das Batch-Enhanced-Modell). https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026
- Speechmatics-Vendor (Melia): gewinnt auf FLEURS gegen Deepgram in 91 %, Microsoft 91 %, AssemblyAI 77 % der Sprachen; keine deutsche Einzelzahl, keine absoluten WER. Eigener interner Vergleich: Melia ≈ 5 % niedrigerer WER als Standard bei verrauschtem monolingualem Audio.
- AA-WER (englisch-lastig): Enhanced 4,0 %, Melia 4,9 %, Standard 5,1 %; Preise bei AA: 12,50 / 4,00 / 7,50 $ pro 1000 Min. (also Enhanced ≈ 0,75 $/Std., konsistent mit Preisseite).
- Eine unabhängige deutsche Gesamtzahl für Enhanced habe ich nicht gefunden.

### Freikontingent (Monatsstunden abgeschafft; Neukunden 100 $, Bestandskonten 25 $)
- **Aktuelle Preisseite und Doku (für Neuanmeldungen): Free-Plan = 100 $ Guthaben bei Anmeldung, ohne Karte, 12 Monate gültig; API pausiert bei leerem Guthaben; 2 gleichzeitige Realtime-Sessions.** https://www.speechmatics.com/our-technology/pricing , https://docs.speechmatics.com/administration/plans.md (Doku nennt keinen Ablauf).
- Ankündigung „Moving to credit based billing": ab **01.08.2026** statt 50 freier Monatsstunden **einmalig 25 $ Übergangsguthaben** für Self-Service-Konten, die **vor** dem 01.08.2026 angemeldet waren (andere Gruppe als die 100 $ für Neukunden, also kein echter Widerspruch; ein Ablaufdatum der 100 $ und ein Anmeldedatum für sie nennt die Doku nicht). https://www.speechmatics.com/company/articles-and-news/moving-to-credit-based-billing
- Ältere/Drittangaben: 8 Std./Monat (FAQ), 40 Std./Monat (Preisseite alt), 20 + 30 Std. (Tracker Juli 2026), Melia-Artikel Juni 2026 „10 Stunden pro Monat frei". Alles vor dem 01.08.2026 → obsolet.
- Rechnerisch aus 100 $: Melia Batch 0,24 $ → ca. 417 Std.; Standard 0,45 $ → ca. 222 Std.; **Enhanced Batch 0,75 $ → ca. 133 Std.**; Enhanced Realtime 0,80 $ → ca. 125 Std.
- Training: Opt-in zum Modelltraining gibt 33 % Rabatt (also Standard = kein Training, abgeleitet).

### Preis je Audiostunde (Listenpreis)
Melia 1: 0,24 $ Batch / 0,40 $ Realtime. Enhanced: 0,75 / 0,80 $. Standard: 0,45 / 0,45 $. Rabatte: Credit-Packs bis 20 %, Abo bis 25 %, Trainings-Opt-in −33 %. https://www.speechmatics.com/our-technology/pricing

### Limits
Batch: 1 GB Mediendatei im Job-POST (größere per URL), 10 neue Jobs/s, 50 Status-Abfragen/s, 20 000 gleichzeitige Jobs; Dauer nicht angegeben. Free: 2 Realtime-Sessions; Self-Serve 50. https://docs.speechmatics.com/speech-to-text/batch/limits.md

### Streaming
Ja (Realtime-WebSocket, Standard/Enhanced); Melia Realtime nur Early-Access-Preview.

### Vokabular
**Custom Dictionary** (`additional_vocab`) mit optionalem `sounds_like`: empfohlen ≤ 1000 Einträge je Job, ab 20 000 Fehler, Einträge > 6 Wörter fallen weg. Standard/Enhanced; Melia noch nicht. https://docs.speechmatics.com/speech-to-text/features/custom-dictionary.md — stark für Eigennamen (Aussprachehilfen), aber Batch-Jobs sind asynchron.

### Halluzinationen
Keine Daten gefunden (unbelegt).

### Datenschutz / Residenz
Regionen **EU1, US1, AU1**, Region = aufgerufener Endpunkt; Self-Serve und Enterprise wählbar. Keine Residenzgarantie in der Doku formuliert; „Whether anything is stored depends on the processing mode". https://docs.speechmatics.com/administration/regions.md

### Latenz
AA: Enhanced 69,7×, Melia 170,6×, Standard 101,2× Echtzeit. Batch ist Job-basiert (POST /v2/jobs, Polling) → für Diktat spürbar langsamer als Sync-APIs.

### Android-Integration (REST)
Batch: `POST /v2/jobs` (multipart: Part `config` JSON mit `transcription_config {language:"de", operating_point:"enhanced", additional_vocab:[…]}` + Part `data_file`), dann Status pollen, dann `GET /v2/jobs/{id}/transcript?format=txt`. Regionale Hostnamen (z. B. `eu1.asr.api.speechmatics.com`) auf der Regionsseite nicht aufgeführt → **unbelegt/zu prüfen**. Mehrstufig, für Diktat weniger geeignet; Realtime-WebSocket wäre die Alternative.

---

## 4. Gladia

### Modelle
`solaria-1` (Standard, 100+ Sprachen, async + live, Codeswitching), `solaria-3` (**nur async**, en/fr/de/es/it, genau eine Sprache, veröffentlicht 10.06.2026), `solaria-fusion` (im Schema, undokumentiert). https://docs.gladia.io/api-reference/v2/pre-recorded/init

### Deutsch-Qualität
- Vendor (via ActuIA, 12.06.2026): Solaria-3 auf Gladias interner Audiobasis bei **Deutsch nur ca. 3 % besser** als Solaria-1 (Englisch 26 %); auf Studio-Daten schlechter (MLS 8,0 % vs 5,9 %; VoxPopuli 2,9 % vs 2,2 %); Datensatz proprietär, nicht reproduzierbar. https://www.actuia.com/nl/news/solaria-3-gladia-aan-kop-in-productie-audio-volgens-eigen-metingen/
- AssemblyAI-Benchmark Realtime FLEURS Deutsch: Gladia Solaria **10,9 %** (hinter Soniox 6,9, Speechmatics 6,8, AssemblyAI 5,0); Common Voice Deutsch 10,5 %.
- Famulor: Solaria-1 nicht bewertet („Stream closed early").
- AA-WER (englisch-lastig): Solaria-3 3,2 %, Solaria-1 4,1 %; ca. 10,2 $/1000 Min.
- Gladia behauptet „#1 für Deutsch auf Produktionsaufnahmen" ohne Zahl (Gladia-Blog). Fazit: **keine belastbare Deutsch-Überlegenheit.**

### Freikontingent (Quellen widersprechen sich)
- Preisseite: **50 € Startguthaben, 1 Jahr gültig**, einmalig; ≈ 312 Std. Solaria-3 (0,16 €/Std.), 185 Std. Solaria-1 (0,27 €), 73 Std. Realtime. Kartenpflicht beim Signup: nicht genannt. https://www.gladia.io/pricing
- Gladia-Blog (Rev-Vergleich) nennt „10 free hours, no credit card" → mutmaßlich veraltet; die „10 Std./Monat" aus der Aufgabenstellung sind **nicht belegt**.

### Preis
Self-Serve: Solaria-3 ab 0,16 €/Std. (async), Solaria-1 ab 0,27 €/Std., Realtime 0,68 €/Std.; Preisstufen fallen mit Volumen. Alt (Blog): Starter 0,61 $ async / 0,75 $ live, Growth ab 0,20 $/0,25 $.

### Limits
Pre-recorded: **135 Min., 1000 MB**, max. 2 Kanäle; Live-Session 3 Std. 25 gleichzeitige Async-Anfragen, 30 Realtime-Sessions (429 bei Überschreitung). https://docs.gladia.io/chapters/limits-and-specifications/supported-formats

### Streaming / Vokabular
Live nur Solaria-1 (Solaria-3 async only). `custom_vocabulary` [Beta] mit Intensität/Aussprache je Begriff.

### Halluzinationen
Keine Daten.

### Datenschutz
Self-Serve: Training **standardmäßig an**, Opt-out im Profil kostenlos; Enterprise: kein Training, Zero Retention möglich. Hosting-Standort **nicht genannt**. Drittquelle meldet Übernahme durch OVHcloud (Aug. 2026), nur Sekundärquelle (unbelegt). https://www.gladia.io/pricing

### Android (REST)
`POST https://api.gladia.io/v2/pre-recorded` mit Header `x-gladia-key`, JSON `{audio_url, language_config:{languages:["de"]}, model:"solaria-3"}`; vorher Datei per `/v2/upload` hochladen (Upload-Pfad hier nicht verifiziert); Antwort 201 mit `id` + `result_url`, dann Polling. Mehrstufig. Für Diktat unattraktiv.

---

## 5. Rev.ai

- Produkte: **Reverb Transcription (nur Englisch) 0,20 $/Std.**, **Reverb Foreign Language 0,30 $/Std.** („Spanisch, Französisch, Chinesisch, Portugiesisch und 53 weitere"), **Deutsch nicht auf der Preisseite genannt**. https://www.rev.ai/pricing
- Freikontingent: **5 Std. Reverb-ASR einmalig** (PAYG). Min. 15 s je Auftrag, Sekundenabrechnung.
- Deutsch-WER: keine Zahl gefunden (Rev legt WER nur für Englisch offen). Streaming: „9+ Sprachen", Deutsch unbelegt.
- Daten: Rev nutzt Kundenaudio **standardmäßig** zur ASR-Verbesserung, Opt-out per Mail an den Support. https://www.gladia.io/blog/rev-ai-vs-gladia-complete-comparison-for-global-teams-2026 (Wettbewerberquelle, mit Vorsicht)
- API: `POST https://api.rev.ai/speechtotext/v1/jobs` (Bearer, Multipart oder URL), Polling. Details (Felder, Dateilimits, Custom Vocabulary für Deutsch) unbelegt.
- **Bewertung: keine Empfehlung** (Deutsch nicht belegt, kein Vorteil gegenüber den anderen).

---

## 6. Soniox

### Modelle
`stt-async-v5` (Async), `stt-rt-v5` (Realtime); `stt-async-v4`/`stt-rt-v4` sind Aliasse auf v5, laut Doku zur Entfernung am **30.06.2026** vorgesehen; `stt-rt-v3` Entfernung 28.02.2026. 60+ Sprachen, Diarization, Sprach-ID, Codeswitching, semantisches Endpointing (RT v5). https://soniox.com/docs/stt/models

### Deutsch-Qualität
- **Soniox-Marketing „1,25 % WER Deutsch"** (Deepgram 1,71, OpenAI 3,24, Google 2,84, ElevenLabs 3,16) taucht nur in Suchtreffern auf; **auf den Vergleichsseiten und der Benchmarkseite fand ich keine deutschen Zahlen**. Vier von fünf Werten (1,71 / 2,84 / 3,16 / 3,24; Soniox selbst 1,25 statt 1,34) stimmen mit dem **englischsprachigen Pipecat-Realtime-Benchmark** überein (Deepgram nova-3-general 1,71, Google latest-long 2,84, ElevenLabs 3,16, gpt-4o-transcribe 3,24). Fazit: **unbelegt und vermutlich falsch zugeordnet; nicht verwenden.** https://soniox.com/benchmarks (Pipecat STT Benchmark, August 2026, Soniox stt-rt-v5 1,34 % semantischer WER, englisch-orientiert, 1000 Samples)
- Älterer Soniox-Eigenbenchmark: https://soniox.com/media/SonioxGermanBenchmarks2023.pdf (Dezember 2023, nicht geprüft, veraltet).
- AssemblyAI-Benchmark: Soniox Realtime FLEURS Deutsch **6,9 %** (ElevenLabs 5,8, Speechmatics Enhanced 6,8, AssemblyAI RT 5,0).
- Deepgram-Vendor Streaming Deutsch: Soniox 14,5 % Produktion / 6,8 % kuratiert (besser als AssemblyAI, schlechter als Nova-3).
- Famulor (n = 161): Soniox STT v5 **3,73 %** (Nova-3 4,35 %).
- AA-WER: Soniox v5 Async **3,8 %** (englisch-lastig), Speed 42,3×, 1,66 $/1000 Min. (= 0,10 $/Std.).
- Widersprüchliche Drittwerte (Azure-Seite zitiert 7 % Deutsch aus einer Studie von 2025).

### Freikontingent
- **Keine Startguthaben mehr für Neuanmeldungen** seit Blogpost vom 27.10.2025 (vorher 200 $ ≈ 2000 Std.; Grund: Missbrauch). Bestandskonten behalten Guthaben. https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api
- Die Preisseite erwähnt weder „$200" noch „free" (geprüft); eine Suchzusammenfassung, die „200 $ ohne Karte" auf der Preisseite behauptet, ist daher nicht bestätigt. Die Quickstart-Seite mit „300 Minuten" ließ sich nicht abrufen (404). Die Apps (Mobil/Desktop) haben ein eigenes wöchentliches Gratis-Guthaben-Programm — nicht für die API.
- Ergebnis: **für die API kein belegtes Freikontingent.** Prüfen: im Console-Signup, ob aktuell Guthaben erscheint.

### Preis
**Async ca. 0,10 $/Std.; Realtime ca. 0,12 $/Std.** (Tokens: Audio-Input 1,50 $ bzw. 2,00 $ je 1 Mio.; ca. 30 000 Audio-Tokens/Std.; zusätzlich Text-Output-Tokens). Günstigster Anbieter der Gruppe, aber teurer als Groq (0,04 $/Std.). https://soniox.com/pricing

### Limits
- Realtime: 100 Requests/Min., 10 gleichzeitige Streams, 300 Min. je Sitzung (fix). https://soniox.com/docs/stt/rt/limits-and-quotas
- Async: 300 Min. je Datei (laut Limits-Seite; ein Suchtreffer nannte 60 Min. — Seite hat Vorrang), 10 GB Speicher, 100 ausstehende / 2000 Transkriptionen, Dateien/Transkripte nach 30 Tagen automatisch gelöscht; Dateigröße nicht angegeben. https://soniox.com/docs/stt/async/limits-and-quotas

### Streaming
Ja (`stt-rt-v5`, WebSocket `stt-rt.soniox.com`), mit Übersetzung und Endpointing.

### Vokabular / Kontext
`context` (JSON): `general` (Schlüssel/Wert, ≤ ca. 10 Paare), `text` (Freitext), **`terms`** (Liste für Eigennamen/Schreibweise), `translation_terms`; insgesamt bis **8000 Tokens** (≈ 10 000 Zeichen) — deutlich mehr Spielraum als Deepgram (500 Tokens). Wirkung auf Deutsch: nicht belegt. https://soniox.com/docs/stt/concepts/context

### Halluzinationen
Keine Daten. Deepgram-Vendor: Einfügungen Batch 4,40 % (vs Nova-3 Multi 1,21 %). Speechmatics-Vergleich: Token-basierte Abrechnung schwankt mit Sprechdichte/Stille.

### Datenschutz / Residenz
Kein Training mit Kundendaten; Realtime wird nicht gespeichert; Async-Daten werden nach 30 Tagen gelöscht. **EU-Region wählbar** (api.eu.soniox.com, stt-rt.eu.soniox.com; Zugang über Console „Request access", Freigabe nötig). Zertifizierungen: GDPR, ISO 27001:2022, SOC 2 Typ 2, HIPAA. https://soniox.com/docs/stt/data-residency , https://soniox.com/docs/security-and-privacy

### Latenz
AA-Speed 42,3× Echtzeit (langsamster dieser Gruppe, Async); Realtime-WebSocket hat Endpointing.

### Android (REST)
Async: (1) `POST https://api.soniox.com/v1/files` multipart `file` → `id`; (2) `POST /v1/transcriptions` JSON `{model:"stt-async-v5", file_id, language_hints:["de"], context:{terms:[…]}}`; (3) Polling `GET /v1/transcriptions/{id}` bis `completed`; (4) `GET /v1/transcriptions/{id}/transcript` → `tokens[].text` zusammensetzen; (5) `DELETE`. Header `Authorization: Bearer`. **Vier bis fünf Requests**, daher für Kurz-Diktate umständlich; für Diktat besser Realtime-WebSocket. https://soniox.com/docs/stt/async/async-transcription

---

## 7. Tabelle „Kerndaten"

| | Deepgram Nova-3 | AssemblyAI U3.5 Pro (Sync) | Speechmatics Enhanced | Gladia Solaria-3 | Rev.ai | Soniox v5 | Groq Turbo (Basis) |
|---|---|---|---|---|---|---|---|
| Deutsch unterstützt | ja (mono `de` + multi) | ja | ja | ja (async) | unbelegt | ja | ja |
| WER Deutsch Vendor/Dritt (verschiedene Sets!) | Prod. Batch 8,8 % (Vendor); FLEURS-n=161: 4,35 %; CV: 12,8 % | pre-rec. 9,07 % (Vendor); FLEURS-n=161: 1,86 % | pre-rec. 10,30 %; RT FLEURS 6,8 % | RT FLEURS 10,9 %; CV 10,5 % | keine | RT FLEURS 6,9 %; FLEURS-n=161: 3,73 % | Whisper 19,9 % Prod./8,4 % kuratiert (Deepgram, Version unklar); AA 4,6 % (englisch-lastig) |
| Freikontingent | 200 $ einmalig, keine Karte, kein Ablauf | 50 $ einmalig, keine Karte, kein Ablauf | 100 $ einmalig für Neukunden, 12 Mon., keine Karte (Bestandskonten vor 01.08.2026: 25 $ Übergang; Monatsstunden abgeschafft) | 50 € einmalig, 1 Jahr | 5 Std. einmalig | keins für Neukunden seit 27.10.2025 | dauerhaft (nicht Teil dieser Recherche) |
| Stunden aus Guthaben | ca. 590–770 | ca. 111 (Sync) / 192–238 (Async) | ca. 133 (Enhanced) – 417 (Melia) | ca. 185–312 | 5 | 0 | — |
| Preis/Std. (Batch) | 0,26 $ (+0,08 Keyterms) | 0,21 $ (+0,05) / Sync 0,45 $ | 0,75 $ (Melia 0,24 $) | 0,16–0,27 € | 0,30 $ (Fremdsprache) | ca. 0,10 $ | ca. 0,04 $ |
| Streaming | ja | ja | ja | ja (Solaria-1) | ja (9+ Spr.) | ja | nein (REST) |
| Vokabular | Keyterm (500 Tokens) | Keyterms 100 (Sync) / 1000 (Async), Prompt | Custom Dictionary + sounds_like | Custom Vocabulary (Beta) | unbelegt | `context.terms` (8000 Tokens) | Prompt (224 Tokens, nicht Teil) |
| EU-Hosting | ja, `api.eu.deepgram.com` | ja, `sync.eu…` | ja, EU1 | nicht genannt | unbelegt | ja (Antrag nötig) | — |
| Training auf Kundendaten | `mip_opt_out` nötig? unbelegt | EU = nein; sonst Opt-out; Sync ZDR bei Opt-out | Standard: nein (abgeleitet) | Starter: ja (Opt-out) | ja (Opt-out per Mail) | nein | — |
| Limits | 2 GB, 100 parallel | Sync 20 Min./256 MB; Async 5 GB/10 Std.; Free 5 parallel | 1 GB, 10 Jobs/s; Free 2 RT | 135 Min./1000 MB | unbelegt | 300 Min./Datei async, RT 10 parallel | — |
| REST-Form | 1 Request (binär) | 1 Request (multipart) | 3 Schritte (Job, Poll, Abruf) | 2–3 Schritte | 3 Schritte | 4–5 Schritte | 1 Request (multipart) |
| Speed (AA, ×RT) | 586,6 | 105,5 | 69,7 | 90,8 | — | 42,3 | 200,8 |

---

## 8. Fazit — Besser als Groq Turbo für Deutsch?

**Gesamturteil: unklar bis wahrscheinlich ja für AssemblyAI Universal-3.5 Pro und Deepgram Nova-3; nein für Gladia und Rev.ai; unklar für Soniox und Speechmatics.**

- **AssemblyAI Universal-3.5 Pro (Sync-API): wahrscheinlich ja.** Beste Deutschzahlen unter den hier getesteten Spezialanbietern in zwei Quellen (Vendor 9,07 % vs Speechmatics 10,30 %; Realtime-FLEURS 5,0 %; Famulor 1,86 %), Keyterms+Prompt direkt im Diktat-Endpunkt, ein einziger Multipart-Request wie bei Groq, EU-Endpoint ohne Training, ZDR bei Opt-out. Nachteile: Vendor-Daten, nur 50 $ einmalig (≈ 111 Std. Sync), teurer als Groq (0,45 $ vs ca. 0,04 $/Std.).
- **Deepgram Nova-3 (Batch `de`): wahrscheinlich ja gegenüber Whisper, aber gegenüber Turbo unklar.** Vendor-Zahl: Whisper 19,9 % vs Nova-3 8,8 % auf Produktionsaudio (Whisper-Version unbekannt, kuratiert nur 8,4 %), Famulor 4,35 %, AssemblyAI-Vendor Common Voice 12,8 %. Größtes Startguthaben (200 $ ≈ 590–770 Std., ohne Karte, ohne Ablauf), schnellste API, 1 Request. Keyterm-Falle (Komma → stiller Ausfall). Gegen AssemblyAI in deutschen Zahlen schwächer.
- **Soniox: unklar.** Deutsche Evidenz nur Vendor/FLEURS-Mittelfeld (6,9 %), kein Startguthaben mehr, aber billigster Preis (0,10 $/Std.) und mächtigster Kontext (8000 Tokens). Die behaupteten 1,25 % WER sind unbelegt.
- **Speechmatics: unklar.** Solide Deutschzahlen (Enhanced 10,3 % vs 9,07 %, RT 6,8 %), starkes Custom Dictionary mit `sounds_like`, EU1, 100 $ Startguthaben für Neukunden (Bestandskonten nur 25 $ Übergang), Batch asynchron und teuer (Enhanced 0,75 $/Std.).
- **Gladia: nein.** Deutsch-Gewinn Solaria-3 nur ca. 3 %, FLEURS Deutsch 10,9 % (hinterm Feld), Training standardmäßig an, Hosting unklar.
- **Rev.ai: nein/unbelegt.** Deutsch nicht ausgewiesen.

**Zum Ziel „großes kostenloses Kontingent":** Keiner dieser Anbieter ersetzt Groqs laufendes Gratis-Kontingent; es gibt nur einmalige Pools. Reihenfolge nach Stunden: Deepgram (≈ 590–770) > Gladia (≈ 185–312, nur ein Jahr) > Speechmatics (≈ 133–417, 12 Monate) > AssemblyAI (≈ 111–238). Bei z. B. 30 Min. Diktat pro Tag (≈ 183 Std./Jahr) hielte das Deepgram-Guthaben 3–4 Jahre, AssemblyAI-Sync ca. 7 Monate.

**Empfehlung für den Test:** 30–50 echte Diktate (mit Eigennamen, Zahlen, Fachwörtern) gegen Groq Turbo, Deepgram Nova-3 (`language=de`, `keyterm`) und AssemblyAI Sync (U3.5 Pro, `keyterms_prompt`) laufen lassen; WER mit gleicher Normalisierung (Groß-/Kleinschreibung, Zahlen) berechnen; Latenz mitnehmen. Soniox und Speechmatics nur bei Bedarf nachziehen.

---

BEST-PRACTICES-KANDIDATEN:
- AssemblyAI Sync-STT-API für Diktat (ein multipart-Request, `X-AAI-Model: universal-3-5-pro`, `config` mit `prompt`/`keyterms_prompt`, EU-Endpoint `sync.eu.assemblyai.com`): https://www.assemblyai.com/docs/sync-stt/getting-started/transcribe-a-short-audio-file — Stand Universal-3.5 Pro, 21.10.2026
- AssemblyAI-Konto: Modell immer explizit setzen (`speech_models`), da Standardmodell bei Free/Paid abweicht: https://www.assemblyai.com/pricing.md — 21.10.2026
- Deepgram Keyterm Prompting korrekt anwenden (je Begriff ein `keyterm=`-Parameter, kein Komma, ≤ 500 Tokens): https://developers.deepgram.com/docs/keyterm — Nova-3 / Flux, 21.10.2026
- Deepgram EU-Endpoint `api.eu.deepgram.com` für Datenschutz-Diktat: https://developers.deepgram.com/reference/regional-endpoints — GA seit 03.12.2025
- STT-Auswahl: unabhängige Deutsch-Benchmarks fehlen; immer mit eigenen Aufnahmen testen (Famulor-n = 161-Hinweis): https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026 — 18.10.2026

BUG-KANDIDATEN:
- Deepgram `keyterm` mit Komma/Semikolon/Zeilenumbruch/Gewichten liefert keinen Fehler und boostet nichts (stiller Ausfall): https://developers.deepgram.com/docs/keyterm — Nova-3, Stand 21.10.2026
- AssemblyAI Streaming: Leerlauf-Verbindungszeit wird abgerechnet („Idle connection time counts"): https://www.assemblyai.com/pricing.md — Universal-3.6 Pro Realtime / Streaming, 21.10.2026
- AssemblyAI Streaming-Multilingual lieferte bei Famulor „Duplicate final results"; Gladia Solaria-1 „Stream closed early": https://www.famulor.io/blog/voice-ai-german-vs-english-benchmark-2026 — 13.10.2026 (Einzelmessung, ungeprüft)
- Soniox: Modell-Aliasse `stt-*-v4` zur Entfernung am 30.06.2026, `stt-rt-v3` 28.02.2026 angekündigt (Breaking Change bei alten Konfigurationen): https://soniox.com/docs/stt/models — v5, 21.10.2026
- Soniox: Startguthaben für Neukunden abgeschafft (27.10.2025), Preisseiten/Suchsnippets teils veraltet: https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api
- Speechmatics: seit 01.08.2026 Credit-Abrechnung, monatliche Gratisstunden (8/10/20/40/50 Std.) abgeschafft, ältere Quellen veraltet; Neukunden 100 $ einmalig, Bestandskonten 25 $ Übergangsguthaben: https://www.speechmatics.com/company/articles-and-news/moving-to-credit-based-billing — 21.10.2026
- Deepgram Streaming-Preise sind laut Preisseite befristete Aktionspreise: https://deepgram.com/pricing — 21.10.2026
