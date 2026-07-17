# Voice-Agent-Sprachpipeline (Spracheingabe → Verstehen → Sprachausgabe) — Best Practices

**Stand:** 2026-07-02 (8 Researcher parallel: Azure/Google/Deepgram/AssemblyAI-Doku,
Alexa/Google-Assistant-Design, LiveKit/Sierra/GetStream-Engineering, HA/Rhasspy/Willow-Praxis,
Windows-Audio, Groq, Google-TTS). Versions-Anker: **.NET 10.0.204** (net10.0-windows, WPF) ·
**NAudio 2.2.1** · **sherpa-onnx 1.13.2** · **Groq Whisper large-v3-turbo** · **Google TTS Chirp 3 HD** ·
VoiceAgent **1.2.0**.

> **Update 2026-07-02:** Re-Recherche fand keine neuen belegten Speech-Pipeline-Bugs seit 2026-06-10. Die Kernpraxis bleibt: FSM, Timer nur im Idle, semantisches Endpointing statt reiner Stille, Barge-in mit Echo-/Filler-Schutz, Tool-Call-Abschnitte bewusst interruptible/non-interruptible markieren.

> **Update 2026-07-10:** Für kurze Windows-UI-Signale bestätigt: PCM-Daten und Audio-Output
> vorhalten. Die kleinste nominelle Pufferlatenz ist nicht automatisch die beste; auf dem getesteten
> Realtek-Treiber waren `100 ms / 3 Buffer` schnell und flüssig, `40 ms / 2 Buffer` stotterte.
>
> **Update 2026-07-17:** Für verlustfreie Windows-Aufnahme bestätigt: Der NAudio-Capture-
> Callback darf weder WAV-I/O noch Flush noch ungedrosseltes stdout ausführen. Buffer nur
> gepoolt in eine FIFO kopieren, auf einem Writer-Thread schreiben und vor `DONE` vollständig
> drainieren. `DropOldest` ist bei Diktat Datenverlust, keine Überlaststrategie.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/desktop/voice-pipeline.md`](../../bugs/desktop/voice-pipeline.md)):
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
| 7 | Daueraufnahme stabil halten | Capture-Callback nur kopieren/queuen; FIFO-Writer drainieren; Watchdog und Stop-Guard | §3 |
| 8 | Windows-Audio einrichten | AGC/Boost/Ducking/Exclusive aus (mmsys.cpl, einmalig) | §3 |
| 9 | Latenz minimieren | Budget < 1 s, Zwischenschritte aufs kleinste Modell/Effort | §4 |
| 10 | Pipeline schnell machen | Parallelisieren statt verketten, Streaming + Ueberlappung | §4 |
| 11 | Gapless-Audio sichern | EIN offener Output, BufferedWaveProvider, LINEAR16/PCM | §4 |
| 12 | Barge-in ermoeglichen | Stufe 1 Mute-Trade-off, Stufe 3 echtes Barge-in mit AEC | §5 |
| 13 | STT-Requests fuer Voice | `language=de` explizit, Timeout 5–10 s, EIN HttpClient | §6 |
| 14 | Frueherkennung sichern | Jeden FSM-Uebergang + Stufen-Latenz als CHECKPOINT loggen | §7 |
| 15 | Aufnahme nicht von aussen abwuergen | Busy-Status (Aufnahme/Transkription) ueber lokalen Endpoint exponieren; Deploy/Rebuild/Kill wartet auf Ruhe | §8 |
| 16 | Hybrid-Diktat (Live-Vorschau + finale Engine) | Vorschau getrennt vom Zielfeld; `previewActive`-Riegel: nach Stopp schreibt nur die finale Engine; Fallback mit Hinweis | §9 |
| 17 | Kurze UI-Töne ohne Kaltstartlatenz | NAudio-Output dauerhaft offen; PCM vorhalten; Puffer live auf schnell + flüssig abstimmen | §4 |

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

- **Capture-Callback als harte Echtzeitgrenze behandeln:** `BufferMilliseconds` 50–100, ≥3
  Treiberbuffer; im Handler nur in einen `ArrayPool`-Buffer kopieren und in eine FIFO legen.
  Kein Datei-I/O, Flush, synchrones Logging oder UI-Marshalling. Genau ein Writer-Thread schreibt
  geordnet. Für verlustfreie Diktate **niemals `DropOldest`**; den maximalen Speicher stattdessen
  durch eine harte maximale Aufnahmedauer begrenzen. `offiziell`(NAudio)+`eigener Vorfall`(TVO 2026-07-17)
- **Stop ist ein Persistenzprotokoll:** Queue für neue Einträge schließen, vollständig drainieren,
  WAV flushen/disposen, dann `DONE` senden. `DONE` ist die Parent-Garantie; danach darf ein noch
  hängender nativer Worker beendet werden. Finalisierung atomar gegen Event-/Timeout-Doppelaufruf
  schützen. Turn-Dauer und PCM-/WAV-Dauer gemeinsam loggen. `eigener Vorfall`(TVO/CVO 2026-07-17)
- **WPF reaktiv halten:** Worker-Start und `READY` asynchron abwarten, Stop während Start als
  Pending-Zustand erhalten. Pegel im Worker auf etwa 10 Hz aggregieren und in WPF höchstens einen
  Dispatcher-Auftrag gleichzeitig offen halten. `eigener Vorfall`(TVO/CVO 2026-07-17)
- **CPU-Druck gezielt abfangen:** Den langlebigen Overlay- und Capture-Prozess höchstens auf
  Windows-Prioritätsklasse `High` setzen und den echten Capture-Callback über MMCSS
  (`Pro Audio`, Fallback `Audio`) mit `Critical` priorisieren. Writer-/Stop-Threads dürfen
  `Highest` nutzen. `RealTime` als Prozessklasse ist verboten: Sie kann Eingabe, Audio-Treiber
  und Systemdienste aushungern und dadurch gerade neue Aussetzer erzeugen. Prioritätsklasse und
  aktives MMCSS-Profil nach dem Deploy live verifizieren. `eigener Vorfall`(TVO/CVO 2026-07-17)
- **Watchdog**: kommt N Sekunden kein `DataAvailable` trotz aktivem Mikro → Capture neu aufbauen
  (Stop → RecordingStopped abwarten → Dispose → Start). Stop/Dispose strikt serialisieren,
  NIE im Callback. `extern`(NAudio #1168/#1150)
- **Geraetewechsel**: `IMMNotificationClient` registrieren, Events 200–500 ms debouncen,
  geordnet neu starten BEVOR ein blockierender WinMM-Call passiert (#657-Falle). `offiziell`
- **Windows-Audio-Checkliste** (einmalig pruefen, mmsys.cpl): AGC/„Mikrofon automatisch
  anpassen" AUS (auch Treiber-/Headset-Tool!), Mic-Boost ≤ +10 dB, Communications-Ducking
  "Do nothing", Exclusive-Mode-Haken weg, RAW-Mode bewusst waehlen. `offiziell`(MS Learn)
- **Prozess-Isolation der Aufnahme, wenn 24/7-Zuverlaessigkeit zaehlt.** Die WinMM-`waveIn*`-
  AccessViolation (§3.2 im Almanach) ist NICHT abfangbar und killt sonst die ganze App. Capture in
  einen Kindprozess auslagern: ein Crash killt nur den Worker, UI + gesprochener Text ueberleben.
  Regeln: (a) die WAV bei jedem Buffer flushen (Header-Laengen mitfuehren) und den Writer VOR dem
  `WaveIn.Dispose` schliessen → die Aufnahme uebersteht selbst einen Mid-Crash und wird
  weiterverarbeitet; (b) im Worker `SynchronizationContext.SetSynchronizationContext(null)`, sonst
  postet NAudio `RecordingStopped` an einen evtl. blockierten Context (Stop haengt bis Timeout);
  (c) traegt der Worker denselben EXE-Namen, den namensbasierten Watchdog per PID-Datei absichern,
  damit er den kurzlebigen Worker nicht mit dem Overlay verwechselt. `eigener Vorfall`(TVO/CVO 2026-07-16)

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
- **Kurze Statussignale unter Windows**: Nicht pro Klick `Console.Beep`, `SoundPlayer` oder einen
  neuen Output starten. Einen `WaveOutEvent` beim App-Start öffnen, mit
  `BufferedWaveProvider(ReadFully=true)` warm halten und vorberechnete PCM-Samples einreihen.
  `DesiredLatency` und Bufferzahl sind Treiberparameter, keine Wettbewerbswerte: mit Sonden und
  Hörtest den kleinsten **stabilen** Wert wählen. Bestätigte Realtek-Baseline: `100 ms / 3 Buffer`;
  `40 ms / 2 Buffer` verursachte Underruns. Output-Stopp/Fehler loggen und reinitialisieren. `eigener Vorfall`

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

---

## 8. Aufnahme-Lebenszyklus vor externen Eingriffen schuetzen (Deploy/Rebuild/Kill)

Ein Voice-Overlay/-Tool, das per Hot-Reload, Rebuild-Skript oder Watchdog neu gestartet wird, darf
NIEMALS hart beendet werden, solange aufgenommen, transkribiert ODER der fertige Text gerade
eingefuegt wird — sonst geht der noch nicht eingefuegte Text still verloren. Besonders kritisch bei
mehreren parallelen Tool-/Agenten-Sessions: die killende Session weiss nichts von der laufenden
Aufnahme in einer anderen Session.

**Pattern (verifiziert 2026-07-17, TVO/CVO — Poka-Yoke St. 3):**
- Das Tool exponiert seinen **Busy-Status** ueber einen winzigen lokalen Loopback-Endpoint
  (`GET http://127.0.0.1:<port>/recording/status` → `{"busy":true|false}`). `busy` = Aufnahme laeuft
  ODER Transkription laeuft ODER **Einfuegen/Paste laeuft** (NICHT nur „Mikro an").
- Jeder Build-, Update- und Rebuild-Einstieg reserviert das laufende Overlay **vor Build und Kill**
  atomar über `POST /deployment/prepare`. Die App erteilt die Reservierung nur bei `busy=false`
  und blockiert danach neue Aufnahmen bis `POST /deployment/release` oder Prozessneustart.
- **`busy` MUSS bis zum bestaetigten Einfuegen true bleiben** — nicht schon nach der Transkription auf
  false fallen. Sonst entsteht eine Luecke, in der der Text noch im Tool haengt (noch nicht im Ziel)
  und ein Kill ihn verliert. Faustregel: `busy=false` erst, NACHDEM der Paste sicher im Zielfenster ist.
- Optional ein **kleiner Nachlauf-Puffer** (z.B. 500–1000 ms `busy=true` nach dem Paste), damit bei sehr
  langen Texten / traeger Ziel-App nichts in der Uebergabe abgeschnitten wird.
- **Wo der Text sicher ist:** Sobald er im Zielfenster (CLI-Zeile, Prompt-Eingabefeld) steht, ist er
  UNABHAENGIG vom Tool — ein Neustart des Overlays beruehrt ihn nicht mehr. Der Schutz muss also nur
  die Phasen Aufnahme→Transkription→**Paste abgeschlossen** abdecken, nicht danach.
- Der Status haengt am **Tool**, nicht an der ausloesenden Session → session-uebergreifender Schutz.
  Endpoint nicht erreichbar bei laufendem Tool → **fail-closed** warten und nach Timeout sicher
  abbrechen, niemals trotzdem bauen oder killen. Nur ohne laufenden Prozess darf es weitergehen.
- Busy aus dem vorhandenen FSM ableiten (§1: nicht-Idle = busy) — keine separate Zustandshaltung (Drift).

**Zwei getrennte Schutzebenen (wichtig — verifiziert 2026-06-22):** Der busy-Lock schuetzt nur die
*Uebergabe* (Aufnahme→Transkription→Einfuegen). Ein Text, der danach RUHEND im tool-eigenen Fenster
steht (z.B. ein Prompt-Eingabefeld im selben Prozess, noch nicht abgeschickt), ueberlebt einen Neustart
NUR mit eigener **Draft-Persistenz**: bei jeder Aenderung sofort atomar in eine kleine Datei spiegeln
(`input-draft.txt`), beim Start wiederherstellen UND das Fenster bei vorhandenem Draft automatisch
oeffnen. Text in einem FREMDEN Fenster (CLI/Terminal) braucht das nicht — der lebt in einem anderen
Prozess und ist nach dem Einfuegen ohnehin sicher. **Merksatz: Lock = Uebergabe schuetzen,
Draft = ruhenden Text schuetzen.** Plus optionaler 5s-Nachlauf nach `busy=false`, damit Transkription
+ Draft-Schreiben garantiert durch sind, bevor gekillt wird.

Macht „kein Datenverlust durch Neustart mitten in der Aufnahme/Uebergabe" strukturell erzwungen statt
von Disziplin abhaengig.

---

## 9. Hybrid-Diktat: Live-Vorschau + finale Transkription (so baut man es richtig)

Viele Diktat-UIs kombinieren zwei STT-Quellen: eine **schnelle Live-Engine** (Web Speech API,
`interimResults`) fuer die Sofort-Vorschau WAEHREND des Sprechens und eine **Qualitaets-Engine**
(Whisper/Groq) fuer die finale Fassung MIT Satzzeichen beim Stopp. Damit das fluessig wirkt UND die finale
Fassung immer gewinnt:

- **Vorschau strikt vom Zielfeld trennen.** Rohe interim results NIE direkt ins finale Zielfeld schreiben.
  Eigenes `<textarea>`: `value` relativ zum Basis-Text setzen (still). Fremdes `contenteditable`
  (ChatGPT/Claude): Vorschau in ein SEPARATES schwebendes Element — Zielfeld bleibt bis zur finalen Fassung
  unberuehrt (kein Flackern/Springen, siehe `best-practices/web/chrome-extensions.md` §7). `extern`
- **Zeitlicher Riegel `previewActive`** (DER Kernschutz): `true` bei Vorschau-Start, **`false` sofort beim
  Stopp** (vor der finalen Transkription) und beim Entfernen der Vorschau. Der Vorschau-Handler beginnt mit
  `if (!previewActive) return;`. So kann ein spaetes `onresult`/`onend` der Live-Engine die finale Fassung
  nicht mehr ueberschreiben — ab dem Stopp schreibt nur die finale Engine ins Feld. `extern`
- **Finale Engine ist die EINZIGE Quelle fuers Zielfeld.** Erst ihr Ergebnis (mit Satzzeichen) wird
  eingefuegt; Auto-Send erst NACH der finalen Fassung.
- **Glaetten:** kleiner Debounce (~120 ms) auf die Vorschau-Updates; finale Woerter deckend, interim
  gedimmt/kursiv — ruhiges Live-Untertitel-Gefuehl.
- **Fallback funktionserhaltend:** Faellt die finale Engine aus, Vorschau als Notnagel behalten, aber MIT
  sichtbarem Hinweis ("unkorrigiert, ohne Satzzeichen") — nie stillschweigend als echte Fassung nutzen.
- **Sprache explizit** setzen (`lang=de`), nicht Auto-Detect (siehe §6).

Belegt: eigener Vorfall 2026-06-24 (second-brain Dashboard 0.5.1, overlays 0.6.4) — von Frank bestaetigt
("funktioniert wirklich super"). Bug-Gegenseite: `bugs/desktop/voice-pipeline.md` §7.

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) ↔ Bug-Almanach [`~/proggs/bugs/desktop/voice-pipeline.md`](../../bugs/desktop/voice-pipeline.md). Links die *Praevention*, rechts die *Falle*.

| Best-Practice-Abschnitt (hier) | Zugehoeriger Bug-Almanach-Abschnitt (`bugs/desktop/voice-pipeline.md`) |
|--------------------------------|-----------------------------------------------------------------------|
| BP §1 Zustandsautomat & Fenster-Design | §1 Wachfenster/Session |
| BP §2 Endpointing richtig dimensionieren | §2 VAD/Endpointing |
| BP §3 Robuste Daueraufnahme | §3 NAudio-Capture |
| BP §4 Latenz-Budget & Pipeline-Ueberlappung | §4 Latenz/Architektur |
| BP §5 Echo & Unterbrechbarkeit | §5 Barge-in/Self-Trigger |
| BP §6 STT-Requests fuer Voice | §6 STT-Anbindung |
