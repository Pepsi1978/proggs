# Bekannte Bugs: TTS-Provider (Edge-TTS, Google Chirp 3 HD, Android-native)

> PFLICHT-LESEN vor Arbeit an der Vorlese-/TTS-Anbindung (BestJournal Android, `vorlese-overlay-v2`).
> Stand: tief recherchiert am 2026-06-14 (7 Researcher parallel, ~110 Rohfunde → dedupliziert).
> Versions-Anker: `rany2/edge-tts` 7.2.8 (22.03.2026) / Edge-Endpunkt Chromium-Emulation **143**;
> Google Cloud Text-to-Speech API v1, Chirp 3: HD (de-DE GA), SSML/Pause/Pronunciation = **Preview**
> (Doku 09.06.2026), Quotas 01.06.2026; Android `TextToSpeech` (compileSdk 35, minSdk 26);
> Chrome MV3 (Offscreen-API ab Chrome 109, WS-Keepalive ab Chrome 116).
> Fokus deutsche Stimmen, multilingual mitgedacht. **ElevenLabs bewusst ausgelassen (zu teuer).**
> Zweite Seite (wie macht man es richtig):
> [`best-practices/apis/tts-provider.md`](../../best-practices/apis/tts-provider.md).

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

---

## E) Edge-TTS — Authentifizierung & Verbindung

### E1. `403 Invalid response status` — Sec-MS-GEC-Token fehlt ⭐ HAEUFIG
- **Symptom:** WebSocket-Handshake `403, message='Invalid response status'`; Audio kommt nie, WS-Close oft 1006.
- **Ursache:** Microsoft verlangt seit ~Nov 2024 zwei Query-Parameter zusätzlich zum `TrustedClientToken`: `Sec-MS-GEC` (SHA-256-Hash) + `Sec-MS-GEC-Version`. Ohne beide → sofort 403.
- **Versionen:** edge-tts < 6.1.18 ohne Implementierung; eigener Kotlin-/JS-Code muss es selbst bauen.
- **FIX:** Token exakt nach `drm.py` erzeugen: Windows-FileTime-Ticks (100-ns seit 1601-01-01) → auf Vielfaches von `3.000.000.000` abrunden (= neuer Token alle 5 Min) → String `f"{ticks}6A5AA1D4EAFF4E9FB37E23D68491D6F4"` → SHA-256 → **Hex uppercase**. URL: `...&Sec-MS-GEC=<hash>&Sec-MS-GEC-Version=1-143.0.3650.75&ConnectionId=<uuid-ohne-bindestriche>`. C#/Kotlin: `ticks = DateTime.Now.ToFileTimeUtc(); ticks -= ticks % 3_000_000_000;`.
- **Quelle:** https://github.com/rany2/edge-tts/issues/290 · https://raw.githubusercontent.com/rany2/edge-tts/master/src/edge_tts/drm.py

### E2. 403 trotz korrektem Token — Systemuhr / Clock-Skew ⭐ HAEUFIG
- **Symptom:** Token-Algorithmus nachweislich korrekt, trotzdem 403; auf anderem Gerät geht es; gehäuft in VMs / auf Android ohne Netz-Zeit.
- **Ursache:** Der Hash basiert auf der lokalen Systemzeit (5-Min-Bucket). Weicht die Uhr > ~5 Min ab, fällt der Client ins falsche Zeitfenster → 403.
- **Versionen:** geräteabhängig, jederzeit.
- **FIX:** Systemzeit per NTP genau halten. Offizielle Lösung (drm.py): bei 403 den `Date`-Header der Server-Antwort lesen, in Unix-Zeit parsen, Differenz als `clock_skew_seconds` speichern, bei allen Token aufaddieren, Request EINMAL wiederholen. Fehlt `Date` → `SkewAdjustmentError`. Diese Skew-Korrektur fehlt in den meisten Eigen-Ports — unbedingt mitportieren.
- **Quelle:** https://raw.githubusercontent.com/rany2/edge-tts/master/src/edge_tts/drm.py · https://github.com/rany2/edge-tts/issues/344

### E3. „No audio received" — Datacenter-/Cloud-IP-Filter ⭐ HAEUFIG
- **Symptom:** Handshake gelingt (kein 403), `turn.start` evtl. da, aber NIE Audio-Frames → `NoAudioReceived`. Lokal/Mobil ok, auf Servern/Colab/HF/AWS/GCP/Azure fehlschlägt.
- **Ursache:** Microsoft filtert bekannte Datacenter-IP-Ranges: WS akzeptiert, aber keine Audio-Frames. Residential/Mobil-IP funktioniert.
- **Versionen:** alle — serverseitige Politik.
- **FIX:** Vom Heim-/Mobil-IP senden (Frank: Handy ok). Vorsicht bei VPNs, die auf Datacenter-IPs rauskommen. Server-/Cloud-Betrieb zwingend: offizielle Azure Speech TTS (stabil) oder Residential-Proxy.
- **Quelle:** https://github.com/rany2/edge-tts/issues/74 · https://github.com/rany2/edge-tts/issues/351 · https://learn.microsoft.com/en-us/answers/questions/5653188

### E4. 403 nach Chromium-Bump — `Sec-MS-GEC-Version` veraltet
- **Symptom:** Lief monatelang, dann wieder 403; teils regional.
- **Ursache:** `Sec-MS-GEC-Version` = `1-<Chromium-Vollversion>` (aktuell `1-143.0.3650.75`). Microsoft kann veraltete Versionen abweisen. **Realität (Stand 06/2026): aktuell NICHT streng validiert** (sogar `1-1.1.1.1` ging zeitweise) — kann sich aber jederzeit verschärfen.
- **Versionen:** rollierend; Referenz war zwischenzeitlich auf 130, jetzt 143.
- **FIX:** Chromium-Vollversion als EINE zentrale Konstante führen; in der MV3-Extension dieselbe Major aus der `declarativeNetRequest`-`Edg/143`-Regel ableiten (UA-Major UND Sec-MS-GEC-Version aus einer Variable → laufen nie auseinander). Bei 403-Schub zuerst Token (E1), nicht die Version prüfen.
- **Quelle:** https://github.com/rany2/edge-tts/issues/290 · https://github.com/travisvn/edge-tts-universal/issues/19

### E5. Falscher/fehlender User-Agent & Origin
- **Symptom:** 403 oder stille Ablehnung trotz korrektem Token.
- **Ursache:** Dienst erwartet UA mit `Edg/<major>` UND einen bestimmten `Origin: chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold` (feste ID der echten Read-Aloud-Komponente — kein eigener Wert) sowie `Sec-WebSocket-Version: 13`.
- **Versionen:** alle Eigen-Implementierungen ohne diese Header.
- **FIX:** In OkHttp (Android) alle Header setzen (`Request.Builder().header(...)`): UA mit `Edg/143`, obigen Origin, `Pragma/Cache-Control: no-cache`. Im Browser (MV3) nicht setzbar → siehe W8/W9.
- **Quelle:** https://raw.githubusercontent.com/rany2/edge-tts/master/src/edge_tts/constants.py

### E6. Fehlende MUID-Cookie → sporadische Abweisung / schnelleres Rate-Limit
- **Symptom:** Vereinzelte Verbindungsabweisungen, weniger zuverlässig als echter Edge-Client.
- **Ursache:** Echter Edge sendet `Cookie: muid=<32 Hex uppercase>;`. Neuere Referenz/`edge-tts-universal` ≥ v1.4.0 ergänzen eine zufällige MUID.
- **Versionen:** ältere Clients ohne MUID.
- **FIX:** Pro Verbindung zufällige MUID (`secrets.token_hex(16).upper()` bzw. 16 Random-Bytes uppercase-hex) als `Cookie: muid=...;` mitsenden.
- **Quelle:** https://raw.githubusercontent.com/rany2/edge-tts/master/src/edge_tts/drm.py · https://github.com/travisvn/edge-tts-universal

### E7. Rate-Limiting / temporäre Sperre bei zu vielen parallelen Streams
- **Symptom:** Nach vielen schnellen/parallelen Requests plötzlich 403 oder NoAudioReceived, regional gehäuft; erholt sich nach Wartezeit.
- **Ursache:** IP-basierte Drosselung; Burst/viele gleichzeitige WS-Streams lösen temporäre Sperren aus; Datacenter-IPs härter (E3).
- **Versionen:** alle — serverseitig.
- **FIX:** Parallele Streams pro IP begrenzen (1–2), serialisieren/staffeln; bei 403 exponentielles Backoff + EIN Retry mit Clock-Skew-Korrektur (E2); langen Text sequenziell statt massiv parallel.
- **Quelle:** https://github.com/rany2/edge-tts/issues/286 · https://github.com/rany2/edge-tts/issues/433

### E8. Frame-Parsing: UnknownResponse / WordBoundary-Drift bei langem Text
- **Symptom:** `Unknown metadata type` / `No WordBoundary metadata found`; Untertitel/Highlighting laufen bei langen/mehrteiligen Texten aus dem Takt.
- **Ursache:** Dienst sendet getrennte Text-Frames (JSON: `turn.start`, `turn.end`, `audio.metadata` mit `WordBoundary`/`SentenceBoundary`) und Binär-Frames (MP3 mit 2-Byte-Header = Header-Länge, dann `Path:audio\r\n`). Default-Boundary ist `SentenceBoundary`. Roh-Offsets von Microsoft driften über Chunks.
- **Versionen:** strikte/ältere Parser; Offset-Padding korrekt erst in neueren Versionen.
- **FIX:** Binär-Frame: erste 2 Bytes (Big-Endian) = `headerLength`, Audio ab `2+headerLength`. Unbekannte Metadatentypen tolerant ignorieren. Für mehrteilige Synthese die Offsets pro Folge-Chunk um die bisherige Audiodauer verschieben — aus CBR-Bytes berechnen (`ticks = bytes*8*10_000_000 // 48000`), NICHT Microsoft-Roh-Offsets aufsummieren. `boundary="WordBoundary"` für wortgenaue Highlights; Metadaten-Text `unescape`-n.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py

### E9. `Communicate`/Stream nur EINMAL konsumierbar (Port-Falle)
- **Symptom:** `RuntimeError: stream can only be called once` bei Wiederverwendung/Retry auf demselben Objekt.
- **Ursache:** `stream()` setzt ein Flag; `texts` ist ein erschöpfbarer Generator.
- **Versionen:** alle 6.x/7.x.
- **FIX:** Pro Versuch/Konsumierung ein FRISCHES Objekt; Audio + Metadaten in EINEM Durchlauf abgreifen. Im Eigen-Code analog: pro Request frischer WS/Generator.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py

---

## ET) Edge-TTS — Text, SSML, Audio-Ausgabe

### ET1. Custom-SSML wird abgelehnt — nur EIN `<voice>` + EIN `<prosody>` ⭐ HAEUFIG
- **Symptom:** Eigenes SSML mit mehreren `<voice>`/`<prosody>`, `<break>`, `<say-as>`, `<phoneme>` liefert keinen/falschen Ton.
- **Ursache:** Microsoft akzeptiert nur exakt das von Edge erzeugte Template: ein `<voice>` mit genau einem `<prosody>`. rany2 hat Custom-SSML deshalb entfernt.
- **Versionen:** alle aktuellen, per Design.
- **FIX:** Kein Roh-SSML senden. Nur Klartext; Variation über `rate`/`volume`/`pitch` + Stimmenwahl. Pausen über Textgliederung (Absätze/Satzzeichen). Im Eigen-Code das `mkssml`-Template 1:1 nachbauen.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py

### ET2. Sprechstil `<mstts:express-as>` (style) wird ignoriert
- **Symptom:** Injizierter Stil (cheerful/sad/newscast) spielt Default ab.
- **Ursache:** Read-Aloud nimmt `mstts:express-as` nicht an (Styles nur in bezahlter Azure-Speech-API).
- **Versionen:** alle, per Design.
- **FIX:** Stimme nach passender `VoicePersonality` (aus `--list-voices`) wählen + `rate`/`pitch`-Feintuning. Keine Stil-Tags.
- **Quelle:** https://github.com/rany2/edge-tts/issues/426

### ET3. Multilinguale Stimmen: Fehl-Aussprache bei gemischtem Text, `<lang>` wirkungslos
- **Symptom:** `de-DE-...MultilingualNeural` spricht gemischtsprachige Passagen teils falsch; `<lang xml:lang>` wird ignoriert.
- **Ursache:** (a) `<lang>` ist Custom-SSML → nicht erlaubt (ET1). (b) Multilingual-Stimmen raten die Sprache automatisch, bei kurzen/gemischten Fragmenten oft daneben.
- **Versionen:** alle, Stimmen-Limit.
- **FIX:** Gemischten Text in einsprachige Segmente zerlegen, je mit passender Landesstimme (de mit `de-DE-...`, en mit `en-US-...`) synthetisieren, MP3-Chunks zusammenfügen. Für rein deutschen Text reine `de-DE`-Stimme.
- **Quelle:** https://learn.microsoft.com/en-ca/answers/questions/1663230

### ET4. Strikte rate/volume/pitch-Syntax; CLI-Negativwerte
- **Symptom:** `rate="1.5"`/`"fast"`, `volume="80"`, `pitch="+5%"` → `ValueError: Invalid …`; CLI `--rate -50%` bricht (als Flag interpretiert).
- **Ursache:** Regex hart: rate/volume `^[+-]\d+%$`, pitch `^[+-]\d+Hz$` (Vorzeichen Pflicht, ganzzahlig, korrekte Einheit). CLI: führendes `-` = neues Flag.
- **Versionen:** alle 6.x/7.x.
- **FIX:** Immer mit Vorzeichen + Einheit: `±N%` (rate/volume), `±NHz` (pitch); Default `+0%`/`+0Hz`. CLI: `--rate=-50%` (mit `=`). Im Eigen-Code dieselbe Regex vorschalten.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/data_classes.py · README

### ET5. Lange Texte → stiller Abbruch / abgeschnittenes Audio
- **Symptom:** Sehr langer Text bricht willkürlich ab (mal alles, mal Teil) ohne Fehler; berichtet auch ~55-Min-Cap.
- **Ursache:** Server begrenzt/blockt still; SSML-Nachrichten dürfen nicht beliebig groß sein.
- **Versionen:** 6.1.9 und früher stark; aktuell bessere Erkennung (`NoAudioReceived`), Server-Limit bleibt.
- **FIX:** Intern splittet edge-tts auf ≤4096 Byte an Wort-/Zeilengrenzen. Zusätzlich serverfern in kleine Absätze/Sätze (≤500–1000 Zeichen) zerlegen, pro Chunk separat, kurze Pausen zwischen Requests, Chunk erneut versuchen wenn Audio fehlt/kürzer als erwartet. Nie ein Riesen-Request.
- **Quelle:** https://github.com/rany2/edge-tts/issues/190

### ET6. Leerer / Whitespace- / nur-Satzzeichen-Text → `NoAudioReceived`
- **Symptom:** `NoAudioReceived` auch bei `"Hello, world! "` (trailing space) oder nach Trim leeren Strings.
- **Ursache:** edge-tts strippt Chunks; bleibt nichts → kein Audio. Server liefert für inhaltslosen Input still nichts.
- **Versionen:** alle.
- **FIX:** Vor dem Senden trimmen + prüfen, ob sprechbarer Inhalt übrig bleibt; leere/nur-Satzzeichen-Segmente überspringen. `NoAudioReceived` als „nichts zu sprechen" behandeln, nicht als Crash.
- **Quelle:** https://github.com/rany2/edge-tts/issues/433

### ET7. C0-Steuerzeichen (vertikaler Tab etc.) → Dienstfehler
- **Symptom:** Texte aus OCR/PDF mit `\x0b` o. ä. lösen Fehler aus.
- **Ursache:** Dienst akzeptiert bestimmte Bereiche nicht; edge-tts ersetzt Codepoints 0–8, 11–12, 14–31 durch Space.
- **Versionen:** alle.
- **FIX:** Im Eigen-Code (Kotlin/JS) alle C0-Steuerzeichen außer `\t`(9), `\n`(10), `\r`(13) durch Space ersetzen, vor SSML-Bau.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py

### ET8. XML-Sonderzeichen `&`/`<`/`>` müssen escaped werden ⭐ HAEUFIG (Eigenbau)
- **Symptom:** "Tom & Jerry", "a < b", HTML-Schnipsel → ungültiges SSML → nichts/Fehler.
- **Ursache:** Roher Text im SSML-XML; unescapte `&`/`<`/`>` zerbrechen das XML.
- **Versionen:** Bibliothek macht es korrekt (`xml.sax.saxutils.escape`); Gefahr im selbstgebauten Pfad.
- **FIX:** Vor dem Einsetzen ins `<prosody>` escapen → `&amp;`/`&lt;`/`&gt;`; Chunk-Grenzen nicht mitten durch Entities legen. **Umlaute (ä/ö/ü/ß) und Emojis brauchen KEIN Escaping** (gültiges UTF-8) — nur die drei Metazeichen.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py · https://learn.microsoft.com/en-us/answers/questions/5789672

### ET9. Multibyte-UTF-8 an selbst gesetzter Chunk-Grenze → kaputtes Audio
- **Symptom:** Beim eigenen Byte-genauen Splitten dt. Texte (ä/ö/ü/ß, Emojis) entstehen ungültige UTF-8-Sequenzen → Fehler/Knackser.
- **Ursache:** ä/ö/ü/ß = 2 Bytes, Emojis = 4 Bytes; Schnitt mitten im Mehrbyte-Zeichen = invalid.
- **Versionen:** Bibliothek korrekt; Gefahr im Eigenbau.
- **FIX:** Nie auf Byte-Index schneiden, sondern auf Zeichen-/Wortgrenzen; in Kotlin per `String`-Char-Grenzen statt `ByteArray`-Offsets; nach Schnitt UTF-8-Gültigkeit prüfen.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py

### ET10. Voice-Name-Tippfehler / unbekannte Stimme → Fehler, KEIN Auto-Fallback
- **Symptom:** Falsche/zurückgezogene Stimme → lokal `ValueError: Invalid voice` oder serverseitig Code **1007 Unsupported voice**. Kein automatischer Fallback.
- **Ursache:** Regex `^([a-z]{2,})-([A-Z]{2,})-(.+Neural)$`; Locale-Schreibweise streng (`de-DE`, nicht `de-de`).
- **Versionen:** alle; 1007 serverseitig.
- **FIX:** Voice-Namen aus `--list-voices` exakt übernehmen (`de-DE-KatjaNeural`, `de-DE-ConradNeural`, `de-DE-SeraphinaMultilingualNeural`); Whitelist + definierter Fallback statt Crash.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/data_classes.py · https://learn.microsoft.com/en-us/answers/questions/1148991

### ET11. MP3-Chunks zusammenfügen → Knackser/Choppy
- **Symptom:** Aneinandergehängte Satz-MP3s „poppen"; auf Android beim MediaPlayer-Wechsel pro Satz Pausen/Knackser.
- **Ursache:** Jeder Chunk = eigener MP3-Stream mit Encoder-Delay/Padding-Stille an den Nähten; MediaPlayer pro Satz neu = Pausen.
- **Versionen:** alle (Format/Player-Verhalten).
- **FIX:** Format ist fest 48 kbps CBR/mono/24 kHz → CBR-Frames lassen sich ohne Re-Encoding konkatenieren. Chunks zu EINER Datei/EINER Player-Sitzung zusammenführen; in der Extension alle Bytes in EINEN Buffer/Blob sammeln, nicht Mini-Snippets einzeln; Padding-Stille tolerieren/trimmen.
- **Quelle:** https://github.com/rany2/edge-tts/blob/master/src/edge_tts/communicate.py · https://github.com/readest/readest/issues/1777

---

## G) Google Chirp 3 HD — Request & Format

### G1. Streaming liefert KEIN MP3 ⭐ HAEUFIG
- **Symptom:** `streamingSynthesize` + MP3 → INVALID_ARGUMENT/Stille; Batch `text:synthesize` + MP3 geht.
- **Ursache:** Streaming-Formate nur ALAW/MULAW/OGG_OPUS/PCM; MP3 nur Batch.
- **Versionen:** v1, Chirp 3 HD, per Design.
- **FIX:** Im Streaming OGG_OPUS/PCM nehmen; MP3 → Batch (euer aktueller Pfad — kein Problem).
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G2. SSML nur synchron, nicht im Streaming; unbekannte Tags still ignoriert ⭐ HAEUFIG
- **Symptom:** SSML wirkt im Streaming nicht; synchron werden manche Tags wortlos ignoriert.
- **Ursache:** Chirp-3-HD-SSML = Preview, nur synchron. Unterstützt: `<speak> <say-as> <p> <s> <phoneme> <sub> <break> <audio> <prosody> <voice>`. Nicht gelistete Tags ignoriert.
- **Versionen:** v1, SSML = Preview.
- **FIX:** Nur gelistete Tags, wohlgeformtes XML. Pausen via `<break>` (SSML) oder `[pause]` (markup). text/ssml/markup exklusiv.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G3. 5.000-BYTE-Limit (nicht Zeichen!) — Umlaute/ß doppelt ⭐ HAEUFIG
- **Symptom:** Dt. Text „unter 5000 Zeichen" → `INVALID_ARGUMENT`/„too long".
- **Ursache:** 5.000 **Bytes** UTF-8. ä/ö/ü/ß = 2 Bytes, CJK/Emoji 3–4; SSML-Tags zählen mit. Limit nicht erhöhbar.
- **Versionen:** v1, alle Stimmen.
- **FIX:** Byte-Länge prüfen: Kotlin `text.toByteArray(UTF_8).size`, JS `new TextEncoder().encode(text).length`. An Satz-/Absatzgrenzen in <5000-Byte-Chunks (Ziel ~4500), MP3-Teile zusammenfügen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas · https://cloud.google.com/php/docs/reference/cloud-text-to-speech/latest/V1.SynthesisInput

### G4. Pause-Tags nur im `markup`-Feld, nicht in `text`
- **Symptom:** `[pause]`/`[pause long]` im `text`-Feld werden vorgelesen/ignoriert.
- **Ursache:** Pause-Control nur in `input.markup`; `markup`/`text`/`ssml` exklusiv.
- **Versionen:** v1, Chirp 3 HD (Pause = Preview).
- **FIX:** `input.markup` statt `input.text`, sobald Pausen nötig. Reines `text` wenn keine Pausen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd · https://discuss.google.dev/t/chirp3-hd-voices-dont-support-markup-field-in-long-audio-synthesis/185977

### G5. Pause-Control NICHT in allen Locales (Doku widersprüchlich) — de-DE OK
- **Symptom:** `markup`+`[pause]` → `400 Pause tags are not supported for locale '<xx>'`.
- **Ursache:** Ausgeschlossen: bg-bg, cs-cz, el-gr, et-ee, he-il, hr-hr, hu-hu, lt-lt, lv-lv, pa-in, ro-ro, sk-sk, sl-si, sr-rs, yue-hk. **de-DE NICHT ausgeschlossen → funktioniert.**
- **Versionen:** v1, Preview.
- **FIX:** Für ausgeschlossene Locales auf Interpunktion ausweichen; 400 abfangen → auf `text` zurückfallen, nicht crashen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd · https://discuss.google.dev/t/chirp-3-hd-pause-control-not-available-for-all-locales-contrary-to-docs/261493

### G6. Doppelte/eng benachbarte `[pause]`-Tags → „garbled" Audio
- **Symptom:** Selten (~1/27) ~1 s völlig verstümmeltes Audio; häufiger bei zwei Pause-Tags direkt nebeneinander.
- **Ursache:** Generatives Modell, Pause = Preview, nicht deterministisch.
- **Versionen:** v1, Preview.
- **FIX:** Pause-Tags nur an natürlichen Grenzen, nie zwei hintereinander; lange Pause = EIN `[pause long]`; alternativ Interpunktion.
- **Quelle:** https://discuss.google.dev/t/chirp-3-voices-output-garbled-words-on-markup/245346

### G7. Kein pitch — nur `speaking_rate`; Doku widersprüchlich
- **Symptom:** `pitch` wirkt nicht/abgelehnt; Verwirrung ob `speaking_rate` erlaubt.
- **Ursache:** „list-voices"-Seite sagt „kein speaking_rate/pitch", chirp3-hd-Seite dokumentiert `speaking_rate` als unterstützt. Fakt 06/2026: **speaking_rate JA (0.25–2.0), pitch NEIN.**
- **Versionen:** v1, Pace-Control GA, Doku-Widerspruch.
- **FIX:** `speaking_rate` 0.25–2.0 (1.0 normal); `pitch` weglassen (sonst INVALID_ARGUMENT). Wer Pitch braucht: andere Stimmfamilie.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd · https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types

### G8. INVALID_ARGUMENT bei zwei/null Input-Feldern oder ungültigem SSML
- **Symptom:** 400 trotz korrekter Stimme/Encoding.
- **Ursache:** `SynthesisInput` erlaubt GENAU EIN Feld (`text`/`ssml`/`markup`). Beide oder keins → 400. Ungültiges SSML → 400. Bei Retrofit/Gson leicht „beide gesetzt" durch non-null-Defaults.
- **Versionen:** v1, alle.
- **FIX:** Nur EIN Feld serialisieren (ungenutzte auf `null`, keine Leerstrings); SSML auf Wohlgeformtheit prüfen.
- **Quelle:** https://cloud.google.com/php/docs/reference/cloud-text-to-speech/latest/V1.SynthesisInput

### G9. Voice-Name-Schema & Region; Chirp3-HD vs. älteres Chirp-HD
- **Symptom:** 400 „voice does not exist"/leeres Ergebnis.
- **Ursache:** Schema `<locale>-Chirp3-HD-<Name>` (Ziffer 3, „HD" groß). Falle: älteres `Chirp-HD` (z. B. `en-US-Chirp-HD-D`) ohne Markup/Pause. Stimmen nicht in jeder Region.
- **Versionen:** v1, Chirp 3 HD.
- **FIX:** Exakt `de-DE-Chirp3-HD-Kore`; `languageCode` zum Präfix passend; Regionen global/us/eu/asia-southeast1/europe-west2/asia-northeast1; im Zweifel `global`; per `voices.list` validieren.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G10. custom_pronunciations (IPA/X-SAMPA) — Locale-Ausschlüsse, de-DE OK
- **Symptom:** In manchen Sprachen ignoriert/abgelehnt.
- **Ursache:** Ausgeschlossen u. a.: bg-bg, bn-in, cs-cz, da-dk, el-gr, et-ee, fi-fi, gu-in, he-il, hr-hr, hu-hu, lt-lt, lv-lv, nb-no, nl-be, pa-in, ro-ro, sk-sk, sl-si, sr-rs, sv-se, sw-ke, th-th, uk-ua, ur-in, vi-vn, yue-hk. **de-DE NICHT ausgeschlossen.**
- **Versionen:** v1, Preview.
- **FIX:** Für de-DE nutzbar; `phonetic_encoding` korrekt (`PHONETIC_ENCODING_IPA`/`_X_SAMPA`); mehrdeutige Phrasen eindeutig formatieren (`read1`/`[read]`); mit `markup` kombinierbar.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G11. LINEAR16 ist Default; Sample-Rate-Mismatch
- **Symptom:** Ohne `audioEncoding` kommt LINEAR16-WAV statt MP3; `sampleRateHertz` ≠ native Rate → Resampling/Artefakte.
- **Ursache:** Default LINEAR16; erzwungene Rate resampelt.
- **Versionen:** v1, alle.
- **FIX:** `audioEncoding` IMMER explizit `MP3`; `sampleRateHertz` für MP3 weglassen (native Rate); tatsächliches Format aus Bytes lesen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G12. `audioContent` ist Base64 → vor Abspielen dekodieren ⭐ HAEUFIG
- **Symptom:** HTTP 200, aber 0-Byte-/Müll-Audio.
- **Ursache:** REST-Antwort liefert `audioContent` **Base64** (anders als Client-Libs, die schon dekodieren). Roh weggeschrieben = Müll. Leer auch bei nur-ignorierten-Tags (G2).
- **Versionen:** v1 REST, alle.
- **FIX:** Erst Base64-dekodieren: Kotlin `Base64.decode(audioContent, Base64.DEFAULT)`, JS `atob`→`Uint8Array`; dann als MP3 schreiben/abspielen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### G13. Long-Audio-Synthesis (`synthesizeLongAudio`) mit Chirp 3 HD kaputt
- **Symptom:** Operation hängt bei 16,666 % (1/6), nur Teildateien, Timeout; `markup` → „Unknown field for SynthesisInput: markup".
- **Ursache:** Chirp 3 HD im Long-Audio-Pfad instabil/eingeschränkt; Markup dort nicht unterstützt.
- **Versionen:** v1, Stand 2025-07 bis 2026.
- **FIX:** Für > 5000 Bytes NICHT Long-Audio mit Chirp 3 HD. Client-seitig in <5000-Byte-Chunks splitten, je synchron als MP3 holen, zusammenfügen (passt zu eurem Setup).
- **Quelle:** https://discuss.google.dev/t/long-form-audio-does-not-work-with-chirp3-hd-voices/194335

### G14. Modellfamilien-Verwechslung (Chirp3-HD / Chirp-HD / Neural2 / Gemini-TTS)
- **Symptom:** Markup/Pause/SSML wirken nicht — falsche Modellfamilie adressiert.
- **Ursache:** Nur `Chirp3-HD` kann Markup/Pause/IPA-Pronunciations. Neural2/WaveNet = klassisches SSML, kein `[pause]`. Gemini-TTS = Prompt-Steuerung.
- **Versionen:** v1, modellübergreifend.
- **FIX:** Konsequent `Chirp3-HD` (mit „3"); Feature-Erwartungen je Familie trennen; per `voices.list` validieren.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types · https://docs.cloud.google.com/text-to-speech/docs/gemini-tts

---

## GA) Google — Authentifizierung, Quoten, Billing, Regionen

### GA1. API-Key in der URL (`?key=...`) → Leak ⭐ HAEUFIG
- **Symptom:** Key in Proxy-/Server-Logs, `adb logcat`, Crashlytics, Retrofit-`HttpLoggingInterceptor`, MV3-Network-Tab → Fremdnutzung/Kosten.
- **Ursache:** Query-Parameter werden überall mitgeloggt; API-Key authentifiziert keinen Principal.
- **Versionen:** TTS v1/v1beta1, alle API-Key-Clients.
- **FIX:** Key im Header `x-goog-api-key: <KEY>` statt URL; Retrofit-Logging in Prod `NONE`/`redactHeader`; Key restriktieren (GA2); pro Quelle eigener Key.
- **Quelle:** https://docs.cloud.google.com/docs/authentication/api-keys-best-practices

### GA2. Key-Restriktion erlaubt nur EINEN Client-Typ → getrennte Keys Android/Chrome
- **Symptom:** Android-Restriktion (Package+SHA-1) bricht den Chrome-Key (oder umgekehrt) → 403.
- **Ursache:** Pro Key nur `androidKeyRestrictions` ODER `browserKeyRestrictions`, nie beides.
- **Versionen:** API Keys v2.
- **FIX:** Eigener Key pro Quelle (Android-Key + Browser-Key); zusätzlich beide auf die TTS-API beschränken (API-Restriction).
- **Quelle:** https://docs.cloud.google.com/api-keys/docs/add-restrictions-api-keys

### GA3. Referrer-Restriktion bei MV3-Erweiterung nutzlos
- **Symptom:** Browser-Key mit HTTP-Referrer-Restriktion → 403 in der Extension.
- **Ursache:** MV3-Service-Worker senden keinen Website-Referrer; Origin ist `chrome-extension://<id>` (öffentlich, fälschbar).
- **Versionen:** Chrome MV3, API Keys v2.
- **FIX:** Key serverseitig (Backend-Proxy) ODER mind. API-Restriction (nur TTS) + harte Quota/Budget-Limits.
- **Quelle:** https://docs.cloud.google.com/docs/authentication/api-keys-best-practices · https://github.com/googleapis/google-cloud-java/issues/3400

### GA4. Service-Account-JSON in der App/Extension = großes Risiko
- **Symptom:** `service-account.json` aus APK/Bundle extrahierbar → voller, langlebiger Projektzugriff.
- **Ursache:** SA-Keys sind langlebige Credentials; auf dem Client per Definition exponiert.
- **Versionen:** alle.
- **FIX:** SA-Key NICHT auf dem Client; Token-Erzeugung ins Backend, Client bekommt kurzlebige Tokens/ruft Proxy. Für Nutzer-Auth: Google Sign-In/OIDC. Notfalls restriktierter API-Key (Schaden begrenzbar).
- **Quelle:** https://cloud.google.com/docs/authentication/production

### GA5. ADC funktioniert auf Android nicht (kein Metadata-Server/gcloud)
- **Symptom:** „Could not load the default credentials" auf dem Gerät.
- **Ursache:** ADC sucht Env-Var/gcloud/Metadata-Server — auf Android keins davon vorhanden.
- **Versionen:** alle Client-Libs auf Android.
- **FIX:** Nicht auf ADC bauen; API-Key (restriktiert) oder Backend-Tokens; Nutzer-Auth via Google Sign-In/OIDC.
- **Quelle:** https://docs.cloud.google.com/docs/authentication/application-default-credentials

### GA6. OAuth-Access-Token läuft nach ~1 h ab → 401
- **Symptom:** Nach ~60 Min plötzlich 401 „Invalid Credentials".
- **Ursache:** Google-Access-Tokens kurzlebig (~1 h); gecachter String ohne Refresh läuft ab.
- **Versionen:** alle OAuth/ADC-Clients.
- **FIX:** Credentials-Objekt (Client-Lib) erneuert automatisch — nicht den reinen String cachen; bei REST vor jedem Batch frisches Token; 401 → Refresh + EIN Retry. `gcloud auth print-access-token` nur für Tests.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/authentication

### GA7. OAuth/ADC ohne Quota-Project → 403 / falsches Billing
- **Symptom:** 403 „user project … needs to be specified" oder Verbrauch im falschen Projekt.
- **Ursache:** Bei API-Key bestimmt der Key das Projekt; bei OAuth/ADC muss das Quota-Project separat gesetzt sein.
- **Versionen:** alle OAuth/ADC-Clients.
- **FIX:** Header `x-goog-user-project: PROJECT_ID`; lokal `gcloud auth application-default set-quota-project`; Principal braucht `serviceusage.services.use`.
- **Quelle:** https://docs.cloud.google.com/docs/quotas/set-quota-project

### GA8. TTS-API nicht aktiviert → 403 SERVICE_DISABLED
- **Symptom:** 403 „… has not been used in project … or it is disabled".
- **Ursache:** `texttospeech.googleapis.com` im Projekt nicht aktiviert.
- **Versionen:** alle.
- **FIX:** API aktivieren (`gcloud services enable texttospeech.googleapis.com`), wenige Minuten Propagierung.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/before-you-begin

### GA9. Billing nicht aktiviert → 403 trotz Free-Tier
- **Symptom:** 403 „requires billing", obwohl unter 1 Mio Zeichen.
- **Ursache:** TTS verlangt aktives Billing auch im Gratis-Kontingent (Free-Tier = Rabatt, kein Ersatz).
- **Versionen:** alle.
- **FIX:** Abrechnungskonto verknüpfen; danach greift das Monatskontingent automatisch.
- **Quelle:** https://cloud.google.com/text-to-speech/pricing

### GA10. IAM-Rolle fehlt (nur OAuth/Service-Account)
- **Symptom:** 403 PERMISSION_DENIED trotz aktiver API + Billing.
- **Ursache:** Principal hat keine TTS-Rechte (API-Keys umgehen IAM).
- **Versionen:** OAuth/SA-Clients.
- **FIX:** Passende TTS-Rolle + Rechte auf dem Quota-Project geben.
- **Quelle:** https://cloud.google.com/text-to-speech/docs/authentication

### GA11. Chirp 3 HD: nur 200 Requests/Minute/Projekt ⭐ HAEUFIG
- **Symptom:** 429 RESOURCE_EXHAUSTED schon bei moderater Parallelität.
- **Ursache:** Dedizierte Quote `Chirp3RequestsPerMinutePerProject = 200` (vs. Standard 1000).
- **Versionen:** v1, Chirp 3 HD (01.06.2026).
- **FIX:** Client-seitig auf 200 RPM drosseln (Token-Bucket), cachen; Quote-Erhöhung anfordern; 429 → Backoff.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas

### GA12. Streaming: max. 100 gleichzeitige Sessions/Projekt
- **Symptom:** Ab 101. paralleler Session 429.
- **Ursache:** `ConcurrentStreamingSessionsPerProject = 100` (projektweit).
- **Versionen:** v1 Streaming.
- **FIX:** Aktive Sessions zählen/begrenzen, zügig schließen; ggf. Quote-Erhöhung.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas

### GA13. Quote gilt PRO PROJEKT, nicht pro Key/Gerät
- **Symptom:** Mehr Keys/Geräte, trotzdem 429.
- **Ursache:** Alle RPM-Limits „…PerProject"; Android-App + Extension teilen sich die Quote.
- **Versionen:** alle.
- **FIX:** Projektweit drosseln + gemeinsam cachen; echte Skalierung nur über Quote-Erhöhung.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas

### GA14. 429-Handling: Backoff statt sofortigem Re-Request
- **Symptom:** Sofortiges Wiederholen → Fehler-Schleife.
- **Ursache:** RESOURCE_EXHAUSTED nur mit Wartezeit retrybar; RPM-Werte „subject to change".
- **Versionen:** alle.
- **FIX:** Exponentielles Backoff + Jitter; Google-TTS-REST liefert nicht immer `Retry-After` → nicht davon abhängig machen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/quotas

### GA15. Free-Tier pro Modell-Tier — Chirp 3 HD 1 Mio frei, danach 30 $/Mio ⭐ HAEUFIG
- **Symptom:** „Überraschungsrechnung".
- **Ursache:** Gratis-Kontingent pro Modellklasse: Chirp 3 HD/Neural2 je 1 Mio/Monat, WaveNet/Standard je 4 Mio, Studio 1 Mio. Chirp 3 HD danach 0,00003 $/Zeichen = 30 $/Mio (~7,5× Neural2/WaveNet).
- **Versionen:** Preisstand 2026.
- **FIX:** Verbrauch pro Modell überwachen (Cloud Monitoring), Budget-Alerts; aggressiv cachen; Massentexte ggf. Neural2/WaveNet.
- **Quelle:** https://cloud.google.com/text-to-speech/pricing

### GA16. SSML-Tags zählen für die Abrechnung mit (außer `<mark>`)
- **Symptom:** Rechnung höher als sichtbare Textlänge.
- **Ursache:** Abgerechnet wird die gesamte Eingabe inkl. Whitespace und aller SSML-Tags; einzige Ausnahme `<mark>`.
- **Versionen:** alle (v1/v1beta1).
- **FIX:** SSML knapp halten; für reinen Vorlese-Text plain `text`; SSML-Zeichen in Kostenschätzung einrechnen.
- **Quelle:** https://cloud.google.com/text-to-speech/pricing

### GA17. EU-Endpoint + Chirp 3 HD = KEINE Datenresidenz (DSGVO-Falle)
- **Symptom:** Bewusst `eu`-Endpoint für DSGVO gewählt, aber Chirp 3 HD garantiert keine Residency.
- **Ursache:** Chirp 3 HD ist „out of scope for regionalization and data residency".
- **Versionen:** Chirp 3 HD, 2026-06-03.
- **FIX:** Bei echter Residency-Pflicht residency-pflichtige Inhalte mit Neural2 über EU-Endpoint; Chirp 3 HD nur für unkritische Inhalte; ggf. Org-Policy `constraints/gcp.restrictEndpointUsage`.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/endpoints

### GA18. Regionaler Endpoint ohne passendes `parent`/Location → 400/404
- **Symptom:** Host auf `eu-texttospeech.googleapis.com` getauscht, aber 400/404.
- **Ursache:** Location muss auch im `parent` stehen; nur Host tauschen reicht nicht.
- **Versionen:** alle regionalen Aufrufe.
- **FIX:** EU-Location im `parent` setzen; bei REST `x-goog-user-project` mitsenden.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/endpoints

### GA19. Single-Region-Endpoints bieten nur Neural2 — Chirp 3 HD nicht überall
- **Symptom:** Chirp 3 HD am Single-Region-Endpoint → voice-not-found/400.
- **Ursache:** Single-Region nur Neural2; Chirp 3 HD nur global/us/eu (Multi-Region).
- **Versionen:** 2026-06-03.
- **FIX:** Für Chirp 3 HD global/us/eu nutzen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/endpoints

### GA20. `languageCode` ≠ Voice-Präfix → 400
- **Symptom:** „voice not found"/„languageCode does not match the voice".
- **Ursache:** `languageCode` muss zum Locale-Präfix des Voice-Namens passen.
- **Versionen:** Chirp 3 HD, v1.
- **FIX:** `languageCode: "de-DE"` + `name: "de-DE-Chirp3-HD-<Stimme>"`; vor Hardcoding per `voices.list` prüfen.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd

### GA21. Stimmen-Verfügbarkeit endpoint-abhängig
- **Symptom:** Stimme am `global`-Endpoint da, am Regional-Endpoint „not found".
- **Ursache:** `voices.list` liefert je Endpoint andere Ergebnisse.
- **Versionen:** Chirp 3 HD.
- **FIX:** `voices.list` IMMER gegen denselben Endpoint wie die Synthese; Fallback auf `global` wenn regional fehlt.
- **Quelle:** https://docs.cloud.google.com/text-to-speech/docs/endpoints

---

## N) Android-native TextToSpeech (Offline-Fallback)

### N1. Init-Race: `speak()` vor `onInit()` → erste Äußerung verschluckt ⭐ HAEUFIG
- **Symptom:** `speak()` direkt nach Konstruktor tut nichts; erste Äußerung verloren, kein Fehler im Log.
- **Ursache:** Engine bindet asynchron; bis `onInit(SUCCESS)` ist sie nicht nutzbar (`speak()` gibt trotzdem SUCCESS zurück).
- **Versionen:** alle (API 26–35).
- **FIX:** Alle Aufrufe erst nach `onInit==SUCCESS`; `isReady`-Flag + Pending-Queue; kein fester Timer.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech.OnInitListener

### N2. `onInit` liefert `ERROR` / Engine fehlt
- **Symptom:** `onInit(ERROR)` oder Callback bleibt aus; nie Ton.
- **Ursache:** Keine Engine installiert/aktiviert; Default-Engine deaktiviert.
- **Versionen:** alle.
- **FIX:** `status` immer prüfen; ggf. explizite Engine `"com.google.android.tts"`; sonst Nutzer zu TTS-Einstellungen.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech.OnInitListener

### N3. `setLanguage` → `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` ⭐ HAEUFIG
- **Symptom:** Rückgabe -1/-2; nichts gesprochen.
- **Ursache:** Deutsche Sprachdaten fehlen (MISSING_DATA) oder Engine kann de_DE nicht (NOT_SUPPORTED).
- **Versionen:** alle.
- **FIX:** Return IMMER auswerten; bei MISSING_DATA `ACTION_INSTALL_TTS_DATA`, danach mit `ACTION_CHECK_TTS_DATA` (`CHECK_VOICE_DATA_PASS`) verifizieren; bei NOT_SUPPORTED Engine wechseln (N5).
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine

### N4. `isLanguageAvailable` trügt — keine konkreten Stimmdaten
- **Symptom:** „verfügbar" gemeldet, dennoch kein/hängendes `speak()`.
- **Ursache:** „grundsätzlich unterstützt" ≠ „Stimmdateien vorhanden".
- **Versionen:** alle.
- **FIX:** Mit `ACTION_CHECK_TTS_DATA` verifizieren; vor dem Sprechen prüfen, ob `getVoices()`/`getDefaultVoice()` eine konkrete de_DE-Voice liefert.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

### N5. Samsung/Bixby-Engine ab One UI 7 für Dritt-Apps gesperrt
- **Symptom:** Auf Samsung (One UI 7/Android 15+) `NOT_SUPPORTED` für Dritt-Apps trotz funktionierender System-Stimme.
- **Ursache:** Samsung beschränkt die eigene Engine auf Samsung-Apps.
- **Versionen:** Samsung One UI 7/Android 15+; Hersteller-Eigenheiten generell.
- **FIX:** Nicht auf Default-Engine verlassen; explizit `"com.google.android.tts"`; `getEngines()` prüfen; sonst Nutzer zu Einstellungen.
- **Quelle:** https://support.tecarta.com/hc/en-us/articles/41616767343387

### N6. `getDefaultEngine()` liefert null/falsch (issuetracker 300116092)
- **Symptom:** Engine-Auswahllogik greift daneben.
- **Ursache:** Plattform-Bug; offener Google-Issue.
- **Versionen:** mehrere neuere API-Level (offen).
- **FIX:** Auf null/leer prüfen; `getEngines()` nach `"com.google.android.tts"` durchsuchen; Abfragen erst nach `onInit`; sauber degradieren.
- **Quelle:** https://issuetracker.google.com/issues/300116092

### N7. `UtteranceProgressListener` feuert nicht (fehlende utteranceId)
- **Symptom:** `onStart/onDone/onError` nie aufgerufen; Folge-Logik blockiert.
- **Ursache:** Ohne utteranceId trackt das Framework nicht; Listener muss VOR `speak()` registriert sein.
- **Versionen:** alle; < API 21 besonders.
- **FIX:** `speak(text, QUEUE_FLUSH, params, "id")` mit eindeutiger ID; Listener vor erstem `speak()`; `onError(String,int)` überschreiben.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener

### N8. `synthesizeToFile` asynchron → Datei leer/fehlt
- **Symptom:** WAV leer/fehlt; Code liest zu früh.
- **Ursache:** Async (nur enqueued); ohne utteranceId kein `onDone`; ungültiger Pfad/Permission.
- **Versionen:** alle.
- **FIX:** utteranceId mitgeben, erst in `onDone` auf Datei zugreifen; neue Signatur ab API 21; `cacheDir`/`filesDir`. Output = WAV/PCM.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

### N9. `shutdown()` vergessen → ServiceConnection-Leak
- **Symptom:** „Activity has leaked ServiceConnection"; Speicher steigt.
- **Ursache:** Gebundener Service bleibt; mehrfaches `new` ohne shutdown verschärft.
- **Versionen:** alle.
- **FIX:** `tts.shutdown()` in `onDestroy()`; eine Instanz pro Scope; alte vor Neu-Instanziierung herunterfahren; `applicationContext` für langlebige Instanzen.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

### N10. Lange Texte > ~4000 Zeichen → stiller Fehlschlag
- **Symptom:** Nichts passiert, `speak()` gibt aber SUCCESS.
- **Ursache:** `getMaxSpeechInputLength()` (~4000) überschritten → still.
- **Versionen:** alle.
- **FIX:** Gegen `getMaxSpeechInputLength()` prüfen; an Satzgrenzen splitten; erstes `QUEUE_FLUSH`, Folgende `QUEUE_ADD`; je eigene utteranceId.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

### N11. Queue-Modus `QUEUE_ADD` vs. `QUEUE_FLUSH`
- **Symptom:** Neue Äußerung schneidet laufende ab oder Äußerungen stapeln sich.
- **Ursache:** FLUSH leert+unterbricht, ADD hängt an.
- **Versionen:** alle.
- **FIX:** Neues Sprechen FLUSH, Fortsetzungen ADD; „abbrechen+neu" = `stop()`+FLUSH.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech

### N12. Audio-Focus / falscher Stream
- **Symptom:** TTS überlagert Musik / wird stummgeschaltet / falsche Lautstärke.
- **Ursache:** Kein Audio-Focus; falscher Stream (nicht `USAGE_MEDIA`/`STREAM_MUSIC`).
- **Versionen:** alle.
- **FIX:** Audio-Focus anfordern (transient), im `onDone` freigeben; `setAudioAttributes(USAGE_MEDIA/CONTENT_TYPE_SPEECH)`.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### N13. Background/Doze ab Android 17 → keine Hintergrund-Wiedergabe
- **Symptom:** TTS schweigt sobald App im Hintergrund (neue Android-Versionen).
- **Ursache:** Android 17 erzwingt Restriktionen für Hintergrund-Audio (sichtbare Activity ODER FGS while-in-use, nicht SHORT_SERVICE); Doze/App-Standby.
- **Versionen:** Android 17+; Doze ab API 23.
- **FIX:** Hintergrund-TTS nur mit geeignetem Foreground-Service (mediaPlayback/while-in-use); sonst dokumentieren, dass Hintergrund nicht garantiert ist.
- **Quelle:** https://developer.android.com/about/versions/17/changes/bg-audio

### N14. „Offline"-Trugschluss: de_DE-Qualitätsstimmen brauchen Netz
- **Symptom:** Als Offline-Fallback gedacht, aber ohne Netz stumm/roboterhaft.
- **Ursache:** Google TTS hat Netz-Stimmen (HQ, Internet nötig) und lokale Stimmen (offline, schlechter); ohne explizite Wahl kann eine Netz-Stimme greifen.
- **Versionen:** alle (Voice-API ab API 21).
- **FIX:** Über `getVoices()` nur Voices mit `!isNetworkConnectionRequired()` + de_DE akzeptieren, `setVoice(...)`; de_DE-Daten vorab via `ACTION_CHECK_TTS_DATA` sichern.
- **Quelle:** https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine

---

## AC) Android Client-Integration (MediaPlayer, Coroutinen, Cache, Prefs, OkHttp)

### AC1. MediaPlayer `IllegalStateException` (State-Machine) ⭐ HAEUFIG
- **Symptom:** Crash bei `start()`/`prepare()`/`setDataSource()`.
- **Ursache:** `setDataSource` nur in Idle, `prepare` nur in Initialized, `start` ab Prepared; falsche Reihenfolge oder Wiederverwendung ohne `reset()`.
- **Versionen:** alle (API 26–35).
- **FIX:** `Idle → setDataSource → prepareAsync → [onPrepared] → start`; vor Wiederverwendung `reset()`; pro Absatz lieber frische Instanz.
- **Quelle:** https://developer.android.com/reference/android/media/MediaPlayer

### AC2. `prepare()` (blockierend) vs. `prepareAsync()`
- **Symptom:** `prepare()` auf Stream → ANR; `prepareAsync()` + sofort `start()` → kein Ton/IllegalState.
- **Ursache:** `prepare()` blockiert bis Ready; `prepareAsync()` meldet erst per `OnPreparedListener`.
- **Versionen:** alle.
- **FIX:** Lokale Cache-Datei: `prepare()` auf IO-Thread ok; Stream: `prepareAsync()` + `start()` nur im `setOnPreparedListener`.
- **Quelle:** https://developer.android.com/reference/android/media/MediaPlayer

### AC3. MediaPlayer `error(1, -38/-2147483648)` — unvollständige Datei
- **Symptom:** `OnErrorListener` feuert, danach Player tot.
- **Ursache:** Leerer/ungültiger Pfad, unvollständig geschriebene MP3 (Race), Format-/Permission-Problem.
- **Versionen:** alle.
- **FIX:** `setOnErrorListener` (return true); vor `setDataSource` Datei existiert + `length()>0` + Download fertig (AC9); bei Error `reset()` + Datei verwerfen.
- **Quelle:** https://blog.weston-fl.com/android-mediaplayer-prepare-throws-status0x1-error1-2147483648/

### AC4. Stale Callbacks / Geister-Audio nach Stop/release ⭐ HAEUFIG
- **Symptom:** Nach `stop()`/`release()` startet altes Audio; alter `OnCompletion` triggert nächsten Absatz doppelt.
- **Ursache:** Callbacks werden über den Looper gepostet und feuern nach `release()` noch.
- **Versionen:** alle.
- **FIX:** Vor `release()` alle Listener nullen (`setOnPreparedListener(null)` etc.) UND in jedem Callback die Sequenz-/Generation-ID prüfen (Mismatch → return). Beide Schichten — euer Sequenz-ID-Guard allein reicht nicht.
- **Quelle:** https://developer.android.com/reference/android/media/MediaPlayer

### AC5. Kein Audio-Focus → Ducking/Unterbrechung ignoriert
- **Symptom:** TTS spielt über Musik/Anruf; kein sauberer Resume.
- **Ursache:** Ohne `AudioFocusRequest` respektiert die App fremde Audio-Events nicht (ab API 26 strenger).
- **Versionen:** API 26+, verschärft 31+.
- **FIX:** `AudioFocusRequest` (`AUDIOFOCUS_GAIN_TRANSIENT`) + Listener; alle Loss-Fälle behandeln; Focus nur bei aktiver Wiedergabe.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### AC6. Ducking statt Pause bei Sprachinhalt
- **Symptom:** Fremder Ton duckt die TTS nur leiser → Wörter gehen unter.
- **Ursache:** Inhalt nicht als Sprache deklariert, `setWillPauseWhenDucked(true)` fehlt.
- **Versionen:** API 26+.
- **FIX:** `AudioAttributes` mit `CONTENT_TYPE_SPEECH`; `setWillPauseWhenDucked(true)`; auf `LOSS_TRANSIENT_CAN_DUCK` mit Pause reagieren.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### AC7. Coroutine-Cancellation bricht Synthese nicht ab ⭐ HAEUFIG
- **Symptom:** Stop, aber WS-Empfang/Retrofit-Call läuft weiter, schreibt Datei, spielt Geister-Audio.
- **Ursache:** `withContext` prüft Cancellation nur vor Block-Start; blockierende Nicht-Suspend-Calls laufen weiter (kooperativ).
- **Versionen:** kotlinx.coroutines alle.
- **FIX:** WS/MediaPlayer mit `suspendCancellableCoroutine` umhüllen, in `invokeOnCancellation` Ressource schließen (`webSocket.cancel()`, `mediaPlayer.release()`, Stream schließen); in langen Schleifen `ensureActive()`; Synthese-Job per `job.cancel()` killen.
- **Quelle:** https://developer.android.com/kotlin/coroutines · https://kt.academy/article/cc-cancellation

### AC8. Cleanup nach Cancel übersprungen
- **Symptom:** `JobCancellationException`/Cleanup (Datei löschen, release) läuft nicht.
- **Ursache:** `withContext(Main)` nach Cancel wirft sofort; Cleanup im Body wird bei Cancel nicht mehr ausgeführt.
- **Versionen:** kotlinx.coroutines alle.
- **FIX:** Cleanup in `withContext(NonCancellable)` (nur Cleanup); UI an `viewModelScope`/`repeatOnLifecycle` binden; `finally` für Ressourcenfreigabe.
- **Quelle:** https://kotlinlang.org/docs/cancellation-and-timeouts.html

### AC9. Race: MediaPlayer liest unvollständig geschriebene MP3 ⭐ HAEUFIG
- **Symptom:** Sporadisch `error(1,-38)`, abgehackte Wiedergabe/Stille, v. a. auf langsamen Geräten.
- **Ursache:** `setDataSource(file)` während Download/Schreiben noch läuft (Read-while-Write).
- **Versionen:** alle.
- **FIX:** Atomar: in `file.part` schreiben, `close`, dann `renameTo(finalFile)`; Player erst auf finale Datei zeigen, wenn Download-Job `await`-fertig.
- **Quelle:** https://proandroiddev.com/understanding-mutex-in-android-preventing-race-conditions-06fae3506614

### AC10. Gleicher Dateiname für parallele Absätze → Überschreiben
- **Symptom:** Vorab-Absatz N+1 überschreibt spielende Datei von N.
- **Ursache:** Fester Cache-Name (`tts_current.mp3`) für parallele Coroutinen.
- **Versionen:** alle.
- **FIX:** Eindeutiger Name pro Absatz (Sequenz-ID + Text-Hash + Voice/Provider/Format); Schreibzugriffe pro Datei via `Mutex`; löschen erst wenn zugehöriger Player released (an Sequenz-ID koppeln).
- **Quelle:** https://www.droidcon.com/2024/09/20/understanding-mutex-in-android-preventing-race-conditions/

### AC11. cacheDir: unbegrenztes Wachstum / vom System gelöscht
- **Symptom:** Cache wächst unbegrenzt; ODER Wiedergabe scheitert, weil System `cacheDir` unter Druck geleert hat.
- **Ursache:** `cacheDir` kann jederzeit (teilweise) geleert werden; ohne eigenen LRU wächst er.
- **Versionen:** alle.
- **FIX:** Eigene LRU-Begrenzung (DiskLruCache/Größenkappung); vor `setDataSource` Existenz+Größe prüfen, sonst neu synthetisieren; `.part`-Reste beim Start aufräumen.
- **Quelle:** https://developer.android.com/reference/android/media/MediaPlayer

### AC12. Cache-Key unvollständig → falsche Datei ⭐ HAEUFIG
- **Symptom:** Falscher Inhalt/alte Stimme trotz Wechsel.
- **Ursache:** Key nur aus Text-Hash, ohne Voice/Provider/Sprache/Tempo/Format.
- **Versionen:** alle.
- **FIX:** `hash(text + provider + voice + lang + rate + pitch + format)` — bei Parameterwechsel automatisch neuer Key.
- **Quelle:** Schlussfolgerung aus DiskLruCache-Key-Praxis.

### AC13. EncryptedSharedPreferences `AEADBadTagException`-Crash
- **Symptom:** Crash in `Application.onCreate` beim Öffnen der Prefs (`AEADBadTagException`/`KeyStoreException`), gerätespezifisch.
- **Ursache:** Keystore inkonsistent (nach Backup/Restore/Migration/Korruption); Master-Key unbrauchbar → Entschlüsselung scheitert.
- **Versionen:** androidx.security:security-crypto 1.0.0–1.1.0-alpha0x (Tink).
- **FIX:** Init in try/catch; im Catch korrupte Prefs-Datei + Keystore-Key löschen + neu anlegen (graceful recovery); danach Nutzer TTS-Key neu eingeben lassen statt Crash-Loop; Keystore nie auf Main-Thread.
- **Quelle:** https://github.com/google/tink/issues/535

### AC14. EncryptedSharedPreferences ist deprecated (Wartung 2024 eingestellt)
- **Symptom:** Deprecation-Warnung; keine Bugfixes mehr.
- **Ursache:** Jetpack-Security-Crypto-Team hat Wartung eingestellt; ab 1.1.0-alpha06 deprecated.
- **Versionen:** ab 1.1.0-alpha06.
- **FIX:** Für Neues nicht mehr neu darauf setzen — DataStore + Tink/Keystore (oder Ackee Guardian). Falls genutzt: Version pinnen, AC13-Recovery zwingend, Migration planen.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/security

### AC15. MediaPlayer-/Context-Leak (Lifecycle)
- **Symptom:** Speicher steigt über Sessions; LeakCanary meldet geleakte Activity; Audio spielt nach Wechsel weiter.
- **Ursache:** `release()` in `onStop()`/`onDestroy()` vergessen; MediaPlayer/Context in ViewModel.
- **Versionen:** alle.
- **FIX:** Player an Lifecycle koppeln (`onStop()` → pause/release, null); für Wiedergabe über Activity-Grenzen Foreground-Service + MediaSession; `applicationContext`.
- **Quelle:** https://symphony-solutions.com/insights/fixing-memory-leaks-in-android

### AC16. Wiedergabe/Synthese überlebt Process-Death nicht
- **Symptom:** Nach Process-Death Position weg; `.part`-Müll bleibt.
- **Ursache:** In-Memory-State (Absatz-Index/Sequenz-ID) verworfen; Coroutinen sterben ohne `finally`-Cleanup.
- **Versionen:** alle.
- **FIX:** Fortschritt in DataStore/`SavedStateHandle`; `.part`-Reste beim Start aufräumen; aktive Wiedergabe als FGS.
- **Quelle:** https://developer.android.com/kotlin/coroutines

### AC17. OkHttp-WebSocket `MAX_QUEUE_SIZE`-Overflow (16 MiB) → Close 1001
- **Symptom:** Bei langen Texten/vielen schnellen Sends schließt der WS (1001), Synthese bricht ab.
- **Ursache:** `send()` puffert; > 16 MiB ausgehend → OkHttp verwirft + fährt graceful runter; keine automatische Backpressure.
- **Versionen:** OkHttp 3.x–5.x.
- **FIX:** Pro WS-Session möglichst ein Request, Audio abwarten, dann nächster; Sende-Rate serialisieren (Mutex/Channel); großen Text häppchenweise.
- **Quelle:** https://github.com/square/okhttp/issues/8123

### AC18. OkHttp-WS `onFailure` ohne Reconnect/Timeout → hängende Verbindung
- **Symptom:** Nach Netzwechsel/Timeout bleibt TTS hängen, kein Audio.
- **Ursache:** `onFailure` ist der einzige Fehlerkanal; ohne Timeout/Reconnect überlebt die Session keine transienten Probleme.
- **Versionen:** OkHttp alle.
- **FIX:** In `onFailure` loggen + Ressourcen frei + Backoff-Reconnect (begrenzt); `connectTimeout`/`readTimeout`/`pingInterval` setzen; beim Stop WS `cancel()`.
- **Quelle:** https://square.github.io/okhttp/3.x/okhttp/okhttp3/WebSocket.html

### AC19. `NetworkOnMainThreadException` / UI-Update vom IO-Thread
- **Symptom:** Crash `NetworkOnMainThreadException` oder „Only the original thread … can touch its views".
- **Ursache:** Synchroner Netz-Call auf Main; bzw. View-Mutation aus `Dispatchers.IO`.
- **Versionen:** alle.
- **FIX:** Netz/I-O in `Dispatchers.IO` bzw. Retrofit-`suspend`; UI-Updates in `Dispatchers.Main`/StateFlow + `repeatOnLifecycle`.
- **Quelle:** https://developer.android.com/kotlin/coroutines

---

## W) Chrome MV3 (`vorlese-overlay-v2`)

### W1. Service-Worker stirbt nach ~30 s Idle → Audio/WS reißt ab ⭐ HAEUFIG
- **Symptom:** Wiedergabe stoppt mitten im Satz; WS schließt; Extension reagiert nicht mehr.
- **Ursache:** Chrome beendet MV3-Service-Worker nach 30 s ohne Event; Audio/WS allein im SW stirbt mit.
- **Versionen:** alle MV3; WS-Keepalive erst ab Chrome 116.
- **FIX:** Audio-Wiedergabe NIE im SW, sondern im **Offscreen-Document** (eigene Lebensdauer); WS am Leben halten (W3); Audio läuft im Offscreen-Doc weiter.
- **Quelle:** https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle

### W2. Offscreen-Doc mit `AUDIO_PLAYBACK` schließt nach 30 s ohne Audio
- **Symptom:** In Lese-Pausen verschwindet das Offscreen-Doc; nächster Befehl läuft ins Leere.
- **Ursache:** Reason `AUDIO_PLAYBACK` schließt das Doc nach 30 s ohne abspielendes Audio (dokumentiert).
- **Versionen:** Chrome 109+.
- **FIX:** Vor jeder Wiedergabe per `runtime.getContexts({contextTypes:['OFFSCREEN_DOCUMENT']})` Existenz prüfen + bei Bedarf neu erzeugen (Re-Create-on-demand); Settings/Cache nicht im Offscreen-Doc halten.
- **Quelle:** https://developer.chrome.com/docs/extensions/reference/api/offscreen

### W3. WS hält den SW erst ab Chrome 116 am Leben — Keepalive nötig
- **Symptom:** Pre-116/ohne Keepalive: SW schläft trotz offener WS nach 30 s, WS bricht.
- **Ursache:** Erst ab Chrome 116 verlängern aktive WS-Nachrichten die SW-Lebensdauer (30-s-Fenster).
- **Versionen:** < 116 problematisch; ab 116 ohne Keepalive.
- **FIX:** `"minimum_chrome_version": "116"` + alle ~20 s Keepalive-Nachricht über den WS; kritisch ist die datenfluss-freie Phase.
- **Quelle:** https://developer.chrome.com/docs/extensions/how-to/web-platform/websockets

### W4. `createDocument`-Race → „single offscreen document"
- **Symptom:** Bei schnellem Doppel-Trigger wirft `createDocument()`.
- **Ursache:** Zwei gleichzeitige Aufrufe; nur EIN Offscreen-Doc erlaubt.
- **Versionen:** alle MV3 mit Offscreen-API.
- **FIX:** Globales `creating`-Promise als Mutex + `getContexts()`-Check; auf `createDocument`-Promise warten, bevor Nachricht ans Doc.
- **Quelle:** https://developer.chrome.com/docs/extensions/reference/api/offscreen

### W5. AudioContext startet „suspended" — kein Ton ohne User-Geste
- **Symptom:** Wiedergabe „läuft", aber kein Ton; `state === "suspended"`.
- **Ursache:** Autoplay-Policy; Offscreen-Doc hat keine direkte User-Geste (Klick im Sidepanel/Overlay).
- **Versionen:** alle Chrome.
- **FIX:** User-Geste im Sidepanel/Overlay einfangen, als Auslöser nutzen; im Offscreen-Doc vor Wiedergabe `await audioContext.resume()`; alternativ `<audio>`-Element für reine MP3-Wiedergabe.
- **Quelle:** https://developer.chrome.com/blog/web-audio-autoplay

### W6. `decodeAudioData` „EncodingError"
- **Symptom:** MP3-Bytes nicht abspielbar; reject mit EncodingError.
- **Ursache:** Teil-/abgeschnittene MP3 (WS-Stream unvollständig), korrupter Uint8Array, oder **detachter** ArrayBuffer nach `postMessage`.
- **Versionen:** alle.
- **FIX:** Erst nach VOLLSTÄNDIGEM Empfang aller Chunks dekodieren (bis `turn.end`); ArrayBuffer korrekt transferieren oder als Blob/ObjectURL übergeben; für reine MP3-Wiedergabe `<audio src=blobURL>`.
- **Quelle:** https://developer.mozilla.org/en-US/docs/Web/API/BaseAudioContext/decodeAudioData

### W7. `createObjectURL`-Leak — Object-URLs nie revoked
- **Symptom:** Speicher wächst pro Absatz; Wiedergabe wird träge.
- **Ursache:** Jeder Blob bekommt eine Object-URL, die nicht freigegeben wird.
- **Versionen:** alle.
- **FIX:** Nach `audio.onended`/erfolgreichem decode `URL.revokeObjectURL(url)`; bei Offscreen-Doc Reason `BLOBS` deklarieren.
- **Quelle:** https://developer.chrome.com/docs/extensions/reference/api/offscreen

### W8. declarativeNetRequest greift NICHT bei WS-aus-Service-Worker (crbug 1285664) ⭐ HAEUFIG/KRITISCH
- **Symptom:** Die `Edg/143`-UA-Regel hat KEINE Wirkung auf den Edge-WS-Handshake; Server bekommt den Chrome-Standard-UA. Kein Fehler — Regel scheint zu wirken.
- **Ursache:** Chromium-Bug: weder `declarativeNetRequest` noch `webRequest` werden auf WS-Upgrade-Requests aus einem Service-Worker angewendet; diese Requests sind in normalen DevTools unsichtbar (nur `chrome://inspect/#workers`).
- **Versionen:** alle MV3 (offen).
- **FIX:** **WS NICHT im Service-Worker öffnen, sondern im Offscreen-Document** — aus Dokument-Kontext greift DNR auf den WS-Upgrade. Falls WS im SW bleiben muss: Edge-Auth läuft primär über die URL-Query-Parameter (Sec-MS-GEC), nicht den UA-Header (W9).
- **Quelle:** https://bugs.chromium.org/p/chromium/issues/detail?id=1285664

### W9. Browser-WS kann keine eigenen Header setzen (UA/Origin/Cookie)
- **Symptom:** `new WebSocket(...)` ignoriert Header-Versuche.
- **Ursache:** Web-`WebSocket`-API kennt keinen Header-Parameter; Origin = Extension-Origin, UA = Chrome-UA. Einzige Modifikation via DNR — die bei WS-aus-SW scheitert (W8).
- **Versionen:** alle.
- **FIX:** Header-Auth vermeiden; Edge-TTS über URL-Query-Parameter (`TrustedClientToken` + `Sec-MS-GEC`); UA-Override nur wo DNR greift (WS aus Offscreen-Doc).
- **Quelle:** https://developer.chrome.com/docs/extensions/how-to/web-platform/websockets · https://github.com/w3c/webextensions/issues/398

### W10. Mythos: `Edg/143`-UA muss exakt zur `Sec-MS-GEC-Version` passen
- **Symptom:** Annahme erzwingt fragile Versionspflege.
- **Ursache:** Bing-Server validiert die `Sec-MS-GEC-Version` aktuell NICHT streng (sogar `1-1.1.1.1` ging). Entscheidend ist der `Sec-MS-GEC`-Hash, nicht die exakte UA-/Versions-Übereinstimmung.
- **Versionen:** Stand 06/2026 tolerant; kann sich ändern.
- **FIX:** Energie in den Token-Algorithmus (W-Bezug E1), nicht in UA-Versionspflege; `Sec-MS-GEC-Version` als robustes, nicht versionssensitives Feld behandeln.
- **Quelle:** https://github.com/rany2/edge-tts/issues/290

### W11. Google-API-Key im Extension-Bundle sichtbar
- **Symptom:** Key im entpackten CRX im Klartext lesbar → Quota-Diebstahl.
- **Ursache:** MV3 hat keinen geheimen Serverraum; alles im Bundle ist öffentlich; Code-Aufteilung hilft nicht.
- **Versionen:** alle MV3.
- **FIX:** Sauber: Backend-Proxy hält den Key. Wenn Key im Client: API-Restriction (nur TTS) + Quota/Budget-Limits; Key vom Nutzer in `chrome.storage` eintragen lassen statt einbacken. Referrer-Restriktion hilft NICHT (W13).
- **Quelle:** https://github.com/GoogleChrome/developer.chrome.com/blob/main/site/en/docs/extensions/mv3/security/index.md

### W12. CSP `connect-src`/`host_permissions` fehlen → Fetch/WS blockiert
- **Symptom:** `fetch` zu googleapis bzw. WSS zu bing blockiert.
- **Ursache:** Strikte MV3-CSP; externe Verbindungen brauchen `host_permissions` und korrektes `connect-src`.
- **Versionen:** alle MV3.
- **FIX:** `host_permissions`: `https://texttospeech.googleapis.com/*` + `wss://speech.platform.bing.com/*`; `connect-src` der extension_pages entsprechend.
- **Quelle:** https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest

### W13. API-Key-Referrer-Restriktion schützt den TTS-REST-Call nicht
- **Symptom:** Referrer-gebundener Key wirkt nicht/inkonsistent (403 oder wirkungslos).
- **Ursache:** Referrer-Restriktion ist für Browser-JS-APIs (Maps) gedacht; TTS-REST wertet Referrer nicht als Sicherheitsgrenze; Extension-Origin ungeeignet.
- **Versionen:** alle.
- **FIX:** Statt Referrer: API-Restriction (nur TTS) + harte Quota/Budget; Key via `X-Goog-Api-Key`-Header; echte Sicherheit nur per Backend-Proxy.
- **Quelle:** https://github.com/googleapis/google-cloud-java/issues/3400

### W14. `chrome.storage.sync`-Quota durch Stimmen-Cache gesprengt
- **Symptom:** Speichern des Stimmen-Katalogs → Quota-Fehler; Settings verloren.
- **Ursache:** `sync` ~100 KB gesamt / ~8 KB pro Item / begrenzte Schreibrate.
- **Versionen:** alle.
- **FIX:** Großen/volatilen Cache (Stimmenliste, MP3) in `chrome.storage.local`; nur kleine Präferenzen in `sync`; Schreibvorgänge bündeln; Stimmen-Katalog mit TTL.
- **Quelle:** https://developer.chrome.com/docs/extensions/reference/api/storage

---

## C) Querschnitt — Retry/Fallback

### C1. Endlose Retries / Kosten bei permanenten Fehlern ⭐ HAEUFIG
- **Symptom:** App wiederholt 4xx/INVALID_ARGUMENT/Auth-Fehler; Kosten/Quota steigen, Edge-403 stumpf wiederholt.
- **Ursache:** Retry trennt nicht transient (429/5xx/Netz/`NoAudioReceived`) von permanent (400/401/403-Auth/Längen-Limit).
- **Versionen:** eigener Code.
- **FIX:** Nur transient retryen, Backoff + Jitter, `Retry-After` wenn vorhanden; Edge-403 zuerst Token/Uhr (E1/E2); permanent → sofort Fallback (anderer Provider / native TTS).
- **Quelle:** Best-Practices §9 + `apis/api-integration-general.md`.

### C2. Fallback-Kette unklar / stilles Mischen
- **Symptom:** Bei Cloud-Ausfall keine definierte Reaktion oder unerwartete Stimme.
- **Ursache:** Keine klare Reihenfolge gewählter Provider → Retry → Offline.
- **Versionen:** eigener Code.
- **FIX:** Definierte Kette: gewählter Provider → 1× Retry → (optional anderer Cloud-Provider) → Android-native → klare UI-Meldung; jede Stufe sichtbar loggen.
- **Quelle:** Best-Practices §9, §11.

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-tts-provider.md`) |
|---------------|-----------------------------------------------------------|
| E1–E9 (Edge Auth/Verbindung) | §1, §3 (Edge Endpunkt/Auth/Streaming) |
| ET1–ET11 (Edge Text/SSML/Audio) | §3, §5, §6 (Stimmen/SSML/Latenz/Text-Aufbereitung) |
| G1–G14 (Chirp Request/Format) | §2, §4 (Chirp Endpunkt/Format/SSML/Controls/Limit) |
| GA1–GA21 (Google Auth/Quota/Region) | §2, §4, §9, §10 (Auth/Limits/Retry/Secrets) |
| N1–N14 (Android-native) | §7 (Offline-Fallback) |
| AC1–AC19 (Android-Client) | §5, §8, §9, §11 (Latenz-Pipeline/Caching/Retry/Architektur) |
| W1–W14 (Chrome MV3) | §3, §5, §10, §11 (Edge im Browser/Latenz/Secrets/Architektur) |
| C1–C2 (Querschnitt) | §9 (Retry/Fallback-Kette) |
