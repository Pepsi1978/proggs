# R2: Microsoft MAI-Transcribe (alle Versionen), Stand 22.10.2026

Recherche am 22.10.2026. Alle Quellen wurden am 22.10.2026 abgerufen; das Quelldatum steht jeweils dabei.
Kennzeichnung: **[P]** = Primärquelle (Microsoft / Azure / Artificial Analysis), **[S]** = Sekundärquelle (Presse/Blogs).

## 1. Welche Versionen gibt es?

| Version | Erscheinungsdatum | Status laut Microsoft Learn |
|---|---|---|
| MAI-Transcribe-1 | 02.04.2026 [P] | **Deprecated seit 20.08.2026** [P] |
| MAI-Transcribe-1.5 | 02.06.2026 [P] | unterstützt (Modellname `MAI-Transcribe-1.5`) |
| MAI-Transcribe-2 (Batch) | 03.09.2026 [P] | aktuell; Public Preview, ohne SLA, nicht für Produktion empfohlen [S eesel + Voice-Live-Tabelle "(preview)"] |
| MAI-Transcribe-2-Streaming (Echtzeit, Begleitmodell) | 01.10.2026 [S, Microsoft-Blog bestätigt Preis/Latenz] | Public Preview, ohne SLA [S] |

Quellen:
- https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe (Learn-Seite, Änderungsdatum 14.10.2026): "The following models are supported: MAI-Transcribe-2, MAI-Transcribe-1.5, MAI-Transcribe-1: Deprecated on Aug 20, 2026."
- https://microsoft.ai/news/today-were-announcing-3-new-world-class-mai-models-available-in-foundry/ (02.04.2026, aktualisiert 09.06.2026)
- https://microsoft.ai/news/mai-transcribe-1-5more-accurate-context-aware-and-built-for-production/ (02.06.2026)
- https://microsoft.ai/news/mai-transcribe-2 (03.09.2026)
- https://microsoft.ai/news/our-first-streaming-transcription-model/ (01.10.2026)

Hinweis zum Widerspruch: eesel.ai schreibt, 1 UND 1.5 seien seit 20.08.2026 deprecated. Die Microsoft-Learn-Seite führt nur MAI-Transcribe-1 als deprecated. Maßgeblich ist Learn.

## 2. Vergleichstabelle

| Merkmal | MAI-Transcribe-1 | MAI-Transcribe-1.5 | MAI-Transcribe-2 (Batch) | MAI-Transcribe-2-Streaming |
|---|---|---|---|---|
| Datum | 02.04.2026 | 02.06.2026 | 03.09.2026 | 01.10.2026 |
| Sprachen | 25 (Top-25 nach MS-Nutzung; Liste nicht genannt) | 43 (**Deutsch `de` enthalten**) | 60 (**Deutsch `de` enthalten**) | 60, automatische kontinuierliche Spracherkennung |
| WER laut Microsoft, FLEURS-Durchschnitt | 3,9 % über 25 Sprachen [S: Suchtreffer zur Modellseite] | 4,9 % über 43 Sprachen [S: Suchtreffer zur Modellseite] | 5,2 % über 60 Sprachen [P]; 3,4 % über die Top-25-FLEURS-Sprachen [S eesel] | 2,5 % (Microsoft-Angabe, Benchmark unklar) [S datanorth] |
| WER Common Voice | nirgends genannt | nirgends genannt | nirgends genannt | nirgends genannt |
| **WER Deutsch (FLEURS de-DE o. ä.)** | **nicht veröffentlicht gefunden** | **nicht veröffentlicht gefunden** | **nicht veröffentlicht gefunden** | **nicht veröffentlicht gefunden** |
| WER laut Artificial Analysis (AA-WER, Mischwert aus AgentTalk 50 %, VoxPopuli 25 %, Earnings22 25 %, überwiegend Englisch) | 3,0 % (Platz 4 bei Erscheinen) [P AA 03.04.2026] | 2,4 % (Platz 3 bei Erscheinen, 02.06.2026) [P AA]; heute 2,4 % [P AA-Seite 22.10.2026] | 2,0 % (Platz 2, hinter Fun-Realtime-ASR-preview 1,7 %) [P AA-Seite 22.10.2026] | bei AA (Streaming-Liste) nicht gefunden |
| Geschwindigkeit (Speed Factor) | ca. 69x Echtzeit [P AA 03.04.2026] | ca. 276x bei Erscheinen [P AA 02.06.2026]; heute auf der AA-Seite 80,4x [P 22.10.2026]; "1 Std. Audio in unter 15 s" laut MS [P], ein Vergleichsbild nennt 53 s/Std. | 307,0x heute auf der AA-Seite [P 22.10.2026]; 410,7x Median laut eWeek [S]; MS: 10x schneller als GPT-Transcribe, 7x Scribe v2, 5x Gemini 3.5 Transcribe | Latenz statt Speed Factor (siehe unten) |
| Echtzeit-Streaming? | nein, Batch | nein, Batch (natives Streaming nur "angekündigt") | **nein** (Fast-Transcription-API, Datei-Upload) | **ja** (WebSocket / Speech SDK / Voice Live) |
| Latenz Streaming | – | – | – | erste Teiltexte knapp über 100 ms nach Audiobeginn, Endtext ca. 0,13 s nach Sprechende (Microsoft-Angabe, nicht unabhängig verifiziert) [P microsoft.ai + S] |
| Preis USD/Stunde Audio | 0,36 ($6 / 1000 Min.) [P] | 0,36 ($6 / 1000 Min.) [P AA] | **0,10** ($1,67 / 1000 Min.), befristete Aktion bis 31.12.2026; reguläre Zukunftspreise nicht veröffentlicht [P] | **0,54** ($9 / 1000 Min.), Einführungspreis bis Jahresende [P microsoft.ai] |
| Verfügbarkeit | Azure Speech in Foundry, Public Preview [P AA]; deprecated | Azure Speech in Foundry, MAI Playground | Azure Speech (Fast Transcription mit `enhancedMode`), Foundry, MAI Playground, OpenRouter, Voice Live (Preview) | Foundry, MAI Playground, Vercel AI Gateway, OpenRouter, Voice Live; LiveKit "coming soon" |
| Vokabel-/Phrase-Liste | – | Keyword Biasing ja (senkt WER auf FLEURS um bis zu 30 %) [P] | **ja**: `phraseList.phrases` (Keyword Biasing, nur Hinweise, kein erzwungener Output) [P] | Keyword Biasing laut Modellseite für MAI-Transcribe-2 ja [P]; Parameter fürs Streaming nicht belegt |

Kernquellen zur Tabelle:
- AA MAI-Transcribe-1: https://artificialanalysis.ai/articles/mai-transcribe-1-everything-you-need-to-know (03.04.2026)
- AA MAI-Transcribe-1.5: https://artificialanalysis.ai/articles/mai-transcribe-1-5-new-speech-to-text-model-leading-the-accuracy-speed-pareto-frontier (02.06.2026)
- AA Live-Rangliste: https://artificialanalysis.ai/speech-to-text (abgerufen 22.10.2026: MAI-Transcribe-2 AA-WER 2,0 %, Speed 307,0, $1,67/1000 Min.; MAI-Transcribe-1.5 AA-WER 2,4 %, Speed 80,4, $6,00/1000 Min.)
- eWeek (Tabelle mit 410,7x, AA-Mediane, Vergleich): https://www.eweek.com/news/microsoft-10-cent-ai-transcription-mai-transcribe-2/
- eesel.ai (FLEURS 3,4 % Top-25, Vergleichswerte Gemini 3.1 Pro 5,3 %, Scribe v2 6,2 %, GPT-Transcribe 10,4 %, Whisper v3-large 22,8 %): https://www.eesel.ai/blog/mai-transcribe-2
- Streaming-Daten (2,5 % WER, 0,13 s, $0,54/h): https://datanorth.ai/news/microsoft-releases-mai-transcribe-2-streaming (02.10.2026) und https://microsoft.ai/news/our-first-streaming-transcription-model/

Wichtig zur Speed-Zahl: Die AA-Werte schwanken je nach Messzeitpunkt stark (MAI-Transcribe-1.5: 276x bei Erscheinen, 190,3x laut eWeek, 80,4x am 22.10.2026). Nicht als feste Größe behandeln.

## 3. Deutsch

- Deutsch ist ab 1.5 (43 Sprachen) und in 2 (60 Sprachen) offiziell unterstützt (Sprachtabelle `de` = German, beide Spalten). Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe
- Für MAI-Transcribe-1 (25 Sprachen) ist die Sprachliste nicht veröffentlicht; Deutsch ist nicht bestätigt.
- Eine Deutsch-spezifische WER (FLEURS de) steht weder auf der Microsoft-Seite, noch in den lesbaren Teilen der Model Cards (die PDFs waren nicht auswertbar, nur Tabellenbilder), noch bei Artificial Analysis. Artificial Analysis nennt nur Chinesisch (4,5 %) und Französisch (2,8 %) als Einzelsprachen für MAI-Transcribe-2 [S, Suchzusammenfassung]. **Deutsch-WER: nicht gefunden. Eigenmessung nötig.**
- Hinweis: Die FLEURS-Mittelwerte (3,9 / 4,9 / 5,2 %) sind über unterschiedliche Sprachmengen gemittelt und nicht direkt miteinander vergleichbar. Alle sind Herstellerangaben.

## 4. API-Weg (Batch, Version 1.5 und 2)

- Dienst: Azure Speech "Fast Transcription" mit `enhancedMode`, REST, synchron (Datei hoch, Text zurück). Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe (Stand 14.10.2026)
- Endpunkt: `POST https://<Ressource>.cognitiveservices.azure.com/speechtotext/transcriptions:transcribe?api-version=2025-10-15`
- Header: `Ocp-Apim-Subscription-Key: <Key>`; Body: `multipart/form-data` mit `audio=@datei.wav` und `definition={...}`.
- Minimal-Definition:
  ```
  definition={"enhancedMode":{"enabled":true,"model":"MAI-Transcribe-2"}}
  ```
  Mit Deutsch erzwingen: `"locales":["de"]` (nur eine Sprache möglich; Microsoft warnt: nur setzen, wenn die Sprache sicher ist, sonst Auto-Erkennung belassen).
  Phrase-Liste: `"phraseList":{"phrases":["Wort1","Wort2"]}`.
  Weitere: `diarization.enabled`, `modelOptions.timestamps` (`none`/`segment`/`word`), `modelOptions.transcribeStyle` (`verbatim` Standard / `clean`).
- Formate laut MAI-Seite: WAV, MP3, FLAC. (Allgemeine Fast-Transcription-Seite nennt zusätzlich OPUS/OGG, WMA, AAC, ALAW/MULAW in WAV, AMR, WebM, SPEEX; für MAI nur WAV/MP3/FLAC zugesichert.)
- Limits (Azure LLM Speech / Fast Transcription, Standard S0): Datei < 500 MB, < 5 Stunden pro Datei, 600 Anfragen pro Minute. Mit Diarization kürzere maximale Aufnahmelänge (Hinweis auf Learn-Seite). Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits (Stand 02.10.2026, Abruf 22.10.2026). Abweichend: ältere REST-Referenz-Seite (api-version 2025-10-15) nennt "< 2 Stunden, < 250 MB"; eesel nennt 300 MB. Die aktuellere Quota-Seite gilt.
- Nicht unterstützt bei MAI-Transcribe-2 laut Feature-Tabelle: Stereo-Kanaltrennung, Custom Prompting, Übersetzung. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/llm-speech
- Echtzeit: `mai-transcribe-2-streaming` über Voice Live (`input_audio_transcription.model`) bzw. WebSocket/Speech SDK (SDK 1.52.0 laut Sekundärquelle). Voice Live nennt `mai-transcribe-2` und `mai-transcribe-2-streaming` beide als "(preview)". Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions?tabs=voice-live

## 5. Regionen

Batch (Fast Transcription mit MAI-Transcribe): nur 6 Regionen: `centralindia`, `eastus`, `northeurope`, `southeastasia`, `westus`, `westus2`. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions?tabs=llmspeech (Stand 08.10.2026). Für Deutschland am nächsten: **`northeurope` (Irland)**; `germanywestcentral` und `westeurope` sind für MAI nicht gelistet.

Streaming über Voice Live: `mai-transcribe-2-streaming` nur in `centralus`, `eastus`, `eastus2`, `northcentralus`, `southcentralus`, `swedencentral`, `westcentralus`, `westus`, `westus2`, `westus3`, `southindia` (Quelle: gleiche Regions-Seite, Tab Voice Live; `swedencentral` = Global-Verarbeitung). Eine Sekundärquelle nennt dagegen nur Sweden Central, Central US, Southeast Asia (+ East US 2 bald): abweichend, die Learn-Tabelle ist aktueller.

Datenverarbeitung: Azure Speech verarbeitet Audio in der Region der Ressource; Voice Live ist eine Ausnahme (Global/Data zone möglich).

## 6. Preis und Freikontingent

- Azure-Retail-Preisliste (API, abgerufen 22.10.2026): "Fast Transcription" 0,36 USD/Stunde (seit 01.11.2024); neu "Fast Transcription Promo" **0,10 USD/Stunde** seit 01.09.2026. Quelle: https://prices.azure.com/api/retail/prices (Filter `productName eq 'Azure Speech'`, Region eastus). Azure-Preisseite: "MAI Transcribe-2 ... Limited-time promotional offer till 12/31/2026"; Fußnote: LLM Speech nutzt dieselbe SKU wie Fast Transcription. Quelle: https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/
- MAI-Transcribe-1.5: 0,36 USD/Stunde ($6/1000 Min.) [AA].
- Streaming: 0,54 USD/Stunde bis Jahresende [microsoft.ai]; danach unbekannt.
- **Freikontingent: gilt NICHT für MAI-Transcribe.** Die F0-Stufe (5 Audiostunden/Monat) betrifft laut Preisseite "Real-time Transcription" (Standard und Custom, geteilt, Batch ausgenommen). Die Quota-Seite führt für Fast Transcription und Azure LLM Speech bei Free (F0) "Not applicable". Quellen: https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/ und https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits
  Heißt: MAI-Transcribe braucht eine Standard-(S0)-Ressource. Ob ein Azure-Free-Account-Guthaben (Startguthaben) darauf anrechenbar ist, habe ich nicht geprüft (Seite "Free Azure services" wurde nicht gelesen).
- Das OpenRouter-Angebot habe ich nicht auf Preis/Gratisnutzung geprüft.

## 7. Geschätzte Antwortzeit für 30 Wörter (ca. 12 s Audio)

Annahmen (reine Rechnung, KEINE Messung):
- Rechenzeit Batch = 12 s / Speed Factor: bei 307x = 0,04 s; bei 80x (schlechtester AA-Wert 1.5) = 0,15 s; "1 Std. in <15 s" (240x) = 0,05 s.
- Upload: 12 s Audio als 16-kHz-Mono-WAV ca. 0,38 MB (MP3/FLAC kleiner, ca. 0,05 bis 0,2 MB). Bei 20 Mbit/s Upstream ca. 0,15 bis 0,2 s.
- Netz/Handshake (TLS, Region northeurope, Heimnetz Deutschland): 0,1 bis 0,3 s; Multipart-Overhead, Warteschlange, Autoscaling-Kaltstart des Dienstes: nicht dokumentiert, 0,2 bis 1 s Puffer.

Ergebnis Batch (MAI-Transcribe-2 / 1.5, Fast Transcription): **ca. 0,5 bis 1,5 s** Gesamtzeit, typisch rund 1 s. Dazu käme die Aufnahme selbst (12 s sprechen), bis die Datei komplett ist. Bei hohem Speed Factor ist die Netz- und Dienstverzögerung deutlich größer als die Rechenzeit.

Ergebnis Streaming (MAI-Transcribe-2-Streaming): Audio läuft schon während des Sprechens. Nach Sprechende ca. 0,13 s Endtext (Herstellerangabe) plus Netz-Hin-und-Rück (30 bis 80 ms nach Schweden) = **ca. 0,2 bis 0,4 s nach Sprechende** (Teiltexte schon nach ca. 100 ms). Region mit Deutschland-Nähe: `swedencentral`. Kosten: 0,54 USD/h statt 0,10 USD/h.

Kosten für 12 s Audio: Batch-Aktion 0,10 USD/h = 0,00033 USD; Streaming 0,54 USD/h = 0,0018 USD.

## 8. Weitere Funktionen und Einschränkungen

- MAI-Transcribe-2: Sprecher-Diarisierung, Wort-Zeitstempel, automatische Spracherkennung, Code-Switching, Keyword Biasing, Stil `verbatim`/`clean`, Rauschrobustheit. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe
- Batch-Modell ist nicht Streaming-fähig; Live nur mit dem Streaming-Begleitmodell.
- Alle WER-Zahlen von Microsoft sind Herstellerangaben (FLEURS = gelesene Sprache); Channel Insider warnt, ein 60-Sprachen-Durchschnitt sage nichts über reale Aufnahmen: https://www.channelinsider.com/ai/news-microsoft-mai-transcribe-2-ai-transcription/

## 9. Offene Punkte (nicht gefunden)

- Deutsch-WER (FLEURS de / Common Voice de) für alle Versionen.
- Common-Voice-Werte überhaupt.
- Model-Card-PDFs (https://microsoft.ai/pdf/MAI-Transcribe-1.5-Model-Card.PDF, https://microsoft.ai/pdf/MAI-Transcribe-1-Model-Card.pdf) enthalten Tabellen, aber mein Textabruf konnte sie nicht lesen. Dort könnte eine Sprachtabelle stehen.
- Reguläre (Nach-Aktion-)Preise ab 2027.
- AA-Streaming-Ergebnis für MAI-Transcribe-2-Streaming (laut Datanorth noch nicht gelistet).

## BEST-PRACTICES-KANDIDATEN:

1. MAI-Transcribe wird über die Azure-Speech-Fast-Transcription-API angesprochen: `enhancedMode.enabled=true` plus `enhancedMode.model="MAI-Transcribe-2"`, `api-version=2025-10-15`, Ressource in `northeurope`, `eastus`, `westus`, `westus2`, `centralindia` oder `southeastasia`. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe
2. `locales` nur setzen, wenn die Sprache sicher ist (starker Hinweis, nur eine Sprache möglich); sonst Auto-Erkennung. Für Fachbegriffe `phraseList.phrases` nutzen (Hinweise, kein erzwungener Output). Quelle: gleiche Seite.
3. 429-Fehler mit Retry-Logik abfangen (Autoscaling): https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits
4. Vor dem Einsatz mit eigenem deutschen Testset messen; Herstellerzahlen sind englisch-/mehrsprachig gemittelt.

## BUG-KANDIDATEN:

1. MAI-Transcribe-1 ist seit 20.08.2026 deprecated; Dienste, die den Modellnamen `MAI-Transcribe-1` fest verdrahten, brechen. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-transcribe (betrifft MAI-Transcribe-1)
2. Freier F0-Tarif funktioniert nicht für Fast Transcription / LLM Speech ("Not applicable"); eine F0-Ressource liefert hier keinen Gratis-Zugriff. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits (betrifft MAI-Transcribe-1.5, 2)
3. Widersprüchliche Dateilimits in der Doku: Quota-Seite 500 MB / 5 Std., REST-Referenz (api-version 2025-10-15) 250 MB / 2 Std.; eesel nennt 300 MB. Mit Diarization kürzere Länge. Betrifft MAI-Transcribe-2. Quellen: https://learn.microsoft.com/en-us/rest/api/speechtotext/transcriptions/transcribe und die Quota-Seite.
4. Regionen-Falle: MAI-Transcribe-Batch gibt es nur in 6 Regionen; Ressourcen in `westeurope` / `germanywestcentral` funktionieren für `enhancedMode.model="MAI-Transcribe-*"` laut Tabelle nicht. Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions?tabs=llmspeech (betrifft 1.5, 2)
5. Stereo-Kanaltrennung wird bei MAI-Transcribe-2 nicht unterstützt (Feature-Tabelle). Quelle: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/llm-speech
6. Speed-Factor-Werte in der Presse und bei AA schwanken stark (MAI-Transcribe-1.5: 276x / 190x / 80x); die Microsoft-Angabe "1 Std. in unter 15 s" widerspricht dem Vergleichsbild (53 s/Std.). Quellen: https://artificialanalysis.ai/speech-to-text und https://microsoft.ai/news/mai-transcribe-1-5more-accurate-context-aware-and-built-for-production/ (betrifft 1.5)
7. Preview ohne SLA: MAI-Transcribe-2 und -2-Streaming sind Public Preview, Microsoft rät von Produktion ab; Preis (0,10 / 0,54 USD/h) nur bis 31.12.2026 garantiert. Quellen: https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/ und https://www.eesel.ai/blog/mai-transcribe-2
