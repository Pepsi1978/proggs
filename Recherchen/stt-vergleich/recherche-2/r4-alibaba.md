# R4 – Alibaba-Spracherkennung (Stand der Recherche: 22.10.2026)

Alle Preise: Alibaba Cloud Model Studio, Region **International / Singapur**, USD. Alle Quellen wurden am 22.10.2026 abgerufen; Seitenstände siehe Quellenliste. Hinweis zur Methodik: Webseiten wurden über WebFetch (Zusammenfassung durch ein kleines Modell) gelesen, nicht im Rohtext; Zahlen sind daher als "laut Abruf" zu verstehen.

## 1. Kernergebnis in einem Satz

Auf Artificial Analysis (AA) führt **Fun-Realtime-ASR-preview** (Alibaba) das STT-Ranking mit **AA-WER 1,7 %** an (Platz 1 von 62 Modellen). **Qwen3.5 Omni Plus** steht bei 3,5 %, **Qwen3.5 Omni Flash** bei 13,5 %. **Qwen3 ASR (Flash)** hat auf der AA-Seite keine eigene Zeile mit WER (nur eine Erwähnung in der Chunking-Notiz). Deutsch wird von den regulären Alibaba-ASR-Modellen unterstützt, aber **nicht von jedem Snapshot** (Falle, siehe 4).

## 2. Tabelle

| Modell (Alibaba-ID) | Erscheinung | AA-WER (AA-Index v2) | AA Speed Factor | AA-Preis | Alibaba-Preis Singapur | Deutsch? |
|---|---|---|---|---|---|---|
| **Fun-Realtime-ASR-preview** (bei AA; genaue Alibaba-Modell-ID unklar, nächste Treffer: `fun-asr-realtime` / `fun-asr-realtime-2026-02-28`) | bei AA nicht angegeben; `fun-asr-realtime-2026-02-28` = Snapshot vom 28.02.2026 | **1,7 %** (Rang 1/62) | 20,3 | 2,82 USD/1000 min (= 0,169 USD/h) | `fun-asr-realtime`: 0,00009 USD/s = **0,324 USD/h** (5,40 USD/1000 min) | `fun-asr-realtime`: ja (`de`); Snapshot `2026-02-28`: **nein** (nur zh/en/ja) |
| **Fun-ASR** (`fun-asr`, Datei) | Snapshot 07.11.2025, Doku-Stand 04/2026 | bei AA nicht gelistet | – | – | 0,000035 USD/s = **0,126 USD/h** | ja (30 Sprachen + Dialekte) |
| **Fun-ASR Flash** (`fun-asr-flash-2026-06-15`, Datei, ≤5 min) | 15.06.2026 (aus Snapshot-Name) | nicht gelistet | – | – | 0,000035 USD/s = 0,126 USD/h | ja |
| **Qwen3-ASR-Flash** (`qwen3-asr-flash`, Datei ≤5 min/10 MB, OpenAI-kompatibel) | Erstvorstellung **08.09.2025**; Flash-1208 (Dez 2025) laut Report; Snapshot 10.02.2026 (OpenRouter) | **nicht bei AA gelistet** (nur Chunking-Notiz) | – | – | 0,000035 USD/s = **0,126 USD/h** | ja (11 Sprachen laut Launch: u. a. Deutsch) |
| **Qwen3-ASR-Flash-Realtime** (`qwen3-asr-flash-realtime`, WebSocket) | wie oben | nicht gelistet | – | – | 0,00009 USD/s = **0,324 USD/h** | ja |
| **Qwen-Audio-3.0/3.1-ASR-Flash** (neue Generation, `qwen-audio-3.1-asr-flash`, `-streaming`, `-message`, `-filetrans`) | Tech-Report 07.09.2026 (v1) | bei AA nicht gelistet | – | – | 3.0: 0,000035 USD/s (Datei), 0,00009 USD/s (Streaming); 3.1: tokenbasiert 0,15 USD/1M Eingabe (Datei) bzw. 0,93 USD/1M (Streaming) | ja (30 Sprachen) |
| **Qwen3.5 Omni Plus** (`qwen3.5-omni-plus`) | ca. 30.03.2026, Snapshot 2026-03-15 | **3,5 %** | **137,3** | 0,00 (bei AA nicht eingepreist) | Audio-Eingabe 11 USD/1M Token, Text-Ausgabe 8,30 USD/1M | 60+ Eingabesprachen laut Doku; Deutsch nicht namentlich bestätigt |
| **Qwen3.5 Omni Flash** (`qwen3.5-omni-flash`) | wie oben, Snapshot 2026-03-15 | **13,5 %** | 69,7 | 0,00 | Audio-Eingabe 3 USD/1M Token, Text-Ausgabe 2,20 USD/1M | wie oben |

Zum Vergleich: AA-Nächste: MAI-Transcribe-2 2,0 %, Voxtral Small Transcribe 3 2,1 %, Scribe v2 2,2 %.

## 3. Erklärungen

### AA-WER und Einordnung
- AA-WER v2 = Audio-gewichteter Schnitt aus AA-AgentTalk (50 %), VoxPopuli (25 %), Earnings22 (25 %). Das sind überwiegend **englische** Sätze. Für Deutsch sagt die Zahl nur indirekt etwas.
- Fun-Realtime-ASR-preview 1,7 % ist die beste Zahl der Liste, aber der Name "preview" findet sich in keiner Alibaba-Doku. Welche Alibaba-ID dahintersteckt, ist nicht belegt. Vermutlich ein aktueller `fun-asr-realtime`-Stand.
- **Preis bei AA = chinesischer Preis:** 2,82 USD/1000 min entspricht 0,000047 USD/s, also dem **China-(Beijing)-Preis** von `fun-asr-realtime`. In Singapur kostet derselbe Dienst 0,00009 USD/s (fast das Doppelte). Das ist meine Ableitung aus den Zahlen, nicht von AA bestätigt.
- Qwen3.5 Omni 0,00 USD bei AA heißt "nicht eingepreist" (Tokenabrechnung), nicht "gratis". Omni Flash ist mit 13,5 % deutlich schlechter als Plus (3,5 %).
- Qwen3.5-Omni-Report (arXiv 2604.15804v2, 21.04.2026): Fleurs (top60) WER Plus 6,55 / Flash 10,75 (Gemini-3.1 Pro 7,32); **kein deutscher ASR-Einzelwert** im Report. Audio-Eingabe: 113 Varietäten (74 Sprachen + 39 Dialekte), Audio-Token-Rate 6,25 Hz.

### Deutsch-WER (soweit bekannt)
Qwen3-ASR-Technical-Report (arXiv 2601.21337v1, 29.01.2026), Anhang Tab. A.2:

| Benchmark (de) | Qwen3-ASR-0.6B | Qwen3-ASR-1.7B | Qwen3-ASR-Flash-1208 (API) |
|---|---|---|---|
| MLS | 9,52 | 6,05 | **4,11** |
| Common Voice | 9,44 | 5,85 | **3,53** |
| FLEURS | 6,48 | 3,92 | **3,03** |

Flash-1208 ist laut Report ein API-Stand "zur Referenz". Für Fun-ASR, Fun-ASR-Realtime und Qwen-Audio-3.x gibt es **keinen veröffentlichten Deutsch-WER** (Fun-ASR-Report nennt Deutsch nicht; Qwen-Audio-3.0-Report zeigt in Tabellen kein Deutsch: Makro-Fleurs 4,94 % vs. GPT-4o Transcribe 3,95 %, Common Voice 15 Makro 4,57 % vs. 8,76 %).

### Geschwindigkeit, Streaming
- AA-Speed-Factor (Audiodauer / Rechenzeit): Fun-Realtime 20,3; Omni Plus 137,3; Omni Flash 69,7. Latenzwerte für Streaming (AA-Streaming-Seite) waren nicht abrufbar (Daten nur in Diagrammen).
- Qwen-Audio-3.0-ASR-Streaming (Report, H100, Herstellerangabe): erstes Token unter 200 ms, Textanzeige unter 300 ms; bei 400 ms algorithmischer Latenz 9,24 % CER (zh), 13,38 % WER (en).
- Qwen3-ASR (Open-Source 0,6B/1,7B, vLLM): TTFT im Schnitt 92 bzw. 102 ms bei Parallelität 1.
- Echtzeit-Streaming per WebSocket: `fun-asr-realtime`, `qwen3-asr-flash-realtime`, `qwen-audio-3.x-asr-flash-streaming/-message`. Datei-Modelle: `qwen3-asr-flash` (synchron, 5 min/10 MB), `fun-asr` (12 h/2 GB).
- Qwen3-ASR-Flash-Realtime: Turn-Erkennung per VAD, `silence_duration_ms` Standard 800 ms (400 für Chat empfohlen).

### Geschätzte Antwortzeit für 30 Wörter ≈ 12 s Audio (Schätzung, nicht gemessen)
Annahmen: Standort Deutschland, Endpunkt Singapur (Netz-RTT ca. 170-200 ms, TLS/WebSocket-Aufbau ca. 2-3 RTT), Audio als komprimiertes 16 kHz.

| Variante | Rechenzeit (AA-Speed-Faktor) | Upload/Netz | Geschätzte Gesamtzeit nach Ende der Aufnahme |
|---|---|---|---|
| Fun-Realtime/`fun-asr-realtime` als Datei-Äquivalent | 12 s / 20,3 = 0,6 s | 0,3-0,8 s | ca. 1-1,5 s |
| Echtzeit-Streaming (Audio läuft während des Sprechens mit) | läuft parallel | – | nur Restverzögerung: ca. 0,3-0,8 s nach Sprechende (VAD/Final-Ergebnis; mit Handstopp ohne VAD eher 0,3-0,5 s) |
| `qwen3-asr-flash` (synchron, Datei) | keine AA-Zahl; Annahme ähnlich Omni-Klasse | 0,3-0,8 s | ca. 0,7-1,5 s |
| Qwen3.5 Omni Plus | 12 s / 137 = 0,09 s | 0,3-0,8 s | ca. 0,5-1,2 s (Ausgabe-Token-Streaming verlängert bei LLM-Antwort) |
| Qwen3.5 Omni Flash | 12 s / 70 = 0,17 s | 0,3-0,8 s | ca. 0,5-1,2 s (aber WER 13,5 %) |

Kosten für 12 s (Singapur): `qwen3-asr-flash` 0,00042 USD; `fun-asr-realtime`/`qwen3-asr-flash-realtime` 0,00108 USD; Omni Plus ca. 0,0012 USD (75 Audio-Token bei 6,25 Hz × 11 USD/1M + ca. 40 Text-Token × 8,30 USD/1M; Token-Rate in der API-Abrechnung nicht bestätigt); Omni Flash ca. 0,0003 USD.

### Freikontingent
- Neukunden, nur Region **Singapur**, Bereitstellungsmodus **International**, automatisch bei erster Aktivierung (Kontoangaben vollständig), gültig **90 Tage** ab Aktivierung/Modellveröffentlichung/Freigabe (je nachdem, was später ist).
- Laut Preisliste: **36.000 Sekunden (10 Stunden)** je Modell für `fun-asr-flash-2026-06-15`, `fun-asr-realtime`, `qwen3-asr-flash-realtime`, `qwen-audio-3.0-asr-flash*`; **1.000.000 Token** für `qwen-audio-3.1-asr-flash*`. Für `fun-asr` und `qwen3-asr-flash` zeigte der erste Abruf ebenfalls 10 Stunden, in der aktuellen Preisliste stehen diese beiden nicht mehr als eigene Zeilen (Abweichung, vor Nutzung im Konto prüfen).
- Bedingungen laut Free-Quota-Seite: deckt nur **Echtzeit-Inferenz**, **Batch-Aufrufe sind ausgeschlossen**; ASR-Modelle müssen **einzeln im Workspace freigeschaltet** werden; Kontingent je Modell, nicht übertragbar; danach automatisch Pay-as-you-go, außer "Free Quota Only" ist aktiv. Zu Omni (Plus/Flash) wird kein Freikontingent genannt.

### API-Zugang außerhalb Chinas
- Ja: Singapur (International), auch US (Virginia) für `qwen3-asr-flash`. API-Schlüssel sind **regionsgebunden** (Singapur ≠ Beijing).
- Neue Endpunktform mit Workspace-ID: `wss://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/api-ws/v1/inference` (Fun-ASR-Realtime, Streaming-Modelle) und `.../api-ws/v1/realtime?model=qwen3-asr-flash-realtime` (Qwen3-ASR-Realtime). Auth: `Authorization: Bearer <API-Key>`. Doku empfiehlt Migration auf diese Workspace-Domains (die ältere `dashscope-intl`-Form wurde in den abgerufenen Seiten nicht mehr genannt).
- OpenAI-kompatibel: **ja** für `qwen3-asr-flash` über `compatible-mode/v1` (Audio als öffentliche URL oder Base64-Data-URL; nicht für Region US). Realtime nutzt eigenes WebSocket-Protokoll, nicht die OpenAI-Realtime-Form 1:1 (nicht geprüft).

### Vokabel-/Kontext-Bias
- Fun-ASR-Realtime / Fun-ASR: **Hotwords** per `vocabulary_id` (vorab angelegte Hotword-Liste); für `fun-asr-realtime` zusätzlich Kontextparameter `raw_input`. Fun-ASR-Report: Hotword-Recall über 0,97 bei den meisten Themen.
- Qwen-Audio-3.x: Hotwords + Prompt-Kontext. Qwen3-ASR-Flash (Launch): Freitext-Kontext für Namen/Fachbegriffe; in der aktuellen Modelltabelle bei `qwen3-asr-flash` als "nicht gelistet" geführt, im Doku-Text aber "context enhancement".

### Datenschutz / Region
- Alibaba nennt: Eingabe/Ausgabe von API-Aufrufen werden nicht für Training oder Optimierung genutzt. Verschlüsselter Cache bis ca. 24 h; Zero-Data-Retention nur für Enterprise-Kunden mit Workspace-Einstellung. Eine Drittseite nennt abweichend 30 Tage Standardaufbewahrung (Widerspruch, offen).
- Rechenstandort bei "International": Singapur. Model Studio listet auch Frankfurt (Deutschland), ob dort ASR-Modelle laufen, wurde nicht belegt. Betreiber ist ein chinesischer Konzern (Alibaba Cloud International); für personenbezogene Sprachdaten DSGVO-Prüfung nötig.

## 4. Fallen / Risiken
1. **Snapshot-Falle Deutsch:** `fun-asr-realtime` (Basis) kennt Deutsch, aber `fun-asr-realtime-2026-02-28` nur Chinesisch/Englisch/Japanisch, und `fun-asr-flash-8k-realtime` nur Chinesisch. Bei AA bewertetes "Fun-Realtime-ASR-preview" kann also Deutsch unter Umständen gar nicht beherrschen. Vor jeder Nutzung gegen Deutsch testen.
2. AA misst überwiegend Englisch; die 1,7 % sind kein Deutsch-Beleg.
3. Kontingent verfällt nach 90 Tagen, deckt keine Batch-Aufrufe, Modelle einzeln freischalten.
4. Preisabweichung Singapur gegenüber AA-Preis (Faktor ca. 1,9 bei Streaming).
5. `paraformer-realtime-v2` (de unterstützt) nur in Beijing-Preisliste, Doku empfiehlt Migration auf Fun-ASR/Qwen-ASR.

## 5. Quellen (Abruf 22.10.2026)
- AA Speed-to-Text Non-Streaming (Tabelle Alibaba + Top 5): https://artificialanalysis.ai/speech-to-text (und https://artificialanalysis.ai/speech-to-text/non-streaming; 62 Modelle, Tabelle über https://artificialanalysis.ai/speech-to-text/models/qwen3-asr); benchmarklist-Spiegel Stand 02.06.2026: https://benchmarklist.com/benchmarks/artificial_analysis_speech_to_text_non_streaming/
- Alibaba Preise und Freikontingent (Seite abgerufen 22.10.2026): https://www.alibabacloud.com/help/en/model-studio/model-pricing
- Freikontingent-Regeln: https://www.alibabacloud.com/help/en/model-studio/new-free-quota.md
- Modellübersicht ASR (Sprachen, Hotwords, Limits; Stand Okt 2026): https://www.alibabacloud.com/help/en/model-studio/asr-model
- Fun-ASR (Datei): https://www.alibabacloud.com/help/en/model-studio/fun-asr
- Fun-ASR-Realtime (SDK, language_hints, Snapshots): https://www.alibabacloud.com/help/en/model-studio/fun-asr-realtime-python-sdk ; WebSocket: https://www.alibabacloud.com/help/en/model-studio/fun-asr-realtime-websocket-api
- Qwen3-ASR-Flash (OpenAI-kompatibel, Limits; Stand 11.10.2026): https://www.alibabacloud.com/help/en/model-studio/qwen-speech-recognition
- Echtzeit-ASR Endpunkte, VAD (Stand 15.10.2026): https://www.alibabacloud.com/help/en/model-studio/qwen-real-time-speech-recognition
- Qwen3.5 Omni Flash / Plus (Preise, Snapshot 2026-03-15; Stand 11.10.2026): https://www.alibabacloud.com/help/en/model-studio/qwen3-5-omni-flash ; https://www.alibabacloud.com/help/en/model-studio/qwen3-5-omni-plus
- Qwen3.5-Omni Technical Report (21.04.2026): https://arxiv.org/html/2604.15804
- Qwen3-ASR Technical Report (29.01.2026): https://arxiv.org/html/2601.21337v1 ; GitHub (Release 29.01.2026, Update 26.06.2026): https://github.com/QwenLM/Qwen3-ASR
- Qwen-Audio-3.0-ASR Technical Report (07.09.2026): https://arxiv.org/abs/2609.07549 ; Inhalt: https://arxiv.org/html/2609.07549v2
- Fun-ASR Technical Report: https://arxiv.org/html/2509.12508v4
- Qwen3-ASR-Flash Launch (08.09.2025): https://wbgsv0a.gigazine.net/gsc_news/en/20250910-qwen3-asr-speech-recognition-service ; https://tech.slator.com/?p=108291
- Datenschutz/Retention: https://www.alibabacloud.com/help/tc/model-studio/data-retention-policy ; https://docs.agent.bailian.aliyun.com/en/resources/agreements

## Nicht belegt / nicht gefunden
- AA-Streaming-Latenzwerte (Diagramme, nicht lesbar), AA-Release-Datum der Modelle, Alibaba-ID hinter "Fun-Realtime-ASR-preview", Deutsch-WER für Fun-ASR / Qwen-Audio-3.x, DSGVO-/Frankfurt-Verfügbarkeit von ASR, Audio-Token-Abrechnungsrate für Omni.

BEST-PRACTICES-KANDIDATEN:
- Alibaba Model Studio: Singapur-Region nutzt Workspace-ID-Endpunkte `wss://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/api-ws/v1/inference`; API-Key je Region getrennt; Freikontingent nur Singapur/International, 90 Tage, nur Echtzeit, ASR-Modelle einzeln freischalten. URL: https://www.alibabacloud.com/help/en/model-studio/new-free-quota.md und https://www.alibabacloud.com/help/en/model-studio/qwen-real-time-speech-recognition (Stand Doku 15.10.2026)
- Fun-ASR-Realtime: `vocabulary_id` (Hotword-Liste) vorab anlegen; `language_hints` nur ein Wert. URL: https://www.alibabacloud.com/help/en/model-studio/fun-asr-realtime-python-sdk

BUG-KANDIDATEN:
- Fun-ASR-Realtime Snapshot `fun-asr-realtime-2026-02-28` unterstützt Deutsch nicht (nur zh/en/ja), Basisalias `fun-asr-realtime` schon. URL: https://www.alibabacloud.com/help/en/model-studio/asr-model (Doku Stand Okt 2026, Version/Snapshot 2026-02-28)
- `qwen3-asr-flash` OpenAI-kompatibler Modus nicht in Region US (Virginia) verfügbar, obwohl das Modell dort gelistet ist. URL: https://www.alibabacloud.com/help/en/model-studio/qwen-speech-recognition (Stand 11.10.2026)
