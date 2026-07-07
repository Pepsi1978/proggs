# Best Practices: Lacher-Extraktion aus YouTube-Audio

**Recherche-Datum:** 2026-05-13
**Forschungsgrundlage:** 4 parallele Researcher-Agents, ~50 Web-Quellen, Stand 2025/2026
**Kontext:** Wir extrahieren Lacher aus YouTube-Videos und bauen MP3-Compilations. Bisheriger Stack: `yt-dlp + Jrgillick laughter-detection (2019) + htdemucs_ft + ffmpeg`. Diese Recherche zeigt was 2025/2026 State-of-the-Art ist.

---

## TL;DR — Die empfohlene Pipeline (Stand 2026)

```
yt-dlp (bestaudio → WAV 44.1 kHz Stereo)
  ↓
Mel-Band RoFormer (Vocals-Stem extrahieren, Musik entfernen)
  ↓
Optional: htdemucs_ft als 2nd Pass (nur bei sichtbaren Resten)
  ↓
16 kHz Mono Kopie (nur für Detektion, Master bleibt 44.1 kHz)
  ↓
Omine 2024 LaughterSegmentation (Transformer, Interspeech 2024)
  ↓
Hysteresis-Threshold (onset=0.65, offset=0.35)
+ Median-Filter Smoothing (250 ms Fenster)
+ Min-Duration 200 ms, Gap-Merge 150 ms
  ↓
Segmente aus 44.1 kHz Master schneiden + Fade 50 ms pro Segment
  ↓
concat-Demuxer (verlustlos)
  ↓
Two-Pass EBU R128 loudnorm (-16 LUFS, TP -1.5 dBTP)
  ↓
MP3 192 kbps
  ↓
Validierung: silencedetect + ebur128 + astats
```

**Kernerkenntnis:** Jeder einzelne Schritt unseres aktuellen Stacks hat einen besseren Nachfolger 2025/2026. Der wichtigste Upgrade-Schritt ist **Jrgillick → Omine 2024** (Detektion) und **htdemucs_ft → Mel-Band RoFormer** (Separation).

---

## 1. Audio-Download (yt-dlp)

### Sample-Rate-Kette (wichtig!)

```
YouTube (Opus 48 kHz Stereo)
  ↓ verlustlos extrahieren
WAV 44.1 kHz Stereo (Master — für finalen Schnitt)
  ↓ downsamplen (Kopie!)
WAV 16 kHz Mono (Detektions-Kopie — wird verworfen)
```

**Warum diese Kette?** Die Detektion (Jrgillick, Omine, YAMNet) erwartet 16 kHz Mono. Aber wenn man am Ende aus dem 16 kHz-File schneidet, klingt die MP3 dumpf. Lösung: 16 kHz nur zur Zeitstempel-Findung, geschnitten wird aus dem 44.1 kHz Master.

### Best-Practice-Befehl

```bash
yt-dlp \
  -f "bestaudio" \
  -x \
  --audio-format wav \
  --audio-quality 0 \
  --postprocessor-args "ffmpeg:-ar 44100 -ac 2" \
  -o "%(id)s.%(ext)s" \
  "https://www.youtube.com/watch?v=VIDEO_ID"

# Danach 16 kHz Mono Kopie nur für Detektion:
ffmpeg -i VIDEO_ID.wav -ar 16000 -ac 1 VIDEO_ID-16k.wav
```

---

## 2. Source Separation (Musik entfernen)

### Ranking 2025/2026 (best to worst für Vocals/Lacher-Erhaltung)

| Modell | SDR Vocals | Vorteil | Nachteil |
|---|---|---|---|
| **Mel-Band RoFormer** | ~10.17 dB | Mel-Subbands = nah an Hörwahrnehmung, beste Lacher-Erhaltung | Braucht ZFTurbo-Repo oder UVR5 |
| **BS-RoFormer (ByteDance)** | ~9.7 dB | SDX23-Gewinner | ~0.5 dB unter Mel-Band RoFormer |
| **MDX-Net / Kim_Vocal_2** | ~10.17 dB | Sehr klare Lead-Vocal-Isolierung | Backing-Vocals können durchwandern |
| **htdemucs_ft** (was wir nutzen) | ~8.78 dB | Standard, einfach via pip | Deutlich unter RoFormer-Klasse |
| **htdemucs_6s** | ~8.5 dB | 6 Stems (Piano, Guitar extra) | Mehr Vocal-Residuen — kontraproduktiv |

### Empfohlener Wechsel

**Pass 1: Mel-Band RoFormer** (statt htdemucs_ft)
- Repo: [ZFTurbo/Music-Source-Separation-Training](https://github.com/ZFTurbo/Music-Source-Separation-Training)
- GUI-Alternative: [Ultimate Vocal Remover (UVR5)](https://github.com/Anjok07/ultimatevocalremovergui)
- Online-Service: [mvsep.com](https://mvsep.com/en)
- SDR-Vorteil: ~1.5 dB besser als htdemucs_ft
- Lacher-Sicherheit: Lachen ist breitbandiges menschliches Audio (150–4000 Hz) — Mel-Band RoFormer klassifiziert es zuverlässig als Vocals, nicht als Musik

**Pass 2 (optional): htdemucs_ft als Nachreinigung**
- Nur anwenden wenn nach Pass 1 noch Musik-Reste über -30 dBFS sichtbar im Spektrogramm
- Aggressive Anwendung schadet: "Washy"-Effekt durch Phasen-Artefakte
- Faustregel: erst Spektrogramm prüfen, dann entscheiden

### Spezial-Filter (nach Source Separation)

```bash
ffmpeg -i vocals.wav -af \
  "highpass=f=80:poles=2, \
   lowpass=f=12000:poles=2, \
   agate=threshold=0.01:ratio=10:attack=5:release=200, \
   loudnorm=I=-16:TP=-1.5:LRA=11" \
  output_clean.wav
```

| Filter | Wert | Zweck |
|---|---|---|
| `highpass=f=80` | 80 Hz | Entfernt Musik-Bass-Reste und Raumrauschen |
| `lowpass=f=12000` | 12 kHz | Kappung von Hochfrequenz-Artefakten |
| `agate` | threshold 0.01 | Noise Gate für Stille zwischen Lachern |
| `loudnorm` | -16 LUFS | EBU R128 für YouTube-Konformität |

**Wann spektrale Subtraktion / Wiener Filter?** Nur bei sehr einfachen, stationären Hintergründen. Für YouTube mit variabler Musik **nicht empfohlen** — ML-Modelle sind klar überlegen.

---

## 3. Lach-Detektion (Best-Modell 2024/2026)

### Ranking

| Modell | Jahr | Ansatz | Stärke | Schwäche |
|---|---|---|---|---|
| **Omine LaughterSegmentation** ⭐ | 2024 | Transformer + synthetische Datenaugmentierung | State-of-the-Art, robust gegen Audio-Umgebungen | 1.26 GB Modell, Python ≤ 3.11 |
| **IDEO LaughDetection** | 2019 (stabil) | VGGish + BiLSTM, AudioSet-trainiert | **Klassifiziert 6 Lach-Typen** (Giggle, Belly Laugh, Chuckle...) | Nicht in Benchmark-Papern |
| **AST/PANN** | 2021-2023 | Audio Spectrogram Transformer / CNN14 | mAP 0.44-0.46 auf AudioSet | Kein dediziertes Lach-Training |
| **Jrgillick** (was wir nutzen) | 2019/2021 | ResNet auf Switchboard | Funktioniert | Switchboard = Telefonkommunikation, schlecht für YouTube |
| **YAMNet** | 2019 | MobileNet, 521 Klassen | Einfach via TF Hub | mAP 0.31 — zu generisch |

### Warum Omine 2024 die beste Wahl ist

- **Synthetische Datenaugmentierung:** Lachen wird automatisch auf beliebige Audio-Tracks gelegt → trainiert auf realer Vielfalt
- **Übertrifft Jrgillick in noisy environments** laut Interspeech-2024-Paper
- **Direkter Drop-in-Ersatz:** gleicher API-Style (Audio → Timestamps)
- **HuggingFace-Modell** verfügbar → `pip install transformers` reicht für Inferenz
- **Repo:** [github.com/omine-me/LaughterSegmentation](https://github.com/omine-me/LaughterSegmentation)
- **Paper:** ["Robust Laughter Segmentation with Automatic Diverse Data Synthesis"](https://www.isca-archive.org/interspeech_2024/omine24_interspeech.html), Interspeech 2024

### IDEO als Ergänzung (für Comedy-Compilations relevant)

- Klassifiziert in 6 Kategorien: Laughter, Baby Laughter, Giggle, Snicker, Belly Laugh, Chuckle/Chortle
- Nutzbar als **Klassifikator NACH Omine-Segmentation**: Erst Zeitstempel finden, dann Lach-Typ einordnen
- Use Case: "Compilation nur mit Belly Laughs" oder "kein höfliches Lachen"
- Repo: [github.com/ideo/LaughDetection](https://github.com/ideo/LaughDetection)

---

## 4. Segmentation Best Practices (aus DCASE/SED-Literatur)

### Smoothing-Strategie

**Median-Filter ist Standard** in DCASE-Systemen — NICHT Moving Average oder Gaussian.

**Begründung:**
- Median unterdrückt Impulse (einzelne falsch-positive Frames) ohne Signal-Verschleppung
- Moving Average "verschmiert" Kanten → schlechte Segmentgrenzen
- Gaussian verzögert → spät detektierte Onsets

**Konkrete Werte bei 100 Hz (10 ms/Frame):**

| Ziel | Fenster | Frames |
|---|---|---|
| Kurze Events (Lachen 0.3–2 s) | 150–300 ms | 15–30 |
| Aggressive Glättung (wenig False Positives) | 400–500 ms | 40–50 |
| Wenn scharfe Ecken wichtig | Savitzky-Golay Ordnung 2, 200 ms | 20 |

```python
from scipy.signal import medfilt
smoothed = medfilt(prob_stream, kernel_size=21)  # 210ms bei 100Hz, ungerade!
```

### Thresholding-Strategie: Hysteresis (klarer Sieger)

**Statischer Threshold** führt zu Flackern — ein Lacher wird in 5 kleine Fragmente zerschnitten.

**Hysteresis-Thresholding** nutzt zwei Schwellwerte:
- **Hi-Threshold (onset)** = aktiviert Detektor (Segment startet)
- **Lo-Threshold (offset)** = deaktiviert Detektor (Segment endet)

**Pyannote-VAD-Standardwerte (gut getestet):**
- onset = **0.5–0.7** (Hi)
- offset = **0.3–0.4** (Lo)
- Praktisch bewährt: onset=0.767, offset=0.377

```python
def hysteresis_threshold(probs, hi=0.65, lo=0.35):
    active = False
    labels = []
    for p in probs:
        if not active and p >= hi:
            active = True
        elif active and p < lo:
            active = False
        labels.append(active)
    return labels
```

**Otsu-Thresholding NICHT empfohlen:** Bei Lachen die selten sind (<10% Frames) schiebt Otsu den Threshold zu tief → False Positives.

### Empfohlene Hyperparameter (aus VAD/SED-Literatur)

```yaml
# Für Lach-Detektion bei 100 Hz Frame-Rate
smoothing:
  method: median
  window_ms: 250          # 25 Frames

thresholding:
  onset: 0.65             # Hi-Threshold
  offset: 0.35            # Lo-Threshold

segment_filter:
  min_duration_on_ms: 200      # kürzestes akzeptiertes Segment
  min_duration_off_ms: 150     # max Pause → Merge
  max_duration_s: 20           # bei langen Segmenten: am Minimum splitten

padding:
  pad_pre_ms: 50          # Vorlauf (Anlach-Phase nicht abschneiden)
  pad_post_ms: 150        # Nachlauf (Ausklang mitnehmen)
```

### Segment-Filtering (Python-Referenzimplementation)

```python
def merge_and_filter(segments, min_on=0.2, min_off=0.1,
                     pad_pre=0.05, pad_post=0.15, total_duration=999):
    # 1. Gap-Merge: Segmente mit Lücke < min_off zusammenführen
    merged = []
    for seg in segments:
        if merged and seg.start - merged[-1].end < min_off:
            merged[-1].end = seg.end
        else:
            merged.append(seg)
    # 2. Min-Duration-Filter
    merged = [s for s in merged if (s.end - s.start) >= min_on]
    # 3. Padding
    for s in merged:
        s.start = max(0, s.start - pad_pre)
        s.end = min(total_duration, s.end + pad_post)
    return merged
```

---

## 5. Concat Best Practices (ffmpeg)

### Grundregel: Niemals hart schneiden

Hard Cuts erzeugen **garantiert** Klick-Artefakte (Diskontinuität in der Wellenform). Lösung: Fade pro Segment ODER Crossfade zwischen Segmenten.

### Methode A: Fade pro Segment + concat-Demuxer (empfohlen)

**Schritt 1: Jedes Segment einzeln mit 50 ms Fade-In/Out:**

```bash
DAUER=6.38  # Beispiel
ffmpeg -i input.wav -ss 12.3 -to 18.7 \
  -af "afade=t=in:st=0:d=0.05,afade=t=out:st=$DAUER:d=0.05" \
  segment_001.wav
```

**Schritt 2: Concat-Demuxer (verlustlos, schnell):**

```bash
# filelist.txt:
# file 'segment_001.wav'
# file 'segment_002.wav'
# ...

ffmpeg -f concat -safe 0 -i filelist.txt -c copy output_raw.wav
```

### Methode B: Crossfade zwischen Segmenten

```bash
# Zwei Segmente mit 50ms Crossfade:
ffmpeg -i seg1.wav -i seg2.wav \
  -filter_complex "[0][1]acrossfade=d=0.05:c1=tri:c2=tri" \
  joined.wav

# Mehrere Segmente (komplexer Filtergraph):
ffmpeg -i s1.wav -i s2.wav -i s3.wav \
  -filter_complex \
  "[0][1]acrossfade=d=0.03:c1=tri:c2=tri[a01]; \
   [a01][2]acrossfade=d=0.03:c1=tri:c2=tri[out]" \
  -map "[out]" compilation.wav
```

### Fade-Längen-Empfehlungen

| Use Case | Fade-Länge |
|---|---|
| Klick-Verhinderung (unsichtbar) | 5–20 ms |
| Weicher Cut (hörbar aber dezent) | 30–80 ms |
| Musikalischer Übergang | 100–300 ms |
| **Lacher-Compilations** | **20–50 ms** (Energie erhalten) |

### Zero-Crossing-Tipp

Vor dem Schnitt prüfen ob Schnittpunkt am Nulldurchgang liegt (vermeidet Klicks ohne Fade):

```python
import numpy as np
def find_zero_crossing_near(audio, sr, target_sec, window_ms=10):
    center = int(target_sec * sr)
    window = int(window_ms / 1000 * sr)
    segment = audio[center-window:center+window]
    crossings = np.where(np.diff(np.sign(segment)))[0]
    if len(crossings) > 0:
        best = crossings[len(crossings)//2]
        return (center - window + best) / sr
    return target_sec
```

---

## 6. Lautstärke-Normalisierung (EBU R128, Two-Pass)

Two-Pass loudnorm ist **Pflicht** für gute Ergebnisse — One-Pass produziert hörbare Pumping-Effekte.

### Zwei-Schritt-Verfahren

**Pass 1 — Analyse (JSON ausgeben, nichts encodieren):**

```bash
ffmpeg -i output_raw.wav \
  -af "loudnorm=I=-16:TP=-1.5:LRA=11:print_format=json" \
  -f null - 2>&1 | grep -A 12 "Parsed_loudnorm"
```

Liefert: `measured_I`, `measured_TP`, `measured_LRA`, `measured_thresh`, `offset`.

**Pass 2 — Encoding mit gemessenen Werten:**

```bash
ffmpeg -i output_raw.wav \
  -af "loudnorm=I=-16:TP=-1.5:LRA=11:\
measured_I=-18.2:measured_TP=-2.1:measured_LRA=7.3:\
measured_thresh=-28.7:offset=1.7:linear=true" \
  -ar 44100 -b:a 192k \
  output_normalized.mp3
```

### Ziel-Werte für Lacher-Compilations

- **Integrated Loudness (I):** -16 LUFS (YouTube-Standard, Streaming-konform)
- **True Peak (TP):** -1.5 dBTP
- **Loudness Range (LRA):** 11 LU
- **NICHT -23 LUFS** (Rundfunk-Standard) — Lachen ist laut, soll laut bleiben

### Einfacher-Wrapper: ffmpeg-normalize

```bash
pip install ffmpeg-normalize
ffmpeg-normalize output_raw.wav -o output.mp3 \
  -c:a libmp3lame -b:a 192k --target-level -16
```

Macht Two-Pass automatisch.

---

## 7. Validierung der finalen MP3

```bash
# 1. Stille-Segmente finden (sollten KEINE in Lacher-Compilation existieren):
ffmpeg -i output.mp3 -af "silencedetect=n=-30dB:d=0.3" -f null - 2>&1 | grep silence

# 2. Audio-Statistiken (Peak, RMS, DC-Offset):
ffmpeg -i output.mp3 -af "astats=metadata=1:reset=1" -f null - 2>&1 \
  | grep -E "RMS|Peak|Flat"

# 3. Lautstärke-Check (LUFS bestätigen):
ffmpeg -i output.mp3 -af "ebur128=peak=true" -f null - 2>&1 | tail -20

# 4. Bitrate/Format-Check:
ffprobe -v quiet -print_format json -show_streams output.mp3 | python3 -m json.tool
```

### Alarmsignale

| Signal | Bedeutung |
|---|---|
| `silence_start` im Output | Concat-Artefakt oder fehlendes Segment |
| `RMS level dB` unter -25 | Zu leise, Normalisierung fehlgeschlagen |
| `Flat factor` über 0.01 | Clipping |
| LUFS nicht in [-17, -15] | loudnorm hat nicht funktioniert |

---

## 8. Cloud-APIs (Alternative ohne GPU)

| API | Laughter-Support | Preis | Empfehlung |
|---|---|---|---|
| **AssemblyAI Universal-3-Pro** | Native "laughter"-Tag | ~$0.15/h | **Ja** für Prototypen |
| Deepgram Nova-3 | Kein Laughter-Tag | $0.46/h | Nein |
| Google Cloud Speech | Kein Laughter-Event | variabel | Nein |

```python
import assemblyai as aai
aai.settings.api_key = "KEY"
transcriber = aai.Transcriber()
transcript = transcriber.transcribe("audio.wav",
    config=aai.TranscriptionConfig(audio_intelligence=True))
# Lacher in transcript.words mit type="non-speech" und tag="laughter"
```

**Fazit:** AssemblyAI kein Ersatz für lokale Modelle bei Präzision — aber nützlich als schneller Vorfilter oder Fallback ohne GPU. Für 60-Min-Video: ~$0.15.

---

## 9. Bestehende End-to-End-Tools

**Kein fertiges Repo für die komplette YouTube → Lacher-MP3-Pipeline gefunden.** Nur Bausteine:

| Repo | Was es tut | Bewertung |
|---|---|---|
| [omine-me/LaughterSegmentation](https://github.com/omine-me/LaughterSegmentation) | State-of-the-Art Detektion 2024 | **Beste Detektion** |
| [jrgillick/laughter-detection](https://github.com/jrgillick/laughter-detection) | Klassiker, gut dokumentiert | Solide aber älter |
| [honeyvig/AI-for-Laughter-Detection-in-Videos](https://github.com/honeyvig/AI-for-Laughter-Detection-in-Videos) | Stand-up/Comedy Timestamps | Wrapper um Jrgillick |
| [jeffg-dev/laughr](https://github.com/jeffg-dev/laughr) | Lacher **stummschalten** | Inverse Richtung — irrelevant |

**Lücke in der Open-Source-Welt:** Ein integriertes Repo `youtube-laughter-compilation` mit Omine + Mel-Band RoFormer + ffmpeg gibt es noch nicht. Wäre ein lohnenswertes Projekt.

---

## 10. Upgrade-Pfad für unsere bestehende Pipeline

| Stack-Komponente | Aktuell | Empfohlen | Aufwand |
|---|---|---|---|
| Download | yt-dlp (16 kHz mono) | yt-dlp (44.1 kHz stereo + 16 kHz Kopie) | 5 Min |
| Source Separation | htdemucs_ft | **Mel-Band RoFormer** (via ZFTurbo oder UVR5) | 30 Min Setup |
| Detektion | Jrgillick ResNet 2019 | **Omine LaughterSegmentation 2024** | 1 h Setup |
| Thresholding | Statisch 0.7 | **Hysteresis 0.65/0.35** + Median-Filter | 15 Min |
| Concat | Hard Cut + filter_complex | **Fade 50ms + concat-Demuxer** | 30 Min |
| Normalisierung | Single-Pass loudnorm | **Two-Pass loudnorm** oder ffmpeg-normalize | 15 Min |
| Validierung | Keine | **silencedetect + ebur128 + astats** | 15 Min |

**Geschätzter Gesamt-Upgrade-Aufwand: ~3-4 Stunden** (inkl. Tests). Erwartetes Qualitätsplus: Deutlich weniger Musik-Reste (Mel-Band RoFormer ~1.5 dB besser), präzisere Lach-Erkennung (Omine schlägt Jrgillick besonders bei YouTube-typischem Audio), keine Klick-Artefakte am Concat.

---

## 11. Offene Forschungsfragen

1. **Lacher vs. Singen/Schreien:** Beide aktivieren ähnliche Vocal-Frequenzen. Wie zuverlässig unterscheidet Omine das?
2. **Dauer-Distribution:** Was ist eine "natürlich" wirkende Compilation-Länge? 30 s, 2 Min, 5 Min?
3. **Multi-Video-Compilations:** Pipeline funktioniert nur für Single-Video. Multi-Video bräuchte zusätzlich Beat-Matching / Lautstärke-Übergänge.
4. **Lach-Typ-Diversität:** Soll eine Compilation gemischte Typen (Giggle + Belly Laugh) enthalten oder nur einen Typ?

---

## 12. Quellen (alphabetisch nach Kategorie)

### Lach-Detektion
- [Omine LaughterSegmentation GitHub](https://github.com/omine-me/LaughterSegmentation)
- [Omine Interspeech 2024 Paper](https://www.isca-archive.org/interspeech_2024/omine24_interspeech.html)
- [Jrgillick laughter-detection](https://github.com/jrgillick/laughter-detection)
- [Gillick 2021 — Robust Laughter Detection in Noisy Environments](https://www.isca-archive.org/interspeech_2021/gillick21_interspeech.html)
- [IDEO LaughDetection](https://github.com/ideo/LaughDetection)
- [ACL 2024 — Multimodal Laughter Stand-Up Comedy](https://aclanthology.org/2024.lrec-main.1037/)
- [arXiv 2403.02112 — Smile/Laughter Intensity](https://arxiv.org/abs/2403.02112)

### Source Separation
- [ZFTurbo Music-Source-Separation-Training](https://github.com/ZFTurbo/Music-Source-Separation-Training)
- [Mel-Band RoFormer Paper (arXiv 2310.01809)](https://arxiv.org/abs/2310.01809)
- [Mel-RoFormer Vocal Separation (arXiv 2409.04702)](https://arxiv.org/html/2409.04702v1)
- [lucidrains/BS-RoFormer](https://github.com/lucidrains/BS-RoFormer)
- [Ultimate Vocal Remover GUI](https://github.com/Anjok07/ultimatevocalremovergui)
- [MVSEP Online-Service + Leaderboard](https://mvsep.com/quality_checker/multisong_leaderboard?sort=vocals)
- [UVR Best Model Guide 2025](https://vocalremover.cloud/blog/uvr-best-model-aug-2025)
- [SDX Benchmarks (arXiv 2305.07489)](https://arxiv.org/html/2305.07489v2)

### Segmentation / VAD / SED
- [DCASE 2024 Task 4: Sound Event Detection](https://arxiv.org/html/2406.08056v1)
- [Post-Processing Independent SED Evaluation (arXiv 2306.15440)](https://arxiv.org/html/2306.15440)
- [DCASE 2024 Cornell Technical Report](https://dcase.community/documents/workshop2024/proceedings/DCASE2024Workshop_Cornell_30.pdf)
- [SpeechBrain VAD Documentation](https://speechbrain.readthedocs.io/en/latest/tutorials/tasks/voice-activity-detection.html)
- [pyannote segmentation-3.0](https://huggingface.co/pyannote/segmentation-3.0)
- [pyannote Parameter Tuning](https://github.com/pyannote/pyannote-audio/discussions/732)

### ffmpeg / Tooling
- [ffmpeg acrossfade Filter](https://ayosec.github.io/ffmpeg-filters-docs/7.1/Filters/Audio/acrossfade.html)
- [ffmpeg highpass Filter](https://ayosec.github.io/ffmpeg-filters-docs/7.1/Filters/Audio/highpass.html)
- [ffmpeg loudnorm Docs](https://ayosec.github.io/ffmpeg-filters-docs/8.0/Filters/Audio/loudnorm.html)
- [ffmpeg silencedetect Docs](https://ayosec.github.io/ffmpeg-filters-docs/8.0/Filters/Audio/silencedetect.html)
- [slhck/ffmpeg-normalize](https://github.com/slhck/ffmpeg-normalize)
- [Audio Loudness Normalization Guide](https://peterforgacs.github.io/2018/05/20/Audio-normalization-with-ffmpeg/)
- [Sound On Sound: Crossfades & Fades](https://www.soundonsound.com/techniques/using-fades-crossfades)
- [Sound On Sound: Klick-Entfernung durch Crossfade](https://www.soundonsound.com/techniques/removing-clicks-crossfades-sonar)

### Cloud-APIs
- [AssemblyAI Audio Intelligence](https://www.assemblyai.com/products/speech-understanding)
- [AssemblyAI vs Deepgram Pricing 2025](https://brasstranscripts.com/blog/assemblyai-vs-deepgram-pricing-high-volume-comparison)

### Bestehende Tools
- [honeyvig/AI-for-Laughter-Detection-in-Videos](https://github.com/honeyvig/AI-for-Laughter-Detection-in-Videos)
- [jeffg-dev/laughr](https://github.com/jeffg-dev/laughr)
- [yt-dlp GitHub](https://github.com/yt-dlp/yt-dlp)
