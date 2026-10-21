# R7 – Lokal kostenlos nutzbare TTS-Modelle mit gutem Deutsch (Stand 21.10.2026)

Researcher R7, Engine C (Sonnet-Schwarm). Alle Abrufe am **21.10.2026**. Vorarbeit vom 28.09.2026 (`Recherchen/tts-lokal-android/README.md`, `3-open-source-modelle.md`, `4-deutsch-g2p.md`) gelesen; dieser Bericht liefert das **Delta** und bestätigt oder korrigiert. Testtext: `Recherchen/tts-vergleich/testtext.md` (Text A/B).

Kennzeichnungen: **[belegt]** = Primärquelle (Modellkarte, Repo, Lizenzdatei, Paper) gelesen. **[Hersteller]** = Zahl stammt vom Anbieter selbst. **[unabhängig]** = Dritttest. **[unbelegt]** = nichts gefunden oder nicht prüfbar.

Hinweis zur Echtzeit-Angabe: Die Quellen benutzen zwei gegenläufige Konventionen. **„x RT“** = Audiosekunden pro Rechensekunde (höher = schneller). **„RTF“** = Rechensekunden pro Audiosekunde (niedriger = schneller). In jeder Zelle steht, welche gemeint ist.

---

## 1. Wichtigste Korrekturen und Neuigkeiten gegenüber dem 28.09.2026

| Punkt | Stand 28.09. | Stand 21.10.2026 | Quelle |
|---|---|---|---|
| sherpa-onnx und espeak-ng | „plant für 2.0.0 den Wegfall“ (Issue #3731 offen) | **Issue #3731 am 20.10.2026 geschlossen, PR #4055 „Remove espeak-ng and piper-phonemize“ am 20.10.2026 in `master` gemergt.** Ersatz: eingebautes Lexikon (`lexicon.txt` je Modellordner) oder externe Phonemisierung mit neuen Feldern `tokens` / `phoneme_codepoints` in `SherpaOnnxGenerationConfig`. Bestehende `.onnx`-Dateien bleiben unverändert. Maintainer am 16.10.: Release „nächste Woche“. Ob die deutschen Piper-Stimmen nach der Umstellung ohne eigenes Lexikon laufen, steht **nicht** in der PR-Seite [unbelegt]. | [Issue #3731](https://github.com/k2-fsa/sherpa-onnx/issues/3731), [PR #4055](https://github.com/k2-fsa/sherpa-onnx/pull/4055), abgerufen 21.10.2026 |
| Piper-Engine-Lizenz | „Piper-DE-Stimme thorsten CC0 kommerziell ok“ (Engine nicht erwähnt) | Die gepflegte Engine `OHF-Voice/piper1-gpl` (PyPI `piper-tts`) ist **GPL-3.0** seit v1.3.0 (Umzug nach OHF-Voice, espeak-ng eingebettet). Aktuell **v1.8.0 vom 04.09.** (2026 laut Umfeld, Jahr auf der Seite nicht ausgewiesen). Das alte MIT-Repo `rhasspy/piper` ist archiviert (06.10.2025 laut Sekundärquelle). Die **Stimme** `de_DE-thorsten-high` hat laut zwei HF-Seiten Lizenz **MIT** (Vorarbeit 28.09. nannte „Modelle Apache 2.0“ – **hiermit korrigiert**; Datensatz laut Vorarbeit CC0). | [piper1-gpl Releases](https://github.com/OHF-Voice/piper1-gpl/releases), [piper1-gpl](https://github.com/OHF-Voice/piper1-gpl), [Thorsten-Voice/Piper](https://huggingface.co/Thorsten-Voice/Piper), 21.10.2026; Archivdatum: [promptquorum](https://www.promptquorum.com/power-local-llm/piper-tts-review) (Sekundärquelle) |
| Supertonic Wartungsstatus | „archiviert/kein Support nach 31.08.2026“ | **Teilweise falsch.** 31.08.2026 ist die Abschaltung von *Voice Builder*. Das Repo wurde am **09.09.2026 archiviert (read-only)**, keine Updates/Fixes/Security-Patches. Supertonic 3 erschien am **29.04.2026** (31 Sprachen, ca. 99M Parameter). | [supertone-inc/supertonic](https://github.com/supertone-inc/supertonic), 21.10.2026 |
| Pocket TTS Deutsch | „seit April 2026“ | Kyutai-Blogpost „Pocket TTS now supports six languages“ ist auf **04.05.2026** datiert. Das GitHub-README nennt inzwischen **sieben** Sprachen (plus Niederländisch), die HF-Karte sechs. Deutsche Stimme `juergen`. Das HF-Repo `kyutai/pocket-tts` ist **gated** (Login + Bedingungen). | [Blog](https://kyutai.org/blog/2026-05-04-pocket-tts-multilingual/), [GitHub](https://github.com/kyutai-labs/pocket-tts), [HF](https://huggingface.co/kyutai/pocket-tts), 21.10.2026 |
| NeuTTS Nano German Lizenz | „other, unklar“ | **NeuTTS Open License 1.0** (Neuphonic, 11.12.2025): kostenlos für Forschung; kommerzielle Nutzung nur bei **Jahresumsatz unter 5 Mio. US$**. Außerdem gated, espeak-ng Pflicht, Ausgabe wasserzeichenmarkiert. | [neuphonic/neutts LICENSE](https://raw.githubusercontent.com/neuphonic/neutts/main/LICENSE), [HF neutts-nano-german](https://huggingface.co/neuphonic/neutts-nano-german), 21.10.2026 |
| Gradium Phonon | „proprietär, kein Open-Source-Gewicht“ | **Korrigiert.** Phonon wurde am **21.10.2026** (NVIDIA GTC Berlin) mit offenen Gewichten veröffentlicht. Aber: nur `phonon-31m-en` ist Apache-2.0 (nur Englisch). Die deutschen Modelle `phonon-40m-de` und `phonon-40m-multilingual` sind **CC BY-NC 4.0 und gated**. Details unten. | [Gradium-Blog](https://gradium.ai/blog/gradium-releases-phonon), [HF phonon-31m-en](https://huggingface.co/gradium/phonon-31m-en), [HF phonon-40m-de](https://huggingface.co/gradium/phonon-40m-de), [xn-ptts](https://github.com/gradium-ai/xn-ptts), 21.10.2026 |
| Magpie (NVIDIA) | WER-Vergleich nur aus Gradium-Quelle | Magpie TTS Multilingual (HF-Karte) ist **v2607 vom 21.07.2026**, 12 Sprachen inkl. Deutsch, NVIDIA Open Model License, Deutsch **CER 0,80 %** [Hersteller]. | [HF nvidia/magpie_tts_multilingual_357m](https://huggingface.co/nvidia/magpie_tts_multilingual_357m), 21.10.2026 |
| Kokoro-Deutsch | „offiziell kein Deutsch“ | Bestätigt: `VOICES.md` listet keine deutsche Stimme. Community-Fine-Tune `Thorsten-Voice/Kokoro` (Apache-2.0) existiert, braucht aber Forks von `misaki`/`kokoro` und **espeak-ng**. | [VOICES.md](https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md), [Thorsten-Voice/Kokoro](https://huggingface.co/Thorsten-Voice/Kokoro), 21.10.2026 |
| Orpheus Deutsch | nur Community-Modell Sunbird mit fehlenden Tokenizer-Dateien | Es gibt das **offizielle** `canopylabs/3b-de-ft-research_release` (Apache-2.0, **gated**, 3B, F32, veröffentlicht 09.04.2025, Llama-3.2-3B-Basis). | [HF](https://huggingface.co/canopylabs/3b-de-ft-research_release), 21.10.2026 |
| Neue Modelle ab 08/2026 | nicht erfasst | Liste in Abschnitt 3.9. Keines davon ist nachweislich ein zusätzlich nutzbares deutsches Modell außer Phonon-DE (NC) und Magpie v2607. | [theopenweights.com](https://theopenweights.com/categories/tts-audio), 21.10.2026 |

---

## 2. Vergleichstabelle (Modelle mit Deutsch)

Spalten: Deutsch (nativ / Klon), Lizenz Code | Gewichte, Größe, Hardware, Geschwindigkeit, Streaming, Textnormalisierung (TN), Android, Qualitätsbeleg. Quellen stehen in den Detailabschnitten (Abschnitt 3).

| # | Modell (exakter Name) | Deutsch | Lizenz Code / Gewichte (kommerziell?) | Größe / Hardware | Geschwindigkeit (Konvention) | Streaming | Deutsche TN eingebaut? | Android / ONNX | Qualitätsbeleg Deutsch |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **Piper `de_DE-thorsten-high`** (Engine `piper-tts` 1.8.0) | nativ, 1 Sprecher | Engine **GPL-3.0** (piper1-gpl); Stimme **MIT** laut HF-Karte, Datensatz CC0 laut Vorarbeit; ja kommerziell für die Stimme, Engine-Copyleft beachten | `.onnx` 114 MB; CPU reicht | RTF ca. **0,07** (CPU, AMD 6800U) [unabhängig, wulffit April 2026] = ca. 14 x RT | Satzweise (Streaming-Details unbelegt) | **Nein** (Forum: Zahlen/Abkürzungen müssen vorverarbeitet werden) | **Ja**: sherpa-onnx-Assets `vits-piper-de_DE-thorsten-{low,medium,high}`, `-emotional-medium` (fp16/int8) belegt; espeak-ng-Abhängigkeit fällt mit sherpa-onnx 2.0 weg (PR #4055) | wulffit: „outdated, klingt nach 2020“ [unabhängig] |
| 2 | **Supertonic 3** (`Supertone/supertonic-3`, ca. 99M) | nativ, 1 von 31 Sprachen | Code MIT; Gewichte **OpenRAIL-M** (kommerziell ja, mit Nutzungsverboten, u. a. **„keine medizinischen Ratschläge / Interpretation medizinischer Ergebnisse“**) | ca. 99M, ONNX; CPU; Pi/E-Reader-Demos | RTF **0,3** auf Onyx Boox Go 6 [Hersteller]; RTF 0,615 auf Intel N100 inkl. Streaming-Vorlauf [Drittprojekt, Entwicklermessung] | Satzweise Streaming via Drittprojekt; offiziell `supertonic serve` | Teilweise: Hersteller nennt Währung, Dezimalzahlen mit Einheiten; Datum/Uhrzeit **nicht** genannt. Drittprojekt (Wyoming) bringt eigene DE-Normalisierung mit | **Ja**: sherpa-onnx-Asset `sherpa-onnx-supertonic-3-tts-int8-2026-05-11` (128,8 MB); offiziell kein Android-SDK, Flutter möglich | Kein unabhängiger DE-Test gefunden; nur Herstellerangaben |
| 3 | **Qwen3-TTS** 0.6B / 1.7B (`Qwen3-TTS-12Hz-*`) | nativ als eine von 10 Sprachen; **kein deutscher Preset-Sprecher** in CustomVoice, Deutsch läuft über Sprachparameter bzw. Klon (Base) | Repo **Apache-2.0**; Gewichte: HF-Karte 0.6B-CustomVoice nennt Apache-2.0 | 0.6B ca. 0,9B Param. BF16; GPU empfohlen (CUDA, bf16); GGUF-Ports vorhanden (Q8 ca. 1 GB / 2,1 GB) | CPU 4 Threads RTF **4,3** (langsamer als Echtzeit), M4-Pro MLX 4-Bit RTF 0,2 [unabhängig, wulffit]; GGUF-CPU ca. 2,9–3,3 (RTF) [Blog] | Ja (Hersteller: erstes Paket nach 1 Zeichen, 97 ms) | **Nein** dokumentiert | Nur Community-Ports (GGUF/ONNX-Wrapper), keine ARM-Handy-Messung gefunden | **wulffit (April 2026): bestes Deutsch von 8 Modellen, „sehr gut“**; Hersteller-WER DE 1,089 (0.6B-Base) / 1,235 (1.7B-Base) |
| 4 | **Chatterbox Multilingual v3** (`ResembleAI/chatterbox`, 0,5B, Llama-Backbone) | nativ-ähnlich, Klon-Modell (Referenzclip optional laut Beispielcode) | **MIT**; Gewichte MIT; eingebettetes PerTh-Wasserzeichen | 0,5B; Beispiel `device="cuda"`, VRAM unbelegt | RTF ca. 0,2 (≈ 5 x RT) auf H100, unoptimiert [Hersteller] | Streaming-Fähigkeit unbelegt | **Nein** dokumentiert | Nein (PC-Server) | Hersteller: Deutsch **CER unter 0,20 %** (Whisper-large-v3, je 100 Samples) [Hersteller]; kein MOS; Elo 1024 Gesamt (nicht deutsch-spezifisch) |
| 5 | **Pocket TTS** (`kyutai/pocket-tts`, ca. 100M, Sprachvariante `german`; 24-Layer-Varianten) | nativ, Stimme `juergen`; Klonen per WAV möglich | Code **MIT**; Gewichte **CC-BY-4.0**, Repo **gated** | ca. 100M; CPU, 2 Kerne; Safetensors ca. 236 MB (Hauptdatei) | ca. **6 x RT** auf MacBook Air M4; **2,3–2,5 x RT** auf 4-vCPU-x86 [Hersteller]; GGUF: Frz. ca. 2,9 x RT, übrige 5,5–6 x RT [Community] | **Ja**, ca. 200 ms bis erster Chunk | **Nein** dokumentiert | Community: LiteRT-Port ca. 1 x RT auf Pixel 8a GPU; sherpa-onnx-Assets `pocket-tts-2026-01-26` (168 MB / int8 98 MB) enthalten **laut Datum nur den Januar-Stand**, Deutsch dort [unbelegt] | Kein MOS/Arena für Deutsch gefunden. Vom Hersteller Gradium-Elo-Tabelle erwähnt Pocket nicht |
| 6 | **Gradium Phonon 40m-de / 40m-multilingual** | nativ | Runtime MIT oder Apache-2.0; Gewichte **CC BY-NC 4.0, gated** → **nicht kommerziell** | 40M (Hub 50,5M); `model.q8.gguf` 74,6 MB | M5 1 Kern 20 x RT, Pi 5 1 Kern 2,2 x RT, iPhone 16 Pro (Neural Engine) 20 x RT [Hersteller] | Ja | **Ja**, sprachabhängig (`lang="de"`): Zahlen, Währung, Datum, E-Mail; **nicht** im Transformers-Tokenizer | **Ja**: Android über CPU-Bibliothek (Rust/NDK, selbst bauen) oder QNN (Snapdragon-NPU) | [Hersteller] DE-Elo 2097 (168 Rater, 2.220 Stimmen), LLM-WER 2,2 % (Hard-Set); Vergleichswerte NeuTTS 1768 / 9,3 %, Magpie 1640 / 29 %. **Kein unabhängiger Test, Modell erst heute veröffentlicht** |
| 7 | **NeuTTS Nano German** (`neuphonic/neutts-nano-german`, ca. 117M aktiv / 229M gesamt) | nativ, Klon (Referenz-WAV + Text Pflicht) | Code: Lizenzart auf README-Seite nicht genannt; Gewichte **NeuTTS Open License 1.0** (kommerziell nur unter 5 Mio. US$ Umsatz); gated | GGUF Q4/Q8, optional ONNX-Decoder; „Laptop-CPU in Echtzeit“ | Nur Token/s: Galaxy A25 CPU 45 tok/s (Q4_0, Nano) [Hersteller]; RTF unbelegt | Nur mit GGUF | Nein; **espeak-ng erforderlich** (Phonem-Eingabe) | Android-Benchmark vorhanden (Galaxy A25), Android-Support nicht erklärt | Laut Phonon-Tabelle [Hersteller Gradium]: Elo 1768, WER 9,3 %. Sonst nichts |
| 8 | **Kani-TTS `kani-tts-400m-de`** (400M, LFM2-350M-Backbone, NVIDIA NanoCodec) | nativ (Datensätze laut Karte: laion/Emolia, Thorsten-Voice TV-44kHz-Full, german_voice_cb) | Code Apache-2.0; **Gewichte widersprüchlich**: Tag `lfm1.0`, Fließtext „Apache 2.0“. LFM-Lizenz hat Umsatzgrenze (10 Mio. US$) laut Liquid-AI-Seite; NanoCodec: NVIDIA Open Model License | 400M, BF16; GPU | RTF 0,19 (RTX 5090) bis 0,60 (RTX 3060 12 GB) [Hersteller, vLLM] | unbelegt | Nein | Nein | Keine MOS/WER |
| 9 | **Magpie TTS Multilingual 357M** (v2607, NVIDIA, 364M) | nativ, 12 Sprachen, 5 Sprecher (Aria, Jason, Leo, Sofia, John Van Stan) | **NVIDIA Open Model License** (laut Karte „ready for commercial use“) | NVIDIA-GPU (L4/A10/A100/H100) unter Linux laut Karte; Community `magpie-tts.cpp` GGUF (q8_0 624 MB), CPU, Windows-Build nicht beschrieben | Nicht angegeben; Community-C++-Engine ca. 66 x schneller als NeMo-Referenz auf der CPU | Nicht beschrieben | **Ja**: Zahlen, Abkürzungen, Sonderzeichen in allen 12 Sprachen eingebaut; IPA-Eingabe möglich | Nein | **Deutsch CER 0,80 %, Sprecherähnlichkeit 0,742** [Hersteller]; Artificial Analysis Open-Weights-Elo 1063 (Magpie Feb 2026) |
| 10 | **Zonos-v0.1** (Zyphra) | nativ gelistet (5 Sprachen: EN, JA, ZH, FR, DE) | Repo **Apache-2.0** (Gewichtslizenz auf der Seite nicht eigens genannt) | 6 GB+ VRAM; Hybrid-Variante braucht RTX 3000 oder neuer; Windows nur inoffiziell; **espeak-ng** Pflicht | ca. 2 x RT auf RTX 4090 [Hersteller] | unbelegt | Nein | Nein | Artificial-Analysis-Elo 1000 (nicht deutsch-spezifisch) |
| 11 | **Kokoro-82M** + **Thorsten-Voice/Kokoro** (Fine-Tune) | Original: **kein Deutsch**; Fine-Tune: Deutsch (1 Sprecher) | Original Apache-2.0; Fine-Tune Apache-2.0 | 82M; CPU in Echtzeit laut Karte | ca. 7,3 x RT auf 1 MacBook-Kern [Hersteller Gradium]; 5 x RT auf 32-Kern-Desktop [Blog] | Ja (Kokoro generell) | Nein; Fine-Tune braucht **espeak-ng** und Forks von `misaki`/`kokoro` (sonst fehlt `de`) | ONNX-Variante und Android-Nachweise nur für Community-Pakete (Vorarbeit) | Keine MOS-Zahl; bekannter Bug `ʏ` fehlt im Vokabular (kurzes ü bricht, z. B. „Brücke“) |
| 12 | **Orpheus 3B Deutsch** (`canopylabs/3b-de-ft-research_release`, 09.04.2025) | nativ (Finetune), Klonen | Apache-2.0, **gated** | 3B, F32, grob 12 GB nur Gewichte (eigene Rechnung) | Hersteller: ca. 200 ms Streaming-Latenz | Ja | Nein | Nein | Keine Benchmarks verlinkt |
| 13 | **VibeVoice-Realtime-0.5B** | experimentelle mehrsprachige Stimmen inkl. DE | MIT; Repo-Hinweis „nur Forschung/Entwicklung, nicht kommerziell ohne weitere Tests“ | 0,5B | ca. 300 ms bis Audio [Hersteller] | Ja | Nein | Nein | wulffit: „gut, Erzähler-Ton, nur MPS brauchbar“ (VibeVoice 0.5B) |
| 14 | **F5-TTS German** (`aihpi`, `hvoss-techfak` u. a.) | Klon-Modell, Referenzaudio nötig | **CC-BY-NC-4.0** (beide geprüfte Varianten); Basismodell SWivid/F5-TTS ebenfalls cc-by-nc-4.0 | Mehrere 100 MB | wulffit: RTF 5–10 (langsam) auf CPU | n/a | Nein | ONNX-Variante `nibor1896` (Vorarbeit) | wulffit: „schlecht*“, stark referenzabhängig (Piper-Referenz unbrauchbar, menschliche Aufnahme deutlich besser) |
| 15 | **Coqui XTTS-v2** (Fork `idiap/coqui-ai-TTS`, PyPI `coqui-tts`) | Deutsch Teil von 17 Sprachen | Code **MPL-2.0** (Fork); Gewichte **CPML** (nicht kommerziell laut pinggy-Übersicht; Volltext nicht abrufbar, Coqui-Seite 404) | GPU-Beispiele; CPU möglich | RTF 1,9 (CPU, wulffit) | <200 ms (Hersteller-News) | **Teilweise im Tokenizer**: Dezimalkomma, Tausenderpunkte, Euro/Cent („20,15€“), Ordinalzahl (grammatisch falsch bei Genus), `%`→prozent, `°`→grad, 5 Abkürzungen (Dr., Fr., St., Co., Jr.); **kein** Datum, **keine** Uhrzeit; `@`→„at“ | Nein | wulffit: „mittel, solide aber nicht überzeugend“; Artificial-Analysis-Elo 916 |
| 16 | **OuteTTS 1.0 (0,6B)** | Deutsch seit v0.3 (Vorarbeit) | Apache-2.0 / MIT | 0,6B | wulffit: RTF 70 (zu langsam, Save-Crash) | n/a | Nein | GGUF | wulffit: nicht bewertet |

**Nicht (praktisch) nutzbar für Franks Zweck, mit Grund:**

| Modell | Grund | Quelle |
|---|---|---|
| Voxtral TTS (Mistral, 4B, `Voxtral-4B-TTS-2603`) | Deutsch ja, aber **CC BY-NC 4.0**; ≥16 GB VRAM; Open-Weights nur feste Stimmen; RTF 0,103 auf H200 [Hersteller] | [HF](https://huggingface.co/mistralai/Voxtral-4B-TTS-2603) |
| Fish Audio S2 Pro / OpenAudio S1-mini | Deutsch ja (Tier 2), aber Fish Audio Research License, **nicht kommerziell**; 5B | [HF s2-pro](https://huggingface.co/fishaudio/s2-pro) |
| Higgs Audio v3 (4B) | Boson Research and Non-Commercial License; v2 Lizenz „other“ (Text nicht gelesen) | [GitHub](https://github.com/boson-ai/higgs-audio) |
| IndexTTS2 / 2.5 | README nennt für 2.5 „Chinesisch, Englisch, Japanisch, Spanisch, Arabisch“, **kein Deutsch**; bilibili Model Use License | [GitHub](https://github.com/index-tts/index-tts) |
| Sesame CSM-1B | Apache-2.0, aber laut FAQ nur „some capacity“ für Nicht-Englisch, voraussichtlich schlecht | [HF](https://huggingface.co/sesame/csm-1b) |
| Kyutai TTS 1.6B (`tts-1.6b-en_fr`) | nur Englisch/Französisch | [HF](https://huggingface.co/kyutai/tts-1.6b-en_fr) |
| Phonon `31m-en` | Apache-2.0, aber nur Englisch | [HF](https://huggingface.co/gradium/phonon-31m-en) |
| VibeVoice-TTS-1.5B / 7B | Microsoft entfernte den TTS-Code am 05.09.2025; Quick-Try deaktiviert | [GitHub](https://github.com/microsoft/VibeVoice) |

---

## 3. Details je Modell (Belege)

### 3.1 Piper (`de_DE-thorsten-high`, `kerstin`, `karlsson`, `ramona`, `eva_k`, `pavoque`, `miro`, `dii`, `glados`, `thorsten_emotional`)
- **Engine:** `OHF-Voice/piper1-gpl`, GPL-3.0, „embeds espeak-ng for phonemization“; Installation `pip install piper-tts`. v1.3.0 „Change license to GPLv3“, v1.8.0 aktuell. [piper1-gpl](https://github.com/OHF-Voice/piper1-gpl), [Releases](https://github.com/OHF-Voice/piper1-gpl/releases), 21.10.2026.
- **Stimmen laut HF-Dateiliste:** `de_DE-thorsten-high.onnx` **114 MB**, `.onnx.json` 4,88 kB, Repo-Seitenleiste „MIT“. [rhasspy/piper-voices thorsten/high](https://huggingface.co/rhasspy/piper-voices/tree/main/de/de_DE/thorsten/high), 21.10.2026. Die HF-Karte `Thorsten-Voice/Piper` führt Lizenz „mit“, trainiert auf `Thorsten-Voice/TV-44kHz-Full`. [Link](https://huggingface.co/Thorsten-Voice/Piper), 21.10.2026.
- **Weitere Stimmen/Lizenzen:** siehe Vorarbeit `4-deutsch-g2p.md` (kerstin CC0, eva_k/karlsson/ramona M-AILABS, mls CC-BY 4.0, pavoque CC BY-NC-SA 4.0, nicht kommerziell). In sherpa-onnx liegen zusätzlich `dii-high`, `miro-high`, `glados*` als Assets (Lizenz dieser drei nicht geprüft [unbelegt]).
- **sherpa-onnx-Assets (GitHub-API-Abfrage am 21.10.2026, Release `tts-models`, 647 Assets, Stand 04.12.2025 für Piper-DE):** `vits-piper-de_DE-thorsten-high` 115,6 MB (fp16 58,5 MB, int8 35,1 MB), `-medium` 67,2 / 35,9 / 20,9 MB, `-low` 67,1 MB, `thorsten_emotional-medium` 80,2 MB, `kerstin-low`, `karlsson-low`, `ramona-low`, `eva_k-x_low`, `pavoque-low`.
- **Geschwindigkeit/Qualität:** wulffit.de (April 2026, AMD Ryzen 7 6800U, ohne NVIDIA): Piper RTF 0,07, Thorsten VITS 0,2, Urteil „klingt nach 2020“. [Link](https://wulffit.de/artikel/tts-deutsch/), 21.10.2026.
- **TN:** Keine eingebaute. Der Thorsten-Voice-Betreiber selbst empfiehlt in einem Home-Assistant-Thread Vorverarbeitung („Abkürzungen entfernen, Zahlen in Wortform“), Beispiel „Dr.“, Währung, Temperatur, Uhrzeit. [HA-Forum](https://community.home-assistant.io/t/piper-tts-pre-processing/826330), 21.10.2026. Für Franks Testtext A heißt das: Datum, Uhrzeit, `1.249,99 €`, `30,5 ms`, `3. Nov.` brauchen eine eigene Vorstufe (z. B. NeMo-text-processing, Apache-2.0, Deutsch laut Vorarbeit).
- **Android:** über sherpa-onnx. Die espeak-ng-Abhängigkeit wird mit PR #4055 entfernt; vorher stand dort die GPL-Falle (Vorarbeit).

### 3.2 Supertonic 3
- Veröffentlichungen: Supertonic 2 am 06.01.2026, Supertonic 3 am 29.04.2026 (31 Sprachen, ca. 99M, ONNX-Assets v2-kompatibel). Repo ist seit 09.09.2026 archiviert. [GitHub](https://github.com/supertone-inc/supertonic), 21.10.2026.
- Lizenz: Beispielcode MIT, Modell **OpenRAIL-M**. Verbotsliste (Attachment A, gelesen): u. a. Rechtsverstoß, Minderjährige, Falschinformation, **Veröffentlichung maschineller Inhalte ohne klare Kennzeichnung**, Deepfake-Imitation, automatisierte Entscheidungen, **„Provide medical advice or interpret medical results“**, Justiz-/Strafverfolgung. Kommerziell, Hosting und Weitergabe erlaubt, Verbote müssen weitergereicht werden. [LICENSE](https://huggingface.co/Supertone/supertonic-3/blob/main/LICENSE), 21.10.2026 (Zusammenfassung durch Abrufwerkzeug, kein Rechtsrat). **Relevanz für Jarvis:** Jarvis wertet Biomarker aus; ob eine vorgelesene Auswertung unter „Interpretation medizinischer Ergebnisse“ fällt, ist rechtlich zu klären [unbelegt].
- Installation: `pip install supertonic`, `TTS(auto_download=True)`, `tts.get_voice_style(voice_name="M1")`, `tts.synthesize(text, voice_style=style, lang="en")` (für Deutsch `lang="de"`). [HF-Karte](https://huggingface.co/Supertone/supertonic-3), 21.10.2026.
- TN laut README: Währungsbeträge mit Dezimalstellen, Zahlen mit Einheiten, Telefonnummern, Abkürzungen („ohne Vorverarbeitung“) – Daten/Zeiten nicht genannt, Deutsch-spezifisch nicht belegt. Drittprojekt `marco-taylor/supertonic3-tts-german-wyoming` (MIT, Rust) bringt eine eigene Deutsch-Normalisierung (Integer, Dezimal, Prozent, °C, Uhrzeiten, gültige Daten, Euro/Cent, Einheiten) und Beispiele: „21,5 °C“ → „einundzwanzig Komma fünf Grad Celsius“, „49,99 €“ → „neunundvierzig Euro und neunundneunzig Cent“. [Unraid-App](https://ca.unraid.net/apps/supertonic3-tts-german-wyoming-05cuqsm0okej76), 21.10.2026.
- RTF: 0,3 auf E-Reader [Hersteller]; Drittprojekt Intel N100 RTF 0,615, Time-to-first-audio 2,26 s, 483 MiB RAM. Das ist die einzige Zahl mit Deutsch-Text.
- Android: sherpa-onnx-Asset `sherpa-onnx-supertonic-3-tts-int8-2026-05-11` (128,8 MB). SDK-seitig kein offizieller Android-Support.
- Qualität: nur Herstellerangaben; ein Medium-Artikel nennt sie „zu verifizieren“. Kein unabhängiger Deutschtest gefunden.

### 3.3 Qwen3-TTS
- Modelle: 1,7B (Base, CustomVoice, VoiceDesign) und 0,6B (Base, CustomVoice); 10 Sprachen inkl. Deutsch; Apache-2.0 im Repo; Streaming mit 97 ms End-to-End [Hersteller]. Installation `pip install -U qwen-tts` (Python 3.12). [GitHub](https://github.com/QwenLM/Qwen3-TTS), 21.10.2026.
- CustomVoice-Sprecher (HF-Karte): Vivian, Serena, Uncle_Fu, Dylan, Eric (Chinesisch), Ryan, Aiden (Englisch), Ono_Anna (Japanisch), Sohee (Koreanisch) – **kein deutscher Sprecher**; die Karte gibt keine Deutsch-Hinweise. [HF](https://huggingface.co/Qwen/Qwen3-TTS-12Hz-0.6B-CustomVoice), 21.10.2026.
- Hersteller-WER Deutsch (Zero-Shot-Testset): 0.6B-Base 1,089, 1.7B-Base 1,235, ElevenLabs 0,572, MiniMax 1,906; Sprecherähnlichkeit 1.7B-Base 0,775 (ElevenLabs 0,614). Die Abrufqualität der Tabelle war eingeschränkt („Spaltenzuordnung unsicher“). [GitHub](https://github.com/QwenLM/Qwen3-TTS).
- Unabhängig: wulffit.de (11./12.04.2026): „Qwen3-TTS 0.6B – bestes Deutsch, saubere Lizenz“, Sprecher **Aiden** lieferte das beste Deutsch; CPU RTF 4,3 (4 Threads) bzw. 6,2 (16 Threads); MLX-4-Bit auf M4 Pro RTF 0,2. **Falle:** mlx-audio akzeptiert nur `lang_code="german"`; `"de"` fällt still auf Auto zurück und liefert Deutsch mit englischem Akzent. [Link](https://wulffit.de/artikel/tts-deutsch/).
- CPU-Ports: qwentts.cpp (Q8_0: 0,6B ca. 993 MB, 1,7B ca. 2,1 GB), `predict-woo/qwen3-tts.cpp` 1,94 (RTF) auf Ryzen 5 3600 / 4 Threads, 0,6B int8 ca. 3,07 RTF auf 4 vCPU, Spitzen-RAM ca. 3,2 GB [Blog, Einzelquelle]. [ocdevel](https://ocdevel.com/blog/20260711-quantized-cpu-tts), [HF-GGUF-Suche](https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF), 21.10.2026. Android: keine Messung gefunden.
- TN: nicht dokumentiert („robuster gegenüber verrauschtem Text“).

### 3.4 Chatterbox Multilingual v3
- Veröffentlicht 10.06.2026; 0,5B, Llama-Backbone, MIT, 23 Sprachen auf der HF-Karte (Seite sagt an anderer Stelle „21 Sprachen und 4 Dialekte“ / „25 gesamt“ – interne Inkonsistenz). Deutsch **CER unter 0,20 %** (Whisper-large-v3, 100 Samples, interne Prüfung); MOS und Sprecherähnlichkeit „in Arbeit“. RTF ca. 5 x Echtzeit auf H100 (unoptimiert). [Resemble-Blog](https://www.resemble.ai/resources/chatterbox-multilingual-v3-tts-with-embedded-watermarking-for-25-languages), [HF](https://huggingface.co/ResembleAI/chatterbox), 21.10.2026.
- Beispielcode (wörtlich aus HF): `ChatterboxMultilingualTTS.from_pretrained(device="cuda", t3_model="v3")`, `model.generate("Guten Tag, wie geht es Ihnen?", language_id="de")`.
- Hinweis der Karte: Referenzclip soll zur Sprache passen, sonst übernimmt die Ausgabe den Akzent des Clips; CFG-Gewicht 0 als Gegenmittel.
- TN: keine dokumentiert. Artificial Analysis Gesamt-Elo 1024 (Open Weights Platz 12) – **nicht** nach Sprache aufgeschlüsselt.

### 3.5 Pocket TTS
- GitHub-README: Sprachen „english, french, german, portuguese, italian, spanish, dutch“; Option `--language`; 24-Layer-Varianten („`italian_24l`“-Muster; ein deutscher 24L-Name ist **nicht belegt**). Voices: `juergen` (Deutsch). Linux/macOS/Windows (x86) Binär. Installation: `pip install pocket-tts` oder `uvx pocket-tts generate`. [GitHub](https://github.com/kyutai-labs/pocket-tts), 21.10.2026.
- **Gating:** `kyutai/pocket-tts` ist gated, Hauptdatei `tts_b6369a24.safetensors` 236 MB, Ordner `languages/`, `embeddings_v3/`. Ein Repo `kyutai/pocket-tts-without-voice-cloning` ist abrufbar und wirkte ungated; **ob die CLI darauf umgestellt werden kann, ist nicht belegt**. [HF-Dateiliste](https://huggingface.co/kyutai/pocket-tts/tree/main), [Alternativ-Repo](https://huggingface.co/kyutai/pocket-tts-without-voice-cloning), 21.10.2026. Der Hinweis „ohne Konto“ gilt deshalb **nicht verlässlich**.
- Community-GGUF: `EryriLabs/pocket-tts-GGUF`, `german/pocket-tts-german.gguf` 152 MB plus mmproj, CC-BY-4.0, Windows per `winget install llama.cpp`; ohne Sprecherdatei fast kein Audio. [HF](https://huggingface.co/EryriLabs/pocket-tts-GGUF).
- Qualität: nur das Gradium-Umfeld (vendor); keine unabhängige Zahl.
- Android: LiteRT-Community-Port ca. 1 x RT auf Pixel 8a GPU; ein „ToBe SAID“-Android-App existiert (Community, unbelegt). Für sherpa-onnx liegt ein Asset vom 26.01.2026 vor; **ob das deutsche Modell enthalten ist, steht nicht belegt fest**.
- TN: keine dokumentiert.

### 3.6 Gradium Phonon (neu, 21.10.2026)
- Blog: „released October 21, 2026, announced at NVIDIA GTC in Berlin“; 31M und 40M; Weights unter Apache-2.0 laut Blog, **widerlegt für 40M**: Repo-Seite sagt `phonon-40m-*` = CC BY-NC 4.0. [Blog](https://gradium.ai/blog/gradium-releases-phonon), [xn-ptts](https://github.com/gradium-ai/xn-ptts), 21.10.2026.
- `phonon-40m-de`: nur Deutsch, 24 kHz mono, Streaming, Beispielstimme `Henrik`, ca. 100 Stimmen in `gradium/phonon-voices`, 74,6 MB q8-GGUF. Aufruf: `uvx ptts --model hf://gradium/phonon-40m-de --lang de --quant q8 --voice Henrik "Hallo von Phonon." -o speech.wav` oder `pip install ptts`. **Gated: Login + Bedingungen + `HF_TOKEN`.** [HF](https://huggingface.co/gradium/phonon-40m-de).
- **Normalisierung:** Die Runtime normalisiert Zahlen, Währung, Datum, E-Mail abhängig von `lang`; der Transformers-Tokenizer tut das **nicht**.
- Geschwindigkeit (Hersteller, „x RT“ = höher schneller): MacBook Air M5 1 Kern 20 x (40M), 4 Kerne 39 x; iPhone 16 Pro Neural Engine 20 x; Raspberry Pi 5 1 Kern 2,2 x. Kokoro auf demselben Kern 7,3 x.
- Qualität (Hersteller): Deutsch Elo 2097 vs. 2046 (multilingual) vs. NeuTTS 1768 vs. Magpie 1640; Hard-Set-LLM-WER 2,2 % / 2,9 % / 9,3 % / 29 %. Seed-TTS-LLM-WER 0,83 %. **Die Gegner sind vom Hersteller gewählt, Bewertungsraster von Gradium (168 Rater).**
- Android: QNN-Bibliothek für Snapdragon-NPUs (Android 12+, ARM64, SoC-spezifisches Bundle, kein CPU-Fallback, nicht auf Maven Central) oder CPU-Bibliothek (Rust/NDK aus Quellen). Franks Fold (Snapdragon-SoC) – SoC-Zuordnung nicht geprüft [unbelegt].
- **Lizenz-Konsequenz:** NC. Für rein privaten Eigengebrauch von Frank nutzbar, nicht für Verkaufs-Apps.

### 3.7 Magpie TTS Multilingual (v2607)
- 12 Sprachen inkl. Deutsch, 364M, NVIDIA Open Model License; „Text normalization is required“ steht auf der Karte, eingebaute TN für Zahlen/Abkürzungen/Sonderzeichen in allen 12 Sprachen; IPA-Eingabe und G2P-Wörterbücher. Deutsch-CER 0,80 %, SSIM 0,742. Lokal: NeMo (Python 3.10.12+) oder `nemo-speech synthesize` (C++), GPUs nur NVIDIA Datacenter-Klassen genannt. [HF](https://huggingface.co/nvidia/magpie_tts_multilingual_357m), 21.10.2026.
- Community-C++: `mudler/magpie-tts.cpp-gguf`, q8_0 624 MB, CMake-Build, Windows nicht erklärt. [HF](https://huggingface.co/mudler/magpie-tts.cpp-gguf).
- Artificial Analysis „Magpie-Multilingual 357M (Feb 2026)“ Elo 1063 (Open-Weights-Liste, nicht deutsch-spezifisch). [AA](https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice) (die Seite hat **keinen Sprachfilter**).

### 3.8 NeuTTS Nano German, Kani-TTS, Orpheus-DE, Zonos
- **NeuTTS Nano German:** `pip install neutts` (+ `neutts[llama]` für GGUF, `neutts[onnx]` für ONNX-Decoder); Windows: `winget install -e --id eSpeak-NG.eSpeak-NG` oder `choco install espeak-ng`; Aufruf `python -m examples.basic_example --input_text "..." --ref_audio samples/greta.wav --ref_text samples/greta.txt --backbone neuphonic/neutts-nano-german`. Referenz 3–15 s, mono, sauber. [HF](https://huggingface.co/neuphonic/neutts-nano-german), [GitHub](https://github.com/neuphonic/neutts), 21.10.2026.
- **Kani-TTS-DE:** `pip install kani-tts`, `pip install -U "transformers==4.57.1"`, `KaniTTS('nineninesix/kani-tts-400m-de')`. Die Karte widerspricht sich bei der Lizenz (Tag `lfm1.0`, Text Apache-2.0) und nennt „Optimized primarily for English“. [HF](https://huggingface.co/nineninesix/kani-tts-400m-de), [GitHub](https://github.com/nineninesix-ai/kani-tts), 21.10.2026.
- **Orpheus 3B DE:** Karte `canopylabs/3b-de-ft-research_release`: Apache-2.0 (Tag), gated, Llama-basiert, 3B F32, zero-shot-Klonen, Emotions-Tags, Hersteller nennt ca. 200 ms Streaming-Latenz, verlinkt keine Benchmarks; Nutzungsverbote (Imitation ohne Zustimmung, Desinformation). [HF](https://huggingface.co/canopylabs/3b-de-ft-research_release), 21.10.2026.
- **Zonos:** [GitHub](https://github.com/Zyphra/Zonos), 21.10.2026.

### 3.9 Weitere Neuzugänge ab August 2026 (Liste theopenweights.com, 21.10.2026)
Magpie TTS (10.08.), IndexTTS-2.5 (10.08., kein Deutsch), AuK (Tencent, 18.08.), Audio8-TTS-Preview 0.1B (19.08.), Breeze-TTS-2 (25.08., nur Englisch), StepAudio 3 Gen/Realtime (10./11.09.), Qwen3-basiertes TTS/ASR von Nari Labs (14.09.), Drama 3 (Fish Audio, 35B MoE, 01.10.), IBM Granite Switch 4.1 (15.10.). Bei keinem steht auf der Listenseite Deutsch oder die Lizenz; eigene Prüfung hat das nicht nachgeholt [unbelegt]. Zusätzlich im Umfeld erwähnt und geprüft: **VoxCPM2** (OpenBMB, 2B, **Apache-2.0**, 30 Sprachen inkl. Deutsch, ca. 8 GB VRAM, RTF ca. 0,30 auf RTX 4090, `pip install voxcpm`, `normalize=True` aktiviert externe TN; Deutsch-WER 0,68 laut Suchergebnis zum Paper, **nicht** am Paper selbst bestätigt) – ein ernsthafter Kandidat, der in der Vorarbeit fehlt. [HF](https://huggingface.co/openbmb/VoxCPM2), [Paper](https://arxiv.org/abs/2606.06928), 21.10.2026. Ebenfalls neu: **dots.tts** (2B, Apache-2.0, Juni/August 2026), Paper nennt nur zh/en-Tests; Deutsch **nicht belegt**. [arXiv](https://arxiv.org/abs/2606.07080).


### 3.10 Primärquellen für Tabellenzeilen ohne eigenen Unterabschnitt (alle abgerufen 21.10.2026)
- VibeVoice-Realtime-0.5B und VibeVoice-TTS-1.5B/7B: [microsoft/VibeVoice](https://github.com/microsoft/VibeVoice) (MIT; TTS-Code am 05.09.2025 entfernt; Realtime-0.5B mit experimentellen Stimmen in neun Sprachen inkl. DE; „intended for research and development purposes only“).
- OuteTTS 1.0 (0,6B): Lizenz und Deutsch aus der Vorarbeit ([HF OuteAI/OuteTTS-0.3-1B](https://huggingface.co/OuteAI/OuteTTS-0.3-1B), Stand 28.09.); RTF 70 aus [wulffit.de](https://wulffit.de/artikel/tts-deutsch/).
- F5-TTS German: [aihpi/F5-TTS-German](https://huggingface.co/aihpi/F5-TTS-German) (cc-by-nc-4.0, Common Voice + Emilia_DE), [hvoss-techfak/F5-TTS-German](https://huggingface.co/hvoss-techfak/F5-TTS-German) (CC BY-NC 4.0, 4,2 Mio. Schritte), Basis [SWivid/F5-TTS](https://huggingface.co/SWivid/F5-TTS) (cc-by-nc-4.0).
- XTTS-v2: [coqui/XTTS-v2](https://huggingface.co/coqui/XTTS-v2) (CPML, 17 Sprachen), [idiap/coqui-ai-TTS](https://github.com/idiap/coqui-ai-TTS) (MPL-2.0, `pip install coqui-tts`), Tokenizer-Code siehe Bug-Kandidaten.
- Kokoro: [hexgrad/Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (Apache-2.0, v1.0 vom 27.01.2025, 8 Sprachen).

---

## 4. Deutsche Textnormalisierung – Zusammenfassung gegen den Testtext A

| Phänomen (Testtext) | Piper | Supertonic 3 | Qwen3-TTS | Chatterbox v3 | Pocket TTS | Phonon-DE | Magpie | XTTS-v2 (Tokenizer) |
|---|---|---|---|---|---|---|---|---|
| Datum 21.10.2026 | nein | nicht genannt (Drittprojekt: ja) | nicht dokumentiert | nicht dokumentiert | nicht dokumentiert | **ja** (Runtime) | **ja** (Hersteller) | **nein** |
| Uhrzeit 18:47 | nein | Drittprojekt: ja | – | – | – | **ja**? (nur „Datum/Zahlen“ genannt, Uhrzeit nicht explizit) | ja (allgemein) | **nein** |
| 30,5 / 2,3 % / 12,5 km / 14 °C | nein | Hersteller: Dezimal + Einheit; Drittprojekt: Prozent, °C | – | – | – | ja | ja | ja (Dezimal, `%`, `°`) |
| 1.249,99 € | nein | Hersteller: Währung; Drittprojekt: Euro/Cent | – | – | – | ja (Währung) | ja | ja (Euro/Cent) |
| 3. Nov., ca., z. B., Dr. | nein | „Abkürzungen“ (nicht Deutsch-spezifisch) | – | – | – | unbelegt | ja (allgemein) | nur Dr. (fünf Abkürzungen) |
| HRV/WLAN/USB | nein | unbelegt | – | – | – | unbelegt | unbelegt | nein |

„–“ bedeutet: keine Aussage in der gelesenen Quelle [unbelegt]. Praktisch gilt: **Alle Modelle außer Phonon-DE und Magpie brauchen für Franks Alltagssätze eine eigene Vorverarbeitung**; die Prüfung am Testtext ist für jedes Modell im Hörtest nachzuholen.

---

## 5. Android-Lauffähigkeit (kompakt)

| Modell | Belegter Weg | Beleg |
|---|---|---|
| Piper-DE | sherpa-onnx (Assets belegt); espeak-ng-Pflicht entfällt mit PR #4055, Lexikon-/Tokens-Weg noch nicht für Deutsch erprobt | API-Abfrage, PR #4055 |
| Supertonic 3 | sherpa-onnx-Asset int8; Flutter-Beispiel; Drittprojekte | API-Abfrage, [GitHub](https://github.com/supertone-inc/supertonic) |
| Pocket TTS | LiteRT-Community (1 x RT Pixel 8a GPU); sherpa-onnx-Asset nur Januar-Stand | [GitHub README](https://github.com/kyutai-labs/pocket-tts) |
| Phonon-DE | offizielle Android-Pfade (CPU Rust/NDK, QNN) – NC | [xn-ptts](https://github.com/gradium-ai/xn-ptts) |
| NeuTTS Nano | GGUF, Galaxy-A25-CPU-Messung in tok/s | [GitHub](https://github.com/neuphonic/neutts) |
| Qwen3-TTS, Chatterbox, Magpie, Zonos, Orpheus, Kani | nur PC-Server (GPU) bzw. Community-CPU-Ports | s. o. |

---

## 6. Qualitätsbelege – ehrliche Lage

- **Unabhängig, deutsch:** nur **wulffit.de, April 2026** (8 Modelle, CPU/Mac, kleine Stichprobe). Rangfolge: 1. Qwen3-TTS 0.6B (sehr gut), 2. VibeVoice 0.5B (gut), 3. XTTS-v2 (mittel), 4. F5-TTS (schlecht*), 5. OuteTTS, 6.–8. Thorsten VITS, Tacotron2-DDC, Piper (alle „outdated“). Chatterbox, Supertonic, Pocket, Phonon, Magpie und NeuTTS waren **nicht** Teil dieses Tests.
- **Artificial Analysis Open-Weights (21.10.2026, nicht sprachspezifisch):** Fish Audio Drama 3 Preview 1248, Breeze TTS 2 1225, Ming-3.0-tts 1190, Fish S2 Pro 1112, Step Audio EditX 1099, Voxtral TTS 1091, Kokoro 1065, Magpie 1063, Maya1 1047, Higgs Audio V3 1044, OpenAudio S1 Mini 1044, Chatterbox 1024, Zonos 1000, VibeVoice 1.5B 956, OpenVoice v2 956, XTTS v2 916. Qwen3-TTS, Supertonic, Pocket TTS und Phonon stehen **nicht** in der abgerufenen Liste. [AA](https://artificialanalysis.ai/text-to-speech/leaderboard/provider-voice). **Kein Deutsch-Filter** vorhanden; die „Controlled Voice Arena“ für Deutsch war über diese Seite nicht abrufbar [unbelegt].
- **Hersteller-Deutschzahlen:** Magpie CER 0,80 %; Chatterbox CER unter 0,20 %; Qwen3-TTS WER 1,089/1,235 (Qualität der Tabellen-Abfrage unsicher); Phonon Elo 2097. Sie sind **nicht vergleichbar** (verschiedene Testsätze, ASR-Modelle, Normalisierung).
- **MOS-Studien für Deutsch bei diesen Modellen:** keine gefunden.

---

## 7. Empfehlung für den schnellen lokalen Hörtest auf dem Windows-PC (NVIDIA-GPU)

Auswahlkriterien: pip-Paket, wenig Dateien, ohne Konto, Deutsch nativ. Gewählt werden vier Modelle, damit eine Baseline, ein kleines CPU-Modell und zwei GPU-Modelle verglichen werden.

| Rang | Modell | Warum | Konto nötig? |
|---|---|---|---|
| 1 | **Piper `de_DE-thorsten-high`** | Baseline; schnell; eine 114-MB-Datei; belegt das „Ist“ der Hörqualität | nein |
| 2 | **Supertonic 3** (`lang="de"`, Stimme `M1`/`F1`) | kleinstes aktuelles 31-Sprachen-Modell, läuft auch auf CPU/Android (sherpa-onnx-Asset) | nein (`auto_download=True`) |
| 3 | **Qwen3-TTS 0.6B** | einziges Modell mit unabhängigem Deutsch-Spitzenurteil (wulffit) | nein |
| 4 | **Chatterbox Multilingual v3** | MIT, Deutsch CER < 0,20 % [Hersteller], eine Zeile Code | nein |
| (5) | Pocket TTS `german` | Streaming + CPU, aber Hauptrepo gated | ja, HF-Login (Frank hat laut MCP-Hinweis ein HF-Konto) |

**Befehle** (PowerShell, jeweils im eigenen venv, z. B. `python -m venv .venv-piper; .\.venv-piper\Scripts\Activate.ps1`). **Nur der volle Testtext A zählt als gültige Hörprobe.** Er liegt UTF-8-kodiert in `C:\Users\barwa\proggs\Recherchen\tts-vergleich\testtext_a.txt`; alle Aufrufe lesen diese Datei (vermeidet auch Escaping von `€`, `–`, Umlauten in `python -c`):

```powershell
$env:PYTHONUTF8 = "1"
$tt = "C:\Users\barwa\proggs\Recherchen\tts-vergleich\testtext_a.txt"
```

**1. Piper**
```powershell
pip install piper-tts
# Stimme laden (zwei Dateien pro Stimme, laut docs/VOICES.md)
curl.exe -L -o de_DE-thorsten-high.onnx      https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/high/de_DE-thorsten-high.onnx
curl.exe -L -o de_DE-thorsten-high.onnx.json https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/high/de_DE-thorsten-high.onnx.json
$t = Get-Content -Raw -Encoding utf8 $tt
python -m piper -m de_DE-thorsten-high -f thorsten.wav -- $t
```
Belegt: `pip install piper-tts`; Aufrufform `python3 -m piper -m <Stimme> -f test.wav -- 'Text'`; „Voices download to the current directory by default“ und `--data-dir` ([CLI.md](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/CLI.md)); beide Dateien pro Stimme ([VOICES.md](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/VOICES.md)), abgerufen 21.10.2026. Der `resolve/main`-URL-Aufbau folgt dem HF-Standardmuster und wurde nicht einzeln abgerufen. `python -m piper.download_voices` ist hier **unbelegt**. Erwartung: Zahlen/Datum/Uhrzeit werden schlecht gelesen (keine TN) – das gehört zum Ergebnis.

**2. Supertonic 3**
```powershell
pip install supertonic
python -c "import sys; from supertonic import TTS; t=TTS(auto_download=True); s=t.get_voice_style(voice_name='M1'); w,d=t.synthesize(open(sys.argv[1],encoding='utf-8').read(), voice_style=s, lang='de'); t.save_audio(w,'supertonic.wav')" $tt
```
Belegt auf der [HF-Karte](https://huggingface.co/Supertone/supertonic-3), 21.10.2026 (`TTS(auto_download=True)`, `get_voice_style(voice_name="M1")`, `synthesize(text, voice_style=style, lang="en")`, `save_audio`). `lang="de"` folgt der Sprachliste. Modell ca. 380 MiB beim ersten Start (Drittprojektangabe). Das Paket ist archiviert, bleibt aber installierbar.

**3. Qwen3-TTS 0.6B** (Python 3.12 empfohlen)
```powershell
pip install -U qwen-tts soundfile
python -c "import sys, torch, soundfile as sf; from qwen_tts import Qwen3TTSModel; m=Qwen3TTSModel.from_pretrained('Qwen/Qwen3-TTS-12Hz-0.6B-CustomVoice', device_map='cuda:0', dtype=torch.bfloat16); w,sr=m.generate_custom_voice(text=open(sys.argv[1],encoding='utf-8').read(), language='German', speaker='Aiden'); sf.write('qwen3.wav', w[0], sr)" $tt
```
Die Aufrufform (`Qwen3TTSModel.from_pretrained(..., device_map="cuda:0", dtype=torch.bfloat16, attn_implementation="flash_attention_2")`, `generate_custom_voice(text=, language=, speaker=, instruct=)`, `sf.write(..., wavs[0], sr)`) stammt wörtlich aus dem [README](https://github.com/QwenLM/Qwen3-TTS), 21.10.2026, dort mit dem Modell `...-1.7B-CustomVoice`, `language="Chinese"`, `speaker="Vivian"`. Hier angepasst auf 0.6B, `language="German"`, `speaker="Aiden"` (wulffit: Aiden lieferte das beste Deutsch). `attn_implementation="flash_attention_2"` ist weggelassen (flash-attn unter Windows lästig; der Verzicht ist **nicht** vom Hersteller als lauffähig belegt). Der Parameterwert `"German"` ist aus der Sprachliste abgeleitet, das README erwähnt auch `Auto`. Erwartung: GPU nötig (bf16/CUDA).

**4. Chatterbox Multilingual v3**
```powershell
pip install chatterbox-tts
python -c "import sys, torchaudio as ta; from chatterbox.mtl_tts import ChatterboxMultilingualTTS as C; m=C.from_pretrained(device='cuda', t3_model='v3'); w=m.generate(open(sys.argv[1],encoding='utf-8').read(), language_id='de'); ta.save('chatterbox.wav', w, m.sr)" $tt
```
Aufruf-Grundform wörtlich aus der [HF-Karte](https://huggingface.co/ResembleAI/chatterbox), 21.10.2026 (`ChatterboxMultilingualTTS.from_pretrained(device="cuda", t3_model="v3")`, `model.generate(..., language_id="de")`, `ta.save(..., wav, model.sr)`). Ob der volle Testtext in einem Durchgang stabil bleibt, ist unbelegt (ggf. satzweise splitten). PerTh-Wasserzeichen ist eingebaut.

**(5) Pocket TTS (nur mit HF-Login)**
```powershell
pip install pocket-tts
pocket-tts generate --language german --voice juergen --text (Get-Content -Raw -Encoding utf8 $tt)
```
Belegt: Optionen `--language`, `--voice`, `--text`; Stimme `juergen` ([README](https://github.com/kyutai-labs/pocket-tts), 21.10.2026). Der Sprachname `german` folgt dem README-Muster, ist aber **nicht** wörtlich genannt. Erster Lauf lädt vom gated Repo; ohne `hf auth login`/Token scheitert er voraussichtlich [unbelegt].

**Bewusst ausgelassen für den ersten Hörtest:** Phonon-DE (gated + NC, aber **zum Zuhören interessant**, weil eingebaute TN und der neueste Stand), NeuTTS (gated + espeak-ng + Umsatzgrenze), Magpie (NeMo unter Linux bzw. CMake-Build), XTTS-v2 (CPML nicht kommerziell, Fork-Installation aufwendiger), Orpheus (gated, 12 GB), F5-German (NC, Referenzclip), VoxCPM2 (nicht am Paper geprüft; optional als 5. Kandidat: `pip install voxcpm`, CUDA ≥ 12, ca. 8 GB VRAM).

---

## 8. Offene Punkte (unbelegt)

- Deutsche Pocket-TTS-Stimme im sherpa-onnx-Asset und CLI-Umstellung auf das ungated Repo.
- Funktionieren die deutschen Piper-Stimmen nach sherpa-onnx PR #4055 ohne Zusatzarbeit (Lexikon)?
- Qwen3-TTS: genaue deutsche Sprachparameter, Gewichtslizenz der 1.7B-Varianten, Android.
- Lizenztext XTTS CPML (Coqui-Seite 404, Archiv nicht abrufbar); Wortlaut „nicht kommerziell“ nur aus Sekundärquelle (pinggy.io, Update 26.08.2026).
- Deutsch bei VoxCPM2/dots.tts und den Neuzugängen ab 08/2026.
- Artificial-Analysis-Ergebnisse der Deutsch-„Controlled Voice Arena“.
- Kani-TTS-DE Gewichtslizenz.

---

BEST-PRACTICES-KANDIDATEN:
- Phonon-Runtime (`gradium-ai/xn-ptts`): sprachabhängige Zahlen-/Währungs-/Datumsnormalisierung in der Runtime statt im Modell als Referenz für deutsche TN (`lang="de"` muss zum Text passen). Quelle: https://github.com/gradium-ai/xn-ptts, 21.10.2026.
- `marco-taylor/supertonic3-tts-german-wyoming` (MIT, Rust): fertige deutsche TN (Datum, Uhrzeit, Euro/Cent, °C, Einheiten) als Vorbild für Jarvis. Quelle: https://ca.unraid.net/apps/supertonic3-tts-german-wyoming-05cuqsm0okej76 und https://github.com/marco-taylor/supertonic3-tts-german-wyoming, 21.10.2026.
- sherpa-onnx 2.0: Migrationsweg von espeak-ng/piper-phonemize auf `lexicon.txt` oder externe Phonemisierung (`tokens`/`phoneme_codepoints`). Quelle: https://github.com/k2-fsa/sherpa-onnx/pull/4055, 21.10.2026.
- Lizenzprüfung bei TTS-Gewichten immer getrennt vom Code und immer die Nutzungsverbote lesen (OpenRAIL-M „keine medizinischen Ergebnisse“, NeuTTS 5-Mio.-Umsatzgrenze, LFM-Umsatzgrenze, CC BY-NC bei Phonon-40M/Voxtral/F5-German). Quelle: https://huggingface.co/Supertone/supertonic-3/blob/main/LICENSE und die Modellkarten oben.
- RTF-Konvention immer mitschreiben (Rechen÷Audio vs. Audio÷Rechen); Quellen mischen beide.

BUG-KANDIDATEN:
- Kani-TTS `kani-tts-400m-de`: HF-Tag `lfm1.0` widerspricht dem Fließtext „Apache 2.0“ (LFM2-Backbone mit Umsatzgrenze). https://huggingface.co/nineninesix/kani-tts-400m-de, 21.10.2026.
- Coqui-XTTS-Tokenizer Deutsch: `@`→„at“ (englisch), Ordinalzahlen ohne Genus („1. Test“→„erste Test“), keine Datums-/Uhrzeit-Normalisierung, Euro-Behandlung läuft in `contextlib.suppress(Exception)` und schluckt Fehler still. https://raw.githubusercontent.com/idiap/coqui-ai-TTS/main/TTS/tts/layers/xtts/tokenizer.py, 21.10.2026.
- Kokoro-Deutsch (`Thorsten-Voice/Kokoro`): misaki-G2P erzeugt `ʏ` (kurzes ü), das nicht im Kokoro-Vokabular steht; „Brücke“ bricht still; Workaround `ʏ`→`y`. https://huggingface.co/Thorsten-Voice/Kokoro, 21.10.2026.
- Pocket TTS: Hauptrepo gated, Dokumentation verspricht „läuft ohne Konto“ nicht; `tts_output.wav` und Stimmen sonst nicht ladbar ohne HF-Login. https://huggingface.co/kyutai/pocket-tts, 21.10.2026.
- Qwen3-TTS in mlx-audio: `lang_code="de"` fällt still auf Auto zurück, Deutsch mit englischem Akzent; nur `"german"` funktioniert; `speed=1.25` wird ignoriert. https://wulffit.de/artikel/tts-deutsch/, 21.10.2026.
- Gradium-Blog widerspricht der Repo-Seite: Blog „Weights unter Apache 2.0“, Repo: `phonon-40m-*` = CC BY-NC 4.0 und gated. https://gradium.ai/blog/gradium-releases-phonon vs. https://github.com/gradium-ai/xn-ptts, 21.10.2026.
- Chatterbox-Seite: widersprüchliche Sprachanzahl („21 Sprachen und 4 Dialekte“ im Titel, „25“ im Text, „23“ auf HF). https://www.resemble.ai/resources/chatterbox-multilingual-v3-tts-with-embedded-watermarking-for-25-languages, 21.10.2026.
- Piper: Home-Assistant-Nutzer berichten, dass nach Listenpunkten („1.“) der nächste Eintrag mit dem vorherigen verschmilzt – Satzzeichenfolge wird von der fehlenden TN nicht korrigiert. https://community.home-assistant.io/t/piper-tts-pre-processing/826330, 21.10.2026.
