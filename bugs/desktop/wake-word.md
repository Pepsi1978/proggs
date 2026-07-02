# Bekannte Bugs: Wake-Word-Detection / Keyword-Spotting in .NET (C#/WPF)

> **PFLICHT-LESEN vor Arbeit an Wake-Word / Keyword-Spotting in einer .NET-Desktop-App.**
> Stand: zuletzt recherchiert am **2026-06-08**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax; keine neuen belastbaren Bugs/Deprecations seit dem Stichtag) fuer
> **.NET 10.0.204** (`net10.0-windows`, WPF) · **sherpa-onnx** NuGet `org.k2fsa.sherpa.onnx` **1.13.2** (14.05.2026, Apache-2.0) ·
> **NAudio 2.2.1** · **Picovoice Porcupine .NET 4.0.2** · **NanoWakeWord** (.NET Standard 2.0).
> Zielprojekt: **VoiceAgent** (`~/proggs/VoiceAgent`). Gegenseite (Praevention):
> `best-practices/desktop/wake-word.md`.

> **Update 2026-07-02:** Re-Recherche fand keine neuen belastbaren .NET/NAudio/sherpa-onnx/Porcupine-Deprecations oder Bugs seit 2026-06-08. Die bestehenden Deployment-, Audioformat-, Streaming-KWS- und TTS-Self-Trigger-Regeln bleiben unveraendert massgeblich.

---

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

---

## A. Deployment / native DLLs (sherpa-onnx, .NET/WPF) — die teuerste Fehlerklasse

### 1. DllNotFoundException durch aelteres `onnxruntime.dll` in System32   [⭐ HAEUFIG]
**Symptom:** Laeuft auf dem Dev-Rechner, crasht beim Nutzer mit `DllNotFoundException` oder einem ONNX-Versions-/Entry-Point-Fehler — obwohl die DLL "daneben liegt".
**Ursache:** Windows-DLL-Suchpriorität laedt eine aeltere/konfligierende `onnxruntime.dll` aus `C:\Windows\System32` BEVOR die mitgelieferte gefunden wird (oft von anderer Software dort abgelegt).
**Versionen:** sherpa-onnx alle (1.13.2 betroffen) — Windows-Plattformverhalten, **per Design, kein Fix**.
**FIX:** Beim App-Start den eigenen DLL-Ordner explizit voranstellen: `SetDllDirectory(appDir)` bzw. `AddDllDirectory` + `SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_*)`. Native DLLs in einen kontrollierten Unterordner legen und diesen registrieren. NIE auf den System-Suchpfad verlassen. Siehe `best-practices-wake-word.md` §1.
**Quelle:** k2-fsa/sherpa-onnx GitHub-Issues (Windows-DLL-loading); claude-mem #13843/#13845.

### 2. Native DLLs fehlen im single-file publish
**Symptom:** `dotnet publish -p:PublishSingleFile=true` erzeugt eine EXE, die beim Start die nativen sherpa/onnx-DLLs nicht findet.
**Ursache:** Native Libraries werden NICHT automatisch in die single-file-EXE eingebettet/extrahiert; sie fallen aus dem Publish-Output.
**Versionen:** sherpa-onnx alle — per Design der .NET-single-file-Mechanik.
**FIX:** `<CopyLocalLockFileAssemblies>true</CopyLocalLockFileAssemblies>` setzen und/oder `IncludeNativeLibrariesForSelfExtract=true`; native DLLs als `Content`/`None` mit `CopyToOutputDirectory=PreserveNewest` mitgeben und neben die EXE deployen. Nach jedem Release-Build verifizieren, dass die DLLs im Output liegen.
**Quelle:** claude-mem #13843; .NET-Doku single-file native libs.

### 3. x86 Calling-Convention-Mismatch (stdcall vs. cdecl)
**Symptom:** Auf **x86**-Builds Laufzeitfehler/Stack-Korruption beim ersten nativen Aufruf; auf x64 unauffaellig.
**Ursache:** Mismatch zwischen managed P/Invoke-Annahme (stdcall) und der nativen C-API (cdecl) auf x86.
**Versionen:** sherpa-onnx x86 — historisch; x64 nicht betroffen.
**FIX:** **win-x64 verwenden** (Standard fuer den VoiceAgent — `net10.0-windows`, x64). x86 meiden; falls unvermeidbar, korrektes `CallingConvention.Cdecl` sicherstellen.
**Quelle:** k2-fsa GitHub-Issues; claude-mem #13843.

### 4. Plattform-Runtime-Paket separat noetig
**Symptom:** Nur `org.k2fsa.sherpa.onnx` referenziert → keine nativen Binaries, Ladefehler.
**Ursache:** Das Haupt-NuGet-Paket enthaelt die nativen Windows-Binaries NICHT — die liegen im Runtime-Paket.
**Versionen:** alle.
**FIX:** Zusaetzlich `org.k2fsa.sherpa.onnx.runtime.win-x64` referenzieren (passend zur RID). Versionen von Haupt- und Runtime-Paket **identisch** halten (Mismatch → Entry-Point-Fehler).
**Quelle:** claude-mem #13843/#13840.

---

## B. Audio-Capture (NAudio) — Format, Threading, Geraete

### 5. NAudio `WaveInEvent` blockiert bei USB-Mikrofon-Abzug   [⭐ HAEUFIG]
**Symptom:** App haengt/blockiert (z. B. an `DeviceCount`/Init), wenn das USB-Mikrofon abgezogen oder umgesteckt wird.
**Ursache:** Windows-WaveIn-API blockiert bei Geraetewechsel; kein sauberer Abbruch — bekannte NAudio/Win-API-Limitation.
**Versionen:** NAudio 2.2.1 — Win-API-bedingt, kein einfacher Fix.
**FIX:** Geraete-Hotplug ueber `MMDeviceEnumerator`/`IMMNotificationClient` ueberwachen, Capture bei Geraetewechsel sauber stoppen/neu starten; Init in Try/Catch mit Timeout, NICHT im UI-Thread blockieren. (Erinnert an Memory `reference_chrome_mic_silent_after_usb_change` — gleiche Geraete-Klasse.)
**Quelle:** NAudio-Issues; claude-mem #13845.

### 6. Sample-Rate-Mismatch: NAudio liefert 48 kHz, Engine will 16 kHz
**Symptom:** Wake-Word wird nie erkannt, obwohl Audio ankommt; Erkennungsrate praktisch 0.
**Ursache:** Mikrofon laeuft mit 44,1/48 kHz, die KWS-Engine erwartet zwingend 16 kHz mono. Ungesampletes Audio = Garbage fuer das Modell.
**Versionen:** unabhaengig.
**FIX:** `MediaFoundationResampler` (Quality 1–60, ~60 = beste) 48 kHz → 16 kHz mono 16-bit, ODER `WaveInEvent` direkt mit `WaveFormat(16000, 16, 1)` initialisieren, wenn das Geraet es unterstuetzt. Resampling VOR der Frame-Zerlegung.
**Quelle:** claude-mem #13845/#13839.

### 7. Cross-Thread-UI-Zugriff aus `DataAvailable`
**Symptom:** `InvalidOperationException` (UI-Thread) oder sporadische Crashes beim UI-Update aus dem Audio-Callback.
**Ursache:** `DataAvailable`/Capture laeuft auf einem eigenen Thread; direkter Zugriff auf WPF-UI ist verboten.
**Versionen:** unabhaengig (WPF).
**FIX:** UI-Updates ueber `Dispatcher.Invoke/BeginInvoke` marshallen. Audio-Verarbeitung off-UI-thread halten, nur Ergebnis-Events auf den Dispatcher.
**Quelle:** claude-mem #13845; WPF-Threading.

### 8. PvRecorder vs. NAudio (nur bei Picovoice relevant)
**Symptom:** Frame-Drift/falsche Frame-Groessen bei Porcupine, wenn man NAudio direkt durchreicht.
**Ursache:** Picovoice empfiehlt offiziell **PvRecorder** fuer die Mikrofon-Erfassung; NAudio geht, braucht aber manuelles Frame-Buffering auf exakt 512 Samples.
**Versionen:** Porcupine .NET 4.0.2.
**FIX:** Bei Porcupine PvRecorder nutzen ODER mit NAudio `WaveFormat(16000,16,1)` + eigenem Ring-Buffer exakt 512-Sample-Frames bilden. (Beim VoiceAgent ohnehin sherpa-onnx → entfaellt.)
**Quelle:** Picovoice-Doku; claude-mem #13845/#13839.

---

## C. sherpa-onnx KeywordSpotter — API-Fallen

### 9. `reset_stream()` Pflicht nach jeder Erkennung   [⭐ HAEUFIG]
**Symptom:** Erstes Weckwort wird erkannt, danach keine weiteren Erkennungen mehr (oder erst nach langer Pause).
**Ursache:** Der Keyword-Stream muss nach einem Treffer explizit zurueckgesetzt werden, sonst bleibt der interne Decoder-Zustand "verbraucht".
**Versionen:** sherpa-onnx alle (1.13.2).
**FIX:** Direkt nach `OnWakeWord`/Treffer `spotter.Reset(stream)` (C#-Aequivalent zu `reset_stream()`) aufrufen, bevor weiter gefuettert wird.
**Quelle:** sherpa-onnx Python/C#-Beispiele; claude-mem #13845.

### 10. `keywords.txt`-Format mit Sonder-Prefixen
**Symptom:** Custom Wake Word wird nie/kaum erkannt oder feuert staendig (zu viele False Positives).
**Ursache:** Das `keywords.txt`-Format hat Steuer-Syntax, die leicht falsch geschrieben wird: `:boost`, `#threshold`, `@original-phrase`. Falsche Tokenisierung (ohne `text2token`) → unbrauchbare Phoneme.
**Versionen:** sherpa-onnx alle.
**FIX:** Keywords mit `text2token` (oder sentencepiece + bpe.model) aus dem Klartext erzeugen. Pro Zeile NUR die BPE-Tokens, optional `:boost` (Erkennungs-Boost) und `#threshold` (Schwelle). **ACHTUNG: keinen `@original`-Marker anhaengen** — siehe Bug 32 (fuehrt mit gigaspeech-3.3M zum Init-Fehler). Threshold pro Wake Word kalibrieren (siehe Bug 20).
**Quelle:** sherpa-onnx KWS-Doku; claude-mem #13845.

### 11. Modellwahl + custom Wake Word ohne Retraining
**Symptom:** Unklar, wie "Okay Computer" ohne 90-Min-Training geht.
**Ursache:** Wissensluecke — sherpa-onnx kann open-vocabulary KWS.
**Versionen:** Modell `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01` (3,3 M Parameter, EN).
**FIX:** Modell bundeln (`CopyToOutputDirectory`), Wake Word via `text2token` in Tokens wandeln — **kein** Modell-Retraining noetig. 3,3 M Parameter = geringer CPU/RAM-Verbrauch, 100 % offline.
**Quelle:** claude-mem #13837/#13840.

### 12. dotnet-examples ohne Mikrofon-KWS-Beispiel in C#
**Symptom:** Man sucht im `dotnet-examples`-Repo ein `keyword-spotting-from-microphone`-C#-Beispiel und findet keins.
**Ursache:** Es gibt nur Python-`KeywordSpotter`-Beispiele; der C#-Mikrofon-Pfad muss selbst gebaut werden (NAudio → Frames → `AcceptWaveform`).
**Versionen:** Stand 2026-06 weiterhin so.
**FIX:** Python-Beispiel als Vorlage nehmen, C#-Wrapper selbst schreiben (siehe `best-practices-wake-word.md` §3 fuer das Grundgeruest `WakeWordListener`).
**Quelle:** claude-mem #13836.

---

## D. openWakeWord / NanoWakeWord — Lizenz & Pipeline

### 13. Pretrained-Modelle sind CC-BY-NC-SA 4.0 (nicht kommerziell)   [⭐ HAEUFIG/RECHTLICH]
**Symptom:** Man baut auf `alexa`/`hey_jarvis`/`hey_marvin`/`hey_mycroft` und merkt zu spaet, dass es nicht kommerziell nutzbar ist.
**Ursache:** Der openWakeWord-**Code** ist Apache-2.0, aber die **vortrainierten Modelle** sind CC-BY-NC-SA 4.0 (unklare Trainingsdaten-Lizenz).
**Versionen:** openWakeWord/NanoWakeWord aktuell.
**FIX:** Fuer kommerzielle/eigene Wake Words eigenes Modell trainieren (Colab, 75–90 Min, synthetische TTS-Daten → Apache-2.0-Ergebnis) ODER **HeyBuddy** (Apache-2.0 fuer Code UND Modelle) nutzen. Fuer den VoiceAgent ist sherpa-onnx der einfachere Weg (kein Training).
**Quelle:** claude-mem #13836/#13839.

### 14. openWakeWord-Chunk-Groesse 1280 Samples non-negotiable
**Symptom:** Erkennung schlaegt fehl, obwohl Audio 16 kHz mono ist.
**Ursache:** Die `melspectrogram.onnx`-Vorverarbeitung erwartet **exakt 1280 Samples (80 ms @ 16 kHz)** pro Chunk; abweichende Groessen brechen die 3-Stufen-Pipeline (melspectrogram → Feature-Extractor → Classifier).
**Versionen:** openWakeWord/NanoWakeWord alle.
**FIX:** Ring-Buffer auf exakt 1280-Sample-Chunks; NanoWakeWord uebernimmt das Preprocessing, wenn man korrekt fuettert.
**Quelle:** claude-mem #13845/#13839.

---

## E. Picovoice Porcupine — Lizenz-Deadline & Audio

### 15. Free-Tier-AccessKeys werden am 30.06.2026 abgeschaltet   [⭐ ZEITKRITISCH]
**Symptom:** Bestehende Free-Tier-`AccessKey`s funktionieren nach dem 30.06.2026 nicht mehr; App faellt aus.
**Ursache:** Picovoice stellt den Free-Tier ein (Umstieg auf 7-Tage-Trial fuer Teams).
**Versionen:** alle Porcupine-Free-Tier-Keys. **Verifiziert 2026-06-08** (Picovoice-Mails, HN, Home-Assistant-Community).
**FIX:** Fuer dauerhafte, kostenlose, kommerziell nutzbare Loesung → **sherpa-onnx** (Apache-2.0) statt Porcupine. Wer bei Porcupine bleibt, braucht ab dann einen bezahlten Plan.
**Quelle:** https://community.home-assistant.io/t/fyi-picovoice-confirmed-free-tier-accesskeys-will-stop-working-after-june-30-2026/1012744 · https://news.ycombinator.com/item?id=48248969

### 16. Free-Tier-Limit: 3 aktive Nutzer/Monat
**Symptom:** App funktioniert beim Entwickler, scheitert beim 4. Nutzer.
**Ursache:** Free-Tier erlaubt nur 3 aktive Nutzer pro Monat (30-Tage-Reset); naechste Stufe ~$899/Monat.
**Versionen:** bis 30.06.2026 (danach siehe Bug 15).
**FIX:** Fuer >3 Nutzer kein Free-Tier — Engine wechseln (sherpa-onnx) oder bezahlen.
**Quelle:** claude-mem #13834/#13839.

### 17. Porcupine-Audioformat exakt 512 Samples/Frame
**Symptom:** `invalid frame length`-Fehler.
**Ursache:** `Porcupine.FrameLength` ist **exakt 512 Samples**, 16 kHz mono 16-bit PCM (`short[]`); Abweichung wirft.
**Versionen:** Porcupine .NET 4.0.2.
**FIX:** Audio in exakt 512-Sample-Frames buffern (PvRecorder macht das; mit NAudio selbst ring-buffern). Niemals Roh-Frames variabler Laenge durchreichen.
**Quelle:** claude-mem #13834/#13843.

### 18. `.ppn`-Modelle sind plattformspezifisch
**Symptom:** Auf Windows trainiertes `.ppn` laeuft nicht auf anderer Plattform/Arch.
**Ursache:** Custom-Wake-Word-Dateien werden plattform-/arch-spezifisch erzeugt.
**Versionen:** alle.
**FIX:** Pro Zielplattform das passende `.ppn` erzeugen/bundlen. (Beim reinen Windows-VoiceAgent unkritisch.)
**Quelle:** claude-mem #13839/#13840.

---

## F. Voice-Assistant-Design (engine-uebergreifend)

### 19. Self-Trigger waehrend TTS-Ausgabe   [⭐ HAEUFIG]
**Symptom:** Die App weckt sich selbst, wenn sie das Weckwort vorliest oder darueber spricht; Endlos-/Doppel-Trigger.
**Ursache:** Das Mikrofon hoert die eigene Lautsprecher-Ausgabe (kein akustischer Echo-Schutz).
**Versionen:** unabhaengig.
**FIX:** Wake-Listener waehrend aktiver TTS-Ausgabe pausieren (einfachster, robuster Weg) ODER Acoustic Echo Cancellation + Double-Talk-Detection fuer echtes Barge-in. Fuer den VoiceAgent: Sleeping-Mode pausiert Transkription, beim TTS-Greeting den KWS kurz stummschalten.
**Quelle:** claude-mem #13845.

### 20. Threshold-Tuning / Kalibrierung
**Symptom:** Entweder reagiert das Weckwort kaum (zu hoch) oder staendig auf Zufallsgeraeusche (zu niedrig).
**Ursache:** Default-Threshold passt selten zu Stimme/Mikro/Raum.
**Versionen:** unabhaengig.
**FIX:** Kalibrierungs-Phase: mehrere Wake-Word-Aufnahmen → Recognition-Scores messen → Threshold automatisch setzen. Zielmetrik: **False-Accept < 0,5/Stunde, False-Reject < 5 %**. Pro Wake Word in `keywords.txt` via `#threshold` feinjustieren.
**Quelle:** claude-mem #13845/#13834.

---

## G. Ergaenzungen aus der Best-Practices-Recherche (2026-06-08)

> Diese 11 Eintraege kamen aus dem Best-Practices-Researcher-Schwarm (sherpa-onnx-API,
> Audio-Pipeline, WPF-Threading, .NET-Deployment). Sie ergaenzen A–F.

### 21. `Decode` ohne `IsReady`-while-Schleife   [⭐ HAEUFIG]
**Symptom:** Keywords werden nie erkannt / `GetResult` leer trotz korrektem Audio.
**Ursache:** `Decode` wird nur einmal statt in `while (spotter.IsReady(stream))` aufgerufen — gepufferte Feature-Frames bleiben unverarbeitet.
**Versionen:** sherpa-onnx alle (1.13.2). API-Vertrag.
**FIX:** Immer `while (spotter.IsReady(stream)) spotter.Decode(stream);` — nie ein einzelner Decode-Call. Siehe `best-practices-wake-word.md` §1.1.
**Quelle:** k2-fsa.github.io/sherpa/onnx/kws · offiziell.

### 22. Hartkodierte `16000` im `AcceptWaveform`-Sample-Rate-Parameter
**Symptom:** Schlechte/keine Erkennung, wenn das Mikro nicht nativ 16 kHz liefert.
**Ursache:** Erster `AcceptWaveform`-Parameter ist die TATSAECHLICHE Input-Rate; hartkodiertes `16000` bei 44,1/48-kHz-Input verhindert das korrekte interne Resampling.
**Versionen:** sherpa-onnx alle. (Verwandt mit Bug 6, aber API-Parameter-Ebene.)
**FIX:** Echte Aufnahme-Rate uebergeben (sherpa resampled selbst) ODER vorher sauber auf 16 kHz mono resampeln und dann 16000 uebergeben — konsistent halten.
**Quelle:** sherpa-onnx Go/JS-Binding-Semantik · offiziell.

### 23. `float → PCM16` ohne Clamp (Integer-Wrap)
**Symptom:** Hoerbare Knackser/Verzerrung bei lauten Passagen.
**Ursache:** Float-Samples ausserhalb [-1,1] erzeugen bei `(short)(f*32767f)` Integer-Wrap-Around (positiv → negativ).
**Versionen:** unabhaengig.
**FIX:** Vor dem Cast `Math.Clamp(f,-1f,1f)`, besser NAudios `SampleToWaveProvider16`/`ToWaveProvider16()`. Siehe §2.2.
**Quelle:** markheath.net/post/convert-16-bit-pcm-to-ieee-float · offiziell.

### 24. `PCM16 → float` mit Divisor 32767 statt 32768
**Symptom:** Negativster Sample (−32768) mappt knapp unter −1,0 → loest nachgelagertes Clipping aus.
**Ursache:** 16-bit-Range ist asymmetrisch [−32768, +32767]; Divisor 32767 ueberschreitet −1,0.
**Versionen:** unabhaengig.
**FIX:** Immer durch **32768f** teilen.
**Quelle:** markheath.net/post/convert-16-bit-pcm-to-ieee-float · offiziell.

### 25. WASAPI Shared Mode verschleiert das echte Geraeteformat
**Symptom:** Capture liefert eine andere Sample-Rate als das physische Geraet.
**Ursache:** WASAPI Shared Mode macht automatische Sample-Rate-Conversion; das gemeldete Mix-Format ist nicht die Hardware-Rate.
**Versionen:** unabhaengig (Windows WASAPI).
**FIX:** Capture-WaveFormat explizit pruefen/setzen und gezielt auf 16 kHz mono resampeln, statt das gemeldete Format als Wahrheit zu nehmen.
**Quelle:** markheath.net/post/wasapi-sample-rate-conversion · offiziell.

### 26. `async void` ohne try/catch crasht die ganze App
**Symptom:** Unerwarteter App-Crash aus einem Audio-/Wake-Event heraus.
**Ursache:** Eine Exception in einer `async void`-Methode (Event-Handler) wird nicht gefangen und reisst den Prozess mit.
**Versionen:** unabhaengig (.NET TAP).
**FIX:** `async void` NUR fuer Event-Handler, dort immer try/catch. Sonst `async Task`. Im Engine-Code `ConfigureAwait(false)`.
**Quelle:** Microsoft Learn (async best practices); Stephen Cleary · offiziell + extern.

### 27. Unbounded Channel / `Dispatcher.Invoke` blockiert oder leakt
**Symptom:** Speicher waechst unbegrenzt ODER UI/Audio-Loop blockiert/deadlockt.
**Ursache:** Unbounded `Channel<T>` als Frame-Puffer laeuft voll (Consumer langsamer als Producer); synchrones `Dispatcher.Invoke` aus dem Audio-Thread blockiert.
**Versionen:** unabhaengig (WPF/.NET).
**FIX:** Bounded `Channel<T>` mit `BoundedChannelFullMode.DropOldest`, `SingleWriter/SingleReader=true`; UI-Updates via `Dispatcher.InvokeAsync` (gedrosselt). Siehe §3.
**Quelle:** Microsoft Learn (System.Threading.Channels, WPF threading) · offiziell.

### 28. AEC funktioniert nur bei linearer Wiedergabekette
**Symptom:** Acoustic Echo Cancellation gegen Self-Trigger wirkt nicht, App weckt sich beim TTS trotzdem selbst.
**Ursache:** AEC/Double-Talk-Detection setzt eine LINEARE Referenz voraus; nichtlineare Verzerrung (dynamische Lautstaerke, Effekte) zwischen TTS und Lautsprecher macht die Echo-Schaetzung unbrauchbar.
**Versionen:** unabhaengig (AEC-Prinzip).
**FIX:** Wiedergabekette linear halten ODER pragmatisch den Wake-Listener waehrend TTS gaten (pausieren). Siehe §5 + Bug 19.
**Quelle:** WebRTC/Speex AEC-Doku · offiziell.

### 29. .NET 8+: `RuntimeIdentifier` impliziert NICHT mehr self-contained   [⭐ DEPLOYMENT]
**Symptom:** Build mit RID laeuft beim Entwickler, startet beim Endnutzer nicht (fehlende Runtime).
**Ursache:** Breaking Change ab .NET 8 — ein gesetztes RID erzeugt NICHT mehr automatisch ein self-contained Publish.
**Versionen:** .NET 8, 9, 10.
**FIX:** `<SelfContained>true</SelfContained>` explizit setzen, wenn eine eigenstaendige EXE gewollt ist. Siehe §6.
**Quelle:** Microsoft Learn (.NET 8 breaking changes, publish) · offiziell.

### 30. `PublishTrimmed` crasht WPF beim Start (NETSDK1168)
**Symptom:** Getrimmte WPF-App startet nicht / Crash beim Start.
**Ursache:** WPF ist nicht trim-kompatibel (Reflection); `PublishTrimmed` entfernt benoetigte Typen, Build warnt mit NETSDK1168.
**Versionen:** WPF auf .NET 8–10.
**FIX:** `PublishTrimmed` bei WPF NICHT verwenden; fuer Groessenreduktion `EnableCompressionInSingleFile`. Siehe §6.
**Quelle:** Microsoft Learn (trimming incompatibilities), dotnet/wpf-Issues · offiziell.

### 31. `Assembly.Location` ist in single-file leer
**Symptom:** Modell-/Datei-Pfade werden zur Laufzeit nicht gefunden, sobald single-file publiziert wird.
**Ursache:** In single-file-Builds ist `Assembly.Location` leer; Pfad-Aufloesung darueber (oder ueber das Arbeitsverzeichnis) schlaegt fehl.
**Versionen:** .NET single-file (8–10).
**FIX:** Modell-/Asset-Pfade ueber **`AppContext.BaseDirectory`** aufloesen. Siehe §6.
**Quelle:** Microsoft Learn (single-file deployment) · offiziell.

### 32. `@original`-Marker in keywords.txt wird als Tokens fehlinterpretiert   [⭐ EIGENER VORFALL]
**Symptom:** KeywordSpotter-Init schlaegt fehl: `Cannot find ID for token COMPUTER at line: ▁OKAY ▁COMP U TER @OKAY COMPUTER` → `Encode keywords failed`.
**Ursache:** Die `@original-phrase`-Syntax (aus aelterer/anderer Doku) wird von sherpa-onnx 1.13.2 mit dem gigaspeech-3.3M-Modell NICHT als Anzeige-Marker, sondern als zusaetzliche **Tokens** geparst — die Klartext-Woerter (`@OKAY`, `COMPUTER`) stehen nicht in tokens.txt → Init bricht ab.
**Versionen:** verifiziert sherpa-onnx 1.13.2 + sherpa-onnx-kws-zipformer-gigaspeech-3.3M (eigener Smoke-Test 2026-06-08).
**FIX:** keywords.txt enthaelt pro Zeile NUR die BPE-Tokens (`▁OKAY ▁COMP U TER`), optional `:boost`/`#threshold`. KEIN `@...`. `result.Keyword` liefert trotzdem die de-tokenisierte Phrase ("OKAY COMPUTER", `▁` → Leerzeichen). Siehe `best-practices-wake-word.md` §1.3.
**Quelle:** Eigener Vorfall — durch Engine-Smoke-Test VOR der Integration gefunden (Observability/Verifikation).

### 33. VAD-Vorfilter VOR dem Streaming-KWS zerstoert die Erkennung   [⭐⭐ EIGENER VORFALL, KRITISCH]
**Symptom:** Im Live-Betrieb wird das Weckwort NIE erkannt, obwohl dieselbe Aufnahme als Batch-WAV sauber erkannt wird. Diagnose-Log: „Sprache gehoert, aber Weckwort nicht erkannt".
**Ursache:** Ein VAD-/Energie-Vorfilter, der einzelne (leise) Audio-Bloecke VERWIRFT, bevor sie an die KWS-Engine gehen. Das KWS-Modell ist ein STREAMING-Modell mit internem Kontext (chunk-16-left-64) und braucht KONTINUIERLICHES Audio. Verworfene Bloecke (Pausen, leise Silben in „Okay Computer", Wort-Anlaut) zerstueckeln den Stream → die Erkennung bricht komplett zusammen.
**Versionen:** sherpa-onnx KWS alle — prinzipbedingt (gilt fuer JEDES Streaming-KWS). Eigener Vorfall sherpa 1.13.2.
**Datengetriebener Beweis (2026-06-08):** dieselbe verifizierte „Okay Computer"-WAV: (A) ganze WAV am Stueck → ERKANNT; (B) Live-Bloecke 100ms OHNE VAD → ERKANNT; (C) Live-Bloecke 100ms MIT VAD (Schwelle 0.012) → NICHT erkannt (9 Bloecke verworfen).
**FIX:** KEIN Block-verwerfender Vorfilter vor einem Streaming-KWS. ALLE Audio-Frames kontinuierlich an die Engine geben. Ein VAD darf hoechstens fuer Endpointing/Schlaf-nach-Stille genutzt werden — NIEMALS um einzelne Frames aus dem Engine-Stream zu nehmen. (Effizienz: das int8-3.3M-Modell ist leicht genug fuer Dauerbetrieb; VAD-Sparen lohnt den Erkennungsverlust nicht.) Siehe `best-practices-wake-word.md` §4.
**Quelle:** Eigener Vorfall — kostete mehrere Fehlversuche, weil mein eigener Best-Practices-Eintrag den VAD-Vorfilter faelschlich empfahl. Lehre: VAD-vor-Streaming-KWS ist ein bekanntes Anti-Pattern.

### 34. Weckwort-Aenderung in den Einstellungen wirkungslos — `keywords.txt` bleibt fest   [⭐ EIGENER VORFALL]
**Symptom:** Der Nutzer aendert das Weckwort in den App-Einstellungen (z.B. von "Okay Computer" auf "Computer"), aber die Erkennung hoert weiter auf das ALTE Wort. Das neue Wort weckt nie.
**Ursache:** Das KWS-Modell erkennt **BPE-Tokens** aus `keywords.txt` (z.B. `▁OKAY ▁COMP U TER`), NICHT den Klartext. Das `WakeWord`-Setting war nur Anzeige-/Greeting-Text; die `keywords.txt` blieb fest die gebundelte Datei. Aendern des Settings-Texts beruehrt die Tokens nicht → Erkennung unveraendert.
**Versionen:** sherpa-onnx KWS alle — prinzipbedingt (Tokens != Klartext, vgl. #10). Eigener Vorfall VoiceAgent 2026-06-08.
**FIX (funktionserhaltend):** Beim Setzen des Weckworts die `keywords.txt` aus dem gewaehlten Wort NEU erzeugen (tokenisiert) und die Engine damit neu laden. Es gibt KEINE Laufzeit-`text2token`-Funktion in der sherpa C-API. Zwei Stufen: (1) gaengige Weckwoerter EINMAL vorab tokenisieren (Python sentencepiece) → kuratierte Auswahlliste; (2) FREIE Texteingabe → eigener schlanker Encoder. Beim Speichern die fertige `keywords.txt` (nur Tokens, KEIN `@original` — #32) in einen BESCHREIBBAREN Pfad (`%LOCALAPPDATA%`) schreiben und der Engine via `KeywordsFile` uebergeben.
**FREIE EINGABE GELOEST (VoiceAgent #46651, 2026-06-08):** `Microsoft.ML.Tokenizers` ist NICHT noetig (und war der falsche Verdacht). Das `bpe.model` ist trotz Namens ein **UNIGRAM**-Modell (`model_type=1`, 500 Pieces, `byte_fallback=False`, `add_dummy_prefix=True`) — KEIN BPE. Loesung ohne neue NuGet-Abhaengigkeit: das Vokabular (Piece+Log-Score) EINMAL aus `bpe.model` nach `unigram-vocab.txt` extrahieren (Scores kommen damit von sentencepiece selbst), zur Laufzeit ein ~80-Zeilen **Unigram-Viterbi** (`Core/SentencePieceUnigram.cs`) den Best-Path tokenisieren lassen. KRITISCH: das Modell ist auf GROSSSCHREIBUNG trainiert ("Computer" → `▁C omputer`, aber "COMPUTER" → `▁COMP U TER`) → Input vor dem Tokenisieren zu Uppercase normalisieren. OOV-Zeichen (Umlaute) → null → in der UI ablehnen. Verifiziert: C#-Encoder == Python-sentencepiece fuer 9 kuratierte + 9 freie Woerter (`SentencePieceUnigramTests`, 39 Tests gruen).
**Quelle:** Eigener Vorfall (VoiceAgent #46648/#46651) — Token-Pipeline per Log, erzeugter Datei und Unit-Test gegen sentencepiece verifiziert.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| sherpa-onnx NuGet aktuell? | **1.13.2 ist die aktuellste** (14.05.2026, Apache-2.0) — verifiziert via nuget.org | Bug 1–4, 9–12 |
| Picovoice Free-Tier | **Wird 30.06.2026 abgeschaltet** — verifiziert (HN + Home Assistant) | Bug 15/16 |

**Noch NICHT "gefixt" (Workaround bleibt aktiv — per Design / Plattformverhalten):**
- Bug 1 (System32-DLL-Prioritaet), Bug 2 (single-file native DLLs), Bug 4 (Runtime-Paket): Windows-/.NET-Mechanik, kein Fix erwartet → Workarounds dauerhaft.
- Bug 5 (NAudio USB-Hotplug): Win-API-Limitation.
- Bug 6/14/17 (feste Sample-/Frame-Groessen): per Design der Modelle.
- Bug 9 (`reset_stream`), Bug 10 (`keywords.txt`-Syntax): API-Vertrag, kein Bug im engeren Sinn.
- Bug 13 (CC-BY-NC-SA-Modelle): Lizenz, aendert sich nicht.
- Bug 19/20 (Self-Trigger/Threshold): inhaerente KWS-Designthemen.

**Ehrlichkeits-Hinweis:** Die zwei zeitkritischen Fakten (NuGet-Version, Picovoice-Deadline) sind live verifiziert. Die uebrigen Eintraege stammen aus der Researcher-Schwarm-Recherche der Vor-Session (claude-mem #13831–#13845) und sind als Deployment-/API-/Lizenz-Gotchas dokumentiert, nicht als "in Version X gefixt" — sie bleiben gueltig.

---

## Kopplung zur Best-Practices-Gegenseite (`best-practices/desktop/wake-word.md`)

| Almanach-Bug | Best-Practice-Abschnitt (Praevention) |
|--------------|----------------------------------------|
| 13, 15, 16 (Lizenz/Engine) | §0 Engine-Entscheidung |
| 9, 10, 11, 12, 21, 22 (KeywordSpotter-API) | §1 KeywordSpotter idiomatisch |
| 5, 6, 8, 14, 17, 23, 24, 25 (Audio) | §2 Audio-Pipeline |
| 7, 26, 27 (Threading/Async) | §3 WPF-Threading & Async |
| (CPU/Energie) | §4 Always-on-Effizienz |
| 19, 20, 28 (Design/Privacy) | §5 Voice-Assistant & Privacy/UX |
| 1, 2, 3, 4, 29, 30, 31 (Deployment) | §6 Modell-Bundling & Deployment |
| (Qualitaetssicherung) | §7 Testbarkeit |
| (alle, Frueherkennung) | §8 Observability |

---

## Pflicht-Checkliste vor Wake-Word-Arbeit im VoiceAgent

- [ ] Engine bewusst gewaehlt? (sherpa-onnx = Apache-2.0 + custom Wake Word ohne Training; Porcupine-Deadline 30.06.2026 bedacht; openWakeWord-Pretrained = nicht kommerziell)
- [ ] Beide NuGet-Pakete drin: `org.k2fsa.sherpa.onnx` **und** `…runtime.win-x64`, **gleiche Version**?
- [ ] DLL-Suchpfad beim Start gesetzt (`SetDllDirectory`/`AddDllDirectory`), nicht auf System32 verlassen?
- [ ] single-file publish: `CopyLocalLockFileAssemblies` + native DLLs im Output verifiziert?
- [ ] Audio-Pipeline: 16 kHz mono 16-bit, Resampler falls Mikro 48 kHz, korrekte Frame-Groesse (sherpa flexibel, Porcupine 512, openWakeWord 1280)?
- [ ] `reset_stream()` nach jeder Erkennung?
- [ ] UI-Updates aus Audio-Callback via `Dispatcher`?
- [ ] Self-Trigger: Wake-Listener waehrend TTS pausiert / AEC?
- [ ] Threshold kalibriert (FA < 0,5/h, FR < 5 %)?
- [ ] USB-Mic-Hotplug abgefangen (kein UI-Block)?
- [ ] Observability-Sonden gesetzt (z. B. CHECKPOINT "Weckwort erkannt") gemaess Observability-First?


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [voice-assistant-trigger](../android/voice-assistant-trigger.md)
- [groq-transkription](groq-transkription.md)
- [voice-pipeline](voice-pipeline.md)
