# Bekannte Bugs: Audio-Wiedergabe mit AndroidX Media3 / ExoPlayer

> PFLICHT-LESEN vor Arbeit an der Audio-WIEDERGABE (ExoPlayer/MediaSession/Player-Lifecycle) in
> EntropieReductor (TTS-Audio in einer Compose-App, teils im Hintergrund).
> Stand: recherchiert am 2026-06-14 mit **7 Researchern parallel** (offizielle Quellen zuerst:
> developer.android.com/media/media3, github.com/androidx/media + google/ExoPlayer Issues + RELEASENOTES.md)
> + Fix-Status-Lauf (Schlüssel-Anker #538/1.1.1 und bufferForPlaybackMs/1.6.0 direkt verifiziert).
> ~60 Einträge in 8 Sektionen.
> **Versions-Anker (live aus EntropieReductor):** AndroidX **media3 1.5.1** (`media3-exoplayer` + `media3-common`;
> für Hintergrund zusätzlich `media3-session`) · Kotlin **2.1.0** · Jetpack Compose · targetSdk 34/35 (Android 12–15).
> Neuer (nicht im Projekt): media3 **1.6.0** (Buffer-Defaults gesenkt, FGS-Verbesserungen), 1.7.x/1.8.x/1.9.x.
> (Hinweis: BestJournalAndroid nutzt aktuell KEIN media3 — der Stack liegt in EntropieReductor.)
>
> **Abgrenzung (was steht woanders):** TTS-Erzeugung / TTS-Provider-APIs → [`../apis/tts-provider.md`](../apis/tts-provider.md).
> Reine Kotlin-/Coroutinen-Themen → [`kotlin.md`](kotlin.md). Allgemeine Android-Platform/Foreground-Service-Runtime →
> [`android-platform.md`](android-platform.md). Hier geht es NUR um die Audio-**Wiedergabe** mit ExoPlayer/Media3.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **L** Lifecycle/Release · **C** Compose · **T** Threading · **F** Audio-Focus/Attributes ·
> **B** Hintergrund/Service · **S** Player-State/Re-Use/MediaItem · **A** Quelle/Format/Buffering · **TTS** kurze TTS-Clips.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Audio läuft nach Verlassen weiter / LeakCanary | `player.release()` + Referenz `null` | L1 |
| 2 | Wo releasen? | API ≥ 24: onStart/onStop · API ≤ 23: onResume/onPause · Service: onDestroy | L2 |
| 3 | Hintergrund-Audio stoppt bei `onStop` | Player in `MediaSessionService`, NICHT in `onStop` releasen | L3 |
| 4 | `stop()` statt `release()` | `stop()` gibt nichts frei → für Aufräumen `release()` | L5 |
| 5 | Compose: doppelter Ton / mehrere Player | `remember { ExoPlayer… }` + `DisposableEffect{ onDispose{ release() } }` | C1 |
| 6 | Compose: Clipwechsel | über `LaunchedEffect(key)`, NICHT Player neu bauen | C2 |
| 7 | Compose: Replay-Button „tot" nach Clip-Ende | meist Mehrfach-Player (C1); dann `seekTo(0)+play()` | C3, S5 |
| 8 | `IllegalStateException: Player is accessed on the wrong thread` | nur vom `applicationLooper` (Default Main) zugreifen | T1 |
| 9 | Player aus Coroutine bedient | `withContext(Dispatchers.Main){ … }` / `Handler(player.applicationLooper).post{}` | T1 |
| 10 | Player auf Background-Thread erstellt → hängt in BUFFERING | `HandlerThread` + `setLooper(...)` (Loop läuft) | T2 |
| 11 | Crash beim `release()` aus Background | Release ebenfalls auf Main-Thread | T1, L6 |
| 12 | spielt ÜBER andere Apps (Musik/Telefon) | `setAudioAttributes(attrs, handleAudioFocus = true)` | F1 |
| 13 | TTS-AudioAttributes | `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH` (NICHT `USAGE_ASSISTANT` → Crash) | F3 |
| 14 | Kopfhörer ab → laut über Lautsprecher | `setHandleAudioBecomingNoisy(true)` | F5 |
| 15 | Hintergrund-Audio stirbt in Doze / Prozess gekillt | Player+MediaSession in `MediaSessionService` + `setWakeMode` | B1 |
| 16 | Android 14: Crash bei `startForeground` | Manifest `foregroundServiceType="mediaPlayback"` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | B2 |
| 17 | `ForegroundServiceStartNotAllowedException` (Android 12) | auto Audio-Focus statt manuell; media3 ≥ 1.6.0 (10-Min-FGS-Timeout) | B3, B5 |
| 18 | `ForegroundServiceDidNotStartInTimeException` (5s) | Resumption nur bei sofort Abspielbarem (`shouldStartForegroundService`) | B4 |
| 19 | Android 13: keine Media-Notification | `POST_NOTIFICATIONS` runtime anfragen | B7 |
| 20 | Zugriff auf bereits `release()`-ten Player | Listener vor Release entfernen; Referenz `null`; nicht aus Callback releasen | S2 |
| 21 | Player nach `release()` wiederverwendet | unbrauchbar → neue Instanz; lieber EINE Instanz wiederverwenden | S3 |
| 22 | `setMediaItem` + `play` → spielt nicht | Reihenfolge **setMediaItem → prepare → play** | S4 |
| 23 | nach `stop()` startet `play()` nicht | `stop()` → IDLE; erneut `prepare()` (oder `pause()` statt `stop()`) | S6 |
| 24 | Replay nach `STATE_ENDED` tut nichts | `seekTo(0)` + `play()` | S5 |
| 25 | `ConcatenatingMediaSource` deprecated | Playlist-API (`setMediaItems`/`addMediaItem`) | S8 |
| 26 | rapide aufeinanderfolgende Clips → falsche Callbacks | Playlist + `onMediaItemTransition`; Item-ID prüfen | S9 |
| 27 | `UnrecognizedInputFormatException` aus ByteArray | `ByteArrayDataSource` NICHT selbst `open()`; `Uri.EMPTY` + `setMimeType` | A1 |
| 28 | Format/MIME nicht erkannt (keine Extension) | `MediaItem.setMimeType(...)` explizit | A2 |
| 29 | MP3 `Searched too many bytes` | media3 ≥ 1.4.1 (in 1.5.1 ✓) | A3 |
| 30 | `ERROR_CODE_DECODER_INIT_FAILED` | in `onPlayerError` `prepare()`-Retry; ggf. SW-Decoder bevorzugen | A4 |
| 31 | spürbare Latenz vor kurzem Clip | eigener `DefaultLoadControl` `bufferForPlaybackMs ≈ 250` (1.5.1-Default 2500!) | A6, TTS2 |
| 32 | `STATE_ENDED` fehlt bei sehr kurzem Clip | media3 ≥ 1.1.1 (in 1.5.1 ✓) + positionsbasierter Fallback | TTS1 |

---

## L) Lifecycle & Release

> Fast alles hier ist **by-design / Lifecycle-Vertrag** (kein Library-Bug) — gilt in jeder Version inkl. 1.5.1.
> Alle FIXes sind funktionserhaltend (Wiedergabe bleibt, nur korrekt aufgeräumt).

### L1. ExoPlayer nie `release()` → Memory-Leak, Audio läuft nach Verlassen weiter ⭐ HAEUFIG
- **Symptom:** Beim Verlassen des Screens läuft der Ton weiter; LeakCanary meldet geleakte Activity/Fragment/ViewModel über den `ExoPlayer`; Hardware-Decoder/AudioTrack/Threads bleiben belegt.
- **Ursache:** `ExoPlayer` hält interne Threads, AudioTrack, Renderer, ggf. Codec/Surface und Context-Referenzen. Das UI-Lebenszyklus-Ende stoppt die Wiedergabe NICHT automatisch.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** immer explizit freigeben + Referenz lösen:
  ```kotlin
  player?.release(); player = null   // gibt Threads, AudioTrack, Surface, Decoder frei
  ```
- **Quelle:** https://developer.android.com/media/implement/playback-app

### L2. Release am falschen Lifecycle-Punkt / falsche API-Stufe
- **Symptom:** Release in `onDestroy` → Ressourcen zu spät frei (andere App bekommt keinen Codec) bzw. nie (onDestroy nicht garantiert). Release in `onPause` bei API ≥ 24 → Wiedergabe stoppt im Split-Screen, obwohl sichtbar. Release nur in `onStop` bei API ≤ 23 → Leak (onStop nicht garantiert).
- **Ursache:** Multi-Window ab API 24 verschiebt den korrekten Punkt; `onDestroy` wird nicht garantiert aufgerufen.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** API ≥ 24 → init in `onStart`, release in `onStop`. API ≤ 23 → init in `onResume`, release in `onPause`. Service → `onDestroy` (erst `player.release()`, dann `mediaSession.release()`).
- **Quelle:** https://developer.android.com/media/implement/playback-app · https://github.com/google/ExoPlayer/issues/7117

### L3. Vordergrund-Release-Regel fälschlich auf Hintergrund-Wiedergabe angewandt
- **Symptom:** Hintergrund-Audio soll weiterspielen, bricht aber beim Bildschirm-Aus (`onStop`) ab.
- **Ursache:** Die „release in onStop"-Regel gilt NUR für reine Vordergrund-Wiedergabe (Player in Activity). Für Hintergrund wurde der Player in `onStop` released → stoppt absichtlich, hier falsch.
- **Versionen:** alle inkl. 1.5.1 (Architektur).
- **FIX:** Player NICHT in der Activity halten; in einen `MediaSessionService` auslagern (siehe B1). UI hält nur einen `MediaController`, der in `onStop` via `MediaController.releaseFuture(...)` gelöst wird — der Player im Service lebt weiter.
- **Quelle:** https://developer.android.com/media/implement/playback-app

### L5. `stop()` mit `release()` verwechselt → Leak; bzw. Player nach `release()` weiterverwendet
- **Symptom:** (a) `stop()` beim Verlassen statt `release()` → Ton stoppt, aber Threads/AudioTrack/Decoder bleiben → Leak bleibt. (b) Nach `release()` weiterverwendet → unbrauchbar.
- **Ursache:** `stop()` versetzt nur in `STATE_IDLE` (Instanz lebt weiter, wiederverwendbar via `prepare()`); `release()` zerstört die Instanz endgültig.
- **Versionen:** alle inkl. 1.5.1 (API-Semantik).
- **FIX:** zum Aufräumen `release()` + `null`; zum Wiederverwenden `stop()` + `setMediaItem` + `prepare()`; nach `release()` neue Instanz bauen.
- **Quelle:** https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer

### L6. Doppel-Release → `IllegalStateException` / Race
- **Symptom:** Sporadischer Crash, wenn `release()` zweimal läuft (z.B. `DisposableEffect.onDispose` UND `onStop`/`onDestroy`).
- **Ursache:** mehrere Aufräumpfade releasen dieselbe (tote) Instanz.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** Release idempotent: nach `release()` sofort Referenz `null`; nur EINEN verantwortlichen Aufräumpfad. `player?.isReleased` prüfbar.
- **Quelle:** https://github.com/google/ExoPlayer/issues/7117

---

## C) Jetpack-Compose-Integration

### C1. Player ohne `remember`/`DisposableEffect` → Recomposition erzeugt mehrere Player, kein Release ⭐ HAEUFIG
- **Symptom:** Bei jeder Recomposition entsteht ein neuer `ExoPlayer`; überlappender/doppelter Ton, steigende RAM-/Thread-Zahl, LeakCanary; Audio läuft nach Verlassen weiter. Häufigster Compose-Fehler.
- **Ursache:** `ExoPlayer.Builder(context).build()` direkt im Composable-Body läuft bei jeder Recomposition neu; ohne `DisposableEffect` kein Aufräum-Hook.
- **Versionen:** alle inkl. 1.5.1 (Compose-Vertrag).
- **FIX:**
  ```kotlin
  val exoPlayer = remember { ExoPlayer.Builder(context).build() }
  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
  ```
  Für Config-Change-Überleben (Rotation) Player im `ViewModel` halten + in `onCleared()` releasen. Pause/Resume per `LifecycleEventObserver`.
- **Quelle:** https://developer.android.com/media/media3/ui/compose · https://github.com/androidx/media/issues/330

### C2. Clipwechsel über Player-Neuerzeugung statt `LaunchedEffect`
- **Symptom:** Bei jedem neuen Clip wird ein neuer Player gebaut → Overlap/Leak.
- **Ursache:** Clip als State, der den `remember`-Block neu auslöst, statt deklarativ über `LaunchedEffect(key)`.
- **Versionen:** alle inkl. 1.5.1.
- **FIX:** Player einmal `remember`n, Clipwechsel über `LaunchedEffect(clipUri){ setMediaItem(...); prepare(); play() }`.
- **Quelle:** https://developer.android.com/develop/ui/compose/side-effects

### C3. Replay-Button „tot" nach Clip-Ende (Compose)
- **Symptom:** Replay/Play funktioniert während der Wiedergabe, aber nicht mehr nachdem ein Clip einmal durchlief.
- **Ursache:** Meist Mehrfach-Player (C1) — der Button referenziert eine andere Instanz als die spielende. (Sekundär: `STATE_ENDED` braucht `seekTo(0)`, siehe S5.)
- **Versionen:** alle inkl. 1.5.1 (App-Referenzproblem).
- **FIX:** zuerst Single-Player-Muster (C1) herstellen; dann `seekTo(0)+play()` für Replay.
- **Quelle:** https://github.com/androidx/media/issues/330

### C4. Player-Bedienung aus `LaunchedEffect` auf Background-Dispatcher → Wrong-Thread
- **Symptom:** `IllegalStateException: Player is accessed on the wrong thread` aus einem `LaunchedEffect`, das `withContext(Dispatchers.IO)` nutzt.
- **Ursache:** `LaunchedEffect` läuft per Default auf `Dispatchers.Main.immediate` (korrekt); ein `withContext(IO)` darin + anschließender Player-Aufruf bricht.
- **Versionen:** alle inkl. 1.5.1.
- **FIX:** schwere Arbeit (TTS-Synthese/IO) in `withContext(Dispatchers.IO)`, Player-Aufruf in `withContext(Dispatchers.Main){ … }`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

---

## T) Threading

> ExoPlayer ist **single-threaded by design**: eine Instanz darf nur vom `applicationLooper`-Thread (default Main,
> der Erzeugungs-Thread) bedient werden. Abfragbar via `player.applicationLooper`. By-design — kein Opt-out.

### T1. `IllegalStateException: Player is accessed on the wrong thread` ⭐ HAEUFIG
- **Symptom:** `Player is accessed on the wrong thread. Current thread: '…' Expected thread: 'main'` — sofort beim ersten Player-Aufruf von einem fremden Thread (Coroutine `Dispatchers.IO`, RxJava-Scheduler, Executor). Auch beim `release()` aus Background.
- **Ursache:** Thread-Affinität; jeder Aufruf prüft `verifyApplicationThread()`. Die Meldung nennt `Current` (woher) und `Expected` (erlaubt).
- **Versionen:** alle inkl. 1.5.1 (by-design; früher nur Warning, heute harter Crash).
- **FIX:** alle Zugriffe (inkl. `release()`) auf den Player-Thread:
  ```kotlin
  withContext(Dispatchers.Main) { player.setMediaItem(item); player.prepare() }
  // oder generisch: Handler(player.applicationLooper).post { player.play() }
  ```
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world · https://github.com/google/ExoPlayer/issues/11060

### T2. Player auf Background-Thread ERSTELLT → hängt in `STATE_BUFFERING` (stiller Fehler)
- **Symptom:** Kein Crash, aber `playbackState` bleibt dauerhaft `STATE_BUFFERING`, Wiedergabe startet nie.
- **Ursache:** Player auf einem Thread mit Looper erstellt, dessen Message-Loop nicht läuft (`Looper.prepare()` ohne `Looper.loop()`); ExoPlayers interne Kommunikation hängt.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** `HandlerThread` nutzen (startet die Loop) + expliziten Looper setzen, ALLE Zugriffe über dessen Handler:
  ```kotlin
  val t = HandlerThread("exo").apply { start() }
  val player = ExoPlayer.Builder(ctx).setLooper(t.looper).build()
  Handler(t.looper).post { player.setMediaItem(item); player.prepare(); player.play() }
  ```
  Regel: für die Mehrheit Player auf Main erstellen+bedienen.
- **Quelle:** https://github.com/google/ExoPlayer/issues/10643

### T3. SurfaceView-Callbacks erzwingen den Main-Thread (Background-Player + Video)
- **Symptom:** `IllegalStateException`/„Exception configuring surface" beim Surface-Anhängen/Zerstören, obwohl der Player auf eigenem Looper läuft. (Für reines Audio/TTS selten relevant.)
- **Ursache:** `SurfaceHolder.Callback` feuert immer vom Main-Thread; läuft der Player auf Background-Looper, ruft die Surface-Kette ihn vom Main → falscher Thread.
- **Versionen:** alle inkl. 1.5.1 (Android-SurfaceView-Vertrag).
- **FIX:** bei SurfaceView-Video den Player auf Main bedienen; sonst Surface vor Zerstörung nicht-blockierend lösen.
- **Quelle:** https://github.com/androidx/media/issues/1336

### T4. `Player.Listener`-Callbacks kommen auf dem Application-Looper
- **Symptom:** Innerhalb eines Callbacks gestarteter periodischer Reader liest den Player später vom falschen Thread → Crash (T1).
- **Ursache:** Listener feuern auf `applicationLooper` (gut: im Callback ist man schon auf dem richtigen Thread). Fehler entsteht erst beim Auslagern auf einen anderen Thread.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** im Callback direkt zugreifen; für periodische Reader vor dem Player-Read auf den Player-Thread zurück (`observeOn(mainThread())` bzw. `Dispatchers.Main`).
- **Quelle:** https://github.com/google/ExoPlayer/issues/11060

### T5. `MediaSessionService` mit Non-Main-Looper-Player → Crash bei Controller-Connect (echter Bug)
- **Symptom:** Service crasht beim Connect eines `MediaController` (`Expected thread: 'OffMainLooper'`), KEIN App-Code im Stack — nur media3-internes `MediaNotificationManager`.
- **Ursache:** echter Bug in media3 1.0.0: `MediaNotificationManager.onConnected()` ruft `getCurrentTimeline()` vom Main-Thread ohne auf den Player-Looper zu posten.
- **Versionen:** betroffen media3 **1.0.0**, **gefixt ab 1.0.1** — in **1.5.1 erledigt**.
- **FIX:** media3 ≥ 1.0.1 (Anker 1.5.1 erfüllt das). Sonst Player auf Main-Looper.
- **Quelle:** https://github.com/androidx/media/issues/318

---

## F) Audio-Focus & AudioAttributes

> ExoPlayer verwaltet Audio-Focus standardmäßig NICHT — bis man `setAudioAttributes(attrs, handleAudioFocus = true)`
> setzt. Die meisten Punkte sind „vergessene/falsche Konfiguration" (by-design).

### F1. App spielt über andere Apps — Audio-Focus nie angefordert ⭐ HAEUFIG
- **Symptom:** TTS mischt sich über laufende Musik, Telefonate, Navi-Ansagen; nichts pausiert die andere App.
- **Ursache:** by-design: ohne `handleAudioFocus = true` fordert ExoPlayer nie Focus an und reagiert nicht auf Focus-Verlust.
- **Versionen:** alle inkl. 1.5.1 (Auto-Focus-Feature seit ExoPlayer 2.9).
- **FIX:** Auto-Focus aktivieren (siehe F3 für TTS-Attribute):
  ```kotlin
  ExoPlayer.Builder(ctx).setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true).build()
  ```
  Mit `true` KEIN eigener `requestAudioFocus`/`OnAudioFocusChangeListener` mehr (sonst doppelte Verwaltung, siehe B5).
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### F2. `setAudioAttributes(..., handleAudioFocus=true)` als Einmal-/Main-Thread-Aufruf
- **Symptom:** Focus unzuverlässig; sporadische Races bei wiederholtem/Off-Thread-Aufruf.
- **Ursache:** Der Boolean schaltet das Auto-Focus-Subsystem scharf; `setAudioAttributes(attrs)` (ohne Boolean) lässt Focus AUS. Nicht thread-safe.
- **Versionen:** alle inkl. 1.5.1.
- **FIX:** genau einmal beim Build auf dem Main-Thread setzen; nicht pro Track neu.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus

### F3. Falsche AudioAttributes für TTS → `IllegalArgumentException`-Crash bzw. falsches Ducking/Routing ⭐ HAEUFIG
- **Symptom:** (a) `setAudioAttributes(attrs, true)` mit `USAGE_ASSISTANT`/`USAGE_ASSISTANCE_*` wirft `IllegalArgumentException` (Crash). (b) Lautstärke-Tasten regeln den falschen Stream. (c) Sprache wird ungewollt geduckt/duckt andere.
- **Ursache:** Auto-Focus funktioniert NUR mit `USAGE_MEDIA` oder `USAGE_GAME` (permanenter Focus) — andere Usages → `IllegalArgumentException`. `setUsage()` bestimmt den Lautstärke-Stream. `CONTENT_TYPE_SPEECH` deaktiviert automatisches Ducking (für Sprache: Pause+Auto-Resume statt leiser).
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** für TTS mit Auto-Focus:
  ```kotlin
  AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)                       // Pflicht für Auto-Focus (NICHT ASSISTANT)
      .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)   // Sprache: kein Ducking, Pause+Resume
      .build()
  ```
  Wer zwingend `USAGE_ASSISTANT` braucht: Auto-Focus deaktivieren und Focus manuell über `AudioManager` anfordern.
- **Quelle:** https://developer.android.com/media/optimize/audio-focus · https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e

### F4. Transienter Focus-Verlust: `getPlayWhenReady()`/`getVolume()` täuschen
- **Symptom:** Bei kurzer Unterbrechung liefert `playWhenReady` weiter `true`, obwohl nichts hörbar ist; eigene UI-Logik zeigt falschen Zustand.
- **Ursache:** by-design: bei transientem Verlust pausiert/duckt ExoPlayer intern, behält aber `playWhenReady=true` (Auto-Resume).
- **Versionen:** alle inkl. 1.5.1.
- **FIX:** Zustand nicht an `playWhenReady`/`volume` festmachen; transient via `onPlaybackSuppressionReasonChanged` (`…TRANSIENT_AUDIO_FOCUS_LOSS`), permanent via `onPlayWhenReadyChanged` (`…AUDIO_FOCUS_LOSS`); Hörbarkeit via `isPlaying`.
- **Quelle:** https://github.com/androidx/media/issues/1470

### F5. Kopfhörer abgezogen → spielt laut über Lautsprecher weiter ⭐ HAEUFIG
- **Symptom:** Headset/Bluetooth getrennt → Audio wird laut auf den Lautsprecher umgeleitet und läuft weiter.
- **Ursache:** Audio-Focus deckt „becoming noisy" (`ACTION_AUDIO_BECOMING_NOISY`) NICHT ab — separater Schalter, default aus.
- **Versionen:** `setHandleAudioBecomingNoisy` seit ExoPlayer 2.11.0, in 1.5.1 vorhanden.
- **FIX:** `ExoPlayer.Builder(ctx).setHandleAudioBecomingNoisy(true)`. (Nur Pause; automatisches Resume beim Wieder-Einstecken liefert ExoPlayer bewusst nicht.)
- **Quelle:** https://github.com/google/ExoPlayer/issues/7288 · https://developer.android.com/media/implement/playback-app

### F6. Echter (gefixter) Bug: Focus nach `AUDIOFOCUS_LOSS_TRANSIENT` kaputt → kein Resume
- **Symptom:** Nach Anruf zeigt die UI „playing", aber kein Ton mehr; Player dauerhaft „tot".
- **Ursache:** Bug im `AudioFocusManager`: nach transientem Verlust kamen keine Events mehr.
- **Versionen:** betroffen **ExoPlayer 2.11.0–2.11.3**, **gefixt ab 2.11.4** — in **1.5.1 erledigt**.
- **FIX:** media3 1.5.1 enthält den Fix (kein Code nötig); nicht auf alten 2.11.x-Ständen bleiben.
- **Quelle:** https://github.com/google/ExoPlayer/issues/7182

### F7. Mehrere Player konkurrieren um Focus (+ herstellerspezifischer Fall)
- **Symptom:** Zwei ExoPlayer-Instanzen derselben App spielen gleichzeitig statt sich abzulösen (gerätespezifisch verschärft, z.B. Xiaomi).
- **Ursache:** Jeder Player verwaltet seinen Focus unabhängig; manche Vendor-AudioManager dispatchen `onAudioFocusChange(LOSS)` nicht zuverlässig.
- **Versionen:** reproduziert mit 1.5.1, gerätespezifisch.
- **FIX:** App-seitig nur EINE Instanz aktiv Audio ausgeben lassen bzw. beim Start des zweiten den ersten explizit pausieren (eigene Koordination).
- **Quelle:** https://github.com/androidx/media/issues/2100

---

## B) Hintergrund-Wiedergabe & Service

> Relevant, weil EntropieReductor TTS teils im Hintergrund spielt. Player NUR in ViewModel/Activity überlebt das nicht.

### B1. Player nur im ViewModel/Activity → Stop in Doze, Prozess gekillt ⭐ HAEUFIG
- **Symptom:** Audio läuft im Vordergrund; bei Bildschirm-Aus/Hintergrund stoppt es nach kurzer Zeit oder der Prozess wird gekillt.
- **Ursache:** Ohne laufenden Foreground-Service ist der Prozess ein normaler Hintergrundprozess → Doze/App-Standby/LMK darf ihn drosseln/killen. Player muss in einen `MediaSessionService`.
- **Versionen:** alle inkl. 1.5.1 (Architektur).
- **FIX:** Player + `MediaSession` in einen `MediaSessionService`; `setWakeMode(C.WAKE_MODE_NETWORK/LOCAL)`; UI verbindet via `MediaController`. `onGetSession` liefert die Session; `onDestroy`: erst `player.release()`, dann `mediaSession.release()`.
- **Quelle:** https://developer.android.com/media/media3/session/background-playback · https://github.com/androidx/media/issues/1715

### B2. Android 14: Crash/Reject ohne `foregroundServiceType="mediaPlayback"` ⭐ HAEUFIG
- **Symptom:** targetSdk 34 crasht beim `startForeground()` (`MissingForegroundServiceTypeException`); Play lehnt Upload ab, wenn die Permission fehlt.
- **Ursache:** ab Android 14 (API 34) muss jeder FGS einen Typ deklarieren; für Audio zusätzlich `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- **Versionen:** greift bei targetSdk ≥ 34.
- **FIX:**
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
  <service android:name=".PlaybackService" android:foregroundServiceType="mediaPlayback" android:exported="true">
      <intent-filter><action android:name="androidx.media3.session.MediaSessionService"/></intent-filter>
  </service>
  ```
- **Quelle:** https://developer.android.com/about/versions/14/changes/fgs-types-required

### B3. `ForegroundServiceStartNotAllowedException` (Android 12) bei Item-Wechsel im Hintergrund
- **Symptom:** Nach längerer Hintergrund-Wiedergabe Crash beim automatischen Item-Übergang: `startForegroundService() not allowed`; Stack endet in `MediaNotificationManager…startForeground`. Vor allem Android 12 / Samsung.
- **Ursache:** Android 12 verbietet FGS-Start aus dem Hintergrund. Fällt der Player kurz aus dem Foreground-State (Pause/Error/Focus-Verlust) und media3 zieht beim nächsten State-Change die Notification per `startForegroundService()` neu hoch → verboten. Häufigster Auslöser: manuelles Audio-Focus-Handling (B5).
- **Versionen:** 1.0.0-beta01 bis 1.5.x, primär Android 12. Strukturell entschärft ab **1.6.0** (Default 10-Min-Foreground-Timeout nach Pause/Error/Stop) — in 1.5.1 NOCH NICHT.
- **FIX:** (1) Auto-Audio-Focus statt manuell (B5) — Kern-Fix. (2) media3 ≥ 1.6.0. (3) bei Playback-Error im Hintergrund nicht auto-retryen, sondern Notification zum manuellen Resume posten. Der kursierende `onUpdateNotification(..., true)`-Trick maskiert nur und kann denselben Crash auslösen.
- **Quelle:** https://github.com/androidx/media/issues/111 · https://github.com/androidx/media/issues/1715

### B4. `ForegroundServiceDidNotStartInTimeException` (5-Sekunden-Regel) ⭐ HAEUFIG
- **Symptom:** `Context.startForegroundService() did not then call Service.startForeground()` — gehäuft Samsung/Xiaomi, oft bei Playback-Resumption (Media-Button/System-UI nach App-Kill) oder verzögertem Item-Übergang.
- **Ursache:** `startForegroundService()` verlangt binnen ~5 s ein `startForeground()` (sichtbare Notification). Beim Resumption-Pfad (ungebunden) ohne sofort abspielbares Material entsteht keine Notification → Timeout-Crash. (Mit gebundenem `MediaController` ist der Service bound → kann nicht so crashen.)
- **Versionen:** 1.0.0-beta02 bis ≥ 1.6.1, OEM-abhängig nie 100% beseitigt; `shouldStartForegroundService` als Hebel ab **1.5.0** (in 1.5.1 verfügbar).
- **FIX:** `MediaButtonReceiver.shouldStartForegroundService(intent)` überschreiben → `false`, wenn nichts Abspielbares vorliegt; `MediaSession.Callback.onPlaybackResumption()` IMMER abspielbares Material liefern; Player früh initialisieren.
- **Quelle:** https://github.com/androidx/media/issues/167 · https://github.com/androidx/media/issues/1709

### B5. Manuelles Audio-Focus-Handling killt den Foreground-State (löst B3 aus)
- **Symptom:** Benachrichtigung/Anruf pausiert; beim Resume (`AUDIOFOCUS_GAIN` → `play()`) kommt `ForegroundServiceStartNotAllowedException`.
- **Ursache:** Manuelles Focus-Handling pausiert bei Verlust und verliert den Foreground-State; Resume aus dem Hintergrund will den FGS neu starten → Android 12 verboten.
- **Versionen:** Android 12, alle media3 < 1.6.0.
- **FIX:** manuelles `OnAudioFocusChangeListener`/`requestAudioFocus` entfernen, ExoPlayer via `setAudioAttributes(attrs, true)` übernehmen lassen (bleibt bei transientem Verlust im Foreground-State).
- **Quelle:** https://github.com/androidx/media/issues/111

### B6. `onGetSession` null / Service nicht im Manifest / Session nicht released
- **Symptom:** `MediaController…buildAsync()` verbindet nie; keine Notification; nach Neustart „Session already exists"/Leak; Service nie zerstört.
- **Ursache/FIX (funktionserhaltend):** `onGetSession` für die eigene UI IMMER die gültige Session liefern (null nur zum bewussten Ablehnen); Service mit `<intent-filter>` `androidx.media3.session.MediaSessionService` deklarieren; in `onDestroy` Player UND Session releasen; Player-Lebensdauer an den Service koppeln (in `onCreate` erzeugen), keinen Singleton-Player nur `stop()`en.
- **Versionen:** alle inkl. 1.5.1 (Setup-Disziplin).
- **Quelle:** https://github.com/androidx/media/issues/167 · https://developer.android.com/media/media3/session/background-playback

### B7. `POST_NOTIFICATIONS` fehlt (Android 13) → keine erlaubte FGS-Anzeige
- **Symptom:** Auf Android 13+ keine Media-Notification; FGS instabil.
- **Ursache:** ab Android 13 (API 33) ist `POST_NOTIFICATIONS` Runtime-Permission; ein Media-FGS braucht eine sichtbare Notification.
- **Versionen:** Android 13+.
- **FIX:** `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` + zur Laufzeit anfragen.
- **Quelle:** https://github.com/androidx/media/issues/167 · https://developer.android.com/media/media3/session/background-playback

### B8. Media-Button/Bluetooth/Voice startet Wiedergabe im Hintergrund → Crash / „spielt von selbst"
- **Symptom:** Fremd-App/Media-Button löst im Hintergrund FGS-Start-Crash (Android 12) aus; oder App startet nach Reboot zufällig Wiedergabe.
- **Ursache:** nur Bluetooth darf per Media-Button im Hintergrund neu starten; `onPlaybackResumption` wird auch für reine Boot-Wiederherstellung getriggert.
- **Versionen:** Android 12 für die Crash-Variante; Exception-Abfang in media3 ab ~1.1.0/1.2.0.
- **FIX:** Resumption-Pfad über `shouldStartForegroundService` absichern (B4); bei abgefangener Exception Notification zum manuellen Resume posten; Boot-Resume vom echten Play unterscheiden.
- **Quelle:** https://github.com/androidx/media/issues/167

---

## S) Player-State, Re-Use & MediaItem

> State-Machine: `STATE_IDLE` (initial/gestoppt/Fehler) → `STATE_BUFFERING` → `STATE_READY` → `STATE_ENDED`.
> Merksatz: **setMediaItem → prepare → play**, Instanz wiederverwenden statt releasen, nach `stop()` neu `prepare()`.

### S2. Zugriff auf bereits `release()`-ten Player / Listener feuert nach Release
- **Symptom:** `IllegalStateException` aus `ListenerSet.release` bzw. NPE, wenn eine Coroutine/ein Callback `player.xxx()` nach `release()` aufruft.
- **Ursache:** ausstehendes Event/asynchrone Aufgabe überlebt das `release()` und greift auf die tote Instanz; Listener nicht abgemeldet.
- **Versionen:** ein ListenerSet-Race war in ExoPlayer 2.18.1 betroffen, **gefixt in media3 1.0.0** (in 1.5.1 ✓). Zugriff-nach-Release generell by-design.
- **FIX:** vor `release()` Listener entfernen, Referenz `null`, ausstehende Coroutines canceln; nie aus einem Callback synchron releasen (`Handler(player.applicationLooper).post { player.release() }`).
- **Quelle:** https://github.com/google/ExoPlayer/issues/10758

### S3. Player nach `release()` wiederverwenden → unbrauchbar
- **Symptom:** Nach `release()` tut der Player nichts mehr; `setMediaItem`+`prepare` bringt keine Wiedergabe.
- **Ursache:** `release()` ist endgültig (kein Re-Init). Anti-Pattern bei TTS: Player pro Äußerung released + dieselbe Variable wiederbenutzt.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** EINE Instanz über viele Clips wiederverwenden (`clearMediaItems` + `setMediaItem` + `prepare` + `play`); `release()` nur am Lebensende. Falls released → neue Instanz.
- **Quelle:** https://github.com/google/ExoPlayer/issues/3768

### S4. `setMediaItem`/`setMediaItems` ohne `prepare()` → spielt nicht ⭐ HAEUFIG
- **Symptom:** `setMediaItem(...)` + `play()`/`playWhenReady=true`, aber nichts passiert; Player bleibt in `STATE_IDLE`.
- **Ursache:** MediaItems setzen bereitet die Quelle nicht vor; ohne `prepare()` verlässt der Player `STATE_IDLE` nie.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** Reihenfolge strikt: `setMediaItem(item)` → `prepare()` → `play()`. (Re-prepare nach Fehler ist das offizielle Recovery-Mittel.)
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists

### S5. Replay nach `STATE_ENDED`: `play()` tut nichts
- **Symptom:** Derselbe Clip soll erneut spielen; nach dem ersten Durchlauf bewirkt `play()` nichts.
- **Ursache:** im `STATE_ENDED` steht die Position am Ende; `play()` setzt nur `playWhenReady=true`, bewegt die Position nicht.
- **Versionen:** alle inkl. 1.5.1 (Semantik).
- **FIX:** `player.seekTo(0); player.play()` (oder `seekToDefaultPosition()`). Falls vorher `stop()`/`release()` lief, vorher neu `prepare()`. Bei „totem Button" zuerst Mehrfach-Player ausschließen (C1).
- **Quelle:** https://github.com/androidx/media/issues/330

### S6. Nach `stop()` ist der Player in `STATE_IDLE` und braucht erneut `prepare()`
- **Symptom:** Nach `player.stop()` startet `play()` nicht wieder.
- **Ursache:** `stop()` versetzt zurück in `STATE_IDLE` und gibt geladene Daten frei.
- **Versionen:** alle inkl. 1.5.1 (State-Machine).
- **FIX:** zum Fortsetzen `pause()` statt `stop()` (bleibt READY); nach echtem `stop()` `prepare()` + `play()`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/listening-to-player-events

### S8. `ConcatenatingMediaSource` deprecated → Playlist-API verwenden
- **Symptom:** Deprecation-Warnungen, Fehlinteraktionen mit `seekToNext()`; ungeeignet für rasche TTS-Clip-Sequenzen.
- **Ursache:** MediaSource-Verkettung wurde durch die Top-Level-Playlist-API auf dem `Player` ersetzt.
- **Versionen:** deprecated seit media3 **1.2.0**; in 1.5.1 weiterhin deprecated.
- **FIX:** `setMediaItems`/`addMediaItem`/`replaceMediaItem`/`removeMediaItem`/`clearMediaItems` direkt am Player.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists

### S9. Rapides `setMediaItem` für viele Clips → Races / alte Callbacks falsch zugeordnet ⭐ HAEUFIG (TTS)
- **Symptom:** Bei schnellem `setMediaItem`+`prepare`+`play` in Folge wird ein `STATE_ENDED`/`STATE_READY` des VORHERIGEN Clips dem NEUEN zugeordnet; Clips übersprungen/doppelt getriggert.
- **Ursache:** `onPlaybackStateChanged(state)` trägt keine Clip-Identität; in-flight Events des alten Clips treffen nach dem Item-Wechsel ein.
- **Versionen:** by-design (Callback-Semantik); `onMediaItemTransition`/`onEvents` in allen Versionen inkl. 1.5.1.
- **FIX:** Playlist-API + `onMediaItemTransition` (eindeutiges Item pro Übergang) statt manueller Einzel-Schleife; bei Einzel-Wiedergabe Item-ID (`setMediaId`/`setTag`) im Callback gegen `player.currentMediaItem?.mediaId` prüfen; Events in `onEvents` bündeln.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists · https://github.com/androidx/media/issues/726

---

## A) Audio-Quelle, Format & Buffering

### A1. `UnrecognizedInputFormatException` aus `ByteArrayDataSource` (TTS-Bytes) ⭐ HAEUFIG (TTS)
- **Symptom:** Beim Abspielen von TTS-Audio aus einem `ByteArray`: `UnrecognizedInputFormatException: None of the available extractors could read the stream`, Abbruch in `onPlayerError`.
- **Ursache:** zwei Fallen: (1) man ruft `byteArrayDataSource.open(...)` selbst auf — das macht die Library; manuelles `open()` leert die Quelle. (2) eine Fantasie-URI ohne erkennbare Extension → kein Extractor-Match.
- **Versionen:** alle inkl. 1.5.1 (API-Falle, by-design).
- **FIX:**
  ```kotlin
  val factory = DataSource.Factory { ByteArrayDataSource(data) }   // NICHT open() aufrufen
  val src = ProgressiveMediaSource.Factory(factory, DefaultExtractorsFactory())
      .createMediaSource(MediaItem.Builder().setUri(Uri.EMPTY).setMimeType(MimeTypes.AUDIO_MPEG).build())
  player.setMediaSource(src); player.prepare()
  ```
  ByteArray nach Übergabe nicht mehr verändern (wird nicht kopiert).
- **Quelle:** https://github.com/google/ExoPlayer/issues/8552

### A2. Format/MIME nicht erkannt bei fehlender/falscher Extension (lokale Datei / data-URI)
- **Symptom:** `ParserException`/`UnrecognizedInputFormatException` bei Datei ohne Extension, falscher Extension oder `data:`-URI ohne MIME.
- **Ursache:** `DefaultExtractorsFactory` wählt primär nach MIME/URI-Extension; Sniffing nur Fallback. Bei TTS-Bytes/data-URIs fehlt die Extension.
- **Versionen:** alle inkl. 1.5.1 (by-design).
- **FIX:** MIME-Hint explizit: `MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.AUDIO_MPEG /* AUDIO_WAV / AUDIO_RAW */).build()`. (data-URI-`seekTo` ist unzuverlässig — für kurze Clips meist egal.)
- **Quelle:** https://developer.android.com/media/media3/exoplayer/progressive

### A3. MP3 `ParserException: Searched too many bytes` (CBR-Seeking-Regression)
- **Symptom:** `ParserException: Searched too many bytes.{contentIsMalformed=true}` aus `Mp3Extractor.synchronize`; betrifft MP3 mit `Info`-Frame (typisch für LAME/ffmpeg-encodete TTS-MP3).
- **Ursache:** Regression: `ConstantBitrateSeeker` meldete die letzte Datenposition als unbekannt → Suche über das durch den `Info`-Header gegebene Ende hinaus.
- **Versionen:** betroffen **1.2.1–1.4.0**, **gefixt ab 1.4.1** (Commit b09cea9) — in **1.5.1 erledigt**.
- **FIX:** media3 ≥ 1.4.1 (Anker 1.5.1 erfüllt das). Sonst CBR-Seeking explizit steuern (`setConstantBitrateSeekingEnabled(true)`).
- **Quelle:** https://github.com/androidx/media/issues/1480

### A4. `ERROR_CODE_DECODER_INIT_FAILED` / `DecoderInitializationException` (geräteabhängig)
- **Symptom:** `onPlayerError` mit `ERROR_CODE_DECODER_INIT_FAILED`, Cause `MediaCodecRenderer$DecoderInitializationException: Decoder init failed: OMX.…`; sporadisch, gerätespezifisch, oft beim schnellen Re-Init/mehreren Instanzen.
- **Ursache:** Hardware-`MediaCodec` lässt sich nicht initialisieren (belegt, Treiber-Race, zu schnelles Re-Acquire nach `release()`).
- **Versionen:** geräteabhängig, kein einzelner Fix; bis inkl. 1.5.1.
- **FIX:** in `onPlayerError` recovern (`seekToDefaultPosition(); prepare()`); für TTS Software-Decoder bevorzugen (`DefaultRenderersFactory().setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)` mit FFmpeg-Extension) — umgeht flaky HW-Codecs.
- **Quelle:** https://github.com/androidx/media/issues/936

### A6. Spürbare Anlauf-Latenz / `DefaultLoadControl`-Default zu groß für kurze Clips ⭐ HAEUFIG (TTS)
- **Symptom:** merkliche Verzögerung bis zum Tonbeginn bei kurzen TTS-Clips; Player verweilt unnötig lange in `STATE_BUFFERING`.
- **Ursache:** `DefaultLoadControl`-Default `bufferForPlaybackMs = 2500` (Streaming-orientiert) — für 1–3-s-Clips fast „erst alles puffern, dann starten".
- **Versionen:** Default **2500 ms in 1.5.1**; offiziell auf **1000 ms gesenkt erst ab 1.6.0** (verifiziert) — d.h. in 1.5.1 selbst setzen.
- **FIX:**
  ```kotlin
  val loadControl = DefaultLoadControl.Builder()
      .setBufferDurationsMs(5_000, 10_000, /* bufferForPlaybackMs */ 250, /* afterRebuffer */ 500)
      .build()
  ExoPlayer.Builder(ctx).setLoadControl(loadControl).build()
  ```
- **Quelle:** https://developer.android.com/media/media3/exoplayer/customization · https://github.com/androidx/media/blob/1.6.0/RELEASENOTES.md

### A7. Endlos-Buffering / Player hängt in `STATE_BUFFERING` ohne Fehler (Netzwerk-Source)
- **Symptom:** Player bleibt dauerhaft in `STATE_BUFFERING`; kein `onPlayerError` auch bei Netzabriss.
- **Ursache:** ohne explizites Timeout an der HTTP-Quelle wartet die Source endlos; zu großer Back-Buffer bricht die Buffering-Logik.
- **Versionen:** kontextabhängig bis 1.5.1.
- **FIX:** `DefaultHttpDataSource.Factory().setConnectTimeoutMs(8000).setReadTimeoutMs(8000)`; Back-Buffer moderat; in `onPlayerError` `prepare()`-Retry. Für TTS: Clips lokal/aus ByteArray laden (umgeht Netzwerk-Buffering ganz).
- **Quelle:** https://github.com/androidx/media/issues/1841

---

## TTS) Sehr kurze TTS-Clips (Use-Case-Spezifika)

### TTS1. `STATE_ENDED` feuert nie bei sehr kurzen Clips ⭐ HAEUFIG
- **Symptom:** Bei sehr kurzen Clips (<~1 s) kommt `onPlaybackStateChanged(STATE_ENDED)` nie; `BUFFERING`/`READY` normal, nur `ENDED` fehlt. „Spiele nächsten Clip wenn aktueller endet"-Kette hängt.
- **Ursache:** Regression durch Integer-Division in `AudioTrackPositionTracker.durationUsToFrames` (Abrunden) → End-of-Stream bei Sub-Sekunden-Clips nicht erkannt.
- **Versionen:** betroffen **1.1.0**, **gefixt ab 1.1.1** (`Util.ceilDivide`, verifiziert) — in **1.5.1 erledigt**.
- **FIX:** media3 ≥ 1.1.1 (Anker 1.5.1 ✓). Härtung: nicht ausschließlich auf `STATE_ENDED` verlassen — positionsbasierten Fallback ergänzen:
  ```kotlin
  override fun onIsPlayingChanged(isPlaying: Boolean) {
      if (!isPlaying && player.playbackState == Player.STATE_READY &&
          player.currentPosition >= player.duration - 15) onClipFinished()
  }
  ```
- **Quelle:** https://github.com/androidx/media/issues/538

### TTS2. Anlauf-Latenz bei kurzen Clips (siehe A6)
- **Symptom:** kurzer Clip „verschluckt"/träge wegen hohem `bufferForPlaybackMs` (2500 in 1.5.1).
- **FIX:** eigener `DefaultLoadControl` mit `bufferForPlaybackMs ≈ 250` (A6) ODER media3 ≥ 1.6.0 (1000-Default).
- **Quelle:** https://github.com/androidx/media/blob/1.6.0/RELEASENOTES.md

### TTS3. `playWhenReady = true` vor `prepare()` → erster Clip „verschluckt"
- **Symptom:** erster TTS-Clip spielt gelegentlich nicht/wirkt verschluckt.
- **Ursache:** Reihenfolge-/Listener-Disziplin; bei extrem kurzem Clip kann `READY→ENDED` durchlaufen, bevor die UI/Listener bereit ist.
- **Versionen:** by-design; Wechselwirkung mit TTS1 nur < 1.1.1.
- **FIX:** Reihenfolge: Listener registrieren → `setMediaItem` → `prepare()` → `play()`.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/hello-world

### TTS4. Knackgeräusche/„Pop" + Tool-Wahl (ExoPlayer vs SoundPool)
- **Symptom:** bei sehr kurzen Clips hörbares Pop am Anfang/Ende, kleine Lücken zwischen Clips.
- **Ursache:** ExoPlayer baut pro Clip Decoder/AudioTrack-Pipeline auf (Anlaufkosten/Boundary-Artefakte); MP3-Encoder-Delay/Padding knackt an Grenzen. (Fallstrick: `SoundPool` ist für sehr kurze One-Shot-Sounds das latenzärmere Tool — aber hier ExoPlayer-Lösung gefordert.)
- **Versionen:** by-design; `setMediaItems`-Playlist in allen Versionen inkl. 1.5.1.
- **FIX (funktionserhaltend, ExoPlayer):** Clips als EINE `setMediaItems(list)`-Playlist statt einzeln (gapless-Übergänge); `bufferForPlaybackMs` senken (A6); PCM/WAV bevorzugen oder TTS-seitig kurzes Fade/Stille-Padding.
- **Quelle:** https://developer.android.com/media/media3/exoplayer/playlists · https://github.com/google/ExoPlayer/issues/7994

---

## ✅ Fix-Status (was ist schon behoben?)

> **Ehrlichkeits-Regel:** „belegt gefixt" = Changelog/RELEASENOTES/Maintainer. `gh`-CLI war nicht verfügbar;
> #538 (1.1.1) und der bufferForPlaybackMs-Default (1.6.0) wurden per WebSearch an den offiziellen Release-Notes
> gegengeprüft. Der FGS-10-Min-Timeout-Mechanismus (1.6.0) ist researcher-/Changelog-basiert, nicht zeilengenau verifiziert.

### Belegt gefixt (bis zum Projekt-Anker media3 1.5.1)

| Früherer Bug | gefixt ab | Beleg | Bezug |
|--------------|-----------|-------|-------|
| `STATE_ENDED` fehlt bei sehr kurzen Clips | **1.1.1** (verifiziert) | #538 / RELEASENOTES 1.1.1 | TTS1 |
| MediaSessionService Wrong-Thread-Crash bei Connect | **1.0.1** | #318 | T5 |
| MediaSession-/Service-Leak (10-s-Finalization) | **1.1.0** (auch 1.0.2) | #346 | (Session) |
| MP3 „Searched too many bytes" (CBR-Seeker) | **1.4.1** | #1480 | A3 |
| AudioFocusManager: kein Resume nach transientem Verlust | **ExoPlayer 2.11.4** | #7182 | F6 |
| ListenerSet-Race beim Entfernen während Release | **1.0.0** | #10758 | S2 |

→ **EntropieReductor (media3 1.5.1) ist gegen TTS1, T5, A3, F6 by-default geschützt.**

### Noch NICHT gefixt / erst NACH 1.5.1 (Workaround in 1.5.1 aktiv)

| Bug | Status | Was tun in 1.5.1 | Bezug |
|-----|--------|------------------|-------|
| `bufferForPlaybackMs`-Default zu hoch (2500) für kurze Clips | gesenkt auf 1000 erst **1.6.0** | eigenen `DefaultLoadControl` (250) setzen | A6, TTS2 |
| `ForegroundServiceStartNotAllowedException` (Android 12) | strukturell entschärft **1.6.0** (10-Min-FGS-Timeout) | Auto-Audio-Focus statt manuell; ggf. Upgrade | B3, B5 |
| Notification zeigt falschen Button bei transientem Focus-Verlust | gefixt **1.6.0** | manueller Workaround via `onPlaybackSuppressionReasonChanged` | (UI) |
| `ForegroundServiceDidNotStartInTimeException` (OEM, 5s) | nie 100% beseitigt; Hebel ab **1.5.0** | `shouldStartForegroundService` + `onPlaybackResumption` befüllen | B4 |
| alle Lifecycle/Threading/State/Setup-Fälle | **by-design** | korrektes Setup, nie Feature weglassen | L*, C*, T*, S*, F1–F5 |

---

## ✅ Pflicht-Checkliste (vor dem Commit von Wiedergabe-Code mental durchgehen)

- [ ] **Release:** `player.release()` + Referenz `null` am richtigen Lifecycle-Punkt (API ≥ 24 onStop / ≤ 23 onPause / Service onDestroy); idempotent, nur EIN Aufräumpfad. (L1, L2, L6)
- [ ] **Compose:** Player in `remember{}` + `DisposableEffect{ onDispose{ release() } }`; Clipwechsel über `LaunchedEffect(key)`; bei Rotation im ViewModel halten. (C1, C2)
- [ ] **Threading:** Player auf Main erstellen UND bedienen (inkl. `release()`); aus Coroutine `withContext(Dispatchers.Main)`; schwere Arbeit in IO. (T1, C4)
- [ ] **Audio-Focus:** `setAudioAttributes(attrs, handleAudioFocus = true)` mit `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH` (NIE `USAGE_ASSISTANT` → Crash); kein manuelles Focus-Handling. (F1, F3, B5)
- [ ] **Becoming noisy:** `setHandleAudioBecomingNoisy(true)`. (F5)
- [ ] **Hintergrund:** Player+MediaSession im `MediaSessionService` + `setWakeMode`; Manifest `foregroundServiceType="mediaPlayback"` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `POST_NOTIFICATIONS`; `onGetSession` liefert Session; Resumption nur bei Abspielbarem. (B1, B2, B4, B6, B7)
- [ ] **State:** Reihenfolge `setMediaItem → prepare → play`; nach `stop()` neu `prepare()`; Replay via `seekTo(0)+play()`; EINE Instanz wiederverwenden statt releasen. (S3, S4, S5, S6)
- [ ] **Sequenz:** viele TTS-Clips als `setMediaItems`-Playlist + `onMediaItemTransition` statt rapider Einzel-`setMediaItem`. (S9, TTS4)
- [ ] **Quelle:** ByteArray via `ByteArrayDataSource` (kein `open()`) + `setMimeType`; lokal statt Netzwerk; `onPlayerError` mit `prepare()`-Retry. (A1, A2, A4, A7)
- [ ] **Latenz:** eigener `DefaultLoadControl` `bufferForPlaybackMs ≈ 250` für kurze Clips (1.5.1-Default 2500). (A6, TTS2)
- [ ] **Version:** media3 1.5.1 deckt TTS1/T5/A3/F6 ab; ein Upgrade auf ≥ 1.6.0 entschärft B3 + Buffer-Latenz ohne Code. (Fix-Status)

---

## Bezug: Bug-Abschnitt ↔ Best-Practices

> Gegenseite (wie macht man es richtig):
> [`best-practices/android/media3-exoplayer.md`](../../best-practices/android/media3-exoplayer.md)
> (dort die Spiegel-Tabelle Best-Practice-Abschnitt ↔ Bug-Abschnitt).

| Bug-Abschnitt (hier) | Verwandter Best-Practice-Abschnitt |
|----------------------|------------------------------------|
| L1 nie released · L5 stop vs release · L6 Doppel-Release | §2.2 release+null |
| L2 falscher Lifecycle-Punkt | §2.1 DefaultLifecycleObserver |
| L3 Hintergrund stoppt in onStop | §2.4 Vordergrund vs Hintergrund |
| C1 Compose Mehrfach-Player · C3 toter Button | §7.1/§7.2 Player im ViewModel/remember |
| C4 Compose-Thread | §3.1 ein Thread · §7.4 LaunchedEffect |
| T1 wrong thread · T2 Background-Erstellung | §3.1 ein Thread |
| T3 SurfaceView-Thread | §7.6 kein Surface (Audio) |
| T4 Listener-Thread | §3.2 Listener-Thread |
| T5 MediaSessionService Wrong-Thread (1.0.1) | §6.1 Service-Container |
| F1 spielt über andere · F6 Focus-Resume | §4.1 handleAudioFocus |
| F3 USAGE_ASSISTANT-Crash | §4.2 USAGE_MEDIA+SPEECH |
| F4 playWhenReady täuscht | §4.4 isPlaying |
| F5 becoming noisy | §4.3 becoming noisy |
| B1 Player in VM | §6.1 MediaSessionService |
| B2 Android14 FGS-Typ | §6.2 Manifest |
| B3/B4/B5 FGS-Crashes | §4.1 handleAudioFocus · §6.2/§6.6 |
| B6 onGetSession/Setup | §6.1 Service-Lifecycle |
| B7 POST_NOTIFICATIONS | §6.2 Manifest/Permissions |
| B8 Media-Button/Resume | §6.6 Media-Buttons/Resumption |
| S3 Re-Use nach release | §5.1 eine Instanz |
| S4 ohne prepare · S5 Replay · S6 nach stop | §5.3 prepare/play/replay |
| S8 ConcatenatingMediaSource | §5.2 Playlist-API |
| S9 rapide Clips/Race | §5.4 Clip-Zuordnung |
| A1 ByteArray-Falle · A2 MIME | §5.6 ByteArrayDataSource |
| A3 MP3 CBR (1.4.1) | §1.1 Version |
| A4 Decoder-Init · A7 Endlos-Buffering | §8.1 Fehler/Retry |
| A6/TTS2 Anlauf-Latenz | §5.5 LoadControl |
| TTS1 STATE_ENDED kurz (1.1.1) | §1.1 Version · §5.4 |
| TTS4 Pops/Tool-Wahl | §5.2 Playlist (gapless) |
