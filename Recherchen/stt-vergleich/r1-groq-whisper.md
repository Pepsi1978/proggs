# R1 — Groq und die Whisper-Familie als Basislinie (Stand 21.10.2026)

Researcher R1 der STT-Vergleichsstudie. Basislinie: Groq `whisper-large-v3-turbo`. Ergänzt (dupliziert nicht) `best-practices/desktop/groq-transkription.md` und `bugs/desktop/groq-transkription.md` (Stand dort 02.07./11.09.2026).
Kennzeichnung: **[belegt]** = Primärquelle mit URL, **[sekundär]** = Blog/Aggregator/Selbstangabe, **[unbelegt]** = nicht gefunden. Vom Tool gelieferte Zusammenfassungen wurden nicht überprüft, soweit nicht anders vermerkt; die PDF-Tabellen (Canary-Paper) habe ich selbst per pdftotext gelesen.

---

## 1. Groq whisper-large-v3-turbo (BASISLINIE)

- **Modell-ID:** `whisper-large-v3-turbo`
- **Status:** Production, GA. Auf der Modellliste unter „Production" geführt [belegt: https://console.groq.com/docs/models]. Kein Abschaltdatum; Deprecations-Seite nennt unter Audio nur `distil-whisper-large-v3-en` (abgeschaltet 23.08.2025, Ersatz = turbo), `playai-tts`, `playai-tts-arabic` [belegt: https://console.groq.com/docs/deprecations]. Kein neues STT-Modell in den Groq-Changelogs 2026 (nur TTS: Jan 30. und Apr 18. 2026, Orpheus) [belegt: https://console.groq.com/docs/changelog]. Eine Suche nach neuen 2026er-STT-Modellen bei Groq (Voxtral, Parakeet, Canary, „v4") fand nichts [sekundär: Suchergebnis, kein Beleg für Abwesenheit].
- **Aufbau:** Von OpenAI als „finetuned version of a pruned Whisper large-v3" beschrieben, Decoder-Schichten 32 → 4, „minor quality degradation"; zwei weitere Epochen auf denselben Multilingual-Daten ohne Übersetzungsdaten, nicht destilliert [belegt: https://huggingface.co/openai/whisper-large-v3-turbo, https://github.com/openai/whisper/discussions/2363]. Groq betreibt es mit „TruePoint Numerics" (reduzierte Präzision „nur dort, wo es die Genauigkeit nicht berührt") [belegt: https://console.groq.com/docs/model/whisper-large-v3-turbo].
- **Deutsch-Qualität (WER) — Achtung: die Groq-Zahl „12 %" ist kein Deutsch-Wert.**
  - Groq-Doku: 12 % WER, Quelle ArtificialAnalysis Stand 20.01.2025, Sprache nicht genannt [belegt: https://console.groq.com/docs/speech-to-text, https://cdn.sanity.io/files/chol0sk5/production/fd29963b63017f46208bf46c165bfd5c55683f7d.pdf]. Artificial Analysis misst inzwischen anders (AA-WER 3 Datensätze AgentTalk/VoxPopuli/Earnings22, Sprachen unbelegt): Groq-Turbo 4,6 %, Preis 0,67 $/1000 min, Speed 200,8x [belegt: https://artificialanalysis.ai/speech-to-text, kein Datum auf der Seite]. Für Deutsch sind beide Werte nicht aussagekräftig.
  - Turbo, Deutsch, drei Benchmarks (Spalte „Original" der TheStageAI-Modellkarte, mutmaßlich unverändertes OpenAI-Turbo, Herkunft der Messung nicht erklärt): CoVoST2-de 4,34 / FLEURS-de 4,36 / MLS-de 3,77 [sekundär: https://huggingface.co/TheStageAI/thewhisper-large-v3-turbo].
  - Turbo laut primeLine-Modellkarte (Selbstangabe): Tuda-De 8,300 / Common Voice 19.0 3,849 / MLS 3,203 / Gesamt 3,649 [sekundär: https://huggingface.co/primeline/whisper-large-v3-turbo-german].
  - OpenAI sagt selbst: Turbo ≈ large-v2, auf FLEURS besser als auf Common Voice (sauberere Aufnahmen), größere Einbußen bei Thai/Kantonesisch; für Deutsch keine eigene Zahl [belegt: https://github.com/openai/whisper/discussions/2363].
- **Freikontingent (exakt):** Free Plan, Organisations-weit: **20 RPM, 2.000 RPD, 7.200 Audio-Sekunden/Stunde (ASH), 28.800 Audio-Sekunden/Tag (ASD)** = max. 2 h Audio pro Stunde, 8 h pro Tag [belegt: https://console.groq.com/docs/rate-limits — die Seite zeigt Tabellen für „Free" und „Developer"; die abgerufene Zusammenfassung ordnete die Zahlen nicht eindeutig dem Free-Tab zu; übereinstimmend nennen sie Drittquellen für Free: https://spokenly.app/blog/free-speech-to-text-apis, https://toolfreebie.com/?p=237 [sekundär]]. Genaue Werte im eingeloggten Limits-Dialog (console.groq.com/settings/limits). Developer-Tier-Werte für Whisper: [unbelegt].
  - Dateigröße Free 25 MB, Dev 100 MB; größere per `url`-Parameter [belegt: https://console.groq.com/docs/speech-to-text]. Die Modellseiten nennen pauschal „100 MB".
  - Voraussetzung/Kreditkarte: [unbelegt] (Drittquellen: keine Karte nötig, sekundär).
  - **Datennutzung zum Training:** Laut Groq Services Agreement 4.2: „Groq is not permitted to use Inputs or Outputs for training or fine-tuning any AI Model Services or other models", außer mit ausdrücklicher Erlaubnis; keine Sonderregel für Free [belegt: https://console.groq.com/docs/legal/services-agreement, Zusammenfassung des Tools]. Die Seite „Your Data" selbst äußert sich zum Training nicht [belegt: https://console.groq.com/docs/your-data]. Beta-Dienste sind ausgenommen (Whisper ist Production).
  - **EWR-Einschränkungen:** Keine regionale Sperre gefunden. EWR/Schweiz-Kunden vertragen mit Groq UK Limited, englisches Recht [belegt: Services Agreement, s. o.]. Mindestalter 18. Nutzungsbeschränkung des Free Tier für EWR: [unbelegt].
- **Preis je Audiostunde:** 0,04 $/h (Min-Abrechnung 10 s/Request, sonst sekundengenau) [belegt: https://console.groq.com/docs/models, /speech-to-text]. Batch-API: 50 % Rabatt (≈ 0,02 $/h), eigene Limits, Fenster 24 h–7 Tage, max. 50.000 Zeilen/200 MB, `/v1/audio/transcriptions` unterstützt [belegt: https://console.groq.com/docs/batch].
- **Rate Limits:** siehe Freikontingent. Rate-Limits gelten pro Organisation, nicht pro Key [sekundär].
- **Max. Datei/Länge:** 25 MB Free / 100 MB Dev; Formate flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm; nur erste Tonspur; intern 16 kHz mono; Mindestlänge 0,01 s, Mindestabrechnung 10 s; „optimiert für 30-s-Segmente", Dateien < 30 s werden aufgefüllt (Groq-PDF); Groq ist laut PDF „am schnellsten bei Dateien über 4 Minuten" [belegt: o. g. Doku + PDF].
- **Streaming/Echtzeit:** Nein. „File-based transcription only — no streaming support" [belegt: Groq ASR Model Guide v3.0, Cloud-PDF s. o., Stand Anfang 2025]; die aktuelle Doku erwähnt kein Streaming. Echtzeit-Eindruck nur über kurze VAD-Chunks.
- **Vokabular/Prompting/Keyterms:** `prompt` max. **224 Token** (Stil/Schreibweise, keine Befehle); `language` ISO-639-1; `temperature` 0–1 (Default 0); `response_format` json/verbose_json/text; `timestamp_granularities[]` segment und/oder word (nur mit verbose_json) [belegt: https://console.groq.com/docs/speech-to-text]. Kein Keyterm-/Boost-Feature. OpenAI-Cookbook: nur die letzten 224 Token zählen, Prompting für Namen „nicht zuverlässig, aber teils nützlich" [belegt: https://developers.openai.com/cookbook/examples/whisper_prompting_guide]. Wirksamkeit speziell für deutsche Eigennamen: [unbelegt] (Frank sollte selbst messen).
- **Halluzinationsverhalten:** Whisper-inhärent: Floskeln bei Stille („Vielen Dank", „Thank you", „Untertitel des ZDF"), Wiederholungsschleifen, Fensterdrift (siehe bestehender Almanach). Neu belegt: Ursache Untertitel-Trainingsdaten und selbstverstärkender Kontext zwischen 30-s-Fenstern; ca. ¼ der Nicht-Sprach-Halluzinationen = „thank you" (arXiv 2501.11378) [sekundär: https://yage.ai/share/whisper-repetition-hallucination-en-20260526.html]. Für Turbo berichten Nutzer in der Release-Diskussion Looping, kaputte Ausgaben, `no_speech_prob` verhält sich „nicht wie erwartet", Übersetzung funktioniert nicht (kein Übersetzungstraining), Zufallsworte bei Japanisch [sekundär, Einzelberichte: https://github.com/openai/whisper/discussions/2363]. Groq-spezifische GitHub-Issues zu Turbo-Halluzination: keine gefunden. Studie zur Halluzination über >20 ASR-Modelle empfiehlt HER (Halluzinationsrate) neben WER; kein Turbo-Vergleich für Deutsch gefunden [belegt: https://arxiv.org/abs/2502.12414].
- **Datenschutz/Datenresidenz:** Standardmäßig keine Speicherung für Inferenz; Logs von Ein-/Ausgaben bis 30 Tage möglich (Zuverlässigkeit/Missbrauch); Zero Data Retention in Console für alle Kunden schaltbar (Batch/Fine-Tuning dann aus); Audio-Endpunkte unterliegen denselben Regeln; Kundendaten liegen in GCP-Buckets in den USA, EU-Transfer über SCC [belegt: https://console.groq.com/docs/your-data]. Verarbeitungsort der Anfragen: [unbelegt]. Kontext: Nvidia-Lizenzdeal Dez. 2025 (≈ 20 Mrd. $), GroqCloud laufe weiter, Preise unverändert [sekundär: https://www.voiceflow.com/blog/groq, https://www.cloudzero.com/blog/groq-pricing/]; Führungsangaben widersprüchlich.
- **Latenz:** Speed-Faktor 216x (Groq-Modellseite), 200,8x (Artificial Analysis, Median der letzten 7 Tage, 10-min-Audio) [belegt: https://console.groq.com/docs/model/whisper-large-v3-turbo, https://artificialanalysis.ai/speech-to-text]. Für kurze Diktate (5–15 s) dominieren Netzwerk/Upload; eigene Messung vorhanden (Almanach §3.7).
- **Android-Integration (REST):** `POST https://api.groq.com/openai/v1/audio/transcriptions`, Header `Authorization: Bearer <KEY>`, multipart/form-data: `file` (WAV/FLAC), `model=whisper-large-v3-turbo`, `language=de`, `response_format=verbose_json`, `temperature=0`, `prompt=<interpunktierter dt. Satz mit Eigennamen>`. OpenAI-kompatibel. Fehler 429 mit `retry-after`.

---

## 2. Groq whisper-large-v3

- **Modell-ID:** `whisper-large-v3`
- **Status:** Production; Seite nennt keinen Status oder Abschaltdatum; 1550 M Parameter, TruePoint Numerics [belegt: https://console.groq.com/docs/model/whisper-large-v3].
- **Deutsch-Qualität:**
  - Groq-Doku: 10,3 % WER (Stand Jan. 2025, Quelle Artificial Analysis, englisch/gemischt, kein Deutsch); Modellseite: 8,4 % Kurzform, 10,0 % sequenzielle Langform, 11,0 % gechunkt, Datensätze nicht genannt [belegt: https://console.groq.com/docs/model/whisper-large-v3].
  - **FLEURS-de: 4,30**, CoVoST2-de: 5,94, (MLS enthält kein Deutsch) [belegt, selbst aus PDF-Tabellen 11/13 gelesen: Canary-1B-v2/Parakeet-v3-Paper https://arxiv.org/pdf/2509.14128].
  - Open ASR Leaderboard (v4 des Papers, Stand 27.03.2026): Deutsch **4,26** (Mittel aus CoVoST-2 und FLEURS), mehrsprachiger Schnitt 4,81, RTFx 111 [belegt: https://arxiv.org/html/2510.06961v4]. Turbo ist in der Mehrsprachig-Tabelle NICHT enthalten.
  - primeLine-Selbstangabe: Tuda-De 7,884 / Common Voice 19.0 3,484 / MLS 2,832 / Gesamt 3,279 [sekundär: https://huggingface.co/primeline/whisper-large-v3-turbo-german].
  - Dialekt-Studie: large-v3 bestes Whisper-Modell, Turbo „nur sehr kleiner" Rückgang (Standarddeutsch-Teil nicht separat ausgewertet) [belegt: https://arxiv.org/pdf/2506.02894].
- **Freikontingent:** identisch zu Turbo (20 RPM / 2.000 RPD / 7.200 ASH / 28.800 ASD) [belegt: https://console.groq.com/docs/rate-limits]. Hinweis: Audio-Sekunden werden pro Organisation gezählt; beide Modelle teilen vermutlich NICHT dasselbe Budget (Tabelle führt sie getrennt) — ob getrennt oder gemeinsam gezählt wird, ist [unbelegt].
- **Preis:** 0,111 $/h (≈ 2,8-fach Turbo); Batch −50 % [belegt: https://console.groq.com/docs/models].
- **Rate Limits / Datei / Streaming / Prompting / Datenschutz / Android:** wie Turbo. Zusätzlich: **Übersetzung → Englisch** nur mit large-v3 (`/audio/translations`, nur `language=en`) [belegt: https://console.groq.com/docs/speech-to-text]. Langform: sequenziell genauer als gechunkt.
- **Halluzination:** Almanach: `large-v3` halluziniert bei Stille stärker als v2/medium. In der Turbo-Release-Diskussion berichten Nutzer mehr Schleifen bei v3 als bei v2 [sekundär: https://github.com/openai/whisper/discussions/2363]; anekdotisch: Turbo ging mit Eigennamen/Akronymen auf verrauschtem Audio besser um als v3 (Einzelbericht, ebd.).
- **Latenz:** Groq-Seite 189x (nicht mehr 164x/300x wie in älteren Dokus) [belegt: https://console.groq.com/docs/speech-to-text]; AA-Messung für Groq-v3 nicht gelistet.

---

## 3. Groq distil-whisper-large-v3-en

- **Status: abgeschaltet** am 23.08.2025, Ersatz `whisper-large-v3-turbo` [belegt: https://console.groq.com/docs/deprecations]. Nur Englisch; war ohnehin für Deutsch ungeeignet. Aus der Studie streichen.

---

## 4. Deutsche Finetunes und Whisper-Nachfolger (außerhalb Groq)

### 4.1 primeline/whisper-large-v3-turbo-german
- **ID:** `primeline/whisper-large-v3-turbo-german` (0,8 B, Apache-2.0, Finetune von `primeline/whisper-large-v3-german`; Trainingsdaten flozi00/asr-german-mixed; Autor Florian Zimmermeister, „Forschung, kein kommerzielles primeLine-Produkt, Nutzung auf eigenes Risiko") [sekundär: https://huggingface.co/primeline/whisper-large-v3-turbo-german].
- **WER (Selbstangabe, niedriger = besser):**

| Datensatz | OpenAI Turbo | OpenAI large-v3 | primeline v3-german | CrisperWhisper | **primeline Turbo-german** |
|---|---|---|---|---|---|
| Tuda-De | 8,300 | 7,884 | 7,711 | 5,148 | 6,441 |
| Common Voice 19.0 | 3,849 | 3,484 | 3,215 | 1,927 | 3,200 |
| Multilingual LibriSpeech | 3,203 | 2,832 | 2,129 | 2,815 | 2,070 |
| Gesamt | 3,649 | 3,279 | 2,734 | 2,662 | 2,628 |

  Relativ zum OpenAI-Turbo: −28 % Gesamt-WER, Tuda −22 %, CV19 −17 %, MLS −35 %. Vorbehalt: Selbstangabe, kein unabhängiger Nachtest gefunden; Common Voice kann im Trainingsmix stecken (Datenüberlappung nicht geprüft) [unbelegt].
- **Verfügbarkeit als API:** Keine. Hugging Face: „No Inference Provider currently hosts the model" (~142.000 Downloads/Monat). Nur lokal (Transformers, CT2-Ableger wie `jimmymeister/whisper-large-v3-turbo-german-ct2`) oder eigener Endpoint [sekundär: Modellkarte; Community-Anfragen nach Hosting: https://huggingface.co/spaces/huggingface/InferenceSupport/discussions/4733]. Bei Groq: nicht verfügbar; Groq erwähnt Fine-Tuning nur für „dedizierte Instanzen"/Enterprise (Groq-PDF), Kosten [unbelegt].
- **Datenschutz bei Eigenbetrieb:** komplett lokal möglich (Apache-2.0). Latenz hängt von der Hardware ab (0,8 B, auf Mittelklasse-GPU in Echtzeit, auf dem Handy nicht praktikabel) [unbelegt, Einschätzung].
- Streaming/Prompting: wie Whisper (`prompt` möglich); Halluzination: Whisper-typisch, kein Gegenbeweis gefunden.

### 4.2 CrisperWhisper (nyrahealth/nyralabs)
- **ID:** `nyrahealth/CrisperWhisper` (≈ 2 B, F16), Deutsch + Englisch, Paper INTERSPEECH 2024 (29.08.2024). **Lizenz CC BY-NC 4.0 — nicht kommerziell.** v1 als „deprecated" markiert, Nachfolger **CrisperWhisper 2.0** (Org `nyralabs`, Turbo-Variante unter „Nyra Health Non-Commercial Research License": kein Produktiv-/Betriebseinsatz) [sekundär: https://huggingface.co/nyrahealth/CrisperWhisper, Suchergebnis zu https://huggingface.co/Masterx/CrisperWhisper2.0-turbo-ONNX].
- **Besonderheiten:** transkribiert wörtlich inkl. Füllwörter/Stottern (für Diktat eher nachteilig), präzisere Wort-Zeitstempel, trainiert mit Nur-Rauschen-Samples → leere Ausgabe statt Halluzination („minimiert Halluzinationen").
- **WER:** Deutsch laut primeLine-Tabelle: Tuda-De 5,148, CV19 1,927, MLS 2,815 (Gesamt 2,662, bestes Modell auf Tuda/CV). Englisch-Mittel 6,66 vs. 7,7 für v3 [sekundär, Selbstangaben].
- **API:** keine gehostete API (HF: kein Inference Provider). Nur lokal [sekundär: Modellkarte].
- **Fazit für Frank:** Lizenz schließt kommerzielle Nutzung aus; für private Eigennutzung theoretisch möglich, aber ohne API nicht für Android-Cloud nutzbar.

### 4.3 Whisper Large v3 / Turbo bei anderen Anbietern (Free-Tier-Relevanz)
- **Cloudflare Workers AI** `@cf/openai/whisper-large-v3-turbo`: 0,0005 $/Audiominute (46,63 Neurons/min); Workers-Free-Plan **10.000 Neurons/Tag ≈ 214 Audiominuten/Tag (≈ 3,6 h)**, Reset 00:00 UTC, keine Zahlungsmethode nötig, harte Obergrenze ohne Upgrade [belegt: https://developers.cloudflare.com/workers-ai/platform/pricing/; 214 Minuten = eigene Rechnung]. Deutlich weniger als Groq (8 h/Tag), aber unabhängiger Fallback. Datenschutz/Datenresidenz bei Workers AI: [unbelegt].
- **Together AI** Whisper Large v3: AA-WER 4,5 %, 1,50 $/1000 min, 294x; **fal.ai** Whisper Large v3: AA-WER 4,1 %, 1,15 $/1000 min; Wizper (fal) 4,7 %, 0,50 $/1000 min; Telnyx Turbo: 5,5 %, 15 $/1000 min [belegt: https://artificialanalysis.ai/speech-to-text; Sprachen des Benchmarks unbelegt]. Freikontingent dieser Anbieter: [unbelegt] (anderen Researchern überlassen).
- **DeepInfra** Turbo: 0,0002 $/min (≈ 0,012 $/h) [sekundär: Suchergebnis zu https://deepinfra.com/openai/whisper-large-v3-turbo/api]. **Fireworks** `whisper-v3-turbo`: Preis nicht verifiziert [unbelegt: https://fireworks.ai/models/fireworks/whisper-v3-turbo].
- **Bessere Nicht-Whisper-Modelle für Deutsch** (Open ASR Leaderboard Mehrsprachig, Deutsch, Stand 27.03.2026; CoVoST-2/FLEURS-Mittel; niedriger = besser): ElevenLabs Scribe v2 2,27 · AssemblyAI Universal 3 Pro 2,34 · Speechmatics Enhanced 2,84 · Mistral Voxtral Small 24B 3,01 · Cohere Transcribe 3,84 · Phi-4 MM 3,96 · NVIDIA Canary-1B-v2 4,10 · Qwen3-ASR 1,7B 4,12 · Parakeet TDT 0,6B v3 4,20 · **Whisper Large v3 4,26** [belegt: https://arxiv.org/html/2510.06961v4]. Details und Freikontingente gehören zu den anderen Researchern; hier nur zur Einordnung der Basislinie. FLEURS-de im selben Paper-Umfeld: Voxtral-Mini 4,62, Canary 4,40, Parakeet 5,04 vs. large-v3 4,30 [belegt: arXiv 2509.14128 Tab. 11] — dort schlägt large-v3 auf FLEURS-de die kleinen NVIDIA/Voxtral-Modelle.

---

## 5. Tabelle „Kerndaten"

| Modell | ID | Status | Deutsch-WER (Quelle) | Preis/h | Freikontingent | Streaming | API verfügbar |
|---|---|---|---|---|---|---|---|
| Groq Turbo (Basislinie) | `whisper-large-v3-turbo` | GA | FLEURS-de 4,36 / CoVoST2 4,34 / MLS 3,77 (sek.); Tuda 8,30 / CV19 3,85 (primeLine) | 0,04 $ (Batch 0,02) | 20 RPM, 2.000 RPD, 7.200 ASH, 28.800 ASD | nein | ja (Groq, OpenAI-kompatibel) |
| Groq large-v3 | `whisper-large-v3` | GA | FLEURS-de 4,30 / CoVoST2 5,94 (Canary-Paper); Leaderboard-de 4,26; Tuda 7,88 / CV19 3,48 (primeLine) | 0,111 $ (Batch 0,056) | wie Turbo (getrennte Zeile) | nein | ja |
| Groq distil-large-v3-en | `distil-whisper-large-v3-en` | abgeschaltet 23.08.2025 | nur EN | — | — | — | nein |
| primeline Turbo-German | `primeline/whisper-large-v3-turbo-german` | HF-Modell, Apache-2.0 | Tuda 6,44 / CV19 3,20 / MLS 2,07 / Gesamt 2,63 (Selbstangabe) | Eigenbetrieb | n/a | nur Eigenbau | keine gehostete API |
| CrisperWhisper | `nyrahealth/CrisperWhisper` (v2: nyralabs) | HF, CC BY-NC | Tuda 5,15 / CV19 1,93 / MLS 2,82 (Selbstangabe) | Eigenbetrieb | n/a | nur Eigenbau | keine gehostete API, nicht kommerziell |
| Cloudflare Workers AI Turbo | `@cf/openai/whisper-large-v3-turbo` | GA | wie Turbo | 0,03 $ (0,0005 $/min) | 10.000 Neurons/Tag ≈ 214 min | nein (unbelegt) | ja |

---

## 6. Fazit „Besser als Turbo für Deutsch?"

- **Groq `whisper-large-v3` — unklar, tendenziell ja, aber klein:** Auf sauberem Lesetext (FLEURS-de) praktisch gleichauf (4,30 vs. 4,36); auf Common Voice, Tuda-De und MLS ist large-v3 laut primeLine-Selbstangabe ca. 6–11 % relativ besser (Gesamt 3,279 vs. 3,649). Kosten 2,8×, Latenz leicht höher. Das Freikontingent ist formal identisch; ob beide Modelle sich ein Audio-Budget teilen, ist unbelegt. Einen Nutzen speziell für Eigennamen oder Zahlen belegt keine gefundene Quelle. Als Zweitversuch bei Fehltranskription denkbar (A/B in Franks Pipeline messen).
- **primeline/whisper-large-v3-turbo-german — ja, deutlich besser in der Selbstmessung (−28 % WER vs. Turbo), aber nicht als API verfügbar** (nur Eigenbetrieb/eigener Endpoint). Für Android/Overlays nur mit eigenem Server; sonst nicht nutzbar. Unabhängige Bestätigung und Messung auf Diktat/Eigennamen/Zahlen fehlen.
- **CrisperWhisper — Deutsch-WER am niedrigsten der Whisper-Familie, aber nicht kommerziell lizenziert, ohne API, wörtlich (Füllwörter) — für Diktat ungeeignet.**
- **Groq-Turbo bleibt die einzige Whisper-Variante mit großem Freikontingent (8 h/Tag) in einer API.** Eine Alternative mit klar besserer Deutsch-WER findet sich nur bei Nicht-Whisper-Modellen (ElevenLabs Scribe v2 2,27, AssemblyAI U3 Pro 2,34, Voxtral Small 3,01 vs. large-v3 4,26 im Open ASR Leaderboard) — deren Freikontingente prüfen die Researcher R2 ff.
- **Wichtig:** Alle Deutsch-Werte sind Lesesprache-Benchmarks (FLEURS, MLS, Common Voice, CoVoST2). Keine gefundene Quelle misst Whisper-Turbo auf deutschen Diktaten mit Eigennamen/Zahlen. Die Groq-Angabe „WER 12 %" ist ein Englisch-/Mischwert aus Anfang 2025 und taugt nicht als Deutsch-Basislinie.

---

## 7. Lücken (unbelegt)

- Developer-Tier-Rate-Limits für Whisper bei Groq; ob Free Tier Kreditkarte verlangt; EWR-spezifische Free-Sperre.
- Verarbeitungsort der Groq-Inferenz (EU/US).
- Unabhängiger Turbo-vs-v3-Deutsch-Vergleich auf Common Voice/Tuda außerhalb primeLine; Turbo-Halluzinationsrate im Deutschen.
- Wirksamkeit des `prompt` für deutsche Eigennamen bei Groq-Turbo.
- Open ASR Leaderboard enthält Turbo nicht in der Mehrsprachig-Tabelle (Tabelle 4).

---

BEST-PRACTICES-KANDIDATEN:
1. Groq-WER-Angaben (10,3 %/12 %) sind Englisch-/Misch-Werte von Anfang 2025, nicht für Deutsch verwenden; Deutsch-Basislinie: FLEURS-de ≈ 4,3 (v3) bzw. 4,36 (Turbo). Quelle: https://console.groq.com/docs/speech-to-text, https://arxiv.org/pdf/2509.14128, https://huggingface.co/TheStageAI/thewhisper-large-v3-turbo. Betroffen: `best-practices/desktop/groq-transkription.md` §2 Tabelle (Spalte „WER"; Speed 164x → 189x für v3, 216x Turbo). Version: Groq-Doku Stand 21.10.2026.
2. Datenschutz-Fakten für Groq nachtragen: Services Agreement 4.2 (kein Training mit Inputs/Outputs), Logs bis 30 Tage, ZDR in Console, Daten in GCP USA. Quelle: https://console.groq.com/docs/legal/services-agreement, https://console.groq.com/docs/your-data. Version: Stand 21.10.2026.
3. Fallback-Anbieter Cloudflare Workers AI Turbo (10.000 Neurons/Tag ≈ 214 min, Free-Plan, keine Zahlungsmethode): Quelle https://developers.cloudflare.com/workers-ai/platform/pricing/. Version: Stand 21.10.2026; ergänzt BP 8 „Health/Fallback".
4. `distil-whisper-large-v3-en` ist seit 23.08.2025 abgeschaltet (Ersatz Turbo) — in der Modelltabelle streichen. Quelle: https://console.groq.com/docs/deprecations.
5. Open-Source-Deutsch-Finetunes (primeline Turbo-German: −28 % WER Selbstangabe; CrisperWhisper: CC BY-NC) sind nicht als Cloud-API verfügbar. Quelle: https://huggingface.co/primeline/whisper-large-v3-turbo-german, https://huggingface.co/nyrahealth/CrisperWhisper.

BUG-KANDIDATEN:
1. Groq-Doku widersprüchlich: Codebeispiel-Kommentar sagt `timestamp_granularities` brauche `response_format=json`, die Parametertabelle sagt `verbose_json` (Tabelle ist konsistenter und deckt sich mit dem Almanach). Quelle: https://console.groq.com/docs/speech-to-text, Stand 21.10.2026. Geringe Schwere, nur Hinweis für Almanach §3.5.
2. Groq-PDF „ASR Model Guide v3.0" behauptet „Whisper models require a minimum of 30 seconds; shorter files are padded", abgerechnet wird aber ab 10 s; Gesamt-Wirkung auf Latenz kurzer Diktate nicht gemessen. Quelle: https://cdn.sanity.io/files/chol0sk5/production/fd29963b63017f46208bf46c165bfd5c55683f7d.pdf. Kein bestätigter Bug, nur Beobachtung; Messung in Franks Pipeline vor Aufnahme in den Almanach.
3. Ansonsten KEINE neuen belegten Groq-Whisper-Bugs seit dem Stand des bestehenden Almanachs gefunden (Suche nach Groq-spezifischen Turbo-Issues: ohne Treffer).
