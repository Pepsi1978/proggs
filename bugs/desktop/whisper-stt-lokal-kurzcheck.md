# Bekannte Bugs/Fallen: On-Device-Whisper / lokale Transkription Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
