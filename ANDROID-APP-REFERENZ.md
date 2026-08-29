# Android-App-Referenz — verbindliche Standard-Bausteine für alle meine Apps

> **Stand:** 29.08.2026, 16:31 Uhr · **Gilt für:** jede neue oder erweiterte Android-App von Frank
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

- **Genau zwei Modi: `hell` und `dunkel`. Kein Automatik-/System-Modus.** Die App folgt der
  Systemvorgabe **nicht** — weder als dritte Auswahl noch als Voreinstellung. `isSystemInDarkTheme()`
  wird nirgends als Quelle für den Modus benutzt, `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` ist
  verboten.
- **Voreinstellung beim allerersten Start: hell.**
- **Die App merkt sich den zuletzt gewählten Modus** und startet immer damit — persistent gespeichert
  (`EncryptedSharedPreferences` bzw. DataStore) und beim Start sofort angewandt, kein Aufblitzen des
  falschen Modus.
- Die Speicherung erfolgt **beim Umschalten**, nicht erst beim Verlassen des Bildschirms — ein
  Absturz oder ein Wegwischen der App darf die Wahl nie verlieren.
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
  `Icons.Default.LightMode` (hell) / `Icons.Default.DarkMode` (dunkel). Ein Tipp = **Umschalter
  zwischen genau diesen beiden** (hell ⇄ dunkel). **Kein dritter Zustand**, kein
  `Icons.Default.BrightnessAuto`, kein Durchlauf über einen Automatik-Modus (Baustein A).
  Der neue Modus wird sofort gespeichert.
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
- **Stimmenauswahl:** ein einziges gemeinsames Dropdown über *alle* Engines — siehe Kapitel 4.6.
  Sprechtempo als Regler (0,5–2,0).
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
  nicht „Sternchen Sternchen" vorgelesen wird — die vollständigen Regeln stehen in **Kapitel 4.5**
  (Vorlage: `CortexAndroid/.../ui/chat/ChatSpeechSanitizer.kt`).
- **Sichtbarer Zustand:** Der Lautsprecher-Knopf zeigt „lädt" / „spricht" / „aus" und stoppt bei
  erneutem Tipp sofort.

### 4.5 Der vorgelesene Text muss TTS-freundlich sein ⭐ PFLICHT

**Grundregel:** Jeder Text, der vorgelesen wird, ist so geschrieben, dass eine Sprachausgabe ihn
sauber sprechen kann — **so gut wie keine Sonderzeichen**. Das gilt **ausschließlich für Text, der
tatsächlich vertont wird** (Vorlese-Bereiche, KI-Antworten, die vorgelesen werden, Ansagen des
Vordergrunddienstes). Text, der nur angezeigt und nie gesprochen wird — Code, Logs, Tabellen, das
Diagnose-Protokoll, technische Bezeichner —, bleibt unangetastet.

**Zwei Stellen, an denen das durchgesetzt wird:**

1. **Schon bei der Texterzeugung** (KI-Prompt, Baustein O): Wird die Antwort anschließend vorgelesen,
   enthält der Systemprompt die Vorgabe:
   > „Dieser Text wird vorgelesen. Schreib ihn in ganzen, gesprochenen Sätzen. Verzichte auf
   > Markdown, Aufzählungszeichen, Sternchen, Rauten, Klammern, Tabellen, Emoji und Abkürzungen.
   > Schreib Zahlen, Einheiten und Abkürzungen aus. Antworte auf Deutsch mit echten Umlauten
   > (ä ö ü Ä Ö Ü ß)."
   Das ist der bessere Weg: sauber erzeugter Text muss hinterher nicht repariert werden.
2. **Als Netz davor** (Aufbereitung, `ChatSpeechSanitizer`): Was trotzdem an Sonderzeichen
   durchkommt, wird unmittelbar vor der Synthese entfernt oder ersetzt. Der Filter läuft **immer**,
   auch bei selbst getipptem oder importiertem Text.

**Was die Aufbereitung macht**

| Im Text | Was passiert |
|---|---|
| `**fett**`, `*kursiv*`, `` `code` ``, `# Überschrift`, `> Zitat` | Auszeichnung raus, Inhalt bleibt |
| Aufzählungszeichen `-`, `*`, `•`, `1.` am Zeilenanfang | entfernt, Zeile wird ein eigener Satz |
| Code-Blöcke (```) | ganz raus, ersetzt durch „Codebeispiel" |
| URLs, E-Mail-Adressen, Dateipfade | ersetzt durch „Link" bzw. „Dateipfad" — nie Zeichen für Zeichen buchstabieren |
| Emoji und Symbolzeichen (☑ → ✓ ★ …) | entfernt |
| Klammern `( )`, `[ ]`, `{ }` | Klammerzeichen weg, Inhalt bleibt als Einschub |
| `&`, `%`, `+`, `=`, `/`, `~`, `_`, `\|`, `#`, `@` | ausgesprochen (`und`, `Prozent`, `plus`, `gleich`, `pro`, …) oder gestrichen |
| Tabellen | zeilenweise als Sätze, Trennstriche `\|` weg |
| Abkürzungen `z. B.`, `u. a.`, `bzw.`, `ca.`, `Nr.`, `ggf.` | ausgeschrieben |
| Zahlen, Einheiten, Datum, Uhrzeit (`12.5.`, `3,5 kg`, `14:30`) | in gesprochene Form (`zwölfter Mai`, `drei Komma fünf Kilogramm`, `vierzehn Uhr dreißig`) |
| Mehrfache Satzzeichen `!!!`, `???`, `…` | auf eines reduziert |
| Ersatzschreibung `ae/oe/ue/ss` | über die Wörterbuch-Korrektur aus **Baustein M.4** |

**Was erhalten bleibt** (das sind die *guten* Sonderzeichen): Punkt, Komma, Fragezeichen,
Ausrufezeichen, Doppelpunkt, Semikolon und Bindestrich in Wörtern — sie steuern Betonung und Pausen.
Absatz-Leerzeilen bleiben ebenfalls, denn sie sind die Schnittkante der Pipeline aus 4.2.

**Leitplanken**

- Die Aufbereitung sitzt an **einer** Stelle (`de.<paket>.tts.SpeechText`) und wird von allen drei
  Engines gemeinsam benutzt — nicht je Engine neu gebaut.
- **Der angezeigte Text ändert sich nicht.** Die Aufbereitung passiert nur auf dem Weg zur Synthese;
  auf dem Bildschirm bleibt das Markdown mit allen Zeichen stehen.
- **Nichts sinnentstellend kürzen.** Im Zweifel wird ein Zeichen gestrichen, nie ein Wort.
- Bleibt nach der Aufbereitung ein **leerer Absatz** übrig (z. B. reiner Code-Block), wird er
  übersprungen statt als Stille abgespielt.
- Vorlage: `CortexAndroid/.../ui/chat/ChatSpeechSanitizer.kt`.

---

### 4.6 Alle Stimmen in **einem** Dropdown-Menü ⭐ PFLICHT

**Was:** Es gibt **genau eine** Stimmenauswahl in der App — ein Aufklapp-Menü (Dropdown), in dem
**sämtliche verfügbaren Stimmen aller Engines** untereinander stehen. Ich wähle die Stimme, nicht die
Engine: Mit der Stimme wird die zugehörige Engine automatisch mitgeschaltet.

**Reihenfolge der Gruppen im Dropdown — fest, immer diese:**

| # | Gruppe | Quelle | Kennung |
|---|---|---|---|
| 1 | **Meine Stimmen** | eigene geklonte Stimmen (Baustein E), aus dem Alibaba-Konto geladen | `qwen_clone` |
| 2 | **Alibaba-Stimmen** | die fertigen Qwen-/DashScope-Standardstimmen | `qwen` |
| 3 | **Google Chirp 3 HD** | `de-DE-Chirp3-HD-<Name>` (Kore, Zephyr, Leda, Puck, Charon, Orus …) | `google_cloud` |
| 4 | **Edge-Stimmen** | Microsoft Edge TTS (kostenlos, kein Schlüssel) | `edge_tts` |

**Regeln**

- **Ein Dropdown, keine zwei Schritte.** Es gibt *keinen* vorgelagerten Engine-Umschalter, aus dem
  danach erst eine zweite Liste entsteht. Engine-Wahl und Stimmenwahl sind derselbe Handgriff.
- **Gruppenüberschriften** trennen die vier Blöcke sichtbar (nicht anklickbar, gedämpfte Schrift,
  darüber ein feiner Trenner). Innerhalb einer Gruppe: nach Geschlecht sortiert bzw. gruppiert.
- **Meine Stimmen stehen immer ganz oben**, auch wenn es nur eine ist. Gibt es noch keine, steht dort
  statt der Liste ein Eintrag **„Eigene Stimme aufnehmen …"**, der direkt zur Aufnahme aus Baustein E
  führt — die Gruppe verschwindet nie ganz.
- **Jeder Eintrag zeigt:** Name der Stimme · ein kleines Kennzeichen der Herkunft (Chip/Icon, z. B.
  „Meine" · „Alibaba" · „Google" · „Edge") · Geschlecht, wo bekannt.
- **Probe-abspielen-Knopf je Eintrag** (kleines Lautsprecher-Symbol rechts): spielt einen kurzen
  deutschen Beispielsatz mit *genau dieser* Stimme, ohne das Menü zu schließen. Bei laufender Probe
  wird das Symbol zum Stopp-Symbol.
- **Favoriten:** Stern je Eintrag; markierte Stimmen erscheinen zusätzlich als Gruppe **„Favoriten"**
  ganz oben, noch vor „Meine Stimmen".
- **Suchfeld im Kopf des Menüs**, sobald mehr als 15 Stimmen gelistet sind — filtert über alle Gruppen.
- **Nicht nutzbare Stimmen werden nicht versteckt, sondern ausgegraut**, mit Klartext-Grund darunter
  („Alibaba-Schlüssel fehlt", „Google-Schlüssel fehlt"). Ein Tipp darauf führt direkt zum passenden
  Schlüsselfeld in den Einstellungen. Kein stilles Weglassen — sonst suche ich Stimmen, die es gibt.
- **Der Fehlschlag einer Engine leert das Menü nicht.** Kann eine Stimmenliste gerade nicht geladen
  werden (kein Netz, Schlüssel abgelehnt), bleiben alle anderen Gruppen bedienbar; die betroffene
  Gruppe zeigt eine Zeile „Konnte nicht geladen werden — erneut versuchen" (Baustein L).
- **Die Auswahl wird persistent gespeichert** (Stimm-Kennung *und* Engine) und beim Start
  wiederhergestellt. Ist die gemerkte Stimme nicht mehr verfügbar (Stimme gelöscht, Schlüssel weg),
  fällt die App auf **Edge TTS** zurück und **sagt es einmal im Klartext**, statt stumm zu schweigen.
- **Edge ist immer wählbar**, weil es keinen Schlüssel braucht — es ist der garantierte Rückfall.
- Der Stimmen-Katalog steht an **einer** Stelle im Projekt (Vorlage:
  `PerfectMoment/app/src/main/java/de/frank/perfectmoment/tts/TtsCatalog.kt`), die eigenen Stimmen
  kommen dynamisch aus dem Verzeichnis (Vorlage: `QwenVoiceDirectory.kt`) — beide fließen in
  **dieselbe** Liste, die das Dropdown speist.

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

**⭐ Meine schon vorhandenen Alibaba-Stimmen werden abgerufen und sind sofort auswählbar**

Das ist der Kern und wurde in der Vergangenheit vergessen: Es reicht **nicht**, nur ein Feld für den
Alibaba-Schlüssel einzubauen. Die Stimmen, die in meinem Alibaba-Konto **bereits geklont sind**, müssen
in der App **erscheinen und benutzbar sein** — ich klone sie nicht in jeder App neu.

- **Sobald ein Alibaba-Schlüssel hinterlegt ist, holt die App die Liste meiner geklonten Stimmen aus
  dem Konto** (DashScope-Stimmenverzeichnis, `list`-Aufruf desselben Dienstes) — ungefragt, beim
  Speichern des Schlüssels und danach bei jedem App-Start.
- Die geladenen Stimmen landen **in der Gruppe „Meine Stimmen"** ganz oben im Dropdown aus Kapitel 4.6
  und sind ohne weiteren Schritt anwählbar.
- **Die Liste wird lokal zwischengespeichert** (Stimm-ID, Name, Anlagedatum, Modell), damit sie ohne
  Netz sofort dasteht; im Hintergrund wird sie aufgefrischt. Kein leeres Menü beim Start.
- **Ein Knopf „Stimmen neu laden"** in den Einstellungen holt die Liste jederzeit von Hand.
- **Namen bleiben erhalten:** Der von mir vergebene Name wird lokal zur Stimm-ID gemerkt. Kennt die App
  eine ID noch nicht, zeigt sie sie mit einem verständlichen Ersatznamen an (z. B. „Eigene Stimme 3 —
  angelegt am 14.06.2026") und lässt sich sofort umbenennen — **nie** als roher Kennungs-String.
- **Zum Sprechen wird dasselbe Modell benutzt, mit dem die Stimme geklont wurde.** Das Modell wird
  daher **je Stimme** mitgespeichert, nicht global angenommen — sonst wird die Stimm-ID abgelehnt.
  Eine Stimme mit unbekanntem Modell wird ausgegraut, nicht stillschweigend mit dem Standardmodell
  probiert.
- **Löschen im Konto, nicht nur lokal:** Der Löschen-Knopf entfernt die Stimme wirklich bei Alibaba,
  mit Rückfrage vorher.
- Schlägt der Abruf fehl, sagt die App **warum** im Klartext (Schlüssel ungültig, kein Netz, Kontingent)
  und bietet Wiederholen an (Baustein L) — die schon zwischengespeicherten Stimmen bleiben nutzbar.
- Vorlage für das Verzeichnis: `PerfectMoment/.../tts/QwenVoiceDirectory.kt`
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

Erreichbar über das Zahnrad aus Baustein C. Enthält **immer mindestens** diese Blöcke:

### 7.1 Vorlesen
**Ein gemeinsames Stimmen-Dropdown** über alle Engines nach Kapitel 4.6 (Meine Stimmen → Alibaba →
Google Chirp 3 HD → Edge), mit Probe-Knopf und Favoriten — **kein separater Engine-Umschalter** ·
Sprechtempo-Regler · Feld **Google-/Gemini-API-Key** · Feld **Alibaba-DashScope-API-Key** ·
Knopf **„Stimmen neu laden"** (holt meine geklonten Alibaba-Stimmen ins Konto-Verzeichnis, Baustein E) ·
Knopf „Eigene Stimme aufnehmen" mit Verwaltung der geklonten Stimmen (umbenennen, löschen).

### 7.2 Spracheingabe
Feld **Groq-API-Key** · Modellanzeige `whisper-large-v3-turbo` · Schalter für die Filter-Schichten
(Voreinstellung: alle an) · optionaler Testknopf „Aufnahme prüfen".

### 7.3 Darstellung
**Hell / Dunkel — nur diese beiden**, als Zwei-Wege-Auswahl (Segmented Button oder zwei Karten).
**Kein „System"-, „Automatik"- oder „Gerätevorgabe"-Eintrag** (Baustein A). Voreinstellung hell,
zuletzt gewählter Modus wird gemerkt · falls vorhanden: Schriftgröße.

### 7.4 KI (sobald die App eine KI benutzt — Baustein O)
Umschalter **Abo / eigener Schlüssel** mit Anmeldestatus · bei der Anmeldung der Code mit Knopf
**„Code kopieren"** (O.1) · **Modell-Dropdown** (Sol / Terra / Luna, neuere Modelle ergänzbar) ·
**Effort-Auswahl** (Niedrig / Mittel / Hoch / Sehr hoch / Maximal) — **beide zusammen, nie nur eines**
(O.2) · Feld für den eigenen API-Schlüssel mit Testknopf.

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
  „Bueromoebel" statt „Büromöbel". Danach folgt die TTS-Aufbereitung aus Kapitel 4.5.

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

**⭐ Der angezeigte Anmeldecode ist immer kopierbar — mit einem Knopf „Code kopieren".**
Das ist keine Kür, sondern Pflicht in jeder App mit Codex-/ChatGPT-Anmeldung. Ich tippe den Code
niemals ab.

- Direkt unter dem Code steht ein gut sichtbarer Knopf **„Code kopieren"** (Icon
  `Icons.Default.ContentCopy` + Beschriftung), der den Code in die Zwischenablage legt
  (`ClipboardManager`, Label „Anmeldecode").
- **Auch der Code selbst ist antippbar** und kopiert dann dasselbe — der Knopf ist der offensichtliche
  Weg, der Code der bequeme.
- Nach dem Kopieren **sichtbare Rückmeldung**: der Knopf wechselt kurz auf „Kopiert ✓" (rund 2
  Sekunden, dann zurück) und/oder ein kurzer Snackbar-Hinweis. Kein stummes Kopieren.
  Auf Android 13+ zeigt das System selbst eine Bestätigung — dann keine zweite doppelt einblenden.
- Der Code steht **groß, mit Sperrschrift und in einer Festbreitenschrift** da (Zeichen einzeln
  lesbar), damit Notfalls auch Abtippen ginge.
- **Der Code wird kopiert wie er angezeigt wird** — mit Bindestrich, wenn er mit Bindestrich dasteht.
  Formatierung an *einer* Stelle (`DeviceCodeFormat.kt`), nie zweimal unterschiedlich.
- Daneben ein Knopf **„Seite öffnen"**, der die Bestätigungs-URL im Browser aufruft, damit ich den
  kopierten Code dort nur noch einfügen muss.
- Bei neuem Code (Ablauf, „Neuen Code anfordern") gilt dasselbe wieder — der Kopier-Knopf verschwindet
  nie und bezieht sich immer auf den aktuell gültigen Code.

Was außerdem zwingend mitkommt:
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

### O.2 Modell **und** Denkaufwand (Effort) sind auswählbar ⭐ PFLICHT

**Was:** Sobald Codex/ChatGPT eingebaut ist, kann ich in den Einstellungen **beides selbst wählen**:
welches **Modell** rechnet und mit wie viel **Denkaufwand**. Beides zusammen, nie nur eines — das
Effort-Feld wurde in der Vergangenheit vergessen, das darf nicht wieder passieren.

**Modell-Auswahl**

- Ein **Dropdown** mit allen Modellen. Ausgangsliste (deutscher Anzeigename → API-Kennung):

  | Anzeige | API-Kennung | Charakter |
  |---|---|---|
  | GPT 5.6 Sol | `gpt-5.6-sol` | schnell |
  | **GPT 5.6 Terra** | `gpt-5.6-terra` | ausgewogen — **Voreinstellung** |
  | GPT 5.6 Luna | `gpt-5.6-luna` | am gründlichsten |

- **Neuere Modelle kommen dazu, ohne dass die App umgebaut wird.** Die Liste steht als *ein*
  `enum class CodexModel(label, apiId)` an *einer* Stelle im Projekt — eine neue Zeile genügt.
  Vorlage: `PerfectMoment/.../auth/CodexModels.kt`, `GenialeIdeen/.../auth/CodexModels.kt`.
- **Eine unbekannte gespeicherte Kennung stürzt nicht ab:** `fromLabel()` fällt auf die
  Voreinstellung zurück (Terra), meldet das aber einmal im Klartext statt stumm umzuschalten.
- Neben jedem Eintrag steht **ein Halbsatz, wofür das Modell gut ist** (schnell / ausgewogen /
  gründlich) — ich soll nicht raten müssen.

**Effort-Auswahl (Denkaufwand)**

- Ein **eigenes Auswahlfeld direkt unter dem Modell** — Dropdown oder Segmented Button, fünf Stufen:

  | Anzeige | API-Wert |
  |---|---|
  | Niedrig | `low` |
  | **Mittel** | `medium` — **Voreinstellung** |
  | Hoch | `high` |
  | Sehr hoch | `xhigh` |
  | Maximal | `max` |

- Der Wert geht bei **jeder** Anfrage mit, als `reasoning.effort` im Anfrage-Rumpf:
  `.put("reasoning", JSONObject().put("effort", reasoningEffort.apiValue))`.
  **Nicht nur beim Hauptaufruf** — jede Stelle, die Codex ruft (Fragen, Zusammenfassung,
  Text glätten, Sitzungs-Prompt), reicht Modell *und* Effort durch. Keine Stelle mit hartverdrahtetem
  Standardwert.
- Ein höherer Effort dauert länger — die App zeigt bei `xhigh`/`max` einen Hinweis („kann deutlich
  länger dauern") und lässt das Abbrechen aus O.3 trotzdem jederzeit zu.

**Gemeinsame Regeln**

- **Modell und Effort werden persistent gespeichert** (nach den Regeln aus Baustein G) und beim Start
  wiederhergestellt — die zuletzt gewählte Kombination gilt weiter.
- **Beides ist auch in den Einstellungen sichtbar, nicht nur änderbar** — ich sehe jederzeit, womit
  gerade gerechnet wird, ohne das Dropdown zu öffnen.
- **Beides gehört als Paar in den Datentyp der Anfrage** (`model` + `reasoningEffort` im
  Request-`data class`), damit man das eine nicht ohne das andere durchreichen kann. Vorlage:
  `CodexQuestionRequest` in `GenialeIdeen/.../auth/CodexModels.kt`.
- Läuft die App über **Weg 2 (eigener API-Schlüssel)** statt über das Abo, gilt dasselbe: Modell- und
  Effort-Auswahl bleiben stehen, nur die Liste passt sich dem Anbieter an. Kennt ein Anbieter kein
  `effort`, wird das Feld ausgegraut **mit Begründung**, nicht entfernt.
- Wird eine Kombination vom Dienst abgelehnt (Modell nicht freigeschaltet, Effort unbekannt), sagt die
  App **welche** Kombination abgelehnt wurde und bietet den Rückfall auf Terra/Mittel an — kein
  stiller Wechsel (Baustein L).

---

### O.3 Antworten strömend anzeigen

- Die Antwort erscheint **Wort für Wort**, während sie entsteht — nicht erst am Stück nach langem
  Warten. Vorher ein Zustand „denkt nach" (Baustein L).
- **Das Vorlesen (Baustein D) hängt sich ein:** Sobald der erste vollständige Absatz durchgelaufen ist,
  beginnt die Sprachausgabe, während der Rest noch einläuft — der Absatz läuft vorher durch die
  TTS-Aufbereitung aus Kapitel 4.5.
- **Abbrechen ist jederzeit möglich** und beendet die Anfrage wirklich (Abbruch der laufenden
  Verbindung), nicht nur die Anzeige.
- Bricht die Verbindung mitten in der Antwort ab, bleibt das **bereits Empfangene erhalten** und wird
  als unvollständig gekennzeichnet — nie kommentarlos verwerfen.

### O.4 KI-Textverbesserung nach dem Diktat

- Neben dem Mikrofon (Baustein F) sitzt ein Knopf **„Text glätten"**: Füllwörter raus, Satzzeichen und
  Absätze rein, Versprecher bereinigt — **ohne den Inhalt zu verändern**.
- **Das Original bleibt erhalten** und ist mit einem Tipp wiederherstellbar („Original anzeigen"). Der
  geglättete Text überschreibt nie unwiderruflich, was ich gesagt habe.
- Der Prompt enthält die Umlaut-Vorgabe aus **Baustein M.3** und die Anweisung, nichts hinzuzuerfinden.
- Nur ein Knopfdruck, nie automatisch — sonst weiß ich nicht mehr, was von mir ist.

### O.5 Allgemein

- Die Modell-Liste steht **an einer Stelle** im Projekt (ein `enum`), und Modell **wie** Effort sind in
  den Einstellungen sichtbar **und auswählbar** (O.2) — nicht bloß angezeigt, nicht hartverdrahtet.
- **Keine Schlüssel und keine Token ins Log** (Baustein P), auch nicht gekürzt.
- Jede KI-Antwort läuft vor der Anzeige durch die Umlaut-Korrektur aus Baustein M.
- **Wird die Antwort vorgelesen, enthält der Prompt zusätzlich die TTS-Vorgabe aus Kapitel 4.5**
  (gesprochene Sätze, kein Markdown, keine Sonderzeichen, Zahlen und Abkürzungen ausgeschrieben).

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

- [ ] **A** Hell- und Dunkelmodus in Gold, beide vollständig, **nur diese zwei (kein Automatik-/System-Modus)**, Start hell, zuletzt gewählter Modus gemerkt, Dynamic Color aus
- [ ] **B** Cover-Display des Fold 8 als Basis-Layout, aufgeklappt sauber, Faltung ohne Neustart
- [ ] **C** Theme-Knopf und Zahnrad oben rechts auf dem Hauptbildschirm, in dieser Reihenfolge
- [ ] **D** Lautsprecher-Knopf am Text; Absatz-Pipeline mit Vorausschau 2 und ~1 s Pause; drei Engines
- [ ] **D (4.6)** **Ein** Dropdown mit allen Stimmen: Meine → Alibaba → Google Chirp 3 HD → Edge, mit Probe-Knopf, Favoriten, ausgegrauten statt versteckten Einträgen; Auswahl gespeichert
- [ ] **E** Eigene Stimme aufnehmbar, benennbar, auswählbar — **und meine schon vorhandenen Alibaba-Stimmen werden aus dem Konto geladen, zwischengespeichert und sind sofort wählbar** (nicht nur ein Schlüsselfeld)
- [ ] **F** Mikrofon-Knopf; `whisper-large-v3-turbo`; alle **vier** Halluzinations-Fixes; 413-Schnitt
- [ ] **G** Einstellungen mit allen Blöcken (Vorlesen, Spracheingabe, Darstellung, KI); Schlüssel verschlüsselt; Testknöpfe
- [ ] **H** Version und Zeitstempel gebumpt und in der App sichtbar
- [ ] **I** Biometrische App-Sperre mit PIN-Rückfall und Hintergrund-Zeitsperre
- [ ] **J** Export/Import als Datei; Drive-Sicherung im `appDataFolder`; `backup_rules.xml` gepflegt
- [ ] **K** Volltextsuche (Room FTS4) über alle Inhalte, mit Hervorhebung und Leerzustand
- [ ] **L** Kein stiller Fehlschlag: Klartext-Meldung + Wiederholen, Lade- und Leerzustände überall
- [ ] **M** Nur echte Umlaute in Oberfläche, Transkript und KI-Text; `strings.xml`-Test läuft; keine blinde Ersetzung
- [ ] **N** Fünf-Sterne-Optik: Effekt-Katalog umgesetzt, `Motion.kt` zentral, jeder Bildschirm durch die Abnahme N.6
- [ ] **O** KI über Abo **und** Schlüssel; **Codex-Anmeldecode mit Knopf „Code kopieren" und Rückmeldung**; Antwort strömt; „Text glätten" mit erhaltenem Original
- [ ] **O (O.2)** **Modell-Dropdown (Sol / Terra / Luna, erweiterbar) UND Effort-Auswahl (Niedrig bis Maximal)** vorhanden, gespeichert und an *jeder* Codex-Anfrage mitgesendet — keine Stelle mit hartverdrahtetem Wert
- [ ] **P** Absturz-Fänger schreibt vor dem Absturz; Diagnose-Bildschirm mit Teilen-Knopf; keine Geheimnisse im Log
- [ ] **D (4.3)** Vorlesen läuft bei ausgeschaltetem Bildschirm weiter, mit Pause/Weiter/Stopp in der Benachrichtigung
- [ ] **D (4.5)** Vorgelesener Text ist TTS-freundlich: KI-Prompt mit Vorlese-Vorgabe **und** Aufbereitung an einer Stelle; keine Sonderzeichen in der Sprachausgabe; Anzeige unverändert
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
| Text für die Sprachausgabe säubern (TTS-freundlich, 4.5) | `CortexAndroid/.../ui/chat/ChatSpeechSanitizer.kt` |
| Google Chirp 3 HD | `PerfectMoment/.../tts/GoogleCloudTtsPlayer.kt` |
| Stimmen-Katalog | `PerfectMoment/.../tts/TtsCatalog.kt` |
| Verzeichnis meiner geklonten Alibaba-Stimmen | `PerfectMoment/.../tts/QwenVoiceDirectory.kt` |
| Anmeldecode formatieren (für Anzeige und Kopieren) | `PerfectMoment/.../auth/DeviceCodeFormat.kt` |
| Eigene Stimme (Klonen und Sprechen) | `PerfectMoment/.../tts/QwenTtsPlayer.kt`, `QwenVoiceEnrollment.kt` |
| Aufnahme 16 kHz mono | `PerfectMoment/.../audio/MicRecorder.kt` |
| Groq-Anfrage | `PerfectMoment/.../audio/GroqTranscriber.kt` |
| Halluzinations-Filter (Schichten 2–4) | `PerfectMoment/.../audio/WhisperHallucinationFilter.kt` |
| Stille-Erkennung (Schicht 1) | `PerfectMoment/.../audio/SpeechAnalyzer.kt` |
| 413-Schnitt bei langen Diktaten | `TerminalVoiceOverlay-Windows/Services/GroqWhisperClient.cs` |
| KI-Zugang über das ChatGPT-Abo (Geräteanmeldung) | `PerfectMoment/.../auth/CodexAuthManager.kt` |
| Modell- und Effort-Auswahl (Sol/Terra/Luna, `low`…`max`) | `GenialeIdeen/.../auth/CodexModels.kt`, `PerfectMoment/.../auth/CodexModels.kt` |
| Modell und Effort in den Einstellungen anbieten | `GenialeIdeen/.../ui/EinstellungenScreen.kt` |
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
| 29.08.2026, 16:31 Uhr | Kapitel O.2 neu: Modell-Auswahl (Sol / Terra / Luna, neuere ergänzbar) **und** Effort-Auswahl (Niedrig bis Maximal) sind Pflicht, gespeichert und an jeder Anfrage mitgesendet; alte O.2–O.4 zu O.3–O.5 verschoben; Einstellungs-Block 7.4 (KI) ergänzt |
| 29.08.2026, 16:17 Uhr | Baustein O.1: Codex-Anmeldecode ist immer kopierbar — Pflicht-Knopf „Code kopieren" mit Rückmeldung, antippbarer Code, „Seite öffnen" |
| 29.08.2026, 16:17 Uhr | Kapitel 4.6 ergänzt: **ein** gemeinsames Stimmen-Dropdown über alle Engines (Meine → Alibaba → Google Chirp 3 HD → Edge) statt getrennter Engine-Auswahl |
| 29.08.2026, 16:17 Uhr | Baustein E erweitert: schon vorhandene geklonte Alibaba-Stimmen werden aus dem Konto geladen, zwischengespeichert und sind sofort auswählbar — nicht nur ein Schlüsselfeld |
| 29.08.2026, 16:17 Uhr | Baustein A, C und 7.3: nur noch Hell- und Dunkelmodus, **kein Automatik-/System-Modus** mehr; Start hell, zuletzt gewählter Modus wird gemerkt |
| 29.08.2026, 15:43 Uhr | Kapitel 4.5 ergänzt: vorgelesener Text muss TTS-freundlich sein — Vorgabe schon im KI-Prompt plus Aufbereitung als Netz, Zeichen-Tabelle, Leitplanken; gilt nur für Text, der wirklich gesprochen wird |
