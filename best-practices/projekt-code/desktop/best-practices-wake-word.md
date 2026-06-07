# Wake-Word / Keyword-Spotting (.NET, C#/WPF) — Best Practices

> Stand: **2026-06-08** · Ziel: **.NET 10** (`net10.0-windows`, WPF) ·
> Engine-Empfehlung: **sherpa-onnx** `org.k2fsa.sherpa.onnx` **1.13.2** (Apache-2.0) ·
> Audio: **NAudio 2.2.1**. Gegenseite (was schiefgeht): `bugs/desktop/wake-word.md`.
>
> Diese Datei sagt **wie man es von vornherein richtig macht**, damit die im Almanach
> dokumentierten Bugs gar nicht erst entstehen. Quelle der Erkenntnisse: Researcher-
> Schwarm-Recherche 2026-06-07/08 (claude-mem #13831–#13845) + Live-Verifikation.

---

## 0. Engine-Entscheidung (zuerst)

| Kriterium | sherpa-onnx | Porcupine | openWakeWord/NanoWakeWord |
|-----------|-------------|-----------|---------------------------|
| Lizenz (Code) | **Apache-2.0** | Apache-2.0 (Bindings) | Apache-2.0 |
| Lizenz (Modelle) | **Apache-2.0** | proprietaer | **CC-BY-NC-SA** (nicht kommerziell) |
| Custom Wake Word | `text2token`, **kein Training** | Console, Sekunden | **75–90 Min Colab-Training** |
| Kosten/Zukunft | dauerhaft frei | **Free-Tier endet 30.06.2026** | frei (nach Training) |
| .NET 10 | ja | ja (4.0.2) | ja (.NET Std 2.0) |

**Empfehlung fuer den VoiceAgent:** sherpa-onnx — Apache-2.0 fuer Code UND Modelle, custom Wake Word ohne Training, 100 % offline, dauerhaft frei.

---

## 1. Native-DLL-Handling sauber aufsetzen (verhindert Almanach-Bug 1–4)

- **Beide Pakete, gleiche Version:** `org.k2fsa.sherpa.onnx` + `org.k2fsa.sherpa.onnx.runtime.win-x64`. Versions-Pinning im `.csproj`, nie auseinanderlaufen lassen.
- **DLL-Suchpfad beim Start explizit setzen**, bevor der erste native Aufruf passiert:
  ```csharp
  // ganz frueh in App.OnStartup, vor jeder sherpa-Nutzung
  var dir = Path.GetDirectoryName(Environment.ProcessPath)!;
  SetDllDirectory(dir);            // P/Invoke kernel32
  // optional haerter: SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_DEFAULT_DIRS) + AddDllDirectory(dir)
  ```
  So gewinnt die mitgelieferte `onnxruntime.dll` gegen eine alte in `System32`.
- **single-file publish:** im `.csproj`
  ```xml
  <CopyLocalLockFileAssemblies>true</CopyLocalLockFileAssemblies>
  <IncludeNativeLibrariesForSelfExtract>true</IncludeNativeLibrariesForSelfExtract>
  ```
  Nach jedem Release-Build verifizieren, dass die nativen DLLs neben/in der EXE liegen.
- **x64 bleiben** (VoiceAgent ist x64) — x86 hat den cdecl/stdcall-Mismatch.

## 2. Audio-Pipeline (verhindert Bug 5–8, 14, 17)

- **Festes Zielformat:** 16 kHz, mono, 16-bit PCM. Mikrofon liefert oft 48 kHz → `MediaFoundationResampler` (Quality ~60) dazwischen, ODER `WaveInEvent` direkt mit `new WaveFormat(16000, 16, 1)`.
- **Ring-Buffer fuer Frame-Groessen:** sherpa ist flexibel, aber wenn man Porcupine (512) oder openWakeWord (1280) je einsetzt, exakt diese Chunk-Groessen liefern. Ring-Buffer statt variabler Frames.
- **Off-UI-Thread:** Audio-Capture + Inferenz im Hintergrund, UI nur via `Dispatcher.BeginInvoke`.
- **Geraete-Hotplug:** `MMDeviceEnumerator` + `IMMNotificationClient`; bei Mic-Wechsel Capture sauber stoppen/neu starten, Init mit Timeout in Try/Catch — nie den UI-Thread blockieren.

## 3. sherpa-onnx KeywordSpotter — Grundgeruest (verhindert Bug 9–12)

- **Custom Wake Word:** Klartext ("Okay Computer") via `text2token` → `keywords.txt`. Pro Zeile optional `:boost`, `#threshold`, `@original`.
- **`Reset` nach jedem Treffer** (C#-Pendant zu `reset_stream()`), sonst feuert die naechste Erkennung nicht.
- **Eigener C#-Mikrofon-Pfad** (kein fertiges Beispiel im dotnet-examples-Repo). Muster:
  ```csharp
  // NAudio DataAvailable -> 16kHz mono floats -> stream.AcceptWaveform(16000, samples)
  // spotter.Decode(stream); var r = spotter.GetResult(stream);
  // if (!string.IsNullOrEmpty(r.Keyword)) { OnWakeWord(r.Keyword); spotter.Reset(stream); }
  ```
- **Modell bundeln:** `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01` als `Content`/`CopyToOutputDirectory=PreserveNewest`.

## 4. Voice-Assistant-Verhalten (verhindert Bug 19/20)

- **Sleep/Wake-State sauber trennen:** Sleeping-Mode = nur leichtgewichtiger WakeWordListener; Wake-Event → Awake-Mode mit voller Transkription, 60 s Timeout, dann zurueck zu Sleep.
- **Self-Trigger vermeiden:** Wake-Listener waehrend TTS-Greeting/Ausgabe pausieren (einfach + robust). Echtes Barge-in nur mit Acoustic Echo Cancellation + Double-Talk-Detection.
- **Kalibrierung anbieten:** mehrere Wake-Word-Aufnahmen → Scores messen → Threshold automatisch setzen. Ziel: FA < 0,5/h, FR < 5 %.

## 5. Observability (Observability-First-Direktive)

- Beim Start einmal Log-Pfad ausgeben; JSON-Lines.
- **Intent-Checkpoint** `kind:CHECKPOINT, step:"Weckwort erkannt", expected/actual` beim Wake-Event — damit live verifizierbar ist, dass die Wake-Logik so greift wie gemeint.
- Logik-Sonden: erwartete Sample-Rate/Frame-Groesse pruefen (Sanity-Check), Threshold-Ueberschreitungen loggen.

---

## Kopplung zum Bug-Almanach (`bugs/desktop/wake-word.md`)

| Best-Practice-Abschnitt | adressiert Almanach-Bug |
|-------------------------|--------------------------|
| §0 Engine-Entscheidung | 13, 15, 16 |
| §1 Native-DLL-Handling | 1, 2, 3, 4 |
| §2 Audio-Pipeline | 5, 6, 7, 8, 14, 17 |
| §3 KeywordSpotter-Geruest | 9, 10, 11, 12 |
| §4 Voice-Assistant-Verhalten | 19, 20 |
| §5 Observability | (quer — Frueherkennung aller) |
