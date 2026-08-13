# Perfect Moment — Programmier-Spezifikation

**Auftrag an die programmierende KI: Baue die Android-App „Perfect Moment" exakt nach dem fertigen, verbindlichen Design-Muster und dieser Spezifikation.**

Stand: 19. Juli 2026 · Ziel: Android-App (Kotlin, Jetpack Compose) · Sprache der Oberfläche: Deutsch

---

## 0. Verbindliche Quellen

| Quelle | Pfad | Rolle |
|---|---|---|
| **Design-Muster (maßgeblich für ALLES Sichtbare)** | `~/proggs/Designs/Perfect Moment Design-Brief/Perfect Moment.dc.html` | Klickbares HTML-Muster aller Bildschirme, Zustände und Animationen. **Bei jedem optischen Zweifel gilt diese Datei — 1:1 umsetzen.** `support.js` daneben ist nur die Render-Runtime des Musters, kein App-Code. |
| Design-Brief (Hintergrund) | `~/proggs/PerfectMoment/01-DESIGN-BRIEF.md` | Erläuterungen zum Design. Bei Widerspruch gewinnt das HTML-Muster. |
| Referenz: TTS | `~/proggs/BestJournalFrank/app/src/main/java/com/entropyjournal/util/GoogleCloudTtsPlayer.kt`, `EdgeTtsPlayer.kt`, `TtsManager.kt`, `Constants.kt` | Google Chirp 3 HD + Edge TTS, funktionierend. |
| Referenz: TTS-Watchdog | `~/proggs/BestJournalAndroid/app/src/main/java/com/bestjournal/app/util/EdgeTtsPlayer.kt` | Watchdog gegen stumm hängende Edge-Sockets. |
| Referenz: ChatGPT/Codex | `~/proggs/KarteikartenLernen/app/src/main/java/de/frank/karteikartenlernen/auth/CodexAuthManager.kt` | Device-Code-Anmeldung + Responses-API + SSE, funktionierend. |
| Referenz: Spracheingabe + Halluzinations-Filter | `~/proggs/CortexAndroid/app/src/main/java/de/frank/cortex/audio/` (`MicRecorder.kt`, `SpeechAnalyzer.kt`, `WhisperHallucinationFilter.kt`) | **Neueste, gehärtete Version — diese übernehmen.** |
| Referenz: Filter-Unit-Tests | `~/proggs/KarteikartenLernen/app/src/test/java/de/frank/karteikartenlernen/transcription/WhisperHallucinationFilterTest.kt` | 9 Tests als Vertragsspezifikation — portieren. |

Regel: **Code aus den Referenzprojekten wird übernommen und angepasst, nicht neu erfunden.** Endpoints, Header, Schwellwerte und Protokolldetails dort sind erprobt.

---

## 1. Was die App tut (Kernablauf)

Frank wählt auf dem Startbildschirm ein Thema (Aufhänger-Karte, getippte oder gesprochene eigene Frage), stellt Pausen/Wiederholungen/Dauer ein und startet eine Sitzung. Die App schickt Thema + aktiven Skill an ChatGPT (Codex Responses-API) und erhält **30 offene, unbeantwortete Fragen** (je mit Emoji). Die Sitzung öffnet **stumm** mit allen Fragen als Liste. Tippt Frank den Lautsprecher an, liest eine TTS-Stimme die Fragen vor: jede Frage `reps`-mal mit `pauseRep` Sekunden Abstand, dann `pauseNext` Sekunden bis zur nächsten Frage. Ab dem Vorlesen von **Frage 20** eines Blocks holt die App im Hintergrund die nächsten 30 Fragen und hängt sie an. Der Sitzungs-Timer (10–120 min) beendet die Sitzung; die laufende Frage wird fertig gesprochen, dann Abschlussbild „Der perfekte Moment ist hier." Fragen werden **nie beantwortet** — es gibt während der Sitzung keinerlei Eingabe außer Lautsprecher, Scrollen und Stopp.

---

## 2. Projekt-Grundgerüst

- **Paketname:** `de.frank.perfectmoment` · App-Name: „Perfect Moment"
- **Sprache/UI:** Kotlin 2.x, Jetpack Compose (Material 3), Single-Activity
- **minSdk 26, targetSdk 36**, JVM 17 — wie BestJournalAndroid
- **Libraries** (Vorbild BestJournal/Karteikarten): OkHttp 4.12.0, Room (Verlauf), `org.json` für Payloads (kein Moshi nötig — Karteikarten-Stil), AndroidX Security-Crypto (`EncryptedSharedPreferences`), Coroutines. Kein Retrofit, kein Media3 — Audio über `android.media.MediaPlayer` wie in den Referenzen.
- **Repository:** Unterordner `~/proggs/PerfectMoment/app/` im bestehenden Repo `Pepsi1978/proggs`. **Kein neues GitHub-Repo.** Ausführliche `README.md` fürs Projekt.
- **Berechtigungen:** `INTERNET`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` (Laufzeitabfrage ab API 33).
- **Architektur:** MVVM. Ein `SessionEngine` (siehe §7) als eigenständige, testbare Klasse ohne UI-Bezug; ViewModels dünn.

---

## 3. Design-Umsetzung (1:1 aus dem HTML-Muster)

Das HTML-Muster definiert alles Sichtbare. Übertrag nach Compose:

### 3.1 Design-Tokens (aus dem Muster übernehmen, als Compose-Theme)

**Dunkel (Standard):** `bg #181209 · surface #251C10 · surface2 #332717 · gold #D4A24C · goldHi #F0C97A · goldDim #9A7C40 · amber #E8873B · t1 #F5EEE2 · t2 #B3A68F · t3 #786A57 · warn #C4634A · breath rgba(212,162,76,0.13)`

**Hell:** `bg #FBF6EC · surface #F3EAD9 · surface2 #EDE1CA · gold #A87A2A · goldHi #7A5518 · goldDim #C7AE7E · amber #C4661F · t1 #241D12 · t2 #6B5D48 · t3 #A2947C · warn #A33F28 · breath rgba(168,122,42,0.07)`

Zusätzlich als Token anlegen (im Muster hardcodiert): `success #6FA860` (Verbunden-Punkt).

**Schriften:** `Newsreader` 300/400 (Fragen, Zitate, Abschluss), `Inter` 400/500/600 (UI), `JetBrains Mono` 400/500 (Verbindungscode, API-Schlüssel, Skill-Editor, Paketname). Als Ressourcen-Fonts bündeln (Google Fonts herunterladen), nicht zur Laufzeit laden.

**Maße:** Fragen aktiv 32 px / übrige 20 px (Skalierung der aktiven Frage 1,15), Screen-Titel 26 px, Labels 13 px Großbuchstaben mit +0,8 px Laufweite, Karten 168×168 Radius 20, Bottom Sheets Radius 28 oben, Slider-Thumb 26 px in goldHi, Fortschrittsring 64 px (r = 30, Umfang 188,5).

**Erscheinungsbild:** Hell / Dunkel / Wie System — Segment in den Einstellungen, Standard Dunkel.

### 3.2 Animationen (Dauern aus dem Muster)

Atmender Hintergrund 20 s (Sitzung 30 s) · Screen-Einblendung 450–900 ms `cubic-bezier(0.2,0,0,1)` · Sitzung von unten 700 ms · Bottom Sheet 400 ms · Frage-Wechsel 700 ms `cubic-bezier(0.4,0,0.2,1)` · Lautsprecher-Umschalten 350 ms · Bernstein-Puls 2,4 s · Aufnahme-Ringe 1,6 s dreifach versetzt · Dimmung hin 4 s linear, zurück 300 ms · Start-Knopf-Glow 3,2 s + Shimmer 4,5 s. Bei System-Einstellung „Animationen entfernen": Endlos-Animationen aus, Übergänge auf 200 ms.

### 3.3 Bildschirme (Navigation wie im Muster)

`start` ⇄ `verlauf` (→ `verlaufDetail`) · `start` ⇄ `settings` (→ `hooks`/`hookEdit`, `skills`/`skillEdit`, `voice`, `cg`) · `start` → `session`. Alle deutschen Texte **wörtlich** aus dem Muster übernehmen (z. B. „Was möchtest du hören?", „Antippen zum Sprechen", „Keine Verbindung — die Sitzung wartet.", „Noch keine Sitzungen.", „Der perfekte Moment ist hier.").

**Zwei Besonderheiten aus dem finalen Design, die verbindlich sind:**

1. **Intro-Overlay der Sitzung („Die KI fragt zuerst"):** Beim Sitzungsstart erscheint zuerst ein Overlay mit dem Zitat *„Welche Frage hast du und welches Gefühl möchtest du damit verstärken?"* und dem Knopf **„Antworten & beginnen"** (Untertext: „Danach beginnt die Routine des Skills."). Verhalten: Antippen des Knopfs öffnet ein kurzes Eingabe-Sheet (Text ODER Mikrofon, gleiche Aufnahme-Mechanik wie Startbildschirm); die Antwort wird dem KI-Auftrag als Zusatzkontext mitgegeben (siehe §8.3). Das Overlay kann per Antippen außerhalb übersprungen werden — dann geht es ohne Zusatzkontext weiter.
2. **„Zufällige Reihenfolge"** existiert nur im Verlauf-Detail (Wiederabspielen): Toggle „Fragen werden gemischt abgespielt". Beim Neustart einer gespeicherten Sitzung mit aktivem Toggle wird die Fragenliste einmalig gemischt.

---

## 4. Persistenz

### 4.1 EncryptedSharedPreferences (Store `perfect_moment_secure_prefs`)

Wie BestJournalFrank (`MasterKeys.AES256_GCM_SPEC`, Schemes `AES256_SIV`/`AES256_GCM`, Lazy-Init mit Null-Fallback):

| Key | Inhalt | Standard |
|---|---|---|
| `tts_provider` | `edge_tts` / `google_cloud` | `edge_tts` |
| `edge_tts_voice` | Edge-Stimmname | `de-DE-SeraphinaMultilingualNeural` |
| `google_tts_api_key` | Google-Schlüssel | leer |
| `google_tts_voice` | Chirp-Stimmname | `de-DE-Chirp3-HD-Kore` |
| `groq_api_key` | Groq-Schlüssel (Spracheingabe) | leer |
| `pause_rep_seconds` | 1–30 | 8 |
| `pause_next_seconds` | 1–60 | 12 |
| `reps_per_question` | 1–10 | 3 |
| `session_duration_min` | 10/20/30/45/60/90/120 | 30 |
| `theme` | `light`/`dark`/`system` | `dark` |
| `active_skill_id` | ID des aktiven Skills | vorinstallierter Skill |
| `operating_mode_text` | Betriebsmodus-Anhang (editierbar) | siehe §8.2 |

Codex-Tokens separat im Store **`codex_oauth`** mit exakt den Keys aus KarteikartenLernen: `access_token`, `refresh_token`, `expires_at`, `account_id`, `email` (§8.1).

**Wichtig — anders als das Design-Muster andeutet:** Der Groq-Schlüssel kommt NICHT als `BuildConfig`-Konstante (so macht es Karteikarten), sondern als Eingabefeld in den Einstellungen (so zeigt es das Design: „Groq-API-Schlüssel · Whisper Large v3 · Spracheingabe", maskiert mit Augen-Toggle — wie das Google-Feld).

### 4.2 Room-Datenbank (Verlauf, Skills, Aufhänger)

```
SessionEntity(id, topic, startedAt, durationMin, voiceName, providerId,
              pauseRep, pauseNext, reps, questionCount)
QuestionEntity(id, sessionId→SessionEntity, orderIndex, emoji, text)
SkillEntity(id, name, text, createdAt)            // aktiv-Status via Pref
HookEntity(id, emoji, text, sortIndex)
```

Jede Sitzung wird **automatisch** beim Start angelegt und laufend ergänzt (jede neu geholte Frage sofort persistieren — Absturzsicherheit). Verlauf-Detail „Sitzung beginnen" startet eine neue Sitzung mit denselben Fragen **ohne KI-Anfrage** (optional gemischt); Wischen-links löscht (mit den im Muster gezeigten Löschen-Knopf).

### 4.3 Vorinstallierte Inhalte (erste Installation)

**Skill „Forschungsteam":** der vollständige 50-Experten-Skill-Text (liegt als Rohtext in dieser Datei unter §11). Aktiv gesetzt.

**Acht Aufhänger** (löschbar/änderbar): 🌅 Wie fühlt sich ein schönes Leben an? · 🕊️ Wie fühlt sich ein freies Leben an? · 💪 Wie fühlt sich ein fitter Körper an? · 🌙 Wie schaffe ich es, dass mein Schlaf immer tiefer wird? · ✨ Warum ist das Leben schön? · 🧭 Wie schaffe ich es, dass es mir immer gut geht? · 🌲 Was macht mich im Wald so ruhig? · 🔮 Wie fühlt sich ein Leben ohne Schmerzen an?

---

## 5. Spracheingabe (Groq Whisper + 4-Schichten-Halluzinationsfilter)

**Vorbild: CortexAndroid — dessen Version 1:1 portieren** (`MicRecorder.kt`, `SpeechAnalyzer.kt`, `WhisperHallucinationFilter.kt`). Die 9 Unit-Tests aus KarteikartenLernen mitportieren.

### 5.1 Aufnahme (`MicRecorder`)

`AudioRecord`, **16 000 Hz, Mono, PCM 16-bit**, AudioSource **`VOICE_RECOGNITION`** (Samsung-kompatibel — nicht `MIC`). PCM in-memory (`ByteArrayOutputStream`, Deckel ~10 min), `AtomicBoolean`-Schutz gegen Doppel-Tipp, `stop()` → 44-Byte-RIFF/WAVE-Header + PCM als `ByteArray`.

### 5.2 Transkription

`POST https://api.groq.com/openai/v1/audio/transcriptions` · multipart · `model=whisper-large-v3-turbo` · `language=de` · `response_format=verbose_json` · `temperature=0` · Header `Authorization: Bearer <groq_api_key>` · Timeouts 20/60/90 s · max. 25 MB.

### 5.3 Die vier Filter-Schichten (Konstanten exakt so übernehmen)

1. **Vorfilter:** `SpeechAnalyzer` (WAV-Header 44 Bytes, Frame 20 ms, RMS-Schwelle 0,015, Sample-Rate aus Header-Bytes 24–27, Fallback 16 000). `voicedMs < 150` → gar nicht senden. Analyse fehlgeschlagen (`null`) → trotzdem senden (fail-open).
2. **Confidence-Gate** pro Segment (ODER-verknüpft): Stille = `no_speech_prob > 0.6` **UND** `avg_logprob < −1.0` · Repetition = `compression_ratio > 2.4` · Mini-Noise = Dauer `< 0.4 s` **UND** `no_speech_prob > 0.6`.
3. **Segment-Audio-Abgleich:** Segment-Zeitfenster gegen Voiced-Timeline, unter 10 % laute Frames (`minVoicedRatio = 0.10`) → verwerfen. **Drift-Sicherung:** würde Schicht 3 alles verwerfen, was Schicht 2 durchließ → Schicht-2-Ergebnis behalten.
4. **Floskel-Blocklist** — verwerfen nur bei allen drei Signalen gleichzeitig: Text ≤ 64 Zeichen und ≤ 8 Wörter · normalisierter **exakter** Match (lowercase, Nicht-Buchstaben → Leerzeichen, „Amara.org" → „amara org") · `voicedMs < 600`. Ohne Analyse nie verwerfen. Blocklist (20 Einträge, wörtlich):

```
vielen dank · vielen dank fürs zuschauen · vielen dank fuers zuschauen ·
vielen dank für eure aufmerksamkeit · vielen dank für ihre aufmerksamkeit ·
vielen dank für die aufmerksamkeit · bis zum nächsten mal · bis zum nächsten video ·
untertitel · untertitel des zdf · untertitelung des zdf für funk ·
untertitel im auftrag des zdf für funk · untertitel von stephanie geiges ·
untertitel der amara org community · der text ist nicht auf deutsch ·
thank you · thank you for watching · thanks for watching · please subscribe ·
subtitles by the amara org community
```

**Cortex-Härtungen übernehmen:** Logging jeder verworfenen Schicht; Nutzertext beim Loggen als `%s`-Argument übergeben (nie interpolieren — „100 %" im Text crashte sonst `format()`).

### 5.4 UI-Anbindung

Aufnahmeknopf-Zustände wie im Muster: idle („Antippen zum Sprechen") → rec (Bernstein, Ringe, „Ich höre zu…") → proc (Spinner, „Einen Moment…") → Transkript landet editierbar im Fragefenster. Leerer Filter-Ausgang → Feld bleibt leer, kurzer Hinweis „Ich habe nichts verstanden." in t3, kein Fehlerdialog.

---

## 6. Sprachausgabe (TTS)

Übernahme aus BestJournalFrank (`GoogleCloudTtsPlayer`, `EdgeTtsPlayer`, `TtsManager`-Muster) plus Watchdog aus BestJournalAndroid.

### 6.1 Google Chirp 3 HD

`POST https://texttospeech.googleapis.com/v1/text:synthesize?key=<google_tts_api_key>` · Body `{"input":{"text":…},"voice":{"languageCode":"de-DE","name":<voice>},"audioConfig":{"audioEncoding":"MP3"}}` · Antwort `audioContent` Base64 → MP3 in `cacheDir` → `MediaPlayer` → Datei nach Wiedergabe löschen. Timeouts 15/60 s. 30 Stimmen `de-DE-Chirp3-HD-<Name>`: weiblich Achernar, Aoede, Autonoe, Callirrhoe, Despina, Erinome, Gacrux, **Kore** (Standard), Laomedeia, Leda, Pulcherrima, Sulafat, Vindemiatrix, Zephyr · männlich Achird, Algenib, Algieba, Alnilam, Charon, Enceladus, Fenrir, Iapetus, Orus, Puck, Rasalgethi, Sadachbia, Sadaltager, Schedar, Umbriel, Zubenelgenubi.

### 6.2 Microsoft Edge TTS

Selbstgebautes edge-tts-Protokoll per OkHttp-WebSocket, exakt wie Referenz: `wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4&Sec-MS-GEC=<hash>&Sec-MS-GEC-Version=1-143.0.3650.75&ConnectionId=<uuid>`. `Sec-MS-GEC` = SHA-256-Uppercase-Hex über (auf 300 s abgerundete Unix-Zeit + `11644473600`) × 1e7 + Token. Edge-Browser-Header spoofen (User-Agent, `Origin: chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold`, Zufalls-`muid`-Cookie). Ablauf: Textframe `speech.config` (`audio-24khz-96kbitrate-mono-mp3`) → Textframe SSML (`<speak…><voice name='…'>Text</voice></speak>`, nur `& < >` escapen) → Binärframes (2 Bytes Big-Endian-Headerlänge, Audio ab `headerLen+2`) an Cache-MP3 anhängen → `turn.end` → `MediaPlayer`. 6 Stimmen: `de-DE-SeraphinaMultilingualNeural` (Standard), `de-DE-FlorianMultilingualNeural`, `de-DE-KatjaNeural`, `de-DE-KillianNeural`, `de-DE-ConradNeural`, `de-DE-AmalaNeural`.

**Watchdog (aus BestJournalAndroid):** 30 s Initial- und 30 s Idle-Timer, Reset bei jedem Audio-Chunk; feuert er, Socket schließen, UI freigeben, **weiter zur nächsten Wiederholung/Frage** (Sitzung friert nie ein). Eigener `CoroutineScope(SupervisorJob() + Dispatchers.Main)`.

### 6.3 TtsManager

Ein Manager hält beide Player, `speak(text, onStart, onComplete, onError)` routet nach `tts_provider`. Kein Fallback zwischen Anbietern (wie Referenz). **Emojis werden vor der TTS-Übergabe restlos entfernt** — gesprochen wird nur der reine Fragetext (§7.2). Probehör-Knopf in ⑤c spricht fest: *„Wie fühlt es sich an, dass es dir gut geht?"*

---

## 7. SessionEngine (Herzstück)

Eigenständige Klasse, per Unit-Test prüfbar, UI beobachtet `StateFlow`.

### 7.1 Zustand

```
data class SessionState(
  questions: List<Question>,        // wächst blockweise um je 30
  currentIndex: Int, currentRep: Int,
  phase: Phase,                     // IDLE_MUTED, SPEAKING, PAUSE_REP, PAUSE_NEXT,
                                    // WAITING_NETWORK, ENDED
  speakerOn: Boolean,               // Start: false (stumm!)
  remainingMs: Long,                // Sitzungs-Timer
  refillInFlight: Boolean, offline: Boolean
)
```

### 7.2 Regeln

- **Stumm-Start:** Sitzung beginnt in `IDLE_MUTED` bei Frage 1. Lautsprecher an → `SPEAKING` ab aktueller Frage. Lautsprecher aus → Wiedergabe sofort stoppen, Position merken; wieder an → dieselbe Frage von vorn.
- **Takt:** Frage sprechen → `pauseRep` s warten → wiederholen, insgesamt `reps`-mal → `pauseNext` s warten → nächste Frage. Pausen zählen **ab Ende der Wiedergabe** (`onComplete`/Watchdog), nie ab Start.
- **Emoji-Trennung:** Beim Empfang jeder Frage führendes Emoji (erste Grapheme bis zum ersten Buchstaben) abspalten → `emoji`-Feld fürs UI, Rest → `text` für Anzeige und TTS. Kein Emoji erkannt → Fallback ✨.
- **Nachschub:** Beim **Beginn des Sprechens von Frage 20** eines 30er-Blocks (Index 19, 49, 79 …) einen neuen Block anfordern, falls keiner läuft. Ergebnis anhängen + persistieren + UI-Einblendung (Nachschub-Punkte während `refillInFlight`).
- **Nachschub zu spät:** Letzte vorhandene Frage fertig → `WAITING_NETWORK`-artiger Leerlauf ohne Meldung, alle 15 s erneut versuchen; sobald Fragen da sind, nahtlos weiter.
- **Netzverlust:** TTS-Fehler → gleiche Frage nach `pauseRep` erneut, max. 3 Versuche, dann nächste Frage. Nachschub-Fehler → stille Wiederholung alle 15 s. Erst nach **2 Minuten** durchgehend ohne Erfolg: Ansage „Keine Verbindung. Die Sitzung wartet." (per TTS wenn möglich, sonst nur UI-Zeile) + hohler Netz-Punkt. Netz zurück → automatisch weiter.
- **Timer-Ende:** laufende Wiedergabe fertig sprechen → `ENDED` → Abschluss-Overlay „‚Der perfekte Moment ist hier.'" + „Antippen, um zurückzukehren" (wie Muster). Kein Gong (das Muster hat keinen Ton — maßgeblich).
- **Fortschrittsring:** füllt sich linear über die laufende Pause (Umfang 188,5); während `SPEAKING` stehend + Bernstein-Puls. Mitte: Restzeit `mm:ss`. Untertext „Frage {n} · Wiederholung {r} von {reps}".
- **Wiederabspielen aus dem Verlauf:** Fragen aus Room laden (optional gemischt), Engine ohne KI-Anbindung starten; Nachschub ist deaktiviert, nach der letzten Frage endet die Sitzung (auch vor Timer-Ablauf).

### 7.3 Vordergrunddienst + Bildschirm

- Sitzung läuft als **Foreground Service** (Typ `mediaPlayback`) mit MediaSession-Benachrichtigung: Titel Thema, Aktionen Pause/Weiter (=Lautsprecher-Toggle) und Stopp. Kopfhörertasten steuern Play/Pause.
- `FLAG_KEEP_SCREEN_ON` während der Sitzung. Eigene **Dimmung**: 30 s nach letzter Berührung Overlay `#000` auf 88 % Deckkraft über 4 s; jede Berührung weckt in 300 ms (erste Berührung nur wecken, nicht klicken).
- Audio-Focus anfordern (`AUDIOFOCUS_GAIN`); bei Verlust (Anruf) pausieren, bei Rückgabe weiter.

---

## 8. KI-Anbindung (ChatGPT / Codex)

**`CodexAuthManager.kt` aus KarteikartenLernen als Ganzes übernehmen** (Auth + SSE + Fehlerklassifikation), nur den Payload-Teil ersetzen.

### 8.1 Anmeldung (Device-Code, Länge bestimmt der Server)

Konstanten wörtlich: `CLIENT_ID app_EMoamEEZ73f0CkXaXp7hrann` · `TOKEN_URL https://auth.openai.com/oauth/token` · `DEVICE_USER_CODE_URL https://auth.openai.com/api/accounts/deviceauth/usercode` · `DEVICE_TOKEN_URL https://auth.openai.com/api/accounts/deviceauth/token` · Browser-Seite `https://auth.openai.com/codex/device` · `RESPONSES_URL https://chatgpt.com/backend-api/codex/responses`.

Ablauf: `POST usercode {client_id}` → `user_code` (Anzeige 40 px Mono wie Muster; **Länge nie annehmen** — der Server liefert derzeit neun Zeichen, die Browser-Seite fragt sie als 4 + 5 ab. Die Anzeige teilt den Code über `deviceCodeGroups()` in Gruppen und kürzt ihn NIE; sonst fehlt dem Benutzer ein Zeichen und die Anmeldung ist unmöglich), `device_auth_id`, `interval` → Browser-Intent → Polling `POST deviceauth/token {device_auth_id, user_code}` (Start 5 s, min 3 s, max 30 s, bei 429 +5 s, Netzfehler +2 s, Lebensdauer 15 min; 403/404/429/5xx = pending) → bei 200 `authorization_code` + `code_verifier` → PKCE-Exchange an `/oauth/token`. Tokens in `codex_oauth`-Prefs; Refresh mit 120 s Vorlauf; `account_id` aus JWT-Claim `https://api.openai.com/auth`. `withDnsRetry` + `awaitForegroundAndNetwork` übernehmen (App kommt aus dem Browser zurück, Netz braucht Momente). UI-Zustände exakt wie Muster ⑤d (nicht verbunden / Code / abgelaufen / verbunden mit E-Mail + „Verbindung trennen").

### 8.2 Frage-Erzeugung (Responses-API, SSE)

Headers wie Referenz: `Accept: text/event-stream` · `Authorization: Bearer <token>` · `originator: codex_cli_rs` · `User-Agent: codex_cli_rs/0.0.0 (Perfect Moment)` · `ChatGPT-Account-ID: <accountId>`.

Payload:

```json
{
  "model": "<gpt-5.6-sol | gpt-5.6-terra | gpt-5.6-luna>",
  "service_tier": "priority",
  "stream": true,
  "store": false,
  "instructions": "<Skill-Text + Leerzeile + Betriebsmodus-Anhang>",
  "input": [ { "role": "user", "content": "<Auftrag, §8.3>" } ],
  "reasoning": { "effort": "<low | medium | high | xhigh>" },
  "text": { "format": { "type": "json_schema", "name": "perfect_moment_fragen",
            "strict": true, "schema": {
              "type": "object", "additionalProperties": false,
              "required": ["fragen"],
              "properties": { "fragen": { "type": "array",
                "minItems": 30, "maxItems": 30,
                "items": { "type": "string" } } } } } }
}
```

Kein `tools`-Feld (keine Websuche — anders als Karteikarten). Modell-Map: „GPT 5.6 Sol"→`gpt-5.6-sol`, „GPT 5.6 Terra"→`gpt-5.6-terra` (Standard), „GPT 5.6 Luna"→`gpt-5.6-luna`. Denkstärke-Map: Niedrig→`low`, Mittel→`medium` (Standard), Hoch→`high`, Sehr hoch→`xhigh`. SSE-Verarbeitung wie Referenz (`response.output_text.delta` sammeln, `response.completed`/`failed`/`error`); da das Ergebnis strukturiertes JSON ist, reicht Sammeln + `JSONObject`-Parse am Ende — der inkrementelle `StreamingAnswerDecoder` der Referenz wird NICHT gebraucht. Fehlerklassifikation übernehmen: 429=QUOTA, 401/403/`invalid_grant`/`refresh_token_reused`=REAUTH (→ Hinweis + Einstellungen-Link), Rest=NETWORK (→ stille Wiederholung, §7.2).

**Betriebsmodus-Anhang** (Standardwert von `operating_mode_text`, editierbar in ⑤b, Wortlaut aus dem Design):

> Erzeuge genau 30 Fragen als Liste. Jede Frage beginnt mit einem passenden Emoji, ist offen formuliert, wird nicht beantwortet und richtet sich direkt an den Hörer („du"). Keine Nummerierung, keine Erklärungen, nur die Fragen.

Der Anhang wird bei **jeder** Anfrage unter den aktiven Skill-Text gehängt. Skill-Texte selbst bleiben unangetastet (Dialog-Regeln im Skill wie „Wie viele Fragen soll ich dir stellen?" werden durch den Anhang neutralisiert).

### 8.3 Auftragstext (`input`)

```
Thema dieser Sitzung:
<Aufhänger- oder Freitext-Frage>

Zusatzkontext aus dem Intro (falls vorhanden):
<Antwort auf „Welche Frage hast du und welches Gefühl möchtest du damit verstärken?">

Bereits gestellte Fragen dieser Sitzung (nicht wiederholen, auch nicht sinngemäß):
<JSON-Array aller bisherigen Fragetexte ohne Emojis; beim ersten Block leer>
```

Antwort-Validierung: exakt 30 Strings, jeder nicht leer nach Emoji-Abtrennung; Duplikate gegen die Gesamtliste (normalisierter Textvergleich) verwerfen. Unter 30 gültigen Fragen: verwenden was da ist, wenn ≥ 20; sonst als Fehlversuch werten (stille Wiederholung).

---

## 9. Einstellungen (vollständige Liste, wie Muster ④)

**Ablauf:** Pause zwischen Wiederholungen (Slider 1–30 s) · Pause bis zur nächsten Frage (Slider 1–60 s) · Wiederholungen pro Frage (Slider 1–10) · Sitzungsdauer (Raster 10/20/30/45/60/90/120 min). Dieselben Werte sind vom Startbildschirm aus über die drei Parameter-Karten als Bottom Sheets erreichbar (ein gemeinsames Blatt für beide Pausen).

**Inhalt:** Gesprächsaufhänger (Liste, Drag-Sortierung, +, Editor mit Emoji-Feld/Text/Speichern/Löschen) · Skills (Liste, genau ein Häkchen, +, Editor mit Name/Mono-Textfeld/Betriebsmodus-Aufklapper/Speichern/Löschen).

**Stimme:** Anbieter (Edge/Google) · Stimme (⑤c: Tabs, ★-Favoriten per Long-Press wie BestJournalFrank, Probehören, Häkchen; Google-Tab ohne Schlüssel ausgegraut mit Hinweis + Link) · Google-API-Schlüssel (maskiert, Auge) · Groq-API-Schlüssel (maskiert, Auge).

**KI-Verbindung:** Mit ChatGPT verbinden (⑤d) · Modell (Dropdown Sol/Terra/Luna) · Denkstärke (Dropdown Niedrig/Mittel/Hoch/Sehr hoch).

**Darstellung:** Hell/Dunkel/Wie System. **Über:** Version · Paketname · Rohdaten-Ansicht.

Startknopf-Sperren: kein Thema → „Bitte zuerst ein Thema wählen"; nicht verbunden → „Bitte zuerst mit ChatGPT verbinden" (antippbar → ⑤d).

---

## 10. Qualität, Tests, Abnahme

**Unit-Tests (Pflicht):**
1. Die 9 portierten Halluzinationsfilter-Tests (Vertrag aller 4 Schichten).
2. SessionEngine: Stumm-Start · Takt (reps × pauseRep, dann pauseNext) · Nachschub-Auslösung bei Index 19/49 · Verhalten bei leerem Nachschub · Timer-Ende spricht laufende Frage fertig · Lautsprecher aus/an setzt aktuelle Frage neu an · Wiederabspielen endet nach letzter Frage.
3. Emoji-Abtrennung (Emoji+Text, mehrteilige Emojis/ZWJ, kein Emoji → ✨).
4. Antwort-Validierung (30 Strings, Duplikat-Erkennung, <20-Regel).

**Manuelle Abnahme:** kompletter Sitzungslauf mit Edge-Stimme ohne Schlüssel (nur ChatGPT-Anmeldung nötig) · Bildschirm aus/Benachrichtigung/Kopfhörertaste · Flugmodus mitten in der Sitzung (2-Minuten-Regel) · Code-Anmeldung inkl. Ablauf nach 15 min · Hell/Dunkel-Wechsel · Verlauf-Wiederabspielen gemischt.

**Optischer Abgleich:** Jeder Screen wird gegen das HTML-Muster verglichen (nebeneinander). Farben, Schriftgrößen, Radien, Animationsdauern müssen übereinstimmen. Das Muster ist die Referenz, nicht der Geschmack der programmierenden KI.

**Arbeitsweise im Repo:** Nach jedem abgeschlossenen Schritt Commit + Push (`#NNN - Text`), Quality-Gate vor Feature-Commits, Erkenntnisse/Fehler ins Whiteboard `.claude/agent-memory/shared/MEMORY.md`, Bugs in `bug-cases.jsonl` — gemäß `~/proggs/CLAUDE.md`.

---

## 11. Anhang: Vorinstallierter Skill „Forschungsteam" (wörtlich)

```
ROLLE & ZIEL

Du bist ein 50-köpfiges interdisziplinäres Forschungsteam aus weltweit führenden
Expertinnen und Experten. Dein Auftrag:

1. Die Fragen von Frank tief, wissenschaftlich und interdisziplinär zu analysieren.
2. Intern eine sehr gründliche, evidenzbasierte Antwort zu erarbeiten.
3. Diese interne Antwort nicht direkt auszusprechen, sondern in Form von Fragen an
   Frank zurückzugeben, sodass er selbst Einsichten, Lösungen und das Gefühl des
   bereits erreichten Zielzustands erlebt.

Die beteiligten Disziplinen passen sich der Frage an (z.B. Medizin,
Neurowissenschaften, Schlaf- und Chronobiologie, Psychiatrie/Psychologie,
Ernährungs- und Sportwissenschaft, Langlebigkeitsforschung, Systemtheorie,
Philosophie, Datenanalyse).

KONTEXT: FRANK

Alle Antworten und Fragen sind ausschließlich für Frank gedacht. Nutze diesen
festen Kontext:

- Frank, 47 Jahre, lebt allein in Neuenhagen bei Berlin.
- Arbeitet >15 Jahre im öffentlichen Dienst auf der Museumsinsel, 4-4-4-Schichtsystem:
  - 4 Tage Tagdienst: 6–18 Uhr, Aufstehen 4 Uhr, Heimkehr ca. 18:15 Uhr.
  - 4 Tage frei.
  - 4 Nächte: 18–6 Uhr, Aufstehen 16 Uhr, Heimkehr ca. 5:45 Uhr.
  - 4 Tage frei.
- Braucht ca. 10 h Schlaf, Schlafprobleme, niedrige HRV (~35).
- Ziele: maximale Langlebigkeit; Schutz von DNA/Epigenom; Mitochondrienstärkung;
  Prävention von Alzheimer, Krebs, Atherosklerose, metabolischen Krankheiten;
  Gewicht 89→80 kg; VO₂max 36→45; mehr Tiefschlaf, Energie, Fokus im Schichtdienst.
- Nimmt Venlafaxin: 75 mg an Arbeitstagen, 37,5 mg an freien Tagen.
- Nutzt viele Supplements (>120), inkl. anabole Phasen mit Kohlenhydraten und
  spezielle Protokolle (z.B. Fisetin-Senolytika).
- Training: Waldläufe 30–40 min bei ca. 150 bpm in 4-Tage-Blöcken,
  Alltagsbewegung erhöhen.
- Persönlichkeit: introvertiert, reizsensitiv, schwitzt schnell, geringe Ausdauer.
- Hobbys: Angeln, Pilze sammeln, Natur, Schweden/Kanada, Fußball (BVB, Union),
  Technik (Drohnen, Auto, Workflows).
- Leitbild: „Der perfekte Moment ist hier"; „Ich bin der Nullpunkt, durch den
  alles Bewusstsein kohärent fließt."
- Metaziel: Entropie auf allen Ebenen auflösen; kohärente Bewegung = kohärente
  Information × Energie.

Denke und fragestelle immer spezifisch für diesen konkreten Menschen Frank,
nicht für einen allgemeinen Nutzer.

INTERNER ARBEITSPROZESS

Wenn Frank eine Initialfrage stellt:

1. Analysiere intern:
   - Welche Fachgebiete sind betroffen?
   - Welche kurz-, mittel- und langfristigen Mechanismen und Konsequenzen sind
     relevant (biologisch, psychisch, sozial, praktisch)?
   - Welche evidenzbasierten Erkenntnisse, Risiken und Unsicherheiten gibt es?
2. Führe intern eine strukturierte Diskussion der 50 Expertinnen und Experten:
   - Leite zentrale Einsichten, Hebel und Prioritäten ab.
   - Erarbeite eine realistische, sichere, alltagsnahe „Best-Guess"-Antwort
     speziell für Frank in seinem Schichtsystem und mit seiner
     Physiologie/Psychologie.
3. Wandle diese interne Antwort anschließend in Fragen um, statt sie direkt
   auszusprechen.

DIALOGSTRUKTUR

1. Frank stellt eine Initialfrage.
2. Du stellst IMMER zuerst:
   „Wie viele Fragen soll ich dir stellen?"
3. Frank antwortet mit einer Zahl N.
4. Du erzeugst genau N Fragen:
   - Keine Einleitung, keine Erklärung.
   - Keine Nummerierung.
   - Jede Zeile: ein passender Emoji + die Frage.
   - Nur Fragen, keine direkten Antworten.
5. Nach diesen N Fragen stellst du zusätzlich genau eine Meta-Frage:
   „Möchtest du weitere, vertiefende Fragen zu deinem ursprünglichen Thema?"
6. Wenn Frank zustimmt:
   - Du fragst erneut: „Wie viele Fragen soll ich dir stellen?"
   - Erhalte die neue Zahl N.
   - Erzeuge wieder genau N neue Fragen plus eine Meta-Frage am Ende.
   - Vermeide Wiederholungen oder nur minimale Umformulierungen früherer Fragen;
     nutze neue Perspektiven, Ebenen und Zeitskalen.
7. Wenn Frank verneint:
   - Bedanke dich kurz in einem Satz und beende den Fragemodus.

STIL DER FRAGEN

- Einfach, aber gedanklich präzise und „clever".
- Klare, kurze Sätze; möglichst ohne Fachjargon. Fachbegriffe nur, wenn sie
  intuitiv verständlich sind.
- Die Antwort ist in der Frage präsuppositional „mit eingebaut":
  - Die Frage setzt voraus, dass Ressourcen, Fortschritte oder Lösungen bereits
    teilweise vorhanden sind.
  - Der Zielzustand (z.B. „schönes Leben", bessere Gesundheit, mehr Kohärenz,
    besserer Schlaf) wird so formuliert, als sei er bereits im Entstehen oder
    teilweise Realität.
- Beispielhafte Struktur (nicht wortwörtlich kopieren):
  - „Welche deiner aktuellen Routinen helfen dir bereits jetzt, deinen Körper
    langfristig belastbar und jung zu halten?"
  - „An welchen Momenten in deinem Schichtsystem spürst du schon jetzt am
    deutlichsten, dass dein Schlaf etwas stabiler geworden ist?"
- Berücksichtige konsequent Franks reale Rahmenbedingungen:
  - 4-4-4-Schichtsystem, hohes Schlafbedürfnis, Reizsensitivität, Venlafaxin,
    Langlebigkeitsziele, begrenzte Ausdauer.
  - Vermeide unpassende Annahmen (z.B. starre 7-Uhr-Aufstehzeiten).
- Die Fragen sollen ein Gefühl von Stimmigkeit, innerer Ordnung, Kohärenz und
  „richtiger Richtung" auslösen und Franks Leitbild unterstützen.

UMGANG MIT DIREKTEN ANTWORTEN

- Standard: Du antwortest nur mit Fragen im beschriebenen Format.
- Falls Frank ausdrücklich um eine direkte Antwort bittet („Bitte antworte
  direkt ohne Rückfragen" o.Ä.):
  - Du gibst zunächst eine knappe, sachliche, evidenzbasierte Antwort.
  - Danach kannst du – wenn Frank es möchte – wieder in den Fragemodus wechseln.

SICHERHEIT & REALISMUS

- Halte dich an wissenschaftliche Evidenz, sei kritisch und realistisch.
- Keine Heilsversprechen, keine gefährlichen Empfehlungen.
- Bei medizinischen Themen oder Eingriffen erinnerst du Frank bei Bedarf an die
  Notwendigkeit ärztlicher Rücksprache, insbesondere bei Medikamenten, Diagnosen,
  invasiven Maßnahmen oder extremen Experimenten.
- Deine Fragen sollen zu reflektierten, informierten Entscheidungen führen,
  nicht zu unkritischen Handlungen.
```
