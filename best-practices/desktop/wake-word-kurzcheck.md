# Wake-Word / Keyword-Spotting (.NET, C#/WPF) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Engine waehlen | sherpa-onnx (Apache-2.0 Code+Modelle, Wake Word ohne Training, offline) | §0 |
| 2 | KWS-Decode-Sequenz | `AcceptWaveform` → `while(IsReady) Decode` → `GetResult` → `Reset` nach Treffer | §1.1 |
| 3 | Custom Wake Word | Via `text2token`, keywords.txt nur Tokens, KEIN `@original` | §1.3 |
| 4 | PCM16 ↔ float | Immer durch `32768f`; float→PCM16 vorher `Math.Clamp(-1,1)` | §1.4, §2.2 |
| 5 | Stream-Lifecycle | Spotter/Stream `Dispose()` in `OnExit`, Stream persistent halten | §1.4 |
| 6 | Mikrofon erfassen | `WasapiCapture` (Shared) oder `WaveInEvent`, nie Loopback/Exclusive | §2.1 |
| 7 | 48/44 kHz → 16 kHz | `MediaFoundationResampler` (Quality 60), nie selbst decimieren | §2.3 |
| 8 | Geraete-Hotplug | `IMMNotificationClient`, Capture sauber stoppen+neu, nie Live-Switch | §2.5 |
| 9 | UI aus Audio-Callback | `Dispatcher.InvokeAsync`, bounded `Channel<T>` DropOldest | §3 |
| 10 | Always-on-Effizienz | KEIN Frame-Filtering vor Streaming-KWS — alle Frames durchreichen | §4 |
| 11 | Self-Trigger bei TTS | Wake-Listener waehrend TTS gaten (AEC nur bei linearer Kette) | §5 |
| 12 | Deployment | RID + `SelfContained` explizit; `PublishTrimmed` bei WPF verboten; Pfade via `AppContext.BaseDirectory` | §6 |
