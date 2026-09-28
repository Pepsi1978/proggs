# TTS-Provider (Edge-TTS, Google Chirp 3 HD) — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man die Text-to-Speech-Anbindung in BestJournal (Android, Kotlin) und
> `vorlese-overlay-v2` (Chrome-Erweiterung, JS) heute am besten baut — Stimmen-Auswahl,
> Vorlese-Funktion, Fallback. **Fokus deutsche Stimmen.**
> **Versions-Anker:** `rany2/edge-tts` **7.2.8** (22.03.2026) · Google Cloud Text-to-Speech
> **API v1**, **Chirp 3: HD** (de-DE GA), SSML für Chirp3-HD = **Preview** (Doku-Stand 09.06.2026),
> Quotas-Stand 01.06.2026 · Android `android.speech.tts.TextToSpeech` (Platform-SDK).
> **Gegenstück (was schiefgeht):** [`bugs/apis/tts-provider.md`](../../bugs/apis/tts-provider.md).
> **Anbieterübergreifende Resilienz** (Retry/Backoff/Timeout/Secrets) gilt zusätzlich:
> [`best-practices-api-integration-general.md`](best-practices-api-integration-general.md).
>
> **ElevenLabs:** auf Wunsch (zu teuer, ~$0.30/1.000 Zeichen Flash v2.5 ≈ das 10-fache von
> Chirp 3 HD) **bewusst herausgelassen**. Bestehende `ElevenLabsTtsPlayer.kt` darf bleiben, ist
> aber nicht mehr der empfohlene Pfad. Empfehlung: **Edge-TTS (frei) als Default**,
> **Google Chirp 3 HD** als Premium-Option, **Android-native TTS** als Offline-Fallback.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Default-Stimme deutsch, kostenlos | Edge-TTS `de-DE-KatjaNeural`/`de-DE-ConradNeural`; Bibliothek/Token aktuell halten | §1, §3 |
| 2 | Premium deutsch, natürlichste Stimme | Google `de-DE-Chirp3-HD-<Name>` (z. B. `Kore`, `Charon`); Free-Tier 1 Mio. Zeichen/Monat | §2, §4 |
| 3 | Edge plötzlich 403 / „No audio received" | Bibliothek updaten (Sec-MS-GEC), Systemuhr per NTP genau, **keine** Datacenter-IP | §3, Bugs B1–B4 |
| 4 | Chirp 3 HD Streaming, aber MP3 gewünscht | Streaming liefert **kein MP3** → OGG_OPUS/PCM nehmen, oder Batch `text:synthesize` für MP3 | §4, Bugs G1 |
| 5 | SSML an Chirp 3 HD | Nur **synchron** (Preview), **nicht** im Streaming; Pausen via `markup`-Feld `[pause]` | §4, Bugs G2 |
| 6 | Langer Text (Briefing/Wochenrückblick) | Pro Absatz/Satz chunken; Google-Limit **5.000 Bytes**/Request, dt. Umlaute = 2 Bytes | §4, §6, Bugs G3 |
| 7 | Latenz beim ersten Ton wichtig | Streaming-Synthese nutzen; sonst pro Absatz vorab erzeugen + Pipeline (wie `speakAbsatzAt`) | §5 |
| 8 | Offline / Cloud fällt aus | Android-native `TextToSpeech` als Fallback, `de_DE`-Verfügbarkeit + Daten prüfen | §7 |
| 9 | Gleiche Texte wiederholt (Briefing) | Audio **cachen**: Key = hash(provider+model+voice+settings+text), MP3/OGG in cacheDir, LRU | §8 |
| 10 | Fehler 429/5xx/Netz | Exponentielles Backoff + Jitter, `Retry-After` lesen; 4xx (INVALID_ARGUMENT) **nicht** retryen | §9 |
| 11 | API-Key Google | Niemals in URL/Repo; verschlüsselt (EncryptedSharedPreferences), besser OAuth/Service-Account | §10 |
| 12 | Provider-Wahl im UI | Eine Quelle der Wahrheit (selektierter Provider), klarer Fallback-Pfad, kein stilles Mischen | §11 |

---

## 1) Edge-TTS — was es ist, Endpunkt, Auth

`offiziell` (Microsoft-Endpunkt, inoffiziell konsumiert) · `extern` (Bibliothek)

- **Was:** Microsoft Edge „Read-Aloud"-Dienst (Azure-Neural-Stimmen) — **kostenlos, ohne API-Key**.
  Genutzt entweder direkt per WebSocket (so im Android-Code: `wss://speech.platform.bing.com/...`)
  oder über die Python-Bibliothek `rany2/edge-tts` (7.2.8, 22.03.2026) bzw. JS-Ports.
- **WebSocket-Endpunkt (Stand 06/2026):**
  `wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4`
  — `TrustedClientToken` ist eine **fest verdrahtete Konstante** des Edge-Browsers.
- **Auth-Token `Sec-MS-GEC` (seit August 2024 PFLICHT):** Query-Parameter `Sec-MS-GEC` +
  `Sec-MS-GEC-Version`. Der Wert ist ein **SHA-256-Hash** aus „aktueller Windows-Datei-Zeit,
  auf 5 Minuten gerundet" **+** dem TrustedClientToken-String. `Sec-MS-GEC-Version` hat das
  Format `1-<Chromium-Vollversion>` (z. B. `1-143.0.3650.75`). **Der Token rotiert alle ~5 min**
  und ist von der **Systemuhr** abhängig → Uhr muss genau sein (sonst 403).
- **ToS-Hinweis:** Inoffizielle Nutzung des Read-Aloud-Dienstes. Für ein privates Tool (Frank)
  praktikabel; in einem öffentlich vertriebenen Produkt rechtlich grau — bewusst entscheiden.
- **Quellen:** https://github.com/rany2/edge-tts (extern, 22.03.2026) ·
  https://github.com/rany2/edge-tts/issues/290 (Sec-MS-GEC/403, extern) ·
  https://learn.microsoft.com/en-us/answers/questions/5653188 („No audio received" dt. Stimmen, offiziell-Forum, 2026).

## 2) Google Chirp 3 HD — was es ist, Endpunkt, Auth

`offiziell`

- **Was:** Neueste generative TTS-Generation von Google Cloud (AudioML), sehr natürlich,
  emotionaler Tonfall, **Low-Latency-Streaming**. **de-DE ist GA** (allgemein verfügbar).
- **REST-Endpunkte (API v1, Host `https://texttospeech.googleapis.com`):**
  - Batch/synchron: `POST /v1/text:synthesize`
  - Streaming (bidirektional, gRPC; im Android-Retrofit-Stack i. d. R. **nicht** genutzt → Batch): `streamingSynthesize`
  - Stimmen auflisten: `GET /v1/voices`
- **Stimmen-Namensschema:** `<locale>-Chirp3-HD-<Stimme>`, z. B. `de-DE-Chirp3-HD-Kore`.
- **Auth:** OAuth2 / Application Default Credentials (Service-Account) **bevorzugt**; API-Key
  möglich, aber riskanter (siehe §10).
- **Regionen:** `global`, `us`, `eu`, `asia-southeast1`, `asia-northeast1`, `europe-west2` (alle GA).
  Für DSGVO-Nähe `eu` oder `europe-west2` per regionalem Endpunkt erwägen.
- **Quellen:** https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (offiziell, 09.06.2026) ·
  https://docs.cloud.google.com/text-to-speech/quotas (offiziell, 01.06.2026).

---

## 3) Edge-TTS — Streaming, Latenz, Kosten, Rate-Limits, Fehler

- **Streaming vs. Batch:** Der Dienst **streamt** Audio-Frames (`audio-24khz-48kbitrate-mono-mp3`
  Standard) plus optionale `WordBoundary`-Metadaten über den WebSocket. Für die Vorlese-Funktion:
  pro Absatz einen Request, Audio während des Empfangs schon abspielen → niedrige gefühlte Latenz.
- **Latenz:** typ. einige hundert ms bis ~1 s bis zum ersten Frame (netzabhängig). Gut genug für
  Absatz-Pipeline; sehr kurze Schnipsel lohnen das Cachen (§8).
- **Kosten:** **kostenlos** (kein Konto, kein Key). Genau das ist Franks Default-Wunsch.
- **Rate-Limits:** Kein offizielles Limit. **Aber:** Microsoft filtert **Datacenter-/Cloud-IPs**
  (Anti-Abuse) → von Servern/CI/Colab kommt „No audio received". **Vom Handy/Heim-IP funktioniert
  es.** Nicht zu aggressiv parallelisieren (sonst temporäre Blockade) — 1–2 gleichzeitige Streams.
- **SSML / Prosody:** **Kein** beliebiges SSML. Der Dienst erlaubt nur **ein** `<voice>` mit
  **einem** `<prosody>` darin. Steuerbar sind nur **rate / volume / pitch** (z. B. `rate=-10%`,
  `pitch=-2Hz`). Die Bibliothek baut das SSML selbst — eigenes SSML wird **abgelehnt**.
- **Deutsche Stimmen (verifiziert, im Projekt genutzt):**

  | Voice-ID | Name | Typ |
  |----------|------|-----|
  | `de-DE-KatjaNeural` | Katja (w, warm) | Standard-Neural |
  | `de-DE-ConradNeural` | Conrad (m, klar) | Standard-Neural |
  | `de-DE-AmalaNeural` | Amala (w, jung) | Standard-Neural |
  | `de-DE-KillianNeural` | Killian (m, warm) | Standard-Neural |
  | `de-DE-FlorianMultilingualNeural` | Florian (m) | Multilingual |
  | `de-DE-SeraphinaMultilingualNeural` | Seraphina (w) | Multilingual |

  Die **Multilingual**-Stimmen lesen deutsche **und** fremdsprachige Passagen sauber — ideal,
  wenn Texte englische Fachbegriffe enthalten (Franks Recherche-/Tech-Texte).
- **Fehlerbilder kurz** (Details → Bug-Almanach): `403` = Sec-MS-GEC/Uhr/Version veraltet →
  Bibliothek updaten + Uhr per NTP. `No audio received` = Datacenter-IP oder leerer/punktuierter
  Text → echten Text vom Heim-IP senden. `Invalid SSML` = eigenes SSML → nur rate/volume/pitch.

## 4) Google Chirp 3 HD — Streaming, SSML, Preise, Limits

- **Online (Batch) vs. Streaming:**
  - **Batch** `text:synthesize` → ganze Audiodatei. **Ausgabeformate:** ALAW, MULAW, **MP3**,
    OGG_OPUS, PCM (Default LINEAR16). Für Android am einfachsten **MP3** oder OGG_OPUS.
  - **Streaming** `streamingSynthesize` → niedrigste Latenz, aber **Ausgabeformate nur** ALAW,
    MULAW, OGG_OPUS, **PCM — KEIN MP3**. Außerdem **kein SSML** im Streaming.
- **SSML (Stand 06/2026, Preview):** Chirp 3: HD unterstützt **jetzt** SSML — **aber nur für
  synchrone Requests**, nicht im Streaming. Unterstützte Tags: `<speak> <say-as> <p> <s>
  <phoneme> <sub> <break> <audio> <prosody> <voice>`. Nicht gelistete Tags werden **ignoriert**.
  > **Wichtig:** Das ist neu — ältere Doku/Tutorials behaupten „Chirp 3 HD kann kein SSML".
  > Beim Cachen die SSML-Variante mit in den Key aufnehmen (sonst alter Plain-Text-Cache-Treffer).
- **Voice-Controls (statt SSML, robuster):**
  - **Tempo:** `speaking_rate` 0.25–2.0 (1.0 = normal). de-DE unterstützt das.
  - **Pausen:** über das **`markup`-Feld** (nicht `text`!) mit `[pause short]`, `[pause]`,
    `[pause long]`. de-DE ist **nicht** in der Ausschlussliste → funktioniert.
  - **Aussprache:** `custom_pronunciations` per IPA/X-SAMPA. de-DE unterstützt das.
  - **Pitch:** Chirp 3 HD bietet **keine** freie Tonhöhensteuerung wie ältere Stimmen — nur Tempo.
- **Preise (Stand 06/2026):** Chirp 3: HD **$30 / 1 Mio. Zeichen**, **Free-Tier 0–1 Mio.
  Zeichen/Monat** (für Franks Vorlese-Mengen real meist kostenlos). Zum Vergleich:
  WaveNet/Standard $4/Mio (4 Mio frei), Neural2 $16/Mio (1 Mio frei), Studio $160/Mio,
  ElevenLabs Flash v2.5 ~$300/Mio (Grund für das Weglassen).
- **Limits/Quoten:** **5.000 Bytes pro Request** (Content-Limit, **nicht** erhöhbar) — dt.
  Umlaute/ß zählen als **2 Bytes** → früher am Limit. `Chirp3RequestsPerMinutePerProject` = **200**,
  `ConcurrentStreamingSessionsPerProject` = **100**. Requests-Limit erhöhbar, Content-Limit nicht.
- **Deutsche Chirp-3-HD-Stimmen:** Schema `de-DE-Chirp3-HD-<Name>` mit den 30 Standard-Stimmen
  (Achernar, Achird, Algenib, Algieba, Alnilam, Aoede, Autonoe, Callirrhoe, Charon, Despina,
  Enceladus, Erinome, Fenrir, Gacrux, Iapetus, **Kore**, Laomedeia, Leda, Orus, Pulcherrima,
  Puck, Rasalgethi, Sadachbia, Sadaltager, Schedar, Sulafat, Umbriel, Vindemiatrix, Zephyr,
  Zubenelgenubi). **Empfehlung dt. Default:** `de-DE-Chirp3-HD-Kore` (w) oder `-Charon` (m).
  > **Stimmenliste nie hart annehmen** — zur Laufzeit `GET /v1/voices?languageCode=de-DE`
  > abrufen und cachen; Google ergänzt Stimmen. Hartkodierte Liste nur als Fallback.

## 5) Latenz-Strategie (beide Provider)

- **Absatz-Pipeline** (im Projekt bereits via `speakAbsatzAt` + `currentSequenceId`): Text in
  Absätze/Sätze schneiden, **nächsten Absatz vorab erzeugen**, während der aktuelle spielt. Senkt
  die wahrgenommene Latenz drastisch und umgeht Längen-Limits.
- **Sequenz-Guard beibehalten:** Jede neue `speak()`-Sequenz erhöht die ID; laufende Erzeugungen
  vergleichen ihre erfasste ID — bei Stopp/Neustart abbrechen statt den nächsten Absatz zu starten
  (verhindert „Geister-Audio" nach Stop). Das ist genau das richtige Muster.
- **Streaming bevorzugen, wenn verfügbar:** Chirp 3 HD `streamingSynthesize` für „erster Ton
  schnell"; Edge streamt ohnehin. Wenn nur Batch nötig (MP3-Wunsch), dann eng chunken.

## 6) Text-Aufbereitung vor dem Senden

- **Chunking:** Immer an Satz-/Absatzgrenzen schneiden, nie mitten im Wort. Ziel: Chirp ≤ ~4.000
  Bytes Sicherheitsmarge (wegen 2-Byte-Umlauten), Edge-Absätze handlich (sehr lange Strings können
  den WebSocket hängen lassen).
- **Bereinigen:** Markdown/Emoji/Steuerzeichen entfernen, mehrfaches Whitespace normalisieren,
  „leere" oder reine Satzzeichen-Chunks **überspringen** (sonst Edge „No audio received").
- **Pausen/Betonung:** Bei Chirp natürliche Interpunktion (Punkt, Komma, Ellipse „…") nutzen —
  laut Google-Doku der wirksamste Hebel; zusätzlich `markup [pause long]` für gezielte Pausen.

## 7) Offline-Fallback: Android-native TextToSpeech

`offiziell` (Android-Platform)

- **Wann:** Kein Netz, Cloud-Fehler (Edge 403 / Google 429/5xx), oder Nutzer wählt „Offline".
- **Init:** `TextToSpeech(context, OnInitListener)`; im Callback `status == TextToSpeech.SUCCESS`
  prüfen. **Sprache:** `setLanguage(Locale.GERMAN)` → Rückgabe prüfen auf `LANG_MISSING_DATA`
  (Sprachdaten fehlen → `ACTION_INSTALL_TTS_DATA` anbieten) bzw. `LANG_NOT_SUPPORTED`.
- **Engine-Wahl ist nicht garantiert:** Auch wenn man eine Engine vorgibt, lädt das System die
  Default-Engine, wenn die gewünschte fehlt/deaktiviert ist. Also nie blind annehmen, dass eine
  bestimmte (z. B. Google-)Engine läuft — `getEngines()` prüfen, `isLanguageAvailable` testen.
- **Fortschritt/Abschluss:** `UtteranceProgressListener` (onStart/onDone/onError) für die
  gleiche „nächster Absatz"-Pipeline wie bei Cloud.
- **Offline-Caching:** `synthesizeToFile(...)` erzeugt eine WAV/Datei lokal — für Offline-Briefings
  einmal erzeugen, wiederverwenden.
- **Qualität:** Deutlich roboterhafter als Chirp/Edge → bewusst nur als Sicherheitsnetz, klar im
  UI kennzeichnen („Offline-Stimme").
- **Quellen:** https://developer.android.com/reference/android/speech/tts/TextToSpeech (offiziell) ·
  https://developer.android.com/reference/android/speech/tts/TextToSpeech.OnInitListener (offiziell).

## 8) Caching generierter Audios

- **Cache-Key:** stabiler Hash über **alle** ausgabe-relevanten Felder:
  `sha256(provider | model | voiceId | speakingRate | rate/volume/pitch | ssml? | normalisierterText)`.
  Fehlt ein Feld im Key → falsche Treffer (z. B. neue Stimme liefert alte Datei).
- **Ablage:** `cacheDir` (flüchtig, Edge/kurzlebig) oder `filesDir` (persistente Briefings);
  Format wie geliefert (Edge=MP3, Chirp=MP3/OGG). **LRU-Eviction** mit Größen-/Alterslimit.
- **Nutzen:** Spart bei Chirp echte Zeichen-Kosten (Free-Tier schonen) und senkt Latenz auf ~0;
  bei Edge vermeidet es unnötige Requests (Anti-Abuse-Risiko ↓). Tagesbriefing/Wochenrückblick mit
  identischem Text = idealer Cache-Kandidat.
- **Hygiene:** Cache beim Stimmen-/Provider-Wechsel **nicht** löschen (Key trennt das schon),
  aber bei App-Update mit geändertem Aufbereitungs-Algorithmus Cache-Version hochzählen.

## 9) Fehler- & Retry-Handling (gilt zusätzlich zu `api-integration-general.md`)

- **Retry nur bei transient:** 429, 500/502/503, Netz-Timeouts, Edge-`No audio received`
  (1× neu, dann Fallback). **Nicht** retryen bei 400/INVALID_ARGUMENT, 401/403-Auth,
  Längen-Limit — das wird durch Wiederholen nicht besser.
- **Backoff:** exponentiell + **Jitter**; bei Google `Retry-After`/Quota-Header respektieren;
  bei Edge-403 zuerst **Token/Uhr** erneuern statt stumpf wiederholen.
- **Fallback-Kette (Empfehlung):** gewählter Cloud-Provider → (1× Retry) → **anderer Cloud-Provider
  optional** → **Android-native TTS** → klare Fehlermeldung im UI. Jede Stufe sichtbar loggen.
- **Timeouts:** wie im Code getrennt setzen (connect ~15 s, read ~60 s für lange Synthese).
- **Idempotenz/Abbruch:** Sequenz-ID-Guard (siehe §5) verhindert Doppelausgabe nach Stop.

## 10) Sicherheit / Secrets

- **Google-API-Key NIE in der URL** (`?key=...` landet in Logs/Proxies) und **nie im Repo**.
  Im Projekt korrekt: `EncryptedSharedPreferences` (AES256). Besser noch: OAuth/Service-Account,
  Key serverseitig halten. Key auf TTS-API **einschränken** (API-Restriction in der Cloud-Console).
- **Edge-TTS:** kein Secret nötig (Pluspunkt) — aber der TrustedClientToken ist öffentlich bekannt,
  kein Geheimnis-Schutz nötig.
- **Kein PII roh loggen:** Vorgelesene Tagebuch-/Journal-Texte sind sensibel → Text **nicht** in
  Klartext-Logs schreiben (höchstens Länge/Hash). Passt zur Observability-Regel.

## 11) Provider-Auswahl & Architektur

- **Eine Quelle der Wahrheit:** Der im Setting gewählte Provider ist der aktive (so im
  `TtsManager`: „No fallback between providers — the selected one is the only one used"). Für die
  **Resilienz** empfiehlt sich dennoch ein **bewusster** Offline-Fallback (Android-native) als
  letzte Stufe, klar getrennt vom Premium-Pfad — kein stilles Mischen von Cloud-Stimmen.
- **Empfohlene Default-Konfiguration für Frank (deutsch, günstig):**
  1. **Edge-TTS** `de-DE-SeraphinaMultilingualNeural` / `de-DE-ConradNeural` — kostenlos, gut, Default.
  2. **Google Chirp 3 HD** `de-DE-Chirp3-HD-Kore` — Premium für „Genie-Antworten"/Wochenrückblick,
     im Free-Tier real meist 0 €.
  3. **Android-native** `de_DE` — Offline-Sicherheitsnetz.
- **Stimmen-IDs zentral halten** (wie `GoogleTtsVoices.kt` / `EdgeTtsVoices`): eine Datei pro
  Provider, zur Laufzeit per `/voices` (Google) bzw. `--list-voices` (Edge) aktualisierbar.

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/apis/tts-provider.md`) |
|-------------------------|------------------------------------------------------|
| §1, §3 (Edge Endpunkt/Auth/Streaming) | E1–E9 (Sec-MS-GEC/Clock-Skew/Datacenter-IP/UA/Origin/MUID/Rate-Limit/Frame-Parsing) |
| §3, §5, §6 (Edge Stimmen/SSML/Text) | ET1–ET11 (Custom-SSML/Styles/Multilingual/Syntax/lange Texte/Escaping/UTF-8/Voice-ID/MP3-Knackser) |
| §2, §4 (Chirp Endpunkt/Format/SSML/Limit) | G1–G14 (Streaming-MP3/SSML-sync/5000-Byte/markup-Pause/Locale/pitch/Base64/Long-Audio/Modellfamilien) |
| §2, §4, §9, §10 (Google Auth/Limits/Retry/Secrets) | GA1–GA21 (Key-Leak/Restriktion/ADC/OAuth/Quota 200 RPM/Free-Tier/DSGVO/Region/languageCode) |
| §7 (Offline-Fallback) | N1–N14 (Init-Race/setLanguage/Samsung-Engine/UtteranceListener/Doze/Offline-Stimme) |
| §5, §8, §9, §11 (Pipeline/Caching/Retry/Architektur) | AC1–AC19 (MediaPlayer-State/Geister-Audio/Coroutine-Cancel/atomares Schreiben/Cache-Key/EncryptedPrefs/OkHttp-WS) |
| §3, §5, §10, §11 (Edge im Browser/Latenz/Secrets/Architektur) | W1–W14 (SW-Lifecycle/Offscreen-Doc/Keepalive/AudioContext/decodeAudioData/crbug-1285664/Key-im-Bundle/CSP) |
| §8/§9 (Caching/Retry) | C1 (Retry bei 4xx zwecklos), C2 (Fallback-Kette) |

---

## Quellen (mit Datum & Flag)

- `offiziell` Google — Chirp 3: HD Doku (Stimmen, Sprachen, Streaming, SSML-Preview, Voice-Controls):
  https://docs.cloud.google.com/text-to-speech/docs/chirp3-hd (09.06.2026)
- `offiziell` Google — Quotas & Limits (5.000 Bytes, 200 RPM Chirp3, 100 Streams):
  https://docs.cloud.google.com/text-to-speech/quotas (01.06.2026)
- `offiziell` Google — Pricing (Chirp 3 HD $30/Mio, Free-Tier 1 Mio):
  https://cloud.google.com/text-to-speech/pricing
- `offiziell` Google — Supported voices and languages:
  https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types
- `offiziell` Android — TextToSpeech / OnInitListener (Fallback, Sprachverfügbarkeit):
  https://developer.android.com/reference/android/speech/tts/TextToSpeech
- `extern` rany2/edge-tts — Bibliothek, Endpunkt, rate/volume/pitch, kein Custom-SSML (7.2.8, 22.03.2026):
  https://github.com/rany2/edge-tts
- `extern` edge-tts Issue #290 — Sec-MS-GEC/403, Token-Mechanik:
  https://github.com/rany2/edge-tts/issues/290
- `offiziell-Forum` Microsoft Q&A — „No audio received" für dt. Edge-Stimmen (Datacenter-IP-Filter):
  https://learn.microsoft.com/en-us/answers/questions/5653188

> **Checkpoint:** Vollständig recherchiert für Edge-TTS + Google Chirp 3 HD (de-DE-Fokus).
> ElevenLabs bewusst ausgelassen (Kosten). Nächste sinnvolle Erweiterung (nicht jetzt nötig):
> `vorlese-overlay-v2`-spezifische Web-Audio-/Offscreen-Eigenheiten (Chrome MV3) — separater Lauf.

## 12) Lokales TTS/STT in einer Verkaufs-App (Offline, Lizenzen) — Stand 27.09.2026 19:03 (Recherche Genialer Wecker Android, Engine C Sonnet-Schwarm)

**TTS (DE/EN/FR/ES, gerätelokal):**
- **Android-System-TTS** (`TextToSpeech`) ist der lizenzsaubere Standardpfad. Je Sprache eine Stimme mit `!isNetworkConnectionRequired()` wählen, die Weckansage **vorab in eine Datei synthetisieren** (`synthesizeToFile`) und beim Wecken nur abspielen. So hängt das Wecken nicht von Init oder Netz ab. Die Qualität hängt vom Gerät ab. `offiziell` https://developer.android.com/reference/android/speech/tts/TextToSpeech
- **sherpa-onnx** (Apache-2.0, v1.13.8) mit Piper-/Kokoro-Modellen bindet **espeak-ng (GPL-3.0) statisch** ein. Die GPL-Freiheit ist erst für sherpa-onnx 2.0.0 geplant (Issue #3731). Für eine Closed-Source-App gibt es drei Wege:
  - (a) Piper als **separate TTS-Engine-App** (anderer Prozess, Zugriff über die System-TTS-API)
  - (b) App unter GPL-3.0 veröffentlichen
  - (c) auf sherpa-onnx 2.0 warten
  - Eigenes Lexikon bzw. Vorab-Phonemisierung ist aufwendig.

  `extern` https://github.com/k2-fsa/sherpa-onnx/issues/3731
- **Piper-Stimmen: die Lizenz jeder Stimme einzeln prüfen** (MODEL_CARD auf huggingface.co/rhasspy/piper-voices).

  | Sprache | Kommerziell brauchbar | Meiden |
  |---|---|---|
  | DE | `thorsten` (CC0) | – |
  | EN | `kristin` (MIT), `libritts_r` (CC-BY 4.0) | `lessac` (Blizzard-Noncommercial), `amy` (Lizenz unklar) |
  | FR | `siwis` (CC-BY 4.0) | `tom` (AGPLv3) |
  | ES | `davefx` (CC0), `sharvard` (CC-BY 3.0) | – |

  Das Piper-Projekt liegt jetzt bei OHF-Voice/piper1-gpl unter GPL-3.0; das betrifft den Code, nicht die Modell-Lizenzen. `extern`
- **Kokoro-82M** kann **kein Deutsch** und hat auf Mittelklasse-Geräten RTF > 1. **MMS-TTS** steht unter CC-BY-NC und scheidet damit aus. `extern`

**STT (Diktat 1–60 s):**
- Zuerst `SpeechRecognizer.isOnDeviceRecognitionAvailable()` prüfen, dann `createOnDeviceSpeechRecognizer()` (API 31+). Immer einen Fallback bereithalten. `offiziell`
- **whisper.cpp** (Code MIT, Modelle MIT) als nachladbares Qualitätsmodell. q5_1 ist der beste Kompromiss:

  | Modell | Größe q5_1 |
  |---|---|
  | tiny | 32 MB |
  | base | 60 MB |
  | small | 190 MB |

  Multilingual, mit Satzzeichen, Batch-Verarbeitung. Es gibt kein offizielles AAR: Der NDK/CMake-Build ist fällig, und **Pfade mit Leerzeichen** sind dabei riskant. `extern` https://github.com/ggml-org/whisper.cpp
- Vosk (Apache-2.0, ca. 40 MB pro Sprache) ist ungenauer und setzt keine Satzzeichen, daher nur als Notlösung. `extern`

## 13) Microsoft-Edge-Stimmen in einer Verkaufs-App — Stand 27.09.2026 19:03 (Recherche Genialer Wecker Android, Engine C Sonnet-Schwarm)
- Der Read-Aloud-Endpunkt (`speech.platform.bing.com`, genutzt von edge-tts) ist **inoffiziell** und per Reverse Engineering erschlossen. Microsoft sagt dazu selbst: Kommerzielle Nutzung ohne bezahltes Azure-Abo kann die Nutzungsbedingungen verletzen. Der Sec-MS-GEC-Token bricht wiederholt (Okt. 2024 bis Jan. 2026, 403). `extern` https://github.com/rany2/edge-tts/issues/290
- **Rechtssichere Alternative:** Azure AI Speech Neural (Paid, ca. 16 USD pro 1 Mio. Zeichen, HD 22 USD).
  - Kommerzielle Nutzungsrechte an der Audioausgabe gibt es nur im **Paid Tier**, nicht in F0.
  - Der Key gehört nie in die APK; der Zugriff läuft über einen Token-Broker auf dem eigenen Server.

  `offiziell` https://learn.microsoft.com/en-us/answers/questions/5805156/
- Wenn Edge überhaupt angeboten wird: nur als Option, sichtbar als **online und inoffiziell** gekennzeichnet, mit Einwilligungsdialog vor dem ersten Versand, nie als Vorgabe. Immer mit lokalem Fallback; die Ansage wird vorab in eine Datei synthetisiert.

## 14) Whisper offline in einer Verkaufs-App (ohne GPL) — Stand 27.09.2026 22:09 (GenialerWeckerAndroid)
- sherpa-onnx lässt sich für Android **nur Spracherkennung** bauen: `SHERPA_ONNX_ENABLE_TTS=OFF`, `SPEAKER_DIARIZATION=OFF`, `C_API=OFF`, `JNI=ON`; dann ist kein espeak-ng (GPL-3.0) enthalten. Damit ist Weg (c) aus §12 (auf 2.0 warten) für STT nicht nötig. Bauanleitung + Skript: `GenialerWeckerAndroid/docs/sherpa-onnx-asr-only/`. v1.13.8, onnxruntime 1.28.2, NDK r28c → 16-KB-Seiten ok.
- Das fertige Release-Paket `sherpa-onnx-v1.13.8-android.tar.bz2` enthält espeak/piper → für Closed Source ungeeignet (auch wenn man nur ASR nutzt).
- Modell fest in der APK statt Download: `whisper-small` int8 (Encoder 112 MB, Decoder 262 MB, Tokens 0,8 MB) als Asset, `androidResources.noCompress += "onnx"`, geladen per `OfflineRecognizer(assets, config)`. Gradle-Task lädt die Dateien beim Bau von Hugging Face (csukuangfj/sherpa-onnx-whisper-small), nicht ins Git. Für Google Play später als **install-time Asset-Pack** (Basismodul max. 200 MB); Zugriff bleibt über den AssetManager gleich.
- Whisper-Halluzinationen bei Stille ("Untertitel im Auftrag des ZDF …"): vor der Erkennung Sprachanteil messen (< 400 ms → nichts senden), Ränder kürzen, Floskel-Liste auch am Textende verwerfen.


## 15) Edge-Qualität lokal auf Android: Lagebild und Entscheidungsweg — Stand 28.09.2026 09:57 (Recherche „hochwertiges lokales TTS“, Engine C Sonnet-Schwarm, 8 Researcher)

Rohberichte mit allen Quellen: `Recherchen/tts-lokal-android/1-…8-*.md`. Versions-Anker: sherpa-onnx v1.13.8 (10.09.2026, noch keine 2.0), onnxruntime 1.28.2, NDK r28c.

**Kernaussage:** Rein lokal UND Edge-Niveau UND Deutsch UND closed-source-sauber gibt es 2026 als Fertigprodukt nicht. Keine erfolgreiche Play-Store-Vorlese-App schafft das. Die deutschen Arena-Spitzenplätze belegen nur Cloud-Modelle (Cartesia Sonic 3.6 ≈ 1283 Elo, Inworld TTS-2 ≈ 1261). `extern` Recherchen/…/1, 3, 7

**Was Verkaufs-Apps tatsächlich tun (4 Muster):** `extern` Recherchen/…/1-playstore-apps.md
1. **System-TTS als Gratis-Basis:** ReadEra, Librera, Moon+ Reader, OsmAnd, HERE WeGo.
2. **Lizenzierte klassische Offline-Stimmen per In-App-Kauf:** @Voice Aloud Reader, Voice Dream Reader (Acapela/Vocalizer/NeoSpeech, 2–5 $ pro Stimme). Offline und ToS-sauber, aber ältere Engine-Generation. https://www.acapela-group.com/about-us/customers/voice-dream-reader/
3. **„Bring your own API key“:** Der Nutzer trägt Cloud-Key und Kosten selbst (@Voice Aloud Reader).
4. **Cloud-Rendering + MP3-Download als Bezahlfunktion:** Speechify, ElevenReader, NaturalReader (Azure/Gemini/OpenAI). „Offline“ heißt dort: vorher gerenderte Datei.
   Sonderfälle: Duolingo und Babbel nutzen eigene Aufnahmen bzw. eigene Modelle. Blinkist hat KI-Sprecher zugunsten menschlicher Sprecher wieder abgeschafft.

**Entscheidungsweg für eine eigene Verkaufs-App (Empfehlung):**

| Stufe | Weg | Qualität DE | Lizenz/Kosten | Wann |
|---|---|---|---|---|
| 1 | **Fester Text → beim Bau vorab rendern** (Cloud, lizenziert, Paid Tier) und als Asset ausliefern | Spitze (Cloud) | einmalige Kosten | Ansagen, UI-Texte, feste Sätze |
| 2 | **Lokales Neural-Modell ohne espeak zur Laufzeit** (siehe §16) | gut, unter Edge | frei, eigener Integrationsaufwand | dynamischer Text, offline |
| 3 | **Lizenzierte Cloud über eigenen Token-Broker** + Cache (siehe §17) | Spitze | laufende Kosten, Abo-Modell nötig | Premium-Stimme, online |
| 4 | **System-TTS** mit bester Offline-Stimme (`getVoices()` nach `getQuality()` sortieren, `!isNetworkConnectionRequired()`) | gerätabhängig, schwach | frei | immer als letzter Fallback |

**Kommerzielle On-Device-SDKs (Indie-Realität):** `extern` Recherchen/…/2-kommerzielle-sdks.md
- **Azure Embedded Speech:** dieselbe Neural-Stimmfamilie wie Edge, auf dem Gerät. de-DE vorhanden, Java-AAR ab API 26. **Nur nach Limited-Access-Antrag**, Preis erst nach Freigabe; für Einzelentwickler ungewiss. Der Antrag kostet nichts außer Zeit. `offiziell` https://learn.microsoft.com/en-us/azure/ai-services/speech-service/embedded-speech
- **Picovoice Orca:** Self-Service, Android-SDK, deutsche Stimme. Test gratis über die Console, danach Foundation-Plan 6.000 $/Jahr (nur Start-ups) oder Enterprise-Angebot. Vor dem Kauf die deutsche Demo-Stimme anhören. `offiziell` https://picovoice.ai/pricing/
- **Acapela:** Android-SDK, Lizenz meist als **Prozent vom App-Verkaufspreis**, individueller Vertrag. Einziges B2B-Modell, das zu kleinen Verkaufs-Apps passt. `offiziell` https://www.acapela-group.com/solutions/acapela-tts-for-android/
- **Nur über den B2B-Vertrieb ohne öffentliche Preise:** ReadSpeaker, Cerence/Vocalizer, CereProc, Sensory.
- **Noch nicht zugänglich:** Cartesia On-Device (private Beta), ElevenLabs On-Device (Enterprise).

**Systemstimmen 2026:** `extern` Recherchen/…/6-systemstimmen.md
- Gemini Nano, AICore und ML Kit GenAI bieten **keine** TTS-API für Drittanbieter-Apps (nur STT).
- Samsung sperrt seit One UI 7/8 seine Neural-Stimmen für fremde Apps (siehe Bug L7).
- Nutzerführung zu einer besseren Engine ist möglich: `TextToSpeech(ctx, listener, enginePackage)` und `ACTION_INSTALL_TTS_DATA`, aber ohne Erfolgsgarantie. Eine vom Nutzer installierte Fremd-Engine per API zu nutzen, ist unproblematisch.

## 16) Lokales Neural-TTS ohne GPL: Kandidaten und G2P-Hebel — Stand 28.09.2026 09:57

**Prüfraster für jeden Kandidaten (Pflicht, jede Zelle belegt, sonst „unklar“):** Code-Lizenz · Gewichts-Lizenz kommerziell · **G2P-Kette ohne GPL** · Deutsch nativ · RTF < 1 auf Mittelklasse-ARM.

| Modell | Größe | Lizenz Code / Gewichte | G2P ohne espeak? | Deutsch | Handy-Tempo | Einschätzung |
|---|---|---|---|---|---|---|
| **Kyutai Pocket TTS** | 100M | MIT / **CC-BY-4.0** (Namensnennung) | **ja**, SentencePiece-Tokenizer | ja, seit 04/2026 (6 Sprachen) | ~1× Echtzeit auf Pixel 8a (Community-LiteRT-Port) | sauberste Lizenz, **Favorit zum Anhören**; knapp an der Echtzeitgrenze. In sherpa-onnx als Modelltyp enthalten, aber Klangabweichung (Bug L9) |
| **Supertonic 3** (Supertone) | 99M, 31 Sprachen | MIT / **OpenRAIL-M** (Nutzungsauflagen + Namensnennung, Closed Source erlaubt) | **ja**, eigenes Unicode-Frontend | ja | 5× Echtzeit auf 16-Thread-Server-CPU; keine Handy-Messung | schnell; Lizenztext prüfen; **Projekt wird archiviert** (kein Support nach 31.08.2026). F-Droid-Engine `com.brahmadeo.supertonic.tts` als Referenz |
| **Kokoro-82M-German-Martin** (kikiri-tts) | ~80 MB | Apache-2.0 / Daten CC0 (HUI) | nur mit **Lexikon-Muster** (siehe unten) | ja (Fine-Tune) | Kokoro-Klasse, Mittelklasse grenzwertig | beste deutsche Kokoro-Option. Variante unora-voices „Martin 1“ mit fertigem Lexikon läuft bereits auf Android |
| **Piper thorsten (high)** | ~60 MB | Apache/MIT / CC0 | nur mit Lexikon-Muster | ja | schnell (VITS) | deutsche Referenzstimme, hörbar unter Edge |
| NeuTTS Nano German | ~117M aktiv | Lizenz „other“ (unklar) | **nein**, espeak Pflicht | ja | unklar | ausgeschlossen, solange espeak nötig ist |
| Chatterbox (Multilingual) | groß | MIT / MIT | unklar | ja | nicht für Handys ausgelegt | eher Server |
| Qwen3-TTS 0.6B | ~2,3 GB RAM | Apache / Apache | ja (LLM-Tokenizer) | ja | zu schwer für die Mittelklasse | Server/Pre-Rendering |
| OuteTTS 1.0 0.6B | GGUF | MIT / MIT | ja | ja | unklar, LLM-Tempo | Wiederholungsschleifen (Bug L12) |
| Kitten TTS | 15M | Apache / Apache | nein (espeak) | **nein** (nur EN) | schnell | für Deutsch raus |
| Fish/OpenAudio S1-mini, F5-TTS-German (aihpi), MMS, pavoque, XTTS | – | **NC** | – | – | – | für Verkaufs-Apps raus |

Quellen: `Recherchen/tts-lokal-android/3-open-source-modelle.md`, `4-deutsch-g2p.md`. Wichtigste Links:
- https://github.com/kyutai-labs/pocket-tts
- https://github.com/supertone-inc/supertonic
- https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin
- https://github.com/georgwinter89-cloud/unora-voices

**Der G2P-Hebel: das Lexikon-Muster (unora-voices).** `extern`
- espeak-ng läuft **nur beim Bauen auf dem Entwickler-PC** und erzeugt für den Wortschatz eine Tabelle Wort → Phoneme im exakten Piper/Kokoro-Phonemsatz.
- In die APK kommt nur diese Datei, weder espeak-Code noch espeak-Binärdatei.
- Die Ausgabe eines GPL-Programms ist in der Regel kein abgeleitetes Werk. **Rechtlich plausibel, aber nicht anwaltlich geprüft.** Die espeak-Wörterbuchdaten selbst nie mitliefern.
- Die Wortliste darf keine CC-BY-SA-Quelle sein (Wiktionary: Share-Alike). unora nutzt Tatoeba und Leipzig-Corpora (CC BY 4.0), dafür ist eine Namensnennung nötig.
- Unbekannte Wörter (OOV) übernimmt ein MIT-G2P (CharsiuG2P ByT5-small, ~30 MB, oder DeepPhonemizer `latin_ipa_forward`). Dessen IPA muss per ONNX-Export und **Mapping auf den espeak-Phonemsatz** angeglichen werden: Das ist der eigentliche Aufwand.
- **Kein Ausweg:** Misaki `de.DEG2P` (ruft intern espeak auf, PR #317 nicht gemergt), OpenPhonemizer (nur EN, archiviert 15.03.2026), Sequitur (GPL-2.0).
- sherpa-onnx 2.0 soll `lexicon.txt` bzw. fertige Tokens (`GenerationConfig.tokens`) offiziell unterstützen (Issue #3731, offen seit 08.07.2026). Bis dahin ist der Weg nicht für alle Modelltypen dokumentiert.

**Deutsche Textnormalisierung (Pflicht vor jeder lokalen Synthese):** Zahlen, Datum, Uhrzeit, Euro, Einheiten, Ordinalzahlen und Abkürzungen (z. B., usw., Nr.) ausschreiben, bevor der Text ins Modell geht.
- GPL-frei: NVIDIA NeMo-text-processing (Apache-2.0, Deutsch, eher als Vorlage für eigene Kotlin-Regeln).
- `german_transliterate` als Referenz für Regeln.
- num2words steht unter LGPL, also vorsichtig.

**Integration Android (sherpa-onnx oder ONNX Runtime direkt):** `extern` Recherchen/…/5-integration-performance.md
- Satzweise synthetisieren, `generateWithCallback` + `AudioTrack.MODE_STREAM`, Samplerate aus dem Modell lesen.
- Beim Start 1–3 Warm-up-Inferenzen, alles außerhalb des Main-Threads.
- **Vorlage für eine eigene System-Engine:** `sherpa-onnx/android/SherpaOnnxTtsEngine/.../TtsEngine.kt` (`TextToSpeechService`).
- Beschleuniger: XNNPACK überall, NNAPI ab Snapdragon 8 Gen 1, Exynos 2200 und Tensor G2 (nicht unabhängig bestätigt), immer mit CPU-Fallback. **int8 ist bei Kokoro auf CPUs teils langsamer** als fp32 (Dequantisierung), deshalb selbst messen.
- Belastbare RTF-Werte von echten Handys fehlen: **eigener Benchmark auf dem Zielgerät ist Pflicht**, bevor ein Modell gewählt wird.
- Auslieferung: Das Basismodul darf höchstens 200 MB haben. Install-time-Asset-Packs dürfen zusammen höchstens 1 GB haben. Play for On-device AI (Beta) nennt offiziell nur LiteRT/MediaPipe, ONNX nur als generischer Asset. Die Alternative ist ein eigener Download **mit Checksumme** (siehe Bug L6).
- Vor dem Play-Upload die 16-KB-Ausrichtung der konkreten AAR prüfen (`zipalign -c -P 16` bzw. `check_elf_alignment.sh`).

## 17) Lizenzierte Cloud als Hybrid: Preise, Rechte, Pflichten — Stand 28.09.2026 09:57

Die Preise beziehen sich auf 1 Mio Zeichen, Stand 09/2026. Bei Entscheidungen erneut live prüfen. `extern` Recherchen/…/7-cloud-hybrid.md

| Anbieter / Modell | Preis | Anmerkung |
|---|---|---|
| Google Standard/WaveNet | ~4 $ | |
| Google Neural2 | 16 $ | |
| Google Chirp 3 HD | ~30 $ | 1 Mio Zeichen Free-Tier |
| Google Studio | ~160 $ | |
| Azure Neural | 16 $ | 0,5 Mio Free-Tier; F0 ohne kommerzielle Rechte |
| Azure Neural HD | 22 $ | seit 03/2026, vorher 30 $ |
| Amazon Polly Neural | 16 $ | |
| Amazon Polly Generative | 30 $ | |
| ElevenLabs Flash v2.5 | ~50 $ | kommerziell erst ab Paid-Plan |
| ElevenLabs Multilingual v2 / v3 | ~100 $ | |
| Inworld TTS-2 | 25 $ (Growth 12,50 $) | **DE-Arena Platz 2** |
| Inworld Flash | 15 $ | |
| Cartesia Sonic 3.6 | creditbasiert | **DE-Arena Platz 1** |
| Deepgram Aura-2 | 30 $ | Deutsch seit 2026 |
| Speechmatics | 11 $ | 1 Mio Zeichen Free-Tier |
| OpenAI gpt-4o-mini-tts | ≈ 15 $ | tokenbasiert |

- **Rechenbeispiel:** 1.000 Nutzer × 10.000 Zeichen im Monat = 10 Mio Zeichen. Das kostet 40 $ (Google Standard) bis 1.600 $ (Studio). Für eine Einmalkauf-App ist Live-Cloud deshalb nur mit Kontingent oder Abo tragbar.
- **Caching:** Google empfiehlt ausdrücklich „cache audio files by text hash“, und ElevenLabs erlaubt im Paid-Plan das Speichern und Ausliefern. Bei Azure ist die Caching-Klausel unklar; vor der Produktion die Product Terms lesen. Bei OpenAI gehört der API-Output dem Kunden (gilt nicht für ChatGPT Voice).
- **EU AI Act Art. 50 (seit 02.08.2026):** Täuschend echte KI-Stimmen muss man für Nutzer wahrnehmbar kennzeichnen, zum Beispiel mit einem Hinweis „KI-Stimme“ in der App. Das gilt auch für lokale Neural-Stimmen.
- **DSGVO:** Der Text geht an den Dienstleister. EU-Region wählen und das in der Datenschutzerklärung nennen.
- **Architektur:**
  - Den Key nie in die APK. Ein Cloudflare Worker oder eine Firebase Function dient als Token-Broker/Proxy, mit Rate-Limit pro Nutzer (Durable Objects).
  - Feste Texte beim Bau vorab rendern und als Asset ausliefern.
  - Dynamischen Text per Hash-Key cachen, lokal als Fallback.
- **edge-tts 2026:** keine belegten Abmahnungen oder Play-Store-Entfernungen. Seit 2026 bricht aber eine Header-Restriktion (`Sec-WebSocket-Version`) die Nutzung im Browser; serverseitige Clients laufen noch. Es bleibt eine Grauzone und ist **kein** Weg für eine Verkaufs-App (siehe §13).
