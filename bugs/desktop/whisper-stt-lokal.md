# Bekannte Bugs/Fallen: On-Device-Whisper / lokale Transkription

> PFLICHT-LESEN vor Arbeit an lokaler Voice-zu-Text in Franks Overlays (Windows + macOS).
> Stand: recherchiert am 2026-06-14 (7 Researcher parallel, offizielle Repos zuerst).
> Versions-Anker: whisper.cpp **v1.8.6**, faster-whisper **1.2.1** (CTranslate2 4.8 / CUDA 12 + cuDNN 9),
> Silero VAD **v6.2**, Modelle large-v3-turbo + primeline-german, Whisper.net **1.9**, WhisperKit v1.0.
> Diese Fallen sind beim **Best-Practices-Lauf** mitgefunden worden; eine noch tiefere, dedizierte
> Bug-Recherche (Issue-Tracker-Fokus + Fix-Status) kann später per `bug-almanach-recherche` ergänzt werden.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/desktop/best-practices-whisper-stt-lokal.md`](../../best-practices/projekt-code/desktop/best-practices-whisper-stt-lokal.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Sektionen: **E** Engine/Setup/Build · **H** Halluzinationen & Genauigkeit · **S** Streaming/Echtzeit ·
> **M** Modell/Speicher/Quantisierung · **C** Cloud/Hybrid.

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
- **FIX:** `Whisper.net.AllRuntimes` referenzieren (Auto-Priorität Cuda→Cuda12→Vulkan→CoreML→CPU); für CPUs ohne AVX `Whisper.net.Runtime.NoAvx`; MSVC-Redist mitliefern.
- **Quelle:** https://github.com/sandrohanea/whisper.net (extern)

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

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-whisper-stt-lokal.md`) |
|---------------|------------------------------------------------------------------|
| E1–E11 (Engine/Setup/Build) | §2 (Engine-Wahl), §4 (GPU/Quantisierung) |
| H1–H9 (Halluzinationen & Genauigkeit) | §5 (VAD & Anti-Halluzination), §3 (Modell/Deutsch) |
| S1–S7 (Streaming/Echtzeit) | §6 (Streaming/Echtzeit) |
| M1–M4 (Modell/Speicher/Quantisierung) | §3 (Modellwahl), §4 (Quantisierung) |
| C1–C6 (Cloud/Hybrid) | §7 (Lokal vs. Groq-Cloud) |
