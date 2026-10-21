# R1 – Google Cloud Text-to-Speech (ohne Gemini-TTS)

Recherchezeitpunkt und Abrufdatum aller Quellen: **21.10.2026**. Engine C (Sonnet-Schwarm), Researcher R1.
Methode: ausschließlich WebSearch/WebFetch. Keine Schlüssel gelesen, keine Testaufrufe gegen die API.

## Quellenlage und Vorbehalte (zuerst lesen)

- **Preisseite `cloud.google.com/text-to-speech/pricing`:** WebFetch liefert von dieser Seite nur den Titel (89 Zeichen, die Tabelle wird per JavaScript nachgeladen). Die Preis- und Kontingentzahlen unten stammen deshalb aus **WebSearch-Auszügen der offiziellen Seite** (Auszug „aktualisiert vor ca. 26 Tagen", also etwa 25.09.2026). Wörtliche Zitate in Anführungszeichen sind diese Auszüge, keine eigene Volltextprüfung. Quervergleich mit dem Abrechnungs-Screenshot eines Entwicklers (dev.to, 01.09.2026) siehe 2.2.
- **Dokuseiten** (`docs.cloud.google.com/text-to-speech/...`) wurden direkt abgerufen. Fußzeile von chirp3-hd, quotas, release-notes und list-voices-and-types: „Last updated 2026-10-20 UTC".
- WebFetch fasst Seiten durch ein kleines Modell zusammen. Bei langen Seiten (list-voices-and-types: 259 109 Zeichen) wurden mehrere Abschnitte gelesen, nicht jedes Zeichen. Wo das relevant ist, steht es dabei.
- Markierung: **belegt** = offizielle Quelle direkt gelesen; **belegt (Auszug)** = offizielle Seite nur über Suchauszug; **sekundär** = Drittquelle; **nicht belegt** = nichts Belastbares gefunden.

---

## 1. Stimmklassen und exakte Namen für de-DE

Quelle: https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types (21.10.2026, Stand 2026-10-20)

| Klasse | de-DE-Stimmen (laut Abruf) | Status laut Doku | Anmerkung |
|---|---|---|---|
| **Chirp 3: HD** | 30 Stimmen `de-DE-Chirp3-HD-<Name>`: Achernar, Achird, Algenib, Algieba, Alnilam, Aoede, Autonoe, Callirrhoe, Charon, Despina, Enceladus, Erinome, Fenrir, Gacrux, Iapetus, Kore, Laomedeia, Leda, Orus, Puck, Pulcherrima, Rasalgethi, Sadachbia, Sadaltager, Schedar, Sulafat, Umbriel, Vindemiatrix, Zephyr, Zubenelgenubi | de-DE **GA**; nur pa-IN und yue-HK sind Preview (chirp3-hd-Seite) | Weiblich: Achernar, Aoede, Autonoe, Callirrhoe, Despina, Erinome, Gacrux, Kore, Laomedeia, Leda, Pulcherrima, Sulafat, Vindemiatrix, Zephyr (14). Männlich: übrige 16. **Leda** (Franks Stimme) ist weiblich. |
| **Chirp-HD** (älter) | `de-DE-Chirp-HD-D` (m), `de-DE-Chirp-HD-F` (w), `de-DE-Chirp-HD-O` (w) | Status nicht angegeben | Seite verweist nur auf „voice refresh plan" (externes PDF, nicht gelesen). Ohne Markup/Pause (Almanach G9). |
| **Polyglot** | `de-DE-Polyglot-1` (m) | Status nicht angegeben (Altquelle: Preview) | In der aktuellen Preistabelle (Auszug) **nicht mehr als eigene Zeile** gelistet: Preis nicht belegt. |
| **Studio** | `de-DE-Studio-B` (m), `de-DE-Studio-C` (w) | GA, SSML außer `<mark>`, `<emphasis>`, `<prosody pitch>`, `<lang>` | „Single-speaker voices for news reading and broadcast content". |
| **Neural2** | `de-DE-Neural2-G` (w), `de-DE-Neural2-H` (m) | GA | Verfügbar in global und Single-Region-Endpunkten. |
| **WaveNet** | `de-DE-Wavenet-G` (w), `de-DE-Wavenet-H` (m) | GA | |
| **Standard** | `de-DE-Standard-G` (w), `de-DE-Standard-H` (m) | GA | |
| **Instant Custom Voice** (Chirp 3, Voice Cloning) | kein fester Stimmname; eigener Voice-Cloning-Key, de-DE als Zielsprache möglich | Launch-Stufe auf der Seite **nicht angegeben**; Zugang **nur für Allowlist**, Kontakt über den Vertrieb | Siehe 1.2 |

Hinweis zur Vollständigkeit: Die Zeilen Neural2/Standard/WaveNet nennen im Abrufauszug nur G und H. Ältere Stimmen mit A–F sind in diesem Auszug nicht aufgetaucht. Ob sie entfallen sind oder nur im Auszug fehlen, ist **nicht belegt**. Für Jarvis irrelevant (Chirp 3 HD).

Die Stimmenliste von Chirp 3 HD deckt sich vollständig mit der im Best-Practice-Dokument (30 Namen, alle für de-DE vorhanden).

### 1.1 Regionen (Chirp 3: HD)

Quelle: https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (21.10.2026)
`global`, `us`, `eu`, `asia-southeast1`, `europe-west2`, `asia-northeast1` – alle GA.

Abweichende Angabe auf https://docs.cloud.google.com/text-to-speech/docs/endpoints (21.10.2026): Chirp 3 HD sei verfügbar auf Global, US-Multiregion, EU-Multiregion, **London (europe-west2), Frankfurt (europe-west3), Eemshaven (europe-west4)** und Asien-Pazifik (Tokio, Sydney, Mumbai, Singapur, Seoul). Frankfurt und Eemshaven stehen **nicht** auf der chirp3-hd-Seite. Die zwei Seiten widersprechen sich. Single-Region US Central1 bietet laut Endpunkte-Seite nur Neural2.

### 1.2 Instant Custom Voice

Quelle: https://docs.cloud.google.com/text-to-speech/docs/chirp3-instant-custom-voice (21.10.2026)

- „Access to Instant Custom Voice is restricted to allow-listed users." Zugang: „contact a member of the sales team".
- 30 Locales, darunter de-DE mit eigenem deutschen Einwilligungstext. Ein Schlüssel mit Locale `en-US` kann auch in `de-DE` synthetisieren.
- Eingabe: zwei Dateien, einkanalig, bis 10 s, LINEAR16/PCM/MP3/M4A. Einwilligungstext der Doku ist Pflicht, eigene Texte sind nicht erlaubt.
- Quote: 10 Cloning-Keys pro Minute und Projekt (Seite) bzw. `GenerateVoiceCloningKeyRequestsPerMinutePerProject` = 10 und `ChirpVoiceCloningRequestsPerMinutePerProject` = 30 (Quotenseite).
- Steuerung: Pace 0,25–2,0 unterstützt; Pause und Custom Pronunciation als „experimental" markiert; Custom Pronunciation nicht in bn-IN, gu-IN, th-TH, vi-VN. SSML für geklonte Stimmen: nicht erwähnt.
- Preis: $60 pro 1 Mio Zeichen, **kein Freikontingent** (Preisseite, Auszug).
- Fazit für Jarvis: ohne Vertriebs-Freischaltung nicht nutzbar.

---

## 2. Preise und Freikontingent

### 2.1 Offizielle Preistabelle (belegt, Auszug)

Quelle: https://cloud.google.com/text-to-speech/pricing (WebSearch-Auszüge vom 21.10.2026; WebFetch liefert nur den Titel)

| Klasse | SKU (laut Auszug) | Freikontingent pro Monat | Preis danach je 1 Mio Zeichen |
|---|---|---|---|
| Standard | 9D01-5995-B545 | 4 Mio | $4 |
| WaveNet | 9D01-5995-B545 (gleiche SKU wie Standard, vermutlich Tippfehler auf der Seite) | 4 Mio | $4 |
| Neural2 | FEBD-04B6-769B | 1 Mio | $16 |
| Studio | 84AB-48C0-F9C3 | 1 Mio | $160 |
| **Chirp 3: HD** | F977-2280-6F1B | **1 Mio** | **$30** (US$0.00003 je Zeichen) |
| Instant Custom Voice | A247-37D7-C094 | keins („Not available") | $60 (US$0.00006 je Zeichen) |
| Polyglot | – | in der aktuellen Tabelle nicht gefunden | nicht belegt |
| Chirp-HD (älter) | – | nicht in der Tabelle gefunden | nicht belegt |

Wortlaut aus dem Auszug zu Chirp 3 HD: „Chirp 3: HD voices (sku:F977-2280-6F1B) … 0 to 1 million characters … US$0.00003 per character (US$30 per 1 million characters)". Weitere Wortlaute: „Text-to-Speech is priced based on the number of characters sent to the service to be synthesized into audio each month." und „You must enable billing to use Text-to-Speech, and will be automatically charged if your usage exceeds the number of free characters allowed per month."

**Widerspruch WaveNet:** Der Abrechnungs-Screenshot eines Entwicklers (dev.to, „Posted Aug 26, edited Sep 1", 2026, sekundär) zeigt WaveNet mit **1 000 000** freien Zeichen, Standard 4 000 000, Chirp3-HD 1 000 000, Studio 1 000 000. Eine ältere Google-Übersichtsseite sagt ebenfalls 1 Mio für WaveNet. Die Preisseite (Auszug) sagt 4 Mio. Ungeklärt, für Chirp 3 HD aber folgenlos: alle Quellen sagen **1 Mio**.
Quelle dev.to: https://dev.to/just_a_side_project/i-checked-googles-own-billing-console-so-you-dont-have-to-the-real-text-to-speech-free-tier-2l0m

### 2.2 Dauerhaft, einmalig oder Aktion?

| Art | Befund | Quelle |
|---|---|---|
| (a) **Dauerhaftes monatliches Freikontingent** | Ja: je Klasse laut Tabelle oben, „per month", bei Chirp 3 HD 1 Mio Zeichen. Es ist ein Rabatt auf die Abrechnung, kein Guthaben, und verfällt am Monatsende. Der Free-Tier-Hilfetext sagt allgemein: „Free Tier limits aren't credit; they don't accumulate or roll over from one interval to the next." | Preisseite (Auszug); https://docs.cloud.google.com/free/docs/free-cloud-features |
| (b) **Einmaliges Testguthaben** | Google Cloud Free Trial: „$300 Welcome credit to spend over 90 days", Zahlungsmittel (Kreditkarte o. ä.) zwingend. Das Guthaben gilt für „products and services covered by the Free Trial program". Ob Text-to-Speech dazugehört, nennt die Seite **nicht** (nicht belegt). Ausgeschlossen sind u. a. Gemini API in AI Studio und Partner-Modelle als Managed API. Trial-Konten können keine Quotenerhöhung beantragen. | https://docs.cloud.google.com/free/docs/free-cloud-features |
| (c) **Aktionen** | Keine Aktion für Cloud TTS gefunden (nicht belegt). | – |

Die Seite „Always Free"-Produkte (free-cloud-features) enthält **keine** Text-to-Speech-Zeile; sie verweist: „If a product isn't listed, then check the product's pricing page to see whether it includes Free Tier offerings." Das TTS-Kontingent steht also nur auf der TTS-Preisseite.

### 2.3 Abrechnungskonto, Zeichenzählung, Geltungsbereich

| Frage | Befund | Status |
|---|---|---|
| Abrechnungskonto nötig? | **Ja**, auch für das Freikontingent: „You must enable billing to use Text-to-Speech". Ohne aktives Billing 403 (Almanach GA9 bestätigt). | belegt (Auszug) |
| Zählen SSML-Tags/Markup? | Ja. Preisseite (Auszug): Eingabezeichen inklusive Leerzeichen und Zeilenumbrüchen zählen, „all SSML tags (except the … tag)" zählen mit. Der Name des Ausnahme-Tags ist im Suchauszug herausgefallen (ein Auszug nannte `<break>`, einer `<mark>`; der Almanach sagt `<mark>`). **Ausnahme-Tag nicht eindeutig belegt.** SSML-Seite: „SSML characters count toward character limits". `[pause]`-Tags im `markup`-Feld: Zählung nicht dokumentiert. | belegt (Auszug) / Ausnahme-Tag unklar |
| Umlaut = 1 Zeichen oder 2 Bytes? | Abrechnung nach **Zeichen**: Auszug der Preisseite „for Japanese and other multibyte scripts, you are only charged for one character, not multiple bytes". Das Requestlimit zählt dagegen **Bytes**: „Total bytes per request: 5,000", „In some locales (for example ja-JP), a single character is made up of multiple bytes". Ein deutscher Umlaut ist in UTF-8 2 Bytes, also 1 Abrechnungszeichen, aber 2 Bytes im 5000-Byte-Limit (Rechnung aus UTF-8, die Doku nennt Deutsch nicht ausdrücklich). | belegt (Auszug) / Ableitung |
| Freikontingent je Rechnungskonto oder Projekt? | Für TTS **nicht ausdrücklich dokumentiert**. Die allgemeine Free-Tier-Seite sagt: „there are monthly usage limits that are calculated per billing account" (einzelne Produkte wie Logging/Firestore sind je Projekt). Die Preisseite führt jede SKU „tied to your billing account" auf. Wahrscheinlich je Rechnungskonto und je SKU, aber **für TTS nicht belegt**. Der dev.to-Autor schließt, dass die Klassen-Kontingente sich **nicht teilen**, sondern je SKU gelten (Eigenbeobachtung, kein Beleg). | indirekt |
| Mindestabrechnung 100 Zeichen je Request? | Nur eine Drittquelle (texttolab.com) behauptet das. Nicht belegt. | nicht belegt |

---

## 3. Ratenlimits, Quoten, Größenlimits

Quelle: https://docs.cloud.google.com/text-to-speech/quotas (21.10.2026, Stand 2026-10-20)

| Limit | Wert |
|---|---|
| **Chirp3RequestsPerMinutePerProject** | **200 pro Minute** |
| ConcurrentStreamingSessionsPerProject | 100 gleichzeitige Sitzungen |
| ChirpVoiceCloningRequestsPerMinutePerProject | 30 pro Minute |
| GenerateVoiceCloningKeyRequestsPerMinutePerProject | 10 pro Minute |
| Neural2 / Polyglot / allgemein (`RequestsPerMinutePerProject`) | je 1 000 pro Minute |
| StudioRequestsPerMinutePerProject | 500 pro Minute |
| LongAudioSynthesis (Query und Request) | je 100 pro Minute |
| Inhalt pro Request | **5 000 Bytes** gesamt (nicht erhöhbar) |
| Größenlimit für Streaming-Requests | **nicht angegeben** |

- Wörtlich: „The content limits cannot be increased." und „You can request an increase to the requests limit" (Konsole). Die Werte seien „default maximum values and they are subject to change."
- `SynthesisInput`-Referenz: „The input size is limited to 5000 bytes." (https://docs.cloud.google.com/text-to-speech/docs/reference/rest/v1/SynthesisInput)
- Alle Limits sind **je Projekt**, nicht je Key.
- Gemini-TTS-Limits stehen ebenfalls auf der Seite (gemini-2.5-flash-tts 150 QPM, gemini-2.5-pro-tts 125 QPM; gemini-3.1-flash-tts-preview dynamisch): Thema R2.
- Tagesquote oder Monatsquote in Zeichen: **nicht dokumentiert**. Es gibt nur Minuten-Raten.

Delta zum Almanach: 200 RPM und 100 Streams bestätigt, 5000-Byte-Limit bestätigt, neu aufgenommen: Cloning-Quoten, Studio 500, Polyglot 1000.

---

## 4. Kostenbegrenzung

| Mittel | Wirkung | Quelle |
|---|---|---|
| **Budgets und Alerts** | Nur Warnung. Wörtlich: „Setting an alerts-only budget doesn't automatically cap Google Cloud or Google Maps Platform usage or spending." Benachrichtigungen bei 50/80/100 %, Verzögerung von mehreren Stunden zwischen Nutzung und Kostenmeldung („there is a delay between your use of Google Cloud resources, and the usage costs reporting to Cloud Billing"). | https://docs.cloud.google.com/billing/docs/how-to/budgets (21.10.2026) |
| **Spend-Cap-Budgets** (echte Kappung) | Existieren, aber nur für **Gemini API, Gemini Enterprise Agent Platform, Cloud Run, Cloud Run functions**. **Text-to-Speech ist nicht aufgeführt.** Durchsetzung nicht sofort („any cost overages are billed as normal"). | https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps (21.10.2026) |
| **Quoten-Override** | Seite: „create a quota override by changing the quota value to a value less than the default quota value" (auch „capping usage" genannt), aber „Quota overrides are not available to all services." Ob TTS Overrides erlaubt, ist **nicht belegt**. Außerdem sind die TTS-Quoten **Minutenraten**, keine Monatsmengen: Eine Absenkung begrenzt die Geschwindigkeit, nicht die Monatskosten (bei 200 RPM bis zu 8,6 Mio Requests im Monat rechnerisch möglich). | https://docs.cloud.google.com/docs/quotas/view-manage (21.10.2026) |
| **Abrechnung automatisch stoppen** | Möglich über Pub/Sub-Budgetbenachrichtigung plus Cloud Function, die Billing am Projekt abschaltet („programmatically disabling Cloud Billing on a project"). Folge laut Drittprojekt: alle Dienste im Projekt enden, auch Free-Tier-Dienste, Ressourcen können gelöscht werden; wegen der Meldeverzögerung kein harter Stopp auf den Cent. Komponente „sekundär": https://github.com/Cyclenerd/poweroff-google-cloud-cap-billing | Doku für den Mechanismus, Drittquelle für die Warnung |
| **Eigene Kappung in der App** | Zeichenzähler pro Monat im eigenen Code/Proxy (ohne Beleg aus der Doku, naheliegende Lösung): Cache nach Texthash, Zähler vor dem Request. | Eigene Schlussfolgerung |

Fazit: Für Cloud TTS gibt es **keine native Monatskappung**. Warnung per Budget, harter Stopp nur per Billing-Abschaltung oder App-seitig.

---

## 5. Chirp 3 HD – Fähigkeiten (Stand 21.10.2026)

Hauptquelle: https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (Stand 2026-10-20)

| Fähigkeit | Befund | Status laut Doku |
|---|---|---|
| **SSML** | Nur **synchrone** Requests: „SSML tags are not currently supported for streaming requests." Tags: `<speak>`, `<say-as>`, `<p>`, `<s>`, `<phoneme>`, `<sub>`, `<break>`, `<audio>`, `<prosody>`, `<voice>`; andere Tags werden ignoriert. `interpret-as="expletive"`/`"bleep"` nicht unterstützt. | **Preview** („Pre-GA Offerings Terms") |
| Widerspruch SSML | Das Release-Note vom 17.10.2025 nennt nur 5 Tags (`phoneme`, `p`, `s`, `sub`, `say-as`). Die aktuelle Seite listet 10. Die Seite „Supported voices" sagt noch: „Chirp 3: HD voices doesn't support SSML input, speaking rate and pitch-audio parameters, and the A-Law audio encoding." (veraltet bzw. widersprüchlich). Der Jarvis-Code-Ansatz „Chirp kann kein SSML" ist **überholt**, SSML ist aber nur Preview. | – |
| **`markup`-Feld, Pausen** | `[pause short]`, `[pause]`, `[pause long]`; „pause tags will only work in the `markup` field, and not in the `text` field." `markup` ist laut Referenz „for HD voices specifically". Nicht unterstützt in bg-bg, cs-cz, el-gr, et-ee, he-il, hr-hr, hu-hu, lt-lt, lv-lv, pa-in, ro-ro, sk-sk, sl-si, sr-rs, yue-hk. **de-DE ist nicht ausgeschlossen.** | Abschnitt „voice controls" Preview |
| **Custom Pronunciations** | IPA und X-SAMPA, JSON-Feld `custom_pronunciations`. Verfügbar „across all locales except" bg-bg, bn-in, cs-cz, da-dk, el-gr, et-ee, fi-fi, gu-in, he-il, hr-hr, hu-hu, lt-lt, lv-lv, nb-no, nl-be, pa-in, ro-ro, sk-sk, sl-si, sr-rs, sv-se, sw-ke, th-th, uk-ua, ur-in, vi-vn, yue-hk. **de-DE ist erlaubt.** Die REST-Referenz von `SynthesisInput.customPronunciations` sagt veraltet „initial support is for en-us". | Preview (Teil „voice controls") |
| **speaking_rate** | 0,25–2,0 (Standard 1,0). | Preview (Teil „voice controls") |
| **pitch** | Kein Pitch-Parameter für Chirp 3 HD. `<prosody pitch>` nur über SSML (Preview, synchron). | – |
| **Streaming** | Bidirektional („send text input and receive audio data simultaneously"), nur mit Chirp 3: HD kompatibel, Konfigurationsrequest zuerst. Formate Streaming: ALAW, MULAW, OGG_OPUS, PCM. Batch: ALAW, MULAW, MP3, OGG_OPUS, PCM (Standard LINEAR16). Die Streaming-Doku (`create-audio-text-streaming`) markiert das Feature als **Preview**. Transport (gRPC/REST) nicht benannt, nur Client-Bibliotheksbeispiel. | Preview |
| **Textnormalisierung Deutsch** | **Nicht belegt.** Weder chirp3-hd- noch SSML-Seite enthalten einen Abschnitt zur Normalisierung. Die SSML-Seite beschreibt `<say-as>` mit `interpret-as` (cardinal, ordinal, characters, verbatim/spell-out, date, time, duration, currency, telephone, fraction, unit); Deutsch wird dort nicht erwähnt. Beispiele der Doku sind englisch. `digits` ist nicht dokumentiert. Ob Chirp 3 HD ohne SSML „1.249,99 €", „21.10.2026", „18:47 Uhr", „3. Nov.", „z. B." korrekt ausspricht, ist **nicht belegt**; eine unabhängige Quelle dazu wurde nicht gefunden. | – |
| **Prompt/Anweisungen** | `prompt`-Feld: „supported only for controllable/promptable voice models" (also nicht für Chirp 3 HD). | – |

### 5.1 Prüfung der Testtext-Phänomene gegen die Doku

Testtext: `C:\Users\barwa\proggs\Recherchen\tts-vergleich\testtext.md`

| Phänomen | Was die Doku hergibt | Bewertung |
|---|---|---|
| 21.10.2026 (Datum) | `<say-as interpret-as="date" format="dmy">` dokumentiert (Beispiel englisch); nur synchron, Preview. Ohne SSML: Verhalten unbelegt. | Normalisierung offen, Hörprobe nötig |
| 18:47 Uhr | `interpret-as="time"` mit `format` (h, m, s, 12/24); Beispiel englisch. | offen |
| 30,5 ms / 2,3 % / 12,5 km / 14 °C | `interpret-as="unit"` und `cardinal` dokumentiert, aber Beispiele englisch; Dezimalkomma nicht erwähnt. | offen |
| 1.249,99 € | `interpret-as="currency"` mit `language`-Attribut; Beispiel `$42.01`. Deutsches Tausenderpunkt/Dezimalkomma-Format nicht dokumentiert. | offen, hohes Risiko |
| 3. Nov. | `interpret-as="ordinal"`; Abkürzung Nov. wäre mit `<sub alias="November">` lösbar. | offen |
| ca. / z. B. / Dr. | `<sub alias="…">` unterstützt (SSML-Tag in der Liste). | per SSML lösbar |
| HRV / WLAN / USB | `interpret-as="characters"` bzw. `verbatim` dokumentiert; „WLAN" als „Weh-Lan" per `<phoneme>` oder `custom_pronunciations`. | per SSML/Pronunciation lösbar |
| Update, App, Meeting, Office, gecancelt | `custom_pronunciations` (IPA/X-SAMPA, de-DE erlaubt) oder `<phoneme>`. | lösbar |
| – ? ! ' (Gedankenstrich, Intonation) | Dokumentiert ist nur `[pause]` im `markup` (und `<break>` in SSML). Die Aussage „Interpunktion steuert Betonung" stammt aus dem bestehenden Best-Practice-Dokument, in dieser Sitzung nicht neu belegt. | Hörprobe nötig |
| **Testtext B** (Satzzeichen wörtlich) | Chirp 3 HD hat keine Anweisungsfunktion (`prompt` nur für promptable Modelle), SSML kennt kein „Satzzeichen vorlesen". Nur durch vorheriges Ausschreiben („Komma", „Ausrufezeichen") im Text erreichbar. | nicht per Dienst steuerbar |

Die Mehrzahl der Fälle lässt sich mit SSML (Preview, synchron, 5000 Byte, Tags zählen als Zeichen) lösen oder durch **Vorverarbeitung im eigenen Code** (Zahlen/Datum/Währung ausschreiben).

### 5.2 Lizenz und kommerzielle Nutzung der Audioausgabe

- Quotenseite, wörtlich: „You can use the audio data files you create using Cloud Text-to-Speech in your applications or media in compliance with the Google Cloud Platform Terms of Service including compliance with all applicable law." Die Seite erwähnt „commercial use" selbst nicht. Quelle: https://docs.cloud.google.com/text-to-speech/quotas (21.10.2026)
- Keine Namensnennungspflicht (Moderator im Google-Entwicklerforum, sekundär): https://discuss.google.dev/t/commercial-use/128101
- Ein Forenbeitrag behauptet, die Nutzungsbedingungen verböten Training eines konkurrierenden TTS-Systems mit der Ausgabe (sekundär, nicht aus den Bedingungen selbst bestätigt): https://discuss.google.dev/t/text-to-speech-api-license/187973
- Abschnitt „Cloud Text-to-Speech" in den Service Specific Terms (https://cloud.google.com/terms/service-terms) wurde in den gelesenen Teilen (Zeichen 0–100 000, 200 000+) nicht gefunden; **nicht belegt**.

### 5.3 Datenschutz, Datenverarbeitung, EU-Endpunkt

| Punkt | Befund | Quelle |
|---|---|---|
| Logging | „Google does not log any customer Cloud TTS text or audio data." und „Cloud Text-to-Speech is both stateless and resourceless". Aufbewahrungsdauer nicht genannt. Ob Kundeninhalte zum Training genutzt werden: die Seite sagt es **nicht**. | https://docs.cloud.google.com/text-to-speech/docs/data-logging (21.10.2026) |
| EU-Endpunkt | Host `https://eu-texttospeech.googleapis.com`, Pfad `/v1/text:synthesize`, Header `x-goog-user-project`; „include the matching `eu` location in the `parent` string" (Beispielbody zeigt kein `parent`-Feld). Endpunkte-Seite: „your data at-rest and in-use stay within the regional or continental boundaries of Europe or the USA". | https://docs.cloud.google.com/text-to-speech/docs/endpoints |
| **Datenresidenz bei Chirp 3 HD** | **Widerspruch zweier offizieller Seiten:** Die Seite „Supported voices" sagt wörtlich: „It's available in the `global`, `eu`, and `us` endpoints but is out of scope for regionalization and data residency." Die Endpunkte-Seite listet Chirp 3 HD unter EU-Endpunkten und enthält kein Wort „out of scope". Für harte Residenzanforderungen gilt die strengere Aussage. Der Almanach-Eintrag GA17 stützt sich auf diese Aussage und ist **bestätigt** (Stand 21.10.2026). | list-voices-and-types, endpoints |
| **Preview-Funktionen und Datenverarbeitungsvertrag** | Service Terms Abschnitt 5: Pre-GA-Angebote sind „not covered by any SLA or Google indemnity"; „no data processing terms (including the Cloud Data Processing Addendum) apply to Pre-GA Offerings and Customer should not use Pre-GA Offerings to process personal data or other data subject to legal or regulatory compliance requirements." 5a stellt klar, dass ein Pre-GA-Bauteil in einem GA-Dienst „unrelated commitments" des Dienstes nicht aufhebt. **Auslegung (nicht rechtlich geprüft):** Wer SSML, `markup`/Pausen, Pace oder Custom Pronunciations bei Chirp 3 HD nutzt, nutzt Funktionen, die die Doku als Preview kennzeichnet. Jarvis spricht Gesundheitswerte (HRV, Schlaf) und nutzt Custom Pronunciations. Prüfenswert. | https://cloud.google.com/terms/service-terms (Abschnitt 5, gelesen am 21.10.2026) |

### 5.4 SLA

- Aktuelle SLA-Seite https://cloud.google.com/text-to-speech/sla (WebFetch liefert nur den Titel; Inhalt aus WebSearch-Auszug): Service Level Objective **99,9 %** monatlich. Gutschriften: 10 % bei 99–<99,9 %, 25 % bei 95–<99 %, 50 % bei <95 %. Summe pro Monat auf 50 % gedeckelt, Gutschrift binnen 60 Tagen nach Anforderung. (belegt, Auszug)
- Archivierte Fassung von 2018 (https://cloud.google.com/text-to-speech/sla-20181017, gelesen): „Downtime means more than a 5% Error Rate." Ausgeschlossen: Features „designated Alpha or Beta", in der Doku vom SLA ausgenommene Features, Fehler durch Quoten.
- Preview-Funktionen sind durch Service Terms 5b ausdrücklich nicht von einer SLA gedeckt.

---

## 6. Neu seit Anfang 2026 (Release Notes)

Quelle: https://docs.cloud.google.com/text-to-speech/docs/release-notes (21.10.2026, Fußzeile „Last updated 2026-10-20 UTC")

**Überraschung:** Die offizielle Release-Notes-Seite enthält **keinen einzigen Eintrag aus 2026**. Der neueste Eintrag ist vom 30.12.2025. Gleichzeitig nennt die Quotenseite (Stand 2026-10-20) bereits `gemini-3.1-flash-tts-preview`, und Artificial Analysis listet Gemini 3.8 Flash TTS. Die Release Notes hinken der Realität also hinterher oder sind unvollständig. Für Chirp 3 HD ist offiziell **nichts Neues seit 2026** belegt.

Letzte Einträge (alle 2025):

| Datum | Eintrag |
|---|---|
| 30.12.2025 | Chirp 3 HD: Preview für Chinesisch (Hongkong) `yue-HK` |
| 12.12.2025 | Chirp 3 HD: Preview für Panjabi `pa-IN` |
| 11.12.2025 | Gemini-TTS in weiteren Regionen |
| 10.11.2025 | Chirp 3 HD: 13 weitere Sprachen (u. a. Bulgarisch, Tschechisch, Griechisch, Ungarisch, Rumänisch) |
| 21.10.2025 | Instant Custom Voice: Schlüsselerzeugung in `eu` und `us` |
| 17.10.2025 | **Chirp 3 HD unterstützt SSML-Eingabe** (5 Tags) |
| 15.09.2025 | Chirp 3 HD auf `asia-northeast1` |
| 27.08.2025 | Chirp 3 HD auf `europe-west2`; Instant Custom Voice mit Pace, Pause, Pronunciations |
| 24.07.2025 | Chirp 3 HD GA für Dänisch, Finnisch, Norwegisch, Schwedisch |

Drittquellen nennen widersprüchliche 2026-Termine für „Chirp 3 HD GA" (Wiki: Februar 2026; Blog: März 2026). Die offiziellen Release Notes widerlegen das (Chirp 3 HD war 2025 GA); Drittangaben **nicht belastbar**.

---

## 7. Unabhängige Qualitätsbelege

| Quelle | Befund | Belastbarkeit |
|---|---|---|
| Artificial Analysis, Provider Voice Leaderboard (englisch-orientiert, Filter nur Akzent US/UK und Kategorie) https://artificialanalysis.ai/text-to-speech/leaderboard (21.10.2026) | **Chirp 3: HD Elo 1061, Rang 60** (8 Stimmen, $30 je 1 Mio Zeichen). Zum Vergleich: Gemini 3.8 Flash TTS Rang 5 (1278), Gemini 3.8 Flash-Lite Rang 8 (1251), Gemini 3.1 Flash TTS Rang 16 (1213), Studio Rang 51 (1077), WaveNet Rang 89 (918), Neural2 Rang 93 (894), Standard Rang 95 (886). Spitze: Eleven v4 Turbo (1335). Modellseite zeigt 1061,02; ein anderer Auszug 1055,66 (anderer Stichtag). | belastbar für Gesamt-/Englisch-Elo, **nicht** für Deutsch |
| Artificial Analysis, Multilingual Arena (angekündigt per X, Sept. 2026) | Neun Sprachen, darunter Deutsch; laut Auszug Cartesia Sonic 3.6 auf Platz 1 für Deutsch, Inworld TTS-2 Platz 2 mit 1261 Elo (22 Punkte hinter Sonic 3.6). Ein Elo-Wert für Chirp 3 HD in Deutsch konnte nicht abgerufen werden (X liefert 402, deutsche Leaderboard-URL liefert 404). | **für Chirp 3 HD Deutsch: keine belastbare Quelle** |
| Unabhängige Hörtests/Fachartikel zu Chirp 3 HD Deutsch | Gesucht (Deutsch und Englisch): nur Google-eigene Doku gefunden. | **keine belastbare Quelle** |
| Deutsche Textnormalisierung von Chirp 3 HD | Kein Forum-/Artikelbeleg gefunden. | **nicht belegt** |

---

## 8. Delta zum bestehenden Wissensstand (`best-practices/apis/tts-provider.md`, `bugs/apis/tts-provider.md`)

| Punkt | Almanach/Best Practice | Stand 21.10.2026 |
|---|---|---|
| Free-Tier Chirp 3 HD 1 Mio, $30/Mio | ja | **bestätigt** |
| WaveNet/Standard 4 Mio frei, $4 | ja | Preisseite bestätigt; ein Abrechnungs-Screenshot zeigt WaveNet 1 Mio (Widerspruch offen) |
| Limits 5000 Bytes, 200 RPM, 100 Streams | ja | **bestätigt**, plus neue Werte (Studio 500, Cloning 30/10) |
| Umlaut 2 Bytes im Limit | ja | bestätigt (UTF-8). **Neu:** Abrechnung zählt Zeichen, nicht Bytes (Auszug Preisseite) |
| SSML nur synchron, 10 Tags, Preview | ja | **bestätigt**; zusätzlich Widerspruch in der Doku (list-voices „kein SSML"; Release Note nennt 5 Tags) |
| Pitch nicht vorhanden, speaking_rate 0,25–2,0 | ja | bestätigt |
| Pause/Pronunciation: de-DE erlaubt | ja | bestätigt (Ausschlusslisten identisch) |
| EU-Endpunkt ohne Datenresidenz für Chirp 3 HD (GA17) | ja | **bestätigt** auf „Supported voices", **widersprochen** durch die Endpunkte-Seite |
| Regionen | global, us, eu, asia-southeast1, asia-northeast1, europe-west2 | bestätigt; Endpunkte-Seite nennt zusätzlich Frankfurt/Eemshaven (Widerspruch) |
| Budget nur Warnung | ja (GA15 Budget-Alerts) | **neu:** Spend-Cap-Budgets existieren, TTS nicht eingeschlossen |
| Preview und Datenverarbeitungsvertrag | nicht erfasst | **neu:** Pre-GA-Features sind vom DPA und von der SLA ausgenommen (Service Terms Abschnitt 5) |
| Streaming-Status | – | Streaming-Doku sagt **Preview** |
| Release Notes 2026 | – | **keine** Einträge für 2026 |
| Instant Custom Voice | – | Allowlist, $60/Mio, kein Freikontingent |

---

## Franks „großes Google-Freikontingent" – Einordnung

- Für **Chirp 3 HD: 1 Mio Zeichen pro Monat**, danach $30 je 1 Mio. Das ist das Kontingent für die aktuelle Jarvis-Stimme. Es ist kein 300-Dollar-Guthaben und keine Aktion, sondern ein monatlich erneuertes Kontingent (Rabatt, verfällt am Monatsende), setzt aber ein **aktives Abrechnungskonto** voraus.
- „Groß" trifft auf **Standard (4 Mio) und WaveNet (4 Mio, Streit um 1 Mio)** zu, nicht auf Chirp 3 HD.
- Zusätzlich gibt es, falls noch nicht genutzt, das einmalige 300-$-Guthaben (90 Tage, neues Konto, Zahlungsmittel nötig). Ob es für TTS gilt, nennt die Seite nicht.
- Die Kontingente der Klassen laufen laut Drittbeobachtung je SKU getrennt (nicht offiziell bestätigt).

---

BEST-PRACTICES-KANDIDATEN:

- Chirp 3 HD: Umlaute zählen in der Abrechnung als 1 Zeichen, im 5000-Byte-Limit als 2 Bytes. Budget nach Zeichen planen, Chunking nach Bytes. Quelle: https://cloud.google.com/text-to-speech/pricing (Auszug 21.10.2026, Seite aktualisiert ca. 25.09.2026) und https://docs.cloud.google.com/text-to-speech/quotas (Stand 2026-10-20)
- Für Cloud TTS gibt es keine native Monatskappung: Spend-Cap-Budgets gelten nur für Gemini API, Agent Platform, Cloud Run und Cloud Run functions. Bei Bedarf Budget-Alert plus eigener Zeichenzähler in App/Proxy. Quelle: https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps (21.10.2026) und https://docs.cloud.google.com/billing/docs/how-to/budgets
- Preview-Funktionen von Chirp 3 HD (SSML, `markup`-Pausen, Pace, Custom Pronunciations, Streaming) fallen unter die Pre-GA-Bedingungen: keine SLA, kein Datenverarbeitungsvertrag. Bei personenbezogenen Texten (Gesundheitsdaten) prüfen. Quelle: https://cloud.google.com/terms/service-terms Abschnitt 5 (21.10.2026) und https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd
- Datenresidenz: Chirp 3 HD ist laut „Supported voices" für Regionalisierung und Datenresidenz nicht abgedeckt, die Endpunkte-Seite widerspricht. Quelle: https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types und https://docs.cloud.google.com/text-to-speech/docs/endpoints (21.10.2026)
- Doku-Widersprüche kennen: „Supported voices" sagt „Chirp 3 HD unterstützt kein SSML/speaking rate", die chirp3-hd-Seite sagt das Gegenteil (aktueller). Immer die chirp3-hd-Seite maßgeblich nehmen. Quelle: beide Seiten, Stand 2026-10-20

BUG-KANDIDATEN:

- Doku-Widerspruch Datenresidenz Chirp 3 HD (EU-Endpunkt `eu-texttospeech.googleapis.com`): „Supported voices" nennt Chirp 3 HD „out of scope for regionalization and data residency", die Endpunkte-Seite listet Chirp 3 HD unter EU-Endpunkten ohne Einschränkung. Version: Doku Stand 2026-10-20. Quellen: https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types und https://docs.cloud.google.com/text-to-speech/docs/endpoints
- Doku-Widerspruch SSML Chirp 3 HD: Release Note 17.10.2025 nennt 5 Tags, aktuelle chirp3-hd-Seite 10 Tags, „Supported voices" sagt weiterhin kein SSML. Version: Doku Stand 2026-10-20. Quellen: https://docs.cloud.google.com/text-to-speech/docs/release-notes, https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd, https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types
- Preisseite: WaveNet- und Standard-Zeile tragen im Auszug dieselbe SKU 9D01-5995-B545; WaveNet-Freikontingent 4 Mio laut Preisseite gegen 1 Mio in einem Abrechnungs-Screenshot (dev.to, 09/2026). Version: Preisseite Stand ca. 25.09.2026. Quellen: https://cloud.google.com/text-to-speech/pricing, https://dev.to/just_a_side_project/i-checked-googles-own-billing-console-so-you-dont-have-to-the-real-text-to-speech-free-tier-2l0m
- Release-Notes-Seite ohne Einträge für 2026, obwohl die Quotenseite 2026er Gemini-Modelle nennt. Version: Stand 2026-10-20. Quellen: https://docs.cloud.google.com/text-to-speech/docs/release-notes, https://docs.cloud.google.com/text-to-speech/quotas
