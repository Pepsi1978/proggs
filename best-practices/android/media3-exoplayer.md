# Media3 / ExoPlayer — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man die Audio-Wiedergabe mit AndroidX Media3 / ExoPlayer in EntropieReductor (TTS-Audio in
> einer Compose-App, teils im Hintergrund) von vornherein **richtig** baut — idiomatisch, offiziell belegt
> (developer.android.com/media/media3, ExoPlayer-Reference, Now in Android). Gegenseite (was schiefgeht) im Bug-Almanach.
> **Versions-Anker (live aus EntropieReductor):** AndroidX **media3 1.5.1** (`media3-exoplayer` + `media3-common`;
> für Hintergrund `media3-session`; optional `media3-common-ktx`, `media3-ui-compose`) · Kotlin **2.1.0** · Compose · Android 12–15.
> **Wichtiger Versions-Hinweis:** Die fertigen Compose-State-Holder (`rememberPlayPauseButtonState`) und
> `PlayerSurface` kamen erst mit **media3 1.6.0** — bei **1.5.1** existiert nur `Player.listen` (seit 1.5.0) als Baustein.
> `bufferForPlaybackMs`-Default ist in 1.5.1 noch **2500 ms** (auf 1000 gesenkt erst 1.6.0).
> **Gegenstück (was schiefgeht):** [`bugs/android/media3-exoplayer.md`](../../bugs/android/media3-exoplayer.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Gradle | nur benötigte media3-Module; ALLE auf GLEICHER Version (Version-Catalog) | §1.1 |
| 2 | Kotlin | optional `media3-common-ktx` (`Player.listen`) | §1.3 |
| 3 | Lifecycle | `DefaultLifecycleObserver`; API ≥ 24 onStart/onStop, ≤ 23 onResume/onPause | §2.1 |
| 4 | Freigabe | `player.release()` + Referenz `null`, deterministisch (onCleared/onDispose) | §2.2 |
| 5 | Config-Change | Player im (Android)ViewModel, Release in `onCleared()` | §2.3 |
| 6 | Vordergrund vs Hintergrund | Vordergrund: in onStop releasen; Hintergrund: `MediaSessionService` | §2.4, §6 |
| 7 | Threading | Player auf EINEM (Main-)Thread erstellen+bedienen; aus Coroutine `withContext(Main)` | §3.1 |
| 8 | Listener-Thread | Callbacks feuern auf `applicationLooper` — UI direkt setzen ok | §3.2 |
| 9 | Audio-Focus | `setAudioAttributes(attrs, handleAudioFocus = true)`; kein eigenes Focus-Handling | §4.1 |
| 10 | TTS-Attribute | `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH` | §4.2 |
| 11 | Kopfhörer ab | `setHandleAudioBecomingNoisy(true)` | §4.3 |
| 12 | Status-UI | an `isPlaying`/`onIsPlayingChanged` koppeln, nicht `playWhenReady` | §4.4 |
| 13 | Player-Anzahl | EINE Instanz über viele Clips wiederverwenden, nicht pro Clip neu | §5.1 |
| 14 | Mehrere Clips | Playlist-API (`setMediaItems`/`addMediaItem`); `ConcatenatingMediaSource` deprecated | §5.2 |
| 15 | Reihenfolge | `setMediaItem(s) → prepare() → play()`; `pause()` statt `stop()`; Replay `seekTo(0)+play()` | §5.3 |
| 16 | Clip-Zuordnung | `setMediaId`/`setTag` + `onMediaItemTransition`/`onEvents` | §5.4 |
| 17 | kurze Clips | `DefaultLoadControl` `bufferForPlaybackMs ≈ 250–1000` (1.5.1-Default 2500) | §5.5 |
| 18 | TTS-Bytes | `ByteArrayDataSource`-Factory + `MediaItem.setMimeType(...)` | §5.6 |
| 19 | Hintergrund | Player+`MediaSession` in `MediaSessionService`; UI via `MediaController` | §6.1, §6.4 |
| 20 | Service-Manifest | `foregroundServiceType="mediaPlayback"` + FGS-Permissions + Intent-Filter | §6.2 |
| 21 | Screen-off-Streaming | `setWakeMode(C.WAKE_MODE_NETWORK)` (+ WAKE_LOCK-Permission) | §6.3 |
| 22 | Notification | media3 erzeugt sie; nur `MediaMetadata` liefern (ab API 33 nicht via Provider biegen) | §6.5 |
| 23 | Media-Buttons/Resume | media3 handhabt Buttons; Resume = MediaButtonReceiver + `onPlaybackResumption` | §6.6 |
| 24 | Compose-Player | im ViewModel; sonst `remember` + `DisposableEffect{ onDispose{ release() } }` | §7.1, §7.2 |
| 25 | Compose-Lifecycle | `LifecycleEventObserver` in `DisposableEffect(lifecycleOwner)` | §7.3 |
| 26 | Compose-Clipwechsel | `LaunchedEffect(clipKey)`, Player-Calls auf Main | §7.4 |
| 27 | Compose-State | Player = Single Source of Truth; State-Holder via `Player.listen` (Holder ab 1.6.0) | §7.5 |
| 28 | reines Audio | KEIN PlayerView/PlayerSurface/SurfaceView nötig | §7.6 |
| 29 | Fehler | `onPlayerError` + `prepare()`-Retry; ggf. `LoadErrorHandlingPolicy` | §8.1 |
| 30 | DI | EINE Instanz `@Singleton`/Service-scoped bereitstellen | §8.2 |
| 31 | R8 | media3 bringt Keep-Regeln mit; aktuelles AGP; Release-Build testen | §8.3 |
| 32 | Diagnose | `player.addAnalyticsListener(EventLogger())` im Debug; `onEvents` für gebündelte Updates | §8.4 |

---

## 1) Build-Setup

### 1.1 Nur benötigte Module, ALLE auf gleicher Version (Version-Catalog)
`offiziell`
- Pflicht `media3-exoplayer` (zieht `media3-common`); UI/Format/Session-Module nur bei Bedarf. „All modules must be of the same version." Zentral im `gradle/libs.versions.toml` (`media3 = "1.5.1"`, alle Module `version.ref`).
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

### 1.2 minSdk 21+, Java-8
`offiziell`
- media3 1.5.x verlangt minSdk ≥ 21 und Java-8-Sprachfeatures (in AGP-8/Kotlin-Projekten Standard).
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/media3

### 1.3 `media3-common-ktx` für Kotlin-Coroutine-Anbindung
`offiziell`
- Optionales Artefakt (seit 1.5.0) mit `Player.listen` (suspend) — sauberes Event-Mitlesen ohne Listener-Boilerplate, unter Wahrung der Threading-Regel.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/media3

---

## 2) Lifecycle & Ressourcen-Freigabe

### 2.1 Lifecycle-bewusst via `DefaultLifecycleObserver`
`offiziell`
- Init/Release-Logik in eine `DefaultLifecycleObserver`-Klasse auslagern + `lifecycle.addObserver(...)` (wiederverwendbar, kein dupliziertes Activity-Boilerplate; `@OnLifecycleEvent` ist veraltet). API-Regel: ≥ 24 init `onStart`/release `onStop`; ≤ 23 init `onResume`/release `onPause` (wegen Multi-Window).
- **Quelle:** https://developer.android.com/media/implement/playback-app · https://developer.android.com/topic/libraries/architecture/lifecycle

### 2.2 Immer `release()` + Referenz `null`, deterministisch
`offiziell`
- „It's important to release the player … to free up limited resources such as video decoders." Nach `release()` keine Methode mehr aufrufen (neue Instanz nötig). Freigabe deterministisch erzwingen (`onStop`/`onPause`, ViewModel-`onCleared()`, Compose-`DisposableEffect.onDispose`). `stop()` ≠ `release()` (stop hält Ressourcen).
- **Quelle:** https://developer.android.com/media/implement/playback-app

### 2.3 Player im (Android)ViewModel für Config-Changes
`offiziell`
- Player im `AndroidViewModel` überlebt Rotation (kein Re-Init, keine Doppel-Instanz bei Recomposition); echte Freigabe in `onCleared()`. KEINE `Context`/`View`/`Activity`-Referenzen im ViewModel halten.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/viewmodel

### 2.4 Vordergrund vs Hintergrund: wann NICHT in onStop releasen
`offiziell`
- Reine Vordergrund-Wiedergabe → in `onStop`/`onPause` releasen. Hintergrund-Audio (TTS, das weiterlaufen soll) → Player NICHT in der Activity halten/releasen, sondern in einen `MediaSessionService` (§6); UI hält nur einen `MediaController` (in onStop via `releaseFuture` lösen).
- **Quelle:** https://developer.android.com/media/media3/session/background-playback

### 2.5 `setForegroundMode(true)` nur im engen Reuse-Fall
`offiziell`
- Hält teure Ressourcen (Decoder) über `stop()`→`prepare()`-Lücken — nur bei Vordergrund + Reuse mit Lücken sinnvoll, sonst aus (blockiert Decoder für andere Apps).
- **Quelle:** https://developer.android.com/reference/kotlin/androidx/media3/exoplayer/ExoPlayer

### 2.6 Wiedergabe-State über den Lifecycle erhalten
`offiziell`
- Beim Release `playWhenReady` + `currentPosition` + `currentMediaItemIndex` sichern, beim Re-Init via `seekTo(index, position)` + `playWhenReady` restaurieren (nahtloses Weitermachen).
- **Quelle:** https://developer.android.com/media/implement/playback-app

---

## 3) Threading

### 3.1 Player auf EINEM (Main-)Thread erstellen UND bedienen
`offiziell`
- „ExoPlayer instances must be accessed from a single application thread … should be the application's main thread." Aus Coroutinen `withContext(Dispatchers.Main)` (UI-getrieben `Main.immediate`); schwere Arbeit (TTS-Synthese/IO) in `Dispatchers.IO`, Player-Call zurück auf Main. Maßgeblicher Thread via `player.applicationLooper`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

### 3.2 `Player.Listener`-Callbacks feuern auf dem applicationLooper
`offiziell`
- „the player also calls all listener callbacks on this thread." Im Callback ist man bereits auf dem richtigen Thread → UI/State direkt setzen, kein `runOnUiThread`. Für aus dem Callback gestartete periodische Reader vor dem Player-Read zurück auf den Player-Thread.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

---

## 4) Audio-Focus & AudioAttributes (TTS)

### 4.1 ExoPlayer den Audio-Focus automatisch managen lassen
`offiziell`
- `setAudioAttributes(attrs, handleAudioFocus = true)` als EINZIGE Focus-Quelle — „your app shouldn't include any code for requesting or responding to audio focus." Kein zusätzliches `AudioManager.requestAudioFocus`/`OnAudioFocusChangeListener`.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### 4.2 Korrekte AudioAttributes für TTS
`offiziell`
- `setUsage(C.USAGE_MEDIA)` (Pflicht für Auto-Focus + Media-Volume-Routing; `USAGE_GAME` ginge auch — NICHT `USAGE_ASSISTANT`, sonst kein Auto-Focus/IllegalArgumentException) + `setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)` (Sprache wird nicht geduckt, sondern pausiert+fortgesetzt → kein verpasstes Wort). Einmalig auf Main setzen.
  ```kotlin
  val attrs = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()
  ExoPlayer.Builder(ctx).setAudioAttributes(attrs, true).setHandleAudioBecomingNoisy(true).build()
  ```
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### 4.3 `setHandleAudioBecomingNoisy(true)`
`offiziell`
- Kopfhörer/BT ab → ExoPlayer pausiert selbst (statt laut über Lautsprecher). Kein eigener `ACTION_AUDIO_BECOMING_NOISY`-Receiver.
- **Quelle:** https://developer.android.com/media/implement/playback-app

### 4.4 Status-UI an `isPlaying` koppeln, nicht `playWhenReady`
`offiziell`
- `isPlaying` ist nur true bei `STATE_READY` + `playWhenReady` + keine Suppression. Bei Focus-Verlust bleibt `playWhenReady=true`, aber `isPlaying=false`. UI an `onIsPlayingChanged`/`getPlaybackSuppressionReason` hängen.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/listening-to-player-events

---

## 5) Player wiederverwenden, Playlist & State

### 5.1 EINE Instanz über viele Clips wiederverwenden
`offiziell`
- Player ist teuer (Decoder/Threads) — eine Instanz behalten, Clips per Playlist-API einspielen, `release()` nur im finalen Teardown. NICHT pro TTS-Clip neu bauen+releasen.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

### 5.2 Playlist-API statt Einzel-Tausch; `ConcatenatingMediaSource` ist deprecated
`offiziell`
- `setMediaItems`/`addMediaItem`/`replaceMediaItem`/`removeMediaItem`/`clearMediaItems` direkt am Player — Änderungen während der Wiedergabe brauchen kein erneutes `prepare()`. `ConcatenatingMediaSource` deprecated.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists

### 5.3 Reihenfolge + pause/stop/replay
`offiziell`
- `setMediaItem(s) → prepare() → play()`. Zum Fortsetzen `pause()` (bleibt READY) statt `stop()` (→ IDLE, braucht neu `prepare()`). Replay nach `STATE_ENDED`: `seekTo(0)` + `play()`. Retry nach Fehler: `prepare()`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

### 5.4 Clips identifizieren: `setMediaId`/`setTag` + `onMediaItemTransition`/`onEvents`
`offiziell`
- Pro Clip `MediaItem.Builder().setMediaId(id)`/`setTag(...)`; in `onMediaItemTransition(item, reason)` eindeutig erkennen, welcher Clip läuft. `onEvents` für gebündelte/Player-steuernde Logik (z.B. seek nach Transition). Nicht über Index-Zählung raten.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists · https://developer.android.com/media/media3/exoplayer/listening-to-player-events

### 5.5 Kurze Clips: `DefaultLoadControl` mit niedrigem `bufferForPlaybackMs`
`offiziell`
- 1.5.1-Default `bufferForPlaybackMs = 2500` (1.6.0 senkt auf 1000) — für kurze TTS-Clips zu hoch. Eigenen LoadControl setzen (Constraint `minBufferMs ≥ bufferForPlaybackMs`):
  ```kotlin
  DefaultLoadControl.Builder().setBufferDurationsMs(5_000, 10_000, 250, 500).build()
  ```
  Bei lokalen Bytes unkritisch; bei Netzquellen Rebuffering-Trade-off beachten.
- **Quelle:** https://github.com/androidx/media/blob/1.6.0/RELEASENOTES.md · https://developer.android.com/media/media3/exoplayer/customization

### 5.6 TTS-Bytes via `ByteArrayDataSource` + MIME-Hint
`offiziell` (Mechanik) · `extern` (konkrete Byte-Nutzung)
- In-memory TTS-Bytes über eine `ByteArrayDataSource`-`DataSource.Factory` (`ProgressiveMediaSource.Factory`/`DefaultMediaSourceFactory`) statt Disk-Umweg; `MediaItem.Builder().setMimeType(...)` setzen, wenn keine Extension/Container. `ByteArrayDataSource` NICHT selbst `open()`en.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/customization · https://developer.android.com/media/media3/exoplayer/media-items

---

## 6) Hintergrund-Audio: MediaSessionService

### 6.1 `MediaSessionService` als Container; `onCreate` baut, `onDestroy` released BEIDE
`offiziell`
- Player + `MediaSession` in einem `MediaSessionService` (nicht in der Activity) — externe Clients (System-UI, Bluetooth, Auto, Assistant) können verbinden. `onCreate` baut Player+Session; `onDestroy`: erst `player.release()`, dann `mediaSession.release()`; `onGetSession` gibt die EINE Session zurück.
- **Quelle:** https://developer.android.com/media/media3/session/background-playback

### 6.2 Manifest: FGS-Typ + Permissions + Intent-Filter
`offiziell`
- `foregroundServiceType="mediaPlayback"`, `android:exported="true"`, Intent-Filter `androidx.media3.session.MediaSessionService`; Permissions `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (Android 14) + `POST_NOTIFICATIONS` (Android 13 runtime). Nur `mediaPlayback` deklarieren (keine überflüssigen Typen).
- **Quelle:** https://developer.android.com/media/media3/session/background-playback · https://developer.android.com/about/versions/14/changes/fgs-types-required

### 6.3 `setWakeMode` für Screen-off-Streaming
`offiziell`
- `setWakeMode(C.WAKE_MODE_NETWORK)` (Streaming) bzw. `WAKE_MODE_LOCAL` (lokale Dateien) + `WAKE_LOCK`-Permission; ExoPlayer hält/gibt die Locks automatisch (kein manuelles WakeLock-Management). Nur mit Foreground-Service, nicht bei Vordergrund-Video.
- **Quelle:** https://developer.android.com/reference/kotlin/androidx/media3/exoplayer/ExoPlayer

### 6.4 UI verbindet über `MediaController` (= ein `Player`)
`offiziell`
- `MediaController.Builder(context, SessionToken(...)).buildAsync()` in `onStart` (asynchron, über Listener), in `onStop` `MediaController.releaseFuture(future)`. `MediaController` implementiert `Player` — gleiche API in der UI. Player nie direkt in der Activity halten.
- **Quelle:** https://developer.android.com/media/media3/session/connect-to-media-app

### 6.5 Notification: media3 erzeugt sie — nur `MediaMetadata` liefern
`offiziell`
- media3 baut automatisch eine `MediaStyle`-Notification. Nur `MediaMetadata` (Titel/Artist/Artwork) am `MediaItem` füllen. Ab API 33 NICHT via `MediaNotification.Provider` umbiegen (wirkt nicht) — stattdessen Metadaten + Media-Button-Preferences.
- **Quelle:** https://developer.android.com/media/media3/session/background-playback

### 6.6 Media-Buttons & Playback-Resumption
`offiziell`
- Hardware-/Bluetooth-Media-Buttons handhabt media3 automatisch (Player-Methoden) — kein eigener `ACTION_MEDIA_BUTTON`-Receiver. Für „Weiterhören nach Neustart": `MediaButtonReceiver` deklarieren UND `MediaSession.Callback.onPlaybackResumption()` implementieren (immer schnell abspielbares Material liefern). Standard-Controls laufen über den Default-`MediaSession.Callback`; eigene Aktionen als Custom-Commands. `onTaskRemoved` bewusst entscheiden (`pauseAllPlayersAndStopSelf()` falls Stopp gewünscht).
- **Quelle:** https://developer.android.com/media/media3/session/control-playback · https://developer.android.com/media/media3/session/background-playback

---

## 7) Jetpack-Compose-Integration

### 7.1 Player im ViewModel, nicht im Composable
`offiziell` (Bausteine) · `extern` (Pattern)
- Player im `AndroidViewModel` (braucht App-Context), Release in `onCleared()`; überlebt Config-Changes, keine Doppel-Instanz bei Recomposition. UI nur über `StateFlow` + `collectAsStateWithLifecycle`.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/viewmodel

### 7.2 Falls Composable-lokal: `remember` + `DisposableEffect.onDispose`
`offiziell`
- `val player = remember { ExoPlayer.Builder(context).build() }` + `DisposableEffect(Unit){ onDispose { player.release() } }` (onDispose ist Pflicht-Abschluss). `remember` überlebt KEINE Config-Changes — dafür ViewModel (§7.1).
- **Quelle:** https://developer.android.com/develop/ui/compose/side-effects

### 7.3 Lifecycle-Pause/Resume via `LifecycleEventObserver`
`offiziell`
- `DisposableEffect(lifecycleOwner)` (aus `LocalLifecycleOwner`) → `addObserver`/`onDispose { removeObserver }`; im Observer `pause()`/`play()`. Lambdas mit `rememberUpdatedState` absichern.
- **Quelle:** https://developer.android.com/develop/ui/compose/side-effects

### 7.4 Clipwechsel deklarativ über `LaunchedEffect(key)`
`offiziell`
- `LaunchedEffect(currentClipUri) { player.setMediaItem(...); player.prepare(); player.play() }` — Key-Wechsel cancelt+startet neu. `LaunchedEffect` läuft auf `Main.immediate` (passt zur Player-Thread-Regel); schwere Arbeit in `withContext(IO)`, Player-Call zurück auf Main.
- **Quelle:** https://developer.android.com/develop/ui/compose/side-effects

### 7.5 Player = Single Source of Truth; State über `Player.listen`
`offiziell`
- UI-State aus State-Holdern (`mutableStateOf` + `Player.listen`), nicht lokal umschalten: Klick → Kommando an Player → Player feuert Event → Holder aktualisiert UI. Fertige `remember*State`-Holder gibt es erst ab **media3 1.6.0**; bei 1.5.1 selbst über `Player.listen` (1.5.0) bauen. Keine selbstgebauten Player-Event-Flows (offiziell abgeraten: Latenz/illegale Zwischenzustände).
- **Quelle:** https://developer.android.com/media/media3/ui/compose-customization

### 7.6 Reines Audio (TTS): kein PlayerView/PlayerSurface/SurfaceView
`offiziell`
- `PlayerSurface`/`ContentFrame` sind nur für Videoausgabe. Für TTS-Audio nur Player + eigene Compose-Controls — kein `AndroidView`-Interop.
- **Quelle:** https://developer.android.com/media/media3/ui/compose

---

## 8) Fehlerbehandlung, DI & R8

### 8.1 `onPlayerError` + `prepare()`-Retry; ggf. `LoadErrorHandlingPolicy`
`offiziell`
- `Player.Listener.onPlayerError(PlaybackException)` implementieren, `errorCode`/`cause` auswerten; Recovery via `player.prepare()`. Backoff/„fail fast" über eigene `LoadErrorHandlingPolicy` (`DefaultMediaSourceFactory(...).setLoadErrorHandlingPolicy(...)`). MIME-Hint am `MediaItem` bei Ambiguität.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/listening-to-player-events · https://developer.android.com/media/media3/exoplayer/customization

### 8.2 DI: EINE Instanz scoped bereitstellen
`offiziell` (Architektur) · `extern` (Hilt-Modul-Muster)
- `@Provides @Singleton` für `ExoPlayer` bzw. (Hintergrund) `MediaController`/`MediaSession` — eine langlebige Instanz, per Konstruktor in ViewModel/Service injizieren, nicht pro Screen neu bauen. Hintergrund: UI ↔ `MediaController` ↔ `MediaSession` ↔ EIN Player.
- **Quelle:** https://developer.android.com/media/media3/session/background-playback

### 8.3 R8: mitgelieferte Keep-Regeln; aktuelles AGP; Release testen
`offiziell`
- media3 liefert Consumer-R8-Regeln selbst — keine eigenen media3-Keeps. Ab 1.5.x (compileSdk 35) AGP-Version mit R8-Auto-Out-of-Lining nutzen; minifizierten Release-Build real testen (Reflection-Extractoren, Session).
- **Quelle:** https://developer.android.com/media/media3/exoplayer/shrinking · https://developer.android.com/jetpack/androidx/releases/media3

### 8.4 Listener-Disziplin, `onEvents`, `EventLogger`
`offiziell`
- `addListener`/`removeListener` am Lifecycle paaren (`Player.Listener` hat leere Defaults — nur Benötigtes implementieren). `onEvents(player, events)` für gebündelte/zusammengehörige State-Updates (und sichere Getter-Kombinationen). Im Debug `player.addAnalyticsListener(EventLogger())`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/listening-to-player-events

---

## Bezug: Best-Practice-Abschnitt ↔ Bug-Abschnitt

> Wechselseitig mit [`bugs/android/media3-exoplayer.md`](../../bugs/android/media3-exoplayer.md) (dort die Spiegel-Tabelle).

| Best-Practice (hier) | Verwandter Bug-Abschnitt (Almanach) |
|----------------------|-------------------------------------|
| §1.1 Module/Version | (Setup) |
| §2.1 DefaultLifecycleObserver | L2 falscher Lifecycle-Punkt |
| §2.2 release+null | L1 nie released, L5 stop vs release, L6 Doppel-Release |
| §2.3 ViewModel/onCleared | C1 Compose Mehrfach-Player |
| §2.4 Vordergrund vs Hintergrund | L3 Hintergrund stoppt in onStop |
| §3.1 ein Thread | T1 wrong thread, T2 Background-Erstellung |
| §3.2 Listener-Thread | T4 Listener auf applicationLooper |
| §4.1 handleAudioFocus | F1 spielt über andere, B5 manuelles Focus killt FGS |
| §4.2 USAGE_MEDIA+SPEECH | F3 USAGE_ASSISTANT-Crash/Ducking |
| §4.3 becoming noisy | F5 Kopfhörer ab |
| §4.4 isPlaying | F4 playWhenReady täuscht |
| §5.1 eine Instanz | S3 Re-Use nach release |
| §5.2 Playlist-API | S8 ConcatenatingMediaSource deprecated |
| §5.3 prepare/play/replay | S4 ohne prepare, S5 Replay, S6 nach stop |
| §5.4 Clip-Zuordnung | S9 rapide Clips/Race |
| §5.5 LoadControl | A6/TTS2 Anlauf-Latenz |
| §5.6 ByteArrayDataSource | A1 ByteArray-Falle, A2 MIME |
| §6.1–6.6 MediaSessionService | B1 Player in VM, B2 Android14 FGS, B3/B4 FGS-Crashes, B6 onGetSession, B7 POST_NOTIFICATIONS, B8 Media-Button |
| §7.1/§7.2 Compose-Player | C1 Mehrfach-Player, C4 Compose-Thread |
| §7.4 LaunchedEffect | C2 Clipwechsel |
| §7.6 kein Surface | T3 SurfaceView-Thread |
| §8.1 Fehler/Retry | A4 Decoder-Init, A7 Endlos-Buffering |
| §8.3 R8 | (media3 Keep-Regeln) |
