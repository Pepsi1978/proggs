# Audio-Wiedergabe mit AndroidX Media3 / ExoPlayer Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
