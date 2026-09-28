# TTS-Recherche — Unterthema 5: Integration + Performance

Stand der Recherche: 28.09.2026. Anker: sherpa-onnx v1.13.8, onnxruntime 1.28.2, NDK r28c.

---

## 1. sherpa-onnx: Stand von 2.0 / Issue #3731, unterstützte Modelltypen, espeak-Ausbau

**Status Issue #3731 ("Breaking Change: Remove espeak-ng and piper-phonemize dependency"):**
Das Issue ist offen, eröffnet am 08.07.2026, bislang ohne sichtbare Folgediskussion/Kommentare. Ziel: espeak-ng und piper-phonemize (GPL) entfernen, weil sie mit der Apache-2.0-Lizenz von sherpa-onnx kollidieren. Geplant als Breaking Change für eine neue Hauptversion **sherpa-onnx 2.0.0**.
Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3731

**Migrationspfade, die im Issue skizziert werden:**
- Eigene `lexicon.txt` (Wort → Phonem-Sequenz) statt espeak-Phonemisierung bereitstellen.
- Eigene Phonemisierungs-Pipeline extern betreiben und bereits erzeugte Tokens direkt übergeben — dafür ist ein neues Feld `std::vector<std::vector<std::string>> tokens` in der `GenerationConfig` vorgesehen.
Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3731

**Release-Stand (Stand heute):** Es gibt **keine 2.0.0**, aktuell ist weiterhin die 1.x-Reihe. Neueste gefundene Releases: v1.13.8 (10.09.2026), v1.13.7 (01.09.2026), v1.13.6 (18.08.2026), v1.13.5 (11.08.2026), v1.13.4 (07.07.2026).
Quelle: https://github.com/k2-fsa/sherpa-onnx/releases

Ergänzend existiert Issue #3969 ("Request: TTS-free artifact and clarification on native library licensing / distribution compliance"), das offenbar dieselbe GPL-Problematik aus Distributionssicht aufgreift — Inhalt nicht im Detail geprüft, nur als weiterer Beleg dafür, dass das Lizenzthema aktiv diskutiert wird.
Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3969

**Unterstützte TTS-Modelltypen (Stand heute, aus offizieller Doku und Code):**
- **VITS/Piper** — größte Modellsammlung, Einzel- und Multi-Speaker (bis 804 Sprecher). Nutzt espeak-ng bzw. piper-phonemize zur Phonemisierung.
- **Matcha** — z. B. matcha-icefall-en_US-ljspeech, matcha-icefall-zh-baker.
- **Kokoro** — Multi-Sprache (u. a. kokoro-multi-lang-v1_1 mit 103 Stimmen), nutzt ebenfalls espeak-ng-Daten für Phonemisierung.
- **KittenTTS** — Nano-/Mini-Varianten; Konfiguration verlangt weiterhin Model, `voices.bin`, `tokens.txt` **und** ein `espeak-ng-data`-Verzeichnis — d. h. auch KittenTTS in sherpa-onnx hängt aktuell noch an espeak-ng zur Text→IPA-Umwandlung, trotz "character-level tokeniser".
  Quelle: https://github.com/k2-fsa/sherpa-onnx/pull/2460, https://github.com/k2-fsa/sherpa-onnx/blob/master/c-api-examples/kitten-tts-en-c-api.c
- **Supertonic** — hat ein **eigenes, nach Rust portiertes Text-Frontend**, das nicht auf espeak-ng aufbaut (jüngste Fixes betrafen ein eigenes Frontend-Problem: verschluckte Diakritika bei Rumänisch/Tschechisch/Polnisch/Ungarisch/Türkisch/Vietnamesisch). Das deutet darauf hin, dass Supertonic in sherpa-onnx **kein espeak-ng** benötigt.
  Quelle: https://k2-fsa.github.io/sherpa/onnx/tts/supertonic.html, https://github.com/k2-fsa/sherpa-onnx/discussions/2833
- **ZipVoice** — Zero-Shot-Voice-Cloning-Modell, auch als Android-TTS-Engine-App-Ziel diskutiert (Issue #3439). Speicherbedarf: volles Modell ~605 MB (≥800 MB freier RAM empfohlen), int8-Distill-Variante `sherpa-onnx-zipvoice-distill-int8-zh-en-emilia` ~104 MB für Geräte mit <8 GB RAM.
  Quellen: https://github.com/k2-fsa/sherpa-onnx/issues/3439, Modellkarten auf HuggingFace (über Suche referenziert)
- **PocketTTS** — als eigener Modelltyp mit C-API-Beispiel vorhanden; es gibt einen offenen Qualitäts-Bug ("Pocket TTS Sounds Different On Original And Sherpa", Issue #3180) — als Bug-Kandidat unten aufgeführt.
  Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3180
- **MMS** — ebenfalls als Modellfamilie gelistet (Facebook Massively Multilingual Speech), nicht näher geprüft.

**Espeak per Build-Flag raus + eigene Phoneme/Tokens übergeben — geht das schon heute?**
Ja, teilweise, unabhängig vom noch offenen 2.0-Umbau: Issue #2260 ("Support for Offline Phonemization Input in Sherpa-ONNX VITS TTS") behandelt genau diesen Wunsch für VITS. Für reine ASR-Builds ist `SHERPA_ONNX_ENABLE_TTS=OFF` bereits Stand der Technik (bereits bekannt/vorausgesetzt). Für TTS mit eigenen Tokens ohne espeak zur Laufzeit ist der Weg laut #3731 die `lexicon.txt`- bzw. externe-Tokens-Route — diese landet aber erst mit dem 2.0-Umbau als offizielles, dokumentiertes Feature; aktuell (v1.13.8) ist es unklar, wie vollständig dieser Pfad schon für alle Modelltypen (insb. Kokoro/KittenTTS, die espeak-ng-data-Verzeichnisse verlangen) nutzbar ist.
Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/2260, https://github.com/k2-fsa/sherpa-onnx/issues/3731

Es existiert außerdem ein Community-Fork **ohne espeak-ng** für reine ASR-Nutzung als Vorlage, falls TTS deaktiviert bleiben soll (Sinsax/sherpa-onnx-prebuilt-no-tts) — nicht offiziell von k2-fsa, aber als Beleg dass "TTS disabled, no espeak-ng"-Builds machbar sind.
Quelle: https://github.com/Sinsax/sherpa-onnx-prebuilt-no-tts/releases/tag/v1.13.3

---

## 2. Alternative Laufzeiten für TTS auf Android

**ONNX Runtime Mobile (was sherpa-onnx nutzt):**
- Execution Provider für Android: CPU (Default), **XNNPACK** (optimierte Float-Kernel für ARM), **NNAPI** (Android Neural Networks API — einheitliche Schnittstelle zu CPU/GPU/NPU-Beschleunigern), **QNN** (Qualcomm AI Engine Direct SDK, für Snapdragon-SoCs mit Hexagon-DSP/NPU).
  Quellen: https://onnxruntime.ai/docs/execution-providers/Xnnpack-ExecutionProvider.html, https://onnxruntime.ai/docs/execution-providers/NNAPI-ExecutionProvider.html, https://onnxruntime.ai/docs/execution-providers/QNN-ExecutionProvider.html, https://onnxruntime.ai/docs/build/android.html
- Zu "NNAPI deprecated": In der offiziellen ONNX-Runtime-Doku selbst fand sich **kein** Hinweis auf eine Deprecation von NNAPI — das ist **unklar** und sollte gesondert geprüft werden (Google hat NNAPI als Plattform-API generell als "keine Weiterentwicklung mehr, aber weiter vorhanden" eingestuft, das lag aber außerhalb der geprüften Quellen dieser Runde).
- Soniqo (Doku-Anbieter, der sherpa-onnx-ähnliche Modelle bündelt) nennt konkrete Beschleuniger-Zuordnung: XNNPACK auf jedem arm64-v8a/x86_64-Gerät; NNAPI ab Snapdragon 8 Gen 1+, Exynos 2200+, Google Tensor G2+; QNN/Hexagon-DSP nur auf bestimmten Automotive-SoCs (SA8295P, SA8255P).
  Quelle: https://soniqo.audio/benchmarks/android

**Alternative Inferenz-Engines (nicht ONNX-basiert):**
- **ExecuTorch** — PyTorchs natives On-Device-Runtime für iOS/Android/Embedded, nutzt u. a. XNNPACK als Backend.
- **MNN** (Alibaba) — leichtgewichtige, für mobile ARM/x86-64 optimierte Inferenz-Engine, laut Quelle mit führender Inferenzgeschwindigkeit auf Mobile-CPU/GPU dank optimiertem KV-Cache und Speicherlayout (Kontext dort primär LLM-fokussiert, nicht TTS-spezifisch).
- **ncnn** (Tencent) — leichtgewichtiges, auf mobile ARM getrimmtes Inferenz-Framework.
- Ein direkter, spezifisch TTS-fokussierter Vergleich ExecuTorch vs. MNN vs. ncnn wurde in der Recherche **nicht gefunden** — die Aussagen stammen aus allgemeinen On-Device-AI-Übersichten, nicht aus TTS-Benchmarks. Als unklar markiert.
  Quelle: https://www.alephzerolabs.com/blog/on-device-ai-2026-sub-20ms, https://github.com/jeho-lee/Awesome-On-Device-AI-Systems/blob/main/README.md

**LiteRT/TFLite:** Wird von Google selbst für "Play for On-device AI" als primär unterstütztes Format genannt (siehe Punkt 4) — spricht dafür, dass LiteRT der von Google bevorzugte Mobile-Inferenzpfad ist, ONNX wird dort nicht explizit erwähnt.
Quelle: https://developer.android.com/google/play/on-device-ai

**llama.cpp/GGUF für LLM-basierte TTS (Orpheus/OuteTTS/NeuTTS):**
- `llama.rn` (React-Native-Bindung von llama.cpp) unterstützt On-Device-TTS über ein eigenes `codec.cpp` als Audio-Codec/Vocoder-Backend: TTS-Backbone (GGUF) + zugehöriger Codec/Vocoder (GGUF) werden geladen. Unterstützte Modellfamilien laut Quelle: OuteTTS (mehrere Versionen), Soprano, NeuTTS (Nano/Air), CSM, Qwen3-TTS, MOSS, Chatterbox, BlueMagpie u. a.
- Konkrete Latenzzahlen für Orpheus/OuteTTS/NeuTTS auf Android wurden **nicht gefunden** — unklar. Als Referenzgröße für LLM-Inferenz allgemein auf Android: Pixel 8 Pro (Tensor G3), 3B-Modell, GGUF Q4_K_M, Cold-Start 4,2 s, 11,2 Token/s; auf aktuellen Flaggschiff-Phones allgemein 10–20 Token/s bei 1–3B-Parametern — das ist LLM-Text-Durchsatz, nicht TTS-Audiolatenz, und daher nur bedingt übertragbar.
  Quellen: https://www.npmjs.com/package/llama.rn, https://github.com/ggml-org/llama.cpp/discussions/14356

**Fazit zu Beschleunigern bei TTS:** Belegt nützlich sind XNNPACK (CPU-Float-Kernel-Optimierung, auf jedem Gerät verfügbar) und NNAPI (2–3x schneller laut Soniqo-Doku auf echter Snapdragon-Hardware ggü. Emulator-Baseline, allerdings ohne Gegenkontrolle einer unabhängigen Quelle — als vorläufig markiert). QNN nur auf ausgewählten Snapdragon-SoCs relevant. Ob NNAPI in ONNX Runtime aktuell als "deprecated" gilt, bleibt unklar und wurde nicht in offizieller Doku bestätigt.

---

## 3. Gemessene RTF/Latenz-Zahlen auf konkreten Android-Geräten

Belastbare, gerätespezifische Messwerte waren in dieser Recherche-Runde nur bei einer Quelle (Soniqo) auffindbar, und dort ausdrücklich **nur aus einem Android-Emulator ohne Hardwarebeschleunigung** — reale Snapdragon/Exynos/Tensor/Dimensity-Zahlen für Piper/VITS/Matcha/Kokoro/Kitten/Supertonic wurden trotz gezielter Suche **nicht** gefunden. Das ist eine wesentliche Lücke.

| Gerät | Modell | RTF / Latenz | Quelle |
|---|---|---|---|
| Android-Emulator (arm64-v8a, keine HW-Beschleunigung) | Kokoro 82M (TTS) | RTF 0,58; Latenz 1,075 s | https://soniqo.audio/benchmarks/android |
| Android-Emulator (arm64-v8a, keine HW-Beschleunigung) | Parakeet TDT v3 (ASR, zum Vergleich) | RTF 0,12; Latenz 175 ms | https://soniqo.audio/benchmarks/android |
| Android-Emulator (arm64-v8a, keine HW-Beschleunigung) | Silero VAD v5 | RTF <0,01; Latenz <1 ms | https://soniqo.audio/benchmarks/android |
| Reale Snapdragon-Geräte (unspezifiziert, NNAPI) | allgemein, nicht modellspezifisch | "2–3x schneller" als Emulator-Baseline (keine absoluten Werte) | https://soniqo.audio/benchmarks/android |
| Nicht-mobil, CPU allgemein (Referenz, nicht Android) | Kokoro-7M ONNX | RTF 17,6x–18,4x schneller als Echtzeit (~330 ms für 6,1 s Sprache) | Suchergebnis, Quelle zu Kokoro-7M auf HuggingFace/Blog (nicht Android-spezifisch) |
| Nicht-mobil, CPU allgemein (Referenz, nicht Android) | Kokoro-82M ONNX | RTF 1,4x–4,5x (also langsamer als Echtzeit je nach Setup) | Suchergebnis, nicht Android-spezifisch |

Zu Quantisierung: Für Kokoro wird berichtet, dass INT8-Quantisierung primär die Modellgröße reduziert (3,5x kleiner), auf CPU aber laut einer Quelle **2,4x langsamer** sein kann (Dequantisierungs-Overhead), und auf Apple-Silicon sogar ~2x langsamer als FP32/FP16. Das ist ein Gegenbefund zur verbreiteten Annahme "int8 = immer schneller" und sollte für Android-ARM separat verifiziert werden, da die Quelle sich nicht eindeutig auf Android-ARM bezieht.
Quelle: Suchergebnis zu Kokoro-ONNX-Quantisierung (NemesisNet-Blog, HuggingFace-Modellkarten NeuML/kokoro-int8-onnx)

**Einschätzung:** Belastbare, gerätespezifische RTF-Werte für Snapdragon 7-/8-Serie, Exynos, Tensor, Dimensity fehlen in den geprüften Quellen komplett. Für eine Kaufentscheidung/Architekturentscheidung sollte das per eigenem Benchmark auf Zielgeräten nachgezogen werden — als "unklar, weiterer Research-Bedarf" markiert.

---

## 4. Architektur: Streaming, TextToSpeechService, Modellauslieferung, 16-KB-Page-Size, Speicher/Akku

**Satzweises Streaming / Time-to-First-Audio:**
- sherpa-onnx bietet eine `generateWithCallback`-API, die PCM-Chunks inkrementell während der Synthese liefert — reduziert Time-to-First-Audio von "gesamte Synthesedauer" auf "Dauer bis zum ersten Chunk" (typischerweise ein Bruchteil der Gesamtdauer).
- Empfohlenes Muster: Text satzweise puffern (Trennung an Satzzeichen), pro Satz synthetisieren und parallel bereits über `AudioTrack` im `MODE_STREAM` abspielen, statt komplett zu synthetisieren und dann abzuspielen.
  Quellen: https://picovoice.ai/blog/android-streaming-text-to-speech/, allgemeine Suchergebnisse zu sherpa-onnx Callback-API
- Konkrete Zahl "< 300 ms bis zur ersten Audioausgabe" für sherpa-onnx auf Android wurde in dieser Runde **nicht** mit einer Quelle belegt — unklar, ob das erreichbar ist ohne eigene Messung.

**Eigene TextToSpeechService-Implementierung (App als System-TTS-Engine):**
- Referenzimplementierung existiert offiziell im sherpa-onnx-Repo: `android/SherpaOnnxTtsEngine/app/src/main/java/com/k2fsa/sherpa/onnx/tts/engine/TtsEngine.kt` — zeigt, wie man Androids `TextToSpeechService`-API mit sherpa-onnx als Backend implementiert.
  Quelle: https://github.com/k2-fsa/sherpa-onnx/blob/master/android/SherpaOnnxTtsEngine/app/src/main/java/com/k2fsa/sherpa/onnx/tts/engine/TtsEngine.kt
- Weitere Community-Implementierungen als Vorlage: VoxSherpa-TTS (Kokoro + Piper/VITS via JNI, `KokoroEngine.java` / `VoiceEngine.java`), jing332/SherpaOnnxTtsEngineAndroid.
  Quellen: https://github.com/CodeBySonu95/VoxSherpa-TTS, https://github.com/jing332/SherpaOnnxTtsEngineAndroid
- Es gibt eine offizielle Diskussion "Android engine/engines for TTS and STT/ASR" im sherpa-onnx-Repo als zentrale Anlaufstelle für dieses Thema.
  Quelle: https://github.com/k2-fsa/sherpa-onnx/discussions/1392
- ZipVoice wird explizit als Ziel für eine Android-TTS-Engine-App diskutiert (Issue #3439, offen) — Zero-Shot-Voice-Cloning als System-TTS ist also ein aktives, aber noch nicht abgeschlossenes Thema.
  Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3439

**Modellauslieferung: APK-Asset vs. Play Asset Delivery vs. Play for On-device AI vs. Download:**
- **Play Asset Delivery (PAD)**: für App-Bundle-Assets generell, Summe aller Install-Time-Asset-Packs max. **1 GB**. Reine Legacy-APKs (kein App Bundle) sind zusätzlich auf **100 MB** APK-Größe begrenzt.
  Quelle: https://support.google.com/googleplay/android-developer/answer/9859372, https://developer.android.com/guide/playcore/asset-delivery
- **Play for On-device AI (Beta)**: neueres, speziell für ML-Modell-Auslieferung gedachtes Google-Play-Feature (kostenfrei). Ein Artefakt für Code+Assets+ML-Modelle, drei Auslieferungsmodi: Install-time (sofort verfügbar, klassisches AAB), Fast-follow (Download startet automatisch nach Installation im Hintergrund), On-demand (Download erst bei Bedarf zur Laufzeit, über `AiPackManager`). Größenlimits: **einzelnes AI-Pack max. 1,5 GB komprimiert**, **kumulative Bundle-Größe max. 4 GB**. Benötigt AGP ≥ 8.8 (Device-Targeting ≥ 8.10.0). Doku nennt explizit nur **LiteRT (TFLite) und MediaPipe** als unterstützte Modellformate — **ONNX wird nicht erwähnt**; für ONNX-Modelle (also sherpa-onnx) wäre es vermutlich nur als generischer Binär-Asset-Transport nutzbar, mit eigener Runtime-Einbindung. Status ausdrücklich **Beta**, nicht GA.
  Quelle: https://developer.android.com/google/play/on-device-ai
- **Eigener Download (eigener Server/CDN)**: bleibt die einzige Variante ohne Größenlimit und mit voller Formatfreiheit (ONNX-Modelle, espeak-ng-data etc.) — in der Praxis das, was sherpa-onnx-Referenzapps typischerweise nutzen (Modelle werden zur Laufzeit heruntergeladen, nicht als APK-Asset gebündelt), da TTS-Modelle (Kokoro ~80 MB+, ZipVoice bis 605 MB) sonst schnell an die PAD-/APK-Grenzen stoßen.

**16-KB-Page-Size-Pflicht (Android 15+, seit 01.11.2025 für neue Apps/Updates bei Google Play):**
- Alle nativen `.so`-Bibliotheken müssen 16-KB-seitenausgerichtet sein, sonst Ablehnung beim Play-Upload. NDK r28+ kompiliert standardmäßig 16-KB-aligned.
  Quellen: https://developer.android.com/guide/practices/page-sizes, https://source.android.com/docs/core/architecture/16kb-page-size/16kb
- **onnxruntime selbst hatte dieses Problem mehrfach offen** (Issues #24902, #25859, #26228 im microsoft/onnxruntime-Repo) — insbesondere `libonnxruntime4j_jni.so` (die Java-JNI-Bindungsschicht) war betroffen. PR #24947 wurde als Fix referenziert, war aber laut einer Quelle in Version 1.22.2 noch nicht vollständig wirksam bzw. der Status blieb in den geprüften Auszügen unklar (Issue #26228 zeigt "Closed", ohne dass der Fix-Release explizit benannt werden konnte).
- **sherpa-onnx-eigenes Tracking-Issue #3291** ("Update bundled ONNX Runtime from 1.17.1 to latest (16KB page alignment / Android 15+)") wurde am 11.03.2026 eröffnet und ist **geschlossen**. Bestätigt über eine unabhängige Suche: **sherpa-onnx v1.13.8 bündelt onnxruntime 1.28.2** (Android-JNI-Lib `libsherpa-onnx-jni.so` gegen ONNX Runtime 1.28.0 neu gebaut) — das deckt sich mit dem in der Aufgabe genannten Versionsanker. Ob damit die 16-KB-Page-Size-Anforderung für sherpa-onnx-Android-Builds vollständig erfüllt ist, konnte nicht mit einer expliziten Bestätigungsaussage (z. B. Release-Notes-Zitat "16KB-safe") belegt werden — **unklar, sollte vor Store-Upload mit einem echten `zipalign`/`check_elf_alignment`-Check gegen die konkrete AAR verifiziert werden.**
  Quellen: https://github.com/k2-fsa/sherpa-onnx/issues/3291, https://github.com/microsoft/onnxruntime/issues/26228, https://github.com/microsoft/onnxruntime/issues/25859, https://github.com/microsoft/onnxruntime/issues/24902

**Speicherbedarf:**
- Piper-Stimme (~30 MB) + sherpa-onnx-Native-Runtime (~25 MB) ≈ 55 MB einmaliger Install-Footprint.
- Kokoro-82M: Modell selbst deutlich größer als Piper (82M Parameter), nach Quantisierung laut einer Quelle unter 80 MB.
- ZipVoice: volles Modell ~605 MB (≥800 MB freier RAM empfohlen), int8-Distill-Variante ~104 MB für Geräte mit <8 GB RAM.
  Quellen: https://medium.com/@patare.vivek/running-neural-text-to-speech-on-device-with-piper-and-sherpa-onnx-58f4eed29247, Zipvoice-Modellkarten (über Suche referenziert)

**Akkuverbrauch:** Trotz gezielter Suche **keine** belastbare Quelle mit konkreten Akku-/Energieverbrauchszahlen für sherpa-onnx-TTS auf Android gefunden — als unklar markiert. Allgemein gilt laut mehreren On-Device-AI-Übersichten, dass NPU/DSP-Beschleunigung (NNAPI/QNN) gegenüber reiner CPU-Inferenz typischerweise auch Energie spart, aber ohne quellenbasierte Zahlen für TTS speziell.

---

## BEST-PRACTICES-KANDIDATEN:

- Beim Ausliefern von sherpa-onnx-TTS-Modellen (Kokoro, ZipVoice u. ä.) NICHT auf Play Asset Delivery oder Play for On-device AI setzen, solange dessen Doku nur LiteRT/MediaPipe als Format nennt — stattdessen eigenen Download-Mechanismus (Server/CDN) für ONNX-Modelle + espeak-ng-data nutzen, um nicht an die PAD-1-GB- bzw. AI-Pack-1,5-GB-Grenze zu stoßen. Quelle: https://developer.android.com/google/play/on-device-ai, https://developer.android.com/guide/playcore/asset-delivery
- Vor jedem Play-Store-Upload einer App mit sherpa-onnx/onnxruntime-Native-Libs die 16-KB-Page-Size-Ausrichtung der konkret verwendeten AAR-Version explizit prüfen (z. B. mit Androids `check_elf_alignment.sh` bzw. `zipalign -P 16`), statt sich allein auf die Versionsnummer zu verlassen — die Historie zeigt mehrfach "closed, aber real noch kaputt"-Fälle bei onnxruntime. Quellen: https://github.com/microsoft/onnxruntime/issues/26228, https://github.com/k2-fsa/sherpa-onnx/issues/3291
- Für Time-to-First-Audio satzweises Chunking + `generateWithCallback` + `AudioTrack.MODE_STREAM` kombinieren, statt komplette Synthese abzuwarten. Quelle: https://picovoice.ai/blog/android-streaming-text-to-speech/

## BUG-KANDIDATEN:

- Symptom: PocketTTS-Ausgabe in sherpa-onnx klingt anders als beim Original-Modell. Ursache: unklar (Issue ohne geprüfte Detailanalyse). Version: sherpa-onnx (Stand 2026, genaue Version im Issue nicht geprüft). Fix: nicht bestätigt, Issue offenbar noch offen. URL: https://github.com/k2-fsa/sherpa-onnx/issues/3180
- Symptom: Supertonic-TTS-Frontend in sherpa-onnx verschluckte Diakritika bei Rumänisch, Tschechisch, Polnisch, Ungarisch, Türkisch, Vietnamesisch (und weiteren nicht-englischen Lateinschrift-Sprachen). Ursache: Fehler im nach Rust portierten Supertonic-Text-Frontend. Version: sherpa-onnx, genaue Versionsnummer des Fixes nicht ermittelt. Fix: laut Suchergebnis bereits behoben ("Recent fixes addressed... dropped diacritics"), aber ohne konkrete Commit-/Release-Referenz verifiziert. URL: https://k2-fsa.github.io/sherpa/onnx/tts/supertonic.html
- Symptom: `libonnxruntime4j_jni.so` (Android-JNI-Bindung von ONNX Runtime) nicht 16-KB-page-size-kompatibel, führt zu Ablehnung/Warnung bei Google-Play-Upload für Android 15+. Ursache: native Bibliothek nicht mit 16-KB-Alignment gebaut. Version: betraf u. a. onnxruntime 1.20.0–1.21.0 laut Issues; PR #24947 als Fix referenziert, Wirksamkeit in 1.22.2 laut einer Quelle noch unklar. Fix: nicht abschließend verifizierbar aus den geprüften Auszügen — für das im Projekt-Anker genannte onnxruntime 1.28.2 (via sherpa-onnx v1.13.8) nicht explizit bestätigt, sollte selbst geprüft werden. URL: https://github.com/microsoft/onnxruntime/issues/26228, https://github.com/microsoft/onnxruntime/issues/25859, https://github.com/microsoft/onnxruntime/issues/24902
