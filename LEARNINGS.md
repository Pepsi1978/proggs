# Zentrale Learnings & Best Practices

> Projektuebergreifende Wissensbasis fuer alle Apps.
> Jedes Projekt schreibt hier rein — so profitieren zukuenftige Apps von allen Erfahrungen.
> Quelle: Best Journal (ehemals Entropy Journal), weitere Projekte folgen.

## Inhaltsverzeichnis

**Audio & Sprache:**
- [sherpa-onnx (Offline Speech-to-Text)](#sherpa-onnx-offline-speech-to-text)
- [Whisper 30-Sekunden-Limit: Stille-basiertes Chunking](#whisper-30-sekunden-limit-stille-basiertes-chunking-wichtig) ⭐
- [Compose Waveform-Visualizer](#compose-waveform-visualizer-mutablestatelistof--rememberupdatedstate-wichtig) ⭐
- [Text-Verbesserung (Tampermonkey-Style)](#text-verbesserung-tampermonkey-style)
- [Chrome Live Caption (Untertitel) umgehen via Web Audio API](#chrome-live-caption-untertitel-umgehen-via-web-audio-api-neu-2026-05-29) ⭐ NEU 2026-05-29

**UI & Navigation:**
- [Compose Chart Smooth Pinch-Zoom (Google-Maps-Smoothness)](#compose-chart-smooth-pinch-zoom-google-maps-smoothness-via-graphicslayer-kritisch) ⭐⭐⭐ NEU 2026-05-17
- [HorizontalPager fuer Wisch-Navigation](#horizontalpager-fuer-wisch-navigation-zwischen-tabs-wichtig) ⭐
- [Pinch-to-Zoom + Pager: awaitEachGesture](#pinch-to-zoom--horizontalpager-awaiteachgesture-pflicht-kritisch) ⭐⭐
- [Scroll-Pattern: LazyRow](#scroll-pattern-lazyrow-statt-rowhorizontalscroll)
- [TextField in AlertDialog: heightIn begrenzen](#textfield-in-alertdialog-heightin-begrenzen-wichtig) ⭐
- [Tastatur-Steuerung: Kontextabhaengiger Auto-Focus](#tastatur-steuerung-kontextabhaengiger-auto-focus)
- [Inline-Editing (Tagebuch)](#inline-editing-tagebuch)
- [Shimmer/Ladebalken](#shimmerladebalken)
- [Kategorie-Icons](#kategorie-icons)
- [Cloud-Sync-Icon: Persistenter Status](#cloud-sync-icon-persistenter-status-statt-kurzes-aufblinken)
- [Dark Mode: Spotify-Prinzip](#dark-mode-spotify-prinzip-solide-farben-kein-glaseffekt-wichtig) ⭐

**Authentifizierung & Sicherheit:**
- [Biometric Lock: savedInstanceState bei Rotation](#biometric-lock-savedinstancestate-bei-rotation-wichtig) ⭐
- [Biometric Lock (Grundlagen)](#biometric-lock)
- [SharedPreferences Sign-Out (Race Condition)](#sharedpreferences-sign-out-race-condition-fix)

**APIs & Cloud:**
- [Google Drive Sync](#google-drive-sync)
- [Gemini API](#gemini-api)
- [Gmail API (Feedback)](#gmail-api-feedback)

**Git & DevOps:**
- [Ordner-Umbenennung: ADB/Gradle stoppen + LFS-Pfade anpassen](#ordner-umbenennung-adbgradle-stoppen--lfs-pfade-anpassen-wichtig) ⭐
- [NestedScrollConnection: LazyRow in HorizontalPager isolieren](#nestedscrollconnection-lazyrow-in-horizontalpager-isolieren)
- [Splash-Animation: Spring-Bounce vs Tween-Settle](#splash-animation-spring-bounce-vs-tween-settle)
- [Scaffold NIE um den gesamten NavHost](#scaffold-nie-um-den-gesamten-navhost-wichtig) ⭐

**Splash Screen & Animationen:**
- [Papyrus-Textur als Bild-Hintergrund](#papyrus-textur-als-bild-hintergrund-fuer-ui-elemente)
- [Handschrift-Font (Caveat) lokal einbinden](#handschrift-font-caveat-lokal-einbinden-wichtig) ⭐
- [Heartbeat-Puls-Animation mit Tap-to-Excite](#heartbeat-puls-animation-mit-tap-to-excite)
- [Fliegende Notebooks: Interaktive Canvas-Objekte](#fliegende-notebooks-interaktive-canvas-objekte)

**Sonstiges:**
- [Theme-Toggle](#theme-toggle-separates-sharedpreferences-fuer-quick-toggle)
- [Hilt/Dagger](#hiltdagger)
- [Sonnenauf-/untergang](#sonnenauf-untergang)

**CLI & Terminal:**
- [Terminal-Input leeren ohne Interrupt: Ctrl+U ist der einzige sichere Weg](#terminal-input-leeren-ohne-interrupt-ctrlu-ist-der-einzige-sichere-weg-kritisch) ⭐⭐

---

## Compose Chart Smooth Pinch-Zoom (Google-Maps-Smoothness via graphicsLayer) (KRITISCH)

> Frank-Bestätigung 2026-05-17 (Entropie Reductor v0.9.94): **"Glatte Eins. Genauso gut wie bei Google Maps funktioniert es jetzt."** Verifiziert auf Samsung Galaxy Z Fold 6 mit 3598 GPS-Punkten.

### Der Schlüssel-Trick
**graphicsLayer-GPU-Transform während aktiver Pinch-Geste.** Während Pinch wird die gesamte Chart-Box vom GPU uniform skaliert (wie Google Maps eine Bitmap-Tile). KEIN Re-Compute der Polyline, kein Y-Auto-Scale, kein Achsen-Neuzeichnen. Erst beim Loslassen wird der Pinch-Faktor mathematisch in echtes scale/offset konvertiert.

### Pattern (vollständig in `~/.claude/projects/C--Users-barwa-proggs/memory/reference_compose_chart_smooth_pinch_zoom.md`)

```kotlin
// Live-Pinch-States (nur während Geste, kein rememberSaveable)
var pinchScale by remember { mutableFloatStateOf(1f) }
var pinchPanX by remember { mutableFloatStateOf(0f) }
var pinchOriginX by remember { mutableFloatStateOf(0.5f) }
var pinchOriginY by remember { mutableFloatStateOf(0.5f) }
var isPinching by remember { mutableStateOf(false) }

// graphicsLayer-Wrapper um die Chart-Box
Box(modifier = Modifier.graphicsLayer {
    if (isPinching) {
        scaleX = pinchScale
        scaleY = pinchScale       // UNIFORM wie Google Maps
        translationX = pinchPanX
        translationY = pinchPanY
        transformOrigin = TransformOrigin(pinchOriginX, pinchOriginY)
    }
}) { ChartContent(...) }
```

### Pinch-End-Mathematik (Pivot-Erhaltung)
```kotlin
val dataPivotFrac = offset + pivotFracCanvas / scale
val newScale = (scale * pinchScale).coerceIn(1f, 200f)
val newVisible = 1f / newScale
val newOffset = (dataPivotFrac - pivotFracCanvas * newVisible).coerceIn(0f, 1f - newVisible)
```

### Was NICHT wichtig war (Anti-Learnings)
- `derivedStateOf` für visibleValues → Mikro-Optimierung ohne spürbaren Effekt
- `drawWithCache` für Polyline → bei graphicsLayer-Ansatz irrelevant
- LTTB-Decimation → hilft minimal aber nicht smoothness-relevant
- Threshold-Tuning auf 0.001f → bei graphicsLayer egal

### Wichtige Details
- `pinchScale.coerceIn(0.001f, 1000f)` WÄHREND Pinch (nicht clampen!)
- `coerceIn(1f, 200f)` erst beim Commit
- `Snapshot.withMutableSnapshot {}` für Batch-State-Updates
- Direction-Lock für Single-Finger (X=Crosshair, Y=Pan)
- 3 Schichten Defense in Depth gegen Rotation: configChanges + Screen-Level-State + rememberSaveable

### Wiederverwendbar für
ALLE zukünftigen Compose Charts mit Pinch-Zoom-Anforderung: Puls, Tempo, Höhenmeter, Cadence, Power, Kalorien, beliebige Time-Series. Template-Code in der Memory.

### Verifikations-Historie
- v0.9.93: Initial-Implementation. Frank: "etwas smoother, noch nicht Google Maps"
- v0.9.94: + Paint-Caching + weite Pinch-Range. Frank: **"glatte Eins"** ✅

---

## Scroll-Pattern: LazyRow statt Row+horizontalScroll
- `Row + Modifier.horizontalScroll()` funktioniert NICHT zuverlaessig innerhalb von `LazyColumn`
- **Loesung**: `LazyRow` mit `contentPadding` und `Arrangement.spacedBy()`
- KEINE Wrapper-Composables mit eigenen Gesten (`pointerInput`, `detectDragGestures`) um klickbare Items in scrollbaren Listen
- `Modifier.clickable` direkt auf der Card — kollidiert nicht mit LazyRow-Scroll

## Theme-Toggle: Separates SharedPreferences fuer Quick-Toggle
- `EncryptedSharedPreferences` kann nicht direkt aus Composables erstellt werden (Key-Mismatch mit Hilt-Instanz)
- **Loesung**: Ein einfaches `SharedPreferences("entropy_theme_quick")` fuer den Quick-Toggle
- `MainActivity` hoert auf BEIDE: encrypted (Einstellungen) + quick (Icon-Klicks)
- Quick-Toggle schreibt zurueck in encrypted Prefs damit Einstellungen synchron bleiben

## Google Drive Sync
- `GoogleAccountCredential.usingOAuth2()` funktioniert NICHT mit Credential Manager Sign-In
- **Loesung**: `GoogleAuthUtil.getToken()` mit `Account(email, "com.google")` holt OAuth-Token direkt
- `UserRecoverableAuthException` muss abgefangen werden — enthaelt Intent fuer Consent-Dialog
- Beim Restore: WAL + SHM Dateien loeschen VOR dem Download, sonst nutzt Room die alten Daten
- Nach Restore/Sign-Out: `Runtime.getRuntime().exit(0)` + Activity-Relaunch um Room-Cache zu leeren
- API-Keys vor Sign-Out sichern und danach wiederherstellen (gehoeren zum Geraet, nicht zum Konto)

## Hilt/Dagger
- `AppDatabase` NICHT direkt in UseCase injizieren wenn Hilt es nicht als Provider kennt → `NoSuchMethodError`
- Stattdessen `context.getDatabasePath()` verwenden
- Nach Hilt-Aenderungen: `./gradlew clean assembleDebug` um generierte Klassen neu zu erstellen

## Shimmer/Ladebalken
- Halbtransparente Highlight-Farben erzeugen sichtbare "Schnitt"-Artefakte im Gradient
- **Loesung**: `lerp(baseColor, Color.White, 0.35f)` fuer opaken Highlight ohne Transparenz-Probleme
- `colorStops` mit expliziten Positionen fuer kontrollierten Verlauf
- 35% White-Blend im Dunkelmodus, 15% Black-Blend im Hellmodus

## Kategorie-Icons
- 50+ Icon-Mappings fuer alle Lebensbereiche (Gesundheit, Psyche, Arbeit, Finanzen, Freizeit, Sucht, Spiritualitaet)
- Deutsche Aliase unterstuetzen: KI kann "schlaf", "arbeit", "finanzen" als Icon-Namen verwenden
- `else -> Icons.Rounded.Lightbulb` als Fallback fuer unbekannte Icons
- Karten: 110x100dp mit 10sp Schrift und maxLines=3 fuer lange Kategorienamen
- `fillMaxSize()` auf Column innerhalb der Card fuer korrekte Zentrierung

## Gemini API
- Modell dynamisch via `@Path("model")` in Retrofit: `@POST("models/{model}:generateContent")`
- Modellauswahl in SharedPreferences gespeichert, von ImproveTextUseCase UND AdviceRepository gelesen
- Prompt fuer Analyse: Eintraege nummerieren ("EINTRAG 1 von 5") damit KI keinen uebersieht
- Frische Analyse (manuell) OHNE vorherigen Kontext senden — sonst produziert KI identischen Text
- Textverbesserung: Tampermonkey-Style Intention-Prompt statt reiner Grammatik-Korrektur

## Sonnenauf-/untergang
- NOAA Solar Position Algorithmus — reine Mathematik, keine API noetig
- `ACCESS_COARSE_LOCATION` reicht aus (keine GPS-Genauigkeit noetig)
- Standort wird einmal geholt und in SharedPreferences gespeichert fuer Offline-Berechnung
- Drei Theme-Modi sind gegenseitig exklusiv: Manuell / System / Sonne

## sherpa-onnx (Offline Speech-to-Text)
- Library: Apache 2.0, kommerziell kostenlos nutzbar
- AAR von GitHub Releases herunterladen und als `implementation(files("libs/....aar"))` einbinden
- Konstruktor: `OfflineRecognizer(context.assets, config)` — laedt Modelle direkt aus Assets
- `noCompress += listOf("onnx", "txt")` in `androidResources` damit Assets nicht komprimiert werden
- Whisper Base int8: Encoder 28 MB + Decoder 125 MB + Tokens 800 KB = 153 MB
- 51x schneller als whisper.cpp auf Android (benchmarked)
- WAV-Samples: 16-bit PCM nach Float normalisieren mit `sample.toShort().toFloat() / 32768.0f`

## Whisper 30-Sekunden-Limit: Stille-basiertes Chunking (WICHTIG)
- Whisper-Modelle (Base, Small, Medium, Large) haben ein **festes 30-Sekunden-Kontextfenster** — architekturbedingt (1500 Mel-Spectrogram-Frames a 20ms)
- Wenn man laengeres Audio als einen Block an `OfflineRecognizer` schickt, wird alles nach 30s abgeschnitten
- **NICHT hart alle 30 Sekunden schneiden** — das zerstueckelt Woerter an den Schnittstellen
- **Loesung: Stille-basiertes Chunking** — in den letzten 5 Sekunden jedes 30s-Fensters nach der leisesten Stelle suchen
- Algorithmus: Sliding-Window RMS-Energie (300ms Fenster, 75ms Schrittweite) ueber die Samples von Sekunde 25-30
- Der Punkt mit niedrigster Energie ist fast immer eine Atempause oder Satzgrenze
- Dort schneiden → kein Wort wird zerstueckelt, kein Text geht verloren, keine Duplikate
- Performance: Stille-Erkennung braucht <1ms (reine Arithmetik auf Float-Array), voellig unsichtbar
- Jeder Chunk wird separat transkribiert, Ergebnisse mit Leerzeichen zusammengefuegt
- Funktioniert fuer beliebige Aufnahmedauer (3, 5, 10, 15+ Minuten)
- **Dieses Pattern ist wiederverwendbar** fuer jedes Projekt das Whisper/sherpa-onnx offline nutzt

## Compose Waveform-Visualizer: mutableStateListOf + rememberUpdatedState (WICHTIG)
- `LaunchedEffect(Unit)` faengt Parameter-Werte einmalig ein — spaetere Updates kommen nie an
- **Loesung**: `rememberUpdatedState(amplitude)` liefert immer den aktuellen Wert auch in laufenden Coroutinen
- `FloatArray` ist KEIN Compose-State — Aenderungen triggern KEINE Recomposition
- **Loesung**: `mutableStateListOf<Float>()` ist ein Compose Snapshot-State — `removeAt(0)` + `add()` triggert automatisch Canvas-Redraw
- Pattern fuer rollendes Waveform: `LaunchedEffect(Unit) { while(true) { delay(80); history.removeAt(0); history.add(currentAmplitude) } }`
- Amplitude-Boost: `(amp * 3f).coerceAtMost(1f)` + `sqrt()` fuer sichtbare Ausschlaege bei normaler Sprechlautstaerke
- 50 Balken mit 3dp Gap, 64dp Hoehe, Cyan-zu-Violett Gradient, abgerundete Ecken
- **Dieses Pattern ist wiederverwendbar** fuer jede Audio-Visualisierung in Compose

## Biometric Lock: savedInstanceState bei Rotation (WICHTIG)
- Android zerstoert und erstellt die Activity bei Bildschirmrotation neu
- State in `mutableStateOf` auf der Activity-Instanz geht dabei verloren
- **Loesung**: `onSaveInstanceState()` ueberschreiben und kritischen State (z.B. `isUnlocked`) im Bundle speichern
- In `onCreate()` aus `savedInstanceState` wiederherstellen wenn nicht null
- Alternative `configChanges` im Manifest ist ein Holzhammer — Google raet davon ab
- ViewModel waere Overkill fuer einen einzelnen Boolean

## HorizontalPager fuer Wisch-Navigation zwischen Tabs (WICHTIG)
- `HorizontalPager` aus `androidx.compose.foundation.pager` ersetzt separate NavHost-Routes
- Drei Hauptseiten (Dashboard, Tagebuch, Einstellungen) als Pager-Pages statt einzelne Composables
- `pagerState.animateScrollToPage()` fuer programmatischen Tab-Wechsel (von BottomNavBar)
- **`beyondViewportPageCount` = Gesamtzahl der Seiten minus 1** — bei 3 Seiten also `= 2`.
  Damit werden ALLE Seiten vorab im Speicher aufgebaut. Lieber mehr vorladen als zu wenig!
  `beyondViewportPageCount = 1` reicht nur fuer direkte Nachbarn — beim Wischen zu entfernteren
  Seiten entsteht sonst eine spuerbare Verzoegerung weil die Seite erst on-demand aufgebaut wird.
- `LaunchedEffect { snapshotFlow { currentPage }.collect { } }` mit leerem Body ist Verschwendung
  — nur verwenden wenn im collect-Block tatsaechlich etwas passiert (z.B. Analytics, BottomNav-Sync).
- Login, Splash und Entry-Detail bleiben als normale NavHost-Routes (kein Pager)
- **Wiederverwendbar** fuer jede App mit Tab-Navigation die auch Wisch-Gesten braucht

## TextField in AlertDialog: heightIn begrenzen (WICHTIG)
- `TextField` in einem `AlertDialog` waechst unbegrenzt mit dem Inhalt
- Bei langen Texten (z.B. 3-Minuten-Transkription) drueckt es Buttons aus dem sichtbaren Bereich
- **Loesung**: `Modifier.heightIn(max = 300.dp)` auf dem TextField — Text scrollt intern
- Buttons darunter bleiben immer sichtbar, egal wie lang der Text ist
- Gilt fuer JEDEN Dialog der editierbare Texte unbekannter Laenge anzeigt

## Tastatur-Steuerung: Kontextabhaengiger Auto-Focus
- Nach Sprachaufnahme (Text schon vorhanden): Tastatur NICHT automatisch oeffnen — Benutzer will erst lesen
- Bei manuellem Texteintrag (leerer Text): Tastatur SOFORT oeffnen — Benutzer will tippen
- **Loesung**: `if (rawText.isBlank()) { delay(300); focusRequester.requestFocus() }`
- Einfache Unterscheidung: leerer Text = Schreib-Modus, gefuellter Text = Lese-Modus

## Dark Mode: Spotify-Prinzip — Solide Farben, kein Glaseffekt (WICHTIG)
- Im Dark Mode NIEMALS Glasmorphismus verwenden (transparente Overlays, Borders, Gradients)
- Auf AMOLED-Displays erzeugen selbst minimale Transparenzen sichtbare Rechteck-Artefakte
- **Spotify-Prinzip**: Hintergrund `#121212`, Karte `#181818` (+6 Punkte), Erhoeht `#282828`
- Nur solide Farben mit minimalem Helligkeitsunterschied (~6-16 Hex-Punkte)
- KEIN Border, KEIN Shadow, KEIN Gradient — nur die Helligkeitsdifferenz trennt Ebenen
- GlassCard im Dark Mode: nur `.clip() + .background(solidColor)` — nichts anderes
- Text: hoher Kontrast auf dunklem Hintergrund (`#E6E1E5` primary, `#CAC4D0` secondary)
- Akzentfarben (Orange, Neon-Dots) PROFITIEREN vom dunklen Hintergrund — nicht aendern
- Gilt fuer ALLE Android-Apps, nicht nur Best Journal
- Quelle: Spotify, Twitter/X, Discord, Instagram — keine Top-App nutzt Glaseffekte im Dark Mode

## Cloud-Sync-Icon: Persistenter Status statt kurzes Aufblinken
- Benutzer wollen DAUERHAFT sehen ob ihr Backup aktuell ist — nicht nur 2 Sekunden gruen
- **Loesung**: Beim App-Start pruefen ob Google-Konto verbunden → Wolke sofort gruen setzen
- Gruen bleibt bis ein Fehler auftritt oder abgemeldet wird
- Klickbare Wolke mit Legende-Dialog erklaert alle Zustaende (gruen/blau/grau/rot)
- Sofortiger Sync beim Speichern statt 30s Debounce — Benutzer erwartet sofortige Sicherung

## Biometric Lock
- `BiometricPrompt` braucht `FragmentActivity` — `ComponentActivity` reicht NICHT
- `MainActivity` von `FragmentActivity()` erben lassen (statt `ComponentActivity`)
- `DEVICE_CREDENTIAL` als Authenticator erlauben → PIN/Muster als Fallback automatisch
- Lock-Delay: `onPause` Timestamp speichern, in `onResume` pruefen ob >60s vergangen
- `showBiometricPrompt` als `fun` (nicht private) damit SettingsScreen sie aufrufen kann

## SharedPreferences Sign-Out (Race Condition Fix)
- NIEMALS zwei separate `edit().apply()` Aufrufe auf dem gleichen SharedPreferences-Objekt
- **Loesung**: Ein einziger `edit().clear().putString(...).commit()` Block
- `commit()` statt `apply()` vor App-Restart — garantiert synchrones Schreiben
- API-Keys, Theme, Biometric-Setting vor `clear()` lesen und danach zurueckschreiben

## Gmail API (Feedback)
- Gmail API muss im Google Cloud Projekt aktiviert werden (einmalig)
- Scope: `oauth2:https://www.googleapis.com/auth/gmail.send`
- Token via `GoogleAuthUtil.getToken()` — gleicher Mechanismus wie Drive API
- E-Mail als RFC 2822 String bauen, Base64url-kodieren, als JSON `{"raw":"..."}` senden
- `android.util.Base64` verwenden (nicht `java.util.Base64`)
- `UserRecoverableAuthException` abfangen und Consent-Intent zeigen

## Inline-Editing (Tagebuch)
- `TextField` mit transparentem Background sieht aus wie Text, wird bei Tap editierbar
- Auto-Save: Debounced nach 1.5s Inaktivitaet via `Job.cancel()` + `delay()`
- Focus-Timeout: Nach 5s Inaktivitaet `focusManager.clearFocus()` per `LaunchedEffect`
- Keyboard-Dismiss: `WindowInsets.ime.getBottom()` beobachten — bei 0 Focus clearen
- Klick ausserhalb: `Column` mit `clickable(indication=null)` → `focusManager.clearFocus()`

## Text-Verbesserung (Tampermonkey-Style)
- Temperature 0.4 fuer konsistente, praezise Ergebnisse
- maxOutputTokens 8192 um Abschneidung zu verhindern
- Truncation Detection: Wenn Ergebnis <85% der Eingangslaenge → in Chunks aufteilen
- Chunks: Paragraphen-basiert, max 3500 Zeichen, an Satzgrenzen splitten
- Parallele Verarbeitung aller Chunks via `coroutineScope { chunks.map { async { ... } } }`

## Ordner-Umbenennung: ADB/Gradle stoppen + LFS-Pfade anpassen ⭐ WICHTIG
**Problem**: `git mv` auf einen Android-Projektordner schlaegt mit "Permission denied" fehl, weil ADB oder der Gradle-Daemon Dateien darin offen haben. Danach schlaegt `git push` fehl, weil Git-LFS-Regeln in `.gitattributes` auf den alten Ordnernamen zeigen — grosse Dateien werden als neue Git-Objekte statt als LFS-Pointer behandelt.
**Pflicht-Ablauf bei Ordner-Umbenennung:**
1. `./gradlew --stop` (Gradle-Daemon beenden)
2. `adb kill-server` (ADB beenden — haelt File-Handles offen!)
3. `cmd.exe //c "ren AlterName NeuerName"` (Windows-natives Rename)
4. `.gitattributes` SOFORT aktualisieren: alle LFS-Regeln auf neuen Pfad aendern
5. Dateien stagen, LFS-Status pruefen: `git lfs status` — alles muss "LFS → LFS" zeigen
6. Falls Dateien als "Git:" statt "LFS:" erscheinen: `git rm --cached` + `git add` (erzwingt LFS-Filter)
7. Erst DANN committen und pushen
**Zeitersparnis**: Dieser Ablauf haette 6+ Minuten Fehlersuche verhindert.

## NestedScrollConnection: LazyRow in HorizontalPager isolieren
**Problem**: LazyRow (Kategorie-Tabs) leakt Scroll-Gesten an den uebergeordneten HorizontalPager. Ein starkes Wischen in den Kategorien springt zur naechsten Seite.
**Loesung**: `NestedScrollConnection` der im `onPostScroll` und `onPostFling` alle ueberschuessigen horizontalen Gesten konsumiert:
```kotlin
val isolation = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
            Offset(available.x, 0f)
        override suspend fun onPostFling(consumed: Velocity, available: Velocity) =
            Velocity(available.x, 0f)
    }
}
Box(modifier = Modifier.nestedScroll(isolation)) { LazyRow(...) }
```
- Die LazyRow scrollt normal (sie konsumiert was sie braucht)
- Ueberschuss (am Ende der Liste) wird von der Connection geschluckt statt an den Pager weitergereicht

## Splash-Animation: Spring-Bounce vs Tween-Settle
**Problem**: `spring(dampingRatio = 0.45)` erzeugt sichtbares Nachschwingen — der Text huepft nach der Landung nochmal hoch.
**Loesung**: Statt Spring einfache `tween()`-Animation verwenden:
```kotlin
// FALSCH — bounced nach der Landung:
textScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 350f))
// RICHTIG — landet sauber ohne Nachhuepfer:
textScale.animateTo(0.85f, tween(180))  // Squash
textScale.animateTo(1f, tween(300))     // Settle
```
- `dampingRatio < 1.0` = unterdaempft = schwingt nach (bounce)
- `dampingRatio = 1.0` = kritisch gedaempft = kein Nachschwingen
- `tween()` = garantiert kein Ueberschwinger, volle Kontrolle ueber Kurve

## Scaffold NIE um den gesamten NavHost ⭐ WICHTIG
**Problem**: Wenn ein `Scaffold` (mit `bottomBar`) den gesamten `NavHost` umschliesst, betrifft das Padding ALLE Screens — auch Splash, Login, Onboarding. Sobald die Route zu "main" wechselt und die BottomBar erscheint, verschiebt sich der Inhalt des noch sichtbaren Splash Screens nach oben (Layout-Shift waehrend der Fade-Transition).
**Loesung**: Scaffold NUR um die "main"-Route wickeln:
```kotlin
// FALSCH — Scaffold um alles:
Scaffold(bottomBar = { if (route == "main") BottomBar() }) {
    NavHost(...) { composable("splash") {...}; composable("main") {...} }
}
// RICHTIG — Scaffold nur um main:
NavHost(...) {
    composable("splash") { SplashScreen(...) }  // volle Bildschirmhoehe
    composable("main") {
        Scaffold(bottomBar = { BottomBar() }) { padding ->
            HorizontalPager(modifier = Modifier.padding(padding)) {...}
        }
    }
}
```
- Splash und Login laufen vollstaendig isoliert — kein Padding, kein Layout-Shift
- Die BottomBar erscheint erst NACH der vollstaendigen Transition zu "main"
- **Gilt fuer JEDE App** mit Splash/Login/Onboarding + Hauptnavigation

## Papyrus-Textur als Bild-Hintergrund fuer UI-Elemente
- Statt Canvas-gezeichneter Hintergruende: PNG-Bild als `painterResource` laden
- `Image(painter, contentScale = ContentScale.Crop)` fuer saubere Skalierung
- Bild-Optimierung: Auf max 512px Breite + WebP/PNG komprimieren fuer Mobile Performance
- Transparenz-Check: Hintergrund per Bildbearbeitungstool entfernen VOR dem Einbinden
- Padding grosszuegig setzen um dekorative Raender (z.B. Papyrus-Rollen) nicht mit Text zu ueberdecken

## Handschrift-Font (Caveat) lokal einbinden ⭐ WICHTIG
- **Google Fonts Provider funktioniert NICHT zuverlaessig** auf Android — manchmal wird der Font
  nicht geladen (Netzwerk-Timeout, Google Play Services fehlt, etc.)
- **Loesung**: Font als lokale TTF-Datei einbinden (`res/font/caveat.ttf`)
- Font-Family erstellen: `val CaveatFamily = FontFamily(Font(R.font.caveat))`
- Direkt in `Text(fontFamily = CaveatFamily)` verwenden — kein Netzwerk noetig
- **Gilt fuer JEDEN Custom-Font**: Immer lokal einbinden, nie auf Google Fonts Provider verlassen
- Dancing Script war der erste Versuch, Caveat sieht natuerlicher aus (weniger "geschwungen")

## Heartbeat-Puls-Animation mit Tap-to-Excite
- `animateFloatAsState` mit `keyframes` fuer einen natuerlichen Herzschlag-Effekt
- Normaler Puls: Sanftes Scale 1.0 -> 1.02 -> 1.0 (kaum sichtbar, lebendig)
- Tap-to-Excite: Bei Beruehrung 3 schnelle starke Pulse (1.0 -> 1.15 -> 0.95 -> 1.1 -> 1.0)
  dann zurueck zum normalen Puls
- `Modifier.pointerInput` fuer Tap-Detection auf dem animierten Element
- `LaunchedEffect(isExcited)` startet den erhoehten Puls und resettet nach Ablauf
- **Wiederverwendbar** fuer: Logos, Buttons, Avatare, Notification-Icons — alles was "lebendig" wirken soll

## Fliegende Notebooks: Interaktive Canvas-Objekte
- Canvas-gezeichnete Objekte (Notebooks mit Fluegeln) die ueber den Bildschirm fliegen
- `data class FlyingNotebook(x, y, size, rotation, speed, wingPhase, wobble)`
- Animation mit `LaunchedEffect` + `withFrameNanos` fuer fluessige 60fps Bewegung
- **Interaktivitaet**: `pointerInput { detectTapGestures }` + Distanz-Check zum Tap-Punkt
  → Notebooks "fliehen" wenn man sie beruehrt (Speed x3, neue zufaellige Richtung)
- **Groessen-Variation**: 8 Notebooks in 3 verschiedenen Groessen (klein, mittel, gross)
  → sieht natuerlicher aus als einheitliche Groesse
- Canvas-Zeichnung: Notebook-Body + Spiralbindung + Linien + animierte Fluegel
  → `rotate(notebook.rotation)` fuer natuerliche Flugrichtung
- **Performance**: `mutableStateListOf` fuer die Notebook-Liste, Position-Updates nur in `withFrameNanos`

---

## Compose Scroll-Performance (Ruckler vermeiden)

> Quelle: Best Journal Session 2026-04-05 — Dashboard + Tagebuch ruckelten beim Scrollen

### Problem
LazyColumn scrollt nicht fluessig — sichtbare Ruckler auf Samsung S23 Ultra und Fold 6.

### Ursachen und Fixes

| Ursache | Fix | Effekt |
|---------|-----|--------|
| **Canvas-Animation mit vielen Objekten** (40 Partikel × sin/cos pro Frame) | Partikelanzahl auf 15 reduzieren, Animationszyklus verdoppeln (10s→20s) | 63% weniger Draw-Calls pro Frame |
| **`highlightMatches()` ohne remember** (String-Scanning bei jedem Recompose) | `remember(text, query) { highlightMatches(...) }` wrappen | Scan nur bei Aenderung, nicht bei jedem Frame |
| **LazyColumn Items ohne stabile Keys** (positional index) | `item(key = "title_bar")`, `item(key = "all_recommendations")` etc. | Compose kann Items korrekt recyclen statt neu erstellen |
| **Farblisten bei jedem Recompose neu erstellt** | `val colors = remember { listOf(...) }` | Keine Allokation bei Recompose |

### Allgemeine Regeln fuer Compose-Performance

1. **Jedes `item {}` in LazyColumn braucht einen stabilen `key`** — besonders wenn die Liste sich aendern kann
2. **Teure Berechnungen (String-Ops, Formatierung) IMMER in `remember` wrappen** — sonst laufen sie bei jedem Frame
3. **Endlos-Animationen (`infiniteTransition`) sparsam einsetzen** — jede verursacht ~60 Recompositions/Sekunde
4. **Canvas-Animationen**: Max 15-20 Objekte, Trig-Operationen minimieren
5. **`Modifier.shadow()` erzwingt Compositing-Layer** — bei vielen Karten Elevation niedrig halten (4dp statt 8dp)
6. **Brush-Objekte in Modifier-Extensions koennen nicht `remember`t werden** — akzeptabler Overhead, aber bewusst sein

---

## ADB Install & Datenverlust (KRITISCH)

> Quelle: Best Journal Session 2026-04-05 — Daten verloren nach adb uninstall auf S23 Ultra

### Problem
`adb uninstall` loescht ALLE App-Daten: SharedPreferences, Room-Datenbank, interne Dateien.
Nach Neuinstallation sind API-Keys, Modellauswahl, Login-Status und Tagebucheintraege weg.

### Wann passiert das?
Wenn die Debug-APK mit einem **anderen Debug-Keystore** signiert ist als die installierte Version
(z.B. anderer Rechner, neuer Keystore). Android zeigt `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
und die einzige Loesung ist Deinstallation.

### Regeln
1. **VOR `adb uninstall` IMMER den Benutzer warnen** — "Alle App-Daten gehen verloren"
2. **Backup anbieten** — "Tagebucheintraege sichern" vor der Deinstallation
3. **Debug-Keystore synchron halten** — gleichen `~/.android/debug.keystore` auf allen Rechnern
4. **Fallback fuer fehlende SharedPreferences** — App muss mit leeren Prefs sauber starten
5. **Default-Werte muessen funktionieren** — Default-Gemini-Modell muss ein stabiles Modell sein, nicht ein Preview

---

## WAL-Checkpoint bei SQLite-Backup (Room + Google Drive)

> Quelle: Best Journal Session 2026-04-05 — 7 Eintraege gesichert, nur 6 wiederhergestellt

### Problem
Room verwendet WAL-Modus (Write-Ahead Log). Neue Eintraege landen zuerst in der `-wal`-Datei,
nicht in der Haupt-`.db`-Datei. Das Backup kopiert nur die Haupt-Datei → neue Eintraege fehlen.

### Falscher Ansatz
```kotlin
// FALSCH — Room haelt die DB offen, Checkpoint kann blockiert sein
database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
```

### Richtiger Ansatz
```kotlin
// RICHTIG — Room schliessen, eigene Verbindung oeffnen, Checkpoint ausfuehren
database.close()
val db = SQLiteDatabase.openDatabase(dbFile.path, null, OPEN_READWRITE)
val cursor = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
cursor.moveToFirst()  // WICHTIG: Checkpoint wird erst bei moveToFirst() ausgefuehrt!
val busy = cursor.getInt(0)  // 0 = OK, 1 = blockiert
cursor.close()
db.close()
```

### Regeln
1. **Room IMMER schliessen** bevor der WAL-Checkpoint ausgefuehrt wird
2. **rawQuery + moveToFirst()** statt `query().close()` — der Cursor muss gelesen werden
3. **Checkpoint-Ergebnis pruefen** — `busy=1` bedeutet Daten koennten fehlen
4. **Nach Restore: WAL + SHM loeschen** bevor die Backup-DB eingespielt wird

---

## HorizontalPager vs. Swipe-Geste in scrollbaren Layouts

> Quelle: Best Journal Session 2026-04-05 — Text sprang beim Tab-Wechsel

### Problem
`HorizontalPager` in einer `verticalScroll`-Column verursacht Layout-Probleme:
- Text startet 20+ Zeilen tiefer als erwartet
- Beim Seiten-Wechsel springt der Text nach oben (visueller Sprung-Effekt)
- Die Pager-Hoehe aendert sich dynamisch und verschiebt den Scroll-State

### Loesung: Swipe-Geste statt Pager
```kotlin
// Statt HorizontalPager — einfache Gesten-Erkennung
GlassCard(
    modifier = if (hasPages) Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
            if (dragAmount < -40) selectedTab = 1  // links wischen
            if (dragAmount > 40) selectedTab = 0   // rechts wischen
        }
    } else Modifier
) {
    // Normaler if/else fuer den Inhalt — kein Pager noetig
    if (selectedTab == 1) { OriginalContent() } else { ImprovedContent() }
}
```

### Regeln
1. **HorizontalPager NICHT in scrollbaren Layouts** verwenden — Layout-Konflikte
2. **`detectHorizontalDragGestures`** ist leichtgewichtig und verursacht keine Hoehen-Probleme
3. **`beyondViewportPageCount = 1`** hilft NICHT — das Grundproblem bleibt

---

## Dual-Device Debug-Workflow (S23 Ultra + Fold 6)

> Quelle: Best Journal Session 2026-04-05 — Paralleles Testen auf 2 Geraeten

### Setup
- S23 Ultra (`R5CW206F0ZM`): Android-Version (BestJournalAndroid)
- Fold 6 (`RFCX70KTDFX`): Frank-Version (BestJournalFrank)
- Beide gleichzeitig per USB angeschlossen

### Workflow (PFLICHT nach jeder Code-Aenderung)
```bash
# Parallel bauen
./gradlew assembleDebug  # in beiden Projektverzeichnissen

# Installieren + SOFORT neu starten (Benutzer oeffnet nie manuell!)
adb -s R5CW206F0ZM install -r .../app-debug.apk && \
adb -s R5CW206F0ZM shell am force-stop com.bestjournal.app.debug && \
adb -s R5CW206F0ZM shell am start -n com.bestjournal.app.debug/com.bestjournal.app.MainActivity

adb -s RFCX70KTDFX install -r .../app-debug.apk && \
adb -s RFCX70KTDFX shell am force-stop com.entropyjournal.debug && \
adb -s RFCX70KTDFX shell am start -n com.entropyjournal.debug/com.entropyjournal.MainActivity
```

### Regeln
1. **Nach JEDEM Install automatisch starten** — `force-stop` + `am start`
2. **Parallel installieren** — beide adb-Befehle koennen gleichzeitig laufen
3. **Device-IDs aus SESSION-RULES.md** — nicht raten oder hardcoden

---

## ADB Binary-Dateien ueber run-as extrahieren

> Quelle: Best Journal Session 2026-04-05 — SQLite-DB vom Geraet herunterladen

### Problem
`adb shell run-as com.app cat databases/db.file` korrumpiert binaere Dateien (Null-Bytes etc.).

### Loesung: Base64-Encoding
```bash
# Auf dem Geraet Base64-kodieren, dann lokal dekodieren
adb -s DEVICE shell "run-as com.app sh -c 'base64 databases/mydb.db'" > /tmp/db_b64.txt
python3 -c "
import base64
data = base64.b64decode(open('/tmp/db_b64.txt').read().strip())
open('/tmp/mydb.db', 'wb').write(data)
"
# Jetzt kann die DB lokal mit sqlite3/Python analysiert werden
```

---

## Gemini API Fehlerbehandlung (Model-Fallback + klare Meldungen)

> Quelle: Best Journal Session 2026-04-05 — HTTP 400 ohne sichtbare Fehlermeldung

### Problem
1. Dashboard zeigte "Erstelle Tagebucheintraege" statt der echten Fehlermeldung
2. Default-Modell `gemini-3-flash-preview` funktionierte nicht mit allen API-Keys
3. Vertauschte API-Keys (Groq ↔ Gemini) gaben nur kryptisches "HTTP 400"

### Regeln
1. **Fehlermeldungen IMMER anzeigen** — nie hinter generischen Texten verstecken
2. **Default-Modell muss ein stabiles, allgemein verfuegbares Modell sein** (z.B. `gemini-2.5-flash-lite`)
3. **Model-Fallback einbauen** — bei HTTP 400 automatisch auf Default-Modell wechseln
4. **HTTP-Status-Codes uebersetzen** — 400 = "Modell nicht verfuegbar", 401 = "Key ungueltig", 429 = "Zu viele Anfragen"
5. **API-Key-Laenge loggen** — hilft sofort beim Erkennen vertauschter Keys (Gemini = 39 Zeichen)

---

## Klammerstruktur in grossen Compose-Dateien

> Quelle: Best Journal Session 2026-04-05 — Falsche Klammer entfernt, TopActionsBlock kaputt

### Problem
In DashboardScreen.kt (800+ Zeilen) war eine ueberzaehlige `}` vorhanden. Beim Entfernen
wurde die FALSCHE Klammer entfernt → Column schloss zu frueh → Items stapelten sich uebereinander.

### Diagnose-Tool
```python
# Klammerbalance pruefen — findet sofort wo es bricht
with open('Datei.kt', 'r') as f:
    lines = f.readlines()
depth = 0
for i, line in enumerate(lines, 1):
    for ch in line:
        if ch == '{': depth += 1
        elif ch == '}': depth -= 1
    if depth < 0:
        print(f'Line {i}: depth went negative ({depth})')
print(f'Final depth: {depth}')
```

### Regeln
1. **VOR dem Entfernen einer Klammer: Klammerbalance-Script laufen lassen**
2. **NACH dem Edit: Nochmal pruefen** — depth muss am Ende 0 sein
3. **Bei grossen Compose-Dateien (500+ Zeilen)**: Immer die umgebende Funktion identifizieren bevor eine Klammer angefasst wird

---

## Room database.close() bricht alle nachfolgenden Saves (KRITISCH)

> Quelle: Best Journal Session 2026-04-05 — Speichern ging nur beim ersten Mal

### Problem
`database.close()` in der WAL-Backup-Funktion schliesst die Room-Singleton-Instanz.
Danach schlaegt JEDER `saveJournalEntryUseCase(entry)`-Aufruf fehl mit
`JobCancellationException` — die App kann keine Eintraege mehr speichern bis sie
neu gestartet wird.

### Root Cause Kette
1. Nutzer speichert Eintrag → `saveEntry()` → `triggerSync()` → `backup()`
2. `backup()` ruft `database.close()` auf (fuer WAL-Checkpoint)
3. Room-Singleton ist jetzt geschlossen
4. Naechster `saveEntry()` → `viewModelScope.launch { saveJournalEntryUseCase(entry) }`
   → Room kann nicht mehr schreiben → Job wird gecancelt

### FALSCHER Ansatz
```kotlin
// FALSCH — schliesst Room permanent, alle folgenden Writes scheitern
database.close()
val db = SQLiteDatabase.openDatabase(dbFile.path, null, OPEN_READWRITE)
db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
db.close()
```

### RICHTIGER Ansatz
```kotlin
// RICHTIG — nutzt Room's eigene Verbindung, Room bleibt offen
try {
    val roomDb = database.openHelper.writableDatabase
    val cursor = roomDb.query("PRAGMA wal_checkpoint(TRUNCATE)")
    cursor.moveToFirst()
    cursor.close()
} catch (_: Exception) { /* Checkpoint ist best-effort */ }
```

### Zweiter Bug: viewModelScope wird gecancelt
Android kann den viewModelScope canceln (Freeze, Hintergrund, etc.).
`viewModelScope.launch { save() }` schlaegt dann fehl.

```kotlin
// FALSCH — viewModelScope kann gecancelt sein
viewModelScope.launch { saveJournalEntryUseCase(entry) }

// RICHTIG — unabhaengiger Scope der IMMER laeuft
CoroutineScope(Dispatchers.IO).launch {
    try {
        val savedId = saveJournalEntryUseCase(entry)
        withContext(Dispatchers.Main) { resetState() }
    } catch (e: Exception) { /* Fehlerbehandlung */ }
}
```

### Regeln
1. **NIEMALS `database.close()` auf einem Room-Singleton aufrufen** — es gibt keinen Weg zurueck
2. **WAL-Checkpoint ueber `database.openHelper.writableDatabase.query()`** statt eigene Verbindung
3. **Kritische Operationen (Save, Sync) in eigenem CoroutineScope** statt viewModelScope
4. Wenn ein Fix `database.close()` enthaelt → SOFORT pruefen ob danach noch Writes kommen

---

## Pinch-to-Zoom + HorizontalPager: awaitEachGesture PFLICHT (KRITISCH)

> Datum: 2026-04-09 | Quelle: BestJournal Foto-Feature | 3 fehlgeschlagene Versuche bevor Loesung gefunden

### Problem

Fullscreen-Bildbetrachter mit **gleichzeitigem** Pinch-to-Zoom UND horizontalem Blaettern
zwischen Bildern (HorizontalPager). Die Standard-Compose-APIs fuer Gesten blockieren den Pager.

### Was NICHT funktioniert (3 Versuche, alle gescheitert)

| Versuch | API | Warum es scheitert |
|---------|-----|-------------------|
| 1 | `detectTransformGestures()` | Verschluckt ALLE Touch-Events inkl. Einzelfinger-Swipes. Pager bekommt nichts. |
| 2 | `Modifier.transformable()` | Gleiche Ursache — konsumiert intern alle Pointer-Events. |
| 3 | `pointerInput(scale)` mit bedingtem `detectTransformGestures` | `detectTransformGestures` konsumiert auch im "nur Zoom"-Modus den initialen Down-Event. |

### Die EINZIGE funktionierende Loesung: awaitEachGesture

```kotlin
.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }

            if (pressed >= 2) {
                // Multi-touch: Zoom + Pan, Events konsumieren
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                scale = (scale * zoom).coerceIn(1f, 5f)
                if (scale > 1f) {
                    offsetX += pan.x; offsetY += pan.y
                    // ... coerceIn fuer maxX/maxY ...
                } else { offsetX = 0f; offsetY = 0f }
                event.changes.forEach { it.consume() }

            } else if (pressed == 1 && scale > 1f) {
                // Einzelfinger + gezoomt: Pan, Events konsumieren
                val pan = event.calculatePan()
                offsetX += pan.x; offsetY += pan.y
                // ... coerceIn ...
                event.changes.forEach { it.consume() }
            }
            // Einzelfinger + NICHT gezoomt: NICHT konsumieren → Pager bekommt Swipe!
        } while (event.changes.any { it.pressed })
    }
}
```

### Warum das funktioniert

Der Schluessel ist **selektives Konsumieren**: `event.changes.forEach { it.consume() }` wird
NUR aufgerufen wenn 2+ Finger erkannt werden ODER wenn bereits gezoomt ist. Bei normalem
Einzelfinger-Wischen wird das Event NICHT konsumiert — es "faellt durch" zum HorizontalPager,
der es als Swipe-Geste interpretiert.

### Zusaetzlich benoetigte Mechanismen

```kotlin
// Pager-Scrolling deaktivieren wenn gezoomt
var currentPageZoomed by remember { mutableStateOf(false) }
HorizontalPager(userScrollEnabled = !currentPageZoomed) { page ->
    // Zoom-State tracken
    LaunchedEffect(scale, pagerState.currentPage) {
        if (page == pagerState.currentPage) currentPageZoomed = scale > 1f
    }
    // Zoom zuruecksetzen beim Seitenwechsel
    LaunchedEffect(pagerState.currentPage) {
        if (page != pagerState.currentPage) { scale = 1f; offsetX = 0f; offsetY = 0f }
    }
}
```

### Doppeltipp-Zoom (separater pointerInput)

```kotlin
.pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = {
            if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
            else { scale = 2.5f }
        },
        onTap = { fullScreenPhotoPath = null },  // Schliessen bei Tap
    )
}
```

### Benoetigte Imports

```kotlin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
```

### Regeln

1. **NIEMALS `detectTransformGestures()` verwenden** wenn ein HorizontalPager im Spiel ist
2. **NIEMALS `Modifier.transformable()`** verwenden wenn ein HorizontalPager im Spiel ist
3. **IMMER `awaitEachGesture`** mit manueller Pointer-Zaehlung fuer Zoom+Pager-Kombination
4. **Events nur konsumieren wenn noetig** — das ist der Kern der Loesung
5. **graphicsLayer fuer die Transformation** — scaleX, scaleY, translationX, translationY

## Terminal-Input leeren ohne Interrupt: Ctrl+U ist der einzige sichere Weg (KRITISCH)

**Problem:** In Voice-Overlays (TerminalVoiceOverlay-Windows/macOS, ClaudeVoiceOverlay)
soll ein X-Button die aktuelle Input-Zeile im Terminal leeren. Wenn Claude Code CLI
gerade eine Aufgabe bearbeitet, darf das X **niemals** die Aufgabe abbrechen.

**Was NICHT funktioniert (mehrfach in verschiedenen Sessions ausprobiert):**

| Versuch | Warum es scheitert |
|---------|-------------------|
| **Ctrl+C** | Sendet SIGINT → Claude Code CLI, bash, zsh, pwsh unterbrechen die laufende Aufgabe ("Interrupted"). Der Benutzer muss jedes Mal "mache weiter" sagen. |
| **Home + Shift+End + Delete** | Terminals haben keine Cursor-basierte Textselektion — Shift+End selektiert NICHTS. Ergebnis: Der Text bleibt stehen, X tut nichts. Selektion per Keyboard ist ein reines GUI-Konzept (WPF/Cocoa), in TUIs unbekannt. |
| **Escape** | In Claude Code CLI stoppt Escape die aktuelle Generierung (auch das ist ein Interrupt) — nicht das Input-Leeren. Und in normalen Shells ist Escape oft unbelegt. |
| **Backspace mehrfach** | Unzuverlaessig (Laenge des Textes unbekannt) und laut. |
| **Ctrl+A + Backspace** | Funktioniert in Electron-Apps (Claude Desktop, Codex Desktop), aber NICHT in echten Terminals — Ctrl+A ist dort "move to beginning of line", nicht "select all". |

**Was zuverlaessig funktioniert: `Ctrl+U`** — der readline-Standard "kill line".

| Umgebung | Ctrl+U-Verhalten |
|----------|-----------------|
| Claude Code CLI (Ink/React TUI) | Leert Input — explizit so dokumentiert |
| PowerShell / PSReadLine | Kill line from cursor to start |
| bash | Kill line from cursor to start |
| zsh | Kill entire line |
| cmd.exe (neu) | Kill line |
| Node.js REPL | Kill line |
| Python REPL | Kill line |
| Codex CLI | Leert Input |

Entscheidend: **Ctrl+U sendet nie SIGINT**. Auch wenn das Input leer ist (weil gerade
eine Aufgabe laeuft), ist Ctrl+U ein harmloser No-Op — die Aufgabe laeuft
unbeeinflusst weiter. Das ist das Verhalten das man fuer einen X-Button braucht.

**Implementation (plattformspezifisch gleiches Konzept):**

```csharp
// Windows (C#, Win32 keybd_event)
private const ushort VK_U = 0x55;
public static void ClearLine(IntPtr terminalHwnd)
{
    BringToForeground(terminalHwnd);
    SendKeyCombo(Win32.VK_CONTROL, VK_U);  // Ctrl+U
}
```

```swift
// macOS (Swift, CGEvent)
static func clearLine() {
    activateTerminal()
    usleep(150_000)
    // 'u' = 0x20 on macOS CGKeyCode; Ctrl+U = kill line
    sendKeyCombo(keyCode: 0x20, flags: .maskControl)
}
```

**Verifiziert am 2026-04-23:** Funktioniert 100% in TerminalVoiceOverlay-Windows
gegen Claude Code CLI in Windows Terminal / PowerShell. Vom Benutzer explizit
bestaetigt: "Einwandfrei. Das funktioniert zu 100%."

**Wichtigste Regel fuer zukuenftige CLI-Tools:** Wenn "Input leeren ohne Interrupt"
gebraucht wird → IMMER Ctrl+U. Nichts anderes ausprobieren. Diese Frage ist
endgueltig geklaert.

**Trennlinie zwischen "Terminal" und "Electron-App" (wichtig!):**
- **Terminals** (PowerShell, Windows Terminal, iTerm, Terminal.app, bash) → **Ctrl+U**
- **Electron-Apps** (Claude Desktop, Codex Desktop) → **Ctrl+A + Backspace**
  (weil diese echte Text-Selektion per Keyboard unterstuetzen)

Das ist der Grund warum TerminalVoiceOverlay und ClaudeVoiceOverlay unterschiedliche
Tastenkombis brauchen, obwohl sie ~80% Code teilen.


## Firebase + SHA-1 + google-services.json: Der vollstaendige Workflow (PFLICHT — KRITISCH)

> Vorfall 2026-04-29: Stundenlange Diagnose von "Sprachaufnahme nutzt lokales Whisper
> statt Groq" und "Verbessern mit Gemini funktioniert nicht" — weil zwei Folgeschritte
> nach Hinzufuegen eines neuen SHA-1 in Firebase vergessen wurden.

### Symptome bei diesem Fehlertyp

- **Sign-In funktioniert** (taeuscht: nutzt einen anderen Auth-Pfad)
- ABER: Logcat zeigt `Firebase Installations failed to get installation auth token for fetch`
- ABER: Sprachaufnahme nutzt lokales Whisper statt Groq (Code faengt Exception ab und macht stillen Fallback)
- ABER: Verbessern mit Gemini funktioniert nicht
- ABER: Cloud Messaging tot
- ABER: Remote Config liefert leere Strings

Wenn diese Symptome auftreten, hat man den falschen Eindruck dass irgendwas mit der
App selbst nicht stimmt — dabei ist es ein Cloud-Konfigurations-Problem.

### Der vollstaendige 5-Schritt-Workflow nach JEDEM SHA-1-Hinzufuegen

**Schritt 1 — SHA-1 in Firebase Console hinzufuegen.**
- Firebase Console → Projekteinstellungen → bei der Android-App → "Fingerabdruck hinzufuegen"
- Den passenden SHA-1 (z.B. App-Signaturschluessel-Zertifikat aus der Play Console) eintragen
- Nicht den falschen kopieren! Es gibt zwei in der Play Console:
  - **App-Signaturschluessel-Zertifikat** = Google's finaler Signing-Key (wird fuer Play-Store-Versionen geprueft)
  - **Upload-Schluessel-Zertifikat** = lokaler Upload-Key (wird nur beim Hochladen geprueft)
- Fuer Firebase Installations brauchst du den App-Signaturschluessel-SHA-1

**Schritt 2 — google-services.json neu herunterladen.**
- Im gleichen Firebase-Console-Bereich wo der SHA-1 hinzugefuegt wurde
- Bei der Android-App das Download-Icon klicken
- Datei landet in Downloads-Ordner

**Schritt 3 — google-services.json an die richtige Stelle kopieren.**
- Bei BestJournal: nach `~/SK/BestJournalAndroid/google-services-release.json` kopieren
  (der `syncSecretsFromSk`-Gradle-Task kopiert sie bei jedem Build von dort ins Projekt)
- Bei einfacheren Projekten ohne SK-Folder-Setup: direkt `app/google-services.json` ueberschreiben
- KEIN direktes Austauschen im Projekt-Ordner wenn der SK-Workflow aktiv ist — wird sonst beim naechsten Build ueberschrieben

**Schritt 4 — API-Schluessel-Einschraenkungen aktualisieren (KRITISCH und oft vergessen!).**
- Cloud Console → APIs & Services → Credentials → den passenden API-Schluessel oeffnen
- Bei "Anwendungseinschraenkungen" → "Android-Apps" pruefen ob bereits Android-App-Filter aktiv
- WENN ja: Den NEUEN SHA-1 zur erlaubten Liste hinzufuegen (Add-Button)
  - Paketname (z.B. com.bestjournal.app)
  - Fingerabdruck (SHA-1 mit Doppelpunkten)
- Speichern, 5 Minuten Propagierungs-Zeit
- WENN nein (keine Restriction): nichts zu tun, Schritt 4 entfaellt

Dieser Schritt wird gerne uebersehen weil er in einer voellig anderen UI ist (Cloud Console
statt Firebase Console). Aber: Wenn der API-Schluessel Android-Einschraenkungen hat
und der neue SHA-1 nicht drin steht, bleiben alle Firebase-Auth-Aufrufe blockiert,
auch wenn sonst alles richtig konfiguriert ist.

**Schritt 5 — App neu bauen und 5 Minuten warten.**
- `./gradlew bundleRelease` (oder `assembleDebug` fuer Debug-Builds)
- AAB hochladen in Play Console
- 5 Minuten warten bevor man testet (Cloud-Propagierung)

### Warum Sign-In trotzdem funktioniert (Diagnose-Falle)

Sign-In nutzt **Google Play Services** direkt. Die machen die SHA-1-Pruefung
ueber einen anderen Pfad (Auth-Backend) als Firebase Installations. Daher:
- Sign-In klappt nach Schritt 1 (SHA-1 in Firebase) sofort
- ABER alle Firebase-Auth-abhaengigen Services (Remote Config, AI Logic, Cloud Messaging)
  brauchen ZUSAETZLICH die google-services.json (Schritt 2-3) und API-Schluessel-Einschraenkung (Schritt 4)

Der "Sign-In klappt aber sonst nichts"-Zustand ist deshalb ein klassischer Hinweis
auf vergessene Schritte 2-4.

### Memory-Trick fuer die Zukunft

Ich denk an die "5 F" — **Firebase, File, Folder, Filter, Fertig:**
1. **F**irebase: SHA-1 hinzufuegen
2. **F**ile: google-services.json runterladen
3. **F**older: An die richtige Stelle kopieren (SK-Folder bei BestJournal, sonst app/)
4. **F**ilter: API-Schluessel-Einschraenkung erweitern in Cloud Console
5. **F**ertig: Build, hochladen, 5 Min warten


---

## Custom Global Hotkeys auf Windows (.NET/WPF) — die 3 Stille-Killer ⭐⭐

> **Quelle:** TerminalVoiceOverlay-Windows, Vorfall 2026-05-15.
> Commits #2234-#2240 (6 Commits, mehrere Stunden Debug-Zeit).
> Frank's Worte: "Das darf beim naechsten Mal nicht mehr passieren."

Globale Hotkeys mit ungewoehnlichen Modifier-Kombinationen (Win+Alt+X, Shift+Alt+X, ...) in einer .NET-WPF-App mit SQLite-Persistenz und einem Low-Level Keyboard Hook haben **drei voneinander unabhaengige Bug-Klassen**, die zusammen das Feature still killen koennen. Einzeln subtil, in Kombination toedlich.

### Killer 1 — EF Core char?/short?/byte? Mapping auf SQLite

**Symptom:** Schreiben in die DB funktioniert, Lesen liefert immer `null` zurueck — obwohl die SQLite-Spalte sichtbar korrekte Werte enthaelt.

**Root cause:** EF Core 8+ default-Mapping fuer `char?` auf SQLite ist **INTEGER** (UTF-16 code unit). Eine manuell per `ALTER TABLE ... ADD COLUMN ... TEXT NULL` angelegte TEXT-Spalte kollidiert. SQLite's dynamische Typisierung (storage class != declared type) maskiert den Bug: Schreiben legt einen Wert ab, aber EF Core's Read-Pfad findet keinen passenden Typ und faellt silent auf `null`.

**Fix in der EntityTypeConfiguration:**
```csharp
b.Property(p => p.HotkeyLetter)
    .HasConversion(
        v => v.HasValue ? v.Value.ToString() : null,
        v => string.IsNullOrEmpty(v) ? (char?)null : v[0])
    .HasMaxLength(1);
```

**Gilt analog fuer alle "exotischen" .NET-Typen** (char, byte, short, sbyte, Guid mit explicit type, custom value types). Faustregel: bei jedem Property, das **kein** `string`/`int`/`long`/`bool`/`double`/`DateTime` ist, einen expliziten `HasConversion` setzen.

### Killer 2 — Bootstrap-Pfad neben Render-Pfad

**Symptom:** Hotkey funktioniert direkt nach der Zuweisung im UI, aber nach App-Restart ist die in-memory-Registry leer und der Hook gibt alle Tastendruecke durch.

**Root cause:** Bei den meisten Hotkey-Implementierungen gibt es **zwei Pfade** die die Lookup-Tabelle befuellen:
1. **Render-Pfad** (z.B. nach jeder UI-Aktualisierung): pflegt die Tabelle waehrend die App laeuft
2. **Bootstrap-Pfad** (einmal beim App-Start): laedt vor dem ersten UI-Render aus der DB

Bei der Feature-Erweiterung wird typisch nur der Render-Pfad angepasst, der Bootstrap-Pfad bleibt auf der alten Feature-Variante stehen.

**Fix:** Im Projekt nach allen Aufrufen der Registry-/Cache-Methode greppen. Jeder Aufrufer muss erweitert werden, nicht nur die offensichtlichste UI-Stelle. Pflicht-Test: **App neu starten** und Feature ohne UI-Refresh testen.

### Killer 3 — Modifier-Konflikt beim SendCtrlV

**Symptom:** Hook feuert nachweislich (File-Log zeigt `FIRE`), Clipboard wird gesetzt, `BringToForeground` laeuft (Cursor flackert) — aber im Zielfenster landet **kein Text**.

**Root cause:** Bei einem Hotkey-getriggerten Paste haelt der User die Trigger-Modifier-Tasten (Win, Alt, Shift) **physisch noch gedrueckt**, waehrend `SendCtrlV` ueber `keybd_event` ein virtuelles `Strg+V` schickt. Windows sieht dann `Win+Alt+Strg+V` statt nur `Strg+V` — kein Paste mehr, sondern eine undefinierte Tastenkombi.

Bei Strg+1..9-Hotkeys faellt das **nicht auf**, weil Ctrl der gewollte Modifier ist und mit dem Paste-Modifier uebereinstimmt. Sobald der Trigger-Modifier != Ctrl ist (Win+, Alt+, Shift+Alt+, ...), bricht das Paste-Verhalten still.

**Fix:** Vor `SendCtrlV` synthetische KEYUP-Events fuer **alle nicht-Ctrl-Modifier** senden:
```csharp
private static void ReleaseNonCtrlModifiers()
{
    void Up(ushort vk)
    {
        byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
        Win32.keybd_event((byte)vk, scan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
    }
    Up(0x5B); Up(0x5C); // VK_LWIN, VK_RWIN
    Up(0xA4); Up(0xA5); // VK_LMENU, VK_RMENU
    Up(0xA0); Up(0xA1); // VK_LSHIFT, VK_RSHIFT
    System.Threading.Thread.Sleep(20); // OS-Verarbeitung
}
```

Hardware-KEYUP-Events die spaeter ankommen (wenn der User physisch loslaesst) sind NoOps, weil der virtuelle State bereits "up" ist.

### Diagnose-Hebel: File-Logging in %TEMP%

Bei WPF-Apps ohne angehaengte Console geht `Console.WriteLine` ins Leere. Fuer Hook-Diagnose ist file-basiertes Logging Gold wert. Ohne Logfile haetten wir die drei Bug-Klassen oben nicht in dieser Reihenfolge gefunden — ein Bug haetten den naechsten maskiert.

```csharp
private static void LogHotkeyEvent(string message)
{
    try
    {
        string path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "TVO-hotkey.log");
        System.IO.File.AppendAllText(path, $"{DateTime.Now:HH:mm:ss.fff} {message}{Environment.NewLine}");
    }
    catch { /* never block the hook thread */ }
}
```

Logge: Bootstrap-Result (Anzahl geladene Eintraege), Hook-Eingang mit Modifier-Status, GATE-Pruefung (Vordergrund-Pruefung), FIRE-Event mit Paste-Laenge. Das macht Hook-Bugs in Minuten sichtbar.

### Pflicht-Checkliste fuer neue Hotkey-Features auf Windows

```
[ ] DB-Spaltentyp passt zum .NET-Property-Typ
[ ] EF-Konfiguration: explicit HasConversion fuer exotische Typen
[ ] ALLE Code-Pfade die die Lookup-Tabelle befuellen erweitert (Render UND Bootstrap UND Restore)
[ ] Falls Trigger-Modifier != Ctrl: ReleaseNonCtrlModifiers vor SendCtrlV
[ ] File-Logging fuer Bootstrap, Hook-Eingang, GATE, FIRE
[ ] Test nach App-Restart (NICHT nur nach UI-Zuweisung)
[ ] Test mit physisch gehaltenem Modifier (kein "press-and-release"-Test allein)
[ ] Test in der Zielanwendung (Terminal, Editor) — nicht nur Notepad
```

### Memory-Trick fuer die Zukunft

Ich merke mir die **3 K** — **Konvertierung, Kaltstart, Konflikt:**
1. **K**onvertierung: EF-Mapping passt zum DB-Typ?
2. **K**altstart: Bootstrap-Pfad erweitert?
3. **K**onflikt: Modifier-Release vor Paste?

Wenn alle drei "ja" sind, funktioniert der Hotkey. Wenn einer "nein" ist, faellt das Feature still aus — und der naechste maskiert die anderen.

---

## Windows/WPF: Transparentes Overlay ruckelfrei ueber Text gleiten lassen ⭐⭐⭐ NEU 2026-05-25

**Kontext:** WPF-Overlay (`AllowsTransparency=True`, WindowStyle=None, Topmost), abgerundete
halbtransparente Pille, schwebt ueber einem Terminal und gleitet beim Orientierungswechsel
(horizontal↔vertikal). Problem: ruckelt/flackert beim Gleiten ueber weisser CLI-Schrift.
Verifiziert in Voice Terminal Overlay (TVO).

**Wurzelursache:** `AllowsTransparency=True` = Layered Window (`UpdateLayeredWindow`, CPU-Pfad).
Ein transluzentes Layered-Fenster ueber Text zu bewegen ist nicht von selbst vsync-synchron →
Flackern/Ruckeln. WPF hat zudem keinen Compositor-Thread (dotnet/wpf #133).

**Loesung — HYBRID nach Fenster-FORM (nicht nach Richtung):**

| Bewegtes Fenster | Technik | Warum |
|---|---|---|
| **flach/kurz** (horizontale Leiste) | kleines Fenster pro Frame per `SetWindowPos` verschieben **+ `DwmFlush()` nach jedem Move** | DwmFlush synct den (synchronen!) Move mit der DWM-Komposition des Terminals → kein Flackern, perfekt glatt. Kleine Flaeche = billig. |
| **hoch/schmal** (vertikale Saeule, 96×612) | Fenster STEHEN lassen, nur **Inhalt per `TranslateTransform` + `CacheMode=BitmapCache`** gleiten | Hohes transluzentes Fenster zu verschieben ist zu teuer (ruckelt). Schmale 96px-Flaeche als stehender Content-Glide = transparent UND fluessig. |

Faustregel: **breit→Fenster-Move+DwmFlush, schmal-hoch→Content-Transform.** Content-Glide NUR
fuer schmale Fenster (bei breitem Fenster ist die stehende Flaeche riesig → flackert ueber Text).

**V-Sync / Bildrate (wichtig):** KEINE feste `Timeline.DesiredFrameRate` setzen, die kein
sauberer Teiler der Monitor-Bildrate ist. 50fps bei 60/120Hz → Beating → Ruckeln; 120fps → die
Layered-Flaeche kann das nicht halten → schlimmer. **WPF-Default (vsync-gebunden) ist am
glattesten** — Bildrate gar nicht erzwingen.

**Tempo:** Dauer streckenabhaengig `clamp(dist*1.3, floor, 600ms)`, aber der kurzen Strecke eine
hoehere Untergrenze (~500ms) geben, sonst wirkt sie schneller als die lange.

**Verworfen:** `DoubleAnimation` auf `Window.Left/Top` (ruckelt); DWM-Umstellung
(`AllowsTransparency=False`+Acrylic) = glatt, aber Look wird milchig → Benutzer abgelehnt;
opake Pille waehrend Glide → Benutzer will durchsichtig.

**DwmFlush P/Invoke:** `[DllImport("dwmapi.dll")] static extern int DwmFlush();` — Schleife als
`Dispatcher.BeginInvoke(DispatcherPriority.Render)` (nicht in `CompositionTarget.Rendering`,
damit das blockierende DwmFlush nicht im Render-Callback sitzt).

**Echte 120Hz mit Per-Pixel-Look** (nicht umgesetzt): braeuchte `Windows.UI.Composition`-Interop
(Compositor-Thread via HwndHost) oder WinUI 3.

Quellen: support.microsoft.com/help/938660 · flutter/flutter#130683 (DwmFlush) ·
dotnet/wpf#133 · learn.microsoft.com/.../animation-tips-and-tricks

---

## Chrome Live Caption (Untertitel) umgehen via Web Audio API (NEU 2026-05-29) ⭐

**Problem:** Eine Chrome-/Chromium-App spielt Audio ab (z.B. TTS / Vorlesen) über ein
HTMLMediaElement (`new Audio()` / `<audio>`). Hat der Nutzer Chromes **Live-Untertitel**
(Live Caption) aktiviert, transkribiert Chrome dieses Audio automatisch und blendet eine
schwebende Untertitel-Box ein — unerwünscht bei eigenem TTS-/Vorlese-Audio. Die App selbst
hat dabei keinerlei Untertitel-Code; die Untertitel kommen rein vom Browser-Feature.

**Root Cause:** Im Chromium-Quellcode hängt der `SpeechRecognitionClient` (SODA / Live
Caption) ausschließlich in `media/renderers/audio_renderer_impl.cc` — und der `AudioRendererImpl`
bedient **nur die Media-Pipeline** (HTMLMediaElement). Audio aus der **Web Audio API** läuft
durch einen anderen Renderpfad **ohne** diesen Hook.

**Lösung:** Audio über die Web Audio API statt über `<audio>` abspielen:

```js
const ctx = new AudioContext();
if (ctx.state === "suspended") await ctx.resume();
const audioBuf = await ctx.decodeAudioData(arrayBuffer.slice(0)); // slice: decode "detached" den Buffer
const src = ctx.createBufferSource();
src.buffer = audioBuf;
src.connect(ctx.destination);
src.onended = () => { /* nächstes Stück abspielen */ };
src.start();
```

**Fallstricke:**
- `decodeAudioData` "detached" den übergebenen ArrayBuffer → immer mit einer Kopie
  (`.slice(0)`) dekodieren, wenn das Original noch gebraucht wird (z.B. für ein Fallback).
- AudioContext kann "suspended" starten → `await ctx.resume()` und `ctx.state === "running"`
  prüfen, sonst spielt nichts und `onended` feuert nie (Queue hängt).
- Es gibt **keine** Extension-API und **kein** HTML-Attribut, um Live Caption pro Element
  auszunehmen — Web Audio ist der einzige codeseitige Hebel. Alternative für den Nutzer:
  Live Caption global aus unter `chrome://settings/accessibility`.
- **Robustheit (Pflicht):** Immer einen Fallback auf `<audio>` einbauen, falls Web Audio
  scheitert (kein AudioContext, suspended, decode-Fehler) — dann spielt das Audio in jedem
  Fall, schlimmstenfalls wieder mit Untertiteln.
- Caveat: Quellcode-belegt; theoretisch könnte Live Caption künftig auf Browser-Prozess-Ebene
  ansetzen. Stand 2026 greift der Web-Audio-Weg aber zuverlässig.

**Gilt für:** alle Chromium-Browser (Chrome, Edge, Brave) auf jedem OS. Relevant für jede
eigene Audio-Wiedergabe (TTS, Sound-Effekte, Player) in Web-Apps und Erweiterungen.

**Quelle:** Vorlese-Overlay (Chrome-Erweiterung), Commit #1191. Chromium-Referenz:
`media/renderers/audio_renderer_impl.cc` (SpeechRecognitionClient-Hook nur in der Media-Pipeline).

