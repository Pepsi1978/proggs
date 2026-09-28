# Bekannte Bugs und Fallen: lokales neurales TTS auf Android

Stand der Recherche: 28.09.2026. Format je Eintrag: Titel | Symptom | Ursache | betroffene Versionen/Geräte | funktionserhaltender Fix/Workaround | Quelle-URL.

## Crashes / Abstürze

1. **sherpa-onnx: onnxruntime-Versions-Mismatch → dlopen-Crash**
   Symptom: `UnsatisfiedLinkError` bzw. `dlopen failed: symbol 'OrtGetApiBase' not found`.
   Ursache: sherpa-onnx v1.13.4 bindet native Binaries gegen onnxruntime 1.27.0; pinnt das Projekt in `libs.versions.toml` eine andere Version (z. B. 1.29.0), scheitert das Laden der JNI-Lib.
   Betroffen: sherpa-onnx v1.13.4 + onnxruntime 1.29.0 (Android).
   Fix/Workaround: onnxRuntime-Version exakt auf die von sherpa-onnx erwartete Version fixieren (hier 1.27.0), nicht eigenständig hochziehen.
   Quelle: https://github.com/Codename-11/hermes-relay/issues/444

2. **sherpa-onnx: fehlende native Lib „libsherpa-onnx-jni.so nicht gefunden“**
   Symptom: `UnsatisfiedLinkError: dlopen failed: library "libsherpa-onnx-jni.so" not found`.
   Ursache: Native Lib landet nicht korrekt im `jniLibs`-Ordner der APK (z. B. bei manuellem AAR-Handling oder ABI-Filterung, die die falsche ABI ausschließt).
   Betroffen: k2-fsa/sherpa-onnx, diverse Versionen.
   Fix/Workaround: `jniLibs`-Struktur pro ABI prüfen (arm64-v8a mindestens), AAR nicht manuell entpacken/neu packen, `abiFilters` mit dem AAR abgleichen.
   Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/1279

3. **sherpa-onnx: C++-Exception (Ort::Exception) durchbricht JNI-Grenze, App stirbt statt Java-Exception**
   Symptom: `terminating due to uncaught exception of type Ort::Exception`, Prozess stirbt hart, kein catchbarer Java-Fehler.
   Ursache: Bei kaputter/korrupter Modelldatei wirft `OfflineRecognizer.newFromFile()` intern eine C++-Exception, die im Konstruktions-Pfad NICHT am JNI-Rand abgefangen und in eine Java-Exception umgewandelt wird (anders als bei der Config-Validierung, die schon abgesichert ist).
   Betroffen: sherpa-onnx 1.13.8, Android AAR arm64, Android 16; Issue vom 25.09.2026, offen.
   Fix/Workaround: Modelldateien vor dem Laden selbst auf Integrität prüfen (Checksumme nach Download), Download-Fehler defensiv behandeln; bis zum Upstream-Fix keine reine try/catch-Sicherheit um den Ladeaufruf verlassen — der Absturz passiert nativ, ein Java-try/catch fängt ihn nicht ab.
   Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3987

4. **sherpa-onnx: Lexikon-Crash bei Text-zu-Token („key not found“) mit VITS-VCTK**
   Symptom: `terminating due to uncaught exception of type std::out_of_range: unordered_map::at: key not found`, SIGABRT während der Synthese.
   Ursache: Mismatch zwischen Modellkonfiguration und Lexikon/Tokenizer-Ressourcen bei bestimmten Texten/Sonderzeichen (Lexicon::ConvertTextToTokenIdsNotChinese greift auf nicht vorhandenen Key zu).
   Betroffen: sherpa-onnx 1.9.4 / 1.9.19 / 1.9.22, Modell vits-vctk, u. a. Samsung Galaxy Tab 7a; Issue seit 29.04.2024 offen, kein dokumentierter Fix.
   Fix/Workaround: Eingabetext vor Synthese normalisieren/auf zulässige Zeichen filtern (Whitelist statt Blacklist), unbekannte Sonderzeichen vor der TTS-Engine entfernen oder ersetzen.
   Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/823

5. **16-KB-Page-Size: .so-Dateien crashen auf neuen Geräten/Android 15+**
   Symptom: Absturz beim Laden nativer Libraries (Segfault) auf Geräten mit 16-KB-Speicherseiten, RELRO-Segment nicht 16-KB-aligned.
   Ursache: .so wurde mit NDK r27 oder älter ohne 16-KB-Alignment-Flags gebaut (betrifft u. a. `libonnxruntime4j_jni.so`, ONNX-Runtime-Java-Binding-Layer).
   Betroffen: onnxruntime (JNI-Layer) vor NDK-r28-Rebuild, 64-Bit-Geräte mit 16-KB-Pages; Pflicht für neue Apps/Updates mit Ziel Android 15+ ab 1.11.2025 laut Google.
   Fix/Workaround: Mit NDK r28+ neu bauen (baut standardmäßig 16-KB-aligned) oder Linker-Flag `-Wl,-z,max-page-size=16384` setzen; sherpa-onnx-eigene Prebuilt-.so-Version auf 16-KB-Kompatibilität prüfen (offenes Issue bei den sherpa-onnx-Abhängigkeiten).
   Quellen: https://developer.android.com/guide/practices/page-sizes , https://github.com/microsoft/onnxruntime/issues/25859 , https://github.com/csukuangfj/onnxruntime-libs/issues/44

6. **ONNX Runtime: XNNPACK-Execution-Provider bricht Prozess hart ab (SIGABRT) statt Fehler zurückzugeben**
   Symptom: `Abort message: 'terminating'`, kompletter Prozessabbruch beim Anlegen der InferenceSession mit XNNPACK-EP unter bestimmten Op-Konstellationen (z. B. Resize-Node mit leeren „scales“).
   Ursache: XNNPACK-EP wirft bei bestimmten ungültigen/leeren Tensor-Inputs eine native Exception statt eines regulären ORT-Fehlercodes; noch in 1.26.0 vorhanden.
   Betroffen: onnxruntime Android arm64, XNNPACK-EP, Modelle mit Resize-Operatoren.
   Fix/Workaround: XNNPACK-EP nur nach Modell-Kompatibilitätstest je Zielgerät aktivieren, Fallback auf CPU-EP vorsehen, Modellexport ohne problematische Resize/leere-Scale-Konstrukte.
   Quelle: https://github.com/microsoft/onnxruntime/issues/32298

7. **ONNX Runtime: NNAPI auf älteren API-Levels fälschlich als verfügbar gelistet**
   Symptom: NNAPI wird von `OrtEnvironment.getProviders()` als Provider gemeldet, obwohl NNAPI unterhalb API-Level 27 nicht existiert/funktioniert — führt zu Laufzeitfehlern bei EP-Auswahl.
   Ursache: Provider-Liste berücksichtigt keine Mindest-API-Prüfung.
   Betroffen: onnxruntime Android, API < 27.
   Fix/Workaround: Vor NNAPI-Nutzung selbst `Build.VERSION.SDK_INT >= 27` prüfen, nicht auf die reine Provider-Liste verlassen.
   Quelle: https://github.com/microsoft/onnxruntime/issues/19496

## Audioprobleme

8. **Falsche/inkonsistente Samplerate an AudioTrack → Knacken oder falsche Tonhöhe**
   Symptom: Klick-/Knackgeräusche bzw. verzerrte Wiedergabe, wenn die AudioTrack-Samplerate nicht exakt zur TTS-Modellausgabe passt (z. B. 22050 Hz Modell gegen 24000/44100 Hz AudioTrack).
   Ursache: AudioTrack-Init und Modell-Samplerate laufen auseinander; dynamischer Sampleratenwechsel während laufender Wiedergabe erzwingt Filter-Neuberechnung, was hörbare Klicks erzeugt.
   Betroffen: generisch alle AudioTrack-basierten TTS-Integrationen (Piper/VITS-Modelle liefern typischerweise 16 kHz/22050 Hz/24000 Hz, je nach Trainingskorpus).
   Fix/Workaround: AudioTrack immer exakt mit der vom Modell gemeldeten Samplerate initialisieren (nicht hardcoden), Sampleratenwechsel zwischen Sätzen vermeiden bzw. AudioTrack bei Wechsel neu anlegen statt live umzukonfigurieren.
   Quelle: https://developer.android.com/ndk/guides/audio/sampling-audio (allgemeine Grundlage, Praxisproblem in mehreren TTS-Projekten beobachtet, u. a. VoxSherpa/sherpa-onnx-Integrationen: https://github.com/CodeBySonu95/VoxSherpa-TTS)

9. **Piper/Wyoming: TTS schneidet die letzten Wörter eines Satzes ab**
   Symptom: „Piper consistently cuts off the last few words of TTS“, reproduzierbar bei 2–3-Satz-Ansagen auf bestimmten Wiedergabegeräten.
   Ursache: In dem konkreten Issue nicht abschließend geklärt (kein Root-Cause im Thread dokumentiert); Community vermutet Zusammenspiel aus Buffer-Ende und Wiedergabe-Geräte-Timing. Als „unklar“ zu kennzeichnen.
   Betroffen: Home Assistant Core 2025.8.0, Wyoming-Piper-Add-on, primär bei Google-Speaker-Wiedergabe; auf anderer Hardware (Cloud-TTS) trat der Fehler laut Melder nicht auf.
   Fix/Workaround: Issue wurde „not planned“ geschlossen, kein offizieller Fix. Präventiv: Ausgabepuffer am Satzende nicht vorzeitig schließen/freigeben, kurzen Stille-Puffer (Silence-Padding) ans Ende jedes Chunks anhängen, bevor der AudioTrack/Player gestoppt wird.
   Quelle: https://github.com/home-assistant/core/issues/150397

10. **Streaming-TTS: letzte Äußerung wird abgeschnitten, weil Output-Transport vor Ende der Warteschlange abgebaut wird**
    Symptom: Der letzte gequeuete TTS-Turn wird nicht vollständig abgespielt, weil der Ausgabe-Transport/Player beendet wird, bevor die Audio-Warteschlange geleert ist.
    Ursache: Kein Warten auf „Queue leer“ vor Teardown von Player/Service.
    Betroffen: pipecat-ai/pipecat, allgemein übertragbar auf jede Streaming-TTS-Pipeline mit eigenem Player-Lifecycle.
    Fix/Workaround: Vor Stoppen/Freigeben des Players explizit auf „alle gequeuten Audio-Chunks abgespielt“ warten (z. B. Callback/Flag), erst danach Ressourcen freigeben.
    Quelle: https://github.com/pipecat-ai/pipecat/issues/4647

11. **Android TextToSpeech: `onError(String, int)` (API 21) wird nie aufgerufen, nur die deprecated 1-Parameter-Variante**
    Symptom: Fehler in der Synthese werden über `UtteranceProgressListener.onError(String utteranceId)` (deprecated) gemeldet, die neuere `onError(String, int errorCode)` läuft nie.
    Ursache: Framework-Bug im System-TTS-Stack, der die neuere Callback-Variante nicht auslöst.
    Betroffen: Android-Versionen vor API 31; laut Meldern über 1,5 Jahre nach „fixed“-Markierung im Tracker weiterhin reproduzierbar.
    Fix/Workaround: Immer beide Overloads (deprecated und neue) implementieren und auf die deprecated Variante als primäre Fehlerquelle für API < 31 verlassen; nicht ausschließlich auf `onError(String, int)` bauen.
    Quelle: https://issuetracker.google.com/issues/138321382 (Zugriff nur mit Login möglich, Inhalt über Sekundärsuche bestätigt)

## Aussprache-Fallen Deutsch / Texteingabe

12. **espeak-ng-basierte G2P versagt bei „schmutzigem“ Text (Zahlen, Abkürzungen, Sonderzeichen)**
    Symptom: Falsche/unverständliche Aussprache, sobald der Eingabetext nicht bereits normalisiert ist (Zahlen, Daten, Abkürzungen wie „z. B.“, „usw.“, „Nr.“); einmal falsche Phoneme erholen sich nicht mehr innerhalb des Satzes.
    Ursache: espeak-ng (in sherpa-onnx als G2P-Fallback für Piper/VITS-Stimmen genutzt) erwartet weitgehend vorverarbeiteten Text; es gibt keine eingebaute robuste Textnormalisierung für Zahlen/Daten/Abkürzungen im deutschen Sprachmodus.
    Betroffen: alle espeak-ng-basierten Piper/VITS-Pipelines, sprachunabhängig, besonders spürbar bei Deutsch (Komposita, Ordinalzahlen, Euro-Beträge).
    Fix/Workaround: Eigene Textnormalisierungs-Vorstufe einbauen (Zahlen/Daten/Uhrzeiten/Abkürzungen vor der Synthese ausschreiben); als Referenzimplementierung eignet sich das `german_transliterate`-Modul, das genau für diesen espeak-ng-Vorverarbeitungsschritt gebaut ist.
    Quellen: https://medium.com/@thorsten_Mueller/increase-text-to-speech-pronunciation-quality-adjusting-espeak-dictionary-8232a0036d69 , https://github.com/repodiac/german_transliterate

13. **Fehlende deutsche Textnormalisierung bei Kokoro-Derivaten (Daten, Uhrzeiten, Dezimalzahlen, Einheiten, Ordinalzahlen, Euro-Beträge)**
    Symptom: Ohne Normalisierungsschicht liest ein deutsches Kokoro-Modell Zahlen/Daten/Einheiten falsch oder buchstabiert sie.
    Ursache: Kokoro selbst bringt keine deutsche Normalisierung mit (Community-Modell „Kokoro-82M-ONNX-German“ ergänzt dafür explizit eine eigene v1.1-Normalisierungsschicht).
    Betroffen: Community-Ports von Kokoro auf Deutsch (nicht offizielles Kokoro, das kein Deutsch spricht — bereits bekannt).
    Fix/Workaround: Normalisierungsschicht vor die Synthese schalten (Daten/Uhrzeiten/Dezimalzahlen/Einheiten/Ordinalzahlen/Euro-Beträge ausschreiben), wie im German-Kokoro-Modellcard beschrieben.
    Quelle: https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin

14. **LLM-basierte TTS (Orpheus u. ä. auf llama.cpp-Basis): Wiederholungsschleifen/Halluzinationen ohne repetition_penalty**
    Symptom: Modell wiederholt Phrasen endlos oder generiert unsinnigen Audio-Output bei längeren Texten.
    Ursache: Wie bei textgenerierenden LLMs verstärkt der wiederholte Kontext die Wahrscheinlichkeit für erneute Wiederholung, wenn kein/zu niedriger repetition_penalty gesetzt ist.
    Betroffen: Orpheus-TTS (Llama-3B-Backbone) und vergleichbare llama.cpp-basierte TTS wie NeuTTS.
    Fix/Workaround: `repetition_penalty >= 1.1` setzen (von den Orpheus-Maintainern als notwendig für stabile Generierung genannt), zusätzlich korrekt konfigurierte Stop-Token und gesunde Sampling-Temperatur.
    Quellen: https://github.com/canopyai/Orpheus-TTS , https://sebastianraschka.com/faq/docs/repetition-loops-generation.html

## Performance / Threading

15. **TTS-/ONNX-Inferenz auf dem Main-Thread verursacht ANR**
    Symptom: App wird als „nicht reagierend“ eingestuft, wenn Synthese oder Modell-Initialisierung synchron auf dem UI-Thread läuft.
    Ursache: TTS-Init-Callbacks und ONNX-Inferenz sind blockierende Operationen; werden sie auf dem Hauptthread ausgeführt, blockieren sie die UI lange genug für einen ANR.
    Betroffen: generisch alle Android-TTS/ONNX-Integrationen ohne Offloading.
    Fix/Workaround: Modell-Load und Synthese konsequent auf Hintergrund-Thread/Coroutine (Dispatchers.Default oder eigener Executor) auslagern, niemals auf dem Main-Thread aufrufen.
    Quelle: https://github.com/chartmann1590/LiveTranscribe-Android/issues/116

16. **Kaltstart-Latenz: erstes Inferenz-Ergebnis eines ONNX-Modells ist deutlich langsamer als Folgeaufrufe**
    Symptom: Der erste synthetisierte Satz nach App-Start braucht spürbar länger als alle folgenden.
    Ursache: Erstinferenz beinhaltet Kernel-Warmup, Cache- und Speicher-Arena-Aufbau sowie ggf. JIT-Kompilierungsanteile der Execution Provider.
    Betroffen: generisch ONNX Runtime (Mobile) auf Android, alle EPs.
    Fix/Workaround: Beim App-/Service-Start proaktiv 1–3 Dummy-Inferenzen („Warmup-Pass“) mit kurzem Dummy-Text durchführen, bevor der Nutzer die erste echte Anfrage stellt.
    Quelle: https://medium.com/@Modexa/8-onnx-runtime-tricks-for-low-latency-python-inference-baee6e535445 (Grundprinzip, plattformübergreifend gültig, für Android in Praxisberichten mehrfach bestätigt: https://soniqo.audio/benchmarks/android)

## Play-Store-Fallen

17. **200-MB-Basis-Modul-Limit trifft ML-Modell-Apps besonders hart**
    Symptom: Play Console lehnt Upload ab bzw. Build überschreitet die komprimierte Downloadgrenze für das Basismodul.
    Ursache: Google Play begrenzt die komprimierte Downloadgröße des Basis-Moduls eines App-Bundles auf 200 MB; neurale TTS-Modelle (insbesondere mehrere Stimmen/Sprachen) sprengen das schnell.
    Betroffen: jede App, die TTS-Modelle direkt im Basismodul bündelt.
    Fix/Workaround: Modelle nicht ins Basismodul packen, sondern über Play Feature Delivery / Play Asset Delivery als On-Demand- oder Install-Time-Asset-Pack ausliefern, oder Modelle beim ersten Start herunterladen (mit Nutzerhinweis wegen Datenvolumen).
    Quellen: https://developer.android.com/guide/playcore/asset-delivery , https://support.google.com/googleplay/android-developer/answer/9859372?hl=en

18. **CC-BY-Piper-Stimmen: Attributionspflicht pro Stimme, nicht pauschal über Repo-Lizenz**
    Symptom: Entwickler übernehmen die Repo-Lizenz von Piper (MIT) und übersehen, dass einzelne Stimmen (z. B. auf LibriTTS basierend) unter CC-BY stehen und eigene Attributionspflichten haben.
    Ursache: Piper-Stimmen sind eigenständige, aus HuggingFace geladene Assets; eine Repo-Lizenz ersetzt nicht die im jeweiligen MODEL_CARD dokumentierten Datensatz-/Sprecherbedingungen.
    Betroffen: alle Piper-Stimmen, deren zugrundeliegender Datensatz CC-BY ist (kommerzielle Nutzung erlaubt, aber nur mit Attribution).
    Fix/Workaround: Für jede eingesetzte Stimme individuell das MODEL_CARD prüfen und bei CC-BY eine sichtbare NOTICE/Attribution im Impressum oder Lizenzbildschirm der App aufnehmen (nicht nur im Source-Repo).
    Quelle: https://github.com/rhasspy/piper/discussions/271 , https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/VOICES.md

## Android-TextToSpeech-API-Fallen (Fallback)

19. **TextToSpeech-Konstruktor unguarded → SecurityException wenn Engine nicht gebunden werden kann**
    Symptom: `SecurityException: Not allowed to bind to service`, App crasht beim Instanziieren von `TextToSpeech`, statt sauber über `onInit(ERROR)` zu degradieren.
    Ursache: Fehlende Absicherung der TTS-Konstruktion; wenn die gewählte Engine (System-TTS) nicht gebunden werden kann (z. B. deaktiviert, deinstalliert, Berechtigungsproblem), wirft der Konstruktor statt eines Fehlercodes eine Exception.
    Betroffen: Android 11 und früher, alle Apps die `new TextToSpeech(context, listener)` ohne try/catch aufrufen.
    Fix/Workaround: Konstruktoraufruf in try/catch kapseln, im catch-Fall `onInit(TextToSpeech.ERROR)`-Pfad simulieren und App auf „TTS nicht verfügbar“ degradieren lassen statt abzustürzen.
    Quelle: https://github.com/brenogonzaga/tauri-plugin-tts/issues/14

20. **Samsung-Geräte: Engine-Auswahl im Systemdialog wechselt nicht wirklich / Stimme bleibt nach Engine-Wechsel bei der alten Engine**
    Symptom: Nutzer wählt im System-Dialog „Google“ statt „Samsung TTS“, das Gerät bleibt aber bei der Samsung-Engine bzw. behält die alte Stimme/das alte Modell der vorherigen Engine bei.
    Ursache: OEM-spezifisches Verhalten der Samsung-TTS-Einstellungen; dokumentiert auch als generisches Problem, dass ein Engine-Wechsel die zuvor gewählte Stimme nicht zurücksetzt.
    Betroffen: Samsung-Geräte (u. a. nach One-UI-7-Beta), sowie generisch capacitor-community/text-to-speech.
    Fix/Workaround: Nach jedem `TextToSpeech`-Neuaufbau aktiv die aktuell aktive Engine/Stimme abfragen (`getDefaultEngine()`, `getVoice()`) statt sich auf zuvor gesetzte Werte zu verlassen; App-eigenes Voice-Caching invalidieren, wenn sich `getDefaultEngine()` ändert.
    Quellen: https://github.com/capacitor-community/text-to-speech/issues/115 , https://us.community.samsung.com/t5/Questions/Text-to-Speech-seems-a-buggy/td-p/3169277

21. **Robotic-Voice-Bug auf älteren Samsung-Geräten**
    Symptom: System-TTS liefert eine deutlich roboterhafte/verzerrte Stimme.
    Ursache: nicht abschließend geklärt, gerätespezifisches Verhalten des Samsung-TTS-Stacks.
    Betroffen: u. a. Galaxy Note FE (One UI 1, Android 9), Galaxy Note 9 (One UI 2, Android 10).
    Fix/Workaround: als Fallback-Strategie System-TTS nicht blind vertrauen — bei erkannter Samsung-Engine ggf. auf Google-TTS-Engine als bevorzugten Fallback umschalten, sofern installiert.
    Quelle: https://github.com/OyaCanli/awesome-android-oem-issues/discussions/2

22. **Hintergrund-Wiedergabe (TTS als Vorleser) wird durch Background-Audio-Hardening/Doze unterbrochen**
    Symptom: Wiedergabe stoppt an Satz-/Artikelgrenzen, weil ein erneuter Audiofocus-Request abgelehnt wird, sobald die App im Hintergrund ist.
    Ursache: Neuere Android-Versionen lehnen Hintergrund-Audiofocus-Requests von Apps ab, die nicht für die Ausnahme qualifizieren (kein laufender `mediaPlayback`-Foreground-Service mit Notification).
    Betroffen: generisch alle TTS-Vorlese-Apps ohne korrekten Foreground-Service; verschärft ab neueren Android-Versionen mit „Background audio hardening“.
    Fix/Workaround: Durchgehenden Foreground-Service vom Typ `mediaPlayback` mit Notification während der gesamten Vorlese-Session halten (nicht nur punktuell pro Satz starten/stoppen), Audiofocus einmal für die Session halten statt pro Satz neu anzufordern.
    Quellen: https://github.com/seazon/FeedMe/issues/225 , https://developer.android.com/about/versions/17/changes/bg-audio , https://developer.android.com/media/media3/session/background-playback

## BEST-PRACTICES-KANDIDATEN

- onnxruntime-Version exakt an die vom TTS-Framework (sherpa-onnx) erwartete Version pinnen, nie eigenmächtig aktualisieren, ohne Kompatibilität zu prüfen. Quelle: https://github.com/Codename-11/hermes-relay/issues/444
- Modelldateien nach Download per Checksumme verifizieren, bevor sie an die native Ladefunktion übergeben werden — ein korruptes Modell kann den Prozess hart beenden, ein Java-try/catch hilft nicht. Quelle: https://github.com/k2-fsa/sherpa-onnx/issues/3987
- Native .so-Dateien / AAR-Abhängigkeiten explizit auf 16-KB-Page-Size-Kompatibilität prüfen (NDK r28+ bzw. passendes Linker-Flag), besonders vor dem Play-Store-Stichtag 1.11.2025 für Android-15+-Ziel-SDKs. Quelle: https://developer.android.com/guide/practices/page-sizes
- AudioTrack-Samplerate immer dynamisch aus dem Modell-Output übernehmen, nie hardcoden; bei Sampleratenwechsel AudioTrack neu anlegen statt live umzukonfigurieren, um Knackgeräusche zu vermeiden. Quelle: https://developer.android.com/ndk/guides/audio/sampling-audio
- Eigene deutsche Textnormalisierung (Zahlen, Daten, Uhrzeiten, Abkürzungen, Einheiten, Ordinalzahlen) als Vorstufe vor jede espeak-ng-/Kokoro-basierte Synthese schalten. Quellen: https://github.com/repodiac/german_transliterate , https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin
- Bei LLM-basierten TTS-Engines (Orpheus & Co.) `repetition_penalty >= 1.1` sowie korrekte Stop-Token-Konfiguration setzen, um Wiederholungsschleifen bei langen Texten zu verhindern. Quelle: https://github.com/canopyai/Orpheus-TTS
- TTS-Modell-Init und -Synthese grundsätzlich off-main-thread ausführen und beim App-/Service-Start 1–3 Warmup-Inferenzen durchführen, um ANRs und hörbare Erstlatenz zu vermeiden. Quellen: https://github.com/chartmann1590/LiveTranscribe-Android/issues/116 , https://medium.com/@Modexa/8-onnx-runtime-tricks-for-low-latency-python-inference-baee6e535445
- TTS-Modelle nicht im 200-MB-Basismodul bündeln, sondern über Play Asset Delivery / Feature Delivery oder Download-beim-ersten-Start ausliefern. Quelle: https://developer.android.com/guide/playcore/asset-delivery
- Für jede eingesetzte Piper/VITS-Stimme individuell das MODEL_CARD auf CC-BY-Attributionspflicht prüfen und NOTICE sichtbar in der App (nicht nur im Repo) unterbringen. Quelle: https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/VOICES.md
- `TextToSpeech`-Konstruktion immer in try/catch kapseln und bei Bind-Fehlern sauber auf „nicht verfügbar“ degradieren statt abzustürzen; nach jedem Neuaufbau aktive Engine/Stimme aktiv abfragen statt gecachte Werte zu vertrauen (Samsung-Falle). Quellen: https://github.com/brenogonzaga/tauri-plugin-tts/issues/14 , https://github.com/capacitor-community/text-to-speech/issues/115
- Für Hintergrund-Vorlesen einen durchgehenden `mediaPlayback`-Foreground-Service mit Notification für die gesamte Session halten, Audiofocus nicht pro Satz neu anfordern. Quellen: https://developer.android.com/about/versions/17/changes/bg-audio , https://github.com/seazon/FeedMe/issues/225
- Bei API < 31 nicht ausschließlich auf `onError(String, int)` verlassen — die deprecated 1-Parameter-Variante als primäre Fehlerquelle mitimplementieren. Quelle: https://issuetracker.google.com/issues/138321382

## Unklar / nicht ausreichend belegt

- Root-Cause des Piper-„letzte-Wörter-abgeschnitten“-Bugs auf Google-Speakern wurde im Issue nicht geklärt (als „not planned“ geschlossen). Quelle: https://github.com/home-assistant/core/issues/150397
- Ein spezifischer, dokumentierter Android-TTS-Bugfall zu UTF-8/Umlaut-Mojibake (ü/ß) wurde nicht gefunden; das Risiko ist aus allgemeinen Encoding-Grundlagen plausibel (UTF-8-Bytes als Latin-1 interpretiert → „Ã¼“ statt „ü“), aber kein konkreter Android-TTS-Case mit Quelle belegt.
