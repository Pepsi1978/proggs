# TTS-Provider (Edge-TTS, Google Chirp 3 HD, Android-native) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext darunter = Pflicht bei
> JEDEM Fehler. Sektionen: **E** Edge-Auth · **ET** Edge-Text/Audio · **G** Chirp-Request ·
> **GA** Google-Auth/Quota · **N** Android-native · **AC** Android-Client · **W** Chrome-MV3 · **C** Querschnitt.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Edge `403 Invalid response status` | Sec-MS-GEC-Token erzeugen (SHA-256, 5-Min-Bucket); Lib updaten | E1 |
| 2 | Edge 403 trotz Token | Systemuhr/NTP; Clock-Skew aus `Date`-Header korrigieren | E2 |
| 3 | Edge „No audio received" | Heim-/Mobil-IP (kein Datacenter); Text nicht leer; Voice-ID prüfen | E3, ET6, ET10 |
| 4 | Edge eigenes SSML | Geht nicht — nur ein `<voice>`+`<prosody>`, nur rate/volume/pitch (`±N%`/`±NHz`) | ET1, ET4 |
| 5 | Edge `&`/`<`/`>` im Text (Eigenbau) | XML-escapen vor SSML-Bau; C0-Steuerzeichen filtern | ET7, ET8 |
| 6 | Chirp Streaming + MP3 | Streaming kann kein MP3 → OGG_OPUS/PCM, oder Batch `text:synthesize` | G1 |
| 7 | Chirp `audioContent` 0 Bytes | REST liefert **Base64** → erst dekodieren, dann abspielen | G12 |
| 8 | Chirp `INVALID_ARGUMENT` langer dt. Text | 5.000-**Byte**-Limit; Umlaute/ß = 2 Bytes; nach Bytes chunken | G3 |
| 9 | Chirp `[pause]` wirkt nicht | Nur im `markup`-Feld; text/ssml/markup exklusiv (genau eins) | G4, G8 |
| 10 | Chirp Tonhöhe/Voice-Fehler | Kein pitch (nur speaking_rate); exakt `de-DE-Chirp3-HD-<Name>` | G7, G9 |
| 11 | Google `403`/`429` | API+Billing aktiv? eigener Key pro Plattform; Chirp3 = 200 RPM/Projekt | GA8, GA9, GA2, GA11 |
| 12 | Google-Key sichtbar/teuer | Key im Header `x-goog-api-key`, nie URL/Bundle; Chirp3 30$/Mio nach 1 Mio | GA1, GA15 |
| 13 | Native TTS spricht nicht | Nichts vor `onInit==SUCCESS`; `setLanguage`-Return prüfen; de_DE-Daten | N1, N3 |
| 14 | Native „offline" trotzdem stumm | Voice mit `!isNetworkConnectionRequired()` wählen | N14 |
| 15 | MediaPlayer-Crash/Geister-Audio | State-Machine + `reset()`; Listener nullen vor `release()` + ID-Guard | AC1, AC4 |
| 16 | Falsche/abgehackte Cache-Datei | Atomar `.part`→rename; Cache-Key = text+provider+voice+rate+format | AC9, AC12 |
| 17 | Stop bricht Synthese nicht ab | `suspendCancellableCoroutine`+`invokeOnCancellation` schließt WS/Player | AC7 |
| 18 | MV3: Audio stirbt nach ~30s | WS UND Wiedergabe ins **Offscreen-Document**, nicht in den Service-Worker | W1, W8 |
| 19 | MV3: `Edg/143`-UA-Regel wirkungslos | declarativeNetRequest greift nicht bei WS-aus-SW (crbug 1285664) → WS im Offscreen-Doc | W8 |
| 20 | sherpa-onnx Piper/Kokoro in Closed-Source-App | espeak-ng GPL-3.0 statisch gelinkt → auslagern/GPL/System-TTS | L1 |
| 21 | Piper-Stimme lessac/amy/tom verbaut | NC/AGPL/unklar → thorsten/kristin/siwis/davefx | L2 |
| 22 | Deutsch mit Kokoro | Kein DE → Piper thorsten/System-TTS | L3 |
| 23 | On-Device-Diktat Samsung `false`/Error 13 | Standard-Recognizer/whisper.cpp als Fallback | L4 |
| 24 | Edge in Verkaufs-App | Inoffiziell + wiederkehrend 403 → nie Default, lokaler Fallback | L5 |
| 25 | Samsung SMT fehlt in `getEngines()` trotz `<queries>` (SM-F971B) | Engine explizit binden, Samsung nicht anbieten; Gerätebefund, keine allgemeine Regel | N-Nachtrag |
| 26 | Lokales Modell crasht hart beim Laden | Korrupte Datei → native Exception; Checksumme vor dem Laden, Java-catch hilft nicht | L6 |
| 27 | Samsung-Neural-Stimmen fehlen (One UI 7/8) | Für Fremd-Apps gesperrt → Google oder eigene Engine | L7 |
| 28 | `OrtGetApiBase not found` | onnxruntime exakt auf die Version von sherpa-onnx pinnen | L8 |
| 29 | Knacken / falsche Tonhöhe | AudioTrack-Rate aus dem Modell lesen, nie hart eintragen | L10 |
| 30 | Letzte Wörter fehlen | Auf leere Queue warten, Stille anhängen | L11 |
| 31 | LLM-TTS wiederholt sich | `repetition_penalty ≥ 1.1`, satzweise | L12 |
| 32 | SIGABRT `key not found` | Zeichen-Whitelist + Normalisierung | L13 |
| 33 | Deutsche Zahlen/Abkürzungen falsch | Eigene Normalisierung vorschalten | L14 |
| 34 | „espeak-frei“, aber doch nicht | Misaki, KittenTTS und NeuTTS nutzen espeak; frei sind nur Supertonic, Pocket TTS und das Lexikon-Muster | L15 |
| 35 | Vorlesen stoppt im Hintergrund | `mediaPlayback`-FGS für die ganze Session | L16 |
| 36 | CC-BY-Stimme ohne Nennung | NOTICE pro Stimme in der App | L17 |
| 37 | TTS-Konstruktor `SecurityException` | `try/catch`, beide `onError`-Overloads | L18 |
| 38 | Chirp 3 HD sagt „dreißig fünf“ statt „dreißig Komma fünf“ | Zahlen vorher ausschreiben oder SSML `say-as` (synchron, Preview) | V1 |
| 39 | Gemini-TTS liest Anweisung vor / HTTP 400 „tried to generate text“ | Nur Transkript senden, Stil ins Stilfeld | V2 |
| 40 | Gemini-TTS Audio zu kurz trotz 200/STOP | Dauer gegen Textlänge prüfen, kürzer teilen, begrenzt wiederholen | V3 |
| 41 | Edge 403 ab 10.10.2026 | Token v2 (`Sec-MS-GEC-Version=2-…`, edge-tts 7.3.1); W10 überholt | V4 |
| 42 | Qwen berechnet 1,34 × Zeichen | `usage` protokollieren, Faktor einplanen | V5 |
| 43 | TTS-Modell plötzlich weg (Gemini 2.5/3.1, OpenAI, Hume, Qwen-VC) | Abschalttermine prüfen, Rückfall-Anbieter | V6 |
| 44 | Cloud-TTS-Kosten laufen trotz Budget weiter | keine Kappung für Cloud TTS; App-Zähler bzw. Gemini-Prepay | V7 |
