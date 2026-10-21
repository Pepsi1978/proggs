# R3 — Google komplett: Cloud Speech-to-Text v2, Gemini-Transcribe/Gemini-Audio und Android-On-Device für deutsches Diktat

Stand: 21.10.2026. Basislinie: Groq `whisper-large-v3-turbo`. Alle Preise in USD. „unbelegt" = nicht aus einer Primärquelle bestätigt. Quellen stehen bei der jeweiligen Aussage; Abrufdatum aller Seiten: 21.10.2026, sofern nichts anderes steht.

Einordnung der Quellenlage vorab:
- Es gibt **keine belastbare, unabhängig gemessene deutsche WER für `gemini-3.5-transcribe`**. Google nennt nur Mittelwerte (FLEURS über „top languages and locales") und englisch lastige Artificial-Analysis-Werte.
- Die einzige deutsch aufgeschlüsselte Gemini-3.5-Tabelle (autointerviewai.com, de-DE 4,60 %) ist **nicht vertrauenswürdig** (Begründung im Abschnitt 2.4).
- Die eigenen Messungen von Frank (Almanach `bugs/apis/google-gemini-api.md` §K26–34, 29.08.2026, TerminalVoiceOverlay) sind Primärquelle für Latenz und Verhalten.

---

## 1. Google Cloud Speech-to-Text v2

### 1.1 Modelle und Status (v2-Modellnamen!)

In v2 heißen die Modelle anders als in v1: `latest_long`/`latest_short` aus v1 heißen in v2 `long`/`short`; dazu `telephony`, `telephony_short`, `chirp_2`, `chirp_3`, `chirp_telephony`, `chirp`. Für `de-DE` listet die Sprachtabelle (https://docs.cloud.google.com/speech-to-text/v2/docs/speech-to-text-supported-languages) je Region:

| Region | Modelle für de-DE |
|---|---|
| `eu` (Multi-Region) | `chirp_3`, `long`, `short`, `telephony`, `telephony_short` |
| `us` (Multi-Region) | `chirp_3`, `long`, `short`, `telephony`, `telephony_short` |
| `global` | `long`, `short`, `telephony`, `telephony_short` |
| `europe-west4` | `chirp`, `chirp_2`, `chirp_telephony` |
| `us-central1`, `asia-southeast1` | `chirp`, `chirp_2`, `chirp_telephony` |

- **`chirp_3`**: GA seit 13.10.2025 (Release Notes https://docs.cloud.google.com/speech-to-text/docs/release-notes). `de-DE` ist GA für StreamingRecognize, Recognize und BatchRecognize. Regionen `us` und `eu` (Multi-Region) GA (https://docs.cloud.google.com/speech-to-text/docs/models/chirp-3). Nur in der v2-API.
- **`chirp_2`**: GA seit 27.01.2025 in `asia-southeast1`, `us-central1`, `europe-west4`. Die Chirp-2-Doku nennt die Regionen heute „Private GA" (https://docs.cloud.google.com/speech-to-text/docs/models/chirp-2). Vorgänger von Chirp 3, für Neues nicht mehr empfehlenswert.
- **`long` / `short` / `telephony`**: klassische Modelle (kein generatives Modell). Telephony ist für 8-kHz-Telefonaudio gedacht, für Handy-Mikrofon-Diktat ungeeignet.
- Abschaltdaten: in den Release Notes bis Stand 21.10.2026 **keine** Abschaltung von `chirp_3` genannt (neuester Eintrag 13.11.2025). Google positioniert aber `gemini-3.5-transcribe` als Nachfolger („improves over Chirp 3").

### 1.2 Deutsch-Qualität
- **Kein deutsches WER von Google für Chirp 3 veröffentlicht** (nicht gefunden).
- Artificial Analysis AA-WER v2 (englisch lastig: AA-AgentTalk 50 %, VoxPopuli-Cleaned 25 %, Earnings22-Cleaned 25 %; Sprachen nicht ausgewiesen): Chirp 3 **4,3 %**, Groq Whisper Large v3 Turbo **4,6 %**, Chirp (alt) 31,2 % (https://artificialanalysis.ai/speech-to-text/non-streaming). Der Abstand Chirp 3 zu Turbo ist marginal und sagt über Deutsch nichts.
- Google selbst: Gemini 3.5 Transcribe ist auf FLEURS (Mittel über Top-Sprachen) besser als Chirp 3 (https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/); Zahlen für Chirp 3 nennt der Blog nicht. Die autointerviewai-Tabelle nennt Chirp 3 de-DE 5,90 % (siehe 2.4, unzuverlässig).
- Halluzination: Hacker-News-Kommentar (film42): „if you pass chirp some audio with noise or even no audio, it will barf text at you" (https://news.ycombinator.com/item?id=49468818). Gilt dort für „Chirp"; für `chirp_3` unbelegt.

### 1.3 Freikontingent (genau geprüft)
Rohtext der offiziellen Preisseite (https://cloud.google.com/speech-to-text/pricing, 21.10.2026 per HTTP abgerufen):
- **v2-Standard-SKU `3099-B70F-0949` („Recognition")**: Stufen beginnen bei „0 minute to 500,000 minute **$0.016** / 1 minute, per 1 month / account". **Es gibt in der v2-Tabelle keine Zeile „Free".**
- **60 Freiminuten pro Monat** stehen nur bei den **v1**-SKUs (`67F5-A183-E319` mit Data Logging: 0–60 min $0,00, danach $0,016; `60AE-2FE3-C3D8` ohne Data Logging: 0–60 min $0,00, danach $0,024) und bei den Medical-SKUs.
- Fazit: „60 Min./Monat gratis" ist **für v1 belegt, für v2 (und damit für `chirp_3`, das nur in v2 existiert) nicht belegt** — die v2-Preisseite führt keine Freiminuten. Chirp 3 ist kein v1-Modell, die 60 Freiminuten helfen also nicht. Ob das 300-$-Neukunden-Guthaben von Google Cloud zusätzlich greift: nicht geprüft (nicht Teil der Preisseite).
- Das Freikontingent ist für Franks Zweck („großes kostenloses Kontingent") bei Cloud STT damit praktisch **nicht vorhanden**.

### 1.4 Preis je Audiostunde
Quelle: https://cloud.google.com/speech-to-text/pricing
- v2 Standard (inkl. `chirp_2`/`chirp_3`/`long`/`short`, Preisseite führt `chirp_3` nicht separat auf; „Standard models include: default, command_and_search, latest_short, latest_long, phone_call, video, chirp (Speech-to-Text V2 only)"): **$0,016/min = $0,96/h** für die ersten 500.000 Min./Monat; 500k–1 Mio. $0,01; 1–2 Mio. $0,008; ab 2 Mio. $0,004/min. Abrechnung sekundengenau; mehrkanalig wird je Kanal berechnet. Mit Data-Logging-Opt-in laut v1-Tabelle günstiger (v1: $0,016 statt $0,024); für v2 steht dort kein Data-Logging-Preis.
- **Dynamic Batch** (v2, SKU `7700-6778-EF8E`): **$0,003/min = $0,18/h**, Auslieferung „within 24 hours" (https://docs.cloud.google.com/ruby/docs/reference/google-cloud-speech-v2/latest/Google-Cloud-Speech-V2-BatchRecognizeRequest-ProcessingStrategy). Die Preisseite nennt im Dynamic-Batch-Block `chirp`, nicht `chirp_3`: **Unterstützung für `chirp_3` ist unbelegt.** Für Diktat (Sofortantwort) ohnehin ungeeignet.
- Zum Vergleich Groq Turbo: ca. $0,67 je 1000 Min. = $0,04/h (AA).

### 1.5 Rate Limits, Größen
Quelle https://docs.cloud.google.com/speech-to-text/quotas (Stand 20.10.2026): 900 Erkennungsanfragen je 60 s, 10 Adaptions-Anfragen je 60 s, 480 Stunden Audio pro Tag; synchron ca. 1 Min., asynchron ca. 480 Min., Streaming ca. 5 Min.; lokale Dateien/Requests max. 10 MB, Cloud-Storage-URI ohne Größenlimit. Chirp 3 Doku: Recognize < 1 Min., BatchRecognize ca. 1 Min. bis 1 Std. (20 Min. mit Wortzeitstempeln).

### 1.6 Streaming / Echtzeit
`StreamingRecognize` für `chirp_3` unterstützt (GA), Streamingsitzung laut Quotas-Seite ca. 5 Min. Utterance-Zeitstempel nur im Streaming; Diarization nur Batch; Wortzeitstempel nicht unterstützt (optional, mit Genauigkeitsverlust, nur Recognize/Batch).

### 1.7 Vokabular / Prompting
- Speech Adaptation (Phrase Sets, Biasing): GA, `chirp_3` bis **1.000 Phrasen**, „use as few entries as possible", sonst Verschlechterung bei Nicht-Adaptionsbegriffen. Allgemein bis 5.000 Phrasen/Request, 100.000 Zeichen, 100 Zeichen je Phrase.
- **Custom Prompt** (Formatierungsanweisung, z. B. „Capitalize the following special words…"): nur **Preview**.
- Denoiser: `denoiser_audio=true` (reduziert Hintergrundgeräusche, keine Fremdstimmen).

### 1.8 Halluzination
Siehe 1.2: Chirp-Familie fällt durch Text bei Stille/Rauschen auf (Einzelbericht, unbelegt für Chirp 3). Sonst nichts Belastbares.

### 1.9 Datenschutz / Datenresidenz / Training
- Standardmäßig loggt Cloud STT **keine** Audiodaten oder Transkripte; nur mit Opt-in in das Data-Logging-Programm (Rabatt gegen Training; Google behält Eigentum an den daraus trainierten Modellen, Daten werden bei Projektlöschung nicht gelöscht) (https://docs.cloud.google.com/speech-to-text/docs/data-logging).
- EU: Multi-Region `eu` für `chirp_3` GA (Endpunkt `eu-speech.googleapis.com`); das ist eine echte Datenresidenz-Option, die die Gemini-API (AI Studio) nicht bietet.

### 1.10 Latenz
Kein belastbarer Messwert. AA: Chirp 3 Speed Factor 28,4 (Faktor Echtzeit) gegen Turbo-Groq 200,8 und Gemini 3.5 Transcribe 89,5 (10-min-Audio). Google: Gemini 3.5 Transcribe hat 70 % bessere „time-to-final-transcription" als Chirp 3.

### 1.11 Android-Integration (REST-Form)
`POST https://{eu|us}-speech.googleapis.com/v2/projects/{PROJECT}/locations/{eu|us}/recognizers/_:recognize` mit `config.model="chirp_3"`, `languageCodes:["de-DE"]`, `autoDecodingConfig:{}`, `content` (Base64). **Auth verlangt OAuth-Scope `https://www.googleapis.com/auth/cloud-platform`** (https://docs.cloud.google.com/speech-to-text/docs/reference/rest/v2/projects.locations.recognizers/recognize); ein API-Schlüssel wie bei Gemini ist dort **nicht** als Auth dokumentiert. Für Jarvis/Overlay heißt das: Dienstkonto oder ADC-Token, kein einfacher Schlüssel im Header — ein Dienstkonto-Schlüssel gehört nicht in eine APK, es bräuchte einen Token-Server (Brücke). Das ist der größte praktische Nachteil gegenüber dem Gemini-Key.

---

## 2. Gemini als Speech-to-Text

### 2.1 `gemini-3.5-transcribe` (Batch/Datei) und `gemini-3.5-transcribe-live` (Streaming)

**Modell-ID / Status (Konflikt, offen ausgewiesen):**
- Google-Blog (26.08.2026): „public preview" (https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-5-transcribe/).
- Modellseite Gemini Enterprise Agent Platform (Stand 21.10.2026): `gemini-3.5-transcribe` und `gemini-3.5-transcribe-live` **GA, Release Oktober 2026**; `-preview`-Varianten Release August 2026 (https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/3-5-transcribe).
- Gemini-API-Modellseite (aktualisiert 01.09.2026): nennt **keinen** Status (https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe). Für die Gemini-API (AI Studio, Franks Weg) ist GA damit **unbelegt**; die Preisseite führt die Modelle ohne „Preview"-Kennzeichnung. Abschaltdaten: keine genannt.

**Deutsch-Qualität:**
- Google: FLEURS-WER **5,04 %** (Datei) / **5,50 %** (Streaming), Mittel über „top languages and locales", **keine Sprachaufschlüsselung**; besser als Chirp 3 (Blog oben).
- Artificial Analysis AA-WER v2 (englisch lastig): `gemini-3.5-transcribe` **2,6 %**, Streaming 4,0 %; Groq Whisper Large v3 Turbo **4,6 %**; Gemini 3 Flash (High) 2,9 %; Gemini 3.1 Flash-Lite Preview (Minimal) 3,4 %; Gemini 2.5 Flash-Lite 5,2 %; Gemini 2.5 Flash 5,1 % (https://artificialanalysis.ai/speech-to-text/non-streaming).
- Einziger belastbarer **deutscher** Anker (nicht 3.5 Transcribe): Voxtral-Paper, Tabelle 4 (FLEURS) und Tabelle 5 (Mozilla Common Voice), **Deutsch**: Gemini 2.5 Flash FLEURS 4,74 / MCV 7,25; Whisper large-v3 FLEURS 5,46 / MCV 6,25; GPT-4o mini Transcribe 3,76 / 6,72; Scribe 4,78 / 3,52; Voxtral Small 3,38 / 3,74 (https://arxiv.org/html/2507.13264). **Beachte:** Das ist `large-v3`, nicht `large-v3-turbo` (Turbo liegt laut Herstellerblog-Einschätzungen bei großen Sprachen ca. 0,5–2 Prozentpunkte hinter v3, nicht deutsch gemessen) und nicht `gemini-3.5-transcribe`. Auf Common Voice war Gemini 2.5 Flash sogar **schlechter** als Whisper v3.
- Praxisberichte: Hacker-News-Thread (https://news.ycombinator.com/item?id=49468818): Lucasoato testete 20+ STT-Modelle mit deutsch/italienischen Besprechungen und sieht nur ElevenLabs „slightly better" als sein lokales Modell (Aussage zu Gemini nicht isoliert); mariano54: „doesn't reach the frontier in either latency or accuracy" für mehrsprachige Gespräche; sireat: Scribe v2 konsistenter bei Sprachmix; sgt (rauschige Aufnahme): Google „cut off the start by several seconds".
- Superwhisper-Benchmark (nur englische Datensätze, kein Deutsch): Gemini 3.5 Transcribe blended 69, WER 8,2 % (Scribe v2 7,5 %, GPT Transcribe 7,8 %, Nova 3 10,1 %) (https://superwhisper.com/benchmarks). Auffällig: „Earnings 22 mit Vokabular" **15,8 %** gegen 8,8 % ohne (https://superwhisper.com/benchmarks/gemini-3-5-transcribe) — das deutet darauf hin, dass `custom_vocabulary` bei unpassenden Listen schaden kann (Vorsicht, andere Messmethodik, Ursache unbelegt).

**Freikontingent / Datennutzung / EWR:**
- Preisseite (https://ai.google.dev/gemini-api/docs/pricing): beide Transcribe-Modelle „Free Tier: Free of charge" für Ein- und Ausgabe; „Used to improve our products": **Free Tier = Yes, Paid Tier = No**.
- **Exakte Limits (RPM/RPD/TPM) für die Transcribe-Modelle: nicht öffentlich dokumentiert** (die Rate-Limit-Seite verweist auf das AI-Studio-Dashboard, https://ai.google.dev/gemini-api/docs/rate-limits). Eigene Messung (Almanach §K32): 429 nach ca. 12 Aufrufen in wenigen Minuten. Forum-Thread: auch Tier 2 wird mit „limit: 100, model: gemini-3.5-transcribe" abgeregelt, obwohl 10.000 RPD angezeigt werden; Google-Antwort 12.10.2026 „This should have been resolved. Please retry", ohne Bestätigung (https://discuss.ai.google.dev/t/tier-2-gemini-3-5-transcribe-shows-10-000-rpd-but-api-enforces-100-rpd/183223). Ein Tier-1-Nutzer meldet dasselbe 100-RPD-Limit. Ob 100 RPD auch das Free-Tier-Limit ist: unbelegt.
- **EWR-Klauseln der Gemini-API-Bedingungen (Stand der Seite 2026-04-28, https://ai.google.dev/gemini-api/terms), zwei getrennte Aussagen:**
  1. Datennutzung: „If you're in the European Economic Area, Switzerland, or the United Kingdom, the terms under 'How Google uses Your Data' in 'Paid Services' apply to all Services, including Google AI Studio and unpaid quota in the Gemini API, even though they are offered free of charge." → In Deutschland gilt auch im Free Tier die Paid-Regel: Google verwendet Prompts/Antworten **nicht** zur Produktverbesserung (Tabellen-Spalte „Yes" auf der Preisseite gilt dort nicht). Das Logging für Missbrauchserkennung bleibt.
  2. Nutzungsbeschränkung: „You may use only Paid Services when making API Clients available to users in the European Economic Area, Switzerland, or the United Kingdom." → Wer eine App mit Gemini-Free-Tier **an Nutzer im EWR ausliefert**, verstößt formal gegen die Bedingungen. Ob Franks eigener Gebrauch seiner privaten Apps (er ist Entwickler und einziger Nutzer) darunter fällt, ist **eine Rechtsfrage; nicht entschieden**. Die Billing-FAQ (https://ai.google.dev/gemini-api/docs/billing) sagt dagegen, Free und Paid seien in „many regions" nutzbar, auch EWR/UK/CH.
  - Paid Services = nur über ein Cloud-Projekt mit aktivem Billing-Konto. Prepay ab 5 USD verschiebt das Projekt in den Paid Tier; dort gibt es **kein** Freikontingent mehr (Aussage der Billing-Seite unklar: „doesn't say whether a billed project keeps any free-tier quota" — Hinweis aus dem Fetch, keine eindeutige Quelle).

**Preis (bezahlt):** Datei-Modell Eingang $2,00/1 Mio. Token = $0,003/min, Ausgang $12,00/1 Mio. = $0,002/min, **gemischt ca. $0,005/min = $0,30/h**; Live-Modell $3,50 / $21,00 → ca. $0,005 + $0,004, **gemischt ca. $0,009/min = $0,54/h**. Annahme laut Google: **25 Audio-Token/s** und 175 Text-Token/min Ausgabe (Preisseite). Kein Batch/Flex/Priority. Groq Turbo ca. $0,04/h → Transcribe ist ca. **7,5×** teurer.

**Rate Limits:** siehe oben (unbelegt, vermutlich 100 RPD je Tier-1/2-Projekt laut Forum).

**Max. Dateigröße/-länge (Konflikt):**
- Gemini API: bis 1 Std. je Anfrage, **30 Min.** mit Diarization/Wortzeitstempeln; Live-Sitzung 10 Min. (https://ai.google.dev/gemini-api/docs/models/gemini-3.5-transcribe). Files-API bis 2 GB je Datei, 48 h Aufbewahrung (https://dev.to/googleai/stop-wrestling-with-asr-the-complete-guide-to-gemini-35-transcribe-1m6i). Inline: Gesamtanfrage max. 20 MB (https://ai.google.dev/gemini-api/docs/audio).
- Gemini Enterprise Agent Platform: Datei **15 Min.**, Live 10 Min. (Konflikt zur Gemini-API-Angabe, plattformabhängig).

**Streaming / Echtzeit:** `gemini-3.5-transcribe-live` über WebSocket (Live API, `BidiGenerateContent`), 16-Bit-PCM, 16 kHz, 100-ms-Chunks, Sitzung 10 Min., keine Diarization, keine Wortzeitstempel, Utterance-Zeitstempel ja. Eigenmessung (§K30): Datei 64,5 s → Batch 4,4 s, Live 15,1 s.

**Vokabular / Prompting:** `custom_vocabulary` bis 1.000 Begriffe (beste Ergebnisse ca. 100); unvereinbar mit Diarization/Wortzeitstempeln; `language_codes` (`["de-DE"]`) anpassen. **Kein freier Prompt, keine System Instructions** (Vertex-Seite: „System instructions: Not supported"). `mode`: Standard `verbatim`; `smart` entfernt Füllwörter und formatiert, **kann umformulieren/auslassen** — Google selbst empfiehlt für wortgetreue Fälle `verbatim` (dev.to-Leitfaden). Eigenmessung (§K31): Smart machte aus „Ich frage mich" ein „Frage mich". HN-Berichte: Smart-Modus lässt Teilsätze weg (Crystalin), „inserts sentence breaks in random places" (jeffbee). Empfehlung: **immer `verbatim`**.

**Halluzinationsverhalten:** Ein HN-Test auf Stille/Rauschen: keine Halluzinationen („I didn't get any hallucinations though", sync), aber Fehler: „exactly 20 seconds of silence will lead to a 403" (sync). Eigenmessung: Live-Modell liefert bei Pausen Abbrüche; zusätzlich Risiko, dass ein LLM gesprochene Anweisungen befolgt statt sie zu transkribieren (simonw im HN-Thread, unbeantwortet); für dieses Spezialmodell unbelegt. Sonderfall: Live-Modell erkannte schwedisch als niederländisch (simedw).

**Datenschutz / Residenz:** Gemini-API/AI Studio: keine Regionswahl, Verarbeitung in Googles Infrastruktur, Daten „may be stored transiently or cached in any country" (Paid Services, Terms). Mit Vertex/Gemini Enterprise Agent Platform: `eu` für Verarbeitung (ML processing „Europe: Multi-region"), Modell global/us/eu verfügbar (Seite oben). Inline-Audio (Base64) statt Files API vermeidet 48 h Speicherung auf Google-Servern.

**Latenz:** Eigenmessung 4,4 s für 64,5 s Audio (Inline-Base64, Interactions API); mit Files-API 5,7 s. Superwhisper-Benchmark: 2,34 s „Wait for the text" auf 8–15-s-Clips (nicht Deutsch). Groq Turbo AA-Speed 200,8× gegen 89,5×; im Diktat (kurze Clips) vermutlich Sekundenbereich für beide, Groq erfahrungsgemäß unter 1 s (nicht in dieser Recherche gemessen).

**Android-Integration (REST-Form):**
```
POST https://generativelanguage.googleapis.com/v1beta/interactions
x-goog-api-key: $GEMINI_API_KEY
{"model":"gemini-3.5-transcribe",
 "input":[{"type":"audio","data":"<base64>","mime_type":"audio/ogg"}],   // oder "uri" aus Files API
 "generation_config":{"transcription_config":{"language_codes":["de-DE"],
   "custom_vocabulary":["…"], "mode":{"type":"verbatim"}}}}
```
Antworttext in `steps[].content[].text` (laut Almanach §K30; `output_text` nur in den SDKs). `generateContent` liefert bei diesem Modell leeren Text mit `STOP` (Almanach §K30). Akzeptierte MIME-Typen: wav, mp3, aiff, aac, ogg, flac, mpeg, m4a, l16, opus, alaw, mulaw, webm (https://ai.google.dev/gemini-api/docs/transcribe).

### 2.2 Allgemeine Gemini-Modelle mit Audioeingang (generateContent)

| Modell | Audio-Preis bezahlt (je 1 Mio. Token) | Free Tier | Anm. |
|---|---|---|---|
| `gemini-3.5-flash-lite` | 0,30 Eingabe / 2,50 Ausgabe | ja („Free of charge") | Free-Tier-Daten: Yes (Training) |
| `gemini-3.1-flash-lite` | 0,50 Audio / 1,50 Ausgabe | ja | |
| `gemini-2.5-flash-lite` | 0,30 Audio / 0,40 Ausgabe | laut Preisseite ja; für **neue Schlüssel 404** (Drittmessung) | |
| `gemini-3-flash-preview`, `gemini-2.5-flash` | 1,00 Audio | ja | |
| `gemini-3.6-flash`, `gemini-3.8-flash` | 0,75 / 3,75 (bis 31.12.2026, danach 1,50 / 7,50) | ja (nach Preisseite) | Audio-Preis nicht separat gelesen |

(Quelle Preisseite https://ai.google.dev/gemini-api/docs/pricing.)

- **Audio-Token:** **32 Token/s** für allgemeine Gemini-Modelle (1.920 Token/min; https://ai.google.dev/gemini-api/docs/audio), dagegen **25 Token/s** bei den Transcribe-Modellen (Preisseite). Beide Angaben gelten nebeneinander: es hängt vom Modell ab. Max. 9,5 Stunden Audio je Prompt; Inline-Gesamtanfrage max. 20 MB, darüber Files API.
- **Kosten je Audiominute (berechnet, nicht von Google angegeben; Ausgabe ca. 175 Token/min wie bei Google angesetzt, Denk-Token nicht gerechnet):** 3.5 Flash-Lite ca. $0,0006 + $0,0004 ≈ **$0,001/min ≈ $0,06/h**; 3.1 Flash-Lite ca. $0,00096 + $0,00026 ≈ **$0,0012/min ≈ $0,07/h**. Das ist billiger als `gemini-3.5-transcribe` ($0,30/h) und in der Größenordnung von Groq ($0,04/h).
- **Free-Tier-Limits:** nicht offiziell veröffentlicht. **Drittmessung vom 02.09.2026** (Auslesen des `QuotaFailure`-Feldes bei einem frischen Projekt ohne Billing, https://aiplug.work/en/blog/gemini-free-tier-limits): `gemini-3.5-flash-lite` und `gemini-3.1-flash-lite` je **15 RPM / 500 RPD**; `gemini-3.7/3.6/3.5-flash` und `gemini-3-flash-preview` je **5 RPM / 20 RPD**. Ein Dritter, nicht von Google garantiert. Sperrt neue Keys für 2.5-Modelle (404).
- **Deutsch-WER-Belege:** AA-WER (englisch lastig): 3.1 Flash-Lite 3,4 %, Gemini 3 Flash 2,9 %, 2.5 Flash-Lite 5,2 %; Voxtral-Paper deutsch (FLEURS 4,74 / MCV 7,25) nur für 2.5 Flash. Für Flash-Lite 3.x kein deutscher Wert gefunden.
- **Kontext-Prompting für Vokabular:** möglich, weil freier Prompt (z. B. „Transkribiere wörtlich, Eigennamen: …") — das ist der Vorteil gegenüber `gemini-3.5-transcribe`.
- **Halluzinations-/Umformulierungsrisiko:** deutlich höher als beim Spezialmodell, weil ein Sprachmodell: Beispiel eines Entwicklers, dessen MCP-Server wegen eines „Adopt a concise style"-Zusatzes Zusammenfassungen statt Wortlaut lieferte (https://glama.ai/mcp/servers/@Garblesnarff/gemini-mcp-server/blob/9826b479c7bc26e1cd528b920f32086463085233/VERBATIM_MODE_README.md); Zeitstempel-Drift bei Gemini 3 Flash/3.1 Pro (https://discuss.ai.google.dev/t/bug-gemini-3-flash-and-3-1-pro-progressive-timestamp-drift-in-audio-transcription/129501). Spoken-Prompt-Injection: unbelegt. Vendor-Blog (Fremdquelle): Gemini Flash schwächer als Whisper large-v3 bei strenger WER, stärker bei Bedeutung.

### 2.3 Live API (Echtzeit-Transkription)
`gemini-3.5-transcribe-live` ist das dedizierte Transkriptionsmodell. Die Dialogmodelle (`gemini-3.8-live`, `gemini-3.1-flash-live`, $3,00/1 Mio. Audio ≈ $0,005/min Hören) liefern Transkription nur als Nebenprodukt (`inputAudioTranscription`), mit Kontextverbrauch quadratisch wachsend (Almanach `best-practices/apis/google-gemini-api.md` §13). Nachteile des Streaming-Weges: 10-Min.-Sitzung, Pausenabbrüche (VAD muss aus, Almanach §K26), kumulative Zwischenstände (§K27), kaputtes `finished`-Flag (§K28). Für Diktat mit fertiger Aufnahme ist der Batch-Weg richtig.

### 2.4 Zur Tabelle „Gemini 3.5 Transcribe de-DE 4,60 %" (autointerviewai.com) — NICHT verwenden
Die Seite (https://www.autointerviewai.com/blog/gemini-3-5-transcribe-speech-to-text-review-2026) zeigt 25 FLEURS-Locales mit de-DE: Gemini Live 4,60 %, Chirp 3 5,90 %, GPT Live 6,10 %, Scribe v2 6,80 %, Nova-3 8,10 %. Gründe für Misstrauen: (1) Gemini schlägt in **allen 25** Zeilen jeden Wettbewerber, (2) die Spaltenmittel stimmen nicht mit den ausgewiesenen Durchschnitten überein (nachgerechnet: Gemini 5,44 statt 5,50; Chirp 3 7,38 statt 7,32), (3) die Seite sagt selbst nicht, woher die Wettbewerber-Spalten stammen, (4) Google nennt in keiner offiziellen Quelle eine solche Tabelle. Die Tabelle ist weder Primärquelle noch reproduzierbar → nicht für die Entscheidung heranziehen.

---

## 3. Android On-Device Google-Spracherkennung

### 3.1 `SpeechRecognizer` (Framework)
Quelle: https://developer.android.com/reference/android/speech/SpeechRecognizer und .../RecognizerIntent
- `createSpeechRecognizer()` (API 8): nutzt den Standarddienst (meist Google), **kann Cloud sein**.
- `createOnDeviceSpeechRecognizer()` und `isOnDeviceRecognitionAvailable()`: **API 31**. `checkRecognitionSupport()` und `triggerModelDownload(Intent)`: **API 33**; `triggerModelDownload` mit Fortschritts-Listener API 34. `EXTRA_BIASING_STRINGS` (Vokabular): API 33. `EXTRA_ENABLE_LANGUAGE_DETECTION`: API 34. `EXTRA_PREFER_OFFLINE`: API 23. `EXTRA_AUDIO_SOURCE` (Datei/Pipe als Eingabe): API 33; ob der Dienst sie unterstützt, ist dienstabhängig. (Picovoice nennt für den On-Device-Recognizer irrtümlich API 33; die Android-Referenz sagt 31.)
- **Deutsch-Qualität: keine WER-Zahlen von Google und keine unabhängige Messung gefunden → unklar.** Anbieter-Blogs (Wettbewerber) behaupten, Gboards Offline-Modell sei klein und schwach bei Eigennamen und Fachbegriffen (https://www.yaps.ai/blog/accurate-offline-speech-to-text-android) — Konkurrenzquelle, unbelegt.
- **Praxisproblem (Fairphone 6, Android 16, Oktober 2026):** Android System Intelligence lieferte nur `en-US`; Deutsch scheiterte mit `ERROR_LANGUAGE_UNAVAILABLE`; Pakete aus „Speech Services by Google" oder Live Caption wurden nicht erkannt; erst `SpeechRecognizer.triggerModelDownload` (Android 13+) holte das deutsche Paket (https://github.com/kosimk/murmel/pull/2). Die PR enthält keine Qualitäts- oder Latenzmessung. Offline-Locale muss in voller Form (`de-DE`) angegeben werden (Forumsbeitrag, nur indirekt belegt).
- Kosten: 0; Datenschutz: auf dem Gerät, sofern wirklich der On-Device-Recognizer läuft (der normale Recognizer kann stillschweigend in die Cloud wechseln).

### 3.2 ML Kit GenAI Speech Recognition (Alpha)
Quelle: https://developers.google.com/ml-kit/genai/speech-recognition/android, Artefakt `genai-speech-recognition` 1.0.0-alpha1 vom 28.01.2026 (https://developers.google.com/ml-kit/release-notes).
- **Basic**: klassisches On-Device-Modell, „generally available on most Android devices with API level 31 and higher"; `de-DE` ist **beta** (en-US GA).
- **Advanced**: GenAI-Modell (Gemini Nano über AICore), nur **Pixel 10 und 11**; `de-DE` steht dort bei den Sprachen „typically high accuracy". Auf anderen Geräten (Fold 8) bleibt nur Basic.
- Alpha ohne SLA/Deprecation-Zusage; Eingabe: Mikrofon oder rohes 16-Bit-PCM, 16 kHz, mono, nur in Echtzeit (ca. 32 KB/s); keine Aussage zu Vokabular/Sprachwahl/Maximaldauer.
- Gboard „Rambler" nutzt auf Android `gemini-3.5-transcribe` (Cloud) in „select countries and languages" — Deutschland nicht ausgewiesen (https://blog.google/…/gemini-3-5-transcribe/, https://9to5google.com/2026/08/26/gemini-3-5-transcribe/).

---

## 4. Tabelle „Kerndaten"

| Kriterium | Groq Turbo (Basis) | Cloud STT v2 `chirp_3` | `gemini-3.5-transcribe` (Datei) | `gemini-3.5-transcribe-live` | Gemini 3.5/3.1 Flash-Lite (Audio) | Android On-Device |
|---|---|---|---|---|---|---|
| Status | GA | GA (13.10.2025) | Preview laut Blog, GA laut Vertex-Seite (Okt. 2026), Gemini-API-Seite ohne Status | wie links | 3.5 Flash-Lite: Free & Paid; Status nicht geprüft | Basic: API 31+, de-DE beta; ML Kit: Alpha |
| Deutsch (Beleg) | AA 4,6 % (englisch); v3 FLEURS de 5,46 % (Voxtral) | AA 4,3 % (englisch); kein de-Wert | Google FLEURS Mittel 5,04 %; AA 2,6 %; de nicht aufgeschlüsselt | FLEURS 5,50 %; AA 4,0 % | AA 3,4 % (3.1 FL); de unbelegt | keine WER, unklar |
| Freikontingent | Groq-Stufe (andere Recherche) | v2: **keins belegt**; 60 Min. nur v1 | „Free of charge", Limits unbelegt; Training im EWR ausgeschlossen, Bereitstellung an EWR-Nutzer nur Paid (Klausel) | wie Datei | Free Tier: Drittmessung 15 RPM / 500 RPD (Flash-Lite) | unbegrenzt |
| Preis je Std. bezahlt | ca. $0,04 | $0,96 (Dyn. Batch $0,18, chirp_3 unbelegt) | ca. $0,30 | ca. $0,54 | ca. $0,06–0,07 (berechnet) | 0 |
| Rate Limits | – | 900 Req./min, 480 h/Tag | unbelegt; Forum: 100 RPD | unbelegt | Drittmessung s. o. | keine |
| Länge/Größe | – | sync 1 Min., Batch 1 Std. (20 Min. Wortzeit), Stream 5 Min., 10 MB lokal | 1 Std. (30 Min.); API-Inline 20 MB; Vertex 15 Min. | Sitzung 10 Min. | 9,5 h, inline 20 MB | kurzer Diktat-Ton, Segmentmodus API 33 |
| Streaming | – | ja | nein | ja (WebSocket) | nein (Live-Dialogmodelle gesondert) | ja |
| Vokabular | – | Phrase Sets bis 1.000; Custom Prompt (Preview) | `custom_vocabulary` bis 1.000 (best ca. 100) | ja | freier Prompt | `EXTRA_BIASING_STRINGS` (API 33) |
| Halluzination/Umformulierung | – | Chirp-Familie: Text bei Stille/Rauschen (Einzelbericht) | Smart-Modus lässt aus, Verbatim setzen; Stille ok (HN) | 403 bei exakt 20 s Stille (HN) | LLM: höheres Umformulierungsrisiko | unbekannt |
| Datenresidenz | – | `eu` Multi-Region GA | AI Studio: keine Wahl; Vertex: `eu` | wie links | AI Studio: keine Wahl | Gerät |
| Latenz | AA Speed 200,8 | AA Speed 28,4 | eigen 4,4 s / 64,5 s; AA Speed 89,5 | eigen 15,1 s / 64,5 s | AA: 3.1 FL Minimal 89,2 | lokal |
| Android-Anbindung | Bearer-Key | **OAuth cloud-platform nötig, kein API-Key** | Interactions-REST mit API-Key | WebSocket mit API-Key | `generateContent` mit API-Key | Framework-API, kein Netz |

---

## 5. Fazit

**Besser als Groq Turbo für Deutsch? — Cloud STT `chirp_3`: nein. `gemini-3.5-transcribe`: unklar, wahrscheinlich ja (richtungsgleich), aber nicht belegt. Flash-Lite: unklar. Android On-Device: nein/unklar.**

Warum:
- **`gemini-3.5-transcribe`**: AA-WER 2,6 % gegen 4,6 % bei Turbo (englisch), und Google meldet auf FLEURS Verbesserungen gegenüber Chirp 3. Für Deutsch gibt es aber keine veröffentlichte, belastbare Zahl; der einzige deutsche Gemini-Vergleich (Voxtral-Paper) betrifft Gemini 2.5 Flash und zeigt auf Common Voice-Deutsch sogar *schlechtere* Werte als Whisper large-v3. Plausibel besser, wäre aber erst mit einem eigenen A/B-Test an Franks echten Diktaten (Eigennamen, Zahlen) zu bestätigen. Nachteile gegenüber Turbo: ca. 7,5× Preis, **Free-Tier-Limits undokumentiert** (Forum: 100 RPD), Free-Tier-Bedingungsfrage im EWR (siehe Klausel 2), `verbatim` zwingend, Latenz höher als Groq.
- **`chirp_3`**: AA 4,3 % gegen 4,6 % marginal, 24× Preis, **kein v2-Freikontingent**, OAuth-Pflicht statt API-Key, Halluzinationsverdacht bei Stille. Nur sinnvoll, wenn EU-Datenresidenz (`eu`) zwingend ist.
- **Flash-Lite mit Prompt**: billig und die einzigen Gemini-Modelle mit belegbar großem Free-Tier (Drittmessung 500 RPD), freier Prompt für Eigennamen — aber LLM-Umformulierungsrisiko und keine deutsche WER; als zweiter Versuch im A/B-Test sinnvoll, nicht als Ersatz.
- **On-Device**: kostenlos und offline, aber de-DE nur beta, ML-Kit-Advanced nur Pixel 10/11, Qualität unmessbar belegt, Sprachpaket muss aktiv per `triggerModelDownload` geholt werden. Taugt als Offline-Notnagel, nicht als Ersatz.

Empfehlung für den nächsten Schritt: A/B-Messung `gemini-3.5-transcribe` (verbatim, `de-DE`, kleine Vokabelliste) gegen Turbo auf 30–50 echten Diktaten; dazu Franks AI-Studio-Seite öffnen und die tatsächlichen RPM/RPD für beide Transcribe-Modelle und Flash-Lite ablesen (https://aistudio.google.com/rate-limit). EWR-Klausel mit Rechtsabwägung entscheiden, bevor ein Free-Tier-Schlüssel an Dritte ausgeliefert wird.

---

BEST-PRACTICES-KANDIDATEN:
- Audio-Token-Rate hängt vom Modell ab: 32 Token/s für allgemeine Gemini-Modelle (https://ai.google.dev/gemini-api/docs/audio), 25 Token/s für `gemini-3.5-transcribe(-live)` (https://ai.google.dev/gemini-api/docs/pricing); Kosten je Minute daraus rechnen, nicht pauschal 32 annehmen. Stand 21.10.2026.
- Gemini-API-Bedingungen, zwei getrennte EWR-Klauseln: Paid-Datenregeln gelten auch im Free Tier (kein Training), aber „use only Paid Services when making API Clients available to users in the EEA, Switzerland, or the UK" (https://ai.google.dev/gemini-api/terms, Stand 2026-04-28).
- Cloud STT v2: `recognize` braucht OAuth-Scope `cloud-platform`, keinen API-Key; Modellnamen v1 `latest_long` = v2 `long` (https://docs.cloud.google.com/speech-to-text/docs/reference/rest/v2/projects.locations.recognizers/recognize, Stand 21.10.2026).
- Cloud STT v2 hat laut Preisseite keine Freiminuten; die 60 Freiminuten gelten nur für v1 (https://cloud.google.com/speech-to-text/pricing, SKU-IDs `67F5-A183-E319` / `60AE-2FE3-C3D8` vs. v2 `3099-B70F-0949`).
- Android: `createOnDeviceSpeechRecognizer`/`isOnDeviceRecognitionAvailable` ab API 31, `triggerModelDownload` ab API 33; deutsches Paket aktiv herunterladen, sonst `ERROR_LANGUAGE_UNAVAILABLE` (https://developer.android.com/reference/android/speech/SpeechRecognizer; https://github.com/kosimk/murmel/pull/2).
- Quellenskepsis: autointerviewai.com-FLEURS-Tabelle nicht zitieren (Durchschnitte stimmen nicht, kein Primärbezug).

BUG-KANDIDATEN:
- `gemini-3.5-transcribe`: API erzwingt 100 RPD auch auf Tier 2, obwohl 10.000 RPD angezeigt werden; Google 12.10.2026 „should have been resolved", unbestätigt (https://discuss.ai.google.dev/t/tier-2-gemini-3-5-transcribe-shows-10-000-rpd-but-api-enforces-100-rpd/183223; Stand 21.10.2026, Modell `gemini-3.5-transcribe`).
- `gemini-3.5-transcribe`: genau 20 Sekunden Stille führen zu HTTP 403 (Einzelbericht, https://news.ycombinator.com/item?id=49468818, 2026).
- Smart-Modus (`transcription_config.mode = smart`) lässt Wörter/Teilsätze weg und setzt zufällige Satzumbrüche (HN-Berichte Crystalin/jeffbee; zusätzlich Eigenmessung Almanach §K31): `verbatim` setzen.
- Gemini 3 Flash / 3.1 Pro: progressiver Timestamp-Drift bei Audiotranskription (https://discuss.ai.google.dev/t/bug-gemini-3-flash-and-3-1-pro-progressive-timestamp-drift-in-audio-transcription/129501, 2026).
- Android On-Device: Systempaket nur `en-US`, Deutsch scheitert mit `ERROR_LANGUAGE_UNAVAILABLE` bis `triggerModelDownload` (Fairphone 6, Android 16; https://github.com/kosimk/murmel/pull/2, Oktober 2026).
- Gemini 3.5 Transcribe Live: erkannte Schwedisch als Niederländisch (Einzelbericht simedw, https://news.ycombinator.com/item?id=49468818).
