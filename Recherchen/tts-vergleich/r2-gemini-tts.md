# R2 — Gemini-TTS und neuere Google-Sprachausgabe

Recherchezeitpunkt und Abrufdatum aller Quellen: **21.10.2026** (Engine C, Sonnet-Schwarm, Researcher R2). Primärquellen wurden zusätzlich als Roh-HTML geladen und ausgewertet, damit keine Zusammenfassung dazwischenfunkt. Kennzeichnung: **belegt** = offizielle Seite mit Link; **sekundär** = Drittquelle; **nicht belegt** = nirgends gefunden. Nichts aus `C:\Users\barwa\SK` gelesen.

Vorab: Die Gemini-3.8-Modelle (Flash TTS und Flash-Lite TTS) sind seit September 2026 da und lösen 3.1 und 2.5 ab. Alle Zahlen zu ihnen sind entsprechend jung.

## 1. Kurzfazit (Kernkriterium Freikontingent)

| Frage | Antwort | Beleg |
|---|---|---|
| Hat Gemini-TTS in Google Cloud Text-to-Speech ein Freikontingent? | **Nein.** Spalte „Free usage limit“ steht bei allen Gemini-TTS-Zeilen auf „Not available“. | cloud.google.com/text-to-speech/pricing |
| Woher kommt dann Franks „großes Google-Freikontingent“? | Sehr wahrscheinlich von **Chirp 3: HD** (Franks heutige Stimme): „0 to 1 million characters“ frei, danach 30 $ je 1 Mio. Zeichen. Dazu WaveNet und Standard 4 Mio., Studio und Neural2 je 1 Mio. | cloud.google.com/text-to-speech/pricing |
| Gibt es sonst kostenlose Gemini-TTS-Nutzung? | **Ja, in der Gemini API (AI Studio, Free Tier)**: 3.8 Flash TTS, 3.8 Flash-Lite TTS, 3.1 Flash TTS Preview und 2.5 Flash Preview TTS stehen auf „Free of charge“ für Ein- und Ausgabe. Konkrete RPM/TPM/RPD-Zahlen sind **nicht öffentlich**. | ai.google.dev/gemini-api/docs/pricing |
| Haken beim Free Tier | Inhalte dienen der Produktverbesserung, Menschen dürfen mitlesen (außer EWR, CH, UK, siehe 8). Zugleich verlangen die Nutzungsbedingungen im EWR/CH/UK für Apps, die Nutzern zur Verfügung stehen, nur bezahlte Dienste. | ai.google.dev/gemini-api/terms |

## 2. Modelle, Namen, Status

### Gemini API / AI Studio (Entwickler-API)

| Modell-ID | Status laut Modellseite | Hinweis | Quelle |
|---|---|---|---|
| `gemini-3.8-flash-tts` | „New Stable“ (Wort „GA“ fällt nicht) | veröffentlicht 22.09.2026 (Deprecations-Seite) | [Models](https://ai.google.dev/gemini-api/docs/models), [Deprecations](https://ai.google.dev/gemini-api/docs/deprecations) |
| `gemini-3.8-flash-lite-tts` | „New Stable“ | veröffentlicht 22.09.2026 | dieselben |
| `gemini-3.1-flash-tts-preview` | „Preview (Legacy)“ | Abschaltung frühestens **17.11.2026**, Ersatz 3.8 | Deprecations |
| `gemini-2.5-flash-preview-tts` | kein Status genannt | Abschaltung frühestens **17.11.2026**, Ersatz 3.8 | Deprecations |
| `gemini-2.5-pro-preview-tts` | kein Status genannt | Abschaltung frühestens **17.11.2026**, Ersatz 3.8 | Deprecations |
| `gemini-2.5-flash-native-audio-preview-12-2025` (Live API) | Preview | Abschaltung frühestens 17.11.2026, Ersatz `gemini-3.8-live` | Deprecations |

**Widerspruch in Googles eigenen Seiten:** Die Modellseite sagt zu den 2.5-Modellen „These models are not deprecated and will continue to be served until further notice“, die Deprecations-Seite nennt dafür den 17.11.2026 als frühesten Abschalttermin. Die Deprecations-Seite gilt als der strengere Wert. Für eine Entscheidung also nur 3.8 in Betracht ziehen.

Das Modell `gemini-2.5-flash-lite-preview-tts` kommt in der Gemini API nicht vor (laut Hauptagent liefert ListModels es auch nicht), nur in der Cloud-Doku und im Artificial-Analysis-Ranking.

### Google Cloud Text-to-Speech API (cloud.google.com)

Quelle: [docs.cloud.google.com/text-to-speech/docs/gemini-tts](https://docs.cloud.google.com/text-to-speech/docs/gemini-tts), abgerufen 21.10.2026.

| Modell-ID | Status | Sprecher |
|---|---|---|
| `gemini-2.5-flash-tts` | im Text nicht als Preview gekennzeichnet; Release Notes: **GA seit 30.09.2025** | einzeln und mehrere |
| `gemini-2.5-pro-tts` | GA seit 30.09.2025 (Release Notes) | einzeln und mehrere |
| `gemini-2.5-flash-lite-preview-tts` | Preview | nur einzeln |
| `gemini-3.1-flash-tts-preview` | Preview, nur Region `global` | einzeln und mehrere |
| 3.8 Flash TTS und 3.8 Flash-Lite TTS | **nicht über die Cloud-TTS-API**; „available in Preview through the Gemini Enterprise API only“ | |

Cloud-Release-Notes: [docs.cloud.google.com/text-to-speech/docs/release-notes](https://docs.cloud.google.com/text-to-speech/docs/release-notes) (Einträge 30.09.2025 GA 2.5; 07.11.2025 Streaming für Gemini-TTS). Die abgerufene Seite endet bei Dezember 2025, spätere Einträge zu 3.1 und 3.8 stehen dort nicht (vermutlich veraltete Seitenfassung, **nicht belegt**).

3.8 Flash TTS in der Vertex-Welt („Gemini Enterprise Agent Platform“): Status **Preview**, Region nur `global`, Veröffentlichung dort mit **28.09.2026** angegeben, Eingabe 8.192 / Ausgabe 16.384 Token, Streaming und GenerateContent unterstützt, Live API nicht ([Modellseite](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/3-8-flash-tts), 21.10.2026). Die Entwickler-API nennt dasselbe Modell „Stable“, die Cloud-Seite „Preview“. Beides ist belegt; für Produktion zählt, über welchen Zugang man geht.

### Stimmen

- **30 vorgefertigte Stimmen** in allen Gemini-Modellen: Zephyr, Puck, Charon, Kore, Fenrir, Leda, Orus, Aoede, Callirrhoe, Autonoe, Enceladus, Iapetus, Umbriel, Algieba, Despina, Erinome, Algenib, Rasalgethi, Laomedeia, Achernar, Alnilam, Schedar, Gacrux, Pulcherrima, Achird, Zubenelgenubi, Vindemiatrix, Sadachbia, Sadaltager, Sulafat ([Speech generation](https://ai.google.dev/gemini-api/docs/speech-generation), 21.10.2026). Mit Stilkürzeln, z. B. Leda = „Youthful“, Kore = „Firm“, Sulafat = „Warm“.
- **Gleiche Namen wie Chirp 3: HD:** Die Cloud-Doku sagt, die Gemini-TTS-Stimmen seien „similar to our existing Chirp 3: HD Voices“; **alle 30 Namen** der Gemini-Liste stehen auch auf der Chirp-3-HD-Seite (Abgleich per Textsuche am 21.10.2026, keiner fehlt; [Chirp 3 HD](https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd)). Ob „Leda“ in Gemini genauso klingt wie „Leda“ in Chirp 3 HD, ist **nicht belegt**; „ähnlich“ ist nicht „identisch“.
- Zusätzlich bei 3.8: Extended Voice Library (`GET /v1beta/voices`; „more than 2,000 curated voices“ laut Cloud-Seite, „hundreds“ laut Gemini-API-Seite, **uneinheitlich**), Voice design (Stimme per Beschreibung), Voice replication (Klonen aus Referenzaudio und Einverständnis). Limit 200 eigene Stimmen je Projekt, Lebensdauer 1 Jahr ab letzter Nutzung.

### Deutsch

- 3.8 Flash TTS: „over 130 languages“ (Deutsch ✔), 3.8 Flash-Lite TTS: 101 Sprachen (Deutsch ✔). Spracherkennung automatisch. Quelle: [Speech generation](https://ai.google.dev/gemini-api/docs/speech-generation), [Modellseite Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash-tts).
- Cloud-TTS-Gemini (2.5/3.1): `de-DE` ist **GA** (24 Sprachen GA). Quelle: Cloud-Seite oben.
- Deutsche Qualitätsmessung (Hörtest, Wortfehler, Aussprache-Benchmark speziell für Deutsch): **nicht belegt**, siehe 9.

### Mehrsprecher, Stil

- Bis zu **2 Sprecher** je Anfrage mit vorgefertigten Stimmen; jeder Beitrag muss `speaker` nennen. Eigene Stimmen nur einzeln erzeugen und zusammenfügen.
- Stil: bei 3.8 über `speech_metadata.style` je Beitrag (z. B. „whispered urgently“); ältere Modelle über einen Freitext-Prompt.

## 3. Preise (Paid Tier) und Preis je Audiominute

**Umrechnung:** „Audio tokens correspond to 25 tokens per second of audio“ ([Preisseite Gemini API](https://ai.google.dev/gemini-api/docs/pricing), Stand der Seite 19.10.2026; ebenso [Cloud-TTS-Preise](https://cloud.google.com/text-to-speech/pricing)). Das sind **1.500 Audio-Token je Minute**. Rechnung: Preis je 1 Mio. Audio-Token ÷ 1 Mio. × 1.500.

Preise je 1 Mio. Token, USD, Standard-Tarif der Gemini API (Cloud-TTS und Vertex haben dieselben Zahlen):

| Modell | Text-Eingabe | Audio-Ausgabe | Audio je Minute (nur Ausgabe) | Batch (Ein/Aus) |
|---|---|---|---|---|
| 3.8 Flash TTS, bis 31.12.2026 | 0,50 | 9,00 | **0,0135 $** (Google nennt 0,00225 $ je 10 s) | 0,25 / 4,50 |
| 3.8 Flash TTS, ab 01.01.2027 | 1,00 | 18,00 | **0,027 $** | 0,50 / 9,00 |
| 3.8 Flash-Lite TTS, bis 31.12.2026 | 0,50 | 6,00 | **0,009 $** | 0,25 / 3,00 |
| 3.8 Flash-Lite TTS, ab 01.01.2027 | 1,00 | 12,00 | **0,018 $** | 0,50 / 6,00 |
| 3.1 Flash TTS Preview | 1,00 | 20,00 | 0,030 $ | 0,50 / 10,00 |
| 2.5 Flash (Preview) TTS | 0,50 | 10,00 | 0,015 $ | 0,25 / 5,00 |
| 2.5 Pro (Preview) TTS | 1,00 | 20,00 | 0,030 $ | 0,50 / 10,00 |

- Die Gemini-API-Seite führt für 3.8 zusätzlich **Flex** (Preis wie Batch) und **Priority** (1,8-fach); Details siehe Seite.
- Die Vertex-Seite vermerkt zu den 3.8-Preisen bis 31.12.2026: „Promotional pricing provided through 50% credits back on net spend on the same model“. Der Aktionspreis läuft also zum Jahreswechsel aus und **verdoppelt** sich ([Vertex-Preise](https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing)). Auf der Seite von 3.1 steht keine solche Befristung.
- **Abrechnungsart in Cloud TTS:** Die Gemini-TTS-Zeilen werden **je Token** abgerechnet (Text-Eingabe und Audio-Ausgabe), während der Rest der Cloud-TTS-Preisseite je Zeichen rechnet. Die Präambel („priced based on the number of characters“) gilt für die klassischen Stimmen.

### Beispielrechnung mit Franks Testtext A (356 Zeichen)

Messwerte des Hauptagenten vom 21.10.2026 (Gemini API, `serviceTier` standard): 152 Prompt-Token, 866 bis 1.046 Audio-Token. Das sind **34,6 bis 41,8 Sekunden** Audio, also 8,5 bis 10,3 Zeichen je Sekunde und 2,4 bis 2,9 Audio-Token je Zeichen (eigene Rechnung).

| Modell | je Aufruf (bei 1.046 Audio-Token) | je 1 Mio. Zeichen (eigene Hochrechnung) |
|---|---|---|
| 3.8 Flash TTS, 2026 | ca. 0,0095 $ | ca. 22 bis 27 $ |
| 3.8 Flash TTS, ab 2027 | ca. 0,0190 $ | ca. 44 bis 53 $ |
| 3.8 Flash-Lite TTS, 2026 | ca. 0,0063 $ | ca. 15 bis 18 $ |
| 3.8 Flash-Lite TTS, ab 2027 | ca. 0,0127 $ | ca. 30 bis 36 $ |
| Chirp 3 HD (zum Vergleich) | 0,0107 $ | 30 $, davon erste 1 Mio. frei |

Hinweis: Artificial Analysis nennt für Standardeinstellungen 16,49 $ (Flash) und 11,03 $ (Flash-Lite) je 1 Mio. Zeichen; das sind englische Textlängen mit eigenem Umrechnungsfaktor. Für Franks deutsche Texte zählt die eigene Hochrechnung (Rundung grob; die Tabelle ist keine Offerte).

## 4. Freikontingente sauber getrennt

### (a) Gemini API / AI Studio, Free Tier

- Preisseite (Stand 19.10.2026, abgerufen 21.10.2026): für `gemini-3.8-flash-tts`, `gemini-3.8-flash-lite-tts`, `gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts` steht bei Free Tier Eingabe und Ausgabe „**Free of charge**“. `gemini-2.5-pro-preview-tts`: „Not available“. Batch und Flex: im Free Tier „Not available“. (Die Priority-Zeilen zeigen widersinnig ebenfalls „Free of charge“, vermutlich Seitenartefakt, nicht beachten.) Quelle: [Pricing](https://ai.google.dev/gemini-api/docs/pricing).
- Ohne Abrechnungskonto? **Ja.** [Rate limits](https://ai.google.dev/gemini-api/docs/rate-limits): Free-Stufe = „Active project“ oder Free Trial. [Billing](https://ai.google.dev/gemini-api/docs/billing): „New accounts begin on the Free Tier, which allows access to certain models in the Gemini API and AI Studio, up to the models' free tier rate limits“. Wechsel in die bezahlte Stufe verlangt Prepay (mindestens 5 $).
- **RPM/TPM/RPD je Modell: nicht öffentlich.** Die Seite nennt nur die drei Messgrößen, „Rate limits are applied per project, not per API key“, RPD-Zurücksetzung um Mitternacht pazifischer Zeit, „Rate limits are more restricted for experimental and preview models“ und verweist auf das Dashboard (aistudio.google.com/rate-limit, Anmeldung nötig). Eine Tabelle mit TTS-Werten fehlt. Zahlen von Dritten (**nicht verifiziert**): ein Spiegel-Dokument nennt für 2.5 Flash Preview TTS 3 RPM, 10.000 TPM, 15 RPD; ein Entwickler berichtet im Forum für **Tier 1 (bezahlt)** 10 RPM und 100 RPD (Flash) bzw. 50 RPD (Pro) im Juli 2025 ([Forum](https://discuss.ai.google.dev/t/tier-1-rate-limit-blocks-20-30-s-gemini-tts-clips-promo-credits-delay-250-spend-30-day-upgrade/91803)). Beides gilt für 2.5, nicht für 3.8. Für 3.8 ist nichts belegt; Franks Schlüssel zeigt die Zahlen im AI-Studio-Dashboard.
- Bezahlte Stufen: Tier 1 (Abrechnungskonto), Tier 2 (100 $ Ausgaben + 3 Tage), Tier 3 (1.000 $ + 30 Tage). Für Batch-TTS: Tier 1 25.000 (2.5 Pro) bzw. 100.000 (2.5 Flash) Token in der Warteschlange.
- Regionen: „Google AI Studio usage is free of charge in all available regions“ (Preisseite); Deutschland steht in den [verfügbaren Regionen](https://ai.google.dev/gemini-api/docs/available-regions). Zur Datennutzung im EWR siehe 8.

### (b) Gemini-TTS in Cloud Text-to-Speech

- Alle Gemini-TTS-Zeilen: „Free usage limit: **Not available**“ ([Preisseite](https://cloud.google.com/text-to-speech/pricing), 21.10.2026).
- Abrechnung muss aktiviert sein; berechnet wird sofort ab dem ersten Token.
- Kontrast: **Chirp 3: HD „0 to 1 million characters“ frei** (auch Instant Custom Voice: nicht frei, 60 $ je 1 Mio. Zeichen). Die Seite sagt: „will be automatically charged if your usage exceeds the number of free characters allowed per month“, die Freigrenze gilt also je Monat.
- Quoten Cloud TTS ([Quotas](https://docs.cloud.google.com/text-to-speech/quotas)): `gemini-2.5-flash-tts` 150 QPM, `gemini-2.5-pro-tts` 125 QPM, 3.1 Flash „dynamic throughput-based“ (Standard PayGo), Chirp 3 HD 200 Anfragen/Min je Projekt, 100 gleichzeitige Streaming-Sitzungen.

### (c) Vertex AI / Gemini Enterprise Agent Platform

- Kein Freikontingent genannt; Preise wie in 3. 3.8 Flash TTS nur Region `global`, Preview.
- Cloud-TTS-Hinweis: Mit Vertex-Aufruf braucht man `aiplatform.endpoints.predict` (Rolle `roles/aiplatform.user`).

### (d) Testguthaben 300 $

- Google Cloud Free Trial: 300 $ für 90 Tage ([Free features](https://docs.cloud.google.com/free/docs/free-cloud-features)). Ausdrücklich: „The $300 credit can't pay for Gemini API in AI Studio costs. Consider using the Gemini API Free tier, or sign up for a Prepay billing plan.“ Für Cloud-TTS oder Vertex ist keine Ausnahme genannt; ob Gemini-TTS in Cloud TTS vom Guthaben bezahlbar ist, ist **nicht ausdrücklich belegt** (keine Ausnahme auf der Seite gefunden). Frank ist laut Hauptagent vermutlich kein Neukunde; Neukunden-Bedingung beachten.

### (e) Aktionen, Abos

- **Google AI Pro** (19,99 $/Monat) enthält im Google Developer Program Premium „$10 GenAI and Cloud monthly credit“; Google AI Ultra (20 TB) 40 $, Ultra (30 TB) 100 $ je Nutzer und Monat ([Developer-Program-Seite](https://developers.google.com/program/plans-and-pricing), 21.10.2026). Das Guthaben dient „AI Studio, Gemini platform or any Google product“ und wird als Cloud-Guthaben gebucht. Die [Billing-Seite](https://ai.google.dev/gemini-api/docs/billing) sagt: Wer ein Prepay-Konto hat, muss zuerst Guthaben einzahlen, bevor Aktionsguthaben greift; danach wird das Aktionsguthaben zuerst verbraucht. Rechenbeispiel (eigene Rechnung): 10 $ entsprechen etwa 740 Minuten 3.8 Flash TTS (Aktionspreis 2026) bzw. 370 Minuten ab 2027, oder rund 1.100 Minuten Flash-Lite 2026. Ob Frank AI Pro hat, ist unbekannt (Hauptagent klären).
- Weitere Aktionen (Startups, Cloud-Credits von Partnern): **nicht recherchiert/nicht belegt**.

## 5. Ratenlimits, Batch, Streaming, Latenz, Eingabelänge

| Punkt | Wert | Beleg |
|---|---|---|
| Eingabe | 8.192 Token | Modellseiten Gemini API und Cloud |
| Ausgabe | 16.384 Token (Serving-Limit Gemini API) | dito |
| Cloud-TTS-API | Textfeld ≤ 4.000 Bytes, Promptfeld ≤ 4.000 Bytes, zusammen ≤ 8.000 Bytes; Ausgabe ca. **655 Sekunden**, danach abgeschnitten | [Cloud-Seite](https://docs.cloud.google.com/text-to-speech/docs/gemini-tts) |
| Kontextfenster | „32k tokens per session“ nur bei einer Drittquelle (invideo, August 2026); offiziell **nicht belegt** | |
| Ausgabeformat Gemini API | unary: WAV (`audio/wav`, RIFF, 24 kHz, mono, 16 Bit), streaming: `audio/l16` (rohes PCM 24 kHz); wählbar `audio/mulaw`, `audio/alaw`, Abtastraten 24000/16000/8000 | [Speech generation](https://ai.google.dev/gemini-api/docs/speech-generation) |
| Ausgabeformat Cloud-TTS | unary LINEAR16 (Standard), ALAW, MULAW, MP3, OGG_OPUS, PCM; streaming PCM, ALAW, MULAW, OGG_OPUS | Cloud-Seite |
| Streaming Gemini API | **ja**, `stream: true` liefert Audiostücke (`step.delta`, Typ audio) | Speech generation |
| Streaming Cloud-TTS | „Multi-Request-Multi-Response“ nur mit Gemini 2.5/3.1: „the API only starts synthesizing when the client sends Half-Close“; seit 07.11.2025. Vertex: eine Anfrage, mehrere Antworten | Cloud-Seite, Release Notes |
| Parameter | Vertex ignoriert `temperature`, `top_k`, `top_p` | Cloud-Seite |
| Batch/Flex/Priority | bei 3.8 unterstützt (Gemini API) | Modellseite |
| Weitere Funktionen | 3.8: keine Live API, kein Caching, kein Function Calling, kein Thinking | Modellseite |

**Latenz (Antwortbeginn):** Google nennt **keine Zahlen** („low latency“ nur als Beschreibung). Ein Messwert für „Zeit bis zum ersten Audio“ ist **nicht belegt** (Suche nach Benchmarks ohne Treffer). Belegt ist nur der **Durchsatz** bei Artificial Analysis (mediane Zeichen je Sekunde Erzeugungszeit, 21.10.2026; keine Latenz bis zum ersten Byte, Feld `latencyPerformance` ist leer):

| Modell | Median Zeichen/s |
|---|---|
| 3.8 Flash-Lite TTS | 94,9 |
| 3.8 Flash TTS | 55,8 |
| 3.1 Flash TTS | 40,7 |
| 2.5 Flash TTS (Dez 2025) | 34,3 |
| Chirp 3: HD (zum Vergleich) | 122,1 |

Quelle: [artificialanalysis.ai/text-to-speech](https://artificialanalysis.ai/text-to-speech) (Daten der Seite). Beispiel: Testtext A (356 Zeichen) braucht bei 55,8 Zeichen/s rund 6,4 s Erzeugungszeit (eigene Rechnung, ohne Netzlauf und Start); bei Chirp 3 HD etwa 2,9 s. Bei Streaming verkürzt sich die Wartezeit auf den ersten Block, die Zahl dafür fehlt.

## 6. Aussprachekontrolle

| Mittel | Gemini-TTS | Beleg |
|---|---|---|
| SSML | **nicht dokumentiert/nicht unterstützt** (Steuerung nur über Prompt/Stil und Tags). Zum Vergleich: **Chirp 3 HD unterstützt SSML-Teilmenge** (`<phoneme>`, `<p>`, `<s>`, `<sub>`, `<say-as>`) seit 17.10.2025. | Cloud-Seite; Release Notes |
| Stil | 3.8: `speech_metadata.style` je Beitrag; „most requests need no `style` instruction at all“ | Speech generation |
| Eingebettete Tags | 3.8: Spitzklammern, z. B. `<laugh>`, `<sigh>`, `<cough>`, `<breath>`, `<short pause>`, `<long pause>`, `<whispers>`; bei nicht-englischem Text trotzdem englische Tags verwenden. Cloud 3.1/2.5: eckige Klammern (`[sigh]`, `[laughing]`, `[short pause]` ≈ 250 ms, `[medium pause]` ≈ 500 ms, `[long pause]` ≥ 1000 ms). | Speech generation; Cloud-Seite |
| Betonung | Großbuchstaben, Satzzeichen, Auslassungspunkte | Speech generation |
| Grundregel 3.8 | Der Text ist „strictly a verbatim transcript“; Regieanweisungen gehören ins `style`-Feld, nicht in den Text. | Speech generation |
| Zahlen, Daten, Währung | **Keine Dokumentation**, wie die Entwickler-API „21.10.2026“ oder „1.249,99 €“ liest. Die Cloud-TTS-API „applies text normalization by default“ (als Vorteil gegenüber Vertex genannt, besonders für Daten und Geldbeträge); die Entwickler-API dazu **nichts**. | Cloud-Seite |
| Abkürzungen, Akronyme | nicht dokumentiert. | |

Zu Testtext B („Lies die Satzzeichen wörtlich mit“): Bei 3.8 würde ein solcher Satz im Textfeld nach Doku wörtlich vorgelesen, nicht befolgt. Er gehört, wenn überhaupt, in `speech_metadata.style`. Ob der Stil „Satzzeichen mitsprechen“ überhaupt eine Wirkung hat, ist **nicht dokumentiert**. Messung des Hauptagenten (21.10.2026): `gemini-2.5-flash-preview-tts` lehnt die Anweisung mit HTTP 400 ab („Model tried to generate text“); 2.5 Pro, 3.1 und 3.8 nehmen sie an (Annahme heißt noch nicht, dass sie sich daran halten; dazu Hörprobe nötig).

### Bekannte Probleme (Foren, Meldungen von Entwicklern)

Alle aus dem Google-AI-Developers-Forum, Abruf 21.10.2026; offizielle Stellungnahmen gibt es kaum.

| Problem | Modell | Datum | Quelle |
|---|---|---|---|
| HTTP 200, Audio fehlt oder abgeschnitten, `finishReason: OTHER`, trotzdem berechnet | 3.1 Flash, 2.5 | Mai bis Juli 2026 | [Forum](https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-preview-audio-truncation-finishreason-other-is-time-varying-and-not-voice-specific-two-audits-of-all-30-voices-2h-apart/176215) |
| Messung 25.07.2026: 15 von 30 Stimmen brachen ab, zwei Stunden später 23 von 30 (50 bis 75 %, 141 französische Wörter, Streaming per SSE); Abhilfe: `finishReason` und Dauer prüfen, bis zu 3 Wiederholungen, Blöcke von ca. 70 Wörtern | 3.1 Flash | 27.07.2026 | dasselbe |
| Streaming-Antwort liefert genau 20 s (1.280.000 Base64-Zeichen, 640 Token) und `OTHER`; Wiederholung liefert oft 29 bis 31 s vollständig | 3.1 Flash | 14.05.2026 | [Forum](https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-sse-sometimes-returns-exactly-20s-1-280-000-base64-chars-and-truncated-audio/144813) |
| Mehrsprecher: Abbrüche (13 bis 46 % der Länge), vertauschte Stimmen, erfundene Zeilen, doppelte/übersprungene Zeilen, verzerrte Kurzäußerungen; Google nennt für die Vertex-Abbrüche einen Kapazitätsvorfall vom 27.02., Abhilfe Developer-API | 2.5 Flash/Pro, 3.1 | 15.03. bis 12.08.2026 | [Forum](https://discuss.ai.google.dev/t/gemini-tts-multi-speaker-mode-7-critical-bugs-after-3-weeks-in-production-finishreason-other-truncation-voice-swapping-hallucinated-lines/132776) |
| Keine Ausgabe bei niedriger Temperatur (unter ca. 0,5, `finishReason: OTHER`), Abhilfe Temperatur 1 | 2.5 Flash Preview | 04.04.2026 | [Forum](https://discuss.ai.google.dev/t/gemini-2-5-tts-model-not-working-at-low-temperatures/137850) |
| 500 INTERNAL auf jede Anfrage / sporadisch (erfolgreich nach 2 bis 4 Wiederholungen) | 3.1 Flash; 2.5 Flash | 2026 | [Forum](https://discuss.ai.google.dev/t/bug-report-gemini-3-1-flash-tts-preview-returns-500-internal-on-every-request-tts-unusable/167574), [Forum](https://discuss.ai.google.dev/t/deadline-of-600-0s-exceeded/102402) |
| Stilanweisungen ignoriert / falsche Stimme | 2.5 Flash/Pro Preview | 2025 | [Forum](https://discuss.ai.google.dev/t/gemini-2-5-flash-pro-preview-tts-wrong-voice-is-used-style-instructions-are-ignored/111935) |
| Prompt wird mitgesprochen (Regieanweisungen, Kopfzeilen wie „DIRECTOR'S“) | 2.5/3.1 | 2026 | [LiveKit-Leitfaden](https://livekit.com/blog/gemini-3.1-flash-tts-prompting-guide) (sekundär) |
| Stimmen wirken „generisch“; Stilanweisungen manchmal ignoriert | 3.8 (und Vorgänger) | 24.09.2026 | [eesel.ai](https://www.eesel.ai/blog/gemini-3-8-flash-tts-review) (sekundär) |

**Gemini 3.8 TTS:** Spezifische Fehlerberichte (Abbrüche, 500er) habe ich **nicht gefunden**; das Modell ist erst vier Wochen alt. Das heißt nicht, dass es solche Probleme nicht gibt. Die 3.8-Doku empfiehlt gegen Stimmdrift: keine langen Stil-Blöcke, stattdessen Voice design; je Beitrag ein kurzer, konstanter Stil; lange Antworten in kürzere Beiträge zerlegen; ein Aufruf je LLM-Textblock (Sprachagenten-Muster).

## 7. Gemini Live API (native audio) als TTS-Alternative, kurz

Preise ([Gemini API Pricing](https://ai.google.dev/gemini-api/docs/pricing)), je 1 Mio. Token:

| Modell | Free Tier | Eingabe | Ausgabe |
|---|---|---|---|
| `gemini-3.8-live`, `-extended-thinking`, `gemini-3.1-flash-live-preview` | „Free of charge“ | Text 0,75; Audio 3,00 (= 0,005 $/Min) | Text 4,50; Audio **12,00 (= 0,018 $/Min)** |
| `gemini-2.5-flash-native-audio-preview-12-2025` | „Free of charge“ | Text 0,50; Audio/Video 3,00 | Text 2,00; Audio 12,00 |

- Pro Minute **gesprochenen** Audios also 0,018 $, gleich teuer wie 3.8 Flash-Lite TTS ab 2027 (0,018 $), 33 % teurer als 3.8 Flash im Aktionspreis 2026 (0,0135 $) und 33 % billiger als 3.8 Flash ab 2027 (0,027 $). Es kommt der wachsende Verlauf als Eingabe dazu (laut Best-Practice-Notiz quadratisch wachsende Kosten, siehe `best-practices/apis/google-gemini-api.md` §13).
- Free-Tier-Limits für Live: nicht veröffentlicht. Das alte Native-Audio-Modell wird frühestens am 17.11.2026 abgeschaltet.
- Einordnung: Live ist ein Dialogmodell mit eigener Antwort, kein reiner Vorleser. Für ein festes, wörtlich zu sprechendes Skript (Jarvis‑Antworten) ist TTS passender, weil es den Text unverändert liest; die Live-API „formuliert“ selbst. Jarvis nutzt Live bereits.

## 8. Lizenz, Nutzungsbedingungen, Datenschutz

- **Kommerziell:** [Gemini API Additional Terms](https://ai.google.dev/gemini-api/terms) (wirksam 23.03.2026): „Google won't claim ownership over that content“; man darf nach den API-Bedingungen generierte Inhalte nutzen, hat aber Recht einzuhalten und bei Bedarf Nutzer zu informieren. Cloud-TTS: „You can use the audio data files you create ... in your applications or media in compliance with the Google Cloud Platform Terms of Service“ ([Quotas](https://docs.cloud.google.com/text-to-speech/quotas)). Der Cloud-Preview-Text erlaubt ausdrücklich Produktions- und kommerzielle Nutzung (Pre-GA-Bedingungen).
- **Nicht für Verbraucher:** „Use of Google AI Studio and Gemini API is for developers ... for professional or business purposes, not for consumer use.“
- **Wasserzeichen:** Jede 3.1-Flash-TTS-Ausgabe trägt ein SynthID-Wasserzeichen (unhörbar; [Google-Blog](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-1-flash-tts/)). Für 3.8 nennt das [japanische Google-Blog](https://blog.google/intl/ja-jp/company-news/technology/gemini-3-8-text-to-speech/) SynthID-Wasserzeichen und C2PA-Credentials beim Stimmklonen, das [deutsche Google-Watch-Blog](https://www.googlewatchblog.de/?p=275767) ebenfalls SynthID für jede Datei. Für 2.5 TTS nicht ausdrücklich belegt.
- **Daten Free vs Paid (Gemini API):** Unpaid Services: Google nutzt Eingaben und Antworten zur Produktverbesserung, menschliche Prüfer dürfen mitlesen („Do not submit sensitive, confidential, or personal information“). Paid Services: kein Training, Protokollierung für begrenzte Zeit nur zur Missbrauchserkennung. **EWR, Schweiz, UK:** „the terms under ‘How Google uses Your Data’ in ‘Paid Services’ apply to all Services, including Google AI Studio and unpaid quota in the Gemini API“. Demgegenüber steht auf der Preisseite pro Modell „Used to improve our products: Yes (Free)“ ohne Regionshinweis. Die Nutzungsbedingungen sind die verbindlichere Quelle.
- **Fallstrick:** Dieselben Bedingungen sagen: „You may use only Paid Services when making API Clients available to users in the European Economic Area, Switzerland, or the United Kingdom.“ Ob Franks eigenes Jarvis (nur Frank, in Deutschland) „API Clients available to users“ ist, ist eine Auslegungsfrage. Wortlaut zeigt: Free Tier für ein App-Produkt mit EWR-Nutzern ist **nicht erlaubt**; für ein rein privates Werkzeug **unklar**. Vor Dauerbetrieb prüfen oder auf den bezahlten Zugang (Prepay mindestens 5 $) setzen.
- **Cloud TTS:** „Google does not log any customer Cloud TTS text or audio data.“ ([Data logging](https://docs.cloud.google.com/text-to-speech/docs/data-logging)); gilt für die Cloud-TTS-API. Region EU (`eu`) ist für 2.5-Modelle verfügbar; 3.1 und 3.8 nur `global`. Datenverarbeitung „in the specific region or multi-region where the request is made“.
- Vertex: Gemini-3.8-Preview-Seite erlaubt Produktivnutzung und personenbezogene Daten gemäß Cloud-Datenverarbeitungszusatz.

## 9. Unabhängige Qualitätsbelege

**Artificial Analysis, Speech Arena (blinde Hörvergleiche, Elo)** — [artificialanalysis.ai/text-to-speech/leaderboard](https://artificialanalysis.ai/text-to-speech/leaderboard), Daten der Seite am 21.10.2026 (98 Einträge, Filter nur Akzent US/UK und Kategorien; **kein Sprachfilter, Deutsch nicht belegt**):

| Rang | Modell | Elo (95 %) | Stimmen | Preis/1 Mio. Zeichen (AA) |
|---|---|---|---|---|
| 1 | Eleven v4 Turbo | 1335 | | |
| 5 | **Gemini 3.8 Flash TTS** | **1278** (1264–1292) | 8 | 16,49 $ |
| 8 | **Gemini 3.8 Flash-Lite TTS** | **1251** (1238–1264) | 8 | 11,03 $ |
| 16 | Gemini 3.1 Flash TTS | 1213 (1202–1224) | 7 | 18,31 $ |
| 49 | Gemini 2.5 Flash Lite TTS | 1086 | 5 | 9,20 $ |
| 59 | Gemini 2.5 Flash TTS (Dez 2025) | 1063 | 8 | 9,20 $ |
| 60 | **Chirp 3: HD** (Franks heutige Stimme) | **1061** (1049–1073) | 8 | 30,00 $ |
| 72 | Gemini 2.5 Pro (Dez 2025) | 1038 | 4 | 18,31 $ |

- Gemini 3.8 Flash TTS liegt etwa **217 Elo-Punkte** über Chirp 3 HD, Flash-Lite 190 Punkte, bei vergleichbarem oder niedrigerem Preis. Der Abstand ist größer als die Konfidenzintervalle (±12 bis ±14). Das ist belegt, aber **englisch geprägt**: Die Stimmproben in der Arena sind (Filter US/UK) englischsprachig; Rückschluss auf Deutsch **nicht belegt**.
- In der separaten Wertung „Provider Voice Arena“ lag 3.8 Flash TTS laut Artificial-Analysis-Start-Meldung auf Rang 2 (Elo ca. 1263) und auf Rang 1 im „Pronunciation Robustness Benchmark“ mit 89,5 % (3.1: 88,2 %; Zweiter 3.8 Flash-Lite 87,4 %) — sekundär zitiert ([eesel.ai](https://www.eesel.ai/blog/gemini-3-8-flash-tts-review), [247wallst](https://247wallst.com/cards/googl-xpost-01m37n6xxramwt7h0ph57acs1m)); der Original-Post auf X war nicht abrufbar (HTTP 402). Die Aussprachemessung ist ebenfalls nicht auf Deutsch eingegrenzt.
- **Deutsch speziell:** Google nennt als Spitzenmärkte der Voice-Arena-Präferenz Japanisch, brasilianisches Portugiesisch, Vietnamesisch, Hocharabisch, mexikanisches Spanisch und Hindi (sekundär, [ecosistemastartup](https://ecosistemastartup.com/google-lanza-gemini-3-8-flash-tts-voz-ia-con-2-000-voces/)); **Deutsch ist nicht darunter**. Eine Suchzusammenfassung nennt Flash-Lite auf Platz 3 im deutschen Arena-Ranking (aus einem X-Post; nicht selbst verifiziert, **nicht als Beleg werten**). Zahlen wie „MOS 4,48 Deutsch“ aus einem Sammelartikel (aiintoai.vercel.app) sind ohne Methodik, nicht verwendbar.
- Der Normalisierungs-Benchmark von Async-Voice-AI (31 Kategorien, englisch plus 6 Sprachen) testet Gemini-TTS nicht und stammt von einem Anbieter; nicht verwertbar.
- Interessenkonflikt: Teile der Hume-Benchmarks stammen von einem Autor der Google-Ankündigung.

## 10. Bewertung für Jarvis (nur Fakten, keine Empfehlung)

- Ist das „große Freikontingent“ gemeint: 1 Mio. Zeichen/Monat Chirp 3 HD gibt es nur dort (Cloud TTS). **Gemini-TTS hat in Cloud TTS keines.**
- Gratis-Gemini-TTS gibt es nur über die Gemini-API (Free Tier), mit unbekannten Limits, Datennutzung zur Produktverbesserung außerhalb EWR/CH/UK und der Bedingung „nur bezahlte Dienste“ für EWR-Nutzer.
- Bezahlt ist 3.8 Flash-Lite mit ca. 0,009 $/Min (2026) bis 0,018 $/Min (ab 2027) deutlich günstiger je Minute als 3.1/2.5 Pro; Jarvis zahlt bei Chirp 3 HD heute 0 $ innerhalb 1 Mio. Zeichen, danach etwa 0,0107 $ je Testtext-A-Aufruf.
- Offene Punkte für den Hauptagenten: Free-Tier-Limits der Schlüssel im AI-Studio-Dashboard ablesen; deutsche Hörprobe (Testtext A/B); Abbruch-Verhalten (`finishReason`) bei 3.8 selbst messen; Abschaltung 17.11.2026 für 3.1/2.5 beachten.

## Offen / nicht belegt

- Free-Tier-RPM/TPM/RPD für 3.8 Flash und Flash-Lite TTS (nur im Dashboard, Login).
- Messwert „Zeit bis zum ersten Audio“ für alle Gemini-TTS (Google nennt keine; AA liefert nur Durchsatz).
- Deutsche Qualität/Aussprache/Normalisierung von Gemini-TTS (kein Benchmark gefunden).
- Normalisierung von Zahlen, Daten, Währung in der Entwickler-API (Cloud-API: Normalisierung „by default“, ohne Details).
- Ob das 300-$-Guthaben Cloud-TTS-Gemini deckt; weitere Aktionen (Startups).
- Ob SynthID auch bei 2.5-TTS gesetzt ist; ob 3.8-Wasserzeichen in Cloud-TTS-API-Pfaden gilt.
- Ob sich 3.8 bei der Anweisung „Satzzeichen wörtlich mitsprechen“ anders verhält als dokumentiert (Hörprobe).

## Quellenliste (alle abgerufen am 21.10.2026)

- https://ai.google.dev/gemini-api/docs/speech-generation (letzte Änderung 12.10.2026)
- https://ai.google.dev/gemini-api/docs/pricing (19.10.2026)
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://ai.google.dev/gemini-api/docs/billing
- https://ai.google.dev/gemini-api/docs/deprecations
- https://ai.google.dev/gemini-api/docs/models und /models/gemini-3.8-flash-tts, /models/gemini-3.8-flash-lite-tts (09.10.2026)
- https://ai.google.dev/gemini-api/terms (wirksam 23.03.2026)
- https://ai.google.dev/gemini-api/docs/available-regions
- https://docs.cloud.google.com/text-to-speech/docs/gemini-tts
- https://cloud.google.com/text-to-speech/pricing
- https://docs.cloud.google.com/text-to-speech/quotas, /docs/release-notes, /docs/data-logging, /docs/chirp3-hd
- https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/3-8-flash-tts
- https://docs.cloud.google.com/free/docs/free-cloud-features
- https://developers.google.com/program/plans-and-pricing
- https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-1-flash-tts/
- https://blog.google/intl/ja-jp/company-news/technology/gemini-3-8-text-to-speech/
- https://artificialanalysis.ai/text-to-speech/leaderboard und /text-to-speech
- Foren/Drittquellen: discuss.ai.google.dev (Links in Abschnitt 6), eesel.ai, invideo.io, 247wallst.com, ecosistemastartup.com, googlewatchblog.de, livekit.com

BEST-PRACTICES-KANDIDATEN:
- https://ai.google.dev/gemini-api/docs/speech-generation — Gemini 3.8 TTS: Text ist wörtliches Transkript, Stil nur in `speech_metadata.style`, Tags in Spitzklammern (bei Nicht-Englisch englische Tags), ein TTS-Aufruf je LLM-Textblock, kein Stil-Block je Beitrag; unary liefert WAV mit Header, streaming rohes PCM `audio/l16` (Version: Seite vom 12.10.2026, Modelle `gemini-3.8-flash-tts`, `gemini-3.8-flash-lite-tts`)
- https://cloud.google.com/text-to-speech/pricing — Freikontingent liegt bei Chirp 3 HD (1 Mio. Zeichen/Monat), Gemini-TTS in Cloud TTS hat keines und rechnet je Token (25 Audio-Token je Sekunde) ab (Stand 21.10.2026)
- https://ai.google.dev/gemini-api/terms — im EWR/CH/UK gelten auch im Free Tier die Paid-Datenregeln; für Apps mit EWR-Nutzern nur bezahlte Dienste erlaubt (Version: wirksam 23.03.2026)
- https://ai.google.dev/gemini-api/docs/deprecations — `gemini-3.1-flash-tts-preview`, `gemini-2.5-flash-preview-tts`, `gemini-2.5-pro-preview-tts` frühestens am 17.11.2026 abgeschaltet, Ersatz 3.8 (Stand 21.10.2026)

BUG-KANDIDATEN:
- https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-preview-audio-truncation-finishreason-other-is-time-varying-and-not-voice-specific-two-audits-of-all-30-voices-2h-apart/176215 — `gemini-3.1-flash-tts-preview` (REST streamGenerateContent): HTTP 200 mit abgeschnittenem Audio und `finishReason: OTHER`, 50 bis 75 % Ausfall, wird trotzdem berechnet (Messung 25.07.2026)
- https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-sse-sometimes-returns-exactly-20s-1-280-000-base64-chars-and-truncated-audio/144813 — `gemini-3.1-flash-tts-preview`: SSE liefert genau 20 s und `OTHER` (14.05.2026)
- https://discuss.ai.google.dev/t/gemini-2-5-tts-model-not-working-at-low-temperatures/137850 — `gemini-2.5-flash-preview-tts`: bei Temperatur unter ca. 0,5 keine Ausgabe, `finishReason: OTHER` (04.04.2026)
- https://discuss.ai.google.dev/t/gemini-tts-multi-speaker-mode-7-critical-bugs-after-3-weeks-in-production-finishreason-other-truncation-voice-swapping-hallucinated-lines/132776 — `gemini-2.5-flash-tts`/`-pro-tts`/3.1 Mehrsprecher: Abbrüche, vertauschte Stimmen, erfundene Zeilen (15.03.2026 bis 12.08.2026)
