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

**UI & Navigation:**
- [HorizontalPager fuer Wisch-Navigation](#horizontalpager-fuer-wisch-navigation-zwischen-tabs-wichtig) ⭐
- [Scroll-Pattern: LazyRow](#scroll-pattern-lazyrow-statt-rowhorizontalscroll)
- [TextField in AlertDialog: heightIn begrenzen](#textfield-in-alertdialog-heightin-begrenzen-wichtig) ⭐
- [Tastatur-Steuerung: Kontextabhaengiger Auto-Focus](#tastatur-steuerung-kontextabhaengiger-auto-focus)
- [Inline-Editing (Tagebuch)](#inline-editing-tagebuch)
- [Shimmer/Ladebalken](#shimmerladebalken)
- [Kategorie-Icons](#kategorie-icons)
- [Cloud-Sync-Icon: Persistenter Status](#cloud-sync-icon-persistenter-status-statt-kurzes-aufblinken)

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

**Sonstiges:**
- [Theme-Toggle](#theme-toggle-separates-sharedpreferences-fuer-quick-toggle)
- [Hilt/Dagger](#hiltdagger)
- [Sonnenauf-/untergang](#sonnenauf-untergang)

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
- `snapshotFlow { pagerState.currentPage }` synchronisiert Pager → BottomNavBar
- `beyondViewportPageCount = 1` holt Nachbarseiten vor — fluessigeres Wischen
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
