# M1.4 — Transkriptionsmodul

Sprachaufnahme bis fertiger Text über Groq Whisper, mit fünf Schutzbausteinen: lokale
Sprachaktivitätserkennung (Silero VAD) für das Aufnahmeende und die Sprachzeitachse, dazu die vier Schichten
gegen stille Whisper-Halluzinationen. Fahr- und Lüftergeräusch halten das Mikrofon nicht offen, reine
Geräuschaufnahmen gehen gar nicht erst an Groq, angehängte Floskeln fallen, kurze echte Äußerungen bleiben.

- **Stand:** v4
- **Plattform:** Android (Kotlin, ohne Compose)
- **Angelegt:** 15.10.2026 20:02
- **Ordner:** `Module/Android/M1.4-Transkriptionsmodul/`
- **Kurzname:** `transkriptionsmodul` — steckt im Namensraum `de.frank.module.transkriptionsmodul`

Ausführliche Begründung, Schwellen, Grenzen und Integration: `best-practices/android/transkription-silero-vad.md`.

## Herkunft

- **App:** Jarvis (Sprachschleife „Hey Jarvis“, Mikrofon-Knopf, Diktat in Merklisten)
- **Commit:** `602aefe23` (Silero-VAD-Stand aus `eb004f5fa`, Jarvis 1.8.73)
- **Quellstand:** `602aefe23` — vor dem ersten Kopieren genommen
- **Dateien:**
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/SprachErkennung.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/MicRecorder.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/weckwort/SprechEnde.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/SpeechAnalyzer.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/WhisperHallucinationFilter.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/GroqTranscriber.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/GroqModels.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/GroqNetz.kt`
  - `Jarvis/app/src/main/java/de/frank/jarvis/audio/Diktat.kt`
  - `Jarvis/app/src/main/assets/vad/silero_vad.onnx`, `LIZENZ.txt`
  - Tests `HalluzinationsSchichtenTest`, `SprechEndeTest`, `SprechEndeVadTest`, Testdaten `test/resources/vad/`,
    Werkzeug `Jarvis/werkzeuge/vad_testfaelle.py`

## Die fünf Schutzbausteine — wer filtert, wer erkennt

| # | Baustein | Art | Datei | Kern |
|---|---|---|---|---|
| 5 | **Sprachaktivität (Silero VAD)** | **erkennt Sprache**, filtert keinen Text | `SprachErkennung.kt`, `MicRecorder.kt`, `SprechEnde.kt` | je 32-ms-Rahmen Wahrscheinlichkeit für menschliche Stimme. Liefert (a) den Zähler `sprachRahmen` fürs Aufnahmeende, (b) die Zeitachse `stimmhaft` für die Schichten 1, 3, 4 und den Zuschnitt |
| 1 | Sprachgehalt-Vorfilter | filtert (ganze Aufnahme) | `GroqTranscriber.transcribe` + `SpeechAnalyzer` | weniger als 150 ms Stimme → nicht hochladen, Ergebnis `""` |
| 2 | Confidence-Gate | filtert (Segment) | `WhisperHallucinationFilter` | `no_speech_prob > 0,6` **und** `avg_logprob < -1,0`; `compression_ratio > 2,4`; Segment < 0,4 s mit `no_speech > 0,6` |
| 3 | Segment-Audio-Abgleich | filtert (Segment) | `WhisperHallucinationFilter` + `SpeechAnalysis.segmentHasSpeech` | < 10 % stimmhafte Rahmen im Segmentfenster → fällt; würde alles fallen, bleibt Schicht 2 stehen (Drift) |
| 4 | Floskel-Blocklist | filtert (Gesamttext) | `WhisperHallucinationFilter` | exakter normalisierter Treffer, ≤ 8 Wörter, ≤ 64 Zeichen **und** < 600 ms Stimme |

Baustein 5 ist kein fünfter Textfilter in der Kette, sondern hebt 1, 3 und 4 von „laut“ auf „Stimme“ und
bestimmt, wann die Aufnahme endet. Silero erkennt nur, *dass* ein Mensch spricht — keine Wörter und nicht *wer*.

## Was drin ist

| Teil | Was |
|---|---|
| `MicRecorder(context, kleidiaiAbschalten)` | 16-kHz-Aufnahme (VOICE_RECOGNITION), WAV-Ausgabe, `pegel` (StateFlow), `sprachRahmen` (live), `letzteStimmhaft` (nach `stop()`), lädt Silero im Hintergrund, wärmt die Groq-Verbindung vor |
| `SprechEnde(anlaufMs, wartenMs, stilleMs, hoechstMs)` | Aufnahmeende im freihändigen Modus: `neu(pegel, ms, sprachRahmen)` → `WEITER`/`FERTIG`/`NICHTS_GEHOERT` |
| `GroqTranscriber(apiKey, model, filterStille, filterMetriken, filterZeitstempel, filterFloskeln, prompt)` | `transcribe(wav, stimmhaft)`: Schicht 1 → Zuschnitt → Upload → Schichten 2–4. `model`: `MODELL_TURBO` (Standard) oder `MODELL_LARGE_V3`; ist large-v3 für die Groq-Organisation gesperrt (`model_permission_blocked_org`), wiederholt er dieselbe Aufnahme mit Turbo und nimmt 30 Min. direkt Turbo. `prompt`: persönliches Wörterbuch aus `woerterbuchPrompt(begriffe(text))` (Vorbelegung `STANDARD_WOERTERBUCH`); gibt Whisper den Prompt als Echo zurück, wird einmal ohne Prompt wiederholt |
| `Diktat(transcriber, nachbearbeitung)` | lange Aufnahmen über 20 MB an Sprechpausen in Teile schneiden (Groq-413), Teile zusammensetzen, letzter Textschritt der App |
| `SileroVad`, `SprachZeitachse` | Modell und Buchführung, testbar ohne Android (`SprachZeitachse` nimmt das Modell als Funktion) |
| `SpeechAnalyzer`, `SpeechAnalysis`, `WhisperHallucinationFilter` | Analyse und Filter, ohne Android |
| `Protokoll` | Anschluss für das Protokoll der App (`Protokoll.aktiv`) |
| `GroqNetz` | ein langlebiger OkHttp-Client für alle Aufnahmen |

## Der Schnitt

> **Das Modul besitzt Aufnahme, Spracherkennung, Aufnahmeende, Upload und alle Filter. Die App besitzt die
> Schalter (welche Filter an sind), den Schlüssel, den Vordergrunddienst, das Protokoll und was mit dem Text
> passiert.**

Vier Nabelschnüre wurden durchgeschnitten, alle als Parameter bzw. Anschluss:

| Vorher in Jarvis | Jetzt |
|---|---|
| `WeckwortErkenner.KLEIDIAI_ABSCHALTEN` in `SileroVad` | Konstruktorparameter `kleidiaiAbschalten` (bewusst ohne Vorgabe — `false` ließe ein Galaxy Fold 8 mit SIGILL abstürzen) |
| `Logbuch.sprache/netz` in `GroqTranscriber`, `GroqNetz` | `Protokoll.aktiv.sprache/netz` |
| `IdeenLog.info/warn` in `Diktat` | `Protokoll.aktiv.info/warn` |
| `UmlautKorrektur` in `Diktat` | Parameter `nachbearbeitung: (String) -> String` |

## Host muss liefern

| Was | Warum |
|---|---|
| `assets/vad/silero_vad.onnx` und `LIZENZ.txt` aus diesem Modul nach `app/src/main/assets/vad/` | `SileroVad` lädt genau diesen Pfad. Fehlt die Datei, läuft alles über den Pegel (Rückfall) |
| `com.microsoft.onnxruntime:onnxruntime-android` (Jarvis: 1.31.0) | rechnet Silero; nur eine ONNX Runtime je App (sherpa-onnx o. Ä. ohne eigene `libonnxruntime.so`) |
| `kleidiaiAbschalten` aus `/proc/cpuinfo` (SME ohne SME2) | siehe `BEISPIEL.md`; dieselbe Entscheidung für alle ONNX-Sitzungen der App |
| `com.squareup.okhttp3:okhttp` 4.x, `kotlinx-coroutines-android`, `androidx.core` | Upload, Coroutinen, `ContextCompat` |
| Berechtigung `RECORD_AUDIO`, Internet | Aufnahme und Groq |
| Vordergrunddienst mit `foregroundServiceType="microphone"`, falls im Hintergrund aufgenommen wird | Jarvis: `AufnahmeDienst` bleibt in der App |
| `abiFilters` (arm64-v8a, ggf. armeabi-v7a) | sonst nimmt ONNX Runtime alle Architekturen mit |
| Groq-Schlüssel, die vier Filterschalter, Whisper-Modell und Wörterbuch (Schalter + Begriffe) | Parameter von `GroqTranscriber`; Vorgabe: Filter an, Turbo, Wörterbuch an mit `STANDARD_WOERTERBUCH` |
| `Protokoll.aktiv` setzen (optional) | sonst keine Protokollzeilen |
| Die Schleife, die alle 50 ms `SprechEnde.neu(mikro.pegel.value, ms, mikro.sprachRahmen)` aufruft | das Modul gibt den Takt nicht vor |

## Gespeicherter Zustand

Keiner. Filterschalter, Schlüssel, Whisper-Modell und Wörterbuch liegen in den Einstellungen der App; Aufnahmen sind flüchtig. Einzig die 30-Minuten-Sperre für large-v3 nach einer Groq-Ablehnung lebt im Speicher (companion object).

- —

## Braucht Module

- —

## Mindestens

- Kotlin 2.1.0, JVM 17, minSdk der App (AudioRecord, kein API-Sonderfall)
- ONNX Runtime Android, geprüft mit 1.31.0 (der Schalter `mlas.disable_kleidiai` betrifft ORT ab 1.22)
- OkHttp 4.12, kotlinx.coroutines 1.9
- Compose wird nicht gebraucht

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| Jarvis | v4 | `Jarvis/app/src/main/java/de/frank/module/transkriptionsmodul/` (Tests: `Jarvis/app/src/test/java/de/frank/module/transkriptionsmodul/`) |

## Abnahme

Tests unter `test/` (in der App unter `app/src/test/java/de/frank/module/transkriptionsmodul/`, Daten unter
`app/src/test/resources/vad/`), JVM-Unit-Tests mit echten Silero-Werten:

```
./gradlew :app:testDebugUnitTest --tests "de.frank.module.transkriptionsmodul.*"
```

| Test | sichert |
|---|---|
| `SprechEndeVadTest` | Fahrgeräusch hält nichts offen, Ende 4,5 s nach dem letzten Wort, Nachdenkpause schneidet nicht ab, leise Sprache im Auto und im ruhigen Raum wird erkannt |
| `TranskriptionsSchutzTest` | Lüftergeräusch hält nichts offen, stille Halluzinationen im Lüfterrauschen fallen, kurze „Ja.“/„Stopp.“ bleiben vom Aufnahmeende bis zum Text, Rückfall auf den Pegel bei VAD-Ausfall |
| `HalluzinationsSchichtenTest` | Schichten 1–4 einzeln, vorher (Pegel) gegen nachher (Spracherkennung) |
| `SprechEndeTest` | Pegel-Rückfall ohne Spracherkennung |

Nicht automatisch geprüft: der Upload selbst (`TRANSCRIPTIONS_URL` ist fest) und `MicRecorder` (braucht
AudioRecord). Die Testdaten sind TTS-Sprache mit synthetischem Geräusch, keine Mitschnitte vom Handy.

## Änderungen

| Version | Datum | Was | Bricht Anbindung | Commit |
|---|---|---|---|---|
| v1 | 15.10.2026 | aus Jarvis herausgelöst. Code verschoben; neu sind nur der Parameter `kleidiaiAbschalten`, der Anschluss `Protokoll` und der Parameter `nachbearbeitung` von `Diktat`. Testfälle Lüfter und kurze Äußerungen ergänzt. Die nie benutzte Konstante `MicRecorder.CLONING_SAMPLE_RATE` blieb draußen (`start(scope, requestedSampleRate)` nimmt andere Raten weiter an). | — | — |
| v2 | 18.10.2026 | `MicRecorder`: Die Leseschleife wartet nicht mehr auf das Laden von Silero (beim Kaltstart lief sonst der Aufnahmepuffer über, die ersten Silben fehlten); ein spät geladenes Modell bekommt das schon Aufgenommene (bis 10 s) nachgereicht, damit die Zeitachse zur WAV passt. `release()` schließt die ONNX-Sitzung (nach dem Ende des Lese-Jobs), die nächste Aufnahme lädt sie neu. | nein | — |
| v3 | 18.10.2026 | `MicRecorder`: Den Rückstand eines spät geladenen Silero-Modells rechnet die Leseschleife nicht mehr am Stück (bis zu 10 s ließen den Aufnahmepuffer überlaufen), sondern je Durchlauf höchstens zwei Rahmen zusätzlich. Bis die Zeitachse die Gegenwart erreicht hat, bleibt `sprachRahmen` null und das Aufnahmeende läuft über den Pegel. | nein | — |
| v4 | 22.10.2026 | `GroqTranscriber`: Whisper-Modell wählbar (`MODELL_TURBO`/`MODELL_LARGE_V3`, 403-Sperre → einmal Turbo, 30 Min. Merker), persönliches Wörterbuch als Prompt (`prompt`, `begriffe`, `woerterbuchPrompt`, `STANDARD_WOERTERBUCH`, max. 100 Begriffe/350 Zeichen) mit Prompt-Echo-Schutz (einmal ohne Prompt wiederholen). Neue Parameter haben Standardwerte. | nein | — |
