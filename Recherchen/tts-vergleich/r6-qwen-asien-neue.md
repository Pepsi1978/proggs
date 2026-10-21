# R6 — Alibaba Qwen-TTS und asiatische/neue TTS-Anbieter (Deutsch-Fokus)

Recherchezeitpunkt und Abrufdatum aller Quellen: **21.10.2026**. Engine C (Sonnet-Schwarm), Researcher R6. Nur WebSearch/WebFetch. Testtext: `Recherchen/tts-vergleich/testtext.md`. Abgrenzung zum Bestand: `best-practices/apis/tts-provider.md` (dort nur Edge, Google Chirp 3 HD, Android-TTS; Alibaba, MiniMax, Fish usw. fehlen dort).

Kennzeichnung: **[offiziell]** = Herstellerdokumentation, **[extern]** = Dritte/Aggregatoren/Presse, **[unbelegt]** = nicht in einer abrufbaren Quelle gefunden. Die Seitenabrufe liefen über ein Kurzmodell, das Seiten zusammenfasst. Wörtliche Zitate stammen aus dessen Wiedergabe, nicht aus einem Rohabzug. Wo Quellen sich widersprechen, steht der Widerspruch im Text.

---

## 0. Kurzfazit

1. **Alibaba hat die TTS-Landschaft seit Franks Integration stark umgebaut.** Neben `qwen3-tts-flash` (HTTP, 600 Zeichen, 10 Sprachen inkl. Deutsch, Systemstimmen) gibt es jetzt die Familie **Qwen-Audio-TTS** (`qwen-audio-3.1-tts-plus`, `qwen-audio-3.1-tts-flash`, `qwen-audio-3.0-tts-plus/-flash`) mit Anweisungssteuerung, SSML-Kennzeichnung, Klonen und Stimmendesign. Quellen: Preisseite, Modellvergleichsseite (Abschnitt 1).
2. **Deutsch:** `qwen3-tts-flash` und alle 48 Stimmen der Stimmenliste (Cherry, Chelsie, Serena, Jada, Sunny, Ethan, Dylan eingeschlossen) listen Deutsch ausdrücklich, `language_type` kennt `German`. Für die neue Qwen-Audio-Familie ist die Deutschlage uneinheitlich dokumentiert (Systemstimmen: Chinesisch/Englisch, vier Mehrsprachstimmen mit Deutsch, Klonstimmen mit Deutsch). Siehe 1.4.
3. **Freikontingent:** Nur Region **Singapur** (Bereitstellungsumfang „International“), **einmalig 90 Tage** ab Aktivierung. `qwen3-tts-flash` 110.000 Zeichen, andere TTS-Modelle 10.000 Zeichen, `qwen-audio-3.1-tts-flash` 1 Mio Token, Stimmen-Anlegen 1.000 Stimmen. Nicht dauerhaft. Schalter „Free Quota Only“ ist standardmäßig aus.
4. **Preise (Singapur, je 10.000 Zeichen):** `qwen3-tts-flash` 0,10 USD (= 10 USD/Mio), `qwen3-tts-flash-realtime` 0,13 USD, `qwen-audio-3.0-tts-flash` 0,15 USD, `qwen-audio-3.0-tts-plus` 0,20 USD, `qwen-audio-3.1-tts-plus` 0,30 USD. Chinesische Zeichen zählen doppelt, SSML-Tags nicht.
5. **Wichtig für Jarvis:** Die Domäne `dashscope.aliyuncs.com` „will no longer support new features after September 30, 2026“ (Alibaba-Regionsseite, Abschnitt 5). Die Singapur-Domäne `dashscope-intl.aliyuncs.com` steht weiterhin in der Tabelle, aber die Doku empfiehlt die Migration auf eine workspace-eigene Domäne (`{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com`). Das gehört auf die Prüfliste.
6. **Unabhängige Qualitätsbelege (nur Englisch-Board belegbar):** Artificial Analysis „Provider Voice“-Rangliste, abgerufen 21.10.2026: Platz 3 Qwen-Audio-3.1-TTS-Plus (Elo 1301, 19,30 USD/Mio), Platz 7 Qwen-Audio-3.0-TTS-Plus (1265), Platz 9 Fish Audio Drama 3 Preview (1248), Platz 24 MiniMax Speech 2.8 HD (1174), Platz 26 Speech 2.8 Turbo (1155), Platz 37 Fish S2 Pro (1112). **Ein belegtes Deutsch-Ranking für Alibaba, MiniMax oder Fish habe ich nicht gefunden** (Abschnitt 6).
7. **MiniMax:** Deutsch in `language_boost`, `speech-2.8-hd` 100 USD/Mio, `speech-2.8-turbo` 60 USD/Mio, 60 Anfragen/Minute. Zu teuer und schwächer platziert als Alibaba und Fish. Datenschutz laut Drittquellen schwach (US-Speicherung, keine Null-Aufbewahrung).
8. **Fish Audio S2.1 Pro:** 83 Sprachen, Deutsch aufgeführt, 15 USD pro Mio **UTF-8-Bytes** (Umlaute zählen zwei Bytes). Kostenlose API-Variante `s2.1-pro-free` ohne SLA, mit Datenverwendung zur Modellverbesserung.
9. **Kyutai Pocket TTS** (Deutsch, 100M, CC-BY-4.0, CPU) und **Chatterbox Multilingual** (Resemble, MIT, Deutsch) sind Open-Weights-Kandidaten zum Selbsthosten. Sesame: nur Englisch belegt. ByteDance Seed-TTS: international und Deutsch nur über Aggregatoren, nicht offiziell belegt.

---

## 1. Alibaba Model Studio (Qwen-TTS und Qwen-Audio-TTS)

### 1.1 Exakte aktuelle Modellnamen

| Familie | Modell-ID | Protokoll | Regionen | Stand/Quelle |
|---|---|---|---|---|
| Qwen3-TTS (Systemstimmen) | `qwen3-tts-flash` | HTTP (SSE-Streaming möglich) | Singapur, Peking | Modellseite und Modellvergleich [offiziell], 21.10.2026 |
| Qwen3-TTS Echtzeit | `qwen3-tts-flash-realtime` | WebSocket | Singapur, Peking | Preisseite, Ratenlimit-Seite [offiziell] |
| Qwen-Audio-TTS 3.1 | `qwen-audio-3.1-tts-plus`, `qwen-audio-3.1-tts-flash` | WebSocket / HTTP | Singapur, Peking | Preisseite, Klon-Anleitung [offiziell] |
| Qwen-Audio-TTS 3.0 | `qwen-audio-3.0-tts-plus`, `qwen-audio-3.0-tts-flash` | WebSocket | Singapur, Peking | Preisseite, Modellvergleich [offiziell] |
| Klonen (Altmodell, Franks Weg) | `qwen-voice-enrollment` + `qwen3-tts-vc-2026-01-22` | – | – | **Siehe 1.6** |
| Qwen3-TTS-Instruct | `qwen3-tts-instruct-flash` (und `-realtime`) | – | – | **Siehe 1.6** |
| CosyVoice | `cosyvoice-v3.5-plus/-flash`, `cosyvoice-v3-plus/-flash`, `cosyvoice-v2` | WebSocket | Peking (v3.5 nur dort), Singapur (v3) | Preisseite [offiziell] |

Quellen:
- Preisseite: https://www.alibabacloud.com/help/en/model-studio/model-pricing (Abschnitt Sprachsynthese, Abruf 21.10.2026)
- Modellvergleich: https://www.alibabacloud.com/help/en/model-studio/tts-model (Abruf 21.10.2026)
- Modellseite: https://www.alibabacloud.com/help/en/model-studio/qwen3-tts-flash (Abruf 21.10.2026)
- Regel laut Modellvergleich: „models with the `-realtime` suffix use WebSocket, while those without it use HTTP.“

Alibaba empfiehlt in der Modellübersicht für „Text-to-speech“ `qwen-audio-3.0-tts-plus` und `MiniMax/speech-2.8-hd` (Alibaba-Marktplatz führt MiniMax als Drittmodell), Quelle https://help.aliyun.com/en/model-studio/model-qwen-tts (Abruf 21.10.2026). Im Modellvergleich gilt `qwen-audio-3.1-tts-plus` als empfohlenes Modell für Standard-TTS.

Einführung der 3.1-Familie: 23.09.2026 laut all-ai.de [extern] https://www.all-ai.de/news/news26/text-to-speech-alibaba-qwen-audio (Abruf 21.10.2026). Qwen-Audio-3.0-TTS-Plus: Juli 2026, Artikel vom 21.07.2026 [extern] https://the-decoder.com/alibabas-qwen-audio-3-0-tts-plus-tops-the-competition-in-the-text-to-speech-rankings/ (Abruf 21.10.2026).

### 1.2 Zahlen zu `qwen3-tts-flash` (HTTP)

| Eigenschaft | Wert | Quelle |
|---|---|---|
| Max. Eingabe | „The maximum input length is 600 characters.“ | API-Referenz https://www.alibabacloud.com/help/en/model-studio/qwen-tts-api, 21.10.2026 [offiziell] |
| `language_type` | Standard `Auto`; zulässig u. a. `German`; „For single-language text, set the language explicitly“ (verbessert Qualität laut Doku) | dieselbe Seite [offiziell] |
| Werte `language_type` | Auto, Chinese, English, German, Italian, Portuguese, Spanish, Japanese, Korean, French, Russian | QwenCloud-Spiegel https://docs.qwencloud.com/api-reference/speech-synthesis/qwen-tts, 21.10.2026 [offiziell/Spiegel] |
| Streaming | `stream=True` (Python-SDK ab 1.24.5), Java `streamCall` (ab 2.19.0), curl-Header `X-DashScope-SSE: enable`; Zwischenstücke tragen Base64-Audio in `data`, das letzte Stück die `url` | API-Referenz, 21.10.2026 [offiziell] |
| Audio | 24.000 Hz, 16 Bit PCM, mono; Ergebnis-URL „valid for 24 hours“ | API-Referenz [offiziell] |
| Endpunkt International (Altform) | `https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation` | API-Referenz [offiziell] |
| Rate | 180 Anfragen/Minute (Singapur und Peking), kein Gleichzeitigkeitslimit genannt | Ratenlimit-Seite https://www.alibabacloud.com/help/en/model-studio/rate-limit, 21.10.2026 [offiziell] |
| Abrechnungsfeld | `usage.characters`, „Returned only by Qwen3-TTS-Flash“ | QwenCloud-Spiegel [offiziell/Spiegel] |
| Stimmenzahl | Modellseite: „featuring 17 expressive voices“; Stimmenliste: 48 Stimmen | Modellseite bzw. Stimmenliste [offiziell, **widersprüchlich**] |
| Tonlage | „automatically adjusts vocal tones based on text semantics“ | Modellseite [offiziell] |
| Anweisungen | Für `qwen3-tts-flash` auf der Modellseite nicht dokumentiert. Modellvergleich: Anweisungssteuerung „No“ | Modellvergleich, 21.10.2026 [offiziell] |
| SSML | Für `qwen3-tts-flash` nicht dokumentiert (Vergleichstabelle: „Not stated“) | Modellvergleich [offiziell] |

Belegter Antwortbeginn (unabhängig):
- Coval-Messung zu `qwen3-tts-flash-realtime`, letzte 30 Tage, Testrechner in den USA: Zeit bis zum ersten Audio im Mittel **720 ms** (p50 729, p95 787, max 1.174), Rang 30 von 32. Die Fehlerrate im Mittel 3,1 % (nicht sprachspezifisch). Coval vermerkt: „Alibaba serves the multilingual proprietary endpoint through Model Studio from an Asia-region deployment.“ https://benchmarks.coval.ai/models/qwen3-tts-flash-realtime, 21.10.2026 [extern].
- Hersteller-Zahl (nicht gleiches Produkt): Open-Source-Qwen3-TTS-Technikbericht „first-packet latency as low as 97 ms (0.6B) and 101 ms (1.7B)“, https://arxiv.org/html/2601.15621v1, 21.10.2026 [Hersteller, Open-Weights-Modell, nicht die gehostete API].
- Qwen-Audio-3.x-Flash: „about 300 milliseconds of latency“ laut Presse (The Decoder, 21.07.2026) [extern, Hersteller-Angabe weitergegeben].
- Für `qwen3-tts-flash` (HTTP, nicht Echtzeit) und für die Qwen-Audio-Familie: **kein unabhängig gemessener Antwortbeginn gefunden [unbelegt]**.

### 1.3 Stimmen für Deutsch (Franks Stimmen)

Offizielle Stimmenliste https://www.alibabacloud.com/help/en/model-studio/qwen-tts-voice-list (21.10.2026):
- 48 Stimmen. „Standard voices (38)“: Cherry, Serena, Ethan, Chelsie, Momo, Vivian, Moon, Maia, Kai, Nofish, Bella, Jennifer, Ryan, Katerina, Aiden, Eldric Sage, Mia, Mochi, Bellona, Vincent, Bunny, Neil, Elias, Arthur, Nini, Seren, Pip, Stella, Bodega, Sonrisa, Alek, Dolce, Sohee, Ono Anna, Lenn, Emilien, Andre, Radio Gol.
- Jede dieser Stimmen führt „Chinese (Mandarin), English, French, German, Russian, Italian, Spanish, Portuguese, Japanese, Korean“.
- 10 Dialektstimmen (u. a. Jada = Shanghai, Dylan = Peking, Sunny = Sichuan, Marcus, Li, Roy, Peter, Eric, Rocky, Kiki) führen zusätzlich Englisch, Französisch, Deutsch usw. Alle von Franks sieben Stimmen sind darin enthalten.
- Die Liste gilt für `qwen3-tts-flash` (Systemstimmen) und `qwen3-tts-flash-realtime`; `qwen3-tts-instruct-flash` wird dort nicht genannt.
- **Keine Stimme ist als deutsch-muttersprachlich ausgewiesen.** Die Stimmen sind chinesisch/englisch ausgerichtet und sprechen Deutsch als Zielsprache mit; ob mit Akzent, steht nirgends in der Doku [unbelegt]. Akzentprüfung nur per Hörprobe.

### 1.4 Deutsch bei Qwen-Audio-TTS (neue Familie) — widersprüchliche Doku

| Aussage | Quelle |
|---|---|
| Systemstimmen von `qwen-audio-3.1-tts-flash`: Chinesisch (Mandarin) und Englisch | Suchtreffer-Zusammenfassung der Doku, 21.10.2026 [extern über Suche] |
| Vier mehrsprachige Stimmen (`longanhuan_v3.1`, `longanlingxin_v3.1`, `longanfengyue_v3.1`, `xunanchuan_v3.1`): „Japanese, Korean, French, German, Portuguese, Italian, Vietnamese, and Indonesian“ | Stimmenliste https://www.alibabacloud.com/help/en/model-studio/qwen-audio-tts-voice-list, 21.10.2026 [offiziell, über Kurzmodell] |
| Modellvergleich: bei `qwen-audio-3.x` Deutsch „Yes (cloned voices only)“ | https://www.alibabacloud.com/help/en/model-studio/tts-model, 21.10.2026 [offiziell] |
| Klon-Anleitung: Qwen-Audio-TTS/-Realtime unterstützen „English, Japanese, Korean, Russian, French, German“ | https://www.alibabacloud.com/help/en/model-studio/qwen-tts-voice-cloning, 21.10.2026 [offiziell] |
| Presse: „16 Sprachen und 20 chinesische Dialektregionen“, Deutsch „sehr gut“ | all-ai.de 23.09.2026 [extern]; arXiv-Abstract 2607.23938 bestätigt 16 Sprachen, nennt sie nicht |

Fazit: Deutsch bei den neuen Modellen ist über **Klonstimmen** und die vier Mehrsprachstimmen belegt, nicht über zwei reine Systemstimmen. Für einen „Standardstimmen-Test“ bleibt `qwen3-tts-flash` die dokumentierte Deutsch-Option mit Systemstimmen.

### 1.5 Preise und Zählweise

Quelle für alle Zeilen: Preisseite https://www.alibabacloud.com/help/en/model-studio/model-pricing, Abruf 21.10.2026 [offiziell]. „Original prices, excluding limited-time promotions“ laut Modellseite.

| Modell | Singapur je 10.000 Zeichen | Singapur je 1 Mio Zeichen | Peking je 10.000 Zeichen |
|---|---|---|---|
| `qwen3-tts-flash` (Ausgabe frei) | 0,10 USD | 10 USD | 0,114682 USD |
| `qwen3-tts-flash-realtime` (Ausgabe frei) | 0,13 USD | 13 USD | 0,143353 USD |
| `qwen-audio-3.0-tts-flash` | 0,15 USD | 15 USD | 0,137521 USD |
| `qwen-audio-3.0-tts-plus` | 0,20 USD | 20 USD | 0,19253 USD |
| `qwen-audio-3.1-tts-plus` | 0,30 USD | 30 USD | 0,20 USD |
| `qwen-audio-3.1-tts-flash` | **tokenbasiert**: 0,23 USD Eingabe / 1,87 USD Ausgabe je 1 Mio Token | – | 0,212 / 1,696 USD je 1 Mio Token |
| `cosyvoice-v3-flash` / `-plus` | 0,13 / 0,26 USD | 13 / 26 USD | 0,14335 / 0,286706 USD |

- **Je Minute:** Die Doku nennt keine Minutenpreise. Eigenrechnung (Annahme: ca. 1.000 deutsche Zeichen pro Sprechminute, nicht aus einer Quelle): `qwen3-tts-flash` ca. 0,01 USD/Minute, `qwen-audio-3.1-tts-plus` ca. 0,03 USD/Minute.
- **Zählweise:** „Each Chinese character (including simplified Chinese characters, traditional Chinese characters...) counts as 2 characters. Each other character (such as an English letter, a digit, a punctuation mark...) counts as 1 character.“ SSML-Tags zählen nicht. Für Deutsch (lateinische Buchstaben, Umlaute eingeschlossen) gilt also **1 Zeichen = 1**, anders als bei Google (Bytes) und Fish (UTF-8-Bytes). Quelle wie oben.
- **Klonen:** `qwen-voice-enrollment`: 0,01 USD je neu angelegte Stimme; Singapur 1.000 Stimmen kostenlos (siehe 1.7). Die Klon-Anleitung sagt dagegen „Voice creation is free“ (für Qwen-Audio/CosyVoice) und begrenzt auf 1.000 Stimmen je Konto, geteilt zwischen Klonen und Design. Quellen: Preisseite bzw. https://www.alibabacloud.com/help/en/model-studio/qwen-tts-voice-cloning, 21.10.2026 [offiziell, **uneinheitlich: kostenpflichtig vs. frei je nach Modellfamilie**].
- **Synthese mit geklonten Stimmen:** „billed according to the target model“ (Klon-Anleitung). Für `qwen3-tts-vc-2026-01-22` hat die abgerufene Preisseite **keine eigene Zeile** [unbelegt]; Drittquellen nennen 0,115 USD/10.000 Zeichen, nicht nachprüfbar.
- **Instruct-Variante:** Eine Suchzusammenfassung nennt 0,115 USD je 10.000 Zeichen für `qwen3-tts-instruct-flash` in Singapur und Peking und „25 timbres“ mit Chinesisch/Englisch-Anweisungen; die abgerufene Doku-Seite lieferte aber nur die Modellübersicht. Wert daher als **[extern/unvollständig]** werten. https://www.alibabacloud.com/help/en/model-studio/qwen3-tts-instruct-flash

### 1.6 Status von `qwen3-tts-vc-…`, `-vd-…`, `-instruct-flash`

- Die Modellvergleichsseite führt „instruct-flash“, „vc“ und „vd“ **nicht** als Modell-IDs; sie behandelt Klonen und Design als Methoden der Qwen-Audio-/CosyVoice-Modelle (Abruf 21.10.2026).
- Die Seiten `.../qwen3-tts-vc` und `.../qwen3-tts-instruct-flash` lieferten beim Abruf nur die Modellübersicht (Weiterleitung). Ob die Modelle ausgelaufen oder nur umgezogen sind, ist **[unbelegt]**.
- **Folge für Jarvis:** Die eigene geklonte Stimme (`qwen3-tts-vc-2026-01-22`) hängt an einem Modellnamen, den die aktuelle Doku nicht mehr bewirbt. Klonstimmen sind laut Doku an ihr `target_model` gebunden („bound to their `target_model` at creation“). Ein Wechsel auf Qwen-Audio erfordert also neue Stimmen. Vor einem Umstieg prüfen, ob der alte Name noch läuft und bis wann.

### 1.7 Freikontingent im Wortlaut

Quellen: Preisseite (21.10.2026) und https://www.alibabacloud.com/help/en/model-studio/new-free-quota.md (21.10.2026) [offiziell].

- „The following models offer a free quota only in Singapore. No free quota is available in other regions.“
- „Valid for 90 days from the date of Model Studio activation, model release, or application approval, whichever is later“ — **einmalig, nicht dauerhaft**, nicht erneuerbar.
- „Only models in the Singapore region with the service deployment scope set to International are eligible for a free quota.“
- Mengen Singapur: `qwen3-tts-flash` **110.000 Zeichen**; `qwen3-tts-flash-realtime` 10.000 Zeichen (Datumsbedingung „nach 13.11.2025“); `qwen-audio-3.1-tts-plus`, `3.0-plus`, `3.0-flash`, `cosyvoice-v3-*` je 10.000 Zeichen; `qwen-audio-3.1-tts-flash` 1.000.000 Token; Klonen 1.000 Stimmen. Eine Drittseite nannte für `qwen3-tts-flash` 10.000 Zeichen (WaveSpeed), die Alibaba-Preisseite nennt 110.000; Alibaba hat Vorrang.
- Konto: „You must complete your account information before activating Model Studio.“ Danach: „After the free quota is exhausted, you are automatically billed on a pay-as-you-go basis.“ Ein Abrechnungskonto ist also Voraussetzung; eine Zahlungsmethode wird in der Doku nicht ausdrücklich genannt [unbelegt].
- **Kostenbremse „Free Quota Only“:** Standard „disabled“; bei Aktivierung „the service stops responding and returns the error code `AllocationQuota.FreeTierOnly`“. Ein Suchtreffer meldet: neue, nicht verifizierte Nutzer könnten nach Verbrauch des Freikontingents nicht weiterarbeiten, verifizierte würden abgerechnet [extern/Suche].
- **Weitere Bremse:** Ratenlimits begrenzen nicht die Summe: „Rate limiting only constrains the call rate per unit time and does not limit cumulative Usage.“ Empfohlen: „enable monthly consumption limits and Configure threshold notifications“ im Bereich Bills and fees. https://www.alibabacloud.com/help/en/model-studio/rate-limit, 21.10.2026 [offiziell].
- **Region-Widerspruch:** Ein Suchtreffer zur chinesischen Doku-Domäne (help.aliyun.com) berichtet, dort gelte das Klon-Freikontingent „only in China (Beijing)“. Die internationale Seite (alibabacloud.com) sagt Singapur. Für ein Konto in Singapur zählt die internationale Seite [Widerspruch dokumentiert, nicht aufgelöst].
- Kommerzielle Nutzung der Ausgabe: In den abgerufenen Seiten **nicht ausdrücklich geregelt** [unbelegt]. Die Nutzungsbedingungen (International Website Product Terms) wurden nicht gelesen.

### 1.8 Aussprachekontrolle und Deutsch-Normalisierung (Testtext)

| Testphänomen | Befund | Quelle |
|---|---|---|
| SSML | Qwen-Audio 3.1 (plus/flash): Stimmenliste markiert „SSML: Supported“; 3.0: nur chinesische Stimmen; Zählregel „SSML tags not counted“. Für `qwen3-tts-flash`: nicht dokumentiert | Stimmenliste, Preisseite, 21.10.2026 [offiziell] |
| Anweisungen (Stil, Tempo, Emotion) | Qwen-Audio: Parameter `instruction`; CosyVoice-`instruction` max. 100 Zeichen (Chinesisch = 2) | Sprachsynthese-Übersicht https://www.alibabacloud.com/help/en/model-studio/qwen-tts, 21.10.2026 [offiziell] |
| Emotionstags im Text | Nur die vier Qwen-Audio-TTS-Modelle (laut Übersichtsseite) | dieselbe Seite [offiziell] |
| Zahlen, Datum, Uhrzeit, Euro, Einheiten, Abkürzungen auf Deutsch | **Keine Doku zur deutschen Normalisierung.** Die Sprachsynthese-Übersicht erwähnt nur `language_hints` zur Korrektur falsch gelesener Zahlen/Abkürzungen für „supported models“; wie sich das auf Qwen3-TTS auswirkt, ist unbelegt | https://www.alibabacloud.com/help/en/model-studio/qwen-tts, 21.10.2026 [unbelegt für `qwen3-tts-flash`] |
| Fremdwörter im deutschen Satz | Doku: „Supports multilingual mixed input“, `Auto` für gemischte Sprachen | API-Referenz [offiziell] |
| Akronyme (HRV, WLAN, USB) | nicht dokumentiert [unbelegt] | – |
| Gedankenstrich, Frage-/Ausrufintonation | nicht dokumentiert [unbelegt]; Doku: Modell passt Ton am Satzsinn an | Modellseite [offiziell] |

Folge: Alle Testtext-Phänomene aus der Tabelle in `testtext.md` für Alibaba **per Hörprobe prüfen**. Als Rückfall gilt Franks eigene deutsche Textaufbereitung vor dem Senden (siehe `tts-provider.md` §6 und tts-lokal-android §16).

### 1.9 Datenschutz, Datenstandort, Zuverlässigkeit

- **Standort:** „Regions determine the access point and data storage location.“ „Request data is stored in that region.“ „Your static data always remains in the selected region.“ Der **Bereitstellungsumfang** („Service deployment scope“) „Determines the inference execution location.“ Quelle: https://www.alibabacloud.com/help/en/model-studio/regions.md, 21.10.2026 [offiziell].
- **Singapur:** Die abgerufenen Regionsseiten definieren den Umfang „International“ nicht. Suchzusammenfassungen lesen ihn als „global nodes excluding the Chinese mainland“ — **[extern, nicht in der Primärquelle bestätigt]**. Coval nennt den Endpunkt „Asia-region deployment“.
- **Frankfurt (eu-central-1) existiert** mit Umfang „Global and EU“, aber ohne DashScope-Domäne („Not supported“); Preisseite listet dort nur Video-Modelle, **keine TTS-Modelle [unbelegt für TTS]**. https://help.aliyun.com/en/model-studio/regions/ , 21.10.2026.
- **Training:** „Alibaba Cloud strictly protects data privacy and never uses your data for model training.“ FAQ https://www.alibabacloud.com/help/en/model-studio/faq-about-alibaba-cloud-model-studio, 21.10.2026 [offiziell]. Aufbewahrungsdauer der Aufrufdaten: **nicht genannt**; die FAQ verweist auf die International Website Product Terms. Eine Suchzusammenfassung zitiert „will store data generated from model and application calls“ [extern/Suche].
- **DSGVO:** Die FAQ erwähnt die DSGVO nicht. Ein Auftragsverarbeitungsvertrag wurde nicht gefunden [unbelegt]. Für Franks privaten Einsatz (eigene Texte, Singapur) relevant: Texte verlassen die EU; keine EU-Sprachsynthese belegt.
- **Domänenwechsel:** „The DashScope domain (dashscope.aliyuncs.com) will no longer support new features after September 30, 2026.“ Singapur-Domäne `dashscope-intl.aliyuncs.com` ist weiter gelistet; empfohlen wird die workspace-eigene Domäne `{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com` („higher throughput, lower latency, and workspace-level traffic isolation“ laut Suchzusammenfassung). Schlüssel sind regionsgebunden, der Schlüssel muss zur Domäne passen. Quelle: Regionsseite, 21.10.2026 [offiziell/extern]. Die Modell-Doku zeigt in Beispielen für Qwen-Audio schon `https://{workspace_id}.cn-beijing.maas.aliyuncs.com/api/v1`.
- **Ausgabe-Lizenz:** nicht geklärt (siehe 1.7).
- **Zuverlässigkeit:** Coval-Fehlerrate (Wörter) im Mittel 3,1 %, p95 20,0 %, Rang 31 von 32 (nicht sprachspezifisch; Echtzeit-Endpunkt). SLA nicht gefunden [unbelegt].

---

## 2. MiniMax Speech

- **Modelle (offiziell):** `speech-2.8-hd`, `speech-2.8-turbo`, ältere `speech-2.6-*`, `speech-02-*`, `speech-01-*`. https://platform.minimax.io/docs/api-reference/speech-t2a-http, 21.10.2026. Keine neuere Version als 2.8 auf der Preisseite https://platform.minimax.io/docs/guides/pricing-paygo (21.10.2026).
- **Deutsch:** `language_boost` führt **German**; Async-Anleitung nennt 40 Sprachen [offiziell/Suchzusammenfassung]. Herstellerqualität für Deutsch: nur allgemeine Aussage, keine Zahlen gefunden [unbelegt]. Technikbericht Qwen3-TTS (Selbstmessung von Alibaba): MiniMax Deutsch WER 1,906 / Sprecherähnlichkeit 0,733; ElevenLabs 0,572 / 0,614; Qwen3-TTS-12Hz-1.7B 1,235 / 0,775 [Hersteller-Konkurrent, extern] https://arxiv.org/html/2601.15621v1.
- **Preise (offiziell, Abruf 21.10.2026):** `speech-2.8-hd` 100 USD je Mio Zeichen (= 1,00 USD je 10.000 Zeichen); `speech-2.8-turbo` 60 USD je Mio. Stimmendesign 3 USD je Stimme, schnelles Klonen 1,50 USD je Stimme, jeweils bei erster Nutzung abgerechnet. Zählweise (ob Chinesisch doppelt zählt): nicht gefunden [unbelegt].
- **Freikontingent:** Preisseite nennt keine Gratisguthaben („The page doesn't mention free credits“). Drittquellen widersprechen sich (Probeguthaben je nach Verifizierung, „no free tier“, abgelaufene 14-Tage-Aktion bis 06.09.2026). **Kein belegtes dauerhaftes Freikontingent.** [offiziell: nicht erwähnt; extern: widersprüchlich]
- **Limits:** Text höchstens 10.000 Zeichen je Anfrage, ab 3.000 Zeichen Streaming empfohlen; **60 Anfragen/Minute** für T2A und Klonen. https://platform.minimax.io/docs/guides/rate-limits, 21.10.2026 [offiziell].
- **Aussprache/Steuerung (offiziell):** Pausen `<#x#>` (0,01 bis 99,99 s); Interjektionen `(laughs)`, `(sighs)`, `(breath)` u. a. nur bei 2.8; `text_normalization_mode` = `basic` (Regeln) oder `quality` (nur nicht-streamend, 2.8 und 2.6); Tempo 0,5 bis 2, Tonhöhe -12 bis 12; Emotionen happy/sad/angry/…; Formate mp3, pcm, flac, wav, opus; Streaming nur `hex`; Untertitel auf Satz-/Wortebene. Ob `quality` deutsche Zahlen, Daten und Abkürzungen korrekt ausschreibt: **[unbelegt]**.
- **Antwortbeginn:** keine belegte Messung gefunden [unbelegt].
- **Datenschutz (extern):** Drittseiten berichten: Betreiber Nanonoble Pte. Ltd. (Singapur), Speicherung in einem US-Rechenzentrum, Ort der Berechnung nicht offengelegt, keine Null-Aufbewahrung, kein Auftragsverarbeitungsvertrag gefunden, Nutzung der Inhalte zur Verbesserung laut API-Bedingungen. https://standardcompute.com/providers/minimax , https://minimax-ai.chat/privacy-security/minimax-privacy-policy/ (21.10.2026) [extern, nicht in der Primärquelle geprüft].
- **Rang (unabhängig):** Artificial Analysis Provider-Voice-Liste: Speech 2.8 HD Platz 24 (Elo 1174, 100 USD/Mio), Speech 2.8 Turbo Platz 26 (1155, 60 USD/Mio). https://artificialanalysis.ai/text-to-speech/leaderboard (Abruf 21.10.2026). Englisch-Board; kein Deutsch.

---

## 3. Fish Audio

- **Modelle:** `s2.1-pro` (empfohlenes Produktionsmodell, 83 Sprachen), `s2.1-pro-free` (0 USD, ohne Laufzeitgarantien), `s2-pro` (Vorgänger, 80+ Sprachen, Open Source), `s1` (veraltet, endet am 31.12.2026, danach `s2.1-pro` zum gleichen Preis). Zusätzlich in der Rangliste: „Drama 3 Preview“ (Platz 9) und „S2.1 Pro (Oct 2026)“ (Platz 20). Quellen: https://docs.fish.audio/developer-guide/models-pricing/models-overview und .../pricing-and-rate-limits (21.10.2026) [offiziell].
- **Deutsch:** Blog zur kostenlosen API listet 83 Sprachen und nennt Deutsch ausdrücklich („English, Japanese, Chinese, Korean, Spanish, Arabic, French, German“; „The same model handles all languages“). https://fish.audio/blog/s2-1-pro-free-api/ (21.10.2026) [offiziell]. Qualität laut Hersteller für Deutsch: keine eigene Zahl [unbelegt].
- **Preis (offiziell):** `s2.1-pro` und `s2-pro` **15 USD je 1 Mio UTF-8-Bytes**; „TTS pricing is based on the size of input text, measured in millions of UTF-8 bytes.“ Das ist keine Zeichenzählung: deutsche Umlaute und ß belegen 2 Bytes, die tatsächliche Zeichenkosten liegen etwas über 15 USD/Mio (Eigenrechnung: bei 3 bis 5 % Umlauten rund 15,5 bis 15,8 USD). Keine Abo- oder Mindestgebühr für die API. Die Rangliste führt 15 USD/Mio. https://docs.fish.audio/developer-guide/models-pricing/pricing-and-rate-limits (21.10.2026).
- **Limits:** gleichzeitige Anfragen je Stufe: Starter (unter 100 USD eingezahlt) 5, Elevated (ab 100 USD) 15, High Volume (ab 1.000 USD) 50. Bei Überschreitung HTTP 429 ohne `Retry-After`, Empfehlung exponentielles Backoff. Stufen schalten durch Vorauszahlung frei. Max. Textlänge je Anfrage: nicht genannt [unbelegt].
- **Freikontingent:** Die Preisseite beschreibt „kein Gratisprogramm“, nur `s2.1-pro-free` zu 0 USD. Der Blog nennt widersprüchliche Enddaten („through July 24“, „through November 30, 2026“, später „through August 31, 2026“); **aktuelles Ende nicht sicher [Widerspruch]**. Bedingungen: „High-volume access with no hard character cap (subject to Fair Use Policy)“, „No uptime/TTFA guarantees“, „Requests may be used to improve model quality“, „Products generating more than $1M ARR should contact us before using S2.1 Pro Free“, „Some commercial scenarios may have restrictions“. Bezahlpläne: „full licensing, SLA, and no data retention“.
- **App-Plan (nicht API):** Free 8.000 Credits/Monat, max. 500 Zeichen je Generierung; Preiskarte nennt „Commercial use“, FAQ sagt „Free plan users can only use generated content for personal, non-commercial projects“ (**Widerspruch auf derselben Seite**). Plus 11 USD/Monat (250.000 Credits, kommerziell erlaubt), Pro 75 USD/Monat (2 Mio Credits). https://fish.audio/plan/ (21.10.2026) [offiziell].
- **Aussprache/Steuerung:** freie Klammer-Hinweise `[whispers sweetly]`, `[laughing nervously]` (S2/S2.1); SSML und Zahlennormalisierung für Deutsch: nicht dokumentiert [unbelegt].
- **Antwortbeginn:** S2-Pro 100 ms, Blog nennt für S2.1 Pro „~70 ms“ und „~90 ms“ (Herstellerangabe, widersprüchlich); kein unabhängiger Messwert [unbelegt].
- **Datenschutz:** kostenlose Variante mit Datenverwendung zur Verbesserung; OpenRouter vermerkt: „`fish-audio/s2.1-pro` is not currently available through EU in-region routing.“ https://openrouter.ai/fish-audio/s2.1-pro (21.10.2026). Datenstandort der eigenen API: nicht gefunden [unbelegt].
- **Rang (unabhängig):** Platz 9 Drama 3 Preview (Elo 1248), Platz 20 S2.1 Pro (1189), Platz 37 S2 Pro (1112); Englisch-Board. https://artificialanalysis.ai/text-to-speech/leaderboard (21.10.2026).
- **Lizenzhinweis zum Selbsthosten:** In `best-practices/apis/tts-provider.md` §16 sind Fish/OpenAudio-S1-mini als nicht kommerziell (NC) geführt; für S2.1 Pro gilt: „not open weights“ laut Suchzusammenfassung [extern]. Lizenz von `s2-pro`-Gewichten: **[unbelegt]** (die Modellseite sagt nur „open-source“).

---

## 4. ByteDance Doubao / Seed-TTS, Resemble AI, Sesame, Kyutai

| Anbieter | Deutsch | Preis / Frei | Betrieb | Befund |
|---|---|---|---|---|
| **ByteDance Seed-TTS** (BytePlus „Seed Speech“) | nur über Aggregatoren: „German“ in Listen mit 10 bis 15+ Sprachen; **offiziell nicht belegt** | BytePlus-Seite: „Pricing details require contacting sales“, kein Gratis-Betrag. Aggregatoren: ca. 30 USD/Mio (unbelegt) | Eigener Schlüssel getrennt von ModelArk; EU-Endpunkt deckt laut TanStack-Adapterdoku nur Chat und Bild; TTS wohl nur Asien-Pazifik [extern] | https://www.byteplus.com/en/product/seedspeech (21.10.2026). **Für Frank praktisch nicht belegbar; nicht weiterverfolgen, außer BytePlus liefert offizielle Sprachliste und Preis.** |
| **Resemble AI** (Chatterbox) | Chatterbox Multilingual: Deutsch unter 23 Sprachen [extern, Modellkarte/Mintlify] | Hosting-Anbieter DeepInfra 1,00 USD/Mio Zeichen (Drittpreis); Resemble-eigene TTS-Preise **nicht gefunden**, Resemble-Preisseite zeigt nur Erkennung/Wasserzeichen (Flex 0 USD, Team 350 USD/Monat, Business 1.000 USD/Monat) | Modell MIT-Lizenz (selbst hostbar); „1.000 Zeichen je Anfrage“ bei einem Aggregator [extern] | Chatterbox Elo 1023,5 auf Artificial Analysis (Englisch). https://deepinfra.com/ResembleAI/chatterbox-multilingual , https://www.resemble.ai/pricing , https://artificialanalysis.ai/text-to-speech/models/chatterbox (21.10.2026). Kein Gratis-Kontingent bei Resemble belegt. |
| **Sesame** (CSM, Maya) | **Nur Englisch belegt** (CSM-1B Eingabe/Ausgabe Englisch, Apache 2.0) | keine API-Preise gefunden | Maya nur über Sesame-App | Für Deutsch ausgeschlossen. https://api.ifq.ai/sesame/csm-1b/uptime , https://tts.ai/voices/sesame-csm/ (21.10.2026) [extern]. |
| **Kyutai Pocket TTS** | **Deutsch: ja** (Sprachwahl English, French, German, Portuguese, Italian, Spanish; „In April, we made it speak five other languages“; Stimme `juergen (de)`) | gratis, selbst hosten; **CC-BY-4.0**, 100M Parameter | CPU: „~6x real-time on a CPU of MacBook Air M4“, „~200ms to get the first audio chunk“, auf 4-vCPU-Cloud-VM „RTF ~2.3-2.5x“; Klonen lokal; Zugriff nach Zustimmung auf Hugging Face; Verbot von Stimmennachahmung ohne Einwilligung | https://kyutai.org/tts/ , https://huggingface.co/kyutai/pocket-tts (21.10.2026) [offiziell]. Kein gehosteter Dienst. Deutsche Qualität: keine Messung [unbelegt]. |

---

## 5. Vergleichstabelle (Stand 21.10.2026)

Preise je Mio Zeichen; Abweichungen in der Zählweise stehen in Klammern.

| Dienst / Modell | Deutsch | Preis je Mio Zeichen | Freikontingent | Aussprache-Kontrolle | Streaming | Standort | Unabhängiger Rang (Englisch-Board) |
|---|---|---|---|---|---|---|---|
| Alibaba `qwen3-tts-flash` | ja, 10 Sprachen, `language_type: German` | 10 USD (SG) / 11,47 USD (Peking) | **einmalig** 110.000 Zeichen, 90 Tage, nur SG | keine Anweisungen/SSML dokumentiert | SSE, 24 kHz | SG; Inferenz „International“ laut Drittdeutung; kein EU | in Rangliste nicht gefunden |
| Alibaba `qwen3-tts-flash-realtime` | ja | 13 USD | 10.000 Zeichen einmalig | – | WebSocket, ca. 720 ms (Coval) | wie oben | Coval Rang 30/32 (Latenz) |
| Alibaba `qwen-audio-3.1-tts-plus` | Klon-/Mehrsprachstimmen ja | 30 USD (SG) / 20 USD (Peking) | 10.000 Zeichen einmalig | Anweisungen, SSML „Supported“, Tags | WebSocket/HTTP | SG/Peking | **Platz 3, Elo 1301** |
| Alibaba `qwen-audio-3.1-tts-flash` | Mehrsprachstimmen ja | Token: 0,23 / 1,87 USD je Mio Token | 1 Mio Token einmalig | Anweisungen, SSML, Tags | WebSocket/HTTP, ca. 300 ms (Hersteller) | SG/Peking | nicht in der abgerufenen Top 25 |
| Alibaba `qwen-audio-3.0-tts-plus` | Klonstimmen ja | 20 USD | 10.000 Zeichen einmalig | Anweisungen | WebSocket | SG/Peking | Platz 7, Elo 1265 |
| MiniMax `speech-2.8-hd` | ja (40 Sprachen) | 100 USD | keins belegt | Pausen `<#x#>`, Interjektionen, `text_normalization_mode` | HTTP-Stream, WebSocket | Drittquellen: US-Speicherung | Platz 24, Elo 1174 |
| MiniMax `speech-2.8-turbo` | ja | 60 USD | keins belegt | wie hd | wie hd | wie oben | Platz 26, Elo 1155 |
| Fish `s2.1-pro` | ja (83 Sprachen) | 15 USD je Mio **UTF-8-Bytes** | `s2.1-pro-free` ohne SLA, Daten zur Verbesserung | freie Klammertags | WebSocket | nicht gefunden; kein EU-Routing (OpenRouter) | Platz 20, Elo 1189 (Drama 3 Preview Platz 9, 1248) |
| Kyutai Pocket TTS | ja | selbst hosten | – | – | lokal, ca. 200 ms | lokal | – |
| Chatterbox Multilingual | ja | selbst hosten; Dritthost 1 USD | – | – | – | lokal / Dritthost | Elo 1023,5 |
| Sesame CSM | nein | – | – | – | – | – | – |
| ByteDance Seed-TTS | unbelegt | nur Vertrieb | unbelegt | unbelegt | unbelegt | Asien (extern) | nicht abgerufen |

Rangquelle für alle Elo-Werte: Artificial Analysis „Text to Speech Leaderboard“, Provider-Voice, abgerufen 21.10.2026 https://artificialanalysis.ai/text-to-speech/leaderboard . Die Seite zeigt kein Datum und keinen Sprachfilter; die Preisangabe der Seite für Qwen-Audio-3.1-TTS-Plus (19,30 USD/Mio) liegt näher am Pekinger Preis (20 USD) als am Singapurer (30 USD).

---

## 6. Unabhängige Qualitätsbelege speziell Deutsch

1. **Artificial Analysis, Mehrsprachige „Controlled Voice Arena“** (angekündigt für neun Sprachen, darunter Deutsch; muttersprachliche Stimmen, standardisierte geklonte Stimmen): Laut Suchzusammenfassung der Ankündigung führt **Cartesia Sonic 3.6** Deutsch, **Inworld Realtime TTS-2** liegt mit 1.261 Elo etwa 22 Punkte dahinter. Weder Qwen noch MiniMax noch Fish tauchen in den gefundenen Auszügen für Deutsch auf. Primärquelle (X-Beitrag) lieferte beim Abruf 402; die Controlled-Voice-Seite im Netz zeigte nur „English (US & UK)“. https://x.com/ArtificialAnlys/status/2102490340678856997 (Abruf 21.10.2026) — **[extern, nicht in der Primärquelle verifiziert]**. Das deckt sich mit `tts-provider.md` §15.
2. **Qwen-Selbstmessung (Technikbericht, Open-Weights-Modelle, nicht die gehostete API):** Deutsch, Inhaltstreue (WER, niedriger ist besser) 0,960 bis 1,501 für Qwen3-TTS-Varianten, **ElevenLabs 0,572**, MiniMax 1,906; Sprecherähnlichkeit Qwen 0,737 bis 0,775, ElevenLabs 0,614, MiniMax 0,733. Der Bericht schreibt, Qwen3-TTS „maintains highly competitive performance comparable to the state-of-the-art“ für Deutsch. https://arxiv.org/html/2601.15621v1 (21.10.2026) [Hersteller, eigene Messung, andere Modelle als die API].
3. **Coval:** Latenz und Wortfehlerrate für `qwen3-tts-flash-realtime` nicht nach Sprache getrennt (siehe 1.2).
4. **Presse zu Qwen-Audio-3.1-TTS:** „Top bei Chinesisch, sehr gut auf Deutsch“ — Überschrift der Quelle, ohne Zahlen; die zitierten Benchmarks betreffen Version 3.0. https://www.all-ai.de/news/news26/text-to-speech-alibaba-qwen-audio (23.09.2026) [extern, Meinung].
5. Ein VoxCPM2-Bericht nennt Deutsch-Werte 77,5 (Qwen3-TTS) gegen 61,4 (ElevenLabs) ohne Metrikbeschriftung im Auszug; **nicht verwertbar** [unbelegt].

**Fazit:** Für Deutsch fehlt ein unabhängiger, nach Sprache getrennter Qualitätsbeleg für Alibaba, MiniMax und Fish. Belegt ist nur: Qwen-Audio-3.1-TTS-Plus steht im allgemeinen (englischen) Arena-Ranking sehr hoch (Platz 3), und der deutsche Arena-Spitzenplatz gehört laut Quelle 1 Cartesia und Inworld.

---

## 7. Profile

### Alibaba Qwen (Franks bestehender Anbieter)
- **Stärken:** niedrigster Preis der Cloud-Kandidaten (10 USD/Mio bei `qwen3-tts-flash`); Deutsch ausdrücklich in `language_type` und bei allen 48 Stimmen gelistet; Zeichenzählung 1:1 für Deutsch; Singapur-Freikontingent; Qwen-Audio-3.1-Plus im Englisch-Ranking auf Platz 3.
- **Schwächen:** 600 Zeichen je Anfrage (Satzweise Zerlegung nötig); keine dokumentierte Zahlen-/Datumsnormalisierung auf Deutsch; Stimmen nicht muttersprachlich deutsch ausgewiesen; Doku inkonsistent (17 vs. 48 Stimmen; Klonkosten; Region des Freikontingents); Standort der Verarbeitung nicht geklärt; Domäne `dashscope*` wird abgekündigt.
- **Für Frank:** `qwen3-tts-flash` bleibt der billigste Weg; für höchste Qualität `qwen-audio-3.1-tts-plus` (30 USD/Mio in Singapur) testen. Bei Hörproben das Normalisierungsverhalten aller Testtext-Phänomene prüfen.

### MiniMax Speech 2.8
- **Stärken:** Deutsch in 40 Sprachen; reichhaltige Steuerung (Pausen, Interjektionen, Normalisierungsmodus); Formate und Untertitel.
- **Schwächen:** 60 bis 100 USD/Mio (das 6- bis 10-fache von Alibaba); Rang 24/26 im Englisch-Ranking; kein Freikontingent belegt; Datenschutz laut Drittquellen schwach; kein Auftragsverarbeitungsvertrag gefunden.
- **Für Frank:** nur wenn die Hörprobe überzeugt; Preis und Datenlage sprechen dagegen.

### Fish Audio S2.1 Pro
- **Stärken:** 83 Sprachen inkl. Deutsch; 15 USD/Mio Bytes; sehr schneller Antwortbeginn laut Hersteller; freie Klammer-Hinweise; Drama 3 Preview Platz 9 im Englisch-Ranking.
- **Schwächen:** Bytes-Abrechnung (Umlaute); Gratisvariante ohne SLA und mit Datenverwendung zur Verbesserung; widersprüchliche Doku zu Enddaten und kommerzieller Nutzung im Free-Plan; kein EU-Routing; Datenstandort offen.
- **Für Frank:** gute Option für Hörproben über die kostenlose API; für Dauerbetrieb Bezahlplan.

### Kyutai Pocket TTS / Chatterbox Multilingual (Open Weights)
- **Stärken:** gratis, offline/selbst gehostet, Deutsch vorhanden; keine Datenabgabe.
- **Schwächen:** Pocket TTS CC-BY-4.0 mit Nutzungsverbot ohne Einwilligung (Stimmenklonen); kein gehosteter Dienst; Chatterbox mäßig im Ranking (1023,5); deutsche Qualität nicht gemessen.
- **Für Frank:** als Offline-/Datenschutz-Option auf dem Handy (siehe `tts-provider.md` §16), nicht als Cloud-Ersatz.

### Seed-TTS und Sesame
- Nicht empfehlenswert für den Vergleich: Seed ohne offizielle Deutsch- und Preisbelege, Sesame nur Englisch.

---

## 8. Offene Punkte (nicht belegt, ehrlich)

1. Deutsche Normalisierung (Zahlen, Datum, Uhrzeit, Euro, Einheiten, Abkürzungen, Akronyme) bei Alibaba, MiniMax und Fish: keine Dokumentation, nur per Hörprobe klärbar.
2. Verbleib und Preis von `qwen3-tts-vc-2026-01-22`, `qwen3-tts-vd-…`, `qwen3-tts-instruct-flash`; Abkündigungsdatum.
3. Ausgaben-Lizenz und Aufbewahrungsdauer bei Alibaba (International Website Product Terms nicht gelesen).
4. Deutsch-Ranking der Artificial-Analysis-Arena für Qwen/MiniMax/Fish.
5. Fish: aktuelles Ende der kostenlosen API, Datenstandort, Lizenz der `s2-pro`-Gewichte.
6. MiniMax: Zählweise der Zeichen, Freikontingent, Antwortbeginn.
7. Seed-TTS: offizielle Sprachliste, Preis, internationale Verfügbarkeit.

---

## 9. Quellenverzeichnis (alle am 21.10.2026 abgerufen)

Alibaba (offiziell):
- Preise: https://www.alibabacloud.com/help/en/model-studio/model-pricing
- Modellvergleich Sprachsynthese: https://www.alibabacloud.com/help/en/model-studio/tts-model
- Sprachsynthese-Übersicht: https://www.alibabacloud.com/help/en/model-studio/qwen-tts
- Qwen-TTS API-Referenz: https://www.alibabacloud.com/help/en/model-studio/qwen-tts-api (Spiegel: https://docs.qwencloud.com/api-reference/speech-synthesis/qwen-tts)
- Modellseite `qwen3-tts-flash`: https://www.alibabacloud.com/help/en/model-studio/qwen3-tts-flash
- Modellseite `qwen-audio-3.1-tts-flash`: https://www.alibabacloud.com/help/en/model-studio/qwen-audio-3-1-tts-flash
- Stimmenliste Qwen-TTS: https://www.alibabacloud.com/help/en/model-studio/qwen-tts-voice-list
- Stimmenliste Qwen-Audio-TTS: https://www.alibabacloud.com/help/en/model-studio/qwen-audio-tts-voice-list
- Klonen: https://www.alibabacloud.com/help/en/model-studio/qwen-tts-voice-cloning
- Freikontingent: https://www.alibabacloud.com/help/en/model-studio/new-free-quota.md
- Ratenlimits: https://www.alibabacloud.com/help/en/model-studio/rate-limit
- Regionen und Domänen: https://www.alibabacloud.com/help/en/model-studio/regions.md , https://help.aliyun.com/en/model-studio/regions/
- FAQ: https://www.alibabacloud.com/help/en/model-studio/faq-about-alibaba-cloud-model-studio

MiniMax: https://platform.minimax.io/docs/api-reference/speech-t2a-http , https://platform.minimax.io/docs/guides/pricing-paygo , https://platform.minimax.io/docs/guides/rate-limits

Fish: https://docs.fish.audio/developer-guide/models-pricing/pricing-and-rate-limits , https://docs.fish.audio/developer-guide/models-pricing/models-overview , https://fish.audio/blog/s2-1-pro-free-api/ , https://fish.audio/plan/ , https://openrouter.ai/fish-audio/s2.1-pro

Kyutai/Resemble/Sesame/Seed: https://kyutai.org/tts/ , https://huggingface.co/kyutai/pocket-tts , https://www.resemble.ai/pricing , https://deepinfra.com/ResembleAI/chatterbox-multilingual , https://www.byteplus.com/en/product/seedspeech , https://api.ifq.ai/sesame/csm-1b/uptime

Unabhängig/extern: https://artificialanalysis.ai/text-to-speech/leaderboard , https://benchmarks.coval.ai/models/qwen3-tts-flash-realtime , https://arxiv.org/html/2601.15621v1 , https://arxiv.org/abs/2607.23938 , https://the-decoder.com/alibabas-qwen-audio-3-0-tts-plus-tops-the-competition-in-the-text-to-speech-rankings/ , https://www.all-ai.de/news/news26/text-to-speech-alibaba-qwen-audio

---

BEST-PRACTICES-KANDIDATEN:
1. **Alibaba Model Studio: Freikontingent ist einmalig (90 Tage), nur Singapur/International, `qwen3-tts-flash` 110.000 Zeichen.** „Free Quota Only“ je Modell einschalten (Standard aus); sonst läuft Abrechnung automatisch. Zusätzlich monatliches Verbrauchslimit und Alarm im Bereich Bills and fees setzen.
2. **Alibaba: `dashscope.aliyuncs.com` bekommt nach dem 30.09.2026 keine neuen Funktionen.** Auf workspace-eigene Domäne `{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com` migrieren; Schlüssel ist regions- und domänengebunden.
3. **Qwen3-TTS-Flash: höchstens 600 Zeichen je Anfrage, `language_type: German` setzen statt `Auto`, 180 Anfragen/Minute; deutsche Texte satzweise zerlegen.** Zeichenzählung 1:1 (Chinesisch = 2, SSML-Tags frei).
4. **Zählweisen unterscheiden:** Alibaba zählt Zeichen (Umlaut = 1), Google 5.000-Byte-Limit (Umlaut = 2), Fish rechnet je UTF-8-Byte ab (Umlaut = 2). Kosten- und Chunk-Rechnung pro Anbieter getrennt.
5. **Deutsche Textnormalisierung vor dem Senden selbst erledigen**, solange ein Anbieter sie nicht dokumentiert (Alibaba, MiniMax, Fish). Hörprobe mit `Recherchen/tts-vergleich/testtext.md` als Abnahme.
6. **Geklonte Stimmen sind an ihr `target_model` gebunden** (Alibaba): Modellwechsel = neue Stimme. Modellnamen mit Datumsstempel (`qwen3-tts-vc-2026-01-22`) regelmäßig gegen die aktuelle Modellseite prüfen.
7. **Gratis-Cloud-Stufen (Fish `s2.1-pro-free`) nur für Tests**: kein SLA, Datenverwendung zur Verbesserung, Enddatum wechselt.

BUG-KANDIDATEN:
1. **Alibaba-Doku widerspricht sich:** Modellseite „17 voices“ vs. Stimmenliste 48 Stimmen für `qwen3-tts-flash`; Klonen „kostenlos“ (Klon-Anleitung) vs. 0,01 USD je Stimme (Preisseite); Freikontingent „nur Singapur“ (alibabacloud.com) vs. „nur Peking“ (laut Suchtreffer help.aliyun.com). Bei Abweichungen immer Preisseite und Konsole zählen lassen.
2. **Alibaba-Doku-Weiterleitungen:** `.../qwen3-tts-vc` und `.../qwen3-tts-instruct-flash` zeigen beim Abruf nur die Modellübersicht. Mögliches Anzeichen für abgelöste Modelle; Modellname im Code kann still in einen Fehler laufen.
3. **`AllocationQuota.FreeTierOnly` (403)** bei aktivem „Free Quota Only“ nach Verbrauch; 429 = Ratenlimit, 403 = Kontingent erschöpft. Retry-Logik muss beide trennen (429 wiederholen, 403 nicht).
4. **Fish: HTTP 429 ohne `Retry-After`** bei Überschreiten der Gleichzeitigkeit (Starter 5): eigenes Backoff nötig.
5. **Fish: Abrechnung je UTF-8-Byte** führt bei deutschen Umlauten zu mehr als 15 USD je 1 Mio Zeichen; Kostenanzeige in Apps, die nach Zeichen rechnet, ist zu niedrig.
6. **Fish-Free-Plan (App): Preiskarte „Commercial use“ vs. FAQ „personal, non-commercial“** widersprechen sich; vor kommerzieller Nutzung die Nutzungsbedingungen lesen.
7. **MiniMax `fluent`/`whisper` nur für 2.6-Modelle**, Interjektionen nur für 2.8; `text_normalization_mode: quality` nur nicht-streamend. Falsche Kombination liefert Fehler oder wirkungslose Parameter.
