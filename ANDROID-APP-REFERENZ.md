# Android-App-Referenz — verbindliche Standard-Bausteine für alle meine Apps

> **Stand:** 29.08.2026, 11:19 Uhr · **Gilt für:** jede neue oder erweiterte Android-App von Frank
> **Referenz-Apps im Repo:** `~/proggs/PerfectMoment`, `~/proggs/CortexAndroid`,
> `~/proggs/BestJournalAndroid`, `~/proggs/TerminalVoiceOverlay-Windows`

---

## 0. Wie diese Datei zu benutzen ist (Anweisung an die KI)

Sage ich **„nutze die Referenzdatei"**, **„bau nach der Android-Referenz"** oder füge ich diese Datei in
den Chat ein, dann gilt:

1. **Jeder Baustein A bis P wird eingebaut — ohne Rückfrage.** Es wird *nicht* gefragt „möchtest
   du Hell/Dunkel-Modus?" oder „soll Vorlesen rein?". Die Antwort ist immer ja.
2. **Einzige Ausnahme:** Ein Baustein ergibt in dieser konkreten App *nachweislich* keinen Sinn (z. B.
   Vorlesen in einer App, die überhaupt keinen Text anzeigt). Dann — und nur dann — **melde es einmal
   kurz** in dieser Form und lass ihn weg:

   > ⚠️ **Baustein D (Vorlesen)** ergibt in dieser App keinen Sinn, weil <ein Satz Begründung>.
   > Ich lasse ihn weg. Sag Bescheid, wenn er trotzdem rein soll.

   Kein Baustein wird stillschweigend weggelassen. Im Zweifel: **einbauen**.
   **Baustein M (echte Umlaute) ist von dieser Ausnahme ausgenommen** — er gilt immer, in jeder App.
3. **Diese Datei ersetzt nicht die Projekt-Regeln** aus `CLAUDE.md` (Version-Bump, Commit+Push vor Build,
   Deutsch mit echten Umlauten, Secrets aus `~/SK/`, Bug-Almanach-Kurzcheck). Sie kommt *zusätzlich*.
4. **Reihenfolge beim Neubau:** Grundgerüst (Kap. 17) → Theme (A) → Kopfleiste (C) → Einstellungen (G) →
   Fold-Layout (B) → Fehler-/Lade-/Leerzustände (L, von Anfang an mitdenken) → App-Logik →
   Vorlesen (D/E) → Transkription (F) → Suche (K) → App-Sperre (I) → Sicherung (J) →
   Version sichtbar (H). **Baustein M (Umlaute) und Baustein N (Optik) laufen durchgehend mit** —
   bei jedem Text und jedem Bildschirm, der entsteht, nicht als Schritt am Ende.
5. **Bestehende App erweitern:** Zuerst prüfen, welche Bausteine schon da sind (Checkliste Kap. 18),
   dann nur die fehlenden nachrüsten. Nichts doppelt bauen, nichts Bestehendes wegwerfen.

---

## 1. Baustein A — Hell- und Dunkelmodus in Goldfarben ⭐ PFLICHT

**Was:** Jede App hat beide Modi, vollständig ausgearbeitet, mit Gold als Leitfarbe.

**Regeln**

- Drei Modi: `hell`, `dunkel`, `system` (Systemvorgabe folgen). Voreinstellung: `system`.
- Die Wahl wird **persistent** gespeichert (`EncryptedSharedPreferences` bzw. DataStore) und beim Start
  sofort angewandt — kein Aufblitzen des falschen Modus.
- Beide Paletten werden **komplett** durchgezeichnet: Hintergrund, Fläche, erhöhte Fläche, Rahmen,
  Text, gedämpfter Text, Eingabefeld, Chip. Kein Modus ist „die schnelle Variante".
- **Dynamic Color (Material You) ist AUS.** Gold ist die Markenfarbe, sie darf nicht vom Systemhintergrund
  überschrieben werden.
- Statusleiste und Navigationsleiste ziehen mit (`enableEdgeToEdge`, `isAppearanceLightStatusBars`).
- Kontrast: Text auf Fläche mindestens **4,5:1** (WCAG AA), große Überschriften mindestens 3:1. Bei
  eigenen Gold-Abwandlungen den Kontrast nachrechnen, nicht schätzen.

**Verbindliche Gold-Palette** (Ausgangswerte; Abweichung nur bewusst und mit Kontrastprüfung)

| Rolle | Dunkelmodus | Hellmodus |
|---|---|---|
| Hintergrund | `#121212` | `#FAF7F0` |
| Fläche / Karte | `#181818` | `#FFFFFF` |
| Erhöhte Fläche (Dialog, Hover) | `#282828` | `#F4EFE3` |
| **Primär (Gold)** | `#E3B341` | `#8B6914` |
| Gold gedämpft / Sekundär | `#C9922B` | `#A9812A` |
| Auf Gold (Text/Icon auf goldener Fläche) | `#1A1408` | `#FFFFFF` |
| Akzent warm (Kupfer, Aktionen) | `#C25E00` | `#A34F00` |
| Text primär | `#EDE7DA` | `#1B1710` |
| Text gedämpft | `#A79C86` | `#6B6151` |
| Rahmen | `#2C2620` | `#E6DFCF` |
| Eingabefeld | `#141414` | `#F7F3EA` |

Semantische Farben (in beiden Modi gleich, Gold bleibt der Marke vorbehalten):
Erfolg `#4CAF7D` · Warnung `#FFB300` · Fehler `#FF5252` · Info `#4ECDC4`.

**Vorlage im Repo:** `BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/theme/Color.kt`
(Gold-Familie `WarmGold`/`WarmCopper`/`WarmSand`) und
`CortexAndroid/app/src/main/java/de/frank/cortex/ui/theme/` (saubere Zwei-Paletten-Struktur).

---

## 2. Baustein B — Galaxy Z Fold 8, Außenbildschirm ist der Standard ⭐ PFLICHT

**Was:** Zielgerät ist mein **Galaxy Z Fold 8 (SM-F971B)**. Der **Außenbildschirm (Cover-Display)** ist
der Normalfall, nach dem gestaltet und getestet wird. Der aufgeklappte Innenbildschirm ist der Bonus.

**Regeln**

- **Basis-Layout auf das schmale, hohe Cover-Display auslegen** (~360–400 dp Breite, sehr hohes
  Seitenverhältnis um 21:9). Alles Wichtige muss dort ohne horizontales Scrollen und ohne
  abgeschnittene Knöpfe bedienbar sein.
- **Die echten Maße kommen vom Gerät, nie aus dem Gedächtnis:** vor dem Layout einmal
  `adb shell wm size` und `adb shell wm density` ausführen (aufgeklappt und zugeklappt) und die Werte
  im Projekt-README notieren.
- **Aufgeklappt sauber mitskalieren:** `WindowSizeClass` auswerten
  (`androidx.compose.material3:material3-window-size-class`). Ab `Medium`/`Expanded` Breite: mehrspaltig,
  Liste und Detail nebeneinander, größere Ränder — **niemals** ein auf Handybreite gestrecktes Layout.
- **Faltvorgang darf die Activity nicht neu starten:**
  `android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"`,
  `android:resizeableActivity="true"`, **keine** feste `screenOrientation`. Zustand über `ViewModel` +
  `rememberSaveable` halten.
- Bedienelemente mindestens **48 dp** Touchfläche, Daumenzone unten bevorzugt.
- Split-Screen und Multi-Window müssen funktionieren (das Fold wird oft geteilt benutzt).
- **Testen:** echtes Gerät zuerst; Emulator ausschließlich über die Werkzeugkette
  `~/proggs/Werkzeuge/fold8-emulator` (`Start-Fold8.ps1`) starten — direkt gestartete Emulatoren landen
  außerhalb des Bildschirms.
- Screenshots für den Play Store immer in beiden Zuständen (zugeklappt und aufgeklappt).

---

## 3. Baustein C — Kopfleiste: Theme-Knopf und Einstellungs-Knopf ⭐ PFLICHT

**Was:** Auf dem **Hauptbildschirm oben rechts** stehen zwei Knöpfe nebeneinander:

```
┌───────────────────────────────────────────────┐
│  <App-Titel / Logo>          [ ☀/🌙 ]  [ ⚙ ]  │
└───────────────────────────────────────────────┘
```

**Regeln**

- **Links der Theme-Knopf**, rechts daneben der **Einstellungs-Knopf** (Zahnrad). Diese Reihenfolge ist
  fest — ich greife sie blind.
- Der Theme-Knopf schaltet direkt um und zeigt den **aktuellen** Zustand als Icon:
  `Icons.Default.LightMode` (hell) / `Icons.Default.DarkMode` (dunkel) /
  `Icons.Default.BrightnessAuto` (system). Ein Tipp = nächster Modus im Kreis (hell → dunkel → system).
- Beide Knöpfe: 38–40 dp Fläche, abgerundetes Quadrat (Radius 12 dp), goldener Rahmen oder goldene
  Tönung, `contentDescription` auf Deutsch gesetzt.
- Die Leiste respektiert `statusBarsPadding()`.
- Auf Unterseiten bleibt der Einstellungs-Knopf erreichbar (Kopfleiste wiederverwenden), der Theme-Knopf
  darf dort entfallen.

**Vorlage im Repo:** `CortexAndroid/app/src/main/java/de/frank/cortex/ui/common/CortexTopBar.kt`

---

## 4. Baustein D — Vorlesen (TTS) mit Absatz-Pipeline ⭐ PFLICHT

**Was:** Überall, wo längerer Text steht, sitzt ein **kleiner Lautsprecher-Knopf**. Ein Tipp liest den
Text vor. Die Wiedergabe startet fast sofort, auch bei sehr langen Texten, und läuft ohne Lücken durch.

### 4.1 Die drei Engines (alle drei einbauen, umschaltbar in den Einstellungen)

| Engine | Kennung | Wofür | Schlüssel |
|---|---|---|---|
| **Google Chirp 3 HD** | `google_cloud` | Standard, beste Qualität | Gemini-/Google-Cloud-API-Key |
| **Meine eigene Stimme** | `qwen_clone` | geklonte Stimme (Baustein E) | Alibaba-DashScope-Key |
| Microsoft Edge TTS | `edge_tts` | kostenloser Rückfall | keiner |

- Google-Endpunkt: `https://texttospeech.googleapis.com/v1/text:synthesize`, Stimmen der Form
  `de-DE-Chirp3-HD-<Name>` (Kore, Zephyr, Leda, Puck, Charon, Orus …). **Wichtig:** Chirp-3-HD-Stimmen
  kennen keinen `pitch`-Parameter — `pitch` nur an Nicht-Chirp-Stimmen senden, sonst Fehler 400.
- **Stimmenauswahl im Einstellungs-Bildschirm:** vollständige Liste, nach Geschlecht gruppiert, mit
  **Probe-abspielen-Knopf** je Stimme und **Favoriten**. Sprechtempo als Regler (0,5–2,0).
- Vollständiger Stimmen-Katalog als Vorlage:
  `PerfectMoment/app/src/main/java/de/frank/perfectmoment/tts/TtsCatalog.kt`

### 4.2 Die Absatz-Pipeline (so und nicht anders)

Das ist der Kern — abgeschaut von **CortexAndroid** (`ui/chat/ChatViewModel.kt`):

1. **Ein Absatz = eine Vorlese-Einheit.** Der Text wird an Leerzeilen in Absätze zerlegt. Absätze werden
   *nicht* zusammengelegt und *nicht* mitten drin geteilt.
2. **Nur überlange Absätze werden geteilt** — Sicherheitsgrenze **1000 Zeichen** (API-Limit), und dann
   ausschließlich an **Satzgrenzen**.
3. **Vorausschauendes Synthetisieren:** Während Absatz *n* vorgelesen wird, sind Absatz *n+1* und *n+2*
   bereits beim TTS-Dienst in Arbeit (`PREFETCH_AHEAD = 2`). Ist Absatz *n* fertig, liegt *n+1* schon
   als Audio bereit und startet **nahtlos**.
4. **Pause zwischen den Absätzen: rund 1 Sekunde** (`PARAGRAPH_GAP_MS = 1000`) — hörbarer Atem, kein
   Loch.
5. **Bei Streaming-Text** (die Antwort läuft noch ein): Vorlesen beginnt, sobald der **erste vollständige
   Absatz** da ist — nie bei einzelnen Wörtern, sonst bricht die Synthese zu früh ab.
6. **Ergebnis:** Der erste Ton kommt nach wenigen hundert Millisekunden statt nach dem Synthetisieren
   des ganzen Textes. Ein 20-Minuten-Text startet genauso schnell wie ein einzelner Absatz.

### 4.3 Vorlesen läuft weiter, auch bei ausgeschaltetem Bildschirm

- Das Vorlesen läuft in einem **Vordergrunddienst**
  (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, Dienst-Typ `mediaPlayback`) — es hört **nicht** auf, wenn der
  Bildschirm ausgeht oder ich die App verlasse. Genau dafür lese ich lange Texte vor.
- **Medien-Benachrichtigung** mit Titel des Vorgelesenen und den Knöpfen **Pause / Weiter / Stopp**,
  auch auf dem Sperrbildschirm sichtbar.
- Die Bedienelemente vom Kopfhörer und aus dem Auto funktionieren (`MediaSession`).
- **Audiofokus** anfordern und wieder abgeben; bei Anruf pausieren, danach fortsetzen.
- **Wachhaltung nur solange gesprochen wird** — der Dienst beendet sich selbst, sobald der letzte
  Absatz durch ist. Kein Dienst, der still im Hintergrund weiterläuft.
- Beim Zurückkehren in die App zeigt sie den laufenden Stand an (welcher Absatz gerade dran ist).
- Ab Android 13 vorher die **Benachrichtigungs-Berechtigung** erfragen (mit einem Satz Begründung,
  siehe Baustein L) — ohne sie gibt es keine Bedienknöpfe.

### 4.4 Robustheit (Pflicht, nicht optional)

- **Gleichzeitigkeit begrenzen** (Semaphore), sonst laufen die TTS-Dienste ins Rate-Limit.
- **429 / Rate-Limit:** exponentiell warten und erneut versuchen; Wartezeit protokollieren.
- **Leere oder abgelehnte Antwort:** Absatz halbieren und erneut senden (Retry-Split).
- **Fehler, die die ganze Sitzung betreffen** (ungültiger Schlüssel, Kontingent leer): Pipeline sofort
  anhalten und **die echte Fehlermeldung anzeigen** — nicht jeden Absatz still überspringen.
- **Text vorher säubern:** Markdown-Zeichen, Code-Blöcke, URLs und Emoji entfernen bzw. ersetzen, damit
  nicht „Sternchen Sternchen" vorgelesen wird
  (Vorlage: `CortexAndroid/.../ui/chat/ChatSpeechSanitizer.kt`).
- **Sichtbarer Zustand:** Der Lautsprecher-Knopf zeigt „lädt" / „spricht" / „aus" und stoppt bei
  erneutem Tipp sofort.

---

## 5. Baustein E — Meine eigene Stimme (Alibaba / Qwen Voice Clone) ⭐ PFLICHT

**Was:** Ich kann in den Einstellungen **eine eigene Stimme aufnehmen**, sie wird geklont, und danach
wird mit meiner eigenen Stimme vorgelesen.

**Regeln**

- Dienst: **Alibaba Model Studio (DashScope, internationaler Endpunkt)**
  `https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation`
- **Klonen und Synthetisieren müssen dasselbe Modell benutzen** (z. B. `qwen3-tts-vc-…`) — sonst wird
  die Stimm-ID abgelehnt. Den Modellnamen zentral als *eine* Konstante halten.
- **Aufnahme-Ablauf:** vorgegebener Vorlese-Text auf dem Bildschirm → Aufnahme (24 kHz mono, 16 bit) →
  Upload → Stimm-ID kommt zurück → **ich vergebe einen Namen** → Stimme erscheint in der Auswahlliste.
- **Mehrere eigene Stimmen** sind möglich: Liste mit Namen, eigener Reihenfolge, Umbenennen und Löschen.
- Die zurückgelieferte Audio-URL kommt teils als `http://` — vor dem Abspielen auf `https://` heben,
  sonst blockt Android sie.
- Vorlagen: `PerfectMoment/app/src/main/java/de/frank/perfectmoment/tts/QwenTtsPlayer.kt`,
  `QwenVoiceEnrollment.kt`, `QwenVoiceDirectory.kt`, `audio/VoiceSampleScript.kt`

---

## 6. Baustein F — Transkription: Whisper large-v3-turbo über Groq ⭐ PFLICHT

**Was:** Überall, wo ich Text eingeben kann, sitzt ein **Mikrofon-Knopf**. Ich spreche, es wird
transkribiert und eingefügt.

### 6.1 Aufnahme und Anfrage

- Aufnahme: `AudioRecord`, Quelle `VOICE_RECOGNITION`, **16 000 Hz, mono, PCM 16 bit**, als WAV.
- Groq-Endpunkt `https://api.groq.com/openai/v1/audio/transcriptions` mit:
  - `model` = **`whisper-large-v3-turbo`**
  - `language` = `de`
  - `temperature` = `0`
  - `response_format` = `verbose_json` ← **zwingend**, sonst fehlen die Segment-Metriken, die die
    Halluzinations-Filter brauchen.
- **Upload-Grenze / HTTP 413:** Groq lehnt zu große Uploads ab (25 MB im Free-Plan, in der Praxis auch
  im Dev-Plan schon ab rund 37 MB). Bei 16 kHz mono sind das etwa **13 Minuten**. Ein 413 ist **nicht**
  wiederholbar. Deshalb: Audio **über 20 MB vor dem Senden schneiden** (Zielgröße ~16 MB je Teil,
  Schnitt an einer Sprechpause im letzten 45-Sekunden-Fenster), Teile einzeln transkribieren und die
  Texte zusammensetzen. Fällt ein Teil aus, gehen nur dessen Sekunden verloren.
  *(Vorfall 29.08.2026: 15,4 Minuten Diktat = 29,5 MB = 413 = kompletter Text weg.)*
- **Die Aufnahme wird nie gelöscht, bevor der Text sicher angekommen ist.**

### 6.2 Die vier Stille-Halluzinations-Fixes ⭐ alle vier, in dieser Reihenfolge

Whisper erfindet bei Stille Sätze („Vielen Dank fürs Zuschauen", „Untertitel des ZDF"). Dagegen vier
Schichten — abgeschaut von **TerminalVoiceOverlay** und **PerfectMoment**
(`audio/WhisperHallucinationFilter.kt`, `audio/SpeechAnalyzer.kt`):

| # | Schicht | Wirkung |
|---|---|---|
| **1** | **Stille-Erkennung vor dem Upload** (VAD): Energie je 20-ms-Frame; weniger als **10 %** laute Frames → gar nicht erst senden | spart Geld und verhindert die Halluzination an der Wurzel |
| **2** | **Segment-Metriken aus `verbose_json`:** verwerfen bei `no_speech_prob > 0,6` **und** `avg_logprob < −1,0`; oder `compression_ratio > 2,4` (Wiederholungsschleife); oder Segment kürzer als **0,4 s** bei hoher `no_speech_prob` | fängt die typischen Erfindungen |
| **3** | **Zeitstempel gegen die Stille-Erkennung abgleichen:** Segmente, deren Zeitfenster im Audio still war, verwerfen. **Sicherung:** Würden *alle* Segmente fallen, wird das Ergebnis von Schicht 2 behalten (dann liegt eher ein Zeitstempel-Versatz vor als eine Halluzination) | fängt Erfundenes mitten in Pausen |
| **4** | **Floskel-Blocklist** — greift nur, wenn **alle drei** Bedingungen zugleich gelten: (1) Ausgabe kurz (≤ 6–8 Wörter, ≤ 64 Zeichen), (2) exakter Treffer in der Liste nach Normalisierung, (3) Stille-Kontext (insgesamt < 600 ms laute Zeit) | „Vielen Dank" nach Fehlklick weg — bewusst gesprochenes „Vielen Dank" bleibt |

**Goldene Regel zu Schicht 4:** Eine Floskel **niemals allein wegen des Wortlauts** verwerfen. Nur die
Kombination aus Kürze + exaktem Treffer + Stille-Kontext darf löschen.

Blocklist-Grundstock (deutsch und englisch, erweiterbar): „vielen dank", „vielen dank fürs zuschauen",
„vielen dank für eure/ihre aufmerksamkeit", „bis zum nächsten mal", „bis zum nächsten video",
„untertitel", „untertitel des zdf", „untertitelung des zdf für funk", „untertitel der amara org
community", „der text ist nicht auf deutsch", „thank you", „thank you for watching",
„thanks for watching", „please subscribe".

**Jede verworfene Zeile wird protokolliert** (welche Schicht, welche Werte, gekürzter Text) — sonst ist
später nicht nachvollziehbar, warum etwas fehlt.

**Vorhandene Tests übernehmen:**
`PerfectMoment/app/src/test/java/de/frank/perfectmoment/audio/WhisperHallucinationFilterTest.kt`

---

## 7. Baustein G — Einstellungs-Bildschirm ⭐ PFLICHT

Erreichbar über das Zahnrad aus Baustein C. Enthält **immer mindestens** diese drei Blöcke:

### 7.1 Vorlesen
Engine-Auswahl (Google Chirp 3 HD / Meine Stimme / Edge) · Stimmenliste mit Probe-Knopf und Favoriten ·
Sprechtempo-Regler · Feld **Google-/Gemini-API-Key** · Feld **Alibaba-DashScope-API-Key** ·
Knopf „Eigene Stimme aufnehmen" mit Verwaltung der geklonten Stimmen.

### 7.2 Spracheingabe
Feld **Groq-API-Key** · Modellanzeige `whisper-large-v3-turbo` · Schalter für die Filter-Schichten
(Voreinstellung: alle an) · optionaler Testknopf „Aufnahme prüfen".

### 7.3 Darstellung
Hell / Dunkel / System · falls vorhanden: Schriftgröße.

**Regeln für die Schlüssel**

- Schlüssel **immer in `EncryptedSharedPreferences`** (`androidx.security:security-crypto`,
  `MasterKeys.AES256_GCM_SPEC`), nie im Klartext, nie im Repo, nie im Code hartcodiert.
- Eingabefelder maskiert mit Augen-Knopf zum Anzeigen; „Einfügen"-Knopf aus der Zwischenablage.
- **Testknopf je Schlüssel**, der einen echten Mini-Aufruf macht und Erfolg oder Fehler im Klartext
  meldet.
- Vorbelegung beim ersten Start aus `~/SK/<Projekt>/.env`, falls beim Bauen vorhanden — nie ins Repo.
- Vorlage: `PerfectMoment/app/src/main/java/de/frank/perfectmoment/data/settings/SecureSettings.kt`

---

## 8. Baustein H — Version sichtbar mit Zeitstempel ⭐ PFLICHT

- In `app/build.gradle.kts`:
  ```kotlin
  versionName = "1.0.27"
  buildConfigField("String", "VERSION_BUMPED_AT", "\"29.08.2026, 11:19 Uhr\"")
  ```
  **Immer beide zusammen**, Uhrzeit mit Doppelpunkt, echte Systemzeit
  (`Get-Date -Format "dd.MM.yyyy HH:mm"`) — nie geschätzt, nie aus dem Kontext übernommen.
- Anzeige unten im Einstellungs-Bildschirm, **abgeleitet aus `BuildConfig`**, nie doppelt hartcodiert:
  ```kotlin
  Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})")
  ```
- Beim App-Start einmal ins Log schreiben.
- `versionCode` bei jedem Release +1 (sonst lehnt der Play Store ab).

---

## 9. Baustein I — Biometrische App-Sperre ⭐ PFLICHT

**Was:** Die App lässt sich per Fingerabdruck oder Gesicht sperren.

**Regeln**

- Schalter in den Einstellungen unter **Sicherheit**: „App-Sperre" (Voreinstellung: aus). Beim
  Einschalten wird einmal biometrisch bestätigt — sonst sperrt man sich versehentlich selbst aus.
- `androidx.biometric:biometric` mit
  `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` — **immer auch PIN/Muster als Rückfall** zulassen, damit ein
  nasser Finger nicht die App blockiert.
- **Auslöser:** beim Kaltstart und wenn die App länger als **1 Minute** im Hintergrund war (Zeitspanne
  in den Einstellungen wählbar: sofort / 1 min / 5 min / nie). Umgesetzt über einen
  `DefaultLifecycleObserver` am `ProcessLifecycleOwner`.
- Solange gesperrt: **Inhalt verdeckt**, kein Durchblitzen von Daten. Zusätzlich
  `FLAG_SECURE` setzen, damit nichts in der App-Umschalter-Vorschau landet.
- Sperre gilt für die ganze App, nicht je Bildschirm. Kein eigenes Passwort erfinden — nur das
  Gerät-Verfahren.
- Vorlage: `PerfectMoment/app/src/main/java/de/frank/perfectmoment/security/AppLockManager.kt`

---

## 10. Baustein J — Sicherung und Wiederherstellung ⭐ PFLICHT

**Was:** Meine Daten sind nie an ein Gerät gefesselt. Zwei Wege, beide in den Einstellungen unter
**Sicherung**:

### J.1 Datei-Export und -Import (immer)
- **Exportieren:** alle Inhalte als eine Datei (JSON oder ZIP mit Anhängen) über
  `ACTION_CREATE_DOCUMENT`. Dateiname mit Zeitstempel: `<app>-sicherung-JJJJ-MM-TT-HHmm.json`.
- **Importieren:** über `ACTION_OPEN_DOCUMENT`, mit **Vorschau vor dem Überschreiben**
  („X Einträge werden eingespielt, Y bestehende bleiben") und der Wahl „zusammenführen" oder „ersetzen".
- Die Sicherungsdatei trägt eine **Schema-Version**; beim Import wird sie geprüft und, wenn nötig,
  migriert. Eine unbekannte, höhere Version wird abgelehnt statt halb eingelesen.
- **API-Schlüssel gehören nicht in die Sicherung.**

### J.2 Google-Drive-Sicherung (wenn die App mehr als Kleinkram speichert)
- Anmeldung über **Credential Manager** (`androidx.credentials` + Google-ID), nicht über die alte
  Sign-In-API.
- Ablage ausschließlich im **`appDataFolder`** (`DriveScopes.DRIVE_APPDATA`) — die App sieht damit
  **nie** die übrigen Dateien auf meinem Drive. Das ist Bedingung, kein Vorschlag.
- Funktionen: „Jetzt sichern", „Wiederherstellen", „Sicherung löschen" (mit Prüfung, dass der
  `appDataFolder` danach wirklich leer ist), Anzeige von Zeitpunkt und Größe der letzten Sicherung.
- Automatische Sicherung optional (täglich, nur im WLAN).
- Vorlagen: `BestJournalAndroid/.../data/remote/googledrive/DriveBackupManager.kt` und
  `DriveRestoreManager.kt`, dazu `NEMS/.../data/remote/`.

### J.3 Android-Systemsicherung
`res/xml/backup_rules.xml` und `data_extraction_rules.xml` pflegen: Datenbank und Einstellungen ja,
**Schlüssel und Zwischendateien ausdrücklich ausschließen**.

---

## 11. Baustein K — Volltextsuche über alle Inhalte ⭐ PFLICHT

**Was:** Ein Suchfeld, das alles findet, was die App gespeichert hat.

**Regeln**

- **Zugang aus der Kopfleiste** (Lupe) oder als festes Feld oben auf dem Hauptbildschirm.
- Technik: **Room mit FTS4** (`@Fts4`-Spiegeltabelle) — nicht `LIKE '%…%'` über die Haupttabelle,
  das wird ab ein paar tausend Einträgen zäh.
- **Sucht über alle Inhaltsarten** der App, nicht nur über Titel. Ergebnisse nach Art gruppiert.
- **Während des Tippens** suchen, mit rund 250 ms Entprellung; der Treffer zeigt die **Fundstelle im
  Text mit hervorgehobenem Suchwort**.
- Groß-/Kleinschreibung und Umlaute egal: „Uber", „über" und „ueber" finden dasselbe (Normalisierung
  beim Indizieren *und* bei der Anfrage).
- **Letzte Suchanfragen** merken (die letzten 10, löschbar).
- Leeres Ergebnis ist ein echter Zustand mit Text: „Nichts gefunden für ‚…' " plus Knopf „Suche leeren".

---

## 12. Baustein L — Fehler, Ladezustände, Leerzustände ⭐ PFLICHT

**Grundregel: Es gibt keinen stillen Fehlschlag.** Wenn etwas nicht klappt, sehe ich das — sofort, auf
Deutsch, mit einem Weg nach vorn.

**Fehler**

- Jede Fehlermeldung nennt **was nicht ging**, **warum** und **was ich tun kann** — in einem Satz,
  ohne Fachkauderwelsch:
  > „Vorlesen fehlgeschlagen: Der Google-Schlüssel wurde abgelehnt (401). Prüf ihn in den
  > Einstellungen." · [Einstellungen öffnen] [Wiederholen]
- **Jeder Fehler, der wiederholbar ist, bekommt einen Wiederholen-Knopf.** Netzwerkfehler nie als
  „Unbekannter Fehler" abtun.
- **Niemals `catch {}` leer lassen** und niemals eine Funktion abschalten, um einen Fehler
  loszuwerden (Direktive 3, Funktionalitäts-Erhaltung).
- Technische Einzelheiten (Statuscode, Ausnahme) gehen ins Log, nicht in die Meldung — aber das Log
  ist über die Einstellungen einsehbar und teilbar.
- Kurze Bestätigungen als Snackbar, echte Probleme als Dialog oder als Streifen im Bildschirm. Kein
  Toast für Wichtiges.

**Ladezustände**

- Alles über 200 ms zeigt einen Zustand: Fortschritt (wenn messbar) oder Platzhalter-Gerüst
  („Skeleton") in Listen. Kein leerer weißer Bildschirm, kein eingefrorenes Bild.
- Laufende Vorgänge sind **abbrechbar**, wenn sie länger als ein paar Sekunden dauern.
- Der auslösende Knopf ist während des Vorgangs gesperrt und zeigt den Zustand — sonst tippe ich
  doppelt.

**Leerzustände**

- Jede Liste hat einen ausgearbeiteten Leerzustand: Symbol, ein Satz was hier später steht, und der
  **Knopf, der ihn füllt** („Ersten Eintrag anlegen"). Kein „Keine Daten".
- Unterschied beachten: „noch nichts angelegt" ≠ „Filter/Suche ohne Treffer" ≠ „Laden fehlgeschlagen" —
  drei verschiedene Bildschirme.

**Berechtigungen**

- Vor der Systemabfrage kurz erklären, wofür (Mikrofon, Benachrichtigungen). Bei dauerhafter Ablehnung
  Knopf, der direkt in die System-Einstellungen der App springt.

---

## 13. Baustein M — Nur echte deutsche Umlaute ⭐ PFLICHT

**Grundregel: Innerhalb der App erscheinen ausschließlich echte Umlaute — ä ö ü Ä Ö Ü ß.**
Niemals die Ersatzschreibung „ae", „oe", „ue", „ss". Das gilt für **jeden** Text, den ich zu sehen
bekomme, ohne Ausnahme:

| Textart | Regel |
|---|---|
| **Angezeigter Text** (Oberfläche) | Alle `strings.xml`, alle Beschriftungen, Knöpfe, Überschriften, Platzhalter, `contentDescription`, Fehler- und Hinweismeldungen, Benachrichtigungen, Widget-Texte, App-Name |
| **Transkribierter Text** (Baustein F) | Was aus der Spracherkennung kommt, wird mit echten Umlauten eingefügt |
| **KI-erzeugter Text** | Was ein Sprachmodell für die App schreibt (Zusammenfassungen, Antworten, Vorschläge), enthält echte Umlaute |
| **Was ich selbst eintippe** | bleibt unangetastet — meine Eingabe wird nie umgeschrieben |

### M.1 Oberfläche
- Alle Textdateien **UTF-8 ohne BOM**. Keine `ä`-Escapes und keine HTML-Entitäten in
  `strings.xml` — dort steht das Zeichen selbst.
- Kein „ae/oe/ue/ss" in Quelltext-Zeichenketten, auch nicht in Log-Meldungen und Kommentaren.
- **Prüfung als Test:** Ein Unit-Test läuft über `res/values/strings.xml` und schlägt fehl, sobald ein
  Wort aus der Ersatzschreib-Liste auftaucht (siehe M.4). Damit rutscht es nicht durch.

### M.2 Transkription
- An der Quelle richtig anfordern: `language = "de"` (Baustein F). Whisper liefert damit von sich aus
  echte Umlaute — die Ersatzschreibung entsteht fast nie im Modell, sondern erst durch falsche
  Weiterverarbeitung.
- **Auf dem Weg zum Textfeld nichts kaputtmachen:** keine ASCII-Normalisierung, kein
  `Normalizer.NFD` mit anschließendem Entfernen der Akzentzeichen, keine Transliteration, kein
  `toByteArray()` mit falschem Zeichensatz. Von der Antwort bis zum Textfeld durchgehend UTF-8.
- Kommt trotzdem Ersatzschreibung an, greift die Wörterbuch-Korrektur aus M.4.

### M.3 KI-erzeugter Text
- **Jeder Systemprompt** an ein Sprachmodell enthält den Satz:
  > „Antworte auf Deutsch mit echten Umlauten (ä ö ü Ä Ö Ü ß). Verwende niemals die Ersatzschreibung
  > ae, oe, ue oder ss."
- Die Antwort wird vor der Anzeige durch dieselbe Wörterbuch-Korrektur geschickt.
- Auch der Text, der zum **Vorlesen** geht (Baustein D), läuft vorher durch — sonst spricht die Stimme
  „Bueromoebel" statt „Büromöbel".

### M.4 Wie korrigiert wird (wichtig: kein blindes Suchen und Ersetzen)

**Verboten:** eine pauschale Ersetzung `ae → ä`, `oe → ö`, `ue → ü`, `ss → ß`. Das zerstört richtige
Wörter — aus „Michael" würde „Michäl", aus „Aerodynamik" „Ärodynamik", aus „Poesie" „Pösie", aus
„Duell" „Düll", aus „Messer" „Meßer".

**Richtig:** eine gepflegte **Wortliste** bekannter Ersatzschreibungen, die als **ganze Wörter**
(Wortgrenzen, Groß-/Kleinschreibung egal) ersetzt werden — plus deutsche Zusammensetzungen davon:

```
ueber → über · fuer → für · koennen → können · muessen → müssen · moechte → möchte
waehlen → wählen · aendern → ändern · loeschen → löschen · schliessen → schließen
groesse → Größe · gruen → grün · zurueck → zurück · naechste → nächste · hoeren → hören
oeffnen → öffnen · erklaeren → erklären · verfuegbar → verfügbar · gueltig → gültig
strasse → Straße · gruss → Gruß · massnahme → Maßnahme · dass ≠ daß (bleibt „dass")
```

- Die Liste liegt an **einer** Stelle im Projekt (`de.<paket>.text.UmlautKorrektur`) und wird von
  Transkription, KI-Antwort und Vorlese-Aufbereitung gemeinsam benutzt.
- **Unsicher heißt: unverändert lassen.** Steht ein Wort nicht in der Liste, wird es nicht angefasst.
- Jede vorgenommene Ersetzung wird protokolliert (Wort vorher/nachher), damit die Liste wachsen kann.
- **Ausgenommen von jeder Umlaut-Regel:** Paketnamen, Klassennamen, Variablennamen, Dateinamen, Pfade,
  URLs, JSON-Schlüssel, API-Parameter und Schlüssel-Zeichenketten — die bleiben ASCII.

---

## 14. Baustein N — Fünf-Sterne-Optik: modern, mit vielen optischen Effekten ⭐ PFLICHT

**Was:** Die App soll aussehen wie eine der besten Apps im Play Store — modern, aufwendig, mit vielen
optischen Effekten. Nichts darf nach Standard-Baukasten aussehen.

### N.1 Grundhaltung

- **Kein Bildschirm ohne Gestaltung.** Jede Fläche bekommt Tiefe, Bewegung und Charakter — auch
  Einstellungen, Leerzustände und Fehlermeldungen.
- **Es bewegt sich immer etwas**, wenn ich etwas tue: nichts erscheint hart, nichts verschwindet
  schlagartig, nichts springt.
- Die **Gold-Palette aus Baustein A ist die Bühne** für alle Effekte. Verläufe, Glanz und Schein werden
  aus Gold gebaut, nicht aus Fremdfarben.
- Grundlage: **Material 3 Expressive** (`MaterialTheme` mit `MotionScheme.expressive()`), nicht das
  nüchterne Standard-Material.

### N.2 Pflicht-Effekt-Katalog

Jede App bekommt **mindestens** das Folgende. Mehr ist ausdrücklich erwünscht.

**Tiefe und Material**
- **Farbverläufe** statt Einfarbflächen: `Brush.linearGradient` auf Karten und Kopfbereichen,
  `Brush.radialGradient` als weicher Schein hinter wichtigen Elementen, `Brush.sweepGradient` für
  Ringe und Fortschritt.
- **Goldener Schein (Glow)** an aktiven Elementen: `Modifier.shadow(elevation, shape,
  ambientColor = Gold, spotColor = Gold)`.
- **Milchglas (Glassmorphismus)** hinter Kopf- und Fußleisten sowie Dialogen: `Modifier.blur()` bzw.
  `RenderEffect.createBlurEffect` auf der darunterliegenden Ebene, dazu halbtransparente Fläche und
  1-dp-Lichtkante oben.
- **Lichtkante:** feiner heller Rahmen oben, dunklerer unten — gibt Karten Körperlichkeit.
- **Gestaffelte Höhenwirkung:** Hintergrund → Karte → erhöhte Karte → Dialog, jede Stufe mit eigener
  Fläche *und* eigenem Schatten.
- **Eigene Formen** statt nur Rechtecke: `androidx.graphics.shapes` (`RoundedPolygon`) für Abzeichen,
  Symbolhintergründe und Fortschrittsanzeigen.

**Bewegung**
- **Federnde Bewegung (Spring)** als Standard, nicht lineares `tween` — alles schwingt kurz aus.
- **Formwandel (Shape-Morphing)** beim Umschalten von Zuständen (`Morph` aus
  `androidx.graphics.shapes`), z. B. Kreis → Rundquadrat beim Auswählen.
- **Pulsieren und Atmen** an laufenden Vorgängen: Aufnahme-Knopf pulsiert, Vorlese-Knopf atmet im
  Sprechrhythmus, Ladeanzeige dreht mit Verlauf.
- **Schimmer (Shimmer)** über den Platzhalter-Gerüsten beim Laden (Baustein L).
- **Gestaffeltes Einblenden** von Listen: jedes Element 40–60 ms nach dem vorigen, mit leichtem
  Hochgleiten und Aufblenden.
- **Parallaxe** beim Scrollen: Kopfbild bewegt sich langsamer als der Inhalt, Titel schrumpft weich
  in die Leiste (`nestedScroll` mit `TopAppBarScrollBehavior`).

**Übergänge**
- **Geteilte Elemente (Shared Element Transition)** zwischen Übersicht und Detail — das angetippte
  Element wandert sichtbar an seinen neuen Platz (`SharedTransitionLayout`).
- **`AnimatedContent`** für jeden Inhaltswechsel, mit Richtung passend zur Navigation.
- **Vorausschauendes Zurück (Predictive Back)** unterstützen, damit die Zurück-Geste den Bildschirm
  schon beim Wischen mitzieht.
- **Listenumsortierung** animiert (`Modifier.animateItem()`), niemals hartes Neuzeichnen.

**Mikro-Interaktionen**
- Jeder Knopf reagiert beim Drücken: kurz einsinken (Skalierung ~0,96) und wieder ausfedern.
- **Wellen-Effekt (Ripple)** in Gold eingefärbt, nicht im Grauton der Vorgabe.
- Umschalter, Haken und Auswahlkreise werden **gezeichnet, nicht getauscht** — der Haken malt sich.
- Erfolgsmomente bekommen eine kleine Feier: aufblitzender Ring, kurzes Aufleuchten, Zähler zählt hoch.

**Text und Farbe**
- Wichtige Überschriften mit **Verlaufsschrift** (`Brush` als `TextStyle.brush`).
- **Zahlen zählen animiert hoch** statt zu springen (`animateIntAsState`).
- Eine gut gewählte **Schriftfamilie über Google Fonts** (`compose.google.fonts`), nicht die
  Systemschrift — mit klarer Größen- und Gewichtungsstaffel.

### N.3 Bewegungs-Standards (einheitlich in der ganzen App)

| Vorgang | Dauer | Kurve |
|---|---|---|
| Mikro-Rückmeldung (Druck, Haken, Umschalter) | 100–150 ms | Spring, straff |
| Zustandswechsel innerhalb eines Bildschirms | 250–300 ms | Spring, mittel |
| Bildschirmwechsel, geteilte Elemente | 350–450 ms | Emphasized / Spring weich |
| Gestaffeltes Listen-Einblenden | 40–60 ms Versatz je Element | Aufblenden + 12 dp Hochgleiten |

Diese Werte stehen **einmal zentral** im Projekt (`ui/theme/Motion.kt`) und werden überall von dort
geholt — keine handgetippten Dauern verstreut im Code.

### N.4 Leitplanken (die Effekte dürfen nichts kaputt machen)

- **Lesbarkeit schlägt Effekt.** Verlauf oder Milchglas hinter Text nur, wenn der Kontrast danach
  immer noch mindestens 4,5:1 beträgt (Baustein A). Im Zweifel eine deckende Schicht unterlegen.
- **Bildrate:** Das Fold 8 läuft mit 120 Hz, ein Bild hat also rund **8 ms**. Blur und Shader nur auf
  kleinen Flächen und niemals in einem scrollenden Listenelement. Bei Rucklern: Effekt vereinfachen,
  nicht die Liste kürzen.
- **Rückfall für ältere Geräte:** `Modifier.blur` wirkt erst ab Android 12, AGSL-Shader erst ab
  Android 13. Bei `minSdk 26` heißt das: **jeder Effekt braucht eine ordentliche einfachere Fassung**
  (statt Milchglas eine halbtransparente Fläche mit Verlauf) — nie ein leeres oder kaputtes Bild.
- **Bewegungsreduzierung achten:** Ist im System „Animationen reduzieren" gesetzt
  (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`), werden Dauern auf nahe null gesetzt und
  Dauerbewegungen (Pulsieren, Schimmer) abgeschaltet. Die App bleibt voll bedienbar.
- **Kein Effekt kostet Verständlichkeit:** Ein Knopf muss als Knopf erkennbar bleiben, ein Feld als
  Feld. Verzierung ersetzt nie eine Beschriftung.

### N.5 Was verboten ist

- Standard-Material-Optik ohne eigene Handschrift („sieht aus wie das Compose-Beispielprojekt").
- Effekte, die nur auf einem Bildschirm auftauchen — dann wirkt die App zusammengestückelt.
- Dauerbewegung im Sichtfeld, während ich lese (blinkende Ränder, endlose Wellen hinter Text).
- Verläufe quer durch fremde Farbwelten — Gold bleibt die Leitfarbe.
- Effekte, die den ersten Bildaufbau verzögern: Der Bildschirm ist zuerst da, die Verzierung kommt
  im selben Atemzug hinterher.

### N.6 Abnahme

Vor „fertig" wird jeder Bildschirm einmal durchgegangen:
Hat er Tiefe? Bewegt sich beim Betreten etwas? Reagiert jeder Knopf sichtbar? Ist der Übergang zum
nächsten Bildschirm animiert? Sieht der Leerzustand gestaltet aus? — Ein „nein" ist eine offene
Aufgabe, keine Geschmacksfrage.

---

## 15. Baustein O — KI-Anbindung ⭐ PFLICHT (sobald die App überhaupt eine KI benutzt)

### O.1 Zwei Wege zur KI — Abo oder Schlüssel

In den Einstellungen wähle ich, **wie** die App an die KI kommt. Beide Wege werden eingebaut:

**Weg 1 — Anmeldung über mein ChatGPT-Abo (Voreinstellung).**
Geräteanmeldung mit Benutzercode: Die App zeigt einen Code, ich bestätige ihn im Browser, danach hat
die App Zugang — **ohne** dass pro Anfrage abgerechnet wird. Das Verfahren ist in
`PerfectMoment/app/src/main/java/de/frank/perfectmoment/auth/CodexAuthManager.kt` fertig ausgearbeitet
und wird von dort übernommen, nicht neu erfunden.

Was dabei zwingend mitkommt:
- **Zugangs- und Auffrischungs-Token in `EncryptedSharedPreferences`**, nie im Klartext.
- **Auffrischung mit Vorlauf** (rund 2 Minuten vor Ablauf) und **unter einem Mutex** — sonst frischen
  mehrere gleichzeitige Anfragen dasselbe Token mehrfach auf und der Anbieter sperrt es
  (`refresh_token_reused`).
- **Abgelaufene Anmeldung ist kein Absturz:** bei 401/403 oder `invalid_grant` wird abgemeldet und im
  Klartext gemeldet: „Die Anmeldung ist abgelaufen. Bitte neu anmelden." mit Knopf dorthin.
- Der Gerätecode hat eine **begrenzte Gültigkeit** (rund 15 Minuten) — die App zeigt die verbleibende
  Zeit und bietet einen neuen Code an, statt stumm zu warten.
- Beim Anmelden auf Netz warten statt sofort zu scheitern (kurzes Nachfassen mit steigendem Abstand).

**Weg 2 — Eigener API-Schlüssel.**
Feld in den Einstellungen (Gemini bzw. der Anbieter der App), gespeichert nach den Regeln aus
Baustein G, mit Testknopf.

**Umschalter** zwischen beiden Wegen, dazu die Anzeige, welcher gerade aktiv ist und ob er
funktioniert. Ist keiner eingerichtet, sagt die App **wofür** sie den Zugang braucht und führt direkt
zur Einrichtung — sie versteckt die Funktion nicht einfach.

### O.2 Antworten strömend anzeigen

- Die Antwort erscheint **Wort für Wort**, während sie entsteht — nicht erst am Stück nach langem
  Warten. Vorher ein Zustand „denkt nach" (Baustein L).
- **Das Vorlesen (Baustein D) hängt sich ein:** Sobald der erste vollständige Absatz durchgelaufen ist,
  beginnt die Sprachausgabe, während der Rest noch einläuft.
- **Abbrechen ist jederzeit möglich** und beendet die Anfrage wirklich (Abbruch der laufenden
  Verbindung), nicht nur die Anzeige.
- Bricht die Verbindung mitten in der Antwort ab, bleibt das **bereits Empfangene erhalten** und wird
  als unvollständig gekennzeichnet — nie kommentarlos verwerfen.

### O.3 KI-Textverbesserung nach dem Diktat

- Neben dem Mikrofon (Baustein F) sitzt ein Knopf **„Text glätten"**: Füllwörter raus, Satzzeichen und
  Absätze rein, Versprecher bereinigt — **ohne den Inhalt zu verändern**.
- **Das Original bleibt erhalten** und ist mit einem Tipp wiederherstellbar („Original anzeigen"). Der
  geglättete Text überschreibt nie unwiderruflich, was ich gesagt habe.
- Der Prompt enthält die Umlaut-Vorgabe aus **Baustein M.3** und die Anweisung, nichts hinzuzuerfinden.
- Nur ein Knopfdruck, nie automatisch — sonst weiß ich nicht mehr, was von mir ist.

### O.4 Allgemein

- Der Modellname steht **an einer Stelle** im Projekt und ist in den Einstellungen sichtbar.
- **Keine Schlüssel und keine Token ins Log** (Baustein P), auch nicht gekürzt.
- Jede KI-Antwort läuft vor der Anzeige durch die Umlaut-Korrektur aus Baustein M.

---

## 16. Baustein P — Absturz-Fänger und Diagnose-Bildschirm ⭐ PFLICHT

**Was:** Wenn etwas schiefgeht, muss ich es nachlesen können, ohne das Gerät an den Rechner zu hängen.

### P.1 Absturz-Fänger
- In der `Application`-Klasse ein `Thread.setDefaultUncaughtExceptionHandler`, der **vor** dem Absturz
  schreibt: Zeitpunkt, App-Version und Zeitstempel (Baustein H), Gerät und Android-Fassung, voller
  Aufrufpfad, letzte Aktion.
- Danach wird der ursprüngliche Handler aufgerufen — der Absturz wird **nicht verschluckt**.
- Beim nächsten Start zeigt die App einen ruhigen Hinweis: „Die App ist beim letzten Mal abgestürzt.
  [Bericht ansehen] [Verwerfen]".
- Vorlagen: `CortexAndroid/.../observability/CortexCrashHandler.kt`,
  `ClaudeKompass/.../observability/KompassCrashHandler.kt`

### P.2 Diagnose-Bildschirm in den Einstellungen
- Eigener Punkt **„Diagnose"** mit: laufendem Protokoll (neueste zuerst, filterbar nach Stufe),
  Absturzberichten, Speicherort der Log-Datei und deren Größe.
- **Knopf „Protokoll teilen"** — schickt die Datei über das Android-Teilen-Menü (per `FileProvider`),
  damit ich sie direkt weiterreichen kann.
- Knopf **„Protokoll leeren"**.
- Vorlage: `EntropieReductor/.../presentation/settings/diagnostics/DiagnosticLogScreen.kt`

### P.3 Was im Protokoll steht — und was nicht
- Struktur wie in Kapitel 17: `ts`, `level`, `module`, `fn`, `msg`, `ctx`; Rotation ab etwa 1 MB.
- **Niemals** hinein: API-Schlüssel, Token, Passwörter, vollständige Diktate oder Notizinhalte. Statt
  des Inhalts die Länge protokollieren (`{"chars": 412}`).
- Der Log-Pfad steht in `.gitignore`.

---

## 17. Technische Grundausstattung

**Stack (Standard, ohne Rückfrage):**

- Kotlin, **Jetpack Compose**, Material 3, `compileSdk`/`targetSdk` **36**, `minSdk` **26**, JVM-Ziel 17
- Version-Katalog `gradle/libs.versions.toml` — **keine** hartcodierten Abhängigkeits-Versionen
- `androidx.navigation:navigation-compose`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`
- `material3-window-size-class` (für Baustein B)
- `androidx.compose.animation:animation`, `androidx.graphics:graphics-shapes` und
  `androidx.compose.ui:ui-text-google-fonts` (für Baustein N)
- `androidx.security:security-crypto` (für Baustein G)
- **Room**, sobald mehr als eine Handvoll Datensätze dauerhaft gespeichert wird; sonst DataStore
- **OkHttp** (dazu Retrofit + Moshi ab mehr als zwei Endpunkten)
- MVVM: `ViewModel` + `StateFlow`, Composables ohne eigene Netzwerk- oder Datenbank-Aufrufe

**Beobachtbarkeit (Pflicht ab rund 150 Zeilen Logik, siehe `observability-first.md`):**
strukturiertes Log (JSON-Zeilen mit `ts`, `level`, `module`, `fn`, `msg`, `ctx`), globaler
Ausnahme-Fänger, Logik-Sonden an Vor- und Nachbedingungen. Vorlage:
`CortexAndroid/app/src/main/java/de/frank/cortex/observability/CortexLog.kt`.
**Keine Schlüssel und keine persönlichen Daten ins Log.**

**Sprache:** Deutsch mit **echten Umlauten** überall — die vollständigen Regeln dazu stehen in
**Baustein M** (Kapitel 13) und gelten für angezeigten, transkribierten und KI-erzeugten Text.

---

## 18. Checkliste vor „fertig"

- [ ] **A** Hell- und Dunkelmodus in Gold, beide vollständig, Wahl gespeichert, Dynamic Color aus
- [ ] **B** Cover-Display des Fold 8 als Basis-Layout, aufgeklappt sauber, Faltung ohne Neustart
- [ ] **C** Theme-Knopf und Zahnrad oben rechts auf dem Hauptbildschirm, in dieser Reihenfolge
- [ ] **D** Lautsprecher-Knopf am Text; Absatz-Pipeline mit Vorausschau 2 und ~1 s Pause; drei Engines
- [ ] **E** Eigene Stimme aufnehmbar, benennbar, auswählbar
- [ ] **F** Mikrofon-Knopf; `whisper-large-v3-turbo`; alle **vier** Halluzinations-Fixes; 413-Schnitt
- [ ] **G** Einstellungen mit den drei Blöcken; Schlüssel verschlüsselt; Testknöpfe
- [ ] **H** Version und Zeitstempel gebumpt und in der App sichtbar
- [ ] **I** Biometrische App-Sperre mit PIN-Rückfall und Hintergrund-Zeitsperre
- [ ] **J** Export/Import als Datei; Drive-Sicherung im `appDataFolder`; `backup_rules.xml` gepflegt
- [ ] **K** Volltextsuche (Room FTS4) über alle Inhalte, mit Hervorhebung und Leerzustand
- [ ] **L** Kein stiller Fehlschlag: Klartext-Meldung + Wiederholen, Lade- und Leerzustände überall
- [ ] **M** Nur echte Umlaute in Oberfläche, Transkript und KI-Text; `strings.xml`-Test läuft; keine blinde Ersetzung
- [ ] **N** Fünf-Sterne-Optik: Effekt-Katalog umgesetzt, `Motion.kt` zentral, jeder Bildschirm durch die Abnahme N.6
- [ ] **O** KI über Abo **und** Schlüssel; Antwort strömt; „Text glätten" mit erhaltenem Original
- [ ] **P** Absturz-Fänger schreibt vor dem Absturz; Diagnose-Bildschirm mit Teilen-Knopf; keine Geheimnisse im Log
- [ ] **D (4.3)** Vorlesen läuft bei ausgeschaltetem Bildschirm weiter, mit Pause/Weiter/Stopp in der Benachrichtigung
- [ ] Bauen und Tests grün → committen → pushen → auf dem Fold 8 installiert
- [ ] Jeder weggelassene Baustein wurde mit einem Satz begründet gemeldet

---

## 19. Fundstellen im Repo (zum Abschauen statt neu erfinden)

| Thema | Datei |
|---|---|
| Gold-Palette | `BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/theme/Color.kt` |
| Zwei-Paletten-Theme | `CortexAndroid/app/src/main/java/de/frank/cortex/ui/theme/` |
| Kopfleiste mit Theme-Knopf | `CortexAndroid/.../ui/common/CortexTopBar.kt` |
| Absatz-Pipeline mit Vorausschau | `CortexAndroid/.../ui/chat/ChatViewModel.kt` (`chunkText`, `TTS_PREFETCH_AHEAD`) |
| Text für die Sprachausgabe säubern | `CortexAndroid/.../ui/chat/ChatSpeechSanitizer.kt` |
| Google Chirp 3 HD | `PerfectMoment/.../tts/GoogleCloudTtsPlayer.kt` |
| Stimmen-Katalog | `PerfectMoment/.../tts/TtsCatalog.kt` |
| Eigene Stimme (Klonen und Sprechen) | `PerfectMoment/.../tts/QwenTtsPlayer.kt`, `QwenVoiceEnrollment.kt` |
| Aufnahme 16 kHz mono | `PerfectMoment/.../audio/MicRecorder.kt` |
| Groq-Anfrage | `PerfectMoment/.../audio/GroqTranscriber.kt` |
| Halluzinations-Filter (Schichten 2–4) | `PerfectMoment/.../audio/WhisperHallucinationFilter.kt` |
| Stille-Erkennung (Schicht 1) | `PerfectMoment/.../audio/SpeechAnalyzer.kt` |
| 413-Schnitt bei langen Diktaten | `TerminalVoiceOverlay-Windows/Services/GroqWhisperClient.cs` |
| KI-Zugang über das ChatGPT-Abo (Geräteanmeldung) | `PerfectMoment/.../auth/CodexAuthManager.kt` |
| Umschalter Abo/Schlüssel | `BestJournalFrank/.../data/remote/ai/AiGateway.kt` |
| KI-Textverbesserung | `BestJournalAndroid/.../domain/usecase/ImproveTextUseCase.kt` |
| Absturz-Fänger | `CortexAndroid/.../observability/CortexCrashHandler.kt` |
| Diagnose-Bildschirm mit Teilen-Knopf | `EntropieReductor/.../presentation/settings/diagnostics/DiagnosticLogScreen.kt` |
| Schlüssel verschlüsselt ablegen | `PerfectMoment/.../data/settings/SecureSettings.kt` |
| Strukturiertes Log | `CortexAndroid/.../observability/CortexLog.kt` |
| Biometrische App-Sperre | `PerfectMoment/.../security/AppLockManager.kt` |
| Drive-Sicherung im `appDataFolder` | `BestJournalAndroid/.../data/remote/googledrive/DriveBackupManager.kt`, `DriveRestoreManager.kt` |
| Systemsicherungs-Regeln | `BestJournalAndroid/app/src/main/res/xml/backup_rules.xml` |

---

## 20. Änderungsprotokoll

| Datum | Änderung |
|---|---|
| 29.08.2026, 11:19 Uhr | Erstfassung: Bausteine A–H aus PerfectMoment, CortexAndroid, BestJournalAndroid und TerminalVoiceOverlay zusammengetragen |
| 29.08.2026, 11:19 Uhr | Bausteine I (App-Sperre), J (Sicherung), K (Volltextsuche) und L (Fehler-, Lade- und Leerzustände) ergänzt — nach Durchsicht aller 14 Android-Apps im Repo |
| 29.08.2026, 13:28 Uhr | Baustein M ergänzt: nur echte deutsche Umlaute in Oberfläche, Transkript und KI-Text — mit Wörterbuch-Korrektur statt blinder Ersetzung |
| 29.08.2026, 13:48 Uhr | Baustein N ergänzt: Fünf-Sterne-Optik mit Pflicht-Effekt-Katalog, Bewegungs-Standards, Leitplanken und Abnahme |
| 29.08.2026, 13:59 Uhr | Zweite Durchsicht aller 14 Apps (Klassennamen, Manifeste, Berechtigungen): Baustein O (KI über Abo oder Schlüssel, strömende Antworten, Text glätten), Baustein P (Absturz-Fänger und Diagnose-Bildschirm) und Kapitel 4.3 (Vorlesen im Vordergrunddienst) ergänzt |
