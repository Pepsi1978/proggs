# Wake-Word / Keyword-Spotting (.NET, C#/WPF) — Best Practices

> Stand: **2026-07-02** · Ziel: **.NET 10** (`net10.0-windows`, WPF) ·
> Engine-Empfehlung: **sherpa-onnx** `org.k2fsa.sherpa.onnx` **1.13.2** (Apache-2.0) ·
> Audio: **NAudio 2.2.1** (NAudio 3 Preview im Blick). Gegenseite (was schiefgeht): `bugs/desktop/wake-word.md`.
>
> Diese Datei sagt **wie man es von vornherein richtig macht**, damit die im Almanach
> dokumentierten Bugs gar nicht erst entstehen. Quellen: Researcher-Schwarm 2026-06-07/08
> (7 parallele Agenten: sherpa-onnx-API, Audio-Pipeline, Effizienz, Testbarkeit, WPF-Threading,
> Deployment, Privacy/UX) + Live-Verifikation. Offizielle Quellen = Grundwahrheit; `extern` = sekundaer.
> Update 2026-07-02: Keine neuen belegten Wake-Word-Regeln seit 2026-06-08; sherpa-onnx bleibt die empfohlene offline/faehige Standard-Engine im bestehenden Setup.

---

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

---

## 0. Engine-Entscheidung (zuerst)

| Kriterium | sherpa-onnx | Porcupine | openWakeWord/NanoWakeWord |
|-----------|-------------|-----------|---------------------------|
| Lizenz (Code) | **Apache-2.0** | Apache-2.0 (Bindings) | Apache-2.0 |
| Lizenz (Modelle) | **Apache-2.0** | proprietaer | **CC-BY-NC-SA** (nicht kommerziell) |
| Custom Wake Word | `text2token`, **kein Training** | Console, Sekunden | **75–90 Min Colab-Training** |
| Kosten/Zukunft | dauerhaft frei | **Free-Tier endet 30.06.2026** | frei (nach Training) |
| Offline | 100 % | 100 % (nach AccessKey-Validierung) | 100 % |
| .NET 10 | ja | ja (4.0.2) | ja (.NET Std 2.0) |

**Empfehlung VoiceAgent:** sherpa-onnx — Apache-2.0 für Code UND Modelle, custom Wake Word ohne Training, 100 % offline, dauerhaft frei. Der echte Offline-Stack (kein Online-Lizenzcheck wie bei Porcupine) ist sogar ein **Privacy-Verkaufsargument** (Quelle: Privacy-Researcher, sherpa-onnx Docs · offiziell).

---

## 1. sherpa-onnx KeywordSpotter — idiomatische C#-Nutzung

### 1.1 Decode-Loop (die korrekte API-Sequenz)
```csharp
// Spotter EINMAL beim App-Start anlegen (teuer, Modelle laden), dann wiederverwenden.
// Pro Mikrofon-Session GENAU EINEN OnlineStream — nicht pro Audio-Block neu erstellen.
stream.AcceptWaveform(actualSampleRate, floatSamples);   // float[-1,1]
while (spotter.IsReady(stream))                            // PFLICHT: while, nicht ein einzelner Decode
    spotter.Decode(stream);
var r = spotter.GetResult(stream);
if (!string.IsNullOrEmpty(r.Keyword)) {
    OnWakeWord(r.Keyword);
    spotter.Reset(stream);                                // PFLICHT nach Treffer, sonst Dauerfeuer
}
```
- **`Decode` nur wenn `IsReady`** (in `while`-Schleife) — sonst werden gepufferte Feature-Frames nicht verarbeitet, Erkennung bleibt leer.
- **`Reset(stream)` nach jedem Treffer** — sonst feuert dasselbe Wake Word mehrfach.
- **Stream persistent halten** — KWS ist ein Streaming-Modell mit linkem Kontext (chunk-16-left-64); ein neuer Stream pro Block zerstört den Kontext.
- Quelle: k2-fsa.github.io/sherpa/onnx/kws · offiziell; Decibri KWS-Integration · extern.

### 1.2 Config (offizielle Defaults)
`OnlineModelConfig`: Transducer encoder/decoder/joiner als **`.int8.onnx`** (12M→4.6M, kaum Genauigkeitsverlust), `Tokens=tokens.txt`, `NumThreads=2`, `Provider="cpu"`. `KeywordSpotterConfig`: `KeywordsFile=keywords.txt`, `KeywordsScore=1.0f`, `KeywordsThreshold=0.25f`, `MaxActivePaths=4`, `FeatConfig.SampleRate=16000`, `FeatureDim=80`. Quelle: KWS Pretrained Models · offiziell.

### 1.3 Custom Wake Word via text2token (PFLICHT)
Das gigaspeech-Modell ist **BPE-basiert** — roher Klartext in `keywords.txt` wird NIE erkannt. Immer:
```
sherpa-onnx-cli text2token --tokens tokens.txt --tokens-type bpe --bpe-model bpe.model keywords_raw.txt keywords.txt
```
Pro Zeile optional `:boost` und `#threshold`, z.B. `OKAY COMPUTER :2.0 #0.3`. Feintuning ohne Modell-Neutraining. **Die fertige keywords.txt enthaelt NUR BPE-Tokens — keinen `@original`-Marker** (sherpa 1.13.2 parst `@...` sonst als Tokens → Init-Fehler, Almanach #32). `result.Keyword` liefert die de-tokenisierte Phrase trotzdem. Quelle: KWS Doku · offiziell; eigener Smoke-Test.

### 1.4 Float-Format & Lifecycle
- PCM16 → float: **`/ 32768f`** (nicht 32767 — sonst mappt −32768 unter −1,0). Erster `AcceptWaveform`-Param = **echte** Aufnahme-Rate (sherpa resampled intern), nicht blind 16000.
- `Spotter`/`Stream` kapseln native Handles → explizit `Dispose()` (in WPF `OnExit`, nicht dem GC überlassen) — sonst Native-Heap-Leak.
- Spotter ist threadsicher zu erstellen, aber jeder Stream gehört genau einem Audio-Thread; `AcceptWaveform`/`Decode` für denselben Stream serialisieren.

---

## 2. Audio-Capture-Pipeline (NAudio)

### 2.1 Capture-Backend
- **`WasapiCapture`** (Shared, event-driven, float32) ist Mark Heaths empfohlener Weg für neue Apps — erlaubt direkte Format-Wahl, evtl. direkt 16 kHz mono (spart Resampling). **Shared Mode** (nie Exclusive — würde das Mikro für andere Apps sperren).
- **`WaveInEvent`** (nicht `WaveIn`) als simpler, latenzarmer Background-Thread-Capture ohne Window-Kontext; liefert 16-bit PCM, aber **keine freie Formatwahl** (Treiber-abhängig, meist nachgelagert resampeln).
- **NIE `WasapiLoopbackCapture`** fürs Mikrofon (das ist System-Audio/Render).
- Zukunft: hinter Interface kapseln für Migration auf **NAudio 3 `WasapiRecorder`** (Span-basiert, allokationsarm — relevant für Dauerschleife).
- Quelle: markheath.net, NAudio Docs · offiziell.

### 2.2 Konvertierung
- PCM16→float: `/32768f`. float→PCM16: **vor `(short)(f*32767f)` `Math.Clamp(f,-1f,1f)`** (sonst Integer-Wrap → Knackser), besser direkt NAudios `SampleToWaveProvider16`/`ToWaveProvider16()`.
- Pipeline intern auf `ISampleProvider` (32-bit float) ausrichten — NAudios definiertes Format für Signalverarbeitung; `WaveBuffer` (Union-View) für allokationsarme Reinterpretation.

### 2.3 Resampling 48/44,1 → 16 kHz mono
- **`MediaFoundationResampler` `{ResamplerQuality=60}`** = transparenteste Qualität (Windows-only), kann Rate+Channels+Bittiefe in einem Schritt. Bei knapper CPU/Latenz Quality ~30.
- **`WdlResamplingSampleProvider(src, 16000)`** = vollständig managed, input-driven, latenzarm — gut für kontinuierliche Streams und Store-Apps. Für 16-kHz-KWS-Input qualitativ ausreichend.
- **Nie selbst decimieren** (Aliasing ohne Tiefpass verschlechtert die Erkennung).

### 2.4 Buffering & feste Frames
`DataAvailable` → `BufferedWaveProvider` (`DiscardOnBufferOverflow=true`, BufferLength großzügig) → Consumer liest. Eigener **Akkumulator-Ring** baut exakt N-Sample-Frames (sherpa flexibel, Porcupine 512, openWakeWord 1280) und behält den Rest — sonst gehen Samples an Frame-Grenzen verloren.

### 2.5 Geräte & Hotplug
- Enumeration: `MMDeviceEnumerator.EnumerateAudioEndPoints(DataFlow.Capture, DeviceState.Active)`; Default-Mic via `GetDefaultAudioEndpoint(DataFlow.Capture, Role.Communications)` (**Communications**, nicht Multimedia).
- Hotplug: eigene **`IMMNotificationClient`** registrieren (`RegisterEndpointNotificationCallback`), bei `OnDefaultDeviceChanged`/`OnDeviceRemoved` die Capture-Instanz **sauber stoppen + neu instanziieren** (kein Live-Switch — WASAPI bindet an festen Endpoint). Init mit Timeout/Try-Catch, nie im UI-Thread.

---

## 3. WPF-Threading & Async (Hintergrund-Listener)

- **UI-Updates** aus dem Audio-Callback via **`Dispatcher.InvokeAsync`** (nicht `Invoke` (blockt/deadlock) und nicht das ältere `BeginInvoke`), gedrosselt auf 50–100 ms (z.B. Pegelanzeige).
- **Frame-Transport** Capture→Inferenz: **`System.Threading.Channels` Channel<T>** bounded mit `BoundedChannelFullMode.DropOldest`, `SingleWriter=true`/`SingleReader=true`; Producer `TryWrite`, Consumer `await ReadAllAsync(ct)`. (`BufferedWaveProvider` nur für Playback-Puffer.)
- **Inferenz-Loop** als `Task.Run` (NICHT `TaskCreationOptions.LongRunning` bei async); ein **dedizierter `Thread`** nur für synchron-blockierende Capture-APIs.
- **Stop** kooperativ mit `CancellationToken`, `OperationCanceledException` erwarten, Shutdown-Timeout; **`IAsyncDisposable`** (Reihenfolge: Capture stoppen → Loop canceln+awaiten → Engine disposen).
- `async void` nur in Event-Handlern (mit try/catch — sonst crasht eine Exception die App); im Engine/Library-Code `ConfigureAwait(false)`; nie `.Result`/`.Wait()` blocken.
- Quelle: Microsoft Learn (WPF threading, Channels, TAP, CancellationToken) · offiziell; Stephen Cleary · extern.

---

## 4. Always-on-Listening — Effizienz

> **⚠️ KRITISCH (Bug #33, eigener Vorfall 2026-06-08):** NIEMALS einen Block-verwerfenden VAD-/
> Energie-Vorfilter VOR ein **Streaming**-KWS-Modell setzen. Das KWS-Modell hat internen Kontext
> (chunk-16-left-64) und braucht KONTINUIERLICHES Audio — verworfene Bloecke zerstueckeln den Stream
> und die Erkennung bricht komplett zusammen (datengetrieben belegt: Batch erkannt, Streaming ohne
> VAD erkannt, Streaming MIT VAD nicht erkannt). **ALLE Frames durchreichen.** Die fruehere Empfehlung
> „Silero-VAD als Vorfilter" war hier FALSCH.

- **KEIN Frame-Filtering vor der Engine.** Alle 16-kHz-Bloecke kontinuierlich an `AcceptWaveform`.
- **Effizienz anders holen:** das int8-3.3M-Modell ist leicht genug fuer Dauerbetrieb. Spar-Hebel ohne
  Stream-Bruch: **ONNX-Runtime-Threading minimal** (`NumThreads`=1–2, Spinning aus), **int8-Modell** (§1.2),
  **Windows EcoQoS / PowerThrottling** fuer den Listener-Thread (`SetProcessInformation`).
- **VAD nur stream-vertraeglich nutzen** (falls ueberhaupt): zum Endpointing / „nach langer Stille die
  Engine schlafen legen" — NIE um einzelne Frames aus dem laufenden Decode-Stream zu nehmen.
- Quelle: eigener Vorfall (Bug #33); onnxruntime.ai (threading), Microsoft EcoQoS · offiziell.

---

## 5. Voice-Assistant-Verhalten & Privacy/UX

- **Sleep/Wake-State**: passives KWS-Lauschen → Wake (+ kurzer Sound/Greeting) → aktive Session → Timeout (60 s Stille) → zurück zu passiv. Im Sleep nur der leichtgewichtige Listener.
- **Self-Trigger bei TTS vermeiden**: pragmatisch den Wake-Listener während eigener TTS-Ausgabe **gaten** (pausieren). Echtes Barge-in nur mit **AEC** (WebRTC/Speex) + **Double-Talk-Detection** — Achtung: AEC funktioniert nur bei **linearer** Wiedergabekette (keine nichtlineare Lautstärke-/Effektverzerrung dazwischen).
- **Threshold-Kalibrierung**: mehrere Wake-Word-Aufnahmen → Scores messen → Threshold automatisch setzen; Ziel **FA < 0,5/h, FR < 5 %**; pro Keyword via `:boost`/`#threshold`.
- **Transparenz**: sichtbarer Mikro-Status ("hört zu" vs. "wach"), Mute-Möglichkeit, klare Aussage dass kein Audio die Maschine verlässt (Offline-Garantie). Branchenstandard: erst NACH Wake aufnehmen/verarbeiten.
- Quelle: Picovoice/openWakeWord Design-Guides, WebRTC/Speex AEC · offiziell + extern.

---

## 6. Modell-Bundling & Deployment

- **NuGet**: nur das Meta-Paket `org.k2fsa.sherpa.onnx` referenzieren — zieht `…runtime.win-x64` automatisch. Version mit Modell zusammen versionieren.
- **RID + SelfContained BEIDE explizit** setzen: seit **.NET 8 impliziert ein `RuntimeIdentifier` NICHT mehr self-contained** — ohne `<SelfContained>true</SelfContained>` liefert man versehentlich framework-dependent aus, die beim Endnutzer nicht startet.
- **Modelle als `Content` + `CopyToOutputDirectory=PreserveNewest`** (NICHT embedded — die native API erwartet Dateipfade).
- **Pfade über `AppContext.BaseDirectory`** auflösen, nie über das Arbeitsverzeichnis; `Assembly.Location` ist in single-file **leer**.
- **`PublishTrimmed` bei WPF strikt verboten** (NETSDK1168, Startup-Crash) — stattdessen `EnableCompressionInSingleFile` für Größe.
- single-file: `CopyLocalLockFileAssemblies` + `IncludeNativeLibrariesForSelfExtract`; native DLLs werden nach `%TEMP%` extrahiert (Pfad-Annahmen vermeiden).
- **DLL-Suchpfad beim Start setzen** (`SetDllDirectory`/`AddDllDirectory`) gegen alte `onnxruntime.dll` in System32. x64 bleiben (x86 cdecl/stdcall-Mismatch).
- Quelle: Microsoft Learn (.NET publish/RID/single-file/trimming), k2-fsa packaging · offiziell.

---

## 7. Testbarkeit & Mockbarkeit

- **Abstraktion + DI**: `IWakeWordEngine` / `IAudioSource` über sherpa-onnx + NAudio legen, Constructor-Injection via `Microsoft.Extensions.DependencyInjection` (im Projekt vorhanden).
- **Hardware-frei testen**: WAV per `WaveFileReader(Stream)` oder synthetische `ISampleProvider`-Buffer in den `AcceptWaveform`-Pfad speisen statt Live-Mikro → deterministische Detection-Tests.
- **`FakeWakeWordEngine`** mit `TriggerWake()` für ViewModel-/UI-Logik-Tests.
- **Konvertierung isoliert** als pure Funktion testen (PCM16→float `/32768f`, little-endian) mit festen Erwartungswerten; Resampling deterministisch via `WdlResamplingSampleProvider` (nicht MediaFoundation — nicht deterministisch genug).
- **Async deterministisch**: `TaskCompletionSource` + `Task.Delay`-Timeout statt `Thread.Sleep`; Timer hinter `ITimer`/`DeterministicTimer`; Tests immer `async Task`.
- **Integration** mit echten Modellen in separater `[Trait("Category","Integration")]`-Suite + Golden-Audio-Samples.
- Quelle: Microsoft Learn (DI, async testing patterns), NAudio (`Mock<IWaveProvider>`) · offiziell.

---

## 8. Observability (Observability-First-Direktive)

- Beim Start einmal Log-Pfad ausgeben; JSON-Lines.
- **Intent-Checkpoint** `kind:CHECKPOINT, step:"Weckwort erkannt", expected/actual` beim Wake-Event — live verifizierbar, dass die Wake-Logik wie gemeint greift.
- Logik-Sonden: erwartete Sample-Rate/Frame-Größe prüfen (Sanity-Check), Threshold-Überschreitungen + VAD-Entscheidungen loggen.

---

## Kopplung zum Bug-Almanach (`bugs/desktop/wake-word.md`)

| Best-Practice-Abschnitt | adressiert Almanach-Bug |
|-------------------------|--------------------------|
| §0 Engine-Entscheidung | 13, 15, 16 |
| §1 KeywordSpotter-API | 9, 10, 11, 12, 21, 22 |
| §2 Audio-Pipeline | 5, 6, 7, 8, 14, 17, 23, 24, 25 |
| §3 WPF-Threading/Async | 7, 26, 27 |
| §4 Always-on-Effizienz | (quer — CPU/Energie) |
| §5 Voice-Assistant/Privacy | 19, 20, 28 |
| §6 Deployment | 1, 2, 3, 4, 29, 30, 31 |
| §7 Testbarkeit | (quer — Qualitätssicherung) |
| §8 Observability | (quer — Früherkennung) |

---

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) ↔ Bug-Almanach [`~/proggs/bugs/desktop/wake-word.md`](../../bugs/desktop/wake-word.md). Links die *Praevention*, rechts die *Falle*.

| Best-Practice-Abschnitt (hier) | Zugehoerige Bug-Almanach-Nummern (`bugs/desktop/wake-word.md`) |
|--------------------------------|----------------------------------------------------------------|
| §0 Engine-Entscheidung | 13, 15, 16 (Lizenz/Engine) |
| §1 KeywordSpotter idiomatisch | 9, 10, 11, 12, 21, 22 (KeywordSpotter-API) |
| §2 Audio-Pipeline | 5, 6, 8, 14, 17, 23, 24, 25 (Audio) |
| §3 WPF-Threading & Async | 7, 26, 27 (Threading/Async) |
| §4 Always-on-Effizienz | CPU/Energie |
| §5 Voice-Assistant & Privacy/UX | 19, 20, 28 (Design/Privacy) |
| §6 Modell-Bundling & Deployment | 1, 2, 3, 4, 29, 30, 31 (Deployment) |
| §7 Testbarkeit | Qualitaetssicherung |
| §8 Observability | alle (Frueherkennung) |
