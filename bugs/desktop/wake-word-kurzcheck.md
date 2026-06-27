# Wake-Word-Detection / Keyword-Spotting in .NET (C#/WPF) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Engine waehlen | sherpa-onnx (Apache-2.0, Wake Word ohne Training); openWakeWord-Pretrained = nicht kommerziell | §13 |
| 2 | Crash beim Nutzer, `DllNotFoundException` | `SetDllDirectory`/`AddDllDirectory`, nie auf System32-`onnxruntime.dll` verlassen | §1 |
| 3 | single-file publish | `CopyLocalLockFileAssemblies` + native DLLs im Output verifizieren | §2 |
| 4 | Nur Meta-NuGet referenziert | Auch `…runtime.win-x64`, gleiche Version | §4 |
| 5 | Wake Word nie erkannt | Audio MUSS 16 kHz mono 16-bit sein → `MediaFoundationResampler` | §6 |
| 6 | Nur erstes Weckwort erkannt | `Reset(stream)` nach jedem Treffer (sonst Dauerfeuer/Stillstand) | §9 |
| 7 | Keywords werden nie erkannt | `while (spotter.IsReady(stream)) Decode(stream)` — nie ein einzelner Decode | §21 |
| 8 | Streaming-KWS erkennt live nicht | KEIN VAD-/Block-Vorfilter vor Streaming-KWS — alle Frames durchreichen | §33 |
| 9 | App weckt sich beim TTS selbst | Wake-Listener waehrend TTS pausieren / AEC | §19 |
| 10 | Custom Wake Word einrichten | Via `text2token`, keywords.txt nur Tokens, KEIN `@original`-Marker | §10, §32 |
| 11 | UI-Update aus Audio-Callback | Via `Dispatcher.InvokeAsync` marshallen | §7 |
| 12 | RID gesetzt, startet nicht beim Nutzer | `<SelfContained>true</SelfContained>` explizit; `PublishTrimmed` bei WPF verboten | §29, §30 |
| 13 | Porcupine genutzt | Free-Tier-Keys enden 30.06.2026; Frame exakt 512 Samples | §15, §17 |
