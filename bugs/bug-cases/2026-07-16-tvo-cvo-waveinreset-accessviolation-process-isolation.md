# TVO/CVO: waveInReset-AccessViolation killt das Overlay — Prozess-Isolation der Aufnahme

## Symptom

TVO (und CVO) stürzten wiederholt ab und wurden vom Watchdog neu gestartet — teils **mitten im
Sprechen** (VU-Meter-Balken eingefroren, dann Neustart), teils **beim/nach dem Stop**. Der gerade
gesprochene Text war jedes Mal verloren; man musste alles neu einsprechen. In der `diag.log` stand
`recording_stop` mit `error:null` — der Crash hinterließ dort also keine Spur.

## Ursache

Windows-Event-Log (Application, ID 1000/1026) zeigte den harten Crash eindeutig:
`0xc0000005` (AccessViolation), Faulting-Offset immer `0x19b2e8`, Stack
`NAudio.Wave.WaveInterop.waveInReset` (beim Stop) bzw. `waveInPrepareHeader` (beim Start/Lauf).
Das ist die bekannte, in .NET **nicht abfangbare** Corrupted-State-Exception der WinMM-`waveIn*`-
Schicht (NAudio #1150/#657/#1084/#1203, alle OPEN — Almanach `bugs/desktop/voice-pipeline.md` §3.2).
Sie killt den GESAMTEN Prozess. Der 2026-07-14-Fix hatte nur den UI-Freeze (Dispatcher) behoben,
nicht den harten Crash. Verstärkt durch einen Almanach-Verstoß: pro Aufnahme wurde eine neue
`WaveInEvent`-Instanz erzeugt und disposed (§3.3 fordert eine langlebige Instanz) — jeder Open/Close
ein Crash-Fenster.

## Fix — Prozess-Isolation der Mikrofon-Aufnahme

- Die gesamte NAudio-Capture läuft in einem **Kindprozess** (`CaptureWorker`, dieselbe EXE,
  gestartet mit `--capture-worker`). Ein `waveIn`-Crash killt nur den Worker; Overlay-UI und
  bereits gesprochener Text überleben. `AudioRecorder` bleibt API-gleich (Start/StopAsync/
  LevelChanged), spricht den Worker per stdin/stdout an.
- **Datenrettung:** Der Worker flusht die WAV bei jedem Buffer (Header-Längen mitgeführt) und
  schließt den Writer **vor** dem crash-anfälligen `WaveIn.Dispose`. Die WAV ist dadurch immer
  valide — der Parent transkribiert sie auch nach einem Worker-Crash. Kein Neu-Einsprechen mehr.
- **SyncContext-Falle:** Der Worker läuft auf dem WPF-Dispatcher-Thread (aus `App.OnStartup`) und
  blockiert ihn. NAudio postet `RecordingStopped` an den beim StartRecording erfassten
  SynchronizationContext (= der blockierte Dispatcher) → das Event feuerte nie → Stop finalisierte
  erst per 3-s-Timeout (spürbare Stop-Latenz). Lösung: `SynchronizationContext.SetSynchronizationContext(null)`
  am Worker-Start → `RecordingStopped` feuert direkt auf dem Capture-Thread.
- **Watchdog gehärtet:** Der Worker trägt denselben EXE-Namen wie das Overlay. Eine `overlay.pid`-
  Datei sorgt dafür, dass der Watchdog nur den echten Overlay adoptiert und nie den kurzlebigen
  Worker (dessen Exit=0 ihn sonst fälschlich stoppen würde).
- **Neue Sonden:** `capture_worker_crashed` (ExitCode), `capture_level_stalled` (VU-Meter
  eingefroren / `DataAvailable` versiegt, §3.1), `workerPid` in `recording_start`.

## Verifikation

- TVO 1.4.87 / CVO 2.1.73 mit `0 Warnungen, 0 Fehler` gebaut (warnaserror).
- Worker der deployten publish-EXE end-to-end: `READY`, 18–32 Pegel-Buffer, `DONE`, valide RIFF-WAV.
- Stop-Latenz: **STOP→Exit 59 ms** (vor dem SyncContext-Fix ~3000 ms, `stop_timeout` im Log).
- `/recording/status` meldete `busy:false`; 2×TVO + 2×CVO laufen, Version per Hash verifiziert.

## Referenz

- Fix-Commits: `f479f1487` (Prozess-Isolation), `e04a4abd8` (SyncContext-Latenzfix)
- Almanach: `bugs/desktop/voice-pipeline.md` §3.2 · Best Practices: `best-practices/desktop/voice-pipeline.md` §3
- Vorgänger: `bugs/bug-cases/2026-07-14-tvo-cvo-naudio-stop-ui-freeze.md` (behob nur den UI-Freeze)
