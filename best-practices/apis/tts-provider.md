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
