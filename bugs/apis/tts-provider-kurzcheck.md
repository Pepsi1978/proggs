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
