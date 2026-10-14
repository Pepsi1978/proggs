# sherpa-onnx für die eigene Weckwort-Phrase

`sherpa-onnx-1.13.8-ohne-onnxruntime.aar` ist die offizielle `sherpa-onnx-1.13.8.aar`
(github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8), angepasst mit `sherpa_anpassen.py`:

1. `jni/*/libonnxruntime.so` entfernt: Jarvis lädt genau EINE ONNX Runtime, die neueste von Microsoft
   (`onnxruntime-android`, Version in `gradle/libs.versions.toml`, Stand 1.31.0).
2. In `libsherpa-onnx-jni.so` (und `-c-api.so`) die Symbolversion von `OrtGetApiBase` auf die Microsoft-ORT
   umgestellt (`VERS_1.28.2` → `VERS_1.31.0`). ORT versieht diesen einzigen Einstiegspunkt mit einer
   Versionsmarke; ohne die Anpassung lehnt der Linker jede andere ORT ab („cannot locate symbol OrtGetApiBase“).
   Alles Weitere holt sherpa über `OrtGetApiBase()->GetApi(<API-Nummer>)`, das jede neuere ORT bedient.

Warum eine gemeinsame ORT: Auf Prozessoren mit SME, aber ohne SME2 (Galaxy Fold 8, SM8850) stürzen die
KleidiAI-Rechenroutinen von ORT ab 1.22 mit SIGILL ab (`ptrue pn9.b`, ein SME2-Befehl). `WeckwortErkenner`
setzt dort `mlas.disable_kleidiai`; das gilt für die Bibliothek und damit auch für sherpa-onnx.
Geräte mit echtem SME2 behalten KleidiAI.

ORT 1.31 bringt eine Telemetrie an Microsoft mit (ContentProvider `ai.onnxruntime.TelemetryInitializer`):
im Manifest per `tools:node="remove"` entfernt, zusätzlich `OrtEnvironment.setTelemetry(false)`.

## Aktualisieren

```
python -I sherpa_anpassen.py <sherpa-onnx-X.aar> <onnxruntime-android-Y.aar> sherpa-onnx-X-ohne-onnxruntime.aar
```

Dateiname in `app/build.gradle.kts` anpassen, ORT-Version in `libs.versions.toml` auf Y. Die Microsoft-ORT muss
mindestens so neu sein wie die, gegen die sherpa gebaut ist. Das Skript bricht ab, wenn die Versionsmarken
verschieden lang sind (dann erweitern).
