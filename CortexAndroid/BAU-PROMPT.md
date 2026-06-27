# Bau-Prompt: „Cortex" — Android-App fürs zweite Gehirn (Grundgerüst)

> Diesen kompletten Text als Auftrag an das Coding-Modell geben. Er ist **selbst-enthalten**:
> alle Bildschirme, jeder Server-Aufruf (Methode, URL, Header, Body, Antwort), die
> WireGuard-Integration, die Einstellungen, das Theme und die bekannten Fallen stehen drin.
> Sprache der App: **Deutsch**. Ziel dieses Auftrags: ein **lauffähiges Grundgerüst**.

---

## 0. Deine Rolle & Liefergegenstand

Du bist Senior-Android-Entwickler. Baue eine **native Android-App in Kotlin + Jetpack Compose
(Material 3)** namens **„Cortex"**. Es ist eine **private App** (nur für die eigenen Geräte des
Besitzers, **kein Play Store**) — daher: **nur Deutsch** (keine Übersetzung/i18n nötig), **kein**
Datenschutz-/Rechts-Screen, **kein** Premium/Abo, **kein** Login.

**Liefergegenstand = ein lauffähiges Grundgerüst** mit:
- 3 verdrahteten Tabs (Gespräch, Dashboard, Einstellungen) + WireGuard-Schalter in der Topbar
- funktionierender WireGuard-Tunnel **in der App selbst**
- echtem Chat gegen den Server-Agenten (inkl. 2-Schritt-Speicher-Bestätigung)
- echtem Dashboard (Übersicht + Stöbern/Suchen/Bearbeiten/Löschen)
- Einstellungen mit allen Schlüsseln (sicher gespeichert)
- angebundenem Groq-STT (Spracheingabe), Gemini-TTS (Vorlesen), Gemini-Verbessern
- der Beobachtungsschicht (Logging + Logik-/Intent-Sonden) von Anfang an
- sauberem Gradle-Build (`./gradlew assembleDebug` grün)

Am Ende: kurze README mit Build-/Install-Schritten und wo die Schlüssel eingetragen werden.

---

## 1. Das Produkt in einem Absatz

„Cortex" ist die mobile Fassung eines bestehenden Web-Cockpits für ein **zweites Gehirn** —
einen persönlichen Wissens-/Erinnerungsspeicher (Vektordatenbank). Die App erlaubt: **mit dem
Gehirn reden** (ablegen + nachschlagen über einen Server-Agenten, der selbst antwortet),
**reinschauen/stöbern** (Statistik, Einträge durchsuchen, ansehen, bearbeiten, löschen) und
**alles per Sprache** (reinsprechen via Groq-Whisper, Antworten vorlesen via Gemini-TTS). Die
Server-Dienste liegen auf einem privaten VPS und sind **ausschließlich über WireGuard-VPN
erreichbar** — deshalb hat die App einen eingebauten WireGuard-Schalter; ohne aktiven Tunnel
gibt es keinen Serverzugriff.

---

## 2. Technischer Stack & Projektrahmen

- **Sprache/UI:** Kotlin, Jetpack Compose, Material 3. MVVM (ViewModel pro Screen + Repository-Schicht).
- **SDK:** `minSdk 26`, `targetSdk 35`, `compileSdk 35` (oder neuer stabil). Package: **`de.frank.cortex`**.
- **Build:** Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog optional. App muss mit
  `./gradlew assembleDebug` bauen. **Versionsnummer sichtbar** (Settings „Über", siehe §9 + §14).
- **Abhängigkeiten (Richtwerte, aktuelle stabile Versionen wählen):**
  - Compose BOM, `androidx.activity:activity-compose`, `androidx.navigation:navigation-compose`,
    `androidx.lifecycle:lifecycle-viewmodel-compose`
  - Coroutines (`kotlinx-coroutines-android`)
  - Netzwerk: **Retrofit + OkHttp + Moshi** (Moshi-Converter). OkHttp-`MultipartBody` für den
    Groq-Audio-Upload. Ein eigener OkHttp-Client pro Ziel (agent/brain/dashboard/groq/gemini)
    mit Logging-Interceptor (nur Debug).
  - Sichere Schlüsselspeicherung: **`androidx.security:security-crypto`**
    (`EncryptedSharedPreferences`) für alle Secrets/Keys. UI-Präferenzen (Theme, Vorlesen an/aus,
    Stimme) dürfen in normalem DataStore/SharedPreferences liegen.
  - WireGuard: **`com.wireguard.android:tunnel`** (Maven Central; das **tunnel-only**-Artefakt,
    keine fertige UI). Bringt GoBackend + nutzt Androids `VpnService`.
  - Audio: **`AudioRecord`** (PCM-Aufnahme fürs Mikrofon) + **`AudioTrack`** (PCM-Wiedergabe
    fürs Vorlesen). Kein ExoPlayer nötig.

- **Projektstruktur (Vorschlag):**
  ```
  de.frank.cortex
    ├─ CortexApp.kt              (Application; initialisiert Logging + WireGuard-Backend)
    ├─ MainActivity.kt          (Compose-Host, Navigation)
    ├─ ui/
    │   ├─ theme/               (Farben, Typo, Theme — siehe §12)
    │   ├─ chat/                (Gespräch-Screen + ViewModel)
    │   ├─ dashboard/           (Dashboard-Screen + ViewModel)
    │   ├─ settings/            (Einstellungen-Screen + ViewModel)
    │   └─ common/              (gemeinsame Composables: TopBar, VpnSwitch, ...)
    ├─ data/
    │   ├─ SettingsStore.kt     (EncryptedSharedPreferences-Wrapper)
    │   ├─ Repository.kt        (bündelt agent/brain/dashboard-Aufrufe)
    │   └─ model/               (DTOs/Datenklassen)
    ├─ network/
    │   ├─ AgentApi.kt          (Retrofit-Interface :8002)
    │   ├─ BrainApi.kt          (Retrofit-Interface :8000)
    │   ├─ DashboardApi.kt      (Retrofit-Interface :8003)
    │   ├─ GroqApi.kt           (Retrofit/OkHttp für Groq-STT)
    │   └─ GeminiApi.kt         (Gemini TTS + Verbessern)
    ├─ vpn/
    │   ├─ WireGuardManager.kt  (Tunnel hoch/runter, Status, Config-Import)
    │   └─ CortexTunnelService.kt (Foreground-Service / VpnService-Integration)
    ├─ audio/
    │   ├─ MicRecorder.kt       (AudioRecord → WAV-Bytes)
    │   └─ PcmPlayer.kt         (AudioTrack für Gemini-PCM)
    └─ observability/
        └─ Log.kt              (JSON-Lines-Logger + probe()/checkpoint())
  ```

---

## 3. Server-Landschaft, Auth & VPN-Zwang (WICHTIG)

Alle Dienste sind an die **WireGuard-VPN-IP `10.8.0.1`** gebunden und **nur über den Tunnel**
erreichbar (kein öffentlicher DNS, kein Internet-Port). **Jeder Server-Aufruf setzt voraus, dass
der Tunnel aktiv ist.** Ist er aus → freundlicher Hinweis „VPN aktivieren", kein Crash.

| Dienst | Basis-URL (Default) | Auth | Wofür in der App |
|---|---|---|---|
| **agent** | `http://10.8.0.1:8002` | `Authorization: Bearer <SB_API_KEY>` | Gespräch-Tab (`POST /chat`), Kategorien-Dropdown, neue Kategorie |
| **brain-api** | `http://10.8.0.1:8000` | `Authorization: Bearer <SB_API_KEY>` | Dashboard-CRUD: suchen, stöbern, ansehen, bearbeiten, löschen, Statistik |
| **dashboard** | `http://10.8.0.1:8003` | **keine** (Tunnel = Schutz) | nur `GET /api/overview` für Server-Vitalwerte + Agent-Status |
| Groq (extern) | `https://api.groq.com` | `Bearer <GROQ_API_KEY>` | Spracheingabe (Whisper) |
| Gemini (extern) | `https://generativelanguage.googleapis.com` | Header `x-goog-api-key: <GEMINI_API_KEY>` | Vorlesen (TTS) + Verbessern |

Host/IP und Ports sind in den Einstellungen änderbar (Default `10.8.0.1`, `8002/8000/8003`).
`SB_API_KEY`, `GROQ_API_KEY`, `GEMINI_API_KEY` gibt der Besitzer in den Einstellungen ein.
Standard-`user_id` für alle Aufrufe: **`"frank"`**.

> Hinweis: Groq und Gemini sind **öffentliche** Dienste — die App ruft sie direkt aus dem Netz
> auf, dafür ist KEIN VPN nötig (nur für die drei privaten Dienste oben).

---

## 4. WireGuard-Integration (eigener Tunnel in der App)

Die App baut den Tunnel **selbst** über `com.wireguard.android:tunnel` + `VpnService` (GoBackend).
KEINE Fernsteuerung einer fremden App.

**Funktionsumfang:**
1. **Config-Import (Einstellungen):** Der Besitzer importiert seine `SecondBrain.conf` einmalig —
   entweder per Datei-Auswahl (`*.conf`) **oder** per Textfeld „Konfiguration einfügen". Den
   Inhalt mit `com.wireguard.config.Config.parse(...)` parsen und sicher
   (EncryptedSharedPreferences) speichern. Bei Parse-Fehler klare Meldung.
2. **Schalter (Topbar):** Ein großer, gut sichtbarer Toggle „SecondBrain VPN". An → Tunnel hoch,
   Aus → runter. Status farbig: **grün „verbunden" / rot „getrennt" / gelb „verbinde…"**.
3. **Erstmaliger Start:** Androids `VpnService.prepare()` aufrufen → System-Bestätigungsdialog
   (einmalig). Bei Zustimmung Tunnel via `backend.setState(tunnel, UP, config)` starten. Einen
   **Foreground-Service** mit dezenter Notification für den laufenden Tunnel verwenden.
4. **Nur ein VPN gleichzeitig:** Android erlaubt nur einen aktiven VPN. Wenn das Hochfahren
   scheitert (anderes VPN aktiv), klar melden: „Ein anderes VPN ist aktiv — bitte erst trennen."
5. **Selbstheilung im UI:** Schlägt ein Server-Aufruf fehl, weil der Tunnel aus ist, zeigt die App
   einen Hinweis + den Schalter, statt nur einen Fehler.

**Technische Punkte:** `minSdk 26` ist ok (VpnService ab API 21). Die `AllowedIPs` der Config
müssen `10.8.0.0/24` enthalten (Split-Tunnel genügt — kein Full-Tunnel nötig). Die nativen
Go-Backend-Bibliotheken kommen mit dem Artefakt; nichts extra kompilieren.

---

## 5. Navigation

- **Bottom Navigation** mit 3 Zielen: **Gespräch** · **Dashboard** · **Einstellungen**.
- **Topbar** (über allen Tabs): links Marke „🧠 Cortex / zweites Gehirn", rechts der
  **WireGuard-Schalter** + Status-Pille, ganz rechts Theme-Umschalter (Sonne/Mond).
- Dezente Tab-Wechsel-Animation; `prefers-reduced-motion`/Reduced-Motion respektieren.

---

## 6. Tab „Gespräch" (Chat mit dem Agenten)

Bildet den Web-Gespräch-Tab 1:1 nach.

**Aufbau:** oben das Nachrichten-Log (Sprechblasen; Nutzer rechts, Agent links; Leerzustand
„Ablegen oder nachschlagen…"), unten der Eingabeblock mit Werkzeugzeile + Textfeld.

**Werkzeugzeile (von links nach rechts), exakt:**
| Element | Funktion |
|---|---|
| **Titel**-Eingabefeld (optional, max 200) | Override-Titel beim Speichern; sonst vergibt der Agent den Titel |
| **Kategorie-Dropdown** | Optionen aus `agent GET /categories/detail`; oben „Auto-Kategorie" (= leer) + „➕ Neue Kategorie…" |
| **rotes X** | leert NUR das Textfeld (Titel/Kategorie bleiben) |
| **Vorlesen-Toggle** (Lautsprecher, **orange**, Standard AN) | TTS der Agent-Antworten an/aus |
| **Mikrofon** (orange) | Spracheingabe → Groq Whisper (siehe §10) |
| **„G"-Knopf** (grün) | Eingetippten/gesprochenen Text mit Gemini glätten (siehe §10) |
| **Senden** (Papierflieger, orange) | Nachricht abschicken |

**Neue Kategorie anlegen:** Bei Auswahl „➕ Neue Kategorie…" einen Dialog zeigen
(„Name der neuen Kategorie. Tipp: ‚Haupt/Unter' legt eine Unterkategorie an") →
`agent POST /categories {name}` → Dropdown neu laden, neue Kategorie auswählen.

**Senden-Flow:**
1. `POST {agent}/chat` (Bearer) mit Body:
   ```json
   { "text": "<Eingabe>", "session_id": "<stabil>", "user_id": "frank",
     "category": "<oder weglassen>", "title": "<oder weglassen>", "store_timestamp": true }
   ```
   - `session_id`: beim App-Start einmal erzeugen (z.B. `"android-" + UUID`) und **über die
     ganze Unterhaltung konstant halten** (sonst funktioniert die Speicher-Bestätigung nicht).
   - `text` max 500000 Zeichen; vorher prüfen, sonst freundliche Ablehnung.
2. Antwort (JSON):
   ```json
   { "ok": true, "reply": "<Antworttext>", "action": "save_confirm|store|cancel|recall|internet|smalltalk|error",
     "session_id": "...", "category": null, "title": null, "stored": false, "replaced": false,
     "recall_hits": null, "options": [ { "label": "Ja", "send": "ja" }, ... ] }
   ```
   - `reply` als Agent-Blase anzeigen.
   - **Meta-Zeile** unter der Blase je nach `action`:
     `store`→„↳ abgelegt in „<category>""; `recall`→„↳ nachgeschlagen · <recall_hits> Treffer";
     `save_confirm`/`store_clarify`→„↳ Rückfrage…"; `cancel`→„↳ nicht gespeichert".
   - **`options`**: als anklickbare Knöpfe unter der Blase rendern. Klick = nächste Nachricht
     mit `text = option.send` und **derselben `session_id`** senden.
   - **2-Schritt-Speicher (vom Server erzwungen):** Beim Ablegen kommt zuerst `save_confirm`
     (nichts gespeichert) mit Ja/Nein-`options`. Erst die Bestätigung („ja") führt zu
     `action:"store"`, `stored:true`. „nein" → `cancel`. Die App muss diesen Dialog über die
     stabile Session abbilden.
3. Nach Erhalt der Antwort: wenn Vorlesen AN → `reply` per Gemini-TTS vorlesen (§10).
4. **Enter** sendet, **Shift+Enter** = Zeilenumbruch. Titel-Feld nach erkanntem Speichern leeren.

**Kein Streaming** — `/chat` liefert genau eine vollständige JSON-Antwort (Ladeindikator während
der Wartezeit, recall kann ein paar Sekunden dauern).

---

## 7. Tab „Dashboard" (Übersicht + Stöbern/Suchen/Löschen)

Zwei Bereiche im selben Tab.

### 7a. Übersicht (oben)
- **Große Gesamtzahl** der Einträge + Label „EINTRÄGE GESAMT".
- **Gedächtnis-Spektrum:** gestapelte Farbleiste, pro Kategorie ein Segment proportional zur
  Anzahl; darunter Legende (farbiges Quadrat + Name + Anzahl). **Klick auf eine Kategorie**
  filtert den Stöber-Bereich (7b) auf diese Kategorie. **Konsistente Kategoriefarben** über
  Spektrum, Legende und Tags (feste 13-Farben-Palette zyklisch, §12).
- **Vier Vital-Karten:** Bibliothekar-Agent (Status „Bereit"/„offline" + Modell + Sitzungen),
  Prozessor (CPU % + Balken), Arbeitsspeicher (% + benutzt/gesamt + Balken), Speicherplatz
  (% + benutzt/gesamt + Balken).
- **Auto-Refresh alle ~20 s.** Eine **Verbindungs-Pille** (grün/rot) zeigt, ob der Server antwortet.

**Datenquellen der Übersicht:**
- Gesamtzahl + Kategorien (Spektrum/Legende): `brain-api GET /category-counts`
  → `{ ok, counts: { "<Kategorie>": <int>, ... }, total_distinct: <int> }` (Bearer).
- Vektor-Punkte/Embed-Modell/Version: `brain-api GET /health`
  → `{ status, version, points, embed_model, ... }`.
- **Vital-Karten + Agent-Status:** `dashboard GET /api/overview` (ohne Auth) →
  ```json
  { "total":177, "brain":{...}, "agent":{"status":"ok","model":"...","sessions":0},
    "server":{"cpu_pct":2.0,"mem_used":1033,"mem_total":8200,"mem_pct":12.0,
              "disk_used":5600,"disk_total":96000,"disk_pct":6.0} }
  ```
  (`server.*` ist in Bytes/Prozent.) Fehlt ein Feld → robust mit „–" anzeigen.

### 7b. Stöbern / Suchen (darunter)
- **Suchfeld** (Live-Suche, ~300 ms Debounce) → `brain-api POST /search`
  Body `{ "query": "<text>", "user_id":"frank", "limit": 20, "category": "<optional>" }` →
  `{ ok, count, items:[ { doc_id, title, category, score, match, text, created_at, updated_at } ] }`.
  `score` (0–1) als % anzeigen, Snippet aus `match` (sonst `text`).
- **Kategorie-Chips:** „Alle" + ein Chip je Kategorie. Klick → `brain-api GET /by-category?category=<name>`
  bzw. für eine Hauptkategorie `GET /by-parent?parent=<name>` → `{ ok, count, items:[...] }`.
- **Trefferliste:** Karten mit Titel (prominent), farbigem Kategorie-Tag, 2-Zeilen-Snippet,
  bei Suche rechts der Score in %.
- **Detail-Ansicht** (Klick auf Karte): voller **Titel + Kategorie + Datum + kompletter Text**
  (monospace-Lesebox). Aktionen:
  - **Bearbeiten:** `brain-api PUT /entry` Body `{ doc_id, text, title?, categories? }`.
  - **Kategorie ändern:** `brain-api POST /entry/category` Body `{ doc_id, category }`.
  - **Löschen:** `brain-api DELETE /entry?doc_id=<id>` (Soft-Delete → Papierkorb; idempotent).
    Vor dem Löschen kurze Bestätigung.
- **Leerzustände:** „lädt…", „Nichts gefunden.", „Fehler beim Laden." sauber gestalten.

---

## 8. Tab „Einstellungen"

Klare Sektionen, alle **Secrets in `EncryptedSharedPreferences`**:

1. **Verbindung / VPN**
   - WireGuard-Konfiguration: importieren (Datei) **oder** einfügen (Textfeld) + Status/Anzeige
     „Konfiguration vorhanden / fehlt".
   - Server-Host (Default `10.8.0.1`), Ports agent (`8002`), brain-api (`8000`), dashboard (`8003`).
   - **SB_API_KEY** (Bearer für agent + brain-api).
2. **KI-Schlüssel**
   - **Groq API-Key** (Spracheingabe).
   - **Gemini API-Key** (Vorlesen + Verbessern).
3. **Sprache & Stimme**
   - Vorlesen an/aus (Default AN), TTS-Stimmen-Auswahl (eine sinnvolle Default-Stimme;
     Liste fest verdrahten, z.B. ein paar Gemini-TTS-Stimmen).
4. **Darstellung**
   - Theme: Dunkel/Hell (persistent).
5. **Über**
   - App-**Version** sichtbar (aus `BuildConfig.VERSION_NAME` ableiten), Hinweis „privat".

Schlüsselfelder als Passwort-Felder (maskiert) mit Auge-Umschalter; „Speichern"-Feedback.

---

## 9. Externe KI-APIs — exakt umsetzen

### 9a. Spracheingabe — Groq Whisper (client-seitig)
- Aufnahme: `AudioRecord`, **16 kHz, Mono, 16-bit PCM**; beim Stopp in eine **WAV**-Bytefolge
  verpacken (44-Byte-WAV-Header + PCM).
- Request: `POST https://api.groq.com/openai/v1/audio/transcriptions`
  - Header: `Authorization: Bearer <GROQ_API_KEY>`
  - **multipart/form-data**: `file` = die WAV-Bytes (Dateiname z.B. `audio.wav`,
    Content-Type `audio/wav`), `model` = `whisper-large-v3-turbo`, `language` = `de`,
    `response_format` = `verbose_json`.
  - Antwort: JSON mit `text` (+ Segment-Infos). Den `text` ins Eingabefeld setzen (an bestehenden
    Text anhängen oder ersetzen — Verhalten wie ein Diktiergerät: anhängen ist ok).
- **Stille-Halluzination filtern:** Bei `verbose_json` die Segmente prüfen; sehr hohe
  `no_speech_prob` / sehr niedrige `avg_logprob` → als „nichts gesagt" behandeln, nichts einfügen.
- Während der Aufnahme eine Live-Anzeige (Pegel/Animation). Modell-ID als Konstante (Groq
  mustert Modelle schnell aus — leicht austauschbar halten).

### 9b. Vorlesen — Gemini TTS (client-seitig)
- Request: `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent`
  - **Header:** `x-goog-api-key: <GEMINI_API_KEY>` (NIEMALS `?key=` im Query — landet in Logs).
  - Body: `contents` mit dem Text + `generationConfig.responseModalities:["AUDIO"]` +
    `speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName:"<Stimme>"`.
  - Antwort: `candidates[0].content.parts[0].inlineData.data` = **Base64-PCM**
    (24 kHz, 16-bit, mono, signed little-endian).
- Wiedergabe: Base64 dekodieren → roh als PCM über **`AudioTrack`** (24 kHz/Mono/16-bit) abspielen
  (kein WAV-Wrapping nötig). Stop-Funktion (laufende Wiedergabe abbrechen).
- **Chunking + Pipelining:** Antworttext in ~220-Zeichen-Häppchen an Satzgrenzen schneiden; erstes
  Häppchen sofort generieren+abspielen, nächstes parallel vorausladen.
- `finishReason`/`promptFeedback.blockReason` prüfen, bevor auf `parts` zugegriffen wird
  (sonst Crash bei leerer Antwort).

### 9c. Verbessern („G") — Gemini Text (client-seitig)
- Request: `POST .../v1beta/models/gemini-2.5-flash:generateContent`, Header `x-goog-api-key`.
- Prompt (System/Instruktion): „Verbessere Grammatik und Zeichensetzung des folgenden deutschen
  Textes. **Inhalt und Bedeutung 1:1 lassen**, nichts hinzufügen/weglassen. Gib NUR den
  verbesserten Text zurück." + der Feldinhalt (max ~8000 Zeichen).
- `generationConfig`: `thinkingConfig.thinkingBudget` niedrig (z.B. 0–256) **und**
  `maxOutputTokens` hoch (z.B. 4096–8192) — sonst frisst „Thinking" das Budget und die Antwort
  ist leer mit `finishReason: MAX_TOKENS`.
- Vor dem Lesen `finishReason`/`blockReason` prüfen. Ergebnis ersetzt den Feldinhalt 1:1.

---

## 10. Genutzte Server-Endpoints (Referenz, exakt)

**agent (`:8002`, Bearer):**
- `POST /chat` — siehe §6.
- `GET /categories/detail` → `{ categories:[ { name, count, empty } ] }` (für das Dropdown,
  inkl. leerer Kategorien; hierarchisch „Haupt/Unter").
- `POST /categories` Body `{ name }` → neue (Unter-)Kategorie anlegen.
- `GET /health` (ohne Auth) → Agent erreichbar?

**brain-api (`:8000`, Bearer):**
- `GET /category-counts` → `{ counts:{<name>:<int>}, total_distinct }` (Übersicht).
- `GET /health` → `{ points, embed_model, version, ... }`.
- `POST /search` Body `{ query, user_id:"frank", limit, category? }` → Trefferliste (s. §7b).
- `GET /by-category?category=<name>&user_id=frank` → `{ items:[...] }`.
- `GET /by-parent?parent=<name>&user_id=frank` → `{ items:[...] }`.
- `GET /by-title?title=<titel>&user_id=frank` → ganzes Dokument.
- `PUT /entry` Body `{ doc_id, text, title?, categories?, user_id:"frank" }` → bearbeiten.
- `POST /entry/category` Body `{ doc_id, category, user_id:"frank" }` → Kategorie ändern.
- `DELETE /entry?doc_id=<id>&user_id=frank` → löschen (Papierkorb).
- (optional später: `POST /store`, `/trash*` — fürs Grundgerüst nicht nötig, da Ablegen über den
  Agenten läuft.)

**dashboard (`:8003`, KEINE Auth):**
- `GET /api/overview` → Vital-Karten + Agent-Status (s. §7a).

Antworten haben i.d.R. `ok:true`; Fehler kommen als HTTP 401 (Auth), 404 (nicht gefunden),
422 (Validierung), 5xx. Sauber behandeln, nie still verschlucken.

---

## 11. Theme & Design (kosmisches „Cockpit", 1:1 zum Web-Cortex)

Ruhig, edel, hochwertig — fühlt sich an wie ein gekauftes Premium-Produkt. **Dunkel + Hell**,
umschaltbar (persistent). Beide gleich gut gestaltet.

- **Schriften (Google Fonts):** Space Grotesk (Überschriften/große Zahlen), Inter (UI/Fließtext),
  JetBrains Mono (Labels/Eyebrows/Volltext-Lesebox).
- **Dunkel-Palette:** Hintergrund `#0B0E14`, Flächen `#141A23`, Rahmen `#222C3A`,
  Text `#E8EEF6`, gedämpft `#8A98AC`. Akzente: Iris `#7B7BF5`, Mint `#4FD1B0`, Amber `#F2B65A`,
  Rose `#F2698E`. Dezenter radialer Glow.
- **Hell-Palette:** Hintergrund `#F4F6FB`, Flächen `#FFFFFF`, Rahmen `#E4E8F1`, Text `#1A2230`;
  Akzente etwas kräftiger.
- **Kategorie-Palette (13, zyklisch):** `#7B7BF5 #4FD1B0 #F2B65A #F2698E #5AB0F2 #B68CF5
  #3FD0D6 #9BD05A #F08A5A #5AD0A0 #F25AC0 #5A7BF2 #D0C45A`. Eine Kategorie hat überall dieselbe Farbe.
- **Audio = Orange:** Vorlesen-/Mikrofon-/Senden-Knöpfe in **Orange `#F97316`**; Verbessern-„G"
  in **Grün**; Lösch-X in **Rot/Rose**.
- Form: Radius ~16 dp, weiche Schatten, Karten mit 1 dp-Rahmen, ruhige Mikro-Animationen
  (Balken/Meter weich, Tab-Wechsel, Drawer/Detail).

---

## 12. Beobachtungsschicht (Observability — Pflicht, von Anfang an)

Baue ZUERST (vor den Features) eine schlanke Beobachtungsschicht ein:
- **Strukturiertes Logging als JSON-Lines** in eine Datei im App-internen Speicher; Felder
  `ts, level, module, fn, msg, ctx`. **Log-Pfad beim Start EINMAL ausgeben** (Logcat).
  Zusätzlich auf **Logcat** spiegeln mit festem TAG **`FRANK_CORTEX`** (Live:
  `adb logcat -s FRANK_CORTEX`).
- **Globaler Crash-Fänger** (`Thread.setDefaultUncaughtExceptionHandler`): nichts stirbt still —
  voller Kontext + Stacktrace ins Log, bevor die App fällt.
- **Logik-Sonden** `probe(bedingung, meldung, ctx)` an Kernstellen (VPN-Up, Netzwerkaufrufe,
  Speicher-Bestätigung).
- **Intent-Checkpoints** in einen eigenen Kanal (TAG **`LOGIC`**, `kind:"CHECKPOINT"`, Felder
  `step, intent, expected, actual, ok`) an genau diesen fachlichen Schritten:
  1. VPN-Schalter an → Tunnel oben (`expected: connected`)
  2. Chat senden → Antwort erhalten (`action` geloggt)
  3. Speicher-Bestätigung „ja" → `stored:true`
  4. Mikrofon → Groq liefert Text
  5. Vorlesen → Audio abgespielt
  Damit ist live prüfbar (`adb logcat -s LOGIC`), ob die Logik so funktioniert wie gemeint.
- **Keine Secrets ins Log** (Keys/Token maskieren). Log-Datei nicht versionieren.

---

## 13. Versionsanzeige

`versionName`/`versionCode` in `build.gradle.kts` setzen; Version im Tab „Über" sichtbar aus
`BuildConfig.VERSION_NAME`. Beim App-Start einmal `version=<x.y.z>` ins Log schreiben.

---

## 14. ⚠ WICHTIGE BEKANNTE FALLEN (aus geprüftem Erfahrungswissen — unbedingt beachten)

- **VPN zuerst:** Kein Aufruf an `10.8.0.1` ohne aktiven Tunnel. Vor Server-Calls Tunnel-Status
  prüfen, sonst freundlicher Hinweis statt Timeout/Crash.
- **Nur ein VPN gleichzeitig:** Läuft die separate WireGuard-App, scheitert der App-Tunnel —
  klar melden.
- **Gemini-Key:** IMMER Header `x-goog-api-key`, **nie** `?key=` im Query (landet in Logs/Referer).
- **Gemini leere Antwort:** Bei 2.5/3-Modellen frisst „Thinking" das `maxOutputTokens` →
  `finishReason: MAX_TOKENS` mit leerem Text. Fix: `maxOutputTokens` hoch + `thinkingBudget`
  niedrig; vor `parts`-Zugriff `finishReason`/`blockReason` prüfen.
- **Gemini-TTS liefert Base64-PCM** (24 kHz/16-bit/mono), KEIN fertiges Audio-File → erst Base64
  dekodieren, dann als PCM über `AudioTrack` abspielen (oder WAV-Header voranstellen).
- **Groq-Whisper halluziniert bei Stille** → `response_format=verbose_json` + `no_speech_prob`/
  `avg_logprob`-Filter; Modell-ID nicht hartkodieren (Groq deprecatet schnell).
- **Groq braucht echtes multipart** mit `file`-Feld (nicht roher Body).
- **Stabile `session_id`** über die ganze Unterhaltung halten — sonst bricht der
  2-Schritt-Speicherdialog des Agenten.
- **Kein Streaming** bei `/chat` — eine vollständige JSON-Antwort, recall darf ein paar Sekunden dauern.
- **`EncryptedSharedPreferences`:** korrekt mit MasterKey initialisieren; bei Schlüssel-Reset/
  Backup-Restore robust bleiben (try/catch, neu anlegen statt crashen).
- **Compose-State über Tab-Wechsel** mit `rememberSaveable`/ViewModel halten, nicht mit `remember`
  (sonst gehen Eingaben/Scroll beim Tabwechsel verloren).
- **Fehler nie still schlucken** (Direktive Resilient Bugfixing): jeden Fehler ins Log + sinnvolles
  Fallback-Verhalten; Funktionalität nie „wegfixen".

---

## 15. Abnahme-Kriterien (Definition of Done fürs Grundgerüst)

1. `./gradlew assembleDebug` baut grün; App startet ohne Crash.
2. WireGuard-Konfig importierbar; Schalter fährt den Tunnel hoch/runter; Status korrekt farbig.
3. Bei aktivem Tunnel: Gespräch-Tab schickt an `agent /chat`, zeigt `reply`, Meta-Zeile und
   Options-Knöpfe; 2-Schritt-Speichern funktioniert.
4. Kategorie-Dropdown gefüllt; „➕ Neue Kategorie…" legt eine an.
5. Dashboard zeigt Gesamtzahl + Spektrum + Vital-Karten; Suche, Kategorie-Filter, Detail-Ansicht,
   Bearbeiten, Kategorie ändern und Löschen funktionieren.
6. Mikrofon → Groq-Text erscheint im Feld; „G" verbessert den Text; Vorlesen liest Antworten vor.
7. Einstellungen speichern alle Keys sicher; Theme-Umschalter wirkt; Version sichtbar.
8. Observability: Log-Datei + `FRANK_CORTEX`/`LOGIC`-Logcat vorhanden; ein provozierter Fehler
   landet mit Kontext im Log.

---

## 16. Tabu / nicht tun

- Keine erfundenen Endpoints/Felder — nur die aus §10/§9.
- Keine öffentliche/Mehrbenutzer-Logik, kein Login, kein Marketing, kein Play-Store-Kram, kein i18n.
- Keine fremde WireGuard-App fernsteuern — eigener Tunnel.
- Keine Secrets im Klartext speichern oder loggen.
- Oberfläche bleibt **deutsch**.
- Keine Funktion „wegfixen", um Fehler zu unterdrücken.
```
