# Voice-Agent-Sprachpipeline (Spracheingabe → Verstehen → Sprachausgabe) — Best Practices

**Stand:** 2026-06-10 (8 Researcher parallel: Azure/Google/Deepgram/AssemblyAI-Doku,
Alexa/Google-Assistant-Design, LiveKit/Sierra/GetStream-Engineering, HA/Rhasspy/Willow-Praxis,
Windows-Audio, Groq, Google-TTS). Versions-Anker: **.NET 10.0.204** (net10.0-windows, WPF) ·
**NAudio 2.2.1** · **sherpa-onnx 1.13.2** · **Groq Whisper large-v3-turbo** · **Google TTS Chirp 3 HD** ·
VoiceAgent **1.2.0**.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/desktop/voice-pipeline.md`](../../../bugs/desktop/voice-pipeline.md)):
> der Almanach sagt *was schiefgeht*, diese Datei sagt *wie man eine Sprach-Pipeline von
> vornherein so baut, dass sie fluessig, schnell und zuverlaessig wirkt*. Nachbar-Seiten:
> `best-practices-wake-word.md` (KWS-Engine), `best-practices-groq-transkription.md` (STT),
> `best-practices-dotnet-csharp.md` (WPF/async). Quellen-Flag: `offiziell` vs. `extern`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Session-Lebenszyklus bauen | FSM Idle→Listening→Thinking→Speaking, Timer nur im Idle | §1 |
| 2 | Follow-up-Fenster setzen | Nach JEDER Antwort oeffnen, ab Antwort-Ende zaehlen | §1 |
| 3 | Im Fenster begonnene Rede | IMMER verarbeiten, nie still verwerfen | §1 |
| 4 | Stille-Pause dimensionieren | 300–550 ms Dialog, 1000–2000 ms Diktat + semantisches Netz | §2 |
| 5 | Endpointing robust machen | Drei Schichten: Energie + Transkript + LLM-Check (FERTIG/WEITER) | §2 |
| 6 | Endlos-Aufnahme verhindern | Max-Utterance-Deckel 15–30 s, finalisieren UND verarbeiten | §2 |
| 7 | Daueraufnahme stabil halten | EINE WaveInEvent-Instanz, Watchdog, Stop/Dispose serialisieren | §3 |
| 8 | Windows-Audio einrichten | AGC/Boost/Ducking/Exclusive aus (mmsys.cpl, einmalig) | §3 |
| 9 | Latenz minimieren | Budget < 1 s, Zwischenschritte aufs kleinste Modell/Effort | §4 |
| 10 | Pipeline schnell machen | Parallelisieren statt verketten, Streaming + Ueberlappung | §4 |
| 11 | Gapless-Audio sichern | EIN offener Output, BufferedWaveProvider, LINEAR16/PCM | §4 |
| 12 | Barge-in ermoeglichen | Stufe 1 Mute-Trade-off, Stufe 3 echtes Barge-in mit AEC | §5 |
| 13 | STT-Requests fuer Voice | `language=de` explizit, Timeout 5–10 s, EIN HttpClient | §6 |
| 14 | Frueherkennung sichern | Jeden FSM-Uebergang + Stufen-Latenz als CHECKPOINT loggen | §7 |

---

## 1. Zustandsautomat & Wachfenster-Design

- **4 Zustaende** als explizite FSM: Idle (passiv/Wake-Lauschen) → Listening (Nutzer spricht) →
  Thinking (STT/LLM) → Speaking (TTS) → zurueck. Uebergaenge loggen (CHECKPOINT). `extern`(HF Deep-Dive)
- **Follow-up-Fenster**: oeffnet nach JEDER Antwort (nicht nur nach Rueckfragen); Timer startet
  ab Antwort-ENDE; Richtwerte: Alexa ~5 s, Google ~8 s, grosszuegig fuer Diktat-Nutzer 30–60 s.
  Timer NUR im Idle ticken lassen — `if (busy || listener.IsCapturing) wake.NotifyActivity()`
  VOR jedem `Tick()`. Sprechbeginn (`OnSpeechStart`) verlaengert sofort. `offiziell`(Alexa/Google-Muster)
- **Kein stilles Verwerfen**: pro Aufnahme beim Sprechbeginn festhalten, ob der Agent wach war;
  ist das Fenster beim Fertigwerden zu, trotzdem verarbeiten + wieder aufwachen. `extern`(LiveKit)
- **Confidence-Gate im Fenster** gegen Fremdgespraeche: Sprachgehalt-Vorfilter + (optional)
  Device-Directed-Heuristik, bevor eine Fenster-Aussage als Befehl gilt. `offiziell`(Alexa)
- **Hoerbares/sichtbares Feedback pro Zustand**: Wach-Ton, Einschlaf-Ton NUR beim Auto-Timeout,
  Status-Text pro FSM-Zustand ("Hoert zu", "Denke nach", "Spreche"). `offiziell`(Alexa Light-Ring-Muster)

## 2. Endpointing richtig dimensionieren

- **Werte-Tabelle (offizielle Anbieter-Defaults):** Azure SegmentationSilenceTimeout 300–500 ms
  Dialog / 2000 ms Diktat (erlaubt 100–5000); AssemblyAI 160 ms (konfident) bis max 2400 ms;
  Deepgram endpointing 10 ms + utterance_end ≥ 1000 ms; Rhasspy silence_sec 0.5 + timeout 30 s. `offiziell`
- **Drei Schichten kombinieren**: (1) Energie-/VAD-Stille (schnell, semantisch blind),
  (2) STT-/Transkript-basiert, (3) semantisch (LLM-Endpoint-Check FERTIG/WEITER — reduziert
  False-Cuts um ~45 % gegenueber VAD-only). Mit Schicht 3 darf Schicht 1 flott sein (1000–1200 ms). `extern`(LiveKit)
- **Hysterese + 20–30-ms-Frames** gegen Flattern und Klick-Spikes; **adaptiver Noise-Floor**
  (`thr ≈ 3×noise`, Update nur in Stille) gegen Luefter/AGC-Drift. `extern`
- **Max-Utterance-Deckel Pflicht** (15–30 s): finalisiert + VERARBEITET; Aufnahme laeuft als
  neue Aussage weiter. Ohne Deckel = "hoert ewig zu"-Bug. `offiziell`(Azure SegmentationMaximumTime)
- **Upgrade-Pfad ML-VAD**: Silero (ONNX, .NET via VadSharp/ManySpeech) fuer die Endpoint-
  Entscheidung — 4× weniger Fehler als WebRTC; threshold 0.5 (noisy 0.7–0.8),
  min_silence ~300–550 ms, speech_pad ~300 ms. NIE als Frame-Filter vor ein Streaming-KWS
  (wake-word Bug #33). `extern`(Silero)+`offiziell`(sherpa #2683)

## 3. Robuste Daueraufnahme (NAudio, Windows)

- **EINE langlebige `WaveInEvent`-Instanz**, BufferMilliseconds 50–100, ≥3 Buffer; Handler kehrt
  SOFORT zurueck (Arbeit in bounded Channel, DropOldest). `offiziell`(NAudio)
- **Watchdog**: kommt N Sekunden kein `DataAvailable` trotz aktivem Mikro → Capture neu aufbauen
  (Stop → RecordingStopped abwarten → Dispose → Start). Stop/Dispose strikt serialisieren,
  NIE im Callback. `extern`(NAudio #1168/#1150)
- **Geraetewechsel**: `IMMNotificationClient` registrieren, Events 200–500 ms debouncen,
  geordnet neu starten BEVOR ein blockierender WinMM-Call passiert (#657-Falle). `offiziell`
- **Windows-Audio-Checkliste** (einmalig pruefen, mmsys.cpl): AGC/„Mikrofon automatisch
  anpassen" AUS (auch Treiber-/Headset-Tool!), Mic-Boost ≤ +10 dB, Communications-Ducking
  "Do nothing", Exclusive-Mode-Haken weg, RAW-Mode bewusst waehlen. `offiziell`(MS Learn)

## 4. Latenz-Budget & Pipeline-Ueberlappung

- **Budget aufstellen und messen** (TurnTrace!): Ziel < 1 s wahrgenommene Antwortzeit;
  Stille-Pause + STT + LLM-Zwischenschritte + Antwort-TTFT + TTS-TTFB einzeln loggen. `extern`
- **LLM-Zwischenschritte sind der haeufigste Budget-Killer**: Klassifikation/Verstehen/Endpoint
  aufs kleinste ausreichende Modell bzw. den niedrigsten Reasoning-Effort legen (VoiceAgent:
  Brain-Rolle Codex Effort "low" — 6,6 s → ~2 s, Modell bleibt stark). Schlanke System-Prompts. `extern`(channel.tel/Sierra)
- **Parallelisieren statt verketten**: unabhaengige LLM-Schritte gleichzeitig (Endpoint-Check ||
  Verstehen); fortgeschritten: Speculation auf Zwischen-Transkripten, Filler-Pattern
  (`<speech>Moment…</speech><tool>…</tool>`) bei langen Tools. `extern`(GetStream/Sierra)
- **STT (Groq)**: kein Streaming verfuegbar → kurze VAD-Chunks sofort senden; 16 kHz mono WAV
  (lokal) bzw. FLAC (langsames Netz); `language=de` explizit; Timeout 5–10 s statt SDK-60 s;
  EIN statischer HttpClient (TLS-Reuse = groesster EU-Hebel); `x-groq-region` zur Diagnose. `offiziell`
- **TTS (Google)**: satzweise Synthese + sofortige Wiedergabe (VoiceAgent StreamingSpeaker ✓);
  naechste Stufe **Chirp 3 HD StreamingSynthesize** (bidirektionales gRPC, loest ~500 ms
  REST-TTFB und das 200-RPM-Limit); kein SSML im Streaming → `[pause]`-Markup; 5.000 Bytes/Request. `offiziell`
- **Gapless Audio**: EIN offener Output, `BufferedWaveProvider`, LINEAR16/PCM, identisches
  WaveFormat fuer alle Chunks — keine Klicks zwischen Saetzen. `extern`(markheath)

## 5. Echo & Unterbrechbarkeit (Barge-in)

- **Stufe 1 (einfach, heute):** Mikro pausieren waehrend TTS — robust gegen Self-Trigger,
  ABER kein Barge-in. Bewusster Trade-off, dokumentieren. `extern`
- **Stufe 2:** Partial Ducking (Mic-Gain -10…-20 dB statt Mute) — laute Unterbrechungen
  kommen durch. `extern`(Coval)
- **Stufe 3 (echtes Barge-in):** VAD waehrend TTS aktiv + AEC mit dem TTS-Signal als Referenz +
  Double-Talk-Detection; bei Nutzer-Sprache TTS < 200 ms stoppen, Rest-Buffer verwerfen,
  sofort zuhoeren. AEC braucht LINEARE Wiedergabekette. Windows-Optionen: Voice Capture DSP,
  Azure Speech ML-AEC (`SpeakerReferenceChannel.LastChannel`), StarTrinity-Lib (.NET). `offiziell`+`extern`

## 6. STT-Requests fuer Voice (Kurzfassung, Details in best-practices-groq-transkription.md)

- `language` explizit (nie Auto-Detect bei kurzen Turns — Whisper padded auf 30 s, Stille
  dominiert die Erkennung). `offiziell`
- Anti-Halluzinations-Kette (Voiced-Vorfilter → verbose_json-Confidence-Gate → Segment-Audio-
  Abgleich) ist Pflicht — siehe groq-transkription. `offiziell`
- 429: `retry-after` lesen; 20 RPM + 7.200 Audio-Sek/h gelten fuer free UND dev. `offiziell`

## 7. Observability (Pflicht, observability-first)

- Jeden FSM-Uebergang, jede Fenster-Entscheidung (verlaengert/abgelaufen/Grace-Verarbeitung),
  jeden Verwerfens-Grund (zu kurz/sprach-arm/Deckel) als CHECKPOINT mit erwartet/tatsaechlich loggen.
- Pro Turn die Stufen-Latenzen (Stille, STT, Verstehen, Antwort, TTS) strukturiert erfassen
  (VoiceAgent TurnTrace ✓) — nur so sind Budget-Verletzungen sichtbar.
- Der Bug vom 2026-06-10 wurde in Minuten per Log-Beweiskette gefunden — ohne CHECKPOINTs
  waere es Raten gewesen.

---

## Kopplung zum Bug-Almanach (`bugs/desktop/voice-pipeline.md`)

| Best-Practice (hier) | verhindert Almanach-Bug |
|----------------------|--------------------------|
| §1 Zustandsautomat & Fenster | §1.1–1.4 |
| §2 Endpointing | §2.1–2.4 |
| §3 Daueraufnahme | §3.1–3.3 |
| §4 Latenz & Ueberlappung | §4.1–4.3 |
| §5 Echo/Barge-in | §5.1–5.2 |
| §6 STT-Requests | §6.1–6.2 |
| §7 Observability | (quer — Frueherkennung aller) |

## Wartung
- Gekoppelt mit dem Almanach: neue Praevention hier → Gegenstueck-Bug dort pflegen, Tabellen synchron.
- Bei Versionsspruengen (NAudio 3, sherpa-Updates, neue Google-TTS-Stimmen): Re-Check der Werte.

## Quellen
- Azure Speech (SegmentationSilenceTimeout/MaximumTime) · Deepgram (Endpointing/Utterance-End/Flux) ·
  Google STT v2 (voice_activity_timeout) · AssemblyAI Universal-Streaming `offiziell`
- Amazon (Follow-Up Mode) · Google Nest (Continued Conversation) `offiziell`
- LiveKit (Turn Detection, Voice-Agent-Architektur) · Sierra (voice latency) · GetStream
  (speculative tool calling) · channel.tel (Latenz-Budget) `extern`
- Picovoice VAD-Guide 2026 · Silero VAD Wiki · MS Learn (AEC/Ducking/Signal-Processing-Modes) ·
  NAudio-Issues (#1168/#539/#1150/#657/#1084/#1203, gh-verifiziert) · markheath.net `offiziell`+`extern`
- HA Community (background noise, Voice PE, continuous conversation) · Rhasspy-Doku · Willow #18 `extern`
