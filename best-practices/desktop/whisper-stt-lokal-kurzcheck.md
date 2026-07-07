# On-Device-Whisper / lokale Transkription Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
