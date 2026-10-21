# R3 — Microsoft Azure AI Speech und Edge-TTS (Stand 21.10.2026)

Recherchezeitpunkt: 21.10.2026. Alle Quellen wurden am 21.10.2026 abgerufen (Abrufdatum je Zeile nicht wiederholt, außer bei Abweichung). Methode: WebSearch/WebFetch, offizielle Microsoft-Dokumentation bevorzugt. Nichts aus dem Ordner SK gelesen. Belegstufen: `offiziell` (Microsoft-Doku/-API), `extern` (GitHub, Dritte), `unbelegt` (nicht gefunden oder nur Hörensagen).

Abgrenzung zum Bestandswissen (`best-practices/apis/tts-provider.md`, `bugs/apis/tts-provider.md`, Stand 14.06.2026 bzw. 27./28.09.2026): Dieser Bericht liefert nur das Delta. Die wichtigsten Abweichungen stehen in Abschnitt 0.

---

## 0. Kurzfassung der Abweichungen zum Bestandswissen

| # | Bestandswissen | Befund 21.10.2026 | Quelle |
|---|---|---|---|
| 1 | edge-tts 7.2.8 (22.03.2026), Token `1-…` (SHA-256) | **edge-tts 7.3.1 (13.10.2026)**; Microsoft verlangt jetzt Sec-MS-GEC **v2** (`2-<Version>`, Token `<16 Hex>-<64 Hex>`). Version 7.2.8 liefert ab 10.10.2026 regional 403. | GitHub-Releases, PR #492, Issue #490 |
| 2 | Bug W10: „Bing validiert `Sec-MS-GEC-Version` aktuell nicht streng" | **Überholt.** Die Version-Zeichenkette und das Tokenformat werden jetzt geprüft (v1-Token führt zu 403). | PR #492 |
| 3 | §13: „Microsoft sagt dazu selbst: Kommerzielle Nutzung ohne bezahltes Azure-Abo kann die Nutzungsbedingungen verletzen" mit Verweis auf Q&A 5805156 | Der abgerufene Q&A-Thread handelt **nur von Azure Speech F0** (Moderatoren zitieren die Product Terms), nicht von Edge Read Aloud. Eine Microsoft-Aussage zu edge-tts/Read-Aloud habe ich **nicht gefunden**. | learn.microsoft.com/answers/questions/5805156 |
| 4 | Azure Neural 16 $ je 1 Mio Zeichen | **15 $** (Azure Retail Prices API, ab 01.02.2024; Doku-Seite Quotas nennt ebenfalls „$15 per million characters"). Neural HD **22 $** (ab 01.03.2026) bestätigt. | prices.azure.com, Quotas-Seite |
| 5 | Azure-Stimmen: Neural, Seraphina/Florian HD | Neu: **MAI-Voice-2 / 2.1 (+ Flash)** mit deutschen Stimmen Klaus, Mia, Grant, Harper (Public Preview, ohne SLA); **DragonHD Omni** (700+ Stimmen, Namensmuster `de-DE-Conrad:DragonHDOmniLatestNeural`). | Sprachunterstützung, HD-Seite, MAI-Seite |
| 6 | Android-Speech-SDK nicht beziffert | `client-sdk` **1.52.0** (28.09.2026), AAR **12,9 MB**; Textstreaming-Eingabe gibt es nur in C#/C++/Python, **nicht in Java/Android**. | Maven Central, Latenz-Seite |
| 7 | Edge-Stimmenliste (Jarvis): Seraphina, Katja, Amala, Florian, Killian, Conrad | Live-Stimmenliste des Edge-Endpunkts (21.10.2026): de-DE = SeraphinaMultilingual, FlorianMultilingual, Amala, Conrad, Katja, Killian (deckungsgleich mit Jarvis). **Keine DragonHD- und keine MAI-Stimmen**, auch nicht Bernd, Christoph, Elke u. a. aus Azure. | speech.platform.bing.com Stimmenliste |

---

## 1. Aktuelle deutsche Stimmen (exakte Namen)

Quelle: `offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support?tabs=tts (Seite abgerufen 21.10.2026), HD-Seite https://learn.microsoft.com/en-us/azure/ai-services/speech-service/high-definition-voices (Doku-Stand 24.09.2026), MAI-Seite https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-voices (Doku-Stand 01.10.2026).

### 1.1 de-DE (Deutschland)

| Stimme (SSML-Name) | Geschlecht | Klasse | Status / Anmerkung |
|---|---|---|---|
| `de-DE-Seraphina:DragonHDLatestNeural` | w | Neural HD (DragonHD) | GA (HD-Seite) |
| `de-DE-Florian:DragonHDLatestNeural` | m | Neural HD (DragonHD) | GA (HD-Seite) |
| `de-DE-SeraphinaMultilingualNeural` | w | Multilingual (ältere Generation, „less contextual awareness") | GA |
| `de-DE-FlorianMultilingualNeural` | m | Multilingual | GA |
| `de-DE-KatjaNeural` | w | Standard | GA |
| `de-DE-ConradNeural` | m | Standard (Stile `cheerful`, `sad`) | GA |
| `de-DE-AmalaNeural` | w | Standard | GA |
| `de-DE-BerndNeural`, `de-DE-ChristophNeural`, `de-DE-KasperNeural`, `de-DE-KillianNeural`, `de-DE-KlausNeural`, `de-DE-RalfNeural` | m | Standard | GA |
| `de-DE-ElkeNeural`, `de-DE-KlarissaNeural`, `de-DE-LouisaNeural`, `de-DE-MajaNeural`, `de-DE-TanjaNeural` | w | Standard | GA |
| `de-DE-GiselaNeural` | w, Kind | Standard | GA |
| `de-DE-Klaus:MAI-Voice-2`, `de-DE-Mia:MAI-Voice-2` (Sprachunterstützung) | m / w | Neural HD (MAI) | Public Preview (Fußnote 1) |
| `de-DE-Klaus:MAI-Voice-2-Flash`, `de-DE-Mia:MAI-Voice-2-Flash` | m / w | Neural HD Flash (MAI) | Public Preview |

**Unstimmigkeit in der Doku (zu beachten):** Die Sprachunterstützung führt `…:MAI-Voice-2`, die MAI-Seite (01.10.2026) dagegen `…:MAI-Voice-2.1` und `…:MAI-Voice-2.1-Flash` und zusätzlich `de-DE-Grant`, `de-DE-Harper`. Welche Namen die API heute tatsächlich akzeptiert, ist nicht belegt; vor dem Einbau per `voices/list` der Region prüfen.

MAI-Stile für `de-DE-Klaus` und `de-DE-Mia` (MAI-Seite): `angry, confused, determined, disgusted, embarrassed, excited, fearful, happy, hopeful, jealous, joyful, neutral, regretful, relieved, sad, shouting, softvoice, surprised, whispering`. Grant und Harper: `agent, audiobook, customer_call_center, educational, narrator, neutral`.

DragonHD Omni: `offiziell` (HD-Seite) — „700+ voices", Namensmuster `languagelocale-voicename:DragonHDOmniLatestNeural`, Beispiel „`de-DE-Conrad:DragonHDOmniLatestNeural`". Eine vollständige Liste deutscher Omni-Stimmen habe ich nicht gefunden (`unbelegt`; per `voices/list` abfragen).

### 1.2 de-AT und de-CH

| Locale | Stimmen |
|---|---|
| de-AT | `de-AT-IngridNeural` (w), `de-AT-JonasNeural` (m) |
| de-CH | `de-CH-LeniNeural` (w), `de-CH-JanNeural` (m) |

Beide nur Standard. Multilingual-Stimmen können de-AT und de-CH als Zielsprache über `<lang xml:lang>` ansprechen (Sprachunterstützung, Tabelle der Multilingual-Locales).

### 1.3 Personal Voice, Custom Neural Voice

| Funktion | Zugang | Quelle |
|---|---|---|
| Custom Voice (Professional Voice) | „limited access feature", Antrag über https://aka.ms/customneural | Preisseite (Fußnote), Text-to-speech-Übersicht |
| Personal Voice | „restricted to certain pre-approved use cases only", Antrag nötig; de-DE ist in der Locale-Tabelle gelistet | Preisseite (Fußnote), Sprachunterstützung |
| MAI-Voice Instant Voice Cloning | gated, derselbe Limited-Access-Antrag, Referenzclip 5–60 s | MAI-Seite |

### 1.4 Edge-Endpunkt: tatsächlich verfügbare deutsche Stimmen

`extern` Eigene Abfrage der Stimmenliste `https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=…` am 21.10.2026 (nur die ersten 100.000 Zeichen der Antwort gelesen; die deutschen Einträge standen davor):

`de-AT-IngridNeural`, `de-AT-JonasNeural`, `de-DE-SeraphinaMultilingualNeural`, `de-DE-FlorianMultilingualNeural`, `de-DE-AmalaNeural`, `de-DE-ConradNeural`, `de-DE-KatjaNeural`, `de-DE-KillianNeural`, `de-CH-JanNeural`, `de-CH-LeniNeural`. Alle Status GA, Codec `audio-24khz-48kbitrate-mono-mp3`. Die Stimmenliste ist **nicht** von der 403-Welle betroffen gewesen (die Abfrage lief durch); die Synthese (WebSocket) habe ich nicht getestet.

---

## 2. Preise, Freikontingent, Kostenbegrenzung

### 2.1 Preise

Die offizielle Preisseite (https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/) liefert die Beträge per JavaScript nach; der Abruf zeigte nur Platzhalter („$-"). Belegt sind die Beträge deshalb über die **Azure Retail Prices API** (`offiziell`, https://prices.azure.com/api/retail/prices, Filter `productName eq 'Azure Speech'`, Region westeurope, USD, abgerufen 21.10.2026):

| Posten (meterName) | USD je 1 Mio Zeichen | gültig ab |
|---|---|---|
| S1 Neural Text To Speech Characters („Neural") | **15,00** | 01.02.2024 |
| Neural HD Text to Speech Characters | **22,00** | 01.03.2026 |
| S1 Text To Speech Characters (Standard, nicht neural) | 4,00 | 01.11.2018 |
| Text to Speech – Personal Voice Characters | 24,00 | 01.02.2024 |
| S1 Custom Neural Realtime Characters | 24,00 | 01.02.2021 |
| CNV Neural HD Synthesis Characters (Custom Voice HD) | 48,00 | 01.07.2025 |
| Commitment Tier Neural TTS (80M … 4000M, Überschreitung) | 12,00 bis 5,70 | 2024 |
| Free Text To Speech Characters | 0,00 | 07.05.2018 |

- **Neural HD Flash** und **MAI-Voice**: In der Retail-Prices-API habe ich **keinen** Zeichenpreis gefunden (Suche nach „MAI" ergab nur MAI-Transcribe-2, MAI-Thinking-1, MAI-Cyber-1-Flash, MAI-DS-R1; Suche nach „Flash" unter Foundry Tools leer). Die Preisseite führt „Neural / Neural HD Flash" als eigene Zeile, Betrag nicht lesbar. Dritte nennen für MAI-Voice-2.1 rund 22 $ je 1 Mio Zeichen (`extern`, https://www.llmreference.com/model/mai-voice-2-1/microsoft-foundry; https://techcommunity.microsoft.com/blog/azure-ai-foundry-blog/new-mai-models-in-microsoft-foundry-across-text-image-voice-and-speech/4524632 laut Suchtreffer) — **nicht amtlich bestätigt**. Die MAI-Seite verweist nur auf die Preisseite.
- Ein Drittvergleich (inventivehq.com, Stand 08.10.2026) nennt Neural 15 $ und Neural HD 22 $ ohne Freikontingent; deckt sich mit der API.
- Regionale Preisunterschiede: nur westeurope abgefragt. Andere Regionen nicht geprüft.

### 2.2 Freikontingent (F0) und Azure-Free-Account

| Aussage (wörtlich oder sinngemäß) | Quelle |
|---|---|
| Free (F0), Neural: „0.5 million characters free per month" | Preisseite, Abruf 21.10.2026 |
| Die F0-Tabelle der Preisseite nennt **nur Neural**; ein Freikontingent für HD, HD Flash oder MAI ist dort **nicht aufgeführt** (`unbelegt`, ob HD in F0 nutzbar ist) | Preisseite |
| Free-Account-Seite: Azure Speech in Foundry Tools ist „always free" mit „0.5 million neural characters per month" (also dauerhaft monatlich, nicht einmalig) | https://azure.microsoft.com/en-us/free/ |
| 200-$-Guthaben „to use on Azure services within 30 days" (einmalig); deckt auch Nutzung über die Freimengen hinaus, nicht Marketplace | ebd. |
| 12-Monate-Dienste gelten für neue Kunden bis 12 Monate nach Kontoerstellung (Speech steht in der „Always"-Gruppe, nicht in der 12-Monate-Gruppe) | ebd. |
| Anmeldung braucht Telefonnummer, **Kredit- oder Debitkarte (nicht prepaid)** und ein Microsoft- oder GitHub-Konto; zeitweise 1-$-Prüfbuchung, wird freigegeben | ebd. |
| Nach 30 Tagen oder verbrauchtem Guthaben muss man auf Pay-as-you-go wechseln, sonst: „your account and services will be disabled" | ebd. |
| „Spending protection — credit card won't be charged", solange man nicht auf Pay-as-you-go wechselt | ebd. |

Folgerung (aus den Aussagen abgeleitet): Eine Karte ist nötig; ohne Wechsel auf Pay-as-you-go wird nichts abgebucht, aber das Konto wird nach 30 Tagen bzw. nach Verbrauch des Guthabens deaktiviert. Ob die F0-Ressource dann weiterläuft, steht auf der Seite nicht eindeutig (`unbelegt`).

### 2.3 Ratenlimits

Quelle `offiziell`: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits (Doku-Stand 19.10.2026).

| Limit (Echtzeit-TTS) | Free (F0) | Standard (S0) |
|---|---|---|
| Transaktionen für Standard- und Custom-Stimmen | **20 Transaktionen / 60 s**, nicht anpassbar | 30 TPS Standard, anpassbar bis 1.000 TPS |
| Max. Audiolänge je Anfrage | 10 min | 10 min |
| Max. `<voice>`/`<audio>`-Tags in SSML | 50 | 50 |
| Max. SSML-Nachricht je Turn (WebSocket) | 64 KB | 64 KB |
| Batch-Synthese | nicht verfügbar | 100 Anfragen / 10 s, Payload 2 MB |

- Ein eigenes Limit für HD-/MAI-Stimmen steht in der Tabelle **nicht** (`unbelegt`).
- Doku-Hinweis: Die meisten HTTP-429-Fehler bei Standardstimmen kommen von **begrenzter Backend-Kapazität** für eine Stimme in einer Region, nicht vom Kontingent. Empfehlung: Stimme in ihrer Heimatregion nutzen oder eine verbreitetere Stimme wählen. Retry-Logik für 429 ist ausdrücklich Pflicht.
- Anhebung der S0-Grenze verlangt eine geschäftliche E-Mail-Adresse (private Adressen wie gmail.com werden abgelehnt) und „work or organization email" — für Einzelentwickler relevant.

### 2.4 Zählweise

`offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/text-to-speech (Abschnitt „Billable characters", Doku-Stand 18.08.2026):

- Abgerechnet wird jedes Zeichen jeder erfolgreich verarbeiteten Anfrage, auch ohne erzeugtes Audio („Charges apply even if speech is not generated due to a mismatch between the selected voice language and the input text").
- Zählt: Text im SSML-Body, **alle Markups im Textfeld außer `<speak>` und `<voice>`**, Buchstaben, Satzzeichen, Leerzeichen, Tabs, Whitespace, jeder Unicode-Codepoint.
- Chinesische Zeichen zählen doppelt (auch Kanji, Hanja, Hanzi).
- Folge: SSML-Tags (`<say-as …>`, `<prosody …>`, `<break …/>`) kosten Zeichen. Bei Dauerbetrieb sparsam markieren oder per Vorverarbeitung im Text normalisieren.

### 2.5 Kostenbegrenzung

- Es gibt **keine harte Ausgabengrenze**. Budgets in Cost Management **warnen nur**: „Resources aren't affected, and your consumption isn't stopped" (`offiziell`, https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets). Daten sind 8–24 h verzögert, Budgets werden alle 24 h ausgewertet. Automatisches Abschalten nur über Action Groups mit eigener Automation (Functions/Webhook).
- Praktisch wirksam sind: F0-Ressource (hartes Limit 20 Anfragen/60 s und 0,5 Mio Zeichen), das nicht aufgeladene Free-Account-Guthaben ohne Wechsel auf Pay-as-you-go, und ein eigener Zähler im Token-Broker.
- Ein Spending-Limit nach dem Muster von Visual-Studio-Abos gibt es für den Free-Account nicht benannt; die Seite spricht nur von „Spending protection" (siehe 2.2).

### 2.6 Kommerzielle Nutzungsrechte

- `offiziell` über Moderatorenzitat (zweite Hand, nicht direkt aus den Product Terms gelesen): „For Customers of the paid tier TTS Service only, Customer may use the audio output of prebuilt neural voices generated using the TTS Service, including for commercial purposes." Der Free-Tier sei „a pricing allowance for evaluation/testing and does not carry those output use rights" (https://learn.microsoft.com/en-us/answers/questions/5805156/). Ein früherer, widersprechender Moderator-Beitrag wird in dem Thread erwähnt, war aber nicht lesbar.
- Die Product Terms selbst habe ich nicht abgerufen (`unbelegt`). Für ein privates Eigenprojekt wie Jarvis ist das unkritisch; für eine Verkaufs-App Pflichtlektüre vor Produktion (siehe Bestandswissen §17: Caching-Klausel unklar).
- MAI-Seite: „Usage: Available for third-party developers. Microsoft holds full licensing rights for commercial use."

---

## 3. Deutsch-Fähigkeiten, SSML und Textnormalisierung

### 3.1 SSML-Elemente je Stimmklasse

`offiziell`: HD-Seite, Tabelle „Supported and unsupported SSML elements" (Doku-Stand 24.09.2026); Strukturseite https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-synthesis-markup-structure.

| Element | Standard-Neural (Katja, Conrad, Amala …) | DragonHD (Seraphina, Florian) | DragonHD Omni | MAI-Voice |
|---|---|---|---|---|
| `say-as` | ja | ja | ja | nicht dokumentiert |
| `phoneme` (ipa, sapi, ups, x-sampa) | ja; sapi-Phonemset für de-DE/de-AT/de-CH definiert | ja | **nein** | nicht dokumentiert |
| `lexicon` | ja | nur `alias` | nur `alias` | nicht dokumentiert |
| `sub` | ja | ja | ja | nicht dokumentiert |
| `break` | ja (0–20.000 ms; Stärken 250–1.250 ms) | ja | **nein** | nicht dokumentiert |
| `prosody` (rate, pitch, volume, contour) | ja (rate 0,5–2×) | **nein** | **nein** | nicht dokumentiert |
| `emphasis` | ja | nein | nein | nicht dokumentiert |
| `mstts:express-as` | nur Stimmen mit Stilliste (de-DE: nur Conrad `cheerful`, `sad`) | **nein** | ja | ja (`style`) |
| `mstts:silence` | ja | nein | nein | nicht dokumentiert |
| `lang xml:lang` | nur Multilingual-Stimmen | ja | ja | ja (mehrsprachig) |
| `p`, `s` | ja | ja | ja | nicht dokumentiert |

Hinweise:
- DragonHD erlaubt Stil-Marker auch als `[excited]` im Text; „Styles are available on all English content for all voices", Paralinguistik (`laughter`, `breathing` …) „on all voices with all languages". Für Deutsch sind **Stile bei HD ausdrücklich nur für englischen Inhalt zugesagt** (HD-Seite, Hinweis unter den Stiltabellen).
- Widerspruch in der Doku: Fußnote 3 der Sprachunterstützung sagt „Phonemes, custom lexicon, and visemes aren't supported" (Zuordnung zu bestimmten Stimmen im Abruf nicht erkennbar), die HD-Seite sagt „phoneme: Yes" für DragonHD. Vor Nutzung empirisch prüfen.
- HD-Parameter in `<voice parameters="…">`: `temperature` (DragonHD Standard 1,0; Omni 0,7), `top_p`, `top_k`, `cfg_scale` (nur Omni), `enhancePronunciation=true` (verbessert Aussprache von Eigennamen, Akronymen, Abkürzungen; ersetzt keine SSML-Steuerung).
- Eine Zeile aus dem Preisabschnitt: SSML-Tags zählen als Zeichen (Abschnitt 2.4).

### 3.2 `say-as` im Detail

`offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-synthesis-markup-pronunciation (Doku-Stand 05.06.2026):

- `interpret-as`-Werte: `characters`/`spell-out` (mit `format="casesensitive"`), `alphanumeric` (`format="spell"`), `cardinal`/`number`, `ordinal`, `number_digit`, `fraction`, `date` (Formate `dmy, mdy, ymd, ym, my, md, dm, d, m, y`), `time` (`hms12`, `hms24`), `duration` (nur Englisch und Spanisch), `telephone`, `currency`, `unit`, `address`, `name`.
- Geltungsbereich: `characters` und `spell-out` gelten für alle Locales; die übrigen Werte für alle Locales der Sprachen Arabisch, Katalanisch, Chinesisch, Dänisch, Niederländisch, Englisch, Französisch, Finnisch, **Deutsch**, Hindi, Italienisch, Japanisch, Koreanisch, Norwegisch, Polnisch, Portugiesisch, Russisch, Spanisch, Schwedisch.
- Alle Beispiele der Doku sind **englisch** (z. B. „10-12-2016", „99.9 USD", „10 m"). Wie deutsche Eingaben („1.249,99 €", „30,5 ms") behandelt werden, steht **nirgends** in der Doku (`unbelegt`); das ist nur durch Hörprobe zu klären.
- Attribut `detail`: „Indicates the level of detail to be spoken. For example, this attribute might request that the speech synthesis engine pronounce punctuation marks. There are no standard values defined for `detail`." Es gibt also **keinen dokumentierten Wert**, der Satzzeichen wörtlich sprechen lässt.

### 3.3 Satzzeichen wörtlich sprechen (Testtext B)

- Dokumentiert ist: „The Speech service automatically handles punctuation as appropriate, such as pausing after a period, or using the correct intonation when a sentence ends with a question mark" (Strukturseite). Das ist das Gegenteil von wörtlichem Vorlesen.
- Ein belegter Weg zur wörtlichen Aussprache ist `<sub alias="Komma">,</sub>` (Element `sub` ist für alle Stimmklassen inklusive HD dokumentiert) bzw. ein Lexikon-`alias`. Das ist **Eigenbau** (Vorverarbeitung im Text), kein Schalter des Dienstes.
- Ob `<say-as interpret-as="characters">,</say-as>` ein Satzzeichen benennt: `unbelegt`; wäre per Hörprobe zu prüfen.

### 3.4 Prüfung der Testtext-Phänomene gegen die Doku

| Phänomen (Testtext A/B) | Was die Doku zusagt | Status |
|---|---|---|
| Datum `21.10.2026` | `say-as date` mit `format="dmy"` dokumentiert (englisches Beispiel); Verhalten ohne Markup bei deutschem Text nicht dokumentiert | Hörprobe nötig |
| Uhrzeit `18:47 Uhr` | `say-as time` `hms24` dokumentiert | Hörprobe nötig |
| Dezimalkomma + Einheit `30,5 ms`, `12,5 km`, `14 °C` | `say-as unit` nur mit englischem Beispiel; Dezimalkomma-Verhalten nicht dokumentiert | unbelegt |
| `2,3 %` | nicht dokumentiert | unbelegt |
| Währung `1.249,99 €` | `say-as currency` (Beispiel „99.9 USD"); Tausenderpunkt/Komma nicht dokumentiert | unbelegt |
| Ordinal `3. Nov.` | `say-as ordinal` (Beispiel „3rd"); deutscher Punkt-Ordinal und Monatsabkürzung nicht dokumentiert | unbelegt |
| Abkürzungen `ca.`, `z. B.`, `Dr.` | Lexikon (`alias`) oder `sub` zur sicheren Steuerung; Standardverhalten nicht dokumentiert; `enhancePronunciation` bei HD kann helfen | Eigenbau zuverlässig |
| Akronyme `HRV`, `USB`, `WLAN` | `say-as characters`/`alphanumeric spell` (alle Locales); „Weh-Lan" statt Buchstabieren nur über `sub`/Lexikon-Alias | belegt für Buchstabieren |
| Fremdwörter `Update`, `Meeting`, `gecancelt` | Multilingual- und HD-Stimmen erkennen die Sprache des Texts automatisch; `<lang xml:lang="en-US">` setzt Akzent; Standardstimmen (Katja, Conrad) lesen deutsch | Hörprobe nötig |
| Gedankenstrich, `?`, `!`, Apostroph | automatische Pausen und Intonation laut Strukturseite | belegt (allgemein) |

Eine eigene deutsche Textnormalisierungs-Doku (Zahlen, Datum, Währung) habe ich bei Microsoft **nicht gefunden**; die Suche über die Speech-Service-Seiten ergab nur die englischen Beispiele (`unbelegt`).

### 3.5 Lexikon (Custom Lexicon)

`offiziell` (Pronunciation-Seite): PLS-1.0-Datei `.xml`/`.pls`, max. **100 KB**, eine Datei gilt nur für **eine Locale**, `alias` hat Vorrang vor `phoneme`, Phoneme ohne Leerzeichen (IPA, dekomponierte Unicode-Zeichen), Cache **15 min** je URI, Pflichtangabe einer **öffentlich erreichbaren URI** (Blob-Storage empfohlen, SAS möglich). Für eine Android-App ohne eigenen Server unpraktisch; ersatzweise `sub`/`phoneme` im SSML pro Wort.

---

## 4. Streaming, Latenz, Formate, SDK, REST

### 4.1 Streaming und Latenz

- Streaming-Ausgabe: SDK über `PullAudioOutputStream`, `PushAudioOutputStream`, `Synthesizing`-Event, `AudioDataStream`; Wiedergabe ab dem ersten Chunk. „First byte latency is independent from text length, while finish latency increases with text length." `offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-lower-speech-synthesis-latency (Doku-Stand 05.06.2026).
- Empfehlungen: `SpeechSynthesizer` wiederverwenden, Verbindung vorab öffnen (`Connection.openConnection(true)`), komprimierte Formate, neueste SDK-Version.
- Belegte Latenzangabe: Vergleichstabelle der HD-Seite — Azure-Speech-HD „**Less than 300 ms**", Azure-OpenAI-HD „Greater than 500 ms", Standard-Stimmen „Less than 300 ms" (`offiziell`, ohne Messmethode, Region oder Perzentil; Herstellerangabe, nicht unabhängig geprüft). HD-Stimmen: „Real-time only", „Cloud only".
- MAI-Voice-2.1-Flash: „Ultra-fast low-latency", **keine Zahl**; MAI-Voice-2.1: „prioritizes naturalness and expressivity over latency-critical scenarios". Eine Drittmessung (P50 ≈ 1,03 s über OpenRouter) ist für MAI-Voice-2 von einem Suchtreffer genannt, nicht selbst geprüft (`extern`, unbelegt).
- **Texteingabe als Stream** (für LLM-Ausgabe): nur über den WebSocket-V2-Endpunkt `wss://{region}.tts.speech.microsoft.com/cognitiveservices/websocket/v2`, `SpeechSynthesisRequestInputType.TextStream`. „Text streaming is supported in C#, C++ and Python with Speech SDK." Das Feature „doesn't support SSML" (Stimme und Format als Globaleigenschaften); OpenAI-Stimmen werden nicht unterstützt. Für **Java/Android gibt es keinen Textstream** (Doku verweist nur auf C#- und Python-Beispiele). Ob HD- oder MAI-Stimmen im Textstream laufen, ist nicht belegt.

### 4.2 Ausgabeformate (REST/SDK)

`offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/rest-text-to-speech (Doku-Stand 05.06.2026). Streamingfähig u. a.: `audio-24khz-48kbitrate-mono-mp3`, `audio-24khz-96kbitrate-mono-mp3`, `audio-24khz-160kbitrate-mono-mp3`, `audio-48khz-96kbitrate-mono-mp3`, `audio-48khz-192kbitrate-mono-mp3`, `audio-24khz-16bit-24kbps-mono-opus`, `ogg-24khz-16bit-mono-opus`, `ogg-48khz-16bit-mono-opus`, `webm-24khz-16bit-mono-opus`, `raw-24khz-16bit-mono-pcm`, `raw-48khz-16bit-mono-pcm`, `raw-16khz-16bit-mono-truesilk`; nicht streamend: `riff-*-pcm`. „Each standard voice model is available at 24 kHz and high-fidelity 48 kHz." HD-Stimmen: Abtastraten 8/16/24/48 kHz, Formate opus, mp3, pcm, truesilk.

### 4.3 REST

- Endpunkt: `POST https://{region}.tts.speech.microsoft.com/cognitiveservices/v1`, `Content-Type: application/ssml+xml`, Header `X-Microsoft-OutputFormat`, `User-Agent` (Pflicht, < 255 Zeichen), Auth entweder `Ocp-Apim-Subscription-Key` oder `Authorization: Bearer` mit Token von `…/sts/v1.0/issueToken` (10 Minuten gültig, Empfehlung: 9 Minuten wiederverwenden). Entra-Auth möglich.
- Audio je Anfrage max. 10 min (längeres wird abgeschnitten). Die Doku rät: REST nur nutzen, wenn das SDK nicht geht.
- Fehlercodes: 400, 401, 415, 429, 502, 503.
- Der Key gehört wegen Geheimhaltung nie in die APK (Bestandswissen); Token-Broker oder kurzlebiger Bearer-Token vom eigenen Server.

### 4.4 Android-SDK

- Maven Central `com.microsoft.cognitiveservices.speech:client-sdk`, neueste Version **1.52.0 (28.09.2026)**; Vorversionen 1.51.2 (20.08.2026), 1.51.1 (25.07.2026), 1.51.0 (21.07.2026), 1.50.0 (12.05.2026). `extern` https://repo1.maven.org/maven2/com/microsoft/cognitiveservices/speech/client-sdk/ (Verzeichnislisting).
- Größe 1.52.0: **AAR 12.940.359 Bytes (12,3 MiB)**, JAR 15.410.355 Bytes. Ob das AAR alle ABIs enthält, wurde nicht geprüft; die Größe ist die der Maven-Datei, nicht der geteilten APK.
- 16-KB-Seitengröße: Im Mai 2025 war die Speech-SDK-Java-Bibliothek nicht 16-KB-ausgerichtet (`extern` https://learn.microsoft.com/en-us/answers/questions/2276346/); ein Nutzer meldete 1.44.0 als behoben (nicht amtlich bestätigt). Für 1.52.0 nicht selbst geprüft: Vor dem Play-Upload `zipalign -c -P 16` ausführen.
- Plattformtabelle der SDK-Seite: Java läuft auf Android, Windows, Linux, macOS.
- Edge-Alternative: ein eigener WebSocket-Client braucht kein SDK (siehe Bestandswissen).

---

## 5. Datenschutz, Lizenz, SLA

### 5.1 Datenschutz

`offiziell` https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/text-to-speech/data-privacy-security (Doku-Stand 26.08.2026):

- Echtzeit-TTS mit vorgefertigten Stimmen: „Neither input text nor output audio content is stored in Microsoft logs." — „Microsoft doesn't retain or store the text that you provide with the real-time synthesis text to speech API" und speichert erzeugtes Audio bei der Echtzeit-API nicht.
- Batch-Synthese: Eingabetext und Ausgabe liegen in Azure-Storage, löschbar per Delete-API.
- Regionsverarbeitung: „Azure Speech stores and processes speech data in the region where you create your resource" (`offiziell` Regionsseite). Ausnahme **Voice Live** (HD-Synthese und LLM-Inferenz mit eigenen Verarbeitungsbereichen, u. U. außerhalb der Region).
- Eigene Kundendaten für Custom Voice werden nicht zum Training von Microsoft-Modellen genutzt (Aussage zu Custom Neural Voice).

EU-Regionen laut Regionstabelle (`offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions, Doku-Stand 18.10.2026):

| Region | Neural | HD-Stimmen | MAI-Stimmen | Anmerkung |
|---|---|---|---|---|
| westeurope | ja | ja | ja | alles |
| francecentral | ja | ja | ja | alles |
| swedencentral | ja | ja | ja | alles |
| northeurope | ja | **nein** | **nein** (MAI-Seite behauptet „Available") | Widerspruch in der Doku |
| germanywestcentral | ja | **nein** | **nein** | nur Standard-Neural |
| switzerlandnorth | ja | nein | nein | nur Standard-Neural |

Vorläufige Empfehlung für DSGVO plus HD: `westeurope`, `francecentral` oder `swedencentral`. Die Regionstabelle trägt die Spalte „Voices and styles in preview"; für westeurope ist sie angehakt.

### 5.2 Lizenz kommerzieller Nutzung

Siehe 2.6: Ausgabe-Nutzungsrechte nur im Paid Tier; F0 nur zur Evaluierung. Die SDK-Lizenz („Microsoft software license terms for the Speech SDK", https://aka.ms/csspeech/license) habe ich nicht gelesen (`unbelegt`).

### 5.3 SLA

- MAI-Voice (2/2.1/Flash): „This preview is provided without a service-level agreement, and is not recommended for production workloads." (`offiziell`, MAI-Seite).
- Für GA-Stimmen (Neural, DragonHD) habe ich **keine** TTS-spezifische SLA-Zahl gefunden. Die Microsoft-Seite „Service Level Agreements for Online Services" nennt nur „Service Level Agreement for Microsoft Online Services (WW), October 2026" ohne Text im Abruf; Speech/Foundry Tools ist dort nicht ablesbar. Offene Frage: Ob F0 und Preview-Stimmen davon ausgeschlossen sind, ist **unbelegt**.

---

## 6. Edge-TTS (inoffiziell), Stand Oktober 2026

### 6.1 Versionslage

| Fakt | Quelle |
|---|---|
| PyPI-Version **7.3.1** (veröffentlicht 13.10.2026 15:57 UTC), `requires_python >=3.7` | https://pypi.org/pypi/edge-tts/json (Abruf 21.10.2026) |
| 7.3.0 (13.10.2026): Fortschrittsbalken auf stderr, **Sec-MS-GEC-v2-Token**, macOS-CoreAudio-Workaround für mpv | https://api.github.com/repos/rany2/edge-tts/releases |
| 7.3.1 (13.10.2026): Fix für locale-abhängiges Parsen des Server-Datums (PR #494) | ebd. |
| 7.2.8 (22.03.2026): CBR-Byte-Offset, SSL-Kontext zwischengespeichert; 7.2.4/7.2.6 (11.12.2025): NoAudioReceived behoben, Endpunkt wie Edge auf macOS | ebd. |
| Letzter Commit 14.10.2026 04:16 UTC: `fix: propagate errors from stream_sync instead of hanging` | https://api.github.com/repos/rany2/edge-tts/commits |
| Konstanten im Master: `SEC_MS_GEC_VERSION = "2-143.0.3650.75"`, Host weiter `speech.platform.bing.com/consumer/speech/synthesize/readaloud`, TrustedClientToken unverändert `6A5AA1D4EAFF4E9FB37E23D68491D6F4` | https://raw.githubusercontent.com/rany2/edge-tts/master/src/edge_tts/constants.py |

### 6.2 Die 403-Welle vom 10.–13.10.2026

Chronologie aus Issue #490 (48 Kommentare, geschlossen 13.10.2026 15:34 UTC), `extern` https://github.com/rany2/edge-tts/issues/490 (Kommentare als Nutzerberichte, nicht amtlich):

- **10.10.2026**: Erste Meldung (Windows 11, 7.2.8): `WSServerHandshakeError: 403`; Stimmenliste liefert noch 200; Edge-Vorlesefunktion funktioniert auf denselben Rechnern. Weitere Meldungen aus Frankreich, Belgien, Mitteleuropa; ein VPN umgeht den Fehler. Drittseiten, die auf edge-tts bauen, fallen ebenfalls aus.
- Muster: Frankreich fällt komplett aus, UK zeitweise, Nordamerika zunächst nicht (Nutzerbeobachtung → gestaffelter Rollout).
- **11.10.2026**: Spanien, Pakistan, Bulgarien; ein Nutzer zeigt: Wechsel des WebSocket-Hosts auf `api.msedgeservices.com` bei **altem** Token und alter Version `1-143.0.3650.75` stellt den Betrieb in Spanien, Frankreich, Belgien und Südafrika wieder her; „Using the new v2 version string with the legacy token still returns 403".
- **12.10.2026 ab ca. 02:50 UTC**: Meldung, dass auch ein Rechenzentrum in Los Angeles (USA) betroffen ist; Versionssprünge von 141 bis 150 und Proxys helfen nicht.
- **13.10.2026**: PR #492 „feat: add Sec-MS-GEC v2 token support" (Autor HarryXiaoCN) gemerged von rany2; Release 7.3.0 und 7.3.1 am selben Tag. PR #491 (neuer Host `api.msedgeservices.com`) ist geschlossen; der Master nutzt weiter `speech.platform.bing.com` (Merge-Status von #491 nicht geprüft).
- Ursache laut PR #492: Microsoft habe Token v2 im Oktober 2026 **verpflichtend** gemacht; der Browser nutzt dieselbe Domain `speech.platform.bing.com`, der v1-Token von edge-tts bekam 403. Der Host `api.msedgeservices.com` funktioniere ebenfalls. Der PR-Autor behauptet, es liege am Tokenformat, nicht an Netz, Region oder Client-Fingerabdruck; das ist seine Auswertung (Paketmitschnitt), die ich nicht nachvollzogen habe.
- **16.10.2026**: Issue #507 „SEC_MS_GEC_VERSION uses prefix '1-' in v7.3.1 …" — Maintainer rany2: „This is simply not true. There is something wrong with your Python environment." Der Melder zog die Meldung am 21.10.2026 zurück. 7.3.1 ist also nach Stand der Issues funktionsfähig.
- Danach nur kleine Fehler (Textsplitter über Bytelimit #499/#506, `stream_sync` hängt #500/#501, Kantonesisch #502). Keine weitere 403-Welle seit dem 13.10.2026 in den Issues sichtbar (Issues #508/#509 vom 16./19.10.2026 ohne 403-Bezug). **Eigener Funktionstest der Synthese: nicht durchgeführt.**

### 6.3 Algorithmus Sec-MS-GEC v2 (für eigene Implementierungen, `extern` drm.py)

- Eingabe wie v1: Windows-FileTime-Ticks (100 ns), auf 5-Minuten-Grenze abgerundet (`ticks -= ticks % 300` in Sekunden), dann `f"{ticks:.0f}{TRUSTED_CLIENT_TOKEN}"` ASCII.
- Ausgabe v2: `<CS64 16 Hex>-<SHA256-artig 64 Hex>`, beides **Großbuchstaben**; der Hash ist **kein Standard-SHA-256** (eigener Startzustand, abgewandelte Konstanten), CS64 ist ein zweiter Digest über die ersten 8 Byte. Der Code in `drm.py` ist die Referenz; Nachbau nur über Portierung (Hinweis: Hash-Funktionen sind in der Datei beschrieben, ich habe sie nicht überprüft).
- Header `Sec-MS-GEC-Version: 2-143.0.3650.75`. Uhrversatz wird über den `Date`-Header der 403-Antwort ausgeglichen (`adj_clock_skew_seconds`).
- Der Tokenwechsel von v1 auf v2 trifft **jeden eigenen Edge-Client**, also auch Jarvis, falls dieser einen eigenen WebSocket-Client nutzt und nicht die Bibliothek.

### 6.4 Rechtliche Lage, Grenzen, Zuverlässigkeit

| Punkt | Befund |
|---|---|
| ToS | **Keine Microsoft-Nutzungsbedingung für den Read-Aloud-Endpunkt gefunden.** Die Aussage im Bestandswissen §13 stützt sich auf einen Q&A-Thread, der nur Azure F0 behandelt (siehe 0, Zeile 3). Der Endpunkt ist nicht dokumentiert, nicht für Dritte freigegeben, daher Grauzone. |
| Durchsetzung | Microsoft sperrt aktiv technisch (Chromium-Version, Token, Origin): msedge-tts-README: seit Dezember 2025 müsse der User-Agent einem Edge-Browser entsprechen (`extern` https://cdn.jsdelivr.net/npm/msedge-tts@2.0.5/README.md, laut Suchtreffer). Abmahnungen oder Store-Entfernungen: nicht gefunden. |
| SSML | edge-tts README: „Support for custom SSML was removed" — Microsoft erlaubt nur ein `<voice>` mit einem `<prosody>`; nutzbar sind `rate`, `volume`, `pitch`. Keine `say-as`, `phoneme`, `lexicon`, `sub`, `break`, `express-as` (`extern` https://github.com/rany2/edge-tts). |
| Stimmenumfang | 6 deutsche Stimmen für de-DE (Abschnitt 1.4); keine HD-/MAI-Stimmen. |
| Zuverlässigkeit | Mindestens zwei Ausfallwellen: Sec-MS-GEC-Einführung (Nov. 2024 bis Jan. 2026, Bestandswissen) und 10.–13.10.2026 (Token v2). Jede Welle brach alle Clients, bis die Bibliothek nachzog (Zeitraum 2026: ca. 3 Tage). Gestaffelter regionaler Rollout. Für eine produktive App kein verlässlicher Weg. |
| Qualität = Azure Neural? | Gleiche Stimmennamen (Katja, Conrad, Amala, Killian, Seraphina/Florian-Multilingual) wie bei Azure Neural/Multilingual; **kein Vergleich zwischen Edge-Ausgabe und Azure-API-Ausgabe gefunden** (`unbelegt`). Das Format ist 24 kHz / 48 kbit/s MP3 (SuggestedCodec in der Stimmenliste); Azure bietet bis 48 kHz / 192 kbit/s. Qualitätsunterschiede durch niedrige Bitrate wahrscheinlich, nicht belegt. |

---

## 7. Unabhängige Qualitätsbelege

### 7.1 Artificial Analysis Speech Arena (Provider Voice Arena)

`extern` https://artificialanalysis.ai/text-to-speech/leaderboard (Abruf 21.10.2026; die Seite nennt kein Stichtagsdatum, nur „© 2026"). Der Arena-Test nutzt laut Methodik **je 8 Anbieterstimmen (US/UK, männlich/weiblich)**, also englische Stimmen; Rater sind laut Methodik für die Nicht-Englisch-Boards Muttersprachler (https://artificialanalysis.ai/methodology/text-to-speech).

| Rang (von 98) | Modell | Elo | Preis je 1 Mio Zeichen | Stimmen | Veröffentlicht |
|---|---|---|---|---|---|
| 1 | Eleven v4 Turbo | 1335 | 40 $ | | |
| 4 | Sonic 3.6 | 1289 | 49 $ | | |
| 6 | Realtime TTS-2 | 1267 | 20,8 $ | | |
| **30** | **Azure HD 2.5** | **1142** | 22 $ | 8 | März 2026 |
| 60 | Chirp 3: HD (Google) | 1061 | 30 $ | | |
| **74** | **Azure Neural** | **1034** | 15 $ | 8 | Sept. 2018 |
| **76** | **MAI-Voice-1** | **1030** | n. a. | 4 | April 2026 |
| 92 | Polly Neural | 897 | 16 $ | | |

- Lesart: Azure HD liegt im Mittelfeld (Platz 30 von 98), Azure Neural im unteren Drittel (Platz 74); MAI-Voice-1 liegt gleichauf mit Azure Neural. **MAI-Voice-2/2.1 und DragonHD Omni sind in diesem Abruf nicht aufgeführt** (`unbelegt`, ob „Azure HD 2.5" mit DragonHD identisch ist).
- Die Boards sind **englisch**; die Seite bietet keinen Sprachfilter (nur Akzent US/UK und Kategorie).

### 7.2 Mehrsprachige Boards (Deutsch)

- Artificial Analysis hat nach Suchtreffer „Multilingual Text to Speech Arena Leaderboards" für 9 Sprachen eingeführt, darunter Deutsch (X-Beitrag https://x.com/ArtificialAnlys/status/2102490340678856997, per WebFetch 402, daher nur über Suchsnippet). Die Methodik bestätigt: Das **Controlled Voice Arena** (geklonte Referenzstimmen) bewertet auch Deutsch, je Sprache ein eigenes Board, sobald genug Vergleiche vorliegen.
- **Azure Neural, Azure HD oder MAI erscheinen in den abgerufenen Controlled-Voice-Zeilen nicht** (45 sichtbare Zeilen, kein deutsches Board erreichbar). Eine belegte deutsche Elo-Zahl für Azure habe ich deshalb **nicht**. Im Bestandswissen (§15) ist die deutsche Spitze „Cartesia Sonic 3.6 ≈ 1283, Inworld TTS-2 ≈ 1261" genannt; das Suchergebnis dieser Recherche nennt für Sonic 3.6 auf einer deutschsprachigen Seite „qualityElo 1281.86" (Suchsnippet, `extern`).

### 7.3 Sonstige Vergleiche

- techstackups.com „best-tts-2026-blind-benchmark" nennt 16 Modelle inklusive Azure, **ohne** erkennbare deutsche Ergebnisse im Snippet (`extern`, nicht gelesen).
- ElevenLabs-Blog-Vergleiche (Herstellerangaben): MOS 4,14 / WER 2,83 % für ElevenLabs, Azure Neural HD „trails on emotional expressiveness" — Hersteller, kein Deutsch, nicht verwertbar.
- Eine unabhängige deutsche Hörstudie zu Seraphina/DragonHD gefunden: **nein**. Aussage zur deutschen Qualität von Azure bleibt auf Hörproben (`proben/`) angewiesen.
- Vendor-Eigenangaben MAI-Voice-2 (https://microsoft.ai/news/mai-voice-2/, Ankündigung 02.06.2026): in einem Test über 11 Sprachen (2.222 Antworten) bevorzugten Hörer MAI-Voice-2 zu 45,5 %, menschliche Aufnahmen zu 44 %, 10,5 % unentschieden; MAI-Voice-2 gegenüber MAI-Voice-1 72 % (2.500 Tests). Das sind **Herstellermessungen**, 15 Sprachen/18 Locales genannt (Deutsch dabei); die MAI-Doku nennt inzwischen 23 Sprachen.

---

## 8. Einordnung für Jarvis (nur aus den belegten Fakten abgeleitet)

1. Der bestehende Edge-Anbieter benötigt Sec-MS-GEC **v2**. Falls Jarvis einen eigenen WebSocket-Client nutzt: Token und Header-Version umstellen (Algorithmus in `drm.py`), sonst regional 403 seit 10.10.2026.
2. Azure Speech über S0/Pay-as-you-go ist rechtssauber, EU-Region `westeurope` / `francecentral` / `swedencentral` bietet DragonHD (Seraphina, Florian) und MAI-Preview. Für Privatnutzung reicht F0 (0,5 Mio Zeichen Neural/Monat); HD im F0-Kontingent ist **nicht belegt**.
3. Normalisierung deutscher Zahlen, Einheiten, Abkürzungen ist bei Azure **nicht zugesichert**; Vorverarbeitung im eigenen Code (oder `sub`/Lexikon) bleibt nötig. Satzzeichen wörtlich: nur über `sub`-Ersatz.
4. Android: Java-SDK 12,9 MB AAR, **kein** Textstream in Java; für LLM-Streaming satzweise REST/WebSocket aufrufen und ersten Chunk sofort abspielen.
5. Die Qualitätsfrage Deutsch ist ohne Hörprobe nicht entscheidbar; unabhängige deutsche Elo-Werte für Azure fehlen.

---

## 9. Offene Punkte (nicht belegt)

- Zeichenpreis für MAI-Voice-2/2.1 und HD Flash (amtlich); ob F0 HD/MAI abdeckt.
- Product Terms im Wortlaut (Ausgaberechte, Caching); SLA-Text für Speech.
- Welche MAI-Namen (`MAI-Voice-2` oder `MAI-Voice-2.1`) die API in den EU-Regionen akzeptiert; Widerspruch northeurope in Regions- und MAI-Seite.
- Deutsche Textnormalisierung (Datum, Währung, Einheiten, Ordinal) ohne Markup: nur Hörprobe.
- Funktioniert Edge-Synthese mit edge-tts 7.3.1 heute? Nicht selbst getestet.
- Ist Edge-Ausgabe qualitativ gleich der Azure-API-Ausgabe derselben Stimme? Kein Beleg.
- 16-KB-Ausrichtung des Speech-SDK 1.52.0.
- Deutsches Arena-Board mit Azure-Modellen.

---

## Quellenverzeichnis (alle abgerufen 21.10.2026)

- Azure Speech Preise: https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/ und Retail Prices API https://prices.azure.com/api/retail/prices
- Azure Free Account: https://azure.microsoft.com/en-us/free/
- Sprachunterstützung: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support?tabs=tts
- HD-Stimmen: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/high-definition-voices
- MAI-Voice: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/mai-voices; https://microsoft.ai/news/mai-voice-2/
- Quotas und Limits: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-services-quotas-and-limits
- TTS-Übersicht (abrechenbare Zeichen): https://learn.microsoft.com/en-us/azure/ai-services/speech-service/text-to-speech
- SSML: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-synthesis-markup-structure, …/speech-synthesis-markup-pronunciation, …/speech-synthesis-markup-voice
- Latenz: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-lower-speech-synthesis-latency
- REST: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/rest-text-to-speech
- SDK: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/speech-sdk; Maven https://repo1.maven.org/maven2/com/microsoft/cognitiveservices/speech/client-sdk/1.52.0/
- Regionen: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/regions
- Datenschutz: https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/text-to-speech/data-privacy-security
- Budgets: https://learn.microsoft.com/en-us/azure/cost-management-billing/costs/tutorial-acm-create-budgets
- Q&A kommerzielle Nutzung F0: https://learn.microsoft.com/en-us/answers/questions/5805156/
- Q&A 16 KB: https://learn.microsoft.com/en-us/answers/questions/2276346/speech-sdk-for-java-is-incompatible-with-android-1
- edge-tts: https://github.com/rany2/edge-tts, https://pypi.org/pypi/edge-tts/json, Issues #490, #491, #492, #507 (GitHub-API)
- Edge-Stimmenliste: https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list
- Artificial Analysis: https://artificialanalysis.ai/text-to-speech/leaderboard, https://artificialanalysis.ai/methodology/text-to-speech

---

BEST-PRACTICES-KANDIDATEN:
1. **Edge-Client: Sec-MS-GEC v2 und Versions-Konstante `2-<Chromium-Version>`** — Token = `<CS64 16 Hex>-<hashed 64 Hex>` über `ticks+TrustedClientToken`, 5-Minuten-Grenze, Uhrversatz aus `Date`-Header der 403-Antwort ausgleichen; Version zentral als Konstante führen. (Quelle: PR #492, `drm.py`, edge-tts 7.3.1, 13.10.2026)
2. **Azure-Preise: Neural 15 $, Neural HD 22 $ je 1 Mio Zeichen** (nicht 16 $); Preise per Retail Prices API abrufen, weil die Preisseite die Beträge per JavaScript lädt. SSML-Markup (außer `<speak>`/`<voice>`) zählt als abrechenbare Zeichen.
3. **Azure-Regionen für deutsche HD-/MAI-Stimmen in der EU:** nur `westeurope`, `francecentral`, `swedencentral`; `germanywestcentral` und `switzerlandnorth` liefern nur Standard-Neural.
4. **HD-SSML-Matrix:** DragonHD = kein `prosody`, kein `express-as`, aber `phoneme`, `break`, `say-as`, `sub`; Omni = `express-as` ja, `phoneme`/`break` nein; Textstream nur C#/C++/Python ohne SSML. Für Deutsch Normalisierung und Abkürzungen im eigenen Code oder per `sub`/Alias vorverarbeiten.
5. **Wörtliche Satzzeichen:** Es gibt keinen dokumentierten Schalter (`say-as detail` hat keine Standardwerte); Ersatz über `<sub alias="Komma">,</sub>` oder Vorverarbeitung.
6. **Android Speech SDK:** `client-sdk` 1.52.0, AAR 12,9 MB; vor Play-Upload `zipalign -c -P 16` prüfen; Key nie in der APK, Token-Broker mit 9-Minuten-Bearer.
7. **Free-Account-Fallen:** Karte nötig (nicht prepaid), 200 $ nur 30 Tage, ohne Wechsel auf Pay-as-you-go wird das Konto deaktiviert; Budgets warnen nur und stoppen nichts; F0 = 20 Anfragen/60 s und kein Anspruch auf kommerzielle Ausgaberechte.

BUG-KANDIDATEN:
1. **Edge 403 „Invalid response status" ab 10.10.2026** (gestaffelter Rollout, Frankreich/Spanien/Belgien zuerst, USA ab 12.10.): Ursache Token v1 wird abgelehnt; Fix edge-tts 7.3.0/7.3.1 oder v2-Token selbst bauen; Notlösung der Community: Host `api.msedgeservices.com` (nicht in die Bibliothek übernommen). Stimmenlisten-Abruf läuft währenddessen weiter (irreführend).
2. **Almanach-Korrektur W10:** Die Aussage „Bing prüft `Sec-MS-GEC-Version` nicht streng" gilt nicht mehr (seit Okt. 2026 Format und Version-Präfix `2-` nötig).
3. **Bestandswissen §13 korrigieren:** Der zitierte Q&A-Thread 5805156 betrifft Azure F0, nicht edge-tts; „Microsoft sagt" zur Edge-Nutzung ist dort nicht belegt. Für edge-tts liegt keine amtliche ToS-Aussage vor.
4. **Azure-Dokumentation widerspricht sich:** MAI-Namen (`MAI-Voice-2` vs. `MAI-Voice-2.1`) und Regionen (northeurope) unterscheiden sich zwischen Sprachunterstützung, Regionsseite und MAI-Seite; Fußnote 3 (Phoneme/Lexikon nicht unterstützt) widerspricht der HD-Tabelle (DragonHD: Phoneme ja).
5. **Azure 429 ist meist Kapazität, nicht Kontingent:** Anhebung der Quote hilft nicht; Retry mit Backoff und Heimatregion der Stimme nutzen.
6. **edge-tts Randfehler Oktober 2026:** `stream_sync()` hängt endlos bei Verbindungsfehler (Issue #500, Fix im Master 14.10.2026, noch kein Release); Textsplitter überschreitet Bytelimit bei Text ohne Leerzeichen (Issues #499/#506 offen).
