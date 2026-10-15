# Transkription mit Silero-VAD-Sprachaktivität (Android, Groq Whisper) — Best Practices

**Stand:** 15.10.2026, lokal aus dem Code (keine Webrecherche). Bewährt in Jarvis 1.8.73 (Commit `eb004f5fa`,
„Fahrgeräusch hält das Mikrofon nicht mehr offen“), von Frank im Alltag bestätigt.
**Fertiger Baustein:** Modul **M1.4 Transkriptionsmodul** (`Module/Android/M1.4-Transkriptionsmodul/`) — einbauen
mit dem Skill `modul-einbauen` statt neu schreiben.
**Gegenstück:** [`desktop/groq-transkription.md`](../desktop/groq-transkription.md) (4-Schichten-Kanon, Groq-Parameter),
Bug-Almanach [`bugs/desktop/groq-transkription.md`](../../bugs/desktop/groq-transkription.md).

---

## ⚡ Kurzcheck

| # | Situation | Regel | § |
|---|---|---|---|
| 1 | Neue Android-App mit Groq-Diktat | M1.4 einbauen, nicht neu bauen | 1 |
| 2 | Aufnahmeende im freihändigen Modus | Silero-Sprachrahmen statt Pegel; 4,5 s ohne Stimme → Ende | 3 |
| 3 | Lautes Umfeld (Auto, Lüfter) | Pegel taugt nicht: Geräusch liegt über der Pegelschwelle. Stimme zählt, nicht Lautstärke | 2 |
| 4 | Was filtert, was erkennt | Schichten 1–4 filtern Halluzinationen, Silero erkennt nur Sprachaktivität | 2 |
| 5 | Silero fällt aus | nie die Aufnahme opfern: Rückfall auf den Pegel, Filter laufen weiter | 5 |
| 6 | Groq bleibt Anbieter | Silero liefert keinen Text und erkennt nicht, *wer* spricht | 2 |
| 7 | Schwellen ändern | nur mit neuen Testfällen aus `vad_testfaelle.py` | 7 |

---

## 1. Integration

Modul M1.4 per `modul-einbauen` in die App kopieren (Kopie, keine Verdrahtung). Danach:

1. `assets/vad/silero_vad.onnx` + `LIZENZ.txt` nach `app/src/main/assets/vad/`.
2. Anbindung (`Anbindung.kt` neben der Kopie): `MicRecorder(context, kleidiaiAbschalten)`, optional
   `Protokoll.aktiv` setzen, `Diktat(transcriber) { eigeneKorrektur(it) }`.
3. Freihändig: alle 50 ms `SprechEnde.neu(mikro.pegel.value, ms, mikro.sprachRahmen)`; bei `FERTIG` stoppen und
   `Diktat(...).transkribiere(wav, mikro.letzteStimmhaft)`.
4. Per Knopf: nur `start`/`stop` + `transkribiere(wav, mikro.letzteStimmhaft)` — die Filter greifen trotzdem.
5. **Immer `letzteStimmhaft` mitgeben.** Ohne Zeitachse fallen die Schichten 1, 3, 4 auf den Pegel zurück und
   greifen im Auto nicht (dort ist alles „laut“).

Aufrufbeispiel: `Module/Android/M1.4-Transkriptionsmodul/BEISPIEL.md`.

## 2. Die fünf Schutzbausteine — tatsächlicher Code

**Baustein 5 erkennt Sprache, die Schichten 1–4 filtern Halluzinationen.** Silero ist kein fünfter Textfilter,
sondern ersetzt in 1, 3, 4 und im Zuschnitt die Frage „war es laut?“ durch „war es eine Stimme?“ und bestimmt das
Aufnahmeende.

| # | Baustein | Art | Wo (M1.4) | Wirkung |
|---|---|---|---|---|
| 5 | Silero VAD v5.1.2 | **Sprachaktivität erkennen** | `SileroVad`, `SprachZeitachse`, `MicRecorder`, `SprechEnde` | je 32 ms (512 Werte bei 16 kHz) eine Wahrscheinlichkeit für Stimme. Zwei Sichten: `sprachRahmen` (streng, fürs Ende) und `stimmhaft()` (großzügig, für Filter und Zuschnitt) |
| 1 | Sprachgehalt-Vorfilter | **filtert** ganze Aufnahme | `GroqTranscriber.transcribe` | < 150 ms Stimme → kein Upload, Ergebnis leer |
| — | Zuschnitt der Geräuschphasen | spart Upload, nimmt Whisper die Rauschpause | `GroqTranscriber.stilleAbschneiden` | vor dem ersten und nach dem letzten stimmhaften Rahmen abschneiden, 700 ms Rand vorn, 1 200 ms hinten, nur wenn ≥ 500 ms gespart; die Zeitachse wird um den Schnitt verschoben |
| 2 | Confidence-Gate | **filtert** Segmente | `WhisperHallucinationFilter` | `no_speech_prob > 0,6` UND `avg_logprob < -1,0`; `compression_ratio > 2,4`; < 0,4 s UND `no_speech > 0,6`. Text aus behaltenen Segmenten neu zusammengesetzt |
| 3 | Segment-Audio-Abgleich | **filtert** Segmente | `WhisperHallucinationFilter`, `SpeechAnalysis.segmentHasSpeech` | < 10 % stimmhafte Rahmen im Segmentfenster → fällt. Würde alles fallen, bleibt das Ergebnis von Schicht 2 (Zeitstempel driften) |
| 4 | Floskel-Blocklist | **filtert** Gesamttext | `WhisperHallucinationFilter` | nur bei exaktem normalisiertem Treffer UND ≤ 8 Wörtern/≤ 64 Zeichen UND < 600 ms Stimme. Ohne Analyse nie |

Abweichung vom Desktop-Kanon: Schicht 4 lässt in Jarvis/M1.4 **≤ 8 Wörter** zu (Kanon nennt ≤ 6). Die Liste enthält
auch Floskeln aus dem Genialen Wecker (FR/ES-Untertitel).

Was Silero **nicht** tut: keinen Text erkennen (das bleibt Groq Whisper `whisper-large-v3-turbo`), keine Person
identifizieren (Sprecherprofil/„nur Frank“ ist in Jarvis ein eigenes Thema: `weckwort/Stimmprofil.kt`, nicht Teil
des Moduls).

## 3. Aufnahmeende im automatischen Sprachmodus (`SprechEnde`)

| Wert | Vorgabe | Bedeutung |
|---|---|---|
| `anlaufMs` | 250 | Piepton, wird nie gewertet |
| `wartenMs` | 7 000 (Jarvis: 9 000 ab der zweiten Runde) | ohne Sprache bis hier → `NICHTS_GEHOERT` |
| `stilleMs` | **4 500** | so lange keine Stimme nach dem letzten Wort → `FERTIG` |
| `hoechstMs` | 120 000 | Höchstdauer |

Mit Silero zählt als Sprache nur, wenn `sprachRahmen` seit dem letzten Blick gewachsen ist. `SprachZeitachse`
beginnt Sprache erst nach **3 Rahmen in Folge ≥ 0,5** (≈ 100 ms; ein Schlagloch oder Blinkerklick setzt nichts in
Gang) und hält sie, solange die Werte **≥ 0,35** bleiben. Der Zähler wächst nur — wer alle 50 ms nachsieht, verpasst
keinen 32-ms-Rahmen. Für Filter und Zuschnitt gilt großzügiger **≥ 0,3** je Rahmen, damit leise Wortanfänge und
Satzenden nicht als Stille zählen.

Warum nicht der Pegel: Motor-, Reifen-, Wind- und Lüftergeräusch sind oft lauter als leise Sprache und schwanken;
jede Spitze setzte den Stille-Timer zurück, das Mikrofon blieb offen, und Whisper bekam die Rauschpause mit und
hängte Wörter an.

## 4. Abhängigkeiten und Modelle

- **Silero VAD v5.1.2** (MIT), `silero_vad.onnx` 2,3 MB, Eingang wie im Original-OnnxWrapper: 64 Werte Kontext
  vom vorigen Rahmen + 512 neue = `[1, 576]`, Zustand `[2, 1, 128]`, `sr` als Skalar 16000. Nur 16 kHz.
- **ONNX Runtime Android** (Jarvis 1.31.0), ein Thread intra/inter. Nur **eine** ORT je App (sherpa-onnx ohne
  eigene `libonnxruntime.so`).
- **KleidiAI-Falle:** ORT ab 1.22 nutzt auf ARM KleidiAI mit SME2-Befehlen; Prozessoren mit SME ohne SME2 (Galaxy
  Fold 8) stürzen mit SIGILL ab → `mlas.disable_kleidiai=1` nur dort (`/proc/cpuinfo`: `sme` ja, `sme2` nein).
  Darum ist `kleidiaiAbschalten` ein Pflichtparameter.
- **Groq** `whisper-large-v3-turbo`, `language=de`, `response_format=verbose_json`, `temperature=0`; ein
  langlebiger OkHttp-Client (`GroqNetz`), beim Aufnahmestart vorgewärmt.
- Aufnahme 16 kHz mono PCM16, `AudioSource.VOICE_RECOGNITION`, kein Denoise/AGC.

## 5. Rückfall bei Ausfall der VAD (Code in `MicRecorder`)

| Auslöser | Folge |
|---|---|
| Modell lädt nicht (Asset fehlt, ORT-Fehler) | `vad` bleibt null, `sprachRahmen` und `letzteStimmhaft` null |
| Ausnahme mitten in der Aufnahme | Zeitachse wird verworfen, Aufnahme läuft weiter |
| Abtastrate ≠ 16 kHz (z. B. Stimm-Aufnahme 24 kHz) | ohne Silero |

Dann gilt überall wieder der Pegel: `SprechEnde` mit Schwelle `max(0,15; Grundrauschen × 1,5 + 0,05)` (Grundrauschen
fällt sofort, steigt langsam und läuft auch mit VAD mit, damit der Wechsel nahtlos ist), `SpeechAnalyzer` mit
RMS ≥ 0,015 je 20-ms-Rahmen und Zuschnitt relativ zum Rauschboden. Nie wird eine Aufnahme wegen der VAD verworfen.

## 6. Konfiguration

- `GroqTranscriber(filterStille, filterMetriken, filterZeitstempel, filterFloskeln)` — Schalter der App, Vorgabe an.
  `filterStille` schaltet Schicht 1 **und** den Zuschnitt. Jarvis: Einstellungen „Filter“.
- `SprechEnde(...)`-Zeiten als Konstruktorparameter. Schwellen von Silero (`EIN`, `AUS`, `STIMMHAFT`,
  `ANLAUF_RAHMEN`) und der Filter sind Konstanten im Modul — Änderung nur im Modul mit neuen Tests.
- Lange Diktate: `Diktat` schneidet ab 20 MB in Teile um 16 MB an der leisesten Stelle der letzten 45 s
  (Groq 413). Die Zeitachse wird dann nur bei einem einzigen Teil mitgegeben.

## 7. Grenzen

- Silero erkennt **jede** menschliche Stimme: Beifahrer, Radio-Sprecher, Fernseher halten die Aufnahme offen und
  gehen mit in die Mitschrift. Dagegen hilft nur Sprechererkennung (nicht Teil von M1.4).
- Ein echt gesprochenes kurzes „Vielen Dank“ mit < 600 ms Stimme fällt durch Schicht 4 — gewollter Kompromiss.
- Sehr leise Sprache unter starkem Geräusch (−10 dB unter Fahrgeräusch) wird erkannt, Silbenanfänge können aber
  unter 0,3 fallen; der Zuschnitt-Rand (700/1 200 ms) fängt das ab.
- Zeitachse und WAV müssen zusammengehören: `letzteStimmhaft` gleich nach `stop()` lesen.
- Nur 16 kHz; Geräte, die 16 kHz verweigern, laufen über den Pegel.
- Testdaten sind TTS-Sprache mit synthetischem Fahr-/Lüftergeräusch, keine echten Handymitschnitte.

## 8. Tests

`./gradlew :app:testDebugUnitTest --tests "de.frank.module.transkriptionsmodul.*"` (JVM, ohne Gerät). Die Tests
spielen echte Silero-Werte je 32-ms-Rahmen aus `test/resources/vad/*.txt` im 50-ms-Takt ab wie auf dem Handy.

| Zusage | Testfälle |
|---|---|
| Fahr-/Lüftergeräusch hält die Aufnahme nicht offen | `auto_nur_fahrt`, `luefter_nur` → `NICHTS_GEHOERT` nach 7 s, 0 Sprachrahmen; `auto_sprache_dann_fahrt`, `luefter_sprache_dann_luefter`, `auto_pause` → Ende 4,5–5,1 s nach dem letzten Wort |
| Stille Halluzinationen fallen | Schicht 1 bei reinem Geräusch; Schicht 3 entfernt angehängtes „Und tschüss.“/„Ja.“ im Geräusch; Schicht 4 entfernt ZDF-/Zuschauen-Floskeln |
| Echte kurze Äußerungen bleiben | `kurz_ja_ruhig`, `kurz_stopp_auto`: Aufnahmeende erkennt sie, Schicht 1 lässt sie hoch, Schichten 2–4 lassen den Text stehen; „Vielen Dank.“ mit Sprache bleibt |
| Rückfall | VAD fällt mitten in der Aufnahme aus → Pegel beendet sie; ohne Zeitachse misst Schicht 1 den Pegel |

Neue Fälle: `werkzeuge/vad_testfaelle.py <ORDNER> <ZIEL> fall1,fall2` (TTS-WAVs 16 kHz, z. B. Windows-Stimme
„Hedda“ über System.Speech, plus `silero_vad.onnx`); rechnet Silero genau wie `SileroVad.kt` und schreibt nur die
genannten Fälle. Nicht automatisch geprüft: Upload (feste URL) und `MicRecorder` (AudioRecord).
