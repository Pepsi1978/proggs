# R7 — Lokale und offene STT-Modelle für Deutsch gegen Groq whisper-large-v3-turbo

Stand: 21.10.2026. Recherche R7 der STT-Vergleichsstudie. Bestehendes Wissen (nicht wiederholt): `best-practices/desktop/whisper-stt-lokal.md` (Whisper lokal, primeline-turbo-german, VAD, Streaming-Tricks) und `bugs/desktop/whisper-stt-lokal.md` (E/H/S/M/C/A/N-Einträge). Dieser Bericht ergänzt die Nicht-Whisper-Modelle und liefert Vergleichszahlen gegen die Basislinie.

Kennzeichnung: **belegt** = Zahl steht in der genannten Primärquelle; **Eigenangabe** = Modellkarte des Autors, nicht unabhängig geprüft; **unbelegt** = nichts Belastbares gefunden.

## 0. Wie die Basislinie (Turbo) hier vermessen ist

Whisper-large-v3-turbo fehlt in fast allen Fremd-Benchmarks (Open ASR Leaderboard Mehrsprachig-Tabelle, NVIDIA-Paper FLEURS/CoVoST). Die einzige direkt vergleichbare Messung auf demselben Prüfstand mit Deutsch ist die des Autors der primeline-Modelle (Testsatz flozi00/asr-german-mixed-evals, Eigenangabe, zwei Modellkarten desselben Autors, die in der zweiten Nachkommastelle leicht abweichen):

| Modell (WER %, Deutsch) | Tuda-De | Common Voice 19.0 | MLS | Gesamt |
|---|---|---|---|---|
| openai whisper-large-v3-turbo (BASISLINIE) | 8,30 | 3,85 | 3,20 | 3,65 |
| openai whisper-large-v3 | 7,88 | 3,48 | 2,83 | 3,28 |
| nvidia parakeet-tdt-0.6b-v3 (Basis) | 7,05 | 3,70 | 2,95 | 3,64 |
| primeline/parakeet-primeline (Finetune) | 4,11 | 3,03 | 2,60 | 2,95 |
| primeline/whisper-large-v3-turbo-german (Finetune) | 6,44 | 3,20 | 2,07 | 2,63 |

Quellen: https://huggingface.co/primeline/whisper-large-v3-turbo-german (Stand der Karte abgerufen 21.10.2026) und https://huggingface.co/primeline/parakeet-primeline (CC-BY-4.0). Auffällig: Basis-Parakeet ist in jeder Einzelspalte besser als Turbo, im Gesamtwert aber gleich (3,64 gegen 3,65). Die Gewichtung des „Gesamt"-Werts ist nicht erklärt; ich lese ihn als „praktisch gleichauf".

Abgeleitet: Turbo ist auf Deutsch rund 0,3 bis 0,4 Punkte schlechter als large-v3. Wo unten nur large-v3 gemessen ist, dient large-v3 als Näherung für Turbo und Turbo ist entsprechend etwas schlechter.

Relative Verbesserung gegen Turbo (Gesamt, Eigenangabe): primeline-parakeet −19 %, primeline-turbo-german −28 %.

## 1. Mehrsprachige Fremdmessung (unabhängiger als die Autoren-Karten)

Open ASR Leaderboard, Mehrsprachig-Spur, Tabelle 4, Deutsch-WER (https://arxiv.org/html/2510.06961v4, abgerufen 21.10.2026):

| Modell | DE-WER % | RTFx (Mittel über Sprachen) | offen? |
|---|---|---|---|
| Mistral Voxtral Small 24B | 3,01 | 42,0 | ja |
| Cohere Labs Transcribe (2B) | 3,84 | 491 | ja |
| Microsoft Phi-4-multimodal | 3,96 | 78,2 | ja |
| NVIDIA Canary-1B-v2 | 4,10 | 634 | ja |
| Qwen3-ASR 1.7B | 4,12 | 113 | ja |
| NVIDIA Parakeet-TDT-0.6B-v3 | 4,20 | 1720 | ja |
| OpenAI Whisper large-v3 | 4,26 | 111 | ja |

Fußnote: Fassung v1 desselben Papers (https://arxiv.org/html/2510.06961v1) nannte andere Zahlen (Phi-4 4,50, Parakeet 4,90, Canary 4,96, Whisper large-v3 4,97, Voxtral Mini 3B 5,36). Die Tabelle wurde in späteren Fassungen neu gerechnet; hier gilt v4. Whisper-large-v3-turbo, Granite, Kyutai und Moonshine stehen in keiner Fassung in der Deutsch-Tabelle.

FLEURS-de und CoVoST2-de aus dem NVIDIA-Paper (https://arxiv.org/html/2509.14128v2, Anhang B, Tabellen 11 und 13):

| Modell | FLEURS-de | CoVoST2-de |
|---|---|---|
| Whisper large-v3 | 4,30 | 5,94 |
| Phi-4-multimodal | 4,37 | 4,62 |
| Canary-1B-v2 | 4,40 | 5,53 |
| Voxtral-Mini-3B-2507 | 4,62 | 6,09 |
| Parakeet-TDT-0.6B-v3 | 5,04 | 4,84 |
| Seamless-M4T-v2-large | 6,25 | 5,34 |

Abweichung: FluidAudio misst Parakeet v3 (CoreML) auf FLEURS-de mit 5,9 % WER (350 Dateien, eigene Normalisierung, https://docs.fluidinference.com/reference/benchmarks). Gegen NVIDIAs 5,04 % ist das ein Normalisierungs-/Portierungsunterschied und kein Beleg für schlechtere Gewichte. Vergleiche nur innerhalb einer Messreihe.

Lesart: Auf den Fremdbenchmarks liegen Parakeet v3, Canary, Qwen3 und Whisper large-v3 innerhalb von etwa 0,2 Punkten, das ist statistisch gleichauf. Deutlich vorn liegen nur Cohere Transcribe (3,84), Phi-4 und die 24B-Voxtral-Variante.

## 2. Modelle im Einzelnen

### 2.1 NVIDIA Parakeet-TDT-0.6B-v3

- Modell-ID: `nvidia/parakeet-tdt-0.6b-v3`. Veröffentlicht August/September 2025 (Paper arXiv 2509.14128).
- Lizenz: CC-BY-4.0, „ready for commercial use" (https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3). Nur Namensnennung nötig.
- Größe: 600 M Parameter. sherpa-onnx int8: encoder 622 MB + decoder 12 MB + joiner 6 MB, zusammen etwa 640 MB (https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/nemo-transducer-models.html).
- Deutsch-Qualität: FLEURS-de 5,04, CoVoST2-de 4,84 (NVIDIA-Paper); Leaderboard-DE 4,20 (v4); primeline-Prüfstand Gesamt 3,64 gleich Turbo 3,65. Fazit: Genauigkeit etwa gleich Turbo, nicht besser.
- Sprachen: 25 europäische, automatische Spracherkennung, keine Sprachvorgabe nötig.
- Hardware/Echtzeitfaktor:
  - Referenz (NVIDIA-GPU): RTFx 3332,74 (Open ASR Leaderboard, englische Kurzform, https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3). Mehrsprachig RTFx 1720 (Tabelle 4 oben). Whisper large-v3 liegt bei 111 bis 145, Turbo bei etwa 350 laut Zusammenfassung (Sekundärquelle, unbelegt im Primärpapier).
  - Windows-Laptop (Core Ultra 7 258V, Arc 140V, 31 GB RAM, ermittelt am 21.10.2026): **keine direkte Messung gefunden, unbelegt.** Nächster Anhaltspunkt: onnx-asr RTFx 36 auf einem Desktop-Ryzen 9800X3D (CPU), 1,0 auf einem Cortex-A53 (https://github.com/istupakov/onnx-asr). sherpa-onnx meldet RTF 0,325 (2 Threads) für eine 3,8-s-Datei und RTF 0,028 mit VAD für eine 334-s-Datei, Hardware nicht genannt. NPU-Weg wäre OpenVINO-Provider; DirectML-NPU-Unterstützung ist laut Intel-Notizen ab Q3'25 veraltet (https://downloadmirror.intel.com/861398/NPU_Win_Release_Notes_v32.0.100.4181.pdf). Belegt ist keine Parakeet-Zahl auf Intel-NPU. Selbstmessen.
  - Android (Fold 8, Snapdragon): sherpa-onnx liefert das int8-Modell und ein Android-APK (laut Doku „Android APK, search parakeet_tdt_0.6b_v3"). Echtgeräte-RTF: **unbelegt.** Einzige Zahl: Soniqo, Android-**Emulator** arm64, CPU, ohne NNAPI, RTF 0,12 (1,5 s Audio in 175 ms); die Seite erwartet auf echtem Snapdragon mit NNAPI 2 bis 3 mal schneller, das ist eine Schätzung des Anbieters (https://soniqo.audio/benchmarks/android). Akkuverbrauch: **unbelegt.** Speicher: Modell ~640 MB (sherpa) bzw. ~1,2 GB laut Soniqo-Paket, RAM-Bedarf nicht gemessen.
  - Mac (Apple Silicon): FluidAudio-CoreML-Portierung läuft komplett auf der Neural Engine: RTFx 221 auf M4 Pro (48 GB), Deutsch FLEURS 5,9 % (https://docs.fluidinference.com/reference/benchmarks); die HF-Karte nennt ca. 110x auf M4 Pro (Batch). Kleinerer Test: parakeet-mlx 0,50 s gegen mlx-whisper turbo 1,02 s auf M4 MacBook Pro für einen kurzen Clip, aber mit Parakeet **v2** (https://github.com/anvanvan/mac-whisper-speedtest). Ein Blog behauptet das Gegenteil für MLX gegen CoreML-Whisper (0,50 gegen 0,19 s, ohne Methodik, https://www.arunbaby.com/speech-tech/0073-whisper-vs-parakeet-asr-decision/). Welcher Chip in Franks Mac steckt, ist mir unbekannt; alle Mac-Zahlen sind M4 Pro oder M4.
- Streaming: **nicht nativ.** Offline-Modell mit gepuffertem Chunk-Skript in NeMo (chunk_secs=2, right_context=2,0, left_context=10) laut Modellkarte. sherpa-onnx bietet „simulated streaming" mit VAD (Dokumentseite oben). Echte Streaming-Variante siehe 2.8 (Nemotron).
- Vokabular/Prompting: kein Prompt wie bei Whisper. NeMo Word Boosting (GPU-Phrase-Boosting-Baum) unterstützt jetzt TDT/RNN-T mit Greedy-Decoding, pro Äußerung über `biasing_request`; `boosting_tree_alpha` muss auf eigenen Daten abgestimmt werden (https://docs.nvidia.com/nemo/speech/latest/asr/asr_customization/word_boosting.html, https://arxiv.org/pdf/2508.07014). Im sherpa-onnx/onnx-asr-Weg ist Boosting nicht belegt. FluidAudio wirbt mit eigener Custom-Vocabulary (Earnings22 Vokabular-F 91,7 %), nur auf CoreML.
- Halluzination: Transducer-Decoder erzeugen kein autoregressives Freitext-Schleifen wie Whisper; ein unabhängiger Beleg nur für Parakeet-TDT-**1.1B** (nicht 0.6B v3): Das Paper nennt es plausibel „weniger anfällig" wegen zeichenbasierter Transducer-Decodierung, misst aber keine Halluzinationsrate (https://arxiv.org/html/2508.17753v1; Cleft-WER Parakeet 169,7 gegen Whisper large-v3 297,1 und Canary 667,7). Whisper large-v3 dagegen: 40,3 % der 301.317 Nicht-Sprach-Clips erzeugten Text, Top-Ausgaben „thank you" und „thanks for watching"; VAD (Silero) + Delooping/BoH drückt die WER von 104,8 auf 6,5 (https://arxiv.org/html/2501.11378v1; nur large-v3 getestet, nicht Turbo). Eine direkte Parakeet-v3-Zahl auf Stille/Rauschen fand ich nicht: **unbelegt.** Typisches Transducer-Risiko ist nicht Erfinden, sondern Auslassen von Wörtern; ein unabhängiger Beleg dafür für Deutsch fehlt: **unbelegt.**
- Integrationsweg: Android sherpa-onnx (Kotlin/JNI-AAR); Windows C# über onnx-asr (Python, MIT) oder sherpa-onnx (C#-Bindings) oder Whisper.net-ähnlich über transcribe.cpp (GGUF, Vulkan/CUDA); Mac FluidAudio (Swift, CoreML) oder parakeet-mlx. Max. Audiolänge je Aufruf bei onnx-asr 20 bis 30 s ohne VAD.

### 2.2 primeline/parakeet-primeline (deutsches Parakeet-Finetune)

- Modell-ID: `primeline/parakeet-primeline`, Basis Parakeet-TDT-0.6B-v3, 600 M Parameter. Lizenz CC-BY-4.0 (Namensnennung). Forschungsmodell von Florian Zimmermeister, „kein kommerzielles Produkt von primeLine, Nutzung auf eigenes Risiko".
- Deutsch-Qualität: Gesamt 2,95 (Tuda-De 4,11, MLS 2,60, CV19 3,03), Eigenangabe, gleicher Prüfstand wie die Turbo-Zahl 3,65. Trainingsdaten auf der Karte nicht beschrieben. Seltene Fachbegriffe können laut Karte falsch erkannt werden.
- Ausführung: ONNX-Export int8 (`encoder-model-int8.onnx`, `decoder_joint-model-int8.onnx`) verfügbar als `Buttermilk03/parakeet-primeline-onnx` für onnx-asr (https://huggingface.co/Buttermilk03/parakeet-primeline-onnx, CPU-Laden geprüft laut Karte). **Kompatibilität mit sherpa-onnx (Android) nicht angegeben: unbelegt.** Dateigrößen auf der Karte nicht genannt (vermutlich wie das Basismodell, nicht belegt).
- Rest (Streaming, Halluzination, Prompting, Plattform-RTF): wie Basis-Parakeet.

### 2.3 NVIDIA Canary-1B-v2

- Modell-ID: `nvidia/canary-1b-v2`; Lizenz CC-BY-4.0; 978 M Parameter (Karte: „1.0B"); 25 europäische Sprachen ASR plus Übersetzung (EN↔24) (https://huggingface.co/nvidia/canary-1b-v2).
- Deutsch: FLEURS-de 4,40, CoVoST2-de 5,53, Leaderboard-DE 4,10 (v4). Gleichauf mit Whisper large-v3.
- Speed: RTFx 749 (englische Leaderboard-Spur), mehrsprachig 634; onnx-asr nur 8 RTFx auf Ryzen-9800X3D-CPU (schwer auf CPU). Mindestens 6 GB RAM, nur NVIDIA Ampere/Hopper/Blackwell auf Linux gelistet. Keine Android-/Intel-NPU-Zahlen: unbelegt.
- Streaming: laut Karte nicht; Offline mit dynamischem Chunking (1 s Überlappung).
- Halluzination: MUSAN-48-Stunden-Satz: 134,7 Zeichen pro Minute (Karte, niedriger ist besser); kein Whisper-Gegenwert auf derselben Skala gefunden. Im 2508.17753-Test hatte Canary-1B (v1) die höchsten WER-Ausreißer bei Sonderfällen (667,7 auf Cleft).
- Fazit: genauigkeitsmäßig kein Gewinn gegen Parakeet v3, schwerer und ohne gute CPU-/Mobil-Laufzeit. Nicht empfohlen.
- Korrekturhinweis zum Bug-Almanach: Eintrag M9 sagt „Canary deckt nur EN/ES/DE/FR ab". Das trifft auf Canary-1B v1 zu; **Canary-1B-v2 deckt 25 Sprachen ab.**

### 2.4 NVIDIA Canary-Qwen-2.5B

- `nvidia/canary-qwen-2.5b`, CC-BY-4.0, **nur Englisch** („trained using English data only"), Mittel-WER 5,63 auf Open ASR (englisch), RTFx 418 (https://huggingface.co/nvidia/canary-qwen-2.5b). Für Deutsch ungeeignet. Sackgasse.

### 2.5 Cohere Transcribe (cohere-transcribe-03-2026)

- Nicht auf der Themenliste, aber das beste offene Modell mit Deutsch, das ich fand. Modell-ID: `CohereLabs/cohere-transcribe-03-2026`; Lizenz **Apache 2.0**; 2 B Parameter (BF16); 14 Sprachen inkl. Deutsch (https://huggingface.co/CohereLabs/cohere-transcribe-03-2026).
- Deutsch-Qualität: Leaderboard-DE **3,84** (Tabelle 4, v4), gegen Whisper large-v3 4,26 und Parakeet 4,20; RTFx 491 mehrsprachig bzw. 524,9 in der englischen Metadaten-Zeile. Die Modellkarte nennt keine Deutsch-Einzelzahl (nur Diagramm). Ein TechCrunch-Bericht meldete Schwäche bei Portugiesisch, Deutsch und Spanisch gegen Wettbewerber (https://techcrunch.com/2026/03/26/cohere-launches-an-open-source-voice-model-specifically-for-transcription/); die Gegenzahlen dazu fand ich nicht. Widerspruch zu Tabelle 4: **unklar**, bei Bedarf selbst messen.
- Laufzeit: Transformers ≥ 5.4.0, vLLM 0.19, MLX (`mlx-audio`), GGUF über transcribe.cpp (Metal/Vulkan/CUDA), Rust, transformers.js/WebGPU. **Kein ONNX/sherpa-onnx belegt, kein Android-Weg belegt.** Mindest-Hardware nicht angegeben. 2 B Parameter sind auf dem Handy nur mit viel Aufwand realistisch (unbelegt).
- Streaming: nicht angegeben (laut Karte nur Offline/vLLM).
- Vokabular/Prompting: keines dokumentiert, nur Sprachcode; **keine automatische Spracherkennung, keine Zeitstempel**; Zeichensetzung standardmäßig an.
- Halluzination: Karte warnt selbst: Modell „tends to transcribe non-speech sounds", empfiehlt Noise-Gate oder VAD; unsicher bei Code-Switching. Also kein Vorteil gegenüber Whisper bei Stille ohne VAD.

### 2.6 IBM Granite Speech (3.3 / 4.0 1B / 4.1 2B)

- `ibm-granite/granite-speech-4.1-2b`, Apache 2.0, ~2 B (BF16). ASR: Englisch, Französisch, **Deutsch**, Spanisch, Portugiesisch, Japanisch; Zeichensetzung/Großschreibung per Prompt (deutsche Substantiv-Großschreibung ausdrücklich unterstützt; Deutsch-Common-Voice: Interpunktions-Fehlerrate PER 3,66, Cap-F1 99,5) (https://huggingface.co/ibm-granite/granite-speech-4.1-2b).
- Deutsch-WER: **unbelegt** (nur Diagramm); Mittel-WER Open ASR (englisch) 5,33, RTFx 231. In keiner Fassung der Mehrsprachig-Tabelle.
- Laufzeit: Transformers ≥ 4.52.1, vLLM, GGUF Q8_0 über llama.cpp, `mlx-audio` ≥ 0.4.1 (Apple Silicon). Kein Android/ONNX belegt. Varianten: `-plus` (Sprecher, Wort-Zeitstempel), `-nar` (nicht-autoregressiv, schneller).
- Streaming: nicht angegeben. Vokabular: Keyword-Liste im Prompt („Keywords: …"), **nicht-englisches ASR verlangt englischen Prompt.** Halluzination: Autoren räumen ein, kleinere Modelle könnten stärker halluzinieren (unsicher).
- Bewertung: Deutsch-Belege fehlen, kein Vorteil nachgewiesen.

### 2.7 Microsoft Phi-4-multimodal-instruct

- MIT-Lizenz, 5,6 B Parameter (Hub: 6 B), Deutsch unterstützt; Leaderboard-DE 3,96 (v4), FLEURS-de 4,37, CoVoST2-de 4,62; RTFx 62 bis 151 auf Leaderboard-GPU; empfohlene Audiolänge höchstens ~40 s; getestet auf A100/A6000/H100 (https://huggingface.co/microsoft/Phi-4-multimodal-instruct). ONNX-Fassung existiert (Phi-4-multimodal-instruct-onnx).
- Für Franks Geräte unrealistisch (5,6 B, GPU-Bedarf, 40-s-Grenze); kein Streaming; als Sprach-LLM mit Halluzinationsrisiko (Karte warnt). Nicht empfohlen.

### 2.8 Streaming-Kandidaten

- **NVIDIA Nemotron 3.5 ASR Streaming 0.6B** (`nvidia/nemotron-3.5-asr-streaming-0.6b`, 04.06.2026, Lizenz OpenMDW-1.1, kommerziell nutzbar laut Karte): Deutsch dabei, aber FLEURS-de nur 8,31 bis 9,90 % je nach Chunk (80 ms bis 1,12 s), also **deutlich schlechter** als die Offline-Modelle (Parakeet FLEURS-de 5,04). Cache-aware FastConformer-RNNT; Laufzeit NeMo 26.06, NeMo-Speech.cpp (GGUF), Transformers ≥ 5.13; Nvidia-GPU unter Linux gelistet (https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b).
- **Voxtral Mini 4B Realtime 2602** (`mistralai/Voxtral-Mini-4B-Realtime-2602`, Apache 2.0, ~4,4 B = 3,4 B LM + 970 M Encoder, 13 Sprachen): FLEURS-de **6,19 % bei 480 ms**, 4,87 % bei 960 ms, 4,15 % bei 2400 ms, 9,50 % bei 160 ms; Offline-Schwester Voxtral Mini Transcribe 2.0 3,54 % (nur über API, nicht offen). vLLM mit ≥ 16 GB GPU-Speicher empfohlen; Transformers ab 5.2.0; ExecuTorch, voxtral.c, MLX nur „untested" (https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602, https://mistral.ai/news/voxtral-transcribe-2). Die Karte widerspricht sich bei vLLM-Pflicht und Transformers-Unterstützung. Für Handy/Laptop ohne dGPU unrealistisch.
- **Voxtral Mini 3B 2507** (offen, Apache 2.0 laut Mistral-Seite sinngemäß; die Seite listet sowohl „4B" als auch „3B" Repo-Namen, Namen vor Nutzung prüfen): FLEURS-de 4,62, Leaderboard-DE v1 5,36; RTFx 110 bis 136. **Voxtral Small 24B**: Leaderboard-DE 3,01 (bestes in der Tabelle), aber 24 B und RTFx 42: lokal nicht sinnvoll.
- **Qwen3-ASR 1.7B** (Apache 2.0, 30 Sprachen inkl. Deutsch, Leaderboard-DE 4,12): Streaming nur mit vLLM, kein ONNX/GGUF auf der Karte (https://huggingface.co/Qwen/Qwen3-ASR-1.7B). Gleichauf mit Whisper large-v3 (FLEURS 12 Sprachen gepoolt 4,90 gegen 5,27). Kein Gewinn.

### 2.9 Whisper large-v3 / turbo lokal und deutsche Finetunes

- Gewichte identisch zur Groq-Basislinie (Turbo) oder besser (large-v3: −0,4 Punkte). Lokale Runtimes und Fallen siehe bestehende Dateien (whisper.cpp, faster-whisper, Whisper.net, WhisperKit; Best Practices §2 bis §6, Bugs E/H/S/N).
- **primeline/whisper-large-v3-turbo-german**: Apache 2.0, 809 M, Gesamt 2,63 gegen 3,65 Turbo (−28 %, Eigenangabe). Direkter Ersatz in jeder Whisper-Laufzeit; die einzige Option, die ohne neue Laufzeit auskommt. Bereits in Best Practices §3 genannt. Bekannte Falle M5 (CT2-Fassung stoppt nach ~20 Wörtern) beachten.
- **CrisperWhisper** (nyrahealth): Gewichte unter „Nyra Health Non-Commercial Research License", Kommerzielles nur mit Zusatzlizenz; verbatim (Füllwörter „[um]", „[uh]" im Standardmodus), keine Deutsch-WER auf der Seite (https://github.com/nyrahealth/CrisperWhisper). Für Diktat ungeeignet. Sackgasse.
- Qualcomm AI Hub Whisper-Large-V3-Turbo: Encoder (quantisiert w8a16, 30 s Eingabe) 1,13 bis 1,16 s auf Snapdragon 8 Elite Gen 5 (Galaxy S26), Decoder ~6,1 ms je Schritt, ein Gesamt-RTF ist nicht belegt. Die Modellseite meldet widersprüchlich „currently not supported on any Mobile chipset" bei gleichzeitig vorhandenen Profiling-Jobs (https://aihub.qualcomm.com/mobile/models/whisper_large_v3_turbo, https://aihub.qualcomm.com/jobs/jgj0old8p). Ob das Fold 8 diesen Chip trägt, ist eine Annahme; unbelegt. Taugt eher als „Turbo lokal aufs Handy" statt als besseres Deutsch.

### 2.10 Moonshine, Kyutai, Vosk (Sackgassen für Deutsch)

- **Moonshine** (MIT, streaming tiny/small/medium): offizielle Streaming-Modelle nur Englisch; Sprachvarianten für Arabisch, Chinesisch, Japanisch, Koreanisch, Ukrainisch, Vietnamesisch; nicht-englische Nicht-Streaming-Altmodelle unter „Moonshine Community License" (nicht kommerziell). Ein deutsches Modell gibt es nur als Community-Modell `fidoriel/moonshine-tiny-de` unter CC-BY-NC-SA-4.0 (https://huggingface.co/fidoriel/moonshine-tiny-de). Die Moonshine-Doku (https://moonshine-voice.readthedocs.io/en/latest/models/) enthält die Sprachliste nur auf Unterseiten, die ich nicht abrufen konnte: ein offizielles Deutsch-Modell ist damit **nicht ausgeschlossen, aber unbelegt.**
- **Kyutai STT**: `stt-1b-en_fr` (EN/FR, ~1 B, 0,5 s Verzögerung) und `stt-2.6b-en`; **kein Deutsch**; Gewichte CC-BY-4.0 (https://github.com/kyutai-labs/delayed-streams-modeling). Nicht nutzbar.
- **Vosk**: Deutsch vorhanden, aber deutlich schlechter: vosk-model-de-0.21 (1,9 GB, Tuda-De 9,83 %, CV 12,82 %, MLS 12,42 %), vosk-model-small-de-0.15 (45 MB, Tuda 13,75 %), alle Apache 2.0 außer `small-de-zamia` (LGPL-3.0) (https://alphacephei.com/vosk/models). Gegen Turbo (Tuda 8,30) klar schlechter. Nicht empfohlen.

### 2.11 Laufzeiten (für die Integration)

- **sherpa-onnx** (k2-fsa): Parakeet-TDT-v3-int8 fertig exportiert, Android-APK und Mikrofon-Demos mit Silero-VAD „simulated streaming". Der richtige Weg für Android (Kotlin).
- **onnx-asr** (istupakov, MIT): Parakeet v2/v3, Canary v1/v2, Whisper; Windows, Linux, macOS; Provider CUDA/DirectML/TensorRT; VAD-Langform. Der richtige Weg für den Windows-Laptop ohne NeMo/Torch. Intel-NPU/OpenVINO nicht getestet (unbelegt).
- **transcribe.cpp** (handy-computer, MIT): ggml-Laufzeit für Parakeet v3, Cohere Transcribe, Whisper turbo, Canary, Moonshine, Voxtral, Granite, Qwen3-ASR, Nemotron; Windows (Vulkan), macOS (Metal), Linux; Bindings Python/TS/Rust/Swift, **Android nicht erwähnt**, keine RTF-Zahlen auf der Seite; Version 0.1 vom 30.06.2026 mit Migrationshinweisen zu 0.2, noch jung (https://github.com/handy-computer/transcribe.cpp). Warnung: Auf genau dem Chip des Laptops (Core Ultra 7 258V) meldet ein llama.cpp-Issue Instabilität und Leistungseinbruch im Vulkan-Backend (https://github.com/ggml-org/llama.cpp/issues/18946, Januar 2026); transcribe.cpp und whisper.cpp teilen dasselbe ggml, Übertragbarkeit nicht belegt.
- **FluidAudio** (Swift/CoreML, Mac/iOS): Parakeet v3 auf der Neural Engine, siehe 2.1.
- **NeMo**: nur Linux/NVIDIA offiziell; Windows-Eintrag nur bei Canary-Qwen genannt. Für Franks Geräte (kein NVIDIA) irrelevant.

## 3. Kerndaten

| Modell | Lizenz | Größe | DE-WER Fremdmessung (v4) | DE-WER primeline-Prüfstand | Streaming | Android | Windows (Intel) | Mac | Prompt/Boosting | Halluzination |
|---|---|---|---|---|---|---|---|---|---|---|
| Whisper large-v3-turbo (BASIS) | MIT | 809 M | nicht gelistet | 3,65 | nein (Tricks) | whisper.cpp/AI Hub | whisper.cpp, Whisper.net | Metal/MLX | initial_prompt | hoch ohne VAD |
| Whisper large-v3 | MIT | 1,55 B | 4,26 | 3,28 | nein | schwer | ja | ja | initial_prompt | 40,3 % Nicht-Sprache |
| Parakeet-TDT-0.6B-v3 | CC-BY-4.0 | 600 M (~640 MB int8) | 4,20 | 3,64 | chunk-simuliert | sherpa-onnx | onnx-asr/sherpa | FluidAudio/MLX | NeMo-Boosting | wohl niedrig, ungemessen |
| primeline-parakeet | CC-BY-4.0 | 600 M | nicht gelistet | **2,95** | wie Basis | ONNX-int8, sherpa unbelegt | onnx-asr | ONNX/NeMo | wie Basis | wie Basis |
| primeline-turbo-german | Apache 2.0 | 809 M | nicht gelistet | **2,63** | wie Whisper | wie Turbo | wie Turbo | wie Turbo | initial_prompt | wie Turbo |
| Canary-1B-v2 | CC-BY-4.0 | 978 M | 4,10 | nicht gelistet | nein | nein belegt | schwer (CPU RTFx 8) | unbelegt | nein | 134,7 Z./min MUSAN |
| Cohere Transcribe | Apache 2.0 | 2 B | **3,84** | nicht gelistet | nicht belegt | nicht belegt | transcribe.cpp | mlx-audio/GGUF | keines | transkribiert Rauschen |
| Granite Speech 4.1 2B | Apache 2.0 | 2 B | nicht gelistet | nicht gelistet | nicht belegt | nicht belegt | GGUF (llama.cpp) | mlx-audio | Keywords (engl. Prompt) | unsicher |
| Phi-4-multimodal | MIT | 5,6 B | 3,96 | nicht gelistet | nein | nein | nein | nein | Prompt | LLM-Risiko |
| Qwen3-ASR 1.7B | Apache 2.0 | 1,7 B | 4,12 | nicht gelistet | nur vLLM | nein | nein | nein | nicht belegt | nicht belegt |
| Voxtral Small 24B | Apache 2.0 | 24 B | **3,01** | nicht gelistet | nein | nein | nein | nein | Prompt | nicht belegt |
| Voxtral Realtime 2602 | Apache 2.0 | 4,4 B | FLEURS 6,19 @480 ms | nicht gelistet | nativ | nein | nein (≥16 GB GPU) | MLX untested | Prompt | nicht belegt |
| Nemotron 3.5 Streaming | OpenMDW-1.1 | 0,6 B | FLEURS 8,3 bis 9,9 | nicht gelistet | nativ | nein | GGUF-Runtime | unbelegt | nein | nicht belegt |
| CrisperWhisper | nicht kommerziell | je nach Größe | keine Deutsch-Zahl | nicht gelistet | nein | nein | wie Whisper | wie Whisper | – | verbatim |
| Vosk de | Apache/LGPL | 45 MB bis 4,4 GB | – | Tuda 9,5 bis 13,8 | ja | ja | ja | ja | – | gering, aber ungenau |
| Moonshine / Kyutai / Canary-Qwen | – | – | kein Deutsch | – | – | – | – | – | – | – |

## 4. Fazit: Besser als Groq Turbo für Deutsch?

**Ja, aber nicht mit dem Basismodell. Für die Basismodelle gilt: unklar bis gleichauf.**

1. Basis-Parakeet-v3 ist **nicht** nachweislich genauer als Turbo: Gesamt 3,64 gegen 3,65 (Eigenangabe, gleicher Prüfstand), Fremdmessung 4,20 gegen large-v3 4,26 (Turbo wäre ~0,3 bis 0,4 schlechter, in dieser Tabelle nicht gemessen). Sein Vorteil ist Geschwindigkeit (RTFx 1720 bis 3333 gegen ~110 bis 350) und mutmaßlich weniger Whisper-typische Phantomtexte (nur indirekt belegt), nicht die Fehlerrate.
2. **Messbar besser** sind: `primeline/parakeet-primeline` (−19 % WER gegen Turbo, Tuda-De 4,11 gegen 8,30), `primeline/whisper-large-v3-turbo-german` (−28 %, sofort in jeder bestehenden Whisper-Laufzeit nutzbar), und Cohere Transcribe (3,84 gegen large-v3-Näherung 4,26; widersprüchliche Berichte, selbst prüfen). Alle Zahlen der primeline-Modelle sind Eigenangaben des Autors, kein unabhängiger Nachtest gefunden: **vor Umstellung auf eigenen Testsätzen (Eigennamen, Zahlen, Franks Diktatstil) nachmessen.**
3. Eigennamen und Zahlen: für keines der Modelle gibt es eine belastbare deutsche Einzelmessung zu Eigennamen oder Zahlen (unbelegt). Wortlisten-Steuerung gibt es bei Whisper (`initial_prompt`), NeMo-Parakeet (Word Boosting, nur im NeMo-Stack) und Granite (Keyword-Prompt); Cohere hat nichts.
4. Auf Franks Geräten **nicht realistisch**: Voxtral Small 24B, Phi-4 5,6 B, Voxtral Realtime 4,4 B (≥ 16 GB GPU), Canary (NVIDIA/Linux).
5. Streaming-Preis: Echtes Streaming kostet Genauigkeit (Nemotron 8,3 bis 9,9 % FLEURS-de, Voxtral Realtime 6,19 % bei 480 ms gegen 4,15 % bei 2,4 s). Für Push-to-Talk (kurze Äußerung, Endfassung nach Loslassen) ist Offline-Modus mit VAD besser.
6. Plattform-Empfehlung (Hypothese, nicht gemessen): Windows-Laptop → onnx-asr mit `parakeet-primeline` int8 (CPU, danach OpenVINO prüfen) oder Whisper.net mit `primeline-turbo-german`; Android → sherpa-onnx mit Parakeet v3 (Echtgeräte-RTF und Akku selbst messen, bisher nur Emulator 0,12), primeline-ONNX-Kompatibilität prüfen; Mac → FluidAudio (Parakeet v3 auf ANE) oder whisper.cpp mit primeline-turbo-german.
7. Schwächen, die Frank kennen muss: Parakeet gibt kein Prompt-Steuern in den schlanken Laufzeiten; keine bekannten deutschen Fehlerbeispiele für ausgelassene Wörter, deshalb Stichprobentest nötig.

Sackgassen (nicht erneut recherchieren): Canary-Qwen (nur Englisch), Kyutai (EN/FR), Moonshine (Deutsch nur als Community-Modell, nicht kommerziell), Vosk (WER 9,5 bis 13,8 % Tuda), CrisperWhisper (nicht kommerziell, verbatim), Granite (keine Deutsch-Zahl), Phi-4 (zu groß).

## 5. Lücken (unbelegt, für Messung vormerken)

- Echtzeitfaktor aller Modelle auf Core Ultra 7 258V (CPU, Arc 140V, NPU).
- Echtgeräte-RTF und Akku von Parakeet v3 auf dem Fold 8 (sherpa-onnx, NNAPI/QNN).
- Mac-Chip von Franks Heimserver und damit die Mac-Zahlen.
- Halluzinationsrate von Parakeet-TDT-0.6B-v3 auf Stille/Rauschen gegen Whisper turbo.
- Deutsche Eigennamen- und Zahlenfehler je Modell; Turbo auf FLEURS-de/Leaderboard-DE.
- Ob Moonshine inzwischen ein offizielles Deutsch-Modell hat (Unterseite der Doku nicht abrufbar).

BEST-PRACTICES-KANDIDATEN:
- Parakeet-TDT-0.6B-v3 / primeline-parakeet als lokale Nicht-Whisper-Option mit Messwerten (3,64 / 2,95 gegen Turbo 3,65, Eigenangabe), Laufzeiten sherpa-onnx (Android), onnx-asr (Windows), FluidAudio (Mac): https://huggingface.co/primeline/parakeet-primeline (CC-BY-4.0), https://github.com/istupakov/onnx-asr (MIT), https://docs.fluidinference.com/reference/benchmarks, sherpa-onnx-Doku https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/nemo-transducer-models.html (Modell `sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8`).
- Cohere Transcribe (Apache 2.0, 2 B, DE 3,84 Leaderboard v4) als beobachtenswerter offener Kandidat: https://huggingface.co/CohereLabs/cohere-transcribe-03-2026, Version `cohere-transcribe-03-2026`, Transformers ≥ 5.4.0.
- transcribe.cpp als gemeinsame ggml-Laufzeit für viele Modellfamilien: https://github.com/handy-computer/transcribe.cpp, Version 0.1 (30.06.2026), Migration auf 0.2 beachten.
- NeMo Word Boosting für TDT/RNN-T (Eigennamen): https://docs.nvidia.com/nemo/speech/latest/asr/asr_customization/word_boosting.html, NeMo 2.x/26.06.
- Lizenz-Checkliste: CC-BY-4.0 (Parakeet, Canary, primeline-parakeet) nur Namensnennung; CrisperWhisper-Gewichte, Moonshine-Altmodelle und `moonshine-tiny-de` (CC-BY-NC-SA-4.0) nicht kommerziell; vosk-small-de-zamia LGPL-3.0.
- Streaming-Preis dokumentieren: Voxtral Realtime FLEURS-de 9,50 / 8,15 / 6,19 / 4,87 / 4,15 % bei 160 / 240 / 480 / 960 / 2400 ms: https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602 (Version 2602); Nemotron 3.5 Streaming 8,31 bis 9,90 %: https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b (Version vom 04.06.2026).
- Vergleichsregel: DE-WER nur innerhalb einer Messreihe vergleichen (Parakeet v3 FLEURS-de 5,04 NVIDIA gegen 5,9 FluidAudio CoreML; Leaderboard-Paper v1 gegen v4 unterschiedliche Zahlen): https://arxiv.org/html/2510.06961v4.

BUG-KANDIDATEN:
- Korrektur zu bestehendem Eintrag M9 („Canary deckt nur EN/ES/DE/FR ab"): Canary-1B-v2 deckt 25 europäische Sprachen ab: https://huggingface.co/nvidia/canary-1b-v2, Version v2 (CC-BY-4.0).
- Lunar-Lake-Vulkan-Instabilität (Core Ultra 7 258V, genau Franks Laptop) betrifft ggml-basierte Laufzeiten potenziell (llama.cpp; Übertragung auf whisper.cpp/transcribe.cpp nicht belegt): https://github.com/ggml-org/llama.cpp/issues/18946 (Jan. 2026).
- Qualcomm AI Hub: Modellseite „not supported on any Mobile chipset" widerspricht vorhandenen Profiling-Jobs für Whisper-Large-V3-Turbo auf Snapdragon 8 Elite Gen 5: https://aihub.qualcomm.com/mobile/models/whisper_large_v3_turbo (AI-Hub-Version aihub-2026.08.14.0 laut Job-Seite).
- Voxtral Realtime 2602: Modellkarte widerspricht sich (vLLM „currently only" gegen Transformers ≥ 5.2.0); ExecuTorch/MLX/voxtral.c „untested"; ≥ 16 GB GPU für vLLM: https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602. Mistral-News nennt Repo-Namen sowohl 3B als auch 4B: https://mistral.ai/news/voxtral-transcribe-2.
- Granite Speech 4.1: nicht-englisches ASR verlangt englischen Prompt; sonst fehlerhaftes Verhalten möglich: https://huggingface.co/ibm-granite/granite-speech-4.1-2b (transformers ≥ 4.52.1).
- Cohere Transcribe: keine Zeitstempel, keine Spracherkennung, transkribiert Rauschen/Stille ohne VAD, inkonsistent bei Code-Switching: https://huggingface.co/CohereLabs/cohere-transcribe-03-2026 (Transformers ≥ 5.4.0).
- Nemotron 3.5 Streaming: Deutsch-WER-Aufschlag gegen Offline (8,3 bis 9,9 % FLEURS-de) bei Nutzung mit kleinen Chunks: https://huggingface.co/nvidia/nemotron-3.5-asr-streaming-0.6b.
- Onnx-asr: Modelle ohne VAD auf ~20 bis 30 s je Aufruf begrenzt (Falle bei langem Diktat): https://github.com/istupakov/onnx-asr.
- Whisper-Halluzinationsrate auf Nicht-Sprache (40,3 %, nur large-v3; Beam 1 senkt von 28,2 auf 19,5 %; Silero-VAD + BoH-Filter bringt 104,8 auf 6,5 WER): https://arxiv.org/html/2501.11378v1 (bestätigt bestehende Einträge H1/H2, Zahlen ergänzen).
