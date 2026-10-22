# R3 – Mistral Voxtral (Stand der Recherche: 22.10.2026)

Ergebnis vorab: "Voxtral Transcribe 3" existiert. Veröffentlicht am **20.10.2026** (zwei Tage alt) als **Public Preview**, Tier "Premier", Teil von "Voxtral 3". Neueste Version = Transcribe 3 (Modell-IDs `voxtral-small-transcribe-3-0`, `voxtral-mini-transcribe-3-0`, `voxtral-mini-transcribe-realtime-3-0`). Die Modell-IDs stammen aus einer Sekundärquelle (ai-tldr.dev); die offizielle Mistral-Doku-Seite nennt nur die Namen und den Endpunkt.

## 1. Modellübersicht

| Modell | Erschienen | Typ | Preis (USD/Min) | AA-WER (Artificial Analysis) | AA Speed Factor | Lizenz |
|---|---|---|---|---|---|---|
| **Voxtral Small Transcribe 3** | 20.10.2026 (Preview) | Batch | 0,004 (= 4,00 $/1000 min) | **2,1 %** (Platz 3) | 64,9 (Orcarouter nennt 66,6) | proprietär, nur API |
| **Voxtral Mini Transcribe 3** (4B) | 20.10.2026 (Preview) | Batch | 0,0035 | **2,2 %** (Platz 5) | **76,1** | proprietär, nur API |
| **Voxtral Mini Transcribe Realtime 3** | 20.10.2026 (Preview) | Streaming (WebSocket) | 0,008 | nicht bei AA gelistet | nicht anwendbar | proprietär, nur API |
| Voxtral Mini Transcribe 2 (V2) | 04.02.2026 | Batch | 0,003 | 3,6 % | 84,4 | API (Premier); Gewichte nicht offen |
| Voxtral Mini Transcribe Realtime (26.02, 4B) | 04.02.2026 | Streaming | 0,006 | nicht bei AA gelistet | – | **Apache 2.0, offene Gewichte (HF)** |
| Voxtral Small (24B, 25.07) | Juli 2025 | Audio-Chat und Transkription | 0,004 | 2,8 % | 55,2 | Apache 2.0 (offene Gewichte) |
| Voxtral Mini (3B, 25.07) | Juli 2025 | Batch | – | 3,8 % (DeepInfra, 1,00 $/1000 min) | 50,6 | Apache 2.0 |

Hinweis: Voxtral Mini Transcribe (25.07) und Voxtral Mini sind bei Mistral deprecated (Abkündigung 27.02.2026, Abschaltung 31.05.2026), Ersatz ist Mini Transcribe 2. Das Pendant im Chat bleibt `voxtral-small-latest` (= Voxtral 2).

Zwei Mistral-Seiten widersprechen sich beim Small-Preis (Haupttext 0,004; TL;DR "ab 0,0035"). Die Modellkarte, Artificial Analysis und die übrigen Quellen nennen 0,004.

## 2. Genauigkeit

### 2.1 Artificial Analysis (unabhängig; gewichteter Mix aus VoxPopuli, Earnings-22 und AgentTalk, also nicht deutsch-spezifisch)
- Small Transcribe 3: 2,1 % (Platz 3). Mini Transcribe 3: 2,2 % (Platz 5). Mini Transcribe 2: 3,6 %. Voxtral Small (2507): 2,8 %.
- Quelle: https://artificialanalysis.ai/speech-to-text (abgerufen 22.10.2026; die Seite nennt kein Messdatum). Gegenstück bei Orcarouter: https://www.orcarouter.ai/blog/voxtral-3-speech-family-release (21.10.2026).

### 2.2 Mistral-Angaben, mehrsprachig (FLEURS 45 Sprachen, Makro-Mittel; Herstellerangabe)
| Modell | Sprache vorgegeben | Sprache automatisch erkannt |
|---|---|---|
| Small Transcribe 3 | 4,0 % (ai-tldr) bzw. 4,1 % (andere Quellen) | 4,1 % bzw. 4,0 % |
| Mini Transcribe 3 | 4,5 % (ai-tldr) bzw. 4,6 % | 4,6 % bzw. 4,5 % |
| Whisper large-v3 (Vergleich) | 16,0 % | 14,9 % |

Die Quellen vertauschen die Reihenfolge "vorgegeben/erkannt" (die Chart-Werte sind in den Textauszügen nicht eindeutig zugeordnet). Als gesichert gilt nur: Small etwa 4,0 bis 4,1 %, Mini etwa 4,5 bis 4,6 %.
Quellen: https://mistral.ai/news/voxtral-3/ (20.10.2026), https://ai-tldr.dev/models/voxtral-transcribe-3/, https://aiweekly.co/alerts/mistral-voxtral-3-posts-41-wer-on-fleurs-beating-whisper

### 2.3 Realtime 3 (FLEURS 35 Sprachen, Herstellerangabe)
| Verzögerung | WER |
|---|---|
| 240 ms | 7,4 % |
| 480 ms | 6,4 % |
| 960 ms | 6,0 % |
| 2400 ms | 5,7 % |

Zum Vergleich: Mini Transcribe 3 offline 4,0 %, Gemini 3.5 Realtime 4,2 %, Microsoft AI Transcribe 2 Streaming 5,8 %.

### 2.4 DEUTSCH
- **Für Transcribe 3 hat Mistral KEINE Deutsch-Einzelwerte veröffentlicht.** Deutsch (DE) steht nur in der FLEURS-45-Sprachliste und fließt in den Makro-Mittelwert ein (https://mistral.ai/news/voxtral-3/). Der HF Open ASR Leaderboard enthielt in der Recherche keinen Voxtral-3-Eintrag (nicht gefunden).
- **Nächstliegender belastbarer Deutsch-Wert (Voxtral Mini Transcribe V2, Februar 2026, Vorgänger):**
  - FLEURS de: 3,54 %. Common Voice de: 4,35 % (Voxtral Realtime Paper, https://arxiv.org/html/2602.11298v1).
  - Zum Vergleich im Paper zu Voxtral 1 (2025), Quelle https://arxiv.org/html/2507.13264v1: Voxtral Mini Transcribe 3,54 % / 4,85 %, Voxtral Small 3,38 % / 3,74 %, Voxtral Mini 4,40 % / 6,07 %, Whisper large-v3 5,46 % / 6,25 %, Scribe 4,78 % / 3,52 %, GPT-4o mini Transcribe 3,76 % / 6,72 %.
- **Voxtral Realtime (26.02, offene Gewichte), Deutsch FLEURS / Common Voice:**

| Verzögerung | FLEURS de | Common Voice de |
|---|---|---|
| 160 ms | 9,50 % | – |
| 240 ms | 8,15 % | 11,13 % |
| 480 ms (empfohlen) | 6,19 % | 8,70 % |
| 960 ms | 4,87 % | 6,85 % |
| 2400 ms | 4,15 % | 5,66 % |

  Quellen: https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602, https://arxiv.org/html/2602.11298v1
- Schluss für Deutsch: Der Transcribe-3-Wert ist nur ableitbar. Erwartet wird Deutsch bei Small Transcribe 3 im niedrigen einstelligen Bereich (Vorgänger: 3,5 % FLEURS). Das ist eine Schätzung, kein Messwert.

## 3. Geschwindigkeit und Latenz
- Artificial Analysis Speed Factor (Median; Sekunden Audio pro Sekunde Rechenzeit, gemessen an längerem Audio): Small 3 = 64,9; Mini 3 = 76,1; Mini 2 = 84,4 (zum Vergleich in der Quelle: MAI-Transcribe-2 = 308,8; Fun-Realtime-ASR-preview = 20,3). Quelle: https://artificialanalysis.ai/speech-to-text und https://www.orcarouter.ai/blog/voxtral-3-speech-family-release
- Realtime 3: Latenz unter 500 ms (Mistral), Verzögerung einstellbar von 80 ms bis 2400 ms. Voller Sprachagent-Zug unter 1 s (Herstellerangabe). Quelle: https://mistral.ai/news/voxtral-3/, https://ai-tldr.dev/models/voxtral-transcribe-3/
- Realtime 26.02 (offen): Verzögerung per `transcription_delay_ms` in `tekken.json`, Vielfache von 80 ms zwischen 80 und 1200, oder 2400 ms; empfohlen 480 ms. Quelle: Hugging-Face-Modellkarte (oben).
- Echtzeit-Streaming: **ja** (Realtime 3 über WebSocket; Batch-Modelle unterstützen zusätzlich `stream` beim Ergebnis-Text). Nicht öffentlich messbar: Antwortzeit der Batch-API für Kurzclips (keine Veröffentlichung gefunden).

## 4. Preise und Kontingent
- Preise: siehe Tabelle in Abschnitt 1. Umrechnung: Small 0,24 $/Std., Mini 0,21 $/Std., Realtime 0,48 $/Std. Abrechnung pro Minute, in USD oder EUR (https://docs.mistral.ai/models/voxtral-small-transcribe-3-0).
- **Freikontingent / Experiment-Plan:** Es gibt einen kostenlosen "Experiment"-Plan (Telefonnummer-Verifizierung, keine Kreditkarte, Anfragen dürfen zum Training verwendet werden, Upgrade auf "Scale" für höhere Limits). **Audio-spezifische Limits oder Gratis-Minuten für Voxtral fand ich nirgends.** Allgemein genannte Zahlen (Sekundärquellen, evtl. veraltet): 1 Anfrage/s, 500.000 Tokens/Min, 1 Mrd. Tokens/Monat je Modell. Mistral verweist für Details auf die Limits-Seite in der Konsole (admin.mistral.ai/plateforme/limits). Ob die Premier-Modelle Transcribe 3 im Gratisplan nutzbar sind, ist nicht belegt (Public Preview, Tier "Premier"). Die Hilfeseiten help.mistral.ai lieferten beim Abruf HTTP 404. Quellen: Websuche-Treffer help.mistral.ai Artikel 450104/455206/225174 (nur Snippets), https://yangmao.ai/zh/deals/mistral-free-api-tier/ (geprüft 16.06.2026, niedrige Verlässlichkeit), https://mistral.ai/pricing (Free-Plan bezieht sich auf Le Chat, keine Audiozahlen).

## 5. API und Funktionen
- Endpunkt: `POST /v1/audio/transcriptions` (Basis-URL https://api.mistral.ai/v1; OpenAI-SDK-kompatibel mit `file`, `language`, `timestamp_granularities`, `stream`; `diarize` und `context_bias` per `extra_body`; `file_url` nicht kompatibel). Quelle: https://docs.mistral.ai/models/voxtral-small-transcribe-3-0 (Endpunkt) und Offline-Transcription-Doku (Parameter, noch für V2 beschrieben).
- Realtime: WebSocket, Sitzungen, Turn-Erkennung (End-of-Turn). Diarisierung und Turn-Erkennung lassen sich in einer Realtime-Sitzung nicht gleichzeitig aktivieren (Fehler). Quelle: https://docs.mistral.ai/studio/audio/speech-to-text/overview
- Kontext-Bias / Vokabelliste: ja, in allen drei Transcribe-3-Modellen (Schlüsselbegriffe und Freitext). Earnings-22-Keyphrase-Recall: 81,4 % ohne, 94,6 % mit Kontext (Small). Für V2 galt: bis 100 Begriffe, auf Englisch optimiert, andere Sprachen experimentell – für Transcribe 3 steht dazu nichts Neues (ungeprüft, ob Deutsch jetzt gleichwertig). Quellen: https://mistral.ai/news/voxtral-3/, https://mistral.ai/news/voxtral-transcribe-2
- Diarisierung: ja, alle drei Modelle. cpWER: Small 18,4 %, Mini 18,6 %, Mini Transcribe 2 30,9 %. Sprecherreferenzen (nur mit Diarisierung) offline möglich.
- Timestamps: Batch: nur Segment-Zeitstempel, **keine Wort-Zeitstempel** bei Transcribe 3 (laut Übersichtsseite); V2 bot Wort-Zeitstempel. Realtime: keine Zeitstempel (Endzeiten ableitbar). Quelle: https://docs.mistral.ai/studio/audio/speech-to-text/overview
- Sprachen: Batch 50+ (inkl. 24 EU-Amtssprachen, also Deutsch), Realtime 35+ bzw. 40+ (Mistral nennt beides). Kontextfenster 32k.
- Dateilimit: **für Transcribe 3 nicht dokumentiert** (Doku-Unterseite "specs-and-limits" gab beim Abruf 404). Bekannt: V2 bis 3 Stunden pro Anfrage; Playground bis 10 Dateien je 1 GB (mp3, wav, m4a, flac, ogg); Voxtral 1 (2025): 30 bzw. 40 Minuten wegen 32k-Kontext.
- Offene Gewichte: **Transcribe 3: nein** (laut benchlm.ai "no weights are published"; Self-Deployment nur auf Anfrage über den Vertrieb). **Offen**: Voxtral Mini 4B Realtime 26.02 (Apache 2.0, ca. 4,4B Parameter, BF16, Hugging Face `mistralai/Voxtral-Mini-4B-Realtime-2602`), Voxtral Mini 3B und Small 24B (25.07, Apache 2.0).

## 6. Rechnung: 30 Wörter ≈ 12 s Audio (Schätzung, nicht gemessen)

Annahme: Speed Factor ist Audio-Sekunden je Rechen-Sekunde. Er wurde von AA an längerem Audio gemessen, bei Kurzclips dominiert fester Overhead.
- Reine Rechenzeit: Small 3: 12 s / 64,9 = **0,18 s**; Mini 3: 12 s / 76,1 = **0,16 s**.
- Netz/Overhead: Upload ca. 100 bis 400 KB (je nach Format) ca. 0,05 bis 0,3 s, TLS und Round-Trip nach EU ca. 0,1 bis 0,3 s, Warteschlange/Modellstart nicht dokumentiert (0,1 bis 0,5 s).
- **Geschätzte Antwortzeit Batch (Small/Mini 3): ca. 0,6 bis 1,5 s, typisch rund 1 s** nach Ende der Aufnahme (Annahme: Datei liegt nach Aufnahmeende komplett vor).
- **Realtime 3** (Streaming während des Sprechens): Text steht ca. 0,25 bis 0,6 s nach Sprechende bereit (bei 240 bis 480 ms Verzögerung), plus Netz. Für Deutsch kostet 480 ms laut Vorgänger-Messung 6,2 % WER (FLEURS de, 26.02-Modell) statt 3,5 % offline. Preis 0,008 $/Min = bei 12 s Audio ca. 0,0016 $.
- Kosten pro 12-s-Diktat Batch: Small ca. 0,0008 $, Mini ca. 0,0007 $.

## 7. Offene Unsicherheiten
- Keine Deutsch-Einzelwerte für Transcribe 3 (Hersteller veröffentlicht nur Makro-Mittel).
- Freikontingent für Audio unbelegt.
- Dateilimit und Rate-Limit für Transcribe 3 unbelegt (Preview).
- Mistrals Launch-Chart ist im Textauszug nicht eindeutig; "vorgegeben/erkannt" und Mini-Werte (4,5 vs. 4,6) schwanken je Quelle.
- Die Erstquelle mistral.ai/news/voxtral-3/ wurde über WebFetch (Kurzfassung durch ein Hilfsmodell) gelesen, nicht im Rohtext.

## Quellen (URL, Datum)
- https://mistral.ai/news/voxtral-3/ – 20.10.2026 (Launch)
- https://mistral.ai/news/voxtral-transcribe-2 – 04.02.2026
- https://docs.mistral.ai/models/overview – abgerufen 22.10.2026 (Modellliste, Deprecations)
- https://docs.mistral.ai/models/voxtral-small-transcribe-3-0 – 20.10.2026 (Endpunkt, Preis, Kontext 32k)
- https://docs.mistral.ai/studio/audio/speech-to-text/overview – 2026-10 (Features)
- https://docs.mistral.ai/capabilities/audio_transcription (V2, deprecated-Sektion) – Parameter, 3 h, OpenAI-Kompatibilität
- https://artificialanalysis.ai/speech-to-text – 21./22.10.2026
- https://www.orcarouter.ai/blog/voxtral-3-speech-family-release – 21.10.2026
- https://ai-tldr.dev/models/voxtral-transcribe-3/ – Okt. 2026 (Sekundärquelle, Modell-IDs)
- https://aiweekly.co/alerts/mistral-voxtral-3-posts-41-wer-on-fleurs-beating-whisper – 20.10.2026
- https://benchlm.ai/models/voxtral-small-transcribe-3 – 20.10.2026
- https://www.marktechpost.com/2026/10/20/mistral-voxtral-3-speech-models-voice-agent-sdk/ – nur Treffer, Abruf 403
- https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602 – Realtime 26.02, Deutsch-Tabelle
- https://arxiv.org/html/2602.11298v1 – Voxtral Realtime Paper (Feb. 2026)
- https://arxiv.org/html/2507.13264v1 – Voxtral Paper (Juli 2025)
- https://yangmao.ai/zh/deals/mistral-free-api-tier/ – 16.06.2026 (Free-Tier, niedrige Verlässlichkeit)

BEST-PRACTICES-KANDIDATEN:
- Mistral Voxtral Transcribe 3 für Deutsch erst nach eigener Messung wählen: Hersteller liefert nur Makro-WER, die belastbaren Deutsch-Zahlen (FLEURS de 3,54 %, Common Voice de 4,35 %) gehören zum Vorgänger V2 (https://arxiv.org/html/2602.11298v1).
- Bei Realtime-Modellen die Verzögerung bewusst wählen: Deutsch 480 ms = 6,19 % WER, 2400 ms = 4,15 % WER (FLEURS de, Voxtral Realtime 26.02, https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602). Für Diktat mit kurzen Clips ist Batch (Mini Transcribe 3, 0,0035 $/Min) die genauere und billigere Wahl.
- Context-Bias bei Voxtral nur für Englisch optimiert, andere Sprachen experimentell (Stand V2, https://mistral.ai/news/voxtral-transcribe-2); für Deutsch vor Einsatz testen.

BUG-KANDIDATEN:
- KEINE belegten Bugs gefunden. (Hinweis kein Bug, aber Einschränkung: Realtime-Sitzung mit gleichzeitig Diarisierung und Turn-Erkennung liefert einen Fehler, dokumentiertes Verhalten; Quelle https://docs.mistral.ai/studio/audio/speech-to-text/overview, Voxtral Mini Transcribe Realtime 3.)
