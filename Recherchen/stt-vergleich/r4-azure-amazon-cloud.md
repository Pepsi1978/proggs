# R4 — Azure, Amazon, Cloudflare, Nvidia, Hugging Face, IBM: STT für Deutsch gegen Groq whisper-large-v3-turbo

Stand der Recherche: 21.10.2026. Basislinie: Groq `whisper-large-v3-turbo` (Franks aktuelles Diktat-Modell).
Kennzeichnung: **[belegt]** = Primärquelle mit URL, **[Sekundär]** = Blog/Presse/Herstellerzahlen Dritter, **[unbelegt]** = nichts Belastbares gefunden.

## Kurzfazit vorab

- Der Azure-Free-Tier F0 gilt **nur für Echtzeit-STT (5 Std./Monat)**. Fast Transcription, LLM Speech, MAI-Transcribe und Batch sind im F0-Tarif **nicht verfügbar** (Quota-Seite: „Not applicable"). Das gute Modell ist also nicht gratis.
- Das neue Azure-Modell **MAI-Transcribe-2** (03.09.2026, Public Preview) ist mit 0,10 US-$/Std. (befristet bis Jahresende 2026) extrem billig und kann Deutsch. Ein belastbarer **deutscher WER-Wert fehlt** aber (Microsoft nennt nur 5,2 % Schnitt über 60 Sprachen auf FLEURS). Ob es Whisper-Turbo schlägt: **unklar, nur durch eigene Messung zu klären**.
- Der Azure-Standard-Echtzeitdienst und Amazon Transcribe liegen auf FLEURS-Deutsch **deutlich hinter Whisper** (9,1 % bzw. 8,0 % gegenüber 4,3 % für whisper-large-v3). Beide sind keine Verbesserung.
- **Cloudflare Workers AI** hostet exakt dasselbe Modell (`@cf/openai/whisper-large-v3-turbo`), mit 10.000 Neurons/Tag gratis = rund 214 Audiominuten pro Tag. Das ist kein Qualitätsgewinn, aber der beste kostenlose Zweitweg bzw. Ausfall-Fallback.
- Hugging Face Inference Providers: Free-Nutzer haben **kein** Monatsguthaben mehr (Doku von heute). Nvidia build.nvidia.com: kostenlose gehostete ASR-Endpunkte **nicht belegbar**, Zugriff über gRPC, für Android unpraktisch. IBM Watson: 500 Min./Monat Lite, aber keine Deutsch-Qualitätsbelege und veraltete Technik.

---

## 1. Microsoft Azure AI Speech

Gemeinsame Basis für alle Modi: Ressource „Speech in Foundry Tools" (früher Azure AI Speech), Schlüssel und Endpunkt sind **regionsgebunden**. Franks Jarvis nutzt bereits `https://<region>.tts.speech.microsoft.com/...` mit einer in den Einstellungen hinterlegten Region (`TtsCatalog.azureRegion`, Quelle: `Jarvis/app/src/main/java/de/frank/jarvis/speech/Synthese.kt`). Ob dieselbe Ressource/Region für STT taugt, hängt von der Region ab (siehe Regionstabelle unten). Die Region seiner Ressource habe ich nicht eingesehen (Zugangsdaten bewusst nicht gelesen).

### 1.1 Echtzeit-STT (Standardmodell, Speech SDK und REST „short audio")

- **Modell-ID:** keine wählbare ID, dienstverwaltetes Standardmodell, Sprache per `language=de-DE`.
- **Status:** GA.
- **Deutsch-Qualität:** FLEURS Deutsch **9,1 % WER** für „Azure Realtime STT" im Streaming-Benchmark von AssemblyAI (Läufe 25.08.–23.09.2026, Stand Dokumentation 28.09.2026). Mittelwert über unterstützte Sprachen 12,7 %. **[Sekundär, Herstellerbenchmark eines Konkurrenten]** https://www.assemblyai.com/benchmarks
  Zum Vergleich nennt die NVIDIA-Veröffentlichung für **whisper-large-v3 auf FLEURS Deutsch 4,30 %** (Canary-1B-v2: 4,40 %, Parakeet-TDT-0.6B-v3: 5,04 %), Tabelle 11. **[Sekundär, Paper]** https://arxiv.org/pdf/2509.14128
  Achtung: unterschiedliche Normalisierung und Testaufbau (Streaming vs. Batch), die beiden Zahlen sind nur grob vergleichbar. Die Richtung ist trotzdem klar: Azure-Standard liegt auf Lese-Sprache etwa doppelt so hoch wie Whisper.
- **Freikontingent (F0):** **5 Audiostunden pro Monat**, gilt für Echtzeit-Transkription Standard und Custom (gemeinsam), „Batch is not supported". Auf der Preisseite sind für Fast Transcription, LLM Speech und MAI **keine** Freistunden aufgeführt. Laufzeit: monatlich erneuert, unbefristet (kein 12-Monats-Limit genannt). Datennutzung: Echtzeit-STT speichert nichts („processed only on the Azure's server memory, and no data is stored at rest"). **[belegt]** https://azure.microsoft.com/en-us/pricing/details/speech/ , https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/speech-to-text/data-privacy-security
  Offen: Ob die REST-„short audio"-Aufrufe ebenfalls auf die 5 Freistunden zählen, steht nirgends ausdrücklich. Sie laufen über denselben Echtzeit-Dienst, ich halte es für wahrscheinlich. **[unbelegt]**
- **Preis je Audiostunde (S0, Azure Retail Prices API, abgerufen 21.10.2026, Region westeurope):** Echtzeit **1,00 $/Std.** (S1 Speech To Text), Custom Echtzeit 1,20 $/Std. **[belegt]** https://prices.azure.com/api/retail/prices (Filter `serviceFamily eq 'AI + Machine Learning' and contains(productName,'Speech')`)
- **Rate Limits:** F0: **1** gleichzeitige Anfrage, nicht erhöhbar. S0: 100 gleichzeitige Anfragen (erhöhbar). **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits
- **Max. Dateigröße/Länge:** REST „short audio": **höchstens 60 Sekunden** Audio pro Anfrage, nur WAV/PCM 16 kHz mono oder OGG/Opus; liefert nur Endergebnisse. Das SDK streamt beliebig lange Sitzungen. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/rest-speech-to-text-short
- **Streaming/Echtzeit:** ja, über Speech SDK (WebSocket); Android-SDK vorhanden (Android x86 seit SDK 1.49 gestrichen). Zwischenergebnisse nur über SDK, nicht über REST short audio. **[belegt]** Release Notes https://learn.microsoft.com/en-us/azure/ai-services/speech-service/releasenotes
- **Vokabular/Prompting:** Phrase List (bis ~2.000 Phrasen, „longer phrase list will impact quality and latency"), Gewicht 0,0–2,0 (nur SDK), keine Modelltrainings nötig; Custom Speech (eigenes Modell, 1 Modell im F0 frei). Kein Freitext-Prompt. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/improve-accuracy-phrase-list
- **Satzzeichen/Diktat:** Ergebnis `DisplayText` enthält Groß-/Kleinschreibung, Satzzeichen und Inverse Text Normalization (Zahlen als Ziffern). Zusätzlich existiert im SDK ein „dictation mode" (Release Notes 1.48.1 erwähnen ihn; Details zu gesprochenen Satzzeichen auf Deutsch **[unbelegt]**).
- **Halluzinationsverhalten:** klassisches (nicht generatives) Modell, Standardfehler sind Auslassungen/Verwechslungen, keine freie Texterfindung. Spezifische Belege **[unbelegt]**.
- **Datenschutz/Residenz:** Sprachdaten werden „in the region where you create your resource" gespeichert und verarbeitet (Ausnahme Voice Live). EU-Regionen: westeurope, northeurope, germanywestcentral, francecentral, swedencentral, italynorth u. a. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions
- **Neu 2026 (Echtzeit):** „Monolingual post-stream refinement" (SDK 1.50, `PostProcessingOption`, Release Notes Mai 2026) verbessert das Endergebnis; laut Regionstabelle u. a. in germanywestcentral, francecentral, northeurope, swedencentral verfügbar, nicht in westeurope. Wirkung auf Deutsch **[unbelegt]**.
- **Latenz:** keine Herstellerzahl; mit Chunked Transfer („reduce recognition latency") und SDK-Streaming laufend. **[unbelegt]**
- **Android-Integration (REST-Form):**
  `POST https://<ressource>.cognitiveservices.azure.com/stt/speech/recognition/conversation/cognitiveservices/v1?language=de-DE&format=detailed`
  Header `Ocp-Apim-Subscription-Key`, `Content-Type: audio/wav; codecs=audio/pcm; samplerate=16000` (oder `audio/ogg; codecs=opus`), Body = Rohaudio, Antwort JSON mit `DisplayText`. `language` ist Pflicht (sonst 4xx). Der Schlüssel ist regionsgebunden.

### 1.2 Fast Transcription API (synchron, Datei-Upload)

- **Endpunkt/Modell-ID:** `POST .../speechtotext/transcriptions:transcribe?api-version=2025-10-15` (Fast Transcription braucht mindestens 2024-11-15, Phrase List 2025-10-15). Standardmodell ohne ID.
- **Status:** GA.
- **Deutsch-Qualität:** kein eigener WER-Beleg. Bei leerem `locales` wird das „latest multilingual model" verwendet, `de-DE` gehört zu dessen 15 Locales. Bei einer gesetzten Locale läuft das einsprachige Modell. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/fast-transcription-create
- **Freikontingent:** **keines**. Quota-Tabelle: F0 „Not applicable" für Dateigröße, Länge und Anfragen/Minute; Preisseite führt keine Freistunden. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits
- **Preis je Audiostunde:** **0,36 $/Std.** (Meter „Fast Transcription Speech To Text", seit 01.11.2024); zusätzlich ein Meter **„Fast Transcription Promo" 0,10 $/Std.**, gültig ab 01.09.2026, in allen EU-Regionen gelistet; Custom Fast Transcription 0,45 $/Std. Batch 0,18 $/Std. **[belegt]** https://prices.azure.com/api/retail/prices (abgerufen 21.10.2026). Einordnung (Schlussfolgerung aus Datumsabgleich, nicht belegt): Der Promo-Meter (ab 01.09.2026) deckt sich zeitlich mit dem Start von MAI-Transcribe-2 (03.09.2026, 0,10 $/Std. befristet) und ist vermutlich dessen Aktionspreis, **kein Rabatt auf das normale Fast-Transcription-Modell**; das normale Modell kostet 0,36 $/Std. Die 0,54-$-Meter (ab 01.10.2026) fallen zeitlich mit dem Start von MAI-Transcribe-2-Streaming (01.10.2026, 0,54 $ Einführungspreis) zusammen. **[Inferenz]**
- **Rate Limits:** 600 Anfragen/Minute, gemeinsam für Fast und Batch (erhöhbar, aber nur mit Firmen-E-Mail, gmail/outlook werden abgelehnt!). **[belegt]** Quota-Seite oben.
- **Max. Dateigröße/Länge:** < 500 MB und < 5 Stunden; Formate WAV, MP3, OPUS/OGG, FLAC, WMA, AAC, ALAW/MULAW (WAV), AMR, WebM, SPEEX. **[belegt]**
- **Streaming:** nein (synchron, „faster than real-time", keine Zahl).
- **Vokabular:** `phraseList.phrases` (Array), `locales`, `profanityFilterMode`, Diarisierung, Kanäle. **[belegt]**
- **Satzzeichen:** nur Display-Form (Satzzeichen + Großschreibung). **[belegt]**
- **Halluzination/Fehlverhalten:** Microsoft-Q&A (Juni 2025): Fast Transcription behandelte deutsche Sprache als Englisch trotz gesetzter `de-DE`-Locale, wurde „häufiger"; Empfehlung des Moderators: Locale fest setzen. Keine Microsoft-Bestätigung. **[Sekundär]** https://learn.microsoft.com/en-us/answers/questions/2284414/transcription-quality-issues-with-fast-transcripti
- **Datenschutz:** „When doing real-time speech to text, fast transcription ... Microsoft does not retain or store the data." **[belegt]** https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/speech-to-text/data-privacy-security . Fast Transcription läuft laut Regionstabelle u. a. in westeurope, northeurope, swedencentral, francecentral, germanywestcentral, italynorth, uksouth (nicht in switzerlandnorth). **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions
- **Android-Integration (REST):** `multipart/form-data` mit Teil `audio` (Datei) und Teil `definition` (JSON), z. B.
  `--form 'definition={"locales":["de-DE"],"phraseList":{"phrases":["Jarvis","Frank"]}}'`
  Header `Ocp-Apim-Subscription-Key`. Mit OkHttp `MultipartBody` einfach; Antwort `combinedPhrases[0].text`.

### 1.3 Azure LLM Speech („enhanced mode", Release „07 2026")

- **Modell-ID:** keine; Aufruf über denselben `transcriptions:transcribe`-Endpunkt mit `"enhancedMode":{"enabled":true,"task":"transcribe"}`. Die Release-Bezeichnung (aktuell „Azure LLM Speech 07 2026") ist keine wählbare ID.
- **Status:** GA seit Juni 2026 (Azure Updates AZ564387, **[Sekundär]** https://mc.merill.net/message/AZ564387). Dokumentation aktualisiert 19.10.2026.
- **Deutsch-Qualität:** Microsoft nennt „#2 auf Hugging Faces OpenASR-Leaderboard" (englischsprachig!) und „decisive quality leap across Tier 1 and Tier 2 languages", **ohne WER-Zahl und ohne Nennung von Deutsch**. Deutsch-WER: **[unbelegt]**. https://devblogs.microsoft.com/foundry/announcing-azure-ai-speech-llm-2607/
  Neu in 2607: bessere Satzzeichen, Großschreibung und Ziffernbehandlung in Tier-2/3-Locales, bessere Eigennamen, Latenz „bis zu 3x schneller" als 2605 (interne Angabe).
- **Freikontingent:** keines (F0: „Not applicable").
- **Preis:** laut Preisseite „LLM Speech is currently sharing the same price and SKU ID with Fast Transcription", also 0,36 $/Std. (bzw. Promo 0,10). **[belegt]** https://azure.microsoft.com/en-us/pricing/details/speech/ (Fußnote 9; die Preise selbst waren beim Abruf Platzhalter „$-", die Zahlen stammen aus der Retail Prices API)
- **Rate Limits:** 600 Anfragen/Minute, nicht erhöhbar.
- **Max. Datei:** < 500 MB, < 5 Stunden.
- **Streaming:** nein.
- **Vokabular/Prompting:** **Freitext-Prompt** (`enhancedMode.prompt`, z. B. „Output must be in lexical format." oder „Pay attention to *Name1*, *Name2*"), Prompt-Länge laut REST-Abschnitt 20.000 Zeichen, laut SDK-Abschnitt derselben Seite 4.096 Zeichen (Widerspruch in der Doku); Prompts ohne Sprachbezug werden ignoriert. Zusätzlich `phraseList` (2.000+ Einträge). **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/llm-speech
- **Halluzination:** LLM-basiert (multimodales Modell), daher grundsätzlich Halluzinations-/Übersetzungsrisiko (Modell kann bei Prompts „übersetzen"). Gemessene Halluzinationsraten **[unbelegt]**.
- **Datenschutz/Residenz:** nur in sechs Regionen: **centralindia, eastus, northeurope, southeastasia, westus, westus2**. In der EU also nur **northeurope (Irland)**. Fehlermeldung bei falscher Region: „Enhanced mode is currently not supported yet". **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions (Reiter „LLM speech")
- **Android-Integration:** wie Fast Transcription, nur mit `enhancedMode` im `definition`-JSON.

### 1.4 MAI-Transcribe-2 (Microsoft AI, 03.09.2026)

- **Modell-ID:** `MAI-Transcribe-2` (Aufruf: `enhancedMode.enabled=true`, `enhancedMode.model="MAI-Transcribe-2"` auf dem Fast-Endpunkt). Vorgänger `MAI-Transcribe-1.5` (Juni 2026) ist weiter unterstützt, **`MAI-Transcribe-1` wurde am 20.08.2026 abgekündigt**. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe
- **Status:** Public Preview, ohne SLA, „nicht für Produktion empfohlen". **[Sekundär, zitiert Microsoft Learn]** https://www.unite.ai/mai-transcribe-2-tops-fleurs-benchmark-across-60-languages-microsoft-says/ ; die Voice-Live-Tabelle führt `mai-transcribe-2` ebenfalls als „(preview)".
- **Deutsch-Qualität:**
  - Deutsch (`de`) ist in der Sprachliste von MAI-Transcribe-1.5 **und** -2 ausdrücklich enthalten. **[belegt]** (Learn-Seite oben, Modellkarte https://microsoft.ai/pdf/MAI-Transcribe-2-Model-Card.pdf)
  - FLEURS (Testsplit, Sprache automatisch erkannt): Durchschnitt **5,2 % über 60 Sprachen**, nach Microsoft Platz 1; 3,4 % über die 25 wichtigsten Sprachen. Wettbewerber laut Microsoft: Gemini 3.1 Pro 5,3 %, ElevenLabs Scribe v2 6,2 %, GPT-Transcribe 10,4 %, Whisper-v3-large 22,8 % (Durchschnitte über alle 60 Sprachen, stark von seltenen Sprachen geprägt, für Deutsch nicht aussagekräftig). **[Sekundär: Herstellerangaben, wiedergegeben von** https://www.unite.ai/mai-transcribe-2-tops-fleurs-benchmark-across-60-languages-microsoft-says/ **; Primärseite** https://microsoft.ai/news/mai-transcribe-2-is-the-fastest-most-accurate-and-cheapest-speech-recognition-model-in-the-world/ **nennt keine Zahlen im Text, die Tabelle ist ein Bild]**
  - Artificial Analysis AA-WER (nur Englisch): **2,0 %**, Platz 2, Geschwindigkeitsfaktor ca. 404–411x Echtzeit (1 Std. Audio in ca. 10 s). **[Sekundär]** Für Deutsch irrelevant.
  - **Deutsche Einzelzahl: [unbelegt]**. Die Modellkarte verweist auf ein Diagramm, dessen Zahlen sich aus dem PDF nicht extrahieren lassen.
- **Freikontingent:** keines (F0 „Not applicable"; Preisseite führt keine Freistunden).
- **Preis je Audiostunde:** Microsoft: **0,10 $/Std. als „limited-time offer"**, laut Presse „bis Ende des Jahres", ein Enddatum und der spätere Normalpreis sind nicht genannt (Preisseite Fußnote: „Limited-time promotional offer till 12/31/2026"). In der Retail Prices API stehen zusätzlich Meter „MAI-Transcribe-2 AzSp/ADM Audio" mit **0,54 $/Std.** (Gültigkeit ab 01.10.2026, u. a. westeurope, germanywestcentral, northeurope, swedencentral). Der Streaming-Ableger hat laut Sekundärquelle einen Einführungspreis von 0,54 $/Std. Welcher Betrag tatsächlich abgerechnet wird, ist **unklar**; für Franks Nutzung (Minuten bis wenige Stunden im Monat) ist der Unterschied egal (10 Std. = 1,00 bis 5,40 $). **[belegt für die Meter]** https://prices.azure.com/api/retail/prices (Filter `contains(meterName,'MAI-Transcribe')`)
- **Rate Limits:** keine eigene Angabe, vermutlich die 600 Anfragen/Minute des Fast-Endpunkts. **[unbelegt]**
- **Max. Dateigröße/Länge:** **300 MB / 2 Stunden**, nur WAV, MP3, FLAC (Modellkarte). Diarisierung verkürzt die zulässige Länge. Kein Stereo/Kanal-Support. **[belegt]**
- **Streaming/Echtzeit:** das Batch-Modell nicht. Separat **`MAI-Transcribe-2-Streaming`** (01.10.2026, Public Preview, ca. 100 ms bis zum ersten Text, AA-WER Streaming 2,5 % Englisch), Speech SDK ab 1.52 nötig (Release Notes: SDK 1.52 „Added support for selecting a streaming model"). Regionen laut Sekundärquelle Sweden Central, Central US, Southeast Asia (East US 2 „coming soon"); Sprachliste für Streaming **nicht bestätigt, Deutsch unklar**; Preis laut Sekundärquelle 0,54 $/Std. **[Sekundär]** https://letsdatascience.com/news/microsoft-releases-streaming-transcription-and-voice-models-b394c738 , https://www.llmreference.com/provider/microsoft-foundry/mai-transcribe-2-streaming
- **Vokabular/Prompting:** `phraseList.phrases` („Keyword Biasing", Hinweise statt Zwang; Microsoft nennt für Vorgänger 1.5 bis zu 30 % WER-Reduktion auf FLEURS). **Kein Freitext-Prompt.** Stil per `modelOptions.transcribeStyle`: `clean` (ohne Füllwörter) oder `verbatim` (Standard!). `locales` optional, aber „very strong hint" (nur setzen, wenn sicher). Wort-Zeitstempel optional. **[belegt]** Learn-Seite oben.
- **Satzzeichen:** Modell liefert formatierte Texte (Beispielausgabe der Modellkarte mit Punkten und Zeilenumbrüchen); mit `clean` lesbarer Fließtext. Eigenes Diktat-Verhalten (gesprochene Satzzeichen „Komma") **[unbelegt]**.
- **Halluzination:** Modellkarte nennt „strong performance in noisy environments"; unabhängige Halluzinationsmessungen für Deutsch **[unbelegt]**. Gefahr bei `verbatim` als Standard: Füllwörter („äh") landen im Text, für Diktat `clean` setzen.
- **Datenschutz/Residenz (wichtig):** Laut Modellkarte „can be accessed globally. The model is currently served from following regions to which requests are routed": East US, West US, West US 2, **North Europe**, Central India, Southeast Asia. Die Regionsdokumentation zeigt für die Fast-API ebenfalls diese sechs. Ob Anfragen einer EU-Ressource im EU-Raum bleiben (Datenresidenz) oder in andere Regionen geroutet werden, ist **widersprüchlich/unklar** (Regions-Doku: „stores and processes speech data in the region where you create your resource"; Modellkarte: globale Zuleitung). Für die Voice-Live-Variante gibt die Regionsseite „Data zone" für EU-Regionen an. **[belegt, aber widersprüchlich]**
- **Latenz:** 1 Std. Audio in ca. 10 s (Hersteller, AA-Wert 403,6x). Die Pressemitteilung nennt „unter 15 Sekunden", eine Vergleichstabelle des 1.5-Beitrags 53 s für beide Vorgänger (Widerspruch). Für Diktatschnipsel von 5–20 s wird die Latenz vom Netz und Upload dominiert, **[unbelegt]** gemessen.
- **Verfügbarkeit außerhalb Azure:** OpenRouter `microsoft/mai-transcribe-2`, `POST https://openrouter.ai/api/v1/audio/transcriptions`, JSON mit `model` und `input_audio` (Base64 + Format). Ein Beispielantwort-Kostenwert (0,000508 $ für 9,2 s) entspricht ungefähr 0,20 $/Std., kein veröffentlichter Tarif. Keine EU-In-Region-Weiterleitung für dieses Modell. **[belegt]** https://openrouter.ai/microsoft/mai-transcribe-2
- **Android-Integration (REST):**
  ```
  POST https://<ressource>.cognitiveservices.azure.com/speechtotext/transcriptions:transcribe?api-version=2025-10-15
  Ocp-Apim-Subscription-Key: <key>
  multipart: audio=@clip.wav ; definition={"enhancedMode":{"enabled":true,"model":"MAI-Transcribe-2","modelOptions":{"transcribeStyle":"clean"}},"locales":["de"],"phraseList":{"phrases":["Jarvis","..."]}}
  ```
  Die Ressource muss in einer der sechs Regionen liegen (für die EU: `northeurope`). Franks bestehender TTS-Schlüssel gilt nur in der Region, in der er angelegt wurde.

### 1.5 Whisper und OpenAI-Modelle in Azure (nur zur Einordnung, gehört zu einem anderen Researcher)

- „Whisper via Azure OpenAI" ist laut Regionstabelle u. a. in westeurope, swedencentral, norwayeast, eastus2 verfügbar; „Whisper via batch transcription" in westeurope, uksouth u. a. **[belegt]** https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions
- Retail-Preise (21.10.2026): `gpt-transcribe` (Global) 0,27 $/Std., Data Zone 0,297 $/Std. **[belegt]** https://prices.azure.com/api/retail/prices

### 1.6 Azure Batch Transcription

- 0,18 $/Std., F0 „not supported", Phrase List nicht unterstützt, asynchron über Blob-Speicher (Job-Warteschlange). Für Diktat ungeeignet. **[belegt]**

### 1.7 Custom Speech

- Eigenes Modell auf Text-/Audiodaten, Training 10 $/Std. (S1), Hosting 1,29 $/Tag je Endpunkt (S1 Custom Speech Model Hosting Unit; F0: 1 Modell frei, „Unused models will be automatically decommissioned after 7 days"). Für privates Diktat mit wenigen Eigennamen Overkill, Phrase List reicht. **[belegt]** Retail Prices API und Preisseite.

---

## 2. Amazon Transcribe

- **Modell-ID:** keine wählbare ID, Sprache `de-DE` (auch `de-CH`).
- **Status:** GA.
- **Deutsch-Qualität:** FLEURS Deutsch **8,0 % WER** („AWS Transcribe", Streaming-Benchmark von AssemblyAI, 2026-08/09); Mittel über unterstützte Sprachen 8,9 %. **[Sekundär, Herstellerbenchmark eines Konkurrenten]** https://www.assemblyai.com/benchmarks . Besser als Azure Echtzeit (9,1 %), aber deutlich schlechter als Whisper-large-v3 (4,3 % laut NVIDIA-Paper, andere Normalisierung).
- **Freikontingent:** Preisseite: „**60 Minuten pro Monat für 12 Monate**", Start mit der ersten Transcribe-Anfrage, über alle Regionen gepoolt, nicht übertragbar; gilt nicht für Redaction und Custom Language Models. **[belegt]** https://aws.amazon.com/transcribe/pricing/
  Neue AWS-Free-Tier-Regeln (seit Juli 2025): Neukonten bekommen statt klassischer 12-Monats-Kontingente **100 $ Guthaben bei Anmeldung plus bis zu 100 $** für Einführungsaufgaben, innerhalb eines „Free Plan" von **6 Monaten** (danach Kontoschließung, wenn nicht auf Paid gewechselt); Konten vor Juli 2025 behalten die alten 12-Monats-Vorteile. **Amazon Transcribe steht nicht auf der Free-Tier-Übersicht** und ob Guthaben Transcribe abdecken, ist **[unbelegt]**. https://aws.amazon.com/about-aws/whats-new/2025/07/aws-free-tier-credits-month-free-plan/ , https://aws.amazon.com/free/
  Praktisch: 60 Min./Monat sind nur 2 Minuten Diktat pro Tag, also sehr knapp.
- **Preis je Audiostunde (AWS-Preisliste, Veröffentlichung 11.09.2026, eu-central-1 Frankfurt, Basisstufe):** Batch 0,0001 $/s = **0,36 $/Std.** (0,006 $/Min.), Streaming 0,0001667 $/s = **0,60 $/Std.** (0,01 $/Min.); Custom Language Model Batch/Streaming 0,0001 $/s zusätzlich je Job. Mengenrabatte erst ab 250.000 Min./Monat. **[belegt]** https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/transcribe/current/index.json
- **Rate Limits:** 25 gleichzeitige Streams, 25 `StartStreamTranscription`/s, 25 `StartTranscriptionJob`/s, 250 gleichzeitige Batch-Jobs (alle erhöhbar). **[belegt]** https://docs.aws.amazon.com/general/latest/gr/transcribe.html
- **Max. Dateigröße/Länge:** Batch 2 GB / 28.800 s (8 Std.); Mindestlänge 500 ms. **[belegt]** (Quota-Seite oben)
- **Streaming/Echtzeit:** ja (HTTP/2 und WebSocket), `de-DE` Batch und Streaming, Zahlen-Transkription („Transcribing numbers") und Akronyme in beiden Modi, **Custom Language Models für `de-DE` in beiden Modi**, Redaction ja. **[belegt]** https://docs.aws.amazon.com/transcribe/latest/dg/supported-languages.html
- **Vokabular/Prompting:** Custom Vocabulary (Tabellenformat mit `Phrase` und `DisplayAs`; je Datei höchstens 50 KB, je Eintrag 256 Zeichen, 100 Vokabulare pro Konto, regionsgebunden), kostenlos in der Basisgebühr; Vokabularfilter; Custom Language Model kostet extra. Kein Freitext-Prompt. **[belegt]** https://docs.aws.amazon.com/transcribe/latest/dg/custom-vocabulary.html
- **Halluzination:** klassisches Modell; Belege **[unbelegt]**.
- **Datenschutz/Residenz:** Region frei wählbar; Frankfurt (`eu-central-1`) bietet Batch und Streaming, Zürich ebenfalls. Nutzung von Inhalten zur Modellverbesserung: **[unbelegt]** (AWS-Opt-out-Richtlinien nicht geprüft).
- **Latenz:** Herstellerzahlen **[unbelegt]**.
- **Android-Integration:** **ungünstig.** Batch verlangt S3-Upload plus `StartTranscriptionJob` plus Polling (Sekunden bis Minuten), das ist kein Diktat. Streaming braucht SigV4-signierte HTTP/2- oder WebSocket-Verbindung (AWS SDK for Kotlin/Java oder Presigned-URL) und IAM-/Cognito-Zugangsdaten. Es gibt **keinen** einfachen REST-Upload-Aufruf wie bei Azure/Groq.

---

## 3. IBM Watson Speech to Text

- **Modell-IDs:** `de-DE_Multimedia` (min. 16 kHz) und `de-DE_Telephony` (8 kHz), beide „next-generation", GA, mit Low-Latency-Unterstützung und automatischer Großschreibung (nur bei Deutsch). **[belegt]** https://cloud.ibm.com/docs/speech-to-text?topic=speech-to-text-models-ng
- **Deutsch-Qualität:** kein unabhängiger WER-Beleg gefunden, auch kein „Large Speech Model" für Deutsch in der Doku. **[unbelegt]**
- **Freikontingent:** Lite-Plan **500 Minuten/Monat** kostenlos; Dienste im Lite-Plan werden nach 30 Tagen Inaktivität gelöscht (Quelle: IBM-Katalogtext, wiedergegeben per Suche; die Katalogseite selbst war nicht abrufbar). Plus-Plan: 0,01 bis 0,02 $/Min. (IBM und G2 widersprechen sich), Premium auf Anfrage. **[Sekundär]** https://cloud.ibm.com/catalog/services/speech_to_text
- Rate Limits, Dateigröße, Residenz, Android-REST-Form: nicht vertieft, **[unbelegt]**. Wegen fehlender Qualitätsbelege und der Rolle von IBM im Spracherkennungsmarkt 2026 nicht weiterverfolgt.

---

## 4. Nvidia Riva / NIM (build.nvidia.com)

- **Modelle im Katalog (ASR-Filter, 100 von 106 Modellen sichtbar):** `canary-1b-asr` (mehrsprachig), `parakeet-1.1b-rnnt-multilingual-asr` (25 Sprachen), `parakeet-tdt-0.6b` (25 europäische Sprachen) und viele englische/chinesische/spanische Varianten. **[belegt]** https://build.nvidia.com/models?label=ASR
- **Deutsch-Qualität (Modell, nicht der gehostete Dienst):** `parakeet-tdt-0.6b-v3`: FLEURS Deutsch **5,04 %**, CoVoST Deutsch 4,84 % (Greedy, ohne Zeichensetzung/Großschreibung gemessen), Lizenz CC-BY-4.0, bis ca. 24 Min. Audio (A100) bzw. 3 Std. mit Local Attention. **[belegt]** https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3 ; im NVIDIA-Paper schlägt **whisper-large-v3 (4,30 %) und Canary-1B-v2 (4,40 %) das Parakeet auf Deutsch FLEURS**. https://arxiv.org/pdf/2509.14128
- **Freikontingent:** Ob es einen kostenlos gehosteten ASR-Endpunkt gibt, ist nicht belegbar: Die Katalogseite zeigt für ASR keine „Free Endpoint"-Marke, eine Suchmaske zeigte „Free Endpoint 2", die gefilterte Liste „0". Allgemein ist der NVIDIA-API-Katalog eine **Testversion von NIM** für Prototyping, Entwicklerprogramm-Mitglieder bekommen Zugang, Community-Beobachtung ca. **40 Anfragen/Minute**, Guthabenregel (frühere „1000 Credits") laut Forum widersprüchlich/entfernt. **[Sekundär/unklar]** https://forums.developer.nvidia.com/t/nvidia-nim-faq/300317
- **Preis:** kein öffentlicher Pay-as-you-go-Tarif für gehostetes ASR gefunden (Produktion = NVIDIA AI Enterprise). **[unbelegt]**
- **Android-Integration:** Riva/NIM-ASR arbeitet mit gRPC (Streaming), kein schlichter REST-Upload belegt. Für Android unpraktisch. **[unbelegt]**
- **Einordnung:** Die Modelle selbst sind offen (CC-BY-4.0) und lokal/selbst gehostet nutzbar, aber das ist nicht „kostenloses Kontingent" und liegt außerhalb dieses Themas.

---

## 5. Cloudflare Workers AI

ASR-Modelle im Katalog: `whisper`, `whisper-large-v3-turbo`, `nova-3` (Deepgram), `flux` (Deepgram, Sprachagenten, WebSocket), `whisper-tiny-en` (Beta, nur Englisch). **[belegt]** https://developers.cloudflare.com/workers-ai/models/?tasks=Automatic+Speech+Recognition

### 5.1 `@cf/openai/whisper-large-v3-turbo`

- **Status:** GA (kein Beta-Vermerk). **Dasselbe Modell wie bei Groq**, also keine zu erwartende Qualitätssteigerung für Deutsch; die Standardparameter (z. B. `beam_size: 5`) können leicht abweichen, ein Effekt ist **[unbelegt]**.
- **Freikontingent:** **10.000 Neurons pro Tag** auf Workers Free und Paid, Rücksetzung 00:00 UTC. Auf Free führt Überschreiten zu **Fehler** („further operations will fail with an error"), es wird nicht berechnet. **[belegt]** https://developers.cloudflare.com/workers-ai/platform/pricing/
  Umrechnung: 46,63 Neurons je Audiominute → 10.000 / 46,63 = **ca. 214 Audiominuten pro Tag** (rund 6.400 Min. ≈ 107 Std. pro Monat). Whisper (alt) 41,14 Neurons → ca. 243 Min./Tag. Nova-3 (HTTP) 472,73 Neurons → ca. 21 Min./Tag. Nova-3 (WebSocket) 836,36 → ca. 12 Min./Tag. Flux 700 → ca. 14 Min./Tag. (Rechnung von mir aus den Tabellenwerten.)
- **Preis (Workers Paid):** 0,011 $ je 1.000 Neurons; Whisper-Turbo **0,0005 $/Min. = ca. 0,031 $/Std.** (Modellseite: 0,000513 $/Min.), Nova-3 0,0052 $/Min. (Batch) bzw. 0,0092 $/Min. (WebSocket), Flux 0,0077 $/Min. **[belegt]** Preisseite oben, https://developers.cloudflare.com/workers-ai/models/whisper-large-v3-turbo/
- **Rate Limit:** ASR **720 Anfragen/Minute**. **[belegt]** https://developers.cloudflare.com/workers-ai/platform/limits/
- **Max. Dateigröße/Länge:** auf den Modellseiten nicht angegeben. **[unbelegt]**
- **Streaming:** Whisper-Turbo nein (Batch); Nova-3 und Flux per WebSocket.
- **Vokabular/Prompting (Whisper-Turbo):** `initial_prompt`, `prefix`, `language`, `task`, `vad_filter` (Standard aus), `beam_size` (5), `condition_on_previous_text` (true), `no_speech_threshold` (0,6), `compression_ratio_threshold` (2,4), `log_prob_threshold` (−1), `hallucination_silence_threshold` (optional). Ausgabe: `transcription_info.text`, `segments`, `vtt`. **[belegt]** Modellseite oben. Gute Anti-Halluzinations-Regler, die Groq so nicht offenlegt (dort nur `prompt`, `temperature`).
- **Nova-3 auf Cloudflare:** `language` (BCP-47), `punctuate`, `smart_format`, `numerals`, `keyterm`/`keywords`, `diarize` u. a.; **Deutsch/Mehrsprachigkeit auf der Modellseite nicht aufgeführt** („Available languages depend on the model and endpoint"). Deutsch-WER Deepgram Nova-3 (Herstellerangabe, Deepgram-eigener Test mit 26 Std. deutschem Produktions- und kuratiertem Audio, März 2026): **8,8 % batch**, Whisper in demselben Test 19,9 % (Produktion) bzw. 8,4 % (kuratiert). **[Sekundär, Hersteller]** https://deepgram.com/learn/german-benchmarks . AssemblyAIs Batch-Benchmark nennt für Nova-3 15,71 % global (nicht Deutsch). https://www.assemblyai.com/benchmarks
- **Halluzinationsverhalten:** wie Whisper allgemein (Wiederholungen/erfundene Texte bei Stille); die Parameter `hallucination_silence_threshold`, `vad_filter`, `no_speech_threshold` sind hier einstellbar.
- **Datenschutz/Residenz:** „Cloudflare does not use Customer Content processed by Workers AI to train AI models"; Eingaben/Ausgaben werden nur für die Verarbeitung gehalten (außer bei zustandsbehafteten Funktionen wie AI Gateway Logging, R2 usw.). **Wo inferiert wird (EU-Residenz): auf der Seite nicht angegeben**, also **[unbelegt]**. https://developers.cloudflare.com/workers-ai/platform/data-usage/
- **Latenz:** **[unbelegt]**.
- **Android-Integration (REST):** `POST https://api.cloudflare.com/client/v4/accounts/<ACCOUNT_ID>/ai/run/@cf/openai/whisper-large-v3-turbo` mit `Authorization: Bearer <API-Token>` (Berechtigung Workers AI) und JSON `{"audio": "<Base64 oder Zahlenfeld>", "language": "de", "initial_prompt": "..."}`. Das Audioformat wird auf der Modellseite nicht eindeutig beschrieben (dort „one of"-Typen), die genaue Eingabeform ist daher **[nicht bestätigt]** und vor dem Einbau mit einem Testaufruf zu prüfen. Alternativ ein kleiner Worker als Proxy mit eigenem Schlüssel.

---

## 6. Hugging Face Inference Providers

- **Modelle/Anbieter für ASR:** Empfohlen `openai/whisper-large-v3`; als Provider im Mapping: deepinfra, fal-ai, hf-inference, replicate, together. **[belegt]** https://huggingface.co/docs/inference-providers/tasks/automatic-speech-recognition
- **Freikontingent:** **Free-Nutzer: kein monatliches Guthaben** („Free Users — Monthly Credits: None ... yes (credits purchase required)"). PRO: 2,00 $/Monat, Team/Enterprise 2,00 $ je Seat; Abrechnung zum Anbieterpreis ohne HF-Aufschlag. Ältere Seiten/Mintlify-Sammlungen nannten „0,10 $/Monat" für Free, die aktuelle Doku widerspricht dem. **[belegt]** https://huggingface.co/docs/inference-providers/pricing
- **Preis je Audiostunde, Rate Limits, Dateigröße, Deutsch-WER, Residenz:** je Anbieter verschieden, nicht ermittelt. `hf-inference` selbst läuft seit Juli 2025 vorwiegend auf CPU und ist für große Whisper-Modelle kein verlässlicher Weg. **[belegt/unbelegt gemischt]**
- **Android-Form:** `POST https://router.huggingface.co/hf-inference/models/openai/whisper-large-v3` (Pfad nicht geprüft), Rohbytes oder Base64 `inputs`, Token mit „Inference Providers"-Berechtigung. **[unbelegt]** im Detail.
- **Fazit:** Kein sinnvolles Gratisangebot für Deutsch-Diktat; zudem dasselbe Whisper-Modell.

---

## 7. Tabelle „Kerndaten"

| Anbieter / Modus | Modell | Deutsch-Qualität (Beleg) | Gratis | Preis je Std. | Besonderheit |
|---|---|---|---|---|---|
| Groq (Basislinie) | whisper-large-v3-turbo | Referenz, FLEURS-DE für large-v3 4,30 % (NVIDIA-Paper) | (nicht mein Thema) | (nicht mein Thema) | Prompt-Parameter |
| Azure Echtzeit (S0/F0) | Standardmodell | FLEURS-DE 9,1 % (AssemblyAI, Sekundär) | **F0: 5 Std./Monat** | 1,00 $ | 1 gleichzeitige Anfrage im F0; REST max. 60 s |
| Azure Fast Transcription | Standard/mehrsprachig | kein WER; Q&A-Bericht Deutsch→Englisch bei Auto-Erkennung | keines | 0,36 $ (der 0,10-$-Promo-Meter gehört vermutlich zu MAI-Transcribe-2, Inferenz) | `locales:["de-DE"]` setzen, Phrase List |
| Azure LLM Speech 07 2026 | LLM, Enhanced Mode | kein WER, kein Deutsch genannt | keines | = Fast (0,36 $) | Freitext-Prompt, nur 6 Regionen, EU nur northeurope |
| **Azure MAI-Transcribe-2** | MAI-Transcribe-2 | 5,2 % Schnitt/60 Sprachen (Hersteller); **DE einzeln unbelegt** | keines | 0,10 $ befristet / Meter 0,54 $ | Preview, 300 MB/2 Std., `clean` setzen, Residenz unklar |
| Azure MAI-Transcribe-2-Streaming | Streaming-Variante | Englisch 2,5 % AA; DE unbestätigt | keines | ca. 0,54 $ (Sekundär) | Preview, SDK ≥ 1.52, Regionen SE/US/Asien |
| Amazon Transcribe | Standard | FLEURS-DE 8,0 % (AssemblyAI, Sekundär) | 60 Min./Monat, 12 Mon. (Altkonten) bzw. Guthaben (Neukonten) | Batch 0,36 $ / Stream 0,60 $ (Frankfurt) | Custom Vocabulary, CLM für de-DE; Android aufwendig |
| IBM Watson STT | de-DE_Multimedia | unbelegt | Lite 500 Min./Monat | Plus 0,60–1,20 $ (0,01–0,02 $/Min.) | Lite-Löschung nach 30 Tagen Inaktivität |
| Nvidia Parakeet-TDT-0.6B-v3 | offen/NIM | FLEURS-DE 5,04 % (Modellkarte) | gehostet unklar (ca. 40 RPM Test) | unbelegt | gRPC, Whisper v3 ist auf DE besser (4,30 %) |
| Cloudflare Whisper-Turbo | `@cf/openai/whisper-large-v3-turbo` | identisches Modell wie Groq | **10.000 Neurons/Tag ≈ 214 Min./Tag** | 0,031 $ | `initial_prompt`, Anti-Halluzinations-Regler, Fehler statt Kosten im Free-Plan |
| Cloudflare Nova-3 | `@cf/deepgram/nova-3` | Deepgram-eigen 8,8 % (DE batch) | ≈ 21 Min./Tag (HTTP) | 0,31 $ (HTTP) | Deutsch auf Modellseite nicht bestätigt |
| Hugging Face Providers | whisper-large-v3 u. a. | wie Whisper | **Free: nichts** (PRO 2 $/Monat) | je Anbieter | kein Gratisweg |

Umrechnung Preis je Minute: 0,36 $/Std. = 0,006 $/Min.; 0,10 $/Std. = 0,0017 $/Min.; 1,00 $/Std. = 0,0167 $/Min.

---

## 8. Fazit: Besser als Groq Turbo für Deutsch?

Hinweis zur Vergleichsbasis: Alle Zahlen für die Basislinie stammen von **whisper-large-v3**, nicht von **large-v3-turbo**. Ein Turbo-spezifischer FLEURS-Deutsch-Wert liegt in meinen Quellen nicht vor **[unbelegt]**; Turbo (destillierter Decoder) dürfte auf Deutsch etwas schlechter als large-v3 liegen. Der Abstand zu den 8–9 % von Azure-Standard und AWS ist groß genug, dass die Nein-Urteile halten, die 4,3 % sind aber nur ein Näherungswert für Turbo.

- **Azure Echtzeit-Standard (F0, gratis):** **Nein.** FLEURS-DE 9,1 % gegen 4,3 % für Whisper-large-v3 (Sekundärquellen, grob vergleichbar); nur 5 Std./Monat; REST auf 60 s begrenzt.
- **Amazon Transcribe:** **Nein.** 8,0 % FLEURS-DE, kaum Freikontingent für neue Konten, Android-Anbindung aufwendig.
- **Azure Fast Transcription / LLM Speech:** **Unklar, eher nein ohne Messung.** Kein Deutsch-WER, bekannter Spracherkennungsfehler bei Auto-Erkennung (Q&A), kein Freikontingent; aber Freitext-Prompt (LLM Speech) und Phrase List sind für Eigennamen stärker als Groqs Prompt.
- **MAI-Transcribe-2:** **Unklar, aber der einzige echte Kandidat.** Für: Deutsch offiziell unterstützt, Hersteller-FLEURS-Spitze (5,2 % Schnitt), Keyword Biasing (Phrase List), `clean`-Stil, Preis 0,10–0,54 $/Std. Gegen: Preview ohne SLA, kein Deutsch-Einzelwert, Residenz widersprüchlich, nur 6 Regionen (EU: northeurope), kein Gratiskontingent, Whisper-Vergleichszahlen von Microsoft sind über 60 Sprachen gemittelt und für Deutsch nicht aussagekräftig. **Nur ein Test mit Franks eigenen deutschen Diktaten (Eigennamen, Zahlen) kann entscheiden.** Kosten dafür: unter 1 US-Cent je Stunde Testaudio, erfordert eine Speech-Ressource in `northeurope` (oder eine der anderen fünf Regionen).
- **Cloudflare Workers AI:** **Nein für Qualität (dasselbe Modell), ja für „großes Gratiskontingent":** ca. 214 Min./Tag. Sinnvoll als Ausfall-Fallback oder Last-Verteiler neben Groq.
- **Hugging Face, Nvidia, IBM:** **Nein** (kein Gratisweg bzw. keine Qualitätsbelege bzw. kein Mehrwert gegenüber Whisper).

Empfehlung für den Test (Reihenfolge): (1) MAI-Transcribe-2 mit `locales:["de"]` **nicht setzen und setzen** (beides messen), `transcribeStyle: clean`, Phrase List mit Franks Eigennamen; (2) Azure LLM Speech mit Prompt „Pay attention to ...", Locale `de-DE`; (3) Cloudflare Whisper-Turbo als Kontrolle (muss Groq entsprechen); jeweils dieselben 20–30 Diktatclips, Fehlerzählung für Eigennamen, Zahlen, Satzzeichen.

---

BEST-PRACTICES-KANDIDATEN:
- Azure Speech Fast Transcription / LLM Speech / MAI-Transcribe-2 per REST: Aufrufmuster (`transcriptions:transcribe`, `api-version=2025-10-15`, `enhancedMode.model`, `phraseList`, `locales` setzen), F0-Ausschluss, Regionsbindung (LLM/MAI nur 6 Regionen). URL: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe , Stand Seite 22.09.2026 (Aktualisierung 14.10.2026), Modell MAI-Transcribe-2 / Azure LLM Speech 07 2026.
- Cloudflare Workers AI Free-Tier für STT: Neuron-Umrechnung (46,63 Neurons/Min. = ca. 214 Min./Tag), Fehler statt Kosten im Free-Plan, `initial_prompt` und `hallucination_silence_threshold`. URL: https://developers.cloudflare.com/workers-ai/platform/pricing/ , abgerufen 21.10.2026.
- Preisabfrage ohne Browser: Azure Retail Prices API (`https://prices.azure.com/api/retail/prices`) und AWS Price List (`https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/transcribe/current/index.json`, Version 2026-09-11), weil die Azure-Preisseite beim Abruf nur „$-" zeigt.
- STT-Vergleichsmethodik: FLEURS-Werte verschiedener Quellen sind wegen Normalisierung nicht direkt vergleichbar; Vergleich nur mit eigenen Clips. Quelle: https://www.assemblyai.com/benchmarks und https://arxiv.org/pdf/2509.14128

BUG-KANDIDATEN:
- Azure Fast Transcription erkennt deutsche Sprache trotz gesetztem `de-DE` als Englisch (Microsoft-Q&A, Juni 2025, keine offizielle Bestätigung; Abhilfe: Locale fest setzen, kurze Clips testen). URL: https://learn.microsoft.com/en-us/answers/questions/2284414/transcription-quality-issues-with-fast-transcripti , API-Version Fast Transcription ab 2024-11-15.
- MAI-Transcribe-1 am 20.08.2026 abgekündigt: Code, der `MAI-Transcribe-1` anspricht, muss auf `MAI-Transcribe-2` (oder 1.5) umgestellt werden. URL: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe , Stand 14.10.2026.
- MAI-Transcribe-2: `transcribeStyle` ist standardmäßig `verbatim` (Füllwörter, Selbstkorrekturen im Text), für Diktat explizit `clean` setzen. URL wie oben, Modell MAI-Transcribe-2.
- Doku-Widerspruch Azure LLM Speech: maximale Prompt-Länge 20.000 Zeichen (REST) gegenüber 4.096 Zeichen (SDK-Abschnitte) auf derselben Seite. URL: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/llm-speech , ms.date 02.10.2026, Release „07 2026".
- Azure-Preisseite liefert beim Abruf ohne JavaScript nur Platzhalter („$-"), die Zahlen kommen aus der Retail Prices API; zusätzlich ist unklar, ob Meter „Fast Transcription" (0,36 $) oder „Fast Transcription Promo" (0,10 $) bzw. „MAI-Transcribe-2" (0,54 $) abgerechnet wird. URL: https://azure.microsoft.com/en-us/pricing/details/speech/ , Stand 21.10.2026.
