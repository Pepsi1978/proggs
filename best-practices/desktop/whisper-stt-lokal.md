# On-Device-Whisper / lokale Transkription — Best Practices (Stand 2026-06-14)

> Für Franks Voice-zu-Text in den Overlays (Windows + macOS). `groq-transkription` deckt die **Cloud** ab —
> diese Datei ist die **lokale/On-Device**-Seite (kein Netz, kein laufender Preis, DSGVO-sauber, offline-fest
> für die Schweden-Wildnis). Versions-Anker (live ermittelt, Juni 2026): **whisper.cpp v1.8.6**,
> **faster-whisper 1.2.1** (CTranslate2 4.8 / CUDA 12 + cuDNN 9), **Silero VAD v6.2**, Modelle **large-v3-turbo**
> (809M) + deutsche Finetunes **primeline/whisper-large-v3-turbo-german**, **WhisperKit/argmax-oss-swift v1.0**,
> **Whisper.net 1.9** (C#). Apple Developer / offizielle Repos zuerst, Community als `extern`.
> Zweite Seite (was schiefgeht): [`bugs/desktop/whisper-stt-lokal.md`](../../bugs/desktop/whisper-stt-lokal.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Welche Engine? | Mac → **whisper.cpp + Metal**; Windows+NVIDIA → **faster-whisper int8**; C#/.NET → **Whisper.net** | §2 |
| 2 | Welches Modell für Deutsch? | **large-v3-turbo** (Default) bzw. **primeline-turbo-german** (beste de-WER 2,6 %) | §3 |
| 3 | Sprache | IMMER `language="de"` / `-l de` fest setzen — nie Auto-Detect bei kurzen Befehlen | §3, §5 |
| 4 | Halluzination bei Stille | **VAD vorschalten** (Silero v6 ist eingebaut) — wichtigster Hebel | §5 |
| 5 | Endlos-Wiederholung | `condition_on_previous_text=False`; whisper.cpp `--entropy-thold 2.6` | §5 |
| 6 | Deutsche „ZDF/funk"-Floskeln bei Stille | VAD + `no_speech_threshold 0.6` + Floskel-Stopliste | §5 |
| 7 | Speicher knapp | Quantisierung: faster-whisper `int8_float16`, whisper.cpp `q5_0` | §4 |
| 8 | cuDNN-Ladefehler (NVIDIA) | CUDA 12 + cuDNN 9 (CT2 ≥4.5); sonst `ctranslate2==4.4.0` pinnen | §4 |
| 9 | Live-Vorschau während des Sprechens | Streaming via whisper.cpp `stream` / WhisperLiveKit; Partial grau, Final schwarz | §6 |
| 10 | Push-to-Talk (kurze Äußerung) | Pro Tastendruck frischer State; kleines Live-Modell + großer Final-Pass | §6 |
| 11 | Audio-Format | 16-bit / 16 kHz / mono WAV; sonst per ffmpeg konvertieren | §2, §4 |
| 12 | Lokal oder Groq-Cloud? | **Lokal = Default** (offline, $0, DSGVO); Groq nur Opt-in-Fallback (kein GPU/Spitzenlast) | §7 |
| 13 | Apple Silicon maximal | whisper.cpp Core-ML-Encoder (ANE) / WhisperKit; macOS-26-only: Apple SpeechAnalyzer | §2, §4 |

---

## §1 Architektur & Grundsatz

Für ein Voice-Overlay ist **lokal der Default**: Audio verlässt das Gerät nie (DSGVO), keine laufenden Kosten,
niedrigste Latenz (kein Netz-Roundtrip), funktioniert offline (Flugzeug, Tunnel, Schweden-Wildnis). Beide
lokalen Runtimes nutzen identische Whisper-Gewichte → identische Genauigkeit; der Unterschied ist nur
**Speed pro Plattform**. Zwei harte Plattform-Regeln prägen alles: **auf dem Mac whisper.cpp (Metal/Core ML),
auf Windows/NVIDIA faster-whisper (CUDA int8)** — faster-whisper hat KEIN Metal-Backend (auf Mac nur CPU,
~3× langsamer). Groq-Cloud ist die bewusste Versicherung für „kein GPU-Gerät" oder „maximale Genauigkeit",
nie der stille Default (§7).

---

## §2 Engine-Wahl je Plattform

| Plattform | Empfehlung | Warum |
|-----------|-----------|-------|
| **Apple Silicon (Macs)** | **whisper.cpp + Metal** (+ Core-ML-Encoder/ANE) oder **WhisperKit** | Metal first-class; large-v3-turbo ~10–18× Echtzeit; faster-whisper hat kein Metal |
| **Windows/Linux + NVIDIA** | **faster-whisper** (CTranslate2, `int8`/`int8_float16`) | ~12× Echtzeit RTX 4070, ~40 % weniger VRAM; auch whisper.cpp CUDA möglich |
| **C#/.NET (WPF-Overlay)** | **Whisper.net** (`Whisper.net.AllRuntimes`) | eine .exe ohne Python; deckt CUDA 13/12 + Vulkan + CPU automatisch ab — passt zu Franks Stack |
| **AMD/Intel-iGPU** | **whisper.cpp Vulkan** (`-DGGML_VULKAN=1`, ≥ v1.8.3) | bis 12× Boost auf iGPU; ein Binary für alle Vendor |
| **macOS 26-only, einsprachig** | **Apple SpeechAnalyzer** (nativ, ANE, kostenlos) | schnellste/energieeffizienteste, aber nur eine Sprache pro Aufnahme |

**whisper.cpp Setup (macOS Metal, Standard):**
```bash
git clone https://github.com/ggml-org/whisper.cpp && cd whisper.cpp
sh ./models/download-ggml-model.sh large-v3-turbo-q5_0
sh ./models/download-vad-model.sh silero-v6.2.0
brew install sdl2 && cmake -B build -DWHISPER_SDL2=ON && cmake --build build -j --config Release
./build/bin/whisper-cli -m models/ggml-large-v3-turbo-q5_0.bin -l de \
  --vad -vm models/ggml-silero-v6.2.0.bin -f input.wav
# optional ANE-Encoder (>3× schneller): cmake -B build -DWHISPER_COREML=1 + ./models/generate-coreml-model.sh
```

**faster-whisper (Windows/NVIDIA):** `pip install faster-whisper` — Modelle laden automatisch von HuggingFace.
**Whisper.net (C#):** `WhisperGgmlDownloader.Default.GetGgmlModelAsync(GgmlType.LargeV3Turbo)`, `factory.CreateBuilder().WithLanguage("de").Build()`.

**Audio-Vorgabe:** alle erwarten 16-bit/16 kHz/mono WAV (`ffmpeg -i in.mp3 -ar 16000 -ac 1 -c:a pcm_s16le out.wav`).

Quellen: [ggml-org/whisper.cpp](https://github.com/ggml-org/whisper.cpp) · [SYSTRAN/faster-whisper](https://github.com/SYSTRAN/faster-whisper) · [argmaxinc/argmax-oss-swift](https://github.com/argmaxinc/argmax-oss-swift) · offiziell. `extern`: [sandrohanea/whisper.net](https://github.com/sandrohanea/whisper.net).

---

## §3 Modellwahl & deutsche Genauigkeit

**`large-v3-turbo`** (Okt 2024) ist der Sweet Spot: large-v3 mit nur 4 statt 32 Decoder-Layern → 809M Params,
~2–6× schneller, nur +0,3–0,7 Punkte WER. Deutsch ist volle Trainingssprache (nur Übersetzung ist schwächer).

| Modell | Params | Speicher (q5/int8) | Speed (GPU) | de-WER | Einsatz |
|--------|--------|--------------------|-------------|--------|---------|
| small | 244M | ~1 GB | ~6× | ~9–12 % | Live-Vorschau / schwache HW |
| medium | 769M | ~2,5 GB | ~2× | ~6–8 % | erst hier diktattauglich |
| **large-v3-turbo** | 809M | ~1,6 GB | ~8× | **3,65 % Mix** | **Default-Endfassung** |
| **primeline-turbo-german** | 809M | ~1,6 GB | ~8× | **2,63 % Mix (Bestwert)** | beste deutsche Genauigkeit |
| large-v3 | 1,55B | ~3 GB | ~1× | 3,5 % CV | max. Genauigkeit/Übersetzung |

**Empfehlung:** `large-v3-turbo` als Default; für maximale deutsche Genauigkeit das Finetune
**`primeline/whisper-large-v3-turbo-german`** (vorquantisierte CT2-Varianten existieren). **Nie** englische
`.en`-Modelle oder das offizielle `distil-large-v3` (English-only) für Deutsch — dafür gibt es
`primeline/distil-whisper-large-v3-german`. **Immer `language="de"` fest setzen** — Auto-Detect versagt bei
kurzen Befehlen und triggert fremdsprachige Halluzinationen.

Quellen: [openai/whisper Discussion #2363 (turbo)](https://github.com/openai/whisper/discussions/2363) · [primeline turbo-german Model Card](https://huggingface.co/primeline/whisper-large-v3-turbo-german) · offiziell/`extern`.

---

## §4 GPU-Beschleunigung & Quantisierung

**Apple Silicon:** Metal ist Default; Core-ML-Encoder (`-DWHISPER_COREML=1`) schiebt den Encoder auf die ANE
(>3× schneller), erster Lauf kompiliert gerätespezifisch (langsam, danach gecacht). WhisperKit schedult
ANE/GPU/CPU automatisch. **ANE > GPU für Energie/Akku** — ideal fürs dauerlaufende Overlay.

**NVIDIA (faster-whisper):** `compute_type` wählen — GPU `float16` (beste Qualität) oder `int8_float16`
(~40 % weniger VRAM, gleiche Qualität); CPU `int8`. **cuDNN-Falle:** CTranslate2 ≥ 4.5 braucht **CUDA 12 +
cuDNN 9**; bei Mismatch entweder cuDNN 9 installieren (`pip install nvidia-cudnn-cu12==9.*`) oder
`ctranslate2==4.4.0` (cuDNN 8) / `==3.24.0` (CUDA 11) pinnen.

**whisper.cpp Quantisierung:** `q5_0` ist der Sweet Spot (minimaler Verlust, ~⅓ Größe), `q8_0` näher am
Original, `q4` nur bei extremem Speicherdruck. Flash-Attention (`-fa`, seit v1.8.0 Default) beschleunigt —
kann aber bei manchen Nicht-EN-Sprachen die Ausgabe verschlechtern, dann gezielt weglassen.

**Vulkan/AMD/Intel:** `-DGGML_VULKAN=1` (ein Binary für alle Vendor); ≥ v1.8.3 (v1.8.0 war kaputt).
**Nicht-Whisper-Alternative für Deutsch auf NVIDIA:** NeMo **Canary-1B-v2** (25 EU-Sprachen, RTFx ~749,
WER besser als large-v3) — schnellste hochgenaue Option, aber eigene Runtime.

Quellen: [CTranslate2 Quantization](https://opennmt.net/CTranslate2/quantization.html) · [WhisperKit-Paper arXiv 2507.10860](https://arxiv.org/html/2507.10860v1) · [NVIDIA NeMo Canary](https://developer.nvidia.com/blog/accelerating-leaderboard-topping-asr-models-10x-with-nvidia-nemo/) · offiziell/`extern`.

---

## §5 VAD-Vorschaltung & Anti-Halluzination

Whisper halluziniert bei Stille/Nicht-Sprache — bei Deutsch besonders die antrainierten **„ZDF/funk"-Floskeln**
(„Untertitel im Auftrag des ZDF", „Vielen Dank fürs Zuschauen"). Gegenmittel sind **zwei Verteidigungslinien**:

1. **VAD-Vorfilter** — **Silero VAD v6.2 ist in faster-whisper und whisper.cpp eingebaut**, kein separates
   webrtcvad mehr nötig. faster-whisper: `vad_filter=True`; whisper.cpp: `--vad -vm ggml-silero-v6.2.0.bin`.
2. **Whisper-interne Schwellen:** `condition_on_previous_text=False` (KRITISCH gegen Drift/Loops),
   `no_speech_threshold=0.6`, `temperature=0` (kein Fallback-Raten bei kurzen Befehlen), `language="de"` fest.
3. **Nachfilter:** bekannte Floskeln per Stopliste/Regex aus dem finalen Text streichen (VAD allein reicht im Grenzfall nicht).

**Empfohlene Parameter für kurze deutsche Befehle (faster-whisper):**
```python
segments, info = model.transcribe(audio, language="de", vad_filter=True,
    vad_parameters=dict(threshold=0.5, min_speech_duration_ms=250,   # Tipp-/Klick-Geräusche raus
                        min_silence_duration_ms=500, speech_pad_ms=200),
    condition_on_previous_text=False, no_speech_threshold=0.6, temperature=0, beam_size=5)
```

**Endpointing:** Push-to-Talk → VAD ist nur Trimmer, Stopp = Taste loslassen (`speech_pad_ms` großzügig).
Always-listening → VAD ist der Endpointer: eigene Stille-Logik auf Sileros 32-ms-Frames mit Hysterese
(0,5 an / 0,35 aus) und Hangover (Befehle 500–700 ms, Diktat 800–1000 ms). Tipp-/Atemgeräusche zusätzlich
über ein RMS-Gate vor dem VAD abfangen; **kein blindes `noisereduce`** davor (macht Halluzinationen schlimmer).

Quellen: [silero-vad Releases](https://github.com/snakers4/silero-vad/releases) · [faster-whisper vad.py](https://github.com/SYSTRAN/faster-whisper/blob/master/faster_whisper/vad.py) · [riotbib ZDF-Gist](https://gist.github.com/riotbib/3b3c5f817b55b68801d14b8bdb02df09) · offiziell/`extern`.

---

## §6 Streaming / Echtzeit-Transkription

Whisper streamt **nicht nativ** (Encoder-Decoder, auf 30-s-Fenster trainiert, nicht-kausal). Live-Betrieb läuft
über einen Inferenz-Trick: Audio puffern, das Offline-Modell auf wachsendem Puffer wiederholt laufen lassen,
eine **Policy** entscheidet, welcher Präfix „final" ist. Zwei Policies: **LocalAgreement-2** (einfach; Wort gilt
als final, wenn 2 Läufe denselben Präfix liefern; Final-Latenz ≈ 2× Chunk) und **AlignAtt** (SimulStreaming,
SOTA 2025, ~5× schneller, attention-guided).

- **Natives Desktop-Overlay (Swift/C#):** whisper.cpp `whisper-stream` als lokaler Prozess. Push-to-Talk:
  `--step 500 --length 5000 --keep 200`; Dauer-Diktat: `--step 0 --length 30000 -vth 0.6` (Sliding-Window + VAD).
  `--keep` (Overlap) verhindert an Chunk-Grenzen zersägte Wörter.
- **Web/Electron-Overlay:** **WhisperLiveKit** (FastAPI + WebSocket + mitgeliefertes Frontend, AlignAtt default).
- **Zwei-Puffer-Anzeige (gegen Flackern):** Partial = grau/überschreibbar (`is_final:false`), Final = schwarz/fest.
- **Two-Pass (empfohlen):** Pass 1 live mit `small`/`base` für sofortige Partials, Pass 2 beim Endpoint mit
  `large-v3-turbo` für die korrigierte Endfassung. Latenz-Untergrenze realistisch ~3 s mit `medium` (Encoder
  dominiert) → turbo + GPU senkt das deutlich.
- **Push-to-Talk-Spezialfall:** kurze Äußerung < 30 s = kein Long-Form-Problem → pro Tastendruck **frischer State**
  (gegen Carryover-Repeats), beim Loslassen ein Final-Pass über den ganzen Puffer = praktisch Offline-Qualität.

Quellen: [ufal/SimulStreaming](https://github.com/ufal/SimulStreaming) · [QuentinFuxa/WhisperLiveKit](https://github.com/QuentinFuxa/WhisperLiveKit) · [whisper.cpp stream](https://github.com/ggml-org/whisper.cpp) · `extern`.

---

## §7 Lokal vs. Groq-Cloud (Entscheidung & Hybrid)

**Groq-Preise (Stand 14.06.2026):** `whisper-large-v3-turbo` **$0,04/h**, `whisper-large-v3` **$0,111/h**;
~200× Echtzeit (Compute trivial schnell, Netzlatenz dominiert); Mindestabrechnung **10 s/Request**; standardmäßig
**keine Retention** (ZDR aktivierbar), aber Daten in **US-GCP-Buckets** (SCC-Transfer).

| Kriterium | → **Lokal** | → **Groq-Cloud** |
|-----------|-------------|------------------|
| Offline / kein Netz (Schweden) | ✅ Pflicht | ❌ tot |
| Datenschutz / DSGVO | ✅ Audio bleibt auf Gerät | ⚠️ US-Buckets, ZDR aktivierbar |
| Laufende Kosten | ✅ $0 (nur Strom) | ⚠️ $0,04–0,111/h, min. 10 s |
| Latenz (Gerät mit GPU) | ✅ 50–300 ms | ⚠️ Netz-Roundtrip |
| Höchste Genauigkeit ohne eigenes GPU | ❌ CPU zu langsam | ✅ large-v3 |
| Spitzenlast / viele Stunden | ⚠️ HW-begrenzt | ✅ skaliert |

**Hybrid-Empfehlung (die eigentliche Best Practice):** Lokal = Default (immer). Cloud-Fallback nur auf
**explizites Opt-in** (sichtbarer Toggle „Nur lokal / Cloud erlaubt", Default lokal) und nur wenn: kein nutzbares
GPU + Netz da → Groq turbo; „höchste Genauigkeit" gefordert → Groq large-v3. Cloud-Call mit kurzem Timeout (~3 s)
absichern und bei Überschreitung automatisch auf lokal zurückfallen. Wenn Cloud: ZDR aktivieren, `language="de"`
mitsenden, kurze Schnipsel lokal verarbeiten (10-s-Mindestabrechnung). Bei harter EU-Datenresidenz: Deepgram
EU-Endpoint statt Groq. Echtes Cloud-Live-Streaming (falls lokal nicht reicht): ElevenLabs Scribe v2 Realtime
($0,39/h) oder OpenAI Realtime-Whisper (~$1,02/h) — Groq-STT ist batch-orientiert.

Quellen: [groq.com/pricing](https://groq.com/pricing) · [console.groq.com/docs/your-data](https://console.groq.com/docs/your-data) · offiziell. `extern`: [promptquorum lokal-Vergleich](https://www.promptquorum.com/power-local-llm/local-whisper-stt-comparison-2026).

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/desktop/whisper-stt-lokal.md`) |
|-------------------------|--------------------------------------------------------------|
| §2 (Engine-Wahl), §4 (GPU/Quantisierung) | E1–E29 (cuDNN/CT2-Pin/Vulkan/WAV/FLAC/Generator/Core-ML/Flash-Attn/whisper.cpp-VAD/Server-Leak/RTX50/Batched/Threads/hotwords) |
| §5 (VAD & Anti-Halluzination), §3 (Modell/Deutsch) | H1–H17 (Stille-/ZDF-Halluzination/Repetition/Drift/Sprach-Autodetect/en-only/large-v3-Regression/no_speech-Loop/Kleinschreibung/ITN/Code-Switching/Dialekt) |
| §6 (Streaming/Echtzeit) | S1–S19 (Wort-Zersägen/Partials/Buffer-Freeze/Latenz-Floor/VAC-512-Crash/Multi-Stream/WebM/WhisperLiveKit/WhisperLive) |
| §3 (Modellwahl), §4 (Quantisierung) | M1–M9 (OOM/int8-Degradation/Turbo-Übersetzung/Mac-faster-whisper/primeline-CT2-Stopp/distil-Timestamps/NeMo) |
| §7 (Lokal vs. Groq-Cloud) | C1–C6 (Latenz-Spikes/Netzausfall/10s-Abrechnung/Data-Retention/no_speech_prob) |
| §2 (Engine-Wahl), §5 (VAD) | A1–A11 (Resampling/Float-Normalisierung/Stereo/NAudio-WASAPI/AVAudioEngine-AirPods/ffmpeg-Pegel/Overflow/AGC-Echo) |
| §2 (Engine-Wahl: Whisper.net/WhisperKit) | N1–N14 (GpuDevice/CUDA-Runtime/Publish-Pfad/CorruptedWave/Concurrency · WhisperKit installTap/manifest.plist/ANE/Leak/Compile/v1.0-Breaking) |

> **Checkpoint:** Vollständig recherchiert (7 Researcher parallel, offizielle Repos zuerst, Stand 2026-06-14).
> Kern für Franks Overlays: **lokal als Default** — Mac whisper.cpp+Metal, Windows faster-whisper int8 (C# via
> Whisper.net), Modell `large-v3-turbo`/`primeline-turbo-german`, **immer `language=de` + VAD** gegen
> Halluzinationen, Streaming via `whisper-stream`/WhisperLiveKit mit Partial/Final, Groq nur als Opt-in-Fallback.
