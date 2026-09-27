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
