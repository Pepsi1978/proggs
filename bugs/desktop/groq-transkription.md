# Bekannte Bugs & Fallen: Audio-Transkription mit Groq (OpenAI Whisper large-v3 / turbo)

> **PFLICHT-LESEN vor JEDER echten Arbeit am Groq-Speech-to-Text-Pfad** (Mikrofon-Aufnahme →
> WAV → `POST /openai/v1/audio/transcriptions`). Betrifft `GroqWhisperClient`, `AlwaysOnListener`
> und jede App, die Whisper ueber Groq nutzt. Trivialer Kleinkram (String, Doku) ausgenommen.
> Loesungen sind **funktionserhaltend** — echte (auch leise/kurze) Sprache darf NIE verworfen werden.
>
> **Stand:** recherchiert am **2026-06-08**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax; keine neuen belastbaren STT-Bugs seit dem Stichtag) (7 Researcher parallel, offizielle Groq-Doku + OpenAI-
> Whisper-Repo + peer-reviewed Paper zuerst). Software: **Groq Speech-to-Text API** mit
> `whisper-large-v3` und `whisper-large-v3-turbo` (Cloud, keine lokale Version). App-Kontext:
> VoiceAgent (.NET 10, WPF, NAudio, 16 kHz mono PCM). GitHub-Status per `gh` verifiziert.
>
> **Zugehoerige Best Practices:** [`best-practices/desktop/groq-transkription.md`](../../best-practices/desktop/groq-transkription.md)
> (zweite Seite: wie man die Pipeline von vornherein richtig baut).

> **Update 2026-07-02:** Keine belegten neuen Groq-Whisper-Halluzinations-/Rate-Limit-/Parameteraenderungen seit 2026-06-08 gefunden. Bestehende Abwehrkette bleibt massgeblich: VAD/Sprachgehalt vor Request, `verbose_json`, UND-Confidence-Gate, Blocklist nur bei Stille-Kontext.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Floskel bei Stille ("Vielen Dank") | Inhaerentes Whisper-Verhalten, nicht abschaltbar — Abwehr-Kette noetig | §1.1 |
| 2 | "Nichts gesagt"-Clip | Sprachgehalt-Vorfilter VOR dem Senden, nicht erst Confidence-Gate | §2.1, §2.3 |
| 3 | Response-Format waehlen | `verbose_json` (bei Groq gratis vs. `text`) fuer Confidence-Felder | §3.1 |
| 4 | Confidence-Gate bauen | Verwerfen nur `no_speech_prob>0.6` UND `avg_logprob<-1.0` — nie ODER | §2.3 |
| 5 | Halluziniertes Wort NACH echtem Satz | Schicht 3: Segment-Zeitfenster gegen RMS-Timeline abgleichen | §2.3 |
| 6 | Floskel-Blocklist | Nur bei kurz + exaktem Match + Stille-Kontext (echte Aussage bleibt) | §2.4 |
| 7 | `temperature=0` gesetzt | Reicht NICHT gegen Stille-Halluzination — Kette ergaenzen | §1.2 |
| 8 | `prompt` als Anti-Halluzination | Nur Stil/Vokabular, kurz, keine Befehle (sonst Leakage) | §1.3 |
| 9 | `language` setzen | ISO-639-1 (`de`), nicht `german`/`de-DE` | §3.4 |
| 10 | Extra-Parameter senden | Nur dokumentierte Groq-Felder (sonst 400) | §3.6 |
| 11 | 429 Rate-Limit | `retry-after`-Header lesen; 413/422 NICHT retryen | §4.2, §4.5 |
| 12 | Viele kurze Clips | Min-Abrechnung 10 s/Clip — buendeln/Vorfilter | §4.1 |

---

## 🔗 Bezug zu den Best Practices ([`best-practices/desktop/groq-transkription.md`](../../best-practices/desktop/groq-transkription.md))

Der Almanach sagt *was schiefgeht*; die Best-Practices-Datei sagt *wie man es von vornherein richtig macht*.

| Bug-Abschnitt (hier) | Praevention in best-practices-groq-transkription.md |
|----------------------|------------------------------------------------------|
| §1 Stille-Halluzination, §1.2 temperature, §1.3 prompt, §1.4 Repetition | BP 1 Aufnahme/Preprocessing, BP 2 Request-Konfig, BP 3 Nachfilter |
| §2 Abwehr-Kette (Vorfilter/Confidence/Blocklist) | BP 1 (Vorfilter), BP 3 (Confidence-Gate + Blocklist) |
| §3 Groq-Parameter/Format/Modell | BP 2 Request-Konfig, BP 4 Performance |
| §4 Rate-Limits/Kosten/Fehlercodes/Endpoint | BP 5 .NET-Resilienz, BP 7 Kosten, BP 8 Integration |

---

## 1. Stille-Halluzination (das Kernproblem)

### 1.1 Whisper erfindet bei Stille Outro-Floskeln  ⭐ HAEUFIG / Franks Vorfall
**Symptom:** Wenn niemand spricht (z.B. App im 60-s-Wachfenster, Mikrofon offen), liefert Groq
trotzdem Text zurueck — typisch eine kurze Floskel wie **„Vielen Dank"**, „Vielen Dank fuers
Zuschauen", „Untertitel des ZDF", englisch „Thank you" / „Thanks for watching".
**Ursache:** Whisper wurde auf 680k h Web-/YouTube-/TV-Audio mit Untertiteln trainiert. Outro-
Boilerplate steht dort oft an Stellen ohne Sprache. Das Modell hat gelernt: *leises/leeres Audio →
Outro-Text* und erfindet ihn bei Stille/Rauschen. Die Floskeln sind regulaere Tokens → nicht
abschaltbar. (~1,4 % aller Transkriptionen betroffen; „Thank you" = 24,8 % aller Halluzinationen.)
**Versionen:** alle Whisper-Modelle; **`large-v3` schlimmer** als large-v2/medium (die bei Stille
eher leer bleiben). Per Design — **NICHT gefixt** (siehe Fix-Status).
**FIX (funktionserhaltend, mehrschichtig — Reihenfolge = Wirkung):**
1. **Sprachgehalt-Vorfilter VOR dem Senden:** Clip nur senden, wenn er genug *echte* Sprache
   enthaelt (nicht nur Dauer!). Einen kurzen Stimm-Anteil/RMS-aktive-Dauer verlangen (z.B. ≥150–250 ms
   aktive Stimme bzw. VAD-bestaetigt). Optional Silero-VAD (ONNX) fuer hoehere Genauigkeit.
2. **Mindest-Sprechdauer-Gate** (konservativ, z.B. 0,3 s — kurze Befehle nicht abschneiden).
3. **`verbose_json`-Confidence-Gate** (§2.3): verwerfen wenn `no_speech_prob > 0.6` UND `avg_logprob < -1.0`.
4. **Floskel-Blocklist** (§2.4) als letzter Filter — nur bei kurzem Clip + Stille-Verdacht.
**Quelle:** Franks Vorfall 2026-06-08 (VoiceAgent) · [arXiv 2501.11378](https://arxiv.org/html/2501.11378v1) · [arXiv 2402.08021](https://arxiv.org/html/2402.08021v2) · [openai/whisper Disc#1606](https://github.com/openai/whisper/discussions/1606) · [t3n](https://t3n.de/news/openai-whisper-stille-namen-1698611/)

### 1.2 `temperature=0` allein verhindert es NICHT
**Symptom:** Trotz `temperature=0` (wie im VoiceAgent gesetzt) treten Stille-Floskeln auf.
**Ursache:** Whisper faehrt bei `temperature=0` intern eine Fallback-Treppe `[0.0,0.2,…,1.0]` und
erhoeht die Temperatur automatisch, wenn ein Segment die Schwellen-Checks reisst. `temperature=0`
ist also nur die Basis, kein Schutz gegen Stille-Halluzination.
**Versionen:** alle. Per Design.
**FIX:** `temperature=0` beibehalten (sinnvoll als Basis), aber die Kette aus §1.1 ergaenzen.
**Quelle:** [OpenAI Community 125300](https://community.openai.com/t/how-to-avoid-hallucinations-in-whisper-transcriptions/125300)

### 1.3 `initial_prompt`/`prompt` ist nur Stil-Lenkung, kein Halluzinations-Stop
**Symptom:** Erwartung, ein `prompt` verhindere die Floskeln — tut es nicht zuverlaessig.
**Ursache:** `prompt` (Groq: max **224 Tokens**) lenkt nur Schreibweise/Stil/Vokabular. Falsch
genutzt (Anweisungen statt Vokabular) wird er sogar **mit-transkribiert** (Leakage) → eigene Halluzination.
**Versionen:** alle.
**FIX:** `prompt` nur fuer Eigennamen/Fachbegriffe/Schreibweisen, kurz, keine Befehle. Nicht als
Anti-Halluzinations-Hauptmittel einsetzen.
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 1.4 Wiederholungs-Halluzination ("danke danke danke…")
**Symptom:** Ein Wort/Satz wird in leisen Passagen endlos wiederholt.
**Ursache:** Decoder-Drift; `condition_on_previous_text` reicht vorigen (Fehl-)Text als Kontext weiter.
**Versionen:** alle.
**FIX:** Server-seitig per `compression_ratio > 2.4` erkennen und verwerfen (§2.3). In lokalem
Whisper zusaetzlich `condition_on_previous_text=False` — bei Groq nicht als Parameter verfuegbar,
daher hier ueber das compression_ratio-Gate abfangen.
**Quelle:** [openai/whisper Disc#2420](https://github.com/openai/whisper/discussions/2420) · [whisper-api Optionen](https://whisper-api.com/docs/transcription-options/)

---

## 2. Die funktionserhaltende Abwehr-Kette (Praxis)

### 2.1 Sprachgehalt-Vorfilter — die wirksamste Einzelmassnahme
**Problem:** Ein kurzer Geraeusch-Peak (Atmen, Tastatur, Luefter, Tuerklicken) ueber der RMS-Schwelle
startet die Aufnahme; danach folgt fast nur Stille. Eine reine **Dauer**-Pruefung (`minUtteranceMs`)
laesst so einen fast-stillen Clip durch → Groq halluziniert.
**Ursache:** Aufnahme-Trigger ≠ Sprachgehalt. Dauer allein misst nicht, *wie viel* echte Stimme drin ist.
**FIX (funktionserhaltend):** Beim Finalisieren den **Anteil aktiver (lauter) Frames** bzw. die
**aufsummierte Voiced-Dauer** messen und nur senden, wenn ein Mindest-Sprachgehalt erreicht ist
(z.B. ≥150–250 ms ueber Schwelle, ODER Voiced-Ratio ≥ ~15–20 %). Konservativ einstellen — lieber
einmal zu viel senden als echte kurze Sprache verwerfen. Optional Silero-VAD (ONNX, .NET via
`VadSharp`/`ManySpeech.SileroVad`) fuer hoehere Genauigkeit; reines RMS-Gate reicht als erster Schritt.
**Heuristik-Werte (16 kHz mono):** Frame 20 ms; RMS-aktiv wenn ueber bestehender `SilenceThreshold`;
adaptiver Noise-Floor (`thr ≈ 3× noise`, `noise = 0.95·noise + 0.05·cur` nur in Stille) macht es robust.
**Im Browser / Chrome-Erweiterung (verifiziert 2026-06-09, overlays):** `MediaRecorder` liefert
KOMPRIMIERTES `audio/webm;codecs=opus` — RMS NICHT direkt auf dem Blob messbar. Erst per Web Audio
dekodieren: `await new AudioContext().decodeAudioData(await blob.arrayBuffer())`, dann `getChannelData(0)`
(Float32) und RMS pro 20-ms-Frame (`frame = round(sampleRate*0.02)`), aktive Frames zaehlen. Vorfilter
gehoert ins **Content-Script** (DOM/Web-Audio da) VOR dem `sendMessage` an den Service Worker — der SW
hat kein Web-Audio. Schwellen wie Desktop: RMS ≥ 0.015, min. 150 ms absolute laute Zeit (Toggle-Mic =
keine Ratio). Decode-Fehler -> NICHT filtern, trotzdem senden (funktionserhaltend).
**Quelle:** [snakers4/silero-vad](https://github.com/snakers4/silero-vad) · [Picovoice VAD-Guide](https://picovoice.ai/blog/complete-guide-voice-activity-detection-vad/) · [dev.to Voice-AI-Guide](https://dev.to/programmerraja/2025-voice-ai-guide-how-to-make-your-own-real-time-voice-agent-part-3-3ocb)

### 2.2 Mindest-Sprechdauer-Gate
**Problem:** Sehr kurze Clips loesen am haeufigsten Halluzinationen aus.
**FIX:** Erst senden, wenn bestaetigte Sprache eine Mindestschwelle ueberschreitet. Praxiswerte:
0,3 s (kurze Befehle wichtig) bis 1,5 s (strenger). VoiceAgent hat `MinUtteranceMs` (350 ms) —
aber das misst Dauer, nicht Sprachgehalt; mit §2.1 kombinieren.
**Quelle:** [Hermes Voice Mode](https://hermes-agent.nousresearch.com/docs/user-guide/features/voice-mode)

### 2.3 `verbose_json`-Confidence-Gate (server-seitige Zweitpruefung)  ⭐ WICHTIG
**Problem:** Was die VAD am Chunk-Rand durchlaesst, muss nach der Transkription gefangen werden.
**Ursache:** `response_format=text` liefert KEINE Confidence-Felder → kein Filtern moeglich.
**FIX (funktionserhaltend):** `response_format=verbose_json` anfordern (bei Groq **keine Mehrlatenz/
-kosten** vs. `text`, §3). Pro Segment liefert Groq `no_speech_prob`, `avg_logprob`,
`compression_ratio`. Verwerfen mit **UND-Logik**:
```
Stille:     no_speech_prob > 0.6  UND  avg_logprob < -1.0
Repetition: compression_ratio > 2.4
Mini-Noise: segment_dauer < 0.4 s  UND  no_speech_prob > 0.6
```
**KRITISCH:** UND, nicht ODER. Echte leise Sprache hat zwar evtl. erhoehtes `no_speech_prob`, aber
ein gutes (nicht so negatives) `avg_logprob` → bleibt erhalten. Mit ODER fielen Fluester-Segmente raus.
Zu aggressiv? Zuerst `no_speech`-Schwelle auf 0.7 anheben, NICHT `avg_logprob` lockern.
**⭐ WICHTIG — das Gate allein faengt REINE Stille NICHT (verifiziert 2026-06-08, Push-to-Talk-Overlay):**
Bei „Aufnahme gestartet, aber NICHTS gesagt" halluziniert Whisper die Floskel mit **HOHER** Confidence —
also **niedrigem** `no_speech_prob` UND **gutem** `avg_logprob`. Die UND-Bedingung greift dann nicht, das
Segment ueberlebt. Verschaerfend: **ultrakurze Clips liefern oft GAR KEINE `segments`** (nur top-level
`text`), sodass jeder Segment-Filter ins Leere laeuft und der top-level-Fallback die Floskel durchreicht.
**Konsequenz:** Fuer den „nichts gesagt"-Fall ist NICHT das Confidence-Gate die Loesung, sondern der
**Sprachgehalt-Vorfilter §2.1 VOR dem Senden** (gar nicht erst an Groq schicken). Bei Push-to-Talk genuegt
ein Vorfilter auf **absolute** laute Zeit (z.B. ≥150 ms RMS-aktiv) — KEINE Voiced-Ratio (sonst fielen echte
Aufnahmen mit langen Denkpausen heraus). Das Confidence-Gate bleibt fuer Pausen MITTEN in echter Sprache zustaendig.

**⭐ ZWEITE LUECKE — Trailing-Halluzination NACH echter Sprache (verifiziert 2026-06-09, overlays):**
Frank sprach einen echten Satz, machte dann am Ende eine Pause (Mic lief weiter), stoppte — Whisper haengte
ein halluziniertes **„Ja"** an. Weder Schicht 1 noch 2 faengt das: Der **Vorfilter §2.1 greift NICHT**
(Gesamt-Sprachgehalt ist hoch — der echte Satz), und das **Confidence-Gate greift NICHT** (das Trailing-Wort
kommt mit HOHER Confidence). Eine reine Floskel-Blocklist (§2.4) wuerde ein ECHTES „Ja" mitloeschen → kein
Funktionserhalt. **Loesung = SCHICHT 3: Segment-Audio-Abgleich.** `verbose_json` liefert pro Segment
`start`/`end`; pruefe das Zeitfenster gegen die eigene RMS-/Voiced-Timeline der Aufnahme — ist es dort
praktisch still (< ~10 % laute Frames), ist das Segment eine Halluzination und wird verworfen; echtes Wort
hat Schall im Fenster → bleibt. Sicherung: wuerde der Abgleich ALLE Segmente verwerfen (Whisper-Timestamps
koennen driften), Roh-Transkript behalten statt den Satz zu verlieren. Gilt plattformuebergreifend
(Desktop wie Browser); im Browser ist die Voiced-Timeline ohnehin aus §2.1 vorhanden.
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text) · [Groq Cookbook](https://deepwiki.com/groq/groq-api-cookbook/2-speech-and-audio-processing) · [gradio VAD](https://www.gradio.app/guides/automatic-voice-detection) · Franks Live-Test 2026-06-08 (TVO/CVO)

### 2.4 Floskel-Blocklist (letzter Filter, mehrsprachig + normalisiert)
**Problem:** Vereinzelte Floskeln rutschen trotz §2.1–2.3 durch.
**Ursache:** VAD/Schwellen sind nicht perfekt am Rand.
**FIX (funktionserhaltend — GOLDENE REGEL: Floskel allein NIE verwerfen):** Nur verwerfen, wenn
**mehrere Signale zugleich** zutreffen: (1) Ausgabe kurz (≤ ~6 Woerter / ≤64 Zeichen) UND
(2) normalisierter **exakter** Match gegen Blocklist (lowercase, Satzzeichen/Whitespace strippen,
`==` nicht `contains`) UND (3) Stille-Kontext (hohes `no_speech_prob` bzw. der Clip war laut §2.1
sprach-arm). So bleibt eine echte kurze Nutzeraussage „Vielen Dank" erhalten (dann ist
`no_speech_prob` niedrig → greift nicht).
**Floskel-Quelle:** Fertiges Dataset [`sachaarbonel/whisper-hallucinations`](https://huggingface.co/datasets/sachaarbonel/whisper-hallucinations)
(7.890 Phrasen ueber ~100 Sprachen). DE-Kern: „Vielen Dank", „Vielen Dank fuers Zuschauen",
„Untertitel des ZDF …", „Untertitelung des ZDF fuer funk", „Untertitel von …", „Bis zum naechsten Mal".
EN-Kern: „Thank you", „Thanks for watching", „Please subscribe", „Subtitles by the amara.org community".
**Quelle:** [arXiv 2501.11378 (BoH)](https://arxiv.org/html/2501.11378v1) · [HF Dataset](https://huggingface.co/datasets/sachaarbonel/whisper-hallucinations) · [Hermes (26 Phrasen + Regex)](https://hermes-agent.nousresearch.com/docs/user-guide/features/voice-mode)

### 2.5 NEGATIV-Erkenntnis: `suppress_non_speech_tokens` macht es SCHLIMMER
**Symptom:** Erwartung, „Nicht-Sprach-Tokens unterdruecken" helfe — fuehrte in whisper.cpp zu
*willkuerlichem* Text auf reiner Stille.
**Ursache:** Zusammenspiel mit `suppress_blank`; Default wurde deshalb auf `false` geaendert.
**FIX:** Nicht nutzen (bei Groq ohnehin kein Parameter). Hier nur als Warnung dokumentiert.
**Quelle:** [whisper.cpp Disc#1258](https://github.com/ggml-org/whisper.cpp/discussions/1258)

---

## 3. Groq-API: Parameter, Format, Audio

### 3.1 `verbose_json` ist „gratis" — `text` verschenkt die Filterfelder
**Detail:** Groq berechnet Audio-Sekunden, nicht das Output-Format. Segment-Metadaten (`verbose_json`)
verursachen **keine** Mehrlatenz/-kosten; nur `timestamp_granularities=["word"]` kostet extra Latenz.
**FIX:** Auf `verbose_json` umstellen (ohne `word`-Granularitaet), Confidence-Felder nutzen (§2.3).
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 3.2 Modellwahl: turbo vs. v3
**Detail:** `whisper-large-v3-turbo` — $0.04/h, WER ~12 %, nur **Transkription** (keine Translation).
`whisper-large-v3` — $0.111/h, WER ~10,3 %, kann auch Translation. Fuer deutsches Diktat ohne
Uebersetzung ist turbo ideal (VoiceAgent-Default). **Falle:** turbos `translation` geht nicht —
fuer non-EN→EN `whisper-large-v3` nehmen.
**Quelle:** [Groq Turbo-Blog](https://groq.com/blog/whisper-large-v3-turbo-now-available-on-groq-combining-speed-quality-for-speech-recognition)

### 3.3 Audio-Aufbereitung: 16 kHz mono WAV
**Detail:** Offizielle Empfehlung: vor dem Senden auf **16 kHz mono** downsamplen; **WAV = geringste
Latenz**. Nur die **erste** Audiospur wird verarbeitet (Multi-Track/Stereo → Rest ignoriert).
**FIX:** Direkt als 16 kHz mono WAV aufnehmen (VoiceAgent tut das bereits) — vermeidet serverseitiges
Resampling.
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 3.4 `language` muss ISO-639-1 sein
**Symptom:** schlechtere Accuracy/Latenz oder Auto-Detect-Fehler.
**FIX:** `de` senden — NICHT `german`/`de-DE`. (VoiceAgent: `SttLanguage="de"` korrekt.)
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 3.5 `word`-Granularitaet → leere `segments` (Groq-Bug, OPEN)
**Symptom:** `timestamp_granularities=['word']` liefert leeres `segments`-Array.
**Ursache:** fehlende OpenAI-Paritaet in Groqs Antwort.
**Versionen:** aktuell — **OPEN** (vercel/ai #12119, per gh verifiziert 2026-06-08).
**FIX:** beide Granularitaeten anfordern (`['word','segment']`) ODER nur `segment` nutzen. Fuer das
Confidence-Gate (§2.3) reicht `segment` — `word` gar nicht erst anfordern.
**Quelle:** [vercel/ai #12119](https://github.com/vercel/ai/issues/12119)

### 3.6 400 BadRequest bei nicht-dokumentierten Parametern
**Symptom:** `400 invalid_request_error`.
**Ursache:** OpenAI-spezifische/extra Felder (z.B. `condition_on_previous_text`, `no_speech_threshold`)
an Groq geschickt — Groq akzeptiert nur die dokumentierten Params.
**FIX:** Nur dokumentierte Felder senden (`model`, `file`/`url`, `language`, `prompt`,
`response_format`, `temperature`, `timestamp_granularities`). Die Whisper-internen Schwellen lassen
sich bei Groq NICHT setzen → Filterung client-seitig ueber `verbose_json` (§2.3).
**Quelle:** [litellm #11402](https://github.com/BerriAI/litellm/issues/11402)

---

## 4. Rate-Limits, Kosten, Fehlercodes

### 4.1 Min-Abrechnung 10 s pro Clip  ⭐ kostenrelevant
**Symptom:** Hohe Kosten bei vielen kurzen Aussagen.
**Ursache:** Jeder Clip wird mit **mindestens 10 s** abgerechnet (Min-Dauer 0,01 s).
**FIX:** Sehr kurze Clips vermeiden/buendeln. Zusaetzliches Argument fuer den Vorfilter (§2.1):
nicht jede 350-ms-Stille als 10-s-Request abrechnen lassen.
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 4.2 Rate-Limit 429 — RPM UND Audio-Sekunden
**Symptom:** 429 trotz weniger Requests.
**Ursache:** Free-Tier `whisper-large-v3(-turbo)`: **RPM 20**, **RPD 2.000**, **ASH 7.200 Audio-Sek/h**,
**ASD 28.800/Tag**. 429 feuert, sobald RPM **oder** das Audio-Sekunden-Kontingent reisst.
**FIX:** Bei 429 den **`retry-after`-Header** lesen und exakt so lange warten + exponential backoff
(+Jitter) — NICHT blind sofort retryen. `x-ratelimit-remaining-*` proaktiv ueberwachen.
**Hinweis VoiceAgent:** Der aktuelle `GroqWhisperClient` retryt mit festen Delays `[2s,4s,8s]` und
liest `retry-after` NICHT → bei 429 ggf. zu frueh. Verbesserung: `retry-after` auswerten.
**Quelle:** [Groq Rate-Limits](https://console.groq.com/docs/rate-limits) · [Grizzly Peak 2026](https://www.grizzlypeaksoftware.com/articles/p/groq-api-free-tier-limits-in-2026-what-you-actually-get-uwysd6mb)

### 4.3 Datei zu gross → STILLER Fehlschlag / 413
**Symptom:** leeres Transkript ohne klare Fehlermeldung.
**Ursache:** **25 MB (free) / 100 MB (dev)** ueberschritten.
**FIX:** `url`-Parameter statt Upload, oder Chunking mit ueberlappenden Segmenten + Merge. (Fuer kurze
Voice-Clips irrelevant, bei langen Aufnahmen beachten.)
**Quelle:** [steipete/summarize #124](https://github.com/steipete/summarize/issues/124)

### 4.4 HTTP 200 + leere/Muell-Antwort
**Symptom:** 200 OK, aber Transkript leer oder Nonsens.
**FIX:** Bei `verbose_json` die Metadaten als Qualitaets-Gate pruefen (§2.3). Leere Antwort nach
Erfolg nicht als „Fehler" werfen, sondern als „nichts Verwertbares" behandeln (still verwerfen) —
gerade im Always-On-Betrieb haeufig und normal.
**Quelle:** [Groq STT Docs](https://console.groq.com/docs/speech-to-text)

### 4.5 Fehlercodes auseinanderhalten
**Detail:** 413 = Datei zu gross (→ chunken) · 422 = wohlgeformt aber semantisch ungueltig (Format/
Param) · 429 = Rate-Limit (→ `retry-after`) · 498/499 = Kapazitaet (Flex)/Caller-Cancel · 500/503 =
serverseitig (schlicht erneut versuchen). VoiceAgent retryt aktuell nur 429/500/503 — 413/422 sind
NICHT retrybar (waere Verschwendung).
**Quelle:** [Groq Errors](https://console.groq.com/docs/errors)

### 4.6 Endpoint-Pfad kanonisch halten
**Symptom:** „provider not available"/Audio bricht ab nach SDK-Update.
**Ursache:** deprecated Legacy-Compat-Subpaths.
**FIX:** Kanonischen Pfad `https://api.groq.com/openai/v1/audio/transcriptions` verwenden (VoiceAgent
tut das bereits via `DefaultUrl`).
**Quelle:** [openclaw #59875](https://github.com/openclaw/openclaw/issues/59875)

---

## Fix-Status (Stand 2026-06-08, per gh verifiziert)

| Frueherer Bug / Erwartung | Status | Bezug |
|---------------------------|--------|-------|
| Stille-Halluzination (Kernproblem, §1.1) | **NICHT gefixt — per Design**, `large-v3` schlimmer | inhaerent, Workaround-Kette bleibt aktiv |
| `hallucination_silence_threshold` (openai/whisper PR #1838) | **MERGED** 2023-12 in OpenAI-Whisper, aber **bei Groq NICHT als Parameter exponiert** | nur lokal nutzbar; bei Groq via §2.3-Gate ersetzen |
| Silero-VAD „Meta"-Halluzinationen (faster-whisper #843) | **OPEN** | VAD ist nicht perfekt → §2.4 als Nachfilter noetig |
| Groq `word`-granularity → leere segments (vercel/ai #12119) | **OPEN** | §3.5 — `word` meiden, `segment` nutzen |

**Ehrlichkeits-Hinweis:** Die Stille-Halluzination ist KEIN versionierter, „demnaechst behobener" Bug,
sondern Eigenschaft des Whisper-Modells. Sie verschwindet nicht durch ein Update — die
funktionserhaltende Abwehr-Kette (§2) ist die dauerhafte Loesung. GitHub-Diskussionen (#1606, #679)
sind Diskussionen (kein OPEN/CLOSED-Status); PR/Issue-Status oben sind per `gh` belegt.

---

## VoiceAgent — konkreter Fix-Vorschlag (GroqWhisperClient + AlwaysOnListener)

Ist-Zustand: `AlwaysOnListener` triggert per RMS, prueft aber nur **Dauer** (`MinUtteranceMs`), nicht
Sprachgehalt. `GroqWhisperClient` nutzt `response_format=text` (keine Confidence-Felder). → Stille mit
einem kurzen Peak rutscht durch → „Vielen Dank".

Funktionserhaltende Kette (klein → gross):
1. **AlwaysOnListener:** beim `FinalizeUtterance` die **Voiced-Dauer** mitzaehlen (Summe der Frames mit
   `rms ≥ SilenceThreshold`). Clip verwerfen, wenn Voiced-Dauer < ~200 ms ODER Voiced-Ratio < ~15 %
   (konservativ, neue Settings `MinVoicedMs`/`MinVoicedRatio`). Verhindert die meisten Faelle ohne API-Call.
2. **GroqWhisperClient:** `response_format=verbose_json`, Segmente mit `no_speech_prob>0.6 AND
   avg_logprob<-1.0` bzw. `compression_ratio>2.4` herausfiltern; bei leerem Rest leeren String liefern
   (MainWindow behandelt leer bereits korrekt).
3. **Floskel-Blocklist** (normalisiert, kurz + Stille-Kontext) als letzter Nachfilter.
4. **Bonus:** `retry-after`-Header im 429-Retry auswerten (§4.2).

Alle Schritte sind verlustfrei: echte (auch leise/kurze) Sprache hat niedriges `no_speech_prob` und
ausreichend Voiced-Dauer → bleibt erhalten.

---

## Pflicht-Checkliste vor Arbeit am Transkriptions-Pfad

```
□ Sende ich Stille/sprach-arme Clips? → Vorfilter (Sprachgehalt/VAD) VOR dem Senden (§2.1)
□ response_format = verbose_json (nicht text)? Confidence-Gate mit UND-Logik (§2.3)
□ Floskel-Blocklist nur bei kurz + Stille-Kontext (echte Aussage bleibt) (§2.4)
□ language = ISO-639-1 ("de"), temperature = 0 (§3.4, §1.2)
□ Nur dokumentierte Groq-Parameter (keine OpenAI-Whisper-internen) (§3.6)
□ 429: retry-after lesen; 413/422 NICHT retryen (§4.2, §4.5)
□ Min-Billing 10s bedacht (keine Flut winziger Clips) (§4.1)
□ Funktionserhaltend: keine echte Sprache verworfen (konservative Schwellen)
```


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [voice-assistant-trigger](../android/voice-assistant-trigger.md)
- [groq-api](../apis/groq-api.md)
- [voice-pipeline](voice-pipeline.md)
- [wake-word](wake-word.md)
