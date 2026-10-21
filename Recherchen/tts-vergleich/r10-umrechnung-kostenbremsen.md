# R10 – Umrechnungsgrundlagen und Kostenkontrolle (TTS-Vergleichsstudie)

Recherchezeitpunkt und Abrufdatum aller Quellen: **21.10.2026**. Engine C (Sonnet-Schwarm), Researcher R10.
Alle Aussagen tragen Link und Abrufdatum. Was ich nicht belegen konnte, ist mit **UNBELEGT** markiert. Eigene Rechnungen sind mit **(abgeleitet)** gekennzeichnet und von Quellenaussagen getrennt.

Hinweis zur Methode: Seiten wurden mit WebFetch geholt, das den Inhalt von einem kleinen Modell zusammenfassen lässt. Wörtliche Zitate unten stammen aus diesen Rückgaben. Die Google-Cloud-TTS-Preisseite habe ich zusätzlich direkt per curl geladen und im Klartext gelesen (Abruf 21.10.2026), die zwei PDFs (Meinhold, Gebhard) per pdftotext.

---

## 0. Kurzfazit

| Thema | Ergebnis |
|---|---|
| Deutsches Nachrichtentempo | Tagesschau (Gebhard 2012): 5,15 Silben/s ohne Pausen, ca. 4,45 Silben/s mit Pausen, 2,16 Wörter/s ≈ **130 Wörter/min inkl. Pausen**, ≈ 150 ohne Pausen (abgeleitet) |
| Zeichen pro Minute (inkl. Leerzeichen) | **ca. 870–1.000** (130–150 Wörter × 6,7 Zeichen/Wort); Gegenprobe OpenAI-Messung 916, Cartesia-Angabe 750–800 |
| Gemini-Audio-Tokens | **TTS-Ausgabe: 25 Tokens/s** (Google-Preisseiten); Audio-Eingabe (Verstehen): 32 Tokens/s. Nicht verwechseln. |
| OpenAI gpt-4o-mini-tts | Keine offizielle Tokens/min-Angabe. Aus Fremdmessung abgeleitet ≈ 1.200 Audio-Tokens/min (≈ 20/s) |
| Tokenisierung Deutsch | 1,76 Tokens/Wort (Deutsch) gegen 1,23 (Englisch), Mittel über 6 Tokenizer (kein Gemini) → Deutsch ≈ 1,4-mal so viele Tokens je Wort |
| Alibaba-Zeichen | Offizielle Regel: Chinesisch 2, alles andere 1, SSML-Tags nicht gezählt. Gemessene 477 für 356 Zeichen **nicht durch die Regel erklärbar** (siehe 2.2) |
| Härteste Bremsen | Gemini API: **Prepay (402 bei 0 Guthaben)** und **Spend-Cap-Budget** (Gemini API ist zulässig, **Cloud Text-to-Speech nicht**). Alibaba: „Free Quota Only“. Mistral/OpenAI: Prepaid-Guthaben bzw. Hard Limit. Cartesia: Overage aus. Google Cloud TTS, Azure PAYG, AWS: nur Warnungen oder Selbstbau |
| Nutzungsprofile (30 Tage, 900 Zeichen/min) | wenig: 135.000 Zeichen, 9.000 s; mittel: 540.000 Zeichen, 36.000 s; intensiv: 1.620.000 Zeichen, 108.000 s |

---

## 1. Belegte Umrechnungen

### 1.1 Deutsche Sprechgeschwindigkeit

| Größe | Wert | Quelle und Einordnung |
|---|---|---|
| Tagesschau, Artikulationsgeschwindigkeit (ohne Pausen) | **5,15 Silben/s** (13 Sprecher, ca. 25 min Aufnahme, 6.535 Silben nach Vokalkern) | Gebhard (2012), Dissertation HU Berlin, Tab. 4.28 und Abschn. 4.5.3. [PDF](https://edoc.hu-berlin.de/bitstreams/9391368c-df94-4887-b305-34236a940f4e/download), abgerufen 21.10.2026. Tabelle im PDF ist durch Texterkennung teils zerschossen; der Wert 5,15 steht eindeutig in der Zeile „AG Silben/s“. |
| Tagesschau, Pausenanteil | 13,67 % der Sprechzeit | ebd., Abschn. 4.5.3 |
| Tagesschau, Wörter pro Sekunde | **2,16 Wörter/s** | ebd., Abschn. 4.5.3 („in Wörtern/s 2,16“). Der dort genannte Morphem-Wert 3,82 deckt sich mit dem englischen Wert im selben Text, möglicher Abschreibfehler der Arbeit; den Wörter-Wert halte ich für plausibel, weil er zu den Silbenwerten passt (siehe unten). |
| Silben pro Wort (Nachrichtensprache Deutsch) | **2,09** | ebd., Tab. 4.33 (Zeile DE; Spaltenzuordnung im OCR unsicher). Die Arbeit sagt im Text, die Sprachen lägen bei etwa 1,9 bis 2,1 Silben/Wort. |
| Plausibilitätsprobe (abgeleitet) | 4,45 Silben/s ÷ 2,09 = 2,13 Wörter/s; 5,15 ÷ 2,09 = 2,46 Wörter/s | passt zu 2,16 Wörtern/s inkl. Pausen |
| Wörter pro Minute inkl. Pausen (abgeleitet) | 2,16 × 60 = **130 Wörter/min** | aus Gebhard |
| Wörter pro Minute ohne Pausen (abgeleitet) | 2,16 ÷ (1 − 0,1367) × 60 ≈ **150 Wörter/min** | aus Gebhard |
| Rundfunknachrichten, Silben/s | **5,7** (Nachrichten), 5,4 (Belletristik), ca. 5,0 (Sprechtheater, OCR „4,97“), 3,7 (Lyrik/Ballade), 5,9 (freie Äußerung) | Meinhold (1987), ICPhS 11, Bd. 5, S. 115, ca. 100 Sprecher, DDR-Rundfunk. [PDF](https://www.coli.uni-saarland.de/Phonetics/icphs/ICPhS1987/11_ICPhS_1987_Vol_5/p11.5_115.pdf), abgerufen 21.10.2026. Ob der Wert Pausen enthält, geht aus dem lesbaren Text nicht klar hervor. Pausenzeitquotient Nachrichten 1,21 (OCR). |
| Allgemeines Sprechtempo Deutsch/Französisch | 280–300 Silben/min ≈ 4,7–5,0 Silben/s | Slembek (1993), zitiert auf einem Doktorandentag-Poster der HU Berlin. [Poster](https://www.linguistik.hu-berlin.de/de/informationen-fuer-promovierende/doktag/poster/gebhard.pdf/@@download/file/gebhard.pdf), Fundstelle über Suche 21.10.2026 (Sekundärquelle, Direktabruf des Posters schlug fehl: Datei war kein gültiges PDF). |
| Mittleres Sprechtempo über 17 Sprachen (gelesene Texte) | 6,63 Silben/s (SD 1,15); deutscher Einzelwert nicht aus dem Text entnehmbar | Coupé et al. (2019), Science Advances. [PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC6984970), abgerufen 21.10.2026. Der deutsche Wert steht in den Zusatzdaten ([GitHub](https://github.com/keruiduo/SupplMatInfoRate)), die ich nicht geöffnet habe. **UNBELEGT** für Deutsch. |
| Wörter/min, Hörbuch und Moderation | 150–160 (Hörbuch), 120–130 (Alltag), TV-Nachrichten 150–170 | Anbieter-Websites ohne Methodik, nur als schwache Gegenprobe: [weesperneonflow.ai](https://weesperneonflow.ai/de/tools/dictation-speed-test/), Suche 21.10.2026. |

Einordnung: Eine einzige deutsche Wörter-pro-Minute-Norm gibt es nicht. Belegt ist der Bereich **ca. 130 (inkl. Pausen) bis 150 (ohne Pausen) Wörter/min** für Nachrichtentempo und **ca. 4,5–5,7 Silben/s**. TTS-Engines sprechen meist ohne lange Pausen, aber mit Satzpausen; für Kostenrechnung ist die Spanne 130–150 ehrlich.

### 1.2 Wortlänge in Zeichen

| Größe | Wert | Quelle |
|---|---|---|
| Laufender deutscher Text | **5,7 Buchstaben/Wort**, mit Leerzeichen **6,7 Zeichen/Wort** | Sekundärbericht über eine Angabe der Duden-Redaktion, Fundstelle über Suche 21.10.2026; Korpus nicht genannt. Die Duden-Seite selbst ([Sprache in Zahlen](https://www.duden.de/sprache-in-zahlen)) lieferte beim Abruf HTTP 503, daher **nicht im Original geprüft**. Wörterbuch-Lemmata (nicht Fließtext): 10,6 Buchstaben. |
| Eigene Messung Testtext A (356 Zeichen, 70 Leerzeichen-getrennte Tokens) | 5,09 Zeichen/Token inkl. Leerzeichen; Zeichen ohne Leerzeichen: 287 | eigene Zählung am 21.10.2026 mit `testtext.md` (abgeleitet). Der Text hat viele Ziffern und Abkürzungen, deshalb kürzere „Wörter“ als Fließtext. Zeichenzahl laut Auftrag „ca. 370“; ich zähle **356 Zeichen** (364 Bytes UTF-8), direkt aus der Datei `testtext.md` gelesen (nicht aus einer Abschrift). Die Datei enthält nur normale Leerzeichen; die einzigen Nicht-ASCII-Zeichen sind – (U+2013), ü, €, ä, °, ü. Es gibt keine geschützten Leerzeichen. |
| Gesprochene Fassung von Testtext A (eigene Ausschreibung der Zahlen, Abkürzungen, Akronyme) | ca. 84 Wörter, ca. 154 Silben (Silbenzählung per Vokalgruppen-Heuristik, ±5 %) | eigene Rechnung (abgeleitet). Zeigt: Der Text wird deutlich länger gesprochen, als die 356 Zeichen vermuten lassen. |
| Vorhersage Dauer Testtext A (abgeleitet) | 154 Silben ÷ 4,45 = **34,6 s**; ÷ 5,15 = 29,9 s; über Wörter: 84 ÷ 2,16 = 38,9 s, 84 ÷ 2,5 = 33,6 s | Spanne ca. **30–39 s**. Zum Vergleich: Gemini-Tokens des Hauptagenten (866–1046) bei 25 Tokens/s ergeben 34,6–41,8 s. Passt grob zusammen. Die echte WAV-Dauer des Hauptagenten ersetzt diese Schätzung. |

### 1.3 Zeichen pro Minute (zusammengeführt)

Formel: **Zeichen/min = Wörter/min × Zeichen/Wort (inkl. Leerzeichen)**

| Fall | Wörter/min | Zeichen/Wort | Zeichen/min | Zeichen/s |
|---|---|---|---|---|
| Nachrichtentempo inkl. Pausen | 130 | 6,7 | **871** | 14,5 |
| Nachrichtentempo ohne Pausen | 150 | 6,7 | **1.005** | 16,8 |
| Mittelwert, den ich für die Rechnung nehme | – | – | **900** | 15,0 |

Gegenproben (nicht deutsch, daher nur Plausibilität):

| Messung | Wert | Quelle |
|---|---|---|
| OpenAI TTS-1 / gpt-4o-mini-tts, 4.096 ASCII-Zeichen englisches Podcast-Skript | 256 s bzw. 268 s Audio → 960 bzw. **917 Zeichen/min** | [s-anand.net](https://www.s-anand.net/blog/openai-tts-cost/), Blog vom 02.11.2025, abgerufen 21.10.2026 |
| Cartesia | „One minute of audio generation requires 750-800 credits“ bei 1 Credit je Zeichen → **750–800 Zeichen/min** | [cartesia.ai/pricing](https://cartesia.ai/pricing), abgerufen 21.10.2026 |
| Testtext A mit Gemini (Hauptagent) | 356 Zeichen in ca. 35–42 s (aus 866–1046 Tokens ÷ 25) → **511–617 Zeichen/min** | Eigenmessung des Hauptagenten; ersetzt durch echte WAV-Dauer. Niedrig, weil Zahlen, Datum, Uhrzeit, Abkürzungen beim Sprechen länger werden als geschrieben. |

Folgerung für die Kostenrechnung: **Kosten pro Zeichen hängen stark vom Textinhalt ab.** Ein Assistent, der viele Zahlen, Uhrzeiten und Einheiten vorliest (wie Testtext A), erzeugt je Zeichen mehr Audio. Zeichenpreise und Token-/Minutenpreise sollten deshalb **an beiden Texten gerechnet werden: Fließtext (ca. 900 Zeichen/min) und Testtext A (ca. 520–620 Zeichen/min)**.

### 1.4 Audio-Tokens

| Anbieter | Aussage | Quelle |
|---|---|---|
| Gemini, TTS-Ausgabe | „Audio tokens correspond to 25 tokens per second of audio“ (Fußnote an Gemini 3.8 Flash TTS, 3.8 Flash-Lite TTS, 3.1 Flash TTS Preview; bei 2.5 Flash/Pro Preview TTS steht die Fußnote auf der Gemini-API-Preisseite **nicht**) | [Gemini API Preise](https://ai.google.dev/gemini-api/docs/pricing) und [Cloud TTS Preise](https://cloud.google.com/text-to-speech/pricing?hl=en), beide 21.10.2026. Die Cloud-Seite führt die Fußnote für die ganze Gemini-TTS-Tabelle. |
| Gemini, Audio-Eingabe (Verstehen) | „Audio: 32 tokens per second“; Video 263 Tokens/s | [Tokens-Seite](https://ai.google.dev/gemini-api/docs/tokens), 21.10.2026. Gilt für **Eingabe**, nicht für TTS-Ausgabe. |
| Gemini, Text | „a token is equivalent to about 4 characters. 100 tokens is equal to about 60-80 English words.“ | ebd. |
| Gemini, Ausgabeformat | Unary: WAV 24 kHz, mono, 16 Bit PCM; Streaming: headerloses PCM 24 kHz | [Speech-Generation-Doku](https://ai.google.dev/gemini-api/docs/speech-generation), 21.10.2026. Dauer = Bytes ÷ 48.000 (24.000 Samples × 2 Bytes). |
| Hauptagent-Messung | 866–1046 Audio-Output-Tokens je Modell für Testtext A | bei 25 Tokens/s → **34,6–41,8 s**; bei 32 Tokens/s → **27,1–32,7 s**. Beide Fälle sind plausibel (meine Silbenschätzung 30–39 s trennt sie nicht). Die **WAV-Dauer entscheidet** die Frage, ob für die Ausgabe 25 oder 32 gelten: Tokens/s = Tokens ÷ WAV-Sekunden. Die Cloud- und Gemini-API-Preisseiten nennen 25 für die 3.x-Modelle; für 2.5 Flash/Pro steht auf der Gemini-API-Seite keine Fußnote. Es gilt der gemessene Wert. |
| OpenAI gpt-4o-mini-tts | **Keine offizielle Angabe** Tokens/Minute. Preise: 0,60 $ je 1 Mio. Text-Eingabe-Tokens, 12 $ je 1 Mio. Audio-Ausgabe-Tokens; Max. Eingabe 2.000 Tokens | [Modellseite](https://developers.openai.com/api/docs/models/gpt-4o-mini-tts), 21.10.2026. Die Seite nennt keine Kosten pro Minute und keine Tokens pro Minute. |
| OpenAI, abgeleitet | Aus s-anand-Messung: 0,0652 $ (eine Zeile; zwei Läufe zusammen 0,12942 $ Ausgabe + 0,0010524 $ Eingabe) für 268 s → Ausgabe ≈ 0,0642 $ ÷ 12 $/Mio. ≈ **5.350 Tokens in 268 s ≈ 20 Tokens/s ≈ 1.200 Tokens/min**, ≈ 0,0144 $/min | Rechnung aus [s-anand.net](https://www.s-anand.net/blog/openai-tts-cost/); passt zur Annahme „1 Audio-Token je 50 ms“, die ein Dritt-Blog für OpenAI-Audio nennt ([Forasoft](https://www.forasoft.com/blog/article/openai-realtime-api-pricing), Suche 21.10.2026; im OpenAI-Original nicht gefunden). **Kein Offizialbeleg.** Eine Foren-Antwort sagt außerdem, die „0,015 $/min“ seien nur eine Schätzung, abgerechnet werde nach Tokens ([Forum](https://community.openai.com/t/understanding-gpt-4o-mini-tts-pricing-input-characters-cost/1151816)). |

### 1.5 Tokenisierung deutscher Texte (Eingabe-Tokens)

| Größe | Wert | Quelle |
|---|---|---|
| Tokens je Wort, Deutsch | **1,76** (Spanne 1,55–1,98 über 6 Modelle) | Ovcharov (2026), „The Tokenizer Tax Across 25 European Languages“, arXiv:2605.24718, Tab. 4 (SIB-200, parallele Texte). [PDF](https://arxiv.org/pdf/2605.24718), 21.10.2026. Modelle: Llama-Familie, Qwen3, GPT-4o u. a.; **Gemini nicht dabei**. |
| Tokens je Wort, Englisch | **1,23** (1,21–1,25) | ebd. |
| Verhältnis Deutsch/Englisch (abgeleitet) | 1,76 ÷ 1,23 ≈ **1,43** | eigene Rechnung |
| Zeichen je Token, Deutsch (abgeleitet) | 6,7 ÷ 1,76 ≈ **3,8 Zeichen/Token** (6,7 aus Sekundärquelle, siehe 1.2; Duden-Seite lieferte beim zweiten Versuch am 21.10.2026 erneut HTTP 503) (Fließtext), gegenüber ca. 4 für Englisch laut Google | eigene Rechnung, Spanne ca. 3,4–4,3 |
| Eigenmessung Hauptagent | 152 Prompt-Tokens für 356 Zeichen + kurze Stil-Anweisung → höchstens 2,3 Zeichen/Token | Testtext A ist ziffernlastig und kurzwortig, daher schlechter als Fließtext |

Kostenwirkung: Text-Eingabe-Tokens kosten bei Gemini 0,50–1,00 $ je Mio. Tokens. 152 Tokens ≈ 0,00015 $ pro Anfrage. Die Eingabeseite ist gegenüber der Audio-Ausgabe **vernachlässigbar** (Audio: 25 Tokens/s × Sekunden × 9–20 $ je Mio.).

---

## 2. Wie Anbieter „Zeichen“ zählen

### 2.1 Übersicht

| Anbieter / Dienst | Leerzeichen | Satzzeichen | SSML-Tags | Umlaute, Sonderzeichen, Emojis | Chinesisch | Mindestabrechnung je Anfrage | Quelle |
|---|---|---|---|---|---|---|---|
| **Google Cloud TTS** (Chirp 3 HD, Studio, Neural2, WaveNet, Standard) | zählen, auch Zeilenumbrüche | zählen | zählen **alle außer `<mark>`** | Standard/WaveNet: Zeichenzahl ≤ Bytezahl; „Japanese … characters in UTF-8 typically require more than one byte each … you are only charged for one character, not multiple bytes“. Umlaute damit je 1 Zeichen. Emojis: **UNBELEGT** | je 1 Zeichen | **UNBELEGT** (keine Angabe). Max. 5.000 Bytes je Anfrage | [Preisseite](https://cloud.google.com/text-to-speech/pricing?hl=en), curl-Klartext, 21.10.2026; Quoten: [Quotas](https://docs.cloud.google.com/text-to-speech/quotas) |
| **Gemini-TTS** (Gemini API und Cloud) | keine Zeichenabrechnung; **Text-Eingabe-Tokens + Audio-Ausgabe-Tokens** | – | Stil-Anweisung und Tags im Prompt zählen als Eingabe-Tokens (Eigenmessung: 152 Tokens inkl. Anweisung) | – | – | **UNBELEGT** | siehe 1.4 |
| **Azure Speech** | zählen (auch Tabs, alle Whitespace) | zählen | zählen, **außer `<speak>` und `<voice>`** | „Every code point defined in Unicode“ zählt (Emoji-Zählung je Codepunkt ist daraus **abgeleitet**, nicht ausdrücklich genannt); zählt auch ohne erzeugtes Audio und bei Sprach-Mismatch; nur erfolgreich verarbeitete Anfragen | **2 Zeichen je chinesischem Zeichen**, auch Kanji/Hanja | **UNBELEGT** | [Microsoft Learn, TTS Überblick](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/text-to-speech), 21.10.2026 |
| **Amazon Polly** | „number of characters you send“ | zählen | **nicht gezählt** („SSML tags are not counted as billed characters“) | Emojis **UNBELEGT**. Speech-Marks-Anfrage über denselben Text wird **zusätzlich** gezählt (1.000-Zeichen-Text + Marks = 2.000). Wiedergabe gecachter Ergebnisse kostenlos | – | keine Mindestabrechnung auf der Preisseite genannt. Limits: synchron 6.000 Zeichen gesamt, davon max. 3.000 abrechenbar | [API-Referenz](https://docs.aws.amazon.com/polly/latest/dg/API_SynthesizeSpeech.html), [Preise](https://aws.amazon.com/polly/pricing/), [FAQ](https://aws.amazon.com/polly/faqs/), 21.10.2026 |
| **ElevenLabs** | Abrechnung je Zeichen; Credits waren früher „Zeichen“ („Credits were previously called characters“) | **UNBELEGT** im Einzelnen | **UNBELEGT** (auch für v3-Audio-Tags) | **UNBELEGT** | **UNBELEGT** | **UNBELEGT**. Max. je Anfrage: Flash v2.5 40.000, Multilingual v2 10.000, v3 5.000 Zeichen | [Billing-Doku](https://elevenlabs.io/docs/overview/administration/billing), [Modelle](https://elevenlabs.io/docs/overview/models), 21.10.2026 |
| **ElevenLabs, Credit-je-Zeichen** | – | – | – | – | – | – | API: Flash/Turbo 0,04 $/1k Zeichen, Multilingual v2 und v3 0,08 $/1k; Plan-Kontingente Flash/Turbo sind genau doppelt so groß wie Multilingual (Creator 550.000 vs. 275.000) → **0,5 Credit je Zeichen bei Flash/Turbo** (abgeleitet). [Preisseite](https://elevenlabs.io/pricing/api), 21.10.2026 |
| **Alibaba Model Studio** (qwen3-tts-flash) | zählen je 1 | zählen je 1 | **nicht gezählt**, nur der Text | Ziffern, Buchstaben, Satzzeichen, Leerzeichen, Kana, Hangul je 1 | **2 je chinesischem Zeichen** (auch Kanji, Hanja) | „Failed calls are not charged“; Mindestmenge **UNBELEGT**. Ausgabe kostenlos | [Preis- und Zählregel](https://www.alibabacloud.com/help/en/model-studio/model-pricing) (Seite Abschn. „Speech synthesis“, Abruf 21.10.2026), [QwenCloud-Preise](https://docs.qwencloud.com/developer-guides/getting-started/pricing) |
| **Mistral Voxtral TTS** | Zählweise **UNBELEGT** (Doku nennt nur Preis je Zeichen) | – | – | – | – | **UNBELEGT** | [Mistral Pricing](https://docs.mistral.ai/inference/pricing), 21.10.2026 |
| **Cartesia Sonic** | 1 Credit = 1 Zeichen | **UNBELEGT** im Einzelnen | **UNBELEGT** | **UNBELEGT** | – | **UNBELEGT** | [cartesia.ai/pricing](https://cartesia.ai/pricing), 21.10.2026 |
| **OpenAI gpt-4o-mini-tts** | Tokens, keine Zeichen; ob Anweisungen („instructions“) als Eingabe zählen, blieb im Forum **unbeantwortet** | – | – | – | – | **UNBELEGT**. Max. Eingabe 2.000 Tokens | [Modellseite](https://developers.openai.com/api/docs/models/gpt-4o-mini-tts); [Forum](https://community.openai.com/t/understanding-gpt-4o-mini-tts-pricing-input-characters-cost/1151816) |

Mindestabrechnung pro Anfrage: **bei keinem der geprüften Anbieter dokumentiert.** Das heißt nicht, dass es keine gibt. Die Studie sollte sie an sehr kurzen Texten (z. B. „Ja!“, 3 Zeichen) in der Abrechnung nachmessen, falls kurze Antworten ein großer Teil der Nutzung sind.

### 2.2 Auffälligkeit: Alibaba zählt 477 statt 356 Zeichen

Die offizielle Regel (Chinesisch 2, alles andere 1, SSML nicht gezählt) liefert für den deutschen Testtext A **356**, nicht die gemessenen **477** (Faktor 1,34). Ausgeschlossene Erklärungen (eigene Rechnung, 21.10.2026):

| Hypothese | Ergebnis für Testtext A |
|---|---|
| Zeichen nach Unicode-Codepunkten | 356 |
| UTF-8-Bytes | 364 |
| nicht-ASCII-Zeichen (– ü € ä ° ü, 6 Stück) doppelt gezählt | 362 |
| UTF-16-Bytes | 712 |

Keine passt zu 477. **UNBELEGT**, woher 477 kommt. Naheliegende, ungeprüfte Hypothesen: (a) Zählung nach einer serverseitigen Textnormalisierung (Zahlen, Datum und Abkürzungen werden ausgeschrieben), (b) zusätzlicher Anweisungstext im Aufruf, (c) die Abrechnungsanzeige zählt etwas anderes als die API-Antwort. **Vorschlag für den Hauptagenten:** Drei Kontrollaufrufe mit je 100 Zeichen („a“ × 100; „ä“ × 100; „1.249,99 €“ wiederholt) und die gemeldete `characters`-Zahl vergleichen. Bis dahin: für Alibaba als Konservativwert den **Faktor 1,34** auf die Zeichenzahl anwenden und kennzeichnen. Ein Faktor aus **einer** Messung ist keine Regel.

Der Free-Quota-Verbrauch (110.000 Zeichen) hängt an derselben Zählung.

---

## 3. Kostenbremsen je Plattform

Legende: **Hart** = Anfragen werden tatsächlich abgelehnt, ohne dass Sie handeln müssen. **Weich** = nur Warnung oder Verzögerung. Alle Verzögerungen sind die dokumentierten.

### 3.1 Übersicht

| Plattform | Harte Bremse vorhanden? | Welche | Verzögerung / Lücke | Fehlerbild | Quelle |
|---|---|---|---|---|---|
| **Gemini API** | **Ja** | (1) **Prepay**: Guthaben im Voraus, bei 0 stoppen alle Schlüssel. (2) **Projekt-Spend-Cap** (monatlich, AI Studio → Spend; als „experimental“ bezeichnet). (3) **Tier-Cap** je Rechnungskonto (Tier 1: 250 $/Monat). (4) **Spend-Cap-Budget in Google Cloud** für `generativelanguage.googleapis.com` | Abrechnungsdaten bis ca. 10 min verzögert; Überschreitung bis dahin möglich, „Prepay billing is subject to overages during the approximately 10 minute billing pipeline latency“. Lang laufende Aufgaben (Batch, Agenten) können den Cap überschreiten | Prepay leer: **HTTP 402 Payment Required** | [Billing-Doku](https://ai.google.dev/gemini-api/docs/billing), [Google-Blog 16.03.2026](https://blog.google/innovation-and-ai/technology/developers-tools/more-control-over-gemini-api-costs/), [Spend-Cap-Budgets](https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps), alle 21.10.2026 |
| **Google Cloud Text-to-Speech** (klassisch, Chirp 3 HD) | **Nein** (nicht für Kosten) | Alerts-only-Budgets warnen nur. **Spend-Cap-Budgets decken Cloud TTS nicht ab** (zulässig laut Seite: Gemini API, Gemini Enterprise Agent Platform/Vertex AI, Cloud Run, Cloud Run functions; „more services will be added“). Selbstbau: Budget → Pub/Sub → Funktion, die die Abrechnung des Projekts abschaltet. Quoten senken nur Anfragevolumen | Budget-Benachrichtigungen mehrmals täglich per Pub/Sub, erste Meldung „may take several hours“; Abschalten der Abrechnung „doesn't guarantee that you won't spend more“ und beendet **alle** Dienste im Projekt, Ressourcen können unwiederbringlich gelöscht werden | – | [Budgets](https://docs.cloud.google.com/billing/docs/how-to/budgets), [Abschalten per Benachrichtigung](https://docs.cloud.google.com/billing/docs/how-to/disable-billing-with-notifications), [Spend-Cap-Budgets](https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps), 21.10.2026 |
| **Google Cloud, Quoten als Kappung** | **Teilweise** | Quoten lassen sich laut Google bei abrechenbaren APIs über „Edit Quotas“ senken („you can set limits on requests to any billable API“). Aber: „these caps regulate individual API volumes rather than overall spending“. Cloud TTS hat laut Quotas-Seite **nur pro-Minute-Anfragelimits** (Chirp 3: 200/min, Neural2/Polyglot: 1.000/min, Studio: 500/min, Gemini 2.5 Flash TTS: 150/min, 2.5 Pro TTS: 125/min, 3.1 Flash TTS: dynamisch) und **kein Tages- oder Monatskontingent** in Zeichen. Ob diese TTS-Quoten kundenseitig **gesenkt** werden dürfen, steht auf der TTS-Quotas-Seite nicht (nur Erhöhung): **UNBELEGT** für TTS | „Quota enforcement has a latency period“ | – | [Cap API usage](https://docs.cloud.google.com/apis/docs/capping-api-usage), [TTS-Quotas](https://docs.cloud.google.com/text-to-speech/quotas), 21.10.2026 |
| **Azure Speech** | **Nur in Sonderfällen** | **Spending Limit** gibt es nur bei Free-Konto und Abos mit Guthaben über mehrere Monate (z. B. Visual Studio); „not available for subscriptions with commitment plans or with pay-as-you-go pricing“; Betrag nicht änderbar („Custom spending limits aren't available“). Budgets sind reine Warnung: „Resources aren't affected, and your consumption isn't stopped“. Aktionsgruppen können Automation (Functions, Webhooks) auslösen. **F0-Ressource** (kostenlos, 0,5 Mio. Neural-Zeichen/Monat laut Preisseiten) wird bei Überschreitung laut einer Microsoft-Q&A-Antwort nicht kostenpflichtig, es kommen eher HTTP 429; F0-Limits sind nicht anpassbar | Budget wird alle 24 h gegen Kostendaten ausgewertet, die selbst 8–24 h alt sind; E-Mail meist binnen 1 h nach Auswertung | 429 (F0, nur Q&A-Aussage) | [Spending limit](https://learn.microsoft.com/en-us/azure/cost-management-billing/manage/spending-limit), [Budgets](https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets), [Q&A F0](https://learn.microsoft.com/en-us/answers/questions/5566384/azure-ai-speech-what-happens-after-free-tier-t0-ex), 21.10.2026 |
| **AWS / Polly** | **Nur selbstgebaut** | AWS Budgets lösen Aktionen aus: IAM-Richtlinie oder Service Control Policy anwenden, EC2/RDS-Instanzen anvisieren. Eine Polly-spezifische Abschaltung ist nicht dokumentiert; eine **IAM-Deny-Richtlinie auf Polly-Aufrufe** wäre über „apply an IAM policy“ denkbar (**abgeleitet, nicht belegt**). Free Tier ist kein harter Stopp | „AWS Budgets information is updated up to three times a day. Updates typically occur 8–12 hours after the previous update.“ Kosten können die Schwelle vor der Benachrichtigung überschreiten | – | [Budgets](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html), [Budget-Aktionen](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-controls.html), 21.10.2026 |
| **ElevenLabs** | **Ja, bei Abo ohne Pay-as-you-go** | Abo hat festes Monatskontingent. Ist es leer, kann man Pay-as-you-go-Credits kaufen (12 Monate gültig); ohne Kauf kein weiterer Verbrauch (**abgeleitet** aus der Doku-Aussage). „Usage based billing“ (Überverbrauch) ist ein **Altfeature**, nur Enterprise und alte Creator/Pro/Scale/Business-Tarife: einschaltbar/ausschaltbar, mit **Höchstzahl zusätzlicher Credits** oder „Unlimited“; Abrechnung am Zyklusende oder früher, wenn der offene Betrag das Doppelte der üblichen Abozahlung übersteigt | Überschuss wird zeitversetzt abgerechnet | – | [Billing-Doku](https://elevenlabs.io/docs/overview/administration/billing), 21.10.2026 |
| **Cartesia** | **Ja** | Overage ist nur für bezahlte Pläne verfügbar und **abschaltbar**. Aus: „new requests will be blocked if all your credits have been consumed“. Ein Ausgabenlimit oder Alerts nennt die Seite nicht | – | blockiert | [cartesia.ai/pricing](https://cartesia.ai/pricing), 21.10.2026 |
| **Alibaba Model Studio** | **Ja („Free Quota Only“)** | Schalter „Free Quota Only“ („worry-free mode“): Nach Verbrauch des Gratiskontingents antwortet der Dienst nicht mehr, kein Abbuchen. Standardmäßig **aus**. Aktivierung: Modellseite oder Seite Model Usage → Reiter Free Quota → Schalter in der Spalte „Actions“ (auch Batch-Aktion). Schalter erscheint **nicht** mehr, wenn das Kontingent schon aufgebraucht oder abgelaufen ist, also **vorher** einschalten. Kontingent für qwen3-tts-flash: **110.000 Zeichen, 90 Tage**, nur Region **Singapur** | Änderungen brauchen ca. **30 min** Sync, Aufrufe in dieser Zeit können noch kosten. Nicht verifizierte Konten werden nach Verbrauch ohnehin gestoppt, verifizierte werden weiter abgerechnet | Fehlercode **`AllocationQuota.FreeTierOnly`** | [Free quota](https://www.alibabacloud.com/help/en/model-studio/new-free-quota.md), [Preise](https://www.alibabacloud.com/help/en/model-studio/model-pricing), 21.10.2026 |
| **Mistral** | **Ja** | Monatliches **Spending Limit** auf Organisationsebene (API und Vibe) und je Workspace (darf das Organisationslimit nicht übersteigen). Bei Erreichen: „API access can be suspended until the next month begins or an admin increases the limit“. **Prepaid-Credits** mit optionalem Auto-Recharge. Credits leer: HTTP 402 | Guthaben kann leicht ins Minus rutschen („usage takes a short time to process“); Credits werden **inkl. Steuer** abgezogen, das Limit gilt **vor Steuer**, Credits können also vor dem Limit enden | 402 (Credits leer); Limit: Sperre, Fehlercode in der Org-Doku nicht genannt | [Billing](https://docs.mistral.ai/admin/billing-usage/billing), 21.10.2026. Hinweis: Eine Suchzusammenfassung nannte für Workspace-Limits HTTP 429, das ich auf der Billing-Seite nicht bestätigt fand. |
| **OpenAI** | **Ja** | **Prepaid-Credits** (Mindestkauf 5 $, Auto-Reload mit optionalem monatlichem Reload-Limit, Credits verfallen nach einem Jahr, laut Suchzusammenfassung der Hilfeseiten). **Hard Spend Limit** je Organisation oder Projekt (HTTP 429), **Spend Alerts** blockieren nichts. Zusätzlich vergibt OpenAI ein tierabhängiges Nutzungslimit | „Enforcement is not instantaneous, so recorded spend can slightly exceed the configured amount.“ | `organization_spend_limit_exceeded` / `project_spend_limit_exceeded` (429), `credit_balance_exhausted` | [Spend limits](https://developers.openai.com/api/docs/guides/spend-limits.md), 21.10.2026; Prepaid-Details nur aus Suchzusammenfassung der [Hilfeseite](https://help.openai.com/en/articles/8264644-how-does-prepaid-billing-work) (Direktabruf HTTP 403) |

### 3.2 Gemini API im Detail (Franks vermutlicher Hauptweg)

| Punkt | Aussage | Quelle (21.10.2026) |
|---|---|---|
| Zwei Abrechnungsarten | **Prepay**: Guthaben im Voraus, „The minimum purchase is $5“, „unused credits expire after 12 months and are non-refundable“. **Postpay**: Kosten laufen auf, Abbuchung am Monatsende oder beim Erreichen des tierabhängigen Caps | [Billing](https://ai.google.dev/gemini-api/docs/billing) |
| Wechsel | Gezielte Nachabfrage der Billing-Seite (21.10.2026): Postpay → Prepay ist möglich („Yes, you can transition an existing Postpay account on the AI Studio Billing page.“). Prepay → Postpay: FAQ sagt „No, switching from a Prepay billing plan to a Postpay billing plan is not supported“, der Abschnitt Erstattungen spricht aber von „When a Prepay account switches to the Postpay account type“ (Upgrade nach Erfüllen von Kriterien). **Seite intern uneinheitlich**; eine Suchzusammenfassung nannte das Gegenteil (Postpay → Prepay nicht möglich). Vor dem Einrichten in AI Studio prüfen. | [Billing](https://ai.google.dev/gemini-api/docs/billing); [Suche 21.10.2026](https://ai.google.dev/gemini-api/docs/billing?hl=cs) |
| Prepay leer | „All API keys in linked projects stop working. Requests then fail with an HTTP 402 Payment Required error until you add credits.“ Projekte fallen nicht automatisch auf Free zurück | [Billing](https://ai.google.dev/gemini-api/docs/billing) |
| Projekt-Cap | Pro Projekt, Rollen Editor/Owner/Admin, AI Studio → Spend → „Monthly spend cap“; „(experimental)“; seit 16.03.2026 | [Billing](https://ai.google.dev/gemini-api/docs/billing), [Blog](https://blog.google/innovation-and-ai/technology/developers-tools/more-control-over-gemini-api-costs/) |
| Tier-Caps | Tier 1: 250 $, Tier 2: 2.000 $, Tier 3: 20.000–100.000+ $ monatlich je Rechnungskonto; bei Überschreitung werden **alle Projekte des Kontos** bis zum nächsten Abrechnungszyklus pausiert | [Billing](https://ai.google.dev/gemini-api/docs/billing) |
| Auto-Reload-Limit | Prepay: „Manage auto-reload“ → „Monthly Limit“; danach wird Auto-Reload bis zum nächsten Monat abgeschaltet | ebd. |
| Spend-Cap-Budget (Cloud-Konsole) | Pro Projekt **und** Dienst; für Gemini API (`generativelanguage.googleapis.com`) zulässig. Auslösung bei bruttogeschätzten Kosten über Ziel; neue Nutzung wird pausiert, laufende Anfragen laufen zu Ende; „spend cap enforcement isn't instantaneous“; Grenzen: nur monatlich, Schätzung ohne Gutschriften, Budget nicht editierbar während Durchsetzung; Aufhebung am Periodenanfang automatisch | [Spend-Cap-Budgets](https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps) |
| Forum-Meldung (ungeprüft) | Ein Nutzer mit Prepaid-Guthaben (10 €) und Monatscap (10 €) bekam trotz „0,00 € von 10,00 € ausgegeben“ HTTP 429 „monthly spending cap exceeded“; keine bestätigte Lösung | [Forum](https://discuss.ai.google.dev/t/gemini-api-429-monthly-spending-cap-exceeded-despite-tier-1-and-prepaid-balance/179506), Fundstelle aus Suche 21.10.2026, Seite nicht geöffnet |

**Empfehlung als Einordnung, nicht als Beleg:** Für Gemini-TTS ist die Kombination **Prepay-Guthaben (kleiner Betrag) + Projekt-Cap** die einzige Konstellation, in der aus den Quellen ein echter harter Stopp hervorgeht. Für klassisches Google Cloud TTS fehlt ein offiziell dokumentierter harter Stopp.

### 3.3 Höchstausgabe über Quoten (Rechenformel, wenn Quotensenkung erlaubt ist)

Falls die TTS-Quote pro Minute senkbar ist (UNBELEGT), begrenzt sie nur die Rate. Obergrenze der Tagesausgabe:

`Kosten_max/Tag = q × 1.440 × z_max × p_zeichen`

mit q = erlaubte Anfragen/min, z_max = Zeichen je Anfrage (Cloud TTS: höchstens 5.000 Bytes), p_zeichen = Preis je Zeichen. Beispiel nur zur Größenordnung (abgeleitet): q = 1, z_max = 5.000, p = 30 $/Mio. → 1 × 1.440 × 5.000 × 0,00003 = 216 $/Tag. Das ist **keine praktikable Kostenbremse**, weil selbst 1 Anfrage/min bei Volllast sehr teuer wird.

---

## 4. Nutzungsprofile eines persönlichen Sprachassistenten

### 4.1 Annahmen

| Profil | Sprechzeit/Tag | Begründung (eigene Annahme, kein Messwert) |
|---|---|---|
| **Wenig** | 5 min | Eine Tagesansage plus etwa 10–15 kurze Antworten (Wetter, Timer, Termin). Entspricht grob 15 Antworten à 20 s. |
| **Mittel** | 20 min | Morgenbriefing (3–5 min), laufende Rückfragen, vorgelesene Nachrichten und Benachrichtigungen. Etwa 60 Antworten à 20 s. |
| **Intensiv** | 60 min | Dauerdialog: Mails und Artikel vorlesen, Recherche-Zusammenfassungen, Navigation, Hörbuch-artige Texte. Etwa 180 Antworten à 20 s. |

Gestützt nur schwach: Sprachsuche-Antworten haben laut einer Sekundärquelle im Mittel **29 Wörter** ([Greenlight Digital über eine Dean-Studie](https://greenlightdigital.com/blog/blog-posts/voice-search-ranking-factors-can-you-guess/), Original nicht gefunden; Direktabruf lief ins Leere) bzw. **41,4 Wörter** ([SEMrush-Studie, nur in Suchtreffer gesehen](https://www.semrush.com/blog/voice-search-study)), also 13–19 s bei 130 Wörtern/min. Meine Annahme „20 s je Antwort“ liegt im selben Bereich. **Echte Assistenten-Nutzungsstatistik für Deutschland: UNBELEGT.**

Monat = 30 Tage. Faktoren: **900 Zeichen/min** (Spanne 750–1.000), **25 Audio-Tokens/s** für Gemini, **60 s/min**.

### 4.2 Formeln

| Größe | Formel |
|---|---|
| Audio-Sekunden/Monat | `S = M × 60 × 30` (M = Minuten pro Tag) |
| Zeichen/Monat | `Z = M × c × 30` (c = Zeichen/min, hier 900) |
| Anfragen/Monat | `N = S ÷ t_Antwort` (t_Antwort = 20 s) |
| Gemini Audio-Ausgabe-Tokens | `T_out = 25 × S` |
| Gemini Text-Eingabe-Tokens | `T_in = N × I + Z ÷ k` (I = Stil-Anweisung in Tokens, k = Zeichen/Token; k ≈ 3,8 Fließtext, ≈ 2,3 ziffernlastig) |
| Kosten Gemini | `K = T_in × p_in + T_out × p_out` (Preise je Token = Preis je Mio. ÷ 10⁶) |
| Kosten Zeichenpreis (Google Cloud, Azure, Polly, ElevenLabs, Cartesia, Mistral) | `K = Z_abr × p_zeichen`, mit `Z_abr = Z × f` |
| Zählfaktor f | Google: ≥ 1 (SSML-Tags zählen mit); Azure: ≥ 1 (Tags außer speak/voice); Polly: 1 (Tags frei); Alibaba: **1,34 (nur eine Messung, UNBELEGT als Regel)** |
| Kosten OpenAI mini-TTS | `K ≈ S ÷ 60 × 1.200 × 12 ÷ 10⁶` (abgeleitet, ≈ 0,0144 $/min) plus Eingabe |
| Free-Quota-Prüfung Alibaba | `Monate_gratis = 110.000 ÷ (Z × f)` (nur Singapur, 90 Tage ab Aktivierung) |

### 4.3 Ergebnis (Faktor 900 Zeichen/min, 25 Tokens/s)

| Größe | Wenig (5 min/Tag) | Mittel (20 min/Tag) | Intensiv (60 min/Tag) |
|---|---|---|---|
| Sprechzeit/Monat | 150 min = 2,5 h | 600 min = 10 h | 1.800 min = 30 h |
| **Audio-Sekunden/Monat** | **9.000 s** | **36.000 s** | **108.000 s** |
| **Zeichen/Monat** (900/min) | **135.000** | **540.000** | **1.620.000** |
| Zeichen/Monat bei 750/min (unten) | 112.500 | 450.000 | 1.350.000 |
| Zeichen/Monat bei 1.000/min (oben) | 150.000 | 600.000 | 1.800.000 |
| Zeichen/Monat bei 520–620/min (ziffernlastig wie Testtext A) | ca. 78.000–93.000 | ca. 312.000–372.000 | ca. 936.000–1.116.000 |
| Gemini Audio-Ausgabe-Tokens/Monat | 225.000 | 900.000 | 2.700.000 |
| Anfragen/Monat (20 s) | 450 | 1.800 | 5.400 |
| Gemini Text-Eingabe-Tokens (k = 3,8; I = 50) | ca. 35.500 + 22.500 = **58.000** | ca. 142.000 + 90.000 = **232.000** | ca. 426.000 + 270.000 = **696.000** |
| Alibaba abrechenbare Zeichen (f = 1,34, nur Eigenmessung) | 181.000 | 724.000 | 2.171.000 |
| Alibaba Gratis-Kontingent 110.000 Zeichen reicht für | ca. 0,6 Monate | ca. 0,15 Monate | ca. 0,05 Monate |

Die Eingabe-Tokens-Zeile ist eine Größenordnung; die Stil-Anweisung I = 50 Tokens ist **Annahme**, der Hauptagent hat 152 Tokens für Text + Anweisung gemessen, die Aufteilung ist mir nicht bekannt.

Kontrollrechnung Audio-Dauer ↔ Zeichen: 900 Zeichen/min ÷ 60 = 15 Zeichen/s; 9.000 s × 15 = 135.000 Zeichen. ✓

### 4.4 Preise (nur als Nebenbefund, ohne Rechnung)

Gefunden beim Lesen der Quellen, damit der Hauptagent die Formeln schneller füllen kann. Preise ändern sich; vor Verwendung gegen die Seite prüfen. Abruf alle 21.10.2026.

| Dienst | Preis | Quelle |
|---|---|---|
| Gemini 3.8 Flash TTS (Preview) | Eingabe 0,50 $/Mio. Text-Tokens, Ausgabe 9,00 $/Mio. Audio-Tokens bis 31.12.2026; ab 01.01.2027 1,00 $ und 18,00 $ | [Cloud-Preise](https://cloud.google.com/text-to-speech/pricing?hl=en), [Gemini-API-Preise](https://ai.google.dev/gemini-api/docs/pricing) |
| Gemini 3.8 Flash-Lite TTS | 0,50 $ / 6,00 $ bis 31.12.2026; danach 1,00 $ / 12,00 $ | ebd. |
| Gemini 2.5 Flash TTS | 0,50 $ / 10,00 $ | ebd. |
| Gemini 3.1 Flash TTS Preview, 2.5 Pro TTS | 1,00 $ / 20,00 $ | ebd. |
| Gemini TTS, Batch | 50 % Nachlass („Batch API (50% cost reduction)“) | [Gemini-API-Preise](https://ai.google.dev/gemini-api/docs/pricing) |
| Google Cloud Chirp 3: HD | 30 $/Mio. Zeichen, 1 Mio. Zeichen/Monat frei | [Cloud-Preise](https://cloud.google.com/text-to-speech/pricing?hl=en) |
| Google Neural2 / Polyglot | 16 $/Mio.; 1 Mio. frei | ebd. |
| Google WaveNet / Standard | 4 $/Mio.; 4 Mio. frei | ebd. |
| Google Studio | 160 $/Mio.; 1 Mio. frei | ebd. |
| Amazon Polly | Standard 4 $, Neural 16 $, Long-Form 100 $, Generative 30 $ je Mio. Zeichen; Freikontingent teils 12 Monate | [Polly-Preise](https://aws.amazon.com/polly/pricing/) |
| ElevenLabs API | Flash/Turbo 0,04 $/1k Zeichen; Multilingual v2 und v3 0,08 $/1k | [Preise](https://elevenlabs.io/pricing/api) |
| Mistral Voxtral TTS | 16 $/Mio. Zeichen (Voxtral TTS), 13 $/Mio. (Voxtral TTS 3) | [Mistral Pricing](https://docs.mistral.ai/inference/pricing) |
| Alibaba qwen3-tts-flash | 0,10 $ je 10.000 Zeichen (Singapur), 0,114682 $ (Peking) | [Preisliste](https://www.alibabacloud.com/help/en/model-studio/model-pricing) |
| OpenAI gpt-4o-mini-tts | 0,60 $ Text-Eingabe, 12 $ Audio-Ausgabe je Mio. Tokens | [Modellseite](https://developers.openai.com/api/docs/models/gpt-4o-mini-tts) |
| Cartesia Pro | 5 $/Monat mit 100.000 Credits; Overage 65 $/Mio. | [cartesia.ai/pricing](https://cartesia.ai/pricing) |

---

## 5. Offene Punkte und Messaufträge für den Hauptagenten

1. **WAV-Dauer gegen Tokens prüfen.** Gemini: Tokens ÷ Sekunden. Erwartet 25; weicht es ab, gilt der Messwert.
2. **Alibaba-Zählung klären** (Abschnitt 2.2): Kontrollaufrufe mit Reststrings.
3. **Kosten an zwei Texten rechnen:** Fließtext (ca. 900 Zeichen/min) und Testtext A (ziffernlastig, ca. 520–620 Zeichen/min), weil Zeichenpreise sonst beim Assistenten mit Zahlen/Uhrzeiten systematisch zu hoch oder zu niedrig wirken. Audiozeit-Preise (Gemini, OpenAI) hängen dagegen an der Dauer.
4. **Spend-Cap-Budget für Cloud TTS** mit der Konsole prüfen; zum Abrufzeitpunkt steht Cloud TTS nicht in der Liste zulässiger Dienste.
5. **Prepay/Postpay-Richtung beim Gemini-API-Wechsel** in AI Studio prüfen (die Billing-Seite widerspricht sich selbst, siehe 3.2).
6. **Mindestabrechnung** an einem 3-Zeichen-Text nachmessen.
7. **Alibaba „Free Quota Only“:** Region und Aktivierungszeitpunkt prüfen. Eine andere Alibaba-Seite beschränkt die Funktion auf China (Peking), die Free-Quota-Seite auf Singapur; Widerspruch in den Quellen.

---

BEST-PRACTICES-KANDIDATEN:
- **TTS-Kostenrechnung nie nur über Zeichen:** Kosten an Audio-Sekunden festmachen (Formel `Z = M × 900 × 30`, `T_out = 25 × S`), Zeichenanbieter zusätzlich an einem ziffernlastigen Text messen (Zahlen werden ausgeschrieben und machen Audio länger).
- **Gemini-Audio-Tokens:** TTS-Ausgabe 25 Tokens/s, Audio-Eingabe 32 Tokens/s. Bei Kostenrechnung die Richtung beachten und gegen die echte WAV-Dauer eichen.
- **Kostenbremse für Gemini API:** Prepay-Guthaben (klein) plus Projekt-Cap in AI Studio, Spend-Cap-Budget in der Cloud-Konsole für `generativelanguage.googleapis.com`; Verzögerung ca. 10 min einplanen.
- **Alibaba „Free Quota Only“ vor Verbrauch des Kontingents einschalten:** Der Schalter verschwindet, sobald das Kontingent aufgebraucht oder abgelaufen ist; Änderungen brauchen ca. 30 min.
- **Google Cloud Budgets sind nur Warnungen:** „Alerts-only“ begrenzt nichts. Selbstbau über Pub/Sub schaltet die Abrechnung des ganzen Projekts ab und kann Ressourcen löschen. Quoten begrenzen nur Raten, nicht Ausgaben.
- **Zeichenzählung je Anbieter:** Google zählt SSML-Tags (außer `<mark>`), Azure zählt Tags (außer `<speak>`/`<voice>`) und chinesische Zeichen doppelt, Polly und Alibaba zählen Tags nicht.

BUG-KANDIDATEN:
- **Alibaba qwen3-tts-flash zählt 477 „characters“ für einen deutschen Text mit 356 Unicode-Zeichen** (Faktor 1,34), den die dokumentierte Regel (Chinesisch 2, sonst 1, SSML frei) nicht erklärt. Ursache offen (Normalisierung, Zusatztext oder Zählung?). Kontrollaufrufe fehlen.
- **Widerspruch innerhalb der Gemini-API-Billing-Seite:** FAQ sagt, Prepay → Postpay werde nicht unterstützt, der Erstattungsabschnitt beschreibt aber genau diesen Wechsel („When a Prepay account switches to the Postpay account type“). Postpay → Prepay ist laut FAQ möglich; eine Suchzusammenfassung behauptete das Gegenteil.
- **Gemini-API-Forum: HTTP 429 „monthly spending cap exceeded“** trotz Prepaid-Guthaben und unverbrauchtem Cap (ungeprüfte Einzelmeldung, Stand Suche 21.10.2026).
- **Alibaba-Doku widersprüchlich** zur Region von „Free Quota Only“ (Singapur laut Free-Quota-Seite, China/Peking laut einer anderen Seite).
- **Gebhard-Dissertation (2012):** Der Morphem-Wert 3,82/s für Deutsch ist identisch mit dem englischen Wert im Text – möglicher Abschreibfehler; Tabelle 4.28/4.33 im PDF durch Texterkennung unsauber.
