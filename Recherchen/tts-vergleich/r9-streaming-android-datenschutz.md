# R9 – Streaming, Antwortbeginn, Android-Integration, Datenschutz, Zuverlässigkeit (anbieterübergreifend)

Researcher R9, Engine C (Sonnet-Schwarm). Recherchezeitpunkt und Abrufdatum aller Quellen, sofern nicht anders vermerkt: **21.10.2026**.
Methode: WebSearch/WebFetch plus Lesen lokaler Dateien (Jarvis-Quelltext, Berichte R1 bis R7 dieses Ordners, Quelltext PerfectMoment/VoiceAgent/GenialerWecker nur per Grep). Keine Schlüssel gelesen, keine Testaufrufe gegen APIs, nichts im Jarvis-Quelltext geändert.

## 0. Quellenlage und Vorbehalte (zuerst lesen)

Markierungen wie in R1: **belegt** = offizielle Quelle direkt abgerufen; **belegt (Auszug)** = offizielle Seite nur als Suchauszug; **extern** = Drittquelle (Forum, Blog, Issue); **abgeleitet** = eigene Schlussfolgerung aus belegten Fakten; **unbelegt** = nichts Belastbares gefunden.

- WebFetch fasst Seiten durch ein kleines Modell zusammen. Wo unten „wörtlich“ steht, sind es kurze Zitate (höchstens ca. 125 Zeichen je Zitat); längere Passagen sind sinngemäß wiedergegeben.
- **Zeit bis zum ersten Ton (TTFA) ist für die Google-Kandidaten (Chirp 3 HD, Gemini-TTS) nicht belastbar belegt.** Vier Suchrunden (Coval, Artificial Analysis, OpenRouter, Blogs) ergaben nur Herstellerprosa ohne Zahl, eine Zahl ohne Quelle und Foren-Einzelberichte. Das ist das Ergebnis, kein Recherchemangel. Details in Abschnitt 1.2.
- Die Seiten `cloud.google.com/terms/*` ließen sich per WebFetch nicht vollständig lesen (Seitenlänge). Aussagen zu DPA-Geltung für Cloud TTS sind deshalb als **unbelegt** markiert, soweit sie nicht aus R1 stammen.
- Die Android-Seite zu `AudioTrack` lieferte beim Abruf keinen Seiteninhalt; Aussagen zu `AudioTrack` stützen sich deshalb **nicht** auf eine abgerufene Seite, sondern auf allgemein bekanntes Verhalten (als **abgeleitet** markiert).

---

## 1. Streaming-Art, Ausgabeformate, Antwortbeginn je Kandidat

### 1.1 Überblick

| Kandidat | Streaming-Art | Ausgabeformate (Streaming / unary) | Antwortbeginn: Hersteller | Antwortbeginn: unabhängig | Status |
|---|---|---|---|---|---|
| **Google Chirp 3 HD** (Cloud TTS) | **gRPC**, bidirektional (`StreamingSynthesize`); in der REST-Referenz v1 fehlt die Methode | Streaming: PCM, ALAW, MULAW, OGG_OPUS; Antwort laut RPC-Referenz headerloses LINEAR16 24 kHz. Unary (REST): u. a. MP3, OGG_OPUS, LINEAR16 (R1) | nur qualitativ („you can start synthesizing speech before the complete input text is sent“), **keine Zahl** | **keine** gefunden | Streaming **Preview** (Pre-GA-Bedingungen); Synthese unary GA |
| **Gemini-TTS in Cloud TTS** (2.5 Flash/Pro, 3.1 Flash Preview) | **gRPC**, „multiple request multiple response“; Synthese beginnt erst nach Half-Close des Clients (also kein inkrementelles Textstreaming) | Streaming: PCM (Standard), ALAW, MULAW, OGG_OPUS. Unary: LINEAR16, ALAW, MULAW, **MP3**, OGG_OPUS, PCM | „delivered with very low latency“, **keine Zahl** | **keine** | 2.5 Flash/Pro ohne Preview-Vermerk (GA seit 30.09.2025 laut R2); 3.1 Preview; **3.8 nicht in Cloud TTS** |
| **Gemini API** (`generativelanguage`, 3.8 Flash/Flash-Lite TTS, 3.1 Preview) | **HTTP/SSE**: Interactions API (`POST /v1beta/interactions`, `stream:true`, Events `step.delta`, Typ `audio`, Base64) bzw. `streamGenerateContent` (3.1: Streaming-Eintrag im Changelog vom 17.06.2026) | Streaming: **rohes PCM** `audio/l16`, 24 kHz, mono, 16 Bit, ohne Header; wählbar `audio/mulaw`, `audio/alaw`, `sample_rate` 24000/16000/8000. Unary: **WAV mit RIFF-Header** (24 kHz, mono, 16 Bit). **Kein MP3, kein Opus** | keine Zahl | siehe 1.2: **nicht belegt**; nur Foren-Einzelwerte | 3.8: Entwickler-API „GA“ laut Changelog 22.09.2026, die Vertex/Agent-Platform-Seite nennt dasselbe Modell „Preview“ (R2) |
| **Azure AI Speech / Edge** | SDK-Streaming (`Synthesizing`-Event, Push/Pull-Stream); REST: HTTP-Antwort kommt in Stücken; Texteingabe-Stream nur über WebSocket V2, **nicht für Java/Android** (R3 §4.1). Edge: inoffizieller WebSocket (so nutzt es Jarvis) | MP3 24/48 kHz, Opus (ogg/webm), PCM raw 24/48 kHz, TrueSilk (R3 §4.2); Edge: MP3 24 kHz 48 kbit/s | Azure-HD „Less than 300 ms“ (Herstellerseite, ohne Messmethode, R3 §4.1) | keine eigene Messung gefunden | Azure Neural/HD GA; MAI-Voice Preview ohne SLA; Edge **inoffiziell** |
| **ElevenLabs** | REST-Stream; WebSocket `stream-input` (nicht für `eleven_v3`/`v4`) | Streaming-fähig u. a. MP3, PCM, Opus (nicht in dieser Sitzung neu geprüft; R4 §3.5) | Flash über WebSocket TTFB 100 bis 150 ms; v4 Turbo „~150 ms bis zur ersten Sprache“ (ohne Netzwerk) | **Coval 23.07.2026: Flash v2.5 Median TTFA 202 ms** (Messung vom Coval-Server, Standort unbekannt) | GA |
| **Cartesia** | WebSocket mit Kontexten, `continue`/`flush`/`cancel` (R4 §4.5) | PCM/MP3-Varianten (R4, nicht neu geprüft) | „sub-90ms“ (Marketing) | Coval/Gradium (Mai/Sept. 2026): Sonic 3.5 Roundtrip 151 ms, davon 134 ms Anfangsstille (Gradium-Beitrag, **Hersteller-Mitautor**) | GA |
| **Inworld** | HTTP-Stream, WebSocket, OpenAI-kompatibler Endpunkt mit SSE (seit 11.09.2026, R4 §5.5) | MP3, OGG_OPUS, LINEAR16, PCM, WAV | P90 TTFB 100 ms (TTS-2), 20 ms (Flash), **serverseitig** | **Coval 23.07.2026: TTS-2 Median TTFA 196 ms** | GA |
| **Mistral Voxtral TTS 3** | Streaming ja; **Transport auf der Modellseite nicht genannt** | „5 output formats“, nicht aufgelistet; ältere Doku: PCM `float32` für Streaming empfohlen (R5 §5) | „~90 ms time-to-first-audio“; ältere Doku: ca. 0,8 s Ende-zu-Ende (PCM), ca. 3 s (MP3) | **keine** | **Public Preview seit 20.10.2026** |
| **Qwen3-TTS** (Alibaba) | `qwen3-tts-flash`: HTTP mit SSE-Option (`X-DashScope-SSE: enable`, Base64-Stücke); `qwen3-tts-flash-realtime`: **WebSocket** (R6 §1) | PCM-Stücke bzw. am Ende URL zu WAV (so nutzt es Jarvis heute) | `qwen-audio-3.1-tts-flash` ca. 300 ms (Hersteller, R6) | **Coval: Realtime Median 720 ms** (Testrechner USA, Endpunkt in Asien; R6 §1.2) | `qwen3-tts-flash` GA |
| **Lokales Modell auf PC-Server** | selbst bestimmbar (Chunked HTTP/WebSocket) | selbst bestimmbar | Kyutai Pocket TTS „~200ms to get the first audio chunk“ (Hersteller, MacBook M4; R6) | keine | je Modell (R7) |

Quellen: Chirp/Streaming: https://docs.cloud.google.com/text-to-speech/docs/create-audio-text-streaming und https://docs.cloud.google.com/text-to-speech/docs/reference/rpc/google.cloud.texttospeech.v1 (belegt; Fußnote: Die RPC-Seite führt `audio_content` als „LINEAR16, 24000 Hz“, die Konfiguration erlaubt zusätzlich ALAW/MULAW/OGG_OPUS; ob die Antwort dann in diesem Format kommt, ist unbelegt). Gemini-TTS in Cloud TTS: https://docs.cloud.google.com/text-to-speech/docs/gemini-tts (belegt). Gemini API: https://ai.google.dev/gemini-api/docs/speech-generation, https://ai.google.dev/gemini-api/docs/changelog (belegt). ElevenLabs/Cartesia/Inworld/Azure/Mistral/Qwen/lokal: Berichte R3, R4, R5, R6, R7 dieses Ordners (jeweils mit dortigen Primärquellen); Coval-Werte: https://www.coval.ai/blog/tts-latency-benchmark-2026/ (Stand 23.07.2026) und https://gradium.ai/blog/coval-perceived-ttfa-benchmark (Stand 09.09.2026). Alle abgerufen 21.10.2026.

### 1.2 Zeit bis zum ersten Ton bei Gemini-TTS: was belegt ist und was nicht

| Aussage | Herkunft | Einstufung |
|---|---|---|
| Google nennt für Chirp 3 HD und Gemini-TTS **keine** TTFA-Zahl; Flash-Lite wird als „low latency“ beschrieben | Modellseiten, Cloud-Seite (21.10.2026) | belegt |
| „~142 ms“ (Flash TTS) und „85 ms“ (Flash-Lite) für den ersten Chunk | aiintoai.vercel.app: **ohne Fußnote, ohne Messmethode, Spaltenüberschrift „Time-to-First-Chunk (TTFB)“**; die Codebeispiele desselben Artikels nutzen `generateContent` ohne Streaming | **unbelegt (Quelle fehlt)** |
| „300 bis 500 ms“ für 3.1 Flash TTS | Segmind (Drittanbieter-Vermarktung) | unbelegt (Marketing) |
| 3.1 Preview, Streaming: 10 bis 20 s bis zum ersten Chunk mit eigenem Schlüssel (Tier 2 Prepaid), unter 2 s mit dem Standardschlüssel von AI Studio; „eine lange stille Wartezeit, danach mehrere Chunks innerhalb von Millisekunden“ (25.07.2026); am 26.07. unter 2 s; am 06.08. wieder „unusable latencies“; die Interactions API sei „significantly slower“ als der generate-content-Stream | Forum-Einzelberichte eines Entwicklers (Autor Rob_Wheatley), keine Google-Antwort: https://discuss.ai.google.dev/t/3-1-flash-tts-preview-streaming-latency/176050 | **extern**, Einzelfall, aber nach dem Autor Hinweis, dass das Streaming serverseitig erst nach vollständiger Erzeugung ausliefert (Vermutung des Autors) |
| Nicht-Streaming, 3.1 Preview: rund 136 s Audio brauchen etwa 77 s Wartezeit | Forum (Thread 169063, Autor Hendrik2) | extern |
| Durchsatz Gemini 3.8 Flash TTS: etwa 55,8 Zeichen je Sekunde Erzeugungszeit, Chirp 3 HD etwa 2,9 s für den 356-Zeichen-Testtext | Artificial Analysis, über R2 §5 (21.10.2026); keine TTFA | belegt (Auszug), **Durchsatz, nicht TTFA** |
| Coval und Artificial Analysis führen **keine** Gemini-TTS- und keine Chirp-TTFA-Zeile | Coval-Blog und Leaderboard-Auszüge; Artificial Analysis: Latenzwerte nur für Speech-to-Speech | belegt (Abwesenheit in den abgerufenen Auszügen; die Tabelle `benchmarks.coval.ai/tts` selbst war per WebFetch leer) |

Abgeleitet für Jarvis: Bei unary Gemini-TTS rechnet sich die Wartezeit ungefähr aus Erzeugungsdauer, nicht aus Streaming. Bei 55,8 Zeichen/s dauert ein Satz von 120 Zeichen rund 2 bis 3 s, ein Absatz von 600 Zeichen rund 11 s (eigene Rechnung aus dem Artificial-Analysis-Durchsatz, ohne Netzlaufzeit und Kaltstart). Das ist mit der heutigen Erst-Satz-Teilung (Vorleser.kt) verträglich, nicht aber mit Gesprächsantworten, bei denen jede Sekunde zählt.

Messdefinition-Warnung: Coval hat die TTFA-Messung 2026 umgestellt (Anfangsstille wird nun mitgezählt, „perceived TTFA“); Zeilen vor und nach dem Wechsel seien „not comparable“ (Gradium/Coval, 09.09.2026). Herstellerangaben (serverseitig, ohne Netzwerk) und Coval (Netzwerk-Roundtrip von einem Testserver, Standort unbekannt) dürfen nicht in eine Reihe gestellt werden.

---

## 2. Android-Eignung

### 2.1 REST/WebSocket mit OkHttp, SDKs, gRPC

| Kandidat | Reiner REST/WebSocket mit OkHttp machbar? | Offizielles Android-SDK | gRPC nötig? |
|---|---|---|---|
| Chirp 3 HD, unary | **ja** (so läuft es heute in `GoogleCloudTtsPlayer`/`Synthese`) | Client-Bibliotheken für Android nicht gefunden (unbelegt) | nein |
| Chirp 3 HD, Streaming | **nein**: nur gRPC; OAuth-Scope `cloud-platform` ist in der RPC-Referenz genannt, **API-Key-Authentifizierung für Streaming ist unbelegt** | keine | **ja**. gRPC-Java auf Android: minSdk 24, `grpc-okhttp` + `grpc-protobuf-lite` + `grpc-stub` (Version 1.85.0 laut README), Netty offiziell nicht unterstützt, TLS meist mit Play-Services-Sicherheitsanbieter; R8/ProGuard-Angaben fehlen in der README (https://github.com/grpc/grpc-java/blob/master/README.md, belegt). Proto-Code für die TTS-API müsste selbst generiert werden (nicht geprüft) |
| Gemini-TTS in Cloud TTS, unary | **ja**, MP3 möglich (unary-Formate siehe 1.1) | – | nein |
| Gemini-TTS in Cloud TTS, Streaming | nein | – | ja (wie Chirp) |
| Gemini API (3.8 / 3.1) | **ja**: HTTPS-POST, Header `x-goog-api-key`; SSE-Streaming als lesbarer HTTP-Stream (OkHttp-`EventSource` oder eigener Zeilenleser) | **Firebase AI Logic**: Beispiele in Kotlin/Java, `generateContentStream`, nur Gemini 3.x TTS, Ausgabe rohes PCM; **Preview „isn't subject to any SLA or deprecation policy“**, App-Check-Erzwingung ab 02.11.2026 (https://firebase.google.com/docs/ai-logic/generate-speech, belegt) | nein |
| Azure AI Speech | REST ja (SSML-POST, Header `X-Microsoft-OutputFormat`); Edge-WebSocket ja (heute in Jarvis) | Speech SDK (`client-sdk` 1.52.0, AAR 12,3 MiB, R3 §4.4); Texteingabe-Stream auf Android nicht | nein |
| ElevenLabs | ja (REST-Stream, WebSocket) | kein TTS-SDK für Android (Kotlin-SDK nur Agents, R4 §3.5) | nein |
| Cartesia | ja (WebSocket) | keines (R4 §4.5) | nein |
| Inworld | ja (HTTP-Stream, WebSocket, SSE) | keines (R4 §5.5) | nein |
| Mistral Voxtral TTS 3 | Transport nicht dokumentiert → unbelegt; vermutlich HTTPS | keines gefunden | unbelegt |
| Qwen3-TTS | ja (HTTP/SSE bzw. WebSocket); Jarvis nutzt heute zwei HTTP-Aufrufe | keines | nein |
| Lokal (PC-Server) | ja, eigene Schnittstelle | – | nein |

**Wiedergabe von Streaming-Audio auf Android** (abgeleitet, nicht aus abgerufener Seite): `MediaPlayer` spielt keine wachsende Rohdatei und keinen reinen PCM-Strom. Für PCM-Streaming ist `AudioTrack` im Modus `MODE_STREAM` der übliche Weg (`write()` blockiert, Puffer mindestens `getMinBufferSize`, Unterläufe führen zu Aussetzern). Für komprimierte Ströme (MP3/Opus) bräuchte es Media3/ExoPlayer mit eigener `DataSource` – Jarvis hat `media3-exoplayer` bereits als Abhängigkeit (`app/build.gradle.kts` Zeile 126) – oder man sammelt erst eine Datei. Android spielt Opus im Ogg-Container ab Android 5.0, MP3 und WAV (16 Bit PCM) ebenso (https://developer.android.com/media/platform/supported-formats, belegt); „HTTP/HTTPS Progressive“ wird unterstützt (belegt).

### 2.2 Schlüssel im Client oder Token-Broker, Key-Restriktion

| Anbieter | Was die Quelle sagt | Einstufung |
|---|---|---|
| Google (Gemini API) | „Do not hardcode API keys directly in web or mobile apps.“ – Empfehlung: Backend-Proxy (https://ai.google.dev/gemini-api/docs/api-key). Key-Restriktion als Android-Anwendung (Paketname + 20-Byte-SHA-1) ist in Google Cloud möglich (https://docs.cloud.google.com/api-keys/docs/add-restrictions-api-keys). **Welche Header ein REST-Client dafür senden muss (`X-Android-Package`, `X-Android-Cert`), steht dort nicht**; ein Forenfall (B4X) meldet `REQUEST_DENIED` trotz gesetzter Header, Ursache offen; eine Drittquelle zeigt das Zertifikat als Großbuchstaben-Hex | Restriktion **belegt**; Header-Pflicht für OkHttp **unbelegt/extern** |
| Google, kurzlebige Tokens | „ephemeral tokens are only compatible with Live API“, nur `v1beta`; Gültigkeit 1 Minute zum Start, Nachrichten 30 Minuten (https://ai.google.dev/gemini-api/docs/ephemeral-tokens) | belegt: **nicht für TTS-Modelle nutzbar** |
| Google, Firebase AI Logic | Proxy mit App Check (Play Integrity); „requests from clients without a valid attestation will be rejected“; Replay-Schutz mit Einmal-Token (Gültigkeit 5 Minuten); „Starting November 2, 2026, Firebase App Check enforcement will be required“ (https://firebase.google.com/docs/ai-logic/app-check und /generate-speech) | belegt. TTS dort nur Preview |
| ElevenLabs | Einmal-Token `tts_websocket`, gültig 15 Minuten, wird bei Nutzung verbraucht (https://elevenlabs.io/docs/api-reference/tokens/create) | belegt (aber kein `stream-input` für v3/v4, s. 1.1) |
| Cartesia | Token mit Grant `tts`, `expires_in` höchstens 3600 s; ob der Token im WebSocket per Query-Parameter geht, steht auf dieser Seite nicht (https://docs.cartesia.ai/api-reference/auth/access-token); R4 nennt „kurzlebiges `access_token` für Client-Apps“ | belegt (Token), WebSocket-Weg teils unbelegt |
| Azure | Bearer-Token von `…/sts/v1.0/issueToken`, 10 Minuten gültig (R3 §4.3) – braucht aber den Schlüssel, um ihn zu holen → Broker | belegt (R3) |
| Inworld, Mistral, Qwen | Mobile-Token unbelegt (R4 §5.5); Qwen/Mistral ebenfalls unbelegt | unbelegt |

**Einordnung für Frank (abgeleitet):** Jarvis ist eine private App auf wenigen eigenen Geräten, die Schlüssel liegen bereits verschlüsselt auf dem Gerät (`EncryptedSharedPreferences`, `TtsManager`; `SecureSettings`). Ein Token-Broker ist für diesen Fall kein Muss, wohl aber für Apps, die an Dritte verteilt werden (z. B. GenialerWecker als Verkaufs-App). Für die Google-Schlüssel gilt zusätzlich: Android-Restriktion einrichten und dabei testen, ob OkHttp-Anfragen noch durchkommen (Header-Frage oben).

---

## 3. Integration in Jarvis (Ist-Zustand und Aufwand)

### 3.1 Wie Jarvis heute Sprache erzeugt (aus dem Quelltext gelesen)

| Stelle | Befund |
|---|---|
| `tts/TtsCatalog.kt` | `enum TtsProvider` mit `QWEN_CLONE`, `QWEN`, `GOOGLE_CLOUD`, `EDGE`; Standard ist `EDGE` („garantierter Rückfall“); 30 Chirp-3-HD-Stimmen fest im Katalog; `anbieterZu(stimmeId)` |
| `speech/Vorleser.kt` | Absatz-Pipeline: Text wird in Absätze geteilt; **Vorausschau**: Absatz *n+1* und *n+2* werden schon beauftragt, während *n* läuft (`VORAUSSCHAU`); der **erste Absatz wird nach dem ersten Satz geteilt**, damit der erste Ton früh kommt; abgespielt wird je Teil als **Datei** mit `MediaPlayer` (`spieleDatei`), Lautstärke über `SpeechLoudness.boost` (LoudnessEnhancer auf der Audiositzung des `MediaPlayer`); Retry: Netzfehler einmal wiederholen, `SyntheseFehler` → Absatz halbieren, `SyntheseZeitlimit` → Absatz überspringen, `SyntheseAbbruch` (Schlüssel fehlt) → alles anhalten; Tonfokus je Quelle (`gespraech:` exklusiv); „Erster Ton“ wird gemessen (`Logbuch.sprache`) |
| `speech/Synthese.kt` | Erzeugt je Anbieter **eine Datei** (`google()` → MP3 aus Base64, `qwen()` → **WAV** per Download, `edge()` → MP3 per WebSocket in Datei); `gleichzeitig = Semaphore(2)`; 429-Wartezeiten 1/3/8 s; 401/403 → `SyntheseAbbruch`; `kannVorausschauen()` ist für alle vier Anbieter `true`; **kein Zwischenspeicher**: die Dateien werden nach dem Abspielen gelöscht (`datei.delete()`, `raeumeAuf()`) |
| `tts/TtsManager.kt` + `*Player.kt` | Älterer Direktweg (ein Player je Anbieter, `MediaPlayer`) für Pfade ohne Vorleser-Pipeline; `GoogleCloudTtsPlayer` ruft `text:synthesize` (MP3, `speakingRate`, bei Chirp kein SSML, `customPronunciations` für „Frank“) |
| `live/LiveGespraech.kt` | **eigene** PCM-16-Wiedergabe mit `AudioTrack` und WebSockets zu OpenAI/Gemini Live (24 kHz) – bleibt unverändert und ist von der TTS-Auswahl unabhängig |
| `data/settings/SecureSettings.kt` | bereits vorhanden: `googleTtsApiKey`, `qwenTtsApiKey`, **`geminiApiKey` (AI-Studio-Projekt ohne Abrechnung, „kostenloses Kontingent“, für Gemini Live)** und **`geminiApiKeyBezahlt`** (Projekt mit Abrechnung, Ersatz bei erschöpftem Kontingent) |

### 3.2 Aufwand: neuer Anbieter neben den vorhandenen

**Der Einhängepunkt ist `Synthese.kt`, nicht `GoogleCloudTtsPlayer`** (abgeleitet aus dem Quelltext). Die Absatz-Pipeline arbeitet mit Dateien; ein neuer Anbieter, der eine Datei liefert, bekommt Vorausschau, Pausen, Halbieren-und-Wiederholen, Tonfokus, Pause/Weiter und Lautstärke **ohne Änderung an `Vorleser.kt`**. `spieleDatei` spielt WAV bereits (der Qwen-Weg liefert `.wav`).

Minimaler Eingriff für „Gemini API, 3.8 Flash/Flash-Lite TTS, unary, WAV“ (abgeleitet):

1. `TtsCatalog.kt`: Eintrag in `TtsProvider` (z. B. `GEMINI`), Stimmenliste (30 vorgefertigte Stimmen; Sprache wird laut Gemini-Doku automatisch erkannt, Deutsch ist gelistet), `anbieterZu()`.
2. `Synthese.kt`: Zweig in `synthetisiere()`, `kannVorausschauen()` ergänzen, neue Methode `gemini()`: POST, Base64 decodieren, `schreibeDatei(bytes, "wav")`. **WAV hat bereits den Header**, es braucht dafür keinen Header-Bau. Wird stattdessen gestreamtes rohes PCM (`audio/l16`) gesammelt, muss ein 44-Byte-WAV-Header selbst vorangestellt werden (24 kHz, mono, 16 Bit) – oder man fordert unary an.
3. `SecureSettings.kt` (+ ggf. `Keys`/`Defaults`): Stimme und Schlüssel – aber **nicht den vorhandenen kostenlosen Gemini-Schlüssel wiederverwenden** (siehe 4.1).
4. UI an drei Stellen: `ui/SpracheBausteine.kt` (Anbieter-Chips, Schlüsselfeld), `ui/BenachrichtigungenEinstellungen.kt` (Standardstimmen, Schlüsselprüfung), `faehigkeit/EinstellungenFaehigkeit.kt` (Sprachsteuerung der Stimme: `alleStimmen()`, `jarvisStimme()`, `setzeJarvisStimme()`).
5. `TtsManager.kt` (Direktweg): nur nötig, wenn der Anbieter auch außerhalb der Pipeline gebraucht wird; `speak()` verzweigt per `when(providerId)`, `stop()/pause()/resume()/shutdown()` brauchen je einen Zweig.
6. Bei einem „Direktweg“-Player nach Vorbild `GoogleCloudTtsPlayer` gilt die Hausregel aus `SpeechLoudness.kt`: **jeder** Player muss `SpeechLoudness.boost/release` nutzen, sonst klingt er leise.

Bei **Gemini-TTS über Cloud TTS (2.5 Flash/Pro, unary, MP3)** wäre der Aufwand noch kleiner, weil nur der Request-Körper von `googleAnfrage()` erweitert wird und weiter MP3 zurückkommt (abgeleitet aus den unary-Formaten der Cloud-Seite). Die genauen Feldnamen für Modell und Prompt in `text:synthesize` habe ich in dieser Sitzung **nicht** geprüft (R2 nennt sie nicht; vor dem Bau gegen https://docs.cloud.google.com/text-to-speech/docs/gemini-tts prüfen). Ob Gemini-TTS in Cloud TTS mit demselben API-Key wie Chirp läuft, ist **unbelegt**.

### 3.3 Was sich bei Gemini-TTS inhaltlich ändert

| Punkt | Chirp 3 HD heute | Gemini 3.8 TTS | Folge im Code |
|---|---|---|---|
| Format | MP3 (Base64 in JSON) | unary **WAV** (24 kHz mono 16 Bit); Streaming rohes PCM | `.wav`-Datei wie Qwen; Dateigröße etwa 48 kB je Sekunde (abgeleitet: 24.000 × 2 Byte) gegenüber MP3 deutlich größer, aber im Cache-Ordner unkritisch |
| Steuerung | `speakingRate` 0,7 bis 1,3, `customPronunciations` (IPA) für „Frank“ | Text ist **wörtliches Transkript**; Stil nur über `speech_metadata.style`, Pausen/Lachen als `<…>`-Tags (bei Nicht-Englisch englische Tags); **kein `speakingRate`-Feld** in der Doku gefunden (unbelegt, ob es eines gibt) | Tempo-Variation aus `TtsManager` (`RATE_STEPS`, `PITCH_STEPS`) hätte keine Entsprechung; stattdessen kurzer Stil-Text; deutsche Aussprache von „Frank“ ist über Tags nicht belegt (R2 §6) |
| SSML | bei Chirp nicht erlaubt; Jarvis nutzt `customPronunciations` | kein SSML | `deutschesSsml()` und `chirpAussprachen()` entfallen |
| Eingabelänge | Absatz (Jarvis teilt vorher) | Eingabe 8.192 Token; Cloud-Variante 4.000 Byte Text + 4.000 Byte Prompt, Ausgabe höchstens etwa 655 s | Absatzgrößen von Jarvis passen; Google rät „Lange Antworten in kürzere Turns aufteilen“ |
| Latenz je Absatz | Chirp ca. 2,9 s je 356 Zeichen (R2) | ca. 6,4 s je 356 Zeichen (R2, Durchsatzrechnung) | Erst-Satz-Teilung bleibt wichtig; bei Gesprächsantworten (`gespraech:`) spürbar langsamer als Chirp (abgeleitet) |
| Gültigkeitsprüfung | HTTP-Status + nichtleeres `audioContent` | **`finishReason` allein reicht nicht** (siehe 5.2) | nach dem Dekodieren Dauer gegen Textlänge prüfen und bei Abweichung als `SyntheseFehler` werfen → vorhandener Halbieren-Pfad greift |
| Wiederholungen | – | Abgeschnittene Ausgabe wird trotzdem berechnet (Forum) | Wiederholungsanzahl begrenzen (Kosten) |
| Zwischenspeicher | keiner | derselbe Text liefert bei Gemini nicht jedes Mal dieselbe Stimme (Voice-Drift, 5.2) | ein Datei-Cache nach Text-Hash würde Kosten sparen und Wiederholungen (`wiederholen=true`, `varied`) stabilisieren; ist heute auch für Chirp nicht vorhanden → eigene Entscheidung |
| Streaming-Pfad | – | PCM-Chunks direkt in `AudioTrack` | **umgeht die Datei-Pipeline** (verliert Vorausschau, `spieleDatei`, Pause/Weiter-Logik, LoudnessEnhancer-Anbindung an den `MediaPlayer`); größerer Umbau; und der Gewinn ist unsicher, weil der Erst-Satz-Schnitt den Antwortbeginn schon verkürzt und das Streaming bei 3.1 nachweislich abbrach (5.2) |

Bewertung (abgeleitet): Für Jarvis ist **unary + Datei** der risikoärmste Weg; Streaming lohnt nur dort, wo der erste Satz selbst lang dauert, und sollte nur für einen Anbieter mit nachweislich stabilem Streaming erwogen werden (aus dieser Recherche: ElevenLabs Flash, Inworld, Cartesia nach Coval/Hersteller; Gemini nicht).

### 3.4 Wechselwirkung mit Live-Gesprächen

`live/LiveGespraech.kt` hat eine eigene `AudioTrack`-Wiedergabe und eigene WebSockets; der TTS-Pfad (`Vorleser`/`Synthese`) wird dort nicht benutzt (Grep auf `tts`/`Vorlese` fand nur Stimmen-Namen für Live-Modelle). Ein neuer TTS-Anbieter berührt Live-Gespräche daher nicht. Der gemeinsame Punkt ist nur der Gemini-Schlüssel (4.1).

### 3.5 Bewertung für die weiteren eigenen Apps (nur kurzer Grep, Quelltext nicht vollständig gelesen)

| App | Heute benutzte Sprachausgabe (Grep) | Folge |
|---|---|---|
| **PerfectMoment** (`app/…/tts/`) | gleiches Paket wie Jarvis: `EdgeTtsPlayer`, `GoogleCloudTtsPlayer`, `QwenTtsPlayer`, `TtsCatalog`, `TtsManager`, `SpeechLoudness`; `TtsProvider` mit Edge, Qwen-Klon u. a. | Änderungen müssten ein zweites Mal gemacht werden, **oder** das `tts`-Paket als Modul (Skill `modul-erstellen`) herausgelöst werden. PerfectMoment-Edge sendet ebenfalls `Sec-MS-GEC-Version=1-…` (Zeile 69), siehe BUG-KANDIDATEN |
| **GenialerWeckerAndroid** | nur **Android-`TextToSpeech`** auf dem Gerät (`LokaleStimme.kt`, Google-Engine `com.google.android.tts` bevorzugt) | offline, kein Schlüssel, keine Datenübertragung → für eine Verkaufs-App datenschutzfreundlich; ein Cloud-Anbieter brächte Kosten, Schlüsselproblem (Broker nötig, Abschnitt 2.2) und AV-Vertragsfragen. Empfehlung (abgeleitet): so lassen; ein lokales Modell (R7: Piper/sherpa-onnx) wäre die einzige Cloud-freie Aufwertung |
| **VoiceAgent** (Windows, C#) | `Services/GoogleTtsClient.cs`: `texttospeech.googleapis.com`, API-Key, **MP3**, Chirp-3-HD-Stimme | Wie Jarvis; Gemini-TTS-Umstieg ist reiner Request-Austausch; Dekodierung/Wiedergabe unter Windows unkritisch |
| **vorlese-overlay-v2** (Browser-Erweiterung) | `engines/edge-tts.js` (mit Sec-MS-GEC-Token, Zeile 65 ff., Version `1-…` in Zeile 122) und `engines/google-tts.js` | gleiches Edge-Risiko wie Jarvis |
| **TerminalVoiceOverlay / ClaudeVoiceOverlay (Windows)** | Grep nach `SpeechSynthesizer`, `edge-tts`, `texttospeech`, `elevenlabs`, `piper` fand in den Ordnern **keinen Treffer**; die Ordner liegen unter `TerminalVoiceOverlay-Windows`/`ClaudeVoiceOverlay-Windows` | Sprachausgabe dort nicht bestimmbar; keine Aussage |

---

## 4. Datenschutz / DSGVO je Anbieter

### 4.1 Google: drei Wege, drei Regelwerke

| Weg | Aufbewahrung / Protokoll | Training mit Kundendaten | EU-Endpunkt | Vertrag | Quelle |
|---|---|---|---|---|---|
| **Cloud Text-to-Speech** (Chirp 3 HD, Gemini-TTS 2.5/3.1) | „Google does not log any customer Cloud TTS text or audio data.“ – Dienst „stateless and resourceless“; keine Aufbewahrungsfristen genannt | Seite beantwortet das **nicht** (nur allgemein „improve its products“) → unbelegt | `eu-texttospeech.googleapis.com`; Modelltabelle nennt Chirp 3 HD, Gemini 2.5 Flash/Pro; **Widerspruch** auf derselben Seite: „Only Neural2 voices are available from these endpoints“ (nicht aufgelöst). Gemini-TTS laut Gemini-TTS-Seite: `eu`/`us` nur 2.5-Modelle, 3.1 nur `global`. Datenverarbeitung „within the specific region“ | Google ist laut DPA Auftragsverarbeiter (4.1 des Cloud DPA); **ob Cloud TTS in Anhang 4 steht, konnte ich nicht lesen → unbelegt**. **Preview-Funktionen (u. a. Chirp-Streaming, SSML) fallen unter Pre-GA-Bedingungen, ohne SLA und laut R1 ohne Datenverarbeitungsvertrag** (R1 §„Preview“, Service-Terms Abschnitt 5, nicht neu geprüft) | https://docs.cloud.google.com/text-to-speech/docs/data-logging, /endpoints, /gemini-tts (belegt); DPA https://cloud.google.com/terms/data-processing-addendum |
| **Gemini API, bezahlt** (Projekt mit aktivem Billing) | Prompts und Antworten werden **55 Tage** für Missbrauchserkennung protokolliert (Gemini-API-Seite „Abuse monitoring“); Verarbeitung kann „in jedem Land mit Google-Einrichtungen“ liegen; Interactions API: Standard `store=true`, bezahlt **55 Tage**, in AI Studio auf 7/14/28/55 Tage kürzbar, `store=false` möglich | **Nein** („Google doesn't use your prompts … or responses to improve our products“); geloggte Daten fließen nicht in Training (außer Modelle zur Richtliniendurchsetzung); menschliche Prüfung nur durch autorisierte Google-Mitarbeitende bei markierten Inhalten | **keine Regionswahl** auf der Entwickler-API belegt; OpenRouter: Gemini 3.8 Flash TTS „not currently available through EU in-region routing“ | Verarbeitung „in accordance with the Data Processing Addendum for Products Where Google is a Data Processor“ | https://ai.google.dev/gemini-api/terms, /docs/usage-policies.md.txt, /docs/interactions (belegt) |
| **Gemini API, kostenlos** („Unpaid Services“, auch AI Studio ohne Billing) | Aufbewahrung nicht allgemein angegeben (Interactions: 1 Tag) | **Ja**: „Google uses the content you submit … to provide, improve, and develop“; „human reviewers may read, annotate, and process your API input and output“; „Do not submit sensitive, confidential, or personal information to the Unpaid Services.“ | – | **EWR-Klausel:** „You may use only Paid Services when making API Clients available to users in the European Economic Area“ (Use Restrictions). Zugleich gilt im Abschnitt Unpaid für EWR/Schweiz/UK, dass die Paid-Datenregeln für alle Dienste gelten („apply to all Services, including Google AI Studio and unpaid quota“) – die Seite widerspricht sich hier nicht, schreibt aber zwei Dinge vor; für Franks privaten Gebrauch in Deutschland ist die zweite Aussage die einschlägige | https://ai.google.dev/gemini-api/terms (belegt) |

**Befund für Jarvis (abgeleitet aus Quelltext + Google-Bedingungen):** `SecureSettings.geminiApiKey` stammt laut Kommentar aus einem AI-Studio-Projekt **ohne Abrechnung**. Wofür „nur bezahlter Schlüssel“ spricht, steht in den Bedingungen selbst: (a) Abschnitt „Use Restrictions“: „You may use only Paid Services when making API Clients available to users in the European Economic Area“ – gilt dem Wortlaut nach für Clients, die EWR-Nutzern bereitgestellt werden; (b) die Protokollierung über 55 Tage für Missbrauchserkennung besteht nach der Abuse-Monitoring-Seite unabhängig vom Tarif (die Seite nennt keine Tarifunterschiede); (c) die EWR-Sonderregel, nach der die Paid-Datenregeln auch für kostenlose Kontingente gelten, würde Franks Konto plausibel ebenfalls erfassen – das ist aber **nicht rechtlich bestätigt**, und die Seite nennt im selben Atemzug für Unpaid Services Produktverbesserung und menschliche Prüfung. Wegen dieser Unklarheit gilt als vorsichtige Wahl: für TTS mit persönlichen Texten (HRV, Schlaf, Termine aus der Tagesauswertung) nur `geminiApiKeyBezahlt` bzw. einen Cloud-TTS-Schlüssel verwenden. Die Rechtsfrage entscheidet diese Recherche nicht. Der kostenlose Schlüssel wird schon heute für Gemini Live (Mikrofon-Audio) benutzt – hier nur als Hinweis vermerkt, nicht Gegenstand der Studie.

### 4.2 Weitere Anbieter (aus R3 bis R7, nicht neu geprüft; dort mit Primärquellen)

| Anbieter | Aufbewahrung / Training | EU / Vertrag | Quelle |
|---|---|---|---|
| **Azure AI Speech** | Echtzeit-TTS: „Neither input text nor output audio content is stored in Microsoft logs.“; eigene Custom-Voice-Daten nicht zum Training | Verarbeitung in der Ressourcenregion; EU mit HD/MAI: westeurope, francecentral, swedencentral; `germanywestcentral` nur Standard-Neural | R3 §5.1 |
| **Edge-Endpunkt (inoffiziell)** | keine dokumentierten Bedingungen gefunden (R3 §6.4: „Grauzone“) | kein Vertrag | R3 §6.4 |
| **ElevenLabs** | Standard-Hosting USA; EU-Residenz und Zero Retention nur Enterprise; DPA und SOC 2 vorhanden | – | R4 §3.6 |
| **Cartesia** | EU-Endpunkt und ZDR nur Enterprise; laut AGB Training möglich, Opt-out per Formular | – | R4 §4.6 |
| **Inworld** | SOC 2 Typ II; DPA nur Enterprise; EU/ZDR nur Enterprise; Training unbelegt | – | R4 §5.6 |
| **Mistral Voxtral TTS 3** | Trust-Center-Seite nennt nur Stichworte (Speicherdauer, Training, EU-Hosting, DPA, Zero Data Retention) ohne Erläuterung, „z. B. 30 Tage“ als Beispielwert → **unbelegt**; Unternehmen mit Sitz in Frankreich | EU-Hosting der TTS-API nicht ausdrücklich belegt | https://trust.mistral.ai/, R5 §6 |
| **OpenAI** | EU-Endpunkt mit Zusatzvereinbarung; Missbrauchsprotokoll bis 30 Tage; Abschaltung der TTS-Modelle 06.01.2027 | – | R5 §6 |
| **Qwen / Alibaba** | „never uses your data for model training“ (FAQ); Aufbewahrungsdauer nicht genannt; Texte gehen nach Singapur (Region `ap-southeast-1`); kein AV-Vertrag gefunden; **Domänenwechsel**: `dashscope.aliyuncs.com` ohne neue Funktionen nach 30.09.2026 | Frankfurt-Region existiert, aber ohne TTS-Modelle belegt | R6 §1.9 |
| **Lokales Modell auf PC-Server** | Texte verlassen das eigene Netz nicht (WLAN/WireGuard); Lizenz je Modell (z. B. Phonon-DE und Voxtral-Open-Weights nur nicht kommerziell, R7/R5) | kein Vertrag nötig | R7 |

KI-Kennzeichnung: R5 verweist auf eine Pflicht zur Kennzeichnung täuschend echter KI-Stimmen seit 02.08.2026 (eigene Auswertung von `tts-provider.md` §17); gilt anbieterunabhängig, für Jarvis als private App eher nachrangig, für eine Verkaufs-App wie GenialerWecker relevant.

---

## 5. Zuverlässigkeit

### 5.1 Statusseiten und Vorfälle 2026

| Quelle | Befund | Einstufung |
|---|---|---|
| Google Cloud Status (`incidents.json`, Abruf 21.10.2026) | Enthält nur 8 Vorfälle im Jahr 2026 (öffentlicher Ausschnitt, kein vollständiger Verlauf). **Kein Vorfall mit „Text-to-Speech“.** Ein Vorfall mit „Vertex Gemini API“: 27.02.2026, 12:37 bis 14:35 UTC, Schweregrad niedrig, „increased error rates when accessing the global endpoint“. 20.10.2026 `europe-west2`: 38 Produkte, darunter Speech-to-Text (nicht TTS). 20.08.2026 `us-west1` hoch: kein TTS gelistet | belegt (Ausschnitt) |
| Gemini-API-Status (`aistudio.google.com/status`) | per WebFetch ohne Inhalt (JavaScript) | unbelegt |
| Azure Status-Verlauf | zwei PIR 2026 sichtbar: 30.09.2026 Gateway (kein Speech), 29.09.2026 „Azure AI services … Sweden Central“ (Speech nicht ausdrücklich genannt); die Seite zeigte eine Fehlermeldung → Liste möglicherweise unvollständig. Eine Suche fand zusätzlich einen Vorfall West US am 23.07.2026 (~3,8 h, „Azure AI Speech“ unter den betroffenen Diensten, extern) | belegt (Auszug)/extern |
| ElevenLabs | laut R4: Ausfälle bzw. Latenzspitzen am 03.08., 27.08., 12.10. und 13.10.2026 (aus Titeln abgeleitet) | belegt (R4) |
| Cartesia | TTS-Verfügbarkeit 99,77 % (Juli bis Oktober 2026) | belegt (R4) |
| Inworld, Mistral | Statusseite nicht auswertbar bzw. Dienst erst seit 20.10.2026 | unbelegt |
| Edge-Endpunkt | Ausfallwellen: Einführung des Sec-MS-GEC-Tokens (Nov. 2024 bis Jan. 2026) und **10. bis 13.10.2026** (Token v2 verpflichtend, 403; gestaffelter Rollout, in Frankreich, Belgien, Spanien, UK zuerst); jeweils etwa 3 Tage Ausfall bis die Bibliothek nachzog | extern (GitHub-Issue #490, R3 §6.2) |

### 5.2 Gemini-TTS: Ausfallmuster mit Belegen

| Muster | Modell | Datum | Beleg | Google-Reaktion |
|---|---|---|---|---|
| **HTTP 500 INTERNAL bei jeder Anfrage** (Einzelfall, Server brauchte ca. 2,7 s bis zum Fehler) | 3.1 Preview | 28.05.2026 | Foren-Bugreport https://discuss.ai.google.dev/t/bug-report-gemini-3-1-flash-tts-preview-returns-500-internal-on-every-request-tts-unusable/167574 (extern, nur Suchauszug) | keine gefunden |
| **Offiziell dokumentiert:** „gelegentlich liefert das Modell Text-Tokens statt Audio, was zu einem Fehler 500 führt. Retry-Logik ist empfohlen.“; bei langen Ausgaben lässt die Qualität nach, Texte in kleinere Teile zerlegen; vage Prompts können vom Klassifizierer abgelehnt werden (`PROHIBITED_CONTENT`) | Gemini 3.x TTS | Stand 21.10.2026 | Firebase AI Logic, https://firebase.google.com/docs/ai-logic/generate-speech (belegt) | – |
| **HTTP 200, aber leere Antwort** mit Audio-Token-Abrechnung, `finishReason: OTHER` | 3.1 Preview (Hebräisch) | 2026 | Foren-Bugreport (Suchauszug) | – |
| **Abbruch bei Streaming** ab ca. 60 s (`OTHER`, HTTP 200), nicht-streamend vollständig | 3.1 Preview | gemeldet 02.06.2026; Google 23.06.2026: „Thanks for catching. We were able to reproduce and will work on a fix.“; weiter offen am 09.07., 26.08., 14.09.2026 | https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-preview-streamgeneratecontent-truncates-audio-finishreason-other-past-60s-while-generatecontent-non-streaming-works/169063 | einmalige Bestätigung, kein bestätigter Fix |
| **Abbruchrate 50 bis 75 %**, zeitabhängig, nicht stimmenabhängig (Audit 25.07.2026: 15/30 vollständig, zwei Stunden später 7/30; Aufteilen in ca. 70 Wörter hilft nur teilweise; abgeschnittenes Audio wird berechnet) | 3.1 Preview | 27.07.2026 | https://discuss.ai.google.dev/t/gemini-3-1-flash-tts-preview-audio-truncation-finishreason-other-is-time-varying-and-not-voice-specific-two-audits-of-all-30-voices-2h-apart/176215 | keine |
| **Exakt 20 s / 1.280.000 Base64-Zeichen** und `OTHER`; Wiederholung liefert oft vollständiges Audio | 3.1 Preview (SSE) | 14.05.2026 | Thread 144813 (R2) | keine |
| **„Stoppt früh, meldet aber STOP/completed“**: bei Text mit `<long pause>`-Tags brechen Läufe nach 16 bis 20 s ab (400 bis 510 statt 1.250 bis 1.640 Ausgabe-Token); ca. die Hälfte der Produktionsanfragen, 7 von 9 bei echten Batches; Wiederholung bricht bei ähnlicher Länge ab; abgeschnittenes Audio berechnet; `gemini-3.8-flash-tts` lieferte in zwei Vergleichsläufen vollständig (76,7 s und 79,6 s) | **3.8 Flash-Lite**, mit Vergleich zu 3.8 Flash | 21.10.2026 | https://discuss.ai.google.dev/t/gemini-3-8-flash-lite-tts-stops-early-on-batched-text-with-long-pause-tags-while-reporting-completed-stop-truncated-audio-is-still-billed/191142 (extern, Einzelfall) | keine sichtbar; Autor fragt nach unterscheidbarem `finishReason` |
| **Ungeskriptete Sprache und lange Stille** (152,68 s Audio, ca. 122 s Stille, wiederholter Input, französischer Antwortteil; 1 von 3 identischen Anfragen) | 3.8 Flash | 18.10.2026 | https://discuss.ai.google.dev/t/gemini-3-8-flash-tts-sometimes-adds-unscripted-speech-and-long-silences/190222 | keine |
| **Stimm-Drift** (Stimme ändert sich bei gleichem Skript, fast alle Stimmen) | 3.8 Flash/Flash-Lite | 07.10.2026; Google bittet am 08.10. per DM um Details | https://discuss.ai.google.dev/t/attn-voice-drifting-and-voice-change-in-gemini-3-8tts-flash-and-flash-lite/187368 | Rückfrage, kein Fix |
| **Rauschstoß am Clipende**, vermutet durch WAV-Header statt rohem PCM bei 3.8 | 3.8 | 2026 | https://github.com/lfnovo/esperanto/issues/291 (Suchauszug) | – |
| 502-Fehler über Drittrouter (Pollinations) ab ca. 30.09.2026; über OpenAI-kompatiblen Endpunkt nutzbar nicht – „native generateContent“ nötig | 3.8 | 30.09. bis 03.10.2026 | GitHub-Issues pollinations #16435, DeepTutor #1951 (Suchauszug) | – |
| 403 nach erster erfolgreicher Anfrage (Tier 1 Prepay) | 3.1 | 09.08.2026 | Foren-Thread 177765 (Suchauszug) | – |

Abgeleitet für Jarvis: `finishReason == STOP` ist kein Gültigkeitsnachweis (Early-Stop meldet STOP). Es braucht eine eigene Prüfung **Dauer des WAV gegen Textlänge** (z. B. erwartete Sekunden aus Zeichenzahl und Durchsatz; ein WAV mit 24 kHz mono 16 Bit hat 48.000 Byte je Sekunde, die Dauer lässt sich ohne Decoder aus der Dateigröße lesen). Bei Abweichung `SyntheseFehler` werfen: Der vorhandene Pfad halbiert und wiederholt, bei 2 parallelen Anfragen und bezahltem Schlüssel entstehen dadurch Mehrkosten, weil Abgeschnittenes trotzdem berechnet wird. Bei Fehlschlag beider Hälften überspringt `hole()` den Absatz – für Gesprächsantworten bleibt das eine hörbare Lücke.

### 5.3 Vorschau gegen allgemein verfügbar: Abschaltrisiken

| Modell | Status | Abschaltung |
|---|---|---|
| `gemini-2.5-flash-preview-tts`, `gemini-2.5-pro-preview-tts` (Gemini API) | Preview | **frühestens 17.11.2026**; Ersatz 3.8 Flash bzw. Flash-Lite (https://ai.google.dev/gemini-api/docs/deprecations, belegt; „earliest possible dates“) |
| `gemini-3.1-flash-tts-preview` (Gemini API) | Preview | **frühestens 17.11.2026** (ebd.); Franks `proben/`-Hörproben mit diesen Modellen sind danach nicht reproduzierbar |
| `gemini-3.8-flash-tts`, `-flash-lite-tts` | Gemini API: GA laut Changelog 22.09.2026; kein Abschaltdatum | Vertex/Agent-Platform-Modellseite: Preview, nur `global`, 28.09.2026 (R2) – Statuswiderspruch zwischen den Zugängen |
| `gemini-2.5-flash-tts`, `gemini-2.5-pro-tts` (**Cloud TTS**) | im Text nicht als Preview geführt, GA seit 30.09.2025 (R2) | in der Abschaltliste der Gemini API **nicht** aufgeführt – die Liste betrifft die Gemini API, nicht Cloud TTS (abgeleitet; Cloud-TTS-Abkündigungsseite nicht gesondert geprüft) |
| Firebase AI Logic Sprachausgabe | Preview, „isn't subject to any SLA or deprecation policy“ | jederzeit änderbar |
| Chirp 3 HD, unary | GA für de-DE | – |
| Chirp 3 HD, Streaming | Preview | jederzeit änderbar |
| Voxtral TTS 3 | Public Preview seit 20.10.2026 | – |
| OpenAI TTS | – | 06.01.2027 (R5) |
| Qwen: `dashscope.aliyuncs.com` | – | keine neuen Funktionen nach 30.09.2026; Wechsel zur Workspace-Domäne empfohlen (R6). **Jarvis nutzt `dashscope-intl.aliyuncs.com`** (Singapur), weiter gelistet |
| Edge-Endpunkt | inoffiziell | jederzeit, belegt zweimal |

### 5.4 Fallback-Strategie (abgeleitet)

Jarvis hat bereits eine Kette: Vorausschau → Netz-Wiederholung → Halbieren → Überspringen. Was fehlt, ist ein **Anbieterwechsel bei Dauerfehlern**: Heute führt ein ausgefallener Anbieter nur zur Meldung „Das Vorlesen kam nicht durch“ (`Vorleser.kt`). Mögliche Reihenfolge, die sich aus den Belegen ergibt: Gemini 3.8 (Qualität) → Chirp 3 HD unary (stabil, GA, belegt „does not log“) → Edge (kein Schlüssel; aber siehe BUG-KANDIDATEN) → Gerät-`TextToSpeech` (offline, wie GenialerWecker). Auslöser: zwei fehlgeschlagene Absätze in Folge oder Dauer-Prüfung fällt durch. Das ist eine Entwurfsidee, keine belegte Praxis.

---

## 6. Gesamtbild (Fakten, die Entscheidung bleibt bei Frank)

- **Streaming ist für Jarvis kein Gewinn, solange die Datei-Pipeline mit Erst-Satz-Schnitt trägt.** Chirp-Streaming ist gRPC und Preview, Gemini-Streaming bricht nachweislich ab und liefert womöglich erst nach vollständiger Erzeugung (extern).
- **Gemini 3.8 per unary-WAV ist technisch ein kleiner Eingriff** (Katalog, `Synthese.gemini()`, Schlüssel, drei UI-Stellen), braucht aber eine **Längenprüfung** und einen **bezahlten Schlüssel**.
- **Die Google-Zugänge unterscheiden sich im Datenschutz stark:** Cloud TTS protokolliert nicht; Gemini API bezahlt protokolliert 55 Tage ohne Training; Gemini API kostenlos trainiert und lässt Menschen mitlesen.
- **Für alle Apps mit Edge** (Jarvis, PerfectMoment, vorlese-overlay-v2) steht ein möglicher Ausfall durch den Token-Wechsel im Raum (BUG-KANDIDATEN).
- **Echte TTFA-Zahlen** für Google gibt es nicht; eine eigene Messung mit `Logbuch.sprache("Erster Ton")` (existiert in `Vorleser.kt`) auf dem Gerät wäre die einzige belastbare Quelle.

---

## Quellenverzeichnis (alle am 21.10.2026 abgerufen, sofern nicht anders vermerkt)

Lokal: `C:\Users\barwa\proggs\Jarvis\app\src\main\java\de\frank\jarvis\tts\{TtsManager,TtsCatalog,GoogleCloudTtsPlayer,EdgeTtsPlayer,QwenTtsPlayer,DeutscheAussprache,SpeechLoudness}.kt`, `speech\{Vorleser,Synthese}.kt`, `live\LiveGespraech.kt`, `data\settings\SecureSettings.kt`, `app\build.gradle.kts`; `PerfectMoment\app\src\main\java\de\frank\perfectmoment\tts\*`; `GenialerWeckerAndroid\app\src\main\java\de\frank\wecker\LokaleStimme.kt`; `VoiceAgent\src\VoiceAgent\Services\GoogleTtsClient.cs`; `vorlese-overlay-v2\engines\{edge-tts,google-tts}.js`; Berichte `Recherchen\tts-vergleich\r1…r7`.

Google: https://docs.cloud.google.com/text-to-speech/docs/create-audio-text-streaming · https://docs.cloud.google.com/text-to-speech/docs/reference/rpc/google.cloud.texttospeech.v1 · https://docs.cloud.google.com/text-to-speech/docs/gemini-tts · https://docs.cloud.google.com/text-to-speech/docs/endpoints · https://docs.cloud.google.com/text-to-speech/docs/data-logging · https://cloud.google.com/terms/data-processing-addendum (nur teilweise lesbar) · https://ai.google.dev/gemini-api/docs/speech-generation · https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash-tts · https://ai.google.dev/gemini-api/docs/changelog · https://ai.google.dev/gemini-api/docs/deprecations · https://ai.google.dev/gemini-api/docs/pricing · https://ai.google.dev/gemini-api/docs/rate-limits · https://ai.google.dev/gemini-api/terms · https://ai.google.dev/gemini-api/docs/usage-policies.md.txt · https://ai.google.dev/gemini-api/docs/interactions · https://ai.google.dev/gemini-api/docs/api-key · https://ai.google.dev/gemini-api/docs/ephemeral-tokens · https://docs.cloud.google.com/api-keys/docs/add-restrictions-api-keys · https://firebase.google.com/docs/ai-logic/generate-speech · https://firebase.google.com/docs/ai-logic/app-check · https://status.cloud.google.com/incidents.json (lokal ausgewertet)

Foren und Issues (extern): https://discuss.ai.google.dev/t/3-1-flash-tts-preview-streaming-latency/176050 · …/gemini-3-1-flash-tts-preview-streamgeneratecontent-truncates-audio-finishreason-other-past-60s-while-generatecontent-non-streaming-works/169063 · …/gemini-3-1-flash-tts-preview-audio-truncation-finishreason-other-is-time-varying-and-not-voice-specific-two-audits-of-all-30-voices-2h-apart/176215 · …/gemini-3-8-flash-lite-tts-stops-early-on-batched-text-with-long-pause-tags-while-reporting-completed-stop-truncated-audio-is-still-billed/191142 · …/gemini-3-8-flash-tts-sometimes-adds-unscripted-speech-and-long-silences/190222 · …/attn-voice-drifting-and-voice-change-in-gemini-3-8tts-flash-and-flash-lite/187368 · Suchauszüge: Thread 167574 (500 INTERNAL), 177765 (403), 144813, 145337, 144297 · https://github.com/lfnovo/esperanto/issues/291 · https://github.com/pollinations/pollinations/issues/16435 · https://github.com/HKUDS/DeepTutor/issues/1951 · https://aiintoai.vercel.app/article/google-launches-expressive-gemini-3-8-flash-tts-models (Quelle der 142 ms fehlt) · https://openrouter.ai/google/gemini-3.8-flash-tts

Messungen: https://www.coval.ai/blog/tts-latency-benchmark-2026/ · https://gradium.ai/blog/coval-perceived-ttfa-benchmark · https://benchmarks.coval.ai/tts (Tabelle per WebFetch leer)

Weitere: https://docs.mistral.ai/models/voxtral-mini-tts-3-0 · https://docs.mistral.ai/capabilities/audio/text_to_speech/ · https://trust.mistral.ai/ · https://elevenlabs.io/docs/api-reference/tokens/create · https://docs.cartesia.ai/api-reference/auth/access-token · https://azure.status.microsoft/en-us/status/history/ · https://developer.android.com/media/platform/supported-formats · https://github.com/grpc/grpc-java/blob/master/README.md · https://grpc.io/docs/platforms/android/java/basics/

---

BEST-PRACTICES-KANDIDATEN:

1. **Gemini-TTS: Gültigkeit selbst prüfen, nicht nur `finishReason`.** Frühabbrüche melden `STOP`/`completed`; WAV-Dauer (Bytes ÷ 48.000 bei 24 kHz/mono/16 Bit) gegen Textlänge vergleichen, bei Abweichung wiederholen oder Text teilen; abgeschnittenes Audio wird berechnet (Foren 169063, 176215, 191142, 21.10.2026; Firebase-Doku: 500 bei Text-Token-Antwort, Retry empfohlen).
2. **Gemini-TTS in Android-Apps: unary + Datei statt Streaming, bis Google den SSE-Abbruch bestätigt behebt.** Unary liefert WAV mit Header; Streaming liefert rohes PCM ohne Header; Header selbst bauen, wenn PCM gesammelt wird (Gemini-Doku, 21.10.2026).
3. **Gemini API: `store=false` setzen** (Interactions API speichert standardmäßig, bezahlt 55 Tage, kostenlos 1 Tag), und für personenbezogene Texte **nur Paid Services** (Unpaid: Training + menschliche Prüfung).
4. **Google-Zugang nach Datenschutz wählen:** Cloud TTS protokolliert Text/Audio nicht („does not log any customer Cloud TTS text or audio data“) und hat `eu`-Endpunkt (2.5-Modelle, Chirp 3 HD laut Modelltabelle); die Entwickler-API hat keine Regionswahl.
5. **Streaming-Audio in Android:** PCM nur mit `AudioTrack` (`MODE_STREAM`); komprimierte Ströme mit Media3/ExoPlayer; bei dateibasierter Pipeline lieber unary. Jede neue Wiedergabe-Route muss `SpeechLoudness.boost/release` nutzen (Hausregel in `SpeechLoudness.kt`).
6. **Client-Schlüssel:** Google rät vom Einbetten in Apps ab; Ephemeral Tokens gelten nur für die Live API; bei Verteilung an Dritte Firebase AI Logic (App Check, ab 02.11.2026 Pflicht) oder eigener Broker; ElevenLabs/Cartesia haben Einmal-/Kurzzeit-Token für TTS.
7. **Modellnamen mit Ablaufdatum führen:** `gemini-2.5-*-preview-tts` und `gemini-3.1-flash-tts-preview` frühestens 17.11.2026 abgeschaltet; Hörproben und feste Modell-Strings in Apps dagegen prüfen.
8. **TTFA-Angaben immer mit Messort kennzeichnen** (Hersteller serverseitig gegenüber Netzwerk-Roundtrip; Coval hat die Definition 2026 geändert, Werte davor/danach nicht vergleichbar).
9. **Schlüssel-Wiederverwendung prüfen:** den kostenlosen Gemini-Schlüssel (Live-Gespräch) nicht für Vorlesen mit persönlichen Texten verwenden.

BUG-KANDIDATEN:

1. **Jarvis/PerfectMoment/vorlese-overlay-v2: Edge-TTS sendet `Sec-MS-GEC-Version=1-143.0.3650.75`** (Jarvis `EdgeTtsPlayer.kt` Zeile 435, PerfectMoment `EdgeTtsPlayer.kt` Zeile 69, vorlese-overlay-v2 `edge-tts.js` Zeile 122). R3 §6.2 dokumentiert den Wechsel auf Token **v2** mit 403-Wellen vom 10. bis 13.10.2026 (Frankreich, Belgien, Spanien, UK zuerst). **Aus Code + R3 abgeleitet, nicht live geprüft.** Einen Hinweis gibt R3 selbst: „Using the new v2 version string with the legacy token still returns 403“ und der Altzugang über den Host `api.msedgeservices.com` funktionierte zeitweise; ob der Altweg über `speech.platform.bing.com` aktuell läuft, ist offen. Prüfen: Jarvis-Logbuch auf „Edge-Stimme Verbindung gescheitert … code=403“ (`Synthese.kt` schreibt das); ist Edge der Standardanbieter und fällt er aus, fehlt der „garantierte Rückfall“.
2. **Jarvis `Vorleser`: Ausfall eines Anbieters ohne Anbieterwechsel.** Kommt kein Absatz durch, folgt nur die Fehlermeldung „Das Vorlesen kam nicht durch. Prüf die Internetverbindung.“ – auch wenn die Ursache ein toter Dienst ist (z. B. Edge-403).
3. **Gemini-TTS (Fremdbug, relevant bei Einbau):** Early-Stop mit `STOP` (Flash-Lite 3.8, 21.10.2026), lange Stille und ungeskriptete Sprache (3.8 Flash, 18.10.2026), Stimm-Drift (3.8, 07.10.2026), Rauschstoß am Ende (Header-Frage), Streaming-Abbruch (3.1, seit 02.06.2026 unbehoben).
4. **Unklarheit in Google-Dokumentation:** EU-Endpunkt-Seite widerspricht sich (Tabelle: Chirp 3 HD/Gemini; Text: „Only Neural2 voices“); Cloud-Release-Notes ohne 2026-Einträge (R1); Gemini-TTS-Seite spricht von „Gemini 3.8 … nur über die Gemini Enterprise API“, die Modellliste nennt 3.1; die Gemini-API-Doku verwendet nun die Interactions API, die Migrationsnotiz nennt `AUDIO_L16` für `generateContent`.
