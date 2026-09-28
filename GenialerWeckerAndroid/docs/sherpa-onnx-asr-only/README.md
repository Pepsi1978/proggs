# sherpa-onnx nur Spracherkennung (ohne TTS, ohne GPL)

Die Bibliotheken unter `app/src/main/jniLibs/arm64-v8a/` (Git-LFS) stammen aus diesem Bau:

- sherpa-onnx **v1.13.8**, onnxruntime **1.28.2**, nur `arm64-v8a`, `ANDROID_PLATFORM=android-26`
- NDK **28.2.13676358** (r28c), CMake **3.31.6** aus dem SDK, Ninja statt make, curl statt wget (Git-Bash hat beides nicht)
- CMake-Schalter: `SHERPA_ONNX_ENABLE_TTS=OFF`, `SHERPA_ONNX_ENABLE_SPEAKER_DIARIZATION=OFF`, `C_API=OFF`, `JNI=ON`
- Grund: Das fertige Android-Paket von sherpa-onnx bindet espeak-ng (GPL-3.0) statisch ein; für eine Closed-Source-Verkaufs-App verboten.
- Geprüft: kein espeak/piper in den `.so`, alle LOAD-Segmente Align 0x4000 (16-KB-Seiten).
- Kotlin-API (`com.k2fsa.sherpa.onnx`, Paketname nie ändern): OfflineRecognizer, OfflineStream, FeatureConfig, HomophoneReplacerConfig, QnnConfig.
- Achtung: `~/.gitignore_global` ignoriert `*.so` → neue Bibliotheken mit `git add -f` hinzufügen.

Neu bauen: `build-asr-only.sh` in Git-Bash ausführen (Pfad ohne Leerzeichen, `ANDROID_NDK` setzen).

## Seit 1.0.33: Spracherkennung + TTS nur für Supertonic und Pocket (weiterhin ohne GPL)

- `libsherpa-onnx-jni.so` stammt jetzt aus `build-asr-tts-ohne-espeak.sh`: zuerst `git apply tts-ohne-espeak.patch` im sherpa-onnx-v1.13.8-Baum (`/c/Users/barwa/build/sherpa-onnx/src`), dann das Skript.
- Der Patch führt die CMake-Option `SHERPA_ONNX_TTS_OHNE_ESPEAK` ein. Sie baut nur Supertonic und Pocket, ohne espeak-ng/piper-phonemize. VITS, Matcha, Kokoro, Kitten und ZipVoice werden per `#ifndef` ausgeklammert.
- Geprüft: 0 Treffer für espeak/piper (die 3 Treffer für „espeak“ sind „wespeaker“). Alle LOAD-Segmente haben Align 0x4000. `libonnxruntime.so` ist unverändert (1.28.2).
- Kotlin-API: zusätzlich `Tts.kt` (unverändert aus v1.13.8, alle Config-Klassen nötig, weil JNI sie per GetFieldID liest).
- Modelle lädt der Gradle-Task `ladeTtsModelle` nach `app/tts-modelle/` (nicht im Git).
- Lizenzen: Supertonic 3 steht unter OpenRAIL-M. Die README des Pocket-Exports sagt „non-commercial“, die LICENSE CC-BY-4.0 (sherpa-Issue #3971). **Vor dem Verkauf klären.**
