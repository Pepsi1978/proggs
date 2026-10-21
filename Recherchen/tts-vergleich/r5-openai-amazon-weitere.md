# R5 — Große Plattformen und weitere API-Anbieter für deutsches TTS

Recherchezeitpunkt: 21.10.2026 (alle "Abruf"-Angaben = 21.10.2026). Engine C (Sonnet-Schwarm), Researcher R5.
Referenz: Franks Jarvis nutzt heute Google Chirp 3 HD (de-DE), Free-Tier 1 Mio. Zeichen/Monat (Stand `best-practices/apis/tts-provider.md`, 06/2026).
Testtext: `Recherchen/tts-vergleich/testtext.md` (Testtext A: Zahlen/Datum/Uhrzeit/Euro/Abkürzungen/Akronyme/Fremdwörter; Testtext B: wörtlich gesprochene Satzzeichen).

Methodik und Vorbehalte (wichtig für die Bewertung):
- Nur WebSearch/WebFetch. WebFetch liefert eine Zusammenfassung der Seite durch ein kleines Modell, keine Wortlaut-Kopie. Wo "im Wortlaut" steht, ist der Wortlaut so, wie ihn die Abruf-Zusammenfassung wiedergibt. Zahlen sind jeweils der offiziellen Seite zugeordnet; wo nur Drittseiten existieren, steht `extern`.
- Kennzeichnung: `offiziell` = Herstellerseite/Herstellerdoku, `extern` = Drittquelle, `UNBELEGT` = nichts Belastbares gefunden, `WIDERSPRUCH` = Quellen passen nicht zusammen.
- Es wurde nichts selbst gehört oder getestet. Alle Qualitätsaussagen stützen sich auf Dritt-Rankings (überwiegend Englisch).
- Kein Zugriff auf das SK-Verzeichnis; keine Zugangsdaten genutzt.

---

## 0. Kurzfazit (Reihenfolge nach Eignung für Frank)

| Rang | Anbieter / Modell | Einschätzung in einem Satz |
|---|---|---|
| 1 | **Mistral Voxtral TTS 3** (`voxtral-mini-tts-3-0`, 20.10.2026, Public Preview) | Mit Abstand beste belegte Arena-Platzierung dieser Gruppe (Elo 1218, Platz 12 von 98, Englisch) bei 13 $/Mio. Zeichen, Deutsch offiziell unter den 9 Sprachen; aber kein Freikontingent gefunden, keine Textnormalisierung (Zahlen/Abkürzungen müssen vorher ausgeschrieben werden), deutsche Voreinstellungsstimmen unbelegt. Kandidat für den Hörtest. |
| 2 | **Murf Falcon 2.1** | Elo 1153 (Platz 28), 10 $/Mio. Zeichen bzw. 0,01 $/Min., deutsche Stimmen (Björn, Erna, Lara, Ralf), IPA-Aussprachekontrolle, EU-Region; Preis- und Freikontingentangaben widersprüchlich. |
| 3 | **xAI/SpaceXAI Grok Voice TTS 1.0** | Elo 1150 (Platz 29; Zuordnung "SpaceXAI TTS" zu Grok Voice TTS 1.0 über den OpenRouter-Eintrag, extern, Artificial Analysis nennt xAI nicht), 15 $/Mio. Zeichen, Deutsch offiziell, Parameter `text_normalization` und `replace`, WebSocket-Streaming; Freikontingent und Datenschutz unklar. |
| 4 | **Amazon Polly Generative** (Vicki, Daniel, Lennart; AT Hannah; CH Sabrina) | Reife, EU-Region Frankfurt/Zürich, volle SSML-Kontrolle (`sub`, `say-as`, `break`), aber Qualität nur etwa auf Chirp-3-HD-Niveau (Elo 1066 vs. 1061, Englisch), nur 100.000 Zeichen frei in den ersten 12 Monaten, AWS-Zugang statt API-Key. |
| 5 | **Deepgram Aura-2** (Deutsch seit 12.12.2025) | Sieben deutsche Stimmen, 200 $ Startguthaben ohne Ablauf, aber 30 $/Mio. Zeichen, keine unabhängige Arena-Messung für Aura-2 gefunden, 2.000 Zeichen je Anfrage. |
| 6 | **OpenAI** (gpt-4o-mini-tts, tts-1, tts-1-hd) | **Nicht empfohlen: alle vier Sprachmodelle werden am 06.01.2027 abgeschaltet** (angekündigt 01.10.2026). Nachfolger ist die Realtime-API (`gpt-realtime-2.1-mini`), kein Ersatz am selben Endpunkt. |
| 7 | **Speechmatics TTS** | Ausgeschieden: laut Doku nur Englisch (4 Vorschau-Stimmen). |

Wichtige Einordnung: Das Ranking der Arena ist Englisch (US/UK). Eine eigene deutsche Arena-Tabelle existiert (Artificial Analysis, "Multilingual"), wurde aber nicht abrufbar gefunden; die per Suchtreffer belegten deutschen Plätze 1 und 2 gehören Cartesia Sonic 3.6 und Inworld (keine Anbieter aus R5). Für keinen R5-Anbieter gibt es einen belegten deutschen Elo-Wert.

---

## 1. Vergleichstabelle

Wechselkurs/Rechnung: alle Preise in USD, je 1 Mio. Zeichen, Stand 21.10.2026. Eigene Umrechnungen sind mit "(gerechnet)" markiert.

| Merkmal | OpenAI | Amazon Polly | Deepgram Aura-2 | Speechmatics | Murf | xAI Grok TTS | Mistral Voxtral TTS 3 |
|---|---|---|---|---|---|---|---|
| Aktuelle Modellnamen | `gpt-4o-mini-tts` (Snapshots 2025-03-20, 2025-12-15), `tts-1`, `tts-1-hd`; Nachfolger `gpt-realtime-2.1-mini` | Engines: Standard, Neural, Long-Form, Generative | `aura-2-<stimme>-de` | TTS Preview (kein Modellname dokumentiert) | Falcon 2.1 (Streaming), Gen2 (nicht streamend) | Grok Voice TTS 1.0 (`x-ai/grok-voice-tts-1.0` bei OpenRouter) | `voxtral-mini-tts-3-0` (Premier, Public Preview, 20.10.2026); Vorgänger `voxtral-mini-tts-2603` |
| Deutsch | ja (folgt Whisper-Sprachen; Stimmen "auf Englisch optimiert") | ja, deutsche Stimmen | ja, seit 12.12.2025 | nicht dokumentiert (nur Englisch) | ja ("German (Germany)") | ja (`de`, 20 Sprachen) | ja (9 Sprachen inkl. Deutsch) |
| Deutsche Stimmen | alle 13 Stimmen sprechen Deutsch (Akzent-Frage UNBELEGT) | de-DE: Vicki (Gen/Neural/Std), Daniel (Gen/Neural), Lennart (Gen), Marlene, Hans (nur Std); de-AT Hannah; de-CH Sabrina | elara, aurelia, lara, julius, fabian, kara, viktoria | – | Falcon: Björn, Erna, Lara, Ralf; Gen2 zusätzlich Matthias, Lia, Josephine u. a. | eve, ara, rex, sal, leo (jede Stimme spricht jede Sprache) | Voreinstellungsstimmen UNBELEGT; Stimmklonen aus ca. 3 s |
| Preis je 1 Mio. Zeichen | tts-1 15 $, tts-1-hd 30 $, gpt-4o-mini-tts tokenbasiert (siehe 3) | Standard 4 $, Neural 16 $, Generative 30 $, Long-Form 100 $ | PAYG 30 $, Growth 27 $ | 11 $ | Falcon 10 $ (WIDERSPRUCH, siehe 3), Gen2 30 $ oder 100 $ (WIDERSPRUCH) | 15 $ | 13 $ (Voxtral TTS 3), 16 $ (Voxtral TTS) |
| Freikontingent | keines (extern) | siehe Abschnitt 2 | 200 $ einmalig, ohne Ablauf | erste 1 Mio. Zeichen frei | UNBELEGT/WIDERSPRUCH | UNBELEGT/WIDERSPRUCH | nicht gefunden |
| Streaming | ja (Chunked, empfohlen `wav`/`pcm`); Nachfolger Realtime per WebSocket | Generative: bidirektionales Streaming (GA 26.03.2026) | REST-Stream + WebSocket | Chunk-Stream, erster Block meist unter 200 ms (Herstellerangabe) | WebSocket, TTFA unter 100 ms (Herstellerangabe) | WebSocket, `optimize_streaming_latency` 0–2 | ja, TTFA ca. 90 ms (Herstellerangabe) |
| SSML | nicht in der Doku erwähnt | ja, je Engine unterschiedlich | nicht belegt | nein | nicht erwähnt (IPA inline/Wörterbuch) | nein (eigene Tags, `replace`) | nein (34 Emotions-Tags, 2 Tempo-Tags) |
| Arena (Englisch, AA Provider Voice) | TTS-1 HD 1103 (Platz 40), TTS-1 1090 (47), GPT-Realtime-2 1075 (53); gpt-4o-mini-tts nicht gelistet | Generative 1066 (56), Long-Form 1029 (77), Neural 897 (92), Standard 831 (98) | Aura-2 nicht gelistet; Flux TTS 1052 (65) | nicht gelistet | Falcon 2 1153 (28), Gen2 986 (82) | "SpaceXAI TTS" 1150 (29; Zuordnung zu Grok über OpenRouter, extern) | Voxtral 3 TTS 1218 (12), Voxtral TTS 1091 (45) |
| Zum Vergleich | Google Chirp 3: HD: 1061 (Platz 60) | | | | | | |

Quelle der gesamten Arena-Zeile: https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice (Abruf 21.10.2026). Reiner Englisch-Test (Filter nur US/UK).

---

## 2. Freikontingente — sauber getrennt (Stand 21.10.2026)

### 2.1 Dauerhaft monatlich

| Anbieter | Angabe | Quelle |
|---|---|---|
| Amazon Polly Standard | "5 million characters per month", Zeitgrenze auf der Preisseite nicht genannt. WIDERSPRUCH/Vorsicht: Die Polly-FAQ sagt "new Amazon Polly customers can synthesize millions of characters for free each month for the first 12 months"; die Preisseite nennt für Neural/Long-Form/Generative ausdrücklich "first 12 months", für Standard keinen Zeitraum. Ob Standard dauerhaft ist, ist UNBELEGT. Für Deutsch ohnehin kaum relevant (Standard-Stimmen Marlene/Hans/Vicki sind die alte Generation). | https://aws.amazon.com/polly/pricing/ (offiziell), https://aws.amazon.com/polly/faqs/ (offiziell) |
| Murf | "$10 Guthaben pro Monat" nur aus Drittseiten, offiziell UNBELEGT | https://costbench.com/software/voice-apis/murf-api/ (extern, Suchergebnis) |
| xAI | Behauptetes "150 $/Monat" gegen Daten-Weitergabe-Programm nur aus Drittseiten, offiziell UNBELEGT; eine Drittseite behauptet Wegfall des 25-$-Guthabens am 13.04.2026 | https://perkstack.co/blog/xai-startup-credits , https://agentdeals.dev/vendor/xai (extern) |
| OpenAI, Deepgram, Speechmatics, Mistral | kein monatliches Dauer-Freikontingent gefunden | – |

### 2.2 Einmaliges Startguthaben

| Anbieter | Angabe | Quelle |
|---|---|---|
| Deepgram | "$200 in free credit", läuft nicht ab, bis verbraucht; gerechnet ca. 6,67 Mio. Zeichen bei Aura-2 (30 $/Mio.), 13,3 Mio. bei Aura-1 (gerechnet vom Abruf). PAYG ohne Mindestumsatz, ohne Kreditkartenpflicht zum Start. | https://deepgram.com/pricing (offiziell, 21.10.2026) |
| Speechmatics | "First 1 million characters free (about 20 hours of speech)"; ob monatlich erneuert: Seite sagt es nicht. | https://www.speechmatics.com/pricing (offiziell) |
| Murf | Drittseite: einmaliger Test mit 100.000 Zeichen, Concurrency 5; Startup-Programm "50 million free characters for a 3-month period" (offiziell, nur für Early-Stage-Startups) | https://murf.ai/api (offiziell), https://costbench.com/software/voice-apis/murf-api/ (extern) |
| OpenAI | Kein Startguthaben mehr für neue Konten (extern: agentdeals.dev, Perkstack Mai 2026); offiziell nicht bestätigt, aber auch nirgends angeboten | https://perkstack.co/blog/openai-api-free-credits (extern) |
| xAI | "25 $ einmalig" nur aus Drittseiten, WIDERSPRUCH zur Gegenbehauptung (siehe oben) | extern |
| Mistral | Preisseite nennt keinen API-Freitarif ("Free plan covers Vibe and chat … plus access to Mistral Studio") | https://mistral.ai/pricing (offiziell) |

### 2.3 AWS Free Tier seit 15.07.2025 (Credits-Modell)

Polly-FAQ (offiziell, Abruf 21.10.2026): neue Konten ab 15.07.2025 erhalten bis zu 200 $ Free-Tier-Credits, anwendbar auf berechtigte Dienste "including Amazon Polly"; Credits müssen innerhalb von 12 Monaten ab Kontoerstellung verbraucht werden.
AWS-Free-Seite (offiziell): 100 $ sofort, bis zu 100 $ weitere durch Erkunden, insgesamt 200 $ über 6 Monate; Free Plan endet nach 6 Monaten oder bei aufgebrauchten Credits; Free Plan nur ausgewählte Dienste, kein Skalieren über die Credits hinaus ("no charges and no surprise bills"). Polly wird auf der Free-Seite nicht namentlich genannt (daher: Polly im Free Plan UNBELEGT, in der Polly-FAQ aber ausdrücklich genannt).
Polly-Preisseite zusätzlich: "free plan will be available for 6 months after account creation"; "All Free Tier credits must be used within 12 months".
Fazit für Oktober 2026: Es gilt das Credits-Modell für Neukunden (ab 15.07.2025 angelegte Konten); die zeichenbasierten Monatskontingente stehen weiterhin auf der Preisseite (Neural 1 Mio./Monat, Long-Form 500.000, Generative 100.000, je "first 12 months"; Standard 5 Mio. ohne genannte Frist). Ob beide Mechanismen parallel greifen, ist nicht ausdrücklich erklärt.
Quellen: https://aws.amazon.com/polly/pricing/ , https://aws.amazon.com/polly/faqs/ , https://aws.amazon.com/free/

### 2.4 Abrechnungskonto und kommerzielle Nutzung

| Anbieter | Abrechnungskonto nötig? | Kommerzielle Nutzung |
|---|---|---|
| OpenAI | ja, Konto muss finanziert sein (extern: Aufrufe scheitern mit insufficient_quota ohne Guthaben) | API-Ausgabe gehört dem Kunden (siehe bestehende Notiz in tts-provider.md §17); Hinweispflicht "KI-Stimme" (siehe 6) |
| Polly | AWS-Konto; Free Plan ohne Kreditkartenzwang UNBELEGT (Seite sagt nichts) | "your Polly output belongs to you" (offiziell, FAQ) |
| Deepgram | PAYG ohne Kreditkarte | keine Einschränkung gefunden, aber nicht ausdrücklich geprüft |
| Speechmatics | über Credit-Konto | UNBELEGT |
| Murf | API-Key kostenlos beantragbar, Zahlungsmittel für Mehrverbrauch | UNBELEGT |
| xAI | Konto, Zahlung | UNBELEGT |
| Mistral | La Plateforme/Studio-Konto | Open-Weights der Version 26.03 sind CC BY-NC 4.0 (nicht kommerziell); die gehostete API ist der kommerzielle Weg. Voxtral TTS 3: "Premier"-Modell (Lizenztext in der Doku nicht genannt) |

---

## 3. Preise, Ratenlimits, Kostenbremse

### 3.1 OpenAI
- Preisseite (offiziell, https://developers.openai.com/api/docs/pricing , Abruf 21.10.2026):
  - `gpt-4o-mini-tts`: Text-Eingabe 0,60 $ je 1 Mio. Token, Audio-Ausgabe 12 $ je 1 Mio. Token.
  - `tts-1`: 15 $ je 1 Mio. Zeichen; `tts-1-hd`: 30 $ je 1 Mio. Zeichen.
  - `gpt-realtime-2.1-mini`: Audio Ein 10 $, Audio Aus 20 $, Text Ein 0,60 $, Text Aus 2,40 $ (je 1 Mio. Token). `gpt-realtime-2.1`: Audio Ein 32 $, Aus 64 $.
  - `gpt-live-1`: 0,05 $ pro Minute, sekundengenau.
  - Offizielle Schätzung "Preis je Minute" für gpt-4o-mini-tts/tts-1/tts-1-hd: auf der Preisseite UNBELEGT. Eine Drittquelle (creativeainews.com) rechnet für Realtime-2.1-mini ca. 0,024 $ pro gesprochene Minute (extern, Listenpreis) und für tts-1 0,090 $ je 1.000 englische Wörter.
- Ratenlimits `gpt-4o-mini-tts` (offiziell, https://developers.openai.com/api/docs/models/gpt-4o-mini-tts): Build 2.000 RPM / 150.000 TPM, Launch 10.000 RPM / 2 Mio. TPM, Grow 10.000 RPM / 8 Mio. TPM; maximale Eingabe 2.000 Token; `tts-1` laut Drittquelle 4.096 Zeichen je Anfrage. `gpt-realtime-2.1-mini`: Build 400 RPM / 200.000 TPM.
- Kostenbremse: Prepaid-Guthaben/Ausgabenlimits konnten auf den abgerufenen offiziellen Seiten nicht belegt werden: UNBELEGT.

### 3.2 Amazon Polly (offiziell, https://aws.amazon.com/polly/pricing/ , Abruf 21.10.2026)
- Je 1 Mio. Zeichen: Standard 4 $, Neural 16 $, Long-Form 100 $, Generative 30 $. Abrechnung "Speech or Speech Marks" (bei Generative nur Sprachanfragen). GovCloud: Standard 4,80 $, Neural 19,20 $.
- Quoten (offiziell, https://docs.aws.amazon.com/polly/latest/dg/limits.html): `SynthesizeSpeech` Generative 8 TPS, bis 26 gleichzeitige Anfragen; Neural 8 TPS (Burst 10), bis 18 gleichzeitig; `StartSpeechSynthesisStream` Generative 8 TPS, 8 gleichzeitig. Eingabe `SynthesizeSpeech`: bis 3.000 abrechenbare Zeichen (6.000 insgesamt, SSML-Tags zählen nicht), Audio-Ausgabe höchstens 10 Minuten. Asynchron (`StartSpeechSynthesisTask`) bis 100.000 abrechenbare Zeichen. Lexika: bis 100 je Konto, 40.000 Zeichen je Lexikon, 5 je Anfrage.
- Kostenbremse: AWS Free Plan kann laut AWS-Free-Seite nicht über die Credits hinaus skalieren ("Cannot scale beyond credits"); für Pay-as-you-go sind AWS-Budgets üblich, in den abgerufenen Seiten aber nicht beschrieben (eigene Einschätzung).

### 3.3 Deepgram (offiziell, https://deepgram.com/pricing)
- Aura-2: 0,030 $ je 1.000 Zeichen (PAYG), Growth 0,027 $; Aura-1: 0,015 $ / 0,0135 $ (die Seite nennt nur je 1.000 Zeichen; Umrechnung auf 1 Mio. gerechnet). Flux TTS 0,045 $ je 1.000 Zeichen. Wegen Aktion bis 31.12.2026 gibt es für Flux TTS Gegenwert-Credits bis 500 $.
- Concurrency (REST + WebSocket zusammen): PAYG bis 45, Growth bis 60. Growth ab 4.000 $/Jahr Vorauszahlung. Limit je Anfrage 2.000 Zeichen (Fehler 413) laut https://developers.deepgram.com/docs/text-to-speech.
- Kostenbremse: Prepaid-Guthaben ($200 Credit wird verbraucht, kein automatisches Nachladen genannt); auf Growth Überverbrauch mit 10 % Aufschlag.

### 3.4 Speechmatics (offiziell, https://www.speechmatics.com/pricing)
- 0,011 $ je 1.000 Zeichen (11 $/Mio., gerechnet); Credits in Paketen (12 Monate Laufzeit) oder Abo; Rabatte bis 25 %. Limits für TTS nicht genannt.

### 3.5 Murf
- WIDERSPRUCH: murf.ai/api sagt "$0.01 / min"; die Falcon-2-Doku sagt "1 cent per 1,000 characters"; Drittseiten nennen Falcon 0,01 $ je 1.000 Zeichen und Gen2 0,03 $ je 1.000 Zeichen; Artificial Analysis listet Falcon 2 mit 10 $/Mio. und Gen2 mit 100 $/Mio. Zeichen. Belastbar ist nur: Falcon = ca. 10 $ je 1 Mio. Zeichen laut AA; Gen2 unklar (30 $ vs. 100 $).
- Concurrency: "5 für US-East, 2 für alle anderen Regionen" (offiziell, Falcon-Doku/WebSockets-Seite), gleichzeitig wirbt die Seite mit "10,000+ concurrent calls" (nur Enterprise).
- Quellen: https://murf.ai/api , https://murf.ai/api/docs/text-to-speech-models/falcon-2-1 , https://murf.ai/api/docs/text-to-speech/web-sockets

### 3.6 xAI (offiziell, https://docs.x.ai/developers/models)
- Zeile "Text to Speech" 15 $ je 1 Mio. Zeichen. Abrechnung nach gesendetem Eingabetext (nicht nach `replace`-Ergebnis).
- Limits (offiziell, https://docs.x.ai/developers/model-capabilities/audio/text-to-speech): 60.000 Zeichen je Einzelanfrage, WebSocket höchstens 50 gleichzeitige Sitzungen je Team, Fehler 429 bei Überlast; konkrete RPM-Zahlen nicht genannt.

### 3.7 Mistral
- Voxtral TTS 3: 13 $ je 1 Mio. Zeichen (https://docs.mistral.ai/models/voxtral-mini-tts-3-0 , offiziell, Abruf 21.10.2026). Voxtral TTS (Vorgänger): 0,016 $ je 1.000 Zeichen = 16 $/Mio. (https://mistral.ai/news/voxtral-tts , offiziell, 23.03.2026).
- Ratenlimits: UNBELEGT. Textempfehlung: höchstens 300 Wörter je Prompt (Doku `voxtral-mini-tts-2603`).

---

## 4. Aussprachekontrolle und Textnormalisierung (Deutsch)

### 4.1 Amazon Polly — die umfassendste Kontrolle
SSML-Matrix (offiziell, https://docs.aws.amazon.com/polly/latest/dg/supportedtags.html , Abruf 21.10.2026):

| Tag | Neural | Long-Form | Generative |
|---|---|---|---|
| `break`, `lang`, `p`, `s`, `speak`, `sub`, `w` | voll | voll | voll |
| `say-as` | teilweise | voll | voll |
| `phoneme` | voll | voll | **teilweise** |
| `prosody` | teilweise | teilweise | teilweise |
| `mark` | voll | voll | teilweise |
| `emphasis`, `amazon:auto-breaths`, Flüstern, `max-duration` | nicht | nicht | nicht |
| `amazon:effect name="drc"` | voll | voll | nicht |

- WIDERSPRUCH: Die Einzelseite zu `phoneme` (https://docs.aws.amazon.com/polly/latest/dg/phoneme-tag.html) sagt "supported by long-form, neural, and standard" und nennt Generative nicht; die Matrix führt Generative als "Partial availability". Lies: Für Generative ist `phoneme` unsicher, im Hörtest prüfen.
- `say-as`-Werte: characters/spell-out, cardinal/number, ordinal, digits, fraction, unit, date (mit `format`: mdy, dmy, ymd, md, dm, ym, my, d, m, y, yyyymmdd), time (nur Dauer in Minuten/Sekunden, nicht Uhrzeit), address, expletive, telephone (u. a. de-DE). Hinweis: Neuronale Stimmen unterstützen `characters` nicht und fallen für den Satz auf die Standardstimme zurück, aber zum Neural-Preis abgerechnet. Für Generative nennt die Matrix "voll".
- Ob die deutschen Ausgaben von `say-as date`/`ordinal` korrekt gebeugt werden ("dem einundzwanzigsten Zehnten"), ist in der Doku nicht ausgeführt: UNBELEGT. Muss mit Testtext A geprüft werden.
- Lexika (PLS): bis zu 5 je Anfrage; Doku (https://docs.aws.amazon.com/polly/latest/dg/managing-lexicons.html) nennt keine Engine-Einschränkung, aber auch keine ausdrückliche Bestätigung für Generative: UNBELEGT.
- Wörtlich gesprochene Satzzeichen (Testtext B): Machbar durch Vorverarbeitung mit `<sub alias="Ausrufezeichen">!</sub>` bzw. Klartext-Ersetzung; `sub` ist in allen Engines "voll". Direkt eingebaut ist das nicht.
- Deutsche Textnormalisierung (Zahlen, Datum, € usw.) automatisch: die Doku trifft keine Aussage: UNBELEGT.

### 4.2 OpenAI
- `instructions` nur bei `gpt-4o-mini-tts`: steuert Akzent, Emotion, Intonation, Tempo, Ton, Flüstern (offiziell, https://developers.openai.com/api/docs/guides/text-to-speech). Ob eine Anweisung wie "Lies Satzzeichen wörtlich" zuverlässig befolgt wird: UNBELEGT (nicht getestet). Die Realtime-Nachfolger lesen Text laut Drittquelle nur dann wörtlich, wenn ein Prompt das verlangt ("a prompt to a reasoning model, not a guarantee", https://www.creativeainews.com/articles/openai-tts-1-shutdown-migration-tested-2026/ , extern).
- SSML: in der Doku nicht erwähnt. Deutsche Normalisierung: nicht dokumentiert (UNBELEGT). `tts-1`/`tts-1-hd` kennen weder Instructions noch SSML.

### 4.3 Deepgram
- Auf den abgerufenen Seiten steht nichts zu SSML, Aussprache-Wörterbuch oder Normalisierung (UNBELEGT). Im Launch-Beitrag wird als Beispiel "Ihr Termin ist für den 24. Mai um 14:30 Uhr bestätigt." genannt; ein belegter Vorteil bei deutschen Ordinalzahlen/Uhrzeiten ergibt sich daraus nicht (Marketingbeispiel). Quelle: https://deepgram.com/learn/aura-2-now-speaks-dutch-french-german-italian-japanese (offiziell).

### 4.4 Speechmatics
- Keine SSML-Unterstützung, keine Tempo-/Tonhöhensteuerung, Normalisierung nicht dokumentiert; Empfehlung der Doku: Abkürzungen und Zahlen ausschreiben (https://docs.speechmatics.com/text-to-speech/quickstart , offiziell).

### 4.5 Murf
- Aussprache: IPA direkt im Text oder `pronunciationDictionary` (offiziell, Falcon-Doku/WebSockets-Seite). SSML und Normalisierung nicht erwähnt (UNBELEGT).

### 4.6 xAI
- `text_normalization` (Boolean, Standard `false`): wandelt Zahlen, Abkürzungen, Symbole vor der Synthese in gesprochene Form um; deutschspezifisches Verhalten nicht beschrieben (UNBELEGT).
- `replace`-Map (bis 200 Einträge, Schlüssel nur Buchstaben/Ziffern/Apostrophe/Leerzeichen, Werte bis 128 Zeichen; Respelling oder IPA). Daraus folgt: Satzzeichen können nicht als Schlüssel dienen; Testtext B muss vor dem Senden selbst ersetzt werden (eigene Schlussfolgerung).
- Sprechtags: `[pause]`, `[long-pause]`, `[laugh]` u. a.; Wrapping-Tags `<whisper>`, `<slow>`, `<soft>`; `speed` 0,7–1,5. Quelle: https://docs.x.ai/developers/model-capabilities/audio/text-to-speech (offiziell). Kein SSML.

### 4.7 Mistral
- Doku `voxtral-mini-tts-2603` (offiziell): "Avoid markdown, emojis, and special characters, and spell out numbers and abbreviations." Also keine eingebaute Normalisierung; Testtext A muss vorher vollständig ausgeschrieben werden. Voxtral TTS 3: 34 Emotions-Tags, 2 Tempo-Tags (https://docs.mistral.ai/models/voxtral-mini-tts-3-0). Kein SSML belegt.

### 4.8 Zusammenfassung Testtext B (Satzzeichen wörtlich)

| Anbieter | Möglich? | Weg |
|---|---|---|
| Polly | ja, mit Vorverarbeitung | `<sub alias>` oder Klartext-Ersatz |
| OpenAI | UNBELEGT | `instructions` testen (nur gpt-4o-mini-tts) |
| Deepgram, Speechmatics, Murf, xAI, Mistral | nur durch eigene Textersetzung vor dem Senden | App-seitiger Ersatz ("Ausrufezeichen") |

---

## 5. Streaming, Antwortbeginn, Android-Eignung

| Anbieter | Streaming | Belegte Antwortbeginn-Zeit | Android-Eignung |
|---|---|---|---|
| OpenAI | Chunked HTTP (`wav`/`pcm` am schnellsten); Realtime per WebRTC/WebSocket/SIP | nur Relativangaben der Doku; Realtime: "p95-Latenz mindestens 25 % gesenkt" (OpenAI-Community, 06.07.2026); keine absolute TTFA | REST/WebSocket einfach per OkHttp; Key nicht in die App (Broker) |
| Polly | Generative: bidirektionales Streaming; Benchmark: 7.045 Zeichen, klassisch 115 s gegenüber Stream 70 s Gesamtzeit (39 % schneller; keine Zeit bis zum ersten Byte). Keine Speech Marks bei Generative. | keine TTFA-Zahl veröffentlicht (UNBELEGT) | AWS-Credentials (SigV4); Direktzugriff aus der App nur mit Cognito-Identitäten oder Broker (eigene Einschätzung, in den Quellen nicht dargelegt). Streaming-SDKs u. a. Kotlin, Java, JS; AWS CLI/Python nicht unterstützt |
| Deepgram | REST-Stream + WebSocket | "sub-200ms" (Herstellerangabe, ursprünglich englisch; für Deutsch keine eigene Zahl) | einfacher Bearer-Key; Key über Broker |
| Speechmatics | Chunk-Stream (kein vollständiges Bidirektional) | "unter 200 ms" erster Block (Herstellerangabe, nur Englisch) | – |
| Murf | WebSocket; Gen2 nicht mehr streamend | unter 100 ms (Herstellerangabe, Coval soll "geringere Zeit bis zum ersten gesprochenen Wort" gemessen haben, Zahlen nicht auf der Seite) | WebSocket, Idle-Session schließt nach 3 Minuten; Concurrency 5 (US-Ost)/2 (sonst) |
| xAI | WebSocket `wss://api.x.ai/v1/tts`, Barge-in per `text.clear` | keine absolute Zahl; WebSocket-Handshake ca. 600 ms bei weit entfernten Clients | Formate MP3/WAV/PCM/µ-law/A-law, 8–48 kHz |
| Mistral | ja | Voxtral TTS 3: ca. 90 ms TTFA (Modell-Seite). Frühere Doku `voxtral-mini-tts-2603`: ca. 0,8 s (PCM) bzw. ca. 3 s (MP3) Ende-zu-Ende; Empfehlung PCM für Streaming | PCM `float32` (nicht int16!) beachten |

Quellen: https://developers.openai.com/api/docs/guides/text-to-speech , https://community.openai.com/t/new-realtime-models-on-the-api-gpt-realtime-2-1-and-gpt-realtime-2-1-mini/1385896 , https://aws.amazon.com/blogs/machine-learning/introducing-amazon-polly-bidirectional-streaming-real-time-speech-synthesis-for-conversational-ai , https://docs.speechmatics.com/text-to-speech/quickstart , https://murf.ai/api/docs/text-to-speech-models/falcon-2-1 , https://docs.mistral.ai/capabilities/audio/text_to_speech/ , https://docs.mistral.ai/models/voxtral-mini-tts-3-0 (alle Abruf 21.10.2026).

---

## 6. Datenschutz, Lizenz, Kennzeichnung, Zuverlässigkeit

| Anbieter | EU-Region | Aufbewahrung/Training | Kennzeichnung/Lizenz | Zuverlässigkeit |
|---|---|---|---|---|
| OpenAI | `eu.api.openai.com`: Speicherung und Verarbeitung in der EU für Audio (inkl. Speech, "gpt-4o-tts"), Realtime, `gpt-live-1`; erfordert Modified Abuse Monitoring oder Zero Data Retention, Freigabe und "Modified Retention amendment"; neue Modelle ab 05.03.2026 mit 10 % Aufschlag | Trainingsnutzung der API-Daten nein (seit 01.03.2023); Missbrauchsprotokoll bis 30 Tage (bei `/v1/audio/speech` ausdrücklich 30 Tage); ZDR nur mit Freigabe. Quelle: https://developers.openai.com/api/docs/guides/your-data | Nutzungsrichtlinien verlangen "clear disclosure … that the TTS voice they are hearing is AI-generated" (TTS-Leitfaden, offiziell) | **Abschaltung 06.01.2027** der vier TTS-Modelle. Realtime-Nachfolger: kein MP3, 10 statt 13 Stimmen (fable/onyx/nova fehlen), Drittquelle |
| Polly | Generative in Frankfurt (eu-central-1), Zürich (eu-central-2), London u. a. (Hinweis der Doku: in London, Kanada, Zürich nur eine Teilmenge der Stimmen, darunter Lennart, Vicki, Sabrina, Hannah; **Daniel ist dort nicht gelistet**) | Eingabetext kann zur Verbesserung der AWS-KI-Dienste gespeichert/genutzt werden; Opt-out über AWS-Organizations-"AI services opt-out policy"; verschlüsselt im Ruhezustand in der Region; Aufbewahrungsfrist nicht genannt. Quelle: https://aws.amazon.com/polly/faqs/ | Ausgabe gehört dem Kunden (FAQ). Keine Kennzeichnungspflicht des Herstellers gefunden. Doku-Hinweis: Updates des Modells können die Klangfarbe leicht ändern; Notstopp-Mechanismus gegen Halluzinationen kann ein halbes Wort abschneiden | produktreif, bekannt als stabil; Drosselungen per TPS |
| Deepgram | EU-Endpunkt `api.eu.deepgram.com` (GA laut Beitrag, TTS-Pfad `/v1/speak`; Drittquelle zu Details: ältere Doku nannte EU nur als Early Access für STT, WIDERSPRUCH, neuere Quellen "GA") | Aufbewahrung für TTS nicht belegt (UNBELEGT); `mip_opt_out` bei TTS nur als Parameter bekannt | – | – |
| Speechmatics | nur Preview-Endpunkt; EU nicht genannt | In der Vorschau werden Eingabetext und erzeugtes Audio zur Dienstverbesserung gespeichert; später "non-retentive" Optionen | – | Vorschau-Status (preview.tts.speechmatics.com), nur Englisch |
| Murf | Regionalendpunkte u. a. EU (Central), UK, US-Ost/West; `api.murf.ai` ist Legacy und leitet immer nach US-Ost | Aufbewahrung UNBELEGT | Kommerzielle Rechte UNBELEGT | Concurrency-Angaben widersprüchlich (5/2 gegen 10.000+) |
| xAI | UNBELEGT | UNBELEGT in der TTS-Doku (nur: Moderation läuft asynchron auf angesammeltem Text, "fail-open"); Drittseiten: Daten-Programm für Guthaben nutzt API-Interaktionen zum Training | UNBELEGT | Modell jung (Mai 2026); Firmenname "SpaceXAI/SpaceXSI" in Drittquellen uneinheitlich |
| Mistral | EU-Hosting auf der Doku nicht ausdrücklich genannt (UNBELEGT); Unternehmen mit Sitz in Frankreich | UNBELEGT | Open-Weights Voxtral TTS 26.03: CC BY-NC 4.0 (nicht kommerziell), kommerziell nur über die API; Voxtral TTS 3: Premier, "Public Preview" | Voxtral TTS 3 erst seit 20.10.2026 verfügbar und im Vorschau-Status: noch keine Betriebserfahrung |

Hinweis zum EU-AI-Act: Eigener Befund aus `tts-provider.md` §17 (Art. 50 seit 02.08.2026: Kennzeichnung täuschend echter KI-Stimmen) gilt unabhängig vom Anbieter; OpenAI verlangt sie zusätzlich vertraglich.

---

## 7. Unabhängige Qualitätsbelege

- **Artificial Analysis Speech Arena, Provider-Voice-Tabelle** (Elo, Blindtests, **nur Englisch US/UK**, kein Sprachfilter Deutsch): siehe Tabelle in Abschnitt 1. Quelle: https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice (Abruf 21.10.2026). Einordnung gegen Franks Ist-Stand: Chirp 3: HD Elo 1061 (Platz 60 von 98).
  - Oberhalb von Chirp 3 HD: Mistral Voxtral 3 TTS (1218), Murf Falcon 2 (1153), SpaceXAI TTS (1150), OpenAI TTS-1 HD (1103), TTS-1 (1090), Voxtral TTS (1091), GPT-Realtime-2 (1075), Polly Generative (1066).
  - Unterhalb oder gleichauf: Deepgram Flux TTS (1052), Polly Long-Form (1029), Murf Gen2 (986), Polly Neural (897), Polly Standard (831).
  - Nicht gelistet: OpenAI gpt-4o-mini-tts, Deepgram Aura-2, Speechmatics.
- **Controlled-Voice-Tabelle** (geklonte Standardstimmen, Englisch): Voxtral 3 TTS 1064, SpaceXAI TTS 1056; Murf, OpenAI, Polly, Deepgram nicht gelistet. Quelle: https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice
- **Mehrsprachige Arena** (Artificial Analysis, neun Nicht-Englisch-Sprachen inkl. Deutsch, Controlled Voice, Muttersprachler-Stimmen): Aus Suchtreffern zum Ankündigungspost: Cartesia Sonic 3.6 führt Deutsch, Inworld Realtime TTS-2 auf Platz 2 mit 1.261 Elo (22 Punkte dahinter); Open-Weights-Spitze in Deutsch: Boson Higgs Audio V3. Die deutsche Tabelle selbst war nicht abrufbar; **kein R5-Anbieter ist als deutscher Elo-Wert belegt.** Quelle: https://x.com/ArtificialAnlys/status/2102490340678856997 (extern, nur Suchtreffer; Abruf der Seite scheiterte mit HTTP 402).
- **TTS Arena (HF)**: nicht gezielt nach Deutsch abgefragt; kein deutscher Beleg für R5-Anbieter gefunden.
- Fazit: Alle Aussagen "klingt auf Deutsch gut" sind für R5-Anbieter **ungeprüft**. Die Englisch-Elo ist nur ein grober Indikator für Modellgeneration, kein Beleg für deutsche Aussprache.

---

## 8. Profile je Anbieter

### 8.1 OpenAI — Abschaltung beschlossen
Kurz: tts-1, tts-1-hd und beide gpt-4o-mini-tts-Snapshots enden am 06.01.2027 ("with at least three months' notice", Eintrag "2026-10-01: Text-to-speech models"; https://developers.openai.com/api/docs/deprecations , offiziell). Nachfolger `gpt-realtime-2.1-mini` unterstützt `/v1/audio/speech` nicht (Modellseite: Speech-Endpunkt "not supported"). Heute noch 13 Stimmen (alloy, ash, ballad, coral, echo, fable, nova, onyx, sage, shimmer, verse, marin, cedar; für beste Qualität marin/cedar), `tts-1`/`tts-1-hd` nur neun. Gut: `instructions`, günstiger Preis (tts-1 15 $/Mio.), EU-Datenresidenz auf Antrag. Schlecht: Abschaltung, Englisch-optimierte Stimmen, kein SSML, keine Normalisierungs-Aussage, Kennzeichnungspflicht, EU nur mit Sondervertrag.
Empfehlung: nicht neu einbauen. Wenn doch, nur über die Realtime-API-Linie und nur für Echtzeit-Dialog, nicht als reiner Vorlese-Dienst.

### 8.2 Amazon Polly
- Deutsche Stimmen nach Engine (Quelle https://docs.aws.amazon.com/polly/latest/dg/available-voices.html): Vicki, Daniel, Lennart sind generativ; Vicki und Daniel auch neural; Marlene, Hans, Vicki auch Standard; Long-Form hat kein Deutsch.
- Stärken: SSML-Tiefe, EU-Regionen (Frankfurt, Zürich), Output gehört dem Kunden, Streaming-Eingang (Bidirektional), bewährte Quoten.
- Schwächen: Generative-Qualität nur auf Chirp-Niveau (Elo 1066 gegen 1061), kein Vorteil beim Preis (30 $ wie Chirp), kein dauerhaftes Gratisvolumen für Generative (100.000 Zeichen/Monat, nur 12 Monate), kein einfacher API-Key.
- Hörtest-Priorität: niedrig, nur als zuverlässige EU-Option und wenn SSML (`sub`, `say-as`) gebraucht wird.

### 8.3 Deepgram Aura-2
- Deutsche Stimmen: elara, aurelia, lara, julius (Hauptstimme), fabian, kara, viktoria (https://developers.deepgram.com/docs/tts-models , offiziell). Sprachen: Englisch, Spanisch, Deutsch, Französisch, Niederländisch, Italienisch, Japanisch.
- Stärken: 200 $ nicht ablaufendes Startguthaben (gerechnet ca. 6,7 Mio. Zeichen), PAYG ohne Mindestumsatz, WebSocket-Streaming, EU-Endpunkt.
- Schwächen: 30 $/Mio. Zeichen (wie Chirp 3 HD), 2.000 Zeichen je Anfrage, keine SSML-/Normalisierungsangaben, keine Arena-Messung für Aura-2 (nur Flux TTS gelistet, 1052), Deutsch im Launch-Beitrag nur mit "ranks consistently among the top conversational voices" (Herstellerbehauptung).
- Hörtest-Priorität: mittel, weil das Startguthaben einen kostenlosen Test über viele Texte erlaubt.

### 8.4 Speechmatics
Nur Englisch (vier Vorschau-Stimmen, 16-kHz-WAV/PCM). 11 $/Mio. Zeichen, erste Million frei. Für Franks Zweck ausgeschieden. Abgrenzung: Der Doku-Text ist als 2025-Vorschau gekennzeichnet; ob inzwischen weitere Sprachen existieren, ist offen. Weder Preis- noch Doku-Seite nennen Deutsch (https://www.speechmatics.com/pricing , https://docs.speechmatics.com/text-to-speech/quickstart).

### 8.5 Murf
- Falcon 2.1 (Streaming, unter 100 ms) und Gen2 (hochwertige Vertonung, nicht streamend). Sprache "German (Germany)" in beiden Listen (https://help.murf.ai/murf-api-languages , offiziell). Deutsche Falcon-Stimmen laut Suchtreffer auf Murfs WebSockets-Seite: Björn, Erna, Lara, Ralf; Gen2-Katalog größer. Die vom Abruf gelieferte WebSockets-Seite selbst listete keine Stimmen: vor dem Einbau `GET /v1/speech/voices?model=falcon-2.1` abfragen.
- Stärken: günstig (ca. 10 $/Mio. laut AA), IPA und Aussprachewörterbuch, EU-Region, hoher Englisch-Elo für Falcon 2 (1153).
- Schwächen: Preisangaben widersprüchlich, Freikontingent ungeklärt, enge Concurrency (5 bzw. 2), Gen2 nur 986 Elo.
- Hörtest-Priorität: mittel bis hoch (billig und Deutsch belegt).

### 8.6 xAI Grok Voice TTS 1.0
- Veröffentlicht 15.05.2026 (OpenRouter-Eintrag, extern). 20 Sprachen mit Automatik (`auto`), Deutsch `de` offiziell. Jede Stimme spricht jede Sprache. Eigene Stimmen aus Referenzclips.
- Stärken: 15 $/Mio. Zeichen, Elo 1150, saubere Parameter (`text_normalization`, `replace`, `speed`, `optimize_streaming_latency`), 60.000 Zeichen je Anfrage, WebSocket mit Barge-in.
- Schwächen: kein Freikontingent gesichert, keine Datenschutz-/Aufbewahrungsangabe in der TTS-Doku, keine EU-Region belegt, `replace` kann keine Satzzeichen ersetzen.
- Hörtest-Priorität: mittel bis hoch.

### 8.7 Mistral Voxtral TTS (3)
- Zwei Versionen: Voxtral TTS (Doku-Version 26.03; ID `voxtral-mini-tts-2603`; 4B-Parameter; Open-Weights CC BY-NC 4.0; API 16 $/Mio.; veröffentlicht 23.03.2026 laut Mistral-Seite) und Voxtral TTS 3 (3.0; `voxtral-mini-tts-3-0`; 13 $/Mio.; 20.10.2026; Premier/Public Preview; Streaming, ca. 90 ms; 34 Emotions-Tags; fünf Formate; Zero-Shot- und Professional-Stimmklonen).
- Stärken: höchster belegter Elo-Wert der Gruppe (1218), günstig, Streaming, EU-Anbieter.
- Schwächen: Public Preview seit gestern, kein Freikontingent, keine Voreinstellungsstimmen belegt (müssen über Voices-Seite/Klon definiert werden), keine Normalisierung (alles ausschreiben), kein SSML, Ratenlimits unbekannt.
- Hörtest-Priorität: hoch.

---

## 9. Offene Punkte, die nur ein Hörtest/Konto klärt
1. Deutsche Qualität, Zahlen-/Datums-/Währungs-Normalisierung (Testtext A) für Mistral, Murf, xAI, Deepgram, Polly Generative.
2. Ob Polly Generative `phoneme` und Lexika wirklich akzeptiert.
3. Ob OpenAI `instructions` Satzzeichen wörtlich lesen (Testtext B) — wegen Abschaltung eher irrelevant.
4. Murf: tatsächlicher Preis (je Minute oder je Zeichen) und Freikontingent im Konto.
5. xAI: Freikontingent, Datennutzung, EU-Verarbeitung.
6. Mistral: Voreinstellungsstimmen auf Deutsch, Ratenlimits, EU-Hosting.

---

## 10. Quellenverzeichnis (alle Abruf 21.10.2026)

OpenAI:
- https://developers.openai.com/api/docs/pricing (offiziell)
- https://developers.openai.com/api/docs/guides/text-to-speech (offiziell)
- https://developers.openai.com/api/docs/deprecations (offiziell)
- https://developers.openai.com/api/docs/models/gpt-4o-mini-tts (offiziell)
- https://developers.openai.com/api/docs/models/gpt-realtime-2.1-mini (offiziell)
- https://developers.openai.com/api/docs/guides/your-data (offiziell)
- https://developers.openai.com/api/docs/guides/audio (offiziell)
- https://community.openai.com/t/new-realtime-models-on-the-api-gpt-realtime-2-1-and-gpt-realtime-2-1-mini/1385896 (offizielles Forum)
- https://www.creativeainews.com/articles/openai-tts-1-shutdown-migration-tested-2026/ (extern)
- https://perkstack.co/blog/openai-api-free-credits (extern)

Amazon Polly / AWS:
- https://aws.amazon.com/polly/pricing/ , https://aws.amazon.com/polly/faqs/ , https://aws.amazon.com/free/ (offiziell)
- https://docs.aws.amazon.com/polly/latest/dg/available-voices.html , generative-voices.html , supportedtags.html , say-as-tag.html , phoneme-tag.html , limits.html , managing-lexicons.html (offiziell)
- https://aws.amazon.com/blogs/machine-learning/introducing-amazon-polly-bidirectional-streaming-real-time-speech-synthesis-for-conversational-ai (offiziell, 26.03.2026)

Deepgram:
- https://deepgram.com/pricing , https://developers.deepgram.com/docs/tts-models , https://developers.deepgram.com/docs/text-to-speech , https://deepgram.com/learn/aura-2-now-speaks-dutch-french-german-italian-japanese (offiziell)
- https://developers.deepgram.com/reference/regional-endpoints , https://deepgram.com/learn/deepgram-eu-endpoint-now-generally-available (offiziell, nur als Suchtreffer ausgewertet)

Speechmatics: https://www.speechmatics.com/pricing , https://docs.speechmatics.com/text-to-speech/quickstart (offiziell)

Murf: https://murf.ai/api , https://murf.ai/api/docs/introduction/overview , https://murf.ai/api/docs/text-to-speech-models/falcon-2-1 , https://murf.ai/api/docs/text-to-speech/web-sockets , https://help.murf.ai/murf-api-languages (offiziell); https://costbench.com/software/voice-apis/murf-api/ (extern)

xAI: https://docs.x.ai/developers/model-capabilities/audio/text-to-speech , https://docs.x.ai/developers/models (offiziell); https://openrouter.ai/x-ai/grok-voice-tts-1.0 (extern); https://perkstack.co/blog/xai-startup-credits , https://agentdeals.dev/vendor/xai (extern)

Mistral: https://docs.mistral.ai/models/voxtral-mini-tts-3-0 , https://docs.mistral.ai/models/ , https://docs.mistral.ai/capabilities/audio/text_to_speech/ , https://mistral.ai/news/voxtral-tts , https://mistral.ai/pricing (offiziell); https://orcarouter.ai/blog/voxtral-3-tts-vs-cartesia-sonic-3-6 (extern, Gegenprobe der AA-Zahlen)

Arena: https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice , https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice , https://artificialanalysis.ai/text-to-speech (alle extern/unabhängig)

---

BEST-PRACTICES-KANDIDATEN:
0. KORREKTUR `best-practices/apis/tts-provider.md` §17: Die Preistabelle führt Speechmatics (11 $, 1 Mio. frei) und OpenAI gpt-4o-mini-tts (ca. 15 $) als Optionen. Speechmatics TTS ist laut Doku nur Englisch (kein Deutsch); OpenAI-TTS (tts-1, tts-1-hd, gpt-4o-mini-tts) wird am 06.01.2027 abgeschaltet. Deepgram-Zeile ergänzen: Aura-2 hat deutsche Stimmen seit 12.12.2025.
1. OpenAI-Sprachausgabe: `tts-1`, `tts-1-hd`, `gpt-4o-mini-tts` enden am 06.01.2027; Nachfolger `gpt-realtime-2.1-mini` ist nur über die Realtime-API erreichbar, liefert PCM/G.711 statt MP3 und hat weniger Stimmen. Beim Einbau eines TTS-Anbieters immer Abkündigungsseite prüfen (Quelle: developers.openai.com/api/docs/deprecations).
2. Anbieter ohne Textnormalisierung (Mistral Voxtral TTS: "spell out numbers and abbreviations") brauchen eine eigene deutsche Normalisierungsstufe (Zahlen, Datum, Uhrzeit, Euro, Einheiten, Abkürzungen) vor dem Senden; gilt auch für Speechmatics.
3. Satzzeichen wörtlich sprechen: bei Anbietern ohne SSML (Mistral, xAI, Deepgram, Murf) per eigener Ersetzung vor dem Senden lösen; bei Polly per `<sub alias>`; xAI-`replace` akzeptiert keine Satzzeichen als Schlüssel.
4. Polly-Quoten für Generative: 8 TPS, 26 parallele Anfragen, 3.000 abrechenbare Zeichen je `SynthesizeSpeech` (Audio höchstens 10 Minuten), bidirektionales Streaming 8 TPS/8 gleichzeitig; Chunking auf Satzgrenzen.
5. AWS Free Tier seit 15.07.2025: Credits-Modell (bis 200 $, 6 Monate Free Plan, Credits 12 Monate nutzbar); Polly-Monatskontingente (Neural 1 Mio., Generative 100.000) gelten "first 12 months".
6. Arena-Ergebnisse (Artificial Analysis Provider Voice) sind Englisch-Messungen; für Deutsch nur die Multilingual/Controlled-Voice-Arena nutzen und nie einen Englisch-Elo als Deutsch-Qualität ausgeben.
7. Vor dem Einbau eines Anbieters Preis auf der Anbieterseite gegen die Arena-Preisspalte prüfen: Murf liefert drei verschiedene Preisdarstellungen (je Minute, je 1.000 Zeichen, je 1 Mio.).

BUG-KANDIDATEN:
1. OpenAI `/v1/audio/speech` mit den vier TTS-Modellen: ab 06.01.2027 harter Ausfall; Open-Source-Tools (n8n, Open WebUI, AnythingLLM u. a.) setzen `tts-1` fest (Drittquelle).
2. Polly Neural + `<say-as interpret-as="characters">`: Der Satz wird mit der Standardstimme gesprochen, aber zum Neural-Preis abgerechnet.
3. Polly Generative: das Modell kann Wörter halb abschneiden (Notstopp gegen Halluzination) und klingt nach Modell-Updates leicht anders (Doku-Hinweis).
4. Polly Dokumentenwiderspruch: Matrix sagt `phoneme` bei Generative "teilweise", Einzelseite nennt Generative nicht.
5. Mistral Voxtral: `pcm` ist float32 (nicht int16), MP3-Streaming hat deutlich höhere Zeit bis zum ersten Ton (ca. 3 s statt ca. 0,8 s laut Doku-Altstand); Anfragen mit moderationsrelevantem Text liefern HTTP 403.
6. Murf WAV-Streaming: Erster Block trägt einen 44-Byte-Header mit Größenfeldern 0xFFFFFFFF; zum Speichern Header selbst neu schreiben. Idle-WebSocket schließt nach 3 Minuten. `api.murf.ai` leitet immer nach US-Ost (kein Geo-Routing, keine EU-Residenz).
7. xAI TTS: `replace` mit falschem IPA-Symbol erzeugt ohne Fehlermeldung falsche Aussprache; Schlüssel mit Satzzeichen werden mit HTTP 400 abgelehnt.
8. Deepgram TTS: mehr als 2.000 Zeichen je Anfrage → HTTP 413, kein Audio.
