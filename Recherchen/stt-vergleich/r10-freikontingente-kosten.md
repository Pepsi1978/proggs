# R10 — Freikontingente und Kosten aller STT-Angebote, umgerechnet in Audio-Stunden pro Monat

Stand: 21.10.2026. Researcher R10 der STT-Vergleichsstudie. Basislinie: Groq `whisper-large-v3-turbo` (kostenlos: 20 RPM, 2.000 RPD, 7.200 Audio-Sekunden/Stunde, 28.800 Audio-Sekunden/Tag).
Methode: Preis- und Kontingentseiten direkt abgerufen (Seitentext per curl bzw. WebFetch, nicht aus Suchzusammenfassungen), Zahlen nachgerechnet. Aussagen aus anderen Reports der Studie sind mit „(R2)“ bis „(R9)“ markiert. Was ich nicht belegen konnte, steht als **unbelegt**. „Monat“ heißt hier 30 Tage; „Stunde“ heißt Audiostunde, nicht Rechenzeit.

---

## 1. Kurzfazit (die wichtigsten Funde)

1. **Groq selbst ist unschlagbar großzügig:** 28.800 Audio-Sekunden/Tag sind **8 Stunden pro Tag = 240 Stunden pro Monat** kostenlos. Franks „Intensiv“-Profil (2 h/Tag) nutzt davon 25 Prozent. Das Kontingent ist also für Franks Nutzung nicht der Engpass, sondern die Qualität.
2. **Es gibt kein dauerhaftes, monatlich erneuertes Gratis-Kontingent eines Modells, das belegbar besser ist als Turbo, in Größenordnung Groq.** Die einzigen Dauerkontingente in Stundengröße sind Groq (240 h/Monat) und Cloudflare (107 h/Monat, aber dasselbe Modell `whisper-large-v3-turbo`, also keine Verbesserung). Alles andere Bessere ist entweder ein **einmaliges Startguthaben** (Deepgram 200 $, Speechmatics 100 $, AssemblyAI 50 $, Gladia 50 €, Google Cloud 300 $) oder ein **Free Tier mit unveröffentlichten oder winzigen Limits** (Gemini, Mistral).
3. **Größte Stundenreserve für ein belastbar besseres Modell:** AssemblyAI Universal-3.5 Pro (in R8 belastbar besser als Turbo) mit **50 $ einmalig, ohne Karte, ohne Ablauf = ca. 185 bis 238 Stunden**. Danach Deepgram Nova-3 mit 200 $ = ca. 640 bis 775 Stunden (Deutsch-Qualität gegenüber Turbo nur teilweise belegt, R5/R8). Bei Franks „Mittel“-Profil (15 h/Monat) reicht allein das AssemblyAI-Guthaben **etwa 12 bis 16 Monate**, bei „Intensiv“ (60 h/Monat) etwa 3 bis 4 Monate.
4. **Voxtral 3 (Mistral) ist ganz neu (20.10.2026) und mit 0,21 bis 0,24 $/h billig**, aber ob das Mistral-Free-Mode Audio abdeckt, ist unbelegt. Ein einziger Testaufruf mit einem Free-Key klärt das.
5. **Gemini 3.5 Transcribe ist laut Preisseite im Free Tier „Free of charge“, aber die Limits stehen nur im AI-Studio-Dashboard.** Indizien: Forum meldet 100 RPD selbst für zahlende Tier-1/Tier-2-Konten, eine Eigenmessung (R3) lief nach ca. 12 Aufrufen in wenigen Minuten in 429. Planungsgrundlage ist das also nicht.
6. **Abrechnungs-Falle bei Speechmatics:** Die Preisseite zeigt „ab 0,12 $/h“, das ist der Bestfall (Listenpreis Melia 1 = 0,24 $/h, minus 33 % Trainings-Opt-in, minus bis zu 25 % Mengenrabatt). Für Frank gilt realistisch 0,24 $/h (Melia 1) bzw. 0,75 $/h (Enhanced). Rekonstruiert aus dem schema.org-Preis der Seite (0,24 $) und den beiden Rabatten (0,24 × 0,67 × 0,75 ≈ 0,12; Enhanced 0,75 × 0,67 × 0,75 ≈ 0,38) sowie R5; der Tooltip „How the $0.12 per hour rate is reached“ wurde nicht gelesen. https://www.speechmatics.com/pricing

---

## 2. Rechengrundlagen und Annahmen

- **Audiostunde je Monat** = Gratis-Menge je Zeiteinheit × Tage. Groq: 28.800 s/Tag ÷ 3.600 = 8 h/Tag × 30 = **240 h**. Stundenlimit 7.200 s/h = 2 h pro Uhrzeit-Stunde (bindet nur bei Dauerlast).
- **Profile** (30 Tage): Wenig 10 min/Tag = **5 h/Monat**; Mittel 30 min/Tag = **15 h/Monat**; Intensiv 120 min/Tag = **60 h/Monat**.
- **Clip-Annahme für Anfragen-Limits:** Diktat besteht aus Einzelaufnahmen. Als Richtwert 15 s je Clip: Wenig 40 Clips/Tag, Mittel 120 Clips/Tag, Intensiv 480 Clips/Tag (Annahme, nicht gemessen).
- **Pausen:** Abgerechnet wird die **gesendete** Audiolänge, nicht die gesprochene. Wer die Aufnahme nicht an der Taste stoppt (oder Stille nicht abschneidet), zahlt auch für Pausen. Alle Profile oben sind als **gesendete Audiozeit** gemeint.
- Preise in USD, außer Gladia (EUR). Keine Steuern. Preise der Preisseiten vom 21.10.2026.
- Stunden aus Guthaben = Guthaben ÷ Stundenpreis des jeweils genannten Modells.

---

## 3. Die große Freikontingent-Tabelle

### 3.1 Dauerhafte oder laufend erneuerte Kontingente

| Anbieter / Modell | Art | Genaue Menge | In Audio-h/Monat | Karte nötig? | Kommerziell erlaubt? | Training mit Free-Tier-Daten? | Beim Überschreiten | Quelle |
|---|---|---|---|---|---|---|---|---|
| **Groq** whisper-large-v3-turbo und -large-v3 | täglich + stündlich (Free Plan) | 20 RPM, 2.000 RPD, 7.200 Audio-s/h, 28.800 Audio-s/Tag | **240 h** (8 h/Tag) | Free: nein (Upgrade auf Developer verlangt Zahlungsmittel) | nicht geprüft (Groq-Nutzungsbedingungen nicht gelesen) | Inferenzanfragen werden standardmäßig nicht gespeichert; Training im Free Plan nicht ausdrücklich geprüft | **harte Sperre**, HTTP 429 mit `retry-after`; keine Abrechnung im Free Plan | https://console.groq.com/docs/rate-limits , https://console.groq.com/docs/your-data , https://community.groq.com/t/do-i-get-charged-for-anything-on-the-groq-free-plan/832 |
| **Cloudflare Workers AI** `@cf/openai/whisper-large-v3-turbo` (dasselbe Modell wie Groq) | täglich (00:00 UTC Reset) | 10.000 Neurons/Tag; Turbo = 46,63 Neurons je Audio-Minute | 10.000 ÷ 46,63 = 214 min/Tag = 3,57 h/Tag = **107 h** | Konto nötig; Free-Plan ohne Zahlungsmittel | nicht geprüft | nicht geprüft | **harte Sperre** („further operations will fail with an error“); Free-Plan hat keinen Pay-as-you-go-Weg, nur Wechsel auf Workers Paid (0,011 $ je 1.000 Neurons über den 10.000) | https://developers.cloudflare.com/workers-ai/platform/pricing/ |
| Cloudflare `@cf/deepgram/nova-3` (Batch-Eingabe) | täglich, selber Topf | 472,73 Neurons/Min (WebSocket 836,36) | 21 min/Tag = **10,6 h** (Streaming 12 min/Tag = 6 h) | s. o. | nicht geprüft | nicht geprüft | wie oben | dieselbe Seite |
| **Azure Speech F0** (nur Echtzeit-Standard und Custom, nicht Batch/Fast/LLM/MAI, laut R4) | monatlich | 5 Audiostunden | **5 h** | Azure-Abo nötig (Karte) | nicht geprüft | Echtzeit-STT speichert nichts (R4) | Verhalten bei Überschreitung auf der Preisseite nicht beschrieben: **unbelegt** (R4: F0 nur 1 gleichzeitige Anfrage) | https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/ , R4 |
| **OpenAI** `whisper-1` (large-v2, wird am 26.02.2027 abgeschaltet) | täglich (Rate-Limit-Zeile Free) | 3 RPM, 200 RPD | bei 15-s-Clips 200 × 15 s = 50 min/Tag ≈ **25 h** (rechnerisch, Annahme) | nein, Konto in unterstützter Region | nach Nutzungsbedingungen ja (nicht geprüft) | unbelegt | harte Sperre 429 | R2: https://developers.openai.com/api/docs/models/whisper-1 |
| OpenAI `gpt-transcribe`, `gpt-4o-*` | **kein Freikontingent**: Free-Zeile fehlt bei allen außer whisper-1 | – | 0 | – | – | – | Konto muss mind. 5 $ aufladen (Sekundärquelle) | R2: https://developers.openai.com/api/docs/guides/rate-limits |
| **Gemini API** `gemini-3.5-transcribe` (und `-live`) | Free Tier ohne feste veröffentlichte Menge | Preisseite: Ein- und Ausgabe „Free of charge“; **RPM/RPD/TPM nirgends veröffentlicht** (nur im AI-Studio-Dashboard) | **unbekannt**; Forum: Tier-1/2 werden auf 100 RPD gedrückt, obwohl 10.000 RPD angezeigt werden; Eigenmessung (R3): 429 nach ca. 12 Aufrufen | nein (Free), Prepay ab 5 $ führt in Paid | nicht geprüft; EWR-Klausel (R3, Bedingungen Stand 28.04.2026) verlangt Paid Services für Apps mit EWR-Nutzern | **Ja**: „Used to improve our products: Yes“ im Free Tier (Preisseite); im EWR/CH/UK greifen laut Bedingungen die Paid-Datenregeln auch im Free Tier (R3) | 429 `RESOURCE_EXHAUSTED`, Reset RPD um Mitternacht Pazifikzeit | https://ai.google.dev/gemini-api/docs/pricing , https://ai.google.dev/gemini-api/docs/rate-limits , https://discuss.ai.google.dev/t/tier-2-gemini-3-5-transcribe-shows-10-000-rpd-but-api-enforces-100-rpd/183223 |
| Gemini `gemini-3.5-flash-lite` und `gemini-3.1-flash-lite` (Audio-Eingabe, LLM, kein reines STT) | täglich + pro Minute (Drittmessung, nicht von Google garantiert) | **15 RPM, 500 RPD** (gemessen am 02.09.2026 über das `quotaValue` im 429-Fehler, frisches Projekt ohne Billing) | bei 15-s-Clips 500 × 15 s = 125 min/Tag ≈ **62 h** (rechnerisch; TPM unbekannt) | nein | s. o. | Ja (Free Tier), EWR-Klausel wie oben | 429 | https://dev.to/romeroyang/geminis-free-tier-measured-20-requests-a-day-and-google-no-longer-publishes-the-number-4gf2 |
| Gemini `gemini-3.5-flash` / 3.6 / 3.7 (Flash) | täglich | **5 RPM, 20 RPD** (dieselbe Messung) | ≈ 1,3 h/Monat bei 15-s-Clips (20 × 15 s × 30) | nein | s. o. | Ja | 429 | dieselbe dev.to-Quelle |
| **ElevenLabs** Scribe v2 (Free-Plan, Credits-Pool) | monatlich, ein Credit-Topf für alle Produkte | 10.000 Credits/Monat; Speech-to-Text-Seite nennt für Free **12 Minuten**; Preisseite rechnet 330 Credits/Min (≈ 30 min) – die beiden Angaben widersprechen sich | **0,2 bis 0,5 h** | nein | **nein** (Free-Plan ohne kommerzielle Lizenz laut R6) | unbelegt (R6) | Credits aus: Sperre bis zum nächsten Zyklus bzw. Upgrade (genaues Verhalten für API: unbelegt) | https://elevenlabs.io/speech-to-text , https://elevenlabs.io/pricing |
| **Google Cloud Speech-to-Text** V1 (nicht Chirp 3) | monatlich, „Always Free“ | 60 Minuten/Monat | **1 h** | Konto mit Karte (Cloud-Billing) | ja nach Cloud-Bedingungen (nicht geprüft) | mit Data Logging gelten günstigere V1-Preise; Training: nicht geprüft | danach V1 0,016 $/min (mit Logging) bzw. 0,024 $/min (ohne) | https://cloud.google.com/speech-to-text/pricing , https://cloud.google.com/free/docs/free-cloud-features |
| Google Cloud STT **V2 (inkl. Chirp 3)** | **kein** Freikontingent in der V2-Preistabelle | – | **0** | – | – | – | 0,016 $/min ab Minute 1 | https://cloud.google.com/speech-to-text/pricing (R3 bestätigt) |
| **Amazon Transcribe** | 12-Monats-Free-Tier (Preisseite) | 60 Minuten/Monat, 12 Monate ab erster Anfrage, kein Übertrag | **1 h** (12 Monate) | ja (AWS-Konto) | ja (nicht geprüft) | nicht geprüft | danach normaler Preis. **Widerspruch:** R4 meldet, dass AWS-Neukonten seit Juli 2025 statt dieser Kontingente einen Credit-„Free Plan“ erhalten und Transcribe dort nicht gelistet ist; für Neukonten also **unbelegt** | https://aws.amazon.com/transcribe/pricing/ , R4 |

### 3.2 Einmalige Startguthaben (nicht erneuert)

Spalte „Stunden“ = Guthaben ÷ Stundenpreis des genannten Modells (siehe Abschnitt 6).

| Anbieter | Menge | Stunden (Modell) | Gültigkeit | Karte? | Kommerziell? | Training im Free Tier? | Beim Aufbrauchen | Quelle |
|---|---|---|---|---|---|---|---|---|
| **AssemblyAI** | 50 $ einmalig (Pre-recorded, Realtime, Voice Agent, Speech Understanding; nicht LLM Gateway) | Universal-3.5 Pro (0,21 $/h): **238 h**; Universal-2 (0,15 $/h): 333 h; Sync-API (0,30 $/h): 167 h. Preisseiten-FAQ nennt „bis zu 185 h“ Pre-recorded und 333 h Streaming (passt rechnerisch nicht genau zu den heutigen Preisen) | **läuft nicht ab**, wird bei Upgrade mitgenommen | Anmeldung ohne Karte (Preisseiten-FAQ); danach Karte | nicht geprüft | unbelegt | API wird pausiert, bis Karte/Guthaben hinterlegt (kein Überlauf); Free: 5 parallele Async-Jobs, 5 neue Streams/Minute | https://www.assemblyai.com/pricing , https://www.assemblyai.com/docs/billing-and-pricing.md |
| **Deepgram** | 200 $ einmalig, „Pay As You Go: No minimums. No expiration. No credit card required.“ | Nova-3 monolingual Batch (0,0043 $/min = 0,258 $/h): **775 h**; Nova-3 multilingual (0,0052 $/min = 0,312 $/h): **641 h**. Welche Variante für `language=de` gilt: unbelegt | laut Preisseite ohne Ablauf (eine Drittquelle sagt dasselbe; Startup-Programm-Guthaben dagegen 12 Monate) | nein | nicht geprüft | unbelegt (R5) | danach Pay-as-you-go; Verhalten bei fehlender Zahlungsart nicht beschrieben | https://deepgram.com/pricing |
| **Speechmatics** | 100 $ einmalig für Neukonten (Bestandskonten vor 01.08.2026: 25 $ Übergangsguthaben) | Melia 1 (Listenpreis 0,24 $/h): **417 h**; Enhanced (0,75 $/h): **133 h**; Standard (0,46 $/h, aus 0,23 $ mit Rabatt hochgerechnet, unbelegt): ca. 217 h | **12 Monate** („Sign-up bonus … last 12 months“, Preisseiten-FAQ) | nein | nicht geprüft | **Kein** Training standardmäßig; Trainings-Opt-in gibt 33 % Rabatt | Free-Plan: API pausiert bei leerem Guthaben; Pro: Karte, Abrechnung am 1. des Folgemonats | https://www.speechmatics.com/pricing , https://docs.speechmatics.com/administration/plans |
| **Gladia** | 50 € einmalig | Solaria-3 (0,16 €/h): **312 h**; Solaria-1 (0,27 €/h): 185 h; Echtzeit (0,68 €/h): 73 h | **1 Jahr** | auf der Preisseite nicht genannt | nicht geprüft | **Ja, standardmäßig an** (Self-serve); Opt-out kostenlos im Profil | Prepaid-Wallet; Auto-Top-up optional, sonst Stopp | https://gladia.io/pricing |
| **Google Cloud** (für Chirp 3) | 300 $ Neukunden-Guthaben | bei 0,96 $/h: **312 h** (Chirp 3 steht nicht separat auf der Preisseite, wird dort unter „Standard“ geführt, R3) | **90 Tage** | **ja** | ja | nicht geprüft | Nach Ablauf: Wechsel auf Bezahlkonto nur auf eigene Zustimmung (Free-Trial-Regel, nicht geprüft) | https://cloud.google.com/free/docs/free-cloud-features |
| **Alibaba Model Studio** `qwen3-asr-flash` (Singapur/International) | 36.000 s = **10 h** | 10 h | 90 Tage ab Aktivierung, je Modell eigener Topf | unbelegt | nicht geprüft | unbelegt | danach 0,000035 $/s (0,126 $/h) | https://www.alibabacloud.com/help/en/model-studio/model-pricing (**Widerspruch:** die China-Spiegelseite https://help.aliyun.com/en/model-studio/model-pricing nennt das Gratiskontingent nur für Beijing, R6 fand „nur Beijing“; die internationale Seite listet Singapur) |
| **Mistral** (Voxtral Mini Transcribe 2, Mini 3, Small 3) | „Experiment“-/Free-Mode ohne Karte (nur Telefonverifizierung laut Suchtreffer-Zitat des Hilfeartikels; der Artikel liefert heute 404) | **Audio im Free Mode: unbelegt** (keine Doku nennt Minutenlimits; Drittseiten nennen 1 Anfrage/s, 500.000 Token/min, 1 Mrd. Token/Monat für Text) | – | nein (laut Hilfeartikel-Zitat), Telefon | unbelegt | **Ja, Anfragen im Experiment-Plan dürfen zum Training dienen** (Hilfeartikel-Zitat); Public-Preview-Modelle (Voxtral 3): Trainingsvorbehalt ohne Opt-out möglich (R6, Drittquelle) | unbekannt | https://help.mistral.ai/en/articles/455206-how-can-i-try-the-api-for-free-with-the-experiment-plan (Seite aktuell 404), R6 |
| **Fireworks** | Kein Startguthaben auf der Preisseite; Streaming-Audio-Start 2025: **2 Wochen Testphase** | – | 2 Wochen (Blogpost 2025, veraltet) | – | – | – | – | https://fireworks.ai/blog/streaming-audio-launch , https://fireworks.ai/pricing |

### 3.3 Anbieter ohne belegbares Freikontingent (Stand 21.10.2026)

| Anbieter | Befund | Quelle |
|---|---|---|
| **Soniox** | Seit 27.10.2025 **keine Startguthaben** mehr für Neuanmeldungen (vorher 200 $ ≈ 2.000 h; Grund Missbrauch). Die Apps haben ein getrenntes wöchentliches Gratis-Guthaben, nicht die API. Eine Suchzusammenfassung „300 Min. kostenlos“ ließ sich nicht bestätigen (Quickstart-Seite 404 bzw. ohne Aussage). | https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api , https://app.soniox.com/help-center/pricing/plans |
| **Together AI** | „Together AI does not currently offer free trials“; mindestens 5 $ Guthaben kaufen; Prepaid, Konto kann nicht negativ werden. | https://support.together.ai/articles/1862638756-changes-to-free-tier-and-billing-july-2025 |
| **DeepInfra** | Pricing-Seite nennt kein Startguthaben; Aggregatoren widersprechen sich (0 bis 5 $). Karte oder Prepay vor der ersten Nutzung (Aggregator). | https://deepinfra.com/pricing |
| **Hugging Face Inference Providers** | **Free-Nutzer: „Monthly Credits: None“**; PRO 2 $/Monat Guthaben. Ältere Quellen („0,10 $“) sind überholt. | https://huggingface.co/docs/inference-providers/pricing |
| **Nvidia build.nvidia.com** | Kein belegbarer Gratis-Endpunkt für ASR; Zugang als Test mit ca. 40 RPM (Community), Guthabenregel widersprüchlich (1.000 Credits laut Forum/Substack, laut Medium-Beitrag Anfang 2025 abgeschafft); kein öffentlicher PAYG-Preis. **Unbelegt.** | R4: https://forums.developer.nvidia.com/t/nvidia-nim-faq/300317 |
| **OpenAI** (außer whisper-1) | siehe 3.1; Daten-teilen-Programm („komplimentäre Tokens“) betrifft laut R2 keine Audiomodelle, **unbelegt** für `gpt-transcribe`. | R2 |
| **Lokal** (Parakeet, Whisper.net, sherpa-onnx …) | Unbegrenzt kostenlos, aber kein Server-Kontingent; Qualität und Laufzeit siehe R7. | R7 |

---

## 4. Abrechnungs-Eigenheiten (Mindestmenge, Rundung, Einheiten)

| Anbieter | Eigenheit | Auswirkung auf Diktat | Quelle |
|---|---|---|---|
| **Groq** | **Mindestens 10 Sekunden je Anfrage abgerechnet** (Minimum Billed Length), Mindestdatei 0,01 s; Whisper arbeitet intern in 30-s-Segmenten, Minimum 10 s je Segment | Clip von 4 s wird wie 10 s berechnet. Ob die 10 s auch gegen das Limit „7.200 Audio-s/Stunde“ zählen: **unbelegt** (Doku nennt nur die Abrechnung) | https://console.groq.com/docs/speech-to-text , https://console.groq.com/docs/model/whisper-large-v3-turbo |
| **Google Cloud STT** | Sekundengenau („rounded up to the nearest increment of 1 seconds“); Mehrkanal wird je Kanal berechnet; leere Antworten werden berechnet, Serverfehler nicht | kein Mindestwert; Mono-Aufnahme verwenden | https://cloud.google.com/speech-to-text/pricing |
| **Gemini API** | Tokenbasiert. `gemini-3.5-transcribe`: Preisseite rechnet **25 Audio-Token/s** (Eingabe 2 $/1 Mio.) plus **175 Text-Token/min** Ausgabe (12 $/1 Mio.) = 0,003 + 0,002 = **ca. 0,005 $/min**. Allgemeine Audio-Doku für andere Modelle: **32 Token/s**, auf 16 kbps heruntergerechnet, Mehrkanal zusammengelegt | Rechenbeispiel Flash-Lite siehe Abschnitt 6 | https://ai.google.dev/gemini-api/docs/pricing , https://ai.google.dev/gemini-api/docs/audio |
| **Azure Speech** | Abrechnung pro Audiostunde, in Sekundenschritten; Mehrkanal teurer; Echtzeit-Standard (S1) 1,00 $/h, Batch 0,18 $/h (Retail-Prices-API, Region westeurope) | F0 nur Echtzeit | https://prices.azure.com/api/retail/prices (Filter productName Azure Speech, skuName S1), https://azure.microsoft.com/en-us/pricing/details/cognitive-services/speech-services/ |
| **Amazon Transcribe** | Sekundenschritte, **kein Minimum** für Standard (Batch und Streaming); 15-s-Minimum nur bei Call-Analytics-Zusammenfassung; Mehrkanal: Gesamtdauer, nicht je Kanal | gut für Kurzclips | https://aws.amazon.com/transcribe/pricing/ |
| **AssemblyAI** | Pre-recorded auf die Sekunde genau, fehlgeschlagene Anfragen nicht berechnet; Multichannel je Kanal; Sync-API (bis 2 min/Anfrage) 0,30 $/h | Sync-API ideal für Diktat, teurer als Async | https://www.assemblyai.com/docs/billing-and-pricing.md , https://www.assemblyai.com/pricing |
| **Speechmatics** | Auf die Sekunde genau; Anzeige „ab 0,12 $/h“ ist **Bestfall nach Rabatten** | Listenpreise einplanen | https://docs.speechmatics.com/administration/plans , https://www.speechmatics.com/pricing |
| **Deepgram** | Preise je Minute; Rundung/Minimum: **unbelegt**; Streaming derzeit mit Aktionspreis (Nova-3 Mono 0,0048 $/min statt 0,0077 $) | Batch-Preis nehmen | https://deepgram.com/pricing |
| **Cloudflare** | Abrechnung in „Neurons“; Rundung je Anfrage: **unbelegt**; Preisseite 0,0005 $/Min, Modellseite 0,000513 $/Min | – | https://developers.cloudflare.com/workers-ai/platform/pricing/ , https://developers.cloudflare.com/workers-ai/models/whisper-large-v3-turbo/ |
| **Alibaba** | Pro Sekunde Eingangsaudio, Ausgabe frei | Rest der Anfrage nicht gerundet (Sekundenpreis) | https://www.alibabacloud.com/help/en/model-studio/model-pricing |
| **Mistral** | Pro Audiominute (Voxtral Mini Transcribe 2: 0,003 $/min, Mini 3: 0,0035 $/min, Small 3: 0,004 $/min); Rundung/Minimum **unbelegt** | – | https://docs.mistral.ai/models/voxtral-mini-transcribe-26-02 , https://docs.mistral.ai/models/voxtral-mini-transcribe-3-0 , https://docs.mistral.ai/models/voxtral-small-transcribe-3-0 |
| **Gladia** | Async-Satz sinkt automatisch mit Monatsvolumen (0–100 h: 0,16 €/h); Echtzeit teurer | – | https://gladia.io/pricing |

---

## 5. Kostenbremsen: Was passiert bei Überschreitung, wie begrenze ich Ausgaben?

| Anbieter | Hartes Limit im Gratisbetrieb? | Bremse bei Bezahlbetrieb | Quelle |
|---|---|---|---|
| **Groq** | Ja: 429 bei Überschreitung, nie Abbuchung im Free Plan. Ausgabenlimit gibt es **nur bei Bezahlplan** („not available on free tier“). | Monatliches Ausgabenlimit pro Organisation, Warnschwellen; bei Erreichen **HTTP 400 `blocked_api_access`**, Aufhebung am 1. des Folgemonats oder nach Anpassung; Erfassung hinkt 10 bis 15 Minuten nach (leichte Überschreitung möglich) | https://console.groq.com/docs/spend-limits |
| **Cloudflare** | Ja: Fehler bis 00:00 UTC; Free-Plan kann nie Geld kosten. | Workers Paid: 0,011 $/1.000 Neurons über 10.000/Tag; kein hartes Limit dokumentiert | https://developers.cloudflare.com/workers-ai/platform/pricing/ |
| **AssemblyAI** | Ja: Mit 0 $ Guthaben wird die API **pausiert**. | Prepaid-Guthaben mit optionalem Auto-Pay (Schwelle und Ziel frei); ohne Auto-Pay entsteht keine Überschreitung. Spend-Limits sind in der Doku nicht beschrieben. | https://www.assemblyai.com/docs/billing-and-pricing.md |
| **Speechmatics** | Ja im Free-Plan (API pausiert). | Pro: Karte, Abrechnung am 1. des Folgemonats für den Vormonat (**Postpaid, kein dokumentiertes Limit**) | https://docs.speechmatics.com/administration/plans |
| **Gladia** | Ja (Prepaid-Wallet). | Auto-Top-up optional, Benachrichtigungen bei niedrigem Guthaben. | https://gladia.io/pricing |
| **Together AI** | – | Prepaid, Konto kann nicht negativ werden; Auto-Recharge optional | https://support.together.ai/articles/1862638756-changes-to-free-tier-and-billing-july-2025 |
| **Fireworks** | – | Prepaid; monatliches Ausgabenlimit („cap usage“) und Auto Reload einstellbar | https://fireworks.ai/pricing |
| **Gemini API** | Ja (429). | Billing-Tier mit Obergrenzen: Tier 1 monatlich 250 $, Ausgabenlimit 10 $ je 10 Minuten (Tier 2: 2.000 $ / 50 $; Tier 3: 20.000+ $ / 200 $); Prepay ab 5 $ möglich (R3). Beim Spend-Limit 429 `RESOURCE_EXHAUSTED` | https://ai.google.dev/gemini-api/docs/rate-limits |
| **OpenAI** | – | Nutzungstiers mit Monatsobergrenze (Free 100 $, Build 500 $, Launch 5.000 $, Grow 200.000 $), Prepaid-Guthaben (R2) | R2: https://developers.openai.com/api/docs/guides/rate-limits |
| **Hugging Face** | Free: nichts nutzbar ohne Guthabenkauf. | Gekauftes Guthaben; Team/Enterprise: Spending Limit | https://huggingface.co/docs/inference-providers/pricing |
| **Azure, Google Cloud, AWS** | F0 bzw. Free-Trial: Verhalten bei Überschreitung auf den Preisseiten nicht beschrieben | Budget-/Quota-Funktionen der Clouds: in dieser Recherche **nicht geprüft** | – |
| **Deepgram, Soniox, ElevenLabs, Mistral, Alibaba** | – | Ausgabenlimits und Verhalten bei leerem Guthaben **unbelegt** | – |

Empfehlung für Franks Apps: Wo immer möglich **Prepaid ohne Auto-Reload** (AssemblyAI, Gladia, Together, Fireworks) oder **Free-Plan mit hartem 429** (Groq, Cloudflare) wählen. Postpaid-Konten ohne Obergrenze (Speechmatics Pro, Cloud-Konten) nur mit externem Budgetalarm.

---

## 6. Kostenrechner-Grundlage: Preis je Audiostunde nach Freikontingent

Formel für ein einzelnes Modell und einen Monat:

`Kosten = max(0, Audio-h/Monat − Gratis-h/Monat) × Preis je Stunde` bei dauerhaftem Kontingent.
Bei Startguthaben: `Monate Guthaben = Guthaben ÷ (Audio-h/Monat × Preis je Stunde)`, begrenzt durch die Gültigkeit.
Für Groq zusätzlich: `abgerechnete Sekunden = Summe aus max(10, Clip-Länge)`, relevant für das 28.800-s-Tageslimit.

### Preisliste (nur die Modelle, die für deutsches Diktat realistisch sind)

| Anbieter / Modell | Preis je Audiostunde (Batch bzw. Kurz-Clip) | Hinweis | Quelle |
|---|---|---|---|
| Groq whisper-large-v3-turbo | **0,04 $** | Free Plan 0 $ bis 240 h/Monat | https://console.groq.com/docs/model/whisper-large-v3-turbo |
| Groq whisper-large-v3 | **0,111 $** | gleiche Free-Limits (20 RPM, 2K RPD, 28,8K s/Tag) | https://console.groq.com/docs/model/whisper-large-v3 |
| Cloudflare whisper-large-v3-turbo | 0,03 $ (0,0005 $/min) | Gratis bis 107 h/Monat | https://developers.cloudflare.com/workers-ai/platform/pricing/ |
| DeepInfra whisper-large-v3-turbo | 0,012 $ (0,0002 $/min, laut Modellseite; Zahl deckt sich mit der Nemotron-Zeile, daher mit Vorsicht) | – | https://deepinfra.com/openai/whisper-large-v3-turbo |
| Together Whisper Large v3 | 0,09 $ (0,0015 $/min) | Streaming 0,21 $/h | https://www.together.ai/pricing |
| Alibaba qwen3-asr-flash | 0,126 $ (0,000035 $/s) | 10 h gratis einmalig | https://www.alibabacloud.com/help/en/model-studio/model-pricing |
| Mistral Voxtral Mini Transcribe V2 / Mini 3 / Small 3 | 0,18 $ / 0,21 $ / 0,24 $ | Mini 3 und Small 3 seit 20.10.2026, Public Preview | Mistral-Modellseiten (Abschnitt 4) |
| Soniox async / Echtzeit | 0,10 $ / 0,12 $ | kein Startguthaben | https://soniox.com/pricing |
| AssemblyAI Universal-2 / Universal-3.5 Pro (Pre-recorded) / Sync-API | 0,15 $ / 0,21 $ / 0,30 $ | Keyterms +0,05 $/h bei U3.5 Pro | https://www.assemblyai.com/pricing |
| Deepgram Nova-3 Batch monolingual / multilingual | 0,258 $ / 0,312 $ | Whisper Large (Deepgram-gehostet) 0,288 $/h | https://deepgram.com/pricing |
| Speechmatics Melia 1 / Enhanced (Listenpreis, siehe 1.6) | 0,24 $ / 0,75 $ (mit Trainings-Opt-in −33 %: 0,16 $ / 0,50 $) | Seite zeigt Bestfall 0,12 $ / 0,38 $ | https://www.speechmatics.com/pricing |
| Gladia Solaria-3 / Solaria-1 (Async) | 0,16 € / 0,27 € | Echtzeit 0,68 €/h | https://gladia.io/pricing |
| ElevenLabs Scribe v2 / Realtime | 0,22 $ / 0,39 $ | Keyterms +0,05 $/h | https://elevenlabs.io/pricing/api |
| OpenAI gpt-4o-mini-transcribe / gpt-transcribe / gpt-4o-transcribe und whisper | 0,18 $ / 0,27 $ / 0,36 $ | Minutenwerte sind Schätzungen der Preisseite | https://developers.openai.com/api/docs/pricing |
| Gemini 3.5 Transcribe | ca. 0,30 $ (0,005 $/min geschätzt, davon 0,003 Eingabe + 0,002 Ausgabe) | Batch/Flex nicht unterstützt (therundown) | https://ai.google.dev/gemini-api/docs/pricing |
| Gemini 3.5 Flash-Lite (Audio, LLM) | **ca. 0,06 $ (eigene Rechnung)**: 115.200 Audio-Token/h × 0,30 $/1 Mio. = 0,035 $ plus ca. 9.000 Ausgabe-Token/h × 2,50 $/1 Mio. = 0,022 $; Annahme 32 Token/s und 9.000 Ausgabetoken je Sprechstunde | Annahme ungeprüft | https://ai.google.dev/gemini-api/docs/pricing |
| Amazon Transcribe Batch / Streaming (US-Ost) | 0,36 $ / 0,60 $ | Preisseite-Beispielrechnung: 0,006 bzw. 0,01 $/min | https://aws.amazon.com/transcribe/pricing/ |
| Azure Speech Echtzeit-Standard (S1) / Batch | 1,00 $ / 0,18 $ | Retail-Prices-API | https://prices.azure.com/api/retail/prices |
| Google Cloud STT V2 (Standard, enthält `chirp`; Chirp 3 nicht separat gelistet) | **0,96 $** (0,016 $/min); Dynamic Batch 0,18 $ | Dynamic Batch für Diktat ungeeignet | https://cloud.google.com/speech-to-text/pricing |

---

## 7. Profiltabelle: Kosten pro Monat (30 Tage)

Annahmen: **Wenig 5 h, Mittel 15 h, Intensiv 60 h** gesendete Audiozeit pro Monat. Spalte „bezahlt“ = Kosten ohne jedes Freikontingent (Listenpreis, Monat 1). Spalte „mit Gratis“ = Kosten unter Berücksichtigung dauerhafter Kontingente (bei Startguthaben siehe Tabelle 7.2).

### 7.1 Monatskosten

| Anbieter / Modell | Wenig 5 h | Mittel 15 h | Intensiv 60 h | Mit Freikontingent (Wenig / Mittel / Intensiv) |
|---|---|---|---|---|
| **Groq Turbo** | 0,20 $ | 0,60 $ | 2,40 $ | **0 / 0 / 0** (Free Plan; Intensiv nutzt 2 h/Tag von 8 h erlaubt) |
| Groq large-v3 | 0,56 $ | 1,67 $ | 6,66 $ | **0 / 0 / 0** (selbes Limit wie Turbo) |
| Cloudflare Whisper-Turbo | 0,15 $ | 0,45 $ | 1,80 $ | **0 / 0 / 0** (10.000 Neurons/Tag; Intensiv: 120 min × 46,63 = 5.596 Neurons/Tag, unter dem Limit) |
| Cloudflare Nova-3 | 1,56 $ | 4,68 $ | 18,72 $ | Wenig 0 (10 min × 472,73 = 4.727 Neurons/Tag); Mittel 14.182 Neurons/Tag > 10.000 = **Sperre**; Intensiv Sperre |
| DeepInfra Whisper-Turbo | 0,06 $ | 0,18 $ | 0,72 $ | kein Gratis |
| Together Whisper Large v3 | 0,45 $ | 1,35 $ | 5,40 $ | kein Gratis |
| Alibaba qwen3-asr-flash | 0,63 $ | 1,89 $ | 7,56 $ | 10 h einmalig |
| Mistral Voxtral Mini Transcribe V2 | 0,90 $ | 2,70 $ | 10,80 $ | Free Mode für Audio unbelegt |
| Mistral Voxtral Mini Transcribe 3 | 1,05 $ | 3,15 $ | 12,60 $ | wie oben |
| Mistral Voxtral Small Transcribe 3 | 1,20 $ | 3,60 $ | 14,40 $ | wie oben |
| Soniox async | 0,50 $ | 1,50 $ | 6,00 $ | kein Gratis |
| AssemblyAI Universal-2 | 0,75 $ | 2,25 $ | 9,00 $ | 50 $ Startguthaben |
| AssemblyAI Universal-3.5 Pro | 1,05 $ | 3,15 $ | 12,60 $ | 50 $ Startguthaben |
| AssemblyAI Sync-API (U3.5 Pro) | 1,50 $ | 4,50 $ | 18,00 $ | 50 $ Startguthaben |
| Deepgram Nova-3 monolingual | 1,29 $ | 3,87 $ | 15,48 $ | 200 $ Startguthaben |
| Deepgram Nova-3 multilingual | 1,56 $ | 4,68 $ | 18,72 $ | 200 $ Startguthaben |
| Speechmatics Melia 1 (Liste 0,24 $/h) | 1,20 $ | 3,60 $ | 14,40 $ | 100 $ Startguthaben (12 Monate) |
| Speechmatics Enhanced (Liste 0,75 $/h) | 3,75 $ | 11,25 $ | 45,00 $ | 100 $ Startguthaben (12 Monate) |
| Gladia Solaria-3 (Async) | 0,80 € | 2,40 € | 9,60 € | 50 € Startguthaben (1 Jahr) |
| ElevenLabs Scribe v2 | 1,10 $ | 3,30 $ | 13,20 $ | Free: 12 bis 30 min/Monat |
| OpenAI gpt-4o-mini-transcribe | 0,90 $ | 2,70 $ | 10,80 $ | keines |
| OpenAI gpt-transcribe | 1,35 $ | 4,05 $ | 16,20 $ | keines |
| OpenAI gpt-4o-transcribe / whisper | 1,80 $ | 5,40 $ | 21,60 $ | keines (whisper-1: 200 RPD gratis) |
| Gemini 3.5 Transcribe | 1,50 $ | 4,50 $ | 18,00 $ | Free Tier, Limit unbekannt (Hypothese: bei 100 RPD und 15-s-Clips = 25 min/Tag, deckt nur Wenig) |
| Gemini 3.5 Flash-Lite | 0,30 $ | 0,90 $ | 3,60 $ | Free Tier 500 RPD (Drittmessung): Wenig 40 Clips/Tag und Mittel 120 Clips/Tag liegen darunter; Intensiv 480 Clips/Tag knapp darunter, aber nur bei 15-s-Clips (bei 5-s-Clips wären es 1.440 Clips/Tag, weit über 500 RPD). TPM unbekannt |
| Amazon Transcribe Batch | 1,80 $ | 5,40 $ | 21,60 $ | −0,36 $ (60 min/Monat, falls das Konto das Kontingent hat) |
| Amazon Transcribe Streaming | 3,00 $ | 9,00 $ | 36,00 $ | −0,60 $ |
| Azure Echtzeit-Standard (S1) | 5,00 $ | 15,00 $ | 60,00 $ | F0: 5 h/Monat gratis, deckt nur Wenig; Mittel/Intensiv nur über S1 ohne Gratisanteil |
| Google Cloud STT V2 | 4,80 $ | 14,40 $ | 57,60 $ | V2 ohne Freiminuten; 300 $ Startguthaben (90 Tage, Karte) |

### 7.2 Wie lange trägt ein Startguthaben? (Monate bei gleichbleibender Nutzung)

| Guthaben (Modell) | Stunden | Wenig 5 h/Monat | Mittel 15 h/Monat | Intensiv 60 h/Monat | Ablauf |
|---|---|---|---|---|---|
| Deepgram 200 $ (Nova-3 monolingual) | 775 | 155 | 52 | 13 | kein Ablauf laut Preisseite |
| Deepgram 200 $ (Nova-3 multilingual) | 641 | 128 | 43 | 11 | kein Ablauf |
| AssemblyAI 50 $ (U3.5 Pro, rechnerisch) | 238 | 48 | 16 | 4 | kein Ablauf |
| AssemblyAI 50 $ (Vendorangabe 185 h) | 185 | 37 | 12 | 3 | kein Ablauf |
| AssemblyAI 50 $ (Sync-API) | 167 | 33 | 11 | 2,8 | kein Ablauf |
| Speechmatics 100 $ (Melia 1) | 417 | 12 (nur 60 h genutzt, Ablauf!) | 12 (180 h) | 6,9 | 12 Monate |
| Speechmatics 100 $ (Enhanced) | 133 | 12 (60 h) | 8,9 | 2,2 | 12 Monate |
| Gladia 50 € (Solaria-3) | 312 | 12 (60 h) | 12 (180 h) | 5,2 | 1 Jahr |
| Google Cloud 300 $ (0,96 $/h) | 312 | 3 (15 h, Ablauf 90 Tage) | 3 (45 h) | 3 (180 h) | 90 Tage, Karte |
| Alibaba 10 h (qwen3-asr-flash) | 10 | 2 | 0,7 | 0,17 | 90 Tage |

### 7.3 Wirkung der Groq-Mindestabrechnung von 10 Sekunden (Diktat-Rechenbeispiel)

Rechnung für das Groq-Tageslimit (28.800 s/Tag, 2.000 RPD, 20 RPM). Abgerechnete Zeit je Clip = max(10 s, Clip-Länge).

| Profil | Reale Sprechzeit/Tag | Clip 5 s: Clips / abgerechnet | Clip 15 s: Clips / abgerechnet | Limit-Auslastung (schlechtester Fall) |
|---|---|---|---|---|
| Wenig | 10 min = 600 s | 120 Clips / 1.200 s | 40 Clips / 600 s | 4 % der Sekunden, 6 % der Anfragen |
| Mittel | 30 min = 1.800 s | 360 Clips / 3.600 s | 120 Clips / 1.800 s | 12,5 % der Sekunden, 18 % der Anfragen |
| Intensiv | 120 min = 7.200 s | 1.440 Clips / 14.400 s | 480 Clips / 7.200 s | **50 % der Sekunden, 72 % der Anfragen** |

Ergebnis: **Selbst bei Intensiv mit 5-s-Clips bleibt Frank unter beiden Tageslimits** (14.400 von 28.800 s; 1.440 von 2.000 Anfragen). Das 20-RPM-Limit ist bei Diktat nie die Bremse, weil ein Mensch nicht schneller als etwa 10 Clips pro Minute diktiert. Zählt die 10-s-Mindestmenge auch gegen das Stundenlimit von 7.200 s, bleibt Intensiv ebenfalls im Rahmen (maximal 1.440 × 10 s ÷ Aktivstunden; **unbelegt**, ob so gezählt wird).

---

## 8. Fazit: Wo gibt es am meisten kostenlose Stunden bei besserer Qualität als Turbo?

Qualität = Einordnung aus R8 (unabhängige Deutsch-Ranglisten): „belastbar besser als Turbo“ sind Whisper large-v3 (selbes Gewicht, anderer Checkpoint), ElevenLabs Scribe v2, AssemblyAI Universal-3.5 Pro, Mistral Voxtral Small und Qwen3-ASR-1.7B; „wahrscheinlich besser, eine Quelle“ sind Azure Speech (LLM/MAI), gpt-transcribe, Speechmatics Enhanced, Soniox v5; „nicht belegt“ sind Gemini 3.5 Transcribe, Chirp 3, Deepgram Nova-3, Gladia.

**Rangfolge nach kostenlosen Stunden bei belegbar oder plausibel besserer Qualität:**

1. **Groq `whisper-large-v3` (nicht Turbo): 240 h/Monat dauerhaft kostenlos.** Das ist das größte Gratisvolumen mit belastbar besserer Qualität (R8: HF 3,20/4,79 gegen 3,67/8,58 für Turbo auf Deutsch). Es zählt als „kleinster Schritt“, weil es dasselbe Modell mit anderem Gewicht ist, aber es ist **der einzige dauerhafte Weg zu mehr Qualität ohne Geldausgabe**: gleiches Free-Limit wie Turbo (20 RPM, 2K RPD, 28,8K s/Tag, in der Rate-Limit-Tabelle identische Zeile für beide Modelle), Preis falls bezahlt 0,111 $/h. Nachteil: langsamer als Turbo. Quellen: https://console.groq.com/docs/rate-limits , R8.
2. **AssemblyAI Universal-3.5 Pro: 185 bis 238 h einmalig, ohne Karte, ohne Ablauf.** Belastbar besser (R8). Bei „Mittel“ reicht das für 12 bis 16 Monate, bei „Intensiv“ für 3 bis 4 Monate. Danach 0,21 $/h (Mittel 3,15 $/Monat). Sync-API für Kurzclips 0,30 $/h.
3. **Deepgram Nova-3: 640 bis 775 h einmalig, ohne Karte, ohne Ablauf.** Mit Abstand das größte Stundenvolumen aus Startguthaben, aber Deutsch-Überlegenheit gegen Turbo **nicht belegt** (R5/R8). Erst nach einem Test mit Franks Diktaten sinnvoll.
4. **Speechmatics Enhanced oder Melia 1: 133 bzw. 417 h, 12 Monate gültig, ohne Karte.** Enhanced „wahrscheinlich besser“ (R8, eine Quelle). Rechnet man mit Listenpreisen (nicht mit dem Bestfall „0,12 $“), trägt es bei „Mittel“ mit Enhanced nur etwa 9 Monate.
5. **Gladia Solaria-3: 312 h, 1 Jahr gültig, Training standardmäßig an.** Deutsch-Qualität nicht belegt.
6. **Gemini 3.5 Transcribe (Free Tier):** plausibel besser, **aber das Limit ist unbekannt (Hinweis auf 100 RPD oder weniger), im EWR gilt die Klausel „nur Paid Services für Apps, die Nutzern angeboten werden“**. Für Frank allein vielleicht ausreichend, als Basis für eine Verkaufs-App (Genialer Wecker) nicht. Flash-Lite hat zwar 500 RPD, ist aber ein LLM mit Umformulierungsrisiko (R3).
7. **Mistral Voxtral (Mini/Small):** vermutlich besser (R6/R8: Small 24B auf HF-Deutsch 2,61/3,19), preislich mit 0,18 bis 0,24 $/h der günstigste Weg zu besserer Qualität **ohne** Startguthaben-Limit; ob der Free Mode Audio erlaubt, ist unbelegt und wäre ein Gewinn. Trainingsvorbehalt im Free Mode beachten.
8. **Google Cloud 300 $: 312 h, aber nur 90 Tage und mit Karte.** Chirp 3 ohne Deutsch-Beleg, 24-facher Preis. Als Gratisweg unattraktiv.

**Gesamturteil:** Für „sehr viel freies Kontingent und besser als Turbo“ gibt es **keinen Anbieter, der dauerhaft und monatlich erneuert mehr als Groq bietet**. Wer Qualität will, muss entweder (a) bei Groq `whisper-large-v3` statt Turbo nehmen (240 h/Monat gratis, kein Anbieterwechsel), oder (b) ein **einmaliges Startguthaben** verbrennen (AssemblyAI zuerst, danach Deepgram, Speechmatics, Gladia nacheinander mit jeweils einem frischen Konto-Guthaben; Achtung: Mehrfachkonten zur Guthabenvermehrung verstoßen häufig gegen die Nutzungsbedingungen, Soniox hat deshalb die Gratis-Credits gestrichen), oder (c) bezahlen: Mistral Voxtral Mini 3 kostet bei „Mittel“ etwa 3,15 $ im Monat, AssemblyAI Universal-3.5 Pro ebenfalls 3,15 $ — für alle drei Profile unter 13 $/Monat (Intensiv 12,60 $).

**Praktische Reihenfolge für den Test (Kostenseite):**
1. Groq `whisper-large-v3` gegen Turbo (kein neues Konto, 0 $).
2. AssemblyAI-Konto anlegen (kein Karte, 50 $), Universal-3.5 Pro und Sync-API testen.
3. Mistral-Free-Key anlegen und einen einzigen Voxtral-Aufruf fahren: beantwortet die Frage „Audio im Free Mode?“.
4. Gemini 3.5 Transcribe: im AI-Studio-Dashboard die echten Limits ablesen (https://aistudio.google.com/rate-limit).
5. Deepgram-Konto für den Großtest (200 $) nur bei Bedarf.

---

## 9. Offene Punkte und Widersprüche

- **Gemini-Free-Limits für `gemini-3.5-transcribe`:** nicht veröffentlicht; Forum zeigt 100-RPD-Durchsetzung bei Tier 1/2 (Google: „should have been resolved“, 12.10.2026, unbestätigt). Aus dieser Recherche nicht klärbar.
- **Mistral Experiment-Plan:** Der verlinkte Hilfeartikel (ID 455206/450104) liefert heute 404; Inhalte stammen aus Suchtreffer-Zitaten. Audio-Limits unbelegt.
- **Alibaba:** Die internationale Preisseite listet das 10-h-Gratiskontingent für Singapur, die China-Spiegelseite nur für Beijing. R6 las die China-Variante. `alibabacloud.com` ist die internationale Doku, `help.aliyun.com` die Festland-Doku; für ein internationales Konto ist daher wahrscheinlich die Singapur-Zeile (10 h) einschlägig. Endgültig zeigt es nur die Konsole.
- **ElevenLabs:** 12 min (Speech-to-Text-Seite) gegen ca. 30 min (330 Credits/min).
- **AssemblyAI:** FAQ „185 h“ gegen Rechnung 238 h bei 0,21 $/h.
- **Deepgram:** Ob „monolingual“ oder „multilingual“ für `language=de` gilt: unbelegt.
- **AWS:** Alt-Kontingent 60 min/12 Monate gegen Credit-Free-Plan für Neukonten (R4).
- **Groq:** Kommerzielle Nutzung des Free Plans und Trainingsnutzung nicht geprüft.
- Fast alle Anbieter: **kommerzielle Nutzung im Gratisbetrieb nicht geprüft** (Nutzungsbedingungen nicht gelesen), Ausnahmen: ElevenLabs Free ausgeschlossen (R6), Gemini mit EWR-Klausel (R3).

---

BEST-PRACTICES-KANDIDATEN:
- Free-Tier-Limits, die ein Anbieter nicht mehr veröffentlicht (Gemini), lassen sich über das Feld `quotaValue` im 429-Fehler (`QuotaFailure`) eines frischen Projekts auslesen; Messung nie auf einem Konto mit Billing, weil dort Tier-Limits gelten. Quelle: https://dev.to/romeroyang/geminis-free-tier-measured-20-requests-a-day-and-google-no-longer-publishes-the-number-4gf2 (Messung 02.09.2026, Modelle gemini-3.5-flash-lite, gemini-3.5-flash).
- STT-Kosten immer als „gesendete Audiozeit × Listenpreis je Stunde“ rechnen, nicht mit „ab“-Preisen: Speechmatics zeigt 0,12 $/h als Bestfall nach Rabatten, Listenpreis 0,24 $/h. URL: https://www.speechmatics.com/pricing (Stand 21.10.2026).
- Groq-Abrechnung: Mindestens 10 s je Anfrage; für Kurz-Diktat-Clips die abgerechnete Länge als max(10 s, Clip-Länge) rechnen. URL: https://console.groq.com/docs/speech-to-text (Stand 21.10.2026), Modellkarte https://console.groq.com/docs/model/whisper-large-v3-turbo.
- Cloudflare Workers AI: Gratis-Stunden aus Neurons rechnen (10.000 ÷ 46,63 Neurons/min = 214 min/Tag für whisper-large-v3-turbo; Nova-3 nur 21 min/Tag). URL: https://developers.cloudflare.com/workers-ai/platform/pricing/ (Stand 21.10.2026).
- Prepaid-Konten und Free-Plans mit hartem 429 als Kostenbremse bevorzugen (AssemblyAI pausiert bei 0 $, Gladia Prepaid-Wallet, Together Prepaid, Groq/Cloudflare 429): https://www.assemblyai.com/docs/billing-and-pricing.md , https://gladia.io/pricing , https://developers.cloudflare.com/workers-ai/platform/pricing/ .
- Google Cloud STT: „60 Min. gratis“ gilt nur für die V1-API, nicht für V2/Chirp 3: https://cloud.google.com/speech-to-text/pricing , https://cloud.google.com/free/docs/free-cloud-features (Stand 21.10.2026).

BUG-KANDIDATEN:
- **Gemini API `gemini-3.5-transcribe`: Tier 2 zeigt 10.000 RPD im Dashboard, die API erzwingt aber 100 RPD (429 mit „limit: 100“).** Gemeldet 16.09.2026, Tier 1 bestätigt am 23.09.2026, Google 12.10.2026: „This should have been resolved. Please retry“, ohne Bestätigung. Modell `gemini-3.5-transcribe`. URL: https://discuss.ai.google.dev/t/tier-2-gemini-3-5-transcribe-shows-10-000-rpd-but-api-enforces-100-rpd/183223
- **Gemini API: neue Keys liefern 404 für gemini-2.5-pro/-flash/-flash-lite, obwohl die Preisseite sie noch als kostenlos führt;** Free-Tier-Limits sind in der Rate-Limit-Doku nicht mehr veröffentlicht. Stand 02.09.2026. URL: https://dev.to/romeroyang/geminis-free-tier-measured-20-requests-a-day-and-google-no-longer-publishes-the-number-4gf2
- **Alibaba Model Studio Preisseite: Spiegelseiten widersprechen sich beim ASR-Gratiskontingent** (international: Singapur; China-Spiegel: nur Beijing) für `qwen3-asr-flash`. URLs: https://www.alibabacloud.com/help/en/model-studio/model-pricing und https://help.aliyun.com/en/model-studio/model-pricing (Stand 21.10.2026).
- **Mistral-Hilfeartikel zum Experiment-Plan liefert 404** (IDs 455206 und 450104), obwohl Suchmaschinen ihn indexieren. URL: https://help.mistral.ai/en/articles/455206-how-can-i-try-the-api-for-free-with-the-experiment-plan (Stand 21.10.2026).
- **Soniox: Quickstart-Seite mit „300 Minuten kostenlos“ ist nicht abrufbar bzw. nicht bestätigt;** Gratis-Credits für neue API-Konten seit 27.10.2025 gestrichen. URL: https://soniox.com/blog/2025-10-27-free-credits-update-for-soniox-api
