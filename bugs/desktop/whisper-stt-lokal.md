# Bekannte Bugs/Fallen: On-Device-Whisper / lokale Transkription

> PFLICHT-LESEN vor Arbeit an lokaler Voice-zu-Text in Franks Overlays (Windows + macOS).
> Stand: recherchiert am 2026-06-14 in **zwei Durchläufen** — (1) Best-Practices-Lauf, (2) dedizierte
> Bug-Recherche (je 7 Researcher; Fokus Lauf 2: GitHub-Issues von whisper.cpp/faster-whisper/Silero/Streaming-
> Repos/Whisper.net/WhisperKit + Audio-Pipeline, konkrete Issue-Nummern + Fix-Versionen). ~105 Einträge in 7 Sektionen.
> Versions-Anker: whisper.cpp **v1.8.6** (Fixes v1.8.3/v1.8.4), faster-whisper **1.2.1** (CTranslate2 4.8 /
> CUDA 12 + cuDNN 9), Silero VAD **v6.2**, Modelle large-v3-turbo + primeline-german, Whisper.net **1.9**,
> WhisperKit/argmax-oss-swift **v1.0**.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/desktop/whisper-stt-lokal.md`](../../best-practices/desktop/whisper-stt-lokal.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Sektionen: **E** Engine/Setup/Build · **H** Halluzinationen & Genauigkeit · **S** Streaming/Echtzeit ·
> **M** Modell/Speicher/Quantisierung · **C** Cloud/Hybrid · **A** Audio-Pipeline (Mikro→16 kHz PCM) ·
> **N** Native-Bindings (Whisper.net / WhisperKit).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Whisper erfindet Text bei Stille | VAD vorschalten (Silero v6 eingebaut) | H1 |
| 2 | „Untertitel im Auftrag des ZDF" o.ä. (deutsch) | VAD + `no_speech_threshold 0.6` + Floskel-Stopliste | H2 |
| 3 | Endlos-Wiederholung derselben Phrase | `condition_on_previous_text=False`; `--entropy-thold 2.6` | H3 |
| 4 | Kurzer DE-Befehl wird Englisch/Müll | `language="de"` HART setzen, nie Auto-Detect | H5 |
| 5 | `.en`-Modell für Deutsch = Kauderwelsch | Multilingual/`primeline-german`, nicht `distil-large-v3` | H6 |
| 6 | cuDNN-Ladefehler (NVIDIA) | CUDA 12 + cuDNN 9; sonst `ctranslate2==4.4.0` pinnen | E1 |
| 7 | `transcribe()` „tut nichts" | `segments` ist Generator → `list(segments)` | E7 |
| 8 | whisper.cpp nimmt Datei nicht / Müll | 16-bit/16 kHz/mono WAV; FLAC vorher konvertieren | E5, E6 |
| 9 | faster-whisper auf Mac quälend langsam | kein Metal → auf Mac whisper.cpp nutzen | M4 |
| 10 | OOM beim Modell-Laden | int8 / `q5_0` statt fp16; oder kleineres Modell | M1 |
| 11 | Wort an Chunk-Grenze zersägt (Streaming) | `--keep`-Overlap / CIF | S1 |
| 12 | Cloud friert ein bei schwachem Netz | lokal Default + Timeout-Fallback | C1 |
| 13 | Atem-/Tipp-Geräusch löst Transkription aus | `min_speech_duration_ms 250` + RMS-Gate | H8 |
| 14 | Flash-Attention verschlechtert DE-Ausgabe | `-fa` gezielt weglassen | E10 |
| 15 | Roh-PCM klingt „zu schnell"/Chipmunk → Müll | Auf exakt 16000 Hz resampeln (kein Header-Umbenennen) | A1 |
| 16 | Gibberish/Dauerhalluzination bei Array-Input | Int16→Float32 **`/32768`** auf [-1,1] | A3 |
| 17 | Crash beim Mikro-Start mit AirPods (macOS) | Tap mit HW-Format (`inputFormat`), nie Wunschformat | A8 |
| 18 | WASAPI-Loopback liefert Rauschen | Float32/Stereo/48 kHz lesen, dann konvertieren | A5 |
| 19 | Whisper.net: „Native Library not found" nach Publish | `AllRuntimes` + `/runtimes` mit ins Output | N3 |
| 20 | Whisper.net: `CorruptedWaveException` | Vor `ProcessAsync` nach 16-bit-PCM konvertieren | N5 |
| 21 | primeline-german (CT2) stoppt nach ~20 Wörtern | `max_length`/`begin_suppress_tokens` löschen, n_mels=128 | M5 |
| 22 | large-v3 halluziniert mehr als large-v2 | Bei viel Stille/Rauschen large-v2; VAD + no-context | H10 |
| 23 | Deutsch komplett klein, keine Satzzeichen | `initial_prompt` mit Beispielsatz; large-v3; Truecasing | H12 |
| 24 | Streaming-VAC crasht „Supported: 256/512" | Audio in exakt-512-Sample-Frames puffern | S8 |

---

## E) Engine / Setup / Build

### E1. cuDNN-Ladefehler `Could not load library libcudnn_ops…` (faster-whisper/NVIDIA) ⭐ HAEUFIG
- **Symptom:** Crash/Kernel-Restart beim ersten GPU-Inference; `Unable to load any of {libcudnn_ops.so.9…}`.
- **Ursache:** Versions-Mismatch — CTranslate2 ≥ 4.5 braucht **CUDA 12 + cuDNN 9**; System hat cuDNN 8 / falsche CUDA.
- **Versionen:** CT2 ≥ 4.5 (faster-whisper 1.x).
- **FIX:** cuDNN 9 installieren (`pip install nvidia-cublas-cu12 nvidia-cudnn-cu12==9.*` + `LD_LIBRARY_PATH` setzen); ODER auf cuDNN 8 pinnen: CUDA 12 → `ctranslate2==4.4.0`, CUDA 11 → `3.24.0`; ODER NVIDIA-Lib-Archiv von Purfviews whisper-standalone-win in den PATH.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper (README GPU) · https://github.com/SYSTRAN/faster-whisper/issues/1236 (extern)

### E2. „This CTranslate2 package was not compiled with CUDA support" / Windows-cuDNN-DLL fehlt
- **Symptom:** CT2 meldet keine CUDA-Unterstützung oder DLL fehlt nach CT2-Upgrade.
- **Ursache:** CUDA/cuDNN/CT2-Versionen passen nicht (häufig CUDA 11 + cuDNN 8 mit neuer CT2).
- **Versionen:** CT2 4.x.
- **FIX:** CT2 zur installierten CUDA pinnen (E1); Docker-Basis `nvidia/cuda:12.3.2-cudnn9-runtime-ubuntu22.04`.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1086 (extern)

### E3. „CUDA driver version is insufficient for CUDA runtime version"
- **Symptom:** Fehler beim GPU-Start.
- **Ursache:** GPU-Treiber zu alt für die CUDA-12-Runtime der CT2-Wheel.
- **Versionen:** CT2 ≥ 4.5.
- **FIX:** NVIDIA-Treiber aktualisieren ODER CT2 auf eine zur installierten CUDA passende Version pinnen.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/734 (extern)

### E4. Windows: kein Vulkan in offiziellen whisper.cpp-Release-Binaries
- **Symptom:** Nutzer mit AMD/Intel-GPU bekommt nur CPU-Performance.
- **Ursache:** Offizielle Assets liefern nur CPU + CUDA, kein Vulkan.
- **Versionen:** whisper.cpp Releases.
- **FIX:** Vulkan selbst bauen (`-DGGML_VULKAN=1`) oder Community-Binary; für die ausgelieferte .exe einen Vulkan-Build mitliefern.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3673 (extern)

### E5. whisper-cli akzeptiert nur 16-bit/16 kHz WAV
- **Symptom:** Andere Formate scheitern oder klingen falsch.
- **Ursache:** Das CLI-Beispiel dekodiert standardmäßig nur 16-bit WAV (miniaudio); breitere Formate nur mit `-DWHISPER_COMMON_FFMPEG=yes`.
- **Versionen:** whisper.cpp alle.
- **FIX:** Mit ffmpeg-Support bauen ODER konsequent vorab konvertieren: `ffmpeg -i in -ar 16000 -ac 1 -c:a pcm_s16le out.wav`.
- **Quelle:** https://github.com/ggml-org/whisper.cpp (README, offiziell)

### E6. FLAC-Datei triggert Repetition/„haywire", identisches WAV läuft sauber
- **Symptom:** Bestimmte komprimierte Formate erzeugen Wiederholungen, das WAV nicht.
- **Ursache:** Audio-Dekodierungsproblem im Decoder-Pfad bei bestimmten komprimierten Formaten.
- **Versionen:** whisper.cpp.
- **FIX:** Immer vorab zu 16-bit/16 kHz/mono WAV konvertieren, nicht FLAC/komprimiert direkt einspeisen.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3687 (extern)

### E7. faster-whisper: `segments` ist ein Generator (lazy) ⭐ HAEUFIG
- **Symptom:** `transcribe()` kehrt sofort zurück, aber „nichts passiert"; Fehler erst später beim Iterieren.
- **Ursache:** `segments` ist ein lazy Generator — die Berechnung startet erst beim Durchlaufen.
- **Versionen:** faster-whisper alle.
- **FIX:** `segments = list(segments)` oder `for`-Loop; try/except um die Iteration legen, nicht um den `transcribe()`-Call.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper (README Usage-Warnung, offiziell)

### E8. Core ML / OpenVINO: erster Lauf extrem langsam (scheint zu hängen)
- **Symptom:** Beim ersten Start scheinbares Hängen (Sekunden bis Minuten).
- **Ursache:** ANE/OpenVINO kompiliert das Modell beim ersten Lauf in ein gerätespezifisches Format.
- **Versionen:** whisper.cpp Core ML / WhisperKit.
- **FIX:** Normal — Ergebnis wird gecacht, Folgeläufe schnell. Im UI „Modell wird vorbereitet" zeigen, nicht als Hänger interpretieren. Core-ML-Export braucht `xcode-select --install` + `pip install coremltools`.
- **Quelle:** https://github.com/ggml-org/whisper.cpp (README Core ML, offiziell)

### E9. whisper.cpp Vulkan-Build in v1.8.0 kaputt
- **Symptom:** Vulkan-Build schlägt fehl / läuft nicht.
- **Ursache:** Regression in v1.8.0.
- **Versionen:** v1.8.0; gefixt ≥ v1.8.3 (brachte bis 12× iGPU-Boost).
- **FIX:** Aktuellere Version (≥ 1.8.3) bauen.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3455 (extern)

### E10. Flash-Attention verschlechtert Nicht-EN-Qualität (Deutsch/Japanisch)
- **Symptom:** Halluzinationen/Drift bei deutscher Ausgabe seit FA-Default.
- **Ursache:** Flash-Attention ist seit whisper.cpp v1.8.0 Default (`-fa`); numerisch äquivalent, driftet aber bei manchen Nicht-EN-Sprachen.
- **Versionen:** whisper.cpp ≥ 1.8.0.
- **FIX:** FA gezielt abschalten (`-fa` weglassen/deaktivieren), wenn deutsche Ausgabe halluziniert — kein globaler Downgrade nötig.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3020 (extern)

### E11. Whisper.net (C#): fehlende Runtime / AVX-Anforderung
- **Symptom:** Crash beim Laden; „runtime not found"; oder auf alter CPU keine Funktion.
- **Ursache:** x86/x64-CPU braucht AVX/AVX2/FMA/F16C; MSVC-2022-Redistributable nötig; CUDA-Runtime ist in `.Cuda` (CUDA 13) / `.Cuda12` gesplittet.
- **Versionen:** Whisper.net 1.9.
- **FIX:** `Whisper.net.AllRuntimes` referenzieren (Auto-Priorität Cuda→Cuda12→Vulkan→CoreML→CPU); für CPUs ohne AVX `Whisper.net.Runtime.NoAvx`; MSVC-Redist mitliefern. (Tiefere Whisper.net-Bugs: Sektion N.)
- **Quelle:** https://github.com/sandrohanea/whisper.net (extern)

### E12. whisper.cpp `--vad`: Timestamps konkateniert, Stille-Lücken gehen verloren
- **Symptom:** Mit `--vad` ist die SRT-Zeitachse lückenlos (`segment[n].end ≈ segment[n+1].start`); von VAD erkannte Stille erscheint nicht als Lücke. Weicht von Python-openai-whisper ab.
- **Ursache:** whisper.cpp generiert Timestamps pro Segment relativ (Reset auf 0); beim Zusammensetzen werden die VAD-Lücken nicht zurückgemappt.
- **Versionen:** offen v1.8.x.
- **FIX:** Pro VAD-Segment den absoluten Start-Offset mitführen und auf die Ausgabe-Timestamps addieren; bis zum Upstream-Fix SRT nachbearbeiten.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3634 (extern)

### E13. whisper.cpp VAD-Timestamp-Drift durch Overlap-Samples → gefixt v1.8.4
- **Symptom:** Mit `--vad` + `--vad-samples-overlap` driften die Timestamps, Wörter verschoben.
- **Ursache:** Overlap-Samples wurden bei der Rückrechnung auf die Original-Zeitachse nicht abgezogen.
- **Versionen:** bis v1.8.3; **gefixt v1.8.4** (PR #3711).
- **FIX:** Auf v1.8.4+ updaten.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/pull/3711 (extern)

### E14. whisper.cpp VAD-Buffer-Overflow in der Sample-Reduktion → gefixt v1.8.3
- **Symptom:** Crash/Speicherkorruption bei VAD-Verarbeitung bestimmter Audiolängen.
- **Ursache:** Out-of-bounds-Schreibzugriff in der Sample-Reduction-Loop des VAD-Codes.
- **Versionen:** bis v1.8.2; **gefixt v1.8.3** (PR #3558).
- **FIX:** Auf v1.8.3+ updaten.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/releases/tag/v1.8.3 (extern)

### E15. whisper.cpp VAD: Wortanfang abgeschnitten / falsche lange Timestamps
- **Symptom:** Mit `--vad` werden Wortanfänge abgeschnitten und ins nächste Segment gezogen (z.B. ein Wort bekommt fälschlich 16 s Dauer). Mit `medium.en`.
- **Ursache:** Segmentgrenzen-Handling von VAD-Chunks ohne sauberes Padding/Overlap an Übergängen.
- **Versionen:** offen v1.8.x.
- **FIX:** `--vad-samples-overlap` erhöhen + VAD-Threshold senken; Overlap-Korrektur #3711 (v1.8.4) lindert teilweise.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3207 (extern)

### E16. whisper.cpp VAD-Modell lädt nicht auf Core-ML-Build (Apple Silicon)
- **Symptom:** `failed to open VAD model` / `failed to compute VAD` beim Core-ML-Build, obwohl die Datei am Pfad liegt.
- **Ursache:** Pfad-/Modell-Auffindung beim Core-ML-Build; falsche/veraltete Silero-Version (v5.1.2 vs. erwartetes v6.2.0, Bump in v1.8.3).
- **Versionen:** v1.7.6 gemeldet; Modell-Bump v1.8.3.
- **FIX:** Aktuelles `ggml-silero-v6.2.0.bin` mit absolutem Pfad referenzieren; Build-Version und Modellversion abstimmen. Notfalls Nicht-Core-ML-Build für den VAD-Schritt.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3299 (extern)

### E17. whisper.cpp `whisper-server`: Handle-/Memory-Leak → leere Antworten im Dauerbetrieb ⭐ HAEUFIG
- **Symptom:** Nach ~6–7 Requests liefert der Server dauerhaft `{"text": ""}`; Handle-Count/Speicher steigt monoton, kein Crash. Backend-unabhängig.
- **Ursache:** `whisper_state`/OS-Handles werden pro Request nicht freigegeben.
- **Versionen:** offen v1.8.x.
- **FIX:** State pro Request freigeben (`whisper_free_state`, Datei-Handles schließen). Workaround: Server periodisch neustarten / Request-Counter mit Auto-Restart.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3358 · https://github.com/ggml-org/whisper.cpp/issues/2605 (extern)

### E18. whisper.cpp RTX-50/Blackwell: `Unsupported gpu architecture 'compute_120'` + CUDA-Graph-Crash
- **Symptom:** CUDA-Build auf RTX 5080/5090 bricht ab; oder `CUDA error: invalid argument` in `ggml_backend_cuda_graph_compute`.
- **Ursache:** sm_120 (Blackwell) erst ab CUDA 12.8+; CUDA-Graph-Capture-Pfad passt bei wechselnden Tensor-Shapes nicht.
- **Versionen:** offen v1.8.x mit altem Toolkit.
- **FIX:** CUDA Toolkit ≥ 12.8 + `-DCMAKE_CUDA_ARCHITECTURES=120`; CUDA-Graphs deaktivieren (`GGML_CUDA_DISABLE_GRAPHS=1`); bei cuBLAS-`NOT_SUPPORTED` auf float16 ausweichen.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/3030 · https://github.com/ggml-org/whisper.cpp/issues/2258 (extern)

### E19. whisper.cpp quantisiertes Modell q8_0 lädt nicht / Metal langsamer
- **Symptom:** `q8_0`-Modell lädt nicht (`GGML_ASSERT: wtype != GGML_TYPE_COUNT`), `q5_1` lädt; auf Metal ist q5/q8 teils langsamer als F16.
- **Ursache:** Nicht unterstützter/inkonsistenter `wtype` im jeweiligen Build; Metal-Quant-Kernel weniger optimiert als F16-Pfad.
- **Versionen:** mehrere.
- **FIX:** Modell mit derselben whisper.cpp-Version neu quantisieren, keine alten Quant-Binaries mischen; auf Apple Silicon F16 statt q5/q8 wenn RAM reicht (Quant dort nur zur RAM-Ersparnis).
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/993 · https://github.com/ggml-org/whisper.cpp/issues/2241 (extern)

### E20. `whisper-stream`/SDL2: App beendet sofort, leerer/langsamer Output (Windows)
- **Symptom:** `whisper-stream` findet Capture-Devices, beendet sich dann sofort ohne Fehler; auf Windows extrem langsamer/leerer Output, keine Device-Auswahl bei mehreren Mikros.
- **Ursache:** SDL2-Default-Capture-Device-Init schlägt fehl/blockiert; Real-Time-Latenz im Stream-Pfad.
- **Versionen:** offen; miniaudio-Bump v1.8.4.
- **FIX:** Capture-Device per Index wählen; SDL2-Init-Fehler abfangen; miniaudio statt SDL2 (CLI v1.8.4), MSVC+SDL2-UTF8-Fix (#2826).
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/2062 (extern)

### E21. faster-whisper: parallele `transcribe`-Calls laufen nicht echt gleichzeitig
- **Symptom:** Mehrere `transcribe`-Calls über `ThreadPoolExecutor` (auch `num_workers=2`) laufen seriell — GPUs wechseln sich ab statt parallel.
- **Ursache:** CTranslate2 serialisiert die Requests effektiv (Thread-Scheduling), trotz GIL-Release.
- **Versionen:** offen 1.2.x / CT2 4.8.
- **FIX:** **`ProcessPoolExecutor` statt `ThreadPoolExecutor`**; ein `WhisperModel` pro GPU/Prozess; für Server eine Modell-Instanz + interne Request-Queue.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1333 (extern)

### E22. CTranslate2 4.8: `intra_threads=0` oversubscribed CPU massiv (macOS/ARM)
- **Symptom:** Mit Default `intra_threads=0` CPU-Last ~1470 %, Durchsatz bricht ein; auf CT2 4.7.2 nativer Abort nach Hunderten Calls.
- **Ursache:** No-OpenMP-CPU-Backend wählt bei `intra_threads=0` pathologische Thread-Zahl, oversubscribed Kerne.
- **Versionen:** CT2 4.7.2 (Abort) / 4.8.0 (Oversubscription), offen.
- **FIX:** `WhisperModel(..., cpu_threads=1)` explizit setzen (schnellster + stabilster Wert).
- **Quelle:** https://github.com/OpenNMT/CTranslate2/issues/2063 (extern)

### E23. faster-whisper BatchedInferencePipeline + VAD: Anfangsphrasen verschluckt
- **Symptom:** Im Batched-Pfad fehlen ganze Phrasen am Audio-Anfang; der Non-Batch-Pfad transkribiert korrekt. Bei `vad_filter=True`.
- **Ursache:** `merge_segments` wendet `speech_pad_ms` doppelt an und merged Segmente auch über große Lücken hinweg (ab 3 Segmenten > `max_speech_duration_s`).
- **Versionen:** offen 1.2.x (verwandt #1175).
- **FIX:** Non-Batch-Pfad nutzen; oder `speech_pad_ms` niedriger; oder `merge_segments` lokal als Pass-Through patchen.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1270 (extern)

### E24. faster-whisper Batched + `clip_timestamps`: völlig falsche Segment-Timestamps
- **Symptom:** Mit `clip_timestamps` im Batched-Pfad sind die Timestamps wild falsch, im Extremfall kollabiert alles zu einem Segment.
- **Ursache:** `restore_speech_timestamps()` rechnet die externen Clip-Grenzen falsch zurück.
- **Versionen:** offen 1.2.x.
- **FIX:** Externe Segmentierung über den Non-Batch-Pfad fahren; Timestamps clientseitig korrigieren.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1361 (extern)

### E25. faster-whisper Batched: `<|nocaptions|>`-Token erscheint als Text
- **Symptom:** Batched gibt wörtlich `<|nocaptions|>` als Segmenttext aus; `WhisperModel` transkribiert korrekt.
- **Ursache:** Im Batched-Pfad fehlen die Suppress-Tokens für das `<|nocaptions|>`-Token.
- **Versionen:** offen 1.2.x.
- **FIX:** Eigene `suppress_tokens`-Liste übergeben (inkl. `1771, 496, 9799`); PR #1338/#1297 beachten.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1319 (extern)

### E26. faster-whisper `word_timestamps=True`: Divide-by-Zero killt den ganzen Prozess (Windows)
- **Symptom:** Bei bestimmten WAVs harter Prozess-Crash (Exit `0xC0000094`), **nicht** aus Python abfangbar. Nur mit `word_timestamps=True` + large-v2.
- **Ursache:** Division durch 0 im Word-Alignment, wenn das Decoding für ein Segment vollständig degeneriert (Compression-Ratio verletzt).
- **Versionen:** offen (Windows, CPU).
- **FIX:** Verarbeitung in Subprozess kapseln (Hard-Crash isolieren), Segment ohne Word-Timestamps retry; large-v3 testen.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1342 (extern)

### E27. faster-whisper: `hotwords` werden ignoriert
- **Symptom:** `transcribe(..., hotwords="...")` ändert das Ergebnis nicht; Eigennamen weiter falsch.
- **Ursache:** Regression durch PR #856 — Hotword-Tokens fließen nicht mehr in den Decoder-Kontext.
- **Versionen:** offen seit PR #856.
- **FIX:** Hotwords über `initial_prompt` als Prompt-Präfix; größeres Modell (Effekt teils noch da). (Eigennamen-Prompt verpufft später → H13.)
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1076 (extern)

### E28. faster-whisper `multilingual=True`: Pro-Segment-Sprache nicht zugänglich
- **Symptom:** Bei Sprachwechsel trägt `Segment` kein `language`-Feld, nur globales `info.language` — mehrsprachige Untertitel nicht auswertbar.
- **Ursache:** Designlücke; pro-Segment erkannte Sprache wird nicht durchgereicht.
- **Versionen:** offen 1.2.x.
- **FIX:** Bis Fix jedes Segment einzeln mit explizitem Language-Detection-Call nachverarbeiten (teuer); oder Fork mit `Segment.language`.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1233 (extern)

### E29. faster-whisper: VAD entfernt allen Inhalt → `ValueError: max() arg is an empty sequence` → gefixt
- **Symptom:** Bei Audio ohne erkennbare Sprache wirft `transcribe(..., vad_filter=True)` unbehandelten `ValueError` statt leerem Ergebnis.
- **Ursache:** Nach VAD 0 Segmente; Sprach-Detektion ruft `max()` über leere Sequenz.
- **Versionen:** in 1.0.3 reproduziert; **gefixt in späteren Releases** (1.2.x verifizieren).
- **FIX:** Auf 1.2.x updaten; bei alten Pins selbst Guard auf leere Segmentliste setzen.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1208 (extern)

---

## H) Halluzinationen & Genauigkeit

### H1. Halluzinationen / Endlos-Wiederholungen bei Stille oder Rauschen ⭐ HAEUFIG
- **Symptom:** Whisper „erfindet" Text in stillen Passagen, oft in Wiederholungsschleifen.
- **Ursache:** Whisper erwartet immer Sprache und füllt Stille mit erfundenem Text (30-s-Padding, Offline-Training).
- **Versionen:** alle (Architektur).
- **FIX:** (a) **VAD aktivieren** (Silero, wirksamster Hebel); (b) whisper.cpp `--entropy-thold 2.6`, `--logprob-thold -1.25`; (c) im Code `no_context=true`/`condition_on_previous_text=False` pro Segment.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/1724 (extern)

### H2. Deutsche „ZDF/funk"-Floskeln bei Stille ⭐ HAEUFIG
- **Symptom:** Bei Stille/Musik erzeugt Whisper „Untertitel im Auftrag des ZDF", „Vielen Dank fürs Zuschauen", „Untertitel der Amara.org-Community".
- **Ursache:** Whisper wurde auf YouTube-Untertiteln öffentlich-rechtlicher Sender trainiert; am Videoende stehen über Musik/Stille Copyright-Hinweise → „Stille → Copyright-Floskel".
- **Versionen:** alle (deutsche Trainingsdaten-Bias).
- **FIX:** VAD-Vorfilter + `no_speech_threshold=0.6` + `log_prob_threshold`; zusätzlich **Floskel-Stopliste/Regex** im Nachfilter (`untertitel`, `zdf`, `amara.org`, `vielen dank fürs zuschauen`). Alle drei zusammen — VAD allein reicht im Grenzfall nicht.
- **Quelle:** https://gist.github.com/riotbib/3b3c5f817b55b68801d14b8bdb02df09 (extern) · https://github.com/openai/whisper/discussions/928 (extern)

### H3. Endlos-Repetition / Token-Loop
- **Symptom:** Dieselbe Phrase wird endlos wiederholt.
- **Ursache:** Bestimmte Audiomuster + Prompt-History lösen Loop aus.
- **Versionen:** alle (v3 seltener als v2).
- **FIX:** `condition_on_previous_text=False` + `vad_filter=True`; optional `hallucination_silence_threshold=2.0`, `no_repeat_ngram_size=3`; whisper.cpp `--entropy-thold 2.6`. Die `temperature`-Fallback-Treppe + `compression_ratio_threshold=2.4` greifen bei Schleifen automatisch.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/blob/master/faster_whisper/transcribe.py (offiziell)

### H4. Halluzinations-Drift über mehrere Segmente
- **Symptom:** Ein einmal halluzinierter Satz wird in den folgenden Segmenten weitergeführt.
- **Ursache:** `condition_on_previous_text=True` (Default) gibt vorigen Output als Prompt weiter → Selbstverstärkung.
- **Versionen:** alle.
- **FIX:** `condition_on_previous_text=False` (verliert minimal Kontext-Kohärenz, eliminiert aber Drift — fürs Overlay fast immer richtig).
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/discussions/349 (extern)

### H5. Falsche Sprach-Autoerkennung kurzer deutscher Befehle ⭐ HAEUFIG
- **Symptom:** Kurzer deutscher Befehl wird als Englisch/Niederländisch transkribiert oder triggert fremdsprachige Halluzination (z.B. arabisches „ترجمة").
- **Ursache:** Auto-Language-Detection ist bei kurzen/leisen Clips unzuverlässig.
- **Versionen:** alle.
- **FIX:** `language="de"` / `-l de` IMMER hart setzen, nie Auto-Detect bei Befehlen.
- **Quelle:** https://github.com/openai/whisper/discussions/2608 (extern)

### H6. English-only-Modell für Deutsch geladen
- **Symptom:** `.en`-Modell (`tiny.en`/`base.en`) oder `distil-large-v3` liefert für Deutsch Müll.
- **Ursache:** `.en`-Modelle und das offizielle HF-`distil-large-v3` sind English-only.
- **Versionen:** alle.
- **FIX:** Multilingual-Modelle (`base`/`small`/`medium`/`large-v3`/`large-v3-turbo`) bzw. deutsche Finetunes (`primeline/whisper-large-v3-turbo-german`, `primeline/distil-whisper-large-v3-german`).
- **Quelle:** https://huggingface.co/distil-whisper/distil-large-v3 (offiziell)

### H7. `temperature`-Fallback halluziniert bei kurzen Befehlen
- **Symptom:** Bei niedriger Konfidenz erhöht Whisper die Temperatur und „rät" → erfundene Wörter.
- **Ursache:** Default-`temperature`-Fallback-Treppe (0 → 0.2 → … → 1.0) bei kurzen/leisen Eingaben.
- **Versionen:** alle.
- **FIX:** Für kurze Befehle `temperature=0` fest; `log_prob_threshold`/`no_speech_threshold` als Verwurf-Kriterium statt Raten (leeres Ergebnis > Halluzination).
- **Quelle:** https://github.com/openai/whisper/discussions/679 (extern)

### H8. Atem-/Tipp-/Klick-Geräusche lösen Transkription aus
- **Symptom:** Tastaturklick, Mausklick, Einatmen erzeugen kurze „Sprach"-Segmente → Fehl-Trigger.
- **Ursache:** Impulsgeräusche überschreiten kurz den VAD-`threshold`; `min_speech_duration_ms=0` (Default) lässt sie durch.
- **Versionen:** alle.
- **FIX:** `min_speech_duration_ms=250` (Befehle 150); zusätzlich RMS-Gate gegen leise Tipp-Geräusche; im Endpointer `MIN_UTTERANCE_MS`. **Kein blindes `noisereduce`** davor (macht es schlimmer).
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/843 (extern)

### H9. Halluzinationen aus VORHERIGEN Transkriptionen im Dauerbetrieb (ohne Reload)
- **Symptom:** Im Server-/Overlay-Dauerbetrieb tauchen Wiederholungen aus früheren Äußerungen auf.
- **Ursache:** Decoder-State/Prompt-History bleibt zwischen Aufrufen erhalten.
- **Versionen:** alle.
- **FIX:** Pro neuer Äußerung State zurücksetzen (`no_context=true` / CLI `--no-context`); bei Push-to-Talk pro Tastendruck frischer State; bei whisper-server Request-Isolation sicherstellen.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/2445 (extern)

### H10. large-v3 produziert MEHR Halluzinationen/Wiederholungen als large-v2 (Regression) ⭐ HAEUFIG
- **Symptom:** Nach Upgrade auf large-v3 wiederkehrende Werbe-/Floskel-Einblendungen alle ~20 s, erfundene Namen am Anfang, mehr fehlende Sätze — large-v2 erzeugt das beim selben Audio nicht.
- **Ursache:** v3 mit Pseudo-Labels + 128 Mel-Bins trainiert, empfindlicher gegen Stille/Rauschen. (OpenAI nutzt in der API weiter v2.)
- **Versionen:** large-v3 (offen, kein Code-Fix); large-v2 stabil.
- **FIX:** Bei viel Stille/Rauschen large-v2; falls v3 nötig, `condition_on_previous_text=False` + Temperatur-Fallback-Tupel + VAD vorschalten.
- **Quelle:** https://github.com/openai/whisper/discussions/2280 · https://github.com/ggml-org/whisper.cpp/issues/1507 (extern)

### H11. faster-whisper: `no_speech`-Logik unterspringt Temperatur-Fallback → deutsche Floskel-Loops
- **Symptom:** Wenn `no_speech_threshold` greift, kommen statt Stille Halluzinationen — auf Deutsch ganze Floskel-Schleifen, obwohl `compression_ratio` weit über Schwelle.
- **Ursache:** Code-Logik-Konflikt: Segment wird als „silence" markiert und überspringt die Fallback-Kette, durchläuft sie in der nächsten Prüfung aber trotzdem nicht — die Compression-Ratio-Prüfung wird wirkungslos.
- **Versionen:** faster-whisper (Logik-Konflikt transcribe.py L455-463 vs L721-725).
- **FIX:** VAD-Filter vorschalten + `compression_ratio_threshold≈2.4` + Temperatur-Tupel `(0.0…1.0)` erzwingen; Purfview-Standalone-Patch nutzt alle Fallbacks auch bei hohem `no_speech_prob`.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/621 (extern)

### H12. Deutsch: durchgehend Kleinschreibung + fehlende Satzzeichen ⭐ HAEUFIG
- **Symptom:** Deutscher Text komplett klein und ohne Interpunktion — bei Deutsch gravierend wegen Substantiv-Großschreibung.
- **Ursache:** Fehlende Punktuation im Trainingssignal bei bestimmten Audio-Typen; kleinere/quantisierte Modelle verstärken es.
- **Versionen:** alle, am stärksten small/medium + quantisiert (offen).
- **FIX:** `initial_prompt` mit korrekt geschriebenem Beispielsatz („Sätze beginnen mit einem Großbuchstaben und enden mit einem Punkt."); large-v3 statt small/medium; Truecasing-/Punktuierungs-Nachbearbeitung.
- **Quelle:** https://github.com/openai/whisper/discussions/290 · https://github.com/SYSTRAN/faster-whisper/issues/601 (extern)

### H13. `initial_prompt`/hotwords verpuffen später — Eigennamen ab Block 2 wieder falsch
- **Symptom:** Deutsche Eigennamen/Fachbegriffe am Audio-Anfang korrekt, später im langen Audio wieder falsch.
- **Ursache:** `initial_prompt` wird nur in den ersten Decode-Kontext gelegt; bei späteren 30-s-Blöcken ist er „aufgebraucht".
- **Versionen:** Vanilla whisper ohne Carry-Option; **gefixt via PR #2343** (`carry_initial_prompt`).
- **FIX:** `carry_initial_prompt=True` setzen (prependet den Prompt vor jeden internen `decode()`-Call); in faster-whisper zusätzlich `hotwords` (Vorsicht E27).
- **Quelle:** https://github.com/openai/whisper/pull/2343 (extern)

### H14. Inkonsistente Zahl-/Zeit-/Datums-Normalisierung (Deutsch)
- **Symptom:** Gesprochenes mal als Ziffer, mal ausgeschrieben; konkret „viertel nach sechs" → „4.15" (falsche Uhrzeit UND Format).
- **Ursache:** Whisper macht inverse Text-Normalisierung nicht-deterministisch ohne festes Schema; deutsches Uhrzeit-Verständnis fehlt.
- **Versionen:** alle (architektonisch, offen).
- **FIX:** Deterministisches Post-Processing/ITN nachschalten; `initial_prompt` mit gewünschtem Stil; zeit-kritische Werte gegen Audio verifizieren.
- **Quelle:** https://github.com/openai/whisper/discussions/1982 (extern)

### H15. Code-Switching Deutsch+Englisch: Modell übersetzt statt zu transkribieren
- **Symptom:** Bei gemischtem DE/EN (Anglizismen, Fachbegriffe) gibt Whisper teils eine Übersetzung statt wörtlicher Transkription; phonetisch ähnliche Tokens verwechselt.
- **Ursache:** Whisper ist auf monolinguales Audio ausgelegt; Sprach-ID läuft nur über die ersten 30 s fürs ganze File, intra-Satz-Code-Switching nicht vorgesehen.
- **Versionen:** alle (architektonisch, offen).
- **FIX:** Audio per VAD/Diarisierung in monolinguale Segmente schneiden, je Segment feste `language`; `task="transcribe"` erzwingen (nie `translate`); Fachbegriffe via `hotwords`.
- **Quelle:** https://github.com/openai/whisper/discussions/2009 (extern)

### H16. large-v3-turbo Core ML: keine Transkription mehr nach langen Rausch-Segmenten
- **Symptom:** Lange Aufnahme: erstes Gespräch perfekt, nach ~90 Min Lärm KEINE Transkription mehr — auch nicht für spätere saubere Passagen; stattdessen „Okay.", „I'm sorry.", „One, two, three…".
- **Ursache:** Kontextverschleppung „vergiftet" den Decoder-Zustand; verschärft durch einen Bug in whisper-cli 1.7.4 (250113) der Core-ML-Pipeline.
- **Versionen:** whisper-cli 1.7.4; Workaround über Go-Bindings.
- **FIX:** `--no-context`/`condition_on_previous_text False`; lange Aufnahmen vorab per VAD in Chunks schneiden; Go-Bindings umgehen den CLI-Pfad.
- **Quelle:** https://github.com/openai/whisper/discussions/2496 (extern)

### H17. Schweizer-/Dialekt-Deutsch: Modell „übersetzt" sinngemäß statt phonetisch + lässt aus
- **Symptom:** Schweizerdeutsch wird zu plausiblem, aber inhaltlich falschem Hochdeutsch; subtile Dialekt-Ausdrücke fehlen.
- **Ursache:** Keine Dialekt-Orthografie-Norm; Fine-Tuning-Daten mischen wörtliche Transkription und Hochdeutsch-Übersetzung → Modell lernt teils semantisch.
- **Versionen:** Dialekt-Finetunes (offen).
- **FIX:** Nur für semantische Analyse nutzen, nicht wortgetreu; ggf. auf kanton-spezifische Daten fine-tunen. Kein reiner Konfig-Fix.
- **Quelle:** https://huggingface.co/nizarmichaud/whisper-large-v3-turbo-swissgerman/discussions/2 (extern)

---

## S) Streaming / Echtzeit

### S1. Wort an Chunk-Grenze zersägt
- **Symptom:** Beim Sliding-Window-Streaming wird ein Wort mitten zerschnitten → Inferenz auf Teilwort halluziniert.
- **Ursache:** Naives Fixed-Window schneidet mitten im Wort.
- **Versionen:** alle Streaming-Setups.
- **FIX:** `--keep`-Overlap (whisper.cpp, Kontext über Fenstergrenze) bzw. CIF-Modell + letztes Wort trimmen (SimulStreaming); LocalAgreement bestätigt nur stabile Präfixe.
- **Quelle:** https://github.com/ufal/SimulStreaming (extern)

### S2. Müll bei erzwungenen Partials mit kleinem Chunk
- **Symptom:** Sofort-Partials auf sehr kleinen Chunks sind instabil/halluziniert.
- **Ursache:** LocalAgreement auf zu kleinen Chunks → instabile Zwischen-Hypothesen.
- **Versionen:** whisper_streaming/LocalAgreement.
- **FIX:** Partials erst ab Min-Chunk emittieren; Partials grau/überschreibbar anzeigen, nur Finals festschreiben (Zwei-Puffer-Modell).
- **Quelle:** https://arxiv.org/html/2506.12154v1 (extern)

### S3. Buffer-Freeze bei langer Stille ohne Endpoint
- **Symptom:** Text bleibt hängen, Puffer wächst, kein Trim-Trigger.
- **Ursache:** Kein Token committed → kein Trim.
- **Versionen:** Streaming-Backends.
- **FIX:** Auto-Reset, wenn `buffer_duration > buffer_trimming_sec` und länger still (WhisperLiveKit-Logik); VAC-Endpointing.
- **Quelle:** https://github.com/QuentinFuxa/WhisperLiveKit (extern)

### S4. RTF > 1 — nicht echtzeitfähig
- **Symptom:** Streaming kann mit dem Sprecher nicht mithalten.
- **Ursache:** LocalAgreement reprozessiert Audio mehrfach; large-v3 auf CPU zu langsam.
- **Versionen:** alle.
- **FIX:** Chunk ≥ 1 s, kleineres Live-Modell + Two-Pass-Final; GPU/MLX-Backend; AlignAtt (SimulStreaming ~5× schneller).
- **Quelle:** https://arxiv.org/html/2506.12154v1 (extern)

### S5. Streaming-Latenz-Untergrenze (~3 s mit medium)
- **Symptom:** Kürzere `--step`-Werte senken die Latenz nicht weiter.
- **Ursache:** Das Encoder-Encoding dominiert; kürzere Steps helfen ab einem Punkt nicht mehr.
- **Versionen:** alle.
- **FIX:** `large-v3-turbo` statt `medium`; GPU/Metal; Erwartung managen (Two-Pass: kleines Live-Modell für Partials).
- **Quelle:** https://allenkuo.medium.com/choosing-a-real-time-whisper-engine-c4eeb5885e22 (extern)

### S6. Pipeline staut, Output kommt schubweise
- **Symptom:** Mehr Prozesse als CPU-Kerne → OS puffert Pipe-Zeilen, Output kommt blockweise.
- **Ursache:** OS-Scheduling/Pipe-Buffering.
- **Versionen:** alle (CLI-Pipelines).
- **FIX:** `stdbuf -oL` vor jedem Prozess (zeilenweises Flushing).
- **Quelle:** https://github.com/ufal/SimulStreaming (extern)

### S7. Falsche Wort-Timestamps im Live-Output
- **Symptom:** Start/End-Zeiten der Wörter ungenau.
- **Ursache:** SimulStreaming warnt: Live-Timestamps „may be inaccurate but good enough".
- **Versionen:** Streaming.
- **FIX:** Live-Timestamps nur für grobe Anzeige nutzen, nicht für harte Sync-Logik; finalen Pass für genaue Timestamps.
- **Quelle:** https://github.com/ufal/SimulStreaming (extern)

### S8. whisper_streaming VAC: Silero-VAD crasht „Provided number of samples … (Supported: 256/512)" ⭐ HAEUFIG
- **Symptom:** Mit `--vac` sofortiger `ValueError`/`torch.jit.Error` — gemeldet 641/720 statt exakt 512 Samples (16 kHz). Transkription bricht komplett ab.
- **Ursache:** Silero v4+ akzeptiert nur exakt 512-Sample-Frames (16 kHz); whisper_streaming reicht den VAC-Chunk (Default 0.04 s = 640 Samples) ungepuffert durch.
- **Versionen:** main (Silero ≥ v4); Fix in offenem PR #115.
- **FIX:** Ringpuffer vor dem Silero-Call, der den Strom in exakt-512-Sample-Frames zerlegt und Reste puffert (am Ende mit Nullen padden).
- **Quelle:** https://github.com/ufal/whisper_streaming/issues/142 (extern)

### S9. whisper_streaming: `vac_chunk_size` wird still von `min_chunk_size` überschrieben
- **Symptom:** Über `whisper_online_server.py` greift die VAC-Chunkgröße nie — ohne Warnung durch `min_chunk_size` ersetzt; falsches Endpointing ohne Fehlermeldung.
- **Ursache:** Falscher Parameter durchgereicht (`min_chunk_size` landet im Slot von `vac_chunk_size`).
- **Versionen:** main bis #159; Fix PR #160.
- **FIX:** `vac_chunk_size` separat durchreichen; bei direktem Import von `VACOnlineASRProcessor` funktioniert es.
- **Quelle:** https://github.com/ufal/whisper_streaming/issues/159 (extern)

### S10. whisper_streaming: mehrere Clients → Turn-Taking statt Parallelität, Latenz bis ~1 Min
- **Symptom:** 2 gleichzeitige WebSocket-Clients werden abwechselnd statt parallel verarbeitet; Latenz bis 1 Min.
- **Ursache:** Blockierende, CPU/GPU-serielle Inferenz + globaler `model_lock`; explizit ein Single-Stream-PoC.
- **Versionen:** main (Designgrenze).
- **FIX:** Getrennte Prozesse/GPU-Worker pro Stream (Prozess-Pool) + Load-Balancer; nicht über Python-Threads.
- **Quelle:** https://github.com/ufal/whisper_streaming/issues/138 (extern)

### S11. whisper_streaming: Browser-Audio (MediaRecorder/WebM-Opus) → faster-whisper `InvalidDataError`
- **Symptom:** `MediaRecorder`-Blobs (WebM/Opus) → beim Buffer-Leeren `av.error.InvalidDataError`.
- **Ursache:** Container-Header sitzt nur im ersten Chunk; nach dem Leeren fehlen Header/Frame-Grenzen. Format-Mismatch Browser-Opus vs. 16-kHz-Mono-PCM.
- **Versionen:** main (Integrationsfalle).
- **FIX:** Browser-Audio via Web Audio API/AudioWorklet als 16-kHz-Mono-Float32-PCM senden; Puffer am Frame-/Stille-Rand leeren, nicht mitten im Container.
- **Quelle:** https://github.com/ufal/whisper_streaming/issues/134 (extern)

### S12. whisper_streaming: VAC-Endpoint-Schwelle (0.5 s) hartkodiert
- **Symptom:** Endpoint feuert immer nach fest 0.5 s Stille; nicht einstellbar → je nach Sprechstil zu früh/spät.
- **Ursache:** Schwelle/Pausenlänge als Konstante im VAC-Pfad, nicht durchgeschleift.
- **Versionen:** main.
- **FIX:** Pausenlänge/Silence-Threshold als CLI-Parameter exponieren und in `VACOnlineASRProcessor` durchreichen.
- **Quelle:** https://github.com/ufal/whisper_streaming/issues/117 (extern)

### S13. SimulStreaming: `IndexError: string index out of range` (CJK) bei Langläufen
- **Symptom:** Bei mehrstündigen Sessions (v.a. CJK) Abbruch in `split_tokens_on_unicode` (tokenizer.py).
- **Ursache:** Unvollständiges Multibyte-Zeichen im Hypothesen-Tail; geerbter Whisper-Tokenizer-Bug (faster-whisper fixte ihn via PR #111, Original nicht).
- **Versionen:** main (offen #31).
- **FIX:** faster-whisper-Fix portieren — Index-Zugriff gegen `len()` absichern, unvollständige Trailing-Bytes überspringen.
- **Quelle:** https://github.com/ufal/SimulStreaming/issues/31 (extern)

### S14. SimulStreaming: LLM-Steuertokens (`<end_of_turn>`) lecken in die Ausgabe
- **Symptom:** Im LLM-Translate-Modus erscheinen Chat-Template-Tags im sichtbaren `text`/`is_final`.
- **Ursache:** Kein systematischer Strip von Special-/Template-Tokens nach dem LLM-Decoding.
- **Versionen:** main (offen #36).
- **FIX:** Generischer Post-Decode-Filter über `tokenizer.special_tokens_map`/`added_tokens` (modellagnostisch).
- **Quelle:** https://github.com/ufal/SimulStreaming/issues/36 (extern)

### S15. WhisperLiveKit + SimulStreaming-Backend: `tensor a (8) must match tensor b (4)` (large-v3)
- **Symptom:** `--backend simulstreaming --model large-v3`: jeder Schritt wirft Dimension-Mismatch (wächst pro Chunk), Warmup „Cannot set attribute 'src'"; Modell geladen, aber KEIN Text.
- **Ursache:** KV-Head-Mismatch zwischen Faster-Whisper-Encoder und SimulStreaming-Decoder (8 vs. 4); KV-Cache/AlignAtt-Frame-Index akkumuliert falsch.
- **Versionen:** WLK 0.22 (Regression nach 0.17), offen.
- **FIX:** Encoder/Decoder-Variante konsistent wählen; pragmatisch `--backend faster-whisper` bis SimulStreaming-large-v3 gepatcht.
- **Quelle:** https://github.com/QuentinFuxa/WhisperLiveKit/issues/152 (extern)

### S16. WhisperLiveKit: Diarization-Start crasht `ml_dtypes has no attribute 'float4_e2m1fn'`
- **Symptom:** `--diarization` bricht beim Start ab, Server startet nie.
- **Ursache:** Dependency-Skew NeMo→onnx→ml_dtypes; neuere onnx referenziert ein in der installierten ml_dtypes fehlendes Attribut.
- **Versionen:** WLK 0.2.8 mit Diarization; Fix über onnx-Release (onnx#7249).
- **FIX:** `ml_dtypes` anheben bzw. `onnx` pinnen; NeMo per Git-main installieren (#321).
- **Quelle:** https://github.com/QuentinFuxa/WhisperLiveKit/issues/213 (extern)

### S17. WhisperLiveKit: `language auto` → Stille nicht erkannt → falsches Endpointing
- **Symptom:** Mit fester Sprache wurde Stille korrekt erkannt, mit `--language auto` nicht (solange Sprache noch nicht detektiert) → falsches Trimming + English-Bias.
- **Ursache:** Silence-/Endpoint-Pfad hing am gesetzten Sprachcode.
- **Versionen:** in WLK-Releases gefixt (Changelog).
- **FIX:** Auf aktuelle WLK-Version updaten; für stabile Per-Chunk-Detektion Voxtral-Backend.
- **Quelle:** https://github.com/QuentinFuxa/WhisperLiveKit/releases (extern)

### S18. WhisperLive: Endpoint-Halluzination bei Rauschen knapp über VAD-Schwelle („Thanks for watching!")
- **Symptom:** Mit aktivem VAD halluziniert das Modell Abspann-Floskeln in Sprechpausen/Stille knapp über der VAD-Schwelle.
- **Ursache:** Decoder-Prior auf YouTube-Abspann-Phrasen; VAD lässt Rest-Rauschen durch, `no_speech_threshold` zu schwach.
- **Versionen:** main/0.7.x (offen).
- **FIX:** In Stillephasen Null-Audio statt Rauschen senden; `no_speech_threshold` erhöhen (server.py ~L714); bekannte Tokens via `--suppress_tokens`.
- **Quelle:** https://github.com/collabora/WhisperLive/issues/185 (extern)

### S19. WhisperLive: HLS-/RTSP-Eingang liefert keine Transkripte (Mikro/Datei funktionieren)
- **Symptom:** `client(hls_url=…)`/`client(rtsp_url=…)` bleibt auf „connecting", keine Transkripte — obwohl die GPU arbeitet; dieselben Streams laufen in Safari/VLC.
- **Ursache:** FFmpeg-Pull/Resampling im Stream-Reader defekt; dekodiertes Audio kommt nicht als 16-kHz-Mono-PCM an. Als Regression bestätigt.
- **Versionen:** 0.7.1 (Regression), offen.
- **FIX:** Stream-Reader auf korrektes FFmpeg-Demux+Resample prüfen; Workaround: externes FFmpeg zieht den Stream nach 16-kHz-PCM und speist ihn als Pipe-Input ein.
- **Quelle:** https://github.com/collabora/WhisperLive/issues/388 (extern)

---

## M) Modell / Speicher / Quantisierung

### M1. OOM bei large-v3
- **Symptom:** Crash beim Modell-Laden auf schwächerer Hardware.
- **Ursache:** large-v3 float32 ~10 GB RAM/VRAM.
- **Versionen:** alle.
- **FIX:** int8-Quantisierung (faster-whisper `compute_type="int8"` → ~2,5 GB) bzw. quantisiertes GGML (`ggml-large-v3-q5_0.bin`); oder auf `medium`/`small` ausweichen.
- **Quelle:** https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026 (extern)

### M2. int8 degradiert bei schwierigem Audio
- **Symptom:** Mehr Fehler bei Rauschen/Akzent mit int8 als mit fp16.
- **Ursache:** Aggressive Quantisierung degradiert bei schwierigem Audio stärker.
- **Versionen:** alle.
- **FIX:** `int8_float16` (Gewichte int8, Compute fp16) statt reinem int8; `q5_0` statt `q4`. Deutsch als Tier-1-Sprache ist bei sauberem Audio robust (< 0,5 Punkte Verlust).
- **Quelle:** https://opennmt.net/CTranslate2/quantization.html (offiziell)

### M3. `turbo` für Übersetzung schlecht
- **Symptom:** Schwache Ergebnisse bei Speech-Translation (de→en).
- **Ursache:** large-v3-turbo wurde ohne Translation-Daten nachtrainiert.
- **Versionen:** large-v3-turbo.
- **FIX:** Für reine Transkription (Franks Fall) ist turbo perfekt; für Übersetzung `large-v3` nehmen.
- **Quelle:** https://github.com/openai/whisper/discussions/2363 (extern)

### M4. faster-whisper auf Mac quälend langsam (kein Metal) ⭐ HAEUFIG
- **Symptom:** large-v3 läuft nur ~3× Echtzeit, Overlay ruckelt.
- **Ursache:** faster-whisper/CTranslate2 hat **kein Metal/ANE-Backend** → auf Apple Silicon CPU-only.
- **Versionen:** alle.
- **FIX:** Auf dem Mac whisper.cpp + Metal/Core ML nutzen (~10×). faster-whisper nur auf NVIDIA. Cross-Platform: Mac → whisper.cpp, Windows+NVIDIA → faster-whisper.
- **Quelle:** https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026 (extern)

### M5. primeline-German (CT2) bricht nach ~20 Wörtern stumm ab ⭐ HAEUFIG
- **Symptom:** Nach CT2-Konvertierung von `primeline/whisper-large-v3-german` stoppt die Ausgabe nach ~20 Wörtern; kein Fehler, tqdm friert. Auch fertig-konvertierte Forks; nur offizielles large-v3 läuft durch.
- **Ursache:** Konvertierung übernimmt fehlerhaft `config.max_length`/`begin_suppress_tokens`; Suppress-Liste unterdrückt EOT-nahe Tokens vorzeitig. Mel-Filter (n_mels=128) muss manuell gesetzt werden.
- **Versionen:** primeline-German CT2-Konvertate (offen, Workaround nötig).
- **FIX:** Vor der Konvertierung `del model.config.max_length` und `del model.config.begin_suppress_tokens`; `mel_filters` explizit auf n_mels=128; aktuelles ctranslate2+transformers.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/567 (extern)

### M6. primeline-German → CT2: `Non-consecutive added token '<|0.02|>'`-ValueError
- **Symptom:** `ct2-transformers-converter` bricht ab: `Non-consecutive added token '<|0.02|>' … should have index 50365 but has 50366`.
- **Ursache:** primeline-Tokenizer hat eine reihenfolgeverschobene Timestamp-Token-Tabelle, die der alte CT2-Vocab-Parser strikt konsekutiv erwartet.
- **Versionen:** alte ctranslate2/transformers; nach Upgrade behoben.
- **FIX:** ctranslate2 + transformers hochziehen; `TransformersConverter`-API statt CLI + `--copy_files` für die Tokenizer-JSONs.
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/567 (extern)

### M7. distil-whisper-large-v3-german: `return_timestamps=True` wirft ValueError
- **Symptom:** Mit `return_timestamps=True` ValueError, dass die GenerationConfig nicht für Timestamps konfiguriert ist (keine SRT).
- **Ursache:** Beim Distill-Export wurde `generation_config.json` ohne `alignment_heads`/`no_timestamps_token_id` gespeichert.
- **Versionen:** primeline/distil-whisper-large-v3-german (offen, Config-Patch).
- **FIX:** `alignment_heads` + Timestamp-Token-IDs aus dem Basismodell in die `generation_config` übernehmen.
- **Quelle:** https://huggingface.co/primeline/distil-whisper-large-v3-german (extern)

### M8. large-v3-turbo (transformers): falsche Word-Timestamps + Wort-Wiederholungen → gefixt
- **Symptom:** Timestamps oft falsch; das Modell wiederholt Wörter; `repetition_penalty=1.2` mildert nur.
- **Ursache:** Bug in der transformers-Timestamp-Logik für turbo (4 Decoder-Layer → instabilere Timestamp-Token).
- **Versionen:** transformers ≤ 4.38.3 betroffen; **gefixt** in main (PR #35750/#36632).
- **FIX:** transformers aus main installieren; `repetition_penalty` 1.1–1.2.
- **Quelle:** https://github.com/huggingface/transformers/issues/37248 (extern)

### M9. NeMo Canary/Parakeet für Deutsch — Stärken UND Schwächen
- **Symptom/Befund:** Canary-1B-v2 schlägt large-v3 auf MCV-DE (~5,77 % WER avg) und halluziniert weniger; ABER NeMo läuft praktisch nur auf NVIDIA-GPU (kein CPU/CoreML, kein whisper.cpp-Ökosystem), Canary deckt nur EN/ES/DE/FR ab.
- **Ursache:** NeMo ist CUDA-zentriert und enger trainiert; Trade-off Genauigkeit-pro-Sprache vs. Breite/Portabilität.
- **Versionen:** aktuell.
- **FIX/Empfehlung:** Für reines Deutsch mit NVIDIA-GPU Canary-1B-v2 prüfen; wenn CPU/Apple-Silicon/Offline-Single-Binary nötig → bei Whisper (large-v2 oder primeline-German) bleiben.
- **Quelle:** https://arxiv.org/html/2509.14128v1 (extern)

---

## C) Cloud / Hybrid

### C1. Cloud-Latenz-Spikes bei schwachem Netz ⭐ HAEUFIG
- **Symptom:** Overlay friert ein, Transkript kommt Sekunden zu spät oder gar nicht.
- **Ursache:** Netz-Roundtrip dominiert — Groq-Compute ist trivial schnell, aber Upload bei schwachem Netz killt die Latenz.
- **Versionen:** Cloud-Pfad.
- **FIX:** Lokal als Default; Cloud-Call mit kurzem Timeout (~3 s), bei Überschreitung automatisch auf lokal zurückfallen; vorab schneller Connectivity-Check.
- **Quelle:** https://groq.com/pricing · https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026 (extern)

### C2. Netzausfall = Cloud-STT komplett tot (Schweden-Wildnis)
- **Symptom:** Im Funkloch transkribiert das Overlay nichts.
- **Ursache:** Reine Cloud-Architektur hat keinen Offline-Pfad.
- **Versionen:** Cloud-only.
- **FIX:** Lokales Modell IMMER mitinstallieren und als Default; Cloud nur optionaler Boost. Genau Franks „Schweden"-Szenario — lokal ist Pflicht.
- **Quelle:** https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026 (extern)

### C3. Groq 10-Sekunden-Mindestabrechnung
- **Symptom:** Cloud-Rechnung explodiert bei kurzen Overlay-Schnipseln.
- **Ursache:** Groq berechnet min. 10 s Audio pro Request — ein 2-s-Befehl kostet wie 10 s.
- **Versionen:** Groq STT.
- **FIX:** Kurze Äußerungen lokal verarbeiten; nur längere/gebündelte Clips an die Cloud; nie pro Wort einen Cloud-Call.
- **Quelle:** https://console.groq.com/docs/speech-to-text (offiziell)

### C4. Data-Retention / DSGVO — Audio in US-Buckets
- **Symptom:** Audio landet (potenziell) in US-GCP-Buckets.
- **Ursache:** Groq loggt per Default nichts, behält sich aber bis 30 Tage Logging bei System-Reliability/Abuse vor; Daten in US-GCP, SCC-Transfer.
- **Versionen:** Groq Cloud.
- **FIX:** **ZDR in Groq Data-Controls aktivieren**; Cloud nur nach explizitem Opt-in (Default „nur lokal"); bei harter EU-Pflicht Deepgram EU-Endpoint statt Groq.
- **Quelle:** https://console.groq.com/docs/your-data (offiziell)

### C5. Halluzinationen auch in der Cloud (no_speech_prob nicht ausgewertet)
- **Symptom:** Auch Groq/Cloud liefert bei Stille erfundene Floskeln.
- **Ursache:** Whisper-Architektur halluziniert auf Nicht-Sprache — auch in der Cloud.
- **Versionen:** alle Cloud-Whisper.
- **FIX:** `verbose_json` anfordern und das `no_speech_prob`-Feld pro Segment auswerten, Segmente über Schwelle verwerfen; `language="de"` mitsenden.
- **Quelle:** https://console.groq.com/docs/speech-to-text (offiziell)

### C6. Lokale Streaming-Vorschau + Cloud-Final desynchron
- **Symptom:** Live-Vorschau (lokal) und Cloud-Endfassung weichen ab, Text „springt" beim Festschreiben.
- **Ursache:** Zwei verschiedene Modelle (kleines lokales Live-Modell vs. Cloud large-v3) liefern leicht andere Wortwahl.
- **Versionen:** Hybrid-Setup.
- **FIX:** Vorschau als „vorläufig" markieren (grau); beim Cloud-Final den ganzen Text ersetzen, nicht inkrementell mergen; oder lokal `large-v3-turbo` als Final, Cloud nur bei „höchste Genauigkeit".
- **Quelle:** https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026 (extern)

---

## A) Audio-Pipeline (Mikrofon → 16 kHz mono PCM → Whisper)

> Gemeinsamer Nenner der gravierendsten Bugs: Whisper macht beim direkten Array-/Stream-Pfad **keine** der
> drei Schutz-Konvertierungen automatisch (Resampling, /32768-Normalisierung, Downmix) — die laufen NUR im
> Datei-Pfad (`load_audio` → ffmpeg). Reihenfolge in jeder Roh-PCM-Pipeline: De-interleave/Downmix → Resample
> 16000 Hz → Int16→Float32 `/32768` → [-1,1].

### A1. Sample-Rate nicht auf 16 kHz resampelt → „Chipmunk"/Tonhöhe verschoben → Müll ⭐ HAEUFIG
- **Symptom:** 44,1/48-kHz-Audio roh an Whisper gegeben (Header gefälscht oder ignoriert) → Sprache klingt zu schnell/hochtönig → Transkript Müll/leer.
- **Ursache:** Whisper/whisper.cpp/faster-whisper nehmen beim Roh-Array-Pfad **immer** 16 kHz an — keine interne Rate-Erkennung. Nur der Datei-Pfad resampelt.
- **Versionen:** alle Plattformen/Engines.
- **FIX:** Vor Whisper IMMER auf exakt 16000 Hz resampeln (`torchaudio`/`librosa`/`scipy.resample_poly`/ffmpeg `-ar 16000`); Rate nie nur im Header „umbenennen"; tatsächliche Eingangsrate aus dem Device-Format lesen.
- **Quelle:** https://github.com/openai/whisper/discussions/870 (extern)

### A2. „16 kHz" mit „16 bit" verwechselt
- **Symptom:** Entwickler liefern 16-bit-Audio bei beliebiger Rate → Müll, weil die Anforderung als „16 bit" missverstanden wird.
- **Ursache:** Historisch missverständliche whisper.cpp-Fehlermeldung; **Rate 16000 Hz** ≠ **Tiefe 16-bit**.
- **Versionen:** whisper.cpp.
- **FIX:** Beides getrennt sicherstellen: Rate = 16000 Hz UND PCM 16-bit.
- **Quelle:** https://github.com/ggml-org/whisper.cpp/issues/909 (extern)

### A3. Int16→Float32 ohne `/32768`-Normalisierung → Gibberish/Dauerhalluzination ⭐ HAEUFIG
- **Symptom:** Roh-Int16 als Float32 übergeben, aber nicht durch 32768 geteilt → Werte −32768…+32767 statt −1…+1 → Mel-Spektrogramm sprengt den Trainingsbereich → Gibberish/Dauerhalluzination. Datei-Pfad geht, Array-Pfad nicht.
- **Ursache:** Whisper ist nicht skaleninvariant; `load_audio()` skaliert implizit `/32768`, der manuelle numpy-Pfad nicht.
- **Versionen:** faster-whisper/openai-whisper bei Array-Übergabe.
- **FIX:** `audio = np.frombuffer(raw, np.int16).astype(np.float32) / 32768.0` (Faktor exakt 1/32768, nicht 32767); Werte in [-1,1).
- **Quelle:** https://github.com/SYSTRAN/faster-whisper/issues/1323 · https://github.com/openai/whisper/discussions/428 (extern)

### A4. Stereo falsch zu Mono (nur ein Kanal / interleaved roh)
- **Symptom:** Stereo→Mono falsch: nur Kanal L (halbe Energie, leise) oder interleaved-Bytes als Mono (halbe Dauer + Müll).
- **Ursache:** Whisper downmixt bei vorgeformtem Array nicht automatisch (nur Datei-Pfad).
- **Versionen:** alle.
- **FIX:** `mono = stereo.reshape(-1,2).mean(axis=1)` (beide Kanäle mitteln) oder ffmpeg `-ac 1`.
- **Quelle:** https://github.com/openai/whisper/discussions/1263 (extern)

### A5. NAudio `WasapiLoopbackCapture` liefert Float32/Stereo/48 kHz → als 16-bit gelesen = Rauschen
- **Symptom:** Loopback-Buffer byteweise als 16-bit-PCM geschrieben → reines Rauschen; „nur halb so viele Samples".
- **Ursache:** `WasapiLoopbackCapture` erzwingt das Geräteformat (32-bit IEEE-Float, Stereo, 44,1/48 kHz); WaveFormat ist vor dem Capture nicht setzbar. 4-Byte-Float als 2-Byte-Int gelesen = Garbage.
- **Versionen:** NAudio (Windows).
- **FIX:** Format aus `capture.WaveFormat` lesen; Pipeline 32-bit-Float → Stereo→Mono → `WdlResampler` auf 16 kHz → Int16; `BytesRecorded/4` = Float-Sample-Anzahl.
- **Quelle:** https://github.com/naudio/NAudio/issues/900 (extern)

### A6. NAudio Bytes→Float falsch (Endianness/Stride)
- **Symptom:** Float-Samples aus dem Byte-Buffer falsch rekonstruiert → verzerrt/leise/Garbage.
- **Ursache:** Falscher 4-Byte-Stride/Offset bei `BitConverter.ToSingle`, oft mit Stereo-Offset-Fehler.
- **Versionen:** NAudio (Windows).
- **FIX:** `BitConverter.ToSingle(buffer, i*4)` mit korrektem Stride; bei Stereo L+R mitteln; NAudio ist Little-Endian (keine manuelle Drehung).
- **Quelle:** https://github.com/naudio/NAudio/issues/1121 (extern)

### A7. NAudio `WaveIn` USB-Disconnect: `DataAvailable` stoppt still / `StopRecording` blockiert ewig
- **Symptom:** USB-Mikro abgezogen → keine Daten mehr, keine Exception → Whisper „hört nichts"; `StopRecording()`/`DeviceCount` blockieren für immer, App hängt.
- **Ursache:** Keine robuste Disconnect-Behandlung; WinMM-Backend blockiert beim Disposen eines weggefallenen Geräts.
- **Versionen:** NAudio WaveIn/WaveInEvent (Windows).
- **FIX:** `RecordingStopped`-Event + `e.Exception` prüfen; Watchdog (Timeout ohne Daten → Reinit); Hotplug via `MMDeviceEnumerator`; `StopRecording` mit Timeout in eigenem Thread.
- **Quelle:** https://github.com/naudio/NAudio/issues/657 (extern)

### A8. macOS AVAudioEngine: AirPods/HFP `installTap`-Format-Crash ⭐ HAEUFIG
- **Symptom:** App crasht beim Mikro-Start mit AirPods/Bluetooth: `required condition is false: format.sampleRate == hwFormat.sampleRate` / „Invalid input sample rate".
- **Ursache:** Bluetooth-HFP wechselt die Input-Rate (AirPods melden teils 24000, HFP fällt auf 16000/8000); ein hart kodiertes `AVAudioFormat` im `installTap` ≠ HW-Rate → Assertion. Breiter als reiner Geräte-Wechsel — crasht schon beim **initialen** Tap.
- **Versionen:** macOS/iOS, alle BT-HFP-Headsets.
- **FIX:** Tap immer mit `inputNode.inputFormat(forBus:0)` (echtes HW-Format), nie Wunschformat; im Callback per `AVAudioConverter` auf 16 kHz mono Float32; auf `AVAudioEngineConfigurationChange` Engine+Tap neu aufsetzen.
- **Quelle:** https://developer.apple.com/forums/thread/705706 (Apple, akzeptierte Antwort)

### A9. ffmpeg: Audio zu leise → VAD/Whisper versagt; naive Normalisierung clippt
- **Symptom:** Korrektes 16 kHz mono, aber sehr leise → VAD schneidet alles als Stille; oder aggressive Peak-Normalisierung clippt → Verzerrung.
- **Ursache:** Reines `-ar 16000 -ac 1` ohne Pegelanpassung; Pegel unter VAD-Threshold.
- **Versionen:** ffmpeg-Vorverarbeitung.
- **FIX:** Dynamik-Normalisierung statt Roh-Boost: `-af "dynaudnorm=f=75:g=25,highpass=f=80,lowpass=f=8000"` bzw. `loudnorm` (EBU R128); VAD-Threshold senken bei systematisch leisem Eingang.
- **Quelle:** https://ayosec.github.io/ffmpeg-filters-docs/8.0/Filters/Audio/whisper.html (extern)

### A10. PortAudio/sounddevice Input-Overflow → still verworfene Samples
- **Symptom:** Bei Echtzeit-Capture verschwinden Samples (Lücken/Aussetzer), v.a. wenn Inferenz im selben Thread läuft; das `input_overflow`-Flag wird ignoriert.
- **Ursache:** Zu langsamer Stream-Callback (Inferenz/I-O/Locks im Callback) → PortAudio verwirft Samples vor dem Buffer.
- **Versionen:** python-sounddevice/PortAudio.
- **FIX:** Im Callback NUR in einen lock-freien Ringpuffer schreiben (keine Inferenz/I-O/Logging); `status`-Flag prüfen; `blocksize`/`latency` erhöhen; Capture- strikt von Inferenz-Thread trennen.
- **Quelle:** https://python-sounddevice.readthedocs.io/en/0.4.7/api/misc.html (extern)

### A11. AGC zu niedrig / Clipping + Echo-Rückkopplung (Always-listening)
- **Symptom:** „Always-listening" hört nichts (AGC regelt Gain herunter) oder clippt; eigener Lautsprecher/TTS wird mitaufgenommen → Selbst-Trigger-Schleife.
- **Ursache:** OS-AGC regelt zur falschen Zeit; keine Echo-Cancellation/Half-Duplex-Gate.
- **Versionen:** Windows + macOS, Always-listening.
- **FIX:** OS-AGC für die Capture-Sitzung deaktivieren, eigene sanfte RMS-Normalisierung; AEC (WebRTC APM / Windows Voice-Capture-DMO) oder Mikro während eigener Ausgabe stummschalten (Half-Duplex).
- **Quelle:** https://www.zegocloud.com/blog/automatic-gain-control (extern)

---

## N) Native-Bindings (Whisper.net / WhisperKit)

### N1. Whisper.net: `WhisperFactoryOptions.GpuDevice` ignoriert → läuft immer auf erster GPU
- **Symptom:** `GpuDevice = 2` hat keinen Effekt; Whisper startet immer auf GPU 0.
- **Ursache:** whisper.cpp ignorierte `gpu_device` beim Backend-Init (whisper.cpp #2668); Index ist zudem **0-basiert**.
- **Versionen:** ≤1.7.5-preview1; **gefixt ab 1.8.0** (whisper.cpp PR #2728).
- **FIX:** Auf Whisper.net 1.8.0+; `GpuDevice` 0-basiert; alt: nur per `CUDA_VISIBLE_DEVICES`.
- **Quelle:** https://github.com/sandrohanea/whisper.net/issues/364 (extern)

### N2. Whisper.net: CUDA-Runtime lädt nicht (braucht CUDA 12) → kein Fallback → Crash
- **Symptom:** Im `nvidia/cuda:11.8`-Image wird die GPU erkannt, aber `libggml-whisper.so`/native lib lädt nicht → `Failed to load native whisper library`, kein CPU-Fallback.
- **Ursache:** Whisper.net-CUDA-Runtime braucht **CUDA 12** (cudart v12); manuelles Umbenennen von cudart 11 hilft nicht (ABI).
- **Versionen:** bis v1.7.x; 1.9 hebt CUDA-Tooling auf 13.0.1, CUDA-12-Mindestanforderung bleibt.
- **FIX:** CUDA-12-Treiber inkl. cudart; zusätzlich `Whisper.net.Runtime`/`AllRuntimes` für echten Fallback; Debug-Logging zeigt, welche Runtime warum (nicht) lädt.
- **Quelle:** https://github.com/sandrohanea/whisper.net/issues/254 (extern)

### N3. Whisper.net: „Native Library not found" nach `dotnet publish` (run läuft) ⭐ HAEUFIG
- **Symptom:** Lokal grün, im publishten Build `FileNotFoundException: Native Library not found in default paths`.
- **Ursache:** `/runtimes`-Verzeichnis nicht ins Output kopiert (single-file/Trimming/`Private`-Flag) oder Server ohne AVX bei nur `Whisper.net.Runtime`.
- **Versionen:** generisches Publish, v1.9.
- **FIX:** `Whisper.net.AllRuntimes` (oder `+ NoAvx`); `/runtimes/...`-Layout muss neben der .exe liegen; bei single-file native DLLs als Content/`CopyToOutputDirectory`, Trimming für Native-Assets ausschließen.
- **Quelle:** https://github.com/sandrohanea/whisper.net/issues/233 (extern)

### N4. Whisper.net: Loader nutzt leere Assembly-Location in Embedded-Hosts (Unity/Godot)
- **Symptom:** `ArgumentException: Invalid path` / `DllNotFoundException` obwohl Datei existiert; Loader sucht im Runtime-Basisverzeichnis statt App-Ordner.
- **Ursache:** `NativeLibraryLoader` leitet den Pfad aus `Assembly.Location`/`GetCommandLineArgs` ab — in Plugin-/Engine-Hosts leer.
- **Versionen:** v1.7.1+ in Embedded-Hosts (native WPF/AppKit nicht betroffen).
- **FIX:** Native Libs vorab per `dlopen(RTLD_NOW|RTLD_GLOBAL)` laden, dann `RuntimeOptions.Instance.SetLoadedLibrary(...)` + `SetBypassLoading(true)` vor `FromPath`.
- **Quelle:** https://github.com/sandrohanea/whisper.net/issues/243 (extern)

### N5. Whisper.net: `CorruptedWaveException` — Parser akzeptiert nur 16-bit-PCM ⭐ HAEUFIG
- **Symptom:** `Unsupported wave file` beim `ProcessAsync`; Auslöser z.B. 24 kHz/32-bit-Float-WAV.
- **Ursache:** Eingebauter `WaveParser` unterstützt nur PCM. Mikrofon-/Loopback-Capture (WASAPI, AVAudioEngine) liefert oft 32-bit-Float → Crash.
- **Versionen:** ≤1.7.1 enger; 1.7.2-preview2 toleranter; nicht-PCM weiter nicht unterstützt.
- **FIX:** Vor `ProcessAsync` nach 16-bit-PCM/16 kHz/mono konvertieren (NAudio `WaveFormatConversionStream`/`MediaFoundationResampler`).
- **Quelle:** https://github.com/sandrohanea/whisper.net/issues/243 (extern)

### N6. Whisper.net: Concurrent-Collection-Crash bei paralleler Nutzung → gefixt 1.9
- **Symptom:** Sporadischer Crash bei gleichzeitiger/wiederholter Verarbeitung über denselben Pfad.
- **Ursache:** Race Condition auf einer internen Collection im `WhisperProcessor`; Factory/Processor nicht beliebig thread-safe.
- **Versionen:** <1.9; **gefixt in 1.9** (auch Cancellation/`WhisperProcessingException`).
- **FIX:** Auf 1.9; 1× `WhisperFactory` (Dispose am App-Ende), pro Job frischer Processor, Zugriffe serialisieren/pro Thread eigene Factory.
- **Quelle:** https://github.com/sandrohanea/whisper.net/releases (extern)

### N7. Whisper.net: Native-Resource-Lifetime (CoreML-Loading, OpenVINO-Pfad-String) → 1.9
- **Symptom:** Schwer reproduzierbare native Fehler bei wiederholter Nutzung; OpenVINO-Pfad-String konnte vorzeitig freigegeben werden (Use-after-free).
- **Ursache:** Lebenszeit-Probleme bei an die native Schicht übergebenen Strings/Handles (GC sammelt Pfad-String ein).
- **Versionen:** <1.9; adressiert in 1.9 (+ Metal-Runtime-NuGet, statische iOS-Libs).
- **FIX:** Auf 1.9 updaten.
- **Quelle:** https://github.com/sandrohanea/whisper.net/releases (extern)

### N8. WhisperKit: AVAudioEngine `installTap`-Crash — ungültiges Format ⭐ HAEUFIG
- **Symptom:** Harter Crash in `AudioProcessor.setupEngine()`: `IsFormatSampleRateAndChannelCountValid(format)` false (in der Praxis hunderte Crash-Events).
- **Ursache:** An `installTap` übergebenes `AVAudioFormat` mit Sample-Rate 0 / 0 Kanälen (Input-Node beim Start noch ohne gültige HW-Config, oder Routing-/Session-Wechsel).
- **Versionen:** WhisperKit Realtime-Pfad.
- **FIX:** Format nicht hart vorgeben — `inputNode.outputFormat(forBus:0)` lesen + validieren (`sampleRate>0 && channelCount>0`); bei `routeChangeNotification` Engine stoppen, Tap mit aktuellem Format neu installieren. (Vgl. A8.)
- **Quelle:** https://github.com/argmaxinc/WhisperKit/issues/261 (extern)

### N9. WhisperKit: `manifest.plist`-Crash beim Compute-Wechsel/Startup
- **Symptom:** Assertion-Crash `failed assertion 'The file "manifest.plist" couldn't be opened…'` beim Start oder nach Compute-Options-Wechsel.
- **Ursache:** MPSGraph/CoreML findet den GPU-kompilierten Modell-Cache nicht (unvollständiger Download / halber Kompilier-Cache).
- **Versionen:** mehrere v0.x; Mechanik in v1.0 gleich.
- **FIX:** Modell aus `Documents/models` löschen + frisch laden; `.mlmodelc`-Integrität prüfen, Compile-Cache nicht zwischen App-Versionen wiederverwenden.
- **Quelle:** https://github.com/argmaxinc/WhisperKit/issues/264 · https://github.com/argmaxinc/argmax-oss-swift/issues/301 (extern)

### N10. WhisperKit: `ANECF error: failed to load ANE model` → lädt nicht / extrem langsam
- **Symptom:** Hängt beim Laden des Audio-Encoders; `ANECF error`/`Program load failure (0x20004)`; mit large-v3-turbo auf älteren A-Chips.
- **Ursache:** ANE kann das (große/Turbo-)Modell nicht kompilieren/laden (Daemon nicht erreichbar / Modell zu groß); Erst-Kompilierung sehr langsam.
- **Versionen:** v0.x mit large-v3-turbo auf älteren Chips; v1.0 gleiche Mechanik.
- **FIX:** Compute-Units gezielt setzen (`audioEncoderCompute: .cpuAndGPU`, `textDecoderCompute: .cpuAndNeuralEngine`); kleineres/quantisiertes Modell; `prewarmModels()` einmalig mit Fortschrittsanzeige.
- **Quelle:** https://github.com/argmaxinc/WhisperKit/issues/268 (extern)

### N11. WhisperKit: Memory-Leak bei `.cpuAndGPU` + Turbo auf Basis-M1 (wiederholtes Instanziieren)
- **Symptom:** Speicher wächst monoton beim wiederholten Zerstören/Neuerzeugen (Turbo, macOS 14.6, M1).
- **Ursache:** Turbo auf Basis-M1 „nicht unterstützt"; `.cpuAndGPU`-Workaround leakt CoreML-/Metal-Ressourcen beim Teardown.
- **Versionen:** WhisperKit/argmax-oss-swift, Turbo auf Basis-M1.
- **FIX:** EINE Instanz über die App-Lebensdauer wiederverwenden statt pro Job neu; auf Basis-M1 Turbo meiden bzw. unterstütztes Modell.
- **Quelle:** https://github.com/argmaxinc/WhisperKit/issues/265 (extern)

### N12. WhisperKit: selbst-konvertierte/heruntergeladene Modelle `.mlmodelc` nicht kompiliert
- **Symptom:** `…coremldata.bin is not a valid .mlmodelc`, `Failed to read model package`, oder Tensor-Alignment-Fehler.
- **Ursache:** Direkt aus dem HF-Repo bezogene Dateien sind teils nicht kompiliert / unvollständig / geräte-inkompatibel.
- **Versionen:** v0.x; v1.0 gleiches Compile-Prinzip.
- **FIX:** Modelle über WhisperKits eigenen Download-Mechanismus beziehen; falls manuell: `MLModel.compileModel(at:)`; Repo/Revision passend zur Version.
- **Quelle:** https://github.com/argmaxinc/WhisperKit/issues/339 (extern)

### N13. WhisperKit: deutlich langsamer als whisper.cpp bei falscher Compute-Konfiguration
- **Symptom:** Auf M1 Pro whisper.cpp (large-v2, GPU) ~29 s vs. WhisperKit `.cpuAndGPU` für alle Stufen ~1:50 min.
- **Ursache:** Pauschal `.cpuAndGPU` ist suboptimal (CPU-Fallback); Modell-Ladezeit fließt ohne Prewarm mit ein.
- **Versionen:** WhisperKit/argmax-oss-swift.
- **FIX:** Compute-Units gezielt (`audioEncoderCompute: .cpuAndNeuralEngine`, `textDecoderCompute: .cpuAndGPU`) + quantisiertes Modell; `prewarmModels()` vor der Messung; `logLevel: .debug` für Timing.
- **Quelle:** https://github.com/argmaxinc/argmax-oss-swift/issues/301 (extern)

### N14. WhisperKit → argmax-oss-swift v1.0: Breaking Changes (Swift 6 Concurrency, Paket-Umbenennung)
- **Symptom:** Build bricht nach Update: entfernte deprecated-APIs, geänderte Signaturen, neuer Paketname; Swift-6-Strict-Concurrency wirft Sendable-/Data-Race-Fehler.
- **Ursache:** v1.0 ist die Graduierung von „WhisperKit" zum „Argmax Open-Source SDK" (`argmax-oss-swift`): voller Swift-6-Concurrency-Support, vendored swift-transformers, neues Dynamic-Library-Produkt — ausdrücklich breaking.
- **Versionen:** WhisperKit ≤0.x → argmax-oss-swift 1.0 (Mai 2026).
- **FIX:** Dependency auf `argmax-oss-swift` umstellen, Importe anpassen; Overlay-Callbacks `@MainActor`/`Sendable`-konform; ggf. vendored `ArgmaxCore`-Typen statt HF-`Hub`.
- **Quelle:** https://github.com/argmaxinc/argmax-oss-swift/releases (extern)

---

## ✅ Fix-Status (was ist in neueren Versionen schon behoben?)

> Belege aus GitHub-Issues/PRs/Release-Notes der jeweiligen Repos. Ehrlichkeit: streng getrennt nach
> *belegt gefixt* vs. *Workaround bleibt aktiv*.

**Belegt gefixt (Versions-Anker):**

| Früherer Bug | Gefixt ab | Beleg |
|--------------|-----------|-------|
| E13 whisper.cpp VAD-Overlap-Timestamp-Drift | whisper.cpp **v1.8.4** | PR #3711 |
| E14 whisper.cpp VAD-Buffer-Overflow | whisper.cpp **v1.8.3** | PR #3558 |
| E29 faster-whisper VAD-leer → `max()`-ValueError | faster-whisper (nach 1.0.3, in 1.2.x verifizieren) | #1208 |
| H13 `initial_prompt` verpufft → `carry_initial_prompt` | openai-whisper **PR #2343** | #1477 |
| M8 large-v3-turbo Timestamp/Repetition (transformers) | transformers **main** (PR #35750/#36632) | #37248 |
| N1 Whisper.net `GpuDevice` ignoriert | Whisper.net **1.8.0** (whisper.cpp PR #2728) | #364 |
| N6 Whisper.net Concurrent-Collection-Crash | Whisper.net **1.9** | Releases |
| N7 Whisper.net Native-Resource-Lifetime | Whisper.net **1.9** | Releases |
| (Kontext) Server-`no_context` Default gegen Repetition | whisper.cpp **v1.8.3** | PR #3482 |
| (Kontext) Silero-VAD-Modell-Bump v6.2.0 | whisper.cpp **v1.8.3** | PR #3524 |

**Noch NICHT gefixt — Workaround bleibt aktiv:**

- **Architektonisch / modell-inhärent (kein Code-Fix):** A1–A4 (Whisper macht keine Auto-Konvertierung im Array-Pfad), H1/H2 (Stille-/ZDF-Halluzination), H10 (large-v3-Regression → large-v2), H14/H15/H17 (ITN/Code-Switching/Dialekt), S-Latenz-Floor.
- **Offen im Issue-Tracker:** E12/#3634 (VAD-Lücken), E15/#3207, E16/#3299, E17/#3358 (Server-Leak), E18/#3030, E21/#1333, E22/CT2#2063, E23–E28 (faster-whisper Batched/Threads/hotwords/multilingual), H11/#621, M5–M7 (primeline/distil-Konvertierung), S8–S19 (Streaming-Repos), N2–N5/N8–N14.
- **By design / Erwartung:** `min_silence_duration_ms` kein Segment-Splitter (faster-whisper #1108); faster-whisper kein Metal auf Mac (M4).
- **Dependency-Disziplin statt Code-Fix:** E1 (cuDNN/CUDA12), S16 (ml_dtypes/onnx).

> Methodik: Fix-Versionen aus den jeweiligen Release-Notes/PRs verifiziert. Wo nur ein Issue-Snippet vorlag,
> bleibt der Status bewusst „offen".

---

## 📋 Pflicht-Checkliste (vor lokaler-STT-Arbeit abhaken)

- [ ] Audio-Pipeline: De-interleave/Downmix → **Resample 16000 Hz** → Int16→Float32 **`/32768`** → [-1,1] (A1/A3/A4)
- [ ] macOS-Capture: Tap mit `inputFormat(forBus:0)`, nie Wunschformat; auf `ConfigurationChange` neu aufsetzen (A8/N8)
- [ ] Windows-Capture: WASAPI-Float32/Stereo/48 kHz korrekt lesen + konvertieren; USB-Disconnect-Watchdog (A5/A7)
- [ ] Engine: Mac whisper.cpp+Metal, Windows faster-whisper int8 / Whisper.net; **`language="de"` immer fest** (M4/H5)
- [ ] **VAD vorschalten** (Silero v6.2 eingebaut) + `condition_on_previous_text=False` + `no_speech_threshold=0.6` (H1/H2/H11)
- [ ] Deutsch: `initial_prompt` mit Beispielsatz (Großschreibung/Punkt) + `carry_initial_prompt`; bei viel Stille large-v2 statt large-v3 (H12/H13/H10)
- [ ] whisper.cpp ≥ **v1.8.4** (VAD-Overlap/Buffer-Fixes, Server-`no_context`, Silero v6.2) (E13/E14)
- [ ] faster-whisper: kein `ThreadPoolExecutor` → `ProcessPoolExecutor`; `cpu_threads=1` (CT2-Oversubscription); Batched-VAD-Fallen (E21/E22/E23)
- [ ] Modell-Konvertierung primeline-german: `max_length`/`begin_suppress_tokens` löschen + n_mels=128 (M5/M6)
- [ ] Whisper.net: **`AllRuntimes`** + `/runtimes` ins Publish-Output; CUDA 12; vor `ProcessAsync` 16-bit-PCM (N2/N3/N5); auf 1.9 (N6/N7)
- [ ] WhisperKit: eine Instanz wiederverwenden (Leak), Compute-Units gezielt + `prewarmModels()`, Modelle über eigenen Downloader (N10/N11/N12/N13)
- [ ] Streaming: VAC-Audio in exakt-512-Sample-Frames; Browser-Audio als 16-kHz-PCM (nicht WebM-Blob); State pro Äußerung resetten (S8/S11/H9)
- [ ] Always-listening: OS-AGC aus + AEC/Half-Duplex gegen Selbst-Trigger (A11)
- [ ] Cloud nur Opt-in (Default lokal) mit Timeout-Fallback; `no_speech_prob` auch in der Cloud auswerten (C1/C5)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-whisper-stt-lokal.md`) |
|---------------|------------------------------------------------------------------|
| E1–E29 (Engine/Setup/Build) | §2 (Engine-Wahl), §4 (GPU/Quantisierung) |
| H1–H17 (Halluzinationen & Genauigkeit) | §5 (VAD & Anti-Halluzination), §3 (Modell/Deutsch) |
| S1–S19 (Streaming/Echtzeit) | §6 (Streaming/Echtzeit) |
| M1–M9 (Modell/Speicher/Quantisierung) | §3 (Modellwahl), §4 (Quantisierung) |
| C1–C6 (Cloud/Hybrid) | §7 (Lokal vs. Groq-Cloud) |
| A1–A11 (Audio-Pipeline) | §2 (Engine-Wahl), §5 (VAD) |
| N1–N14 (Native-Bindings Whisper.net/WhisperKit) | §2 (Engine-Wahl: Whisper.net/WhisperKit) |
