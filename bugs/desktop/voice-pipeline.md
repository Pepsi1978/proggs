# Bekannte Bugs & Fallen: Voice-Agent-Sprachpipeline (Spracheingabe → Verstehen → Sprachausgabe)

> **PFLICHT-LESEN vor JEDER echten Arbeit an einer Sprach-Pipeline** (VAD/Endpointing,
> Wake-Word-Wachfenster, Turn-Taking, STT-Anbindung, TTS-Wiedergabe, Barge-in) — egal in
> welcher App (VoiceAgent, TVO/CVO-Overlays, EntropieReductor-Mikrofon). Loesungen sind
> **funktionserhaltend** — Franks Rede darf NIE still verworfen werden.
>
> **Stand:** recherchiert am **2026-06-10**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax; keine neuen belastbaren Bugreports seit dem Stichtag) (8 Researcher parallel, offizielle Quellen zuerst:
> Azure/Google/Deepgram/AssemblyAI-Doku, Alexa/Google-Assistant-Design, LiveKit/Sierra/Deepgram-
> Engineering-Blogs, HA/Rhasspy/Willow-Praxis). Software-Anker: **.NET 10.0.204** (net10.0-windows,
> WPF) · **NAudio 2.2.1** · **sherpa-onnx 1.13.2** · **Groq Whisper** (large-v3-turbo) ·
> **Google TTS Chirp 3 HD** · VoiceAgent **1.2.0**. GitHub-Issue-Status per `gh` verifiziert.
>
> **Nachbar-Almanache** (NICHT dupliziert, hier nur Pipeline-uebergreifendes):
> `wake-word.md` (KWS-Engine/Deployment), `groq-transkription.md` (Whisper-Halluzination/Groq-API),
> `dotnet-csharp.md` (WPF/async). **Gegenseite (Praevention):**
> `best-practices/desktop/voice-pipeline.md`.

> **Update 2026-07-02:** Keine neuen belegten Bugs/Regressionen seit 2026-06-10 gefunden. Bestehende Designrisiken wurden bestaetigt: silence-only VAD verwechselt Denkpausen mit Turn-Ende, Barge-in braucht Echo-/Filler-Handling, und Tool-Calls duerfen nicht mitten in der Wiedergabe unkontrolliert unterbrochen werden.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Fenster laeuft ab waehrend Nutzer spricht ⭐⭐ | Timer NUR im Idle ticken, ab Antwort-Ende zaehlen | §1.1 |
| 2 | Fertige Aufnahme im Schlaf still geloescht ⭐⭐ | Im Fenster begonnene Rede IMMER verarbeiten, nie verwerfen | §1.2 |
| 3 | Aufnahme bleibt endlos offen (Luefter/TV) ⭐⭐ | Harter Max-Deckel 15–30 s, dann finalisieren UND verarbeiten | §2.1 |
| 4 | Agent traege ODER schneidet im Satz ab | Stille 300–550 ms Dialog, 1000–2000 ms Diktat + semantisches Netz | §2.2 |
| 5 | Erste Antwort dauert viele Sekunden ⭐ | Zwischenschritte aufs kleinste Modell/niedrigsten Effort, parallelisieren | §4.1 |
| 6 | Follow-up/zweiter Befehl wird ignoriert | Fenster nach JEDER Antwort oeffnen, History kurz (3–5) | §1.3 |
| 7 | Fremdgespraech als Befehl, Session bricht ab | Wake-Word 3+ Silben, Confidence-Gate im Fenster | §1.4 |
| 8 | Leise Wort-Enden fehlen / Status flattert | Hysterese (Dual-Threshold), Frames 20–30 ms | §2.3 |
| 9 | Schwelle stimmt nach lauter TTS nicht mehr | AGC/Boost an 3 Stellen pruefen, relative Schwellen statt fester | §2.4 |
| 10 | DataAvailable versiegt still, Mikro taub | Watchdog: kein Event N s → Capture neu starten | §3.1 |
| 11 | AccessViolation/Deadlock bei Stop/USB-Abzug | Stop/Dispose serialisieren, NIE im Callback disposen | §3.2 |
| 12 | Nutzer kann Agent nicht unterbrechen | Partial Ducking statt Mute; echtes Barge-in braucht AEC | §5.1 |
| 13 | Klicks/Luecken zwischen Saetzen | EIN offener Output, BufferedWaveProvider, PCM statt MP3 | §4.3 |
| 14 | Einzelwort ("ja") in falscher Sprache | `language=de` IMMER explizit, nie Auto-Detect | §6.1 |
| 15 | Haengender STT friert Turn 60 s ein | Groq-Timeout auf 5–10 s, EIN statischer HttpClient | §6.2 |
| 16 | Diktat-Live-Vorschau ueberschreibt finale Fassung / springt im Feld ⭐⭐ | Vorschau getrennt vom Zielfeld; `previewActive`-Riegel: nach Stopp schreibt NUR die finale Engine | §7 |

---

## 1. Wake-Word-Wachfenster & Session-Lebenszyklus

### 1.1 Wachfenster-Timer laeuft ab, waehrend der Nutzer noch spricht   [⭐⭐ EIGENER VORFALL 2026-06-10]
**Symptom:** Nach der ersten Antwort hoert der Agent zu, der Nutzer redet — und nichts passiert.
Nach Ablauf des Fensters (60 s) schlaeft der Agent mitten in der Rede ein; die danach fertige
Aufnahme wird verworfen. Log-Beweis VoiceAgent: Timeout 11:23:10.9, Aussage (41,8 s) fertig 11:23:11.1 → geloescht.
**Ursache:** Der Fenster-Timer zaehlte ab der LETZTEN fertigen Aussage und wurde weder durch
laufende Sprache noch durch die laufende Antwort (TTS/LLM) verlaengert. `NotifyActivity` feuerte
nur bei abgeschlossenen Utterances.
**Versionen:** VoiceAgent ≤ 1.1.0; Architektur-Falle in jedem selbstgebauten Assistenten.
**FIX:** Zustandsautomat Idle/Listening/Thinking/Speaking — der Timeout-Timer laeuft NUR im Idle.
Konkret: im Sekundentakt `if (busy || listener.IsCapturing) wake.NotifyActivity();` VOR `wake.Tick()`;
zusaetzlich Sprechbeginn (`OnSpeechStart`) als Aktivitaet werten. Vorbild Alexa Follow-Up Mode
(5 s NACH Antwort-Ende) und Google Continued Conversation (8 s) — Timer startet nach Antwort-ENDE.
Gefixt in VoiceAgent 1.2.0 (#46675).
**Quelle:** eigener Vorfall (Log 2026-06-10) · Tom's Guide/Amazon (Follow-Up Mode) · Google Nest Help · HuggingFace Voice-Agent-Deep-Dive (4-State-FSM).

### 1.2 Fertige Aufnahme wird im Schlaf-Zustand still geloescht   [⭐⭐ EIGENER VORFALL]
**Symptom:** Nutzer-Rede landet kommentarlos im Muell — kein Ton, kein Log-Hinweis fuer den Nutzer.
**Ursache:** Gating-Code wie `if (state == Sleeping) { delete(wav); return; }` prueft nur den
AKTUELLEN Zustand, nicht, ob die Aufnahme im wachen Zustand BEGANN.
**Versionen:** VoiceAgent ≤ 1.1.0; gleiche Falle in HA/ESPHome-Setups dokumentiert.
**FIX:** Pro Aufnahme beim Sprechbeginn merken, ob der Agent wach war (`_speechBeganAwake`).
Ist er beim Eintreffen der fertigen Aussage eingeschlafen, die Aussage TROTZDEM verarbeiten und
wieder aufwachen (Gespraech laeuft offensichtlich weiter). Gefixt in VoiceAgent 1.2.0.
**Quelle:** eigener Vorfall · LiveKit Turn-Detection-Blog ("Aufnahme die im Fenster begann immer fertig verarbeiten").

### 1.3 Follow-up nur nach Rueckfrage / zweiter Befehl scheitert
**Symptom:** Der zweite Befehl ohne Weckwort wird ignoriert, oder nur der erste Befehl einer
Session funktioniert.
**Ursache:** (a) Follow-up-Fenster oeffnet nur, wenn die Antwort eine Frage war (HA-Design);
(b) Conversation-History akkumuliert und verwirrt das Modell.
**Versionen:** Home Assistant Voice PE / Assist; uebertragbar.
**FIX:** (a) Fenster nach JEDER Antwort oeffnen (Alexa/Google-Muster); (b) History fuer
Voice-Befehle kurz halten (HA-Empfehlung: max_history 3–5) bzw. Kontext-Kompression nutzen.
**Quelle:** HA Community (continuous conversation workaround) · HA Troubleshooting.

### 1.4 False-Activation killt die Session / Fremdgespraech als Follow-up
**Symptom:** Session bricht sofort ab (Wake-Word-Fragment wird als Befehl interpretiert) ODER
Gespraech im Raum wird faelschlich als Follow-up verarbeitet.
**Ursache:** Wake-Word-Wahl (kurze Woerter = mehr False Positives); kein Confidence-Gate im Fenster.
**Versionen:** HA Voice PE (Firmware-Bug "Hey Mycroft"); generell.
**FIX:** Wake-Word mit 3+ Silben bevorzugen; im Wachfenster Confidence-/Sprachgehalt-Gate
(Alexa: "nur wenn sicher ein legitimer Befehl"). VoiceAgent: Voiced-Vorfilter wirkt hier mit.
**Quelle:** HA Community (Voice PE stops listening) · Tom's Guide (Follow-Up Confidence).

---

## 2. VAD / Endpointing (Stille-Erkennung)

### 2.1 Hintergrundgeraeusche halten die Aufnahme endlos offen   [⭐⭐ EIGENER VORFALL]
**Symptom:** "Der Assistent hoert einfach nur zu und nichts passiert" — Aufnahmen von 41,8 s
und 131 s im Log, obwohl der Nutzer laengst fertig war. Bei HA: TV/Radio haelt die Session offen.
**Ursache:** Reine RMS-Energie-Schwellen koennen Sprache nicht von Geraeusch unterscheiden:
Tastatur-Klicks, Atmen, Luefter ueber der Schwelle setzen die Stille-Uhr immer wieder zurueck —
das Aussage-Ende kommt nie. Empfindliche Schwelle (z.B. 0.007) verschaerft das.
**Versionen:** jede Energie-VAD (NAudio-RMS, WebRTC-GMM); per Design.
**FIX (Kette):** (1) **Harter Max-Utterance-Deckel** (Azure segmentiert intern ~15 s, Rhasspy
timeout 30 s): beim Deckel finalisieren und VERARBEITEN — nie verwerfen; Aufnahme laeuft danach
als neue Aussage weiter. (2) Adaptiver Noise-Floor (`thr ≈ 3×noise`, noise nur in Stille nachfuehren)
statt fester Schwelle. (3) Upgrade-Pfad: ML-VAD (Silero, ONNX — passt zu sherpa-Stack; 4× weniger
Fehler als WebRTC) fuer die Endpoint-Entscheidung; RMS bleibt als billige Vorstufe.
Gefixt (Stufe 1) in VoiceAgent 1.2.0: `MaxUtteranceMs=30000`.
**Quelle:** eigener Vorfall · Picovoice VAD-Guide 2026 · Silero Quality Metrics · Azure #2740 (SegmentationMaximumTimeMs) · HA Community (background noise).

### 2.2 Stille-Pause falsch dimensioniert: traege ODER schneidet ab
**Symptom:** Entweder wartet der Agent nach jedem Satz sekundenlang (traege, "nicht live"),
oder er schneidet mitten im Satz ab (Denkpausen werden als Ende gewertet).
**Ursache:** Die Stille-Pause (silenceMs) ist ein Kompromiss — ein fester Wert passt nie fuer alles.
**Versionen:** unabhaengig.
**FIX (offizielle Richtwerte):** Konversation **300–550 ms** (AssemblyAI default 160 ms bei hoher
Konfidenz, max 2400 ms); Azure-Dialog 300–500 ms; **Diktat 1000–2000 ms** (Azure: 2000 bei Zahlen/
Codes). Dazu IMMER ein semantisches Sicherheitsnetz: LLM-Endpoint-Check (FERTIG/WEITER) sammelt
bei unvollstaendigen Saetzen weiter (VoiceAgent hat das: SemanticEndpointing + EndpointMaxWaitMs).
Mit diesem Netz ist ein flotter Wert (1000–1200 ms) auch fuer Diktat sicher. VoiceAgent 1.2.0:
SilenceMs 3000 → 1200.
**Quelle:** Azure SegmentationSilenceTimeout-Doku · Deepgram Endpointing · AssemblyAI Universal-Streaming · AlterSquare (VAD End-of-Speech).

### 2.3 Schwelle zu hoch schneidet leise Sprache / Flattern an der Grenze
**Symptom:** Leise Wort-Enden fehlen; oder der Sprach-Status toggelt bei Grenzpegel staendig.
**Ursache:** EINE statische Schwelle fuer Aktivierung UND Deaktivierung.
**Versionen:** unabhaengig.
**FIX:** Hysterese (Dual-Threshold): Deaktivierungs-Schwelle ~0.15 UNTER der Aktivierungs-Schwelle;
Frames 20–30 ms statt 10 ms (stabiler gegen Klick-Spikes).
**Quelle:** AlterSquare · Silero-Wiki · Picovoice VAD-Guide.

### 2.4 Windows-AGC/Boost kippt die RMS-Schwellen
**Symptom:** Nach lauter TTS-Wiedergabe oder spontan stimmt die kalibrierte Schwelle nicht mehr —
Endpointing schneidet zu frueh oder haengt.
**Ursache:** Automatic Gain Control (Windows-Mikro-Einstellung, Treiber-Software, Headset-Utility)
regelt den Pegel live; Mic-Boost (+30 dB) hebt den Noise-Floor ueber jede feste Schwelle.
**Versionen:** Windows 10/11, treiberabhaengig (Win11: Pegel springt teils trotz "Auto aus").
**FIX:** AGC an allen DREI Stellen pruefen/abschalten (Windows-Mikrofon-Eigenschaften,
Treiber-Software, Headset-Tool); Boost ≤ +10 dB; langfristig relative Schwellen
(gleitender Noise-Floor + Hysterese) statt absoluter RMS-Werte.
**Quelle:** MS Q&A (mic gain auto-adjusted) · ittrip (Win11 mic volume drop) · TheWindowsClub (AGC).

---

## 3. Audio-Capture (NAudio, 24/7-Mithoeren) — Issue-Status per gh verifiziert 2026-06-10

### 3.1 `DataAvailable` versiegt still (Aufnahme stoppt ohne Fehler)
**Symptom:** Nach einiger Zeit kommen keine Audio-Events mehr — kein Fehler, kein Event; der
Assistent ist faktisch taub, sieht aber "an" aus.
**Ursache:** WinMM-Buffer-Kette reisst bei zu grossen/wenigen Buffern oder USB-Hiccups
(Power-Management) ab und wird nicht neu eingereiht.
**Versionen:** NAudio 2.x (naudio/NAudio#1168 — **CLOSED COMPLETED**, aber Muster bleibt relevant).
**FIX:** `BufferMilliseconds` 50–100 (Default 100) + ≥3 Buffer lassen; **Watchdog**: kommt X Sekunden
kein `DataAvailable`, Capture stoppen + disposen + neu starten; USB-Selective-Suspend fuers Mikro aus.
**Quelle:** naudio/NAudio#1168 (gh-verifiziert).

### 3.2 Stop/Dispose-Races: AccessViolation & Ewig-Blockieren
**Symptom:** Sporadische `AccessViolationException` (nicht abfangbar, killt den Prozess) beim
Stop/Dispose; `StopRecording()`/`DeviceCount` blockiert fuer immer nach USB-Abzug; Deadlock bei
schnellem Geraetewechsel.
**Ursache:** Fehlende Thread-Safety im WinMM-Interop (kein SafeHandle); WinMM wartet auf ein
nicht mehr existierendes Geraet.
**Versionen:** NAudio 2.x — #1150 **OPEN**, #657 **OPEN**, #1084 **OPEN**, #1203 **OPEN** (gh-verifiziert) → Workarounds dauerhaft.
**FIX:** Stop/Dispose strikt serialisieren (ein Lock), NIE aus dem `DataAvailable`-Handler heraus
disposen; nach `StopRecording()` auf `RecordingStopped` warten; Geraetewechsel ueber
`IMMNotificationClient` proaktiv erkennen und mit 200–500 ms Debounce GEORDNET neu starten,
bevor ein blockierender WinMM-Call passiert; Stop mit Timeout in eigenem Thread kapseln.
**Quelle:** naudio/NAudio#1150 · #657 · #1084 · #1203 (alle gh-verifiziert OPEN).

### 3.3 `WaveInEvent` belegt dauerhaft einen ThreadPool-Thread
**Symptom:** Bei Langlauf wird die App traege; parallele Tasks haengen (Pool-Starvation).
**Ursache:** WaveInEvent stellt eine blockierende Dauerschleife auf einen ThreadPool-Thread.
**Versionen:** NAudio 2.x per Design (#539 CLOSED COMPLETED = dokumentiert).
**FIX:** Genau EINE langlebige Instanz (nicht staendig neu erzeugen); `DataAvailable`-Handler
sofort zurueckkehren lassen (Arbeit in bounded Channel auslagern, DropOldest).
**Quelle:** naudio/NAudio#539 (gh-verifiziert).

---

## 4. Turn-Latenz & Pipeline-Architektur (das "live"-Gefuehl)

### 4.1 Sequenzielle LLM-Kette verdoppelt die Latenz   [⭐ EIGENER VORFALL]
**Symptom:** Vom Sprechende bis zum ersten Ton vergehen viele Sekunden. VoiceAgent-Messung
2026-06-10: 0,6 s Endpoint-Check + **6,6 s Verstehens-Schritt** (Codex gpt-5.5, Effort high) +
~1,3 s Antwort-LLM — sequenziell.
**Ursache:** Jeder LLM-Zwischenschritt (Intent, Verstehen, Endpoint) laeuft NACHEINANDER, und
fuer Klassifikationsjobs wird ein Modell/Effort wie fuer die Hauptantwort verwendet. "Most teams
blow the budget on the LLM slice because they pick the wrong model."
**Versionen:** Architektur-Falle, unabhaengig.
**FIX (Stufen):** (1) Zwischenschritte auf das KLEINSTE ausreichende Modell/den niedrigsten
Effort legen (VoiceAgent 1.2.0: Brain-Rolle auf Codex = Effort "low" → erwartet ~2 s statt 6,6 s;
Modell bleibt stark). (2) Unabhaengige Schritte parallelisieren (Endpoint-Check || Verstehen).
(3) Fortgeschritten: Speculation — Verstehen schon auf dem Zwischen-Transkript starten;
Filler-Architektur (`<speech>Moment …</speech><tool>…</tool>`) ueberbrueckt lange Tools.
**Richtwerte (Profi-Stacks):** Voice-to-Voice < 1 s wirkt live (Mensch erwartet 300–500 ms);
Budget: Turn-Detection 50–100 ms, STT 50–200 ms, LLM-TTFT 100–200 ms, TTS-TTFB 50–80 ms.
**Quelle:** eigener Vorfall · channel.tel (Latenz-Budget) · Sierra (voice latency) · GetStream (speculative tool calling) · LiveKit.

### 4.2 Batch-WAV statt Streaming-STT / TTS erst nach komplettem LLM-Text
**Symptom:** Jede Stufe wartet auf die vorige — 2–4 s Ende-zu-Ende statt < 1 s.
**Ursache:** Pipeline ohne Ueberlappung.
**Versionen:** unabhaengig. Hinweis: Groq hat KEIN STT-Streaming (REST/file-basiert) — dort ist
"VAD-getriggerte kurze Chunks sofort senden" die latenz-aermste Form.
**FIX:** Ueberlappen, wo es geht: LLM-Token satzweise an TTS (VoiceAgent: StreamingSpeaker ✓),
TTS-Audio abspielen waehrend der Rest generiert (✓), haeufige Phrasen vorab synthetisieren.
Naechste Stufe fuer Google TTS: **Chirp 3 HD bidirektionales Streaming** (`StreamingSynthesize`,
gRPC) statt REST pro Satz (~500 ms TTFB) — loest auch das 200-RPM-Limit (Chirp3) bei satzweisen Calls.
ACHTUNG: kein SSML im Streaming; Pausen via `[pause]`-Markup; 5.000 Bytes/Request-Limit.
**Quelle:** LiveKit (Architektur) · Google StreamingSynthesize-Doku · Google TTS Quotas · Softcery (TTFB-Vergleich).

### 4.3 Gapless-Wiedergabe: Klicks/Luecken zwischen Saetzen
**Symptom:** Hoerbare Klicks oder Mini-Pausen zwischen satzweise synthetisierten Audio-Stuecken.
**Ursache:** Output-Stream wird pro Satz geschlossen/neu geoeffnet; Format-Mismatch zwischen
Chunks; MP3-Frame-Grenzen.
**Versionen:** unabhaengig (NAudio).
**FIX:** EINEN Output offen halten und Saetze in einen `BufferedWaveProvider` einspeisen;
LINEAR16/PCM statt MP3; identisches WaveFormat fuer alle Chunks.
**Quelle:** markheath.net (Concatenating WAV) · Hydrogenaudio (Gapless).

---

## 5. Barge-in & Self-Trigger (Agent hoert sich selbst)

### 5.1 Mikro-Muten waehrend TTS killt Barge-in
**Symptom:** Der Nutzer kann den sprechenden Agenten nicht unterbrechen; alles, was er waehrend
der Antwort sagt, geht verloren.
**Ursache:** Einfachster Anti-Echo-Fix (Mic aus waehrend TTS) entfernt zugleich die Faehigkeit
zuzuhoeren. (VoiceAgent macht aktuell PauseMic waehrend TTS — bewusster Trade-off, Bug 19 wake-word.)
**Versionen:** Designfrage, unabhaengig.
**FIX (Stufen):** (a) Partial Ducking (Gain -10 bis -20 dB statt Mute) — laute Unterbrechungen
kommen durch; (b) echtes Barge-in: VAD waehrend TTS aktiv + AEC mit TTS-Referenzsignal +
Double-Talk-Detection; bei erkannter Nutzer-Sprache TTS-Stream < 200 ms stoppen + Buffer verwerfen.
AEC braucht eine LINEARE Wiedergabekette (wake-word-Almanach Bug 28). Windows: Voice Capture DSP /
Azure Speech `AUDIO_INPUT_PROCESSING_ENABLE_V2` (ML-AEC) / StarTrinity-Lib fuer .NET.
**Quelle:** Coval (Voice AI Echo Cancellation) · LiveKit · Speechmatics · MS Learn (AECMicArray).

### 5.2 Windows-Ducking & RAW-Mode-Fallen
**Symptom:** Andere Sounds werden bei "Kommunikationsaktivitaet" automatisch leiser (Ducking)
ODER die erwarteten Windows-Enhancements (AEC/NS) wirken gar nicht.
**Ursache:** Windows-Communications-Ducking (Default "Reduce by 80%"); RAW-Mode-Capture
deaktiviert AEC/AGC/NS komplett.
**Versionen:** Windows 10/11; Communications-Tab in aktuellen Win11-Builds teils unzuverlaessig.
**FIX:** `mmsys.cpl` → Communications → "Do nothing" (oder Ducking-API der App); RAW vs. non-RAW
bewusst waehlen (eigene VAD/AEC → RAW gut; Windows-AEC gewollt → RAW aus + per Audio Effects
Discovery pruefen, ob AEC wirklich aktiv ist).
**Quelle:** MS Learn (Disabling the Ducking Experience · Audio Signal Processing Modes).

---

## 6. STT-Anbindung (Groq-spezifisch, Ergaenzung zu groq-transkription.md)

### 6.1 Kurze Aussagen (<1 s) in falscher Sprache / als Muell transkribiert
**Symptom:** Einzelwoerter ("ja", "stopp") kommen in falscher Sprache oder als Unsinn zurueck.
**Ursache:** Whisper padded intern auf 30 s — bei <1 s echter Sprache dominiert Stille das
Sprach-Auto-Detect.
**Versionen:** alle Whisper (Groq).
**FIX:** `language`-Parameter IMMER explizit setzen (ISO-639-1, `de`) — nie Auto-Detect bei
kurzen Turns. (VoiceAgent ✓)
**Quelle:** Groq STT Docs · Pipecat Groq-Doku.

### 6.2 SDK-Default-Timeout 60 s blockiert den Voice-Turn
**Symptom:** Ein haengender STT-Request friert den Turn fuer eine Minute ein.
**Ursache:** Groq-SDK-Defaults: 60 s Timeout, 2 Auto-Retries — fuer Voice viel zu lang.
**Versionen:** Groq-SDKs aktuell.
**FIX:** Request-Timeout 5–10 s setzen, schnell failen + dem Nutzer Bescheid sagen; bei 429
`retry-after` lesen. Verbindungs-Reuse (EIN statischer HttpClient) ist der groesste
EU-Latenz-Hebel (TLS-Handshake nur einmal); `x-groq-region`-Header zur Diagnose.
**Quelle:** Groq Errors/SDK-Doku · Groq Optimizing-Latency-Doku.

---

## 7. Hybrid-Diktat: Live-Vorschau + finale Transkription

### 7.1 Live-Vorschau ueberschreibt die finale Fassung / wird versehentlich gesendet   [⭐⭐ EIGENER VORFALL 2026-06-24]
**Symptom:** Beim Diktat mit ZWEI STT-Quellen (schnelle Live-Engine wie Web Speech API fuer die
Sofort-Vorschau + Qualitaets-Engine wie Whisper/Groq fuer die finale Fassung MIT Satzzeichen) landet am
Ende die ROHE Live-Vorschau im Feld (ohne Satzzeichen) statt der finalen Transkription. Manchmal wird die
Vorschau sogar abgeschickt, bevor die finale Engine fertig ist.
**Ursache:** Die Live-Engine (`SpeechRecognition`) feuert nach `stop()`/`abort()` oft noch ein spaetes
`onresult`/`onend`. Schreibt dieser Handler weiterhin ins Zielfeld, ueberschreibt die rohe Vorschau die
schon gesetzte finale Fassung (Race); ein automatischer Auto-Send greift dann die Vorschau ab.
**FIX (zeitlicher Riegel):** Ein Flag `previewActive` — `true` beim Start der Vorschau, **`false` SOFORT
beim Stopp** (vor der finalen Transkription) UND beim Entfernen der Vorschau. Der Vorschau-Handler beginnt
mit `if (!previewActive) return;`. Ab dem Stopp schreibt AUSSCHLIESSLICH die finale Engine ins Zielfeld —
die rohe Vorschau kann es nie mehr veraendern und nie versehentlich gesendet werden.
**Fallback (funktionserhaltend):** Faellt die finale Engine aus, bleibt die Vorschau als Notnagel im Feld —
aber MIT sichtbarem Hinweis ("unkorrigierte Vorschau ohne Satzzeichen"), nie stillschweigend als echte Fassung.
**Quelle:** eigener Vorfall 2026-06-24 (second-brain Dashboard 0.5.1, overlays 0.6.4). Verifiziert.

### 7.2 Live-Vorschau "springt"/flackert im Eingabefeld   [⭐ EIGENER VORFALL 2026-06-24]
**Symptom:** Waehrend des Sprechens springt/flackert der Text, Woerter zappeln hin und her, Cursor/Scroll
springen — statt ruhig Wort fuer Wort zu erscheinen.
**Ursache:** Die interim results revidieren sich staendig; wird jede Revision ins Zielfeld geschrieben,
flackert es. Besonders schlimm bei fremden `contenteditable`-Feldern (Paste-Simulation selektiert+ersetzt →
Cursor/Scroll springen, siehe `bugs/web/chrome-extensions.md` #74).
**FIX:** Die Live-Vorschau NICHT roh ins Zielfeld schreiben. Eigenes `<textarea>`: `value` relativ zum
Basis-Text setzen (still genug, EINE Quelle). Fremdes Feld: Vorschau in ein SEPARATES schwebendes Element,
Zielfeld bleibt bis zur finalen Fassung unberuehrt. Glaetten: kleiner Debounce (~120 ms), finale Woerter
deckend, interim gedimmt/kursiv optisch trennen.
**Quelle:** eigener Vorfall 2026-06-24. Gegenseite: `best-practices/desktop/voice-pipeline.md` §9.

## Fix-Status (Stand 2026-06-10, per gh verifiziert)

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| Wachfenster laeuft waehrend Rede ab + stilles Verwerfen | **GEFIXT in VoiceAgent 1.2.0** (#46675) | §1.1, §1.2 |
| Endlos-Aufnahme ohne Max-Deckel | **GEFIXT in VoiceAgent 1.2.0** (MaxUtteranceMs=30 s) | §2.1 |
| Verstehens-Schritt 6,6 s (Effort high) | **GEFIXT in VoiceAgent 1.2.0** (Brain→Effort low) | §4.1 |
| NAudio DataAvailable versiegt (#1168) | CLOSED COMPLETED — Watchdog bleibt sinnvoll | §3.1 |
| NAudio ThreadPool-Block (#539) | CLOSED (per Design dokumentiert) — Muster bleibt | §3.3 |
| NAudio AccessViolation/Block/Deadlock (#1150, #657, #1084, #1203) | **alle OPEN** — Workarounds dauerhaft | §3.2 |
| sherpa Stream-Leak Python-API (#2265) | CLOSED COMPLETED (Destroy nachgeruestet) — C# Dispose bleibt Pflicht | wake-word #9 |
| sherpa KWS-Latenz mit VAD-Gate (#2683) | CLOSED COMPLETED — bestaetigt: kein VAD vor KWS | wake-word #33 |
| Groq word-granularity leere segments (vercel/ai#12119) | OPEN (siehe groq-transkription §3.5) | §6 |

**Noch NICHT gefixt (per Design / Plattform — Workarounds dauerhaft):**
- §2.1 RMS-VAD vs. Geraeusch: prinzipbedingt — Max-Deckel + adaptiver Floor + (optional) Silero bleiben.
- §2.4 Windows-AGC, §5.2 Ducking/RAW: Treiber-/OS-Verhalten.
- §5.1 Barge-in: braucht AEC-Ausbau (VoiceAgent Phase 3) — bis dahin PauseMic-Trade-off.

**Ehrlichkeits-Hinweis:** Issue-Status per `gh issue view` am 2026-06-10 geprueft (OPEN/CLOSED wie
markiert). Alexa-/Google-Fensterwerte stammen aus Hersteller-Support-Doku bzw. etablierter
Sekundaerliteratur; exakte Amazon-Endpointing-ms sind NICHT oeffentlich → nicht behauptet.
Latenz-Richtwerte (channel.tel/Sierra/LiveKit) sind Engineering-Blogs der Anbieter (offiziell fuer
deren Stacks), keine Normen.

---

## Kopplung zur Best-Practices-Gegenseite (`best-practices/desktop/voice-pipeline.md`)

| Almanach-Abschnitt (hier) | Best-Practice-Abschnitt (Praevention) |
|---------------------------|----------------------------------------|
| §1 Wachfenster/Session | BP §1 Zustandsautomat & Fenster-Design |
| §2 VAD/Endpointing | BP §2 Endpointing richtig dimensionieren |
| §3 NAudio-Capture | BP §3 Robuste Daueraufnahme |
| §4 Latenz/Architektur | BP §4 Latenz-Budget & Pipeline-Ueberlappung |
| §5 Barge-in/Self-Trigger | BP §5 Echo & Unterbrechbarkeit |
| §6 STT-Anbindung | BP §6 STT-Requests fuer Voice |

---

## Pflicht-Checkliste vor Arbeit an einer Sprach-Pipeline

- [ ] Wachfenster-Timer: laeuft er NUR im Leerlauf (nicht waehrend Rede/Antwort)? Zaehlt er ab Antwort-Ende?
- [ ] Wird eine Aufnahme, die im Fenster begann, garantiert verarbeitet (nie still verworfen)?
- [ ] Max-Utterance-Deckel gesetzt (15–30 s) mit VERARBEITEN statt Verwerfen?
- [ ] Stille-Pause zum Use-Case passend (Konversation 300–550 ms, Diktat 1000–2000 ms) + semantisches Netz?
- [ ] Zustandswechsel + Fenster-Events als CHECKPOINT geloggt (Observability)?
- [ ] LLM-Zwischenschritte: kleinstes ausreichendes Modell/Effort? Parallelisierbar?
- [ ] NAudio: Watchdog gegen stilles Versiegen? Stop/Dispose serialisiert, nie im Callback?
- [ ] Windows: AGC/Boost/Ducking/Exclusive-Mode geprueft (mmsys.cpl)?
- [ ] STT: language explizit, Timeout kurz, ein statischer HttpClient?
- [ ] Nach neuem erlebtem Bug: Eintrag hier ergaenzen + Stand-Header aktualisieren.


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [voice-assistant-trigger](../android/voice-assistant-trigger.md)
- [groq-transkription](groq-transkription.md)
- [wake-word](wake-word.md)
